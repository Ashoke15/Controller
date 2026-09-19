package com.diyproject.controller.devtools.codecontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diyproject.controller.devtools.DevToolsTheme
import com.diyproject.controller.devtools.consolePanel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.OptIn
import com.diyproject.controller.devtools.common.LineEnding
import com.diyproject.controller.devtools.common.RawCommand
import com.diyproject.controller.devtools.common.TranscriptDirection
import com.diyproject.controller.devtools.common.TranscriptEntry
import com.diyproject.controller.devtools.HelpDialog

/**
 * Manual raw-command console — the "type anything, send it exactly as
 * typed" escape hatch for hardware without a proper UI control yet.
 * Pure UI: every callback here is real I/O performed by
 * [CodeControlActivity] — this file has no Bluetooth or persistence
 * logic of its own, same separation JoystickScreen keeps from
 * JoystickActivity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeControlScreen(
    isConnected: Boolean,
    transcript: List<TranscriptEntry>,
    history: List<String>,
    lineEnding: LineEnding,
    onLineEndingChange: (LineEnding) -> Unit,
    onSend: (String) -> Unit,
    onClearTranscript: () -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showHelp by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DevToolsTheme.backgroundBrush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderRow(
                isConnected = isConnected,
                onConnectClick = onConnectClick,
                onClearTranscript = onClearTranscript,
                onHelp = { showHelp = true }
            )

            TranscriptPanel(
                transcript = transcript,
                modifier = Modifier.weight(1f)
            )

            if (history.isNotEmpty()) {
                HistoryRow(history = history, onPick = { inputText = it })
            }

            LineEndingRow(selected = lineEnding, onSelect = onLineEndingChange)

            InputRow(
                text = inputText,
                onTextChange = { inputText = it },
                byteLength = RawCommand.byteLength(RawCommand.withLineEnding(inputText, lineEnding)),
                canSend = isConnected && inputText.isNotEmpty(),
                onSend = {
                    if (inputText.isNotEmpty()) {
                        onSend(inputText)
                        inputText = ""
                    }
                }
            )
        }
    }
    if (showHelp) {
        HelpDialog(
            screenTitle = "CODE CONTROL",
            sections = CodeControlHelpContent.sections,
            onDismiss = { showHelp = false }
        )
    }
}

@Composable
private fun HeaderRow(
    isConnected: Boolean,
    onConnectClick: () -> Unit,
    onClearTranscript: () -> Unit,
    onHelp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .consolePanel(corner = 10.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CODE CONTROL",
                color = DevToolsTheme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Text(text = "raw serial", color = DevToolsTheme.textFaint, fontSize = 10.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextChip(label = "HELP", onClick = onHelp)
            TextChip(label = "CLEAR", onClick = onClearTranscript)
            ConnectionPill(isConnected = isConnected, onClick = onConnectClick)
        }
    }
}

@Composable
private fun ConnectionPill(isConnected: Boolean, onClick: () -> Unit) {
    val bg = if (isConnected) DevToolsTheme.rxColor.copy(alpha = 0.16f) else DevToolsTheme.panelFillStrong
    val fg = if (isConnected) DevToolsTheme.rxColor else DevToolsTheme.textMuted
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(fg))
        Text(
            text = if (isConnected) "CONNECTED" else "CONNECT",
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
private fun TextChip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = DevToolsTheme.textMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(DevToolsTheme.panelFillStrong)
            .border(1.dp, DevToolsTheme.panelBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@Composable
private fun TranscriptPanel(transcript: List<TranscriptEntry>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to the newest line — a serial monitor that doesn't
    // track the tail isn't useful while driving.
    LaunchedEffect(transcript.size) {
        if (transcript.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(transcript.lastIndex) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .consolePanel(corner = 10.dp)
            .padding(8.dp)
    ) {
        if (transcript.isEmpty()) {
            Text(
                text = "No traffic yet. Connect and send a command, or wait for the device to speak first.",
                color = DevToolsTheme.textFaint,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
        } else {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(transcript, key = { it.id }) { entry -> TranscriptLine(entry) }
            }
        }
    }
}

@Composable
private fun TranscriptLine(entry: TranscriptEntry) {
    val (tag, color) = when (entry.direction) {
        TranscriptDirection.TX -> "TX" to DevToolsTheme.txColor
        TranscriptDirection.RX -> "RX" to DevToolsTheme.rxColor
        TranscriptDirection.SYSTEM -> "--" to DevToolsTheme.systemColor
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = timeFormatter.format(entry.timestampMs),
            color = DevToolsTheme.textFaint,
            fontSize = 10.sp,
            fontFamily = DevToolsTheme.mono,
            modifier = Modifier.width(78.dp)
        )
        Text(
            text = tag,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = DevToolsTheme.mono,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = formatForDisplay(entry.raw),
            color = if (entry.direction == TranscriptDirection.SYSTEM) DevToolsTheme.systemColor else DevToolsTheme.textPrimary,
            fontSize = 11.sp,
            fontFamily = DevToolsTheme.mono
        )
    }
}

/** Makes otherwise-invisible line-ending bytes visible — exactly what got
 *  sent/received, terminator included, is the whole point of this screen. */
