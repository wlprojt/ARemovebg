package com.codepillars.removebg




import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onNavigateResult: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071A3A)),
        contentAlignment = Alignment.Center
    ) {
        CheckerBackground()

        BgRemoverScreen(
            vm = vm,
            onNavigateResult = {
                onNavigateResult()
            }
        )
    }
}


@Composable
fun CheckerBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellSize = 18.dp.toPx()

        for (x in 0..(size.width / cellSize).toInt()) {
            for (y in 0..(size.height / cellSize).toInt()) {
                val color = if ((x + y) % 2 == 0) {
                    Color.White
                } else {
                    Color.LightGray
                }

                drawRect(
                    color = color,
                    topLeft = Offset(x * cellSize, y * cellSize),
                    size = Size(cellSize, cellSize)
                )
            }
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color.Transparent
                )
            )
        )
    }
}