# Questions and problems for Kort

Written while working autonomously on 2026-10-17. Newest at the bottom.
Each item says what was decided in the meantime so work could continue.

## Open questions

(none; the 2026-10-17 items are answered below)

## Answered 2026-10-17

- Phase gates: tester subagent reviews screenshots; Kort playtests the builds listed below when he wants.
- Houses: trait tiers by banner slots 6/10/14; migrated Sunweaver/Gravecaller (and Oracle/Wraith if unlocked) refunded 300 gold each, not kept; spiders keep climbing in Siege; Legendary names approved; boar-kin Wildmoot approved; 29-hour chase accepted; commander active on H; no retro Founders tier-20 Rare; Mark Prey uses the vanilla glow.
- Phase 2 planner's other calls (classes split by House batch, later Houses hidden until their batch, Ballista evolve trimmed to the cap) stand.

## Problems hit

1. **Kingsfield (map 4) crashes the client when the lobby switches to it**
   (`ArrayIndexOutOfBoundsException` in `GravesAndGloryClient.map`: the minimap
   draws 5 points while the contested/progress arrays still hold 3 until the
   next state packet). Found 2026-10-17 by the harness-fix run. Decision: fix it
   inside Phase 1 (regression task) and add a check for it; until then do not
   pick Kingsfield from the lobby.
   Fixed on phase-1-solo-core (commit 6ecbfb0); in-game verification in the regression task.
2. **Two campaign-suite checks were already failing before Phase 1:**
   `solo-pad-3v3` and `roadmap-suite`. Left as-is for now; Phase 1 Task 9
   either fixes them or records why they are stale.

3. **Bot balance misses two of three acceptance targets** (Task 5 sim, 40 matches,
   2026-10-17 23:00). Normal beats Easy 9/10 (pass). Hard beats Normal only 10/20
   (needs 60%). Normal vs Normal: Dawn wins 8/10 (needs 35-65%), and across every
   pairing Dawn won 30/32 on the 3-point maps while Dusk won 5/8 on Kingsfield.
   So the seat decides more than the tier. Decision: a follow-up task (5b) first
   finds the seat bias (suspects: Dawn's base holds the world spawn so its chunks
   always tick; 3-squad spawn rows are centred for 4; Dawn shields vs Dusk kit),
   then makes Hard's flank picker leave a garrison on held points, then reruns.
   Question for you: if Hard still cannot reach 60% with flanking on, is it fine
   to turn `hard.flankLeastDefended` off (Hard keeps faster decisions, timed
   charges and shieldwalls, but picks points like Normal)? My recommendation:
   yes, flanking is worth less than a Hard that actually wins.

## Builds to try

- 0.3.7 (master e78b81b): camp, pads, parties, rooms, contracts verified by
  automated checks in a real client. Import dist/graves-and-glory-0.3.7.mrpack
  (rebuilt 2026-10-17 22:18, Melty removed, archive verified) into Prism.
  Pre-Phase-1 game: no projectiles, morale or bot tiers yet.

## Decisions from Kort 2026-10-17 (UI/HUD round 2)

- Direction chosen: the War Table screens (UI A) and the Iron Ledger group cards (HUD B). Polish only that family; no more three-way reworks.
- Unit portraits show the actual soldier (an in-game model render), not pixel art. Placeholder pixel busts stay until the unit identity pass below delivers each unit's look.
- New track: a unit identity pass. An agent examines every unit and designs each one deliberately with a distinct look and a distinct purpose; the agreed look then feeds the UI portraits. Spec: docs/superpowers/specs/2026-10-17-unit-identity.md (in progress).
- Hunger is removed: no hunger bar and no eating mid-battle. Something else takes that slot later. Goes into the UI implementation plan (hide the bar client-side, pin food level server-side).
- Pixel art detail goes way up on the chosen screens; no corner cutting.
