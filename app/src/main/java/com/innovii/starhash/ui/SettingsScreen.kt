package com.innovii.starhash.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovii.starhash.core.Appearance
import com.innovii.starhash.core.Engine
import com.innovii.starhash.core.Pace
import com.innovii.starhash.core.Settings
import com.innovii.starhash.core.UssdText
import com.innovii.starhash.ussd.SimInfo

@Composable
fun SettingsScreen(
    settings: Settings,
    sims: List<SimInfo>,
    version: String,
    demoBalance: Double,
    onChange: ((Settings) -> Settings) -> Unit,
    onResetDemo: () -> Unit,
    onRestoreTests: () -> Unit,
) {
    var confirmRestore by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().imePadding()) {
        PageHeader("Settings", onBack = null)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionTitle("How to reach the network")
            WhiteCard {
                Column(Modifier.padding(vertical = 6.dp)) {
                    EngineRow(Engine.SCREEN, settings.engine, "Dials like a person and answers the menus in the pop-ups. Needs the accessibility service. Best for *400#.", onChange)
                    EngineRow(Engine.ONE_SHOT, settings.engine, "Android's USSD API, no pop-ups. Sends menus as one code (*400*4*1#), so only for networks that accept chained codes.", onChange)
                    EngineRow(Engine.DEMO, settings.engine, "A copy of the Telesom menus inside the app. No SIM, no charge: for training and demos.", onChange)
                }
            }

            SectionTitle("Pace", "How fast StarHash answers the menus")
            WhiteCard {
                Column(Modifier.padding(vertical = 6.dp)) {
                    Choice(
                        Pace.HUMAN.label,
                        "Like a customer: reads each menu for a few seconds, types the answer, presses Send, and rests at least " +
                            "${Settings.HUMAN_MIN_PAUSE_SEC} s between codes. Best for the network and easy to follow.",
                        settings.pace == Pace.HUMAN,
                    ) { onChange { it.copy(pace = Pace.HUMAN) } }
                    Choice(Pace.FAST.label, "Answers as soon as a menu appears.", settings.pace == Pace.FAST) {
                        onChange { it.copy(pace = Pace.FAST) }
                    }
                }
            }
            if (settings.pace == Pace.HUMAN) {
                Spacer(Modifier.height(8.dp))
                NumberSetting("Reading time per menu", settings.readSec, 1..20) { v -> onChange { it.copy(readSec = v) } }
            }

            SectionTitle("SIM")
            WhiteCard {
                Column(Modifier.padding(vertical = 6.dp)) {
                    Choice("Phone default", "The SIM the phone uses for calls", settings.subscriptionId == -1) { onChange { s -> s.copy(subscriptionId = -1) } }
                    for (sim in sims) {
                        Choice(sim.label, "Subscription ${sim.subscriptionId}", settings.subscriptionId == sim.subscriptionId) {
                            onChange { s -> s.copy(subscriptionId = sim.subscriptionId) }
                        }
                    }
                    if (sims.isEmpty()) Text("Allow the Phone permission to choose a SIM.", fontSize = 12.sp, color = Sh.Muted, modifier = Modifier.padding(16.dp, 4.dp))
                }
            }

            SectionTitle("Balance", "How StarHash reads the balance before and after subscribing")
            TextSetting("Balance code", settings.balanceCode, KeyboardType.Phone) { v -> onChange { it.copy(balanceCode = UssdText.normalizeCode(v)) } }
            TextSetting("Menu answers", settings.balancePath.joinToString(" "), KeyboardType.Phone, "*122# → 1 Prepaid balance") { v ->
                onChange { it.copy(balancePath = UssdText.parsePath(v)) }
            }
            TextSetting("Balance pattern (advanced)", settings.balancePattern, KeyboardType.Text, "Regular expression; the first group is the amount. Empty: “Balance: 0.02USD” and “Hadhagaagu hadda waa: 0.02” are understood.") { v ->
                onChange { it.copy(balancePattern = v) }
            }
            TextSetting("Currency", settings.currency, KeyboardType.Text) { v -> onChange { it.copy(currency = v.trim().ifBlank { "USD" }) } }

            SectionTitle("Timing", "Seconds")
            NumberSetting("Wait for each pop-up", settings.screenTimeoutSec, 5..120) { v -> onChange { it.copy(screenTimeoutSec = v) } }
            NumberSetting("Wait for the charge after subscribing", settings.chargeWaitSec, 0..600) { v -> onChange { it.copy(chargeWaitSec = v) } }
            NumberSetting("Balance checks after subscribing", settings.chargeChecks, 1..10) { v -> onChange { it.copy(chargeChecks = v) } }
            NumberSetting("Pause between USSD sessions", settings.pauseSec, 0..60) { v -> onChange { it.copy(pauseSec = v) } }
            NumberSetting("Wait for the balance SMS", settings.smsWaitSec, 0..120) { v -> onChange { it.copy(smsWaitSec = v) } }

            SectionTitle("Appearance")
            WhiteCard {
                Column(Modifier.padding(vertical = 6.dp)) {
                    for (a in Appearance.entries) {
                        Choice(
                            a.label,
                            when (a) {
                                Appearance.DARK -> "Deep navy, soft text"
                                Appearance.LIGHT -> "Grey-blue, for bright daylight"
                                Appearance.PHONE -> "Dark or light, as the phone is set"
                            },
                            settings.appearance == a,
                        ) { onChange { it.copy(appearance = a) } }
                    }
                }
            }

            SectionTitle("Demo network")
            WhiteCard {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Demo balance ${UssdText.money(demoBalance)} USD", fontWeight = FontWeight.SemiBold)
                        Text("Reset to 1.00 USD and no subscriptions", fontSize = 12.sp, color = Sh.Muted)
                    }
                    OutlinedButton(onClick = onResetDemo) { Text("Reset") }
                }
            }

            SectionTitle("Tests")
            OutlinedButton(onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) { Text("Restore the default tests") }

            Spacer(Modifier.height(24.dp))
            Text("StarHash $version · INNOVII", fontSize = 12.sp, color = Sh.Muted)
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore the default tests?") },
            text = { Text("Your tests are replaced by the *122# and *400# tests StarHash starts with. Reports are kept.") },
            confirmButton = { TextButton(onClick = { confirmRestore = false; onRestoreTests() }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EngineRow(engine: Engine, current: Engine, text: String, onChange: ((Settings) -> Settings) -> Unit) {
    Choice(engine.label, text, engine == current) { onChange { it.copy(engine = engine) } }
}

@Composable
private fun Choice(title: String, text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(text, fontSize = 12.sp, color = Sh.Muted)
        }
    }
}

@Composable
private fun TextSetting(label: String, value: String, type: KeyboardType, hint: String? = null, onCommit: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    OutlinedTextField(
        text, { text = it; onCommit(it) }, label = { Text(label) }, singleLine = true,
        supportingText = if (hint != null) { { Text(hint) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    )
}

@Composable
private fun NumberSetting(label: String, value: Int, range: IntRange, onCommit: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    val n = text.toIntOrNull()
    val ok = n != null && n in range
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedTextField(
            text, { text = it.filter(Char::isDigit).take(3); it.toIntOrNull()?.takeIf { v -> v in range }?.let(onCommit) },
            singleLine = true, isError = !ok,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(0.3f),
        )
    }
}
