package it.curzel.tamahero.village

import it.curzel.tamahero.db.VillageRepository
import it.curzel.tamahero.models.*

object VillageService {

    private const val GRID_SIZE = 40

    fun getOrCreateVillage(userId: Long): GameState =
        loadAndUpdate(userId) { it }

    fun placeBuild(userId: Long, type: BuildingType, x: Int, y: Int): GameState =
        loadAndUpdate(userId) { state ->
            val now = state.lastUpdatedAt
            val config = BuildingConfig.configFor(type, 1)
                ?: throw VillageException("Unknown building type")

            requireTownHallLevel(state, now, config.requiredTownHallLevel)
            requireResources(state, config.cost)
            requireWithinBounds(x, y, config.width, config.height)
            requireNoCollision(state.village.buildings, x, y, config.width, config.height, excludeId = null)

            val newId = (state.village.buildings.maxOfOrNull { it.id } ?: 0) + 1
            val newBuilding = PlacedBuilding(
                id = newId, type = type, level = 1,
                x = x, y = y,
                constructionStartedAt = now,
                lastCollectedAt = now,
                hp = 0,
            )
            state.copy(
                resources = state.resources - config.cost,
                village = state.village.copy(buildings = state.village.buildings + newBuilding),
            )
        }

    fun upgradeBuilding(userId: Long, buildingId: Long): GameState =
        loadAndUpdate(userId) { state ->
            val now = state.lastUpdatedAt
            val building = state.village.buildings.find { it.id == buildingId }
                ?: throw VillageException("Building not found")
            if (building.isUnderConstruction(now)) {
                throw VillageException("Building is under construction")
            }

            val nextLevel = building.level + 1
            val nextConfig = BuildingConfig.configFor(building.type, nextLevel)
                ?: throw VillageException("Building is at max level")

            requireTownHallLevel(state, now, nextConfig.requiredTownHallLevel)
            requireResources(state, nextConfig.cost)

            val updatedBuildings = state.village.buildings.map {
                if (it.id == buildingId) it.copy(level = nextLevel, constructionStartedAt = now, hp = 0)
                else it
            }
            state.copy(
                resources = state.resources - nextConfig.cost,
                village = state.village.copy(buildings = updatedBuildings),
            )
        }

    fun moveBuilding(userId: Long, buildingId: Long, x: Int, y: Int): GameState =
        loadAndUpdate(userId) { state ->
            val now = state.lastUpdatedAt
            val building = state.village.buildings.find { it.id == buildingId }
                ?: throw VillageException("Building not found")
            if (building.isUnderConstruction(now)) {
                throw VillageException("Building is under construction")
            }

            val config = BuildingConfig.configFor(building.type, building.level)
                ?: throw VillageException("Unknown building config")
            requireWithinBounds(x, y, config.width, config.height)
            requireNoCollision(state.village.buildings, x, y, config.width, config.height, excludeId = buildingId)

            val updatedBuildings = state.village.buildings.map {
                if (it.id == buildingId) it.copy(x = x, y = y)
                else it
            }
            state.copy(village = state.village.copy(buildings = updatedBuildings))
        }

    fun collectResources(userId: Long, buildingId: Long): GameState =
        loadAndUpdate(userId) { it }

    fun collectAll(userId: Long): GameState =
        loadAndUpdate(userId) { it }

    fun trainTroops(userId: Long, type: TroopType, count: Int): GameState =
        loadAndUpdate(userId) { state ->
            val now = state.lastUpdatedAt
            val hasBarracks = state.village.buildings.any {
                it.type == BuildingType.Barracks && it.isComplete(now)
            }
            if (!hasBarracks) throw VillageException("No completed Barracks")

            val config = TroopConfig.configFor(type, 1)
                ?: throw VillageException("Unknown troop type")
            val totalCost = config.trainingCost * count.toDouble()
            requireResources(state, totalCost)

            val newEntries = (1..count).map { TrainingQueueEntry(troopType = type) }
            state.copy(
                resources = state.resources - totalCost,
                trainingQueue = TrainingQueue(state.trainingQueue.entries + newEntries),
            )
        }

    fun cancelTraining(userId: Long, index: Int): GameState =
        loadAndUpdate(userId) { state ->
            val entries = state.trainingQueue.entries
            if (index < 0 || index >= entries.size) {
                throw VillageException("Invalid queue index")
            }
            val entry = entries[index]
            if (entry.startedAt != null) {
                throw VillageException("Cannot cancel training in progress")
            }
            val config = TroopConfig.configFor(entry.troopType, entry.level)
            val refund = config?.trainingCost ?: Resources()
            state.copy(
                resources = state.resources + refund,
                trainingQueue = TrainingQueue(entries.filterIndexed { i, _ -> i != index }),
            )
        }

    fun collectEventRewards(userId: Long): GameState =
        loadAndUpdate(userId) { state ->
            val event = state.activeEvent
                ?: throw VillageException("No active event")
            if (!event.completed) throw VillageException("Event still in progress")
            val rewards = event.pendingRewards
                ?: throw VillageException("No rewards to collect")
            state.copy(
                resources = state.resources + rewards,
                activeEvent = null,
            )
        }

    fun rearmTrap(userId: Long, buildingId: Long): GameState =
        loadAndUpdate(userId) { state ->
            val building = state.village.buildings.find { it.id == buildingId }
                ?: throw VillageException("Building not found")
            if (!building.type.isTrap) throw VillageException("Not a trap")
            if (!building.triggered) throw VillageException("Trap is not triggered")
            val config = BuildingConfig.configFor(building.type, building.level)
            val cost = config?.cost?.times(0.5) ?: Resources()
            requireResources(state, cost)
            val updated = state.village.buildings.map {
                if (it.id == buildingId) it.copy(triggered = false) else it
            }
            state.copy(
                resources = state.resources - cost,
                village = state.village.copy(buildings = updated),
            )
        }

