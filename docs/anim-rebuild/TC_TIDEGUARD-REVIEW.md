# tc_tideguard review record (2026-11-06)

The second spear unit, the Tide Court tideguard. It uses the shared spear builder: `GG_SPEAR_TROOP=tc_tideguard python art/anim/spear/build_spear_anims.py`. Its rig comes from `art/anim/common/troop_rig.py tc_tideguard`. It has the same clip set as wm_tusk: thrust, high thrust and low sweep, each with a `_legs` clip, plus locomotion, stances, hits, block and four deaths.

- **Round 1 fix: rear fist out of the torso.** At thrust contact the butt of the shaft passed through the torso. Each troop now has a `rear_x` offset for the rear (right) fist; tideguard uses -0.6 px. The build log shows 0 samples with the shaft in the body in all three attacks.
- **Round 1 fix: death spear lookahead.** In the deaths the wrist could flip the spear to the other side between adjacent keys. Troops can opt in to `death_lookahead`, which makes `flat_weapon` choose each wrist turn with the next half tick in view (from t3.0). With it, the spear lifts, swings and lies flat with no flip or spin.
- **Shared builder changes in this commit:**
  - The knight `death_clip` gains `SOFT` (bone-name prefixes of hanging hair or kelp, which never hold up the falling body) and a ground settle for those strands.
  - `troop_rig` gains a per-troop `HANG` table to re-hang body gear on a leg bone.
  - Both default to empty and only affect tc_admiral (next unit). Tideguard's clips are unchanged by them.

## Review

- **Round 1 (Sonnet high, reel `reel_tide_v1`): SHIP WITH MINORS.** The butt clipped through the torso at thrust contact, and the spear flipped in the deaths. Both are fixed above.
- **Round 2 (Sonnet high, reel `reel_tide_v2`, plus dense side strips of each attack): SHIP WITH MINORS. No blockers.**
  - All four deaths: the spear swings smoothly and rests on the ground beside the body, with no flip or spin.
  - Thrust: a held load with a coil (t296–299), fast contact (t300–302), a lunge with a hip drop and torso turn, an overshoot, then a settle back to guard. The butt exits behind the hip and no longer cuts the torso.
  - Regression check of loco, stances, hits, ready idle, high thrust and low sweep: no pops and no weapon through the body.
  - That was the second fix round, so the remaining items are recorded below.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false (attack sync 34/34 standing, 4/4 walking).
- **Verdict: finished.** Video: `tc_tideguard_v2.mp4`.

## Open minors (polish, not blocking)

- **Behind death** (t631+, side view): the spear tip leans on the arm, slightly above the ground, rather than lying fully flat.
- **Right death** (t735+, front view): the butt end lightly crosses the lower torso.
- **Left death** (t677, front view): arm and spear overlap for a moment in the pile.
- **Low sweep** (t368→t369): a quick drop of the spear to horizontal. It reads as a fast strike, not a pop.
- At contact the thrust and the low sweep both end near horizontal, so they look somewhat alike.

## v2 feel (2026-11-07)

The knight's approved v2 style is ported to the spear builder (`art/anim/spear/build_spear_anims.py`), the same port as wm_tusk, kept to the tideguard's own spear and set-back rear hand. `GG_V2=0` builds the old style. Deaths are unchanged.

