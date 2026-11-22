package com.offlinelabs.nutcracker.data

import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseCategoryMapper
import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.data.model.RoutineExercise

/*
 * Pure ordering and grouping of the Food and Exercise tab lists (no Compose, unit tested).
 */

/** How a list is ordered. Stored by [name] in settings. */
enum class ListSort { CUSTOM, ALPHABETICAL, ADDED_DATE, LAST_CHECKED_IN }

/** Food tab grouping. Stored by [name] in settings. */
enum class MealGrouping { NONE, NUTRI_SCORE }

/** Exercise tab grouping. Stored by [name] in settings. */
enum class ExerciseGrouping { NONE, ROUTINE, MUSCLE, TYPE }

/** Title of a group; the UI turns it into localized text. */
sealed interface GroupTitle {
    data class NutriScore(val grade: String) : GroupTitle
    data object NoNutriScore : GroupTitle
    data class RoutineName(val name: String) : GroupTitle
    data object NoRoutine : GroupTitle
    data class Muscle(val muscle: String) : GroupTitle
    data object NoMuscle : GroupTitle
    data class Type(val type: ExerciseType) : GroupTitle
}

/** One group of a list. [key] is stable and unique within the list (used for item keys). */
data class ListGroup<T>(val key: String, val title: GroupTitle?, val items: List<T>)

/** What sorting needs to know about an item. */
private class SortFields(val id: Long, val name: String, val sortOrder: Int, val lastCheckIn: String?)

private fun <T> comparatorFor(
    sort: ListSort,
    nameComparator: Comparator<String>,
    fields: (T) -> SortFields
): Comparator<T> {
    val byName = Comparator<T> { a, b -> nameComparator.compare(fields(a).name, fields(b).name) }
    val byId = compareBy<T> { fields(it).id }
    return when (sort) {
        ListSort.CUSTOM -> compareBy<T> { fields(it).sortOrder }.then(byName).then(byId)
        ListSort.ALPHABETICAL -> byName.then(byId)
        // Ids are assigned in insertion order, so the newest item has the highest id
        ListSort.ADDED_DATE -> compareByDescending<T> { fields(it).id }
        // Never checked in goes last; the stored "yyyy-MM-dd HH:mm:ss" text sorts chronologically
        ListSort.LAST_CHECKED_IN -> compareBy<T> { fields(it).lastCheckIn == null }
            .thenByDescending { fields(it).lastCheckIn }
            .then(byName).then(byId)
    }
}

fun sortMeals(
    meals: List<Meal>,
    sort: ListSort,
    lastCheckIns: Map<Long, String>,
    nameComparator: Comparator<String> = String.CASE_INSENSITIVE_ORDER
): List<Meal> = meals.sortedWith(
    comparatorFor(sort, nameComparator) { SortFields(it.id, it.name, it.sortOrder, lastCheckIns[it.id]) }
)

fun sortExercises(
    exercises: List<Exercise>,
    sort: ListSort,
    lastCheckIns: Map<Long, String>,
    nameComparator: Comparator<String> = String.CASE_INSENSITIVE_ORDER
): List<Exercise> = exercises.sortedWith(
    comparatorFor(sort, nameComparator) { SortFields(it.id, it.name, it.sortOrder, lastCheckIns[it.id]) }
)

private val NUTRI_GRADES = listOf("A", "B", "C", "D", "E")

/** Groups already-sorted [meals]; group order is A–E then "no score". Empty groups are dropped. */
fun groupMeals(meals: List<Meal>, grouping: MealGrouping): List<ListGroup<Meal>> = when (grouping) {
    MealGrouping.NONE -> listOf(ListGroup("all", null, meals))
    MealGrouping.NUTRI_SCORE -> {
        val byGrade = meals.groupBy { m -> m.nutriscore?.grade?.trim()?.uppercase()?.takeIf { it in NUTRI_GRADES } }
        NUTRI_GRADES.mapNotNull { g -> byGrade[g]?.let { ListGroup("nutri_$g", GroupTitle.NutriScore(g), it) } } +
            listOfNotNull(byGrade[null]?.let { ListGroup("nutri_none", GroupTitle.NoNutriScore, it) })
    }
}

/**
 * Groups already-sorted [exercises]. An exercise appears in every routine it belongs to and under
 * every primary muscle. Routines follow their own order, muscles are alphabetical, types follow
 * [ExerciseType]; the "none" group is last. Empty groups are dropped.
 */
fun groupExercises(
    exercises: List<Exercise>,
    grouping: ExerciseGrouping,
    routines: List<Routine> = emptyList(),
    memberships: List<RoutineExercise> = emptyList()
): List<ListGroup<Exercise>> = when (grouping) {
    ExerciseGrouping.NONE -> listOf(ListGroup("all", null, exercises))
    ExerciseGrouping.TYPE -> {
        val byType = exercises.groupBy { ExerciseCategoryMapper.getExerciseType(it.category) }
        ExerciseType.entries.mapNotNull { t ->
            byType[t]?.let { ListGroup("type_${t.name}", GroupTitle.Type(t), it) }
        }
    }
    ExerciseGrouping.MUSCLE -> {
        fun muscles(e: Exercise) = e.primaryMuscles.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
        val all = exercises.flatMap(::muscles).distinct().sorted()
        all.map { m -> ListGroup("muscle_$m", GroupTitle.Muscle(m), exercises.filter { m in muscles(it) }) } +
            listOfNotNull(
                exercises.filter { muscles(it).isEmpty() }.takeIf { it.isNotEmpty() }
                    ?.let { ListGroup("muscle_none", GroupTitle.NoMuscle, it) }
            )
    }
    ExerciseGrouping.ROUTINE -> {
        val routineIdsByExercise = memberships.groupBy({ it.exerciseId }, { it.routineId })
        routines.mapNotNull { r ->
            exercises.filter { r.id in routineIdsByExercise[it.id].orEmpty() }
                .takeIf { it.isNotEmpty() }
                ?.let { ListGroup("routine_${r.id}", GroupTitle.RoutineName(r.name), it) }
        } + listOfNotNull(
            exercises.filter { e -> routineIdsByExercise[e.id].orEmpty().none { id -> routines.any { it.id == id } } }
                .takeIf { it.isNotEmpty() }
                ?.let { ListGroup("routine_none", GroupTitle.NoRoutine, it) }
        )
    }
}

/**
 * New custom order after the user reordered one group. [customOrder] is the current custom order of
 * the whole list; [groupNewOrder] is the reordered group (a subset of it). The group's items swap
 * into the slots the group already occupied, so items outside the group keep their positions.
 */
fun <K> reorderWithinGroup(customOrder: List<K>, groupNewOrder: List<K>): List<K> {
    val inGroup = groupNewOrder.toSet()
    val next = groupNewOrder.iterator()
    return customOrder.map { if (it in inGroup && next.hasNext()) next.next() else it }
}
