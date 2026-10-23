# size 19 8 9
# kit/market: market square: two stalls under peaked canvas of muted cloth either side of a stone market cross hung with lanterns, crates and sacks of goods on a worn cobble square, walk-throughs between them.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Stalls at x 0..4 and 14..18 (posts at z 1 and 7, canvas over z 0..8 from y 3 to the peak at y 6); the market cross at (9, 4): steps x 7..11, z 2..6 (y 1) and x 8..10, z 3..5 (y 2), the shaft to y 6, arms at y 6, head y 7.
# The walk-throughs at x 5..6 and 12..13 are open from y 1 to 5 for the whole depth. The edge of the square frays: a part of its outer cells is structure void, so the ground of the map shows.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~7 ~2 ~0 minecraft:air
fill ~0 ~1 ~3 ~0 ~4 ~6 minecraft:air
fill ~0 ~1 ~8 ~0 ~2 ~8 minecraft:air
fill ~1 ~1 ~1 ~3 ~3 ~1 minecraft:air
fill ~1 ~1 ~4 ~1 ~4 ~5 minecraft:air
fill ~1 ~1 ~7 ~3 ~2 ~8 minecraft:air
fill ~2 ~1 ~3 ~2 ~3 ~5 minecraft:air
fill ~3 ~1 ~4 ~3 ~4 ~5 minecraft:air
fill ~4 ~1 ~3 ~4 ~4 ~6 minecraft:air
fill ~4 ~1 ~8 ~4 ~2 ~8 minecraft:air
fill ~5 ~1 ~1 ~10 ~7 ~1 minecraft:air
fill ~5 ~1 ~2 ~6 ~7 ~8 minecraft:air
fill ~7 ~1 ~7 ~10 ~7 ~7 minecraft:air
fill ~8 ~1 ~8 ~9 ~7 ~8 minecraft:air
fill ~10 ~1 ~0 ~18 ~2 ~0 minecraft:air
fill ~11 ~1 ~8 ~11 ~7 ~8 minecraft:air
fill ~12 ~1 ~1 ~13 ~7 ~8 minecraft:air
fill ~14 ~1 ~3 ~14 ~4 ~6 minecraft:air
fill ~14 ~1 ~8 ~14 ~2 ~8 minecraft:air
fill ~15 ~1 ~1 ~17 ~3 ~1 minecraft:air
fill ~15 ~1 ~4 ~15 ~4 ~5 minecraft:air
fill ~15 ~1 ~7 ~17 ~2 ~8 minecraft:air
fill ~16 ~1 ~3 ~18 ~3 ~5 minecraft:air
fill ~18 ~1 ~6 ~18 ~4 ~6 minecraft:air
fill ~18 ~1 ~8 ~18 ~2 ~8 minecraft:air
fill ~0 ~2 ~2 ~0 ~4 ~2 minecraft:air
fill ~1 ~2 ~3 ~1 ~4 ~3 minecraft:air
fill ~1 ~2 ~6 ~1 ~4 ~6 minecraft:air
fill ~3 ~2 ~2 ~4 ~4 ~2 minecraft:air
fill ~3 ~2 ~3 ~3 ~4 ~3 minecraft:air
fill ~7 ~2 ~2 ~10 ~7 ~2 minecraft:air
fill ~7 ~2 ~3 ~7 ~7 ~6 minecraft:air
fill ~7 ~2 ~8 ~7 ~7 ~8 minecraft:air
fill ~8 ~2 ~0 ~9 ~7 ~0 minecraft:air
fill ~8 ~2 ~6 ~10 ~7 ~6 minecraft:air
fill ~10 ~2 ~8 ~10 ~7 ~8 minecraft:air
fill ~11 ~2 ~1 ~11 ~7 ~7 minecraft:air
fill ~14 ~2 ~2 ~16 ~4 ~2 minecraft:air
fill ~15 ~2 ~3 ~15 ~4 ~3 minecraft:air
fill ~1 ~3 ~2 ~2 ~4 ~2 minecraft:air
setblock ~1 ~3 ~7 minecraft:air
fill ~2 ~3 ~6 ~3 ~3 ~7 minecraft:air
fill ~5 ~3 ~0 ~7 ~7 ~0 minecraft:air
fill ~8 ~3 ~3 ~10 ~7 ~3 minecraft:air
fill ~8 ~3 ~4 ~8 ~4 ~5 minecraft:air
fill ~9 ~3 ~5 ~9 ~7 ~5 minecraft:air
fill ~10 ~3 ~0 ~13 ~7 ~0 minecraft:air
fill ~10 ~3 ~4 ~10 ~4 ~5 minecraft:air
fill ~15 ~3 ~6 ~17 ~3 ~7 minecraft:air
fill ~17 ~3 ~2 ~18 ~4 ~2 minecraft:air
fill ~0 ~4 ~0 ~4 ~7 ~0 minecraft:air
fill ~0 ~4 ~8 ~4 ~7 ~8 minecraft:air
setblock ~2 ~4 ~3 minecraft:air
fill ~2 ~4 ~5 ~2 ~4 ~6 minecraft:air
setblock ~3 ~4 ~6 minecraft:air
fill ~14 ~4 ~0 ~18 ~7 ~0 minecraft:air
fill ~14 ~4 ~8 ~18 ~7 ~8 minecraft:air
setblock ~15 ~4 ~6 minecraft:air
fill ~16 ~4 ~3 ~18 ~4 ~3 minecraft:air
fill ~16 ~4 ~5 ~16 ~4 ~6 minecraft:air
fill ~17 ~4 ~4 ~18 ~4 ~5 minecraft:air
setblock ~17 ~4 ~6 minecraft:air
fill ~0 ~5 ~1 ~4 ~7 ~1 minecraft:air
fill ~0 ~5 ~7 ~4 ~7 ~7 minecraft:air
fill ~8 ~5 ~5 ~8 ~7 ~5 minecraft:air
fill ~10 ~5 ~5 ~10 ~7 ~5 minecraft:air
fill ~14 ~5 ~1 ~18 ~7 ~1 minecraft:air
fill ~14 ~5 ~7 ~18 ~7 ~7 minecraft:air
fill ~0 ~6 ~2 ~4 ~7 ~3 minecraft:air
fill ~0 ~6 ~5 ~4 ~7 ~6 minecraft:air
fill ~14 ~6 ~2 ~18 ~7 ~3 minecraft:air
fill ~14 ~6 ~5 ~18 ~7 ~6 minecraft:air
fill ~0 ~7 ~4 ~4 ~7 ~4 minecraft:air
setblock ~8 ~7 ~4 minecraft:air
setblock ~10 ~7 ~4 minecraft:air
fill ~14 ~7 ~4 ~18 ~7 ~4 minecraft:air

