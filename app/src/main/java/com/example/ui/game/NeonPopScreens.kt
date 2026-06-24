package com.example.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioSynth
import com.example.data.GameStats
import kotlin.math.*

@Composable
fun NeonGameApp(viewModel: GameViewModel) {
    val screenState by viewModel.currentScreen.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val theme by viewModel.activeTheme.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
    ) {
        // Universal cinematic background grid & stars
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 80f
            // Vertical grid lines
            for (x in 0..(size.width / gridSpacing).toInt()) {
                drawLine(
                    color = theme.primaryColor.copy(alpha = 0.05f),
                    start = Offset(x * gridSpacing, 0f),
                    end = Offset(x * gridSpacing, size.height),
                    strokeWidth = 2f
                )
            }
            // Horizontal grid lines
            for (y in 0..(size.height / gridSpacing).toInt()) {
                drawLine(
                    color = theme.primaryColor.copy(alpha = 0.05f),
                    start = Offset(0f, y * gridSpacing),
                    end = Offset(size.width, y * gridSpacing),
                    strokeWidth = 2f
                )
            }
        }

        // Screen routing selector
        AnimatedContent(
            targetState = screenState,
            transitionSpec = {
                fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "ScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {
                is Screen.MainMenu -> MainMenuScreen(viewModel, stats, theme)
                is Screen.LevelSelect -> LevelSelectScreen(viewModel, stats, theme)
                is Screen.ActivePlay -> ActivePlayScreen(viewModel = viewModel, level = targetScreen.level, theme = theme)
                is Screen.LevelComplete -> LevelCompleteScreen(viewModel, targetScreen.level, targetScreen.score, targetScreen.stars, theme)
                is Screen.GameOver -> GameOverScreen(viewModel, targetScreen.level, targetScreen.score, theme)
                is Screen.Shop -> ShopScreen(viewModel, stats, theme)
            }
        }
    }
}

