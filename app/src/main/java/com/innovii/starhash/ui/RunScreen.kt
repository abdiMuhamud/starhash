package com.innovii.starhash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovii.starhash.LiveRun
import com.innovii.starhash.core.CaseResult
import com.innovii.starhash.core.LogLine
import com.innovii.starhash.core.LogType
import com.innovii.starhash.core.ReportFormat
import com.innovii.starhash.core.RunReport

@Composable
fun RunScreen(
    live: LiveRun?,
    onBack: () -> Unit,
    onStop: () -> Unit,
    onOpenReport: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        PageHeader(if (live?.running == true) "Running" else "Run finished", onBack)
        if (live == null) {
            Text("No test is running.", color = Sh.Muted, modifier = Modifier.padding(16.dp))
            return@Column
        }
        val list = rememberLazyListState()
        val count = 2 + live.results.size + live.lines.size
        LaunchedEffect(live.lines.size, live.running) {
            if (live.running && count > 0) list.animateScrollToItem(count - 1)
        }
        LazyColumn(state = list, contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 32.dp), modifier = Modifier.fillMaxSize()) {
            item { RunHeader(live, onStop, onOpenReport) }
            items(live.results) { r ->
                ResultLine(r)
            }
            item { SectionTitle("Live", "What the network shows, and what StarHash types") }
            items(live.lines) { l -> LogBubble(l) }
        }
    }
}

@Composable
private fun RunHeader(live: LiveRun, onStop: () -> Unit, onOpenReport: (String) -> Unit) {
    NightCard {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(live.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                InnoviiLogo(height = 18.dp)
            }
            if (live.running) {
                Text(
                    "Test ${live.index + 1} of ${live.total}: ${live.current?.name ?: "starting…"}",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { live.results.size.toFloat() / live.total.coerceAtLeast(1) },
                    color = Sh.Teal,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "The USSD pop-ups will appear and close by themselves. Keep the screen on and don't touch them.",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onStop, colors = ButtonDefaults.buttonColors(containerColor = Sh.Fail)) { Text("Stop") }
            } else {
                val report = RunReport("", live.title, 0, 0, live.engine, results = live.results)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(report.overall)
                    Text("  " + ReportFormat.counts(report), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
                val id = live.reportId
                if (id != null) {
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onOpenReport(id) }, colors = ButtonDefaults.buttonColors(containerColor = Sh.Teal, contentColor = Sh.Night)) {
                        Text("Open the report")
                    }
                } else {
                    Text("Stopped before the first test finished: no report.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ResultLine(r: CaseResult) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        StatusIcon(r.status, 26.dp)
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(r.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(r.summary, fontSize = 12.sp, color = Sh.Muted)
        }
    }
}

@Composable
fun LogBubble(l: LogLine) {
    when (l.type) {
        LogType.DIAL, LogType.REPLY -> Box(Modifier.fillMaxWidth().padding(vertical = 3.dp), contentAlignment = Alignment.CenterEnd) {
            Text(
                if (l.type == LogType.DIAL) "Dial ${l.text}" else "Send ${l.text}",
                color = Color.White, fontWeight = FontWeight.SemiBold, fontFamily = Sh.Mono, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp)).background(Sh.Violet)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
        LogType.SCREEN -> Box(Modifier.fillMaxWidth().padding(vertical = 3.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                l.text, color = Sh.BubbleInk, fontSize = 13.sp, lineHeight = 18.sp,
                modifier = Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp))
                    .background(Sh.Bubble).padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        LogType.BALANCE -> Text(
            "💰 ${l.text}", color = Sh.TealInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(8.dp)).background(Sh.TealSoft)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
        LogType.ERROR -> Text("✗ ${l.text}", color = Sh.Fail, fontSize = 13.sp, modifier = Modifier.padding(vertical = 3.dp))
        LogType.SMS -> Text("✉ ${l.text}", color = Sh.Ink, fontSize = 13.sp, modifier = Modifier.padding(vertical = 3.dp))
        LogType.CLOSE -> Text(
            "— ${l.text.lowercase()} —", color = Sh.Muted, fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        LogType.NOTE -> Text(l.text, color = Sh.Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 3.dp))
    }
}
