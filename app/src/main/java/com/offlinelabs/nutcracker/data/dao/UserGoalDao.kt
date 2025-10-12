package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.offlinelabs.nutcracker.data.model.UserGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserGoal(userGoal: UserGoal)

    // Using Flow to observe changes to the goal
    @Query("SELECT * FROM user_goals WHERE id = 1")
    fun getUserGoal(): Flow<UserGoal?> // Nullable if no goal is set yet
}

