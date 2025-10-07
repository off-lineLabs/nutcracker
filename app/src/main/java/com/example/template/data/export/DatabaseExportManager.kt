package com.example.template.data.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.template.data.AppDatabase
import com.example.template.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
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
        private const val CSV_EXTENSION = ".csv"
        
        // Table names for CSV files
        private const val MEALS_CSV = "meals.csv"
        private const val USER_GOALS_CSV = "user_goals.csv"
        private const val MEAL_CHECK_INS_CSV = "meal_check_ins.csv"
        private const val EXERCISES_CSV = "exercises.csv"
        private const val EXERCISE_LOGS_CSV = "exercise_logs.csv"
        private const val PILLS_CSV = "pills.csv"
        private const val PILL_CHECK_INS_CSV = "pill_check_ins.csv"
        private const val EXPORT_INFO_CSV = "export_info.csv"
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
                // Export each table to CSV
                exportTableToCsv(zipOut, MEALS_CSV) { exportMeals() }
                exportTableToCsv(zipOut, USER_GOALS_CSV) { exportUserGoals() }
                exportTableToCsv(zipOut, MEAL_CHECK_INS_CSV) { exportMealCheckIns() }
                exportTableToCsv(zipOut, EXERCISES_CSV) { exportExercises() }
                exportTableToCsv(zipOut, EXERCISE_LOGS_CSV) { exportExerciseLogs() }
                exportTableToCsv(zipOut, PILLS_CSV) { exportPills() }
                exportTableToCsv(zipOut, PILL_CHECK_INS_CSV) { exportPillCheckIns() }
                exportTableToCsv(zipOut, EXPORT_INFO_CSV) { exportMetadata() }
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
     * Export a table to CSV format within the ZIP file
     */
    private suspend fun exportTableToCsv(
        zipOut: ZipOutputStream,
        fileName: String,
        dataProvider: suspend () -> String
    ) {
        val entry = ZipEntry(fileName)
        zipOut.putNextEntry(entry)
        
        val csvData = dataProvider()
        zipOut.write(csvData.toByteArray(Charsets.UTF_8))
        
        zipOut.closeEntry()
    }
    
    /**
     * Export meals table to CSV
     */
    private suspend fun exportMeals(): String = withContext(Dispatchers.IO) {
        val meals = database.mealDao().getAllMeals()
        val mealsList = meals.first() // Get the current value from Flow
        
        val csv = StringBuilder()
        csv.appendLine("id,name,calories,carbohydrates_g,protein_g,fat_g,fiber_g,sodium_mg,servingSize_value,servingSize_unit,notes")
        
        mealsList.forEach { meal: Meal ->
            csv.appendLine("${meal.id},${escapeCsv(meal.name)},${meal.calories},${meal.carbohydrates_g},${meal.protein_g},${meal.fat_g},${meal.fiber_g},${meal.sodium_mg},${meal.servingSize_value},${meal.servingSize_unit.abbreviation},${escapeCsv(meal.notes ?: "")}")
        }
        
        csv.toString()
    }
    
    /**
     * Export user goals table to CSV
     */
    private suspend fun exportUserGoals(): String = withContext(Dispatchers.IO) {
        val goals = database.userGoalDao().getUserGoal()
        val goal = goals.first() // Get the current value from Flow
        
        val csv = StringBuilder()
        csv.appendLine("id,caloriesGoal,carbsGoal_g,proteinGoal_g,fatGoal_g,fiberGoal_g,sodiumGoal_mg")
        
        goal?.let { userGoal: UserGoal ->
            csv.appendLine("${userGoal.id},${userGoal.caloriesGoal},${userGoal.carbsGoal_g},${userGoal.proteinGoal_g},${userGoal.fatGoal_g},${userGoal.fiberGoal_g},${userGoal.sodiumGoal_mg}")
        }
        
        csv.toString()
    }
    
    /**
     * Export meal check-ins table to CSV
     */
    private suspend fun exportMealCheckIns(): String = withContext(Dispatchers.IO) {
        val checkIns = database.mealCheckInDao().getAllMealCheckIns()
        val checkInsList = checkIns.first() // Get the current value from Flow
        
        val csv = StringBuilder()
        csv.appendLine("id,mealId,checkInDate,checkInDateTime,servingSize,notes")
        
        checkInsList.forEach { checkIn: MealCheckIn ->
            csv.appendLine("${checkIn.id},${checkIn.mealId},${checkIn.checkInDate},${checkIn.checkInDateTime},${checkIn.servingSize},${escapeCsv(checkIn.notes ?: "")}")
        }
        
        csv.toString()
    }
    
    /**
     * Export exercises table to CSV
     */
    private suspend fun exportExercises(): String = withContext(Dispatchers.IO) {
        val exercises = database.exerciseDao().getAllExercises()
        val exercisesList = exercises.first() // Get the current value from Flow
        
        val csv = StringBuilder()
        csv.appendLine("id,name,kcalBurnedPerUnit,defaultWeight,defaultReps,defaultSets,category,equipment,primaryMuscles,secondaryMuscles,force,notes")
        
        exercisesList.forEach { exercise: Exercise ->
            csv.appendLine("${exercise.id},${escapeCsv(exercise.name)},${exercise.kcalBurnedPerUnit},${exercise.defaultWeight},${exercise.defaultReps},${exercise.defaultSets},${exercise.category},${escapeCsv(exercise.equipment ?: "")},${exercise.primaryMuscles.joinToString(";")},${exercise.secondaryMuscles.joinToString(";")},${escapeCsv(exercise.force ?: "")},${escapeCsv(exercise.notes ?: "")}")
        }
        
        csv.toString()
    }
    
    /**
     * Export exercise logs table to CSV
     */
    private suspend fun exportExerciseLogs(): String = withContext(Dispatchers.IO) {
        val logs = database.exerciseLogDao().getAllExerciseLogs()
        val logsList = logs.first() // Get the current value from Flow
        
        val csv = StringBuilder()
        csv.appendLine("id,exerciseId,logDate,logDateTime,weight,reps,sets,caloriesBurned,notes")
        
        logsList.forEach { log: ExerciseLog ->
            csv.appendLine("${log.id},${log.exerciseId},${log.logDate},${log.logDateTime},${log.weight},${log.reps},${log.sets},${log.caloriesBurned},${escapeCsv(log.notes ?: "")}")
        }
        
        csv.toString()
    }
    
    /**
     * Export pills table to CSV
     */
    private suspend fun exportPills(): String = withContext(Dispatchers.IO) {
        val pills = database.pillDao().getAllPills()
        val pillsList = pills.first() // Get the current value from Flow
        
        val csv = StringBuilder()
        csv.appendLine("id,name")
        
        pillsList.forEach { pill: Pill ->
            csv.appendLine("${pill.id},${escapeCsv(pill.name)}")
        }
        
        csv.toString()
    }
    
    /**
     * Export pill check-ins table to CSV
     */
    private suspend fun exportPillCheckIns(): String = withContext(Dispatchers.IO) {
        val checkIns = database.pillCheckInDao().getAllPillCheckIns()
        val checkInsList = checkIns.first() // Get the current value from Flow
        
        val csv = StringBuilder()
        csv.appendLine("id,pillId,timestamp")
        
        checkInsList.forEach { checkIn: PillCheckIn ->
            csv.appendLine("${checkIn.id},${checkIn.pillId},${checkIn.timestamp}")
        }
        
        csv.toString()
    }
    
    /**
     * Export metadata about the export
     */
    private suspend fun exportMetadata(): String {
        val csv = StringBuilder()
        csv.appendLine("export_type,export_date,app_version,database_version,table_count")
        csv.appendLine("full_export,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())},1.0,7,7")
        csv.appendLine("")
        csv.appendLine("table_name,description")
        csv.appendLine("meals,Food items with nutritional information")
        csv.appendLine("user_goals,Daily nutritional goals")
        csv.appendLine("meal_check_ins,Meal consumption records")
        csv.appendLine("exercises,Exercise definitions")
        csv.appendLine("exercise_logs,Exercise performance records")
        csv.appendLine("pills,Medication definitions")
        csv.appendLine("pill_check_ins,Medication consumption records")
        
        return csv.toString()
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
