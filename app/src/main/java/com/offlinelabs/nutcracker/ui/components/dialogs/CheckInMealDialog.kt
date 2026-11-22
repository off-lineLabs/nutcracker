package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.runtime.*
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.CheckInData
import java.util.Date

@Composable
fun CheckInMealDialog(
    meal: Meal,
    onDismiss: () -> Unit,
    onCheckIn: (MealCheckIn) -> Unit,
    /** Initial date/time of the check-in; null means now. */
    defaultDateTime: Date? = null
) {
    UnifiedCheckInDialog<CheckInData.Meal>(
        onDismiss = onDismiss,
        onCheckIn = { checkInData -> onCheckIn(checkInData.mealCheckIn) },
        defaultDateTime = defaultDateTime,
        meal = meal
    )
}
