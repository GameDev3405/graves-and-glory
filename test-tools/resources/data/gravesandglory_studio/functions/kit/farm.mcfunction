# size 27 3 19
# kit/farm: fenced wheat field: ripe wheat on wet farmland in five strips between irrigation ditches, a weathered oak fence with four gates, a scarecrow.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Wheat age 7 at y 1 on farmland (moisture 7) at y 0; water at y 0 where x % 5 == 2; the fence at y 1 on the edge cells with gates at x 12..14 (north, south) and z 8..10 (east, west).

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~8 ~0 ~2 ~10 minecraft:air
fill ~2 ~1 ~1 ~2 ~2 ~17 minecraft:air
fill ~7 ~1 ~1 ~7 ~2 ~17 minecraft:air
fill ~12 ~1 ~0 ~14 ~2 ~0 minecraft:air
fill ~12 ~1 ~1 ~12 ~2 ~18 minecraft:air
fill ~13 ~1 ~18 ~14 ~2 ~18 minecraft:air
fill ~17 ~1 ~1 ~17 ~2 ~17 minecraft:air
fill ~22 ~1 ~1 ~22 ~2 ~17 minecraft:air
fill ~26 ~1 ~8 ~26 ~2 ~10 minecraft:air
fill ~0 ~2 ~0 ~10 ~2 ~0 minecraft:air
fill ~0 ~2 ~1 ~1 ~2 ~7 minecraft:air
fill ~0 ~2 ~11 ~0 ~2 ~18 minecraft:air
fill ~1 ~2 ~8 ~1 ~2 ~18 minecraft:air
setblock ~2 ~2 ~18 minecraft:air
fill ~3 ~2 ~1 ~6 ~2 ~18 minecraft:air
fill ~7 ~2 ~18 ~10 ~2 ~18 minecraft:air
fill ~8 ~2 ~1 ~11 ~2 ~17 minecraft:air
fill ~13 ~2 ~1 ~16 ~2 ~17 minecraft:air
fill ~16 ~2 ~0 ~26 ~2 ~0 minecraft:air
fill ~16 ~2 ~18 ~17 ~2 ~18 minecraft:air
fill ~18 ~2 ~1 ~21 ~2 ~5 minecraft:air
fill ~18 ~2 ~7 ~18 ~2 ~18 minecraft:air
fill ~19 ~2 ~6 ~21 ~2 ~18 minecraft:air
setblock ~22 ~2 ~18 minecraft:air
fill ~23 ~2 ~1 ~26 ~2 ~7 minecraft:air
fill ~23 ~2 ~8 ~25 ~2 ~18 minecraft:air
fill ~26 ~2 ~11 ~26 ~2 ~18 minecraft:air

