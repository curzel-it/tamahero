package it.curzel.tamahero.utils

data class IntPoint(
    val x: Int,
    val y: Int
) {
    companion object {
        fun zero() = IntPoint(0, 0)
    }

    operator fun plus(other: IntPoint) = IntPoint(x + other.x, y + other.y)
    operator fun minus(other: IntPoint) = IntPoint(x - other.x, y - other.y)
    operator fun times(scalar: Int) = IntPoint(x * scalar, y * scalar)
}
