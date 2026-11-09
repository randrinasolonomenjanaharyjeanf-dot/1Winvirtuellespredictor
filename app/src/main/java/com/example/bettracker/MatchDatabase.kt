
package com.example.bettracker

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val homeTeam: String,
    val awayTeam: String,
    val homeGoals: Int,
    val awayGoals: Int,
    val matchDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "predictions")
data class PredictionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val homeTeam: String,
    val awayTeam: String,
    val homeProbability: Double,
    val drawProbability: Double,
    val awayProbability: Double,
    val actualResult: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface MatchDao {

    @Insert
    suspend fun insertMatch(match: MatchEntity)

    @Insert
    suspend fun insertPrediction(prediction: PredictionEntity)

    @Query("SELECT * FROM matches ORDER BY matchDate DESC")
    suspend fun getAllMatches(): List<MatchEntity>

    @Query("SELECT * FROM predictions ORDER BY createdAt DESC")
    suspend fun getAllPredictions(): List<PredictionEntity>

    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()
}

@Database(
    entities = [
        MatchEntity::class,
        PredictionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MatchDatabase : RoomDatabase() {

    abstract fun matchDao(): MatchDao
}
