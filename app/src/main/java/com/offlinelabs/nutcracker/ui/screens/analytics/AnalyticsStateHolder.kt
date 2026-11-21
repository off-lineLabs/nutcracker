package com.offlinelabs.nutcracker.ui.screens.analytics

import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.data.repo.FoodLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Plain state holder (the project has no lifecycle-viewmodel-compose dependency) that the
 * composable remembers. It loads the last 12 months once (two range queries) and re-aggregates
 * in memory when the period, toggles, selection or goal change.
 *
 * Collect [state] with collectAsState(). Selection defaults to null (the whole range); changing the
 * period clears it again.
 */
class AnalyticsStateHolder(
    repository: FoodLogRepository,
    scope: CoroutineScope,
    private val today: LocalDate = LocalDate.now(),
    initialPeriod: TimePeriod = TimePeriod.DAYS
) {
    private data class Inputs(
        val period: TimePeriod,
        val includeExercise: Boolean,
        val includeTef: Boolean,
        val selected: Int?
    )

    private val period = MutableStateFlow(initialPeriod)
    private val includeExercise = MutableStateFlow(false)
    private val includeTef = MutableStateFlow(false)
    private val selected = MutableStateFlow<Int?>(null)

    private val dayInputs = run {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val start = fmt.format(AnalyticsAggregator.historyStart(today))
        val end = fmt.format(today)
        combine(
            repository.getDailyFoodTotalsInRange(start, end),
            repository.getDailyExerciseCaloriesInRange(start, end)
        ) { food, exercise -> AnalyticsAggregator.mergeRows(food, exercise) }
    }

    private val inputs = combine(period, includeExercise, includeTef, selected) { p, e, t, s ->
        Inputs(p, e, t, s)
    }

    val state: StateFlow<AnalyticsUiState> = combine(
        dayInputs,
        repository.getUserGoal().map { it ?: UserGoal.default() },
        inputs
    ) { days, goal, i ->
        AnalyticsAggregator.buildState(
            days = days,
            goal = goal,
            today = today,
            period = i.period,
            includeExercise = i.includeExercise,
            includeTef = i.includeTef,
            selectedBucketIndex = i.selected,
            locale = Locale.getDefault()
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun setPeriod(newPeriod: TimePeriod) {
        if (period.value == newPeriod) return
        period.value = newPeriod
        selected.value = null
    }

    fun setIncludeExercise(value: Boolean) = includeExercise.update { value }

    fun setIncludeTef(value: Boolean) = includeTef.update { value }

    /** Null clears the selection (summary covers the whole range). */
    fun selectBucket(index: Int?) {
        selected.value = index
    }
}
