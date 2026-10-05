package com.innovii.starhash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovii.starhash.LiveRun
import com.innovii.starhash.core.CaseResult
import com.innovii.starhash.core.LogLine
import com.innovii.starhash.core.LogType
import com.innovii.starhash.core.RunReport
import com.innovii.starhash.core.Status

@Composable
fun RunScreen(
    live: LiveRun?,
    onBack: () -> Unit,
    onStop: () -> Unit,
    onOpenReport: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        PageHeader(if (live?.running == true) "Testing" else "Run finished", onBack) {
            InnoviiLogo(Modifier.padding(end = 12.dp), height = 16.dp)
        }
        if (live == null) {
            Text("No test is running.", color = Sh.Muted, modifier = Modifier.padding(20.dp))
            return@Column
        }
        val list = rememberLazyListState()
        LaunchedEffect(live.lines.size, live.running) {
            val last = list.layoutInfo.totalItemsCount - 1
            if (live.running && last > 0) list.animateScrollToItem(last)
        }
        LazyColumn(state = list, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), modifier = Modifier.fillMaxSize()) {
            item { RunHeader(live, onStop, onOpenReport) }
            if (live.results.isNotEmpty()) {
                item { SectionTitle("Results") }
                item {
                    Panel {
                        Column {
                            live.results.forEachIndexed { i, r ->
                                if (i > 0) Hairline(Modifier.padding(start = 56.dp))
                                ResultLine(r)
                            }
                        }
                    }
                }
            }
            item { SectionTitle("Live", "What the network shows, and what StarHash types") }
            items(live.lines) { l -> LogBubble(l) }
        }
    }
}

@Composable
private fun RunHeader(live: LiveRun, onStop: () -> Unit, onOpenReport: (String) -> Unit) {
    HeroCard {
        Column(Modifier.padding(20.dp)) {
            Text(live.title, color = Sh.Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            if (live.running) {
                Text(
                    "Test ${live.index + 1} of ${live.total} · ${live.current?.name ?: "starting"}",
                    color = Sh.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.height(16.dp))
                GradientProgress(live.results.size.toFloat() / live.total.coerceAtLeast(1))
                Spacer(Modifier.height(14.dp))
                Text(
                    "The USSD pop-ups open and close by themselves, at a person's pace. Keep the screen on and don't touch them.",
                    color = Sh.Faint, fontSize = 12.sp, lineHeight = 17.sp,
                )
                Spacer(Modifier.height(14.dp))
                SoftButton("Stop", onClick = onStop, color = Sh.Fail, icon = Icons.Filled.Close)
            } else {
                val report = RunReport("", live.title, 0, 0, live.engine, results = live.results)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { StatusPill(report.overall) }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Pass", "${report.count(Status.PASS)}", Sh.Pass, Modifier.weight(1f))
                    StatTile("Check", "${report.count(Status.WARN)}", Sh.Warn, Modifier.weight(1f))
                    StatTile("Fail", "${report.count(Status.FAIL) + report.count(Status.BLOCKED)}", Sh.Fail, Modifier.weight(1f))
                }
                val id = live.reportId
                Spacer(Modifier.height(16.dp))
                if (id != null) {
                    GradientButton("Open the report", onClick = { onOpenReport(id) }, modifier = Modifier.fillMaxWidth())
                } else {
                    Text("Stopped before the first test finished: no report.", color = Sh.Faint, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ResultLine(r: CaseResult) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        StatusIcon(r.status, 30.dp)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(r.name, color = Sh.Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(r.summary, fontSize = 12.sp, color = Sh.Muted, lineHeight = 16.sp)
        }
    }
}

@Composable
fun LogBubble(l: LogLine) {
    when (l.type) {
        LogType.DIAL, LogType.REPLY -> Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.CenterEnd) {
            Text(
                if (l.type == LogType.DIAL) "Dial ${l.text}" else "Send ${l.text}",
                color = Color.White, fontWeight = FontWeight.SemiBold, fontFamily = Sh.Mono, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)).background(Sh.Blue)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        LogType.SCREEN -> Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.CenterStart) {
            val shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
            Text(
                l.text, color = Sh.BubbleInk, fontSize = 13.sp, lineHeight = 19.sp,
                modifier = Modifier.widthIn(max = 320.dp).clip(shape).background(Sh.Bubble).border(1.dp, Sh.Line, shape)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            )
        }
        LogType.BALANCE -> Text(
            "\$  ${l.text}", color = Sh.Money, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 5.dp).clip(RoundedCornerShape(10.dp)).background(Sh.Money.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        LogType.ERROR -> Text("✗  ${l.text}", color = Sh.Fail, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
        LogType.SMS -> Text("✉  ${l.text}", color = Sh.Ink, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
        LogType.CLOSE -> Text(
            l.text.lowercase(), color = Sh.Faint, fontSize = 11.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        LogType.NOTE -> Text(l.text, color = Sh.Faint, fontSize = 12.sp, modifier = Modifier.padding(vertical = 3.dp))
    }
}
