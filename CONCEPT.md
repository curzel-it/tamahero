# TamaHero - 1-Bit City Builder

A Clash of Clans-inspired multiplayer city builder with 1-bit pixel art style.
Kotlin Multiplatform targeting Desktop, Android, and iOS.

---

## Core Loop

1. **Build** your village (place/upgrade buildings)
2. **Collect** resources over real time
3. **Train** troops
4. **Attack** other players' villages for loot
5. **Defend** your village with walls, traps, and defensive buildings
6. Repeat

---

## Features

### Phase 1 - Foundation (MVP)

#### 1.1 Village Grid & Building Placement
- [x] Canvas rendering with camera/zoom
- [ ] Tile-based grid system (e.g. 40x40)
- [ ] Building placement with collision detection
- [ ] Building selection and info panel
- [ ] Drag to move buildings (edit mode)

#### 1.2 Buildings - Resource Production
- [ ] **Town Hall** - central building, determines max level of everything
- [ ] **Gold Mine** - produces gold over real time
- [ ] **Elixir Collector** - produces elixir over real time
- [ ] **Gold Storage** - stores gold (capacity limit)
- [ ] **Elixir Storage** - stores elixir (capacity limit)

#### 1.3 Real-Time Progression
- [ ] Buildings produce resources while offline (server-authoritative)
- [ ] Building construction takes real time (timers)
- [ ] One builder available (can upgrade to more)
- [ ] Server calculates accumulated resources on login

#### 1.4 Authentication & Player Data
- [x] Username/password + social login (Google, Apple)
- [ ] Player profile (name, level, trophies)
- [ ] Save/load village state to server
- [ ] Sync village state on app launch

#### 1.5 Server - Core API
- [x] Auth routes
- [ ] `GET /api/village` - load player's village
- [ ] `PUT /api/village/build` - place a building
- [ ] `PUT /api/village/upgrade` - upgrade a building
- [ ] `PUT /api/village/move` - move a building
- [ ] `POST /api/village/collect` - collect resources from a building
- [ ] Server-side validation of all actions (anti-cheat)

---

### Phase 2 - Combat

#### 2.1 Troops
- [ ] **Barracks** building - trains troops
- [ ] Troop training queue (real-time)
- [ ] Troop types: Warrior (melee), Archer (ranged), Giant (tank)
- [ ] Each troop has: HP, DPS, movement speed, training cost, training time
- [ ] Army camp building (max troop capacity)

#### 2.2 Defenses
- [ ] **Cannon** - single-target, ground only
- [ ] **Archer Tower** - single-target, air + ground
- [ ] **Walls** - block troop pathing
- [ ] **Traps** - hidden, one-time use, reset after triggered
- [ ] Defense buildings auto-target nearest enemy during attacks

#### 2.3 Attacking Other Players
- [ ] Matchmaking: find a base to attack (similar trophy range)
- [ ] Pre-attack scout view (see their base layout)
- [ ] Deploy troops by tapping on the map edge
- [ ] Troops auto-path to nearest building and attack
- [ ] Battle ends when: all troops dead, all buildings destroyed, or 3-min timer
- [ ] Stars system: 1 star = 50% destruction, 2 = Town Hall destroyed, 3 = 100%
- [ ] Loot: steal percentage of opponent's resources
- [ ] Trophy gain/loss based on stars

#### 2.4 Being Attacked
- [ ] Shield system: 12h shield after being attacked (scales with destruction %)
- [ ] Attack replays: watch how your base was attacked
- [ ] Defense log: list of recent attacks on your base

#### 2.5 Server - Combat API
- [ ] `POST /api/matchmaking/find` - find opponent
- [ ] `POST /api/attack/start` - begin attack (locks opponent base)
- [ ] `POST /api/attack/deploy` - deploy troop at position
- [ ] `POST /api/attack/end` - submit battle result
- [ ] Server validates battle results (anti-cheat)
- [ ] Battle replay storage

---

### Phase 3 - Social & Progression

#### 3.1 Clans
- [ ] Create/join a clan
- [ ] Clan chat
- [ ] Donate troops to clanmates
- [ ] Clan wars (coordinated attacks against another clan)

#### 3.2 Leagues & Leaderboards
- [ ] Trophy-based leagues (Bronze, Silver, Gold, Crystal, Champion)
- [ ] Season resets
- [ ] Global and local leaderboards
- [ ] League bonus loot

#### 3.3 Upgrades & Tech Tree
- [ ] Town Hall levels (1-10) gate all other building levels
- [ ] Each building has multiple upgrade levels
- [ ] Upgrade cost and time increase per level
- [ ] New buildings unlock at specific Town Hall levels
- [ ] Troop upgrades via Laboratory building

---

### Phase 4 - Monetization

#### 4.1 Premium Currency (Gems)
- [ ] Gems purchasable with real money (IAP)
- [ ] Gem packs at various price points

#### 4.2 Gem Sinks
- [ ] Speed up construction timers (instant finish)
- [ ] Speed up troop training
- [ ] Buy additional builders (up to 5)
- [ ] Buy resources directly (gold/elixir)
- [ ] Buy shields (prevent attacks)

#### 4.3 Battle Pass (Season Pass)
- [ ] Free tier with basic rewards
- [ ] Premium tier with exclusive rewards
- [ ] Season duration: ~1 month
- [ ] Rewards: gems, resources, cosmetics, magic items

#### 4.4 Cosmetics
- [ ] Village skins/themes
- [ ] Hero skins
- [ ] Special building skins (seasonal events)

#### 4.5 Other Monetization
- [ ] Rewarded ads: watch ad for small gem/resource bonus
- [ ] Starter packs (one-time purchase, high value)
- [ ] Event-exclusive offers

---

## Data Model (Key Entities)

```
Player
  id, username, trophies, level, xp
  gems, gold, elixir
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
  level, hp, cost, buildTime
  productionRate (resource buildings)
  storageCapacity (storage buildings)
  damage, range, attackSpeed (defense buildings)

Troop
  type, level, hp, dps, speed, trainingCost, trainingTime

BattleLog
  attackerId, defenderId
  stars, lootGold, lootElixir
  trophyChange, timestamp
  replay: List<BattleEvent>

Clan
  id, name, description
  members: List<ClanMember>
```

---

## Technical Notes

### Client-Server Architecture
- **Server is authoritative** for all game state (resources, timers, battles)
- Client sends actions, server validates and applies
- Client renders optimistically but corrects on server response
- All timestamps are server-side to prevent clock manipulation

### Real-Time Sync
- On app launch: fetch full village state from server
- Periodic sync every ~30s while app is open
- Push notifications for: attack completed, construction done, shield expiring

### Battle Simulation
- Battles run client-side for responsiveness
- Client sends deployment sequence to server
- Server replays the battle deterministically to validate result
- Troop AI: find nearest target, move toward it, attack until dead

### Anti-Cheat
- Server validates: resource amounts, build costs, upgrade prerequisites
- Server validates: battle results by replaying troop deployment
- Rate limiting on all endpoints
- Signed API requests

---

## Art Style

1-bit pixel art (black and white only):
- Buildings: ~16x16 to 32x32 pixel sprites
- Troops: ~8x8 to 16x16 pixel sprites
- Terrain: simple tile patterns
- UI: pixel art styled panels and buttons
- Animations: minimal frame count (2-4 frames)

---

## Current Status

- [x] Project setup (KMP + Compose Multiplatform)
- [x] Canvas rendering with camera/zoom
- [x] Sprite system
- [x] Authentication (client + server)
- [ ] Everything else
