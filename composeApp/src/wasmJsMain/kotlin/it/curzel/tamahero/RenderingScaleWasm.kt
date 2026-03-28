package it.curzel.tamahero

import it.curzel.tamahero.rendering.RenderingScaleProvider
import kotlinx.browser.window

class RenderingScaleWasm : RenderingScaleProvider {
    override fun calculateScale(windowWidth: Float, windowHeight: Float): Float {
        return (window.devicePixelRatio * 2.0).toFloat()
    }
}
