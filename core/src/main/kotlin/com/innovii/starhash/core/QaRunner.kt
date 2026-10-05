package com.innovii.starhash.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

sealed interface RunEvent {
    data class CaseStarted(val index: Int, val total: Int, val case: TestCase) : RunEvent
    data class Line(val line: LogLine) : RunEvent
    data class CaseDone(val result: CaseResult) : RunEvent
}

/**
 * Runs QA tests against a [UssdDriver]: dials, answers the menus, reads balances and SMS, and gives each test a
 * verdict. Every USSD session is closed afterwards, even when the run is stopped.
 */
class QaRunner(
    private val driver: UssdDriver,
    private val settings: Settings,
    private val sms: SmsSource,
    private val now: () -> Long = System::currentTimeMillis,
    private val onEvent: (RunEvent) -> Unit = {},
    private val random: Random = Random.Default,
) {
    private val cur get() = settings.currency

    suspend fun run(cases: List<TestCase>): List<CaseResult> {
        val out = mutableListOf<CaseResult>()
        for ((i, c) in cases.withIndex()) {
            if (i > 0) pause()
            onEvent(RunEvent.CaseStarted(i, cases.size, c))
            val r = runCase(c)
            out += r
            onEvent(RunEvent.CaseDone(r))
        }
        return out
    }

    suspend fun runCase(case: TestCase): CaseResult {
        val log = CaseLog()
        val base = CaseResult(
            caseId = case.id, name = case.name, kind = case.kind,
            route = if (case.kind == Kind.BALANCE) balanceRoute() else case.route,
            status = Status.FAIL, summary = "", startedAt = now(), endedAt = 0,
        )
        val r = try {
            when (case.kind) {
                Kind.BALANCE -> balanceCase(base, log)
                Kind.MENU -> menuCase(case, base, log)
                Kind.EXPLORE -> exploreCase(case, base, log)
                Kind.SUBSCRIBE -> subscribeCase(case, base, log)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.add(LogType.ERROR, "App error: ${e.message ?: e.javaClass.simpleName}")
            base.copy(status = Status.FAIL, summary = "App error: ${e.message ?: e.javaClass.simpleName}")
        }
        return r.copy(endedAt = now(), log = log.lines.toList())
    }

    private fun balanceRoute(): String {
        val p = settings.balancePath
        return if (p.isEmpty()) settings.balanceCode else settings.balanceCode + " → " + p.joinToString(" → ")
    }

    // --- tests ---

    private suspend fun balanceCase(base: CaseResult, log: CaseLog): CaseResult {
        val b = readBalance(log, "Balance")
        return when {
            b.value != null -> base.copy(
                status = Status.PASS, summary = "Balance ${UssdText.money(b.value)} $cur",
                balanceBefore = b.value, lastScreen = b.screen,
            )
            b.menuFailed -> base.copy(status = Status.FAIL, summary = "Balance code failed: ${b.failure}", lastScreen = b.screen)
            else -> base.copy(
                status = Status.WARN, lastScreen = b.screen,
                summary = "The menu answered but no balance was found in it or in an SMS (set the balance pattern in Settings)",
            )
        }
    }

    private suspend fun menuCase(case: TestCase, base: CaseResult, log: CaseLog): CaseResult {
        val w = walk(case.code, case.path, log)
        return when {
            w.failure != null -> base.copy(status = Status.FAIL, summary = w.failure, lastScreen = w.last)
            !UssdText.hasAny(w.last, case.expectText) -> base.copy(
                status = Status.FAIL, lastScreen = w.last,
                summary = "Last screen does not show “${case.expectText}”: ${UssdText.oneLine(w.last, 60)}",
            )
            else -> base.copy(
                status = Status.PASS, lastScreen = w.last,
                summary = "${w.screens.size} screen(s) answered: ${UssdText.oneLine(UssdText.firstLine(w.last), 60)}",
            )
        }
    }

    private suspend fun exploreCase(case: TestCase, base: CaseResult, log: CaseLog): CaseResult {
        val top = walk(case.code, case.path, log)
        if (top.failure != null) return base.copy(status = Status.FAIL, summary = top.failure, lastScreen = top.last)
        val options = UssdText.options(top.last).filterNot(UssdText::isBackOption)
        if (options.isEmpty()) {
            return base.copy(status = Status.WARN, summary = "The menu has no numbered options to open", lastScreen = top.last)
        }
        val checks = mutableListOf<OptionCheck>()
        for (o in options) {
            pause()
            log.add(LogType.NOTE, "Option ${o.key}: ${o.label}")
            val w = walk(case.code, case.path + o.key, log)
            val ok = w.failure == null && w.last.isNotBlank()
            checks += OptionCheck(o.key, o.label, ok, if (ok) w.last else (w.failure ?: "No answer"))
        }
        val bad = checks.filterNot { it.ok }
        return if (bad.isEmpty()) {
            base.copy(status = Status.PASS, summary = "All ${checks.size} options answered", options = checks, lastScreen = top.last)
        } else {
            base.copy(
                status = Status.FAIL, options = checks, lastScreen = top.last,
                summary = "${bad.size} of ${checks.size} options did not answer: " +
                    bad.joinToString(", ") { "${it.option}.${it.label}" },
            )
        }
    }

    private suspend fun subscribeCase(case: TestCase, base: CaseResult, log: CaseLog): CaseResult {
        val before = readBalance(log, "Balance before")
        if (before.value == null) {
            return base.copy(
                status = Status.BLOCKED, lastScreen = before.screen,
                summary = "Could not read the balance before subscribing (${before.failure}); nothing was dialled",
            )
        }
        pause()
        val smsStart = now()
        log.add(LogType.NOTE, "Subscribing: ${case.route}")
        val w = walk(case.code, case.path, log)
        val shown = w.screens.firstNotNullOfOrNull { UssdText.price(it) }
        val price = case.price ?: shown
        if (case.price != null && shown != null && abs(case.price - shown) > 0.009) {
            log.add(LogType.NOTE, "The menu shows the price ${UssdText.money(shown)}, the test expects ${UssdText.money(case.price)}")
        }
        if (w.failure != null) {
            return base.copy(
                status = Status.FAIL, summary = "The service did not answer: ${w.failure}",
                balanceBefore = before.value, price = price, lastScreen = w.last, sms = sms.since(smsStart),
            )
        }
        val outcome = UssdText.outcome(w.last)
        log.add(LogType.NOTE, "Answer reads as: ${outcome.name.lowercase()}")

        var after: Double? = null
        for (i in 1..max(1, settings.chargeChecks)) {
            log.add(LogType.NOTE, "Waiting ${settings.chargeWaitSec}s for the charge (check $i)")
            delay(settings.chargeWaitSec * 1000L)
            val b = readBalance(log, "Balance after")
            if (b.value != null) after = b.value
            if (after != null && after < before.value - 0.0001) break
            if (outcome == Outcome.INSUFFICIENT || outcome == Outcome.ALREADY) break
            if (i < settings.chargeChecks) pause()
        }

        val received = sms.since(smsStart)
        val smsExpected = case.smsFrom.isNotBlank() || case.smsText.isNotBlank()
        val smsSeen = received.any { smsMatches(case, it) }
        if (smsExpected) log.add(LogType.NOTE, if (smsSeen) "Confirmation SMS received" else "No confirmation SMS")

        val (status, summary) = subscriptionVerdict(before.value, after, price, outcome, smsExpected, smsSeen, cur)
        if (case.unsubscribeCode.isNotBlank()) {
            pause()
            log.add(LogType.NOTE, "Unsubscribing: ${case.unsubscribeCode} ${case.unsubscribePath.joinToString(" ")}")
            val u = walk(case.unsubscribeCode, case.unsubscribePath, log)
            log.add(LogType.NOTE, if (u.failure == null) "Unsubscribe answered: ${UssdText.oneLine(u.last)}" else "Unsubscribe failed: ${u.failure}")
        }
        return base.copy(
            status = status, summary = summary, balanceBefore = before.value, balanceAfter = after,
            charged = after?.let { UssdText.round2(before.value - it) }, price = price, lastScreen = w.last, sms = received,
        )
    }

    // --- sessions ---

    private class Walk(val screens: List<String>, val failure: String?) {
        val last: String get() = screens.lastOrNull() ?: ""
    }

    private suspend fun walk(code: String, path: List<String>, log: CaseLog): Walk {
        val timeout = settings.screenTimeoutSec * 1000L
        val screens = mutableListOf<String>()
        try {
            if (!driver.supportsMenus) {
                val full = UssdText.chain(code, path)
                log.add(LogType.DIAL, full)
                val failure = take(driver.dial(full, timeout), screens, log)
                if (failure == null) read()
                return Walk(screens, failure)
            }
            val c = UssdText.normalizeCode(code)
            log.add(LogType.DIAL, c)
            var r = driver.dial(c, timeout)
            for ((i, step) in path.withIndex()) {
                take(r, screens, log)?.let { return Walk(screens, it) }
                if (!(r as UssdReply.Screen).canReply) {
                    return Walk(screens, "The menu closed before answer ${i + 1} (“$step”)")
                }
                read()
                log.add(LogType.REPLY, step)
                r = driver.reply(step, timeout)
            }
            val failure = take(r, screens, log)
            if (failure == null) read() // look at the last screen before closing it, as a person would
            return Walk(screens, failure)
        } finally {
            withContext(NonCancellable) {
                driver.close()
                log.add(LogType.CLOSE, "Session closed")
            }
        }
    }

    /** Records an answer; returns why it is a failure, or null. */
    private fun take(r: UssdReply, screens: MutableList<String>, log: CaseLog): String? = when (r) {
        is UssdReply.Failed -> {
            log.add(LogType.ERROR, r.reason)
            r.reason
        }
        is UssdReply.Screen -> {
            screens += r.text
            log.add(LogType.SCREEN, r.text)
            UssdText.errorIn(r.text)?.let { "The network answered: $it" }
        }
    }

    private class Balance(val value: Double?, val failure: String?, val menuFailed: Boolean, val screen: String)

    private suspend fun readBalance(log: CaseLog, label: String): Balance {
        val started = now()
        val w = walk(settings.balanceCode, settings.balancePath, log)
        val fromScreen = w.screens.asReversed().firstNotNullOfOrNull { UssdText.balance(it, settings.balancePattern) }
        if (fromScreen != null) {
            log.add(LogType.BALANCE, "$label: ${UssdText.money(fromScreen)} $cur")
            return Balance(fromScreen, null, false, w.last)
        }
        val deadline = now() + settings.smsWaitSec * 1000L
        while (true) {
            val v = sms.since(started).asReversed().firstNotNullOfOrNull { UssdText.balance(it.body, settings.balancePattern) }
            if (v != null) {
                log.add(LogType.BALANCE, "$label: ${UssdText.money(v)} $cur (from the balance SMS)")
                return Balance(v, null, false, w.last)
            }
            if (now() >= deadline) break
            delay(1000)
        }
        val why = w.failure ?: "no balance in the answer or in an SMS"
        log.add(LogType.ERROR, "$label: $why")
        return Balance(null, why, w.failure != null, w.last)
    }

    /** Between two USSD sessions. Human pace rests at least [Settings.HUMAN_MIN_PAUSE_SEC], give or take. */
    private suspend fun pause() {
        val sec = if (settings.human) max(settings.pauseSec, Settings.HUMAN_MIN_PAUSE_SEC) else settings.pauseSec
        if (sec > 0) delay(humanize(sec * 1000L))
    }

    /** Human pace: the time a person takes to read a menu before answering. */
    private suspend fun read() {
        if (settings.human && settings.readSec > 0) delay(humanize(settings.readSec * 1000L))
    }

    /** People are not metronomes: 80–130 % of the time asked for (only at human pace). */
    private fun humanize(ms: Long): Long = if (settings.human) (ms * (0.8 + random.nextDouble() * 0.5)).toLong() else ms

    private inner class CaseLog {
        val lines = mutableListOf<LogLine>()
        fun add(type: LogType, text: String) {
            val l = LogLine(now(), type, text)
            lines += l
            onEvent(RunEvent.Line(l))
        }
    }

    companion object {
        fun smsMatches(case: TestCase, s: Sms): Boolean {
            val fromOk = case.smsFrom.isBlank() || s.from.replace(" ", "").endsWith(case.smsFrom.trim())
            return fromOk && UssdText.hasAny(s.body, case.smsText)
        }

        /** Did the subscription charge what it should? */
        fun subscriptionVerdict(
            before: Double,
            after: Double?,
            price: Double?,
            outcome: Outcome,
            smsExpected: Boolean,
            smsSeen: Boolean,
            cur: String,
        ): Pair<Status, String> {
            val m = UssdText::money
            if (after == null) return Status.WARN to "Subscribed, but the balance after could not be read"
            val charged = UssdText.round2(before - after)
            val noSms = if (smsExpected && !smsSeen) "; no confirmation SMS" else ""
            val move = "balance ${m(before)} → ${m(after)}"
            return when {
                charged > 0 && price != null && abs(charged - price) > 0.009 ->
                    Status.WARN to "Charged ${m(charged)} $cur but the price is ${m(price)} ($move)$noSms"
                charged > 0 ->
                    (if (noSms.isEmpty()) Status.PASS else Status.WARN) to "Charged ${m(charged)} $cur ($move)$noSms"
                charged < 0 ->
                    Status.WARN to "The balance went up ($move): a top-up during the test?"
                outcome == Outcome.INSUFFICIENT ->
                    Status.BLOCKED to "Not charged: the network says the balance (${m(before)}) is too low. Top up the test SIM."
                outcome == Outcome.SUCCESS || (smsExpected && smsSeen) ->
                    Status.FAIL to "Subscribed but NOT charged: balance stayed ${m(before)} $cur (free trial?)"
                price != null && before < price ->
                    Status.BLOCKED to "Not charged: balance ${m(before)} is below the price ${m(price)}. Top up the test SIM."
                outcome == Outcome.ALREADY ->
                    Status.WARN to "Not charged: this number is already subscribed"
                else ->
                    Status.FAIL to "Not charged: balance stayed ${m(before)} $cur"
            }
        }
    }
}
