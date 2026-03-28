package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.DefenseLogEntry
import it.curzel.tamahero.ui.theme.*

@Composable
fun DefenseLogView(
    entries: List<DefenseLogEntry>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TamaColors.Background.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(400.dp)
                .clip(RoundedCornerShape(TamaRadius.Large))
                .background(TamaColors.Surface)
                .clickable(enabled = false, onClick = {})
                .padding(TamaSpacing.Large),
        ) {
            Text("Defense Log", color = TamaColors.Text, fontSize = 18.sp)
            Spacer(Modifier.height(TamaSpacing.Small))

            if (entries.isEmpty()) {
                Text("No attacks received yet", color = TamaColors.TextMuted, fontSize = 14.sp)
            } else {
                for (entry in entries.sortedByDescending { it.timestamp }) {
                    DefenseLogEntryRow(entry)
                    Spacer(Modifier.height(TamaSpacing.XSmall))
                }
            }

            Spacer(Modifier.height(TamaSpacing.Medium))
            TamaSecondaryButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DefenseLogEntryRow(entry: DefenseLogEntry) {
    val stars = "\u2B50".repeat(entry.stars) + "\u2606".repeat(3 - entry.stars)
    val timeAgo = formatTimeAgo(entry.timestamp)
    val trophyText = if (entry.trophyDelta >= 0) "+${entry.trophyDelta}" else "${entry.trophyDelta}"
    val trophyColor = if (entry.trophyDelta >= 0) TamaColors.Success else TamaColors.Error

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TamaRadius.Small))
            .background(TamaColors.SurfaceElevated)
            .padding(TamaSpacing.Small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(entry.attackerName, color = TamaColors.Text, fontSize = 14.sp)
            Text(timeAgo, color = TamaColors.TextMuted, fontSize = 12.sp)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(TamaSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stars, fontSize = 14.sp)
            Text("Trophies: $trophyText", color = trophyColor, fontSize = 13.sp)
        }
        val loot = entry.lootLost
        if (loot.credits > 0 || loot.alloy > 0 || loot.crystal > 0) {
            val lostText = buildList {
                if (loot.credits > 0) add("-${loot.credits} credits")
                if (loot.alloy > 0) add("-${loot.alloy} alloy")
                if (loot.crystal > 0) add("-${loot.crystal} crystal")
            }.joinToString(", ")
            Text(lostText, color = TamaColors.Error, fontSize = 12.sp)
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val deltaMs = now - timestamp
    val minutes = deltaMs / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        else -> "${minutes / 1440}d ago"
    }
}
