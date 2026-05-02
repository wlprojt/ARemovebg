package com.codepillars.removebg



import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class EditMode {
    ERASE, RESTORE
}

@Composable
fun ResultScreen(
    vm: MainViewModel,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val resultBytes by vm.resultBytes.collectAsState()
    var foregroundScale by remember { mutableStateOf(1f) }

    var editMode by remember { mutableStateOf(EditMode.ERASE) }
    var brushSize by remember { mutableStateOf(30f) }

//    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
//    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val vmOriginalBitmap by vm.originalBitmap.collectAsState()
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val undoStack = remember { mutableStateListOf<Bitmap>() }
    val redoStack = remember { mutableStateListOf<Bitmap>() }

    val restoreSource = remember(vmOriginalBitmap, editedBitmap) {
        if (vmOriginalBitmap != null && editedBitmap != null) {
            Bitmap.createScaledBitmap(
                vmOriginalBitmap!!,
                editedBitmap!!.width,
                editedBitmap!!.height,
                true
            )
        } else {
            null
        }
    }

    LaunchedEffect(resultBytes) {
        resultBytes?.let { bytes ->
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?.copy(Bitmap.Config.ARGB_8888, true)

            if (bmp != null && editedBitmap == null) {
                editedBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true)
                undoStack.clear()
                redoStack.clear()
            }
        }
    }

    fun saveStateForUndo() {
        editedBitmap?.let {
            undoStack.add(it.copy(Bitmap.Config.ARGB_8888, true))
            if (undoStack.size > 20) undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty() && editedBitmap != null) {
            redoStack.add(editedBitmap!!.copy(Bitmap.Config.ARGB_8888, true))
            editedBitmap = undoStack.removeAt(undoStack.lastIndex)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty() && editedBitmap != null) {
            undoStack.add(editedBitmap!!.copy(Bitmap.Config.ARGB_8888, true))
            editedBitmap = redoStack.removeAt(redoStack.lastIndex)
        }
    }

    if (resultBytes == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No result found")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Background Edit",
            style = MaterialTheme.typography.headlineMedium.copy(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF023890), // Blue
                        Color(0xFF00BCD4)  // Cyan
                    )
                )
            ),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        editedBitmap?.let { bitmap ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { editMode = EditMode.ERASE },
                    modifier = Modifier
                        .weight(1f)
                    .clip(RoundedCornerShape(25.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF023890), // Blue
                                Color(0xFF00BCD4)  // Cyan
                            )
                        )
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(
                        false
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_ink_eraser_24),
                        contentDescription = "Erase"
                    )
                }

                OutlinedButton(
                    onClick = { editMode = EditMode.RESTORE },
                    modifier = Modifier
                        .weight(1f)
                    .clip(RoundedCornerShape(25.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF023890), // Blue
                                Color(0xFF00BCD4)  // Cyan
                            )
                        )
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(
                        false
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_settings_backup_restore_24),
                        contentDescription = "Restore"
                    )
                }

                OutlinedButton(
                    onClick = {
                        vm.setEditedBitmap(bitmap)
                        onNext()
                    },
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(25.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF023890), // Blue
                                    Color(0xFF00BCD4)  // Cyan
                                )
                            )
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,

                    ),
                    border = ButtonDefaults.outlinedButtonBorder(
                        false
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_background_replace_24),
                        contentDescription = "Add Background"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { undo() },
                    enabled = undoStack.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(25.dp),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFF023890)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF023890)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_undo_24),
                        contentDescription = "Undo"

                    )
                }

                OutlinedButton(
                    onClick = { redo() },
                    enabled = redoStack.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(25.dp),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFF023890)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF023890)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_redo_24),
                        contentDescription = "Redo"
                    )
                }

                OutlinedButton(
                    onClick = {
                        foregroundScale = (foregroundScale + 0.1f).coerceAtMost(1.6f)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(25.dp),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFF023890)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF023890)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_zoom_in_24),
                        contentDescription = "Zoom In"
                    )
                }

                OutlinedButton(
                    onClick = {
                        foregroundScale = (foregroundScale - 0.1f).coerceAtLeast(0.6f)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(25.dp),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFF023890)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF023890)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_zoom_out_24),
                        contentDescription = "Zoom Out"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Brush Size: ${brushSize.toInt()}",
                color = Color(0xFF023890)
            )

            Slider(
                value = brushSize,
                onValueChange = { brushSize = it },
                valueRange = 10f..80f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00BCD4),
                    activeTrackColor = Color(0xFF00BCD4),
                    inactiveTrackColor = Color(0xFF023890)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            EditableBitmapCanvas(
                bitmap = editedBitmap!!,
                originalBitmap = restoreSource,
                editMode = editMode,
                brushSize = brushSize,
                scale = foregroundScale,
                onEditStart = { saveStateForUndo() },
                onBitmapChange = { updated ->
                    editedBitmap = updated
                    vm.setEditedBitmap(updated)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    val bytes = bitmapToPngBytes(bitmap)
                    val fileName = "bg_removed_${System.currentTimeMillis()}.png"

                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/BackgroundRemover"
                        )
                    }

                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )

                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(bytes)
                        }
                        Toast.makeText(context, "Image saved", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF023890), // Blue
                                Color(0xFF00BCD4)  // Cyan
                            )
                        )
                    ),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder(false)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_download_24),
                    contentDescription = "Save"

                )
            }
        } ?: Box(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF00BCD4), // Green
                strokeWidth = 4.dp
            )
        }
    }
}