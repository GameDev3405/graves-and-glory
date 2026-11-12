# Rivergate build plan ("The Eyot")

Status: SIGNED by Kort 2026-11-12 ("Signed, start"; bases "Keep over the river"; ribs "16 high as planned"). Maps lead. Layout signed by Kort 2026-11-12 (record: `C:\Users\kortt\Melty\status\maps-lead-layouts.md`). Kort's picks for this plan (2026-11-12): ribs "Ribcage over the island"; fords "Dry stone causeway"; "Town in the middle"; island "Keep about 100 by 60". Nothing is built. All coordinates are world blocks at the shrunk size (x east, z south, ground y 64).

## 1. Fixed numbers

| Thing | Value |
|---|---|
| Playable field | x -270..270, z -190..190 (same box as Kingsfield, so the game's field size is shared) |
| Build box (sections) | x -288..287, z -192..191, 9 x 6 sections of 64 x 64 |
| Dawn base / Dusk base | (-215, 0) / (215, 0), 430 apart |
| Symmetry | mirror in x and in z. Only the north-west quarter is designed; the mill (north) and the shrine (south) differ in looks but have the same footprint and cover |
| Levels | street and bank y 64; water surface y 62; river bed y 59 (3 deep, a barrier); quay walls 2 above the water |
| River | along z = 0, 18 wide (z -9..8) from base to pool |
| Pool | ellipse radii 72 x 50 around (0, 0) |
| Island (the Eyot) | ellipse radii 50 x 30 (100 x 60), y 64, stone quay all round |
| Bridges | north and south, on x = 0, 16 wide (x -8..7), z 30..50 and mirror, flat deck at y 64, stone parapet 1 high |
| Causeways (fords) | at x = -106 and 106: dry stone, 14 wide, deck y 63 with a slab step each end, three culverts under |
| Points (radius 17) | A Mill Bank (0, -91); B Market Eyot (0, 0); C Shrine Bank (0, 91) |
| Bank roads | 7 wide: base bank gate (-190, -34) > (-152, -76) > A (0, -91) > (152, -76) > (190, -34); south mirrored |
| Bridge street | 9 wide on x = 0 from A to C over both bridges and across the market |
| Ford streets | 7 wide on x = -106 and 106 from the north bank road to the south bank road |
| Quay streets | 5 wide along both banks, 3 back from the quay wall, from the causeways to the pool |
| Lanes | every street at least 12 clear between house fronts where troops march; alleys 5 (shortcuts for players, too narrow for a line) |
| Edge | the Kingsfield edge: bank rising to +10 with a tree line from 258 to 270 (x) and 178 to 190 (z). The river leaves through a culvert arch in the bank behind each base |

Distances: base to causeway 109; base to A or C along the road about 235; A to B 91; causeway to bridge street 106.

Water guard (Kort 2026-11-12: "add a fence ... so unit and players cant just fall in and get stuck in the water"): every water edge is closed. Stone parapet 1 high (stone_brick_wall) round the whole island, round the pool, along both river banks in town and on bridges and causeways; post-and-rail fence along the river banks out of town. No open river stairs: stairs end at a gated landing above the water. The whole-map check walks the water edge and fails on any gap.

Changes agreed while building (Kort 2026-11-12): base gates face the field (east wall), one per bank, 8 wide, between twin towers; the bank roads start at (-180, -26) and (-180, 25). Base yards get tents and drill gear along the walls. Watermill has a closed wheel pit (no mill race). Market hall ridge y 80, gatehouse ridge y 77, shrine tower top y 84.

## 2. Mode sites

- Domination: A, B, C above.
- Escort "Market Run" (about 400): start north bank road (-110, -80) > stop 1 Mill Bank (0, -91) at 110 > north bridge > stop 2 market (0, 0) at 200 > south bridge > stop 3 Shrine Bank (0, 91) at 290 > end east causeway landing (106, 60).
- Siege (the island): yard = north bridgehead square (16, -62); gate = north bridge gatehouse (0, -50); keep = market hall (30, 0). The south gatehouse is walled for the round. Attackers' camp on the north bank road.
- Survival: shrine = C (0, 91); rise sites: river stairs (-61, 14) and (61, 14), graveyard (0, 150), old road (-71, 127).
- Neutral holds (as on Kingsfield, 24 x 24): two toll houses at the outer ends of the ford streets, (-106, -112) and (106, 112).

## 3. Palette

Tone: a damp, crowded river town that has seen better trade. Darker and wetter than Kingsfield, so the two Domination maps do not look alike.

| Use | Blocks |
|---|---|
| Streets | cobblestone, stone, andesite, gravel patches; mossy by the water; bank roads outside the town dirt_path with a cobble edge |
| Quays and bridges | stone_bricks, mossy and cracked stone_bricks, andesite; cobblestone footings; spruce mooring posts |
| House ground floors | stone_bricks or bricks; some whole brick houses |
| Upper floors | timber frame: dark_oak or spruce posts, white_terracotta or mud_bricks infill, jettied 1 block over the street |
| Roofs | three kinds mixed street by street: deepslate_tiles (slate), bricks (tile), spruce shingle. No dark oak sheet roofs |
| The ribs | bone_block and calcite, diorite at the joints: paler than every wall, like the Hand |
| Water edge | reeds are not used (sugar cane breaks); mossy stone, lily pads in the pool corners, mud and gravel shoals under the bridges |
| Out of town | grass, gardens with crops behind fences, orchards, water meadows with flowers, willows (oak with hanging leaves) |
| Team colour | cyan and purple banners at the bases only |
| Time | 3000 ticks |

## 4. Buildings (each built off-map, photographed, checked, then pasted; furnished from the start)

Lessons from the Kingsfield review, applied here from day one: every interior furnished; shutters, lintels, brackets, chimneys on every house; at least two versions of every repeated building, placed on opposite corners; capture points marked by a ring in the ground and standards outside it; nothing inside a ring.

Key pieces (Kort sees them before paste):

| # | Piece | Footprint, height | Notes |
|---|---|---|---|
| K1 | Water gate (Dawn; Dusk mirrored in purple) | 72 x 80, wall 6, gate towers 13 | The base straddles the river. The river runs through it in a walled channel with a bridge inside; a water arch with a raised portcullis faces the field. One gate onto each bank (north and south, 8 wide) and a muster yard on each bank. Barracks north, stores and armoury south |
| K2 | The ribs | 16 ribs (Kort 2026-11-12: "add another pair"), each about 6 x 22, tips at y 80 | Eight pairs: four ribs each side of each bridge, the fourth at the island's ends, rising from the pool at the island's rim and curving in over the bridgeheads. They stand in water. The breastbone is gone; a few rib tips are snapped |
| K3 | Market hall | 22 x 14, eaves 6, ridge 13 | Open arcaded ground floor (troops walk through), chamber above reached by an outside stair. East of the ring; a market cross and stalls mirror its cover on the west |
| K4 | Bridge and gatehouse (north; south mirrored) | bridge 16 x 20; gatehouse 20 x 8, 11 high | Gate passage 8 wide, open. The gatehouse stands on the bank end, outside both rings |
| K5 | Watermill (point A) | 20 x 14, ridge 13, wheel 9 across | On the pool edge north bank with a wheel in a short mill race. Point A on its forecourt, on the bridge street |
| K6 | River shrine (point C) | 20 x 14, tower 16 | Small stone chapel with a bell cote and a walled graveyard behind (the Survival rise site). Same footprint and cover as the mill |

Supporting pieces (I check these myself):

| # | Piece | Footprint | Count |
|---|---|---|---|
| S1 | Town house, narrow, jettied; 3 versions | 7 x 11, 3 floors | about 24 |
| S2 | Town house, wide, with shop front; 3 versions | 11 x 11, 2 floors | about 16 |
| S3 | Corner house with an arch through to a yard; 2 versions | 13 x 13 | 8 |
| S4 | Inn with a stable yard / Guild house | 20 x 16 | 2 + 2 |
| S5 | Warehouse on the quay with a hoist; 2 versions | 16 x 10 | 4 |
| S6 | Causeway | 14 x 22 | 2 |
| S7 | Quay kit: wall, river stairs, mooring posts, a moored barge, a rowing boat, a quay crane | - | laid by generator |
| S8 | Market stalls, market cross, well, pump, carts | small | scattered |
| S9 | Toll house (neutral hold) | 24 x 24 | 2 |
| S10 | Garden plots, orchard rows, willow, garden walls | - | laid by generator |

## 5. Terrain and town plan

1. Flat ground at y 64. River channel and pool cut to y 59, water to y 62, quay walls in town, grass banks with shoals out of town.
2. Town = the middle third: x -84..84 on both banks, plus the island. House blocks line the bridge street, the bank road and the quay street. Each bank has one small square: the mill forecourt (A) and the shrine forecourt (C), each 44 x 44 so the whole ring is open paving.
3. Between town and base (x 84..180): the causeway and its street, garden plots, orchards and water meadow. Open enough for a line of 20 to form.
4. Sight lines: the bridge street gives one long view from A through B to C. The gatehouses break it for archers; the ribs frame it.
5. Corners beyond |x| 170 and |z| 120: wood, as on Kingsfield. Edge bank and tree line all round.

## 6. Sections

Same grid as Kingsfield: columns A to I from x -288, rows 1 to 6 from z -192. The 15 sections below are designed; the rest are mirrors.

| Section | What is in it | Pieces | What the screenshot must show |
|---|---|---|---|
| E3 | North half of the island and pool, north bridge, three rib pairs, market north half | K2, K3, K4, S8 | Ring whole and empty; bridge 16 wide and level; ribs clear of the deck |
| D3 | Pool west end, river, quay streets, warehouses | S5, S7 | Quay wall unbroken; no way into the water except the river stairs |
| C3 | West causeway, ford street, gardens | S6, S10 | Causeway 14 wide, walkable without a jump |
| B3 | Dawn water gate north half, north muster yard | K1 | Gate 8 wide; river channel walled; banners cyan |
| A3 | Base rear, river culvert, edge bank | K1 | No gap in the edge |
| E2 | Point A square, watermill, bridge street, north gatehouse | K5, K4, S1, S2 | Ring whole; mill wheel reads; streets 12 clear |
| D2 | Town block west of A: houses, inn, alleys | S1, S2, S3, S4 | No dead ends for troops; roofs mixed |
| C2 | Bank road, toll house, orchards | S9, S10 | Road clear; hold yard clear |
| B2 | Bank road leaving the base, water meadow | S10 | - |
| A2 | Edge bank, wood edge | - | Edge closed |
| E1 | Back lane behind the mill, garden walls, edge | S10 | - |
| D1 | Town back gardens, edge | S10 | - |
| C1 | Pasture, edge | - | - |
| B1 | Corner wood | - | Trees off the road by 10 |
| A1 | Corner wood, edge bank | - | Edge closed |

## 7. Order of work

1. Workshop: K2 the ribs first (the look of the map hangs on them), then K4 bridge and gatehouse, K3, K1, K5, K6, then the houses and kits. Each piece: offline picture, in-game pictures from commander and soldier height, my check, Kort's look for K pieces.
2. Map generator (a copy of the Kingsfield one): ground, river, pool, island, streets; then pieces. Offline top and iso pictures.
3. Sections placed one at a time in the dev world with a picture of each before the next, in the table's order, then the mirrors.
4. Whole-map checks: rings empty, streets clear, quay wall closed, aerials, then one harsh review before Kort sees it.

## 8. Needs from other lanes

- AI lead / coordinator: can troops and bots path over a causeway with slab steps, through an 8-wide gate passage and across a 16-wide bridge with parapets? Do they avoid deep water? The plan assumes deep water is a barrier.
- Mode code: Siege sites per map; Escort route with three stops.
- Field box is the same as Kingsfield (arenaHalfX 270, arenaHalfZ 190).
