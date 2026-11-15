# size 21 12 17
# frostmarch/refuge: pass refuge of Frostmarch: a stone hut under a snow-laden timber roof with a doorway on every side, a hearth and chimney, a lean-to over the woodpile, a brazier on a pedestal, a trough of ice and a cairn on the road to the pass.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Hut x 6..14, z 4..12 (walls y 1..4, plate y 4), interior x 7..13, z 5..11; a doorway of three on each side: north and south x 9..11, west and east z 7..9, y 1..3 (a dark oak lintel at y 4). The lane of the map starts at the centre (10, 8).
# Roof: gable along x over x 5..15, z 3..13, eaves y 4, ridge y 9 at z 8. Lean-to x 15..17, z 5..11 on the east face (roof y 5, 4, 3). Chimney: the stack x 7..8, z 5..6 inside the north-west corner, cap y 10, smoke at y 11.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~2 ~11 ~4 minecraft:air
fill ~0 ~1 ~6 ~1 ~11 ~10 minecraft:air
fill ~0 ~1 ~13 ~0 ~11 ~13 minecraft:air
fill ~0 ~1 ~15 ~0 ~11 ~16 minecraft:air
fill ~1 ~1 ~5 ~2 ~11 ~5 minecraft:air
fill ~1 ~1 ~11 ~1 ~11 ~12 minecraft:air
fill ~1 ~1 ~14 ~1 ~11 ~14 minecraft:air
fill ~1 ~1 ~16 ~1 ~11 ~16 minecraft:air
fill ~2 ~1 ~7 ~3 ~2 ~11 minecraft:air
fill ~2 ~1 ~12 ~2 ~2 ~13 minecraft:air
fill ~3 ~1 ~1 ~3 ~11 ~4 minecraft:air
fill ~3 ~1 ~13 ~3 ~2 ~13 minecraft:air
fill ~4 ~1 ~0 ~11 ~11 ~0 minecraft:air
fill ~4 ~1 ~2 ~4 ~2 ~13 minecraft:air
fill ~5 ~1 ~1 ~6 ~11 ~2 minecraft:air
fill ~5 ~1 ~7 ~5 ~7 ~9 minecraft:air
fill ~7 ~1 ~2 ~11 ~11 ~2 minecraft:air
fill ~7 ~1 ~7 ~8 ~3 ~9 minecraft:air
fill ~7 ~1 ~14 ~8 ~11 ~15 minecraft:air
fill ~7 ~1 ~16 ~7 ~11 ~16 minecraft:air
fill ~8 ~1 ~1 ~12 ~11 ~1 minecraft:air
fill ~9 ~1 ~3 ~11 ~3 ~3 minecraft:air
fill ~9 ~1 ~5 ~11 ~5 ~11 minecraft:air
fill ~9 ~1 ~13 ~11 ~3 ~16 minecraft:air
fill ~12 ~1 ~7 ~13 ~3 ~9 minecraft:air
fill ~12 ~1 ~14 ~12 ~11 ~15 minecraft:air
fill ~13 ~1 ~0 ~14 ~11 ~2 minecraft:air
fill ~13 ~1 ~16 ~13 ~11 ~16 minecraft:air
fill ~14 ~1 ~15 ~14 ~1 ~16 minecraft:air
fill ~15 ~1 ~1 ~15 ~11 ~1 minecraft:air
fill ~15 ~1 ~7 ~15 ~4 ~9 minecraft:air
fill ~15 ~1 ~14 ~16 ~11 ~14 minecraft:air
fill ~15 ~1 ~16 ~15 ~11 ~16 minecraft:air
fill ~16 ~1 ~0 ~18 ~11 ~0 minecraft:air
fill ~16 ~1 ~2 ~16 ~3 ~2 minecraft:air
fill ~16 ~1 ~4 ~16 ~11 ~4 minecraft:air
fill ~16 ~1 ~8 ~16 ~2 ~8 minecraft:air
fill ~16 ~1 ~12 ~17 ~11 ~13 minecraft:air
fill ~16 ~1 ~15 ~16 ~1 ~16 minecraft:air
fill ~17 ~1 ~1 ~18 ~11 ~1 minecraft:air
fill ~17 ~1 ~7 ~17 ~2 ~10 minecraft:air
fill ~17 ~1 ~16 ~18 ~11 ~16 minecraft:air
fill ~18 ~1 ~3 ~18 ~11 ~12 minecraft:air
fill ~19 ~1 ~2 ~19 ~11 ~2 minecraft:air
fill ~19 ~1 ~6 ~19 ~11 ~9 minecraft:air
fill ~19 ~1 ~11 ~19 ~11 ~12 minecraft:air
fill ~19 ~1 ~15 ~19 ~11 ~15 minecraft:air
fill ~20 ~1 ~1 ~20 ~11 ~5 minecraft:air
fill ~20 ~1 ~7 ~20 ~11 ~12 minecraft:air
fill ~20 ~1 ~14 ~20 ~11 ~16 minecraft:air
fill ~0 ~2 ~5 ~0 ~11 ~5 minecraft:air
fill ~0 ~2 ~11 ~0 ~11 ~12 minecraft:air
fill ~0 ~2 ~14 ~0 ~11 ~14 minecraft:air
fill ~1 ~2 ~13 ~1 ~11 ~13 minecraft:air
fill ~1 ~2 ~15 ~1 ~11 ~15 minecraft:air
fill ~2 ~2 ~6 ~2 ~11 ~6 minecraft:air
fill ~3 ~2 ~0 ~3 ~11 ~0 minecraft:air
fill ~3 ~2 ~5 ~3 ~11 ~5 minecraft:air
fill ~3 ~2 ~15 ~5 ~11 ~15 minecraft:air
fill ~3 ~2 ~16 ~3 ~11 ~16 minecraft:air
fill ~4 ~2 ~1 ~4 ~11 ~1 minecraft:air
fill ~5 ~2 ~3 ~8 ~3 ~3 minecraft:air
fill ~5 ~2 ~4 ~5 ~4 ~6 minecraft:air
fill ~5 ~2 ~10 ~5 ~3 ~13 minecraft:air
fill ~6 ~2 ~13 ~8 ~3 ~13 minecraft:air
fill ~8 ~2 ~10 ~8 ~2 ~11 minecraft:air
fill ~8 ~2 ~16 ~8 ~11 ~16 minecraft:air
fill ~12 ~2 ~0 ~12 ~11 ~0 minecraft:air
fill ~12 ~2 ~3 ~17 ~3 ~3 minecraft:air
fill ~12 ~2 ~10 ~12 ~2 ~11 minecraft:air
fill ~12 ~2 ~13 ~14 ~3 ~13 minecraft:air
fill ~12 ~2 ~16 ~12 ~11 ~16 minecraft:air
fill ~13 ~2 ~11 ~13 ~5 ~11 minecraft:air
fill ~13 ~2 ~14 ~14 ~11 ~14 minecraft:air
fill ~14 ~2 ~16 ~14 ~11 ~16 minecraft:air
fill ~15 ~2 ~0 ~15 ~11 ~0 minecraft:air
fill ~15 ~2 ~4 ~15 ~4 ~4 minecraft:air
fill ~15 ~2 ~12 ~15 ~3 ~13 minecraft:air
fill ~16 ~2 ~1 ~16 ~11 ~1 minecraft:air
fill ~16 ~2 ~7 ~16 ~3 ~7 minecraft:air
fill ~16 ~2 ~9 ~16 ~3 ~9 minecraft:air
fill ~16 ~2 ~16 ~16 ~11 ~16 minecraft:air
fill ~17 ~2 ~4 ~17 ~11 ~4 minecraft:air
setblock ~17 ~2 ~6 minecraft:air
fill ~17 ~2 ~14 ~17 ~11 ~14 minecraft:air
fill ~18 ~2 ~2 ~18 ~11 ~2 minecraft:air
fill ~18 ~2 ~15 ~18 ~11 ~15 minecraft:air
fill ~19 ~2 ~0 ~20 ~11 ~0 minecraft:air
fill ~19 ~2 ~1 ~19 ~11 ~1 minecraft:air
fill ~19 ~2 ~3 ~19 ~11 ~5 minecraft:air
fill ~19 ~2 ~10 ~19 ~11 ~10 minecraft:air
fill ~19 ~2 ~16 ~19 ~11 ~16 minecraft:air
fill ~20 ~2 ~6 ~20 ~11 ~6 minecraft:air
fill ~20 ~2 ~13 ~20 ~11 ~13 minecraft:air
fill ~2 ~3 ~7 ~3 ~11 ~10 minecraft:air
fill ~2 ~3 ~11 ~2 ~11 ~11 minecraft:air
fill ~2 ~3 ~13 ~2 ~11 ~16 minecraft:air
fill ~3 ~3 ~14 ~3 ~11 ~14 minecraft:air
fill ~4 ~3 ~2 ~4 ~11 ~11 minecraft:air
fill ~4 ~3 ~13 ~4 ~11 ~14 minecraft:air
fill ~4 ~3 ~16 ~5 ~11 ~16 minecraft:air
fill ~5 ~3 ~14 ~6 ~11 ~14 minecraft:air
fill ~6 ~3 ~15 ~6 ~11 ~16 minecraft:air
fill ~7 ~3 ~10 ~7 ~5 ~11 minecraft:air
setblock ~8 ~3 ~11 minecraft:air
fill ~12 ~3 ~5 ~13 ~3 ~5 minecraft:air
setblock ~12 ~3 ~11 minecraft:air
fill ~13 ~3 ~6 ~13 ~6 ~6 minecraft:air
fill ~13 ~3 ~10 ~13 ~6 ~10 minecraft:air
fill ~13 ~3 ~15 ~17 ~11 ~15 minecraft:air
fill ~16 ~3 ~5 ~16 ~3 ~6 minecraft:air
fill ~16 ~3 ~10 ~16 ~3 ~11 minecraft:air
fill ~18 ~3 ~14 ~18 ~11 ~14 minecraft:air
fill ~19 ~3 ~13 ~19 ~11 ~14 minecraft:air
fill ~2 ~4 ~12 ~2 ~11 ~12 minecraft:air
fill ~3 ~4 ~11 ~3 ~11 ~13 minecraft:air
fill ~4 ~4 ~12 ~4 ~11 ~12 minecraft:air
fill ~5 ~4 ~10 ~5 ~4 ~12 minecraft:air
fill ~7 ~4 ~7 ~7 ~7 ~9 minecraft:air
fill ~9 ~4 ~14 ~11 ~11 ~16 minecraft:air
fill ~13 ~4 ~5 ~13 ~5 ~5 minecraft:air
fill ~13 ~4 ~7 ~13 ~7 ~9 minecraft:air
fill ~15 ~4 ~5 ~15 ~4 ~6 minecraft:air
fill ~15 ~4 ~10 ~15 ~4 ~12 minecraft:air
fill ~16 ~4 ~3 ~17 ~11 ~3 minecraft:air
fill ~17 ~4 ~5 ~17 ~11 ~11 minecraft:air
fill ~5 ~5 ~3 ~11 ~11 ~3 minecraft:air
fill ~5 ~5 ~5 ~5 ~5 ~6 minecraft:air
fill ~5 ~5 ~10 ~5 ~5 ~11 minecraft:air
fill ~5 ~5 ~13 ~15 ~11 ~13 minecraft:air
fill ~8 ~5 ~7 ~8 ~5 ~11 minecraft:air
fill ~12 ~5 ~2 ~12 ~11 ~3 minecraft:air
fill ~12 ~5 ~5 ~12 ~5 ~11 minecraft:air
fill ~13 ~5 ~3 ~15 ~11 ~3 minecraft:air
fill ~16 ~5 ~5 ~16 ~11 ~11 minecraft:air
fill ~18 ~5 ~13 ~18 ~11 ~13 minecraft:air
fill ~5 ~6 ~4 ~15 ~11 ~4 minecraft:air
setblock ~5 ~6 ~6 minecraft:air
setblock ~5 ~6 ~10 minecraft:air
fill ~5 ~6 ~12 ~15 ~11 ~12 minecraft:air
setblock ~7 ~6 ~10 minecraft:air
fill ~8 ~6 ~7 ~8 ~6 ~10 minecraft:air
fill ~9 ~6 ~6 ~12 ~6 ~10 minecraft:air
fill ~15 ~6 ~6 ~15 ~6 ~10 minecraft:air
fill ~17 ~6 ~2 ~17 ~11 ~2 minecraft:air
fill ~3 ~7 ~6 ~3 ~11 ~6 minecraft:air
fill ~5 ~7 ~5 ~6 ~11 ~5 minecraft:air
fill ~5 ~7 ~11 ~15 ~11 ~11 minecraft:air
fill ~7 ~7 ~1 ~7 ~11 ~1 minecraft:air
fill ~8 ~7 ~7 ~12 ~7 ~9 minecraft:air
fill ~9 ~7 ~5 ~15 ~11 ~5 minecraft:air
fill ~15 ~7 ~2 ~16 ~11 ~2 minecraft:air
fill ~15 ~7 ~7 ~15 ~7 ~9 minecraft:air
fill ~5 ~8 ~6 ~6 ~11 ~6 minecraft:air
setblock ~5 ~8 ~8 minecraft:air
fill ~5 ~8 ~10 ~15 ~11 ~10 minecraft:air
fill ~7 ~8 ~8 ~13 ~8 ~8 minecraft:air
fill ~9 ~8 ~6 ~15 ~11 ~6 minecraft:air
setblock ~15 ~8 ~8 minecraft:air
fill ~5 ~9 ~7 ~15 ~11 ~7 minecraft:air
fill ~5 ~9 ~9 ~15 ~11 ~9 minecraft:air
fill ~5 ~10 ~8 ~15 ~11 ~8 minecraft:air

