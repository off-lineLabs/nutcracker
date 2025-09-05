package com.example.template

import android.app.Application
import com.example.template.data.AppDatabase
import com.example.template.data.repo.FoodLogRepository
import com.example.template.data.repo.OfflineFoodLogRepository

class FoodLogApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    
    val foodLogRepository: FoodLogRepository by lazy {
        OfflineFoodLogRepository(
            database.mealDao(), 
            database.userGoalDao(), 
            database.mealCheckInDao(),
            database.exerciseDao(),
            database.exerciseLogDao(),
            database.pillDao(),
            database.pillCheckInDao()
        )
    }
}
