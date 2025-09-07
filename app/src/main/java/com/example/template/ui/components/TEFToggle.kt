package com.example.template.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.template.R
import com.example.template.ui.theme.*

@Composable
fun TEFToggle(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
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
        
        // Reserve same space as pill tracker to maintain alignment
        Box(
            modifier = Modifier.height(20.dp)
        )
    }
}
