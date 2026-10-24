# size 15 17 13
# kit/house_b: hamlet two-storey house: cobblestone ground storey, a jettied, close-studded upper storey of daub in dark oak, a steep spruce roof broken by a stone chimney, an east porch under a hanging sign, a lean-to on the west.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Body x 2..12, z 2..10; upper storey (floor y 4, walls y 5..7, plate y 8) jettied out to z 1 and 11; the roof over x 1..13 (eaves y 8 at z 0 and 12, ridge y 14 at z 6); the chimney x 4..5, z 6..7 from y 1 to y 15.
# Door at x 12, z 6 (y 1..2) under a lintel, through the porch x 13..14, z 3..9 (its rail open at z 5..7). Ladder to the upper floor at (3, 1..4, 9). The way from the porch to the centre block (7, 6) is kept clear.
# The ground layer is the plinth, the floors, the porch boards and the lean-to floor: every other cell of y 0 is structure void, so the lane and the ground of the map run up to the porch.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~14 ~4 ~0 minecraft:air
fill ~0 ~1 ~1 ~1 ~8 ~2 minecraft:air
setblock ~0 ~1 ~4 minecraft:air
setblock ~0 ~1 ~6 minecraft:air
fill ~0 ~1 ~8 ~1 ~1 ~8 minecraft:air
fill ~0 ~1 ~10 ~0 ~16 ~12 minecraft:air
fill ~1 ~1 ~3 ~1 ~2 ~3 minecraft:air
fill ~1 ~1 ~9 ~1 ~2 ~12 minecraft:air
fill ~2 ~1 ~11 ~12 ~1 ~12 minecraft:air
fill ~3 ~1 ~1 ~14 ~1 ~1 minecraft:air
fill ~3 ~1 ~4 ~4 ~3 ~5 minecraft:air
fill ~3 ~1 ~6 ~3 ~3 ~8 minecraft:air
fill ~4 ~1 ~8 ~5 ~3 ~9 minecraft:air
fill ~5 ~1 ~3 ~5 ~3 ~5 minecraft:air
fill ~6 ~1 ~4 ~6 ~3 ~8 minecraft:air
fill ~7 ~1 ~3 ~7 ~2 ~9 minecraft:air
fill ~8 ~1 ~4 ~10 ~3 ~8 minecraft:air
fill ~8 ~1 ~9 ~9 ~3 ~9 minecraft:air
fill ~11 ~1 ~3 ~11 ~3 ~8 minecraft:air
fill ~13 ~1 ~2 ~14 ~9 ~2 minecraft:air
fill ~13 ~1 ~3 ~13 ~2 ~12 minecraft:air
fill ~14 ~1 ~5 ~14 ~2 ~7 minecraft:air
fill ~14 ~1 ~10 ~14 ~16 ~12 minecraft:air
setblock ~1 ~2 ~8 minecraft:air
fill ~2 ~2 ~1 ~3 ~2 ~1 minecraft:air
fill ~2 ~2 ~11 ~3 ~2 ~12 minecraft:air
fill ~4 ~2 ~3 ~4 ~3 ~3 minecraft:air
fill ~4 ~2 ~12 ~4 ~6 ~12 minecraft:air
fill ~5 ~2 ~1 ~5 ~3 ~1 minecraft:air
fill ~5 ~2 ~11 ~5 ~3 ~12 minecraft:air
fill ~6 ~2 ~3 ~6 ~3 ~3 minecraft:air
fill ~6 ~2 ~9 ~6 ~3 ~9 minecraft:air
fill ~6 ~2 ~12 ~6 ~6 ~12 minecraft:air
setblock ~7 ~2 ~1 minecraft:air
fill ~7 ~2 ~11 ~7 ~2 ~12 minecraft:air
fill ~8 ~2 ~3 ~8 ~3 ~3 minecraft:air
fill ~8 ~2 ~12 ~8 ~6 ~12 minecraft:air
fill ~9 ~2 ~1 ~9 ~3 ~1 minecraft:air
fill ~9 ~2 ~11 ~9 ~3 ~12 minecraft:air
fill ~10 ~2 ~3 ~10 ~3 ~3 minecraft:air
fill ~10 ~2 ~9 ~11 ~3 ~9 minecraft:air
fill ~10 ~2 ~12 ~10 ~6 ~12 minecraft:air
fill ~11 ~2 ~1 ~14 ~2 ~1 minecraft:air
fill ~11 ~2 ~11 ~12 ~2 ~12 minecraft:air
fill ~14 ~2 ~4 ~14 ~3 ~4 minecraft:air
fill ~14 ~2 ~8 ~14 ~3 ~8 minecraft:air
fill ~0 ~3 ~3 ~0 ~16 ~9 minecraft:air
fill ~1 ~3 ~10 ~1 ~6 ~12 minecraft:air
setblock ~2 ~3 ~1 minecraft:air
fill ~2 ~3 ~11 ~2 ~3 ~12 minecraft:air
setblock ~3 ~3 ~3 minecraft:air
fill ~3 ~3 ~12 ~3 ~4 ~12 minecraft:air
setblock ~4 ~3 ~1 minecraft:air
setblock ~4 ~3 ~11 minecraft:air
setblock ~6 ~3 ~1 minecraft:air
setblock ~6 ~3 ~11 minecraft:air
fill ~7 ~3 ~3 ~7 ~3 ~5 minecraft:air
fill ~7 ~3 ~7 ~7 ~3 ~9 minecraft:air
fill ~7 ~3 ~12 ~7 ~4 ~12 minecraft:air
setblock ~8 ~3 ~1 minecraft:air
setblock ~8 ~3 ~11 minecraft:air
setblock ~9 ~3 ~3 minecraft:air
setblock ~10 ~3 ~1 minecraft:air
setblock ~10 ~3 ~11 minecraft:air
fill ~11 ~3 ~12 ~12 ~4 ~12 minecraft:air
fill ~12 ~3 ~1 ~14 ~3 ~1 minecraft:air
setblock ~13 ~3 ~3 minecraft:air
fill ~13 ~3 ~5 ~14 ~3 ~5 minecraft:air
fill ~13 ~3 ~6 ~13 ~3 ~12 minecraft:air
setblock ~14 ~3 ~7 minecraft:air
fill ~1 ~4 ~3 ~1 ~10 ~9 minecraft:air
fill ~2 ~4 ~12 ~2 ~6 ~12 minecraft:air
fill ~5 ~4 ~12 ~5 ~6 ~12 minecraft:air
fill ~9 ~4 ~12 ~9 ~6 ~12 minecraft:air
fill ~13 ~4 ~1 ~14 ~8 ~1 minecraft:air
fill ~13 ~4 ~10 ~13 ~6 ~12 minecraft:air
fill ~0 ~5 ~0 ~2 ~6 ~0 minecraft:air
fill ~3 ~5 ~2 ~6 ~9 ~5 minecraft:air
fill ~3 ~5 ~6 ~3 ~9 ~10 minecraft:air
fill ~4 ~5 ~0 ~6 ~6 ~0 minecraft:air
fill ~4 ~5 ~8 ~5 ~9 ~10 minecraft:air
fill ~6 ~5 ~6 ~6 ~6 ~10 minecraft:air
fill ~7 ~5 ~3 ~10 ~5 ~10 minecraft:air
fill ~8 ~5 ~0 ~10 ~6 ~0 minecraft:air
fill ~11 ~5 ~2 ~11 ~6 ~9 minecraft:air
fill ~12 ~5 ~0 ~14 ~6 ~0 minecraft:air
fill ~12 ~5 ~12 ~12 ~6 ~12 minecraft:air
fill ~13 ~5 ~3 ~14 ~5 ~9 minecraft:air
fill ~7 ~6 ~2 ~10 ~9 ~5 minecraft:air
fill ~7 ~6 ~6 ~7 ~6 ~10 minecraft:air
fill ~8 ~6 ~7 ~8 ~9 ~10 minecraft:air
fill ~9 ~6 ~6 ~10 ~6 ~10 minecraft:air
fill ~11 ~6 ~10 ~11 ~9 ~10 minecraft:air
fill ~13 ~6 ~4 ~13 ~11 ~4 minecraft:air
fill ~13 ~6 ~6 ~13 ~13 ~6 minecraft:air
fill ~13 ~6 ~8 ~13 ~11 ~8 minecraft:air
fill ~14 ~6 ~3 ~14 ~16 ~9 minecraft:air
fill ~0 ~7 ~0 ~0 ~16 ~0 minecraft:air
fill ~1 ~7 ~10 ~1 ~8 ~11 minecraft:air
fill ~6 ~7 ~7 ~7 ~9 ~10 minecraft:air
fill ~9 ~7 ~7 ~11 ~10 ~9 minecraft:air
fill ~9 ~7 ~10 ~10 ~9 ~10 minecraft:air
fill ~11 ~7 ~2 ~11 ~9 ~5 minecraft:air
fill ~13 ~7 ~10 ~13 ~8 ~11 minecraft:air
fill ~14 ~7 ~0 ~14 ~16 ~0 minecraft:air
fill ~6 ~8 ~6 ~11 ~13 ~6 minecraft:air
fill ~13 ~8 ~3 ~13 ~10 ~3 minecraft:air
fill ~13 ~8 ~5 ~13 ~12 ~5 minecraft:air
fill ~13 ~8 ~7 ~13 ~12 ~7 minecraft:air
fill ~13 ~8 ~9 ~13 ~10 ~9 minecraft:air
fill ~0 ~9 ~1 ~0 ~16 ~2 minecraft:air
fill ~1 ~9 ~0 ~13 ~16 ~0 minecraft:air
setblock ~1 ~9 ~2 minecraft:air
setblock ~1 ~9 ~10 minecraft:air
fill ~1 ~9 ~12 ~13 ~16 ~12 minecraft:air
setblock ~13 ~9 ~10 minecraft:air
fill ~14 ~9 ~1 ~14 ~16 ~1 minecraft:air
fill ~1 ~10 ~1 ~13 ~16 ~1 minecraft:air
fill ~1 ~10 ~11 ~13 ~16 ~11 minecraft:air
fill ~3 ~10 ~3 ~11 ~10 ~5 minecraft:air
fill ~3 ~10 ~6 ~3 ~10 ~9 minecraft:air
fill ~4 ~10 ~8 ~5 ~10 ~9 minecraft:air
fill ~6 ~10 ~7 ~8 ~10 ~9 minecraft:air
fill ~14 ~10 ~2 ~14 ~16 ~2 minecraft:air
fill ~1 ~11 ~2 ~13 ~16 ~2 minecraft:air
fill ~1 ~11 ~4 ~1 ~11 ~8 minecraft:air
fill ~1 ~11 ~10 ~13 ~16 ~10 minecraft:air
fill ~3 ~11 ~4 ~11 ~11 ~5 minecraft:air
fill ~3 ~11 ~6 ~3 ~11 ~8 minecraft:air
fill ~4 ~11 ~8 ~5 ~11 ~8 minecraft:air
fill ~6 ~11 ~7 ~11 ~11 ~8 minecraft:air
fill ~1 ~12 ~3 ~13 ~16 ~3 minecraft:air
fill ~1 ~12 ~5 ~1 ~12 ~7 minecraft:air
fill ~1 ~12 ~9 ~13 ~16 ~9 minecraft:air
fill ~3 ~12 ~5 ~11 ~12 ~5 minecraft:air
fill ~3 ~12 ~6 ~3 ~12 ~7 minecraft:air
fill ~6 ~12 ~7 ~11 ~12 ~7 minecraft:air
fill ~1 ~13 ~4 ~13 ~16 ~4 minecraft:air
setblock ~1 ~13 ~6 minecraft:air
fill ~1 ~13 ~8 ~13 ~16 ~8 minecraft:air
setblock ~3 ~13 ~6 minecraft:air
fill ~1 ~14 ~5 ~13 ~16 ~5 minecraft:air
fill ~1 ~14 ~7 ~3 ~16 ~7 minecraft:air
fill ~6 ~14 ~7 ~13 ~16 ~7 minecraft:air
fill ~1 ~15 ~6 ~3 ~16 ~6 minecraft:air
fill ~6 ~15 ~6 ~13 ~16 ~6 minecraft:air
setblock ~4 ~16 ~7 minecraft:air
setblock ~5 ~16 ~6 minecraft:air

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

