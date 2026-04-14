package com.codepillars.removebg


import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun AddBackgroundScreen(
    vm: MainViewModel
) {
    val context = LocalContext.current
    val editedBitmap by vm.editedBitmap.collectAsState()

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    var backgroundUri by remember { mutableStateOf<Uri?>(null) }

    val backgroundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        backgroundUri = uri
    }

    if (editedBitmap == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No edited image found")
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
            text = "Add Background",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { backgroundPickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                if (backgroundUri == null) "Choose Background Image"
                else "Change Background Image"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { scale = (scale + 0.1f).coerceAtMost(3f) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_zoom_in_24),
                    contentDescription = "Zoom In"
                )
            }

            OutlinedButton(
                onClick = { scale = (scale - 0.1f).coerceAtLeast(0.3f) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_zoom_out_24),
                    contentDescription = "Zoom Out"
                )
            }

            OutlinedButton(
                onClick = {
                    offsetX = 0f
                    offsetY = 0f
                    scale = 1f
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_reset_image_24),
                    contentDescription = "Reset"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
//                .background(Color.LightGray, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (backgroundUri != null) {
                backgroundUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                CheckerBackground()
            }

            editedBitmap?.let { fg ->
                Image(
                    bitmap = fg.asImageBitmap(),
                    contentDescription = "Edited Foreground",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                val foreground = editedBitmap ?: return@OutlinedButton

                val finalBitmap = if (backgroundUri != null) {
                    val background = uriToBitmapSafe(
                        context,
                        backgroundUri!!,
                        foreground.width,
                        foreground.height
                    )
                    mergeImages(background, foreground, scale, offsetX, offsetY)
                } else {
                    foreground
                }

                val bytes = bitmapToPngBytes(finalBitmap)
                val fileName = "final_image_${System.currentTimeMillis()}.png"

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
            modifier = Modifier.align(Alignment.End),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.outline_download_24),
                contentDescription = "Save"

            )
        }
    }
}