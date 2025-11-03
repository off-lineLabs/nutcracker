package com.offlinelabs.nutcracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

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

/**
 * Blend a color with the background to reduce contrast (make it more discrete)
 * 
 * @param color The color to blend
 * @param backgroundColor The background color to blend with
 * @param blendFactor The amount to blend (0.0 = no blend, 1.0 = full blend to background). 
 *                    Lower values (0.3-0.5) create subtle, discrete colors
 * @return A color blended with the background for reduced contrast
 */
fun blendWithBackground(color: Color, backgroundColor: Color, blendFactor: Float = 0.4f): Color {
    val clampedBlend = blendFactor.coerceIn(0f, 1f)
    return Color(
        red = color.red * (1f - clampedBlend) + backgroundColor.red * clampedBlend,
        green = color.green * (1f - clampedBlend) + backgroundColor.green * clampedBlend,
        blue = color.blue * (1f - clampedBlend) + backgroundColor.blue * clampedBlend,
        alpha = color.alpha
    )
}

/**
 * Get a discrete text color that blends with the background for minimal contrast
 * This is useful for buttons or text that should be visible but not prominent
 */
@Composable
fun getDiscreteTextColor(backgroundColor: Color = appBackgroundColor()): Color {
    val baseColor = appTextSecondaryColor()
    return blendWithBackground(baseColor, backgroundColor, blendFactor = 0.65f)
}

/**
 * Generate different shades of a color with varying contrast levels
 * 
 * @param baseColor The base color to create shades from
 * @param level Contrast level from 0-4:
 *   - 0: No change (returns original color)
 *   - 1: Very subtle contrast adjustment
 *   - 2: Subtle contrast adjustment
 *   - 3: Moderate contrast adjustment  
 *   - 4: High contrast adjustment
 * @param backgroundColor The background color to contrast against (optional, uses theme background if not provided)
 * @return A new color with the specified contrast level
 */
fun generateColorShade(
    baseColor: Color, 
    level: Int, 
    backgroundColor: Color? = null
): Color {
    if (level == 0) return baseColor
    
    // Clamp level to valid range
    val clampedLevel = level.coerceIn(0, 4)
    
    // Convert to HSL for better color manipulation
    val hsl = rgbToHsl(baseColor)
    
    // Calculate contrast adjustment based on level (more gradual steps)
    val contrastFactor = when (clampedLevel) {
        1 -> 0.08f  // Very subtle
        2 -> 0.15f  // Subtle
        3 -> 0.25f  // Moderate
        4 -> 0.40f  // High
        else -> 0f
    }
    
    // Determine contrast direction based on background luminance (like getContrastingTextColor)
    val backgroundLuminance = backgroundColor?.luminance() ?: 0.5f
    val shouldLighten = backgroundLuminance < 0.5f
    
    // Adjust lightness based on background contrast
    val newLightness = if (shouldLighten) {
        // For dark backgrounds, make colors lighter for better contrast
        (hsl.lightness + contrastFactor).coerceAtMost(0.9f)
    } else {
        // For light backgrounds, make colors darker for better contrast
        (hsl.lightness - contrastFactor).coerceAtLeast(0.1f)
    }
    
    // Slightly adjust saturation for more vibrant results
    val newSaturation = (hsl.saturation + (contrastFactor * 0.2f)).coerceAtMost(1f)
    
    return hslToRgb(hsl.hue, newSaturation, newLightness)
}

/**
 * Generate a themed color shade that automatically detects the current theme background
 */
@Composable
fun generateThemedColorShade(baseColor: Color, level: Int): Color {
    val backgroundColor = MaterialTheme.colorScheme.background
    return generateColorShade(baseColor, level, backgroundColor)
}

/**
 * Generate different shades of your brand colors
 */
@Composable
fun brandPrimaryShade(level: Int): Color {
    return generateThemedColorShade(brandPrimaryColor(), level)
}

@Composable
fun brandSecondaryShade(level: Int): Color {
    return generateThemedColorShade(brandSecondaryColor(), level)
}

@Composable
fun brandAccentShade(level: Int): Color {
    return generateThemedColorShade(brandAccentColor(), level)
}

@Composable
fun brandHighlightShade(level: Int): Color {
    return generateThemedColorShade(brandHighlightColor(), level)
}

/**
 * Extension function for easy color shade generation
 */
fun Color.shade(level: Int, backgroundColor: Color? = null): Color {
    return generateColorShade(this, level, backgroundColor)
}

/**
 * Themed extension function for easy color shade generation
 */
@Composable
fun Color.themedShade(level: Int): Color {
    return generateThemedColorShade(this, level)
}

/**
 * RGB to HSL conversion
 */
private fun rgbToHsl(color: Color): HslColor {
    val r = color.red
    val g = color.green
    val b = color.blue
    
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    
    val lightness = (max + min) / 2f
    
    val saturation = if (delta == 0f) {
        0f
    } else {
        if (lightness < 0.5f) {
            delta / (max + min)
        } else {
            delta / (2f - max - min)
        }
    }
    
    val hue = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta + (if (g < b) 6 else 0)) / 6f
        max == g -> ((b - r) / delta + 2) / 6f
        else -> ((r - g) / delta + 4) / 6f
    }
    
    return HslColor(hue, saturation, lightness)
}

/**
 * HSL to RGB conversion
 */
private fun hslToRgb(hue: Float, saturation: Float, lightness: Float): Color {
    val h = hue * 6f
    val s = saturation
    val l = lightness
    
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h % 2f) - 1f))
    val m = l - c / 2f
    
    val (r, g, b) = when {
        h < 1f -> Triple(c, x, 0f)
        h < 2f -> Triple(x, c, 0f)
        h < 3f -> Triple(0f, c, x)
        h < 4f -> Triple(0f, x, c)
        h < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    
    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f),
        alpha = 1f
    )
}

/**
 * HSL color data class
 */
private data class HslColor(
    val hue: Float,
    val saturation: Float,
    val lightness: Float
)