# the ground storey corners: spruce log posts carrying the jetty; the door frame: dark oak posts and a lintel
fill ~2 ~1 ~2 ~2 ~3 ~2 minecraft:spruce_log[axis=y]
fill ~2 ~1 ~10 ~2 ~3 ~10 minecraft:spruce_log[axis=y]
fill ~12 ~1 ~2 ~12 ~3 ~2 minecraft:spruce_log[axis=y]
fill ~12 ~1 ~5 ~12 ~2 ~5 minecraft:dark_oak_log[axis=y]
fill ~12 ~1 ~7 ~12 ~2 ~7 minecraft:dark_oak_log[axis=y]
fill ~12 ~1 ~10 ~12 ~3 ~10 minecraft:spruce_log[axis=y]
fill ~12 ~3 ~5 ~12 ~3 ~7 minecraft:dark_oak_log[axis=z]

# the door: a planked spruce door at z 6 in the east wall
setblock ~12 ~1 ~6 minecraft:spruce_door[facing=west,half=lower,hinge=right,open=false,powered=false]
setblock ~12 ~2 ~6 minecraft:spruce_door[facing=west,half=upper,hinge=right,open=false,powered=false]

# the upper floor: spruce planks at y 4, jettied one block out north and south on dark oak joists
fill ~2 ~4 ~1 ~12 ~4 ~1 minecraft:dark_oak_log[axis=z]
fill ~2 ~4 ~2 ~12 ~4 ~5 minecraft:spruce_planks
fill ~2 ~4 ~6 ~3 ~4 ~8 minecraft:spruce_planks
fill ~2 ~4 ~9 ~2 ~4 ~10 minecraft:spruce_planks
fill ~2 ~4 ~11 ~12 ~4 ~11 minecraft:dark_oak_log[axis=z]
setblock ~3 ~4 ~10 minecraft:spruce_planks
fill ~4 ~4 ~8 ~5 ~4 ~10 minecraft:spruce_planks
fill ~6 ~4 ~6 ~12 ~4 ~10 minecraft:spruce_planks

