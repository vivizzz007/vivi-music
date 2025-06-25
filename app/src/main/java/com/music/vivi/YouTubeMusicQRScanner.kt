package com.music.vivi

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions



import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.sp
import com.journeyapps.barcodescanner.ScanIntentResult
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.compose.BackHandler
import com.music.vivi.R
import com.journeyapps.barcodescanner.CaptureActivity

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.draw.shadow
import com.airbnb.lottie.compose.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

@OptIn(ExperimentalMaterial3Api::class)
class SquareViewfinderActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeMusicQRScanner(
    onBackClick: () -> Unit,
    onLinkScanned: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var scannedUrl by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Handle system back button press
    BackHandler(enabled = true) {
        onBackClick()
    }

    fun launchScanner(launcher: ActivityResultLauncher<ScanOptions>) {
        launcher.launch(
            ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Align QR code within frame")
                setCameraId(0)
                setBeepEnabled(false)
                setBarcodeImageEnabled(false)
                setOrientationLocked(true)
                setCaptureActivity(SquareViewfinderActivity::class.java)
            }
        )
    }

    fun processQRContent(content: String) {
        if (content.contains("music.youtube.com")) {
            scannedUrl = content
            showError = false
            errorMessage = ""
            onLinkScanned(content)
        } else {
            showError = true
            errorMessage = "Not a YouTube Music link"
        }
    }

    fun processImageForQR(uri: Uri) {
        isProcessingImage = true
        showError = false
        errorMessage = ""

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val intArray = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

                val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                try {
                    val reader = QRCodeReader()
                    val result = reader.decode(binaryBitmap)
                    processQRContent(result.text)
                } catch (e: Exception) {
                    showError = true
                    errorMessage = "No QR code found in image"
                }
            } else {
                showError = true
                errorMessage = "Failed to load image"
            }
        } catch (e: Exception) {
            showError = true
            errorMessage = "Error processing image: ${e.message}"
        } finally {
            isProcessingImage = false
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        isScanning = false
        result.contents?.let { content ->
            processQRContent(content)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { processImageForQR(it) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isScanning = true
            launchScanner(scanLauncher)
        }
    }

    fun startCameraScan() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                isScanning = true
                launchScanner(scanLauncher)
            }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun startGalleryScan() {
        imagePickerLauncher.launch("image/*")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Scan ViviMusic QR Code",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.15.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                isScanning -> ScanInProgressUI()
                isProcessingImage -> ProcessingImageUI()
                scannedUrl != null -> ScannedUrlUI(
                    url = scannedUrl!!,
                    context = context,
                    onRescan = {
                        scannedUrl = null
                        showError = false
                        errorMessage = ""
                    }
                )
                else -> ReadyToScanUI(
                    onCameraScanClick = ::startCameraScan,
                    onGalleryScanClick = ::startGalleryScan,
                    showError = showError,
                    errorMessage = errorMessage
                )
            }
        }
    }
}

@Composable
private fun ScanInProgressUI() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "SCANNING...",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

@Composable
private fun ProcessingImageUI() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "PROCESSING IMAGE...",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}


@Composable
private fun ReadyToScanUI(
    onCameraScanClick: () -> Unit,
    onGalleryScanClick: () -> Unit,
    showError: Boolean,
    errorMessage: String
) {
    // Lottie animation composition and progress
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.shareing_music)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 1f,
        clipSpec = LottieClipSpec.Progress(0f, 1f)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // QR Code Frame with Lottie animation
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Position the QR code within the frame or select from gallery",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Camera Scan Button
        Button(
            onClick = onCameraScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("SCAN WITH CAMERA")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Gallery Scan Button
        OutlinedButton(
            onClick = onGalleryScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 1.5.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("SELECT FROM GALLERY")
        }

        if (showError) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // Glowing Card with Note
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = "Note: You can scan music using QR codes. To share music, go to the song's 3-dot menu > Share > QR",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}



@Composable
private fun ScannedUrlUI(
    url: String,
    context: Context,
    onRescan: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = "Link detected!",
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        )

        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                ContextCompat.startActivity(context, intent, null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 1.5.dp
            )
        ) {
            Text("OPEN IN YOUTUBE MUSIC")
        }

        TextButton(
            onClick = onRescan,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Scan Another Code")
        }
    }
}