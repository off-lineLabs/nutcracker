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
        }
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
        checkedInIds = remember(dailyExerciseLogs) { dailyExerciseLogs.map { it.exerciseId }.toSet() },
        maxWeights = exerciseMaxWeights,
        onQuickCheckIn = { exercise, value -> quickCheckInExercise(exercise, value) },
        onRemoveCheckIns = { removeDayCheckIns(it) },
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

/** Quick check-in from the row: the wheel's value plus the exercise's defaults, on the day shown. */
private fun DashboardState.quickCheckInExercise(exercise: Exercise, value: Double) {
    coroutineScope.launch {
        try {
            val quick = quickCheckIn(exercise, value)
            foodLogRepository.insertExerciseLog(
                ExerciseLog.create(
                    exerciseId = exercise.id,
                    weight = quick.weight,
                    reps = quick.reps,
                    sets = quick.sets,
                    caloriesBurned = quick.caloriesBurned,
                    notes = null,
                    customDateTime = newCheckInDateTime()
                )
            )
            showCheckInLoggedSnackbar(context.getString(R.string.exercise_check_in_completed_success))
        } catch (e: Exception) {
            AppLogger.exception("DashboardScreen", "Quick check-in failed", e)
            snackbarHostState.showSnackbar(message = context.getString(R.string.failed_to_complete_exercise_check_in))
        }
    }
}

/** Unticking removes all of that day's check-ins of the exercise, with Undo. */
private fun DashboardState.removeDayCheckIns(exercise: Exercise) {
    coroutineScope.launch {
        val removed = foodLogRepository.removeExerciseCheckIns(exercise.id, selectedDateString)
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