# the upper storey: close studding, a dark oak post every second block, daub between; the wall plate at y 8 along the jetties
fill ~2 ~5 ~1 ~2 ~7 ~1 minecraft:dark_oak_log[axis=y]
setblock ~2 ~5 ~2 minecraft:packed_mud
fill ~2 ~5 ~3 ~2 ~7 ~3 minecraft:dark_oak_log[axis=y]
setblock ~2 ~5 ~4 minecraft:mud_bricks
fill ~2 ~5 ~5 ~2 ~7 ~5 minecraft:dark_oak_log[axis=y]
setblock ~2 ~5 ~6 minecraft:mud_bricks
fill ~2 ~5 ~7 ~2 ~7 ~7 minecraft:dark_oak_log[axis=y]
setblock ~2 ~5 ~8 minecraft:packed_mud
fill ~2 ~5 ~9 ~2 ~7 ~9 minecraft:dark_oak_log[axis=y]
fill ~2 ~5 ~10 ~2 ~7 ~10 minecraft:packed_mud
fill ~2 ~5 ~11 ~2 ~7 ~11 minecraft:dark_oak_log[axis=y]
setblock ~3 ~5 ~1 minecraft:packed_mud
setblock ~3 ~5 ~11 minecraft:packed_mud
fill ~4 ~5 ~1 ~4 ~7 ~1 minecraft:dark_oak_log[axis=y]
fill ~4 ~5 ~11 ~4 ~7 ~11 minecraft:dark_oak_log[axis=y]
fill ~5 ~5 ~1 ~5 ~7 ~1 minecraft:packed_mud
fill ~5 ~5 ~11 ~5 ~7 ~11 minecraft:packed_mud
fill ~6 ~5 ~1 ~6 ~7 ~1 minecraft:dark_oak_log[axis=y]
fill ~6 ~5 ~11 ~6 ~7 ~11 minecraft:dark_oak_log[axis=y]
setblock ~7 ~5 ~1 minecraft:packed_mud
setblock ~7 ~5 ~11 minecraft:packed_mud
fill ~8 ~5 ~1 ~8 ~7 ~1 minecraft:dark_oak_log[axis=y]
fill ~8 ~5 ~11 ~8 ~7 ~11 minecraft:dark_oak_log[axis=y]
fill ~9 ~5 ~1 ~9 ~6 ~1 minecraft:mud_bricks
setblock ~9 ~5 ~11 minecraft:mud_bricks
fill ~10 ~5 ~1 ~10 ~7 ~1 minecraft:dark_oak_log[axis=y]
fill ~10 ~5 ~11 ~10 ~7 ~11 minecraft:dark_oak_log[axis=y]
setblock ~11 ~5 ~1 minecraft:mud_bricks
setblock ~11 ~5 ~11 minecraft:mud_bricks
fill ~12 ~5 ~1 ~12 ~7 ~1 minecraft:dark_oak_log[axis=y]
setblock ~12 ~5 ~2 minecraft:packed_mud
fill ~12 ~5 ~3 ~12 ~7 ~3 minecraft:dark_oak_log[axis=y]
setblock ~12 ~5 ~4 minecraft:packed_mud
fill ~12 ~5 ~5 ~12 ~7 ~5 minecraft:dark_oak_log[axis=y]
fill ~12 ~5 ~6 ~12 ~7 ~6 minecraft:packed_mud
fill ~12 ~5 ~7 ~12 ~7 ~7 minecraft:dark_oak_log[axis=y]
setblock ~12 ~5 ~8 minecraft:packed_mud
fill ~12 ~5 ~9 ~12 ~7 ~9 minecraft:dark_oak_log[axis=y]
setblock ~12 ~5 ~10 minecraft:packed_mud
fill ~12 ~5 ~11 ~12 ~7 ~11 minecraft:dark_oak_log[axis=y]
fill ~2 ~6 ~2 ~2 ~7 ~2 minecraft:mud_bricks
setblock ~2 ~6 ~6 minecraft:packed_mud
fill ~9 ~6 ~11 ~9 ~7 ~11 minecraft:packed_mud
setblock ~12 ~6 ~2 minecraft:mud_bricks
setblock ~12 ~6 ~10 minecraft:mud_bricks
setblock ~2 ~7 ~6 minecraft:mud_bricks
setblock ~9 ~7 ~1 minecraft:packed_mud
setblock ~12 ~7 ~2 minecraft:packed_mud
setblock ~12 ~7 ~10 minecraft:packed_mud
fill ~2 ~8 ~1 ~12 ~8 ~1 minecraft:dark_oak_log[axis=x]
fill ~2 ~8 ~11 ~12 ~8 ~11 minecraft:dark_oak_log[axis=x]

