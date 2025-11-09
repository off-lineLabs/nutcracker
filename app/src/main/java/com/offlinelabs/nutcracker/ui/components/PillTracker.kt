package com.offlinelabs.nutcracker.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Pill
import com.offlinelabs.nutcracker.data.model.PillCheckIn
import com.offlinelabs.nutcracker.ui.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun PillTracker(
    pill: Pill,
    pillCheckIn: PillCheckIn?,
    onPillToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPillTaken = pillCheckIn != null
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Pill name above icon (small font)
        Text(
            text = pill.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = appTextSecondaryColor(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 60.dp)
                .padding(bottom = 4.dp)
        )
        
        // Pill icon button with glow effect when taken
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .glowEffect(
                    isGlowing = isPillTaken,
                    glowColor = pillTakenColor()
                )
                .combinedClickable(
                    onClick = onPillToggle,
                    onLongClick = onLongPress
                ),
            contentAlignment = Alignment.Center
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
