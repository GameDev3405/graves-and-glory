# size 19 10 9
# kit/market: market square: two stalls under peaked canvas of earthy cloth with produce and wares, a stone market cross with dark oak arms on two steps, a covered well, all on a paved square with stone brick walk-throughs.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Stalls at x 0..4 and 14..18 (posts at z 1 and 7, canvas over z 0..8 from y 3 to the peak at y 6, a boarded floor); the market cross at (9, 6): steps x 7..11, z 4..8 (y 1) and x 8..10, z 5..7 (y 2), the shaft of full blocks from y 3 to y 9, arms of dark oak logs at y 8 (x 7..11 and z 5..7); the well x 8..10, z 0..2 under a gable roof (ridge y 6).
# The walk-throughs at x 5..6 and 12..13 are open from y 1 to 5 for the whole depth. The edge of the square frays: a part of its outer cells is structure void, so the ground of the map shows.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~7 ~2 ~0 minecraft:air
fill ~0 ~1 ~4 ~1 ~4 ~5 minecraft:air
fill ~0 ~1 ~6 ~0 ~4 ~6 minecraft:air
fill ~1 ~1 ~1 ~3 ~3 ~1 minecraft:air
fill ~1 ~1 ~7 ~3 ~2 ~8 minecraft:air
fill ~2 ~1 ~3 ~2 ~3 ~5 minecraft:air
fill ~3 ~1 ~4 ~4 ~4 ~5 minecraft:air
fill ~4 ~1 ~6 ~4 ~4 ~6 minecraft:air
fill ~5 ~1 ~1 ~6 ~9 ~8 minecraft:air
fill ~7 ~1 ~2 ~7 ~4 ~2 minecraft:air
fill ~8 ~1 ~3 ~9 ~9 ~3 minecraft:air
fill ~11 ~1 ~0 ~18 ~2 ~0 minecraft:air
fill ~11 ~1 ~2 ~11 ~4 ~2 minecraft:air
fill ~12 ~1 ~1 ~13 ~9 ~8 minecraft:air
fill ~14 ~1 ~3 ~14 ~4 ~6 minecraft:air
fill ~15 ~1 ~1 ~17 ~3 ~1 minecraft:air
fill ~15 ~1 ~4 ~15 ~4 ~5 minecraft:air
fill ~15 ~1 ~7 ~17 ~2 ~8 minecraft:air
fill ~16 ~1 ~3 ~16 ~3 ~5 minecraft:air
fill ~17 ~1 ~4 ~18 ~4 ~5 minecraft:air
fill ~18 ~1 ~6 ~18 ~4 ~6 minecraft:air
fill ~0 ~2 ~3 ~1 ~4 ~3 minecraft:air
fill ~3 ~2 ~3 ~4 ~4 ~3 minecraft:air
setblock ~4 ~2 ~8 minecraft:air
fill ~7 ~2 ~1 ~8 ~5 ~1 minecraft:air
fill ~7 ~2 ~4 ~9 ~9 ~4 minecraft:air
fill ~7 ~2 ~5 ~7 ~6 ~8 minecraft:air
fill ~8 ~2 ~8 ~10 ~9 ~8 minecraft:air
fill ~9 ~2 ~0 ~9 ~2 ~2 minecraft:air
fill ~10 ~2 ~1 ~11 ~5 ~1 minecraft:air
fill ~10 ~2 ~3 ~11 ~9 ~4 minecraft:air
fill ~11 ~2 ~5 ~11 ~6 ~8 minecraft:air
setblock ~14 ~2 ~8 minecraft:air
fill ~15 ~2 ~2 ~15 ~4 ~3 minecraft:air
fill ~17 ~2 ~3 ~18 ~4 ~3 minecraft:air
fill ~0 ~3 ~2 ~4 ~4 ~2 minecraft:air
fill ~1 ~3 ~6 ~3 ~3 ~7 minecraft:air
fill ~5 ~3 ~0 ~7 ~4 ~0 minecraft:air
fill ~7 ~3 ~3 ~7 ~9 ~3 minecraft:air
fill ~8 ~3 ~5 ~10 ~6 ~5 minecraft:air
fill ~8 ~3 ~6 ~8 ~7 ~7 minecraft:air
fill ~9 ~3 ~0 ~9 ~4 ~0 minecraft:air
fill ~9 ~3 ~2 ~9 ~4 ~2 minecraft:air
fill ~9 ~3 ~7 ~9 ~6 ~7 minecraft:air
fill ~10 ~3 ~6 ~10 ~7 ~7 minecraft:air
fill ~11 ~3 ~0 ~13 ~4 ~0 minecraft:air
fill ~14 ~3 ~2 ~14 ~4 ~2 minecraft:air
fill ~15 ~3 ~6 ~17 ~3 ~7 minecraft:air
fill ~16 ~3 ~2 ~18 ~4 ~2 minecraft:air
fill ~0 ~4 ~0 ~4 ~9 ~0 minecraft:air
fill ~0 ~4 ~8 ~4 ~9 ~8 minecraft:air
setblock ~1 ~4 ~6 minecraft:air
setblock ~2 ~4 ~3 minecraft:air
fill ~2 ~4 ~5 ~2 ~4 ~6 minecraft:air
setblock ~3 ~4 ~6 minecraft:air
fill ~14 ~4 ~0 ~18 ~9 ~0 minecraft:air
fill ~14 ~4 ~8 ~18 ~9 ~8 minecraft:air
setblock ~15 ~4 ~6 minecraft:air
setblock ~16 ~4 ~3 minecraft:air
fill ~16 ~4 ~5 ~16 ~4 ~6 minecraft:air
setblock ~17 ~4 ~6 minecraft:air
fill ~0 ~5 ~1 ~4 ~9 ~1 minecraft:air
fill ~0 ~5 ~7 ~4 ~9 ~7 minecraft:air
fill ~5 ~5 ~0 ~6 ~9 ~0 minecraft:air
fill ~12 ~5 ~0 ~13 ~9 ~0 minecraft:air
fill ~14 ~5 ~1 ~18 ~9 ~1 minecraft:air
fill ~14 ~5 ~7 ~18 ~9 ~7 minecraft:air
fill ~0 ~6 ~2 ~4 ~9 ~3 minecraft:air
fill ~0 ~6 ~5 ~4 ~9 ~6 minecraft:air
fill ~7 ~6 ~0 ~11 ~9 ~0 minecraft:air
fill ~7 ~6 ~2 ~11 ~9 ~2 minecraft:air
fill ~14 ~6 ~2 ~18 ~9 ~3 minecraft:air
fill ~14 ~6 ~5 ~18 ~9 ~6 minecraft:air
fill ~0 ~7 ~4 ~4 ~9 ~4 minecraft:air
fill ~7 ~7 ~1 ~11 ~9 ~1 minecraft:air
fill ~7 ~7 ~5 ~8 ~9 ~5 minecraft:air
fill ~7 ~7 ~7 ~7 ~9 ~8 minecraft:air
fill ~10 ~7 ~5 ~11 ~9 ~5 minecraft:air
fill ~11 ~7 ~7 ~11 ~9 ~8 minecraft:air
fill ~14 ~7 ~4 ~18 ~9 ~4 minecraft:air
fill ~8 ~8 ~7 ~8 ~9 ~7 minecraft:air
fill ~10 ~8 ~7 ~10 ~9 ~7 minecraft:air
fill ~7 ~9 ~6 ~8 ~9 ~6 minecraft:air
setblock ~9 ~9 ~5 minecraft:air
setblock ~9 ~9 ~7 minecraft:air
fill ~10 ~9 ~6 ~11 ~9 ~6 minecraft:air

