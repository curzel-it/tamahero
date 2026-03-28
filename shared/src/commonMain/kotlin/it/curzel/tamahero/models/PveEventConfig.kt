package it.curzel.tamahero.models

object PveEventConfig {

    fun wavesFor(type: EventType): List<EventWave> = when (type) {
        EventType.Quake, EventType.IonStorm -> emptyList()
        EventType.ScoutParty -> listOf(
            EventWave(listOf(
                EventTroop(TroopType.Marine, count = 6),
                EventTroop(TroopType.Sniper, count = 2),
            ))
        )
        EventType.Battle -> listOf(
            EventWave(listOf(
                EventTroop(TroopType.Marine, count = 8),
                EventTroop(TroopType.Sniper, count = 5),
                EventTroop(TroopType.Engineer, count = 2),
                EventTroop(TroopType.Juggernaut, count = 1),
                EventTroop(TroopType.Drone, count = 3),
            ))
        )
        EventType.Raid -> listOf(
            EventWave(listOf(
                EventTroop(TroopType.Marine, count = 12, level = 2),
                EventTroop(TroopType.Sniper, count = 6, level = 2),
                EventTroop(TroopType.Engineer, count = 3),
                EventTroop(TroopType.Drone, count = 8),
            )),
            EventWave(listOf(
                EventTroop(TroopType.Juggernaut, count = 3, level = 2),
                EventTroop(TroopType.Spectre, count = 2),
            ), delayMs = 15_000),
        )
        EventType.Siege -> listOf(
            EventWave(listOf(
                EventTroop(TroopType.Marine, count = 20, level = 3),
                EventTroop(TroopType.Sniper, count = 10, level = 3),
                EventTroop(TroopType.Engineer, count = 5, level = 2),
            )),
            EventWave(listOf(
                EventTroop(TroopType.Juggernaut, count = 5, level = 3),
                EventTroop(TroopType.Spectre, count = 4, level = 2),
                EventTroop(TroopType.Drone, count = 12, level = 3),
            ), delayMs = 20_000),
            EventWave(listOf(
                EventTroop(TroopType.Gunship, count = 2, level = 2),
                EventTroop(TroopType.Juggernaut, count = 3, level = 3),
            ), delayMs = 30_000),
        )
        EventType.Invasion -> listOf(
            EventWave(listOf(
                EventTroop(TroopType.Marine, count = 30, level = 4),
                EventTroop(TroopType.Sniper, count = 15, level = 4),
                EventTroop(TroopType.Engineer, count = 8, level = 3),
            )),
            EventWave(listOf(
                EventTroop(TroopType.Juggernaut, count = 8, level = 4),
                EventTroop(TroopType.Spectre, count = 6, level = 3),
                EventTroop(TroopType.Drone, count = 20, level = 4),
            ), delayMs = 20_000),
            EventWave(listOf(
                EventTroop(TroopType.Gunship, count = 4, level = 3),
                EventTroop(TroopType.Juggernaut, count = 2, level = 5, isBoss = true, hp = 2000, dps = 50),
            ), delayMs = 30_000),
        )
    }

    fun rewardsFor(type: EventType): EventRewards = when (type) {
        EventType.Quake -> EventRewards(success = Resources(credits = 50, metal = 50))
        EventType.IonStorm -> EventRewards(success = Resources(credits = 30, metal = 30))
        EventType.ScoutParty -> EventRewards(success = Resources(credits = 100, metal = 100))
        EventType.Battle -> EventRewards(success = Resources(credits = 200, metal = 200, crystal = 50))
        EventType.Raid -> EventRewards(success = Resources(credits = 500, metal = 500, crystal = 150))
        EventType.Siege -> EventRewards(success = Resources(credits = 1500, metal = 1500, crystal = 500, deuterium = 100))
        EventType.Invasion -> EventRewards(success = Resources(credits = 5000, metal = 5000, crystal = 2000, deuterium = 500))
    }

    fun disasterDamagePercent(type: EventType): IntRange = when (type) {
        EventType.Quake -> 20..50
        EventType.IonStorm -> 15..40
        else -> 0..0
    }

    fun eligibleEvents(townHallLevel: Int): List<EventType> =
        EventType.entries.filter { it.requiredTownHallLevel <= townHallLevel }

    const val MIN_EVENT_INTERVAL_MS = 3_600_000L
    const val MAX_EVENT_INTERVAL_MS = 4 * 3_600_000L
}
