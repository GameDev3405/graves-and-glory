# Animation pass 1 handoff (2026-10-28, branch anim-pass-1, local only)

Scope: six Order troops (knight, archer, mage, sentinel, longbow, gl_marshal). House troops untouched (no model on any branch).
Client animation only: BipedClips, TroopTells OVERRIDES, TroopAnimator (one line: a forced house attack prefers the variant), test tools. No server, protocol, hit-timing or model JSON change; sword contact stays at 0.25 s.

## What landed
- Weapon sets: sword(plod) for knight, gl_marshal, sentinel (plod); staff() for mage. Each has idle, walk, run, attack/cast, hit, death, hold, brace/fall_back.
- Cut keeps the blade right of the head until level (wrist 75 at rest). Staff wrist 55 (top 25 degrees forward).
- Axe, spear, mace hooks (WEAPON_ATTACKS): no Order troop plays them.
- New p2-anim-clear (Phase2Checks.clearance, in the Turn step): weapon mesh vs every body cuboid, 0.5 px depth. GRAVES_PHASE2_CLEAR=1 runs it alone.
- bow() exists but is NOT wired. On archer and longbow it made the clipping worse (longbow face 3.2-3.8 px) and likely broke p2-turn-shots. Reverted after the last render; the revert is unrendered.

## Scores (threshold 9; weapon through the body caps below 9)
- Before (docs/reports/anim-pass-1/before): knight attack 6, mage cast 6, idle 7, hit 7, walk/run/death 8.
- After (docs/reports/anim-pass-1/after, render 2):
  - Sword troops: idle 8, walk 8, run 7, death 8.
  - Sword attack: reads as a cut at contact, but is capped at 8. The windup goes 2.9-3.2 px into the head or helm at 0.15 s.
  - Sword hit: capped at 8 (blade in the right arm, 0.55-0.93 px).
  - Mage: capped at 8 (staff in the right arm about 1.15 px, death 0.86 px). This may be partly the fist inside the arm box (grip skip is 3.5 px).
  - Archer: hood/body 1.7-2.2 px, capped at 8. Longbow: face/arm 2-3.7 px, capped at 8. Both are the vanilla carry, unchanged after the revert.
- No clip reached 9. The two-correction limit is used, so this is recorded, not claimed done.

## Not verified
- The bow revert is unrendered; p2-turn-shots has not been re-run.
- Sheets show one frame per state, not the arc.
- Charge vs retreat: run covers both on the client. Splitting them needs protocol data, which needs Kort.

## Next
- Hand-key the sword windup in Blender: an arc above and outside the helm.
- Hand-key the bow carry: read the bow frame axes first.
- Widen the staff grip skip, or move the grip out of the arm box.
- Re-render all six in one run and re-score.
