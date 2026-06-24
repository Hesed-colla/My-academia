package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val dao: GameStatsDao) {

    val stats: Flow<GameStats> = dao.getStats().map { it ?: GameStats() }

    suspend fun getStatsSync(): GameStats {
        return dao.getStatsSync() ?: GameStats()
    }

    suspend fun saveStats(newStats: GameStats) {
        dao.saveStats(newStats)
    }

    suspend fun updateHighScoreAndStars(score: Int, starsEarned: Int, currentLevelCompleted: Int) {
        val current = getStatsSync()
        val nextLevelToUnlock = if (currentLevelCompleted >= current.unlockedLevel) {
            (currentLevelCompleted + 1).coerceAtMost(10)
        } else {
            current.unlockedLevel
        }
        val nextStats = current.copy(
            highScore = maxOf(current.highScore, score),
            totalStars = current.totalStars + starsEarned,
            unlockedLevel = nextLevelToUnlock
        )
        dao.saveStats(nextStats)
    }

    suspend fun usePowerUp(type: String): Boolean {
        val current = getStatsSync()
        val updated = when (type) {
            "shield" -> if (current.shieldCount > 0) current.copy(shieldCount = current.shieldCount - 1) else null
            "slowMo" -> if (current.slowMoCount > 0) current.copy(slowMoCount = current.slowMoCount - 1) else null
            "bombClear" -> if (current.bombClearCount > 0) current.copy(bombClearCount = current.bombClearCount - 1) else null
            else -> null
        }
        return if (updated != null) {
            dao.saveStats(updated)
            true
        } else {
            false
        }
    }

    suspend fun purchasePowerUp(type: String, cost: Int): Boolean {
        val current = getStatsSync()
        if (current.totalStars < cost) return false
        val updated = when (type) {
            "shield" -> current.copy(totalStars = current.totalStars - cost, shieldCount = current.shieldCount + 1)
            "slowMo" -> current.copy(totalStars = current.totalStars - cost, slowMoCount = current.slowMoCount + 1)
            "bombClear" -> current.copy(totalStars = current.totalStars - cost, bombClearCount = current.bombClearCount + 1)
            else -> null
        }
        return if (updated != null) {
            dao.saveStats(updated)
            true
        } else {
            false
        }
    }

    suspend fun toggleSound(): Boolean {
        val current = getStatsSync()
        val newState = !current.soundEnabled
        dao.saveStats(current.copy(soundEnabled = newState))
        return newState
    }

    suspend fun resetAll() {
        dao.saveStats(GameStats())
    }
}
