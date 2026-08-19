package com.vinaynalavade.expensetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Polished Category Icon with soft tinted rounded container matching the category theme color.
 */
@Composable
fun CategoryIcon(
    iconName: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    cornerRadius: Dp = 12.dp,
    contentDescription: String? = null
) {
    val categoryColor = parseColor(colorHex)
    val imageVector = getIconVector(iconName)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(categoryColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = categoryColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val cleanHex = if (hex.startsWith("#")) hex.substring(1) else hex
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(0xFF000000 or colorInt)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        Color(0xFF64748B)
    }
}

private fun getIconVector(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "restaurant" -> Icons.Default.Restaurant
        "shopping_cart" -> Icons.Default.ShoppingCart
        "shopping_bag" -> Icons.Default.ShoppingBag
        "directions_car" -> Icons.Default.DirectionsCar
        "receipt_long" -> Icons.AutoMirrored.Filled.ReceiptLong
        "movie" -> Icons.Default.Movie
        "medical_services" -> Icons.Default.MedicalServices
        "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
        "account_balance" -> Icons.Default.AccountBalance
        "school" -> Icons.Default.School
        "spa" -> Icons.Default.Spa
        "home" -> Icons.Default.Home
        "subscriptions" -> Icons.Default.Subscriptions
        "payments" -> Icons.Default.Payments
        "storefront" -> Icons.Default.Storefront
        "replay" -> Icons.Default.Replay
        "attach_money" -> Icons.Default.AttachMoney
        "work" -> Icons.Default.Work
        "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
        "card_giftcard" -> Icons.Default.CardGiftcard
        "more_horiz" -> Icons.Default.MoreHoriz
        else -> Icons.Default.Category
    }
}
