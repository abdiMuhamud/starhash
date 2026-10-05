package com.innovii.starhash.ussd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.innovii.starhash.core.Sms
import com.innovii.starhash.core.SmsSource
import java.util.concurrent.CopyOnWriteArrayList

/** Keeps the SMS that arrive while the app is open: the balance SMS and the services' welcome SMS. */
object SmsWatcher : SmsSource {
    private val messages = CopyOnWriteArrayList<Sms>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val parts = runCatching { Telephony.Sms.Intents.getMessagesFromIntent(intent) }.getOrNull() ?: return
            parts.filterNotNull().groupBy { it.originatingAddress ?: "?" }.forEach { (from, list) ->
                messages += Sms(from, list.joinToString("") { it.messageBody ?: "" }, System.currentTimeMillis())
            }
            while (messages.size > 200) messages.removeAt(0)
        }
    }

    fun register(context: Context) {
        ContextCompat.registerReceiver(
            context.applicationContext, receiver, IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION),
            "android.permission.BROADCAST_SMS", null, ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun since(at: Long): List<Sms> = messages.filter { it.at >= at }
}
