package com.offlinelabs.nutcracker.ui.screens.analytics

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/*
 * Pure formatting and layout helpers for the analytics UI. No Compose here, so everything is
 * unit-tested (AnalyticsFormatTest).
 */

/** Minus sign used for negative signed values (typographic, not the hyphen). */
const val MINUS_SIGN = '−'

// ---------------------------------------------------------------------------------------------
// Numbers
// ---------------------------------------------------------------------------------------------

/** Whole number with the [locale]'s grouping, rounded half-up: 1849.6 -> "1,850" (en) / "1.850" (es). */
fun formatWhole(value: Double, locale: Locale = Locale.getDefault()): String {
    if (!value.isFinite()) return ""
    val format = NumberFormat.getIntegerInstance(locale).apply {
        roundingMode = RoundingMode.HALF_UP
        isGroupingUsed = true
    }
    val formatted = format.format(BigDecimal.valueOf(value))
    return if (formatted == "-0") "0" else formatted
}

/** Signed whole number: "+150", "−200" (U+2212) or "0" when it rounds to zero. */
fun formatSignedWhole(value: Double, locale: Locale = Locale.getDefault()): String {
    if (!value.isFinite()) return ""
    val rounded = roundHalfUp(value)
    return when {
        rounded > 0 -> "+" + formatWhole(rounded.toDouble(), locale)
        rounded < 0 -> MINUS_SIGN + formatWhole(abs(rounded).toDouble(), locale)
        else -> formatWhole(0.0, locale)
    }
}

/**
 * Compact y-axis label: whole numbers in full below 10,000 ("0", "1,500"), then thousands with a
 * "k" suffix and at most one decimal ("12k", "12.5k" / "12,5k").
 */
fun formatAxisValue(value: Double, locale: Locale = Locale.getDefault()): String {
    if (!value.isFinite()) return ""
    if (abs(value) < 10_000) return formatWhole(value, locale)
    val format = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
        roundingMode = RoundingMode.HALF_UP
        isGroupingUsed = true
    }
    return format.format(BigDecimal.valueOf(value / 1000.0)) + "k"
}

/** Percent of a whole, e.g. 48 -> "48%" (en) / "48 %" (es). */
fun formatPercent(percent: Int, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getPercentInstance(locale).format(percent / 100.0)

private fun roundHalfUp(value: Double): Long =
    BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toLong()

// ---------------------------------------------------------------------------------------------
// Goal status
// ---------------------------------------------------------------------------------------------

/** Where an average balance (net − goal, positive = over) sits once rounded to whole kcal. */
enum class GoalStatus { UNDER, ON, OVER }

fun goalStatus(balanceKcal: Double): GoalStatus {
    val rounded = roundHalfUp(balanceKcal)
    return when {
        rounded > 0 -> GoalStatus.OVER
        rounded < 0 -> GoalStatus.UNDER
        else -> GoalStatus.ON
    }
}

/** value / goal, or null when there is no value or no positive goal. */
fun goalFraction(value: Double?, goal: Double): Double? =
    if (value == null || !value.isFinite() || goal <= 0.0) null else value / goal

/**
 * Integer percentages of [values] that add up to exactly 100 (largest remainder method).
 * All zeros when the total isn't positive.
 */
fun roundPercentages(values: List<Double>): List<Int> {
    val clean = values.map { if (it.isFinite() && it > 0) it else 0.0 }
    val total = clean.sum()
    if (total <= 0.0) return clean.map { 0 }
    val exact = clean.map { it / total * 100.0 }
    val floors = exact.map { floor(it).toInt() }.toMutableList()
    var missing = 100 - floors.sum()
    val byRemainder = exact.indices.sortedByDescending { exact[it] - floors[it] }
    for (i in byRemainder) {
        if (missing <= 0) break
        floors[i]++
        missing--
    }
    return floors
}

/** Share of energy from carbs / protein / fat (4 / 4 / 9 kcal per g), or null without data. */
fun macroEnergyPercentages(carbsG: Double?, proteinG: Double?, fatG: Double?): List<Int>? {
    val kcal = listOf((carbsG ?: 0.0) * 4.0, (proteinG ?: 0.0) * 4.0, (fatG ?: 0.0) * 9.0)
    if (kcal.none { it.isFinite() && it > 0.0 }) return null
    return roundPercentages(kcal)
}

// ---------------------------------------------------------------------------------------------
// Chart axes
// ---------------------------------------------------------------------------------------------

/**
 * Clean y-axis ticks from 0 to a "nice" top that is >= [maxValue]: steps of 1, 2, 2.5 or 5 times a
 * power of ten, about [targetIntervals] intervals. E.g. 2,300 -> [0, 1000, 2000, 3000],
 * 1,800 -> [0, 500, 1000, 1500, 2000].
 */
fun niceAxisTicks(maxValue: Double, targetIntervals: Int = 4): List<Double> {
    val max = if (maxValue.isFinite() && maxValue > 0) maxValue else 1.0
    val intervals = targetIntervals.coerceAtLeast(1)
    val rawStep = max / intervals
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0)
        .map { it * magnitude }
        .first { it >= rawStep * (1 - 1e-9) }
    val count = ceil(max / step * (1 - 1e-9)).toInt().coerceAtLeast(1)
    return (0..count).map { (it * step * 1e6).roundToLong() / 1e6 }
}

