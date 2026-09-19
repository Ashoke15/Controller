package com.diyproject.controller.devtools.macros

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.diyproject.controller.control.BluetoothSppManager
import com.diyproject.controller.devtools.common.LineEnding
import com.diyproject.controller.devtools.common.RawCommand
import com.diyproject.controller.devtools.common.TranscriptDirection
import com.diyproject.controller.devtools.common.TranscriptEntry
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Hosts both Macro screens (list + editor, switched by whether
 * [editingDraft] is null) and owns the real Bluetooth connection and
 * [MacroExecutor] that actually drives playback — same shared
 * BluetoothSppManager class as the other dev tools, its own instance so
 * this screen works standalone.
 */
class MacroActivity : ComponentActivity() {

    private lateinit var bt: BluetoothSppManager
    private lateinit var macroStore: MacroStore
    private lateinit var macroExecutor: MacroExecutor

    private var isConnected by mutableStateOf(false)
    private var lineEnding by mutableStateOf(LineEnding.NL)

    private var macros by mutableStateOf<List<Macro>>(emptyList())
    private var editingDraft by mutableStateOf<Macro?>(null)

    private var runningMacroId by mutableStateOf<String?>(null)
    private var runningStepIndex by mutableStateOf<Int?>(null)
    private var runningTotalSteps by mutableStateOf<Int?>(null)

