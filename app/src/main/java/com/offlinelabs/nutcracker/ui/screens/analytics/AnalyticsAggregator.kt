package com.offlinelabs.nutcracker.ui.screens.analytics

import com.offlinelabs.nutcracker.data.dao.DailyExerciseCaloriesRow
import com.offlinelabs.nutcracker.data.dao.DailyFoodTotalsRow
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.utils.TEFCalculator
import java.time.LocalDate
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * One calendar day of raw data. [checkInCount] > 0 means the day is "logged"; only logged days
 * take part in averages, balances and the streak.
 */
data class AnalyticsDayInput(
    val date: LocalDate,
    val consumedKcal: Double,
    val carbsG: Double,
    val proteinG: Double,
    val fatG: Double,
    val fiberG: Double,
    val saltMg: Double,
    val exerciseKcal: Double,
    val checkInCount: Int
) {
    val isLogged: Boolean get() = checkInCount > 0
}

/** Goals used for comparison. Salt is in mg, the rest of the macros in g. */
data class AnalyticsGoals(
    val kcal: Int,
    val carbsG: Int,
    val proteinG: Int,
    val fatG: Int,
    val fiberG: Int,
    val saltMg: Int
)

/**
 * Aggregated numbers for a date span. Everything is computed over LOGGED days only.
 *
 * - [consumedKcal] = sum of food kcal.
 * - [exerciseKcal], [tefKcal] = raw burned kcal / TEF bonus of those days, ALWAYS filled
 *   (independent of the toggles) so the UI can show what a toggle would remove.
 * - [netKcal] = sum over days of max(0, consumed - exercise*(includeExercise) - tef*(includeTef)),
 *   exactly like FoodLogRepository.getDailyCombinedTotals. With both toggles off it equals consumed.
 * - [goalKcal] = daily goal * [loggedDays].
 * - [balanceKcal] = [netKcal] - [goalKcal]. POSITIVE = OVER goal, negative = under (remaining).
 * - averages are null when [loggedDays] == 0.
 */
data class PeriodStats(
    val loggedDays: Int,
    val consumedKcal: Double,
    val exerciseKcal: Double,
    val tefKcal: Double,
    val netKcal: Double,
    val goalKcal: Double,
    val balanceKcal: Double,
    val avgIntakePerLoggedDay: Double?,
    val avgNetPerLoggedDay: Double?,
    val avgBalancePerLoggedDay: Double?,
    val avgCarbsG: Double?,
    val avgProteinG: Double?,
    val avgFatG: Double?,
    val avgFiberG: Double?,
    val avgSaltMg: Double?,
    /** Raw exercise kcal per logged day, independent of the include-exercise toggle. */
    val avgExercisePerLoggedDay: Double? = null
)

data class AnalyticsBucket(
    /** First calendar day of the bucket (week start / first of month / the day). */
    val startDate: LocalDate,
    /** Last calendar day of the bucket; may be in the future for the current week/month. */
    val endDate: LocalDate,
    val label: String,
    val stats: PeriodStats
)

/**
 * Summary for the whole range ([selectedBucketIndex] == null in the state) or for one bucket.
 * [streak] is independent of the selection: consecutive logged days ending today, or ending
 * yesterday when today has no logs yet (so the streak is not shown as 0 all morning).
 * It is computed over the loaded history (last 12 months), so it is capped there.
 */
data class AnalyticsSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val stats: PeriodStats,
    val daysLogged: Int,
    val streak: Int,
    /**
     * Calendar days from [startDate] to min([endDate], today), i.e. the days that could have been
     * logged so far (the "7" in "5 of 7 days"). The span's last elapsed day is
     * startDate.plusDays(daysInRange - 1).
     */
    val daysInRange: Int = 0
)

data class AnalyticsUiState(
    val period: TimePeriod = TimePeriod.DAYS,
    val includeExercise: Boolean = false,
    val includeTef: Boolean = false,
    /** Index into [buckets]; null = no bar selected, [summary] covers the whole range. */
    val selectedBucketIndex: Int? = null,
    val buckets: List<AnalyticsBucket> = emptyList(),
    val summary: AnalyticsSummary? = null,
    val goals: AnalyticsGoals = AnalyticsAggregator.goalsOf(UserGoal.default()),
    /** True when no day with logs exists in the whole range of [period]. */
    val isEmpty: Boolean = true,
    val isLoading: Boolean = true
)

