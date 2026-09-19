package com.diyproject.controller.devtools.terminal

import com.diyproject.controller.devtools.HelpItem
import com.diyproject.controller.devtools.HelpSection

/**
 * Help copy for the Terminal screen, kept next to the screen it
 * documents so the two don't drift apart as controls change. Every
 * label here must match the on-screen control text exactly (CONNECT,
 * ALL/TX/RX/SYS, HEX, EXPORT, CLEAR, EOL, SEND) — that's what lets a
 * developer scan this list and match it 1:1 against what they're
 * looking at.
 */
object TerminalHelpContent {

    val sections: List<HelpSection> = listOf(
        HelpSection(
            title = "CONNECTION",
            items = listOf(
                HelpItem(
                    "CONNECT",
                    "Opens the Bluetooth device picker and connects over the same SPP link the D-pad controller and Code Control use. Turns green and reads CONNECTED once the socket is open."
                ),
                HelpItem(
                    "Device name",
                    "Shown under the title once connected — the paired device's Bluetooth name (or MAC address if it has none)."
                )
            )
        ),
        HelpSection(
            title = "READING THE LOG",
            items = listOf(
                HelpItem(
                    "TX (amber)",
                    "A line this app sent to the car — exactly what left the phone, including the line ending you chose in EOL."
                ),
                HelpItem(
                    "RX (green)",
                    "A line the car sent back — e.g. Serial.println(\"BATT:7.4\") on the Arduino side shows up here the moment it arrives."
                ),
                HelpItem(
                    "-- (grey)",
                    "A system notice, not device traffic — connected, disconnected, or an error message."
                ),
                HelpItem(
                    "⏎ symbol",
                    "Marks where a line ending (\\n, \\r, or \\r\\n) was in the raw data — those bytes are otherwise invisible."
                )
            )
        ),
        HelpSection(
            title = "FILTERING",
            items = listOf(
                HelpItem("ALL / TX / RX / SYS", "Shows only that direction — switch to RX alone, for example, to watch sensor output without your own commands mixed in."),
                HelpItem("HEX", "Switches non-printable bytes to \\xNN escapes — turn this on if you suspect the car is sending binary or malformed data instead of clean ASCII lines."),
                HelpItem("Search field", "Filters the visible log to lines containing that text — e.g. type BATT to watch only battery telemetry.")
            )
        ),
        HelpSection(
            title = "SENDING",
            items = listOf(
                HelpItem("Text field", "Type any raw command — same escape-hatch role as Code Control, useful here for provoking a specific RX response while you watch the log."),
                HelpItem("EOL", "Line ending appended before sending. Must match what your sketch's Serial.readStringUntil() or readline() expects, or the car may never see a complete command."),
                HelpItem("SEND", "Transmits the text field plus the chosen EOL immediately, and logs it as a TX line. Disabled while disconnected or empty."),
                HelpItem("Byte counter", "Live size of what SEND will transmit; turns red past 64 bytes, the default Arduino serial RX buffer size — a warning, not a hard block.")
            )
        ),
        HelpSection(
            title = "MANAGING THE LOG",
            items = listOf(
                HelpItem("↓ N new", "Appears when new lines arrive while you've scrolled up to read history — tap it to jump back to the live tail."),
                HelpItem("EXPORT", "Shares the full transcript as a timestamped text file via Android's share sheet — save it to a file, email it, or paste it into a bug report."),
                HelpItem("CLEAR", "Wipes the visible transcript and resets the TX/RX byte counters. Does not disconnect or affect the car."),
                HelpItem("Status bar", "Running total of lines logged and bytes sent/received this session.")
            )
        )
    )
}