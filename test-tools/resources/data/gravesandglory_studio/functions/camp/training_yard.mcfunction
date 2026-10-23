# size 23 8 31
# camp/training_yard: the training yard and tutorial: a fenced drill ground of coarse dirt and gravel, four lesson boards on the west side, six dummy pads, weapon racks and the Drillmaster's shade.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Lesson boards (dark oak planks, y 1..4) at x 0, z 4..6, 10..12, 16..18, 22..24, framed by dark oak posts; the lesson texts (fixed text displays facing east) stand 0.1 in front of them at x 1.1.
# Dummy pads: smooth stone at y 0, z 9, x 4..14 step 2 (the dummies are armor stands from CampWorld). The Drillmaster stands at (18.5, 1, 15.5) under the shade; his label (3.7 up) sweeps a cylinder of radius 4.1, kept clear, and the shade has no front posts, so from the east (the first arrival, the way in, the overview) nothing stands between the eye and the label.
# Open on the east (x 22): the path from the parade's cross way runs in under the shade. The north (z 0), south (z 30) and west (x 0) sides are fenced.

# the clearing: air over the whole yard (y 1..7), so no tuft or branch of the bowl stands in it
fill ~1 ~1 ~1 ~8 ~7 ~3 minecraft:air
fill ~1 ~1 ~4 ~3 ~7 ~28 minecraft:air
fill ~1 ~1 ~29 ~2 ~7 ~29 minecraft:air
fill ~4 ~1 ~7 ~4 ~7 ~9 minecraft:air
fill ~4 ~1 ~13 ~4 ~7 ~15 minecraft:air
fill ~4 ~1 ~19 ~4 ~7 ~21 minecraft:air
fill ~4 ~1 ~25 ~4 ~7 ~28 minecraft:air
fill ~5 ~1 ~4 ~8 ~7 ~28 minecraft:air
fill ~6 ~1 ~29 ~6 ~7 ~29 minecraft:air
fill ~8 ~1 ~29 ~8 ~7 ~29 minecraft:air
fill ~9 ~1 ~2 ~10 ~7 ~28 minecraft:air
fill ~11 ~1 ~1 ~15 ~7 ~10 minecraft:air
fill ~11 ~1 ~11 ~13 ~7 ~28 minecraft:air
fill ~12 ~1 ~29 ~12 ~7 ~29 minecraft:air
fill ~14 ~1 ~12 ~14 ~6 ~18 minecraft:air
fill ~14 ~1 ~20 ~14 ~7 ~28 minecraft:air
fill ~15 ~1 ~11 ~15 ~4 ~12 minecraft:air
fill ~15 ~1 ~19 ~15 ~4 ~29 minecraft:air
fill ~16 ~1 ~2 ~17 ~5 ~29 minecraft:air
fill ~18 ~1 ~1 ~18 ~5 ~29 minecraft:air
fill ~19 ~1 ~2 ~20 ~5 ~29 minecraft:air
fill ~21 ~1 ~1 ~22 ~7 ~1 minecraft:air
fill ~21 ~1 ~3 ~21 ~7 ~29 minecraft:air
fill ~22 ~1 ~2 ~22 ~7 ~29 minecraft:air
fill ~0 ~2 ~1 ~0 ~7 ~2 minecraft:air
fill ~0 ~2 ~8 ~0 ~7 ~8 minecraft:air
fill ~0 ~2 ~14 ~0 ~7 ~14 minecraft:air
fill ~0 ~2 ~20 ~0 ~7 ~20 minecraft:air
fill ~0 ~2 ~26 ~0 ~7 ~29 minecraft:air
fill ~1 ~2 ~0 ~5 ~7 ~0 minecraft:air
fill ~1 ~2 ~30 ~5 ~7 ~30 minecraft:air
fill ~4 ~2 ~4 ~4 ~7 ~6 minecraft:air
fill ~4 ~2 ~10 ~4 ~7 ~12 minecraft:air
fill ~4 ~2 ~16 ~4 ~7 ~18 minecraft:air
fill ~4 ~2 ~22 ~4 ~7 ~24 minecraft:air
fill ~7 ~2 ~0 ~11 ~7 ~0 minecraft:air
fill ~7 ~2 ~29 ~7 ~7 ~30 minecraft:air
fill ~8 ~2 ~30 ~11 ~7 ~30 minecraft:air
fill ~13 ~2 ~0 ~17 ~7 ~0 minecraft:air
fill ~13 ~2 ~29 ~14 ~7 ~30 minecraft:air
fill ~15 ~2 ~13 ~15 ~5 ~13 minecraft:air
fill ~15 ~2 ~18 ~15 ~6 ~18 minecraft:air
fill ~15 ~2 ~30 ~17 ~7 ~30 minecraft:air
fill ~17 ~2 ~1 ~17 ~7 ~1 minecraft:air
fill ~19 ~2 ~0 ~21 ~7 ~0 minecraft:air
fill ~19 ~2 ~1 ~20 ~7 ~1 minecraft:air
fill ~19 ~2 ~30 ~21 ~7 ~30 minecraft:air
fill ~0 ~3 ~0 ~0 ~7 ~0 minecraft:air
fill ~0 ~3 ~30 ~0 ~7 ~30 minecraft:air
fill ~6 ~3 ~0 ~6 ~7 ~0 minecraft:air
fill ~6 ~3 ~30 ~6 ~7 ~30 minecraft:air
fill ~9 ~3 ~1 ~10 ~7 ~1 minecraft:air
fill ~12 ~3 ~0 ~12 ~7 ~0 minecraft:air
fill ~12 ~3 ~30 ~12 ~7 ~30 minecraft:air
fill ~16 ~3 ~1 ~16 ~7 ~1 minecraft:air
fill ~18 ~3 ~0 ~18 ~7 ~0 minecraft:air
fill ~18 ~3 ~30 ~18 ~7 ~30 minecraft:air
fill ~3 ~4 ~29 ~5 ~7 ~29 minecraft:air
fill ~9 ~4 ~29 ~11 ~7 ~29 minecraft:air
fill ~15 ~4 ~14 ~15 ~5 ~17 minecraft:air
fill ~22 ~4 ~0 ~22 ~7 ~0 minecraft:air
fill ~22 ~4 ~30 ~22 ~7 ~30 minecraft:air
fill ~15 ~5 ~12 ~15 ~6 ~12 minecraft:air
fill ~15 ~5 ~20 ~15 ~7 ~29 minecraft:air
fill ~0 ~6 ~4 ~0 ~7 ~7 minecraft:air
fill ~0 ~6 ~10 ~0 ~7 ~13 minecraft:air
fill ~0 ~6 ~16 ~0 ~7 ~19 minecraft:air
fill ~0 ~6 ~22 ~0 ~7 ~24 minecraft:air
fill ~15 ~6 ~14 ~15 ~6 ~16 minecraft:air
fill ~16 ~6 ~2 ~17 ~7 ~10 minecraft:air
fill ~16 ~6 ~12 ~16 ~6 ~18 minecraft:air
fill ~16 ~6 ~20 ~20 ~7 ~29 minecraft:air
fill ~18 ~6 ~1 ~18 ~7 ~10 minecraft:air
fill ~19 ~6 ~2 ~21 ~7 ~2 minecraft:air
fill ~19 ~6 ~3 ~20 ~7 ~10 minecraft:air
setblock ~0 ~7 ~3 minecraft:air
setblock ~0 ~7 ~9 minecraft:air
setblock ~0 ~7 ~15 minecraft:air
setblock ~0 ~7 ~21 minecraft:air
setblock ~0 ~7 ~25 minecraft:air
fill ~17 ~7 ~11 ~20 ~7 ~19 minecraft:air