# brackets under the jetty: upside-down dark oak stairs against the ground storey wall
setblock ~3 ~3 ~1 minecraft:dark_oak_stairs[facing=south,half=top]
setblock ~3 ~3 ~11 minecraft:dark_oak_stairs[facing=north,half=top]
setblock ~7 ~3 ~1 minecraft:dark_oak_stairs[facing=south,half=top]
setblock ~7 ~3 ~11 minecraft:dark_oak_stairs[facing=north,half=top]
setblock ~11 ~3 ~1 minecraft:dark_oak_stairs[facing=south,half=top]
setblock ~11 ~3 ~11 minecraft:dark_oak_stairs[facing=north,half=top]

# windows: glass panes in both storeys; the upper ones of the long walls two tall
setblock ~2 ~2 ~6 minecraft:glass_pane[north=true,east=false,south=true,west=true]
setblock ~5 ~2 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~5 ~2 ~10 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~9 ~2 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~9 ~2 ~10 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~2 ~6 ~4 ~2 ~7 ~4 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~2 ~6 ~8 ~2 ~7 ~8 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~3 ~6 ~1 ~3 ~7 ~1 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~3 ~6 ~11 ~3 ~7 ~11 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~7 ~6 ~1 ~7 ~7 ~1 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~7 ~6 ~11 ~7 ~7 ~11 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~11 ~6 ~1 ~11 ~7 ~1 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~11 ~6 ~11 ~11 ~7 ~11 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~12 ~6 ~4 ~12 ~7 ~4 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~12 ~6 ~8 ~12 ~7 ~8 minecraft:glass_pane[north=true,east=false,south=true,west=false]

