package it.curzel.tamahero.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.ui.theme.TamaSpacing

@Composable
fun LegalLinks(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XXSmall),
    ) {
        TextButton(onClick = { uriHandler.openUri("https://curzel.it/terms-and-conditions") }) {
            Text("Terms", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { uriHandler.openUri("https://curzel.it/privacy") }) {
            Text("Privacy", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
