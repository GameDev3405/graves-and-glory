# tc_admiral review record (2026-11-06)

The third spear unit, the Tide Court admiral, with a trident, a cape, coat tails, drifting hair and a kelp hem. It uses the shared spear builder: `GG_SPEAR_TROOP=tc_admiral python art/anim/spear/build_spear_anims.py`. Its rig comes from `art/anim/common/troop_rig.py tc_admiral`, with the kelp hem re-hung on a leg bone through the `HANG` table. It has the same clip set as tc_tideguard: thrust, high thrust and low sweep, each with a `_legs` clip, plus locomotion, stances, hits, block and four deaths.

- **Gear lag:** the knight's cape lag drives the cape (0.35) and both coat tails (0.7). Hair, kelp, plume, tails and sash ends are `soft`: they never hold the falling body up and settle on the ground after it.
- **Fix: kelp hem re-hung and settled.** The hem rides the leg it hangs from, so a stride no longer pulls it through the thigh, and it settles on the ground in the deaths.
- **Fix: rigid back hair.** Only the drifting hair (`hair_adrift`) counts as soft; the back hair stays fixed to the head, so it no longer folds into the skull in a fall.
- **Fix: dropped trident clears the floor.** `body_ground` measures the floor from the hard body only (soft strands hang below it until they settle). `death_step` 0.25 re-keys the fall on every bake sample, so a fast wrist swing cannot dip the trident under the floor between keys. `death_top` 0.15 keeps the trident's top low in a roll rather than standing up off the body.
- Every new per-troop setting defaults to off, so wm_tusk and tc_tideguard clips are unchanged.

## Review

- **Round 1 (Sonnet high, reel `reel_adm_v1`): SHIP WITH MINORS.** The attacks held their loads and settled cleanly; the death poses needed a closer look at the trident against the floor and body. That check, with zoomed death frames, led to the fixes above.
- **Round 2 (Sonnet high, reel `reel_adm_v2`): PASS. No blockers.** The thrusts and the sweep keep the tideguard's load, fast contact, overshoot and exact settle; the deaths lay the trident beside the body with no floor clip.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false.
- **Verdict: finished.** Video: `tc_admiral_v2.mp4`.

## Open minors (polish, not blocking)

- **Forward death** (t9.8–14.6 of the clip): the trident jerks upward once while the wrist settles it.
- **Wrist turn** at t2.75 of a death: a 76° turn inside one tick reads as a quick flick.
- **Late deaths** (t24+, three-quarter and front views): the trident head sits slightly under the torso silhouette.
- **Kelp hem**: it can sit about 0.4 px into the leg at the extreme of a stride.
- The reel's resolution makes the soft parts (hair, kelp, tails) hard to judge finely.

## v2 feel (2026-11-07)

The knight's approved v2 style is ported to the spear builder (`art/anim/spear/build_spear_anims.py`), the same port as wm_tusk and tc_tideguard, kept to the admiral's own spear and command bearing. `GG_V2=0` builds the old style. Deaths are unchanged.

