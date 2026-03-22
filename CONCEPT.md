# TamaHero - 1-Bit Island Builder

A Clash of Clans-inspired multiplayer island builder with 1-bit pixel art style, set in the **Galdria** universe.
Kotlin Multiplatform targeting Desktop, Android, and iOS.

## Setting

You've claimed a small island in the Galdria archipelago. Draw magic circles to power your buildings, train warriors from across the races, and raid rival settlements for resources — all while keeping dragons and pirates at bay.

The Galdria universe provides:
- **Magic circles** as the visual/thematic backbone — buildings activate with visible circles, spells use circle animations
- **Multiple races** as distinct troop types with personality and lore (Humans, Elves, Dwarves, Orcs, Giants)
- **Dragons** as PvE raid bosses and world flavor
- **The archipelago** as the natural meta-map — each player's base is an island among many

---

## Core Loop

1. **Build** your island settlement (place/upgrade buildings)
2. **Collect** resources over real time (gold, wood, metal)
3. **Train** troops from across the races
4. **Attack** other players' islands or pirate bases for loot
5. **Defend** your island with walls, traps, and defensive buildings
6. Repeat

---

## Visual & Feel

- **Grid size**: 40x40 tiles. Starts at ~20x20 usable area, expands with Town Hall level
- **Camera**: Top-down with slight perspective tilt (classic Zelda / Graveyard Keeper style, NOT isometric). Fits 1-bit art better and differentiates from Clash
- **Colors**: Black on white base, with accent colors per context — red for damage/combat, blue for mana/magic effects. The 1-bit base is the visual identity; rare accent color pops hard
- **Art**: 1-bit pixel art
  - Buildings: ~16x16 to 32x32 pixel sprites
  - Troops: ~8x8 to 16x16 pixel sprites
  - Terrain: simple tile patterns
  - UI: pixel art styled panels and buttons
  - Animations: minimal frame count (2-4 frames)
  - Magic circles: visible glowing circles under active buildings (blue accent)

---

## Resources

### In-Game Resources (farmable)
- **Wood** — early game dominant (basic buildings, walls, barracks)
- **Gold** — mid game (upgrades, troops, trade)
- **Metal** — late game gate (defenses, advanced buildings, dwarven upgrades)

Each has a collector and a storage building:
- **Lumber Camp** → **Wood Storage**
- **Gold Mine** → **Gold Storage**
- **Forge / Smelter** → **Metal Storage**

### Premium Currency: Mana
- Rare and magical — canonically only "True Wizards" can store mana internally
- Purchasable with real money (IAP)
- Earnable in small amounts: clearing obstacles, achievements, PvE victories, dragon raid events
- Cannot be farmed like gold/wood/metal
- Used for: speed-ups, buying builders, purchasing resources, buying shields

---

## Game Pacing

- **Early game (TH1-2)**: seconds to 2 minutes per build
- **Mid game (TH3-4)**: 5-30 minutes per build
- **Late game (TH5)**: 1-4 hours max

Faster than Clash — tighter hooks for retention with a smaller initial player base. Can be slowed down later once volume grows.

---

## Features

### Phase 1 - Foundation (MVP)

**Target: "Build your island, defend against random events."**

#### 1.1 Island Grid & Building Placement
- [x] Canvas rendering with camera/zoom
- [x] Building placement with collision detection (server-validated)
- [x] Building upgrade, move, demolish, cancel construction, speed-up (mana)
- [ ] Tile-based grid rendering (40x40, ~20x20 usable at TH1)
- [ ] Building selection and info panel (UI)
- [ ] Drag to move buildings (edit mode, UI)

#### 1.2 Buildings
- [x] **Town Hall** — central building, gates all other buildings
- [x] **Lumber Camp** — produces wood over real time
- [x] **Gold Mine** — produces gold over real time
- [x] **Forge** — produces metal (unlocks at TH2)
- [x] **Wood/Gold/Metal Storage** — capacity-limited resource storage
- [x] **Barracks** — trains troops (queue-based, multiple barracks = parallel training)
- [x] **Army Camp** — stores trained troops (capacity-limited)

#### 1.3 Defenses (OGame/CoC Blend)
- [x] **Cannon** — single target, ground only
- [x] **Archer Tower** — single target, ground + air
- [x] **Mortar** — splash damage, minimum range, slow fire rate
- [x] **Wall** — blocks troop pathing (troops must destroy or path around)
- [x] **Spike Trap** — hidden, burst AOE damage on trigger, one-time use
- [x] **Spring Trap** — hidden, instant-kills light troops, one-time use
- [x] **Shield Dome** — absorbs damage across all buildings, breaks when depleted
- [x] Post-battle auto-rebuild: 70% of destroyed defenses rebuilt at full HP (OGame mechanic)
- [x] Trap rearming (manual, costs 50% of build price)

