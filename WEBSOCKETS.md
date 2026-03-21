# WebSockets in TamaHero

This document defines TamaHero's WebSocket protocol and architecture, followed by a reference section on how Galdria (the sister project) implements WebSockets.

---

# TamaHero Protocol Design

## Architecture

**WebSocket-only. One connection, one protocol.**

```
┌──────────────┐                       ┌──────────────┐
│  KMP Client  │ ◄──── WebSocket ────► │  Ktor Server │
│  (Compose)   │   JSON over WS        │  (Netty)     │
└──────────────┘                       └──────────────┘
```

Everything goes through a single WebSocket connection:
- **Client → Server:** player actions (build, upgrade, move, collect), keep-alive, battle deployment
- **Server → Client:** game state responses, timer events, battle ticks, errors

The only REST endpoints are auth (`/api/auth/*`) — these run before the WebSocket connection is established.

### Why WebSocket for Everything?

- **One transport, one auth flow, one message format** — simpler to build, debug, and maintain
- **Server can push events at any time** — building completions, incoming attacks, battle ticks
- **Request-response is just a pattern** — client sends action, server responds with updated `GameState` on the same connection
- **Reconnection is simple** — reconnect, send `get_village`, receive full state

### Auth Stays REST

Authentication (`/api/auth/register`, `/api/auth/login`, `/api/auth/social-login`) remains REST because:
- Auth happens before the WebSocket connection exists
- The WebSocket handshake uses the token from auth: `wss://tama.curzel.it/ws?token=eyJhbG...`
- No reason to complicate auth with WebSocket state

## Connection Lifecycle

```
App Launch
    │
    ├── REST: POST /api/auth/login (get token)
    │
    ├── WebSocket: connect to /ws?token=<token>
    │   │
    │   ├── Server sends: connected {playerId, protocolVersion}
    │   │
    │   ├── Client sends: get_village
    │   ├── Server sends: game_state {full state}
    │   │
    │   │   From here, bidirectional:
    │   │
    │   │   Client sends:
    │   │   ├── keep_alive                                  (every 10s)
    │   │   ├── build, upgrade, move, collect               (village actions)
    │   │   ├── deploy_troop, surrender_battle              (during battle)
    │   │   ├── get_village                                 (refresh state)
    │   │
    │   │   Server sends:
    │   │   ├── game_state                                  (response to actions)
    │   │   ├── building_complete, resources_updated         (timer events)
    │   │   ├── battle_tick, battle_end                      (battle simulation)
    │   │   ├── error                                       (validation failures)
    │   │
    │   └── Disconnect on app background / close
    │
    └── App Close
```

There are no "modes." The connection is just a pipe. Village events and battle events can arrive on the same connection at the same time.

### Authentication

Authenticate at WebSocket handshake via token query parameter:

```
wss://tama.curzel.it/ws?token=eyJhbG...
```

The server validates the token before accepting the connection. If invalid or expired, the server closes with a policy violation.

## Protocol Format

JSON text frames with a `type` field for routing (via Kotlin sealed class serialization):

```json
{"type": "build", "buildingType": "LumberCamp", "x": 5, "y": 5}
```

Server responds:

```json
{"type": "game_state", "playerId": 1, "resources": {...}, "village": {...}, ...}
```

Serialization config:

```kotlin
val protocolJson = Json {
    ignoreUnknownKeys = true      // Forward compatibility
    encodeDefaults = true
    isLenient = true
}
```

## Message Types

Every message is JSON with a `type` field (Kotlin `@SerialName`). Direction: C → S = client to server, S → C = server to client.

### Complete Message List

