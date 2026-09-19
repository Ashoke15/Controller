package com.diyproject.controller.devtools.macros

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Real playback engine — sends each step's command (via [send], which the
 * caller wires to actually write to the Bluetooth socket and log the
 * transmitted bytes), waits its delay, and repeats until the macro ends
 * or [stop] cancels it. Only one macro can run at a time by construction:
 * [run] is a no-op while [isRunning], since there's only one physical
 * link to send on.
 *
 * Cancellation is cooperative via [ensureActive] at each step boundary
 * and via `delay()`'s own cancellation point — [stop] cancels the
 * coroutine [Job], which unwinds the loop at the next checkpoint rather
 * than killing mid-write.
 */
class MacroExecutor(
    private val scope: CoroutineScope,
    private val send: (command: String) -> Unit,
    private val onEvent: (MacroExecutionEvent) -> Unit
) {
    private var job: Job? = null

    val isRunning: Boolean get() = job?.isActive == true

    fun run(macro: Macro) {
        if (isRunning) return

        job = scope.launch(Dispatchers.Default) {
            onEvent(MacroExecutionEvent.Started(macro.id, macro.steps.size))
            var completedCleanly = false
            try {
                for ((index, step) in macro.steps.withIndex()) {
                    ensureActive()
                    onEvent(MacroExecutionEvent.StepStarted(macro.id, index, step))

                    // A blank command is a deliberate wait-only step — see
                    // MacroStep's doc. Nothing goes over the wire for it,
                    // but its delay still runs.
                    if (step.command.isNotBlank()) {
                        send(step.command)
                    }

                    if (step.delayAfterMs > 0) delay(step.delayAfterMs)
                }
                completedCleanly = true
            } catch (_: CancellationException) {
                // Intentional stop — stop() already emitted Stopped.
            } finally {
                if (completedCleanly) onEvent(MacroExecutionEvent.Finished(macro.id))
            }
        }
    }

    fun stop() {
        val wasRunning = isRunning
        job?.cancel()
        job = null
        if (wasRunning) onEvent(MacroExecutionEvent.Stopped)
    }
}