# the field: farmland under the wheat, the irrigation ditches where x % 5 == 2, a trodden headland of coarse dirt under the fence and in the gates
setblock ~0 ~0 ~0 minecraft:rooted_dirt
setblock ~0 ~0 ~1 minecraft:coarse_dirt
setblock ~0 ~0 ~2 minecraft:packed_mud
setblock ~0 ~0 ~3 minecraft:coarse_dirt
setblock ~0 ~0 ~4 minecraft:packed_mud
setblock ~0 ~0 ~5 minecraft:coarse_dirt
setblock ~0 ~0 ~6 minecraft:packed_mud
setblock ~0 ~0 ~7 minecraft:rooted_dirt
fill ~0 ~0 ~8 ~0 ~0 ~10 minecraft:coarse_dirt
setblock ~0 ~0 ~11 minecraft:packed_mud
fill ~0 ~0 ~12 ~0 ~0 ~13 minecraft:coarse_dirt
setblock ~0 ~0 ~14 minecraft:packed_mud
fill ~0 ~0 ~15 ~0 ~0 ~18 minecraft:coarse_dirt
fill ~1 ~0 ~0 ~3 ~0 ~0 minecraft:coarse_dirt
fill ~1 ~0 ~1 ~1 ~0 ~17 minecraft:farmland[moisture=7]
fill ~1 ~0 ~18 ~12 ~0 ~18 minecraft:coarse_dirt
fill ~2 ~0 ~1 ~2 ~0 ~17 minecraft:water
fill ~3 ~0 ~1 ~6 ~0 ~17 minecraft:farmland[moisture=7]
setblock ~4 ~0 ~0 minecraft:packed_mud
setblock ~5 ~0 ~0 minecraft:rooted_dirt
setblock ~6 ~0 ~0 minecraft:packed_mud
fill ~7 ~0 ~0 ~10 ~0 ~0 minecraft:coarse_dirt
fill ~7 ~0 ~1 ~7 ~0 ~17 minecraft:water
fill ~8 ~0 ~1 ~11 ~0 ~17 minecraft:farmland[moisture=7]
setblock ~11 ~0 ~0 minecraft:rooted_dirt
fill ~12 ~0 ~0 ~13 ~0 ~0 minecraft:coarse_dirt
fill ~12 ~0 ~1 ~12 ~0 ~17 minecraft:water
fill ~13 ~0 ~1 ~16 ~0 ~17 minecraft:farmland[moisture=7]
setblock ~13 ~0 ~18 minecraft:rooted_dirt
setblock ~14 ~0 ~0 minecraft:packed_mud
setblock ~14 ~0 ~18 minecraft:packed_mud
fill ~15 ~0 ~0 ~22 ~0 ~0 minecraft:coarse_dirt
fill ~15 ~0 ~18 ~17 ~0 ~18 minecraft:coarse_dirt
fill ~17 ~0 ~1 ~17 ~0 ~17 minecraft:water
fill ~18 ~0 ~1 ~21 ~0 ~5 minecraft:farmland[moisture=7]
fill ~18 ~0 ~7 ~18 ~0 ~17 minecraft:farmland[moisture=7]
setblock ~18 ~0 ~18 minecraft:rooted_dirt
fill ~19 ~0 ~6 ~21 ~0 ~17 minecraft:farmland[moisture=7]
fill ~19 ~0 ~18 ~25 ~0 ~18 minecraft:coarse_dirt
fill ~22 ~0 ~1 ~22 ~0 ~17 minecraft:water
fill ~23 ~0 ~0 ~24 ~0 ~0 minecraft:rooted_dirt
fill ~23 ~0 ~1 ~25 ~0 ~11 minecraft:farmland[moisture=7]
fill ~23 ~0 ~12 ~23 ~0 ~17 minecraft:farmland[moisture=7]
fill ~24 ~0 ~14 ~25 ~0 ~17 minecraft:farmland[moisture=7]
fill ~25 ~0 ~0 ~26 ~0 ~0 minecraft:coarse_dirt
setblock ~25 ~0 ~12 minecraft:farmland[moisture=7]
fill ~26 ~0 ~1 ~26 ~0 ~3 minecraft:coarse_dirt
setblock ~26 ~0 ~4 minecraft:packed_mud
setblock ~26 ~0 ~5 minecraft:coarse_dirt
setblock ~26 ~0 ~6 minecraft:packed_mud
setblock ~26 ~0 ~7 minecraft:rooted_dirt
setblock ~26 ~0 ~8 minecraft:packed_mud
setblock ~26 ~0 ~9 minecraft:coarse_dirt
setblock ~26 ~0 ~10 minecraft:packed_mud
fill ~26 ~0 ~11 ~26 ~0 ~12 minecraft:coarse_dirt
setblock ~26 ~0 ~13 minecraft:rooted_dirt
setblock ~26 ~0 ~14 minecraft:coarse_dirt
setblock ~26 ~0 ~15 minecraft:packed_mud
fill ~26 ~0 ~16 ~26 ~0 ~17 minecraft:coarse_dirt
setblock ~26 ~0 ~18 minecraft:packed_mud

