package com.offlinelabs.nutcracker.ui.components.lists

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A row whose [content] slides left to reveal [actions] (like Gmail/Outlook swipe actions).
 *
 * - Swipe left opens it; swipe right or [open] = false closes it. It settles open when dragged
 *   past half the [actionsWidth].
 * - A rightward swipe on a closed row isn't consumed, so a parent pager can still turn the page.
 * - [open] is hoisted so only one row of a list stays open at a time.
 */
@Composable
fun SwipeActionsRow(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    actionsWidth: Dp,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    val maxPx = with(LocalDensity.current) { actionsWidth.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnOpenChange by rememberUpdatedState(onOpenChange)

    LaunchedEffect(open, maxPx) { offset.animateTo(if (open) -maxPx else 0f) }

    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
        // Revealed panel, pinned to the right edge behind the content
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.fillMaxHeight(), content = actions)
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(maxPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var accepted = false
                        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                            // Only take the gesture when it opens the row, or the row is already open
                            if (overSlop < 0f || offset.value < 0f) {
                                change.consume()
                                accepted = true
                            }
                        }
                        if (drag != null && accepted) {
                            // Track the position here: snapTo runs asynchronously, so offset.value lags behind
                            var position = offset.value
                            horizontalDrag(drag.id) { change ->
                                position = (position + change.positionChange().x).coerceIn(-maxPx, 0f)
                                val target = position
                                scope.launch { offset.snapTo(target) }
                                change.consume()
                            }
                            val settleOpen = position < -maxPx / 2f
                            scope.launch { offset.animateTo(if (settleOpen) -maxPx else 0f) }
                            currentOnOpenChange(settleOpen)
                        }
                    }
                }
        ) {
            content()
        }
    }
}

