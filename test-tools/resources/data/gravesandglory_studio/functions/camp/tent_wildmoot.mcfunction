# size 13 12 11
# camp/tent_wildmoot: Wildmoot recruiter tent (stitched hides on spruce, bone tusks and totems, hay and moss).
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Facing south. Body x 1..11, z 0..7 (eave y 4, ridge cloth y 9, ridge pole y 10); porch z 8..10 under the awning at y 7; plinth y 1 along z 9, display slots x 1..11 step 2 (feet y 2).
# Banners: the House wall banner at (6,9,9) facing south on the gable, the House standing banner at (0,7,10) on the dark oak fence pole (0,1..6,10).

# the floor of the body (spruce planks under rushes) and of the porch (spruce planks and moss)
setblock ~0 ~0 ~8 minecraft:rooted_dirt
setblock ~0 ~0 ~9 minecraft:moss_block
setblock ~0 ~0 ~10 minecraft:rooted_dirt
fill ~1 ~0 ~0 ~11 ~0 ~9 minecraft:spruce_planks
fill ~1 ~0 ~10 ~11 ~0 ~10 minecraft:moss_block
fill ~12 ~0 ~8 ~12 ~0 ~9 minecraft:moss_block
setblock ~12 ~0 ~10 minecraft:rooted_dirt

# the frame: corner and door posts of spruce_log from the floor to the eave, the back pole to the ridge
fill ~1 ~1 ~0 ~1 ~3 ~0 minecraft:spruce_log
fill ~1 ~1 ~7 ~1 ~3 ~7 minecraft:spruce_log
fill ~4 ~1 ~7 ~4 ~3 ~7 minecraft:spruce_log
fill ~6 ~1 ~0 ~6 ~8 ~0 minecraft:spruce_log
fill ~8 ~1 ~7 ~8 ~3 ~7 minecraft:spruce_log
fill ~11 ~1 ~0 ~11 ~3 ~0 minecraft:spruce_log
fill ~11 ~1 ~7 ~11 ~3 ~7 minecraft:spruce_log

# the walls: a skirt of stripped spruce logs, then canvas; the front wall has the door (x 5..7)
fill ~1 ~1 ~1 ~1 ~1 ~6 minecraft:stripped_spruce_log[axis=z]
fill ~2 ~1 ~0 ~5 ~1 ~0 minecraft:stripped_spruce_log[axis=x]
fill ~2 ~1 ~7 ~3 ~1 ~7 minecraft:stripped_spruce_log[axis=x]
fill ~7 ~1 ~0 ~10 ~1 ~0 minecraft:stripped_spruce_log[axis=x]
fill ~9 ~1 ~7 ~10 ~1 ~7 minecraft:stripped_spruce_log[axis=x]
fill ~11 ~1 ~1 ~11 ~1 ~6 minecraft:stripped_spruce_log[axis=z]
fill ~1 ~2 ~1 ~1 ~3 ~2 minecraft:orange_terracotta
fill ~1 ~2 ~3 ~1 ~3 ~5 minecraft:packed_mud
fill ~1 ~2 ~6 ~1 ~3 ~6 minecraft:brown_terracotta
fill ~2 ~2 ~0 ~2 ~3 ~0 minecraft:orange_terracotta
fill ~2 ~2 ~7 ~3 ~3 ~7 minecraft:brown_terracotta
fill ~3 ~2 ~0 ~5 ~3 ~0 minecraft:brown_terracotta
fill ~7 ~2 ~0 ~8 ~2 ~0 minecraft:brown_wool
fill ~9 ~2 ~0 ~10 ~4 ~0 minecraft:brown_terracotta
fill ~9 ~2 ~7 ~10 ~4 ~7 minecraft:brown_wool
setblock ~11 ~2 ~1 minecraft:brown_terracotta
fill ~11 ~2 ~2 ~11 ~3 ~6 minecraft:brown_wool
setblock ~7 ~3 ~0 minecraft:packed_mud
fill ~8 ~3 ~0 ~8 ~4 ~0 minecraft:brown_wool
setblock ~11 ~3 ~1 minecraft:brown_wool
fill ~2 ~4 ~0 ~4 ~4 ~0 minecraft:brown_wool
setblock ~2 ~4 ~7 minecraft:brown_terracotta
setblock ~3 ~4 ~7 minecraft:white_terracotta
setblock ~4 ~4 ~7 minecraft:brown_wool
setblock ~5 ~4 ~0 minecraft:white_terracotta
fill ~7 ~4 ~0 ~7 ~7 ~0 minecraft:brown_wool
fill ~8 ~4 ~7 ~8 ~6 ~7 minecraft:brown_wool
fill ~3 ~5 ~0 ~5 ~5 ~0 minecraft:brown_wool
setblock ~3 ~5 ~7 minecraft:brown_wool
fill ~4 ~5 ~7 ~5 ~5 ~7 minecraft:white_terracotta
fill ~6 ~5 ~7 ~7 ~5 ~7 minecraft:brown_terracotta
setblock ~8 ~5 ~0 minecraft:packed_mud
setblock ~9 ~5 ~0 minecraft:brown_terracotta
setblock ~9 ~5 ~7 minecraft:brown_wool
fill ~4 ~6 ~0 ~5 ~6 ~0 minecraft:brown_wool
fill ~4 ~6 ~7 ~5 ~6 ~7 minecraft:brown_wool
fill ~6 ~6 ~7 ~7 ~6 ~7 minecraft:packed_mud
setblock ~8 ~6 ~0 minecraft:brown_wool
setblock ~5 ~7 ~0 minecraft:brown_wool
fill ~5 ~7 ~7 ~7 ~7 ~7 minecraft:brown_wool
setblock ~6 ~8 ~7 minecraft:brown_wool

