package com.innovii.starhash.ussd

import android.content.Context
import com.innovii.starhash.core.UssdDriver
import com.innovii.starhash.core.UssdReply
import com.innovii.starhash.core.UssdText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Dials like a person and reads the pop-ups through [UssdAccessibilityService]. Follows menus. */
class ScreenDriver(private val context: Context, private val subscriptionId: Int) : UssdDriver {
    override val name = "Screen reader"
    override val supportsMenus = true

    override suspend fun dial(code: String, timeoutMs: Long): UssdReply {
        if (UssdBridge.service == null) {
            return UssdReply.Failed("The StarHash accessibility service is off: turn it on in the app's setup card")
        }
        UssdBridge.start()
        val error = withContext(Dispatchers.Main) {
            try {
                Sims.dial(context, code, subscriptionId)
                null
            } catch (e: SecurityException) {
                "StarHash may not make phone calls: allow the Phone permission"
            } catch (e: Exception) {
                "Could not dial $code: ${e.message}"
            }
        }
        if (error != null) {
            UssdBridge.stop()
            return UssdReply.Failed(error)
        }
        return await(timeoutMs)
    }

    override suspend fun reply(text: String, timeoutMs: Long): UssdReply {
        val service = UssdBridge.service ?: return UssdReply.Failed("The StarHash accessibility service stopped")
        val error = withContext(Dispatchers.Main) { service.reply(text) }
        if (error != null) return UssdReply.Failed(error)
        return await(timeoutMs)
    }

    override suspend fun close() {
        val service = UssdBridge.service
        if (service != null) {
            withContext(Dispatchers.Main) { service.dismiss() }
            delay(700)
            // A last pop-up can arrive just after Cancel ("Session ended"): close it too.
            withContext(Dispatchers.Main) { service.dismiss() }
        }
        UssdBridge.stop()
    }

    private suspend fun await(timeoutMs: Long): UssdReply {
        val text = withTimeoutOrNull(timeoutMs) {
            var got: String? = null
            while (got == null) {
                got = withTimeoutOrNull(POLL_MS) { UssdBridge.screens.receive() }
                // Some phones send no event for the pop-up: look at the screen now and then.
                if (got == null) withContext(Dispatchers.Main) { UssdBridge.service?.snapshot() }
            }
            got
        } ?: return UssdReply.Failed("No USSD answer within ${timeoutMs / 1000}s")
        val dialog = withContext(Dispatchers.Main) { UssdBridge.service?.let { DialogReader.find(it) } }
        val canReply = dialog?.takeIf { it.message == text }?.hasInput ?: UssdText.options(text).isNotEmpty()
        return UssdReply.Screen(text, canReply)
    }

    companion object {
        private const val POLL_MS = 1500L
    }
}
