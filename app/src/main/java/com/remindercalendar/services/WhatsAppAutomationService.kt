package com.remindercalendar.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Handler
import android.os.Looper

class WhatsAppAutomationService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!AutomationBridge.isRunning) return

        val rootNode = rootInActiveWindow ?: return

        val sendButtonIds = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp.w4b:id/send"
        )

        var sendButton: AccessibilityNodeInfo? = null

        for (id in sendButtonIds) {
            sendButton = rootNode.findAccessibilityNodeInfosByViewId(id).firstOrNull()
            if (sendButton != null) break
        }

        if (sendButton == null) {
            sendButton = rootNode.findAccessibilityNodeInfosByText("Enviar").firstOrNull()
                ?: rootNode.findAccessibilityNodeInfosByText("Send").firstOrNull()
        }

        if (sendButton != null && sendButton.isEnabled) {
            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            Handler(Looper.getMainLooper()).postDelayed({

                performGlobalAction(GLOBAL_ACTION_BACK)

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 500)
            }, 1000)
        }
    }
    override fun onInterrupt() {}
}