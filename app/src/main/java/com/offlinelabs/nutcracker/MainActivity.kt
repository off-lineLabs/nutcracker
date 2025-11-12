package com.offlinelabs.nutcracker

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
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
import com.offlinelabs.nutcracker.ui.screens.terms.TermsOfUseDialog
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    enableEdgeToEdge()
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
    var shouldShowTutorial by remember { mutableStateOf(false) }
    
    // Check if user has agreed to terms - use state to track changes
    var hasAgreedToTerms by remember { mutableStateOf(settingsManager.hasAgreedToTerms()) }
    var showTermsDialog by remember { mutableStateOf(!hasAgreedToTerms) }
    
    // Check tutorial completion status only after terms have been agreed
    // This only runs on initial composition if terms are already agreed
    LaunchedEffect(Unit) {
        if (hasAgreedToTerms) {
            val hasCompleted = settingsManager.hasCompletedTutorial()
            if (!hasCompleted) {
                shouldShowTutorial = true
            }
        }
    }
    
    // Handle system back button
    BackHandler(enabled = currentScreen == "settings" || currentScreen == "analytics" || currentScreen == "help") {
        currentScreen = "dashboard"
    }
    
    // Show dashboard (and other screens) as background
    when (currentScreen) {
        "dashboard" -> DashboardScreen(
            onNavigateToSettings = { currentScreen = "settings" },
            onNavigateToAnalytics = { currentScreen = "analytics" },
            onNavigateToHelp = { currentScreen = "help" },
            isDarkTheme = settingsManager.isDarkTheme(LocalContext.current),
            settingsManager = settingsManager,
            // Only show tutorial if terms are agreed and tutorial flag is set
            shouldShowTutorial = shouldShowTutorial && hasAgreedToTerms && !showTermsDialog,
            onTutorialCompleted = { shouldShowTutorial = false }
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
            isDarkTheme = settingsManager.isDarkTheme(context),
            onReplayTutorial = { 
                currentScreen = "dashboard"
                shouldShowTutorial = true
            }
        )
    }
    
    // Show terms dialog overlay if user hasn't agreed yet
    if (showTermsDialog) {
        TermsOfUseDialog(
            settingsManager = settingsManager,
            onTermsAgreed = { 
                hasAgreedToTerms = true
                showTermsDialog = false
                // After terms are agreed, check if tutorial should be shown
                val hasCompleted = settingsManager.hasCompletedTutorial()
                if (!hasCompleted) {
                    shouldShowTutorial = true
                }
            }
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
