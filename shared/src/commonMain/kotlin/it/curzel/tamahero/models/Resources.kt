package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class Resources(
    val credits: Long = 0,
    val alloy: Long = 0,
    val crystal: Long = 0,
    val plasma: Long = 0,
) {
    operator fun plus(other: Resources) = Resources(
        credits = credits + other.credits,
        alloy = alloy + other.alloy,
        crystal = crystal + other.crystal,
        plasma = plasma + other.plasma,
    )

    operator fun minus(other: Resources) = Resources(
        credits = credits - other.credits,
        alloy = alloy - other.alloy,
        crystal = crystal - other.crystal,
        plasma = plasma - other.plasma,
    )

    operator fun times(factor: Double) = Resources(
        credits = (credits * factor).toLong(),
        alloy = (alloy * factor).toLong(),
        crystal = (crystal * factor).toLong(),
        plasma = (plasma * factor).toLong(),
    )

    fun hasEnough(cost: Resources): Boolean =
        credits >= cost.credits && alloy >= cost.alloy && crystal >= cost.crystal && plasma >= cost.plasma

    fun capAt(cap: Resources) = Resources(
        credits = if (cap.credits > 0) credits.coerceAtMost(cap.credits) else credits,
        alloy = if (cap.alloy > 0) alloy.coerceAtMost(cap.alloy) else alloy,
        crystal = if (cap.crystal > 0) crystal.coerceAtMost(cap.crystal) else crystal,
        plasma = if (cap.plasma > 0) plasma.coerceAtMost(cap.plasma) else plasma,
    )
}
