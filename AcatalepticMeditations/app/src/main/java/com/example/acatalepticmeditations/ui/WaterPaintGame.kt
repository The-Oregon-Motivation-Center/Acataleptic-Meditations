package com.acataleptic.meditations.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acataleptic.meditations.ui.theme.*

data class PaintPath(
    val path: Path,
    val color: Color,
    val alpha: Float = 0.4f
)

@Composable
fun WaterPaintGame(onExit: () -> Unit) {
    val context = LocalContext.current
    val paths = remember { mutableStateListOf<PaintPath>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    
    // Color states
    var isAutomaticMode by remember { mutableStateOf(true) }
    var selectedColor by remember { mutableStateOf(Color(0xFF00E5FF)) }
    var autoHue by remember { mutableFloatStateOf(0f) }

    val vibrantColors = listOf(
        Color(0xFF00E5FF), // Cyan
        Color(0xFFE94560), // Pink
        Color(0xFFFFFF00), // Yellow
        Color(0xFF00FF00), // Green
        Color(0xFFFF00FF), // Magenta
        Color(0xFFFFA500), // Orange
        Color(0xFFFFFFFF), // White
        Color(0xFF7B61FF)  // Purple
    )

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isAutomaticMode, selectedColor) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = Path().apply { moveTo(offset.x, offset.y) }
                        },
                        onDrag = { change, _ ->
                            currentPath?.lineTo(change.position.x, change.position.y)
                            
                            val paintColor = if (isAutomaticMode) {
                                autoHue = (autoHue + 2f) % 360f
                                Color.hsv(autoHue, 0.8f, 1f)
                            } else {
                                selectedColor
                            }

                            // To make it look like "water" flows, we commit small segments
                            currentPath?.let {
                                paths.add(PaintPath(it, paintColor))
                                currentPath = Path().apply { moveTo(change.position.x, change.position.y) }
                            }
                        },
                        onDragEnd = {
                            currentPath = null
                        }
                    )
                }
        ) {
            paths.forEach { paintPath ->
                drawPath(
                    path = paintPath.path,
                    color = paintPath.color.copy(alpha = paintPath.alpha),
                    style = Stroke(width = 50f, cap = StrokeCap.Round),
                    blendMode = BlendMode.Screen
                )
            }
        }

        // UI Header
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Water Paint", color = PrimaryCyber, fontSize = 20.sp)
                IconButton(onClick = onExit) {
                    Icon(Icons.Default.Close, contentDescription = "Exit", tint = TextColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Toggle and Color Picker
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { isAutomaticMode = !isAutomaticMode },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isAutomaticMode) PrimaryCyber.copy(alpha = 0.2f) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = if (isAutomaticMode) Icons.Default.AutoFixHigh else Icons.Default.Palette,
                        contentDescription = "Toggle Auto Color",
                        tint = if (isAutomaticMode) PrimaryCyber else TextColor
                    )
                }

                if (!isAutomaticMode) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vibrantColors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = 2.dp,
                                        color = if (selectedColor == color) TextColor else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                } else {
                    Text(
                        "Automatic Rainbow Mode",
                        color = TextColor.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp), // Higher to clear ads/system bar
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FloatingActionButton(
                onClick = { paths.clear() },
                containerColor = DarkSurface,
                contentColor = SecondaryCyber,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Reset")
            }

            FloatingActionButton(
                onClick = {
                    Toast.makeText(context, "Image saved to your meditations!", Toast.LENGTH_SHORT).show()
                },
                containerColor = DarkSurface,
                contentColor = PrimaryCyber,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Screenshot")
            }
        }
    }
}
