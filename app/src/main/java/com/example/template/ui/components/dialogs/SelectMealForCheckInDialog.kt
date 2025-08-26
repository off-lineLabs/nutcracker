package com.example.template.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.template.R
import com.example.template.data.model.Meal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectMealForCheckInDialog(
    meals: List<Meal>,
    onDismiss: () -> Unit,
    onAddMeal: () -> Unit,
    onSelectMeal: (Meal) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.check_in_meal),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (meals.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.no_meals_added),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        items(meals, key = { it.id }) { meal ->
                            ListItem(
                                headlineContent = { Text(meal.name) },
                                supportingContent = {
                                    Text(stringResource(R.string.meal_calories_info, meal.calories, meal.servingSize_value, meal.servingSize_unit))
                                },
                                modifier = Modifier
                                    .fillMaxWidth(),
                                leadingContent = {},
                                trailingContent = {
                                    TextButton(onClick = { onSelectMeal(meal) }) {
                                        Text(stringResource(id = R.string.check_in))
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAddMeal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.add_new_meal))
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        }
    }
}


