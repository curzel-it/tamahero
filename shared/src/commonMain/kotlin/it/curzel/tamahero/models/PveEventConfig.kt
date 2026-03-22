package it.curzel.tamahero.models

object PveEventConfig {

    fun wavesFor(type: EventType): List<EventWave> = when (type) {
        EventType.Earthquake, EventType.Storm -> emptyList()
        EventType.ScoutParty -> listOf(
            EventWave(listOf(
                EventTroop(TroopType.HumanSoldier, count = 6),
                EventTroop(TroopType.ElfArcher, count = 2),
            ))
        )
        EventType.Battle -> listOf(
            EventWave(listOf(
                EventTroop(TroopType.HumanSoldier, count = 8),
                EventTroop(TroopType.ElfArcher, count = 5),
                EventTroop(TroopType.DwarfSapper, count = 2),
                EventTroop(TroopType.OrcBerserker, count = 1),
                EventTroop(TroopType.Goblin, count = 3),
            ))
        )
    }

    fun rewardsFor(type: EventType): EventRewards = when (type) {
        EventType.Earthquake -> EventRewards(success = Resources(gold = 50, wood = 50))
        EventType.Storm -> EventRewards(success = Resources(gold = 30, wood = 30))
        EventType.ScoutParty -> EventRewards(success = Resources(gold = 100, wood = 100))
        EventType.Battle -> EventRewards(success = Resources(gold = 200, wood = 200, metal = 50))
    }

    fun disasterDamagePercent(type: EventType): IntRange = when (type) {
        EventType.Earthquake -> 20..50
        EventType.Storm -> 15..40
        else -> 0..0
    }

    fun eligibleEvents(townHallLevel: Int): List<EventType> =
        EventType.entries.filter { it.requiredTownHallLevel <= townHallLevel }

    const val MIN_EVENT_INTERVAL_MS = 3_600_000L
    const val MAX_EVENT_INTERVAL_MS = 4 * 3_600_000L
}
