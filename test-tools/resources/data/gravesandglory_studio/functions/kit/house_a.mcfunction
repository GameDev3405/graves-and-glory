# size 15 11 13
# kit/house_a: hamlet cottage: oak planks in a stripped oak frame on a cobblestone plinth, dark oak roof, a chimney on the west gable, the door east.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Body x 1..14, z 2..10; walls y 1..4; door at x 14, z 5..7 (y 1..3) in the east gable; windows north (z 2) and south (z 10) at x 3..4 and 10..11, y 2..3; chimney at x 0, z 5..7.
# The way in from the door to the middle of the floor (z 5..7) is kept clear: the hamlet lane runs to the centre block (7, 6).

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~14 ~10 ~0 minecraft:air
fill ~0 ~1 ~1 ~5 ~2 ~1 minecraft:air
fill ~0 ~1 ~2 ~0 ~4 ~4 minecraft:air
fill ~0 ~1 ~8 ~0 ~3 ~12 minecraft:air
fill ~1 ~1 ~11 ~11 ~3 ~12 minecraft:air
fill ~2 ~1 ~3 ~2 ~5 ~4 minecraft:air
fill ~2 ~1 ~8 ~2 ~5 ~9 minecraft:air
fill ~3 ~1 ~4 ~5 ~6 ~8 minecraft:air
fill ~6 ~1 ~3 ~8 ~2 ~9 minecraft:air
fill ~9 ~1 ~1 ~14 ~3 ~1 minecraft:air
fill ~9 ~1 ~4 ~10 ~5 ~9 minecraft:air
fill ~11 ~1 ~3 ~11 ~5 ~8 minecraft:air
fill ~12 ~1 ~4 ~13 ~6 ~8 minecraft:air
fill ~12 ~1 ~12 ~13 ~10 ~12 minecraft:air
fill ~14 ~1 ~11 ~14 ~3 ~12 minecraft:air
fill ~2 ~2 ~5 ~2 ~7 ~7 minecraft:air
fill ~3 ~2 ~3 ~5 ~5 ~3 minecraft:air
fill ~3 ~2 ~9 ~3 ~5 ~9 minecraft:air
fill ~5 ~2 ~9 ~5 ~5 ~9 minecraft:air
fill ~6 ~2 ~1 ~6 ~3 ~1 minecraft:air
fill ~8 ~2 ~1 ~8 ~3 ~1 minecraft:air
fill ~9 ~2 ~3 ~10 ~5 ~3 minecraft:air
fill ~11 ~2 ~9 ~11 ~5 ~9 minecraft:air
fill ~12 ~2 ~3 ~13 ~5 ~3 minecraft:air
fill ~12 ~2 ~11 ~13 ~2 ~11 minecraft:air
fill ~13 ~2 ~9 ~13 ~5 ~9 minecraft:air
fill ~0 ~3 ~1 ~1 ~3 ~1 minecraft:air
fill ~3 ~3 ~1 ~5 ~3 ~1 minecraft:air
fill ~4 ~3 ~9 ~4 ~5 ~9 minecraft:air
fill ~6 ~3 ~3 ~8 ~3 ~5 minecraft:air
fill ~6 ~3 ~6 ~6 ~5 ~9 minecraft:air
setblock ~7 ~3 ~1 minecraft:air
fill ~7 ~3 ~7 ~7 ~3 ~9 minecraft:air
fill ~8 ~3 ~6 ~8 ~5 ~9 minecraft:air
fill ~12 ~3 ~9 ~12 ~5 ~9 minecraft:air
setblock ~12 ~3 ~11 minecraft:air
fill ~0 ~4 ~8 ~0 ~4 ~10 minecraft:air
fill ~0 ~4 ~12 ~11 ~10 ~12 minecraft:air
fill ~6 ~4 ~3 ~6 ~5 ~5 minecraft:air
fill ~8 ~4 ~3 ~8 ~5 ~5 minecraft:air
fill ~14 ~4 ~12 ~14 ~10 ~12 minecraft:air
fill ~0 ~5 ~1 ~14 ~10 ~1 minecraft:air
fill ~0 ~5 ~3 ~0 ~5 ~4 minecraft:air
fill ~0 ~5 ~8 ~0 ~5 ~9 minecraft:air
fill ~0 ~5 ~11 ~14 ~10 ~11 minecraft:air
fill ~7 ~5 ~3 ~7 ~5 ~9 minecraft:air
fill ~0 ~6 ~2 ~14 ~10 ~2 minecraft:air
setblock ~0 ~6 ~4 minecraft:air
setblock ~0 ~6 ~8 minecraft:air
fill ~0 ~6 ~10 ~14 ~10 ~10 minecraft:air
setblock ~2 ~6 ~4 minecraft:air
setblock ~2 ~6 ~8 minecraft:air
fill ~6 ~6 ~4 ~11 ~6 ~8 minecraft:air
fill ~0 ~7 ~3 ~14 ~10 ~3 minecraft:air
fill ~0 ~7 ~9 ~14 ~10 ~9 minecraft:air
fill ~3 ~7 ~5 ~13 ~7 ~7 minecraft:air
fill ~0 ~8 ~4 ~14 ~10 ~4 minecraft:air
fill ~0 ~8 ~8 ~14 ~10 ~8 minecraft:air
fill ~2 ~8 ~6 ~13 ~8 ~6 minecraft:air
fill ~0 ~9 ~5 ~14 ~10 ~5 minecraft:air
fill ~0 ~9 ~7 ~14 ~10 ~7 minecraft:air

