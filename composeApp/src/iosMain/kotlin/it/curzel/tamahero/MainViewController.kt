package it.curzel.tamahero

import androidx.compose.ui.window.ComposeUIViewController
import it.curzel.tamahero.rendering.RenderingScaleProviderHolder

fun MainViewController() = ComposeUIViewController {
    RenderingScaleProviderHolder.setProvider(RenderingScaleIos())
    App()
}
