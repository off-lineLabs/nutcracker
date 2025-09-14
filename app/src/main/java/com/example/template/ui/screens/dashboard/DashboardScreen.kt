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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
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
import com.example.template.data.model.ExternalExercise
import com.example.template.data.model.toInternalExercise
import com.example.template.data.model.UserGoal
import com.example.template.data.model.FoodInfo
import com.example.template.data.model.OpenFoodFactsResponse
import com.example.template.data.model.ServingSizeUnit
import com.example.template.data.service.ImageDownloadService
import com.example.template.data.mapper.FoodInfoMapper
import com.example.template.data.mapper.FoodInfoToMealMapper
import com.example.template.ui.components.dialogs.AddMealDialog
import com.example.template.ui.components.dialogs.CheckInMealDialog
import com.example.template.ui.components.dialogs.SelectMealForCheckInDialog
import com.example.template.ui.components.dialogs.BarcodeScanDialog
import com.example.template.ui.components.dialogs.SimpleBarcodeScanDialog
import com.example.template.ui.components.dialogs.GoogleCodeScannerDialog
import com.example.template.ui.components.dialogs.FoodInfoDialog
import com.example.template.ui.components.dialogs.FoodSearchDialog
import com.example.template.ui.components.dialogs.ServingSizeDialog
import com.example.template.ui.components.dialogs.AddExerciseDialog
import com.example.template.ui.components.dialogs.CheckInExerciseDialog
import com.example.template.ui.components.dialogs.SelectExerciseForCheckInDialog
import com.example.template.ui.components.dialogs.EnhancedSelectExerciseDialog
import com.example.template.ui.components.dialogs.SetGoalDialog
import com.example.template.ui.components.dialogs.UnifiedCheckInDialog
import com.example.template.ui.components.dialogs.UnifiedExerciseDetailsDialog
import com.example.template.ui.components.dialogs.UnifiedMealDetailsDialog
import com.example.template.ui.components.dialogs.EditMealDialog
import com.example.template.data.model.CheckInData
import com.example.template.ui.components.FilterableHistoryView
import com.example.template.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.template.util.logger.AppLogger
import com.example.template.util.logger.logUserAction
import com.example.template.util.logger.safeSuspendExecute

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
    val proteinFiberColor = ProteinFiberColor
    val fiberLabel = stringResource(R.string.fiber_label)
    val isFiber = nutrientName == fiberLabel
    val finalValueColor = when {
        consumed > goal && isFiber -> proteinFiberColor
        consumed > goal -> ExceededColor
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
    foodCalories: Double,
    goalCalories: Double,
    exerciseCalories: Double = 0.0,
    tefCalories: Double = 0.0,
    includeExercise: Boolean = false,
    includeTEF: Boolean = false,
    labelColor: Color,
    valueColor: Color,
    consumedColor: Color,
    goalColor: Color,
    ringTrackColor: Color = Color(0xFF374151),
    ringProgressColor: Color = Color(0xFFFFA94D),
    sizeDp: Dp = 160.dp,
    strokeWidthDp: Dp = 12.dp
) {
    // Calculate booster bonuses
    val exerciseBonus = if (includeExercise) exerciseCalories else 0.0
    val tefBonus = if (includeTEF) tefCalories else 0.0
    val totalBonus = exerciseBonus + tefBonus
    
    // Remaining calories = goal - food calories + booster bonuses
    val remaining = goalCalories - foodCalories + totalBonus
    val progress = if (goalCalories > 0) (foodCalories / goalCalories).toFloat().coerceIn(0f, 1f) else 0f
    val finalValueColor = if (remaining < 0) ExceededColor else valueColor

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
                        append(String.format(Locale.getDefault(), "%.0f", foodCalories))
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
    val proteinFiberColor = ProteinFiberColor
    val proteinLabel = stringResource(R.string.protein_label)
    val fiberLabel = stringResource(R.string.fiber_label)
    val isProteinOrFiber = title == proteinLabel || title == fiberLabel
    val finalConsumedColor = when {
        consumed > goal && isProteinOrFiber -> proteinFiberColor
        consumed > goal -> ExceededColor
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
    goals: UserGoal
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
fun DashboardScreen(
    onNavigateToSettings: () -> Unit = {},
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val foodLogRepository = (context.applicationContext as FoodLogApplication).foodLogRepository
    val imageDownloadService = remember { ImageDownloadService(context) }
    val externalExerciseService = (context.applicationContext as FoodLogApplication).externalExerciseService
    val exerciseImageService = (context.applicationContext as FoodLogApplication).exerciseImageService
    val coroutineScope = rememberCoroutineScope()

    var meals by remember { mutableStateOf(emptyList<Meal>()) }
    var exercises by remember { mutableStateOf(emptyList<Exercise>()) }
    var userGoal by remember { mutableStateOf(UserGoal.default()) }
    var dailyMealCheckIns by remember { mutableStateOf(emptyList<DailyNutritionEntry>()) }
    var dailyExerciseLogs by remember { mutableStateOf(emptyList<DailyExerciseEntry>()) }
    var dailyTotalsConsumed by remember { mutableStateOf<DailyTotals?>(null) }
    var consumedCalories by remember { mutableDoubleStateOf(0.0) }
    var foodCalories by remember { mutableDoubleStateOf(0.0) }
    var exerciseCaloriesBurned by remember { mutableDoubleStateOf(0.0) }
    var tefCaloriesBurned by remember { mutableDoubleStateOf(0.0) }
    var includeExerciseCalories by remember { mutableStateOf(true) }
    var includeTEFBonus by remember { mutableStateOf(false) }
    
    // Pill tracking state
    var pills by remember { mutableStateOf(emptyList<Pill>()) }
    var currentPillCheckIn by remember { mutableStateOf<PillCheckIn?>(null) }

    var showSetGoalDialog by remember { mutableStateOf(false) }
    var showAddMealDialog by remember { mutableStateOf(false) }
    var showSelectMealDialog by remember { mutableStateOf(false) }
    var showBarcodeScanDialog by remember { mutableStateOf(false) }
    var showFoodSearchDialog by remember { mutableStateOf(false) }
    var showFoodInfoDialog by remember { mutableStateOf<FoodInfo?>(null) }
    var showServingSizeDialog by remember { mutableStateOf<FoodInfo?>(null) }
    var currentBarcode by remember { mutableStateOf<String?>(null) }
    
    // Store string resources in variables to avoid calling stringResource in non-composable contexts
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
    
    // Additional string resources for hardcoded strings
    val failedToSaveGoal = stringResource(R.string.failed_to_save_goal)
    val failedToCreateDefaultPill = stringResource(R.string.failed_to_create_default_pill)
    val dailySupplementTaken = stringResource(R.string.daily_supplement_taken)
    val dailySupplementRemoved = stringResource(R.string.daily_supplement_removed)
    val failedToUpdatePillStatus = stringResource(R.string.failed_to_update_pill_status)
    val exerciseBonusCaloriesOn = stringResource(R.string.exercise_bonus_calories_on)
    val exerciseBonusCaloriesOff = stringResource(R.string.exercise_bonus_calories_off)
    val tefBonusCaloriesOn = stringResource(R.string.tef_bonus_calories_on)
    val tefBonusCaloriesOff = stringResource(R.string.tef_bonus_calories_off)
    val exerciseCheckInCompletedSuccess = stringResource(R.string.exercise_check_in_completed_success)
    val failedToCompleteExerciseCheckIn = stringResource(R.string.failed_to_complete_exercise_check_in)
    val mealUpdatedSuccess = stringResource(R.string.meal_updated_success)
    val failedToUpdateMeal = stringResource(R.string.failed_to_update_meal)
    val mealDeletedSuccess = stringResource(R.string.meal_deleted_success)
    val failedToDeleteMeal = stringResource(R.string.failed_to_delete_meal)
    val exerciseUpdatedSuccess = stringResource(R.string.exercise_updated_success)
    val failedToUpdateExercise = stringResource(R.string.failed_to_update_exercise)
    val exerciseDeletedSuccess = stringResource(R.string.exercise_deleted_success)
    val failedToDeleteExercise = stringResource(R.string.failed_to_delete_exercise)
    val defaultPillName = stringResource(R.string.default_pill_name)
    val dateFormatPattern = stringResource(R.string.date_format_pattern)
    var showCheckInMealDialog by remember { mutableStateOf<Meal?>(null) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showSelectExerciseDialog by remember { mutableStateOf(false) }
    var showCheckInExerciseDialog by remember { mutableStateOf<Exercise?>(null) }
    var showUnifiedExerciseDetailDialog by remember { mutableStateOf<Exercise?>(null) }
    var showUnifiedMealDetailDialog by remember { mutableStateOf<Meal?>(null) }
    var showEditMealDefinitionDialog by remember { mutableStateOf<Meal?>(null) }
    var selectedExternalExercise by remember { mutableStateOf<ExternalExercise?>(null) }
    var selectedExerciseForEdit by remember { mutableStateOf<Exercise?>(null) }
    var selectedExerciseForCheckIn by remember { mutableStateOf<Exercise?>(null) }
    var lastExerciseLog by remember { mutableStateOf<ExerciseLog?>(null) }
    var maxExerciseWeight by remember { mutableStateOf<Double?>(null) }
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
                    safeSuspendExecute(
                        operation = "Initialize default user goal",
                        block = {
                            foodLogRepository.upsertUserGoal(UserGoal.default())
                        },
                        onError = { e ->
                            AppLogger.exception("DashboardScreen", "Failed to save default user goal", e)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = failedToSaveGoal
                                )
                            }
                        }
                    )
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
                    safeSuspendExecute(
                        operation = "Create default pill",
                        block = {
                            foodLogRepository.insertPill(Pill(name = defaultPillName))
                        },
                        onError = { e ->
                            AppLogger.exception("DashboardScreen", "Failed to create default pill", e)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = failedToCreateDefaultPill
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    val selectedDateString = remember(selectedDate) {
        selectedDate.format(java.time.format.DateTimeFormatter.ofPattern(dateFormatPattern))
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

    // Calculate TEF calories from daily totals
    LaunchedEffect(key1 = foodLogRepository, key2 = selectedDateString) {
        foodLogRepository.getDailyNutrientTotals(selectedDateString).collectLatest { totals ->
            tefCaloriesBurned = totals?.let { 
                com.example.template.utils.TEFCalculator.calculateTEFBonus(it) 
            } ?: 0.0
        }
    }

    // Load raw food calories (without any booster adjustments)
    LaunchedEffect(foodLogRepository, selectedDateString) {
        foodLogRepository.getDailyNutrientTotals(selectedDateString).collectLatest { totals ->
            foodCalories = totals?.totalCalories ?: 0.0
        }
    }

    // Load daily nutrient totals with exercise calories consideration
    LaunchedEffect(foodLogRepository, selectedDateString, includeExerciseCalories, includeTEFBonus) {
        foodLogRepository.getDailyCombinedTotals(selectedDateString, includeExerciseCalories, includeTEFBonus).collectLatest { totals ->
            dailyTotalsConsumed = totals
            consumedCalories = totals?.totalCalories ?: 0.0
        }
    }

    // Use specific colors for the exact look you want
    val lightGray50 = Color(0xFFFAFBFC)  // Improved softer background
    val lightGray100 = Color(0xFFF3F4F6) // Your specific light gray
    val darkGray800 = Color(0xFF1F2937)  // Your specific dark gray for container
    val darkGray900 = Color(0xFF111827)  // Your specific dark background
    val textGray200 = Color(0xFFE5E7EB)  // Your specific text gray

    val gradientStartColor = if (isDarkTheme) darkGray900 else lightGray50
    val gradientEndColor = if (isDarkTheme) darkGray800 else lightGray100
    val innerContainerBackgroundColor = if (isDarkTheme) darkGray800 else Color(0xFFC6C6C7)

    val caloriesRemainingLabelColor = if (isDarkTheme) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val caloriesRemainingValueColor = if (isDarkTheme) Color.White else Color(0xFF111827)
    val caloriesConsumedColor = if (isDarkTheme) textGray200 else Color(0xFF1F2937)
    val caloriesGoalColor = if (isDarkTheme) Color(0xFF6B7280) else Color(0xFF9CA3AF)

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
                            message = dailySupplementTaken
                        )
                    } else {
                        // Delete existing pill check-in
                        foodLogRepository.deletePillCheckInByPillIdAndDate(defaultPillId, selectedDateString)
                        snackbarHostState.showSnackbar(
                            message = dailySupplementRemoved
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.exception("DashboardScreen", "Failed to update pill status", e, mapOf(
                    "selectedDate" to selectedDateString,
                    "pillsCount" to pills.size
                ))
                snackbarHostState.showSnackbar(
                    message = failedToUpdatePillStatus
                )
            }
        }
    }

    // Exercise toggle function
    val onExerciseToggle: () -> Unit = {
        includeExerciseCalories = !includeExerciseCalories
        coroutineScope.launch {
            val message = if (includeExerciseCalories) {
                exerciseBonusCaloriesOn
            } else {
                exerciseBonusCaloriesOff
            }
            snackbarHostState.showSnackbar(message = message)
        }
    }

    // TEF toggle function
    val onTEFToggle: () -> Unit = {
        includeTEFBonus = !includeTEFBonus
        coroutineScope.launch {
            val message = if (includeTEFBonus) {
                tefBonusCaloriesOn
            } else {
                tefBonusCaloriesOff
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
                    
                    // Settings and Progress bar buttons on the right
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Settings button
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.settings_icon_description),
                                tint = Color(0xFFC0C0C0),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Progress bar button
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
                                foodCalories = foodCalories,
                                goalCalories = userGoal.caloriesGoal.toDouble(),
                                exerciseCalories = exerciseCaloriesBurned,
                                tefCalories = tefCaloriesBurned,
                                includeExercise = includeExerciseCalories,
                                includeTEF = includeTEFBonus,
                                labelColor = caloriesRemainingLabelColor,
                                valueColor = caloriesRemainingValueColor,
                                consumedColor = caloriesConsumedColor,
                                goalColor = caloriesGoalColor
                            )
                            
                            // Exercise toggle - positioned on the left
                            ExerciseToggle(
                                isEnabled = includeExerciseCalories,
                                onToggle = onExerciseToggle,
                                exerciseCalories = exerciseCaloriesBurned,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                            
                            // TEF toggle - positioned on the right
                            TEFToggle(
                                isEnabled = includeTEFBonus,
                                onToggle = onTEFToggle,
                                tefCalories = tefCaloriesBurned,
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
                            goals = userGoal
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
                    safeSuspendExecute(
                        operation = "Update user goal",
                        block = {
                            foodLogRepository.upsertUserGoal(updatedGoal)
                            logUserAction("Goal Updated", mapOf(
                                "calories" to updatedGoal.caloriesGoal,
                                "protein" to updatedGoal.proteinGoal_g,
                                "carbs" to updatedGoal.carbsGoal_g,
                                "fat" to updatedGoal.fatGoal_g
                            ))
                        },
                        onError = { e ->
                            AppLogger.exception("DashboardScreen", "Failed to save user goal", e, mapOf(
                                "goal" to updatedGoal.toString()
                            ))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = goalSaveError
                                )
                            }
                        }
                    )
                    snackbarHostState.showSnackbar(
                        message = goalSavedSuccess
                    )
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
            },
            onEditMeal = { meal ->
                showSelectMealDialog = false
                showEditMealDefinitionDialog = meal
            },
            onSearchMeal = {
                showSelectMealDialog = false
                showFoodSearchDialog = true
            },
            onScanBarcode = {
                showSelectMealDialog = false
                showBarcodeScanDialog = true
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
                        AppLogger.exception("DashboardScreen", "Failed to add meal", e, mapOf(
                            "mealName" to newMeal.name
                        ))
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
                        AppLogger.exception("DashboardScreen", "Failed to complete meal check-in", e, mapOf(
                            "mealId" to mealCheckIn.mealId,
                            "date" to mealCheckIn.checkInDate
                        ))
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
        EnhancedSelectExerciseDialog(
            exercises = exercises,
            externalExerciseService = externalExerciseService,
            onDismiss = { showSelectExerciseDialog = false },
            onAddExercise = {
                showSelectExerciseDialog = false
                showAddExerciseDialog = true
            },
            onSelectExercise = { exercise ->
                showSelectExerciseDialog = false
                showUnifiedExerciseDetailDialog = exercise
            },
            onImportExternalExercise = { externalExercise ->
                // Store the external exercise for pre-populating the AddExerciseDialog
                selectedExternalExercise = externalExercise
                showSelectExerciseDialog = false
                showAddExerciseDialog = true
            },
            onEditExercise = { exercise ->
                showSelectExerciseDialog = false
                selectedExerciseForEdit = exercise
                showAddExerciseDialog = true
            }
        )
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            externalExercise = selectedExternalExercise,
            existingExercise = selectedExerciseForEdit,
            onDismiss = { 
                showAddExerciseDialog = false
                selectedExternalExercise = null
                selectedExerciseForEdit = null
            },
            onAddExercise = { newExercise ->
                coroutineScope.launch {
                    try {
                        val existingExercise = selectedExerciseForEdit
                        val externalExercise = selectedExternalExercise
                        
                        if (existingExercise != null) {
                            // Update existing exercise - preserve existing image paths
                            val updatedExercise = newExercise.copy(
                                id = existingExercise.id,
                                imagePaths = existingExercise.imagePaths // Preserve existing image paths
                            )
                            foodLogRepository.updateExercise(updatedExercise)
                            AppLogger.i("DashboardScreen", "Exercise updated: ${updatedExercise.name}")
                            
                            snackbarHostState.showSnackbar(
                                message = exerciseUpdatedSuccess
                            )
                        } else {
                            // Add new exercise
                            AppLogger.i("DashboardScreen", "onAddExercise called with externalExercise: ${externalExercise?.name}")
                            
                            // Insert exercise first to get an ID
                            val exerciseId = foodLogRepository.insertExercise(newExercise)
                            AppLogger.i("DashboardScreen", "Exercise inserted with ID: $exerciseId")
                            
                            // Download and save all images if available
                            externalExercise?.let { exercise ->
                                if (exercise.images.isNotEmpty()) {
                                    val imageUrls = exercise.images.map { externalExerciseService.getImageUrl(it) }
                                    AppLogger.i("DashboardScreen", "Downloading ${imageUrls.size} images for exercise: ${newExercise.name}")
                                    
                                    // Download all images
                                    val localImagePaths = exerciseImageService.downloadAndStoreImages(imageUrls, exerciseId.toString())
                                    
                                    if (localImagePaths.isNotEmpty()) {
                                        AppLogger.i("DashboardScreen", "Downloaded ${localImagePaths.size} images successfully")
                                        
                                        // Create the complete exercise with all external data
                                        val completeExercise = exercise.toInternalExercise(localImagePaths).copy(
                                            id = exerciseId,
                                            kcalBurnedPerRep = newExercise.kcalBurnedPerRep,
                                            kcalBurnedPerMinute = newExercise.kcalBurnedPerMinute,
                                            defaultWeight = newExercise.defaultWeight,
                                            defaultReps = newExercise.defaultReps,
                                            defaultSets = newExercise.defaultSets,
                                            notes = newExercise.notes // Keep user's personal notes separate from instructions
                                        )
                                        
                                        // Update the exercise with all the data
                                        foodLogRepository.updateExercise(completeExercise)
                                        AppLogger.i("DashboardScreen", "Exercise updated with complete external data and ${localImagePaths.size} images")
                                    } else {
                                        AppLogger.w("DashboardScreen", "Failed to download any images for exercise ID: $exerciseId")
                                    }
                                } else {
                                    AppLogger.i("DashboardScreen", "No images available for exercise ID: $exerciseId")
                                }
                            } ?: run {
                                AppLogger.i("DashboardScreen", "No external exercise data for exercise ID: $exerciseId")
                            }
                            
                            snackbarHostState.showSnackbar(
                                message = exerciseAddedSuccess
                            )
                        }
                        
                        // Clear the external exercise and selected exercise after successful processing
                        selectedExternalExercise = null
                        selectedExerciseForEdit = null
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Failed to ${if (selectedExerciseForEdit != null) "update" else "add"} exercise", e, mapOf(
                            "exerciseName" to newExercise.name
                        ))
                        snackbarHostState.showSnackbar(
                            message = if (selectedExerciseForEdit != null) failedToUpdateExercise else exerciseAddError
                        )
                        // Clear state after error handling is complete
                        selectedExternalExercise = null
                        selectedExerciseForEdit = null
                    }
                }
                showAddExerciseDialog = false
            },
            onDelete = if (selectedExerciseForEdit != null) {
                {
                    val exerciseToDelete = selectedExerciseForEdit!!
                    coroutineScope.launch {
                        try {
                            foodLogRepository.deleteExercise(exerciseToDelete)
                            snackbarHostState.showSnackbar(
                                message = "Exercise deleted successfully!"
                            )
                        } catch (e: Exception) {
                            AppLogger.exception("DashboardScreen", "Failed to delete exercise", e, mapOf(
                                "exerciseId" to exerciseToDelete.id.toString(),
                                "exerciseName" to exerciseToDelete.name
                            ))
                            snackbarHostState.showSnackbar(
                                message = "Failed to delete exercise: ${e.message}"
                            )
                        }
                    }
                    showAddExerciseDialog = false
                    selectedExerciseForEdit = null
                }
            } else null
        )
    }

    showCheckInExerciseDialog?.let { exerciseToCheckIn ->
        UnifiedCheckInDialog<CheckInData.Exercise>(
            exercise = exerciseToCheckIn,
            lastLog = lastExerciseLog,
            maxWeight = maxExerciseWeight,
            onDismiss = { 
                showCheckInExerciseDialog = null
                selectedExerciseForCheckIn = null
                lastExerciseLog = null
                maxExerciseWeight = null
            },
            onCheckIn = { checkInData ->
                coroutineScope.launch {
                    try {
                        foodLogRepository.insertExerciseLog(checkInData.exerciseLog)
                        snackbarHostState.showSnackbar(
                            message = exerciseCheckInCompletedSuccess
                        )
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Failed to complete exercise check-in", e, mapOf(
                            "exerciseId" to checkInData.exerciseLog.exerciseId,
                            "date" to checkInData.exerciseLog.logDate
                        ))
                        snackbarHostState.showSnackbar(
                            message = failedToCompleteExerciseCheckIn
                        )
                    }
                }
                showCheckInExerciseDialog = null
                selectedExerciseForCheckIn = null
                lastExerciseLog = null
                maxExerciseWeight = null
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
                                message = mealUpdatedSuccess
                            )
                        } catch (e: Exception) {
                            AppLogger.exception("DashboardScreen", "Failed to update meal", e, mapOf(
                                "mealId" to existingMealCheckIn.mealId,
                                "date" to existingMealCheckIn.checkInDate
                            ))
                            snackbarHostState.showSnackbar(
                                message = failedToUpdateMeal
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
                                message = mealDeletedSuccess
                            )
                        } catch (e: Exception) {
                            AppLogger.exception("DashboardScreen", "Failed to delete meal", e, mapOf(
                                "mealId" to existingMealCheckIn.mealId,
                                "date" to existingMealCheckIn.checkInDate
                            ))
                            snackbarHostState.showSnackbar(
                                message = failedToDeleteMeal
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
                                message = exerciseUpdatedSuccess
                            )
                        } catch (e: Exception) {
                            AppLogger.exception("DashboardScreen", "Failed to update exercise", e, mapOf(
                                "exerciseId" to existingExerciseLog.exerciseId,
                                "date" to existingExerciseLog.logDate
                            ))
                            snackbarHostState.showSnackbar(
                                message = failedToUpdateExercise
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
                                message = exerciseDeletedSuccess
                            )
                        } catch (e: Exception) {
                            AppLogger.exception("DashboardScreen", "Failed to delete exercise", e, mapOf(
                                "exerciseId" to existingExerciseLog.exerciseId,
                                "date" to existingExerciseLog.logDate
                            ))
                            snackbarHostState.showSnackbar(
                                message = failedToDeleteExercise
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

    if (showFoodSearchDialog) {
        FoodSearchDialog(
            onDismiss = { showFoodSearchDialog = false },
            onProductSelected = { product ->
                // Convert Product to FoodInfo and show the food info dialog
                val foodInfo = FoodInfoMapper.mapToFoodInfo(
                    OpenFoodFactsResponse(
                        status = 1,
                        statusVerbose = "found",
                        product = product
                    ),
                    currentLanguage = (context.applicationContext as FoodLogApplication).settingsManager.currentAppLanguage
                )
                if (foodInfo != null) {
                    showFoodInfoDialog = foodInfo
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = stringResource(R.string.unable_to_process_food_info)
                        )
                    }
                }
            },
            openFoodFactsService = (context.applicationContext as FoodLogApplication).openFoodFactsService,
            currentLanguage = (context.applicationContext as FoodLogApplication).settingsManager.currentAppLanguage
        )
    }

    if (showBarcodeScanDialog) {
        GoogleCodeScannerDialog(
            onDismiss = { showBarcodeScanDialog = false },
            onBarcodeScanned = { barcode ->
                // Store barcode for later use
                currentBarcode = barcode
                AppLogger.d("DashboardScreen", "Barcode scanned and stored: $barcode")
                // Fetch food information from Open Food Facts API
                coroutineScope.launch {
                    try {
                        val response = (context.applicationContext as FoodLogApplication).openFoodFactsService.getProductByBarcode(barcode)
                        response.fold(
                            onSuccess = { apiResponse ->
                                AppLogger.d("DashboardScreen", "API call successful for barcode: $barcode")
                                val foodInfo = FoodInfoMapper.mapToFoodInfo(
                                    apiResponse,
                                    currentLanguage = (context.applicationContext as FoodLogApplication).settingsManager.currentAppLanguage
                                )
                                if (foodInfo != null) {
                                    AppLogger.d("DashboardScreen", "FoodInfo created successfully, showing FoodInfoDialog")
                                    showFoodInfoDialog = foodInfo
                                } else {
                                    AppLogger.d("DashboardScreen", "FoodInfo is null, showing error message")
                                    snackbarHostState.showSnackbar(
                                        message = stringResource(R.string.food_info_not_found)
                                    )
                                }
                            },
                            onFailure = { error ->
                                AppLogger.exception("DashboardScreen", "Failed to fetch food info", error, mapOf(
                                    "barcode" to barcode
                                ))
                                snackbarHostState.showSnackbar(
                                    message = "Failed to fetch food information: ${error.message}"
                                )
                            }
                        )
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Unexpected error fetching food info", e, mapOf(
                            "barcode" to barcode
                        ))
                        snackbarHostState.showSnackbar(
                            message = stringResource(R.string.error_fetching_food_info)
                        )
                    }
                }
                showBarcodeScanDialog = false
            }
        )
    }

    // Food Info Dialog
    showFoodInfoDialog?.let { foodInfo ->
        AppLogger.d("DashboardScreen", "Showing FoodInfoDialog for: ${foodInfo.name}")
        FoodInfoDialog(
            foodInfo = foodInfo,
            onBack = { 
                AppLogger.d("DashboardScreen", "FoodInfoDialog dismissed via back button")
                showFoodInfoDialog = null
                // Don't reset currentBarcode here - it should persist through the flow
            },
            onAddToMeals = {
                AppLogger.d("DashboardScreen", "Transitioning from FoodInfoDialog to ServingSizeDialog, currentBarcode: $currentBarcode")
                showFoodInfoDialog = null
                showServingSizeDialog = foodInfo
                // currentBarcode should still be set from the barcode scan
            }
        )
    }
    
    // Serving Size Dialog
    showServingSizeDialog?.let { foodInfo ->
        AppLogger.d("DashboardScreen", "Showing ServingSizeDialog for: ${foodInfo.name}, currentBarcode: $currentBarcode")
        ServingSizeDialog(
            foodInfo = foodInfo,
            barcode = currentBarcode,
            onDismiss = { 
                showServingSizeDialog = null
                currentBarcode = null
            },
            onConfirm = { servingSizeValue, servingSizeUnit ->
                // Store the barcode before it gets reset by onDismiss
                val barcodeToUse = currentBarcode
                AppLogger.d("DashboardScreen", "Creating meal with currentBarcode: $barcodeToUse")
                
                coroutineScope.launch {
                    try {
                        // Convert FoodInfo to Meal
                        val meal = FoodInfoToMealMapper.mapToMeal(
                            foodInfo = foodInfo,
                            servingSizeValue = servingSizeValue,
                            servingSizeUnit = servingSizeUnit,
                            barcode = barcodeToUse,
                            source = if (barcodeToUse != null) "barcode" else "search"
                        )
                        
                        // Download image if available
                        val localImagePath = if (foodInfo.imageUrl != null) {
                            imageDownloadService.downloadAndSaveImage(foodInfo.imageUrl)
                        } else null
                        
                        // Update meal with local image path
                        val finalMeal = meal.copy(localImagePath = localImagePath)
                        
                        // Insert meal into database and get the new mealId
                        val newMealId = foodLogRepository.insertMeal(finalMeal)
                        
                        // Update the meal with the new ID from database
                        val mealWithId = finalMeal.copy(id = newMealId)
                        
                        // Show unified dialog with the added meal (now with correct ID)
                        showUnifiedMealDetailDialog = mealWithId
                        
                        snackbarHostState.showSnackbar(
                            message = "Meal added successfully!"
                        )
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Failed to add meal from food info", e, mapOf(
                            "foodName" to foodInfo.name
                        ))
                        snackbarHostState.showSnackbar(
                            message = "Failed to add meal: ${e.message}"
                        )
                    }
                }
                showServingSizeDialog = null
                currentBarcode = null
            }
        )
    }

    // Unified Exercise Detail Dialog
    showUnifiedExerciseDetailDialog?.let { exercise ->
        UnifiedExerciseDetailsDialog(
            exercise = exercise,
            externalExerciseService = externalExerciseService,
            exerciseImageService = exerciseImageService,
            onBack = {
                showUnifiedExerciseDetailDialog = null
            },
            onEdit = {
                showUnifiedExerciseDetailDialog = null
                selectedExerciseForEdit = exercise
                showAddExerciseDialog = true
            },
            onCheckIn = {
                showUnifiedExerciseDetailDialog = null
                // Launch check-in dialog for this exercise
                coroutineScope.launch {
                    try {
                        val lastLog = foodLogRepository.getLastLogForExercise(exercise.id).first()
                        val maxWeight = foodLogRepository.getMaxWeightForExercise(exercise.id).first()
                        
                        // Set up the check-in dialog state
                        selectedExerciseForCheckIn = exercise
                        lastExerciseLog = lastLog
                        maxExerciseWeight = maxWeight
                        showCheckInExerciseDialog = exercise
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Failed to get exercise data for check-in", e)
                        snackbarHostState.showSnackbar(
                            message = stringResource(R.string.failed_to_load_exercise_data)
                        )
                    }
                }
            }
        )
    }

    // Unified Meal Detail Dialog
    showUnifiedMealDetailDialog?.let { meal ->
        UnifiedMealDetailsDialog(
            meal = meal,
            onBack = {
                showUnifiedMealDetailDialog = null
            },
            onEdit = { mealToEdit ->
                showUnifiedMealDetailDialog = null
                showEditMealDefinitionDialog = mealToEdit
            },
            onCheckIn = {
                showUnifiedMealDetailDialog = null
                showCheckInMealDialog = meal
            }
        )
    }

    // Edit Meal Definition Dialog
    showEditMealDefinitionDialog?.let { meal ->
        EditMealDialog(
            meal = meal,
            onDismiss = {
                showEditMealDefinitionDialog = null
            },
            onUpdateMeal = { updatedMeal ->
                // Close the edit dialog immediately
                showEditMealDefinitionDialog = null
                showUnifiedMealDetailDialog = updatedMeal
                
                // Perform database update in background
                coroutineScope.launch {
                    try {
                        foodLogRepository.updateMeal(updatedMeal)
                        snackbarHostState.showSnackbar(
                            message = "Meal updated successfully!"
                        )
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Failed to update meal", e, mapOf(
                            "mealId" to updatedMeal.id.toString(),
                            "mealName" to updatedMeal.name
                        ))
                        snackbarHostState.showSnackbar(
                            message = "Failed to update meal: ${e.message}"
                        )
                    }
                }
            },
            onDelete = {
                coroutineScope.launch {
                    try {
                        foodLogRepository.deleteMeal(meal)
                        snackbarHostState.showSnackbar(
                            message = "Meal deleted successfully!"
                        )
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Failed to delete meal", e, mapOf(
                            "mealId" to meal.id.toString(),
                            "mealName" to meal.name
                        ))
                        snackbarHostState.showSnackbar(
                            message = "Failed to delete meal: ${e.message}"
                        )
                    }
                }
                showEditMealDefinitionDialog = null
            }
        )
    }
}
