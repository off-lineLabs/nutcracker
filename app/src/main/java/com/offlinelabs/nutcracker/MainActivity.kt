package com.offlinelabs.nutcracker

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.ui.tooling.preview.Preview
import com.offlinelabs.nutcracker.data.SettingsManager
import com.offlinelabs.nutcracker.ui.screens.dashboard.DashboardScreen
import com.offlinelabs.nutcracker.ui.screens.settings.SettingsScreen
import com.offlinelabs.nutcracker.ui.screens.analytics.AnalyticsScreen
import com.offlinelabs.nutcracker.ui.screens.help.HelpScreen
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme
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
            darkTheme = settingsManager.isDarkTheme(context)
        ) {
            AppNavigation(settingsManager)
        }
    }
}

@Composable
fun AppNavigation(settingsManager: SettingsManager) {
    val context = LocalContext.current
    val database = (context.applicationContext as FoodLogApplication).database
    var currentScreen by remember { mutableStateOf("dashboard") }
    
    // Handle system back button
    BackHandler(enabled = currentScreen == "settings" || currentScreen == "analytics" || currentScreen == "help") {
        currentScreen = "dashboard"
    }
    
    when (currentScreen) {
        "dashboard" -> DashboardScreen(
            onNavigateToSettings = { currentScreen = "settings" },
            onNavigateToAnalytics = { currentScreen = "analytics" },
            onNavigateToHelp = { currentScreen = "help" },
            isDarkTheme = settingsManager.isDarkTheme(LocalContext.current)
        )
        "settings" -> SettingsScreen(
            onNavigateBack = { currentScreen = "dashboard" },
            settingsManager = settingsManager,
            database = database
        )
        "analytics" -> AnalyticsScreen(
            onNavigateBack = { currentScreen = "dashboard" },
            isDarkTheme = settingsManager.isDarkTheme(context)
        )
        "help" -> HelpScreen(
            onNavigateBack = { currentScreen = "dashboard" },
            isDarkTheme = settingsManager.isDarkTheme(context)
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
