package com.diyproject.controller.devtools.macros

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diyproject.controller.devtools.DevToolsTheme
import com.diyproject.controller.devtools.common.RawCommand
import com.diyproject.controller.devtools.consolePanel
import androidx.compose.foundation.lazy.itemsIndexed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroEditorScreen(
    draft: Macro,
    isNewMacro: Boolean,
    onNameChange: (String) -> Unit,
    onAddStep: () -> Unit,
    onUpdateStepCommand: (stepId: Long, command: String) -> Unit,
    onUpdateStepDelay: (stepId: Long, delayMs: Long) -> Unit,
    onRemoveStep: (stepId: Long) -> Unit,
    onMoveStep: (stepId: Long, delta: Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDeleteMacro: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DevToolsTheme.backgroundBrush())
            .safeDrawingPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isNewMacro) "NEW MACRO" else "EDIT MACRO",
            color = DevToolsTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp
        )

        NameField(value = draft.name, onChange = onNameChange)

        Text("STEPS", color = DevToolsTheme.textFaint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(draft.steps) { index, step ->
                StepRow(
                    index = index,
                    step = step,
                    isFirst = index == 0,
                    isLast = index == draft.steps.lastIndex,
                    onCommandChange = { onUpdateStepCommand(step.id, it) },
                    onDelayChange = { onUpdateStepDelay(step.id, it) },
                    onRemove = { onRemoveStep(step.id) },
                    onMoveUp = { onMoveStep(step.id, -1) },
                    onMoveDown = { onMoveStep(step.id, 1) }
                )
            }
            item {
                Text(
                    text = "+ ADD STEP",
                    color = DevToolsTheme.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DevToolsTheme.accent.copy(alpha = 0.10f))
                        .clickable(onClick = onAddStep)
                        .padding(vertical = 12.dp)
                )
            }
        }

        if (draft.steps.isEmpty()) {
            Text("Add at least one step before saving.", color = DevToolsTheme.textFaint, fontSize = 10.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton("CANCEL", modifier = Modifier.weight(1f), onClick = onCancel)
            ActionButton(
                "SAVE", modifier = Modifier.weight(1f), enabled = draft.steps.isNotEmpty(),
                primary = true, onClick = onSave
            )
        }

        if (!isNewMacro) {
            Text(
                "DELETE MACRO", color = DevToolsTheme.errorColor, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDeleteMacro)
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun NameField(value: String, onChange: (String) -> Unit) {
    TextField(
        value = value, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().consolePanel(corner = 8.dp),
        placeholder = { Text("Macro name", color = DevToolsTheme.textFaint, fontSize = 13.sp) },
        textStyle = TextStyle(color = DevToolsTheme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        singleLine = true,
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

@Composable
private fun StepRow(
    index: Int,
    step: MacroStep,
    isFirst: Boolean,
    isLast: Boolean,
    onCommandChange: (String) -> Unit,
    onDelayChange: (Long) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().consolePanel(corner = 10.dp).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${index + 1}", color = DevToolsTheme.textFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))

            TextField(
                value = step.command, onValueChange = onCommandChange,
                modifier = Modifier.weight(1f).consolePanel(corner = 6.dp),
                placeholder = { Text("command (blank = wait)", color = DevToolsTheme.textFaint, fontSize = 11.sp) },
                textStyle = TextStyle(color = DevToolsTheme.textPrimary, fontFamily = DevToolsTheme.mono, fontSize = 12.sp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = DevToolsTheme.accent
                )
            )

            TextField(
                value = step.delayAfterMs.toString(),
                onValueChange = { text ->
                    val parsed = text.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    onDelayChange(parsed.coerceIn(0, MacroDefaults.MAX_STEP_DELAY_MS))
                },
                modifier = Modifier.width(76.dp).consolePanel(corner = 6.dp),
                textStyle = TextStyle(color = DevToolsTheme.textPrimary, fontFamily = DevToolsTheme.mono, fontSize = 12.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = DevToolsTheme.accent
                )
            )
            Text("ms", color = DevToolsTheme.textFaint, fontSize = 10.sp)
        }

        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniButton("↑", enabled = !isFirst, onClick = onMoveUp)
            MiniButton("↓", enabled = !isLast, onClick = onMoveDown)
            MiniButton("✕", enabled = true, danger = true, onClick = onRemove)

            val byteLength = RawCommand.byteLength(step.command)
            if (byteLength > RawCommand.ARDUINO_DEFAULT_SERIAL_BUFFER_BYTES) {
                Text(
                    "$byteLength bytes — exceeds 64-byte buffer", color = DevToolsTheme.errorColor, fontSize = 9.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniButton(label: String, enabled: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    val fg = when {
        !enabled -> DevToolsTheme.textFaint
        danger -> DevToolsTheme.errorColor
        else -> DevToolsTheme.textMuted
    }
    Text(
        text = label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(DevToolsTheme.panelFill)
            .border(1.dp, DevToolsTheme.panelBorder, RoundedCornerShape(4.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun ActionButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, primary: Boolean = false, onClick: () -> Unit) {
    val bg = when {
        !enabled -> DevToolsTheme.panelFillStrong
        primary -> DevToolsTheme.accent
        else -> DevToolsTheme.panelFillStrong
    }
    val fg = when {
        !enabled -> DevToolsTheme.textFaint
        primary -> DevToolsTheme.backgroundDeep
        else -> DevToolsTheme.textPrimary
    }
    Text(
        text = label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
    )
}