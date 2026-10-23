# size 23 14 31
# camp/archive: the archive: a stone hall with its door on the west face (the Archivist), and the walled court of the Founders' Stone.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# The Archivist stands at (9.5, 1, 9.5) facing the west door; the Founders' Stone, chiseled stone bricks around a lodestone, at (15, 1, 23).

# paths: the forecourt before the west door, the way from the parade (z 15) and the way to the court gate
fill ~0 ~0 ~6 ~3 ~0 ~6 minecraft:stone_bricks
fill ~0 ~0 ~7 ~0 ~0 ~10 minecraft:stone_bricks
setblock ~0 ~0 ~11 minecraft:mossy_stone_bricks
setblock ~0 ~0 ~12 minecraft:stone_bricks
setblock ~0 ~0 ~13 minecraft:mossy_stone_bricks
fill ~0 ~0 ~14 ~1 ~0 ~14 minecraft:stone_bricks
setblock ~0 ~0 ~15 minecraft:cracked_stone_bricks
fill ~0 ~0 ~16 ~1 ~0 ~16 minecraft:stone_bricks
setblock ~0 ~0 ~17 minecraft:stone_bricks
fill ~1 ~0 ~7 ~2 ~0 ~8 minecraft:cracked_stone_bricks
fill ~1 ~0 ~9 ~1 ~0 ~12 minecraft:stone_bricks
setblock ~1 ~0 ~15 minecraft:cobblestone
setblock ~1 ~0 ~17 minecraft:cracked_stone_bricks
setblock ~2 ~0 ~9 minecraft:cracked_stone_bricks
fill ~2 ~0 ~10 ~2 ~0 ~17 minecraft:stone_bricks
fill ~2 ~0 ~18 ~2 ~0 ~19 minecraft:cobblestone
fill ~3 ~0 ~7 ~3 ~0 ~12 minecraft:stone_bricks
setblock ~3 ~0 ~13 minecraft:cobblestone
fill ~3 ~0 ~14 ~3 ~0 ~15 minecraft:stone_bricks
setblock ~3 ~0 ~16 minecraft:cobblestone
fill ~3 ~0 ~17 ~4 ~0 ~17 minecraft:stone_bricks
fill ~3 ~0 ~18 ~3 ~0 ~20 minecraft:stone_bricks
fill ~4 ~0 ~18 ~4 ~0 ~19 minecraft:mossy_stone_bricks
setblock ~4 ~0 ~20 minecraft:stone_bricks
setblock ~4 ~0 ~21 minecraft:mossy_stone_bricks
setblock ~4 ~0 ~22 minecraft:stone_bricks
fill ~5 ~0 ~18 ~5 ~0 ~23 minecraft:stone_bricks
setblock ~6 ~0 ~20 minecraft:stone_bricks
setblock ~6 ~0 ~21 minecraft:mossy_stone_bricks
setblock ~6 ~0 ~22 minecraft:cracked_stone_bricks
setblock ~6 ~0 ~23 minecraft:mossy_stone_bricks
setblock ~6 ~0 ~24 minecraft:cobblestone
fill ~7 ~0 ~21 ~7 ~0 ~23 minecraft:stone_bricks
fill ~7 ~0 ~24 ~7 ~0 ~25 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~22 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~23 minecraft:stone_bricks
setblock ~8 ~0 ~24 minecraft:cobblestone
setblock ~8 ~0 ~25 minecraft:stone_bricks

# the hall floor: polished deepslate, a dark oak runner from the door to the desk
fill ~4 ~0 ~4 ~16 ~0 ~7 minecraft:polished_deepslate
fill ~4 ~0 ~8 ~4 ~0 ~14 minecraft:polished_deepslate
fill ~5 ~0 ~8 ~8 ~0 ~10 minecraft:dark_oak_planks
fill ~5 ~0 ~11 ~8 ~0 ~14 minecraft:polished_deepslate
fill ~9 ~0 ~8 ~16 ~0 ~14 minecraft:polished_deepslate

# footings under the gable walls, which stand out past the long walls, and under the buttresses
setblock ~4 ~0 ~3 minecraft:stone_bricks
setblock ~4 ~0 ~15 minecraft:stone_bricks
setblock ~6 ~0 ~3 minecraft:stone_bricks
setblock ~6 ~0 ~15 minecraft:stone_bricks
setblock ~10 ~0 ~3 minecraft:stone_bricks
setblock ~10 ~0 ~15 minecraft:stone_bricks
setblock ~14 ~0 ~3 minecraft:stone_bricks
setblock ~14 ~0 ~15 minecraft:stone_bricks
setblock ~16 ~0 ~3 minecraft:stone_bricks
setblock ~16 ~0 ~15 minecraft:stone_bricks