# the plinth and the floor: a course of cobblestone under the walls, worn spruce boards inside, a step of cobble at the door
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
fill ~10 ~0 ~3 ~13 ~0 ~3 minecraft:spruce_planks
setblock ~10 ~0 ~7 minecraft:oak_planks
fill ~10 ~0 ~8 ~10 ~0 ~9 minecraft:spruce_planks
fill ~11 ~0 ~2 ~12 ~0 ~2 minecraft:cobblestone
setblock ~11 ~0 ~4 minecraft:oak_planks
setblock ~11 ~0 ~5 minecraft:spruce_planks
setblock ~11 ~0 ~6 minecraft:oak_planks
fill ~11 ~0 ~7 ~11 ~0 ~9 minecraft:spruce_planks
fill ~11 ~0 ~10 ~13 ~0 ~10 minecraft:cobblestone
fill ~12 ~0 ~4 ~13 ~0 ~7 minecraft:spruce_planks
fill ~12 ~0 ~8 ~12 ~0 ~9 minecraft:spruce_planks
fill ~13 ~0 ~2 ~14 ~0 ~2 minecraft:mossy_cobblestone
setblock ~13 ~0 ~8 minecraft:coarse_dirt
setblock ~13 ~0 ~9 minecraft:spruce_planks
setblock ~14 ~0 ~3 minecraft:cobblestone
setblock ~14 ~0 ~4 minecraft:mossy_cobblestone
setblock ~14 ~0 ~5 minecraft:cobblestone
fill ~14 ~0 ~6 ~14 ~0 ~9 minecraft:mossy_cobblestone
setblock ~14 ~0 ~10 minecraft:andesite

