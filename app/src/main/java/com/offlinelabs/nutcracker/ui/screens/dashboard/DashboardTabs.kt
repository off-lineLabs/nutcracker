package com.offlinelabs.nutcracker.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.ListSort
import com.offlinelabs.nutcracker.data.MealGrouping
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.ExerciseSet
import com.offlinelabs.nutcracker.ui.components.dialogs.DayLog
import com.offlinelabs.nutcracker.ui.components.dialogs.PlannedSet
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.ui.components.dialogs.ExercisePickerContent
import com.offlinelabs.nutcracker.ui.components.dialogs.MealPickerContent
import com.offlinelabs.nutcracker.ui.components.dialogs.quickCheckIn
import com.offlinelabs.nutcracker.ui.components.gym.GymDropdown
import com.offlinelabs.nutcracker.ui.components.gym.selectedGymOrNull
import com.offlinelabs.nutcracker.ui.components.lists.ListArrangementBar
import com.offlinelabs.nutcracker.ui.components.lists.exerciseGroupingLabel
import com.offlinelabs.nutcracker.ui.components.lists.mealGroupingLabel
import com.offlinelabs.nutcracker.util.logger.AppLogger
import kotlinx.coroutines.launch

/** The Food tab: the food list with search, scan, recipes, order and grouping. */
@Composable
internal fun DashboardState.FoodTab() {
    MealPickerContent(
        meals = meals,
        onAddMeal = { showAddMealDialog = true },
        onSelectMeal = { meal -> showCheckInMealDialog = meal },
        onEditMeal = { meal -> openEditMealDefinition(meal) },
        onSearchMeal = { showFoodSearchDialog = true },
        onScanBarcode = { showBarcodeScanDialog = true },
        onSelectRecipe = { showSelectRecipeDialog = true },
        registerElementCoordinates = registerElementCoordinates,
        groups = mealGroups,
        canReorder = { mealSort == ListSort.CUSTOM },
        onReorder = { key, ids -> saveMealOrder(key, ids) },
        arrangementBar = {
            ListArrangementBar(
                sort = mealSort,
                descending = mealDescending,
                onSortChange = { changeMealSort(it) },
                onDescendingChange = { changeMealDescending(it) },
                grouping = mealGrouping,
                groupings = MealGrouping.entries,
                groupingLabel = { mealGroupingLabel(it) },
                onGroupingChange = { changeMealGrouping(it) }
            )
        },
        controlsFirst = controlsFirst,
        checkedInIds = remember(dailyMealCheckIns) { dailyMealCheckIns.map { it.mealId }.toSet() },
        checkedInCategoryKeys = remember(dailyMealCheckIns) { dailyMealCheckIns.map { it.mealId to it.mealCategoryId }.toSet() },
        lastServings = mealLastServings,
        onQuickCheckIn = { meal, multiplier, categoryId -> quickCheckInMeal(meal, multiplier, categoryId) },
        onRemoveCheckIns = { meal, categoryId -> removeMealDayCheckIns(meal, categoryId) },
        onDeleteMeal = { confirmDeleteMeal(it) }
    )
}

/**
 * The Exercise tab: gym dropdown, routines, the exercise list (order, grouping, swipe actions)
 * and the rows' quick check-in.
 */
