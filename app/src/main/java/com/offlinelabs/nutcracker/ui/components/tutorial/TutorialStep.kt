package com.offlinelabs.nutcracker.ui.components.tutorial

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp

/**
 * Represents a single step in the tutorial flow
 */
data class TutorialStep(
    val id: String,
    val title: String,
    val description: String,
    val targetOffset: Offset? = null,
    val targetSize: Size? = null, // Changed from radius to size for proper rectangular highlights
    val showDialog: (() -> Unit)? = null,
    val hideDialog: (() -> Unit)? = null
)

/**
 * Tutorial step types for different UI elements
 */
enum class TutorialStepType {
    DASHBOARD_OVERVIEW,
    CALORIE_RING,
    ADD_MEAL_FAB,
    ADD_EXERCISE_FAB,
    CALENDAR_ICON,
    ANALYTICS_ICON,
    SETTINGS_ICON,
    MEAL_DIALOG_BARCODE,
    COMPLETION
}

/**
 * Helper class to manage tutorial state and step progression
 */
class TutorialState {
    var currentStepIndex by mutableStateOf(0)
        private set
    
    var isActive by mutableStateOf(false)
        private set
    
    var steps = mutableStateListOf<TutorialStep>()
        private set
    
    fun startTutorial(tutorialSteps: List<TutorialStep>) {
        steps.clear()
        steps.addAll(tutorialSteps)
        currentStepIndex = 0
        isActive = true
    }
    
    fun nextStep() {
        if (currentStepIndex < steps.size - 1) {
            currentStepIndex = currentStepIndex + 1
        } else {
            completeTutorial()
        }
    }
    
    fun previousStep() {
        if (currentStepIndex > 0) {
            currentStepIndex = currentStepIndex - 1
        }
    }
    
    fun skipTutorial() {
        completeTutorial()
    }
    
    fun completeTutorial() {
        isActive = false
        currentStepIndex = 0
    }
    
    fun getCurrentStep(): TutorialStep? {
        return if (isActive && currentStepIndex < steps.size) {
            steps[currentStepIndex]
        } else null
    }
    
    fun isLastStep(): Boolean {
        return currentStepIndex >= steps.size - 1
    }
    
    fun isFirstStep(): Boolean {
        return currentStepIndex == 0
    }
}
