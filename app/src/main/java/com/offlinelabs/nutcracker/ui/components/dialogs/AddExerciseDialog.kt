package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.data.model.ExternalExercise
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.ui.components.routine.routinesForGym
import androidx.compose.material.icons.filled.Check
import com.offlinelabs.nutcracker.ui.components.gym.GymPickerField
import com.offlinelabs.nutcracker.ui.components.gym.GymPickerOption
import com.offlinelabs.nutcracker.ui.components.gym.gymPickerOptions
import com.offlinelabs.nutcracker.data.model.toInternalExercise
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.form.FormDialog
import com.offlinelabs.nutcracker.ui.components.form.FormDialogActions
import com.offlinelabs.nutcracker.ui.components.form.FormDialogStackedActions
import com.offlinelabs.nutcracker.ui.components.form.FormDialogSurface
import com.offlinelabs.nutcracker.ui.components.form.FormSection
import com.offlinelabs.nutcracker.ui.components.form.FormSectionDivider
import com.offlinelabs.nutcracker.ui.components.form.FormStackedAction
import com.offlinelabs.nutcracker.ui.components.form.FormTextField
import com.offlinelabs.nutcracker.ui.components.form.NumericInputRow
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme
import com.offlinelabs.nutcracker.util.logger.AppLogger
import java.util.Locale

