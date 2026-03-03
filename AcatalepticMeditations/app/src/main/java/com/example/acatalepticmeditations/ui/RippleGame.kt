package com.example.acatalepticmeditations.ui

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.acatalepticmeditations.ui.theme.*
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

@Composable
fun RippleGame(
    viewModel: JournalViewModel,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var score by remember { mutableIntStateOf(0) }
    var dotPosition by remember { mutableStateOf<Offset?>(null) }
    var isDotVisible by remember { mutableStateOf(true) }
    val ripples = remember { mutableStateListOf<Ripple>() }
    val today = LocalDate.now()
    val dailyScore by viewModel.getScoreForDate(today).collectAsState(initial = null)
    val density = LocalDensity.current.density
    var isMusicPlaying by remember { mutableStateOf(true) }

    // Music Setup
    val mediaPlayer = remember {
        val songNames = listOf("chasm", "desert_oasis", "tranquil_sea", "frozen_caverns")
        val songName = songNames.random()
        
        // This looks for files directly in res/raw (e.g. res/raw/chasm.mp3)
        val resId = context.resources.getIdentifier(songName, "raw", context.packageName)
        
        if (resId != 0) {
            MediaPlayer.create(context, resId).apply { 
                isLooping = true 
            }
        } else {
            Log.e("RippleGame", "Could not find music resource: $songName. Ensure files are directly in res/raw/")
            null
        }
    }

    // Start/Stop music with the game lifecycle and handle pause/play
    DisposableEffect(Unit) {
        if (isMusicPlaying) {
            mediaPlayer?.start()
        }
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(isMusicPlaying) {
        if (isMusicPlaying) {
            mediaPlayer?.start()
        } else {
            mediaPlayer?.pause()
        }
    }

    // Dot Fade Animation
    val dotAlpha by animateFloatAsState(
        targetValue = if (isDotVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "dotAlpha"
    )

    LaunchedEffect(score) {
        if (score > (dailyScore?.highScore ?: 0)) {
            viewModel.updateHighScore(today, score)
        }
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
        val topReservedHeight = 120f * density // Space reserved for score and header

        if (dotPosition == null) {
            dotPosition = Offset(maxWidth / 2f, (maxHeight + topReservedHeight) / 2f)
        }

        val currentDotPosition = dotPosition ?: Offset(maxWidth / 2f, (maxHeight + topReservedHeight) / 2f)

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(currentDotPosition, isDotVisible) { 
                if (!isDotVisible) return@pointerInput
                detectTapGestures { offset ->
                    val distance = (offset - currentDotPosition).getDistance()
                    if (distance < 65f * density) {
                        scope.launch {
                            isDotVisible = false
                            val startColor = listOf(PrimaryCyber, SecondaryCyber, Color.Yellow, Color.Green, Color.Magenta).random()
                            val targetColor = listOf(PrimaryCyber, SecondaryCyber, Color.Yellow, Color.Green, Color.Magenta).random()
                            ripples.add(Ripple(currentDotPosition, startColor, targetColor, System.currentTimeMillis()))
                            score++
                            
                            delay(800) // 300ms for fade-out + 500ms wait
                            
                            dotPosition = Offset(
                                Random.nextFloat() * (maxWidth - 120f * density) + 60f * density,
                                Random.nextFloat() * (maxHeight - topReservedHeight - 60f * density) + topReservedHeight + 30f * density
                            )
                            isDotVisible = true
                        }
                    }
                }
            }) {
            val currentTime = System.currentTimeMillis()
            val _t = ticker // Trigger recomposition

            ripples.removeIf { (currentTime - it.startTime) > it.duration }
            
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

        // The Dot with animation
        Box(
            modifier = Modifier
                .offset(
                    x = (currentDotPosition.x / density).dp - 25.dp,
                    y = (currentDotPosition.y / density).dp - 25.dp
                )
                .graphicsLayer { alpha = dotAlpha }
                .size(50.dp)
                .clip(CircleShape)
                .background(PrimaryCyber)
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Score: $score", color = TextColor, fontSize = 24.sp)
                Row {
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
            dailyScore?.let {
                Text(text = "Daily High: ${it.highScore}", color = PrimaryCyber.copy(alpha = 0.7f), fontSize = 16.sp)
            }
        }
    }
}
