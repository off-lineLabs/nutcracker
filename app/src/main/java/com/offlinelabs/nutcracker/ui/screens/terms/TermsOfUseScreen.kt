package com.offlinelabs.nutcracker.ui.screens.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.SettingsManager
import com.offlinelabs.nutcracker.ui.theme.appTextPrimaryColor
import com.offlinelabs.nutcracker.ui.theme.appTextSecondaryColor

@Composable
fun TermsOfUseDialog(
    settingsManager: SettingsManager,
    onTermsAgreed: () -> Unit
) {
    val scrollState = rememberScrollState()
    var canAgree by remember { mutableStateOf(false) }
    
    // Monitor scroll position to enable agree button
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            val isAtBottom = scrollState.value >= scrollState.maxValue - 50
            canAgree = isAtBottom
        }
    }
    
    // Parse the text content to handle bold tags
    fun parseTermsText(text: String): AnnotatedString {
        return buildAnnotatedString {
            var currentIndex = 0
            while (currentIndex < text.length) {
                val boldStart = text.indexOf("<b>", currentIndex)
                if (boldStart == -1) {
                    append(text.substring(currentIndex))
                    break
                }
                
                // Add text before bold
                append(text.substring(currentIndex, boldStart))
                
                val boldEnd = text.indexOf("</b>", boldStart)
                if (boldEnd == -1) {
                    append(text.substring(currentIndex))
                    break
                }
                
                // Add bold text
                val boldText = text.substring(boldStart + 3, boldEnd)
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(boldText)
                }
                
                currentIndex = boldEnd + 4
            }
        }
    }
    
    Dialog(
        onDismissRequest = { /* Cannot be dismissed */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.75f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Title
                    Text(
                        text = stringResource(id = R.string.terms_of_use_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = appTextPrimaryColor(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                    
                    // Scrollable terms content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(end = 8.dp)
                        ) {
                            val termsText = stringResource(id = R.string.terms_of_use_dialog_content)
                            Text(
                                text = parseTermsText(termsText),
                                style = MaterialTheme.typography.bodySmall,
                                color = appTextPrimaryColor(),
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.5
                            )
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Scroll instruction
                    if (!canAgree) {
                        Text(
                            text = stringResource(id = R.string.terms_of_use_scroll_to_continue),
                            style = MaterialTheme.typography.bodySmall,
                            color = appTextSecondaryColor(),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                    
                    // Agree button
                    Button(
                        onClick = {
                            settingsManager.setTermsAgreed()
                            onTermsAgreed()
                        },
                        enabled = canAgree,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.terms_of_use_agree),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (!canAgree) {
                        Text(
                            text = stringResource(id = R.string.terms_of_use_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
