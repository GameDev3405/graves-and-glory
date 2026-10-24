# size 19 6 17
# hollowwood/logging_camp: logging camp of Hollowwood: a low log hut, the old pile of logs in two stacks, sawhorses and chopping blocks, a fire pit with log seats, a tool lean-to and a jib hoist round a trodden clearing where the old farm lane ends.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Lane (width 3): enters at the east edge near z 11 and ends inside near (10..11, 8); the strip within 1.5 of that line stays open at y 1..2. Hut x 2..8, z 1..5; stacks north-east x 10..16, z 0..4 and south x 9..15, z 12..15; lean-to x 0..2, z 9..14.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~9 ~1 ~0 minecraft:air
fill ~0 ~1 ~1 ~1 ~2 ~4 minecraft:air
fill ~0 ~1 ~5 ~0 ~5 ~8 minecraft:air
fill ~1 ~1 ~6 ~5 ~1 ~6 minecraft:air
fill ~1 ~1 ~8 ~1 ~3 ~9 minecraft:air
fill ~1 ~1 ~14 ~1 ~3 ~14 minecraft:air
fill ~2 ~1 ~7 ~2 ~5 ~8 minecraft:air
fill ~2 ~1 ~10 ~2 ~2 ~13 minecraft:air
fill ~3 ~1 ~3 ~7 ~2 ~3 minecraft:air
fill ~3 ~1 ~8 ~3 ~5 ~10 minecraft:air
fill ~3 ~1 ~12 ~3 ~5 ~12 minecraft:air
fill ~4 ~1 ~4 ~6 ~3 ~4 minecraft:air
fill ~4 ~1 ~7 ~6 ~5 ~7 minecraft:air
fill ~4 ~1 ~10 ~4 ~5 ~12 minecraft:air
fill ~4 ~1 ~16 ~4 ~5 ~16 minecraft:air
fill ~5 ~1 ~8 ~8 ~5 ~8 minecraft:air
fill ~5 ~1 ~9 ~5 ~5 ~9 minecraft:air
fill ~5 ~1 ~11 ~5 ~3 ~16 minecraft:air
fill ~6 ~1 ~12 ~6 ~4 ~16 minecraft:air
fill ~7 ~1 ~6 ~10 ~1 ~7 minecraft:air
fill ~7 ~1 ~13 ~8 ~1 ~13 minecraft:air
fill ~7 ~1 ~14 ~7 ~5 ~16 minecraft:air
fill ~8 ~1 ~16 ~15 ~5 ~16 minecraft:air
fill ~9 ~1 ~3 ~9 ~3 ~4 minecraft:air
fill ~9 ~1 ~9 ~9 ~5 ~9 minecraft:air
fill ~9 ~1 ~11 ~9 ~5 ~11 minecraft:air
fill ~10 ~1 ~8 ~10 ~5 ~10 minecraft:air
fill ~11 ~1 ~7 ~11 ~5 ~10 minecraft:air
fill ~12 ~1 ~5 ~17 ~1 ~5 minecraft:air
fill ~12 ~1 ~6 ~12 ~1 ~11 minecraft:air
fill ~13 ~1 ~7 ~13 ~5 ~11 minecraft:air
fill ~14 ~1 ~6 ~14 ~5 ~11 minecraft:air
fill ~15 ~1 ~7 ~15 ~5 ~11 minecraft:air
fill ~16 ~1 ~6 ~16 ~1 ~16 minecraft:air
fill ~17 ~1 ~2 ~17 ~5 ~3 minecraft:air
fill ~17 ~1 ~7 ~17 ~5 ~7 minecraft:air
fill ~17 ~1 ~9 ~17 ~5 ~16 minecraft:air
fill ~18 ~1 ~0 ~18 ~5 ~16 minecraft:air
fill ~0 ~2 ~0 ~0 ~5 ~0 minecraft:air
fill ~0 ~2 ~15 ~0 ~5 ~16 minecraft:air
setblock ~1 ~2 ~5 minecraft:air
fill ~1 ~2 ~7 ~1 ~5 ~7 minecraft:air
fill ~1 ~2 ~10 ~1 ~3 ~10 minecraft:air
fill ~1 ~2 ~12 ~1 ~3 ~13 minecraft:air
fill ~3 ~2 ~4 ~3 ~3 ~4 minecraft:air
fill ~3 ~2 ~7 ~3 ~5 ~7 minecraft:air
fill ~3 ~2 ~11 ~3 ~5 ~11 minecraft:air
fill ~3 ~2 ~13 ~3 ~5 ~13 minecraft:air
fill ~3 ~2 ~15 ~3 ~5 ~16 minecraft:air
fill ~4 ~2 ~8 ~4 ~5 ~8 minecraft:air
fill ~4 ~2 ~14 ~4 ~5 ~15 minecraft:air
fill ~5 ~2 ~2 ~6 ~3 ~2 minecraft:air
fill ~5 ~2 ~10 ~5 ~5 ~10 minecraft:air
fill ~6 ~2 ~9 ~7 ~5 ~10 minecraft:air
fill ~7 ~2 ~4 ~7 ~3 ~4 minecraft:air
fill ~7 ~2 ~7 ~9 ~5 ~7 minecraft:air
fill ~7 ~2 ~11 ~7 ~5 ~12 minecraft:air
fill ~8 ~2 ~10 ~8 ~5 ~11 minecraft:air
fill ~8 ~2 ~13 ~8 ~5 ~14 minecraft:air
fill ~9 ~2 ~1 ~9 ~2 ~2 minecraft:air
fill ~9 ~2 ~5 ~16 ~2 ~5 minecraft:air
fill ~9 ~2 ~8 ~9 ~5 ~8 minecraft:air
fill ~10 ~2 ~6 ~10 ~5 ~7 minecraft:air
fill ~10 ~2 ~11 ~10 ~5 ~11 minecraft:air
fill ~12 ~2 ~7 ~12 ~5 ~11 minecraft:air
fill ~16 ~2 ~7 ~16 ~5 ~16 minecraft:air
fill ~17 ~2 ~0 ~17 ~5 ~1 minecraft:air
fill ~17 ~2 ~4 ~17 ~5 ~4 minecraft:air
fill ~17 ~2 ~8 ~17 ~5 ~8 minecraft:air
fill ~0 ~3 ~1 ~0 ~5 ~4 minecraft:air
fill ~1 ~3 ~0 ~10 ~5 ~0 minecraft:air
fill ~1 ~3 ~2 ~1 ~3 ~4 minecraft:air
fill ~1 ~3 ~6 ~9 ~5 ~6 minecraft:air
setblock ~1 ~3 ~11 minecraft:air
fill ~1 ~3 ~15 ~2 ~5 ~16 minecraft:air
fill ~3 ~3 ~2 ~4 ~3 ~3 minecraft:air
fill ~3 ~3 ~14 ~3 ~5 ~14 minecraft:air
fill ~4 ~3 ~9 ~4 ~5 ~9 minecraft:air
fill ~6 ~3 ~3 ~6 ~4 ~3 minecraft:air
fill ~6 ~3 ~11 ~6 ~5 ~11 minecraft:air
fill ~7 ~3 ~2 ~7 ~3 ~3 minecraft:air
fill ~8 ~3 ~9 ~8 ~5 ~9 minecraft:air
setblock ~9 ~3 ~2 minecraft:air
fill ~9 ~3 ~12 ~10 ~5 ~12 minecraft:air
fill ~9 ~3 ~13 ~9 ~5 ~15 minecraft:air
fill ~10 ~3 ~1 ~10 ~5 ~5 minecraft:air
fill ~11 ~3 ~5 ~15 ~5 ~5 minecraft:air
fill ~11 ~3 ~11 ~11 ~5 ~12 minecraft:air
fill ~12 ~3 ~12 ~12 ~5 ~13 minecraft:air
fill ~13 ~3 ~14 ~13 ~5 ~14 minecraft:air
fill ~14 ~3 ~12 ~15 ~5 ~12 minecraft:air
fill ~15 ~3 ~6 ~15 ~5 ~6 minecraft:air
fill ~15 ~3 ~13 ~15 ~5 ~15 minecraft:air
fill ~16 ~3 ~0 ~16 ~5 ~0 minecraft:air
fill ~16 ~3 ~2 ~16 ~5 ~6 minecraft:air
fill ~17 ~3 ~5 ~17 ~5 ~5 minecraft:air
fill ~1 ~4 ~1 ~9 ~5 ~1 minecraft:air
setblock ~1 ~4 ~3 minecraft:air
fill ~1 ~4 ~5 ~9 ~5 ~5 minecraft:air
fill ~1 ~4 ~8 ~1 ~5 ~8 minecraft:air
fill ~2 ~4 ~9 ~2 ~5 ~14 minecraft:air
fill ~3 ~4 ~3 ~4 ~4 ~3 minecraft:air
fill ~5 ~4 ~11 ~5 ~5 ~12 minecraft:air
fill ~5 ~4 ~14 ~5 ~5 ~16 minecraft:air
setblock ~7 ~4 ~3 minecraft:air
fill ~8 ~4 ~12 ~8 ~5 ~12 minecraft:air
fill ~8 ~4 ~15 ~8 ~5 ~15 minecraft:air
setblock ~9 ~4 ~3 minecraft:air
fill ~10 ~4 ~13 ~11 ~5 ~13 minecraft:air
fill ~10 ~4 ~14 ~10 ~5 ~15 minecraft:air
fill ~11 ~4 ~0 ~15 ~5 ~0 minecraft:air
fill ~11 ~4 ~1 ~11 ~5 ~4 minecraft:air
fill ~11 ~4 ~6 ~11 ~5 ~6 minecraft:air
fill ~12 ~4 ~4 ~14 ~5 ~4 minecraft:air
fill ~13 ~4 ~6 ~13 ~5 ~6 minecraft:air
fill ~13 ~4 ~12 ~13 ~5 ~13 minecraft:air
fill ~14 ~4 ~13 ~14 ~5 ~15 minecraft:air
fill ~15 ~4 ~2 ~15 ~5 ~4 minecraft:air
fill ~16 ~4 ~1 ~16 ~5 ~1 minecraft:air
fill ~17 ~4 ~6 ~17 ~5 ~6 minecraft:air
fill ~1 ~5 ~2 ~9 ~5 ~2 minecraft:air
fill ~1 ~5 ~4 ~9 ~5 ~4 minecraft:air
fill ~1 ~5 ~9 ~1 ~5 ~14 minecraft:air
setblock ~6 ~5 ~12 minecraft:air
fill ~6 ~5 ~14 ~6 ~5 ~16 minecraft:air
setblock ~9 ~5 ~10 minecraft:air
fill ~11 ~5 ~14 ~11 ~5 ~15 minecraft:air
setblock ~12 ~5 ~1 minecraft:air
setblock ~12 ~5 ~3 minecraft:air
setblock ~12 ~5 ~6 minecraft:air
setblock ~13 ~5 ~15 minecraft:air
fill ~14 ~5 ~1 ~15 ~5 ~1 minecraft:air
setblock ~14 ~5 ~3 minecraft:air

