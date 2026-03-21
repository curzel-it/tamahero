package it.curzel.tamahero.models

object ConstructionUpdateUseCase {

    fun update(building: PlacedBuilding, now: Long): PlacedBuilding {
        val startedAt = building.constructionStartedAt ?: return building
        val config = BuildingConfig.configFor(building.type, building.level) ?: return building
        val completionTime = startedAt + config.buildTimeSeconds * 1000
        if (now < completionTime) return building
        return building.copy(
            constructionStartedAt = null,
            hp = config.hp,
        )
    }
}