# the roof: spruce stairs from the eaves at y 8 (z 0 and 12) a block a row to a ridge of dark oak planks at y 14 (z 6), over x 1..13, a block proud of both gables
fill ~1 ~8 ~0 ~13 ~8 ~0 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~1 ~8 ~12 ~13 ~8 ~12 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~1 ~9 ~1 ~7 ~9 ~1 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~1 ~9 ~11 ~4 ~9 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~5 ~9 ~11 minecraft:mossy_cobblestone_stairs[facing=north,half=bottom]
fill ~6 ~9 ~11 ~13 ~9 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~8 ~9 ~1 minecraft:mossy_cobblestone_stairs[facing=south,half=bottom]
fill ~9 ~9 ~1 ~13 ~9 ~1 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~1 ~10 ~2 ~3 ~10 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~1 ~10 ~10 ~13 ~10 ~10 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~4 ~10 ~2 minecraft:mossy_cobblestone_stairs[facing=south,half=bottom]
fill ~5 ~10 ~2 ~13 ~10 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~1 ~11 ~3 ~13 ~11 ~3 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~1 ~11 ~9 ~9 ~11 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~10 ~11 ~9 minecraft:mossy_cobblestone_stairs[facing=north,half=bottom]
fill ~11 ~11 ~9 ~13 ~11 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~1 ~12 ~4 ~10 ~12 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~1 ~12 ~8 ~13 ~12 ~8 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~11 ~12 ~4 minecraft:mossy_cobblestone_stairs[facing=south,half=bottom]
fill ~12 ~12 ~4 ~13 ~12 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~1 ~13 ~5 ~13 ~13 ~5 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~1 ~13 ~7 ~3 ~13 ~7 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~6 ~13 ~7 ~13 ~13 ~7 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~1 ~14 ~6 ~3 ~14 ~6 minecraft:dark_oak_planks
fill ~6 ~14 ~6 ~13 ~14 ~6 minecraft:dark_oak_planks

