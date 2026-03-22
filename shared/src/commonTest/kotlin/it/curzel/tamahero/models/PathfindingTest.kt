package it.curzel.tamahero.models

import kotlin.test.*

class PathfindingTest {

    private fun building(id: Long, type: BuildingType, x: Int, y: Int, hp: Int = 200): PlacedBuilding =
        PlacedBuilding(id = id, type = type, level = 1, x = x, y = y, hp = hp)

    @Test
    fun directPathNoObstacles() {
        val target = building(1, BuildingType.GoldStorage, x = 10, y = 10)
        val path = Pathfinding.findPath(5f, 10f, target, listOf(target))
        assertNotNull(path)
        assertTrue(path.isNotEmpty())
        val last = path.last()
        assertTrue(last.x in 8..12 && last.y in 8..12, "Path should end adjacent to target")
    }

    @Test
    fun pathAroundBuilding() {
        // Target at (10,5), obstacle 2x2 at (8,5) blocking direct horizontal path
        val target = building(1, BuildingType.GoldStorage, x = 10, y = 5)
        val obstacle = building(2, BuildingType.GoldStorage, x = 8, y = 5)
        val path = Pathfinding.findPath(6f, 5.5f, target, listOf(target, obstacle))
        assertNotNull(path, "Should find path around obstacle")
        // Path should not go through the obstacle tiles (8,5), (8,6), (9,5), (9,6)
        for (pos in path) {
            val inObstacle = pos.x in 8..9 && pos.y in 5..6
            assertFalse(inObstacle, "Path should not go through obstacle at ${pos.x},${pos.y}")
        }
    }

    @Test
    fun trapsAreWalkable() {
        val target = building(1, BuildingType.GoldStorage, x = 10, y = 5)
        val trap = building(2, BuildingType.SpikeTrap, x = 8, y = 5)
        val path = Pathfinding.findPath(6f, 5f, target, listOf(target, trap))
        assertNotNull(path)
        // Traps should not block, so path can go through (8,5)
        // Just verify a path exists and is reasonably short (direct-ish)
        assertTrue(path.size <= 6, "Path should be short since trap doesn't block")
    }

    @Test
    fun springTrapIsWalkable() {
        val target = building(1, BuildingType.GoldStorage, x = 10, y = 5)
        val trap = building(2, BuildingType.SpringTrap, x = 8, y = 5)
        val path = Pathfinding.findPath(6f, 5f, target, listOf(target, trap))
        assertNotNull(path)
        assertTrue(path.size <= 6)
    }

    @Test
    fun noPathReturnsNull() {
        // Surround the target completely with walls
        val target = building(1, BuildingType.GoldStorage, x = 5, y = 5)
        val walls = mutableListOf(target)
        // Ring of walls around the target (5,5)-(6,6) — wall all adjacent tiles
        for (x in 3..8) {
            for (y in 3..8) {
                if (x in 5..6 && y in 5..6) continue // target itself
                walls.add(building(walls.size + 1L, BuildingType.Wall, x = x, y = y))
            }
        }
        val path = Pathfinding.findPath(0f, 0f, target, walls)
        assertNull(path, "Should return null when target is completely walled off")
    }

    @Test
    fun startOnEdgeOfBuildingFootprint() {
        val target = building(1, BuildingType.GoldStorage, x = 10, y = 10)
        val obstacle = building(2, BuildingType.TownHall, x = 0, y = 0) // 4x4, occupies 0-3 x 0-3
        // Start at (3.9, 0) which rounds to tile (3,0) — inside TownHall footprint
        // Start tile is unblocked by the algorithm, adjacent tile (4,0) is free
        val path = Pathfinding.findPath(3.9f, 0f, target, listOf(target, obstacle))
        assertNotNull(path, "Should find path even if start rounds to a building tile")
    }

    @Test
    fun pathAroundLargeBuilding() {
        // TownHall is 4x4 at (8,8), blocking path from (5,10) to target at (14,10)
        val target = building(1, BuildingType.GoldStorage, x = 14, y = 10)
        val townHall = building(2, BuildingType.TownHall, x = 8, y = 8) // 4x4: occupies 8-11 x 8-11
        val path = Pathfinding.findPath(5f, 10f, target, listOf(target, townHall))
        assertNotNull(path)
        for (pos in path) {
            val inTownHall = pos.x in 8..11 && pos.y in 8..11
            assertFalse(inTownHall, "Path should not go through TownHall at ${pos.x},${pos.y}")
        }
    }

    @Test
    fun pathThroughGapInWallLine() {
        val target = building(1, BuildingType.GoldStorage, x = 10, y = 5)
        val buildings = mutableListOf(target)
        // Wall line at x=8, from y=0 to y=10, with gap at y=5
        for (y in 0..10) {
            if (y == 5) continue // gap
            buildings.add(building(buildings.size + 1L, BuildingType.Wall, x = 8, y = y))
        }
        val path = Pathfinding.findPath(5f, 5f, target, buildings)
        assertNotNull(path, "Should find path through gap")
        // Path should go through the gap at (8,5)
        assertTrue(path.any { it.x == 8 && it.y == 5 }, "Path should use the gap at (8,5)")
    }
}
