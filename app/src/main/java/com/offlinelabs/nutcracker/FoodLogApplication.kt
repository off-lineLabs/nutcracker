package com.offlinelabs.nutcracker

import android.app.Application
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.SettingsManager
import com.offlinelabs.nutcracker.data.repo.FoodLogRepository
import com.offlinelabs.nutcracker.data.repo.OfflineFoodLogRepository
import com.offlinelabs.nutcracker.data.service.ExternalExerciseService
import com.offlinelabs.nutcracker.data.service.ExerciseImageService
import com.offlinelabs.nutcracker.data.service.ImageDownloadService
import com.offlinelabs.nutcracker.data.service.OpenFoodFactsService
import com.offlinelabs.nutcracker.data.service.OpenFoodFactsApi
import com.offlinelabs.nutcracker.util.logger.AppLogger
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FoodLogApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    
    val exerciseImageService: ExerciseImageService by lazy { ExerciseImageService(this) }
    val imageDownloadService: ImageDownloadService by lazy { ImageDownloadService(this) }
    
    val foodLogRepository: FoodLogRepository by lazy {
        OfflineFoodLogRepository(
            database.mealDao(), 
            database.userGoalDao(), 
            database.mealCheckInDao(),
            database.exerciseDao(),
            database.exerciseLogDao(),
            database.pillDao(),
            database.pillCheckInDao(),
            exerciseImageService,
            imageDownloadService
        )
    }
    
    val externalExerciseService: ExternalExerciseService by lazy { ExternalExerciseService() }
    
    val openFoodFactsService: OpenFoodFactsService by lazy {
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "OfflineCalorieCalculator/1.0 (Android; ${android.os.Build.MODEL})")
                    .build()
                chain.proceed(request)
            }
            .build()
            
        val retrofit = Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(OpenFoodFactsApi::class.java)
        OpenFoodFactsService(api)
    }
    
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