# the wheat: ripe, age 7, on every farmland cell; a few cells trampled bare along the middle path
fill ~1 ~1 ~1 ~1 ~1 ~17 minecraft:wheat[age=7]
fill ~3 ~1 ~1 ~6 ~1 ~1 minecraft:wheat[age=7]
fill ~3 ~1 ~2 ~4 ~1 ~4 minecraft:wheat[age=7]
fill ~3 ~1 ~5 ~3 ~1 ~13 minecraft:wheat[age=7]
setblock ~3 ~1 ~14 minecraft:wheat[age=6]
fill ~3 ~1 ~15 ~3 ~1 ~17 minecraft:wheat[age=7]
setblock ~4 ~1 ~5 minecraft:wheat[age=6]
fill ~4 ~1 ~6 ~4 ~1 ~11 minecraft:wheat[age=7]
setblock ~4 ~1 ~12 minecraft:wheat[age=6]
fill ~4 ~1 ~13 ~5 ~1 ~14 minecraft:wheat[age=7]
setblock ~4 ~1 ~15 minecraft:wheat[age=5]
fill ~4 ~1 ~16 ~4 ~1 ~17 minecraft:wheat[age=7]
setblock ~5 ~1 ~2 minecraft:wheat[age=4]
fill ~5 ~1 ~3 ~5 ~1 ~11 minecraft:wheat[age=7]
setblock ~5 ~1 ~12 minecraft:wheat[age=3]
fill ~5 ~1 ~15 ~5 ~1 ~17 minecraft:wheat[age=7]
fill ~6 ~1 ~2 ~6 ~1 ~6 minecraft:wheat[age=7]
setblock ~6 ~1 ~7 minecraft:wheat[age=5]
fill ~6 ~1 ~8 ~6 ~1 ~17 minecraft:wheat[age=7]
setblock ~8 ~1 ~1 minecraft:wheat[age=5]
fill ~8 ~1 ~2 ~8 ~1 ~17 minecraft:wheat[age=7]
fill ~9 ~1 ~1 ~11 ~1 ~6 minecraft:wheat[age=7]
fill ~9 ~1 ~7 ~9 ~1 ~13 minecraft:wheat[age=7]
setblock ~9 ~1 ~14 minecraft:wheat[age=5]
fill ~9 ~1 ~15 ~9 ~1 ~17 minecraft:wheat[age=7]
setblock ~10 ~1 ~7 minecraft:wheat[age=4]
fill ~10 ~1 ~8 ~10 ~1 ~17 minecraft:wheat[age=7]
fill ~11 ~1 ~7 ~11 ~1 ~17 minecraft:wheat[age=7]
fill ~13 ~1 ~1 ~16 ~1 ~1 minecraft:wheat[age=7]
fill ~13 ~1 ~2 ~14 ~1 ~3 minecraft:wheat[age=7]
setblock ~13 ~1 ~4 minecraft:wheat[age=3]
fill ~13 ~1 ~5 ~13 ~1 ~17 minecraft:wheat[age=7]
fill ~14 ~1 ~4 ~14 ~1 ~7 minecraft:wheat[age=7]
setblock ~14 ~1 ~8 minecraft:wheat[age=3]
fill ~14 ~1 ~9 ~14 ~1 ~17 minecraft:wheat[age=7]
setblock ~15 ~1 ~2 minecraft:wheat[age=6]
fill ~15 ~1 ~3 ~15 ~1 ~8 minecraft:wheat[age=7]
setblock ~15 ~1 ~9 minecraft:wheat[age=5]
fill ~15 ~1 ~10 ~15 ~1 ~16 minecraft:wheat[age=7]
setblock ~15 ~1 ~17 minecraft:wheat[age=4]
fill ~16 ~1 ~2 ~16 ~1 ~14 minecraft:wheat[age=7]
setblock ~16 ~1 ~15 minecraft:wheat[age=5]
fill ~16 ~1 ~16 ~16 ~1 ~17 minecraft:wheat[age=7]
fill ~18 ~1 ~1 ~21 ~1 ~5 minecraft:wheat[age=7]
fill ~18 ~1 ~7 ~18 ~1 ~10 minecraft:wheat[age=7]
setblock ~18 ~1 ~11 minecraft:wheat[age=5]
fill ~18 ~1 ~12 ~18 ~1 ~17 minecraft:wheat[age=7]
fill ~19 ~1 ~6 ~21 ~1 ~12 minecraft:wheat[age=7]
fill ~19 ~1 ~13 ~19 ~1 ~17 minecraft:wheat[age=7]
setblock ~20 ~1 ~13 minecraft:wheat[age=6]
fill ~20 ~1 ~14 ~20 ~1 ~17 minecraft:wheat[age=7]
fill ~21 ~1 ~13 ~21 ~1 ~17 minecraft:wheat[age=7]
fill ~23 ~1 ~1 ~25 ~1 ~4 minecraft:wheat[age=7]
setblock ~23 ~1 ~5 minecraft:wheat[age=4]
fill ~23 ~1 ~6 ~23 ~1 ~12 minecraft:wheat[age=7]
setblock ~23 ~1 ~13 minecraft:wheat[age=4]
fill ~23 ~1 ~14 ~25 ~1 ~17 minecraft:wheat[age=7]
fill ~24 ~1 ~5 ~25 ~1 ~6 minecraft:wheat[age=7]
setblock ~24 ~1 ~7 minecraft:wheat[age=6]
fill ~24 ~1 ~8 ~24 ~1 ~11 minecraft:wheat[age=7]
fill ~25 ~1 ~7 ~25 ~1 ~12 minecraft:wheat[age=7]

