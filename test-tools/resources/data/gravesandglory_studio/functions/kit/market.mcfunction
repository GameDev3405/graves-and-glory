# size 19 6 9
# kit/market: market stalls: three timber stalls under white, cyan and red awnings on a worn cobble square, walk-throughs between them.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Stalls at x 0..4, 7..11 and 14..18; the walk-throughs at x 5..6 and 12..13 are open from y 1 to 5 for the whole depth.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~18 ~5 ~0 minecraft:air
fill ~0 ~1 ~3 ~4 ~2 ~5 minecraft:air
fill ~0 ~1 ~6 ~0 ~3 ~6 minecraft:air
fill ~0 ~1 ~8 ~0 ~5 ~8 minecraft:air
fill ~1 ~1 ~7 ~3 ~3 ~8 minecraft:air
fill ~4 ~1 ~6 ~4 ~3 ~6 minecraft:air
fill ~4 ~1 ~8 ~4 ~5 ~8 minecraft:air
fill ~5 ~1 ~1 ~6 ~5 ~8 minecraft:air
fill ~7 ~1 ~3 ~11 ~2 ~5 minecraft:air
fill ~7 ~1 ~6 ~7 ~3 ~6 minecraft:air
fill ~7 ~1 ~8 ~7 ~5 ~8 minecraft:air
fill ~8 ~1 ~7 ~10 ~3 ~8 minecraft:air
fill ~11 ~1 ~6 ~11 ~3 ~6 minecraft:air
fill ~11 ~1 ~8 ~11 ~5 ~8 minecraft:air
fill ~12 ~1 ~1 ~13 ~5 ~8 minecraft:air
fill ~14 ~1 ~3 ~18 ~2 ~5 minecraft:air
fill ~14 ~1 ~6 ~14 ~3 ~6 minecraft:air
fill ~14 ~1 ~8 ~14 ~5 ~8 minecraft:air
fill ~15 ~1 ~7 ~17 ~3 ~8 minecraft:air
fill ~18 ~1 ~6 ~18 ~3 ~6 minecraft:air
fill ~18 ~1 ~8 ~18 ~5 ~8 minecraft:air
fill ~0 ~2 ~2 ~0 ~3 ~2 minecraft:air
fill ~1 ~2 ~1 ~3 ~3 ~1 minecraft:air
fill ~1 ~2 ~6 ~1 ~3 ~6 minecraft:air
fill ~2 ~2 ~2 ~4 ~3 ~2 minecraft:air
fill ~3 ~2 ~6 ~3 ~3 ~6 minecraft:air
fill ~7 ~2 ~2 ~7 ~3 ~2 minecraft:air
fill ~8 ~2 ~1 ~10 ~3 ~1 minecraft:air
fill ~8 ~2 ~6 ~8 ~3 ~6 minecraft:air
fill ~9 ~2 ~2 ~11 ~3 ~2 minecraft:air
fill ~10 ~2 ~6 ~10 ~3 ~6 minecraft:air
fill ~14 ~2 ~2 ~14 ~3 ~2 minecraft:air
fill ~15 ~2 ~1 ~17 ~3 ~1 minecraft:air
fill ~15 ~2 ~6 ~15 ~3 ~6 minecraft:air
fill ~16 ~2 ~2 ~18 ~3 ~2 minecraft:air
fill ~17 ~2 ~6 ~17 ~3 ~6 minecraft:air
fill ~0 ~3 ~4 ~0 ~3 ~5 minecraft:air
fill ~1 ~3 ~2 ~1 ~3 ~5 minecraft:air
fill ~2 ~3 ~3 ~4 ~3 ~3 minecraft:air
fill ~2 ~3 ~5 ~2 ~3 ~6 minecraft:air
fill ~3 ~3 ~4 ~4 ~3 ~5 minecraft:air
fill ~7 ~3 ~3 ~7 ~3 ~5 minecraft:air
fill ~8 ~3 ~2 ~8 ~3 ~5 minecraft:air
fill ~9 ~3 ~3 ~11 ~3 ~3 minecraft:air
fill ~9 ~3 ~5 ~9 ~3 ~6 minecraft:air
fill ~10 ~3 ~4 ~11 ~3 ~5 minecraft:air
fill ~14 ~3 ~3 ~14 ~3 ~5 minecraft:air
fill ~15 ~3 ~2 ~15 ~3 ~5 minecraft:air
fill ~16 ~3 ~3 ~18 ~3 ~3 minecraft:air
fill ~16 ~3 ~5 ~16 ~3 ~6 minecraft:air
fill ~17 ~3 ~4 ~17 ~3 ~5 minecraft:air
setblock ~18 ~3 ~5 minecraft:air
fill ~1 ~4 ~8 ~3 ~5 ~8 minecraft:air
fill ~8 ~4 ~8 ~10 ~5 ~8 minecraft:air
fill ~15 ~4 ~8 ~17 ~5 ~8 minecraft:air
fill ~0 ~5 ~3 ~4 ~5 ~7 minecraft:air
fill ~7 ~5 ~3 ~11 ~5 ~7 minecraft:air
fill ~14 ~5 ~3 ~18 ~5 ~7 minecraft:air

