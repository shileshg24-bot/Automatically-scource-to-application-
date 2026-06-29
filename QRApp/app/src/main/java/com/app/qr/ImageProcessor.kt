package com.app.qr

object ImageProcessor {
    init {
        System.loadLibrary("native-lib")
    }

    external fun blendQR(
        bgPixels: IntArray,
        bgWidth: Int,
        bgHeight: Int,
        qrText: String,
        qrSize: Int,
        posX: Int,
        posY: Int,
        opacity: Float,
        fgColor: Int,
        bgColor: Int
    ): IntArray

    external fun hideQR(
        bgPixels: IntArray,
        bgWidth: Int,
        bgHeight: Int,
        qrText: String
    ): IntArray

    external fun adjustOpacity(
        qrPixels: IntArray,
        width: Int,
        height: Int,
        opacityLevel: Float
    ): IntArray
}
