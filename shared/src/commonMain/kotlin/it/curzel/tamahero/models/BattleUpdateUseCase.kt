package it.curzel.tamahero.models

import kotlin.math.sqrt

object BattleUpdateUseCase {

    private const val TICK_MS = 100L

    fun update(state: GameState, now: Long): GameState {
        if (state.troops.isEmpty()) return state
        var current = state
        var time = state.lastUpdatedAt
        while (time < now) {
            current = tick(current, TICK_MS)
            time += TICK_MS
        }
        return current.copy(lastUpdatedAt = now)
    }

    private fun tick(state: GameState, deltaMs: Long): GameState {
        val buildings = state.village.buildings.toMutableList()
        var troops = state.troops.toMutableList()
        val deltaSeconds = deltaMs / 1000.0

        for (i in troops.indices) {
            val troop = troops[i]
            val config = TroopConfig.configFor(troop.type, troop.level) ?: continue
            val target = findTarget(troop, buildings) ?: continue
            val dx = target.x + 0.5f - troop.x
            val dy = target.y + 0.5f - troop.y
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

            if (dist <= config.range) {
                val damage = (config.dps * deltaSeconds).toInt()
                val idx = buildings.indexOfFirst { it.id == target.id }
                if (idx >= 0) {
                    val newHp = buildings[idx].hp - damage
                    if (newHp <= 0) {
                        buildings.removeAt(idx)
                    } else {
                        buildings[idx] = buildings[idx].copy(hp = newHp)
                    }
                }
            } else {
                val moveAmount = (config.speed * deltaSeconds).toFloat()
                val nx = troop.x + (dx / dist) * moveAmount
                val ny = troop.y + (dy / dist) * moveAmount
                troops[i] = troop.copy(x = nx, y = ny, targetId = target.id)
            }
        }

        troops = applyDefenseDamage(troops, buildings, deltaSeconds).toMutableList()

        return state.copy(
            village = state.village.copy(buildings = buildings),
            troops = troops,
        )
    }

    private fun findTarget(troop: Troop, buildings: List<PlacedBuilding>): PlacedBuilding? {
        if (buildings.isEmpty()) return null
        return when (troop.type) {
            TroopType.OrcBerserker -> buildings.maxByOrNull { it.hp }
            TroopType.DwarfSapper -> buildings.filter { isDefense(it.type) }.minByOrNull { distanceTo(troop, it) }
                ?: buildings.minByOrNull { distanceTo(troop, it) }
            else -> buildings.minByOrNull { distanceTo(troop, it) }
        }
    }

    private fun applyDefenseDamage(
        troops: MutableList<Troop>,
        buildings: List<PlacedBuilding>,
        deltaSeconds: Double,
    ): List<Troop> {
        val defenses = buildings.filter { isDefense(it.type) }
        val result = troops.toMutableList()
        for (defense in defenses) {
            val config = BuildingConfig.configFor(defense.type, defense.level) ?: continue
            if (config.damage <= 0) continue
            val target = result.minByOrNull { distanceTo(it, defense) } ?: continue
            val dist = distanceTo(target, defense)
            if (dist > config.range) continue
            val damage = (config.damage * deltaSeconds).toInt()
            val idx = result.indexOfFirst { it.id == target.id }
            if (idx >= 0) {
                val newHp = result[idx].hp - damage
                if (newHp <= 0) {
                    result.removeAt(idx)
                } else {
                    result[idx] = result[idx].copy(hp = newHp)
                }
            }
        }
        return result
    }

    private fun distanceTo(troop: Troop, building: PlacedBuilding): Float {
        val dx = building.x + 0.5f - troop.x
        val dy = building.y + 0.5f - troop.y
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun isDefense(type: BuildingType): Boolean = when (type) {
        BuildingType.Cannon, BuildingType.ArcherTower, BuildingType.Wall -> true
        else -> false
    }
}
