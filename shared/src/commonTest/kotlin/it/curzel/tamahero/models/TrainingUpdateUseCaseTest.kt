package it.curzel.tamahero.models

import kotlin.test.*

class TrainingUpdateUseCaseTest {

    private fun baseState(
        queue: TrainingQueue = TrainingQueue(),
        army: Army = Army(),
        hasBarracks: Boolean = true,
        hasArmyCamp: Boolean = true,
    ): GameState {
        val buildings = mutableListOf(
            PlacedBuilding(id = 1, type = BuildingType.CommandCenter, level = 1, x = 0, y = 0, hp = 1000),
        )
        if (hasBarracks) {
            buildings.add(PlacedBuilding(id = 2, type = BuildingType.Barracks, level = 1, x = 5, y = 0, hp = 300))
        }
        if (hasArmyCamp) {
            buildings.add(PlacedBuilding(id = 3, type = BuildingType.Hangar, level = 1, x = 10, y = 0, hp = 200))
        }
        return GameState(
            playerId = 1,
            resources = Resources(credits = 500),
            village = Village(playerId = 1, buildings = buildings),
            trainingQueue = queue,
            army = army,
            lastUpdatedAt = 0,
        )
    }

    @Test
    fun emptyQueueNoChange() {
        val state = baseState()
        val result = TrainingUpdateUseCase.update(state, now = 100_000)
        assertEquals(state.army, result.army)
        assertEquals(state.trainingQueue, result.trainingQueue)
    }

    @Test
    fun startsTrainingFirstInQueue() {
        val queue = TrainingQueue(listOf(
            TrainingQueueEntry(TroopType.Marine),
        ))
        val state = baseState(queue = queue)
        val result = TrainingUpdateUseCase.update(state, now = 1000)
        assertNotNull(result.trainingQueue.entries.first().startedAt)
    }

    @Test
    fun completesTrainingAfterTime() {
        // HumanSoldier takes 20 seconds
        val queue = TrainingQueue(listOf(
            TrainingQueueEntry(TroopType.Marine, startedAt = 0),
        ))
        val state = baseState(queue = queue)
        val result = TrainingUpdateUseCase.update(state, now = 20_000)
        assertTrue(result.trainingQueue.entries.isEmpty())
        assertEquals(1, result.army.totalCount)
        assertEquals(TroopType.Marine, result.army.troops.first().type)
    }

    @Test
    fun doesNotCompleteBeforeTime() {
        val queue = TrainingQueue(listOf(
            TrainingQueueEntry(TroopType.Marine, startedAt = 0),
        ))
        val state = baseState(queue = queue)
        val result = TrainingUpdateUseCase.update(state, now = 10_000)
        assertEquals(1, result.trainingQueue.entries.size)
        assertEquals(0, result.army.totalCount)
    }

    @Test
    fun startsNextAfterCompletion() {
        val queue = TrainingQueue(listOf(
            TrainingQueueEntry(TroopType.Marine, startedAt = 0),
            TrainingQueueEntry(TroopType.Sniper),
        ))
        val state = baseState(queue = queue)
        val result = TrainingUpdateUseCase.update(state, now = 20_000)
        assertEquals(1, result.army.totalCount)
        assertEquals(1, result.trainingQueue.entries.size)
        assertEquals(TroopType.Sniper, result.trainingQueue.entries.first().troopType)
        assertNotNull(result.trainingQueue.entries.first().startedAt)
    }

    @Test
    fun respectsArmyCapacity() {
        // ArmyCamp level 1 = 20 capacity
        val army = Army((1..20).map { ArmyTroop(TroopType.Marine, 1, 1) }.let {
            listOf(ArmyTroop(TroopType.Marine, 1, 20))
        })
        val queue = TrainingQueue(listOf(
            TrainingQueueEntry(TroopType.Marine, startedAt = 0),
        ))
        val state = baseState(queue = queue, army = army)
        val result = TrainingUpdateUseCase.update(state, now = 20_000)
        // Should not complete because army is full
        assertEquals(20, result.army.totalCount)
        assertEquals(1, result.trainingQueue.entries.size)
    }

    @Test
    fun noBarracksNoTraining() {
        val queue = TrainingQueue(listOf(
            TrainingQueueEntry(TroopType.Marine),
        ))
        val state = baseState(queue = queue, hasBarracks = false)
        val result = TrainingUpdateUseCase.update(state, now = 100_000)
        assertNull(result.trainingQueue.entries.first().startedAt)
    }

    @Test
    fun multipleBarracksTrainInParallel() {
        val buildings = listOf(
            PlacedBuilding(id = 1, type = BuildingType.CommandCenter, level = 1, x = 0, y = 0, hp = 1000),
            PlacedBuilding(id = 2, type = BuildingType.Barracks, level = 1, x = 5, y = 0, hp = 300),
            PlacedBuilding(id = 3, type = BuildingType.Barracks, level = 1, x = 8, y = 0, hp = 300),
            PlacedBuilding(id = 4, type = BuildingType.Hangar, level = 1, x = 12, y = 0, hp = 200),
        )
        val queue = TrainingQueue(listOf(
            TrainingQueueEntry(TroopType.Marine),
            TrainingQueueEntry(TroopType.Sniper),
            TrainingQueueEntry(TroopType.Juggernaut),
        ))
        val state = GameState(
            playerId = 1, resources = Resources(),
            village = Village(playerId = 1, buildings = buildings),
            trainingQueue = queue, army = Army(), lastUpdatedAt = 0,
        )
        val result = TrainingUpdateUseCase.update(state, now = 1000)
        // With 2 barracks, 2 entries should have started
        val started = result.trainingQueue.entries.count { it.startedAt != null }
        assertEquals(2, started)
    }

    @Test
    fun addsTroopToExistingArmyEntry() {
        val army = Army(listOf(ArmyTroop(TroopType.Marine, 1, 3)))
        val queue = TrainingQueue(listOf(
            TrainingQueueEntry(TroopType.Marine, startedAt = 0),
        ))
        val state = baseState(queue = queue, army = army)
        val result = TrainingUpdateUseCase.update(state, now = 20_000)
        assertEquals(1, result.army.troops.size)
        assertEquals(4, result.army.troops.first().count)
    }
}
