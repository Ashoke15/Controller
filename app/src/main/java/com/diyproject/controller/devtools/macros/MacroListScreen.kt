package com.diyproject.controller.devtools.macros

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.diyproject.controller.devtools.DevToolsTheme
import com.diyproject.controller.devtools.HelpDialog
import com.diyproject.controller.devtools.common.LineEnding
import com.diyproject.controller.devtools.common.TranscriptDirection
import com.diyproject.controller.devtools.common.TranscriptEntry
import com.diyproject.controller.devtools.consolePanel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MacroListScreen(
    isConnected: Boolean,
    macros: List<Macro>,
    runningMacroId: String?,
    runningStepIndex: Int?,
    runningTotalSteps: Int?,
    log: List<TranscriptEntry>,
    lineEnding: LineEnding,
    onLineEndingChange: (LineEnding) -> Unit,
    onRun: (Macro) -> Unit,
    onStop: () -> Unit,
    onNew: () -> Unit,
    onEdit: (Macro) -> Unit,
    onDuplicate: (Macro) -> Unit,
    onDelete: (Macro) -> Unit,
    onConnectClick: () -> Unit,
    onClearLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHelp by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Macro?>(null) }

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
                onHelp = { showHelp = true },
                onNew = onNew
            )

            val runningMacro = macros.find { it.id == runningMacroId }
            if (runningMacro != null) {
                RunningBanner(
                    macro = runningMacro,
                    stepIndex = runningStepIndex,
                    totalSteps = runningTotalSteps,
                    onStop = onStop
                )
            }

            if (macros.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f), onNew = onNew)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(macros, key = { it.id }) { macro ->
                        MacroCard(
                            macro = macro,
                            isRunning = macro.id == runningMacroId,
                            anyMacroRunning = runningMacroId != null,
                            canOperate = isConnected,
                            onRun = { onRun(macro) },
                            onStop = onStop,
                            onEdit = { onEdit(macro) },
                            onDuplicate = { onDuplicate(macro) },
                            onDelete = { pendingDelete = macro }
                        )
                    }
                }
            }

            LineEndingRow(selected = lineEnding, onSelect = onLineEndingChange)

            ExecutionLogPanel(
                entries = log,
                onClear = onClearLog,
                modifier = Modifier.height(140.dp)
            )
        }

        if (showHelp) {
            HelpDialog(
                screenTitle = "MACROS",
                sections = MacroHelpContent.sections,
                onDismiss = { showHelp = false }
            )
        }

        pendingDelete?.let { macro ->
            DeleteConfirmDialog(
                macroName = macro.name,
                onConfirm = { onDelete(macro); pendingDelete = null },
                onDismiss = { pendingDelete = null }
            )
        }
    }
}

