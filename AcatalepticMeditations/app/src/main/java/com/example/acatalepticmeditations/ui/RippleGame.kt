package com.acataleptic.meditations.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acataleptic.meditations.R
import com.acataleptic.meditations.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import kotlin.random.Random

data class Ripple(
    val center: Offset,
    val color: Color,
    val targetColor: Color,
    val startTime: Long,
    val duration: Int = 5333 // Slowed down ripples to 3/4 speed
)

enum class GameMode(val label: String) {
    CHILL("Chill"), MEDIUM("Medium"), INTENSE("Intense")
}

data class GameDot(
    val id: Int,
    val position: Offset,
    val isVisible: Boolean = false
)

@Composable
fun RippleGame(
    viewModel: JournalViewModel,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var score by remember { mutableIntStateOf(0) }
    val ripples = remember { mutableStateListOf<Ripple>() }
    val today = LocalDate.now()
    val dailyScore by viewModel.getScoreForDate(today).collectAsState(initial = null)
    val density = LocalDensity.current.density
    var isMusicPlaying by remember { mutableStateOf(true) }
    var gameMode by remember { mutableStateOf(GameMode.CHILL) }
    var isPaused by remember { mutableStateOf(false) }
    var pausedTime by remember { mutableLongStateOf(0L) }
    var showModeMenu by remember { mutableStateOf(false) }
    var showSongMenu by remember { mutableStateOf(false) }

    val allSongs = listOf(
        "chasm", "desert_oasis",
        "tranquil_sea", "frozen_caverns", "lavender_skies", 
        "celestial_drift", "late_light_loops"
    )
    var currentSongIndex by remember { mutableIntStateOf(allSongs.indices.random()) }

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
            setOnPreparedListener { if (isMusicPlaying && !isPaused) it.start() }
            prepareAsync()
        }
    }

    DisposableEffect(mediaPlayer) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(isMusicPlaying, isPaused) {
        try {
            if (isMusicPlaying && !isPaused) mediaPlayer?.start() else mediaPlayer?.pause()
        } catch (_: IllegalStateException) { }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "rippleTicker")
    val ticker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(16, easing = LinearEasing)),
        label = "ticker"
    )

    // Use a list of dots to support Intense mode
    val dots = remember { mutableStateListOf<GameDot>() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val maxWidth = constraints.maxWidth.toFloat()
        val maxHeight = constraints.maxHeight.toFloat()
        val topReservedHeight = 160f * density 
        val bottomReservedHeight = 120f * density // Increased further to prevent dots overlapping with the higher pause button

        // Manage dots based on game mode
        LaunchedEffect(gameMode, maxWidth, maxHeight, isPaused) {
            if (isPaused) return@LaunchedEffect
            
            val targetDotCount = if (gameMode == GameMode.INTENSE) 3 else 1
            if (dots.size != targetDotCount) {
                dots.clear()
                repeat(targetDotCount) { i ->
                    val pos = Offset(
                        Random.nextFloat() * (maxWidth - 120f * density) + 60f * density,
                        Random.nextFloat() * (maxHeight - topReservedHeight - bottomReservedHeight - 100f * density) + topReservedHeight + 50f * density
                    )
                    dots.add(GameDot(i, pos, false))
                }
                // Fade in initial dots
                delay(100)
                for (i in dots.indices) {
                    dots[i] = dots[i].copy(isVisible = true)
                }
            }
        }

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(dots.toList(), gameMode, isPaused) { 
                if (isPaused) return@pointerInput
                detectTapGestures { offset ->
                    dots.forEachIndexed { index, dot ->
                        if (dot.isVisible) {
                            val distance = (offset - dot.position).getDistance()
                            if (distance < 65f * density) {
                                // Increment session score
                                score++
                                // CRITICAL FIX: Direct atomic increment in database
                                viewModel.incrementRippleScore(today, score)
                                
                                scope.launch {
                                    val currentPos = dot.position
                                    val startColor = listOf(PrimaryCyber, SecondaryCyber, Color.Yellow, Color.Green, Color.Magenta).random()
                                    val targetColor = listOf(PrimaryCyber, SecondaryCyber, Color.Yellow, Color.Green, Color.Magenta).random()
                                    ripples.add(Ripple(currentPos, startColor, targetColor, System.currentTimeMillis()))

                                    if (gameMode == GameMode.CHILL) {
                                        dots[index] = dot.copy(isVisible = false)
                                        delay(800) // Fade out (250) + Wait (500)
                                        if (!isPaused) {
                                            val newPos = Offset(
                                                Random.nextFloat() * (maxWidth - 120f * density) + 60f * density,
                                                Random.nextFloat() * (maxHeight - topReservedHeight - bottomReservedHeight - 100f * density) + topReservedHeight + 50f * density
                                            )
                                            dots[index] = GameDot(dot.id, newPos, true)
                                        }
                                    } else {
                                        // Medium and Intense respawn immediately
                                        val newPos = Offset(
                                            Random.nextFloat() * (maxWidth - 120f * density) + 60f * density,
                                            Random.nextFloat() * (maxHeight - topReservedHeight - bottomReservedHeight - 100f * density) + topReservedHeight + 50f * density
                                        )
                                        dots[index] = dot.copy(position = newPos)
                                    }
                                }
                            }
                        }
                    }
                }
            }) {
            val _t = ticker
            val currentTime = if (isPaused) pausedTime else System.currentTimeMillis()
            
            if (!isPaused) {
                ripples.removeIf { (currentTime - it.startTime) > it.duration }
            }
            
            ripples.forEach { ripple ->
                val progress = (currentTime - ripple.startTime).toFloat() / ripple.duration
                for (i in 0 until 3) {
                    val waveDelay = i * 0.15f
                    val waveProgress = (progress - waveDelay).coerceIn(0f, 1f)
                    if (waveProgress > 0f) {
                        val radius = waveProgress * 600f * density
                        val alpha = (1f - waveProgress) * (1f - i * 0.2f)
                        val currentColor = lerp(ripple.color, ripple.targetColor, waveProgress)
                        drawCircle(
                            color = currentColor.copy(alpha = alpha),
                            radius = radius,
                            center = ripple.center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = (8.dp.toPx() * (1f - i * 0.2f)).coerceAtLeast(1f))
                        )
                    }
                }
            }
        }

        // Render all dots
        dots.forEach { dot ->
            val dotAlpha by animateFloatAsState(
                targetValue = if (dot.isVisible && !isPaused) 1f else 0f,
                animationSpec = tween(durationMillis = 250),
                label = "dotAlpha_${dot.id}"
            )
            Box(
                modifier = Modifier
                    .offset(
                        x = (dot.position.x / density).dp - 25.dp,
                        y = (dot.position.y / density).dp - 25.dp
                    )
                    .graphicsLayer { alpha = dotAlpha }
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(PrimaryCyber)
            )
        }

        // Mode Menu and Pause Controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp, end = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                // Mode Dropdown above pause button
                Box {
                    Button(
                        onClick = { showModeMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, PrimaryCyber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Mode: ${gameMode.label}", color = TextColor)
                    }
                    DropdownMenu(
                        expanded = showModeMenu,
                        onDismissRequest = { showModeMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        GameMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label, color = TextColor) },
                                onClick = {
                                    gameMode = mode
                                    showModeMenu = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                IconButton(
                    onClick = { 
                        if (!isPaused) {
                            pausedTime = System.currentTimeMillis()
                            isPaused = true
                        } else {
                            // Adjust ripple start times so they don't skip ahead when unpausing
                            val pauseDuration = System.currentTimeMillis() - pausedTime
                            ripples.indices.forEach { i ->
                                ripples[i] = ripples[i].copy(startTime = ripples[i].startTime + pauseDuration)
                            }
                            isPaused = false
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(DarkSurface, CircleShape)
                        .border(BorderStroke(1.dp, PrimaryCyber), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = PrimaryCyber,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Top UI Overlay
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Title and Score on the left
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                horizontalAlignment = Alignment.Start
            ) {
                Text(text = "Ripple Game", color = PrimaryCyber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Score: $score", color = TextColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(text = "Daily High: ${dailyScore?.rippleHighScore ?: 0}", color = TextColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(text = "Daily Total: ${dailyScore?.rippleTotalScore ?: 0}", color = TextColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            // Controls on the right
            Row(modifier = Modifier.align(Alignment.TopEnd), verticalAlignment = Alignment.CenterVertically) {
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
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Exit", tint = TextColor)
                }
            }
        }
    }
}
