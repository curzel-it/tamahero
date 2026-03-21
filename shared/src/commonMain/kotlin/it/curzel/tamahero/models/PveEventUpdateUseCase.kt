package it.curzel.tamahero.models

object PveEventUpdateUseCase {

    fun update(state: GameState, now: Long): GameState {
        val event = state.activeEvent ?: return state
        if (event.completed) return state

        return when {
            event.type.isDisaster -> applyDisaster(state, event, now)
            event.type.isBattle -> updateBattleEvent(state, event, now)
            else -> state
        }
    }

    fun startEvent(state: GameState, type: EventType, now: Long): GameState {
        val waves = PveEventConfig.wavesFor(type)
        val rewards = PveEventConfig.rewardsFor(type)
        val event = ActiveEvent(
            type = type,
            startedAt = now,
            totalWaves = waves.size.coerceAtLeast(1),
            rewards = rewards,
        )
        var newState = state.copy(activeEvent = event, lastEventAt = now)

        if (type.isDisaster) {
            return applyDisaster(newState, event, now)
        }

        // Spawn first wave of troops
        if (waves.isNotEmpty()) {
            newState = spawnWave(newState, waves[0], now)
        }

        return newState
    }

    private fun applyDisaster(state: GameState, event: ActiveEvent, now: Long): GameState {
        val damageRange = PveEventConfig.disasterDamagePercent(event.type)
        val buildings = state.village.buildings.toMutableList()
        var totalDamage = 0L

        for (i in buildings.indices) {
            val building = buildings[i]
            if (building.constructionStartedAt != null) continue
            val config = BuildingConfig.configFor(building.type, building.level) ?: continue

            val shouldDamage = when (event.type) {
                EventType.Storm -> isEdgeBuilding(building)
                else -> pseudoRandom(building.id, now, 0) < 0.6
            }
            if (!shouldDamage) continue

            val pct = damageRange.first + ((damageRange.last - damageRange.first) *
                pseudoRandom(building.id, now, 1)).toInt()
            val damage = (config.hp * pct) / 100
            totalDamage += damage
            val newHp = (building.hp - damage).coerceAtLeast(1)
            buildings[i] = building.copy(hp = newHp)
        }

        // Calculate rewards
        val success = totalDamage < state.village.buildings.sumOf { it.hp } / 2
        val pendingRewards = if (success) {
            event.rewards.success
        } else {
            Resources(
                gold = (totalDamage * event.rewards.debrisRecoveryRate * 0.5).toLong(),
                wood = (totalDamage * event.rewards.debrisRecoveryRate * 0.5).toLong(),
            )
        }

        return state.copy(
            village = state.village.copy(buildings = buildings),
            activeEvent = event.copy(completed = true, pendingRewards = pendingRewards),
        )
    }

    private fun updateBattleEvent(state: GameState, event: ActiveEvent, now: Long): GameState {
        // If no troops and battle hasn't been processed by BattleUpdateUseCase yet,
        // check if we need to spawn the next wave
        if (state.troops.isEmpty()) {
            val waves = PveEventConfig.wavesFor(event.type)
            val nextWave = event.currentWave + 1

            if (nextWave >= event.totalWaves) {
                // All waves complete — event finished
                return completeEvent(state, event, now)
            }

            // Check if it's time for next wave
            val nextWaveTime = if (event.nextWaveAt > 0) event.nextWaveAt else now
            if (now >= nextWaveTime && nextWave < waves.size) {
                val delay = waves[nextWave].delayMs
                if (delay > 0 && event.nextWaveAt == 0L) {
                    // Schedule next wave
                    return state.copy(
                        activeEvent = event.copy(nextWaveAt = now + delay),
                    )
                }
                // Spawn next wave
                var newState = spawnWave(state, waves[nextWave], now)
                newState = newState.copy(
                    activeEvent = event.copy(
                        currentWave = nextWave,
                        nextWaveAt = 0,
                    ),
                )
                return newState
            }
        }

        return state
    }

    private fun completeEvent(state: GameState, event: ActiveEvent, now: Long): GameState {
        val preBattle = state.preBattleBuildings
        val surviving = state.village.buildings
        val totalPreHp = preBattle.sumOf { it.hp }.coerceAtLeast(1)
        val destroyedHp = preBattle.sumOf { it.hp } - surviving.sumOf { it.hp }
        val destructionPct = (destroyedHp * 100) / totalPreHp

        val success = destructionPct < 50
        val pendingRewards = if (success) {
            event.rewards.success
        } else {
            val recoveryValue = (destroyedHp * event.rewards.debrisRecoveryRate).toLong()
            Resources(
                gold = recoveryValue / 3,
                wood = recoveryValue / 3,
                metal = recoveryValue / 6,
            )
        }

        return state.copy(
            activeEvent = event.copy(completed = true, pendingRewards = pendingRewards),
        )
    }

    private fun spawnWave(state: GameState, wave: EventWave, now: Long): GameState {
        val gridSize = 40
        var nextId = (state.troops.maxOfOrNull { it.id } ?: 0) + 1
        val newTroops = mutableListOf<Troop>()

        for (eventTroop in wave.troops) {
            val config = TroopConfig.configFor(eventTroop.type, eventTroop.level)
            val baseHp = eventTroop.hp ?: config?.hp ?: 50
            for (i in 0 until eventTroop.count) {
                // Spawn on random edge of the grid
                val edge = ((nextId * 7 + i) % 4).toInt()
                val pos = ((nextId * 13 + i * 3) % (gridSize - 2) + 1).toFloat()
                val (x, y) = when (edge) {
                    0 -> 0f to pos           // left
                    1 -> gridSize.toFloat() to pos  // right
                    2 -> pos to 0f           // top
                    else -> pos to gridSize.toFloat() // bottom
                }
                newTroops.add(Troop(
                    id = nextId++,
                    type = eventTroop.type,
                    level = eventTroop.level,
                    hp = baseHp,
                    x = x,
                    y = y,
                ))
            }
        }

        return state.copy(troops = state.troops + newTroops)
    }

    private fun isEdgeBuilding(building: PlacedBuilding): Boolean {
        val config = BuildingConfig.configFor(building.type, building.level)
        val size = config?.size ?: 2
        return building.x <= 2 || building.y <= 2 ||
            building.x + size >= 38 || building.y + size >= 38
    }

    private fun pseudoRandom(id: Long, seed: Long, salt: Int): Double {
        val hash = (id * 31 + seed + salt * 97) xor (id * 17 + seed * 13)
        return (hash.mod(100)) / 100.0
    }
}
