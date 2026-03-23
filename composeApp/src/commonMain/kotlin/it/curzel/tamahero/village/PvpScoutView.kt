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
import it.curzel.tamahero.models.MatchmakingResult
import it.curzel.tamahero.ui.theme.*

@Composable
fun PvpScoutView(
    match: MatchmakingResult,
    searching: Boolean,
    error: String?,
    onAttack: () -> Unit,
    onNext: () -> Unit,
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
                .width(360.dp)
                .clip(RoundedCornerShape(TamaRadius.Large))
                .background(TamaColors.Surface)
                .clickable(enabled = false, onClick = {})
                .padding(TamaSpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Enemy Village", color = TamaColors.Text, fontSize = 20.sp)
            Spacer(Modifier.height(TamaSpacing.Medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(match.targetName, color = TamaColors.Text, fontSize = 16.sp)
                Text("CC ${match.targetCommandCenterLevel}", color = TamaColors.TextMuted, fontSize = 14.sp)
            }

            Spacer(Modifier.height(TamaSpacing.XSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Trophies: ${match.targetTrophies}", color = TamaColors.Warning, fontSize = 14.sp)
                Text("Buildings: ${match.targetBase.buildings.size}", color = TamaColors.TextMuted, fontSize = 14.sp)
            }

            Spacer(Modifier.height(TamaSpacing.Medium))
            Text("Available Loot", color = TamaColors.Text, fontSize = 16.sp)
            Spacer(Modifier.height(TamaSpacing.XSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                LootItem("Credits", match.lootAvailable.credits, TamaColors.Credits)
                LootItem("Alloy", match.lootAvailable.alloy, TamaColors.Alloy)
                LootItem("Crystal", match.lootAvailable.crystal, TamaColors.Crystal)
            }
            if (match.lootAvailable.plasma > 0) {
                Spacer(Modifier.height(TamaSpacing.XXSmall))
                Text("Plasma: ${match.lootAvailable.plasma}", color = TamaColors.Plasma, fontSize = 14.sp)
            }

            if (error != null) {
                Spacer(Modifier.height(TamaSpacing.Small))
                Text(error, color = TamaColors.Error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(TamaSpacing.Large))

            Row(
                horizontalArrangement = Arrangement.spacedBy(TamaSpacing.Small),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TamaDangerButton(
                    text = "Attack!",
                    onClick = onAttack,
                    modifier = Modifier.weight(1f),
                )
                TamaSecondaryButton(
                    text = if (searching) "..." else "Next",
                    onClick = onNext,
                    enabled = !searching,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LootItem(label: String, amount: Long, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TamaColors.TextMuted, fontSize = 12.sp)
        Text("$amount", color = color, fontSize = 16.sp)
    }
}
