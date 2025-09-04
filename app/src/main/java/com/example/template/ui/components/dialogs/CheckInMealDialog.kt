package com.example.template.ui.components.dialogs

import androidx.compose.runtime.*
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn

@Composable
fun CheckInMealDialog(
    meal: Meal,
    onDismiss: () -> Unit,
    onCheckIn: (MealCheckIn) -> Unit
) {
    UnifiedCheckInDialog(
        onDismiss = onDismiss,
        onCheckIn = { checkIn -> onCheckIn(checkIn as MealCheckIn) },
        meal = meal
    )
}
