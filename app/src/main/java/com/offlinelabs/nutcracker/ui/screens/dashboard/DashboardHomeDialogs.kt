package com.offlinelabs.nutcracker.ui.screens.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.dao.DailyExerciseEntry
import com.offlinelabs.nutcracker.data.dao.DailyNutritionEntry
import com.offlinelabs.nutcracker.data.model.CheckInData
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.Pill
import com.offlinelabs.nutcracker.ui.components.dialogs.SetGoalDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.UnifiedCheckInDialog
import com.offlinelabs.nutcracker.ui.components.form.ConfirmDeleteDialog
import com.offlinelabs.nutcracker.ui.theme.appSurfaceColor
import com.offlinelabs.nutcracker.ui.theme.appTextInverseColor
import com.offlinelabs.nutcracker.ui.theme.appTextPrimaryColor
import com.offlinelabs.nutcracker.ui.theme.appTextSecondaryColor
import com.offlinelabs.nutcracker.ui.theme.brandPrimaryColor
import com.offlinelabs.nutcracker.ui.theme.dialogOutlinedTextFieldColorsMaxContrast
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor
import com.offlinelabs.nutcracker.util.logger.AppLogger
import com.offlinelabs.nutcracker.util.logger.logUserAction
import com.offlinelabs.nutcracker.util.logger.safeSuspendExecute
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Dialogs opened from the header and the Home tab, plus the shared delete confirmation. */
@Composable
internal fun DashboardState.HomeDialogs() {
    if (showSetGoalDialog) SetGoalDialogHost()
    if (showCalendarDialog) CalendarDialogHost()
    showEditPillDialog?.let { EditPillDialogHost(it) }
    if (showAddPillDialog) AddPillDialogHost()
    showEditMealDialog?.let { EditMealCheckInDialogHost(it) }
    showEditExerciseDialog?.let { EditExerciseLogDialogHost(it) }
    // Shared delete confirmation (cancel leaves the parent dialog untouched)
    pendingDelete?.let { pending ->
        ConfirmDeleteDialog(
            title = pending.title,
            message = pending.message,
            onConfirm = {
                pendingDelete = null
                pending.onConfirm()
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun DashboardState.SetGoalDialogHost() {
    SetGoalDialog(
        currentUserGoal = userGoal,
        onDismiss = { showSetGoalDialog = false },
        onSetGoal = { updatedGoal ->
            coroutineScope.launch {
                safeSuspendExecute(
                    operation = "Update user goal",
                    block = {
                        foodLogRepository.upsertUserGoal(updatedGoal)
                        logUserAction("Goal Updated", mapOf(
                            "calories" to updatedGoal.caloriesGoal,
                            "protein" to updatedGoal.proteinGoal_g,
                            "carbs" to updatedGoal.carbsGoal_g,
                            "fat" to updatedGoal.fatGoal_g
                        ))
                    },
                    onError = { e ->
                        AppLogger.exception("DashboardScreen", "Failed to save user goal", e, mapOf(
                            "goal" to updatedGoal.toString()
                        ))
                        showSnackbar(context.getString(R.string.goal_save_error))
                    }
                )
                snackbarHostState.showSnackbar(message = context.getString(R.string.goal_saved_success))
            }
            showSetGoalDialog = false
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardState.CalendarDialogHost() {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = { showCalendarDialog = false },
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    showCalendarDialog = false
                }
            ) {
                Text(stringResource(R.string.ok), color = brandPrimaryColor())
            }
        },
        dismissButton = {
            TextButton(onClick = { showCalendarDialog = false }) {
                Text(stringResource(R.string.cancel), color = appTextSecondaryColor())
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = appSurfaceColor(),
            titleContentColor = appTextPrimaryColor(),
            headlineContentColor = appTextPrimaryColor(),
            weekdayContentColor = appTextSecondaryColor(),
            subheadContentColor = appTextPrimaryColor(),
            yearContentColor = appTextPrimaryColor(),
            currentYearContentColor = brandPrimaryColor(),
            selectedYearContentColor = appTextInverseColor(),
            selectedYearContainerColor = brandPrimaryColor(),
            dayContentColor = appTextPrimaryColor(),
            selectedDayContentColor = appTextInverseColor(),
            selectedDayContainerColor = brandPrimaryColor(),
            todayContentColor = brandPrimaryColor(),
            todayDateBorderColor = brandPrimaryColor()
        )
    ) {
        DatePicker(state = datePickerState, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DashboardState.EditPillDialogHost(pill: Pill) {
    var pillName by remember { mutableStateOf(pill.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dialogTextFieldColors = dialogOutlinedTextFieldColorsMaxContrast()

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.confirm_delete_pill_title),
            message = stringResource(R.string.confirm_delete_pill_message),
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                coroutineScope.launch {
                    try {
                        // The pill's check-ins first, then the pill
                        foodLogRepository.getPillCheckInsByPillId(pill.id).first().forEach { checkIn ->
                            foodLogRepository.deletePillCheckIn(checkIn)
                        }
                        foodLogRepository.deletePill(pill)
                        snackbarHostState.showSnackbar(message = context.getString(R.string.pill_deleted_success))
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Failed to delete pill", e, mapOf("pillId" to pill.id.toString()))
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.failed_to_delete_pill_with_reason, e.message.orEmpty())
                        )
                    }
                }
                showDeleteConfirm = false
                showEditPillDialog = null
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = { showEditPillDialog = null },
            title = { Text(stringResource(R.string.edit_pill)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = pillName,
                        onValueChange = { pillName = it },
                        label = { Text(stringResource(R.string.pill_name)) },
                        placeholder = { Text(stringResource(R.string.pill_name_placeholder)) },
                        colors = dialogTextFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pillName.isNotBlank()) {
                            coroutineScope.launch {
                                try {
                                    foodLogRepository.updatePill(pill.copy(name = pillName.trim()))
                                    snackbarHostState.showSnackbar(message = context.getString(R.string.pill_updated_success))
                                } catch (e: Exception) {
                                    AppLogger.exception("DashboardScreen", "Failed to update pill", e, mapOf("pillId" to pill.id.toString()))
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.failed_to_update_pill_with_reason, e.message.orEmpty())
                                    )
                                }
                            }
                            showEditPillDialog = null
                        }
                    },
                    enabled = pillName.isNotBlank() && pillName.trim() != pill.name
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                Row {
                    // The default pill (id 1) can't be deleted
                    if (pill.id != 1L) {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = { showEditPillDialog = null }) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun DashboardState.AddPillDialogHost() {
    var pillName by remember { mutableStateOf("") }
    val dialogTextFieldColors = dialogOutlinedTextFieldColorsMaxContrast()
    AlertDialog(
        onDismissRequest = { showAddPillDialog = false },
        title = { Text(stringResource(R.string.add_pill)) },
        text = {
            OutlinedTextField(
                value = pillName,
                onValueChange = { pillName = it },
                label = { Text(stringResource(R.string.pill_name)) },
                placeholder = { Text(stringResource(R.string.pill_name_placeholder)) },
                colors = dialogTextFieldColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pillName.isNotBlank() && pills.size < 5) {
                        coroutineScope.launch {
                            try {
                                foodLogRepository.insertPill(Pill(name = pillName.trim()))
                                snackbarHostState.showSnackbar(message = context.getString(R.string.pill_added_success))
                            } catch (e: Exception) {
                                AppLogger.exception("DashboardScreen", "Failed to add pill", e)
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.failed_to_add_pill_with_reason, e.message.orEmpty())
                                )
                            }
                        }
                        showAddPillDialog = false
                    }
                },
                enabled = pillName.isNotBlank() && pills.size < 5
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = { showAddPillDialog = false }) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                )
            }
        }
    )
}

