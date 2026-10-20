# UI / HUD grading rubric (Kort, 2026-10-17)

Every mockup is graded out of 10 by the controller. Below 8 goes back to work.
Each line is 1 point; half points allowed. Score each screen, then average per direction.

1. Legibility: no clipped words, no overlapping glyphs or text, nothing covered by another element; every label readable at GUI scale 2 on 1080p.
2. Information budget: only what the player needs at that moment is visible; secondary facts live on hover/select or one click away; no fact shown twice.
3. Focal point: the eye lands on one thing first (reveal card, fight choice, the standards); everything else is quieter.
4. Hierarchy: at most three type sizes per screen; weight and colour carry the hierarchy, not extra boxes or borders.
5. Spacing and alignment: consistent grid, consistent padding, nothing within 12 px of the screen edge, columns and rows line up.
6. Colour discipline: House/team colour as accent only; text contrast 4.5:1 or better everywhere; colour-blind-safe states (rout, contested, flash) carry a shape or word, not colour alone.
7. Readable drama (HUD): a rout, a loss flash, a contested point and an ability on cooldown each read in under a second without reading text.
8. Feel: it looks like this game (grim but heroic, hand-authored pixel art, charcoal/ivory/brass or a palette argued for), not a generic fantasy UI or an AI-slop collage.
9. Honesty: pixel-authored at scale-2, real labels and numbers, real screenshot backgrounds; nothing that cannot be built with 9-slice textures, sprite frames and Minecraft text.
10. Flow: the player can tell what to do next and how to get back (one primary action, key hints only when useful); screens of one direction feel like one family.

A 10/10 reference set is kept in `reference/` with a note on why each is there.
