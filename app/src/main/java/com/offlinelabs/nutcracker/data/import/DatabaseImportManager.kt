package com.offlinelabs.nutcracker.data.import

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import com.offlinelabs.nutcracker.data.repair.LegacyRecipeRepair
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Modern database import manager using Room database and DocumentFile API
 * Imports TSV files from ZIP archive with comprehensive validation
 */
class DatabaseImportManager(
    private val context: Context,
    private val database: AppDatabase
) {
    
    companion object {
        // Expected TSV files in the ZIP archive
        private const val MEALS_TSV = "meals.tsv"
        private const val USER_GOALS_TSV = "user_goals.tsv"
        private const val MEAL_CHECK_INS_TSV = "meal_check_ins.tsv"
        private const val EXERCISES_TSV = "exercises.tsv"
        private const val EXERCISE_LOGS_TSV = "exercise_logs.tsv"
        private const val PILLS_TSV = "pills.tsv"
        private const val PILL_CHECK_INS_TSV = "pill_check_ins.tsv"
        private const val TAGS_TSV = "tags.tsv"
        private const val MEAL_TAGS_TSV = "meal_tags.tsv"
        private const val EXERCISE_TAGS_TSV = "exercise_tags.tsv"
        private const val RECIPES_TSV = "recipes.tsv"
        private const val RECIPE_INGREDIENTS_TSV = "recipe_ingredients.tsv"
        private const val EXPORT_INFO_TSV = "export_info.tsv"
        
        // Import order to maintain referential integrity
        private val IMPORT_ORDER = listOf(
            MEALS_TSV,
            EXERCISES_TSV,
            PILLS_TSV,
            TAGS_TSV,
            USER_GOALS_TSV,
            RECIPES_TSV,
            RECIPE_INGREDIENTS_TSV,
            MEAL_CHECK_INS_TSV,
            EXERCISE_LOGS_TSV,
            PILL_CHECK_INS_TSV,
            MEAL_TAGS_TSV,
            EXERCISE_TAGS_TSV
        )
        
        const val AUTO_BACKUP_FILENAME = "auto-before-restore.zip"
        
        // Date formats for parsing
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        private val DATETIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
    }
    
    private val _importProgress = MutableStateFlow<ImportProgress?>(null)
    val importProgress: StateFlow<ImportProgress?> = _importProgress.asStateFlow()
    
    private val idMappingManager = IdMappingManager()
    private var imagePathMappings: Map<String, String> = emptyMap()
    
    private var mode: ImportMode = ImportMode.MERGE
    // Old ids of backup recipes that matched an existing recipe (MERGE): their ingredients are not re-added
    private val matchedRecipeOldIds = mutableSetOf<Long>()
    // Ids of placeholder meal copies we inserted (legacy id <= 0 rows); removed again if unreferenced
    private val placeholderCopyIds = mutableSetOf<Long>()
    // Natural keys of the backup's parent rows by old id, used to dedupe child rows in MERGE
    private val backupMealKeys = mutableMapOf<Long, ImportMergeKeys.MealKey>()
    private val backupExerciseKeys = mutableMapOf<Long, ImportMergeKeys.ExerciseKey>()
    private val backupTagKeys = mutableMapOf<Long, ImportMergeKeys.TagKey>()
    private val backupPillNames = mutableMapOf<Long, String>()
    // Parents inserted by this import: their children can't be duplicates of pre-existing rows
    private val insertedMealIds = mutableSetOf<Long>()
    private val insertedExerciseIds = mutableSetOf<Long>()
    private val insertedTagIds = mutableSetOf<Long>()
    private val insertedPillIds = mutableSetOf<Long>()
    
    private class ImportAbortedException(message: String) : Exception(message)
    
    /**
     * Import database from TSV files in ZIP archive.
     * The whole database part runs in one transaction: any exception rolls everything back.
     * @param sourceUri The URI of the ZIP file to import
     * @param mode MERGE skips rows that already exist; REPLACE backs up the current data to
     *  filesDir/backups/auto-before-restore.zip, wipes all tables and restores the backup
     * @return Result containing import results and any errors
     */
    suspend fun importDatabase(
        sourceUri: Uri, 
        mode: ImportMode = ImportMode.MERGE
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
            
            this@DatabaseImportManager.mode = mode
            
            // Clear state for a fresh import
            idMappingManager.clear()
            matchedRecipeOldIds.clear()
            placeholderCopyIds.clear()
            backupMealKeys.clear()
            backupExerciseKeys.clear()
            backupTagKeys.clear()
            backupPillNames.clear()
            insertedMealIds.clear()
            insertedExerciseIds.clear()
            insertedTagIds.clear()
            insertedPillIds.clear()
            
            // Open ZIP file and read it fully before touching the database
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Cannot open source file"))
            
            val (tsvFiles, mappings) = ZipInputStream(inputStream).use { extractZipContents(it) }
            imagePathMappings = mappings
            
            // Never wipe without a backup
            if (mode == ImportMode.REPLACE) {
                val backup = createBackup()
                if (backup.isFailure) {
                    return@withContext Result.failure(
                        Exception("Could not create safety backup, import aborted: ${backup.exceptionOrNull()?.message}")
                    )
                }
            }
            
            database.withTransaction {
                if (mode == ImportMode.REPLACE) {
                    wipeAllTables()
                }
                
                // Import tables in correct order
                for ((index, tableName) in IMPORT_ORDER.withIndex()) {
                    val tsvData = tsvFiles[tableName]
                    if (tsvData != null) {
                        _importProgress.value = ImportProgress(
                            currentTable = tableName,
                            currentTableProgress = 0,
                            totalTables = IMPORT_ORDER.size,
                            overallProgress = (index * 100) / IMPORT_ORDER.size,
                            recordsProcessed = 0,
                            recordsImported = 0,
                            isComplete = false
                        )
                        
                        val tableResult = importTable(tableName, tsvData)
                        tableResults.add(tableResult)
                        tablesImported.add(tableName)
                        
                        errors.addAll(tableResult.errors)
                        warnings.addAll(tableResult.warnings)
                        
                        // A restore is all-or-nothing: a row that failed to insert rolls everything back
                        if (mode == ImportMode.REPLACE && tableResult.recordsFailed > 0) {
                            throw ImportAbortedException(
                                "Restore aborted, nothing was changed: ${tableResult.recordsFailed} rows failed in $tableName"
                            )
                        }
                        
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
                
                // Drop placeholder meal copies that no check-in ended up using
                placeholderCopyIds.forEach { database.mealDao().deleteMealIfUnreferenced(it) }
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
     * Extract both TSV files and images from ZIP archive in a single pass
     * @return Pair of (TSV files map, image path mappings)
     */
    private fun extractZipContents(zipIn: ZipInputStream): Pair<Map<String, String>, Map<String, String>> {
        val tsvFiles = mutableMapOf<String, String>()
        val imagePathMappings = mutableMapOf<String, String>() // Old path -> New path
        var entry: ZipEntry? = zipIn.nextEntry
        
        // Create image directories
        val mealImagesDir = File(context.filesDir, "food_images")
        val exerciseImagesDir = File(context.filesDir, "exercise_images")
        if (!mealImagesDir.exists()) mealImagesDir.mkdirs()
        if (!exerciseImagesDir.exists()) exerciseImagesDir.mkdirs()
        
        while (entry != null) {
            when {
                // Extract TSV files
                entry.name.endsWith(".tsv") -> {
                    val tsvContent = zipIn.readBytes().toString(Charsets.UTF_8)
                    tsvFiles[entry.name] = tsvContent
                }
                // Extract meal images
                entry.name.startsWith("images/meals/") && !entry.isDirectory -> {
                    val fileName = File(entry.name).name
                    val localFile = File(mealImagesDir, fileName)
                    
                    // Write image to local storage
                    localFile.outputStream().use { output ->
                        zipIn.copyTo(output)
                    }
                    
                    // Map old path to new path
                    imagePathMappings[entry.name] = localFile.absolutePath
                    com.offlinelabs.nutcracker.util.logger.AppLogger.d("DatabaseImportManager", "Extracted meal image: ${entry.name} -> ${localFile.absolutePath}")
                }
                // Extract recipe images (recipes keep their pictures in the same folder as meals)
                entry.name.startsWith("images/recipes/") && !entry.isDirectory -> {
                    val fileName = File(entry.name).name
                    val localFile = File(mealImagesDir, fileName)
                    localFile.outputStream().use { output ->
                        zipIn.copyTo(output)
                    }
                    imagePathMappings[entry.name] = localFile.absolutePath
                }
                // Extract exercise images
                entry.name.startsWith("images/exercises/") && !entry.isDirectory -> {
                    val fileName = File(entry.name).name
                    val localFile = File(exerciseImagesDir, fileName)
                    
                    // Write image to local storage
                    localFile.outputStream().use { output ->
                        zipIn.copyTo(output)
                    }
                    
                    // Map old path to new path
                    imagePathMappings[entry.name] = localFile.absolutePath
                    com.offlinelabs.nutcracker.util.logger.AppLogger.d("DatabaseImportManager", "Extracted exercise image: ${entry.name} -> ${localFile.absolutePath}")
                }
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        
        return Pair(tsvFiles, imagePathMappings)
    }
    
    /**
     * Import a single table from TSV data
     */
    private suspend fun importTable(tableName: String, tsvData: String): TableImportResult {
        return when (tableName) {
            MEALS_TSV -> importMeals(tsvData)
            USER_GOALS_TSV -> importUserGoals(tsvData)
            RECIPES_TSV -> importRecipes(tsvData)
            RECIPE_INGREDIENTS_TSV -> importRecipeIngredients(tsvData)
            MEAL_CHECK_INS_TSV -> importMealCheckIns(tsvData)
            EXERCISES_TSV -> importExercises(tsvData)
            EXERCISE_LOGS_TSV -> importExerciseLogs(tsvData)
            PILLS_TSV -> importPills(tsvData)
            PILL_CHECK_INS_TSV -> importPillCheckIns(tsvData)
            TAGS_TSV -> importTags(tsvData)
            MEAL_TAGS_TSV -> importMealTags(tsvData)
            EXERCISE_TAGS_TSV -> importExerciseTags(tsvData)
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
    private suspend fun createBackup(): Result<String> {
        val dir = File(context.filesDir, "backups")
        val target = File(dir, AUTO_BACKUP_FILENAME)
        val temp = File(dir, "$AUTO_BACKUP_FILENAME.tmp")
        return try {
            dir.mkdirs()
            val exported = com.offlinelabs.nutcracker.data.export.DatabaseExportManager(context, database).exportToFile(temp)
            if (exported.isFailure) {
                temp.delete()
                return Result.failure(exported.exceptionOrNull() ?: Exception("Backup failed"))
            }
            // Only the latest automatic backup is kept
            temp.copyTo(target, overwrite = true)
            temp.delete()
            Result.success(target.absolutePath)
        } catch (e: Exception) {
            temp.delete()
            Result.failure(e)
        }
    }
    
    /**
     * Delete every user row in FK-safe order (junctions and logs first). Runs inside the import transaction.
     */
    private suspend fun wipeAllTables() {
        database.mealTagDao().deleteAllMealTags()
        database.exerciseTagDao().deleteAllExerciseTags()
        database.recipeIngredientDao().deleteAllIngredients()
        database.mealCheckInDao().deleteAllCheckIns()
        database.exerciseLogDao().deleteAllLogs()
        database.pillCheckInDao().deleteAllPillCheckIns()
        database.recipeDao().deleteAllRecipes()
        database.mealDao().deleteAllMeals()
        database.exerciseDao().deleteAllExercises()
        database.pillDao().deleteAllPills()
        database.tagDao().deleteAllTags()
        database.userGoalDao().deleteAllUserGoals()
    }
    
    /**
     * Parse TSV data into rows
     */
    private fun parseCsvData(csvData: String): List<CsvRow> {
        val lines = csvData.trim().split("\n")
        if (lines.isEmpty()) return emptyList()
        
        val headers = parseTsvLine(lines[0])
        val rows = mutableListOf<CsvRow>()
        
        for (i in 1 until lines.size) {
            val values = parseTsvLine(lines[i])
            val data = headers.zip(values).toMap()
            rows.add(CsvRow(i + 1, data, headers))
        }
        
        return rows
    }
    
    /**
     * Parse a single TSV line, handling escaped characters
     */
    private fun parseTsvLine(line: String): List<String> {
        return line.split('\t').map { unescapeTsv(it) }
    }
    
    /**
     * Unescape TSV values
     */
    private fun unescapeTsv(value: String): String {
        return TsvCodec.unescape(value)
    }
    
    // Import methods for each table
    private suspend fun importMeals(tsvData: String): TableImportResult {
        val rows = parseCsvData(tsvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            // Rows that existed before this import (first/lowest id wins); only used in MERGE mode
            val existingIds = ImportMergeKeys.ExistingIdQueues(
                if (mode == ImportMode.MERGE) {
                    database.mealDao().getAllMealsIncludingHiddenSync().map { m ->
                        ImportMergeKeys.mealKey(m.name, m.brand, m.calories, m.servingSize_value, m.servingSize_unit.abbreviation, m.isVisible) to m.id
                    }
                } else emptyList()
            )
            
            rows.forEach { row ->
                val validation = ImportValidators.validateMealData(row)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
                
                if (validation.isValid) {
                    try {
                        // Map old local image path to new extracted path
                        val oldLocalImagePath = row.getValue("localImagePath")
                        val newLocalImagePath = mapImagePath(oldLocalImagePath, "meals")
                        
                        val oldId = row.getValue("id")?.toLongOrNull()
                        // Legacy placeholder meal (id <= 0): never a visible library meal
                        val isPlaceholder = oldId != null && oldId <= 0
                        
                        val meal = Meal(
                            id = 0, // Let Room auto-generate
                            name = row.getValueOrEmpty("name"),
                            brand = row.getValue("brand").takeIf { !it.isNullOrBlank() },
                            calories = row.getIntValue("calories") ?: 0,
                            carbohydrates_g = row.getDoubleValue("carbohydrates_g") ?: 0.0,
                            protein_g = row.getDoubleValue("protein_g") ?: 0.0,
                            fat_g = row.getDoubleValue("fat_g") ?: 0.0,
                            fiber_g = row.getDoubleValue("fiber_g") ?: 0.0,
                            sodium_mg = row.getDoubleValue("sodium_mg") ?: 0.0,
                            servingSize_value = row.getDoubleValue("servingSize_value") ?: 100.0,
                            servingSize_unit = ServingSizeUnit.fromAbbreviation(row.getValue("servingSize_unit") ?: "") ?: ServingSizeUnit.getDefault(),
                            notes = row.getValue("notes").takeIf { !it.isNullOrBlank() },
                            isVisible = if (isPlaceholder) false else (row.getValue("isVisible")?.toBoolean() ?: true),
                            saturatedFat_g = row.getDoubleValue("saturatedFat_g"),
                            sugars_g = row.getDoubleValue("sugars_g"),
                            cholesterol_mg = row.getDoubleValue("cholesterol_mg"),
                            vitaminC_mg = row.getDoubleValue("vitaminC_mg"),
                            calcium_mg = row.getDoubleValue("calcium_mg"),
                            iron_mg = row.getDoubleValue("iron_mg"),
                            imageUrl = row.getValue("imageUrl").takeIf { !it.isNullOrBlank() },
                            localImagePath = newLocalImagePath,
                            novaClassification = row.getValue("novaClassification")?.let { parseNovaClassification(it) },
                            greenScore = row.getValue("greenScore")?.let { parseGreenScore(it) },
                            nutriscore = row.getValue("nutriscore")?.let { parseNutriscore(it) },
                            ingredients = row.getValue("ingredients").takeIf { !it.isNullOrBlank() },
                            quantity = row.getValue("quantity").takeIf { !it.isNullOrBlank() },
                            servingSize = row.getValue("servingSize").takeIf { !it.isNullOrBlank() },
                            barcode = row.getValue("barcode").takeIf { !it.isNullOrBlank() },
                            source = row.getValue("source").takeIf { !it.isNullOrBlank() } ?: "manual"
                        )
                        
                        val mealKey = ImportMergeKeys.mealKey(
                            meal.name, meal.brand, meal.calories, meal.servingSize_value,
                            meal.servingSize_unit.abbreviation, meal.isVisible
                        )
                        if (oldId != null) backupMealKeys[oldId] = mealKey
                        val matchedId = existingIds.take(mealKey)
                        if (matchedId != null) {
                            if (oldId != null) {
                                idMappings[oldId.toString()] = matchedId
                                idMappingManager.addMapping("meals", oldId.toString(), matchedId)
                            }
                            skipped++
                            return@forEach
                        }
                        
                        val newId = database.mealDao().insertMeal(meal)
                        insertedMealIds.add(newId)
                        if (isPlaceholder) placeholderCopyIds.add(newId)
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
    
    private suspend fun importUserGoals(tsvData: String): TableImportResult {
        val rows = parseCsvData(tsvData)
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
                            sodiumGoal_mg = row.getIntValue("sodiumGoal_mg") ?: 5750
                        )
                        
                        if (mode == ImportMode.MERGE &&
                            !ImportMergeKeys.shouldApplyBackupGoal(database.userGoalDao().getUserGoalSync())
                        ) {
                            // Keep the user's own goal
                            skipped++
                        } else {
                            database.userGoalDao().upsertUserGoal(userGoal)
                            imported++
                        }
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
    
    private suspend fun importRecipes(tsvData: String): TableImportResult {
        val rows = parseCsvData(tsvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            val existingIds = ImportMergeKeys.ExistingIdQueues(
                if (mode == ImportMode.MERGE) {
                    database.recipeDao().getAllRecipesIncludingHiddenSync().map { r ->
                        ImportMergeKeys.recipeKey(r.name, r.createdDate) to r.id
                    }
                } else emptyList()
            )
            
            rows.forEach { row ->
                val validation = ImportValidators.validateRecipeData(row)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
                
                if (validation.isValid) {
                    try {
                        // Map old local image path to new extracted path
                        val oldLocalImagePath = row.getValue("localImagePath")
                        val newLocalImagePath = mapImagePath(oldLocalImagePath, "recipes")
                        
                        val recipe = Recipe(
                            id = 0, // Let Room auto-generate
                            name = row.getValueOrEmpty("name"),
                            description = row.getValue("description").takeIf { !it.isNullOrBlank() },
                            imageUrl = row.getValue("imageUrl").takeIf { !it.isNullOrBlank() },
                            localImagePath = newLocalImagePath,
                            isVisible = row.getValue("isVisible")?.toBoolean() ?: true,
                            createdDate = row.getValueOrEmpty("createdDate")
                        )
                        
                        val oldId = row.getValue("id")?.toLongOrNull()
                        val matchedId = existingIds.take(ImportMergeKeys.recipeKey(recipe.name, recipe.createdDate))
                        if (matchedId != null) {
                            if (oldId != null) {
                                idMappings[oldId.toString()] = matchedId
                                idMappingManager.addMapping("recipes", oldId.toString(), matchedId)
                                matchedRecipeOldIds.add(oldId)
                            }
                            skipped++
                            return@forEach
                        }
                        
                        val newId = database.recipeDao().insertRecipe(recipe)
                        if (oldId != null) {
                            idMappings[oldId.toString()] = newId
                            idMappingManager.addMapping("recipes", oldId.toString(), newId)
                        }
                        imported++
                    } catch (e: Exception) {
                        errors.add(ImportError(
                            tableName = "recipes",
                            rowNumber = row.rowNumber,
                            fieldName = null,
                            errorMessage = "Failed to insert recipe: ${e.message}",
                            severity = ErrorSeverity.ERROR
                        ))
                        failed++
                    }
                } else {
                    skipped++
                }
            }
            
            TableImportResult(
                tableName = "recipes",
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
    
    private suspend fun importRecipeIngredients(tsvData: String): TableImportResult {
        val rows = parseCsvData(tsvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            rows.forEach { row ->
                val validation = ImportValidators.validateRecipeIngredientData(row)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
                
                if (validation.isValid) {
                    try {
                        val oldRecipeId = row.getLongValue("recipeId")
                        if (oldRecipeId != null && oldRecipeId in matchedRecipeOldIds) {
                            // The existing recipe already has its ingredients
                            skipped++
                            return@forEach
                        }
                        val newRecipeId = if (oldRecipeId != null) {
                            // Try to find the new recipe ID from our mappings
                            idMappingManager.getNewId("recipes", oldRecipeId.toString())
                                ?: return@forEach // Skip if parent recipe doesn't exist
                        } else {
                            return@forEach
                        }
                        
                        val oldMealId = row.getLongValue("mealId")
                        val newMealId = if (oldMealId != null) {
                            // Try to find the new meal ID from our mappings
                            idMappingManager.getNewId("meals", oldMealId.toString())
                                ?: return@forEach // Skip if meal doesn't exist
                        } else {
                            return@forEach
                        }
                        
                        val recipeIngredient = RecipeIngredient(
                            id = 0, // Let Room auto-generate
                            recipeId = newRecipeId,
                            mealId = newMealId,
                            quantity = row.getDoubleValue("quantity") ?: 100.0,
                            unit = ServingSizeUnit.fromAbbreviation(row.getValue("unit") ?: "") ?: ServingSizeUnit.getDefault(),
                            order = row.getIntValue("order") ?: 0
                        )
                        
                        val newId = database.recipeIngredientDao().insertRecipeIngredient(recipeIngredient)
                        val oldId = row.getValue("id")?.toLongOrNull()
                        if (oldId != null) {
                            idMappings[oldId.toString()] = newId
                        }
                        imported++
                    } catch (e: Exception) {
                        errors.add(ImportError(
                            tableName = "recipe_ingredients",
                            rowNumber = row.rowNumber,
                            fieldName = null,
                            errorMessage = "Failed to insert recipe ingredient: ${e.message}",
                            severity = ErrorSeverity.ERROR
                        ))
                        failed++
                    }
                } else {
                    skipped++
                }
            }
            
            TableImportResult(
                tableName = "recipe_ingredients",
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
    
    private suspend fun importMealCheckIns(tsvData: String): TableImportResult {
        val rows = parseCsvData(tsvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            val existingKeys = HashSet<ImportMergeKeys.MealCheckInKey>()
            val existingLegacyKeys = HashSet<ImportMergeKeys.LegacyCheckInKey>()
            if (mode == ImportMode.MERGE) {
                val mealKeysById = existingMealKeysById()
                database.mealCheckInDao().getAllMealCheckInsSync().forEach { c ->
                    mealKeysById[c.mealId]?.let { existingKeys.add(ImportMergeKeys.mealCheckInKey(it, c.checkInDateTime)) }
                    if (LegacyRecipeRepair.parseRecipeId(c.notes) != null) {
                        existingLegacyKeys.add(ImportMergeKeys.legacyCheckInKey(c.checkInDateTime, c.notes))
                    }
                }
            }
            
            rows.forEach { row ->
                val validation = ImportValidators.validateMealCheckInData(row)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
                
                if (validation.isValid) {
                    try {
                        val oldMealId = row.getLongValue("mealId") ?: return@forEach
                        val dateTime = row.getValueOrEmpty("checkInDateTime")
                        var notes = row.getValue("notes").takeIf { !it.isNullOrBlank() }
                        
                        val legacyRecipeOldId = if (oldMealId <= 0) LegacyRecipeRepair.parseRecipeId(notes) else null
                        val newMealId: Long
                        if (legacyRecipeOldId != null) {
                            // Old app versions logged recipes against a shared placeholder meal: build a own snapshot
                            val mappedRecipeId = idMappingManager.getNewId("recipes", legacyRecipeOldId.toString())
                                ?.takeIf { database.recipeDao().getRecipeByIdSync(it) != null }
                            val mappedPlaceholderId = idMappingManager.getNewId("meals", oldMealId.toString())
                            if (mappedRecipeId != null) notes = ImportMergeKeys.legacyNotes(mappedRecipeId)
                            
                            if (mode == ImportMode.MERGE &&
                                ImportMergeKeys.legacyCheckInKey(dateTime, notes) in existingLegacyKeys
                            ) {
                                skipped++
                                return@forEach
                            }
                            
                            val resolved = when (val action = ImportMergeKeys.decideLegacyCheckIn(mappedRecipeId, mappedPlaceholderId)) {
                                is ImportMergeKeys.LegacyCheckInAction.SnapshotFromRecipe ->
                                    createLegacySnapshot(action.recipeId) ?: clonePlaceholder(mappedPlaceholderId)
                                is ImportMergeKeys.LegacyCheckInAction.CloneMeal -> clonePlaceholder(action.mealId)
                                ImportMergeKeys.LegacyCheckInAction.Skip -> null
                            }
                            if (resolved == null) {
                                warnings.add(ImportWarning(
                                    tableName = "meal_check_ins",
                                    rowNumber = row.rowNumber,
                                    fieldName = "mealId",
                                    warningMessage = "Legacy recipe check-in skipped: recipe and placeholder meal not found"
                                ))
                                skipped++
                                return@forEach
                            }
                            newMealId = resolved
                        } else {
                            // Try to find the new meal ID from our mappings
                            newMealId = idMappingManager.getNewId("meals", oldMealId.toString())
                                ?: return@forEach // Skip if parent meal doesn't exist
                            
                            val groupKey = backupMealKeys[oldMealId]?.let { ImportMergeKeys.mealCheckInKey(it, dateTime) }
                            if (mode == ImportMode.MERGE && groupKey != null && newMealId !in insertedMealIds && groupKey in existingKeys) {
                                skipped++
                                return@forEach
                            }
                        }
                        
                        val checkIn = MealCheckIn(
                            id = 0, // Let Room auto-generate
                            mealId = newMealId,
                            checkInDate = row.getValueOrEmpty("checkInDate"),
                            checkInDateTime = dateTime,
                            servingSize = row.getDoubleValue("servingSize") ?: 1.0,
                            notes = notes
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
    
    private suspend fun existingMealKeysById(): Map<Long, ImportMergeKeys.MealKey> =
        database.mealDao().getAllMealsIncludingHiddenSync().associate { m ->
            m.id to ImportMergeKeys.mealKey(m.name, m.brand, m.calories, m.servingSize_value, m.servingSize_unit.abbreviation, m.isVisible)
        }
    
    private suspend fun existingExerciseKeysById(): Map<Long, ImportMergeKeys.ExerciseKey> =
        database.exerciseDao().getAllExercisesIncludingHiddenSync().associate { e ->
            e.id to ImportMergeKeys.exerciseKey(e.name, e.category)
        }
    
    private suspend fun existingTagKeysById(): Map<Long, ImportMergeKeys.TagKey> =
        database.tagDao().getAllTagsSync().associate { t -> t.id to ImportMergeKeys.tagKey(t.name, t.type.name) }
    
    /** Hidden snapshot meal with the nutrition of a recipe; null if it cannot be built. */
    private suspend fun createLegacySnapshot(recipeId: Long): Long? {
        val repository = (context.applicationContext as? com.offlinelabs.nutcracker.FoodLogApplication)?.foodLogRepository
            ?: return null
        return repository.createRecipeSnapshotMeal(recipeId)
    }
    
    /** Hidden copy of an imported placeholder meal, so each check-in owns its meal row. */
    private suspend fun clonePlaceholder(placeholderMealId: Long?): Long? {
        if (placeholderMealId == null) return null
        val source = database.mealDao().getMealByIdSync(placeholderMealId) ?: return null
        return database.mealDao().insertMeal(source.copy(id = 0, isVisible = false))
    }
    
    private suspend fun importExercises(tsvData: String): TableImportResult {
        val rows = parseCsvData(tsvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            val existingIds = ImportMergeKeys.ExistingIdQueues(
                if (mode == ImportMode.MERGE) {
                    database.exerciseDao().getAllExercisesIncludingHiddenSync().map { e ->
                        ImportMergeKeys.exerciseKey(e.name, e.category) to e.id
                    }
                } else emptyList()
            )
            
            rows.forEach { row ->
                val validation = ImportValidators.validateExerciseData(row)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
                
                if (validation.isValid) {
                    try {
                        // Map old image paths to new extracted paths
                        val oldImagePaths = parseMuscleList(row.getValue("imagePaths"))
                        val newImagePaths = mapImagePaths(oldImagePaths, "exercises")
                        
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
                            level = row.getValue("level").takeIf { !it.isNullOrBlank() },
                            mechanic = row.getValue("mechanic").takeIf { !it.isNullOrBlank() },
                            instructions = parseMuscleList(row.getValue("instructions")),
                            notes = row.getValue("notes").takeIf { !it.isNullOrBlank() },
                            imagePaths = newImagePaths,
                            isVisible = row.getValue("isVisible")?.toBoolean() ?: true
                        )
                        
                        val oldId = row.getValue("id")?.toLongOrNull()
                        val exerciseKey = ImportMergeKeys.exerciseKey(exercise.name, exercise.category)
                        if (oldId != null) backupExerciseKeys[oldId] = exerciseKey
                        val matchedId = existingIds.take(exerciseKey)
                        if (matchedId != null) {
                            if (oldId != null) {
                                idMappings[oldId.toString()] = matchedId
                                idMappingManager.addMapping("exercises", oldId.toString(), matchedId)
                            }
                            skipped++
                            return@forEach
                        }
                        
                        val newId = database.exerciseDao().upsertExercise(exercise)
                        insertedExerciseIds.add(newId)
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
    
    private suspend fun importExerciseLogs(tsvData: String): TableImportResult {
        val rows = parseCsvData(tsvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            val existingKeys = HashSet<ImportMergeKeys.ExerciseLogKey>()
            if (mode == ImportMode.MERGE) {
                val exerciseKeysById = existingExerciseKeysById()
                database.exerciseLogDao().getAllExerciseLogsSync().forEach { l ->
                    exerciseKeysById[l.exerciseId]?.let { existingKeys.add(ImportMergeKeys.exerciseLogKey(it, l.logDateTime)) }
                }
            }
            
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
                        
                        val logGroupKey = backupExerciseKeys[oldExerciseId]
                            ?.let { ImportMergeKeys.exerciseLogKey(it, row.getValueOrEmpty("logDateTime")) }
                        if (logGroupKey != null && newExerciseId !in insertedExerciseIds && logGroupKey in existingKeys) {
                            skipped++
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
    
    private suspend fun importPills(tsvData: String): TableImportResult {
        val rows = parseCsvData(tsvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            var existingPills: List<Pair<String, Long>> = emptyList()
            if (mode == ImportMode.MERGE) {
                val pills = database.pillDao().getAllPillsSync()
                val checkInCount = database.pillCheckInDao().getAllPillCheckInsSync().size
                if (ImportMergeKeys.shouldRemoveSeededDefaultPill(pills.size, checkInCount, rows.size)) {
                    // Untouched auto-seeded default pill: the backup brings its own pills
                    database.pillDao().deletePill(pills.first())
                } else {
                    existingPills = pills.map { ImportMergeKeys.pillKey(it.name) to it.id }
                }
            }
            val existingIds = ImportMergeKeys.ExistingIdQueues(existingPills)
            
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
                        
                        val oldId = row.getValue("id")?.toLongOrNull()
                        if (oldId != null) backupPillNames[oldId] = pill.name
                        val matchedId = existingIds.take(ImportMergeKeys.pillKey(pill.name))
                        if (matchedId != null) {
                            if (oldId != null) {
                                idMappings[oldId.toString()] = matchedId
                                idMappingManager.addMapping("pills", oldId.toString(), matchedId)
                            }
                            skipped++
                            return@forEach
                        }
                        
                        val newId = database.pillDao().insertPill(pill)
                        insertedPillIds.add(newId)
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
    
    private suspend fun importPillCheckIns(tsvData: String): TableImportResult {
        val rows = parseCsvData(tsvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            val existingKeys = HashSet<ImportMergeKeys.PillCheckInKey>()
            if (mode == ImportMode.MERGE) {
                val pillNamesById = database.pillDao().getAllPillsSync().associate { it.id to it.name }
                database.pillCheckInDao().getAllPillCheckInsSync().forEach { c ->
                    pillNamesById[c.pillId]?.let { existingKeys.add(ImportMergeKeys.pillCheckInKey(it, c.timestamp.toString())) }
                }
            }
            
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
                        
                        val pillGroupKey = backupPillNames[oldPillId]
                            ?.let { ImportMergeKeys.pillCheckInKey(it, timestamp.toString()) }
                        if (pillGroupKey != null && newPillId !in insertedPillIds && pillGroupKey in existingKeys) {
                            skipped++
                            return@forEach
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
     * Import tags table from TSV
     */
    private suspend fun importTags(tsvData: String): TableImportResult {
        val rows = parseCsvData(tsvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        val idMappings = mutableMapOf<String, Long>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            val existingIds = ImportMergeKeys.ExistingIdQueues(
                if (mode == ImportMode.MERGE) {
                    database.tagDao().getAllTagsSync().map { t -> ImportMergeKeys.tagKey(t.name, t.type.name) to t.id }
                } else emptyList()
            )
            
            rows.forEach { row ->
                try {
                    val tag = Tag(
                        id = 0, // Let Room auto-generate
                        name = row.getValueOrEmpty("name"),
                        color = row.getValueOrEmpty("color"),
                        type = TagType.valueOf(row.getValueOrEmpty("type"))
                    )
                    
                    val oldId = row.getValue("id")?.toLongOrNull()
                    val tagKey = ImportMergeKeys.tagKey(tag.name, tag.type.name)
                    if (oldId != null) backupTagKeys[oldId] = tagKey
                    val matchedId = existingIds.take(tagKey)
                    if (matchedId != null) {
                        if (oldId != null) {
                            idMappings[oldId.toString()] = matchedId
                            idMappingManager.addMapping("tags", oldId.toString(), matchedId)
                        }
                        skipped++
                        return@forEach
                    }
                    
                    val newId = database.tagDao().insertTag(tag)
                    insertedTagIds.add(newId)
                    if (oldId != null) {
                        idMappings[oldId.toString()] = newId
                        idMappingManager.addMapping("tags", oldId.toString(), newId)
                    }
                    imported++
                } catch (e: Exception) {
                    errors.add(ImportError(
                        tableName = "tags",
                        rowNumber = row.rowNumber,
                        fieldName = null,
                        errorMessage = "Failed to insert tag: ${e.message}",
                        severity = ErrorSeverity.ERROR
                    ))
                    failed++
                }
            }
            
            TableImportResult(
                tableName = "tags",
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
     * Import meal tags junction table from TSV
     */
    private suspend fun importMealTags(tsvData: String): TableImportResult {
        val rows = parseCsvData(tsvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            val existingPairs = HashSet<ImportMergeKeys.MealTagKey>()
            if (mode == ImportMode.MERGE) {
                val mealKeysById = existingMealKeysById()
                val tagKeysById = existingTagKeysById()
                database.mealTagDao().getAllMealTagsSync().forEach { mt ->
                    val mk = mealKeysById[mt.mealId]
                    val tk = tagKeysById[mt.tagId]
                    if (mk != null && tk != null) existingPairs.add(ImportMergeKeys.mealTagKey(mk, tk))
                }
            }
            
            rows.forEach { row ->
                try {
                    val oldMealId = row.getLongValue("mealId")
                    val oldTagId = row.getLongValue("tagId")
                    
                    val newMealId = if (oldMealId != null) {
                        idMappingManager.getNewId("meals", oldMealId.toString())
                    } else null
                    
                    val newTagId = if (oldTagId != null) {
                        idMappingManager.getNewId("tags", oldTagId.toString())
                    } else null
                    
                    val mealTagGroup = backupMealKeys[oldMealId]?.let { mk ->
                        backupTagKeys[oldTagId]?.let { tk -> ImportMergeKeys.mealTagKey(mk, tk) }
                    }
                    if (newMealId != null && newTagId != null && mealTagGroup != null &&
                        newMealId !in insertedMealIds && newTagId !in insertedTagIds && mealTagGroup in existingPairs) {
                        skipped++
                    } else if (newMealId != null && newTagId != null) {
                        val mealTag = MealTag(
                            id = 0, // Let Room auto-generate
                            mealId = newMealId,
                            tagId = newTagId
                        )
                        
                        database.mealTagDao().insertMealTag(mealTag)
                        imported++
                    } else {
                        skipped++
                    }
                } catch (e: Exception) {
                    errors.add(ImportError(
                        tableName = "meal_tags",
                        rowNumber = row.rowNumber,
                        fieldName = null,
                        errorMessage = "Failed to insert meal tag: ${e.message}",
                        severity = ErrorSeverity.ERROR
                    ))
                    failed++
                }
            }
            
            TableImportResult(
                tableName = "meal_tags",
                recordsProcessed = rows.size,
                recordsImported = imported,
                recordsSkipped = skipped,
                recordsFailed = failed,
                errors = errors,
                warnings = warnings
            )
        }
    }
    
    /**
     * Import exercise tags junction table from TSV
     */
    private suspend fun importExerciseTags(tsvData: String): TableImportResult {
        val rows = parseCsvData(tsvData)
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        var imported = 0
        var skipped = 0
        var failed = 0
        
        return database.withTransaction {
            val existingPairs = HashSet<ImportMergeKeys.ExerciseTagKey>()
            if (mode == ImportMode.MERGE) {
                val exerciseKeysById = existingExerciseKeysById()
                val tagKeysById = existingTagKeysById()
                database.exerciseTagDao().getAllExerciseTagsSync().forEach { et ->
                    val ek = exerciseKeysById[et.exerciseId]
                    val tk = tagKeysById[et.tagId]
                    if (ek != null && tk != null) existingPairs.add(ImportMergeKeys.exerciseTagKey(ek, tk))
                }
            }
            
            rows.forEach { row ->
                try {
                    val oldExerciseId = row.getLongValue("exerciseId")
                    val oldTagId = row.getLongValue("tagId")
                    
                    val newExerciseId = if (oldExerciseId != null) {
                        idMappingManager.getNewId("exercises", oldExerciseId.toString())
                    } else null
                    
                    val newTagId = if (oldTagId != null) {
                        idMappingManager.getNewId("tags", oldTagId.toString())
                    } else null
                    
                    val exerciseTagGroup = backupExerciseKeys[oldExerciseId]?.let { ek ->
                        backupTagKeys[oldTagId]?.let { tk -> ImportMergeKeys.exerciseTagKey(ek, tk) }
                    }
                    if (newExerciseId != null && newTagId != null && exerciseTagGroup != null &&
                        newExerciseId !in insertedExerciseIds && newTagId !in insertedTagIds && exerciseTagGroup in existingPairs) {
                        skipped++
                    } else if (newExerciseId != null && newTagId != null) {
                        val exerciseTag = ExerciseTag(
                            id = 0, // Let Room auto-generate
                            exerciseId = newExerciseId,
                            tagId = newTagId
                        )
                        
                        database.exerciseTagDao().insertExerciseTag(exerciseTag)
                        imported++
                    } else {
                        skipped++
                    }
                } catch (e: Exception) {
                    errors.add(ImportError(
                        tableName = "exercise_tags",
                        rowNumber = row.rowNumber,
                        fieldName = null,
                        errorMessage = "Failed to insert exercise tag: ${e.message}",
                        severity = ErrorSeverity.ERROR
                    ))
                    failed++
                }
            }
            
            TableImportResult(
                tableName = "exercise_tags",
                recordsProcessed = rows.size,
                recordsImported = imported,
                recordsSkipped = skipped,
                recordsFailed = failed,
                errors = errors,
                warnings = warnings
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
    
    /**
     * Map old image path to new extracted image path
     * @param oldPath The old local path from the export
     * @param imageType "meals" or "exercises"
     * @return The new local path, or null if image wasn't found
     */
    private fun mapImagePath(oldPath: String?, imageType: String): String? {
        if (oldPath.isNullOrBlank()) return null
        
        // Extract just the filename from the old path
        val fileName = File(oldPath).name
        
        // Construct the ZIP path
        val zipPath = "images/$imageType/$fileName"
        
        // Look up the new local path
        return imagePathMappings[zipPath]
    }
    
    /**
     * Map list of old image paths to new extracted paths
     */
    private fun mapImagePaths(oldPaths: List<String>, imageType: String): List<String> {
        return oldPaths.mapNotNull { mapImagePath(it, imageType) }
    }
    
    /**
     * Parse NovaClassification from group number string
     */
    private fun parseNovaClassification(groupString: String): NovaClassification? {
        return try {
            val group = groupString.toIntOrNull() ?: return null
            NovaClassification.fromGroup(group)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse GreenScore from grade string
     */
    private fun parseGreenScore(gradeString: String): GreenScore? {
        return if (gradeString.isNotBlank()) {
            GreenScore(grade = gradeString, score = null)
        } else {
            null
        }
    }
    
    /**
     * Parse Nutriscore from grade string
     */
    private fun parseNutriscore(gradeString: String): Nutriscore? {
        return if (gradeString.isNotBlank()) {
            Nutriscore(grade = gradeString, score = null)
        } else {
            null
        }
    }
}
