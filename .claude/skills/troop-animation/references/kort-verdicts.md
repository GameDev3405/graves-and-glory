# What Kort approved and what he rejected

Every line is something Kort said or decided, with its date. This is the taste the checks cannot measure. Read it
before authoring a clip, and add to it after every grading round (`references/frame-grading.md`). Quote him; do not
paraphrase a verdict into something stronger than he said.

## His four standing complaints (2026-11-11)

He named these as the points every animation session must work on: **clipping**, **stale or stagnant motion**,
**blades and weapons facing the wrong way**, and **arms and limbs glitching or going the wrong direction**. Each one
has a history below and a mechanical check in `references/four-focus-points.md`.

### Clipping
- 2026-11-06, the first knight deaths: "the old falls looked glitchy, with the sword and shield clipping."
- 2026-11-07, death weapons: the gear "glitches around a lot and ends up in places that don't make sense." Fixed by the
  v4 death system (gear lands beside the hand that let go of it, on that hand's own side). Approved: "the animations
  are looking great."
- 2026-11-07: a dropped sword "fell too far from the hand and ended up on the shield's side."
- 2026-11-08: knee and elbow z-fighting fix approved.

### Stale, and its opposite
- 2026-11-05: the shield was "stuck to the forearm and looked stagnant"; he wanted it "to move with the unit." After
  the shield was made to ride the body: "Looking good."
- 2026-11-07, the other side of the same dial: "way too much sway", "heavy hits break their back", and for the ready
  stance "bring it down to like 25%". He chose variant B (scales 0.4 / 0.35 / 0.25). **Alive is not the same as
  swaying.** A clip is alive when every part has a reason to move; it is over-animated when the spine bends further
  than a person in armour would.
- 2026-11-10, the first stalker: "is no good… everything feels off." A clip can pass every number and still be
  rejected whole. The redo that was kept started from an open brief (what would this unit do with this weapon), not
  from patching the rejected clips.

### Weapon facing
- 2026-11-03, the standing rule: "The weapon must always face the way the move logically needs. This mistake keeps
  coming back, so check it on every attack before showing it."
- 2026-11-08: "late in the slash the blade turned flat against the target." The edge-roll fix was approved. The edge
  leads on every fast frame of an arc, not only at the impact.
- The mage's "staff was held at the bottom like a wand": a weapon is gripped where that weapon is gripped.

### Limbs
- 2026-11-04: elbows and knees signed off after every hand-keyed clip was found to bend the forearm backward.
- Reviewer rounds since then removed euler-branch spins and one-frame pops; he has not had to report one since. Keep
  it that way: these are caught by `focus_check.py` (`pop`, `jitter`, `elbow`, `knee`).

## What he approved (the templates)

- **Knight v2** (2026-11-06/07) is the template for every troop: guard as a ready pose, idle breathing inside it,
  strikes driven from the pelvis with a step, graded hits, v4 deaths.
- **The combat reel** in `docs/anim-rebuild/STYLE.md` is his 10/10 reference for style.
- **Every clip must suit that unit and its weapon** (2026-11-10). No filling a slot with another family's clip.
- **`hit_pulled` is reviewed in every batch** (2026-11-11).

## How he reviews

- On his phone. Published files and `/_blob/` assets do not load there: every picture on a page for him is a JPEG
  data URI inside the HTML.
- Frame by frame. He grades each tick and says what is good and what is bad, so that the next clip keeps the first
  and stops the second (`references/frame-grading.md`).
- From 16 blocks as well as near: a pose that only reads in close-up does not read in a battle.

## Grading rounds

Add one block per round, newest first: date, clip, his overall score, then one line per tick he marked, in his words.

### 2026-11-11, knight `attack_overhead` (round `knight-attack_overhead-r1`)
Built as this skill's demo with the full-body kit. Grading page: https://claude.ai/artifact/LVqUBq4SUWYHH5iVrrsmLS
(grades under `rounds/knight-attack_overhead-r1`). Awaiting his grades.
