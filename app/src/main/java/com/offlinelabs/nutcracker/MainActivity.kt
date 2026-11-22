package com.offlinelabs.nutcracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.ui.tooling.preview.Preview
import com.offlinelabs.nutcracker.data.SettingsManager
import com.offlinelabs.nutcracker.ui.screens.dashboard.DashboardScreen
import com.offlinelabs.nutcracker.ui.screens.settings.SettingsScreen
import com.offlinelabs.nutcracker.ui.screens.analytics.AnalyticsScreen
import com.offlinelabs.nutcracker.ui.screens.analytics.AnalyticsSection
import com.offlinelabs.nutcracker.ui.screens.dashboard.DASHBOARD_PAGE_HOME
import com.offlinelabs.nutcracker.ui.screens.help.HelpScreen
import com.offlinelabs.nutcracker.ui.screens.terms.TermsOfUseDialog
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide the action bar for Compose UI
        supportActionBar?.hide()
        enableEdgeToEdge()
        setContent {
            AppContent()
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val settingsManager = (context.applicationContext as FoodLogApplication).settingsManager
    
    // Directly observe the mutable state from SettingsManager
    val currentThemeMode = settingsManager.currentThemeMode
    
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
    // Saveable so rotation or a language change (activity recreation) keeps the user on the same screen
    var currentScreen by rememberSaveable { mutableStateOf("dashboard") }
    var shouldShowTutorial by rememberSaveable { mutableStateOf(false) }
    // Dashboard tab and day survive a trip to analytics/settings/help (the dashboard leaves composition)
    var dashboardPage by rememberSaveable { mutableIntStateOf(DASHBOARD_PAGE_HOME) }
    var dashboardEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var analyticsSection by rememberSaveable { mutableStateOf(AnalyticsSection.NUTRITION) }
    
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
            onNavigateToAnalytics = { fromExerciseTab ->
                analyticsSection = if (fromExerciseTab) AnalyticsSection.EXERCISE else AnalyticsSection.NUTRITION
                currentScreen = "analytics"
            },
            initialPage = dashboardPage,
            initialDate = dashboardEpochDay?.let { java.time.LocalDate.ofEpochDay(it) },
            onPageChanged = { dashboardPage = it },
            onDateChanged = { dashboardEpochDay = it.toEpochDay() },
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
            initialSection = analyticsSection,
            isDarkTheme = settingsManager.isDarkTheme(context)
        )
        "help" -> HelpScreen(
            onNavigateBack = { currentScreen = "dashboard" },
            isDarkTheme = settingsManager.isDarkTheme(context),
            onReplayTutorial = { 
                // The tutorial spotlights Home elements
                dashboardPage = DASHBOARD_PAGE_HOME
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