object AnalyticsAggregator {

    const val DAY_BUCKETS = 7
    const val WEEK_BUCKETS = 12
    const val MONTH_BUCKETS = 12

    fun bucketCount(period: TimePeriod): Int = when (period) {
        TimePeriod.DAYS -> DAY_BUCKETS
        TimePeriod.WEEKS -> WEEK_BUCKETS
        TimePeriod.MONTHS -> MONTH_BUCKETS
    }

    /** First day that must be loaded so that every period (and the streak) is covered. */
    fun historyStart(today: LocalDate): LocalDate =
        today.minusMonths((MONTH_BUCKETS - 1).toLong()).withDayOfMonth(1)

    fun goalsOf(goal: UserGoal) = AnalyticsGoals(
        kcal = goal.caloriesGoal,
        carbsG = goal.carbsGoal_g,
        proteinG = goal.proteinGoal_g,
        fatG = goal.fatGoal_g,
        fiberG = goal.fiberGoal_g,
        saltMg = goal.sodiumGoal_mg
    )

    /** Joins the two range queries into one list of day inputs (days with food or exercise). */
    fun mergeRows(
        food: List<DailyFoodTotalsRow>,
        exercise: List<DailyExerciseCaloriesRow>
    ): List<AnalyticsDayInput> {
        val exerciseByDate = exercise.associate { it.logDate to it.caloriesBurned }
        return food.mapNotNull { row ->
            val date = runCatching { LocalDate.parse(row.checkInDate) }.getOrNull() ?: return@mapNotNull null
            AnalyticsDayInput(
                date = date,
                consumedKcal = row.totalCalories,
                carbsG = row.totalCarbohydrates,
                proteinG = row.totalProtein,
                fatG = row.totalFat,
                fiberG = row.totalFiber,
                saltMg = row.totalSaltMg,
                exerciseKcal = exerciseByDate[row.checkInDate] ?: 0.0,
                checkInCount = row.checkInCount
            )
        }
    }

    /** Bucket boundaries (start, end) oldest first. Not clipped to today. */
    fun bucketRanges(period: TimePeriod, today: LocalDate, locale: Locale): List<Pair<LocalDate, LocalDate>> =
        when (period) {
            TimePeriod.DAYS -> (DAY_BUCKETS - 1 downTo 0).map {
                val d = today.minusDays(it.toLong())
                d to d
            }
            TimePeriod.WEEKS -> {
                val first = WeekFields.of(locale).firstDayOfWeek
                val currentStart = today.with(TemporalAdjusters.previousOrSame(first))
                (WEEK_BUCKETS - 1 downTo 0).map {
                    val s = currentStart.minusWeeks(it.toLong())
                    s to s.plusDays(6)
                }
            }
            TimePeriod.MONTHS -> {
                val currentStart = today.withDayOfMonth(1)
                (MONTH_BUCKETS - 1 downTo 0).map {
                    val s = currentStart.minusMonths(it.toLong())
                    s to s.withDayOfMonth(s.lengthOfMonth())
                }
            }
        }

