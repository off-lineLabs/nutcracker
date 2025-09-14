package com.example.template.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.template.util.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ImageDownloadService(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageDownloadService"
        private const val IMAGES_DIR = "food_images"
        private const val MAX_IMAGE_DIMENSION = 800 // Max width/height
    }
    
    /**
     * Downloads an image from URL and saves it locally
     * @param imageUrl The URL of the image to download
     * @return The local file path if successful, null otherwise
     */
    suspend fun downloadAndSaveImage(imageUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                AppLogger.d(TAG, "Starting image download from: $imageUrl")
                
                // Create images directory if it doesn't exist
                val imagesDir = File(context.filesDir, IMAGES_DIR)
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                
                // Generate unique filename
                val fileExtension = getFileExtension(imageUrl) ?: "jpg"
                val fileName = "${UUID.randomUUID()}.$fileExtension"
                val localFile = File(imagesDir, fileName)
                
                // Download image
                val bitmap = downloadImage(imageUrl)
                if (bitmap != null) {
                    // Resize image if needed
                    val resizedBitmap = resizeImageIfNeeded(bitmap)
                    
                    // Save to local file
                    saveBitmapToFile(resizedBitmap, localFile)
                    
                    AppLogger.d(TAG, "Image saved successfully to: ${localFile.absolutePath}")
                    localFile.absolutePath
                } else {
                    AppLogger.e(TAG, "Failed to download image from: $imageUrl")
                    null
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error downloading image from: $imageUrl", e)
                null
            }
        }
    }
    
    /**
     * Downloads an image from URL and returns as Bitmap
     */
    private suspend fun downloadImage(imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000 // 10 seconds
                connection.readTimeout = 15000 // 15 seconds
                connection.doInput = true
                connection.connect()
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream: InputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    connection.disconnect()
                    bitmap
                } else {
                    AppLogger.e(TAG, "HTTP error: ${connection.responseCode}")
                    connection.disconnect()
                    null
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error downloading image", e)
                null
            }
        }
    }
    
    /**
     * Resizes image if it's too large
     */
    private fun resizeImageIfNeeded(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return bitmap
        }
        
        val scale = minOf(
            MAX_IMAGE_DIMENSION.toFloat() / width,
            MAX_IMAGE_DIMENSION.toFloat() / height
        )
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Saves bitmap to file
     */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        outputStream.flush()
        outputStream.close()
    }
    
    /**
     * Gets file extension from URL
     */
    private fun getFileExtension(url: String): String? {
        return try {
            val lastDot = url.lastIndexOf('.')
            if (lastDot != -1 && lastDot < url.length - 1) {
                url.substring(lastDot + 1).lowercase()
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Deletes a local image file
     */
    suspend fun deleteLocalImage(localPath: String?) {
        if (localPath != null) {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(localPath)
                    if (file.exists()) {
                        file.delete()
                        AppLogger.d(TAG, "Deleted local image: $localPath")
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error deleting local image: $localPath", e)
                }
            }
        }
    }
    
    /**
     * Gets the local file path for an image URL (if already downloaded)
     */
    fun getLocalImagePath(imageUrl: String): String? {
        val imagesDir = File(context.filesDir, IMAGES_DIR)
        if (!imagesDir.exists()) return null
        
        // Look for existing file with same name
        val fileName = getFileNameFromUrl(imageUrl)
        if (fileName != null) {
            val file = File(imagesDir, fileName)
            if (file.exists()) {
                return file.absolutePath
            }
        }
        
        return null
    }
    
    /**
     * Extracts filename from URL
     */
    private fun getFileNameFromUrl(url: String): String? {
        return try {
            val lastSlash = url.lastIndexOf('/')
            if (lastSlash != -1 && lastSlash < url.length - 1) {
                url.substring(lastSlash + 1)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