@Composable
internal fun DashboardState.ExerciseTab() {
    ExercisePickerContent(
        exercises = listedExercises,
        groups = exerciseGroups,
        // In custom order rows drag; inside a routine group that edits the routine's own order
        canReorder = { exerciseSort == ListSort.CUSTOM },
        onReorder = { key, ids -> saveExerciseOrder(key, ids) },
        arrangementBar = {
            ListArrangementBar(
                sort = exerciseSort,
                descending = exerciseDescending,
                onSortChange = { changeExerciseSort(it) },
                onDescendingChange = { changeExerciseDescending(it) },
                grouping = effectiveExerciseGrouping,
                groupings = exerciseGroupings,
                groupingLabel = { exerciseGroupingLabel(it) },
                onGroupingChange = { changeExerciseGrouping(it) }
            )
        },
        header = { ExerciseTabHeader() },
        externalExerciseService = externalExerciseService,
        foodLogRepository = foodLogRepository,
        onAddExercise = { showAddExerciseDialog = true },
        onSelectExercise = { exercise -> showUnifiedExerciseDetailDialog = exercise },
        onImportExternalExercise = { externalExercise ->
            // Pre-populates the AddExerciseDialog
            selectedExternalExercise = externalExercise
            showAddExerciseDialog = true
        },
        onEditExercise = { exercise ->
            selectedExerciseForEdit = exercise
            showAddExerciseDialog = true
        },
        controlsFirst = controlsFirst,
        checkedInIds = remember(dailyExerciseLogs) { dailyExerciseLogs.map { it.exerciseId }.toSet() },
        checkedInRoutineKeys = remember(dailyExerciseLogs) { dailyExerciseLogs.map { it.exerciseId to it.routineId }.toSet() },
        maxWeights = exerciseMaxWeights,
        lastReps = exerciseLastReps,
        onQuickCheckIn = { exercise, value, reps, routineId -> quickCheckInExercise(exercise, value, reps, routineId) },
        onRemoveCheckIns = { exercise, routineId -> removeDayCheckIns(exercise, routineId) },
        dayLogs = remember(dailyExerciseLogs) {
            dailyExerciseLogs.map { DayLog(it.logId, it.exerciseId, it.routineId, it.logDateTime) }
        },
        daySets = dailyExerciseSets,
        onCheckInSets = { exercise, sessionLogId, sets, routineId -> checkInExerciseSets(exercise, sessionLogId, sets, routineId) },
        onUncheckSet = { exercise, logId, setIndex ->
            coroutineScope.launch { foodLogRepository.uncheckSet(logId, setIndex, exercise.kcalBurnedPerUnit ?: 0.0) }
        },
        onDeleteExercise = { confirmDeleteExercise(it) }
    )
}

@Composable
private fun DashboardState.ExerciseTabHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GymDropdown(
            selectedGym = selectedGymOrNull(gymSelection, gyms),
            gyms = gyms,
            onSelectAll = { selectGym(null) },
            onSelectGym = { selectGym(it) },
            onAddGym = { gymDialogsState.showAdd = true },
            onManageGyms = { gymDialogsState.showManage = true }
        )
        OutlinedButton(
            onClick = { showRoutines = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Icon(Icons.Filled.FormatListNumbered, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            val routinesLabel = stringResource(R.string.routines_button)
            Text(if (listedRoutines.isEmpty()) routinesLabel else "$routinesLabel · ${listedRoutines.size}")
        }
    }
}

/**
 * Quick check-in from the row: the wheels' values plus the exercise's defaults, on the day shown,
 * remembering the routine group it was ticked in (if any).
 */
private fun DashboardState.quickCheckInExercise(exercise: Exercise, value: Double, reps: Int, routineId: Long?) {
    coroutineScope.launch {
        try {
            val quick = quickCheckIn(exercise, value, reps)
            foodLogRepository.insertExerciseLog(
                ExerciseLog.create(
                    exerciseId = exercise.id,
                    weight = quick.weight,
                    reps = quick.reps,
                    sets = quick.sets,
                    caloriesBurned = quick.caloriesBurned,
                    notes = null,
                    customDateTime = newCheckInDateTime(),
                    routineId = routineId
                )
            )
            showCheckInLoggedSnackbar(context.getString(R.string.exercise_check_in_completed_success))
        } catch (e: Exception) {
            AppLogger.exception("DashboardScreen", "Quick check-in failed", e)
            snackbarHostState.showSnackbar(message = context.getString(R.string.failed_to_complete_exercise_check_in))
        }
    }
}

/**
 * Unticking removes that day's check-ins of the exercise, with Undo: in a routine group the ones
 * made under that routine (and those without one, which show there too), elsewhere all of them.
 */
private fun DashboardState.removeDayCheckIns(exercise: Exercise, routineId: Long?) {
    coroutineScope.launch {
        val removed = foodLogRepository.removeExerciseCheckIns(exercise.id, selectedDateString, routineId)
        if (removed.isNotEmpty()) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.quick_check_ins_removed),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) foodLogRepository.restoreExerciseLogs(removed)
        }
    }
}

/**
 * Ticks sets of a strength exercise from its row or set list, each with its own weight and reps,
 * on the day shown (the tick time is kept per set): into the day's session, or a new check-in
 * under the group's routine. Starting a session shows the "Logged" message.
 */
