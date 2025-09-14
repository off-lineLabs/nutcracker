package com.example.template.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/**
 * App-specific color utilities that provide themed colors
 * Use these instead of hardcoded colors throughout your app
 * 
 * This system uses your brand colors and respects the app's theme setting
 */

// ===== THEMED BRAND COLORS =====
@Composable
fun brandPrimaryColor(): Color {
    return if (isDarkTheme()) BrandNavyLight else BrandNavyLightTheme
}

@Composable
private fun isDarkTheme(): Boolean {
    val currentBackground = MaterialTheme.colorScheme.background
    return currentBackground == BrandNavy
}

@Composable
fun brandSecondaryColor(): Color {
    return if (isDarkTheme()) BrandRed else BrandRedLightTheme
}

@Composable
fun brandAccentColor(): Color {
    return if (isDarkTheme()) BrandGold else BrandGoldLightTheme
}

@Composable
fun brandHighlightColor(): Color {
    return if (isDarkTheme()) BrandGoldLight else BrandGoldLightLightTheme
}

// ===== THEMED BACKGROUND COLORS =====
@Composable
fun appBackgroundColor(): Color {
    return if (isDarkTheme()) OriginalDarkGray900 else OriginalLightGray50
}

@Composable
fun appSurfaceColor(): Color {
    return if (isDarkTheme()) OriginalDarkGray800 else OriginalContainerGray
}

@Composable
fun appSurfaceVariantColor(): Color {
    return if (isDarkTheme()) OriginalDarkGray800 else OriginalLightGray100
}

@Composable
fun appContainerBackgroundColor(): Color {
    return if (isDarkTheme()) {
        MaterialTheme.colorScheme.surface
    } else {
        OriginalContainerGray
    }
}

@Composable
fun appCardBackgroundColor(): Color {
    return if (isDarkTheme()) BrandNavyLight else Color.White
}

// ===== THEMED TEXT COLORS =====
@Composable
fun appTextPrimaryColor(): Color {
    return if (isDarkTheme()) Color.White else Color(0xFF111827)
}

@Composable
fun appTextSecondaryColor(): Color {
    return if (isDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF6B7280)
}

@Composable
fun appTextTertiaryColor(): Color {
    return if (isDarkTheme()) NeutralDark500 else TextTertiary
}

@Composable
fun appTextInverseColor(): Color {
    return if (isDarkTheme()) TextPrimary else TextInverse
}

// ===== THEMED SEMANTIC COLORS =====
@Composable
fun successColor(): Color {
    return if (isDarkTheme()) SuccessLight else Success
}

@Composable
fun warningColor(): Color {
    return if (isDarkTheme()) WarningLight else Warning
}

@Composable
fun errorColor(): Color {
    return if (isDarkTheme()) ErrorLight else Error
}

@Composable
fun infoColor(): Color {
    return if (isDarkTheme()) InfoLight else Info
}

// ===== THEMED NUTRIENT COLORS (PRESERVED) =====
// These keep your existing nutrient colors exactly as they are
@Composable
fun nutrientCarbsColor(): Color {
    return if (isDarkTheme()) NutrientCarbs else NutrientCarbsDark
}

@Composable
fun nutrientProteinColor(): Color {
    return if (isDarkTheme()) NutrientProtein else NutrientProteinDark
}

@Composable
fun nutrientFatColor(): Color {
    return if (isDarkTheme()) NutrientFat else NutrientFatDark
}

@Composable
fun nutrientFiberColor(): Color {
    return if (isDarkTheme()) NutrientFiber else NutrientFiberDark
}

// ===== THEMED COMPONENT COLORS =====
@Composable
fun pillTakenColor(): Color = PillTaken

@Composable
fun pillNotTakenColor(): Color = PillNotTaken

@Composable
fun exerciseEnabledColor(): Color = BrandRed

@Composable
fun exerciseDisabledColor(): Color = ExerciseDisabled

@Composable
fun tefEnabledColor(): Color = BrandGold

@Composable
fun tefDisabledColor(): Color = ExerciseDisabled

