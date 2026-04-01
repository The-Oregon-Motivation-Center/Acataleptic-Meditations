package com.acataleptic.meditations.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acataleptic.meditations.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

data class Star(var x: Float, var y: Float, var z: Float)

enum class SpaceSpeed(val label: String, val speedValue: Float) {
    SLOW("Slow", 0.001f),
    MEDIUM("Medium", 0.0025f),
    FAST("Fast", 0.006f)
}

enum class RainIntensity(val label: String, val coverage: Float) {
    LITTLE("Little", 0.75f),
    MEDIUM("Medium", 0.9f),
    MAX("Max", 0.99f)
}

enum class RainVariation(val label: String) {
    CITY("City Rain"),
    RAIN_ONLY("Rain Only"),
    RAINFOREST("Rainforest")
}

data class Raindrop(
    val x: Float,
    var y: Float,
    val speed: Float,
    val isForeground: Boolean
)

data class Cloud(
    var x: Float,
    var y: Float,
    val scale: Float,
    var speedX: Float,
    var speedY: Float
)

data class Spiral(
    val center: Offset,
    val color: Color,
    val startTime: Long,
    val duration: Int = 2000,
    val isClockwise: Boolean = true
)

data class WormholeCircle(
    val center: Offset,
    val hue: Float,
    val startTime: Long,
    val duration: Int = 3000
)

data class FishJump(
    val x: Float,
    val y: Float,
    val targetX: Float,
    val targetY: Float,
    val startTime: Long,
    val duration: Long = 800L,
    val color: Color,
    val maxHeight: Float
)

data class Crab(
    val y: Float,
    val fromLeft: Boolean,
    val startTime: Long,
    val duration: Long = 5000L
)

data class Seagull(
    val y: Float,
    val fromLeft: Boolean,
    val startTime: Long,
    val duration: Long = 6000L,
    val wingScale: Float = 1f
)

data class JungleAnimal(
    val type: String,
    val startTime: Long,
    val fromLeft: Boolean,
    val duration: Long
)

