# size 13 12 11
# camp/tent_tidecallers: Tidecallers recruiter tent (sea-faded canvas on driftwood, prismarine, a tide pool, a mast and sea lanterns).
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Facing south. Body x 1..11, z 0..7 (eave y 4, ridge cloth y 9, ridge pole y 10); porch z 8..10 under the awning at y 7; plinth y 1 along z 9, display slots x 1..11 step 2 (feet y 2).
# Banners: the House wall banner at (6,9,9) facing south on the gable, the House standing banner at (0,7,10) on the dark oak fence pole (0,1..6,10).

# the floor of the body (prismarine bricks) and of the porch (driftwood (stripped oak) in a dark prismarine border)
fill ~0 ~0 ~8 ~0 ~0 ~10 minecraft:dark_prismarine
fill ~1 ~0 ~0 ~11 ~0 ~7 minecraft:prismarine_bricks
fill ~1 ~0 ~8 ~11 ~0 ~9 minecraft:stripped_oak_log[axis=x]
fill ~1 ~0 ~10 ~11 ~0 ~10 minecraft:dark_prismarine
fill ~12 ~0 ~8 ~12 ~0 ~10 minecraft:dark_prismarine

# the frame: corner and door posts of stripped_oak_log from the floor to the eave, the back pole to the ridge
fill ~1 ~1 ~0 ~1 ~3 ~0 minecraft:stripped_oak_log
fill ~1 ~1 ~7 ~1 ~3 ~7 minecraft:stripped_oak_log
fill ~3 ~1 ~7 ~3 ~3 ~7 minecraft:stripped_oak_log
fill ~6 ~1 ~0 ~6 ~8 ~0 minecraft:stripped_oak_log
fill ~9 ~1 ~7 ~9 ~3 ~7 minecraft:stripped_oak_log
fill ~11 ~1 ~0 ~11 ~3 ~0 minecraft:stripped_oak_log
fill ~11 ~1 ~7 ~11 ~3 ~7 minecraft:stripped_oak_log

# the walls: a skirt of dark prismarine, then canvas; the front wall has the door (x 4..8)
fill ~1 ~1 ~1 ~1 ~1 ~6 minecraft:dark_prismarine
fill ~2 ~1 ~0 ~5 ~1 ~0 minecraft:dark_prismarine
setblock ~2 ~1 ~7 minecraft:dark_prismarine
fill ~7 ~1 ~0 ~10 ~1 ~0 minecraft:dark_prismarine
setblock ~10 ~1 ~7 minecraft:dark_prismarine
fill ~11 ~1 ~1 ~11 ~1 ~6 minecraft:dark_prismarine
setblock ~1 ~2 ~1 minecraft:cyan_terracotta
fill ~1 ~2 ~2 ~1 ~2 ~3 minecraft:gray_wool
fill ~1 ~2 ~4 ~1 ~3 ~6 minecraft:cyan_terracotta
fill ~2 ~2 ~0 ~5 ~3 ~0 minecraft:cyan_terracotta
fill ~2 ~2 ~7 ~2 ~3 ~7 minecraft:light_gray_wool
fill ~7 ~2 ~0 ~8 ~3 ~0 minecraft:cyan_terracotta
fill ~9 ~2 ~0 ~10 ~3 ~0 minecraft:light_gray_wool
fill ~10 ~2 ~7 ~10 ~4 ~7 minecraft:cyan_terracotta
fill ~11 ~2 ~1 ~11 ~3 ~1 minecraft:light_gray_wool
fill ~11 ~2 ~2 ~11 ~3 ~6 minecraft:cyan_terracotta
fill ~1 ~3 ~1 ~1 ~3 ~2 minecraft:gray_wool
setblock ~1 ~3 ~3 minecraft:cyan_terracotta
fill ~2 ~4 ~0 ~4 ~4 ~0 minecraft:gray_wool
fill ~2 ~4 ~7 ~3 ~4 ~7 minecraft:cyan_terracotta
setblock ~5 ~4 ~0 minecraft:cyan_terracotta
fill ~7 ~4 ~0 ~9 ~4 ~0 minecraft:gray_wool
fill ~9 ~4 ~7 ~9 ~5 ~7 minecraft:cyan_terracotta
setblock ~10 ~4 ~0 minecraft:cyan_terracotta
fill ~3 ~5 ~0 ~5 ~5 ~0 minecraft:gray_wool
fill ~3 ~5 ~7 ~7 ~5 ~7 minecraft:cyan_terracotta
fill ~7 ~5 ~0 ~8 ~5 ~0 minecraft:gray_wool
fill ~8 ~5 ~7 ~8 ~6 ~7 minecraft:gray_wool
setblock ~9 ~5 ~0 minecraft:cyan_terracotta
fill ~4 ~6 ~0 ~5 ~6 ~0 minecraft:cyan_terracotta
fill ~4 ~6 ~7 ~7 ~6 ~7 minecraft:cyan_terracotta
fill ~7 ~6 ~0 ~8 ~6 ~0 minecraft:cyan_terracotta
setblock ~5 ~7 ~0 minecraft:cyan_terracotta
fill ~5 ~7 ~7 ~7 ~7 ~7 minecraft:cyan_terracotta
setblock ~7 ~7 ~0 minecraft:cyan_terracotta
setblock ~6 ~8 ~7 minecraft:cyan_terracotta