@Composable
fun AddExerciseDialog(
    externalExercise: ExternalExercise? = null,
    existingExercise: Exercise? = null,
    /** Gyms offered by the picker (visible ones, plus the exercise's own gym when editing). */
    gyms: List<Gym> = emptyList(),
    /** Gym preselected for a new exercise (null = shared). Ignored when editing. */
    defaultGymId: Long? = null,
    /** Muscle choices besides the selected ones (exercise database + muscles already in use). */
    muscleChoices: List<String> = emptyList(),
    /** Visible routines; the form offers those of its gym (all of them for a shared exercise). */
    routines: List<Routine> = emptyList(),
    /** Routines picked in the form (hoisted, so every save path can store them). */
    selectedRoutineIds: Set<Long> = emptySet(),
    onSelectedRoutineIdsChange: (Set<Long>) -> Unit = {},
    /** Gyms of the copies this exercise is synced with (e.g. "Basic Fit · Uni"); null when not synced. */
    syncedWith: String? = null,
    /** Stops syncing this exercise (applied right away). */
    onUnsync: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onAddExercise: (Exercise) -> Unit,
    onDelete: (() -> Unit)? = null,
    onSaveAndCheckIn: ((Exercise) -> Unit)? = null,
    onJustSave: ((Exercise) -> Unit)? = null,
    onJustCheckIn: ((Exercise) -> Unit)? = null
) {
    val locale = Locale.getDefault()
    var input by remember(externalExercise, existingExercise) {
        AppLogger.i("AddExerciseDialog", "Init with externalExercise: ${externalExercise?.name}, existingExercise: ${existingExercise?.name}")
        mutableStateOf(
            when {
                existingExercise != null -> exerciseFormFrom(existingExercise, locale)
                externalExercise != null -> exerciseFormFrom(externalExercise, locale, gymId = defaultGymId)
                else -> ExerciseFormInput(gymId = defaultGymId)
            }
        )
    }
    // Show "required" for the name only once the user has typed in it (or cleared it).
    var nameTouched by remember(externalExercise, existingExercise) { mutableStateOf(false) }
    // Set once the user stops syncing here, so saving doesn't re-join the old sync group
    var unsynced by remember(existingExercise) { mutableStateOf(false) }
    var confirmUnsync by remember { mutableStateOf(false) }

    val errors = exerciseFormErrors(input)
    val valid = errors.isEmpty()
    val isEdit = existingExercise != null

    // Edit keeps everything the form doesn't show; import starts from the external exercise.
    fun createExercise(): Exercise {
        val base = existingExercise ?: externalExercise?.toInternalExercise() ?: Exercise(name = "")
        val form = applyExerciseForm(base, input)
        return if (unsynced) form.copy(syncGroupId = null) else form
    }

    val showThreeButtons = externalExercise != null && existingExercise == null &&
        onSaveAndCheckIn != null && onJustSave != null && onJustCheckIn != null

    FormDialog(
        title = stringResource(if (isEdit) R.string.edit_exercise else R.string.add_exercise),
        onDismissRequest = onDismiss,
        actions = {
            if (showThreeButtons) {
                FormDialogStackedActions(
                    primaryText = stringResource(R.string.save_and_check_in),
                    onPrimary = { onSaveAndCheckIn(createExercise()) },
                    secondary = listOf(
                        FormStackedAction(stringResource(R.string.just_save)) { onJustSave?.invoke(createExercise()) },
                        FormStackedAction(stringResource(R.string.just_check_in)) { onJustCheckIn?.invoke(createExercise()) }
                    ),
                    onDismiss = onDismiss,
                    confirmEnabled = valid
                )
            } else {
                FormDialogActions(
                    confirmText = stringResource(if (isEdit) R.string.update else R.string.add),
                    onConfirm = { onAddExercise(createExercise()) },
                    onDismiss = onDismiss,
                    confirmEnabled = valid,
                    onDelete = if (isEdit) onDelete else null
                )
            }
        }
    ) {
        ExerciseFormBody(
            input = input,
            onInputChange = { new ->
                if (new.name != input.name) nameTouched = true
                // Another gym offers other routines: keep only the picks still offered
                if (new.gymId != input.gymId) {
                    val offered = routinesForGym(routines, new.gymId).map { it.id }.toSet()
                    onSelectedRoutineIdsChange(selectedRoutineIds intersect offered)
                }
                input = new
            },
            showNameError = nameTouched,
            colors = FormDefaults.colors(),
            gymOptions = gymPickerOptions(gyms),
            muscleChoices = muscleChoices
        )
        val offeredRoutines = routinesForGym(routines, input.gymId)
        if (offeredRoutines.isNotEmpty()) {
            RoutinePicker(
                routines = offeredRoutines,
                gyms = gyms,
                // A shared exercise lists routines of several gyms: name the gym
                showGym = input.gymId == null && offeredRoutines.map { it.gymId }.distinct().size > 1,
                selected = selectedRoutineIds,
                onSelectedChange = onSelectedRoutineIdsChange,
                colors = FormDefaults.colors()
            )
        }
        if (syncedWith != null && onUnsync != null && !unsynced) {
            SyncedWithRow(syncedWith, FormDefaults.colors(), onClick = { confirmUnsync = true })
        }
    }

    if (confirmUnsync && syncedWith != null && onUnsync != null) {
        val colors = FormDefaults.colors()
        AlertDialog(
            onDismissRequest = { confirmUnsync = false },
            icon = { Icon(Icons.Filled.SyncDisabled, contentDescription = null) },
            title = { Text(stringResource(R.string.sync_stop_title)) },
            text = { Text(stringResource(R.string.sync_stop_message, syncedWith)) },
            confirmButton = {
                TextButton(onClick = { confirmUnsync = false; unsynced = true; onUnsync() }) {
                    Text(stringResource(R.string.sync_stop), color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnsync = false }) { Text(stringResource(R.string.cancel), color = colors.accent) }
            }
        )
    }
}

