package com.offlinelabs.nutcracker.ui.components.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.RecipeIngredient
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor

@Composable
fun RecipeIngredientItem(
    ingredient: RecipeIngredient,
    meal: Meal?,
    onEdit: (RecipeIngredient) -> Unit,
    onDelete: (RecipeIngredient) -> Unit
) {
    val cardBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val contrastingTextColor = getContrastingTextColor(cardBackgroundColor)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = meal?.name ?: "Unknown meal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = contrastingTextColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${ingredient.quantity} ${ingredient.unit.abbreviation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = contrastingTextColor.copy(alpha = 0.7f)
                )
                meal?.let { m ->
                    val multiplier = ingredient.quantity / m.servingSize_value
                    val calories = (m.calories * multiplier).toInt()
                    Text(
                        text = "$calories kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = contrastingTextColor.copy(alpha = 0.6f)
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onEdit(ingredient) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        tint = contrastingTextColor.copy(alpha = 0.7f)
                    )
                }
                IconButton(
                    onClick = { onDelete(ingredient) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
