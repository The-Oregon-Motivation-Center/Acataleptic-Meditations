package com.acataleptic.meditations.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acataleptic.meditations.ui.theme.*
import java.io.OutputStream
import java.util.Locale

data class PaintPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val alpha: Float = 0.4f
)

enum class PaintSize(val label: String, val width: Float) {
    SMALL("Small", 16.66f),
    MEDIUM("Medium", 33.33f),
    LARGE("Large", 50f)
}

@Composable
fun WaterPaintGame(onExit: () -> Unit) {
    val context = LocalContext.current
    val paths = remember { mutableStateListOf<PaintPath>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    
    // Color states
    var isAutomaticMode by remember { mutableStateOf(true) }
    var selectedColor by remember { mutableStateOf(Color(0xFF00E5FF)) }
    var autoHue by remember { mutableFloatStateOf(0f) }
    var isMusicPlaying by remember { mutableStateOf(true) }
    var paintSize by remember { mutableStateOf(PaintSize.LARGE) }
    var showSizeMenu by remember { mutableStateOf(false) }
    var showSongMenu by remember { mutableStateOf(false) }

    val allSongs = listOf(
        "chasm", "desert_oasis",
        "tranquil_sea", "frozen_caverns", "lavender_skies", 
        "celestial_drift", "late_light_loops"
    )
    var currentSongIndex by remember { mutableIntStateOf(allSongs.indices.random()) }

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

    // Music Setup
    val mediaPlayer = remember(currentSongIndex) {
        val songName = allSongs[currentSongIndex]
        val ext = mapOf("late_light_loops" to "wav", "lavender_skies" to "wav", "celestial_drift" to "wav")[songName] ?: "mp3"
        val url = "https://the-oregon-motivation-center.github.io/Acatmedassets/$songName.$ext"
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(url)
            isLooping = true
            setOnPreparedListener { if (isMusicPlaying) it.start() }
            prepareAsync()
        }
    }

    DisposableEffect(mediaPlayer) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(isMusicPlaying) {
        try {
            if (isMusicPlaying) mediaPlayer?.start() else mediaPlayer?.pause()
        } catch (_: IllegalStateException) { }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        val maxWidth = constraints.maxWidth.toFloat()
        val maxHeight = constraints.maxHeight.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isAutomaticMode, selectedColor, paintSize) {
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

                            currentPath?.let {
                                paths.add(PaintPath(it, paintColor, paintSize.width))
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
                    style = Stroke(width = paintPath.strokeWidth, cap = StrokeCap.Round),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(onClick = { showSongMenu = true }) {
                            Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Change Song", tint = TextColor)
                        }
                        DropdownMenu(
                            expanded = showSongMenu,
                            onDismissRequest = { showSongMenu = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            allSongs.forEachIndexed { index, song ->
                                DropdownMenuItem(
                                    text = { 
                                        val displayName = song.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
                                        Text(displayName, color = if (currentSongIndex == index) PrimaryCyber else TextColor) 
                                    },
                                    onClick = {
                                        currentSongIndex = index
                                        showSongMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { isMusicPlaying = !isMusicPlaying }) {
                        Icon(
                            imageVector = if (isMusicPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = "Toggle Music",
                            tint = TextColor
                        )
                    }
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, contentDescription = "Exit", tint = TextColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Toggle, Size and Color Picker
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

                Box {
                    IconButton(onClick = { showSizeMenu = true }) {
                        Icon(Icons.Default.Brush, contentDescription = "Paint Size", tint = TextColor)
                    }
                    DropdownMenu(
                        expanded = showSizeMenu,
                        onDismissRequest = { showSizeMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        PaintSize.entries.forEach { size ->
                            DropdownMenuItem(
                                text = { Text(size.label, color = if (paintSize == size) PrimaryCyber else TextColor) },
                                onClick = {
                                    paintSize = size
                                    showSizeMenu = false
                                }
                            )
                        }
                    }
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
                .padding(bottom = 64.dp),
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
                    saveBitmapToGallery(context, paths.toList(), maxWidth.toInt(), maxHeight.toInt())
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

private fun saveBitmapToGallery(context: android.content.Context, paths: List<PaintPath>, width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(DarkBackground.toArgb())

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }

        paths.forEach { paintPath ->
            paint.strokeWidth = paintPath.strokeWidth
            paint.color = paintPath.color.copy(alpha = paintPath.alpha).toArgb()
            canvas.drawPath(paintPath.path.asAndroidPath(), paint)
        }

        val filename = "Acataleptic_Paint_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Acataleptic")
            }
        }

        val contentResolver = context.contentResolver
        val imageUri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let {
            val fos: OutputStream? = contentResolver.openOutputStream(it)
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()
                Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, "Failed to create MediaStore entry", Toast.LENGTH_SHORT).show()
        }
        
        bitmap.recycle()
    } catch (e: Exception) {
        Log.e("WaterPaint", "Error saving image", e)
        Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
