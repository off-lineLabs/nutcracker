package com.offlinelabs.nutcracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.abs

private val WheelItemHeight = 24.dp

/** "42" for whole values, otherwise one decimal ("42.5"). */
fun formatWheelValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.getDefault(), "%.1f", value)

/**
 * Small slot-machine style picker: [values] scroll vertically and snap, the centred one is the
 * selection (neighbours fade out above and below). [onValueChange] fires when the wheel settles on
 * a new value; [unit] is shown under it.
 */
@Composable
fun ValueWheel(
    values: List<Double>,
    value: Double,
    onValueChange: (Double) -> Unit,
    unit: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val startIndex = remember(values) { values.indices.minByOrNull { abs(values[it] - value) } ?: 0 }
    // With one item of padding above and below, the first visible item is the centred one
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val fling = rememberSnapFlingBehavior(listState, snapPosition = SnapPosition.Center)
    val haptics = LocalHapticFeedback.current
    val currentOnChange by rememberUpdatedState(onValueChange)
    // The latest value: comparing with the one from launch time would miss a scroll back to it
    val currentValue by rememberUpdatedState(value)
    val centredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - centre) }?.index ?: startIndex
        }
    }

    // A tick per value passed, and report the value once the wheel stops
    LaunchedEffect(listState) {
        snapshotFlow { centredIndex }.distinctUntilChanged().drop(1).collect {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    LaunchedEffect(listState, values) {
        snapshotFlow { listState.isScrollInProgress }.distinctUntilChanged().collect { scrolling ->
            if (!scrolling) values.getOrNull(centredIndex)?.let { if (it != currentValue) currentOnChange(it) }
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .size(width = 60.dp, height = WheelItemHeight * 3)
                .semantics { this.contentDescription = contentDescription }
        ) {
            Box {
                LazyColumn(
                    state = listState,
                    flingBehavior = fling,
                    contentPadding = PaddingValues(vertical = WheelItemHeight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(values) { index, v ->
                        val distance = abs(index - centredIndex)
                        Text(
                            text = formatWheelValue(v),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(WheelItemHeight)
                                .graphicsLayer {
                                    alpha = if (distance == 0) 1f else 0.35f
                                    val scale = if (distance == 0) 1f else 0.85f
                                    scaleX = scale
                                    scaleY = scale
                                }
                        )
                    }
                }
            }
        }
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