# the square: worn cobble, gravel and packed earth, the walk-throughs trodden to dirt; its outer ring frays into the ground
setblock ~0 ~0 ~1 minecraft:coarse_dirt
setblock ~0 ~0 ~2 minecraft:packed_mud
setblock ~0 ~0 ~3 minecraft:gravel
setblock ~0 ~0 ~7 minecraft:gravel
setblock ~1 ~0 ~0 minecraft:mossy_cobblestone
fill ~1 ~0 ~1 ~1 ~0 ~2 minecraft:packed_mud
setblock ~1 ~0 ~3 minecraft:cobblestone
setblock ~1 ~0 ~4 minecraft:gravel
setblock ~1 ~0 ~5 minecraft:packed_mud
fill ~1 ~0 ~6 ~1 ~0 ~7 minecraft:coarse_dirt
setblock ~2 ~0 ~0 minecraft:packed_mud
fill ~2 ~0 ~1 ~2 ~0 ~3 minecraft:coarse_dirt
fill ~2 ~0 ~4 ~2 ~0 ~7 minecraft:cobblestone
fill ~3 ~0 ~0 ~3 ~0 ~2 minecraft:cobblestone
setblock ~3 ~0 ~3 minecraft:gravel
setblock ~3 ~0 ~4 minecraft:coarse_dirt
fill ~3 ~0 ~5 ~3 ~0 ~7 minecraft:cobblestone
fill ~4 ~0 ~1 ~4 ~0 ~2 minecraft:gravel
setblock ~4 ~0 ~3 minecraft:cobblestone
fill ~4 ~0 ~4 ~4 ~0 ~5 minecraft:packed_mud
setblock ~4 ~0 ~6 minecraft:mossy_cobblestone
setblock ~4 ~0 ~7 minecraft:packed_mud
fill ~4 ~0 ~8 ~5 ~0 ~8 minecraft:gravel
fill ~5 ~0 ~1 ~6 ~0 ~1 minecraft:coarse_dirt
fill ~5 ~0 ~2 ~6 ~0 ~2 minecraft:gravel
fill ~5 ~0 ~3 ~5 ~0 ~4 minecraft:coarse_dirt
fill ~5 ~0 ~5 ~5 ~0 ~6 minecraft:gravel
setblock ~5 ~0 ~7 minecraft:coarse_dirt
setblock ~6 ~0 ~3 minecraft:gravel
fill ~6 ~0 ~4 ~6 ~0 ~5 minecraft:coarse_dirt
setblock ~6 ~0 ~6 minecraft:packed_mud
setblock ~6 ~0 ~7 minecraft:gravel
setblock ~7 ~0 ~0 minecraft:cobblestone
fill ~7 ~0 ~1 ~7 ~0 ~3 minecraft:packed_mud
fill ~7 ~0 ~4 ~8 ~0 ~4 minecraft:gravel
setblock ~7 ~0 ~5 minecraft:mossy_cobblestone
fill ~7 ~0 ~6 ~9 ~0 ~6 minecraft:cobblestone
fill ~7 ~0 ~7 ~8 ~0 ~7 minecraft:coarse_dirt
setblock ~8 ~0 ~0 minecraft:packed_mud
setblock ~8 ~0 ~1 minecraft:gravel
setblock ~8 ~0 ~2 minecraft:coarse_dirt
setblock ~8 ~0 ~3 minecraft:packed_mud
setblock ~8 ~0 ~5 minecraft:coarse_dirt
setblock ~9 ~0 ~1 minecraft:mossy_cobblestone
fill ~9 ~0 ~2 ~10 ~0 ~2 minecraft:gravel
fill ~9 ~0 ~3 ~9 ~0 ~4 minecraft:cobblestone
fill ~9 ~0 ~5 ~10 ~0 ~5 minecraft:gravel
setblock ~9 ~0 ~7 minecraft:packed_mud
setblock ~10 ~0 ~1 minecraft:packed_mud
setblock ~10 ~0 ~3 minecraft:mossy_cobblestone
setblock ~10 ~0 ~4 minecraft:packed_mud
setblock ~10 ~0 ~6 minecraft:gravel
setblock ~10 ~0 ~7 minecraft:coarse_dirt
setblock ~10 ~0 ~8 minecraft:cobblestone
setblock ~11 ~0 ~0 minecraft:gravel
fill ~11 ~0 ~1 ~12 ~0 ~1 minecraft:coarse_dirt
setblock ~11 ~0 ~2 minecraft:mossy_cobblestone
setblock ~11 ~0 ~3 minecraft:cobblestone
setblock ~11 ~0 ~4 minecraft:gravel
setblock ~11 ~0 ~5 minecraft:packed_mud
fill ~11 ~0 ~6 ~11 ~0 ~7 minecraft:cobblestone
fill ~12 ~0 ~0 ~13 ~0 ~0 minecraft:coarse_dirt
setblock ~12 ~0 ~2 minecraft:packed_mud
fill ~12 ~0 ~3 ~12 ~0 ~5 minecraft:coarse_dirt
setblock ~12 ~0 ~6 minecraft:packed_mud
setblock ~12 ~0 ~7 minecraft:coarse_dirt
setblock ~12 ~0 ~8 minecraft:packed_mud
fill ~13 ~0 ~1 ~13 ~0 ~5 minecraft:packed_mud
setblock ~13 ~0 ~6 minecraft:coarse_dirt
setblock ~13 ~0 ~7 minecraft:packed_mud
setblock ~13 ~0 ~8 minecraft:gravel
setblock ~14 ~0 ~1 minecraft:coarse_dirt
setblock ~14 ~0 ~2 minecraft:packed_mud
setblock ~14 ~0 ~3 minecraft:gravel
setblock ~14 ~0 ~4 minecraft:cobblestone
setblock ~14 ~0 ~5 minecraft:coarse_dirt
fill ~14 ~0 ~6 ~15 ~0 ~6 minecraft:gravel
fill ~14 ~0 ~7 ~15 ~0 ~7 minecraft:cobblestone
setblock ~15 ~0 ~0 minecraft:gravel
setblock ~15 ~0 ~1 minecraft:packed_mud
setblock ~15 ~0 ~2 minecraft:cobblestone
setblock ~15 ~0 ~3 minecraft:packed_mud
setblock ~15 ~0 ~4 minecraft:mossy_cobblestone
setblock ~15 ~0 ~5 minecraft:cobblestone
setblock ~16 ~0 ~0 minecraft:mossy_cobblestone
fill ~16 ~0 ~1 ~17 ~0 ~1 minecraft:cobblestone
setblock ~16 ~0 ~2 minecraft:coarse_dirt
fill ~16 ~0 ~3 ~16 ~0 ~4 minecraft:cobblestone
setblock ~16 ~0 ~5 minecraft:gravel
setblock ~16 ~0 ~6 minecraft:packed_mud
setblock ~16 ~0 ~7 minecraft:gravel
setblock ~16 ~0 ~8 minecraft:packed_mud
fill ~17 ~0 ~2 ~17 ~0 ~3 minecraft:cobblestone
setblock ~17 ~0 ~4 minecraft:packed_mud
fill ~17 ~0 ~5 ~17 ~0 ~7 minecraft:coarse_dirt
setblock ~18 ~0 ~1 minecraft:coarse_dirt
setblock ~18 ~0 ~2 minecraft:gravel
setblock ~18 ~0 ~4 minecraft:cobblestone
setblock ~18 ~0 ~8 minecraft:packed_mud