# the gables at x 2 and x 12: spruce boards under the roof on a collar beam at y 10, a dark oak king post, a loft window either side of it
fill ~2 ~8 ~2 ~2 ~9 ~3 minecraft:spruce_planks
setblock ~2 ~8 ~4 minecraft:stripped_spruce_wood
fill ~2 ~8 ~5 ~2 ~9 ~5 minecraft:spruce_planks
fill ~2 ~8 ~6 ~2 ~9 ~6 minecraft:dark_oak_log[axis=y]
fill ~2 ~8 ~7 ~2 ~9 ~7 minecraft:spruce_planks
fill ~2 ~8 ~8 ~2 ~8 ~9 minecraft:stripped_spruce_wood
fill ~2 ~8 ~10 ~2 ~9 ~10 minecraft:spruce_planks
fill ~12 ~8 ~2 ~12 ~8 ~5 minecraft:spruce_planks
fill ~12 ~8 ~6 ~12 ~9 ~6 minecraft:dark_oak_log[axis=y]
fill ~12 ~8 ~7 ~12 ~8 ~10 minecraft:spruce_planks
setblock ~2 ~9 ~4 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~2 ~9 ~8 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~2 ~9 ~9 minecraft:spruce_planks
fill ~12 ~9 ~2 ~12 ~9 ~3 minecraft:spruce_planks
setblock ~12 ~9 ~4 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~12 ~9 ~5 minecraft:spruce_planks
setblock ~12 ~9 ~7 minecraft:spruce_planks
setblock ~12 ~9 ~8 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~12 ~9 ~9 ~12 ~9 ~10 minecraft:spruce_planks
fill ~2 ~10 ~3 ~2 ~10 ~9 minecraft:dark_oak_log[axis=z]
fill ~12 ~10 ~3 ~12 ~10 ~9 minecraft:dark_oak_log[axis=z]
fill ~2 ~11 ~4 ~2 ~11 ~5 minecraft:spruce_planks
fill ~2 ~11 ~6 ~2 ~13 ~6 minecraft:dark_oak_log[axis=y]
fill ~2 ~11 ~7 ~2 ~11 ~8 minecraft:spruce_planks
fill ~12 ~11 ~4 ~12 ~11 ~5 minecraft:spruce_planks
fill ~12 ~11 ~6 ~12 ~13 ~6 minecraft:dark_oak_log[axis=y]
fill ~12 ~11 ~7 ~12 ~11 ~8 minecraft:spruce_planks
setblock ~2 ~12 ~5 minecraft:spruce_planks
setblock ~2 ~12 ~7 minecraft:spruce_planks
setblock ~12 ~12 ~5 minecraft:spruce_planks
setblock ~12 ~12 ~7 minecraft:spruce_planks

