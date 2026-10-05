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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.innovii.starhash.core.CaseResult
import com.innovii.starhash.core.Kind
import com.innovii.starhash.core.ReportFormat
import com.innovii.starhash.core.RunReport
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
                IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete all reports") }
            }
        }
        if (reports.isEmpty()) {
            Text(
                "No reports yet. Run a test on the Tests page: every run is saved here.",
                color = Sh.Muted, modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(reports, key = { it.id }) { r ->
                WhiteCard(onClick = { onOpen(r.id) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusIcon(r.overall, 34.dp)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(r.title, fontWeight = FontWeight.SemiBold)
                            Text("${ReportFormat.date(r.startedAt)} · ${r.engine.label}", fontSize = 12.sp, color = Sh.Muted)
                            Text(ReportFormat.counts(r), fontSize = 12.sp, color = Sh.Muted)
                        }
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
fun ReportScreen(report: RunReport?, context: Context, onBack: () -> Unit, onDelete: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader("Report", onBack) {
            if (report != null) {
                IconButton(onClick = { onDelete(report.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete this report") }
            }
        }
        if (report == null) {
            Text("This report was deleted.", color = Sh.Muted, modifier = Modifier.padding(16.dp))
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                NightCard {
                    Column(Modifier.padding(18.dp)) {
                        Text(report.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(ReportFormat.date(report.startedAt), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        Text(
                            listOf(report.engine.label, report.sim, report.device).filter { it.isNotBlank() }.joinToString(" · "),
                            color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(report.overall)
                            Text(
                                "  " + ReportFormat.counts(report) + if (report.stopped) " · stopped early" else "",
                                color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { Share.text(context, report) }) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Message")
                    }
                    FilledTonalButton(onClick = { Share.file(context, report, "html", "text/html", ReportFormat.html(report)) }) {
                        Text("HTML")
                    }
                    FilledTonalButton(onClick = { Share.file(context, report, "csv", "text/csv", ReportFormat.csv(report)) }) {
                        Text("CSV")
                    }
                }
            }
            items(report.results) { c -> CaseCard(c, report.currency) }
        }
    }
}

@Composable
private fun CaseCard(c: CaseResult, currency: String) {
    var open by remember { mutableStateOf(false) }
    WhiteCard(onClick = { open = !open }) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIcon(c.status, 30.dp)
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(c.name, fontWeight = FontWeight.SemiBold)
                    Text("${c.route} · ${c.seconds}s", fontFamily = Sh.Mono, fontSize = 11.sp, color = Sh.Muted)
                }
                Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Sh.Muted)
            }
            Text(c.summary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            if (c.kind == Kind.SUBSCRIBE && c.balanceBefore != null) {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Figure("Before", UssdText.money(c.balanceBefore), Modifier.weight(1f))
                    Figure("After", UssdText.money(c.balanceAfter), Modifier.weight(1f))
                    Figure("Charged", UssdText.money(c.charged), Modifier.weight(1f), strong = true)
                    Figure("Price", UssdText.money(c.price), Modifier.weight(1f))
                }
                Text(currency, fontSize = 11.sp, color = Sh.Muted, modifier = Modifier.padding(top = 2.dp))
            } else if (c.kind == Kind.BALANCE && c.balanceBefore != null) {
                Row(Modifier.padding(top = 10.dp)) { Figure("Balance", "${UssdText.money(c.balanceBefore)} $currency", Modifier.weight(1f), strong = true) }
            }
            if (c.options.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                for (o in c.options) {
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(if (o.ok) "✓" else "✗", color = if (o.ok) Sh.Pass else Sh.Fail, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
                        Text("${o.option}. ${o.label}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(0.45f))
                        Text(UssdText.oneLine(o.screen, 60), fontSize = 12.sp, color = Sh.Muted, modifier = Modifier.weight(0.55f))
                    }
                }
            }
            for (s in c.sms) {
                Text(
                    "✉ ${s.from}: ${s.body}", fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).background(Sh.Background).padding(8.dp),
                )
            }
            if (open) {
                Spacer(Modifier.height(10.dp))
                Text("Step by step", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                for (l in c.log) LogBubble(l)
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier, strong: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(if (strong) Sh.VioletSoft else Sh.Background).padding(8.dp),
    ) {
        Text(label, fontSize = 11.sp, color = Sh.Muted)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (strong) Sh.Violet else Sh.Ink)
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
