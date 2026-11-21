package com.offlinelabs.nutcracker.ui.screens.analytics

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinelabs.nutcracker.FoodLogApplication
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.form.formatSaltMg
import com.offlinelabs.nutcracker.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") isDarkTheme: Boolean
) {
    // Animation state for the circular reveal effect
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var currentSection by rememberSaveable { mutableStateOf(AnalyticsSection.NUTRITION) }
    var currentTimePeriod by rememberSaveable { mutableStateOf(TimePeriod.DAYS) }

    // Nutrition state lives at screen level: the content below is stateless (previewable) and the
    // selection/toggles survive switching to the Exercise tab and back.
    val context = LocalContext.current
    val foodLogRepository = remember(context) {
        (context.applicationContext as FoodLogApplication).foodLogRepository
    }
    val nutritionHolder = remember(foodLogRepository) {
        AnalyticsStateHolder(foodLogRepository, coroutineScope, initialPeriod = currentTimePeriod)
    }
    LaunchedEffect(currentTimePeriod) { nutritionHolder.setPeriod(currentTimePeriod) }
    val nutritionState by nutritionHolder.state.collectAsState()

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
            .background(MaterialTheme.colorScheme.background)
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

                    AnalyticsHeaderColumn {
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
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Nutrition is the left tab, Exercise the right one: slide accordingly
                AnimatedContent(
                    targetState = currentSection,
                    transitionSpec = {
                        val toRight = targetState == AnalyticsSection.EXERCISE
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> if (toRight) fullWidth else -fullWidth },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { fullWidth -> if (toRight) -fullWidth else fullWidth },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        )).using(SizeTransform(clip = false))
                    },
                    label = "sectionContent"
                ) { section ->
                    when (section) {
                        AnalyticsSection.NUTRITION -> NutritionAnalyticsContent(
                            state = nutritionState,
                            onSelectBucket = nutritionHolder::selectBucket,
                            onIncludeExerciseChange = nutritionHolder::setIncludeExercise,
                            onIncludeTefChange = nutritionHolder::setIncludeTef
                        )
                        AnalyticsSection.EXERCISE -> ExerciseAnalyticsTab()
                    }
                }
            }
        }
    }
}

/** Centred column limited to [AnalyticsMaxContentWidth] for the tabs under the top bar. */
@Composable
internal fun AnalyticsHeaderColumn(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = AnalyticsMaxContentWidth)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun SectionTabs(
    currentSection: AnalyticsSection,
    onSectionChange: (AnalyticsSection) -> Unit
) {
    val sections = listOf(AnalyticsSection.NUTRITION, AnalyticsSection.EXERCISE)
    SegmentedTabs(
        tabs = listOf(
            SegmentedTab(stringResource(R.string.analytics_nutrition), Icons.Filled.Restaurant),
            SegmentedTab(stringResource(R.string.analytics_exercise), Icons.AutoMirrored.Filled.DirectionsRun)
        ),
        selectedIndex = sections.indexOf(currentSection),
        onSelect = { onSectionChange(sections[it]) }
    )
}

@Composable
fun TimePeriodTabs(
    currentTimePeriod: TimePeriod,
    onTimePeriodChange: (TimePeriod) -> Unit
) {
    val periods = TimePeriod.entries
    SegmentedTabs(
        tabs = periods.map { period ->
            SegmentedTab(
                stringResource(
                    when (period) {
                        TimePeriod.DAYS -> R.string.analytics_days
                        TimePeriod.WEEKS -> R.string.analytics_weeks
                        TimePeriod.MONTHS -> R.string.analytics_months
                    }
                )
            )
        },
        selectedIndex = periods.indexOf(currentTimePeriod),
        onSelect = { onTimePeriodChange(periods[it]) }
    )
}

// ---------------------------------------------------------------------------------------------
// Nutrition tab (stateless; the holder is wired in AnalyticsScreen)
// ---------------------------------------------------------------------------------------------

/**
 * Nutrition analytics: hero, KPI tiles, chart with toggles and the macro breakdown.
 * Stateless: pass the [AnalyticsUiState] and callbacks (see AnalyticsStateHolder).
 * Tapping a bar selects it, tapping it again or "Show all" calls [onSelectBucket] with null.
 */