#### 1.4 Real-Time Progression
- [x] Buildings produce resources while offline (server-authoritative)
- [x] Building construction takes real time (timers)
- [x] Server calculates accumulated resources on login
- [x] Troop training queue with real-time timers

#### 1.5 Authentication & Networking
- [x] Username/password + social login (Google, Apple)
- [x] WebSocket-only architecture (REST only for auth)
- [x] Full village CRUD via WebSocket messages
- [x] Server-side validation of all actions
- [x] Timer monitoring with push events (building_complete, training_complete, resources_updated)
- [x] Save/load village state (SQLite, serialized JSON)

#### 1.6 PvE Events (SimCity-style)
Random server-driven events that test the player's defenses. Player does NOT deploy troops — defenses fight automatically. Events scale with Town Hall level.

**Natural Disasters (instant damage, no troops):**
- [x] **Earthquake** — random buildings take 20-50% HP damage
- [x] **Storm** — edge buildings take damage, some resources lost

**Enemy Raids (battle events, troops spawn at edge):**
- [x] **Scout Party** — small group of weak soldiers
- [x] **Battle** — mixed troops, waves/difficulty can be modulated via config

**Outcomes:**
- **Success** (<50% destruction) → bonus rewards (gold, wood, metal; mana for hard events)
- **Failure** (50%+ destruction) → debris recovery (30% of lost value as resources)
- Post-battle: defenses auto-rebuild 70%, shield granted based on destruction %

#### 1.7 Clients
- [x] CLI client with full feature coverage (build, upgrade, train, etc.)
- [x] CLI integration test suite (end-to-end via embedded server)
- [ ] Compose Multiplatform rendering (Desktop, Android, iOS)

---

### Phase 2 - Combat

#### 2.1 Troops
- [ ] **Barracks** building — trains troops
- [ ] Troop training queue (real-time)
- [ ] 4 troop types at launch (one per race):
  - **Human Soldier** — melee, balanced (the Barbarian)
  - **Elf Archer** — ranged, fragile (the Archer)
  - **Dwarf Sapper** — targets walls/defenses, tanky (Wall Breaker + Giant hybrid)
  - **Orc Berserker** — high DPS, targets strongest building instead of nearest (unique, no Clash analog)
- [ ] Each troop has: HP, DPS, movement speed, training cost, training time
- [ ] Army camp building (max troop capacity)
- [ ] Giant and Dragon troops unlock at TH4-5

#### 2.2 Combat System
- **Real-time simulated** — troops move and fight on screen (the dopamine hit)
- **Tap-to-place deployment** — deploy troops by tapping on the map edge (proven mechanic)
- **Grid-based pathing** — simpler to implement and validate server-side, fits tile aesthetic
- **Troop AI**: find nearest target, move toward it, attack until dead (Orc Berserker exception: targets strongest)

#### 2.3 Defenses
- [ ] **Cannon** — single-target, ground only
- [ ] **Archer Tower** — single-target, air + ground
- [ ] **Walls** — block troop pathing (wood early, metal late)
- [ ] **Traps** — hidden, one-time use, reset after triggered
- [ ] Defense buildings auto-target nearest enemy during attacks

#### 2.4 Attacking Other Players (PvP unlocks at TH2)
- [ ] Matchmaking: find a base to attack (similar trophy range)
- [ ] Pre-attack scout view (see their base layout)
- [ ] Battle ends when: all troops dead, all buildings destroyed, or 3-min timer
- [ ] Stars system: 1 star = 50% destruction, 2 = Town Hall destroyed, 3 = 100%
- [ ] Loot: steal percentage of opponent's resources
- [ ] Trophy gain/loss based on stars

#### 2.5 Being Attacked
- [ ] Shield system (scales with destruction %):
  - Under 30% destruction = 4h shield
  - 30-89% destruction = 8h shield
  - 90%+ destruction = 12h shield
- [ ] Village guard: 30-minute cooldown after shield expires before next attack
- [ ] Online players cannot be attacked
- [ ] Attack replays: watch how your base was attacked
- [ ] Defense log: list of recent attacks on your base

#### 2.6 Server - Combat API
- [ ] `POST /api/matchmaking/find` — find opponent
- [ ] `POST /api/attack/start` — begin attack (locks opponent base)
- [ ] `POST /api/attack/deploy` — deploy troop at position
- [ ] `POST /api/attack/end` — submit battle result
- [ ] Server validates battle results (anti-cheat)
- [ ] Battle replay storage

---

### Phase 3 - Social & Progression

#### 3.1 Island Guilds (Clans)
- [ ] Create/join a guild
- [ ] Guild chat
- [ ] Donate troops to guildmates
- [ ] Guild wars (coordinated attacks against another guild)

