package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.offlinelabs.nutcracker.data.model.Pill
import kotlinx.coroutines.flow.Flow

@Dao
interface PillDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPill(pill: Pill): Long

    @Update
    suspend fun updatePill(pill: Pill)

    @Delete
    suspend fun deletePill(pill: Pill)

    @Query("SELECT * FROM pills ORDER BY name ASC")
    fun getAllPills(): Flow<List<Pill>>

    @Query("SELECT * FROM pills WHERE id = :pillId")
    fun getPillById(pillId: Long): Flow<Pill?>

    @Query("DELETE FROM pills")
    suspend fun deleteAllPills()
}
