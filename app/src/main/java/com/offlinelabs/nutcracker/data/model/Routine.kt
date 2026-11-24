package com.offlinelabs.nutcracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A workout routine (e.g. "Leg day") at one gym. Exercises join routines through [RoutineExercise]
 * in a user-defined order; only exercises of that gym or shared ones belong in it, and one exercise
 * can be part of several routines.
 */
@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = Gym::class,
            parentColumns = ["id"],
            childColumns = ["gymId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gymId")]
)
data class Routine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Gym the routine is done at (required). */
    val gymId: Long,
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
