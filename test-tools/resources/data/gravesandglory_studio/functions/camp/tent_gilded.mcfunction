# size 13 12 11
# camp/tent_gilded: Gilded Order recruiter tent (soiled linen and dull gilt canvas on birch, a sandstone dais, gold finials).
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Facing south. Body x 1..11, z 0..7 (eave y 4, ridge cloth y 9, ridge pole y 10); porch z 8..10 under the awning at y 7; plinth y 1 along z 9, display slots x 1..11 step 2 (feet y 2).
# Banners: the House wall banner at (6,9,9) facing south on the gable, the House standing banner at (0,7,10) on the dark oak fence pole (0,1..6,10).

# the floor of the body (birch planks) and of the porch (smooth sandstone with a cut sandstone border)
fill ~0 ~0 ~8 ~0 ~0 ~10 minecraft:cut_sandstone
fill ~1 ~0 ~0 ~11 ~0 ~7 minecraft:birch_planks
fill ~1 ~0 ~8 ~11 ~0 ~9 minecraft:smooth_sandstone
fill ~1 ~0 ~10 ~11 ~0 ~10 minecraft:cut_sandstone
fill ~12 ~0 ~8 ~12 ~0 ~10 minecraft:cut_sandstone

# the frame: corner and door posts of birch_log from the floor to the eave, the back pole to the ridge
fill ~1 ~1 ~0 ~1 ~3 ~0 minecraft:birch_log
fill ~1 ~1 ~7 ~1 ~3 ~7 minecraft:birch_log
fill ~4 ~1 ~7 ~4 ~3 ~7 minecraft:birch_log
fill ~6 ~1 ~0 ~6 ~8 ~0 minecraft:birch_log
fill ~8 ~1 ~7 ~8 ~3 ~7 minecraft:birch_log
fill ~11 ~1 ~0 ~11 ~3 ~0 minecraft:birch_log
fill ~11 ~1 ~7 ~11 ~3 ~7 minecraft:birch_log

# the walls: a skirt of birch planks, then canvas; the front wall has the door (x 5..7)
fill ~1 ~1 ~1 ~1 ~1 ~6 minecraft:birch_planks
fill ~2 ~1 ~0 ~5 ~1 ~0 minecraft:birch_planks
fill ~2 ~1 ~7 ~3 ~1 ~7 minecraft:birch_planks
fill ~7 ~1 ~0 ~10 ~1 ~0 minecraft:birch_planks
fill ~9 ~1 ~7 ~10 ~1 ~7 minecraft:birch_planks
fill ~11 ~1 ~1 ~11 ~1 ~6 minecraft:birch_planks
fill ~1 ~2 ~1 ~1 ~3 ~4 minecraft:light_gray_wool
setblock ~1 ~2 ~5 minecraft:white_wool
fill ~1 ~2 ~6 ~1 ~3 ~6 minecraft:light_gray_wool
fill ~2 ~2 ~0 ~4 ~4 ~0 minecraft:light_gray_wool
fill ~2 ~2 ~7 ~2 ~4 ~7 minecraft:white_wool
setblock ~3 ~2 ~7 minecraft:light_gray_wool
fill ~5 ~2 ~0 ~5 ~7 ~0 minecraft:white_wool
setblock ~7 ~2 ~0 minecraft:light_gray_wool
fill ~8 ~2 ~0 ~8 ~6 ~0 minecraft:white_wool
fill ~9 ~2 ~0 ~10 ~4 ~0 minecraft:light_gray_wool
fill ~9 ~2 ~7 ~9 ~5 ~7 minecraft:white_wool
setblock ~10 ~2 ~7 minecraft:light_gray_wool
fill ~11 ~2 ~1 ~11 ~2 ~3 minecraft:light_gray_wool
fill ~11 ~2 ~4 ~11 ~3 ~5 minecraft:white_wool
fill ~11 ~2 ~6 ~11 ~3 ~6 minecraft:light_gray_wool
setblock ~1 ~3 ~5 minecraft:light_gray_wool
fill ~3 ~3 ~7 ~3 ~5 ~7 minecraft:white_wool
fill ~7 ~3 ~0 ~7 ~7 ~0 minecraft:white_wool
fill ~10 ~3 ~7 ~10 ~4 ~7 minecraft:white_wool
fill ~11 ~3 ~1 ~11 ~3 ~3 minecraft:white_wool
fill ~4 ~4 ~7 ~4 ~6 ~7 minecraft:white_wool
fill ~8 ~4 ~7 ~8 ~5 ~7 minecraft:light_gray_wool
fill ~3 ~5 ~0 ~4 ~5 ~0 minecraft:light_gray_wool
fill ~5 ~5 ~7 ~5 ~7 ~7 minecraft:white_wool
setblock ~7 ~5 ~7 minecraft:light_gray_wool
setblock ~9 ~5 ~0 minecraft:white_wool
setblock ~4 ~6 ~0 minecraft:light_gray_wool
fill ~7 ~6 ~7 ~8 ~6 ~7 minecraft:white_wool
setblock ~7 ~7 ~7 minecraft:white_wool

