package it.curzel.tamahero.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.village.FloatingText

fun DrawScope.drawFloatingTexts(
    texts: List<FloatingText>,
    textMeasurer: TextMeasurer,
    canvasSize: Size,
) {
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    var yOffset = 60f

    for (ft in texts) {
        val age = now - ft.createdAt
        if (age > 2000) continue
        val alpha = (1f - age / 2000f).coerceIn(0f, 1f)
        val rise = (age / 2000f) * 30f

        val layout = textMeasurer.measure(
            ft.text,
            style = TextStyle(color = ft.color.copy(alpha = alpha), fontSize = 14.sp),
        )
        drawText(
            layout,
            topLeft = Offset(10f, yOffset - rise),
        )
        yOffset += 20f
    }
}
