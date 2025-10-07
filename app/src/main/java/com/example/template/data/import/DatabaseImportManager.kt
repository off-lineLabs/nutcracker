package com.example.template.data.import

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.template.data.AppDatabase
import com.example.template.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Modern database import manager using Room database and DocumentFile API
 * Imports CSV files from ZIP archive with comprehensive validation
 */
class DatabaseImportManager(
    private val context: Context,
    private val database: AppDatabase
) {
    
    companion object {
        // Expected CSV files in the ZIP archive
        private const val MEALS_CSV = "meals.csv"
        private const val USER_GOALS_CSV = "user_goals.csv"
        private const val MEAL_CHECK_INS_CSV = "meal_check_ins.csv"
        private const val EXERCISES_CSV = "exercises.csv"
        private const val EXERCISE_LOGS_CSV = "exercise_logs.csv"
        private const val PILLS_CSV = "pills.csv"
        private const val PILL_CHECK_INS_CSV = "pill_check_ins.csv"
        private const val EXPORT_INFO_CSV = "export_info.csv"
        
        // Import order to maintain referential integrity
        private val IMPORT_ORDER = listOf(
            MEALS_CSV,
            EXERCISES_CSV,
            PILLS_CSV,
            USER_GOALS_CSV,
            MEAL_CHECK_INS_CSV,
            EXERCISE_LOGS_CSV,
            PILL_CHECK_INS_CSV
        )
        
        // Date formats for parsing
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        private val DATETIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
    }
    
    private val _importProgress = MutableStateFlow<ImportProgress?>(null)
    val importProgress: StateFlow<ImportProgress?> = _importProgress.asStateFlow()
    
    private val idMappingManager = IdMappingManager()
    
    /**
     * Import database from CSV files in ZIP archive
     * @param sourceUri The URI of the ZIP file to import
     * @param createBackup Whether to create a backup before importing
     * @return Result containing import results and any errors
     */
    suspend fun importDatabase(
        sourceUri: Uri, 
        createBackup: Boolean = true
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val tableResults = mutableListOf<TableImportResult>()
        val tablesImported = mutableListOf<String>()
        
        try {
            // Validate source file
            val documentFile = DocumentFile.fromSingleUri(context, sourceUri)
                ?: return@withContext Result.failure(Exception("Invalid source URI"))
            
            if (!documentFile.exists()) {
                return@withContext Result.failure(Exception("Source file does not exist"))
            }
            
            // Create backup if requested
            if (createBackup) {
                createBackup()
            }
            
            // Clear ID mappings for fresh import
            idMappingManager.clear()
            
            // Open ZIP file
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Cannot open source file"))
            
            ZipInputStream(inputStream).use { zipIn ->
                val csvFiles = extractCsvFiles(zipIn)
                
                // Import tables in correct order
                for ((index, tableName) in IMPORT_ORDER.withIndex()) {
                    val csvData = csvFiles[tableName]
                    if (csvData != null) {
                        _importProgress.value = ImportProgress(
                            currentTable = tableName,
                            currentTableProgress = 0,
                            totalTables = IMPORT_ORDER.size,
                            overallProgress = (index * 100) / IMPORT_ORDER.size,
                            recordsProcessed = 0,
                            recordsImported = 0,
                            isComplete = false
                        )
                        
                        val tableResult = importTable(tableName, csvData)
                        tableResults.add(tableResult)
                        tablesImported.add(tableName)
                        
                        errors.addAll(tableResult.errors)
                        warnings.addAll(tableResult.warnings)
                        
                        _importProgress.value = ImportProgress(
                            currentTable = tableName,
                            currentTableProgress = 100,
                            totalTables = IMPORT_ORDER.size,
                            overallProgress = ((index + 1) * 100) / IMPORT_ORDER.size,
                            recordsProcessed = tableResult.recordsProcessed,
                            recordsImported = tableResult.recordsImported,
                            isComplete = false
                        )
                    } else {
                        warnings.add(ImportWarning(
                            tableName = tableName,
                            rowNumber = 0,
                            fieldName = null,
                            warningMessage = "Table $tableName not found in import file"
                        ))
                    }
                }
            }
            
            val endTime = System.currentTimeMillis()
            val totalRecordsProcessed = tableResults.sumOf { it.recordsProcessed }
            val totalRecordsImported = tableResults.sumOf { it.recordsImported }
            val totalRecordsSkipped = tableResults.sumOf { it.recordsSkipped }
            val totalRecordsFailed = tableResults.sumOf { it.recordsFailed }
            
            _importProgress.value = ImportProgress(
                currentTable = "",
                currentTableProgress = 100,
                totalTables = IMPORT_ORDER.size,
                overallProgress = 100,
                recordsProcessed = totalRecordsProcessed,
                recordsImported = totalRecordsImported,
                isComplete = true
            )
            
            val result = ImportResult(
                isSuccess = errors.none { it.severity == ErrorSeverity.FATAL },
                totalRecordsProcessed = totalRecordsProcessed,
                recordsImported = totalRecordsImported,
                recordsSkipped = totalRecordsSkipped,
                recordsFailed = totalRecordsFailed,
                errors = errors,
                warnings = warnings,
                importDuration = endTime - startTime,
                tablesImported = tablesImported
            )
            
            Result.success(result)
            
        } catch (e: Exception) {
            _importProgress.value = null
            Result.failure(e)
        }
    }
    
    /**
     * Extract CSV files from ZIP archive
     */
    private fun extractCsvFiles(zipIn: ZipInputStream): Map<String, String> {
        val csvFiles = mutableMapOf<String, String>()
        var entry: ZipEntry? = zipIn.nextEntry
        
        while (entry != null) {
            if (entry.name.endsWith(".csv")) {
                val csvContent = zipIn.readBytes().toString(Charsets.UTF_8)
                csvFiles[entry.name] = csvContent
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        
        return csvFiles
    }
    
    /**
     * Import a single table from CSV data
     */
    private suspend fun importTable(tableName: String, csvData: String): TableImportResult {
        return when (tableName) {
            MEALS_CSV -> importMeals(csvData)
            USER_GOALS_CSV -> importUserGoals(csvData)
            MEAL_CHECK_INS_CSV -> importMealCheckIns(csvData)
            EXERCISES_CSV -> importExercises(csvData)
            EXERCISE_LOGS_CSV -> importExerciseLogs(csvData)
            PILLS_CSV -> importPills(csvData)
            PILL_CHECK_INS_CSV -> importPillCheckIns(csvData)
            else -> TableImportResult(
                tableName = tableName,
                recordsProcessed = 0,
                recordsImported = 0,
                recordsSkipped = 0,
                recordsFailed = 0,
                errors = listOf(ImportError(
                    tableName = tableName,
                    rowNumber = 0,
                    fieldName = null,
                    errorMessage = "Unknown table type: $tableName",
                    severity = ErrorSeverity.ERROR
                )),
                warnings = emptyList()
            )
        }
    }
    
    /**
     * Create a backup of the current database
     */
    private suspend fun createBackup() {
        // TODO: Implement backup creation
        // This could export current database to a backup file
        // For now, we'll skip backup creation to keep the implementation simple
    }
    
    /**
     * Parse CSV data into rows
     */
    private fun parseCsvData(csvData: String): List<CsvRow> {
        val lines = csvData.trim().split("\n")
        if (lines.isEmpty()) return emptyList()
        
        val headers = parseCsvLine(lines[0])
        val rows = mutableListOf<CsvRow>()
        
        for (i in 1 until lines.size) {
            val values = parseCsvLine(lines[i])
            val data = headers.zip(values).toMap()
            rows.add(CsvRow(i + 1, data, headers))
        }
        
        return rows
    }
    
    /**
     * Parse a single CSV line, handling quoted values
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        
        while (i < line.length) {
            val char = line[i]
            
            when {
                char == '"' && !inQuotes -> {
                    inQuotes = true
                }
                char == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        // Escaped quote
                        current.append('"')
                        i++ // Skip next quote
                    } else {
                        inQuotes = false
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> {
                    current.append(char)
                }
            }
            i++
        }
        
        result.add(current.toString())
        return result
    }
    
    // Import methods for each table
    private suspend fun importMeals(csvData: String): TableImportResult {
        val rows = parseCsvData(csvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            rows.forEach { row ->
                val validation = ImportValidators.validateMealData(row)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
                
                if (validation.isValid) {
                    try {
                        val meal = Meal(
                            id = 0, // Let Room auto-generate
                            name = row.getValueOrEmpty("name"),
                            calories = row.getIntValue("calories") ?: 0,
                            carbohydrates_g = row.getDoubleValue("carbohydrates_g") ?: 0.0,
                            protein_g = row.getDoubleValue("protein_g") ?: 0.0,
                            fat_g = row.getDoubleValue("fat_g") ?: 0.0,
                            fiber_g = row.getDoubleValue("fiber_g") ?: 0.0,
                            sodium_mg = row.getDoubleValue("sodium_mg") ?: 0.0,
                            servingSize_value = row.getDoubleValue("servingSize_value") ?: 100.0,
                            servingSize_unit = ServingSizeUnit.fromAbbreviation(row.getValue("servingSize_unit") ?: "") ?: ServingSizeUnit.getDefault(),
                            notes = row.getValue("notes").takeIf { !it.isNullOrBlank() }
                        )
                        
                        val newId = database.mealDao().insertMeal(meal)
                        val oldId = row.getValue("id")?.toLongOrNull()
                        if (oldId != null) {
                            idMappings[oldId.toString()] = newId
                            idMappingManager.addMapping("meals", oldId.toString(), newId)
                        }
                        imported++
                    } catch (e: Exception) {
                        errors.add(ImportError(
                            tableName = "meals",
                            rowNumber = row.rowNumber,
                            fieldName = null,
                            errorMessage = "Failed to insert meal: ${e.message}",
                            severity = ErrorSeverity.ERROR
                        ))
                        failed++
                    }
                } else {
                    skipped++
                }
            }
            
            TableImportResult(
                tableName = "meals",
                recordsProcessed = rows.size,
                recordsImported = imported,
                recordsSkipped = skipped,
                recordsFailed = failed,
                errors = errors,
                warnings = warnings,
                idMappings = idMappings
            )
        }
    }
    
    private suspend fun importUserGoals(csvData: String): TableImportResult {
        val rows = parseCsvData(csvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            rows.forEach { row ->
                val validation = ImportValidators.validateUserGoalData(row)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
                
                if (validation.isValid) {
                    try {
                        val userGoal = UserGoal(
                            id = 1, // Fixed ID for user goals
                            caloriesGoal = row.getIntValue("caloriesGoal") ?: 2000,
                            carbsGoal_g = row.getIntValue("carbsGoal_g") ?: 250,
                            proteinGoal_g = row.getIntValue("proteinGoal_g") ?: 100,
                            fatGoal_g = row.getIntValue("fatGoal_g") ?: 65,
                            fiberGoal_g = row.getIntValue("fiberGoal_g") ?: 30,
                            sodiumGoal_mg = row.getIntValue("sodiumGoal_mg") ?: 2300
                        )
                        
                        database.userGoalDao().upsertUserGoal(userGoal)
                        imported++
                    } catch (e: Exception) {
                        errors.add(ImportError(
                            tableName = "user_goals",
                            rowNumber = row.rowNumber,
                            fieldName = null,
                            errorMessage = "Failed to insert user goal: ${e.message}",
                            severity = ErrorSeverity.ERROR
                        ))
                        failed++
                    }
                } else {
                    skipped++
                }
            }
            
            TableImportResult(
                tableName = "user_goals",
                recordsProcessed = rows.size,
                recordsImported = imported,
                recordsSkipped = skipped,
                recordsFailed = failed,
                errors = errors,
                warnings = warnings
            )
        }
    }
    
    private suspend fun importMealCheckIns(csvData: String): TableImportResult {
        val rows = parseCsvData(csvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            rows.forEach { row ->
                val validation = ImportValidators.validateMealCheckInData(row)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
                
                if (validation.isValid) {
                    try {
                        val oldMealId = row.getLongValue("mealId")
                        val newMealId = if (oldMealId != null) {
                            // Try to find the new meal ID from our mappings
                            idMappingManager.getNewId("meals", oldMealId.toString())
                                ?: return@forEach // Skip if parent meal doesn't exist
                        } else {
                            return@forEach
                        }
                        
                        val checkIn = MealCheckIn(
                            id = 0, // Let Room auto-generate
                            mealId = newMealId,
                            checkInDate = row.getValueOrEmpty("checkInDate"),
                            checkInDateTime = row.getValueOrEmpty("checkInDateTime"),
                            servingSize = row.getDoubleValue("servingSize") ?: 1.0,
                            notes = row.getValue("notes").takeIf { !it.isNullOrBlank() }
                        )
                        
                        val newId = database.mealCheckInDao().insertMealCheckIn(checkIn)
                        val oldId = row.getValue("id")?.toLongOrNull()
                        if (oldId != null) {
                            idMappings[oldId.toString()] = newId
                        }
                        imported++
                    } catch (e: Exception) {
                        errors.add(ImportError(
                            tableName = "meal_check_ins",
                            rowNumber = row.rowNumber,
                            fieldName = null,
                            errorMessage = "Failed to insert meal check-in: ${e.message}",
                            severity = ErrorSeverity.ERROR
                        ))
                        failed++
                    }
                } else {
                    skipped++
                }
            }
            
            TableImportResult(
                tableName = "meal_check_ins",
                recordsProcessed = rows.size,
                recordsImported = imported,
                recordsSkipped = skipped,
                recordsFailed = failed,
                errors = errors,
                warnings = warnings,
                idMappings = idMappings
            )
        }
    }
    
    private suspend fun importExercises(csvData: String): TableImportResult {
        val rows = parseCsvData(csvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            rows.forEach { row ->
                val validation = ImportValidators.validateExerciseData(row)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
                
                if (validation.isValid) {
                    try {
                        val exercise = Exercise(
                            id = 0, // Let Room auto-generate
                            name = row.getValueOrEmpty("name"),
                            kcalBurnedPerUnit = row.getDoubleValue("kcalBurnedPerUnit"),
                            defaultWeight = row.getDoubleValue("defaultWeight") ?: 0.0,
                            defaultReps = row.getIntValue("defaultReps") ?: 0,
                            defaultSets = row.getIntValue("defaultSets") ?: 0,
                            category = row.getValueOrEmpty("category").takeIf { it.isNotBlank() } ?: "strength",
                            equipment = row.getValue("equipment").takeIf { !it.isNullOrBlank() },
                            primaryMuscles = parseMuscleList(row.getValue("primaryMuscles")),
                            secondaryMuscles = parseMuscleList(row.getValue("secondaryMuscles")),
                            force = row.getValue("force").takeIf { !it.isNullOrBlank() },
                            notes = row.getValue("notes").takeIf { !it.isNullOrBlank() }
                        )
                        
                        val newId = database.exerciseDao().upsertExercise(exercise)
                        val oldId = row.getValue("id")?.toLongOrNull()
                        if (oldId != null) {
                            idMappings[oldId.toString()] = newId
                            idMappingManager.addMapping("exercises", oldId.toString(), newId)
                        }
                        imported++
                    } catch (e: Exception) {
                        errors.add(ImportError(
                            tableName = "exercises",
                            rowNumber = row.rowNumber,
                            fieldName = null,
                            errorMessage = "Failed to insert exercise: ${e.message}",
                            severity = ErrorSeverity.ERROR
                        ))
                        failed++
                    }
                } else {
                    skipped++
                }
            }
            
            TableImportResult(
                tableName = "exercises",
                recordsProcessed = rows.size,
                recordsImported = imported,
                recordsSkipped = skipped,
                recordsFailed = failed,
                errors = errors,
                warnings = warnings,
                idMappings = idMappings
            )
        }
    }
    
    private suspend fun importExerciseLogs(csvData: String): TableImportResult {
        val rows = parseCsvData(csvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            rows.forEach { row ->
                val validation = ImportValidators.validateExerciseLogData(row)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
                
                if (validation.isValid) {
                    try {
                        val oldExerciseId = row.getLongValue("exerciseId")
                        val newExerciseId = if (oldExerciseId != null) {
                            // Try to find the new exercise ID from our mappings
                            idMappingManager.getNewId("exercises", oldExerciseId.toString())
                                ?: return@forEach // Skip if parent exercise doesn't exist
                        } else {
                            return@forEach
                        }
                        
                        val exerciseLog = ExerciseLog(
                            id = 0, // Let Room auto-generate
                            exerciseId = newExerciseId,
                            logDate = row.getValueOrEmpty("logDate"),
                            logDateTime = row.getValueOrEmpty("logDateTime"),
                            weight = row.getDoubleValue("weight") ?: 0.0,
                            reps = row.getIntValue("reps") ?: 0,
                            sets = row.getIntValue("sets") ?: 0,
                            caloriesBurned = row.getDoubleValue("caloriesBurned") ?: 0.0,
                            notes = row.getValue("notes").takeIf { !it.isNullOrBlank() }
                        )
                        
                        val newId = database.exerciseLogDao().insertExerciseLog(exerciseLog)
                        val oldId = row.getValue("id")?.toLongOrNull()
                        if (oldId != null) {
                            idMappings[oldId.toString()] = newId
                        }
                        imported++
                    } catch (e: Exception) {
                        errors.add(ImportError(
                            tableName = "exercise_logs",
                            rowNumber = row.rowNumber,
                            fieldName = null,
                            errorMessage = "Failed to insert exercise log: ${e.message}",
                            severity = ErrorSeverity.ERROR
                        ))
                        failed++
                    }
                } else {
                    skipped++
                }
            }
            
            TableImportResult(
                tableName = "exercise_logs",
                recordsProcessed = rows.size,
                recordsImported = imported,
                recordsSkipped = skipped,
                recordsFailed = failed,
                errors = errors,
                warnings = warnings,
                idMappings = idMappings
            )
        }
    }
    
    private suspend fun importPills(csvData: String): TableImportResult {
        val rows = parseCsvData(csvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            rows.forEach { row ->
                val validation = ImportValidators.validatePillData(row)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
                
                if (validation.isValid) {
                    try {
                        val pill = Pill(
                            id = 0, // Let Room auto-generate
                            name = row.getValueOrEmpty("name")
                        )
                        
                        val newId = database.pillDao().insertPill(pill)
                        val oldId = row.getValue("id")?.toLongOrNull()
                        if (oldId != null) {
                            idMappings[oldId.toString()] = newId
                            idMappingManager.addMapping("pills", oldId.toString(), newId)
                        }
                        imported++
                    } catch (e: Exception) {
                        errors.add(ImportError(
                            tableName = "pills",
                            rowNumber = row.rowNumber,
                            fieldName = null,
                            errorMessage = "Failed to insert pill: ${e.message}",
                            severity = ErrorSeverity.ERROR
                        ))
                        failed++
                    }
                } else {
                    skipped++
                }
            }
            
            TableImportResult(
                tableName = "pills",
                recordsProcessed = rows.size,
                recordsImported = imported,
                recordsSkipped = skipped,
                recordsFailed = failed,
                errors = errors,
                warnings = warnings,
                idMappings = idMappings
            )
        }
    }
    
    private suspend fun importPillCheckIns(csvData: String): TableImportResult {
        val rows = parseCsvData(csvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            rows.forEach { row ->
                val validation = ImportValidators.validatePillCheckInData(row)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
                
                if (validation.isValid) {
                    try {
                        val oldPillId = row.getLongValue("pillId")
                        val newPillId = if (oldPillId != null) {
                            // Try to find the new pill ID from our mappings
                            idMappingManager.getNewId("pills", oldPillId.toString())
                                ?: return@forEach // Skip if parent pill doesn't exist
                        } else {
                            return@forEach
                        }
                        
                        val timestampStr = row.getValueOrEmpty("timestamp")
                        val timestamp = try {
                            java.time.LocalDateTime.parse(timestampStr)
                        } catch (e: Exception) {
                            java.time.LocalDateTime.now()
                        }
                        
                        val pillCheckIn = PillCheckIn(
                            id = 0, // Let Room auto-generate
                            pillId = newPillId,
                            timestamp = timestamp
                        )
                        
                        val newId = database.pillCheckInDao().insertPillCheckIn(pillCheckIn)
                        val oldId = row.getValue("id")?.toLongOrNull()
                        if (oldId != null) {
                            idMappings[oldId.toString()] = newId
                        }
                        imported++
                    } catch (e: Exception) {
                        errors.add(ImportError(
                            tableName = "pill_check_ins",
                            rowNumber = row.rowNumber,
                            fieldName = null,
                            errorMessage = "Failed to insert pill check-in: ${e.message}",
                            severity = ErrorSeverity.ERROR
                        ))
                        failed++
                    }
                } else {
                    skipped++
                }
            }
            
            TableImportResult(
                tableName = "pill_check_ins",
                recordsProcessed = rows.size,
                recordsImported = imported,
                recordsSkipped = skipped,
                recordsFailed = failed,
                errors = errors,
                warnings = warnings,
                idMappings = idMappings
            )
        }
    }
    
    /**
     * Parse muscle list from semicolon-separated string
     */
    private fun parseMuscleList(muscleString: String?): List<String> {
        return if (muscleString.isNullOrBlank()) {
            emptyList()
        } else {
            muscleString.split(";").map { it.trim() }.filter { it.isNotBlank() }
        }
    }
}
