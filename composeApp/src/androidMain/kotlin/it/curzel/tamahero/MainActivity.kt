package it.curzel.tamahero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import it.curzel.tamahero.auth.SocialAuthAndroid
import it.curzel.tamahero.auth.SocialAuthProviderHolder
import it.curzel.tamahero.auth.TokenStorageAndroid
import it.curzel.tamahero.auth.TokenStorageProvider
import it.curzel.tamahero.rendering.RenderingScaleProviderHolder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        RenderingScaleProviderHolder.setProvider(RenderingScaleAndroid())
        TokenStorageProvider.setProvider(TokenStorageAndroid(this))
        SocialAuthProviderHolder.setProvider(SocialAuthAndroid(this))

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
