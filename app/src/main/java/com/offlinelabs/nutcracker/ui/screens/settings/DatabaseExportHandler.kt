package com.offlinelabs.nutcracker.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.export.DatabaseExportManager
import kotlinx.coroutines.launch

/**
 * Modern database export handler using Activity Result API
 * Handles file picker and export operations with proper state management
 */
@Composable
fun rememberDatabaseExportHandler(
    database: AppDatabase
): DatabaseExportHandler {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var showMessage by remember { mutableStateOf(false) }
    
    val exportManager = remember { DatabaseExportManager(context, database) }
    
    // Modern Activity Result API for file picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isExporting = true
                exportMessage = null
                
                try {
                    val result = exportManager.exportDatabase(uri)
                    result.fold(
                        onSuccess = { message ->
                            exportMessage = message
                            showMessage = true
                        },
                        onFailure = { exception ->
                            exportMessage = "Export failed: ${exception.message}"
                            showMessage = true
                        }
                    )
                } catch (e: Exception) {
                    exportMessage = "Export failed: ${e.message}"
                    showMessage = true
                } finally {
                    isExporting = false
                }
            }
        }
    }
    
    val startExport = {
        val filename = exportManager.generateExportFilename()
        filePickerLauncher.launch(filename)
    }
    
    return remember {
        DatabaseExportHandler(
            isExporting = isExporting,
            exportMessage = exportMessage,
            showMessage = showMessage,
            onDismissMessage = { showMessage = false },
            onStartExport = startExport
        )
    }
}

/**
 * Data class to hold export handler state
 */
data class DatabaseExportHandler(
    val isExporting: Boolean,
    val exportMessage: String?,
    val showMessage: Boolean,
    val onDismissMessage: () -> Unit,
    val onStartExport: () -> Unit
)
