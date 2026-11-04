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
  the in-betweens point sideways. Take the arc with shoulder and elbow; keep the wrist within about ±40 through a strike;
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

## 4. Joints and rig

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

- The walk looked like a jog: the trailing shin folded to about 90 degrees, sole to the camera, peaking early. Walk
  swing knee 20-25 degrees peaking at mid-swing; 50-60 for runs only; armoured Houses lower still.
- Hip motion was sub-texel (sway 0.35 px, roll 1.5 degrees, bob 0.9 px), so hips looked locked. Walk sway 0.8-1.0 px,
  bob 1.2-1.5 px with the drop at contact, hip roll 3-4 degrees toward the stance leg. Motion under one texel is
  invisible at our scale.
- Foot slide cannot be judged from one frame; trace each sole's world xz through stance (drift at most 0.25 px).

## 6. Review evidence

- A sheet that shows the walk from behind and the attacks from the front cannot compare silhouettes. One camera relative
  to the troop for every key, plus a side orthographic view for blade and joints.
- Floating frames (3-6 px above the shadow) came from clips that do not key root y; `MotionFeet.ground` only lifts.
- "No failure at sheet resolution" on 150-220 px tiles is low confidence; review attacks frame by frame at full size.
- The grade video once showed a different attack than named; it now fails on a mismatch (072bf2a1). Make every sheet
  name the clip it shows.
- The art QA practice: pass only what the evidence shows. "Not checked" is a valid, required answer.

## 7. Process

- Blockbench is not the game: no weapon, no shield board, no engine layers. Every approval came from in-game renders.
- `art/clips/build_clips.py` overwrites hand edits; it is retired. Use anim_edit.
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
