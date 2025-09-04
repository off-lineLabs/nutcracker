package com.example.template.ui.components.dialogs

import androidx.compose.runtime.*
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExerciseLog

@Composable
fun CheckInExerciseDialog(
    exercise: Exercise,
    lastLog: ExerciseLog?,
    maxWeight: Double?,
    onDismiss: () -> Unit,
    onCheckIn: (ExerciseLog) -> Unit
) {
    UnifiedCheckInDialog(
        onDismiss = onDismiss,
        onCheckIn = { checkIn -> onCheckIn(checkIn as ExerciseLog) },
        exercise = exercise,
        lastLog = lastLog,
        maxWeight = maxWeight
    )
}
