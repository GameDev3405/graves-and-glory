# stalker review record (2026-11-06)

The fifth and last bow unit, the barrow stalker. It uses the archer's bow builder (`GG_BOW_TROOP=stalker python art/anim/archer/build_bow_anims.py`) with its own character values (`CHAR["stalker"]`: lean −18, hips sunk 1.6×, the least rock and recoil of the family, a patient low shot) and its own aim (volley 35°, direct 4°). Its rig comes from `art/anim/common/troop_rig.py stalker`.

- **Gear.** The pelt tail takes the hood-cape lag and the wrap tails the tippet lag; it has no quiver bone.
- **Death back.** The wide mantle swallowed a bow laid by the hip, so from tick 7 of the fall the bow arm flings out wide (`left_arm` −30, 0, −90, forearm straight) and the bow lands clear of the body.

## Review

- **Round 1 (Sonnet high, reel `stalker_v1`): fix needed.** In death back the bow was buried in the mantle.
- **Round 2 (Sonnet high, reel `stalker_v2`): SHIP WITH MINORS.** The arm fling clears the bow in death back; no blockers elsewhere.
- **In-game checks** (`GRAVES_GEO_CHECK_TROOP=stalker`): **run 7 PASS, all 51 checks.** Attack frames 25/25 at side and three-quarter (6 stepped past) and 12/12 far; sync 7/7 standing and 4/4 walking; impact weight 1.0; footwork 1.0; hits 7 blows × 3 views; deaths 4/4.
- **Verdict: finished** (visual review and gameplay testing both pass). Video `stalker_v2`.

## Open minors (polish, not blocking)

- **Bow art.** The bow reads thin and grey; the string renders as a rigid rod (shared art minor).
- **Bow limbs at the hip** read as floating cubes in some frames.
- **Death left.** The bow looks detached near the reel panel's edge.
- **Deaths** drift or clip at the reel panel's edges (camera framing).
- **Brace, hold the line and ward** read close to combat ready (same as the archer).
- **Foot slide** could not be confirmed at thumbnail size.

## v2 feel (2026-11-07, branch anim/v2-bow)

The same Knight v2 feel as the archer (see `ARCHER-REVIEW.md`, "v2 feel"), built by `art/anim/archer/build_bow_anims.py` with the stalker's own CHAR values. The stalker character is a patient hunter, set low with the deepest sink. The draw, the shot and the deaths are unchanged, and the deaths are byte-identical to the previous export.

- **Probe.**
  - No bow or fist in the body in any clip.
  - Floor: hits -0.39 to -0.48 px; hold the line -0.62 px; brace -0.46 px (the mantle hem and boots, as at baseline).
  - Largest hit pop: 27.9 px/t, at the heavy front peak, at the bow tip. The volley reaches 32.2 px/t.
- **Review.** Sonnet high, 1 round, verdict SHIP WITH MINORS.
  - Reads like the Knight: hits graded, stances apart (brace wide and low, hold the line forward).
  - The bow arm swings out and back in the heavy hit and never folds in. In the front view at the heavy start, the bow tip sits near the chin; this is depth overlap only, as for the archer.
  - No clipping through the body or mantle, and no floor breaks, pops or wrong facing seen.
- **Open minors.**
  - Heavy front peak leans about 55-65 degrees, slightly past the archer's accepted 45-55.
  - Medium hits from behind (about 60 degrees forward) and from the left (the head pitched far down) read close to heavy. Their recovery cells are still hunched.
  - The plain idle weight shift is faint in the front view. The deepest sink shows in brace and combat ready more than in the idle.
- **Harness.** geo-knight suite with `GRAVES_GEO_CHECK_TROOP=stalker`: 22 `[p1] p1-` lines, 2 `=false`.
  - `attack-sync-stand` matched 8 of 9 swings; one swing logged `clipnull`.
  - The suite line is false only because that check failed.
  - The attack clips are byte-identical to the previous export, so this is not caused by this pass. The archer showed the same miss, under lag.
  - Not rerun: the client budget for this pass ran out.
- Video: `stalker_v2feel`.

## Own builder (2026-11-10, branch anim/stalker-fable-2)

A trial run: all 35 stalker animations rebuilt from scratch on the stalker's own builder, not the shared bow builder. Preview art; not merged.

