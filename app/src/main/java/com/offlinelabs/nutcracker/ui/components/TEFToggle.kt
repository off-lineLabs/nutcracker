package com.offlinelabs.nutcracker.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.theme.*

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
                contentDescription = stringResource(R.string.tef_bonus_toggle),
                tint = if (isEnabled) tefEnabledColor() else tefDisabledColor(),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Always reserve space for calories display to prevent layout shift
        Box(
            modifier = Modifier.height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Show TEF calories when there are any (handles both positive and negative)
            if (tefCalories != 0.0) {
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
                        text = stringResource(
                            R.string.kcal_value_format,
                            "${if (tefCalories >= 0) "+" else ""}${tefCalories.toInt()}"
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }
        }
    }
}
