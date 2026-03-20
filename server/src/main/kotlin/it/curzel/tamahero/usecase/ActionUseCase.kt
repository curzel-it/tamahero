package it.curzel.tamahero.usecase

import it.curzel.tamahero.db.ActionRepository
import it.curzel.tamahero.db.HeroRepository
import it.curzel.tamahero.models.ActionType
import it.curzel.tamahero.models.Hero
import it.curzel.tamahero.models.HeroAction
import it.curzel.tamahero.models.HeroStats

sealed class ActionResult {
    data class Started(val action: HeroAction) : ActionResult()
    data class InProgress(val action: HeroAction) : ActionResult()
    data class Completed(val hero: Hero, val action: HeroAction) : ActionResult()
    data object Idle : ActionResult()
    data class Error(val message: String) : ActionResult()
}

object ActionUseCase {

    fun startAction(heroId: Long, userId: Long, actionType: ActionType): ActionResult {
        val hero = HeroRepository.getHero(heroId) ?: return ActionResult.Error("Hero not found")
        if (hero.userId != userId) return ActionResult.Error("Hero not found")

        val existing = ActionRepository.getActiveAction(heroId)
        if (existing != null) return ActionResult.Error("Hero already has an active action")

        val now = System.currentTimeMillis()
        val duration = getActionDuration(actionType)
        val action = ActionRepository.createAction(heroId, actionType, now, now + duration)
        return ActionResult.Started(action)
    }

    fun resolveAction(heroId: Long, userId: Long): ActionResult {
        val hero = HeroRepository.getHero(heroId) ?: return ActionResult.Error("Hero not found")
        if (hero.userId != userId) return ActionResult.Error("Hero not found")

        val action = ActionRepository.getActiveAction(heroId) ?: return ActionResult.Idle
        val now = System.currentTimeMillis()

        if (now < action.completesAt) {
            return ActionResult.InProgress(action)
        }

        val updatedHero = applyActionRewards(hero, action)
        HeroRepository.updateHero(updatedHero)
        ActionRepository.completeAction(action.id)
        return ActionResult.Completed(updatedHero, action.copy(completed = true))
    }

    fun getActionDuration(actionType: ActionType): Long = when (actionType) {
        ActionType.TRAIN_STRENGTH -> 5 * 60 * 1000L
        ActionType.TRAIN_AGILITY -> 5 * 60 * 1000L
        ActionType.TRAIN_INTELLIGENCE -> 5 * 60 * 1000L
        ActionType.TRAIN_ENDURANCE -> 5 * 60 * 1000L
        ActionType.REST -> 10 * 60 * 1000L
        ActionType.WORK -> 30 * 60 * 1000L
    }

    fun applyActionRewards(hero: Hero, action: HeroAction): Hero {
        val xpGain = when (action.type) {
            ActionType.TRAIN_STRENGTH, ActionType.TRAIN_AGILITY,
            ActionType.TRAIN_INTELLIGENCE, ActionType.TRAIN_ENDURANCE -> 25L
            ActionType.WORK -> 10L
            ActionType.REST -> 5L
        }
        val newStats = when (action.type) {
            ActionType.TRAIN_STRENGTH -> hero.stats.copy(strength = hero.stats.strength + 1)
            ActionType.TRAIN_AGILITY -> hero.stats.copy(agility = hero.stats.agility + 1)
            ActionType.TRAIN_INTELLIGENCE -> hero.stats.copy(intelligence = hero.stats.intelligence + 1)
            ActionType.TRAIN_ENDURANCE -> hero.stats.copy(endurance = hero.stats.endurance + 1)
            else -> hero.stats
        }
        val newExperience = hero.experience + xpGain
        val newLevel = calculateLevel(newExperience)
        return hero.copy(stats = newStats, experience = newExperience, level = newLevel)
    }

    private fun calculateLevel(experience: Long): Int {
        return (experience / 100).toInt() + 1
    }
}
