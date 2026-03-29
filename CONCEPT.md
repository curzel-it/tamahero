# TamaHero - Sci-Fi Colony Builder

A Clash of Clans / OGame hybrid — multiplayer base builder with 1-bit pixel art, set in a sci-fi universe.
Kotlin Multiplatform targeting Desktop, Android, iOS, and Web (WASM).

## Setting

You command a colony on a remote planet. Mine resources, build defenses, train troops, and raid rival colonies for loot — all while fending off hostile scout parties and natural disasters.

---

## Core Loop

1. **Build** your colony (place/upgrade buildings on a 20x20 grid)
2. **Produce** resources over real time (metal, crystal, deuterium)
3. **Train** troops at the barracks
4. **Attack** other players' colonies for loot and trophies
5. **Defend** your base with lasers, cannons, walls, and traps
6. Repeat

---

## Visual & Feel

- **Grid**: 20x20 tiles
- **Camera**: Top-down with zoom/pan
- **Art**: 1-bit pixel art (black on white base, accent colors for context)
- **Buildings**: 16x16 to 32x32 pixel sprites
- **Troops**: 8x8 to 16x16 pixel sprites
- **Animations**: minimal frame count (2-4 frames)

---

## Resources

### Farmable Resources
| Resource | Producer | Storage | Notes |
|----------|----------|---------|-------|
| **Metal** | Metal Mine | Metal Storage | Primary building material |
| **Crystal** | Crystal Mine | Crystal Storage | Advanced building material (unlocks TH2) |
| **Deuterium** | Deuterium Synthesizer | Deuterium Storage | Rare resource (unlocks TH2) |

### Currency
- **Credits** — universal currency, unlimited storage, no dedicated producer
- Earned through PvP loot, event rewards, and initial grant

### Base Storage
- **Command Center** provides base-level storage for metal, crystal, and deuterium at all levels
- Additional storage buildings increase capacity

---

## Buildings

### Economy
| Building | Role | TH Req |
|----------|------|--------|
| Command Center | Town hall + base storage | - |
| Metal Mine | Produces metal | 1 |
| Crystal Mine | Produces crystal | 2 |
| Deuterium Synthesizer | Produces deuterium | 2 |
| Metal Storage | Stores metal | 1 |
| Crystal Storage | Stores crystal | 2 |
| Deuterium Storage | Stores deuterium | 2 |
| Robotics Factory | Provides workers (builders) | 1 |

### Military
| Building | Role | TH Req |
|----------|------|--------|
| Barracks | Trains troops | 1 |
| Hangar | Houses troops (capacity) | 1 |

### Defenses
| Building | Role | TH Req |
|----------|------|--------|
| Gauss Cannon | Single target, high damage | 1 |
| Light Laser | Fast attack, long range | 1 |
| Heavy Laser | Stronger laser, higher damage | 3 |
| Missile Launcher | Splash damage, min range | 2 |
| Ion Cannon | Splash damage, medium range | 2 |
| Plasma Cannon | Highest damage tier | 5 |
| Wall | Blocks troop pathing (1x1) | 1 |
| Shield Dome | Absorbs damage across buildings | 2 |

### Traps (hidden, 1x1, one-time use)
| Building | Effect |
|----------|--------|
| Land Mine | Burst AOE damage on trigger |
| Gravity Well | Instant-kills light troops |
| Nova Bomb | Massive burst AOE |

---

## Troops

| Troop | Role | Targeting |
|-------|------|-----------|
| Marine | Balanced infantry | Nearest |
| Sniper | Ranged, fragile | Nearest |
| Engineer | Wall breaker (10x wall damage) | Walls first |
| Juggernaut | Heavy tank | Defenses first |
| Drone | Fast resource raider | Resources first |
| Spectre | Ranged splash, glass cannon | Nearest |
| Gunship | Heavy air, splash | Nearest |

Training requires Barracks (completed). Troop capacity requires Hangar.
Academy level gates max troop level.

---

## Combat

### PvP
- **Matchmaking**: trophy-based (±200 range), requires 3+ buildings, shields checked
- **Deployment**: tap map edges to place troops
- **Battle duration**: 3 minutes max
- **Stars**: 1 = 50% destruction, 2 = Command Center destroyed, 3 = 100%
- **Loot**: 20% of defender's resources available, scales with destruction %
- **Trophies**: gained/lost based on stars and trophy difference
- **Shields**: 30%+ destruction = 12h, 60%+ = 14h, 90%+ = 16h
- **Attacking breaks attacker's shield**

### PvE Events (server-driven)
Random events that test defenses. Player does NOT deploy troops.

**Disasters (instant):**
- Earthquake — random buildings take 20-50% HP damage
- Ion Storm — edge buildings take damage

**Battle Events (troops spawn at edges):**
- Scout Party, Battle, Raid, Siege, Invasion (escalating difficulty)

**Outcomes:**
- Success (<50% destruction) → bonus rewards
- Failure → debris recovery (30% of lost value)
- Post-battle: defenses auto-rebuild at 70% chance

---

## Game Pacing

- **Early game (TH1-2)**: seconds to 2 minutes per build
- **Mid game (TH3-4)**: 5-30 minutes per build
- **Late game (TH5+)**: 1-4 hours max

Starting resources: 500 credits, 500 metal, 500 crystal, 250 deuterium.

---

## Implementation Status

### Done
- [x] Canvas rendering with camera/zoom and sprite system
- [x] Building placement, upgrade, move, demolish, cancel, speed-up
- [x] Resource production (server-authoritative, offline accumulation)
- [x] Storage capacity with Command Center base storage
- [x] Worker system (Robotics Factory)
- [x] Troop training queue with real-time timers
- [x] 7 troop types with 6 levels each
- [x] 6 defense buildings + walls + shield dome
- [x] 3 trap types
- [x] PvP: matchmaking, battle, deploy, surrender, loot, trophies, shields
- [x] PvE: disasters + battle events with wave spawning
- [x] Leaderboard (top 50 by trophies)
- [x] Auth: username/password + Google/Apple social login
- [x] WebSocket architecture (REST for auth only)
- [x] Push notifications (Firebase)
- [x] CLI client with full coverage
- [x] Compose UI with testTag accessibility IDs
- [x] 124 E2E tests (server + UI + CLI)
- [x] Web client (WASM)
- [x] Automated deploy pipeline

### Not Yet Built
- [ ] Guilds / clans
- [ ] Leagues and seasons
- [ ] Battle replays
- [ ] Energy resource (OGame-style)
- [ ] Sound / music
- [ ] Cosmetics
- [ ] Battle pass
- [ ] IAP / monetization

---

## Technical Architecture

- **Server**: Ktor (Kotlin), SQLite, WebSocket-only game protocol
- **Client**: Compose Multiplatform (Desktop/Android/iOS/WASM)
- **Auth**: JWT tokens, BCrypt passwords, Google/Apple ID token verification
- **State**: Server-authoritative, client renders optimistically
- **Deploy**: Local build → deploy repo → SSH pull on VPS

---

## What NOT to Build

- Builder Base / secondary village
- Loot boxes / gacha
- Pay-to-win troops
- Interstitial ads
- Aggressive push notifications
