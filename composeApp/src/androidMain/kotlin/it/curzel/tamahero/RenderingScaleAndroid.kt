package it.curzel.tamahero

import android.content.Context
import it.curzel.tamahero.rendering.RenderingScaleProvider

class RenderingScaleAndroid(private val context: Context) : RenderingScaleProvider {
    override fun calculateScale(windowWidth: Float, windowHeight: Float): Float {
        val density = context.resources.displayMetrics.density
        return density * 2f
    }
}
