package com.offlinelabs.nutcracker.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.FoodLogApplication
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

// ---------------------------------------------------------------------------------------------
// Pure helpers (unit-tested in ExerciseAnalyticsTest)
// ---------------------------------------------------------------------------------------------

/** Number of days shown in the "recent" window (today included). */
const val ExerciseRecentDays = 7

/** The calendar always shows 6 weeks so the card keeps its height when changing month. */
const val ExerciseCalendarWeeks = 6

/** First cell of the calendar grid: the [firstDayOfWeek] on or before the 1st of [month]. */
fun calendarGridStart(month: YearMonth, firstDayOfWeek: DayOfWeek): LocalDate {
    val first = month.atDay(1)
    val back = Math.floorMod(first.dayOfWeek.value - firstDayOfWeek.value, 7)
    return first.minusDays(back.toLong())
}

/** The 42 days of the calendar grid, in reading order. */
fun calendarGridDays(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val start = calendarGridStart(month, firstDayOfWeek)
    return (0 until ExerciseCalendarWeeks * 7).map { start.plusDays(it.toLong()) }
}

/** The seven weekdays starting at [firstDayOfWeek]. */
fun orderedWeekdays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
    (0 until 7).map { firstDayOfWeek.plus(it.toLong()) }

/** Average burned per day with exercise, or null when there was none. */
fun avgKcalPerActiveDay(totalKcal: Double, activeDays: Int): Double? =
    if (activeDays > 0) totalKcal / activeDays else null

/** Whole days between [last] and [today] (never negative), or null when nothing was logged. */
fun daysSinceLastExercise(last: LocalDate?, today: LocalDate): Int? =
    last?.let { maxOf(0L, today.toEpochDay() - it.toEpochDay()).toInt() }

/** Splits the pipe-delimited muscle strings, drops blanks and duplicates, capitalizes each name. */
fun parseMuscles(raw: List<String>, locale: Locale = Locale.getDefault()): List<String> =
    raw.flatMap { it.split("|") }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .map { name -> name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() } }

data class ExerciseAnalyticsUiState(
    val isLoading: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val month: YearMonth = YearMonth.from(today),
    val caloriesLast7Days: Double = 0.0,
    val activeDaysLast7: Int = 0,
    /** Date of the most recent exercise, null if none was ever logged. */
    val lastExerciseDate: LocalDate? = null,
    val lastMuscles: List<String> = emptyList(),
    /** Days with exercise inside the visible calendar grid. */
    val exerciseDates: Set<LocalDate> = emptySet()
) {
    val isEmpty: Boolean get() = lastExerciseDate == null
}

// ---------------------------------------------------------------------------------------------
// Stateful entry point: loads from the DAOs
// ---------------------------------------------------------------------------------------------

@Composable
fun ExerciseAnalyticsTab(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as FoodLogApplication
    val dao = application.database.exerciseLogDao()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val today = remember { LocalDate.now() }

    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = remember(monthText) { YearMonth.parse(monthText) }

    var loaded by remember { mutableStateOf(false) }
    var calories by remember { mutableStateOf(0.0) }
    var activeDays by remember { mutableStateOf(0) }
    var lastDate by remember { mutableStateOf<LocalDate?>(null) }
    var muscles by remember { mutableStateOf<List<String>>(emptyList()) }
    var dates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }

    LaunchedEffect(today) {
        val start = today.minusDays((ExerciseRecentDays - 1).toLong()).toString()
        val end = today.toString()
        launch { dao.getCaloriesBurnedInDateRange(start, end).collectLatest { calories = it } }
        launch { dao.getExerciseDaysInDateRange(start, end).collectLatest { activeDays = it } }
        launch { dao.getPrimaryMusclesFromLastExercise().collectLatest { muscles = it } }
        launch {
            dao.getLastExerciseDate().collectLatest {
                lastDate = it?.let { text -> runCatching { LocalDate.parse(text) }.getOrNull() }
                loaded = true
            }
        }
    }

    // Collected directly in this effect, so changing month cancels the previous month's collector.
    LaunchedEffect(month, firstDayOfWeek) {
        val start = calendarGridStart(month, firstDayOfWeek)
        val end = start.plusDays((ExerciseCalendarWeeks * 7 - 1).toLong())
        dao.getExerciseDatesInRange(start.toString(), end.toString()).collectLatest { list ->
            dates = list.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        }
    }

    ExerciseAnalyticsContent(
        state = ExerciseAnalyticsUiState(
            isLoading = !loaded,
            today = today,
            month = month,
            caloriesLast7Days = calories,
            activeDaysLast7 = activeDays,
            lastExerciseDate = lastDate,
            lastMuscles = muscles,
            exerciseDates = dates
        ),
        onMonthChange = { monthText = it.toString() },
        modifier = modifier
    )
}

