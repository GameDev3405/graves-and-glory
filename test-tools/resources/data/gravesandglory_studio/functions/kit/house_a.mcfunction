# size 15 11 13
# kit/house_a: hamlet cottage: oak boards and daub in a dark oak frame on a cobblestone plinth, a dark oak roof proud of both gables over soffited eaves, shuttered windows, a planked door under a lintel and a hood in the east gable, a chimney on the west gable.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Body x 1..13, z 2..10; walls y 1..4 (a cobblestone sill at y 1, the wall plate at y 4); the roof over x 0..14 (eaves y 4 at z 1 and 11, ridge y 9 at z 6). Door at x 13, z 6 (y 1..2) under a lintel at y 3; doorstep x 14, z 5..7 at y 0.
# The ground layer is the plinth, the floor, the chimney foot and the doorstep: every other cell of y 0 is structure void, so the lane and the ground of the map run up to the walls and the step.
# Windows: east gable z 3 and 9 (y 2..3) and a loft window z 6 (y 5), shuttered; north and south walls x 3 and 11 (y 2), shuttered; west gable z 9 (y 2..3). The way in from the door to the middle of the floor (x 7..12, z 5..7, y 1..2) is kept clear.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~14 ~10 ~0 minecraft:air
fill ~0 ~1 ~1 ~5 ~1 ~1 minecraft:air
fill ~0 ~1 ~2 ~0 ~4 ~4 minecraft:air
fill ~0 ~1 ~8 ~0 ~6 ~8 minecraft:air
fill ~0 ~1 ~10 ~0 ~2 ~12 minecraft:air
fill ~1 ~1 ~12 ~2 ~10 ~12 minecraft:air
fill ~2 ~1 ~3 ~2 ~5 ~4 minecraft:air
fill ~2 ~1 ~8 ~2 ~5 ~9 minecraft:air
fill ~3 ~1 ~4 ~5 ~6 ~8 minecraft:air
fill ~3 ~1 ~11 ~13 ~1 ~12 minecraft:air
fill ~6 ~1 ~3 ~8 ~2 ~9 minecraft:air
fill ~9 ~1 ~1 ~14 ~1 ~1 minecraft:air
fill ~9 ~1 ~4 ~12 ~6 ~8 minecraft:air
fill ~9 ~1 ~9 ~9 ~5 ~9 minecraft:air
setblock ~14 ~1 ~2 minecraft:air
fill ~14 ~1 ~4 ~14 ~1 ~8 minecraft:air
fill ~14 ~1 ~10 ~14 ~1 ~12 minecraft:air
fill ~0 ~2 ~1 ~1 ~2 ~1 minecraft:air
fill ~0 ~2 ~9 ~0 ~5 ~9 minecraft:air
setblock ~1 ~2 ~11 minecraft:air
fill ~2 ~2 ~5 ~2 ~7 ~7 minecraft:air
setblock ~3 ~2 ~1 minecraft:air
fill ~3 ~2 ~3 ~5 ~5 ~3 minecraft:air
fill ~3 ~2 ~9 ~3 ~5 ~9 minecraft:air
fill ~3 ~2 ~11 ~3 ~2 ~12 minecraft:air
fill ~4 ~2 ~12 ~4 ~10 ~12 minecraft:air
fill ~5 ~2 ~1 ~6 ~2 ~1 minecraft:air
fill ~5 ~2 ~9 ~5 ~5 ~9 minecraft:air
fill ~5 ~2 ~11 ~9 ~2 ~12 minecraft:air
fill ~8 ~2 ~1 ~9 ~2 ~1 minecraft:air
fill ~9 ~2 ~3 ~12 ~5 ~3 minecraft:air
fill ~10 ~2 ~9 ~10 ~5 ~9 minecraft:air
fill ~10 ~2 ~12 ~10 ~10 ~12 minecraft:air
setblock ~11 ~2 ~1 minecraft:air
fill ~11 ~2 ~11 ~11 ~2 ~12 minecraft:air
fill ~12 ~2 ~9 ~12 ~5 ~9 minecraft:air
fill ~12 ~2 ~12 ~12 ~10 ~12 minecraft:air
fill ~13 ~2 ~1 ~14 ~2 ~1 minecraft:air
fill ~13 ~2 ~11 ~14 ~2 ~12 minecraft:air
fill ~14 ~2 ~3 ~14 ~5 ~3 minecraft:air
setblock ~14 ~2 ~6 minecraft:air
fill ~14 ~2 ~9 ~14 ~5 ~9 minecraft:air
fill ~0 ~3 ~10 ~0 ~4 ~10 minecraft:air
fill ~0 ~3 ~12 ~0 ~10 ~12 minecraft:air
fill ~3 ~3 ~12 ~3 ~10 ~12 minecraft:air
fill ~4 ~3 ~9 ~4 ~5 ~9 minecraft:air
fill ~5 ~3 ~12 ~9 ~10 ~12 minecraft:air
fill ~6 ~3 ~3 ~8 ~3 ~5 minecraft:air
fill ~6 ~3 ~6 ~6 ~5 ~9 minecraft:air
fill ~7 ~3 ~7 ~7 ~3 ~9 minecraft:air
fill ~8 ~3 ~6 ~8 ~5 ~9 minecraft:air
fill ~11 ~3 ~9 ~11 ~5 ~9 minecraft:air
fill ~11 ~3 ~12 ~11 ~10 ~12 minecraft:air
fill ~13 ~3 ~12 ~14 ~10 ~12 minecraft:air
fill ~6 ~4 ~3 ~6 ~5 ~5 minecraft:air
fill ~8 ~4 ~3 ~8 ~5 ~5 minecraft:air
setblock ~14 ~4 ~2 minecraft:air
fill ~14 ~4 ~4 ~14 ~4 ~8 minecraft:air
setblock ~14 ~4 ~10 minecraft:air
fill ~0 ~5 ~1 ~14 ~10 ~1 minecraft:air
fill ~0 ~5 ~3 ~0 ~5 ~4 minecraft:air
fill ~0 ~5 ~11 ~14 ~10 ~11 minecraft:air
fill ~7 ~5 ~3 ~7 ~5 ~9 minecraft:air
fill ~14 ~5 ~4 ~14 ~6 ~4 minecraft:air
fill ~14 ~5 ~6 ~14 ~8 ~6 minecraft:air
fill ~14 ~5 ~8 ~14 ~6 ~8 minecraft:air
fill ~0 ~6 ~2 ~14 ~10 ~2 minecraft:air
setblock ~0 ~6 ~4 minecraft:air
fill ~0 ~6 ~10 ~14 ~10 ~10 minecraft:air
setblock ~2 ~6 ~4 minecraft:air
setblock ~2 ~6 ~8 minecraft:air
fill ~6 ~6 ~4 ~8 ~6 ~8 minecraft:air
fill ~14 ~6 ~5 ~14 ~7 ~5 minecraft:air
fill ~14 ~6 ~7 ~14 ~7 ~7 minecraft:air
fill ~0 ~7 ~3 ~14 ~10 ~3 minecraft:air
fill ~0 ~7 ~9 ~14 ~10 ~9 minecraft:air
fill ~3 ~7 ~5 ~12 ~7 ~7 minecraft:air
fill ~0 ~8 ~4 ~14 ~10 ~4 minecraft:air
fill ~0 ~8 ~8 ~14 ~10 ~8 minecraft:air
fill ~2 ~8 ~6 ~12 ~8 ~6 minecraft:air
fill ~0 ~9 ~5 ~14 ~10 ~5 minecraft:air
fill ~0 ~9 ~7 ~14 ~10 ~7 minecraft:air