# the square: paved with cobblestone, stone brick, andesite and mossy cobble in patches; the walk-throughs laid in lighter stone brick; its outer ring frays into the ground
setblock ~0 ~0 ~8 minecraft:stone
setblock ~1 ~0 ~0 minecraft:coarse_dirt
setblock ~2 ~0 ~0 minecraft:andesite
setblock ~3 ~0 ~0 minecraft:cobblestone
setblock ~4 ~0 ~8 minecraft:stone_bricks
fill ~5 ~0 ~1 ~5 ~0 ~2 minecraft:stone_bricks
setblock ~5 ~0 ~3 minecraft:cobblestone
setblock ~5 ~0 ~4 minecraft:stone_bricks
setblock ~5 ~0 ~5 minecraft:cobblestone
fill ~5 ~0 ~6 ~6 ~0 ~7 minecraft:stone_bricks
setblock ~5 ~0 ~8 minecraft:andesite
fill ~6 ~0 ~0 ~6 ~0 ~3 minecraft:stone_bricks
fill ~6 ~0 ~4 ~6 ~0 ~5 minecraft:cracked_stone_bricks
setblock ~7 ~0 ~0 minecraft:cobblestone
fill ~7 ~0 ~1 ~7 ~0 ~3 minecraft:andesite
setblock ~7 ~0 ~4 minecraft:cobblestone
setblock ~7 ~0 ~5 minecraft:coarse_dirt
fill ~7 ~0 ~6 ~8 ~0 ~6 minecraft:cobblestone
setblock ~7 ~0 ~7 minecraft:stone
setblock ~7 ~0 ~8 minecraft:mossy_cobblestone
setblock ~8 ~0 ~3 minecraft:andesite
setblock ~8 ~0 ~4 minecraft:stone_bricks
setblock ~8 ~0 ~5 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~7 minecraft:cracked_stone_bricks
fill ~8 ~0 ~8 ~9 ~0 ~8 minecraft:andesite
fill ~9 ~0 ~3 ~9 ~0 ~6 minecraft:cobblestone
fill ~9 ~0 ~7 ~10 ~0 ~7 minecraft:mossy_cobblestone
setblock ~10 ~0 ~3 minecraft:coarse_dirt
setblock ~10 ~0 ~4 minecraft:mossy_cobblestone
setblock ~10 ~0 ~5 minecraft:stone_bricks
fill ~10 ~0 ~6 ~11 ~0 ~6 minecraft:cobblestone
setblock ~10 ~0 ~8 minecraft:cobblestone
setblock ~11 ~0 ~0 minecraft:stone_bricks
setblock ~11 ~0 ~1 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~2 minecraft:stone
setblock ~11 ~0 ~3 minecraft:cobblestone
fill ~11 ~0 ~4 ~12 ~0 ~4 minecraft:stone_bricks
setblock ~11 ~0 ~5 minecraft:mossy_cobblestone
fill ~11 ~0 ~7 ~11 ~0 ~8 minecraft:cobblestone
fill ~12 ~0 ~0 ~13 ~0 ~0 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~1 minecraft:mossy_stone_bricks
setblock ~12 ~0 ~2 minecraft:stone_bricks
setblock ~12 ~0 ~3 minecraft:andesite
setblock ~12 ~0 ~5 minecraft:stone_bricks
fill ~12 ~0 ~6 ~12 ~0 ~7 minecraft:andesite
setblock ~12 ~0 ~8 minecraft:stone_bricks
setblock ~13 ~0 ~1 minecraft:stone_bricks
fill ~13 ~0 ~2 ~13 ~0 ~3 minecraft:andesite
setblock ~13 ~0 ~4 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~5 minecraft:andesite
fill ~13 ~0 ~6 ~13 ~0 ~7 minecraft:stone_bricks
setblock ~13 ~0 ~8 minecraft:cracked_stone_bricks
setblock ~14 ~0 ~8 minecraft:cobblestone
setblock ~15 ~0 ~0 minecraft:cobblestone
setblock ~16 ~0 ~0 minecraft:coarse_dirt
setblock ~16 ~0 ~8 minecraft:mossy_cobblestone
setblock ~17 ~0 ~0 minecraft:mossy_cobblestone
setblock ~17 ~0 ~8 minecraft:stone_bricks
setblock ~18 ~0 ~0 minecraft:cobblestone
setblock ~18 ~0 ~8 minecraft:andesite

