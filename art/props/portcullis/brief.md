# Prop brief: Portcullis (`portcullis`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** gameplay (gameplay object (cover, barricade, siege piece, objective): collision IS the design; any rule change needs Kort).
- **What it is, in one line:** one cell of a heavy gate grille: four full-height 2 px iron bars with 2 px gaps and an iron crossbar behind them every 8 px; tiles side by side and stacked to fill any gateway. No spikes (`portcullis_bottom` has them).
- **Where it goes:** gateways and arches of Siege and fortress maps: `portcullis_bottom` for the bottom row, `portcullis` above it, `portcullis_raised` in the arch when the gate is up.
- **Setting / House:** Any House; gateways
- **Footprint (cells, x east * y up * z south):** 1 x 1 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 16 px a cell (0.46 of the Knight); two cells high is troop height; bars every 4 px, even across cell edges.
- **Silhouette at 15 m:** a dense wall of dark vertical bars with one lighter band behind.
- **Close-up story at 3 m:** the crossbars show through the 2 px gaps behind the bars; nothing else, so cells tile cleanly.
- **Materials and ramps (proplib):** `iron` (bars, crossbars); no new ramps.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the one cell is a full barrier cube: troops cannot pass and arrows are stopped; the 2 px gaps are art only (the design: a closed gate).
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** today a closed wall by collision alone (S9). Proposal for Kort, no code: a gate that opens and closes (siege: attackers breach, defenders raise it), swapping the cells for `portcullis_raised`. Kort picks none, any or other.
- **References:** `stake_barricade` (collision-as-design); the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
