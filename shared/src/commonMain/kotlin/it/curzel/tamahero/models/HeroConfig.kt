package it.curzel.tamahero.models

object HeroConfig {
    const val MAX_LEVEL = 20
    const val HUNGER_DECAY_PER_HOUR = 5
    const val HAPPINESS_DECAY_PER_HOUR = 3
    const val FEED_COST_GOLD = 50L
    const val TRAIN_COST_MANA = 20L
    const val FEED_HUNGER_RESTORE = 30
    const val TRAIN_HAPPINESS_RESTORE = 25
    const val TRAIN_XP_GAIN = 50L

    fun xpForLevel(level: Int): Long = (level * level * 100).toLong()

    fun bonusDamagePercent(level: Int): Int = level * 5
}
