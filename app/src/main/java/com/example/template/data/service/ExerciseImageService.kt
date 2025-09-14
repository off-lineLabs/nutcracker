package com.example.template.data.service

import android.content.Context
import com.example.template.util.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

class ExerciseImageService(private val context: Context) {
    
    private val imageDir = File(context.filesDir, "exercise_images")
    
    init {
        // Create the directory if it doesn't exist
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }
    }
    
    /**
     * Downloads and stores exercise images locally
     * @param imageUrls List of image URLs to download
     * @param exerciseId The exercise ID to use as a prefix for file names
     * @return List of local file paths where images were saved
     */
    suspend fun downloadAndStoreImages(imageUrls: List<String>, exerciseId: String): List<String> {
        return withContext(Dispatchers.IO) {
            val localPaths = mutableListOf<String>()
            
            imageUrls.forEachIndexed { index, imageUrl ->
                try {
                    val fileName = "${exerciseId}_${index}.jpg"
                    val localFile = File(imageDir, fileName)
                    
                    // Skip if file already exists
                    if (localFile.exists()) {
                        localPaths.add(localFile.absolutePath)
                        return@forEachIndexed
                    }
                    
                    // Download the image
                    val url = URL(imageUrl)
                    val inputStream: InputStream = url.openStream()
                    val outputStream = FileOutputStream(localFile)
                    
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    localPaths.add(localFile.absolutePath)
                    AppLogger.d("ExerciseImageService", "Downloaded image: $fileName")
                    
                } catch (e: Exception) {
                    AppLogger.e("ExerciseImageService", "Failed to download image: $imageUrl", e)
                    // Continue with other images even if one fails
                }
            }
            
            localPaths
        }
    }
    
    /**
     * Downloads and saves a single image for an exercise (legacy method for backward compatibility)
     * @param imageUrl The URL of the image to download
     * @param exerciseId The exercise ID to use as a prefix for file name
     * @return The local file path if successful, null otherwise
     */
    suspend fun downloadAndSaveImage(imageUrl: String, exerciseId: Long): String? {
        val localPaths = downloadAndStoreImages(listOf(imageUrl), exerciseId.toString())
        return localPaths.firstOrNull()
    }
    
    /**
     * Deletes a single image file
     * @param imagePath The local file path to delete
     */
    suspend fun deleteImage(imagePath: String?) {
        if (imagePath != null) {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(imagePath)
                    if (file.exists()) {
                        file.delete()
                        AppLogger.d("ExerciseImageService", "Deleted image: $imagePath")
                    }
                } catch (e: Exception) {
                    AppLogger.e("ExerciseImageService", "Error deleting image: $imagePath", e)
                }
            }
        }
    }
    
    /**
     * Gets the local file path for an image
     * @param imagePath The local file path
     * @return The file if it exists, null otherwise
     */
    fun getImageFile(imagePath: String): File? {
        val file = File(imagePath)
        return if (file.exists()) file else null
    }
    
    /**
     * Deletes all images for a specific exercise
     * @param exerciseId The exercise ID
     */
    suspend fun deleteExerciseImages(exerciseId: String) {
        withContext(Dispatchers.IO) {
            val files = imageDir.listFiles { file ->
                file.name.startsWith("${exerciseId}_")
            }
            files?.forEach { file ->
                if (file.delete()) {
                    AppLogger.d("ExerciseImageService", "Deleted image: ${file.name}")
                }
            }
        }
    }
    
    /**
     * Cleans up orphaned image files (images that don't correspond to any existing exercise)
     * @param validImagePaths List of valid image paths that should be kept
     */
    suspend fun cleanupOrphanedImages(validImagePaths: List<String>) {
        withContext(Dispatchers.IO) {
            val validFiles = validImagePaths.map { File(it) }.toSet()
            val allImageFiles = imageDir.listFiles() ?: return@withContext
            
            allImageFiles.forEach { file ->
                if (!validFiles.contains(file)) {
                    if (file.delete()) {
                        AppLogger.d("ExerciseImageService", "Cleaned up orphaned image: ${file.name}")
                    }
                }
            }
        }
    }
}