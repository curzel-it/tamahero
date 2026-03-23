package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.*
import it.curzel.tamahero.ui.theme.*

@Composable
fun PvpBattleHudView(
    battle: PvpBattle,
    selectedTroop: TroopType?,
    onSelectTroop: (TroopType) -> Unit,
    onSurrender: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(TamaColors.Background.copy(alpha = 0.85f))
            .padding(horizontal = TamaSpacing.Small, vertical = TamaSpacing.XSmall),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(TamaSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "vs ${battle.defenderName}",
                color = TamaColors.Text,
                fontSize = 14.sp,
            )
            val stars = "\u2B50".repeat(battle.currentStars) + "\u2606".repeat(3 - battle.currentStars)
            Text(stars, fontSize = 16.sp)
            Text(
                "${battle.destructionPercent}%",
                color = if (battle.destructionPercent >= 50) TamaColors.Success else TamaColors.Text,
                fontSize = 14.sp,
            )
        }

        Spacer(Modifier.height(TamaSpacing.XXSmall))

        Row(
            horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (troop in battle.availableTroops.troops) {
                val isSelected = selectedTroop == troop.type
                TamaButton(
                    text = "${troop.type.name.take(4)} x${troop.count}",
                    color = if (isSelected) TamaColors.Accent else TamaColors.Success,
                    enabled = troop.count > 0,
                    onClick = { onSelectTroop(troop.type) },
                )
            }
            if (battle.availableTroops.troops.isEmpty() && battle.deployedTroops.isEmpty()) {
                Text("No troops left", color = TamaColors.TextMuted, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            TamaDangerButton(text = "Surrender", onClick = onSurrender)
        }

        if (selectedTroop != null) {
            Spacer(Modifier.height(TamaSpacing.XXSmall))
            Text("Tap on map edges to deploy ${selectedTroop.name}", color = TamaColors.TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
fun PvpResultView(
    result: PvpResult,
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
            val title = when {
                result.stars >= 3 -> "Total Destruction!"
                result.stars >= 1 -> "Victory!"
                else -> "Defeat"
            }
            val titleColor = if (result.stars >= 1) TamaColors.Success else TamaColors.Error
            Text(title, color = titleColor, fontSize = 22.sp)

            Spacer(Modifier.height(TamaSpacing.Medium))

            val stars = "\u2B50".repeat(result.stars) + "\u2606".repeat(3 - result.stars)
            Text(stars, fontSize = 28.sp)

            Spacer(Modifier.height(TamaSpacing.XSmall))
            Text(
                "${result.destructionPercent}% destruction",
                color = TamaColors.TextMuted,
                fontSize = 14.sp,
            )

            Spacer(Modifier.height(TamaSpacing.Medium))

            if (result.loot.credits > 0 || result.loot.alloy > 0 || result.loot.crystal > 0) {
                Text("Loot", color = TamaColors.Text, fontSize = 16.sp)
                Spacer(Modifier.height(TamaSpacing.XSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    if (result.loot.credits > 0) LootColumn("Credits", result.loot.credits, TamaColors.Credits)
                    if (result.loot.alloy > 0) LootColumn("Alloy", result.loot.alloy, TamaColors.Alloy)
                    if (result.loot.crystal > 0) LootColumn("Crystal", result.loot.crystal, TamaColors.Crystal)
                }
            }

            Spacer(Modifier.height(TamaSpacing.Medium))

            val trophyText = if (result.attackerTrophyDelta >= 0) "+${result.attackerTrophyDelta}" else "${result.attackerTrophyDelta}"
            val trophyColor = if (result.attackerTrophyDelta >= 0) TamaColors.Success else TamaColors.Error
            Text("Trophies: $trophyText", color = trophyColor, fontSize = 18.sp)

            Spacer(Modifier.height(TamaSpacing.Large))

            TamaButton(text = "Return Home", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LootColumn(label: String, amount: Long, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TamaColors.TextMuted, fontSize = 12.sp)
        Text("+$amount", color = color, fontSize = 16.sp)
    }
}