# the clearing: trodden earth, bark mulch and sawdust (coarse dirt, podzol, packed mud, trodden path), the worn way of the lane from the east edge to the fire pit
setblock ~0 ~0 ~4 minecraft:coarse_dirt
setblock ~0 ~0 ~5 minecraft:podzol
fill ~0 ~0 ~6 ~0 ~0 ~7 minecraft:coarse_dirt
setblock ~0 ~0 ~8 minecraft:packed_mud
fill ~0 ~0 ~9 ~0 ~0 ~10 minecraft:podzol
setblock ~0 ~0 ~11 minecraft:coarse_dirt
setblock ~0 ~0 ~12 minecraft:podzol
setblock ~1 ~0 ~3 minecraft:coarse_dirt
setblock ~1 ~0 ~4 minecraft:rooted_dirt
fill ~1 ~0 ~5 ~1 ~0 ~7 minecraft:coarse_dirt
fill ~1 ~0 ~8 ~2 ~0 ~8 minecraft:podzol
fill ~1 ~0 ~9 ~4 ~0 ~9 minecraft:rooted_dirt
setblock ~1 ~0 ~10 minecraft:coarse_dirt
fill ~1 ~0 ~11 ~2 ~0 ~11 minecraft:packed_mud
fill ~1 ~0 ~12 ~1 ~0 ~13 minecraft:rooted_dirt
setblock ~2 ~0 ~6 minecraft:rooted_dirt
setblock ~2 ~0 ~7 minecraft:packed_mud
setblock ~2 ~0 ~10 minecraft:podzol
fill ~2 ~0 ~12 ~2 ~0 ~13 minecraft:coarse_dirt
setblock ~2 ~0 ~14 minecraft:rooted_dirt
setblock ~3 ~0 ~6 minecraft:dirt_path
setblock ~3 ~0 ~7 minecraft:rooted_dirt
setblock ~3 ~0 ~8 minecraft:coarse_dirt
fill ~3 ~0 ~10 ~3 ~0 ~11 minecraft:coarse_dirt
setblock ~3 ~0 ~12 minecraft:rooted_dirt
fill ~3 ~0 ~13 ~3 ~0 ~15 minecraft:coarse_dirt
fill ~4 ~0 ~6 ~4 ~0 ~8 minecraft:coarse_dirt
setblock ~4 ~0 ~10 minecraft:dirt_path
setblock ~4 ~0 ~11 minecraft:rooted_dirt
fill ~4 ~0 ~12 ~4 ~0 ~13 minecraft:packed_mud
setblock ~4 ~0 ~14 minecraft:coarse_dirt
setblock ~4 ~0 ~15 minecraft:packed_mud
fill ~5 ~0 ~6 ~5 ~0 ~7 minecraft:packed_mud
fill ~5 ~0 ~8 ~6 ~0 ~8 minecraft:coarse_dirt
setblock ~5 ~0 ~9 minecraft:packed_mud
fill ~5 ~0 ~10 ~6 ~0 ~10 minecraft:coarse_dirt
setblock ~5 ~0 ~11 minecraft:coarse_dirt
setblock ~5 ~0 ~12 minecraft:dirt_path
setblock ~5 ~0 ~13 minecraft:rooted_dirt
setblock ~5 ~0 ~14 minecraft:podzol
setblock ~5 ~0 ~15 minecraft:coarse_dirt
setblock ~6 ~0 ~0 minecraft:podzol
setblock ~6 ~0 ~6 minecraft:rooted_dirt
setblock ~6 ~0 ~7 minecraft:packed_mud
fill ~6 ~0 ~9 ~7 ~0 ~9 minecraft:coarse_dirt
setblock ~6 ~0 ~11 minecraft:podzol
fill ~6 ~0 ~12 ~6 ~0 ~13 minecraft:coarse_dirt
fill ~6 ~0 ~14 ~6 ~0 ~15 minecraft:packed_mud
fill ~6 ~0 ~16 ~7 ~0 ~16 minecraft:podzol
setblock ~7 ~0 ~0 minecraft:rooted_dirt
fill ~7 ~0 ~6 ~7 ~0 ~7 minecraft:coarse_dirt
setblock ~7 ~0 ~8 minecraft:podzol
fill ~7 ~0 ~10 ~7 ~0 ~12 minecraft:podzol
fill ~7 ~0 ~13 ~7 ~0 ~14 minecraft:coarse_dirt
fill ~7 ~0 ~15 ~8 ~0 ~15 minecraft:podzol
setblock ~8 ~0 ~0 minecraft:podzol
setblock ~8 ~0 ~6 minecraft:dirt_path
setblock ~8 ~0 ~7 minecraft:packed_mud
fill ~8 ~0 ~8 ~8 ~0 ~11 minecraft:coarse_dirt
fill ~8 ~0 ~12 ~8 ~0 ~14 minecraft:packed_mud
setblock ~8 ~0 ~16 minecraft:coarse_dirt
fill ~9 ~0 ~0 ~9 ~0 ~1 minecraft:coarse_dirt
setblock ~9 ~0 ~2 minecraft:rooted_dirt
fill ~9 ~0 ~3 ~9 ~0 ~5 minecraft:packed_mud
setblock ~9 ~0 ~6 minecraft:coarse_dirt
setblock ~9 ~0 ~7 minecraft:dirt_path
setblock ~9 ~0 ~8 minecraft:packed_mud
fill ~9 ~0 ~9 ~9 ~0 ~11 minecraft:coarse_dirt
fill ~9 ~0 ~12 ~11 ~0 ~12 minecraft:podzol
setblock ~9 ~0 ~13 minecraft:coarse_dirt
setblock ~9 ~0 ~14 minecraft:podzol
fill ~9 ~0 ~15 ~10 ~0 ~15 minecraft:packed_mud
setblock ~9 ~0 ~16 minecraft:podzol
setblock ~10 ~0 ~0 minecraft:packed_mud
fill ~10 ~0 ~1 ~11 ~0 ~1 minecraft:coarse_dirt
setblock ~10 ~0 ~2 minecraft:coarse_dirt
fill ~10 ~0 ~3 ~11 ~0 ~3 minecraft:rooted_dirt
setblock ~10 ~0 ~4 minecraft:rooted_dirt
fill ~10 ~0 ~5 ~10 ~0 ~7 minecraft:packed_mud
setblock ~10 ~0 ~8 minecraft:gravel
setblock ~10 ~0 ~9 minecraft:dirt_path
setblock ~10 ~0 ~10 minecraft:coarse_dirt
setblock ~10 ~0 ~11 minecraft:packed_mud
setblock ~10 ~0 ~13 minecraft:podzol
setblock ~10 ~0 ~14 minecraft:coarse_dirt
setblock ~10 ~0 ~16 minecraft:coarse_dirt
setblock ~11 ~0 ~2 minecraft:podzol
fill ~11 ~0 ~4 ~12 ~0 ~4 minecraft:coarse_dirt
fill ~11 ~0 ~5 ~12 ~0 ~5 minecraft:rooted_dirt
setblock ~11 ~0 ~6 minecraft:coarse_dirt
fill ~11 ~0 ~7 ~12 ~0 ~7 minecraft:dirt_path
fill ~11 ~0 ~8 ~11 ~0 ~10 minecraft:dirt_path
fill ~11 ~0 ~11 ~14 ~0 ~11 minecraft:coarse_dirt
setblock ~11 ~0 ~13 minecraft:coarse_dirt
fill ~11 ~0 ~14 ~11 ~0 ~15 minecraft:packed_mud
setblock ~12 ~0 ~1 minecraft:packed_mud
fill ~12 ~0 ~2 ~13 ~0 ~2 minecraft:coarse_dirt
setblock ~12 ~0 ~3 minecraft:podzol
fill ~12 ~0 ~6 ~13 ~0 ~6 minecraft:podzol
setblock ~12 ~0 ~8 minecraft:coarse_dirt
setblock ~12 ~0 ~9 minecraft:dirt_path
setblock ~12 ~0 ~10 minecraft:packed_mud
fill ~12 ~0 ~12 ~15 ~0 ~12 minecraft:coarse_dirt
fill ~12 ~0 ~13 ~12 ~0 ~14 minecraft:podzol
setblock ~12 ~0 ~15 minecraft:rooted_dirt
setblock ~13 ~0 ~1 minecraft:rooted_dirt
setblock ~13 ~0 ~3 minecraft:rooted_dirt
setblock ~13 ~0 ~4 minecraft:podzol
setblock ~13 ~0 ~5 minecraft:coarse_dirt
setblock ~13 ~0 ~7 minecraft:coarse_dirt
fill ~13 ~0 ~8 ~13 ~0 ~10 minecraft:dirt_path
fill ~13 ~0 ~13 ~14 ~0 ~13 minecraft:rooted_dirt
fill ~13 ~0 ~14 ~13 ~0 ~15 minecraft:podzol
setblock ~14 ~0 ~2 minecraft:packed_mud
setblock ~14 ~0 ~3 minecraft:podzol
fill ~14 ~0 ~4 ~14 ~0 ~5 minecraft:packed_mud
fill ~14 ~0 ~6 ~14 ~0 ~8 minecraft:coarse_dirt
setblock ~14 ~0 ~9 minecraft:dirt_path
fill ~14 ~0 ~10 ~15 ~0 ~10 minecraft:packed_mud
setblock ~14 ~0 ~14 minecraft:coarse_dirt
setblock ~15 ~0 ~3 minecraft:coarse_dirt
fill ~15 ~0 ~4 ~16 ~0 ~4 minecraft:rooted_dirt
fill ~15 ~0 ~5 ~15 ~0 ~7 minecraft:coarse_dirt
setblock ~15 ~0 ~8 minecraft:rooted_dirt
setblock ~15 ~0 ~9 minecraft:podzol
setblock ~15 ~0 ~11 minecraft:podzol
setblock ~15 ~0 ~13 minecraft:coarse_dirt
setblock ~16 ~0 ~5 minecraft:podzol
setblock ~16 ~0 ~6 minecraft:packed_mud
setblock ~16 ~0 ~7 minecraft:coarse_dirt
fill ~16 ~0 ~8 ~17 ~0 ~8 minecraft:packed_mud
fill ~16 ~0 ~9 ~16 ~0 ~12 minecraft:coarse_dirt
fill ~17 ~0 ~6 ~17 ~0 ~7 minecraft:podzol
setblock ~17 ~0 ~9 minecraft:podzol
setblock ~17 ~0 ~10 minecraft:dirt_path

