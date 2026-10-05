package com.innovii.starhash

import android.content.Intent
import android.os.Build
import com.innovii.starhash.core.CaseResult
import com.innovii.starhash.core.Engine
import com.innovii.starhash.core.LogLine
import com.innovii.starhash.core.QaRunner
import com.innovii.starhash.core.RunEvent
import com.innovii.starhash.core.RunReport
import com.innovii.starhash.core.SmsSource
import com.innovii.starhash.core.TestCase
import com.innovii.starhash.core.UssdDriver
import com.innovii.starhash.ussd.OneShotDriver
import com.innovii.starhash.ussd.ScreenDriver
import com.innovii.starhash.ussd.Sims
import com.innovii.starhash.ussd.SmsWatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** What the run screen shows. */
data class LiveRun(
    val title: String,
    val engine: Engine,
    val total: Int,
    val index: Int = 0,
    val current: TestCase? = null,
    val lines: List<LogLine> = emptyList(),
    val results: List<CaseResult> = emptyList(),
    val running: Boolean = true,
    val reportId: String? = null,
)

/** Runs tests in the background of the app and saves a report at the end, also when stopped. */
class RunController(private val app: StarHashApp) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _live = MutableStateFlow<LiveRun?>(null)
    val live: StateFlow<LiveRun?> = _live

    val running: Boolean get() = _live.value?.running == true

    fun start(title: String, cases: List<TestCase>) {
        if (running || cases.isEmpty()) return
        val ws = app.store.workspace.value
        val engine = ws.settings.engine
        val settings = if (engine == Engine.DEMO) {
            ws.settings.copy(chargeWaitSec = 2, pauseSec = 1, smsWaitSec = 2, chargeChecks = 2)
        } else {
            ws.settings
        }
        val demo = app.demo
        val driver: UssdDriver = when (engine) {
            Engine.SCREEN -> ScreenDriver(app, settings.subscriptionId)
            Engine.ONE_SHOT -> OneShotDriver(app, settings.subscriptionId)
            Engine.DEMO -> demo
        }
        val sms: SmsSource = if (engine == Engine.DEMO) demo else SmsWatcher
        val sim = if (engine == Engine.DEMO) "Demo SIM" else Sims.label(app, settings.subscriptionId)

        _live.value = LiveRun(title, engine, cases.size)
        job = scope.launch {
            val startedAt = System.currentTimeMillis()
            var stopped = false
            val runner = QaRunner(driver, settings, sms, onEvent = ::onEvent)
            try {
                runner.run(cases)
            } catch (e: CancellationException) {
                stopped = true
            }
            withContext(NonCancellable) {
                val results = _live.value?.results.orEmpty()
                val report = RunReport(
                    id = UUID.randomUUID().toString(), title = title, startedAt = startedAt,
                    endedAt = System.currentTimeMillis(), engine = engine,
                    device = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}",
                    sim = sim, currency = settings.currency, stopped = stopped, results = results,
                )
                if (results.isNotEmpty()) app.store.saveReport(report)
                _live.update { it?.copy(running = false, current = null, reportId = report.id.takeIf { results.isNotEmpty() }) }
                if (engine != Engine.DEMO) bringToFront()
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    fun clear() {
        if (!running) _live.value = null
    }

    private fun onEvent(e: RunEvent) {
        _live.update { run ->
            run ?: return@update null
            when (e) {
                is RunEvent.CaseStarted -> run.copy(index = e.index, current = e.case)
                is RunEvent.Line -> run.copy(lines = (run.lines + e.line).takeLast(MAX_LINES))
                is RunEvent.CaseDone -> run.copy(results = run.results + e.result)
            }
        }
    }

    /** The pop-ups cover the app during a run: show it again with the results. */
    private fun bringToFront() {
        runCatching {
            app.startActivity(
                Intent(app, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            )
        }
    }

    companion object {
        private const val MAX_LINES = 400
    }
}
