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
import it.curzel.tamahero.ui.theme.*

@Composable
fun PveEventResultView(
    result: ServerMessage.EventEnded,
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
                .clickable(enabled = false, onClick = {})
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
            val hasRewards = rewards.credits > 0 || rewards.alloy > 0 || rewards.crystal > 0 || rewards.plasma > 0
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
                    if (rewards.alloy > 0) RewardColumn("Alloy", rewards.alloy, TamaColors.Alloy)
                    if (rewards.crystal > 0) RewardColumn("Crystal", rewards.crystal, TamaColors.Crystal)
                    if (rewards.plasma > 0) RewardColumn("Plasma", rewards.plasma, TamaColors.Plasma)
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
