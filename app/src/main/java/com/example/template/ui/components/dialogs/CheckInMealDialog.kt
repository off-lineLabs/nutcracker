package com.example.template.ui.components.dialogs

import androidx.compose.runtime.*
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn
import com.example.template.data.model.CheckInData

@Composable
fun CheckInMealDialog(
    meal: Meal,
    onDismiss: () -> Unit,
    onCheckIn: (MealCheckIn) -> Unit
) {
    UnifiedCheckInDialog<CheckInData.Meal>(
        onDismiss = onDismiss,
        onCheckIn = { checkInData -> onCheckIn(checkInData.mealCheckIn) },
        meal = meal
    )
}
