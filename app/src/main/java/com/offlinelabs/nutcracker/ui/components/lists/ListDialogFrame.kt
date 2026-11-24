package com.offlinelabs.nutcracker.ui.components.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults

/**
 * Dialog card with a title, a scrolling (reorderable-friendly) list and an actions row; the list
 * takes the free height, up to most of the screen.
 */
@Composable
fun ListDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    listState: LazyListState,
    actions: @Composable () -> Unit,
    content: LazyListScope.() -> Unit
) {
    val colors = FormDefaults.colors()
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.88f).dp
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.container,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = maxHeight)
                .imePadding()
        ) {
            Column(modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.content,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
                Box(modifier = Modifier.padding(horizontal = 16.dp)) { actions() }
            }
        }
    }
}

/** A list card: [title] with an optional smaller [subtitle] and a trailing slot. */
@Composable
fun ListCard(
    title: String,
    subtitle: String?,
    colors: FormColors,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Surface(shape = RoundedCornerShape(12.dp), color = colors.section, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp).heightIn(min = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.contentSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            trailing()
        }
    }
}

/** Secondary explanatory text inside a list dialog. */
@Composable
fun ListHint(text: String, colors: FormColors) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.contentSecondary)
}