# the canvas roof stepping from the eaves (y 4) to the ridge (y 9), with a hem of dark prismarine weights along both eaves
fill ~1 ~4 ~0 ~1 ~4 ~7 minecraft:dark_prismarine
fill ~11 ~4 ~0 ~11 ~4 ~7 minecraft:dark_prismarine
setblock ~2 ~5 ~0 minecraft:gray_wool
fill ~2 ~5 ~1 ~2 ~5 ~7 minecraft:cyan_terracotta
fill ~10 ~5 ~0 ~10 ~5 ~6 minecraft:cyan_terracotta
setblock ~10 ~5 ~7 minecraft:light_gray_wool
fill ~3 ~6 ~0 ~3 ~6 ~7 minecraft:cyan_wool
fill ~9 ~6 ~0 ~9 ~6 ~7 minecraft:cyan_wool
fill ~4 ~7 ~0 ~4 ~7 ~7 minecraft:cyan_terracotta
fill ~8 ~7 ~0 ~8 ~7 ~4 minecraft:cyan_terracotta
fill ~8 ~7 ~5 ~8 ~7 ~7 minecraft:gray_wool
fill ~5 ~8 ~0 ~5 ~8 ~2 minecraft:cyan_terracotta
setblock ~5 ~8 ~3 minecraft:gray_wool
fill ~5 ~8 ~4 ~5 ~8 ~7 minecraft:cyan_terracotta
fill ~7 ~8 ~0 ~7 ~8 ~6 minecraft:cyan_terracotta
setblock ~7 ~8 ~7 minecraft:gray_wool
setblock ~6 ~9 ~0 minecraft:cyan_terracotta
setblock ~6 ~9 ~1 minecraft:light_gray_wool
setblock ~6 ~9 ~2 minecraft:gray_wool
fill ~6 ~9 ~3 ~6 ~9 ~7 minecraft:cyan_terracotta

# the lintel over the door and the pole above it to the ridge
fill ~4 ~4 ~7 ~8 ~4 ~7 minecraft:dark_prismarine

# the ridge pole (stripped_oak_log, y 10, over the body and the gable)
fill ~6 ~10 ~0 ~6 ~10 ~8 minecraft:stripped_oak_log[axis=z]

# the porch posts at its four corners (the west front one is the dark oak fence pole of the standard)
setblock ~0 ~1 ~8 minecraft:oak_fence[north=false,east=false,south=true,west=false]
setblock ~0 ~1 ~10 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
setblock ~12 ~1 ~8 minecraft:oak_fence[north=false,east=false,south=true,west=false]
setblock ~12 ~1 ~10 minecraft:oak_fence[north=true,east=false,south=false,west=false]
fill ~0 ~2 ~8 ~0 ~6 ~8 minecraft:oak_fence[north=false,east=false,south=false,west=false]
fill ~0 ~2 ~10 ~0 ~6 ~10 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~12 ~2 ~8 ~12 ~6 ~8 minecraft:oak_fence[north=false,east=false,south=false,west=false]
fill ~12 ~2 ~10 ~12 ~6 ~10 minecraft:oak_fence[north=false,east=false,south=false,west=false]

# the awning at y 7 over the porch, its front edge in dark prismarine weights
fill ~0 ~7 ~8 ~1 ~7 ~9 minecraft:cyan_terracotta
fill ~1 ~7 ~10 ~12 ~7 ~10 minecraft:dark_prismarine
setblock ~2 ~7 ~8 minecraft:gray_wool
setblock ~2 ~7 ~9 minecraft:sea_lantern
fill ~3 ~7 ~8 ~3 ~7 ~9 minecraft:cyan_wool
fill ~4 ~7 ~8 ~5 ~7 ~8 minecraft:cyan_terracotta
setblock ~4 ~7 ~9 minecraft:gray_wool
setblock ~5 ~7 ~9 minecraft:cyan_terracotta
setblock ~6 ~7 ~8 minecraft:cyan_wool
setblock ~6 ~7 ~9 minecraft:sea_lantern
fill ~7 ~7 ~8 ~8 ~7 ~9 minecraft:gray_wool
fill ~9 ~7 ~8 ~9 ~7 ~9 minecraft:cyan_wool
fill ~10 ~7 ~8 ~12 ~7 ~8 minecraft:cyan_terracotta
setblock ~10 ~7 ~9 minecraft:sea_lantern
fill ~11 ~7 ~9 ~12 ~7 ~9 minecraft:cyan_terracotta

