package com.offlinelabs.nutcracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.offlinelabs.nutcracker.R
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.data.dao.DailyNutritionEntry
import com.offlinelabs.nutcracker.data.dao.DailyExerciseEntry
import com.offlinelabs.nutcracker.ui.components.items.CheckInItem
import com.offlinelabs.nutcracker.ui.components.items.ExerciseItem
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
    onDeleteExercise: (DailyExerciseEntry) -> Unit,
    onEditMeal: (DailyNutritionEntry) -> Unit,
    onEditExercise: (DailyExerciseEntry) -> Unit
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
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
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
        // Filter bubbles - centered layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exercises filter (left) - icon only
            FilterBubble(
                text = null,
                isSelected = selectedFilter == HistoryFilter.EXERCISES,
                onClick = { selectedFilter = HistoryFilter.EXERCISES },
                modifier = Modifier.size(48.dp),
                painter = painterResource(R.drawable.ic_sprint)
            )
            
            // All filter (center)
            FilterBubble(
                text = stringResource(R.string.all),
                isSelected = selectedFilter == HistoryFilter.ALL,
                onClick = { selectedFilter = HistoryFilter.ALL },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Meals filter (right) - icon only
            FilterBubble(
                text = null,
                isSelected = selectedFilter == HistoryFilter.MEALS,
                onClick = { selectedFilter = HistoryFilter.MEALS },
                modifier = Modifier.size(48.dp),
                icon = Icons.Filled.Restaurant
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
                        HistoryFilter.ALL -> stringResource(R.string.no_activities_today)
                        HistoryFilter.MEALS -> stringResource(R.string.no_meals_today)
                        HistoryFilter.EXERCISES -> stringResource(R.string.no_exercises_today)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column {
                filteredItems.take(30).forEach { item ->
                    when (item) {
                        is HistoryItem.MealItem -> {
                            CheckInItem(
                                checkIn = item.entry,
                                onEdit = { onEditMeal(item.entry) }
                            )
                        }
                        is HistoryItem.ExerciseItem -> {
                            ExerciseItem(
                                exerciseEntry = item.entry,
                                onEdit = { onEditExercise(item.entry) }
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
    text: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    painter: Painter? = null
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
            .then(
                if (text != null) {
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                } else {
                    Modifier // No padding for icon-only bubbles
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            // Text + optional icon layout
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
                painter?.let {
                    Icon(
                        painter = it,
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
        } else {
            // Icon-only layout
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            painter?.let {
                Icon(
                    painter = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
