package it.curzel.tamahero.models

import kotlin.test.Test
import kotlin.test.assertEquals

class PvpBattleTest {

    @Test
    fun `loot available is 20 percent of resources and 10 percent of plasma`() {
        val resources = Resources(credits = 10000, alloy = 5000, crystal = 2000, plasma = 1000)
        val available = PvpCalculations.calculateLootAvailable(resources)
        assertEquals(2000, available.credits)
        assertEquals(1000, available.alloy)
        assertEquals(400, available.crystal)
        assertEquals(100, available.plasma)
    }

    @Test
    fun `loot stolen scales with destruction percent`() {
        val available = Resources(credits = 2000, alloy = 1000, crystal = 400, plasma = 100)
        val loot50 = PvpCalculations.calculateLootStolen(available, 50)
        assertEquals(1000, loot50.credits)
        assertEquals(500, loot50.alloy)
        assertEquals(200, loot50.crystal)
        assertEquals(50, loot50.plasma)

        val loot100 = PvpCalculations.calculateLootStolen(available, 100)
        assertEquals(2000, loot100.credits)

        val loot0 = PvpCalculations.calculateLootStolen(available, 0)
        assertEquals(0, loot0.credits)
    }

    @Test
    fun `trophy delta positive for win`() {
        val delta = PvpCalculations.calculateTrophyDelta(1000, 1000, 3)
        assert(delta > 0) { "Expected positive trophies for 3-star win, got $delta" }
    }

    @Test
    fun `trophy delta negative for loss`() {
        val delta = PvpCalculations.calculateTrophyDelta(1000, 1000, 0)
        assert(delta < 0) { "Expected negative trophies for 0-star loss, got $delta" }
    }

    @Test
    fun `trophy delta higher when attacking stronger opponent`() {
        val deltaWeaker = PvpCalculations.calculateTrophyDelta(1000, 800, 2)
        val deltaStronger = PvpCalculations.calculateTrophyDelta(1000, 1200, 2)
        assert(deltaStronger > deltaWeaker) {
            "Expected more trophies vs stronger opponent: $deltaStronger vs $deltaWeaker"
        }
    }

    @Test
    fun `trophy delta clamped to valid range`() {
        val huge = PvpCalculations.calculateTrophyDelta(100, 5000, 3)
        assert(huge in 1..59) { "Trophy delta $huge should be between 1 and 59" }

        val tiny = PvpCalculations.calculateTrophyDelta(5000, 100, 1)
        assert(tiny >= 1) { "Trophy delta $tiny should be at least 1" }
    }

    @Test
    fun `shield duration based on destruction`() {
        assertEquals(0L, PvpCalculations.calculateShieldDuration(0))
        assertEquals(0L, PvpCalculations.calculateShieldDuration(29))
        assertEquals(12 * 3_600_000L, PvpCalculations.calculateShieldDuration(30))
        assertEquals(12 * 3_600_000L, PvpCalculations.calculateShieldDuration(59))
        assertEquals(14 * 3_600_000L, PvpCalculations.calculateShieldDuration(60))
        assertEquals(14 * 3_600_000L, PvpCalculations.calculateShieldDuration(89))
        assertEquals(16 * 3_600_000L, PvpCalculations.calculateShieldDuration(90))
        assertEquals(16 * 3_600_000L, PvpCalculations.calculateShieldDuration(100))
    }

    @Test
    fun `star calculation based on destruction and command center`() {
        val base = listOf(
            PlacedBuilding(id = 1, type = BuildingType.CommandCenter, level = 1, x = 18, y = 18, hp = 1000),
            PlacedBuilding(id = 2, type = BuildingType.RailGun, level = 1, x = 10, y = 10, hp = 500),
            PlacedBuilding(id = 3, type = BuildingType.CreditMint, level = 1, x = 5, y = 5, hp = 500),
        )

        val battle = PvpBattle(
            battleId = "test",
            attackerId = 1,
            defenderId = 2,
            defenderName = "test",
            defenderTrophies = 100,
            defenderBase = Village(playerId = 2, buildings = base),
            availableTroops = Army(),
            buildings = base,
            startedAt = 0,
        )

        assertEquals(0, battle.currentStars)
        assertEquals(0, battle.destructionPercent)

        // 50% destruction (remove cannon entirely = 500/2000 = 25%... not enough)
        // Actually TH=1000, Cannon=500, GoldMine=500 = 2000 total
        // Remove GoldMine+Cannon (1000hp destroyed / 2000 total = 50%)
        val after50 = battle.copy(buildings = listOf(base[0]))
        assertEquals(50, after50.destructionPercent)
        assertEquals(1, after50.currentStars)

        // TH destroyed (50% destruction: 1000/2000 = 50%, plus TH destroyed = 2 stars)
        val noTh = battle.copy(buildings = listOf(base[1], base[2]))
        assert(noTh.commandCenterDestroyed)
        assertEquals(50, noTh.destructionPercent)
        assertEquals(2, noTh.currentStars)

        // 100% destruction
        val total = battle.copy(buildings = emptyList())
        assertEquals(100, total.destructionPercent)
        assertEquals(3, total.currentStars)
    }

    @Test
    fun `time up detection`() {
        val battle = PvpBattle(
            battleId = "test",
            attackerId = 1,
            defenderId = 2,
            defenderName = "test",
            defenderTrophies = 100,
            defenderBase = Village(playerId = 2, buildings = emptyList()),
            availableTroops = Army(),
            buildings = emptyList(),
            startedAt = 1000,
            timeLimitMs = 180_000,
        )

        assert(!battle.isTimeUp(100_000))
        assert(battle.isTimeUp(181_001))
        assertEquals(80_000, battle.timeRemainingMs(101_000))
    }
}
