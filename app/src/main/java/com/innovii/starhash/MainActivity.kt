package com.innovii.starhash

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innovii.starhash.core.Appearance
import com.innovii.starhash.core.Defaults
import com.innovii.starhash.core.Engine
import com.innovii.starhash.core.TestCase
import com.innovii.starhash.core.UssdText
import com.innovii.starhash.ui.EditTestScreen
import com.innovii.starhash.ui.HomeScreen
import com.innovii.starhash.ui.LoadingScreen
import com.innovii.starhash.ui.PERMISSIONS
import com.innovii.starhash.ui.ReportScreen
import com.innovii.starhash.ui.ReportsScreen
import com.innovii.starhash.ui.RunScreen
import com.innovii.starhash.ui.SettingsScreen
import com.innovii.starhash.ui.SetupState
import com.innovii.starhash.ui.Sh
import com.innovii.starhash.ui.StarHashTheme
import com.innovii.starhash.ui.openAccessibilitySettings
import com.innovii.starhash.ussd.Sims
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as StarHashApp
        setContent {
            val ws by app.store.workspace.collectAsStateWithLifecycle()
            val dark = when (ws.settings.appearance) {
                Appearance.DARK -> true
                Appearance.LIGHT -> false
                Appearance.PHONE -> isSystemInDarkTheme()
            }
            LaunchedEffect(dark) {
                val bars = if (dark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            StarHashTheme(dark) {
                // The loading screen: StarHash and INNOVII for a moment, once per launch.
                var loading by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1_600)
                    loading = false
                }
                Crossfade(targetState = loading, label = "launch") { showLoading ->
                    if (showLoading) LoadingScreen() else AppRoot(app, this@MainActivity)
                }
            }
        }
    }
}

private sealed interface Page {
    data object Tests : Page
    data object Reports : Page
    data object Settings : Page
    data object Run : Page
    data class Report(val id: String) : Page
    data class Edit(val id: String?) : Page
}

