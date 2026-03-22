package it.curzel.tamahero

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import it.curzel.tamahero.auth.AuthClient
import it.curzel.tamahero.auth.AuthScreenView
import it.curzel.tamahero.auth.AuthState
import it.curzel.tamahero.auth.AuthViewModel
import it.curzel.tamahero.models.ServerMessage
import it.curzel.tamahero.network.GameSocketClient
import it.curzel.tamahero.rendering.GameView
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun App() {
    val authClient = remember { AuthClient() }
    val authViewModel = remember { AuthViewModel(authClient) }
    val authState by authViewModel.authState.collectAsState()

    MaterialTheme(colorScheme = darkColorScheme()) {
        when (val state = authState) {
            is AuthState.LoggedIn -> {
                LaunchedEffect(state.token) {
                    GameSocketClient.connect(state.token)
                    val connected = withTimeoutOrNull(10_000) {
                        GameSocketClient.events.filterIsInstance<ServerMessage.Connected>().first()
                    }
                    if (connected != null) {
                        GameSocketClient.getVillage()
                    } else {
                        GameSocketClient.disconnect()
                        authViewModel.logout()
                    }
                }
                DisposableEffect(state.token) {
                    onDispose { GameSocketClient.disconnect() }
                }
                GameView()
            }
            else -> AuthScreenView(authViewModel)
        }
    }
}
