package com.example.template.util.logger

import kotlin.system.measureTimeMillis

/**
 * Extension functions for convenient logging throughout the app
 */

/**
 * Log a function execution with timing
 */
inline fun <T> Any.logExecution(
    tag: String = this::class.java.simpleName,
    operation: String,
    crossinline block: () -> T
): T {
    var result: T
    val duration = measureTimeMillis {
        result = block()
    }
    AppLogger.performance(operation, duration)
    return result
}

/**
 * Log a suspend function execution with timing
 */
suspend inline fun <T> Any.logSuspendExecution(
    tag: String = this::class.java.simpleName,
    operation: String,
    crossinline block: suspend () -> T
): T {
    var result: T
    val duration = measureTimeMillis {
        result = block()
    }
    AppLogger.performance(operation, duration)
    return result
}

/**
 * Log user action with context
 */
fun Any.logUserAction(
    action: String,
    details: Map<String, Any>? = null
) {
    AppLogger.userAction(action, details)
}

/**
 * Log exception with automatic context
 */
fun Any.logException(
    message: String,
    throwable: Throwable,
    context: Map<String, Any>? = null
) {
    val tag = this::class.java.simpleName
    AppLogger.exception(tag, message, throwable, context)
}

/**
 * Log info message with automatic tag
 */
fun Any.logInfo(message: String) {
    val tag = this::class.java.simpleName
    AppLogger.i(tag, message)
}

/**
 * Log debug message with automatic tag
 */
fun Any.logDebug(message: String) {
    val tag = this::class.java.simpleName
    AppLogger.d(tag, message)
}

/**
 * Log warning message with automatic tag
 */
fun Any.logWarning(message: String, throwable: Throwable? = null) {
    val tag = this::class.java.simpleName
    AppLogger.w(tag, message, throwable)
}

/**
 * Log error message with automatic tag
 */
fun Any.logError(message: String, throwable: Throwable? = null) {
    val tag = this::class.java.simpleName
    AppLogger.e(tag, message, throwable)
}

/**
 * Log verbose message with automatic tag
 */
fun Any.logVerbose(message: String) {
    val tag = this::class.java.simpleName
    AppLogger.v(tag, message)
}

/**
 * Safe execution with logging
 */
inline fun <T> Any.safeExecute(
    operation: String,
    crossinline block: () -> T,
    crossinline onError: (Throwable) -> T
): T {
    return try {
        logDebug("Starting $operation")
        val result = block()
        logDebug("Completed $operation successfully")
        result
    } catch (e: Exception) {
        logException("Failed to execute $operation", e)
        onError(e)
    }
}

/**
 * Safe suspend execution with logging
 */
suspend inline fun <T> Any.safeSuspendExecute(
    operation: String,
    crossinline block: suspend () -> T,
    crossinline onError: (Throwable) -> T
): T {
    return try {
        logDebug("Starting $operation")
        val result = block()
        logDebug("Completed $operation successfully")
        result
    } catch (e: Exception) {
        logException("Failed to execute $operation", e)
        onError(e)
    }
}
