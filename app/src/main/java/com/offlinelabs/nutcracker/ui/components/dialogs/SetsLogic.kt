package com.offlinelabs.nutcracker.ui.components.dialogs

/*
 * Pure logic behind a strength row's set list on the Exercise tab, kept free of Compose so it can
 * be unit tested.
 */

/** A set about to be ticked: its position and values. */
data class PlannedSet(val index: Int, val weight: Double, val reps: Int)

/** One of the day's check-ins, as far as the set list needs it. */
data class DayLog(val logId: Long, val exerciseId: Long, val routineId: Long?, val logDateTime: String)

/**
 * The check-in that a row's sets go into: the exercise's latest of the day. In a routine group
 * ([routineId] set) only one made under that routine or without a routine counts.
 */
fun sessionLog(dayLogs: List<DayLog>, exerciseId: Long, routineId: Long?): DayLog? =
    dayLogs.filter { it.exerciseId == exerciseId && (routineId == null || it.routineId == null || it.routineId == routineId) }
        .maxWithOrNull(compareBy({ it.logDateTime }, { it.logId }))

/**
 * How many set rows to show: the exercise's default sets (at least one), or more when sets past
 * it are already done, plus the ones added with "+ set".
 */
fun plannedSetCount(defaultSets: Int, doneIndices: Collection<Int>, added: Int): Int =
    maxOf(defaultSets.coerceAtLeast(1), (doneIndices.maxOrNull() ?: -1) + 1) + added

/**
 * A set's value before it's done: the latest change at or before [index] carries over to the
 * following sets, so changing set 2 also changes 3, 4… until a set changed on its own; without
 * any, the row's own wheel ([rowValue]).
 */
fun <T> carriedSetValue(index: Int, changes: Map<Int, T>, rowValue: T): T =
    changes.filterKeys { it <= index }.maxByOrNull { it.key }?.value ?: rowValue

/** The row's check-in box: nothing done, some sets done, or all of them. */
enum class SetsTick { NONE, PARTIAL, ALL }

fun setsTick(doneCount: Int, planned: Int): SetsTick = when {
    doneCount <= 0 -> SetsTick.NONE
    doneCount < planned -> SetsTick.PARTIAL
    else -> SetsTick.ALL
}