# the hall walls: stone bricks; the long walls y 1..6, the gable walls (z 3..15, past the eaves) up to the roof line
fill ~4 ~1 ~3 ~4 ~2 ~4 minecraft:stone_bricks
fill ~4 ~1 ~5 ~4 ~1 ~6 minecraft:mossy_stone_bricks
setblock ~4 ~1 ~7 minecraft:cracked_stone_bricks
fill ~4 ~1 ~11 ~4 ~1 ~12 minecraft:mossy_stone_bricks
fill ~4 ~1 ~13 ~4 ~2 ~14 minecraft:stone_bricks
setblock ~4 ~1 ~15 minecraft:mossy_stone_bricks
setblock ~5 ~1 ~4 minecraft:stone_bricks
fill ~5 ~1 ~14 ~8 ~1 ~14 minecraft:stone_bricks
setblock ~6 ~1 ~4 minecraft:cracked_stone_bricks
setblock ~7 ~1 ~4 minecraft:mossy_cobblestone
setblock ~8 ~1 ~4 minecraft:stone_bricks
fill ~9 ~1 ~4 ~10 ~1 ~4 minecraft:mossy_stone_bricks
setblock ~9 ~1 ~14 minecraft:mossy_stone_bricks
fill ~10 ~1 ~14 ~10 ~5 ~14 minecraft:stone_bricks
fill ~11 ~1 ~4 ~11 ~3 ~4 minecraft:stone_bricks
setblock ~11 ~1 ~14 minecraft:cracked_stone_bricks
fill ~12 ~1 ~4 ~13 ~1 ~4 minecraft:mossy_cobblestone
setblock ~12 ~1 ~14 minecraft:stone_bricks
fill ~13 ~1 ~14 ~14 ~1 ~14 minecraft:mossy_stone_bricks
fill ~14 ~1 ~4 ~15 ~2 ~4 minecraft:stone_bricks
fill ~15 ~1 ~14 ~15 ~3 ~14 minecraft:stone_bricks
fill ~16 ~1 ~3 ~16 ~1 ~5 minecraft:stone_bricks
fill ~16 ~1 ~6 ~16 ~2 ~7 minecraft:mossy_stone_bricks
setblock ~16 ~1 ~8 minecraft:stone_bricks
fill ~16 ~1 ~10 ~16 ~5 ~10 minecraft:stone_bricks
fill ~16 ~1 ~11 ~16 ~1 ~12 minecraft:mossy_stone_bricks
fill ~16 ~1 ~13 ~16 ~1 ~15 minecraft:stone_bricks
fill ~4 ~2 ~5 ~4 ~3 ~6 minecraft:stone_bricks
fill ~4 ~2 ~7 ~4 ~3 ~7 minecraft:mossy_stone_bricks
fill ~4 ~2 ~11 ~4 ~2 ~12 minecraft:stone_bricks
fill ~4 ~2 ~15 ~4 ~4 ~15 minecraft:stone_bricks
setblock ~5 ~2 ~4 minecraft:mossy_stone_bricks
fill ~5 ~2 ~14 ~7 ~2 ~14 minecraft:stone_bricks
fill ~6 ~2 ~4 ~7 ~2 ~4 minecraft:stone_bricks
setblock ~9 ~2 ~4 minecraft:mossy_stone_bricks
setblock ~9 ~2 ~14 minecraft:stone_bricks
setblock ~10 ~2 ~4 minecraft:cracked_stone_bricks
fill ~11 ~2 ~14 ~11 ~6 ~14 minecraft:stone_bricks
fill ~13 ~2 ~4 ~13 ~3 ~4 minecraft:stone_bricks
setblock ~13 ~2 ~14 minecraft:mossy_stone_bricks
setblock ~14 ~2 ~14 minecraft:stone_bricks
setblock ~16 ~2 ~3 minecraft:mossy_stone_bricks
fill ~16 ~2 ~4 ~16 ~2 ~5 minecraft:stone_bricks
setblock ~16 ~2 ~8 minecraft:cracked_stone_bricks
fill ~16 ~2 ~11 ~16 ~2 ~13 minecraft:stone_bricks
setblock ~16 ~2 ~14 minecraft:cracked_stone_bricks
setblock ~16 ~2 ~15 minecraft:stone_bricks
setblock ~4 ~3 ~3 minecraft:cracked_stone_bricks
fill ~4 ~3 ~4 ~5 ~4 ~4 minecraft:stone_bricks
fill ~4 ~3 ~11 ~4 ~4 ~11 minecraft:stone_bricks
fill ~4 ~3 ~12 ~4 ~6 ~12 minecraft:mossy_stone_bricks
fill ~4 ~3 ~13 ~4 ~4 ~13 minecraft:stone_bricks
fill ~4 ~3 ~14 ~4 ~4 ~14 minecraft:mossy_stone_bricks
fill ~5 ~3 ~14 ~6 ~4 ~14 minecraft:stone_bricks
setblock ~6 ~3 ~4 minecraft:cracked_stone_bricks
fill ~7 ~3 ~4 ~7 ~6 ~4 minecraft:stone_bricks
setblock ~7 ~3 ~14 minecraft:mossy_stone_bricks
setblock ~9 ~3 ~4 minecraft:cracked_stone_bricks
setblock ~9 ~3 ~14 minecraft:mossy_stone_bricks
fill ~10 ~3 ~4 ~10 ~5 ~4 minecraft:stone_bricks
fill ~13 ~3 ~14 ~13 ~4 ~14 minecraft:stone_bricks
fill ~14 ~3 ~4 ~14 ~6 ~4 minecraft:stone_bricks
setblock ~14 ~3 ~14 minecraft:mossy_stone_bricks
setblock ~15 ~3 ~4 minecraft:cracked_stone_bricks
fill ~16 ~3 ~3 ~16 ~3 ~4 minecraft:stone_bricks
setblock ~16 ~3 ~5 minecraft:mossy_stone_bricks
fill ~16 ~3 ~6 ~16 ~5 ~6 minecraft:stone_bricks
setblock ~16 ~3 ~7 minecraft:mossy_stone_bricks
fill ~16 ~3 ~8 ~16 ~10 ~8 minecraft:stone_bricks
fill ~16 ~3 ~11 ~16 ~8 ~12 minecraft:stone_bricks
setblock ~16 ~3 ~13 minecraft:cracked_stone_bricks
fill ~16 ~3 ~14 ~16 ~7 ~14 minecraft:stone_bricks
setblock ~16 ~3 ~15 minecraft:mossy_stone_bricks
fill ~4 ~4 ~3 ~4 ~6 ~3 minecraft:stone_bricks
fill ~4 ~4 ~5 ~4 ~7 ~5 minecraft:stone_bricks
setblock ~4 ~4 ~6 minecraft:cracked_stone_bricks
fill ~4 ~4 ~7 ~4 ~6 ~7 minecraft:stone_bricks
fill ~6 ~4 ~4 ~6 ~6 ~4 minecraft:stone_bricks
setblock ~7 ~4 ~14 minecraft:cracked_stone_bricks
fill ~9 ~4 ~4 ~9 ~6 ~4 minecraft:stone_bricks
fill ~9 ~4 ~14 ~9 ~6 ~14 minecraft:stone_bricks
fill ~11 ~4 ~4 ~11 ~5 ~4 minecraft:mossy_stone_bricks
setblock ~13 ~4 ~4 minecraft:mossy_stone_bricks
setblock ~14 ~4 ~14 minecraft:stone_bricks
fill ~15 ~4 ~4 ~16 ~5 ~4 minecraft:stone_bricks
fill ~15 ~4 ~14 ~15 ~5 ~14 minecraft:cracked_stone_bricks
setblock ~16 ~4 ~3 minecraft:mossy_stone_bricks
setblock ~16 ~4 ~5 minecraft:stone_bricks
setblock ~16 ~4 ~7 minecraft:stone_bricks
setblock ~16 ~4 ~13 minecraft:stone_bricks
setblock ~16 ~4 ~15 minecraft:stone_bricks
setblock ~4 ~5 ~4 minecraft:mossy_stone_bricks
fill ~4 ~5 ~6 ~4 ~6 ~6 minecraft:stone_bricks
fill ~4 ~5 ~8 ~4 ~5 ~9 minecraft:mossy_stone_bricks
fill ~4 ~5 ~10 ~4 ~6 ~10 minecraft:stone_bricks
fill ~4 ~5 ~11 ~4 ~6 ~11 minecraft:cracked_stone_bricks
fill ~4 ~5 ~13 ~4 ~5 ~15 minecraft:cracked_stone_bricks
fill ~5 ~5 ~4 ~5 ~6 ~4 minecraft:stone_bricks
fill ~5 ~5 ~14 ~6 ~5 ~14 minecraft:cracked_stone_bricks
fill ~7 ~5 ~14 ~8 ~6 ~14 minecraft:stone_bricks
fill ~8 ~5 ~4 ~8 ~6 ~4 minecraft:stone_bricks
fill ~12 ~5 ~4 ~13 ~5 ~4 minecraft:stone_bricks
setblock ~12 ~5 ~14 minecraft:stone_bricks
fill ~13 ~5 ~14 ~14 ~5 ~14 minecraft:mossy_stone_bricks
fill ~16 ~5 ~3 ~16 ~6 ~3 minecraft:stone_bricks
setblock ~16 ~5 ~5 minecraft:cracked_stone_bricks
setblock ~16 ~5 ~7 minecraft:mossy_stone_bricks
setblock ~16 ~5 ~13 minecraft:cracked_stone_bricks
setblock ~16 ~5 ~15 minecraft:mossy_stone_bricks
fill ~4 ~6 ~4 ~4 ~7 ~4 minecraft:stone_bricks
fill ~4 ~6 ~8 ~4 ~6 ~9 minecraft:stone_bricks
setblock ~4 ~6 ~13 minecraft:mossy_stone_bricks
fill ~4 ~6 ~14 ~6 ~6 ~14 minecraft:stone_bricks
setblock ~4 ~6 ~15 minecraft:stone_bricks
fill ~10 ~6 ~4 ~11 ~6 ~4 minecraft:cracked_stone_bricks
setblock ~10 ~6 ~14 minecraft:mossy_stone_bricks
setblock ~12 ~6 ~4 minecraft:stone_bricks
fill ~12 ~6 ~14 ~13 ~6 ~14 minecraft:mossy_stone_bricks
setblock ~13 ~6 ~4 minecraft:cracked_stone_bricks
fill ~14 ~6 ~14 ~15 ~6 ~14 minecraft:stone_bricks
setblock ~15 ~6 ~4 minecraft:mossy_stone_bricks
setblock ~16 ~6 ~4 minecraft:cracked_stone_bricks
fill ~16 ~6 ~5 ~16 ~6 ~6 minecraft:mossy_stone_bricks
setblock ~16 ~6 ~7 minecraft:cracked_stone_bricks
setblock ~16 ~6 ~10 minecraft:mossy_stone_bricks
fill ~16 ~6 ~13 ~16 ~7 ~13 minecraft:stone_bricks
setblock ~16 ~6 ~15 minecraft:stone_bricks
fill ~4 ~7 ~6 ~4 ~7 ~8 minecraft:mossy_stone_bricks
setblock ~4 ~7 ~9 minecraft:stone_bricks
setblock ~4 ~7 ~10 minecraft:cracked_stone_bricks
fill ~4 ~7 ~11 ~4 ~7 ~13 minecraft:stone_bricks
setblock ~4 ~7 ~14 minecraft:mossy_stone_bricks
fill ~16 ~7 ~4 ~16 ~7 ~5 minecraft:stone_bricks
fill ~16 ~7 ~6 ~16 ~8 ~6 minecraft:mossy_stone_bricks
fill ~16 ~7 ~7 ~16 ~10 ~7 minecraft:stone_bricks
fill ~16 ~7 ~10 ~16 ~9 ~10 minecraft:stone_bricks
setblock ~4 ~8 ~5 minecraft:cracked_stone_bricks
fill ~4 ~8 ~6 ~4 ~8 ~7 minecraft:mossy_stone_bricks
fill ~4 ~8 ~8 ~4 ~11 ~8 minecraft:stone_bricks
setblock ~4 ~8 ~10 minecraft:stone_bricks
fill ~4 ~8 ~11 ~4 ~8 ~13 minecraft:mossy_stone_bricks
setblock ~16 ~8 ~5 minecraft:stone_bricks
setblock ~16 ~8 ~13 minecraft:cracked_stone_bricks
setblock ~4 ~9 ~6 minecraft:stone_bricks
fill ~4 ~9 ~7 ~4 ~10 ~7 minecraft:mossy_stone_bricks
setblock ~4 ~9 ~10 minecraft:cracked_stone_bricks
fill ~4 ~9 ~11 ~4 ~9 ~12 minecraft:stone_bricks
setblock ~16 ~9 ~6 minecraft:stone_bricks
fill ~16 ~9 ~11 ~16 ~9 ~12 minecraft:mossy_stone_bricks
fill ~4 ~10 ~10 ~4 ~10 ~11 minecraft:mossy_stone_bricks
fill ~16 ~10 ~10 ~16 ~11 ~10 minecraft:mossy_stone_bricks
setblock ~16 ~10 ~11 minecraft:cracked_stone_bricks
setblock ~4 ~11 ~9 minecraft:cracked_stone_bricks
setblock ~4 ~11 ~10 minecraft:stone_bricks
setblock ~16 ~11 ~8 minecraft:cracked_stone_bricks
setblock ~4 ~12 ~9 minecraft:mossy_stone_bricks