# the hut: walls of stacked logs (spruce and dark oak courses alternating), a dark oak log at each corner, a plank floor on a cobblestone foot; walls y 1..2 on x 2 and x 8 (z 1..5) and z 1 and z 5 (x 2..8); the door a gap of one at (8, 1..2, 3) on the east gable (the eave of the long sides is too low for a door), a window gap at (5, 1..2, 5)
fill ~2 ~0 ~1 ~8 ~0 ~1 minecraft:cobblestone
fill ~2 ~0 ~2 ~2 ~0 ~5 minecraft:cobblestone
fill ~3 ~0 ~2 ~4 ~0 ~2 minecraft:dark_oak_planks
fill ~3 ~0 ~3 ~4 ~0 ~3 minecraft:spruce_planks
setblock ~3 ~0 ~4 minecraft:spruce_planks
fill ~3 ~0 ~5 ~7 ~0 ~5 minecraft:cobblestone
setblock ~4 ~0 ~4 minecraft:dark_oak_planks
fill ~5 ~0 ~2 ~7 ~0 ~3 minecraft:spruce_planks
fill ~5 ~0 ~4 ~6 ~0 ~4 minecraft:spruce_planks
setblock ~7 ~0 ~4 minecraft:dark_oak_planks
fill ~8 ~0 ~2 ~8 ~0 ~5 minecraft:cobblestone
fill ~2 ~1 ~1 ~2 ~2 ~1 minecraft:dark_oak_log[axis=y]
fill ~2 ~1 ~2 ~2 ~1 ~4 minecraft:spruce_log[axis=z]
fill ~2 ~1 ~5 ~2 ~2 ~5 minecraft:dark_oak_log[axis=y]
fill ~3 ~1 ~1 ~7 ~1 ~1 minecraft:spruce_log[axis=x]
fill ~3 ~1 ~5 ~4 ~1 ~5 minecraft:spruce_log[axis=x]
fill ~5 ~1 ~5 ~5 ~2 ~5 minecraft:air
fill ~6 ~1 ~5 ~7 ~1 ~5 minecraft:spruce_log[axis=x]
fill ~8 ~1 ~1 ~8 ~2 ~1 minecraft:dark_oak_log[axis=y]
setblock ~8 ~1 ~2 minecraft:spruce_log[axis=z]
fill ~8 ~1 ~3 ~8 ~2 ~3 minecraft:air
setblock ~8 ~1 ~4 minecraft:spruce_log[axis=z]
fill ~8 ~1 ~5 ~8 ~2 ~5 minecraft:dark_oak_log[axis=y]
fill ~2 ~2 ~2 ~2 ~2 ~4 minecraft:dark_oak_log[axis=z]
fill ~3 ~2 ~1 ~7 ~2 ~1 minecraft:dark_oak_log[axis=x]
fill ~3 ~2 ~5 ~4 ~2 ~5 minecraft:dark_oak_log[axis=x]
fill ~6 ~2 ~5 ~7 ~2 ~5 minecraft:dark_oak_log[axis=x]
setblock ~8 ~2 ~2 minecraft:dark_oak_log[axis=z]
setblock ~8 ~2 ~4 minecraft:dark_oak_log[axis=z]

