package com.vinaynalavade.expensetracker.presentation.settings.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Premium Profile Avatar component supporting custom selected image URIs
 * with fail-safe fallback to high-contrast initials or stylized KharchaFlow icon.
 */
@Composable
fun ProfileAvatar(
    imageUri: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showEditBadge: Boolean = true,
    onEditClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(imageUri) { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        if (imageUri.isNullOrBlank()) {
            bitmap = null
            loadFailed = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(imageUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    // Decode bounds first for memory-safe sampling
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(stream, null, options)

                    val targetSizePx = (size.value * 3).toInt().coerceAtLeast(128)
                    var sampleSize = 1
                    while (options.outWidth / (sampleSize * 2) >= targetSizePx &&
                        options.outHeight / (sampleSize * 2) >= targetSizePx
                    ) {
                        sampleSize *= 2
                    }

                    // Decode actual bitmap with sample size
                    context.contentResolver.openInputStream(uri)?.use { actualStream ->
                        val decodeOptions = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val decoded = BitmapFactory.decodeStream(actualStream, null, decodeOptions)
                        bitmap = decoded
                        loadFailed = (decoded == null)
                    }
                }
            } catch (e: Exception) {
                // Fail gracefully: SecurityException, FileNotFoundException, etc.
                bitmap = null
                loadFailed = true
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onEditClick != null) Modifier.clickable(onClick = onEditClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow border
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier
                .size(size)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            val currentBitmap = bitmap
            if (currentBitmap != null && !loadFailed) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                // Polished Default Avatar with Initials / Icon
                DefaultAvatarContent(displayName = displayName, size = size)
            }
        }

        // Camera / Edit Badge
        if (showEditBadge && onEditClick != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size((size.value * 0.36f).dp.coerceIn(20.dp, 28.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Profile Photo",
                        modifier = Modifier.size((size.value * 0.2f).dp.coerceIn(12.dp, 16.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultAvatarContent(
    displayName: String,
    size: Dp
) {
    val initials = remember(displayName) { extractInitials(displayName) }
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isNotEmpty()) {
            val fontSize = (size.value * 0.38f).sp
            Text(
                text = initials,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size((size.value * 0.55f).dp)
            )
        }
    }
}

private fun extractInitials(name: String): String {
    val cleaned = name.trim()
    if (cleaned.isBlank()) return ""
    val parts = cleaned.split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> ""
    }
}
