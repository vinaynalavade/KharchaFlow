package com.vinaynalavade.expensetracker.presentation.entry.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.vinaynalavade.expensetracker.core.util.AmountInputFormatter

/**
 * Custom VisualTransformation that automatically formats numeric amount inputs with comma grouping
 * (e.g. 1234 -> 1,234, 1234567.89 -> 1,234,567.89) while preserving exact bidirectional cursor positioning.
 */
class AmountVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val transformed = AmountInputFormatter.formatWithCommas(raw)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= raw.length) return transformed.length

                var rawCount = 0
                var transformedIndex = 0
                while (transformedIndex < transformed.length && rawCount < offset) {
                    if (transformed[transformedIndex] == raw[rawCount]) {
                        rawCount++
                    }
                    transformedIndex++
                }

                // If right after a comma in transformed text, advance past the comma
                while (transformedIndex < transformed.length && transformed[transformedIndex] == ',') {
                    transformedIndex++
                }

                return transformedIndex.coerceIn(0, transformed.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= transformed.length) return raw.length

                var rawIndex = 0
                val limit = offset.coerceIn(0, transformed.length)
                for (i in 0 until limit) {
                    if (transformed[i] != ',') {
                        rawIndex++
                    }
                }
                return rawIndex.coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}
