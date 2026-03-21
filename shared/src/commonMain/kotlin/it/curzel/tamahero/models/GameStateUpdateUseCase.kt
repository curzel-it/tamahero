package it.curzel.tamahero.models

object GameStateUpdateUseCase {

    fun update(state: GameState, now: Long): GameState {
        var current = state

        if (current.troops.isNotEmpty()) {
            current = BattleUpdateUseCase.update(current, now)
        }

        val updatedBuildings = current.village.buildings.map { building ->
            ConstructionUpdateUseCase.update(building, now)
        }
        current = current.copy(village = current.village.copy(buildings = updatedBuildings))

        val (newResources, buildingsAfterProduction) = ResourceProductionUpdateUseCase.update(
            current.resources, current.village.buildings, now
        )
        current = current.copy(
            resources = newResources,
            village = current.village.copy(buildings = buildingsAfterProduction),
            lastUpdatedAt = now,
        )

        return current
    }
}
