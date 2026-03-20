package it.curzel.tamahero.db

import it.curzel.tamahero.models.ActionType
import it.curzel.tamahero.models.HeroAction

object ActionRepository {

    fun createAction(heroId: Long, type: ActionType, startedAt: Long, completesAt: Long): HeroAction {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement(
            "INSERT INTO actions (hero_id, type, started_at, completes_at) VALUES (?, ?, ?, ?)",
            java.sql.Statement.RETURN_GENERATED_KEYS
        )
        stmt.use {
            it.setLong(1, heroId)
            it.setString(2, type.name)
            it.setLong(3, startedAt)
            it.setLong(4, completesAt)
            it.executeUpdate()
            val keys = it.generatedKeys
            keys.next()
            val id = keys.getLong(1)
            return HeroAction(id = id, heroId = heroId, type = type, startedAt = startedAt, completesAt = completesAt)
        }
    }

    fun getActiveAction(heroId: Long): HeroAction? {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement(
            "SELECT * FROM actions WHERE hero_id = ? AND completed = 0 ORDER BY started_at DESC LIMIT 1"
        )
        stmt.use {
            it.setLong(1, heroId)
            val rs = it.executeQuery()
            return if (rs.next()) actionFromResultSet(rs) else null
        }
    }

    fun completeAction(actionId: Long) {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement("UPDATE actions SET completed = 1 WHERE id = ?")
        stmt.use {
            it.setLong(1, actionId)
            it.executeUpdate()
        }
    }

    private fun actionFromResultSet(rs: java.sql.ResultSet): HeroAction {
        return HeroAction(
            id = rs.getLong("id"),
            heroId = rs.getLong("hero_id"),
            type = ActionType.valueOf(rs.getString("type")),
            startedAt = rs.getLong("started_at"),
            completesAt = rs.getLong("completes_at"),
            completed = rs.getInt("completed") == 1
        )
    }
}
