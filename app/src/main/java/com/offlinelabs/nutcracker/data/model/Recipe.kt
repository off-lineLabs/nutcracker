package com.offlinelabs.nutcracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "recipes")
@TypeConverters(ServingSizeUnitConverter::class)
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val isVisible: Boolean = true, // Soft delete field
    val createdDate: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
)
