package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.GoalLinkMode
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A pill-shaped button (tinted, outlined, with a swap icon) so it reads as tappable.
        Surface(
            onClick = onToggle,
            shape = RoundedCornerShape(999.dp),
            color = colors.accent.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.45f)),
            modifier = Modifier
                .weight(1f, fill = false)
                .semantics { contentDescription = description }
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (mode == GoalLinkMode.MACROS_TO_KCAL) {
                        Icons.Filled.ArrowUpward
                    } else {
                        Icons.Filled.ArrowDownward
                    },
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.content,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
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

@Composable
private fun GoalLinkRowPreviewContent() {
    val colors = FormDefaults.colors()
    Column(
        modifier = Modifier
            .background(colors.section)
            .padding(16.dp)
    ) {
        GoalLinkRow(GoalLinkMode.KCAL_TO_MACROS, {}, colors)
        GoalLinkRow(GoalLinkMode.MACROS_TO_KCAL, {}, colors)
    }
}

@Preview(name = "Link row - light", widthDp = 360, showBackground = true)
@Composable
private fun GoalLinkRowLightPreview() {
    FoodLogTheme(darkTheme = false) { GoalLinkRowPreviewContent() }
}

@Preview(name = "Link row - dark", widthDp = 360, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun GoalLinkRowDarkPreview() {
    FoodLogTheme(darkTheme = true) { GoalLinkRowPreviewContent() }
}