# stall 1: a boarded floor, spruce posts at the corners (logs at the back, fences at the front), a ridge pole at y 5 and peaked canvas of smooth_sandstone and terracotta stripes over it, a brown_terracotta ridge
fill ~0 ~0 ~1 ~2 ~0 ~1 minecraft:dark_oak_planks
fill ~0 ~0 ~2 ~1 ~0 ~2 minecraft:dark_oak_planks
fill ~0 ~0 ~3 ~0 ~0 ~5 minecraft:dark_oak_planks
setblock ~0 ~0 ~6 minecraft:spruce_planks
setblock ~0 ~0 ~7 minecraft:dark_oak_planks
fill ~1 ~0 ~3 ~1 ~0 ~7 minecraft:spruce_planks
fill ~2 ~0 ~2 ~3 ~0 ~2 minecraft:spruce_planks
fill ~2 ~0 ~3 ~2 ~0 ~4 minecraft:dark_oak_planks
fill ~2 ~0 ~5 ~2 ~0 ~7 minecraft:spruce_planks
fill ~3 ~0 ~1 ~4 ~0 ~1 minecraft:spruce_planks
fill ~3 ~0 ~3 ~3 ~0 ~4 minecraft:spruce_planks
setblock ~3 ~0 ~5 minecraft:dark_oak_planks
fill ~3 ~0 ~6 ~3 ~0 ~7 minecraft:spruce_planks
fill ~4 ~0 ~2 ~4 ~0 ~4 minecraft:dark_oak_planks
fill ~4 ~0 ~5 ~4 ~0 ~7 minecraft:spruce_planks
fill ~0 ~1 ~1 ~0 ~3 ~1 minecraft:spruce_log[axis=y]
fill ~0 ~1 ~7 ~0 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
fill ~4 ~1 ~1 ~4 ~3 ~1 minecraft:spruce_log[axis=y]
setblock ~4 ~1 ~7 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~4 ~2 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~0 ~3 ~0 minecraft:smooth_sandstone
setblock ~0 ~3 ~8 minecraft:smooth_sandstone
setblock ~1 ~3 ~0 minecraft:terracotta
setblock ~1 ~3 ~8 minecraft:terracotta
setblock ~2 ~3 ~0 minecraft:smooth_sandstone
setblock ~2 ~3 ~8 minecraft:smooth_sandstone
setblock ~3 ~3 ~0 minecraft:terracotta
setblock ~3 ~3 ~8 minecraft:terracotta
setblock ~4 ~3 ~0 minecraft:smooth_sandstone
setblock ~4 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~4 ~3 ~8 minecraft:smooth_sandstone
setblock ~0 ~4 ~1 minecraft:smooth_sandstone
setblock ~0 ~4 ~7 minecraft:smooth_sandstone
setblock ~1 ~4 ~1 minecraft:terracotta
setblock ~1 ~4 ~7 minecraft:terracotta
setblock ~2 ~4 ~1 minecraft:smooth_sandstone
setblock ~2 ~4 ~7 minecraft:smooth_sandstone
setblock ~3 ~4 ~1 minecraft:terracotta
setblock ~3 ~4 ~7 minecraft:terracotta
setblock ~4 ~4 ~1 minecraft:smooth_sandstone
setblock ~4 ~4 ~7 minecraft:smooth_sandstone
fill ~0 ~5 ~2 ~0 ~5 ~3 minecraft:smooth_sandstone
fill ~0 ~5 ~4 ~4 ~5 ~4 minecraft:spruce_log[axis=x]
fill ~0 ~5 ~5 ~0 ~5 ~6 minecraft:smooth_sandstone
fill ~1 ~5 ~2 ~1 ~5 ~3 minecraft:terracotta
fill ~1 ~5 ~5 ~1 ~5 ~6 minecraft:terracotta
fill ~2 ~5 ~2 ~2 ~5 ~3 minecraft:smooth_sandstone
fill ~2 ~5 ~5 ~2 ~5 ~6 minecraft:smooth_sandstone
fill ~3 ~5 ~2 ~3 ~5 ~3 minecraft:terracotta
fill ~3 ~5 ~5 ~3 ~5 ~6 minecraft:terracotta
fill ~4 ~5 ~2 ~4 ~5 ~3 minecraft:smooth_sandstone
fill ~4 ~5 ~5 ~4 ~5 ~6 minecraft:smooth_sandstone
fill ~0 ~6 ~4 ~4 ~6 ~4 minecraft:brown_terracotta

