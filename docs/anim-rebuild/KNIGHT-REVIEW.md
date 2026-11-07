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
