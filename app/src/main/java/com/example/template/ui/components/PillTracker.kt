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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.template.R
import com.example.template.data.model.PillCheckIn
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun PillTracker(
    isPillTaken: Boolean,
    pillCheckIn: PillCheckIn?,
    onPillToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Pill icon button with glow effect when taken
        IconButton(
            onClick = onPillToggle,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .then(
                    if (isPillTaken) {
                        Modifier.shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                            spotColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_pill_24),
                contentDescription = "Pill",
                tint = if (isPillTaken) Color(0xFF4CAF50) else Color(0xFF9E9E9E), // Mint green when taken, grey when not
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Always reserve space for timestamp to prevent layout shift
        Box(
            modifier = Modifier.height(20.dp),
            contentAlignment = Alignment.Center
        ) {
            // Show timestamp when pill is taken
            if (isPillTaken && pillCheckIn != null) {
                Text(
                    text = pillCheckIn.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}
