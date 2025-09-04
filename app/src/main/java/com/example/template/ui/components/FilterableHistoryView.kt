package com.example.template.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.template.R
import com.example.template.data.dao.DailyNutritionEntry
import com.example.template.data.dao.DailyExerciseEntry
import com.example.template.ui.components.items.CheckInItem
import com.example.template.ui.components.items.ExerciseItem
import java.text.SimpleDateFormat
import java.util.*

// Sealed class to represent unified history items
sealed class HistoryItem {
    data class MealItem(val entry: DailyNutritionEntry) : HistoryItem()
    data class ExerciseItem(val entry: DailyExerciseEntry) : HistoryItem()
    
    val timestamp: String
        get() = when (this) {
            is MealItem -> entry.checkInDateTime
            is ExerciseItem -> entry.logDateTime
        }
}

enum class HistoryFilter {
    ALL, MEALS, EXERCISES
}

@Composable
fun FilterableHistoryView(
    mealEntries: List<DailyNutritionEntry>,
    exerciseEntries: List<DailyExerciseEntry>,
    onDeleteMeal: (DailyNutritionEntry) -> Unit,
    onDeleteExercise: (DailyExerciseEntry) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    
    // Convert to unified history items and sort by timestamp
    val allHistoryItems = remember(mealEntries, exerciseEntries) {
        val items = mutableListOf<HistoryItem>()
        items.addAll(mealEntries.map { HistoryItem.MealItem(it) })
        items.addAll(exerciseEntries.map { HistoryItem.ExerciseItem(it) })
        
        // Sort by timestamp (most recent first)
        items.sortedByDescending { item ->
            try {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                formatter.parse(item.timestamp)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }
    
    // Filter items based on selected filter
    val filteredItems = remember(allHistoryItems, selectedFilter) {
        when (selectedFilter) {
            HistoryFilter.ALL -> allHistoryItems
            HistoryFilter.MEALS -> allHistoryItems.filterIsInstance<HistoryItem.MealItem>()
            HistoryFilter.EXERCISES -> allHistoryItems.filterIsInstance<HistoryItem.ExerciseItem>()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Filter bubbles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All filter
            FilterBubble(
                text = "All",
                isSelected = selectedFilter == HistoryFilter.ALL,
                onClick = { selectedFilter = HistoryFilter.ALL },
                modifier = Modifier.weight(1f)
            )
            
            // Meals filter
            FilterBubble(
                text = "Meals",
                isSelected = selectedFilter == HistoryFilter.MEALS,
                onClick = { selectedFilter = HistoryFilter.MEALS },
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Restaurant
            )
            
            // Exercises filter
            FilterBubble(
                text = "Exercises",
                isSelected = selectedFilter == HistoryFilter.EXERCISES,
                onClick = { selectedFilter = HistoryFilter.EXERCISES },
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.FitnessCenter
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content based on filter
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (selectedFilter) {
                        HistoryFilter.ALL -> "No activities today"
                        HistoryFilter.MEALS -> stringResource(R.string.no_meals_today)
                        HistoryFilter.EXERCISES -> stringResource(R.string.no_exercises_today)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column {
                filteredItems.take(10).forEach { item ->
                    when (item) {
                        is HistoryItem.MealItem -> {
                            CheckInItem(
                                checkIn = item.entry,
                                onDelete = { onDeleteMeal(item.entry) }
                            )
                        }
                        is HistoryItem.ExerciseItem -> {
                            ExerciseItem(
                                exerciseEntry = item.entry,
                                onDelete = { onDeleteExercise(item.entry) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBubble(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}
