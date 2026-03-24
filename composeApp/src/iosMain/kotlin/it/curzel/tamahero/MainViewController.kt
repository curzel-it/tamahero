package it.curzel.tamahero

import androidx.compose.ui.window.ComposeUIViewController
import it.curzel.tamahero.auth.SocialAuthIos
import it.curzel.tamahero.auth.SocialAuthProviderHolder
import it.curzel.tamahero.auth.TokenStorageIos
import it.curzel.tamahero.auth.TokenStorageProvider
import it.curzel.tamahero.notifications.PushNotificationIos
import it.curzel.tamahero.notifications.PushNotificationProvider
import it.curzel.tamahero.rendering.RenderingScaleProviderHolder

fun MainViewController() = ComposeUIViewController {
    RenderingScaleProviderHolder.setProvider(RenderingScaleIos())
    TokenStorageProvider.setProvider(TokenStorageIos())
    SocialAuthProviderHolder.setProvider(SocialAuthIos())
    PushNotificationProvider.setProvider(PushNotificationIos())
    App()
}