| # | Direction | Type | Key Fields | Phase | Description |
|---|-----------|------|------------|-------|-------------|
| | **Connection** | | | | |
| 1 | S → C | `connected` | `playerId`, `protocolVersion` | 1 | Server confirms WebSocket handshake |
| 2 | C → S | `keep_alive` | — | 1 | Client heartbeat, sent every 10s |
| 3 | S → C | `error` | `reason`, `details` | 1 | Validation failure or protocol error |
| | **Village Actions** | | | | |
| 4 | C → S | `get_village` | — | 1 | Request full game state |
| 5 | C → S | `build` | `buildingType`, `x`, `y` | 1 | Place a new building |
| 6 | C → S | `upgrade` | `buildingId` | 1 | Upgrade an existing building |
| 7 | C → S | `move` | `buildingId`, `x`, `y` | 1 | Move a building |
| 8 | C → S | `collect` | `buildingId` | 1 | Collect resources from a producer |
| 9 | S → C | `game_state` | full `GameState` object | 1 | Response to any village action |
| | **Timer Events (server push)** | | | | |
| 10 | S → C | `building_complete` | `buildingId`, `buildingType`, `level` | 1 | Construction or upgrade finished |
| 11 | S → C | `resources_updated` | `resources` | 1 | Periodic resource sync (~30s) |
| | **Battle — Setup** | | | | |
| 12 | C → S | `start_battle` | `targetId` | 2 | Start attack on a target (PvE or PvP) |
| 13 | S → C | `battle_started` | `battleId`, `defenderBase`, `availableTroops`, `timeLimit` | 2 | Battle initialized, here's the layout |
| | **Battle — Troop Deployment** | | | | |
| 14 | C → S | `deploy_troop` | `battleId`, `troopType`, `x`, `y` | 2 | Place a troop on the map edge |
| 15 | S → C | `deploy_ack` | `battleId`, `troopInstanceId`, `troopType`, `x`, `y` | 2 | Deployment accepted |
| 16 | S → C | `deploy_rejected` | `battleId`, `troopType`, `reason` | 2 | Deployment rejected |
| | **Battle — Simulation** | | | | |
| 17 | S → C | `battle_tick` | `battleId`, `tick`, `troops`, `buildings`, `effects` | 2 | Sim snapshot, ~10/sec |
| 18 | S → C | `building_destroyed` | `battleId`, `buildingId`, `buildingType`, `stars` | 2 | Building destroyed |
| | **Battle — End** | | | | |
| 19 | C → S | `surrender_battle` | `battleId` | 2 | Forfeit remaining troops |
| 20 | S → C | `battle_end` | `battleId`, `stars`, `loot`, `trophyChange`, `replayId` | 2 | Battle finished |
| | **Battle — Reconnection** | | | | |
| 21 | C → S | `request_battle_state` | `battleId` | 2 | Request full snapshot after reconnect |
| 22 | S → C | `battle_state` | `battleId`, `tick`, `troops`, `buildings`, `stars`, `timeRemaining` | 2 | Full battle snapshot |
| | **Defense Notifications** | | | | |
| 23 | S → C | `under_attack` | `attackerName`, `attackerTrophies` | 2 | Someone is raiding you |
| 24 | S → C | `defense_result` | `attackerName`, `stars`, `loot`, `trophyChange` | 2 | Attack on your base finished |
| | **Shields** | | | | |
| 25 | S → C | `shield_activated` | `expiresAt` | 2 | Shield granted |
| 26 | S → C | `shield_expired` | — | 2 | Shield ran out |
| | **Guild** | | | | |
| 27 | C → S | `guild_chat_send` | `message` | 3 | Send chat message |
| 28 | S → C | `guild_chat_message` | `senderName`, `message`, `timestamp` | 3 | Receive chat message |
| | **Speed-ups & Purchases** | | | | |
| 29 | S → C | `timer_cancelled` | `buildingId`, `buildingType`, `level` | 1 | Instant finish via mana |
| 30 | S → C | `purchase_confirmed` | `item`, `manaSpent`, `newManaBalance` | 1 | Purchase processed |

### Village Action Flow

Every village action follows the same pattern:

```
Client sends:  {"type": "build", "buildingType": "LumberCamp", "x": 5, "y": 5}
Server responds: {"type": "game_state", ...full updated GameState...}
  OR
Server responds: {"type": "error", "reason": "Insufficient resources"}
```

The server always:
1. Runs `GameStateUpdateUseCase.update(state, now)` to catch up elapsed time
2. Validates the action
3. Applies the action
4. Saves the updated state
5. Responds with the full `GameState` or an `error`

### Battle Tick Format

The `battle_tick` is the most frequent message. Keep it compact:

```json
{
    "type": "battle_tick",
    "battleId": "bat_456",
    "tick": 142,
    "troops": [
        {"id": 1, "type": "HumanSoldier", "x": 12.0, "y": 8.0, "hp": 45, "targetId": 7, "state": "attacking"},
        {"id": 2, "type": "ElfArcher", "x": 15.0, "y": 10.0, "hp": 20, "targetId": 3, "state": "moving"}
    ],
    "buildings": [
        {"id": 3, "hp": 80},
        {"id": 7, "hp": 120}
    ],
    "effects": [
        {"type": "damage", "x": 12, "y": 8, "value": 5}
    ]
}
```

Only buildings with changed HP since last tick are included.

### Tick Rate & Bandwidth

- **Sim tick rate:** 10 ticks/second (100ms intervals)
- **Network send rate:** 10/sec during active combat, drop to 2/sec when idle
- **Estimated tick size:** ~500 bytes for a typical battle
- **Peak bandwidth:** ~5 KB/sec — trivial
- **Battle duration:** max 3 minutes = max ~1800 ticks

## Heartbeat & Keep-Alive

Three-layer approach:

| Layer | Mechanism | Interval | Timeout |
|-------|-----------|----------|---------|
| Transport | Ktor ping-pong | 15s | 15s |
| Application | Client `keep_alive` message | 10s | — |
| Server | Keep-alive monitoring | 5s check | 30s timeout |

## Reconnection

On disconnect:

