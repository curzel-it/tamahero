package it.curzel.tamahero.models

import kotlin.math.sqrt

object BattleUpdateUseCase {

    private const val TICK_MS = 100L
    private const val REBUILD_CHANCE = 0.7
    private const val SHIELD_UNDER_30 = 4 * 3_600_000L
    private const val SHIELD_30_TO_89 = 8 * 3_600_000L
    private const val SHIELD_90_PLUS = 12 * 3_600_000L

    fun update(state: GameState, now: Long): GameState {
        if (state.troops.isEmpty()) return state

        // Initialize battle state on first tick
        var current = if (state.preBattleBuildings.isEmpty()) {
            val shieldHp = state.village.buildings
                .filter { it.type == BuildingType.ShieldDome && it.constructionStartedAt == null }
                .sumOf { BuildingConfig.configFor(it.type, it.level)?.shieldHp ?: 0 }
            state.copy(
                preBattleBuildings = state.village.buildings,
                battleShieldHp = shieldHp,
            )
        } else state

        var time = current.lastUpdatedAt
        while (time < now && current.troops.isNotEmpty()) {
            current = tick(current, TICK_MS)
            time += TICK_MS
        }

        // Battle ended — post-battle processing
        if (current.troops.isEmpty() && current.preBattleBuildings.isNotEmpty()) {
            current = postBattle(current, now)
        }

        return current.copy(lastUpdatedAt = now)
    }

    private fun tick(state: GameState, deltaMs: Long): GameState {
        val buildings = state.village.buildings.toMutableList()
        var troops = state.troops.toMutableList()
        var shieldHp = state.battleShieldHp
        val deltaSeconds = deltaMs / 1000.0

        // Troop actions
        for (i in troops.indices) {
            val troop = troops[i]
            val config = TroopConfig.configFor(troop.type, troop.level) ?: continue
            val target = findTarget(troop, buildings) ?: continue
            val dist = distanceTo(troop, target)

            if (dist <= config.range) {
                val damage = (config.dps * deltaSeconds).toInt()
                shieldHp = applyDamageToBuilding(buildings, target.id, damage, shieldHp)
            } else {
                val moveAmount = (config.speed * deltaSeconds).toFloat()
                val dx = target.x + 0.5f - troop.x
                val dy = target.y + 0.5f - troop.y
                troops[i] = troop.copy(
                    x = troop.x + (dx / dist) * moveAmount,
                    y = troop.y + (dy / dist) * moveAmount,
                    targetId = target.id,
                )
            }
        }

        // Trigger traps
        troops = triggerTraps(troops, buildings).toMutableList()

        // Defense damage (cannons, archer towers, mortars)
        troops = applyDefenseDamage(troops, buildings, deltaSeconds).toMutableList()

        return state.copy(
            village = state.village.copy(buildings = buildings),
            troops = troops,
            battleShieldHp = shieldHp,
        )
    }

    private fun findTarget(troop: Troop, buildings: List<PlacedBuilding>): PlacedBuilding? {
        val available = buildings.filter { !it.type.isTrap && it.type != BuildingType.ShieldDome }
        if (available.isEmpty()) return null

        val preferred = when (troop.type) {
            TroopType.OrcBerserker -> available.maxByOrNull { it.hp }
            TroopType.DwarfSapper -> available.filter { it.type.isDefense }.minByOrNull { distanceTo(troop, it) }
                ?: available.minByOrNull { distanceTo(troop, it) }
            else -> available.minByOrNull { distanceTo(troop, it) }
        } ?: return null

        // Check if a wall blocks the path to the preferred target
        val blockingWall = buildings
            .filter { it.type == BuildingType.Wall }
            .filter { isWallBlocking(troop, preferred, it) }
            .minByOrNull { distanceTo(troop, it) }

        return blockingWall ?: preferred
    }

    private fun isWallBlocking(troop: Troop, target: PlacedBuilding, wall: PlacedBuilding): Boolean {
        val wx = wall.x + 0.5f
        val wy = wall.y + 0.5f
        val tx = target.x + 0.5f
        val ty = target.y + 0.5f

        // Wall must be between troop and target (closer to troop than target)
        val distToWall = distanceTo(troop, wall)
        val distToTarget = distanceTo(troop, target)
        if (distToWall >= distToTarget) return false

        // Check if the wall is roughly on the line from troop to target
        val dx = tx - troop.x
        val dy = ty - troop.y
        val len = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (len < 0.01f) return false

        // Project wall position onto the troop→target line
        val t = ((wx - troop.x) * dx + (wy - troop.y) * dy) / (len * len)
        if (t < 0 || t > 1) return false

        val projX = troop.x + t * dx
        val projY = troop.y + t * dy
        val perpDist = sqrt(((wx - projX) * (wx - projX) + (wy - projY) * (wy - projY)).toDouble()).toFloat()

        return perpDist < 1.0f
    }

    private fun triggerTraps(troops: MutableList<Troop>, buildings: MutableList<PlacedBuilding>): List<Troop> {
        val result = troops.toMutableList()
        for (i in buildings.indices) {
            val trap = buildings[i]
            if (!trap.type.isTrap || trap.triggered) continue
            val config = BuildingConfig.configFor(trap.type, trap.level) ?: continue

            val troopsOnTrap = result.filter { isOnTile(it, trap) }
            if (troopsOnTrap.isEmpty()) continue

            when (trap.type) {
                BuildingType.SpikeTrap -> {
                    val affected = result.filter { distanceTo(it, trap) <= config.triggerRadius }
                    for (troop in affected) {
                        val idx = result.indexOfFirst { it.id == troop.id }
                        if (idx >= 0) {
                            val newHp = result[idx].hp - config.burstDamage
                            if (newHp <= 0) result.removeAt(idx)
                            else result[idx] = result[idx].copy(hp = newHp)
                        }
                    }
                    buildings[i] = trap.copy(triggered = true)
                }
                BuildingType.SpringTrap -> {
                    val victim = troopsOnTrap.firstOrNull {
                        it.type == TroopType.HumanSoldier || it.type == TroopType.ElfArcher
                    }
                    if (victim != null) {
                        result.removeAll { it.id == victim.id }
                        buildings[i] = trap.copy(triggered = true)
                    }
                }
                else -> {}
            }
        }
        return result
    }

