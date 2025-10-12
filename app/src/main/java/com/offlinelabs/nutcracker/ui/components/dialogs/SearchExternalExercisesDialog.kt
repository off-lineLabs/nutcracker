package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import com.offlinelabs.nutcracker.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import android.content.Intent
import coil.compose.AsyncImage
import com.offlinelabs.nutcracker.data.model.ExternalExercise
import com.offlinelabs.nutcracker.data.service.ExternalExerciseService
import androidx.compose.ui.res.stringResource
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor
import com.offlinelabs.nutcracker.ui.theme.generateThemedColorShade

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
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    externalExerciseService: ExternalExerciseService
) {
    var showEquipmentFilter by remember { mutableStateOf(false) }
    var showMuscleFilter by remember { mutableStateOf(false) }
    var showCategoryFilter by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
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
                    text = stringResource(R.string.search_exercise_database),
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
                            label = { Text(stringResource(R.string.equipment)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEquipmentFilter)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
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
                            label = { Text(stringResource(R.string.primary_muscle)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMuscleFilter)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
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
                            label = { Text(stringResource(R.string.category)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryFilter)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Filter count info - only show when there are many results
                if (currentFilterCount > 15) {
                    Text(
                        text = stringResource(R.string.search_found_exercises, currentFilterCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = getContrastingTextColor(MaterialTheme.colorScheme.surface),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                // Search results
                if (searchQuery.length < 3 && searchResults.isEmpty() && currentFilterCount > 15) {
                    Text(
                        text = stringResource(R.string.search_too_many_results, currentFilterCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (searchQuery.length < 3 && searchResults.isEmpty() && currentFilterCount == 0) {
                    Text(
                        text = stringResource(R.string.search_use_filters),
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
                            text = stringResource(R.string.search_no_exercises_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.no_exercises_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
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
                                    // Show exercise image if available, otherwise fallback to icon
                                    if (exercise.images.isNotEmpty()) {
                                        AsyncImage(
                                            model = externalExerciseService.getImageUrl(exercise.images.first()),
                                            contentDescription = "Exercise image",
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.FitnessCenter,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercise.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = getContrastingTextColor(MaterialTheme.colorScheme.surfaceVariant)
                                        )
                                        Text(
                                            text = "${exercise.category} • ${exercise.equipment ?: stringResource(R.string.search_no_equipment)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = getContrastingTextColor(MaterialTheme.colorScheme.surfaceVariant).copy(alpha = 0.7f)
                                        )
                                        if (exercise.primaryMuscles.isNotEmpty()) {
                                            Text(
                                                text = stringResource(R.string.search_exercise_muscles, exercise.primaryMuscles.joinToString(", ")),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = getContrastingTextColor(MaterialTheme.colorScheme.surfaceVariant).copy(alpha = 0.7f)
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
            // Empty confirm button to satisfy AlertDialog signature
        },
        dismissButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.provided_by),
                        style = MaterialTheme.typography.bodySmall,
                        color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                    )
                    TextButton(
                        onClick = { 
                            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/yuhonas/free-exercise-db".toUri())
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.yuhonas),
                            style = MaterialTheme.typography.bodySmall,
                            color = generateThemedColorShade(MaterialTheme.colorScheme.primary, 3)
                        )
                    }
                }
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    )
}

