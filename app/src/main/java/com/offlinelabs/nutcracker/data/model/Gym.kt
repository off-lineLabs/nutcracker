package com.offlinelabs.nutcracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A gym (training location). Exercises can belong to one gym ([Exercise.gymId]) or be shared (null).
 * Gyms are soft-deleted ([isVisible] = false) so history can still show their name.
 */
@Entity(tableName = "gyms")
data class Gym(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val unit: String? = null, // Optional location label, e.g. "Universitat"
    val mapsPlaceId: String? = null, // Reserved for a future maps integration
    val mapsProvider: String? = null, // Reserved for a future maps integration
    val isVisible: Boolean = true
)
