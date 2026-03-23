# TamaHero - TODO

## Rendering & Visual Polish
- [x] Troop rendering — Troops now drawn on canvas during battles using sprites or colored circles with HP bars.

## Game Mechanics
- [x] Mana generation — ManaWell building produces mana, ManaStorage stores it (TH2 required).
- [x] SpringTrap — Now has triggerRadius and insta-kills lightweight troops (HumanSoldier, ElfArcher, Goblin).
- [x] Building move in UI — "Move" button in BuildingInfoView enters ghost-placement mode for relocation.
- [x] Building queue — BuilderHut building controls worker count. 1 worker = 1 concurrent construction. Default village starts with 1 BuilderHut.
- [x] Hero unit — Tamagotchi-style hero with hunger, happiness, XP, leveling. Feed (gold) and train (mana) actions. Auto-participates in events.
- [x] PvP / Raiding — Clash of Clans-style PvP: matchmaking by trophies, deploy troops on grid edges, 3-min battles, stars (50%/TH/100%), loot stealing, trophy system, shields (12h/14h/16h), attacker loses shield on attack, defense log, real-time defender notification.
- [x] Leaderboards — Trophy-based leaderboard (top 50 + your rank) via WebSocket message. CLI: `leaderboard` / `lb`.

## Push Notifications (Firebase)
- [x] Firebase scaffolding — Server-side FCM via firebase-admin SDK. Device token registration (`RegisterDevice`/`UnregisterDevice` messages). Auto-cleanup of stale tokens.
- [x] Notification types — Under attack, defense result, PvE event start/end, building complete, training complete.
- [x] Offline-only delivery — Push notifications only sent when user has no active WebSocket connection. Testable via `MockPushNotificationService`.
- [ ] Firebase project setup — Project `runeclash-11637`. Service account JSON in place. Still need to add Firebase Messaging SDK to iOS/Android apps and send `RegisterDevice` on token refresh.

## UI/UX
- [x] Army overview in HUD — "Army" button shows army composition, training queue, and train buttons.
- [x] Production indicators — Small colored dot above producer buildings with >10min accumulated resources.
- [x] Building HP bars — HP bar below damaged buildings (green/yellow/red based on HP ratio).
- [x] Resource change feedback — Floating "+500 gold" / "-200 wood" text on resource changes, fading out over 2s.
- [x] Bulk actions — "Collect" (collect all) button in HUD, "Rearm" / "Rearm All" buttons in trap info view.

## Quality of Life
- [x] Building info on hover — Desktop shows tooltip (name, level, HP, production/damage) when hovering over buildings.
- [x] Settings screen — Toggle grid and FPS display.
- [x] Offline progress summary — "While you were away..." overlay on reconnect after >5 min gap.

## Skip for now
- [ ] Particle effects — Explosions, magic, resource collection sparkles, damage numbers.
- [ ] Animations — Everything is static sprites. Construction progress, production bubbles, upgrade sparkles, trap triggers, building destruction would all add life.
- [ ] Day/night cycle — Tint the village based on time of day.
- [ ] Weather effects — Rain during storms, screen shake during earthquakes.
- [ ] Achievements / milestones — No progression tracking beyond TH level.
- [ ] Story / campaign — No narrative or campaign missions.
- [ ] Tutorial / onboarding — New players get dropped into an empty village with no guidance.
- [ ] Event notifications — PvE events start/end in the background with minimal visual feedback. Toast notifications or a battle replay would help.
- [ ] Mini-map — On a 40x40 grid, a corner mini-map would help navigation.
- [ ] Sound effects & music — No audio at all currently.
- [ ] Clans / guilds — No social grouping.
- [ ] Chat — No player communication.
- [ ] Trading — No resource exchange between players.
