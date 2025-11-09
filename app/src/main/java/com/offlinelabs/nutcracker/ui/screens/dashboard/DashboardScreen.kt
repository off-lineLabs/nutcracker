package com.offlinelabs.nutcracker.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.os.Build
import android.util.Log
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.offlinelabs.nutcracker.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.offlinelabs.nutcracker.ui.components.MultiPillTracker
import com.offlinelabs.nutcracker.ui.components.ExerciseToggle
import com.offlinelabs.nutcracker.ui.components.TEFToggle
import com.offlinelabs.nutcracker.data.model.Pill
import com.offlinelabs.nutcracker.data.model.PillCheckIn
import com.offlinelabs.nutcracker.data.model.ExerciseCategoryMapper
import com.offlinelabs.nutcracker.data.model.ExerciseType
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import com.offlinelabs.nutcracker.FoodLogApplication
import com.offlinelabs.nutcracker.data.dao.DailyNutritionEntry
import com.offlinelabs.nutcracker.data.dao.DailyExerciseEntry
import com.offlinelabs.nutcracker.data.dao.DailyTotals
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.ExternalExercise
import com.offlinelabs.nutcracker.data.model.toInternalExercise
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.data.model.FoodInfo
import com.offlinelabs.nutcracker.data.model.OpenFoodFactsResponse
import com.offlinelabs.nutcracker.data.service.ImageDownloadService
import com.offlinelabs.nutcracker.data.mapper.FoodInfoMapper
import com.offlinelabs.nutcracker.data.mapper.FoodInfoToMealMapper
import com.offlinelabs.nutcracker.ui.components.dialogs.AddMealDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.CheckInMealDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.SelectMealForCheckInDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.GoogleCodeScannerDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.FoodInfoDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.FoodSearchDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.ServingSizeDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.AddExerciseDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.EnhancedSelectExerciseDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.SetGoalDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.UnifiedCheckInDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.UnifiedExerciseDetailsDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.UnifiedMealDetailsDialog
import com.offlinelabs.nutcracker.ui.components.dialogs.EditMealDialog
import com.offlinelabs.nutcracker.data.model.CheckInData
import com.offlinelabs.nutcracker.ui.components.FilterableHistoryView
import com.offlinelabs.nutcracker.ui.components.tutorial.TutorialState
import com.offlinelabs.nutcracker.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import com.offlinelabs.nutcracker.util.logger.AppLogger
import com.offlinelabs.nutcracker.util.logger.logUserAction
import com.offlinelabs.nutcracker.util.logger.safeSuspendExecute
import androidx.compose.ui.geometry.Offset as ComposeOffset
import androidx.compose.ui.unit.Dp as ComposeDp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.foundation.lazy.rememberLazyListState
import com.offlinelabs.nutcracker.ui.components.tutorial.SpotlightOverlay
import com.offlinelabs.nutcracker.ui.components.tutorial.TutorialStep
import android.content.Context

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
    sizeDp: ComposeDp = 160.dp,
    strokeWidthDp: ComposeDp = 12.dp
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
            val topLeft = ComposeOffset(
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
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    isDarkTheme: Boolean,
    settingsManager: com.offlinelabs.nutcracker.data.SettingsManager? = null,
    shouldShowTutorial: Boolean = false,
    onTutorialCompleted: () -> Unit = {}
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
    var refreshCheckIns by remember { mutableStateOf(0) }
    var dailyTotalsConsumed by remember { mutableStateOf<DailyTotals?>(null) }
    var consumedCalories by remember { mutableDoubleStateOf(0.0) }
    var foodCalories by remember { mutableDoubleStateOf(0.0) }
    var exerciseCaloriesBurned by remember { mutableDoubleStateOf(0.0) }
    var tefCaloriesBurned by remember { mutableDoubleStateOf(0.0) }
    var includeExerciseCalories by remember { mutableStateOf(true) }
    var includeTEFBonus by remember { mutableStateOf(false) }
    
    // Pill tracking state
    var pills by remember { mutableStateOf(emptyList<Pill>()) }
    var pillCheckInsMap by remember { mutableStateOf<Map<Long, PillCheckIn>>(emptyMap()) }
    var refreshPillCheckIns by remember { mutableStateOf(0) }

    var showSetGoalDialog by remember { mutableStateOf(false) }
    var showAddMealDialog by remember { mutableStateOf(false) }
    var showSelectMealDialog by remember { mutableStateOf(false) }
    var showBarcodeScanDialog by remember { mutableStateOf(false) }
    var showFoodSearchDialog by remember { mutableStateOf(false) }
    var showFoodInfoDialog by remember { 
        mutableStateOf<FoodInfo?>(null).also { 
            AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: showFoodInfoDialog initialized to null")
        }
    }
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
    var showEditPillDialog by remember { mutableStateOf<Pill?>(null) }
    var showAddPillDialog by remember { mutableStateOf(false) }
    
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

    LaunchedEffect(key1 = foodLogRepository, key2 = selectedDateString, key3 = refreshCheckIns) {
        AppLogger.d("DashboardScreen", "🔍 LAUNCHED EFFECT: CheckIns LaunchedEffect triggered")
        AppLogger.d("DashboardScreen", "🔍 LAUNCHED EFFECT: refreshCheckIns = $refreshCheckIns")
        AppLogger.d("DashboardScreen", "🔍 LAUNCHED EFFECT: showFoodInfoDialog = $showFoodInfoDialog")
        AppLogger.d("DashboardScreen", "🔍 LAUNCHED EFFECT: showServingSizeDialog = $showServingSizeDialog")
        AppLogger.d("DashboardScreen", "🔍 LAUNCHED EFFECT: About to start collectLatest")
        foodLogRepository.getCheckInsByDate(selectedDateString).collectLatest { checkInsList ->
            AppLogger.d("DashboardScreen", "🔍 LAUNCHED EFFECT: CheckIns data updated, count: ${checkInsList.size}")
            AppLogger.d("DashboardScreen", "🔍 LAUNCHED EFFECT: About to update dailyMealCheckIns")
            dailyMealCheckIns = checkInsList
            AppLogger.d("DashboardScreen", "🔍 LAUNCHED EFFECT: dailyMealCheckIns updated")
        }
    }

    LaunchedEffect(key1 = foodLogRepository, key2 = selectedDateString) {
        foodLogRepository.getExerciseLogsByDate(selectedDateString).collectLatest { exerciseLogsList ->
            dailyExerciseLogs = exerciseLogsList
        }
    }

    // Check for pill check-ins on selected date for all pills
    LaunchedEffect(foodLogRepository, selectedDateString, pills, refreshPillCheckIns) {
        if (pills.isNotEmpty()) {
            // Load check-ins for all pills using combine
            val flows = pills.map { pill ->
                foodLogRepository.getPillCheckInByPillIdAndDate(pill.id, selectedDateString)
            }
            if (flows.isNotEmpty()) {
                combine(flows) { checkIns ->
                    val checkInsMap = mutableMapOf<Long, PillCheckIn>()
                    pills.forEachIndexed { index, pill ->
                        val checkIn = checkIns[index] as? PillCheckIn?
                        if (checkIn != null) {
                            checkInsMap[pill.id] = checkIn
                        }
                    }
                    checkInsMap.toMap()
                }.collectLatest { checkIns ->
                    pillCheckInsMap = checkIns
                }
            } else {
                pillCheckInsMap = emptyMap()
            }
        } else {
            pillCheckInsMap = emptyMap()
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
                com.offlinelabs.nutcracker.utils.TEFCalculator.calculateTEFBonus(it)
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

    // Use theme-aware colors for consistent theming
    val gradientStartColor = appBackgroundColor()
    val gradientEndColor = appSurfaceVariantColor()
    val innerContainerBackgroundColor = appSurfaceColor()

    val caloriesRemainingLabelColor = appTextSecondaryColor()
    val caloriesRemainingValueColor = appTextPrimaryColor()
    val caloriesConsumedColor = appTextPrimaryColor()
    val caloriesGoalColor = appTextSecondaryColor()

    val view = LocalView.current
    val density = LocalDensity.current
    
    // Calculate status bar height
    val statusBarHeight = with(density) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsCompat.Type.statusBars().let { insets ->
                view.rootWindowInsets?.getInsets(insets)?.top ?: 0
            }.toDp()
        } else {
            0.dp
        }
    }

    // Pill toggle function for specific pill
    val onPillToggle: (Long) -> Unit = { pillId ->
        coroutineScope.launch {
            try {
                val checkIn = pillCheckInsMap[pillId]
                val today = java.time.LocalDate.now()
                
                if (checkIn == null) {
                    // Create new pill check-in
                    // If today: use current time, if another date: use 00:00
                    val timestamp = if (selectedDate == today) {
                        java.time.LocalDateTime.now()
                    } else {
                        java.time.LocalDateTime.of(selectedDate, java.time.LocalTime.of(0, 0))
                    }
                    
                    val newCheckIn = PillCheckIn(
                        pillId = pillId,
                        timestamp = timestamp
                    )
                    foodLogRepository.insertPillCheckIn(newCheckIn)
                    // Optimistically update the state immediately
                    pillCheckInsMap = pillCheckInsMap + (pillId to newCheckIn)
                    // Trigger refresh to ensure Flow updates
                    refreshPillCheckIns++
                    snackbarHostState.showSnackbar(
                        message = dailySupplementTaken
                    )
                } else {
                    // Delete existing pill check-in
                    foodLogRepository.deletePillCheckInByPillIdAndDate(pillId, selectedDateString)
                    // Optimistically update the state immediately
                    val updatedMap = pillCheckInsMap.toMutableMap()
                    updatedMap.remove(pillId)
                    pillCheckInsMap = updatedMap.toMap()
                    // Trigger refresh to ensure Flow updates
                    refreshPillCheckIns++
                    snackbarHostState.showSnackbar(
                        message = dailySupplementRemoved
                    )
                }
            } catch (e: Exception) {
                AppLogger.exception("DashboardScreen", "Failed to update pill status", e, mapOf(
                    "selectedDate" to selectedDateString,
                    "pillId" to pillId
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

    // Tutorial state management
    val tutorialState = remember { TutorialState() }
    var elementCoordinates by remember { mutableStateOf<Map<String, Pair<ComposeOffset, ComposeDp>>>(emptyMap()) }
    var showHistorySpotlight by remember { mutableStateOf(false) }
    
    // Function to register element coordinates
    val registerElementCoordinates: (String, ComposeOffset, ComposeDp) -> Unit = { id, offset, radius ->
        elementCoordinates = elementCoordinates + (id to (offset to radius))
    }
    
    // Start tutorial if needed
    LaunchedEffect(shouldShowTutorial, settingsManager?.hasCompletedTutorial()) {
        if (settingsManager != null) {
            val hasCompleted = settingsManager.hasCompletedTutorial()
            val shouldStartTutorial = shouldShowTutorial || !hasCompleted
            if (shouldStartTutorial) {
                val steps = createTutorialSteps(elementCoordinates, context)
                tutorialState.startTutorial(steps)
            }
        }
    }
    
    // Update current step with coordinates
    LaunchedEffect(elementCoordinates, tutorialState.currentStepIndex, showHistorySpotlight) {
        val currentStep = tutorialState.getCurrentStep()
        if (currentStep != null && elementCoordinates.isNotEmpty()) {
            val stepId = getStepIdFromIndex(tutorialState.currentStepIndex)
            val coordinates = elementCoordinates[stepId]
            
            // For history step, only update if showHistorySpotlight is true
            val shouldUpdate = if (stepId == "check_in_history") {
                showHistorySpotlight && coordinates != null
            } else {
                coordinates != null
            }
            
            if (shouldUpdate && coordinates != null) {
                val updatedStep = currentStep.copy(
                    targetOffset = coordinates.first,
                    targetRadius = coordinates.second
                )
                tutorialState.steps[tutorialState.currentStepIndex] = updatedStep
            }
        }
    }
    
    // Scroll to check-in history section when that tutorial step becomes active
    val lazyListState = rememberLazyListState()
    LaunchedEffect(tutorialState.currentStepIndex) {
        if (tutorialState.currentStepIndex == 8) {
            // Step 8 is the check-in history step
            showHistorySpotlight = false // Hide spotlight during scroll
            
            // Wait for layout to be ready
            kotlinx.coroutines.delay(100)
            
            // Scroll to the history section - it's the last item in the LazyColumn
            if (lazyListState.layoutInfo.totalItemsCount > 0) {
                lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
            }
            
            // Wait for scroll animation to complete and layout to settle
            kotlinx.coroutines.delay(500)
            
            // Now show the spotlight
            showHistorySpotlight = true
        } else {
            showHistorySpotlight = true // For all other steps, show immediately
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
                    // Left side - Help and Calendar icons with fixed spacing
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Help icon
                        IconButton(
                            onClick = onNavigateToHelp,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Help,
                                contentDescription = "Help",
                                tint = appTextSecondaryColor(),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        // Calendar icon
                        IconButton(
                            onClick = { showCalendarDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                    val center = coordinates.boundsInWindow().center
                                    registerElementCoordinates("calendar_icon", center, 25.dp)
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = stringResource(R.string.select_date),
                                tint = appTextSecondaryColor(),
                                modifier = Modifier.size(22.dp)
                            )
                        }
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
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.previous_day),
                                tint = appTextSecondaryColor(),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Date display
                        val today = java.time.LocalDate.now()
                        val dateText = if (selectedDate == today) {
                            stringResource(R.string.today)
                        } else {
                            // Custom formatter with 2-digit year
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy")
                            selectedDate.format(formatter)
                        }
                        Text(
                            text = dateText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = appTextPrimaryColor(),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(80.dp)
                        )
                        
                        // Right arrow
                        IconButton(
                            onClick = { 
                                selectedDate = selectedDate.plusDays(1)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.next_day),
                                tint = appTextSecondaryColor(),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Right side - Settings and Analytics icons with fixed spacing
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Settings button
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(44.dp)
                                .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                    val center = coordinates.boundsInWindow().center
                                    registerElementCoordinates("settings_icon", center, 25.dp)
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.settings_icon_description),
                                tint = appTextSecondaryColor(),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        // Progress bar button
                        IconButton(
                            onClick = onNavigateToAnalytics,
                            modifier = Modifier
                                .size(44.dp)
                                .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                    val center = coordinates.boundsInWindow().center
                                    registerElementCoordinates("analytics_icon", center, 25.dp)
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BarChart,
                                contentDescription = stringResource(R.string.progress_details),
                                tint = appTextSecondaryColor(),
                                modifier = Modifier.size(22.dp)
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
                    ),
                    modifier = Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        val center = coordinates.boundsInWindow().center
                        val radius = with(density) { 30.dp.toPx() }
                        registerElementCoordinates("add_exercise_fab", center, 30.dp)
                    }
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
                    ),
                    modifier = Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        val center = coordinates.boundsInWindow().center
                        val radius = with(density) { 30.dp.toPx() }
                        registerElementCoordinates("add_meal_fab", center, 30.dp)
                    }
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
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        // Calories ring section with symmetrical toggles
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                    val center = coordinates.boundsInWindow().center
                                    registerElementCoordinates("calorie_ring", center, 60.dp)
                                },
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
                                    .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                        val center = coordinates.boundsInWindow().center
                                        registerElementCoordinates("edit_goals_icon", center, 25.dp)
                                    }
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                    val bounds = coordinates.boundsInWindow()
                                    val center = bounds.center
                                    // Calculate radius from actual bounds to cover entire pill tracker section
                                    val height = bounds.height
                                    val width = bounds.width
                                    val radius = with(density) { 
                                        // Use the larger dimension to ensure full coverage, with padding
                                        (maxOf(height, width) / 2f + 8.dp.toPx()).toDp()
                                    }
                                    registerElementCoordinates("supplement_pill", center, radius)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            MultiPillTracker(
                                pills = pills,
                                pillCheckIns = pillCheckInsMap,
                                onPillToggle = onPillToggle,
                                onPillLongPress = { pill ->
                                    showEditPillDialog = pill
                                },
                                onAddPill = {
                                    if (pills.size < 5) {
                                        showAddPillDialog = true
                                    }
                                }
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                    val bounds = coordinates.boundsInWindow()
                                    val center = bounds.center
                                    // Calculate radius based on the height of the section
                                    val height = bounds.height
                                    val radius = with(density) { (height / 2f + 16.dp.toPx()).toDp() }
                                    registerElementCoordinates("check_in_history", center, radius)
                                }
                        ) {
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
            },
            registerElementCoordinates = registerElementCoordinates
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
                            AppLogger.i("DashboardScreen", "=== START onAddExercise ===")
                            AppLogger.i("DashboardScreen", "External exercise name: ${externalExercise?.name}")
                            AppLogger.i("DashboardScreen", "External exercise has images: ${externalExercise?.images?.isNotEmpty()}")
                            AppLogger.i("DashboardScreen", "External exercise image count: ${externalExercise?.images?.size}")
                            
                            // Insert exercise first to get an ID
                            AppLogger.i("DashboardScreen", "Inserting newExercise with name: ${newExercise.name}")
                            AppLogger.i("DashboardScreen", "newExercise.imagePaths before insert: ${newExercise.imagePaths}")
                            val exerciseId = foodLogRepository.insertExercise(newExercise)
                            AppLogger.i("DashboardScreen", "Exercise inserted with ID: $exerciseId")
                            
                            // Download and save all images if available, and update with external exercise data
                            externalExercise?.let { exercise ->
                                AppLogger.i("DashboardScreen", "Processing external exercise data...")
                                
                                val localImagePaths = if (exercise.images.isNotEmpty()) {
                                    AppLogger.i("DashboardScreen", "External exercise has ${exercise.images.size} images")
                                    val imageUrls = exercise.images.map { externalExerciseService.getImageUrl(it) }
                                    AppLogger.i("DashboardScreen", "Full image URLs:")
                                    imageUrls.forEachIndexed { index, url ->
                                        AppLogger.i("DashboardScreen", "  URL $index: $url")
                                    }
                                    
                                    AppLogger.i("DashboardScreen", "Calling downloadAndStoreImages with exerciseId: $exerciseId")
                                    // Download all images
                                    val paths = exerciseImageService.downloadAndStoreImages(imageUrls, exerciseId.toString())
                                    
                                    AppLogger.i("DashboardScreen", "downloadAndStoreImages returned ${paths.size} paths")
                                    paths.forEachIndexed { index, path ->
                                        AppLogger.i("DashboardScreen", "  Path $index: $path")
                                    }
                                    
                                    if (paths.isNotEmpty()) {
                                        AppLogger.i("DashboardScreen", "✓ Downloaded ${paths.size} images successfully")
                                    } else {
                                        AppLogger.w("DashboardScreen", "✗ Failed to download any images for exercise ID: $exerciseId")
                                    }
                                    paths
                                } else {
                                    AppLogger.i("DashboardScreen", "No images in external exercise")
                                    emptyList()
                                }
                                
                                AppLogger.i("DashboardScreen", "Creating completeExercise with ${localImagePaths.size} image paths")
                                
                                // ALWAYS update the exercise with external data, even if image downloads failed
                                // This ensures we preserve instructions, equipment, muscles, etc.
                                val completeExercise = exercise.toInternalExercise(localImagePaths).copy(
                                    id = exerciseId,
                                    kcalBurnedPerUnit = newExercise.kcalBurnedPerUnit,
                                    defaultWeight = newExercise.defaultWeight,
                                    defaultReps = newExercise.defaultReps,
                                    defaultSets = newExercise.defaultSets,
                                    notes = newExercise.notes // Keep user's personal notes separate from instructions
                                )
                                
                                AppLogger.i("DashboardScreen", "completeExercise created:")
                                AppLogger.i("DashboardScreen", "  - ID: ${completeExercise.id}")
                                AppLogger.i("DashboardScreen", "  - Name: ${completeExercise.name}")
                                AppLogger.i("DashboardScreen", "  - imagePaths.size: ${completeExercise.imagePaths.size}")
                                completeExercise.imagePaths.forEachIndexed { index, path ->
                                    AppLogger.i("DashboardScreen", "  - imagePath $index: $path")
                                }
                                AppLogger.i("DashboardScreen", "  - equipment: ${completeExercise.equipment}")
                                AppLogger.i("DashboardScreen", "  - primaryMuscles: ${completeExercise.primaryMuscles}")
                                AppLogger.i("DashboardScreen", "  - instructions.size: ${completeExercise.instructions.size}")
                                
                                // Update the exercise with all the data
                                AppLogger.i("DashboardScreen", "Calling updateExercise...")
                                foodLogRepository.updateExercise(completeExercise)
                                AppLogger.i("DashboardScreen", "✓ updateExercise completed")
                                AppLogger.i("DashboardScreen", "=== END onAddExercise ===")
                            } ?: run {
                                AppLogger.w("DashboardScreen", "No external exercise data for exercise ID: $exerciseId")
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
            } else null,
            onSaveAndCheckIn = if (selectedExternalExercise != null && selectedExerciseForEdit == null) {
                { exercise ->
                    // Capture the external exercise reference before the coroutine
                    val capturedExternalExercise = selectedExternalExercise
                    
                    coroutineScope.launch {
                        try {
                            AppLogger.i("DashboardScreen", "=== START onSaveAndCheckIn ===")
                            AppLogger.i("DashboardScreen", "Exercise name: ${exercise.name}")
                            AppLogger.i("DashboardScreen", "capturedExternalExercise: ${capturedExternalExercise?.name}")
                            AppLogger.i("DashboardScreen", "capturedExternalExercise has images: ${capturedExternalExercise?.images?.isNotEmpty()}")
                            
                            // Save exercise with isVisible = true
                            val exerciseId = foodLogRepository.insertExercise(exercise.copy(isVisible = true))
                            AppLogger.i("DashboardScreen", "Exercise inserted with ID: $exerciseId")
                            
                            // Download images if available and update with external exercise data
                            capturedExternalExercise?.let { externalExercise ->
                                AppLogger.i("DashboardScreen", "Processing external exercise: ${externalExercise.name}")
                                AppLogger.i("DashboardScreen", "External exercise image count: ${externalExercise.images.size}")
                                
                                val localImagePaths = if (externalExercise.images.isNotEmpty()) {
                                    AppLogger.i("DashboardScreen", "Calling downloadAndStoreImages...")
                                    val imageUrls = externalExercise.images.map { externalExerciseService.getImageUrl(it) }
                                    imageUrls.forEachIndexed { index, url ->
                                        AppLogger.i("DashboardScreen", "  Image URL $index: $url")
                                    }
                                    val paths = exerciseImageService.downloadAndStoreImages(imageUrls, exerciseId.toString())
                                    AppLogger.i("DashboardScreen", "Downloaded ${paths.size} images")
                                    paths
                                } else {
                                    AppLogger.i("DashboardScreen", "No images to download")
                                    emptyList()
                                }
                                
                                AppLogger.i("DashboardScreen", "Creating completeExercise with ${localImagePaths.size} image paths")
                                // ALWAYS update the exercise with external data, even if image downloads failed
                                val completeExercise = externalExercise.toInternalExercise(localImagePaths).copy(
                                    id = exerciseId,
                                    kcalBurnedPerUnit = exercise.kcalBurnedPerUnit,
                                    defaultWeight = exercise.defaultWeight,
                                    defaultReps = exercise.defaultReps,
                                    defaultSets = exercise.defaultSets,
                                    notes = exercise.notes,
                                    isVisible = true
                                )
                                AppLogger.i("DashboardScreen", "Calling updateExercise...")
                                foodLogRepository.updateExercise(completeExercise)
                                AppLogger.i("DashboardScreen", "✓ updateExercise completed")
                            } ?: AppLogger.w("DashboardScreen", "capturedExternalExercise is null!")
                            
                            // Create check-in with appropriate values based on exercise type
                            val exerciseType = ExerciseCategoryMapper.getExerciseType(exercise.category)
                            val (weight, reps, sets) = when (exerciseType) {
                                ExerciseType.STRENGTH -> Triple(exercise.defaultWeight, exercise.defaultReps, exercise.defaultSets)
                                ExerciseType.CARDIO -> Triple(0.0, exercise.defaultReps, 1) // weight=0, reps=minutes, sets=1
                                ExerciseType.BODYWEIGHT -> Triple(0.0, exercise.defaultReps, 1) // weight=0, reps=reps, sets=1
                            }
                            
                            // Calculate calories burned based on exercise type
                            val caloriesBurned = when (exerciseType) {
                                ExerciseType.STRENGTH -> {
                                    val kcalPerSet = exercise.kcalBurnedPerUnit ?: 0.0
                                    sets * kcalPerSet
                                }
                                ExerciseType.CARDIO -> {
                                    val kcalPerMinute = exercise.kcalBurnedPerUnit ?: 0.0
                                    reps * kcalPerMinute // reps = minutes for cardio
                                }
                                ExerciseType.BODYWEIGHT -> {
                                    val kcalPerRep = exercise.kcalBurnedPerUnit ?: 0.0
                                    reps * kcalPerRep
                                }
                            }
                            
                            val exerciseLog = ExerciseLog.create(
                                exerciseId = exerciseId,
                                weight = weight,
                                reps = reps,
                                sets = sets,
                                caloriesBurned = caloriesBurned,
                                notes = null
                            )
                            foodLogRepository.insertExerciseLog(exerciseLog)
                            
                            snackbarHostState.showSnackbar(message = "Exercise saved and check-in completed!")
                            refreshCheckIns++
                        } catch (e: Exception) {
                            AppLogger.exception("DashboardScreen", "Failed to save and check-in exercise", e)
                            snackbarHostState.showSnackbar(message = "Failed to complete action: ${e.message}")
                        }
                    }
                    showAddExerciseDialog = false
                    selectedExternalExercise = null
                }
            } else null,
            onJustSave = if (selectedExternalExercise != null && selectedExerciseForEdit == null) {
                { exercise ->
                    // Capture the external exercise reference before the coroutine
                    val capturedExternalExercise = selectedExternalExercise
                    
                    coroutineScope.launch {
                        try {
                            AppLogger.i("DashboardScreen", "=== START onJustSave ===")
                            AppLogger.i("DashboardScreen", "Exercise name: ${exercise.name}")
                            AppLogger.i("DashboardScreen", "capturedExternalExercise: ${capturedExternalExercise?.name}")
                            AppLogger.i("DashboardScreen", "capturedExternalExercise has images: ${capturedExternalExercise?.images?.isNotEmpty()}")
                            
                            // Save exercise with isVisible = true
                            val exerciseId = foodLogRepository.insertExercise(exercise.copy(isVisible = true))
                            AppLogger.i("DashboardScreen", "Exercise inserted with ID: $exerciseId")
                            
                            // Download images if available and update with external exercise data
                            capturedExternalExercise?.let { externalExercise ->
                                AppLogger.i("DashboardScreen", "Processing external exercise: ${externalExercise.name}")
                                AppLogger.i("DashboardScreen", "External exercise image count: ${externalExercise.images.size}")
                                
                                val localImagePaths = if (externalExercise.images.isNotEmpty()) {
                                    AppLogger.i("DashboardScreen", "Calling downloadAndStoreImages...")
                                    val imageUrls = externalExercise.images.map { externalExerciseService.getImageUrl(it) }
                                    imageUrls.forEachIndexed { index, url ->
                                        AppLogger.i("DashboardScreen", "  Image URL $index: $url")
                                    }
                                    val paths = exerciseImageService.downloadAndStoreImages(imageUrls, exerciseId.toString())
                                    AppLogger.i("DashboardScreen", "Downloaded ${paths.size} images")
                                    paths
                                } else {
                                    AppLogger.i("DashboardScreen", "No images to download")
                                    emptyList()
                                }
                                
                                AppLogger.i("DashboardScreen", "Creating completeExercise with ${localImagePaths.size} image paths")
                                // ALWAYS update the exercise with external data, even if image downloads failed
                                val completeExercise = externalExercise.toInternalExercise(localImagePaths).copy(
                                    id = exerciseId,
                                    kcalBurnedPerUnit = exercise.kcalBurnedPerUnit,
                                    defaultWeight = exercise.defaultWeight,
                                    defaultReps = exercise.defaultReps,
                                    defaultSets = exercise.defaultSets,
                                    notes = exercise.notes,
                                    isVisible = true
                                )
                                AppLogger.i("DashboardScreen", "Calling updateExercise...")
                                foodLogRepository.updateExercise(completeExercise)
                                AppLogger.i("DashboardScreen", "✓ updateExercise completed")
                            } ?: AppLogger.w("DashboardScreen", "capturedExternalExercise is null!")
                            
                            snackbarHostState.showSnackbar(message = "Exercise saved successfully!")
                        } catch (e: Exception) {
                            AppLogger.exception("DashboardScreen", "Failed to save exercise", e)
                            snackbarHostState.showSnackbar(message = "Failed to save exercise: ${e.message}")
                        }
                    }
                    showAddExerciseDialog = false
                    selectedExternalExercise = null
                }
            } else null,
            onJustCheckIn = if (selectedExternalExercise != null && selectedExerciseForEdit == null) {
                { exercise ->
                    // Capture the external exercise reference before the coroutine
                    val capturedExternalExercise = selectedExternalExercise
                    
                    coroutineScope.launch {
                        try {
                            // Save exercise with isVisible = false
                            val exerciseId = foodLogRepository.insertExercise(exercise.copy(isVisible = false))
                            
                            // Download images if available and update with external exercise data
                            capturedExternalExercise?.let { externalExercise ->
                                val localImagePaths = if (externalExercise.images.isNotEmpty()) {
                                    val imageUrls = externalExercise.images.map { externalExerciseService.getImageUrl(it) }
                                    exerciseImageService.downloadAndStoreImages(imageUrls, exerciseId.toString())
                                } else {
                                    emptyList()
                                }
                                
                                // ALWAYS update the exercise with external data, even if image downloads failed
                                val completeExercise = externalExercise.toInternalExercise(localImagePaths).copy(
                                    id = exerciseId,
                                    kcalBurnedPerUnit = exercise.kcalBurnedPerUnit,
                                    defaultWeight = exercise.defaultWeight,
                                    defaultReps = exercise.defaultReps,
                                    defaultSets = exercise.defaultSets,
                                    notes = exercise.notes,
                                    isVisible = false
                                )
                                foodLogRepository.updateExercise(completeExercise)
                            }
                            
                            // Create check-in with appropriate values based on exercise type
                            val exerciseType = ExerciseCategoryMapper.getExerciseType(exercise.category)
                            val (weight, reps, sets) = when (exerciseType) {
                                ExerciseType.STRENGTH -> Triple(exercise.defaultWeight, exercise.defaultReps, exercise.defaultSets)
                                ExerciseType.CARDIO -> Triple(0.0, exercise.defaultReps, 1) // weight=0, reps=minutes, sets=1
                                ExerciseType.BODYWEIGHT -> Triple(0.0, exercise.defaultReps, 1) // weight=0, reps=reps, sets=1
                            }
                            
                            // Calculate calories burned based on exercise type
                            val caloriesBurned = when (exerciseType) {
                                ExerciseType.STRENGTH -> {
                                    val kcalPerSet = exercise.kcalBurnedPerUnit ?: 0.0
                                    sets * kcalPerSet
                                }
                                ExerciseType.CARDIO -> {
                                    val kcalPerMinute = exercise.kcalBurnedPerUnit ?: 0.0
                                    reps * kcalPerMinute // reps = minutes for cardio
                                }
                                ExerciseType.BODYWEIGHT -> {
                                    val kcalPerRep = exercise.kcalBurnedPerUnit ?: 0.0
                                    reps * kcalPerRep
                                }
                            }
                            
                            val exerciseLog = ExerciseLog.create(
                                exerciseId = exerciseId,
                                weight = weight,
                                reps = reps,
                                sets = sets,
                                caloriesBurned = caloriesBurned,
                                notes = null
                            )
                            foodLogRepository.insertExerciseLog(exerciseLog)
                            
                            snackbarHostState.showSnackbar(message = "Check-in completed!")
                            refreshCheckIns++
                        } catch (e: Exception) {
                            AppLogger.exception("DashboardScreen", "Failed to check-in exercise", e)
                            snackbarHostState.showSnackbar(message = "Failed to check-in: ${e.message}")
                        }
                    }
                    showAddExerciseDialog = false
                    selectedExternalExercise = null
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
                    Text(okText, color = brandPrimaryColor())
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCalendarDialog = false }
                ) {
                    Text(cancelText, color = appTextSecondaryColor())
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = appSurfaceColor(),
                titleContentColor = appTextPrimaryColor(),
                headlineContentColor = appTextPrimaryColor(),
                weekdayContentColor = appTextSecondaryColor(),
                subheadContentColor = appTextPrimaryColor(),
                yearContentColor = appTextPrimaryColor(),
                currentYearContentColor = brandPrimaryColor(),
                selectedYearContentColor = appTextInverseColor(),
                selectedYearContainerColor = brandPrimaryColor(),
                dayContentColor = appTextPrimaryColor(),
                selectedDayContentColor = appTextInverseColor(),
                selectedDayContainerColor = brandPrimaryColor(),
                todayContentColor = brandPrimaryColor(),
                todayDateBorderColor = brandPrimaryColor()
            )
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    
    // Edit Pill Dialog
    showEditPillDialog?.let { pill ->
        var pillName by remember { mutableStateOf(pill.name) }
        var showDeleteConfirm by remember { mutableStateOf(false) }
        val dialogTextFieldColors = dialogOutlinedTextFieldColorsMaxContrast()
        
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = {
                    Text(stringResource(R.string.delete))
                },
                text = {
                    Text("Are you sure you want to delete this pill? All check-in records will be removed.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    // Delete all check-ins for this pill first
                                    val checkIns = foodLogRepository.getPillCheckInsByPillId(pill.id).first()
                                    checkIns.forEach { checkIn ->
                                        foodLogRepository.deletePillCheckIn(checkIn)
                                    }
                                    // Then delete the pill
                                    foodLogRepository.deletePill(pill)
                                    snackbarHostState.showSnackbar(
                                        message = "Pill deleted successfully"
                                    )
                                } catch (e: Exception) {
                                    AppLogger.exception("DashboardScreen", "Failed to delete pill", e, mapOf(
                                        "pillId" to pill.id.toString()
                                    ))
                                    snackbarHostState.showSnackbar(
                                        message = "Failed to delete pill: ${e.message}"
                                    )
                                }
                            }
                            showDeleteConfirm = false
                            showEditPillDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showEditPillDialog = null },
                title = {
                    Text(stringResource(R.string.edit_pill))
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = pillName,
                            onValueChange = { pillName = it },
                            label = { Text(stringResource(R.string.pill_name)) },
                            placeholder = { Text(stringResource(R.string.pill_name_placeholder)) },
                            colors = dialogTextFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pillName.isNotBlank()) {
                                coroutineScope.launch {
                                    try {
                                        val updatedPill = pill.copy(name = pillName.trim())
                                        foodLogRepository.updatePill(updatedPill)
                                        snackbarHostState.showSnackbar(
                                            message = "Pill updated successfully"
                                        )
                                    } catch (e: Exception) {
                                        AppLogger.exception("DashboardScreen", "Failed to update pill", e, mapOf(
                                            "pillId" to pill.id.toString()
                                        ))
                                        snackbarHostState.showSnackbar(
                                            message = "Failed to update pill: ${e.message}"
                                        )
                                    }
                                }
                                showEditPillDialog = null
                            }
                        },
                        enabled = pillName.isNotBlank() && pillName.trim() != pill.name
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    Row {
                        // Only show delete button if pill ID is not 1 (default pill)
                        if (pill.id != 1L) {
                            TextButton(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(stringResource(R.string.delete))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        TextButton(onClick = { showEditPillDialog = null }) {
                            Text(
                                text = stringResource(R.string.cancel),
                                color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                            )
                        }
                    }
                }
            )
        }
    }
    
    // Add Pill Dialog
    if (showAddPillDialog) {
        var pillName by remember { mutableStateOf("") }
        val dialogTextFieldColors = dialogOutlinedTextFieldColorsMaxContrast()
        
        AlertDialog(
            onDismissRequest = { showAddPillDialog = false },
            title = {
                Text(stringResource(R.string.add_pill))
            },
            text = {
                OutlinedTextField(
                    value = pillName,
                    onValueChange = { pillName = it },
                    label = { Text(stringResource(R.string.pill_name)) },
                    placeholder = { Text(stringResource(R.string.pill_name_placeholder)) },
                    colors = dialogTextFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pillName.isNotBlank() && pills.size < 5) {
                            coroutineScope.launch {
                                try {
                                    foodLogRepository.insertPill(Pill(name = pillName.trim()))
                                    snackbarHostState.showSnackbar(
                                        message = "Pill added successfully"
                                    )
                                } catch (e: Exception) {
                                    AppLogger.exception("DashboardScreen", "Failed to add pill", e)
                                    snackbarHostState.showSnackbar(
                                        message = "Failed to add pill: ${e.message}"
                                    )
                                }
                            }
                            showAddPillDialog = false
                        }
                    },
                    enabled = pillName.isNotBlank() && pills.size < 5
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPillDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        )
    }

    showEditMealDialog?.let { checkIn ->
        // Get the meal directly from database (including hidden meals)
        var meal by remember { mutableStateOf<Meal?>(null) }
        
        LaunchedEffect(checkIn.mealId) {
            foodLogRepository.getMealById(checkIn.mealId).collectLatest { foundMeal ->
                meal = foundMeal
            }
        }
        
        meal?.let { foundMeal ->
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
                meal = foundMeal,
                existingMealCheckIn = existingMealCheckIn
            )
        }
    }
    
    // Edit Exercise Dialog
    showEditExerciseDialog?.let { exerciseEntry ->
        // Get the exercise directly from database (including hidden exercises)
        var exercise by remember { mutableStateOf<Exercise?>(null) }
        
        LaunchedEffect(exerciseEntry.exerciseId) {
            foodLogRepository.getExerciseById(exerciseEntry.exerciseId).collectLatest { foundExercise ->
                exercise = foundExercise
            }
        }
        
        exercise?.let { foundExercise ->
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
                exercise = foundExercise,
                existingExerciseLog = existingExerciseLog,
                isEditMode = true,
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
                }
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
                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: FoodSearchDialog setting showFoodInfoDialog")
                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Before setting - showFoodInfoDialog = $showFoodInfoDialog")
                    showFoodInfoDialog = foodInfo
                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: After setting - showFoodInfoDialog = $showFoodInfoDialog")
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Unable to process food information"
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
                                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: BarcodeScanDialog setting showFoodInfoDialog")
                                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Before setting - showFoodInfoDialog = $showFoodInfoDialog")
                                    AppLogger.d("DashboardScreen", "FoodInfo created successfully, showing FoodInfoDialog")
                                    showFoodInfoDialog = foodInfo
                                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: After setting - showFoodInfoDialog = $showFoodInfoDialog")
                                } else {
                                    AppLogger.d("DashboardScreen", "FoodInfo is null, showing error message")
                                    snackbarHostState.showSnackbar(
                                        message = "Food information not found for this barcode"
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
                            message = "An error occurred while fetching food information"
                        )
                    }
                }
                showBarcodeScanDialog = false
            }
        )
    }

    // Food Info Dialog - only show if serving size dialog is not already showing
    showFoodInfoDialog?.let { foodInfo ->
        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: FoodInfoDialog?.let triggered - foodInfo = ${foodInfo.name}")
        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: FoodInfo object hash = ${foodInfo.hashCode()}")
        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: showServingSizeDialog check = $showServingSizeDialog")
        if (showServingSizeDialog == null) {
            AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Showing FoodInfoDialog for: ${foodInfo.name}")
            AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: showFoodInfoDialog = $showFoodInfoDialog")
            AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: showServingSizeDialog = $showServingSizeDialog")
            AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: currentBarcode = $currentBarcode")
            FoodInfoDialog(
                foodInfo = foodInfo,
                onBack = { 
                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: FoodInfoDialog dismissed via back button")
                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Setting showFoodInfoDialog = null")
                    showFoodInfoDialog = null
                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: After back - showFoodInfoDialog = $showFoodInfoDialog")
                    // Don't reset currentBarcode here - it should persist through the flow
                },
                onSelect = {
                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Transitioning from FoodInfoDialog to ServingSizeDialog")
                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Before transition - showFoodInfoDialog = $showFoodInfoDialog, showServingSizeDialog = $showServingSizeDialog")
                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: currentBarcode: $currentBarcode")
                    showFoodInfoDialog = null
                    showServingSizeDialog = foodInfo
                    AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: After transition - showFoodInfoDialog = $showFoodInfoDialog, showServingSizeDialog = $showServingSizeDialog")
                    // currentBarcode should still be set from the barcode scan
                }
            )
        } else {
            AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: FoodInfoDialog blocked - showServingSizeDialog is not null: $showServingSizeDialog")
        }
    }
    
    // Serving Size Dialog
    showServingSizeDialog?.let { foodInfo ->
        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Showing ServingSizeDialog for: ${foodInfo.name}")
        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: showServingSizeDialog = $showServingSizeDialog")
        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: showFoodInfoDialog = $showFoodInfoDialog")
        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: currentBarcode = $currentBarcode")
        ServingSizeDialog(
            foodInfo = foodInfo,
            barcode = currentBarcode,
            onDismiss = { 
                AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: ServingSizeDialog dismissed")
                AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Before dismiss - showServingSizeDialog = $showServingSizeDialog, showFoodInfoDialog = $showFoodInfoDialog")
                showServingSizeDialog = null
                showFoodInfoDialog = null
                currentBarcode = null
                AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: After dismiss - showServingSizeDialog = $showServingSizeDialog, showFoodInfoDialog = $showFoodInfoDialog")
            },
            onSaveAndCheckIn = { servingSizeValue, servingSizeUnit ->
                val barcodeToUse = currentBarcode
                AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Save and Check-in clicked")
                AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Before clearing - showServingSizeDialog = $showServingSizeDialog, showFoodInfoDialog = $showFoodInfoDialog")
                AppLogger.d("DashboardScreen", "Save and Check-in: Creating meal with currentBarcode: $barcodeToUse")
                
                // Clear all dialog states IMMEDIATELY to prevent race condition
                showServingSizeDialog = null
                showFoodInfoDialog = null
                currentBarcode = null
                AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: After clearing - showServingSizeDialog = $showServingSizeDialog, showFoodInfoDialog = $showFoodInfoDialog")
                
                coroutineScope.launch {
                    try {
                        // Save meal
                        val meal = FoodInfoToMealMapper.mapToMeal(
                            foodInfo = foodInfo,
                            servingSizeValue = servingSizeValue,
                            servingSizeUnit = servingSizeUnit,
                            barcode = barcodeToUse,
                            source = if (barcodeToUse != null) "barcode" else "search",
                            isVisible = true
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
                        val savedMeal = finalMeal.copy(id = newMealId)
                        
                        // Create a check-in for this meal
                        val mealCheckIn = MealCheckIn.create(
                            mealId = savedMeal.id,
                            servingSize = 1.0,
                            notes = null
                        )
                        
                        foodLogRepository.insertMealCheckIn(mealCheckIn)
                        
                        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: About to increment refreshCheckIns")
                        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Before refreshCheckIns++ - showServingSizeDialog = $showServingSizeDialog, showFoodInfoDialog = $showFoodInfoDialog")
                        AppLogger.d("DashboardScreen", "Save and Check-in: Check-in created successfully with mealId: ${savedMeal.id}")
                        snackbarHostState.showSnackbar(message = "Meal saved and check-in completed successfully!")
                        // showUnifiedMealDetailDialog = savedMeal  // Removed to prevent dialog from showing
                        refreshCheckIns++
                        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: After refreshCheckIns++ - showServingSizeDialog = $showServingSizeDialog, showFoodInfoDialog = $showFoodInfoDialog")
                        
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Failed to save and check-in meal", e, mapOf(
                            "foodName" to foodInfo.name
                        ))
                        snackbarHostState.showSnackbar(message = "Failed to complete action: ${e.message}")
                    }
                }
            },
            onJustSave = { servingSizeValue, servingSizeUnit ->
                val barcodeToUse = currentBarcode
                AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Just Save clicked")
                AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Before clearing - showServingSizeDialog = $showServingSizeDialog, showFoodInfoDialog = $showFoodInfoDialog")
                AppLogger.d("DashboardScreen", "Just Save: Creating meal with currentBarcode: $barcodeToUse")
                
                // Clear all dialog states IMMEDIATELY to prevent race condition
                showServingSizeDialog = null
                showFoodInfoDialog = null
                currentBarcode = null
                AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: After clearing - showServingSizeDialog = $showServingSizeDialog, showFoodInfoDialog = $showFoodInfoDialog")
                
                coroutineScope.launch {
                    try {
                        // Save meal
                        val meal = FoodInfoToMealMapper.mapToMeal(
                            foodInfo = foodInfo,
                            servingSizeValue = servingSizeValue,
                            servingSizeUnit = servingSizeUnit,
                            barcode = barcodeToUse,
                            source = if (barcodeToUse != null) "barcode" else "search",
                            isVisible = true
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
                        val savedMeal = finalMeal.copy(id = newMealId)
                        
                        snackbarHostState.showSnackbar(message = "Meal saved successfully!")
                        // showUnifiedMealDetailDialog = savedMeal  // Removed to prevent dialog from showing
                        
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Failed to save meal", e, mapOf(
                            "foodName" to foodInfo.name
                        ))
                        snackbarHostState.showSnackbar(message = "Failed to complete action: ${e.message}")
                    }
                }
            },
            onJustCheckIn = { servingSizeValue, servingSizeUnit ->
                val barcodeToUse = currentBarcode
                AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Just Check-in clicked")
                AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Before clearing - showServingSizeDialog = $showServingSizeDialog, showFoodInfoDialog = $showFoodInfoDialog")
                AppLogger.d("DashboardScreen", "Just Check-in: Creating meal with currentBarcode: $barcodeToUse")
                
                // Clear all dialog states IMMEDIATELY to prevent race condition
                showServingSizeDialog = null
                showFoodInfoDialog = null
                currentBarcode = null
                AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: After clearing - showServingSizeDialog = $showServingSizeDialog, showFoodInfoDialog = $showFoodInfoDialog")
                
                coroutineScope.launch {
                    try {
                        // Create meal for check-in only (not visible in meal list)
                        val meal = FoodInfoToMealMapper.mapToMeal(
                            foodInfo = foodInfo,
                            servingSizeValue = servingSizeValue,
                            servingSizeUnit = servingSizeUnit,
                            barcode = barcodeToUse,
                            source = if (barcodeToUse != null) "barcode" else "search",
                            isVisible = false
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
                        val savedMeal = finalMeal.copy(id = newMealId)
                        
                        // Create a check-in for this meal
                        val mealCheckIn = MealCheckIn.create(
                            mealId = savedMeal.id,
                            servingSize = 1.0,
                            notes = null
                        )
                        
                        foodLogRepository.insertMealCheckIn(mealCheckIn)
                        
                        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: About to increment refreshCheckIns")
                        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: Before refreshCheckIns++ - showServingSizeDialog = $showServingSizeDialog, showFoodInfoDialog = $showFoodInfoDialog")
                        AppLogger.d("DashboardScreen", "Check-in created successfully with mealId: ${savedMeal.id}")
                        snackbarHostState.showSnackbar(message = "Check-in completed successfully!")
                        refreshCheckIns++
                        AppLogger.d("DashboardScreen", "🔍 DIALOG STATE: After refreshCheckIns++ - showServingSizeDialog = $showServingSizeDialog, showFoodInfoDialog = $showFoodInfoDialog")
                        
                    } catch (e: Exception) {
                        AppLogger.exception("DashboardScreen", "Failed to check-in meal", e, mapOf(
                            "foodName" to foodInfo.name
                        ))
                        snackbarHostState.showSnackbar(message = "Failed to complete action: ${e.message}")
                    }
                }
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
                            message = "Failed to load exercise data"
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
    
    // Tutorial overlay
    if (tutorialState.isActive) {
        val currentStep = tutorialState.getCurrentStep()
        SpotlightOverlay(
            step = currentStep,
            onNext = {
                if (currentStep?.showDialog != null) {
                    currentStep.showDialog()
                } else {
                    val wasLastStep = tutorialState.currentStepIndex == tutorialState.steps.size - 1
                    tutorialState.nextStep()
                    if (wasLastStep) {
                        settingsManager?.setTutorialCompleted()
                        onTutorialCompleted()
                    }
                }
            },
            onSkip = {
                tutorialState.skipTutorial()
                settingsManager?.setTutorialCompleted()
                onTutorialCompleted()
            },
            onPrevious = {
                tutorialState.previousStep()
            }
        )
    }
}

private fun createTutorialSteps(elementCoordinates: Map<String, Pair<ComposeOffset, ComposeDp>>, context: Context): List<TutorialStep> {
    return listOf(
        TutorialStep(
            id = "dashboard_overview",
            title = context.getString(R.string.tutorial_welcome_title),
            description = context.getString(R.string.tutorial_welcome_description)
        ),
        TutorialStep(
            id = "calorie_ring",
            title = context.getString(R.string.tutorial_calorie_ring_title),
            description = context.getString(R.string.tutorial_calorie_ring_description),
            targetOffset = elementCoordinates["calorie_ring"]?.first,
            targetRadius = elementCoordinates["calorie_ring"]?.second ?: 90.dp
        ),
        TutorialStep(
            id = "add_meal_fab",
            title = context.getString(R.string.tutorial_add_meal_title),
            description = context.getString(R.string.tutorial_add_meal_description),
            targetOffset = elementCoordinates["add_meal_fab"]?.first,
            targetRadius = elementCoordinates["add_meal_fab"]?.second ?: 30.dp
        ),
        TutorialStep(
            id = "add_exercise_fab",
            title = context.getString(R.string.tutorial_add_exercise_title),
            description = context.getString(R.string.tutorial_add_exercise_description),
            targetOffset = elementCoordinates["add_exercise_fab"]?.first,
            targetRadius = elementCoordinates["add_exercise_fab"]?.second ?: 30.dp
        ),
        TutorialStep(
            id = "analytics_icon",
            title = context.getString(R.string.tutorial_analytics_title),
            description = context.getString(R.string.tutorial_analytics_description),
            targetOffset = elementCoordinates["analytics_icon"]?.first,
            targetRadius = elementCoordinates["analytics_icon"]?.second ?: 25.dp
        ),
        TutorialStep(
            id = "settings_icon",
            title = context.getString(R.string.tutorial_settings_title),
            description = context.getString(R.string.tutorial_settings_description),
            targetOffset = elementCoordinates["settings_icon"]?.first,
            targetRadius = elementCoordinates["settings_icon"]?.second ?: 25.dp
        ),
        TutorialStep(
            id = "edit_goals_icon",
            title = context.getString(R.string.tutorial_edit_goals_title),
            description = context.getString(R.string.tutorial_edit_goals_description),
            targetOffset = elementCoordinates["edit_goals_icon"]?.first,
            targetRadius = elementCoordinates["edit_goals_icon"]?.second ?: 25.dp
        ),
        TutorialStep(
            id = "supplement_pill",
            title = context.getString(R.string.tutorial_supplement_pill_title),
            description = context.getString(R.string.tutorial_supplement_pill_description),
            targetOffset = elementCoordinates["supplement_pill"]?.first,
            targetRadius = elementCoordinates["supplement_pill"]?.second ?: 55.dp
        ),
        TutorialStep(
            id = "check_in_history",
            title = context.getString(R.string.tutorial_check_in_history_title),
            description = context.getString(R.string.tutorial_check_in_history_description),
            targetOffset = elementCoordinates["check_in_history"]?.first,
            targetRadius = elementCoordinates["check_in_history"]?.second ?: 100.dp
        ),
        TutorialStep(
            id = "completion",
            title = context.getString(R.string.tutorial_completion_title),
            description = context.getString(R.string.tutorial_completion_description)
        )
    )
}

private fun getStepIdFromIndex(index: Int): String {
    return when (index) {
        0 -> "dashboard_overview"
        1 -> "calorie_ring"
        2 -> "add_meal_fab"
        3 -> "add_exercise_fab"
        4 -> "analytics_icon"
        5 -> "settings_icon"
        6 -> "edit_goals_icon"
        7 -> "supplement_pill"
        8 -> "check_in_history"
        9 -> "completion"
        else -> ""
    }
}