```
1. Client detects disconnect
2. Client reconnects WebSocket (with same token)
3. Client sends: get_village
4. Server responds: game_state {full state}
5. Done — the client is caught up
```

If mid-battle:

```
6. Client sends: request_battle_state {battleId}
7. Server sends: battle_state {full snapshot}
8. Client renders current state, server resumes sending battle_tick
```

The server keeps battles running during disconnection. The 3-minute battle timer is the only timeout.

## Rate Limiting

```
Client → Server messages: max 120/minute
```

Normal play generates ~10 messages/min (keep-alive + occasional actions). During battle, add ~50 deploy_troop messages over 3 minutes. The limit catches misbehaving clients.

## Server-Side Battle Simulation

The server is fully authoritative:

1. Client sends `start_battle` → server initializes simulation
2. Client sends `deploy_troop` → server validates and adds to simulation
3. Server runs sim at 10 ticks/sec, streams `battle_tick` to client
4. Battle ends → server sends `battle_end`, applies results (loot, trophies, shields)

### Why Server-Authoritative?

- No client/server divergence — one simulation
- Anti-cheat is trivial — client can't lie about results
- Battle replays are just the server's tick log
- Cost is ~5 KB/sec bandwidth per battle, negligible

## File Structure

```
shared/
  src/commonMain/.../models/
    ServerMessage.kt             — sealed class: all server → client messages
    ClientMessage.kt             — sealed class: all client → server messages

server/
  src/main/kotlin/.../
    websocket/
      WebSocketHandler.kt       — message routing, village action handling
      ConnectionManager.kt      — keep-alive monitoring, connection tracking
      TimerMonitor.kt           — background timer checks, push events

composeApp/
  src/commonMain/.../
    network/
      GameSocketManager.kt      — WebSocket connection singleton
      GameSocketClient.kt       — protocol handling, keep-alive, event flow
```

---
---

# Galdria WebSocket Reference

Everything below documents how the Galdria project (sister project) implements WebSockets. This serves as a reference and proven pattern for TamaHero's implementation.

## Galdria Architecture Overview

```
┌──────────────┐         WSS          ┌──────────────┐
│  KMP Client  │ ◄──────────────────► │  Ktor Server │
│  (Compose)   │   JSON over WS       │  (Netty)     │
└──────────────┘                       └──────────────┘
     │                                       │
     │ Ktor Client WebSockets                │ Ktor Server WebSockets
     │ Platform engines:                     │ Port 8080
     │  - Android: CIO                       │ Reverse proxy → WSS
     │  - iOS: Darwin                        │
     │  - Desktop: CIO                       │
     │  - Web: JS                            │
```

- **Transport:** WebSocket (RFC 6455), JSON text frames
- **Endpoint:** `/ws` with optional `?deviceId=` query parameter
- **Production URL:** `wss://galdria.com/ws`
- **Max frame size:** 64 KB
- **Protocol version:** 6 (sent in the `connected` message so clients can verify compatibility)

---

## Server Setup (Ktor)

### Installation & Configuration

In `Application.kt`:

```kotlin
fun main() {
    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
    server.start(wait = true)
}

fun Application.module() {
    install(WebSockets) {
        pingPeriod = 15.seconds    // Server sends ping every 15s
        timeout = 15.seconds       // Connection drops if no pong within 15s
        maxFrameSize = 65536       // 64 KB
        masking = false            // Server-to-client frames are unmasked
    }

    routing {
        webSocket("/ws") {
            val deviceId = call.request.queryParameters["deviceId"]
            webSocketHandler.handleConnection(this, deviceId)
        }
    }
}
```

### Connection Handler

The `RuneclashWebSocketHandler` manages the lifecycle of each connection:

```kotlin
suspend fun handleConnection(
    session: WebSocketSession,
    deviceId: String? = null,
) {
    val playerId = registerConnection(session, deviceId)

    try {
        for (frame in session.incoming) {
            when (frame) {
                is Frame.Text -> handleMessage(playerId, frame.readText())
                is Frame.Close -> { /* log close */ }
                else -> {}
            }
        }
    } catch (e: Exception) {
        logger.error("WebSocket error for player {}", playerId, e)
    } finally {
        // Cleanup on disconnect
        messageWindows.remove(playerId)
        matchmakingQueue.removeFromQueue(playerId)
        privateRoomManager.handlePlayerDisconnected(playerId)
        connectionManager.handleConnectionClosed(playerId)
    }
}
```

Key points:
- Each connection gets a unique `playerId`
- The `finally` block ensures cleanup even on unexpected disconnects
- The `deviceId` parameter links connections to physical devices for reconnection

---

## Client Setup (Ktor KMP)

### Socket Manager

`GameSocketManager` is a singleton that wraps Ktor's WebSocket client:

