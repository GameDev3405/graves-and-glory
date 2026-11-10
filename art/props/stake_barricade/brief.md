# Prop brief: Stake Barricade (`stake_barricade`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** gameplay (gameplay object (cover, barricade, siege piece, objective): collision IS the design; any rule change needs Kort).
- **What it is, in one line:** a field fortification: sharpened stakes lashed to two rails on a split-log sill, points leaning out toward the enemy, braced from behind.
- **Where it goes:** any battle map: across a road or a gap in a hedge, in front of a Domination point, along Siege approaches. Builders and map designers place them; nothing in the game spawns them.
- **Setting / House:** any House; plain field carpentry, no colours, so it suits every side.
- **Footprint (cells, x east * y up * z south):** 3 x 2 x 1. Front faces north (the points lean north). Anchor cell (1, 0, 0).
- **Scale against a troop:** about 30 px, just under a troop (32-35 px): a troop standing behind it shows head and shoulders; it is too tall to step over.
- **Silhouette at 15 m:** a row of pale points over a dark wall, leaning one way, so a player reads which side is "front" at a glance.
- **Close-up story at 3 m:** the fresh pale heartwood on each sharpened point; rope lashings; the braces behind that would take a charge.
- **Materials and ramps (proplib):** `wood_y`/`wood_x` (stakes, rails, braces), `stump` and `endgrain` (the sill log), `swood_y` (chocks); new ramp `make_ramp("B49A72")` for the cut points; `LINEN` ramp for rope.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** all 6 cells (3 wide, 2 high) solid: troops must go around, arrows fired at chest height hit it, and a troop cannot hop it. The model fills the top row only partly (stakes), which is intended: the barrier is the barricade's height, rounded up.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** today it is cover by collision alone (S9: blocks paths and arrows). Proposal for Kort, no code: (1) destructible: about 3 heavy hits or siege fire removes it (barriers and parts go, leaving a broken stump prop); (2) troops directly behind it take less ranged damage, since arrows already stop on it, maybe nothing more is needed; (3) Siege defenders may place up to N before the battle from the War Table. Kort picks none, any, or other.
- **References:** the approved forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light; the Knight on the card for scale. No outside game's assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
