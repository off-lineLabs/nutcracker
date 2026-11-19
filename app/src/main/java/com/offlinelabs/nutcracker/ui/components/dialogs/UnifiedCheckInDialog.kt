package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.CheckInData
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseCategoryMapper
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import com.offlinelabs.nutcracker.ui.components.DateTimePicker
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.form.FormDialog
import com.offlinelabs.nutcracker.ui.components.form.FormDialogActions
import com.offlinelabs.nutcracker.ui.components.form.FormDialogSurface
import com.offlinelabs.nutcracker.ui.components.form.FormRowIcon
import com.offlinelabs.nutcracker.ui.components.form.FormSection
import com.offlinelabs.nutcracker.ui.components.form.FormSectionDivider
import com.offlinelabs.nutcracker.ui.components.form.FormTextField
import com.offlinelabs.nutcracker.ui.components.form.NumericInputRow
import com.offlinelabs.nutcracker.ui.components.form.formatDecimal
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme
import com.offlinelabs.nutcracker.ui.theme.brandAccentShade
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun <T : CheckInData> UnifiedCheckInDialog(
    // Common parameters
    onDismiss: () -> Unit,
    onCheckIn: (T) -> Unit,
    onDelete: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    
    // Exercise-specific parameters
    exercise: Exercise? = null,
    lastLog: ExerciseLog? = null,
    maxWeight: Double? = null,
    existingExerciseLog: ExerciseLog? = null,
    
    // Meal-specific parameters
    meal: Meal? = null,
    existingMealCheckIn: MealCheckIn? = null
) {
    // Determine if this is an exercise or meal check-in
    val isExercise = exercise != null
    val isMeal = meal != null
    
    require((isExercise && !isMeal) || (!isExercise && isMeal)) { "Exactly one of exercise or meal must be provided" }
    
    if (isExercise) {
        ExerciseCheckInContent(
            exercise = exercise!!,
            lastLog = lastLog,
            maxWeight = maxWeight,
            existingExerciseLog = existingExerciseLog,
            isEditMode = isEditMode,
            onDismiss = onDismiss,
            onCheckIn = { exerciseLog -> 
                @Suppress("UNCHECKED_CAST")
                onCheckIn(CheckInData.Exercise(exerciseLog) as T)
            },
            onDelete = onDelete
        )
    } else {
        MealCheckInContent(
            meal = meal!!,
            existingMealCheckIn = existingMealCheckIn,
            isEditMode = isEditMode,
            onDismiss = onDismiss,
            onCheckIn = { mealCheckIn -> 
                @Suppress("UNCHECKED_CAST")
                onCheckIn(CheckInData.Meal(mealCheckIn) as T)
            },
            onDelete = onDelete
        )
    }
}

private fun parseStoredDateTime(value: String?): Date = try {
    value?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).parse(it) } ?: Date()
} catch (e: Exception) {
    Date()
}

// ----- Exercise -----