```kotlin
object GameSocketManager {
    private val client = HttpClient {
        install(WebSockets)
    }

    private var session: WebSocketSession? = null
    private var sendChannel: Channel<String>? = null

    suspend fun connect(
        url: String,
        onMessage: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit,
    ) {
        sendChannel = Channel(Channel.BUFFERED)

        CoroutineScope(Dispatchers.Default).launch {
            try {
                client.webSocket(url) {
                    session = this

                    // Sender coroutine: reads from channel, sends to WS
                    launch {
                        for (message in sendChannel!!) {
                            send(Frame.Text(message))
                        }
                    }

                    // Receiver loop: reads from WS, calls callback
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> onMessage(frame.readText())
                            is Frame.Close -> { onClose(); break }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "WebSocket error")
            } finally {
                onClose()
            }
        }
    }

    fun send(message: String) {
        sendChannel?.trySend(message)
    }

    fun close() {
        sendChannel?.close()
        CoroutineScope(Dispatchers.Default).launch {
            session?.close()
        }
    }
}
```

### Why a Channel for Sending?

The `Channel<String>` decouples message production from sending:
- Game logic calls `send()` from any thread without blocking
- The sender coroutine serializes writes to the WebSocket session
- `trySend()` is non-blocking — if the channel is full, the message is dropped (acceptable for a buffered channel)

### Platform Engines

Each platform needs a specific HTTP client engine:
- **Android/Desktop (JVM):** `io.ktor:ktor-client-cio`
- **iOS:** `io.ktor:ktor-client-darwin`
- **Web/WASM:** `io.ktor:ktor-client-js`

These are configured in platform-specific `dependencies` blocks in `build.gradle.kts`.

---

## Protocol Design

### Serialization

All messages are JSON, serialized with `kotlinx.serialization`:

```kotlin
val protocolJson = Json {
    ignoreUnknownKeys = true      // Forward compatibility: ignore new fields
    encodeDefaults = true          // Always include default values
    isLenient = true               // Accept unquoted strings, trailing commas
    classDiscriminator = "#class"  // Polymorphism discriminator field
}
```

### Message Routing

Every message has a `type` field used for routing:

```json
{"type": "cast_spell", "cardInstanceId": "abc123", "discardCardInstanceIds": []}
```

On the server, the `type` field determines which handler processes the message. On the client, it determines which state update to apply.

### Sealed Classes

Messages are modeled as sealed class hierarchies:

```kotlin
@Serializable
sealed class RuneclashClientMessage {
    abstract val type: String

    @Serializable
    @SerialName("cast_spell")
    data class CastSpell(
        override val type: String = "cast_spell",
        val cardInstanceId: String,
        val discardCardInstanceIds: List<String> = emptyList()
    ) : RuneclashClientMessage()

    // ... more message types
}
```

### What Makes This a Good Protocol

1. **Type-tagged JSON** — Every message is self-describing via the `type` field. No need for positional parsing or binary headers.
2. **Forward compatible** — `ignoreUnknownKeys = true` means old clients don't break when the server adds new fields.
3. **Version negotiated** — The `connected` message includes `protocolVersion`, so the client can detect mismatches early.
4. **Sealed class exhaustiveness** — Kotlin's `when` expressions on sealed classes catch unhandled message types at compile time.
5. **Event-driven + pull-based** — Most state changes are pushed as events. The client can also pull full state with `request_state` (useful after reconnection).

---

## Message Types Reference

### Client → Server

| Category | Type | Key Fields | Purpose |
|----------|------|------------|---------|
| **Connection** | `keep_alive` | — | Heartbeat |
| **Matchmaking** | `join_matchmaking` | `deckId`, `authToken?` | Enter queue |
| | `leave_matchmaking` | — | Leave queue |
| **Game Actions** | `cast_spell` | `cardInstanceId`, `discardCardInstanceIds` | Play a spell card |
| | `set_reaction` | `cardInstanceId` | Set a face-down reaction |
| | `reveal_reaction` | — | Reveal reaction to counter |
| | `pass_reaction` | — | Decline to react |
| | `pass_turn` | — | End turn |
| | `surrender` | — | Forfeit match |
| | `draw` | — | Draw a card |
| **Equipment** | `play_equipment` | `cardInstanceId` | Play equipment card |
| | `activate_equipment` | `discardCardInstanceIds` | Use equipment ability |
| | `skip_equipment_activation` | — | Skip activation phase |
| **Chain/Deck** | `place_reaction_from_chain` | `cardInstanceId` | Chain reaction placement |
| | `skip_chain_reaction` | — | Skip chain |
| | `apply_deck_effect` | `cardInstanceId` | Apply deck effect |
| | `skip_deck_effects` | — | Skip deck effects |
| **Abilities** | `activate_class_ability` | `discardCardInstanceIds` | Use class ability |
| | `activate_race_ability` | — | Use race ability |
| **State** | `request_state` | — | Pull full game state |
| **Reconnection** | `rejoin_match` | `matchId?`, `playerId?`, `authToken?` | Rejoin after disconnect |
| **Rematch** | `request_rematch` | — | Ask for rematch |
| | `accept_rematch` | — | Accept rematch |
| | `decline_rematch` | — | Decline rematch |
| **Private Rooms** | `create_private_room` | `deckId`, `authToken?` | Create a room |
| | `join_private_room` | `roomCode`, `deckId`, `authToken?` | Join by code |
| | `leave_private_room` | — | Leave room |

