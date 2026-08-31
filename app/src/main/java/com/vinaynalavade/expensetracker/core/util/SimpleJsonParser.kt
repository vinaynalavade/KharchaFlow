package com.vinaynalavade.expensetracker.core.util

/**
 * Pure Kotlin, dependency-free lightweight JSON parser.
 * Guarantees 100% deterministic operation across Android runtime and JVM unit test environments.
 */
object SimpleJsonParser {

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

    fun parse(json: String): JsonElement {
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
}
