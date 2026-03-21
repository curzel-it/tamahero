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
        EventType.RaidingParty -> listOf(
            EventWave(listOf(
                EventTroop(TroopType.HumanSoldier, count = 8),
                EventTroop(TroopType.ElfArcher, count = 5),
                EventTroop(TroopType.DwarfSapper, count = 2),
            ))
        )
        EventType.DragonRaid -> listOf(
            EventWave(listOf(
                EventTroop(TroopType.OrcBerserker, count = 1, hp = 500, dps = 30, isBoss = true),
            ))
        )
        EventType.FullInvasion -> listOf(
            EventWave(listOf(
                EventTroop(TroopType.HumanSoldier, count = 15),
                EventTroop(TroopType.ElfArcher, count = 8),
                EventTroop(TroopType.DwarfSapper, count = 5),
                EventTroop(TroopType.OrcBerserker, count = 3),
                EventTroop(TroopType.OrcBerserker, count = 1, hp = 200, dps = 25, isBoss = true),
            ))
        )
        EventType.Siege -> listOf(
            EventWave(listOf(
                EventTroop(TroopType.HumanSoldier, count = 8),
                EventTroop(TroopType.ElfArcher, count = 4),
            )),
            EventWave(listOf(
                EventTroop(TroopType.HumanSoldier, count = 10),
                EventTroop(TroopType.ElfArcher, count = 6),
                EventTroop(TroopType.DwarfSapper, count = 3),
            ), delayMs = 5_000),
            EventWave(listOf(
                EventTroop(TroopType.HumanSoldier, count = 12),
                EventTroop(TroopType.DwarfSapper, count = 5),
                EventTroop(TroopType.OrcBerserker, count = 4),
                EventTroop(TroopType.OrcBerserker, count = 1, hp = 300, dps = 30, isBoss = true),
            ), delayMs = 5_000),
        )
    }

    fun rewardsFor(type: EventType): EventRewards = when (type) {
        EventType.Earthquake -> EventRewards(success = Resources(gold = 50, wood = 50))
        EventType.Storm -> EventRewards(success = Resources(gold = 30, wood = 30))
        EventType.ScoutParty -> EventRewards(success = Resources(gold = 100, wood = 100))
        EventType.RaidingParty -> EventRewards(success = Resources(gold = 200, wood = 200, metal = 50))
        EventType.DragonRaid -> EventRewards(success = Resources(gold = 300, wood = 300, metal = 100, mana = 5))
        EventType.FullInvasion -> EventRewards(success = Resources(gold = 500, wood = 500, metal = 200, mana = 10))
        EventType.Siege -> EventRewards(success = Resources(gold = 800, wood = 800, metal = 300, mana = 20))
    }

    fun disasterDamagePercent(type: EventType): IntRange = when (type) {
        EventType.Earthquake -> 20..50
        EventType.Storm -> 15..40
        else -> 0..0
    }

    fun eligibleEvents(townHallLevel: Int): List<EventType> =
        EventType.entries.filter { it.requiredTownHallLevel <= townHallLevel }

    const val MIN_EVENT_INTERVAL_MS = 3_600_000L  // 1 hour minimum between events
    const val MAX_EVENT_INTERVAL_MS = 4 * 3_600_000L  // 4 hours max
}