### Server → Client

| Category | Type | Key Fields | Purpose |
|----------|------|------------|---------|
| **Connection** | `connected` | `playerId`, `protocolVersion` | Confirm connection |
| **Matchmaking** | `matchmaking_joined` | `queuePosition` | Confirm queue entry |
| | `matchmaking_left` | — | Confirm queue exit |
| | `matchmaking_error` | `reason`, `details` | Queue error |
| | `match_found` | `matchId`, `opponent`, `yourHand`, `youGoFirst`, `yourLife` | Match starts |
| **Turn Flow** | `your_turn` | `turnNumber`, `phase`, `drewCard?` | Your turn begins |
| | `opponents_turn` | `turnNumber`, `phase` | Opponent's turn |
| **Combat** | `spell_cast` | `byYou`, `cardName`, `damage`, `element` | Spell was played |
| | `reaction_window` | `pendingSpell`, `pendingDamage`, `deadlineTimestamp` | React now |
| | `spell_resolved` | `cardName`, `damageDealt`, `wasCountered`, `effects` | Spell outcome |
| | `spell_countered` | `cardName`, `byReaction` | Spell was countered |
| **State** | `state_update` | 20+ fields (life, hand, buffs, etc.) | Incremental sync |
| | `full_state` | All state fields + match metadata | Full sync (reconnection) |
| **Damage/Life** | `damage_taken` | — | HP changed |
| | `life_healed` | — | HP restored |
| **Cards** | `card_drawn` | — | Card added to hand |
| | `card_discarded` | — | Card removed |
| | `hand_discarded` | — | Whole hand discarded |
| | `hand_revealed` | — | Hand shown to opponent |
| **Status** | `buff_gained` | — | Positive effect applied |
| | `debuff_gained` | — | Negative effect applied |
| | `shield_gained` | — | Shield applied |
| **Equipment** | `equipment_played` | — | Equipment placed |
| | `equipment_activated` | — | Equipment used |
| | `equipment_destroyed` | — | Equipment removed |
| **Reactions** | `reaction_set` | — | Reaction placed face-down |
| | `reaction_revealed` | — | Reaction flipped |
| | `reaction_destroyed` | — | Reaction removed |
| **Traps** | `trap_planted` | — | Trap set |
| | `trap_triggered` | — | Trap activated |
| **Disconnection** | `opponent_disconnected` | `reconnectTimeoutSeconds` | Opponent went offline |
| | `opponent_reconnected` | — | Opponent came back |
| | `rejoin_success` | `matchId` | Reconnection worked |
| | `rejoin_failed` | `reason` | Reconnection failed |
| **Game End** | `game_over` | `winner`, `reason`, `finalState`, `seriesScore` | Match ended |
| **Rematch** | `rematch_requested` | — | Opponent wants rematch |
| | `rematch_accepted` | — | Rematch confirmed |
| | `rematch_declined` | — | Rematch rejected |
| **Private Rooms** | `private_room_created` | — | Room ready |
| | `private_room_waiting` | — | Waiting for opponent |
| | `private_room_left` | — | Player left room |
| | `private_room_cancelled` | — | Room closed |
| | `private_room_error` | — | Room error |
| **Errors** | `invalid_action` | `reason`, `details`, `actionType` | Bad request |

---

## Connection Lifecycle

```
Client                                    Server
  │                                          │
  │──── WS handshake (/ws?deviceId=X) ─────►│
  │                                          │ registerConnection()
  │◄──── connected {playerId, version} ──────│ startKeepAliveMonitoring()
  │                                          │
  │──── keep_alive (every 10s) ─────────────►│ updateKeepAlive()
  │                                          │
  │──── join_matchmaking {deckId} ──────────►│
  │◄──── matchmaking_joined {position} ──────│
  │                                          │ ... waiting for opponent ...
  │◄──── match_found {matchId, hand, ...} ───│
  │                                          │
  │ ◄──► game messages back and forth ──► │
  │                                          │
  │◄──── game_over {winner, reason, ...} ────│
  │                                          │
  │──── close ──────────────────────────────►│ cleanup in finally block
```

### Client Connection Flow