# the gable copings: stone brick stairs following the roof, one block proud of it, a chiseled block at the apex
setblock ~4 ~7 ~3 minecraft:stone_brick_stairs[facing=south]
setblock ~4 ~7 ~15 minecraft:stone_brick_stairs[facing=north]
setblock ~16 ~7 ~3 minecraft:stone_brick_stairs[facing=south]
setblock ~16 ~7 ~15 minecraft:stone_brick_stairs[facing=north]
setblock ~4 ~8 ~4 minecraft:stone_brick_stairs[facing=south]
setblock ~4 ~8 ~14 minecraft:stone_brick_stairs[facing=north]
setblock ~16 ~8 ~4 minecraft:stone_brick_stairs[facing=south]
setblock ~16 ~8 ~14 minecraft:stone_brick_stairs[facing=north]
setblock ~4 ~9 ~5 minecraft:stone_brick_stairs[facing=south]
setblock ~4 ~9 ~13 minecraft:stone_brick_stairs[facing=north]
setblock ~16 ~9 ~5 minecraft:stone_brick_stairs[facing=south]
setblock ~16 ~9 ~13 minecraft:stone_brick_stairs[facing=north]
setblock ~4 ~10 ~6 minecraft:stone_brick_stairs[facing=south]
setblock ~4 ~10 ~12 minecraft:stone_brick_stairs[facing=north]
setblock ~16 ~10 ~6 minecraft:stone_brick_stairs[facing=south]
setblock ~16 ~10 ~12 minecraft:stone_brick_stairs[facing=north]
setblock ~4 ~11 ~7 minecraft:stone_brick_stairs[facing=south]
setblock ~4 ~11 ~11 minecraft:stone_brick_stairs[facing=north]
setblock ~16 ~11 ~7 minecraft:stone_brick_stairs[facing=south]
setblock ~16 ~11 ~11 minecraft:stone_brick_stairs[facing=north]
setblock ~4 ~12 ~8 minecraft:stone_brick_stairs[facing=south]
setblock ~4 ~12 ~10 minecraft:stone_brick_stairs[facing=north]
setblock ~4 ~13 ~9 minecraft:chiseled_stone_bricks

