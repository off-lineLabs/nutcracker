package com.example.template.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.template.data.model.FoodInfo
import com.example.template.data.model.NovaClassification
import com.example.template.data.model.GreenScore
import com.example.template.data.model.Nutriscore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodInfoDialog(
    foodInfo: FoodInfo,
    onBack: () -> Unit,
    onAddToMeals: () -> Unit
) {
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
                    text = foodInfo.name,
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
                // Food image
                foodInfo.imageUrl?.let { imageUrl ->
                    item {
                        FoodImageCard(imageUrl = imageUrl)
                    }
                }
                
                // Nutrition information
                item {
                    NutritionInfoCard(nutrition = foodInfo.nutrition)
                }
                
                // Classification and scores
                item {
                    ClassificationCard(
                        novaClassification = foodInfo.novaClassification,
                        greenScore = foodInfo.greenScore,
                        nutriscore = foodInfo.nutriscore
                    )
                }
                
                // Additional information
                item {
                    AdditionalInfoCard(foodInfo = foodInfo)
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onBack) {
                    Text("Cancel")
                }
                Button(
                    onClick = onAddToMeals,
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
                    Text("Add to My Meals")
                }
            }
        }
    )
}

@Composable
private fun FoodImageCard(imageUrl: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Food product image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(8.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun NutritionInfoCard(nutrition: com.example.template.data.model.NutritionInfo) {
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
                text = "Nutrition Facts (per 100g)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            nutrition.calories?.let { 
                NutritionRow("Calories", "${it.toInt()} kcal", Icons.Filled.LocalFireDepartment)
            }
            nutrition.fat?.let { 
                NutritionRow("Fat", "${String.format("%.1f", it)}g", Icons.Filled.OilBarrel)
            }
            nutrition.carbohydrates?.let { 
                NutritionRow("Carbs", "${String.format("%.1f", it)}g", Icons.Filled.Grain)
            }
            nutrition.proteins?.let { 
                NutritionRow("Proteins", "${String.format("%.1f", it)}g", Icons.Filled.FitnessCenter)
            }
            nutrition.sodium?.let { 
                NutritionRow("Sodium", "${String.format("%.1f", it)}mg", Icons.Filled.Water)
            }
            nutrition.fiber?.let { 
                NutritionRow("Fiber", "${String.format("%.1f", it)}g", Icons.Filled.Park)
            }
            nutrition.sugars?.let { 
                NutritionRow("Sugars", "${String.format("%.1f", it)}g", Icons.Filled.Cake)
            }
            nutrition.salt?.let { 
                NutritionRow("Salt", "${String.format("%.1f", it)}g", Icons.Filled.Water)
            }
        }
    }
}

@Composable
private fun NutritionRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ClassificationCard(
    novaClassification: NovaClassification,
    greenScore: GreenScore?,
    nutriscore: Nutriscore?
) {
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
            Text(
                text = "Classification & Scores",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Nova Classification
            ClassificationRow(
                label = "NOVA Classification",
                value = "Group ${novaClassification.group}",
                description = novaClassification.description,
                color = when (novaClassification.group) {
                    1 -> Color(0xFF4CAF50) // Green
                    2 -> Color(0xFF8BC34A) // Light Green
                    3 -> Color(0xFFFF9800) // Orange
                    4 -> Color(0xFFF44336) // Red
                    else -> Color(0xFF9E9E9E) // Gray
                }
            )
            
            // Green Score (Eco-Score)
            greenScore?.let { score ->
                ClassificationRow(
                    label = score.displayName,
                    value = score.grade,
                    description = "Environmental impact score",
                    color = when (score.grade.uppercase()) {
                        "A" -> Color(0xFF4CAF50)
                        "B" -> Color(0xFF8BC34A)
                        "C" -> Color(0xFFFFEB3B)
                        "D" -> Color(0xFFFF9800)
                        "E" -> Color(0xFFF44336)
                        else -> Color(0xFF9E9E9E)
                    }
                )
            }
            
            // Nutri-Score
            nutriscore?.let { score ->
                ClassificationRow(
                    label = score.displayName,
                    value = score.grade,
                    description = "Nutritional quality score",
                    color = when (score.grade.uppercase()) {
                        "A" -> Color(0xFF4CAF50)
                        "B" -> Color(0xFF8BC34A)
                        "C" -> Color(0xFFFFEB3B)
                        "D" -> Color(0xFFFF9800)
                        "E" -> Color(0xFFF44336)
                        else -> Color(0xFF9E9E9E)
                    }
                )
            }
        }
    }
}

@Composable
private fun ClassificationRow(
    label: String,
    value: String,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdditionalInfoCard(foodInfo: FoodInfo) {
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
                text = "Additional Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            foodInfo.brand?.let { 
                DetailRow("Brand", it)
            }
            foodInfo.categories?.let { 
                DetailRow("Categories", it)
            }
            foodInfo.quantity?.let { 
                DetailRow("Quantity", it)
            }
            foodInfo.servingSize?.let { 
                DetailRow("Serving Size", it)
            }
            foodInfo.ingredients?.let { 
                DetailRow("Ingredients", it.take(200) + if (it.length > 200) "..." else "")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 3
        )
    }
}
