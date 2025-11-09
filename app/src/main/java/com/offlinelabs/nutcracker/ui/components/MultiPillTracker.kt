package com.offlinelabs.nutcracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Pill
import com.offlinelabs.nutcracker.data.model.PillCheckIn
import com.offlinelabs.nutcracker.ui.theme.*

@Composable
fun MultiPillTracker(
    pills: List<Pill>,
    pillCheckIns: Map<Long, PillCheckIn>,
    onPillToggle: (Long) -> Unit,
    onPillLongPress: (Pill) -> Unit,
    onAddPill: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        // Title with Add button on the right (like Nutrient Details)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.supplement_tracker),
                style = MaterialTheme.typography.titleMedium,
                color = appTextPrimaryColor(),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            // Add pill button on the right (only if less than 5 pills)
            if (pills.size < 5) {
                IconButton(
                    onClick = onAddPill,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.CenterEnd)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pill_24),
                            contentDescription = "Add pill",
                            tint = appTextSecondaryColor(),
                            modifier = Modifier.size(20.dp)
                        )
                        // Overlay a "+" text with shadow for visibility, offset to separate from icon
                        Text(
                            text = "+",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.8f),
                                    offset = Offset(1f, 1f),
                                    blurRadius = 2f
                                )
                            ),
                            modifier = Modifier.offset(x = 6.dp, y = (-2).dp)
                        )
                    }
                }
            }
        }
        
        // Pills row with centering
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Display up to 5 pills
            pills.take(5).forEach { pill ->
                val checkIn = pillCheckIns[pill.id]
                
                PillTracker(
                    pill = pill,
                    pillCheckIn = checkIn,
                    onPillToggle = { onPillToggle(pill.id) },
                    onLongPress = { onPillLongPress(pill) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

