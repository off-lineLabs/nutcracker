package com.example.template.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.Dp // Added import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.template.FoodLogApplication
import com.example.template.R
import com.example.template.data.dao.DailyNutritionEntry
import com.example.template.data.dao.DailyTotals // Added import
import com.example.template.data.model.Meal
import com.example.template.data.model.UserGoal
import com.example.template.ui.components.dialogs.AddMealDialog
import com.example.template.ui.components.dialogs.CheckInMealDialog
import com.example.template.ui.components.dialogs.SelectMealForCheckInDialog // Added import
import com.example.template.ui.components.dialogs.SetGoalDialog
import com.example.template.ui.components.items.CheckInItem
import com.example.template.ui.components.items.MealItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun NutrientProgressDisplay(
    nutrientName: String,
    consumed: Double,
    goal: Double,
    unit: String,
    labelColor: Color,
    valueColor: Color,
    goalColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = nutrientName.uppercase(Locale.getDefault()),
            color = labelColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) {
                    append(String.format(Locale.getDefault(), "%.1f", consumed))
                }
                withStyle(style = SpanStyle(color = goalColor, fontSize = 12.sp, fontWeight = FontWeight.Normal)) {
                    append(" / ")
                    append(String.format(Locale.getDefault(), "%.1f", goal))
                    append(" $unit")
                }
            },
            textAlign = TextAlign.End
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun CaloriesRing(
    consumedCalories: Double,
    goalCalories: Double,
    labelColor: Color,
    valueColor: Color,
    consumedColor: Color,
    goalColor: Color,
    ringTrackColor: Color = Color(0xFF374151),
    ringProgressColor: Color = Color(0xFFFFA94D),
    sizeDp: Dp = 160.dp,
    strokeWidthDp: Dp = 12.dp
) {
    val remaining = (goalCalories - consumedCalories).coerceAtLeast(0.0)
    val progress = if (goalCalories > 0) (consumedCalories / goalCalories).toFloat().coerceIn(0f, 1f) else 0f

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val stroke = Stroke(width = strokeWidthDp.toPx(), cap = StrokeCap.Round)
            val diameter = size.minDimension
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            // Track (full circle)
            drawArc(
                color = ringTrackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            // Progress
            drawArc(
                color = ringProgressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.calories_remaining_label),
                color = labelColor,
                fontSize = 12.sp
            )
            Text(
                text = remaining.toInt().toString(),
                color = valueColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = consumedColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) {
                        append(String.format(Locale.getDefault(), "%.0f", consumedCalories))
                    }
                    withStyle(style = SpanStyle(color = goalColor, fontSize = 12.sp, fontWeight = FontWeight.Normal)) {
                        append(" / ")
                        append("${goalCalories.toInt()}")
                        append(" kcal")
                    }
                }
            )
        }
    }
}

@Composable
private fun NutrientBarRow(
    title: String,
    consumed: Double,
    goal: Double,
    barStartColor: Color,
    barEndColor: Color,
    trackColor: Color,
    unit: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = Color(0xFFE5E7EB))
            val goalText = String.format(Locale.getDefault(), "%.0f", goal)
            val consumedText = String.format(Locale.getDefault(), "%.0f", consumed)
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFFE5E7EB), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) {
                        append(consumedText)
                    }
                    withStyle(style = SpanStyle(color = Color(0xFF9CA3AF), fontSize = 12.sp)) {
                        append(" / ${goalText}${unit}")
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(trackColor)
        ) {
            val pct = if (goal > 0) (consumed / goal).toFloat().coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(listOf(barStartColor, barEndColor))
                    )
            )
        }
    }
}

