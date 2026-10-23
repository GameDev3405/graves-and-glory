# size 13 12 11
# camp/tent_ironclad: Ironclad recruiter tent (black and gray canvas, iron fittings, a forge chimney, a blast furnace and a fenced fire).
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Facing south. Body x 1..11, z 0..7 (a low squat hall: black canvas walls on a blackstone footing, a shallow roof of deepslate tile rising half a block a column to y 7, a stepped pediment); porch z 8..10 under the awning at y 7; plinth y 1 along z 9, display slots x 1..11 step 2 (feet y 2).
# Banners: the House wall banner at (6,9,9) facing south on the gable, the House standing banner at (0,7,10) on the dark oak fence pole (0,1..6,10).

# the floor of the body (dark oak planks) and of the porch (polished andesite in a stone brick border)
fill ~0 ~0 ~8 ~0 ~0 ~10 minecraft:stone_bricks
fill ~1 ~0 ~0 ~11 ~0 ~7 minecraft:dark_oak_planks
fill ~1 ~0 ~8 ~11 ~0 ~9 minecraft:polished_andesite
fill ~1 ~0 ~10 ~11 ~0 ~10 minecraft:stone_bricks
fill ~12 ~0 ~8 ~12 ~0 ~10 minecraft:stone_bricks

# the body: a low squat hall: black canvas walls on a blackstone footing, a shallow roof of deepslate tile rising half a block a column to y 7, a stepped pediment
fill ~1 ~1 ~0 ~11 ~1 ~0 minecraft:polished_blackstone_bricks
fill ~1 ~1 ~1 ~1 ~1 ~7 minecraft:polished_blackstone_bricks
fill ~2 ~1 ~7 ~3 ~1 ~7 minecraft:polished_blackstone_bricks
fill ~9 ~1 ~7 ~10 ~1 ~7 minecraft:polished_blackstone_bricks
fill ~11 ~1 ~1 ~11 ~1 ~7 minecraft:polished_blackstone_bricks
fill ~1 ~2 ~0 ~1 ~3 ~0 minecraft:dark_oak_log
fill ~1 ~2 ~1 ~1 ~3 ~1 minecraft:gray_wool
setblock ~1 ~2 ~2 minecraft:iron_bars[north=true,east=false,south=true,west=false]
setblock ~1 ~2 ~3 minecraft:gray_wool
fill ~1 ~2 ~4 ~1 ~3 ~4 minecraft:dark_oak_log
setblock ~1 ~2 ~5 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~1 ~2 ~6 ~1 ~3 ~6 minecraft:black_wool
fill ~1 ~2 ~7 ~1 ~3 ~7 minecraft:dark_oak_log
fill ~2 ~2 ~0 ~5 ~3 ~0 minecraft:gray_wool
fill ~2 ~2 ~7 ~3 ~3 ~7 minecraft:black_wool
fill ~6 ~2 ~0 ~10 ~3 ~0 minecraft:black_wool
fill ~9 ~2 ~7 ~10 ~3 ~7 minecraft:black_wool
fill ~11 ~2 ~0 ~11 ~3 ~0 minecraft:dark_oak_log
fill ~11 ~2 ~1 ~11 ~3 ~1 minecraft:black_wool
setblock ~11 ~2 ~2 minecraft:iron_bars[north=true,east=true,south=true,west=false]
fill ~11 ~2 ~3 ~11 ~3 ~3 minecraft:black_wool
fill ~11 ~2 ~4 ~11 ~3 ~4 minecraft:dark_oak_log
setblock ~11 ~2 ~5 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~11 ~2 ~6 ~11 ~3 ~6 minecraft:black_wool
fill ~11 ~2 ~7 ~11 ~3 ~7 minecraft:dark_oak_log
fill ~1 ~3 ~2 ~1 ~3 ~3 minecraft:black_wool
setblock ~1 ~3 ~5 minecraft:black_wool
setblock ~11 ~3 ~2 minecraft:gray_wool
setblock ~11 ~3 ~5 minecraft:black_wool
setblock ~1 ~4 ~0 minecraft:polished_blackstone_brick_slab[type=bottom]
fill ~1 ~4 ~1 ~1 ~4 ~6 minecraft:deepslate_tile_slab[type=bottom]
setblock ~1 ~4 ~7 minecraft:polished_blackstone_brick_slab[type=bottom]
fill ~2 ~4 ~0 ~10 ~4 ~0 minecraft:polished_blackstone_bricks
fill ~2 ~4 ~1 ~3 ~4 ~6 minecraft:deepslate_tiles
fill ~2 ~4 ~7 ~4 ~4 ~7 minecraft:polished_blackstone_bricks
fill ~8 ~4 ~7 ~10 ~4 ~7 minecraft:polished_blackstone_bricks
fill ~9 ~4 ~1 ~10 ~4 ~6 minecraft:deepslate_tiles
setblock ~11 ~4 ~0 minecraft:polished_blackstone_brick_slab[type=bottom]
fill ~11 ~4 ~1 ~11 ~4 ~6 minecraft:deepslate_tile_slab[type=bottom]
setblock ~11 ~4 ~7 minecraft:polished_blackstone_brick_slab[type=bottom]
setblock ~3 ~5 ~0 minecraft:polished_blackstone_brick_slab[type=bottom]
fill ~3 ~5 ~1 ~3 ~5 ~6 minecraft:deepslate_tile_slab[type=bottom]
setblock ~3 ~5 ~7 minecraft:polished_blackstone_brick_slab[type=bottom]
fill ~4 ~5 ~0 ~8 ~5 ~0 minecraft:polished_blackstone_bricks
fill ~4 ~5 ~1 ~5 ~5 ~6 minecraft:deepslate_tiles
fill ~4 ~5 ~7 ~8 ~5 ~7 minecraft:polished_blackstone_bricks
fill ~7 ~5 ~1 ~8 ~5 ~6 minecraft:deepslate_tiles
setblock ~9 ~5 ~0 minecraft:polished_blackstone_brick_slab[type=bottom]
fill ~9 ~5 ~1 ~9 ~5 ~6 minecraft:deepslate_tile_slab[type=bottom]
setblock ~9 ~5 ~7 minecraft:polished_blackstone_brick_slab[type=bottom]
setblock ~5 ~6 ~0 minecraft:polished_blackstone_brick_slab[type=bottom]
fill ~5 ~6 ~1 ~5 ~6 ~6 minecraft:deepslate_tile_slab[type=bottom]
setblock ~5 ~6 ~7 minecraft:polished_blackstone_brick_slab[type=bottom]
setblock ~6 ~6 ~0 minecraft:polished_blackstone_bricks
fill ~6 ~6 ~1 ~6 ~6 ~6 minecraft:deepslate_tiles
setblock ~6 ~6 ~7 minecraft:polished_blackstone_bricks
setblock ~7 ~6 ~0 minecraft:polished_blackstone_brick_slab[type=bottom]
fill ~7 ~6 ~1 ~7 ~6 ~6 minecraft:deepslate_tile_slab[type=bottom]
setblock ~7 ~6 ~7 minecraft:polished_blackstone_brick_slab[type=bottom]

