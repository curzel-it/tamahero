package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class PlacedBuilding(
    val id: Long,
    val type: BuildingType,
    val level: Int,
    val x: Int,
    val y: Int,
    val constructionStartedAt: Long? = null,
    val lastCollectedAt: Long = 0,
    val hp: Int = 0,
    val triggered: Boolean = false,
) {
    fun isUnderConstruction(now: Long): Boolean {
        val startedAt = constructionStartedAt ?: return false
        val config = BuildingConfig.configFor(type, level) ?: return false
        return now < startedAt + config.buildTimeSeconds * 1000
    }

    fun isComplete(now: Long): Boolean = !isUnderConstruction(now)
}