// ---------------------------------------------------------------------------------------------
// Stateless content
// ---------------------------------------------------------------------------------------------

@Composable
fun ExerciseAnalyticsContent(
    state: ExerciseAnalyticsUiState,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FormDefaults.colors()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
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
            state.isEmpty -> item(key = "empty") {
                AnalyticsCard(modifier = itemModifier, colors = colors) {
                    EmptyState(
                        title = stringResource(R.string.analytics_exercise_empty_title),
                        message = stringResource(R.string.analytics_exercise_empty_message),
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        colors = colors
                    )
                }
            }
            else -> {
                item(key = "hero") { ExerciseHero(state, locale, colors, itemModifier) }
                item(key = "tiles") { ExerciseStatTiles(state, locale, colors, itemModifier) }
                item(key = "calendar") {
                    ExerciseCalendarCard(state, locale, onMonthChange, colors, itemModifier)
                }
            }
        }
    }
}

@Composable
private fun ExerciseHero(
    state: ExerciseAnalyticsUiState,
    locale: Locale,
    colors: FormColors,
    modifier: Modifier
) {
    val start = state.today.minusDays((ExerciseRecentDays - 1).toLong())
    HeroStat(
        label = stringResource(R.string.analytics_calories_burned),
        value = formatWhole(state.caloriesLast7Days, locale),
        unit = stringResource(R.string.unit_kcal),
        caption = stringResource(
            R.string.analytics_caption_format,
            stringResource(R.string.analytics_range_last_days, ExerciseRecentDays),
            formatDateRange(start, state.today, false, locale)
        ),
        colors = colors,
        modifier = modifier
    )
}

