package com.example.template.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.template.R

@Composable
fun ExerciseToggle(
    isEnabled: Boolean,
    onToggle: () -> Unit,
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
                .then(
                    if (isEnabled) {
                        Modifier.shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFF2196F3).copy(alpha = 0.3f),
                            spotColor = Color(0xFF2196F3).copy(alpha = 0.3f)
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_shape_up_stack_24),
                contentDescription = "Exercise Toggle",
                tint = if (isEnabled) Color(0xFF2196F3) else Color(0xFF9E9E9E), // Blue when enabled, grey when disabled
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
