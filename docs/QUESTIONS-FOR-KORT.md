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

## Builds to try

- 0.3.7 (master e78b81b): camp, pads, parties, rooms, contracts verified by
  automated checks in a real client. Import dist/graves-and-glory-0.3.7.mrpack
  (rebuilt 2026-10-17 22:18, Melty removed, archive verified) into Prism.
  Pre-Phase-1 game: no projectiles, morale or bot tiers yet.
