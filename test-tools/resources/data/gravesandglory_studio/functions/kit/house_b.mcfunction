# size 15 11 13
# kit/house_b: hamlet two-storey house: cobblestone ground storey, jettied spruce upper storey in a dark oak frame, an east porch, a lean-to on the west.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Body x 2..12, z 2..10; upper storey (floor y 4, walls y 5..7) jettied out to z 1 and 11; door through the porch at x 14, z 5..7 and the body wall at x 12, z 5..7 (y 1..3).
# Ladder to the upper floor at (3, 1..4, 9) on the west wall. The way from the door to the centre block (7, 6) is kept clear.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~14 ~7 ~0 minecraft:air
fill ~0 ~1 ~1 ~1 ~7 ~2 minecraft:air
setblock ~0 ~1 ~4 minecraft:air
setblock ~0 ~1 ~6 minecraft:air
fill ~0 ~1 ~8 ~1 ~1 ~8 minecraft:air
fill ~0 ~1 ~10 ~0 ~10 ~12 minecraft:air
fill ~1 ~1 ~3 ~1 ~2 ~3 minecraft:air
fill ~1 ~1 ~9 ~1 ~2 ~12 minecraft:air
fill ~2 ~1 ~11 ~12 ~2 ~12 minecraft:air
fill ~3 ~1 ~1 ~14 ~2 ~1 minecraft:air
fill ~3 ~1 ~4 ~4 ~3 ~8 minecraft:air
fill ~4 ~1 ~9 ~4 ~3 ~9 minecraft:air
fill ~5 ~1 ~3 ~5 ~3 ~9 minecraft:air
fill ~6 ~1 ~4 ~6 ~3 ~8 minecraft:air
fill ~7 ~1 ~3 ~7 ~2 ~9 minecraft:air
fill ~8 ~1 ~4 ~10 ~3 ~8 minecraft:air
fill ~8 ~1 ~9 ~9 ~3 ~9 minecraft:air
fill ~11 ~1 ~3 ~11 ~3 ~8 minecraft:air
fill ~13 ~1 ~2 ~14 ~8 ~2 minecraft:air
fill ~13 ~1 ~3 ~13 ~2 ~12 minecraft:air
fill ~14 ~1 ~5 ~14 ~3 ~7 minecraft:air
fill ~14 ~1 ~10 ~14 ~10 ~12 minecraft:air
setblock ~1 ~2 ~8 minecraft:air
fill ~2 ~2 ~1 ~2 ~3 ~1 minecraft:air
fill ~4 ~2 ~3 ~4 ~3 ~3 minecraft:air
fill ~6 ~2 ~3 ~6 ~3 ~3 minecraft:air
fill ~6 ~2 ~9 ~6 ~3 ~9 minecraft:air
fill ~8 ~2 ~3 ~8 ~3 ~3 minecraft:air
fill ~10 ~2 ~3 ~10 ~3 ~3 minecraft:air
fill ~10 ~2 ~9 ~11 ~3 ~9 minecraft:air
fill ~14 ~2 ~4 ~14 ~3 ~4 minecraft:air
fill ~14 ~2 ~8 ~14 ~3 ~8 minecraft:air
fill ~0 ~3 ~3 ~0 ~10 ~9 minecraft:air
fill ~1 ~3 ~10 ~1 ~7 ~12 minecraft:air
fill ~2 ~3 ~11 ~10 ~3 ~12 minecraft:air
setblock ~3 ~3 ~3 minecraft:air
fill ~4 ~3 ~1 ~14 ~3 ~1 minecraft:air
fill ~7 ~3 ~3 ~7 ~3 ~5 minecraft:air
fill ~7 ~3 ~7 ~7 ~3 ~9 minecraft:air
setblock ~9 ~3 ~3 minecraft:air
fill ~11 ~3 ~12 ~11 ~7 ~12 minecraft:air
fill ~12 ~3 ~11 ~12 ~3 ~12 minecraft:air
fill ~13 ~3 ~3 ~13 ~3 ~5 minecraft:air
fill ~13 ~3 ~7 ~13 ~3 ~12 minecraft:air
fill ~1 ~4 ~3 ~1 ~8 ~9 minecraft:air
fill ~2 ~4 ~12 ~10 ~7 ~12 minecraft:air
fill ~12 ~4 ~12 ~12 ~7 ~12 minecraft:air
fill ~13 ~4 ~1 ~14 ~7 ~1 minecraft:air
fill ~13 ~4 ~10 ~13 ~7 ~12 minecraft:air
fill ~3 ~5 ~2 ~3 ~6 ~10 minecraft:air
fill ~4 ~5 ~3 ~5 ~6 ~10 minecraft:air
fill ~6 ~5 ~2 ~8 ~5 ~10 minecraft:air
fill ~9 ~5 ~3 ~10 ~6 ~10 minecraft:air
fill ~11 ~5 ~2 ~11 ~6 ~9 minecraft:air
fill ~13 ~5 ~3 ~14 ~8 ~9 minecraft:air
fill ~4 ~6 ~2 ~10 ~8 ~2 minecraft:air
fill ~6 ~6 ~3 ~8 ~8 ~5 minecraft:air
fill ~6 ~6 ~7 ~6 ~8 ~10 minecraft:air
fill ~7 ~6 ~6 ~8 ~6 ~10 minecraft:air
fill ~11 ~6 ~10 ~11 ~8 ~10 minecraft:air
fill ~3 ~7 ~2 ~3 ~8 ~5 minecraft:air
fill ~3 ~7 ~7 ~5 ~8 ~10 minecraft:air
fill ~4 ~7 ~3 ~5 ~8 ~5 minecraft:air
fill ~7 ~7 ~7 ~11 ~8 ~9 minecraft:air
fill ~7 ~7 ~10 ~10 ~8 ~10 minecraft:air
fill ~9 ~7 ~3 ~10 ~8 ~5 minecraft:air
fill ~11 ~7 ~2 ~11 ~8 ~5 minecraft:air
fill ~0 ~8 ~0 ~0 ~10 ~2 minecraft:air
setblock ~1 ~8 ~2 minecraft:air
setblock ~1 ~8 ~10 minecraft:air
fill ~3 ~8 ~6 ~11 ~9 ~6 minecraft:air
setblock ~13 ~8 ~10 minecraft:air
fill ~14 ~8 ~0 ~14 ~10 ~1 minecraft:air
fill ~1 ~9 ~0 ~13 ~10 ~1 minecraft:air
fill ~1 ~9 ~4 ~1 ~9 ~8 minecraft:air
fill ~1 ~9 ~11 ~13 ~10 ~12 minecraft:air
fill ~3 ~9 ~4 ~11 ~9 ~5 minecraft:air
fill ~3 ~9 ~7 ~11 ~9 ~8 minecraft:air
fill ~13 ~9 ~4 ~13 ~9 ~8 minecraft:air
fill ~14 ~9 ~2 ~14 ~10 ~9 minecraft:air
fill ~1 ~10 ~2 ~13 ~10 ~3 minecraft:air
fill ~1 ~10 ~9 ~13 ~10 ~10 minecraft:air

