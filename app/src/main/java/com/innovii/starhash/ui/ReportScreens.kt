package com.innovii.starhash.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.innovii.starhash.core.CaseResult
import com.innovii.starhash.core.Kind
import com.innovii.starhash.core.ReportFormat
import com.innovii.starhash.core.RunReport
import com.innovii.starhash.core.Status
import com.innovii.starhash.core.UssdText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(reports: List<RunReport>, onOpen: (String) -> Unit, onDeleteAll: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        PageHeader("Reports", onBack = null) {
            if (reports.isNotEmpty()) {
                IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete all reports", tint = Sh.Muted) }
            }
        }
        if (reports.isEmpty()) {
            Text(
                "No reports yet. Every run is saved here, with each pop-up, the balances and the SMS.",
                color = Sh.Muted, modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(reports, key = { it.id }) { r ->
                Panel(onClick = { onOpen(r.id) }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusIcon(r.overall, 40.dp)
                        Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                            Text(r.title, color = Sh.Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("${ReportFormat.date(r.startedAt)} · ${r.engine.label}", fontSize = 12.sp, color = Sh.Faint)
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniCount(r.count(Status.PASS), "pass", Sh.Pass)
                                MiniCount(r.count(Status.WARN), "check", Sh.Warn)
                                MiniCount(r.count(Status.FAIL) + r.count(Status.BLOCKED), "fail", Sh.Fail)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Sh.Faint)
                    }
                }
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Delete all reports?") },
            text = { Text("The ${reports.size} saved reports will be removed from this phone.") },
            confirmButton = { TextButton(onClick = { confirm = false; onDeleteAll() }) { Text("Delete", color = Sh.Fail) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep") } },
        )
    }
}

@Composable
private fun MiniCount(n: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    val c = if (n == 0) Sh.Faint else color
    Text(
        "$n $label", color = c, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun ReportScreen(report: RunReport?, context: Context, onBack: () -> Unit, onDelete: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader("Report", onBack) {
            if (report != null) {
                IconButton(onClick = { onDelete(report.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete this report", tint = Sh.Muted) }
            }
        }
        if (report == null) {
            Text("This report was deleted.", color = Sh.Muted, modifier = Modifier.padding(20.dp))
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                HeroCard {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ReportFormat.date(report.startedAt), color = Sh.Muted, fontSize = 13.sp)
                                Text(report.title, color = Sh.Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            }
                            StatusPill(report.overall)
                        }
                        Text(
                            listOf(report.engine.label, report.sim, report.device).filter { it.isNotBlank() }.joinToString(" · ") +
                                if (report.stopped) " · stopped early" else "",
                            color = Sh.Faint, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile("Pass", "${report.count(Status.PASS)}", Sh.Pass, Modifier.weight(1f))
                            StatTile("Check", "${report.count(Status.WARN)}", Sh.Warn, Modifier.weight(1f))
                            StatTile("Fail", "${report.count(Status.FAIL)}", Sh.Fail, Modifier.weight(1f))
                            StatTile("Blocked", "${report.count(Status.BLOCKED)}", Sh.Blocked, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SoftButton("Message", onClick = { Share.text(context, report) }, icon = Icons.Filled.Share, modifier = Modifier.weight(1.3f))
                            SoftButton("HTML", onClick = { Share.file(context, report, "html", "text/html", ReportFormat.html(report)) }, modifier = Modifier.weight(1f))
                            SoftButton("CSV", onClick = { Share.file(context, report, "csv", "text/csv", ReportFormat.csv(report)) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            item { SectionTitle("Tests", "Tap a test for every step") }
            items(report.results) { c -> CaseCard(c, report.currency) }
        }
    }
}

@Composable
private fun CaseCard(c: CaseResult, currency: String) {
    var open by remember { mutableStateOf(false) }
    Panel(onClick = { open = !open }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIcon(c.status, 34.dp)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(c.name, color = Sh.Ink, fontWeight = FontWeight.SemiBold)
                    Text("${c.route} · ${c.seconds}s", fontFamily = Sh.Mono, fontSize = 11.sp, color = Sh.Faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Sh.Faint)
            }
            Text(c.summary, color = Sh.Ink, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 10.dp))
            if (c.kind == Kind.SUBSCRIBE && c.balanceBefore != null) {
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Figure("Before", UssdText.money(c.balanceBefore), Modifier.weight(1f))
                    Figure("After", UssdText.money(c.balanceAfter), Modifier.weight(1f))
                    Figure("Charged", UssdText.money(c.charged), Modifier.weight(1f), strong = true)
                    Figure("Price", UssdText.money(c.price), Modifier.weight(1f))
                }
                Text(currency, fontSize = 11.sp, color = Sh.Faint, modifier = Modifier.padding(top = 4.dp))
            } else if (c.kind == Kind.BALANCE && c.balanceBefore != null) {
                Row(Modifier.padding(top = 12.dp)) {
                    Figure("Balance", "${UssdText.money(c.balanceBefore)} $currency", Modifier.weight(1f), strong = true)
                }
            }
            if (c.options.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.clip(RoundedCornerShape(12.dp)).background(Sh.Inset).padding(10.dp)) {
                    for (o in c.options) {
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text(if (o.ok) "✓" else "✗", color = if (o.ok) Sh.Pass else Sh.Fail, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                            Text("${o.option}. ${o.label}", color = Sh.Ink, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(0.45f))
                            Text(UssdText.oneLine(o.screen, 60), fontSize = 12.sp, color = Sh.Muted, modifier = Modifier.weight(0.55f))
                        }
                    }
                }
            }
            for (s in c.sms) {
                Text(
                    "✉  ${s.from}: ${s.body}", color = Sh.Ink, fontSize = 12.sp, lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Sh.Inset).padding(10.dp),
                )
            }
            if (open) {
                Spacer(Modifier.height(12.dp))
                Hairline()
                Text("Step by step", color = Sh.Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                for (l in c.log) LogBubble(l)
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier, strong: Boolean = false) {
    val c = if (strong) Sh.Money else Sh.Ink
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (strong) Sh.Money.copy(alpha = 0.12f) else Sh.Inset).padding(10.dp),
    ) {
        Text(label, fontSize = 11.sp, color = Sh.Muted)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c)
    }
}

object Share {
    fun text(context: Context, r: RunReport) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "StarHash QA report: ${r.title}")
            .putExtra(Intent.EXTRA_TEXT, ReportFormat.text(r))
        context.startActivity(Intent.createChooser(send, "Share the report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun file(context: Context, r: RunReport, ext: String, mime: String, content: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date(r.startedAt))
        val f = File(dir, "StarHash-report-$stamp.$ext")
        f.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", f)
        val send = Intent(Intent.ACTION_SEND)
            .setType(mime)
            .putExtra(Intent.EXTRA_SUBJECT, "StarHash QA report: ${r.title}")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Share the report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
