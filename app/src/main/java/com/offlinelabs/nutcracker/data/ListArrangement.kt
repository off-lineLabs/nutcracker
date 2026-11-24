package com.offlinelabs.nutcracker.data

import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseCategoryMapper
import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCategory
import com.offlinelabs.nutcracker.data.model.MealCategoryMeal
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.data.model.RoutineExercise

/*
 * Pure ordering and grouping of the Food and Exercise tab lists (no Compose, unit tested).
 */

/** How a list is ordered. Stored by [name] in settings. */
enum class ListSort(
    /** Direction picked when the user switches to this sort (newest/most recent first for dates). */
    val defaultDescending: Boolean,
    /** Whether the up/down direction applies (custom order is the user's own order). */
    val hasDirection: Boolean = true
) {
    CUSTOM(defaultDescending = false, hasDirection = false),
    ALPHABETICAL(defaultDescending = false),
    ADDED_DATE(defaultDescending = true),
    LAST_CHECKED_IN(defaultDescending = true)
}

/** Food tab grouping. Stored by [name] in settings. */
enum class MealGrouping { NONE, MEAL_TYPE, NOVA }

/** Exercise tab grouping. Stored by [name] in settings. */
enum class ExerciseGrouping { NONE, GYM, ROUTINE, MUSCLE, TYPE }

/** Title of a group; the UI turns it into localized text. */
sealed interface GroupTitle {
    /** A meal category; [defaultKey] set for the built-in, translated ones. */
    data class MealType(val name: String, val defaultKey: String?) : GroupTitle
    data object NoMealType : GroupTitle
    data class Nova(val group: Int) : GroupTitle
    data object NoNova : GroupTitle
    data class GymName(val name: String, val unit: String?) : GroupTitle
    data object Shared : GroupTitle
    /** [gym] is set when routines of several gyms are listed together (All gyms). */
    data class RoutineName(val name: String, val gym: GymName? = null) : GroupTitle
    data object NoRoutine : GroupTitle
    data class Muscle(val muscle: String) : GroupTitle
    data object NoMuscle : GroupTitle
    data class Type(val type: ExerciseType) : GroupTitle
}

/**
 * One group of a list. [key] is stable and unique within the list (used for item keys).
 * [routineId] is set for routine groups, whose order is the routine's own exercise order.
 * [mealCategoryId] is set for meal type groups. Check-ins made from a routine / meal type group
 * remember it; such a group shows its own check-ins (and those without one) as done.
 */
data class ListGroup<T>(
    val key: String,
    val title: GroupTitle?,
    val items: List<T>,
    val routineId: Long? = null,
    val mealCategoryId: Long? = null
)

/** What sorting needs to know about an item. */
private class SortFields(val id: Long, val name: String, val sortOrder: Int, val lastCheckIn: String?)

private fun <T> comparatorFor(
    sort: ListSort,
    descending: Boolean,
    nameComparator: Comparator<String>,
    fields: (T) -> SortFields
): Comparator<T> {
    val byName = Comparator<T> { a, b -> nameComparator.compare(fields(a).name, fields(b).name) }
    val byId = compareBy<T> { fields(it).id }
    fun Comparator<T>.directed() = if (descending) reversed() else this
    return when (sort) {
        ListSort.CUSTOM -> compareBy<T> { fields(it).sortOrder }.then(byName).then(byId)
        ListSort.ALPHABETICAL -> byName.then(byId).directed()
        // Ids are assigned in insertion order, so a higher id was added later
        ListSort.ADDED_DATE -> byId.directed()
        // Never checked in always goes last; the stored "yyyy-MM-dd HH:mm:ss" text sorts chronologically
        ListSort.LAST_CHECKED_IN -> compareBy<T> { fields(it).lastCheckIn == null }
            .then(compareBy<T> { fields(it).lastCheckIn }.directed())
            .then(byName).then(byId)
    }
}

fun sortMeals(
    meals: List<Meal>,
    sort: ListSort,
    lastCheckIns: Map<Long, String>,
    nameComparator: Comparator<String> = String.CASE_INSENSITIVE_ORDER,
    descending: Boolean = sort.defaultDescending
): List<Meal> = meals.sortedWith(
    comparatorFor(sort, descending, nameComparator) { SortFields(it.id, it.name, it.sortOrder, lastCheckIns[it.id]) }
)

fun sortExercises(
    exercises: List<Exercise>,
    sort: ListSort,
    lastCheckIns: Map<Long, String>,
    nameComparator: Comparator<String> = String.CASE_INSENSITIVE_ORDER,
    descending: Boolean = sort.defaultDescending
): List<Exercise> = exercises.sortedWith(
    comparatorFor(sort, descending, nameComparator) { SortFields(it.id, it.name, it.sortOrder, lastCheckIns[it.id]) }
)

private val NOVA_GROUPS = listOf(1, 2, 3, 4)