# the roof: dark spruce shingles (one in five dark oak) over x 1..9, z 0..6, eaves y 2, ridge y 5 of dark oak planks, dark oak verge
fill ~1 ~2 ~0 ~9 ~2 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~1 ~2 ~6 ~9 ~2 ~6 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~1 ~3 ~1 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~1 ~3 ~5 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~2 ~3 ~1 ~7 ~3 ~1 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~2 ~3 ~5 ~8 ~3 ~5 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~8 ~3 ~1 ~9 ~3 ~1 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~9 ~3 ~5 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~1 ~4 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~1 ~4 ~4 ~2 ~4 ~4 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~2 ~4 ~2 ~8 ~4 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~3 ~4 ~4 ~8 ~4 ~4 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~9 ~4 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~9 ~4 ~4 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~1 ~5 ~3 ~9 ~5 ~3 minecraft:dark_oak_planks

# the gable walls of the hut (x 2 and x 8) filled with boards, a king post at z 3
setblock ~2 ~3 ~2 minecraft:dark_oak_planks
fill ~2 ~3 ~3 ~2 ~4 ~3 minecraft:dark_oak_log[axis=y]
setblock ~2 ~3 ~4 minecraft:spruce_planks
setblock ~8 ~3 ~2 minecraft:spruce_planks
fill ~8 ~3 ~3 ~8 ~4 ~3 minecraft:dark_oak_log[axis=y]
setblock ~8 ~3 ~4 minecraft:spruce_planks

