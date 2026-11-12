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

### 2026-11-11, Warmaw tusk `attack_thrust` (round `wm_tusk-attack_thrust-r1`): no score
Page: https://claude.ai/artifact/HgtYPfwf7mAbctsXDucheX. Overall, his words: "stance and body movement look good. the
hand placement and lunge could do better. have the unit poke out the spear a tad bit more, not a lot just a little
bit."

| Tick | Grade | His note |
|---|---|---|
| 0 | ok | "hand placement could be spaced out a tad bit more." |
| 1, 2 | ok | |
| 3-21 | good | |

**Keep doing**: the stance, the sit-back load, the body driving the thrust, the carry past the impact.

| What he saw | Cause | Rule from now on |
|---|---|---|
| Hands too close on the shaft in the guard | The spear builder's guard holds the fists 6.0 px apart | Guard grip 8.0 on a two-hand spear (about a third of the shaft). The stance clips must match. |
| The spear does not poke out far enough | The front arm was already straight; the body stopped 1.1 px short of what the legs allow | Measure which arm is straight, then buy reach with the body, the front shoulder and the rear fist driving through the front hand (`fullbody-kit.md`, rule 7). +1.3 px. "A tad" is 1 to 1.5 px, not 4. |

### 2026-11-11, archer `attack_direct` (round `archer-attack_direct-r1`): score 8
Page: https://claude.ai/artifact/C5779ZnWs9G8uPQ5dj8caH. Overall, his words: "legs look abnormal. shot seems quick.
the bows string is inside the archers arm to start. the head shake while the unit is pulling bow is nice touch."

| Tick | Grade | His note |
|---|---|---|
| 0, 1 | ok | "string inside archer arm" |
| 2, 3, 5, 6 | ok | (the stepping ticks) |
| 4 | good | "arm bend and reach back for arrow good" |
| 7, 8 | good | "arm placing arrow in string good." |
| 9-14 | good | |
| 15 | good | "sholder and elbow on full bow pull look good." |
| 16-19 | good | "release looks good" |
| 20 | good | |
| 21 | ok | "string in arm" |
| 22-25 | ok | (the stepping ticks) |

**Keep doing**: the head settling during the draw; the reach for the arrow; the nock; the full draw; the release.

| What he saw | Cause | Rule from now on |
|---|---|---|
| String inside the bow arm in the guard (ticks 0, 1, 21) | The lead's guard wrist lays the bow along the forearm; the resting string is 3.8 px behind the grip | A bow is held across the forearm, and the guard's hold is the aim's hold. Measured on every resting-string tick. |
| "legs look abnormal" (every stepping tick only "ok") | Pelvis sunk 3 px over a 4 px stance: knees near 90 degrees; both feet in the air together on the way home | Archers stand tall on a long stance (knees about 50 degrees); one foot in the air at a time. |
| "shot seems quick" | 16 ticks to the loose is the design sheet's number; the game adds a 0 to 3 tick hold | Gameplay timing: asked, not changed in a clip. Asked the same day (longer shot, reorder the clip inside 16 ticks, or leave it), he chose **"Make the shot longer"**. That is a rate-of-fire change (`sheets/design.json` `bow_direct` `impactTicks`, the bow builder's `ATTACK_TICKS` / `FULL_AT` / `IMPACT`): it waits for the coordinator's approval and a number. |

### 2026-11-11, knight `attack_overhead` (round `knight-attack_overhead-r2`): score 8
Overall, his words: "sholder kind of twists out of place in a couple frames. ive labled them below". Tick 10 bad:
"why is wrist/arm above shoulder? ". Ticks 9 and 11 ok, every other tick good.

| What he saw | Cause | Rule from now on | Check |
|---|---|---|---|
| The shoulder "twists out of place"; the arm above the shoulder (tick 10) | Round 2 let the pauldron follow only 30% of its arm (to keep it out of the helm for a reviewer's 1.88 px figure), so with the arm overhead the plate stayed down where the shoulder had been | **Armour never leaves its limb.** A plate trails its arm by at most 10 degrees and may slide down its own arm, nothing else. Do not detach a piece to win a clip number on ticks he graded good. | `test_fullbody.py` (the plate stays within `PLATE_LAG`) |

Everything round 1 complained of was graded good in round 2: the grip, the moving cape and crest, the chest plates.

### 2026-11-11, knight `attack_overhead` (round `knight-attack_overhead-r1`): score 7
Built as this skill's first demo with the full-body kit. Grading page:
https://claude.ai/artifact/LVqUBq4SUWYHH5iVrrsmLS (grades saved in
`work/grade/knight-attack_overhead-r1/grades/`).

Overall, his words: "i like that the legs flow into the attack, i like that the arm is in full motion. i do not like
that the sword switches angels mid wind up, and i also don't like how stale the cape, and his red fur on the helmet,
i want the whole unit to be animated including pieces of its armor that way it feels alive."

| Tick | Grade | Tags | His note |
|---|---|---|---|
| 0-4 | good | | |
| 5 | bad | weapon facing | "weapons spins around in hand" |
| 6 | ok | weapon facing | "still spun around" |
| 7, 8 | ok | | "the textures on its chest are glitching into each other." |
| 9, 10 | good | | |
| 11 | bad | limb wrong, clipping | |
| 12, 13 | ok | | |
| 14 | ok | limb wrong, clipping | |
| 15 | good | | |
| 16 | not graded | | |
| 17 | good | pose | |
| 18-21 | good | | |

**Keep doing** (he said so): legs that flow into the attack; an arm in full motion.

**Stop doing**, and what now stops it:

| What he saw | Cause | Rule from now on | Check |
|---|---|---|---|
| The sword turns in the hand during the wind-up (ticks 5, 6) | The kit rolled the hand to put the edge on the tip's path whenever the tip was fast, the wind-up included | **The grip only turns inside the strike** (3 ticks before the impact to 2 after). In a wind-up the wrist carries the blade; it never spins about its own length. | `grip_roll` |
| Cape and the red crest stand still | Only the cape had a lag, scaled small; crest, plates and skirts had no keys | **Every piece that hangs or sits on the body moves**: cloth, crest, plume, skirt plates, shoulder plates, hung trophies. `fullbody.soft_motion` does it for any rig. | `secondary_life` |
| Chest textures "glitching into each other" (ticks 7, 8) | The shield arm's plates sat inside the chest plates by under 2 px: too shallow for `limb_in_body`, but two textures in one place flicker | **Armour on one bone never sits inside armour on another**, even shallowly. | `armour_clip` |
| Limb wrong and clipping (ticks 11, 14) | The shield arm folded across the chest in the cut and the follow-through; the pauldron rode the arm into the helm | Keep the shield fist outside the chest's width while the body turns. (Round 2 also let the plate follow only part of the arm; he rejected that, see round 2 above: the plate stays on its arm.) | `armour_clip`, `limb_in_body` |

Round 2 of the same clip carries these fixes (focus check: 0 FAIL, 2 WARN). He also asked that the next test be on
harder units: "work on a unit that isnt so easy, like a archer and a wildenmoot unit". Those are
`art/anim/archer/build_direct_demo.py` (the shot) and `art/anim/wm_tusk/build_thrust_demo.py` (a spear in two
hands); see `fullbody-kit.md`, "Other rigs".
