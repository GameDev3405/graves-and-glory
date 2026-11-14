# Prop brief: Commander's Pavilion (`tent_pavilion`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** the commander's tent at the heart of a war camp: an octagonal walk-in pavilion of striped cream and madder-red canvas under a stepped, slightly concave roof, with a scalloped valance, a centre pole with a gilt finial and a red pennant, an open door with both flaps tied back, and eight guy ropes pegged out.
- **Where it goes:** the middle of any army camp on a battle map (behind a side's spawn, inside a stake-barricade ring) and the camp scenes of the campaign. Builders and map designers place it; nothing in the game spawns it. Neutral cream and dark red for now; team-colour variants come later.
- **Setting / House:** any House's war camp
- **Footprint (cells, x east * y up * z south):** 9 x 8 x 9. Front faces north (the door). Anchor cell (4, 0, 4), which holds the centre pole.
- **Scale against a troop:** walls 40 px (2.5 blocks, a head over a troop), eave 44 px, crown at 100 px, finial tip 122 px: about 3.5 troops tall at the pole. The door is 24 px wide and 34 px high, so a troop or player walks in upright; inside is 5.5 blocks across.
- **Silhouette at 15 m:** a broad striped octagonal cone with a dagged valance skirt and a pennant on top: the biggest tent in the camp, unmistakably the commander's.
- **Close-up story at 3 m:** the door flaps folded back and tied with hemp; a red rug with a gilt border round the pole and the commander's iron-strapped chest against the back wall; the ropes running from under the valance to their pegs.
- **Materials and ramps (proplib):** new ramps `make_ramp("E0D2AC", 0.16)` (undyed canvas) and `make_ramp("8C2A25", 0.20)` (madder-red canvas), painted by a striped canvas painter (24 stripes by angle round the pole, so walls, valance and roof line up; a red crown at the top); `make_ramp("A8977A", 0.16)` hemp for ropes and ties; `wood_y` pole and door posts, `swood_y` pegs, `gilt` collar and finial, `plank_x`/`swood_x`/`iron` chest. The diagonal octagon faces are boxes turned 45 degrees about y.
- **Budget:** a flagship big prop (648 cells): the roof alone is 23 octagonal rings of 8 boxes at 2 px a step (so the stripes run straight) and a 7-box crown. 297 elements, sheet 512 (about 103k px of faces: canvas is seen from outside and inside).
- Budget override: elements 450, sheet 512
- **Collision:** solid (barriers) only where the canvas wall or the pole stands: the 15 cells the wall ring passes through at y=0 and y=1 (the ring is closed corner to corner, so nobody walks through the canvas), and the pole column (4, 0..6, 4). The door cell (4, y, 1) and every interior cell are open, so a troop or player walks in through the door and round the pole. Ropes, pegs, flaps, the rug and the chest have no collision (a dressing prop never blocks a lane by accident; the ropes are thin). The wall at y=2 (8 px above the barrier) and the roof are open: above head height. Card fill ignores turns, so the diagonal wall cells show a little less fill there than they really have (12-15%, true 11-16%).
- **Light and effects:** none (a camp lantern prop can stand at the door).
- **Gameplay effect (gameplay use only, design only):** none; dressing.
- **References:** the approved forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light; the Knight on the card for scale; `stake_barricade` (field carpentry beside it in a camp). No outside game's assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
