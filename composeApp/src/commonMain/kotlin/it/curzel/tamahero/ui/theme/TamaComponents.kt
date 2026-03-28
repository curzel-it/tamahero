package it.curzel.tamahero.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TamaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = TamaColors.Accent,
    tag: String = text.lowercase().replace(" ", "_"),
) {
    val bg = if (enabled) color else TamaColors.SurfaceElevated
    val textColor = if (enabled) Color.White else TamaColors.TextMuted

    Box(
        modifier = modifier
            .testTag(tag)
            .clip(RoundedCornerShape(TamaRadius.Medium))
            .background(bg)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = TamaSpacing.Large, vertical = TamaSpacing.Small),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
fun TamaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tag: String = text.lowercase().replace(" ", "_"),
) {
    val borderColor = if (enabled) TamaColors.TextMuted else TamaColors.SurfaceElevated
    val textColor = if (enabled) TamaColors.Text else TamaColors.TextMuted

    Box(
        modifier = modifier
            .testTag(tag)
            .clip(RoundedCornerShape(TamaRadius.Medium))
            .background(TamaColors.Surface)
            .border(1.dp, borderColor, RoundedCornerShape(TamaRadius.Medium))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = TamaSpacing.Large, vertical = TamaSpacing.Small),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
fun TamaDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tag: String = text.lowercase().replace(" ", "_"),
) {
    val bg = if (enabled) TamaColors.Error.copy(alpha = 0.2f) else TamaColors.SurfaceElevated
    val textColor = if (enabled) TamaColors.Error else TamaColors.TextMuted

    Box(
        modifier = modifier
            .testTag(tag)
            .clip(RoundedCornerShape(TamaRadius.Medium))
            .background(bg)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = TamaSpacing.Large, vertical = TamaSpacing.Small),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
fun TamaGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tag: String = text.lowercase().replace(" ", "_"),
) {
    val textColor = if (enabled) TamaColors.TextMuted else TamaColors.TextMuted.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .testTag(tag)
            .clip(RoundedCornerShape(TamaRadius.Medium))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = TamaSpacing.Large, vertical = TamaSpacing.Small),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}
