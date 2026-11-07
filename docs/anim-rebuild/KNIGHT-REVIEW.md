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
