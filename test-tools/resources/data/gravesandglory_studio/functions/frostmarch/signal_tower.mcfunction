# size 15 21 15
# frostmarch/signal_tower: signal tower of the Frostmarch pass: a battered grey stone shaft with arrow slits, an inner stair to a corbelled platform, a crenellated parapet and the signal fire on netherrack in its middle, a dark oak door and lanterns on the road side.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Shaft x 3..11, z 3..11, y 1..17 (walls one block thick), interior x 4..10, z 4..10; footing x 2..12, y 1..2. Doorway north x 6..8, y 1..3. Inner stair: stone brick stairs along the inner wall from (9, 1, 4) clockwise, one block up per cell, the last at (4, 18, 6); hatch (4, 18, 7..9), the last stair (4, 18, 6) flush with the floor.
# Platform floor y 18 over x 2..12, z 2..12 on corbels at y 17; parapet y 19 (wall) and y 20 (merlons) on the ring x 2..12, z 2..12. Signal fire: netherrack (7, 19, 7), fire (7, 20, 7), cobblestone walls (6, 19, 7), (8, 19, 7), (7, 19, 6), (7, 19, 8).

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~0 ~20 ~0 minecraft:air
fill ~0 ~1 ~5 ~0 ~20 ~6 minecraft:air
fill ~0 ~1 ~8 ~0 ~20 ~9 minecraft:air
fill ~0 ~1 ~12 ~0 ~20 ~12 minecraft:air
fill ~0 ~1 ~14 ~0 ~20 ~14 minecraft:air
fill ~1 ~1 ~2 ~1 ~20 ~2 minecraft:air
fill ~1 ~1 ~4 ~1 ~20 ~4 minecraft:air
fill ~1 ~1 ~7 ~1 ~20 ~14 minecraft:air
fill ~2 ~1 ~0 ~2 ~20 ~1 minecraft:air
fill ~2 ~1 ~14 ~2 ~20 ~14 minecraft:air
fill ~3 ~1 ~1 ~3 ~20 ~1 minecraft:air
fill ~4 ~1 ~4 ~8 ~17 ~4 minecraft:air
fill ~4 ~1 ~5 ~4 ~17 ~5 minecraft:air
fill ~4 ~1 ~13 ~4 ~20 ~13 minecraft:air
fill ~5 ~1 ~0 ~9 ~20 ~0 minecraft:air
fill ~5 ~1 ~7 ~5 ~17 ~9 minecraft:air
fill ~6 ~1 ~1 ~8 ~20 ~1 minecraft:air
fill ~6 ~1 ~5 ~8 ~13 ~9 minecraft:air
fill ~6 ~1 ~13 ~7 ~20 ~13 minecraft:air
fill ~6 ~1 ~14 ~6 ~20 ~14 minecraft:air
fill ~8 ~1 ~14 ~8 ~20 ~14 minecraft:air
fill ~9 ~1 ~6 ~9 ~17 ~9 minecraft:air
fill ~9 ~1 ~13 ~9 ~20 ~14 minecraft:air
fill ~10 ~1 ~1 ~12 ~20 ~1 minecraft:air
fill ~10 ~1 ~14 ~10 ~20 ~14 minecraft:air
fill ~12 ~1 ~13 ~12 ~20 ~13 minecraft:air
fill ~13 ~1 ~0 ~14 ~20 ~1 minecraft:air
fill ~13 ~1 ~3 ~13 ~20 ~3 minecraft:air
fill ~13 ~1 ~6 ~13 ~20 ~6 minecraft:air
fill ~13 ~1 ~8 ~14 ~20 ~8 minecraft:air
fill ~13 ~1 ~11 ~13 ~20 ~11 minecraft:air
fill ~13 ~1 ~14 ~13 ~20 ~14 minecraft:air
fill ~14 ~1 ~2 ~14 ~20 ~2 minecraft:air
fill ~14 ~1 ~9 ~14 ~20 ~9 minecraft:air
fill ~14 ~1 ~13 ~14 ~20 ~13 minecraft:air
fill ~0 ~2 ~1 ~0 ~20 ~4 minecraft:air
fill ~0 ~2 ~7 ~0 ~20 ~7 minecraft:air
fill ~0 ~2 ~10 ~0 ~20 ~11 minecraft:air
fill ~0 ~2 ~13 ~0 ~20 ~13 minecraft:air
fill ~1 ~2 ~0 ~1 ~20 ~1 minecraft:air
fill ~1 ~2 ~3 ~1 ~20 ~3 minecraft:air
fill ~1 ~2 ~5 ~1 ~20 ~6 minecraft:air
fill ~2 ~2 ~13 ~3 ~20 ~13 minecraft:air
fill ~3 ~2 ~0 ~4 ~20 ~0 minecraft:air
fill ~3 ~2 ~14 ~4 ~20 ~14 minecraft:air
fill ~4 ~2 ~1 ~4 ~20 ~1 minecraft:air
fill ~5 ~2 ~6 ~5 ~17 ~6 minecraft:air
fill ~5 ~2 ~13 ~5 ~20 ~14 minecraft:air
fill ~7 ~2 ~14 ~7 ~20 ~14 minecraft:air
fill ~8 ~2 ~13 ~8 ~20 ~13 minecraft:air
fill ~9 ~2 ~4 ~9 ~17 ~5 minecraft:air
fill ~10 ~2 ~0 ~12 ~20 ~0 minecraft:air
fill ~10 ~2 ~13 ~11 ~20 ~13 minecraft:air
fill ~11 ~2 ~14 ~12 ~20 ~14 minecraft:air
fill ~13 ~2 ~2 ~13 ~20 ~2 minecraft:air
fill ~13 ~2 ~4 ~13 ~20 ~5 minecraft:air
fill ~13 ~2 ~7 ~13 ~20 ~7 minecraft:air
fill ~13 ~2 ~9 ~13 ~20 ~10 minecraft:air
fill ~13 ~2 ~12 ~13 ~20 ~13 minecraft:air
fill ~14 ~2 ~3 ~14 ~20 ~7 minecraft:air
fill ~14 ~2 ~10 ~14 ~20 ~12 minecraft:air
fill ~14 ~2 ~14 ~14 ~20 ~14 minecraft:air
fill ~2 ~3 ~2 ~12 ~5 ~2 minecraft:air
fill ~2 ~3 ~3 ~2 ~16 ~12 minecraft:air
fill ~3 ~3 ~12 ~11 ~16 ~12 minecraft:air
fill ~5 ~3 ~5 ~5 ~17 ~5 minecraft:air
fill ~10 ~3 ~4 ~10 ~17 ~4 minecraft:air
fill ~12 ~3 ~3 ~12 ~16 ~12 minecraft:air
fill ~5 ~4 ~1 ~5 ~20 ~1 minecraft:air
fill ~9 ~4 ~1 ~9 ~20 ~1 minecraft:air
fill ~10 ~4 ~5 ~10 ~17 ~5 minecraft:air
fill ~10 ~5 ~6 ~10 ~17 ~6 minecraft:air
fill ~2 ~6 ~2 ~5 ~16 ~2 minecraft:air
fill ~7 ~6 ~2 ~7 ~16 ~2 minecraft:air
fill ~9 ~6 ~2 ~12 ~16 ~2 minecraft:air
fill ~10 ~6 ~7 ~10 ~17 ~7 minecraft:air
fill ~6 ~7 ~2 ~6 ~16 ~2 minecraft:air
fill ~8 ~7 ~2 ~8 ~16 ~2 minecraft:air
fill ~10 ~7 ~8 ~10 ~17 ~8 minecraft:air
fill ~10 ~8 ~9 ~10 ~17 ~9 minecraft:air
fill ~10 ~9 ~10 ~10 ~17 ~10 minecraft:air
fill ~9 ~10 ~10 ~9 ~17 ~10 minecraft:air
fill ~8 ~11 ~10 ~8 ~17 ~10 minecraft:air
fill ~7 ~12 ~10 ~7 ~17 ~10 minecraft:air
fill ~6 ~13 ~10 ~6 ~17 ~10 minecraft:air
fill ~5 ~14 ~10 ~5 ~17 ~10 minecraft:air
fill ~6 ~14 ~5 ~8 ~17 ~6 minecraft:air
fill ~6 ~14 ~7 ~6 ~17 ~9 minecraft:air
fill ~7 ~14 ~8 ~7 ~17 ~9 minecraft:air
fill ~8 ~14 ~7 ~8 ~17 ~9 minecraft:air
fill ~4 ~15 ~10 ~4 ~17 ~10 minecraft:air
fill ~4 ~16 ~9 ~4 ~20 ~9 minecraft:air
fill ~4 ~17 ~8 ~4 ~20 ~8 minecraft:air
fill ~4 ~18 ~7 ~4 ~20 ~7 minecraft:air
fill ~3 ~19 ~11 ~3 ~20 ~11 minecraft:air
fill ~4 ~19 ~5 ~5 ~20 ~6 minecraft:air
fill ~5 ~19 ~7 ~5 ~20 ~9 minecraft:air
fill ~6 ~19 ~6 ~6 ~20 ~6 minecraft:air
fill ~6 ~19 ~8 ~6 ~20 ~8 minecraft:air
fill ~8 ~19 ~6 ~8 ~20 ~6 minecraft:air
fill ~8 ~19 ~8 ~8 ~20 ~8 minecraft:air
fill ~9 ~19 ~4 ~9 ~20 ~4 minecraft:air
fill ~10 ~19 ~5 ~10 ~20 ~5 minecraft:air
setblock ~2 ~20 ~9 minecraft:air
fill ~3 ~20 ~3 ~11 ~20 ~3 minecraft:air
fill ~3 ~20 ~4 ~8 ~20 ~4 minecraft:air
fill ~3 ~20 ~5 ~3 ~20 ~10 minecraft:air
fill ~4 ~20 ~10 ~4 ~20 ~11 minecraft:air
setblock ~5 ~20 ~11 minecraft:air
fill ~6 ~20 ~5 ~9 ~20 ~5 minecraft:air
setblock ~6 ~20 ~7 minecraft:air
fill ~6 ~20 ~9 ~6 ~20 ~11 minecraft:air
setblock ~7 ~20 ~6 minecraft:air
fill ~7 ~20 ~8 ~7 ~20 ~11 minecraft:air
setblock ~8 ~20 ~7 minecraft:air
fill ~8 ~20 ~9 ~8 ~20 ~11 minecraft:air
fill ~9 ~20 ~6 ~10 ~20 ~9 minecraft:air
fill ~9 ~20 ~10 ~9 ~20 ~11 minecraft:air
fill ~11 ~20 ~4 ~11 ~20 ~9 minecraft:air