@Composable
private fun ExerciseCheckInContent(
    exercise: Exercise,
    lastLog: ExerciseLog?,
    maxWeight: Double?,
    existingExerciseLog: ExerciseLog?,
    isEditMode: Boolean,
    onDismiss: () -> Unit,
    onCheckIn: (ExerciseLog) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val editing = if (isEditMode) existingExerciseLog else null
    var weight by remember {
        mutableStateOf(formatDecimal(editing?.weight ?: lastLog?.weight ?: exercise.defaultWeight))
    }
    var reps by remember { mutableStateOf((editing?.reps ?: lastLog?.reps ?: exercise.defaultReps).toString()) }
    var sets by remember { mutableStateOf((editing?.sets ?: lastLog?.sets ?: exercise.defaultSets).toString()) }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    var selectedDateTime by remember { mutableStateOf(parseStoredDateTime(editing?.logDateTime)) }
    var dateTimeValid by remember { mutableStateOf(true) }

    val exerciseType = remember(exercise) { ExerciseCategoryMapper.getExerciseType(exercise.category) }

    // Calculate calories burned
    val caloriesBurned = remember(reps, sets, exercise) {
        val kcalPerUnit = exercise.kcalBurnedPerUnit ?: 0.0
        when (exerciseType) {
            ExerciseType.STRENGTH -> (sets.trim().toIntOrNull() ?: 0) * kcalPerUnit
            // Reps represent minutes for cardio
            ExerciseType.CARDIO -> (reps.trim().toIntOrNull() ?: 0) * kcalPerUnit
            ExerciseType.BODYWEIGHT -> (reps.trim().toIntOrNull() ?: 0) * kcalPerUnit
        }
    }

    val isValid = dateTimeValid && isExerciseInputValid(exerciseType, weight, reps, sets)

    FormDialog(
        title = stringResource(R.string.check_in_exercise_title, exercise.name),
        onDismissRequest = onDismiss,
        actions = {
            FormDialogActions(
                confirmText = stringResource(if (isEditMode) R.string.update else R.string.check_in),
                onConfirm = {
                    val exerciseLog = if (editing != null) {
                        // Update existing log with new values including timestamp
                        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                        val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                        editing.copy(
                            logDate = dateFormatter.format(selectedDateTime),
                            logDateTime = dateTimeFormatter.format(selectedDateTime),
                            weight = parseWeight(weight),
                            reps = reps.trim().toIntOrNull() ?: 0,
                            sets = sets.trim().toIntOrNull() ?: 0,
                            caloriesBurned = caloriesBurned,
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    } else {
                        // Create new log with custom timestamp
                        ExerciseLog.create(
                            exerciseId = exercise.id,
                            weight = parseWeight(weight),
                            reps = reps.trim().toIntOrNull() ?: 0,
                            sets = sets.trim().toIntOrNull() ?: 0,
                            caloriesBurned = caloriesBurned,
                            notes = notes.takeIf { it.isNotBlank() },
                            customDateTime = selectedDateTime
                        )
                    }
                    onCheckIn(exerciseLog)
                },
                onDismiss = onDismiss,
                confirmEnabled = isValid,
                onDelete = if (isEditMode) onDelete else null
            )
        }
    ) {
        ExerciseCheckInBody(
            exerciseType = exerciseType,
            maxWeight = maxWeight,
            selectedDateTime = selectedDateTime,
            onDateTimeChanged = { selectedDateTime = it },
            onDateTimeValidityChanged = { dateTimeValid = it },
            weight = weight,
            onWeightChange = { weight = it },
            reps = reps,
            onRepsChange = { reps = it },
            sets = sets,
            onSetsChange = { sets = it },
            caloriesBurned = caloriesBurned.toInt(),
            notes = notes,
            onNotesChange = { notes = it }
        )
    }
}

@Composable
private fun ExerciseCheckInBody(
    exerciseType: ExerciseType,
    maxWeight: Double?,
    selectedDateTime: Date,
    onDateTimeChanged: (Date) -> Unit,
    onDateTimeValidityChanged: (Boolean) -> Unit,
    weight: String,
    onWeightChange: (String) -> Unit,
    reps: String,
    onRepsChange: (String) -> Unit,
    sets: String,
    onSetsChange: (String) -> Unit,
    caloriesBurned: Int,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    val colors = FormDefaults.colors()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Show max weight if available
        maxWeight?.let { max ->
            Text(
                text = stringResource(R.string.max_weight_recorded, max),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.accent,
                fontWeight = FontWeight.Bold
            )
        }

        FormSection(colors = colors) {
            DateTimePicker(
                selectedDateTime = selectedDateTime,
                onDateTimeChanged = onDateTimeChanged,
                onValidityChanged = onDateTimeValidityChanged
            )
        }

        FormSection(colors = colors) {
            val fields = requiredExerciseFields(exerciseType)
            if (ExerciseField.WEIGHT in fields) {
                NumericInputRow(
                    label = stringResource(R.string.exercise_weight_label),
                    value = weight,
                    onValueChange = onWeightChange,
                    unit = stringResource(R.string.kg_unit),
                    leading = { FormRowIcon(Icons.Filled.FitnessCenter, colors.accent) },
                    errorText = validateExerciseField(ExerciseField.WEIGHT, weight)
                        ?.let { stringResource(it.messageRes) },
                    colors = colors
                )
                FormSectionDivider(colors = colors)
            }
            if (exerciseType == ExerciseType.CARDIO) {
                // For cardio only the minutes field is shown (stored in reps)
                NumericInputRow(
                    label = stringResource(R.string.minutes),
                    value = reps,
                    onValueChange = onRepsChange,
                    unit = stringResource(R.string.min_unit),
                    leading = { FormRowIcon(Icons.Filled.Timer, colors.accent) },
                    errorText = validateExerciseField(ExerciseField.REPS, reps)
                        ?.let { stringResource(it.messageRes) },
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    colors = colors
                )
            } else {
                NumericInputRow(
                    label = stringResource(R.string.reps),
                    value = reps,
                    onValueChange = onRepsChange,
                    unit = stringResource(R.string.reps_unit),
                    leading = { FormRowIcon(Icons.Filled.Repeat, colors.accent) },
                    errorText = validateExerciseField(ExerciseField.REPS, reps)
                        ?.let { stringResource(it.messageRes) },
                    keyboardType = KeyboardType.Number,
                    colors = colors
                )
                FormSectionDivider(colors = colors)
                NumericInputRow(
                    label = stringResource(R.string.sets),
                    value = sets,
                    onValueChange = onSetsChange,
                    unit = stringResource(R.string.sets_unit),
                    leading = { FormRowIcon(Icons.Filled.Layers, colors.accent) },
                    errorText = validateExerciseField(ExerciseField.SETS, sets)
                        ?.let { stringResource(it.messageRes) },
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    colors = colors
                )
            }
        }

        FormSection(colors = colors) {
            SummaryValueRow(
                label = stringResource(R.string.calories_burned),
                value = stringResource(R.string.calories_value, caloriesBurned),
                colors = colors
            )
        }

        FormTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = stringResource(R.string.notes_optional),
            singleLine = false,
            minLines = 2,
            maxLines = 3,
            imeAction = ImeAction.Default,
            colors = colors
        )
    }
}