// ===== THEMED FAB AND ACTION COLORS =====
@Composable
fun fabExerciseColor(): Color {
    return if (isDarkTheme()) FabExercise else FabExerciseLight
}

@Composable
fun fabMealColor(): Color {
    return if (isDarkTheme()) FabMeal else FabMealLight
}

@Composable
fun exerciseItemBackgroundColor(): Color {
    return if (isDarkTheme()) ExerciseItemBackground else ExerciseItemBackgroundLight
}

@Composable
fun mealItemBackgroundColor(): Color {
    return if (isDarkTheme()) MealItemBackground else MealItemBackgroundLight
}

// ===== THEMED DIVIDER AND BORDER COLORS =====
@Composable
fun appDividerColor(): Color {
    return if (isDarkTheme()) NeutralDark400 else NeutralLight300
}

@Composable
fun appBorderColor(): Color {
    return if (isDarkTheme()) NeutralDark300 else NeutralLight200
}

// ===== THEMED PROGRESS COLORS =====
@Composable
fun progressTrackColor(): Color {
    return if (isDarkTheme()) NeutralDark400 else NeutralLight300
}

@Composable
fun progressBackgroundColor(): Color {
    return if (isDarkTheme()) NeutralDark300 else NeutralLight200
}

// ===== THEMED CALORIE RING COLORS (PRESERVED) =====
// These keep your existing calorie ring colors exactly as they are
@Composable
fun calorieRingTrackColor(): Color = CalorieRingTrack

@Composable
fun calorieRingProgressColor(): Color = CalorieRingProgress

// ===== THEMED ELEVATION COLORS =====
@Composable
fun appElevationColor(): Color {
    return if (isDarkTheme()) {
        Color.Black.copy(alpha = 0.3f)
    } else {
        Color.Black.copy(alpha = 0.1f)
    }
}

// ===== THEMED OVERLAY COLORS =====
@Composable
fun appOverlayColor(): Color {
    return if (isDarkTheme()) {
        Color.Black.copy(alpha = 0.5f)
    } else {
        Color.Black.copy(alpha = 0.3f)
    }
}

// ===== UTILITY FUNCTIONS =====
/**
 * Get a color with opacity based on theme
 */
@Composable
fun Color.withThemeOpacity(lightOpacity: Float = 0.1f, darkOpacity: Float = 0.2f): Color {
    val opacity = if (isDarkTheme()) darkOpacity else lightOpacity
    return this.copy(alpha = opacity)
}

/**
 * Determines the appropriate text color (black or white) based on the background color's luminance.
 * Uses a threshold of 0.5 - if the background is lighter than this, use black text, otherwise white.
 */
@Composable
fun getContrastingTextColor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) {
        Color.Black
    } else {
        Color.White
    }
}

/**
 * Get a softer contrasting color for icons - provides contrast but with reduced intensity
 * This is useful for icons that need to be visible but not as prominent as text
 */
@Composable
fun getContrastingIconColor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) {
        // For light backgrounds, use a dark gray instead of pure black
        Color(0xFF424242) // Dark gray
    } else {
        // For dark backgrounds, use a light gray instead of pure white
        Color(0xFFBDBDBD) // Light gray
    }
}

/**
 * Get a contrasting color for UI elements like sliders - provides good contrast
 * but with a slight blue tint for better visual appeal
 */
@Composable
fun getContrastingSliderColor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) {
        // For light backgrounds, use a dark blue-gray
        Color(0xFF1976D2) // Material Blue 700
    } else {
        // For dark backgrounds, use a light blue
        Color(0xFF64B5F6) // Material Blue 300
    }
}

/**
 * Get a color that contrasts well with the current background
 */
@Composable
fun getContrastingTextColor(): Color {
    return if (isDarkTheme()) TextInverse else TextPrimary
}

/**
 * Get a color that works well as a secondary text color
 */
@Composable
fun getSecondaryTextColor(): Color {
    return if (isDarkTheme()) NeutralDark600 else TextSecondary
}
