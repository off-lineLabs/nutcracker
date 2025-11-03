package com.offlinelabs.nutcracker.ui.components.tutorial

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.theme.appBackgroundColor
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor

@Composable
fun SpotlightOverlay(
    step: TutorialStep?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (step == null) return
    
    val density = LocalDensity.current
    val isDarkTheme = true // Will be passed from parent
    
    // Animate spotlight position and radius
    val animatedOffset by animateOffsetAsState(
        targetValue = step.targetOffset ?: Offset.Zero,
        animationSpec = tween(500),
        label = "spotlight_offset"
    )
    
    val animatedRadius by animateDpAsState(
        targetValue = step.targetRadius ?: 0.dp,
        animationSpec = tween(500),
        label = "spotlight_radius"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(1000f) // Ensure it's above all content
    ) {
        // Semi-transparent overlay with spotlight cutout (only if there's a target)
        if (step.targetOffset != null) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Enable blend mode support
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            ) {
                val radiusPx = with(density) { animatedRadius.toPx() }
                val overlayColor = Color.Black.copy(alpha = 0.7f)
                
                // Draw the full overlay first
                drawRect(
                    color = overlayColor,
                    size = size
                )
                
                // Cut out the spotlight area with rounded corners using blend mode
                if (radiusPx > 0) {
                    val cornerRadius = radiusPx * 0.15f // 15% of radius for smooth corners
                    
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(
                            animatedOffset.x - radiusPx,
                            animatedOffset.y - radiusPx
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            radiusPx * 2,
                            radiusPx * 2
                        ),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                        blendMode = BlendMode.Clear
                    )
                }
            }
        } else {
            // For steps without target, show a lighter overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
        
        // Tooltip content - show for all steps
        TooltipContent(
            step = step,
            onNext = onNext,
            onSkip = onSkip,
            onPrevious = onPrevious,
            targetOffset = animatedOffset,
            targetRadius = animatedRadius,
            isDarkTheme = isDarkTheme,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun TooltipContent(
    step: TutorialStep,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onPrevious: () -> Unit,
    targetOffset: Offset,
    targetRadius: Dp,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Get actual screen dimensions in pixels
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp * density.density
    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * density.density
    
    // Calculate tooltip position (center for first step, above/below target for others)
    val tooltipOffset = if (step.targetOffset != null) {
        calculateTooltipPosition(
            targetOffset = targetOffset,
            targetRadius = targetRadius,
            screenSize = androidx.compose.ui.unit.IntSize(screenWidth.toInt(), screenHeight.toInt()),
            density = density
        )
    } else {
        // Center the tooltip for steps without target (like welcome step)
        val tooltipWidthPx = with(density) { 320.dp.toPx() }
        Offset(
            x = (screenWidth - tooltipWidthPx) / 2f, // Center horizontally
            y = screenHeight / 3f // Position in upper third
        )
    }
    
    Box(
        modifier = modifier
    ) {
        // Tooltip card
        Card(
            modifier = Modifier
                .offset(
                    x = with(density) { tooltipOffset.x.toDp() },
                    y = with(density) { tooltipOffset.y.toDp() }
                )
                .padding(16.dp)
                .widthIn(min = 280.dp, max = 320.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = appBackgroundColor()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = step.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = getContrastingTextColor(appBackgroundColor()),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                Text(
                    text = step.description,
                    fontSize = 14.sp,
                    color = getContrastingTextColor(appBackgroundColor()).copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Previous button (only show if not first step)
                    if (!step.id.contains("overview")) {
                        OutlinedButton(
                            onClick = onPrevious,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.tutorial_previous),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Skip button
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.tutorial_skip),
                            fontSize = 14.sp
                        )
                    }
                    
                    // Next/Done button
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (step.id.contains("completion")) {
                            Text(stringResource(R.string.tutorial_done))
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.tutorial_next),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculateTooltipPosition(
    targetOffset: Offset,
    targetRadius: Dp,
    screenSize: androidx.compose.ui.unit.IntSize,
    density: androidx.compose.ui.unit.Density
): Offset {
    val targetRadiusPx = with(density) { targetRadius.toPx() }
    val screenWidth = screenSize.width.toFloat()
    val screenHeight = screenSize.height.toFloat()
    
    // Safety check for invalid screen dimensions
    if (screenWidth <= 0 || screenHeight <= 0) {
        return Offset(0f, 0f)
    }
    
    // Convert dp to pixels for consistent spacing
    val marginDp = 16.dp
    val tooltipSpacingDp = 20.dp
    val tooltipWidthDp = 320.dp
    val tooltipHeightDp = 200.dp // Approximate tooltip height
    
    val marginPx = with(density) { marginDp.toPx() }
    val tooltipSpacingPx = with(density) { tooltipSpacingDp.toPx() }
    val tooltipWidthPx = with(density) { tooltipWidthDp.toPx() }
    val tooltipHeightPx = with(density) { tooltipHeightDp.toPx() }
    
    // Safety check for invalid tooltip dimensions
    if (tooltipWidthPx <= 0 || tooltipHeightPx <= 0) {
        return Offset(screenWidth / 2f, screenHeight / 2f)
    }
    
    // Try to position above the target first
    val tooltipYAbove = targetOffset.y - targetRadiusPx - tooltipSpacingPx - tooltipHeightPx
    val tooltipYBelow = targetOffset.y + targetRadiusPx + tooltipSpacingPx
    
    // Center horizontally on target
    val tooltipX = targetOffset.x - (tooltipWidthPx / 2f)
    
    // Determine if we should position above or below
    val shouldPositionAbove = tooltipYAbove >= marginPx
    val shouldPositionBelow = tooltipYBelow + tooltipHeightPx <= screenHeight - marginPx
    
    val finalY = when {
        shouldPositionAbove -> tooltipYAbove
        shouldPositionBelow -> tooltipYBelow
        else -> {
            // If neither works, position in the middle of the screen
            (screenHeight - tooltipHeightPx) / 2f
        }
    }
    
    // Ensure tooltip stays within screen bounds
    val maxX = screenWidth - tooltipWidthPx - marginPx
    val maxY = screenHeight - tooltipHeightPx - marginPx
    
    val clampedX = if (maxX > marginPx) {
        tooltipX.coerceIn(marginPx, maxX)
    } else {
        // If tooltip is wider than screen, center it
        (screenWidth - tooltipWidthPx) / 2f
    }
    
    val clampedY = if (maxY > marginPx) {
        finalY.coerceIn(marginPx, maxY)
    } else {
        // If tooltip is taller than screen, center it vertically
        (screenHeight - tooltipHeightPx) / 2f
    }
    
    return Offset(clampedX, clampedY)
}