# inside the hut: a bunk of hay under a red blanket, a table and a stool, barrels, a lantern hung from the ridge
fill ~3 ~1 ~2 ~4 ~1 ~2 minecraft:hay_block[axis=x]
setblock ~3 ~1 ~4 minecraft:barrel[facing=up]
setblock ~6 ~1 ~2 minecraft:spruce_stairs[facing=east,half=bottom]
setblock ~7 ~1 ~2 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~7 ~1 ~4 minecraft:barrel[facing=up]
fill ~3 ~2 ~2 ~4 ~2 ~2 minecraft:red_carpet
setblock ~7 ~2 ~2 minecraft:spruce_pressure_plate
setblock ~5 ~3 ~3 minecraft:lantern[hanging=true]
setblock ~5 ~4 ~3 minecraft:chain[axis=y]

# the old pile, north-east stack: logs along z, seven wide at the foot, a course narrower each layer, four high at the middle; bark and cut ends alternate (spruce and stripped spruce)
fill ~10 ~1 ~0 ~11 ~1 ~3 minecraft:spruce_log[axis=z]
fill ~10 ~1 ~4 ~11 ~1 ~4 minecraft:stripped_spruce_log[axis=z]
setblock ~12 ~1 ~0 minecraft:stripped_spruce_log[axis=z]
fill ~12 ~1 ~1 ~12 ~2 ~2 minecraft:spruce_log[axis=z]
fill ~12 ~1 ~3 ~12 ~2 ~3 minecraft:stripped_spruce_log[axis=z]
fill ~12 ~1 ~4 ~12 ~3 ~4 minecraft:spruce_log[axis=z]
fill ~13 ~1 ~0 ~16 ~1 ~1 minecraft:spruce_log[axis=z]
fill ~13 ~1 ~2 ~15 ~3 ~2 minecraft:spruce_log[axis=z]
fill ~13 ~1 ~3 ~13 ~2 ~3 minecraft:spruce_log[axis=z]
fill ~13 ~1 ~4 ~13 ~3 ~4 minecraft:stripped_spruce_log[axis=z]
fill ~14 ~1 ~3 ~14 ~2 ~3 minecraft:stripped_spruce_log[axis=z]
fill ~14 ~1 ~4 ~14 ~3 ~4 minecraft:spruce_log[axis=z]
fill ~15 ~1 ~3 ~16 ~1 ~4 minecraft:spruce_log[axis=z]
setblock ~16 ~1 ~2 minecraft:stripped_spruce_log[axis=z]
fill ~10 ~2 ~0 ~10 ~2 ~4 minecraft:spruce_log[axis=z]
setblock ~11 ~2 ~0 minecraft:stripped_spruce_log[axis=z]
fill ~11 ~2 ~1 ~11 ~2 ~4 minecraft:spruce_log[axis=z]
fill ~12 ~2 ~0 ~13 ~3 ~0 minecraft:spruce_log[axis=z]
fill ~13 ~2 ~1 ~14 ~4 ~1 minecraft:spruce_log[axis=z]
fill ~14 ~2 ~0 ~14 ~3 ~0 minecraft:stripped_spruce_log[axis=z]
fill ~15 ~2 ~0 ~16 ~2 ~1 minecraft:spruce_log[axis=z]
fill ~15 ~2 ~3 ~15 ~3 ~3 minecraft:spruce_log[axis=z]
setblock ~15 ~2 ~4 minecraft:stripped_spruce_log[axis=z]
fill ~16 ~2 ~2 ~16 ~2 ~4 minecraft:spruce_log[axis=z]
setblock ~11 ~3 ~0 minecraft:spruce_log[axis=z]
fill ~11 ~3 ~1 ~12 ~3 ~2 minecraft:stripped_spruce_log[axis=z]
fill ~11 ~3 ~3 ~12 ~3 ~3 minecraft:spruce_log[axis=z]
setblock ~11 ~3 ~4 minecraft:spruce_log[axis=z]
fill ~13 ~3 ~3 ~13 ~4 ~3 minecraft:stripped_spruce_log[axis=z]
fill ~14 ~3 ~3 ~14 ~4 ~3 minecraft:spruce_log[axis=z]
setblock ~15 ~3 ~0 minecraft:spruce_log[axis=z]
setblock ~15 ~3 ~1 minecraft:stripped_spruce_log[axis=z]
setblock ~15 ~3 ~4 minecraft:spruce_log[axis=z]
setblock ~12 ~4 ~1 minecraft:stripped_spruce_log[axis=z]
fill ~12 ~4 ~2 ~13 ~4 ~2 minecraft:spruce_log[axis=z]
setblock ~12 ~4 ~3 minecraft:spruce_log[axis=z]
setblock ~14 ~4 ~2 minecraft:stripped_spruce_log[axis=z]
fill ~13 ~5 ~1 ~13 ~5 ~3 minecraft:spruce_log[axis=z]

