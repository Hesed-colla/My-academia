package com.example.ui.game

import androidx.compose.ui.graphics.Color
import java.util.UUID

// Game Themes
data class PyramidTheme(
    val id: String,
    val name: String,
    val emoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val backgroundColor: Color,
    val starCost: Int,
    val isUnlocked: Boolean = false
)

val GAME_THEMES = listOf(
    PyramidTheme(
        id = "giza",
        name = "Giza Sandstone",
        emoji = "🏜️",
        primaryColor = Color(0xFFE5A93B),
        secondaryColor = Color(0xFFC68B2C),
        accentColor = Color(0xFFFFD700),
        backgroundColor = Color(0xFF1E140A),
        starCost = 0,
        isUnlocked = true
    ),
    PyramidTheme(
        id = "cyber",
        name = "Cyber Neon",
        emoji = "⚡",
        primaryColor = Color(0xFF39FF14),
        secondaryColor = Color(0xFF00FFFF),
        accentColor = Color(0xFFFF007F),
        backgroundColor = Color(0xFF0D021A),
        starCost = 5
    ),
    PyramidTheme(
        id = "frost",
        name = "Arctic Ice",
        emoji = "❄️",
        primaryColor = Color(0xFFE0F7FA),
        secondaryColor = Color(0xFF80DEEA),
        accentColor = Color(0xFF00E5FF),
        backgroundColor = Color(0xFF001F3F),
        starCost = 10
    ),
    PyramidTheme(
        id = "lava",
        name = "Volcano Fire",
        emoji = "🔥",
        primaryColor = Color(0xFFFF3D00),
        secondaryColor = Color(0xFFDD2C00),
        accentColor = Color(0xFFFFEA00),
        backgroundColor = Color(0xFF1B0000),
        starCost = 15
    ),
    PyramidTheme(
        id = "candy",
        name = "Candy Palace",
        emoji = "🍭",
        primaryColor = Color(0xFFF48FB1),
        secondaryColor = Color(0xFFF06292),
        accentColor = Color(0xFFFFEB3B),
        backgroundColor = Color(0xFF2D1620),
        starCost = 20
    )
)

// Represents a stacked block layer in the pyramid
data class StackedBlock(
    val id: String = UUID.randomUUID().toString(),
    val width: Float,
    val centerX: Float,
    val color: Color,
    val layerIndex: Int,
    val isPerfect: Boolean = false
)

// Active target sliding block
data class SlidingBlock(
    var width: Float,
    var centerX: Float,
    var speed: Float,
    var dir: Int // +1 right, -1 left
)

// Sliced piece animation falling down
data class SliceParticle(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    var alpha: Float = 1.0f,
    var rotation: Float = 0f,
    val rotationSpeed: Float = (Math.random() * 8 - 4).toFloat()
)

// Glow/sparkle particles emitted on Perfect landings
data class SparkleParticle(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val color: Color,
    var alpha: Float = 1.0f,
    val gravity: Float = 0.5f
)

// Text showing score multipliers/actions
data class FloatingGameText(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    val text: String,
    val color: Color,
    var alpha: Float = 1.0f,
    val size: Float = 18f
)

// Ambient background effects
data class AmberParticle(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var vy: Float,
    val size: Float,
    val color: Color,
    var alpha: Float = 0.2f + Math.random().toFloat() * 0.5f
)
