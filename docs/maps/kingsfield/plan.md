# Kingsfield build plan ("Crown Quarry")

Status: SIGNED by Kort 2026-11-12 ("Looks good, start the workshop"; Hand "44 long, road over the palm"; fields "Cut stubble and hay stooks"; "Yes, four beacons"). Maps lead. Layout signed by Kort 2026-11-12 (record: `C:\Users\kortt\Melty\status\maps-lead-layouts.md`). Nothing is built. All coordinates are world blocks at the shrunk size (x east, z south, ground y 64).

## 1. Fixed numbers

| Thing | Value |
|---|---|
| Playable field | x -270..270, z -190..190 (541 x 381) |
| Build box (sections) | x -288..287, z -192..191, 9 x 6 sections of 64 x 64 |
| Dawn base / Dusk base | (-215, 0) / (215, 0), 430 apart |
| Symmetry | mirror in x and in z. Only the north-west quarter is designed; the other three are mirrored by the plan generator |
| Pit | centre (0, 0); rim radii 66 x 50; floor radii 44 x 31; floor y 56 (8 deep); one bench 6 wide at y 60 |
| Ramps | west and east: 14 wide, x 74..44, 30 long. North and south: 10 wide, z 58..31, 27 long (north widened for the wagon) |
| Points (radius 17) | A West Grange (-128, 0); B Tithe Barn (0, -114); C the Hand (0, 0, y 56); D Mill Rise (0, 114); E East Grange (128, 0) |
| Granges | 56 x 56 walls (was 60; see changes), x 100..156 / -156..-100, z -28..27; wall 5 high; gates west and east 6 wide, side doors north and south 3 wide |
| Knolls | B and D on a mound +3 (y 67), flat top radius 20, slope 1 in 4 out to radius 32 |
| King's Road | 9 wide: base > grange > west ramp > pit floor > east ramp > grange > base |
| North / south roads | 7 wide: (-215,0) (-152,-91) (0,-114) (152,-91) (215,0); south mirrored |
| Link roads | 5 wide: B to north ramp, D to south ramp |
| Lanes | gaps in hedges and walls at least 12 wide |
| Edge | bank rising to +10 with a tree line from 258 to 270 (x) and 178 to 190 (z); nothing playable beyond |

Changes from the signed sheet, forced by the shrink (tell Kort):
- Granges 56 x 56 at x = +-128, not 60 x 60 at +-125. Otherwise the grange gate opens straight onto the pit ramp. Now 26 blocks of open ground lie between gate and ramp, and 34 between grange and base.
- The pit shrinks less than the field (rim 132 x 100, was 156 x 120) so the Hand (44 long) and point C still fit on the floor (88 x 62).
- Base to first point is 87 blocks. Base to pit centre is 215.

## 2. Mode sites

- Domination: the five points above.
- Escort "Tithe Run" (about 375): start (-98, 0) > west ramp > stop 1 the Hand (0, 0) at 95 > north ramp > stop 2 Tithe Barn (0, -114) at 210 > stop 3 beacon crossroads (95, -100) at 305 > end East Grange north door (128, -30).
- Siege (East Grange): yard (84, 8) on the rim road; gate = grange west gate (100, 0); keep = the hall in the north-east corner of the yard (144, -20; it was moved out of the capture ring in the workshop). East gate and side doors walled for the round. Attackers' camp at the pit's east ramp.
- Survival: shrine = the Hand (0, 0); rise sites at the ramp heads (0, -62), (0, 62), (-78, 14), (78, -14).
- Neutral holds (Kort 2026-11-12: "little locations/strong holds of separate npc/bots that are neutral to both sides which you can hire, fight/steal from, or stay neutral with"): two plots, about 24 x 24, at (-70, -96) and (70, 96). They replace supply houses. The mechanic is the coordinator's lane; the building is piece S9.

## 3. Palette

Tone: a worked, tired farm country around a quarry. Grim and weathered, not pretty.

