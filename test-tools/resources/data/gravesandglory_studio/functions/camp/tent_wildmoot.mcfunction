# size 13 12 11
# camp/tent_wildmoot: Wildmoot recruiter tent (stitched hides on spruce, bone tusks and totems, hay and moss).
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Facing south. Body x 1..11, z 0..7 (a rounded lodge of hides: a low dome on spruce ribs, rounded at the back, the crown at y 9 with antlers); porch z 8..10 under the awning at y 7; plinth y 1 along z 9, display slots x 1..11 step 2 (feet y 2).
# Banners: the House wall banner at (6,9,9) facing south on the gable, the House standing banner at (0,7,10) on the dark oak fence pole (0,1..6,10).

# the floor of the body (spruce planks under rushes) and of the porch (spruce planks and moss)
setblock ~0 ~0 ~8 minecraft:rooted_dirt
setblock ~0 ~0 ~9 minecraft:moss_block
setblock ~0 ~0 ~10 minecraft:rooted_dirt
fill ~1 ~0 ~0 ~11 ~0 ~9 minecraft:spruce_planks
fill ~1 ~0 ~10 ~11 ~0 ~10 minecraft:moss_block
fill ~12 ~0 ~8 ~12 ~0 ~9 minecraft:moss_block
setblock ~12 ~0 ~10 minecraft:rooted_dirt

