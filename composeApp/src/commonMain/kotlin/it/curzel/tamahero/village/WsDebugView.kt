package it.curzel.tamahero.village

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.network.GameSocketClient
import it.curzel.tamahero.network.WsLogEntry
import it.curzel.tamahero.ui.theme.TamaColors
import it.curzel.tamahero.ui.theme.TamaRadius
import it.curzel.tamahero.ui.theme.TamaSpacing

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WsDebugView(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("") }
    var logCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TamaColors.SurfaceElevated.copy(alpha = 0.8f))
                .clickable {
                    if (!expanded) {
                        val snapshot = GameSocketClient.messageLog.value
                        logText = buildLogPlainText(snapshot)
                        logCount = snapshot.size
                    }
                    expanded = !expanded
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("WS", color = TamaColors.TextMuted, fontSize = 11.sp)
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .padding(top = TamaSpacing.XXSmall)
                    .width(420.dp)
                    .heightIn(max = 500.dp)
                    .clip(RoundedCornerShape(TamaRadius.Medium))
                    .background(TamaColors.Background.copy(alpha = 0.95f))
                    .onPointerEvent(PointerEventType.Scroll) { it.changes.forEach { c -> c.consume() } },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TamaColors.SurfaceElevated)
                        .padding(horizontal = TamaSpacing.XSmall, vertical = TamaSpacing.XXSmall + 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("WebSocket Log ($logCount)", color = TamaColors.Text, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XSmall)) {
                        Text(
                            "Refresh",
                            color = TamaColors.Accent,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(TamaRadius.XSmall))
                                .clickable {
                                    val snapshot = GameSocketClient.messageLog.value
                                    logText = buildLogPlainText(snapshot)
                                    logCount = snapshot.size
                                }
                                .padding(TamaSpacing.XXSmall),
                        )
                        Text(
                            "X",
                            color = TamaColors.TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { expanded = false }
                                .padding(TamaSpacing.XXSmall),
                        )
                    }
                }

                TextField(
                    value = logText,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = TamaColors.Text,
                    ),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = TamaColors.Background.copy(alpha = 0f),
                        focusedContainerColor = TamaColors.Background.copy(alpha = 0f),
                        unfocusedIndicatorColor = TamaColors.Background.copy(alpha = 0f),
                        focusedIndicatorColor = TamaColors.Background.copy(alpha = 0f),
                        cursorColor = TamaColors.Accent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

private fun buildLogPlainText(messages: List<WsLogEntry>): String {
    val sb = StringBuilder()
    for ((i, entry) in messages.withIndex()) {
        if (i > 0) sb.append('\n')
        val dir = if (entry.direction == "SENT") ">>>" else "<<<"
        sb.append(formatTime(entry.timestamp))
        sb.append(' ')
        sb.append(dir)
        sb.append(' ')
        sb.append(prettyJson(entry.text))
        sb.append('\n')
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
