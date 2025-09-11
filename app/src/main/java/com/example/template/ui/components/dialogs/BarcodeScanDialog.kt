package com.example.template.ui.components.dialogs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.template.R
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanDialog(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
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
    var cameraError by remember { mutableStateOf<String?>(null) }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
                } else if (cameraError != null) {
                    Text(
                        text = "Camera error: $cameraError",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Real camera preview
                    if (hasCameraPermission) {
                        CameraPreview(
                            onBarcodeDetected = { barcode ->
                                barcodeResult = barcode
                            },
                            onError = { error ->
                                cameraError = error
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (barcodeResult != null) {
                Button(
                    onClick = {
                        onBarcodeScanned(barcodeResult!!)
                        onDismiss()
                    }
                ) {
                    Text("Use This Barcode")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun CameraPreview(
    onBarcodeDetected: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var textureView by remember { mutableStateOf<TextureView?>(null) }
    var cameraDevice by remember { mutableStateOf<CameraDevice?>(null) }
    var imageReader by remember { mutableStateOf<ImageReader?>(null) }
    var backgroundExecutor by remember { mutableStateOf<ExecutorService?>(null) }
    
    // Initialize ML Kit barcode scanner
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    
    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Camera will be opened when texture is available
                }
                Lifecycle.Event.ON_PAUSE -> {
                    cameraDevice?.close()
                    cameraDevice = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            cameraDevice?.close()
            imageReader?.close()
            backgroundExecutor?.shutdown()
            barcodeScanner.close()
        }
    }
    
    AndroidView(
        factory = { ctx ->
            val texture = TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        openCamera(ctx, surface, width, height, onBarcodeDetected, onError) { device, reader, executor ->
                            cameraDevice = device
                            imageReader = reader
                            backgroundExecutor = executor
                        }
                    }
                    
                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = true
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
            textureView = texture
            texture
        },
        modifier = modifier
    )
}

private fun openCamera(
    context: Context,
    surface: SurfaceTexture,
    width: Int,
    height: Int,
    onBarcodeDetected: (String) -> Unit,
    onError: (String) -> Unit,
    onCameraReady: (CameraDevice, ImageReader, ExecutorService) -> Unit
) {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraId = cameraManager.cameraIdList[0] // Use first available camera
    
    try {
        Log.d("BarcodeScan", "Opening camera with ID: $cameraId")
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = map?.getOutputSizes(ImageFormat.JPEG)
        val largest = sizes?.maxByOrNull { it.height * it.width }
        
        Log.d("BarcodeScan", "Camera image sizes available: ${sizes?.size ?: 0}")
        Log.d("BarcodeScan", "Using image size: ${largest?.width}x${largest?.height}")
        
        val imageReader = ImageReader.newInstance(
            largest?.width ?: width,
            largest?.height ?: height,
            ImageFormat.JPEG,
            1
        )
        
        val backgroundExecutor = Executors.newSingleThreadExecutor()
        val backgroundHandler = Handler(Looper.getMainLooper())
        
        // Set up barcode detection
        imageReader.setOnImageAvailableListener({
            val image = imageReader.acquireLatestImage()
            if (image != null) {
                Log.d("BarcodeScan", "Processing camera image for barcode detection")
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    Log.d("BarcodeScan", "Bitmap created successfully, size: ${bitmap.width}x${bitmap.height}")
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    
                    val barcodeScanner = BarcodeScanning.getClient()
                    barcodeScanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            Log.d("BarcodeScan", "Barcode detection completed, found ${barcodes.size} barcodes")
                            for (barcode in barcodes) {
                                Log.d("BarcodeScan", "Found barcode: ${barcode.rawValue}")
                                barcode.rawValue?.let { value ->
                                    Log.d("BarcodeScan", "Detected barcode value: $value")
                                    onBarcodeDetected(value)
                                    return@addOnSuccessListener
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("BarcodeScan", "Barcode detection failed", exception)
                        }
                        .addOnCompleteListener {
                            image.close()
                        }
                } else {
                    Log.e("BarcodeScan", "Failed to create bitmap from camera image")
                    image.close()
                }
            } else {
                Log.w("BarcodeScan", "Camera image is null")
            }
        }, backgroundHandler)
        
        val stateCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.d("BarcodeScan", "Camera opened successfully")
                val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                val textureSurface = Surface(surface)
                captureRequestBuilder.addTarget(textureSurface)
                captureRequestBuilder.addTarget(imageReader.surface)
                
                val sessionCallback = object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d("BarcodeScan", "Camera session configured successfully")
                        val captureRequest = captureRequestBuilder.build()
                        session.setRepeatingRequest(captureRequest, null, backgroundHandler)
                        Log.d("BarcodeScan", "Camera preview started")
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("BarcodeScan", "Failed to configure camera session")
                        onError("Failed to configure camera session")
                    }
                }
                
                camera.createCaptureSession(
                    listOf(textureSurface, imageReader.surface),
                    sessionCallback,
                    backgroundHandler
                )
                
                onCameraReady(camera, imageReader, backgroundExecutor)
            }
            
            override fun onDisconnected(camera: CameraDevice) {
                Log.d("BarcodeScan", "Camera disconnected")
                camera.close()
            }
            
            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("BarcodeScan", "Camera error: $error")
                onError("Camera error: $error")
                camera.close()
            }
        }
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
        } else {
            onError("Camera permission not granted")
        }
        
    } catch (e: Exception) {
        onError("Failed to open camera: ${e.message}")
    }
}
