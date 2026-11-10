# What works and what to avoid

Sources: `docs/anim-rebuild/PLAN.md` (review log), `KNIGHT-REVIEW.md`, the per-troop `*-REVIEW.md` files, and the
legacy clip history (2026-11-03/04). Each lesson is the failure, then the fix that worked. When a review finds the
same mistake twice, add it here and, if it can be measured, ask for a scorecard metric.

## 1. Weapon facing (Kort: "this mistake keeps coming back")

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
- Wind-up held long enough to read; contact fast; follow-through overshoots and ends low; settle eases to the exact
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

## 8. Legacy clip pipeline (for old-model fixes only)

- 18 of 30 troops once had no attack clip and every check passed: a missing clip must fail the build, never be shot
  or graded as a stand-in (`clips.py` now refuses it).
- Backwards elbows: forearm rx was positive in every hand-keyed sword and bow clip. Re-key the whole chain together;
  never let the wrist cover for a broken elbow.
- Catmull-rom through the top of a swing never stops the arm; hold the top 150-250 ms with linear or flat keys.
- Clip timing changed Siege attacker wins from 4/6 to 0/6 (`docs/reports/mode-gate-timing-2026-11-04.md`).
