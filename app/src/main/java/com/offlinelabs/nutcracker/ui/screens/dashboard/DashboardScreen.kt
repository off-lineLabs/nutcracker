package com.offlinelabs.nutcracker.ui.screens.dashboard

import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.SettingsManager
import com.offlinelabs.nutcracker.ui.components.tutorial.SpotlightOverlay
import com.offlinelabs.nutcracker.ui.components.tutorial.TutorialStep
import com.offlinelabs.nutcracker.ui.theme.appBackgroundColor
import com.offlinelabs.nutcracker.ui.theme.appSurfaceColor
import com.offlinelabs.nutcracker.ui.theme.appSurfaceVariantColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The main screen: a shared header over three swipeable tabs (Food, Home, Exercise) and the
 * dialogs they open. Its state lives in [DashboardState]; the tabs and dialog groups are split
 * into their own files so no single composable grows too big for the runtime to compile.
 */
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit = {},
    /** Opens analytics; [fromExerciseTab] is true when the user was on the Exercise tab. */
    onNavigateToAnalytics: (fromExerciseTab: Boolean) -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") isDarkTheme: Boolean,
    settingsManager: SettingsManager? = null,
    shouldShowTutorial: Boolean = false,
    onTutorialCompleted: () -> Unit = {},
    /** Tab and day to show when (re)entering the dashboard, e.g. after returning from analytics. */
    initialPage: Int = DASHBOARD_PAGE_HOME,
    initialDate: java.time.LocalDate? = null,
    onPageChanged: (Int) -> Unit = {},
    onDateChanged: (java.time.LocalDate) -> Unit = {}
) {
    val state = rememberDashboardState(settingsManager, initialPage, initialDate)
    state.CollectDashboardData(onPageChanged, onDateChanged)
    state.TutorialEffects(shouldShowTutorial)

    // Back from Food or Exercise returns to Home; on Home back behaves as before.
    BackHandler(enabled = state.pagerState.currentPage != DASHBOARD_PAGE_HOME) {
        state.coroutineScope.launch { state.pagerState.animateScrollToPage(DASHBOARD_PAGE_HOME) }
    }

    state.DashboardScaffold(
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToAnalytics = onNavigateToAnalytics,
        onNavigateToHelp = onNavigateToHelp
    )

    state.FoodDialogs()
    state.ExerciseDialogs()
    state.HomeDialogs()
    state.TutorialOverlay(onTutorialCompleted)
}

@Composable
private fun DashboardState.DashboardScaffold(
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: (fromExerciseTab: Boolean) -> Unit,
    onNavigateToHelp: () -> Unit
) {
    val view = LocalView.current
    val statusBarHeight = with(LocalDensity.current) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (view.rootWindowInsets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0).toDp()
        } else {
            0.dp
        }
    }
    val gradientStartColor = appBackgroundColor()
    val gradientEndColor = appSurfaceVariantColor()
    val innerContainerBackgroundColor = appSurfaceColor()

    Scaffold(
        topBar = {
            DashboardHeader(
                selectedDate = selectedDate,
                onSelectedDateChange = { selectedDate = it },
                onOpenCalendar = { showCalendarDialog = true },
                onNavigateToHelp = onNavigateToHelp,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToAnalytics = { onNavigateToAnalytics(pagerState.currentPage == DASHBOARD_PAGE_EXERCISE) },
                registerElementCoordinates = registerElementCoordinates,
                modifier = Modifier.padding(top = statusBarHeight + 8.dp)
            )
        },
        bottomBar = {
            DashboardTabBar(
                selectedPage = pagerState.currentPage,
                onSelectPage = { page -> coroutineScope.launch { pagerState.animateScrollToPage(page) } },
                registerElementCoordinates = registerElementCoordinates
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { scaffoldPaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(colors = listOf(gradientStartColor, gradientEndColor)))
                .padding(scaffoldPaddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !tutorialState.isActive
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .background(color = innerContainerBackgroundColor.copy(alpha = 0.8f), shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    when (page) {
                        DASHBOARD_PAGE_FOOD -> FoodTab()
                        DASHBOARD_PAGE_EXERCISE -> ExerciseTab()
                        else -> HomeTab()
                    }
                }
            }
        }
    }
}

