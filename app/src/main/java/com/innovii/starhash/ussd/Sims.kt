package com.innovii.starhash.ussd

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class SimInfo(val subscriptionId: Int, val slot: Int, val label: String)

/** The phone's SIMs, and dialling a USSD code on the chosen one. */
object Sims {
    private fun has(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun list(context: Context): List<SimInfo> {
        if (!has(context, Manifest.permission.READ_PHONE_STATE)) return emptyList()
        val sm = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        return runCatching {
            sm.activeSubscriptionInfoList.orEmpty().map {
                val name = (it.carrierName ?: it.displayName ?: "").toString().ifBlank { "SIM" }
                SimInfo(it.subscriptionId, it.simSlotIndex, "SIM ${it.simSlotIndex + 1} · $name")
            }.sortedBy { it.slot }
        }.getOrDefault(emptyList())
    }

    fun label(context: Context, subscriptionId: Int): String =
        list(context).firstOrNull { it.subscriptionId == subscriptionId }?.label
            ?: list(context).singleOrNull()?.label
            ?: "Phone default SIM"

    /** Dials [code] like the phone app would; the network's answer comes back as a pop-up. */
    @SuppressLint("MissingPermission")
    fun dial(context: Context, code: String, subscriptionId: Int) {
        val telecom = context.getSystemService(TelecomManager::class.java)
            ?: throw IllegalStateException("This device cannot make calls")
        val extras = Bundle()
        handleFor(context, subscriptionId)?.let { extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it) }
        telecom.placeCall(Uri.fromParts("tel", code, null), extras)
    }

    @SuppressLint("MissingPermission")
    private fun handleFor(context: Context, subscriptionId: Int): PhoneAccountHandle? {
        if (subscriptionId < 0 || !has(context, Manifest.permission.READ_PHONE_STATE)) return null
        return runCatching {
            val telecom = context.getSystemService(TelecomManager::class.java) ?: return null
            val accounts = telecom.callCapablePhoneAccounts
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val tm = context.getSystemService(TelephonyManager::class.java)
                accounts.firstOrNull { tm?.getSubscriptionId(it) == subscriptionId }?.let { return it }
            }
            val info = context.getSystemService(SubscriptionManager::class.java)?.getActiveSubscriptionInfo(subscriptionId)
            val iccId = runCatching { info?.iccId }.getOrNull().orEmpty()
            accounts.firstOrNull { it.id == subscriptionId.toString() || (iccId.isNotBlank() && it.id.startsWith(iccId)) }
        }.getOrNull()
    }
}
