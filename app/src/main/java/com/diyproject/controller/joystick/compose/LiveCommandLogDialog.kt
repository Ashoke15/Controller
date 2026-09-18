package com.diyproject.controller.joystick.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private enum class CommandTab { LIVE, REFERENCE }

/** One row of the static protocol reference: a control and exactly what it sends. */
private data class ProtocolEntry(
    val control: String,
    val trigger: String,
    val wireFormat: String,
    val note: String? = null
)

// Source of truth for this table: DriveCommand.kt, GimbalCommand.kt, RcTransmitter.kt,
// and CarCommand.* call sites in JoystickActivity.kt. CarCommand's own literal string
// values aren't visible from this package (CarCommand.kt hasn't been shared) — those
// rows show the symbolic constant name instead of a literal until that file is added.
private val PROTOCOL_REFERENCE = listOf(
    ProtocolEntry(
        control = "Left stick",
        trigger = "Drag (continuous)",
        wireFormat = "STR:<-100..100>\\n\nTHR:<-100..100>\\n",
        note = "Streamed by RcTransmitter at ~25Hz, only when a value changes, plus a forced resend of everything about once a second (heartbeat) in case a byte gets dropped."
    ),
    ProtocolEntry(
        control = "Left stick — motion control ON",
        trigger = "Device tilt (replaces stick X)",
        wireFormat = "STR:<-100..100>\\n",
        note = "Same STR token — steering source changes, wire format doesn't. THR still comes from the stick's Y."
    ),
    ProtocolEntry(
        control = "Right stick",
        trigger = "Drag (continuous)",
        wireFormat = "PAN:<0..180>\\n\nTLT:<0..180>\\n",
        note = "Same streaming behavior as the left stick, via the same RcTransmitter frame loop."
    ),
    ProtocolEntry(
        control = "HL — headlight",
        trigger = "Tap to toggle",
        wireFormat = "CarCommand.FRONT_LIGHTS_ON\nCarCommand.FRONT_LIGHTS_OFF",
        note = "One-shot on click, not repeated. Literal value: see CarCommand.kt."
    ),
    ProtocolEntry(
        control = "CL — cabin light",
        trigger = "Tap to toggle",
        wireFormat = "CarCommand.BACK_LIGHTS_ON\nCarCommand.BACK_LIGHTS_OFF",
        note = "One-shot on click. Literal value: see CarCommand.kt."
    ),
    ProtocolEntry(
        control = "HN — horn",
        trigger = "Tap to toggle",
        wireFormat = "CarCommand.HORN_ON\nCarCommand.HORN_OFF",
        note = "One-shot on click. Literal value: see CarCommand.kt."
    ),
    ProtocolEntry(
        control = "WN — warning",
        trigger = "Tap to toggle",
        wireFormat = "CarCommand.EXTRA_ON\nCarCommand.EXTRA_OFF",
        note = "One-shot on click. Literal value: see CarCommand.kt."
    ),
    ProtocolEntry(
        control = "MO — motion control",
        trigger = "Tap to toggle",
        wireFormat = "(no token sent)",
        note = "Local-only switch: it changes whether STR comes from the left stick or the gyro. Nothing goes over Bluetooth when you tap it."
    ),
    ProtocolEntry(
        control = "Throttle limit slider",
        trigger = "Drag",
        wireFormat = "(no token sent)",
        note = "Local-only scale factor (20–100%) applied to THR before it's sent. The car firmware never sees the percentage, only the already-scaled THR value."
    ),
    ProtocolEntry(
        control = "STOP",
        trigger = "Tap",
        wireFormat = "CarCommand.STOP_ALL\nSTR:0\\n\nTHR:0\\n\nPAN:90\\n\nTLT:90\\n",
        note = "The 4 channel tokens are sent immediately, bypassing the ~40ms frame timer — this is the one path that doesn't wait for the next tick."
    ),
    ProtocolEntry(
        control = "CONNECT",
        trigger = "Tap",
        wireFormat = "(no token sent)",
        note = "Opens the Bluetooth SPP connection to a paired device. Right after connecting, every channel is force-resent once (steering/throttle/pan/tilt), even if unchanged, so the car's state matches the app's."
    )
)

@Composable
fun CommandCenterDialog(
    entries: List<String>,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableStateOf(CommandTab.LIVE) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                // REMOVED .glassPanel() AND ADDED A SOLID BACKGROUND:
                .clip(RoundedCornerShape(18.dp))
                .background(ControllerTheme.backgroundTop)
                .border(1.dp, ControllerTheme.panelBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TabChip("LIVE", tab == CommandTab.LIVE) { tab = CommandTab.LIVE }
                    TabChip("REFERENCE", tab == CommandTab.REFERENCE) { tab = CommandTab.REFERENCE }
                }
                Text(
                    text = "CLOSE",
                    color = ControllerTheme.cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Text(
                text = if (tab == CommandTab.LIVE)
                    "Actual bytes sent to the car, newest on top."
                else
                    "Fixed wire protocol — what firmware should expect from each control, whether or not it's been pressed yet.",
                color = ControllerTheme.textFaint,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
            )

            when (tab) {
                CommandTab.LIVE -> LiveLogList(entries)
                CommandTab.REFERENCE -> ReferenceList()
            }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) ControllerTheme.cyan.copy(alpha = 0.18f) else ControllerTheme.panelFill
    val fg = if (selected) ControllerTheme.cyan else ControllerTheme.textMuted
    Text(
        text = label,
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun LiveLogList(entries: List<String>) {
    if (entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
            Text("Nothing sent yet — move a stick or connect first.", color = ControllerTheme.textMuted, fontSize = 11.sp)
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(entries) { line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ControllerTheme.panelFill)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(line, color = ControllerTheme.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun ReferenceList() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(PROTOCOL_REFERENCE) { entry -> ReferenceRow(entry) }
    }
}

@Composable
private fun ReferenceRow(entry: ProtocolEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ControllerTheme.panelFill)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(entry.control, color = ControllerTheme.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(entry.trigger, color = ControllerTheme.textFaint, fontSize = 10.sp)
        }
        Text(
            text = entry.wireFormat,
            color = ControllerTheme.cyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp)
        )
        entry.note?.let {
            Text(it, color = ControllerTheme.textMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}