package com.example.template.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service for downloading and managing exercise images locally
 */
class ExerciseImageService(private val context: Context) {
    
    companion object {
        private const val TAG = "ExerciseImageService"
        private const val IMAGE_DIR = "exercise_images"
        private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB max image size
    }
    
    private val imageDir: File by lazy {
        File(context.filesDir, IMAGE_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * Download and save the first image from an external exercise
     * @param imageUrl The URL of the image to download
     * @param exerciseId The ID of the exercise (used for filename)
     * @return The local file path if successful, null otherwise
     */
    suspend fun downloadAndSaveImage(imageUrl: String, exerciseId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "exercise_${exerciseId}.jpg"
            val localFile = File(imageDir, fileName)
            
            // If file already exists, return the path
            if (localFile.exists()) {
                return@withContext localFile.absolutePath
            }
            
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.doInput = true
            connection.connect()
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Failed to download image: HTTP ${connection.responseCode}")
                return@withContext null
            }
            
            val inputStream: InputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            connection.disconnect()
            
            if (bitmap == null) {
                Log.w(TAG, "Failed to decode image bitmap")
                return@withContext null
            }
            
            // Resize image if too large to save space
            val resizedBitmap = resizeBitmapIfNeeded(bitmap)
            
            // Save to local file
            val outputStream = FileOutputStream(localFile)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
            outputStream.close()
            
            Log.d(TAG, "Successfully saved image: ${localFile.absolutePath}")
            return@withContext localFile.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Resize bitmap if it's too large to save storage space
     */
    private fun resizeBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDimension = 400 // Max width/height in pixels
        
        if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) {
            return bitmap
        }
        
        val scale = minOf(
            maxDimension.toFloat() / bitmap.width,
            maxDimension.toFloat() / bitmap.height
        )
        
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Delete the image file for an exercise
     * @param imagePath The local path to the image file
     */
    suspend fun deleteImage(imagePath: String?) = withContext(Dispatchers.IO) {
        if (imagePath.isNullOrBlank()) return@withContext
        
        try {
            val file = File(imagePath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Image deletion result: $deleted for path: $imagePath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image: ${e.message}", e)
        }
    }
    
    /**
     * Clean up all exercise images (useful for testing or app reset)
     */
    suspend fun clearAllImages() = withContext(Dispatchers.IO) {
        try {
            if (imageDir.exists()) {
                imageDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.startsWith("exercise_")) {
                        file.delete()
                    }
                }
                Log.d(TAG, "Cleared all exercise images")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing images: ${e.message}", e)
        }
    }
    
    /**
     * Get the local file path for an exercise image
     * @param exerciseId The exercise ID
     * @return The local file path if it exists, null otherwise
     */
    fun getImagePath(exerciseId: Long): String? {
        val fileName = "exercise_${exerciseId}.jpg"
        val file = File(imageDir, fileName)
        return if (file.exists()) file.absolutePath else null
    }
}
