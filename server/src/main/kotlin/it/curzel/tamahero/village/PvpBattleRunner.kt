package it.curzel.tamahero.village

import it.curzel.tamahero.models.PvpCalculations
import it.curzel.tamahero.models.PvpResult
import it.curzel.tamahero.models.ServerMessage
import it.curzel.tamahero.websocket.ConnectionManager
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

object PvpBattleRunner {

    private val logger = LoggerFactory.getLogger(PvpBattleRunner::class.java)
    private var job: Job? = null

    private const val TICK_INTERVAL_MS = 100L

    fun start() {
        job = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            logger.info("PvpBattleRunner started")
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                tickAllBattles()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun tickAllBattles() {
        val now = System.currentTimeMillis()
        val battles = PvpService.getActiveBattles()

        for ((attackerId, battle) in battles) {
            if (battle.ended) continue
            // Don't tick if no troops are deployed yet and attacker still has troops to place
            if (battle.deployedTroops.isEmpty() && battle.availableTroops.totalCount > 0) {
                // But check time limit even during deployment phase
                if (battle.isTimeUp(now)) {
                    try {
                        val updated = PvpService.tickBattle(attackerId, now) ?: continue
                        if (updated.ended) {
                            val result = buildEndResult(updated)
                            ConnectionManager.sendToPlayer(attackerId, ServerMessage.PvpBattleEnded(result))
                        }
                    } catch (e: Exception) {
                        logger.error("Error ending timed-out battle for attacker {}", attackerId, e)
                    }
                }
                continue
            }

            try {
                val updated = PvpService.tickBattle(attackerId, now) ?: continue

                if (updated.ended) {
                    val result = buildEndResult(updated)
                    ConnectionManager.sendToPlayer(attackerId, ServerMessage.PvpBattleEnded(result))
                } else {
                    ConnectionManager.sendToPlayer(attackerId, ServerMessage.PvpBattleTick(updated))
                }
            } catch (e: Exception) {
                logger.error("Error ticking battle for attacker {}", attackerId, e)
            }
        }
    }

    private fun buildEndResult(battle: it.curzel.tamahero.models.PvpBattle): PvpResult {
        val lootStolen = PvpCalculations.calculateLootStolen(battle.loot, battle.destructionPercent)
        return PvpResult(
            battleId = battle.battleId,
            attackerId = battle.attackerId,
            defenderId = battle.defenderId,
            stars = battle.currentStars,
            loot = lootStolen,
            attackerTrophyDelta = 0,
            defenderTrophyDelta = 0,
            destructionPercent = battle.destructionPercent,
        )
    }
}
