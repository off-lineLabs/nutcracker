package com.offlinelabs.nutcracker.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.dao.DailyTotals
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.ui.components.ExerciseToggle
import com.offlinelabs.nutcracker.ui.components.FilterableHistoryView
import com.offlinelabs.nutcracker.ui.components.MultiPillTracker
import com.offlinelabs.nutcracker.ui.components.TEFToggle
import com.offlinelabs.nutcracker.ui.components.form.formatSaltMg
import com.offlinelabs.nutcracker.ui.theme.ExceededColor
import com.offlinelabs.nutcracker.ui.theme.ProteinFiberColor
import com.offlinelabs.nutcracker.ui.theme.appContainerBackgroundColor
import com.offlinelabs.nutcracker.ui.theme.appTextPrimaryColor
import com.offlinelabs.nutcracker.ui.theme.appTextSecondaryColor
import com.offlinelabs.nutcracker.ui.theme.nutrientCarbsColor
import com.offlinelabs.nutcracker.ui.theme.nutrientFatColor
import com.offlinelabs.nutcracker.ui.theme.nutrientProteinColor
import com.offlinelabs.nutcracker.ui.theme.progressTrackColor
import java.util.Locale

/** The Home tab: calorie ring with the booster toggles, nutrients, supplements and the day's history. */
@Composable
internal fun DashboardState.HomeTab() {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "calorie_ring") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .tutorialTarget("calorie_ring", 16.dp, registerElementCoordinates),
                contentAlignment = Alignment.Center
            ) {
                CaloriesRing(
                    foodCalories = foodCalories,
                    goalCalories = userGoal.caloriesGoal.toDouble(),
                    exerciseCalories = exerciseCaloriesBurned,
                    tefCalories = tefCaloriesBurned,
                    includeExercise = includeExerciseCalories,
                    includeTEF = includeTEFBonus,
                    labelColor = appTextSecondaryColor(),
                    valueColor = appTextPrimaryColor(),
                    consumedColor = appTextPrimaryColor(),
                    goalColor = appTextSecondaryColor()
                )
                ExerciseToggle(
                    isEnabled = includeExerciseCalories,
                    onToggle = { toggleExerciseCalories() },
                    exerciseCalories = exerciseCaloriesBurned,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                TEFToggle(
                    isEnabled = includeTEFBonus,
                    onToggle = { toggleTefBonus() },
                    tefCalories = tefCaloriesBurned,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item(key = "nutrients") {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                            .tutorialTarget("edit_goals_icon", 8.dp, registerElementCoordinates)
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
                NutrientBox(totals = dailyTotalsConsumed, goals = userGoal)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item(key = "supplements") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .tutorialTarget("supplement_pill", 16.dp, registerElementCoordinates),
                contentAlignment = Alignment.Center
            ) {
                MultiPillTracker(
                    pills = pills,
                    pillCheckIns = pillCheckInsMap,
                    onPillToggle = { togglePill(it) },
                    onPillLongPress = { pill -> showEditPillDialog = pill },
                    onAddPill = { if (pills.size < 5) showAddPillDialog = true }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item(key = "history") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .tutorialTarget("check_in_history", 16.dp, registerElementCoordinates),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.recent_check_ins),
                    style = MaterialTheme.typography.titleMedium,
                    color = appTextPrimaryColor()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Deleting is only offered in the edit dialogs
                FilterableHistoryView(
                    mealEntries = dailyMealCheckIns,
                    exerciseEntries = dailyExerciseLogs,
                    onDeleteMeal = { },
                    onDeleteExercise = { },
                    onEditMeal = { checkIn -> showEditMealDialog = checkIn },
                    onEditExercise = { exerciseEntry -> showEditExerciseDialog = exerciseEntry }
                )
            }
        }
    }
}

