# TamaHero - TODO

## Rendering & Visual Polish
- [ ] Troop rendering — Battles happen server-side but troops are never drawn on the canvas. Showing units moving, attacking, and dying would be huge.

## Game Mechanics
- [ ] Mana generation — New building that procuces mana
- [ ] SpringTrap — Defined in code but has no damage values — it should insta-kill some trupps
- [ ] Building move in UI — The server supports Move, but the client UI never exposes it.
- [ ] Building queue — Can only build one thing at a time, no queue for "build next" ALSO, it seems to be possible to build as many things as we want, which is wrong. let's copy clash of clash with the building that allows you to have a worker to build things
- [ ] Hero unit — The game is called TamaHero but there's no hero character! A tamagotchi-style hero you feed/train/level could be the core loop.

## UI/UX
- [ ] Army overview in HUD — You can train troops but there's no persistent army display during normal gameplay.
- [ ] Production indicators — No visual cue on buildings that have resources ready to collect (e.g., a floating icon or glow).
- [ ] Building HP bars — Damaged buildings don't show their HP unless you tap them.
- [ ] Resource change feedback — No "+500 gold" floating text when collecting.
- [ ] Bulk actions — No "upgrade all" or "rearm all" with visual confirmation.

## Quality of Life
- [ ] Building info on hover — Desktop could show tooltips on hover instead of requiring a click.
- [ ] Settings screen — No in-game settings (camera sensitivity, grid default, etc.).
- [ ] Offline progress summary — When reconnecting, no "while you were away..." screen showing what happened.

## Skip for now
- [ ] Particle effects — Explosions, magic, resource collection sparkles, damage numbers.
- [ ] Animations — Everything is static sprites. Construction progress, production bubbles, upgrade sparkles, trap triggers, building destruction would all add life.
- [ ] Day/night cycle — Tint the village based on time of day.
- [ ] Weather effects — Rain during storms, screen shake during earthquakes.
- [ ] PvP / Raiding — Only PvE events exist. Player-vs-player attacks would add replayability.
- [ ] Achievements / milestones — No progression tracking beyond TH level.
- [ ] Story / campaign — No narrative or campaign missions.
- [ ] Tutorial / onboarding — New players get dropped into an empty village with no guidance.
- [ ] Event notifications — PvE events start/end in the background with minimal visual feedback. Toast notifications or a battle replay would help.
- [ ] Mini-map — On a 40x40 grid, a corner mini-map would help navigation.
- [ ] Sound effects & music — No audio at all currently.
- [ ] Notifications — No push notifications when construction/training completes or events happen (mobile).
- [ ] Clans / guilds — No social grouping.
- [ ] Leaderboards — No ranking system.
- [ ] Chat — No player communication.
- [ ] Trading — No resource exchange between players.