```kotlin
suspend fun connect(serverUrl: String) {
    _state.value = _state.value.copy(connectionState = CONNECTING)

    val urlWithDeviceId = if (DeviceIdStorageProvider.isInitialized()) {
        val deviceId = DeviceIdStorageProvider.instance.getOrCreateDeviceId()
        "$serverUrl?deviceId=$deviceId"
    } else {
        serverUrl
    }

    GameSocketManager.connect(
        url = urlWithDeviceId,
        onMessage = { message -> scope.launch { handleMessage(message) } },
        onError = { error ->
            _state.value = _state.value.copy(
                connectionState = DISCONNECTED,
                showYouDisconnectedNotification = wasInMatch
            )
        },
        onClose = {
            _state.value = _state.value.copy(
                connectionState = DISCONNECTED,
                showYouDisconnectedNotification = wasInMatch
            )
        }
    )
    startKeepAlive()
    startStatePoll()
}
```

### Server Cleanup (finally block)

When a connection ends (gracefully or not), the server:
1. Removes rate-limit tracking (`messageWindows`)
2. Removes from matchmaking queue
3. Notifies private room manager
4. Notifies connection manager → starts 30s reconnection timer

---

## Heartbeat & Keep-Alive

There are three layers of liveness detection:

### 1. Ktor Ping-Pong (Transport Layer)

Built into the WebSocket protocol. Configured server-side:
- **Ping period:** 15 seconds
- **Timeout:** 15 seconds
- Ktor handles this automatically — no application code needed

### 2. Application Keep-Alive (Client → Server)

The client sends a `keep_alive` message every 10 seconds:

```kotlin
private fun startKeepAlive() {
    keepAliveJob = scope.launch {
        while (isActive) {
            delay(10_000)
            try {
                sendMessage(RuneclashClientMessage.KeepAlive())
            } catch (e: Exception) {
                // Swallow errors — the connection closing will be caught elsewhere
            }
        }
    }
}
```

### 3. Server Keep-Alive Monitoring

The server checks each player every 5 seconds:

```kotlin
private fun startKeepAliveMonitoring(playerId: String) {
    CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
        while (connections.containsKey(playerId)) {
            delay(5_000)
            connections[playerId]?.let { conn ->
                val elapsed = Clock.System.now().toEpochMilliseconds() - conn.lastKeepAlive
                if (elapsed > 30_000 && !conn.isDisconnected) {
                    handleDisconnection(playerId)
                }
            }
        }
    }
}
```

### Why Three Layers?

- **Ktor ping-pong** detects dead TCP connections (e.g., network cable unplugged)
- **Application keep-alive** works even when the WebSocket protocol layer is healthy but the application is frozen
- **Server monitoring** catches cases where the client stops sending keep-alives but the TCP connection hasn't timed out yet

### Timing Summary

| Mechanism | Interval | Timeout |
|-----------|----------|---------|
| Ktor ping-pong | 15s | 15s |
| Client keep-alive | 10s | — |
| Server keep-alive check | 5s | 30s |
| Reconnection window | — | 30s |
| State polling | 5s | — |

---

## Reconnection & Recovery

### Overview

When a player disconnects mid-match, they have **30 seconds** to reconnect and rejoin. The opponent is notified and the match is paused.

### Server-Side: ConnectionManager

```kotlin
data class PlayerConnection(
    val playerId: String,
    val session: WebSocketSession,
    val deviceId: String? = null,
    var lastKeepAlive: Long = Clock.System.now().toEpochMilliseconds(),
    var disconnectionJob: Job? = null,     // 30s timer
    var monitoringJob: Job? = null,        // Keep-alive checker
    var isDisconnected: Boolean = false,   // Temporary disconnect flag
)
```

#### Disconnection Flow

```kotlin
private suspend fun handleDisconnection(playerId: String) {
    mutex.withLock {
        connections[playerId]?.let { conn ->
            if (!conn.isDisconnected) {
                conn.isDisconnected = true

                // Start 30-second countdown
                conn.disconnectionJob = CoroutineScope(Dispatchers.Default).launch {
                    delay(30_000)
                    if (connections[playerId]?.isDisconnected == true) {
                        onPlayerFailedToReconnect?.invoke(playerId)
                        removeConnection(playerId)
                    }
                }
            }
        }
    }
    onPlayerDisconnected?.invoke(playerId)  // Notifies opponent
}
```

#### Reconnection via Keep-Alive

If the same WebSocket session recovers (e.g., brief network hiccup):

```kotlin
suspend fun updateKeepAlive(playerId: String) {
    mutex.withLock {
        connections[playerId]?.let { conn ->
            conn.lastKeepAlive = Clock.System.now().toEpochMilliseconds()
            if (conn.isDisconnected) {
                conn.isDisconnected = false
                conn.disconnectionJob?.cancel()   // Cancel the 30s timer
            }
        }
    }
    onPlayerReconnected?.invoke(playerId)
}
```

#### Reconnection via New Session (Rejoin)

If the client opens a brand new WebSocket connection:

