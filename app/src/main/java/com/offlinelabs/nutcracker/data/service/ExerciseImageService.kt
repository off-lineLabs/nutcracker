package com.offlinelabs.nutcracker.data.service

import android.content.Context
import com.offlinelabs.nutcracker.util.logger.AppLogger
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
        AppLogger.i("ExerciseImageService", "=== START downloadAndStoreImages ===")
        AppLogger.i("ExerciseImageService", "Exercise ID: $exerciseId")
        AppLogger.i("ExerciseImageService", "Number of URLs to download: ${imageUrls.size}")
        AppLogger.i("ExerciseImageService", "Image directory: ${imageDir.absolutePath}")
        AppLogger.i("ExerciseImageService", "Directory exists: ${imageDir.exists()}")
        
        return withContext(Dispatchers.IO) {
            val localPaths = mutableListOf<String>()
            
            imageUrls.forEachIndexed { index, imageUrl ->
                try {
                    AppLogger.i("ExerciseImageService", "Processing image $index: $imageUrl")
                    
                    val fileName = "${exerciseId}_${index}.jpg"
                    val localFile = File(imageDir, fileName)
                    
                    AppLogger.i("ExerciseImageService", "Target file: ${localFile.absolutePath}")
                    
                    // Skip if file already exists
                    if (localFile.exists()) {
                        AppLogger.i("ExerciseImageService", "File already exists, reusing: $fileName")
                        localPaths.add(localFile.absolutePath)
                        AppLogger.i("ExerciseImageService", "Added existing path to list. Total paths: ${localPaths.size}")
                        return@forEachIndexed
                    }
                    
                    // Download the image
                    AppLogger.i("ExerciseImageService", "Starting download from URL: $imageUrl")
                    val url = URL(imageUrl)
                    val connection = url.openConnection()
                    connection.connectTimeout = 10000 // 10 seconds
                    connection.readTimeout = 10000
                    val inputStream: InputStream = connection.getInputStream()
                    val outputStream = FileOutputStream(localFile)
                    
                    var bytesDownloaded = 0L
                    inputStream.use { input ->
                        outputStream.use { output ->
                            bytesDownloaded = input.copyTo(output)
                        }
                    }
                    
                    AppLogger.i("ExerciseImageService", "Downloaded $bytesDownloaded bytes")
                    AppLogger.i("ExerciseImageService", "File exists after download: ${localFile.exists()}")
                    AppLogger.i("ExerciseImageService", "File size: ${localFile.length()} bytes")
                    
                    localPaths.add(localFile.absolutePath)
                    AppLogger.i("ExerciseImageService", "Successfully downloaded: $fileName")
                    AppLogger.i("ExerciseImageService", "Added path to list: ${localFile.absolutePath}")
                    AppLogger.i("ExerciseImageService", "Total paths so far: ${localPaths.size}")
                    
                } catch (e: Exception) {
                    AppLogger.e("ExerciseImageService", "Failed to download image $index from: $imageUrl", e)
                    AppLogger.e("ExerciseImageService", "Error type: ${e.javaClass.simpleName}, Message: ${e.message}")
                    // Continue with other images even if one fails
                }
            }
            
            AppLogger.i("ExerciseImageService", "=== END downloadAndStoreImages ===")
            AppLogger.i("ExerciseImageService", "Total paths returned: ${localPaths.size}")
            localPaths.forEachIndexed { index, path ->
                AppLogger.i("ExerciseImageService", "Path $index: $path")
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