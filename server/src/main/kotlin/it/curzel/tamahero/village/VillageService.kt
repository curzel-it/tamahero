package it.curzel.tamahero.village

import it.curzel.tamahero.db.VillageRepository
import it.curzel.tamahero.models.*

object VillageService {

    private const val GRID_SIZE = 40

    fun getOrCreateVillage(userId: Long): GameState {
        val now = System.currentTimeMillis()
        val state = VillageRepository.getVillage(userId) ?: createDefaultVillage(userId, now)
        val updated = GameStateUpdateUseCase.update(state, now)
        VillageRepository.saveVillage(userId, updated)
        return updated
    }

    fun placeBuild(userId: Long, type: BuildingType, x: Int, y: Int): GameState {
        val now = System.currentTimeMillis()
        val state = GameStateUpdateUseCase.update(
            VillageRepository.getVillage(userId) ?: createDefaultVillage(userId, now), now
        )
        val config = BuildingConfig.configFor(type, 1)
            ?: throw VillageException("Unknown building type")

        val townHallLevel = state.village.buildings
            .filter { it.type == BuildingType.TownHall && it.isComplete(now) }
            .maxOfOrNull { it.level } ?: 0
        if (config.requiredTownHallLevel > townHallLevel) {
            throw VillageException("Town Hall level ${config.requiredTownHallLevel} required")
        }
        if (!state.resources.hasEnough(config.cost)) {
            throw VillageException("Insufficient resources")
        }
        if (!isWithinBounds(x, y, config.size)) {
            throw VillageException("Position out of bounds")
        }
        if (hasCollision(state.village.buildings, x, y, config.size, excludeId = null)) {
            throw VillageException("Position overlaps with existing building")
        }

        val newId = (state.village.buildings.maxOfOrNull { it.id } ?: 0) + 1
        val newBuilding = PlacedBuilding(
            id = newId, type = type, level = 1,
            x = x, y = y,
            constructionStartedAt = now,
            lastCollectedAt = now,
            hp = 0,
        )
        val updated = state.copy(
            resources = state.resources - config.cost,
            village = state.village.copy(buildings = state.village.buildings + newBuilding),
            lastUpdatedAt = now,
        )
        VillageRepository.saveVillage(userId, updated)
        return updated
    }

    fun upgradeBuilding(userId: Long, buildingId: Long): GameState {
        val now = System.currentTimeMillis()
        val state = GameStateUpdateUseCase.update(
            VillageRepository.getVillage(userId) ?: throw VillageException("No village found"), now
        )
        val building = state.village.buildings.find { it.id == buildingId }
            ?: throw VillageException("Building not found")
        if (building.isUnderConstruction(now)) {
            throw VillageException("Building is under construction")
        }

        val nextLevel = building.level + 1
        val nextConfig = BuildingConfig.configFor(building.type, nextLevel)
            ?: throw VillageException("Building is at max level")

        val townHallLevel = state.village.buildings
            .filter { it.type == BuildingType.TownHall && it.isComplete(now) }
            .maxOfOrNull { it.level } ?: 0
        if (nextConfig.requiredTownHallLevel > townHallLevel) {
            throw VillageException("Town Hall level ${nextConfig.requiredTownHallLevel} required")
        }
        if (!state.resources.hasEnough(nextConfig.cost)) {
            throw VillageException("Insufficient resources")
        }

        val updatedBuildings = state.village.buildings.map {
            if (it.id == buildingId) it.copy(level = nextLevel, constructionStartedAt = now, hp = 0)
            else it
        }
        val updated = state.copy(
            resources = state.resources - nextConfig.cost,
            village = state.village.copy(buildings = updatedBuildings),
            lastUpdatedAt = now,
        )
        VillageRepository.saveVillage(userId, updated)
        return updated
    }

    fun moveBuilding(userId: Long, buildingId: Long, x: Int, y: Int): GameState {
        val now = System.currentTimeMillis()
        val state = GameStateUpdateUseCase.update(
            VillageRepository.getVillage(userId) ?: throw VillageException("No village found"), now
        )
        val building = state.village.buildings.find { it.id == buildingId }
            ?: throw VillageException("Building not found")
        if (building.isUnderConstruction(now)) {
            throw VillageException("Building is under construction")
        }

        val config = BuildingConfig.configFor(building.type, building.level)
            ?: throw VillageException("Unknown building config")
        if (!isWithinBounds(x, y, config.size)) {
            throw VillageException("Position out of bounds")
        }
        if (hasCollision(state.village.buildings, x, y, config.size, excludeId = buildingId)) {
            throw VillageException("Position overlaps with existing building")
        }

        val updatedBuildings = state.village.buildings.map {
            if (it.id == buildingId) it.copy(x = x, y = y)
            else it
        }
        val updated = state.copy(
            village = state.village.copy(buildings = updatedBuildings),
            lastUpdatedAt = now,
        )
        VillageRepository.saveVillage(userId, updated)
        return updated
    }

    fun collectResources(userId: Long, buildingId: Long): GameState {
        val now = System.currentTimeMillis()
        val state = GameStateUpdateUseCase.update(
            VillageRepository.getVillage(userId) ?: throw VillageException("No village found"), now
        )
        VillageRepository.saveVillage(userId, state)
        return state
    }

    private fun createDefaultVillage(userId: Long, now: Long): GameState {
        val state = GameState(
            playerId = userId,
            resources = Resources(gold = 500, wood = 500),
            village = Village(
                playerId = userId,
                buildings = listOf(
                    PlacedBuilding(id = 1, type = BuildingType.TownHall, level = 1, x = 19, y = 19, hp = 1000, lastCollectedAt = now),
                    PlacedBuilding(id = 2, type = BuildingType.GoldStorage, level = 1, x = 16, y = 19, hp = 200, lastCollectedAt = now),
                    PlacedBuilding(id = 3, type = BuildingType.WoodStorage, level = 1, x = 22, y = 19, hp = 200, lastCollectedAt = now),
                ),
            ),
            lastUpdatedAt = now,
        )
        VillageRepository.saveVillage(userId, state)
        return state
    }

    private fun isWithinBounds(x: Int, y: Int, size: Int): Boolean =
        x >= 0 && y >= 0 && x + size <= GRID_SIZE && y + size <= GRID_SIZE

    private fun hasCollision(
        buildings: List<PlacedBuilding>,
        x: Int, y: Int, size: Int,
        excludeId: Long?,
    ): Boolean {
        for (building in buildings) {
            if (building.id == excludeId) continue
            val bConfig = BuildingConfig.configFor(building.type, building.level) ?: continue
            val bSize = bConfig.size
            if (x < building.x + bSize && x + size > building.x &&
                y < building.y + bSize && y + size > building.y) {
                return true
            }
        }
        return false
    }
}

class VillageException(message: String) : RuntimeException(message)
