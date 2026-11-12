# What works and what to avoid

Sources: `docs/anim-rebuild/PLAN.md` (review log), `KNIGHT-REVIEW.md`, the per-troop `*-REVIEW.md` files, and the
legacy clip history (2026-11-03/04). Each lesson is the failure, then the fix that worked. When a review finds the
same mistake twice, add it here and, if it can be measured, ask for a scorecard metric.

Lessons log: during a pass keep a dated `work/anim-lessons-YYYY-MM-DD.md` in your lane worktree (untracked): what
went wrong, how it was found (reviewer, check, Kort), the fix, the commit. At the end of the pass fold the durable
lessons into this file with their commit hashes.

## 1. Weapon facing (Kort: "this mistake keeps coming back")

- Measured on every quarter tick by `focus_check.py` (`edge_lead`, `point_lead`, `at_foe`, `roll_spin`, `wrist`);
  see `four-focus-points.md`. The full-body kit rolls the edge onto the tip's path by itself (`fullbody-kit.md`).

- Check the weapon on every frame, not only contact, in front, side and three-quarter views. A clean pose with the
  blade wrong is wrong.
- Aim numerically, not by eye: print the blade direction and tip position at wind-up, contact and follow-through,
  and the lowest tip over the clip. Long blades drove tips into the floor on follow-throughs (gl_marshal); the fix
  was a flatter blade on those keys (`FLAT`), checked by probe (tip height above the soles).
- Edge lead: on a slash the edge must lead the tip's travel (scorecard `edge_lead`). A reap moving across its blade
  scored 0.1 and read wrong.
- Take the arc with shoulder and elbow; a wrist that spins through a strike makes in-betweens point sideways.
- Two-handed and solved weapons (spear, bow, crossbow): solve both hands to the weapon each sample. Hand-tuned
  off-hand values never converged. Read grip offsets from the rig pivots, not per-troop constants.
- Bows: keep the bow on the aim line with limbs upright and the string hand on the nock; solve the full-draw key so
  the nock lands on the cheek anchor (archer).

## 2. Weight and timing

- Every blow is driven from the pelvis (v2 style): hips drop and turn, torso leans, head follows, arms finish. Arms
  alone read stiff (knight round 1: "overhead load was arms only").
- Wind-up held long enough to read (STYLE.md rule 5; the legacy rule of thumb was a 150-250 ms hold, and the knight
  review asked for the overhead load to read as one pose over 0.30-0.45 s); contact fast; follow-through overshoots and ends low; settle eases to the exact
  guard. A wind-up that passes vertical and comes back reads as a hitch: keep the rise monotone.
- Each attack has its own `_legs` clip (plant, step, hip drop at contact) for the standing case; walking keeps the
  gait legs.
- Impact marker is data-linked to `impactTicks` and checked in game; never move it to fix a look.
- Ranged shots are announced early (a bow draw 10 ticks ahead): AttackController quickens the wind-up and holds at
  the `full` marker for a seeded hold, so a line never looses in lockstep.

## 3. Hits, stances and idle

- Hits are graded: light flinch about half the medium's first key, medium stagger, heavy = medium x1.55; hit and
  block footwork clips (`hit_*_legs`) so the whole body reacts. A front hit throws the sword arm out wide, back on
  guard in about 0.25 s.
- Stances must read apart at 16 blocks (wall vs ward, brace vs ready). Check the far view, not only near.
- Idle: double breath plus a weight cycle (lead-knee flex, hip drop, lateral shift, arms trailing the torso). Too
  little idle reads as a mannequin.
- Shields stay upright in the fist and ride the body: rock against the stride, settle a beat after the breath, get
  tossed by the swing. A shield glued to the forearm reads dead.

## 4. Deaths

- Use the shared v4 system (`art/anim/common/deaths.py`): eased body, grounded on its own corners, no gear propping,
  no slide, no rebound; drop variants land beside the releasing hand on its own side. Configure gear per troop;
  do not copy-paste the knight's falls.
- Probe floor and gear-in-body numbers (min y, worst penetration) and record them in the review md.
- Ranged and two-handed troops need their own gear solve on the ground (archer `flat_bow`).

## 5. Gait

- Foot slide is measured, not judged from one frame (`render_geo.py --report`, builder `--check`). Planted feet come
  from the IK solve; do not key legs by hand.
- A walk that looks like a jog: trailing shin folds too far. A locked look: sub-texel hip motion. Anything under one
  texel is invisible at our scale.
- Gait bands scale with each troop's speed (Gait): a fast troop must not run in place at walk speed.

## 6. Evidence and review

- The renderer is offline truth for poses; the harness is truth for timing, clip choice and blending. Both are needed
  before the booth.
- One camera relative to the troop for every key, plus side and three-quarter. A sheet that names the wrong clip, or
  a "blade above the head" that is really the opponent, wastes a round: confirm which troop and clip each frame is.
- "Not checked" is a required answer when the evidence cannot show something.
- A loaded machine fails speed and sample-count checks; rerun once and keep both logs before diagnosing.

## 7. Process

- Change the builder, rebuild, and prove the other troops on that builder stay byte-identical.
- One worker per builder at a time; a shared builder change re-exports every troop that uses it (sentinel fixes
  changed the knight).
