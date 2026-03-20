package it.curzel.tamahero

import it.curzel.tamahero.rendering.RenderingScaleProvider
import java.awt.GraphicsEnvironment

class RenderingScaleDesktop : RenderingScaleProvider {
    override fun calculateScale(windowWidth: Float, windowHeight: Float): Float {
        val displayScale = try {
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val gd = ge.defaultScreenDevice
            2f * gd.defaultConfiguration.defaultTransform.scaleX.toFloat()
        } catch (_: Exception) {
            2f
        }
        return displayScale
    }
}
