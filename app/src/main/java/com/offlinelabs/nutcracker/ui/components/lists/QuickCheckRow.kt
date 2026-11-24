package com.offlinelabs.nutcracker.ui.components.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.ui.state.ToggleableState
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.theme.appSurfaceColor

/** Width of one swipe-panel button. */
val SwipeActionWidth = 64.dp

/**
 * A full-width row of the Food / Exercise tab: [content] (image, name, details) and [controls]
 * (number wheels and the check-in box). With [controlsFirst] the controls go on the left, away
 * from a right thumb scrolling the list. Tap opens the item; swipe left reveals [actions]
 * ([actionCount] buttons of [SwipeActionWidth]). Rows are separated by a hairline.
 */
@Composable
fun QuickCheckRow(
    controlsFirst: Boolean,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    actionCount: Int,
    actions: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    controls: @Composable RowScope.() -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val divider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    SwipeActionsRow(
        open = open,
        onOpenChange = onOpenChange,
        actionsWidth = SwipeActionWidth * actionCount,
        shape = RectangleShape,
        // Transparent at rest (the row sits on the page); opaque only while sliding over the buttons
        slidingBackground = appSurfaceColor(),
        actions = actions
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val y = size.height - 0.5.dp.toPx()
                    drawLine(divider, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                // While open, a tap closes the row instead of opening the item
                .clickable { if (open) onOpenChange(false) else onClick() }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (controlsFirst) {
                controls()
                content()
            } else {
                content()
                controls()
            }
        }
    }
}

/**
 * The controls of a [QuickCheckRow]: the check-in box sits at the outer edge ([controlsFirst]:
 * left, otherwise right) with the [wheels] next to it.
 */
@Composable
fun RowScope.QuickControls(
    controlsFirst: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkLabel: String,
    wheels: @Composable RowScope.() -> Unit
) = QuickControls(
    controlsFirst = controlsFirst,
    state = ToggleableState(checked),
    onClick = { onCheckedChange(!checked) },
    checkLabel = checkLabel,
    wheels = wheels
)

/** [QuickControls] with a three-state box (e.g. some of an exercise's sets done). */
@Composable
fun RowScope.QuickControls(
    controlsFirst: Boolean,
    state: ToggleableState,
    onClick: () -> Unit,
    checkLabel: String,
    wheels: @Composable RowScope.() -> Unit
) {
    val box: @Composable () -> Unit = { CheckInBox(state, onClick, checkLabel) }
    if (controlsFirst) {
        box()
        wheels()
    } else {
        wheels()
        box()
    }
}

/** The rows' check-in box in the accent colour: unticked, partly done (a dash) or ticked. */
@Composable
fun CheckInBox(state: ToggleableState, onClick: () -> Unit, label: String) {
    val colors = FormDefaults.colors()
    TriStateCheckbox(
        state = state,
        onClick = onClick,
        colors = CheckboxDefaults.colors(
            checkedColor = colors.accent,
            checkmarkColor = colors.onAccent,
            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.semantics { contentDescription = label }
    )
}

/** One button of the swipe panel: an icon on a coloured square. */
@Composable
fun SwipeAction(
    icon: ImageVector,
    label: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(SwipeActionWidth)
            .fillMaxHeight()
            .background(background)
            .clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = label, tint = content)
    }
}

/** Horizontal padding of the non-row items (buttons, headers) in the full-width lists. */
val ListSidePadding = 16.dp

/** Pads a non-row item of a full-width list like the rest of the screen. */
fun Modifier.listSidePadding(): Modifier = padding(horizontal = ListSidePadding)