# the square: worn cobble, gravel and packed earth, the walk-throughs trodden to dirt
fill ~0 ~0 ~0 ~1 ~0 ~0 minecraft:mossy_cobblestone
setblock ~0 ~0 ~1 minecraft:coarse_dirt
setblock ~0 ~0 ~2 minecraft:packed_mud
setblock ~0 ~0 ~3 minecraft:gravel
setblock ~0 ~0 ~4 minecraft:mossy_cobblestone
fill ~0 ~0 ~5 ~0 ~0 ~7 minecraft:gravel
setblock ~0 ~0 ~8 minecraft:coarse_dirt
fill ~1 ~0 ~1 ~1 ~0 ~2 minecraft:packed_mud
setblock ~1 ~0 ~3 minecraft:cobblestone
setblock ~1 ~0 ~4 minecraft:gravel
setblock ~1 ~0 ~5 minecraft:packed_mud
fill ~1 ~0 ~6 ~1 ~0 ~7 minecraft:coarse_dirt
setblock ~1 ~0 ~8 minecraft:cobblestone
setblock ~2 ~0 ~0 minecraft:packed_mud
fill ~2 ~0 ~1 ~2 ~0 ~3 minecraft:coarse_dirt
fill ~2 ~0 ~4 ~2 ~0 ~7 minecraft:cobblestone
setblock ~2 ~0 ~8 minecraft:gravel
fill ~3 ~0 ~0 ~3 ~0 ~2 minecraft:cobblestone
setblock ~3 ~0 ~3 minecraft:gravel
setblock ~3 ~0 ~4 minecraft:coarse_dirt
fill ~3 ~0 ~5 ~3 ~0 ~7 minecraft:cobblestone
setblock ~3 ~0 ~8 minecraft:mossy_cobblestone
fill ~4 ~0 ~0 ~4 ~0 ~2 minecraft:gravel
setblock ~4 ~0 ~3 minecraft:cobblestone
fill ~4 ~0 ~4 ~4 ~0 ~5 minecraft:packed_mud
setblock ~4 ~0 ~6 minecraft:mossy_cobblestone
setblock ~4 ~0 ~7 minecraft:packed_mud
fill ~4 ~0 ~8 ~5 ~0 ~8 minecraft:gravel
setblock ~5 ~0 ~0 minecraft:packed_mud
setblock ~5 ~0 ~1 minecraft:coarse_dirt
fill ~5 ~0 ~2 ~6 ~0 ~2 minecraft:gravel
fill ~5 ~0 ~3 ~5 ~0 ~4 minecraft:coarse_dirt
fill ~5 ~0 ~5 ~5 ~0 ~6 minecraft:gravel
setblock ~5 ~0 ~7 minecraft:coarse_dirt
fill ~6 ~0 ~0 ~6 ~0 ~1 minecraft:coarse_dirt
setblock ~6 ~0 ~3 minecraft:gravel
fill ~6 ~0 ~4 ~6 ~0 ~5 minecraft:coarse_dirt
setblock ~6 ~0 ~6 minecraft:packed_mud
setblock ~6 ~0 ~7 minecraft:gravel
setblock ~6 ~0 ~8 minecraft:coarse_dirt
setblock ~7 ~0 ~0 minecraft:cobblestone
fill ~7 ~0 ~1 ~7 ~0 ~3 minecraft:packed_mud
fill ~7 ~0 ~4 ~8 ~0 ~4 minecraft:gravel
setblock ~7 ~0 ~5 minecraft:mossy_cobblestone
fill ~7 ~0 ~6 ~9 ~0 ~6 minecraft:cobblestone
fill ~7 ~0 ~7 ~8 ~0 ~7 minecraft:coarse_dirt
fill ~7 ~0 ~8 ~8 ~0 ~8 minecraft:packed_mud
fill ~8 ~0 ~0 ~9 ~0 ~0 minecraft:packed_mud
setblock ~8 ~0 ~1 minecraft:gravel
setblock ~8 ~0 ~2 minecraft:coarse_dirt
setblock ~8 ~0 ~3 minecraft:packed_mud
setblock ~8 ~0 ~5 minecraft:coarse_dirt
setblock ~9 ~0 ~1 minecraft:mossy_cobblestone
fill ~9 ~0 ~2 ~10 ~0 ~2 minecraft:gravel
fill ~9 ~0 ~3 ~9 ~0 ~4 minecraft:cobblestone
fill ~9 ~0 ~5 ~10 ~0 ~5 minecraft:gravel
fill ~9 ~0 ~7 ~9 ~0 ~8 minecraft:packed_mud
setblock ~10 ~0 ~0 minecraft:cobblestone
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
fill ~11 ~0 ~6 ~11 ~0 ~8 minecraft:cobblestone
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
setblock ~14 ~0 ~0 minecraft:packed_mud
setblock ~14 ~0 ~1 minecraft:coarse_dirt
setblock ~14 ~0 ~2 minecraft:packed_mud
setblock ~14 ~0 ~3 minecraft:gravel
setblock ~14 ~0 ~4 minecraft:cobblestone
setblock ~14 ~0 ~5 minecraft:coarse_dirt
fill ~14 ~0 ~6 ~15 ~0 ~6 minecraft:gravel
fill ~14 ~0 ~7 ~15 ~0 ~8 minecraft:cobblestone
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
setblock ~17 ~0 ~0 minecraft:packed_mud
fill ~17 ~0 ~2 ~17 ~0 ~3 minecraft:cobblestone
setblock ~17 ~0 ~4 minecraft:packed_mud
fill ~17 ~0 ~5 ~17 ~0 ~7 minecraft:coarse_dirt
setblock ~17 ~0 ~8 minecraft:gravel
setblock ~18 ~0 ~0 minecraft:gravel
setblock ~18 ~0 ~1 minecraft:coarse_dirt
setblock ~18 ~0 ~2 minecraft:gravel
setblock ~18 ~0 ~3 minecraft:packed_mud
fill ~18 ~0 ~4 ~18 ~0 ~5 minecraft:cobblestone
setblock ~18 ~0 ~6 minecraft:mossy_cobblestone
fill ~18 ~0 ~7 ~18 ~0 ~8 minecraft:packed_mud