# the floor: a cobblestone plinth under the walls, packed earth and boards inside, porch boards
setblock ~2 ~0 ~2 minecraft:mossy_cobblestone
fill ~2 ~0 ~3 ~2 ~0 ~4 minecraft:cobblestone
setblock ~2 ~0 ~5 minecraft:mossy_cobblestone
fill ~2 ~0 ~6 ~2 ~0 ~7 minecraft:cobblestone
setblock ~2 ~0 ~8 minecraft:mossy_cobblestone
fill ~2 ~0 ~9 ~2 ~0 ~10 minecraft:cobblestone
fill ~3 ~0 ~2 ~9 ~0 ~2 minecraft:cobblestone
fill ~3 ~0 ~3 ~3 ~0 ~6 minecraft:spruce_planks
fill ~3 ~0 ~7 ~5 ~0 ~7 minecraft:packed_mud
fill ~3 ~0 ~8 ~5 ~0 ~9 minecraft:spruce_planks
setblock ~3 ~0 ~10 minecraft:cobblestone
fill ~4 ~0 ~3 ~5 ~0 ~3 minecraft:packed_mud
fill ~4 ~0 ~4 ~5 ~0 ~6 minecraft:spruce_planks
setblock ~4 ~0 ~10 minecraft:andesite
fill ~5 ~0 ~10 ~6 ~0 ~10 minecraft:mossy_cobblestone
fill ~6 ~0 ~3 ~6 ~0 ~7 minecraft:spruce_planks
fill ~6 ~0 ~8 ~7 ~0 ~8 minecraft:packed_mud
setblock ~6 ~0 ~9 minecraft:packed_mud
fill ~7 ~0 ~3 ~8 ~0 ~3 minecraft:packed_mud
fill ~7 ~0 ~4 ~8 ~0 ~4 minecraft:spruce_planks
setblock ~7 ~0 ~5 minecraft:packed_mud
fill ~7 ~0 ~6 ~7 ~0 ~7 minecraft:spruce_planks
setblock ~7 ~0 ~9 minecraft:spruce_planks
fill ~7 ~0 ~10 ~8 ~0 ~10 minecraft:cobblestone
fill ~8 ~0 ~5 ~8 ~0 ~8 minecraft:spruce_planks
setblock ~8 ~0 ~9 minecraft:packed_mud
fill ~9 ~0 ~3 ~9 ~0 ~5 minecraft:spruce_planks
setblock ~9 ~0 ~6 minecraft:packed_mud
setblock ~9 ~0 ~7 minecraft:coarse_dirt
fill ~9 ~0 ~8 ~10 ~0 ~8 minecraft:spruce_planks
setblock ~9 ~0 ~9 minecraft:coarse_dirt
setblock ~9 ~0 ~10 minecraft:mossy_cobblestone
fill ~10 ~0 ~2 ~11 ~0 ~2 minecraft:mossy_cobblestone
fill ~10 ~0 ~3 ~10 ~0 ~4 minecraft:coarse_dirt
setblock ~10 ~0 ~5 minecraft:spruce_planks
fill ~10 ~0 ~6 ~11 ~0 ~6 minecraft:coarse_dirt
setblock ~10 ~0 ~7 minecraft:packed_mud
setblock ~10 ~0 ~9 minecraft:packed_mud
fill ~10 ~0 ~10 ~11 ~0 ~10 minecraft:cobblestone
fill ~11 ~0 ~3 ~11 ~0 ~5 minecraft:spruce_planks
fill ~11 ~0 ~7 ~11 ~0 ~8 minecraft:spruce_planks
setblock ~11 ~0 ~9 minecraft:coarse_dirt
fill ~12 ~0 ~2 ~12 ~0 ~6 minecraft:cobblestone
fill ~12 ~0 ~7 ~12 ~0 ~8 minecraft:andesite
fill ~12 ~0 ~9 ~12 ~0 ~10 minecraft:mossy_cobblestone
fill ~13 ~0 ~3 ~14 ~0 ~3 minecraft:dark_oak_planks
fill ~13 ~0 ~4 ~14 ~0 ~5 minecraft:spruce_planks
setblock ~13 ~0 ~6 minecraft:dark_oak_planks
fill ~13 ~0 ~7 ~13 ~0 ~9 minecraft:spruce_planks
fill ~14 ~0 ~6 ~14 ~0 ~9 minecraft:spruce_planks