# the south stack: logs along x, seven long and four deep at the foot, three high, the top course unfinished; stakes of dark oak fences hold its west end
setblock ~8 ~1 ~12 minecraft:dark_oak_fence[north=true,east=true,south=false,west=false]
fill ~8 ~1 ~15 ~8 ~2 ~15 minecraft:dark_oak_fence[north=false,east=true,south=false,west=false]
fill ~9 ~1 ~12 ~9 ~2 ~15 minecraft:spruce_log[axis=x]
setblock ~10 ~1 ~12 minecraft:stripped_spruce_log[axis=x]
fill ~10 ~1 ~13 ~10 ~3 ~15 minecraft:spruce_log[axis=x]
fill ~11 ~1 ~12 ~13 ~1 ~14 minecraft:spruce_log[axis=x]
setblock ~11 ~1 ~15 minecraft:spruce_log[axis=x]
setblock ~12 ~1 ~15 minecraft:stripped_spruce_log[axis=x]
setblock ~13 ~1 ~15 minecraft:spruce_log[axis=x]
fill ~14 ~1 ~12 ~15 ~1 ~12 minecraft:stripped_spruce_log[axis=x]
fill ~14 ~1 ~13 ~15 ~2 ~15 minecraft:spruce_log[axis=x]
setblock ~8 ~2 ~12 minecraft:dark_oak_fence[north=false,east=true,south=false,west=false]
fill ~10 ~2 ~12 ~14 ~2 ~12 minecraft:spruce_log[axis=x]
fill ~11 ~2 ~13 ~11 ~3 ~14 minecraft:spruce_log[axis=x]
setblock ~11 ~2 ~15 minecraft:stripped_spruce_log[axis=x]
fill ~12 ~2 ~13 ~12 ~2 ~14 minecraft:stripped_spruce_log[axis=x]
setblock ~12 ~2 ~15 minecraft:spruce_log[axis=x]
fill ~13 ~2 ~13 ~13 ~2 ~14 minecraft:spruce_log[axis=x]
setblock ~13 ~2 ~15 minecraft:stripped_spruce_log[axis=x]
setblock ~15 ~2 ~12 minecraft:stripped_spruce_log[axis=x]
setblock ~8 ~3 ~12 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~8 ~3 ~15 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~11 ~3 ~15 ~11 ~4 ~15 minecraft:spruce_log[axis=x]
fill ~12 ~3 ~14 ~12 ~4 ~14 minecraft:spruce_log[axis=x]
setblock ~12 ~3 ~15 minecraft:stripped_spruce_log[axis=x]
fill ~13 ~3 ~13 ~14 ~3 ~13 minecraft:spruce_log[axis=x]
fill ~13 ~3 ~15 ~13 ~4 ~15 minecraft:spruce_log[axis=x]
setblock ~14 ~3 ~14 minecraft:spruce_log[axis=x]
setblock ~14 ~3 ~15 minecraft:stripped_spruce_log[axis=x]
setblock ~11 ~4 ~14 minecraft:spruce_log[axis=x]
setblock ~12 ~4 ~15 minecraft:spruce_log[axis=x]