@Composable
fun NutritionAnalyticsContent(
    state: AnalyticsUiState,
    onSelectBucket: (Int?) -> Unit,
    onIncludeExerciseChange: (Boolean) -> Unit,
    onIncludeTefChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FormDefaults.colors()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val summary = state.summary
    val itemModifier = Modifier
        .widthIn(max = AnalyticsMaxContentWidth)
        .fillMaxWidth()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            state.isLoading -> item(key = "loading") {
                val loading = stringResource(R.string.analytics_loading)
                AnalyticsCard(modifier = itemModifier, colors = colors) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.accent,
                            modifier = Modifier.semantics { contentDescription = loading }
                        )
                    }
                }
            }
            state.isEmpty || summary == null -> item(key = "empty") {
                AnalyticsCard(modifier = itemModifier, colors = colors) {
                    EmptyState(
                        title = stringResource(R.string.analytics_empty_title),
                        message = stringResource(R.string.analytics_empty_message),
                        icon = Icons.Filled.Restaurant,
                        colors = colors
                    )
                }
            }
            else -> {
                item(key = "hero") {
                    NutritionHero(state, summary, locale, onShowAll = { onSelectBucket(null) }, colors, itemModifier)
                }
                item(key = "tiles") {
                    NutritionStatTiles(summary, locale, colors, itemModifier)
                }
                item(key = "chart") {
                    NutritionChartCard(
                        state, summary, locale, onSelectBucket, onIncludeExerciseChange, onIncludeTefChange,
                        colors, itemModifier
                    )
                }
                item(key = "macros") {
                    NutritionMacrosCard(summary.stats, state.goals, locale, colors, itemModifier)
                }
            }
        }
    }
}

@Composable
private fun NutritionHero(
    state: AnalyticsUiState,
    summary: AnalyticsSummary,
    locale: Locale,
    onShowAll: () -> Unit,
    colors: FormColors,
    modifier: Modifier
) {
    val netMode = state.includeExercise || state.includeTef
    val average = summary.stats.avgNetPerLoggedDay
    val scope = when (state.selectedBucketIndex) {
        null -> when (state.period) {
            TimePeriod.DAYS -> stringResource(R.string.analytics_range_last_days, state.buckets.size)
            TimePeriod.WEEKS -> stringResource(R.string.analytics_range_last_weeks, state.buckets.size)
            TimePeriod.MONTHS -> stringResource(R.string.analytics_range_last_months, state.buckets.size)
        }
        else -> stringResource(
            when (state.period) {
                TimePeriod.DAYS -> R.string.analytics_selected_day
                TimePeriod.WEEKS -> R.string.analytics_selected_week
                TimePeriod.MONTHS -> R.string.analytics_selected_month
            }
        )
    }
    val range = formatDateRange(summary.startDate, elapsedEnd(summary), state.period == TimePeriod.MONTHS, locale)
    HeroStat(
        label = stringResource(
            if (netMode) R.string.analytics_avg_net_per_logged_day else R.string.analytics_avg_intake_per_logged_day
        ),
        value = average?.let { formatWhole(it, locale) } ?: stringResource(R.string.analytics_no_value),
        unit = if (average != null) stringResource(R.string.unit_kcal) else null,
        comparison = if (average != null) {
            stringResource(R.string.analytics_of_goal_kcal, formatWhole(state.goals.kcal.toDouble(), locale))
        } else {
            stringResource(R.string.analytics_nothing_logged_period)
        },
        caption = stringResource(R.string.analytics_caption_format, scope, range),
        action = if (state.selectedBucketIndex != null) {
            {
                AssistChip(
                    onClick = onShowAll,
                    label = { Text(stringResource(R.string.analytics_show_all)) },
                    trailingIcon = {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = colors.accent,
                        trailingIconContentColor = colors.accent
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        enabled = true,
                        borderColor = colors.accent.copy(alpha = 0.5f)
                    )
                )
            }
        } else {
            null
        },
        colors = colors,
        modifier = modifier
    )
}

