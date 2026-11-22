package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.background
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
import com.offlinelabs.nutcracker.R
import coil.compose.AsyncImage
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseCategoryMapper
import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.data.gymLabel
import com.offlinelabs.nutcracker.ui.components.gym.GymPickerSheet
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import androidx.compose.foundation.BorderStroke
import com.offlinelabs.nutcracker.ui.components.gym.gymPickerOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import com.offlinelabs.nutcracker.data.repo.FoodLogRepository
import com.offlinelabs.nutcracker.data.service.ExternalExerciseService
import com.offlinelabs.nutcracker.data.service.ExerciseImageService
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedExerciseDetailsDialog(
    exercise: Exercise,
    externalExerciseService: ExternalExerciseService? = null,
    exerciseImageService: ExerciseImageService? = null,
    foodLogRepository: FoodLogRepository? = null,
    /** The exercise's gym (may be a hidden one), for the header label. Null for shared exercises. */
    gym: Gym? = null,
    /** Visible gyms, the targets of "Copy to another gym". */
    gyms: List<Gym> = emptyList(),
    /** Called with the target gym id (null = shared). When null the copy action is hidden. */
    onCopyToGym: ((Long?) -> Unit)? = null,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCheckIn: () -> Unit
) {
    val imagePaths = exercise.imagePaths
    val pagerState = rememberPagerState { imagePaths.size }
    val copyTargets = remember(gyms, exercise.gymId) {
        gymPickerOptions(gyms, excludeGymId = exercise.gymId, excludeShared = exercise.gymId == null)
    }
    var showCopyPicker by rememberSaveable { mutableStateOf(false) }
    
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    if (gym != null) {
                        Text(
                            text = gymLabel(gym.name, gym.unit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                    PersonalDataCard(exercise = exercise, foodLogRepository = foodLogRepository, onEdit = onEdit)
                }
                
                if (onCopyToGym != null && copyTargets.isNotEmpty()) {
                    item {
                        // The theme's primary is navy on a navy surface in dark mode; the form
                        // accent is primary in light and a max-contrast shade in dark.
                        val copyColor = FormDefaults.colors().accent
                        OutlinedButton(
                            onClick = { showCopyPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = copyColor),
                            border = BorderStroke(1.dp, copyColor.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.gym_copy_action))
                        }
                    }
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
                    Text(
                        text = stringResource(R.string.close),
                        color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                    )
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
                    Text(stringResource(R.string.check_in_exercise))
                }
            }
        }
    )

    if (showCopyPicker && onCopyToGym != null) {
        GymPickerSheet(
            title = stringResource(R.string.gym_copy_action),
            subtitle = stringResource(R.string.gym_copy_subtitle),
            options = copyTargets,
            onSelected = { target ->
                showCopyPicker = false
                onCopyToGym(target)
            },
            onDismiss = { showCopyPicker = false }
        )
    }
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
                        contentDescription = stringResource(R.string.exercise_image_numbered, page + 1),
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
                            contentDescription = stringResource(R.string.exercise_image),
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.exercise_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contrastingTextColor
            )
            
            DetailRow(stringResource(R.string.category), exercise.category.replaceFirstChar { it.uppercase() }, contrastingTextColor)
            exercise.equipment?.let { DetailRow(stringResource(R.string.equipment), it.replaceFirstChar { it.uppercase() }, contrastingTextColor) }
            exercise.force?.let { DetailRow(stringResource(R.string.exercise_force), it.replaceFirstChar { it.uppercase() }, contrastingTextColor) }
            exercise.level?.let { DetailRow(stringResource(R.string.exercise_level), it.replaceFirstChar { it.uppercase() }, contrastingTextColor) }
            exercise.mechanic?.let { DetailRow(stringResource(R.string.exercise_mechanic), it.replaceFirstChar { it.uppercase() }, contrastingTextColor) }
            
            val muscleName = com.offlinelabs.nutcracker.ui.components.rememberMuscleLabeler()
            if (exercise.primaryMuscles.isNotEmpty()) {
                DetailRow(stringResource(R.string.primary_muscles), exercise.primaryMuscles.joinToString(", ", transform = muscleName), contrastingTextColor)
            }
            
            if (exercise.secondaryMuscles.isNotEmpty()) {
                DetailRow(stringResource(R.string.secondary_muscles), exercise.secondaryMuscles.joinToString(", ", transform = muscleName), contrastingTextColor)
            }
        }
    }
}

@Composable
private fun PersonalDataCard(exercise: Exercise, foodLogRepository: FoodLogRepository? = null, onEdit: () -> Unit) {
    val cardBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val contrastingTextColor = getContrastingTextColor(cardBackgroundColor)
    
    // Fetch maximum weight recorded
    var maxWeight by remember { mutableStateOf(0.0) }
    
    LaunchedEffect(exercise.id, foodLogRepository) {
        foodLogRepository?.getMaxWeightForExercise(exercise.id)?.collect { weight ->
            maxWeight = weight ?: 0.0
        }
    }
    
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
            // Header row with centered title and edit button
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Centered title
                Text(
                    text = stringResource(R.string.personal_data),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contrastingTextColor,
                    modifier = Modifier.align(Alignment.Center)
                )
                // Edit button on the right
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterEnd)
                ) {
                    Icon(
                        Icons.Filled.Edit, 
                        contentDescription = stringResource(R.string.edit_exercise),
                        modifier = Modifier.size(18.dp),
                        tint = contrastingTextColor
                    )
                }
            }
            
            PersonalDataRow(stringResource(R.string.default_weight), stringResource(R.string.kg_value_format, exercise.defaultWeight.toString()), contrastingTextColor)
            PersonalDataRow(stringResource(R.string.maximum_weight), stringResource(R.string.kg_value_format, maxWeight.toString()), contrastingTextColor)
            PersonalDataRow(stringResource(R.string.default_reps), exercise.defaultReps.toString(), contrastingTextColor)
            PersonalDataRow(stringResource(R.string.default_sets), exercise.defaultSets.toString(), contrastingTextColor)
            
            exercise.kcalBurnedPerUnit?.let { 
                val label = when (ExerciseCategoryMapper.getExerciseType(exercise.category)) {
                    ExerciseType.STRENGTH -> stringResource(R.string.kcal_per_set)
                    ExerciseType.CARDIO -> stringResource(R.string.kcal_per_minute)
                    ExerciseType.BODYWEIGHT -> stringResource(R.string.kcal_per_rep)
                }
                PersonalDataRow(label, it.toString(), contrastingTextColor) 
            }
            
            exercise.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.notes_label),
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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


