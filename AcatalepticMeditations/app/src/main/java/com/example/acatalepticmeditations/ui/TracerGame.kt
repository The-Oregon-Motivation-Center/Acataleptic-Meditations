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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acataleptic.meditations.ui.theme.*
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

data class Spark(
    val position: Offset,
    val target: Offset,
    val color: Color,
    val startTime: Long,
    val duration: Int = 1500
)

@Composable
fun TracerGame(
    viewModel: JournalViewModel,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var score by remember { mutableIntStateOf(0) }
    val ripples = remember { mutableStateListOf<Ripple>() }
    val sparks = remember { mutableStateListOf<Spark>() }
    val targets = remember { mutableStateListOf<Pair<Offset, Color>>() }
    
    val today = LocalDate.now()
    val dailyScore by viewModel.getScoreForDate(today).collectAsState(initial = null)
    val density = LocalDensity.current.density
    var isMusicPlaying by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }
    var pausedTime by remember { mutableLongStateOf(0L) }

    val gameColors = listOf(
        PrimaryCyber, SecondaryCyber, Color.Yellow, Color.Green, 
        Color.Magenta, Color.Cyan, Color(0xFFFFA500), Color(0xFFFF4500)
    )

    // Music Setup
    val mediaPlayer = remember {
        val songNames = listOf("chasm", "desert_oasis", "tranquil_sea", "frozen_caverns")
        val songName = songNames.random()
        val resId = context.resources.getIdentifier(songName, "raw", context.packageName)
        if (resId != 0) {
            MediaPlayer.create(context, resId).apply { isLooping = true }
        } else {
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

    // Music control
    LaunchedEffect(isMusicPlaying, isPaused) {
        if (isMusicPlaying && !isPaused) mediaPlayer?.start() else mediaPlayer?.pause()
    }

    // Continuous redraw ticker
    val infiniteTransition = rememberInfiniteTransition(label = "tracerTicker")
    val ticker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)),
        label = "ticker"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val maxWidth = constraints.maxWidth.toFloat()
        val maxHeight = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(isPaused) {
                if (isPaused) return@pointerInput
                detectTapGestures { offset ->
                    val randomColor = gameColors.random()
                    targets.add(offset to randomColor)
                    val startPos = Offset(maxWidth / 2, maxHeight / 2)
                    sparks.add(Spark(startPos, offset, randomColor, System.currentTimeMillis()))
                }
            }) {
            // Read ticker to force redraw every frame
            val _t = ticker 
            val currentTime = if (isPaused) pausedTime else System.currentTimeMillis()
            
            // Update and Draw Sparks
            val sparksToRemove = mutableListOf<Spark>()
            sparks.forEach { spark ->
                val progress = (currentTime - spark.startTime).toFloat() / spark.duration
                if (progress >= 1f) {
                    if (!isPaused) {
                        sparksToRemove.add(spark)
                        val targetColor = gameColors.random()
                        ripples.add(Ripple(spark.target, spark.color, targetColor, currentTime))
                        targets.removeAll { it.first == spark.target }
                        score++
                        viewModel.incrementTracerScore(today, score)
                    }
                } else {
                    val currentPos = Offset(
                        spark.position.x + (spark.target.x - spark.position.x) * progress,
                        spark.position.y + (spark.target.y - spark.position.y) * progress
                    )
                    
                    drawCircle(
                        color = spark.color,
                        radius = 6.dp.toPx(),
                        center = currentPos
                    )
                    
                    // Particles
                    repeat(3) { i ->
                        val angle = (currentTime / 100.0 + i * 2.0).toFloat()
                        val orbitPos = currentPos + Offset(cos(angle) * 15f, sin(angle) * 15f)
                        drawCircle(
                            color = spark.color.copy(alpha = 0.5f),
                            radius = 2.dp.toPx(),
                            center = orbitPos
                        )
                    }
                }
            }
            if (!isPaused) {
                sparks.removeAll(sparksToRemove)
            }

            // Update and Draw Ripples
            if (!isPaused) {
                ripples.removeIf { (currentTime - it.startTime) > it.duration }
            }
            ripples.forEach { ripple ->
                val progress = (currentTime - ripple.startTime).toFloat() / ripple.duration
                for (i in 0 until 2) {
                    val waveDelay = i * 0.2f
                    val waveProgress = (progress - waveDelay).coerceIn(0f, 1f)
                    if (waveProgress > 0f) {
                        val radius = waveProgress * 200f * density
                        val alpha = (1f - waveProgress) * 0.8f
                        drawCircle(
                            color = ripple.color.copy(alpha = alpha),
                            radius = radius,
                            center = ripple.center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                        )
                    }
                }
            }

            // Draw active target markers
            targets.forEach { (target, color) ->
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    radius = 10.dp.toPx(),
                    center = target
                )
            }
        }

        // Pause Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp, end = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            IconButton(
                onClick = { 
                    if (!isPaused) {
                        pausedTime = System.currentTimeMillis()
                        isPaused = true
                    } else {
                        val pauseDuration = System.currentTimeMillis() - pausedTime
                        // Adjust spark start times
                        sparks.indices.forEach { i ->
                            sparks[i] = sparks[i].copy(startTime = sparks[i].startTime + pauseDuration)
                        }
                        // Adjust ripple start times
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

        // UI
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Tracer Game", color = PrimaryCyber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Score: $score", color = TextColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                dailyScore?.let {
                    Text(text = "Daily High: ${it.tracerHighScore}", color = PrimaryCyber.copy(alpha = 0.7f), fontSize = 14.sp)
                    Text(text = "Daily Total: ${it.tracerTotalScore}", color = PrimaryCyber.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }

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