# the plinth and the floor: cobblestone under the walls, worn spruce boards inside, a doorstep of stone before the door
setblock ~1 ~0 ~2 minecraft:mossy_cobblestone
fill ~1 ~0 ~3 ~1 ~0 ~6 minecraft:cobblestone
setblock ~1 ~0 ~7 minecraft:andesite
setblock ~1 ~0 ~8 minecraft:mossy_cobblestone
fill ~1 ~0 ~9 ~1 ~0 ~10 minecraft:cobblestone
fill ~2 ~0 ~2 ~7 ~0 ~2 minecraft:cobblestone
fill ~2 ~0 ~3 ~2 ~0 ~5 minecraft:spruce_planks
fill ~2 ~0 ~6 ~2 ~0 ~7 minecraft:oak_planks
fill ~2 ~0 ~8 ~2 ~0 ~9 minecraft:spruce_planks
fill ~2 ~0 ~10 ~4 ~0 ~10 minecraft:cobblestone
setblock ~3 ~0 ~3 minecraft:coarse_dirt
fill ~3 ~0 ~4 ~3 ~0 ~9 minecraft:spruce_planks
fill ~4 ~0 ~3 ~5 ~0 ~3 minecraft:spruce_planks
setblock ~4 ~0 ~4 minecraft:oak_planks
fill ~4 ~0 ~5 ~4 ~0 ~9 minecraft:spruce_planks
fill ~5 ~0 ~4 ~6 ~0 ~5 minecraft:spruce_planks
fill ~5 ~0 ~6 ~6 ~0 ~6 minecraft:oak_planks
fill ~5 ~0 ~7 ~6 ~0 ~9 minecraft:spruce_planks
setblock ~5 ~0 ~10 minecraft:mossy_cobblestone
setblock ~6 ~0 ~3 minecraft:oak_planks
fill ~6 ~0 ~10 ~8 ~0 ~10 minecraft:cobblestone
fill ~7 ~0 ~3 ~8 ~0 ~5 minecraft:spruce_planks
fill ~7 ~0 ~6 ~7 ~0 ~9 minecraft:spruce_planks
setblock ~8 ~0 ~2 minecraft:mossy_cobblestone
setblock ~8 ~0 ~6 minecraft:oak_planks
fill ~8 ~0 ~7 ~9 ~0 ~8 minecraft:spruce_planks
setblock ~8 ~0 ~9 minecraft:oak_planks
setblock ~9 ~0 ~2 minecraft:cobblestone
setblock ~9 ~0 ~3 minecraft:coarse_dirt
fill ~9 ~0 ~4 ~10 ~0 ~6 minecraft:spruce_planks
setblock ~9 ~0 ~9 minecraft:spruce_planks
fill ~9 ~0 ~10 ~10 ~0 ~10 minecraft:mossy_cobblestone
setblock ~10 ~0 ~2 minecraft:mossy_cobblestone
fill ~10 ~0 ~3 ~12 ~0 ~3 minecraft:spruce_planks
setblock ~10 ~0 ~7 minecraft:oak_planks
fill ~10 ~0 ~8 ~10 ~0 ~9 minecraft:spruce_planks
fill ~11 ~0 ~2 ~12 ~0 ~2 minecraft:cobblestone
setblock ~11 ~0 ~4 minecraft:oak_planks
setblock ~11 ~0 ~5 minecraft:spruce_planks
setblock ~11 ~0 ~6 minecraft:oak_planks
fill ~11 ~0 ~7 ~11 ~0 ~9 minecraft:spruce_planks
fill ~11 ~0 ~10 ~13 ~0 ~10 minecraft:cobblestone
fill ~12 ~0 ~4 ~12 ~0 ~9 minecraft:spruce_planks
setblock ~13 ~0 ~2 minecraft:mossy_cobblestone
setblock ~13 ~0 ~3 minecraft:cobblestone
setblock ~13 ~0 ~4 minecraft:mossy_cobblestone
setblock ~13 ~0 ~5 minecraft:andesite
fill ~13 ~0 ~6 ~13 ~0 ~7 minecraft:mossy_cobblestone
setblock ~13 ~0 ~8 minecraft:cobblestone
setblock ~13 ~0 ~9 minecraft:mossy_cobblestone
setblock ~14 ~0 ~5 minecraft:mossy_cobblestone
setblock ~14 ~0 ~6 minecraft:stone_bricks
setblock ~14 ~0 ~7 minecraft:cobblestone

