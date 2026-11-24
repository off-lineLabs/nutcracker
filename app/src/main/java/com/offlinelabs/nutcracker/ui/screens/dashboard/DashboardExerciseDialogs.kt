package com.offlinelabs.nutcracker.ui.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.gymLabel
import com.offlinelabs.nutcracker.data.model.CheckInData
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.data.reorderWithinGroup
import com.offlinelabs.nutcracker.data.syncPartners
import com.offlinelabs.nutcracker.ui.components.dialogs.AddExerciseDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.UnifiedCheckInDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.UnifiedExerciseDetailsDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.quickCheckIn
import com.offlinelabs.nutcracker.ui.components.dialogs.quickWheelStart
import com.offlinelabs.nutcracker.ui.components.gym.GymDialogsHost
import com.offlinelabs.nutcracker.ui.components.gym.gymsIncludingCurrent
import com.offlinelabs.nutcracker.ui.components.routine.RoutineEditorDialog
import com.offlinelabs.nutcracker.ui.components.routine.RoutinesDialog
import com.offlinelabs.nutcracker.ui.components.routine.routinesForGym
import com.offlinelabs.nutcracker.util.logger.AppLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult

/** Dialogs opened from the Exercise tab: routines, gyms, the exercise form, details and check-in. */
@Composable
internal fun DashboardState.ExerciseDialogs() {
    if (showRoutines) RoutinesDialogHost()
    editingRoutineId?.let { RoutineEditorDialogHost(it) }
    GymDialogsHost(
        state = gymDialogsState,
        gyms = gyms,
        selectedGymId = selectedGymId,
        onSelectedGymIdChange = { selectGym(it) },
        repository = foodLogRepository
    )
    if (showAddExerciseDialog) AddExerciseDialogHost()
    showCheckInExerciseDialog?.let { CheckInExerciseDialogHost(it) }
    showUnifiedExerciseDetailDialog?.let { UnifiedExerciseDetailsDialogHost(it) }
}

@Composable
private fun DashboardState.RoutinesDialogHost() {
    RoutinesDialog(
        routines = routines,
        memberships = routineMemberships,
        gyms = gyms,
        selectedGymId = selectedGymForList,
        onDismiss = { showRoutines = false },
        onNew = { editingRoutineId = NEW_ROUTINE_ID },
        onEdit = { editingRoutineId = it.id },
        onReorder = { shownOrder ->
            // Only the shown routines moved; the others keep their slots in the full order
            val full = routinesForGym(routines, null).map { it.id }
            coroutineScope.launch { foodLogRepository.applyRoutineOrder(reorderWithinGroup(full, shownOrder)) }
        }
    )
}

@Composable
private fun DashboardState.RoutineEditorDialogHost(routineId: Long) {
    val routine = routines.firstOrNull { it.id == routineId }
    // key: a different routine (or a new one) starts with fresh editor state
    key(routineId) {
        RoutineEditorDialog(
            routine = routine,
            initialExerciseIds = routineMemberships.filter { it.routineId == routineId }.sortedBy { it.position }.map { it.exerciseId },
            gyms = gyms,
            exercises = exercises,
            defaultGymId = selectedGymForList,
            onDismiss = { editingRoutineId = null },
            onSave = { toSave, exerciseIds ->
                editingRoutineId = null
                val shownGymId = selectedGymForList
                coroutineScope.launch {
                    foodLogRepository.saveRoutine(toSave, exerciseIds)
                    // Saved at a gym that isn't shown: say where it went and offer to switch
                    if (shownGymId != null && toSave.gymId != shownGymId) {
                        val gymName = gyms.firstOrNull { it.id == toSave.gymId }?.let { gymLabel(it.name, it.unit) }.orEmpty()
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.routine_saved_other_gym, toSave.name, gymName),
                            actionLabel = context.getString(R.string.routine_show_gym),
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) selectGym(toSave.gymId)
                    }
                }
            },
            onDelete = routine?.let { r ->
                {
                    editingRoutineId = null
                    coroutineScope.launch { foodLogRepository.deleteRoutine(r.id) }
                }
            }
        )
    }
}