private fun DashboardState.checkInExerciseSets(exercise: Exercise, sessionLogId: Long?, sets: List<PlannedSet>, routineId: Long?) {
    if (sets.isEmpty()) return
    coroutineScope.launch {
        try {
            val at = newCheckInDateTime()
            val stamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(at)
            // The summary is recomputed from the sets once they're stored
            val newLog = ExerciseLog.create(
                exerciseId = exercise.id,
                weight = sets.maxOf { it.weight },
                reps = sets.last().reps,
                sets = sets.size,
                caloriesBurned = 0.0,
                customDateTime = at,
                routineId = routineId
            )
            foodLogRepository.checkInSets(
                sessionLogId,
                newLog,
                sets.map { ExerciseSet(logId = 0, setIndex = it.index, weight = it.weight, reps = it.reps, completedAt = stamp) },
                exercise.kcalBurnedPerUnit ?: 0.0
            )
            if (sessionLogId == null) showCheckInLoggedSnackbar(context.getString(R.string.exercise_check_in_completed_success))
        } catch (e: Exception) {
            AppLogger.exception("DashboardScreen", "Set check-in failed", e)
            snackbarHostState.showSnackbar(message = context.getString(R.string.failed_to_complete_exercise_check_in))
        }
    }
}

/** Quick check-in of a food from its row: the portion wheel, on the day shown, under the group's meal type. */
private fun DashboardState.quickCheckInMeal(meal: Meal, servingMultiplier: Double, mealCategoryId: Long?) {
    coroutineScope.launch {
        try {
            foodLogRepository.insertMealCheckIn(
                MealCheckIn.create(
                    mealId = meal.id,
                    servingSize = servingMultiplier,
                    customDateTime = newCheckInDateTime(),
                    mealCategoryId = mealCategoryId
                )
            )
            showCheckInLoggedSnackbar(context.getString(R.string.check_in_completed_success))
        } catch (e: Exception) {
            AppLogger.exception("DashboardScreen", "Quick meal check-in failed", e)
            snackbarHostState.showSnackbar(message = context.getString(R.string.check_in_completed_error))
        }
    }
}

/**
 * Unticking a food removes that day's check-ins of it, with Undo: in a meal type group that type's
 * (and those without a type, which show there too), elsewhere all of them.
 */
private fun DashboardState.removeMealDayCheckIns(meal: Meal, mealCategoryId: Long?) {
    coroutineScope.launch {
        val removed = foodLogRepository.removeMealCheckIns(meal.id, selectedDateString, mealCategoryId)
        if (removed.isNotEmpty()) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.quick_check_ins_removed),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) foodLogRepository.restoreMealCheckIns(removed)
        }
    }
}

/** Asks before deleting [meal] (from the row's swipe actions or its edit form). */
internal fun DashboardState.confirmDeleteMeal(meal: Meal, onDeleted: () -> Unit = {}) {
    pendingDelete = PendingDelete(
        title = context.getString(R.string.confirm_delete_meal_title),
        message = context.getString(R.string.confirm_delete_meal_message, meal.name)
    ) {
        coroutineScope.launch {
            try {
                foodLogRepository.deleteMeal(meal)
                snackbarHostState.showSnackbar(message = context.getString(R.string.meal_deleted_success))
            } catch (e: Exception) {
                AppLogger.exception("DashboardScreen", "Failed to delete meal", e, mapOf(
                    "mealId" to meal.id.toString(),
                    "mealName" to meal.name
                ))
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.failed_to_delete_meal_with_reason, e.message.orEmpty())
                )
            }
        }
        onDeleted()
    }
}

/** Asks before deleting [exercise] (from the row's swipe actions or its edit form). */
internal fun DashboardState.confirmDeleteExercise(exercise: Exercise, onDeleted: () -> Unit = {}) {
    pendingDelete = PendingDelete(
        title = context.getString(R.string.confirm_delete_exercise_title),
        message = context.getString(R.string.confirm_delete_exercise_message, exercise.name)
    ) {
        coroutineScope.launch {
            try {
                foodLogRepository.deleteExercise(exercise)
                snackbarHostState.showSnackbar(message = context.getString(R.string.exercise_deleted_success))
            } catch (e: Exception) {
                AppLogger.exception("DashboardScreen", "Failed to delete exercise", e, mapOf(
                    "exerciseId" to exercise.id.toString(),
                    "exerciseName" to exercise.name
                ))
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.failed_to_delete_exercise_with_reason, e.message.orEmpty())
                )
            }
        }
        onDeleted()
    }
}
