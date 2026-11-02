# Map standards (task 2A.7)

Status: working standard, written 2026-10-30 from the code at commit bd05fa2 (main). Owner: Agent. Evidence for row 2A.7 of `docs/PLAN-TO-1.0.md`.

How to read this file. Every number is either read from the code or the sheets (cited as `file:line`) or is listed under "Questions for Kort". A value marked "current" is what the generator produces today; it becomes a standard only where this file says "binding" (a check already enforces it) or Kort approves it. Nothing here moves a spawn, a capture point or a road; those are Kort-approved changes (`docs/PLAN-TO-1.0.md` 7.3).

Scope caveat. All coordinates are the pre-redesign layouts. Kort ruled on 2026-10-22 that a separate layout-redesign spec follows Phase 3 (memory `kort-map-vision`), and the adoption format is a generated base plus an edit overlay (`docs/PLAN-TO-1.0.md` 7.2, section 4). Once a map is adopted, these standards apply to the final blocks (base plus overlay), and the numbers that come from the generator must be re-derived (plan row 4A.3).

Paths below are relative to the repository root. `Tests:` means `test-tools/java/gg/gravesandglory/`.

## 1. Where the maps come from

- One parametrised generator builds all five maps: `CampaignMaps.select(index)` (`src/main/java/gg/gravesandglory/CampaignMaps.java:28-107`). `selected` is 0 to 4: 0 Rivergate, 1 Ironhold, 2 Frostmarch, 3 Hollowwood, 4 Kingsfield (names: `sheets/design.json` key `maps`; sheet text at `CampaignMaps.java:32`).
- It swaps whole tables into `Spec`: POINTS, STRUCTURES, TERRAIN, SCENERY, and FACTIONS rows 0 and 1 (`CampaignMaps.java:96,104,106`). `Spec.java` is generated from `sheets/` by `tools/preflight.py`; never edit it by hand.
- Terrain: `Arena.height` (`Arena.java:96-119`), columns by `Arena.buildColumn` (`Arena.java:206-230`), structures and rings by `Arena.finishBuild` (`Arena.java:232-250`), props by `MapDressing.dress` in the order camps, points, bridgeheads, redoubts, setPieces, roadLamps, worn (`MapDressing.java:24`).
- Landmarks are authored NBT pieces (`data/gravesandglory/structures`, 38 pieces in `sheets/design.json` key `pieces`, 98 rows in key `landmarks`: map 0 18, map 1 25, map 2 18, map 3 19, map 4 18). A landmarks row replaces the generator row it names (`CampaignMaps.java:114-125`).
- The playable box is |x| <= 350, |z| <= 250, floor y 64; the world box is 366 by 266 (`sheets/design.json` key `rules`: arenaHalfX 350, arenaHalfZ 250, floorY 64, worldHalfX 366, worldHalfZ 266). A barrier column stands on the box edge (`Arena.java:229`). Terrain rises at the border by up to boundaryRise 12 (`Arena.java:113-114`; key `terrainStyle`).
- Cache identity: `LayoutMarker` holds layout `campaign-4`, the map, margin `margin-1/<CampTerrain.REVISION>` and the landmarks hash (`LayoutMarker.java:9-20`). Any change to what the generator lays must bump one of them, or saves keep the old arena.

## 2. The five maps at a glance (current)

Points: radius 17, capture time 8 s, elevation 64 unless noted (`CampaignMaps.java:38`). Point x is the river centre at that z (`riverX`, `CampaignMaps.java:130`) plus an offset of -29 / 0 / +29 for the three-point maps (`CampaignMaps.java:37`).

| Map | Points (id, name, x, z) | River x(z), half-width | Crossings (A / B / C), deck x depth |
|---|---|---|---|
| 0 Rivergate | A Mill Hamlet (-39,-145); B Trade Crossing (0,0); C River Shrine (39,145) | 12 sin(0.014 z), 6 | wood / stone / ford, 44 x 16, 44 x 24, 44 x 16 |
| 1 Ironhold | A Supply Yard (-29,-130); B Iron Gate (0,0); C Watch Hill (29,130) | 0 (straight), 6 | same kinds, same sizes |
| 2 Frostmarch | A Caravan Refuge (-37,-155); B Pass Crossing (0,0); C Signal Ridge (37,155) | 9 sin(0.013 z), 6 | same kinds, same sizes |
| 3 Hollowwood | A Logging Camp (-46,-145); B Old Causeway (0,0); C Moon Shrine (46,145) | 18 sin(0.011 z), 9 | same kinds, same sizes |
| 4 Kingsfield | A North Ford (-9,-160); B West Outpost (-130,100, elev 74); C Kings Bridge (0,0); D East Outpost (130,-100, elev 74); E South Ford (9,160) | 12 sin(0.014 z), 6 | same kinds, same sizes |