# the drill ground: coarse dirt with patches of gravel and rooted dirt, packed mud where it is worn; on the open east side the lawn comes in in tongues
fill ~0 ~0 ~0 ~0 ~0 ~2 minecraft:coarse_dirt
setblock ~0 ~0 ~3 minecraft:rooted_dirt
setblock ~0 ~0 ~4 minecraft:coarse_dirt
fill ~0 ~0 ~5 ~0 ~0 ~8 minecraft:packed_mud
setblock ~0 ~0 ~9 minecraft:rooted_dirt
fill ~0 ~0 ~10 ~2 ~0 ~10 minecraft:coarse_dirt
fill ~0 ~0 ~11 ~1 ~0 ~12 minecraft:rooted_dirt
setblock ~0 ~0 ~13 minecraft:coarse_dirt
fill ~0 ~0 ~14 ~0 ~0 ~16 minecraft:gravel
fill ~0 ~0 ~17 ~0 ~0 ~22 minecraft:coarse_dirt
fill ~0 ~0 ~23 ~2 ~0 ~23 minecraft:rooted_dirt
fill ~0 ~0 ~24 ~0 ~0 ~30 minecraft:coarse_dirt
fill ~1 ~0 ~0 ~2 ~0 ~0 minecraft:packed_mud
setblock ~1 ~0 ~1 minecraft:coarse_dirt
fill ~1 ~0 ~2 ~1 ~0 ~6 minecraft:packed_mud
fill ~1 ~0 ~7 ~1 ~0 ~8 minecraft:grass_block
setblock ~1 ~0 ~9 minecraft:packed_mud
setblock ~1 ~0 ~13 minecraft:rooted_dirt
setblock ~1 ~0 ~14 minecraft:packed_mud
fill ~1 ~0 ~15 ~1 ~0 ~16 minecraft:grass_block
fill ~1 ~0 ~17 ~1 ~0 ~21 minecraft:packed_mud
setblock ~1 ~0 ~22 minecraft:coarse_dirt
fill ~1 ~0 ~24 ~2 ~0 ~24 minecraft:rooted_dirt
fill ~1 ~0 ~25 ~2 ~0 ~25 minecraft:packed_mud
setblock ~1 ~0 ~26 minecraft:grass_block
fill ~1 ~0 ~27 ~1 ~0 ~30 minecraft:coarse_dirt
fill ~2 ~0 ~1 ~2 ~0 ~9 minecraft:packed_mud
fill ~2 ~0 ~11 ~2 ~0 ~14 minecraft:coarse_dirt
fill ~2 ~0 ~15 ~3 ~0 ~17 minecraft:packed_mud
setblock ~2 ~0 ~18 minecraft:rooted_dirt
fill ~2 ~0 ~19 ~2 ~0 ~22 minecraft:packed_mud
setblock ~2 ~0 ~26 minecraft:packed_mud
fill ~2 ~0 ~27 ~3 ~0 ~27 minecraft:gravel
setblock ~2 ~0 ~28 minecraft:packed_mud
fill ~2 ~0 ~29 ~4 ~0 ~30 minecraft:coarse_dirt
fill ~3 ~0 ~0 ~5 ~0 ~0 minecraft:rooted_dirt
fill ~3 ~0 ~1 ~5 ~0 ~1 minecraft:coarse_dirt
fill ~3 ~0 ~2 ~3 ~0 ~5 minecraft:packed_mud
setblock ~3 ~0 ~6 minecraft:coarse_dirt
fill ~3 ~0 ~7 ~3 ~0 ~8 minecraft:packed_mud
fill ~3 ~0 ~9 ~3 ~0 ~14 minecraft:coarse_dirt
fill ~3 ~0 ~18 ~3 ~0 ~26 minecraft:packed_mud
setblock ~3 ~0 ~28 minecraft:gravel
fill ~4 ~0 ~2 ~4 ~0 ~8 minecraft:coarse_dirt
fill ~4 ~0 ~10 ~4 ~0 ~15 minecraft:coarse_dirt
setblock ~4 ~0 ~16 minecraft:rooted_dirt
fill ~4 ~0 ~17 ~4 ~0 ~23 minecraft:coarse_dirt
fill ~4 ~0 ~24 ~4 ~0 ~25 minecraft:gravel
fill ~4 ~0 ~26 ~4 ~0 ~28 minecraft:rooted_dirt
fill ~5 ~0 ~2 ~6 ~0 ~4 minecraft:packed_mud
fill ~5 ~0 ~5 ~5 ~0 ~7 minecraft:coarse_dirt
fill ~5 ~0 ~8 ~7 ~0 ~8 minecraft:packed_mud
fill ~5 ~0 ~9 ~5 ~0 ~27 minecraft:coarse_dirt
fill ~5 ~0 ~28 ~6 ~0 ~28 minecraft:packed_mud
setblock ~5 ~0 ~29 minecraft:packed_mud
fill ~5 ~0 ~30 ~6 ~0 ~30 minecraft:gravel
setblock ~6 ~0 ~0 minecraft:gravel
fill ~6 ~0 ~1 ~8 ~0 ~1 minecraft:grass_block
fill ~6 ~0 ~5 ~9 ~0 ~6 minecraft:packed_mud
fill ~6 ~0 ~7 ~8 ~0 ~7 minecraft:packed_mud
fill ~6 ~0 ~10 ~6 ~0 ~12 minecraft:coarse_dirt
setblock ~6 ~0 ~13 minecraft:packed_mud
fill ~6 ~0 ~14 ~6 ~0 ~21 minecraft:coarse_dirt
fill ~6 ~0 ~22 ~6 ~0 ~24 minecraft:rooted_dirt
fill ~6 ~0 ~25 ~6 ~0 ~27 minecraft:coarse_dirt
fill ~6 ~0 ~29 ~8 ~0 ~29 minecraft:grass_block
fill ~7 ~0 ~0 ~14 ~0 ~0 minecraft:coarse_dirt
fill ~7 ~0 ~2 ~8 ~0 ~2 minecraft:coarse_dirt
setblock ~7 ~0 ~3 minecraft:packed_mud
setblock ~7 ~0 ~4 minecraft:coarse_dirt
fill ~7 ~0 ~9 ~7 ~0 ~28 minecraft:coarse_dirt
fill ~7 ~0 ~30 ~8 ~0 ~30 minecraft:coarse_dirt
fill ~8 ~0 ~3 ~9 ~0 ~4 minecraft:coarse_dirt
setblock ~8 ~0 ~8 minecraft:coarse_dirt
fill ~8 ~0 ~10 ~10 ~0 ~11 minecraft:coarse_dirt
fill ~8 ~0 ~12 ~9 ~0 ~16 minecraft:coarse_dirt
fill ~8 ~0 ~17 ~8 ~0 ~18 minecraft:rooted_dirt
fill ~8 ~0 ~19 ~8 ~0 ~28 minecraft:coarse_dirt
fill ~9 ~0 ~1 ~11 ~0 ~2 minecraft:coarse_dirt
fill ~9 ~0 ~7 ~9 ~0 ~8 minecraft:coarse_dirt
setblock ~9 ~0 ~9 minecraft:rooted_dirt
fill ~9 ~0 ~17 ~10 ~0 ~18 minecraft:gravel
setblock ~9 ~0 ~19 minecraft:gravel
fill ~9 ~0 ~20 ~9 ~0 ~30 minecraft:coarse_dirt
fill ~10 ~0 ~3 ~10 ~0 ~5 minecraft:packed_mud
fill ~10 ~0 ~6 ~10 ~0 ~8 minecraft:coarse_dirt
setblock ~10 ~0 ~12 minecraft:packed_mud
fill ~10 ~0 ~13 ~10 ~0 ~16 minecraft:coarse_dirt
fill ~10 ~0 ~19 ~10 ~0 ~24 minecraft:coarse_dirt
setblock ~10 ~0 ~25 minecraft:rooted_dirt
fill ~10 ~0 ~26 ~10 ~0 ~30 minecraft:coarse_dirt
setblock ~11 ~0 ~3 minecraft:gravel
setblock ~11 ~0 ~4 minecraft:packed_mud
fill ~11 ~0 ~5 ~11 ~0 ~7 minecraft:coarse_dirt
setblock ~11 ~0 ~8 minecraft:packed_mud
fill ~11 ~0 ~9 ~11 ~0 ~13 minecraft:coarse_dirt
fill ~11 ~0 ~14 ~12 ~0 ~14 minecraft:packed_mud
fill ~11 ~0 ~15 ~12 ~0 ~27 minecraft:coarse_dirt
fill ~11 ~0 ~28 ~11 ~0 ~30 minecraft:coarse_dirt
fill ~12 ~0 ~1 ~13 ~0 ~1 minecraft:grass_block
fill ~12 ~0 ~2 ~12 ~0 ~3 minecraft:gravel
fill ~12 ~0 ~4 ~13 ~0 ~6 minecraft:coarse_dirt
fill ~12 ~0 ~7 ~12 ~0 ~8 minecraft:packed_mud
fill ~12 ~0 ~10 ~12 ~0 ~13 minecraft:coarse_dirt
fill ~12 ~0 ~28 ~12 ~0 ~30 minecraft:gravel
setblock ~13 ~0 ~2 minecraft:coarse_dirt
setblock ~13 ~0 ~3 minecraft:packed_mud
setblock ~13 ~0 ~7 minecraft:coarse_dirt
fill ~13 ~0 ~8 ~13 ~0 ~11 minecraft:packed_mud
setblock ~13 ~0 ~12 minecraft:coarse_dirt
fill ~13 ~0 ~13 ~13 ~0 ~18 minecraft:packed_mud
fill ~13 ~0 ~19 ~13 ~0 ~22 minecraft:coarse_dirt
fill ~13 ~0 ~23 ~14 ~0 ~23 minecraft:rooted_dirt
fill ~13 ~0 ~24 ~13 ~0 ~28 minecraft:coarse_dirt
fill ~13 ~0 ~29 ~13 ~0 ~30 minecraft:gravel
fill ~14 ~0 ~1 ~15 ~0 ~5 minecraft:coarse_dirt
fill ~14 ~0 ~6 ~14 ~0 ~8 minecraft:coarse_dirt
fill ~14 ~0 ~10 ~14 ~0 ~13 minecraft:packed_mud
setblock ~14 ~0 ~14 minecraft:coarse_dirt
fill ~14 ~0 ~15 ~14 ~0 ~17 minecraft:packed_mud
fill ~14 ~0 ~18 ~14 ~0 ~22 minecraft:coarse_dirt
fill ~14 ~0 ~24 ~14 ~0 ~26 minecraft:rooted_dirt
fill ~14 ~0 ~27 ~14 ~0 ~29 minecraft:coarse_dirt
setblock ~14 ~0 ~30 minecraft:gravel
setblock ~15 ~0 ~0 minecraft:gravel
setblock ~15 ~0 ~6 minecraft:rooted_dirt
setblock ~15 ~0 ~7 minecraft:gravel
fill ~15 ~0 ~8 ~15 ~0 ~11 minecraft:packed_mud
fill ~15 ~0 ~19 ~18 ~0 ~19 minecraft:coarse_dirt
fill ~15 ~0 ~20 ~16 ~0 ~25 minecraft:coarse_dirt
fill ~15 ~0 ~26 ~15 ~0 ~28 minecraft:rooted_dirt
fill ~15 ~0 ~29 ~16 ~0 ~30 minecraft:coarse_dirt
fill ~16 ~0 ~0 ~22 ~0 ~1 minecraft:coarse_dirt
fill ~16 ~0 ~2 ~16 ~0 ~10 minecraft:coarse_dirt
setblock ~16 ~0 ~11 minecraft:packed_mud
setblock ~16 ~0 ~26 minecraft:coarse_dirt
fill ~16 ~0 ~27 ~19 ~0 ~28 minecraft:rooted_dirt
fill ~17 ~0 ~2 ~17 ~0 ~3 minecraft:packed_mud
fill ~17 ~0 ~4 ~17 ~0 ~11 minecraft:coarse_dirt
fill ~17 ~0 ~20 ~18 ~0 ~20 minecraft:packed_mud
fill ~17 ~0 ~21 ~19 ~0 ~21 minecraft:rooted_dirt
fill ~17 ~0 ~22 ~17 ~0 ~26 minecraft:coarse_dirt
fill ~17 ~0 ~29 ~19 ~0 ~29 minecraft:rooted_dirt
fill ~17 ~0 ~30 ~18 ~0 ~30 minecraft:coarse_dirt
fill ~18 ~0 ~2 ~22 ~0 ~2 minecraft:coarse_dirt
fill ~18 ~0 ~3 ~19 ~0 ~4 minecraft:packed_mud
fill ~18 ~0 ~5 ~18 ~0 ~7 minecraft:coarse_dirt
setblock ~18 ~0 ~8 minecraft:rooted_dirt
fill ~18 ~0 ~9 ~18 ~0 ~11 minecraft:coarse_dirt
setblock ~18 ~0 ~22 minecraft:rooted_dirt
fill ~18 ~0 ~23 ~18 ~0 ~26 minecraft:coarse_dirt
setblock ~19 ~0 ~5 minecraft:packed_mud
fill ~19 ~0 ~6 ~19 ~0 ~11 minecraft:coarse_dirt
fill ~19 ~0 ~19 ~20 ~0 ~20 minecraft:packed_mud
fill ~19 ~0 ~22 ~19 ~0 ~26 minecraft:coarse_dirt
setblock ~19 ~0 ~30 minecraft:rooted_dirt
fill ~20 ~0 ~3 ~21 ~0 ~3 minecraft:coarse_dirt
fill ~20 ~0 ~4 ~21 ~0 ~4 minecraft:packed_mud
fill ~20 ~0 ~5 ~20 ~0 ~7 minecraft:coarse_dirt
fill ~20 ~0 ~8 ~20 ~0 ~10 minecraft:rooted_dirt
setblock ~20 ~0 ~11 minecraft:coarse_dirt
fill ~20 ~0 ~21 ~21 ~0 ~22 minecraft:coarse_dirt
fill ~20 ~0 ~23 ~20 ~0 ~25 minecraft:coarse_dirt
setblock ~20 ~0 ~26 minecraft:rooted_dirt
setblock ~20 ~0 ~30 minecraft:coarse_dirt
setblock ~21 ~0 ~5 minecraft:packed_mud
setblock ~21 ~0 ~6 minecraft:gravel
fill ~21 ~0 ~7 ~21 ~0 ~10 minecraft:rooted_dirt
setblock ~21 ~0 ~30 minecraft:gravel
fill ~22 ~0 ~3 ~22 ~0 ~5 minecraft:packed_mud
fill ~22 ~0 ~6 ~22 ~0 ~7 minecraft:coarse_dirt
setblock ~22 ~0 ~30 minecraft:coarse_dirt