# the door posts and the lintel over the door (x 5..7)
fill ~4 ~1 ~7 ~4 ~3 ~7 minecraft:dark_oak_log
fill ~8 ~1 ~7 ~8 ~3 ~7 minecraft:dark_oak_log
fill ~5 ~4 ~7 ~7 ~4 ~7 minecraft:iron_block

# the eaves: a hard edge of blackstone slab overhanging both sides, iron caps on the ridge ends
fill ~0 ~4 ~0 ~0 ~4 ~7 minecraft:polished_blackstone_slab[type=bottom]
fill ~12 ~4 ~1 ~12 ~4 ~7 minecraft:polished_blackstone_slab[type=bottom]
setblock ~6 ~7 ~0 minecraft:iron_block
setblock ~6 ~7 ~7 minecraft:iron_block

# the pediment over the porch: blackstone stepped to a point, chiseled at the top (the House banner hangs on it)
setblock ~4 ~8 ~8 minecraft:polished_blackstone_bricks
setblock ~8 ~8 ~8 minecraft:polished_blackstone_bricks
setblock ~4 ~9 ~8 minecraft:polished_blackstone_brick_slab[type=bottom]
setblock ~5 ~9 ~8 minecraft:polished_blackstone_bricks
setblock ~7 ~9 ~8 minecraft:polished_blackstone_bricks
setblock ~8 ~9 ~8 minecraft:polished_blackstone_brick_slab[type=bottom]
setblock ~6 ~10 ~8 minecraft:chiseled_polished_blackstone

# the porch posts at its four corners (the west front one is the dark oak fence pole of the standard)
setblock ~0 ~1 ~8 minecraft:dark_oak_fence[north=false,east=false,south=true,west=false]
setblock ~0 ~1 ~10 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
setblock ~12 ~1 ~8 minecraft:dark_oak_fence[north=false,east=false,south=true,west=false]
setblock ~12 ~1 ~10 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
fill ~0 ~2 ~8 ~0 ~6 ~8 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~0 ~2 ~10 ~0 ~6 ~10 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~12 ~2 ~8 ~12 ~6 ~8 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~12 ~2 ~10 ~12 ~6 ~10 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]

# the awning at y 7 over the porch, its front edge in gray canvas
fill ~0 ~7 ~8 ~0 ~7 ~9 minecraft:gray_wool
fill ~1 ~7 ~8 ~3 ~7 ~9 minecraft:black_wool
fill ~1 ~7 ~10 ~11 ~7 ~10 minecraft:polished_blackstone_brick_stairs[facing=north,half=top]
fill ~4 ~7 ~8 ~4 ~7 ~9 minecraft:gray_wool
fill ~5 ~7 ~8 ~7 ~7 ~9 minecraft:black_wool
fill ~8 ~7 ~8 ~8 ~7 ~9 minecraft:gray_wool
fill ~9 ~7 ~8 ~11 ~7 ~9 minecraft:black_wool
fill ~12 ~7 ~8 ~12 ~7 ~9 minecraft:gray_wool
setblock ~12 ~7 ~10 minecraft:iron_block

