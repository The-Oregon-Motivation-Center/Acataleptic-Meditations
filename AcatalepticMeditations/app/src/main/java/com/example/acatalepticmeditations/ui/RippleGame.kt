package com.acataleptic.meditations.ui

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
import kotlin.random.Random

data class Ripple(
    val center: Offset,
    val color: Color,
    val targetColor: Color,
    val startTime: Long,
    val duration: Int = 5333 // Slowed down ripples to 3/4 speed
)

enum class GameMode {
    CHILL, MEDIUM, INTENSE
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

    // Use a list of dots to support Intense mode
    val dots = remember { mutableStateListOf<GameDot>() }

    // Music Setup
    val mediaPlayer = remember {
        val songNames = listOf("chasm", "desert_oasis", "tranquil_sea", "frozen_caverns")
        val songName = songNames.random()
        val resId = context.resources.getIdentifier(songName, "raw", context.packageName)
        if (resId != 0) {
            MediaPlayer.create(context, resId).apply { isLooping = true }
        } else {
            Log.e("RippleGame", "Could not find music resource: $songName")
            null
        }
    }

    DisposableEffect(Unit) {
        if (isMusicPlaying) mediaPlayer?.start()
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(isMusicPlaying, isPaused) {
        if (isMusicPlaying && !isPaused) mediaPlayer?.start() else mediaPlayer?.pause()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "rippleTicker")
    val ticker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(16, easing = LinearEasing)),
        label = "ticker"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val maxWidth = constraints.maxWidth.toFloat()
        val maxHeight = constraints.maxHeight.toFloat()
        val topReservedHeight = 160f * density 
        val bottomReservedHeight = 80f * density // Increased to accommodate pause button

        // Manage dots based on game mode
        LaunchedEffect(gameMode, maxWidth, maxHeight, isPaused) {
            if (isPaused) return@LaunchedEffect
            
            if (dots.isEmpty()) {
                val targetDotCount = if (gameMode == GameMode.INTENSE) 3 else 1
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
                                viewModel.incrementDailyScore(today, score)
                                
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
                targetValue = if (dot.isVisible) 1f else 0f,
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

        // Pause Button at the bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
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
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, PrimaryCyber),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp).width(120.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = PrimaryCyber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isPaused) "Resume" else "Pause", color = TextColor, fontSize = 16.sp)
                }
            }
        }

        // Top UI Overlay
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Mode buttons on the left
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameModeButton("Chill", gameMode == GameMode.CHILL && !isPaused) { if(!isPaused) gameMode = GameMode.CHILL }
                GameModeButton("Medium", gameMode == GameMode.MEDIUM && !isPaused) { if(!isPaused) gameMode = GameMode.MEDIUM }
                GameModeButton("Intense", gameMode == GameMode.INTENSE && !isPaused) { if(!isPaused) gameMode = GameMode.INTENSE }
            }

            // Score in the center
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Score: $score", color = TextColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                dailyScore?.let {
                    Text(text = "Daily High: ${it.highScore}", color = PrimaryCyber.copy(alpha = 0.7f), fontSize = 14.sp)
                    Text(text = "Daily Total: ${it.totalScore}", color = PrimaryCyber.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }

            // Controls on the right
            Row(modifier = Modifier.align(Alignment.TopEnd)) {
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

@Composable
fun GameModeButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryCyber else DarkSurface,
            contentColor = if (isSelected) Color.Black else TextColor
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (!isSelected) BorderStroke(1.dp, PrimaryCyber.copy(alpha = 0.5f)) else null
    ) {
        Text(text = text, fontSize = 12.sp)
    }
}
