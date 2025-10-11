package com.offlinelabs.nutcracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "pill_check_ins")
data class PillCheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pillId: Long,
    val timestamp: LocalDateTime
)
