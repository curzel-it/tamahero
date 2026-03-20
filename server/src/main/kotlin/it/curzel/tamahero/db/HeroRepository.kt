package it.curzel.tamahero.db

import it.curzel.tamahero.models.Hero
import it.curzel.tamahero.models.HeroStats

object HeroRepository {

    fun createHero(userId: Long, name: String, createdAt: Long): Hero {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement(
            "INSERT INTO heroes (user_id, name, created_at) VALUES (?, ?, ?)",
            java.sql.Statement.RETURN_GENERATED_KEYS
        )
        stmt.use {
            it.setLong(1, userId)
            it.setString(2, name)
            it.setLong(3, createdAt)
            it.executeUpdate()
            val keys = it.generatedKeys
            keys.next()
            val id = keys.getLong(1)
            return Hero(id = id, userId = userId, name = name, createdAt = createdAt)
        }
    }

    fun getHero(heroId: Long): Hero? {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement("SELECT * FROM heroes WHERE id = ?")
        stmt.use {
            it.setLong(1, heroId)
            val rs = it.executeQuery()
            return if (rs.next()) heroFromResultSet(rs) else null
        }
    }

    fun getHeroesByUser(userId: Long): List<Hero> {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement("SELECT * FROM heroes WHERE user_id = ?")
        stmt.use {
            it.setLong(1, userId)
            val rs = it.executeQuery()
            val heroes = mutableListOf<Hero>()
            while (rs.next()) {
                heroes.add(heroFromResultSet(rs))
            }
            return heroes
        }
    }

    fun updateHero(hero: Hero) {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement(
            """
            UPDATE heroes SET level = ?, experience = ?,
                strength = ?, agility = ?, intelligence = ?, endurance = ?
            WHERE id = ?
            """
        )
        stmt.use {
            it.setInt(1, hero.level)
            it.setLong(2, hero.experience)
            it.setInt(3, hero.stats.strength)
            it.setInt(4, hero.stats.agility)
            it.setInt(5, hero.stats.intelligence)
            it.setInt(6, hero.stats.endurance)
            it.setLong(7, hero.id)
            it.executeUpdate()
        }
    }

    private fun heroFromResultSet(rs: java.sql.ResultSet): Hero {
        return Hero(
            id = rs.getLong("id"),
            userId = rs.getLong("user_id"),
            name = rs.getString("name"),
            level = rs.getInt("level"),
            experience = rs.getLong("experience"),
            stats = HeroStats(
                strength = rs.getInt("strength"),
                agility = rs.getInt("agility"),
                intelligence = rs.getInt("intelligence"),
                endurance = rs.getInt("endurance")
            ),
            createdAt = rs.getLong("created_at")
        )
    }
}
