# Prop brief: Market Stall (Fish) (`market_stall_fish`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** lobby (lobby / Old Hold hub: seen up close (2-6 m) and lingered on, so detail and story carry).
- **What it is, in one line:** a market stall selling fish: the base stall with two ice trays of silver fish on the counter and three fish hung from the awning.
- **Where it goes:** the hub and Old Hold lobby streets and any town or market map; builders place it along a lane or against a wall (front faces north).
- **Setting / House:** any House; wooden counter, red and cream striped awning (not team coloured).
- **Footprint (cells, x east * y up * z south):** 3 x 3 x 2. Front faces north. Anchor cell (1, 0, 0).
- **Scale against a troop:** counter top at 15 px (waist height on a 32-35 px troop); awning front at 34 px and back at 47 px, so a troop stands under it; 3 cells wide.
- **Silhouette at 15 m:** a striped red and cream awning pitched over a dark counter, with the goods' colour showing on the counter and under the awning edge.
- **Close-up story at 3 m:** Silver fish with an eye on ice in plank trays; fish strung up under the awning edge. Scalloped valance, price slate, back shelf. Scalloped valance, price slate, back shelf.
- **Materials and ramps (proplib):** `plank_x`, `swood_x`, `wood_y`, `wood_x`, `wood_z`; awning ramps via make_ramp (red A33A2E, cream D2C29A) in a Stripe painter; slate ramp 3B4A4A; goods ramps via make_ramp (see model.py).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** barriers on the three counter cells only (x 0..2, y 0, z 0); posts, awning and goods are walk-through for the seller's side; the counter is 15 px, so a troop is blocked by the barrier and cannot hop it.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (lobby dressing).
- **References:** the approved forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light; muster_board for lobby close-up detail; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
