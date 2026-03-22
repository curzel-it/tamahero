package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.network.GameSocketClient
import it.curzel.tamahero.network.WsLogEntry
import it.curzel.tamahero.ui.theme.TamaColors
import it.curzel.tamahero.ui.theme.TamaRadius
import it.curzel.tamahero.ui.theme.TamaSpacing

@Composable
fun WsDebugPanel(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages by GameSocketClient.messageLog.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(messages.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .width(400.dp)
            .background(TamaColors.Background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TamaColors.SurfaceElevated)
                .padding(horizontal = TamaSpacing.Small, vertical = TamaSpacing.XSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("WebSocket Log (${messages.size})", color = TamaColors.Text, fontSize = 13.sp)
            Text(
                "Close",
                color = TamaColors.Accent,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(TamaRadius.XSmall))
                    .clickable(onClick = onClose)
                    .padding(TamaSpacing.XXSmall),
            )
        }

        HorizontalDivider(color = TamaColors.SurfaceElevated, thickness = 1.dp)

        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = buildLogPlainText(messages),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = TamaColors.Text,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(TamaSpacing.XSmall),
            )
        }
    }
}

private fun buildLogPlainText(messages: List<WsLogEntry>): String {
    val sb = StringBuilder()
    for (entry in messages) {
        val dir = if (entry.direction == "SENT") ">>>" else "<<<"
        sb.append(formatTime(entry.timestamp))
        sb.append(' ')
        sb.append(dir)
        sb.append(' ')
        sb.append(prettyJson(entry.text))
        sb.append("\n\n")
    }
    return sb.toString()
}

private fun prettyJson(raw: String): String {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return trimmed
    val sb = StringBuilder()
    var indent = 0
    var inString = false
    var escape = false
    for (c in trimmed) {
        if (escape) {
            sb.append(c)
            escape = false
            continue
        }
        if (c == '\\' && inString) {
            sb.append(c)
            escape = true
            continue
        }
        if (c == '"') {
            inString = !inString
            sb.append(c)
            continue
        }
        if (inString) {
            sb.append(c)
            continue
        }
        when (c) {
            '{', '[' -> {
                sb.append(c)
                indent++
                sb.append('\n')
                repeat(indent) { sb.append("  ") }
            }
            '}', ']' -> {
                indent--
                sb.append('\n')
                repeat(indent) { sb.append("  ") }
                sb.append(c)
            }
            ',' -> {
                sb.append(",\n")
                repeat(indent) { sb.append("  ") }
            }
            ':' -> sb.append(": ")
            ' ', '\n', '\r', '\t' -> {}
            else -> sb.append(c)
        }
    }
    return sb.toString()
}

private fun formatTime(epochMs: Long): String {
    val totalSeconds = (epochMs / 1000) % 86400
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