```kotlin
suspend fun replaceSession(originalPlayerId: String, newSession: WebSocketSession, tempPlayerId: String) {
    mutex.withLock {
        connections[originalPlayerId]?.disconnectionJob?.cancel()
        connections[originalPlayerId] = PlayerConnection(
            playerId = originalPlayerId,
            session = newSession,
            isDisconnected = false,
        )
        connections.remove(tempPlayerId)  // Remove the temporary connection
    }
    startKeepAliveMonitoring(originalPlayerId)
}
```

### Client-Side: Automatic Rejoin

When the client connects (or reconnects), it checks for saved match credentials:

```kotlin
private fun handleConnected(messageText: String) {
    val msg = json.decodeFromString<RuneclashServerMessage.Connected>(messageText)

    // Method 1: Saved match credentials (matchId + playerId)
    if (MatchStorageProvider.isInitialized()) {
        val credentials = MatchStorageProvider.instance.loadMatchCredentials()
        if (credentials != null) {
            sendMessage(RuneclashClientMessage.RejoinMatch(
                matchId = credentials.matchId,
                playerId = credentials.playerId,
            ))
            return
        }
    }

    // Method 2: Auth token (server looks up active match for this user)
    val authToken = getAuthToken?.invoke()
    if (authToken != null) {
        sendMessage(RuneclashClientMessage.RejoinMatch(authToken = authToken))
        return
    }

    // No match to rejoin — ready for new match
    _state.value = _state.value.copy(connectionState = CONNECTED, playerId = msg.playerId)
}
```

#### After Successful Rejoin

```kotlin
private fun handleRejoinSuccess(messageText: String) {
    _state.value = _state.value.copy(
        connectionState = IN_MATCH,
        isReconnecting = false,
        showYouReconnectedNotification = true,
    ).addEvent(GameEventLog("Reconnected to match!", isPositive = true))

    requestState()  // Pull full game state to restore the UI
}
```

#### After Failed Rejoin

```kotlin
private fun handleRejoinFailed(messageText: String) {
    MatchStorageProvider.instance.clearMatchCredentials()
    _state.value = _state.value.copy(
        connectionState = CONNECTED,
        isReconnecting = false,
    )
}
```

### Reconnection Methods (Priority Order)

1. **Match credentials** — `matchId` + `playerId` stored locally when match starts
2. **Auth token** — OAuth token sent to server, which resolves it to an active match
3. **Device ID** — URL parameter `?deviceId=X`, tracked server-side in `deviceIdToPlayerId`

### Opponent's Perspective

```kotlin
// Opponent receives:
OpponentDisconnected(reconnectTimeoutSeconds = 30)
// UI shows: "Opponent disconnected! Waiting 30s for reconnection..."

// If reconnected:
OpponentReconnected()
// UI shows: "Opponent reconnected!"

// If timeout expires:
GameOver(winner = "you", reason = "opponent_disconnected")
```

---

## Authentication

Authentication is **not** done at the WebSocket handshake level. Instead, auth tokens are passed inside specific messages:

```kotlin
// In matchmaking:
JoinMatchmaking(deckId = "deck123", authToken = "eyJhbG...")

// In reconnection:
RejoinMatch(authToken = "eyJhbG...")

// In private rooms:
CreatePrivateRoom(deckId = "deck123", authToken = "eyJhbG...")
JoinPrivateRoom(roomCode = "ABCD", deckId = "deck123", authToken = "eyJhbG...")
```

The server validates tokens via `AuthService.validateToken()` to resolve the user identity.

**Why not authenticate at handshake?**
- WebSocket handshake doesn't natively support auth headers in all browser environments
- Passing tokens in messages allows unauthenticated play (guest mode) on the same endpoint
- Simpler reconnection — the new session authenticates itself via `rejoin_match`

---

## Game State Synchronization

### Strategy: Event-Driven + Pull-Based

The protocol uses **two complementary approaches**:

#### 1. Event-Driven (Push)

Most gameplay changes are pushed as individual events:

```
Server → spell_cast {cardName: "Fireball", damage: 5}
Server → damage_taken {amount: 5}
Server → state_update {yourLife: 15, opponentLife: 20, ...}
```

This is efficient — only the changed data is sent.

#### 2. Pull-Based (Request)

The client can request a full state snapshot at any time:

```
Client → request_state
Server → full_state {matchId, turnNumber, phase, isYourTurn, ...everything...}
```

This is used:
- After reconnection (to restore the entire UI)
- As a periodic consistency check (every 5 seconds if no other messages were received)

### State Polling

```kotlin
private fun startStatePoll() {
    statePollJob = scope.launch {
        while (isActive) {
            delay(5_000)
            if (!messageReceivedSinceLastPoll) {
                val connState = _state.value.connectionState
                if (connState == IN_MATCHMAKING || connState == IN_MATCH) {
                    sendMessage(RuneclashClientMessage.RequestState())
                }
            }
            messageReceivedSinceLastPoll = false
        }
    }
}
```

