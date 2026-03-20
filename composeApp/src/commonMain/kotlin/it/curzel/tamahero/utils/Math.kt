package it.curzel.tamahero.utils

import kotlin.math.abs
import kotlin.math.round

private const val EPSILON = 0.001f

fun areEqual(first: Float, second: Float): Boolean {
    return abs(first - second) < 0.0001f
}

interface ZeroComparable {
    fun isZero(): Boolean
    fun isCloseToInt(): Boolean
}

fun Float.isZero(): Boolean {
    return abs(this) < EPSILON
}

fun Float.isCloseToInt(): Boolean {
    return abs(this - round(this)) < EPSILON
}
