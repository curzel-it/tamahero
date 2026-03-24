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
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.LeaderboardEntry
import it.curzel.tamahero.ui.theme.*

@Composable
fun LeaderboardView(
    entries: List<LeaderboardEntry>,
    yourRank: Int,
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
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = TamaRadius.Large, topEnd = TamaRadius.Large))
                .background(TamaColors.Surface)
                .clickable(enabled = false, onClick = {})
                .padding(TamaSpacing.Medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Leaderboard", color = TamaColors.Text, fontSize = 18.sp)
                if (yourRank > 0) {
                    Text("Your Rank: #$yourRank", color = TamaColors.TextMuted, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(TamaSpacing.Small))

            if (entries.isEmpty()) {
                Text("No data yet", color = TamaColors.TextMuted, fontSize = 14.sp)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("#", color = TamaColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(TamaSpacing.Large))
                    Text("Player", color = TamaColors.TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("Trophies", color = TamaColors.TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.width(TamaSpacing.Medium))
                    Text("CC", color = TamaColors.TextMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(TamaSpacing.XXSmall))

                for (entry in entries) {
                    val isYou = entry.rank == yourRank
                    val bg = if (isYou) TamaColors.SurfaceElevated else TamaColors.Surface
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bg)
                            .padding(vertical = TamaSpacing.XXSmall),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${entry.rank}", color = TamaColors.Text, fontSize = 14.sp, modifier = Modifier.width(TamaSpacing.Large))
                        Text(entry.playerName, color = if (isYou) TamaColors.Credits else TamaColors.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("${entry.trophies}", color = TamaColors.Credits, fontSize = 14.sp)
                        Spacer(Modifier.width(TamaSpacing.Medium))
                        Text("Lv${entry.commandCenterLevel}", color = TamaColors.TextMuted, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
