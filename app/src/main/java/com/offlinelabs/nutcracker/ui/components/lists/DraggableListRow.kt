package com.offlinelabs.nutcracker.ui.components.lists

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

/**
 * A list row that can be dragged within a reorderable LazyColumn when [enabled]: long-press
 * anywhere on it, or drag the handle icon straight away. [onDragStopped] fires when it is dropped.
 */
@Composable
fun LazyItemScope.DraggableListRow(
    state: ReorderableLazyListState,
    key: String,
    enabled: Boolean,
    handleDescription: String,
    onDragStopped: () -> Unit,
    content: @Composable () -> Unit
) {
    ReorderableItem(state, key = key, enabled = enabled) { isDragging ->
        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "dragElevation")
        val haptics = LocalHapticFeedback.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation, RoundedCornerShape(12.dp))
                .then(
                    if (enabled) {
                        Modifier.longPressDraggableHandle(
                            onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                            onDragStopped = { onDragStopped() }
                        )
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) { content() }
            if (enabled) {
                Icon(
                    imageVector = Icons.Filled.DragIndicator,
                    contentDescription = handleDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Slim handle (the whole row also drags on long-press, so it needn't be a big target)
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .draggableHandle(
                            onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                            onDragStopped = { onDragStopped() }
                        )
                        .padding(vertical = 12.dp)
                        .size(width = 16.dp, height = 20.dp)
                )
            }
        }
    }
}
