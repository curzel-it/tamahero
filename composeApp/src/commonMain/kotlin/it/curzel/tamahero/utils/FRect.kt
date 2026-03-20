package it.curzel.tamahero.utils

import kotlin.math.round

data class FRect(
    var x: Float = 0f,
    var y: Float = 0f,
    var w: Float = 0f,
    var h: Float = 0f
) {

    companion object {
        fun zero() = FRect(0f, 0f, 0f, 0f)

        fun fromOrigin(w: Float, h: Float) = FRect(0f, 0f, w, h)

        fun squareFromOrigin(size: Float) = fromOrigin(size, size)

        private fun linesIntersect(
            x1: Float, y1: Float, x2: Float, y2: Float,
            x3: Float, y3: Float, x4: Float, y4: Float
        ): Boolean {
            val denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
            if (denom == 0f) {
                return false
            }

            val ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom
            val ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom

            return ua >= 0f && ua <= 1f && ub >= 0f && ub <= 1f
        }
    }

    fun padded(padding: Padding): FRect {
        return FRect(
            x + padding.left,
            y + padding.top,
            w - padding.left - padding.right,
            h - padding.top - padding.bottom
        )
    }

    fun paddedAll(padding: Float): FRect {
        return padded(Padding(padding, padding, padding, padding))
    }

    fun center(): Vector2d {
        return Vector2d(x + w / 2f, y + h / 2f)
    }

    fun origin(): Vector2d {
        return Vector2d(x, y)
    }

    fun centerIn(other: FRect) {
        centerAt(other.center())
    }

    fun centerAt(point: Vector2d) {
        x = point.x - w / 2f
        y = point.y - h / 2f
    }

    fun centeredAt(point: Vector2d): FRect {
        val newX = point.x - w / 2f
        val newY = point.y - h / 2f
        return FRect(newX, newY, w, h)
    }

    fun offset(dx: Float, dy: Float): FRect {
        return FRect(x + dx, y + dy, w, h)
    }

    fun offsetBy(delta: Pair<Float, Float>): FRect {
        return offset(delta.first, delta.second)
    }

    fun offsetX(dx: Float): FRect {
        return offset(dx, 0f)
    }

    fun offsetY(dy: Float): FRect {
        return offset(0f, dy)
    }

    fun withH(newH: Float): FRect {
        return FRect(x, y, w, newH)
    }

    fun size(): Vector2d {
        return Vector2d(w, h)
    }

    fun overlapsOrTouches(other: FRect): Boolean {
        return x <= other.x + other.w &&
                x + w >= other.x &&
                y <= other.y + other.h &&
                y + h >= other.y
    }

    fun containsOrTouches(point: Vector2d): Boolean {
        return x <= point.x && point.x <= x + w && y <= point.y && point.y <= y + h
    }

    fun contains(other: FRect): Boolean {
        return x <= other.x && maxX() >= other.maxX() && y <= other.y && maxY() >= other.maxY()
    }

    fun scaled(scalar: Float): FRect {
        return FRect(x * scalar, y * scalar, w * scalar, h * scalar)
    }

    fun scaledFromCenter(scalar: Float): FRect {
        val newW = w * scalar
        val newH = h * scalar
        val c = center()

        return FRect(
            c.x - newW / 2f,
            c.y - newH / 2f,
            newW,
            newH
        )
    }

    fun withClosestIntOrigin(): FRect {
        return FRect(round(x), round(y), w, h)
    }

    fun withClosestIntOriginX(): FRect {
        return FRect(round(x), y, w, h)
    }

    fun withClosestIntOriginY(): FRect {
        return FRect(x, round(y), w, h)
    }

    fun maxX(): Float = x + w

    fun maxY(): Float = y + h

    fun intersectsLine(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val p1 = Vector2d(x1, y1)
        val p2 = Vector2d(x2, y2)

        if (containsOrTouches(p1) && containsOrTouches(p2)) {
            return true
        }

        val edges = listOf(
            Pair(Pair(x, y), Pair(x + w, y)),
            Pair(Pair(x + w, y), Pair(x + w, y + h)),
            Pair(Pair(x + w, y + h), Pair(x, y + h)),
            Pair(Pair(x, y + h), Pair(x, y))
        )

        for ((start, end) in edges) {
            val (ex1, ey1) = start
            val (ex2, ey2) = end
            if (linesIntersect(x1, y1, x2, y2, ex1, ey1, ex2, ey2)) {
                return true
            }
        }

        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FRect
        return (x - other.x).isZero() &&
                (y - other.y).isZero() &&
                (w - other.w).isZero() &&
                (h - other.h).isZero()
    }

    override fun hashCode(): Int {
        var result = (x * 1000).toInt()
        result = 31 * result + (y * 1000).toInt()
        result = 31 * result + (w * 1000).toInt()
        result = 31 * result + (h * 1000).toInt()
        return result
    }

    data class Padding(
        val top: Float,
        val right: Float,
        val bottom: Float,
        val left: Float
    )
}