# the ground storey: cobblestone walls, mossy at the foot
fill ~2 ~1 ~3 ~2 ~2 ~3 minecraft:mossy_cobblestone
fill ~2 ~1 ~4 ~2 ~1 ~6 minecraft:cobblestone
fill ~2 ~1 ~7 ~2 ~1 ~8 minecraft:mossy_cobblestone
fill ~2 ~1 ~9 ~2 ~2 ~9 minecraft:cobblestone
fill ~3 ~1 ~2 ~3 ~2 ~2 minecraft:cobblestone
fill ~3 ~1 ~10 ~5 ~1 ~10 minecraft:mossy_cobblestone
fill ~4 ~1 ~2 ~6 ~1 ~2 minecraft:mossy_cobblestone
setblock ~6 ~1 ~10 minecraft:cobblestone
fill ~7 ~1 ~2 ~8 ~1 ~2 minecraft:cobblestone
fill ~7 ~1 ~10 ~9 ~1 ~10 minecraft:mossy_cobblestone
setblock ~9 ~1 ~2 minecraft:mossy_cobblestone
fill ~10 ~1 ~2 ~10 ~3 ~2 minecraft:cobblestone
fill ~10 ~1 ~10 ~11 ~2 ~10 minecraft:cobblestone
setblock ~11 ~1 ~2 minecraft:mossy_cobblestone
fill ~12 ~1 ~3 ~12 ~1 ~4 minecraft:cobblestone
fill ~12 ~1 ~8 ~12 ~3 ~8 minecraft:cobblestone
fill ~12 ~1 ~9 ~12 ~2 ~9 minecraft:mossy_cobblestone
setblock ~2 ~2 ~4 minecraft:mossy_cobblestone
setblock ~2 ~2 ~5 minecraft:cobblestone
fill ~2 ~2 ~7 ~2 ~3 ~8 minecraft:cobblestone
setblock ~3 ~2 ~10 minecraft:mossy_cobblestone
setblock ~4 ~2 ~2 minecraft:cobblestone
fill ~4 ~2 ~10 ~4 ~3 ~10 minecraft:cobblestone
fill ~6 ~2 ~2 ~7 ~2 ~2 minecraft:cobblestone
setblock ~6 ~2 ~10 minecraft:mossy_cobblestone
fill ~7 ~2 ~10 ~8 ~2 ~10 minecraft:cobblestone
setblock ~8 ~2 ~2 minecraft:mossy_cobblestone
setblock ~11 ~2 ~2 minecraft:cobblestone
setblock ~12 ~2 ~3 minecraft:mossy_cobblestone
fill ~12 ~2 ~4 ~12 ~3 ~4 minecraft:cobblestone
fill ~2 ~3 ~3 ~2 ~3 ~4 minecraft:cobblestone
setblock ~2 ~3 ~5 minecraft:mossy_cobblestone
setblock ~2 ~3 ~6 minecraft:cobblestone
setblock ~2 ~3 ~9 minecraft:mossy_cobblestone
setblock ~3 ~3 ~2 minecraft:mossy_cobblestone
setblock ~3 ~3 ~10 minecraft:andesite
setblock ~4 ~3 ~2 minecraft:andesite
fill ~5 ~3 ~2 ~6 ~3 ~2 minecraft:cobblestone
fill ~5 ~3 ~10 ~7 ~3 ~10 minecraft:cobblestone
setblock ~7 ~3 ~2 minecraft:mossy_cobblestone
fill ~8 ~3 ~2 ~9 ~3 ~2 minecraft:cobblestone
setblock ~8 ~3 ~10 minecraft:mossy_cobblestone
setblock ~9 ~3 ~10 minecraft:cobblestone
setblock ~10 ~3 ~10 minecraft:andesite
setblock ~11 ~3 ~2 minecraft:mossy_cobblestone
setblock ~11 ~3 ~10 minecraft:cobblestone
setblock ~12 ~3 ~3 minecraft:cobblestone
setblock ~12 ~3 ~9 minecraft:cobblestone

