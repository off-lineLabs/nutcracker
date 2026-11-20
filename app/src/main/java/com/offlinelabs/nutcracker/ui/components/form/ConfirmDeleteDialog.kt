package com.offlinelabs.nutcracker.ui.components.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme

/**
 * Shared confirmation for every delete in the app, in the form-kit style.
 * Cancel (or tapping outside / Back) calls [onDismiss]; the destructive button calls [onConfirm].
 * The host closes the dialog (clears its pending state) in both callbacks.
 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(R.string.delete)
) {
    val colors = FormDefaults.colors()
    FormDialog(
        title = title,
        onDismissRequest = onDismiss,
        actions = {
            ConfirmDeleteActions(
                confirmLabel = confirmLabel,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                colors = colors
            )
        }
    ) {
        ConfirmDeleteMessage(message, colors)
    }
}

@Composable
private fun ConfirmDeleteMessage(message: String, colors: FormColors) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = colors.contentSecondary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun ConfirmDeleteActions(
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    colors: FormColors
) {
    // 0.179 is the WCAG crossover where dark text beats white; the dark theme's light red needs dark text
    val onError = if (colors.error.luminance() > 0.179f) Color(0xFF0B1426) else Color.White
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onDismiss,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
        ) {
            Text(stringResource(R.string.cancel), maxLines = 2, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.width(if (FormDefaults.isCompactWidth) 4.dp else 8.dp))
        Button(
            onClick = onConfirm,
            contentPadding = if (FormDefaults.isCompactWidth) {
                PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            } else {
                ButtonDefaults.ContentPadding
            },
            colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = onError)
        ) {
            Text(confirmLabel, maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ConfirmDeletePreviewContent() {
    val colors = FormDefaults.colors()
    FormDialogSurface(
        title = stringResource(R.string.confirm_delete_meal_title),
        actions = { ConfirmDeleteActions(stringResource(R.string.delete), {}, {}, colors) },
        colors = colors
    ) {
        ConfirmDeleteMessage(stringResource(R.string.confirm_delete_meal_message, "Granola"), colors)
    }
}

@Preview(name = "Confirm delete - light", widthDp = 360, heightDp = 360, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun ConfirmDeleteLightPreview() {
    FoodLogTheme(darkTheme = false) { ConfirmDeletePreviewContent() }
}

@Preview(name = "Confirm delete - dark, pt-BR", widthDp = 360, heightDp = 360, showBackground = true, backgroundColor = 0xFF0B1426, locale = "pt-rBR")
@Composable
private fun ConfirmDeleteDarkPreview() {
    FoodLogTheme(darkTheme = true) { ConfirmDeletePreviewContent() }
}
