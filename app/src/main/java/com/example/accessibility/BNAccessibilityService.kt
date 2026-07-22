package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility Service providing hands-free mobile device controls
 * such as tap simulation, app launching, screen navigation, and volume controls.
 */
class BNAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "BN Accessibility Service Connected")

        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event monitoring if needed
    }

    override fun onInterrupt() {
        Log.w(TAG, "BN Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        private const val TAG = "BNAccessibilityService"
        private var instance: BNAccessibilityService? = null

        fun isEnabled(context: Context): Boolean {
            val accessibilityEnabled = try {
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED
                )
            } catch (e: Exception) {
                0
            }
            if (accessibilityEnabled == 1) {
                val service = "${context.packageName}/${BNAccessibilityService::class.java.canonicalName}"
                val settingValue = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                return settingValue?.contains(service, ignoreCase = true) == true
            }
            return false
        }

        fun getInstance(): BNAccessibilityService? = instance

        fun performHome(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        }

        fun performBack(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
        }

        fun performRecents(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) ?: false
        }

        fun performNotifications(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false
        }

        fun performQuickSettings(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) ?: false
        }

        fun performScreenshot(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                instance?.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) ?: false
            } else {
                false
            }
        }

        fun adjustVolume(context: Context, raise: Boolean): Boolean {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            val direction = if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            return true
        }

        fun clickText(targetText: String): Boolean {
            val root = instance?.rootInActiveWindow ?: return false
            val nodes = root.findAccessibilityNodeInfosByText(targetText)
            for (node in nodes) {
                if (node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                    parent = parent.parent
                }
            }
            return false
        }

        fun scroll(forward: Boolean): Boolean {
            val root = instance?.rootInActiveWindow ?: return false
            val scrollable = findScrollableNode(root) ?: return false
            val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            return scrollable.performAction(action)
        }

        private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = findScrollableNode(child)
                if (found != null) return found
            }
            return null
        }

        fun openApp(context: Context, appName: String): String {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            
            // Map common app names
            val nameClean = appName.lowercase().trim()
            val targetPackage = when {
                nameClean.contains("youtube") -> "com.google.android.youtube"
                nameClean.contains("chrome") || nameClean.contains("browser") -> "com.android.chrome"
                nameClean.contains("whatsapp") -> "com.whatsapp"
                nameClean.contains("camera") -> "com.google.android.GoogleCamera"
                nameClean.contains("setting") -> "com.android.settings"
                nameClean.contains("map") -> "com.google.android.apps.maps"
                nameClean.contains("gmail") || nameClean.contains("mail") -> "com.google.android.gm"
                nameClean.contains("play store") || nameClean.contains("store") -> "com.android.vending"
                nameClean.contains("clock") || nameClean.contains("alarm") -> "com.google.android.deskclock"
                else -> {
                    packages.find { app ->
                        val label = pm.getApplicationLabel(app).toString().lowercase()
                        label.contains(nameClean) || nameClean.contains(label)
                    }?.packageName
                }
            }

            if (targetPackage != null) {
                val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "Opening ${appName.replaceFirstChar { it.uppercase() }}"
                }
            }

            // Fallback launch intent search
            val queryIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(queryIntent, 0)
            for (info in resolveInfos) {
                val label = info.loadLabel(pm).toString().lowercase()
                if (label.contains(nameClean) || nameClean.contains(label)) {
                    val intent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return "Opening ${info.loadLabel(pm)}"
                    }
                }
            }

            return "Could not find app named '$appName' on device."
        }
    }
}
