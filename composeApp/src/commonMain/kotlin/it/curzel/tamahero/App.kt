package it.curzel.tamahero

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import it.curzel.tamahero.auth.AuthClient
import it.curzel.tamahero.auth.AuthScreenView
import it.curzel.tamahero.auth.AuthState
import it.curzel.tamahero.auth.AuthViewModel
import it.curzel.tamahero.rendering.GameView

@Composable
fun App() {
    val authClient = remember { AuthClient() }
    val authViewModel = remember { AuthViewModel(authClient) }
    val authState by authViewModel.authState.collectAsState()

    MaterialTheme {
        when (authState) {
            is AuthState.LoggedIn -> GameView()
            else -> AuthScreenView(authViewModel)
        }
    }
}