# stall 1: a counter of slabs at the front, a lantern from the ridge pole
fill ~1 ~1 ~6 ~3 ~1 ~6 minecraft:spruce_slab[type=top]
setblock ~2 ~4 ~4 minecraft:lantern[hanging=true]

# stall 2: a boarded floor, spruce posts at the corners (logs at the back, fences at the front), a ridge pole at y 5 and peaked canvas of white_terracotta and red_terracotta stripes over it, a brown_terracotta ridge
fill ~14 ~0 ~1 ~18 ~0 ~1 minecraft:spruce_planks
fill ~14 ~0 ~2 ~14 ~0 ~3 minecraft:spruce_planks
fill ~14 ~0 ~4 ~14 ~0 ~5 minecraft:dark_oak_planks
fill ~14 ~0 ~6 ~14 ~0 ~7 minecraft:spruce_planks
setblock ~15 ~0 ~2 minecraft:dark_oak_planks
fill ~15 ~0 ~3 ~15 ~0 ~7 minecraft:spruce_planks
setblock ~16 ~0 ~2 minecraft:spruce_planks
setblock ~16 ~0 ~3 minecraft:dark_oak_planks
fill ~16 ~0 ~4 ~16 ~0 ~7 minecraft:spruce_planks
setblock ~17 ~0 ~2 minecraft:dark_oak_planks
setblock ~17 ~0 ~3 minecraft:spruce_planks
fill ~17 ~0 ~4 ~17 ~0 ~5 minecraft:dark_oak_planks
fill ~17 ~0 ~6 ~18 ~0 ~6 minecraft:spruce_planks
setblock ~17 ~0 ~7 minecraft:spruce_planks
fill ~18 ~0 ~2 ~18 ~0 ~4 minecraft:spruce_planks
setblock ~18 ~0 ~5 minecraft:dark_oak_planks
setblock ~18 ~0 ~7 minecraft:dark_oak_planks
fill ~14 ~1 ~1 ~14 ~3 ~1 minecraft:spruce_log[axis=y]
setblock ~14 ~1 ~7 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
fill ~18 ~1 ~1 ~18 ~3 ~1 minecraft:spruce_log[axis=y]
fill ~18 ~1 ~7 ~18 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~14 ~2 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~14 ~3 ~0 minecraft:white_terracotta
setblock ~14 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~14 ~3 ~8 minecraft:white_terracotta
setblock ~15 ~3 ~0 minecraft:red_terracotta
setblock ~15 ~3 ~8 minecraft:red_terracotta
setblock ~16 ~3 ~0 minecraft:white_terracotta
setblock ~16 ~3 ~8 minecraft:white_terracotta
setblock ~17 ~3 ~0 minecraft:red_terracotta
setblock ~17 ~3 ~8 minecraft:red_terracotta
setblock ~18 ~3 ~0 minecraft:white_terracotta
setblock ~18 ~3 ~8 minecraft:white_terracotta
setblock ~14 ~4 ~1 minecraft:white_terracotta
setblock ~14 ~4 ~7 minecraft:white_terracotta
setblock ~15 ~4 ~1 minecraft:red_terracotta
setblock ~15 ~4 ~7 minecraft:red_terracotta
setblock ~16 ~4 ~1 minecraft:white_terracotta
setblock ~16 ~4 ~7 minecraft:white_terracotta
setblock ~17 ~4 ~1 minecraft:red_terracotta
setblock ~17 ~4 ~7 minecraft:red_terracotta
setblock ~18 ~4 ~1 minecraft:white_terracotta
setblock ~18 ~4 ~7 minecraft:white_terracotta
fill ~14 ~5 ~2 ~14 ~5 ~3 minecraft:white_terracotta
fill ~14 ~5 ~4 ~18 ~5 ~4 minecraft:spruce_log[axis=x]
fill ~14 ~5 ~5 ~14 ~5 ~6 minecraft:white_terracotta
fill ~15 ~5 ~2 ~15 ~5 ~3 minecraft:red_terracotta
fill ~15 ~5 ~5 ~15 ~5 ~6 minecraft:red_terracotta
fill ~16 ~5 ~2 ~16 ~5 ~3 minecraft:white_terracotta
fill ~16 ~5 ~5 ~16 ~5 ~6 minecraft:white_terracotta
fill ~17 ~5 ~2 ~17 ~5 ~3 minecraft:red_terracotta
fill ~17 ~5 ~5 ~17 ~5 ~6 minecraft:red_terracotta
fill ~18 ~5 ~2 ~18 ~5 ~3 minecraft:white_terracotta
fill ~18 ~5 ~5 ~18 ~5 ~6 minecraft:white_terracotta
fill ~14 ~6 ~4 ~18 ~6 ~4 minecraft:brown_terracotta

