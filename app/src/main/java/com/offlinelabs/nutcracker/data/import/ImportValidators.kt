package com.offlinelabs.nutcracker.data.import

import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import java.text.SimpleDateFormat
import java.util.*

/**
 * Validation utilities for import operations
 */
object ImportValidators {
    
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    private val DATETIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
    
    /**
     * Validate meal data from TSV row
     */
    fun validateMealData(row: CsvRow): ValidationResult {
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        
        // Required fields
        if (row.getValueOrEmpty("name").isBlank()) {
            errors.add(ImportError(
                tableName = "meals",
                rowNumber = row.rowNumber,
                fieldName = "name",
                errorMessage = "Meal name is required",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        // Numeric validations
        val calories = row.getIntValue("calories")
        if (calories == null || calories < 0) {
            errors.add(ImportError(
                tableName = "meals",
                rowNumber = row.rowNumber,
                fieldName = "calories",
                errorMessage = "Calories must be a non-negative integer",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        val carbs = row.getDoubleValue("carbohydrates_g")
        if (carbs == null || carbs < 0) {
            errors.add(ImportError(
                tableName = "meals",
                rowNumber = row.rowNumber,
                fieldName = "carbohydrates_g",
                errorMessage = "Carbohydrates must be a non-negative number",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        val protein = row.getDoubleValue("protein_g")
        if (protein == null || protein < 0) {
            errors.add(ImportError(
                tableName = "meals",
                rowNumber = row.rowNumber,
                fieldName = "protein_g",
                errorMessage = "Protein must be a non-negative number",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        val fat = row.getDoubleValue("fat_g")
        if (fat == null || fat < 0) {
            errors.add(ImportError(
                tableName = "meals",
                rowNumber = row.rowNumber,
                fieldName = "fat_g",
                errorMessage = "Fat must be a non-negative number",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        val fiber = row.getDoubleValue("fiber_g")
        if (fiber == null || fiber < 0) {
            errors.add(ImportError(
                tableName = "meals",
                rowNumber = row.rowNumber,
                fieldName = "fiber_g",
                errorMessage = "Fiber must be a non-negative number",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        val sodium = row.getDoubleValue("sodium_mg")
        if (sodium == null || sodium < 0) {
            errors.add(ImportError(
                tableName = "meals",
                rowNumber = row.rowNumber,
                fieldName = "sodium_mg",
                errorMessage = "Sodium must be a non-negative number",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        val servingSizeValue = row.getDoubleValue("servingSize_value")
        if (servingSizeValue == null || servingSizeValue <= 0) {
            errors.add(ImportError(
                tableName = "meals",
                rowNumber = row.rowNumber,
                fieldName = "servingSize_value",
                errorMessage = "Serving size value must be a positive number",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        // Serving size unit validation
        val servingSizeUnit = row.getValue("servingSize_unit")
        if (servingSizeUnit != null) {
            val unit = ServingSizeUnit.fromAbbreviation(servingSizeUnit)
            if (unit == null) {
                errors.add(ImportError(
                    tableName = "meals",
                    rowNumber = row.rowNumber,
                    fieldName = "servingSize_unit",
                    errorMessage = "Invalid serving size unit: $servingSizeUnit",
                    severity = ErrorSeverity.ERROR
                ))
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validate user goal data from TSV row
     */
    fun validateUserGoalData(row: CsvRow): ValidationResult {
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        
        // All fields are required and must be non-negative integers
        val fields = listOf(
            "caloriesGoal" to "Calories goal",
            "carbsGoal_g" to "Carbs goal",
            "proteinGoal_g" to "Protein goal",
            "fatGoal_g" to "Fat goal",
            "fiberGoal_g" to "Fiber goal",
            "sodiumGoal_mg" to "Sodium goal"
        )
        
        fields.forEach { (fieldName, displayName) ->
            val value = row.getIntValue(fieldName)
            if (value == null || value < 0) {
                errors.add(ImportError(
                    tableName = "user_goals",
                    rowNumber = row.rowNumber,
                    fieldName = fieldName,
                    errorMessage = "$displayName must be a non-negative integer",
                    severity = ErrorSeverity.ERROR
                ))
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validate meal check-in data from TSV row
     */
    fun validateMealCheckInData(row: CsvRow): ValidationResult {
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        
        // Meal ID validation
        val mealId = row.getLongValue("mealId")
        if (mealId == null || mealId <= 0) {
            errors.add(ImportError(
                tableName = "meal_check_ins",
                rowNumber = row.rowNumber,
                fieldName = "mealId",
                errorMessage = "Meal ID must be a positive integer",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        // Date validation
        val checkInDate = row.getValue("checkInDate")
        if (checkInDate != null) {
            try {
                DATE_FORMAT.parse(checkInDate)
            } catch (e: Exception) {
                errors.add(ImportError(
                    tableName = "meal_check_ins",
                    rowNumber = row.rowNumber,
                    fieldName = "checkInDate",
                    errorMessage = "Invalid date format. Expected: yyyy-MM-dd",
                    severity = ErrorSeverity.ERROR
                ))
            }
        }
        
        // DateTime validation
        val checkInDateTime = row.getValue("checkInDateTime")
        if (checkInDateTime != null) {
            try {
                DATETIME_FORMAT.parse(checkInDateTime)
            } catch (e: Exception) {
                errors.add(ImportError(
                    tableName = "meal_check_ins",
                    rowNumber = row.rowNumber,
                    fieldName = "checkInDateTime",
                    errorMessage = "Invalid datetime format. Expected: yyyy-MM-dd HH:mm:ss",
                    severity = ErrorSeverity.ERROR
                ))
            }
        }
        
        // Serving size validation
        val servingSize = row.getDoubleValue("servingSize")
        if (servingSize == null || servingSize <= 0) {
            errors.add(ImportError(
                tableName = "meal_check_ins",
                rowNumber = row.rowNumber,
                fieldName = "servingSize",
                errorMessage = "Serving size must be a positive number",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validate exercise data from TSV row
     */
    fun validateExerciseData(row: CsvRow): ValidationResult {
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        
        // Required fields
        if (row.getValueOrEmpty("name").isBlank()) {
            errors.add(ImportError(
                tableName = "exercises",
                rowNumber = row.rowNumber,
                fieldName = "name",
                errorMessage = "Exercise name is required",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        // Numeric validations
        val kcalPerUnit = row.getDoubleValue("kcalBurnedPerUnit")
        
        if (kcalPerUnit != null && kcalPerUnit < 0) {
            errors.add(ImportError(
                tableName = "exercises",
                rowNumber = row.rowNumber,
                fieldName = "kcalBurnedPerUnit",
                errorMessage = "Kcal per unit must be non-negative",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        val defaultWeight = row.getDoubleValue("defaultWeight")
        if (defaultWeight != null && defaultWeight < 0) {
            errors.add(ImportError(
                tableName = "exercises",
                rowNumber = row.rowNumber,
                fieldName = "defaultWeight",
                errorMessage = "Default weight must be non-negative",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        val defaultReps = row.getIntValue("defaultReps")
        if (defaultReps != null && defaultReps < 0) {
            errors.add(ImportError(
                tableName = "exercises",
                rowNumber = row.rowNumber,
                fieldName = "defaultReps",
                errorMessage = "Default reps must be non-negative",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        val defaultSets = row.getIntValue("defaultSets")
        if (defaultSets != null && defaultSets < 0) {
            errors.add(ImportError(
                tableName = "exercises",
                rowNumber = row.rowNumber,
                fieldName = "defaultSets",
                errorMessage = "Default sets must be non-negative",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validate exercise log data from TSV row
     */
    fun validateExerciseLogData(row: CsvRow): ValidationResult {
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        
        // Exercise ID validation - only validate if present
        val exerciseId = row.getLongValue("exerciseId")
        if (exerciseId != null && exerciseId <= 0) {
            errors.add(ImportError(
                tableName = "exercise_logs",
                rowNumber = row.rowNumber,
                fieldName = "exerciseId",
                errorMessage = "Exercise ID must be a positive integer",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        // Date validation
        val logDate = row.getValue("logDate")
        if (logDate != null) {
            try {
                DATE_FORMAT.parse(logDate)
            } catch (e: Exception) {
                errors.add(ImportError(
                    tableName = "exercise_logs",
                    rowNumber = row.rowNumber,
                    fieldName = "logDate",
                    errorMessage = "Invalid date format. Expected: yyyy-MM-dd",
                    severity = ErrorSeverity.ERROR
                ))
            }
        }
        
        // DateTime validation
        val logDateTime = row.getValue("logDateTime")
        if (logDateTime != null) {
            try {
                DATETIME_FORMAT.parse(logDateTime)
            } catch (e: Exception) {
                errors.add(ImportError(
                    tableName = "exercise_logs",
                    rowNumber = row.rowNumber,
                    fieldName = "logDateTime",
                    errorMessage = "Invalid datetime format. Expected: yyyy-MM-dd HH:mm:ss",
                    severity = ErrorSeverity.ERROR
                ))
            }
        }
        
        // Numeric validations - allow empty values, but validate format if present
        val weight = row.getDoubleValue("weight")
        if (weight != null && weight < 0) {
            errors.add(ImportError(
                tableName = "exercise_logs",
                rowNumber = row.rowNumber,
                fieldName = "weight",
                errorMessage = "Weight must be a non-negative number",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        val reps = row.getIntValue("reps")
        if (reps != null && reps < 0) {
            errors.add(ImportError(
                tableName = "exercise_logs",
                rowNumber = row.rowNumber,
                fieldName = "reps",
                errorMessage = "Reps must be a non-negative integer",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        val sets = row.getIntValue("sets")
        if (sets != null && sets < 0) {
            errors.add(ImportError(
                tableName = "exercise_logs",
                rowNumber = row.rowNumber,
                fieldName = "sets",
                errorMessage = "Sets must be a non-negative integer",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        val caloriesBurned = row.getDoubleValue("caloriesBurned")
        if (caloriesBurned != null && caloriesBurned < 0) {
            errors.add(ImportError(
                tableName = "exercise_logs",
                rowNumber = row.rowNumber,
                fieldName = "caloriesBurned",
                errorMessage = "Calories burned must be a non-negative number",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validate pill data from TSV row
     */
    fun validatePillData(row: CsvRow): ValidationResult {
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        
        // Required fields
        if (row.getValueOrEmpty("name").isBlank()) {
            errors.add(ImportError(
                tableName = "pills",
                rowNumber = row.rowNumber,
                fieldName = "name",
                errorMessage = "Pill name is required",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validate pill check-in data from TSV row
     */
    fun validatePillCheckInData(row: CsvRow): ValidationResult {
        val errors = mutableListOf<ImportError>()
        val warnings = mutableListOf<ImportWarning>()
        
        // Pill ID validation
        val pillId = row.getLongValue("pillId")
        if (pillId == null || pillId <= 0) {
            errors.add(ImportError(
                tableName = "pill_check_ins",
                rowNumber = row.rowNumber,
                fieldName = "pillId",
                errorMessage = "Pill ID must be a positive integer",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        // Timestamp validation
        val timestamp = row.getValue("timestamp")
        if (timestamp != null) {
            try {
                // Try parsing as LocalDateTime format
                java.time.LocalDateTime.parse(timestamp)
            } catch (e: Exception) {
                errors.add(ImportError(
                    tableName = "pill_check_ins",
                    rowNumber = row.rowNumber,
                    fieldName = "timestamp",
                    errorMessage = "Invalid timestamp format",
                    severity = ErrorSeverity.ERROR
                ))
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}