@Composable
fun MainMenuScreen(viewModel: GameViewModel, stats: GameStats, theme: PyramidTheme) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Header stats panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⭐", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${stats.totalStars} Stars",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🏆", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Best: ${stats.highScore}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Cinematic Brand Logo with glowing bevel stacking simulation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 40.dp)
            ) {
                Text(
                    text = "APEX PYRAMID",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "NEON STACK",
                    color = theme.accentColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 6.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Beautiful abstract mini stack representation
                Box(
                    modifier = Modifier
                        .size(160.dp, 100.dp)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val baseW = 120.dp.toPx()
                        val h = 18.dp.toPx()
                        val centerY = size.height - h/2
                        
                        // Drawn Beveled block layers
                        for (i in 0..3) {
                            val layerW = baseW - (i * 24.dp.toPx())
                            val rectY = centerY - (i * h * 1.15f)
                            drawRect(
                                color = if (i == 3) theme.accentColor else theme.primaryColor.copy(alpha = 1.0f - (i * 0.15f)),
                                topLeft = Offset(size.width / 2 - layerW / 2, rectY - h / 2),
                                size = Size(layerW, h)
                            )
                            // Edge highlight
                            drawRect(
                                color = Color.White.copy(alpha = 0.3f),
                                topLeft = Offset(size.width / 2 - layerW / 2, rectY - h / 2),
                                size = Size(layerW, 2.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Game modes actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        AudioSynth.playStack()
                        viewModel.navigateTo(Screen.LevelSelect)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp)
                        .testTag("dynasty_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = theme.backgroundColor)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "DYNASTY PROGRESSION",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.backgroundColor
                    )
                }

                Button(
                    onClick = {
                        AudioSynth.playStack()
                        viewModel.startLevel(0) // Infinite mode ID
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, theme.accentColor),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp)
                        .testTag("endless_button")
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = theme.accentColor)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ENDLESS ASCENT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.accentColor
                    )
                }

                Button(
                    onClick = {
                        AudioSynth.playStack()
                        viewModel.navigateTo(Screen.Shop)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(54.dp)
                        .testTag("shop_button")
                ) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ARCHITECT'S SHOP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer / Settings
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sound controller button
                Button(
                    onClick = {
                        viewModel.toggleSound()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color.Gray),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (stats.soundEnabled) "🔈 Sound On" else "❌ Muted",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                // Global purge database button
                Text(
                    text = "Purge Stats",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier
                        .clickable {
                            AudioSynth.playSlice()
                            viewModel.resetAllStats()
                        }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun LevelSelectScreen(viewModel: GameViewModel, stats: GameStats, theme: PyramidTheme) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    AudioSynth.playSlice()
                    viewModel.navigateTo(Screen.MainMenu)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Dynasty Monuments",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Select a site to build your legacy pyramid. Keep alignment precise to satisfy the pharaohs.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Level grid choices
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(10) { index ->
                val levelNum = index + 1
                val isUnlocked = levelNum <= stats.unlockedLevel
                val targetHeight = 10 + (levelNum * 3)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable(enabled = isUnlocked) {
                            AudioSynth.playStack()
                            viewModel.startLevel(levelNum)
                        }
                        .testTag("level_card_${levelNum}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) {
                            theme.primaryColor.copy(alpha = 0.15f)
                        } else {
                            Color.DarkGray.copy(alpha = 0.4f)
                        }
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isUnlocked) theme.primaryColor.copy(alpha = 0.5f) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isUnlocked) {
                                Text(
                                    text = "SITE $levelNum",
                                    color = theme.accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Goal: $targetHeight Blocks",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row {
                                    Text(text = "⭐", fontSize = 12.sp)
                                    Text(text = "⭐", fontSize = 12.sp)
                                    Text(text = "⭐", fontSize = 12.sp)
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Site Locked",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivePlayScreen(
    viewModel: GameViewModel,
    level: Int,
    theme: PyramidTheme
) {
    val stats by viewModel.stats.collectAsState()
    val score by viewModel.score.collectAsState()
    val lives by viewModel.lives.collectAsState()
    val combo by viewModel.combo.collectAsState()
    val progressGoal by viewModel.progressGoal.collectAsState()
    val isShieldActive by viewModel.isShieldActive.collectAsState()
    val isSlowMoActive by viewModel.isSlowMoActive.collectAsState()
    val shakeAmplitude by viewModel.shakeAmplitude.collectAsState()

    val slidingBlock by viewModel.slidingBlock.collectAsState()
    val cameraScrollY by viewModel.cameraScrollY.collectAsState()

    val density = LocalDensity.current

    // Smooth screen shaking animation
    val shakeOffset = remember(shakeAmplitude) {
        if (shakeAmplitude > 0f) {
            val angle = Math.random() * 2 * Math.PI
            Offset(
                (cos(angle) * shakeAmplitude).toFloat(),
                (sin(angle) * shakeAmplitude).toFloat()
            )
        } else {
            Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        viewModel.tapToDrop()
                    }
                )
            }
            .testTag("game_tap_area")
    ) {
        // Physical stacking view container (with shake offset)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = shakeOffset.x.dp / 8, y = shakeOffset.y.dp / 8)
        ) {
            // Main game board canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val W = size.width
                val H = size.height
                val scale = W / 1080f
                val baseY = H - with(density) { 320.dp.toPx() }
                val blockHeight = 85f * scale

                // 1. Draw weather particles (falling sand, lava ashes, snow)
                for (ap in viewModel.ambientParticles) {
                    drawCircle(
                        color = ap.color.copy(alpha = ap.alpha),
                        radius = ap.size * scale,
                        center = Offset(ap.x * scale, ap.y * scale)
                    )
                }

                // 2. Draw static stacked tower blocks
                for (block in viewModel.stackedBlocks) {
                    val idx = block.layerIndex
                    // Apply camera dynamic scrolling adjustment
                    val drawY = baseY - (idx - cameraScrollY) * blockHeight

                    // Exclude from draw space if entirely offscreen
                    if (drawY > H + 100f || drawY < -150f) continue

                    val drawW = block.width * scale
                    val drawX = (block.centerX - block.width / 2) * scale

                    // 2.1 Subtle volume bottom drop shadow
                    drawRect(
                        color = Color.Black.copy(alpha = 0.35f),
                        topLeft = Offset(drawX - 4f, drawY + 8f),
                        size = Size(drawW + 8f, blockHeight)
                    )

                    // 2.2 Core block body fill
                    drawRect(
                        color = block.color,
                        topLeft = Offset(drawX, drawY),
                        size = Size(drawW, blockHeight)
                    )

                    // 2.3 Stylish 3D side edge bevel highlights
                    val bevel = 8f * scale
                    drawRect(
                        color = Color.White.copy(alpha = 0.25f),
                        topLeft = Offset(drawX, drawY),
                        size = Size(drawW, bevel)
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.2f),
                        topLeft = Offset(drawX, drawY + blockHeight - bevel),
                        size = Size(drawW, bevel)
                    )

                    // If block is perfectly stacked, draw golden neon corner markers
                    if (block.isPerfect) {
                        drawRect(
                            color = theme.accentColor,
                            topLeft = Offset(drawX, drawY),
                            size = Size(drawW, blockHeight),
                            style = Stroke(width = 3f * scale)
                        )
                    }
                }

                // 3. Draw active horizontal sliding tier block
                slidingBlock?.let { block ->
                    val idx = viewModel.stackedBlocks.size
                    val drawY = baseY - (idx - cameraScrollY) * blockHeight
                    val drawW = block.width * scale
                    val drawX = (block.centerX - block.width / 2) * scale

                    // Sliding glow aura
                    drawRect(
                        color = theme.accentColor.copy(alpha = 0.15f),
                        topLeft = Offset(drawX - 10f, drawY - 10f),
                        size = Size(drawW + 20f, blockHeight + 20f)
                    )

                    // Slider core
                    drawRect(
                        color = theme.accentColor,
                        topLeft = Offset(drawX, drawY),
                        size = Size(drawW, blockHeight)
                    )

                    // Top polished glass sheen
                    drawRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset(drawX, drawY),
                        size = Size(drawW, 6f * scale)
                    )
                }

                // 4. Draw sliced pieces tumbling off
                for (p in viewModel.sliceParticles) {
                    val actualY = p.y * scale + (cameraScrollY * blockHeight)
                    
                    rotate(p.rotation, Offset(p.x * scale + (p.width * scale) / 2, actualY + 15f)) {
                        drawRect(
                            color = p.color.copy(alpha = p.alpha),
                            topLeft = Offset(p.x * scale, actualY),
                            size = Size(p.width * scale, p.height * scale)
                        )
                    }
                }

                // 5. Draw sparkles (gold fountain on Perfect snap)
                for (sp in viewModel.sparkleParticles) {
                    val actualY = sp.y * scale + (cameraScrollY * blockHeight)
                    drawCircle(
                        color = sp.color.copy(alpha = sp.alpha),
                        radius = sp.size * scale,
                        center = Offset(sp.x * scale, actualY)
                    )
                }
            }

            // Draw floating animated game text layers
            Box(modifier = Modifier.fillMaxSize()) {
                viewModel.floatingGameTexts.forEach { textItem ->
                    Text(
                        text = textItem.text,
                        color = textItem.color.copy(alpha = textItem.alpha),
                        fontSize = textItem.size.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .offset(
                                x = with(density) { textItem.x.toDp() },
                                y = with(density) { textItem.y.toDp() }
                            )
                    )
                }
            }
        }

        // Gameplay HUD Interface Overlays (notch safe, edge to edge compatible)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exit button to menu safely
                IconButton(
                    onClick = {
                        AudioSynth.playSlice()
                        viewModel.navigateTo(Screen.MainMenu)
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                // Real-time Heights / Goal
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "TIER HEIGHT",
                        fontSize = 11.sp,
                        color = theme.accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (level == 0) "${viewModel.stackedBlocks.size - 1} layers" else "${viewModel.stackedBlocks.size - 1} / $progressGoal",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }

                // Balance remaining lives indicators
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..3) {
                        Text(
                            text = if (i <= lives) "❤️" else "🖤",
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Top-middle score HUD with perfect combo streak alert
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-50).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SCORE",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$score",
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black
                )

                if (combo > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "COMBO x$combo",
                        color = theme.accentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .background(theme.primaryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Bottom-side Level Goal Progress indicators or Infinite labels
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (level > 0) {
                    val progressRatio = (viewModel.stackedBlocks.size - 1).toFloat() / progressGoal
                    Column(modifier = Modifier.fillMaxWidth(0.9f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PHARAOH MONUMENT PROGRESS",
                                fontSize = 10.sp,
                                color = theme.primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(progressRatio * 100).toInt()}%",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressRatio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = theme.primaryColor,
                            trackColor = Color.DarkGray
                        )
                    }
                } else {
                    Text(
                        text = "🌌 INFINITE ASCENT MONUMENT 🌌",
                        color = theme.accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Powerup panel (Perfect fit, Slomo, Repair)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Perfect Fit Option
                    PowerUpButton(
                        icon = Icons.Default.Add,
                        label = "Perfect Fit",
                        count = stats.shieldCount,
                        active = isShieldActive,
                        enabled = stats.shieldCount > 0,
                        accentColor = Color.Cyan,
                        onClick = { viewModel.triggerShieldPowerup() }
                    )

                    // 2. Slomo Option
                    PowerUpButton(
                        icon = Icons.Default.Star,
                        label = "Time Warp",
                        count = stats.slowMoCount,
                        active = isSlowMoActive,
                        enabled = stats.slowMoCount > 0,
                        accentColor = Color(0xFFD500F9),
                        onClick = { viewModel.triggerSlowMoPowerup() }
                    )

                    // 3. Repair Stack Option
                    PowerUpButton(
                        icon = Icons.Default.Build,
                        label = "Repair Stack",
                        count = stats.bombClearCount,
                        active = false,
                        enabled = stats.bombClearCount > 0,
                        accentColor = Color.Yellow,
                        onClick = { viewModel.triggerBombClearPowerup() }
                    )
                }

                Text(
                    text = "Tap anywhere on top to drop your tower slab!",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PowerUpButton(
    icon: Any,
    label: String,
    count: Int,
    active: Boolean,
    enabled: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = enabled) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (active) accentColor else Color.DarkGray.copy(alpha = 0.6f),
                    CircleShape
                )
                .border(2.dp, if (enabled) accentColor else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onClick,
                enabled = enabled
            ) {
                Icon(
                    imageVector = icon as ImageVector,
                    contentDescription = label,
                    tint = if (active) Color.Black else if (enabled) Color.White else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            // Count badge
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(accentColor, CircleShape)
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = if (enabled) Color.White else Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LevelCompleteScreen(
    viewModel: GameViewModel,
    level: Int,
    score: Int,
    stars: Int,
    theme: PyramidTheme
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🎉",
                fontSize = 55.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "SITE MONUMENT BUILT!",
                color = theme.accentColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Monolithic Level $level Completed",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Stars visual rating
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..3) {
                    val isActive = i <= stars
                    Text(
                        text = if (isActive) "⭐" else "☆",
                        fontSize = if (isActive) 54.sp else 38.sp,
                        color = if (isActive) theme.accentColor else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Score card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL ARCHITECT SCORE",
                        fontSize = 11.sp,
                        color = theme.primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$score",
                        fontSize = 42.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Unlocks Earned", fontSize = 10.sp, color = Color.Gray)
                            Text(text = "+$stars ⭐ stars", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Actions buttons
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        AudioSynth.playSlice()
                        viewModel.navigateTo(Screen.LevelSelect)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(text = "Sites List", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        AudioSynth.playStack()
                        val nextLvl = (level + 1).coerceAtMost(10)
                        viewModel.startLevel(nextLvl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp)
                        .testTag("next_monument_button")
                ) {
                    Text(text = "Next Monument", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GameOverScreen(
    viewModel: GameViewModel,
    level: Int,
    score: Int,
    theme: PyramidTheme
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🌋",
                fontSize = 55.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PYRAMID COLLAPSED!",
                color = Color.Red,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (level == 0) "Slabs misaligned in Infinite Ascent" else "Monument site $level collapsed",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Score card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HEIGHT REACHED",
                        fontSize = 11.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$score score",
                        fontSize = 32.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = {
                        AudioSynth.playStack()
                        viewModel.startLevel(level)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("retry_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = theme.backgroundColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Try Again", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        AudioSynth.playSlice()
                        viewModel.navigateTo(Screen.MainMenu)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("back_to_menu_button")
                ) {
                    Text(text = "Back to Menu", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ShopScreen(
    viewModel: GameViewModel,
    stats: GameStats,
    theme: PyramidTheme
) {
    val unlockedIDs by viewModel.unlockedThemeIDs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    AudioSynth.playSlice()
                    viewModel.navigateTo(Screen.MainMenu)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Architect's Shop",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Balance info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = theme.primaryColor.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "YOUR STARRY WALLET", fontSize = 11.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${stats.totalStars} Stars Earned", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(text = "✨", fontSize = 32.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Select atmospheric visual themes below. Achieve higher pharaoh milestone stars to unlock premium styles automatically!",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Shop items matrix
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(GAME_THEMES) { item ->
                val isUnlocked = unlockedIDs.contains(item.id)
                val isSelected = theme.id == item.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isUnlocked) {
                            AudioSynth.playStack()
                            viewModel.selectTheme(item.id)
                        }
                        .testTag("theme_card_${item.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            item.primaryColor.copy(alpha = 0.25f)
                        } else {
                            Color.DarkGray.copy(alpha = 0.3f)
                        }
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) item.primaryColor else if (isUnlocked) Color.Gray.copy(alpha = 0.4f) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(item.backgroundColor, CircleShape)
                                    .border(1.dp, item.primaryColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = item.emoji, fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = item.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Visual swatch row
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(14.dp, 8.dp).background(item.primaryColor, RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.size(14.dp, 8.dp).background(item.secondaryColor, RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.size(14.dp, 8.dp).background(item.accentColor, RoundedCornerShape(2.dp)))
                                }
                            }
                        }

                        // Buy/Select Actions button
                        Box {
                            if (isSelected) {
                                Text(
                                    text = "SELECTED",
                                    color = item.accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            } else if (isUnlocked) {
                                Button(
                                    onClick = {
                                        AudioSynth.playStack()
                                        viewModel.selectTheme(item.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = item.primaryColor),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "USE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(text = "Need ${item.starCost}", color = Color.LightGray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "⭐", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