# the floor of the hut: spruce boards between stone flags down the two doorway strips and round the hearth
setblock ~7 ~0 ~5 minecraft:andesite
setblock ~7 ~0 ~6 minecraft:stone_bricks
setblock ~7 ~0 ~7 minecraft:cracked_stone_bricks
fill ~7 ~0 ~8 ~7 ~0 ~9 minecraft:stone_bricks
setblock ~7 ~0 ~10 minecraft:dark_oak_planks
setblock ~7 ~0 ~11 minecraft:spruce_planks
fill ~8 ~0 ~5 ~8 ~0 ~9 minecraft:stone_bricks
fill ~8 ~0 ~10 ~8 ~0 ~11 minecraft:spruce_planks
setblock ~9 ~0 ~5 minecraft:cracked_stone_bricks
fill ~9 ~0 ~6 ~9 ~0 ~7 minecraft:stone_bricks
fill ~9 ~0 ~8 ~11 ~0 ~8 minecraft:andesite
fill ~9 ~0 ~9 ~11 ~0 ~9 minecraft:stone_bricks
fill ~9 ~0 ~10 ~10 ~0 ~10 minecraft:stone_bricks
setblock ~9 ~0 ~11 minecraft:stone_bricks
fill ~10 ~0 ~5 ~11 ~0 ~5 minecraft:stone_bricks
fill ~10 ~0 ~6 ~11 ~0 ~6 minecraft:andesite
setblock ~10 ~0 ~7 minecraft:cracked_stone_bricks
setblock ~10 ~0 ~11 minecraft:cracked_stone_bricks
fill ~11 ~0 ~7 ~12 ~0 ~7 minecraft:stone_bricks
setblock ~11 ~0 ~10 minecraft:cobblestone
setblock ~11 ~0 ~11 minecraft:stone_bricks
fill ~12 ~0 ~5 ~13 ~0 ~6 minecraft:spruce_planks
setblock ~12 ~0 ~8 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~9 minecraft:andesite
fill ~12 ~0 ~10 ~13 ~0 ~11 minecraft:spruce_planks
setblock ~13 ~0 ~7 minecraft:cobblestone
setblock ~13 ~0 ~8 minecraft:andesite
setblock ~13 ~0 ~9 minecraft:stone_bricks

