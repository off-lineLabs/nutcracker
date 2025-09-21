package com.example.template.ui.components.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.template.data.model.Meal
import com.example.template.ui.theme.getContrastingTextColor

@Composable
fun MealItem(meal: Meal, onMealClick: (Meal) -> Unit) {
    val cardBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val contrastingTextColor = getContrastingTextColor(cardBackgroundColor)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMealClick(meal) }
            .padding(vertical = 4.dp, horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // First row: Meal name only
            Text(
                text = meal.name,
                style = MaterialTheme.typography.bodyMedium, // Slightly reduced from bodyLarge
                fontWeight = FontWeight.Medium,
                color = contrastingTextColor,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Second row: Picture | Brand, Source | Kcal, portion size, Nova
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Food image
                meal.localImagePath?.let { imagePath ->
                    AsyncImage(
                        model = imagePath,
                        contentDescription = "Food image",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } ?: meal.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Food image",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                // Brand and source info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Serving size (moved from nutrition column)
                    Text(
                        text = "${meal.servingSize_value.toInt()}${meal.servingSize_unit.abbreviation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = contrastingTextColor.copy(alpha = 0.7f)
                    )
                    
                    // Brand if available
                    meal.brand?.let { brand ->
                        Text(
                            text = brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = contrastingTextColor.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Source indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        when (meal.source) {
                            "barcode" -> {
                                Icon(
                                    Icons.Filled.QrCodeScanner,
                                    contentDescription = "Scanned",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Scanned",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contrastingTextColor
                                )
                            }
                            "search" -> {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "Searched",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Searched",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contrastingTextColor
                                )
                            }
                            else -> {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Manual",
                                    modifier = Modifier.size(12.dp),
                                    tint = contrastingTextColor.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "Manual",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contrastingTextColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
                
                // Nutrition info and classification
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Calories
                    Text(
                        text = "${meal.calories} kcal",
                        style = MaterialTheme.typography.bodyMedium, // Same size as title
                        fontWeight = FontWeight.SemiBold,
                        color = contrastingTextColor
                    )
                    
                    // NOVA classification if available
                    meal.novaClassification?.let { classification ->
                        val (color, icon) = when (classification.group) {
                            1 -> Color(0xFF4CAF50) to Icons.Filled.Eco
                            2 -> Color(0xFF8BC34A) to Icons.Filled.Nature
                            3 -> Color(0xFFFF9800) to Icons.Filled.Warning
                            4 -> Color(0xFFF44336) to Icons.Filled.Error
                            else -> Color(0xFF9E9E9E) to Icons.AutoMirrored.Filled.Help
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                icon,
                                contentDescription = "NOVA ${classification.group}",
                                modifier = Modifier.size(10.dp),
                                tint = color
                            )
                            Text(
                                text = "NOVA ${classification.group}",
                                style = MaterialTheme.typography.labelSmall,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}