| Use | Blocks |
|---|---|
| Plain | grass_block; breakup coarse_dirt, rooted_dirt, podzol; grass and a few poppies |
| Fields | stubble: coarse_dirt and rooted_dirt in rows, hay_block stooks, dead_bush. No standing wheat (it hides troops from the commander and farmland tramples) |
| King's Road | dirt_path core patched with cobblestone, gravel, andesite |
| Other roads | dirt_path, coarse_dirt edge |
| Field walls | cobblestone / mossy_cobblestone with a cobblestone_wall cap, 2 high |
| Hedges | oak_leaves (persistent) on a coarse_dirt bank, 2 high. Test first whether leaves stop arrows and sight; if not, cobble core |
| Quarry rock | stone, andesite, cobblestone, tuff; gravel scree; floor stone and gravel |
| The Hand | calcite, diorite, polished_diorite, white_terracotta shading; bone_block at the knuckle lines. Paler than every quarry rock so it reads as something else |
| Masonry | stone_bricks with cracked and mossy; cobblestone footings; andesite |
| Timber | oak_log frames, stripped_oak beams, spruce_planks floors, white_terracotta infill |
| Roofs | dark_oak stairs and slabs; barn and mill spruce; a few missing tiles |
| Trim, posts, lamps | stone_bricks, oak_fence, lantern (map standard) |
| Team colour | cyan (Dawn) and purple (Dusk) wool banners, at the bases only |
| Time | 3000 ticks, as now |

## 4. Buildings (all new; each built off-map in the workshop, photographed, checked, then pasted)

Key pieces (Kort sees them before paste):

| # | Piece | Footprint, height | Notes |
|---|---|---|---|
| K1 | Base camp (Dawn; Dusk is the mirror in purple) | 72 x 80, wall 5, gate tower 12 | Walled muster yard, open front 24 wide, command dais, barracks, stores, stable. Muster lane kept empty |
| K2 | Grange (West; East is the mirror) | 56 x 56, wall 5, gatehouses 9 | Two gatehouses, two side doors, wall walk reached by straight stairs. Inside: point ring in the yard, hall (Siege keep) 18 x 12, granary, cart shed, well |
| K3 | Tithe Barn | 34 x 16 plus a 3-deep wagon porch on each long side (34 x 22 over all), eaves 6, ridge 14 | Wide and low, great doors both long sides so the wagon passes through. Point B on its forecourt |
| K4 | Windmill | base 13 x 13, cap at 22, sails 21 across | Tall post mill on a stone base. Point D on its forecourt. Same cover as the barn: a 34 x 16 yard wall |
| K5 | The Hand | 37 x 48 box (x by z), 4 high | Palm up, wrist south, fingers north toward the north ramp; the King's Road crosses the palm, which is flush with the pit floor. Kort picked "B: curled" (2026-11-12): fingers and thumb curl up, tips 4 high; heel 1 high. Cover only on the north side of the point. Little finger snapped. Piece `pieces/k5_hand.nbt` (run `gen/hand.py OUT curled`). Point C pad sits in the palm. Generator `gen/hand.py` |
| K6 | Quarry crane | 9 x 9 base, 17 high, jib 12 out over the pit | Treadwheel crane on the north rim, east of the ramp |

Supporting pieces (I check these myself):

| # | Piece | Footprint | Count |
|---|---|---|---|
| S1 | Beacon tower | 7 x 7, 14 high, fire basket | 4, at the road bends (+-152, +-91) |
| S2 | Mason's lodge | 12 x 9 | 2, on the rim by the west and east ramps |
| S3 | Ramp works: retaining walls, timber scaffold, stone sledge | per ramp | 4 |
| S4 | Farm cottage, two variants | 9 x 7, 9 x 11 | 8 |
| S5 | Hay barn / cart shed | 12 x 8 | 4 |
| S6 | Field wall and hedge kit: straight 8, corner, gap with posts, stile | - | laid by generator |
| S7 | Wayside cross and milestone | 3 x 3 | 6 |
| S8 | Well, trough, stooks, cart | small | scattered |
| S9 | Neutral hold: small walled steading with a yard, a hall and a stores shed | 24 x 24 | 2 |

