# Prop brief: Raised Portcullis (`portcullis_raised`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a gate drawn up into its arch: the bottom few pixels of the bars and their tips hang from the top of the cell, with the lowest crossbar behind them.
- **Where it goes:** the top cell of a gateway, over an opening, with `portcullis` as the lowered variant; or alone as dressing under an arch.
- **Setting / House:** Any House; gateway arches
- **Footprint (cells, x east * y up * z south):** 1 x 1 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 16 px a cell; the tips reach 10 px below the cell top, above a troop's head when placed at y 2 or higher.
- **Silhouette at 15 m:** a row of four tipped bar ends hanging from the top of the arch.
- **Close-up story at 3 m:** the spikes hang over a troop's head; same bars, tips and crossbar as `portcullis_bottom` so the pair matches.
- **Materials and ramps (proplib):** `iron`, `steel` as `portcullis_bottom`; no new ramps.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** none: the spikes hang above head height when the cell is placed at y 2 or higher; it is dressing only.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (not a gameplay prop).
- **References:** `portcullis` and `portcullis_bottom` (same x and z, same bars); the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