# buttresses on the long walls, with sloped tops
setblock ~6 ~1 ~3 minecraft:cracked_stone_bricks
fill ~6 ~1 ~15 ~6 ~2 ~15 minecraft:mossy_stone_bricks
fill ~10 ~1 ~3 ~10 ~4 ~3 minecraft:stone_bricks
fill ~10 ~1 ~15 ~10 ~4 ~15 minecraft:stone_bricks
setblock ~14 ~1 ~3 minecraft:stone_bricks
setblock ~14 ~1 ~15 minecraft:mossy_stone_bricks
setblock ~6 ~2 ~3 minecraft:mossy_stone_bricks
fill ~14 ~2 ~3 ~14 ~4 ~3 minecraft:mossy_stone_bricks
fill ~14 ~2 ~15 ~14 ~4 ~15 minecraft:stone_bricks
setblock ~6 ~3 ~3 minecraft:stone_bricks
fill ~6 ~3 ~15 ~6 ~4 ~15 minecraft:stone_bricks
setblock ~6 ~4 ~3 minecraft:mossy_stone_bricks
setblock ~6 ~5 ~3 minecraft:stone_brick_stairs[facing=south]
setblock ~6 ~5 ~15 minecraft:stone_brick_stairs[facing=north]
setblock ~10 ~5 ~3 minecraft:stone_brick_stairs[facing=south]
setblock ~10 ~5 ~15 minecraft:stone_brick_stairs[facing=north]
setblock ~14 ~5 ~3 minecraft:stone_brick_stairs[facing=south]
setblock ~14 ~5 ~15 minecraft:stone_brick_stairs[facing=north]

# the roof: deepslate tile stairs from the eaves (y 6, overhanging) to a deepslate tile ridge at y 12
setblock ~5 ~6 ~3 minecraft:deepslate_tile_stairs[facing=south]
fill ~5 ~6 ~15 ~9 ~6 ~15 minecraft:deepslate_tile_stairs[facing=north]
setblock ~6 ~6 ~3 minecraft:deepslate_brick_stairs[facing=south]
fill ~7 ~6 ~3 ~15 ~6 ~3 minecraft:deepslate_tile_stairs[facing=south]
fill ~10 ~6 ~15 ~11 ~6 ~15 minecraft:deepslate_brick_stairs[facing=north]
fill ~12 ~6 ~15 ~15 ~6 ~15 minecraft:deepslate_tile_stairs[facing=north]
fill ~5 ~7 ~4 ~11 ~7 ~4 minecraft:deepslate_tile_stairs[facing=south]
fill ~5 ~7 ~14 ~10 ~7 ~14 minecraft:deepslate_tile_stairs[facing=north]
setblock ~11 ~7 ~14 minecraft:deepslate_brick_stairs[facing=north]
setblock ~12 ~7 ~4 minecraft:deepslate_brick_stairs[facing=south]
fill ~12 ~7 ~14 ~15 ~7 ~14 minecraft:deepslate_tile_stairs[facing=north]
fill ~13 ~7 ~4 ~15 ~7 ~4 minecraft:deepslate_tile_stairs[facing=south]
fill ~5 ~8 ~5 ~15 ~8 ~5 minecraft:deepslate_tile_stairs[facing=south]
fill ~5 ~8 ~13 ~15 ~8 ~13 minecraft:deepslate_tile_stairs[facing=north]
setblock ~5 ~9 ~6 minecraft:deepslate_brick_stairs[facing=south]
setblock ~5 ~9 ~12 minecraft:deepslate_brick_stairs[facing=north]
fill ~6 ~9 ~6 ~8 ~9 ~6 minecraft:deepslate_tile_stairs[facing=south]
fill ~6 ~9 ~12 ~10 ~9 ~12 minecraft:deepslate_tile_stairs[facing=north]
fill ~9 ~9 ~6 ~11 ~9 ~6 minecraft:deepslate_brick_stairs[facing=south]
setblock ~11 ~9 ~12 minecraft:deepslate_brick_stairs[facing=north]
fill ~12 ~9 ~6 ~15 ~9 ~6 minecraft:deepslate_tile_stairs[facing=south]
setblock ~12 ~9 ~12 minecraft:deepslate_tile_stairs[facing=north]
setblock ~13 ~9 ~12 minecraft:deepslate_brick_stairs[facing=north]
fill ~14 ~9 ~12 ~15 ~9 ~12 minecraft:deepslate_tile_stairs[facing=north]
setblock ~5 ~10 ~7 minecraft:deepslate_tile_stairs[facing=south]
fill ~5 ~10 ~11 ~9 ~10 ~11 minecraft:deepslate_tile_stairs[facing=north]
setblock ~6 ~10 ~7 minecraft:deepslate_brick_stairs[facing=south]
fill ~7 ~10 ~7 ~11 ~10 ~7 minecraft:deepslate_tile_stairs[facing=south]
setblock ~10 ~10 ~11 minecraft:deepslate_brick_stairs[facing=north]
fill ~11 ~10 ~11 ~12 ~10 ~11 minecraft:deepslate_tile_stairs[facing=north]
setblock ~12 ~10 ~7 minecraft:deepslate_brick_stairs[facing=south]
fill ~13 ~10 ~7 ~15 ~10 ~7 minecraft:deepslate_tile_stairs[facing=south]
setblock ~13 ~10 ~11 minecraft:deepslate_brick_stairs[facing=north]
fill ~14 ~10 ~11 ~15 ~10 ~11 minecraft:deepslate_tile_stairs[facing=north]
fill ~5 ~11 ~8 ~14 ~11 ~8 minecraft:deepslate_tile_stairs[facing=south]
fill ~5 ~11 ~10 ~15 ~11 ~10 minecraft:deepslate_tile_stairs[facing=north]
setblock ~15 ~11 ~8 minecraft:deepslate_brick_stairs[facing=south]
fill ~5 ~12 ~9 ~15 ~12 ~9 minecraft:deepslate_tiles

