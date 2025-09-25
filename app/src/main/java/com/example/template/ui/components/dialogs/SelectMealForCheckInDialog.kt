package com.example.template.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
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
import com.example.template.ui.theme.getContrastingTextColor
import com.example.template.ui.theme.brandAccentShade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectMealForCheckInDialog(
    meals: List<Meal>,
    onDismiss: () -> Unit,
    onAddMeal: () -> Unit,
    onSelectMeal: (Meal) -> Unit,
    onEditMeal: (Meal) -> Unit = {},
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
                onEdit = { mealToEdit ->
                    showUnifiedDialog = false
                    selectedMeal = null
                    onEditMeal(mealToEdit)
                },
                onCheckIn = {
                    onSelectMeal(meal)
                    showUnifiedDialog = false
                    selectedMeal = null
                }
            )
        }
    }
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(8.dp),
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
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                            tint = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                        )
                    }
                    
                    // Centered title
                    Text(
                        text = stringResource(R.string.registrar_comida),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                // Action buttons at the top
                // Scan barcode button
                Button(
                    onClick = onScanBarcode,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandAccentShade(0)
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
                        containerColor = brandAccentShade(1)
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
                
                // Type information button
                Button(
                    onClick = onAddMeal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandAccentShade(2)
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

                // Existing meals list
                if (meals.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_meals_added),
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
                }
            }
        }
    }
}


