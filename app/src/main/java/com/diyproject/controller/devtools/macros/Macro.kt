package com.diyproject.controller.devtools.macros

import java.util.UUID

object MacroDefaults {
    const val DEFAULT_STEP_DELAY_MS = 300L
    /** Upper bound on a single step's delay — guards against a typo (e.g.
     *  an extra zero) silently stalling a macro for minutes. */
    const val MAX_STEP_DELAY_MS = 60_000L
}

/**
 * One step in a macro. A blank [command] is a deliberate, valid state —
 * it means "wait [delayAfterMs] and send nothing," useful for pacing a
 * sequence without needing a no-op command the firmware has to ignore.
 */
data class MacroStep(
    val id: Long,
    val command: String,
    val delayAfterMs: Long
)

data class Macro(
    val id: String,
    val name: String,
    val steps: List<MacroStep>,
    val createdAtMs: Long,
    val updatedAtMs: Long
) {
    companion object {
        fun new(name: String = "New macro"): Macro {
            val now = System.currentTimeMillis()
            return Macro(
                id = UUID.randomUUID().toString(),
                name = name,
                steps = emptyList(),
                createdAtMs = now,
                updatedAtMs = now
            )
        }
    }
}