# the four approaches: trodden mud, gravel and cobble in strips three wide from each doorway to the edge of the piece; flags at the doorsteps
fill ~0 ~0 ~7 ~0 ~0 ~9 minecraft:packed_mud
fill ~1 ~0 ~7 ~2 ~0 ~8 minecraft:gravel
setblock ~1 ~0 ~9 minecraft:packed_mud
setblock ~2 ~0 ~9 minecraft:gravel
setblock ~3 ~0 ~7 minecraft:coarse_dirt
setblock ~3 ~0 ~8 minecraft:gravel
setblock ~3 ~0 ~9 minecraft:coarse_dirt
setblock ~4 ~0 ~7 minecraft:gravel
setblock ~4 ~0 ~8 minecraft:packed_mud
setblock ~4 ~0 ~9 minecraft:gravel
setblock ~5 ~0 ~7 minecraft:stone_bricks
setblock ~5 ~0 ~8 minecraft:cracked_stone_bricks
setblock ~5 ~0 ~9 minecraft:cobblestone
fill ~9 ~0 ~0 ~10 ~0 ~0 minecraft:gravel
fill ~9 ~0 ~1 ~10 ~0 ~1 minecraft:packed_mud
setblock ~9 ~0 ~2 minecraft:gravel
setblock ~9 ~0 ~3 minecraft:stone_bricks
setblock ~9 ~0 ~13 minecraft:stone_bricks
fill ~9 ~0 ~14 ~9 ~0 ~15 minecraft:packed_mud
setblock ~9 ~0 ~16 minecraft:gravel
setblock ~10 ~0 ~2 minecraft:packed_mud
setblock ~10 ~0 ~3 minecraft:andesite
setblock ~10 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~10 ~0 ~14 minecraft:cobblestone
setblock ~10 ~0 ~15 minecraft:gravel
setblock ~10 ~0 ~16 minecraft:cobblestone
setblock ~11 ~0 ~0 minecraft:packed_mud
fill ~11 ~0 ~1 ~11 ~0 ~2 minecraft:gravel
setblock ~11 ~0 ~3 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~13 minecraft:stone_bricks
fill ~11 ~0 ~14 ~11 ~0 ~15 minecraft:gravel
setblock ~11 ~0 ~16 minecraft:coarse_dirt