# the canvas roof stepping from the eaves (y 4) to the ridge (y 9), with a hem of ochre canvas (gilt gone dull) along both eaves
fill ~1 ~4 ~0 ~1 ~4 ~7 minecraft:yellow_terracotta
fill ~11 ~4 ~0 ~11 ~4 ~7 minecraft:yellow_terracotta
setblock ~2 ~5 ~0 minecraft:white_wool
fill ~2 ~5 ~1 ~2 ~5 ~5 minecraft:light_gray_wool
fill ~2 ~5 ~6 ~2 ~5 ~7 minecraft:white_wool
fill ~10 ~5 ~0 ~10 ~5 ~3 minecraft:light_gray_wool
fill ~10 ~5 ~4 ~10 ~5 ~7 minecraft:white_wool
fill ~3 ~6 ~0 ~3 ~6 ~3 minecraft:light_gray_wool
fill ~3 ~6 ~4 ~3 ~6 ~7 minecraft:white_wool
fill ~9 ~6 ~0 ~9 ~6 ~7 minecraft:white_wool
fill ~4 ~7 ~0 ~4 ~7 ~1 minecraft:light_gray_wool
fill ~4 ~7 ~2 ~4 ~7 ~7 minecraft:white_wool
fill ~8 ~7 ~0 ~8 ~7 ~7 minecraft:white_wool
fill ~5 ~8 ~0 ~5 ~8 ~1 minecraft:light_gray_wool
fill ~5 ~8 ~2 ~5 ~8 ~7 minecraft:white_wool
fill ~7 ~8 ~0 ~7 ~8 ~3 minecraft:white_wool
setblock ~7 ~8 ~4 minecraft:light_gray_wool
fill ~7 ~8 ~5 ~7 ~8 ~7 minecraft:white_wool
fill ~6 ~9 ~0 ~6 ~9 ~7 minecraft:yellow_terracotta

# the lintel over the door and the pole above it to the ridge
fill ~5 ~4 ~7 ~7 ~4 ~7 minecraft:stripped_birch_log[axis=x]
fill ~6 ~5 ~7 ~6 ~8 ~7 minecraft:birch_log

# the ridge pole (birch_log, y 10, over the body and the gable)
fill ~6 ~10 ~0 ~6 ~10 ~8 minecraft:birch_log[axis=z]

# the porch posts at its four corners (the west front one is the dark oak fence pole of the standard)
setblock ~0 ~1 ~8 minecraft:birch_fence[north=false,east=false,south=true,west=false]
setblock ~0 ~1 ~10 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
setblock ~12 ~1 ~8 minecraft:birch_fence[north=false,east=false,south=true,west=false]
setblock ~12 ~1 ~10 minecraft:birch_fence[north=true,east=false,south=false,west=false]
fill ~0 ~2 ~8 ~0 ~6 ~8 minecraft:birch_fence[north=false,east=false,south=false,west=false]
fill ~0 ~2 ~10 ~0 ~6 ~10 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~12 ~2 ~8 ~12 ~6 ~8 minecraft:birch_fence[north=false,east=false,south=false,west=false]
fill ~12 ~2 ~10 ~12 ~6 ~10 minecraft:birch_fence[north=false,east=false,south=false,west=false]

# the awning at y 7 over the porch, its front edge in ochre canvas (gilt gone dull)
fill ~0 ~7 ~8 ~0 ~7 ~9 minecraft:light_gray_wool
fill ~1 ~7 ~8 ~2 ~7 ~9 minecraft:white_wool
fill ~1 ~7 ~10 ~2 ~7 ~10 minecraft:yellow_terracotta
fill ~3 ~7 ~8 ~3 ~7 ~10 minecraft:yellow_terracotta
fill ~4 ~7 ~8 ~8 ~7 ~9 minecraft:white_wool
fill ~4 ~7 ~10 ~8 ~7 ~10 minecraft:yellow_terracotta
fill ~9 ~7 ~8 ~9 ~7 ~10 minecraft:yellow_terracotta
fill ~10 ~7 ~8 ~12 ~7 ~9 minecraft:white_wool
fill ~10 ~7 ~10 ~12 ~7 ~10 minecraft:yellow_terracotta