// ----- Meal -----

@Composable
private fun MealCheckInContent(
    meal: Meal,
    existingMealCheckIn: MealCheckIn?,
    isEditMode: Boolean,
    onDismiss: () -> Unit,
    onCheckIn: (MealCheckIn) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val editing = if (isEditMode) existingMealCheckIn else null
    var servingSize by remember { mutableDoubleStateOf(editing?.servingSize ?: 1.0) }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    var selectedDateTime by remember { mutableStateOf(parseStoredDateTime(editing?.checkInDateTime)) }
    var dateTimeValid by remember { mutableStateOf(true) }

    val baseServing = meal.servingSize_value

    // Manual amount (in the meal's serving unit), kept in sync with the thumbwheel
    var manualInput by remember {
        mutableStateOf(formatDecimal(multiplierToAmount(servingSize, baseServing), maxFractionDigits = 2))
    }
    var isUserTyping by remember { mutableStateOf(false) }
    val amountError = validateServingAmount(manualInput)

    // Update manual input when the wheel changes (only if the user is not typing)
    LaunchedEffect(servingSize) {
        if (!isUserTyping) {
            manualInput = formatDecimal(multiplierToAmount(servingSize, baseServing), maxFractionDigits = 2)
        }
    }

    // After the user stops typing, reformat a valid value with the locale's decimal separator
    LaunchedEffect(manualInput) {
        if (isUserTyping) {
            kotlinx.coroutines.delay(1000) // Wait 1 second after user stops typing
            isUserTyping = false
            if (amountToMultiplier(manualInput, baseServing) != null) {
                manualInput = formatDecimal(multiplierToAmount(servingSize, baseServing), maxFractionDigits = 2)
            }
        }
    }

    // Calculate total calories based on serving size
    val totalCalories = remember(servingSize, meal) { (meal.calories * servingSize).toInt() }
    val isValid = dateTimeValid && amountError == null

    FormDialog(
        title = stringResource(R.string.check_in_meal_title, meal.name),
        onDismissRequest = onDismiss,
        actions = {
            FormDialogActions(
                confirmText = stringResource(if (isEditMode) R.string.update else R.string.check_in),
                onConfirm = {
                    val checkIn = if (editing != null) {
                        // Update existing check-in with new values including timestamp
                        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                        val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                        editing.copy(
                            checkInDate = dateFormatter.format(selectedDateTime),
                            checkInDateTime = dateTimeFormatter.format(selectedDateTime),
                            servingSize = servingSize,
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    } else {
                        // Create new check-in with custom timestamp
                        MealCheckIn.create(
                            mealId = meal.id,
                            servingSize = servingSize,
                            notes = notes.takeIf { it.isNotBlank() },
                            customDateTime = selectedDateTime
                        )
                    }
                    onCheckIn(checkIn)
                },
                onDismiss = onDismiss,
                confirmEnabled = isValid,
                onDelete = if (isEditMode) onDelete else null
            )
        }
    ) {
        MealCheckInBody(
            meal = meal,
            selectedDateTime = selectedDateTime,
            onDateTimeChanged = { selectedDateTime = it },
            onDateTimeValidityChanged = { dateTimeValid = it },
            servingSize = servingSize,
            onServingSizeChange = { servingSize = it },
            manualInput = manualInput,
            onManualInputChange = { newValue ->
                isUserTyping = true
                manualInput = newValue
                // Convert manual input to serving size multiplier; invalid input is ignored
                amountToMultiplier(newValue, baseServing)?.let { servingSize = it }
            },
            amountError = amountError?.let { stringResource(it.messageRes) },
            totalCalories = totalCalories,
            notes = notes,
            onNotesChange = { notes = it }
        )
    }
}

@Composable
private fun MealCheckInBody(
    meal: Meal,
    selectedDateTime: Date,
    onDateTimeChanged: (Date) -> Unit,
    onDateTimeValidityChanged: (Boolean) -> Unit,
    servingSize: Double,
    onServingSizeChange: (Double) -> Unit,
    manualInput: String,
    onManualInputChange: (String) -> Unit,
    amountError: String?,
    totalCalories: Int,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    val colors = FormDefaults.colors()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Show meal info
        Text(
            text = stringResource(
                R.string.meal_calories_info,
                meal.calories,
                meal.servingSize_value.toInt(),
                stringResource(meal.servingSize_unit.abbreviationResId)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.contentSecondary
        )

        FormSection(colors = colors) {
            DateTimePicker(
                selectedDateTime = selectedDateTime,
                onDateTimeChanged = onDateTimeChanged,
                onValidityChanged = onDateTimeValidityChanged
            )
        }

        FormSection(header = stringResource(R.string.serving_size), colors = colors) {
            // Slide instruction text
            Text(
                text = stringResource(R.string.slide_to_change),
                style = MaterialTheme.typography.bodySmall,
                color = colors.contentSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Horizontal thumbwheel
            HorizontalThumbwheel(
                value = servingSize,
                onValueChange = onServingSizeChange,
                colors = colors
            )

            // Current value display
            Text(
                text = stringResource(R.string.serving_multiplier_format, servingSize),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.content,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            FormSectionDivider(colors = colors)

            // Manual serving size input
            NumericInputRow(
                label = stringResource(R.string.amount),
                value = manualInput,
                onValueChange = onManualInputChange,
                unit = stringResource(meal.servingSize_unit.abbreviationResId),
                errorText = amountError,
                imeAction = ImeAction.Next,
                colors = colors
            )
        }

        FormSection(colors = colors) {
            SummaryValueRow(
                label = stringResource(R.string.total_calories),
                value = stringResource(R.string.calories_value, totalCalories),
                colors = colors
            )
        }

        FormTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = stringResource(R.string.notes_optional),
            singleLine = false,
            minLines = 2,
            maxLines = 3,
            imeAction = ImeAction.Default,
            colors = colors
        )
    }
}

/** Read-only headline row of a section: `[flame] Label ........ value`, readable in both themes. */
@Composable
private fun SummaryValueRow(label: String, value: String, colors: FormColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(FormDefaults.LeadingSize), contentAlignment = Alignment.Center) {
            FormRowIcon(Icons.Filled.LocalFireDepartment, brandAccentShade(2))
        }
        Spacer(Modifier.width(if (FormDefaults.isCompactWidth) 8.dp else 10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.content,
            maxLines = 1
        )
    }
}

// ----- Previews -----

private val previewMeal = Meal(
    id = 1,
    name = "Oatmeal with banana",
    calories = 320,
    carbohydrates_g = 54.0,
    protein_g = 10.0,
    fat_g = 6.0,
    fiber_g = 4.0,
    sodium_mg = 120.0,
    servingSize_value = 100.0,
    servingSize_unit = ServingSizeUnit.GRAMS
)

@Composable
private fun MealPreview(manualInput: String, error: String?) {
    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        FormDialogSurface(
            title = stringResource(R.string.check_in_meal_title, previewMeal.name),
            actions = {
                FormDialogActions(
                    confirmText = stringResource(R.string.check_in),
                    onConfirm = {},
                    onDismiss = {},
                    confirmEnabled = error == null
                )
            }
        ) {
            MealCheckInBody(
                meal = previewMeal,
                selectedDateTime = Date(),
                onDateTimeChanged = {},
                onDateTimeValidityChanged = {},
                servingSize = 1.5,
                onServingSizeChange = {},
                manualInput = manualInput,
                onManualInputChange = {},
                amountError = error,
                totalCalories = 480,
                notes = "",
                onNotesChange = {}
            )
        }
    }
}

