package com.acataleptic.meditations.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acataleptic.meditations.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Element(val label: String, val color: Color) {
    FIRE("Fire", Color(0xFFFF4500)),
    WATER("Water", Color(0xFF0077BE)),
    EARTH("Earth", Color(0xFF8B4513)),
    WIND("Wind", Color(0xFFA5F2F3)),
    SPIRIT("Spirit", Color(0xFFE94560)),
    GOOD("Good", Color(0xFFFFFF00)),
    EVIL("Evil", Color(0xFF4B0082))
}

data class ElementInstance(
    val id: Int,
    var position: Offset,
    var velocity: Offset,
    val element: Element,
    var mergeCount: Int = 1
)

data class MergerCreature(
    val id: Int,
    var position: Offset,
    var velocity: Offset,
    val color: Color,
    val startTime: Long,
    var radius: Float = 100f
)

data class Particle(
    val position: Offset,
    val velocity: Offset,
    val color: Color,
    val startTime: Long,
    val duration: Long = 1500L
)

@Composable
fun MergerGame(onExit: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val elements = remember { mutableStateListOf<ElementInstance>() }
    val creatures = remember { mutableStateListOf<MergerCreature>() }
    val ripples = remember { mutableStateListOf<Ripple>() }
    val particles = remember { mutableStateListOf<Particle>() }
    
    var isMusicPlaying by remember { mutableStateOf(true) }
    val allSongs = listOf("chasm", "desert_oasis", "tranquil_sea", "frozen_caverns", "lavender_skies", "celestial_drift", "late_light_loops")
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

    val infiniteTransition = rememberInfiniteTransition(label = "mergerTicker")
    val ticker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(16, easing = LinearEasing)),
        label = "ticker"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Define safe bounds for generation
        val safeTop = 150f * density
        val safeBottom = height - 100f * density
        val safeLeft = 50f * density
        val safeRight = width - 50f * density

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val touchPos = change.position
                    
                    val draggedIndex = elements.indexOfFirst { instance ->
                        val radius = (30f + instance.mergeCount * 10f) * density
                        (instance.position - touchPos).getDistance() < radius
                    }

                    if (draggedIndex != -1) {
                        val instance = elements[draggedIndex]
                        val newPos = instance.position + dragAmount
                        instance.position = Offset(
                            newPos.x.coerceIn(0f, width),
                            newPos.y.coerceIn(0f, height)
                        )
                        instance.velocity = Offset.Zero
                        
                        val currentElements = elements.toList()
                        val other = currentElements.find { it.id != instance.id && (it.position - instance.position).getDistance() < (30f + it.mergeCount * 10f) * density }
                        
                        if (other != null) {
                            val totalMerge = instance.mergeCount + other.mergeCount
                            val mergePos = instance.position
                            val element = instance.element
                            
                            elements.remove(other)
                            elements.remove(instance)
                            
                            if (totalMerge >= 5) {
                                val randomAngle = Random.nextFloat() * 2 * Math.PI.toFloat()
                                val velocity = Offset(cos(randomAngle) * 5f, sin(randomAngle) * 5f)
                                creatures.add(MergerCreature(Random.nextInt(), mergePos, velocity, element.color, System.currentTimeMillis()))
                            } else {
                                val randomAngle = Random.nextFloat() * 2 * Math.PI.toFloat()
                                val velocity = Offset(cos(randomAngle) * 6f, sin(randomAngle) * 6f)
                                elements.add(ElementInstance(Random.nextInt(), mergePos, velocity, element, totalMerge))
                            }
                        } else {
                            elements[draggedIndex] = instance.copy(position = instance.position, velocity = instance.velocity)
                        }
                    }
                }
            }
        ) {
            val _t = ticker
            val currentTime = System.currentTimeMillis()

            // Update and draw particles
            val particlesToRemove = mutableListOf<Particle>()
            particles.forEach { particle ->
                val age = currentTime - particle.startTime
                if (age > particle.duration) {
                    particlesToRemove.add(particle)
                } else {
                    val progress = age.toFloat() / particle.duration
                    val currentPos = particle.position + particle.velocity * (age / 16f)
                    drawCircle(
                        color = particle.color.copy(alpha = 1f - progress),
                        radius = 4.dp.toPx(),
                        center = currentPos
                    )
                }
            }
            particles.removeAll(particlesToRemove)

            // Update and Draw Elements
            val elementsToMerge = mutableListOf<Pair<ElementInstance, ElementInstance>>()
            
            elements.forEachIndexed { index, instance ->
                val radius = (30f + instance.mergeCount * 10f) * density
                
                var newX = instance.position.x + instance.velocity.x
                var newY = instance.position.y + instance.velocity.y
                
                if (newX < radius || newX > width - radius) {
                    instance.velocity = instance.velocity.copy(x = -instance.velocity.x)
                    newX = instance.position.x + instance.velocity.x
                }
                if (newY < safeTop || newY > safeBottom) {
                    instance.velocity = instance.velocity.copy(y = -instance.velocity.y)
                    newY = instance.position.y + instance.velocity.y
                }
                instance.position = Offset(newX, newY)

                for (j in index + 1 until elements.size) {
                    val other = elements[j]
                    val otherRadius = (30f + other.mergeCount * 10f) * density
                    if ((instance.position - other.position).getDistance() < (radius + otherRadius) * 0.8f) {
                        elementsToMerge.add(instance to other)
                    }
                }

                // Draw unique shapes for elements
                when (instance.element) {
                    Element.FIRE -> { // Triangle
                        val path = Path()
                        path.moveTo(instance.position.x, instance.position.y - radius)
                        path.lineTo(instance.position.x - radius, instance.position.y + radius)
                        path.lineTo(instance.position.x + radius, instance.position.y + radius)
                        path.close()
                        drawPath(path, instance.element.color)
                    }
                    Element.WATER -> { // Circle
                        drawCircle(color = instance.element.color, radius = radius, center = instance.position)
                    }
                    Element.EARTH -> { // Square
                        drawRect(
                            color = instance.element.color,
                            topLeft = Offset(instance.position.x - radius, instance.position.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                    }
                    Element.WIND -> { // Pentagon
                        val path = Path()
                        for (k in 0 until 5) {
                            val angle = k * (2.0 * Math.PI / 5.0) - Math.PI / 2.0
                            val x = instance.position.x + radius * cos(angle).toFloat()
                            val y = instance.position.y + radius * sin(angle).toFloat()
                            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, instance.element.color)
                    }
                    Element.SPIRIT -> { // Star
                        val path = Path()
                        for (k in 0 until 10) {
                            val angle = k * (Math.PI / 5.0) - Math.PI / 2.0
                            val r = if (k % 2 == 0) radius else radius * 0.4f
                            val x = instance.position.x + r * cos(angle).toFloat()
                            val y = instance.position.y + r * sin(angle).toFloat()
                            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, instance.element.color)
                    }
                    Element.GOOD -> { // Rhombus
                        val path = Path()
                        path.moveTo(instance.position.x, instance.position.y - radius)
                        path.lineTo(instance.position.x + radius * 1.5f, instance.position.y)
                        path.lineTo(instance.position.x, instance.position.y + radius)
                        path.lineTo(instance.position.x - radius * 1.5f, instance.position.y)
                        path.close()
                        drawPath(path, instance.element.color)
                    }
                    Element.EVIL -> { // Diamond
                        val path = Path()
                        path.moveTo(instance.position.x, instance.position.y - radius * 1.5f)
                        path.lineTo(instance.position.x + radius, instance.position.y)
                        path.lineTo(instance.position.x, instance.position.y + radius * 1.5f)
                        path.lineTo(instance.position.x - radius, instance.position.y)
                        path.close()
                        drawPath(path, instance.element.color)
                    }
                }
                drawCircle(color = Color.White.copy(alpha = 0.4f), radius = radius, center = instance.position, style = Stroke(2f))
            }

            // Handle element merging
            elementsToMerge.forEach { (e1, e2) ->
                if (elements.contains(e1) && elements.contains(e2)) {
                    val totalMerge = e1.mergeCount + e2.mergeCount
                    val mergePos = e1.position
                    val element = e1.element
                    elements.remove(e1)
                    elements.remove(e2)
                    if (totalMerge >= 5) {
                        val randomAngle = Random.nextFloat() * 2 * Math.PI.toFloat()
                        val velocity = Offset(cos(randomAngle) * 5f, sin(randomAngle) * 5f)
                        creatures.add(MergerCreature(Random.nextInt(), mergePos, velocity, element.color, System.currentTimeMillis()))
                    } else {
                        val randomAngle = Random.nextFloat() * 2 * Math.PI.toFloat()
                        val velocity = Offset(cos(randomAngle) * 6f, sin(randomAngle) * 6f)
                        elements.add(ElementInstance(Random.nextInt(), mergePos, velocity, element, totalMerge))
                    }
                }
            }

            // Update and draw creatures
            val creaturesToRemove = mutableListOf<MergerCreature>()
            val creaturesToMerge = mutableListOf<Pair<MergerCreature, MergerCreature>>()
            
            creatures.forEachIndexed { index, creature ->
                val age = currentTime - creature.startTime
                if (age > 10000) {
                    creaturesToRemove.add(creature)
                    // BIGGER Firework effect
                    repeat(40) {
                        val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
                        val speed = Random.nextFloat() * 15f + 5f
                        particles.add(Particle(creature.position, Offset(cos(angle) * speed, sin(angle) * speed), creature.color, currentTime))
                    }
                    // BIGGER Ripples
                    ripples.add(Ripple(creature.position, creature.color, Color.White, currentTime, duration = 3000))
                    ripples.add(Ripple(creature.position, creature.color, Color.White, currentTime + 400, duration = 3500))
                    ripples.add(Ripple(creature.position, creature.color, Color.White, currentTime + 800, duration = 4000))
                } else {
                    var newX = creature.position.x + creature.velocity.x
                    var newY = creature.position.y + creature.velocity.y
                    
                    if (newX < creature.radius || newX > width - creature.radius) {
                        creature.velocity = creature.velocity.copy(x = -creature.velocity.x)
                        newX = creature.position.x + creature.velocity.x
                    }
                    if (newY < safeTop || newY > safeBottom) {
                        creature.velocity = creature.velocity.copy(y = -creature.velocity.y)
                        newY = creature.position.y + creature.velocity.y
                    }
                    creature.position = Offset(newX, newY)

                    for (j in index + 1 until creatures.size) {
                        val other = creatures[j]
                        if ((creature.position - other.position).getDistance() < (creature.radius + other.radius) * 0.8f) {
                            creaturesToMerge.add(creature to other)
                        }
                    }

                    drawCircle(color = creature.color, radius = creature.radius, center = creature.position)
                    repeat(12) { j ->
                        val lineAngle = j * (Math.PI.toFloat() / 6f)
                        drawLine(
                            color = creature.color.copy(alpha = 0.6f),
                            start = creature.position,
                            end = creature.position + Offset(cos(lineAngle) * (creature.radius + 30f), sin(lineAngle) * (creature.radius + 30f)),
                            strokeWidth = 5f
                        )
                    }
                }
            }
            
            creaturesToMerge.forEach { (c1, c2) ->
                if (creatures.contains(c1) && creatures.contains(c2)) {
                    val newRadius = (c1.radius + c2.radius) * 0.85f
                    creatures.remove(c1)
                    creatures.remove(c2)
                    creatures.add(c1.copy(radius = newRadius.coerceAtMost(400f), startTime = currentTime))
                }
            }
            creatures.removeAll(creaturesToRemove)

            ripples.removeIf { (currentTime - it.startTime) > it.duration }
            ripples.forEach { ripple ->
                val progress = (currentTime - ripple.startTime).toFloat() / ripple.duration
                if (progress in 0f..1f) {
                    val radius = progress * 600f
                    drawCircle(color = ripple.color.copy(alpha = 1f - progress), radius = radius, center = ripple.center, style = Stroke(6f))
                }
            }
        }

        // UI Header
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Merger Game", color = PrimaryCyber, fontSize = 20.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { currentSongIndex = (currentSongIndex + 1) % allSongs.size }) {
                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next Song", tint = TextColor)
                    }
                    IconButton(onClick = { isMusicPlaying = !isMusicPlaying }) {
                        Icon(imageVector = if (isMusicPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff, contentDescription = "Toggle Music", tint = TextColor)
                    }
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, contentDescription = "Exit", tint = TextColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Element Buttons
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                items(Element.entries) { element ->
                    Button(
                        onClick = { 
                            val randomPos = Offset(
                                Random.nextFloat() * (safeRight - safeLeft) + safeLeft,
                                Random.nextFloat() * (safeBottom - safeTop) + safeTop
                            )
                            val randomAngle = Random.nextFloat() * 2 * Math.PI.toFloat()
                            val velocity = Offset(cos(randomAngle) * 6f, sin(randomAngle) * 6f)
                            elements.add(ElementInstance(Random.nextInt(), randomPos, velocity, element))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = element.color),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        val shapeText = when(element) {
                            Element.FIRE -> "△"
                            Element.WATER -> "○"
                            Element.EARTH -> "□"
                            Element.WIND -> "⬠"
                            Element.SPIRIT -> "☆"
                            Element.GOOD -> "▱"
                            Element.EVIL -> "◇"
                        }
                        Text("$shapeText ${element.label}", color = if (element == Element.WATER || element == Element.WIND || element == Element.GOOD) Color.White else Color.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
