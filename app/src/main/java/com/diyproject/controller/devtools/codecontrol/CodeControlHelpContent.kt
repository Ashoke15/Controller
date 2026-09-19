package com.diyproject.controller.devtools.codecontrol

import com.diyproject.controller.devtools.HelpItem
import com.diyproject.controller.devtools.HelpSection

/**
 * Help copy for the Code Control screen, kept next to the screen it
 * documents. Labels here must match the on-screen control text exactly
 * (CONNECT, CLEAR, RECENT, EOL, SEND) so a developer can scan this list
 * and match it 1:1 against what they're looking at.
 */
object CodeControlHelpContent {

    val sections: List<HelpSection> = listOf(
        HelpSection(
            title = "CONNECTION",
            items = listOf(
                HelpItem(
                    "CONNECT",
                    "Opens the Bluetooth device picker and connects over the same SPP link the D-pad controller, Terminal, and Macros use. Turns green and reads CONNECTED once the socket is open."
                )
            )
        ),
        HelpSection(
            title = "WHAT THIS SCREEN IS FOR",
            items = listOf(
                HelpItem(
                    "Purpose",
                    "A manual testing ground for hardware that doesn't have a UI button yet — just wired a laser or buzzer? Type its command here and send it, without building a screen control for it first."
                ),
                HelpItem(
                    "TX / RX lines",
                    "TX (amber) is exactly what this app sent, including the line ending you chose. RX (green) is a line the device sent back — e.g. an Arduino Serial.println() reply."
                ),
                HelpItem(
                    "⏎ symbol",
                    "Marks where a line ending (\\n, \\r, or \\r\\n) sits in the raw data — those bytes are otherwise invisible in the log."
                )
            )
        ),
        HelpSection(
            title = "SENDING",
            items = listOf(
                HelpItem("Text field", "Type any raw command exactly as your firmware expects it — nothing is reshaped or validated beyond the line ending."),
                HelpItem("EOL", "Line ending appended before sending. Must match what your sketch's Serial.readStringUntil() or readline() expects, or the device may never see a complete command."),
                HelpItem("SEND", "Transmits the text field plus the chosen EOL immediately, and logs it as a TX line. Disabled while disconnected or the field is empty."),
                HelpItem("Byte counter", "Live size of what SEND will transmit; turns red past 64 bytes, the default Arduino serial RX buffer size — a warning, not a hard block."),
                HelpItem("RECENT", "Your last commands, newest first — tap one to load it back into the text field instead of retyping. Saved on-device and remembered across app restarts.")
            )
        ),
        HelpSection(
            title = "MANAGING THE LOG",
            items = listOf(
                HelpItem("CLEAR", "Wipes the visible transcript. Does not disconnect or affect the device, and doesn't touch your RECENT command history.")
            )
        )
    )
}