# the west door: an arched opening at z 8..10 under a carved portal, which stands out one block
fill ~3 ~1 ~7 ~3 ~4 ~7 minecraft:stone_bricks
fill ~3 ~1 ~11 ~3 ~4 ~11 minecraft:stone_bricks
fill ~4 ~1 ~8 ~4 ~3 ~10 minecraft:air
setblock ~4 ~4 ~8 minecraft:stone_brick_stairs[facing=north,half=top]
setblock ~4 ~4 ~9 minecraft:air
setblock ~4 ~4 ~10 minecraft:stone_brick_stairs[facing=south,half=top]
setblock ~3 ~5 ~7 minecraft:chiseled_stone_bricks
fill ~3 ~5 ~8 ~3 ~5 ~10 minecraft:stone_bricks
setblock ~3 ~5 ~11 minecraft:chiseled_stone_bricks
fill ~3 ~6 ~7 ~3 ~6 ~11 minecraft:stone_brick_stairs[facing=east]

# windows: lancets in the long walls and one in the west gable (light gray glass panes)
fill ~8 ~2 ~4 ~8 ~4 ~4 minecraft:light_gray_stained_glass_pane
fill ~8 ~2 ~14 ~8 ~4 ~14 minecraft:light_gray_stained_glass_pane
fill ~12 ~2 ~4 ~12 ~4 ~4 minecraft:light_gray_stained_glass_pane
fill ~12 ~2 ~14 ~12 ~4 ~14 minecraft:light_gray_stained_glass_pane
fill ~4 ~8 ~9 ~4 ~10 ~9 minecraft:light_gray_stained_glass_pane

# the hearth on the east wall: a campfire behind an iron bar screen, a stone breast inside and a stack outside; the flue rises to y 13
setblock ~15 ~1 ~9 minecraft:iron_bars
setblock ~17 ~1 ~8 minecraft:cracked_stone_bricks
fill ~17 ~1 ~9 ~17 ~1 ~10 minecraft:mossy_stone_bricks
setblock ~15 ~2 ~8 minecraft:stone_brick_stairs[facing=east,half=top]
fill ~15 ~2 ~9 ~15 ~11 ~9 minecraft:stone_bricks
setblock ~15 ~2 ~10 minecraft:stone_brick_stairs[facing=east,half=top]
fill ~16 ~2 ~9 ~16 ~13 ~9 minecraft:air
fill ~17 ~2 ~8 ~17 ~3 ~8 minecraft:mossy_stone_bricks
setblock ~17 ~2 ~9 minecraft:stone_bricks
setblock ~17 ~2 ~10 minecraft:mossy_stone_bricks
setblock ~17 ~3 ~9 minecraft:cracked_stone_bricks
fill ~17 ~3 ~10 ~17 ~6 ~10 minecraft:stone_bricks
fill ~17 ~4 ~8 ~17 ~4 ~9 minecraft:stone_bricks
fill ~17 ~5 ~8 ~17 ~7 ~8 minecraft:stone_bricks
setblock ~17 ~5 ~9 minecraft:cracked_stone_bricks
fill ~17 ~6 ~9 ~17 ~9 ~9 minecraft:stone_bricks
setblock ~17 ~7 ~10 minecraft:mossy_stone_bricks
fill ~17 ~8 ~8 ~17 ~9 ~8 minecraft:mossy_stone_bricks
fill ~17 ~8 ~10 ~17 ~9 ~10 minecraft:stone_bricks
fill ~17 ~10 ~8 ~17 ~13 ~8 minecraft:stone_bricks
setblock ~17 ~10 ~9 minecraft:mossy_stone_bricks
setblock ~17 ~10 ~10 minecraft:cracked_stone_bricks
fill ~17 ~11 ~9 ~17 ~11 ~10 minecraft:stone_bricks
fill ~16 ~12 ~8 ~16 ~13 ~8 minecraft:stone_bricks
fill ~16 ~12 ~10 ~16 ~13 ~10 minecraft:stone_bricks
fill ~17 ~12 ~9 ~17 ~13 ~9 minecraft:stone_bricks
setblock ~17 ~12 ~10 minecraft:mossy_stone_bricks
setblock ~15 ~13 ~9 minecraft:stone_bricks
setblock ~17 ~13 ~10 minecraft:cracked_stone_bricks

# tie beams across the hall at the wall head (dark oak logs)
fill ~7 ~6 ~5 ~7 ~6 ~13 minecraft:dark_oak_log[axis=z]
fill ~13 ~6 ~5 ~13 ~6 ~13 minecraft:dark_oak_log[axis=z]

# bookshelves along the long walls, three high, with a dark oak cornice; a free-standing case behind the desk
fill ~5 ~1 ~5 ~7 ~3 ~5 minecraft:bookshelf
fill ~5 ~1 ~13 ~7 ~3 ~13 minecraft:bookshelf
fill ~9 ~1 ~5 ~11 ~3 ~5 minecraft:bookshelf
fill ~9 ~1 ~13 ~11 ~3 ~13 minecraft:bookshelf
fill ~12 ~1 ~8 ~12 ~3 ~11 minecraft:bookshelf
fill ~13 ~1 ~5 ~15 ~3 ~5 minecraft:bookshelf
fill ~13 ~1 ~13 ~15 ~3 ~13 minecraft:bookshelf
fill ~5 ~4 ~5 ~7 ~4 ~5 minecraft:dark_oak_slab[type=bottom]
fill ~5 ~4 ~13 ~7 ~4 ~13 minecraft:dark_oak_slab[type=bottom]
fill ~9 ~4 ~5 ~11 ~4 ~5 minecraft:dark_oak_slab[type=bottom]
fill ~9 ~4 ~13 ~11 ~4 ~13 minecraft:dark_oak_slab[type=bottom]
fill ~12 ~4 ~8 ~12 ~4 ~11 minecraft:dark_oak_slab[type=bottom]
fill ~13 ~4 ~5 ~15 ~4 ~5 minecraft:dark_oak_slab[type=bottom]
fill ~13 ~4 ~13 ~15 ~4 ~13 minecraft:dark_oak_slab[type=bottom]

