package it.curzel.tamahero.sprites

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import it.curzel.tamahero.game.TILE_SIZE
import it.curzel.tamahero.utils.IntRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import tamahero.composeapp.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
object SpritesRepository {
    private var sprites: MutableMap<UInt, ImageBitmap> = mutableMapOf()
    private var extractedSprites: MutableMap<String, ImageBitmap> = mutableMapOf()

    private val spriteSheetMapping: MutableMap<UInt, String> = mutableMapOf()

    private val placeholder = ImageBitmap(16, 16, ImageBitmapConfig.Argb8888, false, ColorSpaces.Srgb)
    private val loadingSheets = mutableSetOf<UInt>()

    fun registerSheet(sheetId: UInt, name: String) {
        spriteSheetMapping[sheetId] = name
    }

    suspend fun preloadSheet(sheetId: UInt) {
        if (sprites.containsKey(sheetId)) return
        val sheetName = spriteSheetMapping[sheetId] ?: return
        try {
            val image = Res.readBytes("drawable/$sheetName.png").decodeToImageBitmap()
            sprites[sheetId] = image
        } catch (e: Exception) {
            println("Failed to preload sprite sheet: $sheetName - ${e.message}")
        }
    }

    fun imageBitmap(sheetId: UInt): ImageBitmap {
        sprites[sheetId]?.let { return it }

        if (!loadingSheets.contains(sheetId)) {
            loadingSheets.add(sheetId)
            val sheetName = spriteSheetMapping[sheetId] ?: "blank"
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val image = Res.readBytes("drawable/$sheetName.png").decodeToImageBitmap()
                    sprites[sheetId] = image
                    val keysToRemove = extractedSprites.keys.filter { it.startsWith("$sheetId:") }
                    keysToRemove.forEach { extractedSprites.remove(it) }
                } catch (e: Exception) {
                    println("Failed to load sprite sheet: $sheetName - ${e.message}")
                }
            }
        }

        return placeholder
    }

    fun imageBitmap(sprite: AnimatedSprite): ImageBitmap {
        val key = "${sprite.spriteSheet}:${sprite.frame}"
        extractedSprites[key]?.let { return it }

        val image = buildSprite(
            spriteSheetId = sprite.spriteSheet,
            frame = sprite.frame
        )
        if (image !== placeholder) {
            extractedSprites[key] = image
        }
        return image
    }

    private fun buildSprite(spriteSheetId: UInt, frame: IntRect): ImageBitmap {
        val bitmap = imageBitmap(spriteSheetId)
        if (bitmap === placeholder) return placeholder
        val tileSize = TILE_SIZE.toInt()

        val targetBitmap = ImageBitmap(
            width = frame.w * tileSize,
            height = frame.h * tileSize,
            config = ImageBitmapConfig.Argb8888,
            hasAlpha = true,
            colorSpace = ColorSpaces.Srgb
        )

        Canvas(targetBitmap).drawImageRect(
            image = bitmap,
            srcOffset = IntOffset(
                x = frame.x * tileSize,
                y = frame.y * tileSize
            ),
            srcSize = IntSize(
                width = frame.w * tileSize,
                height = frame.h * tileSize
            ),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(
                width = frame.w * tileSize,
                height = frame.h * tileSize
            ),
            paint = androidx.compose.ui.graphics.Paint()
        )

        return targetBitmap
    }

    fun registerCustomSheet(sheetId: UInt, pngBytes: ByteArray) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val image = pngBytes.decodeToImageBitmap()
                sprites[sheetId] = image
                val keysToRemove = extractedSprites.keys.filter { it.startsWith("$sheetId:") }
                keysToRemove.forEach { extractedSprites.remove(it) }
            } catch (e: Exception) {
                println("Failed to register custom sprite sheet: ${e.message}")
            }
        }
    }

    fun removeCustomSheet(sheetId: UInt) {
        sprites.remove(sheetId)
        val keysToRemove = extractedSprites.keys.filter { it.startsWith("$sheetId:") }
        keysToRemove.forEach { extractedSprites.remove(it) }
    }

    fun clear() {
        sprites.clear()
        extractedSprites.clear()
        loadingSheets.clear()
    }
}
