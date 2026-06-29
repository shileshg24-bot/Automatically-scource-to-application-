package com.app.qr.database

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.qr.ImageProcessor
import com.app.qr.QRGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class QRViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "QRViewModel"
    private val repository: HistoryRepository

    init {
        val database = HistoryDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
    }

    val historyState: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI States
    private val _inputPayload = MutableStateFlow("https://t.me/BLOODYNIGHTMODMENU")
    val inputPayload: StateFlow<String> = _inputPayload.asStateFlow()

    private val _selectedMode = MutableStateFlow(0) // 0: Standard, 1: Stealth LSB, 2: Overlay, 3: Batch
    val selectedMode: StateFlow<Int> = _selectedMode.asStateFlow()

    private val _eccLevel = MutableStateFlow(1) // 0: L, 1: M, 2: Q, 3: H
    val eccLevel: StateFlow<Int> = _eccLevel.asStateFlow()

    private val _qrSize = MutableStateFlow(512) // 512, 1024, 2048
    val qrSize: StateFlow<Int> = _qrSize.asStateFlow()

    private val _fgColor = MutableStateFlow(0xFF001D36.toInt()) // Standard dark blue from palette
    val fgColor: StateFlow<Int> = _fgColor.asStateFlow()

    private val _bgColor = MutableStateFlow(0xFFFFFFFF.toInt()) // White
    val bgColor: StateFlow<Int> = _bgColor.asStateFlow()

    // Background Image & Overlay States
    private val _backgroundImageUri = MutableStateFlow<Uri?>(null)
    val backgroundImageUri: StateFlow<Uri?> = _backgroundImageUri.asStateFlow()

    private val _backgroundImage = MutableStateFlow<Bitmap?>(null)
    val backgroundImage: StateFlow<Bitmap?> = _backgroundImage.asStateFlow()

    private val _logoImage = MutableStateFlow<Bitmap?>(null)
    val logoImage: StateFlow<Bitmap?> = _logoImage.asStateFlow()

    private val _qrOpacity = MutableStateFlow(1.0f)
    val qrOpacity: StateFlow<Float> = _qrOpacity.asStateFlow()

    private val _qrPosX = MutableStateFlow(100f)
    val qrPosX: StateFlow<Float> = _qrPosX.asStateFlow()

    private val _qrPosY = MutableStateFlow(100f)
    val qrPosY: StateFlow<Float> = _qrPosY.asStateFlow()

    // Main Output Preview Bitmap
    private val _outputBitmap = MutableStateFlow<Bitmap?>(null)
    val outputBitmap: StateFlow<Bitmap?> = _outputBitmap.asStateFlow()

    // Processing indicator
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Status logs
    private val _statusText = MutableStateFlow<String?>(null)
    val statusText: StateFlow<String?> = _statusText.asStateFlow()

    fun setInputPayload(text: String) {
        _inputPayload.value = text
    }

    fun setSelectedMode(mode: Int) {
        _selectedMode.value = mode
        triggerGeneration()
    }

    fun setEccLevel(level: Int) {
        _eccLevel.value = level
        triggerGeneration()
    }

    fun setQrSize(size: Int) {
        _qrSize.value = size
        triggerGeneration()
    }

    fun setQRColors(fg: Int, bg: Int) {
        _fgColor.value = fg
        _bgColor.value = bg
        triggerGeneration()
    }

    fun setQrOpacity(opacity: Float) {
        _qrOpacity.value = opacity
        triggerGeneration()
    }

    fun setQrPosition(x: Float, y: Float) {
        _qrPosX.value = x
        _qrPosY.value = y
        triggerGeneration()
    }

    fun selectBackgroundImage(uri: Uri, bitmap: Bitmap) {
        _backgroundImageUri.value = uri
        _backgroundImage.value = bitmap
        triggerGeneration()
    }

    fun selectLogoImage(bitmap: Bitmap?) {
        _logoImage.value = bitmap
        triggerGeneration()
    }

    fun clearStatus() {
        _statusText.value = null
    }

    fun triggerGeneration() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val text = _inputPayload.value
                val size = _qrSize.value
                val ecc = _eccLevel.value
                val fg = _fgColor.value
                val bg = _bgColor.value
                val mode = _selectedMode.value

                withContext(Dispatchers.Default) {
                    when (mode) {
                        0 -> { // Standard Mode
                            val qrPixels = QRGenerator.generateQR(text, size, ecc, fg, bg)
                            var bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                            bitmap.setPixels(qrPixels, 0, size, 0, 0, size, size)

                            // Apply Center Logo in C++ (Overlay in Kotlin, drawing in logic)
                            val logo = _logoImage.value
                            if (logo != null) {
                                val logoScaledSize = size / 5
                                val scaledLogo = Bitmap.createScaledBitmap(logo, logoScaledSize, logoScaledSize, true)
                                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                                val canvas = Canvas(mutableBitmap)
                                canvas.drawBitmap(scaledLogo, ((size - logoScaledSize) / 2).toFloat(), ((size - logoScaledSize) / 2).toFloat(), null)
                                bitmap = mutableBitmap
                            }
                            _outputBitmap.value = bitmap
                        }
                        1 -> { // Stealth (LSB) Mode
                            val bgImg = _backgroundImage.value
                            if (bgImg != null) {
                                val resizedBg = Bitmap.createScaledBitmap(bgImg, size, size, true)
                                val bgPixels = IntArray(size * size)
                                resizedBg.getPixels(bgPixels, 0, size, 0, 0, size, size)

                                val hiddenPixels = ImageProcessor.hideQR(bgPixels, size, size, text)
                                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                                bitmap.setPixels(hiddenPixels, 0, size, 0, 0, size, size)
                                _outputBitmap.value = bitmap
                            } else {
                                // Fallback standard
                                val qrPixels = QRGenerator.generateQR(text, size, ecc, fg, bg)
                                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                                bitmap.setPixels(qrPixels, 0, size, 0, 0, size, size)
                                _outputBitmap.value = bitmap
                            }
                        }
                        2 -> { // Background + QR Overlay Mode
                            val bgImg = _backgroundImage.value
                            if (bgImg != null) {
                                val resizedBg = Bitmap.createScaledBitmap(bgImg, size, size, true)
                                val bgPixels = IntArray(size * size)
                                resizedBg.getPixels(bgPixels, 0, size, 0, 0, size, size)

                                // QR overlays inside resizedBg
                                val overlayQrSize = (size * 0.4f).toInt() // 40% of background width
                                val posX = _qrPosX.value.coerceIn(0f, (size - overlayQrSize).toFloat()).toInt()
                                val posY = _qrPosY.value.coerceIn(0f, (size - overlayQrSize).toFloat()).toInt()

                                val blendedPixels = ImageProcessor.blendQR(
                                    bgPixels, size, size, text, overlayQrSize, posX, posY, _qrOpacity.value, fg, bg
                                )
                                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                                bitmap.setPixels(blendedPixels, 0, size, 0, 0, size, size)
                                _outputBitmap.value = bitmap
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Save history item and record it in database
    fun saveToHistory() {
        viewModelScope.launch {
            val payload = _inputPayload.value
            val isStealth = _selectedMode.value == 1
            val size = _qrSize.value
            val fg = _fgColor.value
            val bg = _bgColor.value

            val item = HistoryItem(
                text = payload,
                isStealth = isStealth,
                size = size,
                fgColor = fg,
                bgColor = bg
            )
            repository.insert(item)
            _statusText.value = "Saved to SQLite History!"
        }
    }

    fun deleteHistory(item: HistoryItem) {
        viewModelScope.launch {
            repository.deleteById(item.id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // FEATURE 6: Batch Generation on sequential threads
    fun generateBatchQRs(links: List<String>, onCompleted: (File?) -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val size = _qrSize.value
                val ecc = _eccLevel.value
                val fg = _fgColor.value
                val bg = _bgColor.value

                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val zipFile = File(context.cacheDir, "Batch_QRs_${System.currentTimeMillis()}.zip")
                    ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                        links.take(5).forEachIndexed { index, link ->
                            val qrPixels = QRGenerator.generateQR(link, size, ecc, fg, bg)
                            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                            bitmap.setPixels(qrPixels, 0, size, 0, 0, size, size)

                            val bos = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
                            val bytes = bos.toByteArray()

                            val zipEntry = ZipEntry("QR_Code_${index + 1}.png")
                            zipOut.putNextEntry(zipEntry)
                            zipOut.write(bytes)
                            zipOut.closeEntry()

                            // Also insert to history
                            val item = HistoryItem(
                                text = link,
                                isStealth = false,
                                size = size,
                                fgColor = fg,
                                bgColor = bg
                            )
                            repository.insert(item)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onCompleted(zipFile)
                        _statusText.value = "Batch ZIP Generated Successfully!"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to run batch generation: ${e.message}")
                withContext(Dispatchers.Main) {
                    onCompleted(null)
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Save standard or composited Bitmap to regular Gallery
    fun saveBitmapToGallery(bitmap: Bitmap, isHidden: Boolean = false) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val rootDir = if (isHidden) {
                        // HIDDEN FOLDER: .nomedia is created
                        val hiddenDir = File(context.getExternalFilesDir(null), ".hidden_qrs")
                        if (!hiddenDir.exists()) {
                            hiddenDir.mkdirs()
                        }
                        val nomediaFile = File(hiddenDir, ".nomedia")
                        if (!nomediaFile.exists()) {
                            nomediaFile.createNewFile()
                        }
                        hiddenDir
                    } else {
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "NDK_QR_Pro")
                    }

                    if (!rootDir.exists()) {
                        rootDir.mkdirs()
                    }

                    val fileName = "QR_${System.currentTimeMillis()}.png"
                    val file = File(rootDir, fileName)
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    withContext(Dispatchers.Main) {
                        if (isHidden) {
                            _statusText.value = "Saved to Hidden Folder! (.nomedia active)"
                        } else {
                            _statusText.value = "Saved successfully to: ${file.absolutePath}"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Save failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    _statusText.value = "Save Failed: ${e.message}"
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Convert output bitmap to Base64
    fun shareAsBase64(bitmap: Bitmap, onCompleted: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
            val bytes = bos.toByteArray()
            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
            withContext(Dispatchers.Main) {
                onCompleted(base64String)
            }
        }
    }
}