# the upper floor: spruce planks at y 4, jettied one block out north and south on dark oak joists
fill ~2 ~4 ~1 ~12 ~4 ~1 minecraft:dark_oak_log[axis=z]
fill ~2 ~4 ~2 ~12 ~4 ~8 minecraft:spruce_planks
fill ~2 ~4 ~9 ~2 ~4 ~10 minecraft:spruce_planks
fill ~2 ~4 ~11 ~12 ~4 ~11 minecraft:dark_oak_log[axis=z]
setblock ~3 ~4 ~10 minecraft:spruce_planks
fill ~4 ~4 ~9 ~12 ~4 ~10 minecraft:spruce_planks

# the upper storey: spruce planks in a dark oak frame (posts at the corners and every three blocks, a sill at y 5)
fill ~2 ~5 ~1 ~2 ~7 ~1 minecraft:dark_oak_log[axis=y]
fill ~2 ~5 ~2 ~2 ~5 ~5 minecraft:spruce_planks
fill ~2 ~5 ~6 ~2 ~7 ~6 minecraft:dark_oak_log[axis=y]
fill ~2 ~5 ~7 ~2 ~5 ~10 minecraft:spruce_planks
fill ~2 ~5 ~11 ~2 ~7 ~11 minecraft:dark_oak_log[axis=y]
setblock ~3 ~5 ~1 minecraft:spruce_planks
setblock ~3 ~5 ~11 minecraft:spruce_planks
setblock ~4 ~5 ~1 minecraft:stripped_spruce_wood
setblock ~4 ~5 ~11 minecraft:stripped_spruce_wood
fill ~5 ~5 ~1 ~5 ~7 ~1 minecraft:dark_oak_log[axis=y]
fill ~5 ~5 ~11 ~5 ~7 ~11 minecraft:dark_oak_log[axis=y]
fill ~6 ~5 ~1 ~6 ~7 ~1 minecraft:spruce_planks
fill ~6 ~5 ~11 ~6 ~7 ~11 minecraft:spruce_planks
fill ~8 ~5 ~1 ~8 ~7 ~1 minecraft:spruce_planks
fill ~8 ~5 ~11 ~8 ~7 ~11 minecraft:spruce_planks
fill ~9 ~5 ~1 ~9 ~7 ~1 minecraft:dark_oak_log[axis=y]
fill ~9 ~5 ~11 ~9 ~7 ~11 minecraft:dark_oak_log[axis=y]
fill ~10 ~5 ~1 ~11 ~5 ~1 minecraft:stripped_spruce_wood
fill ~10 ~5 ~11 ~11 ~5 ~11 minecraft:spruce_planks
fill ~12 ~5 ~1 ~12 ~7 ~1 minecraft:dark_oak_log[axis=y]
fill ~12 ~5 ~2 ~12 ~5 ~5 minecraft:spruce_planks
fill ~12 ~5 ~6 ~12 ~7 ~6 minecraft:dark_oak_log[axis=y]
setblock ~12 ~5 ~7 minecraft:stripped_spruce_wood
fill ~12 ~5 ~8 ~12 ~5 ~10 minecraft:spruce_planks
fill ~12 ~5 ~11 ~12 ~7 ~11 minecraft:dark_oak_log[axis=y]
fill ~2 ~6 ~2 ~2 ~7 ~3 minecraft:spruce_planks
fill ~2 ~6 ~5 ~2 ~7 ~5 minecraft:spruce_planks
fill ~2 ~6 ~7 ~2 ~7 ~7 minecraft:spruce_planks
fill ~2 ~6 ~9 ~2 ~7 ~10 minecraft:spruce_planks
fill ~12 ~6 ~2 ~12 ~7 ~3 minecraft:spruce_planks
setblock ~12 ~6 ~5 minecraft:spruce_planks
fill ~12 ~6 ~7 ~12 ~7 ~7 minecraft:spruce_planks
setblock ~12 ~6 ~9 minecraft:spruce_planks
setblock ~12 ~6 ~10 minecraft:stripped_spruce_wood
setblock ~2 ~7 ~4 minecraft:spruce_planks
setblock ~2 ~7 ~8 minecraft:spruce_planks
fill ~3 ~7 ~1 ~4 ~7 ~1 minecraft:spruce_planks
fill ~3 ~7 ~11 ~4 ~7 ~11 minecraft:spruce_planks
setblock ~7 ~7 ~1 minecraft:spruce_planks
setblock ~7 ~7 ~11 minecraft:spruce_planks
fill ~10 ~7 ~1 ~11 ~7 ~1 minecraft:spruce_planks
fill ~10 ~7 ~11 ~11 ~7 ~11 minecraft:spruce_planks
setblock ~12 ~7 ~4 minecraft:spruce_planks
setblock ~12 ~7 ~5 minecraft:stripped_spruce_wood
setblock ~12 ~7 ~8 minecraft:spruce_planks
setblock ~12 ~7 ~9 minecraft:stripped_spruce_wood
setblock ~12 ~7 ~10 minecraft:spruce_planks