# stall 2: a counter of slabs at the front, a lantern from the ridge pole
fill ~15 ~1 ~6 ~17 ~1 ~6 minecraft:spruce_slab[type=top]
setblock ~16 ~4 ~4 minecraft:lantern[hanging=true]

# stall 1 (produce): barrels and hay on the back shelf with pumpkins and melons on them, sacks of grain behind, a cake, melons and pumpkins on the counter, crates in front
fill ~0 ~1 ~2 ~0 ~2 ~2 minecraft:barrel[facing=up]
setblock ~0 ~1 ~3 minecraft:white_wool
setblock ~0 ~1 ~8 minecraft:barrel[facing=up]
setblock ~1 ~1 ~2 minecraft:hay_block[axis=y]
setblock ~1 ~1 ~3 minecraft:composter
setblock ~2 ~1 ~2 minecraft:barrel[facing=north]
setblock ~3 ~1 ~2 minecraft:hay_block[axis=y]
setblock ~3 ~1 ~3 minecraft:brown_wool
setblock ~4 ~1 ~2 minecraft:barrel[facing=up]
setblock ~4 ~1 ~3 minecraft:hay_block[axis=z]
setblock ~4 ~1 ~8 minecraft:hay_block[axis=z]
setblock ~0 ~2 ~8 minecraft:barrel[facing=north]
setblock ~1 ~2 ~2 minecraft:pumpkin
setblock ~1 ~2 ~6 minecraft:melon
setblock ~2 ~2 ~2 minecraft:melon
setblock ~2 ~2 ~6 minecraft:cake
setblock ~3 ~2 ~2 minecraft:pumpkin
setblock ~3 ~2 ~6 minecraft:pumpkin
setblock ~4 ~2 ~2 minecraft:hay_block[axis=x]