@Composable
private fun ExerciseStatTiles(
    state: ExerciseAnalyticsUiState,
    locale: Locale,
    colors: FormColors,
    modifier: Modifier
) {
    val noValue = stringResource(R.string.analytics_no_value)
    val avg = avgKcalPerActiveDay(state.caloriesLast7Days, state.activeDaysLast7)
    val since = daysSinceLastExercise(state.lastExerciseDate, state.today)
    val muscles = parseMuscles(state.lastMuscles, locale)
    val tileModifier = Modifier.fillMaxHeight()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTileRow {
            StatTile(
                label = stringResource(R.string.analytics_exercise_days),
                value = formatWhole(state.activeDaysLast7.toDouble(), locale),
                supporting = stringResource(R.string.analytics_of_days, ExerciseRecentDays),
                icon = Icons.Filled.CalendarMonth,
                tone = if (state.activeDaysLast7 > 0) StatTone.Accent else StatTone.Neutral,
                colors = colors,
                modifier = tileModifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.analytics_days_since_last_exercise),
                value = when (since) {
                    null -> stringResource(R.string.analytics_never)
                    0 -> stringResource(R.string.analytics_today)
                    1 -> stringResource(R.string.analytics_days_count_one, since)
                    else -> stringResource(R.string.analytics_days_count, since)
                },
                icon = Icons.Filled.History,
                colors = colors,
                modifier = tileModifier.weight(1f)
            )
        }
        StatTileRow {
            StatTile(
                label = stringResource(R.string.analytics_exercise_tile_avg),
                value = avg?.let { stringResource(R.string.kcal_value_format, formatWhole(it, locale)) } ?: noValue,
                icon = Icons.Filled.LocalFireDepartment,
                colors = colors,
                modifier = tileModifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.analytics_last_exercise_muscles),
                value = if (muscles.isEmpty()) stringResource(R.string.none) else muscles.joinToString(", "),
                icon = Icons.Filled.FitnessCenter,
                colors = colors,
                valueStyle = MaterialTheme.typography.titleSmall,
                valueMaxLines = 2,
                modifier = tileModifier.weight(1f)
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Calendar
// ---------------------------------------------------------------------------------------------

@Composable
private fun ExerciseCalendarCard(
    state: ExerciseAnalyticsUiState,
    locale: Locale,
    onMonthChange: (YearMonth) -> Unit,
    colors: FormColors,
    modifier: Modifier
) {
    ExerciseCalendar(
        month = state.month,
        today = state.today,
        exerciseDates = state.exerciseDates,
        locale = locale,
        onPreviousMonth = { onMonthChange(state.month.minusMonths(1)) },
        onNextMonth = { onMonthChange(state.month.plusMonths(1)) },
        colors = colors,
        modifier = modifier
    )
}

@Composable
fun ExerciseCalendar(
    month: YearMonth,
    today: LocalDate,
    exerciseDates: Set<LocalDate>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale = Locale.getDefault(),
    colors: FormColors = FormDefaults.colors()
) {
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val days = remember(month, firstDay) { calendarGridDays(month, firstDay) }
    val title = formatDateRange(month.atDay(1), month.atDay(1), true, locale)

    AnalyticsCard(modifier = modifier, colors = colors, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.analytics_previous_month),
                    tint = colors.content
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.content,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.analytics_next_month),
                    tint = colors.content
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            orderedWeekdays(firstDay).forEach { day ->
                Text(
                    text = day.getDisplayName(JavaTextStyle.NARROW_STANDALONE, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.contentSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    CalendarDayCell(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        isToday = date == today,
                        isFuture = date.isAfter(today),
                        hasExercise = date in exerciseDates,
                        locale = locale,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    hasExercise: Boolean,
    locale: Locale,
    colors: FormColors,
    modifier: Modifier = Modifier
) {
    val filled = hasExercise && inMonth
    val description = if (filled) {
        stringResource(R.string.analytics_calendar_day_exercise, formatMediumDate(date, locale))
    } else {
        null
    }
    val textColor = when {
        filled -> colors.onAccent
        !inMonth -> colors.contentSecondary.copy(alpha = 0.35f)
        isFuture -> colors.contentSecondary.copy(alpha = 0.6f)
        else -> colors.content
    }
    Box(
        modifier = modifier
            .height(44.dp)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .then(if (filled) Modifier.background(colors.accent) else Modifier)
                .then(
                    if (isToday && inMonth) {
                        Modifier.border(2.dp, if (filled) colors.content else colors.accent, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || filled) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------------------------

private fun previewExerciseState(empty: Boolean): ExerciseAnalyticsUiState {
    val today = LocalDate.of(2026, 11, 18)
    if (empty) return ExerciseAnalyticsUiState(today = today)
    return ExerciseAnalyticsUiState(
        today = today,
        caloriesLast7Days = 1240.0,
        activeDaysLast7 = 4,
        lastExerciseDate = today.minusDays(1),
        lastMuscles = listOf("chest|triceps", "shoulders"),
        exerciseDates = setOf(1, 3, 4, 8, 10, 11, 15, 16, 17).map { LocalDate.of(2026, 11, it) }.toSet()
    )
}

@Composable
private fun ExercisePreview(dark: Boolean, state: ExerciseAnalyticsUiState) {
    FoodLogTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                AnalyticsHeaderColumn {
                    SectionTabs(currentSection = AnalyticsSection.EXERCISE, onSectionChange = {})
                }
                ExerciseAnalyticsContent(
                    state = state,
                    onMonthChange = {},
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(name = "Exercise - light", widthDp = 412, heightDp = 1000)
@Composable
private fun ExerciseLightPreview() {
    ExercisePreview(dark = false, state = previewExerciseState(empty = false))
}

@Preview(name = "Exercise - dark", widthDp = 412, heightDp = 1000)
@Composable
private fun ExerciseDarkPreview() {
    ExercisePreview(dark = true, state = previewExerciseState(empty = false))
}

@Preview(name = "Exercise - empty - light", widthDp = 412, heightDp = 700)
@Composable
private fun ExerciseEmptyLightPreview() {
    ExercisePreview(dark = false, state = previewExerciseState(empty = true))
}

@Preview(name = "Exercise - empty - dark", widthDp = 412, heightDp = 700)
@Composable
private fun ExerciseEmptyDarkPreview() {
    ExercisePreview(dark = true, state = previewExerciseState(empty = true))
}

@Preview(name = "Exercise - narrow dark", widthDp = 360, heightDp = 1000)
@Composable
private fun ExerciseNarrowDarkPreview() {
    ExercisePreview(dark = true, state = previewExerciseState(empty = false))
}
