package com.innovii.starhash.ussd

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

/** A USSD pop-up as read from the screen. */
class UssdDialog(
    val message: String,
    val packageName: String,
    val input: AccessibilityNodeInfo?,
    val buttons: List<Button>,
) {
    class Button(val label: String, val viewId: String, val node: AccessibilityNodeInfo)

    val hasInput: Boolean get() = input != null

    fun sendButton(): Button? =
        buttons.firstOrNull { it.viewId.endsWith("button1") && !isCancel(it.label) }
            ?: buttons.firstOrNull { isSend(it.label) }
            ?: buttons.lastOrNull { !isCancel(it.label) }

    fun closeButton(): Button? =
        buttons.firstOrNull { isCancel(it.label) }
            ?: buttons.firstOrNull { it.viewId.endsWith("button2") }
            ?: buttons.firstOrNull { isOk(it.label) }
            ?: buttons.firstOrNull()

    companion object {
        private val sendWords = listOf("send", "reply", "submit", "answer", "respond", "dir", "ok", "yes", "haa", "enter")
        private val cancelWords = listOf("cancel", "dismiss", "close", "jooji", "ka noqo", "no", "maya", "end")
        private val okWords = listOf("ok", "done", "close")

        private fun norm(s: String) = s.trim().lowercase()
        fun isSend(s: String) = sendWords.any { norm(s) == it || norm(s).startsWith("$it ") }
        fun isCancel(s: String) = cancelWords.any { norm(s) == it || norm(s).startsWith("$it ") }
        fun isOk(s: String) = okWords.any { norm(s) == it }
    }
}

/** Finds the USSD pop-up among the windows on screen and reads its text, answer box and buttons. */
object DialogReader {
    private val phonePackages = listOf("phone", "telecom", "dialer", "incall", "ussd", "stk", "telephony", "contacts", "mms")
    private val ignored = listOf("systemui", "launcher", "inputmethod", "keyboard", "honeyboard", "gboard")

    fun ignoredPackage(pkg: String): Boolean = ignored.any { pkg.contains(it) }

    fun find(service: AccessibilityService): UssdDialog? {
        val roots = mutableListOf<AccessibilityNodeInfo>()
        runCatching { service.rootInActiveWindow }.getOrNull()?.let(roots::add)
        runCatching { service.windows }.getOrNull().orEmpty()
            .filter { it.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD && it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
            .mapNotNullTo(roots) { runCatching { it.root }.getOrNull() }
        var fallback: UssdDialog? = null
        for (root in roots) {
            val pkg = root.packageName?.toString() ?: continue
            if (pkg == service.packageName || ignoredPackage(pkg)) continue
            val d = read(root, pkg) ?: continue
            if (phonePackages.any { pkg.contains(it) }) return d
            if (fallback == null && d.buttons.any { UssdDialog.isSend(it.label) || UssdDialog.isCancel(it.label) }) fallback = d
        }
        return fallback
    }

    private fun read(root: AccessibilityNodeInfo, pkg: String): UssdDialog? {
        val texts = mutableListOf<String>()
        var messageById: String? = null
        var input: AccessibilityNodeInfo? = null
        val buttons = mutableListOf<UssdDialog.Button>()
        var count = 0

        fun visit(n: AccessibilityNodeInfo, depth: Int) {
            if (depth > 30 || ++count > 400) return
            val cls = n.className?.toString().orEmpty()
            val id = n.viewIdResourceName.orEmpty()
            val text = n.text?.toString()?.trim().orEmpty()
            when {
                n.isEditable || cls.endsWith("EditText") -> if (input == null) input = n
                cls.endsWith("Button") -> if (text.isNotEmpty()) buttons += UssdDialog.Button(text, id, n)
                text.isNotEmpty() && n.isClickable && text.length <= 20 &&
                    (UssdDialog.isSend(text) || UssdDialog.isCancel(text)) -> buttons += UssdDialog.Button(text, id, n)
                text.isNotEmpty() -> {
                    if (id.endsWith("id/message")) messageById = text
                    texts += text
                }
            }
            for (i in 0 until n.childCount) {
                val child = runCatching { n.getChild(i) }.getOrNull() ?: continue
                visit(child, depth + 1)
            }
        }
        visit(root, 0)

        val message = messageById ?: texts.joinToString("\n")
        if (message.isBlank()) return null
        if (buttons.isEmpty() && input == null && count > 60) return null // a whole app screen, not a pop-up
        return UssdDialog(message, pkg, input, buttons)
    }
}
