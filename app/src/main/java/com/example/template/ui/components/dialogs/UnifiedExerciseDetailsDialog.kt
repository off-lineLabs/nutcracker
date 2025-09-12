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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedExerciseDetailsDialog(
    exercise: Exercise,
    externalExerciseService: ExternalExerciseService? = null,
    exerciseImageService: ExerciseImageService? = null,
    onBack: () -> Unit,
    onEdit: () -> Unit
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
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                
                // Exercise details
                item {
                    ExerciseDetailsCard(exercise = exercise)
                }
                
                // Swipable content: Instructions vs Personal Data
                item {
                    SwipableContentCard(exercise = exercise)
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onBack) {
                    Text("Close")
                }
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Exercise")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Exercise Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            DetailRow("Category", exercise.category.replaceFirstChar { it.uppercase() })
            exercise.equipment?.let { DetailRow("Equipment", it.replaceFirstChar { it.uppercase() }) }
            exercise.force?.let { DetailRow("Force", it.replaceFirstChar { it.uppercase() }) }
            exercise.level?.let { DetailRow("Level", it.replaceFirstChar { it.uppercase() }) }
            exercise.mechanic?.let { DetailRow("Mechanic", it.replaceFirstChar { it.uppercase() }) }
            
            if (exercise.primaryMuscles.isNotEmpty()) {
                DetailRow("Primary Muscles", exercise.primaryMuscles.joinToString(", ") { it.replaceFirstChar { it.uppercase() } })
            }
            
            if (exercise.secondaryMuscles.isNotEmpty()) {
                DetailRow("Secondary Muscles", exercise.secondaryMuscles.joinToString(", ") { it.replaceFirstChar { it.uppercase() } })
            }
        }
    }
}

@Composable
private fun SwipableContentCard(exercise: Exercise) {
    var currentTab by remember { mutableStateOf(0) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tab indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton(
                    text = "Instructions",
                    isSelected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "Personal Data",
                    isSelected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Content based on selected tab
            Box(
                modifier = Modifier.height(200.dp)
            ) {
                when (currentTab) {
                    0 -> InstructionsContent(exercise.instructions)
                    1 -> PersonalDataContent(exercise)
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun InstructionsContent(instructions: List<String>) {
    if (instructions.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Instructions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            instructions.forEachIndexed { index, instruction ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No instructions available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PersonalDataContent(exercise: Exercise) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Personal Data",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        PersonalDataRow("Default Weight", "${exercise.defaultWeight} kg")
        PersonalDataRow("Default Reps", exercise.defaultReps.toString())
        PersonalDataRow("Default Sets", exercise.defaultSets.toString())
        
        exercise.kcalBurnedPerRep?.let { 
            PersonalDataRow("Kcal per Rep", it.toString()) 
        }
        exercise.kcalBurnedPerMinute?.let { 
            PersonalDataRow("Kcal per Minute", it.toString()) 
        }
        
        exercise.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Notes:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PersonalDataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
