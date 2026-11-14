# Prop brief: Trebuchet (`trebuchet`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** gameplay (gameplay object (cover, barricade, siege piece, objective): collision IS the design; any rule change needs Kort).
- **What it is, in one line:** a counterweight trebuchet on a four-wheeled bed, cocked to throw north: arm wound down to the winch at the back, iron-banded weight box raised high at the front, sling and stone laid forward in the trough.
- **Where it goes:** the Ironhold siege camp at Kingsfield (flagship piece of the camp), facing the walls; also any Siege map's attacker lines. Placed by builders and map designers; nothing spawns it.
- **Setting / House:** Ironhold siege camp, Kingsfield
- **Footprint (cells, x east * y up * z south):** 7 x 13 x 18. Front faces north. Anchor cell (3, 0, 8). (Rebuilt 2026-11-13 about 1.4x bigger and sturdier after the Ironhold placement, Kort's call: 12 px timbers, an X of braces inside each A, raking struts front and back, 8-spoke wheels of radius 17. CMD blocks renumbered 10-12 -> 140-149, since the bigger model has 60 region parts.)
- **Scale against a troop:** about 189 px (11.8 blocks), five to six troops tall at the raised arm and weight box, about three times a 4-block palisade; the bed rail is 24 px (knee to hip), the axle at 140 px is far out of reach, and the winch posts (62 px) stand above a troop, so a crew works the handspikes at chest height.
- **Silhouette at 15 m:** the A-frame with a long diagonal arm and a heavy box hanging at its high end, on a long low wheeled bed: unmistakably a trebuchet, and the box side says which way it throws.
- **Close-up story at 3 m:** hemp wound on the winch drum and pulled taut to the arm's hook; ballast stones heaped in the banded box; a stone waiting in the leather sling pouch in the trough.
- **Materials and ramps (proplib):** `wood_x|y|z` (legs, arm, beams, wheels, handspikes), `swood_*` (sills, bearings, posts, box rim, trough rails), `plank_x|z` (deck, box walls), `endgrain` (sill and post ends, arm head), `iron` (bands, shoes, axle, hubs, hangers, pin), `leather` (pouch). New ramps: hemp rope `make_ramp("A48A58", 0.18)` with a diagonal lay line; field stone for ballast and shot `make_ramp("8A8478", 0.20)` (the stone_hand stone).
- **Budget:** over the default, agreed for big props:
- Budget override: elements 400, sheet 1024
  (1638 cells, split into 60 region parts; 333 elements. Long beams (288 px sills and deck, 126 px legs, rakers up to 164 px) are cut at every region joint and do not pack into 512, so the sheet is 1024, the cap.)
- **Collision:** every cell of the ground layer (y=0, 7 x 18 = 126 cells) is solid: the wheeled bed (sills, plank deck, cross beams, wheels) fills each by 12-30%, so troops and arrows treat the bed as a low wall and path around it. Nothing above y=0 is solid: the A-frames, arm, box and winch stand open, so troops are never blocked by thin timber in mid-air.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** today dressing with a solid bed only. Proposal for Kort, no code: (1) a Siege objective the defenders must burn or the attackers must crew; (2) a crewed engine that throws a stone at the wall every N seconds while friendly troops stand at the winch. Kort picks none, any, or other.
- **References:** the forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light, `stake_barricade` for field carpentry; the Knight on the card for scale. No outside game's assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