# the ground round the tower: packed mud and gravel on the north approach to the doorway, a ring of cobble round the footing
setblock ~2 ~0 ~2 minecraft:stone
fill ~2 ~0 ~3 ~2 ~0 ~6 minecraft:cobblestone
fill ~2 ~0 ~7 ~2 ~0 ~8 minecraft:stone
setblock ~2 ~0 ~9 minecraft:cobbled_deepslate
fill ~2 ~0 ~10 ~2 ~0 ~12 minecraft:cobblestone
setblock ~3 ~0 ~2 minecraft:cobblestone
setblock ~3 ~0 ~12 minecraft:stone
setblock ~4 ~0 ~2 minecraft:andesite
fill ~4 ~0 ~4 ~4 ~0 ~5 minecraft:stone_bricks
setblock ~4 ~0 ~6 minecraft:cracked_stone_bricks
fill ~4 ~0 ~7 ~4 ~0 ~9 minecraft:stone_bricks
setblock ~4 ~0 ~10 minecraft:andesite
setblock ~4 ~0 ~12 minecraft:cobblestone
fill ~5 ~0 ~0 ~6 ~0 ~0 minecraft:packed_mud
setblock ~5 ~0 ~1 minecraft:gravel
setblock ~5 ~0 ~2 minecraft:cobblestone
fill ~5 ~0 ~4 ~5 ~0 ~5 minecraft:cobblestone
fill ~5 ~0 ~6 ~9 ~0 ~6 minecraft:stone_bricks
setblock ~5 ~0 ~7 minecraft:andesite
fill ~5 ~0 ~8 ~9 ~0 ~8 minecraft:stone_bricks
setblock ~5 ~0 ~9 minecraft:cobblestone
fill ~5 ~0 ~10 ~6 ~0 ~10 minecraft:stone_bricks
fill ~5 ~0 ~12 ~6 ~0 ~12 minecraft:andesite
setblock ~6 ~0 ~1 minecraft:packed_mud
setblock ~6 ~0 ~2 minecraft:andesite
fill ~6 ~0 ~4 ~8 ~0 ~4 minecraft:stone_bricks
fill ~6 ~0 ~5 ~7 ~0 ~5 minecraft:cracked_stone_bricks
fill ~6 ~0 ~7 ~10 ~0 ~7 minecraft:stone_bricks
setblock ~6 ~0 ~9 minecraft:cracked_stone_bricks
setblock ~7 ~0 ~0 minecraft:coarse_dirt
setblock ~7 ~0 ~1 minecraft:gravel
fill ~7 ~0 ~2 ~8 ~0 ~2 minecraft:cobblestone
setblock ~7 ~0 ~9 minecraft:cobblestone
setblock ~7 ~0 ~10 minecraft:cracked_stone_bricks
setblock ~7 ~0 ~12 minecraft:cobbled_deepslate
fill ~8 ~0 ~0 ~8 ~0 ~1 minecraft:packed_mud
setblock ~8 ~0 ~5 minecraft:andesite
fill ~8 ~0 ~9 ~8 ~0 ~10 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~12 minecraft:andesite
setblock ~9 ~0 ~0 minecraft:gravel
fill ~9 ~0 ~1 ~9 ~0 ~2 minecraft:cobblestone
fill ~9 ~0 ~4 ~10 ~0 ~4 minecraft:cracked_stone_bricks
fill ~9 ~0 ~5 ~10 ~0 ~5 minecraft:stone_bricks
setblock ~9 ~0 ~9 minecraft:cobblestone
fill ~9 ~0 ~10 ~10 ~0 ~10 minecraft:stone_bricks
setblock ~9 ~0 ~12 minecraft:cobblestone
fill ~10 ~0 ~2 ~12 ~0 ~2 minecraft:cobblestone
setblock ~10 ~0 ~6 minecraft:cracked_stone_bricks
setblock ~10 ~0 ~8 minecraft:cobblestone
setblock ~10 ~0 ~9 minecraft:andesite
fill ~10 ~0 ~12 ~11 ~0 ~12 minecraft:cobbled_deepslate
setblock ~12 ~0 ~3 minecraft:andesite
setblock ~12 ~0 ~4 minecraft:cobblestone
setblock ~12 ~0 ~5 minecraft:stone
setblock ~12 ~0 ~6 minecraft:cobblestone
setblock ~12 ~0 ~7 minecraft:stone
fill ~12 ~0 ~8 ~12 ~0 ~12 minecraft:cobblestone