# the canvas roof stepping from the eaves (y 4) to the ridge (y 9), with a hem of rough spruce poles along both eaves
fill ~1 ~4 ~0 ~1 ~4 ~7 minecraft:stripped_spruce_log[axis=z]
fill ~11 ~4 ~0 ~11 ~4 ~7 minecraft:stripped_spruce_log[axis=z]
fill ~2 ~5 ~0 ~2 ~5 ~7 minecraft:brown_wool
fill ~10 ~5 ~0 ~10 ~5 ~2 minecraft:brown_terracotta
fill ~10 ~5 ~3 ~10 ~5 ~7 minecraft:brown_wool
fill ~3 ~6 ~0 ~3 ~6 ~7 minecraft:brown_wool
fill ~9 ~6 ~0 ~9 ~6 ~4 minecraft:brown_terracotta
fill ~9 ~6 ~5 ~9 ~6 ~7 minecraft:brown_wool
fill ~4 ~7 ~0 ~4 ~7 ~7 minecraft:brown_wool
fill ~8 ~7 ~0 ~8 ~7 ~7 minecraft:brown_wool
fill ~5 ~8 ~0 ~5 ~8 ~7 minecraft:brown_wool
fill ~7 ~8 ~0 ~7 ~8 ~7 minecraft:brown_wool
fill ~6 ~9 ~0 ~6 ~9 ~7 minecraft:brown_wool

# the lintel over the door and the pole above it to the ridge
fill ~5 ~4 ~7 ~7 ~4 ~7 minecraft:stripped_spruce_log[axis=x]

# the ridge pole (spruce_log, y 10, over the body and the gable)
fill ~6 ~10 ~0 ~6 ~10 ~8 minecraft:spruce_log[axis=z]

# the porch posts at its four corners (the west front one is the dark oak fence pole of the standard)
setblock ~0 ~1 ~8 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~0 ~1 ~10 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
setblock ~12 ~1 ~8 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~12 ~1 ~10 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
fill ~0 ~2 ~8 ~0 ~6 ~8 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~0 ~2 ~10 ~0 ~6 ~10 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~12 ~2 ~8 ~12 ~6 ~8 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~12 ~2 ~10 ~12 ~6 ~10 minecraft:spruce_fence[north=false,east=false,south=false,west=false]

# the awning at y 7 over the porch, its front edge in rough spruce poles
fill ~0 ~7 ~8 ~1 ~7 ~8 minecraft:brown_wool
setblock ~0 ~7 ~9 minecraft:brown_terracotta
setblock ~1 ~7 ~9 minecraft:brown_wool
fill ~1 ~7 ~10 ~12 ~7 ~10 minecraft:stripped_spruce_log[axis=z]
fill ~2 ~7 ~8 ~2 ~7 ~9 minecraft:brown_terracotta
fill ~3 ~7 ~8 ~3 ~7 ~9 minecraft:brown_wool
setblock ~4 ~7 ~8 minecraft:brown_terracotta
setblock ~4 ~7 ~9 minecraft:brown_wool
fill ~5 ~7 ~8 ~7 ~7 ~9 minecraft:brown_wool
fill ~8 ~7 ~8 ~9 ~7 ~8 minecraft:brown_terracotta
setblock ~8 ~7 ~9 minecraft:brown_terracotta
fill ~9 ~7 ~9 ~10 ~7 ~9 minecraft:brown_wool
fill ~10 ~7 ~8 ~11 ~7 ~8 minecraft:brown_wool
setblock ~11 ~7 ~9 minecraft:brown_terracotta
setblock ~12 ~7 ~8 minecraft:brown_terracotta
setblock ~12 ~7 ~9 minecraft:brown_wool