- **Build.** `python art/anim/stalker/build_stalker_anims.py`, then `python art/anim/common/export.py stalker`. The solver is `art/anim/stalker/stalker_core.py`. The shared bow builder is imported as a library only and is unchanged, so the other bow troops are untouched.
- **Timing.** Clip names, loop flags, lengths and attack markers match the shipped file, so gameplay timing is unchanged.
- **Archer-specific stances.** Hold stands the bow upright at the lead foot like a staff. Wall holds it upright before the chest with an arrow nocked. Ward thrusts it out level like a bar. Shield up is a deep duck under the level bow with the string hand guarding the face (he has no shield). Rest trails the bow at the thigh. Brace is a crouch at half draw. Idle carries the bow one-handed with no arrow on the string; ready nocks one.
- **Footage.** In-game capture suite `stalker-reel` (`test-tools/java/gg/gravesandglory/StalkerReelChecks.java`): 35 segments, 1,446 frames, all checks pass on the second run. Video `C:\Users\kortt\Melty\status\booth-videos\stalker-fable-2.mp4` (90 s, each animation labelled).
- **Review (Sonnet high, sheets, one round).** Fixed: the bow stood upright after death forward (the bow now lies flat in all four deaths); the run and charge reel segments were too short to reach the run gait. Not changed: wall and shield up read weakly head-on (the bow is edge-on from the front; they read from three-quarter in game); light hits are small by design.
- **Tests.** `python -m unittest discover -s tools/anim`: 115 pass. `python tools/verify.py fast`: 696 Python tests, one failure (jar leak guard missing the new reel class), fixed in `tools/release_config.py` and that test re-run alone; the full fast verify was not re-run after the fix.

### Scorecard: 19 of 35 clips miss a knight-derived limit (shipped stalker: 5)

- **Attacks.** String forearm inside the jaw-anchor volume (pen_limb 3.9 px, limit 1.4); volley torso speed 76 (limit 60); direct arm speed 164 (limit 160).
- **Stances.** pen_limb 1.8 to 2.7 px on ready, wall, ward and the three aims (limit 1.79); pen_weapon 1.6 to 2.7 px on wall, shield up, hold and rally (limit 1.49); fallback misses four limits (arms 9.0/8, legs 68/65, pen_weapon 1.16/1.01, pen_limb 2.0/0.6).
- **Movement.** Run torso speed 19.7 (limit 19.1); charge pen_weapon 2.9 px (limit 2.4).
- **Hits and stun.** Medium left weapon speed 102 (limit 100); stun loop_vel 10.3 (limit 9.4).
- **Deaths.** Weapon speed as the bow is let go: back 91, left 121, right 85 (limit 83).

### Open points

- **Ward is unreachable in game.** A troop with fallback clips plays those for `fall_back` (StanceController), so `stance_ward_upper` never plays; the reel's ward segment shows fallback standing.
- **Knockback and pulled** are filmed in place; the capture does not slide the body.

## Loose on tick 22 (2026-11-11, branch anim/stalker-bow-22)

Kort set the bow family's loose at tick 22 (it was 16), same damage per arrow. The stalker's two shots follow; nothing else changes.

- **Base.** The own-builder commit (anim/stalker-fable-2, 9a54ce6d) replayed onto anim/bow-22 at 320f0423, which holds the family timing (`sheets/design.json`, `Spec.java`, `build_bow_anims.py`). One conflict: the exported `stalker.animation.json`, a generated file; the own builder's copy was taken and then rebuilt.
- **Remap.** The shot's times stay written on the 16-tick clock and go through the bow builder's `at()`: stretched by 22/16 up to the loose, 6 ticks later after it. Clips are 32 ticks, `full` at 16.5, `impact` at 22. The builder reads `ATTACK_TICKS`, `FULL_AT` and `IMPACT` from the bow builder, so the family's timing has one home. The held draw's tremble (2.3 ticks a shake) and the kick after the loose keep their own speed.
- **Proof that only timing changed.** Built unchanged at the start: 57 of 57 clips identical to the committed file. The 16-tick shot rebuilt on today's bow builder: identical to the committed clips. After the remap: 53 clips identical, the 4 shot clips changed. New tick `at(t)` against old tick `t` wherever both fall on the sample grid: no bone pivot moves more than 0.10 px and no bone turns more than 0.5 degrees (cloth aside, which is a spring in real time).
- **Scorecard.** The same misses as before on the two shots (volley torso speed 76.1; direct arm speed 163.8; pen_limb 3.72 and 3.98, were 3.89 and 3.98); no new miss, none removed. The other 33 animations are byte-identical.
- **Also changed.** `StalkerReelChecks.ATTACK_FRAMES` counts a 32-tick clip; `art/anim/archer/booth.stalker.json` has `impact_tick` 22.
- **Tests.** `python tools/verify.py fast`: PASS. The in-game reel was not re-run.
- **Footage.** `C:/Users/kortt/Melty/status/booth-videos/stalker_bow22_direct.mp4` and `stalker_bow22_volley.mp4`: the new shot beside the 16-tick one, tick by tick.
- **Review (Sonnet high, sheets and the exported numbers, one round): ship with minors.** Nothing but timing differs; the loose and what follows match the 16-tick clip to the decimal; no stall in the slower lead-in. Minors, none changed: the tremble sits at a different phase in the last ticks of the hold (it keeps its own period by design), the first tick after `full` is a little softer than before (the stretched ease-out), and the lead foot's 10 degree roll in a quarter tick at the plant was already in the chosen clip. Not checked: the clips in motion in game, the seeded hold on top of the 5.5-tick hold.