# the walls: oak planks, the lowest course cobblestone (rising damp), weathered with a few spruce boards
fill ~1 ~1 ~3 ~1 ~1 ~4 minecraft:mossy_cobblestone
setblock ~1 ~1 ~5 minecraft:cobblestone
fill ~1 ~1 ~7 ~1 ~1 ~8 minecraft:mossy_cobblestone
setblock ~1 ~1 ~9 minecraft:cobblestone
setblock ~2 ~1 ~2 minecraft:andesite
setblock ~2 ~1 ~10 minecraft:andesite
fill ~3 ~1 ~2 ~4 ~1 ~2 minecraft:mossy_cobblestone
setblock ~3 ~1 ~10 minecraft:mossy_cobblestone
setblock ~4 ~1 ~10 minecraft:andesite
fill ~6 ~1 ~2 ~8 ~1 ~2 minecraft:cobblestone
setblock ~6 ~1 ~10 minecraft:cobblestone
setblock ~7 ~1 ~10 minecraft:mossy_cobblestone
setblock ~8 ~1 ~10 minecraft:cobblestone
fill ~10 ~1 ~2 ~11 ~1 ~2 minecraft:mossy_cobblestone
fill ~10 ~1 ~10 ~13 ~1 ~10 minecraft:cobblestone
setblock ~12 ~1 ~2 minecraft:andesite
setblock ~13 ~1 ~2 minecraft:cobblestone
setblock ~14 ~1 ~3 minecraft:mossy_cobblestone
setblock ~14 ~1 ~9 minecraft:cobblestone
fill ~1 ~2 ~3 ~1 ~2 ~5 minecraft:oak_planks
setblock ~1 ~2 ~7 minecraft:spruce_planks
fill ~1 ~2 ~8 ~1 ~3 ~9 minecraft:oak_planks
fill ~2 ~2 ~2 ~2 ~3 ~2 minecraft:oak_planks
fill ~2 ~2 ~10 ~2 ~3 ~10 minecraft:oak_planks
setblock ~6 ~2 ~2 minecraft:spruce_planks
fill ~6 ~2 ~10 ~6 ~3 ~10 minecraft:oak_planks
fill ~7 ~2 ~2 ~8 ~2 ~2 minecraft:oak_planks
setblock ~7 ~2 ~10 minecraft:spruce_planks
fill ~8 ~2 ~10 ~8 ~3 ~10 minecraft:oak_planks
fill ~12 ~2 ~2 ~13 ~3 ~2 minecraft:oak_planks
fill ~12 ~2 ~10 ~13 ~2 ~10 minecraft:spruce_planks
setblock ~14 ~2 ~3 minecraft:spruce_planks
setblock ~14 ~2 ~9 minecraft:spruce_planks
setblock ~1 ~3 ~3 minecraft:stripped_oak_wood
fill ~1 ~3 ~4 ~1 ~3 ~5 minecraft:oak_planks
setblock ~1 ~3 ~7 minecraft:stripped_oak_wood
setblock ~6 ~3 ~2 minecraft:oak_planks
setblock ~7 ~3 ~2 minecraft:spruce_planks
setblock ~7 ~3 ~10 minecraft:oak_planks
setblock ~8 ~3 ~2 minecraft:oak_planks
fill ~12 ~3 ~10 ~13 ~3 ~10 minecraft:oak_planks
setblock ~14 ~3 ~3 minecraft:oak_planks
setblock ~14 ~3 ~9 minecraft:oak_planks

# the frame: stripped oak posts at the corners and every few blocks, a wall plate along the top
fill ~1 ~1 ~2 ~1 ~3 ~2 minecraft:stripped_oak_log[axis=y]
fill ~1 ~1 ~6 ~1 ~3 ~6 minecraft:stripped_oak_log[axis=y]
fill ~1 ~1 ~10 ~1 ~3 ~10 minecraft:stripped_oak_log[axis=y]
fill ~5 ~1 ~2 ~5 ~3 ~2 minecraft:stripped_oak_log[axis=y]
fill ~5 ~1 ~10 ~5 ~3 ~10 minecraft:stripped_oak_log[axis=y]
fill ~9 ~1 ~2 ~9 ~3 ~2 minecraft:stripped_oak_log[axis=y]
fill ~9 ~1 ~10 ~9 ~3 ~10 minecraft:stripped_oak_log[axis=y]
fill ~14 ~1 ~2 ~14 ~3 ~2 minecraft:stripped_oak_log[axis=y]
fill ~14 ~1 ~4 ~14 ~3 ~4 minecraft:stripped_oak_log[axis=y]
fill ~14 ~1 ~8 ~14 ~3 ~8 minecraft:stripped_oak_log[axis=y]
fill ~14 ~1 ~10 ~14 ~3 ~10 minecraft:stripped_oak_log[axis=y]
fill ~1 ~4 ~2 ~14 ~4 ~2 minecraft:stripped_oak_log[axis=x]
fill ~1 ~4 ~3 ~1 ~4 ~9 minecraft:stripped_oak_log[axis=z]
fill ~1 ~4 ~10 ~14 ~4 ~10 minecraft:stripped_oak_log[axis=x]
fill ~14 ~4 ~3 ~14 ~4 ~9 minecraft:stripped_oak_log[axis=z]

# the door: an opening three wide in the east gable under a lintel, open to the lane
fill ~14 ~1 ~5 ~14 ~3 ~7 minecraft:air

# the windows: glass panes in the north and south walls, two pairs each
fill ~3 ~2 ~2 ~4 ~3 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~3 ~2 ~10 ~4 ~3 ~10 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~10 ~2 ~2 ~11 ~3 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~10 ~2 ~10 ~11 ~3 ~10 minecraft:glass_pane[north=false,east=true,south=false,west=true]

