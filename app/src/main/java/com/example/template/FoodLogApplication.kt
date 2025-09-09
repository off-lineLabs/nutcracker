package com.example.template

import android.app.Application
import com.example.template.data.AppDatabase
import com.example.template.data.SettingsManager
import com.example.template.data.repo.FoodLogRepository
import com.example.template.data.repo.OfflineFoodLogRepository
import com.example.template.data.service.ExternalExerciseService
import com.example.template.data.service.ExerciseImageService
import com.example.template.util.logger.AppLogger

class FoodLogApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    
    val exerciseImageService: ExerciseImageService by lazy { ExerciseImageService(this) }
    
    val foodLogRepository: FoodLogRepository by lazy {
        OfflineFoodLogRepository(
            database.mealDao(), 
            database.userGoalDao(), 
            database.mealCheckInDao(),
            database.exerciseDao(),
            database.exerciseLogDao(),
            database.pillDao(),
            database.pillCheckInDao(),
            exerciseImageService
        )
    }
    
    val externalExerciseService: ExternalExerciseService by lazy { ExternalExerciseService() }
    
    val settingsManager: SettingsManager by lazy { SettingsManager(this) }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging framework
        AppLogger.initialize(this)
        AppLogger.i("FoodLogApplication", "Application started")
    }

    override fun onTerminate() {
        super.onTerminate()
        AppLogger.shutdown()
    }
}
