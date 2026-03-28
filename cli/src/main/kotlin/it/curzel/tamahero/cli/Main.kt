package it.curzel.tamahero.cli

import it.curzel.tamahero.models.*
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val autoplay = args.contains("--autoplay")
    val url = args.firstOrNull { !it.startsWith("--") } ?: "http://localhost:8081"
    val client = CliClient(url)

    println("TamaHero CLI — server: $url")

    runBlocking {
        var connected = false

        // Auto-connect if we have a saved token
        if (client.loadSavedToken()) {
            try {
                client.connectInBackground()
                connected = true
                println("Auto-connected.")
            } catch (e: Exception) {
                println("Saved token expired. Use 'login' or 'register'.")
                client.clearSavedToken()
            }
        }

        if (autoplay) {
            if (!connected) {
                println("Error: --autoplay requires a saved token. Login first.")
                client.close()
                return@runBlocking
            }
            AutoPlay(client).run()
            return@runBlocking
        }

        println("Type 'help' for commands.")
        println()

        while (true) {
            print("> ")
            val line = readlnOrNull()?.trim() ?: break
            if (line.isEmpty()) continue
            val parts = line.split("\\s+".toRegex())
            val cmd = parts[0].lowercase()

            try {
                when (cmd) {
                    "quit", "exit", "q" -> break

                    "register" -> {
                        if (parts.size < 3) { println("Usage: register <username> <password>"); continue }
                        client.register(parts[1], parts[2])
                    }

                    "login" -> {
                        if (parts.size < 3) { println("Usage: login <username> <password>"); continue }
                        client.login(parts[1], parts[2])
                    }

                    "logout" -> {
                        client.clearSavedToken()
                        connected = false
                    }

                    "connect" -> {
                        client.connectInBackground()
                        connected = true
                    }

                    "village", "get" -> {
                        if (!connected) { println("Connect first."); continue }
                        client.sendAndReceive(ClientMessage.GetVillage)
                    }

                    "build" -> {
                        if (!connected) { println("Connect first."); continue }
                        if (parts.size < 4) { println("Usage: build <type> <x> <y>"); continue }
                        val type = parseBuildingType(parts[1]) ?: continue
                        client.sendAndReceive(ClientMessage.Build(type, parts[2].toInt(), parts[3].toInt()))
                    }

                    "upgrade" -> {
                        if (!connected) { println("Connect first."); continue }
                        if (parts.size < 2) { println("Usage: upgrade <buildingId>"); continue }
                        client.sendAndReceive(ClientMessage.Upgrade(parts[1].toLong()))
                    }

                    "move" -> {
                        if (!connected) { println("Connect first."); continue }
                        if (parts.size < 4) { println("Usage: move <buildingId> <x> <y>"); continue }
                        client.sendAndReceive(ClientMessage.Move(parts[1].toLong(), parts[2].toInt(), parts[3].toInt()))
                    }

                    "collect" -> {
                        if (!connected) { println("Connect first."); continue }
                        if (parts.size < 2) { println("Usage: collect <buildingId>"); continue }
                        client.sendAndReceive(ClientMessage.Collect(parts[1].toLong()))
                    }

                    "collectall" -> {
                        if (!connected) { println("Connect first."); continue }
                        client.sendAndReceive(ClientMessage.CollectAll)
                    }

                    "demolish" -> {
                        if (!connected) { println("Connect first."); continue }
                        if (parts.size < 2) { println("Usage: demolish <buildingId>"); continue }
                        client.sendAndReceive(ClientMessage.Demolish(parts[1].toLong()))
                    }

                    "cancel" -> {
                        if (!connected) { println("Connect first."); continue }
                        if (parts.size < 2) { println("Usage: cancel <buildingId>"); continue }
                        client.sendAndReceive(ClientMessage.CancelConstruction(parts[1].toLong()))
                    }

                    "speedup" -> {
                        if (!connected) { println("Connect first."); continue }
                        if (parts.size < 2) { println("Usage: speedup <buildingId>"); continue }
                        client.sendAndReceive(ClientMessage.SpeedUp(parts[1].toLong()))
                    }

                    "train" -> {
                        if (!connected) { println("Connect first."); continue }
                        if (parts.size < 2) { println("Usage: train <troopType> [count] [level]"); continue }
                        val type = parseTroopType(parts[1]) ?: continue
                        val count = if (parts.size >= 3) parts[2].toInt() else 1
                        val level = if (parts.size >= 4) parts[3].toInt() else 1
                        client.sendAndReceive(ClientMessage.Train(type, count, level))
                    }

                    "canceltraining" -> {
                        if (!connected) { println("Connect first."); continue }
                        if (parts.size < 2) { println("Usage: canceltraining <index>"); continue }
                        client.sendAndReceive(ClientMessage.CancelTraining(parts[1].toInt()))
                    }

                    "army" -> showArmy(client.getLastState())

                    "troops" -> showTroopTypes()

                    "rearm" -> {
                        if (!connected) { println("Connect first."); continue }
                        if (parts.size < 2) { println("Usage: rearm <buildingId>"); continue }
                        client.sendAndReceive(ClientMessage.RearmTrap(parts[1].toLong()))
                    }

                    "collectrewards" -> {
                        if (!connected) { println("Connect first."); continue }
                        client.sendAndReceive(ClientMessage.CollectEventRewards)
                    }

                    "event" -> showEvent(client.getLastState())

                    "rearmall" -> {
                        if (!connected) { println("Connect first."); continue }
                        client.sendAndReceive(ClientMessage.RearmAllTraps)
                    }

                    "info" -> {
                        if (parts.size < 2) { println("Usage: info <buildingId>"); continue }
                        showBuildingInfo(client.getLastState(), parts[1].toLong())
                    }

                    "buildings" -> showBuildingTypes()

                    "storage" -> showStorageCapacity(client.getLastState())

                    "map" -> showMap(client.getLastState())

                    "state" -> {
                        val state = client.getLastState()
                        if (state == null) println("No state yet. Run 'village' first.")
                        else println("Last known state cached. Run 'village' to refresh.")
                    }

                    "attack" -> {
                        if (!connected) { println("Connect first."); continue }
                        client.sendAndReceive(ClientMessage.FindOpponent)
                    }

                    "next" -> {
                        if (!connected) { println("Connect first."); continue }
                        client.sendAndReceive(ClientMessage.NextOpponent)
                    }

                    "go" -> {
                        if (!connected) { println("Connect first."); continue }
                        val targetId = client.getLastMatchTarget()
                        if (targetId == null) { println("No opponent found. Use 'attack' first."); continue }
                        client.sendAndReceive(ClientMessage.StartPvp(targetId))
                    }

                    "deploy" -> {
                        if (!connected) { println("Connect first."); continue }
                        if (parts.size < 4) { println("Usage: deploy <troopType> <x> <y>"); continue }
                        val type = parseTroopType(parts[1]) ?: continue
                        client.sendAndReceive(ClientMessage.DeployTroop(type, parts[2].toFloat(), parts[3].toFloat()))
                    }

                    "surrender" -> {
                        if (!connected) { println("Connect first."); continue }
                        client.sendAndReceive(ClientMessage.EndBattle)
                    }

                    "defenselog" -> showDefenseLog(client.getLastState())

                    "leaderboard", "lb" -> {
                        if (!connected) { println("Connect first."); continue }
                        client.sendAndReceive(ClientMessage.GetLeaderboard)
                    }

                    "help", "?" -> printHelp()

                    else -> println("Unknown command: $cmd (type 'help' for commands)")
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    client.close()
    println("Bye!")
}

private fun printHelp() {
    println("""
        |Connection:
        |  register <user> <pass>   Register a new account
        |  login <user> <pass>      Login to existing account
        |  logout                   Clear saved session
        |  connect                  Connect WebSocket
        |
        |Village:
        |  village                  Load/refresh village state
        |  build <type> <x> <y>    Place a new building
        |  upgrade <id>            Upgrade a building
        |  move <id> <x> <y>      Move a building
        |  demolish <id>           Demolish a building (50% refund)
        |  cancel <id>             Cancel construction (full refund)
        |  speedup <id>            Instant finish with plasma
        |
        |Resources:
        |  collect <id>            Collect from a producer
        |  collectall              Collect from all producers
        |  storage                 Show storage capacity
        |
        |Troops:
        |  train <type> [count]    Queue troops for training
        |  canceltraining <index>  Cancel queued training
        |  army                    Show army & training queue
        |  troops                  List troop types & stats
        |
        |Defense:
        |  rearm <id>              Rearm a triggered trap (50% cost)
        |  rearmall                Rearm all triggered traps
        |
        |PvP:
        |  attack                  Find an opponent to attack
        |  next                    Skip to next opponent
        |  go                      Start battle with shown opponent
        |  deploy <type> <x> <y>  Deploy troop during battle (edges only)
        |  surrender               End battle early
        |  defenselog              Show recent attacks against you
        |  leaderboard / lb       Show trophy leaderboard
        |
        |Events:
        |  event                   Show active event status
        |  collectrewards          Collect rewards from completed event
        |
        |Info:
        |  info <id>               Building details
        |  buildings               List building types & costs
        |  map                     ASCII village map
        |  state                   Show cached state status
        |
        |  help                    Show this help
        |  quit                    Exit
    """.trimMargin())
}

private fun parseBuildingType(name: String): BuildingType? {
    return try {
        BuildingType.valueOf(name)
    } catch (_: Exception) {
        println("Unknown type: $name. Options: ${BuildingType.entries.joinToString()}")
        null
    }
}

private fun showBuildingInfo(state: GameState?, buildingId: Long) {
    if (state == null) { println("No state. Run 'village' first."); return }
    val building = state.village.buildings.find { it.id == buildingId }
    if (building == null) { println("Building $buildingId not found."); return }

    val config = BuildingConfig.configFor(building.type, building.level)
    println("  ${building.type} level ${building.level} at (${building.x}, ${building.y})")
    println("  HP: ${building.hp}/${config?.hp ?: "?"}")
    if (building.constructionStartedAt != null) {
        println("  Status: Under construction")
    }
    if (config != null) {
        if (config.productionPerHour != Resources()) {
            println("  Production/hr: ${formatResources(config.productionPerHour)}")
        }
        if (config.storageCapacity != Resources()) {
            println("  Storage capacity: ${formatResources(config.storageCapacity)}")
        }
        if (config.damage > 0) {
            println("  Damage: ${config.damage}, Range: ${config.range}, Speed: ${config.attackSpeedMs}ms")
        }
    }

    val nextConfig = BuildingConfig.configFor(building.type, building.level + 1)
    if (nextConfig != null) {
        println("  Upgrade cost: ${formatResources(nextConfig.cost)}")
        println("  Upgrade time: ${nextConfig.buildTimeSeconds}s")
    } else {
        println("  Max level reached")
    }
}

private fun showBuildingTypes() {
    println("Available buildings:")
    for (type in BuildingType.entries) {
        val config = BuildingConfig.configFor(type, 1) ?: continue
        val extras = mutableListOf<String>()
        if (config.productionPerHour != Resources()) extras.add("produces ${formatResources(config.productionPerHour)}/hr")
        if (config.storageCapacity != Resources()) extras.add("stores ${formatResources(config.storageCapacity)}")
        if (config.damage > 0) extras.add("dmg=${config.damage}")
        if (config.requiredTownHallLevel > 1) extras.add("requires CC${config.requiredTownHallLevel}")
        val extra = if (extras.isNotEmpty()) " — ${extras.joinToString()}" else ""
        println("  $type: cost=${formatResources(config.cost)}, build=${config.buildTimeSeconds}s, ${config.width}x${config.height}$extra")
    }
}

private fun showStorageCapacity(state: GameState?) {
    if (state == null) { println("No state. Run 'village' first."); return }
    var totalCapacity = Resources()
    for (building in state.village.buildings) {
        if (building.constructionStartedAt != null) continue
        val config = BuildingConfig.configFor(building.type, building.level) ?: continue
        totalCapacity = totalCapacity + config.storageCapacity
    }
    println("Resources / Storage capacity:")
    println("  Metal:      ${state.resources.metal} / ${if (totalCapacity.metal > 0) totalCapacity.metal else "unlimited"}")
    println("  Crystal:    ${state.resources.crystal} / ${if (totalCapacity.crystal > 0) totalCapacity.crystal else "unlimited"}")
    println("  Deuterium:  ${state.resources.deuterium} / ${if (totalCapacity.deuterium > 0) totalCapacity.deuterium else "unlimited"}")
    println("  Credits:    ${state.resources.credits} (unlimited)")
}

private fun showMap(state: GameState?) {
    if (state == null) { println("No state. Run 'village' first."); return }

    val minX = state.village.buildings.minOf { it.x } - 1
    val minY = state.village.buildings.minOf { it.y } - 1
    val maxX = state.village.buildings.maxOf { b ->
        val config = BuildingConfig.configFor(b.type, b.level)
        b.x + (config?.width ?: 2)
    } + 1
    val maxY = state.village.buildings.maxOf { b ->
        val config = BuildingConfig.configFor(b.type, b.level)
        b.y + (config?.height ?: 2)
    } + 1

    val grid = Array(maxY - minY) { CharArray(maxX - minX) { '.' } }

    for (building in state.village.buildings) {
        val config = BuildingConfig.configFor(building.type, building.level) ?: continue
        val label = buildingLabel(building.type)
        for (dy in 0 until config.height) {
            for (dx in 0 until config.width) {
                val gx = building.x - minX + dx
                val gy = building.y - minY + dy
                if (gy in grid.indices && gx in grid[0].indices) {
                    grid[gy][gx] = label
                }
            }
        }
    }

    println("Map (${minX},$minY to ${maxX},$maxY):")
    for (row in grid) {
        println("  ${String(row)}")
    }
    println()
    println("Legend: C=CommandCenter A=AlloyRefinery M=CreditMint F=Foundry")
    println("  a=AlloySilo c=CreditVault x=CrystalSilo B=Academy")
    println("  H=Hangar R=RailGun L=LaserTurret W=Barrier")
}

private fun buildingLabel(type: BuildingType): Char = when (type) {
    BuildingType.CommandCenter -> 'C'
    BuildingType.MetalMine -> 'M'
    BuildingType.CrystalMine -> 'K'
    BuildingType.DeuteriumSynthesizer -> 'D'
    BuildingType.MetalStorage -> 'm'
    BuildingType.CrystalStorage -> 'k'
    BuildingType.DeuteriumStorage -> 'd'
    BuildingType.Barracks -> 'B'
    BuildingType.Hangar -> 'H'
    BuildingType.RoboticsFactory -> 'R'
    BuildingType.GaussCannon -> 'G'
    BuildingType.LightLaser -> 'L'
    BuildingType.HeavyLaser -> 'V'
    BuildingType.MissileLauncher -> 'I'
    BuildingType.IonCannon -> 'N'
    BuildingType.PlasmaCannon -> 'P'
    BuildingType.Wall -> 'W'
    BuildingType.LandMine -> 'x'
    BuildingType.GravityWell -> 'g'
    BuildingType.NovaBomb -> 'b'
    BuildingType.ShieldDome -> 'S'
}

private fun showEvent(state: GameState?) {
    if (state == null) { println("No state. Run 'village' first."); return }
    val event = state.activeEvent
    if (event == null) {
        println("No active event.")
        if (state.shieldExpiresAt > 0) {
            println("Shield active until ${state.shieldExpiresAt}")
        }
        return
    }
    println("Event: ${event.type}")
    println("  Status: ${if (event.completed) "COMPLETED" else "ACTIVE"}")
    println("  Wave: ${event.currentWave + 1}/${event.totalWaves}")
    val rewards = event.pendingRewards
    if (rewards != null) {
        println("  Rewards ready: ${formatResources(rewards)}")
        println("  Use 'collectrewards' to claim.")
    }
}

private fun parseTroopType(name: String): TroopType? {
    return try {
        TroopType.valueOf(name)
    } catch (_: Exception) {
        println("Unknown troop: $name. Options: ${TroopType.entries.joinToString()}")
        null
    }
}

private fun showArmy(state: GameState?) {
    if (state == null) { println("No state. Run 'village' first."); return }
    val capacity = state.village.buildings
        .filter { it.type == BuildingType.Hangar && it.constructionStartedAt == null }
        .sumOf { BuildingConfig.configFor(it.type, it.level)?.troopCapacity ?: 0 }

    println("Army (${state.army.totalCount}/$capacity):")
    if (state.army.troops.isEmpty()) {
        println("  (empty)")
    } else {
        for (t in state.army.troops) {
            println("  ${t.type} lv${t.level} x${t.count}")
        }
    }
    if (state.trainingQueue.entries.isNotEmpty()) {
        println("Training queue (${state.trainingQueue.entries.size}):")
        for ((i, e) in state.trainingQueue.entries.withIndex()) {
            val status = if (e.startedAt != null) " [TRAINING]" else " [QUEUED]"
            println("  [$i] ${e.troopType} lv${e.level}$status")
        }
    }
}

private fun showTroopTypes() {
    println("Available troops:")
    for (type in TroopType.entries) {
        val config = TroopConfig.configFor(type, 1) ?: continue
        println("  $type: hp=${config.hp} dps=${config.dps} spd=${config.speed} rng=${config.range} cost=${formatResources(config.trainingCost)} time=${config.trainingTimeSeconds}s")
    }
}

private fun showDefenseLog(state: GameState?) {
    if (state == null) { println("No state. Run 'village' first."); return }
    if (state.defenseLog.isEmpty()) {
        println("No attacks recorded.")
        return
    }
    println("Defense log:")
    for (entry in state.defenseLog) {
        val stars = "*".repeat(entry.stars) + "_".repeat(3 - entry.stars)
        println("  ${entry.attackerName} — $stars — lost: ${formatResources(entry.lootLost)} — trophies: ${entry.trophyDelta}")
    }
    println("Trophies: ${state.trophies}")
    if (state.shieldExpiresAt > System.currentTimeMillis()) {
        val remaining = (state.shieldExpiresAt - System.currentTimeMillis()) / 3_600_000
        println("Shield: ${remaining}h remaining")
    }
}

private fun formatResources(r: Resources): String {
    val parts = mutableListOf<String>()
    if (r.credits > 0) parts.add("${r.credits}cr")
    if (r.metal > 0) parts.add("${r.metal}m")
    if (r.crystal > 0) parts.add("${r.crystal}c")
    if (r.deuterium > 0) parts.add("${r.deuterium}dt")
    return if (parts.isEmpty()) "free" else parts.joinToString(" ")
}
