package com.example.template.util.logger

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration for the logging framework
 */
object LoggingConfig {
    
    private const val PREFS_NAME = "logging_config"
    private const val KEY_LOG_LEVEL = "log_level"
    private const val KEY_ENABLE_FILE_LOGGING = "enable_file_logging"
    private const val KEY_MAX_FILE_SIZE = "max_file_size"
    private const val KEY_MAX_FILES = "max_files"
    
    private var prefs: SharedPreferences? = null
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get the current log level
     */
    fun getLogLevel(): LogLevel {
        val levelString = prefs?.getString(KEY_LOG_LEVEL, LogLevel.INFO.name) ?: LogLevel.INFO.name
        return try {
            LogLevel.valueOf(levelString)
        } catch (e: IllegalArgumentException) {
            LogLevel.INFO
        }
    }
    
    /**
     * Set the log level
     */
    fun setLogLevel(level: LogLevel) {
        prefs?.edit()?.putString(KEY_LOG_LEVEL, level.name)?.apply()
    }
    
    /**
     * Check if file logging is enabled
     */
    fun isFileLoggingEnabled(): Boolean {
        return prefs?.getBoolean(KEY_ENABLE_FILE_LOGGING, true) ?: true
    }
    
    /**
     * Enable or disable file logging
     */
    fun setFileLoggingEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_ENABLE_FILE_LOGGING, enabled)?.apply()
    }
    
    /**
     * Get maximum file size in bytes
     */
    fun getMaxFileSize(): Long {
        return prefs?.getLong(KEY_MAX_FILE_SIZE, 5 * 1024 * 1024) ?: 5 * 1024 * 1024 // 5MB default
    }
    
    /**
     * Set maximum file size in bytes
     */
    fun setMaxFileSize(sizeBytes: Long) {
        prefs?.edit()?.putLong(KEY_MAX_FILE_SIZE, sizeBytes)?.apply()
    }
    
    /**
     * Get maximum number of log files to keep
     */
    fun getMaxFiles(): Int {
        return prefs?.getInt(KEY_MAX_FILES, 3) ?: 3
    }
    
    /**
     * Set maximum number of log files to keep
     */
    fun setMaxFiles(maxFiles: Int) {
        prefs?.edit()?.putInt(KEY_MAX_FILES, maxFiles)?.apply()
    }
    
    /**
     * Reset to default configuration
     */
    fun resetToDefaults() {
        prefs?.edit()?.clear()?.apply()
    }
}
