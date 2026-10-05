package com.innovii.starhash.core

import kotlinx.serialization.Serializable

/** What a test does. */
@Serializable
enum class Kind(val label: String) {
    /** Dial the balance code (Settings) and read the balance from the menu or the balance SMS. */
    BALANCE("Balance"),

    /** Dial a code, answer the menus in [TestCase.path], check the last screen. */
    MENU("Menu check"),

    /** Dial a code, then open every numbered option of the menu once (never answers the next screen). */
    EXPLORE("Explore menu"),

    /** Balance before, subscribe through the menus, balance after: was the SIM charged? */
    SUBSCRIBE("Subscribe + charge"),
}

/** How the app talks to the network. */
@Serializable
enum class Engine(val label: String) {
    /** Dials like a person and reads the USSD pop-ups (needs the accessibility service). Follows menus. */
    SCREEN("Screen reader"),

    /** Android's USSD API: one request per test, menus sent as a chain (*400*4*1#). No accessibility needed. */
    ONE_SHOT("One-shot API"),

    /** A built-in copy of the Telesom menus: try the app without a SIM or a charge. */
    DEMO("Demo network"),
}

@Serializable
data class TestCase(
    val id: String,
    val name: String,
    val kind: Kind,
    val code: String = "",
    /** The answers typed into the menus, in order, e.g. 4, 1, 1, 1. */
    val path: List<String> = emptyList(),
    /** Words, separated by commas; the last screen must show at least one. */
    val expectText: String = "",
    /** Expected charge; null means use the price shown in the menus. */
    val price: Double? = null,
    /** Sender of the SMS expected after subscribing (e.g. 400); empty = no SMS check. */
    val smsFrom: String = "",
    /** Words the SMS should contain; empty = any SMS from [smsFrom]. */
    val smsText: String = "",
    val unsubscribeCode: String = "",
    val unsubscribePath: List<String> = emptyList(),
    val enabled: Boolean = true,
    val note: String = "",
) {
    val charges: Boolean get() = kind == Kind.SUBSCRIBE

    /** What is dialled, e.g. "*400# → 4 → 1". */
    val route: String
        get() {
            val c = if (kind == Kind.BALANCE) "balance code" else code
            return if (path.isEmpty()) c else c + " → " + path.joinToString(" → ")
        }
}

@Serializable
data class Settings(
    val balanceCode: String = "*122#",
    val balancePath: List<String> = listOf("1"),
    /** Optional regular expression whose first group is the balance. */
    val balancePattern: String = "",
    val currency: String = "USD",
    val screenTimeoutSec: Int = 30,
    /** Wait after subscribing before reading the balance again. */
    val chargeWaitSec: Int = 20,
    /** How many times to read the balance again while it has not changed. */
    val chargeChecks: Int = 3,
    /** Pause between two USSD sessions, so the network closes the last one. */
    val pauseSec: Int = 4,
    /** How long to wait for the balance SMS when the menu does not show the balance. */
    val smsWaitSec: Int = 10,
    val engine: Engine = Engine.SCREEN,
    /** Android subscription id of the SIM to use; -1 = the phone's default. */
    val subscriptionId: Int = -1,
)

@Serializable
enum class Status(val label: String) { PASS("Pass"), WARN("Check"), FAIL("Fail"), BLOCKED("Blocked") }

@Serializable
enum class LogType { DIAL, SCREEN, REPLY, CLOSE, SMS, BALANCE, NOTE, ERROR }

@Serializable
data class LogLine(val at: Long, val type: LogType, val text: String)

@Serializable
data class Sms(val from: String, val body: String, val at: Long)

@Serializable
data class OptionCheck(val option: String, val label: String, val ok: Boolean, val screen: String)

@Serializable
data class CaseResult(
    val caseId: String,
    val name: String,
    val kind: Kind,
    val route: String,
    val status: Status,
    val summary: String,
    val startedAt: Long,
    val endedAt: Long,
    val balanceBefore: Double? = null,
    val balanceAfter: Double? = null,
    val charged: Double? = null,
    val price: Double? = null,
    val lastScreen: String = "",
    val sms: List<Sms> = emptyList(),
    val options: List<OptionCheck> = emptyList(),
    val log: List<LogLine> = emptyList(),
) {
    val seconds: Long get() = ((endedAt - startedAt) / 1000).coerceAtLeast(0)
}

@Serializable
data class RunReport(
    val id: String,
    val title: String,
    val startedAt: Long,
    val endedAt: Long,
    val engine: Engine,
    val device: String = "",
    val sim: String = "",
    val currency: String = "USD",
    val stopped: Boolean = false,
    val results: List<CaseResult> = emptyList(),
) {
    fun count(s: Status) = results.count { it.status == s }

    val overall: Status
        get() = when {
            results.isEmpty() -> Status.BLOCKED
            results.any { it.status == Status.FAIL } -> Status.FAIL
            results.any { it.status == Status.BLOCKED } -> Status.BLOCKED
            results.any { it.status == Status.WARN } -> Status.WARN
            else -> Status.PASS
        }
}

/** Everything the app keeps: the tests and the settings. */
@Serializable
data class Workspace(
    val settings: Settings = Settings(),
    val tests: List<TestCase> = Defaults.tests(),
)
