package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class Resources(
    val credits: Long = 0,
    val metal: Long = 0,
    val crystal: Long = 0,
    val deuterium: Long = 0,
) {
    operator fun plus(other: Resources) = Resources(
        credits = credits + other.credits,
        metal = metal + other.metal,
        crystal = crystal + other.crystal,
        deuterium = deuterium + other.deuterium,
    )

    operator fun minus(other: Resources) = Resources(
        credits = credits - other.credits,
        metal = metal - other.metal,
        crystal = crystal - other.crystal,
        deuterium = deuterium - other.deuterium,
    )

    operator fun times(factor: Double) = Resources(
        credits = (credits * factor).toLong(),
        metal = (metal * factor).toLong(),
        crystal = (crystal * factor).toLong(),
        deuterium = (deuterium * factor).toLong(),
    )

    fun hasEnough(cost: Resources): Boolean =
        credits >= cost.credits && metal >= cost.metal && crystal >= cost.crystal && deuterium >= cost.deuterium

    fun capAt(cap: Resources) = Resources(
        credits = if (cap.credits > 0) credits.coerceAtMost(cap.credits) else credits,
        metal = if (cap.metal > 0) metal.coerceAtMost(cap.metal) else metal,
        crystal = if (cap.crystal > 0) crystal.coerceAtMost(cap.crystal) else crystal,
        deuterium = if (cap.deuterium > 0) deuterium.coerceAtMost(cap.deuterium) else deuterium,
    )
}
