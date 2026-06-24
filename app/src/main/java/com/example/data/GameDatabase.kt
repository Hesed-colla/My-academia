package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_stats")
data class GameStats(
    @PrimaryKey val id: Int = 1,
    val unlockedLevel: Int = 1,
    val totalStars: Int = 0,
    val highScore: Int = 0,
    val shieldCount: Int = 3,
    val slowMoCount: Int = 3,
    val bombClearCount: Int = 3,
    val soundEnabled: Boolean = true
)

@Dao
interface GameStatsDao {
    @Query("SELECT * FROM game_stats WHERE id = 1")
    fun getStats(): Flow<GameStats?>

    @Query("SELECT * FROM game_stats WHERE id = 1")
    suspend fun getStatsSync(): GameStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStats(stats: GameStats)
}

@Database(entities = [GameStats::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract val gameStatsDao: GameStatsDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getInstance(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "neon_pop_quest.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
