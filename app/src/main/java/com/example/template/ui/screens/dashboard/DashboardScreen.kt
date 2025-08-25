package com.example.template.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.template.FoodLogApplication
import com.example.template.R
import com.example.template.data.dao.DailyNutritionEntry
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn
import com.example.template.data.model.UserGoal // Added import
import com.example.template.ui.components.dialogs.AddMealDialog
import com.example.template.ui.components.dialogs.CheckInMealDialog
import com.example.template.ui.components.dialogs.SetGoalDialog
import com.example.template.ui.components.items.CheckInItem
import com.example.template.ui.components.items.MealItem
import kotlinx.coroutines.flow.collectLatest // Added import
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val foodLogRepository = (context.applicationContext as FoodLogApplication).foodLogRepository
    val coroutineScope = rememberCoroutineScope()

    var meals by remember { mutableStateOf(emptyList<Meal>()) }
    var userGoal by remember { mutableStateOf(UserGoal.default()) } // Use UserGoal state
    var dailyCheckIns by remember { mutableStateOf(emptyList<DailyNutritionEntry>()) } // Real check-ins from database
    var consumedCalories by remember { mutableStateOf(0) } // Daily calorie consumption

    var showSetGoalDialog by remember { mutableStateOf(false) }
    var showAddMealDialog by remember { mutableStateOf(false) }
    var showCheckInMealDialog by remember { mutableStateOf<Meal?>(null) }

    LaunchedEffect(key1 = foodLogRepository) {
        foodLogRepository.getAllMeals().collectLatest { mealList ->
            meals = mealList
        }
    }

    LaunchedEffect(key1 = foodLogRepository) {
        foodLogRepository.getUserGoal().collectLatest { goalFromDb ->
            userGoal = goalFromDb ?: UserGoal.default() // Update state, use default if null
            // If no goal is in DB, insert the default one
            if (goalFromDb == null) {
                coroutineScope.launch {
                    foodLogRepository.upsertUserGoal(UserGoal.default())
                }
            }
        }
    }

    // Get today's date in YYYY-MM-DD format
    val today = remember {
        val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        dateFormatter.format(java.util.Date())
    }

    // Load daily check-ins and calorie consumption
    LaunchedEffect(key1 = foodLogRepository, key2 = today) {
        foodLogRepository.getDailyNutritionSummary(today).collectLatest { checkInsList ->
            dailyCheckIns = checkInsList
        }
    }

    LaunchedEffect(key1 = foodLogRepository, key2 = today) {
        foodLogRepository.getDailyCalories(today).collectLatest { calories ->
            consumedCalories = calories
        }
    }

    val lightGray50 = Color(0xFFF9FAFB)
    val lightGray100 = Color(0xFFF3F4F6)
    val darkGray800 = Color(0xFF1F2937)
    val darkGray900 = Color(0xFF111827)
    val textGray200 = Color(0xFFE5E7EB)

    val gradientStartColor = if (isSystemInDarkTheme()) darkGray900 else lightGray50
    val gradientEndColor = if (isSystemInDarkTheme()) darkGray800 else lightGray100
    val innerContainerBackgroundColor = darkGray800

    // Calculate remaining calories based on actual data
    val remainingCalories = userGoal.caloriesGoal - consumedCalories

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.progress_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC0C0C0),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddMealDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_new_meal))
            }
        },
        containerColor = Color.Transparent
    ) { scaffoldPaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientStartColor, gradientEndColor)
                    )
                )
                .padding(scaffoldPaddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = innerContainerBackgroundColor.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val caloriesRemainingLabelColor = if (isSystemInDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                    val caloriesRemainingValueColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF111827)
                    val caloriesConsumedColor = if (isSystemInDarkTheme()) textGray200 else Color(0xFF1F2937)
                    val caloriesGoalColor = if (isSystemInDarkTheme()) Color(0xFF6B7280) else Color(0xFF9CA3AF)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(id = R.string.calories_remaining_label),
                            color = caloriesRemainingLabelColor,
                            fontSize = 12.sp
                        )
                        Text(
                            text = remainingCalories.toString(),
                            color = caloriesRemainingValueColor,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = caloriesConsumedColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) {
                                    append("$consumedCalories")
                                }
                                withStyle(style = SpanStyle(color = caloriesGoalColor, fontSize = 12.sp, fontWeight = FontWeight.Normal)) {
                                    append(" / ")
                                    append("${userGoal.caloriesGoal}")
                                    append(" kcal")
                                }
                            }
                        )
                    }

                    Button(onClick = { showSetGoalDialog = true }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.set_goal))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.your_meals),
                        style = MaterialTheme.typography.titleMedium,
                        color = textGray200
                    )
                    if (meals.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_meals_added),
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Normal,
                            color = textGray200
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(meals, key = { it.id }) { meal ->
                                MealItem(meal = meal, onCheckInClick = { showCheckInMealDialog = meal })
                            }
                        }
                    }
                    Button(
                        onClick = { if (meals.isNotEmpty()) showCheckInMealDialog = meals.first() },
                        enabled = meals.isNotEmpty(),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(stringResource(R.string.check_in_meal))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.recent_check_ins),
                        style = MaterialTheme.typography.titleMedium,
                        color = textGray200
                    )
                    if (dailyCheckIns.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_check_ins_yet),
                            modifier = Modifier.padding(8.dp),
                            fontStyle = FontStyle.Normal,
                            color = textGray200
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(dailyCheckIns.take(5), key = { it.checkInId }) { checkIn ->
                                CheckInItem(
                                    checkIn = checkIn,
                                    onDelete = {
                                        // TODO: Implement delete functionality
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSetGoalDialog) {
        SetGoalDialog(
            currentUserGoal = userGoal, // Pass the full UserGoal object
            onDismiss = { showSetGoalDialog = false },
            onSetGoal = { updatedGoal -> // Callback gives the full UserGoal object
                coroutineScope.launch {
                    foodLogRepository.upsertUserGoal(updatedGoal)
                }
                showSetGoalDialog = false
            }
        )
    }

    if (showAddMealDialog) {
        AddMealDialog(
            onDismiss = { showAddMealDialog = false },
            onAddMeal = { newMeal ->
                coroutineScope.launch {
                    foodLogRepository.insertMeal(newMeal)
                }
                showAddMealDialog = false
            }
        )
    }

    showCheckInMealDialog?.let { mealToCheckIn ->
        CheckInMealDialog(
            meal = mealToCheckIn,
            onDismiss = { showCheckInMealDialog = null },
            onCheckIn = { mealCheckIn ->
                coroutineScope.launch {
                    foodLogRepository.insertMealCheckIn(mealCheckIn)
                }
                showCheckInMealDialog = null
            }
        )
    }
}

