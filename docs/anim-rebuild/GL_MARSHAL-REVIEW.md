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

# Batch review 2026-11-11

Branch `anim/review-gl_marshal` (from `anim/troop-gl_marshal`). The sentinel's builder commit (89d82e46 on
`anim/review-sentinel`) is cherry-picked as 86f8dc70; this branch adds the marshal to `ALIVE_TROOPS`, keeps the
Templar-only low cut fixes (shield fist drop, wrist low-pass) on the sentinel (the marshal's shield into head is 1.28 WARN
and its wrist runs smooth), tunes the soft pieces for the marshal's rig (`SOFT_TUNE`), and adds a `gl_marshal` entry to
`art/anim/knight/soft_layers.py`. Knight, sentinel, revenant, tc_raider and gb_hollowking are untouched by construction:
every change is gated on the troop (`ALIVE`, `TROOP == "sentinel"`, `SOFT_TUNE` and `LAYERS` keyed by troop); their
exported files and out/ are not in the diff.

**Focus check (raw, every clip over the bind pose):** before 14 of 41 clips with a FAIL (24 rows), after 7 (10 rows); all 10
are rig differences or bind-pose artefacts (below). Composed the way the game plays it (`work/review/sen_compose.py`):
k 1.0, 0 FAIL except stun_out_upper whole_body (the composition holds the legs still); k 1.55, only hit_knockback's two
rows from reactions.py (below); hit_block at its top block scale 1.3, 0 FAIL. Scorecard: exit 0, 0 fails.

**hit_medium_back weapon_t3 under the ground 1.39 px at t2.5** (raw): the clip is additive and the check plays it over the
bind pose, where the sword hangs straight down from a lowered arm. Played over the ready stance the blade is well clear:
k 1.0 and k 1.55 both pass the floor (k 1.55 has the shield rim at 0.81 px, WARN, unchanged). It is a tool artefact, so the
clip was not re-authored; the same holds for its knee -13.01 at t11.5.

## What changed, per clip

- **Every attack, the medium hits and the block** (builder `soft()`): the cape, fur collar, skirt plates and pauldrons swing.
  For this rig the fur collar (a plume by the kit's defaults) popped 14-24 deg in a sample on the hits, and the cape swung
  into the hips: the collar is damped (gain 0.6, max 4 deg, zeta 0.8), the cape may not swing inward and is
  capped at 10 deg. secondary_life FAIL -> pass.
- **stance_wall (legs):** the hips sit 1.25 px higher: left knee 1.11 px under the ground -> pass.
- **hit_pulled** (soft_layers): the same fix as the sentinel (sword arm wider of the helm, body lifted off its dragged feet,
  toes 10 deg less); skirt swing capped at 5 deg, cape at 8 deg, collar damped. As played: blade 2.42 px into the head at t2
  (3.56 at 1.55) and toe 2.51 px under the ground -> WARN only.
- **hit_knockback** (soft_layers): hips sink 1.7 px less on the shove and the stumbling steps, the braced toe lifts:
  right boot 1.61 px under -> WARN.
- **stun_upper / stun_out_upper** (soft_layers): the pieces swing (collar damped so the loop keeps loop_pos under its limit;
  the shake-off's cape and skirts at gain 2 so they read).

Length, impact and timeline markers unchanged on every clip.

## Remaining FAIL rows (raw), with reason

| Clip | Row | Reason |
|---|---|---|
| stance_wall_upper | floor 1.17 t10, shield | the wall plants the shield rim on purpose (was 2.42; the raised hips lift it) |
| stun_upper | floor 1.81 t7.75, shield | leans on its planted shield (unchanged) |
| stun_out_upper | floor 1.72 t0.75, right_boot | additive over the bind pose; over the stun's last pose: pass |
| hit_pulled | weapon_t3 into right_leg 1.92 t2.25; elbow 162.8 t2.75 | bind-pose artefact; composed k 1 and 1.55: pass |
| hit_knockback | weapon_t3 into right_leg 1.82 t1.5; elbow 146.58 t8.5 | bind-pose artefact; composed: pass |
| hit_medium_back | floor 1.39 t2.5 weapon; knee -13.01 t11.5 | bind-pose artefact (above); composed: pass |
| hit_block | elbow 176.88 t8.25 | bind-pose artefact; composed: pass |

New WARNs that were a pass: stun_upper cape_t3 into left_leg 1.80 t21.5 (the cape now swings), hit_pulled raw weapon_in_body
(above).

## Proposed exemptions (troop, clip, check, value, reason)

- gl_marshal, stance_wall_upper, floor, 1.17, shield rim planted by design.
- gl_marshal, stun_upper, floor, 1.81, leans on the planted shield by design.
- gl_marshal, hit_pulled / hit_knockback / hit_medium_back / hit_block / stun_out_upper: the rows above, additive clip judged
  over the bind pose (better: focus_check composing additive hits over the ready stance, a tool change).

## Open

- Heavy blows (k 1.55) on hit_knockback: gorget into head 1.91 t3 and left knee_aim 88.8 t5.5 come from
  art/anim/common/reactions.py (every biped); not changed here.
- `art/anim/gl_marshal/out/` is not regenerated: the full build of this troop takes over an hour on this machine and ran
  out of memory before; the changed clips were rebuilt with the builder (`work/review/part.py`) and exported over the
  committed asset. Re-running export.py from the stale out/ would drop them: rebuild out/ before any re-export. Fixed
  2026-11-12: see "Build recipe" below.

## Build recipe (2026-11-12)

`GG_ANIM_TROOP=gl_marshal python art/anim/knight/rebuild_out.py` then `python art/anim/common/export.py gl_marshal` (4-15 min).
It writes out/ whole: the builder's 40 clips, the 12 deaths spliced unchanged from 8cb35120 (the builder no longer reproduces
them), then stances, reactions, charge, reactions (hit_pulled) and soft_layers.py. Checked 2026-11-12: out/ equals the shipped
asset byte for byte and the export leaves it unchanged. Never run stances/reactions/soft_layers on the asset by hand again.

## Test results

- `python -m unittest discover -s tools/anim`: 152 tests OK (`work/review/unittest.log`).
- `python tools/verify.py fast`: build PASS, ui-assets PASS, python-tests 727 run, 1 failure:
  test_where `test_extract_is_fast_and_covers_every_troop_and_section`, 44.6 s against its 30 s limit (a speed limit; the
  machine was running several builds and test suites from other lanes). No animation test failed
  (`work/review/verify_fast_final.log`).
- In-game reel `gl_marshal-review2` (job 20261112-034324-299-560f): 22 `[p1] p1-` checks plus the suite line, 0 `=false`.
  The first run (review1) failed march-run-front alone (client speed 0.273 against 0.255; side and three-quarter passed;
  the run clip is unchanged in this batch).
