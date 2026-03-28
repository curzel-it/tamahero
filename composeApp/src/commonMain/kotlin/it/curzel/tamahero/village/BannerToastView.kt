package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.ui.theme.TamaColors
import it.curzel.tamahero.ui.theme.TamaRadius
import it.curzel.tamahero.ui.theme.TamaSpacing
import kotlinx.coroutines.delay

@Composable
fun BannerToastView(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(message) {
        delay(3000)
        onDismiss()
    }

    Box(
        modifier = modifier
            .padding(TamaSpacing.Medium)
            .fillMaxWidth(0.8f)
            .clip(RoundedCornerShape(TamaRadius.Medium))
            .background(TamaColors.Info.copy(alpha = 0.9f))
            .clickable(onClick = onDismiss)
            .padding(horizontal = TamaSpacing.Medium, vertical = TamaSpacing.Small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            color = TamaColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
