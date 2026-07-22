package com.example.device

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import android.util.Log
import com.example.accessibility.BNAccessibilityService
import com.example.api.GeminiApiClient
import com.example.api.GeminiContent
import com.example.api.GeminiGenerationConfig
import com.example.api.GeminiPart
import com.example.api.GeminiRequest
import com.example.security.NativeSecurityBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class VoiceCommandResult {
    data class ActionExecuted(val actionName: String, val spokenFeedback: String) : VoiceCommandResult()
    data class AiResponse(val spokenText: String) : VoiceCommandResult()
    data class Error(val message: String) : VoiceCommandResult()
}

/**
 * Complete and Final VoiceAssistantRepository handling full device control,
 * dynamic app launching, YouTube searches, and Gemini Live style natural chat.
 */
class VoiceAssistantRepository(private val context: Context) {

    private val systemInstruction = GeminiContent(
        role = null,
        parts = listOf(
            GeminiPart(
                text = "You are BN AI, an advanced, intelligent, highly capable AI Voice Assistant like Gemini Live or Jarvis. " +
                        "CRITICAL RULES: " +
                        "1. NEVER repeat, echo, or paraphrase what the user said. " +
                        "2. Directly answer questions, chat naturally, and respond in conversational Hindi or English as spoken. " +
                        "3. Keep your responses concise, direct, helpful, and friendly (1 to 3 sentences maximum)."
            )
        )
    )