This acts as a safety net: if the push mechanism misses a message, the poll will catch the discrepancy within 5 seconds.

### Key State Messages

**`state_update`** — Incremental, sent after game actions:
```kotlin
StateUpdate(
    yourLife: Int,
    opponentLife: Int,
    yourHand: List<CardInfoData>,
    yourDeckSize: Int,
    opponentHandSize: Int,
    opponentDeckSize: Int,
    yourShields: List<ShieldData>,
    yourBuffs: List<String>,
    yourDebuffs: List<String>,
    opponentBuffs: List<String>,
    opponentDebuffs: List<String>,
    yourSetReaction: Boolean,
    opponentSetReaction: Boolean,
    yourEquipment: CardInfoData?,
    opponentEquipmentName: String?,
    hasPlayableCards: Boolean,
    // ... class/race ability state, etc.
)
```

**`full_state`** — Complete snapshot, sent on reconnection/request:
```kotlin
FullState(
    // Everything from StateUpdate, plus:
    matchId: String,
    turnNumber: Int,
    phase: String,
    isYourTurn: Boolean,
    pendingSpell: PendingSpellData?,
    seriesScore: SeriesScoreData,
    opponentName: String,
)
```

---

## Rate Limiting

The server limits WebSocket messages to prevent abuse:

```kotlin
private companion object {
    const val WS_MESSAGE_LIMIT = 60      // Max messages per window
    const val WS_WINDOW_MS = 60_000L     // 60-second window
}

private fun isMessageAllowed(playerId: String): Boolean {
    val now = System.currentTimeMillis()
    var allowed = true
    messageWindows.compute(playerId) { _, existing ->
        if (existing == null || now - existing.start > WS_WINDOW_MS) {
            MessageWindow(now, 1)
        } else {
            val newCount = existing.count + 1
            if (newCount > WS_MESSAGE_LIMIT) allowed = false
            MessageWindow(existing.start, newCount)
        }
    }
    return allowed
}
```

Violators receive:
```json
{
  "type": "invalid_action",
  "reason": "rate_limited",
  "details": "Too many messages. Please slow down.",
  "actionType": "cast_spell"
}
```

**60 messages per 60 seconds** is generous for a turn-based card game — normal play generates maybe 5-10 messages per minute. The limit exists to catch misbehaving clients or bots.

---

## Dependencies

### Server (`build.gradle.kts`)

```kotlin
implementation("io.ktor:ktor-server-websockets-jvm:3.3.3")
implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.3.3")
implementation("io.ktor:ktor-server-content-negotiation-jvm:3.3.3")
implementation(libs.ktor.serverCore)
implementation(libs.ktor.serverNetty)
```

### Client (`composeApp/build.gradle.kts`)

```kotlin
// Shared
commonMain.dependencies {
    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.clientWebsockets)
}

// Platform engines
androidMain.dependencies { implementation(libs.ktor.clientCio) }
jvmMain.dependencies { implementation(libs.ktor.clientCio) }
iosMain.dependencies { implementation(libs.ktor.clientDarwin) }
jsMain.dependencies { implementation(libs.ktor.clientJs) }
```

---

## Design Principles

### 1. Type Safety via Sealed Classes

Every message is a typed Kotlin data class. The compiler enforces exhaustive handling:

```kotlin
when (message) {
    is SpellCast -> handleSpellCast(message)
    is YourTurn -> handleYourTurn(message)
    // Compiler error if a type is missing
}
```

### 2. Forward Compatibility

`ignoreUnknownKeys = true` means:
- The server can add new fields to messages without breaking old clients
- The protocol version in `connected` allows graceful version negotiation
- New message types are logged and ignored rather than crashing

### 3. Information Hiding

The server never sends the opponent's private state:
- `yourHand` contains full card data; the opponent only sees `opponentHandSize`
- `yourEquipment` has full details; opponent sees only `opponentEquipmentName`
- Reactions are hidden until revealed

### 4. Idempotent State Recovery

After any disconnect, the client can call `request_state` and receive a `full_state` that fully describes the current game. No need to replay events — the state snapshot is self-contained.

### 5. Graceful Degradation

- Keep-alive failures don't crash — errors are swallowed
- State polling acts as a safety net for missed events
- Multiple reconnection methods (credentials, auth token, device ID) provide fallbacks

### 6. Simple Singleton Pattern

`GameSocketManager` is a singleton — there's only ever one WebSocket connection. This avoids complexity around multiple connections, connection pools, or session management on the client side.

### 7. Separation of Transport and Protocol

- `GameSocketManager` handles raw WebSocket frames
- `RuneclashGameClient` handles message serialization and game logic
- `RuneclashProtocol.kt` defines the message types

This separation means swapping transport (e.g., to HTTP long-polling for a fallback) wouldn't require changing the game logic.