#### 3.2 Leagues & Leaderboards
- [ ] Trophy-based leagues (Bronze, Silver, Gold, Crystal, Champion)
- [ ] Season resets
- [ ] Global and local leaderboards
- [ ] League bonus loot

#### 3.3 Upgrades & Tech Tree
- [ ] 5 Town Hall levels at launch (expandable later)
- [ ] TH level gates all other building levels
- [ ] Each building has multiple upgrade levels
- [ ] Upgrade cost and time increase per level
- [ ] New buildings unlock at specific TH levels
- [ ] Troop upgrades via Laboratory building

#### 3.4 PvE Events
- [x] Periodic server-driven events (Earthquake, Storm, ScoutParty, Battle)
- [x] Survive = bonus rewards; failure = debris recovery
- [x] Admin API to trigger events for testing/demos

#### 3.5 Battle Pass (Season Pass)
- [ ] Free tier with basic rewards
- [ ] Premium tier with exclusive rewards
- [ ] Season duration: ~1 month
- [ ] Rewards: mana, resources, cosmetics

---

### Phase 4 - Monetization

#### 4.1 Premium Currency (Mana)
- [ ] Mana purchasable with real money (IAP)
- [ ] Mana packs at various price points
- [ ] Small amounts earnable in-game (obstacles, achievements, PvE, dragon raids)

#### 4.2 Mana Sinks
- [ ] Speed up construction timers (instant finish)
- [ ] Speed up troop training
- [ ] Buy additional builders (up to 4)
- [ ] Buy resources directly (gold/wood/metal)
- [ ] Buy shields (prevent attacks)

#### 4.3 Cosmetics
- [ ] Island skins/themes
- [ ] Hero skins
- [ ] Special building skins (seasonal events)

#### 4.4 Other Monetization
- [ ] Rewarded ads (added later, NOT at launch): watch ad for resource boost
- [ ] Starter packs (one-time purchase, high value)
- [ ] Event-exclusive offers
- [ ] Never: interstitial ads, loot boxes/gacha, pay-to-win troops, aggressive push notifications

---

## Data Model (Key Entities)

```
Player
  id, username, trophies, level, xp
  mana, gold, wood, metal
  shieldExpiry, lastLogin

Village
  playerId
  buildings: List<PlacedBuilding>

PlacedBuilding
  buildingType, level, x, y
  constructionEndTime (null if complete)
  lastCollectedAt (for resource buildings)
  hitPoints

BuildingType (config/static data)
  id, name, size (e.g. 3x3)
  levels: List<BuildingLevel>

BuildingLevel
  level, hp, cost (gold/wood/metal), buildTime
  productionRate (resource buildings)
  storageCapacity (storage buildings)
  damage, range, attackSpeed (defense buildings)

Troop
  type, level, hp, dps, speed, trainingCost, trainingTime

BattleLog
  attackerId, defenderId
  stars, lootGold, lootWood, lootMetal
  trophyChange, timestamp
  replay: List<BattleEvent>

Guild
  id, name, description
  members: List<GuildMember>
```

---

## Technical Notes

### Client-Server Architecture
- **Server is authoritative** for all game state (resources, timers, battles)
- Client sends actions, server validates and applies
- Client renders optimistically but corrects on server response
- All timestamps are server-side to prevent clock manipulation

### Real-Time Sync
- On app launch: fetch full island state from server
- Periodic sync every ~30s while app is open
- Push notifications for: attack completed, construction done, shield expiring (minimal, not spammy)

### Battle Simulation
- Battles run client-side for responsiveness
- Client sends deployment sequence to server
- Server replays the battle deterministically to validate result
- Troop AI: find nearest target, move toward it, attack until dead
- Grid-based pathing for deterministic replay validation

### Anti-Cheat
- Server validates: resource amounts, build costs, upgrade prerequisites
- Server validates: battle results by replaying troop deployment
- Rate limiting on all endpoints
- Signed API requests

---

## What NOT to Build

Features from Clash explicitly excluded:
- **Builder Base / secondary village** — splits attention, most players find it a chore
- **Clan War Leagues** — too complex for a small team to balance
- **Hero equipment / pet system** — feature bloat
- **Loot boxes / gacha** — regulatory risk, player hostility
- **Pay-to-win troops** — mana-exclusive troop types would kill competitive integrity

---

## Development Notes

- **Primary dev platform**: Desktop (JVM) — fastest iteration, no emulator needed
- **Sound/music**: Not at launch, add after PvP works. 1-bit game + moody chiptune = perfect fit
- **Test on Android/iOS weekly** during development

---

## Current Status

- [x] Project setup (KMP + Compose Multiplatform)
- [x] Canvas rendering with camera/zoom
- [x] Sprite system
- [x] Authentication (client + server)
- [ ] Everything else
