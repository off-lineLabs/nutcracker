package com.offlinelabs.nutcracker.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseCategoryMapper
import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.ui.theme.exerciseItemBackgroundColor
import com.offlinelabs.nutcracker.util.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Composable that displays an exercise image if available, otherwise shows the appropriate icon
 * @param exercise The exercise to display
 * @param modifier Modifier for the container
 * @param size The size of the image/icon
 * @param showShadow Whether to show shadow around the image/icon
 */
@Composable
fun ExerciseImageIcon(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    showShadow: Boolean = true
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageLoadError by remember { mutableStateOf(false) }
    
    // Load image if path exists
    val firstImagePath = exercise.imagePaths.firstOrNull()
    LaunchedEffect(firstImagePath) {
        AppLogger.i("ExerciseImageIcon", "LaunchedEffect triggered for exercise: ${exercise.name} (ID: ${exercise.id}), imagePath: $firstImagePath")
        
        if (!firstImagePath.isNullOrBlank() && !imageLoadError) {
            AppLogger.i("ExerciseImageIcon", "Attempting to load image from path: $firstImagePath")
            imageBitmap = loadImageFromPath(firstImagePath)
            
            if (imageBitmap == null) {
                AppLogger.w("ExerciseImageIcon", "Failed to load image from path: $firstImagePath")
                imageLoadError = true
            } else {
                AppLogger.i("ExerciseImageIcon", "Successfully loaded image for exercise: ${exercise.name}")
            }
        } else {
            AppLogger.i("ExerciseImageIcon", "Skipping image load - imagePath: $firstImagePath, imageLoadError: $imageLoadError")
        }
    }
    
    val exerciseType = ExerciseCategoryMapper.getExerciseType(exercise.category)
    
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showShadow) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(8.dp),
                        ambientColor = exerciseItemBackgroundColor().copy(alpha = 0.3f),
                        spotColor = exerciseItemBackgroundColor().copy(alpha = 0.3f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            // Display the exercise image filling the entire box
            androidx.compose.foundation.Image(
                bitmap = imageBitmap!!,
                contentDescription = "Exercise image for ${exercise.name}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop // Show cropped version instead of stretched
            )
        } else {
            // Display the fallback icon with background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                exerciseItemBackgroundColor(),
                                exerciseItemBackgroundColor().copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (exerciseType) {
                        ExerciseType.CARDIO -> Icons.Filled.Favorite
                        ExerciseType.BODYWEIGHT -> Icons.Filled.SportsGymnastics
                        ExerciseType.STRENGTH -> Icons.Filled.FitnessCenter
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
    }
}

/**
 * Load image from local file path
 */
private suspend fun loadImageFromPath(imagePath: String): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        AppLogger.i("ExerciseImageIcon", "Loading image from path: $imagePath")
        val file = File(imagePath)
        
        AppLogger.i("ExerciseImageIcon", "File absolute path: ${file.absolutePath}")
        AppLogger.i("ExerciseImageIcon", "File exists: ${file.exists()}")
        AppLogger.i("ExerciseImageIcon", "File is readable: ${file.canRead()}")
        
        if (file.exists()) {
            AppLogger.i("ExerciseImageIcon", "File exists, size: ${file.length()} bytes")
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                AppLogger.i("ExerciseImageIcon", "Successfully decoded bitmap: ${bitmap.width}x${bitmap.height}")
                bitmap.asImageBitmap()
            } else {
                AppLogger.e("ExerciseImageIcon", "Failed to decode bitmap from file: $imagePath")
                null
            }
        } else {
            AppLogger.w("ExerciseImageIcon", "File does not exist: $imagePath")
            null
        }
    } catch (e: Exception) {
        AppLogger.e("ExerciseImageIcon", "Exception loading image from path: $imagePath", e)
        null
    }
}