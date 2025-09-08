package com.example.template.data.import

/**
 * Result of importing a single table
 */
data class TableImportResult(
    val tableName: String,
    val recordsProcessed: Int,
    val recordsImported: Int,
    val recordsSkipped: Int,
    val recordsFailed: Int,
    val errors: List<ImportError>,
    val warnings: List<ImportWarning>,
    val idMappings: Map<String, Long> = emptyMap() // Maps old IDs to new IDs
) {
    val successRate: Double
        get() = if (recordsProcessed > 0) {
            (recordsImported.toDouble() / recordsProcessed) * 100
        } else 0.0
}

/**
 * Validation result for a single record
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ImportError> = emptyList(),
    val warnings: List<ImportWarning> = emptyList()
)

/**
 * Parsed CSV row data
 */
data class CsvRow(
    val rowNumber: Int,
    val data: Map<String, String>,
    val headers: List<String>
) {
    fun getValue(columnName: String): String? = data[columnName]
    
    fun getValueOrEmpty(columnName: String): String = data[columnName] ?: ""
    
    fun getIntValue(columnName: String): Int? = data[columnName]?.toIntOrNull()
    
    fun getLongValue(columnName: String): Long? = data[columnName]?.toLongOrNull()
    
    fun getDoubleValue(columnName: String): Double? = data[columnName]?.toDoubleOrNull()
    
    fun getBooleanValue(columnName: String): Boolean? = data[columnName]?.toBooleanStrictOrNull()
}
