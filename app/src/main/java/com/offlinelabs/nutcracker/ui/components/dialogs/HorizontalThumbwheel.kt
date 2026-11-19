package com.offlinelabs.nutcracker.ui.components.dialogs

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.form.formatDecimal
import kotlin.math.abs
import kotlin.math.roundToInt

/** Thumbwheel sensitivity: pixels of horizontal drag per 0.1x change. Higher = less sensitive. */
private const val THUMBWHEEL_PIXELS_PER_STEP = 45f

/**
 * Horizontal tick wheel for the serving multiplier (0.1x steps, minimum 0.1x). Drag mechanics are
 * unchanged; only [colors] (form kit) control its look.
 */
@Composable
internal fun HorizontalThumbwheel(
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    colors: FormColors = FormDefaults.colors()
) {
    val thumbwheelColor = colors.content
    val centerIndicatorColor = colors.accent
    val containerColor = colors.field
    val borderColor = colors.fieldBorder

    // Track current value during drag (since recomposition doesn't happen mid-gesture)
    var currentDragValue by remember { mutableStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }

    val pixelsPerStep = THUMBWHEEL_PIXELS_PER_STEP

    // Update currentDragValue when external value changes (e.g., from manual input)
    // BUT only when we're not actively dragging
    LaunchedEffect(value) {
        if (!isDragging) {
            currentDragValue = value
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Thumbwheel container with visual encapsulation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            // Mark that we're dragging - prevents external value from overwriting
                            isDragging = true
                        },
                        onDragEnd = {
                            // Stop dragging - allow external updates again
                            isDragging = false
                        },
                        onDragCancel = {
                            // Stop dragging - allow external updates again
                            isDragging = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            // REVERSED: Negative dragAmount (swipe left) increases, positive (swipe right) decreases
                            val deltaValue = -(dragAmount / pixelsPerStep) * 0.1

                            // Update current drag value
                            currentDragValue += deltaValue

                            // Ensure we don't go below 0.1x
                            currentDragValue = maxOf(0.1, currentDragValue)

                            // Round to nearest 0.1
                            val roundedValue = (currentDragValue * 10).roundToInt() / 10.0

                            // Update external state continuously
                            onValueChange(roundedValue)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2

                // Draw container background (rounded rectangle)
                drawRoundRect(
                    color = containerColor,
                    topLeft = Offset(8.dp.toPx(), 0f),
                    size = Size(size.width - 16.dp.toPx(), size.height),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )

                // Draw border
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(8.dp.toPx(), 0f),
                    size = Size(size.width - 16.dp.toPx(), size.height),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Visible range: +-0.5x from current value (show 11 ticks total)
                val visibleRange = 0.5
                val startValue = value - visibleRange

                // Create paint for text labels
                val textPaint = Paint().apply {
                    color = thumbwheelColor.copy(alpha = 0.7f).toArgb()
                    textSize = 10.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                // Draw ticks for 0.1x increments
                for (i in 0..10) {
                    val tickValue = startValue + (i * 0.1)

                    // Skip negative values
                    if (tickValue < 0.1) continue

                    // Calculate position relative to center
                    val offset = (tickValue - value) * 10 // 10 ticks per 1.0x
                    val tickX = (centerX + (offset * pixelsPerStep)).toFloat()

                    // Only draw if within bounds
                    if (tickX >= 0 && tickX <= size.width) {
                        val isMediumTick = abs((tickValue * 10).roundToInt() % 5) == 0 && abs((tickValue * 10).roundToInt() % 10) != 0

                        // Determine tick height based on value
                        val tickHeight = when {
                            // Large tick: whole numbers (1.0x, 2.0x, etc)
                            abs((tickValue * 10).roundToInt() % 10) == 0 -> 16.dp.toPx()
                            // Medium tick: 0.5x increments (0.5x, 1.5x, etc)
                            isMediumTick -> 12.dp.toPx()
                            // Small tick: 0.1x increments
                            else -> 8.dp.toPx()
                        }

                        // Determine tick width (thicker for larger marks)
                        val tickWidth = when {
                            abs((tickValue * 10).roundToInt() % 10) == 0 -> 3.dp.toPx()
                            isMediumTick -> 2.dp.toPx()
                            else -> 1.5.dp.toPx()
                        }

                        // Draw tick
                        drawLine(
                            color = thumbwheelColor.copy(alpha = 0.7f),
                            start = Offset(tickX, centerY - tickHeight / 2),
                            end = Offset(tickX, centerY + tickHeight / 2),
                            strokeWidth = tickWidth
                        )

                        // Draw label for medium ticks (0.5x increments)
                        if (isMediumTick) {
                            drawIntoCanvas { canvas ->
                                val label = formatDecimal(tickValue, maxFractionDigits = 1)
                                canvas.nativeCanvas.drawText(
                                    label,
                                    tickX,
                                    centerY - tickHeight / 2 - 4.dp.toPx(),
                                    textPaint
                                )
                            }
                        }
                    }
                }

                // Draw center indicator (pointer/selector)
                val indicatorHeight = 24.dp.toPx()
                drawLine(
                    color = centerIndicatorColor,
                    start = Offset(centerX, centerY - indicatorHeight / 2),
                    end = Offset(centerX, centerY + indicatorHeight / 2),
                    strokeWidth = 4.dp.toPx()
                )
            }
        }
    }
}
