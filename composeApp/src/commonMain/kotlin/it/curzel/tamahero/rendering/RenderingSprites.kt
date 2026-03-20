package it.curzel.tamahero.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import it.curzel.tamahero.game.TILE_SIZE
import it.curzel.tamahero.sprites.AnimatedSprite
import it.curzel.tamahero.sprites.SpritesRepository
import it.curzel.tamahero.utils.FRect
import it.curzel.tamahero.utils.Vector2d
import kotlin.math.roundToInt

fun DrawScope.drawTileMap(
    tileMap: ImageBitmap,
    camera: Vector2d,
    renderingScale: Float
) {
    val screenX = (-camera.x * TILE_SIZE * renderingScale).roundToInt()
    val screenY = (-camera.y * TILE_SIZE * renderingScale).roundToInt()
    val screenW = (tileMap.width * renderingScale).roundToInt()
    val screenH = (tileMap.height * renderingScale).roundToInt()

    drawImage(
        image = tileMap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(tileMap.width, tileMap.height),
        dstOffset = IntOffset(screenX, screenY),
        dstSize = IntSize(screenW, screenH),
        filterQuality = FilterQuality.None
    )
}

fun DrawScope.drawSprite(
    sprite: AnimatedSprite,
    frame: FRect,
    rotationZ: Float,
    camera: Vector2d,
    renderingScale: Float
) {
    if (sprite.isBlank()) return

    val screenX = (frame.x - camera.x) * TILE_SIZE * renderingScale
    val screenY = (frame.y - camera.y) * TILE_SIZE * renderingScale
    val screenW = frame.w * TILE_SIZE * renderingScale
    val screenH = frame.h * TILE_SIZE * renderingScale

    val destination = androidx.compose.ui.geometry.Rect(
        left = screenX,
        top = screenY,
        right = screenX + screenW,
        bottom = screenY + screenH
    )

    val pivot = Offset(
        x = screenX + screenW / 2,
        y = screenY + screenH / 2
    )

    withTransform({ rotate(degrees = rotationZ, pivot = pivot) }) {
        drawImage(
            image = SpritesRepository.imageBitmap(sprite.spriteSheet),
            srcOffset = IntOffset(
                x = (sprite.frame.x * TILE_SIZE).roundToInt(),
                y = (sprite.frame.y * TILE_SIZE).roundToInt()
            ),
            srcSize = IntSize(
                width = (sprite.frame.w * TILE_SIZE).roundToInt(),
                height = (sprite.frame.h * TILE_SIZE).roundToInt()
            ),
            dstOffset = IntOffset(
                x = destination.left.roundToInt(),
                y = destination.top.roundToInt()
            ),
            dstSize = IntSize(
                width = destination.width.roundToInt(),
                height = destination.height.roundToInt()
            ),
            filterQuality = FilterQuality.None
        )
    }
}
