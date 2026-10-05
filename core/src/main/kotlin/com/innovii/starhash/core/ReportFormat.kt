package com.innovii.starhash.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A run report as a message (WhatsApp, email) or as a stand-alone HTML page. */
object ReportFormat {

    fun icon(s: Status) = when (s) {
        Status.PASS -> "✅"
        Status.WARN -> "⚠️"
        Status.FAIL -> "❌"
        Status.BLOCKED -> "⛔"
    }

    fun date(ms: Long): String = SimpleDateFormat("d MMM yyyy HH:mm", Locale.US).format(Date(ms))

    private fun time(ms: Long): String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ms))

    fun counts(r: RunReport): String =
        "${r.count(Status.PASS)} pass, ${r.count(Status.FAIL)} fail, ${r.count(Status.WARN)} to check, ${r.count(Status.BLOCKED)} blocked"

    fun text(r: RunReport): String = buildString {
        appendLine("StarHash QA report (INNOVII)")
        appendLine("${r.title} · ${date(r.startedAt)}")
        appendLine(listOf("Engine: ${r.engine.label}", r.sim.takeIf { it.isNotBlank() }?.let { "SIM: $it" }, r.device.takeIf { it.isNotBlank() })
            .filterNotNull().joinToString(" · "))
        appendLine("Result: ${icon(r.overall)} ${r.overall.label.uppercase()} (${counts(r)})" + if (r.stopped) " · stopped early" else "")
        for (c in r.results) {
            appendLine()
            appendLine("${icon(c.status)} ${c.name}  [${c.route}]")
            appendLine("   ${c.summary}")
            if (c.kind == Kind.SUBSCRIBE && c.balanceBefore != null) {
                appendLine(
                    "   Balance ${UssdText.money(c.balanceBefore)} → ${UssdText.money(c.balanceAfter)} ${r.currency}" +
                        ", charged ${UssdText.money(c.charged)}, price ${UssdText.money(c.price)}",
                )
            }
            for (o in c.options) {
                appendLine("   ${if (o.ok) "✓" else "✗"} ${o.option}.${o.label}: ${UssdText.oneLine(o.screen, 50)}")
            }
            for (s in c.sms) appendLine("   SMS ${s.from}: ${UssdText.oneLine(s.body, 70)}")
        }
    }

    fun csv(r: RunReport): String = buildString {
        appendLine("test,route,status,summary,balance_before,balance_after,charged,price,seconds,sms")
        for (c in r.results) {
            appendLine(
                listOf(
                    c.name, c.route, c.status.label, c.summary, c.balanceBefore?.let(UssdText::money) ?: "",
                    c.balanceAfter?.let(UssdText::money) ?: "", c.charged?.let(UssdText::money) ?: "",
                    c.price?.let(UssdText::money) ?: "", c.seconds.toString(),
                    c.sms.joinToString(" | ") { "${it.from}: ${it.body}" },
                ).joinToString(",") { "\"" + it.replace("\"", "\"\"").replace("\n", " ") + "\"" },
            )
        }
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun color(s: Status) = when (s) {
        Status.PASS -> "#1FA463"
        Status.WARN -> "#C98A0C"
        Status.FAIL -> "#D9474E"
        Status.BLOCKED -> "#6B7280"
    }

    fun html(r: RunReport): String = buildString {
        append(
            """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>StarHash QA report</title><style>
body{margin:0;background:#ECEEF2;color:#15161A;font:15px/1.45 system-ui,-apple-system,Segoe UI,Roboto,sans-serif}
.wrap{max-width:860px;margin:0 auto;padding:16px}
header{background:#161619;color:#ECEDF1;border-radius:20px;padding:22px;border-top:4px solid #5B8DEF;border-image:linear-gradient(90deg,#5B8DEF,#4CD08A) 1}
header h1{margin:0;font-size:22px}header .sub{opacity:.8;margin-top:4px}
.pill{display:inline-block;border-radius:999px;padding:2px 10px;font-weight:700;font-size:12px;color:#fff}
.card{background:#F6F7F9;border-radius:16px;margin-top:12px;padding:14px 16px;border-left:6px solid #ccc;box-shadow:0 1px 2px rgba(0,0,0,.06)}
.card h2{margin:0;font-size:16px}.route{color:#6B7280;font-family:ui-monospace,monospace;font-size:13px}
.sum{margin:6px 0 0}.kv{display:flex;flex-wrap:wrap;gap:8px 18px;margin-top:8px;font-size:13px;color:#374151}
pre{white-space:pre-wrap;background:#F3F4F6;border-radius:8px;padding:8px;margin:6px 0;font-size:12.5px}
table{border-collapse:collapse;width:100%;font-size:12.5px;margin-top:8px}td{border-top:1px solid #E5E7EB;padding:4px 6px;vertical-align:top}
td.t{color:#6B7280;white-space:nowrap}td.k{font-weight:700;white-space:nowrap}
details summary{cursor:pointer;color:#3F74DE;margin-top:8px;font-size:13px}
</style></head><body><div class="wrap">""",
        )
        append("<header><h1>StarHash QA report</h1><div class=\"sub\">INNOVII · ${esc(r.title)} · ${esc(date(r.startedAt))}</div>")
        append("<div class=\"sub\">Engine: ${esc(r.engine.label)}")
        if (r.sim.isNotBlank()) append(" · SIM: ${esc(r.sim)}")
        if (r.device.isNotBlank()) append(" · ${esc(r.device)}")
        append("</div><p><span class=\"pill\" style=\"background:${color(r.overall)}\">${r.overall.label.uppercase()}</span> ")
        append(esc(counts(r)))
        if (r.stopped) append(" · stopped early")
        append("</p></header>")
        for (c in r.results) {
            append("<div class=\"card\" style=\"border-left-color:${color(c.status)}\">")
            append("<h2>${esc(c.name)} <span class=\"pill\" style=\"background:${color(c.status)}\">${c.status.label.uppercase()}</span></h2>")
            append("<div class=\"route\">${esc(c.route)} · ${esc(c.kind.label)} · ${c.seconds}s</div>")
            append("<p class=\"sum\">${esc(c.summary)}</p>")
            if (c.balanceBefore != null || c.price != null) {
                append("<div class=\"kv\">")
                append("<span>Balance before: <b>${UssdText.money(c.balanceBefore)}</b></span>")
                if (c.kind == Kind.SUBSCRIBE) {
                    append("<span>after: <b>${UssdText.money(c.balanceAfter)}</b></span>")
                    append("<span>charged: <b>${UssdText.money(c.charged)}</b></span>")
                    append("<span>price: <b>${UssdText.money(c.price)}</b></span>")
                }
                append("</div>")
            }
            if (c.options.isNotEmpty()) {
                append("<table>")
                for (o in c.options) {
                    append("<tr><td class=\"k\">${if (o.ok) "✓" else "✗"} ${esc(o.option)}</td><td>${esc(o.label)}</td><td>${esc(UssdText.oneLine(o.screen, 120))}</td></tr>")
                }
                append("</table>")
            }
            if (c.lastScreen.isNotBlank()) append("<pre>${esc(c.lastScreen)}</pre>")
            for (s in c.sms) append("<div class=\"kv\"><span>SMS from <b>${esc(s.from)}</b> at ${time(s.at)}: ${esc(s.body)}</span></div>")
            if (c.log.isNotEmpty()) {
                append("<details><summary>Step by step (${c.log.size})</summary><table>")
                for (l in c.log) {
                    append("<tr><td class=\"t\">${time(l.at)}</td><td class=\"k\">${l.type.name.lowercase()}</td><td>${esc(l.text).replace("\n", "<br>")}</td></tr>")
                }
                append("</table></details>")
            }
            append("</div>")
        }
        append("</div></body></html>")
    }
}
