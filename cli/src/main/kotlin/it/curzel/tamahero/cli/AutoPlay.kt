package it.curzel.tamahero.cli

import it.curzel.tamahero.models.*
import kotlinx.coroutines.delay

class AutoPlay(private val client: CliClient) {

    private var nextX = 5
    private var nextY = 5

    suspend fun run() {
        println("[AutoPlay] Starting autonomous play...")
        client.sendAndReceive(ClientMessage.GetVillage)
        delay(2000)

        while (true) {
            try {
                tick()
            } catch (e: Exception) {
                println("[AutoPlay] Error: ${e.message}")
            }
            delay(15_000)
        }
    }

    private suspend fun tick() {
        client.sendAndReceive(ClientMessage.CollectAll)
        delay(1000)

        client.sendAndReceive(ClientMessage.GetVillage)
        delay(1000)

        val state = client.getLastState() ?: run {
            println("[AutoPlay] No state available, skipping tick")
            return
        }

        if (state.activeEvent?.completed == true && state.activeEvent?.pendingRewards != null) {
            println("[AutoPlay] Collecting event rewards")
            client.sendAndReceive(ClientMessage.CollectEventRewards)
            delay(1000)
        }

        val buildings = state.village.buildings
        val isBuilding = buildings.any { it.constructionStartedAt != null }
        if (isBuilding) {
            println("[AutoPlay] Waiting for construction to finish...")
            return
        }

        updateGrid(buildings)

        val townHall = buildings.first { it.type == BuildingType.CommandCenter }
        val thLevel = townHall.level
        val resources = state.resources

        val action = pickAction(buildings, thLevel, resources, state)
        if (action != null) {
            println("[AutoPlay] $action")
            client.sendAndReceive(action.message)
        } else {
            println("[AutoPlay] Nothing to do, waiting for resources...")
        }
    }

    private fun updateGrid(buildings: List<PlacedBuilding>) {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        for (b in buildings) {
            val config = BuildingConfig.configFor(b.type, b.level)
            val w = config?.width ?: 2
            val h = config?.height ?: 2
            for (dx in 0 until w) {
                for (dy in 0 until h) {
                    occupied.add(Pair(b.x + dx, b.y + dy))
                }
            }
        }
        while (isOccupied(nextX, nextY, 3, occupied)) {
            nextX += 3
            if (nextX > 30) {
                nextX = 0
                nextY += 3
            }
        }
    }

    private fun isOccupied(x: Int, y: Int, size: Int, occupied: Set<Pair<Int, Int>>): Boolean {
        for (dx in 0 until size) {
            for (dy in 0 until size) {
                if (Pair(x + dx, y + dy) in occupied) return true
            }
        }
        return false
    }

