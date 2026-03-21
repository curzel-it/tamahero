package it.curzel.tamahero.db

import it.curzel.tamahero.models.GameState
import it.curzel.tamahero.models.ProtocolJson

object VillageRepository {

    fun getVillage(userId: Long): GameState? {
        val conn = Database.getConnection()
        conn.prepareStatement("SELECT state_json FROM villages WHERE user_id = ?").use { stmt ->
            stmt.setLong(1, userId)
            val rs = stmt.executeQuery()
            if (!rs.next()) return null
            return ProtocolJson.decodeFromString<GameState>(rs.getString("state_json"))
        }
    }

    fun saveVillage(userId: Long, state: GameState) {
        val conn = Database.getConnection()
        val stateJson = ProtocolJson.encodeToString(GameState.serializer(), state)
        val now = System.currentTimeMillis()
        conn.prepareStatement(
            "INSERT INTO villages (user_id, state_json, updated_at) VALUES (?, ?, ?) ON CONFLICT(user_id) DO UPDATE SET state_json = ?, updated_at = ?"
        ).use { stmt ->
            stmt.setLong(1, userId)
            stmt.setString(2, stateJson)
            stmt.setLong(3, now)
            stmt.setString(4, stateJson)
            stmt.setLong(5, now)
            stmt.executeUpdate()
        }
    }

    fun deleteVillage(userId: Long) {
        val conn = Database.getConnection()
        conn.prepareStatement("DELETE FROM villages WHERE user_id = ?").use { stmt ->
            stmt.setLong(1, userId)
            stmt.executeUpdate()
        }
    }
}
