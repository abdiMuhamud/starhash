package com.innovii.starhash.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.innovii.starhash.core.Engine
import com.innovii.starhash.core.Status
import com.innovii.starhash.ussd.UssdBridge

val PERMISSIONS = arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.RECEIVE_SMS)

data class SetupState(val phone: Boolean, val sms: Boolean, val accessibility: Boolean) {
    fun readyFor(engine: Engine): Boolean = when (engine) {
        Engine.DEMO -> true
        Engine.ONE_SHOT -> phone
        Engine.SCREEN -> phone && accessibility
    }

    companion object {
        fun read(context: Context): SetupState {
            fun has(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
            return SetupState(
                phone = has(Manifest.permission.CALL_PHONE) && has(Manifest.permission.READ_PHONE_STATE),
                sms = has(Manifest.permission.RECEIVE_SMS),
                accessibility = UssdBridge.isEnabled(context),
            )
        }
    }
}

fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun openAppInfo(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@Composable
fun SetupCard(state: SetupState, engine: Engine, onAskPermissions: () -> Unit, context: Context) {
    WhiteCard {
        Column(Modifier.padding(16.dp)) {
            Text("Get the phone ready", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                "StarHash dials the codes and reads the answers for you. It needs these once.",
                fontSize = 13.sp, color = Sh.Muted,
            )
            SetupRow(
                done = state.phone, title = "Phone", text = "Dial *122# and *400# and choose the SIM",
                action = "Allow", onAction = onAskPermissions,
            )
            SetupRow(
                done = state.sms, title = "SMS", text = "Read the balance SMS and the welcome SMS (optional)",
                action = "Allow", onAction = onAskPermissions,
            )
            if (engine == Engine.SCREEN) {
                SetupRow(
                    done = state.accessibility, title = "Accessibility",
                    text = "Settings → Accessibility → StarHash USSD tester → On. It reads the USSD pop-ups and answers the menus.",
                    action = "Turn on", onAction = { openAccessibilitySettings(context) },
                )
                if (!state.accessibility) {
                    Text(
                        (if (Build.VERSION.SDK_INT >= 33) {
                            "Switch greyed out? Open App info → ⋮ → Allow restricted settings, then try again. "
                        } else {
                            ""
                        }) + "On Xiaomi, Redmi, Tecno and Infinix also allow Autostart and set Battery saver to No restrictions, " +
                            "or the phone may switch the service off.",
                        fontSize = 12.sp, color = Sh.Muted, modifier = Modifier.padding(top = 8.dp),
                    )
                    TextButton(onClick = { openAppInfo(context) }) { Text("Open App info") }
                }
            }
        }
    }
}

@Composable
private fun SetupRow(done: Boolean, title: String, text: String, action: String, onAction: () -> Unit) {
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        StatusIcon(if (done) Status.PASS else Status.WARN, 26.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(text, fontSize = 12.sp, color = Sh.Muted)
        }
        if (!done) {
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onAction) { Text(action) }
        }
    }
}