Sources: points `CampaignMaps.java:32-38,82-83`; crossings `CampaignMaps.java:39` (`bridge`, 44 wide along x, depth 16 / 24 / 16, elevation 64, palette wood / stone / ford by index); river `CampaignMaps.java:130-131`.

Shared on every map: two team camps at x = +-286 (section 9), four watchtowers at (+-180, +-85) elevation 67 (`CampaignMaps.java:63`), a ridge, a hill and a shelter hollow per side (`CampaignMaps.java:64-66`), and mountains at z = +-247, radius (180, 45), height 20 (45 on Frostmarch) (`CampaignMaps.java:68-69`).

Per-map swaps: Ironhold removes farm, mill, shrine, adds a supply barn, garrison and four curtain walls at (+-72, +-62), and replaces ruin and breach with the keep and the gate (`CampaignMaps.java:53,70-74`). Frostmarch adds a caravan lodge, a pass refuge and the signal tower at (62,170), and removes farm, mill, market (`CampaignMaps.java:75-80`). Hollowwood adds a sawmill and a broken chapel and removes farm and mill (`CampaignMaps.java:85-89`). Kingsfield adds the two raised outposts and their roads (`CampaignMaps.java:81-84`).

## 3. Block palette

### 3.1 Per map (current, binding for the dressing code)

| Slot | Rivergate (0) | Ironhold (1) | Frostmarch (2) | Hollowwood (3) | Kingsfield (4) |
|---|---|---|---|---|---|
| Ground (`Arena.natural`, stone above y 94) | grass_block | grass_block | snow_block | podzol | grass_block |
| Ground plants | grass, poppy | grass, poppy | snow | fern | grass, poppy |
| Cover wall block | cobblestone | cracked_stone_bricks | packed_ice | mossy_cobblestone | stone_bricks |
| Cover top | cobblestone_wall | stone_brick_wall | spruce_fence | mossy_cobblestone_wall | stone_brick_wall |
| Pave (plaza ring, parade) | polished_andesite | andesite | packed_ice | mud_bricks | polished_andesite |
| Trim | stone_bricks | stone_bricks | polished_diorite | mossy_stone_bricks | stone_bricks |
| Post / fence | dark_oak_fence | stone_brick_wall | spruce_fence | spruce_fence | oak_fence |
| Lamp | lantern | lantern | lantern | soul_lantern | lantern |
| Planks / log | oak | oak | spruce | spruce | oak |
| Capture edge stone | polished_blackstone | polished_blackstone | blue_ice | polished_blackstone_bricks | polished_blackstone |
| Brazier fire | campfire | campfire | campfire | soul_campfire | campfire |
| Time of day (ticks) | 3000 | 3000 | 6000 | 11500 | 3000 |

Sources: `MapDressing.java:16-22,131,146,156`; ground `Arena.java:204`; plants `Arena.java:227`; time `Arena.java:233`. Minimap colours per map: `Arena.java:197-199`.

Structures take a palette name, not a block list: `dawn` (stone_bricks, cyan wool), `dusk` (deepslate_bricks, purple wool), `stone`, `wood` (`Arena.java:282`, `CampaignMaps.java:58-61`). Fixed blocks that do not change per map: the 3x3 chiseled_stone_bricks pad and the 6-block oak_fence flag pole at every point (`Arena.java:242,315`), the smooth_stone ring on the radius (`Arena.java:240`), the camp inlays cyan / purple glazed terracotta (`MapDressing.java:82`), banners in cyan / purple / white wool (`MapDressing.java:71`).

Standard (binding, rule of the code): a prop's block comes from the slot of its map, never from a literal, except the fixed blocks above. New dressing adds a slot to the table above rather than a block name in a method. Palette tone targets from Kort's vision (grim, weathered, Souls-like; Hollowwood eerie) are in section 12.

### 3.2 Per House