# the walls: a sill course of cobblestone (rising damp) under oak boards, the middle bays of the long walls and the west gable in daub
fill ~1 ~1 ~3 ~1 ~1 ~4 minecraft:mossy_cobblestone
fill ~1 ~1 ~5 ~1 ~1 ~6 minecraft:cobblestone
fill ~1 ~1 ~7 ~1 ~1 ~8 minecraft:mossy_cobblestone
setblock ~1 ~1 ~9 minecraft:cobblestone
setblock ~2 ~1 ~2 minecraft:andesite
setblock ~2 ~1 ~10 minecraft:andesite
fill ~3 ~1 ~2 ~4 ~1 ~2 minecraft:mossy_cobblestone
setblock ~3 ~1 ~10 minecraft:mossy_cobblestone
setblock ~4 ~1 ~10 minecraft:andesite
setblock ~5 ~1 ~2 minecraft:andesite
fill ~5 ~1 ~10 ~6 ~1 ~10 minecraft:cobblestone
fill ~6 ~1 ~2 ~8 ~1 ~2 minecraft:cobblestone
setblock ~7 ~1 ~10 minecraft:mossy_cobblestone
setblock ~8 ~1 ~10 minecraft:cobblestone
setblock ~9 ~1 ~2 minecraft:andesite
setblock ~9 ~1 ~10 minecraft:mossy_cobblestone
fill ~10 ~1 ~2 ~11 ~1 ~2 minecraft:mossy_cobblestone
fill ~10 ~1 ~10 ~12 ~1 ~10 minecraft:cobblestone
setblock ~12 ~1 ~2 minecraft:andesite
setblock ~13 ~1 ~3 minecraft:cobblestone
setblock ~13 ~1 ~4 minecraft:mossy_cobblestone
setblock ~13 ~1 ~5 minecraft:cobblestone
setblock ~13 ~1 ~7 minecraft:cobblestone
setblock ~13 ~1 ~8 minecraft:andesite
setblock ~13 ~1 ~9 minecraft:cobblestone
fill ~1 ~2 ~3 ~1 ~3 ~3 minecraft:mud_bricks
setblock ~1 ~2 ~4 minecraft:packed_mud
setblock ~1 ~2 ~5 minecraft:mud_bricks
fill ~1 ~2 ~7 ~1 ~3 ~7 minecraft:packed_mud
fill ~1 ~2 ~8 ~1 ~3 ~8 minecraft:mud_bricks
setblock ~2 ~2 ~2 minecraft:spruce_planks
fill ~2 ~2 ~10 ~2 ~3 ~10 minecraft:oak_planks
fill ~4 ~2 ~2 ~4 ~3 ~2 minecraft:oak_planks
setblock ~4 ~2 ~10 minecraft:spruce_planks
fill ~6 ~2 ~2 ~6 ~3 ~2 minecraft:mud_bricks
fill ~6 ~2 ~10 ~8 ~2 ~10 minecraft:packed_mud
fill ~7 ~2 ~2 ~8 ~2 ~2 minecraft:packed_mud
fill ~10 ~2 ~2 ~10 ~3 ~2 minecraft:oak_planks
setblock ~10 ~2 ~10 minecraft:spruce_planks
fill ~12 ~2 ~2 ~12 ~3 ~2 minecraft:oak_planks
setblock ~12 ~2 ~10 minecraft:spruce_planks
fill ~13 ~2 ~4 ~13 ~3 ~4 minecraft:oak_planks
setblock ~13 ~2 ~8 minecraft:stripped_oak_wood
setblock ~1 ~3 ~4 minecraft:mud_bricks
setblock ~1 ~3 ~5 minecraft:packed_mud
fill ~2 ~3 ~2 ~3 ~3 ~2 minecraft:oak_planks
setblock ~3 ~3 ~10 minecraft:spruce_planks
setblock ~4 ~3 ~10 minecraft:oak_planks
fill ~6 ~3 ~10 ~7 ~3 ~10 minecraft:packed_mud
setblock ~7 ~3 ~2 minecraft:mud_bricks
setblock ~8 ~3 ~2 minecraft:packed_mud
setblock ~8 ~3 ~10 minecraft:mud_bricks
fill ~10 ~3 ~10 ~12 ~3 ~10 minecraft:oak_planks
setblock ~11 ~3 ~2 minecraft:oak_planks
setblock ~13 ~3 ~8 minecraft:spruce_planks

