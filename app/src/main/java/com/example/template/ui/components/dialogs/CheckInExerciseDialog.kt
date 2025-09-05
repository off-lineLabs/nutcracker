package com.example.template.ui.components.dialogs

import androidx.compose.runtime.*
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExerciseLog
import com.example.template.data.model.CheckInData

@Composable
fun CheckInExerciseDialog(
    exercise: Exercise,
    lastLog: ExerciseLog?,
    maxWeight: Double?,
    onDismiss: () -> Unit,
    onCheckIn: (ExerciseLog) -> Unit
) {
    UnifiedCheckInDialog<CheckInData.Exercise>(
        onDismiss = onDismiss,
        onCheckIn = { checkInData -> onCheckIn(checkInData.exerciseLog) },
        exercise = exercise,
        lastLog = lastLog,
        maxWeight = maxWeight
    )
}
