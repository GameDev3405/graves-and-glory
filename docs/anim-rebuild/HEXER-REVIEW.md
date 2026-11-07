# hexer review record (2026-11-06)

The Gravecaller (hexer), the second caster: a grave lantern hung from a short pole in the right fist, a burial robe and a hood. Its clips come from the caster builder (`art/anim/caster/build_caster_anims.py`, `GG_CAST_TROOP=hexer`). The mage's built output is byte-identical; every hexer change branches on the troop.

- **Clips:** guard, idle, walk and run carry, stances, hits and deaths, plus `attack_cast` (36 ticks) and `attack_cast_legs`. The timeline `impact` marker sits on the server's release tick (10).
- **Cast:** an underhand censer swing, not the mage's overhead gather. From the guard (lantern at the right hip) the body coils back and sinks over the rear foot while the lantern sweeps low and back, wide of the right hip, and the off hand reaches at the foe. The coil keeps winding through the held load (ticks 4.5-8.4). The hips then drive forward over the lead foot, and the lantern swings up and is thrust at chest height on tick 10 (torso lean about 29 degrees on top of the hip pitch), with an overshoot at 11.8 and a supported recovery. The feet step out and come back; the root is on guard by tick 30. Probe: fist error at most 0.064 px, 0 samples with the lantern in the body.
- **Stances:** brace (Hold position) is a deep hunch with the lantern low before the lead knee. Wall (Hold the line) is upright, with the lantern raised high at arm's length as a warding light and the off hand up. Ward (fall back) holds the lantern forward at the chest with the off hand up to fend.
- **Lantern probes:** the hexer focus is checked at its own points (the pole runs along local -z from the grip, the light hangs below it), as well as along the shaft.
- **Deaths:** the shared caster falls. The left fall takes the mage's arm and cowl tuck; the right fall lies lower without it. Rest heights (min-y, px): left torso 1.17, hips 4.13; right torso 0.70, hips 3.66.
- **Gameplay (Kort approved, 2026-11-06, cosmetic only):** the hexer's attack is its blight curse (`Abilities` blight case), which used to send no attack event. A caster whose ability mechanic is blight (`Abilities.castsAhead`, so the hexer only) now announces the cast 10 ticks early from `Tactics` (`Projectiles.drawAhead`) on the beat before a cast is due, and `Abilities` sends the matching `loose` when the curse is released; the client drops the repeat. Damage, the release tick and the cooldowns are unchanged. The hexer is not in `Tactics.announcesAhead`.
- **Checks:** `GeoKnightChecks` has a `BLIGHT` flag (ranged, magic caster, blight). It runs the ranged duel with frames over the 36-tick cast clip and releases through `Tactics.shoot` with the ability cooldown cleared.

## Review

- **Round 1 (Sonnet high, reel `hexer/reel`, video `hexer_v1.mp4`): SHIP WITH MINORS.** No blockers. Minors: the lantern vanished behind the torso in side view at the drive, the load parked then snapped, the release lean (about 15-20 degrees) was below the mage bar, brace and wall were hard to tell apart, the ward lantern may have been hidden, and the left fall lay about 0.2 block off the floor.
- **Round 2 (Sonnet high, reel `hexer/reel2`, video `hexer_v2.mp4`): SHIP WITH MINORS.** Fixed: the swing comes through wide of the hip and stays visible; brace, wall and ready are three distinct silhouettes; the ward lantern reads at the chest; the side falls lie on the floor. Improved: the load keeps winding, and the release lean is now about 25-30 degrees.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false. 35 of 35 cast frames caught in side and three-quarter views (17 of 17 far). All 9 standing and 4 walking casts matched the server release. Weight at the release is 1.0, and 0 between casts. Stances, hits and deaths pass in 3 views.
- **Verdict: finished.** Video: `hexer_v2.mp4`.

## Open minors (polish, not blocking)

- **Cast load:** a short near-hold remains around clip ticks 6-8, before the drive.
- **Cast release:** the lean holds only 2-3 ticks. It is still a little short of the mage's drive. A deeper hip pitch made the lead foot unreachable, so the extra lean is in the torso.
- **Cast, front view:** the swing reads mostly in the side and three-quarter views. The lantern stays close behind the body from the front.
- **Brace:** very little breathing motion in the hold.
- **Left fall:** the rest pose is a tight heap, and the lantern may overlap the torso at the end.
- **Right fall:** the lantern swings out to the side during the tumble (plausible, not broken).
- **Shared with the mage, not reopened:** the heavy front hit folds about 80 degrees with a rebound (knight hit keys); the deaths have a long static end with no separate lantern settle; there is a walk and run seam pop; the run has little forward lean; the ward walk's back foot flicks.
- The early announce needs the blight ability to be off cooldown on the beat before the cast. The harness checks the announce and release timing through `Tactics.shoot`, not the live blight targeting itself.