# the frame: dark oak corner posts from the plinth to the plate, posts on the sill every four blocks and either side of the door, the wall plate at y 4
fill ~1 ~1 ~2 ~1 ~3 ~2 minecraft:dark_oak_log[axis=y]
fill ~1 ~1 ~10 ~1 ~3 ~10 minecraft:dark_oak_log[axis=y]
fill ~13 ~1 ~2 ~13 ~3 ~2 minecraft:dark_oak_log[axis=y]
fill ~13 ~1 ~10 ~13 ~3 ~10 minecraft:dark_oak_log[axis=y]
fill ~1 ~2 ~6 ~1 ~3 ~6 minecraft:dark_oak_log[axis=y]
fill ~5 ~2 ~2 ~5 ~3 ~2 minecraft:dark_oak_log[axis=y]
fill ~5 ~2 ~10 ~5 ~3 ~10 minecraft:dark_oak_log[axis=y]
fill ~9 ~2 ~2 ~9 ~3 ~2 minecraft:dark_oak_log[axis=y]
fill ~9 ~2 ~10 ~9 ~3 ~10 minecraft:dark_oak_log[axis=y]
setblock ~13 ~2 ~5 minecraft:dark_oak_log[axis=y]
setblock ~13 ~2 ~7 minecraft:dark_oak_log[axis=y]
fill ~1 ~4 ~2 ~13 ~4 ~2 minecraft:dark_oak_log[axis=x]
fill ~1 ~4 ~3 ~1 ~4 ~9 minecraft:dark_oak_log[axis=z]
fill ~1 ~4 ~10 ~13 ~4 ~10 minecraft:dark_oak_log[axis=x]
fill ~13 ~4 ~3 ~13 ~4 ~9 minecraft:dark_oak_log[axis=z]