    private fun allocatePosition(width: Int, height: Int, buildings: List<PlacedBuilding>): Pair<Int, Int> {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        for (b in buildings) {
            val config = BuildingConfig.configFor(b.type, b.level)
            val bw = config?.width ?: 2
            val bh = config?.height ?: 2
            for (dx in 0 until bw) {
                for (dy in 0 until bh) {
                    occupied.add(Pair(b.x + dx, b.y + dy))
                }
            }
        }
        // Spiral outward from CommandCenter (18,18) to cluster buildings nicely
        for (radius in 1..20) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    if (kotlin.math.abs(dx) != radius && kotlin.math.abs(dy) != radius) continue
                    val x = 18 + dx * 3
                    val y = 18 + dy * 3
                    if (x < 0 || y < 0 || x + width > 40 || y + height > 40) continue
                    if (!isOccupied(x, y, maxOf(width, height), occupied)) return Pair(x, y)
                }
            }
        }
        return Pair(5, 5)
    }

    private fun pickAction(
        buildings: List<PlacedBuilding>,
        thLevel: Int,
        resources: Resources,
        state: GameState
    ): Action? {
        fun count(type: BuildingType) = buildings.count { it.type == type }
        fun canAfford(cost: Resources) = resources.credits >= cost.credits && resources.alloy >= cost.alloy && resources.crystal >= cost.crystal

        val buildOrder = listOf(
            // Phase 1: basic economy
            BuildRequest(BuildingType.CreditVault, maxCount = 1),
            BuildRequest(BuildingType.AlloySilo, maxCount = 1),
            BuildRequest(BuildingType.AlloyRefinery, maxCount = 1),
            BuildRequest(BuildingType.CreditMint, maxCount = 1),
            BuildRequest(BuildingType.AlloyRefinery, maxCount = 2),
            BuildRequest(BuildingType.CreditMint, maxCount = 2),
            // Phase 2: military
            BuildRequest(BuildingType.Academy, maxCount = 1),
            BuildRequest(BuildingType.Hangar, maxCount = 1),
            // Phase 3: more economy
            BuildRequest(BuildingType.AlloyRefinery, maxCount = 3),
            BuildRequest(BuildingType.CreditMint, maxCount = 3),
            BuildRequest(BuildingType.CreditVault, maxCount = 2),
            BuildRequest(BuildingType.AlloySilo, maxCount = 2),
            // Phase 4: defenses
            BuildRequest(BuildingType.RailGun, maxCount = 1),
            BuildRequest(BuildingType.LaserTurret, maxCount = 1),
            BuildRequest(BuildingType.RailGun, maxCount = 2),
            BuildRequest(BuildingType.LaserTurret, maxCount = 2),
            BuildRequest(BuildingType.MissileBattery, maxCount = 1),
        )

        // Try to build next thing in build order
        for (request in buildOrder) {
            val config = BuildingConfig.configFor(request.type, 1) ?: continue
            if (config.requiredTownHallLevel > thLevel) continue
            if (count(request.type) >= request.maxCount) continue
            if (!canAfford(config.cost)) continue
            val (x, y) = allocatePosition(config.width, config.height, buildings)
            return Action("Building ${request.type} at ($x, $y)", ClientMessage.Build(request.type, x, y))
        }

        // Try to upgrade existing buildings (producers first, then storage, then defenses)
        val upgradeOrder = buildings
            .filter { it.constructionStartedAt == null && it.type != BuildingType.CommandCenter }
            .sortedBy { b ->
                when {
                    b.type.isProducer -> 0
                    b.type.isStorage -> 1
                    b.type.isDefense -> 2
                    else -> 3
                }
            }

        for (building in upgradeOrder) {
            val nextConfig = BuildingConfig.configFor(building.type, building.level + 1) ?: continue
            if (nextConfig.requiredTownHallLevel > thLevel) continue
            if (!canAfford(nextConfig.cost)) continue
            return Action(
                "Upgrading ${building.type} (id=${building.id}) to level ${building.level + 1}",
                ClientMessage.Upgrade(building.id)
            )
        }

        // Try to train troops
        val hasBarracks = buildings.any { it.type == BuildingType.Academy && it.constructionStartedAt == null }
        val armyCapacity = buildings
            .filter { it.type == BuildingType.Hangar && it.constructionStartedAt == null }
            .sumOf { BuildingConfig.configFor(it.type, it.level)?.troopCapacity ?: 0 }
        val currentTroops = state.army.totalCount + state.trainingQueue.entries.size

        if (hasBarracks && currentTroops < armyCapacity) {
            val troopType = TroopType.Marine
            val config = TroopConfig.configFor(troopType, 1)
            if (config != null && canAfford(config.trainingCost)) {
                return Action("Training $troopType", ClientMessage.Train(troopType, 1))
            }
        }

        // Try to upgrade TownHall last
        val townHall = buildings.first { it.type == BuildingType.CommandCenter }
        val thNextConfig = BuildingConfig.configFor(BuildingType.CommandCenter, townHall.level + 1)
        if (thNextConfig != null && canAfford(thNextConfig.cost)) {
            return Action(
                "Upgrading CommandCenter to level ${townHall.level + 1}",
                ClientMessage.Upgrade(townHall.id)
            )
        }

        return null
    }

    private data class BuildRequest(val type: BuildingType, val maxCount: Int)
    private data class Action(val description: String, val message: ClientMessage)
}
