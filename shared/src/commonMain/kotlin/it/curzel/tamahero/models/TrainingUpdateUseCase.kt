package it.curzel.tamahero.models

object TrainingUpdateUseCase {

    fun update(state: GameState, now: Long): GameState {
        if (state.trainingQueue.entries.isEmpty()) return state

        val armyCapacity = totalArmyCapacity(state.village.buildings, now)
        var army = state.army
        val queue = state.trainingQueue.entries.toMutableList()
        val barracksCount = state.village.buildings.count {
            it.type == BuildingType.Academy && it.isComplete(now)
        }
        if (barracksCount == 0) return state

        var changed = false

        // Complete trained troops
        val completed = mutableListOf<Int>()
        for (i in queue.indices) {
            val entry = queue[i]
            val startedAt = entry.startedAt ?: continue
            val config = TroopConfig.configFor(entry.troopType, entry.level) ?: continue
            val completionTime = startedAt + config.trainingTimeSeconds * 1000
            if (now >= completionTime && army.totalCount < armyCapacity) {
                army = army.add(entry.troopType, entry.level)
                completed.add(i)
                changed = true
            }
        }
        for (i in completed.reversed()) {
            queue.removeAt(i)
        }

        // Start training for idle queue entries (one per Barracks)
        val activelyTraining = queue.count { it.startedAt != null }
        val slotsAvailable = barracksCount - activelyTraining
        if (slotsAvailable > 0) {
            var started = 0
            for (i in queue.indices) {
                if (started >= slotsAvailable) break
                if (queue[i].startedAt == null && army.totalCount < armyCapacity) {
                    queue[i] = queue[i].copy(startedAt = now)
                    started++
                    changed = true
                }
            }
        }

        if (!changed) return state

        return state.copy(
            army = army,
            trainingQueue = TrainingQueue(queue),
        )
    }

    private fun totalArmyCapacity(buildings: List<PlacedBuilding>, now: Long): Int {
        var capacity = 0
        for (building in buildings) {
            if (building.type != BuildingType.Hangar) continue
            if (building.isUnderConstruction(now)) continue
            val config = BuildingConfig.configFor(building.type, building.level) ?: continue
            capacity += config.troopCapacity
        }
        return capacity
    }
}
