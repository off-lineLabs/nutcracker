package com.offlinelabs.nutcracker.ui.components

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
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.PillCheckIn
import com.offlinelabs.nutcracker.ui.theme.*
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
                .glowEffect(
                    isGlowing = isPillTaken,
                    glowColor = pillTakenColor()
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_pill_24),
                contentDescription = "Pill",
                tint = if (isPillTaken) pillTakenColor() else pillNotTakenColor(),
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
                    color = pillTakenColor()
                )
            }
        }
    }
}