# the way in from the parade's cross way and the floor of the Drillmaster's shade: stone bricks, worn and mossy
setblock ~15 ~0 ~12 minecraft:stone_bricks
setblock ~15 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~14 minecraft:stone_bricks
setblock ~15 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~16 minecraft:stone_bricks
setblock ~15 ~0 ~17 minecraft:cobblestone
setblock ~15 ~0 ~18 minecraft:stone_bricks
setblock ~16 ~0 ~12 minecraft:mossy_stone_bricks
setblock ~16 ~0 ~13 minecraft:stone_bricks
setblock ~16 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~16 ~0 ~15 minecraft:stone_bricks
setblock ~16 ~0 ~16 minecraft:mossy_stone_bricks
fill ~16 ~0 ~17 ~16 ~0 ~18 minecraft:stone_bricks
setblock ~17 ~0 ~12 minecraft:cobblestone
setblock ~17 ~0 ~13 minecraft:cracked_stone_bricks
fill ~17 ~0 ~14 ~18 ~0 ~15 minecraft:stone_bricks
setblock ~17 ~0 ~16 minecraft:cracked_stone_bricks
fill ~17 ~0 ~17 ~17 ~0 ~18 minecraft:mossy_stone_bricks
fill ~18 ~0 ~12 ~18 ~0 ~13 minecraft:cracked_stone_bricks
fill ~18 ~0 ~16 ~18 ~0 ~18 minecraft:stone_bricks
fill ~19 ~0 ~12 ~19 ~0 ~18 minecraft:stone_bricks
setblock ~20 ~0 ~12 minecraft:cracked_stone_bricks
fill ~20 ~0 ~13 ~20 ~0 ~14 minecraft:stone_bricks
fill ~20 ~0 ~15 ~20 ~0 ~16 minecraft:mossy_stone_bricks
fill ~20 ~0 ~17 ~21 ~0 ~17 minecraft:stone_bricks
setblock ~20 ~0 ~18 minecraft:stone_bricks
fill ~21 ~0 ~14 ~21 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~21 ~0 ~16 minecraft:cobblestone
fill ~22 ~0 ~14 ~22 ~0 ~17 minecraft:stone_bricks

