package dev.sadakat.thinkfast.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import dev.sadakat.thinkfast.data.local.database.entities.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Upsert
    suspend fun upsertGoal(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE targetApp = :targetApp")
    suspend fun getGoalByApp(targetApp: String): GoalEntity?

    @Query("SELECT * FROM goals WHERE targetApp = :targetApp")
    fun observeGoalByApp(targetApp: String): Flow<GoalEntity?>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoals(): List<GoalEntity>

    @Query("SELECT * FROM goals")
    fun observeAllGoals(): Flow<List<GoalEntity>>

    @Query("UPDATE goals SET currentStreak = :currentStreak WHERE targetApp = :targetApp")
    suspend fun updateCurrentStreak(targetApp: String, currentStreak: Int)

    @Query("UPDATE goals SET longestStreak = :bestStreak WHERE targetApp = :targetApp")
    suspend fun updateBestStreak(targetApp: String, bestStreak: Int)

    @Query("DELETE FROM goals WHERE targetApp = :targetApp")
    suspend fun deleteGoal(targetApp: String)
}
