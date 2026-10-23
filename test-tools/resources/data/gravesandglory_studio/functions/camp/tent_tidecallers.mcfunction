# size 13 12 11
# camp/tent_tidecallers: Tidecallers recruiter tent (sea-faded canvas on driftwood, prismarine, a tide pool, a mast and sea lanterns).
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Facing south. Body x 1..11, z 0..7 (a bowed sail: canvas slung between two curved driftwood spars that sweep from y 6 at the back up to a prow at y 11 over the porch, the belly two blocks under them); porch z 8..10 under the awning at y 7; plinth y 1 along z 9, display slots x 1..11 step 2 (feet y 2).
# Banners: the House wall banner at (6,9,9) facing south on the gable, the House standing banner at (0,7,10) on the dark oak fence pole (0,1..6,10).

# the floor of the body (prismarine bricks) and of the porch (driftwood (stripped oak) in a dark prismarine border)
fill ~0 ~0 ~8 ~0 ~0 ~10 minecraft:dark_prismarine
fill ~1 ~0 ~0 ~11 ~0 ~7 minecraft:prismarine_bricks
fill ~1 ~0 ~8 ~11 ~0 ~9 minecraft:stripped_oak_log[axis=x]
fill ~1 ~0 ~10 ~11 ~0 ~10 minecraft:dark_prismarine
fill ~12 ~0 ~8 ~12 ~0 ~10 minecraft:dark_prismarine