# rolled logs against the north side of the south stack (x 10..11, z 11)
fill ~10 ~1 ~11 ~11 ~1 ~11 minecraft:spruce_log[axis=x]

# the fire pit at (6..8, 9..11): a ring of cobblestone round a lit campfire, log seats (spruce stairs and stripped logs on end) round it
setblock ~4 ~1 ~8 minecraft:stripped_spruce_log[axis=y]
setblock ~4 ~1 ~9 minecraft:spruce_log[axis=y]
setblock ~5 ~1 ~10 minecraft:spruce_stairs[facing=east,half=bottom]
setblock ~6 ~1 ~9 minecraft:andesite
fill ~6 ~1 ~10 ~6 ~1 ~11 minecraft:mossy_cobblestone
fill ~7 ~1 ~9 ~8 ~1 ~9 minecraft:cobblestone
setblock ~7 ~1 ~10 minecraft:campfire[lit=true,signal_fire=false,facing=east]
setblock ~7 ~1 ~11 minecraft:andesite
setblock ~7 ~1 ~12 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~8 ~1 ~10 minecraft:mossy_cobblestone
setblock ~8 ~1 ~11 minecraft:cobblestone
setblock ~9 ~1 ~8 minecraft:spruce_stairs[facing=west,half=bottom]

# two sawhorses north of the lane (x 11..13 and x 15..17, z 6): two fence legs and a log across each, a stripped log lying on the first
setblock ~11 ~1 ~6 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
setblock ~13 ~1 ~6 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~15 ~1 ~6 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~17 ~1 ~6 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~11 ~2 ~6 ~13 ~2 ~6 minecraft:spruce_log[axis=x]
fill ~15 ~2 ~6 ~17 ~2 ~6 minecraft:spruce_log[axis=x]
fill ~11 ~3 ~6 ~13 ~3 ~6 minecraft:stripped_spruce_log[axis=x]

# chopping blocks (stripped logs on end) with split wood round them
setblock ~3 ~1 ~7 minecraft:stripped_spruce_log[axis=y]
setblock ~9 ~1 ~1 minecraft:spruce_log[axis=z]
setblock ~9 ~1 ~2 minecraft:spruce_log[axis=x]
fill ~9 ~1 ~5 ~10 ~1 ~5 minecraft:stripped_spruce_log[axis=y]
setblock ~11 ~1 ~5 minecraft:spruce_log[axis=x]

