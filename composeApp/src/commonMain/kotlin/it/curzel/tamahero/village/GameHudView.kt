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
    onAttackClick: () -> Unit = {},
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
            Text("Credits: ${resources.credits}", color = TamaColors.Credits, fontSize = 14.sp)
            Text("Alloy: ${resources.alloy}", color = TamaColors.Alloy, fontSize = 14.sp)
            Text("Crystal: ${resources.crystal}", color = TamaColors.Crystal, fontSize = 14.sp)
            Text("Plasma: ${resources.plasma}", color = TamaColors.Plasma, fontSize = 14.sp)
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
                TamaDangerButton(text = "Attack", onClick = onAttackClick)
            }
            TamaSecondaryButton(text = "Account", onClick = onAccountClick)
            TamaSecondaryButton(text = "Settings", onClick = onSettingsClick)
            TamaSecondaryButton(text = "WS", onClick = onWsLogClick)
        }
    }
}
