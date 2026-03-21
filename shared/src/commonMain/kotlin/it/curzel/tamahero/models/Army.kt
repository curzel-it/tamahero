package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class ArmyTroop(
    val type: TroopType,
    val level: Int = 1,
    val count: Int,
)

@Serializable
data class Army(
    val troops: List<ArmyTroop> = emptyList(),
) {
    val totalCount: Int get() = troops.sumOf { it.count }

    fun add(type: TroopType, level: Int): Army {
        val existing = troops.find { it.type == type && it.level == level }
        return if (existing != null) {
            Army(troops.map {
                if (it.type == type && it.level == level) it.copy(count = it.count + 1) else it
            })
        } else {
            Army(troops + ArmyTroop(type, level, 1))
        }
    }
}