@Composable
private fun DashboardState.EditMealCheckInDialogHost(checkIn: DailyNutritionEntry) {
    // The meal straight from the database (hidden meals included)
    var meal by remember { mutableStateOf<Meal?>(null) }
    LaunchedEffect(checkIn.mealId) {
        foodLogRepository.getMealById(checkIn.mealId).collectLatest { meal = it }
    }
    val foundMeal = meal ?: return
    val existingMealCheckIn = MealCheckIn(
        id = checkIn.checkInId,
        mealId = checkIn.mealId,
        checkInDate = checkIn.checkInDate,
        checkInDateTime = checkIn.checkInDateTime,
        servingSize = checkIn.servingSize,
        notes = checkIn.notes
    )
    UnifiedCheckInDialog<CheckInData.Meal>(
        onDismiss = { showEditMealDialog = null },
        onCheckIn = { checkInData ->
            coroutineScope.launch {
                try {
                    foodLogRepository.updateMealCheckIn(checkInData.mealCheckIn)
                    snackbarHostState.showSnackbar(message = context.getString(R.string.meal_updated_success))
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Failed to update meal", e, mapOf(
                        "mealId" to existingMealCheckIn.mealId,
                        "date" to existingMealCheckIn.checkInDate
                    ))
                    snackbarHostState.showSnackbar(message = context.getString(R.string.failed_to_update_meal))
                }
            }
            showEditMealDialog = null
        },
        onDelete = {
            pendingDelete = PendingDelete(
                title = context.getString(R.string.confirm_delete_check_in_title),
                message = context.getString(R.string.confirm_delete_check_in_message, foundMeal.name)
            ) {
                coroutineScope.launch {
                    try {
                        foodLogRepository.deleteMealCheckIn(existingMealCheckIn)
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.meal_deleted_success),
                            actionLabel = context.getString(R.string.undo),
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            // REPLACE restores the same row (same id) with identical values
                            foodLogRepository.insertMealCheckIn(existingMealCheckIn)
                        }
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Failed to delete meal", e, mapOf(
                            "mealId" to existingMealCheckIn.mealId,
                            "date" to existingMealCheckIn.checkInDate
                        ))
                        snackbarHostState.showSnackbar(message = context.getString(R.string.failed_to_delete_meal))
                    }
                }
                showEditMealDialog = null
            }
        },
        isEditMode = true,
        meal = foundMeal,
        existingMealCheckIn = existingMealCheckIn
    )
}

