# Knight review record (2026-11-06)

Pilot unit for the GeckoLib rebuild. It was judged against STYLE.md by a screenshot reviewer, over two rounds.

- **Round 1: FIX FIRST.** Four problems:
  - Hits rocked only the upper body.
  - The block was too subtle.
  - The idle was flat.
  - The overhead load was arms only.
- **Fixes:**
  - Hit and block footwork clips (`hit_*_legs`, `hit_block_legs`), applied by HitController at stance-still weight, with positions.
  - A bigger throw-back.
  - Block phases: guard, absorb, shove.
  - Idle breath doubled, plus a weight shift.
  - The overhead load sits back on the rear foot.
- **Round 2: SHIP WITH MINORS.** All four are resolved. No new blockers.
- **In-game checks:** the geo-knight suite, including hit footwork weight per blow.

## Open minors (polish, not blocking)

- **Overhead:**
  - Hold the load longer, so 0.30-0.45 s reads as one clear pose.
  - The sword may brush the shield on the low follow-through (0.90 s, three-quarter view).
- **Block:** lean the torso into the shove instead of still leaning back at 0.60 s.
- **Idle:** the weight shift barely reads in the front and three-quarter views. The shield and sword could respond more.
- **Footwork:** the in-game block reads smaller than offline. The hit skid reads as lean and knee bend more than as visible foot travel.
- **To verify:**
  - A thin dark vertical line at overhead 0.75 s, side view (probably the cape).
  - A green streak beside the knight in the in-game medium-back hit, t2-t5.
  - Cape and plume lag (rule 9) is hard to judge at review scale.

## Shared changes from the sentinel review (2026-11-06)

The sentinel shares this builder, so its fix rounds changed the knight too:

- **Idle:** a bigger weight cycle with a lead-knee flex, hip drop, lateral hip shift and arms trailing the torso. This addresses the idle minor above.
- **Block shove:** less head pitch, with the shield arm further out.

The knight was re-exported with both. See SENTINEL-REVIEW.md for what remains open.

## v2 style (approved by Kort, 2026-11-06)

Kort approved the v2 style after watching an old-vs-new video, and it is now the default in `art/anim/knight/build_anims.py` (`GG_V2=0` builds the old style).

- **Style:** each blow is driven from the pelvis, with the sword on its own lane outside the shield rim. Hits are graded (light flinch, medium shift, heavy stagger), deaths buckle the knees first, and the ready, brace and hold stances read apart at gameplay scale.
- **Hit fix:** a front hit knocks the sword arm out wide to the right and it is back on guard in about 0.25 s. The shield is untouched, and the fold-in is removed from the other hits.
- **Harness:** `geo-knight` check on the knight passed with 22 `[p1] p1-` lines and 0 `=false`.
- **Other troops:** sentinel, gl_marshal and the other users of this builder are not re-exported yet; they pick up v2 on their next rebuild.

## Deaths v3 (2026-11-06)

Kort: the old falls looked glitchy, with the sword and shield clipping. The four deaths are rebuilt as twelve clips, gated to `TROOP == "knight" and V2` (`d3_death_clips`). Other troops build byte-identical output.

- **Clips:** `death_back|forward|left|right`, each with `_drop1` (the sword is let go) and `_drop2` (sword and shield). 20 ticks each, holding the last frame.
- **Body:** eased per-bone tracks, grounded on the body's own corners (no gear propping, no slide, no rebound). The wrists slerp, and their euler angles are unwrapped.
- **Held gear:** a per-sample shoulder plus wrist correction (`d3_clear`, a pattern search, gaussian-smoothed) keeps the blade and shield off the floor and out of the body and the other piece.
- **Dropped gear:** baked into the gear bones' own keys, with no rig change.
  - It leaves with the hand's velocity and spin, capped at 3.5 px/tick sideways (a dead hand lets go, it does not throw). It then falls under gravity to where it would land, moved out from under the body.
  - If the straight flight would cross the body, the path bows out (`off*sin(pi*u)`) just enough to clear it. If no bow clears it, the landing moves 3 px further out and the search runs again.
  - The landing spot is checked against the body from just before touch until the end of the clip.
  - It lands on one end, tips flat, gives one small clatter and lies still before the clip ends.
