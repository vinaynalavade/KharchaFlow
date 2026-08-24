package com.vinaynalavade.expensetracker.presentation.settings.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ImageCropDialog(
    imageUri: Uri,
    onCropConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isCropping by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationDegrees by remember { mutableIntStateOf(0) }

    // Load and normalize bitmap orientation
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                val loaded = loadAndOrientBitmap(context, imageUri)
                sourceBitmap = loaded
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isCropping) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isCropping,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color(0xFF121212)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isCropping
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.btn_cancel),
                            tint = Color.White
                        )
                    }

                    Text(
                        text = stringResource(R.string.crop_photo_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            rotationDegrees = (rotationDegrees + 90) % 360
                        },
                        enabled = !isCropping && sourceBitmap != null
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = stringResource(R.string.crop_photo_rotate),
                            tint = Color.White
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.crop_photo_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Interactive Crop Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else if (sourceBitmap != null) {
                        val currentBitmap = sourceBitmap!!

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CardShape)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            val containerWidth = constraints.maxWidth.toFloat()
                            val containerHeight = constraints.maxHeight.toFloat()
                            val cropSize = min(containerWidth, containerHeight) * 0.85f

                            val cropRect = remember(containerWidth, containerHeight, cropSize) {
                                val left = (containerWidth - cropSize) / 2f
                                val top = (containerHeight - cropSize) / 2f
                                Rect(left, top, left + cropSize, top + cropSize)
                            }

                            // Gesture Handler for Pinch/Pan
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(0.8f, 5.0f)
                                            offset += pan
                                        }
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // 1. Draw transformed bitmap
                                    val bmpWidth = currentBitmap.width.toFloat()
                                    val bmpHeight = currentBitmap.height.toFloat()

                                    val baseScale = max(cropSize / bmpWidth, cropSize / bmpHeight)
                                    val finalScale = baseScale * scale

                                    val scaledWidth = bmpWidth * finalScale
                                    val scaledHeight = bmpHeight * finalScale

                                    val centerX = containerWidth / 2f + offset.x
                                    val centerY = containerHeight / 2f + offset.y

                                    val dstLeft = centerX - scaledWidth / 2f
                                    val dstTop = centerY - scaledHeight / 2f

                                    // Rotate & translate canvas
                                    drawContext.canvas.save()
                                    drawContext.canvas.translate(centerX, centerY)
                                    drawContext.canvas.rotate(rotationDegrees.toFloat())
                                    drawContext.canvas.translate(-centerX, -centerY)

                                    drawImage(
                                        image = currentBitmap.asImageBitmap(),
                                        dstOffset = IntOffset(dstLeft.roundToInt(), dstTop.roundToInt()),
                                        dstSize = IntSize(scaledWidth.roundToInt(), scaledHeight.roundToInt())
                                    )

                                    drawContext.canvas.restore()

                                    // 2. Draw overlay mask with clear 1:1 circular & square viewport
                                    val maskPath = Path().apply {
                                        fillType = PathFillType.EvenOdd
                                        addRect(Rect(0f, 0f, containerWidth, containerHeight))
                                        addOval(cropRect)
                                    }
                                    drawPath(
                                        path = maskPath,
                                        color = Color.Black.copy(alpha = 0.65f)
                                    )

                                    // 3. Draw viewport border & rule-of-thirds grid
                                    drawOval(
                                        color = Color.White.copy(alpha = 0.9f),
                                        topLeft = cropRect.topLeft,
                                        size = cropRect.size,
                                        style = Stroke(width = 2.dp.toPx())
                                    )

                                    // Sub-grid lines inside crop circle
                                    val step = cropSize / 3f
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.25f),
                                        start = Offset(cropRect.left + step, cropRect.top),
                                        end = Offset(cropRect.left + step, cropRect.bottom),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.25f),
                                        start = Offset(cropRect.left + step * 2, cropRect.top),
                                        end = Offset(cropRect.left + step * 2, cropRect.bottom),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.25f),
                                        start = Offset(cropRect.left, cropRect.top + step),
                                        end = Offset(cropRect.right, cropRect.top + step),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.25f),
                                        start = Offset(cropRect.left, cropRect.top + step * 2),
                                        end = Offset(cropRect.right, cropRect.top + step * 2),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                            }

                            // Floating Reset Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        scale = 1f
                                        offset = Offset.Zero
                                        rotationDegrees = 0
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.crop_photo_reset),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Could not load image for cropping",
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = ButtonShape,
                        enabled = !isCropping
                    ) {
                        Text(
                            text = stringResource(R.string.btn_cancel),
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            if (sourceBitmap != null && !isCropping) {
                                isCropping = true
                                coroutineScope.launch {
                                    val croppedUri = withContext(Dispatchers.IO) {
                                        cropAndSaveAvatar(
                                            context = context,
                                            bitmap = sourceBitmap!!,
                                            scale = scale,
                                            offset = offset,
                                            rotation = rotationDegrees
                                        )
                                    }
                                    isCropping = false
                                    if (croppedUri != null) {
                                        onCropConfirmed(croppedUri)
                                    } else {
                                        onDismiss()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = ButtonShape,
                        enabled = !isCropping && sourceBitmap != null
                    ) {
                        if (isCropping) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.btn_crop_save),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Loads an image from a content URI, scales to a safe working dimension,
 * and fixes EXIF orientation.
 */
private fun loadAndOrientBitmap(context: Context, uri: Uri): Bitmap? {
    val input = context.contentResolver.openInputStream(uri) ?: return null

    // 1. Check EXIF orientation
    var orientation = ExifInterface.ORIENTATION_NORMAL
    try {
        val exifInput = context.contentResolver.openInputStream(uri)
        if (exifInput != null) {
            val exif = ExifInterface(exifInput)
            orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            exifInput.close()
        }
    } catch (_: Exception) {}

    // 2. Decode bounds to sample
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeStream(input, null, options)
    input.close()

    val maxDim = 1200
    var inSampleSize = 1
    while (options.outWidth / (inSampleSize * 2) >= maxDim || options.outHeight / (inSampleSize * 2) >= maxDim) {
        inSampleSize *= 2
    }

    // 3. Decode scaled bitmap
    val decodeStream = context.contentResolver.openInputStream(uri) ?: return null
    val decodeOptions = BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val rawBitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
    decodeStream.close()

    if (rawBitmap == null) return null

    // 4. Apply EXIF rotation if needed
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
    }

    return if (!matrix.isIdentity) {
        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
    } else {
        rawBitmap
    }
}

/**
 * Performs final crop computation, creates a crisp 512x512 avatar,
 * and saves to private internal storage.
 */
private fun cropAndSaveAvatar(
    context: Context,
    bitmap: Bitmap,
    scale: Float,
    offset: Offset,
    rotation: Int
): String? {
    try {
        // Apply manual rotation first if non-zero
        val workingBitmap = if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        val bmpWidth = workingBitmap.width.toFloat()
        val bmpHeight = workingBitmap.height.toFloat()

        // The cropped portion corresponds to a centered square modified by offset and scale
        val minDim = min(bmpWidth, bmpHeight)
        val cropDimension = (minDim / max(scale, 0.5f)).coerceIn(64f, minDim)

        val normalizedOffsetX = -(offset.x / (scale * 200f)) * (minDim / 2f)
        val normalizedOffsetY = -(offset.y / (scale * 200f)) * (minDim / 2f)

        var cropLeft = ((bmpWidth - cropDimension) / 2f + normalizedOffsetX).roundToInt()
        var cropTop = ((bmpHeight - cropDimension) / 2f + normalizedOffsetY).roundToInt()
        val cropSizeInt = cropDimension.roundToInt()

        // Clamp crop boundaries
        cropLeft = cropLeft.coerceIn(0, (bmpWidth.toInt() - cropSizeInt).coerceAtLeast(0))
        cropTop = cropTop.coerceIn(0, (bmpHeight.toInt() - cropSizeInt).coerceAtLeast(0))
        val actualSize = min(cropSizeInt, min(workingBitmap.width - cropLeft, workingBitmap.height - cropTop))

        if (actualSize <= 0) return null

        val croppedSubBitmap = Bitmap.createBitmap(
            workingBitmap,
            cropLeft,
            cropTop,
            actualSize,
            actualSize
        )

        // Scale to standard 512x512 avatar
        val finalAvatar = Bitmap.createScaledBitmap(croppedSubBitmap, 512, 512, true)

        // Save to internal storage
        val profileDir = File(context.filesDir, "profile").apply { if (!exists()) mkdirs() }
        val avatarFile = File(profileDir, "profile_avatar_${System.currentTimeMillis()}.jpg")

        // Clean up old avatar files in dir
        profileDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("profile_avatar_")) {
                file.delete()
            }
        }

        FileOutputStream(avatarFile).use { out ->
            finalAvatar.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        return Uri.fromFile(avatarFile).toString()
    } catch (e: Exception) {
        return null
    }
}
