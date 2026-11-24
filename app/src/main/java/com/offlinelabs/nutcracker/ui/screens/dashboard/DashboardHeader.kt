package com.offlinelabs.nutcracker.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.theme.appTextPrimaryColor
import com.offlinelabs.nutcracker.ui.theme.appTextSecondaryColor
import java.time.LocalDate

/**
 * Reports this element's centre and size (grown by [padding] on each side) to the tutorial under
 * [id], so its spotlight can frame it.
 */
@Composable
internal fun Modifier.tutorialTarget(
    id: String,
    padding: Dp,
    register: (String, Offset, Size) -> Unit
): Modifier {
    val paddingPx = with(LocalDensity.current) { padding.toPx() }
    return onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        register(id, bounds.center, Size(bounds.width + paddingPx * 2, bounds.height + paddingPx * 2))
    }
}

/** The dashboard's top bar: help and calendar, the day with arrows, settings and analytics. */
@Composable
internal fun DashboardHeader(
    selectedDate: LocalDate,
    onSelectedDateChange: (LocalDate) -> Unit,
    onOpenCalendar: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    registerElementCoordinates: (String, Offset, Size) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderIcon(Icons.AutoMirrored.Filled.Help, stringResource(R.string.help), onNavigateToHelp)
                HeaderIcon(
                    Icons.Filled.DateRange, stringResource(R.string.select_date), onOpenCalendar,
                    Modifier.tutorialTarget("calendar_icon", 8.dp, registerElementCoordinates)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_day)) {
                    onSelectedDateChange(selectedDate.minusDays(1))
                }
                val dateText = if (selectedDate == LocalDate.now()) {
                    stringResource(R.string.today)
                } else {
                    selectedDate.format(
                        java.time.format.DateTimeFormatter
                            .ofLocalizedDate(java.time.format.FormatStyle.SHORT)
                            .withLocale(java.util.Locale.getDefault())
                    )
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
                DayArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_day)) {
                    onSelectedDateChange(selectedDate.plusDays(1))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderIcon(
                    Icons.Filled.Settings, stringResource(R.string.settings_icon_description), onNavigateToSettings,
                    Modifier.tutorialTarget("settings_icon", 8.dp, registerElementCoordinates)
                )
                HeaderIcon(
                    Icons.Filled.BarChart, stringResource(R.string.progress_details), onNavigateToAnalytics,
                    Modifier.tutorialTarget("analytics_icon", 8.dp, registerElementCoordinates)
                )
            }
        }
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(44.dp)) {
        Icon(icon, contentDescription = description, tint = appTextSecondaryColor(), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun DayArrow(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = description, tint = appTextSecondaryColor(), modifier = Modifier.size(20.dp))
    }
}
