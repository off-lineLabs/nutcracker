package com.offlinelabs.nutcracker.ui.components.gym

import com.offlinelabs.nutcracker.data.GymSelection
import com.offlinelabs.nutcracker.data.PartitionedExercises
import com.offlinelabs.nutcracker.data.model.Gym

/** A gym needs a non-blank name; the unit is optional. */
fun isGymNameValid(name: String): Boolean = name.isNotBlank()

/**
 * Builds the gym to save from the form. Name and unit are trimmed and a blank unit becomes null.
 * When [existing] is given (edit), its id, maps fields and visibility are preserved.
 */
fun buildGymFromForm(existing: Gym?, name: String, unit: String): Gym {
    val base = existing ?: Gym(name = "")
    return base.copy(name = name.trim(), unit = unit.trim().ifEmpty { null })
}

/** What to tell the user when the Exercise tab list has nothing to show for the current selection. */
enum class ExerciseListHint {
    /** Nothing special to show. */
    NONE,

    /** The whole exercise library is empty (the existing "no exercises yet" text). */
    LIBRARY_EMPTY,

    /** A gym is selected but has no exercises of its own. */
    GYM_EMPTY
}

/**
 * Decides the empty-state hint of the Exercise tab. "All gyms" lists every exercise, so it only
 * needs a hint when the library is empty.
 * @param totalVisibleExercises all visible exercises, regardless of gym.
 */
fun exerciseListHint(
    selection: GymSelection,
    partitioned: PartitionedExercises,
    totalVisibleExercises: Int
): ExerciseListHint = when (selection) {
    is GymSelection.Specific ->
        if (partitioned.gymExercises.isEmpty()) ExerciseListHint.GYM_EMPTY else ExerciseListHint.NONE
    GymSelection.AllGyms ->
        if (totalVisibleExercises == 0) ExerciseListHint.LIBRARY_EMPTY else ExerciseListHint.NONE
}

/** The gym whose name the dropdown header shows, or null for "All gyms". */
fun selectedGymOrNull(selection: GymSelection, gyms: List<Gym>): Gym? =
    (selection as? GymSelection.Specific)?.let { s -> gyms.firstOrNull { it.id == s.gymId } }
