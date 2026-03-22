package it.curzel.tamahero.village

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.network.GameSocketClient
import it.curzel.tamahero.network.WsLogEntry
import it.curzel.tamahero.ui.theme.TamaColors
import it.curzel.tamahero.ui.theme.TamaRadius
import it.curzel.tamahero.ui.theme.TamaSpacing

@Composable
fun WsDebugView(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<List<WsLogEntry>>(emptyList()) }
    var snapshotText by remember { mutableStateOf(AnnotatedString("")) }

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
                        snapshot = GameSocketClient.messageLog.value
                        snapshotText = buildLogText(snapshot)
                    }
                    expanded = !expanded
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("WS", color = TamaColors.TextMuted, fontSize = 11.sp)
        }

        AnimatedVisibility(visible = expanded) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .padding(top = TamaSpacing.XXSmall)
                    .width(420.dp)
                    .heightIn(max = 500.dp)
                    .clip(RoundedCornerShape(TamaRadius.Medium))
                    .background(TamaColors.Background.copy(alpha = 0.95f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TamaColors.SurfaceElevated)
                        .padding(horizontal = TamaSpacing.XSmall, vertical = TamaSpacing.XXSmall + 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("WebSocket Log (${snapshot.size})", color = TamaColors.Text, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XSmall)) {
                        Text(
                            "Refresh",
                            color = TamaColors.Accent,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(TamaRadius.XSmall))
                                .clickable {
                                    snapshot = GameSocketClient.messageLog.value
                                    snapshotText = buildLogText(snapshot)
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

                SelectionContainer {
                    Text(
                        text = snapshotText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(TamaSpacing.XSmall),
                    )
                }
            }
        }
    }
}

private fun buildLogText(messages: List<WsLogEntry>): AnnotatedString {
    return buildAnnotatedString {
        for ((i, entry) in messages.withIndex()) {
            if (i > 0) append("\n")
            val isSent = entry.direction == "SENT"
            val dirColor = if (isSent) TamaColors.Success else TamaColors.Info

            withStyle(SpanStyle(color = TamaColors.TextMuted)) {
                append(formatTime(entry.timestamp))
                append(" ")
            }
            withStyle(SpanStyle(color = dirColor)) {
                append(if (isSent) ">>> " else "<<< ")
            }
            withStyle(SpanStyle(color = TamaColors.Text)) {
                append(prettyJson(entry.text))
            }
            append("\n")
        }
    }
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