# the roof: dark oak stairs from the eaves at y 4 (z 1 and 11, overhanging the walls) to a ridge of dark oak planks at y 9 (z 6), over x 0..14
fill ~0 ~4 ~1 ~14 ~4 ~1 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~4 ~11 ~14 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~0 ~5 ~2 ~6 ~5 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~5 ~10 ~14 ~5 ~10 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~7 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
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

# the gables: oak planks up to the roof, a stripped oak king post, a small shuttered loft window in the east gable
setblock ~1 ~5 ~3 minecraft:spruce_planks
fill ~1 ~5 ~4 ~1 ~5 ~5 minecraft:oak_planks
fill ~1 ~5 ~6 ~1 ~8 ~6 minecraft:stripped_oak_log[axis=y]
fill ~1 ~5 ~7 ~1 ~5 ~9 minecraft:oak_planks
fill ~14 ~5 ~3 ~14 ~5 ~4 minecraft:oak_planks
setblock ~14 ~5 ~5 minecraft:spruce_planks
setblock ~14 ~5 ~6 minecraft:stripped_oak_log[axis=y]
fill ~14 ~5 ~7 ~14 ~5 ~9 minecraft:oak_planks
setblock ~1 ~6 ~4 minecraft:oak_planks
setblock ~1 ~6 ~5 minecraft:spruce_planks
fill ~1 ~6 ~7 ~1 ~6 ~8 minecraft:oak_planks
setblock ~14 ~6 ~4 minecraft:oak_planks
setblock ~14 ~6 ~5 minecraft:spruce_trapdoor[facing=east,half=top,open=true]
setblock ~14 ~6 ~6 minecraft:air
fill ~14 ~6 ~7 ~14 ~6 ~8 minecraft:oak_planks
setblock ~1 ~7 ~5 minecraft:oak_planks
setblock ~1 ~7 ~7 minecraft:oak_planks
setblock ~14 ~7 ~5 minecraft:oak_planks
fill ~14 ~7 ~6 ~14 ~8 ~6 minecraft:stripped_oak_log[axis=y]
setblock ~14 ~7 ~7 minecraft:spruce_planks

# a ridge course of dark oak slabs and a few broken shingles (cobblestone and spruce stairs) for weather
fill ~1 ~10 ~6 ~14 ~10 ~6 minecraft:dark_oak_slab[type=bottom]

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
fill ~3 ~1 ~3 ~4 ~1 ~3 minecraft:white_wool
setblock ~3 ~1 ~9 minecraft:spruce_stairs[facing=east,half=bottom]
setblock ~4 ~1 ~9 minecraft:oak_fence[north=false,east=false,south=true,west=false]
setblock ~5 ~1 ~3 minecraft:spruce_slab[type=bottom]
setblock ~5 ~1 ~9 minecraft:spruce_stairs[facing=west,half=bottom]
setblock ~9 ~1 ~3 minecraft:crafting_table
setblock ~10 ~1 ~3 minecraft:barrel[facing=north]
fill ~11 ~1 ~9 ~12 ~1 ~9 minecraft:barrel[facing=up]
setblock ~12 ~1 ~3 minecraft:composter
setblock ~13 ~1 ~3 minecraft:hay_block[axis=y]
setblock ~13 ~1 ~9 minecraft:chest[facing=north]
setblock ~4 ~2 ~9 minecraft:oak_pressure_plate
setblock ~12 ~2 ~9 minecraft:flower_pot

# a tie beam across the room at y 4 carrying the lantern on a chain
setblock ~7 ~3 ~6 minecraft:lantern[hanging=true]
fill ~7 ~4 ~3 ~7 ~4 ~9 minecraft:stripped_oak_log[axis=z]

# outside: a woodpile under the north eave, a barrel and a bench by the door, cobwebs in the eaves
fill ~6 ~1 ~1 ~8 ~1 ~1 minecraft:oak_log[axis=x]
setblock ~12 ~1 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~13 ~1 ~11 minecraft:barrel[facing=up]
setblock ~7 ~2 ~1 minecraft:oak_log[axis=x]
setblock ~2 ~3 ~1 minecraft:cobweb
setblock ~13 ~3 ~11 minecraft:cobweb
