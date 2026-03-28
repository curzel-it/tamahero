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

            // End battle if no targetable buildings remain (attackers win)
            val hasTargets = current.village.buildings.any { !it.type.isTrap && it.type != BuildingType.ShieldDome }
            if (!hasTargets) {
                current = current.copy(troops = emptyList())
                break
            }
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
                var damage = (config.dps * deltaSeconds).toInt().coerceAtLeast(1)
                if (target.type == BuildingType.Wall && config.wallDamageMultiplier > 1f) {
                    damage = (damage * config.wallDamageMultiplier).toInt()
                }
                if (config.splashRadius > 0f) {
                    // Splash damage to nearby buildings
                    for (b in buildings.indices) {
                        if (buildings[b].id == target.id) continue
                        if (distanceBetweenBuildings(target, buildings[b]) <= config.splashRadius) {
                            shieldHp = applyDamageToBuilding(buildings, buildings[b].id, damage / 2, shieldHp)
                        }
                    }
                }
                shieldHp = applyDamageToBuilding(buildings, target.id, damage, shieldHp)
                troops[i] = troop.copy(targetId = target.id, path = emptyList(), pathTargetId = null)
            } else {
                troops[i] = moveAlongPath(troop, target, buildings, config, deltaSeconds)
            }
        }

        // Trigger traps
        troops = triggerTraps(troops, buildings).toMutableList()

        // Defense damage
        troops = applyDefenseDamage(troops, buildings, deltaSeconds).toMutableList()

        return state.copy(
            village = state.village.copy(buildings = buildings),
            troops = troops,
            battleShieldHp = shieldHp,
        )
    }

    private fun moveAlongPath(
        troop: Troop,
        target: PlacedBuilding,
        buildings: List<PlacedBuilding>,
        config: TroopLevelConfig,
        deltaSeconds: Double,
    ): Troop {
        val needsRepath = troop.pathTargetId != target.id || troop.path.isEmpty()
        val path = if (needsRepath) {
            Pathfinding.findPath(troop.x, troop.y, target, buildings)
        } else {
            troop.path
        }

        if (path != null && path.isNotEmpty()) {
            val waypoint = path.first()
            val wpX = waypoint.x + 0.5f
            val wpY = waypoint.y + 0.5f
            val dx = wpX - troop.x
            val dy = wpY - troop.y
            val wpDist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            val moveAmount = (config.speed * deltaSeconds).toFloat()

            return if (wpDist <= moveAmount) {
                troop.copy(
                    x = wpX, y = wpY,
                    targetId = target.id,
                    path = path.drop(1),
                    pathTargetId = target.id,
                )
            } else {
                troop.copy(
                    x = troop.x + (dx / wpDist) * moveAmount,
                    y = troop.y + (dy / wpDist) * moveAmount,
                    targetId = target.id,
                    path = path,
                    pathTargetId = target.id,
                )
            }
        }

        // No path found — fall back to straight-line toward nearest point of building
        val moveAmount = (config.speed * deltaSeconds).toFloat()
        val bConfig = BuildingConfig.configFor(target.type, target.level)
        val bw = bConfig?.width ?: 2
        val bh = bConfig?.height ?: 2
        val nearX = troop.x.coerceIn(target.x.toFloat(), (target.x + bw).toFloat())
        val nearY = troop.y.coerceIn(target.y.toFloat(), (target.y + bh).toFloat())
        val dx = nearX - troop.x
        val dy = nearY - troop.y
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (dist < 0.01f) return troop.copy(targetId = target.id, path = emptyList(), pathTargetId = null)
        return troop.copy(
            x = troop.x + (dx / dist) * moveAmount,
            y = troop.y + (dy / dist) * moveAmount,
            targetId = target.id,
            path = emptyList(),
            pathTargetId = null,
        )
    }

    private fun findTarget(troop: Troop, buildings: List<PlacedBuilding>): PlacedBuilding? {
        val available = buildings.filter { !it.type.isTrap && it.type != BuildingType.ShieldDome }
        if (available.isEmpty()) return null

        return when (troop.type.targetPreference) {
            TargetPreference.Defenses ->
                available.filter { it.type.isDefense }.minByOrNull { distanceTo(troop, it) }
                    ?: available.minByOrNull { distanceTo(troop, it) }
            TargetPreference.Walls ->
                available.filter { it.type == BuildingType.Wall }.minByOrNull { distanceTo(troop, it) }
                    ?: available.filter { it.type.isDefense }.minByOrNull { distanceTo(troop, it) }
                    ?: available.minByOrNull { distanceTo(troop, it) }
            TargetPreference.Resources ->
                available.filter { it.type.isResource }.minByOrNull { distanceTo(troop, it) }
                    ?: available.minByOrNull { distanceTo(troop, it) }
            TargetPreference.Nearest ->
                available.minByOrNull { distanceTo(troop, it) }
        }
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
                BuildingType.LandMine, BuildingType.NovaBomb -> {
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
                BuildingType.GravityWell -> {
                    val victim = troopsOnTrap.firstOrNull {
                        it.type == TroopType.Marine || it.type == TroopType.Sniper || it.type == TroopType.Drone
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
                // Splash damage to all troops in range, respects minRange
                val inRange = result.filter {
                    val dist = distanceTo(it, defense)
                    dist <= config.range && dist >= config.minRange
                }
                if (inRange.isEmpty()) continue
                val target = inRange.minByOrNull { distanceTo(it, defense) } ?: continue
                val splashCenter = target
                val splashed = result.filter { distanceTo(it, splashCenter) <= config.splashRadius }
                val damage = (config.damage * deltaSeconds).toInt().coerceAtLeast(1)
                for (troop in splashed) {
                    val idx = result.indexOfFirst { it.id == troop.id }
                    if (idx >= 0) {
                        val newHp = result[idx].hp - damage
                        if (newHp <= 0) result.removeAt(idx)
                        else result[idx] = result[idx].copy(hp = newHp)
                    }
                }
            } else {
                // Single target
                val target = result.minByOrNull { distanceTo(it, defense) } ?: continue
                val dist = distanceTo(target, defense)
                if (dist > config.range) continue
                val damage = (config.damage * deltaSeconds).toInt().coerceAtLeast(1)
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
        val config = BuildingConfig.configFor(building.type, building.level)
        val bw = config?.width ?: 2
        val bh = config?.height ?: 2
        val nearestX = troop.x.coerceIn(building.x.toFloat(), (building.x + bw).toFloat())
        val nearestY = troop.y.coerceIn(building.y.toFloat(), (building.y + bh).toFloat())
        val dx = nearestX - troop.x
        val dy = nearestY - troop.y
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun distanceTo(a: Troop, b: Troop): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun distanceBetweenBuildings(a: PlacedBuilding, b: PlacedBuilding): Float {
        val ac = BuildingConfig.configFor(a.type, a.level)
        val bc = BuildingConfig.configFor(b.type, b.level)
        val aCx = a.x + (ac?.width ?: 2) / 2f
        val aCy = a.y + (ac?.height ?: 2) / 2f
        val bCx = b.x + (bc?.width ?: 2) / 2f
        val bCy = b.y + (bc?.height ?: 2) / 2f
        val dx = aCx - bCx
        val dy = aCy - bCy
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
