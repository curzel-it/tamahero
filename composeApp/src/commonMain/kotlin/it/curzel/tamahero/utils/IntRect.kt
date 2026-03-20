package it.curzel.tamahero.utils

data class IntRect(
    var x: Int,
    var y: Int,
    var w: Int,
    var h: Int
) {
    companion object {
        fun zero() = IntRect(0, 0, 0, 0)
    }

    fun copy(): IntRect = IntRect(x, y, w, h)
}
