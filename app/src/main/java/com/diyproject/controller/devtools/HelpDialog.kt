package com.diyproject.controller.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Reusable in-app help sheet for developer-tool screens — a scrollable
 * list of [HelpSection]s, each control paired with a plain description
 * of what it does and when to use it. Terminal is the first consumer;
 * Code Control can pass its own section list through the same dialog
 * rather than building a second one.
 */
@Composable
fun HelpDialog(
    screenTitle: String,
    sections: List<HelpSection>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .consolePanel(corner = 14.dp)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOW TO USE · $screenTitle",
                    color = DevToolsTheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = "CLOSE",
                    color = DevToolsTheme.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(sections) { section -> HelpSectionBlock(section) }
            }
        }
    }
}

@Composable
private fun HelpSectionBlock(section: HelpSection) {
    Column {
        Text(
            text = section.title,
            color = DevToolsTheme.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            section.items.forEach { item -> HelpItemRow(item) }
        }
    }
}

@Composable
private fun HelpItemRow(item: HelpItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DevToolsTheme.panelFill)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = item.label,
            color = DevToolsTheme.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = DevToolsTheme.mono,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text = item.description,
            color = DevToolsTheme.textMuted,
            fontSize = 11.sp
        )
    }
}