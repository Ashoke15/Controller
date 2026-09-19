package com.diyproject.controller.devtools.terminal

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
import androidx.compose.foundation.lazy.LazyColumn
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
import com.diyproject.controller.devtools.common.LineEnding
import com.diyproject.controller.devtools.common.RawCommand
import com.diyproject.controller.devtools.common.TranscriptDirection
import com.diyproject.controller.devtools.common.TranscriptEntry
import com.diyproject.controller.devtools.HelpDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Mobile equivalent of the Arduino IDE Serial Monitor. Unlike Code
 * Control, the point here is reading a live two-way stream, not
 * composing one command at a time — so this adds what a real serial
 * monitor needs on top of the shared TX/RX log: direction filtering,
 * text search, sticky-to-latest autoscroll with a "new lines" catch-up
 * badge, byte/line counters, and a hex view for non-printable bytes
 * (sensor telemetry, binary framing, etc). Pure UI — [TerminalActivity]
 * owns the socket and is the only source of truth for [transcript].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    isConnected: Boolean,
    connectedDeviceLabel: String?,
    transcript: List<TranscriptEntry>,
    txByteCount: Long,
    rxByteCount: Long,
    lineEnding: LineEnding,
    onLineEndingChange: (LineEnding) -> Unit,
    onSend: (String) -> Unit,
    onClearTranscript: () -> Unit,
    onExportTranscript: () -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(TerminalFilter.ALL) }
    var showHelp by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var hexMode by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    val filtered = remember(transcript, filter, searchQuery) {
        transcript.filter { matchesFilter(it, filter) && matchesSearch(it, searchQuery) }
    }

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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HeaderRow(
                isConnected = isConnected,
                connectedDeviceLabel = connectedDeviceLabel,
                onConnectClick = onConnectClick
            )

            ToolRow(
                filter = filter,
                onFilterChange = { filter = it },
                hexMode = hexMode,
                onHexModeChange = { hexMode = it },
                onClear = onClearTranscript,
                onExport = onExportTranscript,
                onHelp = { showHelp = true }
            )

            SearchField(query = searchQuery, onQueryChange = { searchQuery = it })

            TranscriptPanel(
                entries = filtered,
                hexMode = hexMode,
                modifier = Modifier.weight(1f)
            )

            StatusBar(lineCount = transcript.size, txBytes = txByteCount, rxBytes = rxByteCount)

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
            screenTitle = "TERMINAL",
            sections = TerminalHelpContent.sections,
            onDismiss = { showHelp = false }
        )
    }
}

private fun matchesFilter(entry: TranscriptEntry, filter: TerminalFilter): Boolean = when (filter) {
    TerminalFilter.ALL -> true
    TerminalFilter.TX -> entry.direction == TranscriptDirection.TX
    TerminalFilter.RX -> entry.direction == TranscriptDirection.RX
    TerminalFilter.SYSTEM -> entry.direction == TranscriptDirection.SYSTEM
}

private fun matchesSearch(entry: TranscriptEntry, query: String): Boolean =
    query.isBlank() || entry.raw.contains(query, ignoreCase = true)

@Composable
private fun HeaderRow(
    isConnected: Boolean,
    connectedDeviceLabel: String?,
    onConnectClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .consolePanel(corner = 10.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TERMINAL", color = DevToolsTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Text("serial monitor", color = DevToolsTheme.textFaint, fontSize = 10.sp)
            }
            if (isConnected && connectedDeviceLabel != null) {
                Text(connectedDeviceLabel, color = DevToolsTheme.rxColor, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }

        ConnectionPill(isConnected = isConnected, onClick = onConnectClick)
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
            color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp
        )
    }
}