# the gable along z 8 above the awning (it carries the House wall banner)
fill ~5 ~8 ~8 ~7 ~8 ~8 minecraft:dark_oak_planks
setblock ~6 ~9 ~8 minecraft:dark_oak_planks

# the plinth at y 1 along z 9 (the six display slots at x 1..11 step 2 stand on it) and the step before it
setblock ~0 ~1 ~9 minecraft:iron_block
setblock ~1 ~1 ~9 minecraft:polished_blackstone_bricks
fill ~1 ~1 ~10 ~11 ~1 ~10 minecraft:polished_blackstone_slab[type=bottom]
setblock ~2 ~1 ~9 minecraft:chiseled_polished_blackstone
setblock ~3 ~1 ~9 minecraft:polished_blackstone_bricks
setblock ~4 ~1 ~9 minecraft:chiseled_polished_blackstone
setblock ~5 ~1 ~9 minecraft:polished_blackstone_bricks
setblock ~6 ~1 ~9 minecraft:chiseled_polished_blackstone
setblock ~7 ~1 ~9 minecraft:polished_blackstone_bricks
setblock ~8 ~1 ~9 minecraft:chiseled_polished_blackstone
setblock ~9 ~1 ~9 minecraft:polished_blackstone_bricks
setblock ~10 ~1 ~9 minecraft:chiseled_polished_blackstone
setblock ~11 ~1 ~9 minecraft:polished_blackstone_bricks
setblock ~12 ~1 ~9 minecraft:iron_block

# the forge on the east side: a blast furnace, an anvil, iron blocks and a water trough of cauldron
setblock ~12 ~1 ~1 minecraft:iron_block
setblock ~12 ~1 ~2 minecraft:blast_furnace[facing=east,lit=false]
setblock ~12 ~2 ~2 minecraft:iron_bars[north=false,east=false,south=false,west=true]

# the forge chimney behind the blast furnace: a stack of cobbled deepslate from the ground to y 8 with a smoking campfire on top
fill ~12 ~1 ~0 ~12 ~7 ~0 minecraft:cobbled_deepslate
setblock ~12 ~1 ~3 minecraft:anvil[facing=north]
setblock ~12 ~1 ~5 minecraft:cauldron
setblock ~12 ~1 ~6 minecraft:iron_block
setblock ~12 ~8 ~0 minecraft:polished_deepslate
setblock ~12 ~9 ~0 minecraft:campfire[lit=true,facing=south,signal_fire=false]

# the fenced fire on the west side: a campfire in a ring of iron bars, a chain rack
setblock ~0 ~1 ~2 minecraft:iron_bars[north=false,east=true,south=false,west=false]
setblock ~0 ~1 ~3 minecraft:campfire[lit=true,facing=west]
setblock ~0 ~1 ~4 minecraft:iron_bars[north=false,east=true,south=false,west=false]
setblock ~0 ~1 ~6 minecraft:barrel[facing=up]
setblock ~0 ~2 ~6 minecraft:iron_block

# chains hang from the awning between the slots, and chain lanterns at the porch corners
setblock ~0 ~2 ~9 minecraft:lantern
setblock ~12 ~2 ~9 minecraft:lantern
setblock ~2 ~6 ~10 minecraft:chain[axis=y]
setblock ~4 ~6 ~10 minecraft:chain[axis=y]
setblock ~6 ~6 ~10 minecraft:chain[axis=y]
setblock ~8 ~6 ~10 minecraft:chain[axis=y]
setblock ~10 ~6 ~10 minecraft:chain[axis=y]

# inside: an armour bench, chests, a lantern on a chain from the ridge
fill ~2 ~1 ~5 ~3 ~1 ~5 minecraft:gray_carpet
setblock ~3 ~1 ~2 minecraft:smithing_table
fill ~9 ~1 ~2 ~9 ~1 ~3 minecraft:chest[facing=south]
fill ~9 ~1 ~5 ~10 ~1 ~5 minecraft:gray_carpet
setblock ~6 ~4 ~3 minecraft:lantern[hanging=true]
setblock ~6 ~5 ~3 minecraft:chain[axis=y]

# the banners: the House wall banner on the gable, the House standing banner on the pole
setblock ~0 ~7 ~10 minecraft:black_banner[rotation=0]{Patterns:[{Pattern:"bri",Color:7},{Pattern:"mc",Color:1},{Pattern:"bo",Color:4}]}
setblock ~6 ~9 ~9 minecraft:black_wall_banner[facing=south]{Patterns:[{Pattern:"bri",Color:7},{Pattern:"mc",Color:1},{Pattern:"bo",Color:4}]}
