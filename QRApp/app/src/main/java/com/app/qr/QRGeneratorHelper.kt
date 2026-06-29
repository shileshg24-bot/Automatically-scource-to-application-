package com.app.qr

import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import android.util.Log

object QRGeneratorHelper {
    private const val TAG = "QRGeneratorHelper"

    @JvmStatic
    fun generateQRMatrix(text: String, eccLevelInt: Int): Array<BooleanArray> {
        Log.d(TAG, "generateQRMatrix called with text length: ${text.length}, eccLevel: $eccLevelInt")
        val eccLevel = when (eccLevelInt) {
            0 -> ErrorCorrectionLevel.L
            1 -> ErrorCorrectionLevel.M
            2 -> ErrorCorrectionLevel.Q
            else -> ErrorCorrectionLevel.H
        }
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.ERROR_CORRECTION] = eccLevel
        
        return try {
            val qrCode = Encoder.encode(text, eccLevel, hints)
            val byteMatrix = qrCode.matrix
            val width = byteMatrix.width
            val height = byteMatrix.height
            val result = Array(height) { BooleanArray(width) }
            for (y in 0 until height) {
                for (x in 0 until width) {
                    result[y][x] = byteMatrix.get(x, y).toInt() == 1
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate QR Matrix: ${e.message}")
            Array(21) { BooleanArray(21) }
        }
    }
}
