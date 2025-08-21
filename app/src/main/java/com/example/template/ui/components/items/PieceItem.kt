package com.example.template.ui.components.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.template.data.model.Piece

@Composable
fun PieceItem(piece: Piece, onCheckInClick: (Piece) -> Unit) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable { onCheckInClick(piece) }
			.padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Text("${piece.name}: ${piece.value}", style = MaterialTheme.typography.bodyLarge)
	}
	HorizontalDivider()
}


