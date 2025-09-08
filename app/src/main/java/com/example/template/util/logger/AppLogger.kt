package com.example.template.util.logger

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max

/**
 * Modern logging framework for the Offline Calorie Calculator app.
 * Provides structured logging with automatic log rotation and size management.
 */
object AppLogger {
    
    private const val TAG = "CalorieCalculator"
    private const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024 // 5MB per file
    private const val MAX_LOG_FILES = 3 // Keep only 3 log files
    private const val LOG_BUFFER_SIZE = 100 // Buffer size for batch writing
    
    private var isInitialized = false
    private var logDirectory: File? = null
    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Initialize the logging framework
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        logDirectory = File(context.filesDir, "logs")
        if (!logDirectory!!.exists()) {
            logDirectory!!.mkdirs()
        }
        
        // Plant Timber tree for console logging
        // Note: BuildConfig will be available after first build
        try {
            val buildConfigClass = Class.forName("com.example.template.BuildConfig")
            val debugField = buildConfigClass.getField("DEBUG")
            val isDebug = debugField.getBoolean(null)
            
            if (isDebug) {
                Timber.plant(Timber.DebugTree())
            } else {
                Timber.plant(ReleaseTree())
            }
        } catch (e: Exception) {
            // Fallback to debug tree if BuildConfig is not available
            Timber.plant(Timber.DebugTree())
        }
        
        // Start background log writer
        startLogWriter()
        
        // Clean up old log files
        cleanupOldLogs()
        