    suspend fun processUserCommand(
        input: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): VoiceCommandResult = withContext(Dispatchers.IO) {
        val cleanInput = input.trim().lowercase()

        // 1. Try local system actions first for instant execution (Apps, YouTube, Torch, WiFi, etc.)
        val directAction = tryLocalDeviceAction(cleanInput, input)
        if (directAction != null) {
            return@withContext directAction
        }

        // 2. Fallback to Gemini AI for natural conversational replies
        val apiKey = NativeSecurityBridge.getApiKey()
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val contentsList = mutableListOf<GeminiContent>()
                conversationHistory.takeLast(6).forEach { (user, model) ->
                    contentsList.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = user))))
                    contentsList.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = model))))
                }
                contentsList.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = input))))

                val request = GeminiRequest(
                    contents = contentsList,
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.8f,
                        maxOutputTokens = 300
                    ),
                    systemInstruction = systemInstruction
                )

                val response = GeminiApiClient.service.generateContent(apiKey, request)
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!replyText.isNullOrBlank()) {
                    return@withContext VoiceCommandResult.AiResponse(replyText.trim())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API error, falling back to local fallback chat", e)
            }
        }

        // 3. Local conversational fallback if API is not configured
        val fallbackResponse = getLocalConversationalReply(cleanInput)
        return@withContext VoiceCommandResult.AiResponse(fallbackResponse)
    }

    private fun getLocalConversationalReply(input: String): String {
        return when {
            input.contains("kaise ho") || input.contains("how are you") ->
                "Main ekdum badhiya hoon! Boliye, aaj aapki kya madad karoon?"
            input.contains("hello") || input.contains("hi") || input.contains("hey") ->
                "Hello! Main BN AI hoon. Aap mujhse kuch bhi pooch sakte hain ya device control karwa sakte hain."
            input.contains("who are you") || input.contains("tum kaun ho") || input.contains("aap kaun ho") ->
                "Main aapka personal AI assistant hoon, jo aapse baat bhi kar sakta hai aur aapke phone ke apps ya commands bhi chala sakta hai."
            input.contains("joke") || input.contains("chutkule") ->
                "Sunayein: Ek machhar jab pehli baar cinema hall gaya, toh usne socha ki sab log uske liye taaliyan baja rahe hain!"
            input.contains("thank") || input.contains("shukriya") ->
                "Aapka swagat hai! Agar aur kuch chahiye ho toh batayiyega."
            else ->
                "Main aapki baat samajh raha hoon. Boliye, ispar aur kya karna hai?"
        }
    }

    private fun tryLocalDeviceAction(cleanInput: String, rawInput: String): VoiceCommandResult? {
        // --- 1. Dynamic YouTube Search & Play ---
        if (cleanInput.contains("youtube") || cleanInput.startsWith("play ") || cleanInput.contains("search for") || cleanInput.contains("search karo")) {
            val query = cleanInput
                .replace("on youtube", "")
                .replace("youtube par", "")
                .replace("youtube", "")
                .replace("search for", "")
                .replace("search karo", "")
                .replace("search", "")
                .replace("play", "")
                .trim()
            
            if (query.isNotBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return VoiceCommandResult.ActionExecuted("YouTube", "Searching for $query on YouTube.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open YouTube search", e)
                }
            } else {
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                        ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(intent)
                    return VoiceCommandResult.ActionExecuted("YouTube", "Opening YouTube.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open YouTube app", e)
                }
            }
        }

        // --- 2. Dynamic App Opening (Telegram, WhatsApp, Instagram, etc.) ---
        if (cleanInput.startsWith("open ") || cleanInput.startsWith("launch ") || cleanInput.startsWith("start ") || cleanInput.startsWith("kholo ")) {
            val appName = rawInput
                .replace(Regex("(?i)^(open|launch|start|kholo)\\s+"), "")
                .replace("app", "")
                .trim()
            
            if (appName.isNotBlank()) {
                val result = BNAccessibilityService.openApp(context, appName)
                return VoiceCommandResult.ActionExecuted("Open App", result)
            }
        }

        // --- 3. Torch / Flashlight Control ---
        if (cleanInput.contains("torch on") || cleanInput.contains("flashlight on") || cleanInput.contains("light on") || cleanInput.contains("torch chalu") || cleanInput.contains("light chalu")) {
            setTorchState(true)
            return VoiceCommandResult.ActionExecuted("Torch", "Turning torch on.")
        }
        if (cleanInput.contains("torch off") || cleanInput.contains("flashlight off") || cleanInput.contains("light off") || cleanInput.contains("torch band") || cleanInput.contains("light band")) {
            setTorchState(false)
            return VoiceCommandResult.ActionExecuted("Torch", "Turning torch off.")
        }

        // --- 4. Wi-Fi Settings Toggle ---
        if (cleanInput.contains("wifi") || cleanInput.contains("wi-fi")) {
            try {
                val panelIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(panelIntent)
                return VoiceCommandResult.ActionExecuted("WiFi", "Opening Wi-Fi settings.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open wifi settings", e)
            }
        }

        // --- 5. Bluetooth Settings Toggle ---
        if (cleanInput.contains("bluetooth")) {
            try {
                val panelIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(panelIntent)
                return VoiceCommandResult.ActionExecuted("Bluetooth", "Opening Bluetooth settings.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open bluetooth settings", e)
            }
        }

        // --- 6. System Navigation (Home, Back, Recents) ---
        if (cleanInput == "go home" || cleanInput == "home screen" || cleanInput == "open home" || cleanInput == "home") {
            val success = BNAccessibilityService.performHome()
            val msg = if (success) "Navigating to home screen" else "Opening home screen"
            if (!success) {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
            return VoiceCommandResult.ActionExecuted("Home", msg)
        }

        if (cleanInput == "go back" || cleanInput == "back" || cleanInput == "peeche jao") {
            val success = BNAccessibilityService.performBack()
            val msg = if (success) "Going back" else "Please enable Accessibility Service for back control."
            return VoiceCommandResult.ActionExecuted("Back", msg)
        }

        if (cleanInput.contains("recent apps") || cleanInput.contains("show recents") || cleanInput == "recents") {
            val success = BNAccessibilityService.performRecents()
            val msg = if (success) "Showing recent apps" else "Please enable Accessibility Service for recents control."
            return VoiceCommandResult.ActionExecuted("Recents", msg)
        }

        // --- 7. Volume Control ---
        if (cleanInput.contains("volume up") || cleanInput.contains("increase volume") || cleanInput == "louder" || cleanInput.contains("awaz badhao")) {
            BNAccessibilityService.adjustVolume(context, raise = true)
            return VoiceCommandResult.ActionExecuted("Volume Up", "Increased media volume.")
        }

        if (cleanInput.contains("volume down") || cleanInput.contains("decrease volume") || cleanInput == "quieter" || cleanInput.contains("awaz kam karo")) {
            BNAccessibilityService.adjustVolume(context, raise = false)
            return VoiceCommandResult.ActionExecuted("Volume Down", "Decreased media volume.")
        }

        // --- 8. Utility Info (Time, Date, Battery, Screenshot) ---
        if (cleanInput.contains("what time is it") || cleanInput.contains("current time") || cleanInput == "time" || cleanInput.contains("samay kya hai")) {
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            return VoiceCommandResult.AiResponse("The current time is $timeStr.")
        }

        if (cleanInput.contains("what day is it") || cleanInput.contains("today's date") || cleanInput == "date" || cleanInput.contains("aaj ki tareekh")) {
            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
            return VoiceCommandResult.AiResponse("Today is $dateStr.")
        }

        if (cleanInput.contains("battery level") || cleanInput.contains("battery status") || cleanInput.contains("how much battery")) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val reply = if (batLevel >= 0) "Your device battery level is at $batLevel percent." else "Battery information is currently unavailable."
            return VoiceCommandResult.AiResponse(reply)
        }

        if (cleanInput.contains("take screenshot") || cleanInput.contains("screen shot")) {
            val success = BNAccessibilityService.performScreenshot()
            val msg = if (success) "Taking screenshot" else "Please enable Accessibility Service for screenshot capture."
            return VoiceCommandResult.ActionExecuted("Screenshot", msg)
        }

        return null
    }

    private fun setTorchState(enabled: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enabled)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting torch state", e)
        }
    }

    companion object {
        private const val TAG = "VoiceAssistantRepository"
    }
}