# the back wall of the lean-to (x 0, z 9..14, y 1..4) is a wall of firewood: spruce logs laid end on to the west, one in four stripped, a dark oak post at each end
fill ~0 ~1 ~9 ~0 ~4 ~9 minecraft:dark_oak_log[axis=x]
setblock ~0 ~1 ~10 minecraft:spruce_log[axis=x]
setblock ~0 ~1 ~11 minecraft:stripped_spruce_log[axis=x]
fill ~0 ~1 ~12 ~0 ~1 ~13 minecraft:spruce_log[axis=x]
fill ~0 ~1 ~14 ~0 ~4 ~14 minecraft:dark_oak_log[axis=x]
setblock ~1 ~1 ~10 minecraft:grindstone[face=floor,facing=east]
fill ~1 ~1 ~11 ~1 ~2 ~11 minecraft:barrel[facing=up]
setblock ~1 ~1 ~12 minecraft:chest[facing=east]
setblock ~1 ~1 ~13 minecraft:hay_block[axis=z]
fill ~2 ~1 ~9 ~2 ~2 ~9 minecraft:dark_oak_log[axis=y]
fill ~2 ~1 ~14 ~2 ~2 ~14 minecraft:dark_oak_log[axis=y]
setblock ~0 ~2 ~10 minecraft:stripped_spruce_log[axis=z]
setblock ~0 ~2 ~11 minecraft:spruce_log[axis=z]
fill ~0 ~2 ~12 ~0 ~3 ~12 minecraft:spruce_log[axis=x]
setblock ~0 ~2 ~13 minecraft:spruce_log[axis=z]
fill ~0 ~3 ~10 ~0 ~4 ~11 minecraft:spruce_log[axis=x]
setblock ~0 ~3 ~13 minecraft:stripped_spruce_log[axis=x]
fill ~2 ~3 ~9 ~2 ~3 ~14 minecraft:spruce_stairs[facing=west,half=bottom]
setblock ~0 ~4 ~12 minecraft:stripped_spruce_log[axis=x]
setblock ~0 ~4 ~13 minecraft:spruce_log[axis=x]
fill ~1 ~4 ~9 ~1 ~4 ~14 minecraft:spruce_stairs[facing=west,half=bottom]
fill ~0 ~5 ~9 ~0 ~5 ~14 minecraft:spruce_stairs[facing=west,half=bottom]

# the jib hoist at (4, 1..5, 13): a dark oak post, an arm of logs over to x 7 at y 5, a brace of stairs, a chain with a lantern hanging for a hook over the fire side
fill ~4 ~1 ~13 ~4 ~5 ~13 minecraft:dark_oak_log[axis=y]
setblock ~7 ~2 ~13 minecraft:lantern[hanging=true]
fill ~7 ~3 ~13 ~7 ~4 ~13 minecraft:chain[axis=y]
setblock ~5 ~4 ~13 minecraft:dark_oak_stairs[facing=west,half=top]
fill ~5 ~5 ~13 ~7 ~5 ~13 minecraft:dark_oak_log[axis=x]

# split wood piled outside the south-west corner of the lean-to (x 0..3, z 15..16): two courses of logs end on to the south, a stripped log set on end beside it, and a lantern post at the lane side of the fire pit (9, 1..3, 10)
fill ~0 ~1 ~15 ~2 ~1 ~15 minecraft:spruce_log[axis=z]
setblock ~0 ~1 ~16 minecraft:stripped_spruce_log[axis=z]
fill ~1 ~1 ~16 ~3 ~1 ~16 minecraft:spruce_log[axis=z]
setblock ~3 ~1 ~15 minecraft:stripped_spruce_log[axis=z]
setblock ~4 ~1 ~15 minecraft:stripped_spruce_log[axis=y]
setblock ~9 ~1 ~10 minecraft:dark_oak_fence[north=false,east=false,south=false,west=true]
fill ~1 ~2 ~15 ~2 ~2 ~15 minecraft:spruce_log[axis=z]
fill ~1 ~2 ~16 ~2 ~2 ~16 minecraft:stripped_spruce_log[axis=z]
fill ~9 ~2 ~10 ~9 ~3 ~10 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~9 ~4 ~10 minecraft:lantern[hanging=false]

# stumps round the clearing: stripped log stumps, a few single stumps of spruce
setblock ~1 ~1 ~5 minecraft:stripped_spruce_log[axis=y]
setblock ~1 ~1 ~7 minecraft:spruce_log[axis=y]
setblock ~3 ~1 ~11 minecraft:spruce_log[axis=y]
setblock ~17 ~1 ~8 minecraft:stripped_spruce_log[axis=y]

# moss carpet on a few log ends and wall tops, vines on the stack ends
fill ~17 ~0 ~4 ~17 ~1 ~4 minecraft:vine[west=true]
setblock ~3 ~1 ~13 minecraft:vine[east=true]
fill ~3 ~1 ~14 ~3 ~2 ~14 minecraft:vine[west=true]
setblock ~4 ~1 ~14 minecraft:vine[north=true]
setblock ~5 ~1 ~2 minecraft:vine[north=true]
setblock ~6 ~1 ~6 minecraft:vine[north=true]
setblock ~8 ~1 ~14 minecraft:vine[east=true]
fill ~17 ~1 ~0 ~17 ~1 ~1 minecraft:vine[west=true]
setblock ~4 ~2 ~9 minecraft:moss_carpet
setblock ~6 ~2 ~11 minecraft:moss_carpet
setblock ~8 ~2 ~9 minecraft:moss_carpet
setblock ~11 ~2 ~11 minecraft:moss_carpet
setblock ~17 ~2 ~5 minecraft:vine[south=true]
setblock ~13 ~3 ~12 minecraft:vine[south=true]
setblock ~16 ~3 ~1 minecraft:moss_carpet
setblock ~17 ~3 ~6 minecraft:moss_carpet
setblock ~12 ~4 ~6 minecraft:moss_carpet
setblock ~15 ~4 ~1 minecraft:moss_carpet
setblock ~12 ~5 ~2 minecraft:moss_carpet
fill ~12 ~5 ~14 ~12 ~5 ~15 minecraft:moss_carpet
setblock ~14 ~5 ~2 minecraft:moss_carpet
