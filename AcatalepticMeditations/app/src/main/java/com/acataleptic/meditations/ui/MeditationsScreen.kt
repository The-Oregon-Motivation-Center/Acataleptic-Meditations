package com.acataleptic.meditations.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acataleptic.meditations.ui.theme.*
import kotlinx.coroutines.delay

private const val BASE_URL = "https://the-oregon-motivation-center.github.io/Acatmedassets"

private val meditations = listOf(
    "Day 1 Affirmations  - Self affirmations" to "Day 1 - Self Affirmations",
    "Day 2 Monday exercise motivation 1 v2 final" to "Day 2 - Monday Exercise Motivation"
)

private val calmScapes = listOf("Beach", "Rain", "Psychedelic", "Space", "Wormhole")

private fun formatMs(ms: Int): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
fun MeditationsScreen(onExit: () -> Unit) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableIntStateOf(0) }

    var showCalmScape by remember { mutableStateOf(false) }
    var hideControls by remember { mutableStateOf(false) }
    var calmScapeIndex by remember { mutableIntStateOf(0) }
    var calmSpaceSpeed by remember { mutableStateOf(SpaceSpeed.MEDIUM) }
    var calmRainIntensity by remember { mutableStateOf(RainIntensity.LITTLE) }

    val (filename, displayName) = meditations[currentIndex]
    val url = "$BASE_URL/${filename}.mp3"

    LaunchedEffect(currentIndex) {
        isPrepared = false
        progress = 0f
        duration = 0
    }

    val mediaPlayer = remember(currentIndex) {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(url)
            setOnPreparedListener { mp ->
                isPrepared = true
                duration = mp.duration
                if (isPlaying) mp.start()
            }
            prepareAsync()
        }
    }

    DisposableEffect(mediaPlayer) {
        onDispose {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        try {
            if (isPlaying) mediaPlayer.start() else mediaPlayer.pause()
        } catch (_: IllegalStateException) { }
    }

    LaunchedEffect(isPlaying, isPrepared) {
        while (isPlaying && isPrepared) {
            if (duration > 0) progress = mediaPlayer.currentPosition.toFloat() / duration
            delay(500)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showCalmScape) {
            CalmScapeVisual(
                currentScape = calmScapes[calmScapeIndex],
                spaceSpeed = calmSpaceSpeed,
                rainIntensity = calmRainIntensity
            )
        }

        if (!hideControls) Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (showCalmScape) Color.Transparent else DarkBackground)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit", tint = PrimaryCyber)
                }
                Text(
                    text = "Meditations",
                    color = TextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                if (showCalmScape) {
                    IconButton(onClick = { hideControls = true }) {
                        Icon(
                            Icons.Default.VisibilityOff,
                            contentDescription = "Hide Controls",
                            tint = TextColor.copy(alpha = 0.7f)
                        )
                    }
                }
                IconButton(onClick = { showCalmScape = !showCalmScape; if (!showCalmScape) hideControls = false }) {
                    Icon(
                        Icons.Default.Landscape,
                        contentDescription = "Toggle Calm Scape",
                        tint = if (showCalmScape) PrimaryCyber else TextColor.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calm Scape selector
            if (showCalmScape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    calmScapes.forEachIndexed { index, name ->
                        val isSelected = index == calmScapeIndex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) PrimaryCyber.copy(alpha = 0.85f)
                                    else Color.Black.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryCyber else TextColor.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { calmScapeIndex = index }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) Color.Black else TextColor,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Meditation selector
            meditations.forEachIndexed { index, (_, name) ->
                val isSelected = index == currentIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) PrimaryCyber.copy(alpha = 0.15f)
                            else if (showCalmScape) Color.Black.copy(alpha = 0.45f)
                            else DarkBackground
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PrimaryCyber else TextColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (index != currentIndex) {
                                isPlaying = false
                                currentIndex = index
                            }
                        }
                        .padding(16.dp)
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) PrimaryCyber else TextColor,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Player controls — semi-transparent card when scape is active
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (showCalmScape) Color.Black.copy(alpha = 0.55f) else Color.Transparent)
                    .padding(if (showCalmScape) 16.dp else 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = displayName,
                    color = TextColor,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = progress,
                    onValueChange = { value ->
                        progress = value
                        if (isPrepared && duration > 0) mediaPlayer.seekTo((value * duration).toInt())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryCyber,
                        activeTrackColor = PrimaryCyber,
                        inactiveTrackColor = TextColor.copy(alpha = 0.3f)
                    ),
                    enabled = isPrepared
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPrepared) formatMs((progress * duration).toInt()) else "0:00",
                        color = TextColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isPrepared) formatMs(duration) else "--:--",
                        color = TextColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = PrimaryCyber,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (hideControls && showCalmScape) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                IconButton(
                    onClick = { hideControls = false },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Show Controls",
                        tint = PrimaryCyber
                    )
                }
            }
        }
    }
}
