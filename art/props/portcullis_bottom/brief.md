# Prop brief: Portcullis Bottom (`portcullis_bottom`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** gameplay (gameplay object (cover, barricade, siege piece, objective): collision IS the design; any rule change needs Kort).
- **What it is, in one line:** the grille of `portcullis` with 4 px tapered steel tips under the bar feet: the bottom row of a gate. Same bars, same crossbar, same x and z.
- **Where it goes:** the bottom row of a gateway, with `portcullis` stacked above it; `portcullis_raised` in the arch when the gate is up.
- **Setting / House:** Any House; gateways
- **Footprint (cells, x east * y up * z south):** 1 x 1 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 16 px a cell (0.46 of the Knight); the tips are 4 px long.
- **Silhouette at 15 m:** a dense wall of dark bars ending in a row of pale points at the ground.
- **Close-up story at 3 m:** steel tips tapering to a 1 px point under each bar, with a steel ferrule band.
- **Materials and ramps (proplib):** `iron` (bars, crossbars), `steel` (ferrules, tips); no new ramps.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the one cell is a full barrier cube: troops cannot pass and arrows are stopped.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** as `portcullis`: closed wall by collision today; open and close is a proposal for Kort, no code.
- **References:** `portcullis` (same bars); the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
