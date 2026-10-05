package com.innovii.starhash.ussd

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import com.innovii.starhash.core.UssdDriver
import com.innovii.starhash.core.UssdReply
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Android's own USSD API: one request, one answer, no pop-up and no accessibility service. Menus are sent as one
 * chained code (*400*4*1#), which works only where the network accepts chained codes.
 */
class OneShotDriver(private val context: Context, private val subscriptionId: Int) : UssdDriver {
    override val name = "One-shot API"
    override val supportsMenus = false

    @SuppressLint("MissingPermission")
    override suspend fun dial(code: String, timeoutMs: Long): UssdReply =
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                try {
                    val base = context.getSystemService(TelephonyManager::class.java)
                        ?: throw IllegalStateException("this device has no telephony")
                    val tm = if (subscriptionId >= 0) base.createForSubscriptionId(subscriptionId) else base
                    tm.sendUssdRequest(
                        code,
                        object : TelephonyManager.UssdResponseCallback() {
                            override fun onReceiveUssdResponse(t: TelephonyManager, request: String, response: CharSequence) {
                                if (cont.isActive) cont.resume(UssdReply.Screen(response.toString(), false))
                            }

                            override fun onReceiveUssdResponseFailed(t: TelephonyManager, request: String, failureCode: Int) {
                                val why = when (failureCode) {
                                    TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL -> "USSD service unavailable"
                                    else -> "USSD request failed (code $failureCode)"
                                }
                                if (cont.isActive) cont.resume(UssdReply.Failed(why))
                            }
                        },
                        Handler(Looper.getMainLooper()),
                    )
                } catch (e: SecurityException) {
                    if (cont.isActive) cont.resume(UssdReply.Failed("StarHash may not make phone calls: allow the Phone permission"))
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(UssdReply.Failed("USSD request failed: ${e.message}"))
                }
            }
        } ?: UssdReply.Failed("No USSD answer within ${timeoutMs / 1000}s")

    override suspend fun reply(text: String, timeoutMs: Long): UssdReply =
        UssdReply.Failed("The one-shot engine cannot answer menus")

    override suspend fun close() = Unit
}