    fun label(period: TimePeriod, start: LocalDate, today: LocalDate, locale: Locale): String = when (period) {
        TimePeriod.DAYS -> start.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        TimePeriod.WEEKS -> weekLabelFormatter(locale).format(start)
        TimePeriod.MONTHS -> {
            val month = start.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale)
            if (start.year != today.year) "$month ${start.year}" else month
        }
    }

    /** "Nov 10" (month first) or "10 nov" (day first), following the locale's short date order. */
    private fun weekLabelFormatter(locale: Locale): DateTimeFormatter {
        val shortPattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale
        )
        val dayFirst = shortPattern.indexOf('d').let { d -> d >= 0 && d < shortPattern.indexOf('M') }
        return DateTimeFormatter.ofPattern(if (dayFirst) "d MMM" else "MMM d", locale)
    }

    /** Aggregates [days] (logged ones, between [start] and min([end], [today])). */
    fun statsFor(
        days: List<AnalyticsDayInput>,
        start: LocalDate,
        end: LocalDate,
        today: LocalDate,
        dailyGoalKcal: Int,
        includeExercise: Boolean,
        includeTef: Boolean
    ): PeriodStats {
        val effectiveEnd = if (end.isAfter(today)) today else end
        val logged = days.filter { it.isLogged && !it.date.isBefore(start) && !it.date.isAfter(effectiveEnd) }
        val n = logged.size
        var consumed = 0.0
        var exercise = 0.0
        var tef = 0.0
        var net = 0.0
        var carbs = 0.0
        var protein = 0.0
        var fat = 0.0
        var fiber = 0.0
        var salt = 0.0
        for (d in logged) {
            val dayTef = TEFCalculator.calculateTEFBonus(d.proteinG, d.carbsG, d.fatG)
            consumed += d.consumedKcal
            exercise += d.exerciseKcal
            tef += dayTef
            var dayNet = d.consumedKcal
            if (includeExercise) dayNet -= d.exerciseKcal
            if (includeTef) dayNet -= dayTef
            net += dayNet.coerceAtLeast(0.0)
            carbs += d.carbsG
            protein += d.proteinG
            fat += d.fatG
            fiber += d.fiberG
            salt += d.saltMg
        }
        val goal = dailyGoalKcal.toDouble() * n
        val balance = net - goal
        fun avg(v: Double): Double? = if (n == 0) null else v / n
        return PeriodStats(
            loggedDays = n,
            consumedKcal = consumed,
            exerciseKcal = exercise,
            tefKcal = tef,
            netKcal = net,
            goalKcal = goal,
            balanceKcal = balance,
            avgIntakePerLoggedDay = avg(consumed),
            avgNetPerLoggedDay = avg(net),
            avgBalancePerLoggedDay = avg(balance),
            avgCarbsG = avg(carbs),
            avgProteinG = avg(protein),
            avgFatG = avg(fat),
            avgFiberG = avg(fiber),
            avgSaltMg = avg(salt),
            avgExercisePerLoggedDay = avg(exercise)
        )
    }

    /** Consecutive logged days ending today, or ending yesterday if today has no logs yet. */
    fun currentStreak(days: List<AnalyticsDayInput>, today: LocalDate): Int {
        val loggedDates = days.filter { it.isLogged && !it.date.isAfter(today) }.map { it.date }.toHashSet()
        var cursor = if (today in loggedDates) today else today.minusDays(1)
        var streak = 0
        while (cursor in loggedDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun buildState(
        days: List<AnalyticsDayInput>,
        goal: UserGoal,
        today: LocalDate,
        period: TimePeriod,
        includeExercise: Boolean,
        includeTef: Boolean,
        selectedBucketIndex: Int?,
        locale: Locale
    ): AnalyticsUiState {
        val ranges = bucketRanges(period, today, locale)
        val buckets = ranges.map { (s, e) ->
            AnalyticsBucket(
                startDate = s,
                endDate = e,
                label = label(period, s, today, locale),
                stats = statsFor(days, s, e, today, goal.caloriesGoal, includeExercise, includeTef)
            )
        }
        val selected = selectedBucketIndex?.takeIf { it in buckets.indices }
        val summaryStart = selected?.let { buckets[it].startDate } ?: ranges.first().first
        val summaryEnd = selected?.let { buckets[it].endDate } ?: ranges.last().second
        val stats = selected?.let { buckets[it].stats }
            ?: statsFor(days, summaryStart, summaryEnd, today, goal.caloriesGoal, includeExercise, includeTef)
        val wholeRangeLogged = buckets.sumOf { it.stats.loggedDays }
        val elapsedEnd = if (summaryEnd.isAfter(today)) today else summaryEnd
        val daysInRange = (ChronoUnit.DAYS.between(summaryStart, elapsedEnd) + 1).toInt().coerceAtLeast(0)
        return AnalyticsUiState(
            period = period,
            includeExercise = includeExercise,
            includeTef = includeTef,
            selectedBucketIndex = selected,
            buckets = buckets,
            summary = AnalyticsSummary(
                startDate = summaryStart,
                endDate = summaryEnd,
                stats = stats,
                daysLogged = stats.loggedDays,
                streak = currentStreak(days, today),
                daysInRange = daysInRange
            ),
            goals = goalsOf(goal),
            isEmpty = wholeRangeLogged == 0,
            isLoading = false
        )
    }
}
