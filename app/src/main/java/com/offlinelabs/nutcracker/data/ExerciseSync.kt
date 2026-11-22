package com.offlinelabs.nutcracker.data

import com.offlinelabs.nutcracker.data.model.Exercise

/**
 * [target] (a synced copy at another gym) with every setting of [source]. What stays per copy:
 * its id, gym, images (each copy owns its files), visibility, list position and sync group.
 */
fun applySyncedFields(target: Exercise, source: Exercise): Exercise = target.copy(
    name = source.name,
    kcalBurnedPerUnit = source.kcalBurnedPerUnit,
    defaultWeight = source.defaultWeight,
    defaultReps = source.defaultReps,
    defaultSets = source.defaultSets,
    category = source.category,
    equipment = source.equipment,
    primaryMuscles = source.primaryMuscles,
    secondaryMuscles = source.secondaryMuscles,
    force = source.force,
    level = source.level,
    mechanic = source.mechanic,
    instructions = source.instructions,
    notes = source.notes
)

/** Visible exercises synced with [exercise] (same sync group, other rows). */
fun syncPartners(exercise: Exercise, all: List<Exercise>): List<Exercise> =
    exercise.syncGroupId?.let { group -> all.filter { it.syncGroupId == group && it.id != exercise.id && it.isVisible } }
        .orEmpty()
