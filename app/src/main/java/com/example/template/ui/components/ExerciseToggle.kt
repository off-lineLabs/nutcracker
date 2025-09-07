package com.example.template.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.template.R
import com.example.template.ui.theme.*
import java.util.Locale

@Composable
fun ExerciseToggle(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    exerciseCalories: Double = 0.0,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Exercise toggle icon button with glow effect when enabled
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .glowEffect(
                    isGlowing = isEnabled,
                    glowColor = exerciseEnabledColor()
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_shape_up_stack_24),
                contentDescription = "Exercise Toggle",
                tint = if (isEnabled) exerciseEnabledColor() else exerciseDisabledColor(),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Always reserve space for calories display to prevent layout shift
        Box(
            modifier = Modifier.height(20.dp),
            contentAlignment = Alignment.Center
        ) {
            // Show exercise calories when there are any
            if (exerciseCalories > 0) {
                Text(
                    text = "${exerciseCalories.toInt()} kcal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isEnabled) exerciseEnabledColor() else exerciseDisabledColor()
                )
            }
        }
    }
}
