# Hollowwood build plan ("The Hollow")

Status: **full map built, 2026-11-14** (`gen/hwbuild.py`, self-check OK, 54 sections in `test-tools/resources/data/ggmaps/structures/hw/sec/`). Kort approved sketch 3, the blockout, the Yoke (v2) and both stumps. Awaiting the screenshot review and Kort's look at the whole map. Where this file and `gen/hwmap.py` disagree on a coordinate, hwmap.py is right (the Root Run gully and ramp angles moved when the two ways down were evened out).

Kort's rulings (2026-11-14, AskUserQuestion): sketch 1 (ring track, three points in a line) rejected: "Let's not make it seem like all the other maps, let's make it interesting and fun". Marsh is a "Dry bog, water out of reach". Canopy "Open over roads and glades". Sketch 2: "Good, but go wilder"; root bridges "Yes, walkable"; fallen trunks "Through hollows only". Sketch 3: "Yes, build this"; High Walk "5 wide with rails". Earlier rulings that still hold (2026-11-12): giant-trunk forest; relic is the Yoke (a giant's collarbone arched over the shrine, no face); light late dusk; Survival is the home mode; escort is "The Offering" with the middle stop on point B; Rivergate size.

## 1. Fixed numbers
- Build box x -288..287, z -192..191 (576 x 384). Sections A..I from x -288, rows 1..6 from z -192, 64 x 64 each, 54 in all.
- Half-turn symmetric: every cell (x, z) has its twin at (-x, -z). The generator builds Dawn's half and turns it.
- Heights (map y of the ground block; test world adds 135): forest floor 72; the Hollow floor 62 (10 down); stump tops, the High Walk, bridges and boardwalks 78 (6 up). Trunks to about 125, canopy to 140.
- Spawns: Dawn (-236, 124), Dusk (236, -124), both at 72. Camps in opposite corners.
- Capture points, radius 17, flat, nothing standing in the ring:
  - A (-128, -96) at 78: the Great Stump.
  - B (0, 0) at 62: the Hollow floor, the shrine under the Yoke (the Yoke's feet stand outside the ring).
  - C (128, 96) at 78: the Drowned Stump.
- Climbs are full-block steps, one at a time. Roads 7 wide, paths 5. High Walk decks and bridges 5 wide with full-block rails each side (7 over all).
- No water a troop can reach; the bog is mud, moss and rooted dirt with pools sunk behind a 2-high lip. No lit fire in reach. Rails, steps and deck edges are full blocks.

### Fairness note (maps lead)
A and C must be the same shape, because Dawn's near point is A and Dusk's is C. So C is a second giant stump with the same foot, rim and openings as A, standing rotted in the dry bog, with the stilt huts built on and round its rim. A is dressed as the loggers' stockade, C as the marsh stilts. Collision is identical; only the dressing differs.

## 2. Levels and routes (the point of this map)
Three levels cross each other:
1. **The floor of the wood (72).** Roads, the two Fallen Giants, the bog.
2. **The High Walk (78).** Plank decks round giant trunks. From each stump top to the nearest bridge end, a gallery along the west and east lips joining the two bridges, and a spur out to a stair tree on each team's rim path. Ways up: the two stair trees, both stump tops.
3. **The Hollow (62).** A round pit, radius 60 at the lip, floor radius 44. Ways down: two spiral ramps along the wall (7 wide), and the two Root Runs (a gully off each long road, then a root tunnel 5 wide and 4 high through the pit wall).

Roads (Dawn's half; Dusk's is the twin):
- Near road: camp > A, winding, about 250.
- Rim path: off the near road at (-196, 20) > west lip of the Hollow (-60, 0); the stair tree stands on it at (-150, 24).
- Long road: camp > south of the Hollow > C, about 370. The Root Run leaves it at (-60, 112).
- Stump path: A > north-west lip (-38, -46).
- The Fallen Giant lies between the near road and the long road, from (-230, -20) to (-96, 84), 11 thick, with two hollows (5 wide, 4 high) at (-176, 28) and (-118, 70).

Ways across the middle: down a ramp, over the floor and up the other ramp; over either root bridge; round the lip; through a Root Run and out by the other.

## 3. Mode sites
- Domination: A, B, C.
- Survival (home mode): hold the shrine on the Hollow floor. Rise sites: the far ends of the two Root Runs, the two stair trees, and the two Fallen Giant hollows nearest the camps.
- Siege: the Great Stump. Yard = the near road below the south split; gates = the two splits in the bark rim (south-west and east); keep = the sawmill on the stump top by point A.
- Escort "The Offering" (Dawn escorts): start, A's east split (-96, -90) > stop 1, north-west lip (-44, -50) > stop 2, the shrine (0, 0) by the north-west spiral ramp > stop 3, south-east lip (44, 50) by the south-east ramp > end, C's west split (96, 90). The ramps are 7 wide for the wagon and clear of props.

## 4. Palette
- Ground: podzol, coarse dirt, rooted dirt, moss block; packed mud and mud in the bog; roads of dirt path look done in coarse dirt and packed mud (full blocks only).
- Trunks: dark oak and spruce logs and wood, stripped faces where bark has fallen; roots of mangrove roots look done in dark oak wood and rooted dirt (full blocks). Canopy dark oak and spruce leaves, with azalea and flowering azalea leaves as light specks; open above roads, glades, the Hollow and the stumps.
- The stumps and Fallen Giants: stripped dark oak and spruce wood for the cut faces with growth rings, bark of dark oak wood, moss and mushroom-block shelves on the rotted one.
- The Yoke: bone block, calcite and smooth quartz, weathered with diorite. No face.
- Decks: spruce and dark oak planks; rails stripped spruce logs; rope look from chains only out of reach.
- Light: late dusk. Lanterns under decks and in hollows; shroomlights and glow lichen-free amber glass in the canopy; `brazier_tall` props only where no troop can reach the fire block.

## 5. Pieces (each built off-map, photographed, checked, then pasted; Kort sees the key ones first)
| Piece | Where | Notes |
|---|---|---|
| The Yoke and shrine | Hollow floor (0, 0) | The landmark. Collarbone arch about 36 across and 18 high, feet outside the ring |
| The Great Stump | A (-128, -96) | Stump 40 x 34 radius, top at 78, bark rim to 84 with two splits; sawmill and stockade dressing |
| The Drowned Stump | C (128, 96) | Same foot turned; rotted, mossed; stilt huts and boardwalks on the rim |
| Fallen Giant (2) | see routes | 11 thick trunk with root plate at one end and two hollows |
| Giant trunks (4 kinds) | the whole wood | Radius 4..7, buttress roots, not climbable |
| Deck tree, stair tree | the High Walk | Round deck with rails; stair tree has a 5-wide plank stair of full blocks |
| Root bridge (2) | over the Hollow | 5 wide, gently arched, root rails |
| Root tunnel mouth (2) | pit wall | 5 wide, 4 high |
| Camps (2) | (-236, 124) and twin | Palisade of logs, team-colour tents and pavilion, supply wagons |

Props through anchors, as on Ironhold and Frostmarch. No block-built stand-ins for props that exist.

## 6. Order of work
1. Blockout (`gen/hwmap.py`): heights and routes only, with the walk check. Kort sees a picture.
2. Key pieces, pictures to Kort before paste: the Yoke and shrine, the Great Stump, a Fallen Giant, a deck tree and stair tree.
3. Full build (`gen/hwbuild.py`), self-check, 54 sections, in-game pictures, non-full-block inventory.
4. One Sonnet (high) screenshot review, at most two fix rounds, then Kort.
5. Hand-over note and a one-commit landing branch on main for the coordinator.