# under the lean-to: gravel and packed mud, flags at the east doorstep
setblock ~15 ~0 ~5 minecraft:packed_mud
setblock ~15 ~0 ~6 minecraft:gravel
fill ~15 ~0 ~7 ~15 ~0 ~8 minecraft:cobblestone
setblock ~15 ~0 ~9 minecraft:stone_bricks
setblock ~15 ~0 ~10 minecraft:packed_mud
setblock ~15 ~0 ~11 minecraft:gravel
fill ~16 ~0 ~5 ~17 ~0 ~5 minecraft:coarse_dirt
fill ~16 ~0 ~6 ~16 ~0 ~7 minecraft:coarse_dirt
fill ~16 ~0 ~8 ~16 ~0 ~10 minecraft:packed_mud
setblock ~16 ~0 ~11 minecraft:coarse_dirt
setblock ~17 ~0 ~6 minecraft:gravel
fill ~17 ~0 ~7 ~18 ~0 ~8 minecraft:packed_mud
setblock ~17 ~0 ~9 minecraft:packed_mud
setblock ~17 ~0 ~10 minecraft:gravel
setblock ~17 ~0 ~11 minecraft:packed_mud
setblock ~18 ~0 ~9 minecraft:cobblestone
setblock ~19 ~0 ~7 minecraft:gravel
setblock ~19 ~0 ~8 minecraft:cobblestone
setblock ~19 ~0 ~9 minecraft:packed_mud
fill ~20 ~0 ~7 ~20 ~0 ~8 minecraft:packed_mud
setblock ~20 ~0 ~9 minecraft:gravel

# the walls: cobblestone at the foot (y 1), stone bricks above (y 2..3), a plate of dark oak logs at y 4 that is the lintel of the doorways, dark oak posts at the corners and at the jambs of the doorways
fill ~6 ~1 ~4 ~6 ~3 ~4 minecraft:dark_oak_log[axis=y]
setblock ~6 ~1 ~5 minecraft:stone
setblock ~6 ~1 ~11 minecraft:cobblestone
fill ~6 ~1 ~12 ~6 ~3 ~12 minecraft:dark_oak_log[axis=y]
setblock ~7 ~1 ~4 minecraft:cobbled_deepslate
setblock ~7 ~1 ~12 minecraft:cobblestone
setblock ~13 ~1 ~4 minecraft:cobblestone
setblock ~13 ~1 ~12 minecraft:andesite
fill ~14 ~1 ~4 ~14 ~3 ~4 minecraft:dark_oak_log[axis=y]
setblock ~14 ~1 ~5 minecraft:stone
setblock ~14 ~1 ~11 minecraft:cobblestone
fill ~14 ~1 ~12 ~14 ~3 ~12 minecraft:dark_oak_log[axis=y]
setblock ~6 ~2 ~5 minecraft:cracked_stone_bricks
fill ~7 ~2 ~4 ~7 ~3 ~4 minecraft:stone_bricks
setblock ~6 ~3 ~5 minecraft:stone_bricks
fill ~6 ~4 ~4 ~14 ~4 ~4 minecraft:dark_oak_log[axis=x]
fill ~6 ~4 ~5 ~6 ~4 ~11 minecraft:dark_oak_log[axis=z]
fill ~6 ~4 ~12 ~14 ~4 ~12 minecraft:dark_oak_log[axis=x]
fill ~14 ~4 ~5 ~14 ~4 ~11 minecraft:dark_oak_log[axis=z]

