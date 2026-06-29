package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.qr.QRGenerator
import com.app.qr.QRGeneratorHelper
import com.app.qr.database.HistoryItem
import com.app.qr.database.QRViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val viewModel: QRViewModel = viewModel()
    
    val inputPayload by viewModel.inputPayload.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val eccLevel by viewModel.eccLevel.collectAsState()
    val qrSize by viewModel.qrSize.collectAsState()
    val fgColor by viewModel.fgColor.collectAsState()
    val bgColor by viewModel.bgColor.collectAsState()
    val backgroundImage by viewModel.backgroundImage.collectAsState()
    val logoImage by viewModel.logoImage.collectAsState()
    val qrOpacity by viewModel.qrOpacity.collectAsState()
    val qrPosX by viewModel.qrPosX.collectAsState()
    val qrPosY by viewModel.qrPosY.collectAsState()
    val outputBitmap by viewModel.outputBitmap.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val historyList by viewModel.historyState.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Home/Editor, 1: Scan, 2: History, 3: Native NDK

    // Show status alerts
    LaunchedEffect(statusText) {
        statusText?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatus()
        }
    }

    // Trigger initial QR code render
    LaunchedEffect(inputPayload, selectedMode, eccLevel, qrSize, fgColor, bgColor, backgroundImage, logoImage) {
        viewModel.triggerGeneration()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF3F4F9),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Text("🏠", fontSize = 20.sp) },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001D36),
                        selectedTextColor = Color(0xFF005FB0),
                        indicatorColor = Color(0xFFD1E4FF)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Text("📸", fontSize = 20.sp) },
                    label = { Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001D36),
                        selectedTextColor = Color(0xFF005FB0),
                        indicatorColor = Color(0xFFD1E4FF)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Text("📜", fontSize = 20.sp) },
                    label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001D36),
                        selectedTextColor = Color(0xFF005FB0),
                        indicatorColor = Color(0xFFD1E4FF)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Text("⚙️", fontSize = 20.sp) },
                    label = { Text("Native", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001D36),
                        selectedTextColor = Color(0xFF005FB0),
                        indicatorColor = Color(0xFFD1E4FF)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> EditorDashboard(
                    viewModel = viewModel,
                    inputPayload = inputPayload,
                    selectedMode = selectedMode,
                    eccLevel = eccLevel,
                    qrSize = qrSize,
                    fgColor = fgColor,
                    bgColor = bgColor,
                    backgroundImage = backgroundImage,
                    logoImage = logoImage,
                    qrOpacity = qrOpacity,
                    qrPosX = qrPosX,
                    qrPosY = qrPosY,
                    outputBitmap = outputBitmap,
                    isProcessing = isProcessing
                )
                1 -> CameraScannerScreen(onResultScanned = { decoded ->
                    viewModel.setInputPayload(decoded)
                    activeTab = 0
                    viewModel.triggerGeneration()
                    Toast.makeText(context, "Loaded Scanned QR Payload!", Toast.LENGTH_SHORT).show()
                })
                2 -> HistoryScreen(
                    historyList = historyList,
                    onItemClick = { item ->
                        viewModel.setInputPayload(item.text)
                        viewModel.setQrSize(item.size)
                        viewModel.setQRColors(item.fgColor, item.bgColor)
                        viewModel.setSelectedMode(if (item.isStealth) 1 else 0)
                        activeTab = 0
                        Toast.makeText(context, "Restored and dynamically re-rendered from C++!", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { viewModel.deleteHistory(it) },
                    onClearAll = { viewModel.clearAllHistory() }
                )
                3 -> NativePlaygroundScreen(viewModel = viewModel)
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF005FB0))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Processing JNI C++ Core...", fontWeight = FontWeight.Bold, color = Color(0xFF001D36))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorDashboard(
    viewModel: QRViewModel,
    inputPayload: String,
    selectedMode: Int,
    eccLevel: Int,
    qrSize: Int,
    fgColor: Int,
    bgColor: Int,
    backgroundImage: Bitmap?,
    logoImage: Bitmap?,
    qrOpacity: Float,
    qrPosX: Float,
    qrPosY: Float,
    outputBitmap: Bitmap?,
    isProcessing: Boolean
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Activity launchers for Background and Logo Images
    val bgImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.selectBackgroundImage(it, bitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load background: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val logoImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.selectLogoImage(bitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load logo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NDK QR Pro",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001D36),
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "C++ CORE ENGINE ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF44474E),
                        letterSpacing = 1.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1E4FF))
                        .border(1.dp, Color(0xFFA8C8FB), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("C++", fontWeight = FontWeight.Bold, color = Color(0xFF001D36), fontSize = 12.sp)
                }
            }
        }

        // Live Preview Editor Container
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LIVE RENDER PREVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF005FB0),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (outputBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF3F4F9))
                                .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedMode == 2 && backgroundImage != null) {
                                // Interactive Overlay Drag Mode
                                Box(
                                    modifier = Modifier.size(240.dp)
                                ) {
                                    // Background
                                    Image(
                                        bitmap = backgroundImage.asImageBitmap(),
                                        contentDescription = "Background",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    // Drag-and-drop QR overlay
                                    var xPos by remember { mutableStateOf(qrPosX) }
                                    var yPos by remember { mutableStateOf(qrPosY) }

                                    val density = LocalDensity.current
                                    val overlaySizeDp = 80.dp
                                    val overlaySizePx = with(density) { overlaySizeDp.toPx() }
                                    val containerSizePx = with(density) { 240.dp.toPx() }

                                    // Pre-render a tiny preview of QR to drag
                                    var tinyQrBitmap by remember { mutableStateOf<Bitmap?>(null) }
                                    LaunchedEffect(inputPayload, fgColor, bgColor) {
                                        val pixels = QRGenerator.generateQR(inputPayload, 128, eccLevel, fgColor, bgColor)
                                        val qr = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
                                        qr.setPixels(pixels, 0, 128, 0, 0, 128, 128)
                                        tinyQrBitmap = qr
                                    }

                                    tinyQrBitmap?.let { qrBmp ->
                                        Image(
                                            bitmap = qrBmp.asImageBitmap(),
                                            contentDescription = "QR Overlay",
                                            alpha = qrOpacity,
                                            modifier = Modifier
                                                .size(overlaySizeDp)
                                                .offset {
                                                    IntOffset(
                                                        xPos.roundToInt(),
                                                        yPos.roundToInt()
                                                    )
                                                }
                                                .pointerInput(Unit) {
                                                    detectDragGestures { change, dragAmount ->
                                                        change.consume()
                                                        xPos = (xPos + dragAmount.x).coerceIn(0f, containerSizePx - overlaySizePx)
                                                        yPos = (yPos + dragAmount.y).coerceIn(0f, containerSizePx - overlaySizePx)
                                                        // Sync position back to C++ render coordinate scaling
                                                        viewModel.setQrPosition(
                                                            (xPos / containerSizePx) * qrSize,
                                                            (yPos / containerSizePx) * qrSize
                                                        )
                                                    }
                                                }
                                                .border(2.dp, Color.White, RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                            } else {
                                // Standard / Stealth Render
                                Image(
                                    bitmap = outputBitmap.asImageBitmap(),
                                    contentDescription = "Generated QR",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF3F4F9))
                                .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No Live Output Rendered", color = Color.Gray, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live preview actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                outputBitmap?.let { viewModel.saveBitmapToGallery(it, false) }
                            },
                            enabled = outputBitmap != null,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FB0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save HD", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                outputBitmap?.let { viewModel.saveBitmapToGallery(it, true) }
                            },
                            enabled = outputBitmap != null,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1B1F)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Hide Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                outputBitmap?.let { bmp ->
                                    viewModel.shareAsBase64(bmp) { base64 ->
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Base64 QR String:\n$base64")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Base64 String"))
                                    }
                                }
                            },
                            enabled = outputBitmap != null,
                            modifier = Modifier.background(Color(0xFFD1E4FF), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF001D36))
                        }
                    }
                }
            }
        }

        // Payload Text Input Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "PAYLOAD INPUT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF005FB0)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputPayload,
                        onValueChange = { viewModel.setInputPayload(it) },
                        placeholder = { Text("Enter Text/URL to encode...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF005FB0),
                            unfocusedBorderColor = Color(0xFFE1E2EC)
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // NDK Processing Mode Switches
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "NDK C++ MODE SELECTOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF005FB0)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F9), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Standard", "Stealth (LSB)", "Overlay").forEachIndexed { index, name ->
                            val isSelected = selectedMode == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color.White else Color.Transparent)
                                    .clickable { viewModel.setSelectedMode(index) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF001D36) else Color(0xFF44474E),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mode specific configurations
        if (selectedMode == 1 || selectedMode == 2) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "IMAGE PARAMETERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF005FB0)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { bgImageLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD1E4FF)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (backgroundImage != null) "Change Background" else "Upload Background",
                                    color = Color(0xFF001D36),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (selectedMode == 2) {
                            // Opacity Control slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("QR Opacity:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${(qrOpacity * 100).toInt()}%", fontSize = 12.sp, color = Color(0xFF005FB0), fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = qrOpacity,
                                    onValueChange = { viewModel.setQrOpacity(it) },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF005FB0),
                                        activeTrackColor = Color(0xFF005FB0)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom QR configuration options
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ADVANCED SPECIFICATIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF005FB0)
                    )

                    // Error correction level selection
                    Column {
                        Text("Error Correction Level (ECC):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("L (7%)", "M (15%)", "Q (25%)", "H (30%)").forEachIndexed { index, level ->
                                val isSel = eccLevel == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            1.dp,
                                            if (isSel) Color(0xFF005FB0) else Color(0xFFE1E2EC),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .background(if (isSel) Color(0xFFD1E4FF) else Color.White, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setEccLevel(index) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(level, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color(0xFF001D36) else Color.DarkGray)
                                }
                            }
                        }
                    }

                    // Resolution size configuration
                    Column {
                        Text("Output Native Resolution:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(512, 1024, 2048).forEach { size ->
                                val isSel = qrSize == size
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            1.dp,
                                            if (isSel) Color(0xFF005FB0) else Color(0xFFE1E2EC),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .background(if (isSel) Color(0xFFD1E4FF) else Color.White, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setQrSize(size) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${size}px", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color(0xFF001D36) else Color.DarkGray)
                                }
                            }
                        }
                    }

                    // Center Logo Upload
                    if (selectedMode == 0) {
                        Column {
                            Text("Center Logo Integration:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { logoImageLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F9)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Pick Custom Logo", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (logoImage != null) {
                                    Image(
                                        bitmap = logoImage.asImageBitmap(),
                                        contentDescription = "Logo",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text(
                                        "Clear",
                                        color = Color.Red,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { viewModel.selectLogoImage(null) }
                                    )
                                } else {
                                    Text("None Loaded", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    // Customizable QR Colors
                    Column {
                        Text("Custom Palette Rendering:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf(
                                Pair(0xFF001D36.toInt(), 0xFFFFFFFF.toInt()), // Classic Slate
                                Pair(0xFF004385.toInt(), 0xFFF3F4F9.toInt()), // Royal Ocean
                                Pair(0xFF1B1B1F.toInt(), 0xFFE3E2E6.toInt()), // Minimal Charcoal
                                Pair(0xFF052A11.toInt(), 0xFFD3FDE2.toInt())  // Matrix Green
                            ).forEach { pair ->
                                val isSel = fgColor == pair.first && bgColor == pair.second
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(pair.second))
                                        .border(
                                            if (isSel) 3.dp else 1.dp,
                                            if (isSel) Color(0xFF005FB0) else Color.LightGray,
                                            CircleShape
                                        )
                                        .clickable { viewModel.setQRColors(pair.first, pair.second) }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(pair.first))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trigger History & DB saves
        item {
            Button(
                onClick = { viewModel.saveToHistory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FB0))
            ) {
                Text("Save to JNI SQL Database", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun CameraScannerScreen(onResultScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (hasCameraPermission) {
            Text("ALIGN QR CODE IN FRAME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF005FB0))
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(3.dp, Color(0xFF005FB0), RoundedCornerShape(24.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = androidx.camera.core.Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                val buffer: ByteBuffer = imageProxy.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                val width = imageProxy.width
                                val height = imageProxy.height

                                val source = PlanarYUVLuminanceSource(
                                    bytes, width, height, 0, 0, width, height, false
                                )
                                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                                try {
                                    val result = MultiFormatReader().decode(binaryBitmap)
                                    val decodedText = result.text
                                    if (decodedText.isNotEmpty()) {
                                        onResultScanned(decodedText)
                                    }
                                } catch (e: Exception) {
                                    // Ignore frame decoding errors
                                } finally {
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Continuous scanning powered by CameraX NDK feed...", fontSize = 11.sp, color = Color.Gray)
        } else {
            Text("Camera Permission Required for Built-In Scanner", fontWeight = FontWeight.Bold, color = Color.Red)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FB0))
            ) {
                Text("Grant Camera Access")
            }
        }
    }
}

@Composable
fun HistoryScreen(
    historyList: List<HistoryItem>,
    onItemClick: (HistoryItem) -> Unit,
    onDelete: (HistoryItem) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("SQLite QR History", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF001D36))
                Text("Dynamic C++ on-the-fly regeneration", fontSize = 11.sp, color = Color.Gray)
            }
            if (historyList.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Clear All", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📜", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("History is completely empty", color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("Save NDK QRs to populate database", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyList) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color(0xFF001D36)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (item.isStealth) Color(0xFFF5D3FD) else Color(0xFFD1E4FF),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (item.isStealth) "Stealth" else "Standard",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isStealth) Color(0xFF20052A) else Color(0xFF001D36)
                                        )
                                    }
                                    Text("${item.size}px", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            IconButton(onClick = { onDelete(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NativePlaygroundScreen(viewModel: QRViewModel) {
    val context = LocalContext.current
    var batchInput by remember { mutableStateOf("https://google.com\nhttps://telegram.org\nhttps://github.com") }
    var benchmarkResult by remember { mutableStateOf<String?>(null) }
    var benchmarkRunning by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Native NDK Control Panel", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF001D36))
                Text("Direct C++ JNI interface telemetry & batch sequencing", fontSize = 11.sp, color = Color.Gray)
            }
        }

        // TELEMETRY CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("JNI NDK LINK STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF005FB0))
                    Divider(color = Color(0xFFF3F4F9))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Library:", fontSize = 12.sp, color = Color.Gray)
                        Text("libqr_native.so", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Core Generator engine:", fontSize = 12.sp, color = Color.Gray)
                        Text("ZXing NDK Bridge via C++", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Hardcoded watermark signature:", fontSize = 12.sp, color = Color.Gray)
                        Text("ENABLED (80% opacity, 5% h)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF005FB0))
                    }
                }
            }
        }

        // JNI RENDER BENCHMARKING
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("C++ VS KOTLIN SPEED BENCHMARK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF005FB0))
                    Text(
                        "Benchmark rendering a high-fidelity 2048x2048 QR code payload using C++ NDK logic versus standard Kotlin logic.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Button(
                        onClick = {
                            benchmarkRunning = true
                            benchmarkResult = null
                            val testPayload = "TG- @BLOODYNIGHTMODMENU_BENCHMARK_TEST"
                            
                            // Measure C++ JNI implementation speed
                            val startTimeCpp = System.nanoTime()
                            val cppPixels = QRGenerator.generateQR(testPayload, 2048, 1, 0xFF000000.toInt(), 0xFFFFFFFF.toInt())
                            val durationCpp = (System.nanoTime() - startTimeCpp) / 1_000_000.0

                            // Measure Kotlin-only generation simulation speed for equivalent matrix upscale
                            val startTimeKotlin = System.nanoTime()
                            val matrix = QRGeneratorHelper.generateQRMatrix(testPayload, 1)
                            val matrixSize = matrix.size
                            val ktPixels = IntArray(2048 * 2048)
                            for (y in 0 until 2048) {
                                val qy = (y * matrixSize) / 2048
                                for (x in 0 until 2048) {
                                    val qx = (x * matrixSize) / 2048
                                    ktPixels[y * 2048 + x] = if (matrix[qy][qx]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                                }
                            }
                            val durationKotlin = (System.nanoTime() - startTimeKotlin) / 1_000_000.0

                            benchmarkResult = "C++ NDK Execution Time: ${"%.2f".format(durationCpp)} ms\nKotlin Execution Time: ${"%.2f".format(durationKotlin)} ms\nSpeedup Factor: ${"%.2f".format(durationKotlin / durationCpp)}x Faster in C++!"
                            benchmarkRunning = false
                        },
                        enabled = !benchmarkRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1B1F)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (benchmarkRunning) "Running Benchmark..." else "Execute Native Performance Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    benchmarkResult?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3F4F9), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = it,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF001D36)
                            )
                        }
                    }
                }
            }
        }

        // BATCH LINK GENERATOR
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("FEATURE: MULTI-THREAD BATCH GENERATOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF005FB0))
                    Text(
                        "Enter up to 5 URLs/Payloads (one per line). QRs will be sequentially rendered in background threads by the C++ engine, packed, and exported as a ZIP file.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = batchInput,
                        onValueChange = { batchInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        textStyle = FontFamily.Monospace.toTextStyle(),
                        placeholder = { Text("Enter URLs, one per line...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF005FB0),
                            unfocusedBorderColor = Color(0xFFE1E2EC)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val links = batchInput.lines().filter { it.isNotBlank() }
                            if (links.isEmpty()) {
                                Toast.makeText(context, "Please enter at least one link!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.generateBatchQRs(links) { file ->
                                if (file != null) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        type = "application/zip"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Batch QR Code ZIP"))
                                } else {
                                    Toast.makeText(context, "Failed to compile ZIP", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FB0)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Compile and Export Batch ZIP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Convert Font Family to TextStyle helper
private fun FontFamily.toTextStyle() = androidx.compose.ui.text.TextStyle(
    fontFamily = this,
    fontSize = 13.sp,
    lineHeight = 16.sp
)