# stall 1: spruce posts at the corners (logs at the back, fences at the front), a ridge pole at y 5 and peaked canvas of brown and light_gray stripes over it, a gray ridge
fill ~0 ~1 ~1 ~0 ~3 ~1 minecraft:spruce_log[axis=y]
fill ~0 ~1 ~7 ~0 ~2 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~1 ~4 ~3 ~1 minecraft:spruce_log[axis=y]
fill ~4 ~1 ~7 ~4 ~2 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~0 ~3 ~0 minecraft:brown_wool
setblock ~0 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~0 ~3 ~8 minecraft:brown_wool
setblock ~1 ~3 ~0 minecraft:light_gray_wool
setblock ~1 ~3 ~8 minecraft:light_gray_wool
setblock ~2 ~3 ~0 minecraft:brown_wool
setblock ~2 ~3 ~8 minecraft:brown_wool
setblock ~3 ~3 ~0 minecraft:light_gray_wool
setblock ~3 ~3 ~8 minecraft:light_gray_wool
setblock ~4 ~3 ~0 minecraft:brown_wool
setblock ~4 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~4 ~3 ~8 minecraft:brown_wool
setblock ~0 ~4 ~1 minecraft:brown_wool
setblock ~0 ~4 ~7 minecraft:brown_wool
setblock ~1 ~4 ~1 minecraft:light_gray_wool
setblock ~1 ~4 ~7 minecraft:light_gray_wool
setblock ~2 ~4 ~1 minecraft:brown_wool
setblock ~2 ~4 ~7 minecraft:brown_wool
setblock ~3 ~4 ~1 minecraft:light_gray_wool
setblock ~3 ~4 ~7 minecraft:light_gray_wool
setblock ~4 ~4 ~1 minecraft:brown_wool
setblock ~4 ~4 ~7 minecraft:brown_wool
fill ~0 ~5 ~2 ~0 ~5 ~3 minecraft:brown_wool
fill ~0 ~5 ~4 ~4 ~5 ~4 minecraft:spruce_log[axis=x]
fill ~0 ~5 ~5 ~0 ~5 ~6 minecraft:brown_wool
fill ~1 ~5 ~2 ~1 ~5 ~3 minecraft:light_gray_wool
fill ~1 ~5 ~5 ~1 ~5 ~6 minecraft:light_gray_wool
fill ~2 ~5 ~2 ~2 ~5 ~3 minecraft:brown_wool
fill ~2 ~5 ~5 ~2 ~5 ~6 minecraft:brown_wool
fill ~3 ~5 ~2 ~3 ~5 ~3 minecraft:light_gray_wool
fill ~3 ~5 ~5 ~3 ~5 ~6 minecraft:light_gray_wool
fill ~4 ~5 ~2 ~4 ~5 ~3 minecraft:brown_wool
fill ~4 ~5 ~5 ~4 ~5 ~6 minecraft:brown_wool
fill ~0 ~6 ~4 ~4 ~6 ~4 minecraft:gray_wool

