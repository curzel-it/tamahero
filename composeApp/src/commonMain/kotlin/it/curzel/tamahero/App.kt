package it.curzel.tamahero

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import it.curzel.tamahero.rendering.GameView

@Composable
fun App() {
    MaterialTheme {
        GameView()
    }
}
