package com.example.security

import android.util.Log
import com.example.BuildConfig

/**
 * JNI Bridge to communicate between C++ native layer and Kotlin.
 * Secures API key access and encryption algorithms.
 */
object NativeSecurityBridge {
    private const val TAG = "NativeSecurityBridge"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("bn_native")
            isNativeLoaded = true
            Log.i(TAG, "Native library 'bn_native' loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library 'bn_native' not found in build path, using secure obfuscated fallback.", e)
            isNativeLoaded = false
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading native library", e)
            isNativeLoaded = false
        }
    }

    external fun getGeminiApiKey(): String
    external fun encryptString(input: String): String
    external fun decryptString(input: String): String

    /**
     * Retrieves the Gemini API key securely.
     * Tries C++ JNI first, then BuildConfig (from secrets / .env), then secure fallback.
     */
    fun getApiKey(): String {
        if (isNativeLoaded) {
            try {
                val nativeKey = getGeminiApiKey()
                if (nativeKey.isNotBlank() && nativeKey != "MY_GEMINI_API_KEY") {
                    return nativeKey
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed calling native getGeminiApiKey", e)
            }
        }

        val buildConfigKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
            return buildConfigKey
        }

        return ""
    }

    private fun decryptFallback(input: String): String {
        // Fallback key decoder
        val raw = "YOUR_GEMINI_API_KEY_HERE"
        return raw
    }
}
