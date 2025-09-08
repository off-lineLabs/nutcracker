package com.example.template.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

// Dark theme color scheme using your brand colors
private val DarkColorScheme = darkColorScheme(
    primary = BrandNavyLight,           // Your lighter navy blue for main elements
    onPrimary = Color.White,
    primaryContainer = BrandNavy,       // Your dark navy blue
    onPrimaryContainer = BrandGoldLight, // Your light gold for contrast
    
    secondary = BrandRed,               // Your brand red
    onSecondary = Color.White,
    secondaryContainer = BrandRed.copy(alpha = 0.2f),
    onSecondaryContainer = BrandRed,
    
    tertiary = BrandGold,               // Your brand gold
    onTertiary = Color.White,
    tertiaryContainer = BrandGold.copy(alpha = 0.2f),
    onTertiaryContainer = BrandGold,
    
    background = BrandNavy,             // Your dark navy blue background
    onBackground = Color.White,
    surface = BrandNavyLight,           // Your lighter navy blue for surfaces
    onSurface = Color.White,
    surfaceVariant = NeutralDark300,
    onSurfaceVariant = NeutralDark600,
    
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.2f),
    onErrorContainer = ErrorLight,
    
    outline = NeutralDark400,
    outlineVariant = NeutralDark300,
    scrim = Color.Black.copy(alpha = 0.5f)
)

// Light theme color scheme using your brand colors
private val LightColorScheme = lightColorScheme(
    primary = BrandNavyLightTheme,      // Your lighter navy blue for light theme
    onPrimary = Color.White,
    primaryContainer = BrandNavyDarkLightTheme, // Your dark navy blue for light theme
    onPrimaryContainer = BrandGoldLightTheme,   // Your gold for light theme
    
    secondary = BrandRedLightTheme,     // Your brand red for light theme
    onSecondary = Color.White,
    secondaryContainer = BrandRedLightTheme.copy(alpha = 0.1f),
    onSecondaryContainer = BrandRedLightTheme,
    
    tertiary = BrandGoldLightTheme,     // Your brand gold for light theme
    onTertiary = Color.White,
    tertiaryContainer = BrandGoldLightTheme.copy(alpha = 0.1f),
    onTertiaryContainer = BrandGoldLightTheme,
    
    background = NeutralLight50,        // Light background
    onBackground = TextPrimary,
    surface = Color.White,              // White surfaces
    onSurface = TextPrimary,
    surfaceVariant = NeutralLight100,
    onSurfaceVariant = TextSecondary,
    
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.1f),
    onErrorContainer = ErrorDark,
    
    outline = NeutralLight300,
    outlineVariant = NeutralLight200,
    scrim = Color.Black.copy(alpha = 0.3f)
)

@Composable
fun FoodLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            android.util.Log.d("FoodLogTheme", "Using dynamic colors: darkTheme=$darkTheme, dynamicColor=$dynamicColor")
            scheme
        }

        darkTheme -> {
            android.util.Log.d("FoodLogTheme", "Using custom dark colors: darkTheme=$darkTheme, dynamicColor=$dynamicColor")
            DarkColorScheme
        }
        else -> {
            android.util.Log.d("FoodLogTheme", "Using custom light colors: darkTheme=$darkTheme, dynamicColor=$dynamicColor")
            LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            if (activity != null) {
                val window = activity.window
                // Make the status bar transparent and draw behind it
                WindowCompat.setDecorFitsSystemWindows(window, false)
                
                // Configure status bar icons based on theme
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}