# the four doorways (three wide, three high) with a post on each jamb
fill ~6 ~1 ~6 ~6 ~3 ~6 minecraft:dark_oak_log[axis=y]
fill ~6 ~1 ~7 ~6 ~3 ~9 minecraft:air
fill ~6 ~1 ~10 ~6 ~3 ~10 minecraft:dark_oak_log[axis=y]
fill ~8 ~1 ~4 ~8 ~3 ~4 minecraft:dark_oak_log[axis=y]
fill ~8 ~1 ~12 ~8 ~3 ~12 minecraft:dark_oak_log[axis=y]
fill ~9 ~1 ~4 ~11 ~3 ~4 minecraft:air
fill ~9 ~1 ~12 ~11 ~3 ~12 minecraft:air
fill ~12 ~1 ~4 ~12 ~3 ~4 minecraft:dark_oak_log[axis=y]
fill ~12 ~1 ~12 ~12 ~3 ~12 minecraft:dark_oak_log[axis=y]
fill ~14 ~1 ~6 ~14 ~3 ~6 minecraft:dark_oak_log[axis=y]
fill ~14 ~1 ~7 ~14 ~3 ~9 minecraft:air
fill ~14 ~1 ~10 ~14 ~3 ~10 minecraft:dark_oak_log[axis=y]

# windows: a pane two high in the wall beside each doorway, a lantern window in each gable
setblock ~6 ~2 ~11 minecraft:glass_pane[north=true,east=true,south=true,west=false]
setblock ~7 ~2 ~12 minecraft:glass_pane[north=true,east=true,south=false,west=true]
fill ~13 ~2 ~4 ~13 ~3 ~4 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~13 ~2 ~12 ~13 ~3 ~12 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~14 ~2 ~5 ~14 ~3 ~5 minecraft:glass_pane[north=true,east=true,south=true,west=false]
fill ~14 ~2 ~11 ~14 ~3 ~11 minecraft:glass_pane[north=true,east=true,south=true,west=false]
setblock ~6 ~3 ~11 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~7 ~3 ~12 minecraft:glass_pane[north=false,east=true,south=false,west=true]

# the roof: dark oak stairs at the eaves (three rows), snow blocks above, a dark oak verge, a snow ridge along z 8 at y 9
fill ~5 ~4 ~3 ~15 ~4 ~3 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~5 ~4 ~13 ~15 ~4 ~13 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~5 ~5 ~4 ~15 ~5 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~5 ~5 ~12 ~15 ~5 ~12 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~5 ~6 ~5 ~6 ~6 ~5 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~5 ~6 ~11 ~15 ~6 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~9 ~6 ~5 ~15 ~6 ~5 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~5 ~7 ~6 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~5 ~7 ~10 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~6 ~7 ~6 minecraft:snow_block
fill ~6 ~7 ~10 ~14 ~7 ~10 minecraft:snow_block
fill ~9 ~7 ~6 ~14 ~7 ~6 minecraft:snow_block
setblock ~15 ~7 ~6 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~15 ~7 ~10 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~5 ~8 ~7 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~5 ~8 ~9 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~6 ~8 ~7 ~14 ~8 ~7 minecraft:snow_block
fill ~6 ~8 ~9 ~14 ~8 ~9 minecraft:snow_block
setblock ~15 ~8 ~7 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~15 ~8 ~9 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~5 ~9 ~8 ~15 ~9 ~8 minecraft:snow_block

# the gable walls (x 6 and x 14): stone bricks to the underside of the roof with a king post of dark oak at z 8 and a lantern window
fill ~6 ~5 ~5 ~6 ~5 ~6 minecraft:stone_bricks
setblock ~6 ~5 ~7 minecraft:deepslate_bricks
fill ~6 ~5 ~8 ~6 ~8 ~8 minecraft:dark_oak_log[axis=y]
setblock ~6 ~5 ~9 minecraft:deepslate_bricks
setblock ~6 ~5 ~10 minecraft:cracked_stone_bricks
setblock ~6 ~5 ~11 minecraft:stone_bricks
setblock ~14 ~5 ~5 minecraft:stone_bricks
fill ~14 ~5 ~6 ~14 ~5 ~7 minecraft:cracked_stone_bricks
fill ~14 ~5 ~8 ~14 ~8 ~8 minecraft:dark_oak_log[axis=y]
fill ~14 ~5 ~9 ~14 ~5 ~11 minecraft:stone_bricks
setblock ~6 ~6 ~6 minecraft:cracked_stone_bricks
setblock ~6 ~6 ~7 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~6 ~6 ~9 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~6 ~6 ~10 minecraft:stone_bricks
setblock ~14 ~6 ~6 minecraft:stone_bricks
setblock ~14 ~6 ~7 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~14 ~6 ~9 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~14 ~6 ~10 minecraft:stone_bricks
setblock ~6 ~7 ~7 minecraft:stone_bricks
setblock ~6 ~7 ~9 minecraft:stone_bricks
setblock ~14 ~7 ~7 minecraft:stone_bricks
setblock ~14 ~7 ~9 minecraft:stone_bricks