- **Probes** (px = 1/16 block):
  - Floor: the worst is -0.42 px (kept sword, death_left). Dropped gear never goes below 0.
  - Gear in body: the worst is 0.77 px (kept shield into left_pauldron, death_right). Dropped gear reaches 0.45 px at most, when the sword grip slides out at release. Sword into shield reaches 0.49 px at most.
  - Rotation rate: the largest local rate is 69.8 deg/tick (forward_drop2 shield, in flight). GeckoLib's linear euler interpolation between the 0.25-tick keys stays within 1.6 deg of the true slerp.
  - Settling: dropped gear is still by tick 11.8-19.0.
  - Other clips: gl_marshal and archer build byte-identical. The knight's non-death clips are identical to HEAD.
- **Review:** one independent Sonnet review, verdict SHIP-WITH-MINORS. These minors are open:
  1. right_drop2: the shield comes to rest partly on the dropped sword, so the two read as a pile.
  2. death_left: the kept sword hangs straight down beside the shield, like a planted stake. This is the stiffest of the resting poses.
  3. Early releases (ticks 5-6): the gear can reach the floor while the knight is still mostly upright.
  4. back clips: the kept arm keeps easing until about tick 18, and the dropped gear makes a small settle at about tick 19. It reads as a soft settle, not a rebound.
- **Java (not done here):** in DeathController.process, after `latched = pick(...)`, take a stable per-entity suffix, e.g. `floorMod(mix(uuid.hashCode()), 3)` into `{"", "_drop1", "_drop2"}`. Then try `model.getAnimation(animatable, latched + suffix)` and fall back to `latched` when it is null. The gear bones need no Java change.

## Deaths v4 (2026-11-06)

Kort, on the v1 death reel: the dropped sword fell too far from the hand and ended up on the shield's side of the body. He asked for a longer grip and a drop on the side it was held. This happened in several clips. Only the eight drop variants and the kept death_forward arm change. gl_marshal and archer build byte-identical.

- **Landing spot (`_d3_beside`):** the rest spot is searched beside the releasing hand, on the hand's side of the hips-to-head midline. Steps go out from the hand and along the body, nearest first (`D3_BESIDE_STEPS`), with a 1 px gap. The body's final frame is checked first because it is cheap, then the zone from touch to the end. The cost is distance from the hand plus 0.06 per degree of turn.
- **Grip:** releases are later, with the hand near the floor.

  | Variant | Sword release tick | Sword landing distance from the hand (px) | Shield release tick | Shield landing distance from the hand (px) |
  |---|---|---|---|---|
  | back | 13.0 | 7.7 | 14.0 | 8.8 |
  | forward | 12.0 | 7.4 | 12.25 | 13.9 |
  | left | 12.75 | 14.9-15.1 | 13.0 | 8.7 |
  | right | 11.75 | 8.3 | 13.5 | 11.7 |

  The release ticks are on the 20-tick clip. Distances are horizontal, in px.
- **death_forward:** the right-hand rest is now `(-60, 15, -60)`, blade out along his right, so the drop1/2 sword no longer crosses under him. The smallest margin to the midline is 11.2 px.
- **Probes:** dropped gear never goes below the floor (min y 0). No dropped piece moves between ticks 18 and 20, and every piece is still by tick 14.4-17.9. The sword lands on the hand's side in every clip. Flight briefly grazes the midline by 0.5 px (right sword, left_drop2 shield).
- **Review:** one independent Sonnet review of the v2 reel (dense ticks 10-20 sheets plus close-ups) gave APPROVE with no blockers. These minors are open:
  1. left_drop2 and right_drop2: the sword hilt sits against the shield rim. They touch but do not overlap.
  2. left_drop1/2: the right hand is the upper arm when he falls left, and it cannot get within about 10 px of the floor without entering the body. The sword therefore lands about 15 px out, in front of the belly and past the kept shield. It is on his right, but Kort should confirm that it reads as "same side".
  3. forward_drop2: after the shield lands flat, it shifts outward about half its width (a clatter, not a pop).
  4. The v3 minor about the death_left kept sword hanging like a stake was not addressed in this pass. In the back clips, the dropped gear is now still by tick 17.5 (it was about 19), but the kept arm's slow easing was not changed.

## Approval (2026-11-07)

Kort approved the v4 deaths ("the animations are looking great"), including the left-fall sword landing about 15 px out on his right side. These next steps are approved:

