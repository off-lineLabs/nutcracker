package com.offlinelabs.nutcracker.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.offlinelabs.nutcracker.data.import.ImportProgress
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.import.ErrorSeverity
import com.offlinelabs.nutcracker.data.import.ImportError
import com.offlinelabs.nutcracker.data.import.ImportMode
import com.offlinelabs.nutcracker.data.import.ImportResult
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.form.FormDialog
import com.offlinelabs.nutcracker.ui.components.form.FormDialogSurface
import com.offlinelabs.nutcracker.ui.components.form.FormSection
import com.offlinelabs.nutcracker.ui.components.form.FormSectionDivider
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme
import java.util.Locale

private const val MAX_LISTED_ERRORS = 50
private const val MAX_LISTED_WARNINGS = 3

/** Non-dismissable progress dialog shown while a backup is being imported. */
@Composable
internal fun ImportProgressDialog(progress: ImportProgress) {
    FormDialog(
        title = stringResource(R.string.importing_database),
        onDismissRequest = { /* Cannot dismiss during import */ },
        dismissOnClickOutside = false,
        actions = {}
    ) {
        ImportProgressBody(progress)
    }
}

@Composable
private fun ImportProgressBody(progress: ImportProgress, colors: FormColors = FormDefaults.colors()) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = stringResource(R.string.processing_format, progress.currentTable),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.content
        )
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress.overallProgress / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = colors.accent,
            trackColor = colors.content.copy(alpha = 0.16f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.progress_percentage_format, progress.overallProgress),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.contentSecondary
        )
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.records_processed_format, progress.recordsProcessed),
                style = MaterialTheme.typography.bodySmall,
                color = colors.contentSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.records_imported_format, progress.recordsImported),
                style = MaterialTheme.typography.bodySmall,
                color = colors.contentSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Asks whether a picked backup should be merged into, or replace, the current data. */
@Composable
internal fun ImportModeDialog(
    onSelect: (ImportMode) -> Unit,
    onDismiss: () -> Unit
) {
    FormDialog(
        title = stringResource(R.string.import_mode_dialog_title),
        onDismissRequest = onDismiss,
        actions = { ImportModeActions(onSelect = onSelect, onDismiss = onDismiss) }
    ) {
        ImportModeBody()
    }
}

@Composable
private fun ImportModeBody(colors: FormColors = FormDefaults.colors()) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormSection(colors = colors) {
            ModeOption(
                title = stringResource(R.string.import_mode_merge_title),
                description = stringResource(R.string.import_mode_merge_desc),
                titleColor = colors.content,
                colors = colors
            )
        }
        FormSection(colors = colors) {
            ModeOption(
                title = stringResource(R.string.import_mode_replace_title),
                description = stringResource(R.string.import_mode_replace_desc),
                titleColor = colors.error,
                colors = colors
            )
        }
    }
}

@Composable
private fun ModeOption(title: String, description: String, titleColor: androidx.compose.ui.graphics.Color, colors: FormColors) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = titleColor)
        Spacer(Modifier.height(4.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.contentSecondary)
    }
}

