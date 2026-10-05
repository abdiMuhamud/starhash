package com.innovii.starhash

import android.content.Context
import com.innovii.starhash.core.RunReport
import com.innovii.starhash.core.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.io.File

/** The tests, the settings and the reports, as JSON files in the app's private storage. */
class Store(context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val workspaceFile = File(context.filesDir, "workspace.json")
    private val reportDir = File(context.filesDir, "reports").apply { mkdirs() }

    private val _workspace = MutableStateFlow(loadWorkspace())
    val workspace: StateFlow<Workspace> = _workspace

    private val _reports = MutableStateFlow(loadReports())
    val reports: StateFlow<List<RunReport>> = _reports

    @Synchronized
    fun update(change: (Workspace) -> Workspace) {
        val next = change(_workspace.value)
        _workspace.value = next
        writeAtomically(workspaceFile, json.encodeToString(Workspace.serializer(), next))
    }

    @Synchronized
    fun saveReport(r: RunReport) {
        writeAtomically(File(reportDir, "${r.id}.json"), json.encodeToString(RunReport.serializer(), r))
        val list = (listOf(r) + _reports.value.filter { it.id != r.id }).sortedByDescending { it.startedAt }
        val keep = list.take(MAX_REPORTS)
        list.drop(MAX_REPORTS).forEach { File(reportDir, "${it.id}.json").delete() }
        _reports.value = keep
    }

    @Synchronized
    fun deleteReport(id: String) {
        File(reportDir, "$id.json").delete()
        _reports.value = _reports.value.filter { it.id != id }
    }

    @Synchronized
    fun deleteAllReports() {
        reportDir.listFiles()?.forEach { it.delete() }
        _reports.value = emptyList()
    }

    fun report(id: String): RunReport? = _reports.value.firstOrNull { it.id == id }

    private fun loadWorkspace(): Workspace =
        runCatching { json.decodeFromString(Workspace.serializer(), workspaceFile.readText()) }.getOrNull() ?: Workspace()

    private fun loadReports(): List<RunReport> =
        reportDir.listFiles { f -> f.name.endsWith(".json") }.orEmpty()
            .mapNotNull { f -> runCatching { json.decodeFromString(RunReport.serializer(), f.readText()) }.getOrNull() }
            .sortedByDescending { it.startedAt }

    private fun writeAtomically(file: File, text: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }

    companion object {
        const val MAX_REPORTS = 200
    }
}