# the dummy pads: smooth stone, flush, along z 9 at x 4..14
setblock ~4 ~0 ~9 minecraft:smooth_stone
setblock ~6 ~0 ~9 minecraft:smooth_stone
setblock ~8 ~0 ~9 minecraft:smooth_stone
setblock ~10 ~0 ~9 minecraft:smooth_stone
setblock ~12 ~0 ~9 minecraft:smooth_stone
setblock ~14 ~0 ~9 minecraft:smooth_stone

# the fence posts: dark oak logs at the corners and every six blocks along the north and south sides
fill ~0 ~1 ~0 ~0 ~2 ~0 minecraft:dark_oak_log
fill ~0 ~1 ~30 ~0 ~2 ~30 minecraft:dark_oak_log
fill ~6 ~1 ~0 ~6 ~2 ~0 minecraft:dark_oak_log
fill ~6 ~1 ~30 ~6 ~2 ~30 minecraft:dark_oak_log
fill ~12 ~1 ~0 ~12 ~2 ~0 minecraft:dark_oak_log
fill ~12 ~1 ~30 ~12 ~2 ~30 minecraft:dark_oak_log
fill ~18 ~1 ~0 ~18 ~2 ~0 minecraft:dark_oak_log
fill ~18 ~1 ~30 ~18 ~2 ~30 minecraft:dark_oak_log
fill ~22 ~1 ~0 ~22 ~2 ~0 minecraft:dark_oak_log
fill ~22 ~1 ~30 ~22 ~2 ~30 minecraft:dark_oak_log