# the Archivist's desk at x 8, between him and the door
fill ~8 ~1 ~8 ~8 ~1 ~11 minecraft:dark_oak_slab[type=top]

# the court of the Founders: low walls with a stone brick wall cap, a gate on the west, piers at the corners
fill ~9 ~0 ~17 ~21 ~0 ~17 minecraft:stone_bricks
fill ~9 ~0 ~18 ~9 ~0 ~21 minecraft:stone_bricks
fill ~9 ~0 ~25 ~9 ~0 ~29 minecraft:stone_bricks
fill ~10 ~0 ~29 ~20 ~0 ~29 minecraft:stone_bricks
fill ~21 ~0 ~18 ~21 ~0 ~29 minecraft:stone_bricks
fill ~9 ~1 ~17 ~10 ~1 ~17 minecraft:mossy_stone_bricks
fill ~9 ~1 ~18 ~9 ~2 ~18 minecraft:stone_bricks
fill ~9 ~1 ~19 ~9 ~1 ~20 minecraft:mossy_stone_bricks
setblock ~9 ~1 ~21 minecraft:stone_bricks
setblock ~9 ~1 ~25 minecraft:mossy_stone_bricks
fill ~9 ~1 ~26 ~9 ~1 ~28 minecraft:stone_bricks
setblock ~9 ~1 ~29 minecraft:mossy_stone_bricks
setblock ~10 ~1 ~29 minecraft:mossy_cobblestone
setblock ~11 ~1 ~17 minecraft:stone_bricks
fill ~11 ~1 ~29 ~12 ~1 ~29 minecraft:stone_bricks
setblock ~12 ~1 ~17 minecraft:mossy_cobblestone
setblock ~13 ~1 ~17 minecraft:mossy_stone_bricks
setblock ~13 ~1 ~29 minecraft:cracked_stone_bricks
fill ~14 ~1 ~17 ~14 ~2 ~17 minecraft:stone_bricks
fill ~14 ~1 ~29 ~15 ~1 ~29 minecraft:mossy_stone_bricks
setblock ~15 ~1 ~17 minecraft:mossy_cobblestone
setblock ~16 ~1 ~17 minecraft:mossy_stone_bricks
fill ~16 ~1 ~29 ~17 ~2 ~29 minecraft:stone_bricks
fill ~17 ~1 ~17 ~21 ~1 ~17 minecraft:stone_bricks
setblock ~18 ~1 ~29 minecraft:mossy_stone_bricks
fill ~19 ~1 ~29 ~21 ~1 ~29 minecraft:stone_bricks
setblock ~21 ~1 ~18 minecraft:mossy_cobblestone
setblock ~21 ~1 ~19 minecraft:mossy_stone_bricks
fill ~21 ~1 ~20 ~21 ~2 ~20 minecraft:stone_bricks
fill ~21 ~1 ~21 ~21 ~2 ~21 minecraft:mossy_stone_bricks
setblock ~21 ~1 ~22 minecraft:mossy_cobblestone
fill ~21 ~1 ~23 ~21 ~1 ~27 minecraft:stone_bricks
fill ~21 ~1 ~28 ~21 ~2 ~28 minecraft:mossy_stone_bricks
setblock ~9 ~2 ~17 minecraft:mossy_cobblestone
setblock ~9 ~2 ~19 minecraft:cracked_stone_bricks
setblock ~9 ~2 ~20 minecraft:stone_bricks
setblock ~9 ~2 ~21 minecraft:mossy_stone_bricks
fill ~9 ~2 ~25 ~9 ~2 ~26 minecraft:stone_bricks
setblock ~9 ~2 ~27 minecraft:mossy_stone_bricks
fill ~9 ~2 ~28 ~9 ~2 ~29 minecraft:stone_bricks
setblock ~10 ~2 ~17 minecraft:cracked_stone_bricks
fill ~10 ~2 ~29 ~12 ~2 ~29 minecraft:mossy_stone_bricks
setblock ~11 ~2 ~17 minecraft:mossy_stone_bricks
fill ~12 ~2 ~17 ~13 ~2 ~17 minecraft:stone_bricks
fill ~13 ~2 ~29 ~14 ~2 ~29 minecraft:stone_bricks
setblock ~15 ~2 ~17 minecraft:cracked_stone_bricks
setblock ~15 ~2 ~29 minecraft:cracked_stone_bricks
fill ~16 ~2 ~17 ~19 ~2 ~17 minecraft:stone_bricks
setblock ~18 ~2 ~29 minecraft:cracked_stone_bricks
setblock ~19 ~2 ~29 minecraft:stone_bricks
setblock ~20 ~2 ~17 minecraft:cracked_stone_bricks
setblock ~20 ~2 ~29 minecraft:mossy_stone_bricks
fill ~21 ~2 ~17 ~21 ~3 ~17 minecraft:stone_bricks
setblock ~21 ~2 ~18 minecraft:mossy_stone_bricks
setblock ~21 ~2 ~19 minecraft:stone_bricks
fill ~21 ~2 ~22 ~21 ~2 ~24 minecraft:stone_bricks
setblock ~21 ~2 ~25 minecraft:mossy_stone_bricks
setblock ~21 ~2 ~26 minecraft:cracked_stone_bricks
setblock ~21 ~2 ~27 minecraft:mossy_stone_bricks
fill ~21 ~2 ~29 ~21 ~3 ~29 minecraft:stone_bricks
setblock ~9 ~3 ~17 minecraft:stone_bricks
fill ~9 ~3 ~18 ~9 ~3 ~19 minecraft:stone_brick_wall
setblock ~9 ~3 ~20 minecraft:mossy_stone_brick_wall
setblock ~9 ~3 ~21 minecraft:stone_bricks
setblock ~9 ~3 ~25 minecraft:stone_bricks
setblock ~9 ~3 ~26 minecraft:stone_brick_wall
setblock ~9 ~3 ~27 minecraft:mossy_stone_brick_wall
setblock ~9 ~3 ~28 minecraft:stone_brick_wall
setblock ~9 ~3 ~29 minecraft:stone_bricks
fill ~10 ~3 ~17 ~14 ~3 ~17 minecraft:stone_brick_wall
fill ~10 ~3 ~29 ~11 ~3 ~29 minecraft:stone_brick_wall
setblock ~12 ~3 ~29 minecraft:mossy_stone_brick_wall
fill ~13 ~3 ~29 ~20 ~3 ~29 minecraft:stone_brick_wall
setblock ~15 ~3 ~17 minecraft:mossy_stone_brick_wall
fill ~16 ~3 ~17 ~17 ~3 ~17 minecraft:stone_brick_wall
fill ~18 ~3 ~17 ~19 ~3 ~17 minecraft:mossy_stone_brick_wall
setblock ~20 ~3 ~17 minecraft:stone_brick_wall
setblock ~21 ~3 ~18 minecraft:stone_brick_wall
setblock ~21 ~3 ~19 minecraft:mossy_stone_brick_wall
fill ~21 ~3 ~20 ~21 ~3 ~21 minecraft:stone_brick_wall
fill ~21 ~3 ~22 ~21 ~3 ~24 minecraft:mossy_stone_brick_wall
setblock ~21 ~3 ~25 minecraft:stone_brick_wall
setblock ~21 ~3 ~26 minecraft:mossy_stone_brick_wall
fill ~21 ~3 ~27 ~21 ~3 ~28 minecraft:stone_brick_wall
setblock ~9 ~4 ~17 minecraft:chiseled_stone_bricks
setblock ~9 ~4 ~21 minecraft:chiseled_stone_bricks
setblock ~9 ~4 ~25 minecraft:chiseled_stone_bricks
setblock ~9 ~4 ~29 minecraft:chiseled_stone_bricks
setblock ~21 ~4 ~17 minecraft:chiseled_stone_bricks
setblock ~21 ~4 ~29 minecraft:chiseled_stone_bricks

