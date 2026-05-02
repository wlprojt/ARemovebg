package com.codepillars.removebg



import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun BgRemoverScreen(
    vm: MainViewModel,
    onNavigateResult: () -> Unit
) {
    val selectedImage by vm.selectedImage.collectAsState()
    val resultBytes by vm.resultBytes.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val original = uriToBitmap(context, uri)
                .copy(Bitmap.Config.ARGB_8888, true)

            vm.setOriginalBitmap(original)
            vm.setSelectedImage(uri)
        }
    }

    LaunchedEffect(resultBytes) {
        if (resultBytes != null) {
            onNavigateResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

                Text(
                    text = "Background Remover",
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

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier
                .size(80.dp)
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
            border = BorderStroke(
                2.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF023890), // Blue
                        Color(0xFF00BCD4)  // Cyan
                    )
                )
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.outline_add_a_photo_24),
                contentDescription = "Select Image",
                modifier = Modifier.size(32.dp) // ✅ fixed size
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedImage?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "Selected Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp)
                    ),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    vm.removeBackground()
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(25.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF023890), // Blue
                                Color(0xFF00BCD4)  // Cyan
                            )
                        )
                    ),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Text("Remove Background")
            }
        }

        if (loading) {
            Spacer(modifier = Modifier.height(20.dp))
            CircularProgressIndicator(
                color = Color(0xFF00BCD4), // Green
                strokeWidth = 4.dp
            )
        }

        error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}