# the notice wall on the west side: dark oak log posts y 1..5 framing the four boards
fill ~0 ~1 ~3 ~0 ~5 ~3 minecraft:dark_oak_log
fill ~0 ~1 ~7 ~0 ~5 ~7 minecraft:dark_oak_log
fill ~0 ~1 ~9 ~0 ~5 ~9 minecraft:dark_oak_log
fill ~0 ~1 ~13 ~0 ~5 ~13 minecraft:dark_oak_log
fill ~0 ~1 ~15 ~0 ~5 ~15 minecraft:dark_oak_log
fill ~0 ~1 ~19 ~0 ~5 ~19 minecraft:dark_oak_log
fill ~0 ~1 ~21 ~0 ~5 ~21 minecraft:dark_oak_log
fill ~0 ~1 ~25 ~0 ~5 ~25 minecraft:dark_oak_log

# the four lesson backboards: dark oak planks, y 1..4
fill ~0 ~1 ~4 ~0 ~4 ~6 minecraft:dark_oak_planks
fill ~0 ~1 ~10 ~0 ~4 ~12 minecraft:dark_oak_planks
fill ~0 ~1 ~16 ~0 ~4 ~18 minecraft:dark_oak_planks
fill ~0 ~1 ~22 ~0 ~4 ~24 minecraft:dark_oak_planks