@Composable
private fun NutritionStatTiles(
    summary: AnalyticsSummary,
    locale: Locale,
    colors: FormColors,
    modifier: Modifier
) {
    val stats = summary.stats
    val noValue = stringResource(R.string.analytics_no_value)
    val balance = stats.avgBalancePerLoggedDay
    val status = balance?.let { goalStatus(it) }
    val tileModifier = Modifier.fillMaxHeight()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTileRow {
            // Raw exercise data, shown regardless of the include-exercise toggle
            StatTile(
                label = stringResource(R.string.analytics_tile_avg_burned),
                value = stats.avgExercisePerLoggedDay?.let {
                    stringResource(R.string.kcal_value_format, formatWhole(it, locale))
                } ?: noValue,
                supporting = if (stats.loggedDays > 0) {
                    stringResource(R.string.analytics_total_kcal, formatWhole(stats.exerciseKcal, locale))
                } else {
                    null
                },
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                colors = colors,
                modifier = tileModifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.analytics_tile_avg_balance),
                value = balance?.let {
                    stringResource(R.string.kcal_value_format, formatSignedWhole(it, locale))
                } ?: noValue,
                supporting = when (status) {
                    GoalStatus.UNDER -> stringResource(R.string.analytics_under_goal)
                    GoalStatus.OVER -> stringResource(R.string.analytics_over_goal)
                    GoalStatus.ON -> stringResource(R.string.analytics_on_goal)
                    null -> null
                },
                icon = when (status) {
                    GoalStatus.UNDER -> Icons.AutoMirrored.Filled.TrendingDown
                    GoalStatus.OVER -> Icons.AutoMirrored.Filled.TrendingUp
                    else -> Icons.AutoMirrored.Filled.TrendingFlat
                },
                tone = when (status) {
                    GoalStatus.UNDER -> StatTone.Positive
                    GoalStatus.OVER -> StatTone.Negative
                    else -> StatTone.Neutral
                },
                colors = colors,
                modifier = tileModifier.weight(1f)
            )
        }
        StatTileRow {
            StatTile(
                label = stringResource(R.string.analytics_tile_days_logged),
                value = formatWhole(summary.daysLogged.toDouble(), locale),
                supporting = stringResource(
                    if (summary.daysInRange == 1) R.string.analytics_of_days_one else R.string.analytics_of_days,
                    summary.daysInRange
                ),
                icon = Icons.Filled.CalendarMonth,
                colors = colors,
                modifier = tileModifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.analytics_tile_streak),
                value = stringResource(
                    if (summary.streak == 1) R.string.analytics_days_count_one else R.string.analytics_days_count,
                    summary.streak
                ),
                supporting = stringResource(R.string.analytics_streak_supporting),
                icon = Icons.Filled.Whatshot,
                tone = if (summary.streak > 0) StatTone.Accent else StatTone.Neutral,
                colors = colors,
                modifier = tileModifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NutritionChartCard(
    state: AnalyticsUiState,
    summary: AnalyticsSummary,
    locale: Locale,
    onSelectBucket: (Int?) -> Unit,
    onIncludeExerciseChange: (Boolean) -> Unit,
    onIncludeTefChange: (Boolean) -> Unit,
    colors: FormColors,
    modifier: Modifier
) {
    val netMode = state.includeExercise || state.includeTef
    val labels = remember(state.buckets, state.period, locale) {
        axisLabels(state.period, state.buckets.map { it.startDate }, locale)
    }
    val entries = state.buckets.mapIndexed { i, bucket ->
        val value = bucket.stats.avgNetPerLoggedDay
        val date = when (state.period) {
            TimePeriod.DAYS -> formatDateRange(bucket.startDate, bucket.startDate, monthly = false, locale = locale)
            TimePeriod.WEEKS -> stringResource(R.string.analytics_week_of, formatMediumDate(bucket.startDate, locale))
            TimePeriod.MONTHS -> formatDateRange(bucket.startDate, bucket.startDate, monthly = true, locale = locale)
        }
        val description = when {
            value == null -> stringResource(R.string.analytics_bar_cd_empty, date)
            state.period == TimePeriod.DAYS ->
                stringResource(R.string.analytics_bar_cd_value, date, formatWhole(value, locale))
            else -> stringResource(R.string.analytics_bar_cd_average, date, formatWhole(value, locale))
        }
        BarChartEntry(value = value, label = labels[i], contentDescription = description)
    }
    val goal = state.goals.kcal.toDouble()

    AnalyticsCard(
        modifier = modifier,
        title = stringResource(if (netMode) R.string.analytics_chart_title_net else R.string.analytics_chart_title_intake),
        subtitle = stringResource(
            if (state.period == TimePeriod.DAYS) R.string.analytics_chart_subtitle_daily
            else R.string.analytics_chart_subtitle_average
        ),
        colors = colors
    ) {
        BarChart(
            entries = entries,
            selectedIndex = state.selectedBucketIndex,
            onBarClick = { i -> onSelectBucket(if (state.selectedBucketIndex == i) null else i) },
            referenceValue = goal.takeIf { it > 0 },
            referenceLabel = stringResource(R.string.analytics_chart_goal, formatWhole(goal, locale)),
            valueFormatter = { formatWhole(it, locale) },
            axisFormatter = { formatAxisValue(it, locale) },
            barColor = colors.accent,
            colors = colors
        )
        if (state.selectedBucketIndex == null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.analytics_chart_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.contentSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalyticsFilterChip(
                selected = state.includeExercise,
                onClick = { onIncludeExerciseChange(!state.includeExercise) },
                label = stringResource(R.string.analytics_include_exercise),
                icon = Icons.Filled.FitnessCenter,
                colors = colors
            )
            AnalyticsFilterChip(
                selected = state.includeTef,
                onClick = { onIncludeTefChange(!state.includeTef) },
                label = stringResource(R.string.analytics_include_tef),
                icon = Icons.Filled.LocalFireDepartment,
                colors = colors
            )
        }
        Text(
            text = stringResource(
                R.string.analytics_toggles_supporting,
                stringResource(R.string.kcal_value_format, formatWhole(summary.stats.exerciseKcal, locale)),
                stringResource(R.string.kcal_value_format, formatWhole(summary.stats.tefKcal, locale))
            ),
            style = MaterialTheme.typography.bodySmall,
            color = colors.contentSecondary
        )
    }
}

@Composable
private fun AnalyticsFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    colors: FormColors
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = if (selected) Icons.Filled.Check else icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            labelColor = colors.content,
            iconColor = colors.contentSecondary,
            selectedContainerColor = colors.accent.copy(alpha = 0.16f),
            selectedLabelColor = colors.content,
            selectedLeadingIconColor = colors.accent
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.fieldBorder,
            selectedBorderColor = colors.accent.copy(alpha = 0.6f)
        )
    )
}

