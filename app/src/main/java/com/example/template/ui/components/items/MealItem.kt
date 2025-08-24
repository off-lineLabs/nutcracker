package com.example.template.ui.components.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.template.data.model.Meal // Updated import

@Composable
fun MealItem(meal: Meal, onCheckInClick: (Meal) -> Unit) { // Renamed Piece to Meal
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckInClick(meal) }
                .padding(vertical = 12.dp, horizontal = 8.dp), // Added some horizontal padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFE5E7EB) // textGray200 for dark theme consistency
            )
            Text(
                text = "${meal.calories} kcal", // Display calories
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9CA3AF) // text-gray-400 for calorie value
            )
        }
        HorizontalDivider(color = Color(0xFF374151)) // dark:border-gray-700
    }
}