# stall 1: a counter of slabs at the front, the wares on the back row, a lantern from the ridge pole
fill ~1 ~1 ~6 ~3 ~1 ~6 minecraft:spruce_slab[type=top]
setblock ~2 ~4 ~4 minecraft:lantern[hanging=true]

# stall 2: spruce posts at the corners (logs at the back, fences at the front), a ridge pole at y 5 and peaked canvas of light_gray and gray stripes over it, a brown ridge
fill ~14 ~1 ~1 ~14 ~3 ~1 minecraft:spruce_log[axis=y]
fill ~14 ~1 ~7 ~14 ~2 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~18 ~1 ~1 ~18 ~3 ~1 minecraft:spruce_log[axis=y]
fill ~18 ~1 ~7 ~18 ~2 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~14 ~3 ~0 minecraft:light_gray_wool
setblock ~14 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~14 ~3 ~8 minecraft:light_gray_wool
setblock ~15 ~3 ~0 minecraft:gray_wool
setblock ~15 ~3 ~8 minecraft:gray_wool
setblock ~16 ~3 ~0 minecraft:light_gray_wool
setblock ~16 ~3 ~8 minecraft:light_gray_wool
setblock ~17 ~3 ~0 minecraft:gray_wool
setblock ~17 ~3 ~8 minecraft:gray_wool
setblock ~18 ~3 ~0 minecraft:light_gray_wool
setblock ~18 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~18 ~3 ~8 minecraft:light_gray_wool
setblock ~14 ~4 ~1 minecraft:light_gray_wool
setblock ~14 ~4 ~7 minecraft:light_gray_wool
setblock ~15 ~4 ~1 minecraft:gray_wool
setblock ~15 ~4 ~7 minecraft:gray_wool
setblock ~16 ~4 ~1 minecraft:light_gray_wool
setblock ~16 ~4 ~7 minecraft:light_gray_wool
setblock ~17 ~4 ~1 minecraft:gray_wool
setblock ~17 ~4 ~7 minecraft:gray_wool
setblock ~18 ~4 ~1 minecraft:light_gray_wool
setblock ~18 ~4 ~7 minecraft:light_gray_wool
fill ~14 ~5 ~2 ~14 ~5 ~3 minecraft:light_gray_wool
fill ~14 ~5 ~4 ~18 ~5 ~4 minecraft:spruce_log[axis=x]
fill ~14 ~5 ~5 ~14 ~5 ~6 minecraft:light_gray_wool
fill ~15 ~5 ~2 ~15 ~5 ~3 minecraft:gray_wool
fill ~15 ~5 ~5 ~15 ~5 ~6 minecraft:gray_wool
fill ~16 ~5 ~2 ~16 ~5 ~3 minecraft:light_gray_wool
fill ~16 ~5 ~5 ~16 ~5 ~6 minecraft:light_gray_wool
fill ~17 ~5 ~2 ~17 ~5 ~3 minecraft:gray_wool
fill ~17 ~5 ~5 ~17 ~5 ~6 minecraft:gray_wool
fill ~18 ~5 ~2 ~18 ~5 ~3 minecraft:light_gray_wool
fill ~18 ~5 ~5 ~18 ~5 ~6 minecraft:light_gray_wool
fill ~14 ~6 ~4 ~18 ~6 ~4 minecraft:brown_wool