@Composable
private fun NutritionMacrosCard(
    stats: PeriodStats,
    goals: AnalyticsGoals,
    locale: Locale,
    colors: FormColors,
    modifier: Modifier
) {
    val carbsLabel = stringResource(R.string.carbohydrates_label)
    val proteinLabel = stringResource(R.string.protein_label)
    val fatLabel = stringResource(R.string.fat_label)
    val carbsColor = nutrientCarbsColor()
    val proteinColor = nutrientProteinColor()
    val fatColor = nutrientFatColor()

    AnalyticsCard(
        modifier = modifier,
        title = stringResource(R.string.analytics_macros_title),
        subtitle = stringResource(R.string.analytics_macros_subtitle),
        colors = colors
    ) {
        val split = macroEnergyPercentages(stats.avgCarbsG, stats.avgProteinG, stats.avgFatG)
        if (split != null) {
            Text(
                text = stringResource(R.string.analytics_energy_split),
                style = MaterialTheme.typography.labelMedium,
                color = colors.contentSecondary
            )
            Spacer(Modifier.height(8.dp))
            ShareBar(
                segments = listOf(
                    ShareSegment(carbsLabel, split[0], carbsColor),
                    ShareSegment(proteinLabel, split[1], proteinColor),
                    ShareSegment(fatLabel, split[2], fatColor)
                ),
                colors = colors
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colors.divider)
            Spacer(Modifier.height(16.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GramsRow(carbsLabel, carbsColor, stats.avgCarbsG, goals.carbsG, locale, colors)
            GramsRow(proteinLabel, proteinColor, stats.avgProteinG, goals.proteinG, locale, colors)
            GramsRow(fatLabel, fatColor, stats.avgFatG, goals.fatG, locale, colors)
            GramsRow(
                stringResource(R.string.fiber_label), nutrientFiberColor(), stats.avgFiberG, goals.fiberG, locale, colors,
                overIsGood = true
            )
            val salt = stats.avgSaltMg
            val saltGoal = goals.saltMg.toDouble()
            MacroProgressRow(
                label = stringResource(R.string.sodium_label),
                // Same neutral tone as the salt row of the meal form
                color = colors.contentSecondary,
                amountText = amountText(
                    value = salt?.let { stringResource(R.string.milligrams_value_format, formatSaltMg(it, locale)) },
                    goal = if (saltGoal > 0) stringResource(R.string.milligrams_value_format, formatSaltMg(saltGoal, locale)) else null
                ),
                fraction = goalFraction(salt, saltGoal),
                enabled = salt != null,
                colors = colors
            )
        }
    }
}

@Composable
private fun GramsRow(
    label: String,
    color: Color,
    value: Double?,
    goal: Int,
    locale: Locale,
    colors: FormColors,
    overIsGood: Boolean = false
) {
    MacroProgressRow(
        label = label,
        color = color,
        amountText = amountText(
            value = value?.let { stringResource(R.string.analytics_grams_format, formatWhole(it, locale)) },
            goal = if (goal > 0) stringResource(R.string.analytics_grams_format, formatWhole(goal.toDouble(), locale)) else null
        ),
        fraction = goalFraction(value, goal.toDouble()),
        overIsGood = overIsGood,
        enabled = value != null,
        colors = colors
    )
}

/** "182 g / 250 g", "182 g" without a goal, or a dash without a value. */
@Composable
private fun amountText(value: String?, goal: String?): String = when {
    value == null -> stringResource(R.string.analytics_no_value)
    goal == null -> value
    else -> stringResource(R.string.analytics_amount_of_goal, value, goal)
}

// ---------------------------------------------------------------------------------------------
// Nutrition previews (fake data through the real aggregator)
// ---------------------------------------------------------------------------------------------

private val PreviewToday: LocalDate = LocalDate.of(2026, 11, 20)

private fun previewNutritionState(
    period: TimePeriod,
    selected: Int? = null,
    empty: Boolean = false,
    includeExercise: Boolean = false
): AnalyticsUiState {
    val days = if (empty) emptyList() else (0 until 340).mapNotNull { back ->
        if (back % 6 == 4) return@mapNotNull null
        AnalyticsDayInput(
            date = PreviewToday.minusDays(back.toLong()),
            consumedKcal = 1650.0 + (back * 137) % 750,
            carbsG = 190.0 + (back * 31) % 90,
            proteinG = 80.0 + (back * 17) % 50,
            fatG = 55.0 + (back * 13) % 35,
            fiberG = 18.0 + (back * 7) % 16,
            saltMg = 4200.0 + (back * 211) % 2600,
            exerciseKcal = if (back % 3 == 0) 320.0 else 0.0,
            checkInCount = 3
        )
    }
    return AnalyticsAggregator.buildState(
        days = days,
        goal = UserGoal.default(),
        today = PreviewToday,
        period = period,
        includeExercise = includeExercise,
        includeTef = false,
        selectedBucketIndex = selected,
        locale = Locale.getDefault()
    )
}

@Composable
private fun NutritionPreview(dark: Boolean, state: AnalyticsUiState) {
    FoodLogTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                AnalyticsHeaderColumn {
                    SectionTabs(currentSection = AnalyticsSection.NUTRITION, onSectionChange = {})
                    TimePeriodTabs(currentTimePeriod = state.period, onTimePeriodChange = {})
                }
                NutritionAnalyticsContent(
                    state = state,
                    onSelectBucket = {},
                    onIncludeExerciseChange = {},
                    onIncludeTefChange = {},
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(name = "Nutrition - days - light", widthDp = 412, heightDp = 1900)
@Composable
private fun NutritionDaysLightPreview() {
    NutritionPreview(dark = false, state = previewNutritionState(TimePeriod.DAYS))
}

@Preview(name = "Nutrition - days - dark", widthDp = 412, heightDp = 1900)
@Composable
private fun NutritionDaysDarkPreview() {
    NutritionPreview(dark = true, state = previewNutritionState(TimePeriod.DAYS))
}

@Preview(name = "Nutrition - weeks, selected, exercise on - dark", widthDp = 412, heightDp = 1900)
@Composable
private fun NutritionWeeksSelectedDarkPreview() {
    NutritionPreview(
        dark = true,
        state = previewNutritionState(TimePeriod.WEEKS, selected = 9, includeExercise = true)
    )
}

@Preview(name = "Nutrition - months - light", widthDp = 412, heightDp = 1900)
@Composable
private fun NutritionMonthsLightPreview() {
    NutritionPreview(dark = false, state = previewNutritionState(TimePeriod.MONTHS))
}

@Preview(name = "Nutrition - months, selected - light 360dp", widthDp = 360, heightDp = 1900)
@Composable
private fun NutritionMonthsSelectedNarrowPreview() {
    NutritionPreview(dark = false, state = previewNutritionState(TimePeriod.MONTHS, selected = 10))
}

@Preview(name = "Nutrition - empty - light", widthDp = 412, heightDp = 900)
@Composable
private fun NutritionEmptyLightPreview() {
    NutritionPreview(dark = false, state = previewNutritionState(TimePeriod.DAYS, empty = true))
}

@Preview(name = "Nutrition - empty - dark", widthDp = 412, heightDp = 900)
@Composable
private fun NutritionEmptyDarkPreview() {
    NutritionPreview(dark = true, state = previewNutritionState(TimePeriod.DAYS, empty = true))
}