There is no per-House block palette in the arena code. The five Houses (gilded, graveborn, wildmoot, ironclad, tidecallers; `sheets/design.json` key `houses`) have an accent colour (0xDFCE68, 0x9DBF9E, 0xC9853A, 0xE0703A, 0x4FA89A) and a glyph, used by the UI and the camp pieces. On the battlefield a team shows only its side colour: cyan for team 0 and purple for team 1 (`MapDressing.java:71-72`, `Arena.java:324`). The camp has one tent piece per House (`camp/tent_gilded`, `tent_wildmoot`, `tent_ironclad`, `tent_tidecallers`; `sheets/design.json` key `pieces`). Whether the battlefield should carry House blocks is a Question for Kort (Q3).

## 4. Road widths

The `Road` record is `(ax, az, bx, bz, width)` (`CampaignMaps.java:26`). A cell is road when its distance to the segment is below `width` (`CampaignMaps.java:127-129`), so `width` is the half-width: a road of "width 5" is about 10 to 11 cells wide on the ground. The walk scan and the through-road rule read the same number (`Walkability.java:76`).

Current values:

| Road | Width | Source |
|---|---|---|
| Camp fan: muster point (+-290,0) to lane head (+-205, z), bent once on the barracks side | 5 | `CampaignMaps.java:40-42,110` |
| Lane head to the river bank (+-205,z) to (rx+-28,z) | 5 | `CampaignMaps.java:43` |
| Across the crossing, (rx-29,z) to (rx+29,z) | 6 | `CampaignMaps.java:45` |
| Camp internal, barracks to stores (+-286,-32) to (+-286,40) | 5 | `CampaignMaps.java:62` |
| Lateral road joining the three lane heads (+-205, z0) to (+-205, z2) | 4 | `CampaignMaps.java:62` |
| Rivergate house lanes, farm and mill spurs | 3 | `CampaignMaps.java:50-51` |
| Tower spurs (+-180,+-85) to (+-205,+-85) | 3 | `CampaignMaps.java:63` |
| Frostmarch lodge and refuge roads | 4 | `CampaignMaps.java:80` |
| Hollowwood sawmill road (4) and chapel road (3) | 4 / 3 | `CampaignMaps.java:89` |
| Kingsfield outpost roads | 4 | `CampaignMaps.java:84` |
| Supply-house spur, yard centre to the lane (the table in section 8.3) | 4 | `CampaignMaps.java:93` |

Standard (current practice, binding where a check enforces it):
- Widths 6 (crossing), 5 (army approach and camp), 4 (lateral, spur, outposts), 3 (hamlet lane, tower spur) are the only widths in use. A new road takes one of these four, and a road wide enough to carry a squad line (5 or more) never ends inside a building.
- A road of width 4 or more that covers a building cell without either end inside that footprint is a "through" road and fails the walk scan (`Walkability.java:12-13,74-78`). A house lane ends at its house; a through road does not.
- Roads are terrain constraints: the ground follows them within 8 blocks (`Arena.java:107-108`), woodland keeps at least 10 off (`CampaignMaps.java:99`), props keep 1.5 off (`MapDressing.java:53,56`), the dressing never covers a road cell.
- A road that ends on a raised end (Kingsfield outposts, raised supply houses) climbs on a smoothstep over CLIMB = 48 blocks (`Arena.java:169,173-192`); a road that only passes an outpost stays level.
- Road surface is dirt path (`Arena.java:221`); each map paves the camp parade ground only (`MapDressing.java:80-81`). Worn earth is laid one block wide along road edges and one block outside each capture edge, last, and lone cells are put back (`MapDressing.java:231-256`).
- Lamps stand every 16 blocks along a road, from 8 blocks in, alternating sides at width + 2 off the centre line, at least 9 from another lamp, skipped on roads under 12 long (`MapDressing.java:192-195`).

## 5. Capture circles

