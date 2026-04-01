package com.acataleptic.meditations.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

@Composable
fun CalmScapeVisual(
    currentScape: String,
    spaceSpeed: SpaceSpeed = SpaceSpeed.MEDIUM,
    rainIntensity: RainIntensity = RainIntensity.LITTLE,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var spaceDirectionForward by remember { mutableStateOf(true) }
    var sunXFactor by remember { mutableFloatStateOf(0.5f) }
    var sunYFactor by remember { mutableFloatStateOf(0.15f) }
    val activeSeagulls = remember { mutableStateListOf<Seagull>() }

    val raindrops = remember { mutableStateListOf<Raindrop>() }
    var lightningAlpha by remember { mutableFloatStateOf(0f) }
    var strikingCloudIndex by remember { mutableIntStateOf(-1) }

    val activeSpirals = remember { mutableStateListOf<Spiral>() }
    val activeFishJumps = remember { mutableStateListOf<FishJump>() }
    val activeCrabs = remember { mutableStateListOf<Crab>() }
    val wormholeCircles = remember { mutableStateListOf<WormholeCircle>() }
    var lastWormholeCenter by remember { mutableStateOf(Offset.Zero) }
    var targetWormholeCenter by remember { mutableStateOf<Offset?>(null) }
    var wormholeVelocity by remember { mutableStateOf(Offset(5f, 5f)) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }

    val clouds = remember {
        mutableStateListOf<Cloud>().apply {
            repeat(7) {
                add(Cloud(
                    x = Random.nextFloat(),
                    y = 0.12f + Random.nextFloat() * 0.4f,
                    scale = 1.2f + Random.nextFloat() * 1.8f,
                    speedX = (Random.nextFloat() - 0.5f) * 0.001f,
                    speedY = (Random.nextFloat() - 0.5f) * 0.0005f
                ))
            }
        }
    }

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

    LaunchedEffect(currentScape, rainIntensity) {
        while (true) {
            delay(12)
            if (currentScape.contains("Rain") && Random.nextFloat() < rainIntensity.coverage) {
                val isForeground = Random.nextBoolean()
                raindrops.add(Raindrop(
                    x = Random.nextFloat(),
                    y = if (currentScape == "City Rain") 0.06f else -0.05f,
                    speed = if (isForeground) 0.00656f else 0.00337f,
                    isForeground = isForeground
                ))
            }
            raindrops.removeIf { it.y > 1.1f }
        }
    }

    LaunchedEffect(currentScape) {
        if (currentScape == "City Rain") {
            while (true) {
                delay(Random.nextLong(4000, 12000))
                performStrike()
            }
        } else {
            lightningAlpha = 0f
            strikingCloudIndex = -1
        }
    }

    LaunchedEffect(currentScape, screenSize) {
        if (currentScape == "Psychedelic") {
            while (true) {
                delay(500)
                if (screenSize.width > 0 && screenSize.height > 0) {
                    activeSpirals.add(Spiral(
                        center = Offset(Random.nextFloat() * screenSize.width, Random.nextFloat() * screenSize.height),
                        color = Color.hsv(Random.nextFloat() * 360f, 0.8f, 1f),
                        startTime = System.currentTimeMillis(),
                        isClockwise = Random.nextBoolean()
                    ))
                }
            }
        } else {
            activeSpirals.clear()
        }
    }

    LaunchedEffect(currentScape, screenSize) {
        if (currentScape == "Beach") {
            while (true) {
                delay(15000)
                if (screenSize.width > 0 && screenSize.height > 0) {
                    if (Random.nextBoolean()) {
                        val startX = Random.nextFloat() * screenSize.width
                        val startY = (0.4f + Random.nextFloat() * 0.26f) * screenSize.height
                        val jumpWidth = (Random.nextFloat() - 0.5f) * 300f
                        activeFishJumps.add(FishJump(
                            x = startX, y = startY,
                            targetX = startX + jumpWidth, targetY = startY,
                            startTime = System.currentTimeMillis(),
                            color = Color(0xFF81D4FA),
                            maxHeight = 80f + Random.nextFloat() * 100f
                        ))
                    } else {
                        activeCrabs.add(Crab(
                            y = (0.66f + Random.nextFloat() * 0.34f) * screenSize.height,
                            fromLeft = Random.nextBoolean(),
                            startTime = System.currentTimeMillis()
                        ))
                    }
                }
            }
        } else {
            activeFishJumps.clear()
            activeCrabs.clear()
            activeSeagulls.clear()
        }
    }

    LaunchedEffect(currentScape, spaceSpeed, screenSize) {
        if (currentScape == "Wormhole") {
            val baseSpeed = when (spaceSpeed) {
                SpaceSpeed.SLOW -> 4f; SpaceSpeed.MEDIUM -> 10f; SpaceSpeed.FAST -> 20f
            }
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            wormholeVelocity = Offset(cos(angle) * baseSpeed, sin(angle) * baseSpeed)
            if (lastWormholeCenter == Offset.Zero && screenSize.width > 0) {
                lastWormholeCenter = Offset(screenSize.width / 2f, screenSize.height / 2f)
            }
            while (true) {
                val nextX: Float; val nextY: Float
                val currentTarget = targetWormholeCenter
                if (currentTarget != null) {
                    val angleToTarget = atan2(currentTarget.y - lastWormholeCenter.y, currentTarget.x - lastWormholeCenter.x)
                    val step = when (spaceSpeed) { SpaceSpeed.SLOW -> 8f; SpaceSpeed.MEDIUM -> 18f; SpaceSpeed.FAST -> 35f }
                    val dist = (lastWormholeCenter - currentTarget).getDistance()
                    if (dist < step) {
                        nextX = currentTarget.x; nextY = currentTarget.y; targetWormholeCenter = null
                        val a = Random.nextFloat() * 2 * Math.PI.toFloat()
                        wormholeVelocity = Offset(cos(a) * baseSpeed, sin(a) * baseSpeed)
                    } else {
                        nextX = lastWormholeCenter.x + cos(angleToTarget) * step
                        nextY = lastWormholeCenter.y + sin(angleToTarget) * step
                    }
                } else {
                    var trialX = lastWormholeCenter.x + wormholeVelocity.x
                    var trialY = lastWormholeCenter.y + wormholeVelocity.y
                    var newVelX = wormholeVelocity.x; var newVelY = wormholeVelocity.y
                    if (screenSize.width > 0) {
                        val margin = 40f
                        if (trialX < margin || trialX > screenSize.width - margin) { newVelX = -newVelX; trialX = trialX.coerceIn(margin, screenSize.width.toFloat() - margin) }
                        if (trialY < margin || trialY > screenSize.height - margin) { newVelY = -newVelY; trialY = trialY.coerceIn(margin, screenSize.height.toFloat() - margin) }
                    }
                    nextX = trialX; nextY = trialY; wormholeVelocity = Offset(newVelX, newVelY)
                }
                wormholeCircles.add(WormholeCircle(center = Offset(nextX, nextY), hue = (System.currentTimeMillis() / 20f) % 360f, startTime = System.currentTimeMillis()))
                val now = System.currentTimeMillis()
                wormholeCircles.removeAll { now - it.startTime > it.duration }
                lastWormholeCenter = Offset(nextX, nextY)
                delay(when (spaceSpeed) { SpaceSpeed.SLOW -> 80L; SpaceSpeed.MEDIUM -> 40L; SpaceSpeed.FAST -> 16L })
            }
        } else {
            wormholeCircles.clear()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scapeTicker")
    val frameTicker by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(16, easing = LinearEasing)), label = "frameTicker")
    val waveOffset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 2f * Math.PI.toFloat(), animationSpec = infiniteRepeatable(animation = tween(16000, easing = LinearEasing)), label = "wave")
    val tideOffset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 2f * Math.PI.toFloat(), animationSpec = infiniteRepeatable(animation = tween(48000, easing = LinearEasing)), label = "tide")
    val cloudTime by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(animation = tween(1000000, easing = LinearEasing)), label = "cloudTime")
    val sunTimeFactor by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(60000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "sunTime")

    val stars = remember {
        mutableStateListOf<Star>().apply {
            repeat(800) { add(Star(Random.nextFloat() * 2 - 1, Random.nextFloat() * 2 - 1, Random.nextFloat())) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { screenSize = it }
            .pointerInput(currentScape) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    val inSkyArea = currentScape == "Beach" && downPos.y < size.height * 0.33f
                    var dragStarted = false
                    var lastPos = downPos

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (!dragStarted) {
                                when (currentScape) {
                                    "City Rain" -> scope.launch { performStrike() }
                                    "Psychedelic" -> activeSpirals.add(Spiral(center = downPos, color = Color.hsv(Random.nextFloat() * 360f, 0.8f, 1f), startTime = System.currentTimeMillis(), isClockwise = Random.nextBoolean()))
                                    "Wormhole" -> targetWormholeCenter = downPos
                                    "Space" -> spaceDirectionForward = !spaceDirectionForward
                                    "Beach" -> {
                                        if (inSkyArea) {
                                            val count = 2 + Random.nextInt(3)
                                            repeat(count) { i ->
                                                activeSeagulls.add(Seagull(
                                                    y = (0.05f + Random.nextFloat() * 0.24f) * size.height,
                                                    fromLeft = Random.nextBoolean(),
                                                    startTime = System.currentTimeMillis() + i * 400L,
                                                    wingScale = 0.8f + Random.nextFloat() * 0.5f
                                                ))
                                            }
                                        } else if (downPos.y > size.height * 0.66f) {
                                            activeCrabs.add(Crab(y = downPos.y, fromLeft = Random.nextBoolean(), startTime = System.currentTimeMillis()))
                                        } else if (downPos.y > size.height * 0.4f) {
                                            val jumpWidth = (Random.nextFloat() - 0.5f) * 300f
                                            activeFishJumps.add(FishJump(x = downPos.x, y = downPos.y, targetX = downPos.x + jumpWidth, targetY = downPos.y, startTime = System.currentTimeMillis(), color = Color(0xFF81D4FA), maxHeight = 80f + Random.nextFloat() * 100f))
                                        }
                                    }
                                }
                            }
                            break
                        }
                        val delta = change.position - lastPos
                        if (inSkyArea && !dragStarted && delta.getDistance() > 10f) {
                            dragStarted = true
                        }
                        if (inSkyArea && dragStarted) {
                            sunXFactor = (sunXFactor + delta.x / size.width).coerceIn(0.05f, 0.95f)
                            sunYFactor = (sunYFactor + delta.y / size.height).coerceIn(0.03f, 0.30f)
                            change.consume()
                        }
                        lastPos = change.position
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            @Suppress("UNUSED_VARIABLE") val _t = frameTicker

            if (lastWormholeCenter == Offset.Zero) {
                lastWormholeCenter = Offset(width / 2, height / 2)
            }

            if (currentScape == "Beach") {
                val skyColorMidday = listOf(Color(0xFF87CEEB), Color(0xFFE0F6FF))
                val skyColorSunset = listOf(Color(0xFFFF4500), Color(0xFFFFD700))
                val currentSkyColors = skyColorSunset.zip(skyColorMidday) { s, m -> androidx.compose.ui.graphics.lerp(s, m, sunTimeFactor) }
                drawRect(brush = Brush.verticalGradient(currentSkyColors, endY = height * 0.4f), size = size.copy(height = height * 0.4f))
                val sunColor = androidx.compose.ui.graphics.lerp(Color(0xFFFF6347), Color(0xFFFFFACD), sunTimeFactor)
                val sunY = height * sunYFactor
                drawCircle(color = sunColor.copy(alpha = 0.8f), radius = 60f, center = Offset(width * sunXFactor, sunY))
                val sandColor = androidx.compose.ui.graphics.lerp(Color(0xFFC2B280), Color(0xFFEDC9AF), sunTimeFactor)
                drawRect(color = sandColor, topLeft = Offset(0f, height * 0.4f), size = size.copy(height = height * 0.6f))
                val oceanColor = androidx.compose.ui.graphics.lerp(Color(0xFF191970), Color(0xFF0077BE), sunTimeFactor)
                val accentColor = androidx.compose.ui.graphics.lerp(Color(0xFF4B0082), Color(0xFF00A2E8), sunTimeFactor)
                for (i in 4 downTo 0) {
                    val phaseShift = i * (2f * Math.PI.toFloat() / 5f); val individualTideFactor = (sin(tideOffset - phaseShift) + 1f) / 2f
                    val reachMultiplier = 0.1f + (4 - i) * 0.06f; val waveEdgeY = height * 0.4f + (individualTideFactor * height * reachMultiplier)
                    val waterPath = Path().apply { moveTo(0f, height * 0.4f); lineTo(width, height * 0.4f); lineTo(width, waveEdgeY) }
                    for (x in width.toInt() downTo 0 step 15) { val ripple = sin(x * (0.01f + ((4 - i) * 0.001f)) + waveOffset + i) * (5f + ((4 - i) * 4f)); waterPath.lineTo(x.toFloat(), waveEdgeY + ripple) }
                    waterPath.close(); drawPath(path = waterPath, color = if (i % 2 == 0) oceanColor.copy(alpha = 0.2f + (4 - i) * 0.1f) else accentColor.copy(alpha = 0.2f + (4 - i) * 0.1f))
                    if (individualTideFactor > 0.3f) {
                        val foamPath = Path().apply { moveTo(width, waveEdgeY) }
                        for (x in width.toInt() downTo 0 step 10) { val ripple = sin(x * 0.012f + waveOffset * 1.1f + i) * (5f + (4 - i) * 3f); foamPath.lineTo(x.toFloat(), waveEdgeY + ripple) }
                        drawPath(path = foamPath, color = Color.White.copy(alpha = (individualTideFactor * 0.4f).coerceAtMost(0.4f)), style = Stroke(width = (1 + (4 - i)).dp.toPx()))
                    }
                }
                val currentTime = System.currentTimeMillis()
                activeFishJumps.removeIf { currentTime - it.startTime > it.duration }
                activeFishJumps.forEach { fish ->
                    val progress = (currentTime - fish.startTime).toFloat() / fish.duration
                    val currentX = fish.x + (fish.targetX - fish.x) * progress
                    val currentY = fish.y - 4 * fish.maxHeight * progress * (1 - progress)
                    if (progress < 0.2f) { val p = progress / 0.2f; for (j in 0..4) { val angle = Math.toRadians((j * 30 + 30).toDouble()); val dist = 40f * p; drawLine(Color.White.copy(alpha = 1f - p), Offset(fish.x, fish.y), Offset(fish.x + cos(angle).toFloat() * dist, fish.y - sin(angle).toFloat() * dist), strokeWidth = 3f) } }
                    if (progress > 0.8f) { val p = (progress - 0.8f) / 0.2f; for (j in 0..4) { val angle = Math.toRadians((j * 30 + 30).toDouble()); val dist = 40f * p; drawLine(Color.White.copy(alpha = 1f - p), Offset(fish.targetX, fish.targetY), Offset(fish.targetX + cos(angle).toFloat() * dist, fish.targetY - sin(angle).toFloat() * dist), strokeWidth = 3f) } }
                    drawOval(color = fish.color, topLeft = Offset(currentX - 12f, currentY - 6f), size = androidx.compose.ui.geometry.Size(24f, 12f))
                    val tailPath = Path().apply { moveTo(currentX, currentY); if (fish.targetX > fish.x) { lineTo(currentX - 18f, currentY - 8f); lineTo(currentX - 18f, currentY + 8f) } else { lineTo(currentX + 18f, currentY - 8f); lineTo(currentX + 18f, currentY + 8f) }; close() }
                    drawPath(tailPath, fish.color)
                }
                activeCrabs.removeIf { currentTime - it.startTime > it.duration }
                activeCrabs.forEach { crab ->
                    val progress = (currentTime - crab.startTime).toFloat() / crab.duration
                    val currentX = if (crab.fromLeft) progress * (width + 100f) - 50f else (1f - progress) * (width + 100f) - 50f
                    val wiggle = sin(progress * 40f) * 5f
                    drawOval(color = Color(0xFFE57373), topLeft = Offset(currentX - 20f, crab.y - 12f), size = androidx.compose.ui.geometry.Size(40f, 24f))
                    drawCircle(Color.White, 4f, Offset(currentX - 8f, crab.y - 12f)); drawCircle(Color.White, 4f, Offset(currentX + 8f, crab.y - 12f))
                    drawCircle(Color.Black, 2f, Offset(currentX - 8f, crab.y - 13f)); drawCircle(Color.Black, 2f, Offset(currentX + 8f, crab.y - 13f))
                    for (i in 0..2) { val legOffset = i * 8f - 8f; drawLine(Color(0xFFD32F2F), Offset(currentX - 15f, crab.y + legOffset), Offset(currentX - 30f, crab.y + legOffset + wiggle), strokeWidth = 3f); drawLine(Color(0xFFD32F2F), Offset(currentX + 15f, crab.y + legOffset), Offset(currentX + 30f, crab.y + legOffset - wiggle), strokeWidth = 3f) }
                }
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
            } else if (currentScape == "Rain" || currentScape == "City Rain") {
                if (currentScape == "Rain") drawRect(Color(0xFF0D0D1A)) else {
                    drawRect(Color(0xFF050510)); if (lightningAlpha > 0f) drawRect(Color.White.copy(alpha = lightningAlpha * 0.15f))
                    val topCloudColor = if (lightningAlpha > 0.3f) Color.White.copy(alpha = lightningAlpha * 0.8f) else if (lightningAlpha > 0f) Color.LightGray.copy(alpha = 0.5f) else Color(0xFF151525)
                    drawRect(color = topCloudColor, size = androidx.compose.ui.geometry.Size(width, height * 0.04f))
                    for (i in 0..(width / 40f).toInt() + 1) drawCircle(color = topCloudColor, radius = 40f * (0.8f + (sin(i.toFloat() + cloudTime * 0.05f) * 0.2f)), center = Offset(i * 40f * 1.2f, height * 0.04f))
                }
                raindrops.forEach { drop -> drop.y += drop.speed; if (!drop.isForeground) drawLine(color = Color.White.copy(alpha = if (currentScape == "Rain") 0.4f else 0.3f), start = Offset(drop.x * width, drop.y * height), end = Offset(drop.x * width, drop.y * height + (if (currentScape == "Rain") 50f else 30f)), strokeWidth = if (currentScape == "Rain") 2.5f else 2f) }
                if (currentScape == "City Rain") {
                    clouds.forEachIndexed { i, cloud ->
                        cloud.x += cloud.speedX; cloud.y += cloud.speedY
                        val baseRadius = 55f * cloud.scale; val marginX = (baseRadius * 1.5f) / width
                        if (cloud.x < marginX || cloud.x > 1f - marginX) { cloud.speedX *= -1f; cloud.x = cloud.x.coerceIn(marginX, 1f - marginX) }
                        val minY = 0.12f + (baseRadius * 1.2f) / height; val maxY = 0.6f - (baseRadius * 1.2f) / height
                        if (cloud.y < minY || cloud.y > maxY) { cloud.speedY *= -1f; cloud.y = cloud.y.coerceIn(minY, maxY) }
                        for (j in i + 1 until clouds.size) { val other = clouds[j]; val dx = (cloud.x - other.x) * width; val dy = (cloud.y - other.y) * height; val dist = sqrt((dx * dx + dy * dy)); val minDist = (cloud.scale + other.scale) * 55f; if (dist < minDist) { val tempX = cloud.speedX; val tempY = cloud.speedY; cloud.speedX = other.speedX; cloud.speedY = other.speedY; other.speedX = tempX; other.speedY = tempY } }
                        val cloudColor = if (lightningAlpha > 0.3f) Color.White.copy(alpha = lightningAlpha * 0.8f) else if (lightningAlpha > 0f) Color.LightGray.copy(alpha = 0.5f) else Color(0xFF1A1A2E)
                        drawCircle(cloudColor, baseRadius, Offset(cloud.x * width, cloud.y * height)); drawCircle(cloudColor, baseRadius * 0.8f, Offset(cloud.x * width - baseRadius * 0.7f, cloud.y * height + baseRadius * 0.2f)); drawCircle(cloudColor, baseRadius * 0.8f, Offset(cloud.x * width + baseRadius * 0.7f, cloud.y * height + baseRadius * 0.2f)); drawCircle(cloudColor, baseRadius * 0.6f, Offset(cloud.x * width - baseRadius * 1.2f, cloud.y * height + baseRadius * 0.4f)); drawCircle(cloudColor, baseRadius * 0.6f, Offset(cloud.x * width + baseRadius * 1.2f, cloud.y * height + baseRadius * 0.4f))
                        if (i == strikingCloudIndex && lightningAlpha > 0.3f) {
                            val boltPath = Path().apply { moveTo(cloud.x * width, cloud.y * height) }; var curX = cloud.x * width; var curY = cloud.y * height
                            repeat(12) { val r = Random(System.currentTimeMillis() / 150); curX += (r.nextFloat() - 0.5f) * 160f; curY += (height * 0.95f - cloud.y * height) / 12; boltPath.lineTo(curX, curY) }
                            drawPath(path = boltPath, color = Color.White.copy(alpha = lightningAlpha), style = Stroke(width = 6f, cap = StrokeCap.Round)); drawPath(path = boltPath, color = Color.White.copy(alpha = lightningAlpha * 0.4f), style = Stroke(width = 18f, cap = StrokeCap.Round))
                        }
                    }
                    val cityRandom = Random(42)
                    for (i in 0 until 8) {
                        val bWidth = (width / 8) * (0.7f + cityRandom.nextFloat() * 0.4f); val bHeight = height * (0.2f + cityRandom.nextFloat() * 0.35f)
                        val bX = i * (width / 8) + ((width / 8) - bWidth) / 2; val bY = height - bHeight
                        drawRect(color = if (lightningAlpha > 0f) Color(0xFF1A1A25) else Color(0xFF0D0D12), topLeft = Offset(bX, bY), size = androidx.compose.ui.geometry.Size(bWidth, bHeight))
                        for (r in 1 until (bHeight / 25f).toInt() - 1) for (c in 1 until (bWidth / 15f).toInt() - 1) { if (Random((42 + i) * 1000 + r * 100 + c).nextFloat() > 0.6f) drawRect(color = if ((r + c) % 2 == 0) Color(0xFFFFFACD).copy(alpha = 0.7f) else Color(0xFFE0F6FF).copy(alpha = 0.7f), topLeft = Offset(bX + c * 15f, bY + r * 25f), size = androidx.compose.ui.geometry.Size(6f, 10f)) }
                    }
                }
                raindrops.forEach { if (it.isForeground) drawLine(color = Color.White.copy(alpha = if (currentScape == "Rain") 0.8f else 0.7f), start = Offset(it.x * width, it.y * height), end = Offset(it.x * width, it.y * height + (if (currentScape == "Rain") 50f else 45f)), strokeWidth = if (currentScape == "Rain") 4.5f else 4f) }
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
                        val rad = Math.toRadians(angle.toDouble()).toFloat(); val r = (angle.toFloat() / 720f) * 200f * (1f + progress)
                        val currentAngle = if (spiral.isClockwise) rad + rotation else -rad + rotation
                        val px = spiral.center.x + cos(currentAngle) * r; val py = spiral.center.y + sin(currentAngle) * r
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
                val baseRadius = width.coerceAtLeast(height) * 0.166f
                wormholeCircles.toList().forEach { circle ->
                    val elapsed = currentTime - circle.startTime; val progress = elapsed.toFloat() / circle.duration; val alpha = (1f - progress).coerceIn(0f, 1f)
                    if (alpha > 0) drawCircle(color = Color.hsv(circle.hue, 0.7f, 1f).copy(alpha = alpha), radius = baseRadius * (1f + progress * 2f), center = circle.center, style = Stroke(width = 10f * alpha))
                }
            }
        }
    }
}