# stall 1: spruce posts (the back ones taller), a counter of slabs at the front with the wares on a back bench, a white awning that falls to the front
fill ~0 ~1 ~1 ~0 ~3 ~1 minecraft:spruce_log[axis=y]
setblock ~0 ~1 ~2 minecraft:barrel[facing=up]
fill ~0 ~1 ~7 ~0 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~1 ~1 ~1 ~3 ~1 ~1 minecraft:spruce_planks
setblock ~1 ~1 ~2 minecraft:hay_block[axis=y]
fill ~1 ~1 ~6 ~3 ~1 ~6 minecraft:spruce_slab[type=top]
setblock ~2 ~1 ~2 minecraft:composter
setblock ~3 ~1 ~2 minecraft:barrel[facing=up]
fill ~4 ~1 ~1 ~4 ~3 ~1 minecraft:spruce_log[axis=y]
setblock ~4 ~1 ~2 minecraft:pumpkin
fill ~4 ~1 ~7 ~4 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~1 ~2 ~2 minecraft:candle[candles=3,lit=true]
setblock ~2 ~2 ~6 minecraft:flower_pot
setblock ~2 ~3 ~4 minecraft:lantern[hanging=true]
fill ~0 ~4 ~1 ~4 ~4 ~2 minecraft:spruce_planks
fill ~0 ~4 ~3 ~4 ~4 ~3 minecraft:white_wool
fill ~0 ~4 ~4 ~0 ~4 ~7 minecraft:white_wool
setblock ~1 ~4 ~4 minecraft:light_gray_wool
setblock ~1 ~4 ~5 minecraft:white_wool
fill ~1 ~4 ~6 ~1 ~4 ~7 minecraft:light_gray_wool
fill ~2 ~4 ~4 ~2 ~4 ~7 minecraft:white_wool
setblock ~3 ~4 ~4 minecraft:light_gray_wool
setblock ~3 ~4 ~5 minecraft:white_wool
fill ~3 ~4 ~6 ~3 ~4 ~7 minecraft:light_gray_wool
fill ~4 ~4 ~4 ~4 ~4 ~7 minecraft:white_wool
fill ~0 ~5 ~1 ~4 ~5 ~2 minecraft:white_wool

