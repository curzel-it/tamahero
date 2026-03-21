package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class Village(
    val playerId: Long,
    val buildings: List<PlacedBuilding>,
)
