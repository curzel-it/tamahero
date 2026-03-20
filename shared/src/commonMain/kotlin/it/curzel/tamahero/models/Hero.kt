package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class Hero(
    val id: Long = 0,
    val userId: Long = 0,
    val name: String,
    val level: Int = 1,
    val experience: Long = 0,
    val stats: HeroStats = HeroStats(),
    val currentAction: HeroAction? = null,
    val createdAt: Long = 0
)
