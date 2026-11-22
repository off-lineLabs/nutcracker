package com.offlinelabs.nutcracker.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class AppLanguage {
    ENGLISH, PORTUGUESE, SPANISH
}

/** How the calories and the macros of the daily goal follow each other in the goals dialog. */
enum class GoalLinkMode {
    /** Calories drive the macros (Down arrow). Default. */
    KCAL_TO_MACROS,

    /** Macros drive the calories (Up arrow). A stored value that no longer exists reads as the default. */
    MACROS_TO_KCAL
}

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    // Theme state
    var currentThemeMode by mutableStateOf(getStoredThemeMode())
        private set
    
    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_TERMS_AGREED = "terms_agreed"
        private const val KEY_TERMS_AGREED_TIMESTAMP = "terms_agreed_timestamp"
        private const val KEY_TUTORIAL_COMPLETED = "tutorial_completed"
        private const val KEY_GOAL_LINK_MODE = "goal_link_mode"
        private const val KEY_LAST_SELECTED_GYM_ID = "last_selected_gym_id"
        private const val KEY_MEAL_LIST_SORT = "meal_list_sort"
        private const val KEY_MEAL_LIST_GROUPING = "meal_list_grouping"
        private const val KEY_EXERCISE_LIST_SORT = "exercise_list_sort"
        private const val KEY_EXERCISE_LIST_GROUPING = "exercise_list_grouping"
    }

    private inline fun <reified E : Enum<E>> enumPref(key: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == prefs.getString(key, null) } ?: default

    private fun putEnumPref(key: String, value: Enum<*>) = prefs.edit().putString(key, value.name).apply()

    /** Order and grouping of the Food tab list (alphabetical, ungrouped until changed). */
    var mealListSort: ListSort
        get() = enumPref(KEY_MEAL_LIST_SORT, ListSort.ALPHABETICAL)
        set(value) = putEnumPref(KEY_MEAL_LIST_SORT, value)
    var mealListGrouping: MealGrouping
        get() = enumPref(KEY_MEAL_LIST_GROUPING, MealGrouping.NONE)
        set(value) = putEnumPref(KEY_MEAL_LIST_GROUPING, value)

    /** Order and grouping of the Exercise tab list (alphabetical, ungrouped until changed). */
    var exerciseListSort: ListSort
        get() = enumPref(KEY_EXERCISE_LIST_SORT, ListSort.ALPHABETICAL)
        set(value) = putEnumPref(KEY_EXERCISE_LIST_SORT, value)
    var exerciseListGrouping: ExerciseGrouping
        get() = enumPref(KEY_EXERCISE_LIST_GROUPING, ExerciseGrouping.NONE)
        set(value) = putEnumPref(KEY_EXERCISE_LIST_GROUPING, value)

    /** Last gym picked in the Exercise tab dropdown; null means "All gyms". */
    var lastSelectedGymId: Long?
        get() = if (prefs.contains(KEY_LAST_SELECTED_GYM_ID)) prefs.getLong(KEY_LAST_SELECTED_GYM_ID, 0L) else null
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_LAST_SELECTED_GYM_ID) else putLong(KEY_LAST_SELECTED_GYM_ID, value)
            }.apply()
        }

    /** Last chosen link mode of the goals dialog; [GoalLinkMode.KCAL_TO_MACROS] until the user changes it. */
    var goalLinkMode: GoalLinkMode
        get() = GoalLinkMode.entries.firstOrNull { it.name == prefs.getString(KEY_GOAL_LINK_MODE, null) }
            ?: GoalLinkMode.KCAL_TO_MACROS
        set(value) {
            prefs.edit().putString(KEY_GOAL_LINK_MODE, value.name).apply()
        }
    
    private fun getStoredThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, null)
        return when (stored) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            "SYSTEM" -> ThemeMode.SYSTEM
            else -> ThemeMode.DARK // Default to dark theme
        }
    }
    
    fun setThemeMode(themeMode: ThemeMode) {
        this.currentThemeMode = themeMode
        prefs.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
        // Debug log
        com.offlinelabs.nutcracker.util.logger.AppLogger.d("SettingsManager", "Theme changed to: $themeMode")
    }
    
    /**
     * Returns the current app language based on the system's current locale.
     * This is now derived from Locale.getDefault() which reflects the per-app language
     * set via LocaleManager (API 33+) or AppCompatDelegate (API <33).
     * No longer stored in SharedPreferences.
     */
    val currentAppLanguage: AppLanguage
        get() {
            val systemLocale = Locale.getDefault()
            return when (systemLocale.language) {
                "es" -> AppLanguage.SPANISH
                "pt" -> AppLanguage.PORTUGUESE
                else -> AppLanguage.ENGLISH
            }
        }
    
    fun isDarkTheme(context: Context): Boolean {
        return when (currentThemeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> {
                val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
    
    fun hasAgreedToTerms(): Boolean {
        return prefs.getBoolean(KEY_TERMS_AGREED, false)
    }
    
    fun getTermsAgreementTimestamp(): Long {
        return prefs.getLong(KEY_TERMS_AGREED_TIMESTAMP, 0L)
    }
    
    fun setTermsAgreed() {
        val currentTime = System.currentTimeMillis()
        prefs.edit()
            .putBoolean(KEY_TERMS_AGREED, true)
            .putLong(KEY_TERMS_AGREED_TIMESTAMP, currentTime)
            .apply()
    }
    
    fun hasCompletedTutorial(): Boolean {
        return prefs.getBoolean(KEY_TUTORIAL_COMPLETED, false)
    }
    
    fun setTutorialCompleted() {
        prefs.edit()
            .putBoolean(KEY_TUTORIAL_COMPLETED, true)
            .apply()
    }
}
