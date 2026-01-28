package com.offlinelabs.nutcracker.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinelabs.nutcracker.ui.theme.appTextPrimaryColor
import com.offlinelabs.nutcracker.ui.theme.appTextSecondaryColor
import com.offlinelabs.nutcracker.ui.theme.nutrientCarbsColor
import com.offlinelabs.nutcracker.ui.theme.nutrientFatColor
import com.offlinelabs.nutcracker.ui.theme.nutrientProteinColor
import com.offlinelabs.nutcracker.ui.theme.progressTrackColor
import com.offlinelabs.nutcracker.utils.MacroCalculator
import kotlin.math.roundToInt

/**
 * CombinedMacroBar - Video game stat-style bars for macros
 * 
 * Displays three separate horizontal bars (like Mario Kart stats) for carbs, protein, and fat.
 * When enabled, dragging one bar increases it and proportionally decreases the others.
 * Max values are based on caloric content divided by caloric density.
 * 
 * @param carbsGrams Current carbs in grams
 * @param proteinGrams Current protein in grams
 * @param fatGrams Current fat in grams
 * @param totalKcal Total calorie goal (used to calculate max values)
 * @param suggestedCarbsGrams Suggested carbs value to show as marker
 * @param suggestedProteinGrams Suggested protein value to show as marker
 * @param suggestedFatGrams Suggested fat value to show as marker
 * @param showSuggested Whether to show suggested value markers
 * @param onMacrosChanged Callback when macros are adjusted via dragging
 * @param enabled Whether dragging is enabled
 */
@Composable
fun CombinedMacroBar(
    carbsGrams: Double,
    proteinGrams: Double,
    fatGrams: Double,
    totalKcal: Int,
    suggestedCarbsGrams: Double = 0.0,
    suggestedProteinGrams: Double = 0.0,
    suggestedFatGrams: Double = 0.0,
    showSuggested: Boolean = false,
    onMacrosChanged: (carbs: Double, protein: Double, fat: Double) -> Unit
) {
    val carbsColor = nutrientCarbsColor()
    val proteinColor = nutrientProteinColor()
    val fatColor = nutrientFatColor()
    val trackColor = progressTrackColor()
    
    // Track which macros are locked (can lock up to 2)
    var lockedMacros by remember { mutableStateOf(setOf<String>()) }
    
    // Calculate max values based on caloric density
    val maxCarbsGrams = if (totalKcal > 0) totalKcal / 4.0 else 500.0
    val maxProteinGrams = if (totalKcal > 0) totalKcal / 4.0 else 500.0
    val maxFatGrams = if (totalKcal > 0) totalKcal / 9.0 else 222.0
    
    // Calculate suggested markers, adjusting for locked macros
    val adjustedSuggestions = when {
        lockedMacros.contains("CARBS") && lockedMacros.contains("PROTEIN") -> {
            // Carbs and Protein locked, Fat gets all remaining calories
            val remainingKcal = totalKcal - (carbsGrams * 4.0) - (proteinGrams * 4.0)
            Triple(carbsGrams, proteinGrams, remainingKcal / 9.0)
        }
        lockedMacros.contains("CARBS") && lockedMacros.contains("FAT") -> {
            // Carbs and Fat locked, Protein gets all remaining calories
            val remainingKcal = totalKcal - (carbsGrams * 4.0) - (fatGrams * 9.0)
            Triple(carbsGrams, remainingKcal / 4.0, fatGrams)
        }
        lockedMacros.contains("PROTEIN") && lockedMacros.contains("FAT") -> {
            // Protein and Fat locked, Carbs gets all remaining calories
            val remainingKcal = totalKcal - (proteinGrams * 4.0) - (fatGrams * 9.0)
            Triple(remainingKcal / 4.0, proteinGrams, fatGrams)
        }
        lockedMacros.contains("CARBS") -> {
            // Only Carbs locked, redistribute remaining calories between protein and fat
            val remainingKcal = totalKcal - (carbsGrams * 4.0)
            val proteinRatio = suggestedProteinGrams / (suggestedProteinGrams + suggestedFatGrams)
            val fatRatio = suggestedFatGrams / (suggestedProteinGrams + suggestedFatGrams)
            Triple(
                carbsGrams,
                (remainingKcal * proteinRatio) / 4.0,
                (remainingKcal * fatRatio) / 9.0
            )
        }
        lockedMacros.contains("PROTEIN") -> {
            // Only Protein locked, redistribute remaining calories between carbs and fat
            val remainingKcal = totalKcal - (proteinGrams * 4.0)
            val carbsRatio = suggestedCarbsGrams / (suggestedCarbsGrams + suggestedFatGrams)
            val fatRatio = suggestedFatGrams / (suggestedCarbsGrams + suggestedFatGrams)
            Triple(
                (remainingKcal * carbsRatio) / 4.0,
                proteinGrams,
                (remainingKcal * fatRatio) / 9.0
            )
        }
        lockedMacros.contains("FAT") -> {
            // Only Fat locked, redistribute remaining calories between carbs and protein
            val remainingKcal = totalKcal - (fatGrams * 9.0)
            val carbsRatio = suggestedCarbsGrams / (suggestedCarbsGrams + suggestedProteinGrams)
            val proteinRatio = suggestedProteinGrams / (suggestedCarbsGrams + suggestedProteinGrams)
            Triple(
                (remainingKcal * carbsRatio) / 4.0,
                (remainingKcal * proteinRatio) / 4.0,
                fatGrams
            )
        }
        else -> Triple(suggestedCarbsGrams, suggestedProteinGrams, suggestedFatGrams)
    }
    
    val (finalSuggestedCarbs, finalSuggestedProtein, finalSuggestedFat) = adjustedSuggestions
    
    // Calculate current calorie percentages
    val carbsKcal = carbsGrams * 4.0
    val proteinKcal = proteinGrams * 4.0
    val fatKcal = fatGrams * 9.0
    val currentTotalKcal = carbsKcal + proteinKcal + fatKcal
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Carbs bar - draggable, only updates carbs value
        MacroStatBar(
            label = "Carbs",
            grams = carbsGrams,
            maxGrams = maxCarbsGrams,
            suggestedGrams = finalSuggestedCarbs,
            showSuggested = showSuggested,
            color = carbsColor,
            trackColor = trackColor,
            isLocked = lockedMacros.contains("CARBS"),
            onLockToggle = {
                lockedMacros = if (lockedMacros.contains("CARBS")) {
                    lockedMacros - "CARBS"
                } else if (lockedMacros.size < 2) {
                    lockedMacros + "CARBS"
                } else {
                    lockedMacros // Already 2 locked, can't lock more
                }
            },
            onValueChange = { newValue ->
                onMacrosChanged(newValue, proteinGrams, fatGrams)
            }
        )
        
        // Protein bar - draggable, only updates protein value
        MacroStatBar(
            label = "Protein",
            grams = proteinGrams,
            maxGrams = maxProteinGrams,
            suggestedGrams = finalSuggestedProtein,
            showSuggested = showSuggested,
            color = proteinColor,
            trackColor = trackColor,
            isLocked = lockedMacros.contains("PROTEIN"),
            onLockToggle = {
                lockedMacros = if (lockedMacros.contains("PROTEIN")) {
                    lockedMacros - "PROTEIN"
                } else if (lockedMacros.size < 2) {
                    lockedMacros + "PROTEIN"
                } else {
                    lockedMacros // Already 2 locked, can't lock more
                }
            },
            onValueChange = { newValue ->
                onMacrosChanged(carbsGrams, newValue, fatGrams)
            }
        )
        
        // Fat bar - draggable, only updates fat value
        MacroStatBar(
            label = "Fat",
            grams = fatGrams,
            maxGrams = maxFatGrams,
            suggestedGrams = finalSuggestedFat,
            showSuggested = showSuggested,
            color = fatColor,
            trackColor = trackColor,
            isLocked = lockedMacros.contains("FAT"),
            onLockToggle = {
                lockedMacros = if (lockedMacros.contains("FAT")) {
                    lockedMacros - "FAT"
                } else if (lockedMacros.size < 2) {
                    lockedMacros + "FAT"
                } else {
                    lockedMacros // Already 2 locked, can't lock more
                }
            },
            onValueChange = { newValue ->
                onMacrosChanged(carbsGrams, proteinGrams, newValue)
            }
        )
    }
}