/** Chips of the routines the exercise can join; tapping one adds or removes it. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RoutinePicker(
    routines: List<Routine>,
    gyms: List<Gym>,
    showGym: Boolean,
    selected: Set<Long>,
    onSelectedChange: (Set<Long>) -> Unit,
    colors: FormColors
) {
    val gymsById = remember(gyms) { gyms.associateBy { it.id } }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = stringResource(R.string.routines_button),
            style = MaterialTheme.typography.labelLarge,
            color = colors.contentSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            routines.forEach { routine ->
                val isSelected = routine.id in selected
                val gymName = if (showGym) gymsById[routine.gymId]?.let { com.offlinelabs.nutcracker.data.gymLabel(it.name, it.unit) } else null
                androidx.compose.material3.FilterChip(
                    selected = isSelected,
                    onClick = { onSelectedChange(if (isSelected) selected - routine.id else selected + routine.id) },
                    label = { Text(if (gymName != null) "${routine.name} · $gymName" else routine.name) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accent,
                        selectedLabelColor = colors.onAccent,
                        selectedLeadingIconColor = colors.onAccent
                    )
                )
            }
        }
    }
}

/** "Synced with …" note at the bottom of the form; tapping it offers to stop syncing. */
@Composable
private fun SyncedWithRow(syncedWith: String, colors: FormColors, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = colors.section,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Sync, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(stringResource(R.string.sync_synced_with, syncedWith), style = MaterialTheme.typography.bodyMedium, color = colors.content)
                Text(stringResource(R.string.sync_tap_to_stop), style = MaterialTheme.typography.bodySmall, color = colors.contentSecondary)
            }
        }
    }
}

