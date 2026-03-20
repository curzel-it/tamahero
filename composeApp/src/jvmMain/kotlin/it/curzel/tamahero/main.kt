package it.curzel.tamahero

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import it.curzel.tamahero.rendering.RenderingScaleProviderHolder

fun main() {
    RenderingScaleProviderHolder.setProvider(RenderingScaleDesktop())

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "TamaHero",
        ) {
            App()
        }
    }
}