# the chimney of the north-west corner: a stack of stone bricks x 7..8, z 5..6 from the floor to a cap at y 10 above the ridge, a firebox in the south face (x 7..8, z 6, y 1..2) with a campfire and a cauldron, a lintel at y 3, smoke from a campfire on the cap
fill ~7 ~1 ~5 ~8 ~1 ~5 minecraft:stone_bricks
setblock ~7 ~1 ~6 minecraft:campfire[lit=true,signal_fire=false,facing=south]
setblock ~8 ~1 ~6 minecraft:cauldron
fill ~7 ~2 ~5 ~7 ~5 ~5 minecraft:stone_bricks
fill ~7 ~2 ~6 ~8 ~2 ~6 minecraft:air
setblock ~8 ~2 ~5 minecraft:cracked_stone_bricks
fill ~7 ~3 ~6 ~8 ~3 ~6 minecraft:chiseled_stone_bricks
fill ~8 ~3 ~5 ~8 ~7 ~5 minecraft:stone_bricks
fill ~7 ~4 ~6 ~8 ~7 ~6 minecraft:stone_bricks
setblock ~7 ~6 ~5 minecraft:andesite
setblock ~7 ~7 ~5 minecraft:stone_bricks
fill ~7 ~8 ~5 ~7 ~8 ~6 minecraft:cracked_stone_bricks
setblock ~8 ~8 ~5 minecraft:andesite
fill ~8 ~8 ~6 ~8 ~10 ~6 minecraft:stone_bricks
fill ~7 ~9 ~5 ~8 ~10 ~5 minecraft:stone_bricks
fill ~7 ~9 ~6 ~7 ~10 ~6 minecraft:stone_bricks
setblock ~7 ~11 ~5 minecraft:campfire[lit=true,signal_fire=false,facing=south]

# tie beams of dark oak at x 8 and x 12 (y 4), a lantern hung from each beam beside the doorway strips
setblock ~8 ~3 ~10 minecraft:lantern[hanging=true]
setblock ~12 ~3 ~6 minecraft:lantern[hanging=true]
setblock ~12 ~3 ~10 minecraft:lantern[hanging=true]
fill ~8 ~4 ~7 ~8 ~4 ~11 minecraft:dark_oak_log[axis=z]
fill ~12 ~4 ~5 ~12 ~4 ~11 minecraft:dark_oak_log[axis=z]

# two bunks in the north-east corner (hay under red blankets), a chest and barrels in the south-east, split logs in the south-west
fill ~7 ~1 ~10 ~8 ~1 ~11 minecraft:spruce_log[axis=z]
fill ~12 ~1 ~5 ~13 ~1 ~6 minecraft:hay_block[axis=x]
setblock ~12 ~1 ~11 minecraft:barrel[facing=up]
fill ~13 ~1 ~10 ~13 ~2 ~10 minecraft:barrel[facing=up]
setblock ~13 ~1 ~11 minecraft:chest[facing=west]
fill ~7 ~2 ~10 ~7 ~2 ~11 minecraft:spruce_log[axis=z]
fill ~12 ~2 ~5 ~12 ~2 ~6 minecraft:gray_carpet
fill ~13 ~2 ~5 ~13 ~2 ~6 minecraft:red_carpet

# the lean-to on the east face (x 15..17, z 5..11): roof of snow blocks at y 5 and 4 and a row of dark oak stairs at y 3 on posts; the end rows are dark oak stairs
fill ~17 ~1 ~5 ~17 ~2 ~5 minecraft:dark_oak_log[axis=y]
fill ~17 ~1 ~11 ~17 ~2 ~11 minecraft:dark_oak_log[axis=y]
setblock ~16 ~3 ~8 minecraft:lantern[hanging=true]
fill ~17 ~3 ~5 ~17 ~3 ~11 minecraft:dark_oak_stairs[facing=west,half=bottom]
setblock ~16 ~4 ~5 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~16 ~4 ~6 ~16 ~4 ~10 minecraft:snow_block
setblock ~16 ~4 ~11 minecraft:dark_oak_stairs[facing=west,half=bottom]
setblock ~15 ~5 ~5 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~15 ~5 ~6 ~15 ~5 ~10 minecraft:snow_block
setblock ~15 ~5 ~11 minecraft:dark_oak_stairs[facing=west,half=bottom]

# under the lean-to: a woodpile of spruce logs against the wall in both corners (y 1..3), a chopping block, a bench
fill ~15 ~1 ~5 ~16 ~2 ~6 minecraft:spruce_log[axis=z]
fill ~15 ~1 ~10 ~16 ~2 ~11 minecraft:spruce_log[axis=z]
setblock ~16 ~1 ~7 minecraft:stripped_spruce_log[axis=y]
setblock ~16 ~1 ~9 minecraft:spruce_stairs[facing=west,half=bottom]
fill ~15 ~3 ~5 ~15 ~3 ~6 minecraft:spruce_log[axis=z]
fill ~15 ~3 ~10 ~15 ~3 ~11 minecraft:spruce_log[axis=z]

