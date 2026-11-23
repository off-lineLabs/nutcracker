package com.offlinelabs.nutcracker.ui.components.gym

import com.offlinelabs.nutcracker.data.GymSelection
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

/** The gym whose name the dropdown header shows, or null for "All gyms". */
fun selectedGymOrNull(selection: GymSelection, gyms: List<Gym>): Gym? =
    (selection as? GymSelection.Specific)?.let { s -> gyms.firstOrNull { it.id == s.gymId } }
