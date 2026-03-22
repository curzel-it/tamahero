package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class Troop(
    val id: Long,
    val type: TroopType,
    val level: Int = 1,
    val hp: Int,
    val x: Float,
    val y: Float,
    val targetId: Long? = null,
    val path: List<GridPos> = emptyList(),
    val pathTargetId: Long? = null,
)
