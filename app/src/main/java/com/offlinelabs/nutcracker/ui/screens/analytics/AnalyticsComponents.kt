package com.offlinelabs.nutcracker.ui.screens.analytics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme
import com.offlinelabs.nutcracker.ui.theme.nutrientCarbsColor
import com.offlinelabs.nutcracker.ui.theme.nutrientFatColor
import com.offlinelabs.nutcracker.ui.theme.nutrientFiberColor
import com.offlinelabs.nutcracker.ui.theme.nutrientProteinColor
import com.offlinelabs.nutcracker.ui.theme.successColor
import kotlin.math.max
import kotlin.math.min

/*
 * Reusable analytics building blocks (Nutrition tab now, Exercise tab next). They follow the form
 * kit: colors from FormDefaults.colors(), 16dp rounded surfaces, MaterialTheme typography.
 */

private val CardShape = RoundedCornerShape(16.dp)
private val TileShape = RoundedCornerShape(16.dp)

/** Max width of analytics content, so cards don't stretch on tablets and landscape. */
val AnalyticsMaxContentWidth: Dp = 600.dp

// ---------------------------------------------------------------------------------------------
// Card
// ---------------------------------------------------------------------------------------------

/**
 * Rounded card for one analytics block: optional [title] / [subtitle] header with a [trailing]
 * slot (e.g. a chip), then [content]. Uses the form kit's container color with a hairline border.
 */
