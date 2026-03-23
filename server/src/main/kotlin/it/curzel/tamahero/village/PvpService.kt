package it.curzel.tamahero.village

import it.curzel.tamahero.db.UserRepository
import it.curzel.tamahero.db.VillageRepository
import it.curzel.tamahero.models.*
import it.curzel.tamahero.notifications.PushNotificationServiceProvider
import it.curzel.tamahero.websocket.ConnectionManager
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PvpService {

    private val logger = LoggerFactory.getLogger(PvpService::class.java)

    private val activeBattles = ConcurrentHashMap<Long, PvpBattle>()
    private val lockedDefenders = ConcurrentHashMap.newKeySet<Long>()

    private const val GRID_SIZE = 40
    private const val EDGE_MARGIN = 2
    private const val MATCHMAKING_TROPHY_RANGE = 200
    private const val NEXT_OPPONENT_COST = 100L

    fun findOpponent(attackerId: Long): ServerMessage {
        if (activeBattles.containsKey(attackerId)) {
            return ServerMessage.Error(reason = "Already in a battle")
        }

        val attackerState = VillageService.getOrCreateVillage(attackerId)

        if (attackerState.army.totalCount == 0) {
            return ServerMessage.Error(reason = "No troops in army")
        }

        val now = System.currentTimeMillis()
        val attackerTrophies = attackerState.trophies

        val allVillages = VillageRepository.getAllVillageUserIds()
        val candidates = allVillages
            .filter { it != attackerId }
            .filter { !lockedDefenders.contains(it) }
            .mapNotNull { userId ->
                val state = VillageRepository.getVillage(userId) ?: return@mapNotNull null
                val user = UserRepository.findById(userId) ?: return@mapNotNull null
                if (state.shieldExpiresAt > now) return@mapNotNull null
                if (kotlin.math.abs(state.trophies - attackerTrophies) > MATCHMAKING_TROPHY_RANGE) return@mapNotNull null
                val thLevel = state.village.buildings
                    .filter { it.type == BuildingType.TownHall && it.constructionStartedAt == null }
                    .maxOfOrNull { it.level } ?: 1
                if (state.village.buildings.size < 3) return@mapNotNull null
                Triple(userId, state, user)
            }

        if (candidates.isEmpty()) {
            return ServerMessage.NoOpponentFound(reason = "No suitable opponents found")
        }

        val (targetId, targetState, targetUser) = candidates.random()
        val updatedTarget = GameStateUpdateUseCase.update(targetState, now)
        val thLevel = updatedTarget.village.buildings
            .filter { it.type == BuildingType.TownHall && it.constructionStartedAt == null }
            .maxOfOrNull { it.level } ?: 1

        val lootAvailable = PvpCalculations.calculateLootAvailable(updatedTarget.resources)

        return ServerMessage.OpponentFound(MatchmakingResult(
            targetId = targetId,
            targetName = targetUser.username,
            targetTrophies = updatedTarget.trophies,
            targetTownHallLevel = thLevel,
            targetBase = updatedTarget.village,
            lootAvailable = lootAvailable,
        ))
    }

    fun startBattle(attackerId: Long, defenderId: Long): ServerMessage {
        if (activeBattles.containsKey(attackerId)) {
            return ServerMessage.Error(reason = "Already in a battle")
        }

        val now = System.currentTimeMillis()

        val attackerState = VillageService.getOrCreateVillage(attackerId)
        val attackerUser = UserRepository.findById(attackerId)
            ?: return ServerMessage.Error(reason = "Player not found")
        val defenderState = VillageService.getOrCreateVillage(defenderId)
        val defenderUser = UserRepository.findById(defenderId)
            ?: return ServerMessage.Error(reason = "Opponent not found")

        if (defenderState.shieldExpiresAt > now) {
            return ServerMessage.Error(reason = "Opponent is shielded")
        }
        if (lockedDefenders.contains(defenderId)) {
            return ServerMessage.Error(reason = "Opponent is already being attacked")
        }
        if (attackerState.army.totalCount == 0) {
            return ServerMessage.Error(reason = "No troops in army")
        }

        lockedDefenders.add(defenderId)

        val updatedDefender = GameStateUpdateUseCase.update(defenderState, now)
        val completedBuildings = updatedDefender.village.buildings
            .filter { it.constructionStartedAt == null }

        val shieldHp = completedBuildings
            .filter { it.type == BuildingType.ShieldDome }
            .sumOf { BuildingConfig.configFor(it.type, it.level)?.shieldHp ?: 0 }

        val lootAvailable = PvpCalculations.calculateLootAvailable(updatedDefender.resources)

        val battle = PvpBattle(
            battleId = UUID.randomUUID().toString(),
            attackerId = attackerId,
            defenderId = defenderId,
            defenderName = defenderUser.username,
            defenderTrophies = updatedDefender.trophies,
            defenderBase = Village(playerId = defenderId, buildings = completedBuildings),
            availableTroops = attackerState.army,
            buildings = completedBuildings,
            battleShieldHp = shieldHp,
            startedAt = now,
            loot = lootAvailable,
        )

        activeBattles[attackerId] = battle

        // Attacking breaks the attacker's shield
        if (attackerState.shieldExpiresAt > now) {
            VillageRepository.saveVillage(attackerId, attackerState.copy(shieldExpiresAt = 0))
        }

        logger.info("PvP battle started: {} vs {} (battle {})", attackerId, defenderId, battle.battleId)

        PushNotificationServiceProvider.instance.notifyUnderAttack(defenderId, attackerUser.username)

        return ServerMessage.PvpBattleStarted(battle)
    }

    fun deployTroop(attackerId: Long, troopType: TroopType, x: Float, y: Float): ServerMessage {
        val battle = activeBattles[attackerId]
            ?: return ServerMessage.Error(reason = "No active battle")
        if (battle.ended) return ServerMessage.Error(reason = "Battle has ended")

        // Validate edge deployment
        val onEdge = x <= EDGE_MARGIN || x >= GRID_SIZE - EDGE_MARGIN ||
                     y <= EDGE_MARGIN || y >= GRID_SIZE - EDGE_MARGIN
        if (!onEdge) {
            return ServerMessage.Error(reason = "Troops must be deployed on the map edges")
        }

        // Check troop availability
        val available = battle.availableTroops.troops.find { it.type == troopType }
        if (available == null || available.count <= 0) {
            return ServerMessage.Error(reason = "No ${troopType.name} available")
        }

        val config = TroopConfig.configFor(troopType, available.level)
            ?: return ServerMessage.Error(reason = "Unknown troop type")

        val troopId = (battle.deployedTroops.maxOfOrNull { it.id } ?: 0) + 1
        val troop = Troop(
            id = troopId,
            type = troopType,
            level = available.level,
            hp = config.hp,
            x = x,
            y = y,
        )

        // Decrement available troops
        val updatedArmy = Army(battle.availableTroops.troops.map {
            if (it.type == troopType && it.level == available.level) {
                it.copy(count = it.count - 1)
            } else it
        }.filter { it.count > 0 })

        val updatedBattle = battle.copy(
            availableTroops = updatedArmy,
            deployedTroops = battle.deployedTroops + troop,
        )
        activeBattles[attackerId] = updatedBattle

        return ServerMessage.PvpBattleTick(updatedBattle)
    }

    fun tickBattle(attackerId: Long, now: Long): PvpBattle? {
        val battle = activeBattles[attackerId] ?: return null
        if (battle.ended) return battle
        if (battle.deployedTroops.isEmpty()) return battle

        // Build a temporary GameState to use BattleUpdateUseCase
        val tempState = GameState(
            playerId = battle.defenderId,
            village = Village(playerId = battle.defenderId, buildings = battle.buildings),
            troops = battle.deployedTroops,
            battleShieldHp = battle.battleShieldHp,
            preBattleBuildings = battle.defenderBase.buildings,
            lastUpdatedAt = now - TICK_MS,
        )

        val updated = BattleUpdateUseCase.update(tempState, now)

        val updatedBattle = battle.copy(
            deployedTroops = updated.troops,
            buildings = updated.village.buildings,
            battleShieldHp = updated.battleShieldHp,
            stars = battle.copy(buildings = updated.village.buildings).currentStars,
        )

        // Check end conditions
        val shouldEnd = updatedBattle.isTimeUp(now) ||
                        (updatedBattle.deployedTroops.isEmpty() && updatedBattle.availableTroops.totalCount == 0) ||
                        updatedBattle.destructionPercent >= 100

        if (shouldEnd) {
            return finalizeBattle(attackerId, updatedBattle, now)
        }

        activeBattles[attackerId] = updatedBattle
        return updatedBattle
    }

    fun endBattle(attackerId: Long): ServerMessage {
        val battle = activeBattles[attackerId]
            ?: return ServerMessage.Error(reason = "No active battle")
        if (battle.ended) return ServerMessage.Error(reason = "Battle already ended")

        val now = System.currentTimeMillis()
        val finalBattle = finalizeBattle(attackerId, battle.copy(surrendered = true), now)
            ?: return ServerMessage.Error(reason = "Failed to end battle")

        val result = buildResult(finalBattle)
        return ServerMessage.PvpBattleEnded(result)
    }

    @Synchronized
    private fun finalizeBattle(attackerId: Long, battle: PvpBattle, now: Long): PvpBattle? {
        if (!activeBattles.containsKey(attackerId)) return null

        val finalBattle = battle.copy(
            ended = true,
            stars = battle.currentStars,
        )

        val result = buildResult(finalBattle)
        applyBattleResults(result, finalBattle, now)

        lockedDefenders.remove(finalBattle.defenderId)
        activeBattles.remove(attackerId)

        logger.info(
            "PvP battle ended: {} vs {} — {} stars, {}% destruction, loot={}g/{}w/{}m",
            result.attackerId, result.defenderId, result.stars, result.destructionPercent,
            result.loot.gold, result.loot.wood, result.loot.metal,
        )

        return finalBattle
    }

    private fun buildResult(battle: PvpBattle): PvpResult {
        val lootStolen = PvpCalculations.calculateLootStolen(battle.loot, battle.destructionPercent)
        val attackerState = VillageRepository.getVillage(battle.attackerId)
        val attackerTrophies = attackerState?.trophies ?: 0
        val trophyDelta = PvpCalculations.calculateTrophyDelta(
            attackerTrophies, battle.defenderTrophies, battle.currentStars,
        )

        return PvpResult(
            battleId = battle.battleId,
            attackerId = battle.attackerId,
            defenderId = battle.defenderId,
            stars = battle.currentStars,
            loot = lootStolen,
            attackerTrophyDelta = trophyDelta,
            defenderTrophyDelta = -trophyDelta,
            destructionPercent = battle.destructionPercent,
        )
    }

    private fun applyBattleResults(result: PvpResult, battle: PvpBattle, now: Long) {
        // Update attacker: add loot, add trophies, remove spent troops
        val attackerState = VillageRepository.getVillage(result.attackerId) ?: return
        val remainingArmy = battle.availableTroops
        val updatedAttacker = attackerState.copy(
            resources = attackerState.resources + result.loot,
            trophies = (attackerState.trophies + result.attackerTrophyDelta).coerceAtLeast(0),
            army = remainingArmy,
        )
        VillageRepository.saveVillage(result.attackerId, updatedAttacker)

        // Update defender: remove loot, remove trophies, apply building damage, grant shield
        val defenderState = VillageRepository.getVillage(result.defenderId) ?: return
        val attackerUser = UserRepository.findById(result.attackerId)
        val shieldDuration = PvpCalculations.calculateShieldDuration(result.destructionPercent)

        // Apply building damage from battle to defender's actual village
        val damagedBuildings = applyBattleDamage(defenderState.village.buildings, battle)

        val logEntry = DefenseLogEntry(
            timestamp = now,
            attackerName = attackerUser?.username ?: "Unknown",
            stars = result.stars,
            lootLost = result.loot,
            trophyDelta = result.defenderTrophyDelta,
        )

        val updatedDefender = defenderState.copy(
            resources = Resources(
                gold = (defenderState.resources.gold - result.loot.gold).coerceAtLeast(0),
                wood = (defenderState.resources.wood - result.loot.wood).coerceAtLeast(0),
                metal = (defenderState.resources.metal - result.loot.metal).coerceAtLeast(0),
                mana = (defenderState.resources.mana - result.loot.mana).coerceAtLeast(0),
            ),
            trophies = (defenderState.trophies + result.defenderTrophyDelta).coerceAtLeast(0),
            village = defenderState.village.copy(buildings = damagedBuildings),
            shieldExpiresAt = if (shieldDuration > 0) now + shieldDuration else defenderState.shieldExpiresAt,
            defenseLog = (listOf(logEntry) + defenderState.defenseLog).take(20),
        )
        VillageRepository.saveVillage(result.defenderId, updatedDefender)

        // Notify defender via WebSocket and push
        kotlinx.coroutines.runBlocking {
            ConnectionManager.sendToPlayer(result.defenderId, ServerMessage.DefenseResult(logEntry))
            ConnectionManager.sendToPlayer(result.defenderId, ServerMessage.GameStateUpdated(updatedDefender))
        }
        PushNotificationServiceProvider.instance.notifyDefenseResult(
            defenderId = result.defenderId,
            attackerName = attackerUser?.username ?: "Unknown",
            stars = result.stars,
            lootGold = result.loot.gold,
            lootWood = result.loot.wood,
            lootMetal = result.loot.metal,
        )
    }

    private fun applyBattleDamage(
        originalBuildings: List<PlacedBuilding>,
        battle: PvpBattle,
    ): List<PlacedBuilding> {
        val battleBuildingMap = battle.buildings.associateBy { it.id }
        val preBattleMap = battle.defenderBase.buildings.associateBy { it.id }

        return originalBuildings.mapNotNull { building ->
            val preBattle = preBattleMap[building.id]
            val afterBattle = battleBuildingMap[building.id]

            if (preBattle == null) {
                // Building was added after battle snapshot (e.g. under construction) — keep as-is
                building
            } else if (afterBattle == null) {
                // Building was destroyed in battle — rebuild defense at 70% chance
                if (building.type.isDefense && !building.type.isTrap) {
                    val hash = (building.id * 31 + battle.startedAt) % 100
                    if (hash < 70) {
                        val config = BuildingConfig.configFor(building.type, building.level)
                        building.copy(hp = config?.hp ?: building.hp)
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                // Building survived — apply damage
                building.copy(hp = afterBattle.hp)
            }
        }
    }

    fun getActiveBattle(attackerId: Long): PvpBattle? = activeBattles[attackerId]

    fun getActiveBattles(): Map<Long, PvpBattle> = activeBattles.toMap()

    fun isInBattle(playerId: Long): Boolean =
        activeBattles.containsKey(playerId) || lockedDefenders.contains(playerId)

    const val TICK_MS = 100L
}
