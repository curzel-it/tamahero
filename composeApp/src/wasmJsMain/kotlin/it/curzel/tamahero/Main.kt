package it.curzel.tamahero

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import it.curzel.tamahero.auth.SocialAuthProviderHolder
import it.curzel.tamahero.auth.SocialAuthWasm
import it.curzel.tamahero.auth.TokenStorageProvider
import it.curzel.tamahero.auth.TokenStorageWasm
import it.curzel.tamahero.network.GameSocketManager
import it.curzel.tamahero.notifications.PushNotificationProvider
import it.curzel.tamahero.notifications.PushNotificationWasm
import it.curzel.tamahero.rendering.RenderingScaleProviderHolder
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    RenderingScaleProviderHolder.setProvider(RenderingScaleWasm())
    TokenStorageProvider.setProvider(TokenStorageWasm())
    SocialAuthProviderHolder.setProvider(SocialAuthWasm())
    PushNotificationProvider.setProvider(PushNotificationWasm())
    GameSocketManager.initialize(HttpClient { install(WebSockets) })

    ComposeViewport(document.body!!) {
        App()
    }
}