Current, the same on all maps (`CampaignMaps.java:38`):
- Radius 17 blocks (34 across), capture 8 s, scoring and win rules in `sheets/design.json` key `rules` (winScore 300, scoreInterval 80 ticks, roundSeconds 900).
- Layout per point (all measured from the pole at the point centre): 3x3 chiseled pad (`Arena.java:242`); trampled earth inside 9.2 with a trim compass 3 wide on the axes and pave from radius 8 (`MapDressing.java:127-129`); smooth_stone ring on the cells with distance d from r-1 to r at the point's elevation (`Arena.java:236-241`); a flush edge stone band at r-0.5 to r+0.5 (`MapDressing.java:131-133`); 20 boundary posts on r+3 with a lantern (`MapDressing.java:135-136`); six cover walls at r = 11 (section 6); four braziers at r = 15 on the diagonals (`MapDressing.java:145-146`); eight approach clumps at r+9 = 26 (`MapDressing.java:148-151`); two obelisks 9 high at r+5 = 22 on the two road-free diagonals farthest out (`MapDressing.java:153-157`); flag pole 6 blocks, cloth 3 by 2 (`Arena.java:315,324`).
- The ring is only laid when the band has at least 40 contiguous cells on ground clear of the river bank and decks (`Arena.ringLaid`, `Arena.java:42-51`). Where it is not laid (the river crossing points) the point is the bridge, and neither ring nor edge stone is laid.
- Ground under a point is flat out to radius + 2 and blends to the terrain over 9 blocks (`Arena.java:103`). Props on a plaza are skipped over water, on built cells, and within 1.5 of a road.

Standard (binding where a check enforces it; `p3-arena-ring-map<N>` and the landmark `-ring` check, `Phase3Checks.java:1480,3412-3436`):
- Radius 17 on every map until a design decision says otherwise (Q6). The ring, pad and pole must exist exactly as above wherever `ringLaid` says so; for a landmark that encloses a point (the Kingsfield outposts) the check wants the ring in smooth_stone on every ring cell, the 9 pad cells chiseled, a pole of 6 fence blocks, 6 wool cloth cells and a sea_lantern at +7 (`Phase3Checks.java:3413-3423`).
- Every road that ends at an enclosing landmark's point must be open (no collision shape at ground + 1 and + 2) on the cells within 2 of the road from radius 17.5 to the footprint edge (`Phase3Checks.java:3424-3434`).
- No landmark column may stand within 19 blocks of a point it does not enclose (17 + 2; `Phase3Checks.java:3408-3411`).
- A raised point (elevation above 64) gets the stockade and four lookouts from `MapDressing.outpost` unless the map replaces it with a landmark (`MapDressing.java:158,162-169`).

## 6. Cover density

There is no density target in the code or the sheets; only the amounts the generator lays (current), listed so a landmark or an overlay can be measured against them:

- Per capture point: six cover walls of 5 cells, 2 high (cover block plus cover top), centred at r = 11 on angles pi/6 + k pi/3, each only where all 5 cells are off roads (1.5), the river and structures and within 1 block of the point's elevation, plus a barrel or hay block 2 behind (`MapDressing.java:137-143`): at most 30 wall cells. Eight approach clumps of 3 cells at r + 9 (`MapDressing.java:147-151`): at most 24 cells.
- Per army junction (lane head at x = +-205 on each lane, plus Kingsfield's flank junctions): a crescent of up to 36 cells at each of two radii (11 cover with top, 12 coarse dirt) on angles -70 to 70 degrees step 4 facing the enemy, a rally banner, three supply props, a campfire on a 2-high cobble stack (`MapDressing.java:179-189`).
- Per map set pieces (`MapDressing.java:259-288`): Rivergate 8 piers, a well, hay stacks; Ironhold 70 rubble tries plus a stake line at x = +-90 and a trebuchet each side at (+-112, +-40); Frostmarch 4 wagons and 28 windbreak logs; Hollowwood 6 log stacks, boardwalks within 40 of the river, mushrooms by every third tree; Kingsfield hedgerows and 2 windmills at (-62,-200) and (62,200).
- Woodland as cover: candidates on a 13 by 13 grid over x -325..325, z -225..225, kept when a hash is below 22 of 97 (Rivergate, Ironhold, Kingsfield), 13 of 97 (Frostmarch) or 55 of 97 (Hollowwood), then removed within 10 of a road, 17 of the river, 9 of a structure footprint and 30 of a point (`CampaignMaps.java:98-102`). Tree height 7 to 9 and width 5 (10 to 14 and 7 on Hollowwood); spruce on maps 2 to 4, oak otherwise.
- Landmark cover lines (section 7.2) record the intended cover of each authored piece.

Standard proposed from the existing checks (no new numbers): a prop is open ground only if `MapDressing.free` is true (inside |x| <= 346 and |z| <= 246, off roads by the shoulder, off the river by 9 (12 on Hollowwood), off structures by 1, off muster lanes, off trees, and level within 1 block on all four sides; `MapDressing.java:51-55`). Any overlay edit that adds cover must keep that rule. The target density per point and per lane is Q7.

## 7. Sight-line rules

### 7.1 Shared sight

- Team sight range 36 blocks, memory 10 s, refresh every 10 ticks, server-side team line of sight (`sheets/design.json` key `scouting`).

### 7.2 Authored lines

- `landmarkLines` rows give, per piece, one `cover` line and one `sight` line in the piece's local coordinates (22 rows, 11 pieces: the mill, gate, keep, lodge, chapel, outpost, and the five `supply/<map>` houses; `sheets/design.json` key `landmarkLines`). A toY of -1 means the ground at the line end plus 2.6 (`Phase3Checks.java:3441`).
- Binding verdicts (`Phase3Checks.java:3317-3322,3444-3449`): a `sight` line is open (a ray with the COLLIDER shape does not hit a block, or hits within 0.3 of the end); a `cover` line is blocked (a block is hit more than 0.3 before the end). The world end points must equal the stated points of the plan (`stated`, `EXPECTED`, or `houseLine` for supply houses), so moving a piece or editing a line fails.
- Supply houses (`Phase3Checks.java:3308-3312`): at rotation 0 the cover line runs from (x+9.5, e+2.6, z+16.5) to (x+9.5, ground+2.6, z+30.5) and the sight line from (x+10.5, e+7.6, z-7.5) to (x+1.5, ground+2.6, z+30.5), with e the house elevation; at rotation 180 the mirror. The flag of a house must be visible from the lane end (pole `housePole(e)` = 11 + max(0, (e-75)*4/3); `Arena.java:318-321`).
- The check also proves it can fail: `p3-lm-supply-lines-control-map<N>` puts a stone in the sight line and removes the blocking cell of the cover line and wants both verdicts to flip (`Phase3Checks.java:3345-3376`).

### 7.3 Generator-level sight rules (current)

- Banners and lamp posts are posts at head height, not walls; lane standards stand behind each squad lane so a commander reads the line from the dais (`MapDressing.java:90-91`). Obelisks, 9 high, mark each point (`MapDressing.java:152-157`).
- The signal tower on Frostmarch carries a fire at its top (`MapDressing.java:271`). Outposts are raised 10 (elevation 74) with corner lookouts 7 high (`MapDressing.java:166-168`).
- The `sight-` pictures of the walkthrough take the two longest sight lines of each map at render distance 28 (`Phase3Checks.java:1782`). They are photographs and notes, not pass or fail.

Not defined anywhere: a maximum open sight length across a lane, a minimum number of blockers on the longest line, or fog and canopy rules (Q8).

## 8. Landmark rules

### 8.1 Sheet and preflight (binding)

A landmarks row names a piece (`template`), a map, x and z, a rotation of 0, 90, 180 or 270, an elevation, the generator row it replaces (or `-`) and the point it encloses (or `-`) (`sheets/design.json` key `landmarks`).
- Elevation is 64 to 90 (`tools/preflight.py:438`). The foot shoulder is 10 + 2 (elevation - 64) blocks (`Arena.java:120-121`), so a raised landmark meets the plain in a slope a squad can walk; a house also keeps the 12 + 2 (elevation - 64) gap to other landmarks (`tools/preflight.py:478-480`).
- Footprints may not overlap another landmark on the same map (`tools/preflight.py:445-448`). `encloses` is a point id of the map (A to C, or A to E on Kingsfield) (`tools/preflight.py:442-443`).
- A landmark that replaces nothing or a row already replaced is logged (`CampaignMaps.java:118`).

### 8.2 Placement checks (binding, per row, `Phase3Checks.java:3383-3475`)

`p3-lm-<id>-placed` (placed, none missing, replaced row gone, landmark row present); `-fits` (the placed box equals the footprint and lies inside |x| <= 350, |z| <= 250); `-clear` (every structure-void cell at local y 1 and up is air; the flag of an enclosed point and the flag column of a house excused); `-roads` (no through-road cell and no blocked deck cell over the footprint); `-ring` and `-gates` for an enclosing landmark (section 5), or no template column within 19 of a point; `-cover` and `-sight` (section 7.2); `-openings` (the passage, gates and crenel boxes stay free of collision shapes); and the reach goals (walkers can enter and reach the listed interior goals from outside the footprint + 4; `Phase3Checks.java:3460-3475`). Placement time is recorded and a row over 100 ms is listed (`Phase3Checks.java:3391`).

### 8.3 Supply houses

Two rows per map side except Kingsfield (counts 4, 4, 4, 4, 2; `tools/preflight.py:456`). Yard centre is 6 from the piece centre toward the front gate (`tools/preflight.py:495`); half-turn symmetric partner on every map (`tools/preflight.py:507-510`). A spur of width 4 runs from the yard to the lane (`CampaignMaps.java:93`).

| Map | Elevation | Yards (x, z) | Spur end |
|---|---|---|---|
| Rivergate | 74 | (-125,-66), (-125,66), (125,-66), (125,66) | (+-125, 0) |
| Ironhold | 77 | (-140,-66), (-125,66), (125,-66), (140,66) | (+-140 / +-125, 0) |
| Frostmarch | 80 | (+-125, +-66) | (+-125, 0) |
| Hollowwood | 74 | (+-125, +-66) | (+-125, 0) |
| Kingsfield | 76 | (-125,-66), (125,66) | (+-125, 0) |

(Source: `sheets/design.json` key `supplyHouses`.)

### 8.4 Style notes in the sheet

`landmarkStyle` fixes four features: hamlet (framed windows, chimneys, clear doorways), gatehouse (breach, tall hanging banner, broken curtain; approached from the front and the flank), shrine (broken pillars, fallen stone, altar; clear central approach), ford (solid causeway, water margins, natural banks; six-block clear centre).

## 9. Spawn rules

Current on Rivergate, Frostmarch, Hollowwood and Kingsfield (`CampaignMaps.java:106`, `sheets/design.json` key `factions`): team 0 spawns at (-283, 0), team 1 at (283, 0). Ironhold's signed layout (pick D1a) moves them to Dawn (-314, 0) and Dusk (128, 0) (see the Ironhold exception below; the shipped game keeps +-283 until adoption); barracks at (-286, 32) and (286, -32). Team camps: gate (+-314, 0), 20 by 72, height 16; barracks (+-286, -side*32); stores (+-287, side*40) (`CampaignMaps.java:59-61`). The muster lane is the strip from 2 behind the spawn to 24 in front and 40 either side (`MapDressing.java:34`); nothing is placed on it and `free` returns false there (`MapDressing.java:53`). The parade ground is 19 behind to 25 in front, 63 either side, paved and edged in trim, with a palisade at 64 (`MapDressing.java:80-81,93-95,204`). Two command banners 9 high stand behind the dais and four lane standards 6 to 7 high in front (`MapDressing.java:87,91`). The first road is the camp fan of width 5 to the lane heads at (+-205, z) (`CampaignMaps.java:40-42`).

Standard:
- Spawns are half-turn symmetric (x -> -x, z -> -z) on every map, and the distance from a spawn to its nearest lane head (78 blocks along x) is the same on every map. No map is faster or slower on purpose (Kort, 2026-10-22).
- Ironhold exception (record, 2026-11-02): the signed Ironhold layout (pick D1a, 2026-10-30) puts Dawn at (-314, 0) on the camp shelf at y 72 and Dusk at (128, 0) in the Donjon Court, so Ironhold's spawns are not half-turn symmetric and its spawn-to-lane-head distances are about 315 (Dawn) and 130 (Dusk). The 78-block rule above holds on the other four maps. The shipped game still has Ironhold at (-283, 0) and (283, 0) until the rebuilt map is adopted (plan task 2A.5). `docs/maps/ironhold/scan-2026-11-02.md` has the scanned numbers.
- Agents do not move spawns or capture points without Kort's approval and a recorded undo (`docs/PLAN-TO-1.0.md` 7.3). An overlay may dress the camp but may not change `FACTIONS` rows.
- The camp must keep: an open muster lane, a clear exit toward the lane head, and the road open from the muster point to the lane head (the through-road and base counts of the walk scan, section 10.1).

## 10. What the suites must assert

The fast gate does not run these; they are game-client suites launched through the Phase 3 harness (`Tests: Phase3Checks.java:52-57` registers `routes`, `walk`, `walkthrough` and `campwalk`; see `docs/SETUP.md` for the runner).

### 10.1 Walk suite (`walk`, `Tests: Walkability.java`, `Phase3Checks.java:549-617`)

Per map, over every road cell, every bridge deck cell (the planks) and every capture-ring cell, a cell is blocked when a block with a collision shape stands at `Arena.ground` + 1 or + 2 (`Walkability.java:34-35`). Blocked cells are classed once each: road, base (inside a camp gate, barracks or stores), building (inside any other footprint), through (a building cell that a road of width 4 or more covers while neither end lies in that footprint), point (a ring cell that is not a road cell). The flag pole at the exact centre of a point counts as point, not road (`Walkability.java:15,51-55`); a landmark's enclosed ring belongs to the arena (`Walkability.java:64-71`).

Binding assertions:
- `p3-walk-map<N>` passes when `road == 0` and `through == 0`; building, base and point counts are in the line (`Phase3Checks.java:595`). The blocked cells are written to `screenshots/p3-walk-map<N>-blocked.txt`.
- `p3-walk-rule-map<N>` proves the classification can fail: a stone put on a ring cell that is also a road cell counts as road, on a ring cell off the roads as point, and on a road cell outside every footprint as road (`Phase3Checks.java:601-617`).
- Each landmark: `-roads` (section 8.2).

Add (proposed, from numbers already in the code): the same scan with `base` and `point` also expected to be zero on a camp's muster road, so a stone on a lane head is caught; today `base`, `building` and `point` are reported and not failed.

### 10.2 Route suite (`routes`, `Tests: MapRouteChecks.java`)

- Bridge mode: for each map and each of its three crossings, a squad of 20 knights placed 14 blocks west of the deck (`MapRouteChecks.java:117`) is ordered across. The crossing passes when all 20 have passed the east end + 4 and are at y above 63 within 1200 ticks (`MapRouteChecks.java:96-99`). `p3-routes-map<N>` passes when 3 of 3 crossings pass (`Phase3Checks.java:639`). Setup requires exactly 20 living knights and no foreign troops (`MapRouteChecks.java:90`). Reported as `all-map-navigation`.
- House mode: for each supply house two goals, a march from the faction spawn to the yard and a march from 12 blocks out of the nearest point toward the yard, each with 3600 ticks and an arrival of 10 blocks (`MapRouteChecks.java:127-141`). Hollowwood's `hw-supply-se` also runs the pocket goals `nook-even`, `nook-odd` (1200 ticks each, the controls for the chapel nook at (81,102)) and `nook-far` (an order 200 blocks past the pocket); the pocket walls must still exist or the goal fails (`MapRouteChecks.java:30-40,138`). Reported as `all-house-navigation`.
- Stuck handling the goals exercise: a unit that moves under 0.03 blocks for the stuck time is hopped, then detoured with a wedge counter (`Tactics.java:288-307`); the stuck time is 3 s, repath every 20 ticks, search range 40, waypoint distance 16, bridge entry inset 2, arrival tolerance 0.35 (`sheets/design.json` key `navigation`).

Required for each new or adopted map (the standard): all 3 crossings or the map's equivalent crossings at 20 of 20, every supply house at 20 of 20 from both starts, and a goal for any pocket the walk suite finds (a nook with walls on three sides is a regression test, not a one-off).

### 10.3 Walkthrough and camp walk (`walkthrough`, `campwalk`, `Phase3Checks.java:1780-1790,2508-2520`)

A real client walker holds the movement keys along lanes (camp, lane1..3, flank, link, spur, band, rail, posts, wall, hall, plaza, edge, outpost, ramp, float, building, sight) and photographs stops. Binding: `p3-wt<N>-leg-<id>` passes when the walker reached the end and no 2 s window gained less than 3 blocks (`stuck == 0`); `p3-wt<N>-stuck` is the total; `-foreign` (no column's top block belongs to another map); `-river`; `-floating` (every solid block touches a chain that reaches the floor). For the camp, `legOk` also wants no forced step (`Phase3Checks.java:2550`).

### 10.4 Bot path checks

There is no suite that asserts bot paths today. `BotSimulation` plays bot-against-bot rounds on a dedicated server across `-PsimMaps` and writes `botsim/results.csv`; it records winners and fight statistics and does not assert that a bot reached a point (`Tests: BotSimulation.java:1-25`). The tactical-AI constants are in `sheets/design.json` key `navigation`. Standard to add (Phase 3.1 row, `docs/PLAN-TO-1.0.md`): on every map, a squad ordered from each spawn must reach each capture point within a bounded time, using the same stuck and arrival rules as the route suite, and a full bot round on the map must reach a capture on every point. The time bound is Q9.

### 10.5 Gate wording

A map is accepted only when walk, route and (when added) bot path suites pass on the final blocks and the placement checks of section 8 pass for every landmark row. The checks that prove they can fail (`walk-rule`, `supply-clear-control`, `supply-lines-control`) must stay in the suite.

## 11. Change control

- Row changes go through `sheets/design.json`; run `tools/preflight.py` to regenerate `Spec.java` and `sheets/preflight.json`.
- A change to how the arena lays ground, roads or ring (not to a single sheet row) bumps `LayoutMarker.MARGIN_BASE` or `Structures.PASS` so caches rebuild (`LayoutMarker.java:12-13,17`).
- Kort approves a House, map or landmark before export; an agent may not delete a region, change a station, move a spawn or a capture point, or overwrite a live world without a backup and a recorded undo (`docs/PLAN-TO-1.0.md` 7.3).
- Do not move points, rivers or bases inside Phase 3 (memory `kort-map-vision`); layout changes belong to the redesign spec.

## 12. Questions for Kort

Where the code and Kort's 2026-10-22 vision disagree, or a number is undecided. None of these were answered by an agent.

Code against vision:
1. Ironhold is "a real siege with a walled fortress on one side and an intact looming keep". The code is half-turn symmetric: curtain walls at (+-72, +-62) on both sides, the keep and gate at the middle (`CampaignMaps.java:53,74`; landmarks `ih-keep`, `ih-gate`, `ih-curtain-*`), and a river at x = 0 through the keep. Should Ironhold stay symmetric for fairness, or put the fortress on one side?
2. Hollowwood should be "eerie, very dense" with "giant sequoias". Trees are at most 14 blocks and 7 wide (`CampaignMaps.java:102`) with 56.7 percent of grid cells tried (`CampaignMaps.java:98`). What height, width and count count as giant, and should it be a landmark piece or scenery?
3. Frostmarch should have "signal tower beacon plus cave refuge". The signal tower and its fire exist (`MapDressing.java:271`); there is no cave (only the `pass_refuge` house). Add a cave piece? Kingsfield should have "fortified high-ground outposts, a watchtower network and firework flares". The outposts exist (two, elevation 74), the four towers are the same on all maps (`CampaignMaps.java:63`), and there are no flares. Is the network the four standard towers, or new towers, and should flares be a mechanic or a prop?
4. Kingsfield is described as "two fords, a central bridge" (`sheets/design.json` key `maps`), and point A is "North Ford", but the crossing generator gives A a wooden bridge, B stone and C a ford on every map (`CampaignMaps.java:39`). Which crossing should each Kingsfield point have?
5. House palette: should the arena carry per-House blocks or banners? The sheet's faction rows have gold / violet colours (`sheets/design.json` key `factions`: gold, violet, crimson, teal) while the arena dresses team 0 cyan and team 1 purple everywhere (`MapDressing.java:71`, `Arena.java:324`). Which is canonical?

Undecided numbers:
6. Capture circle size: radius 17 and 8 s on all maps. Should a map vary it (Ironhold, Kingsfield outposts, Hollowwood)?
7. Cover density target per capture point and per lane (section 6 has only what is laid now).
8. Sight-line limits: longest open line across a lane, and whether Hollowwood canopy and Frostmarch snow count as blockers.
9. Time bound for a bot to reach each point, and the pass rate for a full bot round (section 10.4).
10. Road widths: are the four widths of section 4 (3, 4, 5, 6 as half-widths) the final set, and should the army approach stay 5 on every map or differ (Kingsfield wider, Hollowwood narrower)? No part of the vision answers this.
11. Is the spawn-to-lane-head distance (78 blocks) and the spawn position (+-283, 0) fixed for all five maps in the redesign, or may the redesign change them per map?
12. The name `width` in the code is a half-width; rename in the sheet at adoption, or keep and document? (Section 4 documents it.)