@Composable
private fun DashboardState.AddExerciseDialogHost() {
    val edited = selectedExerciseForEdit
    val external = selectedExternalExercise
    val editedGymId = edited?.gymId
    // The edited exercise's gym may be hidden (soft-deleted); look it up so the picker can show it.
    val editedGym by produceState<Gym?>(null, editedGymId) {
        value = editedGymId?.let { foodLogRepository.getGymById(it) }
    }
    val sharedGymLabel = stringResource(R.string.gym_all_gyms_shared)
    // The quick buttons of an import from the exercise database (new exercises only)
    val importing = external != null && edited == null
    // Routines picked in the form; an edited exercise starts with the ones it's in
    var chosenRoutineIds by remember(edited?.id) {
        mutableStateOf(edited?.let { e -> routineMemberships.filter { it.exerciseId == e.id }.map { it.routineId }.toSet() } ?: emptySet())
    }

    /** Stores the form's routine picks for [exerciseId], among the routines offered for [gymId]. */
    suspend fun saveRoutines(exerciseId: Long, gymId: Long?, picked: Set<Long>) {
        foodLogRepository.setExerciseRoutines(exerciseId, picked, routinesForGym(routines, gymId).map { it.id }.toSet())
    }

    fun close() {
        showAddExerciseDialog = false
        selectedExternalExercise = null
        selectedExerciseForEdit = null
    }

    /** Saves the imported exercise ([visible] false: only for this check-in) and optionally checks it in with its defaults. */
    fun saveImported(exercise: Exercise, visible: Boolean, checkIn: Boolean, successMessage: String, errorMessageRes: Int) {
        val picked = chosenRoutineIds
        coroutineScope.launch {
            try {
                val exerciseId = insertExerciseWithExternalData(exercise.copy(isVisible = visible), external, keepFormMusclesAndVisibility = true)
                // A check-in-only (hidden) exercise isn't listed, so it joins no routine
                if (visible) saveRoutines(exerciseId, exercise.gymId, picked)
                if (checkIn) {
                    val quick = quickCheckIn(exercise, quickWheelStart(exercise))
                    foodLogRepository.insertExerciseLog(
                        ExerciseLog.create(
                            exerciseId = exerciseId,
                            weight = quick.weight,
                            reps = quick.reps,
                            sets = quick.sets,
                            caloriesBurned = quick.caloriesBurned,
                            notes = null,
                            customDateTime = newCheckInDateTime()
                        )
                    )
                    showCheckInLoggedSnackbar(successMessage)
                    refreshCheckIns++
                } else {
                    snackbarHostState.showSnackbar(message = successMessage)
                }
            } catch (e: Exception) {
                AppLogger.exception("DashboardScreen", "Failed to save imported exercise", e)
                snackbarHostState.showSnackbar(message = context.getString(errorMessageRes, e.message.orEmpty()))
            }
        }
        showAddExerciseDialog = false
        selectedExternalExercise = null
    }

    AddExerciseDialog(
        externalExercise = external,
        existingExercise = edited,
        gyms = gymsIncludingCurrent(gyms, editedGymId, editedGym),
        defaultGymId = selectedGymForList,
        // Database muscles plus any (custom) muscle already used, so custom entries come back
        muscleChoices = remember(exercises) {
            externalExerciseService.getKnownMuscles() + exercises.flatMap { it.primaryMuscles }
        },
        // Gyms of the synced copies, e.g. "G1mart · U1, All gyms (shared)"
        syncedWith = edited?.let {
            syncPartners(it, exercises).takeIf { partners -> partners.isNotEmpty() }?.map { partner ->
                gyms.firstOrNull { gym -> gym.id == partner.gymId }?.let { gym -> gymLabel(gym.name, gym.unit) }
                    ?: sharedGymLabel
            }?.distinct()?.joinToString(", ")
        },
        onUnsync = edited?.let { { coroutineScope.launch { foodLogRepository.unsyncExercise(it.id) } } },
        routines = routines,
        selectedRoutineIds = chosenRoutineIds,
        onSelectedRoutineIdsChange = { chosenRoutineIds = it },
        onDismiss = { close() },
        onAddExercise = { newExercise ->
            val picked = chosenRoutineIds
            coroutineScope.launch {
                try {
                    if (edited != null) {
                        // Keep the existing image paths
                        foodLogRepository.updateExercise(newExercise.copy(id = edited.id, imagePaths = edited.imagePaths))
                        saveRoutines(edited.id, newExercise.gymId, picked)
                        snackbarHostState.showSnackbar(message = context.getString(R.string.exercise_updated_success))
                    } else {
                        val newId = insertExerciseWithExternalData(newExercise, external, keepFormMusclesAndVisibility = false)
                        saveRoutines(newId, newExercise.gymId, picked)
                        snackbarHostState.showSnackbar(message = context.getString(R.string.exercise_added_success))
                    }
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Failed to ${if (edited != null) "update" else "add"} exercise", e, mapOf(
                        "exerciseName" to newExercise.name
                    ))
                    snackbarHostState.showSnackbar(
                        message = context.getString(if (edited != null) R.string.failed_to_update_exercise else R.string.exercise_add_error)
                    )
                }
            }
            close()
        },
        onDelete = edited?.let {
            {
                confirmDeleteExercise(it) {
                    showAddExerciseDialog = false
                    selectedExerciseForEdit = null
                }
            }
        },
        onSaveAndCheckIn = if (importing) {
            { exercise ->
                saveImported(exercise, visible = true, checkIn = true,
                    context.getString(R.string.exercise_saved_and_checked_in), R.string.failed_to_complete_action_with_reason)
            }
        } else null,
        onJustSave = if (importing) {
            { exercise ->
                saveImported(exercise, visible = true, checkIn = false,
                    context.getString(R.string.exercise_saved_success), R.string.failed_to_save_exercise_with_reason)
            }
        } else null,
        onJustCheckIn = if (importing) {
            { exercise ->
                // Not listed in the Exercise tab, only logged
                saveImported(exercise, visible = false, checkIn = true,
                    context.getString(R.string.check_in_completed_success), R.string.failed_to_check_in_with_reason)
            }
        } else null
    )
}

