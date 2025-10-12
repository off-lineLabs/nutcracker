package com.offlinelabs.nutcracker.data.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Modern database export manager using Room database and DocumentFile API
 * Exports all tables to CSV format in a ZIP file
 */
class DatabaseExportManager(
    private val context: Context,
    private val database: AppDatabase
) {
    
    companion object {
        private const val EXPORT_FILENAME_PREFIX = "nutcracker_export"
        private const val EXPORT_FILE_EXTENSION = ".zip"
        private const val TSV_EXTENSION = ".tsv"
        
        // Table names for TSV files
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
        private const val EXPORT_INFO_TSV = "export_info.tsv"
    }
    
    /**
     * Export all database tables to CSV files in a ZIP archive
     * @param destinationUri The URI where the ZIP file should be saved
     * @return Result indicating success or failure with error message
     */
    suspend fun exportDatabase(destinationUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val documentFile = DocumentFile.fromSingleUri(context, destinationUri)
                ?: return@withContext Result.failure(Exception("Invalid destination URI"))
            
            // Create ZIP output stream
            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext Result.failure(Exception("Cannot create output stream"))
            
            ZipOutputStream(outputStream).use { zipOut ->
                // Export each table to TSV
                exportTableToTsv(zipOut, MEALS_TSV) { exportMeals() }
                exportTableToTsv(zipOut, USER_GOALS_TSV) { exportUserGoals() }
                exportTableToTsv(zipOut, MEAL_CHECK_INS_TSV) { exportMealCheckIns() }
                exportTableToTsv(zipOut, EXERCISES_TSV) { exportExercises() }
                exportTableToTsv(zipOut, EXERCISE_LOGS_TSV) { exportExerciseLogs() }
                exportTableToTsv(zipOut, PILLS_TSV) { exportPills() }
                exportTableToTsv(zipOut, PILL_CHECK_INS_TSV) { exportPillCheckIns() }
                exportTableToTsv(zipOut, TAGS_TSV) { exportTags() }
                exportTableToTsv(zipOut, MEAL_TAGS_TSV) { exportMealTags() }
                exportTableToTsv(zipOut, EXERCISE_TAGS_TSV) { exportExerciseTags() }
                exportTableToTsv(zipOut, EXPORT_INFO_TSV) { exportMetadata() }
                
                // Export images
                exportImages(zipOut)
            }
            
            val fileName = documentFile.name ?: "nutcracker_export.zip"
            Result.success("Database exported successfully to $fileName")
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate a filename with timestamp for the export
     */
    fun generateExportFilename(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        return "${EXPORT_FILENAME_PREFIX}_$timestamp$EXPORT_FILE_EXTENSION"
    }
    
    /**
     * Export a table to TSV format within the ZIP file
     */
    private suspend fun exportTableToTsv(
        zipOut: ZipOutputStream,
        fileName: String,
        dataProvider: suspend () -> String
    ) {
        val entry = ZipEntry(fileName)
        zipOut.putNextEntry(entry)
        
        val tsvData = dataProvider()
        zipOut.write(tsvData.toByteArray(Charsets.UTF_8))
        
        zipOut.closeEntry()
    }
    
    /**
     * Export meals table to CSV
     */
    private suspend fun exportMeals(): String = withContext(Dispatchers.IO) {
        val meals = database.mealDao().getAllMeals()
        val mealsList = meals.first() // Get the current value from Flow
        
        val tsv = StringBuilder()
        tsv.appendLine("id\tname\tbrand\tcalories\tcarbohydrates_g\tprotein_g\tfat_g\tfiber_g\tsodium_mg\tservingSize_value\tservingSize_unit\tnotes\tisVisible\tsaturatedFat_g\tsugars_g\tcholesterol_mg\tvitaminC_mg\tcalcium_mg\tiron_mg\timageUrl\tlocalImagePath\tnovaClassification\tgreenScore\tnutriscore\tingredients\tquantity\tservingSize\tbarcode\tsource")
        
        mealsList.forEach { meal: Meal ->
            tsv.appendLine("${meal.id}\t${escapeTsv(meal.name)}\t${escapeTsv(meal.brand ?: "")}\t${meal.calories}\t${meal.carbohydrates_g}\t${meal.protein_g}\t${meal.fat_g}\t${meal.fiber_g}\t${meal.sodium_mg}\t${meal.servingSize_value}\t${meal.servingSize_unit.abbreviation}\t${escapeTsv(meal.notes ?: "")}\t${meal.isVisible}\t${meal.saturatedFat_g ?: ""}\t${meal.sugars_g ?: ""}\t${meal.cholesterol_mg ?: ""}\t${meal.vitaminC_mg ?: ""}\t${meal.calcium_mg ?: ""}\t${meal.iron_mg ?: ""}\t${escapeTsv(meal.imageUrl ?: "")}\t${escapeTsv(meal.localImagePath ?: "")}\t${meal.novaClassification?.group ?: ""}\t${meal.greenScore?.grade ?: ""}\t${meal.nutriscore?.grade ?: ""}\t${escapeTsv(meal.ingredients ?: "")}\t${escapeTsv(meal.quantity ?: "")}\t${escapeTsv(meal.servingSize ?: "")}\t${escapeTsv(meal.barcode ?: "")}\t${meal.source}")
        }
        
        tsv.toString()
    }
    
    /**
     * Export user goals table to TSV
     */
    private suspend fun exportUserGoals(): String = withContext(Dispatchers.IO) {
        val goals = database.userGoalDao().getUserGoal()
        val goal = goals.first() // Get the current value from Flow
        
        val tsv = StringBuilder()
        tsv.appendLine("id\tcaloriesGoal\tcarbsGoal_g\tproteinGoal_g\tfatGoal_g\tfiberGoal_g\tsodiumGoal_mg")
        
        goal?.let { userGoal: UserGoal ->
            tsv.appendLine("${userGoal.id}\t${userGoal.caloriesGoal}\t${userGoal.carbsGoal_g}\t${userGoal.proteinGoal_g}\t${userGoal.fatGoal_g}\t${userGoal.fiberGoal_g}\t${userGoal.sodiumGoal_mg}")
        }
        
        tsv.toString()
    }
    
    /**
     * Export meal check-ins table to TSV
     */
    private suspend fun exportMealCheckIns(): String = withContext(Dispatchers.IO) {
        val checkIns = database.mealCheckInDao().getAllMealCheckIns()
        val checkInsList = checkIns.first() // Get the current value from Flow
        
        val tsv = StringBuilder()
        tsv.appendLine("id\tmealId\tcheckInDate\tcheckInDateTime\tservingSize\tnotes")
        
        checkInsList.forEach { checkIn: MealCheckIn ->
            tsv.appendLine("${checkIn.id}\t${checkIn.mealId}\t${checkIn.checkInDate}\t${checkIn.checkInDateTime}\t${checkIn.servingSize}\t${escapeTsv(checkIn.notes ?: "")}")
        }
        
        tsv.toString()
    }
    
    /**
     * Export exercises table to CSV
     */
    private suspend fun exportExercises(): String = withContext(Dispatchers.IO) {
        val exercises = database.exerciseDao().getAllExercises()
        val exercisesList = exercises.first() // Get the current value from Flow
        
        val tsv = StringBuilder()
        tsv.appendLine("id\tname\tkcalBurnedPerUnit\tdefaultWeight\tdefaultReps\tdefaultSets\tcategory\tequipment\tprimaryMuscles\tsecondaryMuscles\tforce\tlevel\tmechanic\tinstructions\tnotes\timagePaths\tisVisible")
        
        exercisesList.forEach { exercise: Exercise ->
            tsv.appendLine("${exercise.id}\t${escapeTsv(exercise.name)}\t${exercise.kcalBurnedPerUnit}\t${exercise.defaultWeight}\t${exercise.defaultReps}\t${exercise.defaultSets}\t${exercise.category}\t${escapeTsv(exercise.equipment ?: "")}\t${exercise.primaryMuscles.joinToString(";")}\t${exercise.secondaryMuscles.joinToString(";")}\t${escapeTsv(exercise.force ?: "")}\t${escapeTsv(exercise.level ?: "")}\t${escapeTsv(exercise.mechanic ?: "")}\t${exercise.instructions.joinToString(";")}\t${escapeTsv(exercise.notes ?: "")}\t${exercise.imagePaths.joinToString(";")}\t${exercise.isVisible}")
        }
        
        tsv.toString()
    }
    
    /**
     * Export exercise logs table to TSV
     */
    private suspend fun exportExerciseLogs(): String = withContext(Dispatchers.IO) {
        val logs = database.exerciseLogDao().getAllExerciseLogs()
        val logsList = logs.first() // Get the current value from Flow
        
        val tsv = StringBuilder()
        tsv.appendLine("id\texerciseId\tlogDate\tlogDateTime\tweight\treps\tsets\tcaloriesBurned\tnotes")
        
        logsList.forEach { log: ExerciseLog ->
            tsv.appendLine("${log.id}\t${log.exerciseId}\t${log.logDate}\t${log.logDateTime}\t${log.weight}\t${log.reps}\t${log.sets}\t${log.caloriesBurned}\t${escapeTsv(log.notes ?: "")}")
        }
        
        tsv.toString()
    }
    
    /**
     * Export pills table to TSV
     */
    private suspend fun exportPills(): String = withContext(Dispatchers.IO) {
        val pills = database.pillDao().getAllPills()
        val pillsList = pills.first() // Get the current value from Flow
        
        val tsv = StringBuilder()
        tsv.appendLine("id\tname")
        
        pillsList.forEach { pill: Pill ->
            tsv.appendLine("${pill.id}\t${escapeTsv(pill.name)}")
        }
        
        tsv.toString()
    }
    
    /**
     * Export pill check-ins table to TSV
     */
    private suspend fun exportPillCheckIns(): String = withContext(Dispatchers.IO) {
        val checkIns = database.pillCheckInDao().getAllPillCheckIns()
        val checkInsList = checkIns.first() // Get the current value from Flow
        
        val tsv = StringBuilder()
        tsv.appendLine("id\tpillId\ttimestamp")
        
        checkInsList.forEach { checkIn: PillCheckIn ->
            tsv.appendLine("${checkIn.id}\t${checkIn.pillId}\t${checkIn.timestamp}")
        }
        
        tsv.toString()
    }
    
    /**
     * Export tags table to TSV
     */
    private suspend fun exportTags(): String = withContext(Dispatchers.IO) {
        val tags = database.tagDao().getAllTags()
        val tagsList = tags.first() // Get the current value from Flow
        
        val tsv = StringBuilder()
        tsv.appendLine("id\tname\tcolor\ttype")
        
        tagsList.forEach { tag: Tag ->
            tsv.appendLine("${tag.id}\t${escapeTsv(tag.name)}\t${tag.color}\t${tag.type.name}")
        }
        
        tsv.toString()
    }
    
    /**
     * Export meal tags junction table to TSV
     */
    private suspend fun exportMealTags(): String = withContext(Dispatchers.IO) {
        val mealTags = database.mealTagDao().getAllMealTags()
        val mealTagsList = mealTags.first() // Get the current value from Flow
        
        val tsv = StringBuilder()
        tsv.appendLine("id\tmealId\ttagId")
        
        mealTagsList.forEach { mealTag: MealTag ->
            tsv.appendLine("${mealTag.id}\t${mealTag.mealId}\t${mealTag.tagId}")
        }
        
        tsv.toString()
    }
    
    /**
     * Export exercise tags junction table to TSV
     */
    private suspend fun exportExerciseTags(): String = withContext(Dispatchers.IO) {
        val exerciseTags = database.exerciseTagDao().getAllExerciseTags()
        val exerciseTagsList = exerciseTags.first() // Get the current value from Flow
        
        val tsv = StringBuilder()
        tsv.appendLine("id\texerciseId\ttagId")
        
        exerciseTagsList.forEach { exerciseTag: ExerciseTag ->
            tsv.appendLine("${exerciseTag.id}\t${exerciseTag.exerciseId}\t${exerciseTag.tagId}")
        }
        
        tsv.toString()
    }
    
    /**
     * Export images to ZIP file
     */
    private suspend fun exportImages(zipOut: ZipOutputStream) = withContext(Dispatchers.IO) {
        try {
            // Get all meals with images
            val meals = database.mealDao().getAllMeals().first()
            val exercises = database.exerciseDao().getAllExercises().first()
            
            // Export meal images
            meals.forEach { meal ->
                meal.localImagePath?.let { imagePath ->
                    val file = java.io.File(imagePath)
                    if (file.exists()) {
                        val entry = ZipEntry("images/meals/${file.name}")
                        zipOut.putNextEntry(entry)
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                }
            }
            
            // Export exercise images
            exercises.forEach { exercise ->
                exercise.imagePaths.forEach { imagePath ->
                    val file = java.io.File(imagePath)
                    if (file.exists()) {
                        val entry = ZipEntry("images/exercises/${file.name}")
                        zipOut.putNextEntry(entry)
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't fail the entire export
            com.offlinelabs.nutcracker.util.logger.AppLogger.e("DatabaseExportManager", "Failed to export images", e)
        }
    }
    
    /**
     * Export metadata about the export
     */
    private suspend fun exportMetadata(): String {
        val tsv = StringBuilder()
        tsv.appendLine("export_type\texport_date\tapp_version\tdatabase_version\ttable_count")
        tsv.appendLine("full_export\t${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())}\t1.0\t18\t10")
        tsv.appendLine("")
        tsv.appendLine("table_name\tdescription")
        tsv.appendLine("meals\tFood items with nutritional information")
        tsv.appendLine("user_goals\tDaily nutritional goals")
        tsv.appendLine("meal_check_ins\tMeal consumption records")
        tsv.appendLine("exercises\tExercise definitions")
        tsv.appendLine("exercise_logs\tExercise performance records")
        tsv.appendLine("pills\tMedication definitions")
        tsv.appendLine("pill_check_ins\tMedication consumption records")
        tsv.appendLine("tags\tTag definitions for meals and exercises")
        tsv.appendLine("meal_tags\tMeal-tag relationships")
        tsv.appendLine("exercise_tags\tExercise-tag relationships")
        
        return tsv.toString()
    }
    
    /**
     * Escape TSV values that contain tabs, newlines, or backslashes
     */
    private fun escapeTsv(value: String): String {
        return value
            .replace("\\", "\\\\")  // Escape backslashes first
            .replace("\t", "\\t")   // Escape tabs
            .replace("\n", "\\n")   // Escape newlines
            .replace("\r", "\\r")   // Escape carriage returns
    }
    
    /**
     * Escape CSV values that contain commas, quotes, or newlines
     */
    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
