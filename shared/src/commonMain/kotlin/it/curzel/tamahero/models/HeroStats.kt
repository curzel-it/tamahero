package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class HeroStats(
    val strength: Int = 10,
    val agility: Int = 10,
    val intelligence: Int = 10,
    val endurance: Int = 10
)
