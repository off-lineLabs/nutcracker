package com.example.template.ui.screens.analytics

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.abs
import com.example.template.FoodLogApplication
import com.example.template.R
import com.example.template.data.dao.DailyTotals
import com.example.template.data.model.UserGoal
import com.example.template.ui.components.ExerciseToggle
import com.example.template.ui.components.TEFToggle
import com.example.template.ui.theme.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

// Enum for analytics sections
enum class AnalyticsSection {
    NUTRITION,
    EXERCISE
}

// Enum for time periods
enum class TimePeriod {
    DAYS,
    WEEKS,
    MONTHS
}

// Data classes for different time periods
data class DayData(
    val date: LocalDate,
    val calories: Double
)

data class WeekData(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val calories: Double,
    val weekLabel: String
)

data class MonthData(
    val monthStart: LocalDate,
    val monthEnd: LocalDate,
    val calories: Double,
    val monthLabel: String
)

data class SelectedPeriodData(
    val period: String,
    val calories: Double,
    val date: LocalDate
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean
) {
    // Animation state for the circular reveal effect
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Current section state
    var currentSection by remember { mutableStateOf(AnalyticsSection.NUTRITION) }
    
    // Current time period state
    var currentTimePeriod by remember { mutableStateOf(TimePeriod.DAYS) }
    
    // Start animation on first composition
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // Animated values for scale and alpha
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )
    
    // Animated background colors based on selected section
    val backgroundStartColor by animateColorAsState(
        targetValue = appBackgroundColor(),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "backgroundStart"
    )
    
    val backgroundEndColor by animateColorAsState(
        targetValue = when (currentSection) {
            AnalyticsSection.NUTRITION -> if (isDarkTheme) BrandGold else BrandGoldLightTheme
            AnalyticsSection.EXERCISE -> if (isDarkTheme) BrandRed else BrandRedLightTheme
        },
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "backgroundEnd"
    )
    
    // Handle back navigation with reverse animation
    fun handleBack() {
        isVisible = false
        // Delay to allow animation to complete
        coroutineScope.launch {
            delay(300)
            onNavigateBack()
        }
    }
    
    // Intercept system back button
    androidx.activity.compose.BackHandler {
        handleBack()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundStartColor,
                        backgroundEndColor.copy(alpha = 0.3f)
                    )
                )
            )
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
                // Center the scaling transformation
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.9f, 0.1f)
            }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.analytics_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = appTextPrimaryColor()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { handleBack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = appTextPrimaryColor()
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                    
                    // Section tabs
                    SectionTabs(
                        currentSection = currentSection,
                        onSectionChange = { currentSection = it }
                    )
                    
                    // Time period navigation (only for nutrition)
                    if (currentSection == AnalyticsSection.NUTRITION) {
                        TimePeriodTabs(
                            currentTimePeriod = currentTimePeriod,
                            onTimePeriodChange = { currentTimePeriod = it }
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Animated content with swipe-like transition
                AnimatedContent(
                    targetState = currentSection,
                    transitionSpec = {
                        // Determine slide direction based on section change
                        val slideDirection = if (targetState == AnalyticsSection.EXERCISE) {
                            // Moving from Nutrition to Exercise (right to left)
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            )
                        } else {
                            // Moving from Exercise to Nutrition (left to right)
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            )
                        }
                        slideDirection.using(SizeTransform(clip = false))
                    },
                    label = "sectionContent"
                ) { section ->
                    when (section) {
                        AnalyticsSection.NUTRITION -> NutritionAnalyticsContent(
                            timePeriod = currentTimePeriod
                        )
                        AnalyticsSection.EXERCISE -> ExerciseAnalyticsContent()
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTabs(
    currentSection: AnalyticsSection,
    onSectionChange: (AnalyticsSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Exercise Tab
        SectionTabButton(
            icon = { Icon(painterResource(R.drawable.ic_sprint), contentDescription = "Exercise") },
            label = "Exercise",
            isSelected = currentSection == AnalyticsSection.EXERCISE,
            onClick = { onSectionChange(AnalyticsSection.EXERCISE) },
            selectedColor = BrandRed
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Nutrition Tab
        SectionTabButton(
            icon = { Icon(Icons.Filled.Restaurant, contentDescription = "Nutrition") },
            label = "Nutrition",
            isSelected = currentSection == AnalyticsSection.NUTRITION,
            onClick = { onSectionChange(AnalyticsSection.NUTRITION) },
            selectedColor = BrandGold
        )
    }
}

@Composable
fun SectionTabButton(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else appSurfaceColor(),
        animationSpec = tween(300),
        label = "tabBackground"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else appTextSecondaryColor(),
        animationSpec = tween(300),
        label = "tabContent"
    )
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        modifier = Modifier
            .height(48.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                icon()
            }
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun TimePeriodTabs(
    currentTimePeriod: TimePeriod,
    onTimePeriodChange: (TimePeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimePeriod.values().forEach { period ->
            TimePeriodTabButton(
                period = period,
                isSelected = currentTimePeriod == period,
                onClick = { onTimePeriodChange(period) }
            )
            
            if (period != TimePeriod.values().last()) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun TimePeriodTabButton(
    period: TimePeriod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) BrandGold else appSurfaceColor(),
        animationSpec = tween(300),
        label = "timePeriodBackground"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else appTextSecondaryColor(),
        animationSpec = tween(300),
        label = "timePeriodContent"
    )
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        modifier = Modifier
            .height(36.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = when (period) {
                TimePeriod.DAYS -> "Days"
                TimePeriod.WEEKS -> "Weeks"
                TimePeriod.MONTHS -> "Months"
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

// Helper functions for time period data
fun generateDayData(last7Days: List<LocalDate>, dailyCalories: Map<LocalDate, Double>): List<DayData> {
    return last7Days.map { date ->
        DayData(
            date = date,
            calories = dailyCalories[date] ?: 0.0
        )
    }
}

fun generateWeekData(dailyCalories: Map<LocalDate, Double>): List<WeekData> {
    val weeks = mutableListOf<WeekData>()
    val sortedDates = dailyCalories.keys.sorted()
    
    if (sortedDates.isEmpty()) return weeks
    
    // Group dates by week (Monday to Sunday)
    val weekGroups = mutableMapOf<String, MutableList<LocalDate>>()
    
    for (date in sortedDates) {
        // Get Monday of the week
        val monday = date.with(DayOfWeek.MONDAY)
        val weekKey = monday.toString()
        
        weekGroups.getOrPut(weekKey) { mutableListOf() }.add(date)
    }
    
    // Convert to WeekData
    weekGroups.entries.sortedBy { it.key }.forEach { (_, dates) ->
        val weekStart = dates.minOrNull() ?: return@forEach
        val weekEnd = dates.maxOrNull() ?: return@forEach
        val weekCalories = dates.sumOf { dailyCalories[it] ?: 0.0 }
        
        // Format: "29/09 - 05/10"
        val startStr = "${weekStart.dayOfMonth.toString().padStart(2, '0')}/${weekStart.monthValue.toString().padStart(2, '0')}"
        val endStr = "${weekEnd.dayOfMonth.toString().padStart(2, '0')}/${weekEnd.monthValue.toString().padStart(2, '0')}"
        
        weeks.add(
            WeekData(
                weekStart = weekStart,
                weekEnd = weekEnd,
                calories = weekCalories,
                weekLabel = "$startStr - $endStr"
            )
        )
    }
    
    return weeks.takeLast(7) // Return last 7 weeks
}

fun generateMonthData(dailyCalories: Map<LocalDate, Double>): List<MonthData> {
    val months = mutableListOf<MonthData>()
    val sortedDates = dailyCalories.keys.sorted()
    
    if (sortedDates.isEmpty()) return months
    
    var currentMonth = sortedDates.first().month
    var currentYear = sortedDates.first().year
    var monthStart = sortedDates.first()
    var monthEnd = monthStart
    var monthCalories = 0.0
    
    for (date in sortedDates) {
        if (date.month == currentMonth && date.year == currentYear) {
            monthEnd = date
            monthCalories += dailyCalories[date] ?: 0.0
        } else {
            // Save previous month
            months.add(
                MonthData(
                    monthStart = monthStart,
                    monthEnd = monthEnd,
                    calories = monthCalories,
                    monthLabel = "${monthStart.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())} ${monthStart.year}"
                )
            )
            
            // Start new month
            currentMonth = date.month
            currentYear = date.year
            monthStart = date
            monthEnd = date
            monthCalories = dailyCalories[date] ?: 0.0
        }
    }
    
    // Add the last month
    months.add(
        MonthData(
            monthStart = monthStart,
            monthEnd = monthEnd,
            calories = monthCalories,
            monthLabel = "${monthStart.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())} ${monthStart.year}"
        )
    )
    
    return months.takeLast(7) // Return last 7 months
}

@Composable
fun NutritionAnalyticsContent(
    timePeriod: TimePeriod = TimePeriod.DAYS
) {
    // State for selected bar
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val foodLogRepository = (context.applicationContext as FoodLogApplication).foodLogRepository
    
    // State for toggles
    var includeExerciseCalories by remember { mutableStateOf(false) }
    var includeTEFBonus by remember { mutableStateOf(false) }
    
    // State for data
    var dailyCalories by remember { mutableStateOf<Map<LocalDate, Double>>(emptyMap()) }
    var userGoal by remember { mutableStateOf(UserGoal.default()) }
    
    // Get last 7 days
    val last7Days = remember {
        (0..6).map { LocalDate.now().minusDays(it.toLong()) }.reversed()
    }
    
    // Load user goal
    LaunchedEffect(foodLogRepository) {
        foodLogRepository.getUserGoal().collectLatest { goal ->
            userGoal = goal ?: UserGoal.default()
        }
    }
    
    // Load data for all 7 days
    LaunchedEffect(foodLogRepository, includeExerciseCalories, includeTEFBonus) {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
        // Collect all flows simultaneously
        kotlinx.coroutines.flow.combine(
            last7Days.map { date ->
                val dateString = date.format(dateFormatter)
                if (includeExerciseCalories || includeTEFBonus) {
                    foodLogRepository.getDailyCombinedTotals(
                        dateString, 
                        includeExerciseCalories, 
                        includeTEFBonus
                    )
                } else {
                    foodLogRepository.getDailyNutrientTotals(dateString)
                }
            }
        ) { totalsArray ->
            // Map each total to its corresponding date
            last7Days.mapIndexed { index, date ->
                date to (totalsArray[index]?.totalCalories ?: 0.0)
            }.toMap()
        }.collectLatest { caloriesMap ->
            dailyCalories = caloriesMap
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Bar Chart - now supports different time periods
            when (timePeriod) {
                TimePeriod.DAYS -> {
                    val dayData = generateDayData(last7Days, dailyCalories)
                    DayCaloriesBarChart(
                        dayData = dayData,
                        calorieGoal = userGoal.caloriesGoal.toDouble(),
                        timePeriod = timePeriod,
                        onBarSelected = { selectedBarIndex = it }
                    )
                }
                TimePeriod.WEEKS -> {
                    val weekData = generateWeekData(dailyCalories)
                    WeekCaloriesBarChart(
                        weekData = weekData,
                        calorieGoal = userGoal.caloriesGoal.toDouble() * 7, // Weekly goal
                        timePeriod = timePeriod,
                        onBarSelected = { selectedBarIndex = it }
                    )
                }
                TimePeriod.MONTHS -> {
                    val monthData = generateMonthData(dailyCalories)
                    MonthCaloriesBarChart(
                        monthData = monthData,
                        calorieGoal = userGoal.caloriesGoal.toDouble() * 30, // Monthly goal (approximate)
                        timePeriod = timePeriod,
                        onBarSelected = { selectedBarIndex = it }
                    )
                }
            }
        }
        
        item {
            // Toggles Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Calculate exercise and TEF totals for display
                var totalExerciseCalories by remember { mutableStateOf(0.0) }
                var totalTEFCalories by remember { mutableStateOf(0.0) }
                
                LaunchedEffect(foodLogRepository, last7Days) {
                    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    
                    // Collect exercise calories for all days
                    val exerciseFlows = last7Days.map { date ->
                        foodLogRepository.getDailyExerciseCalories(date.format(dateFormatter))
                    }
                    
                    // Collect TEF calories for all days
                    val tefFlows = last7Days.map { date ->
                        foodLogRepository.getDailyNutrientTotals(date.format(dateFormatter))
                    }
                    
                    kotlinx.coroutines.flow.combine(
                        exerciseFlows + tefFlows
                    ) { values ->
                        val exerciseValues = values.take(7) as List<Double>
                        val tefTotals = values.drop(7) as List<DailyTotals?>
                        
                        val exerciseSum = exerciseValues.sum()
                        val tefSum = tefTotals.sumOf { totals ->
                            totals?.let { com.example.template.utils.TEFCalculator.calculateTEFBonus(it) } ?: 0.0
                        }
                        
                        exerciseSum to tefSum
                    }.collectLatest { (exerciseSum, tefSum) ->
                        totalExerciseCalories = exerciseSum
                        totalTEFCalories = tefSum
                    }
                }
                
                ExerciseToggle(
                    isEnabled = includeExerciseCalories,
                    onToggle = { includeExerciseCalories = !includeExerciseCalories },
                    exerciseCalories = -totalExerciseCalories // Negative since it removes calories
                )
                
                TEFToggle(
                    isEnabled = includeTEFBonus,
                    onToggle = { includeTEFBonus = !includeTEFBonus },
                    tefCalories = -totalTEFCalories // Negative since it removes calories
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        item {
            // Stats Cards Title
            Text(
                text = "For the selected period:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = appTextPrimaryColor()
            )
        }
        
        item {
            // Stats Cards - now show data for selected period
            val selectedPeriodData = when (timePeriod) {
                TimePeriod.DAYS -> {
                    val dayData = generateDayData(last7Days, dailyCalories)
                    val selectedIndex = selectedBarIndex ?: (dayData.size - 1).coerceAtLeast(0)
                    dayData.getOrNull(selectedIndex)?.let { 
                        SelectedPeriodData(
                            period = "Day",
                            calories = it.calories,
                            date = it.date
                        )
                    }
                }
                TimePeriod.WEEKS -> {
                    val weekData = generateWeekData(dailyCalories)
                    val selectedIndex = selectedBarIndex ?: (weekData.size - 1).coerceAtLeast(0)
                    weekData.getOrNull(selectedIndex)?.let {
                        SelectedPeriodData(
                            period = "Week",
                            calories = it.calories,
                            date = it.weekStart
                        )
                    }
                }
                TimePeriod.MONTHS -> {
                    val monthData = generateMonthData(dailyCalories)
                    val selectedIndex = selectedBarIndex ?: (monthData.size - 1).coerceAtLeast(0)
                    monthData.getOrNull(selectedIndex)?.let {
                        SelectedPeriodData(
                            period = "Month",
                            calories = it.calories,
                            date = it.monthStart
                        )
                    }
                }
            }
            
            StatsCards(
                dailyCalories = dailyCalories,
                calorieGoal = userGoal.caloriesGoal.toDouble(),
                selectedPeriodData = selectedPeriodData
            )
        }
    }
}

// Chart functions for different time periods
@Composable
fun DayCaloriesBarChart(
    dayData: List<DayData>,
    calorieGoal: Double,
    timePeriod: TimePeriod,
    onBarSelected: (Int?) -> Unit = {}
) {
    CaloriesBarChartImpl(
        data = dayData.map { it.calories },
        labels = dayData.map { it.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).take(1).uppercase() },
        calorieGoal = calorieGoal,
        timePeriod = timePeriod,
        onBarSelected = onBarSelected
    )
}

@Composable
fun WeekCaloriesBarChart(
    weekData: List<WeekData>,
    calorieGoal: Double,
    timePeriod: TimePeriod,
    onBarSelected: (Int?) -> Unit = {}
) {
    CaloriesBarChartImpl(
        data = weekData.map { it.calories },
        labels = weekData.map { it.weekLabel },
        calorieGoal = calorieGoal,
        timePeriod = timePeriod,
        onBarSelected = onBarSelected
    )
}

@Composable
fun MonthCaloriesBarChart(
    monthData: List<MonthData>,
    calorieGoal: Double,
    timePeriod: TimePeriod,
    onBarSelected: (Int?) -> Unit = {}
) {
    CaloriesBarChartImpl(
        data = monthData.map { it.calories },
        labels = monthData.map { it.monthLabel },
        calorieGoal = calorieGoal,
        timePeriod = timePeriod,
        onBarSelected = onBarSelected
    )
}

@Composable
fun CaloriesBarChartImpl(
    data: List<Double>,
    labels: List<String>,
    calorieGoal: Double,
    timePeriod: TimePeriod,
    onBarSelected: (Int?) -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()
    val textColor = appTextPrimaryColor()
    val textSecondaryColor = appTextSecondaryColor()
    val surfaceColor = appSurfaceColor()
    val barColor = BrandGold
    val selectedBarColor = BrandGoldLight
    val goalLineColor = BrandRed.copy(alpha = 0.7f)
    
    var selectedDayIndex by remember { mutableStateOf<Int?>(data.size - 1) } // Default to last bar
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Goal line legend only
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(goalLineColor)
                )
                Text(
                    text = "Goal: ${calorieGoal.toInt()} kcal",
                    fontSize = 11.sp,
                    color = textSecondaryColor
                )
            }
        }
        
        // Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    color = appSurfaceColor().copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Calculate which bar was tapped
                            val chartWidth = size.width.toFloat()
                            val barWidth = chartWidth / (data.size * 2)
                            val spacing = barWidth
                            
                            data.forEachIndexed { index, _ ->
                                val x = index * (barWidth + spacing) + spacing / 2
                                if (offset.x >= x && offset.x <= x + barWidth) {
                                    selectedDayIndex = if (selectedDayIndex == index) null else index
                                    onBarSelected(selectedDayIndex)
                                }
                            }
                        }
                    }
            ) {
                val chartWidth = size.width
                val chartHeight = if (timePeriod == TimePeriod.WEEKS || timePeriod == TimePeriod.MONTHS) {
                    size.height - 80f // More space for rotated labels
                } else {
                    size.height - 60f // Regular space for horizontal labels
                }
                val barWidth = chartWidth / (data.size * 2)
                val spacing = barWidth
                
                // Calculate max value for scaling
                val maxCalories = maxOf(
                    data.maxOrNull() ?: 0.0,
                    calorieGoal
                ) * 1.15 // Add 15% padding
                
                if (maxCalories > 0) {
                    // Draw Y-axis scale lines and labels
                    val scaleSteps = 4
                    for (i in 0..scaleSteps) {
                        val value = (maxCalories / scaleSteps * i).toInt()
                        val y = chartHeight - (value / maxCalories * chartHeight).toFloat()
                        
                        // Scale line
                        drawLine(
                            color = textSecondaryColor.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1f
                        )
                        
                        // Scale label (skip first one at 0)
                        if (i > 0) {
                            val labelText = if (value >= 1000) {
                                val kValue = value / 1000.0
                                String.format(Locale.US, "%.1fk", kValue)
                            } else {
                                "$value"
                            }
                            val textLayoutResult = textMeasurer.measure(
                                labelText,
                                style = TextStyle(
                                    color = textSecondaryColor.copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                            )
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(4f, y - textLayoutResult.size.height / 2)
                            )
                        }
                    }
                    
                    // Draw goal line (dashed)
                    val goalY = chartHeight - (calorieGoal / maxCalories * chartHeight).toFloat()
                    drawLine(
                        color = goalLineColor,
                        start = Offset(0f, goalY),
                        end = Offset(chartWidth, goalY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    
                    // Draw bars
                    data.forEachIndexed { index, calories ->
                        val barHeight = (calories / maxCalories * chartHeight).toFloat()
                        val x = index * (barWidth + spacing) + spacing / 2
                        val y = chartHeight - barHeight
                        
                        val isSelected = selectedDayIndex == index
                        val currentBarColor = if (isSelected) selectedBarColor else barColor
                        
                        // Draw bar
                        drawRoundRect(
                            color = currentBarColor,
                            topLeft = Offset(x, y.coerceAtLeast(0f)),
                            size = Size(barWidth, barHeight.coerceAtLeast(1f)),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        
                        // Draw value on selected bar
                        if (isSelected && calories > 0) {
                            val valueText = "${calories.toInt()}"
                            val valueLayoutResult = textMeasurer.measure(
                                valueText,
                                style = TextStyle(
                                    color = textColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            
                            // Background for value
                            val valuePadding = 4f
                            val valueBoxWidth = valueLayoutResult.size.width + valuePadding * 2
                            val valueBoxHeight = valueLayoutResult.size.height + valuePadding * 2
                            val valueX = x + barWidth / 2 - valueBoxWidth / 2
                            val valueY = (y - valueBoxHeight - 8f).coerceAtLeast(0f)
                            
                            drawRoundRect(
                                color = surfaceColor,
                                topLeft = Offset(valueX, valueY),
                                size = Size(valueBoxWidth, valueBoxHeight),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            
                            drawText(
                                textLayoutResult = valueLayoutResult,
                                topLeft = Offset(
                                    valueX + valuePadding,
                                    valueY + valuePadding
                                )
                            )
                        }
                        
                        // Draw label below chart
                        val label = labels.getOrNull(index) ?: ""
                        val labelLayoutResult = textMeasurer.measure(
                            label,
                            style = TextStyle(
                                color = if (isSelected) textColor else textSecondaryColor,
                                fontSize = if (timePeriod == TimePeriod.DAYS) 14.sp else 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                        
                        // Draw labels with rotation for weeks and months
                        if (timePeriod == TimePeriod.WEEKS || timePeriod == TimePeriod.MONTHS) {
                            // For rotated labels, we'll use a different approach
                            // Draw them with a slight offset to avoid overlap
                            val labelX = x + barWidth / 2 - labelLayoutResult.size.width / 2
                            val labelY = chartHeight + 15f
                            
                            // Draw a small background for better readability
                            drawRect(
                                color = surfaceColor.copy(alpha = 0.8f),
                                topLeft = Offset(labelX - 2f, labelY - 2f),
                                size = androidx.compose.ui.geometry.Size(
                                    labelLayoutResult.size.width + 4f,
                                    labelLayoutResult.size.height + 4f
                                )
                            )
                            
                            drawText(
                                textLayoutResult = labelLayoutResult,
                                topLeft = Offset(labelX, labelY)
                            )
                        } else {
                            // Regular horizontal labels for days
                            drawText(
                                textLayoutResult = labelLayoutResult,
                                topLeft = Offset(
                                    x + barWidth / 2 - labelLayoutResult.size.width / 2,
                                    chartHeight + 12f
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCards(
    dailyCalories: Map<LocalDate, Double>,
    calorieGoal: Double,
    selectedPeriodData: SelectedPeriodData? = null
) {
    // Use selected period data if available, otherwise use total data
    val totalCalories = selectedPeriodData?.calories ?: dailyCalories.values.sum()
    val targetCalories = if (selectedPeriodData != null) {
        when (selectedPeriodData.period) {
            "Day" -> calorieGoal
            "Week" -> calorieGoal * 7
            "Month" -> calorieGoal * 30
            else -> calorieGoal
        }
    } else {
        calorieGoal * 7 // 7 days
    }
    val balance = targetCalories - totalCalories // Inverted: remaining calories (positive = under goal, negative = over goal)
    val averageBalance = if (selectedPeriodData != null) balance else balance / 7
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Total calories consumed
        EnhancedStatCard(
            title = "Total calories consumed",
            value = "${totalCalories.toInt()} kcal",
            icon = Icons.Filled.LocalDining,
            gradientColors = listOf(
                BrandGold.copy(alpha = 0.15f),
                BrandGold.copy(alpha = 0.05f)
            )
        )
        
        // Card 2: Final balance
        EnhancedStatCard(
            title = "Your final balance",
            value = "${if (balance >= 0) "+" else ""}${balance.toInt()} kcal",
            valueColor = if (balance >= 0) ProteinFiberColor else ExceededColor,
            icon = if (balance >= 0) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
            gradientColors = if (balance >= 0) {
                listOf(
                    ProteinFiberColor.copy(alpha = 0.15f),
                    ProteinFiberColor.copy(alpha = 0.05f)
                )
            } else {
                listOf(
                    ExceededColor.copy(alpha = 0.15f),
                    ExceededColor.copy(alpha = 0.05f)
                )
            }
        )
        
        // Card 3: Average balance
        EnhancedStatCard(
            title = "Average balance per day",
            value = "${if (averageBalance >= 0) "+" else ""}${averageBalance.toInt()} kcal",
            valueColor = if (averageBalance >= 0) ProteinFiberColor else ExceededColor,
            icon = if (averageBalance >= 0) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
            gradientColors = if (averageBalance >= 0) {
                listOf(
                    ProteinFiberColor.copy(alpha = 0.12f),
                    ProteinFiberColor.copy(alpha = 0.03f)
                )
            } else {
                listOf(
                    ExceededColor.copy(alpha = 0.12f),
                    ExceededColor.copy(alpha = 0.03f)
                )
            }
        )
    }
}

@Composable
fun EnhancedStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = appTextPrimaryColor(),
    gradientColors: List<Color>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = appTextPrimaryColor().copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = gradientColors
                    )
                )
                .background(
                    color = appSurfaceColor(),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        color = appTextSecondaryColor(),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = value,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = valueColor,
                        letterSpacing = (-0.5).sp
                    )
                }
                
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    valueColor.copy(alpha = 0.2f),
                                    valueColor.copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = valueColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseAnalyticsContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Exercise Analytics\n(Coming soon)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = appTextPrimaryColor()
        )
    }
}

