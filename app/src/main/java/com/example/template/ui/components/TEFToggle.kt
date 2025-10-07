package com.example.template.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
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
fun TEFToggle(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    tefCalories: Double = 0.0
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // TEF toggle icon button with glow effect when enabled
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .glowEffect(
                    isGlowing = isEnabled,
                    glowColor = tefEnabledColor()
                )
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "TEF Toggle",
                tint = if (isEnabled) tefEnabledColor() else tefDisabledColor(),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Always reserve space for calories display to prevent layout shift
        Box(
            modifier = Modifier.height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Show TEF calories when there are any
            if (tefCalories > 0) {
                val textColor = if (isEnabled) tefEnabledColor() else tefDisabledColor()
                val backgroundColor = if (isEnabled) {
                    tefEnabledColor().copy(alpha = 0.15f)
                } else {
                    tefDisabledColor().copy(alpha = 0.1f)
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
                        text = "+${tefCalories.toInt()} kcal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }
        }
    }
}