# the door: a planked spruce door at z 6 in the east gable, a dark oak lintel over it, the sill cut away under it
setblock ~13 ~1 ~6 minecraft:spruce_door[facing=west,half=lower,hinge=left,open=false,powered=false]
setblock ~13 ~2 ~6 minecraft:spruce_door[facing=west,half=upper,hinge=left,open=false,powered=false]
fill ~13 ~3 ~5 ~13 ~3 ~7 minecraft:dark_oak_log[axis=z]

# the windows: glass panes, two tall in the east gable and the west gable, one tall in the long walls under the eaves
fill ~1 ~2 ~9 ~1 ~3 ~9 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~3 ~2 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~3 ~2 ~10 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~11 ~2 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~11 ~2 ~10 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~13 ~2 ~3 ~13 ~3 ~3 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~13 ~2 ~9 ~13 ~3 ~9 minecraft:glass_pane[north=true,east=false,south=true,west=false]

# the roof: dark oak stairs from the eaves at y 4 (z 1 and 11, a block out from the walls) to a ridge of dark oak planks at y 9 (z 6), over x 0..14, a block proud of both gables
fill ~0 ~4 ~1 ~14 ~4 ~1 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~4 ~11 ~14 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~0 ~5 ~2 ~6 ~5 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~5 ~10 ~4 ~5 ~10 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~5 ~5 ~10 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~6 ~5 ~10 ~14 ~5 ~10 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~7 ~5 ~2 minecraft:cobblestone_stairs[facing=south,half=bottom]
fill ~8 ~5 ~2 ~14 ~5 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~6 ~3 ~2 ~6 ~3 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~6 ~9 ~10 ~6 ~9 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~3 ~6 ~3 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~4 ~6 ~3 ~14 ~6 ~3 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~11 ~6 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~12 ~6 ~9 ~14 ~6 ~9 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~0 ~7 ~4 ~11 ~7 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~7 ~8 ~14 ~7 ~8 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~12 ~7 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~13 ~7 ~4 ~14 ~7 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~1 ~8 ~5 ~14 ~8 ~5 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~1 ~8 ~7 ~14 ~8 ~7 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~1 ~9 ~6 ~14 ~9 ~6 minecraft:dark_oak_planks