# the footing: a ring of cobblestone (y 1) and stone bricks (y 2) round the shaft
setblock ~2 ~1 ~2 minecraft:andesite
setblock ~2 ~1 ~3 minecraft:cobblestone
fill ~2 ~1 ~4 ~2 ~1 ~5 minecraft:stone
setblock ~2 ~1 ~6 minecraft:cobbled_deepslate
setblock ~2 ~1 ~7 minecraft:andesite
fill ~2 ~1 ~8 ~2 ~1 ~9 minecraft:cobblestone
setblock ~2 ~1 ~10 minecraft:stone
setblock ~2 ~1 ~11 minecraft:cobblestone
setblock ~2 ~1 ~12 minecraft:stone
setblock ~3 ~1 ~2 minecraft:cobblestone
fill ~3 ~1 ~12 ~6 ~1 ~12 minecraft:cobblestone
setblock ~4 ~1 ~2 minecraft:cobbled_deepslate
setblock ~5 ~1 ~2 minecraft:cobblestone
fill ~7 ~1 ~12 ~8 ~1 ~12 minecraft:andesite
setblock ~9 ~1 ~2 minecraft:stone
fill ~9 ~1 ~12 ~11 ~1 ~12 minecraft:cobblestone
setblock ~10 ~1 ~2 minecraft:cobblestone
setblock ~11 ~1 ~2 minecraft:stone
fill ~12 ~1 ~2 ~12 ~1 ~5 minecraft:cobblestone
setblock ~12 ~1 ~6 minecraft:stone
fill ~12 ~1 ~7 ~12 ~1 ~9 minecraft:cobblestone
setblock ~12 ~1 ~10 minecraft:stone
fill ~12 ~1 ~11 ~12 ~1 ~12 minecraft:cobblestone
fill ~2 ~2 ~2 ~2 ~2 ~3 minecraft:cracked_stone_bricks
setblock ~2 ~2 ~4 minecraft:stone_bricks
setblock ~2 ~2 ~5 minecraft:cracked_stone_bricks
fill ~2 ~2 ~6 ~2 ~2 ~8 minecraft:stone_bricks
setblock ~2 ~2 ~9 minecraft:cracked_stone_bricks
fill ~2 ~2 ~10 ~2 ~2 ~12 minecraft:stone_bricks
fill ~3 ~2 ~2 ~5 ~2 ~2 minecraft:stone_bricks
fill ~3 ~2 ~12 ~4 ~2 ~12 minecraft:stone_bricks
setblock ~5 ~2 ~12 minecraft:cracked_stone_bricks
fill ~6 ~2 ~12 ~11 ~2 ~12 minecraft:stone_bricks
setblock ~9 ~2 ~2 minecraft:cracked_stone_bricks
fill ~10 ~2 ~2 ~12 ~2 ~2 minecraft:stone_bricks
fill ~12 ~2 ~3 ~12 ~2 ~9 minecraft:stone_bricks
setblock ~12 ~2 ~10 minecraft:cracked_stone_bricks
fill ~12 ~2 ~11 ~12 ~2 ~12 minecraft:stone_bricks

