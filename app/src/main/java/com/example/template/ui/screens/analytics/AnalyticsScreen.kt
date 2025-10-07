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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

// Enum for analytics sections
enum class AnalyticsSection {
    NUTRITION,
    EXERCISE
}

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
                        AnalyticsSection.NUTRITION -> NutritionAnalyticsContent()
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
fun NutritionAnalyticsContent() {
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
            // Bar Chart
            CaloriesBarChart(
                dailyCalories = dailyCalories,
                calorieGoal = userGoal.caloriesGoal.toDouble(),
                last7Days = last7Days
            )
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
                text = "For this period:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = appTextPrimaryColor()
            )
        }
        
        item {
            // Stats Cards
            StatsCards(
                dailyCalories = dailyCalories,
                calorieGoal = userGoal.caloriesGoal.toDouble()
            )
        }
    }
}

@Composable
fun CaloriesBarChart(
    dailyCalories: Map<LocalDate, Double>,
    calorieGoal: Double,
    last7Days: List<LocalDate>
) {
    val textMeasurer = rememberTextMeasurer()
    val textColor = appTextPrimaryColor()
    val barColor = BrandGold
    val goalLineColor = appTextSecondaryColor()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(
                color = appSurfaceColor().copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartWidth = size.width
            val chartHeight = size.height
            val barWidth = chartWidth / (last7Days.size * 2)
            val spacing = barWidth
            
            // Calculate max value for scaling
            val maxCalories = maxOf(
                dailyCalories.values.maxOrNull() ?: 0.0,
                calorieGoal
            ) * 1.1 // Add 10% padding
            
            if (maxCalories > 0) {
                // Draw goal line
                val goalY = chartHeight - (calorieGoal / maxCalories * chartHeight).toFloat()
                drawLine(
                    color = goalLineColor,
                    start = Offset(0f, goalY),
                    end = Offset(chartWidth, goalY),
                    strokeWidth = 3f
                )
                
                // Draw bars
                last7Days.forEachIndexed { index, date ->
                    val calories = dailyCalories[date] ?: 0.0
                    val barHeight = (calories / maxCalories * chartHeight).toFloat()
                    val x = index * (barWidth + spacing) + spacing / 2
                    val y = chartHeight - barHeight
                    
                    // Draw bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    
                    // Draw day label
                    val dayLabel = date.dayOfWeek.getDisplayName(
                        JavaTextStyle.SHORT,
                        Locale.getDefault()
                    ).take(1) // First letter only
                    
                    val textLayoutResult = textMeasurer.measure(
                        dayLabel,
                        style = TextStyle(
                            color = textColor,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x + barWidth / 2 - textLayoutResult.size.width / 2,
                            chartHeight + 8f
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCards(
    dailyCalories: Map<LocalDate, Double>,
    calorieGoal: Double
) {
    val totalCalories = dailyCalories.values.sum()
    val targetCalories = calorieGoal * 7 // 7 days
    val balance = targetCalories - totalCalories // Inverted: remaining calories (positive = under goal, negative = over goal)
    val averageBalance = balance / 7
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Total calories consumed
        StatCard(
            title = "Total calories consumed",
            value = "${totalCalories.toInt()} kcal"
        )
        
        // Card 2: Final balance
        StatCard(
            title = "Your final balance",
            value = "${if (balance >= 0) "+" else ""}${balance.toInt()} kcal",
            valueColor = if (balance >= 0) ProteinFiberColor else ExceededColor // Green if under goal, red if over
        )
        
        // Card 3: Average balance
        StatCard(
            title = "Average balance per day",
            value = "${if (averageBalance >= 0) "+" else ""}${averageBalance.toInt()} kcal",
            valueColor = if (averageBalance >= 0) ProteinFiberColor else ExceededColor // Green if under goal, red if over
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    valueColor: Color = appTextPrimaryColor()
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = appSurfaceColor(),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                color = appTextSecondaryColor()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
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