# stall 2: a counter of slabs at the front, the wares on the back row, a lantern from the ridge pole
fill ~15 ~1 ~6 ~17 ~1 ~6 minecraft:spruce_slab[type=top]
setblock ~16 ~4 ~4 minecraft:lantern[hanging=true]

# stall 1 (produce): crates (barrels), hay, pumpkins and melons on the back row, sacks of grain, a basket on the counter
setblock ~0 ~1 ~2 minecraft:barrel[facing=north]
setblock ~1 ~1 ~2 minecraft:hay_block[axis=y]
setblock ~1 ~1 ~3 minecraft:white_wool
setblock ~2 ~1 ~2 minecraft:barrel[facing=north]
setblock ~3 ~1 ~2 minecraft:pumpkin
setblock ~3 ~1 ~3 minecraft:composter
setblock ~4 ~1 ~2 minecraft:melon
setblock ~1 ~2 ~2 minecraft:hay_block[axis=x]
setblock ~2 ~2 ~2 minecraft:brown_wool
setblock ~2 ~2 ~6 minecraft:flower_pot
setblock ~3 ~2 ~6 minecraft:candle[candles=3,lit=true]

# stall 2 (cloth and wares): a loom, bolts of cloth, barrels, a chest of coin on the back row, folded cloth on the counter
setblock ~14 ~1 ~2 minecraft:barrel[facing=north]
setblock ~15 ~1 ~2 minecraft:loom[facing=south]
setblock ~15 ~1 ~3 minecraft:cauldron
setblock ~16 ~1 ~2 minecraft:chest[facing=south]
setblock ~17 ~1 ~2 minecraft:barrel[facing=north]
setblock ~18 ~1 ~2 minecraft:barrel[facing=up]
setblock ~15 ~2 ~6 minecraft:brown_carpet
setblock ~16 ~2 ~6 minecraft:light_gray_carpet
setblock ~17 ~2 ~2 minecraft:light_gray_wool
setblock ~17 ~2 ~6 minecraft:lantern
setblock ~18 ~2 ~2 minecraft:brown_wool