# the board caps: dark oak slabs over each board
fill ~0 ~5 ~4 ~0 ~5 ~6 minecraft:dark_oak_slab[type=bottom]
fill ~0 ~5 ~10 ~0 ~5 ~12 minecraft:dark_oak_slab[type=bottom]
fill ~0 ~5 ~16 ~0 ~5 ~18 minecraft:dark_oak_slab[type=bottom]
fill ~0 ~5 ~22 ~0 ~5 ~24 minecraft:dark_oak_slab[type=bottom]

# the fence rails: dark oak fence between the posts (it joins the posts and the boards)
fill ~0 ~1 ~1 ~0 ~1 ~2 minecraft:dark_oak_fence
setblock ~0 ~1 ~8 minecraft:dark_oak_fence
setblock ~0 ~1 ~14 minecraft:dark_oak_fence
setblock ~0 ~1 ~20 minecraft:dark_oak_fence
fill ~0 ~1 ~26 ~0 ~1 ~29 minecraft:dark_oak_fence
fill ~1 ~1 ~0 ~5 ~1 ~0 minecraft:dark_oak_fence
fill ~1 ~1 ~30 ~5 ~1 ~30 minecraft:dark_oak_fence
fill ~7 ~1 ~0 ~11 ~1 ~0 minecraft:dark_oak_fence
fill ~7 ~1 ~30 ~11 ~1 ~30 minecraft:dark_oak_fence
fill ~13 ~1 ~0 ~17 ~1 ~0 minecraft:dark_oak_fence
fill ~13 ~1 ~30 ~17 ~1 ~30 minecraft:dark_oak_fence
fill ~19 ~1 ~0 ~21 ~1 ~0 minecraft:dark_oak_fence
fill ~19 ~1 ~30 ~21 ~1 ~30 minecraft:dark_oak_fence

