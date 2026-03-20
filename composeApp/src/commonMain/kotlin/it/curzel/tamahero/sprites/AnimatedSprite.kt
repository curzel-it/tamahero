package it.curzel.tamahero.sprites

import it.curzel.tamahero.utils.IntPoint
import it.curzel.tamahero.utils.IntRect
import it.curzel.tamahero.utils.TimedContentProvider

data class AnimatedSprite(
    var spriteSheet: UInt,
    var frame: IntRect,
    var originalFrame: IntRect = frame.copy(),
    var isFlippedHorizontally: Boolean = false,
    var isFlippedVertically: Boolean = false,
    var drawOffset: IntPoint = IntPoint.zero(),
    val fps: Float = 1f,
    val numberOfLoops: Int = 0,
    var numberOfFrames: Int = 1,
    val isPixelArt: Boolean = true
) {
    private var framesProvider: TimedContentProvider<Int> = TimedContentProvider(
        frames = (0 until numberOfFrames).map { originalFrame.x + it * originalFrame.w },
        fps = fps
    )
    private var lastFrameX = -1

    fun isBlank(): Boolean {
        return spriteSheet == 0u
    }

    fun update(timeSinceLastUpdate: Float) {
        if (numberOfFrames > 1) {
            framesProvider.update(timeSinceLastUpdate)
            val newFrameX = framesProvider.currentFrame()
            if (newFrameX != lastFrameX) {
                frame.x = newFrameX
                lastFrameX = newFrameX
            }
        }
    }

    fun loopDuration(): Float {
        return numberOfFrames.toFloat() / fps
    }

    fun flippedHorizontally(): AnimatedSprite {
        return copy(isFlippedHorizontally = !isFlippedHorizontally)
    }

    fun flippedVertically(): AnimatedSprite {
        return copy(isFlippedVertically = !isFlippedVertically)
    }

    companion object {
        fun new(spriteSheet: UInt, frame: IntRect, numberOfFrames: Int = 1): AnimatedSprite {
            return AnimatedSprite(
                spriteSheet = spriteSheet,
                frame = frame,
                originalFrame = frame.copy(),
                numberOfFrames = numberOfFrames,
            )
        }

        fun blank(): AnimatedSprite {
            return AnimatedSprite(
                spriteSheet = 0u,
                frame = IntRect(0, 0, 1, 1),
                numberOfFrames = 1,
            )
        }
    }
}
