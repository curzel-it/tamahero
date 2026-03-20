package it.curzel.tamahero

import it.curzel.tamahero.rendering.RenderingScaleProvider

class RenderingScaleAndroid : RenderingScaleProvider {
    override fun calculateScale(windowWidth: Float, windowHeight: Float): Float {
        return 4f
    }
}
