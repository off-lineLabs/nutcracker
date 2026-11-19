package com.offlinelabs.nutcracker.ui.components.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.offlinelabs.nutcracker.R

/**
 * Keyboard-aware dialog shell for value-entry forms: fixed title, scrollable content and fixed
 * actions row. The window is edge-to-edge (`decorFitsSystemWindows = false`) and the card is padded
 * by the safe-drawing insets (system bars, cutout and IME), so with the keyboard open the card
 * shrinks, the content scrolls and the actions stay visible. When very little height is left
 * (landscape with the keyboard open) the whole card scrolls instead; see [FormDialogSurface].
 *
 * @param actions usually a [FormDialogActions].
 * @param dismissOnClickOutside tapping the dimmed area calls [onDismissRequest].
 */
@Composable
fun FormDialog(
    title: String,
    onDismissRequest: () -> Unit,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // The window fills the screen, so "outside the card" taps must be handled here.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dismissOnClickOutside) {
                    if (dismissOnClickOutside) detectTapGestures { currentOnDismiss() }
                }
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            // Little height left (landscape + keyboard): don't waste it on the dimmed margin.
            val verticalMargin = if (maxHeight < FormDefaults.CompactHeight) 4.dp else 16.dp
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = verticalMargin),
                contentAlignment = Alignment.Center
            ) {
                FormDialogSurface(
                    title = title,
                    actions = actions,
                    // Swallow taps on the card so they don't reach the dismiss handler.
                    modifier = modifier.pointerInput(Unit) { detectTapGestures { } },
                    content = content
                )
            }
        }
    }
}

/**
 * The card used by [FormDialog], without the window. Useful for previews and for embedding the
 * same look elsewhere. Content scrolls when it doesn't fit; title and [actions] stay fixed.
 *
 * Adapts to the space it gets:
 * - shorter than [FormDefaults.CompactHeight] (landscape with the keyboard open): title, content
 *   and actions scroll together in one column with tighter spacing, so the focused field can be
 *   scrolled into view and the actions are always reachable;
 * - narrower than [FormDefaults.CompactWidth] (small phones): tighter horizontal paddings, and the
 *   rows inside use [FormDefaults.NumberFieldWidthCompact] (see [FormDefaults.isCompactWidth]).
 *
 * Both modes keep the same composition structure (only modifiers change), so switching while the
 * keyboard opens doesn't reset the content or drop focus.
 */
@Composable
fun FormDialogSurface(
    title: String,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: FormColors = FormDefaults.colors(),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.container,
        contentColor = colors.content,
        shadowElevation = 6.dp
    ) {
        BoxWithConstraints {
            val compactHeight = maxHeight < FormDefaults.CompactHeight
            val compactWidth = maxWidth < FormDefaults.CompactWidth
            val horizontalPadding = if (compactWidth) 16.dp else 20.dp
            val cardScroll = rememberScrollState()
            val contentScroll = rememberScrollState()
            CompositionLocalProvider(LocalFormCompactWidth provides compactWidth) {
                Column(
                    modifier = Modifier
                        .then(if (compactHeight) Modifier.verticalScroll(cardScroll) else Modifier)
                        .padding(top = if (compactHeight) 12.dp else 24.dp, bottom = if (compactHeight) 4.dp else 12.dp)
                ) {
                    Text(
                        text = title,
                        style = if (compactHeight) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.headlineSmall
                        },
                        color = colors.content,
                        modifier = Modifier.padding(horizontal = horizontalPadding)
                    )
                    Spacer(Modifier.height(if (compactHeight) 8.dp else 16.dp))
                    Column(
                        modifier = Modifier
                            .then(
                                // Nested scrolling isn't needed (nor allowed with weight) when the
                                // whole card scrolls.
                                if (compactHeight) {
                                    Modifier
                                } else {
                                    Modifier.weight(1f, fill = false).verticalScroll(contentScroll)
                                }
                            )
                            .padding(horizontal = horizontalPadding),
                        content = content
                    )
                    Spacer(Modifier.height(if (compactHeight) 4.dp else 12.dp))
                    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                        actions()
                    }
                }
            }
        }
    }
}

