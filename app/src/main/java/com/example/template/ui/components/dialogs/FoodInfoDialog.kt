package com.example.template.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalUriHandler
import com.example.template.data.model.FoodInfo
import com.example.template.data.model.NovaClassification
import com.example.template.data.model.GreenScore
import com.example.template.data.model.Nutriscore
import androidx.compose.ui.res.stringResource
import java.util.Locale
import com.example.template.R
import com.example.template.ui.theme.getContrastingTextColor
import com.example.template.ui.theme.getContrastingIconColor
import com.example.template.ui.theme.generateThemedColorShade


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodInfoDialog(
    foodInfo: FoodInfo,
    onBack: () -> Unit,
    onSelect: () -> Unit
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
                        text = foodInfo.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    foodInfo.brand?.let { brand ->
                        Text(
                            text = brand,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
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
                
                // Attribution
                item {
                    AttributionCard()
                }
                
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.select))
                }
            }
        }
    )
}

@Composable
private fun FoodImageCard(imageUrl: String) {
    HeroFoodImage(
        imageUrl = imageUrl,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun HeroFoodImage(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // Background blurred image
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 20.dp),
            contentScale = ContentScale.Crop
        )
        
        // Gradient overlay for better contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
        )
        
        // Main image with original aspect ratio preserved and levitating effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Food product image",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.2f),
                        spotColor = Color.Black.copy(alpha = 0.1f)
                    ),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun NutritionInfoCard(nutrition: com.example.template.data.model.NutritionInfo) {
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
                text = "Nutrition Facts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contrastingTextColor
            )
            Text(
                text = "per 100g/100ml",
                style = MaterialTheme.typography.bodySmall,
                color = contrastingTextColor
            )
            
            nutrition.calories?.let { 
                NutritionRow("Calories", "${it.toInt()} kcal", Icons.Filled.LocalFireDepartment, contrastingTextColor, cardBackgroundColor)
            }
            nutrition.fat?.let { 
                NutritionRow("Fat", "${String.format(Locale.US, "%.1f", it)}g", Icons.Filled.OilBarrel, contrastingTextColor, cardBackgroundColor)
            }
            nutrition.carbohydrates?.let { 
                NutritionRow("Carbs", "${String.format(Locale.US, "%.1f", it)}g", Icons.Filled.Grain, contrastingTextColor, cardBackgroundColor)
            }
            nutrition.proteins?.let { 
                NutritionRow("Proteins", "${String.format(Locale.US, "%.1f", it)}g", Icons.Filled.FitnessCenter, contrastingTextColor, cardBackgroundColor)
            }
            nutrition.sodium?.let { 
                NutritionRow("Sodium", "${String.format(Locale.US, "%.1f", it)}mg", Icons.Filled.Water, contrastingTextColor, cardBackgroundColor)
            }
            nutrition.fiber?.let { 
                NutritionRow("Fiber", "${String.format(Locale.US, "%.1f", it)}g", Icons.Filled.Park, contrastingTextColor, cardBackgroundColor)
            }
            nutrition.sugars?.let { 
                NutritionRow("Sugars", "${String.format(Locale.US, "%.1f", it)}g", Icons.Filled.Cake, contrastingTextColor, cardBackgroundColor)
            }
            nutrition.saturatedFat?.let { 
                NutritionRow("Saturated Fat", "${String.format(Locale.US, "%.1f", it)}g", Icons.Filled.OilBarrel, contrastingTextColor, cardBackgroundColor)
            }
            nutrition.cholesterol?.let { 
                NutritionRow("Cholesterol", "${String.format(Locale.US, "%.1f", it)}mg", Icons.Filled.Favorite, contrastingTextColor, cardBackgroundColor)
            }
            nutrition.vitaminC?.let { 
                NutritionRow("Vitamin C", "${String.format(Locale.US, "%.1f", it)}mg", Icons.Filled.LocalPharmacy, contrastingTextColor, cardBackgroundColor)
            }
            nutrition.calcium?.let { 
                NutritionRow("Calcium", "${String.format(Locale.US, "%.1f", it)}mg", Icons.Filled.LocalDrink, contrastingTextColor, cardBackgroundColor)
            }
            nutrition.iron?.let { 
                NutritionRow("Iron", "${String.format(Locale.US, "%.1f", it)}mg", Icons.Filled.Build, contrastingTextColor, cardBackgroundColor)
            }
        }
    }
}

@Composable
private fun NutritionRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val iconColor = getContrastingIconColor(backgroundColor)
    
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
                tint = iconColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun ClassificationCard(
    novaClassification: NovaClassification,
    greenScore: GreenScore?,
    nutriscore: Nutriscore?
) {
    // Check if we have any valid classifications to show
    val hasValidClassifications = novaClassification.group > 0 || greenScore != null || nutriscore != null
    
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Classification & Scores",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Nova Classification - only show if group > 0 (not unknown)
            if (novaClassification.group > 0) {
                ClassificationRow(
                    label = "NOVA Classification",
                    value = novaClassification.group.toString(),
                    description = novaClassification.description,
                    color = when (novaClassification.group) {
                        1 -> Color(0xFF4CAF50) // Green
                        2 -> Color(0xFF8BC34A) // Light Green
                        3 -> Color(0xFFFF9800) // Orange
                        4 -> Color(0xFFF44336) // Red
                        else -> Color(0xFF9E9E9E) // Gray
                    }
                )
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
private fun AttributionCard() {
    val uriHandler = LocalUriHandler.current
    val contrastingTextColor = getContrastingTextColor(MaterialTheme.colorScheme.surface)
    val linkColor = generateThemedColorShade(MaterialTheme.colorScheme.primary, 3)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.provided_by),
            style = MaterialTheme.typography.bodySmall,
            color = contrastingTextColor
        )
        Text(
            text = "© Open Food Facts contributors",
            style = MaterialTheme.typography.bodySmall,
            color = linkColor,
            modifier = Modifier.clickable {
                uriHandler.openUri("https://world.openfoodfacts.org/")
            }
        )
    }
}