1. In game, each troop picks one of `""`, `_drop1` or `_drop2` (a stable pick per entity). A troop without drop clips falls back to the plain direction clip.
2. The 12 deaths are copied, each with its own gear, to sentinel, gl_marshal and ic_hearthguard.

The Knight's set (v2 style plus the v4 deaths) is the template for the other troops. Which troops reuse which parts is being worked out with Kort.

# Batch review 2026-11-11: hit_pulled

Branch `anim/review-hit-pulled` (cut from `anim/hit-pulled`, which added the booth configs). Only `hit_pulled` and
`hit_pulled_legs` changed; every other knight clip is byte-identical (checked clip by clip against HEAD).

**Why it changed.** `hit_pulled` is new (built by `art/anim/common/reactions.py`) and was never approved. The focus check
plays additive hits over the bind pose, so it hid the clip's real faults; composed over the ready stance the way
HitController adds it (`work/review/sen_compose.py`), the clip had:

| As played | Before | After |
|---|---|---|
| k 1.0 | FAIL sword_t3 into helm_great 1.73 px (t3.5); right toe 2.51 px under the ground (t1, exempted); cape, pauldrons, skirt plates still | WARN right toe 0.76 px (t0.5); WARN secondary_life 3.93 (cape) |
| k 1.55 (heavy) | FAIL sword into helm 3.54, right_cuff into helm 2.03, helm into cuff 2.03, knee_aim 89.8 | WARN floor 0.87, hips into right_skirt_gilt 1.76, knee_aim 54.1 |

The blade crossed the visor through the fling (side and front frames t2-t6 of the before sheet).

**What changed.** `art/anim/knight/soft_layers.py` (the knight family's late layer, run after reactions.py and export; it
also carries the sentinel's layer, cherry-picked from `anim/review-sentinel`) has a `knight` entry with `hit_pulled` only:
the sword arm swings wider of the helm through the fling (arm z -16, x +6 at full fling), the body is lifted 2.6 px off
its dragged feet (2 px more on the catching steps) and the toes point 10 degrees less; then the cape, skirt plates and
pauldrons swing (fullbody.soft_motion, cape gain 2.5). Length 0.7 s, no timeline markers, unchanged.

**Raw focus check (bind pose)** after: 0 FAIL on the whole knight (41 clips). hit_pulled: the floor row passes (its
exemption is no longer needed), secondary_life 3.93 WARN (no longer needs its exemption), elbow 162.8 still exempt (bind-pose
artefact: composed it passes). New WARN: sword_t3 into hips 1.41 px at t4.25, only over the bind pose (composed: pass).

**Open.** `test_scorecard.KnightTest.test_shipped_limits_match_derivation` fails: the shipped limits are derived from the
knight, and the new hit_pulled lowers the knight's hit foot slide (0.4928 -> 0.4818), so the hit foot_slide limit would
derive to 0.5318 (shipped 0.5428). `tools/anim/scorecard_limits.json` is outside this task; the knight still passes the
shipped limits. The exemption rows for knight hit_pulled floor and secondary_life can be dropped.

**Sentinel's hit_pulled.** The sentinel's hit_pulled booth batch (`art/anim/knight/booth.sentinel.hit_pulled.json` on this
branch) is superseded by `anim/review-sentinel`, which reworks the sentinel's hit_pulled with the same layer (and its
stun, knockback, attacks and hits). Review the sentinel's clip there, not here.

**Tests.**
- In-game reel (geo-knight through checkq, root `knight-review2`, job 20261112-014136-321-0a83): passed, 22 `[p1] p1-`
  checks true plus the suite line, 0 `=false`; `p1-geo-knight-hit-pulled=true` (3 pulls by the server path). The first
  run (`knight-review1`) hit the 30-minute wait limit at 19 checks with 0 `=false` and was rerun.
- `python -m unittest discover -s tools/anim`: 152 tests, 1 failure, `test_shipped_limits_match_derivation` (above).
- `python tools/verify.py fast`: FAIL on machine load, twice: every step passed except python-tests, which hit verify's 600 s step limit (the machine ran several builds and clients). Run on its own (`tools/parallel_tests.py test-tools/standalone`, 12.5 min): 724 of 727 pass; the 3 failures are test_where's speed limits (3.66 s, 154.8 s and 3.02 s against 3, 30 and 3 s), load, not this change.
