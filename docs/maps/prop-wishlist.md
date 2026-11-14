# Maps lead: prop and block wish list (Kort asked for this, 2026-11-13)

Kort's words: "make a list of any props or new blocks/objects you think would be useful when filling out the maps and designing them and decorating them, send it to orchestrator to delegate the task."

From building Kingsfield, Rivergate and Ironhold out of plain blocks. Nothing here repeats `docs/props/PROP-LIST.md` (stone_hand, stake_barricade, drowned_boat, mooring_post, fish_rack, ore_cart, supply_crates, quarry_winch, cut_stone, waystone, gabion_wall, mantlet, siege_ladder, objective_plinth, training_dummy and the lobby set), but several of those are wanted on the maps too: ore_cart, supply_crates, mantlet, siege_ladder, gabion_wall, training_dummy, mooring_post.

## What the maps need from every prop (technical asks)
1. Placed as blocks (or block entities), not mobs, so they travel inside the map section templates (`place template`), which carry blocks only.
2. A stated collision for each: either a full cube per cell or none at all. Half-height or odd shapes break troop pathing and my map checks (the non-full-block inventory and the Rivergate water seal). If a prop has collision, troops must not be able to stand on it unless it is a full cube.
3. Four facings at least; eight for long things (wagons, boats, trebuchet).
4. Team-coloured props take the colour from a block state (teal / purple / neutral), not from separate ids.
5. No fire damage, no light flicker that sets things alight. Light level as a block state where it glows.

## A. Highest value (vanilla blocks do these badly; I faked each one this week)
| id | size (w x h x d) | maps | why |
|---|---|---|---|
| `trebuchet` | 5 x 9 x 13 | Ironhold camp, Kingsfield | My block version is a stick figure. A real model with wheels, winch, sling and weight box is the siege camp's centrepiece. Flagship. |
| `supply_wagon` | 3 x 3 x 6 | all | Blocks cannot make wheels. Variants: empty, hay, barrels, stone shot, covered (canvas tilt). |
| `handcart` | 2 x 2 x 3 | towns, mine, camp | Small two-wheel cart; variants empty, ore, sacks. |
| `tent_soldier` | 5 x 4 x 7 | camps, base yards | Wool tents look like stairs. Sagging canvas, guy ropes, open flap; team colour state. |
| `tent_pavilion` | 9 x 8 x 9 | camps | Commander's tent with a striped roof and pennant; walk-in, so collision only on poles and walls. |
| `giant_bone` set | 3 x 3 x 6 pieces | Rivergate ribs, any | Rib segment, vertebra, skull fragment, knuckle. The ribs are the map's landmark and are built from bone blocks now. Fits the giants lore. Flagship. |
| `boulder` set | 1 to 3 cubes | all | Rounded rocks in three sizes and three stones (grey, dark, mossy). Vanilla rock is cubes. |
| `market_stall` | 3 x 3 x 2 | Rivergate, Kingsfield | Awning, counter and goods; variants fish, bread, cloth, pots. |
| `weapon_rack` | 2 x 2 x 1 | barracks, keeps, camps | Spears and swords on a frame; wall and free-standing. I used fences and iron bars. |
| `brazier` | 1 x 2 x 1 | walls, gates, camps | Iron basket of coals on legs, glowing, harmless. I cannot put real fire near walkways. Standing and wall-hung. |
| `portcullis` | 1 x 1 x 1 tiling | gates | Heavy iron grille that tiles across a gateway, raised or lowered look. Iron bars read as a window. |
| `smoke_vent` | 1 x 1 x 1 | forges, chimneys, volcano | Invisible block that makes chimney smoke (state: thin, thick, black). I hide lit campfires inside chimney tops now. |

## B. Siege and fortress (Ironhold first, reusable)
| id | size | why |
|---|---|---|
| `battering_ram` | 3 x 3 x 7 | Covered ram on wheels for the gate approach and the breach. |
| `siege_tower` | 5 x 12 x 5 | A wheeled tower standing off the wall; scenery, not climbable. Flagship. |
| `shot_pile` | 1 x 1 x 1 | Pyramid of stone shot beside the trebuchets. |
| `arrow_barrel` | 1 x 1 x 1 | Barrel of arrows / bundle of javelins for wall tops. |
| `pavise` | 2 x 2 x 1 | Standing archer's shield; cover (gameplay is Kort's call). |
| `winch_drum` | 2 x 2 x 2 | Gate and mine winch with rope. |
| `sheave_wheel` | 3 x 3 x 1 | The big spoked wheel on the mine head frame. |
| `bellows_forge` | 3 x 2 x 2 | Hearth with bellows and a glowing bed, harmless. |
| `crucible` | 2 x 2 x 2 | Pouring pot on a frame with a molten glow; smelter interior. |
| `ingot_stack` | 1 x 1 x 1 | Stacked iron pigs. |
| `scaffold_frame` | 2 x 3 x 1 | Builders' scaffolding for wall repairs at the breach. |
| `rubble_heap` | 2 x 1 x 2 | Fallen masonry in the wall's stone; full-cube collision only. |
| `chain_hoist` | 1 x 4 x 1 | Crane arm with hook and chain for quays, stores, mine. |
| `great_banner` | 1 x 6 x 3 | A tall war standard, team colour state, for towers and yards. I build flags from wool. |
| `lava_crack` | 1 x 1 x 1 overlay | Glowing fissure decal on rock for the volcanic rim; no damage. |

