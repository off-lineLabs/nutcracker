package com.example.template.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import android.os.Build
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.template.R
import com.example.template.data.AppLanguage
import com.example.template.data.SettingsManager
import com.example.template.data.ThemeMode
import com.example.template.data.AppDatabase
import com.example.template.ui.theme.*
import com.example.template.ui.screens.settings.DatabaseExportHandler
import com.example.template.ui.screens.settings.rememberDatabaseExportHandler
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    settingsManager: SettingsManager,
    database: AppDatabase
) {
    val context = LocalContext.current
    var showTermsDialog by remember { mutableStateOf(false) }
    var showLanguageChangeDialog by remember { mutableStateOf(false) }
    
    // Database export handler
    val exportHandler = rememberDatabaseExportHandler(database)
    
    // Get current settings from the manager
    val currentTheme = settingsManager.currentThemeMode
    val currentLanguage = settingsManager.currentAppLanguage
    val isDarkTheme = settingsManager.isDarkTheme(context)
    
    // Use specific colors for the exact look you want
    val lightGray50 = Color(0xFFFAFBFC)
    val lightGray100 = Color(0xFFF3F4F6)
    val darkGray800 = Color(0xFF1F2937)
    val darkGray900 = Color(0xFF111827)
    val textGray200 = Color(0xFFE5E7EB)

    val gradientStartColor = if (isDarkTheme) darkGray900 else lightGray50
    val gradientEndColor = if (isDarkTheme) darkGray800 else lightGray100
    val innerContainerBackgroundColor = if (isDarkTheme) darkGray800 else Color(0xFFC6C6C7)

    val view = LocalView.current
    val density = LocalDensity.current
    
    // Calculate status bar height
    val statusBarHeight = with(density) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsCompat.Type.statusBars().let { insets ->
                view.rootWindowInsets?.getInsets(insets)?.top ?: 0
            }.toDp()
        } else {
            0.dp
        }
    }

    Scaffold(
        topBar = {
            // Custom header with back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarHeight + 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button on the left
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = Color(0xFFC0C0C0),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Settings title in center
                    Text(
                        text = stringResource(R.string.settings),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC0C0C0),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Empty space for balance
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        },
        containerColor = Color.Transparent
    ) { scaffoldPaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientStartColor, gradientEndColor)
                    )
                )
                .padding(scaffoldPaddingValues)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = innerContainerBackgroundColor.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Theme Section
                    item {
                        SettingsSection(
                            title = stringResource(R.string.theme),
                            icon = Icons.Filled.Palette
                        ) {
                            ThemeSelector(
                                selectedTheme = currentTheme,
                                onThemeSelected = { settingsManager.setThemeMode(it) }
                            )
                        }
                    }
                    
                    // Language Section
                    item {
                        SettingsSection(
                            title = stringResource(R.string.language),
                            icon = Icons.Filled.Language
                        ) {
                            LanguageSelector(
                                selectedLanguage = currentLanguage,
                                onLanguageSelected = { 
                                    settingsManager.setAppLanguage(it)
                                    showLanguageChangeDialog = true
                                }
                            )
                        }
                    }
                    
                    // Database Section
                    item {
                        SettingsSection(
                            title = stringResource(R.string.database),
                            icon = Icons.Filled.Settings
                        ) {
                            DatabaseSettings(exportHandler, database)
                        }
                    }
                    
                    // App Version Section
                    item {
                        SettingsSection(
                            title = stringResource(R.string.app_version),
                            icon = Icons.Filled.Info
                        ) {
                            AppVersionInfo()
                        }
                    }
                    
                    // Legal Information Section
                    item {
                        SettingsSection(
                            title = stringResource(R.string.legal_information),
                            icon = Icons.Filled.Gavel
                        ) {
                            LegalInformation(
                                onTermsClick = { showTermsDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Terms of Use Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.terms_of_use_dialog_title),
                    color = appTextPrimaryColor()
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.terms_of_use_dialog_content),
                            color = appTextPrimaryColor(),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showTermsDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.close),
                        color = Color(0xFF60A5FA)
                    )
                }
            },
            containerColor = if (isDarkTheme) Color(0xFF374151) else Color.White,
            titleContentColor = appTextPrimaryColor(),
            textContentColor = appTextSecondaryColor()
        )
    }
    
    // Language Change Dialog
    if (showLanguageChangeDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageChangeDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.language),
                    color = appTextPrimaryColor()
                )
            },
            text = {
                Text(
                    text = "Language changed. The app will restart to apply the new language.",
                    color = appTextSecondaryColor()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showLanguageChangeDialog = false
                        // Restart the activity to apply language change
                        (context as? androidx.activity.ComponentActivity)?.recreate()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.restart_app),
                        color = Color(0xFF60A5FA)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLanguageChangeDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = Color(0xFF9CA3AF)
                    )
                }
            },
            containerColor = if (isDarkTheme) Color(0xFF374151) else Color.White,
            titleContentColor = appTextPrimaryColor(),
            textContentColor = appTextSecondaryColor()
        )
    }
    
    // Export message dialog
    if (exportHandler.showMessage) {
        AlertDialog(
            onDismissRequest = exportHandler.onDismissMessage,
            title = {
                Text(
                    text = stringResource(R.string.database_export),
                    color = appTextPrimaryColor(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = exportHandler.exportMessage ?: "",
                    color = appTextPrimaryColor(),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = exportHandler.onDismissMessage,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF2196F3) // Use a blue color instead of appAccentColor
                    )
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            containerColor = appSurfaceColor(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = appContainerBackgroundColor(),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = appTextPrimaryColor(),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = appTextPrimaryColor(),
                fontWeight = FontWeight.SemiBold
            )
        }
        content()
    }
}

