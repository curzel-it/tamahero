package it.curzel.tamahero.models

object ResourceProductionUpdateUseCase {

    fun update(
        resources: Resources,
        buildings: List<PlacedBuilding>,
        now: Long,
    ): Pair<Resources, List<PlacedBuilding>> {
        val storageCapacity = totalStorageCapacity(buildings, now)
        var produced = Resources()
        val updatedBuildings = buildings.map { building ->
            if (!building.type.isProducer) return@map building
            if (building.isUnderConstruction(now)) return@map building
            val config = BuildingConfig.configFor(building.type, building.level) ?: return@map building
            val elapsedMs = now - building.lastCollectedAt
            if (elapsedMs <= 0) return@map building
            val elapsedHours = elapsedMs / 3_600_000.0
            val amount = Resources(
                credits = (config.productionPerHour.credits * elapsedHours).toLong(),
                alloy = (config.productionPerHour.alloy * elapsedHours).toLong(),
                crystal = (config.productionPerHour.crystal * elapsedHours).toLong(),
                plasma = (config.productionPerHour.plasma * elapsedHours).toLong(),
            )
            produced = produced + amount
            building.copy(lastCollectedAt = now)
        }
        val newResources = (resources + produced).capAt(storageCapacity)
        return Pair(newResources, updatedBuildings)
    }

    private fun totalStorageCapacity(buildings: List<PlacedBuilding>, now: Long): Resources {
        var capacity = Resources()
        for (building in buildings) {
            if (building.isUnderConstruction(now)) continue
            val config = BuildingConfig.configFor(building.type, building.level) ?: continue
            capacity = capacity + config.storageCapacity
        }
        return capacity
    }
}
