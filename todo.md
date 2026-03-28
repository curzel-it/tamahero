# TamaHero - TODO

## P0 — Can't properly playtest without these

### PvP feels blind
- [x] Battle timer visible in HUD (3 min countdown)
- [x] Train multiple at once — +1 and +5 buttons per troop type
- [x] Troop info panel — Tap troop name to see stats table (HP, DPS, speed, range, cost per level)

### Core loop friction
- [x] Confirmation dialogs — Demolish requires "Confirm" step
- [x] Connecting/loading state — "Connecting..." spinner shown while WebSocket connects

### PvE is disorienting
- [x] Wave countdown — "Next wave in Xs" shown between waves
- [x] Post-event damage summary — Damage report shown in event result (destroyed/damaged buildings with HP changes)

## P1 — Annoying during playtesting but survivable

### PvP quality of life
- [x] Pre-battle army preview — Your army composition shown in scout view
- [x] Loot preview during battle — Running loot total shown in battle HUD
- [x] "Search again" from result screen — "Attack Again" button next to "Return Home"
- [x] Attack cost warning — "Next (100 plasma)" shown on skip button

### Feedback gaps
- [x] In-app banner when construction completes — Blue toast: "Academy Lv2 complete!"
- [x] In-app banner when training completes — Blue toast: "Marine Lv2 trained!"
- [x] Event warning — Blue toast: "Incoming Raid!"

### Building workflow
- [x] Build queue — Build button disabled when no workers free (clear "Builders: X/Y" indicator)

## P2 — Would improve playtesting but not blocking

### Status visibility
- [x] Village stats summary — Trophies shown in HUD status row
- [x] Profile page — Account view shows CC level, trophies, troops, buildings, defense record

### Connection resilience
- [x] Auto-reconnect with exponential backoff (1s → 2s → 4s → ... → 30s max) on connection drop

### Push notifications (between sessions)
- [ ] Firebase project wiring — Add FCM SDK to iOS/Android apps
- [ ] "Your Academy is complete!" push
- [ ] "You're being attacked!" push
- [ ] "Storages full" push
- [ ] Shield expiring soon push

## P3 — Polish (not needed for playtesting)

### Rendering & animation
- [ ] Animations — Construction progress, production bubbles, upgrade sparkles
- [ ] Particle effects — Explosions, damage numbers, resource collection
- [ ] Troop movement animation — Smooth pathfinding visualization
- [ ] Building destruction animation
- [ ] Day/night cycle
- [ ] Weather effects during PvE events
- [ ] Mini-map for navigation on the 40x40 grid

### Audio
- [ ] Sound effects — Building, upgrade, training, battle
- [ ] Background music — Village theme, battle theme
- [ ] Notification sounds

### First-time experience
- [ ] Tutorial / onboarding
- [ ] Suggested build order highlights
- [ ] Tooltip hints on first load
- [ ] Starting quest chain
- [ ] Better default village (more starting buildings)

### Social
- [ ] Clans / guilds
- [ ] Clan chat
- [ ] Global chat
- [ ] Friend list
- [ ] Troop donations

### Progression & engagement
- [ ] Achievements / milestones
- [ ] Daily missions
- [ ] Season pass
- [ ] Story campaign

### Monetization
- [ ] Shop
- [ ] Premium currency purchase
- [ ] Cosmetic skins
- [ ] Battle pass

### Infrastructure
- [ ] Rate limiting
- [ ] Analytics
- [ ] Cloud saves / account migration
- [ ] Moderation tools

### Skip for now
- [ ] Replay / battle log
- [ ] Clan wars
- [ ] Trading between players
- [ ] Real-time co-op
- [ ] Base decorations
- [ ] Spell system
- [ ] Hero units
- [ ] Alliance wars / tournaments

## Done
- [x] Village building — Place, upgrade, move, demolish on 40x40 grid
- [x] Resource production — AlloyRefinery, CreditMint, Foundry
- [x] Resource storage — Silos, Vaults, Banks with capacity scaling
- [x] Workers — DroneStation controls concurrent builds
- [x] Troop training — Academy trains, Hangar sets capacity
- [x] Academy-level gating — Academy level → max troop level
- [x] PvP — Matchmaking, scouting, deploy, 3-star, loot, trophies, shields
- [x] PvE events — Raids, sieges, invasions with waves
- [x] Leaderboards — Trophy ranking
- [x] Defense log — Track past attacks
- [x] Build menu with categories
- [x] Building info panel (upgrade, move, demolish, collect, rearm)
- [x] Army overview (composition, queue, train)
- [x] PvP scout view, battle HUD, result screen
- [x] PvE event HUD + result screen
- [x] Resource bar with real-time updates
- [x] Floating text on resource changes
- [x] Building HP bars
- [x] Production indicators
- [x] Hover tooltips (desktop)
- [x] Offline progress summary
- [x] Collect All
- [x] Building upgrade preview (stat diffs)
- [x] Defense log viewer
- [x] Shield timer in HUD
- [x] Builder status in HUD
- [x] Storage capacity in resource bar (current/max, color warnings)
- [x] Training queue progress (time remaining)
- [x] Push notification framework (Android/iOS/desktop)
- [x] Auth (username/password, Google, Apple)
- [x] Shield system (12-16h after defense)
- [x] Sprite rendering, camera pan/zoom, grid overlay
- [x] Ghost placement preview, construction overlay
- [x] Battle timer (3 min countdown in PvP HUD)
- [x] Batch train (+1/+5 buttons)
- [x] Troop info panel (stats table, all 6 levels)
- [x] Demolish confirmation dialog
- [x] Connecting/loading spinner
- [x] PvE wave countdown
- [x] Pre-battle army preview in scout view
- [x] Loot preview during battle
- [x] Attack Again from result screen
- [x] Next opponent cost warning (100 plasma)
- [x] Construction/training/event banners (auto-dismiss toasts)
- [x] Trophies in HUD
- [x] Post-event damage summary (destroyed/damaged buildings in result modal)
- [x] Build button disabled when no workers free
- [x] Profile page (CC level, trophies, troops, buildings, defense record)
- [x] Auto-reconnect with exponential backoff