@Composable
fun CalmScapesGame(onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isMusicPlaying by remember { mutableStateOf(true) }
    var currentScapeIndex by remember { mutableIntStateOf(0) }
    val scapes = listOf("Beach", "Rain", "Psychedelic", "Space", "Wormhole")
    val currentScape = scapes[currentScapeIndex]
    
    var showScapeMenu by remember { mutableStateOf(false) }
    var spaceSpeed by remember { mutableStateOf(SpaceSpeed.MEDIUM) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var rainIntensity by remember { mutableStateOf(RainIntensity.LITTLE) }
    var showRainMenu by remember { mutableStateOf(false) }
    var rainVariation by remember { mutableStateOf(RainVariation.CITY) }
    var showRainVariationMenu by remember { mutableStateOf(false) }
    var showSongMenu by remember { mutableStateOf(false) }
    
    var spaceDirectionForward by remember { mutableStateOf(true) }
    var sunXFactor by remember { mutableFloatStateOf(0.5f) }
    var sunYOffset by remember { mutableFloatStateOf(0f) }

    val allSongs = listOf(
        "chasm", "desert_oasis",
        "tranquil_sea", "frozen_caverns", "lavender_skies", 
        "celestial_drift", "late_light_loops"
    )
    
    var currentSongIndex by remember { mutableIntStateOf(allSongs.indices.random()) }

    // Rain State
    val raindrops = remember { mutableStateListOf<Raindrop>() }
    
    // Lightning State
    var lightningAlpha by remember { mutableStateOf(0f) }
    var strikingCloudIndex by remember { mutableIntStateOf(-1) }

    // Psychedelic Spirals
    val activeSpirals = remember { mutableStateListOf<Spiral>() }

    // Fish Jumps for Beach
    val activeFishJumps = remember { mutableStateListOf<FishJump>() }
    val activeCrabs = remember { mutableStateListOf<Crab>() }
    val activeSeagulls = remember { mutableStateListOf<Seagull>() }
    var activeJungleAnimal by remember { mutableStateOf<JungleAnimal?>(null) }

    // Wormhole State
    val wormholeCircles = remember { mutableStateListOf<WormholeCircle>() }
    var lastWormholeCenter by remember { mutableStateOf(Offset.Zero) }
    var targetWormholeCenter by remember { mutableStateOf<Offset?>(null) }
    var wormholeVelocity by remember { mutableStateOf(Offset(5f, 5f)) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }

    // Cloud State
    val clouds = remember { 
        val list = mutableStateListOf<Cloud>()
        repeat(7) {
            list.add(Cloud(
                x = Random.nextFloat(),
                y = 0.12f + Random.nextFloat() * 0.4f,
                scale = 1.2f + Random.nextFloat() * 1.8f,
                speedX = (Random.nextFloat() - 0.5f) * 0.001f,
                speedY = (Random.nextFloat() - 0.5f) * 0.0005f
            ))
        }
        list
    }

    // Common strike logic
    suspend fun performStrike() {
        if (strikingCloudIndex != -1) return
        strikingCloudIndex = Random.nextInt(clouds.size)
        lightningAlpha = 0.4f
        delay(60)
        lightningAlpha = 0f
        delay(40)
        if (Random.nextFloat() > 0.4f) {
            lightningAlpha = 0.5f
            delay(80)
            lightningAlpha = 0f
        }
        strikingCloudIndex = -1
    }

    // Procedural Rain Generation
    LaunchedEffect(currentScape, rainIntensity, rainVariation) {
        while (true) {
            delay(12)
            if (currentScape == "Rain" && Random.nextFloat() < rainIntensity.coverage) {
                val isForeground = Random.nextBoolean()
                val startY = when (rainVariation) {
                    RainVariation.CITY -> 0.06f
                    RainVariation.RAINFOREST -> 0.18f
                    RainVariation.RAIN_ONLY -> -0.05f
                }
                raindrops.add(Raindrop(
                    x = Random.nextFloat(),
                    y = startY,
                    speed = if (isForeground) 0.00656f else 0.00337f,
                    isForeground = isForeground
                ))
            }
            raindrops.removeIf { it.y > 1.1f }
        }
    }

    // Lightning Effect for City Rain
    LaunchedEffect(currentScape, rainVariation) {
        if (currentScape == "Rain" && rainVariation == RainVariation.CITY) {
            while (true) {
                delay(Random.nextLong(4000, 12000))
                performStrike()
            }
        } else {
            lightningAlpha = 0f
            strikingCloudIndex = -1
        }
    }

    // Psychedelic Spiral Generation
    LaunchedEffect(currentScape, screenSize) {
        if (currentScape == "Psychedelic") {
            while (true) {
                delay(500)
                if (screenSize.width > 0 && screenSize.height > 0) {
                    activeSpirals.add(
                        Spiral(
                            center = Offset(
                                Random.nextFloat() * screenSize.width,
                                Random.nextFloat() * screenSize.height
                            ),
                            color = Color.hsv(Random.nextFloat() * 360f, 0.8f, 1f),
                            startTime = System.currentTimeMillis(),
                            isClockwise = Random.nextBoolean()
                        )
                    )
                }
            }
        } else {
            activeSpirals.clear()
        }
    }

    LaunchedEffect(currentScape, screenSize) {
        if (currentScape == "Beach" && screenSize.width > 0 && screenSize.height > 0) {
            // Seagulls — sky area
            launch {
                while (true) {
                    val randomDelay = Random.nextLong(0, 15000)
                    delay(randomDelay)
                    val count = 2 + Random.nextInt(3)
                    repeat(count) { i ->
                        activeSeagulls.add(Seagull(
                            y = (0.05f + Random.nextFloat() * 0.24f) * screenSize.height,
                            fromLeft = Random.nextBoolean(),
                            startTime = System.currentTimeMillis() + i * 400L,
                            wingScale = 0.8f + Random.nextFloat() * 0.5f
                        ))
                    }
                    delay(15000 - randomDelay)
                }
            }
            // Fish — water area
            launch {
                while (true) {
                    val randomDelay = Random.nextLong(0, 15000)
                    delay(randomDelay)
                    val count = 2 + Random.nextInt(3)
                    repeat(count) { i ->
                        val startX = Random.nextFloat() * screenSize.width
                        val startY = (0.4f + Random.nextFloat() * 0.2f) * screenSize.height
                        val jumpWidth = (Random.nextFloat() - 0.5f) * 300f
                        activeFishJumps.add(FishJump(
                            x = startX,
                            y = startY,
                            targetX = startX + jumpWidth,
                            targetY = startY,
                            startTime = System.currentTimeMillis() + i * 400L,
                            color = Color(0xFF81D4FA),
                            maxHeight = 80f + Random.nextFloat() * 100f
                        ))
                    }
                    delay(15000 - randomDelay)
                }
            }
            // Crabs — sand area
            launch {
                while (true) {
                    val randomDelay = Random.nextLong(0, 15000)
                    delay(randomDelay)
                    val count = 2 + Random.nextInt(3)
                    repeat(count) { i ->
                        activeCrabs.add(Crab(
                            y = (0.66f + Random.nextFloat() * 0.34f) * screenSize.height,
                            fromLeft = Random.nextBoolean(),
                            startTime = System.currentTimeMillis() + i * 500L
                        ))
                    }
                    delay(15000 - randomDelay)
                }
            }
        } else {
            activeFishJumps.clear()
            activeCrabs.clear()
            activeSeagulls.clear()
        }
    }

    // Jungle Animals for Rainforest scape
    LaunchedEffect(currentScape, rainVariation) {
        if (currentScape == "Rain" && rainVariation == RainVariation.RAINFOREST) {
            while (true) {
                delay(Random.nextLong(20000L, 32000L))
                if (screenSize.width > 0) {
                    val aType = listOf("monkey", "jaguar", "toucan").random()
                    val dur = when (aType) { "monkey" -> 5500L; "jaguar" -> 9000L; else -> 6000L }
                    activeJungleAnimal = JungleAnimal(type = aType, startTime = System.currentTimeMillis(), fromLeft = Random.nextBoolean(), duration = dur)
                    delay(dur + 500L)
                    activeJungleAnimal = null
                }
            }
        } else {
            activeJungleAnimal = null
        }
    }

    // Wormhole Generation
    LaunchedEffect(currentScape, spaceSpeed, screenSize) {
        if (currentScape == "Wormhole") {
            // Initialize velocity based on speed setting
            val baseSpeed = when(spaceSpeed) {
                SpaceSpeed.SLOW -> 4f
                SpaceSpeed.MEDIUM -> 10f
                SpaceSpeed.FAST -> 20f
            }
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            wormholeVelocity = Offset(cos(angle) * baseSpeed, sin(angle) * baseSpeed)

            if (lastWormholeCenter == Offset.Zero && screenSize.width > 0) {
                lastWormholeCenter = Offset(screenSize.width / 2f, screenSize.height / 2f)
            }

            while (true) {
                val nextX: Float
                val nextY: Float
                val currentTarget = targetWormholeCenter
                
                if (currentTarget != null) {
                    val angleToTarget = atan2(currentTarget.y - lastWormholeCenter.y, currentTarget.x - lastWormholeCenter.x)
                    val step = when(spaceSpeed) {
                        SpaceSpeed.SLOW -> 8f
                        SpaceSpeed.MEDIUM -> 18f
                        SpaceSpeed.FAST -> 35f
                    }
                    val dist = (lastWormholeCenter - currentTarget).getDistance()
                    if (dist < step) {
                        nextX = currentTarget.x
                        nextY = currentTarget.y
                        targetWormholeCenter = null
                        // Give it a fresh bounce velocity after reaching target
                        val a = Random.nextFloat() * 2 * Math.PI.toFloat()
                        wormholeVelocity = Offset(cos(a) * baseSpeed, sin(a) * baseSpeed)
                    } else {
                        nextX = lastWormholeCenter.x + cos(angleToTarget) * step
                        nextY = lastWormholeCenter.y + sin(angleToTarget) * step
                    }
                } else {
                    // DVD Bounce Logic
                    var trialX = lastWormholeCenter.x + wormholeVelocity.x
                    var trialY = lastWormholeCenter.y + wormholeVelocity.y
                    
                    var newVelX = wormholeVelocity.x
                    var newVelY = wormholeVelocity.y
                    
                    if (screenSize.width > 0) {
                        val margin = 40f
                        if (trialX < margin || trialX > screenSize.width - margin) {
                            newVelX = -newVelX
                            trialX = trialX.coerceIn(margin, screenSize.width.toFloat() - margin)
                        }
                        if (trialY < margin || trialY > screenSize.height - margin) {
                            newVelY = -newVelY
                            trialY = trialY.coerceIn(margin, screenSize.height.toFloat() - margin)
                        }
                    }
                    nextX = trialX
                    nextY = trialY
                    wormholeVelocity = Offset(newVelX, newVelY)
                }
                
                wormholeCircles.add(WormholeCircle(
                    center = Offset(nextX, nextY), 
                    hue = (System.currentTimeMillis() / 20f) % 360f, 
                    startTime = System.currentTimeMillis()
                ))
                
                val now = System.currentTimeMillis()
                wormholeCircles.removeAll { now - it.startTime > it.duration }
                
                lastWormholeCenter = Offset(nextX, nextY)
                val delayMs = when(spaceSpeed) {
                    SpaceSpeed.SLOW -> 80L
                    SpaceSpeed.MEDIUM -> 40L
                    SpaceSpeed.FAST -> 16L
                }
                delay(delayMs)
            }
        } else {
            wormholeCircles.clear()
        }
    }

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

    val infiniteTransition = rememberInfiniteTransition(label = "scapeTicker")
    val frameTicker by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(16, easing = LinearEasing)), label = "frameTicker")
    val waveOffset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 2f * Math.PI.toFloat(), animationSpec = infiniteRepeatable(animation = tween(16000, easing = LinearEasing)), label = "wave")
    val tideOffset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 2f * Math.PI.toFloat(), animationSpec = infiniteRepeatable(animation = tween(48000, easing = LinearEasing)), label = "tide")
    val cloudTime by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(animation = tween(1000000, easing = LinearEasing)), label = "cloudTime")
    
    val sunTimeFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunTime"
    )

    val stars = remember {
        mutableStateListOf<Star>().apply {
            repeat(800) { add(Star(Random.nextFloat() * 2 - 1, Random.nextFloat() * 2 - 1, Random.nextFloat())) }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .onSizeChanged { screenSize = it }
        .pointerInput(currentScapeIndex) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    if (currentScape == "Beach" && change.position.y < size.height * 0.4f) {
                        sunXFactor = (sunXFactor + dragAmount.x / (size.width / 1.5f)).coerceIn(0f, 1f)
                        sunYOffset = (sunYOffset + dragAmount.y / size.height).coerceIn(-0.15f, 0.15f)
                    } else if (currentScape == "Beach" && change.position.y >= size.height * 0.4f) {
                        if (dragAmount.x > 50) {
                            currentScapeIndex = (currentScapeIndex - 1 + scapes.size) % scapes.size
                        } else if (dragAmount.x < -50) {
                            currentScapeIndex = (currentScapeIndex + 1) % scapes.size
                        }
                    }
                    
                    if (currentScape != "Beach") {
                        if (dragAmount.x > 50) {
                            currentScapeIndex = (currentScapeIndex - 1 + scapes.size) % scapes.size
                        } else if (dragAmount.x < -50) {
                            currentScapeIndex = (currentScapeIndex + 1) % scapes.size
                        }
                    }
                }
            )
        }
        .pointerInput(currentScape) {
            detectTapGestures { offset ->
                if (currentScape == "Rain" && rainVariation == RainVariation.RAINFOREST) {
                    if (activeJungleAnimal == null) {
                        val aType = listOf("monkey", "jaguar", "toucan").random()
                        val dur = when (aType) { "monkey" -> 5500L; "jaguar" -> 9000L; else -> 6000L }
                        activeJungleAnimal = JungleAnimal(type = aType, startTime = System.currentTimeMillis(), fromLeft = Random.nextBoolean(), duration = dur)
                        scope.launch { delay(dur + 500L); activeJungleAnimal = null }
                    }
                } else if (currentScape == "Rain" && rainVariation == RainVariation.CITY) {
                    scope.launch { performStrike() }
                } else if (currentScape == "Psychedelic") {
                    activeSpirals.add(Spiral(center = offset, color = Color.hsv(Random.nextFloat() * 360f, 0.8f, 1f), startTime = System.currentTimeMillis(), isClockwise = Random.nextBoolean()))
                } else if (currentScape == "Wormhole") {
                    targetWormholeCenter = offset
                } else if (currentScape == "Space") {
                    spaceDirectionForward = !spaceDirectionForward
                } else if (currentScape == "Beach" && offset.y < size.height * 0.33f) {
                    val count = 2 + Random.nextInt(3)
                    repeat(count) { i ->
                        activeSeagulls.add(Seagull(
                            y = (0.05f + Random.nextFloat() * 0.24f) * size.height,
                            fromLeft = Random.nextBoolean(),
                            startTime = System.currentTimeMillis() + i * 400L,
                            wingScale = 0.8f + Random.nextFloat() * 0.5f
                        ))
                    }
                } else if (currentScape == "Beach" && offset.y > size.height * 0.4f) {
                    val count = 2 + Random.nextInt(3)
                    if (offset.y > size.height * 0.66f) {
                        repeat(count) { i ->
                            activeCrabs.add(Crab(
                                y = (0.66f + Random.nextFloat() * 0.34f) * size.height,
                                fromLeft = Random.nextBoolean(),
                                startTime = System.currentTimeMillis() + i * 500L
                            ))
                        }
                    } else {
                        repeat(count) { i ->
                            val startX = Random.nextFloat() * size.width
                            val startY = (0.4f + Random.nextFloat() * 0.2f) * size.height
                            val jumpWidth = (Random.nextFloat() - 0.5f) * 300f
                            activeFishJumps.add(FishJump(
                                x = startX,
                                y = startY,
                                targetX = startX + jumpWidth,
                                targetY = startY,
                                startTime = System.currentTimeMillis() + i * 400L,
                                color = Color(0xFF81D4FA),
                                maxHeight = 80f + Random.nextFloat() * 100f
                            ))
                        }
                    }
                }
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val _t = frameTicker

            if (lastWormholeCenter == Offset.Zero) {
                lastWormholeCenter = Offset(width / 2, height / 2)
            }

            if (currentScape == "Beach") {
                // Auto drift: sun moves between y=0.35 (low/sunset) and y=0.15 (high/midday)
                val autoSunY = 0.35f - sunTimeFactor * 0.2f
                val effectiveSunYFactor = (autoSunY + sunYOffset).coerceIn(0.03f, 0.38f)
                // colorFactor: 0 = sunset (sun low), 1 = midday (sun high)
                val colorFactor = 1f - (effectiveSunYFactor - 0.03f) / (0.35f - 0.03f)
                val skyColorMidday = listOf(Color(0xFF87CEEB), Color(0xFFE0F6FF))
                val skyColorSunset = listOf(Color(0xFFFF4500), Color(0xFFFFD700))
                val currentSkyColors = skyColorSunset.zip(skyColorMidday) { s, m -> androidx.compose.ui.graphics.lerp(s, m, colorFactor) }
                drawRect(brush = Brush.verticalGradient(currentSkyColors, endY = height * 0.4f), size = size.copy(height = height * 0.4f))
                val sunColor = androidx.compose.ui.graphics.lerp(Color(0xFFFF6347), Color(0xFFFFFACD), colorFactor)
                val sunY = height * effectiveSunYFactor
                drawCircle(color = sunColor.copy(alpha = 0.8f), radius = 60f, center = Offset(width * sunXFactor, sunY))
                val sandColor = androidx.compose.ui.graphics.lerp(Color(0xFFC2B280), Color(0xFFEDC9AF), colorFactor)
                drawRect(color = sandColor, topLeft = Offset(0f, height * 0.4f), size = size.copy(height = height * 0.6f))
                val oceanColor = androidx.compose.ui.graphics.lerp(Color(0xFF191970), Color(0xFF0077BE), colorFactor)
                val accentColor = androidx.compose.ui.graphics.lerp(Color(0xFF4B0082), Color(0xFF00A2E8), colorFactor)
                for (i in 4 downTo 0) {
                    val phaseShift = i * (2f * Math.PI.toFloat() / 5f); val individualTideFactor = (sin(tideOffset - phaseShift) + 1f) / 2f
                    val reachMultiplier = 0.1f + (4 - i) * 0.06f; val waveEdgeY = height * 0.4f + (individualTideFactor * height * reachMultiplier)
                    val waterPath = Path().apply { moveTo(0f, height * 0.4f); lineTo(width, height * 0.4f); lineTo(width, waveEdgeY) }
                    for (x in width.toInt() downTo 0 step 15) { val ripple = sin(x * (0.01f + ((4-i) * 0.001f)) + waveOffset + i) * (5f + ((4-i) * 4f)); waterPath.lineTo(x.toFloat(), waveEdgeY + ripple) }
                    waterPath.close(); drawPath(path = waterPath, color = if (i % 2 == 0) oceanColor.copy(alpha = 0.2f + (4-i)*0.1f) else accentColor.copy(alpha = 0.2f + (4-i)*0.1f))
                    if (individualTideFactor > 0.3f) {
                        val foamPath = Path().apply { moveTo(width, waveEdgeY) }
                        for (x in width.toInt() downTo 0 step 10) { val ripple = sin(x * 0.012f + waveOffset * 1.1f + i) * (5f + (4-i) * 3f); foamPath.lineTo(x.toFloat(), waveEdgeY + ripple) }
                        drawPath(path = foamPath, color = Color.White.copy(alpha = (individualTideFactor * 0.4f).coerceAtMost(0.4f)), style = Stroke(width = (1 + (4-i)).dp.toPx()))
                    }
                }

                // Fish Jumps
                val currentTime = System.currentTimeMillis()
                activeFishJumps.removeIf { currentTime - it.startTime > it.duration }
                activeFishJumps.forEach { fish ->
                    val progress = (currentTime - fish.startTime).toFloat() / fish.duration
                    val currentX = fish.x + (fish.targetX - fish.x) * progress
                    val currentY = fish.y - 4 * fish.maxHeight * progress * (1 - progress)

                    // Fish Body
                    drawOval(
                        color = fish.color,
                        topLeft = Offset(currentX - 12f, currentY - 6f),
                        size = androidx.compose.ui.geometry.Size(24f, 12f)
                    )
                    // Tail
                    val tailPath = Path().apply {
                        moveTo(currentX, currentY)
                        if (fish.targetX > fish.x) {
                            lineTo(currentX - 18f, currentY - 8f)
                            lineTo(currentX - 18f, currentY + 8f)
                        } else {
                            lineTo(currentX + 18f, currentY - 8f)
                            lineTo(currentX + 18f, currentY + 8f)
                        }
                        close()
                    }
                    drawPath(tailPath, fish.color)
                }

                // Crabs
                activeCrabs.removeIf { currentTime - it.startTime > it.duration }
                activeCrabs.forEach { crab ->
                    val progress = (currentTime - crab.startTime).toFloat() / crab.duration
                    val currentX = if (crab.fromLeft) progress * (width + 100f) - 50f else (1f - progress) * (width + 100f) - 50f
                    val wiggle = sin(progress * 40f) * 5f
                    
                    // Crab Body
                    drawOval(
                        color = Color(0xFFE57373),
                        topLeft = Offset(currentX - 20f, crab.y - 12f),
                        size = androidx.compose.ui.geometry.Size(40f, 24f)
                    )
                    // Eyes
                    drawCircle(Color.White, 4f, Offset(currentX - 8f, crab.y - 12f))
                    drawCircle(Color.White, 4f, Offset(currentX + 8f, crab.y - 12f))
                    drawCircle(Color.Black, 2f, Offset(currentX - 8f, crab.y - 13f))
                    drawCircle(Color.Black, 2f, Offset(currentX + 8f, crab.y - 13f))
                    
                    // Legs
                    for (i in 0..2) {
                        val legOffset = i * 8f - 8f
                        drawLine(Color(0xFFD32F2F), Offset(currentX - 15f, crab.y + legOffset), Offset(currentX - 30f, crab.y + legOffset + wiggle), strokeWidth = 3f)
                        drawLine(Color(0xFFD32F2F), Offset(currentX + 15f, crab.y + legOffset), Offset(currentX + 30f, crab.y + legOffset - wiggle), strokeWidth = 3f)
                    }
                }
                // Seagulls
                activeSeagulls.removeIf { currentTime - it.startTime > it.duration }
                activeSeagulls.forEach { gull ->
                    if (currentTime < gull.startTime) return@forEach
                    val progress = (currentTime - gull.startTime).toFloat() / gull.duration
                    val gullX = if (gull.fromLeft) progress * (width + 120f) - 60f else (1f - progress) * (width + 120f) - 60f
                    val wingFlap = sin(progress * 30f) * 10f * gull.wingScale
                    val span = 18f * gull.wingScale
                    val gullPath = Path().apply {
                        moveTo(gullX - span, gull.y + wingFlap)
                        quadraticBezierTo(gullX - span * 0.45f, gull.y - wingFlap, gullX, gull.y)
                        quadraticBezierTo(gullX + span * 0.45f, gull.y - wingFlap, gullX + span, gull.y + wingFlap)
                    }
                    drawPath(gullPath, Color.White.copy(alpha = 0.9f), style = Stroke(width = 3f, cap = StrokeCap.Round))
                    drawCircle(Color(0xFFF5DEB3), radius = 3f * gull.wingScale, center = Offset(gullX, gull.y))
                }
            } else if (currentScape == "Rain") {
                when (rainVariation) {
                    RainVariation.RAIN_ONLY -> {
                        drawRect(Color(0xFF0D0D1A))
                        raindrops.forEach { drop -> drop.y += drop.speed; if (!drop.isForeground) drawLine(Color.White.copy(alpha = 0.4f), Offset(drop.x * width, drop.y * height), Offset(drop.x * width, drop.y * height + 50f), strokeWidth = 2.5f) }
                        raindrops.forEach { if (it.isForeground) drawLine(Color.White.copy(alpha = 0.8f), Offset(it.x * width, it.y * height), Offset(it.x * width, it.y * height + 50f), strokeWidth = 4.5f) }
                    }
                    RainVariation.CITY -> {
                        drawRect(Color(0xFF050510)); if (lightningAlpha > 0f) drawRect(Color.White.copy(alpha = lightningAlpha * 0.15f))
                        val topCloudColor = if (lightningAlpha > 0.3f) Color.White.copy(alpha = lightningAlpha * 0.8f) else if (lightningAlpha > 0f) Color.LightGray.copy(alpha = 0.5f) else Color(0xFF151525)
                        drawRect(color = topCloudColor, size = androidx.compose.ui.geometry.Size(width, height * 0.04f))
                        for (i in 0..(width / 40f).toInt() + 1) drawCircle(color = topCloudColor, radius = 40f * (0.8f + (sin(i.toFloat() + cloudTime * 0.05f) * 0.2f)), center = Offset(i * 40f * 1.2f, height * 0.04f))
                        raindrops.forEach { drop -> drop.y += drop.speed; if (!drop.isForeground) drawLine(Color.White.copy(alpha = 0.3f), Offset(drop.x * width, drop.y * height), Offset(drop.x * width, drop.y * height + 30f), strokeWidth = 2f) }
                        clouds.forEachIndexed { i, cloud ->
                            cloud.x += cloud.speedX; cloud.y += cloud.speedY
                            val baseRadius = 55f * cloud.scale; val marginX = (baseRadius * 1.5f) / width
                            if (cloud.x < marginX || cloud.x > 1f - marginX) { cloud.speedX *= -1f; cloud.x = cloud.x.coerceIn(marginX, 1f - marginX) }
                            val minY = 0.12f + (baseRadius * 1.2f) / height; val maxY = 0.6f - (baseRadius * 1.2f) / height
                            if (cloud.y < minY || cloud.y > maxY) { cloud.speedY *= -1f; cloud.y = cloud.y.coerceIn(minY, maxY) }
                            for (j in i + 1 until clouds.size) { val other = clouds[j]; val dx = (cloud.x - other.x) * width; val dy = (cloud.y - other.y) * height; val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat(); val minDist = (cloud.scale + other.scale) * 55f; if (dist < minDist) { val tempX = cloud.speedX; val tempY = cloud.speedY; cloud.speedX = other.speedX; cloud.speedY = other.speedY; other.speedX = tempX; other.speedY = tempY } }
                            val cloudColor = if (lightningAlpha > 0.3f) Color.White.copy(alpha = lightningAlpha * 0.8f) else if (lightningAlpha > 0f) Color.LightGray.copy(alpha = 0.5f) else Color(0xFF1A1A2E)
                            drawCircle(cloudColor, baseRadius, Offset(cloud.x * width, cloud.y * height)); drawCircle(cloudColor, baseRadius * 0.8f, Offset(cloud.x * width - baseRadius * 0.7f, cloud.y * height + baseRadius * 0.2f)); drawCircle(cloudColor, baseRadius * 0.8f, Offset(cloud.x * width + baseRadius * 0.7f, cloud.y * height + baseRadius * 0.2f)); drawCircle(cloudColor, baseRadius * 0.6f, Offset(cloud.x * width - baseRadius * 1.2f, cloud.y * height + baseRadius * 0.4f)); drawCircle(cloudColor, baseRadius * 0.6f, Offset(cloud.x * width + baseRadius * 1.2f, cloud.y * height + baseRadius * 0.4f))
                            if (i == strikingCloudIndex && lightningAlpha > 0.3f) {
                                val boltPath = Path().apply { moveTo(cloud.x * width, cloud.y * height) }; var curX = cloud.x * width; var curY = cloud.y * height
                                repeat(12) { val r = Random(System.currentTimeMillis() / 150); curX += (r.nextFloat() - 0.5f) * 160f; curY += (height * 0.95f - cloud.y * height) / 12; boltPath.lineTo(curX, curY) }
                                drawPath(boltPath, Color.White.copy(alpha = lightningAlpha), style = Stroke(width = 6f, cap = StrokeCap.Round)); drawPath(boltPath, Color.White.copy(alpha = lightningAlpha * 0.4f), style = Stroke(width = 18f, cap = StrokeCap.Round))
                            }
                        }
                        val cityRandom = Random(42)
                        for (i in 0 until 8) {
                            val bWidth = (width / 8) * (0.7f + cityRandom.nextFloat() * 0.4f); val bHeight = height * (0.2f + cityRandom.nextFloat() * 0.35f)
                            val bX = i * (width / 8) + ((width / 8) - bWidth) / 2; val bY = height - bHeight
                            drawRect(color = if (lightningAlpha > 0f) Color(0xFF1A1A25) else Color(0xFF0D0D12), topLeft = Offset(bX, bY), size = androidx.compose.ui.geometry.Size(bWidth, bHeight))
                            for (r in 1 until (bHeight / 25f).toInt() - 1) for (c in 1 until (bWidth / 15f).toInt() - 1) { if (Random((42+i) * 1000 + r * 100 + c).nextFloat() > 0.6f) drawRect(color = if ((r+c) % 2 == 0) Color(0xFFFFFACD).copy(alpha = 0.7f) else Color(0xFFE0F6FF).copy(alpha = 0.7f), topLeft = Offset(bX + c * 15f, bY + r * 25f), size = androidx.compose.ui.geometry.Size(6f, 10f)) }
                        }
                        raindrops.forEach { if (it.isForeground) drawLine(Color.White.copy(alpha = 0.7f), Offset(it.x * width, it.y * height), Offset(it.x * width, it.y * height + 45f), strokeWidth = 4f) }
                    }
                    RainVariation.RAINFOREST -> {
                        // === BACKGROUND ===
                        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF050C07), Color(0xFF091508), Color(0xFF061009)), endY = height))

                        // === BACK FOLIAGE TEXTURE ===
                        val bgRnd = Random(55)
                        for (i in 0 until 25) {
                            val bx = bgRnd.nextFloat() * width; val by = height * (0.05f + bgRnd.nextFloat() * 0.65f); val br = 20f + bgRnd.nextFloat() * 50f
                            drawCircle(Color(0xFF081208).copy(alpha = 0.6f), radius = br, center = Offset(bx, by))
                        }

                        // === TREE TRUNKS — left ===
                        val trunkL = Path().apply {
                            moveTo(0f, height); cubicTo(0f, height * 0.6f, width * 0.12f, height * 0.3f, width * 0.15f, -20f)
                            lineTo(width * 0.27f, -20f); cubicTo(width * 0.24f, height * 0.3f, width * 0.20f, height * 0.6f, width * 0.24f, height); close()
                        }
                        drawPath(trunkL, Color(0xFF0A0E08))
                        for (v in 1..4) { val ty = height * (0.2f + v * 0.15f); drawLine(Color(0xFF0E1C0B).copy(alpha = 0.5f), Offset(width * 0.045f, ty), Offset(width * 0.21f, ty + 8f), strokeWidth = 1.5f) }
                        val rootL1 = Path().apply { moveTo(0f, height); cubicTo(-30f, height * 0.85f, -52f, height * 0.92f, -67f, height) }
                        drawPath(rootL1, Color(0xFF080C06), style = Stroke(width = 15f, cap = StrokeCap.Round))
                        val rootL2 = Path().apply { moveTo(width * 0.24f, height); cubicTo(width * 0.33f, height * 0.87f, width * 0.42f, height * 0.91f, width * 0.48f, height) }
                        drawPath(rootL2, Color(0xFF080C06), style = Stroke(width = 12f, cap = StrokeCap.Round))

                        // === TREE TRUNKS — right ===
                        val trunkR = Path().apply {
                            moveTo(width * 0.76f, height); cubicTo(width * 0.805f, height * 0.6f, width * 0.865f, height * 0.3f, width * 0.82f, -20f)
                            lineTo(width, -20f); cubicTo(width, height * 0.3f, width, height * 0.6f, width, height); close()
                        }
                        drawPath(trunkR, Color(0xFF0A0E08))
                        for (v in 1..4) { val ty = height * (0.2f + v * 0.15f); drawLine(Color(0xFF0E1C0B).copy(alpha = 0.5f), Offset(width * 0.79f, ty + 6f), Offset(width * 0.955f, ty), strokeWidth = 1.5f) }
                        val rootR1 = Path().apply { moveTo(width * 0.76f, height); cubicTo(width * 0.67f, height * 0.87f, width * 0.58f, height * 0.91f, width * 0.535f, height) }
                        drawPath(rootR1, Color(0xFF080C06), style = Stroke(width = 13.5f, cap = StrokeCap.Round))

                        // === HANGING VINES ===
                        for (i in 0 until 9) {
                            val vx = width * (0.08f + i * 0.105f) + sin(i * 1.7f) * 15f
                            val vLen = height * (0.25f + (i % 3) * 0.13f)
                            val vinePath = Path().apply {
                                moveTo(vx, 0f)
                                for (seg in 0..12) { val t = seg / 12f; lineTo(vx + sin(t * Math.PI.toFloat() * 1.5f + i * 0.9f) * (10f + i % 3 * 6f), vLen * t) }
                            }
                            drawPath(vinePath, Color(0xFF0C1E0A).copy(alpha = 0.85f), style = Stroke(width = 2f + (i % 2), cap = StrokeCap.Round))
                            for (n in 1..2) { val t2 = n * 0.35f; val nx = vx + sin(t2 * Math.PI.toFloat() * 1.5f + i * 0.9f) * (10f + i % 3 * 6f); drawOval(Color(0xFF0F2210), topLeft = Offset(nx - 8f, vLen * t2 - 4f), size = androidx.compose.ui.geometry.Size(16f, 8f)) }
                        }

                        // === SLOTH — loops up left trunk every 60 seconds ===
                        val slothPhase = (System.currentTimeMillis() % 60000L).toFloat() / 60000f
                        val slothX = width * 0.17f
                        val slothY = height * (0.90f - slothPhase * 0.78f)
                        val sArmSway = sin(System.currentTimeMillis().toFloat() / 1800f) * 4.5f
                        drawLine(Color(0xFF6B4C28), Offset(slothX - 12f, slothY - 12f), Offset(slothX - 28.5f + sArmSway, slothY - 33f), strokeWidth = 7.5f, cap = StrokeCap.Round)
                        drawLine(Color(0xFF6B4C28), Offset(slothX + 12f, slothY - 12f), Offset(slothX + 6f - sArmSway, slothY - 33f), strokeWidth = 7.5f, cap = StrokeCap.Round)
                        drawOval(Color(0xFF5C3E1C), topLeft = Offset(slothX - 19.5f, slothY - 12f), size = androidx.compose.ui.geometry.Size(39f, 28.5f))
                        drawCircle(Color(0xFF6B4C28), radius = 15f, center = Offset(slothX, slothY - 25.5f))
                        drawCircle(Color(0xFF2C1A08).copy(alpha = 0.75f), radius = 7.5f, center = Offset(slothX - 4.5f, slothY - 27f))
                        drawCircle(Color(0xFF2C1A08).copy(alpha = 0.75f), radius = 7.5f, center = Offset(slothX + 4.5f, slothY - 27f))
                        drawCircle(Color(0xFFD8AA7A).copy(alpha = 0.8f), radius = 3f, center = Offset(slothX - 4.5f, slothY - 27f))
                        drawCircle(Color(0xFFD8AA7A).copy(alpha = 0.8f), radius = 3f, center = Offset(slothX + 4.5f, slothY - 27f))

                        // === RIGHT TREE SLOTH WITH BABY — 30 second offset ===
                        val rSlothPhase = ((System.currentTimeMillis() + 30000L) % 60000L).toFloat() / 60000f
                        val rSlothX = width * 0.83f
                        val rSlothY = height * (0.90f - rSlothPhase * 0.78f)
                        val rArmSway = sin(System.currentTimeMillis().toFloat() / 1800f) * 4.5f
                        // Arms gripping right trunk (mirrored from left)
                        drawLine(Color(0xFF6B4C28), Offset(rSlothX + 12f, rSlothY - 12f), Offset(rSlothX + 28.5f - rArmSway, rSlothY - 33f), strokeWidth = 7.5f, cap = StrokeCap.Round)
                        drawLine(Color(0xFF6B4C28), Offset(rSlothX - 12f, rSlothY - 12f), Offset(rSlothX - 6f + rArmSway, rSlothY - 33f), strokeWidth = 7.5f, cap = StrokeCap.Round)
                        // Baby body (on parent's back, peeking out to the left toward screen center)
                        drawOval(Color(0xFF6B4C28), topLeft = Offset(rSlothX - 36f, rSlothY - 9f), size = androidx.compose.ui.geometry.Size(21f, 16.5f))
                        // Baby tiny arms clinging to parent
                        drawLine(Color(0xFF5A3818), Offset(rSlothX - 30f, rSlothY - 12f), Offset(rSlothX - 19.5f, rSlothY - 7.5f), strokeWidth = 3f, cap = StrokeCap.Round)
                        drawLine(Color(0xFF5A3818), Offset(rSlothX - 30f, rSlothY - 1.5f), Offset(rSlothX - 19.5f, rSlothY + 1.5f), strokeWidth = 3f, cap = StrokeCap.Round)
                        // Parent body (draws over inner edge of baby body for layered look)
                        drawOval(Color(0xFF5C3E1C), topLeft = Offset(rSlothX - 19.5f, rSlothY - 12f), size = androidx.compose.ui.geometry.Size(39f, 28.5f))
                        // Baby head (visible peeking beside parent)
                        drawCircle(Color(0xFF7A5A32), radius = 9f, center = Offset(rSlothX - 28.5f, rSlothY - 22.5f))
                        drawCircle(Color(0xFF2C1A08).copy(alpha = 0.7f), radius = 4.5f, center = Offset(rSlothX - 30f, rSlothY - 24f))
                        drawCircle(Color(0xFFD8AA7A).copy(alpha = 0.75f), radius = 2f, center = Offset(rSlothX - 30f, rSlothY - 24f))
                        // Parent head
                        drawCircle(Color(0xFF6B4C28), radius = 15f, center = Offset(rSlothX, rSlothY - 25.5f))
                        drawCircle(Color(0xFF2C1A08).copy(alpha = 0.75f), radius = 7.5f, center = Offset(rSlothX - 4.5f, rSlothY - 27f))
                        drawCircle(Color(0xFF2C1A08).copy(alpha = 0.75f), radius = 7.5f, center = Offset(rSlothX + 4.5f, rSlothY - 27f))
                        drawCircle(Color(0xFFD8AA7A).copy(alpha = 0.8f), radius = 3f, center = Offset(rSlothX - 4.5f, rSlothY - 27f))
                        drawCircle(Color(0xFFD8AA7A).copy(alpha = 0.8f), radius = 3f, center = Offset(rSlothX + 4.5f, rSlothY - 27f))

                        // === RANDOM JUNGLE ANIMAL ===
                        val jAnimal = activeJungleAnimal
                        if (jAnimal != null) {
                            val aProg = ((System.currentTimeMillis() - jAnimal.startTime).toFloat() / jAnimal.duration).coerceIn(0f, 1f)
                            val aDir = if (jAnimal.fromLeft) 1f else -1f
                            when (jAnimal.type) {
                                "monkey" -> {
                                    val mx = if (jAnimal.fromLeft) -50f + aProg * (width + 100f) else width + 50f - aProg * (width + 100f)
                                    val mHandY = height * 0.42f + sin(aProg * Math.PI.toFloat() * 5f) * height * 0.045f
                                    val mBodyY = mHandY + 45f
                                    val swingLeg = sin(aProg * Math.PI.toFloat() * 9f) * 15f
                                    drawLine(Color(0xFF1A3A12), Offset(mx, 0f), Offset(mx, mHandY), strokeWidth = 4.5f, cap = StrokeCap.Round)
                                    drawLine(Color(0xFF5A3E1E), Offset(mx, mHandY), Offset(mx, mBodyY - 15f), strokeWidth = 6f, cap = StrokeCap.Round)
                                    drawLine(Color(0xFF5A3E1E), Offset(mx - 7.5f, mBodyY + 15f), Offset(mx - 7.5f + swingLeg, mBodyY + 36f), strokeWidth = 6f, cap = StrokeCap.Round)
                                    drawLine(Color(0xFF5A3E1E), Offset(mx + 7.5f, mBodyY + 15f), Offset(mx + 7.5f - swingLeg, mBodyY + 36f), strokeWidth = 6f, cap = StrokeCap.Round)
                                    drawOval(Color(0xFF4A3015), topLeft = Offset(mx - 16.5f, mBodyY - 13.5f), size = androidx.compose.ui.geometry.Size(33f, 33f))
                                    val mTailPath = Path().apply {
                                        moveTo(mx - 4.5f * aDir, mBodyY + 15f)
                                        cubicTo(mx - 33f * aDir, mBodyY + 24f, mx - 45f * aDir, mBodyY + 6f, mx - 36f * aDir, mBodyY - 12f)
                                    }
                                    drawPath(mTailPath, Color(0xFF4A3015), style = Stroke(width = 6f, cap = StrokeCap.Round))
                                    drawCircle(Color(0xFF5A3E1E), radius = 16.5f, center = Offset(mx + 3f * aDir, mBodyY - 27f))
                                    drawOval(Color(0xFF8A6840), topLeft = Offset(mx - 4.5f + 3f * aDir, mBodyY - 31.5f), size = androidx.compose.ui.geometry.Size(15f, 12f))
                                    drawCircle(Color(0xFFDDCC99).copy(alpha = 0.85f), radius = 3.3f, center = Offset(mx - 3f + 3f * aDir, mBodyY - 30f))
                                    drawCircle(Color(0xFFDDCC99).copy(alpha = 0.85f), radius = 3.3f, center = Offset(mx + 4.5f + 3f * aDir, mBodyY - 30f))
                                    drawCircle(Color(0xFF150A00), radius = 1.65f, center = Offset(mx - 3f + 3f * aDir, mBodyY - 30f))
                                    drawCircle(Color(0xFF150A00), radius = 1.65f, center = Offset(mx + 4.5f + 3f * aDir, mBodyY - 30f))
                                }
                                "jaguar" -> {
                                    val jY = height * 0.82f
                                    val jX = when {
                                        aProg < 0.4f -> { val p = aProg / 0.4f; if (jAnimal.fromLeft) -60f + p * (width * 0.5f + 60f) else width + 60f - p * (width * 0.5f + 60f) }
                                        aProg < 0.6f -> width * 0.5f
                                        else -> { val p = (aProg - 0.6f) / 0.4f; if (jAnimal.fromLeft) width * 0.5f + p * (width * 0.5f + 60f) else width * 0.5f - p * (width * 0.5f + 60f) }
                                    }
                                    val lookFactor = if (aProg >= 0.4f && aProg <= 0.6f) { val t = (aProg - 0.4f) / 0.2f; if (t < 0.5f) t * 2f else (1f - t) * 2f } else 0f
                                    val walking = aProg < 0.4f || aProg > 0.6f
                                    val walkWave = if (walking) sin(aProg * 25f) else 0f
                                    val lFront = if (walking) sin(aProg * 26f) * 10.5f else 0f
                                    val jTailPath = Path().apply {
                                        moveTo(jX - 42f * aDir, jY + 3f)
                                        cubicTo(jX - 66f * aDir, jY - 12f + walkWave * 7.5f, jX - 78f * aDir, jY - 33f, jX - 69f * aDir, jY - 54f)
                                    }
                                    drawPath(jTailPath, Color(0xFF8A6428), style = Stroke(width = 7.5f, cap = StrokeCap.Round))
                                    drawLine(Color(0xFF7A5820), Offset(jX - 24f * aDir, jY + 9f), Offset(jX - 24f * aDir - lFront, jY + 40.5f), strokeWidth = 10.5f, cap = StrokeCap.Round)
                                    drawLine(Color(0xFF7A5820), Offset(jX - 9f * aDir, jY + 9f), Offset(jX - 9f * aDir + lFront, jY + 40.5f), strokeWidth = 10.5f, cap = StrokeCap.Round)
                                    drawLine(Color(0xFF7A5820), Offset(jX + 9f * aDir, jY + 9f), Offset(jX + 9f * aDir - lFront, jY + 40.5f), strokeWidth = 10.5f, cap = StrokeCap.Round)
                                    drawLine(Color(0xFF7A5820), Offset(jX + 24f * aDir, jY + 9f), Offset(jX + 24f * aDir + lFront, jY + 40.5f), strokeWidth = 10.5f, cap = StrokeCap.Round)
                                    drawOval(Color(0xFF8A6428), topLeft = Offset(jX - 45f, jY - 18f), size = androidx.compose.ui.geometry.Size(90f, 36f))
                                    for (s in 0..3) {
                                        val sx = jX - 27f + s * 18f; val spY = jY - 3f + sin(s.toFloat()) * 6f
                                        drawCircle(Color(0xFF2A1808).copy(alpha = 0.75f), radius = 7.5f, center = Offset(sx, spY))
                                        drawCircle(Color(0xFF9A7432).copy(alpha = 0.4f), radius = 3.3f, center = Offset(sx, spY))
                                    }
                                    val headCX = jX + 45f * aDir * (1f - lookFactor * 0.25f)
                                    val headCY = jY - 12f + walkWave * 3f
                                    drawCircle(Color(0xFF8A6428), radius = 22.5f, center = Offset(headCX, headCY))
                                    drawCircle(Color(0xFF6A4818), radius = 7.5f, center = Offset(headCX + 12f * aDir - 9f * lookFactor, headCY - 19.5f))
                                    drawCircle(Color(0xFF6A4818), radius = 7.5f, center = Offset(headCX - 7.5f * aDir + 9f * lookFactor, headCY - 19.5f))
                                    drawOval(Color(0xFF7A5820), topLeft = Offset(headCX + 7.5f * aDir * (1f - lookFactor) - 10.5f, headCY - 4.5f), size = androidx.compose.ui.geometry.Size(21f, 13.5f))
                                    drawCircle(Color(0xFFD4AA00).copy(alpha = 0.9f), radius = 5.25f, center = Offset(headCX + 6f * aDir, headCY - 7.5f))
                                    drawCircle(Color(0xFF0A0500), radius = 2.7f, center = Offset(headCX + 6f * aDir, headCY - 7.5f))
                                    if (lookFactor > 0.1f) {
                                        drawCircle(Color(0xFFD4AA00).copy(alpha = lookFactor * 0.9f), radius = 5.25f, center = Offset(headCX - 7.5f * aDir, headCY - 7.5f))
                                        drawCircle(Color(0xFF0A0500), radius = 2.7f * lookFactor, center = Offset(headCX - 7.5f * aDir, headCY - 7.5f))
                                    }
                                }
                                "toucan" -> {
                                    val tx = if (jAnimal.fromLeft) -90f + aProg * (width + 180f) else width + 90f - aProg * (width + 180f)
                                    val ty = height * 0.35f + sin(aProg * Math.PI.toFloat() * 7f) * height * 0.02f
                                    val wingFlap = sin(aProg * Math.PI.toFloat() * 15f)
                                    val wingSpread = 42f + wingFlap * 21f
                                    val wPath = Path().apply {
                                        moveTo(tx - 7.5f * aDir, ty - 3f)
                                        cubicTo(tx - 12f * aDir, ty - wingSpread, tx - 33f * aDir, ty - wingSpread * 0.6f, tx - wingSpread * aDir, ty + 12f)
                                        cubicTo(tx - 27f * aDir, ty + 18f, tx - 12f * aDir, ty + 6f, tx - 7.5f * aDir, ty - 3f)
                                    }
                                    drawPath(wPath, Color(0xFF1A1A18))
                                    drawOval(Color(0xFF1A1A18), topLeft = Offset(tx - 27f, ty - 15f), size = androidx.compose.ui.geometry.Size(54f, 30f))
                                    drawOval(Color(0xFFF0ECD0), topLeft = Offset(tx - 12f + 9f * aDir, ty - 10.5f), size = androidx.compose.ui.geometry.Size(21f, 16.5f))
                                    drawOval(Color(0xFFBB2000), topLeft = Offset(tx - 21f * aDir - 7.5f, ty + 6f), size = androidx.compose.ui.geometry.Size(18f, 10.5f))
                                    drawCircle(Color(0xFF1A1A18), radius = 18f, center = Offset(tx + 21f * aDir, ty - 6f))
                                    val bBaseX = tx + 21f * aDir
                                    val bTipX = tx + 69f * aDir
                                    val beakPath = Path().apply {
                                        moveTo(bBaseX, ty - 12f)
                                        lineTo(bTipX, ty - 6f)
                                        lineTo(bTipX, ty)
                                        lineTo(bBaseX + 12f * aDir, ty + 6f)
                                        close()
                                    }
                                    drawPath(beakPath, Color(0xFFE87800))
                                    drawLine(Color(0xFFFFCC00), Offset(bBaseX, ty - 3f), Offset(bTipX, ty - 3f), strokeWidth = 3f)
                                    drawCircle(Color.White, radius = 6.75f, center = Offset(tx + 16.5f * aDir, ty - 9f))
                                    drawCircle(Color(0xFF0A0500), radius = 4.2f, center = Offset(tx + 16.5f * aDir, ty - 9f))
                                    drawCircle(Color.White.copy(alpha = 0.5f), radius = 1.5f, center = Offset(tx + 18f * aDir, ty - 10.5f))
                                }
                            }
                        }

                        // === GROUND MIST ===
                        for (i in 0..3) {
                            val mistY = height * (0.74f + i * 0.09f)
                            val mistAlpha = 0.06f + sin(waveOffset * 0.35f + i * 1.8f) * 0.018f
                            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFAAC8B0).copy(alpha = mistAlpha), Color.Transparent), startY = mistY - 28f, endY = mistY + 28f), size = size)
                        }

                        // === DENSE CANOPY AT TOP ===
                        val capRnd = Random(7)
                        for (i in 0 until 24) { val cx2 = (capRnd.nextFloat() * 1.5f - 0.25f) * width; val cr = 45f + capRnd.nextFloat() * 105f; val cy2 = capRnd.nextFloat() * height * 0.15f; drawCircle(Color(0xFF050B04), radius = cr, center = Offset(cx2, cy2)) }
                        drawRect(Color(0xFF040A03), size = androidx.compose.ui.geometry.Size(width, height * 0.07f))

                        // === RAIN — angled, filtered through canopy ===
                        val ctJ = System.currentTimeMillis()
                        raindrops.forEach { drop ->
                            drop.y += drop.speed * 0.88f
                            val rx = drop.x * width + drop.y * height * 0.06f
                            if (!drop.isForeground) drawLine(Color(0xFF78B088).copy(alpha = 0.20f), Offset(rx, drop.y * height), Offset(rx + 2f, drop.y * height + 20f), strokeWidth = 1.5f)
                        }
                        for (i in 0 until 14) {
                            val dripX = (i + 1) * width / 15f + sin(cloudTime * 0.07f + i * 0.75f) * 16f
                            val dripPhase = ((ctJ % 3800L).toFloat() / 3800f + i * 0.071f) % 1f
                            drawCircle(Color(0xFF88BEA0).copy(alpha = (1f - dripPhase) * 0.58f), radius = 2.8f, center = Offset(dripX, height * 0.08f + dripPhase * height * 0.40f))
                        }
                        raindrops.forEach { if (it.isForeground) { val rx = it.x * width + it.y * height * 0.06f; drawLine(Color(0xFF98C4AE).copy(alpha = 0.42f), Offset(rx, it.y * height), Offset(rx + 3f, it.y * height + 36f), strokeWidth = 2.5f) } }

                        // === FIREFLIES ===
                        for (i in 0 until 10) {
                            val fx = (sin(cloudTime * 0.022f + i * 1.4f) * 0.42f + 0.5f) * width
                            val fy = height * (0.62f + (i % 5) * 0.07f)
                            val fa = ((sin(cloudTime * 0.09f + i * 0.65f) + 1f) / 2f) * 0.22f
                            drawCircle(Color(0xFF80FF70).copy(alpha = fa), radius = 5f, center = Offset(fx, fy))
                            drawCircle(Color(0xFF50CC40).copy(alpha = fa * 0.35f), radius = 13f, center = Offset(fx, fy))
                        }
                    }
                }
            } else if (currentScape == "Psychedelic") {
                drawRect(Color(0xFF0D0D1A))
                for (i in 0 until 10) {
                    val hue = (waveOffset * 50f + i * 36f) % 360f; val sizeFactor = (sin(waveOffset + i) + 1f) / 2f
                    drawRect(color = Color.hsv(hue, 0.7f, 0.8f).copy(alpha = 0.1f), topLeft = Offset(width * (1f - sizeFactor) / 2f, height * (1f - sizeFactor) / 2f), size = androidx.compose.ui.geometry.Size(width * sizeFactor, height * sizeFactor))
                    drawCircle(color = Color.hsv((hue + 180f) % 360f, 0.8f, 1f).copy(alpha = 0.2f), radius = (waveOffset * 100f + i * 150f) % (width.coerceAtLeast(height)), center = center, style = Stroke(width = 20f))
                }
                val currentTime = System.currentTimeMillis()
                activeSpirals.removeIf { currentTime - it.startTime > it.duration }
                activeSpirals.forEach { spiral ->
                    val progress = (currentTime - spiral.startTime).toFloat() / spiral.duration; val alpha = 1f - progress; val spiralPath = Path()
                    val rotation = if (spiral.isClockwise) progress * 5f else -progress * 5f
                    for (angle in 0 until 720 step 5) {
                        val rad = Math.toRadians(angle.toDouble()).toFloat()
                        val r = (angle.toFloat() / 720f) * 200f * (1f + progress)
                        val currentAngle = if (spiral.isClockwise) rad + rotation else -rad + rotation
                        val px = spiral.center.x + cos(currentAngle) * r
                        val py = spiral.center.y + sin(currentAngle) * r
                        if (angle == 0) spiralPath.moveTo(px, py) else spiralPath.lineTo(px, py)
                    }
                    drawPath(path = spiralPath, color = spiral.color.copy(alpha = alpha), style = Stroke(width = 4f))
                }
            } else if (currentScape == "Space") {
                drawRect(Color.Black)
                stars.forEach {
                    if (spaceDirectionForward) { it.z -= spaceSpeed.speedValue; if (it.z < 0.1f) { it.x = Random.nextFloat() * 2 - 1; it.y = Random.nextFloat() * 2 - 1; it.z = 1f } } else { it.z += spaceSpeed.speedValue; if (it.z > 1f) { it.x = Random.nextFloat() * 2 - 1; it.y = Random.nextFloat() * 2 - 1; it.z = 0.1f } }
                    val k = 128f / it.z; val px = it.x * k + width / 2; val py = it.y * k + height / 2
                    if (px > 0 && px < width && py > 0 && py < height) drawCircle(color = Color.White, radius = (1f - it.z).pow(2) * 5f, center = Offset(px, py))
                }
            } else if (currentScape == "Wormhole") {
                drawRect(Color.Black)
                val currentTime = System.currentTimeMillis()
                val currentCircles = wormholeCircles.toList()
                
                // Boundaries logic refined: clamp center based on maximum potential radius
                val baseRadius = width.coerceAtLeast(height) * 0.166f
                val maxRadius = baseRadius * 3f

                currentCircles.forEach { circle ->
                    val elapsed = currentTime - circle.startTime; val progress = elapsed.toFloat() / circle.duration; val alpha = (1f - progress).coerceIn(0f, 1f)
                    if (alpha > 0) drawCircle(color = Color.hsv(circle.hue, 0.7f, 1f).copy(alpha = alpha), radius = baseRadius * (1f + progress * 2f), center = circle.center, style = Stroke(width = 10f * alpha))
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.border(1.dp, PrimaryCyber, RoundedCornerShape(8.dp)).size(40.dp), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { showScapeMenu = true }, modifier = Modifier.size(32.dp)) { Icon(imageVector = Icons.Default.Landscape, contentDescription = "Change Scape", tint = PrimaryCyber, modifier = Modifier.size(20.dp)) }
                        DropdownMenu(expanded = showScapeMenu, onDismissRequest = { showScapeMenu = false }, modifier = Modifier.background(DarkSurface)) {
                            scapes.forEachIndexed { index, scape -> DropdownMenuItem(text = { Text(scape, color = TextColor) }, onClick = { currentScapeIndex = index; showScapeMenu = false }) }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp)); Text("Calm Scapes", color = PrimaryCyber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(onClick = { showSongMenu = true }) { Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Change Song", tint = TextColor) }
                        DropdownMenu(expanded = showSongMenu, onDismissRequest = { showSongMenu = false }, modifier = Modifier.background(DarkSurface)) {
                            allSongs.forEachIndexed { index, song ->
                                DropdownMenuItem(text = { val displayName = song.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
                                    Text(displayName, color = if (currentSongIndex == index) PrimaryCyber else TextColor) }, onClick = { currentSongIndex = index; showSongMenu = false })
                            }
                        }
                    }
                    IconButton(onClick = { isMusicPlaying = !isMusicPlaying }) { Icon(imageVector = if (isMusicPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff, contentDescription = "Toggle Music", tint = TextColor) }
                    IconButton(onClick = onExit) { Icon(imageVector = Icons.Default.Close, contentDescription = "Exit", tint = TextColor) }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 64.dp)) {
                if (currentScape == "Space") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showSpeedMenu = true }) { Icon(Icons.Default.Speed, contentDescription = null, tint = PrimaryCyber) }
                    }
                    DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }, modifier = Modifier.background(DarkSurface)) {
                        SpaceSpeed.entries.forEach { speed -> DropdownMenuItem(text = { Text(speed.label, color = TextColor) }, onClick = { spaceSpeed = speed; showSpeedMenu = false }) }
                    }
                } else if (currentScape == "Wormhole") {
                    IconButton(onClick = { showSpeedMenu = true }) { Icon(Icons.Default.Speed, contentDescription = null, tint = PrimaryCyber) }
                    DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }, modifier = Modifier.background(DarkSurface)) {
                        SpaceSpeed.entries.forEach { speed -> DropdownMenuItem(text = { Text(speed.label, color = TextColor) }, onClick = { spaceSpeed = speed; showSpeedMenu = false }) }
                    }
                } else if (currentScape == "Rain") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            IconButton(onClick = { showRainMenu = true }) { Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PrimaryCyber) }
                            DropdownMenu(expanded = showRainMenu, onDismissRequest = { showRainMenu = false }, modifier = Modifier.background(DarkSurface)) {
                                RainIntensity.entries.forEach { intensity -> DropdownMenuItem(text = { Text(intensity.label, color = if (rainIntensity == intensity) PrimaryCyber else TextColor) }, onClick = { rainIntensity = intensity; showRainMenu = false }) }
                            }
                        }
                        Box {
                            TextButton(onClick = { showRainVariationMenu = true }) { Text(rainVariation.label, color = PrimaryCyber, fontSize = 12.sp) }
                            DropdownMenu(expanded = showRainVariationMenu, onDismissRequest = { showRainVariationMenu = false }, modifier = Modifier.background(DarkSurface)) {
                                RainVariation.entries.forEach { v -> DropdownMenuItem(text = { Text(v.label, color = if (rainVariation == v) PrimaryCyber else TextColor) }, onClick = { rainVariation = v; showRainVariationMenu = false }) }
                            }
                        }
                    }
                }
            }
        }
    }
}