    fun rearmAllTraps(userId: Long): GameState =
        loadAndUpdate(userId) { state ->
            val triggeredTraps = state.village.buildings.filter { it.type.isTrap && it.triggered }
            if (triggeredTraps.isEmpty()) throw VillageException("No triggered traps")
            var totalCost = Resources()
            for (trap in triggeredTraps) {
                val config = BuildingConfig.configFor(trap.type, trap.level)
                totalCost = totalCost + (config?.cost?.times(0.5) ?: Resources())
            }
            requireResources(state, totalCost)
            val updated = state.village.buildings.map {
                if (it.type.isTrap && it.triggered) it.copy(triggered = false) else it
            }
            state.copy(
                resources = state.resources - totalCost,
                village = state.village.copy(buildings = updated),
            )
        }

    fun demolishBuilding(userId: Long, buildingId: Long): GameState =
        loadAndUpdate(userId) { state ->
            val building = state.village.buildings.find { it.id == buildingId }
                ?: throw VillageException("Building not found")
            if (building.type == BuildingType.TownHall) {
                throw VillageException("Cannot demolish Town Hall")
            }
            val config = BuildingConfig.configFor(building.type, building.level)
            val refund = config?.cost?.times(0.5) ?: Resources()
            state.copy(
                resources = state.resources + refund,
                village = state.village.copy(buildings = state.village.buildings.filter { it.id != buildingId }),
            )
        }

    fun cancelConstruction(userId: Long, buildingId: Long): GameState =
        loadAndUpdate(userId) { state ->
            val now = state.lastUpdatedAt
            val building = state.village.buildings.find { it.id == buildingId }
                ?: throw VillageException("Building not found")
            if (!building.isUnderConstruction(now)) {
                throw VillageException("Building is not under construction")
            }
            val config = BuildingConfig.configFor(building.type, building.level)
            val refund = config?.cost ?: Resources()
            state.copy(
                resources = state.resources + refund,
                village = state.village.copy(buildings = state.village.buildings.filter { it.id != buildingId }),
            )
        }

    fun speedUpConstruction(userId: Long, buildingId: Long): GameState =
        loadAndUpdate(userId) { state ->
            val now = state.lastUpdatedAt
            val building = state.village.buildings.find { it.id == buildingId }
                ?: throw VillageException("Building not found")
            if (!building.isUnderConstruction(now)) {
                throw VillageException("Building is not under construction")
            }
            val config = BuildingConfig.configFor(building.type, building.level)
                ?: throw VillageException("Unknown building config")
            val completionTime = (building.constructionStartedAt ?: 0) + config.buildTimeSeconds * 1000
            val remainingMs = (completionTime - now).coerceAtLeast(0)
            val manaCost = ((remainingMs / 10_000) + 1).toLong()
            requireResources(state, Resources(mana = manaCost))

            val updatedBuildings = state.village.buildings.map {
                if (it.id == buildingId) it.copy(constructionStartedAt = null, hp = config.hp)
                else it
            }
            state.copy(
                resources = state.resources - Resources(mana = manaCost),
                village = state.village.copy(buildings = updatedBuildings),
            )
        }

    private fun loadAndUpdate(userId: Long, action: (GameState) -> GameState): GameState {
        val now = System.currentTimeMillis()
        val state = VillageRepository.getVillage(userId) ?: createDefaultVillage(userId, now)
        val updated = action(GameStateUpdateUseCase.update(state, now))
        VillageRepository.saveVillage(userId, updated.copy(lastUpdatedAt = now))
        return updated
    }

    private fun requireTownHallLevel(state: GameState, now: Long, required: Int) {
        val townHallLevel = state.village.buildings
            .filter { it.type == BuildingType.TownHall && it.isComplete(now) }
            .maxOfOrNull { it.level } ?: 0
        if (required > townHallLevel) {
            throw VillageException("Town Hall level $required required")
        }
    }

    private fun requireResources(state: GameState, cost: Resources) {
        if (!state.resources.hasEnough(cost)) {
            throw VillageException("Insufficient resources")
        }
    }

    private fun requireWithinBounds(x: Int, y: Int, width: Int, height: Int) {
        if (x < 0 || y < 0 || x + width > GRID_SIZE || y + height > GRID_SIZE) {
            throw VillageException("Position out of bounds")
        }
    }

    private fun requireNoCollision(buildings: List<PlacedBuilding>, x: Int, y: Int, width: Int, height: Int, excludeId: Long?) {
        for (building in buildings) {
            if (building.id == excludeId) continue
            val bConfig = BuildingConfig.configFor(building.type, building.level) ?: continue
            if (x < building.x + bConfig.width && x + width > building.x &&
                y < building.y + bConfig.height && y + height > building.y) {
                throw VillageException("Position overlaps with existing building")
            }
        }
    }

    private fun createDefaultVillage(userId: Long, now: Long): GameState {
        val state = GameState(
            playerId = userId,
            resources = Resources(gold = 1000, wood = 1000, metal = 500),
            village = Village(
                playerId = userId,
                buildings = listOf(
                    PlacedBuilding(id = 1, type = BuildingType.TownHall, level = 1, x = 18, y = 18, hp = 1000, lastCollectedAt = now),
                ),
            ),
            lastUpdatedAt = now,
        )
        VillageRepository.saveVillage(userId, state)
        return state
    }
}

class VillageException(message: String) : RuntimeException(message)