## 5. Terrain

1. Flat plain at y 64. Noise of +-1 only away from roads, points and lanes.
2. Pit: ellipse cut, bench at y 60, floor at y 56, rock faces sealed, four ramps. Rim: a bare ring 10 wide of gravel and stone, no cover.
3. Knolls at B and D.
4. Roads cut last so they stay level; ramps graded.
5. Field pattern: strip fields square to the roads. Walls and hedges run north-south (across the line of march), every 40 to 55 blocks, each with a gap at least 12 wide.
6. Woods: only in the four corners, beyond |x| 170 and |z| 120, never within 10 of a road.
7. Edge bank and tree line.

## 6. Sections

Grid: columns A to I from x -288 (A = -288..-225, B = -224..-161, C = -160..-97, D = -96..-33, E = -32..31, F = 32..95, G = 96..159, H = 160..223, I = 224..287); rows 1 to 6 from z -192 (1 = -192..-129, 2 = -128..-65, 3 = -64..-1, 4 = 0..63, 5 = 64..127, 6 = 128..191). The 15 sections below are designed; every other section is its mirror (F..I mirror D..A; rows 4..6 mirror 3..1).

| Section | What is in it | Pieces | What the screenshot must show |
|---|---|---|---|
| E3 | Pit north half: floor, bench, north ramp, north half of the Hand, rim | K5, K6, S3 | Hand reads as a hand from above; ramp walkable and 10 wide; no cover on the rim |
| D3 | Pit west side: west ramp, rim, mason's lodge, King's Road | S2, S3 | Ramp 14 wide and even; road meets ramp without a step |
| C3 | West Grange north half: wall, north door, gatehouses' north halves, hall | K2 | Gates 6 wide and open; point ring whole; stairs straight |
| B3 | Dawn base north half: yard, barracks, front opening | K1 | Muster lane empty; exit 24 wide; banners cyan |
| A3 | Base rear wall, edge bank, tree line | K1 | No gap in the edge |
| E2 | Tithe Barn on its knoll, point B, link road to the pit | K3 | Barn doors line up with the road; ring whole on the forecourt |
| D2 | North road, west slope of the knoll, strip fields, supply plot | S6, S9 | Lanes at least 12; slope walkable |
| C2 | Road bend, beacon tower, cottage, hay barn | S1, S4, S5 | Tower visible from the base and from B |
| B2 | North road leaving the base, fields, two walls across the march | S6, S7 | Each wall has its gap; road clear |
| A2 | Edge bank, wood edge | - | Edge closed |
| E1 | Orchard and back wall behind the barn, edge | S6 | Nothing blocks the barn's rear door |
| D1 | Pasture, hedges, cottage, edge | S4, S6 | - |
| C1 | Pasture, edge | S6 | - |
| B1 | Corner wood, edge | - | Trees off the road by 10 |
| A1 | Corner wood, edge bank | - | Edge closed |

## 7. Order of work

1. Workshop world: one flat pad per piece. Build K5 (the Hand) first, then K2, K1, K3, K4, K6, then S pieces. Each: top and iso render, in-game screenshots from the commander's height and from a soldier's height, self-check against this plan, fix, then Kort's look for K pieces. Export each as an nbt piece.
2. Map world: flat field, then the terrain passes for the whole north-west quarter, mirrored. Screenshot.
3. Sections in this order, one at a time, each pasted, screenshotted in game and checked before the next: E3, D3, C3, B3, A3, E2, D2, C2, B2, A2, E1, D1, C1, B1, A1; then the mirrors, each with its own screenshot.
4. Whole-map checks: walk scan (roads and rings clear), gap check, a top render at full size, a commander's-view screenshot from each base.

## 8. Needs from other lanes

- Field box: Kort approved changing it ("Yes, change it"): arenaHalfX 270, arenaHalfZ 190.
- Escort: three stops and this route. Siege: read sites per map. Both in the mode code.
- Neutral holds: new mechanic to scope (hire, raid, or ignore).
