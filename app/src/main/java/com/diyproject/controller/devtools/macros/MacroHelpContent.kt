package com.diyproject.controller.devtools.macros

import com.diyproject.controller.devtools.HelpItem
import com.diyproject.controller.devtools.HelpSection

object MacroHelpContent {

    val sections: List<HelpSection> = listOf(
        HelpSection(
            title = "CONNECTION",
            items = listOf(
                HelpItem("CONNECT", "Opens the device picker and connects over the same link the D-pad controller, Code Control, and Terminal use."),
                HelpItem("EOL", "Line ending appended to every step's command before it's sent — must match what your sketch's readStringUntil()/readline() expects.")
            )
        ),
        HelpSection(
            title = "RUNNING A MACRO",
            items = listOf(
                HelpItem("RUN", "Sends each step's command in order, waiting that step's delay before the next one. Disabled while disconnected or while another macro is already running — only one can run at a time on a single link."),
                HelpItem("STOP", "Cancels playback immediately after the step currently in flight. Steps already sent stay sent — STOP doesn't undo anything, it just prevents the rest of the sequence from firing."),
                HelpItem("Progress banner", "Shows the running macro's name and \"step X / N\" while it plays."),
                HelpItem("EXECUTION LOG", "Every command actually transmitted during playback, in the exact bytes sent (line ending included) — the same TX/SYS log format as Terminal.")
            )
        ),
        HelpSection(
            title = "BUILDING A MACRO",
            items = listOf(
                HelpItem("+ NEW MACRO", "Opens the editor with an empty, unsaved macro — nothing is written until SAVE."),
                HelpItem("+ ADD STEP", "Appends a new step with an empty command and a 300ms default delay."),
                HelpItem("Command field", "The raw text sent for this step. Leave it blank to make this a pure wait — no command is sent, only the delay runs."),
                HelpItem("Delay (ms)", "How long to wait after this step before the next one starts. Clamped to 60,000ms (60s) per step to catch typos."),
                HelpItem("↑ / ↓", "Reorders a step. Order matters — this is a sequence, not a set."),
                HelpItem("✕", "Removes this step from the macro."),
                HelpItem("SAVE", "Writes the macro to the library. Disabled until it has at least one step."),
                HelpItem("CANCEL", "Discards changes made in this editing session and returns to the list."),
                HelpItem("DELETE MACRO", "Removes the macro from the library entirely. Only shown for macros already saved, and disabled while that macro is running.")
            )
        ),
        HelpSection(
            title = "MANAGING THE LIBRARY",
            items = listOf(
                HelpItem("EDIT", "Opens an existing macro in the editor."),
                HelpItem("DUPLICATE", "Creates an independent copy named \"<name> copy\" — editing the copy never affects the original."),
                HelpItem("DELETE", "Removes a macro from the list. Disabled while that macro is currently running.")
            )
        )
    )
}