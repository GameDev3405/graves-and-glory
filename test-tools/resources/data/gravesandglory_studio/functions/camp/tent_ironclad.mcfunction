# size 13 12 11
# camp/tent_ironclad: Ironclad recruiter tent (black and gray canvas, iron fittings, a forge chimney, a blast furnace and a fenced fire).
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Facing south. Body x 1..11, z 0..7 (eave y 4, ridge cloth y 9, ridge pole y 10); porch z 8..10 under the awning at y 7; plinth y 1 along z 9, display slots x 1..11 step 2 (feet y 2).
# Banners: the House wall banner at (6,9,9) facing south on the gable, the House standing banner at (0,7,10) on the dark oak fence pole (0,1..6,10).

# the floor of the body (dark oak planks) and of the porch (polished andesite in a stone brick border)
fill ~0 ~0 ~8 ~0 ~0 ~10 minecraft:stone_bricks
fill ~1 ~0 ~0 ~11 ~0 ~7 minecraft:dark_oak_planks
fill ~1 ~0 ~8 ~11 ~0 ~9 minecraft:polished_andesite
fill ~1 ~0 ~10 ~11 ~0 ~10 minecraft:stone_bricks
fill ~12 ~0 ~8 ~12 ~0 ~10 minecraft:stone_bricks

# the frame: corner and door posts of dark_oak_log from the floor to the eave, the back pole to the ridge
fill ~1 ~1 ~0 ~1 ~3 ~0 minecraft:dark_oak_log
fill ~1 ~1 ~7 ~1 ~3 ~7 minecraft:dark_oak_log
fill ~4 ~1 ~7 ~4 ~3 ~7 minecraft:dark_oak_log
fill ~6 ~1 ~0 ~6 ~8 ~0 minecraft:dark_oak_log
fill ~8 ~1 ~7 ~8 ~3 ~7 minecraft:dark_oak_log
fill ~11 ~1 ~0 ~11 ~3 ~0 minecraft:dark_oak_log
fill ~11 ~1 ~7 ~11 ~3 ~7 minecraft:dark_oak_log

# the walls: a skirt of iron-banded dark oak, then canvas; the front wall has the door (x 5..7)
fill ~1 ~1 ~1 ~1 ~1 ~6 minecraft:dark_oak_planks
fill ~2 ~1 ~0 ~5 ~1 ~0 minecraft:dark_oak_planks
fill ~2 ~1 ~7 ~3 ~1 ~7 minecraft:dark_oak_planks
fill ~7 ~1 ~0 ~10 ~1 ~0 minecraft:dark_oak_planks
fill ~9 ~1 ~7 ~10 ~1 ~7 minecraft:dark_oak_planks
fill ~11 ~1 ~1 ~11 ~1 ~6 minecraft:dark_oak_planks
fill ~1 ~2 ~1 ~1 ~3 ~1 minecraft:gray_wool
setblock ~1 ~2 ~2 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~1 ~2 ~3 ~1 ~2 ~4 minecraft:gray_wool
setblock ~1 ~2 ~5 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~1 ~2 ~6 ~1 ~3 ~6 minecraft:black_wool
fill ~2 ~2 ~0 ~5 ~3 ~0 minecraft:gray_wool
fill ~2 ~2 ~7 ~3 ~4 ~7 minecraft:black_wool
fill ~7 ~2 ~0 ~10 ~4 ~0 minecraft:black_wool
fill ~9 ~2 ~7 ~10 ~4 ~7 minecraft:black_wool
fill ~11 ~2 ~1 ~11 ~3 ~1 minecraft:black_wool
setblock ~11 ~2 ~2 minecraft:iron_bars[north=true,east=true,south=true,west=false]
fill ~11 ~2 ~3 ~11 ~3 ~4 minecraft:black_wool
setblock ~11 ~2 ~5 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~11 ~2 ~6 ~11 ~3 ~6 minecraft:black_wool
fill ~1 ~3 ~2 ~1 ~3 ~5 minecraft:black_wool
setblock ~11 ~3 ~2 minecraft:gray_wool
setblock ~11 ~3 ~5 minecraft:black_wool
fill ~2 ~4 ~0 ~5 ~4 ~0 minecraft:black_wool
fill ~4 ~4 ~7 ~4 ~6 ~7 minecraft:black_wool
fill ~8 ~4 ~7 ~8 ~6 ~7 minecraft:black_wool
fill ~3 ~5 ~0 ~5 ~5 ~0 minecraft:black_wool
setblock ~3 ~5 ~7 minecraft:black_wool
fill ~5 ~5 ~7 ~5 ~6 ~7 minecraft:black_wool
fill ~7 ~5 ~0 ~9 ~5 ~0 minecraft:black_wool
fill ~7 ~5 ~7 ~7 ~7 ~7 minecraft:black_wool
setblock ~9 ~5 ~7 minecraft:black_wool
fill ~4 ~6 ~0 ~5 ~6 ~0 minecraft:gray_wool
fill ~7 ~6 ~0 ~8 ~6 ~0 minecraft:black_wool
setblock ~5 ~7 ~0 minecraft:gray_wool
setblock ~5 ~7 ~7 minecraft:gray_wool
setblock ~7 ~7 ~0 minecraft:black_wool