# the body: a rounded lodge of hides: a low dome on spruce ribs, rounded at the back, the crown at y 9 with antlers
setblock ~1 ~1 ~2 minecraft:mud_bricks
setblock ~1 ~1 ~3 minecraft:stripped_spruce_log[axis=y]
fill ~1 ~1 ~4 ~1 ~1 ~6 minecraft:mud_bricks
setblock ~1 ~1 ~7 minecraft:stripped_spruce_log[axis=y]
setblock ~2 ~1 ~1 minecraft:mud_bricks
fill ~2 ~1 ~7 ~3 ~1 ~7 minecraft:mud_bricks
fill ~3 ~1 ~0 ~9 ~1 ~0 minecraft:mud_bricks
fill ~9 ~1 ~7 ~10 ~1 ~7 minecraft:mud_bricks
setblock ~10 ~1 ~1 minecraft:mud_bricks
setblock ~11 ~1 ~2 minecraft:mud_bricks
setblock ~11 ~1 ~3 minecraft:stripped_spruce_log[axis=y]
fill ~11 ~1 ~4 ~11 ~1 ~6 minecraft:mud_bricks
setblock ~11 ~1 ~7 minecraft:stripped_spruce_log[axis=y]
setblock ~1 ~2 ~2 minecraft:orange_terracotta
fill ~1 ~2 ~3 ~1 ~2 ~5 minecraft:packed_mud
fill ~1 ~2 ~6 ~1 ~3 ~7 minecraft:brown_terracotta
fill ~2 ~2 ~1 ~2 ~3 ~1 minecraft:orange_terracotta
fill ~2 ~2 ~7 ~3 ~3 ~7 minecraft:brown_terracotta
fill ~3 ~2 ~0 ~5 ~3 ~0 minecraft:brown_terracotta
fill ~6 ~2 ~0 ~6 ~4 ~0 minecraft:packed_mud
fill ~7 ~2 ~0 ~8 ~2 ~0 minecraft:brown_wool
fill ~9 ~2 ~0 ~9 ~3 ~0 minecraft:brown_terracotta
fill ~9 ~2 ~7 ~10 ~5 ~7 minecraft:brown_wool
fill ~10 ~2 ~1 ~10 ~3 ~1 minecraft:brown_terracotta
fill ~11 ~2 ~2 ~11 ~2 ~7 minecraft:brown_wool
setblock ~1 ~3 ~2 minecraft:stripped_spruce_wood
fill ~1 ~3 ~3 ~1 ~3 ~4 minecraft:packed_mud
setblock ~1 ~3 ~5 minecraft:stripped_spruce_wood
setblock ~7 ~3 ~0 minecraft:packed_mud
fill ~8 ~3 ~0 ~8 ~4 ~0 minecraft:brown_wool
setblock ~11 ~3 ~2 minecraft:stripped_spruce_wood
fill ~11 ~3 ~3 ~11 ~3 ~4 minecraft:brown_wool
setblock ~11 ~3 ~5 minecraft:stripped_spruce_wood
fill ~11 ~3 ~6 ~11 ~3 ~7 minecraft:brown_wool
fill ~2 ~4 ~2 ~2 ~5 ~2 minecraft:stripped_spruce_wood
fill ~2 ~4 ~3 ~2 ~5 ~4 minecraft:brown_wool
fill ~2 ~4 ~5 ~2 ~5 ~5 minecraft:stripped_spruce_wood
fill ~2 ~4 ~6 ~2 ~4 ~7 minecraft:brown_terracotta
fill ~3 ~4 ~1 ~3 ~5 ~1 minecraft:brown_wool
setblock ~3 ~4 ~7 minecraft:white_terracotta
setblock ~4 ~4 ~0 minecraft:brown_wool
setblock ~4 ~4 ~7 minecraft:brown_wool
setblock ~5 ~4 ~0 minecraft:white_terracotta
fill ~7 ~4 ~0 ~7 ~5 ~0 minecraft:brown_wool
fill ~8 ~4 ~7 ~8 ~8 ~7 minecraft:brown_wool
fill ~9 ~4 ~1 ~9 ~5 ~1 minecraft:brown_terracotta
fill ~10 ~4 ~2 ~10 ~5 ~2 minecraft:stripped_spruce_wood
fill ~10 ~4 ~3 ~10 ~5 ~4 minecraft:brown_wool
fill ~10 ~4 ~5 ~10 ~5 ~5 minecraft:stripped_spruce_wood
fill ~10 ~4 ~6 ~10 ~5 ~6 minecraft:brown_wool
fill ~2 ~5 ~6 ~2 ~5 ~7 minecraft:brown_wool
fill ~3 ~5 ~7 ~3 ~7 ~7 minecraft:brown_wool
fill ~4 ~5 ~1 ~4 ~6 ~1 minecraft:brown_wool
fill ~4 ~5 ~7 ~5 ~5 ~7 minecraft:white_terracotta
fill ~5 ~5 ~0 ~6 ~5 ~0 minecraft:brown_wool
fill ~6 ~5 ~7 ~7 ~5 ~7 minecraft:brown_terracotta
setblock ~8 ~5 ~1 minecraft:packed_mud
setblock ~3 ~6 ~2 minecraft:stripped_spruce_wood
fill ~3 ~6 ~3 ~3 ~7 ~4 minecraft:brown_wool
fill ~3 ~6 ~5 ~3 ~7 ~5 minecraft:stripped_spruce_wood
fill ~3 ~6 ~6 ~3 ~7 ~6 minecraft:brown_wool
fill ~4 ~6 ~7 ~5 ~8 ~7 minecraft:brown_wool
fill ~5 ~6 ~1 ~8 ~6 ~1 minecraft:brown_wool
fill ~6 ~6 ~7 ~7 ~6 ~7 minecraft:packed_mud
setblock ~9 ~6 ~2 minecraft:stripped_spruce_wood
fill ~9 ~6 ~3 ~9 ~6 ~4 minecraft:brown_terracotta
fill ~9 ~6 ~5 ~9 ~7 ~5 minecraft:stripped_spruce_wood
fill ~9 ~6 ~6 ~9 ~7 ~7 minecraft:brown_wool
setblock ~4 ~7 ~2 minecraft:stripped_spruce_wood
fill ~5 ~7 ~1 ~7 ~7 ~1 minecraft:brown_wool
fill ~6 ~7 ~7 ~7 ~8 ~7 minecraft:brown_wool
setblock ~8 ~7 ~2 minecraft:stripped_spruce_wood
fill ~9 ~7 ~3 ~9 ~7 ~4 minecraft:brown_wool
fill ~4 ~8 ~3 ~5 ~8 ~3 minecraft:brown_wool
setblock ~4 ~8 ~4 minecraft:brown_wool
setblock ~4 ~8 ~5 minecraft:stripped_spruce_wood
fill ~4 ~8 ~6 ~5 ~8 ~6 minecraft:brown_wool
fill ~5 ~8 ~2 ~7 ~8 ~2 minecraft:stripped_spruce_wood
fill ~5 ~8 ~4 ~5 ~8 ~5 minecraft:stripped_spruce_wood
fill ~7 ~8 ~3 ~8 ~8 ~3 minecraft:brown_wool
fill ~7 ~8 ~4 ~7 ~8 ~5 minecraft:stripped_spruce_wood
fill ~7 ~8 ~6 ~8 ~8 ~6 minecraft:brown_wool
setblock ~8 ~8 ~4 minecraft:brown_wool
setblock ~8 ~8 ~5 minecraft:stripped_spruce_wood
fill ~6 ~9 ~3 ~6 ~9 ~4 minecraft:brown_wool
setblock ~6 ~9 ~5 minecraft:stripped_spruce_wood
fill ~6 ~9 ~6 ~6 ~9 ~7 minecraft:brown_wool

