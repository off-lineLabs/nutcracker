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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A row whose [content] slides sideways to reveal [actions] (like Gmail/Outlook swipe actions).
 *
 * - By default a swipe left opens it and reveals the actions on the right edge; with
 *   [revealFromLeft] a swipe right opens it and reveals them on the left edge. Swiping back or
 *   [open] = false closes it. It settles open when dragged past half the [actionsWidth].
 * - A swipe the other way on a closed row isn't consumed, so a parent pager can still turn the
 *   page (Food, the first page, opens from the left so its "next page" swipe stays free).
 * - [open] is hoisted so only one row of a list stays open at a time.
 */
@Composable
fun SwipeActionsRow(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    actionsWidth: Dp,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    /**
     * Painted behind [content] while it's moved away from rest, so the panel doesn't show through
     * a transparent row. At rest the panel is hidden and the content keeps its own background.
     */
    slidingBackground: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    /** Open with a swipe right, the actions on the left edge (instead of a swipe left). */
    revealFromLeft: Boolean = false,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    val maxPx = with(LocalDensity.current) { actionsWidth.toPx() }
    // +1: the content moves right to open; -1: it moves left
    val dir = if (revealFromLeft) 1f else -1f
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnOpenChange by rememberUpdatedState(onOpenChange)

    LaunchedEffect(open, maxPx, dir) { offset.animateTo(if (open) dir * maxPx else 0f) }

    Box(modifier = modifier.fillMaxWidth().clip(shape)) {
        // Revealed panel, pinned to the opening edge behind the content; hidden while the row is
        // at rest (read in the draw phase, so sliding doesn't recompose)
        Row(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = if (offset.value * dir > 0f) 1f else 0f },
            horizontalArrangement = if (revealFromLeft) {
                androidx.compose.foundation.layout.Arrangement.Absolute.Left
            } else {
                androidx.compose.foundation.layout.Arrangement.Absolute.Right
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.fillMaxHeight(), content = actions)
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .drawBehind {
                    if (offset.value * dir > 0f && slidingBackground.isSpecified) drawRect(slidingBackground)
                }
                .pointerInput(maxPx, dir) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var accepted = false
                        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                            // Only take the gesture when it opens the row, or the row is already open
                            if (overSlop * dir > 0f || offset.value * dir > 0f) {
                                change.consume()
                                accepted = true
                            }
                        }
                        if (drag != null && accepted) {
                            // Track the position here: snapTo runs asynchronously, so offset.value lags behind
                            var position = offset.value
                            horizontalDrag(drag.id) { change ->
                                position = (position + change.positionChange().x)
                                    .let { if (revealFromLeft) it.coerceIn(0f, maxPx) else it.coerceIn(-maxPx, 0f) }
                                val target = position
                                scope.launch { offset.snapTo(target) }
                                change.consume()
                            }
                            val settleOpen = position * dir > maxPx / 2f
                            scope.launch { offset.animateTo(if (settleOpen) dir * maxPx else 0f) }
                            currentOnOpenChange(settleOpen)
                        }
                    }
                }
        ) {
            content()
        }
    }
}