- **Pelvis-driven blows:** the thrust, high thrust and low sweep each load into the back hip, then the pelvis turns and drops into the blow and the spear follows the hips. Both fists stay on the shaft.
- **Low sweep:** its yaw keys follow the set-back rear hand, so the shaft stays clear of the torso under the new pelvis lead.
- **Graded hits:** a light blow is a knee flinch at 0.4 of the peak. A medium blow throws the upper body further, holds it a beat, shifts the pelvis and catches it with a recovery step. A heavy blow plays the medium clip at 1.55 times its size, so it reads as a stagger. A blow from behind keeps the spear point off the ground (it went 1.8 px under the floor before).
- **Idle:** the knight's weight cycle (breath, then a slow shift from foot to foot).
- **Stances:** ready is a forward guard. Brace crouches deep with the point raised. Hold the line sets the spear level, feet wide and the torso side-on. Ward turns the torso with the point up. All four read apart.
- **Probe:** no shaft or fist in the body (the old hit from behind had 5 samples), nothing below the floor, no new pops.
- **Byte identity:** knight, archer and gl_marshal build byte-identical to HEAD.
- **Review (Sonnet high, sheets and the knight v2 comparison): PASS WITH MINORS**, no fix round. Its question whether the medium hits finish at guard after the sheet's last frame (t12) was checked in the clip data: every hit clip ends exactly on its first key (medium 16 ticks, light 9).
- **Harness:** `geo-knight` with tc_tideguard: 22 `[p1]` checks, 0 false (stance, hit, attack and death checks all passed; the server lagged during start-up only).
- **New minors:**
  - The thrust's fastest joint step is 109 deg/tick on the left forearm at t6.5 (87 before). It reads as the snap of the lunge.
  - Thrust and low-sweep contacts both end near level; the sweep reads apart by its deeper crouch and shoulder swing.
  - Brace is close to ready in profile (crouch depth and spear angle only).
  - The idle shift is subtle next to the knight's.

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

- **Guard grip:** kept at the deployed 10.1 px (already wider than the 8.0 ask; it does not hold the spear like the tusk).
- **Deaths:** not deployed (drift only; kept byte-identical).
- **Focus:** before 17 clips / 35 rows; after 17 / 20 (composed). Fixed: every secondary_life but hit_block, all floor,
  attack pops (23.4, 17.8), grip_roll, stun_out tabard, hit_medium_back knee. Clips with no FAIL before are byte-identical.
- **Open:** thrust/high-thrust bracers in each other 2.03/2.04 (spreading the fists to 6 px put the right bracer in the
  breastplate, 2.46: rejected) and breastplate into left_pauldron 2.15/2.05 at t0.5-1 (new row: the pass's slide);
  walk/low-sweep/hit tabard into thigh 1.91-2.36 (cloth pass measured below 1.9); stance wall/ward/fallback and stun
  plates in the helm 2.36-2.65 (the slide down a forward-held arm runs into the breastplate); composed hits head into
  left_plate 2.0-2.5 (the stance's own fit plus the hit); knockback left forearm into breastplate 2.53.
- **Scorecard:** exit 0.
- **In-game:** geo-knight, root tc_tideguard-review1: 22 `[p1] p1-` lines, 0 `=false`, suite passed.
- **Tests:** `python -m unittest discover -s tools/anim`: 152 OK. `python tools/verify.py fast`: FAIL on one timing test only
  (test_where extract 53.7 s, limit 30 s, machine under heavy load); every other check PASS.
- **Looked at:** three-view sheets of high thrust, low sweep, thrust, hit_medium_front, fallback and stun (work/render/),
  every second tick, not every tick.

## Tabard (2026-11-13)

Kort chose to fix the recorded tabard debt in the model ("Tabards"). Both tabard panels are 7 rows tall in place of 9: the
hem ends 2 px below the hips (was 5) and the top is 1 px higher (`art/hand/tc_tideguard/build_tc_tideguard.py`, a0cfce55).
Same width, standoff, bone, paint and UVs; no new bones.

- Tabard armour rows: 16 WARN and 3 exempt before, 5 WARN and 0 exempt after. The three exemptions (hit_pulled,
  hit_medium_right, hit_medium_left) are removed. Focus check 0 FAIL rows; scorecard 31 clips, 0 failing.
- The clips are the reviewed ones, byte-identical. **A rebuild of this troop's clips with the new model re-keys 17 of
  them** (the spear builder solves the tabard against its geometry; the brace stance and three death wrists follow).
  That rebuild was not deployed. Whoever next runs `GG_SPEAR_TROOP=tc_tideguard build_spear_anims.py` must review those
  17 clips before committing them (a copy of the rebuild is in `work/modelfix/out_new.json` on the lane, untracked).
- Report: `bb-anim-skill-fable/work/batch/reports/modelfix_tc_tideguard.md`; sheet `work/modelfix/tc_tideguard_sheet.jpg`.