private fun formatForDisplay(raw: String): String =
    raw.replace("\r\n", "⏎").replace("\n", "⏎").replace("\r", "⏎")

@Composable
private fun HistoryRow(history: List<String>, onPick: (String) -> Unit) {
    Column {
        Text(
            text = "RECENT",
            color = DevToolsTheme.textFaint,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(history) { cmd ->
                Text(
                    text = cmd,
                    color = DevToolsTheme.textMuted,
                    fontSize = 10.sp,
                    fontFamily = DevToolsTheme.mono,
                    maxLines = 1,
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(50))
                        .background(DevToolsTheme.panelFill)
                        .border(1.dp, DevToolsTheme.panelBorder, RoundedCornerShape(50))
                        .clickable { onPick(cmd) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun LineEndingRow(selected: LineEnding, onSelect: (LineEnding) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "EOL",
            color = DevToolsTheme.textFaint,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 2.dp)
        )
        LineEnding.entries.forEach { option ->
            val active = option == selected
            Text(
                text = option.label,
                color = if (active) DevToolsTheme.accent else DevToolsTheme.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DevToolsTheme.mono,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (active) DevToolsTheme.accent.copy(alpha = 0.16f) else DevToolsTheme.panelFill)
                    .border(1.dp, if (active) DevToolsTheme.accent.copy(alpha = 0.5f) else DevToolsTheme.panelBorder, RoundedCornerShape(4.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun InputRow(
    text: String,
    onTextChange: (String) -> Unit,
    byteLength: Int,
    canSend: Boolean,
    onSend: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .consolePanel(corner = 8.dp),
                placeholder = { Text("LASER_ON, BEEP_3, ...", color = DevToolsTheme.textFaint, fontSize = 12.sp) },
                textStyle = TextStyle(
                    color = DevToolsTheme.textPrimary,
                    fontFamily = DevToolsTheme.mono,
                    fontSize = 13.sp
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = DevToolsTheme.accent
                )
            )

            SendButton(enabled = canSend, onClick = onSend)
        }

        val overBuffer = byteLength > RawCommand.ARDUINO_DEFAULT_SERIAL_BUFFER_BYTES
        Text(
            text = if (overBuffer)
                "$byteLength bytes — exceeds the default 64-byte Arduino serial buffer"
            else
                "$byteLength bytes",
            color = if (overBuffer) DevToolsTheme.errorColor else DevToolsTheme.textFaint,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 3.dp, start = 2.dp)
        )
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) DevToolsTheme.accent else DevToolsTheme.panelFillStrong
    val fg = if (enabled) Color(0xFF04211FL) else DevToolsTheme.textFaint
    Text(
        text = "SEND",
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    )
}