# the fence: weathered oak round the edge; the gate posts are stripped oak logs with a lantern on each north and south pair
setblock ~0 ~1 ~0 minecraft:stripped_oak_log[axis=y]
fill ~0 ~1 ~1 ~0 ~1 ~4 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~0 ~1 ~5 minecraft:spruce_fence[north=true,east=false,south=true,west=false]
setblock ~0 ~1 ~6 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~0 ~1 ~7 minecraft:stripped_oak_log[axis=y]
setblock ~0 ~1 ~11 minecraft:stripped_oak_log[axis=y]
fill ~0 ~1 ~12 ~0 ~1 ~17 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~0 ~1 ~18 minecraft:stripped_oak_log[axis=y]
fill ~1 ~1 ~0 ~10 ~1 ~0 minecraft:oak_fence[north=false,east=true,south=false,west=true]
fill ~1 ~1 ~18 ~10 ~1 ~18 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~11 ~1 ~0 minecraft:stripped_oak_log[axis=y]
setblock ~11 ~1 ~18 minecraft:stripped_oak_log[axis=y]
setblock ~15 ~1 ~0 minecraft:stripped_oak_log[axis=y]
setblock ~15 ~1 ~18 minecraft:stripped_oak_log[axis=y]
fill ~16 ~1 ~0 ~25 ~1 ~0 minecraft:oak_fence[north=false,east=true,south=false,west=true]
fill ~16 ~1 ~18 ~25 ~1 ~18 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~26 ~1 ~0 minecraft:stripped_oak_log[axis=y]
fill ~26 ~1 ~1 ~26 ~1 ~2 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~26 ~1 ~3 minecraft:spruce_fence[north=true,east=false,south=true,west=false]
fill ~26 ~1 ~4 ~26 ~1 ~6 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~26 ~1 ~7 minecraft:stripped_oak_log[axis=y]
setblock ~26 ~1 ~11 minecraft:stripped_oak_log[axis=y]
setblock ~26 ~1 ~12 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~26 ~1 ~13 minecraft:oak_fence[north=true,east=false,south=true,west=true]
setblock ~26 ~1 ~14 minecraft:spruce_fence[north=true,east=false,south=true,west=false]
fill ~26 ~1 ~15 ~26 ~1 ~17 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~26 ~1 ~18 minecraft:stripped_oak_log[axis=y]
setblock ~11 ~2 ~0 minecraft:lantern
setblock ~11 ~2 ~18 minecraft:lantern
setblock ~15 ~2 ~0 minecraft:lantern
setblock ~15 ~2 ~18 minecraft:lantern

# the scarecrow in the middle strip: a fence post with a carved pumpkin head; a few hay bales and a composter by the east gate
setblock ~18 ~0 ~6 minecraft:coarse_dirt
fill ~24 ~0 ~12 ~24 ~0 ~13 minecraft:coarse_dirt
setblock ~25 ~0 ~13 minecraft:coarse_dirt
setblock ~18 ~1 ~6 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~24 ~1 ~12 minecraft:hay_block[axis=y]
setblock ~24 ~1 ~13 minecraft:composter
setblock ~25 ~1 ~13 minecraft:hay_block[axis=x]
setblock ~18 ~2 ~6 minecraft:carved_pumpkin[facing=south]