    private val log = mutableStateListOf<TranscriptEntry>()
    private val nextLogId = AtomicLong(0)
    private var nextStepId = AtomicLong(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        macroStore = MacroStore(this)
        macros = macroStore.loadAll()

        bt = BluetoothSppManager(
            context = this,
            onConnected = { isConnected = true; appendLog(TranscriptDirection.SYSTEM, "connected") },
            onDisconnected = {
                isConnected = false
                appendLog(TranscriptDirection.SYSTEM, "disconnected")
                // A dropped link mid-macro can't keep sending — stop cleanly
                // rather than leaving the executor waiting to write to a
                // dead socket.
                macroExecutor.stop()
            },
            onError = { msg ->
                runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                appendLog(TranscriptDirection.SYSTEM, "error: $msg")
            }
        )

        macroExecutor = MacroExecutor(
            scope = lifecycleScope,
            send = { rawCommand -> sendMacroCommand(rawCommand) },
            onEvent = { event -> runOnUiThread { handleMacroEvent(event) } }
        )

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier, color = Color.Transparent) {
                    val draft = editingDraft
                    if (draft == null) {
                        MacroListScreen(
                            isConnected = isConnected,
                            macros = macros,
                            runningMacroId = runningMacroId,
                            runningStepIndex = runningStepIndex,
                            runningTotalSteps = runningTotalSteps,
                            log = log,
                            lineEnding = lineEnding,
                            onLineEndingChange = { lineEnding = it },
                            onRun = { macro -> runMacro(macro) },
                            onStop = { macroExecutor.stop() },
                            onNew = { editingDraft = Macro.new() },
                            onEdit = { macro -> editingDraft = macro },
                            onDuplicate = { macro -> duplicateMacro(macro) },
                            onDelete = { macro -> deleteMacro(macro) },
                            onConnectClick = { checkBluetoothPermissionsAndShowPicker() },
                            onClearLog = { log.clear() }
                        )
                    } else {
                        MacroEditorScreen(
                            draft = draft,
                            isNewMacro = macros.none { it.id == draft.id },
                            onNameChange = { name -> editingDraft = draft.copy(name = name) },
                            onAddStep = { addDraftStep() },
                            onUpdateStepCommand = { id, command -> updateDraftStepCommand(id, command) },
                            onUpdateStepDelay = { id, delay -> updateDraftStepDelay(id, delay) },
                            onRemoveStep = { id -> removeDraftStep(id) },
                            onMoveStep = { id, delta -> moveDraftStep(id, delta) },
                            onSave = { saveDraft() },
                            onCancel = { editingDraft = null },
                            onDeleteMacro = { deleteMacro(draft) }
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Execution
    // -------------------------------------------------------------

    private fun runMacro(macro: Macro) {
        if (!isConnected) {
            Toast.makeText(this, "Connect first", Toast.LENGTH_SHORT).show()
            return
        }
        if (macroExecutor.isRunning) {
            Toast.makeText(this, "A macro is already running", Toast.LENGTH_SHORT).show()
            return
        }
        macroExecutor.run(macro)
    }

    /** The single real transmit path for macro steps: applies the chosen
     *  line ending, writes to the socket, and logs the exact bytes sent —
     *  this is the source of truth for the execution log, not a copy of
     *  it derived from events. */
    private fun sendMacroCommand(rawCommand: String) {
        val withEnding = RawCommand.withLineEnding(rawCommand, lineEnding)
        bt.send(withEnding)
        appendLog(TranscriptDirection.TX, withEnding)
    }

    private fun handleMacroEvent(event: MacroExecutionEvent) {
        when (event) {
            is MacroExecutionEvent.Started -> {
                runningMacroId = event.macroId
                runningStepIndex = null
                runningTotalSteps = event.totalSteps
                appendLog(TranscriptDirection.SYSTEM, "macro started: ${macroNameFor(event.macroId)}")
            }
            is MacroExecutionEvent.StepStarted -> {
                runningStepIndex = event.stepIndex
            }
            is MacroExecutionEvent.Finished -> {
                appendLog(TranscriptDirection.SYSTEM, "macro finished: ${macroNameFor(event.macroId)}")
                runningMacroId = null
                runningStepIndex = null
                runningTotalSteps = null
            }
            MacroExecutionEvent.Stopped -> {
                appendLog(TranscriptDirection.SYSTEM, "macro stopped")
                runningMacroId = null
                runningStepIndex = null
                runningTotalSteps = null
            }
        }
    }

    private fun macroNameFor(id: String): String = macros.find { it.id == id }?.name ?: id

    private fun appendLog(direction: TranscriptDirection, raw: String) {
        log.add(TranscriptEntry(nextLogId.getAndIncrement(), direction, raw, System.currentTimeMillis()))
        if (log.size > MAX_LOG_ENTRIES) log.removeRange(0, log.size - MAX_LOG_ENTRIES)
    }

    // -------------------------------------------------------------
    // Library CRUD
    // -------------------------------------------------------------

    private fun duplicateMacro(macro: Macro) {
        val now = System.currentTimeMillis()
        val copy = macro.copy(id = UUID.randomUUID().toString(), name = "${macro.name} copy", createdAtMs = now, updatedAtMs = now)
        macros = macros + copy
        macroStore.saveAll(macros)
    }

    private fun deleteMacro(macro: Macro) {
        if (macro.id == runningMacroId) return // UI already disables this path
        macros = macros.filterNot { it.id == macro.id }
        macroStore.saveAll(macros)
        if (editingDraft?.id == macro.id) editingDraft = null
    }

    // -------------------------------------------------------------
    // Draft editing
    // -------------------------------------------------------------

    private fun addDraftStep() {
        val d = editingDraft ?: return
        val newStep = MacroStep(id = nextStepId.getAndIncrement(), command = "", delayAfterMs = MacroDefaults.DEFAULT_STEP_DELAY_MS)
        editingDraft = d.copy(steps = d.steps + newStep)
    }

    private fun updateDraftStepCommand(stepId: Long, command: String) {
        val d = editingDraft ?: return
        editingDraft = d.copy(steps = d.steps.map { if (it.id == stepId) it.copy(command = command) else it })
    }

    private fun updateDraftStepDelay(stepId: Long, delayMs: Long) {
        val d = editingDraft ?: return
        val clamped = delayMs.coerceIn(0, MacroDefaults.MAX_STEP_DELAY_MS)
        editingDraft = d.copy(steps = d.steps.map { if (it.id == stepId) it.copy(delayAfterMs = clamped) else it })
    }

    private fun removeDraftStep(stepId: Long) {
        val d = editingDraft ?: return
        editingDraft = d.copy(steps = d.steps.filterNot { it.id == stepId })
    }

    private fun moveDraftStep(stepId: Long, delta: Int) {
        val d = editingDraft ?: return
        val index = d.steps.indexOfFirst { it.id == stepId }
        if (index < 0) return
        val target = index + delta
        if (target !in d.steps.indices) return
        val mutable = d.steps.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(target, item)
        editingDraft = d.copy(steps = mutable)
    }

    private fun saveDraft() {
        val d = editingDraft ?: return
        if (d.steps.isEmpty()) return
        val finalized = d.copy(name = d.name.ifBlank { "Untitled macro" }, updatedAtMs = System.currentTimeMillis())
        macros = if (macros.any { it.id == finalized.id }) {
            macros.map { if (it.id == finalized.id) finalized else it }
        } else {
            macros + finalized
        }
        macroStore.saveAll(macros)
        editingDraft = null
    }

    // -------------------------------------------------------------
    // Connection
    // -------------------------------------------------------------

    private fun checkBluetoothPermissionsAndShowPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 104)
                return
            }
        }
        showDevicePicker()
    }

    private fun showDevicePicker() {
        try {
            val devices = bt.getPairedDevices()
            when {
                devices.isEmpty() -> Toast.makeText(this, "No paired devices. Pair your HC-05 first.", Toast.LENGTH_LONG).show()
                devices.size == 1 -> bt.connect(devices.first())
                else -> {
                    val names = devices.map { v -> v.name ?: v.address }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("Select device")
                        .setItems(names) { _, which -> bt.connect(devices[which]) }
                        .show()
                }
            }
        } catch (_: SecurityException) {
            Toast.makeText(this, "Bluetooth permission missing", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        macroExecutor.stop()
        bt.disconnect()
    }

    private companion object {
        const val MAX_LOG_ENTRIES = 500
    }
}