@Composable
fun NutrientProgressDisplay(
    nutrientName: String,
    consumed: Double,
    goal: Double,
    unit: String,
    labelColor: Color,
    valueColor: Color,
    goalColor: Color,
    wholeNumbers: Boolean = false
) {
    val locale = Locale.getDefault()
    val fiberLabel = stringResource(R.string.fiber_label)
    val isFiber = nutrientName == fiberLabel
    val finalValueColor = when {
        consumed > goal && isFiber -> ProteinFiberColor
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
                    append(if (wholeNumbers) formatSaltMg(consumed, locale) else String.format(locale, "%.1f", consumed))
                }
                withStyle(style = SpanStyle(color = goalColor, fontSize = 12.sp, fontWeight = FontWeight.Normal)) {
                    append(" / ")
                    append(if (wholeNumbers) formatSaltMg(goal, locale) else String.format(locale, "%.1f", goal))
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
    // Remaining calories = goal - food calories + booster bonuses
    val exerciseBonus = if (includeExercise) exerciseCalories else 0.0
    val tefBonus = if (includeTEF) tefCalories else 0.0
    val remaining = goalCalories - foodCalories + exerciseBonus + tefBonus
    val progress = if (goalCalories > 0) (foodCalories / goalCalories).toFloat().coerceIn(0f, 1f) else 0f
    val finalValueColor = if (remaining < 0) ExceededColor else valueColor
    val kcalUnit = stringResource(R.string.unit_kcal)

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val stroke = Stroke(width = strokeWidthDp.toPx(), cap = StrokeCap.Round)
            val diameter = size.minDimension - strokeWidthDp.toPx()
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            // Track (full circle), then the progress
            drawArc(ringTrackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            drawArc(ringProgressColor, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.calories_remaining_label), color = labelColor, fontSize = 12.sp)
            Text(text = remaining.toInt().toString(), color = finalValueColor, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = consumedColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) {
                        append(String.format(Locale.getDefault(), "%.0f", foodCalories))
                    }
                    withStyle(style = SpanStyle(color = goalColor, fontSize = 12.sp, fontWeight = FontWeight.Normal)) {
                        append(" / ")
                        append("${goalCalories.toInt()}")
                        append(" ")
                        append(kcalUnit)
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
    val proteinLabel = stringResource(R.string.protein_label)
    val fiberLabel = stringResource(R.string.fiber_label)
    val isProteinOrFiber = title == proteinLabel || title == fiberLabel
    val finalConsumedColor = when {
        consumed > goal && isProteinOrFiber -> ProteinFiberColor
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
                    .background(Brush.horizontalGradient(listOf(barStartColor, barEndColor)))
            )
        }
    }
}

@Composable
private fun NutrientBox(totals: DailyTotals?, goals: UserGoal) {
    val track = progressTrackColor()
    val gramsUnit = stringResource(R.string.unit_g)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = appContainerBackgroundColor(), shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        NutrientBarRow(
            title = stringResource(id = R.string.carbohydrates_label),
            consumed = totals?.totalCarbohydrates ?: 0.0,
            goal = goals.carbsGoal_g.toDouble(),
            barStartColor = nutrientCarbsColor(),
            barEndColor = nutrientCarbsColor(),
            trackColor = track,
            unit = gramsUnit
        )
        Spacer(modifier = Modifier.height(12.dp))
        NutrientBarRow(
            title = stringResource(id = R.string.protein_label),
            consumed = totals?.totalProtein ?: 0.0,
            goal = goals.proteinGoal_g.toDouble(),
            barStartColor = nutrientProteinColor(),
            barEndColor = nutrientProteinColor(),
            trackColor = track,
            unit = gramsUnit
        )
        Spacer(modifier = Modifier.height(12.dp))
        NutrientBarRow(
            title = stringResource(id = R.string.fat_label),
            consumed = totals?.totalFat ?: 0.0,
            goal = goals.fatGoal_g.toDouble(),
            barStartColor = nutrientFatColor(),
            barEndColor = nutrientFatColor(),
            trackColor = track,
            unit = gramsUnit
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = track)
        // Secondary nutrients
        NutrientProgressDisplay(
            nutrientName = stringResource(id = R.string.fiber_label),
            consumed = totals?.totalFiber ?: 0.0,
            goal = goals.fiberGoal_g.toDouble(),
            unit = gramsUnit,
            labelColor = appTextSecondaryColor(),
            valueColor = appTextPrimaryColor(),
            goalColor = appTextSecondaryColor()
        )
        NutrientProgressDisplay(
            nutrientName = stringResource(id = R.string.sodium_label),
            consumed = totals?.totalSodium ?: 0.0,
            goal = goals.sodiumGoal_mg.toDouble(),
            unit = stringResource(R.string.unit_mg),
            labelColor = appTextSecondaryColor(),
            valueColor = appTextPrimaryColor(),
            goalColor = appTextSecondaryColor(),
            wholeNumbers = true
        )
    }
}
