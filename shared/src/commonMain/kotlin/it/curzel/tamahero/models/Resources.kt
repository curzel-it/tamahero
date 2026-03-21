package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class Resources(
    val gold: Long = 0,
    val wood: Long = 0,
    val metal: Long = 0,
    val mana: Long = 0,
) {
    operator fun plus(other: Resources) = Resources(
        gold = gold + other.gold,
        wood = wood + other.wood,
        metal = metal + other.metal,
        mana = mana + other.mana,
    )

    operator fun minus(other: Resources) = Resources(
        gold = gold - other.gold,
        wood = wood - other.wood,
        metal = metal - other.metal,
        mana = mana - other.mana,
    )

    fun hasEnough(cost: Resources): Boolean =
        gold >= cost.gold && wood >= cost.wood && metal >= cost.metal && mana >= cost.mana

    fun capAt(cap: Resources) = Resources(
        gold = if (cap.gold > 0) gold.coerceAtMost(cap.gold) else gold,
        wood = if (cap.wood > 0) wood.coerceAtMost(cap.wood) else wood,
        metal = if (cap.metal > 0) metal.coerceAtMost(cap.metal) else metal,
        mana = if (cap.mana > 0) mana.coerceAtMost(cap.mana) else mana,
    )
}
