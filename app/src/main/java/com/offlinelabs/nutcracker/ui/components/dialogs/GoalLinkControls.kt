package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.GoalLinkMode
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import kotlinx.coroutines.launch

/**
 * Row under the calories field: link-mode toggle (Down / Up), a one-line caption
 * for the current mode, and a "?" that explains the Real value.
 */
@Composable
fun GoalLinkRow(mode: GoalLinkMode, onToggle: () -> Unit, colors: FormColors) {
    val description = stringResource(
        when (mode) {
            GoalLinkMode.KCAL_TO_MACROS -> R.string.goal_link_kcal_to_macros_desc
            GoalLinkMode.MACROS_TO_KCAL -> R.string.goal_link_macros_to_kcal_desc
        }
    )
    val caption = stringResource(
        when (mode) {
            GoalLinkMode.KCAL_TO_MACROS -> R.string.goal_link_kcal_to_macros_caption
            GoalLinkMode.MACROS_TO_KCAL -> R.string.goal_link_macros_to_kcal_caption
        }
    )
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (mode == GoalLinkMode.MACROS_TO_KCAL) {
                    Icons.Filled.ArrowUpward
                } else {
                    Icons.Filled.ArrowDownward
                },
                contentDescription = description,
                tint = colors.accent,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = colors.contentSecondary,
            modifier = Modifier.weight(1f)
        )
        RealKcalHelp(colors)
    }
}

/** "?" button that opens a persistent tooltip explaining the Real kcal value. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RealKcalHelp(colors: FormColors) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip {
                Text(
                    text = stringResource(R.string.real_kcal_help),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        state = tooltipState
    ) {
        IconButton(onClick = { scope.launch { tooltipState.show() } }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = stringResource(R.string.real_kcal_help_desc),
                tint = colors.contentSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
