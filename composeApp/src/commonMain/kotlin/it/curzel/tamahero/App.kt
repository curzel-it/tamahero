package it.curzel.tamahero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import it.curzel.tamahero.auth.AuthClient
import it.curzel.tamahero.auth.AuthScreenView
import it.curzel.tamahero.auth.AuthState
import it.curzel.tamahero.auth.AuthViewModel
import it.curzel.tamahero.ui.theme.TamaColors
import it.curzel.tamahero.ui.theme.TamaSpacing
import it.curzel.tamahero.models.ServerMessage
import it.curzel.tamahero.network.GameSocketClient
import it.curzel.tamahero.rendering.GameView
import it.curzel.tamahero.rendering.initBuildingSprites
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun App(autoLoginUser: String? = null, autoLoginPass: String? = null) {
    LaunchedEffect(Unit) { initBuildingSprites() }
    val authClient = remember { AuthClient() }
    val authViewModel = remember { AuthViewModel(authClient) }
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(autoLoginUser, autoLoginPass) {
        if (autoLoginUser != null && autoLoginPass != null && !authClient.isLoggedIn()) {
            authClient.login(autoLoginUser, autoLoginPass)
        }
    }

    val colorScheme = darkColorScheme(
        primary = TamaColors.Accent,
        secondary = TamaColors.AccentHover,
        background = TamaColors.Background,
        surface = TamaColors.Surface,
        surfaceVariant = TamaColors.SurfaceElevated,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = TamaColors.Text,
        onSurface = TamaColors.Text,
        error = TamaColors.Error,
    )
    MaterialTheme(colorScheme = colorScheme) {
        when (val state = authState) {
            is AuthState.LoggedIn -> {
                var connectionFailed by remember { mutableStateOf(false) }
                var retryTrigger by remember { mutableIntStateOf(0) }

                LaunchedEffect(state.token, retryTrigger) {
                    connectionFailed = false
                    println("[App] Disconnecting previous connection...")
                    GameSocketClient.disconnect()
                    println("[App] Starting connection attempt (retry=$retryTrigger)")
                    val connectedDeferred = async {
                        GameSocketClient.events.filterIsInstance<ServerMessage.Connected>().first()
                    }
                    GameSocketClient.connect(state.token)
                    val connected = withTimeoutOrNull(10_000) { connectedDeferred.await() }
                    if (connected != null) {
                        println("[App] Connected! Requesting village...")
                        GameSocketClient.getVillage()
                    } else {
                        println("[App] Connection timed out after 10s")
                        GameSocketClient.disconnect()
                        connectionFailed = true
                    }
                }
                DisposableEffect(state.token) {
                    onDispose { GameSocketClient.disconnect() }
                }

                if (connectionFailed) {
                    ConnectionErrorView(
                        onRetry = { retryTrigger++ },
                        onLogout = { authViewModel.logout() },
                    )
                } else {
                    GameView(
                        username = state.username,
                        onLogout = { authViewModel.logout() },
                        onDeleteAccount = { authViewModel.deleteAccount() },
                    )
                }
            }
            else -> AuthScreenView(authViewModel)
        }
    }
}

@Composable
private fun ConnectionErrorView(onRetry: () -> Unit, onLogout: () -> Unit) {
    Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("Could not connect to server", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = androidx.compose.ui.Modifier.height(TamaSpacing.Medium))
                Button(onClick = onRetry) { Text("Retry") }
                Spacer(modifier = androidx.compose.ui.Modifier.height(TamaSpacing.XSmall))
                TextButton(onClick = onLogout) { Text("Logout") }
            }
        }
    }
}
