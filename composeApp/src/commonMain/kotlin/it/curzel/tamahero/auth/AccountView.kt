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
import it.curzel.tamahero.ui.theme.*

@Composable
fun AccountView(
    username: String,
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
                .clickable(enabled = false, onClick = {})
                .padding(TamaSpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Account", color = TamaColors.Text, fontSize = 20.sp)
            Spacer(Modifier.height(TamaSpacing.Medium))
            Text(username, color = TamaColors.TextMuted, fontSize = 16.sp)
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
