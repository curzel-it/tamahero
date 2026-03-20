package it.curzel.tamahero.rendering

interface RenderingScaleProvider {
    fun calculateScale(windowWidth: Float, windowHeight: Float): Float
}

object RenderingScaleProviderHolder {
    lateinit var instance: RenderingScaleProvider
        private set

    fun setProvider(provider: RenderingScaleProvider) {
        instance = provider
    }
}