# the gable along z 8 above the awning (it carries the House wall banner)
fill ~5 ~8 ~8 ~7 ~8 ~8 minecraft:stripped_oak_log[axis=x]
setblock ~6 ~9 ~8 minecraft:stripped_oak_log[axis=x]

# the plinth at y 1 along z 9 (the six display slots at x 1..11 step 2 stand on it) and the step before it
setblock ~0 ~1 ~9 minecraft:dark_prismarine
setblock ~1 ~1 ~9 minecraft:prismarine_bricks
fill ~1 ~1 ~10 ~11 ~1 ~10 minecraft:dark_prismarine_slab[type=bottom]
setblock ~2 ~1 ~9 minecraft:dark_prismarine
setblock ~3 ~1 ~9 minecraft:prismarine_bricks
setblock ~4 ~1 ~9 minecraft:dark_prismarine
setblock ~5 ~1 ~9 minecraft:prismarine_bricks
setblock ~6 ~1 ~9 minecraft:dark_prismarine
setblock ~7 ~1 ~9 minecraft:prismarine_bricks
setblock ~8 ~1 ~9 minecraft:dark_prismarine
setblock ~9 ~1 ~9 minecraft:prismarine_bricks
setblock ~10 ~1 ~9 minecraft:dark_prismarine
setblock ~11 ~1 ~9 minecraft:prismarine_bricks
setblock ~12 ~1 ~9 minecraft:dark_prismarine

# a sea lantern finial on the back of the ridge pole, sea lanterns in the awning over the slots
setblock ~6 ~11 ~0 minecraft:sea_lantern

# the tide pool inside the open front: a prismarine basin (rim at y 1) with water, sea pickles and seagrass, lit by sea lanterns
setblock ~3 ~1 ~2 minecraft:sea_lantern
fill ~3 ~1 ~3 ~3 ~1 ~5 minecraft:prismarine_bricks
fill ~4 ~1 ~2 ~8 ~1 ~2 minecraft:prismarine_bricks
setblock ~4 ~1 ~3 minecraft:water
setblock ~4 ~1 ~4 minecraft:seagrass
fill ~4 ~1 ~5 ~8 ~1 ~5 minecraft:prismarine_bricks
setblock ~5 ~1 ~3 minecraft:sea_pickle[pickles=3,waterlogged=true]
fill ~5 ~1 ~4 ~6 ~1 ~4 minecraft:water
fill ~6 ~1 ~3 ~7 ~1 ~3 minecraft:water
setblock ~7 ~1 ~4 minecraft:sea_pickle[pickles=2,waterlogged=true]
setblock ~8 ~1 ~3 minecraft:seagrass
setblock ~8 ~1 ~4 minecraft:water
setblock ~9 ~1 ~2 minecraft:sea_lantern
fill ~9 ~1 ~3 ~9 ~1 ~5 minecraft:prismarine_bricks

# the shore stores on the sides: barrels, nets (oak fence), a prismarine wall of shells, rope coils of hay
setblock ~0 ~1 ~1 minecraft:barrel[facing=up]
setblock ~0 ~1 ~3 minecraft:oak_fence[north=false,east=true,south=true,west=false]
setblock ~0 ~1 ~4 minecraft:oak_fence[north=true,east=true,south=false,west=false]
setblock ~0 ~1 ~6 minecraft:prismarine_wall[north=none,east=low,south=none,west=none,up=true]
setblock ~12 ~1 ~2 minecraft:prismarine_wall[north=none,east=none,south=low,west=low,up=true]
setblock ~12 ~1 ~3 minecraft:dark_prismarine
setblock ~12 ~1 ~5 minecraft:barrel[facing=up]
setblock ~12 ~1 ~6 minecraft:hay_block[axis=z]
setblock ~0 ~2 ~1 minecraft:barrel[facing=west]
setblock ~12 ~2 ~3 minecraft:sea_lantern

# the mast at the east back corner: a stripped oak pole to y 11 with a yard and a furled sail of white and light gray canvas
fill ~12 ~1 ~0 ~12 ~11 ~0 minecraft:stripped_oak_log
setblock ~12 ~8 ~1 minecraft:light_gray_wool
setblock ~12 ~9 ~1 minecraft:white_wool
setblock ~12 ~10 ~1 minecraft:stripped_oak_log[axis=z]

# lanterns on the plinth ends
setblock ~0 ~2 ~9 minecraft:lantern
setblock ~12 ~2 ~9 minecraft:lantern

# the banners: the House wall banner on the gable, the House standing banner on the pole
setblock ~0 ~7 ~10 minecraft:blue_banner[rotation=0]{Patterns:[{Pattern:"gru",Color:9},{Pattern:"bts",Color:0},{Pattern:"bo",Color:9}]}
setblock ~6 ~9 ~9 minecraft:blue_wall_banner[facing=south]{Patterns:[{Pattern:"gru",Color:9},{Pattern:"bts",Color:0},{Pattern:"bo",Color:9}]}
