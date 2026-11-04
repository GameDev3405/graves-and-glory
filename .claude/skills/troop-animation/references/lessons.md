# What works and what to avoid

Mined from the animation history: commits on main and `work/anim-rework`, `docs/ANIMATION-EDITING.md`,
`docs/reports/`, `work/anim-review-2026-11-04.md` (the aggressive review, kept in the main checkout's `work/`) and
`docs/reports/mode-gate-timing-2026-11-04.md`. Each lesson names the failure, the evidence and the fix that worked.

## Lessons log (keep it going)

Every fix pass keeps a dated lessons file, `work/anim-lessons-YYYY-MM-DD.md` in the main checkout, with one entry per
lesson: what went wrong, how it was found (reviewer, check, Kort), the fix, the commit. At the end of the pass, fold the
durable lessons into this file with their commit hashes. Start by reading the newest lessons file, for example
`work/anim-lessons-2026-11-04.md` from the attack-clips fix pass.

## Contents
1. Failures that shipped and must not come back
2. Weapon facing
3. Timing and weight
4. Joints and rig
5. Gait
6. Review evidence
7. Process
8. What worked

## 1. Failures that shipped and must not come back

- **18 of 30 troops had no attack animation at all, and every check passed.** Only bow, caster, sword_shield and
  common had clips. `TroopMotion.build` gave those families no attacks, the engine started nothing, ClipLibrary only
  logged a warning, and the contact sheet filled the attack columns with idle or hit frames, which told Kort four
  Houses swing. Fix (674b97e0): the clip build refuses a missing attackVariants clip, a test asserts every troop
  resolves its attacks, the turn suite prints NO CLIP and fails. **Rule: a missing clip fails the build; never shoot or
  grade a substitute.**
- **14 families had no clips again; attackIndex stayed -1 and no check failed.** Fix: `clips.py` fails when a used
  family's attackVariants row has no clip, `test_attack_clips.py` asserts every troop resolves every row, and the Turn
  capture prints NO CLIP. The guard runs before `clips.py` writes anything, so iterate one family at a time with a
  scratch wrapper that stubs `clips.unclipped` and calls `clips.main([])`.
- **14 families had no clips again; attackIndex stayed -1 and no check failed.** Fix: `clips.py` fails when a used
  family's attackVariants row has no clip, `test_attack_clips.py` asserts every troop resolves every row, and the Turn
  capture prints NO CLIP. The guard runs before `clips.py` writes anything, so iterate one family at a time with a
  scratch wrapper that stubs `clips.unclipped` and calls `clips.main([])`.
- **Backwards elbows in every hand-keyed sword and bow clip.** Forearm rx was positive: overhead +38 at impact,
  overhead_wall +71/+72, thrust_wall +65/+68, bow_direct and bow_volley +62/+63, bow_draw +61, aim +51, hits +3..+9.
  The engine's own gait used the right sign, so the clips contradicted it. When batch-1 bow troops gained forearm bones
  (071b7ffb) the keys landed on real bones and three more troops broke. Fix: re-key as flexion (negative), add default
  joint limits in `handlib.limb()`, and a check that no clip key drives a forearm above 0 or a shin below 0.
- **The wrist covered for a broken elbow.** thrust_wall held the hand at -93..-68 to point the tip forward against a
  +65 elbow. Fixing the elbow then breaks the wrist. Re-key the whole chain together, never patch one bone.
- **The knight overhead read as a salute and then as follow-through.** Wind-up: arm -120..-132 with a straight
  (hyperextended) elbow, blade straight up, not cocked. Impact: blade down and back by the hip. Fix: cocked wind-up
  (arm -150, forearm -100, hand +20), impact tip forward at head height (arm -80, forearm -15, hand -10).
- **Clip timing changed battle results.** After the rework merged, Siege attacker wins went from 4/6 to 0/6; no single
  timing rule explained it (diffuse tempo loss against a yard clock with no slack). Animation timing is gameplay.

## 2. Weapon facing (Kort, 2026-11-03: "this mistake keeps coming back")

- Check the blade at every key, not only the impact, from front, 3/4 and side. A clean pose with a wrong blade is wrong.
- The wall overhead flipped the wrist 158 degrees in 90 ms (+65 to -93): the blade spun in the palm and catmull-rom made
  the in-betweens point sideways. Take the arc with shoulder and elbow; keep the wrist within about ±40 through a strike (only on models with a forearm bone; where right_arm goes
  straight to right_hand the wrist carries the blade and the limit does not apply);
  linear keys on the strike segment. Lint any change over 90 degrees in 100 ms.
- The thrust read as a point from the front. Turning the arm out about 6 degrees at impact (408dc109) made it read as a
  blade line; keep the tip within 3-8 degrees of forward through impact (fe59569f; was 20-35).
- Upright weapons with no grip offset (staff, spear, trident) ran up through the forearm and pauldron. Carry them 3 px
  in front of the fist with the tip leaned 10 degrees forward (9c077960); staffs and banners use the "staff" hold, 12
  degrees forward and 8 out (32297c95).
- Bows covered the face. Stand the archer side-on, cant the bow (52 degrees off the face, 13ab2123), anchor below the
  chin, drop the bow shoulder a pixel (4520fa83, 6d2dbd9e).
- Spear carry with the tip trailing behind means the thrust's first keys blend from a backwards weapon. Carry at the
  port or shouldered, tip forward; author the thrust from a couched guard with the tip on target from the wind-up.
- Knight walk held the sword flat at shoulder height pointing back because the gait arm swing stacked on a carry that
  already held the arm. Scale the weapon-arm swing down while a carry keys it; carry point-up or low and forward.
- Tidecaller raider's walk put the sword behind the head like a fin. Check the walk column for weapons too.

- **Aim the blade numerically, not from stick renders** (their side views sort depth wrongly). Side plane: 0 down,
  90 forward, 180 up, arm angle = -rx. Sword grip [90,45,90]: blade = arm + 135 - wrist; upright grip: arm + 164 -
  wrist. Print arm angle, blade angle and tip at the windup and impact markers, and the lowest tip over the whole clip
  (catmull-rom overshoot between impact and follow-through drove tips 4-6 px into the ground).
- **Lead metric:** cosine between the weapon's +x (bit or edge) and the tip velocity at impact; at least 0.75 on cuts.
  It caught a reap moving perpendicular to its blade (0.1).
- Do not `lag` the wrist on a thrust (blade ends 20-30 degrees off at impact) or the off-hand arm on a two-handed
  weapon (hands separate at impact).
- Two-handed weapons: FK grid search the off-hand arm toward a grip point on the weapon (`both_hands` in
  `build_attacks.py`); hands go from 14-19 px apart to 5-9. Hand-tuned values never converged.
- rz on a raised arm (rx about -110) flips it to the other side; use negative rz to go out to the right and check the
  tip's x. On a forward-held arm rz only spins the arm; lateral motion comes from ry (positive ry turns a left arm in).
- Blades on the hand's +x (scythe): the weapon's +x lies on the wrist's rx axis, so arm and wrist pitch cannot aim it.
  Sweep laterally with body ry; a wrist ry twist swings the whole snath. The diagonal scythe hook reaches a lead of
  only about 0.45 (accepted).
- Models with no forearm bone (right_arm to right_hand): forearm keys do nothing; carry the swing with shoulder and
  wrist.

- **Aim the blade numerically, not from stick renders** (their side views sort depth wrongly). Side plane: 0 down,
  90 forward, 180 up, arm angle = -rx. Sword grip [90,45,90]: blade = arm + 135 - wrist; upright grip: arm + 164 -
  wrist. Print arm angle, blade angle and tip at the windup and impact markers, and the lowest tip over the whole clip
  (catmull-rom overshoot between impact and follow-through drove tips 4-6 px into the ground).
- **Lead metric:** cosine between the weapon's +x (bit or edge) and the tip velocity at impact; at least 0.75 on cuts.
  It caught a reap moving perpendicular to its blade (0.1).
- Do not `lag` the wrist on a thrust (blade ends 20-30 degrees off at impact) or the off-hand arm on a two-handed
  weapon (hands separate at impact).
- Two-handed weapons: FK grid search the off-hand arm toward a grip point on the weapon (`both_hands` in
  `build_attacks.py`); hands go from 14-19 px apart to 5-9. Hand-tuned values never converged.
- rz on a raised arm (rx about -110) flips it to the other side; use negative rz to go out to the right and check the
  tip's x. On a forward-held arm rz only spins the arm; lateral motion comes from ry (positive ry turns a left arm in).
- Blades on the hand's +x (scythe): the weapon's +x lies on the wrist's rx axis, so arm and wrist pitch cannot aim it.
  Sweep laterally with body ry; a wrist ry twist swings the whole snath. The diagonal scythe hook reaches a lead of
  only about 0.45 (accepted).
- Models with no forearm bone (right_arm to right_hand): forearm keys do nothing; carry the swing with shoulder and
  wrist.

## 3. Timing and weight

- Catmull-rom through the top of a swing never stops the arm: no loaded beat. 3197 of 4930 keys were catmull-rom.
  Hold the top 150-250 ms (two equal keys or flat bezier handles), strike 60-100 ms linear or ease-in, ease out of the
  recovery. Catmull-rom for idles and hits only.
- The overhead impact rose 1 px (root y -1) with no knee keys: no mass. At impact root y +1.5..+2, knees +15..+25,
  front foot planted, root z forward 2-3 px; hold through the contact, recover over about 0.4 s.
- A recovery longer than the strike at an even ease floats. Follow through to a low blade, pause 0.15-0.2 s (the
  Souls punish window), then return quickly.
- The bow shot read too fast; Kort chose a slower draw (loose at 0.8 s of 1.8 s) and cooldown +25% (b711c5f3). Root
  keys let the archer settle into the draw (+y dip), give at the loose and rise back. Kort approved the side render.
- Bow troops loosing in lockstep looked mechanical; a per-troop 0-3 tick UUID offset fixed it (bde23063).
- Hit reactions must stay under the hit-over-attack check (15 degrees per tick; the knight v2 hits run 14.7, a4bb848b).

- **Rate limit 60 degrees per 100 ms** (r2 review). Compute it from the clip JSON: the peak per-axis change over any
  100 ms window, linear between keys, on every bone the review grades, before reshooting. Moving a windup key later can
  push the excess into the strike (colossus hit 68 until its hold shrank to 0.18).
- Keys, not only markers, must sit on the 0.05 s tick; clips.py refuses an impact at 0.34 ("not on a tick").
- Make the impact root y the deepest sink, with the follow-through a little shallower; a root rising into the hit kills
  the weight. Kill a held windup with linear segments and an early key at 0.9 of the cocked pose (a creep, not a freeze).
- MotionEngineCheck "hit over attack" caps the sword_shield thrust at 15 degrees per tick (baseline 14.7): any shin key
  gives 18.6 and a root sink of 0.9 or more fails (0.8 passes). Low_cut is not gated.

## 4. Joints and rig

- **Clips are offsets on the stance** (`MotionStack.action`). An elbow or knee that looks right in the clip's own keys
  can be backwards once added to the guard or aim stance, so lint and review the net pose. The review doc's numbers and
  the old lint read the action alone and were wrong for sword_shield and bow. Write net targets and store
  (net - stance).
- Solve a flipped elbow as a pure hinge (forearm ry = rz = 0), keeping hand pivot and world orientation; leaving the old
  twist in cost 3.6 px of hand error on bow_draw.
- `handlib.JOINT_LIMITS` gives every `_forearm` (-150, 0) and `_shin` (0, 150) a runtime limit unless the model sets
  its own, so `TroopMotion.limits` clamps any clip or sum. wm_tusk's custom shin has pivot (0,0,0), is not a hinge, and
  gets none.
- Check a weapon's parent bone before keying the hand. ic_colossus's maul sat on right_arm, so hand keys moved nothing
  and the haft ran back along the arm; parented to right_hand (pivot 0, rot -18), hand rx swings it.
- Gear hung from a thigh or upper arm must stop at the hinge (y6 on a leg). A 3D robe tier below the knee belongs on the
  shin (mage `*_shin_robe`).
- `build_knight.py install` runs `tools/build_skins.py`, which rebuilds all 30 troops; a full sheet stops it
  (pack() names troop and sheet). 2 px joint overlap boxes can overflow a skin (longbow grew to 128 x 128).
- Troops from the unmerged joints branch (071b7ffb) have no forearm or shin bones until it merges.
- bow_draw snaps 169 degrees at the release (0.78 to 0.8) by design; a speed scan flagging it is inherited.

- Jointing troops (071b7ffb, d5f92d0d, 3fdbc22d) took the revenant to 66 bones and the client refused the model.
  Merging static plates into one bone fixed it (3cff44ff) and handlib now fails the build past 64. Plan headroom first.
- A hinge on the box cut plane opens a 2 px notch past about 30 degrees and the inner corner pokes through. At 1 texel
  per pixel that reads as a broken limb. Overlap the lower box 1-2 px into the upper one.
- Gear spanning a joint floats on a bend; split it or end it above the joint.
- 23 of 24 jointed troops had no limits, so additive layers stacked past anatomy. Give every forearm and shin a limit.
- Fractional box sizes (tc_admiral 49/78 boxes, revenant breastplate 3.3 x 6 x 0.8) break 1 texel per pixel; a joints
  rebuild must not carry them forward. Round to whole pixels; `dil` only for layered offsets.
- Clip keys authored on the upright knight point into the ground on leaning troops. Decide one compensation rule.

## 5. Gait

- Current gait (`MotionBody.gaitPose`): swing knee squared and lowered to 22 (run 33 with back-foot kick), walk bob
  1.35, hip sway 0.9 px, body roll 3.5 degrees. `MotionLayersCheck` pins only relations, not these constants.

- The walk looked like a jog: the trailing shin folded to about 90 degrees, sole to the camera, peaking early. Walk
  swing knee 20-25 degrees peaking at mid-swing; 50-60 for runs only; armoured Houses lower still.
- Hip motion was sub-texel (sway 0.35 px, roll 1.5 degrees, bob 0.9 px), so hips looked locked. Walk sway 0.8-1.0 px,
  bob 1.2-1.5 px with the drop at contact, hip roll 3-4 degrees toward the stance leg. Motion under one texel is
  invisible at our scale.
- Foot slide cannot be judged from one frame; trace each sole's world xz through stance (drift at most 0.25 px).

## 6. Review evidence

- Family-wide findings from the attack-clips review (r1, nothing at B+): catmull-rom with no hold or load caps clips at
  C; the root rises into impact on crew, bear and ravager clips; ic_arbalist crossbow rolled 90 degrees; wm_stalker
  floats at the windup; ic_colossus haft crosses the arm at impact.
- The Turn suite freezes one walk frame (walk25); grade the walk from the motion-grade loco sheets.
- Turn suite shots land in `<root>/run/screenshots/p2-turn-*.png`. The alias attack's impact file is `-attack-`, a bow's
  poses are nock, draw, loose, recovery, and named attacks use `<short>_windup` and so on; the sheet script must map
  these names.

- A sheet that shows the walk from behind and the attacks from the front cannot compare silhouettes. One camera relative
  to the troop for every key, plus a side orthographic view for blade and joints.
- Floating frames (3-6 px above the shadow) came from clips that do not key root y; `MotionFeet.ground` only lifts.
- "No failure at sheet resolution" on 150-220 px tiles is low confidence; review attacks frame by frame at full size.
- The grade video once showed a different attack than named; it now fails on a mismatch (072bf2a1). Make every sheet
  name the clip it shows.
- The art QA practice: pass only what the evidence shows. "Not checked" is a valid, required answer.

## 7. Process

- Blockbench is not the game: no weapon, no shield board, no engine layers. Every approval came from in-game renders.
- `art/clips/build_clips.py` overwrites hand edits; it is retired (new families go in `art/clips/build_attacks.py`,
  which imports its Anim and project and swaps `B.RIG` and `B.PIVOT` per rig). Use anim_edit.
- Put loops in scratch Python scripts (for example one that edits a named parameter, rebuilds and checks in one call);
  the worktree isolation hook blocks sed loops and heredoc-plus-cd.
- Kort said "Show me first" (2026-11-01): merges wait for his look. He approved the rework only once motion checks were
  stable (b4dfef01).
- One worker per clip family at a time; another worker on the same Motion*.java or clip folder means stop and ask.
- After two fix rounds, escalate with evidence rather than a third blind pass.

## 8. What worked

- Hand-keyed clips over procedural attacks for anything Kort judges; procedural layers for gait, breath, look, springs.
- Video-style references per family (roadmap), used for timing and silhouette only, never copied.
- The thrust lunge from the guard: sword drawn back to the hip point-forward, driven out as the root lunges 2.6 px and
  the body leans in; blade within about 1 degree of forward at impact (c763c337).
- The caster cast: staff swept back on the wind-up, driven forward with the free palm on the impact tick, face clear in
  front, 3/4 and side (4fea3d9b).
- Hard gates in the build (missing clip, impact off tick, attack too long, death not grounded, loop not closing)
  stopped whole classes of regressions. Add a gate whenever a reviewer finds the same mistake twice.
- An aggressive, honest, read-only review with file and key references found what every passing check missed.