# the shaft walls: stone bricks (cracked, a few andesite and deepslate flecks), a course of chiseled bricks at y 3, belt courses of smooth stone at y 9 and y 14 and dark oak posts in the four corners up to the platform
fill ~3 ~1 ~3 ~3 ~17 ~3 minecraft:dark_oak_log[axis=y]
setblock ~3 ~1 ~4 minecraft:cobbled_deepslate
fill ~3 ~1 ~5 ~3 ~1 ~9 minecraft:cobblestone
setblock ~3 ~1 ~10 minecraft:andesite
fill ~3 ~1 ~11 ~3 ~17 ~11 minecraft:dark_oak_log[axis=y]
setblock ~4 ~1 ~3 minecraft:cobbled_deepslate
setblock ~4 ~1 ~11 minecraft:stone
setblock ~5 ~1 ~11 minecraft:andesite
fill ~6 ~1 ~11 ~7 ~1 ~11 minecraft:cobblestone
fill ~8 ~1 ~11 ~9 ~1 ~11 minecraft:stone
setblock ~10 ~1 ~3 minecraft:cobblestone
setblock ~10 ~1 ~11 minecraft:cobblestone
fill ~11 ~1 ~3 ~11 ~17 ~3 minecraft:dark_oak_log[axis=y]
fill ~11 ~1 ~4 ~11 ~1 ~9 minecraft:cobblestone
setblock ~11 ~1 ~10 minecraft:stone
fill ~11 ~1 ~11 ~11 ~17 ~11 minecraft:dark_oak_log[axis=y]
setblock ~3 ~2 ~4 minecraft:cracked_stone_bricks
fill ~3 ~2 ~5 ~3 ~2 ~9 minecraft:stone_bricks
setblock ~3 ~2 ~10 minecraft:cracked_stone_bricks
fill ~4 ~2 ~3 ~4 ~8 ~3 minecraft:stone_bricks
fill ~4 ~2 ~11 ~10 ~2 ~11 minecraft:stone_bricks
fill ~10 ~2 ~3 ~10 ~8 ~3 minecraft:stone_bricks
fill ~11 ~2 ~4 ~11 ~2 ~6 minecraft:stone_bricks
setblock ~11 ~2 ~7 minecraft:cracked_stone_bricks
fill ~11 ~2 ~8 ~11 ~2 ~10 minecraft:stone_bricks
fill ~3 ~3 ~4 ~3 ~6 ~4 minecraft:stone_bricks
setblock ~3 ~3 ~5 minecraft:chiseled_stone_bricks
setblock ~3 ~3 ~6 minecraft:cracked_stone_bricks
setblock ~3 ~3 ~7 minecraft:chiseled_stone_bricks
setblock ~3 ~3 ~8 minecraft:stone_bricks
setblock ~3 ~3 ~9 minecraft:chiseled_stone_bricks
fill ~3 ~3 ~10 ~3 ~8 ~10 minecraft:stone_bricks
fill ~4 ~3 ~11 ~4 ~6 ~11 minecraft:stone_bricks
setblock ~5 ~3 ~11 minecraft:chiseled_stone_bricks
fill ~6 ~3 ~11 ~6 ~4 ~11 minecraft:stone_bricks
setblock ~7 ~3 ~11 minecraft:chiseled_stone_bricks
fill ~8 ~3 ~11 ~8 ~8 ~11 minecraft:stone_bricks
setblock ~9 ~3 ~11 minecraft:chiseled_stone_bricks
fill ~10 ~3 ~11 ~10 ~8 ~11 minecraft:stone_bricks
fill ~11 ~3 ~4 ~11 ~6 ~4 minecraft:stone_bricks
setblock ~11 ~3 ~5 minecraft:chiseled_stone_bricks
fill ~11 ~3 ~6 ~11 ~6 ~6 minecraft:stone_bricks
setblock ~11 ~3 ~7 minecraft:chiseled_stone_bricks
setblock ~11 ~3 ~8 minecraft:cracked_stone_bricks
setblock ~11 ~3 ~9 minecraft:chiseled_stone_bricks
fill ~11 ~3 ~10 ~11 ~7 ~10 minecraft:stone_bricks
fill ~3 ~4 ~5 ~3 ~4 ~7 minecraft:stone_bricks
setblock ~3 ~4 ~8 minecraft:cracked_stone_bricks
fill ~3 ~4 ~9 ~3 ~8 ~9 minecraft:stone_bricks
setblock ~5 ~4 ~11 minecraft:cracked_stone_bricks
fill ~7 ~4 ~11 ~7 ~7 ~11 minecraft:stone_bricks
fill ~9 ~4 ~11 ~9 ~8 ~11 minecraft:stone_bricks
fill ~11 ~4 ~5 ~11 ~8 ~5 minecraft:stone_bricks
setblock ~11 ~4 ~7 minecraft:cracked_stone_bricks
fill ~11 ~4 ~8 ~11 ~8 ~9 minecraft:stone_bricks
setblock ~3 ~5 ~5 minecraft:andesite
fill ~3 ~5 ~6 ~3 ~8 ~8 minecraft:stone_bricks
fill ~5 ~5 ~3 ~6 ~5 ~3 minecraft:stone_bricks
fill ~5 ~5 ~11 ~5 ~6 ~11 minecraft:stone_bricks
fill ~6 ~5 ~11 ~6 ~6 ~11 minecraft:cracked_stone_bricks
setblock ~7 ~5 ~3 minecraft:cracked_stone_bricks
fill ~8 ~5 ~3 ~9 ~8 ~3 minecraft:stone_bricks
fill ~11 ~5 ~7 ~11 ~6 ~7 minecraft:stone_bricks
setblock ~3 ~6 ~5 minecraft:cracked_stone_bricks
fill ~5 ~6 ~3 ~6 ~7 ~3 minecraft:cracked_stone_bricks
setblock ~7 ~6 ~3 minecraft:stone_bricks
setblock ~3 ~7 ~4 minecraft:andesite
fill ~4 ~7 ~11 ~5 ~7 ~11 minecraft:cracked_stone_bricks
setblock ~6 ~7 ~11 minecraft:stone_bricks
setblock ~11 ~7 ~4 minecraft:cracked_stone_bricks
setblock ~11 ~7 ~6 minecraft:andesite
setblock ~3 ~8 ~4 minecraft:stone_bricks
fill ~4 ~8 ~11 ~5 ~8 ~11 minecraft:stone_bricks
setblock ~5 ~8 ~3 minecraft:stone_bricks
setblock ~6 ~8 ~3 minecraft:cracked_stone_bricks
fill ~6 ~8 ~11 ~7 ~8 ~11 minecraft:cracked_stone_bricks
setblock ~11 ~8 ~4 minecraft:andesite
setblock ~11 ~8 ~6 minecraft:stone_bricks
setblock ~11 ~8 ~10 minecraft:cracked_stone_bricks
fill ~3 ~9 ~4 ~3 ~9 ~10 minecraft:smooth_stone
fill ~4 ~9 ~3 ~10 ~9 ~3 minecraft:smooth_stone
fill ~4 ~9 ~11 ~10 ~9 ~11 minecraft:smooth_stone
fill ~11 ~9 ~4 ~11 ~9 ~10 minecraft:smooth_stone
fill ~3 ~10 ~4 ~3 ~11 ~4 minecraft:cracked_stone_bricks
fill ~3 ~10 ~5 ~3 ~10 ~9 minecraft:stone_bricks
setblock ~3 ~10 ~10 minecraft:cracked_stone_bricks
fill ~4 ~10 ~3 ~10 ~11 ~3 minecraft:stone_bricks
fill ~4 ~10 ~11 ~5 ~11 ~11 minecraft:stone_bricks
setblock ~6 ~10 ~11 minecraft:cracked_stone_bricks
fill ~7 ~10 ~11 ~9 ~13 ~11 minecraft:stone_bricks
setblock ~10 ~10 ~11 minecraft:cracked_stone_bricks
fill ~11 ~10 ~4 ~11 ~13 ~6 minecraft:stone_bricks
setblock ~11 ~10 ~7 minecraft:cracked_stone_bricks
fill ~11 ~10 ~8 ~11 ~13 ~8 minecraft:stone_bricks
setblock ~11 ~10 ~9 minecraft:andesite
fill ~11 ~10 ~10 ~11 ~13 ~10 minecraft:stone_bricks
fill ~3 ~11 ~5 ~3 ~11 ~6 minecraft:stone_bricks
setblock ~3 ~11 ~7 minecraft:deepslate_bricks
fill ~3 ~11 ~8 ~3 ~11 ~10 minecraft:stone_bricks
fill ~6 ~11 ~11 ~6 ~13 ~11 minecraft:stone_bricks
fill ~10 ~11 ~11 ~10 ~12 ~11 minecraft:stone_bricks
setblock ~11 ~11 ~7 minecraft:stone_bricks
fill ~11 ~11 ~9 ~11 ~13 ~9 minecraft:stone_bricks
setblock ~3 ~12 ~4 minecraft:stone_bricks
fill ~3 ~12 ~6 ~3 ~12 ~8 minecraft:stone_bricks
setblock ~3 ~12 ~9 minecraft:andesite
fill ~3 ~12 ~10 ~3 ~13 ~10 minecraft:stone_bricks
fill ~4 ~12 ~3 ~6 ~12 ~3 minecraft:stone_bricks
fill ~4 ~12 ~11 ~4 ~13 ~11 minecraft:cracked_stone_bricks
setblock ~5 ~12 ~11 minecraft:stone_bricks
fill ~8 ~12 ~3 ~10 ~12 ~3 minecraft:stone_bricks
setblock ~3 ~13 ~4 minecraft:cracked_stone_bricks
setblock ~3 ~13 ~6 minecraft:deepslate_bricks
fill ~3 ~13 ~7 ~3 ~13 ~9 minecraft:stone_bricks
setblock ~4 ~13 ~3 minecraft:cracked_stone_bricks
fill ~5 ~13 ~3 ~6 ~13 ~3 minecraft:stone_bricks
setblock ~5 ~13 ~11 minecraft:cracked_stone_bricks
setblock ~8 ~13 ~3 minecraft:cracked_stone_bricks
fill ~9 ~13 ~3 ~10 ~13 ~3 minecraft:stone_bricks
setblock ~10 ~13 ~11 minecraft:cracked_stone_bricks
fill ~3 ~14 ~4 ~3 ~14 ~10 minecraft:smooth_stone
fill ~4 ~14 ~3 ~10 ~14 ~3 minecraft:smooth_stone
fill ~4 ~14 ~11 ~6 ~14 ~11 minecraft:smooth_stone
fill ~8 ~14 ~11 ~10 ~14 ~11 minecraft:smooth_stone
fill ~11 ~14 ~4 ~11 ~14 ~10 minecraft:smooth_stone
fill ~3 ~15 ~4 ~3 ~15 ~6 minecraft:stone_bricks
setblock ~3 ~15 ~7 minecraft:cracked_stone_bricks
fill ~3 ~15 ~8 ~3 ~16 ~10 minecraft:stone_bricks
setblock ~4 ~15 ~3 minecraft:cracked_stone_bricks
fill ~4 ~15 ~11 ~6 ~15 ~11 minecraft:stone_bricks
fill ~5 ~15 ~3 ~10 ~15 ~3 minecraft:stone_bricks
fill ~8 ~15 ~11 ~9 ~15 ~11 minecraft:stone_bricks
setblock ~10 ~15 ~11 minecraft:cracked_stone_bricks
fill ~11 ~15 ~4 ~11 ~17 ~4 minecraft:stone_bricks
setblock ~11 ~15 ~5 minecraft:deepslate_bricks
fill ~11 ~15 ~6 ~11 ~15 ~7 minecraft:stone_bricks
setblock ~11 ~15 ~8 minecraft:cracked_stone_bricks
fill ~11 ~15 ~9 ~11 ~16 ~10 minecraft:stone_bricks
fill ~3 ~16 ~4 ~3 ~17 ~4 minecraft:stone_bricks
fill ~3 ~16 ~6 ~3 ~16 ~7 minecraft:stone_bricks
fill ~4 ~16 ~3 ~6 ~16 ~3 minecraft:stone_bricks
fill ~4 ~16 ~11 ~5 ~16 ~11 minecraft:stone_bricks
setblock ~6 ~16 ~11 minecraft:andesite
fill ~8 ~16 ~3 ~10 ~16 ~3 minecraft:stone_bricks
fill ~8 ~16 ~11 ~9 ~16 ~11 minecraft:cracked_stone_bricks
setblock ~10 ~16 ~11 minecraft:stone_bricks
fill ~11 ~16 ~5 ~11 ~17 ~6 minecraft:stone_bricks
fill ~11 ~16 ~8 ~11 ~17 ~8 minecraft:stone_bricks
setblock ~3 ~17 ~6 minecraft:stone_bricks
setblock ~3 ~17 ~7 minecraft:cracked_stone_bricks
setblock ~3 ~17 ~8 minecraft:stone_bricks
fill ~3 ~17 ~9 ~3 ~17 ~10 minecraft:cracked_stone_bricks
fill ~4 ~17 ~3 ~5 ~17 ~3 minecraft:stone_bricks
fill ~4 ~17 ~11 ~5 ~17 ~11 minecraft:cracked_stone_bricks
setblock ~6 ~17 ~3 minecraft:cracked_stone_bricks
setblock ~6 ~17 ~11 minecraft:stone_bricks
setblock ~8 ~17 ~3 minecraft:stone_bricks
setblock ~8 ~17 ~11 minecraft:stone_bricks
setblock ~9 ~17 ~3 minecraft:cracked_stone_bricks
setblock ~9 ~17 ~11 minecraft:deepslate_bricks
setblock ~10 ~17 ~3 minecraft:deepslate_bricks
setblock ~10 ~17 ~11 minecraft:cracked_stone_bricks
setblock ~11 ~17 ~9 minecraft:cracked_stone_bricks
setblock ~11 ~17 ~10 minecraft:stone_bricks