@Composable
private fun DashboardState.EditExerciseLogDialogHost(exerciseEntry: DailyExerciseEntry) {
    // The exercise straight from the database (hidden exercises included)
    var exercise by remember { mutableStateOf<Exercise?>(null) }
    LaunchedEffect(exerciseEntry.exerciseId) {
        foodLogRepository.getExerciseById(exerciseEntry.exerciseId).collectLatest { exercise = it }
    }
    val foundExercise = exercise ?: return
    val existingExerciseLog = ExerciseLog(
        id = exerciseEntry.logId,
        exerciseId = exerciseEntry.exerciseId,
        logDate = exerciseEntry.logDate,
        logDateTime = exerciseEntry.logDateTime,
        weight = exerciseEntry.weight,
        reps = exerciseEntry.reps,
        sets = exerciseEntry.sets,
        caloriesBurned = exerciseEntry.caloriesBurned,
        notes = exerciseEntry.notes
    )
    UnifiedCheckInDialog<CheckInData.Exercise>(
        exercise = foundExercise,
        existingExerciseLog = existingExerciseLog,
        isEditMode = true,
        onDismiss = { showEditExerciseDialog = null },
        onCheckIn = { checkInData ->
            coroutineScope.launch {
                try {
                    foodLogRepository.updateExerciseLog(checkInData.exerciseLog)
                    snackbarHostState.showSnackbar(message = context.getString(R.string.exercise_updated_success))
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Failed to update exercise", e, mapOf(
                        "exerciseId" to existingExerciseLog.exerciseId,
                        "date" to existingExerciseLog.logDate
                    ))
                    snackbarHostState.showSnackbar(message = context.getString(R.string.failed_to_update_exercise))
                }
            }
            showEditExerciseDialog = null
        },
        onDelete = {
            pendingDelete = PendingDelete(
                title = context.getString(R.string.confirm_delete_exercise_log_title),
                message = context.getString(R.string.confirm_delete_exercise_log_message, foundExercise.name)
            ) {
                coroutineScope.launch {
                    try {
                        foodLogRepository.deleteExerciseLog(existingExerciseLog)
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.exercise_deleted_success),
                            actionLabel = context.getString(R.string.undo),
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            // REPLACE restores the same row (same id) with identical values
                            foodLogRepository.insertExerciseLog(existingExerciseLog)
                        }
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Failed to delete exercise", e, mapOf(
                            "exerciseId" to existingExerciseLog.exerciseId,
                            "date" to existingExerciseLog.logDate
                        ))
                        snackbarHostState.showSnackbar(message = context.getString(R.string.failed_to_delete_exercise))
                    }
                }
                showEditExerciseDialog = null
            }
        }
    )
}
