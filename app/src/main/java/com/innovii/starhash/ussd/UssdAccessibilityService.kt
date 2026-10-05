package com.innovii.starhash.ussd

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Reads the USSD pop-ups and presses their buttons, the way a tester would. It only acts while StarHash runs a
 * test ([UssdBridge.listening]).
 */
class UssdAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val snapshot = Runnable { snapshot() }

    override fun onServiceConnected() {
        UssdBridge.service = this
    }

    override fun onDestroy() {
        if (UssdBridge.service === this) UssdBridge.service = null
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!UssdBridge.listening) return
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName || DialogReader.ignoredPackage(pkg)) return
        // The pop-up changes a few times while it fills in: read it once it settles.
        handler.removeCallbacks(snapshot)
        handler.postDelayed(snapshot, SETTLE_MS)
    }

    /** Main thread. */
    fun snapshot() {
        if (!UssdBridge.listening) return
        UssdBridge.offer(DialogReader.find(this))
    }

    /** Types [text] into the pop-up's answer box. Main thread; returns an error or null. */
    fun type(text: String): String? {
        val d = DialogReader.find(this) ?: return "The USSD pop-up is gone"
        val input = d.input ?: return "The USSD pop-up has no answer box"
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        input.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return if (input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) null else "Could not type into the USSD pop-up"
    }

    /** Presses Send on the pop-up. Main thread; returns an error or null. */
    fun send(): String? {
        val d = DialogReader.find(this) ?: return "The USSD pop-up is gone"
        val send = d.sendButton() ?: return "The USSD pop-up has no Send button"
        return if (click(send.node)) null else "Could not press ${send.label}"
    }

    /** Closes the pop-up if one is open (Cancel, else OK). Main thread. */
    fun dismiss(): Boolean {
        val d = DialogReader.find(this) ?: return false
        val b = d.closeButton()
        if (b != null && click(b.node)) return true
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun click(node: AccessibilityNodeInfo): Boolean {
        var n: AccessibilityNodeInfo? = node
        while (n != null && !n.isClickable) n = n.parent
        return (n ?: node).performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    companion object {
        private const val SETTLE_MS = 350L
    }
}