@Composable
private fun MacroStatBar(
    label: String,
    grams: Double,
    maxGrams: Double,
    suggestedGrams: Double,
    showSuggested: Boolean,
    color: Color,
    trackColor: Color,
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    onValueChange: (Double) -> Unit
) {
    val fillRatio = if (maxGrams > 0) (grams / maxGrams).coerceIn(0.0, 1.0) else 0.0
    val suggestedRatio = if (maxGrams > 0 && showSuggested) (suggestedGrams / maxGrams).coerceIn(0.0, 1.0) else 0.0
    
    // Track value at drag start - we'll add accumulated pixel deltas to this
    var dragStartGrams by remember { mutableDoubleStateOf(0.0) }
    var totalPixelDrag by remember { mutableDoubleStateOf(0.0) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = appTextPrimaryColor()
                )
                // Lock icon - clickable
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isLocked) "Locked" else "Unlocked",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onLockToggle() },
                    tint = if (isLocked) color else appTextSecondaryColor()
                )
            }
            Text(
                text = "${grams.roundToInt()}g",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = appTextSecondaryColor()
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        
        // Container for bar and marker
        Box(modifier = Modifier.fillMaxWidth()) {
            // Main progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(trackColor)
                    .pointerInput(maxGrams, isLocked) {
                        if (!isLocked) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    // Capture starting point
                                    dragStartGrams = grams
                                    totalPixelDrag = 0.0
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    // Accumulate pixel deltas
                                    totalPixelDrag += dragAmount
                                    val barWidth = size.width.toFloat()
                                    // Convert total pixels to grams once
                                    val gramsDelta = (totalPixelDrag / barWidth) * maxGrams
                                    val newGrams = (dragStartGrams + gramsDelta).coerceIn(0.0, maxGrams)
                                    onValueChange(newGrams)
                                }
                            )
                        }
                    }

            ) {
                // Progress fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillRatio.toFloat())
                        .height(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(listOf(color, color))
                        )
                )
            }
            
            // Suggested value marker - extends outside bar, darker for visibility
            if (showSuggested && suggestedRatio > 0.01) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(suggestedRatio.toFloat())
                        .height(20.dp)
                        .offset(y = (-4).dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}
