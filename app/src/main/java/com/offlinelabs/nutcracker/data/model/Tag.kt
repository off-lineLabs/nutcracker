package com.offlinelabs.nutcracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String, // Hex color code (e.g., "#FF5722")
    val type: TagType // Either MEAL or EXERCISE
)

enum class TagType {
    MEAL,
    EXERCISE
}
