# mage review record (2026-11-06)

The Sunweaver Mage, the first caster: a sun staff held in the right fist, robes and a cowl. Its clips come from the new caster builder (`art/anim/caster/build_caster_anims.py`). Its rig comes from `art/anim/common/troop_rig.py mage` (staff `build_sun_staff`, held at (-90, 0, 0)).

- **Clips:** guard, idle, walk and run carry, stances, hits and deaths, plus `attack_cast` (36 ticks) and `attack_cast_legs`. The timeline `impact` marker sits on the server's release tick (10).
- **Cast:** gather and raise the staff, a held coil, then a whole-body drive (torso lean and twist of about 26 and -20 degrees) with a hip drop and a lead step. The release is at tick 10, with the staff about 63 degrees from vertical, and an overshoot at 11.8. The root is back on guard by tick 30.
- **Stances:** brace (Hold position) is a deep crouch with the staff planted upright. Wall (Hold the line) is tall, with the staff pushed forward and the off hand raised.
- **Deaths:** hard robe panels, with the robes lifted off the ground over ticks 0-4.
- **Gameplay (Kort approved, 2026-11-06, cosmetic only):** the mage's cast is its heal (`Abilities.mend`), not an enemy attack beat. On the beat before a heal, `mend()` announces the cast 10 ticks early (`Projectiles.drawAhead`), so the wind-up plays before the release. The later `loose` sends the same event, and the client drops the repeat. `Tactics.announcesAhead` now admits a magic caster only when its attack shoots a bolt (`Abilities.shoots`): wm_seer yes, hexer and tc_mist no. Damage, healing and release ticks are unchanged.
- **Checks:** `GeoKnightChecks` has a `CAST` flag (the mage). It runs the ranged duel with frames over the 36-tick cast clip, and releases a 0-heal mend at the foe as a stand-in.

## Review

- **Round 1 (Sonnet high, reel `reel_mage_v1`): FIX NEEDED.** The cast was weak, wall looked like brace, and the deaths had floor offsets.
- **Round 2 (Sonnet high, reel `reel_mage_v2`): SHIP WITH MINORS.** The cast has a full-body drive, the stances are distinct, and the deaths rest on the floor. Probe: cast fist error at most 0.54 px. Lowest legs in death_forward went from -2.22 to 0.95 px.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false. 35 of 35 cast frames caught side and three-quarter (17 of 17 far). All 9 standing and 4 walking casts matched the server release. Weight at the release is 1.0, and 0 between casts.
- **Verdict: finished.** Video: `mage_v2.mp4`.

## Open minors (polish, not blocking)

- **Cast, pure side view (ticks 3-8):** the staff overlaps the cowl in projection. It does not pass through it.
- **Wall:** the staff is only about 20 degrees forward, so it does not read strongly as a barrier.
- **Brace:** it reads shallower in the blended reel than in the still poses.
- **Side falls:** the hips sit about 3-4 px up, hidden by the robe.
- Hits were not reopened closely in round 2. The tick 0-4 robe lift was not checked on the reel.
- Locomotion is the same as round 1; there is no walk-to-run pop.
- The early announce needs a hurt ally in sight on the beat before the heal. The harness checks the announce and release timing, but not `Abilities.mend` itself.
- wm_seer is not on GeckoLib yet. Its legacy renderer now gets the early attack events (deduplicated, but untested).