# the gable along z 8 above the awning (it carries the House wall banner)
fill ~5 ~8 ~8 ~7 ~8 ~8 minecraft:stripped_birch_log[axis=x]
setblock ~6 ~9 ~8 minecraft:stripped_birch_log[axis=x]

# the plinth at y 1 along z 9 (the six display slots at x 1..11 step 2 stand on it) and the step before it
setblock ~0 ~1 ~9 minecraft:gold_block
setblock ~1 ~1 ~9 minecraft:chiseled_sandstone
fill ~1 ~1 ~10 ~11 ~1 ~10 minecraft:smooth_sandstone_slab[type=bottom]
setblock ~2 ~1 ~9 minecraft:cut_sandstone
setblock ~3 ~1 ~9 minecraft:chiseled_sandstone
setblock ~4 ~1 ~9 minecraft:cut_sandstone
setblock ~5 ~1 ~9 minecraft:chiseled_sandstone
setblock ~6 ~1 ~9 minecraft:cut_sandstone
setblock ~7 ~1 ~9 minecraft:chiseled_sandstone
setblock ~8 ~1 ~9 minecraft:cut_sandstone
setblock ~9 ~1 ~9 minecraft:chiseled_sandstone
setblock ~10 ~1 ~9 minecraft:cut_sandstone
setblock ~11 ~1 ~9 minecraft:chiseled_sandstone
setblock ~12 ~1 ~9 minecraft:gold_block

# gold finials: a gold block on the back of the ridge pole and on the two back corner posts
setblock ~6 ~11 ~0 minecraft:gold_block

# the armoury along the east side: a weapon stand of birch, a smithing table, a grindstone, barrels and a bell on a quartz post
fill ~12 ~1 ~1 ~12 ~2 ~1 minecraft:barrel[facing=up]
setblock ~12 ~1 ~2 minecraft:smithing_table
setblock ~12 ~1 ~3 minecraft:grindstone[face=floor,facing=east]
fill ~12 ~1 ~5 ~12 ~2 ~5 minecraft:quartz_pillar
setblock ~12 ~1 ~6 minecraft:barrel[facing=east]

# the west side: a quartz table with candles, a chest, hay for the horses
fill ~0 ~1 ~2 ~0 ~1 ~3 minecraft:quartz_slab[type=top]
setblock ~0 ~1 ~5 minecraft:chest[facing=west]
fill ~0 ~1 ~6 ~0 ~2 ~6 minecraft:hay_block

# inside: a birch table with a map, bedrolls of white and yellow carpet, a lantern on the back pole
fill ~2 ~1 ~4 ~3 ~1 ~5 minecraft:white_carpet
fill ~3 ~1 ~2 ~4 ~1 ~2 minecraft:stripped_birch_log[axis=x]
setblock ~6 ~1 ~3 minecraft:cartography_table
fill ~8 ~1 ~2 ~9 ~1 ~2 minecraft:stripped_birch_log[axis=x]
fill ~9 ~1 ~4 ~10 ~1 ~5 minecraft:yellow_carpet

# the bell on its post and the candles on the table; lanterns at the porch
setblock ~0 ~2 ~2 minecraft:candle[candles=3,lit=true]
setblock ~0 ~2 ~3 minecraft:white_candle[candles=2,lit=true]
setblock ~0 ~2 ~9 minecraft:lantern
setblock ~12 ~2 ~9 minecraft:lantern
setblock ~12 ~3 ~5 minecraft:bell[attachment=floor,facing=east]
setblock ~6 ~7 ~1 minecraft:lantern[hanging=true]
setblock ~6 ~8 ~1 minecraft:chain[axis=y]

# the banners: the House wall banner on the gable, the House standing banner on the pole
setblock ~0 ~7 ~10 minecraft:white_banner[rotation=0]{Patterns:[{Pattern:"sc",Color:4},{Pattern:"bo",Color:4}]}
setblock ~6 ~9 ~9 minecraft:white_wall_banner[facing=south]{Patterns:[{Pattern:"sc",Color:4},{Pattern:"bo",Color:4}]}
