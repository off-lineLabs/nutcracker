package com.offlinelabs.nutcracker.data.import

/**
 * Result of a database import operation
 */
data class ImportResult(
    val isSuccess: Boolean,
    val totalRecordsProcessed: Int,
    val recordsImported: Int,
    val recordsSkipped: Int,
    val recordsFailed: Int,
    val errors: List<ImportError>,
    val warnings: List<ImportWarning>,
    val importDuration: Long, // in milliseconds
    val tablesImported: List<String>
) {
    val successRate: Double
        get() = if (totalRecordsProcessed > 0) {
            (recordsImported.toDouble() / totalRecordsProcessed) * 100
        } else 0.0
}

/**
 * Represents an error during import
 */
data class ImportError(
    val tableName: String,
    val rowNumber: Int,
    val fieldName: String?,
    val errorMessage: String,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

/**
 * Represents a warning during import
 */
data class ImportWarning(
    val tableName: String,
    val rowNumber: Int,
    val fieldName: String?,
    val warningMessage: String
)

/**
 * Severity levels for import errors
 */
enum class ErrorSeverity {
    WARNING,    // Non-critical issue, import can continue
    ERROR,      // Critical issue, record will be skipped
    FATAL       // Fatal issue, entire import should stop
}

/**
 * Progress information for import operations
 */
data class ImportProgress(
    val currentTable: String,
    val currentTableProgress: Int,
    val totalTables: Int,
    val overallProgress: Int,
    val recordsProcessed: Int,
    val recordsImported: Int,
    val isComplete: Boolean
)
