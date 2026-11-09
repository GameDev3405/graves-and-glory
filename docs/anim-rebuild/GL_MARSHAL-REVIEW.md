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

# Gl_marshal: v2 style + v4 deaths (2026-11-07)

The marshal now builds in the v2 shield style (Kort approved v2 as the default for shield units), so its whole set changed, not only the deaths. It also has the knight's 12 v4 deaths: `death_{back,forward,left,right}`, each plus `_drop1` (drops the sword) and `_drop2` (drops the sword and the shield). The only builder change is adding `gl_marshal` to `D3_TROOPS` in `art/anim/knight/build_anims.py`; the D3 death system already takes the troop's own weapon bones and forearm pieces (from the sentinel commit).

- **Byte-identical:** knight and sentinel rebuild byte-identical with the committed outputs. The other troops are unaffected by construction: `D3_TROOPS` is used only in the membership test `D3 = TROOP in D3_TROOPS and V2`, and none of them is in it. Export changed only the gl_marshal animation (geo, rig and textures unchanged).
- **Review (Sonnet, high effort): SHIP WITH MINORS.** It covered the whole set (gait, stances, idle, 3 attacks, hits, 12 deaths) from contact sheets, compared against the sentinel. No blockers; quality equal to the sentinel.
- **Harness (geo-knight, `GRAVES_GEO_CHECK_TROOP=gl_marshal`), on a heavily loaded machine (another worktree's harness and several builders running):**
  - Run 1: 22 p1 and 2 `=false`. The death check saw an empty death layer for two client ticks at the start of the left fall (render had not caught up); the clip then played correctly (`death_left_drop1`, 22 frames). The other line is the suite rollup.
  - Run 2: 22 p1 and 2 `=false`: the death check passed; `march-walk-side` failed on lag, plus the rollup.
  - Run 3: 22 p1 and 0 `=false`.
  - Across the three runs, 10 of the 12 death clips played: `death_back_drop1`, `death_back_drop2`, `death_forward`, `death_forward_drop1`, `death_forward_drop2`, `death_left`, `death_left_drop1`, `death_left_drop2`, `death_right` and `death_right_drop1`.

## Open minors (v2 + v4, polish, not blocking)

The shared v2 and v4 minors recorded in SENTINEL-REVIEW.md apply here too (thrust yaw at t=11, low cut ends blade-down, subtle hits and idle, fast death_back, hard-to-see knee buckle in death_forward). Specific to this review:

- **death_forward_drop2:** the dropped shield shows as a thin sliver beside the torso in the front view. It could sit slightly further out so it reads as a shield. The sentinel's is the same.
- **death_left and death_right (kept gear):** the kept shield ends mostly hidden under or against the torso. In the side view the kept sword reads as a bar pointing at the camera.
- **Overhead cut:** at t=11-13 the blade passes close behind and beside the crown. No intersection is visible at sheet size; confirm in game.
- **Hits:** subtle, as on the sentinel.
- **Not seen in game by eye:** only the harness and the offline reel were run.
