package com.innovii.starhash.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovii.starhash.LiveRun
import com.innovii.starhash.core.Engine
import com.innovii.starhash.core.Kind
import com.innovii.starhash.core.ReportFormat
import com.innovii.starhash.core.RunReport
import com.innovii.starhash.core.Status
import com.innovii.starhash.core.TestCase
import com.innovii.starhash.core.UssdText
import com.innovii.starhash.core.Workspace

@Composable
fun HomeScreen(
    ws: Workspace,
    setup: SetupState,
    simLabel: String,
    live: LiveRun?,
    lastReport: RunReport?,
    context: Context,
    onAskPermissions: () -> Unit,
    onRun: (title: String, cases: List<TestCase>) -> Unit,
    onOpenRun: () -> Unit,
    onOpenReport: (String) -> Unit,
    onEdit: (id: String?) -> Unit,
    onToggle: (TestCase) -> Unit,
) {
    val tests = ws.tests
    val checks = tests.filter { !it.charges }
    val subs = tests.filter { it.charges }
    val selected = tests.filter { it.enabled }
    val balanceCode = UssdText.normalizeCode(ws.settings.balanceCode)
    val codes = checks.filter { it.kind != Kind.BALANCE }.map { UssdText.normalizeCode(it.code) }.distinct()
    val running = live?.running == true

    fun checksFor(code: String) = checks.filter {
        UssdText.normalizeCode(it.code) == code || (it.kind == Kind.BALANCE && code == balanceCode)
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), modifier = Modifier.weight(1f)) {
            item { TopBar() }
            item {
                Spacer(Modifier.height(14.dp))
                Overview(ws, simLabel, live, lastReport, onOpenRun, onOpenReport)
            }
            if (!setup.readyFor(ws.settings.engine) || (!setup.sms && ws.settings.engine != Engine.DEMO)) {
                item {
                    Spacer(Modifier.height(14.dp))
                    SetupCard(setup, ws.settings.engine, onAskPermissions, context)
                }
            }

            item { SectionTitle("Check the codes", "Dials, walks the menus and reads the answers. No charge.") }
            item {
                val tiles = listOf(Tile("Balance", balanceCode, money = true)) +
                    codes.map { Tile(it, "${checksFor(it).size} tests", money = false) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (row in tiles.chunked(2)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (tile in row) {
                                ActionTile(
                                    title = if (tile.money) tile.title else "Check ${tile.title}",
                                    hint = tile.hint,
                                    glyph = if (tile.money) "\$" else null,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (tile.money) {
                                        val b = tests.firstOrNull { it.kind == Kind.BALANCE } ?: TestCase("balance", "Balance", Kind.BALANCE)
                                        onRun("Balance", listOf(b))
                                    } else {
                                        onRun("Check ${tile.title}", checksFor(tile.title))
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    GradientButton(
                        "Check all codes  ·  ${checks.size} tests",
                        onClick = { onRun("All codes", checks) },
                        icon = Icons.Filled.PlayArrow,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item { SectionTitle("Subscriptions", "Balance before → subscribe → balance after. These charge the test SIM.") }
            if (subs.isEmpty()) {
                item { Text("No subscription tests yet: add one below.", color = Sh.Faint, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp)) }
            }
            items(subs, key = { "sub-" + it.id }) { t ->
                SubscriptionRow(t, onOpen = { onEdit(t.id) }, onRun = { onRun(t.name, listOf(t)) })
                Spacer(Modifier.height(10.dp))
            }

            item { SectionTitle("All tests", "Tick the tests to run together; tap one to change it.") }
            item {
                Panel {
                    Column {
                        tests.forEachIndexed { i, t ->
                            if (i > 0) Hairline(Modifier.padding(start = 62.dp))
                            TestRow(t, onOpen = { onEdit(t.id) }, onToggle = { onToggle(t) })
                        }
                        if (tests.isNotEmpty()) Hairline()
                        Row(
                            Modifier.fillMaxWidth().clickable { onEdit(null) }.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Sh.Accent)
                            Spacer(Modifier.width(10.dp))
                            Text("New test", color = Sh.Accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        if (selected.isNotEmpty() && !running) {
            Box(Modifier.fillMaxWidth().background(Sh.Bar).padding(horizontal = 16.dp, vertical = 10.dp)) {
                GradientButton(
                    "Run selected  ·  ${selected.size}",
                    onClick = { onRun("Selected tests", selected) },
                    icon = Icons.Filled.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private class Tile(val title: String, val hint: String, val money: Boolean)

/** StarHash on the left, INNOVII on the right. */
@Composable
private fun TopBar() {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Logo(40.dp)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text("StarHash", color = Sh.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text("USSD & VAS quality checks", color = Sh.Faint, fontSize = 12.sp)
        }
        InnoviiLogo(height = 18.dp)
    }
}

/** What is going on: a run in progress, or the result of the last one. */
@Composable
private fun Overview(
    ws: Workspace,
    simLabel: String,
    live: LiveRun?,
    last: RunReport?,
    onOpenRun: () -> Unit,
    onOpenReport: (String) -> Unit,
) {
    HeroCard {
        Column(Modifier.padding(20.dp)) {
            if (live != null && live.running) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Testing now", color = Sh.Muted, fontSize = 13.sp)
                        Text(live.title, color = Sh.Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    SoftButton("Watch", onClick = onOpenRun)
                }
                Spacer(Modifier.height(14.dp))
                GradientProgress(live.results.size.toFloat() / live.total.coerceAtLeast(1))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Test ${live.index + 1} of ${live.total} · ${live.current?.name ?: "starting"}",
                    color = Sh.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            } else if (last != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Last run · ${ReportFormat.date(last.startedAt)}", color = Sh.Muted, fontSize = 13.sp)
                        Text(last.title, color = Sh.Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    StatusPill(last.overall)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Pass", "${last.count(Status.PASS)}", Sh.Pass, Modifier.weight(1f))
                    StatTile("Check", "${last.count(Status.WARN)}", Sh.Warn, Modifier.weight(1f))
                    StatTile("Fail", "${last.count(Status.FAIL) + last.count(Status.BLOCKED)}", Sh.Fail, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable { onOpenReport(last.id) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Open the report", color = Sh.Accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Sh.Accent)
                }
            } else {
                Text("Ready to test", color = Sh.Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(
                    "Start with a code check below. Every run is saved under Reports.",
                    color = Sh.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag(ws.settings.engine.label)
                Tag(if (ws.settings.engine == Engine.DEMO) "Demo SIM" else simLabel)
                Tag(ws.settings.pace.label)
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text,
        color = Sh.Muted,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Sh.Inset).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun ActionTile(title: String, hint: String, glyph: String?, modifier: Modifier, onClick: () -> Unit) {
    Panel(modifier, onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            if (glyph != null) IconBadge(Sh.Money, glyph = glyph, size = 38.dp) else IconBadge(Sh.Accent, icon = Icons.Filled.Phone, size = 38.dp)
            Spacer(Modifier.height(12.dp))
            Text(title, color = Sh.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(hint, color = Sh.Faint, fontSize = 12.sp)
        }
    }
}

@Composable
fun KindBadge(kind: Kind) {
    val c = when (kind) {
        Kind.SUBSCRIBE -> Sh.Money
        Kind.EXPLORE -> Sh.Amber
        else -> Sh.Accent
    }
    Text(
        kind.label,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = c,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.copy(alpha = 0.13f)).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
private fun SubscriptionRow(t: TestCase, onOpen: () -> Unit, onRun: () -> Unit) {
    Panel(onClick = onOpen) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Sh.Money, icon = Icons.Filled.ShoppingCart, size = 42.dp)
            Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                Text(t.name, color = Sh.Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(t.route, fontFamily = Sh.Mono, fontSize = 12.sp, color = Sh.Faint)
                if (t.price != null) {
                    Text(
                        "${UssdText.money(t.price)} USD",
                        color = Sh.Money, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            SoftButton("Run", onClick = onRun, color = Sh.Money, icon = Icons.Filled.PlayArrow)
        }
    }
}

@Composable
private fun TestRow(t: TestCase, onOpen: () -> Unit, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckDot(t.enabled, onClick = onToggle)
        Column(Modifier.padding(start = 8.dp).weight(1f)) {
            Text(t.name, color = Sh.Ink, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                KindBadge(t.kind)
                Spacer(Modifier.width(8.dp))
                Text(t.route, fontFamily = Sh.Mono, fontSize = 11.sp, color = Sh.Faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Sh.Faint, modifier = Modifier.size(20.dp))
    }
}
