# Ironhold build plan ("The Iron Pass")

Status: **draft for Kort, 2026-11-13.** Fresh design; it replaces the signed 2026-10-30 layout (`layout-draft-2026-10-30.md`, kept for history). Shape approved by Kort on sketch 2 (`gen/layout.py`, picture `work/ih-layout2.png`). Nothing is built yet.

Kort's rulings (2026-11-13): start fresh; Rivergate size; lopsided (attackers and defenders); a mountain fortress siege; main gate plus two flanks; one point outside, one at the gate, one inside; equal walking time; no water troops can reach; gentle 12-block climb; walls not a box; volcanic backdrop.

## 1. Fixed numbers
- Build box x -288..287, z -192..191 (576 x 384). Sections: columns A..I from x -288, rows 1..6 from z -192, 64 x 64 each, 54 in all.
- Heights (map y; test world adds 135): valley 64, outer ward 70, inner and upper ward 76. Keep spur top 84. Mountain rim 96..130.
- Spawns: Dawn (attackers) (-256, 0) at 64. Dusk (defenders) (256, 0) at 76.
- Capture points, radius 17, flat ground, nothing standing in the ring:
  - A (-112, 0) at 64: the Outwork, a ruined watch post on the road below the main gate.
  - B (0, 0) at 70: the Muster Yard, the middle of the outer ward.
  - C (112, 0) at 76: the Keep Court, under the Great Keep.
- Walks: both spawns to B 256 blocks; Dawn to A and Dusk to C 144 blocks. Climbs are ramps of full blocks, one step at a time, at least 5 wide on flank roads and 9 on the Iron Road.
- No water anywhere a troop can reach. Lava is scenery only: outside the play area, behind solid rock at least 3 thick and 4 high, never beside a walkable cell. `check` must print 0 for both.

## 2. Mode sites
- Domination: A, B, C as above.
- Siege (Dawn attacks): stage A hold the Muster Yard (B); stage B break the inner gate at (40, 0); stage C hold the Keep Court (C). Rows and timings are for the modes lane; this map only fixes the places.
- Defender stations: main gatehouse top (-72, ±9), inner gatehouse top (40, ±9), keep wall-walk facing C.

## 3. Palette
- Fortress stone: deepslate bricks and tiles, polished deepslate bands, blackstone footings, cracked variants at the breach. Iron: iron bars, chains, anvils, cauldrons, lightning rods, heavy-weighted plates used only as trim off the routes.
- Timber: dark oak and spruce, soot-dark. Roofs deepslate tile and dark oak.
- Valley: grass, coarse dirt, gravel, podzol, dead bushes, spruce; greyer and sparser toward the wall. Slag heaps of gravel, tuff, basalt and raw iron blocks.
- Mountain: stone, tuff, deepslate, basalt, blackstone; magma blocks and lava glow in far cracks; smoke from basalt vents. No snow.
- Banners: Dawn teal in the camp, Dusk colours on the walls (as on Rivergate).

## 4. Buildings (each built off-map, photographed, checked, then pasted; furnished from the start)
| Piece | Where | Notes |
|---|---|---|
| Main gatehouse | (-72, 0) | Twin towers, passage 9 wide, raised portcullis, murder holes as looks only |
| Outer curtain | cliff to cliff, crooked, x -84..-64 | Wall-walk, 4 towers at the bends, buttresses |
| Mine gate | (-77, -95) | North flank: an ore-cart tunnel through the wall, 5 wide, rails as trim beside the path |
| The breach | (-68, 73) | South flank: collapsed wall, rubble ramp of full blocks, 7 wide |
| Inner gatehouse | (40, 0) | Smaller; the Siege gate target |
| Inner curtain | x 26..48 | Two posterns, north (39, -67) and south (45, 49) |
| Upper wall and gate | x 168..178 | Defenders' way down from the barracks |
| Great Keep | spur at about (120, -62) | The skyline landmark, 30 tall above the court, entered from the court |
| Barracks | (234, 0) | Dusk spawn building |
| Forges and smelter | outer ward north | 3 buildings, chimneys, cold hearths outdoors (no lit fire on walkways) |
| Stables and stores | outer ward south | 2 buildings and a yard |
| Outwork ruin | around A, outside the ring | Broken tower stump and low walls for cover |
| Siege camp | (-258, 0) | Tents, palisade, two trebuchets, supply wagons |
| Mine head | valley north, about (-150, -96) | Winding frame, ore carts, slag heaps |
| Ward houses | inner ward | 6 to 8, from the Rivergate house kit in dark stone and timber |

## 5. Terrain and roads
- The pass floor narrows eastward (sketch 2). Outside it the mountain rises at once: 2 blocks high within 2 cells of the edge, so nobody walks out.
- Iron Road (centre): camp, round the crag, A, main gate, B, inner gate, C, upper gate, barracks.
- Mine road (north): camp, mine head, mine gate, forges, north postern, C.
- Goat path (south): camp, scree, breach, stables, south postern, C.
- Cross links: in the valley west of the crag; in the outer ward between the rock knuckles; in the inner ward round the Keep Court. Each ward is reachable by all three lanes.
- Cover: valley medium (boulders, carts, mantlets), thinning to open ground in the last 30 blocks before the wall; outer ward dense; inner ward medium.
- A dry ditch in front of the outer curtain, 2 deep, crossed by the road on a stone causeway and filled level at the two flank ways in. Its sides are full-block steps so a troop can always walk out.

## 6. Sections
54 sections, A1..I6. Rows 1 and 6 and column I's corners are mostly mountain. Build order by column, west to east, after the pieces: A-C (valley, camp, mine head), D (outer curtain), E (outer ward), F (inner curtain), G (inner ward, keep), H-I (upper ward, barracks, backdrop).

## 7. Order of work
1. Generator skeleton: heights, pass edge, roads, points, `check` (rings clear and flat, no reachable water or lava, every lane walkable spawn to spawn with 1-block steps).
2. Blockout picture of the whole map for Kort.
3. Pieces in the workshop, one at a time, each photographed.
4. Terrain and dressing by column, export, in-game shots of every section.
5. Non-full-block inventory and a Sonnet (high) review; fixes; Kort's page.

## 8. Needs from other lanes
- Coordinator / modes: Ironhold's spawn, point and Siege rows all change (old rows used x -350..349 coordinates). To be wired after Kort approves the built map, as Rivergate was.
- AI lead: per-map lane waypoints (three lanes, six wall openings listed in section 4).
