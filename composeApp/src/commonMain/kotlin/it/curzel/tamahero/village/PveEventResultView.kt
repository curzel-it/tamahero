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
import it.curzel.tamahero.models.ServerMessage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import it.curzel.tamahero.ui.theme.*

@Composable
fun PveEventResultView(
    result: ServerMessage.EventEnded,
    damageSummary: List<DamagedBuildingInfo>?,
    onCollect: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TamaColors.Background.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(340.dp)
                .clip(RoundedCornerShape(TamaRadius.Large))
                .background(TamaColors.Surface)
                .clickable { }
                .padding(TamaSpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val title = if (result.success) "Defense Successful!" else "Defense Failed!"
            val titleColor = if (result.success) TamaColors.Success else TamaColors.Error
            Text(title, color = titleColor, fontSize = 22.sp)

            Spacer(Modifier.height(TamaSpacing.Small))
            Text(
                result.eventType.name,
                color = TamaColors.TextMuted,
                fontSize = 14.sp,
            )

            val rewards = result.rewards
            val hasRewards = rewards.credits > 0 || rewards.metal > 0 || rewards.crystal > 0 || rewards.deuterium > 0
            if (hasRewards) {
                Spacer(Modifier.height(TamaSpacing.Medium))
                Text(
                    if (result.success) "Rewards" else "Debris Recovery",
                    color = TamaColors.Text,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(TamaSpacing.XSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    if (rewards.credits > 0) RewardColumn("Credits", rewards.credits, TamaColors.Credits)
                    if (rewards.metal > 0) RewardColumn("Alloy", rewards.metal, TamaColors.Alloy)
                    if (rewards.crystal > 0) RewardColumn("Crystal", rewards.crystal, TamaColors.Crystal)
                    if (rewards.deuterium > 0) RewardColumn("Plasma", rewards.deuterium, TamaColors.Plasma)
                }
            }

            if (damageSummary != null) {
                Spacer(Modifier.height(TamaSpacing.Medium))
                Text("Damage Report", color = TamaColors.Text, fontSize = 16.sp)
                Spacer(Modifier.height(TamaSpacing.XXSmall))
                for (d in damageSummary) {
                    val text = if (d.destroyed) "${d.name}: Destroyed" else "${d.name}: ${d.hpBefore} \u2192 ${d.hpAfter} HP"
                    Text(text, color = if (d.destroyed) TamaColors.Error else TamaColors.Warning, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(TamaSpacing.Large))

            if (hasRewards) {
                TamaButton(text = "Collect Rewards", onClick = onCollect, modifier = Modifier.fillMaxWidth())
            } else {
                TamaButton(text = "Dismiss", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RewardColumn(label: String, amount: Long, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TamaColors.TextMuted, fontSize = 12.sp)
        Text("+$amount", color = color, fontSize = 16.sp)
    }
}