# the gables: oak boards up to the roof on a collar beam at y 6, a dark oak king post above it; a loft window in the east gable
setblock ~1 ~5 ~3 minecraft:mud_bricks
fill ~1 ~5 ~4 ~1 ~5 ~9 minecraft:packed_mud
fill ~13 ~5 ~3 ~13 ~5 ~5 minecraft:oak_planks
setblock ~13 ~5 ~6 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~13 ~5 ~7 minecraft:oak_planks
setblock ~13 ~5 ~8 minecraft:spruce_planks
setblock ~13 ~5 ~9 minecraft:oak_planks
fill ~1 ~6 ~4 ~1 ~6 ~8 minecraft:dark_oak_log[axis=z]
fill ~13 ~6 ~4 ~13 ~6 ~8 minecraft:dark_oak_log[axis=z]
setblock ~1 ~7 ~5 minecraft:packed_mud
fill ~1 ~7 ~6 ~1 ~8 ~6 minecraft:dark_oak_log[axis=y]
setblock ~1 ~7 ~7 minecraft:packed_mud
setblock ~13 ~7 ~5 minecraft:oak_planks
fill ~13 ~7 ~6 ~13 ~8 ~6 minecraft:dark_oak_log[axis=y]
setblock ~13 ~7 ~7 minecraft:oak_planks

# the eaves: a soffit of upside-down dark oak stairs under each eave, the length of the roof
fill ~0 ~3 ~1 ~14 ~3 ~1 minecraft:dark_oak_stairs[facing=south,half=top]
fill ~0 ~3 ~11 ~14 ~3 ~11 minecraft:dark_oak_stairs[facing=north,half=top]

# a ridge course of dark oak slabs and a few broken shingles (spruce and cobblestone stairs) for weather
fill ~1 ~10 ~6 ~14 ~10 ~6 minecraft:dark_oak_slab[type=bottom]

# the shutters: spruce trapdoors swung open against the wall beside every window, the door hood: a slab on two brackets
setblock ~2 ~2 ~1 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
setblock ~2 ~2 ~11 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
setblock ~4 ~2 ~1 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
setblock ~4 ~2 ~11 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
setblock ~10 ~2 ~1 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
setblock ~10 ~2 ~11 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
setblock ~12 ~2 ~1 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
setblock ~12 ~2 ~11 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
fill ~14 ~2 ~2 ~14 ~3 ~2 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
fill ~14 ~2 ~4 ~14 ~3 ~4 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
setblock ~14 ~2 ~5 minecraft:dark_oak_stairs[facing=west,half=top]
setblock ~14 ~2 ~7 minecraft:dark_oak_stairs[facing=west,half=top]
fill ~14 ~2 ~8 ~14 ~3 ~8 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
fill ~14 ~2 ~10 ~14 ~3 ~10 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
fill ~14 ~3 ~5 ~14 ~3 ~7 minecraft:dark_oak_slab[type=bottom]
setblock ~14 ~5 ~5 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
setblock ~14 ~5 ~7 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]

