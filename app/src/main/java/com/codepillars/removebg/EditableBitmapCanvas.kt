package com.codepillars.removebg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import java.io.ByteArrayOutputStream
import java.nio.file.Files.size

@Composable
fun EditableBitmapCanvas(
    bitmap: Bitmap,
    originalBitmap: Bitmap?,
    editMode: EditMode,
    brushSize: Float,
    scale: Float,
    onEditStart: () -> Unit,
    onBitmapChange: (Bitmap) -> Unit
) {
    val hasStartedDrawing = remember { mutableStateOf(false) }
    var brushPosition by remember { mutableStateOf<Offset?>(null) }
    var lastPoint by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        CheckerBackground()

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Editable Image",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                )
                .pointerInput(bitmap, originalBitmap, editMode, brushSize, scale) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            hasStartedDrawing.value = false
                            brushPosition = offset
                            lastPoint = offset
                        },
                        onDragEnd = {
                            hasStartedDrawing.value = false
                            brushPosition = null
                            lastPoint = null
                        },
                        onDragCancel = {
                            brushPosition = null
                            lastPoint = null
                        }
                    ) { change, _ ->

                        val currentPoint = change.position
                        brushPosition = currentPoint

                        if (!hasStartedDrawing.value) {
                            onEditStart()
                            hasStartedDrawing.value = true
                        }

                        val previousPoint = lastPoint ?: currentPoint

                        drawSmoothBrushStroke(
                            targetBitmap = bitmap,
                            originalBitmap = originalBitmap,
                            start = previousPoint,
                            end = currentPoint,
                            canvasWidth = size.width.toFloat(),
                            canvasHeight = size.height.toFloat(),
                            scale = scale,
                            editMode = editMode,
                            brushRadius = brushSize
                        )

                        onBitmapChange(bitmap)
                        lastPoint = currentPoint
                    }
                },
            contentScale = ContentScale.Fit
        )

        brushPosition?.let { pos ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radiusPx = brushSize * (size.width / bitmap.width.toFloat())

                drawCircle(
                    color = if (editMode == EditMode.ERASE) {
                        Color.Red.copy(alpha = 0.5f)
                    } else {
                        Color.Green.copy(alpha = 0.5f)
                    },
                    radius = radiusPx,
                    center = pos,
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

private fun drawSmoothBrushStroke(
    targetBitmap: Bitmap,
    originalBitmap: Bitmap?,
    start: Offset,
    end: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    scale: Float,
    editMode: EditMode,
    brushRadius: Float
) {
    val startMapped = mapTouchToBitmap(
        touch = start,
        bitmapWidth = targetBitmap.width,
        bitmapHeight = targetBitmap.height,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        scale = scale
    )

    val endMapped = mapTouchToBitmap(
        touch = end,
        bitmapWidth = targetBitmap.width,
        bitmapHeight = targetBitmap.height,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        scale = scale
    )

    val dx = endMapped.x - startMapped.x
    val dy = endMapped.y - startMapped.y
    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

    val step = maxOf(1f, brushRadius * 0.35f)
    val steps = maxOf(1, (distance / step).toInt())

    for (i in 0..steps) {
        val t = i.toFloat() / steps.toFloat()
        val x = startMapped.x + dx * t
        val y = startMapped.y + dy * t

        applyBrushAtPoint(
            targetBitmap = targetBitmap,
            originalBitmap = originalBitmap,
            centerX = x,
            centerY = y,
            radius = brushRadius,
            editMode = editMode
        )
    }
}

private fun mapTouchToBitmap(
    touch: Offset,
    bitmapWidth: Int,
    bitmapHeight: Int,
    canvasWidth: Float,
    canvasHeight: Float,
    scale: Float
): Offset {
    val scaledWidth = canvasWidth * scale
    val scaledHeight = canvasHeight * scale

    val extraX = (scaledWidth - canvasWidth) / 2f
    val extraY = (scaledHeight - canvasHeight) / 2f

    val adjustedX = (touch.x + extraX) / scale
    val adjustedY = (touch.y + extraY) / scale

    val imageX = adjustedX * bitmapWidth / canvasWidth
    val imageY = adjustedY * bitmapHeight / canvasHeight

    return Offset(imageX, imageY)
}

private fun applyBrushAtPoint(
    targetBitmap: Bitmap,
    originalBitmap: Bitmap?,
    centerX: Float,
    centerY: Float,
    radius: Float,
    editMode: EditMode
) {
    val left = (centerX - radius).toInt()
    val right = (centerX + radius).toInt()
    val top = (centerY - radius).toInt()
    val bottom = (centerY + radius).toInt()

    for (px in left..right) {
        for (py in top..bottom) {
            if (px in 0 until targetBitmap.width && py in 0 until targetBitmap.height) {
                val dx = px - centerX
                val dy = py - centerY
                val distSq = dx * dx + dy * dy

                if (distSq <= radius * radius) {
                    when (editMode) {
                        EditMode.ERASE -> {
                            targetBitmap.setPixel(px, py, android.graphics.Color.TRANSPARENT)
                        }

                        EditMode.RESTORE -> {
                            val original = originalBitmap ?: return
                            targetBitmap.setPixel(px, py, original.getPixel(px, py))
                        }
                    }
                }
            }
        }
    }
}

fun bitmapToPngBytes(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}

fun uriToBitmapSafe(
    context: Context,
    uri: Uri,
    reqWidth: Int,
    reqHeight: Int
): Bitmap {

    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }

    var inSampleSize = 1
    val (height, width) = options.outHeight to options.outWidth

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while (halfHeight / inSampleSize >= reqHeight &&
            halfWidth / inSampleSize >= reqWidth
        ) {
            inSampleSize *= 2
        }
    }

    val decodeOptions = BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, decodeOptions)
    }

    return bitmap ?: throw Exception("Image decode failed")
}

fun mergeImages(
    background: Bitmap,
    foreground: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): Bitmap {

    val resultWidth = foreground.width
    val resultHeight = foreground.height

    val result = Bitmap.createBitmap(
        resultWidth,
        resultHeight,
        Bitmap.Config.ARGB_8888
    )

    val canvas = android.graphics.Canvas(result)

    // scale background
    val bgScaled = Bitmap.createScaledBitmap(
        background,
        resultWidth,
        resultHeight,
        true
    )

    canvas.drawBitmap(bgScaled, 0f, 0f, null)

    // scale foreground
    val fgWidth = (resultWidth * scale).toInt()
    val fgHeight = (resultHeight * scale).toInt()

    val fgScaled = Bitmap.createScaledBitmap(
        foreground,
        fgWidth,
        fgHeight,
        true
    )

    val left = (resultWidth - fgWidth) / 2f + offsetX
    val top = (resultHeight - fgHeight) / 2f + offsetY

    canvas.drawBitmap(fgScaled, left, top, null)

    return result
}