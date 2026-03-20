package it.curzel.tamahero.utils

import kotlin.math.abs
import kotlin.math.floor

data class Vector2d(
    val x: Float = 0f,
    val y: Float = 0f
) : ZeroComparable {

    companion object {
        fun zero() = Vector2d(0f, 0f)

        fun fromIndices(x: Int, y: Int) = Vector2d(x.toFloat(), y.toFloat())
    }

    fun scaled(value: Float) = Vector2d(x * value, y * value)

    fun dumbDistanceTo(other: Vector2d): Float {
        return abs(x - other.x) + abs(y - other.y)
    }

    fun offset(offsetX: Float, offsetY: Float) = Vector2d(x + offsetX, y + offsetY)

    fun offsetX(offsetX: Float) = offset(offsetX, 0f)

    fun offsetY(offsetY: Float) = offset(0f, offsetY)

    override fun isZero(): Boolean {
        return x.isZero() && y.isZero()
    }

    override fun isCloseToInt(): Boolean {
        return x.isCloseToInt() && y.isCloseToInt()
    }

    fun isCloseToTile(tolerance: Float): Boolean {
        val absX = abs(x)
        val absY = abs(y)
        return (absX - floor(absX)) < tolerance && (absY - floor(absY)) < tolerance
    }

    operator fun plus(other: Vector2d) = Vector2d(x + other.x, y + other.y)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Vector2d
        return (x - other.x).isZero() && (y - other.y).isZero()
    }

    override fun hashCode(): Int {
        var result = (x * 1000).toInt()
        result = 31 * result + (y * 1000).toInt()
        return result
    }
}