# the chimney: a cobblestone stack against the west gable (x 0, z 5..7), narrowing to one flue at the ridge, a smouldering campfire on top
setblock ~0 ~0 ~5 minecraft:mossy_cobblestone
setblock ~0 ~0 ~6 minecraft:cobblestone
setblock ~0 ~0 ~7 minecraft:andesite
fill ~0 ~1 ~5 ~0 ~2 ~5 minecraft:cobblestone
setblock ~0 ~1 ~6 minecraft:mossy_cobblestone
setblock ~0 ~1 ~7 minecraft:cobblestone
fill ~0 ~2 ~6 ~0 ~4 ~6 minecraft:cobblestone
fill ~0 ~2 ~7 ~0 ~4 ~7 minecraft:mossy_cobblestone
fill ~0 ~3 ~5 ~0 ~4 ~5 minecraft:mossy_cobblestone
setblock ~0 ~5 ~5 minecraft:andesite
setblock ~0 ~5 ~6 minecraft:mossy_cobblestone
fill ~0 ~5 ~7 ~0 ~7 ~7 minecraft:cobblestone
setblock ~0 ~6 ~5 minecraft:mossy_cobblestone
fill ~0 ~6 ~6 ~0 ~9 ~6 minecraft:andesite
setblock ~0 ~7 ~5 minecraft:cobblestone
setblock ~0 ~8 ~5 minecraft:cobblestone_stairs[facing=south,half=bottom]
setblock ~0 ~8 ~7 minecraft:cobblestone_stairs[facing=north,half=bottom]
setblock ~0 ~10 ~6 minecraft:campfire[lit=true,signal_fire=false,facing=east]

# the hearth inside the west wall: a furnace and a cauldron by the chimney
setblock ~2 ~1 ~5 minecraft:cauldron
setblock ~2 ~1 ~6 minecraft:furnace[facing=east]
setblock ~2 ~1 ~7 minecraft:barrel[facing=up]

# the room: a cot along the north wall, a table and stools, sacks and barrels; nothing on the way from the door to the middle
fill ~3 ~1 ~3 ~4 ~1 ~3 minecraft:brown_wool
setblock ~3 ~1 ~9 minecraft:spruce_stairs[facing=east,half=bottom]
setblock ~4 ~1 ~9 minecraft:oak_fence[north=false,east=false,south=true,west=false]
setblock ~5 ~1 ~3 minecraft:spruce_slab[type=bottom]
setblock ~5 ~1 ~9 minecraft:spruce_stairs[facing=west,half=bottom]
setblock ~9 ~1 ~3 minecraft:crafting_table
setblock ~10 ~1 ~3 minecraft:barrel[facing=north]
fill ~10 ~1 ~9 ~11 ~1 ~9 minecraft:barrel[facing=up]
setblock ~11 ~1 ~3 minecraft:composter
setblock ~12 ~1 ~3 minecraft:hay_block[axis=y]
setblock ~12 ~1 ~9 minecraft:chest[facing=north]
setblock ~4 ~2 ~9 minecraft:oak_pressure_plate
setblock ~11 ~2 ~9 minecraft:flower_pot

# a tie beam across the room at y 4 carrying the lantern on a chain
setblock ~7 ~3 ~6 minecraft:lantern[hanging=true]
fill ~7 ~4 ~3 ~7 ~4 ~9 minecraft:dark_oak_log[axis=z]

# outside: a woodpile under the north eave, a rain barrel under the window and a bench by the door, moss at the foot of the walls
setblock ~0 ~1 ~9 minecraft:moss_carpet
fill ~1 ~1 ~11 ~2 ~1 ~11 minecraft:moss_carpet
fill ~6 ~1 ~1 ~8 ~1 ~1 minecraft:oak_log[axis=x]
setblock ~14 ~1 ~3 minecraft:barrel[facing=up]
setblock ~14 ~1 ~9 minecraft:spruce_stairs[facing=west,half=bottom]
setblock ~7 ~2 ~1 minecraft:oak_log[axis=x]