# stall 2 (cloth and wares): a loom, a chest of coin, barrels and bolts of cloth on the back shelf, a dye vat, folded cloth and a lantern on the counter
setblock ~14 ~1 ~2 minecraft:barrel[facing=up]
setblock ~14 ~1 ~8 minecraft:hay_block[axis=x]
setblock ~15 ~1 ~2 minecraft:loom[facing=south]
setblock ~15 ~1 ~3 minecraft:cauldron
setblock ~16 ~1 ~2 minecraft:chest[facing=south]
setblock ~17 ~1 ~2 minecraft:barrel[facing=north]
setblock ~17 ~1 ~3 minecraft:barrel[facing=up]
setblock ~18 ~1 ~2 minecraft:barrel[facing=up]
setblock ~18 ~1 ~3 minecraft:brown_wool
setblock ~18 ~1 ~8 minecraft:barrel[facing=up]
setblock ~14 ~2 ~2 minecraft:flower_pot
setblock ~15 ~2 ~6 minecraft:brown_carpet
setblock ~16 ~2 ~2 minecraft:light_gray_wool
setblock ~16 ~2 ~6 minecraft:white_carpet
setblock ~17 ~2 ~2 minecraft:white_wool
setblock ~17 ~2 ~6 minecraft:lantern
setblock ~18 ~2 ~2 minecraft:brown_wool
setblock ~18 ~2 ~8 minecraft:barrel[facing=north]

