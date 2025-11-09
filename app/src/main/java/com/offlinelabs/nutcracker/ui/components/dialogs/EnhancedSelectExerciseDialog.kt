package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExternalExercise
import com.offlinelabs.nutcracker.data.service.ExternalExerciseService
import com.offlinelabs.nutcracker.ui.components.ExerciseImageIcon
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor
import com.offlinelabs.nutcracker.ui.theme.brandSecondaryShade
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSelectExerciseDialog(
    exercises: List<Exercise>,
    externalExerciseService: ExternalExerciseService,
    onDismiss: () -> Unit,
    onAddExercise: () -> Unit,
    onSelectExercise: (Exercise) -> Unit,
    onImportExternalExercise: (ExternalExercise) -> Unit,
    onEditExercise: (Exercise) -> Unit
) {
    var dialogState by remember { mutableStateOf(DialogState.MAIN) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedEquipment by remember { mutableStateOf<String?>(null) }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchResults by remember { mutableStateOf<List<ExternalExercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedExternalExercise by remember { mutableStateOf<ExternalExercise?>(null) }
    var currentFilterCount by remember { mutableIntStateOf(0) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Smart search with filter count trigger
    LaunchedEffect(searchQuery, selectedEquipment, selectedMuscle, selectedCategory) {
        isLoading = true
        delay(300) // Debounce
        
        // Get all exercises first
        val allExercises = externalExerciseService.getAllExercises()
        
        // Apply filters
        val filteredExercises = allExercises.filter { exercise ->
            val matchesQuery = searchQuery.isEmpty() || 
                exercise.name.contains(searchQuery, ignoreCase = true) ||
                exercise.primaryMuscles.any { it.contains(searchQuery, ignoreCase = true) } ||
                exercise.equipment?.contains(searchQuery, ignoreCase = true) == true ||
                exercise.secondaryMuscles.any { it.contains(searchQuery, ignoreCase = true) }
            
            val matchesEquipment = selectedEquipment.isNullOrEmpty() || 
                exercise.equipment?.equals(selectedEquipment, ignoreCase = true) == true
            
            val matchesMuscle = selectedMuscle.isNullOrEmpty() || 
                exercise.primaryMuscles.any { it.equals(selectedMuscle, ignoreCase = true) }
            
            val matchesCategory = selectedCategory.isNullOrEmpty() || 
                exercise.category.equals(selectedCategory, ignoreCase = true)
            
            matchesQuery && matchesEquipment && matchesMuscle && matchesCategory
        }
        
        // Update filter count
        currentFilterCount = filteredExercises.size
        
        // Show results if we have a reasonable number (15 or fewer)
        // OR if user has typed 3+ characters
        val shouldShowResults = filteredExercises.size <= 15 || searchQuery.length >= 3
        
        searchResults = if (shouldShowResults) filteredExercises else emptyList()
        isLoading = false
    }
    
    when (dialogState) {
        DialogState.MAIN -> {
            MainExerciseSelectionDialog(
                exercises = exercises,
                onDismiss = onDismiss,
                onAddExercise = onAddExercise,
                onSelectExercise = onSelectExercise,
                onEditExercise = onEditExercise,
                onSearchExternal = { dialogState = DialogState.SEARCH }
            )
        }
        
        DialogState.SEARCH -> {
            SearchExternalExercisesDialog(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedEquipment = selectedEquipment,
                onEquipmentChange = { selectedEquipment = it },
                selectedMuscle = selectedMuscle,
                onMuscleChange = { selectedMuscle = it },
                selectedCategory = selectedCategory,
                onCategoryChange = { selectedCategory = it },
                searchResults = searchResults,
                isLoading = isLoading,
                currentFilterCount = currentFilterCount,
                onBack = { 
                    dialogState = DialogState.MAIN
                    searchQuery = ""
                    searchResults = emptyList()
                },
                onSelectExternalExercise = { exercise ->
                    selectedExternalExercise = exercise
                    dialogState = DialogState.EXTERNAL_DETAILS
                },
                keyboardController = keyboardController,
                externalExerciseService = externalExerciseService
            )
        }
        
        DialogState.EXTERNAL_DETAILS -> {
            selectedExternalExercise?.let { exercise ->
                ExternalExerciseDetailsDialog(
                    exercise = exercise,
                    externalExerciseService = externalExerciseService,
                    onBack = { dialogState = DialogState.SEARCH },
                    onImport = { 
                        onImportExternalExercise(exercise)
                        onDismiss()
                    }
                )
            }
        }
    }
}

enum class DialogState {
    MAIN, SEARCH, EXTERNAL_DETAILS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainExerciseSelectionDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onAddExercise: () -> Unit,
    onSelectExercise: (Exercise) -> Unit,
    onEditExercise: (Exercise) -> Unit,
    onSearchExternal: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.9f)
                .padding(4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Custom title with back arrow
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Back arrow button on the left
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                            tint = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                        )
                    }
                    
                    // Centered title
                    Text(
                        text = stringResource(R.string.my_exercises),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                // Search external database button
                Button(
                    onClick = onSearchExternal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandSecondaryShade(0)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.search_exercise_database))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add new exercise button
                Button(
                    onClick = onAddExercise,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandSecondaryShade(2)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ballot),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.type_information))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Existing exercises list
                if (exercises.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_exercises_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(exercises) { exercise ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectExercise(exercise) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                val cardBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
                                val contrastingTextColor = getContrastingTextColor(cardBackgroundColor)
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ExerciseImageIcon(
                                        exercise = exercise,
                                        size = 48.dp,
                                        showShadow = true
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercise.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = contrastingTextColor
                                        )
                                        Text(
                                            text = exercise.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = contrastingTextColor.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    // Edit button with pencil icon on the right
                                    IconButton(
                                        onClick = { onEditExercise(exercise) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.edit_exercise),
                                            tint = contrastingTextColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
