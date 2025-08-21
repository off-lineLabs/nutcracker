package com.example.template.ui.components.items

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.template.data.model.CheckIn

@Composable
fun CheckInItem(checkIn: CheckIn) {
	Text(
		text = "${checkIn.timestamp}: ${checkIn.percentage}% of ${checkIn.pieceName}",
		modifier = Modifier.padding(vertical = 8.dp),
		style = MaterialTheme.typography.bodyLarge
	)
}


