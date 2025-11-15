package com.offlinelabs.nutcracker.data.service

import com.offlinelabs.nutcracker.data.model.ExternalExercise
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
interface ExternalExerciseApi {
    @GET("dist/exercises.json")
    suspend fun getAllExercises(): List<ExternalExercise>
}

class ExternalExerciseService {
    private val baseUrl = "https://raw.githubusercontent.com/off-lineLabs/exercises.json/refs/heads/master/"
    private val imageBaseUrl = "https://raw.githubusercontent.com/off-lineLabs/exercises.json/master/exercises/"
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val api = retrofit.create(ExternalExerciseApi::class.java)
    
    private var cachedExercises: List<ExternalExercise>? = null
    
    suspend fun getAllExercises(): List<ExternalExercise> {
        if (cachedExercises == null) {
            try {
                cachedExercises = api.getAllExercises()
            } catch (e: Exception) {
                // Log the error for debugging
                com.offlinelabs.nutcracker.util.logger.AppLogger.e("ExternalExerciseService", "Failed to fetch exercises", e)
                // Return empty list if network fails
                return emptyList()
            }
        }
        return cachedExercises ?: emptyList()
    }
    
    suspend fun searchExercises(
        query: String,
        equipment: String? = null,
        primaryMuscle: String? = null,
        category: String? = null
    ): List<ExternalExercise> {
        val allExercises = getAllExercises()
        
        return allExercises.filter { exercise ->
            val matchesQuery = query.isEmpty() || 
                exercise.name.contains(query, ignoreCase = true) ||
                exercise.primaryMuscles.any { it.contains(query, ignoreCase = true) } ||
                exercise.equipment?.contains(query, ignoreCase = true) == true ||
                exercise.secondaryMuscles.any { it.contains(query, ignoreCase = true) }
            
            val matchesEquipment = equipment.isNullOrEmpty() || 
                exercise.equipment?.equals(equipment, ignoreCase = true) == true
            
            val matchesMuscle = primaryMuscle.isNullOrEmpty() || 
                exercise.primaryMuscles.any { it.equals(primaryMuscle, ignoreCase = true) }
            
            val matchesCategory = category.isNullOrEmpty() || 
                exercise.category.equals(category, ignoreCase = true)
            
            matchesQuery && matchesEquipment && matchesMuscle && matchesCategory
        }
    }
    
    fun getImageUrl(imagePath: String): String {
        return "$imageBaseUrl$imagePath"
    }
    
    fun getAvailableEquipment(): List<String> {
        return listOf(
            "body only", "dumbbell", "barbell", "kettlebells", "machine", 
            "cable", "bands", "medicine ball", "exercise ball", "foam roll", 
            "e-z curl bar", "other"
        )
    }
    
    fun getAvailableMuscles(): List<String> {
        return listOf(
            "abdominals", "abductors", "adductors", "biceps", "calves", 
            "chest", "forearms", "glutes", "hamstrings", "lats", 
            "lower back", "middle back", "neck", "quadriceps", 
            "shoulders", "traps", "triceps"
        )
    }
    
    fun getAvailableCategories(): List<String> {
        return listOf(
            "strength", "stretching", "strongman", "plyometrics", 
            "cardio", "olympic weightlifting"
        )
    }
}
