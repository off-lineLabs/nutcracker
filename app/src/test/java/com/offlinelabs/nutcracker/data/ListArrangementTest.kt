package com.offlinelabs.nutcracker.data

import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.Nutriscore
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.data.model.RoutineExercise
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ListArrangementTest {

    private fun meal(id: Long, name: String, sortOrder: Int = 0, grade: String? = null) = Meal(
        id = id, name = name, calories = 100, carbohydrates_g = 0.0, protein_g = 0.0, fat_g = 0.0,
        fiber_g = 0.0, sodium_mg = 0.0, servingSize_value = 100.0, servingSize_unit = ServingSizeUnit.GRAMS,
        nutriscore = grade?.let { Nutriscore(it, null) }, sortOrder = sortOrder
    )

    private fun ex(id: Long, name: String, sortOrder: Int = 0, category: String = "strength", muscles: List<String> = emptyList()) =
        Exercise(id = id, name = name, sortOrder = sortOrder, category = category, primaryMuscles = muscles)

    private val meals = listOf(meal(1, "banana", 2), meal(2, "Apple", 0), meal(3, "cherry", 0))

    @Test
    fun alphabetical_ignoresCase() {
        assertEquals(listOf(2L, 1L, 3L), sortMeals(meals, ListSort.ALPHABETICAL, emptyMap()).map { it.id })
    }

    @Test
    fun custom_usesSortOrderThenName() {
        assertEquals(listOf(2L, 3L, 1L), sortMeals(meals, ListSort.CUSTOM, emptyMap()).map { it.id })
    }

    @Test
    fun addedDate_newestFirst() {
        assertEquals(listOf(3L, 2L, 1L), sortMeals(meals, ListSort.ADDED_DATE, emptyMap()).map { it.id })
    }

    @Test
    fun lastCheckedIn_mostRecentFirst_neverLast() {
        val last = mapOf(1L to "2026-11-01 08:00:00", 3L to "2026-11-20 19:30:00")
        assertEquals(listOf(3L, 1L, 2L), sortMeals(meals, ListSort.LAST_CHECKED_IN, last).map { it.id })
    }

    @Test
    fun nutriScoreGroups_inGradeOrder_noScoreLast() {
        val list = listOf(meal(1, "a", grade = "c"), meal(2, "b"), meal(3, "c", grade = "A"), meal(4, "d", grade = "x"))
        val groups = groupMeals(list, MealGrouping.NUTRI_SCORE)
        assertEquals(listOf("nutri_A", "nutri_C", "nutri_none"), groups.map { it.key })
        assertEquals(listOf(2L, 4L), groups.last().items.map { it.id })
        assertEquals(GroupTitle.NutriScore("C"), groups[1].title)
    }

    @Test
    fun muscleGroups_exerciseUnderEachMuscle_noneLast() {
        val list = listOf(ex(1, "Bench", muscles = listOf("chest", "Triceps")), ex(2, "Curl", muscles = listOf("biceps")), ex(3, "Run"))
        val groups = groupExercises(list, ExerciseGrouping.MUSCLE)
        assertEquals(listOf("muscle_biceps", "muscle_chest", "muscle_triceps", "muscle_none"), groups.map { it.key })
        assertEquals(listOf(1L), groups[2].items.map { it.id })
        assertEquals(listOf(3L), groups[3].items.map { it.id })
    }

    @Test
    fun typeGroups_followExerciseTypeOrder() {
        val list = listOf(ex(1, "Run", category = "cardio"), ex(2, "Squat"), ex(3, "Stretch", category = "stretching"))
        val groups = groupExercises(list, ExerciseGrouping.TYPE)
        assertEquals(
            listOf(GroupTitle.Type(ExerciseType.STRENGTH), GroupTitle.Type(ExerciseType.CARDIO), GroupTitle.Type(ExerciseType.BODYWEIGHT)),
            groups.map { it.title }
        )
    }

    @Test
    fun routineGroups_manyToMany_inRoutineOrder_noRoutineLast() {
        val list = listOf(ex(1, "Bench"), ex(2, "Squat"), ex(3, "Curl"))
        val routines = listOf(Routine(id = 20, name = "Full body"), Routine(id = 10, name = "Push day"))
        val links = listOf(RoutineExercise(10, 1), RoutineExercise(20, 1), RoutineExercise(20, 2), RoutineExercise(99, 3))
        val groups = groupExercises(list, ExerciseGrouping.ROUTINE, routines, links)
        assertEquals(listOf("routine_20", "routine_10", "routine_none"), groups.map { it.key })
        assertEquals(listOf(1L, 2L), groups[0].items.map { it.id })
        assertEquals(listOf(3L), groups[2].items.map { it.id }) // hidden/unknown routine counts as none
    }

    @Test
    fun reorderWithinGroup_keepsOtherItemsInPlace() {
        // Group {b, d} reordered to [d, b] inside a, b, c, d, e
        assertEquals(listOf("a", "d", "c", "b", "e"), reorderWithinGroup(listOf("a", "b", "c", "d", "e"), listOf("d", "b")))
        assertEquals(listOf(1L, 2L), reorderWithinGroup(listOf(1L, 2L), listOf(1L, 2L)))
    }
}