# the Drillmaster's shade, a lean-to: two dark oak log posts y 1..6 at its back (x 14, z 11 and 19)
fill ~14 ~1 ~11 ~14 ~6 ~11 minecraft:dark_oak_log
fill ~14 ~1 ~19 ~14 ~6 ~19 minecraft:dark_oak_log

# the shade frame: the back beam on the posts at y 7, rafters at y 6 along both sides out to the eave at x 20
fill ~15 ~6 ~11 ~20 ~6 ~11 minecraft:dark_oak_log[axis=x]
fill ~15 ~6 ~19 ~20 ~6 ~19 minecraft:dark_oak_log[axis=x]
fill ~14 ~7 ~11 ~14 ~7 ~19 minecraft:dark_oak_log[axis=z]

# the brackets under the rafters at the posts: upturned dark oak stairs
setblock ~15 ~5 ~11 minecraft:dark_oak_stairs[facing=west,half=top]
setblock ~15 ~5 ~19 minecraft:dark_oak_stairs[facing=west,half=top]

# the shade roof, falling to the east: spruce planks at y 7 by the back beam, at y 6 to the eave, a slab lip at the eave
fill ~17 ~6 ~12 ~19 ~6 ~18 minecraft:spruce_planks
fill ~20 ~6 ~12 ~20 ~6 ~18 minecraft:spruce_slab[type=top]
fill ~15 ~7 ~11 ~16 ~7 ~11 minecraft:spruce_slab[type=bottom]
fill ~15 ~7 ~12 ~16 ~7 ~18 minecraft:spruce_planks
fill ~15 ~7 ~19 ~16 ~7 ~19 minecraft:spruce_slab[type=bottom]