@Composable
private fun ToolRow(
    filter: TerminalFilter,
    onFilterChange: (TerminalFilter) -> Unit,
    hexMode: Boolean,
    onHexModeChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    onExport: () -> Unit,
    onHelp: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TerminalFilter.values().forEach { option ->
                FilterChip(label = option.label, active = option == filter) { onFilterChange(option) }
            }
            FilterChip(label = "HEX", active = hexMode) { onHexModeChange(!hexMode) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextChip("HELP", onHelp)
            TextChip("EXPORT", onExport)
            TextChip("CLEAR", onClear)
        }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) DevToolsTheme.accent.copy(alpha = 0.16f) else DevToolsTheme.panelFill
    val fg = if (active) DevToolsTheme.accent else DevToolsTheme.textMuted
    Text(
        text = label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, if (active) DevToolsTheme.accent.copy(alpha = 0.5f) else DevToolsTheme.panelBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

@Composable
private fun TextChip(label: String, onClick: () -> Unit) {
    Text(
        text = label, color = DevToolsTheme.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(DevToolsTheme.panelFillStrong)
            .border(1.dp, DevToolsTheme.panelBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().consolePanel(corner = 8.dp),
        placeholder = { Text("Filter transcript text…", color = DevToolsTheme.textFaint, fontSize = 11.sp) },
        textStyle = TextStyle(color = DevToolsTheme.textPrimary, fontFamily = DevToolsTheme.mono, fontSize = 12.sp),
        singleLine = true,
        trailingIcon = {
            if (query.isNotEmpty()) {
                Text(
                    "✕", color = DevToolsTheme.textMuted, fontSize = 12.sp,
                    modifier = Modifier.clickable { onQueryChange("") }.padding(horizontal = 10.dp)
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = DevToolsTheme.accent
        )
    )
}

private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@Composable
private fun TranscriptPanel(entries: List<TranscriptEntry>, hexMode: Boolean, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var pendingNewCount by remember { mutableStateOf(0) }

    // Sticks to the bottom only if the viewer was already at (or very
    // near) the bottom before this update — checked against the *previous*
    // frame's layout, since a brand-new item hasn't been measured yet this
    // frame. If they'd scrolled up to read history, new lines accumulate
    // silently and surface as the catch-up badge instead of yanking their
    // scroll position.
    LaunchedEffect(entries.size, entries.lastOrNull()?.id) {
        val lastIndex = entries.lastIndex
        if (lastIndex < 0) { pendingNewCount = 0; return@LaunchedEffect }
        val lastVisibleBefore = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val wasNearBottom = lastVisibleBefore >= lastIndex - 2
        if (wasNearBottom) {
            listState.animateScrollToItem(lastIndex)
            pendingNewCount = 0
        } else {
            pendingNewCount++
        }
    }

    // Reset framing whenever the visible set changes shape (filter/search) —
    // stale indices from before shouldn't drive the catch-up badge.
    LaunchedEffect(entries) {
        pendingNewCount = 0
    }

    Box(modifier = modifier.fillMaxWidth().consolePanel(corner = 10.dp).padding(8.dp)) {
        if (entries.isEmpty()) {
            Text(
                text = "No traffic matches the current filter.",
                color = DevToolsTheme.textFaint, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
        } else {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(entries, key = { it.id }) { entry -> TranscriptLine(entry, hexMode) }
            }
        }

        if (pendingNewCount > 0) {
            Text(
                text = "↓ $pendingNewCount new",
                color = Color(0xFF04211FL),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(50))
                    .background(DevToolsTheme.accent)
                    .clickable {
                        scope.launch {
                            listState.animateScrollToItem(entries.lastIndex)
                            pendingNewCount = 0
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun TranscriptLine(entry: TranscriptEntry, hexMode: Boolean) {
    val (tag, color) = when (entry.direction) {
        TranscriptDirection.TX -> "TX" to DevToolsTheme.txColor
        TranscriptDirection.RX -> "RX" to DevToolsTheme.rxColor
        TranscriptDirection.SYSTEM -> "--" to DevToolsTheme.systemColor
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = timeFormatter.format(entry.timestampMs), color = DevToolsTheme.textFaint,
            fontSize = 10.sp, fontFamily = DevToolsTheme.mono, modifier = Modifier.width(78.dp)
        )
        Text(
            text = tag, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            fontFamily = DevToolsTheme.mono, modifier = Modifier.width(24.dp)
        )
        Text(
            text = formatForDisplay(entry.raw, hexMode),
            color = if (entry.direction == TranscriptDirection.SYSTEM) DevToolsTheme.systemColor else DevToolsTheme.textPrimary,
            fontSize = 11.sp, fontFamily = DevToolsTheme.mono
        )
    }
}

private fun formatForDisplay(raw: String, hexMode: Boolean): String {
    if (hexMode) {
        val sb = StringBuilder()
        for (c in raw) {
            val code = c.code
            if (code in 32..126) sb.append(c) else sb.append("\\x%02X".format(code))
        }
        return sb.toString()
    }
    return raw.replace("\r\n", "⏎").replace("\n", "⏎").replace("\r", "⏎")
}

@Composable
private fun StatusBar(lineCount: Int, txBytes: Long, rxBytes: Long) {
    Text(
        text = "$lineCount lines  ·  TX ${formatBytes(txBytes)}  ·  RX ${formatBytes(rxBytes)}",
        color = DevToolsTheme.textFaint, fontSize = 9.sp, modifier = Modifier.padding(start = 2.dp)
    )
}

private fun formatBytes(bytes: Long): String =
    if (bytes < 1024) "$bytes B" else "%.1f KB".format(bytes / 1024.0)

@Composable
private fun LineEndingRow(selected: LineEnding, onSelect: (LineEnding) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("EOL", color = DevToolsTheme.textFaint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 2.dp))
        LineEnding.values().forEach { option ->
            val active = option == selected
            Text(
                text = option.label,
                color = if (active) DevToolsTheme.accent else DevToolsTheme.textMuted,
                fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = DevToolsTheme.mono,
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
                value = text, onValueChange = onTextChange,
                modifier = Modifier.weight(1f).consolePanel(corner = 8.dp),
                placeholder = { Text("Send a test command…", color = DevToolsTheme.textFaint, fontSize = 12.sp) },
                textStyle = TextStyle(color = DevToolsTheme.textPrimary, fontFamily = DevToolsTheme.mono, fontSize = 13.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Send),
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
            text = if (overBuffer) "$byteLength bytes — exceeds the default 64-byte Arduino serial buffer" else "$byteLength bytes",
            color = if (overBuffer) DevToolsTheme.errorColor else DevToolsTheme.textFaint,
            fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp, start = 2.dp)
        )
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) DevToolsTheme.accent else DevToolsTheme.panelFillStrong
    val fg = if (enabled) Color(0xFF04211FL) else DevToolsTheme.textFaint
    Text(
        text = "SEND", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    )
}