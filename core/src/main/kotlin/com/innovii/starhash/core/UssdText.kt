package com.innovii.starhash.core

import kotlin.math.round

data class MenuOption(val key: String, val label: String)

enum class Outcome { SUCCESS, INSUFFICIENT, ALREADY, FAILED, UNKNOWN }

/** Reading what the network sends back. Telesom menus mix English and Somali, so both are covered. */
object UssdText {

    private val progressWords = listOf(
        "ussd code running", "running ussd", "please wait", "sending", "loading", "connecting", "processing",
    )

    /** "USSD code running…" and similar: not an answer yet. */
    fun isProgress(text: String): Boolean {
        val t = text.lowercase().trim()
        return t.isEmpty() || (t.length < 80 && progressWords.any { t.contains(it) })
    }

    private val errorWords = listOf(
        "connection problem", "invalid mmi", "mmi code", "unknown application", "ussd error", "network error",
        "network busy", "service unavailable", "not reachable", "request failed", "ussd failed",
        "external application", "network not available", "unable to process", "error performing request",
        "no response from", "ussd request failed", "cannot be completed", "ussd timeout", "session timed out",
    )

    /** The network or the phone refused the code (not a menu, not a business answer). */
    fun errorIn(text: String): String? {
        val t = text.lowercase()
        return if (errorWords.any { t.contains(it) }) firstLine(text) else null
    }

    private val optionLine = Regex("""^\s*(\d{1,3})\s*[.):\-]\s*(\S.*)$""")

    /** Numbered lines of a menu: "4.Mobile Market" → (4, Mobile Market). */
    fun options(text: String): List<MenuOption> =
        text.lines().mapNotNull { line ->
            optionLine.find(line)?.let { MenuOption(it.groupValues[1], it.groupValues[2].trim()) }
        }

    private val backWords = listOf("back", "dib u", "exit", "ka bax", "previous", "hore u laabo", "main menu")

    fun isBackOption(o: MenuOption): Boolean {
        val l = o.label.lowercase()
        return backWords.any { l.contains(it) }
    }

    private const val NUMBER = """([0-9]+(?:[.,][0-9]+)?)(?![.)]\s*[A-Za-z])"""
    private val balanceWords = """(?:balance|hadh?aag\w*|hadhag\w*|haraag\w*)"""
    private val balanceRegex = Regex("""$balanceWords[^0-9\n]{0,25}$NUMBER""", RegexOption.IGNORE_CASE)

    /** "Balance: 0.02USD", "Hadhagaagu hadda waa: 0.02," → 0.02. */
    fun balance(text: String, pattern: String = ""): Double? {
        if (pattern.isNotBlank()) {
            val custom = runCatching { Regex(pattern, RegexOption.IGNORE_CASE) }.getOrNull()
            custom?.find(text)?.let { m -> m.groupValues.getOrNull(1)?.let { toAmount(it) }?.let { return it } }
        }
        return balanceRegex.find(text)?.groupValues?.get(1)?.let(::toAmount)
    }

    private val priceWordRegex = Regex("""(?:price|qiim\w*|cost|lacagta|charge)[^0-9\n]{0,25}$NUMBER""", RegexOption.IGNORE_CASE)
    private val priceAfter = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*(?:\$|usd\b|dollar|doolar)""", RegexOption.IGNORE_CASE)
    private val priceBefore = Regex("""(?:\$|usd)\s*([0-9]+(?:[.,][0-9]+)?)""", RegexOption.IGNORE_CASE)
    private val balanceNear = Regex("""$balanceWords[^0-9\n]{0,25}$""", RegexOption.IGNORE_CASE)

    /** The service price shown in a menu: "qiimihiisu waa 0.5$" → 0.5. Balances are ignored. */
    fun price(text: String): Double? {
        priceWordRegex.find(text)?.let { return toAmount(it.groupValues[1]) }
        for (r in listOf(priceAfter, priceBefore)) {
            for (m in r.findAll(text)) {
                val before = text.substring(0, m.range.first).takeLast(30)
                if (balanceNear.containsMatchIn(before)) continue
                toAmount(m.groupValues[1])?.let { return it }
            }
        }
        return null
    }

    private val insufficientWords = listOf(
        "insufficient", "not enough", "low balance", "kuma filna", "kuguma filna", "ma ku filna", "ku filnayn",
        "kugu filnayn", "ma filna", "lacag kugu filan ma",
    )
    private val alreadyWords = listOf("already", "horey", "hore ayaad", "hore ugu", "hore u diiwaan")
    private val failedWords = listOf("failed", "failure", "error", "sorry", "khalad", "ma suurtagal", "lama aqbalin", "invalid")
    private val successWords = listOf(
        "success", "subscribed", "activated", "welcome", "thank you", "congratulation", "mahadsanid", "guul",
        "guuleysatay", "ku soo dhawaw", "ku soo dhawow", "waad ku biirtay", "diiwaan", "waa laguu", "hambalyo",
        "registered", "confirmed",
    )

    /** What the last screen of a subscription says. */
    fun outcome(text: String): Outcome {
        val t = text.lowercase()
        return when {
            t.isBlank() -> Outcome.UNKNOWN
            insufficientWords.any { t.contains(it) } -> Outcome.INSUFFICIENT
            alreadyWords.any { t.contains(it) } -> Outcome.ALREADY
            failedWords.any { t.contains(it) } -> Outcome.FAILED
            successWords.any { t.contains(it) } -> Outcome.SUCCESS
            else -> Outcome.UNKNOWN
        }
    }

    /** "*400#" + [4, 1] → "*400*4*1#" (for the one-shot engine). */
    fun chain(code: String, path: List<String>): String {
        val c = normalizeCode(code)
        if (path.isEmpty()) return c
        return c.removeSuffix("#") + "*" + path.joinToString("*") + "#"
    }

    fun normalizeCode(code: String): String {
        val c = code.trim().replace(" ", "")
        if (c.isEmpty()) return c
        return if (c.endsWith("#")) c else "$c#"
    }

    /** "4 1 1 1", "4,1,1,1", "4*1*1*1" and "4 → 1" all give [4, 1, 1, 1]. */
    fun parsePath(text: String): List<String> =
        text.split(Regex("""[\s,*→>/;]+""")).map { it.trim() }.filter { it.isNotEmpty() }

    /** Comma-separated words; true when the text has at least one (or when there are none). */
    fun hasAny(text: String, words: String): Boolean {
        val list = words.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (list.isEmpty()) return true
        val t = text.lowercase()
        return list.any { t.contains(it) }
    }

    fun firstLine(text: String): String = text.lines().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: ""

    fun oneLine(text: String, max: Int = 90): String {
        val s = text.replace(Regex("""\s+"""), " ").trim()
        return if (s.length <= max) s else s.take(max - 1) + "…"
    }

    fun money(v: Double?): String = if (v == null) "?" else String.format(java.util.Locale.US, "%.2f", v)

    fun round2(v: Double): Double = round(v * 100) / 100

    private fun toAmount(s: String): Double? = s.replace(',', '.').toDoubleOrNull()
}