# the doorway: x 6..8, y 1..3 open through the footing as well, a lintel of dark oak at y 4, jamb posts at x 5 and x 9
fill ~5 ~1 ~3 ~5 ~3 ~3 minecraft:dark_oak_log[axis=y]
fill ~6 ~1 ~2 ~8 ~2 ~3 minecraft:air
fill ~9 ~1 ~3 ~9 ~3 ~3 minecraft:dark_oak_log[axis=y]
fill ~6 ~3 ~3 ~8 ~3 ~3 minecraft:air
fill ~5 ~4 ~3 ~9 ~4 ~3 minecraft:dark_oak_log[axis=x]

# arrow slits one block wide and two high: north (x 7) and east (z 7) at y 7..8 and y 12..13, west (z 5) the same, south (x 7) at y 14..15; the stair mass stands behind no slit
fill ~3 ~7 ~5 ~3 ~8 ~5 minecraft:air
fill ~7 ~7 ~3 ~7 ~8 ~3 minecraft:air
fill ~11 ~7 ~7 ~11 ~8 ~7 minecraft:air
fill ~3 ~12 ~5 ~3 ~13 ~5 minecraft:air
fill ~7 ~12 ~3 ~7 ~13 ~3 minecraft:air
fill ~11 ~12 ~7 ~11 ~13 ~7 minecraft:air
fill ~7 ~14 ~11 ~7 ~15 ~11 minecraft:air

# a small pane window two high under the platform on every face (y 16..17)
fill ~3 ~16 ~5 ~3 ~17 ~5 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~7 ~16 ~3 ~7 ~17 ~3 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~7 ~16 ~11 ~7 ~17 ~11 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~11 ~16 ~7 ~11 ~17 ~7 minecraft:glass_pane[north=true,east=false,south=true,west=false]

