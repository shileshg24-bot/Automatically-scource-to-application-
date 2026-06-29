package com.app.qr

object QRGenerator {
    init {
        System.loadLibrary("native-lib")
    }

    external fun generateQR(
        text: String,
        size: Int,
        eccLevel: Int,
        fgColor: Int,
        bgColor: Int
    ): IntArray
}
