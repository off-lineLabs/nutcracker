package com.offlinelabs.nutcracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
        // Title
        Text(
            text = stringResource(R.string.supplement_tracker),
            style = MaterialTheme.typography.titleMedium,
            color = appTextPrimaryColor(),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
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
            
            // Add pill button (smaller, only if less than 5 pills)
            if (pills.size < 5) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onAddPill,
                        modifier = Modifier.size(40.dp)
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
                            // Overlay a "+" text in the center
                            Text(
                                text = "+",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = appTextSecondaryColor()
                            )
                        }
                    }
                }
            }
        }
    }
}

