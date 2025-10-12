# Modern Logging Framework

This directory contains a comprehensive logging framework for the Offline Calorie Calculator app, designed with modern Android development practices and proper size management.

## Features

### 🚀 Modern & Efficient
- **Timber Integration**: Uses Timber for console logging with automatic tree management
- **Structured Logging**: Consistent log format with timestamps, levels, and context
- **Coroutine Support**: Non-blocking log writing with proper coroutine scope management
- **Performance Logging**: Built-in timing and performance measurement utilities

### 📁 Smart File Management
- **Automatic Log Rotation**: Files rotate when they reach 5MB (configurable)
- **Size Limits**: Maximum 3 log files kept (configurable)
- **Background Writing**: Logs are buffered and written asynchronously every 5 seconds
- **Storage Optimization**: Old log files are automatically cleaned up

### 🎯 Multiple Log Levels
- **VERBOSE**: Detailed debugging information
- **DEBUG**: Development debugging information
- **INFO**: General information and user actions
- **WARN**: Warning messages and recoverable errors
- **ERROR**: Error messages and exceptions

### 🔧 Configuration
- **Runtime Configuration**: Adjust log levels and file settings
- **Build Variants**: Different behavior for debug vs release builds
- **User Control**: Users can clear logs or adjust settings

## Usage

### Basic Logging

```kotlin
import com.example.template.util.logger.AppLogger

// Simple logging
AppLogger.i("MyClass", "User logged in successfully")
AppLogger.e("MyClass", "Database connection failed", exception)

// With context
AppLogger.exception("MyClass", "Failed to save user data", exception, mapOf(
    "userId" to userId,
    "operation" to "saveProfile"
))
```

### Extension Functions (Recommended)

```kotlin
import com.example.template.util.logger.logInfo
import com.example.template.util.logger.logException
import com.example.template.util.logger.logUserAction

class MyActivity {
    fun onUserAction() {
        // Automatic tag from class name
        logInfo("User clicked save button")
        
        // User action tracking
        logUserAction("Button Clicked", mapOf(
            "button" to "save",
            "screen" to "profile"
        ))
        
        // Exception logging with context
        try {
            riskyOperation()
        } catch (e: Exception) {
            logException("Failed to perform operation", e, mapOf(
                "operation" to "riskyOperation",
                "userId" to currentUserId
            ))
        }
    }
}
```

### Safe Execution

```kotlin
import com.example.template.util.logger.safeExecute
import com.example.template.util.logger.safeSuspendExecute

// For regular functions
val result = safeExecute(
    operation = "Load user data",
    block = { loadUserData() },
    onError = { e -> 
        // Handle error
        null 
    }
)

// For suspend functions
val result = safeSuspendExecute(
    operation = "Save to database",
    block = { saveToDatabase(data) },
    onError = { e -> 
        // Handle error
        false 
    }
)
```

### Performance Logging

```kotlin
import com.example.template.util.logger.logExecution

// Automatic timing
val result = logExecution("Database Query") {
    database.query("SELECT * FROM users")
}

// Manual performance logging
AppLogger.performance("Image Processing", durationMs, mapOf(
    "imageSize" to imageSize,
    "format" to "JPEG"
))
```

## Configuration

### Runtime Configuration

```kotlin
import com.example.template.util.logger.LoggingConfig

// Set log level
LoggingConfig.setLogLevel(LogLevel.DEBUG)

// Enable/disable file logging
LoggingConfig.setFileLoggingEnabled(false)

// Adjust file size limits
LoggingConfig.setMaxFileSize(10 * 1024 * 1024) // 10MB
LoggingConfig.setMaxFiles(5) // Keep 5 files
```

### Build Configuration

The framework automatically adjusts behavior based on build type:

- **Debug Builds**: All log levels enabled, console logging active
- **Release Builds**: Only WARN and ERROR logs, optimized for performance

## File Structure

```
app/src/main/java/com/example/template/util/logger/
├── AppLogger.kt              # Main logging framework
├── LoggingExtensions.kt      # Convenient extension functions
├── LoggingConfig.kt          # Configuration management
└── README.md                 # This documentation
```

## Log File Management

### Automatic Rotation
- Log files are automatically rotated when they reach the size limit
- Old files are cleaned up to maintain storage limits
- Files are named with timestamps for easy identification

### Manual Management
```kotlin
// Get all log files
val logFiles = AppLogger.getLogFiles()

// Clear all logs
AppLogger.clearLogs()

// Shutdown logger (called automatically in Application.onTerminate())
AppLogger.shutdown()
```

## Best Practices

### 1. Use Appropriate Log Levels
- **VERBOSE**: Detailed flow information
- **DEBUG**: Development debugging
- **INFO**: Important business events
- **WARN**: Recoverable issues
- **ERROR**: Failures and exceptions

### 2. Include Context
```kotlin
// Good
logException("Failed to save meal", e, mapOf(
    "mealId" to mealId,
    "userId" to userId,
    "operation" to "saveMeal"
))

// Avoid
logException("Error occurred", e)
```

### 3. Use Extension Functions
```kotlin
// Good - automatic tag
logInfo("User action completed")

// Avoid - manual tag
AppLogger.i("MyClass", "User action completed")
```

### 4. Performance Considerations
- Logs are written asynchronously to avoid blocking the UI
- File operations are batched for efficiency
- Release builds have reduced logging overhead

## Integration

The logging framework is automatically initialized in `FoodLogApplication.onCreate()` and will be available throughout the app lifecycle.

## Dependencies

- **Timber**: For console logging
- **Kotlin Coroutines**: For asynchronous log writing
- **Android Context**: For file system access

## Storage Impact

- **Maximum Storage**: ~15MB (3 files × 5MB each)
- **Automatic Cleanup**: Old files are removed automatically
- **User Control**: Users can clear logs if needed
- **Efficient Format**: Compressed log format with essential information only
