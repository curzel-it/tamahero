package it.curzel.tamahero.utils

class TimedContentProvider<T>(
    private val frames: List<T>,
    fps: Float
) {
    private val frameDuration: Float = if (fps > 0f) 1f / fps else 0f
    private var currentFrameIndex: Int = 0
    var completedLoops: UInt = 0u
        private set
    private var leftover: Float = 0f

    fun currentFrame(): T {
        return frames[currentFrameIndex]
    }

    fun update(timeSinceLastUpdate: Float) {
        leftover += timeSinceLastUpdate

        if (leftover >= frameDuration) {
            leftover -= frameDuration
            loadNextFrame()
        }
    }

    private fun loadNextFrame() {
        val nextIndex = (currentFrameIndex + 1) % frames.size
        checkLoopCompletion(nextIndex)
        currentFrameIndex = nextIndex
    }

    private fun checkLoopCompletion(nextIndex: Int) {
        if (nextIndex < currentFrameIndex) {
            completedLoops++
        }
    }

    fun reset() {
        currentFrameIndex = 0
        leftover = 0f
        completedLoops = 0u
    }
}