# the eaves: soffits of upside-down spruce stairs under both eaves; a few shingles gone to moss (mossy cobblestone stairs)
fill ~1 ~7 ~0 ~13 ~7 ~0 minecraft:spruce_stairs[facing=south,half=top]
fill ~1 ~7 ~12 ~13 ~7 ~12 minecraft:spruce_stairs[facing=north,half=top]

# the chimney: a stack of cobblestone and stone (x 4..5, z 6..7) from the hearth through both floors and the ridge, two smouldering campfires on top
fill ~4 ~1 ~6 ~4 ~3 ~6 minecraft:cobblestone
setblock ~4 ~1 ~7 minecraft:stone_bricks
setblock ~5 ~1 ~6 minecraft:smoker[facing=north,lit=false]
setblock ~5 ~1 ~7 minecraft:mossy_cobblestone
setblock ~4 ~2 ~7 minecraft:cobblestone
fill ~5 ~2 ~6 ~5 ~4 ~6 minecraft:mossy_cobblestone
setblock ~5 ~2 ~7 minecraft:cracked_stone_bricks
setblock ~4 ~3 ~7 minecraft:stone_bricks
setblock ~5 ~3 ~7 minecraft:mossy_cobblestone
setblock ~4 ~4 ~6 minecraft:cracked_stone_bricks
fill ~4 ~4 ~7 ~5 ~4 ~7 minecraft:cobblestone
setblock ~4 ~5 ~6 minecraft:stone_bricks
setblock ~4 ~5 ~7 minecraft:cobblestone
setblock ~5 ~5 ~6 minecraft:cobblestone
setblock ~5 ~5 ~7 minecraft:mossy_cobblestone
setblock ~4 ~6 ~6 minecraft:cobblestone
setblock ~4 ~6 ~7 minecraft:mossy_cobblestone
setblock ~5 ~6 ~6 minecraft:cracked_stone_bricks
setblock ~5 ~6 ~7 minecraft:stone_bricks
setblock ~4 ~7 ~6 minecraft:mossy_cobblestone
setblock ~4 ~7 ~7 minecraft:cracked_stone_bricks
fill ~5 ~7 ~6 ~5 ~8 ~7 minecraft:cobblestone
fill ~4 ~8 ~6 ~4 ~8 ~7 minecraft:stone_bricks
setblock ~4 ~9 ~6 minecraft:cobblestone
setblock ~4 ~9 ~7 minecraft:stone_bricks
setblock ~5 ~9 ~6 minecraft:stone_bricks
fill ~5 ~9 ~7 ~5 ~11 ~7 minecraft:cobblestone
setblock ~4 ~10 ~6 minecraft:mossy_cobblestone
fill ~4 ~10 ~7 ~4 ~12 ~7 minecraft:cobblestone
fill ~5 ~10 ~6 ~5 ~14 ~6 minecraft:cobblestone
setblock ~4 ~11 ~6 minecraft:cracked_stone_bricks
fill ~4 ~12 ~6 ~4 ~13 ~6 minecraft:cobblestone
setblock ~5 ~12 ~7 minecraft:stone_bricks
fill ~4 ~13 ~7 ~4 ~14 ~7 minecraft:mossy_cobblestone
fill ~5 ~13 ~7 ~5 ~15 ~7 minecraft:cobblestone
setblock ~4 ~14 ~6 minecraft:mossy_cobblestone
fill ~4 ~15 ~6 ~4 ~15 ~7 minecraft:cobblestone
setblock ~5 ~15 ~6 minecraft:stone_bricks
setblock ~4 ~16 ~6 minecraft:campfire[lit=true,signal_fire=false,facing=east]
setblock ~5 ~16 ~7 minecraft:campfire[lit=true,signal_fire=false,facing=east]

