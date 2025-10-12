package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.runtime.*
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.CheckInData

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
