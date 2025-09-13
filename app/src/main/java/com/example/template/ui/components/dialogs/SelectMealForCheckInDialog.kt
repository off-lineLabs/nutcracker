package com.example.template.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.template.R
import com.example.template.data.model.Meal
import com.example.template.ui.components.items.MealItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectMealForCheckInDialog(
    meals: List<Meal>,
    onDismiss: () -> Unit,
    onAddMeal: () -> Unit,
    onSelectMeal: (Meal) -> Unit,
    onSearchMeal: () -> Unit = {},
    onScanBarcode: () -> Unit = {}
) {
    var selectedMeal by remember { mutableStateOf<Meal?>(null) }
    var showUnifiedDialog by remember { mutableStateOf(false) }
    
    // Show unified dialog when a meal is selected
    selectedMeal?.let { meal ->
        if (showUnifiedDialog) {
            UnifiedMealDetailsDialog(
                meal = meal,
                onBack = {
                    showUnifiedDialog = false
                    selectedMeal = null
                },
                onEdit = {
                    // TODO: Implement edit functionality
                    showUnifiedDialog = false
                    selectedMeal = null
                },
                onCheckIn = {
                    onSelectMeal(meal)
                    showUnifiedDialog = false
                    selectedMeal = null
                }
            )
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.registrar_comida),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Show existing meals if any
                if (meals.isNotEmpty()) {
                    Text(
                        text = "Your Meals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(meals) { meal ->
                            MealItem(
                                meal = meal,
                                onMealClick = { 
                                    selectedMeal = meal
                                    showUnifiedDialog = true
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HorizontalDivider()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Action buttons
                Text(
                    text = "Add New Meal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Scan barcode button
                Button(
                    onClick = onScanBarcode,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_barcode))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Search meal database button
                Button(
                    onClick = onSearchMeal,
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
                    Text(stringResource(R.string.search_meal_database))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Type information button (previously "Add new meal")
                Button(
                    onClick = onAddMeal,
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
                    Text(stringResource(R.string.type_information))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}


