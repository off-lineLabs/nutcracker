package com.example.template

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.template.data.AppLanguage
import com.example.template.data.SettingsManager
import com.example.template.data.ThemeMode
import com.example.template.ui.screens.dashboard.DashboardScreen
import com.example.template.ui.screens.settings.SettingsScreen
import com.example.template.ui.theme.FoodLogTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppContent()
        }
    }
    
    override fun attachBaseContext(newBase: Context?) {
        val settingsManager = SettingsManager(newBase!!)
        val locale = settingsManager.getLocale()
        val context = updateLocale(newBase, locale)
        super.attachBaseContext(context)
    }
    
    private fun updateLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val settingsManager = (context.applicationContext as FoodLogApplication).settingsManager
    
    // Directly observe the mutable state from SettingsManager
    val currentThemeMode = settingsManager.currentThemeMode
    val currentAppLanguage = settingsManager.currentAppLanguage
    
    // Update language when it changes
    LaunchedEffect(currentAppLanguage) {
        // Language changes require activity recreation
        // This will be handled by the settings screen
    }
    
    // Use the theme mode as a key to force recomposition when theme changes
    key(currentThemeMode) {
        FoodLogTheme(
            darkTheme = settingsManager.isDarkTheme(context),
            dynamicColor = true // Re-enable dynamic colors for modern practices
        ) {
            AppNavigation(settingsManager)
        }
    }
}

@Composable
fun AppNavigation(settingsManager: SettingsManager) {
    var currentScreen by remember { mutableStateOf("dashboard") }
    
    when (currentScreen) {
        "dashboard" -> DashboardScreen(
            onNavigateToSettings = { currentScreen = "settings" },
            isDarkTheme = settingsManager.isDarkTheme(LocalContext.current)
        )
        "settings" -> SettingsScreen(
            onNavigateBack = { currentScreen = "dashboard" },
            settingsManager = settingsManager
        )
    }
}

@Preview
@Composable
fun DefaultPreview() {
    FoodLogTheme {
        // Preview doesn't need settings manager
    }
}
