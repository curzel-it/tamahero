package it.curzel.tamahero

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import it.curzel.tamahero.auth.SocialAuthDesktop
import it.curzel.tamahero.auth.SocialAuthProviderHolder
import it.curzel.tamahero.auth.TokenStorageDesktop
import it.curzel.tamahero.auth.TokenStorageProvider
import it.curzel.tamahero.network.GameSocketManager
import it.curzel.tamahero.notifications.PushNotificationDesktop
import it.curzel.tamahero.notifications.PushNotificationProvider
import it.curzel.tamahero.rendering.RenderingScaleProviderHolder

fun main(args: Array<String>) {
    val parsedArgs = parseArgs(args)

    RenderingScaleProviderHolder.setProvider(RenderingScaleDesktop())
    TokenStorageProvider.setProvider(TokenStorageDesktop(parsedArgs.user ?: "default"))
    SocialAuthProviderHolder.setProvider(SocialAuthDesktop())
    PushNotificationProvider.setProvider(PushNotificationDesktop())
    GameSocketManager.initialize(HttpClient { install(WebSockets) })

    if (parsedArgs.server != null) {
        ServerConfig.overrideBaseUrl(parsedArgs.server)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = if (parsedArgs.user != null) "TamaHero — ${parsedArgs.user}" else "TamaHero",
        ) {
            App(autoLoginUser = parsedArgs.user, autoLoginPass = parsedArgs.pass)
        }
    }
}

private data class ParsedArgs(
    val user: String? = null,
    val pass: String? = null,
    val server: String? = null,
)

private fun parseArgs(args: Array<String>): ParsedArgs {
    var user: String? = null
    var pass: String? = null
    var server: String? = null

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--user" -> { user = args.getOrNull(i + 1); i += 2 }
            "--pass" -> { pass = args.getOrNull(i + 1); i += 2 }
            "--server" -> { server = args.getOrNull(i + 1); i += 2 }
            else -> i++
        }
    }
    return ParsedArgs(user, pass, server)
}