# the brazier in front of the west door: a pad of flags, a pedestal of stone bricks with a chiseled head, the fire in an iron cage (x 3, z 12), lit
fill ~2 ~0 ~11 ~3 ~0 ~13 minecraft:stone_bricks
setblock ~4 ~0 ~11 minecraft:cracked_stone_bricks
fill ~4 ~0 ~12 ~4 ~0 ~13 minecraft:stone_bricks
setblock ~3 ~1 ~12 minecraft:stone_bricks
setblock ~3 ~2 ~12 minecraft:chiseled_stone_bricks
setblock ~2 ~3 ~12 minecraft:iron_bars[north=false,east=false,south=false,west=false]
setblock ~3 ~3 ~11 minecraft:iron_bars[north=false,east=false,south=false,west=false]
setblock ~3 ~3 ~12 minecraft:campfire[lit=true,signal_fire=false,facing=north]
setblock ~3 ~3 ~13 minecraft:iron_bars[north=false,east=false,south=false,west=false]
setblock ~4 ~3 ~12 minecraft:iron_bars[north=false,east=false,south=false,west=false]

# the lantern post on the north approach and a waymark cairn on the south-east
fill ~12 ~1 ~2 ~12 ~3 ~2 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~18 ~1 ~13 ~19 ~1 ~13 minecraft:cobblestone
setblock ~18 ~1 ~14 minecraft:cobblestone
setblock ~19 ~1 ~14 minecraft:andesite
setblock ~18 ~2 ~13 minecraft:stone
setblock ~18 ~3 ~13 minecraft:cobblestone
setblock ~12 ~4 ~2 minecraft:lantern[hanging=false]
setblock ~18 ~4 ~13 minecraft:lantern[hanging=false]

# snow poles with a red banner on the top on both sides of the lane to the north-west (the lane of the map leaves the hut to the north-west, 4 wide): dark oak fence posts five high at (7, 1) and (3, 6), a standing banner above each, the tops showing over the drifts in a blizzard
fill ~3 ~1 ~6 ~3 ~5 ~6 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~7 ~1 ~1 ~7 ~5 ~1 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~3 ~6 ~6 minecraft:red_banner[rotation=4]
setblock ~7 ~6 ~1 minecraft:red_banner[rotation=0]

# the pass bell on the north-east (x 15..17, z 2): two dark oak posts five high with a beam of logs across them at y 5, a bell hanging from the beam, flags under the posts and a stone step in front
setblock ~15 ~0 ~2 minecraft:cracked_stone_bricks
fill ~16 ~0 ~2 ~17 ~0 ~2 minecraft:stone_bricks
fill ~15 ~1 ~2 ~15 ~4 ~2 minecraft:dark_oak_log[axis=y]
setblock ~16 ~1 ~3 minecraft:stone_brick_stairs[facing=north,half=bottom]
fill ~17 ~1 ~2 ~17 ~4 ~2 minecraft:dark_oak_log[axis=y]
setblock ~16 ~4 ~2 minecraft:bell[attachment=ceiling,facing=north]
fill ~15 ~5 ~2 ~17 ~5 ~2 minecraft:dark_oak_log[axis=x]

# a trough of dark oak planks with ice for water south-west of the south doorway, a hitching rail of dark oak fences with a hay bale beside it
fill ~2 ~1 ~14 ~6 ~1 ~14 minecraft:dark_oak_planks
fill ~2 ~1 ~15 ~2 ~1 ~16 minecraft:dark_oak_planks
fill ~3 ~1 ~15 ~5 ~1 ~15 minecraft:ice
fill ~3 ~1 ~16 ~5 ~1 ~16 minecraft:dark_oak_planks
fill ~6 ~1 ~15 ~6 ~1 ~16 minecraft:dark_oak_planks
fill ~13 ~1 ~14 ~14 ~1 ~14 minecraft:hay_block[axis=x]
setblock ~13 ~1 ~15 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
setblock ~15 ~1 ~15 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~17 ~1 ~15 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~13 ~2 ~15 minecraft:dark_oak_fence[north=false,east=true,south=false,west=false]
fill ~14 ~2 ~15 ~16 ~2 ~15 minecraft:dark_oak_fence[north=false,east=true,south=false,west=true]
setblock ~17 ~2 ~15 minecraft:dark_oak_fence[north=false,east=false,south=false,west=true]