# the inner stair: stone brick stairs along the inner face of the wall, once round clockwise from the north-east corner and up the west side, one block up for each cell (y 1 to 18, the last one flush with the platform floor), each facing the next cell
fill ~4 ~1 ~6 ~4 ~1 ~9 minecraft:stone_bricks
fill ~4 ~1 ~10 ~4 ~2 ~10 minecraft:cracked_stone_bricks
fill ~5 ~1 ~10 ~5 ~6 ~10 minecraft:stone_bricks
setblock ~6 ~1 ~10 minecraft:cracked_stone_bricks
fill ~7 ~1 ~10 ~9 ~1 ~10 minecraft:stone_bricks
setblock ~9 ~1 ~4 minecraft:stone_brick_stairs[facing=east,half=bottom]
fill ~10 ~1 ~4 ~10 ~1 ~10 minecraft:stone_bricks
fill ~4 ~2 ~6 ~4 ~3 ~6 minecraft:cracked_stone_bricks
fill ~4 ~2 ~7 ~4 ~2 ~9 minecraft:stone_bricks
setblock ~6 ~2 ~10 minecraft:stone_bricks
setblock ~7 ~2 ~10 minecraft:cracked_stone_bricks
fill ~8 ~2 ~10 ~9 ~4 ~10 minecraft:stone_bricks
setblock ~10 ~2 ~4 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~10 ~2 ~5 minecraft:cracked_stone_bricks
fill ~10 ~2 ~6 ~10 ~2 ~7 minecraft:stone_bricks
setblock ~10 ~2 ~8 minecraft:cracked_stone_bricks
fill ~10 ~2 ~9 ~10 ~5 ~9 minecraft:stone_bricks
setblock ~10 ~2 ~10 minecraft:cracked_stone_bricks
fill ~4 ~3 ~7 ~4 ~6 ~8 minecraft:stone_bricks
setblock ~4 ~3 ~9 minecraft:andesite
fill ~4 ~3 ~10 ~4 ~13 ~10 minecraft:stone_bricks
fill ~6 ~3 ~10 ~6 ~4 ~10 minecraft:cracked_stone_bricks
fill ~7 ~3 ~10 ~7 ~9 ~10 minecraft:stone_bricks
setblock ~10 ~3 ~5 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~10 ~3 ~6 minecraft:andesite
fill ~10 ~3 ~7 ~10 ~4 ~8 minecraft:stone_bricks
fill ~10 ~3 ~10 ~10 ~7 ~10 minecraft:stone_bricks
fill ~4 ~4 ~6 ~4 ~17 ~6 minecraft:stone_bricks
setblock ~4 ~4 ~9 minecraft:stone_bricks
setblock ~10 ~4 ~6 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~4 ~5 ~9 minecraft:deepslate_bricks
setblock ~6 ~5 ~10 minecraft:stone_bricks
setblock ~8 ~5 ~10 minecraft:cracked_stone_bricks
fill ~9 ~5 ~10 ~9 ~8 ~10 minecraft:stone_bricks
setblock ~10 ~5 ~7 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~10 ~5 ~8 minecraft:stone_bricks
fill ~4 ~6 ~9 ~4 ~9 ~9 minecraft:stone_bricks
fill ~6 ~6 ~10 ~6 ~7 ~10 minecraft:cracked_stone_bricks
fill ~8 ~6 ~10 ~8 ~9 ~10 minecraft:stone_bricks
setblock ~10 ~6 ~8 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~10 ~6 ~9 minecraft:deepslate_bricks
fill ~4 ~7 ~7 ~4 ~12 ~7 minecraft:stone_bricks
setblock ~4 ~7 ~8 minecraft:cracked_stone_bricks
setblock ~5 ~7 ~10 minecraft:deepslate_bricks
setblock ~10 ~7 ~9 minecraft:stone_brick_stairs[facing=south,half=bottom]
fill ~4 ~8 ~8 ~4 ~9 ~8 minecraft:stone_bricks
fill ~5 ~8 ~10 ~6 ~11 ~10 minecraft:stone_bricks
setblock ~10 ~8 ~10 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~9 ~9 ~10 minecraft:stone_brick_stairs[facing=west,half=bottom]
fill ~4 ~10 ~8 ~4 ~10 ~9 minecraft:cracked_stone_bricks
setblock ~7 ~10 ~10 minecraft:deepslate_bricks
setblock ~8 ~10 ~10 minecraft:stone_brick_stairs[facing=west,half=bottom]
fill ~4 ~11 ~8 ~4 ~13 ~9 minecraft:stone_bricks
setblock ~7 ~11 ~10 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~5 ~12 ~10 minecraft:stone_bricks
setblock ~6 ~12 ~10 minecraft:stone_brick_stairs[facing=west,half=bottom]
fill ~4 ~13 ~7 ~4 ~14 ~7 minecraft:cracked_stone_bricks
setblock ~5 ~13 ~10 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~4 ~14 ~8 minecraft:deepslate_bricks
setblock ~4 ~14 ~9 minecraft:stone_bricks
setblock ~4 ~14 ~10 minecraft:stone_brick_stairs[facing=north,half=bottom]
fill ~4 ~15 ~7 ~4 ~15 ~8 minecraft:stone_bricks
setblock ~4 ~15 ~9 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~4 ~16 ~7 minecraft:stone_bricks
setblock ~4 ~16 ~8 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~4 ~17 ~7 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~4 ~18 ~6 minecraft:stone_brick_stairs[facing=north,half=bottom]

# the platform floor at y 18 over the whole shaft and its overhang, with the hatch (4, 18, 7..9) over the last steps (the last stair at (4, 18, 6) is flush with the floor); a scorch of blackstone round the fire pit
fill ~2 ~18 ~2 ~12 ~18 ~2 minecraft:smooth_stone
fill ~2 ~18 ~3 ~2 ~18 ~12 minecraft:smooth_stone
fill ~3 ~18 ~3 ~11 ~18 ~3 minecraft:stone_bricks
fill ~3 ~18 ~4 ~3 ~18 ~11 minecraft:stone_bricks
fill ~3 ~18 ~12 ~11 ~18 ~12 minecraft:smooth_stone
fill ~4 ~18 ~4 ~6 ~18 ~4 minecraft:cracked_stone_bricks
fill ~4 ~18 ~5 ~8 ~18 ~5 minecraft:stone_bricks
fill ~4 ~18 ~10 ~4 ~18 ~11 minecraft:stone_bricks
fill ~5 ~18 ~6 ~5 ~18 ~11 minecraft:stone_bricks
fill ~6 ~18 ~6 ~8 ~18 ~8 minecraft:blackstone
setblock ~6 ~18 ~9 minecraft:cracked_stone_bricks
fill ~6 ~18 ~10 ~9 ~18 ~11 minecraft:stone_bricks
fill ~7 ~18 ~4 ~11 ~18 ~4 minecraft:stone_bricks
fill ~7 ~18 ~9 ~11 ~18 ~9 minecraft:stone_bricks
setblock ~9 ~18 ~5 minecraft:cracked_stone_bricks
fill ~9 ~18 ~6 ~9 ~18 ~7 minecraft:stone_bricks
setblock ~9 ~18 ~8 minecraft:cracked_stone_bricks
fill ~10 ~18 ~5 ~11 ~18 ~7 minecraft:stone_bricks
setblock ~10 ~18 ~8 minecraft:deepslate_bricks
setblock ~10 ~18 ~10 minecraft:cracked_stone_bricks
setblock ~10 ~18 ~11 minecraft:stone_bricks
setblock ~11 ~18 ~8 minecraft:cracked_stone_bricks
fill ~11 ~18 ~10 ~11 ~18 ~11 minecraft:stone_bricks
fill ~12 ~18 ~3 ~12 ~18 ~12 minecraft:smooth_stone

