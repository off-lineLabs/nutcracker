package com.offlinelabs.nutcracker.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.offlinelabs.nutcracker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocScreen(
    documentType: LegalDocumentType,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = when (documentType) {
                        LegalDocumentType.TERMS -> stringResource(R.string.terms_of_use)
                        LegalDocumentType.PRIVACY -> stringResource(R.string.privacy_policy)
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )

        // Content
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (hasError) {
                // Error state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.error_loading_document),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        hasError = false
                        isLoading = true
                    }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            } else {
                // WebView content
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
                                }
                                
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    hasError = true
                                    isLoading = false
                                }
                            }
                        }
                    },
                    update = { webView ->
                        val resourceName = when (documentType) {
                            LegalDocumentType.TERMS -> "terms"
                            LegalDocumentType.PRIVACY -> "privacy"
                        }
                        val url = "file:///android_res/raw/$resourceName.html"
                        webView.loadUrl(url)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Loading indicator
            if (isLoading && !hasError) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

enum class LegalDocumentType {
    TERMS,
    PRIVACY
}
