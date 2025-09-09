package com.example.template.ui.components.dialogs

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.template.R
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleCodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var barcodeResult by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            startScanning(context, onBarcodeScanned) { result, error ->
                barcodeResult = result
                errorMessage = error
                isScanning = false
                // Automatically process the barcode when detected
                if (result != null) {
                    onBarcodeScanned(result)
                    onDismiss()
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startScanning(context, onBarcodeScanned) { result, error ->
                barcodeResult = result
                errorMessage = error
                isScanning = false
                // Automatically process the barcode when detected
                if (result != null) {
                    onBarcodeScanned(result)
                    onDismiss()
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.scan_barcode),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!hasCameraPermission) {
                    Text(
                        text = "Camera permission is required to scan barcodes",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else if (barcodeResult != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Barcode Scanned Successfully!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = barcodeResult!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (errorMessage != null) {
                    Text(
                        text = "Error: $errorMessage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Scanning state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Opening camera scanner...",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            // No confirm button needed - barcode is processed automatically
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun startScanning(
    context: android.content.Context,
    onBarcodeScanned: (String) -> Unit,
    onResult: (String?, String?) -> Unit
) {
    Log.d("GoogleCodeScanner", "Starting barcode scanning")
    
    // Check if modules are installed
    val moduleInstall = ModuleInstall.getClient(context)
    val moduleInstallRequest = ModuleInstallRequest.newBuilder()
        .addApi(GmsBarcodeScanning.getClient(context))
        .build()
    
    moduleInstall.installModules(moduleInstallRequest)
        .addOnSuccessListener { response ->
            Log.d("GoogleCodeScanner", "Modules installation check completed")
            if (response.areModulesAlreadyInstalled()) {
                Log.d("GoogleCodeScanner", "Modules already installed, starting scan")
                performScan(context, onBarcodeScanned, onResult)
            } else {
                Log.d("GoogleCodeScanner", "Modules were just installed, waiting before scan")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    performScan(context, onBarcodeScanned, onResult)
                }, 1000)
            }
        }
        .addOnFailureListener { e ->
            Log.e("GoogleCodeScanner", "Module installation failed", e)
            onResult(null, "Failed to install required modules: ${e.message}")
        }
}

private fun performScan(
    context: android.content.Context,
    onBarcodeScanned: (String) -> Unit,
    onResult: (String?, String?) -> Unit
) {
    Log.d("GoogleCodeScanner", "Performing barcode scan")
    
    val scanner = GmsBarcodeScanning.getClient(context)
    scanner.startScan()
        .addOnSuccessListener { barcode ->
            Log.d("GoogleCodeScanner", "Barcode scan successful: ${barcode.rawValue}")
            barcode.rawValue?.let { value ->
                onResult(value, null)
            } ?: run {
                onResult(null, "No barcode value found")
            }
        }
        .addOnFailureListener { e ->
            Log.e("GoogleCodeScanner", "Barcode scan failed", e)
            onResult(null, "Scan failed: ${e.message}")
        }
        .addOnCanceledListener {
            Log.d("GoogleCodeScanner", "Barcode scan canceled by user")
            onResult(null, "Scan canceled")
        }
}
