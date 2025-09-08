package com.example.template.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExternalExercise
import com.example.template.data.service.ExternalExerciseService
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSelectExerciseDialog(
    exercises: List<Exercise>,
    externalExerciseService: ExternalExerciseService,
    onDismiss: () -> Unit,
    onAddExercise: () -> Unit,
    onSelectExercise: (Exercise) -> Unit,
    onImportExternalExercise: (ExternalExercise) -> Unit
) {
    var dialogState by remember { mutableStateOf(DialogState.MAIN) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedEquipment by remember { mutableStateOf<String?>(null) }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchResults by remember { mutableStateOf<List<ExternalExercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedExternalExercise by remember { mutableStateOf<ExternalExercise?>(null) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Debounced search
    LaunchedEffect(searchQuery, selectedEquipment, selectedMuscle, selectedCategory) {
        if (searchQuery.length >= 3) {
            isLoading = true
            delay(300) // Debounce
            searchResults = externalExerciseService.searchExercises(
                query = searchQuery,
                equipment = selectedEquipment,
                primaryMuscle = selectedMuscle,
                category = selectedCategory
            )
            isLoading = false
        } else {
            searchResults = emptyList()
        }
    }
    
    when (dialogState) {
        DialogState.MAIN -> {
            MainExerciseSelectionDialog(
                exercises = exercises,
                onDismiss = onDismiss,
                onAddExercise = onAddExercise,
                onSelectExercise = onSelectExercise,
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
                onBack = { 
                    dialogState = DialogState.MAIN
                    searchQuery = ""
                    searchResults = emptyList()
                },
                onSelectExternalExercise = { exercise ->
                    selectedExternalExercise = exercise
                    dialogState = DialogState.EXTERNAL_DETAILS
                },
                keyboardController = keyboardController
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

@Composable
private fun MainExerciseSelectionDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onAddExercise: () -> Unit,
    onSelectExercise: (Exercise) -> Unit,
    onSearchExternal: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Exercise",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Search external database button
                Button(
                    onClick = onSearchExternal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search Exercise Database")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add new exercise button
                Button(
                    onClick = onAddExercise,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Exercise")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Existing exercises list
                if (exercises.isEmpty()) {
                    Text(
                        text = "No exercises yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Select existing exercise",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FitnessCenter,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercise.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = exercise.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