# snow banked against the foot of the walls and along the edges of the approaches; the trodden strips stay bare
setblock ~0 ~0 ~5 minecraft:snow_block
fill ~0 ~0 ~11 ~0 ~0 ~12 minecraft:snow_block
setblock ~0 ~0 ~14 minecraft:snow_block
setblock ~1 ~0 ~13 minecraft:snow_block
setblock ~1 ~0 ~15 minecraft:snow_block
setblock ~2 ~0 ~6 minecraft:snow_block
setblock ~3 ~0 ~0 minecraft:snow_block
setblock ~3 ~0 ~5 minecraft:snow_block
setblock ~4 ~0 ~1 minecraft:snow_block
fill ~5 ~0 ~3 ~8 ~0 ~3 minecraft:snow_block
fill ~5 ~0 ~4 ~5 ~0 ~6 minecraft:snow_block
fill ~5 ~0 ~10 ~5 ~0 ~13 minecraft:snow_block
fill ~6 ~0 ~13 ~8 ~0 ~13 minecraft:snow_block
setblock ~8 ~0 ~16 minecraft:snow_block
setblock ~12 ~0 ~0 minecraft:snow_block
fill ~12 ~0 ~3 ~15 ~0 ~3 minecraft:snow_block
fill ~12 ~0 ~13 ~14 ~0 ~13 minecraft:snow_block
setblock ~12 ~0 ~16 minecraft:snow_block
setblock ~15 ~0 ~0 minecraft:snow_block
setblock ~15 ~0 ~4 minecraft:snow_block
fill ~15 ~0 ~12 ~15 ~0 ~13 minecraft:snow_block
setblock ~16 ~0 ~1 minecraft:snow_block
fill ~17 ~0 ~3 ~17 ~0 ~4 minecraft:snow_block
setblock ~17 ~0 ~14 minecraft:snow_block
setblock ~18 ~0 ~2 minecraft:snow_block
setblock ~18 ~0 ~15 minecraft:snow_block
fill ~19 ~0 ~0 ~20 ~0 ~0 minecraft:snow_block
setblock ~19 ~0 ~1 minecraft:snow_block
fill ~19 ~0 ~3 ~19 ~0 ~5 minecraft:snow_block
setblock ~19 ~0 ~10 minecraft:snow_block
setblock ~19 ~0 ~16 minecraft:snow_block
setblock ~20 ~0 ~6 minecraft:snow_block
setblock ~20 ~0 ~13 minecraft:snow_block
setblock ~0 ~1 ~5 minecraft:snow[layers=1]
setblock ~0 ~1 ~11 minecraft:snow[layers=2]
setblock ~0 ~1 ~12 minecraft:snow[layers=1]
setblock ~0 ~1 ~14 minecraft:snow[layers=1]
setblock ~1 ~1 ~13 minecraft:snow[layers=1]
setblock ~1 ~1 ~15 minecraft:snow[layers=2]
setblock ~2 ~1 ~6 minecraft:snow[layers=1]
setblock ~3 ~1 ~0 minecraft:snow[layers=1]
setblock ~3 ~1 ~5 minecraft:snow[layers=1]
setblock ~4 ~1 ~1 minecraft:snow[layers=1]
fill ~5 ~1 ~3 ~6 ~1 ~3 minecraft:snow[layers=1]
setblock ~5 ~1 ~4 minecraft:snow[layers=1]
fill ~5 ~1 ~5 ~5 ~1 ~6 minecraft:snow[layers=2]
setblock ~5 ~1 ~10 minecraft:snow[layers=1]
setblock ~5 ~1 ~11 minecraft:snow[layers=2]
fill ~5 ~1 ~12 ~5 ~1 ~13 minecraft:snow[layers=1]
setblock ~6 ~1 ~13 minecraft:snow[layers=2]
setblock ~7 ~1 ~3 minecraft:snow[layers=2]
fill ~7 ~1 ~13 ~8 ~1 ~13 minecraft:snow[layers=1]
setblock ~8 ~1 ~3 minecraft:snow[layers=1]
setblock ~8 ~1 ~16 minecraft:snow[layers=1]
setblock ~12 ~1 ~0 minecraft:snow[layers=2]
fill ~12 ~1 ~3 ~13 ~1 ~3 minecraft:snow[layers=1]
setblock ~12 ~1 ~10 minecraft:snow[layers=2]
setblock ~12 ~1 ~13 minecraft:snow[layers=2]
setblock ~12 ~1 ~16 minecraft:snow[layers=1]
fill ~13 ~1 ~13 ~14 ~1 ~13 minecraft:snow[layers=1]
setblock ~14 ~1 ~3 minecraft:snow[layers=2]
setblock ~15 ~1 ~0 minecraft:snow[layers=1]
fill ~15 ~1 ~3 ~15 ~1 ~4 minecraft:snow[layers=1]
fill ~15 ~1 ~12 ~15 ~1 ~13 minecraft:snow[layers=1]
setblock ~16 ~1 ~1 minecraft:snow[layers=1]
setblock ~17 ~1 ~3 minecraft:snow[layers=2]
setblock ~17 ~1 ~4 minecraft:snow[layers=1]
setblock ~17 ~1 ~6 minecraft:snow[layers=1]
setblock ~17 ~1 ~14 minecraft:snow[layers=1]
setblock ~18 ~1 ~2 minecraft:snow[layers=1]
setblock ~18 ~1 ~15 minecraft:snow[layers=2]
fill ~19 ~1 ~0 ~20 ~1 ~0 minecraft:snow[layers=1]
setblock ~19 ~1 ~1 minecraft:snow[layers=1]
fill ~19 ~1 ~3 ~19 ~1 ~5 minecraft:snow[layers=1]
setblock ~19 ~1 ~10 minecraft:snow[layers=1]
setblock ~19 ~1 ~16 minecraft:snow[layers=1]
setblock ~20 ~1 ~6 minecraft:snow[layers=2]
setblock ~20 ~1 ~13 minecraft:snow[layers=1]

# snow settles on every sky-facing top of a full block: the chimney cap, the cairn, the pedestal, the woodpile, the trough (one layer)
fill ~2 ~2 ~14 ~6 ~2 ~14 minecraft:snow[layers=1]
fill ~2 ~2 ~15 ~2 ~2 ~16 minecraft:snow[layers=1]
fill ~4 ~2 ~16 ~5 ~2 ~16 minecraft:snow[layers=1]
fill ~6 ~2 ~15 ~6 ~2 ~16 minecraft:snow[layers=1]
setblock ~18 ~2 ~14 minecraft:snow[layers=1]
fill ~19 ~2 ~13 ~19 ~2 ~14 minecraft:snow[layers=1]
fill ~15 ~6 ~2 ~16 ~6 ~2 minecraft:snow[layers=1]
setblock ~7 ~11 ~6 minecraft:snow[layers=1]
fill ~8 ~11 ~5 ~8 ~11 ~6 minecraft:snow[layers=1]