# the court floor: old flagstones wall to wall, the moss creeping in from the walls, a clean flagged path from the gate to the dais
fill ~9 ~0 ~22 ~10 ~0 ~23 minecraft:stone_bricks
setblock ~9 ~0 ~24 minecraft:stone_bricks
fill ~10 ~0 ~18 ~12 ~0 ~18 minecraft:mossy_stone_bricks
fill ~10 ~0 ~19 ~11 ~0 ~19 minecraft:mossy_stone_bricks
fill ~10 ~0 ~20 ~11 ~0 ~20 minecraft:stone_bricks
fill ~10 ~0 ~21 ~11 ~0 ~21 minecraft:mossy_stone_bricks
setblock ~10 ~0 ~24 minecraft:cracked_stone_bricks
fill ~10 ~0 ~25 ~10 ~0 ~26 minecraft:mossy_stone_bricks
fill ~10 ~0 ~27 ~10 ~0 ~28 minecraft:stone_bricks
fill ~11 ~0 ~22 ~11 ~0 ~23 minecraft:cracked_stone_bricks
fill ~11 ~0 ~24 ~11 ~0 ~25 minecraft:stone_bricks
setblock ~11 ~0 ~26 minecraft:mossy_stone_bricks
fill ~11 ~0 ~27 ~12 ~0 ~27 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~28 minecraft:mossy_stone_bricks
setblock ~12 ~0 ~19 minecraft:stone_bricks
setblock ~12 ~0 ~20 minecraft:cracked_stone_bricks
fill ~12 ~0 ~21 ~12 ~0 ~24 minecraft:stone_bricks
setblock ~12 ~0 ~25 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~26 minecraft:stone_bricks
setblock ~12 ~0 ~28 minecraft:cracked_stone_bricks
fill ~13 ~0 ~18 ~13 ~0 ~20 minecraft:stone_bricks
setblock ~13 ~0 ~26 minecraft:mossy_stone_bricks
fill ~13 ~0 ~27 ~13 ~0 ~28 minecraft:stone_bricks
setblock ~14 ~0 ~18 minecraft:mossy_stone_bricks
fill ~14 ~0 ~19 ~14 ~0 ~20 minecraft:stone_bricks
fill ~14 ~0 ~26 ~14 ~0 ~27 minecraft:stone_bricks
setblock ~14 ~0 ~28 minecraft:mossy_stone_bricks
fill ~15 ~0 ~18 ~15 ~0 ~20 minecraft:stone_bricks
setblock ~15 ~0 ~26 minecraft:cracked_stone_bricks
fill ~15 ~0 ~27 ~15 ~0 ~28 minecraft:stone_bricks
setblock ~16 ~0 ~18 minecraft:mossy_stone_bricks
fill ~16 ~0 ~19 ~16 ~0 ~20 minecraft:stone_bricks
fill ~16 ~0 ~26 ~16 ~0 ~27 minecraft:stone_bricks
fill ~16 ~0 ~28 ~17 ~0 ~28 minecraft:mossy_stone_bricks
fill ~17 ~0 ~18 ~17 ~0 ~19 minecraft:stone_bricks
setblock ~17 ~0 ~20 minecraft:mossy_stone_bricks
setblock ~17 ~0 ~26 minecraft:mossy_stone_bricks
setblock ~17 ~0 ~27 minecraft:cracked_stone_bricks
fill ~18 ~0 ~18 ~20 ~0 ~18 minecraft:mossy_stone_bricks
fill ~18 ~0 ~19 ~19 ~0 ~19 minecraft:stone_bricks
setblock ~18 ~0 ~20 minecraft:stone_bricks
fill ~18 ~0 ~22 ~18 ~0 ~23 minecraft:cracked_stone_bricks
fill ~18 ~0 ~24 ~19 ~0 ~24 minecraft:stone_bricks
fill ~19 ~0 ~20 ~19 ~0 ~21 minecraft:cracked_stone_bricks
setblock ~19 ~0 ~22 minecraft:mossy_stone_bricks
fill ~19 ~0 ~23 ~20 ~0 ~23 minecraft:stone_bricks
fill ~19 ~0 ~25 ~20 ~0 ~25 minecraft:mossy_stone_bricks
fill ~20 ~0 ~19 ~20 ~0 ~22 minecraft:mossy_stone_bricks
setblock ~20 ~0 ~24 minecraft:cracked_stone_bricks

# the dais of the Founders' Stone: polished deepslate in a deepslate tile ring
fill ~13 ~0 ~21 ~17 ~0 ~21 minecraft:deepslate_tiles
fill ~13 ~0 ~22 ~13 ~0 ~25 minecraft:deepslate_tiles
fill ~14 ~0 ~22 ~16 ~0 ~24 minecraft:polished_deepslate
fill ~14 ~0 ~25 ~16 ~0 ~25 minecraft:deepslate_tiles
fill ~17 ~0 ~22 ~17 ~0 ~25 minecraft:deepslate_tiles