@Composable
fun AnalyticsCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    colors: FormColors = FormDefaults.colors(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = colors.container,
        contentColor = colors.content,
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            if (title != null || trailing != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (title != null) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.content
                            )
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.contentSecondary
                            )
                        }
                    }
                    if (trailing != null) {
                        Spacer(Modifier.width(8.dp))
                        trailing()
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Stat tiles
// ---------------------------------------------------------------------------------------------

/**
 * Meaning of a tile. [Positive] / [Negative] are status tones (e.g. under / over goal) and must be
 * paired with a text label; color is never the only signal.
 */
enum class StatTone { Neutral, Accent, Positive, Negative }

/** Ink/icon color of this tone for the current theme. */
@Composable
fun StatTone.color(colors: FormColors = FormDefaults.colors()): Color = when (this) {
    StatTone.Neutral -> colors.contentSecondary
    StatTone.Accent -> colors.accent
    StatTone.Positive -> successColor()
    StatTone.Negative -> colors.error
}

/**
 * KPI tile: small [label], prominent [value], optional [supporting] line and [icon].
 * Status tones tint the icon, the supporting text and (very lightly) the tile background.
 * In a row, give tiles `Modifier.weight(1f).fillMaxHeight()` inside [StatTileRow] for equal heights.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    tone: StatTone = StatTone.Neutral,
    colors: FormColors = FormDefaults.colors(),
    // Text values (e.g. a list of names) read better smaller and capped, not in the number style
    valueStyle: TextStyle = MaterialTheme.typography.titleLarge,
    valueMaxLines: Int = Int.MAX_VALUE
) {
    val toneColor = tone.color(colors)
    val isStatus = tone == StatTone.Positive || tone == StatTone.Negative
    val background = if (isStatus) toneColor.copy(alpha = 0.08f).compositeOver(colors.container) else colors.container
    val iconTint = if (tone == StatTone.Neutral) colors.accent else toneColor
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = TileShape,
        color = background,
        contentColor = colors.content,
        border = BorderStroke(1.dp, if (isStatus) toneColor.copy(alpha = 0.35f) else colors.divider)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconTint.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.contentSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = valueStyle,
                fontWeight = FontWeight.SemiBold,
                color = colors.content,
                maxLines = valueMaxLines,
                overflow = TextOverflow.Ellipsis
            )
            if (supporting != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isStatus) FontWeight.Medium else FontWeight.Normal,
                    color = if (isStatus) toneColor else colors.contentSecondary
                )
            }
        }
    }
}

/** Row of equal-height tiles; give each child `Modifier.weight(1f).fillMaxHeight()`. */
@Composable
fun StatTileRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

// ---------------------------------------------------------------------------------------------
// Hero figure
// ---------------------------------------------------------------------------------------------

/**
 * The one headline number of a view, inside an [AnalyticsCard]: [label] above, big [value] with
 * its [unit] and an optional [comparison] ("of 2,000 kcal goal") on the same baseline (wrapping
 * on narrow screens), then a [caption] (date range). [action] sits top-right (e.g. "Show all").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    comparison: String? = null,
    caption: String? = null,
    action: (@Composable () -> Unit)? = null,
    colors: FormColors = FormDefaults.colors()
) {
    AnalyticsCard(modifier = modifier, colors = colors, contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 32.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.contentSecondary,
                modifier = Modifier.weight(1f)
            )
            if (action != null) {
                Spacer(Modifier.width(8.dp))
                action()
            }
        }
        Spacer(Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp, lineHeight = 50.sp),
                fontWeight = FontWeight.SemiBold,
                color = colors.content,
                modifier = Modifier.alignByBaseline()
            )
            if (unit != null) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.content,
                    modifier = Modifier.alignByBaseline()
                )
            }
            if (comparison != null) {
                Text(
                    text = comparison,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.contentSecondary,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
        if (caption != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = colors.contentSecondary
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Segmented tabs
// ---------------------------------------------------------------------------------------------

@Immutable
data class SegmentedTab(val label: String, val icon: ImageVector? = null)

/**
 * Segmented control (pill track, accent-filled selected segment). Segments share the width
 * equally and keep equal heights when a long translation wraps.
 */
@Composable
fun SegmentedTabs(
    tabs: List<SegmentedTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: FormColors = FormDefaults.colors()
) {
    val pill = RoundedCornerShape(50)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = pill,
        color = colors.section,
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val background by animateColorAsState(
                    targetValue = if (selected) colors.accent else colors.accent.copy(alpha = 0f),
                    animationSpec = tween(200),
                    label = "segmentBackground"
                )
                val content by animateColorAsState(
                    targetValue = if (selected) colors.onAccent else colors.content,
                    animationSpec = tween(200),
                    label = "segmentContent"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 40.dp)
                        .clip(pill)
                        .background(background)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (tab.icon != null) {
                            Icon(tab.icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = content,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Bar chart
// ---------------------------------------------------------------------------------------------

/**
 * One column of a [BarChart]. [value] null = nothing logged (drawn as a faint stub, not a bar).
 * [contentDescription] is read by TalkBack for the column, e.g. "Nov 10: 1,850 kcal average".
 */
@Immutable
data class BarChartEntry(
    val value: Double?,
    val label: AxisLabel,
    val contentDescription: String
)

/**
 * Single-series column chart on Canvas: hairline gridlines with compact y labels, rounded bar
 * tops anchored to the baseline, an optional dashed reference line ([referenceValue], e.g. the
 * daily goal) with a legend entry ([referenceLabel]), and two-line x labels that never overlap
 * (labels are measured; every k-th is shown, the group line only where it changes).
 *
 * Selection: with [selectedIndex] null every bar uses [barColor]; otherwise the selected bar keeps
 * it, shows its value above, and the others are muted. Each column (full height) is a tap target
 * calling [onBarClick] with its index; toggling (tap again to clear) is up to the caller.
 */
@Composable
fun BarChart(
    entries: List<BarChartEntry>,
    selectedIndex: Int?,
    onBarClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    referenceValue: Double? = null,
    referenceLabel: String? = null,
    valueFormatter: (Double) -> String = { formatWhole(it) },
    axisFormatter: (Double) -> String = { formatAxisValue(it) },
    barColor: Color = FormDefaults.colors().accent,
    plotHeight: Dp = 180.dp,
    colors: FormColors = FormDefaults.colors()
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val baseStyle = MaterialTheme.typography.labelSmall
    val axisStyle = remember(baseStyle, colors.contentSecondary) {
        baseStyle.copy(color = colors.contentSecondary, fontSize = 11.sp, letterSpacing = 0.sp)
    }
    val selectedAxisStyle = remember(axisStyle, colors.content) {
        axisStyle.copy(color = colors.content, fontWeight = FontWeight.SemiBold)
    }
    val groupStyle = remember(axisStyle) { axisStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal) }
    val valueStyle = remember(baseStyle, colors.content) {
        baseStyle.copy(color = colors.content, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    }

    val maxValue = max(entries.maxOfOrNull { it.value ?: 0.0 } ?: 0.0, referenceValue ?: 0.0)
    val ticks = remember(maxValue) { niceAxisTicks(maxValue) }
    val top = ticks.last()
    val tickLayouts = remember(ticks, axisStyle) { ticks.map { measurer.measure(axisFormatter(it), axisStyle) } }
    val primaryLayouts = remember(entries, selectedIndex, axisStyle, selectedAxisStyle) {
        entries.mapIndexed { i, e ->
            measurer.measure(e.label.primary, if (i == selectedIndex) selectedAxisStyle else axisStyle)
        }
    }
    val groupLayouts = remember(entries, groupStyle) {
        entries.map { e -> e.label.group?.let { measurer.measure(it, groupStyle) } }
    }
    val selectedValue = selectedIndex?.let { entries.getOrNull(it)?.value }
    val valueLayout = remember(selectedValue, valueStyle) {
        selectedValue?.let { measurer.measure(valueFormatter(it), valueStyle) }
    }
    val valueTextHeight = remember(valueStyle) { measurer.measure("0", valueStyle).size.height }
    val axisTextHeight = remember(axisStyle) { measurer.measure("0", axisStyle).size.height }

    val gapPx = with(density) { 6.dp.toPx() }
    val valueBandPx = valueTextHeight + with(density) { 8.dp.toPx() }
    val plotPx = with(density) { plotHeight.toPx() }
    val primaryHeightPx = primaryLayouts.maxOfOrNull { it.size.height } ?: axisTextHeight
    val groupHeightPx = groupLayouts.maxOfOrNull { it?.size?.height ?: 0 } ?: 0
    val xBandPx = gapPx + primaryHeightPx + (if (groupHeightPx > 0) groupHeightPx + with(density) { 1.dp.toPx() } else 0f)
    val gutterPx = (tickLayouts.maxOfOrNull { it.size.width } ?: 0) + with(density) { 8.dp.toPx() }
    val totalHeight = with(density) { (valueBandPx + plotPx + xBandPx).toDp() }
    val gutterDp = with(density) { gutterPx.toDp() }

    val gridColor = colors.divider
    val baselineColor = colors.contentSecondary.copy(alpha = 0.45f)
    val referenceColor = colors.content.copy(alpha = 0.6f)
    val stubColor = colors.contentSecondary.copy(alpha = 0.3f)
    val mutedBarColor = barColor.copy(alpha = 0.32f)
    val labelBackground = colors.container.copy(alpha = 0.92f)

    Column(modifier = modifier.fillMaxWidth()) {
        if (referenceValue != null && referenceLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(width = 18.dp, height = 2.dp)) {
                    drawLine(
                        color = referenceColor,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = size.height,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = referenceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.contentSecondary
                )
            }
        }
        if (entries.isEmpty()) return@Column

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val slot = (widthPx - gutterPx) / entries.size
            val visible = visibleLabelIndices(primaryLayouts.map { it.size.width.toFloat() }, slot, gapPx, selectedIndex)
            val groupsShown = groupLabelIndices(
                visible,
                entries.map { it.label.group },
                groupLayouts.map { it?.size?.width?.toFloat() ?: 0f },
                slot,
                gapPx
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val plotBottom = valueBandPx + plotPx
                fun yOf(v: Double): Float = plotBottom - (v / top).toFloat() * plotPx
                fun centerOf(i: Int): Float = gutterPx + slot * (i + 0.5f)

                // Gridlines and y labels
                ticks.forEachIndexed { i, t ->
                    val y = yOf(t)
                    drawLine(
                        color = if (i == 0) baselineColor else gridColor,
                        start = Offset(gutterPx, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    val l = tickLayouts[i]
                    drawText(l, topLeft = Offset(gutterPx - 8.dp.toPx() - l.size.width, y - l.size.height / 2f))
                }

                // Bars
                val barWidth = min(slot * 0.62f, 24.dp.toPx())
                entries.forEachIndexed { i, e ->
                    val left = centerOf(i) - barWidth / 2
                    val v = e.value
                    if (v == null) {
                        val h = 3.dp.toPx()
                        drawRoundRect(
                            color = stubColor,
                            topLeft = Offset(left, plotBottom - h),
                            size = Size(barWidth, h),
                            cornerRadius = CornerRadius(h / 2)
                        )
                    } else {
                        val h = max((v / top).toFloat() * plotPx, (if (v > 0) 2 else 1).dp.toPx())
                        val r = CornerRadius(min(4.dp.toPx(), min(barWidth / 2, h)))
                        val path = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = left,
                                    top = plotBottom - h,
                                    right = left + barWidth,
                                    bottom = plotBottom,
                                    topLeftCornerRadius = r,
                                    topRightCornerRadius = r
                                )
                            )
                        }
                        drawPath(path, if (selectedIndex == null || selectedIndex == i) barColor else mutedBarColor)
                    }
                }

                // Reference (goal) line, over the bars so it stays visible
                if (referenceValue != null && referenceValue > 0) {
                    val y = yOf(referenceValue)
                    drawLine(
                        color = referenceColor,
                        start = Offset(gutterPx, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
                    )
                }

                // Selected value above its bar
                if (selectedIndex != null && valueLayout != null && selectedValue != null) {
                    val pad = 4.dp.toPx()
                    val w = valueLayout.size.width + pad * 2
                    val h = valueLayout.size.height.toFloat()
                    val x = (centerOf(selectedIndex) - w / 2).coerceIn(0f, max(0f, size.width - w))
                    val y = (yOf(selectedValue) - h - 4.dp.toPx()).coerceAtLeast(0f)
                    drawRoundRect(labelBackground, Offset(x, y), Size(w, h), CornerRadius(pad))
                    drawText(valueLayout, topLeft = Offset(x + pad, y))
                }

                // X labels
                val labelTop = plotBottom + gapPx
                for (i in visible) drawCenteredText(primaryLayouts[i], centerOf(i), labelTop)
                for (i in groupsShown) {
                    val g = groupLayouts[i] ?: continue
                    drawCenteredText(g, centerOf(i), labelTop + primaryHeightPx + 1.dp.toPx())
                }
            }

            // Full-height tap targets + accessibility, one per column
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = gutterDp)
            ) {
                entries.forEachIndexed { i, e ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = i == selectedIndex,
                                interactionSource = null,
                                indication = null,
                                onClick = { onBarClick(i) }
                            )
                            .semantics { contentDescription = e.contentDescription }
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCenteredText(
    layout: TextLayoutResult,
    centerX: Float,
    top: Float
) {
    val w = layout.size.width.toFloat()
    val x = (centerX - w / 2).coerceIn(0f, max(0f, size.width - w))
    drawText(layout, topLeft = Offset(x, top))
}

// ---------------------------------------------------------------------------------------------
// Progress rows and share bar
// ---------------------------------------------------------------------------------------------

/**
 * One nutrient vs its goal: colored dot, [label] with [amountText] ("182 g / 250 g") below it, the
 * percentage on the right and a progress bar underneath (fill capped at 100%). Above 100% the percentage gets an up arrow and a
 * status tone: [Negative][StatTone.Negative] by default, [Positive][StatTone.Positive] when
 * [overIsGood] (e.g. fiber). [fraction] null (no goal or no data) hides the percentage and fill.
 * [enabled] false greys the row out (no logged days).
 */
@Composable
fun MacroProgressRow(
    label: String,
    color: Color,
    amountText: String,
    fraction: Double?,
    modifier: Modifier = Modifier,
    overIsGood: Boolean = false,
    enabled: Boolean = true,
    colors: FormColors = FormDefaults.colors()
) {
    val over = fraction != null && fraction > 1.0 + 1e-9
    val overColor = (if (overIsGood) StatTone.Positive else StatTone.Negative).color(colors)
    val overText = stringResource(R.string.analytics_over_goal)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .semantics(mergeDescendants = true) { if (over) stateDescription = overText }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.content
                )
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.contentSecondary
                )
            }
            if (fraction != null) {
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier.heightIn(min = 20.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (over) {
                        Icon(
                            Icons.Filled.ArrowUpward,
                            contentDescription = null,
                            tint = overColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = formatPercent(Math.round(fraction * 100).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (over) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (over) overColor else colors.contentSecondary,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
        ) {
            if (fraction != null && fraction > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0.0, 1.0).toFloat())
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Immutable
data class ShareSegment(val label: String, val percent: Int, val color: Color)

/**
 * Slim part-to-whole bar (8dp, rounded ends, 2dp gaps) with a legend of dot + "label percent".
 * Segments at 0% draw nothing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareBar(
    segments: List<ShareSegment>,
    modifier: Modifier = Modifier,
    colors: FormColors = FormDefaults.colors()
) {
    val visible = segments.filter { it.percent > 0 }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            visible.forEach { s ->
                Box(
                    modifier = Modifier
                        .weight(s.percent.toFloat())
                        .fillMaxHeight()
                        .background(s.color)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            segments.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(s.color)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = s.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.contentSecondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatPercent(s.percent),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.content
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------------------------

/** Friendly empty state: tinted icon, [title] and [message], centred. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.BarChart,
    colors: FormColors = FormDefaults.colors()
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.content,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.contentSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Previews (fake data)
// ---------------------------------------------------------------------------------------------

/** Themed background for analytics previews. */
@Composable
internal fun AnalyticsPreviewSurface(dark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    FoodLogTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

private fun previewMonthEntries(): List<BarChartEntry> {
    val start = java.time.LocalDate.of(2025, 12, 1)
    val starts = (0 until 12).map { start.plusMonths(it.toLong()) }
    val values = listOf(1850.0, 2100.0, null, 1950.0, 2250.0, 1780.0, 2010.0, 1890.0, 2400.0, 1700.0, 2050.0, 1920.0)
    return axisLabels(TimePeriod.MONTHS, starts).mapIndexed { i, label ->
        BarChartEntry(values[i], label, "${label.primary}: ${values[i]}")
    }
}

@Composable
private fun ComponentsPreviewContent() {
    SegmentedTabs(
        tabs = listOf(SegmentedTab("Nutrition", Icons.Filled.Restaurant), SegmentedTab("Exercise", Icons.Filled.Whatshot)),
        selectedIndex = 0,
        onSelect = {}
    )
    StatTileRow {
        StatTile(
            label = "Avg burned / day", value = "160 kcal", supporting = "960 kcal in total",
            icon = Icons.Filled.Whatshot, modifier = Modifier.weight(1f).fillMaxHeight()
        )
        StatTile(
            label = "Avg balance / day", value = "−150 kcal", supporting = "under goal",
            icon = Icons.AutoMirrored.Filled.TrendingDown, tone = StatTone.Positive,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
    StatTileRow {
        StatTile(
            label = "Days logged", value = "5", supporting = "of 7 days",
            icon = Icons.Filled.CalendarMonth, modifier = Modifier.weight(1f).fillMaxHeight()
        )
        StatTile(
            label = "Avg balance / day", value = "+240 kcal", supporting = "over goal",
            icon = Icons.AutoMirrored.Filled.TrendingUp, tone = StatTone.Negative,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
    AnalyticsCard(title = "Intake", subtitle = "Average kcal per logged day") {
        BarChart(
            entries = previewMonthEntries(),
            selectedIndex = 8,
            onBarClick = {},
            referenceValue = 2000.0,
            referenceLabel = "Goal 2,000 kcal"
        )
    }
    AnalyticsCard(title = "Macros", subtitle = "Average per logged day vs goal") {
        ShareBar(
            segments = listOf(
                ShareSegment("Carbs", 48, nutrientCarbsColor()),
                ShareSegment("Protein", 22, nutrientProteinColor()),
                ShareSegment("Fat", 30, nutrientFatColor())
            )
        )
        Spacer(Modifier.height(16.dp))
        MacroProgressRow("Carbs", nutrientCarbsColor(), "182 g / 250 g", 0.73)
        Spacer(Modifier.height(12.dp))
        MacroProgressRow("Fiber", nutrientFiberColor(), "34 g / 30 g", 1.13, overIsGood = true)
        Spacer(Modifier.height(12.dp))
        MacroProgressRow("Salt", FormDefaults.colors().contentSecondary, "6,200 mg / 5,750 mg", 1.08)
        Spacer(Modifier.height(12.dp))
        MacroProgressRow("Fat", nutrientFatColor(), "—", null, enabled = false)
    }
    AnalyticsCard {
        EmptyState(title = "No meals logged yet", message = "Log meals and your trends for this period will appear here.")
    }
}

@Preview(name = "Analytics components - light", widthDp = 412, heightDp = 1500, showBackground = true)
@Composable
private fun AnalyticsComponentsLightPreview() {
    AnalyticsPreviewSurface(dark = false) { ComponentsPreviewContent() }
}

@Preview(name = "Analytics components - dark", widthDp = 412, heightDp = 1500, showBackground = true)
@Composable
private fun AnalyticsComponentsDarkPreview() {
    AnalyticsPreviewSurface(dark = true) { ComponentsPreviewContent() }
}
