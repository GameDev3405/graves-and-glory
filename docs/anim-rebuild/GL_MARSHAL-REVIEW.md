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

# Own moves 2026-11-13

Kort (2026-11-13): "Own moves for all three". The Marshal no longer swings the knight's choreography: his three attacks
are authored whole-body in `art/anim/gl_marshal/build_own_attacks.py` (fullbody kit, phases in its docstring) and
`art/anim/knight/rebuild_out.py` step 8 puts them in out/. Name, length (22 ticks), play-once and the impact marker
(thrust 0.35, overhead 0.5, low cut 0.4) are unchanged; first and last keys equal the present clips' (legs within
0.001 deg). Every other clip is byte-identical (only the six attack clips differ from c1f97231, in out/ and the asset).

## The moves

- **Thrust, a passing lunge (impact 7, body).** 0-3.6 the rear foot draws back and turns out, chest side-on, hand drawn
  back low along the line; 3.6-5.6 hold; 5.6-7 the arm leads, then the lead foot shoots 7 px forward into a deep lunge,
  rear leg straight, shield arm out to the side; 7-9.6 locked out and held; 9.6-22 pushes back and walks home.
- **Overhead, a descending diagonal cleave with a step through (impact 10, head).** 0-5 the blade rises on the sword
  side to a high guard (no roll), chest coiled 47 deg; 5-7.4 hold, the pelvis rocks back onto the rear foot; 7.4-10.2
  the rear foot passes 9 px forward as the chest uncoils and the edge comes down from high sword side; 10-15 on through
  low to the shield side, upright over the passed foot; 14-22 the foot walks back.
- **Low cut, an offline backhand (impact 8, legs).** 0.8-4 the shield-side foot steps out and forward off the line,
  hips turning; 1.2-5.6 the sword crosses over the tucked shield and stands upright on the shield side; 5.6-8 he drops
  onto the stepped foot and cuts down and outward across the legs; 8-10.5 on out to the sword side, hand wide of the
  hip; 10.5-22 both feet walk home.

## How different (present clip against own)

Hips: yaw difference (pelvis distance px); torso: yaw difference; weapon: angle between blade directions; lead foot:
distance between the most forward ankles. A column counts at 15 deg or 2 px (`work/ownmoves/difftable.py`).

| Attack | Phase (present t / own t) | Hips yaw deg (pelvis px) | Torso yaw deg | Weapon dir deg | Lead foot px | Differ (of 4) |
|---|---|---|---|---|---|---|
| thrust | wind-up 5 / 5.6 | 26 (0.7) | 70 | 47 | 0.7 | 3 |
| thrust | impact 7 / 7 | 12 (1.6) | 17 | 20 | 3.8 | 3 |
| thrust | follow-through 9 / 9 | 11 (1.8) | 16 | 25 | 3.8 | 3 |
| overhead | wind-up 8 / 7.4 | 1 (0.9) | 16 | 55 | 1.8 | 2 |
| overhead | impact 10 / 10 | 20 (2.4) | 2 | 26 | 7.2 | 3 |
| overhead | follow-through 12 / 12.5 | 25 (3.1) | 2 | 31 | 7.2 | 3 |
| low_cut | wind-up 6 / 5.6 | 43 (3.1) | 104 | 25 | 11.1 | 4 |
| low_cut | impact 8 / 8 | 20 (4.2) | 14 | 25 | 12.0 | 3 |
| low_cut | follow-through 10 / 10.5 | 10 (4.2) | 35 | 99 | 12.0 | 4 |

The weakest separation is the overhead wind-up (2 of 4): both versions carry the blade up and back on the sword side.
Its impact and follow-through differ in 3 of 4 (the passed foot, 7.2 px).

## Checks

- Impact (point/edge along the facing axis, present -> own): thrust z 32.5 -> 33.2 at y 14.7 (body); overhead
  31.7 -> 32.5 at y 21.8 (head); low cut 35.3 -> 36.0 at y 6.4 (legs). All within 1.5 px.
- `scorecard.py --troop gl_marshal`: exit 0, 41 clips, 0 fails. (A first pass failed elbow_bend on the thrust 140.2 and
  the overhead 117.7, and ang_speed_legs on the low cut 54.95: the thrust's drawn-back hand and shield fist, the
  overhead's finishing shield fist and the low cut's step yaw were opened or eased.)
- `focus_check.py --troop gl_marshal`: 41 clips, 0 FAIL. Other clips: the same 55 rows as before, none new. Attacks,
  WARN only: thrust weapon_in_body 1.27 t6.25, prop_in_body 1.94 t7.0 (shield into torso), limb_in_body 1.21 t6.75,
  floor 0.57 t9.5 (left knee), phases 3.87 (load held 1.00 tick, needs 2), point_lead 32.66 t6.75 (present 28.66);
  overhead armour_clip 1.78 t12.5 (cape_t3 into hips); low cut weapon_in_body 1.03 t2.25 (pommel by the head),
  prop_in_body 1.58 t5.75 (shield into left shin), limb_in_body 1.05 t8.0, armour_clip 1.85 t1.5. The present attacks
  had 6 WARN rows including "stops at the impact" phases on the overhead and the low cut; both now carry through.
- fullbody worst between-key error: thrust 0.17, overhead 0.91 (was 6.0: the upper arm swivels 14 deg after the impact),
  low cut 0.22 deg.
