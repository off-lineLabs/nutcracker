package com.offlinelabs.nutcracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A workout routine (e.g. "Push day"). Exercises join routines through [RoutineExercise], so one
 * exercise can be part of several routines. There is no UI to manage routines yet (planned); the
 * table exists so exercise lists can already be grouped by routine.
 */
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Position among routines (ascending). */
    val sortOrder: Int = 0,
    /** Soft delete: false hides the routine. */
    val isVisible: Boolean = true
)

/** Membership of an exercise in a routine, with its [position] inside that routine. */
@Entity(
    tableName = "routine_exercises",
    primaryKeys = ["routineId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class RoutineExercise(
    val routineId: Long,
    val exerciseId: Long,
    val position: Int = 0
)