## Replayed onto the reviewed bow lane (2026-11-12, branch anim/stalker-on-bow)

The stalker as chosen (642f0027) replayed onto `anim/review-bow` at 8b2682cb so the two can land together.

- **Conflicts.** `Phase1Checks.java` and `tools/release_config.py`: the bow lane already registers `stalker-reel` and lists
  `StalkerReelChecks` (it holds an earlier copy of the own-builder commit) and adds `KnightOverheadReelChecks` and the `Ai*`
  leak-guard entries; the bow lane's side was kept, which contains both intents. The second commit applied cleanly.
- **Rebuild.** `python art/anim/stalker/build_stalker_anims.py`, then `python art/anim/common/export.py stalker`. The bow troops'
  `bow_fix_layer.py` + `export.py` re-run for archer, longbow, bonebow and wm_hunter: no file changed.
- **Clips against 642f0027.** 53 of 57 identical. The four deaths differ in `left_hand` rotation only (positions 0.00 px; largest
  key delta 43 to 94 deg, Euler form of the bow's rest turn). Cause: af51daad (rig export mirrors a weapon cube's turn into the
  geo) changes the bow cubes in `out/preview/*.geo.json`, which the flat-bow search (`rest_hand` over `K.body_corners`) reads.
  Rebuilt against the 642f0027 preview geo, all four deaths are identical to 642f0027. Renders at ticks 8/14/20: the same falls;
  in death left the bow lies along the ground at tick 14 instead of hanging below the body.
- **Scorecard.** 18 of 35 clips miss (642f0027: 19); the same misses except death back is gone, death left weapon speed
  121 -> 100, and hold / charge pen_weapon read 1.74 / 3.21 (were 1.62 / 2.94) on the corrected bow cubes (clips identical).
- **Focus check.** 101 FAIL, 72 WARN, the same as the 642f0027 clips scored on today's geo; only death WARN values move.
- **Timing.** Every clip length and marker identical to 642f0027.

## Review pass (2026-11-13, branch anim/stalker-on-bow)

One review pass, like the other four bows. The two shots and every clip's timing are untouched (attack_volley,
attack_direct and their legs are byte-identical to 0c165ab1; animation_length, loop and timeline match on all 57 clips).

What changed (builder only, nothing shared):
- The boots (`<side>_foot_gear`, children of the shin) now turn with the foot about the foot's pivot (the knight's
  boot_follow). They sank 1.7 px into the floor in every standing clip. In additive hit legs the boot offset is solved
  against the ready stance's foot.
- The hood fur, hip wraps and shoulder plates swing in the hits and the stun (fullbody.soft_motion, low-passed); the
  stun shake-off gets a lagging shoulder-plate jump. Hits end exactly on zero.
- Grip and aim nudges: idle and carry grip out of the hips, aims turn 20 deg more in the hips and the half-draw hand sits
  1.6 px out from the cowl, ward / shield_up / fallback hands forward, a rally swing key, run tuck and string-hand hint,
  hit_block grip, knockback hips drop 2 px so the planted knee stays bent (the -9 deg shin clamp was sinking the foot).
- Tried and put back: brace chest yaw 46 (made scorecard pen_limb fail), wall grip out (bow into greave), charge yaw 50
  (forearm into breastplate), stun grip out, hit bow pushed out.

Rows: focus FAIL 101 -> 50 (WARN 72 -> 72, no row got worse), clips with a FAIL 31 -> 24. Fixed: floor 29, settle 11,
secondary_life 9, armour 1, weapon 1. Scorecard 18 -> 17 failing clips (24 -> 22 rows; the aim clip and fallback
pen_weapon pass now). Scorecard cannot exit 0 while the approved shots fail it.

Still open (two attempts each, then recorded; no exemption added):
- Shots (approved, not touched): volley limb 2.99, floor 1.71, armour 4.32, secondary_life, elbow 175, head_turn 126;
  direct floor 1.72, armour 4.0, whole_body 1.53, secondary_life, elbow 167, shoulder 139, head_turn 99.8, jitter 63.7.
- The string forearm crossing the chest into the fur collar (`knee_mantle`) and breastplate: stance_ready limb 2.74 /
  armour 2.63, wall, ward, shield_up, fallback, run, and every hit at 2.78-2.80 (inherited from the stance, 0.16-0.18
  above it). The left arm cannot reach further out; a model change (collar or breastplate) is the likely fix.