        isInitialized = true
        i("AppLogger", "Logging framework initialized")
    }
    
    /**
     * Log verbose message
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.VERBOSE, tag, message, throwable)
    }
    
    /**
     * Log debug message
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }
    
    /**
     * Log info message
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.INFO, tag, message, throwable)
    }
    
    /**
     * Log warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }
    
    /**
     * Log error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }
    
    /**
     * Log exception with context
     */
    fun exception(tag: String, message: String, throwable: Throwable, context: Map<String, Any>? = null) {
        val contextStr = context?.let { 
            it.entries.joinToString(", ") { "${it.key}=${it.value}" }
        } ?: ""
        
        val fullMessage = if (contextStr.isNotEmpty()) {
            "$message | Context: $contextStr"
        } else {
            message
        }
        
        log(LogLevel.ERROR, tag, fullMessage, throwable)
    }
    
    /**
     * Log user action for analytics/debugging
     */
    fun userAction(action: String, details: Map<String, Any>? = null) {
        val detailsStr = details?.let { 
            it.entries.joinToString(", ") { "${it.key}=${it.value}" }
        } ?: ""
        
        val message = if (detailsStr.isNotEmpty()) {
            "User Action: $action | Details: $detailsStr"
        } else {
            "User Action: $action"
        }
        
        log(LogLevel.INFO, "UserAction", message)
    }
    
    /**
     * Log performance metrics
     */
    fun performance(operation: String, durationMs: Long, details: Map<String, Any>? = null) {
        val detailsStr = details?.let { 
            it.entries.joinToString(", ") { "${it.key}=${it.value}" }
        } ?: ""
        
        val message = if (detailsStr.isNotEmpty()) {
            "Performance: $operation took ${durationMs}ms | Details: $detailsStr"
        } else {
            "Performance: $operation took ${durationMs}ms"
        }
        
        log(LogLevel.DEBUG, "Performance", message)
    }
    
    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (!isInitialized) {
            // Fallback to system log if not initialized
            when (level) {
                LogLevel.VERBOSE -> Log.v(tag, message, throwable)
                LogLevel.DEBUG -> Log.d(tag, message, throwable)
                LogLevel.INFO -> Log.i(tag, message, throwable)
                LogLevel.WARN -> Log.w(tag, message, throwable)
                LogLevel.ERROR -> Log.e(tag, message, throwable)
            }
            return
        }
        
        // Log to console via Timber
        when (level) {
            LogLevel.VERBOSE -> Timber.tag(tag).v(throwable, message)
            LogLevel.DEBUG -> Timber.tag(tag).d(throwable, message)
            LogLevel.INFO -> Timber.tag(tag).i(throwable, message)
            LogLevel.WARN -> Timber.tag(tag).w(throwable, message)
            LogLevel.ERROR -> Timber.tag(tag).e(throwable, message)
        }
        
        // Add to buffer for file logging
        val logEntry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        
        logBuffer.offer(logEntry)
        
        // Trigger immediate write for errors
        if (level == LogLevel.ERROR) {
            scope.launch {
                flushLogs()
            }
        }
    }
    
    private fun startLogWriter() {
        scope.launch {
            while (isActive) {
                delay(5000) // Write logs every 5 seconds
                flushLogs()
            }
        }
    }
    
    private suspend fun flushLogs() {
        if (logBuffer.isEmpty()) return
        
        val logsToWrite = mutableListOf<LogEntry>()
        repeat(LOG_BUFFER_SIZE) {
            logBuffer.poll()?.let { logsToWrite.add(it) }
        }
        
        if (logsToWrite.isEmpty()) return
        
        try {
            val logFile = getCurrentLogFile()
            FileWriter(logFile, true).use { writer ->
                logsToWrite.forEach { entry ->
                    writer.write(formatLogEntry(entry))
                    writer.write("\n")
                }
            }
            
            // Check if we need to rotate logs
            if (logFile.length() > MAX_LOG_FILE_SIZE) {
                rotateLogs()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write logs to file", e)
        }
    }
    
    private fun getCurrentLogFile(): File {
        val fileName = "app_log_${fileNameFormat.format(Date())}.txt"
        return File(logDirectory, fileName)
    }
    
    private fun rotateLogs() {
        try {
            val currentFile = getCurrentLogFile()
            val timestamp = System.currentTimeMillis()
            val rotatedFile = File(logDirectory, "app_log_${fileNameFormat.format(Date())}_$timestamp.txt")
            currentFile.renameTo(rotatedFile)
            
            // Clean up old files
            cleanupOldLogs()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate logs", e)
        }
    }
    
    private fun cleanupOldLogs() {
        try {
            val logFiles = logDirectory?.listFiles { file ->
                file.name.startsWith("app_log_") && file.name.endsWith(".txt")
            } ?: return
            
            if (logFiles.size > MAX_LOG_FILES) {
                logFiles.sortedBy { it.lastModified() }
                    .take(logFiles.size - MAX_LOG_FILES)
                    .forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old logs", e)
        }
    }
    
    private fun formatLogEntry(entry: LogEntry): String {
        val timestamp = dateFormat.format(Date(entry.timestamp))
        val level = entry.level.name.padEnd(5)
        val throwableStr = entry.throwable?.let { 
            "\n${Log.getStackTraceString(it)}"
        } ?: ""
        
        return "[$timestamp] $level [${entry.tag}] ${entry.message}$throwableStr"
    }
    
    /**
     * Get all log files for debugging purposes
     */
    fun getLogFiles(): List<File> {
        return logDirectory?.listFiles { file ->
            file.name.startsWith("app_log_") && file.name.endsWith(".txt")
        }?.toList() ?: emptyList()
    }
    
    /**
     * Clear all log files
     */
    fun clearLogs() {
        try {
            getLogFiles().forEach { it.delete() }
            logBuffer.clear()
            i("AppLogger", "All logs cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
    
    /**
     * Shutdown the logger
     */
    fun shutdown() {
        scope.launch {
            flushLogs()
            scope.cancel()
        }
    }
}

/**
 * Log levels
 */
enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}

/**
 * Log entry data class
 */
private data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable?
)

/**
 * Release tree for production builds - only logs errors and warnings
 */
private class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.ERROR || priority == Log.WARN) {
            super.log(priority, tag, message, t)
        }
    }
}
