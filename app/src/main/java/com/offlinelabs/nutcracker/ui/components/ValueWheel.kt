package com.offlinelabs.nutcracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.abs

internal val WheelItemHeight = 24.dp

/** "42" for whole values, otherwise one decimal ("42.5"). */
fun formatWheelValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.getDefault(), "%.1f", value)

/**
 * A number box that only shows the chosen value (so scrolling a list can't change it by
 * accident). Tapping it opens a wheel floating over it (springing open vertically, the row
 * doesn't move); the wheel closes itself shortly after it stops, or on a tap on it or outside.
 *
 * @param unit shown under the value, or after it with [inlineUnit] (compact boxes).
 */
@Composable
fun ValuePicker(
    values: List<Double>,
    value: Double,
    onValueChange: (Double) -> Unit,
    unit: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    width: Dp = 56.dp,
    height: Dp = 44.dp,
    inlineUnit: Boolean = false
) {
    var open by remember { mutableStateOf(false) }
    val accent = FormDefaults.colors().accent
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            onClick = { open = true },
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, if (open) accent else MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .size(width = width, height = height)
                .semantics { this.contentDescription = contentDescription }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(
                    text = if (inlineUnit && unit != null) "${formatWheelValue(value)} $unit" else formatWheelValue(value),
                    style = if (inlineUnit) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (unit != null && !inlineUnit) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
        if (open) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true)
            ) {
                // Springs open from the box's height
                val reveal = remember { Animatable(0f) }
                LaunchedEffect(Unit) { reveal.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) }
                // Closes itself a moment after the user stops turning it
                var scrolling by remember { mutableStateOf(false) }
                var turned by remember { mutableStateOf(false) }
                LaunchedEffect(scrolling, turned) {
                    if (turned && !scrolling) {
                        delay(900)
                        open = false
                    }
                }
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            transformOrigin = TransformOrigin.Center
                            scaleY = 0.35f + 0.65f * reveal.value
                            alpha = reveal.value
                        }
                        // A tap (not a drag) on the wheel picks the centred value and closes it
                        .clickable(interactionSource = null, indication = null) { open = false }
                ) {
                    ValueWheel(
                        values = values,
                        value = value,
                        onValueChange = onValueChange,
                        unit = null,
                        contentDescription = contentDescription,
                        width = width + 8.dp,
                        itemHeight = 32.dp,
                        visibleItems = 5,
                        elevated = true,
                        onScrollingChange = { now ->
                            scrolling = now
                            if (now) turned = true
                        }
                    )
                }
            }
        }
    }
}

/**
 * Slot-machine style picker: [values] scroll vertically and snap, the centred one is the selection
 * (neighbours fade out above and below). [onValueChange] fires when the wheel settles on a new
 * value; [unit] is shown under it.
 */
@Composable
fun ValueWheel(
    values: List<Double>,
    value: Double,
    onValueChange: (Double) -> Unit,
    /** Shown under the wheel; null for none. */
    unit: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    width: Dp = 60.dp,
    /** Height of one value. */
    itemHeight: Dp = WheelItemHeight,
    /** Values shown at once (odd): the centred one and as many above as below. */
    visibleItems: Int = 3,
    /** A floating wheel: opaque with a shadow (the [ValuePicker]'s). */
    elevated: Boolean = false,
    /** Told when the wheel starts and stops moving. */
    onScrollingChange: ((Boolean) -> Unit)? = null
) {
    val startIndex = remember(values) { values.indices.minByOrNull { abs(values[it] - value) } ?: 0 }
    // With padding of half the window above and below, the first visible item is the centred one
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val fling = rememberSnapFlingBehavior(listState, snapPosition = SnapPosition.Center)
    val haptics = LocalHapticFeedback.current
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentOnScrolling by rememberUpdatedState(onScrollingChange)
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
            currentOnScrolling?.invoke(scrolling)
            if (!scrolling) values.getOrNull(centredIndex)?.let { if (it != currentValue) currentOnChange(it) }
        }
    }

    val half = visibleItems / 2
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (elevated) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, if (elevated) FormDefaults.colors().accent else MaterialTheme.colorScheme.outline),
            shadowElevation = if (elevated) 8.dp else 0.dp,
            modifier = Modifier
                .size(width = width, height = itemHeight * visibleItems)
                .semantics { this.contentDescription = contentDescription }
        ) {
            Box {
                LazyColumn(
                    state = listState,
                    flingBehavior = fling,
                    contentPadding = PaddingValues(vertical = itemHeight * half),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(values) { index, v ->
                        val distance = abs(index - centredIndex)
                        Text(
                            text = formatWheelValue(v),
                            // Short items use a smaller font so their neighbours fit
                            style = if (itemHeight < WheelItemHeight) {
                                MaterialTheme.typography.bodyMedium.copy(lineHeight = TextUnit(16f, TextUnitType.Sp))
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .graphicsLayer {
                                    alpha = when (distance) {
                                        0 -> 1f
                                        1 -> 0.45f
                                        else -> 0.2f
                                    }
                                    val scale = if (distance == 0) 1f else 0.85f
                                    scaleX = scale
                                    scaleY = scale
                                }
                        )
                    }
                }
            }
        }
        if (unit != null) {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
