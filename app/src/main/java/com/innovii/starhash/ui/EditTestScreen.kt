package com.innovii.starhash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.innovii.starhash.core.Kind
import com.innovii.starhash.core.TestCase
import com.innovii.starhash.core.UssdText
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTestScreen(
    existing: TestCase?,
    onBack: () -> Unit,
    onSave: (TestCase) -> Unit,
    onDelete: (String) -> Unit,
    onRun: (TestCase) -> Unit,
) {
    val start = existing ?: TestCase(id = UUID.randomUUID().toString(), name = "", kind = Kind.MENU, code = "*400#")
    var name by remember { mutableStateOf(start.name) }
    var kind by remember { mutableStateOf(start.kind) }
    var code by remember { mutableStateOf(start.code) }
    var path by remember { mutableStateOf(start.path.joinToString(" ")) }
    var expect by remember { mutableStateOf(start.expectText) }
    var price by remember { mutableStateOf(start.price?.let { UssdText.money(it) } ?: "") }
    var smsFrom by remember { mutableStateOf(start.smsFrom) }
    var smsText by remember { mutableStateOf(start.smsText) }
    var unCode by remember { mutableStateOf(start.unsubscribeCode) }
    var unPath by remember { mutableStateOf(start.unsubscribePath.joinToString(" ")) }
    var note by remember { mutableStateOf(start.note) }
    var enabled by remember { mutableStateOf(start.enabled) }
    var confirmDelete by remember { mutableStateOf(false) }

    val codeOk = kind == Kind.BALANCE || UssdText.normalizeCode(code).matches(Regex("""[*#][0-9*#]*#"""))
    val priceOk = price.isBlank() || price.replace(',', '.').toDoubleOrNull() != null
    val valid = name.isNotBlank() && codeOk && priceOk

    fun build() = start.copy(
        name = name.trim(), kind = kind, code = if (kind == Kind.BALANCE) "" else UssdText.normalizeCode(code),
        path = UssdText.parsePath(path), expectText = expect.trim(),
        price = price.replace(',', '.').toDoubleOrNull(), smsFrom = smsFrom.trim(), smsText = smsText.trim(),
        unsubscribeCode = unCode.trim().let { if (it.isBlank()) "" else UssdText.normalizeCode(it) },
        unsubscribePath = UssdText.parsePath(unPath), note = note.trim(), enabled = enabled,
    )

    Column(Modifier.fillMaxSize().imePadding()) {
        PageHeader(if (existing == null) "New test" else "Edit test", onBack) {
            if (existing != null) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete test") }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Type", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (k in Kind.entries) FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) })
            }
            Text(
                when (kind) {
                    Kind.BALANCE -> "Dials the balance code from Settings and reads the balance."
                    Kind.MENU -> "Dials the code, types the answers in order, and checks the last screen."
                    Kind.EXPLORE -> "Opens every numbered item of the menu once, to see each service answers. Never subscribes."
                    Kind.SUBSCRIBE -> "Reads the balance, subscribes through the menus, reads the balance again: was the SIM charged the price?"
                },
                fontSize = 12.sp, color = Sh.Muted,
            )
            if (kind != Kind.BALANCE) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    code, { code = it }, label = { Text("USSD code") }, placeholder = { Text("*400#") }, singleLine = true,
                    isError = !codeOk, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    path, { path = it }, label = { Text("Menu answers, in order") }, placeholder = { Text("4 1 1 1") },
                    supportingText = { Text("What you would type in each pop-up. Mobile Market in Hargeysa: 4 1 1 1") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (kind == Kind.MENU || kind == Kind.SUBSCRIBE) {
                OutlinedTextField(
                    expect, { expect = it }, label = { Text("Last screen should show") },
                    supportingText = { Text("Words separated by commas; one is enough. Empty: any answer.") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (kind == Kind.SUBSCRIBE) {
                OutlinedTextField(
                    price, { price = it }, label = { Text("Price (USD)") }, placeholder = { Text("0.50") }, singleLine = true,
                    isError = !priceOk, supportingText = { Text("Empty: use the price shown in the menu (e.g. “qiimihiisu waa 0.5\$”)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(smsFrom, { smsFrom = it }, label = { Text("SMS from") }, placeholder = { Text("400") }, singleLine = true, modifier = Modifier.weight(0.4f))
                    OutlinedTextField(smsText, { smsText = it }, label = { Text("SMS should say") }, placeholder = { Text("Mobile market") }, singleLine = true, modifier = Modifier.weight(0.6f))
                }
                Text("Unsubscribe afterwards (optional), so the test SIM is not charged again next month", fontSize = 12.sp, color = Sh.Muted, modifier = Modifier.padding(top = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(unCode, { unCode = it }, label = { Text("Code") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(0.4f))
                    OutlinedTextField(unPath, { unPath = it }, label = { Text("Answers") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(0.6f))
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Selected", fontWeight = FontWeight.SemiBold)
                    Text("Included in “Run selected”", fontSize = 12.sp, color = Sh.Muted)
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { val t = build(); onSave(t); onRun(t) }, enabled = valid, modifier = Modifier.weight(1f)) {
                Text("Save and run")
            }
            Button(
                onClick = { onSave(build()); onBack() }, enabled = valid, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Sh.Violet),
            ) { Text("Save") }
        }
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “${existing.name}”?") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(existing.id) }) { Text("Delete", color = Sh.Fail) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
}
