package it.curzel.tamahero.village

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.network.GameSocketClient
import it.curzel.tamahero.network.WsLogEntry
import it.curzel.tamahero.ui.theme.*

@Composable
fun WsDebugView(modifier: Modifier = Modifier) {
    val messages by GameSocketClient.messageLog.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TamaColors.SurfaceElevated.copy(alpha = 0.8f))
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center,
        ) {
            Text("WS", color = TamaColors.TextMuted, fontSize = 11.sp)
        }

        AnimatedVisibility(visible = expanded) {
            val listState = rememberLazyListState()

            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.lastIndex)
                }
            }

            Column(
                modifier = Modifier
                    .padding(top = TamaSpacing.XXSmall)
                    .width(360.dp)
                    .heightIn(max = 400.dp)
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
                    Text("WebSocket Log (${messages.size})", color = TamaColors.Text, fontSize = 12.sp)
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

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(messages.size) { index ->
                        val entry = messages[index]
                        val isSelected = index == selectedIndex
                        WsLogItem(
                            entry = entry,
                            isExpanded = isSelected,
                            onClick = { selectedIndex = if (isSelected) -1 else index },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WsLogItem(
    entry: WsLogEntry,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val isSent = entry.direction == "SENT"
    val dirColor = if (isSent) TamaColors.Success else TamaColors.Info
    val timeStr = formatTime(entry.timestamp)
    val preview = entry.text.take(80).replace("\n", " ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = TamaSpacing.XSmall, vertical = TamaSpacing.XXSmall),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isSent) ">>>" else "<<<", color = dirColor, fontSize = 10.sp)
            Spacer(Modifier.width(TamaSpacing.XXSmall))
            Text(timeStr, color = TamaColors.TextMuted, fontSize = 10.sp)
            Spacer(Modifier.width(TamaSpacing.XXSmall))
            Text(preview, color = TamaColors.TextMuted, fontSize = 11.sp, maxLines = 1)
        }
        if (isExpanded) {
            Text(
                entry.text,
                color = TamaColors.Text,
                fontSize = 11.sp,
                modifier = Modifier
                    .padding(top = TamaSpacing.XXSmall, start = TamaSpacing.Large)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TamaRadius.XSmall))
                    .background(TamaColors.SurfaceElevated)
                    .padding(TamaSpacing.XSmall),
            )
        }
    }
}

private fun formatTime(epochMs: Long): String {
    val totalSeconds = (epochMs / 1000) % 86400
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
