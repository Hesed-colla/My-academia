package com.example.data

import android.content.Context

object GameDataModule {
    private var database: GameDatabase? = null
    private var repository: GameRepository? = null

    fun getRepository(context: Context): GameRepository {
        return repository ?: synchronized(this) {
            val db = database ?: GameDatabase.getInstance(context).also { database = it }
            val repo = GameRepository(db.gameStatsDao)
            repository = repo
            repo
        }
    }
}