@Composable
private fun AppRoot(app: StarHashApp, activity: ComponentActivity) {
    val context = LocalContext.current
    val ws by app.store.workspace.collectAsStateWithLifecycle()
    val reports by app.store.reports.collectAsStateWithLifecycle()
    val live by app.runs.live.collectAsStateWithLifecycle()

    var stack by remember { mutableStateOf(listOf<Page>(Page.Tests)) }
    val page = stack.last()
    fun go(p: Page) { stack = stack + p }
    fun back() { if (stack.size > 1) stack = stack.dropLast(1) }
    BackHandler(enabled = stack.size > 1) { back() }

    // Permissions and the accessibility switch change outside the app: look again on every return.
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
    val setup = remember(tick) { SetupState.read(context) }
    val sims = remember(tick) { Sims.list(context) }
    val simLabel = remember(tick, ws.settings.subscriptionId) { Sims.label(context, ws.settings.subscriptionId) }

    val running = live?.running == true
    DisposableEffect(running) {
        if (running) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { }
    }

    var confirmCharge by remember { mutableStateOf<Pair<String, List<TestCase>>?>(null) }
    var blocker by remember { mutableStateOf<String?>(null) }

    fun start(title: String, cases: List<TestCase>) {
        app.runs.clear()
        app.runs.start(title, cases)
        go(Page.Run)
    }

    fun launch(title: String, cases: List<TestCase>) {
        if (cases.isEmpty()) return
        if (app.runs.running) {
            go(Page.Run)
            return
        }
        val engine = ws.settings.engine
        val now = SetupState.read(context)
        if (!now.readyFor(engine)) {
            blocker = if (!now.phone) {
                "StarHash needs the Phone permission to dial the codes. Tap Allow on the setup card."
            } else {
                "Turn on the StarHash accessibility service first: it reads the USSD pop-ups and answers the menus."
            }
            return
        }
        if (engine != Engine.DEMO && cases.any { it.charges }) {
            confirmCharge = title to cases
            return
        }
        start(title, cases)
    }

    val tabs = listOf(Page.Tests, Page.Reports, Page.Settings)
    Scaffold(
        containerColor = Sh.Background,
        contentColor = Sh.Ink,
        bottomBar = {
            if (page in tabs) {
                NavigationBar(containerColor = Sh.Bar, tonalElevation = 0.dp) {
                    val item = NavigationBarItemDefaults.colors(
                        selectedIconColor = Sh.Accent,
                        selectedTextColor = Sh.Ink,
                        indicatorColor = Sh.Accent.copy(alpha = 0.16f),
                        unselectedIconColor = Sh.Faint,
                        unselectedTextColor = Sh.Faint,
                    )
                    NavigationBarItem(
                        selected = page == Page.Tests, onClick = { stack = listOf(Page.Tests) }, colors = item,
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) }, label = { Text("Tests") },
                    )
                    NavigationBarItem(
                        selected = page == Page.Reports, onClick = { stack = listOf(Page.Reports) }, colors = item,
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }, label = { Text("Reports") },
                    )
                    NavigationBarItem(
                        selected = page == Page.Settings, onClick = { stack = listOf(Page.Settings) }, colors = item,
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) }, label = { Text("Settings") },
                    )
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (val p = page) {
                Page.Tests -> HomeScreen(
                    ws = ws, setup = setup, simLabel = simLabel, live = live, lastReport = reports.firstOrNull(), context = context,
                    onOpenReport = { go(Page.Report(it)) },
                    onAskPermissions = { permissions.launch(PERMISSIONS) },
                    onRun = ::launch,
                    onOpenRun = { go(Page.Run) },
                    onEdit = { id -> go(Page.Edit(id)) },
                    onToggle = { t ->
                        app.store.update { w -> w.copy(tests = w.tests.map { if (it.id == t.id) it.copy(enabled = !it.enabled) else it }) }
                    },
                )
                Page.Reports -> ReportsScreen(
                    reports = reports, onOpen = { go(Page.Report(it)) }, onDeleteAll = { app.store.deleteAllReports() },
                )
                Page.Settings -> {
                    var demoTick by remember { mutableIntStateOf(0) }
                    SettingsScreen(
                        settings = ws.settings, sims = sims, version = BuildConfig.VERSION_NAME,
                        demoBalance = remember(demoTick, live?.running) { app.demo.balance },
                        onChange = { change -> app.store.update { w -> w.copy(settings = change(w.settings)) } },
                        onResetDemo = { app.resetDemo(); demoTick++ },
                        onRestoreTests = { app.store.update { it.copy(tests = Defaults.tests()) } },
                    )
                }
                Page.Run -> RunScreen(
                    live = live, onBack = { back() }, onStop = { app.runs.stop() },
                    onOpenReport = { id -> stack = listOf(Page.Reports, Page.Report(id)) },
                )
                is Page.Report -> ReportScreen(
                    report = reports.firstOrNull { it.id == p.id }, context = context, onBack = { back() },
                    onDelete = { id -> app.store.deleteReport(id); back() },
                )
                is Page.Edit -> EditTestScreen(
                    existing = p.id?.let { id -> ws.tests.firstOrNull { it.id == id } },
                    onBack = { back() },
                    onSave = { t ->
                        app.store.update { w ->
                            val known = w.tests.any { it.id == t.id }
                            w.copy(tests = if (known) w.tests.map { if (it.id == t.id) t else it } else w.tests + t)
                        }
                    },
                    onDelete = { id -> app.store.update { w -> w.copy(tests = w.tests.filter { it.id != id }) }; back() },
                    onRun = { t -> back(); launch(t.name, listOf(t)) },
                )
            }
        }
    }

    confirmCharge?.let { (title, cases) ->
        val charging = cases.filter { it.charges }
        AlertDialog(
            onDismissRequest = { confirmCharge = null },
            title = { Text("This charges the SIM") },
            text = {
                Text(
                    charging.joinToString("\n") { t ->
                        "• ${t.name}: ${t.route}" + (t.price?.let { " (${UssdText.money(it)} ${ws.settings.currency})" } ?: "")
                    } + "\n\nStarHash reads the balance before and after, so the test SIM needs enough credit.",
                )
            },
            confirmButton = { TextButton(onClick = { confirmCharge = null; start(title, cases) }) { Text("Subscribe and check") } },
            dismissButton = { TextButton(onClick = { confirmCharge = null }) { Text("Cancel") } },
        )
    }
    blocker?.let { msg ->
        AlertDialog(
            onDismissRequest = { blocker = null },
            title = { Text("Not ready yet") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = {
                    blocker = null
                    if (SetupState.read(context).phone) openAccessibilitySettings(context) else permissions.launch(PERMISSIONS)
                }) { Text("Fix it") }
            },
            dismissButton = { TextButton(onClick = { blocker = null }) { Text("Later") } },
        )
    }
}
