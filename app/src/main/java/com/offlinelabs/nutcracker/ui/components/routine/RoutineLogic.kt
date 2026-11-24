package com.offlinelabs.nutcracker.ui.components.routine

import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.Routine

/** Exercises a routine at [gymId] can contain: that gym's own and the shared ones. */
fun exercisesAvailableAtGym(exercises: List<Exercise>, gymId: Long): List<Exercise> =
    exercises.filter { it.gymId == null || it.gymId == gymId }

/** Routines of [gymId] in their order, or every routine when [gymId] is null (All gyms). */
fun routinesForGym(routines: List<Routine>, gymId: Long?): List<Routine> =
    routines.filter { gymId == null || it.gymId == gymId }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