- After two fix rounds, record the minors in the troop's review md and escalate blockers with evidence.
- Kort sees results through the review booth; no merge or landing before `booth.py decide` says so.

## 7b. From the full-body demo (knight overhead, 2026-11-11)

- Joint angles guessed by hand were the source of most clipping and wrong-way limbs. Saying where the hand, blade,
  pelvis and feet are in the world and solving the joints removed the class of bug (`fullbody-kit.md`).
- Blending two edge directions as vectors flips the hand 180 degrees when they oppose. Blend the roll as an angle
  about the blade and follow one branch through a swing.
- A hand target more than the arm's length from the shoulder is the usual cause of a snapped elbow. Print the
  distance every half tick (`fullbody.reach_report`) before solving.
- The mechanical checks did not separate the stalker Kort rejected from the one he kept. Numbers gate the four
  faults; they do not grade the move. Kort's frame grades do (`frame-grading.md`).

## 7c. From Kort's first frame grades and the two harder rigs (2026-11-11)

Kort scored the knight overhead demo 7. Reviewers had scored it 7 twice. What he caught that the checks and both
reviewers let through, and what the archer and the Warmaw tusk taught on top:

- **A clean check run is not a graded clip.** All three of his complaints (weapon turning in the hand, still cape and
  crest, chest plates flickering) were measurable, and none was measured. Each is now a check with a test
  (`grip_roll`, `secondary_life`, `armour_clip`). When he marks a tick, first ask what number would have shown it.
- **Edge-on-the-path belongs to the strike only.** Rolling the hand to keep the edge on the tip's line during the
  wind-up reads as the weapon spinning in the hand. The kit now limits it to the strike window.
- **Nothing on the unit stands still in an action.** A cape lag alone is not enough; crest, plates, skirts, quivers
  and trophies all get `fullbody.soft_motion`. This is the cheapest gain there is: one call before baking.
- **Shallow overlaps flicker.** Two textured plates 1.7 px inside each other z-fight even though no limb is
  "through" anything. Judge armour against armour, not only limbs against the body.
- **Look at the model before the motion.** The bow of every bow troop is exported with its limb cubes turned the
  wrong way (`troop_rig.py` copies a Blockbench turn into a Bedrock geo without changing its sign). Hours can go into
  "fixing" a clip around a broken model. One big still frame of the weapon first.
- **A stance's faults travel into every clip that starts from it.** The tusk's guard holds the left arm 1.8 px in the
  chest and the rear fist in the hip skull; 11 of its clips fail the scorecard's `pen_limb` for that reason alone.
  Baseline the stance first and report it to its owner.
- **Limits measured on the knight are not laws for other rigs.** They still found the real faults on the archer and
  the tusk, but each new rig needed one rule taught to the check (a second hand's grip, hinge neighbours of one limb,
  inherited stance clipping). Add the rule with a test; never raise the number.
- **Solve, do not key, anything that has to touch.** String hand to nock, fist to ear, two fists on a shaft. And ease
  the target, not the joint angles, when leaving a solved pose.
- **Signs differ between tracks** (hips z is back, steps z is forward, spear head keys are world-relative). The table
  is in `fullbody-kit.md`, "Other rigs". Getting one wrong costs a round.
- **Render tiered troops with `--rig`**, or every armour tier is drawn at once and reads as clipping that is not there.

- **Prove whose fault a flicker is before you move a key.** Round 2's reviewer blocked the knight on a red hatch on
  the leg cloth; the archer's and the tusk's reviewers saw the same family. It was the renderer: the two faces of a
  zero-depth cube tied in depth. Test: does it show at tick 0, and in the previous version? Then it is not the clip.
  Fixed in `render_geo.py` (`DEPTH_TIE`), with a test.
- **The check reports one worst pair per row.** The pauldron sat 1.88 px in the helm at tick 4.5 of round 2 while the
  row showed another pair with the same number, at another tick. When a reviewer names a place the row does not,
  list every pair there: `python tools/anim/fit_pairs.py --troop <troop> --clip <clip> --part pauldron`.
- **A hold needs life from three places**: the body (a breath: a quarter pixel up and down again into the move), the
  held things (the shield trails a little), and the hanging pieces (soft_motion's own sway). Any one alone still
  reads as a statue to someone grading tick by tick.
- **Do not "fix" a tick Kort graded good.** Round 2's reviewer wanted the blade tipped further down at the contact
  (tick 10). Kort had graded ticks 9 and 10 good in round 1. It was left as it was and noted on the page.

## 8. Legacy clip pipeline (for old-model fixes only)

- 18 of 30 troops once had no attack clip and every check passed: a missing clip must fail the build, never be shot
  or graded as a stand-in (`clips.py` now refuses it).
- Backwards elbows: forearm rx was positive in every hand-keyed sword and bow clip. Re-key the whole chain together;
  never let the wrist cover for a broken elbow.
- Catmull-rom through the top of a swing never stops the arm; hold the top 150-250 ms with linear or flat keys.
- Clip timing changed Siege attacker wins from 4/6 to 0/6 (`docs/reports/mode-gate-timing-2026-11-04.md`).
