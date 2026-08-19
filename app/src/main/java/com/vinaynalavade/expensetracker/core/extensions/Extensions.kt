package com.vinaynalavade.expensetracker.core.extensions

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency

/**
 * Extension helper to convert Long to Amount
 */
fun Long.toAmount(): Amount = Amount.fromSubunits(this)

/**
 * Extension helper to format Long as Currency String directly
 */
fun Long.toFormattedCurrency(currency: Currency = Currency.DEFAULT): String =
    Amount.fromSubunits(this).format(currency)

/**
 * String capitalization utility
 */
fun String.capitalizeFirstLetter(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