# the ground storey corners: spruce log posts carrying the jetty
fill ~2 ~1 ~2 ~2 ~3 ~2 minecraft:spruce_log[axis=y]
fill ~2 ~1 ~10 ~2 ~3 ~10 minecraft:spruce_log[axis=y]
fill ~12 ~1 ~2 ~12 ~3 ~2 minecraft:spruce_log[axis=y]
fill ~12 ~1 ~10 ~12 ~3 ~10 minecraft:spruce_log[axis=y]

# windows: glass panes in both storeys, shutters of spruce trapdoors beside the upper ones
setblock ~2 ~2 ~6 minecraft:glass_pane[north=true,east=false,south=true,west=true]
setblock ~5 ~2 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~5 ~2 ~10 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~9 ~2 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~9 ~2 ~10 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~7 ~5 ~1 ~7 ~6 ~1 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~7 ~5 ~11 ~7 ~6 ~11 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~2 ~6 ~4 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~2 ~6 ~8 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~3 ~6 ~1 ~4 ~6 ~1 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~3 ~6 ~11 ~4 ~6 ~11 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~10 ~6 ~1 ~11 ~6 ~1 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~10 ~6 ~11 ~11 ~6 ~11 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~12 ~6 ~4 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~12 ~6 ~8 minecraft:glass_pane[north=true,east=false,south=true,west=false]

