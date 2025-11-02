package com.offlinelabs.nutcracker.ui.components.tutorial

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.SettingsManager
import android.content.Context

@Composable
fun TutorialManager(
    settingsManager: SettingsManager,
    shouldShowTutorial: Boolean,
    onTutorialCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (TutorialState, (String, Offset, Dp) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val tutorialState = remember { TutorialState() }
    
    // Track element coordinates
    var elementCoordinates by remember { mutableStateOf<Map<String, Pair<Offset, Dp>>>(emptyMap()) }
    
    // Function to register element coordinates
    val registerElementCoordinates: (String, Offset, Dp) -> Unit = { id, offset, radius ->
        elementCoordinates = elementCoordinates + (id to (offset to radius))
    }
    
    // Start tutorial if needed
    LaunchedEffect(shouldShowTutorial, settingsManager.hasCompletedTutorial()) {
        if (shouldShowTutorial && !settingsManager.hasCompletedTutorial()) {
            val steps = createTutorialSteps(elementCoordinates, context)
            tutorialState.startTutorial(steps)
        }
    }
    
    // Update current step with coordinates
    LaunchedEffect(elementCoordinates, tutorialState.currentStepIndex) {
        val currentStep = tutorialState.getCurrentStep()
        if (currentStep != null && elementCoordinates.isNotEmpty()) {
            val stepId = getStepIdFromIndex(tutorialState.currentStepIndex)
            val coordinates = elementCoordinates[stepId]
            if (coordinates != null) {
                val updatedStep = currentStep.copy(
                    targetOffset = coordinates.first,
                    targetRadius = coordinates.second
                )
                tutorialState.steps[tutorialState.currentStepIndex] = updatedStep
            }
        }
    }
    
    // Content with tutorial state
    content(tutorialState, registerElementCoordinates)
    
    // Tutorial overlay
    if (tutorialState.isActive) {
        val currentStep = tutorialState.getCurrentStep()
        SpotlightOverlay(
            step = currentStep,
            onNext = {
                if (currentStep?.showDialog != null) {
                    currentStep.showDialog()
                } else {
                    tutorialState.nextStep()
                }
            },
            onSkip = {
                tutorialState.skipTutorial()
                settingsManager.setTutorialCompleted()
                onTutorialCompleted()
            },
            onPrevious = {
                tutorialState.previousStep()
            }
        )
    }
}

private fun createTutorialSteps(elementCoordinates: Map<String, Pair<Offset, Dp>>, context: Context): List<TutorialStep> {
    return listOf(
        TutorialStep(
            id = "dashboard_overview",
            title = context.getString(R.string.tutorial_welcome_title),
            description = context.getString(R.string.tutorial_welcome_description)
        ),
        TutorialStep(
            id = "calorie_ring",
            title = context.getString(R.string.tutorial_calorie_ring_title),
            description = context.getString(R.string.tutorial_calorie_ring_description),
            targetOffset = elementCoordinates["calorie_ring"]?.first,
            targetRadius = elementCoordinates["calorie_ring"]?.second ?: 60.dp
        ),
        TutorialStep(
            id = "add_meal_fab",
            title = context.getString(R.string.tutorial_add_meal_title),
            description = context.getString(R.string.tutorial_add_meal_description),
            targetOffset = elementCoordinates["add_meal_fab"]?.first,
            targetRadius = elementCoordinates["add_meal_fab"]?.second ?: 30.dp
        ),
        TutorialStep(
            id = "add_exercise_fab",
            title = context.getString(R.string.tutorial_add_exercise_title),
            description = context.getString(R.string.tutorial_add_exercise_description),
            targetOffset = elementCoordinates["add_exercise_fab"]?.first,
            targetRadius = elementCoordinates["add_exercise_fab"]?.second ?: 30.dp
        ),
        TutorialStep(
            id = "analytics_icon",
            title = context.getString(R.string.tutorial_analytics_title),
            description = context.getString(R.string.tutorial_analytics_description),
            targetOffset = elementCoordinates["analytics_icon"]?.first,
            targetRadius = elementCoordinates["analytics_icon"]?.second ?: 25.dp
        ),
        TutorialStep(
            id = "settings_icon",
            title = context.getString(R.string.tutorial_settings_title),
            description = context.getString(R.string.tutorial_settings_description),
            targetOffset = elementCoordinates["settings_icon"]?.first,
            targetRadius = elementCoordinates["settings_icon"]?.second ?: 25.dp
        ),
        TutorialStep(
            id = "edit_goals_icon",
            title = context.getString(R.string.tutorial_edit_goals_title),
            description = context.getString(R.string.tutorial_edit_goals_description),
            targetOffset = elementCoordinates["edit_goals_icon"]?.first,
            targetRadius = elementCoordinates["edit_goals_icon"]?.second ?: 25.dp
        ),
        TutorialStep(
            id = "supplement_pill",
            title = context.getString(R.string.tutorial_supplement_pill_title),
            description = context.getString(R.string.tutorial_supplement_pill_description),
            targetOffset = elementCoordinates["supplement_pill"]?.first,
            targetRadius = elementCoordinates["supplement_pill"]?.second ?: 30.dp
        ),
        TutorialStep(
            id = "completion",
            title = context.getString(R.string.tutorial_completion_title),
            description = context.getString(R.string.tutorial_completion_description)
        )
    )
}

private fun getStepIdFromIndex(index: Int): String {
    return when (index) {
        0 -> "dashboard_overview"
        1 -> "calorie_ring"
        2 -> "add_meal_fab"
        3 -> "add_exercise_fab"
        4 -> "analytics_icon"
        5 -> "settings_icon"
        6 -> "edit_goals_icon"
        7 -> "supplement_pill"
        8 -> "completion"
        else -> ""
    }
}
