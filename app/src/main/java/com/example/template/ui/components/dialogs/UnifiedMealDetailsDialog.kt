package com.example.template.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalUriHandler
import com.example.template.data.model.Meal
import com.example.template.data.model.NovaClassification
import com.example.template.data.model.GreenScore
import com.example.template.data.model.Nutriscore

/**
 * Determines the appropriate text color (black or white) based on the background color's luminance.
 * Uses a threshold of 0.5 - if the background is lighter than this, use black text, otherwise white.
 */
private fun getContrastingTextColor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) {
        Color.Black
    } else {
        Color.White
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedMealDetailsDialog(
    meal: Meal,
    onBack: () -> Unit,
    onEdit: (Meal) -> Unit,
    onCheckIn: () -> Unit
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = meal.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    meal.brand?.let { brand ->
                        Text(
                            text = brand,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                IconButton(onClick = { onEdit(meal) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Meal")
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
                // Food image
                meal.localImagePath?.let { imagePath ->
                    item {
                        MealImageCard(imagePath = imagePath)
                    }
                } ?: meal.imageUrl?.let { imageUrl ->
                    item {
                        MealImageCard(imageUrl = imageUrl)
                    }
                }
                
                // Basic nutrition information
                item {
                    BasicNutritionCard(meal = meal)
                }
                
                // Extended nutrition information (if available from Open Food Facts)
                if (hasExtendedNutrition(meal)) {
                    item {
                        ExtendedNutritionCard(meal = meal)
                    }
                }
                
                // Classification and scores (if available)
                if (hasClassifications(meal)) {
                    item {
                        ClassificationCard(
                            novaClassification = meal.novaClassification,
                            greenScore = meal.greenScore,
                            nutriscore = meal.nutriscore
                        )
                    }
                }
                
                // Ingredients (if available and not empty)
                meal.ingredients?.let { ingredients ->
                    if (ingredients.isNotBlank()) {
                        item {
                            IngredientsCard(ingredients = ingredients)
                        }
                    }
                }
                
                
                // Attribution (only for Open Food Facts sourced meals)
                if (meal.source != "manual") {
                    item {
                        AttributionCard()
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onBack) {
                    Text("Close")
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
                    Text("Check-in Meal")
                }
            }
        }
    )
}

@Composable
private fun MealImageCard(imagePath: String? = null, imageUrl: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = imagePath ?: imageUrl,
                contentDescription = "Food product image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun BasicNutritionCard(meal: Meal) {
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
                text = "Nutrition Facts (per ${meal.servingSize_value.toInt()}${meal.servingSize_unit.abbreviation})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            NutritionRow("Calories", "${meal.calories} kcal", Icons.Filled.LocalFireDepartment)
            NutritionRow("Carbs", "${String.format("%.1f", meal.carbohydrates_g)}g", Icons.Filled.Grain)
            NutritionRow("Proteins", "${String.format("%.1f", meal.protein_g)}g", Icons.Filled.FitnessCenter)
            NutritionRow("Fat", "${String.format("%.1f", meal.fat_g)}g", Icons.Filled.OilBarrel)
            NutritionRow("Fiber", "${String.format("%.1f", meal.fiber_g)}g", Icons.Filled.Park)
            NutritionRow("Sodium", "${String.format("%.1f", meal.sodium_mg)}mg", Icons.Filled.Water)
        }
    }
}

@Composable
private fun ExtendedNutritionCard(meal: Meal) {
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
                text = "Additional Nutrition",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            meal.saturatedFat_g?.let { 
                NutritionRow("Saturated Fat", "${String.format("%.1f", it)}g", Icons.Filled.OilBarrel)
            }
            meal.sugars_g?.let { 
                NutritionRow("Sugars", "${String.format("%.1f", it)}g", Icons.Filled.Cake)
            }
            meal.cholesterol_mg?.let { 
                NutritionRow("Cholesterol", "${String.format("%.1f", it)}mg", Icons.Filled.Favorite)
            }
            meal.vitaminC_mg?.let { 
                NutritionRow("Vitamin C", "${String.format("%.1f", it)}mg", Icons.Filled.LocalPharmacy)
            }
            meal.calcium_mg?.let { 
                NutritionRow("Calcium", "${String.format("%.1f", it)}mg", Icons.Filled.LocalDrink)
            }
            meal.iron_mg?.let { 
                NutritionRow("Iron", "${String.format("%.1f", it)}mg", Icons.Filled.Build)
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
    novaClassification: NovaClassification?,
    greenScore: GreenScore?,
    nutriscore: Nutriscore?
) {
    // Check if we have any valid classifications to show
    val hasValidClassifications = novaClassification?.group?.let { it > 0 } == true || greenScore != null || nutriscore != null
    
    if (!hasValidClassifications) {
        return // Don't show the card if no valid classifications
    }
    
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
            
            // Nova Classification - only show if group > 0 (not unknown)
            novaClassification?.let { classification ->
                if (classification.group > 0) {
                    ClassificationRow(
                        label = "NOVA Classification",
                        value = classification.group.toString(),
                        description = classification.description,
                        color = when (classification.group) {
                            1 -> Color(0xFF4CAF50) // Green
                            2 -> Color(0xFF8BC34A) // Light Green
                            3 -> Color(0xFFFF9800) // Orange
                            4 -> Color(0xFFF44336) // Red
                            else -> Color(0xFF9E9E9E) // Gray
                        }
                    )
                }
            }
            
            // Green Score (Eco-Score) - only show if not null
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
            
            // Nutri-Score - only show if not null
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
                color = getContrastingTextColor(color),
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
private fun IngredientsCard(ingredients: String) {
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
                text = "Ingredients",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = parseOpenFoodFactsFormatting(ingredients),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Simple parser for OpenFoodFacts formatting:
 * - _text_ becomes italic
 * - __text__ becomes bold
 * - Removes other markdown-like formatting
 */
private fun parseOpenFoodFactsFormatting(text: String) = buildAnnotatedString {
    val boldPattern = "__([^_]+)__".toRegex()
    val italicPattern = "_([^_]+)_".toRegex()
    
    var processedText = text
    val boldMatches = boldPattern.findAll(text).toList()
    val italicMatches = italicPattern.findAll(text).toList()
    
    // Remove formatting markers
    processedText = processedText.replace(boldPattern, "$1")
    processedText = processedText.replace(italicPattern, "$1")
    
    // Build annotated string with styles
    var currentIndex = 0
    val allMatches = (boldMatches + italicMatches).sortedBy { it.range.first }
    
    for (match in allMatches) {
        // Add text before the match
        if (currentIndex < match.range.first) {
            append(processedText.substring(currentIndex, match.range.first))
        }
        
        // Add styled text
        val content = match.groupValues[1]
        val isBold = match.value.startsWith("__")
        
        withStyle(
            style = SpanStyle(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (isBold) FontStyle.Normal else FontStyle.Italic
            )
        ) {
            append(content)
        }
        
        currentIndex = match.range.last + 1
    }
    
    // Add remaining text
    if (currentIndex < processedText.length) {
        append(processedText.substring(currentIndex))
    }
}


@Composable
private fun AttributionCard() {
    val uriHandler = LocalUriHandler.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Provided by ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "© Open Food Facts contributors",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                uriHandler.openUri("https://world.openfoodfacts.org/")
            }
        )
    }
}

// Helper functions
private fun hasExtendedNutrition(meal: Meal): Boolean {
    return meal.saturatedFat_g != null || 
           meal.sugars_g != null || 
           meal.cholesterol_mg != null || 
           meal.vitaminC_mg != null || 
           meal.calcium_mg != null || 
           meal.iron_mg != null
}

private fun hasClassifications(meal: Meal): Boolean {
    return (meal.novaClassification?.group?.let { it > 0 } == true) || 
           meal.greenScore != null || 
           meal.nutriscore != null
}