## C. Camp and soldier life
| id | size | why |
|---|---|---|
| `camp_fire_pot` | 2 x 2 x 2 | Tripod, pot and embers; harmless glow. |
| `bedroll` | 1 x 1 x 2 | Rolled or laid blanket with a pack. |
| `war_drum` | 1 x 2 x 1 | Drum on a stand. |
| `cook_spit` | 2 x 2 x 1 | Spit with a roast. |
| `water_butt` | 1 x 2 x 1 | Barrel on a stand with a ladle (water with no fluid in it). |
| `stacked_shields` | 1 x 1 x 1 | Shields and helmets leaned together. |
| `horse_tack` | 1 x 2 x 1 | Saddle rack and bridles for stables. |
| `hay_rick` | 2 x 2 x 2 | Rounded stack; vanilla bales are cubes. |

## D. Town and river (Rivergate, Kingsfield)
| id | size | why |
|---|---|---|
| `well_head` | 3 x 4 x 3 | Roofed well with windlass and bucket, capped: no water a troop can fall into. |
| `rowboat` | 2 x 1 x 4 | Moored or beached, whole (the list has only the drowned one). |
| `quay_crane` | 3 x 6 x 3 | Treadwheel crane for the Rivergate quays. Flagship. |
| `net_rack` | 3 x 2 x 1 | Fishing nets drying. |
| `shop_sign` | 1 x 1 x 1 | Hanging sign on a bracket: anvil, fish, loaf, boot, tankard, scales. |
| `street_lamp` | 1 x 3 x 1 | Lamp post; wall bracket variant. |
| `bench` | 2 x 1 x 1 | Plank bench and a stone bench. |
| `trough` | 2 x 1 x 1 | Horse trough, looks full, no fluid. |
| `sack_pile` | 1 x 1 x 1 | Grain sacks; mill and stores. |
| `barrel_stack` | 2 x 2 x 1 | Racked barrels on their sides. |
| `ladder_leaning` | 1 x 3 x 2 | Scenery ladder against a wall or cart. |
| `wheelbarrow` | 1 x 1 x 2 | |
| `shrine_idol` | 1 x 2 x 1 | Small wayside shrine figure with offerings. |
| `bell_frame` | 2 x 3 x 2 | Alarm bell on a timber frame for squares and gates. |
| `washing_line` | 4 x 2 x 1 | Between houses; makes streets lived-in. |
| `flower_box` | 1 x 1 x 1 | Window box that sits against a wall. |

## E. Ground and nature
| id | size | why |
|---|---|---|
| `dead_tree` | 3 x 6 x 3 | Bare, scorched trunk with limbs; the Ironhold valley and Hollowwood. |
| `stump_log` | 2 x 1 x 2 | Stumps and felled logs with an axe. |
| `reed_clump` | 1 x 2 x 1 | Reeds for river banks, no collision. |
| `pebble_scatter` | 1 x 1 x 1 overlay | Loose stones on roads and scree, no collision. |
| `bone_pile` | 1 x 1 x 1 | Small bones and a skull. |
| `cart_ruts` | 1 x 1 x 1 overlay | Wheel tracks on dirt roads, no collision. |

## F. Blocks that would change how maps are built (each needs Kort's ruling; gameplay)
| id | what | why |
|---|---|---|
| `troop_fence` | Invisible block: stops troops and players walking through, lets arrows and sight pass. | Would guard water edges, cliff tops and map borders without stacking wall posts. Rivergate needed 422 extra guard blocks to keep troops out of the river. |
| `still_water` | A water-look surface block with no fluid: cannot be swum in, drowned in or pushed by. | Rivers and moats as pure scenery. Kort ruled troops must never get into water. |
| `ford_stones` | Walkable stepping-stone surface over still_water. | A real crossing without a bridge. |
| `team_banner` | Banner that takes the colour of the side holding the nearest point or spawn. | Walls now carry fixed purple banners even if the attackers take them. |
| `spawn_gate` | Barred gate that opens when the match starts. | Tidy spawn rooms. |

## Suggested order
1. Section A (12 props): they replace my weakest block builds and are needed on all three maps.
2. `troop_fence` and `still_water` rulings, because they change how Ironhold's cliffs and the last three maps are designed.
3. Section B for Ironhold, then D for the two town maps, then C and E as filler.

Where I would place them first: Ironhold camp (trebuchet x2, wagons, tents, shot piles, great banners), Ironhold walls (braziers, portcullis, weapon racks, arrow barrels), forges (bellows, crucible, smoke vents, ingots), mine head (sheave wheel, ore carts, handcarts), Rivergate (giant bones, stalls, rowboats, crane, signs), Kingsfield (wagons, well, stalls, boulders).