- Baseline rebuild before any edit drifted only the `cape` bone (0.77-2.54 deg) on the three attacks, stun_upper and
  the four hit_medium clips, probably from 1f485170 (shield_face pivot) feeding soft_motion. Not shipped: out/ was made
  by `rebuild_out.py --only <six clips>` over the committed out/, so every other clip stays byte-identical. A whole-file
  rebuild would still carry that cape drift.
- `python -m unittest discover -s tools/anim`: OK (`work/ownmoves/unittest.log`), with the new
  `tools/anim/test_own_attacks_gl_marshal.py` (timing, ends equal the c1f97231 clips', shipped equals `clips()`).
- Renders: every tick, front, side and three-quarter, near and game16, tier 3 (`work/ownmoves/final/`), looked at.

## Open

- Thrust load hold is 1 tick (phases WARN); the shield sits 1.94 px into the torso at the lunge (limit 2.0).
- The overhead wind-up reads close to the family's at 16 blocks; the difference is in the pass and the finish.
- Whole-file rebuild cape drift (above) on stun_upper and hit_medium_*: for whoever owns soft_layers / the pivot change.
- Not seen in game; no Gradle, harness or verify.py run (the lead runs those).

## Fix round 1 (2026-11-13, after the independent review)

The reviewer found the overhead still the family's move (F1 wind-up/finish silhouette, F2 a level stab at t10).
Builder only (`build_own_attacks.py`); out/ by `rebuild_out.py --only <six clips>`, then export.

- **F2 overhead impact.** The cut now runs t8.5-11.5, fastest across the impact: tip high on the sword side at t9
  (-16, 43, 11), level forward about t9.75, then down and across. At t10 the blade is 36 deg below level, yaw 12, edge
  leading; tip (-2.8, 11.7, 31.1), 0.6 px from the present 31.7. With the tip held at z 31 and the pitch at least 35
  the hand cannot go higher on this rig, so the blade's middle meets a target at chest to chin height, not crown.
- **F1 overhead silhouette.** A true roof guard (hilt above the head, blade back) does not fit: the crown reaches
  y 36.9 and the 8.1 px arm cannot carry the hilt over it without the blade passing through the helm. Nearest clean
  thing: an upright blade at the sword-side ear (pitch -96 to -102, no roll), shield drawn in to the chest, chest
  coiled 21-24 deg, weight back over the rear foot. Follow-through: the rear foot passes (plant 6.8 px), the torso
  turns through to -48 deg, the blade finishes low outside the lead knee on the shield side (tip x 13.7 at t12.5-13),
  then he rises tall by t18.
- **F4 thrust.** Draw shortened to end at 2.8; load held 2.8-5.9 (3.1 ticks). The phases WARN is gone.
- **F3 thrust.** Shield fist moved out: prop_in_body now 1.33 into the belt (was 1.94 into the torso).
- **F5 low cut.** Blade kept off the helm at t2.25 (the head graze row is gone; the remaining weapon row is the
  gorget, 0.87 at t5.75).
- **F6 low cut.** Torso pitch at t10-11 6.5-7.6 deg (was 14-15); tip y 6.6-9.8, off the floor. Impact tip z 35.2.
- F7, F8 not touched.

Overhead, same tick, present (c1f97231) against own (`work/ownmoves/fx_table.py`):

| Tick | Hips yaw deg (pelvis px) | Torso yaw deg | Weapon dir deg | Lead foot px | Differ (of 4) |
|---|---|---|---|---|---|
| t6 | 5 (2.1) | 13 | 38 | 0.0 | 2 |
| t8 | 6 (2.1) | 13 | 67 | 1.8 | 2 |
| t10 | 18 (3.2) | 8 | 7 | 6.9 | 2 |
| t12 | 17 (3.5) | 21 | 34 | 6.9 | 4 |
| t13 | 15 (3.4) | 23 | 35 | 6.9 | 4 |

Phase table after fix 1 (`difftable.py`): thrust 3/3/3, overhead 2/2/4, low cut 4/3/4.

Sheets (`work/ownmoves/`): `attack_overhead_present_vs_own_fix1.jpg`, `attack_overhead_strip_8.5-11.5_fix1.jpg`,
`attack_{thrust,overhead,low_cut}_every2_own_fix1.jpg`. Every tick rendered in `work/ownmoves/fx/final/`.

Checks after fix 1: scorecard exit 0 (41 clips, 0 fails); focus_check 0 FAIL, 66 WARN rows (67 at 7cd2abbd), every
changed row on the three attacks and none new on other clips; unittest discover OK (161 tests). Attack WARN rows now:
thrust weapon_in_body 1.23 t6.5, prop_in_body 1.33 t6.5 (belt), floor 0.57 t9.5, point_lead 33.18; overhead
armour_clip 1.84 t9.5, pop 8.87 t10.5 (cape); low cut weapon_in_body 0.87 t5.75, prop_in_body 1.58 t5.75,
limb_in_body 1.05 t8, armour_clip 1.86 t2.5.

Open after fix 1: overhead armour_clip WARN 1.84 at t9.5 (sword arm into torso), cape pop WARN at t10.5; in side view
the overhead finish still reads low and forward like the family's (the difference shows in front and three-quarter);
blade middle at chest height at the impact (rig limit above).