# the gable along z 8 above the awning (it carries the House wall banner)
fill ~5 ~8 ~8 ~7 ~8 ~8 minecraft:spruce_planks
setblock ~6 ~9 ~8 minecraft:spruce_planks

# the plinth at y 1 along z 9 (the six display slots at x 1..11 step 2 stand on it) and the step before it
setblock ~0 ~1 ~9 minecraft:bone_block
fill ~1 ~1 ~9 ~11 ~1 ~9 minecraft:stripped_spruce_log[axis=x]
fill ~1 ~1 ~10 ~11 ~1 ~10 minecraft:spruce_slab[type=bottom]
setblock ~12 ~1 ~9 minecraft:bone_block

# a bone finial on the back of the ridge pole
setblock ~6 ~11 ~0 minecraft:bone_block[axis=y]

# bone tusks over the front of the ridge (a pair rising from the gable) and bone hooks beside it
setblock ~4 ~9 ~8 minecraft:bone_block[axis=x]
setblock ~8 ~9 ~8 minecraft:bone_block[axis=x]
setblock ~5 ~10 ~8 minecraft:bone_block[axis=y]
setblock ~7 ~10 ~8 minecraft:bone_block[axis=y]

# the bone totem at the east back corner: bone blocks on a mossy stone, a skull on top
setblock ~12 ~1 ~1 minecraft:mossy_cobblestone
fill ~12 ~2 ~1 ~12 ~3 ~1 minecraft:bone_block[axis=y]
setblock ~12 ~4 ~1 minecraft:skeleton_skull[rotation=0]

# hay bales stacked along the west side, moss underfoot
setblock ~0 ~0 ~4 minecraft:moss_block
setblock ~0 ~0 ~6 minecraft:moss_block
setblock ~12 ~0 ~3 minecraft:moss_block
setblock ~12 ~0 ~6 minecraft:moss_block
fill ~0 ~1 ~1 ~0 ~1 ~3 minecraft:hay_block[axis=z]
setblock ~0 ~1 ~6 minecraft:moss_carpet
setblock ~12 ~1 ~3 minecraft:moss_carpet
setblock ~0 ~2 ~2 minecraft:hay_block[axis=z]

# a drying rack of spruce fence and a barrel on the east side
setblock ~12 ~1 ~4 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~12 ~1 ~5 minecraft:barrel[facing=up]
setblock ~12 ~2 ~4 minecraft:spruce_fence[north=false,east=false,south=false,west=true]

# inside: furs (brown and white carpet), a fire bowl, a lantern on the back pole
fill ~2 ~1 ~2 ~4 ~1 ~2 minecraft:brown_carpet
fill ~2 ~1 ~3 ~2 ~1 ~5 minecraft:brown_carpet
fill ~3 ~1 ~3 ~3 ~1 ~4 minecraft:white_carpet
setblock ~3 ~1 ~5 minecraft:brown_carpet
fill ~4 ~1 ~3 ~4 ~1 ~5 minecraft:brown_carpet
setblock ~6 ~1 ~3 minecraft:campfire[lit=false,facing=south]
fill ~8 ~1 ~2 ~10 ~1 ~5 minecraft:brown_carpet
setblock ~6 ~7 ~1 minecraft:lantern[hanging=true]
setblock ~6 ~8 ~1 minecraft:chain[axis=y]

# bone posts at the plinth ends carry lanterns
setblock ~0 ~2 ~9 minecraft:lantern
setblock ~12 ~2 ~9 minecraft:lantern

# the banners: the House wall banner on the gable, the House standing banner on the pole
setblock ~0 ~7 ~10 minecraft:brown_banner[rotation=0]{Patterns:[{Pattern:"tts",Color:0},{Pattern:"bts",Color:0},{Pattern:"mr",Color:1}]}
setblock ~6 ~9 ~9 minecraft:brown_wall_banner[facing=south]{Patterns:[{Pattern:"tts",Color:0},{Pattern:"bts",Color:0},{Pattern:"mr",Color:1}]}
