package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.offlinelabs.nutcracker.data.model.PillCheckIn
import kotlinx.coroutines.flow.Flow

@Dao
interface PillCheckInDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillCheckIn(pillCheckIn: PillCheckIn): Long

    @Update
    suspend fun updatePillCheckIn(pillCheckIn: PillCheckIn)

    @Delete
    suspend fun deletePillCheckIn(pillCheckIn: PillCheckIn)

    @Query("SELECT * FROM pill_check_ins ORDER BY timestamp DESC")
    fun getAllPillCheckIns(): Flow<List<PillCheckIn>>

    @Query("SELECT * FROM pill_check_ins WHERE pillId = :pillId ORDER BY timestamp DESC")
    fun getPillCheckInsByPillId(pillId: Long): Flow<List<PillCheckIn>>

    @Query("SELECT * FROM pill_check_ins WHERE DATE(timestamp) = :date ORDER BY timestamp DESC")
    fun getPillCheckInsByDate(date: String): Flow<List<PillCheckIn>>

    @Query("SELECT * FROM pill_check_ins WHERE pillId = :pillId AND DATE(timestamp) = :date")
    fun getPillCheckInByPillIdAndDate(pillId: Long, date: String): Flow<PillCheckIn?>

    @Query("DELETE FROM pill_check_ins WHERE pillId = :pillId AND DATE(timestamp) = :date")
    suspend fun deletePillCheckInByPillIdAndDate(pillId: Long, date: String)

    @Query("DELETE FROM pill_check_ins")
    suspend fun deleteAllPillCheckIns()
}
