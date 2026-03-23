package it.curzel.tamahero.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import it.curzel.tamahero.game.TILE_SIZE
import it.curzel.tamahero.models.Troop
import it.curzel.tamahero.models.TroopType
import it.curzel.tamahero.sprites.SpritesRepository
import it.curzel.tamahero.utils.Vector2d
import kotlin.math.roundToInt

private const val SPRITES_SHEET_ID: UInt = 100u

fun DrawScope.drawTroops(
    troops: List<Troop>,
    camera: Vector2d,
    renderingScale: Float,
) {
    val tileSize = TILE_SIZE * renderingScale
    val spriteSheet = SpritesRepository.imageBitmap(SPRITES_SHEET_ID)

    for (troop in troops) {
        val screenX = (troop.x - camera.x) * tileSize
        val screenY = (troop.y - camera.y) * tileSize
        val troopSize = tileSize * 0.8f

        val region = SpriteSheetConfig.regionFor(troop.type)
        if (region != null) {
            drawImage(
                image = spriteSheet,
                srcOffset = IntOffset(region.x, region.y),
                srcSize = IntSize(region.width, region.height),
                dstOffset = IntOffset(
                    (screenX - troopSize * 0.1f).roundToInt(),
                    (screenY - troopSize * 0.1f).roundToInt(),
                ),
                dstSize = IntSize(troopSize.roundToInt(), troopSize.roundToInt()),
                filterQuality = FilterQuality.None,
            )
        } else {
            val color = when (troop.type) {
                TroopType.Marine -> Color(0xFF4CAF50)
                TroopType.Sniper -> Color(0xFF8BC34A)
                TroopType.Engineer -> Color(0xFF795548)
                TroopType.Juggernaut -> Color(0xFFF44336)
                TroopType.Drone -> Color(0xFF9C27B0)
                TroopType.Spectre -> Color(0xFF3F51B5)
                TroopType.Gunship -> Color(0xFFFF5722)
            }
            val radius = tileSize * 0.3f
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(screenX + tileSize * 0.5f, screenY + tileSize * 0.5f),
            )
        }

        // HP bar
        val maxHp = it.curzel.tamahero.models.TroopConfig.configFor(troop.type, troop.level)?.hp ?: troop.hp
        if (troop.hp < maxHp) {
            val barWidth = tileSize * 0.6f
            val barHeight = 2f * renderingScale
            val barX = screenX + (tileSize - barWidth) * 0.5f
            val barY = screenY - barHeight - 2f
            val hpRatio = troop.hp.toFloat() / maxHp
            drawRect(Color.Red, Offset(barX, barY), Size(barWidth, barHeight))
            drawRect(Color.Green, Offset(barX, barY), Size(barWidth * hpRatio, barHeight))
        }
    }
}
