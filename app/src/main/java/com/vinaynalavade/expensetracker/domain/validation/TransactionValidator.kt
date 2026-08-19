package com.vinaynalavade.expensetracker.domain.validation

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.TransactionType

data class ValidationResult(
    val isValid: Boolean,
    val amountError: String? = null,
    val categoryError: String? = null,
    val parsedAmount: Amount? = null
)

/**
 * Domain validator ensuring transaction data integrity before persistence.
 */
object TransactionValidator {

    private const val MAX_SUBUNITS_LIMIT = 100_000_000_000_00L // ₹100 Crore upper ceiling

    fun validateAmount(input: String, currency: Currency = Currency.DEFAULT): Pair<Boolean, String?> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return false to "Please enter an amount."
        }

        val parsed = Amount.fromStringOrNull(trimmed, currency)
            ?: return false to "Invalid amount format."

        if (parsed.isZero || parsed.isNegative) {
            return false to "Amount must be greater than zero."
        }

        if (parsed.subunits > MAX_SUBUNITS_LIMIT) {
            return false to "Amount exceeds the maximum limit."
        }

        return true to null
    }

    fun validateCategory(category: Category?, expectedType: TransactionType): Pair<Boolean, String?> {
        if (category == null) {
            return false to "Please select a category."
        }

        if (category.type != expectedType) {
            return false to "Category does not match ${expectedType.displayName.lowercase()} type."
        }

        return true to null
    }

    fun validateTransaction(
        amountInput: String,
        category: Category?,
        expectedType: TransactionType,
        currency: Currency = Currency.DEFAULT
    ): ValidationResult {
        val (isAmountValid, amountError) = validateAmount(amountInput, currency)
        val (isCategoryValid, categoryError) = validateCategory(category, expectedType)

        val parsedAmount = if (isAmountValid) {
            Amount.fromStringOrNull(amountInput.trim(), currency)
        } else {
            null
        }

        return ValidationResult(
            isValid = isAmountValid && isCategoryValid,
            amountError = amountError,
            categoryError = categoryError,
            parsedAmount = parsedAmount
        )
    }
}