@Composable
private fun HeaderRow(
    isConnected: Boolean,
    onConnectClick: () -> Unit,
    onHelp: () -> Unit,
    onNew: () -> Unit
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
            Text("MACROS", color = DevToolsTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Text("command sequences", color = DevToolsTheme.textFaint, fontSize = 10.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextChip("HELP", onHelp)
            TextChip("+ NEW MACRO", onNew)
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
        Text(if (isConnected) "CONNECTED" else "CONNECT", color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
    }
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
private fun RunningBanner(macro: Macro, stepIndex: Int?, totalSteps: Int?, onStop: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DevToolsTheme.accent.copy(alpha = 0.12f))
            .border(1.dp, DevToolsTheme.accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("RUNNING · ${macro.name}", color = DevToolsTheme.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            val progress = if (stepIndex != null && totalSteps != null) "step ${stepIndex + 1} / $totalSteps" else "starting…"
            Text(progress, color = DevToolsTheme.textMuted, fontSize = 10.sp)
        }
        Text(
            text = "STOP", color = DevToolsTheme.errorColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DevToolsTheme.errorColor.copy(alpha = 0.16f))
                .border(1.dp, DevToolsTheme.errorColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .clickable(onClick = onStop)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onNew: () -> Unit) {
    Box(modifier = modifier.fillMaxWidth().consolePanel(corner = 10.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No macros yet", color = DevToolsTheme.textMuted, fontSize = 12.sp)
            Text(
                text = "+ NEW MACRO",
                color = DevToolsTheme.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onNew)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun MacroCard(
    macro: Macro,
    isRunning: Boolean,
    anyMacroRunning: Boolean,
    canOperate: Boolean,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val preview = macro.steps.joinToString(" → ") { it.command.ifBlank { "(wait)" } }
    val structuralActionsEnabled = !isRunning

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .consolePanel(corner = 10.dp)
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(macro.name, color = DevToolsTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("${macro.steps.size} step${if (macro.steps.size == 1) "" else "s"}", color = DevToolsTheme.textFaint, fontSize = 10.sp)
            }
            RunButton(
                isRunning = isRunning,
                enabled = if (isRunning) true else canOperate && !anyMacroRunning,
                onRun = onRun,
                onStop = onStop
            )
        }

        if (preview.isNotEmpty()) {
            Text(
                text = preview, color = DevToolsTheme.textMuted, fontSize = 10.sp, fontFamily = DevToolsTheme.mono,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallChip("EDIT", enabled = structuralActionsEnabled, onClick = onEdit)
            SmallChip("DUPLICATE", enabled = structuralActionsEnabled, onClick = onDuplicate)
            SmallChip("DELETE", enabled = structuralActionsEnabled, onClick = onDelete, danger = true)
        }
    }
}

@Composable
private fun RunButton(isRunning: Boolean, enabled: Boolean, onRun: () -> Unit, onStop: () -> Unit) {
    if (isRunning) {
        Text(
            "STOP", color = DevToolsTheme.errorColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DevToolsTheme.errorColor.copy(alpha = 0.16f))
                .border(1.dp, DevToolsTheme.errorColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .clickable(onClick = onStop)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    } else {
        val bg = if (enabled) DevToolsTheme.accent else DevToolsTheme.panelFillStrong
        val fg = if (enabled) DevToolsTheme.backgroundDeep else DevToolsTheme.textFaint
        Text(
            "RUN", color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .then(if (enabled) Modifier.clickable(onClick = onRun) else Modifier)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SmallChip(label: String, enabled: Boolean, onClick: () -> Unit, danger: Boolean = false) {
    val fg = when {
        !enabled -> DevToolsTheme.textFaint
        danger -> DevToolsTheme.errorColor
        else -> DevToolsTheme.textMuted
    }
    Text(
        text = label, color = fg, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(DevToolsTheme.panelFill)
            .border(1.dp, DevToolsTheme.panelBorder, RoundedCornerShape(4.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

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

private val logTimeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@Composable
private fun ExecutionLogPanel(entries: List<TranscriptEntry>, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().consolePanel(corner = 10.dp).padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("EXECUTION LOG", color = DevToolsTheme.textFaint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            Text(
                "CLEAR", color = DevToolsTheme.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onClear).padding(horizontal = 6.dp)
            )
        }
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No macro traffic yet.", color = DevToolsTheme.textFaint, fontSize = 10.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(entries, key = { it.id }) { entry ->
                    val (tag, color) = when (entry.direction) {
                        TranscriptDirection.TX -> "TX" to DevToolsTheme.txColor
                        TranscriptDirection.RX -> "RX" to DevToolsTheme.rxColor
                        TranscriptDirection.SYSTEM -> "--" to DevToolsTheme.systemColor
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(logTimeFormatter.format(entry.timestampMs), color = DevToolsTheme.textFaint, fontSize = 9.sp, fontFamily = DevToolsTheme.mono, modifier = Modifier.width(66.dp))
                        Text(tag, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = DevToolsTheme.mono, modifier = Modifier.width(20.dp))
                        Text(entry.raw.replace("\n", "⏎").replace("\r", "⏎"), color = DevToolsTheme.textPrimary, fontSize = 10.sp, fontFamily = DevToolsTheme.mono)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(macroName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().consolePanel(corner = 14.dp).padding(16.dp)) {
            Text("Delete \"$macroName\"?", color = DevToolsTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("This can't be undone.", color = DevToolsTheme.textMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "CANCEL", color = DevToolsTheme.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Text(
                    "DELETE", color = DevToolsTheme.errorColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onConfirm).padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