/**
 * Rounded panel grouping related rows, with an optional small header above it.
 * Use [FormSectionDivider] between groups of rows inside.
 */
@Composable
fun FormSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    colors: FormColors = FormDefaults.colors(),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (header != null) {
            Text(
                text = header,
                style = MaterialTheme.typography.labelLarge,
                color = colors.contentSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.section,
            contentColor = colors.content,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = if (FormDefaults.isCompactWidth) 8.dp else 12.dp,
                    vertical = 8.dp
                ),
                content = content
            )
        }
    }
}

/** Thin divider between rows of a [FormSection]. */
@Composable
fun FormSectionDivider(modifier: Modifier = Modifier, colors: FormColors = FormDefaults.colors()) {
    HorizontalDivider(modifier = modifier.padding(vertical = 6.dp), color = colors.divider)
}

/**
 * Dialog actions: optional destructive delete icon on the left, then Cancel (text button) and the
 * primary confirm button on the right.
 *
 * @param confirmEnabled disable while the form is invalid.
 * @param onDelete shows a delete icon button when non-null.
 */
@Composable
fun FormDialogActions(
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    dismissText: String = stringResource(R.string.cancel),
    onDelete: (() -> Unit)? = null,
    deleteContentDescription: String = stringResource(R.string.delete),
    colors: FormColors = FormDefaults.colors()
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = deleteContentDescription,
                    tint = colors.error
                )
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = onDismiss,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
        ) {
            Text(dismissText, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(if (FormDefaults.isCompactWidth) 4.dp else 8.dp))
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            contentPadding = if (FormDefaults.isCompactWidth) {
                PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            } else {
                ButtonDefaults.ContentPadding
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                disabledContainerColor = colors.content.copy(alpha = 0.12f),
                disabledContentColor = colors.content.copy(alpha = 0.38f)
            )
        ) {
            // Wraps to two lines on narrow screens / large font scales; never cut the main action.
            Text(confirmText, maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}

/** A secondary action of [FormDialogStackedActions]. */
data class FormStackedAction(val text: String, val onClick: () -> Unit)

/**
 * Dialog actions for several confirm-type choices: a full-width primary button, full-width outlined
 * secondary buttons, then a Cancel text button. Use instead of [FormDialogActions] when one
 * confirm isn't enough.
 *
 * @param confirmEnabled applies to the primary and all [secondary] actions (not to Cancel).
 */
@Composable
fun FormDialogStackedActions(
    primaryText: String,
    onPrimary: () -> Unit,
    secondary: List<FormStackedAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    dismissText: String = stringResource(R.string.cancel),
    colors: FormColors = FormDefaults.colors()
) {
    val buttonPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    val buttonHeight = 44.dp
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(
            onClick = onPrimary,
            enabled = confirmEnabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = buttonHeight),
            contentPadding = buttonPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                disabledContainerColor = colors.content.copy(alpha = 0.12f),
                disabledContentColor = colors.content.copy(alpha = 0.38f)
            )
        ) {
            Text(primaryText, maxLines = 2, textAlign = TextAlign.Center)
        }
        secondary.forEach { action ->
            OutlinedButton(
                onClick = action.onClick,
                enabled = confirmEnabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = buttonHeight),
                contentPadding = buttonPadding,
                border = BorderStroke(
                    1.dp,
                    if (confirmEnabled) colors.accent else colors.content.copy(alpha = 0.12f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.accent,
                    disabledContentColor = colors.content.copy(alpha = 0.38f)
                )
            ) {
                Text(action.text, maxLines = 2, textAlign = TextAlign.Center)
            }
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().heightIn(min = buttonHeight),
            contentPadding = buttonPadding,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
        ) {
            Text(dismissText, maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}