- **Pelvis-driven blows:** the thrust, high thrust and low sweep each load into the back hip, then the pelvis turns and drops into the blow and the spear follows the hips, point leading. Both fists stay on the shaft.
- **Graded hits:** a light blow is a knee flinch at 0.4 of the peak. A medium blow throws the upper body further, holds it a beat, shifts the pelvis and catches it with a recovery step. A heavy blow plays the medium clip at 1.55 times its size, so it reads as a stagger. A blow from behind no longer drives the spear through the body and under the floor (HEAD: 5 shaft-in-body samples, the point 2.9 px under the soles).
- **Idle:** the knight's weight cycle (breath, then a slow shift from foot to foot).
- **Stances:** ready is a forward guard. Brace raises the point at an angle. Hold the line sets the spear level, low and wide. Ward turns the body with the point up. All four read apart.
- **Probe:** no shaft or fist in the body in any clip, nothing below the floor beyond the sole depth HEAD already had, no new pops.
- **Byte identity:** knight, archer and gl_marshal build byte-identical to HEAD.
- **Review (Sonnet high, sheets, the knight v2 and tideguard v2 comparison): PASS WITH MINORS**, no fix round.
- **Harness:** `geo-knight` with tc_admiral, run twice (the machine was loaded by other builds): 22 `[p1]` checks each run. Run 1 failed only the three march-run checks under server lag (up to 199 ticks behind, run clocked as walk); run 2 failed only attack-sync-walk (3 swings sampled, all nudged by lag) and the suite roll-up. Every check passed in one of the two runs; the gait clips are unchanged from HEAD.
- **New minors:**
  - The light hit from the front barely moves before t6, so the flinch reads a little late.
  - From the front the hip load reads only from t8 (it is clear from the side).
  - The plain idle's weight shift is subtle next to the knight's.
  - The medium front hit throws the spear tip high and wide, the largest silhouette change of the set.
  - The thrust's fastest joint step is 109 deg/tick on the left forearm at t6.5 (as the tideguard); it reads as the lunge snap.

## Batch review 2026-11-11

Batch review by the spear lane worker (branch `anim/review-spear`, cut from `anim/fix-spear`); two fix rounds, then stopped.
Builder: `art/anim/spear/build_spear_anims.py` (every change gated on the wide guard: ic_breaker untouched). Passes added,
each touching only a clip that fails: despike (arm pops), soft motion on attacks, hits, stun and shake-off (plumes damped),
plate_clear (shoulder plates slide down their own arm off the helm, every bone pair judged), head_clear (head turns off a
passing forearm), cloth_clear (tabard, kilt, sash swing off a stepping thigh), keep_left_fist (hits), floor_guard, and the
low-sweep blade-roll curve (grip_roll). Deploy splices only the clips that had a FAIL (`--deploy=<list>`); the shared
modules' rally and charge run keep their own keys. Focus "after" is judged with the additive hits and the shake-off
composed over the pose the game adds them to (stance t0, stun t4); "before" was judged over the rest pose, so before and
after rows of hit_* are not like for like (check fault, reported to the lead).

- **Guard grip:** kept at the deployed 10.1 px (see the tusk; not the same hold).
- **Deaths:** not deployed (drift only; kept byte-identical).
- **Focus:** before 22 clips / 36 rows; after 18 / 19 (composed). Fixed: every secondary_life but hit_block, all floor,
  attack pops, grip_roll, hit_medium_back knee, knockback weapon/vambrace rows, the hair and plume pops.
- **Open:** sash_end into the left greave/leg in walk, run, idle, stances, high thrust and hits 1.93-2.27 (pre-existing,
  hidden before behind the head rows; cloth pass measured below 1.9 or swung it the wrong way); head into left_pauldron
  in ward 2.89, rally 2.58 (rally from stances.py: not edited here, did not fail before), stun 2.7, knockback 2.17,
  charge 2.04, idle 2.14; wall right_epaulette into torso 1.97; hit_medium_back cape pop 14.3.
- **Scorecard: exit 1 (blocker).** pen_limb attack_thrust 1.41, high_thrust 1.59 (limit 1.39); stun_upper loop_pos 1.76
  (limit 1.09; the stun seam pass). Before: exit 0.
- **In-game: FAIL (one check).** geo-knight, roots tc_admiral-review1 and -review2: 22 `[p1] p1-` lines, both runs
  `attack-sync-walk=false` (3/3 swings matched, 3 nudged; the tideguard and tusk get 4/4, 2 nudged). Every clip length,
  loop flag and impact marker is unchanged from HEAD; whether HEAD fails it too was not run.
- **Tests:** `python -m unittest discover -s tools/anim`: 152 OK. `python tools/verify.py fast`: FAIL on one timing test only
  (test_where extract 53.7 s, limit 30 s, machine under heavy load); every other check PASS.
- **Looked at:** three-view sheets of high thrust, low sweep, thrust, hit_medium_front, fallback and stun (work/render/),
  every second tick, not every tick.