- Aims: the anchor at the jaw is inside the fur cowl (armour 3.1-3.7, limb 2.0-2.6) and head_turn 96-128: model
  geometry and the shared shot anchor.
- brace / charge head into right_pauldron 2.8-2.9; charge bow into breastplate 2.01; run bow into greave 1.65;
  stun bow into hips 1.96; stun_out bow 1.78 and end rows inherited from stance_ready; walk armour 1.91; rally 2.35.
- Scorecard not attempted: death_left/right and hit_medium_left weapon speed, stun loop_vel, run torso, fallback legs.

## Fix round 2 (2026-11-13, branch anim/stalker-on-bow)
Builder only (`build_stalker_anims.py`, `stalker_core.py`). The two shots and their legs are byte-identical to
2e04d4ec. `animation_length`, `loop` and `timeline` match on all 57 clips. No model, design.json, scorecard, focus
or exemption change.

What changed:
- Ready stance: the string hand no longer holds the nock across the chest. It rests on the lower string by the right
  hip (new solver goal `slide`, 0 = nock, 1 = lower limb tip), arrow still nocked, bow canted down to the left. The
  shots keep the old nocked guard (`READY_SHOT`), and brace, wall and the stun build on it. stance_ready limb
  2.74 -> 1.23 (WARN), armour 2.63 -> pass, scorecard pen_limb 1.799 -> pass.
- Wall, ward, fallback: same low string hand, bow forward and canted. shield_up: bow held flat across the chest,
  string hand high on the string. hold: bow out from the hips. Rally swing keys moved out. Aims: half-draw anchor
  2.5 px further out (aim_down 3.5). Brace and charge look down 10 deg. Charge grip and hand hints re-solved.
- Fallback: the bow follows the chest in the step; the back gait lifts the feet 0.85 (was 1.5).
- Run: string hand tucked lower and wider, chest swing 5.4 (was 6.0).
- Hits (additive over the new ready): the bow follows the chest twist (`BOW_FOLLOW`), lean only forward; the string
  hand lets go of the string by half the blow; right-side hits keep the bow still and half the kick; knockback holds
  the grip forward and tears slower.
- Stun: eased loop phase, bow lower and grip back. Deaths: hand turn clamped 12 deg (was 32).
- New optional solver goals in stalker_core: `relbow` (elbow target) and `slide`. Unused by the shots.

Rows (shots counted apart; shots unchanged at 14 FAIL / 8 WARN focus rows and 4 scorecard rows):
- Focus, other clips: FAIL rows 36 -> 19, WARN 64 -> 77, clips with a FAIL 22 -> 15. Troop: 50 -> 33 FAIL rows,
  24 -> 17 clips.
- Scorecard, other clips: 18 -> 2 rows, 15 -> 2 clips.
- Got worse in status: hit_knockback armour WARN 2.64 (stance-exempt) -> FAIL 2.31 (value lower; the stance
  exemption no longer applies because ready passes). New WARNs: bow into body 0.81-1.12 in six hits, left_pauldron
  still in four medium hits (secondary_life 3.04), hit_light_right dead_hold 3.25, death_forward weapon_in_arm 2.19.
  aim_up limb 2.55 -> 2.75 (was already FAIL).

Hand-off ready -> shot (largest upper-body or hand difference): tick 0 was 3.2 deg / 0.52 px, now 84.8 deg
(right_arm) / 13.2 px (right_hand). Last tick back to ready: volley 85.2 -> 146.9 deg, direct 79.6 -> 71.8 deg, about
15 px both. The draw-up jump is now visibly larger. AttackController's FADE_IN (2.5 ticks, smoothstep) covers the
entry and FADE_OUT (6 ticks) the exit; peak about 51 deg per tick on entry.

Still open (two or more attempts each; model parts named where only geometry clears them):
- charge_run armour 2.86 (head into right_pauldron), weapon 2.02, scorecard pen_weapon 2.728; brace armour 2.64.
  Right pauldron (x -10.3..-3.1, y 22.8-27.6) sits where the lowered head goes.
- Aims: armour 1.94 / 3.08 / 3.31 and head_turn 115-123 (aim_down, aim_up), aim_up limb 2.75. The fur_cowl flaps
  (x +-5.3..6.7, y 21-28) sit on the shot anchor.
- walk 1.91 and run 2.05 armour: left bracer into mantle_front_l, the knee_mantle collar (x +-6.8, y 19.7-25.3,
  z +-3.8). Bracers are about 4.6 px wide.
- Hits armour 1.98-2.36: fur_cowl into pelt_tail or pauldrons, mantle_front_r into right_bracer (cloth parts).
- rally armour 2.34, scorecard pen_weapon 1.614; stun bow into left_greave 1.9.
