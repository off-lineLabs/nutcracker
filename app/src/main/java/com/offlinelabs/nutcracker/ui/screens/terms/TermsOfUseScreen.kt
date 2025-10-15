package com.offlinelabs.nutcracker.ui.screens.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.SettingsManager
import com.offlinelabs.nutcracker.ui.theme.appTextPrimaryColor
import com.offlinelabs.nutcracker.ui.theme.appTextSecondaryColor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.graphics.toArgb
import android.content.Intent
import android.net.Uri

@Composable
fun TermsOfUseDialog(
    settingsManager: SettingsManager,
    onTermsAgreed: () -> Unit
) {
    var canAgree by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
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
                    
                    // HTML content via WebView
                    // Resolve themed text color in composable scope
                    val themedTextHex = run {
                        val c = appTextPrimaryColor()
                        String.format("#%06X", (0xFFFFFF and c.toArgb()))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (hasError) {
                            Text(
                                text = stringResource(R.string.error_loading_document),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            AndroidView(
                                factory = { context ->
                                    WebView(context).apply {
                                        settings.apply {
                                            javaScriptEnabled = false
                                            domStorageEnabled = false
                                            loadWithOverviewMode = true
                                            useWideViewPort = true
                                            defaultTextEncodingName = "utf-8"
                                        }
                                        // Transparent background to blend with dialog
                                        setBackgroundColor(0x00000000)
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                isLoading = false
                                                canAgree = !this@apply.canScrollVertically(1)
                                            }
                                            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                                val uri = request?.url ?: return false
                                                try {
                                                    view?.context?.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                    return true
                                                } catch (_: Exception) {
                                                    return false
                                                }
                                            }
                                        }
                                        isHorizontalScrollBarEnabled = false
                                        setOnScrollChangeListener { _, _, _, _, _ ->
                                            canAgree = !canScrollVertically(1)
                                        }
                                    }
                                },
                                update = { webView ->
                                    val rawId = R.raw.terms
                                    val raw = webView.context.resources.openRawResource(rawId).bufferedReader().use { it.readText() }
                                    val css = """
                                        <style>
                                          html, body { background: transparent !important; }
                                          body { color: $themedTextHex !important; }
                                          h1, h2, h3, h4, h5, h6 { color: $themedTextHex !important; }
                                          a { color: inherit; text-decoration: underline; }
                                          html, body { overflow-x: hidden; }
                                          img, table, pre, code, div { max-width: 100%; box-sizing: border-box; }
                                        </style>
                                    """.trimIndent()
                                    val themedHtml = if (raw.contains("</head>", ignoreCase = true)) {
                                        raw.replace(Regex("</head>", RegexOption.IGNORE_CASE), css + "</head>")
                                    } else {
                                        "<head>" + css + "</head>" + raw
                                    }
                                    webView.loadDataWithBaseURL(
                                        "file:///android_res/raw/",
                                        themedHtml,
                                        "text/html",
                                        "utf-8",
                                        null
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (isLoading && !hasError) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
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
