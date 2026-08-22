package com.vinaynalavade.expensetracker.core.backup

/**
 * Pure Kotlin, dependency-free JSON serializer and parser for KharchaFlow backups and transaction exports.
 * Guarantees 100% deterministic operation across Android runtime and JVM unit test environments.
 */
object JsonBackupParser {

    // ==========================================
    // JSON SERIALIZATION
    // ==========================================

    fun toJson(backup: BackupData): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"backupVersion\": ").append(backup.backupVersion).append(",\n")
        sb.append("  \"appVersion\": \"").append(escape(backup.appVersion)).append("\",\n")
        sb.append("  \"createdAt\": ").append(backup.createdAt).append(",\n")

        // Preferences
        sb.append("  \"preferences\": {\n")
        sb.append("    \"openingBalanceSubunits\": ").append(backup.preferences.openingBalanceSubunits).append(",\n")
        sb.append("    \"currencyCode\": \"").append(escape(backup.preferences.currencyCode)).append("\",\n")
        sb.append("    \"themeMode\": \"").append(escape(backup.preferences.themeMode)).append("\",\n")
        sb.append("    \"dailyReminderEnabled\": ").append(backup.preferences.dailyReminderEnabled).append(",\n")
        sb.append("    \"dailyReminderHour\": ").append(backup.preferences.dailyReminderHour).append(",\n")
        sb.append("    \"dailyReminderMinute\": ").append(backup.preferences.dailyReminderMinute).append(",\n")
        sb.append("    \"emiRemindersEnabled\": ").append(backup.preferences.emiRemindersEnabled).append(",\n")
        sb.append("    \"notificationsMasterEnabled\": ").append(backup.preferences.notificationsMasterEnabled).append(",\n")
        sb.append("    \"budgetAlertsEnabled\": ").append(backup.preferences.budgetAlertsEnabled).append(",\n")
        sb.append("    \"monthlyBudgetLimitSubunits\": ").append(backup.preferences.monthlyBudgetLimitSubunits).append(",\n")
        sb.append("    \"recurringRemindersEnabled\": ").append(backup.preferences.recurringRemindersEnabled).append(",\n")
        sb.append("    \"recurringReminderAdvanceDays\": ").append(backup.preferences.recurringReminderAdvanceDays).append(",\n")
        sb.append("    \"savingsGoalNotificationsEnabled\": ").append(backup.preferences.savingsGoalNotificationsEnabled).append("\n")
        sb.append("  },\n")

        // Categories
        sb.append("  \"categories\": [\n")
        backup.categories.forEachIndexed { index, cat ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(cat.id).append(",\n")
            sb.append("      \"name\": \"").append(escape(cat.name)).append("\",\n")
            sb.append("      \"iconName\": \"").append(escape(cat.iconName)).append("\",\n")
            sb.append("      \"colorHex\": \"").append(escape(cat.colorHex)).append("\",\n")
            sb.append("      \"type\": \"").append(escape(cat.type)).append("\",\n")
            sb.append("      \"isDefault\": ").append(cat.isDefault).append("\n")
            sb.append("    }").append(if (index < backup.categories.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // Transactions
        sb.append("  \"transactions\": [\n")
        backup.transactions.forEachIndexed { index, tx ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(tx.id).append(",\n")
            sb.append("      \"amountSubunits\": ").append(tx.amountSubunits).append(",\n")
            sb.append("      \"type\": \"").append(escape(tx.type)).append("\",\n")
            sb.append("      \"categoryId\": ").append(tx.categoryId).append(",\n")
            sb.append("      \"paymentMethod\": \"").append(escape(tx.paymentMethod)).append("\",\n")
            sb.append("      \"note\": ").append(if (tx.note != null) "\"${escape(tx.note)}\"" else "null").append(",\n")
            sb.append("      \"timestamp\": ").append(tx.timestamp).append("\n")
            sb.append("    }").append(if (index < backup.transactions.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // Recurring Transactions
        sb.append("  \"recurringTransactions\": [\n")
        backup.recurringTransactions.forEachIndexed { index, rec ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(rec.id).append(",\n")
            sb.append("      \"title\": \"").append(escape(rec.title)).append("\",\n")
            sb.append("      \"amountSubunits\": ").append(rec.amountSubunits).append(",\n")
            sb.append("      \"type\": \"").append(escape(rec.type)).append("\",\n")
            sb.append("      \"categoryId\": ").append(rec.categoryId).append(",\n")
            sb.append("      \"paymentMethod\": \"").append(escape(rec.paymentMethod)).append("\",\n")
            sb.append("      \"note\": ").append(if (rec.note != null) "\"${escape(rec.note)}\"" else "null").append(",\n")
            sb.append("      \"frequency\": \"").append(escape(rec.frequency)).append("\",\n")
            sb.append("      \"dayOfMonth\": ").append(rec.dayOfMonth).append(",\n")
            sb.append("      \"dayOfWeek\": ").append(rec.dayOfWeek).append(",\n")
            sb.append("      \"startDate\": ").append(rec.startDate).append(",\n")
            sb.append("      \"endDate\": ").append(rec.endDate ?: "null").append(",\n")
            sb.append("      \"isEnabled\": ").append(rec.isEnabled).append(",\n")
            sb.append("      \"isAutoGenerated\": ").append(rec.isAutoGenerated).append(",\n")
            sb.append("      \"reminderDaysBefore\": ").append(rec.reminderDaysBefore ?: "null").append(",\n")
            sb.append("      \"lastGeneratedDate\": ").append(rec.lastGeneratedDate ?: "null").append(",\n")
            sb.append("      \"createdAt\": ").append(rec.createdAt).append(",\n")
            sb.append("      \"updatedAt\": ").append(rec.updatedAt).append("\n")
            sb.append("    }").append(if (index < backup.recurringTransactions.size - 1) "," else "").append("\n")
        }
        sb.append("  ]\n")

        sb.append("}")
        return sb.toString()
    }

    fun exportTransactionsToJson(transactions: List<BackupTransaction>, categories: List<BackupCategory>): String {
        val categoryMap = categories.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"exportedAt\": ").append(System.currentTimeMillis()).append(",\n")
        sb.append("  \"count\": ").append(transactions.size).append(",\n")
        sb.append("  \"transactions\": [\n")
        transactions.forEachIndexed { index, tx ->
            val cat = categoryMap[tx.categoryId]
            sb.append("    {\n")
            sb.append("      \"id\": ").append(tx.id).append(",\n")
            sb.append("      \"amountSubunits\": ").append(tx.amountSubunits).append(",\n")
            sb.append("      \"type\": \"").append(escape(tx.type)).append("\",\n")
            sb.append("      \"categoryId\": ").append(tx.categoryId).append(",\n")
            sb.append("      \"categoryName\": \"").append(escape(cat?.name ?: "Unknown")).append("\",\n")
            sb.append("      \"paymentMethod\": \"").append(escape(tx.paymentMethod)).append("\",\n")
            sb.append("      \"note\": ").append(if (tx.note != null) "\"${escape(tx.note)}\"" else "null").append(",\n")
            sb.append("      \"timestamp\": ").append(tx.timestamp).append("\n")
            sb.append("    }").append(if (index < transactions.size - 1) "," else "").append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    // ==========================================
    // JSON PARSING
    // ==========================================

    fun fromJson(jsonStr: String): BackupData {
        val root = parseElement(jsonStr.trim()) as? JsonObject
            ?: throw IllegalArgumentException("Invalid JSON root: Expected a JSON Object")

        val backupVersion = root.getInt("backupVersion")
            ?: throw IllegalArgumentException("Missing required 'backupVersion' in backup file")
        val appVersion = root.getString("appVersion") ?: "1.0.0"
        val createdAt = root.getLong("createdAt") ?: System.currentTimeMillis()

        // Parse Preferences
        val prefObj = root.getObject("preferences") ?: JsonObject(emptyMap())
        val preferences = BackupPreferences(
            openingBalanceSubunits = prefObj.getLong("openingBalanceSubunits") ?: 0L,
            currencyCode = prefObj.getString("currencyCode") ?: "INR",
            themeMode = prefObj.getString("themeMode") ?: "SYSTEM",
            dailyReminderEnabled = prefObj.getBoolean("dailyReminderEnabled") ?: false,
            dailyReminderHour = prefObj.getInt("dailyReminderHour") ?: 21,
            dailyReminderMinute = prefObj.getInt("dailyReminderMinute") ?: 0,
            emiRemindersEnabled = prefObj.getBoolean("emiRemindersEnabled") ?: true,
            notificationsMasterEnabled = prefObj.getBoolean("notificationsMasterEnabled") ?: false,
            budgetAlertsEnabled = prefObj.getBoolean("budgetAlertsEnabled") ?: false,
            monthlyBudgetLimitSubunits = prefObj.getLong("monthlyBudgetLimitSubunits") ?: 0L,
            recurringRemindersEnabled = prefObj.getBoolean("recurringRemindersEnabled") ?: false,
            recurringReminderAdvanceDays = prefObj.getInt("recurringReminderAdvanceDays") ?: 1,
            savingsGoalNotificationsEnabled = prefObj.getBoolean("savingsGoalNotificationsEnabled") ?: false
        )

        // Parse Categories
        val catArray = root.getArray("categories")
            ?: throw IllegalArgumentException("Missing required 'categories' in backup file")
        val categories = catArray.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj.getLong("id") ?: return@mapNotNull null
            val name = obj.getString("name") ?: return@mapNotNull null
            val iconName = obj.getString("iconName") ?: "category"
            val colorHex = obj.getString("colorHex") ?: "#64748B"
            val type = obj.getString("type") ?: "EXPENSE"
            val isDefault = obj.getBoolean("isDefault") ?: false
            BackupCategory(id, name, iconName, colorHex, type, isDefault)
        }

        // Parse Transactions
        val txArray = root.getArray("transactions")
            ?: throw IllegalArgumentException("Missing required 'transactions' in backup file")
        val transactions = txArray.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj.getLong("id") ?: return@mapNotNull null
            val amountSubunits = obj.getLong("amountSubunits") ?: return@mapNotNull null
            val type = obj.getString("type") ?: return@mapNotNull null
            val categoryId = obj.getLong("categoryId") ?: return@mapNotNull null
            val paymentMethod = obj.getString("paymentMethod") ?: "CASH"
            val note = obj.getString("note")
            val timestamp = obj.getLong("timestamp") ?: return@mapNotNull null
            BackupTransaction(id, amountSubunits, type, categoryId, paymentMethod, note, timestamp)
        }

        // Parse Recurring Transactions
        val recArray = root.getArray("recurringTransactions") ?: emptyList()
        val recurringTransactions = recArray.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj.getLong("id") ?: return@mapNotNull null
            val title = obj.getString("title") ?: return@mapNotNull null
            val amountSubunits = obj.getLong("amountSubunits") ?: return@mapNotNull null
            val type = obj.getString("type") ?: "EXPENSE"
            val categoryId = obj.getLong("categoryId") ?: return@mapNotNull null
            val paymentMethod = obj.getString("paymentMethod") ?: "CASH"
            val note = obj.getString("note")
            val frequency = obj.getString("frequency") ?: "MONTHLY"
            val dayOfMonth = obj.getInt("dayOfMonth") ?: 1
            val dayOfWeek = obj.getInt("dayOfWeek") ?: 1
            val startDate = obj.getLong("startDate") ?: System.currentTimeMillis()
            val endDate = obj.getLong("endDate")
            val isEnabled = obj.getBoolean("isEnabled") ?: true
            val isAutoGenerated = obj.getBoolean("isAutoGenerated") ?: true
            val reminderDaysBefore = obj.getInt("reminderDaysBefore")
            val lastGeneratedDate = obj.getLong("lastGeneratedDate")
            val recCreatedAt = obj.getLong("createdAt") ?: System.currentTimeMillis()
            val recUpdatedAt = obj.getLong("updatedAt") ?: System.currentTimeMillis()

            BackupRecurringTransaction(
                id, title, amountSubunits, type, categoryId, paymentMethod, note,
                frequency, dayOfMonth, dayOfWeek, startDate, endDate, isEnabled,
                isAutoGenerated, reminderDaysBefore, lastGeneratedDate, recCreatedAt, recUpdatedAt
            )
        }

        return BackupData(
            backupVersion = backupVersion,
            appVersion = appVersion,
            createdAt = createdAt,
            categories = categories,
            transactions = transactions,
            recurringTransactions = recurringTransactions,
            preferences = preferences
        )
    }

    fun parseTransactionsJson(jsonStr: String): List<BackupTransaction> {
        val root = parseElement(jsonStr.trim()) as? JsonObject
            ?: throw IllegalArgumentException("Expected JSON object with transactions array")

        val txArray = root.getArray("transactions")
            ?: throw IllegalArgumentException("Missing 'transactions' array")

        return txArray.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj.getLong("id") ?: 0L
            val amountSubunits = obj.getLong("amountSubunits") ?: return@mapNotNull null
            val type = obj.getString("type") ?: return@mapNotNull null
            val categoryId = obj.getLong("categoryId") ?: 0L
            val paymentMethod = obj.getString("paymentMethod") ?: "CASH"
            val note = obj.getString("note")
            val timestamp = obj.getLong("timestamp") ?: System.currentTimeMillis()
            BackupTransaction(id, amountSubunits, type, categoryId, paymentMethod, note, timestamp)
        }
    }

    // ==========================================
    // LIGHTWEIGHT JSON AST & TOKENIZER
    // ==========================================

    sealed class JsonElement
    data class JsonObject(val map: Map<String, JsonElement>) : JsonElement() {
        fun getString(key: String): String? = (map[key] as? JsonPrimitive)?.value?.takeIf { it != "null" }
        fun getLong(key: String): Long? = (map[key] as? JsonPrimitive)?.value?.toLongOrNull()
        fun getInt(key: String): Int? = (map[key] as? JsonPrimitive)?.value?.toIntOrNull()
        fun getBoolean(key: String): Boolean? = (map[key] as? JsonPrimitive)?.value?.toBooleanStrictOrNull()
        fun getObject(key: String): JsonObject? = map[key] as? JsonObject
        fun getArray(key: String): List<JsonElement>? = (map[key] as? JsonArray)?.list
    }
    data class JsonArray(val list: List<JsonElement>) : JsonElement()
    data class JsonPrimitive(val value: String) : JsonElement()

    private fun parseElement(json: String): JsonElement {
        var index = 0

        fun skipWhitespace() {
            while (index < json.length && json[index].isWhitespace()) {
                index++
            }
        }

        fun parseString(): String {
            index++ // skip opening quote
            val sb = StringBuilder()
            while (index < json.length) {
                val c = json[index++]
                if (c == '"') return sb.toString()
                if (c == '\\' && index < json.length) {
                    when (val next = json[index++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000c')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (index + 4 <= json.length) {
                                val hex = json.substring(index, index + 4)
                                index += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                        }
                        else -> sb.append(next)
                    }
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }

        fun parseValue(): JsonElement {
            skipWhitespace()
            if (index >= json.length) throw IllegalArgumentException("Unexpected end of JSON")
            return when (val c = json[index]) {
                '{' -> {
                    index++
                    val map = mutableMapOf<String, JsonElement>()
                    skipWhitespace()
                    if (index < json.length && json[index] == '}') {
                        index++
                        return JsonObject(map)
                    }
                    while (index < json.length) {
                        skipWhitespace()
                        if (json[index] != '"') throw IllegalArgumentException("Expected string key at index $index, found '${json[index]}'")
                        val key = parseString()
                        skipWhitespace()
                        if (index >= json.length || json[index] != ':') throw IllegalArgumentException("Expected ':' at index $index")
                        index++
                        val value = parseValue()
                        map[key] = value
                        skipWhitespace()
                        if (index < json.length && json[index] == ',') {
                            index++
                        } else if (index < json.length && json[index] == '}') {
                            index++
                            break
                        } else {
                            throw IllegalArgumentException("Expected ',' or '}' at index $index")
                        }
                    }
                    JsonObject(map)
                }
                '[' -> {
                    index++
                    val list = mutableListOf<JsonElement>()
                    skipWhitespace()
                    if (index < json.length && json[index] == ']') {
                        index++
                        return JsonArray(list)
                    }
                    while (index < json.length) {
                        val value = parseValue()
                        list.add(value)
                        skipWhitespace()
                        if (index < json.length && json[index] == ',') {
                            index++
                        } else if (index < json.length && json[index] == ']') {
                            index++
                            break
                        } else {
                            throw IllegalArgumentException("Expected ',' or ']' at index $index")
                        }
                    }
                    JsonArray(list)
                }
                '"' -> JsonPrimitive(parseString())
                else -> {
                    val start = index
                    while (index < json.length && json[index] !in ",}] \t\r\n") {
                        index++
                    }
                    val literal = json.substring(start, index)
                    JsonPrimitive(literal)
                }
            }
        }

        return parseValue()
    }

    private fun escape(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\b' -> sb.append("\\b")
                '\u000c' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code < 0x20) {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }
}