@Composable
private fun ExercisePreview(type: ExerciseType, weight: String, error: Boolean) {
    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        FormDialogSurface(
            title = stringResource(R.string.check_in_exercise_title, "Bench press"),
            actions = {
                FormDialogActions(
                    confirmText = stringResource(R.string.check_in),
                    onConfirm = {},
                    onDismiss = {},
                    confirmEnabled = !error,
                    onDelete = {}
                )
            }
        ) {
            ExerciseCheckInBody(
                exerciseType = type,
                maxWeight = 82.5,
                selectedDateTime = Date(),
                onDateTimeChanged = {},
                onDateTimeValidityChanged = {},
                weight = weight,
                onWeightChange = {},
                reps = "10",
                onRepsChange = {},
                sets = "3",
                onSetsChange = {},
                caloriesBurned = 150,
                notes = "",
                onNotesChange = {}
            )
        }
    }
}

@Preview(name = "Meal check-in - light", widthDp = 360, heightDp = 820, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun MealCheckInLightPreview() {
    FoodLogTheme(darkTheme = false) { MealPreview("150", null) }
}

@Preview(name = "Meal check-in - dark, error", widthDp = 360, heightDp = 820, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun MealCheckInDarkErrorPreview() {
    FoodLogTheme(darkTheme = true) { MealPreview("0", stringResource(R.string.form_error_must_be_positive)) }
}

@Preview(name = "Exercise check-in - light", widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun ExerciseCheckInLightPreview() {
    FoodLogTheme(darkTheme = false) { ExercisePreview(ExerciseType.STRENGTH, "12,5", false) }
}

@Preview(name = "Exercise check-in - dark, error", widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun ExerciseCheckInDarkPreview() {
    FoodLogTheme(darkTheme = true) { ExercisePreview(ExerciseType.STRENGTH, "abc", true) }
}