# the Founders' Stone: a lodestone at (15, 1, 23) with chiseled stone bricks around it: four posts two high at the corners, a polished deepslate step on each side; nothing over it, so its label stays in sight
fill ~14 ~1 ~22 ~14 ~2 ~22 minecraft:chiseled_stone_bricks
setblock ~14 ~1 ~23 minecraft:polished_deepslate_stairs[facing=east]
fill ~14 ~1 ~24 ~14 ~2 ~24 minecraft:chiseled_stone_bricks
setblock ~15 ~1 ~22 minecraft:polished_deepslate_stairs[facing=south]
setblock ~15 ~1 ~23 minecraft:lodestone
setblock ~15 ~1 ~24 minecraft:polished_deepslate_stairs[facing=north]
fill ~16 ~1 ~22 ~16 ~2 ~22 minecraft:chiseled_stone_bricks
setblock ~16 ~1 ~23 minecraft:polished_deepslate_stairs[facing=west]
fill ~16 ~1 ~24 ~16 ~2 ~24 minecraft:chiseled_stone_bricks

# a dead tree in the south-east corner of the court: a bare spruce trunk with thin crooked twigs (dark oak fence), in a square bed of rooted earth
fill ~18 ~0 ~26 ~20 ~0 ~28 minecraft:rooted_dirt
fill ~19 ~1 ~27 ~19 ~5 ~27 minecraft:spruce_log

# its twigs, which hang on the trunk
fill ~17 ~4 ~27 ~18 ~4 ~27 minecraft:dark_oak_fence
setblock ~19 ~4 ~28 minecraft:dark_oak_fence
setblock ~17 ~5 ~27 minecraft:dark_oak_fence
fill ~20 ~5 ~27 ~20 ~6 ~27 minecraft:dark_oak_fence
fill ~19 ~6 ~27 ~19 ~7 ~27 minecraft:dark_oak_fence

# a woodpile by the chimney stack (bare earth only under the logs)
fill ~18 ~0 ~5 ~19 ~0 ~7 minecraft:coarse_dirt
fill ~18 ~1 ~5 ~19 ~2 ~7 minecraft:spruce_log[axis=z]
fill ~18 ~3 ~6 ~19 ~3 ~6 minecraft:spruce_log[axis=z]

# banner poles flanking the portal and the Stone (dark oak fences on stone)
setblock ~1 ~0 ~5 minecraft:polished_andesite
setblock ~1 ~0 ~13 minecraft:polished_andesite
setblock ~18 ~0 ~21 minecraft:polished_andesite
setblock ~18 ~0 ~25 minecraft:polished_andesite
fill ~1 ~1 ~5 ~1 ~3 ~5 minecraft:dark_oak_fence
fill ~1 ~1 ~13 ~1 ~3 ~13 minecraft:dark_oak_fence
fill ~18 ~1 ~21 ~18 ~3 ~21 minecraft:dark_oak_fence
fill ~18 ~1 ~25 ~18 ~3 ~25 minecraft:dark_oak_fence

# ivy on the north wall of the hall and the court walls (vines hung south, on full stone blocks)
fill ~16 ~1 ~16 ~16 ~2 ~16 minecraft:vine[south=true]
fill ~5 ~2 ~3 ~5 ~5 ~3 minecraft:vine[south=true]
fill ~11 ~2 ~3 ~11 ~5 ~3 minecraft:vine[south=true]
fill ~11 ~2 ~16 ~12 ~2 ~16 minecraft:vine[south=true]
fill ~19 ~2 ~16 ~20 ~2 ~16 minecraft:vine[south=true]
fill ~9 ~3 ~3 ~9 ~5 ~3 minecraft:vine[south=true]
fill ~7 ~4 ~3 ~7 ~5 ~3 minecraft:vine[south=true]
fill ~15 ~4 ~3 ~15 ~5 ~3 minecraft:vine[south=true]
setblock ~13 ~5 ~3 minecraft:vine[south=true]

# lanterns: on the court piers and the gate, hung in the portal and under the tie beams
setblock ~3 ~4 ~9 minecraft:lantern[hanging=true]
setblock ~7 ~5 ~6 minecraft:lantern[hanging=true]
setblock ~7 ~5 ~12 minecraft:lantern[hanging=true]
setblock ~9 ~5 ~17 minecraft:lantern
setblock ~9 ~5 ~21 minecraft:lantern
setblock ~9 ~5 ~25 minecraft:lantern
setblock ~9 ~5 ~29 minecraft:lantern
setblock ~13 ~5 ~6 minecraft:lantern[hanging=true]
setblock ~13 ~5 ~12 minecraft:lantern[hanging=true]
setblock ~21 ~5 ~17 minecraft:lantern
setblock ~21 ~5 ~29 minecraft:lantern

# candles: on the desk, and on the four posts of the Stone
setblock ~8 ~2 ~8 minecraft:candle[candles=3,lit=true]
setblock ~8 ~2 ~11 minecraft:candle[candles=2,lit=true]
setblock ~14 ~3 ~22 minecraft:candle[candles=2,lit=true]
setblock ~14 ~3 ~24 minecraft:candle[candles=2,lit=true]
setblock ~16 ~3 ~22 minecraft:candle[candles=3,lit=true]
setblock ~16 ~3 ~24 minecraft:candle[candles=3,lit=true]

# the hearth fire
setblock ~16 ~1 ~9 minecraft:campfire[lit=true]

# banners: on the poles before the portal and beside the Stone, and over the hearth
setblock ~1 ~4 ~5 minecraft:black_banner[rotation=4]{Patterns:[{Pattern:"bs",Color:7},{Pattern:"bo",Color:4},{Pattern:"mr",Color:4}]}
setblock ~1 ~4 ~13 minecraft:black_banner[rotation=4]{Patterns:[{Pattern:"bs",Color:7},{Pattern:"bo",Color:4},{Pattern:"mr",Color:4}]}
setblock ~18 ~4 ~21 minecraft:black_banner[rotation=4]{Patterns:[{Pattern:"bo",Color:4},{Pattern:"cbo",Color:15},{Pattern:"mr",Color:4}]}
setblock ~18 ~4 ~25 minecraft:black_banner[rotation=4]{Patterns:[{Pattern:"bo",Color:4},{Pattern:"cbo",Color:15},{Pattern:"mr",Color:4}]}
setblock ~14 ~8 ~9 minecraft:black_wall_banner[facing=west]{Patterns:[{Pattern:"bo",Color:4},{Pattern:"mc",Color:4}]}
