package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun VisualColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var hsv by remember {
        val hsvArr = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsvArr)
        mutableStateOf(hsvArr)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Saturation-Value Box
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        val y = change.position.y.coerceIn(0f, size.height.toFloat())
                        val s = x / size.width.toFloat()
                        val v = 1f - (y / size.height.toFloat())
                        hsv = floatArrayOf(hsv[0], s, v)
                        onColorChanged(Color(android.graphics.Color.HSVToColor(hsv)))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val s = offset.x.coerceIn(0f, size.width.toFloat()) / size.width.toFloat()
                        val v = 1f - (offset.y.coerceIn(0f, size.height.toFloat()) / size.height.toFloat())
                        hsv = floatArrayOf(hsv[0], s, v)
                        onColorChanged(Color(android.graphics.Color.HSVToColor(hsv)))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val saturationGradient = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.hsv(hsv[0], 1f, 1f))
                )
                val valueGradient = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
                drawRect(saturationGradient)
                drawRect(valueGradient)

                // Selector
                val selectorX = hsv[1] * size.width
                val selectorY = (1f - hsv[2]) * size.height
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(selectorX, selectorY),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.Black,
                    radius = 7.dp.toPx(),
                    center = Offset(selectorX, selectorY),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hue Slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        val h = (x / size.width.toFloat()) * 360f
                        hsv = floatArrayOf(h, hsv[1], hsv[2])
                        onColorChanged(Color(android.graphics.Color.HSVToColor(hsv)))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val h = (offset.x.coerceIn(0f, size.width.toFloat()) / size.width.toFloat()) * 360f
                        hsv = floatArrayOf(h, hsv[1], hsv[2])
                        onColorChanged(Color(android.graphics.Color.HSVToColor(hsv)))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hueGradient = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    )
                )
                drawRect(hueGradient)

                // Selector
                val selectorX = (hsv[0] / 360f) * size.width
                drawRect(
                    color = Color.White,
                    topLeft = Offset(selectorX - 2.dp.toPx(), 0f),
                    size = Size(4.dp.toPx(), size.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            VisualColorPicker(
                initialColor = initialColor,
                onColorChanged = { selectedColor = it },
                modifier = Modifier.padding(top = 8.dp)
            )
        },
        confirmButton = {
            Button(onClick = { 
                onColorSelected(selectedColor)
                onDismiss()
            }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
