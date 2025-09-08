package com.example.template.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.template.R
import com.example.template.data.AppDatabase
import com.example.template.data.import.DatabaseImportManager
import com.example.template.data.import.ImportProgress
import com.example.template.data.import.ImportResult
import kotlinx.coroutines.launch

/**
 * Handler for database import operations
 */
@Composable
fun DatabaseImportHandler(
    database: AppDatabase,
    onImportComplete: (ImportResult) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf<ImportProgress?>(null) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    
    val importManager = remember { DatabaseImportManager(context, database) }
    
    // Observe import progress
    LaunchedEffect(importManager) {
        importManager.importProgress.collect { progress ->
            importProgress = progress
            showProgressDialog = progress != null && !progress.isComplete
        }
    }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                isImporting = true
                showProgressDialog = true
                
                try {
                    val result = importManager.importDatabase(selectedUri, createBackup = true)
                    result.fold(
                        onSuccess = { result ->
                            importResult = result
                            showResultDialog = true
                            onImportComplete(result)
                        },
                        onFailure = { error ->
                            // Handle error
                            importResult = ImportResult(
                                isSuccess = false,
                                totalRecordsProcessed = 0,
                                recordsImported = 0,
                                recordsSkipped = 0,
                                recordsFailed = 0,
                                errors = listOf(
                                    com.example.template.data.import.ImportError(
                                        tableName = "import",
                                        rowNumber = 0,
                                        fieldName = null,
                                        errorMessage = "Import failed: ${error.message}",
                                        severity = com.example.template.data.import.ErrorSeverity.FATAL
                                    )
                                ),
                                warnings = emptyList(),
                                importDuration = 0,
                                tablesImported = emptyList()
                            )
                            showResultDialog = true
                        }
                    )
                } finally {
                    isImporting = false
                    showProgressDialog = false
                }
            }
        }
    }
    
    // Import button
    Button(
        onClick = {
            if (!isImporting) {
                filePickerLauncher.launch("application/zip")
            }
        },
        enabled = !isImporting,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isImporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = if (isImporting) "Importing..." else "Import Database",
            fontWeight = FontWeight.Medium
        )
    }
    
    // Progress dialog
    if (showProgressDialog && importProgress != null) {
        ImportProgressDialog(
            progress = importProgress!!,
            onDismiss = { /* Cannot dismiss during import */ }
        )
    }
    
    // Result dialog
    if (showResultDialog && importResult != null) {
        ImportResultDialog(
            result = importResult!!,
            onDismiss = { 
                showResultDialog = false
                importResult = null
            }
        )
    }
}

/**
 * Dialog showing import progress
 */
@Composable
private fun ImportProgressDialog(
    progress: ImportProgress,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Importing Database",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Processing: ${progress.currentTable}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LinearProgressIndicator(
                    progress = progress.overallProgress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${progress.overallProgress}% Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Records processed: ${progress.recordsProcessed}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Records imported: ${progress.recordsImported}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Dialog showing import results
 */
@Composable
private fun ImportResultDialog(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Import ${if (result.isSuccess) "Completed" else "Failed"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (result.isSuccess) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Text("✕", style = MaterialTheme.typography.titleLarge)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summary statistics
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Import Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total processed:")
                            Text("${result.totalRecordsProcessed}")
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Successfully imported:")
                            Text("${result.recordsImported}")
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Skipped:")
                            Text("${result.recordsSkipped}")
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Failed:")
                            Text("${result.recordsFailed}")
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Success rate:")
                            Text("${String.format("%.1f", result.successRate)}%")
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Duration:")
                            Text("${result.importDuration}ms")
                        }
                    }
                }
                
                // Show errors if any
                if (result.errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Errors (${result.errors.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    result.errors.take(5).forEach { error ->
                        Text(
                            text = "• ${error.tableName} (Row ${error.rowNumber}): ${error.errorMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    if (result.errors.size > 5) {
                        Text(
                            text = "... and ${result.errors.size - 5} more errors",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Show warnings if any
                if (result.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Warnings (${result.warnings.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    result.warnings.take(3).forEach { warning ->
                        Text(
                            text = "• ${warning.tableName} (Row ${warning.rowNumber}): ${warning.warningMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (result.warnings.size > 3) {
                        Text(
                            text = "... and ${result.warnings.size - 3} more warnings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