# corbels under the overhang: upside-down dark oak stairs at y 17 round the ring x 2..12 (the back against the shaft), the corner blocks of dark oak logs
setblock ~2 ~17 ~2 minecraft:dark_oak_log[axis=y]
fill ~2 ~17 ~3 ~2 ~17 ~11 minecraft:dark_oak_stairs[facing=east,half=top]
setblock ~2 ~17 ~12 minecraft:dark_oak_log[axis=y]
fill ~3 ~17 ~2 ~11 ~17 ~2 minecraft:dark_oak_stairs[facing=south,half=top]
fill ~3 ~17 ~12 ~11 ~17 ~12 minecraft:dark_oak_stairs[facing=north,half=top]
setblock ~12 ~17 ~2 minecraft:dark_oak_log[axis=y]
fill ~12 ~17 ~3 ~12 ~17 ~11 minecraft:dark_oak_stairs[facing=west,half=top]
setblock ~12 ~17 ~12 minecraft:dark_oak_log[axis=y]

# the parapet on the ring x 2..12, z 2..12: a wall of stone bricks at y 19 with a merlon at y 20 on every second cell, taller corner blocks, an embrasure in the middle of each face
fill ~2 ~19 ~2 ~12 ~19 ~2 minecraft:stone_bricks
fill ~2 ~19 ~3 ~2 ~19 ~6 minecraft:stone_bricks
fill ~2 ~19 ~7 ~2 ~19 ~8 minecraft:cracked_stone_bricks
fill ~2 ~19 ~9 ~2 ~19 ~12 minecraft:stone_bricks
fill ~3 ~19 ~12 ~4 ~19 ~12 minecraft:stone_bricks
setblock ~5 ~19 ~12 minecraft:andesite
fill ~6 ~19 ~12 ~6 ~20 ~12 minecraft:stone_bricks
setblock ~7 ~19 ~12 minecraft:cracked_stone_bricks
fill ~8 ~19 ~12 ~11 ~19 ~12 minecraft:stone_bricks
fill ~12 ~19 ~3 ~12 ~19 ~4 minecraft:stone_bricks
setblock ~12 ~19 ~5 minecraft:cracked_stone_bricks
fill ~12 ~19 ~6 ~12 ~19 ~12 minecraft:stone_bricks
setblock ~2 ~20 ~2 minecraft:chiseled_stone_bricks
setblock ~2 ~20 ~4 minecraft:stone_bricks
setblock ~2 ~20 ~6 minecraft:stone_bricks
setblock ~2 ~20 ~8 minecraft:stone_bricks
setblock ~2 ~20 ~10 minecraft:stone_bricks
setblock ~2 ~20 ~12 minecraft:chiseled_stone_bricks
setblock ~4 ~20 ~2 minecraft:stone_bricks
setblock ~4 ~20 ~12 minecraft:stone_bricks
setblock ~6 ~20 ~2 minecraft:cracked_stone_bricks
setblock ~8 ~20 ~2 minecraft:stone_bricks
setblock ~8 ~20 ~12 minecraft:cracked_stone_bricks
setblock ~10 ~20 ~2 minecraft:stone_bricks
setblock ~10 ~20 ~12 minecraft:cracked_stone_bricks
setblock ~12 ~20 ~2 minecraft:chiseled_stone_bricks
setblock ~12 ~20 ~4 minecraft:stone_bricks
setblock ~12 ~20 ~6 minecraft:stone_bricks
setblock ~12 ~20 ~8 minecraft:stone_bricks
setblock ~12 ~20 ~10 minecraft:stone_bricks
setblock ~12 ~20 ~12 minecraft:chiseled_stone_bricks

# the signal fire: netherrack at (7, 19, 7), fire at (7, 20, 7), a cobblestone wall on each of the four sides at y 19
setblock ~6 ~19 ~7 minecraft:cobblestone_wall[north=none,east=low,south=none,west=none,up=true]
setblock ~7 ~19 ~6 minecraft:cobblestone_wall[north=none,east=none,south=low,west=none,up=true]
setblock ~7 ~19 ~7 minecraft:netherrack
setblock ~7 ~19 ~8 minecraft:cobblestone_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~8 ~19 ~7 minecraft:cobblestone_wall[north=none,east=none,south=none,west=low,up=true]
setblock ~7 ~20 ~7 minecraft:fire[age=0,north=false,south=false,east=false,west=false,up=false]

# on the platform: a stack of spruce logs for the fire (kept three blocks from it), two barrels of pitch and a lantern on a post at the north-east corner
fill ~5 ~19 ~10 ~5 ~19 ~11 minecraft:barrel[facing=up]
setblock ~10 ~19 ~4 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~10 ~19 ~10 ~11 ~19 ~11 minecraft:spruce_log[axis=x]
setblock ~5 ~20 ~10 minecraft:barrel[facing=up]
setblock ~10 ~20 ~4 minecraft:lantern[hanging=false]
fill ~10 ~20 ~10 ~11 ~20 ~10 minecraft:spruce_log[axis=x]

# inside: a lantern on a chain from the platform floor, a barrel and a bench by the door, a rack for spears; wall torches are replaced by lanterns on the stair side
fill ~5 ~1 ~5 ~5 ~1 ~6 minecraft:barrel[facing=up]
setblock ~9 ~1 ~5 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~5 ~2 ~5 minecraft:barrel[facing=up]
setblock ~7 ~14 ~7 minecraft:lantern[hanging=true]
fill ~7 ~15 ~7 ~7 ~17 ~7 minecraft:chain[axis=y]

# two red banners hung on the north face either side of the doorway above the lintel
setblock ~6 ~6 ~2 minecraft:red_wall_banner[facing=north]
setblock ~8 ~6 ~2 minecraft:red_wall_banner[facing=north]

# the door side: lanterns on posts at the doorstep
fill ~5 ~1 ~1 ~5 ~2 ~1 minecraft:dark_oak_fence[north=false,east=false,south=true,west=false]
fill ~9 ~1 ~1 ~9 ~2 ~1 minecraft:dark_oak_fence[north=false,east=false,south=true,west=false]
setblock ~5 ~3 ~1 minecraft:lantern[hanging=false]
setblock ~9 ~3 ~1 minecraft:lantern[hanging=false]