@Composable
private fun ImportModeActions(
    onSelect: (ImportMode) -> Unit,
    onDismiss: () -> Unit,
    colors: FormColors = FormDefaults.colors()
) {
    val padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(
            onClick = { onSelect(ImportMode.MERGE) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            contentPadding = padding,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)
        ) {
            Text(stringResource(R.string.import_mode_merge_action), maxLines = 2, textAlign = TextAlign.Center)
        }
        OutlinedButton(
            onClick = { onSelect(ImportMode.REPLACE) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            contentPadding = padding,
            border = BorderStroke(1.dp, colors.error),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error)
        ) {
            Text(stringResource(R.string.import_mode_replace_action), maxLines = 2, textAlign = TextAlign.Center)
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            contentPadding = padding,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
        ) {
            Text(stringResource(R.string.cancel), maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}

/** Dialog showing the outcome of an import (or why it failed). */
@Composable
internal fun ImportResultDialog(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    FormDialog(
        title = stringResource(if (result.isSuccess) R.string.import_completed else R.string.import_failed),
        onDismissRequest = onDismiss,
        actions = { ImportResultActions(onDismiss) }
    ) {
        ImportResultBody(result)
    }
}

@Composable
private fun ImportResultActions(onClose: () -> Unit, colors: FormColors = FormDefaults.colors()) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(
            onClick = onClose,
            modifier = Modifier.heightIn(min = 44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)
        ) {
            Text(stringResource(R.string.close), maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ImportResultBody(result: ImportResult, colors: FormColors = FormDefaults.colors()) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // A failure before anything was processed has nothing to summarize, only the reason.
        if (result.isSuccess || result.totalRecordsProcessed > 0) {
            FormSection(header = stringResource(R.string.import_summary), colors = colors) {
                SummaryRow(stringResource(R.string.import_total_processed_label), "${result.totalRecordsProcessed}", colors)
                FormSectionDivider(colors = colors)
                SummaryRow(stringResource(R.string.import_successfully_imported_label), "${result.recordsImported}", colors)
                FormSectionDivider(colors = colors)
                SummaryRow(stringResource(R.string.import_skipped_label), "${result.recordsSkipped}", colors)
                FormSectionDivider(colors = colors)
                SummaryRow(stringResource(R.string.import_failed_label), "${result.recordsFailed}", colors)
                FormSectionDivider(colors = colors)
                SummaryRow(
                    stringResource(R.string.import_success_rate_label),
                    stringResource(
                        R.string.percentage_value_format,
                        String.format(Locale.getDefault(), "%.1f", result.successRate)
                    ),
                    colors
                )
                FormSectionDivider(colors = colors)
                SummaryRow(
                    stringResource(R.string.import_duration_label),
                    stringResource(R.string.duration_ms_format, result.importDuration),
                    colors
                )
            }
        }

        if (result.errors.isNotEmpty()) {
            FormSection(colors = colors) {
                Text(
                    text = stringResource(R.string.errors_count_format, result.errors.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.error
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    result.errors.take(MAX_LISTED_ERRORS).forEach { error ->
                        Text(
                            text = if (result.isSuccess || result.totalRecordsProcessed > 0) {
                                stringResource(R.string.error_item_format, error.tableName, error.rowNumber, error.errorMessage)
                            } else {
                                error.errorMessage
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error
                        )
                    }
                    if (result.errors.size > MAX_LISTED_ERRORS) {
                        Text(
                            text = stringResource(R.string.more_errors_format, result.errors.size - MAX_LISTED_ERRORS),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.contentSecondary
                        )
                    }
                }
            }
        }

        if (result.warnings.isNotEmpty()) {
            FormSection(colors = colors) {
                Text(
                    text = stringResource(R.string.warnings_count_format, result.warnings.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.content
                )
                Spacer(Modifier.height(8.dp))
                result.warnings.take(MAX_LISTED_WARNINGS).forEach { warning ->
                    Text(
                        text = stringResource(R.string.warning_item_format, warning.tableName, warning.rowNumber, warning.warningMessage),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.contentSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                if (result.warnings.size > MAX_LISTED_WARNINGS) {
                    Text(
                        text = stringResource(R.string.more_warnings_format, result.warnings.size - MAX_LISTED_WARNINGS),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.contentSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, colors: FormColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.contentSecondary,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.content
        )
    }
}

// ---- Previews ----

private fun sampleResult(errors: Int = 0) = ImportResult(
    isSuccess = true,
    totalRecordsProcessed = 1840,
    recordsImported = 1827,
    recordsSkipped = 4,
    recordsFailed = errors,
    errors = List(errors) {
        ImportError("meal_logs", it + 2, null, "Missing meal reference", ErrorSeverity.ERROR)
    },
    warnings = emptyList(),
    importDuration = 842,
    tablesImported = emptyList()
)

private fun failureResult() = ImportResult(
    isSuccess = false,
    totalRecordsProcessed = 0,
    recordsImported = 0,
    recordsSkipped = 0,
    recordsFailed = 0,
    errors = listOf(
        ImportError("import", 0, null, "Restore aborted, nothing was changed: invalid backup file", ErrorSeverity.FATAL)
    ),
    warnings = emptyList(),
    importDuration = 0,
    tablesImported = emptyList()
)

@Composable
private fun ModePreview() {
    FormDialogSurface(
        title = stringResource(R.string.import_mode_dialog_title),
        actions = { ImportModeActions(onSelect = {}, onDismiss = {}) }
    ) { ImportModeBody() }
}

@Composable
private fun ResultPreview(result: ImportResult) {
    FormDialogSurface(
        title = stringResource(if (result.isSuccess) R.string.import_completed else R.string.import_failed),
        actions = { ImportResultActions({}) }
    ) { ImportResultBody(result) }
}

@Composable
private fun ProgressPreview() {
    FormDialogSurface(
        title = stringResource(R.string.importing_database),
        actions = {}
    ) { ImportProgressBody(ImportProgress("meal_logs", 40, 12, 46, 840, 812, false)) }
}

@Preview(name = "Import progress - light", widthDp = 360, heightDp = 360, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun ImportProgressLightPreview() { FoodLogTheme(darkTheme = false) { ProgressPreview() } }

@Preview(name = "Import progress - dark", widthDp = 360, heightDp = 360, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun ImportProgressDarkPreview() { FoodLogTheme(darkTheme = true) { ProgressPreview() } }

@Preview(name = "Import mode - light", widthDp = 360, heightDp = 600, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun ImportModeLightPreview() { FoodLogTheme(darkTheme = false) { ModePreview() } }

@Preview(name = "Import mode - dark", widthDp = 360, heightDp = 600, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun ImportModeDarkPreview() { FoodLogTheme(darkTheme = true) { ModePreview() } }

@Preview(name = "Import result - light", widthDp = 360, heightDp = 760, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun ImportResultLightPreview() { FoodLogTheme(darkTheme = false) { ResultPreview(sampleResult(errors = 8)) } }

@Preview(name = "Import result - dark", widthDp = 360, heightDp = 760, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun ImportResultDarkPreview() { FoodLogTheme(darkTheme = true) { ResultPreview(sampleResult(errors = 8)) } }

@Preview(name = "Import failure - dark", widthDp = 360, heightDp = 500, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun ImportFailureDarkPreview() { FoodLogTheme(darkTheme = true) { ResultPreview(failureResult()) } }

@Preview(name = "Import failure - light", widthDp = 360, heightDp = 500, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun ImportFailureLightPreview() { FoodLogTheme(darkTheme = false) { ResultPreview(failureResult()) } }
