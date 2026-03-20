package it.curzel.tamahero

import it.curzel.tamahero.rendering.RenderingScaleProvider
import platform.UIKit.UIScreen

class RenderingScaleIos : RenderingScaleProvider {
    override fun calculateScale(windowWidth: Float, windowHeight: Float): Float {
        return UIScreen.mainScreen.scale.toFloat() * 2f
    }
}
