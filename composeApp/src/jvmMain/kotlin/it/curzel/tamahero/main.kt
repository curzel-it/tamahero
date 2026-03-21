package it.curzel.tamahero

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import it.curzel.tamahero.auth.SocialAuthDesktop
import it.curzel.tamahero.auth.SocialAuthProviderHolder
import it.curzel.tamahero.auth.TokenStorageDesktop
import it.curzel.tamahero.auth.TokenStorageProvider
import it.curzel.tamahero.rendering.RenderingScaleProviderHolder

fun main() {
    RenderingScaleProviderHolder.setProvider(RenderingScaleDesktop())
    TokenStorageProvider.setProvider(TokenStorageDesktop())
    SocialAuthProviderHolder.setProvider(SocialAuthDesktop())

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "TamaHero",
        ) {
            App()
        }
    }
}