# the door: through the east wall (x 12) and the porch rail (x 14), z 5..7, y 1..3
fill ~12 ~1 ~5 ~12 ~3 ~7 minecraft:air

# the roof: spruce stairs from the eaves at y 8 (z 0 and 12) at half a block a row to a flat ridge of dark oak slabs at y 10, over x 1..13
fill ~1 ~8 ~0 ~13 ~8 ~0 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~1 ~8 ~1 ~13 ~8 ~1 minecraft:spruce_slab[type=top]
fill ~1 ~8 ~11 ~13 ~8 ~11 minecraft:spruce_slab[type=top]
fill ~1 ~8 ~12 ~13 ~8 ~12 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~1 ~9 ~2 ~13 ~9 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~1 ~9 ~3 ~13 ~9 ~3 minecraft:spruce_slab[type=top]
fill ~1 ~9 ~9 ~13 ~9 ~9 minecraft:spruce_slab[type=top]
fill ~1 ~9 ~10 ~13 ~9 ~10 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~1 ~10 ~4 ~13 ~10 ~4 minecraft:dark_oak_slab[type=bottom]
fill ~1 ~10 ~5 ~13 ~10 ~5 minecraft:spruce_slab[type=top]
fill ~1 ~10 ~6 ~13 ~10 ~6 minecraft:dark_oak_slab[type=bottom]
fill ~1 ~10 ~7 ~13 ~10 ~7 minecraft:spruce_slab[type=top]
fill ~1 ~10 ~8 ~13 ~10 ~8 minecraft:dark_oak_slab[type=bottom]

# the gables: spruce planks under the roof at x 2 and x 12, y 8..9
fill ~2 ~8 ~2 ~2 ~8 ~5 minecraft:spruce_planks
fill ~2 ~8 ~6 ~2 ~9 ~6 minecraft:dark_oak_log[axis=y]
fill ~2 ~8 ~7 ~2 ~8 ~10 minecraft:spruce_planks
fill ~12 ~8 ~2 ~12 ~8 ~5 minecraft:spruce_planks
fill ~12 ~8 ~6 ~12 ~9 ~6 minecraft:dark_oak_log[axis=y]
fill ~12 ~8 ~7 ~12 ~8 ~10 minecraft:spruce_planks
fill ~2 ~9 ~4 ~2 ~9 ~5 minecraft:spruce_planks
fill ~2 ~9 ~7 ~2 ~9 ~8 minecraft:spruce_planks
fill ~12 ~9 ~4 ~12 ~9 ~5 minecraft:spruce_planks
fill ~12 ~9 ~7 ~12 ~9 ~8 minecraft:spruce_planks

