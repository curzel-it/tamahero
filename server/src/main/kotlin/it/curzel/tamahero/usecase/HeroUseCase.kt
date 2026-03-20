package it.curzel.tamahero.usecase

import it.curzel.tamahero.db.ActionRepository
import it.curzel.tamahero.db.HeroRepository
import it.curzel.tamahero.models.Hero

sealed class HeroResult {
    data class Success(val hero: Hero) : HeroResult()
    data class Error(val message: String) : HeroResult()
}

object HeroUseCase {

    fun createHero(userId: Long, name: String): HeroResult {
        if (name.isBlank()) return HeroResult.Error("Hero name cannot be blank")
        val hero = HeroRepository.createHero(userId, name, System.currentTimeMillis())
        return HeroResult.Success(hero)
    }

    fun getHero(heroId: Long, userId: Long): HeroResult {
        val hero = HeroRepository.getHero(heroId) ?: return HeroResult.Error("Hero not found")
        if (hero.userId != userId) return HeroResult.Error("Hero not found")
        val action = ActionRepository.getActiveAction(heroId)
        return HeroResult.Success(hero.copy(currentAction = action))
    }

    fun getHeroesByUser(userId: Long): List<Hero> {
        return HeroRepository.getHeroesByUser(userId).map { hero ->
            val action = ActionRepository.getActiveAction(hero.id)
            hero.copy(currentAction = action)
        }
    }
}
