package it.curzel.tamahero.models

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.sqrt

@Serializable
data class GridPos(val x: Int, val y: Int)

object Pathfinding {

    private const val GRID_SIZE = 40
    private val SQRT2 = sqrt(2.0).toFloat()

    private val DIRECTIONS = listOf(
        GridPos(1, 0), GridPos(-1, 0), GridPos(0, 1), GridPos(0, -1),
        GridPos(1, 1), GridPos(1, -1), GridPos(-1, 1), GridPos(-1, -1),
    )

    fun findPath(
        startX: Float,
        startY: Float,
        targetBuilding: PlacedBuilding,
        buildings: List<PlacedBuilding>,
    ): List<GridPos>? {
        val blocked = buildGrid(targetBuilding, buildings)
        val start = GridPos(
            startX.toInt().coerceIn(0, GRID_SIZE - 1),
            startY.toInt().coerceIn(0, GRID_SIZE - 1),
        )
        // Ensure start is walkable even if troop is inside a building footprint
        blocked[start.x][start.y] = false

        val goals = goalTiles(targetBuilding, blocked)
        if (goals.isEmpty()) return null
        if (start in goals) return listOf(start)

        return astar(start, goals, blocked)
    }

    private fun buildGrid(target: PlacedBuilding, buildings: List<PlacedBuilding>): Array<BooleanArray> {
        val blocked = Array(GRID_SIZE) { BooleanArray(GRID_SIZE) }
        for (building in buildings) {
            if (building.id == target.id) continue
            if (building.type.isTrap) continue
            val config = BuildingConfig.configFor(building.type, building.level) ?: continue
            for (bx in building.x until (building.x + config.width).coerceAtMost(GRID_SIZE)) {
                for (by in building.y until (building.y + config.height).coerceAtMost(GRID_SIZE)) {
                    if (bx in 0 until GRID_SIZE && by in 0 until GRID_SIZE) {
                        blocked[bx][by] = true
                    }
                }
            }
        }
        return blocked
    }

    private fun goalTiles(target: PlacedBuilding, blocked: Array<BooleanArray>): Set<GridPos> {
        val config = BuildingConfig.configFor(target.type, target.level) ?: return emptySet()
        val goals = mutableSetOf<GridPos>()
        val bx = target.x
        val by = target.y
        val bw = config.width
        val bh = config.height

        // Only face-adjacent tiles (not diagonal corners) so troops are within melee range
        // Top and bottom rows
        for (x in bx until bx + bw) {
            val top = by - 1
            if (top >= 0 && !blocked[x][top]) goals.add(GridPos(x, top))
            val bottom = by + bh
            if (bottom < GRID_SIZE && !blocked[x][bottom]) goals.add(GridPos(x, bottom))
        }
        // Left and right columns
        for (y in by until by + bh) {
            val left = bx - 1
            if (left >= 0 && !blocked[left][y]) goals.add(GridPos(left, y))
            val right = bx + bw
            if (right < GRID_SIZE && !blocked[right][y]) goals.add(GridPos(right, y))
        }

        return goals
    }

    private fun astar(start: GridPos, goals: Set<GridPos>, blocked: Array<BooleanArray>): List<GridPos>? {
        val gScore = HashMap<GridPos, Float>(256)
        val fScore = HashMap<GridPos, Float>(256)
        val cameFrom = HashMap<GridPos, GridPos>(256)
        val openSet = mutableListOf(start)
        val closedSet = HashSet<GridPos>(256)

        gScore[start] = 0f
        fScore[start] = heuristic(start, goals)

        while (openSet.isNotEmpty()) {
            val current = openSet.minByOrNull { fScore[it] ?: Float.MAX_VALUE } ?: break
            if (current in goals) return reconstructPath(cameFrom, current)

            openSet.remove(current)
            closedSet.add(current)

            for (dir in DIRECTIONS) {
                val nx = current.x + dir.x
                val ny = current.y + dir.y
                val neighbor = GridPos(nx, ny)

                if (nx !in 0 until GRID_SIZE || ny !in 0 until GRID_SIZE) continue
                if (blocked[nx][ny]) continue
                if (neighbor in closedSet) continue

                // No corner cutting: for diagonal moves, both cardinal neighbors must be free
                val isDiagonal = dir.x != 0 && dir.y != 0
                if (isDiagonal) {
                    if (blocked[current.x + dir.x][current.y] || blocked[current.x][current.y + dir.y]) continue
                }

                val moveCost = if (isDiagonal) SQRT2 else 1f
                val tentativeG = (gScore[current] ?: Float.MAX_VALUE) + moveCost

                if (tentativeG < (gScore[neighbor] ?: Float.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeG
                    fScore[neighbor] = tentativeG + heuristic(neighbor, goals)
                    if (neighbor !in openSet) {
                        openSet.add(neighbor)
                    }
                }
            }
        }
        return null
    }

    private fun heuristic(pos: GridPos, goals: Set<GridPos>): Float {
        return goals.minOf { goal -> octileDistance(pos, goal) }
    }

    private fun octileDistance(a: GridPos, b: GridPos): Float {
        val dx = abs(a.x - b.x)
        val dy = abs(a.y - b.y)
        return maxOf(dx, dy) + (SQRT2 - 1f) * minOf(dx, dy)
    }

    private fun reconstructPath(cameFrom: Map<GridPos, GridPos>, end: GridPos): List<GridPos> {
        val path = mutableListOf(end)
        var current = end
        while (cameFrom.containsKey(current)) {
            current = cameFrom[current]!!
            path.add(current)
        }
        path.reverse()
        // Drop the start position (troop is already there)
        return if (path.size > 1) path.drop(1) else path
    }
}
