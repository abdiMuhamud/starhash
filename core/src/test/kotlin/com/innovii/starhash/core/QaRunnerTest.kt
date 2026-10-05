package com.innovii.starhash.core

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QaRunnerTest {
    private val fast = Settings(chargeWaitSec = 5, pauseSec = 2, smsWaitSec = 3)

    private fun TestScope.setup(balance: Double = 1.0, settings: Settings = fast, charges: Boolean = true): Pair<DemoNetwork, QaRunner> {
        val now = { testScheduler.currentTime }
        val net = DemoNetwork(balance = balance, now = now, charges = charges)
        return net to QaRunner(net, settings, net, now)
    }

    private fun case(id: String) = Defaults.tests().first { it.id == id }

    @Test
    fun `every default availability test passes on the demo network`() = runTest {
        val (_, runner) = setup()
        val results = runner.run(Defaults.tests().filter { !it.charges })
        results.forEach { assertEquals(Status.PASS, it.status, "${it.name}: ${it.summary}") }
        val explore = results.first { it.caseId == "400-explore" }
        assertEquals(10, explore.options.size)
        assertEquals(1.0, results.first { it.caseId == "balance" }.balanceBefore)
    }

    @Test
    fun `subscription is charged and the welcome SMS arrives`() = runTest {
        val (net, runner) = setup(balance = 1.0)
        val r = runner.runCase(case("400-market-sub"))
        assertEquals(Status.PASS, r.status, r.summary)
        assertEquals(1.0, r.balanceBefore)
        assertEquals(0.5, r.balanceAfter)
        assertEquals(0.5, r.charged)
        assertTrue(r.sms.any { it.from == "400" })
        assertTrue("Mobile Market" in net.subscriptions)
        assertTrue(r.log.count { it.type == LogType.DIAL } >= 3)
    }

    @Test
    fun `low balance is blocked, not a pass`() = runTest {
        val (_, runner) = setup(balance = 0.02)
        val r = runner.runCase(case("400-market-sub"))
        assertEquals(Status.BLOCKED, r.status, r.summary)
        assertEquals(0.0, r.charged)
    }

    @Test
    fun `subscribed without a charge is a fail`() = runTest {
        val (_, runner) = setup(balance = 1.0, charges = false)
        val r = runner.runCase(case("400-market-sub"))
        assertEquals(Status.FAIL, r.status, r.summary)
        assertTrue(r.summary.contains("NOT charged"))
    }

    @Test
    fun `already subscribed is a warning`() = runTest {
        val (net, runner) = setup(balance = 2.0)
        net.subscriptions += "Mobile Market"
        val r = runner.runCase(case("400-market-sub"))
        assertEquals(Status.WARN, r.status, r.summary)
    }

    @Test
    fun `wrong price is a warning`() = runTest {
        val (_, runner) = setup(balance = 1.0)
        val r = runner.runCase(case("400-market-sub").copy(price = 0.25))
        assertEquals(Status.WARN, r.status, r.summary)
    }

    @Test
    fun `an unknown code fails with the network message`() = runTest {
        val (_, runner) = setup()
        val r = runner.runCase(TestCase("x", "Bad code", Kind.MENU, code = "*999#"))
        assertEquals(Status.FAIL, r.status)
        assertTrue(r.summary.contains("MMI"), r.summary)
    }

    @Test
    fun `a missing expected word fails`() = runTest {
        val (_, runner) = setup()
        val r = runner.runCase(case("400-menu").copy(expectText = "Lottery"))
        assertEquals(Status.FAIL, r.status)
    }

    @Test
    fun `the menu closing early fails`() = runTest {
        val (_, runner) = setup()
        val r = runner.runCase(TestCase("x", "Too deep", Kind.MENU, code = "*400#", path = listOf("4", "2", "1")))
        assertEquals(Status.FAIL, r.status)
        assertTrue(r.summary.contains("closed"), r.summary)
    }

    @Test
    fun `one-shot engine sends chained codes`() = runTest {
        val now = { testScheduler.currentTime }
        val net = DemoNetwork(balance = 1.0, now = now)
        val oneShot = object : UssdDriver by net {
            override val supportsMenus = false
            val dialled = mutableListOf<String>()
            override suspend fun dial(code: String, timeoutMs: Long): UssdReply {
                dialled += code
                return net.dial(code, timeoutMs)
            }
        }
        val runner = QaRunner(oneShot, fast, net, now)
        val r = runner.runCase(case("400-market-sub"))
        assertEquals(Status.PASS, r.status, r.summary)
        assertTrue("*400*4*1*1*1#" in oneShot.dialled, oneShot.dialled.toString())
        assertTrue("*122*1#" in oneShot.dialled)
    }

    @Test
    fun `verdicts`() {
        val v = QaRunner::subscriptionVerdict
        assertEquals(Status.PASS, v(1.0, 0.5, 0.5, Outcome.SUCCESS, true, true, "USD").first)
        assertEquals(Status.WARN, v(1.0, 0.5, 0.5, Outcome.SUCCESS, true, false, "USD").first)
        assertEquals(Status.WARN, v(1.0, null, 0.5, Outcome.SUCCESS, false, false, "USD").first)
        assertEquals(Status.FAIL, v(0.02, 0.02, 0.5, Outcome.UNKNOWN, true, true, "USD").first)
        assertEquals(Status.BLOCKED, v(0.02, 0.02, 0.5, Outcome.UNKNOWN, true, false, "USD").first)
        assertEquals(Status.FAIL, v(1.0, 1.0, 0.5, Outcome.UNKNOWN, false, false, "USD").first)
    }

    @Test
    fun `reports and the workspace survive JSON and render`() = runTest {
        val (_, runner) = setup()
        val results = runner.run(listOf(case("balance"), case("400-market-sub")))
        val report = RunReport("r1", "All codes", 0, 1, Engine.DEMO, "Test phone", "SIM 1", results = results)
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        assertEquals(report, json.decodeFromString<RunReport>(json.encodeToString(RunReport.serializer(), report)))
        assertEquals(Workspace(), json.decodeFromString<Workspace>(json.encodeToString(Workspace())))
        val text = ReportFormat.text(report)
        assertTrue(text.contains("Charged 0.50 USD"), text)
        assertTrue(ReportFormat.html(report).contains("Mobile Market"))
        assertEquals(3, ReportFormat.csv(report).lines().filter { it.isNotBlank() }.size)
    }
}
