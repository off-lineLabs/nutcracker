package com.example.template.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Extension function to add a glowing effect to a modifier.
 * 
 * @param isGlowing Whether the glow effect should be applied
 * @param glowColor The color of the glow effect
 * @param elevation The elevation of the shadow (default: 12.dp)
 * @param shape The shape of the shadow (default: CircleShape)
 * @return Modifier with or without the glow effect
 */
@Composable
fun Modifier.glowEffect(
    isGlowing: Boolean,
    glowColor: Color,
    elevation: Dp = 12.dp,
    shape: Shape = CircleShape
): Modifier {
    return if (isGlowing) {
        this.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = glowColor.copy(alpha = 0.5f),
            spotColor = glowColor.copy(alpha = 0.6f)
        )
    } else {
        this
    }
}
