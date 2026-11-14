# Prop brief: Map Table (`map_table`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** lobby (lobby / Old Hold hub: seen up close (2-6 m) and lingered on, so detail and story carry).
- **What it is, in one line:** a field commander's trestle table: plank top on two A-frame trestles, a spread map held down by weights and daggers at the corners, and a candle stub with a lit flame.
- **Where it goes:** a command tent or camp, the lobby war room, a Siege defenders' camp. Builders and map designers place it.
- **Setting / House:** any House; plain field carpentry with a neutral parchment map.
- **Footprint (cells, x east * y up * z south):** 2 x 2 x 1 (the upper cell holds only the candle's light block: a light cannot share a solid cell and the engine requires a light cell inside the size; the table itself is 32 x 14 x 16 px). Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** the top at 14 px, 40% of a Knight (35 px), a hip-height table; the candle flame reaches 23 px.
- **Silhouette at 15 m:** a low wide pale table top on a dark trestle with a pin of orange flame at one end.
- **Close-up story at 3 m:** the map itself (hills, river, a route in red, a village, a red X); iron weights and a dagger driven in at the corners; the candle stub with its flame; a rolled spare scroll at the back edge.
- **Materials and ramps (proplib):** `plank_x`, `swood_x`, `wood_y` (table, trestles), `iron`, `steel`, `stone`, `linen`, `gilt` (dagger pommel); new ramps via `make_ramp`: parchment, wax, map red 982418, field green 6B7F45; HOT ramp for the glow flame.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px.
- **Collision:** both table cells solid (y=0): a troop cannot walk through a table, arrows hit it. The upper cell (1,1,0) is open: it holds the light block only.
- **Light and effects:** one light, level 10, at the candle: cell (1, 1, 0). The flame is its own glow part (`map_table_flame`, drawn full-bright). No particles.
- **Gameplay effect (gameplay use only, design only):** none (lobby).
- **References:** `muster_board` (the map and dagger); `brazier` (glow part); the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
