package com.vinaynalavade.expensetracker.core.backup

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * RFC 4180 compliant CSV generator and parser for transaction import and export.
 */
object CsvTransactionHelper {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    private val SIMPLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

    const val CSV_HEADER = "Date,Type,Category,Amount,Currency,Payment Method,Note,Created At"

    fun exportToCsv(transactions: List<Transaction>, currency: Currency): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER).append("\n")

        for (tx in transactions) {
            val dateStr = DATE_FORMATTER.format(Instant.ofEpochMilli(tx.timestamp))
            val typeStr = tx.type.displayName
            val categoryStr = tx.category.name
            val amountStr = tx.amount.toInputString(currency)
            val currencyCode = currency.code
            val paymentMethodStr = tx.paymentMethod.displayName
            val noteStr = tx.note ?: ""
            val createdAtStr = tx.timestamp.toString()

            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(typeStr)).append(",")
            sb.append(escapeCsv(categoryStr)).append(",")
            sb.append(escapeCsv(amountStr)).append(",")
            sb.append(escapeCsv(currencyCode)).append(",")
            sb.append(escapeCsv(paymentMethodStr)).append(",")
            sb.append(escapeCsv(noteStr)).append(",")
            sb.append(escapeCsv(createdAtStr)).append("\n")
        }

        return sb.toString()
    }

    data class ParsedCsvRow(
        val rowIndex: Int,
        val dateEpoch: Long?,
        val type: TransactionType?,
        val categoryName: String?,
        val amount: Amount?,
        val paymentMethod: PaymentMethod,
        val note: String?,
        val errorMessage: String? = null
    ) {
        val isValid: Boolean get() = errorMessage == null && dateEpoch != null && type != null && !categoryName.isNullOrBlank() && amount != null
    }

    fun parseCsv(csvContent: String, defaultCurrency: Currency = Currency.DEFAULT): List<ParsedCsvRow> {
        val rows = parseCsvRows(csvContent)
        if (rows.isEmpty()) return emptyList()

        val results = mutableListOf<ParsedCsvRow>()
        val header = rows.first()
        val dataRows = rows.drop(1)

        // Identify column indices
        var dateIdx = -1
        var typeIdx = -1
        var categoryIdx = -1
        var amountIdx = -1
        var paymentMethodIdx = -1
        var noteIdx = -1
        var createdAtIdx = -1

        for ((index, col) in header.withIndex()) {
            val normalized = col.trim().lowercase()
            when {
                normalized.contains("date") && !normalized.contains("created") -> dateIdx = index
                normalized.contains("type") -> typeIdx = index
                normalized.contains("category") -> categoryIdx = index
                normalized.contains("amount") -> amountIdx = index
                normalized.contains("payment") || normalized.contains("method") -> paymentMethodIdx = index
                normalized.contains("note") || normalized.contains("desc") -> noteIdx = index
                normalized.contains("created") -> createdAtIdx = index
            }
        }

        // Fallbacks if header names differ
        if (dateIdx == -1 && header.isNotEmpty()) dateIdx = 0
        if (typeIdx == -1 && header.size > 1) typeIdx = 1
        if (categoryIdx == -1 && header.size > 2) categoryIdx = 2
        if (amountIdx == -1 && header.size > 3) amountIdx = 3
        if (paymentMethodIdx == -1 && header.size > 5) paymentMethodIdx = 5
        if (noteIdx == -1 && header.size > 6) noteIdx = 6

        for ((rowIndex, row) in dataRows.withIndex()) {
            val humanRowIndex = rowIndex + 2 // 1-indexed including header
            if (row.all { it.isBlank() }) continue // Skip blank lines

            val dateRaw = if (dateIdx in row.indices) row[dateIdx].trim() else ""
            val typeRaw = if (typeIdx in row.indices) row[typeIdx].trim() else ""
            val categoryRaw = if (categoryIdx in row.indices) row[categoryIdx].trim() else ""
            val amountRaw = if (amountIdx in row.indices) row[amountIdx].trim() else ""
            val paymentMethodRaw = if (paymentMethodIdx in row.indices) row[paymentMethodIdx].trim() else ""
            val noteRaw = if (noteIdx in row.indices) row[noteIdx].trim().takeIf { it.isNotBlank() } else null
            val createdAtRaw = if (createdAtIdx in row.indices) row[createdAtIdx].trim() else ""

            // 1. Parse Date
            var parsedEpoch: Long? = createdAtRaw.toLongOrNull()
            if (parsedEpoch == null && dateRaw.isNotBlank()) {
                parsedEpoch = try {
                    Instant.from(DATE_FORMATTER.parse(dateRaw)).toEpochMilli()
                } catch (_: Exception) {
                    try {
                        val localDate = LocalDate.parse(dateRaw)
                        localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } catch (_: Exception) {
                        dateRaw.toLongOrNull()
                    }
                }
            }

            // 2. Parse Type
            val parsedType = when (typeRaw.uppercase()) {
                "EXPENSE", "EXPENSES", "DEBIT", "SPEND" -> TransactionType.EXPENSE
                "INCOME", "EARNING", "CREDIT", "SALARY" -> TransactionType.INCOME
                else -> null
            }

            // 3. Parse Amount
            val parsedAmount = parseAmountFlexible(amountRaw, defaultCurrency)

            // 4. Parse Payment Method
            val parsedPaymentMethod = PaymentMethod.fromString(paymentMethodRaw)

            // 5. Validate row
            val errorMessage = when {
                parsedEpoch == null -> "Row $humanRowIndex: Invalid date format '$dateRaw'"
                parsedType == null -> "Row $humanRowIndex: Invalid transaction type '$typeRaw'"
                categoryRaw.isBlank() -> "Row $humanRowIndex: Missing category name"
                parsedAmount == null || parsedAmount.subunits <= 0L -> "Row $humanRowIndex: Invalid amount '$amountRaw'"
                else -> null
            }

            results.add(
                ParsedCsvRow(
                    rowIndex = humanRowIndex,
                    dateEpoch = parsedEpoch ?: System.currentTimeMillis(),
                    type = parsedType,
                    categoryName = categoryRaw,
                    amount = parsedAmount,
                    paymentMethod = parsedPaymentMethod,
                    note = noteRaw,
                    errorMessage = errorMessage
                )
            )
        }

        return results
    }

    private fun parseAmountFlexible(raw: String, currency: Currency): Amount? {
        val trimmed = raw.trim()
        if (trimmed.startsWith("-") || trimmed.contains("-")) return null
        val cleaned = trimmed.replace(currency.symbol, "")
            .replace("$", "")
            .replace("₹", "")
            .replace(",", "")
            .replace("+", "")
            .trim()

        if (cleaned.isBlank()) return null
        return Amount.fromStringOrNull(cleaned, currency)
    }

    fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun parseCsvRows(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuotes = false
        var i = 0

        while (i < content.length) {
            val c = content[i]
            when {
                c == '"' -> {
                    if (insideQuotes && i + 1 < content.length && content[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip escaped quote
                    } else {
                        insideQuotes = !insideQuotes
                    }
                }
                c == ',' && !insideQuotes -> {
                    currentRow.add(currentField.toString())
                    currentField.clear()
                }
                (c == '\r' || c == '\n') && !insideQuotes -> {
                    if (c == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
                        i++ // Skip CRLF
                    }
                    currentRow.add(currentField.toString())
                    currentField.clear()
                    if (currentRow.any { it.isNotBlank() }) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                else -> {
                    currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
        }

        return rows
    }
}
