package com.innovii.starhash.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovii.starhash.LiveRun
import com.innovii.starhash.core.Kind
import com.innovii.starhash.core.TestCase
import com.innovii.starhash.core.UssdText
import com.innovii.starhash.core.Workspace

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    ws: Workspace,
    setup: SetupState,
    simLabel: String,
    live: LiveRun?,
    context: Context,
    onAskPermissions: () -> Unit,
    onRun: (title: String, cases: List<TestCase>) -> Unit,
    onOpenRun: () -> Unit,
    onEdit: (id: String?) -> Unit,
    onToggle: (TestCase) -> Unit,
) {
    val tests = ws.tests
    val checks = tests.filter { !it.charges }
    val subs = tests.filter { it.charges }
    val selected = tests.filter { it.enabled }
    val balanceCode = UssdText.normalizeCode(ws.settings.balanceCode)
    val codes = checks.filter { it.kind != Kind.BALANCE }.map { UssdText.normalizeCode(it.code) }.distinct()

    fun checksFor(code: String) = checks.filter {
        UssdText.normalizeCode(it.code) == code || (it.kind == Kind.BALANCE && code == balanceCode)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp), modifier = Modifier.fillMaxSize()) {
            item {
                NightCard {
                    Column(Modifier.padding(18.dp)) {
                        Brand("USSD & VAS quality checks · INNOVII")
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(ws.settings.engine.label)
                            Chip(if (ws.settings.engine == com.innovii.starhash.core.Engine.DEMO) "Demo SIM" else simLabel)
                        }
                    }
                }
            }
            if (live != null && live.running) {
                item {
                    Spacer(Modifier.height(12.dp))
                    WhiteCard(onClick = onOpenRun) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Running: ${live.title}", fontWeight = FontWeight.Bold)
                            Text(
                                "Test ${live.index + 1} of ${live.total}: ${live.current?.name ?: "…"}",
                                fontSize = 13.sp, color = Sh.Muted,
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (live.results.size.toFloat() / live.total.coerceAtLeast(1)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            if (!setup.readyFor(ws.settings.engine) || (!setup.sms && ws.settings.engine != com.innovii.starhash.core.Engine.DEMO)) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SetupCard(setup, ws.settings.engine, onAskPermissions, context)
                }
            }

            item { SectionTitle("Check the codes", "Dials, walks the menus and reads the answers. No charge.") }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigAction("Balance", balanceCode) {
                        val b = tests.firstOrNull { it.kind == Kind.BALANCE } ?: TestCase("balance", "Balance", Kind.BALANCE)
                        onRun("Balance", listOf(b))
                    }
                    for (code in codes) {
                        BigAction("Check $code", "${checksFor(code).size} tests") { onRun("Check $code", checksFor(code)) }
                    }
                    BigAction("All codes", "${checks.size} tests", strong = true) { onRun("All codes", checks) }
                }
            }

            item { SectionTitle("Subscription tests", "Balance before → subscribe → balance after. These charge the test SIM.") }
            if (subs.isEmpty()) {
                item { Text("No subscription tests yet: add one below.", color = Sh.Muted, fontSize = 13.sp) }
            }
            items(subs, key = { "sub-" + it.id }) { t ->
                TestRow(t, onClick = { onEdit(t.id) }, onToggle = null, onRun = { onRun(t.name, listOf(t)) })
                Spacer(Modifier.height(8.dp))
            }

            item { SectionTitle("All tests", "Tick the tests to run together, tap one to change it.") }
            items(tests, key = { it.id }) { t ->
                TestRow(t, onClick = { onEdit(t.id) }, onToggle = { onToggle(t) }, onRun = null)
                Spacer(Modifier.height(8.dp))
            }
            item {
                OutlinedButton(onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("New test")
                }
            }
        }
        if (selected.isNotEmpty() && live?.running != true) {
            ExtendedFloatingActionButton(
                onClick = { onRun("Selected tests", selected) },
                icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                text = { Text("Run selected (${selected.size})") },
                containerColor = Sh.Violet,
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun BigAction(title: String, hint: String, strong: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = if (strong) {
            ButtonDefaults.buttonColors(containerColor = Sh.Teal, contentColor = Sh.Night)
        } else {
            ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Sh.Night)
        },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (strong) Sh.Night else Sh.Violet)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(hint, fontSize = 11.sp, color = if (strong) Sh.Night.copy(alpha = 0.7f) else Sh.Muted)
        }
    }
}

@Composable
fun KindBadge(kind: Kind) {
    val (bg, fg) = when (kind) {
        Kind.SUBSCRIBE -> Color(0xFFFFF1E0) to Color(0xFFB45309)
        Kind.EXPLORE -> Sh.TealSoft to Color(0xFF047857)
        else -> Sh.VioletSoft to Sh.Violet
    }
    Text(
        kind.label,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun TestRow(t: TestCase, onClick: () -> Unit, onToggle: (() -> Unit)?, onRun: (() -> Unit)?) {
    WhiteCard(onClick = onClick) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KindBadge(t.kind)
                    if (t.charges && t.price != null) {
                        Spacer(Modifier.width(6.dp))
                        Text("${UssdText.money(t.price)} USD", fontSize = 11.sp, color = Sh.Muted)
                    }
                }
                Text(t.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                Text(t.route, fontFamily = Sh.Mono, fontSize = 12.sp, color = Sh.Muted)
            }
            if (onToggle != null) Checkbox(checked = t.enabled, onCheckedChange = { onToggle() })
            if (onRun != null) {
                Button(
                    onClick = onRun,
                    colors = ButtonDefaults.buttonColors(containerColor = Sh.Violet),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.padding(end = 6.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Run")
                }
            }
        }
    }
}
