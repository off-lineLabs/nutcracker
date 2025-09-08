package com.example.template.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.template.data.model.ExternalExercise
import com.example.template.data.service.ExternalExerciseService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchExternalExercisesDialog(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedEquipment: String?,
    onEquipmentChange: (String?) -> Unit,
    selectedMuscle: String?,
    onMuscleChange: (String?) -> Unit,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    searchResults: List<ExternalExercise>,
    isLoading: Boolean,
    currentFilterCount: Int,
    onBack: () -> Unit,
    onSelectExternalExercise: (ExternalExercise) -> Unit,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    var showEquipmentFilter by remember { mutableStateOf(false) }
    var showMuscleFilter by remember { mutableStateOf(false) }
    var showCategoryFilter by remember { mutableStateOf(false) }
    
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
                    text = "Search Exercise Database",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search exercises...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { keyboardController?.hide() }
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Filter dropdowns - stacked vertically for better space usage
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Equipment filter
                    ExposedDropdownMenuBox(
                        expanded = showEquipmentFilter,
                        onExpandedChange = { showEquipmentFilter = !showEquipmentFilter }
                    ) {
                        OutlinedTextField(
                            value = selectedEquipment ?: "All Equipment",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Equipment") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEquipmentFilter)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showEquipmentFilter,
                            onDismissRequest = { showEquipmentFilter = false }
                        ) {
                            val equipmentOptions = listOf(
                                "body only", "dumbbell", "barbell", "kettlebells", "machine", 
                                "cable", "bands", "medicine ball", "exercise ball", "foam roll", 
                                "e-z curl bar", "other"
                            )
                            equipmentOptions.forEach { equipment ->
                                DropdownMenuItem(
                                    text = { Text(equipment.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        onEquipmentChange(if (equipment == selectedEquipment) null else equipment)
                                        showEquipmentFilter = false
                                    },
                                    leadingIcon = if (equipment == selectedEquipment) {
                                        { Icon(Icons.Filled.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                    
                    // Muscle filter
                    ExposedDropdownMenuBox(
                        expanded = showMuscleFilter,
                        onExpandedChange = { showMuscleFilter = !showMuscleFilter }
                    ) {
                        OutlinedTextField(
                            value = selectedMuscle ?: "All Muscles",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Primary Muscle") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMuscleFilter)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showMuscleFilter,
                            onDismissRequest = { showMuscleFilter = false }
                        ) {
                            val muscleOptions = listOf(
                                "abdominals", "abductors", "adductors", "biceps", "calves", 
                                "chest", "forearms", "glutes", "hamstrings", "lats", 
                                "lower back", "middle back", "neck", "quadriceps", 
                                "shoulders", "traps", "triceps"
                            )
                            muscleOptions.forEach { muscle ->
                                DropdownMenuItem(
                                    text = { Text(muscle.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        onMuscleChange(if (muscle == selectedMuscle) null else muscle)
                                        showMuscleFilter = false
                                    },
                                    leadingIcon = if (muscle == selectedMuscle) {
                                        { Icon(Icons.Filled.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                    
                    // Category filter
                    ExposedDropdownMenuBox(
                        expanded = showCategoryFilter,
                        onExpandedChange = { showCategoryFilter = !showCategoryFilter }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory ?: "All Categories",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryFilter)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showCategoryFilter,
                            onDismissRequest = { showCategoryFilter = false }
                        ) {
                            val categoryOptions = listOf(
                                "strength", "stretching", "strongman", "plyometrics", 
                                "cardio", "olympic weightlifting"
                            )
                            categoryOptions.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        onCategoryChange(if (category == selectedCategory) null else category)
                                        showCategoryFilter = false
                                    },
                                    leadingIcon = if (category == selectedCategory) {
                                        { Icon(Icons.Filled.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                }
                
                // Show selected filters
                if (selectedEquipment != null || selectedMuscle != null || selectedCategory != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedEquipment?.let { equipment ->
                            AssistChip(
                                onClick = { onEquipmentChange(null) },
                                label = { Text(equipment) },
                                trailingIcon = {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove filter", modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        selectedMuscle?.let { muscle ->
                            AssistChip(
                                onClick = { onMuscleChange(null) },
                                label = { Text(muscle) },
                                trailingIcon = {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove filter", modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        selectedCategory?.let { category ->
                            AssistChip(
                                onClick = { onCategoryChange(null) },
                                label = { Text(category) },
                                trailingIcon = {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove filter", modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Filter count info
                if (currentFilterCount > 0) {
                    Text(
                        text = "Found $currentFilterCount exercises matching your filters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                // Search results
                if (searchQuery.length < 3 && searchResults.isEmpty() && currentFilterCount > 15) {
                    Text(
                        text = "Too many results ($currentFilterCount). Use more filters or type 3+ characters to narrow down.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (searchQuery.length < 3 && searchResults.isEmpty() && currentFilterCount == 0) {
                    Text(
                        text = "Use filters or type 3+ characters to search",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (searchResults.isEmpty()) {
                    if (searchQuery.length >= 3) {
                        Text(
                            text = "No exercises found. Check your internet connection and try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No exercises found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Found ${searchResults.size} exercises",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { exercise ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectExternalExercise(exercise) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FitnessCenter,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercise.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${exercise.category} • ${exercise.equipment ?: "No equipment"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (exercise.primaryMuscles.isNotEmpty()) {
                                            Text(
                                                text = "Muscles: ${exercise.primaryMuscles.joinToString(", ")}",
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
            }
        },
        confirmButton = {
            TextButton(onClick = onBack) {
                Text("Cancel")
            }
        }
    )
    
}

