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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.example.template.ui.components.PillTracker
import com.example.template.ui.components.ExerciseToggle
import com.example.template.ui.components.TEFToggle
import com.example.template.data.model.Pill
import com.example.template.data.model.PillCheckIn
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import com.example.template.FoodLogApplication
import com.example.template.R
import com.example.template.data.dao.DailyNutritionEntry
import com.example.template.data.dao.DailyExerciseEntry
import com.example.template.data.dao.DailyTotals
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExerciseLog
import com.example.template.data.model.UserGoal
import com.example.template.ui.components.dialogs.AddMealDialog
import com.example.template.ui.components.dialogs.CheckInMealDialog
import com.example.template.ui.components.dialogs.SelectMealForCheckInDialog
import com.example.template.ui.components.dialogs.AddExerciseDialog
import com.example.template.ui.components.dialogs.CheckInExerciseDialog
import com.example.template.ui.components.dialogs.SelectExerciseForCheckInDialog
import com.example.template.ui.components.dialogs.SetGoalDialog
import com.example.template.ui.components.dialogs.UnifiedCheckInDialog
import com.example.template.data.model.CheckInData
import com.example.template.ui.components.FilterableHistoryView
import com.example.template.ui.theme.*
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
    val exceededColor = Color(0xFFB65755)
    val proteinFiberColor = Color(0xFF5D916D)
    val fiberLabel = stringResource(R.string.fiber_label)
    val isFiber = nutrientName == fiberLabel
    val finalValueColor = when {
        consumed > goal && isFiber -> proteinFiberColor
        consumed > goal -> exceededColor
        else -> valueColor
    }
    
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
                withStyle(style = SpanStyle(color = finalValueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) {
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
    val remaining = goalCalories - consumedCalories
    val progress = if (goalCalories > 0) (consumedCalories / goalCalories).toFloat().coerceIn(0f, 1f) else 0f
    val exceededColor = Color(0xFFB65755)
    val finalValueColor = if (remaining < 0) exceededColor else valueColor

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val stroke = Stroke(width = strokeWidthDp.toPx(), cap = StrokeCap.Round)
            val strokeWidthPx = strokeWidthDp.toPx()
            val diameter = size.minDimension - strokeWidthPx
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
                color = finalValueColor,
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
    val exceededColor = Color(0xFFB65755)
    val proteinFiberColor = Color(0xFF5D916D)
    val proteinLabel = stringResource(R.string.protein_label)
    val fiberLabel = stringResource(R.string.fiber_label)
    val isProteinOrFiber = title == proteinLabel || title == fiberLabel
    val finalConsumedColor = when {
        consumed > goal && isProteinOrFiber -> proteinFiberColor
        consumed > goal -> exceededColor
        else -> appTextPrimaryColor()
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = appTextPrimaryColor())
            val goalText = String.format(Locale.getDefault(), "%.0f", goal)
            val consumedText = String.format(Locale.getDefault(), "%.0f", consumed)
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = finalConsumedColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) {
                        append(consumedText)
                    }
                    withStyle(style = SpanStyle(color = appTextSecondaryColor(), fontSize = 12.sp)) {
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
    // Use themed colors instead of hardcoded values
    val track = progressTrackColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = appContainerBackgroundColor(),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        NutrientBarRow(
            title = stringResource(id = R.string.carbohydrates_label),
            consumed = totals?.totalCarbohydrates ?: 0.0,
            goal = goals.carbsGoal_g.toDouble(),
            barStartColor = nutrientCarbsColor(),
            barEndColor = nutrientCarbsColor(),
            trackColor = track,
            unit = "g"
        )
        Spacer(modifier = Modifier.height(12.dp))
        NutrientBarRow(
            title = stringResource(id = R.string.protein_label),
            consumed = totals?.totalProtein ?: 0.0,
            goal = goals.proteinGoal_g.toDouble(),
            barStartColor = nutrientProteinColor(),
            barEndColor = nutrientProteinColor(),
            trackColor = track,
            unit = "g"
        )
        Spacer(modifier = Modifier.height(12.dp))
        NutrientBarRow(
            title = stringResource(id = R.string.fat_label),
            consumed = totals?.totalFat ?: 0.0,
            goal = goals.fatGoal_g.toDouble(),
            barStartColor = nutrientFatColor(),
            barEndColor = nutrientFatColor(),
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
            labelColor = appTextSecondaryColor(),
            valueColor = appTextPrimaryColor(),
            goalColor = appTextSecondaryColor()
        )
        NutrientProgressDisplay(
            nutrientName = stringResource(id = R.string.sodium_label),
            consumed = totals?.totalSodium ?: 0.0,
            goal = goals.sodiumGoal_mg.toDouble(),
            unit = "mg",
            labelColor = appTextSecondaryColor(),
            valueColor = appTextPrimaryColor(),
            goalColor = appTextSecondaryColor()
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
    var exercises by remember { mutableStateOf(emptyList<Exercise>()) }
    var userGoal by remember { mutableStateOf(UserGoal.default()) }
    var dailyMealCheckIns by remember { mutableStateOf(emptyList<DailyNutritionEntry>()) }
    var dailyExerciseLogs by remember { mutableStateOf(emptyList<DailyExerciseEntry>()) }
    var dailyTotalsConsumed by remember { mutableStateOf<DailyTotals?>(null) }
    var consumedCalories by remember { mutableStateOf(0.0) }
    var exerciseCaloriesBurned by remember { mutableStateOf(0.0) }
    var includeExerciseCalories by remember { mutableStateOf(true) }
    var includeTEFBonus by remember { mutableStateOf(false) }
    
    // Pill tracking state
    var pills by remember { mutableStateOf(emptyList<Pill>()) }
    var currentPillCheckIn by remember { mutableStateOf<PillCheckIn?>(null) }

    var showSetGoalDialog by remember { mutableStateOf(false) }
    var showAddMealDialog by remember { mutableStateOf(false) }
    var showSelectMealDialog by remember { mutableStateOf(false) }
    
    // Store string resources in variables to avoid calling stringResource in non-composable contexts
    val exerciseLogDeletedSuccess = stringResource(R.string.exercise_log_deleted_success)
    val exerciseLogDeleteError = stringResource(R.string.exercise_log_delete_error)
    val goalSavedSuccess = stringResource(R.string.goal_saved_success)
    val goalSaveError = stringResource(R.string.goal_save_error)
    val mealAddedSuccess = stringResource(R.string.meal_added_success)
    val mealAddError = stringResource(R.string.meal_add_error)
    val checkInCompletedSuccess = stringResource(R.string.check_in_completed_success)
    val checkInCompletedError = stringResource(R.string.check_in_completed_error)
    val exerciseAddedSuccess = stringResource(R.string.exercise_added_success)
    val exerciseAddError = stringResource(R.string.exercise_add_error)
    val okText = stringResource(R.string.ok)
    val cancelText = stringResource(R.string.cancel)
    var showCheckInMealDialog by remember { mutableStateOf<Meal?>(null) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showSelectExerciseDialog by remember { mutableStateOf(false) }
    var showCheckInExerciseDialog by remember { mutableStateOf<Exercise?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    
    // Edit dialog state variables
    var showEditMealDialog by remember { mutableStateOf<DailyNutritionEntry?>(null) }
    var showEditExerciseDialog by remember { mutableStateOf<DailyExerciseEntry?>(null) }
    
    // Date navigation state - always start with today
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    
    // Snackbar state for error handling
    var snackbarHostState by remember { mutableStateOf(SnackbarHostState()) }

    LaunchedEffect(key1 = foodLogRepository) {
        foodLogRepository.getAllMeals().collectLatest { mealList ->
            meals = mealList
        }
    }

    LaunchedEffect(key1 = foodLogRepository) {
        foodLogRepository.getAllExercises().collectLatest { exerciseList ->
            exercises = exerciseList
        }
    }

    LaunchedEffect(key1 = foodLogRepository) {
        foodLogRepository.getUserGoal().collectLatest { goalFromDb ->
            userGoal = goalFromDb ?: UserGoal.default()
            if (goalFromDb == null) {
                coroutineScope.launch {
                    try {
                        foodLogRepository.upsertUserGoal(UserGoal.default())
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            message = "Failed to save goal. Please try again."
                        )
                    }
                }
            }
        }
    }

    // Load pills and initialize default pill if none exist
    LaunchedEffect(key1 = foodLogRepository) {
        foodLogRepository.getAllPills().collectLatest { pillList ->
            pills = pillList
            if (pillList.isEmpty()) {
                // Create default pill
                coroutineScope.launch {
                    try {
                        foodLogRepository.insertPill(Pill(name = "default"))
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            message = "Failed to create default pill. Please try again."
                        )
                    }
                }
            }
        }
    }

    val selectedDateString = remember(selectedDate) {
        selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    LaunchedEffect(key1 = foodLogRepository, key2 = selectedDateString) {
        foodLogRepository.getCheckInsByDate(selectedDateString).collectLatest { checkInsList ->
            dailyMealCheckIns = checkInsList
        }
    }

    LaunchedEffect(key1 = foodLogRepository, key2 = selectedDateString) {
        foodLogRepository.getExerciseLogsByDate(selectedDateString).collectLatest { exerciseLogsList ->
            dailyExerciseLogs = exerciseLogsList
        }
    }

    // Check for pill check-in on selected date
    LaunchedEffect(key1 = foodLogRepository, key2 = selectedDateString, key3 = pills) {
        if (pills.isNotEmpty()) {
            val defaultPillId = pills.first().id
            foodLogRepository.getPillCheckInByPillIdAndDate(defaultPillId, selectedDateString).collectLatest { checkIn ->
                currentPillCheckIn = checkIn
            }
        }
    }

    LaunchedEffect(key1 = foodLogRepository, key2 = selectedDateString) {
        foodLogRepository.getDailyExerciseCalories(selectedDateString).collectLatest { calories ->
            exerciseCaloriesBurned = calories
        }
    }

    // Load daily nutrient totals with exercise calories consideration
    LaunchedEffect(foodLogRepository, selectedDateString, includeExerciseCalories, includeTEFBonus) {
        foodLogRepository.getDailyCombinedTotals(selectedDateString, includeExerciseCalories, includeTEFBonus).collectLatest { totals ->
            dailyTotalsConsumed = totals
            consumedCalories = totals?.totalCalories ?: 0.0
        }
    }

    // Use themed colors instead of hardcoded values
    val gradientStartColor = appBackgroundColor()
    val gradientEndColor = appSurfaceVariantColor()
    val innerContainerBackgroundColor = if (isSystemInDarkTheme()) BrandNavyLight else Color.White

    // Calculate remaining calories based on actual data
    val remainingCalories = (userGoal.caloriesGoal - consumedCalories).toInt()

    val caloriesRemainingLabelColor = appTextSecondaryColor()
    val caloriesRemainingValueColor = appTextPrimaryColor()
    val caloriesConsumedColor = appTextPrimaryColor()
    val caloriesGoalColor = appTextSecondaryColor()

    // Nutrient specific colors (can be themed as well)
    val nutrientLabelColor = caloriesRemainingLabelColor
    val nutrientConsumedColor = caloriesConsumedColor
    val nutrientGoalColor = caloriesGoalColor


    val view = LocalView.current
    val density = LocalDensity.current
    
    // Calculate status bar height
    val statusBarHeight = with(density) {
        WindowInsetsCompat.Type.statusBars().let { insets ->
            view.rootWindowInsets?.getInsets(insets)?.top ?: 0
        }.toDp()
    }

    // Pill toggle function
    val onPillToggle: () -> Unit = {
        coroutineScope.launch {
            try {
                if (pills.isNotEmpty()) {
                    val defaultPillId = pills.first().id
                    if (currentPillCheckIn == null) {
                        // Create new pill check-in
                        val newCheckIn = PillCheckIn(
                            pillId = defaultPillId,
                            timestamp = java.time.LocalDateTime.now()
                        )
                        foodLogRepository.insertPillCheckIn(newCheckIn)
                        snackbarHostState.showSnackbar(
                            message = "Daily supplement taken"
                        )
                    } else {
                        // Delete existing pill check-in
                        foodLogRepository.deletePillCheckInByPillIdAndDate(defaultPillId, selectedDateString)
                        snackbarHostState.showSnackbar(
                            message = "Daily supplement removed"
                        )
                    }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Failed to update pill status. Please try again."
                )
            }
        }
    }

    // Exercise toggle function
    val onExerciseToggle: () -> Unit = {
        includeExerciseCalories = !includeExerciseCalories
        coroutineScope.launch {
            val message = if (includeExerciseCalories) {
                "Exercise bonus calories ON"
            } else {
                "Exercise bonus calories OFF"
            }
            snackbarHostState.showSnackbar(message = message)
        }
    }

    // TEF toggle function
    val onTEFToggle: () -> Unit = {
        includeTEFBonus = !includeTEFBonus
        coroutineScope.launch {
            val message = if (includeTEFBonus) {
                "TEF bonus calories ON"
            } else {
                "TEF bonus calories OFF"
            }
            snackbarHostState.showSnackbar(message = message)
        }
    }

    Scaffold(
        topBar = {
            // Custom header with date navigation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarHeight + 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Calendar icon on the left
                    IconButton(
                        onClick = { showCalendarDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = stringResource(R.string.select_date),
                            tint = Color(0xFFC0C0C0),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Center section with navigation arrows and date
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left arrow
                        IconButton(
                            onClick = { 
                                selectedDate = selectedDate.minusDays(1)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.previous_day),
                                tint = Color(0xFFC0C0C0),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                                                 // Date display
                         val today = java.time.LocalDate.now()
                         val dateText = if (selectedDate == today) {
                             stringResource(R.string.today)
                         } else {
                             selectedDate.format(
                                 java.time.format.DateTimeFormatter.ofLocalizedDate(
                                     java.time.format.FormatStyle.MEDIUM
                                 )
                             )
                         }
                         Text(
                             text = dateText,
                             fontSize = 20.sp,
                             fontWeight = FontWeight.Bold,
                             color = Color(0xFFC0C0C0),
                             textAlign = TextAlign.Center
                         )
                        
                        // Right arrow
                        IconButton(
                            onClick = { 
                                selectedDate = selectedDate.plusDays(1)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.next_day),
                                tint = Color(0xFFC0C0C0),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Progress bar button on the right
                    IconButton(
                        onClick = { /* TODO: Implement progress bar functionality */ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = stringResource(R.string.progress_details),
                            tint = Color(0xFFC0C0C0),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Two FABs for Add Meal and Add Exercise with modern styling
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Add Exercise FAB - using your brand red
                FloatingActionButton(
                    onClick = { showSelectExerciseDialog = true },
                    containerColor = fabExerciseColor(),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sprint), 
                        contentDescription = stringResource(R.string.add_exercise),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Add Meal FAB - using your brand gold
                FloatingActionButton(
                    onClick = { showSelectMealDialog = true },
                    containerColor = fabMealColor(),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        Icons.Filled.Restaurant, 
                        contentDescription = stringResource(R.string.add_meal),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = innerContainerBackgroundColor.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        // Calories ring section with symmetrical toggles
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Calories ring - centered as before
                            CaloriesRing(
                                consumedCalories = consumedCalories,
                                goalCalories = userGoal.caloriesGoal.toDouble(),
                                labelColor = caloriesRemainingLabelColor,
                                valueColor = caloriesRemainingValueColor,
                                consumedColor = caloriesConsumedColor,
                                goalColor = caloriesGoalColor
                            )
                            
                            // Exercise toggle - positioned on the left
                            ExerciseToggle(
                                isEnabled = includeExerciseCalories,
                                onToggle = onExerciseToggle,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                            
                            // TEF toggle - positioned on the right
                            TEFToggle(
                                isEnabled = includeTEFBonus,
                                onToggle = onTEFToggle,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Nutrient Details Section with Edit button
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.nutrient_details_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = appTextPrimaryColor(),
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = { showSetGoalDialog = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.CenterEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.set_goal),
                                    tint = appTextSecondaryColor(),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        NutrientBox(
                            totals = dailyTotalsConsumed,
                            goals = userGoal,
                            isDark = isSystemInDarkTheme()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Pill tracker - positioned after nutrients box
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            PillTracker(
                                isPillTaken = currentPillCheckIn != null,
                                pillCheckIn = currentPillCheckIn,
                                onPillToggle = onPillToggle
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            stringResource(R.string.recent_check_ins),
                            style = MaterialTheme.typography.titleMedium,
                            color = appTextPrimaryColor()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Use FilterableHistoryView for both meals and exercises
                        FilterableHistoryView(
                            mealEntries = dailyMealCheckIns,
                            exerciseEntries = dailyExerciseLogs,
                            onDeleteMeal = { }, // No longer used - delete only available in edit dialog
                            onDeleteExercise = { }, // No longer used - delete only available in edit dialog
                            onEditMeal = { checkIn ->
                                showEditMealDialog = checkIn
                            },
                            onEditExercise = { exerciseEntry ->
                                showEditExerciseDialog = exerciseEntry
                            }
                        )
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
                    try {
                        foodLogRepository.upsertUserGoal(updatedGoal)
                        snackbarHostState.showSnackbar(
                            message = goalSavedSuccess
                        )
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            message = goalSaveError
                        )
                    }
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
                    try {
                        foodLogRepository.insertMeal(newMeal)
                        snackbarHostState.showSnackbar(
                            message = mealAddedSuccess
                        )
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            message = mealAddError
                        )
                    }
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
                    try {
                        foodLogRepository.insertMealCheckIn(mealCheckIn)
                        snackbarHostState.showSnackbar(
                            message = checkInCompletedSuccess
                        )
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            message = checkInCompletedError
                        )
                    }
                }
                showCheckInMealDialog = null
            }
        )
    }

    if (showSelectExerciseDialog) {
        SelectExerciseForCheckInDialog(
            exercises = exercises,
            onDismiss = { showSelectExerciseDialog = false },
            onAddExercise = {
                showSelectExerciseDialog = false
                showAddExerciseDialog = true
            },
            onSelectExercise = { exercise ->
                showSelectExerciseDialog = false
                showCheckInExerciseDialog = exercise
            }
        )
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onAddExercise = { newExercise ->
                coroutineScope.launch {
                    try {
                        foodLogRepository.insertExercise(newExercise)
                        snackbarHostState.showSnackbar(
                            message = exerciseAddedSuccess
                        )
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            message = exerciseAddError
                        )
                    }
                }
                showAddExerciseDialog = false
            }
        )
    }

    showCheckInExerciseDialog?.let { exerciseToCheckIn ->
        var lastLog by remember { mutableStateOf<ExerciseLog?>(null) }
        var maxWeight by remember { mutableStateOf<Double?>(null) }
        
        // Load last log and max weight for this exercise
        LaunchedEffect(exerciseToCheckIn.id) {
            foodLogRepository.getLastLogForExercise(exerciseToCheckIn.id).collectLatest { log ->
                lastLog = log
            }
        }
        
        LaunchedEffect(exerciseToCheckIn.id) {
            foodLogRepository.getMaxWeightForExercise(exerciseToCheckIn.id).collectLatest { weight ->
                maxWeight = weight
            }
        }
        
        CheckInExerciseDialog(
            exercise = exerciseToCheckIn,
            lastLog = lastLog,
            maxWeight = maxWeight,
            onDismiss = { showCheckInExerciseDialog = null },
            onCheckIn = { exerciseLog ->
                coroutineScope.launch {
                    try {
                        foodLogRepository.insertExerciseLog(exerciseLog)
                        snackbarHostState.showSnackbar(
                            message = "Exercise check-in completed successfully"
                        )
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            message = "Failed to complete exercise check-in. Please try again."
                        )
                    }
                }
                showCheckInExerciseDialog = null
            }
        )
    }

    // Calendar Dialog - Using DatePickerDialog for better layout and compatibility
    if (showCalendarDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(
                java.time.ZoneId.systemDefault()
            ).toInstant().toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showCalendarDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = java.time.Instant.ofEpochMilli(millis)
                            val zonedDateTime = instant.atZone(java.time.ZoneId.systemDefault())
                            selectedDate = zonedDateTime.toLocalDate()
                        }
                        showCalendarDialog = false
                    }
                ) {
                    Text(okText, color = Color(0xFF60A5FA))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCalendarDialog = false }
                ) {
                    Text(cancelText, color = Color(0xFF9CA3AF))
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF374151),
                titleContentColor = Color(0xFFE5E7EB),
                headlineContentColor = Color(0xFFE5E7EB),
                weekdayContentColor = Color(0xFF9CA3AF),
                subheadContentColor = Color(0xFFE5E7EB),
                yearContentColor = Color(0xFFE5E7EB),
                currentYearContentColor = Color(0xFF60A5FA),
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = Color(0xFF60A5FA),
                dayContentColor = Color(0xFFE5E7EB),
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = Color(0xFF60A5FA),
                todayContentColor = Color(0xFF60A5FA),
                todayDateBorderColor = Color(0xFF60A5FA)
            )
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    
    // Edit Meal Dialog
    showEditMealDialog?.let { checkIn ->
        // Find the corresponding meal
        val meal = meals.find { it.id == checkIn.mealId }
        if (meal != null) {
            // Convert DailyNutritionEntry to MealCheckIn for editing
            val existingMealCheckIn = MealCheckIn(
                id = checkIn.checkInId,
                mealId = checkIn.mealId,
                checkInDate = checkIn.checkInDate,
                checkInDateTime = checkIn.checkInDateTime,
                servingSize = checkIn.servingSize,
                notes = checkIn.notes
            )
            
            UnifiedCheckInDialog<CheckInData.Meal>(
                onDismiss = { showEditMealDialog = null },
                onCheckIn = { checkInData ->
                    coroutineScope.launch {
                        try {
                            foodLogRepository.updateMealCheckIn(checkInData.mealCheckIn)
                            snackbarHostState.showSnackbar(
                                message = "Meal updated successfully"
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "Failed to update meal. Please try again."
                            )
                        }
                    }
                    showEditMealDialog = null
                },
                onDelete = {
                    coroutineScope.launch {
                        try {
                            foodLogRepository.deleteMealCheckIn(existingMealCheckIn)
                            snackbarHostState.showSnackbar(
                                message = "Meal deleted successfully"
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "Failed to delete meal. Please try again."
                            )
                        }
                    }
                    showEditMealDialog = null
                },
                isEditMode = true,
                meal = meal,
                existingMealCheckIn = existingMealCheckIn
            )
        }
    }
    
    // Edit Exercise Dialog
    showEditExerciseDialog?.let { exerciseEntry ->
        // Find the corresponding exercise
        val exercise = exercises.find { it.id == exerciseEntry.exerciseId }
        if (exercise != null) {
            // Convert DailyExerciseEntry to ExerciseLog for editing
            val existingExerciseLog = ExerciseLog(
                id = exerciseEntry.logId,
                exerciseId = exerciseEntry.exerciseId,
                logDate = exerciseEntry.logDate,
                logDateTime = exerciseEntry.logDateTime,
                weight = exerciseEntry.weight,
                reps = exerciseEntry.reps,
                sets = exerciseEntry.sets,
                caloriesBurned = exerciseEntry.caloriesBurned,
                notes = exerciseEntry.notes
            )
            
            UnifiedCheckInDialog<CheckInData.Exercise>(
                onDismiss = { showEditExerciseDialog = null },
                onCheckIn = { checkInData ->
                    coroutineScope.launch {
                        try {
                            foodLogRepository.updateExerciseLog(checkInData.exerciseLog)
                            snackbarHostState.showSnackbar(
                                message = "Exercise updated successfully"
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "Failed to update exercise. Please try again."
                            )
                        }
                    }
                    showEditExerciseDialog = null
                },
                onDelete = {
                    coroutineScope.launch {
                        try {
                            foodLogRepository.deleteExerciseLog(existingExerciseLog)
                            snackbarHostState.showSnackbar(
                                message = "Exercise deleted successfully"
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "Failed to delete exercise. Please try again."
                            )
                        }
                    }
                    showEditExerciseDialog = null
                },
                isEditMode = true,
                exercise = exercise,
                existingExerciseLog = existingExerciseLog
            )
        }
    }
}