# stall 2: spruce posts (the back ones taller), a counter of slabs at the front with the wares on a back bench, a cyan awning that falls to the front
fill ~7 ~1 ~1 ~7 ~3 ~1 minecraft:spruce_log[axis=y]
fill ~7 ~1 ~2 ~8 ~1 ~2 minecraft:barrel[facing=up]
fill ~7 ~1 ~7 ~7 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~8 ~1 ~1 ~10 ~1 ~1 minecraft:spruce_planks
fill ~8 ~1 ~6 ~10 ~1 ~6 minecraft:spruce_slab[type=top]
setblock ~9 ~1 ~2 minecraft:chest[facing=south]
setblock ~10 ~1 ~2 minecraft:cauldron
fill ~11 ~1 ~1 ~11 ~3 ~1 minecraft:spruce_log[axis=y]
setblock ~11 ~1 ~2 minecraft:smithing_table
fill ~11 ~1 ~7 ~11 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~8 ~2 ~2 minecraft:candle[candles=3,lit=true]
setblock ~9 ~2 ~6 minecraft:lantern
setblock ~9 ~3 ~4 minecraft:lantern[hanging=true]
fill ~7 ~4 ~1 ~11 ~4 ~2 minecraft:spruce_planks
fill ~7 ~4 ~3 ~11 ~4 ~3 minecraft:cyan_wool
fill ~7 ~4 ~4 ~7 ~4 ~7 minecraft:cyan_wool
setblock ~8 ~4 ~4 minecraft:light_gray_wool
setblock ~8 ~4 ~5 minecraft:cyan_wool
fill ~8 ~4 ~6 ~8 ~4 ~7 minecraft:light_gray_wool
fill ~9 ~4 ~4 ~9 ~4 ~7 minecraft:cyan_wool
setblock ~10 ~4 ~4 minecraft:light_gray_wool
setblock ~10 ~4 ~5 minecraft:cyan_wool
fill ~10 ~4 ~6 ~10 ~4 ~7 minecraft:light_gray_wool
fill ~11 ~4 ~4 ~11 ~4 ~7 minecraft:cyan_wool
fill ~7 ~5 ~1 ~11 ~5 ~2 minecraft:cyan_wool

# stall 3: spruce posts (the back ones taller), a counter of slabs at the front with the wares on a back bench, a red awning that falls to the front
fill ~14 ~1 ~1 ~14 ~3 ~1 minecraft:spruce_log[axis=y]
setblock ~14 ~1 ~2 minecraft:melon
fill ~14 ~1 ~7 ~14 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~15 ~1 ~1 ~17 ~1 ~1 minecraft:spruce_planks
fill ~15 ~1 ~2 ~15 ~2 ~2 minecraft:barrel[facing=up]
fill ~15 ~1 ~6 ~17 ~1 ~6 minecraft:spruce_slab[type=top]
setblock ~16 ~1 ~2 minecraft:hay_block[axis=x]
setblock ~17 ~1 ~2 minecraft:barrel[facing=up]
fill ~18 ~1 ~1 ~18 ~3 ~1 minecraft:spruce_log[axis=y]
setblock ~18 ~1 ~2 minecraft:carved_pumpkin[facing=south]
fill ~18 ~1 ~7 ~18 ~3 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~16 ~2 ~6 minecraft:flower_pot
setblock ~16 ~3 ~4 minecraft:lantern[hanging=true]
fill ~14 ~4 ~1 ~18 ~4 ~2 minecraft:spruce_planks
fill ~14 ~4 ~3 ~18 ~4 ~3 minecraft:red_wool
fill ~14 ~4 ~4 ~14 ~4 ~6 minecraft:red_wool
setblock ~15 ~4 ~4 minecraft:brown_wool
setblock ~15 ~4 ~5 minecraft:red_wool
fill ~15 ~4 ~6 ~15 ~4 ~7 minecraft:brown_wool
fill ~16 ~4 ~4 ~16 ~4 ~7 minecraft:red_wool
setblock ~17 ~4 ~4 minecraft:brown_wool
setblock ~17 ~4 ~5 minecraft:red_wool
fill ~17 ~4 ~6 ~17 ~4 ~7 minecraft:brown_wool
fill ~18 ~4 ~4 ~18 ~4 ~7 minecraft:red_wool
fill ~14 ~5 ~1 ~18 ~5 ~2 minecraft:red_wool

# weather on the awnings: a few rotten patches (cobwebs under the cloth, a torn corner)
setblock ~0 ~3 ~3 minecraft:cobweb
setblock ~18 ~3 ~4 minecraft:cobweb
setblock ~14 ~4 ~7 minecraft:air
