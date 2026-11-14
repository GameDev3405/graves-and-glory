# Prop brief: Soldier's Tent (Large, Dusk) (`tent_soldier_large_dusk`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** Colour variant of `tent_soldier_large` (approved by Kort, 2026-11-13); the original stays unchanged. Geometry, collision and solid cells are identical; model.py is a thin wrapper that calls the source model with a `Palette`. Linen canvas with the Dusk team's purple (UI #a07cc2, ink #52376d) as two roof stripes, the eave skirt trim and the rolled door flap; lightly weathered (low mud hem, grimed lower wall, one patch).
- **Where it goes:** battle maps: officers' or command tents in army camps, Siege camps, Domination camp sites. Builders and map designers place it; the small tent stays for outposts.
- **Setting / House:** Dusk team camp
- **Footprint (cells, x east * y up * z south):** 9 x 6 x 9. Front faces north. Anchor cell (4, 0, 4).
- **Scale against a troop:** body 112 x 112 px (7 x 7 cells, the block tent's footprint), walls 43 px, ridge 91 px (the block tent is 5 blocks = 80 px); the door is 32 px wide and 38 px high, so a Knight (35 px) walks in.
- **Silhouette at 15 m:** a big pale canvas roof with a ridge pole, a dark door opening on the north end and guy ropes fanning to pegs on all sides.
- **Close-up story at 3 m:** the rolled flap tied with two ropes over the door; inside: groundsheet, bedrolls, a chest, a barrel and hay bales; seams and stitching in the canvas.
- **Materials and ramps (proplib):** linen D8CCA6, team ramp from the Dusk UI colour and ink (`art/props/tentlib.py`), hemp 8E7448, mud 6A5841, patch CDC3A2; the rest as the source.
- **Budget:** a big prop cut into 18 regions; the splitter multiplies long elements.

Budget override: elements 240, sheet 512
- **Collision:** full barrier cubes on the wall cells only (the perimeter of the 7 x 7 body, y 0..2), none on the doorway cells or the interior; the ropes and pegs are walk-through. The walls are 2 px canvas in 16 px cells, so the collision map warns about low fill: from outside the tent reads solid.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `tent_soldier` (the small tent, same style) and the compare fixture `compare/tent_soldier.nbt` (the block tent, 7 x 7 body, 5 blocks high); the Knight on the card for scale.
- **Done means:** the card passes every automatic check, the geometry, collision and solid cells equal `tent_soldier_large`, and a fresh Sonnet-high reviewer passes the card against this brief.
