package it.curzel.tamahero.models

object HeroUpdateUseCase {

    fun update(hero: Hero, now: Long): Hero {
        if (hero.lastUpdatedAt == 0L) return hero.copy(lastUpdatedAt = now)
        val elapsedMs = (now - hero.lastUpdatedAt).coerceAtLeast(0)
        val elapsedHours = elapsedMs / 3_600_000.0

        val hungerDecay = (HeroConfig.HUNGER_DECAY_PER_HOUR * elapsedHours).toInt()
        val happinessDecay = (HeroConfig.HAPPINESS_DECAY_PER_HOUR * elapsedHours).toInt()

        val newHunger = (hero.hunger - hungerDecay).coerceIn(0, 100)
        val newHappiness = (hero.happiness - happinessDecay).coerceIn(0, 100)

        return hero.copy(
            hunger = newHunger,
            happiness = newHappiness,
            lastUpdatedAt = now,
        )
    }

    fun feed(hero: Hero): Hero {
        val newHunger = (hero.hunger + HeroConfig.FEED_HUNGER_RESTORE).coerceAtMost(100)
        return hero.copy(hunger = newHunger, lastFedAt = hero.lastUpdatedAt)
    }

    fun train(hero: Hero): Hero {
        val newHappiness = (hero.happiness + HeroConfig.TRAIN_HAPPINESS_RESTORE).coerceAtMost(100)
        val newXp = hero.xp + HeroConfig.TRAIN_XP_GAIN
        val newLevel = calculateLevel(newXp, hero.level)
        return hero.copy(
            happiness = newHappiness,
            xp = newXp,
            level = newLevel,
            lastTrainedAt = hero.lastUpdatedAt,
        )
    }

    fun addBattleXp(hero: Hero, xp: Long): Hero {
        val newXp = hero.xp + xp
        val newLevel = calculateLevel(newXp, hero.level)
        return hero.copy(xp = newXp, level = newLevel)
    }

    private fun calculateLevel(xp: Long, currentLevel: Int): Int {
        var level = currentLevel
        while (level < HeroConfig.MAX_LEVEL && xp >= HeroConfig.xpForLevel(level + 1)) {
            level++
        }
        return level
    }
}