/**
 * Groups already-sorted [meals].
 * - MEAL_TYPE: one group per category in [categories] order (a food in several appears in each),
 *   then "no meal type".
 * - NOVA: 1–4 then "no NOVA".
 * Empty groups are dropped.
 */
fun groupMeals(
    meals: List<Meal>,
    grouping: MealGrouping,
    categories: List<MealCategory> = emptyList(),
    links: List<MealCategoryMeal> = emptyList()
): List<ListGroup<Meal>> = when (grouping) {
    MealGrouping.NONE -> listOf(ListGroup("all", null, meals))
    MealGrouping.MEAL_TYPE -> {
        val categoryIds = categories.map { it.id }.toSet()
        val categoriesByMeal = links.filter { it.categoryId in categoryIds }.groupBy({ it.mealId }, { it.categoryId })
        categories.mapNotNull { c ->
            meals.filter { c.id in categoriesByMeal[it.id].orEmpty() }
                .takeIf { it.isNotEmpty() }
                ?.let { ListGroup("mealtype_${c.id}", GroupTitle.MealType(c.name, c.defaultKey), it, mealCategoryId = c.id) }
        } + listOfNotNull(
            meals.filter { categoriesByMeal[it.id].isNullOrEmpty() }
                .takeIf { it.isNotEmpty() }
                ?.let { ListGroup("mealtype_none", GroupTitle.NoMealType, it) }
        )
    }
    MealGrouping.NOVA -> {
        val byGroup = meals.groupBy { m -> m.novaClassification?.group?.takeIf { it in NOVA_GROUPS } }
        NOVA_GROUPS.mapNotNull { g -> byGroup[g]?.let { ListGroup("nova_$g", GroupTitle.Nova(g), it) } } +
            listOfNotNull(byGroup[null]?.let { ListGroup("nova_none", GroupTitle.NoNova, it) })
    }
}

/**
 * The exercises listed for a gym selection: a gym shows its own and the shared ones; All gyms
 * ([selectedGymId] null) shows every exercise.
 */
fun exercisesForGym(exercises: List<Exercise>, selectedGymId: Long?): List<Exercise> =
    if (selectedGymId == null) exercises else exercises.filter { it.gymId == selectedGymId || it.gymId == null }

/**
 * Groups already-sorted [exercises] (see [exercisesForGym]).
 * - GYM: with a gym selected, its exercises (untitled) then "Shared"; with All gyms, one group per
 *   gym in [gyms] order, then "Shared" (exercises of an unlisted gym count as shared).
 * - ROUTINE: one group per routine in [routines] order, including routines with no exercise yet
 *   (so a new routine is visible); items follow the routine's own order when [routineOrder] (custom
 *   sort), otherwise the list's sort. An exercise appears in every routine it belongs to;
 *   "no routine" last.
 * - MUSCLE: an exercise appears under every primary muscle; alphabetical; "no muscle group" last.
 * - TYPE: in [ExerciseType] order.
 * Other empty groups are dropped.
 */
fun groupExercises(
    exercises: List<Exercise>,
    grouping: ExerciseGrouping,
    routines: List<Routine> = emptyList(),
    memberships: List<RoutineExercise> = emptyList(),
    gyms: List<Gym> = emptyList(),
    selectedGymId: Long? = null,
    routineOrder: Boolean = true
): List<ListGroup<Exercise>> = when (grouping) {
    ExerciseGrouping.NONE -> listOf(ListGroup("all", null, exercises))
    ExerciseGrouping.GYM -> {
        val knownGymIds = gyms.map { it.id }.toSet()
        val shared = exercises.filter { it.gymId == null || it.gymId !in knownGymIds }
        val gymGroups = if (selectedGymId != null) {
            listOf(ListGroup("gym_$selectedGymId", null, exercises.filter { it.gymId == selectedGymId }))
        } else {
            gyms.map { g -> ListGroup("gym_${g.id}", GroupTitle.GymName(g.name, g.unit), exercises.filter { it.gymId == g.id }) }
        }
        (gymGroups + ListGroup("shared", GroupTitle.Shared, shared)).filter { it.items.isNotEmpty() }
    }
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
        val byId = exercises.associateBy { it.id }
        val routineIds = routines.map { it.id }.toSet()
        val inSomeRoutine = memberships.filter { it.routineId in routineIds }.map { it.exerciseId }.toSet()
        routines.map { r ->
            val links = memberships.filter { it.routineId == r.id }
            val items = if (routineOrder) {
                links.sortedBy { it.position }.mapNotNull { byId[it.exerciseId] }
            } else {
                val ids = links.map { it.exerciseId }.toSet()
                exercises.filter { it.id in ids }
            }
            val gym = gyms.firstOrNull { g -> g.id == r.gymId }?.takeIf { selectedGymId == null }
            ListGroup("routine_${r.id}", GroupTitle.RoutineName(r.name, gym?.let { g -> GroupTitle.GymName(g.name, g.unit) }), items, routineId = r.id)
        } + listOfNotNull(
            exercises.filter { it.id !in inSomeRoutine }
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