# snow banked round the footing, drifted against the north approach; the doorstep stays trodden
fill ~0 ~0 ~1 ~0 ~0 ~4 minecraft:snow_block
setblock ~0 ~0 ~7 minecraft:snow_block
fill ~0 ~0 ~10 ~0 ~0 ~11 minecraft:snow_block
setblock ~0 ~0 ~13 minecraft:snow_block
fill ~1 ~0 ~0 ~1 ~0 ~1 minecraft:snow_block
setblock ~1 ~0 ~3 minecraft:snow_block
fill ~1 ~0 ~5 ~1 ~0 ~6 minecraft:snow_block
fill ~2 ~0 ~13 ~3 ~0 ~13 minecraft:snow_block
fill ~3 ~0 ~0 ~4 ~0 ~0 minecraft:snow_block
fill ~3 ~0 ~14 ~4 ~0 ~14 minecraft:snow_block
setblock ~4 ~0 ~1 minecraft:snow_block
fill ~5 ~0 ~13 ~5 ~0 ~14 minecraft:snow_block
setblock ~7 ~0 ~14 minecraft:snow_block
setblock ~8 ~0 ~13 minecraft:snow_block
fill ~10 ~0 ~0 ~12 ~0 ~0 minecraft:snow_block
fill ~10 ~0 ~13 ~11 ~0 ~13 minecraft:snow_block
fill ~11 ~0 ~14 ~12 ~0 ~14 minecraft:snow_block
setblock ~13 ~0 ~2 minecraft:snow_block
fill ~13 ~0 ~4 ~13 ~0 ~5 minecraft:snow_block
setblock ~13 ~0 ~7 minecraft:snow_block
fill ~13 ~0 ~9 ~13 ~0 ~10 minecraft:snow_block
fill ~13 ~0 ~12 ~13 ~0 ~13 minecraft:snow_block
fill ~14 ~0 ~3 ~14 ~0 ~7 minecraft:snow_block
fill ~14 ~0 ~10 ~14 ~0 ~12 minecraft:snow_block
setblock ~14 ~0 ~14 minecraft:snow_block
fill ~0 ~1 ~1 ~0 ~1 ~2 minecraft:snow[layers=1]
fill ~0 ~1 ~3 ~0 ~1 ~4 minecraft:snow[layers=2]
setblock ~0 ~1 ~7 minecraft:snow[layers=1]
setblock ~0 ~1 ~10 minecraft:snow[layers=2]
setblock ~0 ~1 ~11 minecraft:snow[layers=1]
setblock ~0 ~1 ~13 minecraft:snow[layers=2]
setblock ~1 ~1 ~0 minecraft:snow[layers=1]
setblock ~1 ~1 ~1 minecraft:snow[layers=2]
setblock ~1 ~1 ~3 minecraft:snow[layers=1]
setblock ~1 ~1 ~5 minecraft:snow[layers=1]
setblock ~1 ~1 ~6 minecraft:snow[layers=2]
fill ~2 ~1 ~13 ~3 ~1 ~13 minecraft:snow[layers=1]
fill ~3 ~1 ~0 ~4 ~1 ~0 minecraft:snow[layers=1]
fill ~3 ~1 ~14 ~4 ~1 ~14 minecraft:snow[layers=1]
setblock ~4 ~1 ~1 minecraft:snow[layers=2]
fill ~5 ~1 ~13 ~5 ~1 ~14 minecraft:snow[layers=1]
setblock ~7 ~1 ~14 minecraft:snow[layers=2]
setblock ~8 ~1 ~13 minecraft:snow[layers=1]
setblock ~10 ~1 ~0 minecraft:snow[layers=1]
fill ~10 ~1 ~13 ~11 ~1 ~13 minecraft:snow[layers=1]
setblock ~11 ~1 ~0 minecraft:snow[layers=2]
setblock ~11 ~1 ~14 minecraft:snow[layers=1]
setblock ~12 ~1 ~0 minecraft:snow[layers=1]
setblock ~12 ~1 ~14 minecraft:snow[layers=2]
setblock ~13 ~1 ~2 minecraft:snow[layers=1]
fill ~13 ~1 ~4 ~14 ~1 ~4 minecraft:snow[layers=2]
setblock ~13 ~1 ~5 minecraft:snow[layers=1]
setblock ~13 ~1 ~7 minecraft:snow[layers=1]
fill ~13 ~1 ~9 ~13 ~1 ~10 minecraft:snow[layers=1]
fill ~13 ~1 ~12 ~13 ~1 ~13 minecraft:snow[layers=1]
setblock ~14 ~1 ~3 minecraft:snow[layers=1]
setblock ~14 ~1 ~5 minecraft:snow[layers=2]
setblock ~14 ~1 ~6 minecraft:snow[layers=1]
setblock ~14 ~1 ~7 minecraft:snow[layers=2]
fill ~14 ~1 ~10 ~14 ~1 ~12 minecraft:snow[layers=1]
setblock ~14 ~1 ~14 minecraft:snow[layers=2]

# snow settles on the sky-facing tops: the parapet, the merlons, the footing and the log stack (one layer); the hatch and the fire pit stay clear
fill ~3 ~19 ~3 ~11 ~19 ~3 minecraft:snow[layers=1]
fill ~3 ~19 ~4 ~8 ~19 ~4 minecraft:snow[layers=1]
fill ~3 ~19 ~5 ~3 ~19 ~10 minecraft:snow[layers=1]
fill ~4 ~19 ~10 ~4 ~19 ~11 minecraft:snow[layers=1]
fill ~6 ~19 ~5 ~9 ~19 ~5 minecraft:snow[layers=1]
fill ~6 ~19 ~9 ~8 ~19 ~11 minecraft:snow[layers=1]
fill ~9 ~19 ~6 ~10 ~19 ~9 minecraft:snow[layers=1]
fill ~9 ~19 ~10 ~9 ~19 ~11 minecraft:snow[layers=1]
fill ~11 ~19 ~4 ~11 ~19 ~9 minecraft:snow[layers=1]
setblock ~2 ~20 ~3 minecraft:snow[layers=1]
setblock ~2 ~20 ~5 minecraft:snow[layers=1]
setblock ~2 ~20 ~7 minecraft:snow[layers=1]
setblock ~2 ~20 ~11 minecraft:snow[layers=1]
setblock ~3 ~20 ~2 minecraft:snow[layers=1]
setblock ~3 ~20 ~12 minecraft:snow[layers=1]
setblock ~5 ~20 ~2 minecraft:snow[layers=1]
setblock ~5 ~20 ~12 minecraft:snow[layers=1]
setblock ~7 ~20 ~2 minecraft:snow[layers=1]
setblock ~7 ~20 ~12 minecraft:snow[layers=1]
setblock ~9 ~20 ~2 minecraft:snow[layers=1]
setblock ~9 ~20 ~12 minecraft:snow[layers=1]
fill ~10 ~20 ~11 ~12 ~20 ~11 minecraft:snow[layers=1]
setblock ~11 ~20 ~2 minecraft:snow[layers=1]
setblock ~11 ~20 ~12 minecraft:snow[layers=1]
setblock ~12 ~20 ~3 minecraft:snow[layers=1]
setblock ~12 ~20 ~5 minecraft:snow[layers=1]
setblock ~12 ~20 ~7 minecraft:snow[layers=1]
setblock ~12 ~20 ~9 minecraft:snow[layers=1]