/** Smallest k so that labels every k slots don't touch: k * slot >= widest label + gap. */
fun labelStep(maxLabelWidth: Float, slotWidth: Float, minGap: Float): Int {
    if (slotWidth <= 0f) return 1
    return ceil(((maxLabelWidth + minGap) / slotWidth).toDouble()).toInt().coerceAtLeast(1)
}

/**
 * Which x-axis labels to draw so none overlap. Labels sit centred under slots of [slotWidth];
 * [widths] are the measured label widths. Every k-th label is kept, anchored on the newest (last)
 * bucket. A [selected] label is always shown; neighbours that would touch it are dropped.
 */
fun visibleLabelIndices(
    widths: List<Float>,
    slotWidth: Float,
    minGap: Float,
    selected: Int? = null
): List<Int> {
    val n = widths.size
    if (n == 0) return emptyList()
    val step = labelStep(widths.max(), slotWidth, minGap)
    val shown = (n - 1 downTo 0 step step).toMutableList()
    if (selected != null && selected in widths.indices && selected !in shown) {
        shown.removeAll { j -> !labelsFit(j, selected, widths[j], widths[selected], slotWidth, minGap) }
        shown += selected
    }
    return shown.sorted()
}

/**
 * Which of the [visible] labels also get their group caption (month under week days, year under
 * months, ...): the first visible label and every visible label whose group differs from the
 * previous visible one, minus any caption that would touch the caption kept before it.
 */
fun groupLabelIndices(
    visible: List<Int>,
    groups: List<String?>,
    groupWidths: List<Float>,
    slotWidth: Float,
    minGap: Float
): List<Int> {
    val result = mutableListOf<Int>()
    var previousGroup: String? = null
    var first = true
    for (i in visible.sorted()) {
        val group = groups.getOrNull(i)
        val isChange = first || group != previousGroup
        first = false
        previousGroup = group
        if (group == null || !isChange) continue
        val last = result.lastOrNull()
        if (last == null ||
            labelsFit(last, i, groupWidths.getOrElse(last) { 0f }, groupWidths.getOrElse(i) { 0f }, slotWidth, minGap)
        ) {
            result += i
        }
    }
    return result
}

private fun labelsFit(i: Int, j: Int, wi: Float, wj: Float, slot: Float, gap: Float): Boolean =
    abs(i - j) * slot >= (wi + wj) / 2f + gap

/** Two-line x-axis label: [primary] under every shown bar, [group] only where it changes. */
data class AxisLabel(val primary: String, val group: String?)

/**
 * Chart x labels for buckets starting at [starts]:
 * - DAYS: weekday ("Thu") over day of month ("20");
 * - WEEKS: day of month of the week start ("10") over its month ("Nov");
 * - MONTHS: month ("Dec") over year ("2025").
 * The group line is shown by the chart only where it changes, so a year appears once per year.
 */
fun axisLabels(period: TimePeriod, starts: List<LocalDate>, locale: Locale = Locale.getDefault()): List<AxisLabel> =
    starts.map { d ->
        when (period) {
            TimePeriod.DAYS -> AxisLabel(
                capitalize(d.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), locale),
                d.dayOfMonth.toString()
            )
            TimePeriod.WEEKS -> AxisLabel(
                d.dayOfMonth.toString(),
                capitalize(d.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale), locale)
            )
            TimePeriod.MONTHS -> AxisLabel(
                capitalize(d.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale), locale),
                d.year.toString()
            )
        }
    }

// ---------------------------------------------------------------------------------------------
// Dates
// ---------------------------------------------------------------------------------------------

/**
 * Localized date span:
 * - one day: full date ("Thursday, November 20, 2026");
 * - [monthly] and one month: "November 2026"; [monthly] range: "Dec 2025 – Nov 2026";
 * - otherwise medium dates: "Nov 10, 2026 – Nov 16, 2026".
 */
fun formatDateRange(start: LocalDate, end: LocalDate, monthly: Boolean, locale: Locale = Locale.getDefault()): String {
    if (monthly) {
        if (YearMonth.from(start) == YearMonth.from(end)) {
            return capitalize(DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(start), locale)
        }
        val f = DateTimeFormatter.ofPattern("LLL yyyy", locale)
        return "${capitalize(f.format(start), locale)} – ${capitalize(f.format(end), locale)}"
    }
    if (start == end) {
        return capitalize(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(start), locale)
    }
    val medium = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    return "${medium.format(start)} – ${medium.format(end)}"
}

/** Medium localized date, e.g. "Nov 10, 2026" (en) / "10 nov 2026" (es). */
fun formatMediumDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)

/** Last day of [summary] that has already happened (the span may end in the future). */
fun elapsedEnd(summary: AnalyticsSummary): LocalDate =
    if (summary.daysInRange > 0) summary.startDate.plusDays((summary.daysInRange - 1).toLong()) else summary.endDate

private fun capitalize(text: String, locale: Locale): String =
    text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