@Composable
private fun ExerciseFormBody(
    input: ExerciseFormInput,
    onInputChange: (ExerciseFormInput) -> Unit,
    showNameError: Boolean,
    colors: FormColors,
    gymOptions: List<GymPickerOption>? = null,
    muscleChoices: List<String> = emptyList()
) {
    val nameError = if (showNameError) {
        validateExerciseFormField(ExerciseFormField.NAME, input)?.messageRes
    } else {
        null
    }
    FormTextField(
        value = input.name,
        onValueChange = { onInputChange(input.copy(name = it)) },
        label = stringResource(R.string.exercise_name),
        errorText = nameError?.let { stringResource(it) }
    )

    Spacer(Modifier.height(12.dp))
    ExerciseTypeSelector(
        selected = input.type,
        onSelected = { onInputChange(input.copy(type = it)) },
        colors = colors
    )

    if (gymOptions != null) {
        Spacer(Modifier.height(12.dp))
        GymPickerField(
            options = gymOptions,
            selectedGymId = input.gymId,
            onSelected = { onInputChange(input.copy(gymId = it)) },
            colors = colors
        )
    }

    Spacer(Modifier.height(12.dp))
    MusclePicker(
        options = muscleOptions(muscleChoices, emptyList(), input.primaryMuscles),
        selected = input.primaryMuscles,
        onSelectedChange = { onInputChange(input.copy(primaryMuscles = it)) },
        colors = colors
    )

    Spacer(Modifier.height(12.dp))
    FormSection {
        val fields = visibleExerciseFormFields(input.type)
        fields.forEachIndexed { index, field ->
            if (index > 0) FormSectionDivider()
            val error = validateExerciseFormField(field, input)?.let { stringResource(it.messageRes) }
            when (field) {
                ExerciseFormField.KCAL -> NumericInputRow(
                    label = stringResource(
                        when (input.type) {
                            ExerciseType.STRENGTH -> R.string.kcal_per_set
                            ExerciseType.CARDIO -> R.string.kcal_per_minute
                            ExerciseType.BODYWEIGHT -> R.string.kcal_per_rep
                        }
                    ),
                    value = input.kcal,
                    onValueChange = { onInputChange(input.copy(kcal = it)) },
                    unit = stringResource(R.string.unit_kcal),
                    errorText = error
                )
                ExerciseFormField.WEIGHT -> NumericInputRow(
                    label = stringResource(R.string.default_weight),
                    value = input.weight,
                    onValueChange = { onInputChange(input.copy(weight = it)) },
                    unit = stringResource(R.string.kg_unit),
                    errorText = error
                )
                ExerciseFormField.REPS -> NumericInputRow(
                    label = stringResource(R.string.default_reps),
                    value = input.reps,
                    onValueChange = { onInputChange(input.copy(reps = it)) },
                    unit = stringResource(R.string.reps_unit),
                    errorText = error
                )
                ExerciseFormField.SETS -> NumericInputRow(
                    label = stringResource(R.string.default_sets),
                    value = input.sets,
                    onValueChange = { onInputChange(input.copy(sets = it)) },
                    unit = stringResource(R.string.sets_unit),
                    errorText = error
                )
                ExerciseFormField.MINUTES -> NumericInputRow(
                    label = stringResource(R.string.default_minutes),
                    value = input.minutes,
                    onValueChange = { onInputChange(input.copy(minutes = it)) },
                    unit = stringResource(R.string.min_unit),
                    errorText = error
                )
                ExerciseFormField.BODYWEIGHT_REPS -> NumericInputRow(
                    label = stringResource(R.string.default_reps),
                    value = input.bodyweightReps,
                    onValueChange = { onInputChange(input.copy(bodyweightReps = it)) },
                    unit = stringResource(R.string.reps_unit),
                    errorText = error
                )
                ExerciseFormField.NAME -> Unit
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    FormTextField(
        value = input.notes,
        onValueChange = { onInputChange(input.copy(notes = it)) },
        label = stringResource(R.string.notes_optional),
        singleLine = false,
        minLines = 2,
        maxLines = 4,
        imeAction = ImeAction.Default
    )
    Spacer(Modifier.height(4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseTypeSelector(
    selected: ExerciseType,
    onSelected: (ExerciseType) -> Unit,
    colors: FormColors
) {
    val types = ExerciseType.entries
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.exercise_type),
            style = MaterialTheme.typography.labelLarge,
            color = colors.contentSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        // IntrinsicSize.Min + fillMaxHeight keeps all segments the same height when a
        // long localized label (e.g. "Peso Corporal") wraps to two lines.
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            types.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = selected == type,
                    onClick = { onSelected(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, types.size),
                    modifier = Modifier.fillMaxHeight(),
                    icon = {},
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.accent,
                        activeContentColor = colors.onAccent,
                        activeBorderColor = colors.accent,
                        inactiveContainerColor = colors.field,
                        inactiveContentColor = colors.content,
                        inactiveBorderColor = colors.fieldBorder
                    ),
                    label = {
                        Text(
                            text = stringResource(
                                when (type) {
                                    ExerciseType.STRENGTH -> R.string.strength
                                    ExerciseType.CARDIO -> R.string.cardio
                                    ExerciseType.BODYWEIGHT -> R.string.bodyweight
                                }
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2,
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }
        }
    }
}

// ----- Previews -----

@Composable
private fun ExerciseFormPreview(initial: ExerciseFormInput) {
    var input by remember { mutableStateOf(initial) }
    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        FormDialogSurface(
            title = stringResource(R.string.add_exercise),
            actions = {
                FormDialogActions(
                    confirmText = stringResource(R.string.add),
                    onConfirm = {},
                    onDismiss = {},
                    confirmEnabled = exerciseFormErrors(input).isEmpty()
                )
            }
        ) {
            ExerciseFormBody(input, { input = it }, showNameError = true, colors = FormDefaults.colors())
        }
    }
}

@Preview(name = "Add exercise - light", widthDp = 360, heightDp = 780, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun AddExerciseLightPreview() {
    FoodLogTheme(darkTheme = false) {
        ExerciseFormPreview(
            ExerciseFormInput(name = "Bench press", kcal = "2,5", weight = "12,5", reps = "8", sets = "3")
        )
    }
}

@Preview(name = "Add exercise - dark, errors", widthDp = 360, heightDp = 780, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun AddExerciseDarkErrorPreview() {
    FoodLogTheme(darkTheme = true) {
        ExerciseFormPreview(
            ExerciseFormInput(name = "", kcal = "abc", weight = "-1", reps = "1,5", sets = "3")
        )
    }
}