# the door posts and the lintel over the door (x 5..7)
fill ~4 ~1 ~7 ~4 ~3 ~7 minecraft:spruce_log
fill ~8 ~1 ~7 ~8 ~3 ~7 minecraft:spruce_log
fill ~5 ~4 ~7 ~7 ~4 ~7 minecraft:stripped_spruce_log[axis=x]

# antlers at the crown: bone tines of a great elk over the front and the back of the lodge
setblock ~5 ~8 ~1 minecraft:bone_block[axis=x]
setblock ~6 ~8 ~1 minecraft:bone_block[axis=y]
setblock ~7 ~8 ~1 minecraft:bone_block[axis=x]
setblock ~3 ~9 ~1 minecraft:bone_block[axis=x]
setblock ~4 ~9 ~1 minecraft:bone_block[axis=y]
setblock ~8 ~9 ~1 minecraft:bone_block[axis=y]
setblock ~9 ~9 ~1 minecraft:bone_block[axis=x]
setblock ~5 ~10 ~6 minecraft:bone_block[axis=x]
setblock ~6 ~10 ~6 minecraft:bone_block[axis=y]
setblock ~7 ~10 ~6 minecraft:bone_block[axis=x]
setblock ~4 ~11 ~6 minecraft:bone_block[axis=y]
setblock ~8 ~11 ~6 minecraft:bone_block[axis=y]

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

# bone tusks over the front of the ridge (a pair rising from the gable) and bone hooks beside it
setblock ~4 ~9 ~8 minecraft:bone_block[axis=x]
setblock ~8 ~9 ~8 minecraft:bone_block[axis=x]
setblock ~5 ~10 ~8 minecraft:bone_block[axis=y]
setblock ~7 ~10 ~8 minecraft:bone_block[axis=y]

# hides hang ragged from the front of the awning, between the slots
setblock ~2 ~6 ~10 minecraft:brown_terracotta
setblock ~3 ~6 ~10 minecraft:packed_mud
setblock ~5 ~6 ~10 minecraft:brown_terracotta
fill ~9 ~6 ~10 ~10 ~6 ~10 minecraft:brown_terracotta

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
setblock ~6 ~5 ~2 minecraft:lantern[hanging=true]
fill ~6 ~6 ~2 ~6 ~7 ~2 minecraft:chain[axis=y]

# bone posts at the plinth ends carry lanterns
setblock ~0 ~2 ~9 minecraft:lantern
setblock ~12 ~2 ~9 minecraft:lantern

# the banners: the House wall banner on the gable, the House standing banner on the pole
setblock ~0 ~7 ~10 minecraft:brown_banner[rotation=0]{Patterns:[{Pattern:"tts",Color:0},{Pattern:"bts",Color:0},{Pattern:"mr",Color:1}]}
setblock ~6 ~9 ~9 minecraft:brown_wall_banner[facing=south]{Patterns:[{Pattern:"tts",Color:0},{Pattern:"bts",Color:0},{Pattern:"mr",Color:1}]}
