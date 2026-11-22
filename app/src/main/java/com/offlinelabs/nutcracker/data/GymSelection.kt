package com.offlinelabs.nutcracker.data

import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.Gym

/** What the gym dropdown of the Exercise tab has selected. */
sealed interface GymSelection {
    data object AllGyms : GymSelection
    data class Specific(val gymId: Long) : GymSelection
}

/** Resolves the persisted gym id; a null or stale (missing or hidden gym) id means [GymSelection.AllGyms]. */
fun resolveGymSelection(savedGymId: Long?, visibleGyms: List<Gym>): GymSelection =
    if (savedGymId != null && visibleGyms.any { it.id == savedGymId }) {
        GymSelection.Specific(savedGymId)
    } else {
        GymSelection.AllGyms
    }

data class PartitionedExercises(
    val gymExercises: List<Exercise>,
    val shared: List<Exercise>
)

/**
 * Splits visible exercises into those of the selected gym and the shared ones (gymId null).
 * [GymSelection.AllGyms] yields only shared exercises. Input order is kept.
 */
fun partitionExercisesForSelection(exercises: List<Exercise>, selection: GymSelection): PartitionedExercises {
    val visible = exercises.filter { it.isVisible }
    val shared = visible.filter { it.gymId == null }
    val gymExercises = when (selection) {
        GymSelection.AllGyms -> emptyList()
        is GymSelection.Specific -> visible.filter { it.gymId == selection.gymId }
    }
    return PartitionedExercises(gymExercises = gymExercises, shared = shared)
}

/** "name · unit", or just the name when the unit is null or blank. */
fun gymLabel(name: String, unit: String?): String {
    val trimmedName = name.trim()
    val trimmedUnit = unit?.trim().orEmpty()
    return if (trimmedUnit.isEmpty()) trimmedName else "$trimmedName · $trimmedUnit"
}
