package com.offlinelabs.nutcracker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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
import com.offlinelabs.nutcracker.data.repair.LegacyRecipeRepair
import com.offlinelabs.nutcracker.data.repair.RoomLegacyRecipeRepairStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FoodLogApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            database.recipeDao(),
            database.recipeIngredientDao(),
            database.gymDao(),
            database.routineDao(),
            database.mealCategoryDao(),
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
                    .addHeader("User-Agent", "NutCracker/1.0 (online@offline-labs.com) Android; ${android.os.Build.MODEL}")
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
        
        // Create .nomedia files to hide app images from gallery
        createNoMediaFiles()

        repairLegacyRecipeCheckIns()
        
        // Log the current locale state for debugging
        val storedLocales = AppCompatDelegate.getApplicationLocales()
        AppLogger.d("FoodLogApplication", "AppCompatDelegate stored locales: ${storedLocales.toLanguageTags()}")
        AppLogger.d("FoodLogApplication", "System Locale.getDefault(): ${java.util.Locale.getDefault().toLanguageTag()}")
    }
    
    /**
     * One-time-per-start, idempotent repair of recipe check-ins that shared the stray meal id -1.
     * Runs off the main thread; failures are logged and never crash startup.
     */
    private fun repairLegacyRecipeCheckIns() {
        appScope.launch {
            try {
                val store = RoomLegacyRecipeRepairStore(database, foodLogRepository)
                val repaired = LegacyRecipeRepair(store).repairLegacyRecipeCheckIns()
                if (repaired > 0) {
                    AppLogger.i("FoodLogApplication", "Repaired $repaired legacy recipe check-ins")
                }
            } catch (e: Exception) {
                AppLogger.e("FoodLogApplication", "Legacy recipe check-in repair failed", e)
            }
        }
    }

    /**
     * Creates .nomedia files in image directories to prevent them from appearing in the gallery.
     * This works for both new installs and existing users who already have images.
     */
    private fun createNoMediaFiles() {
        try {
            val imageDirectories = listOf(
                java.io.File(filesDir, "exercise_images"),
                java.io.File(filesDir, "food_images")
            )
            
            imageDirectories.forEach { dir ->
                // Create directory if it doesn't exist
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                
                // Create .nomedia file
                val noMediaFile = java.io.File(dir, ".nomedia")
                if (!noMediaFile.exists()) {
                    noMediaFile.createNewFile()
                    AppLogger.i("FoodLogApplication", "Created .nomedia file: ${noMediaFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("FoodLogApplication", "Failed to create .nomedia files", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        AppLogger.shutdown()
    }
}
