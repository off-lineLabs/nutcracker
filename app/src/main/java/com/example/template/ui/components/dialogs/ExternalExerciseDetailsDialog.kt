package com.example.template.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.template.R
import com.example.template.data.model.ExternalExercise
import com.example.template.data.service.ExternalExerciseService
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.example.template.ui.theme.getContrastingTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalExerciseDetailsDialog(
    exercise: ExternalExercise,
    externalExerciseService: ExternalExerciseService,
    onBack: () -> Unit,
    onImport: () -> Unit
) {
    var currentImageIndex by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState { exercise.images.size }
    
    // Auto-advance slideshow
    LaunchedEffect(pagerState) {
        if (exercise.images.size > 1) {
            while (true) {
                delay(3000) // 3 seconds per image
                val nextPage = (pagerState.currentPage + 1) % exercise.images.size
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
                if (exercise.images.isNotEmpty()) {
                    item {
                        ExerciseImageSlideshow(
                            images = exercise.images,
                            externalExerciseService = externalExerciseService,
                            pagerState = pagerState
                        )
                    }
                }
                
                // Exercise details
                item {
                    ExerciseDetailsCard(exercise = exercise)
                }
                
                // Instructions
                if (exercise.instructions.isNotEmpty()) {
                    item {
                        InstructionsCard(instructions = exercise.instructions)
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onBack) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                    )
                }
                Button(
                    onClick = onImport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_to_my_exercises))
                }
            }
        }
    )
}

@Composable
private fun ExerciseImageSlideshow(
    images: List<String>,
    externalExerciseService: ExternalExerciseService,
    pagerState: androidx.compose.foundation.pager.PagerState
) {
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
                AsyncImage(
                    model = externalExerciseService.getImageUrl(images[page]),
                    contentDescription = "Exercise image ${page + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Page indicators
            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(images.size) { index ->
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
private fun ExerciseDetailsCard(exercise: ExternalExercise) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
            DetailRow("Level", exercise.level.replaceFirstChar { it.uppercase() }, contrastingTextColor)
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.instructions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contrastingTextColor
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
        }
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

