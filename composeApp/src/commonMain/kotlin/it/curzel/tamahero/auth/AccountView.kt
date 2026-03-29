package it.curzel.tamahero.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.Army
import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.DefenseLogEntry
import it.curzel.tamahero.models.PlacedBuilding
import it.curzel.tamahero.ui.theme.*

@Composable
fun AccountView(
    username: String,
    buildings: List<PlacedBuilding> = emptyList(),
    trophies: Int = 0,
    army: Army = Army(),
    defenseLog: List<DefenseLogEntry> = emptyList(),
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TamaColors.Background.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(TamaRadius.Large))
                .background(TamaColors.Surface)
                .clickable { }
                .padding(TamaSpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Account", color = TamaColors.Text, fontSize = 20.sp)
            Spacer(Modifier.height(TamaSpacing.Medium))
            Text(username, color = TamaColors.TextMuted, fontSize = 16.sp)
            Spacer(Modifier.height(TamaSpacing.Medium))

            val ccLevel = buildings
                .filter { it.type == BuildingType.CommandCenter && it.constructionStartedAt == null }
                .maxOfOrNull { it.level } ?: 0
            val defenseWins = defenseLog.count { it.stars < 2 }
            val defenseLosses = defenseLog.count { it.stars >= 2 }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem("CC Level", "$ccLevel")
                StatItem("Trophies", "$trophies")
                StatItem("Troops", "${army.totalCount}")
                StatItem("Buildings", "${buildings.count { it.constructionStartedAt == null }}")
            }
            Spacer(Modifier.height(TamaSpacing.XSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem("Defenses", "${defenseLog.size}")
                StatItem("Defended", "$defenseWins")
                StatItem("Breached", "$defenseLosses")
            }

            Spacer(Modifier.height(TamaSpacing.Large))

            TamaSecondaryButton(
                text = "Log out",
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(TamaSpacing.Large))
            HorizontalDivider(color = TamaColors.SurfaceElevated)
            Spacer(Modifier.height(TamaSpacing.Medium))

            TamaGhostButton(
                text = "Terms and Conditions",
                onClick = { uriHandler.openUri("https://curzel.it/terms-and-conditions") },
                modifier = Modifier.fillMaxWidth(),
            )
            TamaGhostButton(
                text = "Privacy Policy",
                onClick = { uriHandler.openUri("https://curzel.it/privacy") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(TamaSpacing.Medium))
            HorizontalDivider(color = TamaColors.SurfaceElevated)
            Spacer(Modifier.height(TamaSpacing.Medium))

            TamaDangerButton(
                text = "Delete Account",
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Account?") },
            text = { Text("This will permanently delete your account and all your game data. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDeleteAccount()
                }) {
                    Text("Delete", color = TamaColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TamaColors.Text, fontSize = 16.sp)
        Text(label, color = TamaColors.TextMuted, fontSize = 11.sp)
    }
}