# the rack of practice spears behind the Drillmaster: dark oak fence staves under a slab shelf, a smithing table and a grindstone at its ends
setblock ~15 ~1 ~13 minecraft:smithing_table
fill ~15 ~1 ~14 ~15 ~2 ~17 minecraft:dark_oak_fence
setblock ~15 ~1 ~18 minecraft:grindstone[face=floor,facing=east]
fill ~15 ~3 ~14 ~15 ~3 ~17 minecraft:dark_oak_slab[type=bottom]

# the armoury corner by the north fence: barrels, an anvil and a chest
fill ~16 ~1 ~1 ~17 ~1 ~1 minecraft:barrel[facing=up]
setblock ~19 ~1 ~1 minecraft:anvil[facing=west]
setblock ~20 ~1 ~1 minecraft:chest[facing=south]
setblock ~16 ~2 ~1 minecraft:barrel[facing=up]

# the archery butts on the north side: hay bales, one stacked, and a target
fill ~9 ~1 ~1 ~10 ~1 ~1 minecraft:hay_block[axis=x]
setblock ~10 ~2 ~1 minecraft:hay_block[axis=x]

# the weapon racks by the south fence: dark oak fence staves under slab shelves, with barrels between
fill ~3 ~1 ~29 ~5 ~2 ~29 minecraft:dark_oak_fence
setblock ~7 ~1 ~29 minecraft:barrel[facing=up]
fill ~9 ~1 ~29 ~11 ~2 ~29 minecraft:dark_oak_fence
setblock ~13 ~1 ~29 minecraft:barrel[facing=up]
setblock ~14 ~1 ~29 minecraft:hay_block[axis=x]
fill ~3 ~3 ~29 ~5 ~3 ~29 minecraft:dark_oak_slab[type=bottom]
fill ~9 ~3 ~29 ~11 ~3 ~29 minecraft:dark_oak_slab[type=bottom]

# benches before the boards: spruce stairs, their backs to the east
fill ~4 ~1 ~4 ~4 ~1 ~6 minecraft:spruce_stairs[facing=east]
fill ~4 ~1 ~10 ~4 ~1 ~12 minecraft:spruce_stairs[facing=east]
fill ~4 ~1 ~16 ~4 ~1 ~18 minecraft:spruce_stairs[facing=east]
fill ~4 ~1 ~22 ~4 ~1 ~24 minecraft:spruce_stairs[facing=east]

# the target on the stacked bale
setblock ~9 ~2 ~1 minecraft:target

# the yard standard: a dark oak fence pole in the north-east corner
fill ~21 ~1 ~2 ~21 ~4 ~2 minecraft:dark_oak_fence

# lanterns: on the posts of the notice wall and the two east corner posts, and two hung under the high back of the shade
setblock ~22 ~3 ~0 minecraft:lantern
setblock ~22 ~3 ~30 minecraft:lantern
setblock ~0 ~6 ~3 minecraft:lantern
setblock ~0 ~6 ~9 minecraft:lantern
setblock ~0 ~6 ~15 minecraft:lantern
setblock ~0 ~6 ~21 minecraft:lantern
setblock ~0 ~6 ~25 minecraft:lantern
setblock ~15 ~6 ~13 minecraft:lantern[hanging=true]
setblock ~15 ~6 ~17 minecraft:lantern[hanging=true]

# banners: the camp standard on the yard pole
setblock ~21 ~5 ~2 minecraft:black_banner[rotation=8]{Patterns:[{Pattern:"bo",Color:4},{Pattern:"mc",Color:4}]}
