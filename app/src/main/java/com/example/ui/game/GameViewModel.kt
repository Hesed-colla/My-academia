package com.example.ui.game

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSynth
import com.example.data.GameDataModule
import com.example.data.GameStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object MainMenu : Screen()
    object LevelSelect : Screen()
    data class ActivePlay(val level: Int) : Screen()
    data class LevelComplete(val level: Int, val score: Int, val stars: Int) : Screen()
    data class GameOver(val level: Int, val score: Int) : Screen()
    object Shop : Screen()
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameDataModule.getRepository(application)

    val stats: StateFlow<GameStats> = repository.stats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GameStats()
        )

    private val _currentScreen = MutableStateFlow<Screen>(Screen.MainMenu)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Stacking Game Board Elements
    val stackedBlocks = mutableStateListOf<StackedBlock>()
    val sliceParticles = mutableStateListOf<SliceParticle>()
    val sparkleParticles = mutableStateListOf<SparkleParticle>()
    val floatingGameTexts = mutableStateListOf<FloatingGameText>()
    val ambientParticles = mutableStateListOf<AmberParticle>()

    private val _slidingBlock = MutableStateFlow<SlidingBlock?>(null)
    val slidingBlock: StateFlow<SlidingBlock?> = _slidingBlock.asStateFlow()

    private val _cameraScrollY = MutableStateFlow(0f)
    val cameraScrollY: StateFlow<Float> = _cameraScrollY.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score = _score.asStateFlow()

    private val _lives = MutableStateFlow(3)
    val lives = _lives.asStateFlow()

    private val _progressGoal = MutableStateFlow(10)
    val progressGoal = _progressGoal.asStateFlow()

    private val _progressCurrent = MutableStateFlow(0)
    val progressCurrent = _progressCurrent.asStateFlow()

    private val _combo = MutableStateFlow(0)
    val combo = _combo.asStateFlow()

    private val _shakeAmplitude = MutableStateFlow(0f)
    val shakeAmplitude = _shakeAmplitude.asStateFlow()

    // Powerup activation flags
    private val _isShieldActive = MutableStateFlow(false) // "Perfect Fit"
    val isShieldActive = _isShieldActive.asStateFlow()

    private val _isSlowMoActive = MutableStateFlow(false) // "Slow Motion"
    val isSlowMoActive = _isSlowMoActive.asStateFlow()

    private val _isBombClearActive = MutableStateFlow(false) // Just keeping consistent
    val isBombClearActive = _isBombClearActive.asStateFlow()

    // Active visual theme
    private val _activeTheme = MutableStateFlow(GAME_THEMES[0])
    val activeTheme: StateFlow<PyramidTheme> = _activeTheme.asStateFlow()

    // Set of unlocked theme IDs (restored or kept)
    private val _unlockedThemeIDs = MutableStateFlow<Set<String>>(setOf("giza"))
    val unlockedThemeIDs: StateFlow<Set<String>> = _unlockedThemeIDs.asStateFlow()

    private var activeLevel = 0 // 0 means Infinite Stacking Mode, 1..10 is Level Mode
    private var gameLoopJob: Job? = null
    private var baseSlideSpeed = 400f
    private var slowMoTimerMs = 0L

    init {
        // Collect sound preferences and update AudioSynth
        viewModelScope.launch {
            stats.collect {
                AudioSynth.setSoundEnabled(it.soundEnabled)
            }
        }
        
        // Restore purchased themes based on stars or custom settings stored locally
        viewModelScope.launch {
            stats.collect { currentStats ->
                val unlocked = mutableSetOf("giza")
                // Retroactively unlock based on stars or milestones
                if (currentStats.totalStars >= GAME_THEMES[1].starCost) unlocked.add("cyber")
                if (currentStats.totalStars >= GAME_THEMES[2].starCost) unlocked.add("frost")
                if (currentStats.totalStars >= GAME_THEMES[3].starCost) unlocked.add("lava")
                if (currentStats.totalStars >= GAME_THEMES[4].starCost) unlocked.add("candy")
                _unlockedThemeIDs.value = unlocked
            }
        }
    }

    fun navigateTo(screen: Screen) {
        if (screen !is Screen.ActivePlay) {
            stopGame()
        }
        _currentScreen.value = screen
    }

    fun toggleSound() {
        viewModelScope.launch {
            repository.toggleSound()
        }
    }

    fun resetAllStats() {
        viewModelScope.launch {
            repository.resetAll()
            _activeTheme.value = GAME_THEMES[0]
        }
    }

    fun purchasePowerUp(type: String, cost: Int) {
        viewModelScope.launch {
            repository.purchasePowerUp(type, cost)
        }
    }

    fun selectTheme(themeId: String) {
        val theme = GAME_THEMES.find { it.id == themeId } ?: return
        if (_unlockedThemeIDs.value.contains(themeId)) {
            _activeTheme.value = theme
        }
    }

    fun startLevel(level: Int) {
        activeLevel = level
        _score.value = 0
        _lives.value = 3
        _combo.value = 0
        _progressCurrent.value = 0
        _cameraScrollY.value = 0f
        
        // Setup goals
        if (level == 0) {
            _progressGoal.value = 99999 // Infinite Mode high cap
        } else {
            _progressGoal.value = 10 + (level * 3) // e.g. Level 1 needs 13 stacked layers
        }

        stackedBlocks.clear()
        sliceParticles.clear()
        sparkleParticles.clear()
        floatingGameTexts.clear()
        ambientParticles.clear()

        _isShieldActive.value = false
        _isSlowMoActive.value = false
        slowMoTimerMs = 0L

        // Initialize Pyramid base foundation block
        val theme = _activeTheme.value
        stackedBlocks.add(
            StackedBlock(
                width = 650f,
                centerX = 540f,
                color = theme.primaryColor,
                layerIndex = 0
            )
        )

        // Spawn first sliding layer
        spawnSlidingBlock(width = 650f, index = 1)

        // Pre-fill ambient weather particles
        spawnAmbientWeather(30)

        navigateTo(Screen.ActivePlay(level))
        startGameLoop()
    }

    private fun spawnSlidingBlock(width: Float, index: Int) {
        val speedFactor = 1.0f + (activeLevel * 0.15f) + (index * 0.015f)
        val speed = baseSlideSpeed * speedFactor
        val dir = if (index % 2 == 0) 1 else -1
        val startX = if (dir == 1) width / 2 else 1080f - (width / 2)

        _slidingBlock.value = SlidingBlock(
            width = width,
            centerX = startX,
            speed = speed,
            dir = dir
        )
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (true) {
                delay(16) // ~60fps ticks
                val now = System.currentTimeMillis()
                val dt = (now - lastTime) / 1000f
                lastTime = now

                updateGame(dt)
            }
        }
    }

    private fun stopGame() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        _slidingBlock.value = null
    }

    private fun updateGame(dt: Float) {
        // Decrement physical screen shake
        if (_shakeAmplitude.value > 0f) {
            _shakeAmplitude.value = (_shakeAmplitude.value - dt * 35f).coerceAtLeast(0f)
        }

        // Ticks for SlowMo powerup mode
        if (slowMoTimerMs > 0L) {
            slowMoTimerMs = (slowMoTimerMs - (dt * 1000f).toLong()).coerceAtLeast(0L)
            _isSlowMoActive.value = slowMoTimerMs > 0L
        } else {
            _isSlowMoActive.value = false
        }

        // Update active sliding block
        _slidingBlock.value?.let { block ->
            val actualSpeed = if (_isSlowMoActive.value) block.speed * 0.35f else block.speed
            block.centerX += block.dir * actualSpeed * dt

            // Bounce on boundaries
            val halfW = block.width / 2
            if (block.dir == 1 && block.centerX + halfW >= 1080f) {
                block.centerX = 1080f - halfW
                block.dir = -1
            } else if (block.dir == -1 && block.centerX - halfW <= 0f) {
                block.centerX = halfW
                block.dir = 1
            }
        }

        // Camera smoothly interpolates vertical offset standardly
        val targetScrollIdx = (stackedBlocks.size - 4).coerceAtLeast(0)
        val targetScrollY = targetScrollIdx * 85f
        _cameraScrollY.value += (targetScrollY - _cameraScrollY.value) * dt * 5.0f

        // Update chopped off slice physics
        for (i in sliceParticles.indices.reversed()) {
            val p = if (i < sliceParticles.size) sliceParticles[i] else continue
            p.x += p.vx * dt * 60f
            p.y += p.vy * dt * 60f
            p.vy += 22f * dt // gravity simulations
            p.rotation += p.rotationSpeed * dt * 90f
            p.alpha -= dt * 1.2f

            if (p.alpha <= 0f || p.y > 2400f) {
                sliceParticles.removeAt(i)
            }
        }

        // Update sparks/bursts
        for (i in sparkleParticles.indices.reversed()) {
            val p = if (i < sparkleParticles.size) sparkleParticles[i] else continue
            p.x += p.vx * dt * 60f
            p.y += p.vy * dt * 60f
            p.vy += p.gravity
            p.alpha -= dt * 1.5f

            if (p.alpha <= 0f) {
                sparkleParticles.removeAt(i)
            }
        }

        // Update floating combat texts
        for (i in floatingGameTexts.indices.reversed()) {
            val t = if (i < floatingGameTexts.size) floatingGameTexts[i] else continue
            t.y -= dt * 100f
            t.alpha -= dt * 1.0f
            if (t.alpha <= 0f) {
                floatingGameTexts.removeAt(i)
            }
        }

        // Update weather ambient particles
        val maxAmbient = 35
        if (ambientParticles.size < maxAmbient && Math.random() < 0.1) {
            spawnAmbientWeather(1)
        }
        for (i in ambientParticles.indices.reversed()) {
            val p = if (i < ambientParticles.size) ambientParticles[i] else continue
            p.y += p.vy * dt * 60f
            // Recycle or wrap particles on dropping out of the vertical coordinate frame
            if (p.y > 2200f) {
                p.y = -50f
                p.x = Math.random().toFloat() * 1080f
            }
        }
    }

    private fun spawnAmbientWeather(count: Int) {
        val theme = _activeTheme.value
        val isCyber = theme.id == "cyber"
        val isIce = theme.id == "frost"
        val isLava = theme.id == "lava"
        
        val color = when {
            isCyber -> Color(0x3300FFFF)
            isIce -> Color(0x44FFFFFF)
            isLava -> Color(0x55FF5722)
            else -> Color(0x22E5A93B) // warm sand
        }

        for (i in 0 until count) {
            ambientParticles.add(
                AmberParticle(
                    x = Math.random().toFloat() * 1080f,
                    y = Math.random().toFloat() * 2100f - 100f,
                    vy = (Math.random().toFloat() * 1.5f + 0.5f),
                    size = (Math.random().toFloat() * 6f + 2f),
                    color = color
                )
            )
        }
    }

    // Handles user trigger click on active layout
    fun tapToDrop() {
        val slider = _slidingBlock.value ?: return
        val currentStack = stackedBlocks.lastOrNull() ?: return

        val rightS = slider.centerX + slider.width / 2
        val leftS = slider.centerX - slider.width / 2

        val rightC = currentStack.centerX + currentStack.width / 2
        val leftC = currentStack.centerX - currentStack.width / 2

        // Check overall overlap
        val intersectLeft = maxOf(leftS, leftC)
        val intersectRight = minOf(rightS, rightC)
        val overlapWidth = intersectRight - intersectLeft

        if (overlapWidth <= 0f) {
            // COMPLETE MISS!
            AudioSynth.playSlice()
            triggerShake(18f)

            // Spawn whole slide block tumbling off
            createSliceTumble(
                x = slider.centerX,
                width = slider.width,
                color = getDynamicLayerColor(stackedBlocks.size)
            )

            // Deduct life
            _lives.value = (_lives.value - 1).coerceAtLeast(0)
            _combo.value = 0

            floatingGameTexts.add(
                FloatingGameText(
                    x = slider.centerX,
                    y = 1100f,
                    text = "MISS!",
                    color = Color.Red,
                    size = 28f
                )
            )

            if (_lives.value <= 0) {
                checkGameOver()
            } else {
                // Spawn next block above currentStack size
                spawnSlidingBlock(width = currentStack.width, index = stackedBlocks.size)
            }
            return
        }

        // Active overlap. Evaluate "Perfect" timing tolerances (clamped to center align)
        val tolerance = 15f // pixels virtual
        val diff = Math.abs(slider.centerX - currentStack.centerX)

        val finalWidth: Float
        val finalCenterX: Float
        val isPerfectClick = diff < tolerance

        val layerIndex = stackedBlocks.size
        val blockColor = getDynamicLayerColor(layerIndex)

        if (isPerfectClick) {
            // PERFECT SNAP!
            finalWidth = currentStack.width
            finalCenterX = currentStack.centerX

            _combo.value += 1
            AudioSynth.playPerfect(_combo.value)

            val pts = 10 * _combo.value
            _score.value += pts

            // Grow slightly wide on high perfect streak to reward skilled players
            val wideBonus = if (_combo.value >= 3) {
                // Slowly restore width of chopped block up to solid limits
                val originalW = 650f
                val addedWidth = minOf(20f, originalW - finalWidth)
                addedWidth
            } else 0f

            val adjustedWidth = minOf(650f, finalWidth + wideBonus)

            stackedBlocks.add(
                StackedBlock(
                    width = adjustedWidth,
                    centerX = finalCenterX,
                    color = blockColor,
                    layerIndex = layerIndex,
                    isPerfect = true
                )
            )

            // Burst sparkles
            createPerfectSparkles(finalCenterX, adjustedWidth, blockColor)

            // Add text alert
            floatingGameTexts.add(
                FloatingGameText(
                    x = finalCenterX,
                    y = 900f,
                    text = if (_combo.value >= 2) "PERFECT! x${_combo.value}" else "PERFECT!",
                    color = _activeTheme.value.accentColor,
                    size = 24f
                )
            )

            // Trigger score popup
            floatingGameTexts.add(
                FloatingGameText(
                    x = finalCenterX,
                    y = 1000f,
                    text = if (wideBonus > 0f) "+$pts (WIDENED!)" else "+$pts score",
                    color = Color.White,
                    size = 18f
                )
            )

            _progressCurrent.value = (_progressCurrent.value + 1).coerceAtMost(_progressGoal.value)
            checkLevelProgress()

            if (_currentScreen.value is Screen.ActivePlay) {
                spawnSlidingBlock(width = adjustedWidth, index = stackedBlocks.size)
            }
        } else {
            // PARTIAL LANDING (CHOP!)
            finalWidth = overlapWidth
            finalCenterX = (intersectLeft + intersectRight) / 2

            _combo.value = 0
            AudioSynth.playSlice()
            triggerShake(6f)

            // Spawn the cut piece tumbling away with physical momentum
            if (leftS < leftC) {
                // Sliced off left side
                val cutW = leftC - leftS
                val cutX = leftS + cutW / 2
                createSlicePhysics(cutX, cutW, blockColor, -3f)
            } else if (rightS > rightC) {
                // Sliced off right side
                val cutW = rightS - rightC
                val cutX = rightC + cutW / 2
                createSlicePhysics(cutX, cutW, blockColor, 3f)
            }

            stackedBlocks.add(
                StackedBlock(
                    width = finalWidth,
                    centerX = finalCenterX,
                    color = blockColor,
                    layerIndex = layerIndex,
                    isPerfect = false
                )
            )

            val basePts = 5
            _score.value += basePts

            floatingGameTexts.add(
                FloatingGameText(
                    x = finalCenterX,
                    y = 1000f,
                    text = "+$basePts score",
                    color = Color.Gray,
                    size = 15f
                )
            )

            _progressCurrent.value = (_progressCurrent.value + 1).coerceAtMost(_progressGoal.value)
            checkLevelProgress()

            if (_currentScreen.value is Screen.ActivePlay) {
                if (finalWidth <= 10f) {
                    // Too narrow to stack, trigger automatic defeat
                    _lives.value = 0
                    checkGameOver()
                } else {
                    spawnSlidingBlock(width = finalWidth, index = stackedBlocks.size)
                }
            }
        }
    }

    // Powerup trigger helper
    fun triggerShieldPowerup() {
        if (_currentScreen.value !is Screen.ActivePlay) return
        viewModelScope.launch {
            val success = repository.usePowerUp("shield")
            if (success) {
                AudioSynth.playPowerUp()
                val currentWidth = _slidingBlock.value?.width ?: 200f
                // Max restore to 650f
                val targetW = minOf(650f, currentWidth + 250f)
                
                _slidingBlock.value?.width = targetW
                
                floatingGameTexts.add(
                    FloatingGameText(
                        x = 540f, y = 1100f, text = "PERFECT FIT: EXPANDED!", color = Color.Cyan, size = 22f
                    )
                )
                createPerfectSparkles(540f, targetW, Color.Cyan)
            }
        }
    }

    fun triggerSlowMoPowerup() {
        if (_currentScreen.value !is Screen.ActivePlay) return
        viewModelScope.launch {
            val success = repository.usePowerUp("slowMo")
            if (success) {
                AudioSynth.playPowerUp()
                slowMoTimerMs = 8000L // Slow motion for 8 seconds
                _isSlowMoActive.value = true
                floatingGameTexts.add(
                    FloatingGameText(
                        x = 540f, y = 1100f, text = "TIME WARP ACTIVE (SLOW!)", color = Color(0xFFD500F9), size = 22f
                    )
                )
            }
        }
    }

    fun triggerBombClearPowerup() {
        if (_currentScreen.value !is Screen.ActivePlay) return
        viewModelScope.launch {
            val success = repository.usePowerUp("bombClear")
            if (success) {
                AudioSynth.playPowerUp()
                
                // Repair: undo last bad slice chop. Remove top stacked layer, expand previous
                if (stackedBlocks.size > 1) {
                    val last = stackedBlocks.removeAt(stackedBlocks.size - 1)
                    val baseBlock = stackedBlocks.last()
                    // Restore active wide to previous block
                    spawnSlidingBlock(width = baseBlock.width, index = stackedBlocks.size)
                    
                    floatingGameTexts.add(
                        FloatingGameText(
                            x = 540f, y = 1100f, text = "STACK REPAIRED!", color = Color.Yellow, size = 22f
                        )
                    )
                    createPerfectSparkles(baseBlock.centerX, baseBlock.width, Color.Yellow)
                } else {
                    // Can't undo further, refund power up locally
                    val currentStats = repository.getStatsSync()
                    repository.saveStats(currentStats.copy(bombClearCount = currentStats.bombClearCount + 1))
                }
            }
        }
    }

    private fun checkLevelProgress() {
        if (_progressCurrent.value >= _progressGoal.value) {
            stopGame()
            AudioSynth.playWin()

            val remLives = _lives.value
            val starCount = if (remLives >= 3) 3 else if (remLives == 2) 2 else 1

            viewModelScope.launch {
                repository.updateHighScoreAndStars(
                    score = _score.value,
                    starsEarned = starCount,
                    currentLevelCompleted = activeLevel
                )
                _currentScreen.value = Screen.LevelComplete(
                    level = activeLevel,
                    score = _score.value,
                    stars = starCount
                )
            }
        }
    }

    private fun checkGameOver() {
        stopGame()
        AudioSynth.playGameOver()

        viewModelScope.launch {
            val current = repository.getStatsSync()
            repository.saveStats(current.copy(highScore = maxOf(current.highScore, _score.value)))
            _currentScreen.value = Screen.GameOver(level = activeLevel, score = _score.value)
        }
    }

    private fun triggerShake(amplitude: Float) {
        _shakeAmplitude.value = (_shakeAmplitude.value + amplitude).coerceAtMost(30f)
    }

    private fun getDynamicLayerColor(layerIdx: Int): Color {
        val theme = _activeTheme.value
        // Interpolate coloring vertically based on layers to give stacked blocks a stunning look
        val ratio = (layerIdx % 12) / 12f
        return lerpColor(theme.primaryColor, theme.secondaryColor, ratio)
    }

    private fun lerpColor(c1: Color, c2: Color, bias: Float): Color {
        val r = c1.red + (c2.red - c1.red) * bias
        val g = c1.green + (c2.green - c1.green) * bias
        val b = c1.blue + (c2.blue - c1.blue) * bias
        return Color(r, g, b, 1.0f)
    }

    private fun createSlicePhysics(centerX: Float, width: Float, color: Color, initialVx: Float) {
        sliceParticles.add(
            SliceParticle(
                x = centerX,
                y = 1600f - stackedBlocks.size * 85f, // approximate top height
                vx = initialVx + (Math.random().toFloat() - 0.5f) * 2f,
                vy = -4f - Math.random().toFloat() * 4f,
                width = width,
                height = 35f,
                color = color
            )
        )
    }

    private fun createSliceTumble(x: Float, width: Float, color: Color) {
        sliceParticles.add(
            SliceParticle(
                x = x,
                y = 1600f - stackedBlocks.size * 85f,
                vx = (Math.random().toFloat() - 0.5f) * 5f,
                vy = -2f,
                width = width,
                height = 35f,
                color = color
            )
        )
    }

    private fun createPerfectSparkles(centerX: Float, width: Float, color: Color) {
        val halfW = width / 2
        // Spawn sparks from left and right edges of snap overlap
        val leftEdge = centerX - halfW
        val rightEdge = centerX + halfW

        val count = 12
        for (i in 0 until count) {
            // Left sparks
            sparkleParticles.add(
                SparkleParticle(
                    x = leftEdge,
                    y = 1600f - stackedBlocks.size * 85f,
                    vx = -3f - Math.random().toFloat() * 6f,
                    vy = -2f - Math.random().toFloat() * 7f,
                    size = (6f + Math.random().toFloat() * 8f),
                    color = color
                )
            )
            // Right sparks
            sparkleParticles.add(
                SparkleParticle(
                    x = rightEdge,
                    y = 1600f - stackedBlocks.size * 85f,
                    vx = 3f + Math.random().toFloat() * 6f,
                    vy = -2f - Math.random().toFloat() * 7f,
                    size = (6f + Math.random().toFloat() * 8f),
                    color = color
                )
            )
        }
    }
}
