package it.curzel.tamahero.village

import it.curzel.tamahero.db.UserRepository
import it.curzel.tamahero.db.VillageRepository
import it.curzel.tamahero.models.*

object LeaderboardService {

    private const val LEADERBOARD_SIZE = 50

    fun getLeaderboard(requesterId: Long): ServerMessage {
        val allIds = VillageRepository.getAllVillageUserIds()

        val ranked = allIds.mapNotNull { userId ->
            val state = VillageRepository.getVillage(userId) ?: return@mapNotNull null
            val user = UserRepository.findById(userId) ?: return@mapNotNull null
            val thLevel = state.village.buildings
                .filter { it.type == BuildingType.TownHall && it.constructionStartedAt == null }
                .maxOfOrNull { it.level } ?: 1
            LeaderboardEntry(
                rank = 0,
                playerId = userId,
                playerName = user.username,
                trophies = state.trophies,
                townHallLevel = thLevel,
            )
        }.sortedByDescending { it.trophies }

        val yourRank = ranked.indexOfFirst { it.playerId == requesterId } + 1

        val entries = ranked.take(LEADERBOARD_SIZE).mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }

        return ServerMessage.Leaderboard(entries = entries, yourRank = yourRank)
    }
}
