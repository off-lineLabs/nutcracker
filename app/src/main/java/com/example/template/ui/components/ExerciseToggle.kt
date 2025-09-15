package com.example.template.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier,
    exerciseCalories: Double = 0.0
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
            modifier = Modifier.height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Show exercise calories when there are any
            if (exerciseCalories > 0) {
                val textColor = if (isEnabled) exerciseEnabledColor() else exerciseDisabledColor()
                val backgroundColor = if (isEnabled) {
                    exerciseEnabledColor().copy(alpha = 0.15f)
                } else {
                    exerciseDisabledColor().copy(alpha = 0.1f)
                }
                
                Box(
                    modifier = Modifier
                        .background(
                            color = backgroundColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+${exerciseCalories.toInt()} kcal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }
        }
    }
}
