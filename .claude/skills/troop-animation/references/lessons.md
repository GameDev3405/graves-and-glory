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
- **Offline renders cannot show a layering fault** (the spin fault, 2026-11-13: a hood, cape or pauldron spinning in
  play while every rendered frame looked right). render_geo plays one clip on a fresh model; in the game the stance,
  attack, hit and death layers add onto bones the model shares between every troop drawn with it. A bone the playing
  gait clips do not key kept the last draw's value, so a hit clip's offset piled up each frame. Fixed in
  `GaitController.rest` (bones the gait did not write start each frame at rest). The harness row `-layer-rest` guards
  it: it leaves an impossible value on every bone after each draw and fails if any drawn bone still carries it. So a
  new clip that keys a bone no gait clip keys (soft-motion parts: cape, hood, skirt, pauldron, braid) is only proven
  by an in-game reel, never by renders alone, and "something spins or drifts in play only" means look at the layers,
  not the clip.
- **Code that reads a bone reads the last draw of the model, not this troop** (2026-11-14). A controller's clip pose
  goes onto the bones after its `process` returns, so inside `process` the bones still hold what the previous draw
  left: in a battle, another troop. `GaitController.matchPhase` picked a troop's first stride from that, so troops
  set off on a phase taken from someone else's legs. Anything that must know what this troop held reads a pose
  the controller kept for it, never the bones: `wrote` (the gait layer, taken in `after()`) or `shown` (the legs
  as drawn under every layer, taken by `GaitController.Shown`, registered last). The harness row `-start-own-legs`
  guards it: it leaves a walk pose half a stride off on the leg bones after each draw, keeps each troop's legs as
  last drawn, and fails if a troop that sets off does not start on the phase its own drawn legs match.

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
- **Nothing that hangs stands still in an action.** A cape lag alone is not enough; plumes, plates, skirts, quivers
  and trophies all get `fullbody.soft_motion`. (Not the helm's crest: see 7e.) This is the cheapest gain there is: one call before baking.
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

## 7d. From Kort's grades of round 2 and the two harder rigs (2026-11-11: knight 8, archer 8, tusk no score)

- **A fix for a reviewer can be a fault for Kort.** The pauldron was detached from its arm to shave a shallow clip
  number; he read a dislocated shoulder at once. Armour, straps, wraps and guards stay on the limb they are strapped
  to. Trail it a few degrees, slide it along the limb, never leave it behind.
- **His "ok" ticks are a message too.** On the archer every tick he left "ok" without a note was a stepping tick,
  and his overall note said "legs look abnormal". Lay the unnoted grades over the phases before deciding what a
  general note means.
- **Look at how a real person holds the thing.** String in the arm, squatting archer, fists a hand apart on a
  spear: none was a number, all three were visible in one honest look at the guard frame. Grade tick 0 yourself,
  hard, before any motion: he does.
- **One hold for one weapon through the clip.** If the guard holds the weapon one way and the action another, the
  change between them is where the clipping and the pops live. Make the guard's hold the action's hold.
- **Find the limiting joint before buying reach.** "Poke out a bit more" got 0.5 px from 1.1 px of body travel
  until the straight arm was measured (the front one, not the rear one that was being pushed).
- **"A tad" means a tad.** He asks for small changes in small words. +1.3 px of reach and +2 px of grip, with the
  parts he graded good left alone, not a new clip.
- **Timing notes are design questions.** Clip length and the impact tick are the design sheet's. Explain what the
  game does with them and ask; do not move them to answer a look.
- **The leg solver's error is tiny when the cause is big.** "ankle unreachable (err 0.07 px)" on a foot that has not
  moved means the pelvis left it behind: lift that foot earlier or keep the pelvis back, do not hunt the step that
  was just edited.

## 7e. From Kort's grades of knight round 3, archer round 2 and tusk round 2 (2026-11-11)

"knight v3 looks good", "archer v2 looks good", and three notes.

- **He can take back something he asked for.** Round 1: the "red fur on the helmet" was stale. Round 3: "remove
  the wobble of the red strip on his head, i thought i wanted it but i don't." Record both, date both, and change
  the kit (not the clip) so no other troop repeats it. The rule that survives: things that hang move, things fixed
  to a rigid piece do not.
- **A joint the checks do not look at is still a joint he looks at.** No row measures the ankle. The archer's rear
  foot was flat on the ground with its shin 50 degrees off it, and he called the crease. Print the foot's local
  rotation on every stance where the feet turn, and keep each angle under about 30 degrees.
- **A shared helper carries its assumptions.** `K.attack_legs` was written for a knight who faces his foe: knees to
  the front. Side-on stances need the thigh to follow the foot. Read what a borrowed helper pins before trusting it
  on a new stance; wrap it in the demo, report it to its owner, do not edit it.
- **"Less body, more arms" is a geometry problem, not a slider.** Shortening the steps takes the reach away px for
  px unless the arms find it, and they find it only where the chest lets them. Measure shoulder to wrist for both
  arms, find what each px is spent on (crossing the chest, a folded elbow), and change that.
- **When a limb clips only by its elbow's choice, steer the elbow, not the fist.** Four attempts moved the fist and
  the chest and stayed at 1.97 to 2.0 px; one elbow prior cleared it with more reach than any of them.
- **Look at a stance's own number before judging a move.** The tusk's guard stands at limb 1.6 / armour 1.8, so a
  drive at 1.87 adds 0.07, and one at 2.0 adds 0.2. Print the per-tick series, not only the worst row.
- **Re-time through one map** (the archer's 16 to 22 ticks): every key, step and marker through the same
  piecewise-linear `w(t)`, exact times, then re-measure everything that has to touch.

## 8. Legacy clip pipeline (for old-model fixes only)

- 18 of 30 troops once had no attack clip and every check passed: a missing clip must fail the build, never be shot
  or graded as a stand-in (`clips.py` now refuses it).
- Backwards elbows: forearm rx was positive in every hand-keyed sword and bow clip. Re-key the whole chain together;
  never let the wrist cover for a broken elbow.
- Catmull-rom through the top of a swing never stops the arm; hold the top 150-250 ms with linear or flat keys.
- Clip timing changed Siege attacker wins from 4/6 to 0/6 (`docs/reports/mode-gate-timing-2026-11-04.md`).