@Composable
private fun ThemeSelector(
    selectedTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit
) {
    Column {
        ThemeOption(
            title = stringResource(R.string.light_theme),
            isSelected = selectedTheme == ThemeMode.LIGHT,
            onClick = { onThemeSelected(ThemeMode.LIGHT) }
        )
        ThemeOption(
            title = stringResource(R.string.dark_theme),
            isSelected = selectedTheme == ThemeMode.DARK,
            onClick = { onThemeSelected(ThemeMode.DARK) }
        )
        ThemeOption(
            title = stringResource(R.string.system_theme),
            isSelected = selectedTheme == ThemeMode.SYSTEM,
            onClick = { onThemeSelected(ThemeMode.SYSTEM) }
        )
    }
}

@Composable
private fun ThemeOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF60A5FA),
                unselectedColor = appTextSecondaryColor()
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = appTextPrimaryColor(),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun LanguageSelector(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val languages = listOf(
        AppLanguage.ENGLISH to stringResource(R.string.english),
        AppLanguage.PORTUGUESE to stringResource(R.string.portuguese),
        AppLanguage.SPANISH to stringResource(R.string.spanish)
    )
    
    val selectedLanguageName = languages.find { it.first == selectedLanguage }?.second ?: stringResource(R.string.english)
    
    Box {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            colors = CardDefaults.outlinedCardColors(
                containerColor = appSurfaceColor()
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, 
                if (expanded) Color(0xFF60A5FA) else appTextSecondaryColor().copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedLanguageName,
                    color = appTextPrimaryColor(),
                    fontSize = 14.sp
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = appTextSecondaryColor(),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = appSurfaceColor(),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            languages.forEach { (language, name) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = name,
                            color = appTextPrimaryColor(),
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = appTextPrimaryColor()
                    )
                )
            }
        }
    }
}

/**
 * Dialog showing import progress
 */
@Composable
private fun ImportProgressDialog(
    progress: com.example.template.data.import.ImportProgress,
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
                    text = stringResource(R.string.importing_database),
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
                    progress = { progress.overallProgress / 100f },
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
    result: com.example.template.data.import.ImportResult,
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
                        text = stringResource(if (result.isSuccess) R.string.import_completed else R.string.import_failed),
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
                            text = stringResource(R.string.import_summary),
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
                            Text("${String.format(Locale.US, "%.1f", result.successRate)}%")
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
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}



@Composable
private fun DatabaseSettings(exportHandler: DatabaseExportHandler, database: AppDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf<com.example.template.data.import.ImportProgress?>(null) }
    var importResult by remember { mutableStateOf<com.example.template.data.import.ImportResult?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    
    val importManager = remember { com.example.template.data.import.DatabaseImportManager(context, database) }
    
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
                        },
                        onFailure = { error ->
                            importResult = com.example.template.data.import.ImportResult(
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
    
    Column {
        DatabaseButton(
            title = stringResource(R.string.export_database),
            icon = Icons.Filled.FileUpload,
            onClick = exportHandler.onStartExport,
            enabled = !exportHandler.isExporting
        )
        DatabaseButton(
            title = stringResource(R.string.import_database),
            icon = Icons.Filled.FileDownload,
            onClick = {
                if (!isImporting) {
                    filePickerLauncher.launch("application/zip")
                }
            },
            enabled = !isImporting
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

@Composable
private fun DatabaseButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp)
            .alpha(if (enabled) 1f else 0.6f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) appTextSecondaryColor() else appTextSecondaryColor().copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = if (enabled) appTextPrimaryColor() else appTextPrimaryColor().copy(alpha = 0.6f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AppVersionInfo() {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName ?: "1.0"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode.toInt()
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode
    }
    
    Text(
        text = stringResource(R.string.version_info, versionName, versionCode),
        color = appTextSecondaryColor(),
        fontSize = 14.sp
    )
}

@Composable
private fun LegalInformation(
    onTermsClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column {
        LegalItem(
            title = stringResource(R.string.publisher_brand),
            icon = Icons.Filled.Info
        )
        LegalItem(
            title = stringResource(R.string.license_info),
            icon = Icons.Filled.Balance
        )
        LegalItem(
            title = stringResource(R.string.github_repo),
            icon = Icons.Filled.Code,
            isClickable = true,
            onClick = { 
                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/JnCoe/offline-calorie-calculator".toUri())
                context.startActivity(intent)
            }
        )
        LegalItem(
            title = stringResource(R.string.donate),
            icon = Icons.Filled.Favorite,
            isClickable = true,
            onClick = { /* TODO: Open donation link */ }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Terms of Use with special styling
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = appTextSecondaryColor())) {
                    append("By using this app you agree with the\n")
                }
                withStyle(
                    style = SpanStyle(
                        color = Color(0xFF60A5FA),
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("terms of use")
                }
            },
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTermsClick() }
        )
    }
}

@Composable
private fun LegalItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isClickable: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isClickable && onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isClickable) Color(0xFF60A5FA) else appTextSecondaryColor(),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = if (isClickable) Color(0xFF60A5FA) else appTextSecondaryColor(),
            fontSize = 12.sp
        )
    }
}