/** Starts the tutorial when needed, feeds it the targets' positions and scrolls Home to them. */
@Composable
private fun DashboardState.TutorialEffects(shouldShowTutorial: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(shouldShowTutorial, settingsManager?.hasCompletedTutorial()) {
        if (settingsManager != null && (shouldShowTutorial || !settingsManager.hasCompletedTutorial())) {
            tutorialState.startTutorial(createTutorialSteps(elementCoordinates, context))
        }
    }

    // Point the current step at its target once the target's position is known
    LaunchedEffect(elementCoordinates, tutorialState.currentStepIndex, showHistorySpotlight) {
        val currentStep = tutorialState.getCurrentStep()
        if (currentStep != null && elementCoordinates.isNotEmpty()) {
            val stepId = getStepIdFromIndex(tutorialState.currentStepIndex)
            val coordinates = elementCoordinates[stepId]
            // The history step waits for its scroll to finish (showHistorySpotlight)
            val shouldUpdate = coordinates != null && (stepId != "check_in_history" || showHistorySpotlight)
            if (shouldUpdate && coordinates != null) {
                tutorialState.steps[tutorialState.currentStepIndex] = currentStep.copy(
                    targetOffset = coordinates.first,
                    targetSize = coordinates.second
                )
            }
        }
    }

    // Scroll Home to the supplements and history steps
    LaunchedEffect(tutorialState.currentStepIndex) {
        when (tutorialState.currentStepIndex) {
            7 -> {
                // Supplements: centre them a little better
                showHistorySpotlight = true
                delay(100)
                lazyListState.animateScrollToItem(2, scrollOffset = -100)
            }
            8 -> {
                // History: hide the spotlight during the scroll, show it once the layout settled
                showHistorySpotlight = false
                delay(100)
                lazyListState.animateScrollToItem(3)
                delay(500)
                showHistorySpotlight = true
            }
            else -> showHistorySpotlight = true
        }
    }

    // The tutorial spotlights Home content and the tab items: keep the pager on Home meanwhile.
    LaunchedEffect(tutorialState.isActive) {
        if (tutorialState.isActive && pagerState.currentPage != DASHBOARD_PAGE_HOME) {
            pagerState.scrollToPage(DASHBOARD_PAGE_HOME)
        }
    }
}

@Composable
private fun DashboardState.TutorialOverlay(onTutorialCompleted: () -> Unit) {
    if (!tutorialState.isActive) return
    val currentStep = tutorialState.getCurrentStep()
    SpotlightOverlay(
        step = currentStep,
        onNext = {
            if (currentStep?.showDialog != null) {
                currentStep.showDialog()
            } else {
                val wasLastStep = tutorialState.currentStepIndex == tutorialState.steps.size - 1
                tutorialState.nextStep()
                if (wasLastStep) {
                    settingsManager?.setTutorialCompleted()
                    onTutorialCompleted()
                }
            }
        },
        onSkip = {
            tutorialState.skipTutorial()
            settingsManager?.setTutorialCompleted()
            onTutorialCompleted()
        },
        onPrevious = { tutorialState.previousStep() }
    )
}

/** Tutorial step ids in order (the index is the step number). */
private val TUTORIAL_STEP_IDS = listOf(
    "dashboard_overview", "calorie_ring", "food_tab", "exercise_tab", "analytics_icon",
    "settings_icon", "edit_goals_icon", "supplement_pill", "check_in_history", "completion"
)

private fun getStepIdFromIndex(index: Int): String = TUTORIAL_STEP_IDS.getOrElse(index) { "" }

private fun createTutorialSteps(elementCoordinates: Map<String, Pair<Offset, Size>>, context: Context): List<TutorialStep> {
    fun step(id: String, titleRes: Int, descriptionRes: Int) = TutorialStep(
        id = id,
        title = context.getString(titleRes),
        description = context.getString(descriptionRes),
        targetOffset = elementCoordinates[id]?.first,
        targetSize = elementCoordinates[id]?.second
    )
    return listOf(
        TutorialStep(
            id = "dashboard_overview",
            title = context.getString(R.string.tutorial_welcome_title),
            description = context.getString(R.string.tutorial_welcome_description)
        ),
        step("calorie_ring", R.string.tutorial_calorie_ring_title, R.string.tutorial_calorie_ring_description),
        step("food_tab", R.string.tutorial_add_meal_title, R.string.tutorial_add_meal_description),
        step("exercise_tab", R.string.tutorial_add_exercise_title, R.string.tutorial_add_exercise_description),
        step("analytics_icon", R.string.tutorial_analytics_title, R.string.tutorial_analytics_description),
        step("settings_icon", R.string.tutorial_settings_title, R.string.tutorial_settings_description),
        step("edit_goals_icon", R.string.tutorial_edit_goals_title, R.string.tutorial_edit_goals_description),
        step("supplement_pill", R.string.tutorial_supplement_pill_title, R.string.tutorial_supplement_pill_description),
        step("check_in_history", R.string.tutorial_check_in_history_title, R.string.tutorial_check_in_history_description),
        TutorialStep(
            id = "completion",
            title = context.getString(R.string.tutorial_completion_title),
            description = context.getString(R.string.tutorial_completion_description)
        )
    )
}
