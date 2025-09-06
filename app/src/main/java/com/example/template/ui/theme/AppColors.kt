package com.example.template.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * App-specific color utilities that provide themed colors
 * Use these instead of hardcoded colors throughout your app
 * 
 * This system uses your brand colors and provides automatic dark/light theme support
 */

// ===== THEMED BRAND COLORS =====
@Composable
fun brandPrimaryColor(): Color {
    return if (isSystemInDarkTheme()) BrandNavyLight else BrandNavyLightTheme
}

@Composable
fun brandSecondaryColor(): Color {
    return if (isSystemInDarkTheme()) BrandRed else BrandRedLightTheme
}

@Composable
fun brandAccentColor(): Color {
    return if (isSystemInDarkTheme()) BrandGold else BrandGoldLightTheme
}

@Composable
fun brandHighlightColor(): Color {
    return if (isSystemInDarkTheme()) BrandGoldLight else BrandGoldLightLightTheme
}

// ===== THEMED BACKGROUND COLORS =====
@Composable
fun appBackgroundColor(): Color {
    return if (isSystemInDarkTheme()) BrandNavy else NeutralLight50
}

@Composable
fun appSurfaceColor(): Color {
    return if (isSystemInDarkTheme()) BrandNavyLight else Color.White
}

@Composable
fun appSurfaceVariantColor(): Color {
    return if (isSystemInDarkTheme()) NeutralDark300 else NeutralLight100
}

@Composable
fun appContainerBackgroundColor(): Color {
    return if (isSystemInDarkTheme()) {
        BrandNavyLight.copy(alpha = 0.6f)
    } else {
        NeutralLight100.copy(alpha = 0.6f)
    }
}

@Composable
fun appCardBackgroundColor(): Color {
    return if (isSystemInDarkTheme()) BrandNavyLight else Color.White
}

// ===== THEMED TEXT COLORS =====
@Composable
fun appTextPrimaryColor(): Color {
    return if (isSystemInDarkTheme()) TextInverse else TextPrimary
}

@Composable
fun appTextSecondaryColor(): Color {
    return if (isSystemInDarkTheme()) NeutralDark600 else TextSecondary
}

@Composable
fun appTextTertiaryColor(): Color {
    return if (isSystemInDarkTheme()) NeutralDark500 else TextTertiary
}

@Composable
fun appTextInverseColor(): Color {
    return if (isSystemInDarkTheme()) TextPrimary else TextInverse
}

// ===== THEMED SEMANTIC COLORS =====
@Composable
fun successColor(): Color {
    return if (isSystemInDarkTheme()) SuccessLight else Success
}

@Composable
fun warningColor(): Color {
    return if (isSystemInDarkTheme()) WarningLight else Warning
}

@Composable
fun errorColor(): Color {
    return if (isSystemInDarkTheme()) ErrorLight else Error
}

@Composable
fun infoColor(): Color {
    return if (isSystemInDarkTheme()) InfoLight else Info
}

// ===== THEMED NUTRIENT COLORS (PRESERVED) =====
// These keep your existing nutrient colors exactly as they are
@Composable
fun nutrientCarbsColor(): Color {
    return if (isSystemInDarkTheme()) NutrientCarbs else NutrientCarbsDark
}

@Composable
fun nutrientProteinColor(): Color {
    return if (isSystemInDarkTheme()) NutrientProtein else NutrientProteinDark
}

@Composable
fun nutrientFatColor(): Color {
    return if (isSystemInDarkTheme()) NutrientFat else NutrientFatDark
}

@Composable
fun nutrientFiberColor(): Color {
    return if (isSystemInDarkTheme()) NutrientFiber else NutrientFiberDark
}

// ===== THEMED COMPONENT COLORS =====
@Composable
fun pillTakenColor(): Color = PillTaken

@Composable
fun pillNotTakenColor(): Color = PillNotTaken

@Composable
fun exerciseEnabledColor(): Color = ExerciseEnabled

@Composable
fun exerciseDisabledColor(): Color = ExerciseDisabled

// ===== THEMED DIVIDER AND BORDER COLORS =====
@Composable
fun appDividerColor(): Color {
    return if (isSystemInDarkTheme()) NeutralDark400 else NeutralLight300
}

@Composable
fun appBorderColor(): Color {
    return if (isSystemInDarkTheme()) NeutralDark300 else NeutralLight200
}

// ===== THEMED PROGRESS COLORS =====
@Composable
fun progressTrackColor(): Color {
    return if (isSystemInDarkTheme()) NeutralDark400 else NeutralLight300
}

@Composable
fun progressBackgroundColor(): Color {
    return if (isSystemInDarkTheme()) NeutralDark300 else NeutralLight200
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
    return if (isSystemInDarkTheme()) {
        Color.Black.copy(alpha = 0.3f)
    } else {
        Color.Black.copy(alpha = 0.1f)
    }
}

// ===== THEMED OVERLAY COLORS =====
@Composable
fun appOverlayColor(): Color {
    return if (isSystemInDarkTheme()) {
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
    val opacity = if (isSystemInDarkTheme()) darkOpacity else lightOpacity
    return this.copy(alpha = opacity)
}

/**
 * Get a color that contrasts well with the current background
 */
@Composable
fun getContrastingTextColor(): Color {
    return if (isSystemInDarkTheme()) TextInverse else TextPrimary
}

/**
 * Get a color that works well as a secondary text color
 */
@Composable
fun getSecondaryTextColor(): Color {
    return if (isSystemInDarkTheme()) NeutralDark600 else TextSecondary
}