# the canvas roof stepping from the eaves (y 4) to the ridge (y 9), with a hem of gray canvas along both eaves
fill ~1 ~4 ~0 ~1 ~4 ~7 minecraft:gray_wool
fill ~11 ~4 ~0 ~11 ~4 ~7 minecraft:gray_wool
setblock ~2 ~5 ~0 minecraft:gray_wool
fill ~2 ~5 ~1 ~2 ~5 ~5 minecraft:black_wool
fill ~2 ~5 ~6 ~2 ~5 ~7 minecraft:gray_wool
fill ~10 ~5 ~0 ~10 ~5 ~1 minecraft:gray_wool
fill ~10 ~5 ~2 ~10 ~5 ~6 minecraft:black_wool
setblock ~10 ~5 ~7 minecraft:gray_wool
setblock ~3 ~6 ~0 minecraft:gray_wool
fill ~3 ~6 ~1 ~3 ~6 ~5 minecraft:black_wool
fill ~3 ~6 ~6 ~3 ~6 ~7 minecraft:gray_wool
setblock ~9 ~6 ~0 minecraft:gray_wool
fill ~9 ~6 ~1 ~9 ~6 ~3 minecraft:black_wool
fill ~9 ~6 ~4 ~9 ~6 ~5 minecraft:gray_wool
setblock ~9 ~6 ~6 minecraft:black_wool
setblock ~9 ~6 ~7 minecraft:gray_wool
fill ~4 ~7 ~0 ~4 ~7 ~3 minecraft:gray_wool
fill ~4 ~7 ~4 ~4 ~7 ~6 minecraft:black_wool
setblock ~4 ~7 ~7 minecraft:gray_wool
setblock ~8 ~7 ~0 minecraft:gray_wool
fill ~8 ~7 ~1 ~8 ~7 ~6 minecraft:black_wool
setblock ~8 ~7 ~7 minecraft:gray_wool
fill ~5 ~8 ~0 ~5 ~8 ~3 minecraft:gray_wool
fill ~5 ~8 ~4 ~5 ~8 ~6 minecraft:black_wool
setblock ~5 ~8 ~7 minecraft:gray_wool
setblock ~7 ~8 ~0 minecraft:gray_wool
fill ~7 ~8 ~1 ~7 ~8 ~6 minecraft:black_wool
setblock ~7 ~8 ~7 minecraft:gray_wool
setblock ~6 ~9 ~0 minecraft:gray_wool
fill ~6 ~9 ~1 ~6 ~9 ~6 minecraft:black_wool
setblock ~6 ~9 ~7 minecraft:gray_wool

# the lintel over the door and the pole above it to the ridge
fill ~5 ~4 ~7 ~7 ~4 ~7 minecraft:iron_block
fill ~6 ~5 ~7 ~6 ~8 ~7 minecraft:dark_oak_log

# the ridge pole (dark_oak_log, y 10, over the body and the gable)
fill ~6 ~10 ~0 ~6 ~10 ~8 minecraft:dark_oak_log[axis=z]

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
fill ~1 ~7 ~10 ~3 ~7 ~10 minecraft:gray_wool
fill ~4 ~7 ~8 ~4 ~7 ~10 minecraft:gray_wool
fill ~5 ~7 ~8 ~7 ~7 ~9 minecraft:black_wool
fill ~5 ~7 ~10 ~7 ~7 ~10 minecraft:gray_wool
fill ~8 ~7 ~8 ~8 ~7 ~10 minecraft:gray_wool
fill ~9 ~7 ~8 ~11 ~7 ~9 minecraft:black_wool
fill ~9 ~7 ~10 ~11 ~7 ~10 minecraft:gray_wool
fill ~12 ~7 ~8 ~12 ~7 ~10 minecraft:gray_wool

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

# an iron finial on the back of the ridge pole
setblock ~6 ~11 ~0 minecraft:iron_block

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
setblock ~6 ~7 ~3 minecraft:lantern[hanging=true]
setblock ~6 ~8 ~3 minecraft:chain[axis=y]

# the banners: the House wall banner on the gable, the House standing banner on the pole
setblock ~0 ~7 ~10 minecraft:black_banner[rotation=0]{Patterns:[{Pattern:"bri",Color:7},{Pattern:"mc",Color:1},{Pattern:"bo",Color:4}]}
setblock ~6 ~9 ~9 minecraft:black_wall_banner[facing=south]{Patterns:[{Pattern:"bri",Color:7},{Pattern:"mc",Color:1},{Pattern:"bo",Color:4}]}