@Composable
private fun DashboardState.CheckInExerciseDialogHost(exerciseToCheckIn: Exercise) {
    fun close() {
        showCheckInExerciseDialog = null
        selectedExerciseForCheckIn = null
        lastExerciseLog = null
        maxExerciseWeight = null
    }
    UnifiedCheckInDialog<CheckInData.Exercise>(
        exercise = exerciseToCheckIn,
        defaultDateTime = remember(exerciseToCheckIn) { newCheckInDateTime() },
        lastLog = lastExerciseLog,
        maxWeight = maxExerciseWeight,
        onDismiss = { close() },
        onCheckIn = { checkInData ->
            coroutineScope.launch {
                try {
                    foodLogRepository.insertExerciseLog(checkInData.exerciseLog)
                    showCheckInLoggedSnackbar(context.getString(R.string.exercise_check_in_completed_success))
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Failed to complete exercise check-in", e, mapOf(
                        "exerciseId" to checkInData.exerciseLog.exerciseId,
                        "date" to checkInData.exerciseLog.logDate
                    ))
                    snackbarHostState.showSnackbar(message = context.getString(R.string.failed_to_complete_exercise_check_in))
                }
            }
            close()
        }
    )
}

@Composable
private fun DashboardState.UnifiedExerciseDetailsDialogHost(exercise: Exercise) {
    // Includes hidden gyms, so the header still names a deleted gym.
    val exerciseGym by produceState<Gym?>(null, exercise.gymId) {
        value = exercise.gymId?.let { foodLogRepository.getGymById(it) }
    }
    UnifiedExerciseDetailsDialog(
        exercise = exercise,
        gym = exerciseGym,
        gyms = gyms,
        onCopyToGym = { targetGymId, sync ->
            // Close the dialog so the confirmation snackbar is visible.
            showUnifiedExerciseDetailDialog = null
            coroutineScope.launch {
                try {
                    foodLogRepository.copyExerciseToGym(exercise, targetGymId, sync)
                    val targetName = gyms.firstOrNull { it.id == targetGymId }
                        ?.let { gymLabel(it.name, it.unit) }
                        ?: context.getString(R.string.gym_all_gyms_shared)
                    snackbarHostState.showSnackbar(message = context.getString(R.string.gym_copied_format, targetName))
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Failed to copy exercise to gym", e, mapOf(
                        "exerciseId" to exercise.id.toString()
                    ))
                    snackbarHostState.showSnackbar(message = context.getString(R.string.gym_copy_failed))
                }
            }
        },
        externalExerciseService = externalExerciseService,
        exerciseImageService = exerciseImageService,
        foodLogRepository = foodLogRepository,
        onBack = { showUnifiedExerciseDetailDialog = null },
        onEdit = {
            showUnifiedExerciseDetailDialog = null
            selectedExerciseForEdit = exercise
            showAddExerciseDialog = true
        },
        onCheckIn = {
            showUnifiedExerciseDetailDialog = null
            coroutineScope.launch {
                try {
                    val lastLog = foodLogRepository.getLastLogForExercise(exercise.id).first()
                    val maxWeight = foodLogRepository.getMaxWeightForExercise(exercise.id).first()
                    selectedExerciseForCheckIn = exercise
                    lastExerciseLog = lastLog
                    maxExerciseWeight = maxWeight
                    showCheckInExerciseDialog = exercise
                } catch (e: Exception) {
                    AppLogger.exception("DashboardScreen", "Failed to get exercise data for check-in", e)
                    snackbarHostState.showSnackbar(message = context.getString(R.string.failed_to_load_exercise_data))
                }
            }
        }
    )
}