# the body: a bowed sail: canvas slung between two curved driftwood spars that sweep from y 6 at the back up to a prow at y 11 over the porch, the belly two blocks under them
fill ~1 ~1 ~0 ~11 ~1 ~0 minecraft:dark_prismarine
fill ~1 ~1 ~1 ~1 ~1 ~7 minecraft:dark_prismarine
setblock ~2 ~1 ~7 minecraft:dark_prismarine
setblock ~10 ~1 ~7 minecraft:dark_prismarine
fill ~11 ~1 ~1 ~11 ~1 ~7 minecraft:dark_prismarine
setblock ~1 ~2 ~0 minecraft:stripped_oak_log
setblock ~1 ~2 ~1 minecraft:cyan_terracotta
fill ~1 ~2 ~2 ~1 ~2 ~3 minecraft:gray_wool
fill ~1 ~2 ~4 ~1 ~7 ~6 minecraft:cyan_terracotta
setblock ~1 ~2 ~7 minecraft:stripped_oak_log
fill ~2 ~2 ~0 ~8 ~3 ~0 minecraft:cyan_terracotta
fill ~2 ~2 ~7 ~2 ~3 ~7 minecraft:light_gray_wool
fill ~9 ~2 ~0 ~10 ~3 ~0 minecraft:light_gray_wool
fill ~10 ~2 ~7 ~10 ~4 ~7 minecraft:cyan_terracotta
setblock ~11 ~2 ~0 minecraft:stripped_oak_log
fill ~11 ~2 ~1 ~11 ~3 ~1 minecraft:light_gray_wool
fill ~11 ~2 ~2 ~11 ~4 ~6 minecraft:cyan_terracotta
setblock ~11 ~2 ~7 minecraft:stripped_oak_log
setblock ~1 ~3 ~0 minecraft:cyan_terracotta
fill ~1 ~3 ~1 ~1 ~3 ~2 minecraft:gray_wool
fill ~1 ~3 ~3 ~1 ~7 ~3 minecraft:cyan_terracotta
fill ~1 ~3 ~7 ~1 ~10 ~7 minecraft:cyan_terracotta
setblock ~11 ~3 ~0 minecraft:light_gray_wool
fill ~11 ~3 ~7 ~11 ~10 ~7 minecraft:cyan_terracotta
fill ~1 ~4 ~0 ~2 ~5 ~0 minecraft:gray_wool
setblock ~1 ~4 ~1 minecraft:gray_wool
fill ~1 ~4 ~2 ~1 ~6 ~2 minecraft:cyan_terracotta
fill ~2 ~4 ~7 ~3 ~7 ~7 minecraft:cyan_terracotta
fill ~3 ~4 ~0 ~3 ~5 ~0 minecraft:cyan_wool
fill ~4 ~4 ~0 ~4 ~4 ~1 minecraft:gray_wool
fill ~5 ~4 ~0 ~6 ~4 ~0 minecraft:cyan_terracotta
setblock ~5 ~4 ~1 minecraft:gray_wool
setblock ~6 ~4 ~1 minecraft:cyan_terracotta
fill ~7 ~4 ~0 ~8 ~4 ~1 minecraft:gray_wool
fill ~9 ~4 ~0 ~9 ~5 ~0 minecraft:cyan_wool
fill ~9 ~4 ~7 ~9 ~5 ~7 minecraft:cyan_terracotta
fill ~10 ~4 ~0 ~11 ~5 ~0 minecraft:cyan_terracotta
fill ~11 ~4 ~1 ~11 ~5 ~1 minecraft:cyan_terracotta
fill ~1 ~5 ~1 ~2 ~5 ~1 minecraft:cyan_terracotta
setblock ~3 ~5 ~1 minecraft:cyan_wool
fill ~4 ~5 ~2 ~8 ~5 ~2 minecraft:cyan_terracotta
fill ~4 ~5 ~7 ~7 ~7 ~7 minecraft:cyan_terracotta
fill ~8 ~5 ~7 ~8 ~9 ~7 minecraft:gray_wool
setblock ~9 ~5 ~1 minecraft:cyan_wool
setblock ~10 ~5 ~1 minecraft:cyan_terracotta
setblock ~10 ~5 ~7 minecraft:light_gray_wool
fill ~11 ~5 ~2 ~11 ~6 ~5 minecraft:cyan_terracotta
setblock ~11 ~5 ~6 minecraft:light_gray_wool
fill ~1 ~6 ~0 ~1 ~6 ~1 minecraft:stripped_oak_log[axis=z]
setblock ~2 ~6 ~2 minecraft:cyan_terracotta
setblock ~3 ~6 ~2 minecraft:cyan_wool
fill ~4 ~6 ~3 ~8 ~6 ~4 minecraft:cyan_terracotta
setblock ~9 ~6 ~2 minecraft:cyan_wool
fill ~9 ~6 ~7 ~9 ~8 ~7 minecraft:gray_wool
setblock ~10 ~6 ~2 minecraft:cyan_terracotta
fill ~10 ~6 ~7 ~10 ~8 ~7 minecraft:cyan_terracotta
fill ~11 ~6 ~0 ~11 ~6 ~1 minecraft:stripped_oak_log[axis=z]
fill ~11 ~6 ~6 ~11 ~8 ~6 minecraft:cyan_terracotta
setblock ~1 ~7 ~2 minecraft:stripped_oak_log[axis=z]
fill ~2 ~7 ~3 ~2 ~7 ~4 minecraft:cyan_terracotta
fill ~3 ~7 ~3 ~3 ~7 ~4 minecraft:cyan_wool
fill ~4 ~7 ~5 ~7 ~7 ~5 minecraft:cyan_terracotta
setblock ~8 ~7 ~5 minecraft:gray_wool
fill ~9 ~7 ~3 ~9 ~7 ~4 minecraft:cyan_wool
fill ~10 ~7 ~3 ~11 ~7 ~4 minecraft:cyan_terracotta
setblock ~11 ~7 ~2 minecraft:stripped_oak_log[axis=z]
fill ~11 ~7 ~5 ~11 ~8 ~5 minecraft:cyan_terracotta
fill ~1 ~8 ~3 ~1 ~8 ~4 minecraft:stripped_oak_log[axis=z]
fill ~1 ~8 ~5 ~2 ~8 ~5 minecraft:cyan_terracotta
setblock ~1 ~8 ~6 minecraft:cyan_terracotta
fill ~2 ~8 ~7 ~3 ~10 ~7 minecraft:gray_wool
setblock ~3 ~8 ~5 minecraft:cyan_wool
fill ~4 ~8 ~6 ~7 ~8 ~6 minecraft:cyan_terracotta
fill ~4 ~8 ~7 ~6 ~8 ~7 minecraft:cyan_terracotta
fill ~7 ~8 ~7 ~7 ~9 ~7 minecraft:gray_wool
setblock ~8 ~8 ~6 minecraft:gray_wool
setblock ~9 ~8 ~5 minecraft:cyan_wool
setblock ~10 ~8 ~5 minecraft:cyan_terracotta
fill ~11 ~8 ~3 ~11 ~8 ~4 minecraft:stripped_oak_log[axis=z]
setblock ~1 ~9 ~5 minecraft:stripped_oak_log[axis=z]
setblock ~1 ~9 ~6 minecraft:light_gray_wool
setblock ~2 ~9 ~6 minecraft:gray_wool
setblock ~3 ~9 ~6 minecraft:cyan_wool
setblock ~4 ~9 ~7 minecraft:gray_wool
fill ~5 ~9 ~7 ~6 ~9 ~7 minecraft:cyan_terracotta
setblock ~9 ~9 ~6 minecraft:cyan_wool
fill ~9 ~9 ~7 ~10 ~10 ~7 minecraft:light_gray_wool
fill ~10 ~9 ~6 ~11 ~9 ~6 minecraft:light_gray_wool
setblock ~11 ~9 ~5 minecraft:stripped_oak_log[axis=z]
setblock ~1 ~10 ~6 minecraft:stripped_oak_log[axis=z]
setblock ~11 ~10 ~6 minecraft:stripped_oak_log[axis=z]
setblock ~1 ~11 ~7 minecraft:stripped_oak_log[axis=z]
setblock ~11 ~11 ~7 minecraft:stripped_oak_log[axis=z]

# the door posts and the lintel over the door (x 4..8)
fill ~3 ~1 ~7 ~3 ~3 ~7 minecraft:stripped_oak_log
fill ~9 ~1 ~7 ~9 ~3 ~7 minecraft:stripped_oak_log
fill ~4 ~4 ~7 ~8 ~4 ~7 minecraft:dark_prismarine

# a sea lantern in the belly of the sail at the prow, and rope stays (chains) from the spar tips to the awning
setblock ~6 ~10 ~7 minecraft:sea_lantern

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

# the awning bows like a sail: its corners lifted to y 8 on the posts, its belly sagging to y 6 between the middle slots
setblock ~4 ~6 ~10 minecraft:cyan_terracotta
fill ~5 ~6 ~10 ~7 ~6 ~10 minecraft:light_gray_wool
setblock ~8 ~6 ~10 minecraft:cyan_terracotta
setblock ~1 ~8 ~10 minecraft:dark_prismarine
setblock ~11 ~8 ~10 minecraft:dark_prismarine
setblock ~12 ~8 ~10 minecraft:stripped_oak_log

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