# the market cross: two steps of mossy stone bricks (x 7..11, z 4..8 and x 8..10, z 5..7), a shaft of full stone bricks from y 3 to y 9 with a chiseled block at the crossing (y 8), arms of dark oak logs at y 8 (two blocks out along x, one along z), a lantern under the end of each arm
setblock ~7 ~1 ~4 minecraft:stone_bricks
setblock ~7 ~1 ~5 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~7 ~1 ~6 minecraft:mossy_stone_brick_stairs[facing=east,half=bottom]
setblock ~7 ~1 ~7 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~7 ~1 ~8 minecraft:stone_bricks
fill ~8 ~1 ~4 ~10 ~1 ~4 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~8 ~1 ~5 minecraft:mossy_stone_bricks
setblock ~8 ~1 ~6 minecraft:cracked_stone_bricks
fill ~8 ~1 ~7 ~8 ~2 ~7 minecraft:mossy_stone_bricks
fill ~8 ~1 ~8 ~10 ~1 ~8 minecraft:stone_brick_stairs[facing=north,half=bottom]
fill ~9 ~1 ~5 ~10 ~1 ~5 minecraft:stone_bricks
fill ~9 ~1 ~6 ~9 ~1 ~7 minecraft:stone_bricks
setblock ~10 ~1 ~6 minecraft:mossy_stone_bricks
setblock ~10 ~1 ~7 minecraft:cracked_stone_bricks
setblock ~11 ~1 ~4 minecraft:stone_bricks
fill ~11 ~1 ~5 ~11 ~1 ~6 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~11 ~1 ~7 minecraft:mossy_stone_brick_stairs[facing=west,half=bottom]
setblock ~11 ~1 ~8 minecraft:stone_bricks
setblock ~8 ~2 ~5 minecraft:stone_bricks
setblock ~8 ~2 ~6 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~9 ~2 ~5 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~9 ~2 ~6 minecraft:chiseled_stone_bricks
setblock ~9 ~2 ~7 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~10 ~2 ~5 minecraft:stone_bricks
setblock ~10 ~2 ~6 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~10 ~2 ~7 minecraft:stone_bricks
fill ~9 ~3 ~6 ~9 ~4 ~6 minecraft:stone_bricks
setblock ~9 ~5 ~6 minecraft:mossy_stone_bricks
fill ~9 ~6 ~6 ~9 ~7 ~6 minecraft:stone_bricks
setblock ~7 ~7 ~6 minecraft:lantern[hanging=true]
setblock ~9 ~7 ~5 minecraft:lantern[hanging=true]
setblock ~9 ~7 ~7 minecraft:lantern[hanging=true]
setblock ~11 ~7 ~6 minecraft:lantern[hanging=true]
fill ~7 ~8 ~6 ~8 ~8 ~6 minecraft:dark_oak_log[axis=x]
setblock ~9 ~8 ~5 minecraft:dark_oak_log[axis=z]
setblock ~9 ~8 ~6 minecraft:chiseled_stone_bricks
setblock ~9 ~8 ~7 minecraft:dark_oak_log[axis=z]
fill ~10 ~8 ~6 ~11 ~8 ~6 minecraft:dark_oak_log[axis=x]
setblock ~9 ~9 ~6 minecraft:mossy_stone_bricks

# the well at the north edge: a ring of cobblestone round the water, four fence posts, a gable roof of spruce stairs, a lantern on a chain over the water
fill ~8 ~0 ~0 ~9 ~0 ~1 minecraft:cobblestone
fill ~8 ~0 ~2 ~8 ~1 ~2 minecraft:cobblestone
fill ~9 ~0 ~2 ~9 ~1 ~2 minecraft:mossy_cobblestone
setblock ~10 ~0 ~0 minecraft:mossy_cobblestone
fill ~10 ~0 ~1 ~10 ~0 ~2 minecraft:cobblestone
fill ~8 ~1 ~0 ~10 ~1 ~0 minecraft:cobblestone
setblock ~8 ~1 ~1 minecraft:cobblestone
setblock ~9 ~1 ~1 minecraft:water
setblock ~10 ~1 ~1 minecraft:cobblestone
setblock ~10 ~1 ~2 minecraft:mossy_cobblestone
fill ~8 ~2 ~0 ~8 ~4 ~0 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~8 ~2 ~2 ~8 ~4 ~2 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~10 ~2 ~0 ~10 ~4 ~0 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~10 ~2 ~2 ~10 ~4 ~2 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~9 ~3 ~1 minecraft:lantern[hanging=true]
fill ~9 ~4 ~1 ~9 ~5 ~1 minecraft:chain[axis=y]
fill ~7 ~5 ~0 ~11 ~5 ~0 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~7 ~5 ~2 ~11 ~5 ~2 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~7 ~6 ~1 ~11 ~6 ~1 minecraft:spruce_planks

# crates and sacks on the square round the cross and the well
setblock ~7 ~1 ~1 minecraft:barrel[facing=east]
setblock ~7 ~1 ~3 minecraft:barrel[facing=north]
setblock ~10 ~1 ~3 minecraft:pumpkin
setblock ~11 ~1 ~1 minecraft:white_wool
setblock ~11 ~1 ~3 minecraft:hay_block[axis=x]
setblock ~7 ~2 ~3 minecraft:barrel[facing=up]
