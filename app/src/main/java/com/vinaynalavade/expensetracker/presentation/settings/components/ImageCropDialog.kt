package com.vinaynalavade.expensetracker.presentation.settings.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.presentation.theme.BrandGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Premium, production-grade Profile Picture Cropping Experience.
 * Provides intuitive pinch-to-zoom, fluid dragging with strict boundary clamping (zero empty areas),
 * 90-degree step rotation, reset capabilities, and guaranteed full-screen inset compliance.
 */
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

    var containerSize by remember { mutableStateOf(Offset.Zero) }
    var calculatedCropSize by remember { mutableFloatStateOf(0f) }

    // Load and normalize bitmap orientation
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                val loaded = loadAndOrientBitmap(context, imageUri)
                sourceBitmap = loaded
                isLoading = false
            } catch (_: Exception) {
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
            decorFitsSystemWindows = false,
            dismissOnBackPress = !isCropping,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F0F10)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 1. Top Navigation Bar (Pinned)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isCropping,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.btn_cancel),
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.crop_photo_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.crop_photo_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            rotationDegrees = (rotationDegrees + 90) % 360
                            scale = 1f
                            offset = Offset.Zero
                        },
                        enabled = !isCropping && sourceBitmap != null,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = stringResource(R.string.crop_photo_rotate),
                            tint = Color.White
                        )
                    }
                }

                // 2. Interactive Crop Viewport Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = BrandGreen,
                            strokeWidth = 3.dp
                        )
                    } else if (sourceBitmap != null) {
                        val currentBitmap = sourceBitmap!!

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            val containerWidth = constraints.maxWidth.toFloat()
                            val containerHeight = constraints.maxHeight.toFloat()
                            val cropSize = min(containerWidth, containerHeight) * 0.82f

                            LaunchedEffect(containerWidth, containerHeight, cropSize) {
                                containerSize = Offset(containerWidth, containerHeight)
                                calculatedCropSize = cropSize
                            }

                            val cropRect = remember(containerWidth, containerHeight, cropSize) {
                                val left = (containerWidth - cropSize) / 2f
                                val top = (containerHeight - cropSize) / 2f
                                Rect(left, top, left + cropSize, top + cropSize)
                            }

                            // Calculate effective dimensions based on rotation
                            val isRotated90 = (rotationDegrees % 180 != 0)
                            val effBmpWidth = if (isRotated90) currentBitmap.height.toFloat() else currentBitmap.width.toFloat()
                            val effBmpHeight = if (isRotated90) currentBitmap.width.toFloat() else currentBitmap.height.toFloat()

                            val baseScale = max(cropSize / effBmpWidth, cropSize / effBmpHeight)

                            // Gesture Handler for Fluid Pan & Pinch-to-Zoom with Clamping
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(rotationDegrees, currentBitmap) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val newScale = (scale * zoom).coerceIn(1.0f, 5.0f)
                                            scale = newScale

                                            val scaledWidth = effBmpWidth * baseScale * newScale
                                            val scaledHeight = effBmpHeight * baseScale * newScale

                                            val maxOffsetX = max(0f, (scaledWidth - cropSize) / 2f)
                                            val maxOffsetY = max(0f, (scaledHeight - cropSize) / 2f)

                                            val newOffsetX = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                            val newOffsetY = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                            offset = Offset(newOffsetX, newOffsetY)
                                        }
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val finalScale = baseScale * scale
                                    val scaledDrawWidth = currentBitmap.width * finalScale
                                    val scaledDrawHeight = currentBitmap.height * finalScale

                                    val centerX = containerWidth / 2f + offset.x
                                    val centerY = containerHeight / 2f + offset.y

                                    val dstLeft = centerX - scaledDrawWidth / 2f
                                    val dstTop = centerY - scaledDrawHeight / 2f

                                    // A. Draw rotated and translated source bitmap
                                    drawContext.canvas.save()
                                    drawContext.canvas.translate(centerX, centerY)
                                    drawContext.canvas.rotate(rotationDegrees.toFloat())
                                    drawContext.canvas.translate(-centerX, -centerY)

                                    drawImage(
                                        image = currentBitmap.asImageBitmap(),
                                        dstOffset = IntOffset(dstLeft.roundToInt(), dstTop.roundToInt()),
                                        dstSize = IntSize(scaledDrawWidth.roundToInt(), scaledDrawHeight.roundToInt())
                                    )

                                    drawContext.canvas.restore()

                                    // B. Draw Dimmed Mask with 1:1 Circular Cutout Viewport
                                    val maskPath = Path().apply {
                                        fillType = PathFillType.EvenOdd
                                        addRect(Rect(0f, 0f, containerWidth, containerHeight))
                                        addOval(cropRect)
                                    }
                                    drawPath(
                                        path = maskPath,
                                        color = Color(0xD9000000)
                                    )

                                    // C. Draw Viewport Circular Border
                                    drawOval(
                                        color = Color.White,
                                        topLeft = cropRect.topLeft,
                                        size = cropRect.size,
                                        style = Stroke(width = 2.dp.toPx())
                                    )

                                    // D. Subtle Rule-of-Thirds Inner Grid
                                    val step = cropSize / 3f
                                    val gridColor = Color.White.copy(alpha = 0.22f)
                                    val gridStroke = 1.dp.toPx()

                                    drawLine(
                                        color = gridColor,
                                        start = Offset(cropRect.left + step, cropRect.top),
                                        end = Offset(cropRect.left + step, cropRect.bottom),
                                        strokeWidth = gridStroke
                                    )
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(cropRect.left + step * 2, cropRect.top),
                                        end = Offset(cropRect.left + step * 2, cropRect.bottom),
                                        strokeWidth = gridStroke
                                    )
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(cropRect.left, cropRect.top + step),
                                        end = Offset(cropRect.right, cropRect.top + step),
                                        strokeWidth = gridStroke
                                    )
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(cropRect.left, cropRect.top + step * 2),
                                        end = Offset(cropRect.right, cropRect.top + step * 2),
                                        strokeWidth = gridStroke
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Could not load image for cropping",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // 3. Bottom Pinned Toolbar & Action Controls
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = Color(0xFF18181B),
                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Quick Utility Tools Row (Reset & Rotate)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF27272A),
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            scale = 1f
                                            offset = Offset.Zero
                                            rotationDegrees = 0
                                        },
                                        enabled = !isCropping,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = stringResource(R.string.crop_photo_reset),
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.crop_photo_reset),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Primary Action Buttons Row (Cancel & Save Photo)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                enabled = !isCropping
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_cancel),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }

                            Button(
                                onClick = {
                                    if (sourceBitmap != null && !isCropping && calculatedCropSize > 0f) {
                                        isCropping = true
                                        coroutineScope.launch {
                                            val croppedUri = withContext(Dispatchers.IO) {
                                                cropAndSaveAvatar(
                                                    context = context,
                                                    bitmap = sourceBitmap!!,
                                                    scale = scale,
                                                    offset = offset,
                                                    rotation = rotationDegrees,
                                                    cropSize = calculatedCropSize
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
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandGreen,
                                    contentColor = Color.White
                                ),
                                enabled = !isCropping && sourceBitmap != null
                            ) {
                                if (isCropping) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.btn_crop_save),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
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

    val maxDim = 1600
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
 * Performs exact mathematical crop computation matching the on-screen circular viewport,
 * creates a crisp 512x512 avatar, and saves to private internal storage.
 */
private fun cropAndSaveAvatar(
    context: Context,
    bitmap: Bitmap,
    scale: Float,
    offset: Offset,
    rotation: Int,
    cropSize: Float
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

        // Base scale matches exactly how the image was rendered on screen inside the crop box
        val baseScale = max(cropSize / bmpWidth, cropSize / bmpHeight)
        val finalScale = baseScale * scale

        val scaledWidth = bmpWidth * finalScale
        val scaledHeight = bmpHeight * finalScale

        // Calculate screen-relative top-left coordinates of the crop area on the image
        val relCropLeftOnScreen = -offset.x + (scaledWidth - cropSize) / 2f
        val relCropTopOnScreen = -offset.y + (scaledHeight - cropSize) / 2f

        // Map screen pixels directly to source bitmap coordinates
        val cropLeft = (relCropLeftOnScreen / finalScale).roundToInt().coerceIn(0, (bmpWidth.toInt() - 1).coerceAtLeast(0))
        val cropTop = (relCropTopOnScreen / finalScale).roundToInt().coerceIn(0, (bmpHeight.toInt() - 1).coerceAtLeast(0))
        val cropDimension = (cropSize / finalScale).roundToInt().coerceAtLeast(1)

        val safeCropWidth = min(cropDimension, workingBitmap.width - cropLeft)
        val safeCropHeight = min(cropDimension, workingBitmap.height - cropTop)
        val cropSquareSize = min(safeCropWidth, safeCropHeight)

        if (cropSquareSize <= 0) return null

        val croppedSubBitmap = Bitmap.createBitmap(
            workingBitmap,
            cropLeft,
            cropTop,
            cropSquareSize,
            cropSquareSize
        )

        // Scale to standard high-resolution 512x512 avatar
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
            finalAvatar.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        return Uri.fromFile(avatarFile).toString()
    } catch (_: Exception) {
        return null
    }
}
