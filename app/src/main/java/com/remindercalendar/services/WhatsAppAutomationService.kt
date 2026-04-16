package com.remindercalendar.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Handler
import android.os.Looper

class WhatsAppAutomationService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!AutomationBridge.isRunning) return

        // Solo actuamos si la ventana cambió o algo cambió en WhatsApp
        val rootNode = rootInActiveWindow ?: return

        val sendButton = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send").firstOrNull()
            ?: rootNode.findAccessibilityNodeInfosByText("Enviar").firstOrNull()

        if (sendButton != null && sendButton.isEnabled) {
            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            // 2. Esperamos un poco para que se envíe y volvemos atrás
            Handler(Looper.getMainLooper()).postDelayed({
                performGlobalAction(GLOBAL_ACTION_BACK)
                Handler(Looper.getMainLooper()).postDelayed({
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }, 300)
            }, 800)
        }
    }

    override fun onInterrupt() {}
}