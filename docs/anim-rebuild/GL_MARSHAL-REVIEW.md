# gl_marshal review record (2026-11-06)

The Gilded Legion Marshal, the fourth shield unit: shield upright in the left fist and a 24-32 px broadsword. It shares the knight's clip builder (`art/anim/knight/build_anims.py`, `GG_ANIM_TROOP=gl_marshal`). Its rig comes from `art/anim/common/troop_rig.py gl_marshal`.

- **Shared clips:** guard, idle, gait, stances, block, hits and deaths follow the knight set, with the sentinel's long-blade follow-through (`LONG`).
- **Attacks:** thrust, overhead and low_cut, each with an `_legs` clip; the timeline `impact` marker sits on the server's impact tick.
- **Round 1 fix: floor clip.** The broadsword is longer than the Templar's, so the overhead and low_cut follow-throughs drove the tip into the ground. `FLAT` (18 degrees for the Marshal, overridable with `GG_FLAT`) flattens the blade at overhead t12/t14 and low_cut t8/t11. Probe after the fix: tip lowest point above the soles is thrust 7.3 px, overhead 1.8 px, low_cut 1.2 px.
- **Rig:** the spear builder's grip and fist offsets now read from the rig pivots (`GRIP`, `FIST_L`, `S()`), so new troops do not need hand-tuned offsets.

## Review

- **Round 1 (Sonnet high, reel `reel_gm_v1`): FIX NEEDED.** The sword tip went through the floor in the overhead and low_cut follow-throughs. The fix above.
- **Round 2 (Sonnet high, reel `reel_gm_v2`): SHIP WITH MINORS.** No floor clip at the probe's numbers; the attacks keep their whole-body drive.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false. All 54 standing swings and 5 walking swings matched the server impact.
- **Verdict: finished.** Video: `gl_marshal_v2.mp4`.

## Open minors (polish, not blocking)

- **Deaths:** floor offsets. In death_forward the tip sits about 0.2 block below the floor; death_left sinks about 0.2; death_right floats about 0.1.
- **overhead** follow-through (f608-614): the blade overlaps the shield and forearm. At f612 the tip is borderline at the sole line.
- **low_cut** front view (f740-746): the tip looks slightly below the soles. This is foreshortening; the probe gives 1.2 px above.
- The flatter follow-through is slightly less dramatic than the knight's.
- **Stances:** the first ready frame has the blade low; hold-position holds the blade low and forward.
- Block was checked only on the small sheet. Round 2 did not reopen the stance, idle, locomotion or death sheets.
