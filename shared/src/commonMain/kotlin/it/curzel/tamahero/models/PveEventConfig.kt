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
    }

    fun rewardsFor(type: EventType): EventRewards = when (type) {
        EventType.Quake -> EventRewards(success = Resources(credits = 50, alloy = 50))
        EventType.IonStorm -> EventRewards(success = Resources(credits = 30, alloy = 30))
        EventType.ScoutParty -> EventRewards(success = Resources(credits = 100, alloy = 100))
        EventType.Battle -> EventRewards(success = Resources(credits = 200, alloy = 200, crystal = 50))
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
