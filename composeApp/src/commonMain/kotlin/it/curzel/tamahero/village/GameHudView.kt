package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import it.curzel.tamahero.models.Resources
import it.curzel.tamahero.ui.theme.*

@Composable
fun GameHudView(
    resources: Resources,
    isPlacing: Boolean,
    onBuildClick: () -> Unit,
    onCancelClick: () -> Unit,
    onAccountClick: () -> Unit,
    onArmyClick: () -> Unit = {},
    onHeroClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onCollectAllClick: () -> Unit = {},
    onWsLogClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(TamaColors.Background.copy(alpha = 0.8f))
            .padding(horizontal = TamaSpacing.Small, vertical = TamaSpacing.XSmall),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(TamaSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Gold: ${resources.gold}", color = TamaColors.Gold, fontSize = 14.sp)
            Text("Wood: ${resources.wood}", color = TamaColors.Wood, fontSize = 14.sp)
            Text("Metal: ${resources.metal}", color = TamaColors.Metal, fontSize = 14.sp)
            Text("Mana: ${resources.mana}", color = TamaColors.Mana, fontSize = 14.sp)
        }
        Spacer(Modifier.height(TamaSpacing.XXSmall))
        Row(
            horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XSmall),
        ) {
            if (isPlacing) {
                TamaDangerButton(text = "Cancel", onClick = onCancelClick)
            } else {
                TamaButton(text = "Build", onClick = onBuildClick, color = TamaColors.Success)
                TamaSecondaryButton(text = "Collect", onClick = onCollectAllClick)
                TamaSecondaryButton(text = "Army", onClick = onArmyClick)
                TamaSecondaryButton(text = "Hero", onClick = onHeroClick)
            }
            TamaSecondaryButton(text = "Account", onClick = onAccountClick)
            TamaSecondaryButton(text = "Settings", onClick = onSettingsClick)
            TamaSecondaryButton(text = "WS", onClick = onWsLogClick)
        }
    }
}