# the market cross: two steps of mossy stone bricks (x 7..11, z 2..6 and x 8..10, z 3..5), a shaft of stone brick wall from y 3 to y 6 with arms at y 6 and a head at y 7, a lantern under each arm
setblock ~7 ~1 ~2 minecraft:stone_bricks
fill ~7 ~1 ~3 ~7 ~1 ~5 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~7 ~1 ~6 minecraft:stone_bricks
setblock ~8 ~1 ~2 minecraft:stone_brick_stairs[facing=south,half=bottom]
fill ~8 ~1 ~3 ~9 ~1 ~3 minecraft:stone_bricks
fill ~8 ~1 ~4 ~8 ~1 ~5 minecraft:mossy_stone_bricks
fill ~8 ~1 ~6 ~10 ~1 ~6 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~9 ~1 ~2 minecraft:mossy_stone_brick_stairs[facing=south,half=bottom]
fill ~9 ~1 ~4 ~10 ~1 ~5 minecraft:stone_bricks
setblock ~10 ~1 ~2 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~10 ~1 ~3 minecraft:mossy_stone_bricks
setblock ~11 ~1 ~2 minecraft:stone_bricks
fill ~11 ~1 ~3 ~11 ~1 ~5 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~11 ~1 ~6 minecraft:stone_bricks
setblock ~8 ~2 ~3 minecraft:stone_bricks
setblock ~8 ~2 ~4 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~8 ~2 ~5 minecraft:stone_bricks
setblock ~9 ~2 ~3 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~9 ~2 ~4 minecraft:chiseled_stone_bricks
setblock ~9 ~2 ~5 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~10 ~2 ~3 minecraft:stone_bricks
setblock ~10 ~2 ~4 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~10 ~2 ~5 minecraft:stone_bricks
fill ~9 ~3 ~4 ~9 ~5 ~4 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~8 ~5 ~4 minecraft:lantern[hanging=true]
setblock ~10 ~5 ~4 minecraft:lantern[hanging=true]
setblock ~8 ~6 ~4 minecraft:stone_brick_wall[north=none,east=low,south=none,west=none,up=true]
setblock ~9 ~6 ~4 minecraft:stone_brick_wall[north=none,east=tall,south=none,west=tall,up=true]
setblock ~10 ~6 ~4 minecraft:stone_brick_wall[north=none,east=none,south=none,west=low,up=true]
setblock ~9 ~7 ~4 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]

# crates and sacks on the square round the cross
setblock ~7 ~1 ~8 minecraft:hay_block[axis=x]
setblock ~8 ~1 ~0 minecraft:barrel[facing=north]
setblock ~9 ~1 ~0 minecraft:barrel[facing=up]
setblock ~10 ~1 ~8 minecraft:composter
setblock ~11 ~1 ~1 minecraft:brown_wool
setblock ~11 ~1 ~7 minecraft:white_wool
