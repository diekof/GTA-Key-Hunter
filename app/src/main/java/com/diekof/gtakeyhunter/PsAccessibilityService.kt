package com.diekof.gtakeyhunter
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
/** Somente observação assistida; não confirma compras ou resgates. */
class PsAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}