package com.example.template.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.template.R
import com.example.template.data.AppLanguage
import com.example.template.data.SettingsManager
import com.example.template.data.ThemeMode
import com.example.template.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    var showTermsDialog by remember { mutableStateOf(false) }
    var showLanguageChangeDialog by remember { mutableStateOf(false) }
    
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
        WindowInsetsCompat.Type.statusBars().let { insets ->
            view.rootWindowInsets?.getInsets(insets)?.top ?: 0
        }.toDp()
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
                            title = "Database",
                            icon = Icons.Filled.Settings
                        ) {
                            DatabaseSettings()
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
                Text(
                    text = stringResource(R.string.terms_of_use_dialog_content),
                    color = appTextSecondaryColor()
                )
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
                        text = "Restart App",
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
    Column {
        LanguageOption(
            title = stringResource(R.string.english),
            isSelected = selectedLanguage == AppLanguage.ENGLISH,
            onClick = { onLanguageSelected(AppLanguage.ENGLISH) }
        )
        LanguageOption(
            title = stringResource(R.string.portuguese),
            isSelected = selectedLanguage == AppLanguage.PORTUGUESE,
            onClick = { onLanguageSelected(AppLanguage.PORTUGUESE) }
        )
        LanguageOption(
            title = stringResource(R.string.spanish),
            isSelected = selectedLanguage == AppLanguage.SPANISH,
            onClick = { onLanguageSelected(AppLanguage.SPANISH) }
        )
    }
}

@Composable
private fun LanguageOption(
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
private fun DatabaseSettings() {
    Column {
        DatabaseButton(
            title = stringResource(R.string.export_database),
            icon = Icons.Filled.FileDownload,
            onClick = { /* TODO: Implement export functionality */ }
        )
        DatabaseButton(
            title = stringResource(R.string.import_database),
            icon = Icons.Filled.FileUpload,
            onClick = { /* TODO: Implement import functionality */ }
        )
    }
}

@Composable
private fun DatabaseButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = appTextSecondaryColor(),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = appTextPrimaryColor(),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AppVersionInfo() {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName ?: "1.0"
    val versionCode = packageInfo.longVersionCode.toInt()
    
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
    Column {
        LegalItem(
            title = stringResource(R.string.publisher_brand),
            icon = Icons.Filled.Info
        )
        LegalItem(
            title = stringResource(R.string.license_info),
            icon = Icons.Filled.Gavel
        )
        LegalItem(
            title = stringResource(R.string.github_repo),
            icon = Icons.Filled.Code,
            isClickable = true,
            onClick = { /* TODO: Open GitHub repository */ }
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
                    append("By using this app you agree with the ")
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
            modifier = Modifier.clickable { onTermsClick() }
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
