package dev.sadakat.thinkfast.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.sadakat.thinkfast.data.local.database.entities.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: String): GoalEntity?

    @Query("SELECT * FROM goals LIMIT 1")
    suspend fun getCurrentGoal(): GoalEntity?

    @Query("SELECT * FROM goals LIMIT 1")
    fun observeCurrentGoal(): Flow<GoalEntity?>

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: String)
}
