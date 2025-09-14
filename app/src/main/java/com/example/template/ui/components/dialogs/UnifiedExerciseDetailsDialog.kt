package com.example.template.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExternalExercise
import com.example.template.data.service.ExternalExerciseService
import com.example.template.data.service.ExerciseImageService
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.example.template.R
import com.example.template.ui.theme.getContrastingTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedExerciseDetailsDialog(
    exercise: Exercise,
    externalExerciseService: ExternalExerciseService? = null,
    exerciseImageService: ExerciseImageService? = null,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCheckIn: () -> Unit
) {
    val imagePaths = exercise.imagePaths
    val pagerState = rememberPagerState { imagePaths.size }
    
    // Auto-advance slideshow if there are multiple images
    LaunchedEffect(pagerState) {
        if (imagePaths.size > 1) {
            while (true) {
                delay(3000) // 3 seconds per image
                val nextPage = (pagerState.currentPage + 1) % imagePaths.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onBack,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Exercise")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Exercise images slideshow
                if (imagePaths.isNotEmpty()) {
                    item {
                        ExerciseImageSlideshow(
                            imagePaths = imagePaths,
                            exerciseImageService = exerciseImageService
                        )
                    }
                }
                
                // Personal Data card (moved from swipable content)
                item {
                    PersonalDataCard(exercise = exercise)
                }
                
                // Exercise details
                item {
                    ExerciseDetailsCard(exercise = exercise)
                }
                
                // Instructions card (simplified from swipable content)
                item {
                    InstructionsCard(exercise.instructions)
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.close))
                }
                Button(
                    onClick = onCheckIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check-in Exercise")
                }
            }
        }
    )
}

@Composable
private fun ExerciseImageSlideshow(
    imagePaths: List<String>,
    exerciseImageService: ExerciseImageService?
) {
    val pagerState = rememberPagerState { imagePaths.size }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val imageFile = exerciseImageService?.getImageFile(imagePaths[page])
                if (imageFile != null) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = "Exercise image ${page + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback to a placeholder or icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = "Exercise image",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Page indicators
            if (imagePaths.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(imagePaths.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (index == pagerState.currentPage) 
                                        Color.White 
                                    else 
                                        Color.White.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailsCard(exercise: Exercise) {
    val cardBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val contrastingTextColor = getContrastingTextColor(cardBackgroundColor)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.exercise_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contrastingTextColor
            )
            
            DetailRow("Category", exercise.category.replaceFirstChar { it.uppercase() }, contrastingTextColor)
            exercise.equipment?.let { DetailRow("Equipment", it.replaceFirstChar { it.uppercase() }, contrastingTextColor) }
            exercise.force?.let { DetailRow("Force", it.replaceFirstChar { it.uppercase() }, contrastingTextColor) }
            exercise.level?.let { DetailRow("Level", it.replaceFirstChar { it.uppercase() }, contrastingTextColor) }
            exercise.mechanic?.let { DetailRow("Mechanic", it.replaceFirstChar { it.uppercase() }, contrastingTextColor) }
            
            if (exercise.primaryMuscles.isNotEmpty()) {
                DetailRow("Primary Muscles", exercise.primaryMuscles.joinToString(", ") { it.replaceFirstChar { it.uppercase() } }, contrastingTextColor)
            }
            
            if (exercise.secondaryMuscles.isNotEmpty()) {
                DetailRow("Secondary Muscles", exercise.secondaryMuscles.joinToString(", ") { it.replaceFirstChar { it.uppercase() } }, contrastingTextColor)
            }
        }
    }
}

@Composable
private fun PersonalDataCard(exercise: Exercise) {
    val cardBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val contrastingTextColor = getContrastingTextColor(cardBackgroundColor)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.personal_data),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contrastingTextColor
            )
            
            PersonalDataRow("Default Weight", "${exercise.defaultWeight} kg", contrastingTextColor)
            PersonalDataRow("Default Reps", exercise.defaultReps.toString(), contrastingTextColor)
            PersonalDataRow("Default Sets", exercise.defaultSets.toString(), contrastingTextColor)
            
            exercise.kcalBurnedPerRep?.let { 
                PersonalDataRow("Kcal per Rep", it.toString(), contrastingTextColor) 
            }
            exercise.kcalBurnedPerMinute?.let { 
                PersonalDataRow("Kcal per Minute", it.toString(), contrastingTextColor) 
            }
            
            exercise.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = contrastingTextColor.copy(alpha = 0.7f)
                )
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contrastingTextColor
                )
            }
        }
    }
}

@Composable
private fun InstructionsCard(instructions: List<String>) {
    val cardBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val contrastingTextColor = getContrastingTextColor(cardBackgroundColor)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.instructions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contrastingTextColor
            )
            
            if (instructions.isNotEmpty()) {
                instructions.forEachIndexed { index, instruction ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = contrastingTextColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = instruction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contrastingTextColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.no_instructions_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contrastingTextColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}


@Composable
private fun PersonalDataRow(label: String, value: String, contrastingTextColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contrastingTextColor.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = contrastingTextColor
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, contrastingTextColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contrastingTextColor.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = contrastingTextColor
        )
    }
}