@Composable
private fun NutrientBox(
    totals: DailyTotals?,
    goals: UserGoal,
    isDark: Boolean
) {
    val track = if (isDark) Color(0xFF4B5563) else Color(0xFFE5E7EB)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF374151).copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        NutrientBarRow(
            title = stringResource(id = R.string.carbohydrates_label),
            consumed = totals?.totalCarbohydrates ?: 0.0,
            goal = goals.carbsGoal_g.toDouble(),
            barStartColor = Color(0xFF60A5FA),
            barEndColor = Color(0xFF2563EB),
            trackColor = track,
            unit = "g"
        )
        Spacer(modifier = Modifier.height(12.dp))
        NutrientBarRow(
            title = stringResource(id = R.string.protein_label),
            consumed = totals?.totalProtein ?: 0.0,
            goal = goals.proteinGoal_g.toDouble(),
            barStartColor = Color(0xFFF87171),
            barEndColor = Color(0xFFDC2626),
            trackColor = track,
            unit = "g"
        )
        Spacer(modifier = Modifier.height(12.dp))
        NutrientBarRow(
            title = stringResource(id = R.string.fat_label),
            consumed = totals?.totalFat ?: 0.0,
            goal = goals.fatGoal_g.toDouble(),
            barStartColor = Color(0xFFFBBF24),
            barEndColor = Color(0xFFD97706),
            trackColor = track,
            unit = "g"
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = track)
        // Secondary nutrients
        NutrientProgressDisplay(
            nutrientName = stringResource(id = R.string.fiber_label),
            consumed = totals?.totalFiber ?: 0.0,
            goal = goals.fiberGoal_g.toDouble(),
            unit = "g",
            labelColor = Color(0xFF9CA3AF),
            valueColor = Color(0xFFE5E7EB),
            goalColor = Color(0xFF9CA3AF)
        )
        NutrientProgressDisplay(
            nutrientName = stringResource(id = R.string.sodium_label),
            consumed = totals?.totalSodium ?: 0.0,
            goal = goals.sodiumGoal_mg.toDouble(),
            unit = "mg",
            labelColor = Color(0xFF9CA3AF),
            valueColor = Color(0xFFE5E7EB),
            goalColor = Color(0xFF9CA3AF)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val foodLogRepository = (context.applicationContext as FoodLogApplication).foodLogRepository
    val coroutineScope = rememberCoroutineScope()

    var meals by remember { mutableStateOf(emptyList<Meal>()) }
    var userGoal by remember { mutableStateOf(UserGoal.default()) }
    var dailyCheckIns by remember { mutableStateOf(emptyList<DailyNutritionEntry>()) }
    var dailyTotalsConsumed by remember { mutableStateOf<DailyTotals?>(null) } // New state for all totals
    var consumedCalories by remember { mutableStateOf(0.0) } // Changed to Double

    var showSetGoalDialog by remember { mutableStateOf(false) }
    var showAddMealDialog by remember { mutableStateOf(false) }
    var showSelectMealDialog by remember { mutableStateOf(false) }
    var showCheckInMealDialog by remember { mutableStateOf<Meal?>(null) }

    LaunchedEffect(key1 = foodLogRepository) {
        foodLogRepository.getAllMeals().collectLatest { mealList ->
            meals = mealList
        }
    }

    LaunchedEffect(key1 = foodLogRepository) {
        foodLogRepository.getUserGoal().collectLatest { goalFromDb ->
            userGoal = goalFromDb ?: UserGoal.default()
            if (goalFromDb == null) {
                coroutineScope.launch {
                    foodLogRepository.upsertUserGoal(UserGoal.default())
                }
            }
        }
    }

    val today = remember {
        val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        dateFormatter.format(java.util.Date())
    }

    LaunchedEffect(key1 = foodLogRepository, key2 = today) {
        foodLogRepository.getDailyNutritionSummary(today).collectLatest { checkInsList ->
            dailyCheckIns = checkInsList
        }
    }

    // Load daily nutrient totals (replaces getDailyCalories)
    LaunchedEffect(key1 = foodLogRepository, key2 = today) {
        foodLogRepository.getDailyNutrientTotals(today).collectLatest { totals ->
            dailyTotalsConsumed = totals
            consumedCalories = totals?.totalCalories ?: 0.0
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
    val remainingCalories = (userGoal.caloriesGoal - consumedCalories).toInt()

    val caloriesRemainingLabelColor = if (isSystemInDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val caloriesRemainingValueColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF111827)
    val caloriesConsumedColor = if (isSystemInDarkTheme()) textGray200 else Color(0xFF1F2937)
    val caloriesGoalColor = if (isSystemInDarkTheme()) Color(0xFF6B7280) else Color(0xFF9CA3AF)

    // Nutrient specific colors (can be themed as well)
    val nutrientLabelColor = caloriesRemainingLabelColor
    val nutrientConsumedColor = caloriesConsumedColor
    val nutrientGoalColor = caloriesGoalColor


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.progress_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC0C0C0), // Consider theming this
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
            FloatingActionButton(onClick = { showSelectMealDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.check_in_meal))
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
                    // Calories ring section
                    CaloriesRing(
                        consumedCalories = consumedCalories,
                        goalCalories = userGoal.caloriesGoal.toDouble(),
                        labelColor = caloriesRemainingLabelColor,
                        valueColor = caloriesRemainingValueColor,
                        consumedColor = caloriesConsumedColor,
                        goalColor = caloriesGoalColor
                    )

                    Button(onClick = { showSetGoalDialog = true }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.set_goal))
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // Spacer before nutrient details

                    // Nutrient Details Section
                    Text(
                        text = stringResource(id = R.string.nutrient_details_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = textGray200,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    NutrientBox(
                        totals = dailyTotalsConsumed,
                        goals = userGoal,
                        isDark = isSystemInDarkTheme()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

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
                        LazyColumn( // Similar to above, manage height/weight if overall Column is scrollable
                            modifier = Modifier
                                .weight(1f) // This weight might be an issue if the parent Column isn't weighted correctly
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
            currentUserGoal = userGoal,
            onDismiss = { showSetGoalDialog = false },
            onSetGoal = { updatedGoal ->
                coroutineScope.launch {
                    foodLogRepository.upsertUserGoal(updatedGoal)
                }
                showSetGoalDialog = false
            }
        )
    }

    if (showSelectMealDialog) {
        SelectMealForCheckInDialog(
            meals = meals,
            onDismiss = { showSelectMealDialog = false },
            onAddMeal = {
                showSelectMealDialog = false
                showAddMealDialog = true
            },
            onSelectMeal = { meal ->
                showSelectMealDialog = false
                showCheckInMealDialog = meal
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
