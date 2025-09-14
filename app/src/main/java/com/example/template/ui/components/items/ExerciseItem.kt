package com.example.template.ui.components.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.template.R
import com.example.template.data.dao.DailyExerciseEntry
import com.example.template.data.model.ExerciseCategoryMapper
import com.example.template.data.model.ExerciseType
import com.example.template.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExerciseItem(
    exerciseEntry: DailyExerciseEntry,
    onEdit: (() -> Unit)? = null
) {
    // Map database category to UI exercise type
    val exerciseType = ExerciseCategoryMapper.getExerciseType(exerciseEntry.exerciseType)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit?.invoke() }
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = appContainerBackgroundColor() // Use themed container color
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exercise icon with modern gradient and shadow - using your brand red
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = exerciseItemBackgroundColor().copy(alpha = 0.3f),
                        spotColor = exerciseItemBackgroundColor().copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                exerciseItemBackgroundColor(),
                                exerciseItemBackgroundColor().copy(alpha = 0.8f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (exerciseType) {
                        ExerciseType.CARDIO -> Icons.Filled.Favorite // ECG heart icon
                        ExerciseType.BODYWEIGHT -> Icons.Filled.SportsGymnastics // Sports gymnastics icon
                        ExerciseType.STRENGTH -> Icons.Filled.FitnessCenter // Exercise icon
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Exercise details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exerciseEntry.exerciseName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = appTextPrimaryColor()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sets (for non-cardio exercises) - First
                    if (exerciseType != ExerciseType.CARDIO) {
                        Text(
                            text = "${exerciseEntry.sets} ${stringResource(R.string.sets_unit)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = appTextSecondaryColor()
                        )
                    }
                    
                    // Reps - Second
                    Text(
                        text = when (exerciseType) {
                            ExerciseType.CARDIO -> "${exerciseEntry.reps} ${stringResource(R.string.min_unit)}"
                            else -> "${exerciseEntry.reps} ${stringResource(R.string.reps_unit)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = appTextSecondaryColor()
                    )
                    
                    // Weight (for strength exercises) - Last, with distinguished color
                    if (exerciseType == ExerciseType.STRENGTH) {
                        Text(
                            text = "${exerciseEntry.weight} ${stringResource(R.string.kg_unit)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = appTextTertiaryColor() // Distinguished lighter color
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Calories burned
                Text(
                    text = "-${exerciseEntry.caloriesBurned.toInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981)
                )
                
                // Time
                Text(
                    text = formatTime(exerciseEntry.logDateTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun formatTime(dateTimeString: String): String {
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = formatter.parse(dateTimeString)
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeFormatter.format(date ?: Date())
    } catch (e: Exception) {
        ""
    }
}