    private fun isOnTile(troop: Troop, building: PlacedBuilding): Boolean {
        val config = BuildingConfig.configFor(building.type, building.level) ?: return false
        return troop.x >= building.x && troop.x < building.x + config.width &&
               troop.y >= building.y && troop.y < building.y + config.height
    }

    private fun applyDefenseDamage(
        troops: MutableList<Troop>,
        buildings: List<PlacedBuilding>,
        deltaSeconds: Double,
    ): List<Troop> {
        val result = troops.toMutableList()
        for (defense in buildings.filter { it.type.isDefense && !it.type.isTrap }) {
            val config = BuildingConfig.configFor(defense.type, defense.level) ?: continue
            if (config.damage <= 0) continue

            if (config.splashRadius > 0) {
                // Mortar — splash damage to all troops in range, respects minRange
                val inRange = result.filter {
                    val dist = distanceTo(it, defense)
                    dist <= config.range && dist >= config.minRange
                }
                if (inRange.isEmpty()) continue
                val target = inRange.minByOrNull { distanceTo(it, defense) } ?: continue
                val splashCenter = target
                val splashed = result.filter { distanceTo(it, splashCenter) <= config.splashRadius }
                val damage = (config.damage * deltaSeconds).toInt()
                for (troop in splashed) {
                    val idx = result.indexOfFirst { it.id == troop.id }
                    if (idx >= 0) {
                        val newHp = result[idx].hp - damage
                        if (newHp <= 0) result.removeAt(idx)
                        else result[idx] = result[idx].copy(hp = newHp)
                    }
                }
            } else {
                // Single target (Cannon, ArcherTower)
                val target = result.minByOrNull { distanceTo(it, defense) } ?: continue
                val dist = distanceTo(target, defense)
                if (dist > config.range) continue
                val damage = (config.damage * deltaSeconds).toInt()
                val idx = result.indexOfFirst { it.id == target.id }
                if (idx >= 0) {
                    val newHp = result[idx].hp - damage
                    if (newHp <= 0) result.removeAt(idx)
                    else result[idx] = result[idx].copy(hp = newHp)
                }
            }
        }
        return result
    }

    private fun applyDamageToBuilding(
        buildings: MutableList<PlacedBuilding>,
        targetId: Long,
        damage: Int,
        shieldHp: Int,
    ): Int {
        if (shieldHp > 0) {
            val absorbed = damage.coerceAtMost(shieldHp)
            val remaining = damage - absorbed
            val newShieldHp = shieldHp - absorbed
            if (newShieldHp <= 0) {
                buildings.removeAll { it.type == BuildingType.ShieldDome }
            }
            if (remaining <= 0) return newShieldHp
            removeBuildingHp(buildings, targetId, remaining)
            return newShieldHp
        }
        removeBuildingHp(buildings, targetId, damage)
        return 0
    }

    private fun removeBuildingHp(buildings: MutableList<PlacedBuilding>, targetId: Long, damage: Int) {
        val idx = buildings.indexOfFirst { it.id == targetId }
        if (idx < 0) return
        val newHp = buildings[idx].hp - damage
        if (newHp <= 0) buildings.removeAt(idx)
        else buildings[idx] = buildings[idx].copy(hp = newHp)
    }

    private fun postBattle(state: GameState, now: Long): GameState {
        val survivingIds = state.village.buildings.map { it.id }.toSet()
        val destroyed = state.preBattleBuildings.filter { it.id !in survivingIds }

        // Auto-rebuild 70% of destroyed defenses (not traps, not dome)
        val rebuiltDefenses = destroyed
            .filter { it.type.isDefense && !it.type.isTrap }
            .filter { pseudoRandom(it.id, now) < REBUILD_CHANCE }
            .map { building ->
                val config = BuildingConfig.configFor(building.type, building.level)
                building.copy(hp = config?.hp ?: building.hp)
            }

        val updatedBuildings = state.village.buildings + rebuiltDefenses

        // Calculate destruction percentage for shield
        val totalPreBattle = state.preBattleBuildings.size
        val destroyedCount = destroyed.size - rebuiltDefenses.size
        val destructionPct = if (totalPreBattle > 0) (destroyedCount * 100) / totalPreBattle else 0

        val shieldDuration = when {
            destructionPct >= 90 -> SHIELD_90_PLUS
            destructionPct >= 30 -> SHIELD_30_TO_89
            destructionPct > 0 -> SHIELD_UNDER_30
            else -> 0L
        }

        return state.copy(
            village = state.village.copy(buildings = updatedBuildings),
            preBattleBuildings = emptyList(),
            battleShieldHp = 0,
            shieldExpiresAt = if (shieldDuration > 0) now + shieldDuration else state.shieldExpiresAt,
        )
    }

    private fun pseudoRandom(id: Long, seed: Long): Double {
        val hash = (id * 31 + seed) xor (id * 17 + seed * 13)
        return (hash.mod(100)) / 100.0
    }

    private fun distanceTo(troop: Troop, building: PlacedBuilding): Float {
        val dx = building.x + 0.5f - troop.x
        val dy = building.y + 0.5f - troop.y
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun distanceTo(a: Troop, b: Troop): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
