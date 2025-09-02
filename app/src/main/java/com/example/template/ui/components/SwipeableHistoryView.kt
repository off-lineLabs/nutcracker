package com.example.template.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.template.R
import com.example.template.data.dao.DailyNutritionEntry
import com.example.template.data.dao.DailyExerciseEntry
import com.example.template.ui.components.items.CheckInItem
import com.example.template.ui.components.items.ExerciseItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableHistoryView(
    mealEntries: List<DailyNutritionEntry>,
    exerciseEntries: List<DailyExerciseEntry>,
    onDeleteMeal: (DailyNutritionEntry) -> Unit,
    onDeleteExercise: (DailyExerciseEntry) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Tab indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { /* Pager will handle this */ },
                    text = {
                        Text(
                            text = stringResource(R.string.meals),
                            fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { /* Pager will handle this */ },
                    text = {
                        Text(
                            text = stringResource(R.string.exercises),
                            fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> {
                    // Meals tab
                    if (mealEntries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_meals_today),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column {
                            mealEntries.take(5).forEach { entry ->
                                CheckInItem(
                                    checkIn = entry,
                                    onDelete = { onDeleteMeal(entry) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Exercises tab
                    if (exerciseEntries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_exercises_today),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column {
                            exerciseEntries.take(5).forEach { entry ->
                                ExerciseItem(
                                    exerciseEntry = entry,
                                    onDelete = { onDeleteExercise(entry) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
