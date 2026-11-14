# Frostmarch build plan ("The Switchback")

Status: **draft, 2026-11-14.** Shape approved by Kort on sketch 1 (`gen/layout.py`, picture `layout-sketch1.jpg`). Nothing is built yet.

Kort's rulings (2026-11-14, AskUserQuestion): Frostmarch next; keep the Switchback; equal sides; ice is scenery only; "Yes, plan from this" (sketch 1); the Spine is a "Wall with small gaps"; the wagon run is "About 420, inside the S". On the blockout (`blockout1.jpg`): "Keep the ridges" (a rock ridge each side between the wagon road and the low road, with a cleft through it); corners are "Pine woods and a small hold"; the tower is a "Stone beacon tower". Earlier rulings that still hold (2026-11-12): home mode Convoy; three wagon stops with the middle one on point B; relic is the spine; Rivergate size; no water troops can reach.

## 1. Fixed numbers
- Build box x -288..287, z -192..191 (576 x 384). Sections A..I from x -288, rows 1..6 from z -192, 64 x 64 each, 54 in all.
- Half-turn symmetric: every cell (x, z) has its twin at (-x, -z). The generator builds the west half and turns it.
- Heights (map y; test world adds 135): camps 64; the road climbs to 72 at the Cut; shelves 80; mountain rim 100..135.
- Spawns: Dawn (-256, 0), Dusk (256, 0), both at 64.
- Capture points, radius 17, flat, nothing standing in the ring:
  - A (0, -112) at 80: the Signal Shelf.
  - B (0, 0) at 72: the Spine Cut.
  - C (0, 112) at 80: the Refuge Shelf.
- Walks (targets, `check` must print them): both spawns to B equal (about 300 by road); Dawn to A equals Dusk to C; Dawn to C equals Dusk to A; the near and far shelf walks within 10 % of each other.
- Climbs are full-block steps, one at a time. Wagon road 9 wide; side roads 5; shelf ramps 10 wide, 34 long for 8 up.
- No water, no ice, no powder snow, no snow layers anywhere a troop can stand. Ground is snow blocks, stone, gravel and packed dirt. Ice only in tarns and falls behind rock or a 2-high lip.

## 2. Mode sites
- Domination: A, B, C.
- Convoy (Dawn escorts, about 420 along the road): start (-176, -44) > stop 1 first bend (-100, -72) > stop 2 the Cut (0, 0), the exact middle > stop 3 second bend (100, 72) > end (176, 44). The road is clear of props and non-full blocks for its whole width.
- Siege: the signal tower. Yard = west ramp foot (-92, -112), gate = shelf gatehouse at the ramp head (-58, -112), keep = tower court by point A. East ramp shut for the round.
- Survival: hold the refuge at C; rise sites on the low roads below both ramps, at the south Spine gap and at the back of the refuge.

## 3. Palette
- Ground: snow block, with stone, andesite and gravel showing through on the road and under overhangs; packed dirt (coarse dirt, rooted dirt) in the camps.
- Rock: stone, andesite, tuff, deepslate low down; calcite and diorite snow caps; packed ice and blue ice in falls and tarns (scenery).
- The Spine: bone block, calcite and smooth quartz, weathered with diorite and cobweb-free cracks of tuff. No faces.
- Timber: spruce and dark oak; roofs spruce and deepslate tile under snow blocks. Wool and banners in team colours at the camps only.
- Light: late afternoon, clear. Braziers (`brazier_tall`) at stops, camps and the tower; lanterns under eaves.

## 4. Pieces (each built off-map, photographed, checked, then pasted)
| Piece | Where | Notes |
|---|---|---|
| Vertebra (3 kinds) and the broken pair at the Cut | x = 0, z -84..84 | 11..13 tall, not climbable; gaps 3 wide at z = -48 and z = 48 |
| Signal tower | back of the Signal Shelf (40, -128) | The landmark: tall, a beacon cage on top; Siege keep |
| Shelf gatehouse (2) | ramp heads (-58, -112) and its twin | Open arch 7 wide; Siege gate |
| Refuge (hospice) | back of the Refuge Shelf (-40, 128) | A free-standing stone and timber hospice, same foot as the tower (as Kort approved it); Survival hold |
| Waystation (2) | first and second bend | Wagon stop: stable, woodpile, brazier; off the road |
| Camps (2) | (-258, 0) and twin | Palisade, team-colour tents and pavilion, supply wagons |
| Road shrines, cairns, snow sheds | along the S | Small, for cover and for reading the route |
| Neutral hold plots (2) | tarn shores (-196, -118) and twin | 24 x 24 flat, dressed as ice-fishers' huts until the mechanic exists |

Props through anchors: `tent_soldier_large_dawn` / `_dusk`, `tent_pavilion_large_dawn` / `_dusk`, supply wagons, handcarts, `brazier_tall`. No block-built stand-ins.

## 5. Terrain and lanes
- Wagon road (the S): Dawn camp, first bend (north-west), the Cut, second bend (south-east), Dusk camp.
- Low roads: Dawn camp to the west foot of the Refuge Shelf; twin from the Dusk camp to the east foot of the Signal Shelf.
- Links: first bend to the west foot of the Signal Shelf; a cross path between the two roads round each crag.
- Five ways across the middle: the Cut (wide), over each shelf (ramp up, ramp down), and two narrow Spine gaps.
- Shelves look down on the road but stand back from B: no shooting onto B from a shelf (at least 60 blocks of ground between).
- Cover: dense at the bends and the Cut (bone fragments, boulders, sheds, pines); thin on the open snow between, so crossing it is a choice.
- The mountain rises at once outside the pass: 2 high within 2 cells of the edge.

## 6. Order of work
1. Generator skeleton: heights, pass edge, roads, shelves, points, `check` (walks, rings flat and empty, five crossings each work alone, no reachable ice, water or mountain).
2. Blockout picture for Kort.
3. Pieces in the workshop, photographed; Kort sees the key ones (vertebrae and the Cut, signal tower, refuge).
4. Terrain and dressing, export 54 sections to `ggmaps:fm/sec`, in-game pictures of every section.
5. Non-full-block inventory and a Sonnet (high) review; fixes; Kort's page.

## 7. Needs from other lanes
- Coordinator / modes: Frostmarch spawn, point, Convoy route (three stops), Siege and Survival rows, after Kort approves the built map.
- AI lead: lane waypoints (wagon road, two low roads, five crossings).
