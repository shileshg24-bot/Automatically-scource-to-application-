package com.example.geminiassistant

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * After the user manually enables this in
 * Settings > Accessibility > Gemini Assistant,
 * the app can trigger basic system actions.
 * Extend performAction() to add more commands.
 */
class MyAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // React to screen changes here if needed
    }

    override fun onInterrupt() {}

    fun performAction(command: String) {
        when (command.lowercase()) {
            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }
    }
}
