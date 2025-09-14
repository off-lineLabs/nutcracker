package com.example.template.data

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

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    // Theme state
    var currentThemeMode by mutableStateOf(getStoredThemeMode())
        private set
    
    // Language state
    var currentAppLanguage by mutableStateOf(getStoredLanguage())
        private set
    
    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_LANGUAGE = "app_language"
    }
    
    private fun getStoredThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, null)
        return when (stored) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            "SYSTEM" -> ThemeMode.SYSTEM
            else -> ThemeMode.SYSTEM // Default to system
        }
    }
    
    private fun getStoredLanguage(): AppLanguage {
        val stored = prefs.getString(KEY_APP_LANGUAGE, null)
        return when (stored) {
            "ENGLISH" -> AppLanguage.ENGLISH
            "PORTUGUESE" -> AppLanguage.PORTUGUESE
            "SPANISH" -> AppLanguage.SPANISH
            else -> detectSystemLanguage() // Default to system language
        }
    }
    
    private fun detectSystemLanguage(): AppLanguage {
        val systemLocale = Locale.getDefault()
        return when (systemLocale.language) {
            "es" -> AppLanguage.SPANISH
            "pt" -> AppLanguage.PORTUGUESE
            else -> AppLanguage.ENGLISH
        }
    }
    
    fun setThemeMode(themeMode: ThemeMode) {
        this.currentThemeMode = themeMode
        prefs.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
        // Debug log
        com.example.template.util.logger.AppLogger.d("SettingsManager", "Theme changed to: $themeMode")
    }
    
    fun setAppLanguage(language: AppLanguage) {
        this.currentAppLanguage = language
        prefs.edit().putString(KEY_APP_LANGUAGE, language.name).apply()
    }
    
    fun getLocale(): Locale {
        return when (currentAppLanguage) {
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.PORTUGUESE -> Locale.forLanguageTag("pt-BR")
            AppLanguage.SPANISH -> Locale.forLanguageTag("es-ES")
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
}
