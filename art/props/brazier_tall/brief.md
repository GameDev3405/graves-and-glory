# Prop brief: Tall Brazier (`brazier_tall`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a tall brazier: a big iron fire bowl with a heavy rim on a stone pedestal (iron-banded column, four iron claws), open fire with yellow and orange flame tongues rising above the rim; harmless.
- **Where it goes:** gate yards and gate stairs (Ironhold's main gate first, where the 1-block `brazier` was too small to read), camps, hall doors; any battle map near a gateway or Domination point. The old `brazier` stays for low corners.
- **Setting / House:** Any House; gate yards and camps
- **Footprint (cells, x east * y up * z south):** 1 x 3 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 48 px tall with the flames (1.4 Knights); the rim at 34 px sits at a Knight's shoulder (35 px), so the bowl and fire read above the troops' heads in a crowd.
- **Silhouette at 15 m:** a dark column under a wide black bowl with a bright orange-yellow flame on top, visible by day.
- **Close-up story at 3 m:** glowing coals under the flame; iron claws gripping the bowl; carrying rings on the bowl and iron bands on the stone column.
- **Materials and ramps (proplib):** `stone`, `stone_top` (pedestal), `iron` (bands, claws, bowl), `coal` (glow coal bed); flames use the proplib `HOT` ramp (the coals' glow ramp) unlit, tone 3 orange tongues and tone 4 yellow core; no new ramps.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** ground cell (0,0,0) only (a barrier): the pedestal fills it. The bowl and fire cells above are open (a troop cannot reach them past the 1-block barrier anyway); arrows at head height pass over the bowl.
- **Light and effects:** light level 15 on the open cell (0,2,0) in the flames; no particles or sound. The fire part is full-bright (glow), so it reads by day and night.
- **Gameplay effect (gameplay use only, design only):** none (not a gameplay prop).
- **References:** `brazier` (base style, unchanged), the forge coals for the glow; Ironhold gate screenshot (bb-maps ih-map-6 gate-brazier.png) where the small brazier vanished; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
