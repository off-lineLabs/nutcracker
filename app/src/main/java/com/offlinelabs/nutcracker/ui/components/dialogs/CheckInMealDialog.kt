package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.runtime.*
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.CheckInData

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