# shutters and window boxes: spruce trapdoors open against the wall beside the ground windows and the gable windows above the porch; boxes of potted plants under the upper windows of the long walls
setblock ~4 ~2 ~1 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
setblock ~4 ~2 ~11 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
setblock ~6 ~2 ~1 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
setblock ~6 ~2 ~11 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
setblock ~8 ~2 ~1 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
setblock ~8 ~2 ~11 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
setblock ~10 ~2 ~1 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
setblock ~10 ~2 ~11 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
setblock ~3 ~5 ~0 minecraft:spruce_trapdoor[facing=north,half=top,open=false]
setblock ~3 ~5 ~12 minecraft:spruce_trapdoor[facing=south,half=top,open=false]
setblock ~7 ~5 ~0 minecraft:spruce_trapdoor[facing=north,half=top,open=false]
setblock ~7 ~5 ~12 minecraft:spruce_trapdoor[facing=south,half=top,open=false]
setblock ~11 ~5 ~0 minecraft:spruce_trapdoor[facing=north,half=top,open=false]
setblock ~11 ~5 ~12 minecraft:spruce_trapdoor[facing=south,half=top,open=false]
setblock ~3 ~6 ~0 minecraft:potted_fern
setblock ~3 ~6 ~12 minecraft:potted_azure_bluet
setblock ~7 ~6 ~0 minecraft:potted_red_tulip
setblock ~7 ~6 ~12 minecraft:potted_poppy
setblock ~11 ~6 ~0 minecraft:potted_fern
setblock ~11 ~6 ~12 minecraft:potted_fern
fill ~13 ~6 ~3 ~13 ~7 ~3 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
fill ~13 ~6 ~5 ~13 ~7 ~5 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
fill ~13 ~6 ~7 ~13 ~7 ~7 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
fill ~13 ~6 ~9 ~13 ~7 ~9 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]

# the porch: spruce posts at the corners, a fence rail on the east side with the way in at z 5..7, a dark oak slab roof at y 4, a lantern and the hanging sign of the house under it
fill ~14 ~1 ~3 ~14 ~3 ~3 minecraft:spruce_log[axis=y]
setblock ~14 ~1 ~4 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
setblock ~14 ~1 ~8 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
fill ~14 ~1 ~9 ~14 ~3 ~9 minecraft:spruce_log[axis=y]
setblock ~13 ~3 ~4 minecraft:lantern[hanging=true]
setblock ~14 ~3 ~6 minecraft:spruce_hanging_sign[rotation=12,attached=false]{front_text:{messages:['{"text":""}','{"text":"ALE AND"}','{"text":"BREAD"}','{"text":""}']}}
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
fill ~7 ~5 ~2 ~8 ~5 ~2 minecraft:red_wool
fill ~9 ~5 ~2 ~10 ~5 ~2 minecraft:brown_wool
setblock ~11 ~5 ~10 minecraft:chest[facing=north]
setblock ~8 ~6 ~6 minecraft:lantern[hanging=true]
fill ~6 ~7 ~6 ~11 ~7 ~6 minecraft:dark_oak_log[axis=x]

# weather: a cobweb under the jetty and moss on the ground by the plinth
setblock ~2 ~1 ~1 minecraft:moss_carpet
setblock ~12 ~3 ~11 minecraft:cobweb
