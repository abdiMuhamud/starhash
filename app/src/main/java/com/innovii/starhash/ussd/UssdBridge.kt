package com.innovii.starhash.ussd

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.innovii.starhash.core.UssdText
import kotlinx.coroutines.channels.Channel

/** Hands the pop-ups the accessibility service reads to the test that is waiting for them. */
object UssdBridge {
    @Volatile
    var service: UssdAccessibilityService? = null

    /** True while a test waits for a pop-up; the service ignores the screen otherwise. */
    @Volatile
    var listening = false
        private set

    @Volatile
    private var lastText: String? = null
    val screens = Channel<String>(Channel.UNLIMITED)

    /** Before dialling: forget the last pop-up and start listening. */
    fun start() {
        while (screens.tryReceive().isSuccess) Unit
        lastText = null
        listening = true
    }

    fun stop() {
        listening = false
    }

    /** Called on the main thread with what is on screen now (null: no pop-up). */
    fun offer(dialog: UssdDialog?) {
        if (!listening) return
        if (dialog == null || UssdText.isProgress(dialog.message)) {
            lastText = null // the pop-up went away or is loading: the next answer is new even if it reads the same
            return
        }
        if (dialog.message == lastText) return
        lastText = dialog.message
        screens.trySend(dialog.message)
    }

    fun isEnabled(context: Context): Boolean {
        val me = ComponentName(context, UssdAccessibilityService::class.java)
        val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return list.split(':').any { ComponentName.unflattenFromString(it) == me }
    }
}