# the porch: spruce posts at the corners, a fence rail on the east side with the door gap at z 5..7, a dark oak slab roof at y 4
fill ~14 ~1 ~3 ~14 ~3 ~3 minecraft:spruce_log[axis=y]
setblock ~14 ~1 ~4 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
setblock ~14 ~1 ~8 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
fill ~14 ~1 ~9 ~14 ~3 ~9 minecraft:spruce_log[axis=y]
setblock ~13 ~3 ~6 minecraft:lantern[hanging=true]
fill ~13 ~4 ~3 ~14 ~4 ~9 minecraft:dark_oak_slab[type=bottom]

# the lean-to on the west: a spruce stair roof at y 3 over x 0..1, posts at x 0, a woodpile and a grindstone under it
setblock ~0 ~0 ~4 minecraft:packed_mud
setblock ~0 ~0 ~5 minecraft:coarse_dirt
setblock ~0 ~0 ~6 minecraft:packed_mud
setblock ~0 ~0 ~7 minecraft:coarse_dirt
setblock ~0 ~1 ~3 minecraft:spruce_log[axis=y]
setblock ~0 ~1 ~5 minecraft:grindstone[face=floor,facing=north]
setblock ~0 ~1 ~7 minecraft:barrel[facing=up]
setblock ~0 ~1 ~9 minecraft:spruce_log[axis=y]
fill ~1 ~1 ~4 ~1 ~2 ~7 minecraft:spruce_log[axis=z]
fill ~0 ~2 ~3 ~0 ~2 ~9 minecraft:spruce_slab[type=top]
fill ~1 ~3 ~3 ~1 ~3 ~9 minecraft:spruce_stairs[facing=east,half=bottom]

# inside: a ladder to the loft on the west wall, a table, barrels and a chest on the ground storey; beds of wool and a chest above
fill ~3 ~1 ~3 ~4 ~1 ~3 minecraft:barrel[facing=up]
fill ~3 ~1 ~9 ~3 ~4 ~9 minecraft:ladder[facing=east]
setblock ~6 ~1 ~3 minecraft:crafting_table
setblock ~6 ~1 ~9 minecraft:cauldron
setblock ~8 ~1 ~3 minecraft:spruce_stairs[facing=east,half=bottom]
setblock ~9 ~1 ~3 minecraft:oak_fence[north=true,east=false,south=false,west=false]
setblock ~10 ~1 ~3 minecraft:spruce_stairs[facing=west,half=bottom]
setblock ~10 ~1 ~9 minecraft:chest[facing=north]
setblock ~11 ~1 ~9 minecraft:barrel[facing=up]
setblock ~3 ~2 ~3 minecraft:barrel[facing=up]
setblock ~9 ~2 ~3 minecraft:oak_pressure_plate
setblock ~7 ~3 ~6 minecraft:lantern[hanging=true]
fill ~4 ~5 ~2 ~5 ~5 ~2 minecraft:red_wool
fill ~9 ~5 ~2 ~10 ~5 ~2 minecraft:brown_wool
setblock ~11 ~5 ~10 minecraft:chest[facing=north]
setblock ~6 ~6 ~6 minecraft:lantern[hanging=true]
fill ~3 ~7 ~6 ~11 ~7 ~6 minecraft:dark_oak_log[axis=x]

# weather: cobwebs under the jetty and a moss patch on the plinth
setblock ~2 ~1 ~1 minecraft:moss_carpet
setblock ~3 ~3 ~1 minecraft:cobweb
setblock ~11 ~3 ~11 minecraft:cobweb
