package com.example.template.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pills")
data class Pill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)
