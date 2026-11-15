# size 5 19 31
# ironhold/curtain: curtain wall section: a 12-high wall of dark stone with pilasters under a corbelled parapet, a two-wide wall-walk between merlons and a shoulder, a roofed guard tower at the north end and an open crenellated tower at the south end.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Outer face x 0 (the west); turned 180 degrees for the east wall. Wall z 5..25 (walk x 1..2, feet 12; merlons x 0, y 12..13, odd z); north tower z 0..4 (guard room y 12..13, pyramid roof to y 16, spire y 17..18); south tower z 26..30 (open platform at feet 12, merlons y 12..13).
# Towers: ground room y 1..10, floor y 11 with a ladder hole at (3, 11, 2) and (3, 11, 28); ladder x 3 on the inner wall x 4 from y 1 to y 12; the way in is a door at (4, 1..2, 3) and (4, 1..2, 27) on the inner side; the walk is reached through (2, 12..13, 4) and (1..2, 12..13, 26).

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~1 ~1 ~1 ~3 ~10 ~1 minecraft:air
fill ~1 ~1 ~2 ~2 ~2 ~3 minecraft:air
fill ~1 ~1 ~27 ~3 ~2 ~27 minecraft:air
fill ~1 ~1 ~28 ~2 ~10 ~29 minecraft:air
fill ~3 ~1 ~3 ~3 ~10 ~3 minecraft:air
fill ~3 ~1 ~29 ~3 ~10 ~29 minecraft:air
fill ~0 ~2 ~5 ~0 ~6 ~8 minecraft:air
fill ~0 ~2 ~10 ~0 ~6 ~14 minecraft:air
fill ~0 ~2 ~16 ~0 ~6 ~20 minecraft:air
fill ~0 ~2 ~22 ~0 ~7 ~25 minecraft:air
fill ~1 ~3 ~2 ~2 ~10 ~2 minecraft:air
fill ~1 ~3 ~3 ~1 ~10 ~3 minecraft:air
fill ~1 ~3 ~27 ~1 ~10 ~27 minecraft:air
fill ~3 ~3 ~27 ~3 ~10 ~27 minecraft:air
fill ~2 ~5 ~3 ~2 ~10 ~3 minecraft:air
fill ~2 ~5 ~27 ~2 ~10 ~27 minecraft:air
fill ~0 ~7 ~5 ~0 ~7 ~7 minecraft:air
fill ~0 ~7 ~10 ~0 ~7 ~12 minecraft:air
fill ~0 ~7 ~14 ~0 ~9 ~14 minecraft:air
fill ~0 ~7 ~17 ~0 ~8 ~18 minecraft:air
fill ~0 ~7 ~20 ~0 ~9 ~20 minecraft:air
fill ~0 ~8 ~5 ~0 ~9 ~5 minecraft:air
setblock ~0 ~8 ~7 minecraft:air
fill ~0 ~8 ~10 ~0 ~9 ~10 minecraft:air
setblock ~0 ~8 ~12 minecraft:air
setblock ~0 ~8 ~16 minecraft:air
fill ~0 ~8 ~22 ~0 ~8 ~23 minecraft:air
fill ~0 ~8 ~25 ~0 ~9 ~25 minecraft:air
setblock ~0 ~9 ~8 minecraft:air
setblock ~0 ~9 ~13 minecraft:air
setblock ~0 ~9 ~17 minecraft:air
setblock ~0 ~9 ~19 minecraft:air
setblock ~0 ~9 ~22 minecraft:air
setblock ~0 ~9 ~24 minecraft:air
fill ~0 ~12 ~6 ~0 ~18 ~6 minecraft:air
fill ~0 ~12 ~8 ~0 ~18 ~8 minecraft:air
fill ~0 ~12 ~10 ~0 ~18 ~10 minecraft:air
fill ~0 ~12 ~12 ~0 ~18 ~12 minecraft:air
fill ~0 ~12 ~14 ~0 ~18 ~14 minecraft:air
fill ~0 ~12 ~16 ~0 ~18 ~16 minecraft:air
fill ~0 ~12 ~18 ~0 ~18 ~18 minecraft:air
fill ~0 ~12 ~20 ~0 ~18 ~20 minecraft:air
fill ~0 ~12 ~22 ~0 ~18 ~22 minecraft:air
fill ~0 ~12 ~24 ~0 ~18 ~24 minecraft:air
fill ~1 ~12 ~1 ~3 ~13 ~1 minecraft:air
fill ~1 ~12 ~2 ~2 ~13 ~3 minecraft:air
fill ~1 ~12 ~5 ~2 ~18 ~25 minecraft:air
fill ~1 ~12 ~28 ~2 ~18 ~28 minecraft:air
fill ~1 ~12 ~29 ~1 ~18 ~29 minecraft:air
fill ~2 ~12 ~27 ~3 ~18 ~27 minecraft:air
fill ~3 ~12 ~3 ~3 ~13 ~3 minecraft:air
fill ~3 ~12 ~29 ~3 ~18 ~29 minecraft:air
fill ~0 ~13 ~27 ~1 ~18 ~27 minecraft:air
fill ~0 ~13 ~29 ~0 ~18 ~29 minecraft:air
fill ~1 ~13 ~30 ~1 ~18 ~30 minecraft:air
fill ~2 ~13 ~29 ~2 ~18 ~29 minecraft:air
setblock ~3 ~13 ~2 minecraft:air
fill ~3 ~13 ~5 ~4 ~18 ~8 minecraft:air
fill ~3 ~13 ~9 ~3 ~18 ~26 minecraft:air
fill ~3 ~13 ~28 ~3 ~18 ~28 minecraft:air
fill ~3 ~13 ~30 ~3 ~18 ~30 minecraft:air
fill ~4 ~13 ~10 ~4 ~18 ~20 minecraft:air
fill ~4 ~13 ~22 ~4 ~18 ~25 minecraft:air
fill ~4 ~13 ~27 ~4 ~18 ~27 minecraft:air
fill ~4 ~13 ~29 ~4 ~18 ~29 minecraft:air
fill ~0 ~14 ~26 ~2 ~18 ~26 minecraft:air
fill ~0 ~14 ~28 ~0 ~18 ~28 minecraft:air
fill ~0 ~14 ~30 ~0 ~18 ~30 minecraft:air
fill ~2 ~14 ~30 ~2 ~18 ~30 minecraft:air
fill ~4 ~14 ~9 ~4 ~18 ~9 minecraft:air
fill ~4 ~14 ~21 ~4 ~18 ~21 minecraft:air
fill ~4 ~14 ~26 ~4 ~18 ~26 minecraft:air
fill ~4 ~14 ~28 ~4 ~18 ~28 minecraft:air
fill ~4 ~14 ~30 ~4 ~18 ~30 minecraft:air
fill ~0 ~15 ~0 ~4 ~18 ~0 minecraft:air
fill ~0 ~15 ~1 ~0 ~18 ~5 minecraft:air
fill ~0 ~15 ~7 ~0 ~18 ~7 minecraft:air
fill ~0 ~15 ~9 ~0 ~18 ~9 minecraft:air
fill ~0 ~15 ~11 ~0 ~18 ~11 minecraft:air
fill ~0 ~15 ~13 ~0 ~18 ~13 minecraft:air
fill ~0 ~15 ~15 ~0 ~18 ~15 minecraft:air
fill ~0 ~15 ~17 ~0 ~18 ~17 minecraft:air
fill ~0 ~15 ~19 ~0 ~18 ~19 minecraft:air
fill ~0 ~15 ~21 ~0 ~18 ~21 minecraft:air
fill ~0 ~15 ~23 ~0 ~18 ~23 minecraft:air
fill ~0 ~15 ~25 ~0 ~18 ~25 minecraft:air
fill ~1 ~15 ~4 ~3 ~18 ~4 minecraft:air
fill ~4 ~15 ~1 ~4 ~18 ~4 minecraft:air
fill ~1 ~16 ~1 ~3 ~18 ~1 minecraft:air
fill ~1 ~16 ~2 ~1 ~18 ~3 minecraft:air
fill ~2 ~16 ~3 ~2 ~18 ~3 minecraft:air
fill ~3 ~16 ~2 ~3 ~18 ~3 minecraft:air

# the footing: worn cobble under the wall (x 1..4) and the towers, the pilaster feet at x 0
fill ~0 ~0 ~0 ~1 ~0 ~1 minecraft:cobblestone
fill ~0 ~0 ~2 ~2 ~0 ~2 minecraft:mossy_cobblestone
fill ~0 ~0 ~3 ~1 ~0 ~3 minecraft:cobblestone
setblock ~0 ~0 ~4 minecraft:mossy_cobblestone
fill ~0 ~0 ~9 ~1 ~0 ~9 minecraft:cobblestone
setblock ~0 ~0 ~15 minecraft:cobblestone
setblock ~0 ~0 ~21 minecraft:cobblestone
fill ~0 ~0 ~26 ~0 ~0 ~27 minecraft:cobblestone
setblock ~0 ~0 ~28 minecraft:mossy_cobblestone
setblock ~0 ~0 ~29 minecraft:andesite
setblock ~0 ~0 ~30 minecraft:mossy_cobblestone
setblock ~1 ~0 ~4 minecraft:cobblestone
setblock ~1 ~0 ~5 minecraft:andesite
fill ~1 ~0 ~6 ~1 ~0 ~7 minecraft:mossy_cobblestone
setblock ~1 ~0 ~8 minecraft:andesite
setblock ~1 ~0 ~10 minecraft:cobblestone
setblock ~1 ~0 ~11 minecraft:andesite
setblock ~1 ~0 ~12 minecraft:cobblestone
setblock ~1 ~0 ~13 minecraft:mossy_cobblestone
fill ~1 ~0 ~14 ~1 ~0 ~16 minecraft:cobblestone
setblock ~1 ~0 ~17 minecraft:mossy_cobblestone
setblock ~1 ~0 ~18 minecraft:cobblestone
setblock ~1 ~0 ~19 minecraft:mossy_cobblestone
fill ~1 ~0 ~20 ~2 ~0 ~20 minecraft:cobblestone
setblock ~1 ~0 ~21 minecraft:mossy_cobblestone
setblock ~1 ~0 ~22 minecraft:andesite
fill ~1 ~0 ~23 ~1 ~0 ~30 minecraft:cobblestone
setblock ~2 ~0 ~0 minecraft:mossy_cobblestone
setblock ~2 ~0 ~1 minecraft:cobblestone
fill ~2 ~0 ~3 ~2 ~0 ~4 minecraft:andesite
setblock ~2 ~0 ~5 minecraft:mossy_cobblestone
fill ~2 ~0 ~6 ~3 ~0 ~7 minecraft:cobblestone
fill ~2 ~0 ~8 ~2 ~0 ~13 minecraft:cobblestone
fill ~2 ~0 ~14 ~4 ~0 ~14 minecraft:mossy_cobblestone
fill ~2 ~0 ~15 ~2 ~0 ~16 minecraft:mossy_cobblestone
setblock ~2 ~0 ~17 minecraft:cobblestone
setblock ~2 ~0 ~18 minecraft:mossy_cobblestone
setblock ~2 ~0 ~19 minecraft:andesite
setblock ~2 ~0 ~21 minecraft:cobblestone
fill ~2 ~0 ~22 ~2 ~0 ~24 minecraft:mossy_cobblestone
fill ~2 ~0 ~25 ~3 ~0 ~26 minecraft:cobblestone
setblock ~2 ~0 ~27 minecraft:andesite
fill ~2 ~0 ~28 ~2 ~0 ~29 minecraft:mossy_cobblestone
setblock ~2 ~0 ~30 minecraft:andesite
setblock ~3 ~0 ~0 minecraft:cobblestone
setblock ~3 ~0 ~1 minecraft:mossy_cobblestone
fill ~3 ~0 ~2 ~3 ~0 ~3 minecraft:cobblestone
fill ~3 ~0 ~4 ~3 ~0 ~5 minecraft:mossy_cobblestone
setblock ~3 ~0 ~8 minecraft:mossy_cobblestone
fill ~3 ~0 ~9 ~3 ~0 ~13 minecraft:cobblestone
fill ~3 ~0 ~15 ~4 ~0 ~15 minecraft:cobblestone
setblock ~3 ~0 ~16 minecraft:cobblestone
setblock ~3 ~0 ~17 minecraft:mossy_cobblestone
setblock ~3 ~0 ~18 minecraft:andesite
fill ~3 ~0 ~19 ~3 ~0 ~20 minecraft:cobblestone
setblock ~3 ~0 ~21 minecraft:andesite
setblock ~3 ~0 ~22 minecraft:cobblestone
setblock ~3 ~0 ~23 minecraft:andesite
setblock ~3 ~0 ~24 minecraft:mossy_cobblestone
fill ~3 ~0 ~27 ~4 ~0 ~27 minecraft:cobblestone
fill ~3 ~0 ~28 ~3 ~0 ~29 minecraft:cobblestone
fill ~3 ~0 ~30 ~4 ~0 ~30 minecraft:mossy_cobblestone
setblock ~4 ~0 ~0 minecraft:mossy_cobblestone
fill ~4 ~0 ~1 ~4 ~0 ~2 minecraft:cobblestone
setblock ~4 ~0 ~3 minecraft:mossy_cobblestone
fill ~4 ~0 ~4 ~4 ~0 ~5 minecraft:cobblestone
setblock ~4 ~0 ~6 minecraft:andesite
fill ~4 ~0 ~7 ~4 ~0 ~9 minecraft:cobblestone
setblock ~4 ~0 ~10 minecraft:mossy_cobblestone
setblock ~4 ~0 ~11 minecraft:cobblestone
setblock ~4 ~0 ~12 minecraft:andesite
setblock ~4 ~0 ~13 minecraft:cobblestone
setblock ~4 ~0 ~16 minecraft:andesite
fill ~4 ~0 ~17 ~4 ~0 ~18 minecraft:cobblestone
setblock ~4 ~0 ~19 minecraft:mossy_cobblestone
fill ~4 ~0 ~20 ~4 ~0 ~24 minecraft:cobblestone
fill ~4 ~0 ~25 ~4 ~0 ~26 minecraft:mossy_cobblestone
setblock ~4 ~0 ~28 minecraft:mossy_cobblestone
setblock ~4 ~0 ~29 minecraft:cobblestone

# the wall: stone bricks with cracked ones, moss in the lowest courses; x 1..4, y 1..10, z 5..25
fill ~2 ~1 ~5 ~2 ~1 ~7 minecraft:stone_bricks
setblock ~2 ~1 ~8 minecraft:deepslate_bricks
setblock ~2 ~1 ~9 minecraft:stone_bricks
fill ~2 ~1 ~10 ~2 ~3 ~10 minecraft:mossy_stone_bricks
fill ~2 ~1 ~11 ~3 ~1 ~11 minecraft:cracked_stone_bricks
setblock ~2 ~1 ~12 minecraft:cracked_stone_bricks
fill ~2 ~1 ~13 ~2 ~1 ~15 minecraft:stone_bricks
fill ~2 ~1 ~16 ~2 ~1 ~17 minecraft:mossy_stone_bricks
setblock ~2 ~1 ~18 minecraft:cracked_stone_bricks
fill ~2 ~1 ~19 ~2 ~1 ~23 minecraft:stone_bricks
fill ~2 ~1 ~24 ~2 ~2 ~24 minecraft:mossy_stone_bricks
setblock ~2 ~1 ~25 minecraft:stone_bricks
setblock ~3 ~1 ~5 minecraft:cracked_stone_bricks
fill ~3 ~1 ~6 ~3 ~2 ~6 minecraft:mossy_stone_bricks
fill ~3 ~1 ~7 ~3 ~1 ~10 minecraft:stone_bricks
setblock ~3 ~1 ~12 minecraft:mossy_stone_bricks
setblock ~3 ~1 ~13 minecraft:cracked_stone_bricks
fill ~3 ~1 ~14 ~3 ~1 ~16 minecraft:stone_bricks
setblock ~3 ~1 ~17 minecraft:cracked_stone_bricks
fill ~3 ~1 ~18 ~3 ~1 ~19 minecraft:mossy_stone_bricks
fill ~3 ~1 ~20 ~3 ~2 ~21 minecraft:stone_bricks
fill ~3 ~1 ~22 ~3 ~2 ~22 minecraft:mossy_stone_bricks
fill ~3 ~1 ~23 ~3 ~1 ~24 minecraft:stone_bricks
setblock ~3 ~1 ~25 minecraft:mossy_stone_bricks
fill ~1 ~2 ~5 ~1 ~2 ~6 minecraft:stone_bricks
setblock ~1 ~2 ~7 minecraft:mossy_stone_bricks
fill ~1 ~2 ~8 ~1 ~3 ~8 minecraft:cracked_stone_bricks
fill ~1 ~2 ~9 ~1 ~2 ~11 minecraft:stone_bricks
setblock ~1 ~2 ~12 minecraft:cracked_stone_bricks
fill ~1 ~2 ~13 ~1 ~2 ~15 minecraft:stone_bricks
setblock ~1 ~2 ~16 minecraft:deepslate_bricks
fill ~1 ~2 ~17 ~1 ~3 ~17 minecraft:mossy_stone_bricks
fill ~1 ~2 ~18 ~2 ~2 ~19 minecraft:stone_bricks
setblock ~1 ~2 ~20 minecraft:stone_bricks
setblock ~1 ~2 ~21 minecraft:cracked_stone_bricks
fill ~1 ~2 ~22 ~1 ~2 ~25 minecraft:stone_bricks
setblock ~2 ~2 ~5 minecraft:mossy_stone_bricks
fill ~2 ~2 ~6 ~2 ~10 ~6 minecraft:stone_bricks
setblock ~2 ~2 ~7 minecraft:cracked_stone_bricks
fill ~2 ~2 ~8 ~2 ~5 ~8 minecraft:stone_bricks
setblock ~2 ~2 ~9 minecraft:mossy_stone_bricks
fill ~2 ~2 ~11 ~2 ~5 ~12 minecraft:stone_bricks
fill ~2 ~2 ~13 ~2 ~3 ~13 minecraft:mossy_stone_bricks
fill ~2 ~2 ~14 ~4 ~2 ~14 minecraft:stone_bricks
fill ~2 ~2 ~15 ~2 ~4 ~16 minecraft:stone_bricks
fill ~2 ~2 ~17 ~2 ~3 ~17 minecraft:cracked_stone_bricks
setblock ~2 ~2 ~20 minecraft:cracked_stone_bricks
fill ~2 ~2 ~21 ~2 ~2 ~23 minecraft:stone_bricks
setblock ~2 ~2 ~25 minecraft:mossy_stone_bricks
fill ~3 ~2 ~5 ~3 ~7 ~5 minecraft:stone_bricks
fill ~3 ~2 ~7 ~3 ~2 ~8 minecraft:stone_bricks
setblock ~3 ~2 ~9 minecraft:cracked_stone_bricks
setblock ~3 ~2 ~10 minecraft:stone_bricks
fill ~3 ~2 ~11 ~3 ~3 ~11 minecraft:mossy_stone_bricks
fill ~3 ~2 ~12 ~3 ~5 ~12 minecraft:stone_bricks
setblock ~3 ~2 ~13 minecraft:deepslate_bricks
setblock ~3 ~2 ~15 minecraft:deepslate_bricks
fill ~3 ~2 ~16 ~3 ~2 ~18 minecraft:stone_bricks
fill ~3 ~2 ~19 ~3 ~3 ~19 minecraft:cracked_stone_bricks
fill ~3 ~2 ~23 ~4 ~2 ~23 minecraft:cracked_stone_bricks
fill ~3 ~2 ~24 ~4 ~2 ~25 minecraft:stone_bricks
setblock ~4 ~2 ~5 minecraft:mossy_stone_bricks
setblock ~4 ~2 ~6 minecraft:stone_bricks
setblock ~4 ~2 ~8 minecraft:cracked_stone_bricks
fill ~4 ~2 ~9 ~4 ~2 ~10 minecraft:mossy_stone_bricks
fill ~4 ~2 ~11 ~4 ~8 ~12 minecraft:stone_bricks
fill ~4 ~2 ~15 ~4 ~2 ~16 minecraft:mossy_stone_bricks
fill ~4 ~2 ~17 ~4 ~8 ~17 minecraft:stone_bricks
setblock ~4 ~2 ~18 minecraft:mossy_stone_bricks
fill ~4 ~2 ~20 ~4 ~2 ~21 minecraft:stone_bricks
setblock ~4 ~2 ~22 minecraft:mossy_stone_bricks
setblock ~1 ~3 ~5 minecraft:deepslate_bricks
fill ~1 ~3 ~6 ~1 ~3 ~7 minecraft:stone_bricks
fill ~1 ~3 ~9 ~2 ~3 ~9 minecraft:stone_bricks
fill ~1 ~3 ~10 ~1 ~6 ~10 minecraft:stone_bricks
setblock ~1 ~3 ~11 minecraft:mossy_stone_bricks
setblock ~1 ~3 ~12 minecraft:stone_bricks
fill ~1 ~3 ~13 ~1 ~3 ~14 minecraft:cracked_stone_bricks
fill ~1 ~3 ~15 ~1 ~4 ~15 minecraft:stone_bricks
setblock ~1 ~3 ~16 minecraft:mossy_stone_bricks
fill ~1 ~3 ~18 ~1 ~3 ~19 minecraft:mossy_stone_bricks
setblock ~1 ~3 ~20 minecraft:cracked_stone_bricks
fill ~1 ~3 ~21 ~1 ~3 ~22 minecraft:stone_bricks
setblock ~1 ~3 ~23 minecraft:deepslate_bricks
setblock ~1 ~3 ~24 minecraft:mossy_stone_bricks
fill ~1 ~3 ~25 ~1 ~8 ~25 minecraft:stone_bricks
fill ~2 ~3 ~5 ~2 ~10 ~5 minecraft:stone_bricks
fill ~2 ~3 ~7 ~2 ~6 ~7 minecraft:stone_bricks
fill ~2 ~3 ~14 ~2 ~4 ~14 minecraft:stone_bricks
fill ~2 ~3 ~18 ~4 ~7 ~18 minecraft:stone_bricks
setblock ~2 ~3 ~19 minecraft:stone_bricks
fill ~2 ~3 ~20 ~3 ~3 ~20 minecraft:mossy_stone_bricks
setblock ~2 ~3 ~21 minecraft:mossy_stone_bricks
fill ~2 ~3 ~22 ~2 ~4 ~25 minecraft:stone_bricks
fill ~3 ~3 ~6 ~3 ~3 ~7 minecraft:stone_bricks
fill ~3 ~3 ~8 ~3 ~3 ~10 minecraft:mossy_stone_bricks
fill ~3 ~3 ~13 ~3 ~3 ~14 minecraft:mossy_stone_bricks
fill ~3 ~3 ~15 ~3 ~3 ~16 minecraft:stone_bricks
setblock ~3 ~3 ~17 minecraft:mossy_stone_bricks
fill ~3 ~3 ~21 ~3 ~6 ~23 minecraft:stone_bricks
setblock ~3 ~3 ~24 minecraft:deepslate_bricks
setblock ~3 ~3 ~25 minecraft:stone_bricks
fill ~4 ~3 ~5 ~4 ~6 ~5 minecraft:stone_bricks
fill ~4 ~3 ~6 ~4 ~4 ~6 minecraft:cracked_stone_bricks
fill ~4 ~3 ~8 ~4 ~3 ~10 minecraft:stone_bricks
fill ~4 ~3 ~14 ~4 ~8 ~14 minecraft:stone_bricks
setblock ~4 ~3 ~15 minecraft:cracked_stone_bricks
setblock ~4 ~3 ~16 minecraft:mossy_stone_bricks
setblock ~4 ~3 ~20 minecraft:stone_bricks
setblock ~4 ~3 ~21 minecraft:cracked_stone_bricks
fill ~4 ~3 ~22 ~4 ~8 ~22 minecraft:stone_bricks
setblock ~4 ~3 ~23 minecraft:mossy_stone_bricks
fill ~4 ~3 ~24 ~4 ~4 ~25 minecraft:stone_bricks
fill ~1 ~4 ~5 ~1 ~6 ~6 minecraft:stone_bricks
fill ~1 ~4 ~8 ~1 ~4 ~9 minecraft:stone_bricks
fill ~1 ~4 ~11 ~1 ~5 ~11 minecraft:stone_bricks
fill ~1 ~4 ~13 ~3 ~5 ~13 minecraft:stone_bricks
setblock ~1 ~4 ~14 minecraft:deepslate_bricks
fill ~1 ~4 ~16 ~1 ~6 ~17 minecraft:stone_bricks
fill ~1 ~4 ~19 ~1 ~6 ~20 minecraft:stone_bricks
setblock ~1 ~4 ~21 minecraft:cracked_stone_bricks
fill ~1 ~4 ~22 ~1 ~6 ~22 minecraft:stone_bricks
fill ~1 ~4 ~24 ~1 ~5 ~24 minecraft:stone_bricks
setblock ~2 ~4 ~9 minecraft:cracked_stone_bricks
fill ~2 ~4 ~10 ~2 ~8 ~10 minecraft:stone_bricks
fill ~2 ~4 ~17 ~2 ~5 ~17 minecraft:stone_bricks
fill ~2 ~4 ~19 ~2 ~4 ~20 minecraft:cracked_stone_bricks
fill ~2 ~4 ~21 ~2 ~10 ~21 minecraft:stone_bricks
fill ~3 ~4 ~6 ~3 ~4 ~7 minecraft:cracked_stone_bricks
fill ~3 ~4 ~8 ~4 ~5 ~8 minecraft:stone_bricks
fill ~3 ~4 ~9 ~3 ~4 ~11 minecraft:stone_bricks
fill ~3 ~4 ~14 ~3 ~4 ~15 minecraft:stone_bricks
fill ~3 ~4 ~16 ~3 ~4 ~17 minecraft:cracked_stone_bricks
fill ~3 ~4 ~19 ~3 ~10 ~20 minecraft:stone_bricks
fill ~3 ~4 ~24 ~3 ~9 ~24 minecraft:stone_bricks
setblock ~3 ~4 ~25 minecraft:cracked_stone_bricks
setblock ~4 ~4 ~9 minecraft:cracked_stone_bricks
fill ~4 ~4 ~10 ~4 ~6 ~10 minecraft:stone_bricks
fill ~4 ~4 ~15 ~4 ~7 ~16 minecraft:stone_bricks
setblock ~4 ~4 ~20 minecraft:cracked_stone_bricks
fill ~4 ~4 ~21 ~4 ~8 ~21 minecraft:stone_bricks
setblock ~4 ~4 ~23 minecraft:stone_bricks
fill ~1 ~5 ~8 ~1 ~6 ~8 minecraft:stone_bricks
setblock ~1 ~5 ~9 minecraft:cracked_stone_bricks
setblock ~1 ~5 ~14 minecraft:stone_bricks
fill ~1 ~5 ~15 ~2 ~5 ~15 minecraft:cracked_stone_bricks
setblock ~1 ~5 ~21 minecraft:stone_bricks
fill ~2 ~5 ~9 ~4 ~5 ~9 minecraft:stone_bricks
fill ~2 ~5 ~14 ~3 ~5 ~14 minecraft:cracked_stone_bricks
fill ~2 ~5 ~16 ~2 ~9 ~16 minecraft:stone_bricks
fill ~2 ~5 ~19 ~2 ~5 ~20 minecraft:stone_bricks
fill ~2 ~5 ~22 ~2 ~7 ~24 minecraft:stone_bricks
setblock ~2 ~5 ~25 minecraft:cracked_stone_bricks
fill ~3 ~5 ~6 ~4 ~8 ~6 minecraft:stone_bricks
setblock ~3 ~5 ~7 minecraft:cracked_stone_bricks
fill ~3 ~5 ~10 ~3 ~6 ~10 minecraft:cracked_stone_bricks
fill ~3 ~5 ~11 ~3 ~10 ~11 minecraft:stone_bricks
fill ~3 ~5 ~15 ~3 ~6 ~15 minecraft:stone_bricks
setblock ~3 ~5 ~16 minecraft:cracked_stone_bricks
setblock ~3 ~5 ~17 minecraft:stone_bricks
fill ~3 ~5 ~25 ~3 ~7 ~25 minecraft:stone_bricks
fill ~4 ~5 ~20 ~4 ~8 ~20 minecraft:stone_bricks
setblock ~4 ~5 ~23 minecraft:deepslate_bricks
fill ~4 ~5 ~24 ~4 ~7 ~24 minecraft:stone_bricks
setblock ~4 ~5 ~25 minecraft:deepslate_bricks
fill ~1 ~6 ~9 ~2 ~7 ~9 minecraft:stone_bricks
fill ~1 ~6 ~11 ~2 ~6 ~11 minecraft:cracked_stone_bricks
fill ~1 ~6 ~13 ~1 ~6 ~14 minecraft:cracked_stone_bricks
fill ~1 ~6 ~15 ~1 ~8 ~15 minecraft:stone_bricks
setblock ~1 ~6 ~21 minecraft:cracked_stone_bricks
setblock ~1 ~6 ~24 minecraft:cracked_stone_bricks
setblock ~2 ~6 ~8 minecraft:deepslate_bricks
setblock ~2 ~6 ~12 minecraft:cracked_stone_bricks
fill ~2 ~6 ~13 ~3 ~6 ~14 minecraft:stone_bricks
setblock ~2 ~6 ~15 minecraft:deepslate_bricks
fill ~2 ~6 ~17 ~3 ~6 ~17 minecraft:cracked_stone_bricks
fill ~2 ~6 ~19 ~2 ~7 ~19 minecraft:cracked_stone_bricks
fill ~2 ~6 ~20 ~2 ~10 ~20 minecraft:stone_bricks
fill ~2 ~6 ~25 ~2 ~9 ~25 minecraft:stone_bricks
fill ~3 ~6 ~7 ~3 ~7 ~7 minecraft:stone_bricks
setblock ~3 ~6 ~8 minecraft:cracked_stone_bricks
setblock ~3 ~6 ~9 minecraft:deepslate_bricks
setblock ~3 ~6 ~12 minecraft:deepslate_bricks
setblock ~3 ~6 ~16 minecraft:stone_bricks
fill ~4 ~6 ~8 ~4 ~6 ~9 minecraft:stone_bricks
setblock ~4 ~6 ~23 minecraft:cracked_stone_bricks
setblock ~4 ~6 ~25 minecraft:stone_bricks
fill ~1 ~7 ~5 ~1 ~8 ~5 minecraft:stone_bricks
fill ~1 ~7 ~6 ~1 ~8 ~6 minecraft:cracked_stone_bricks
setblock ~1 ~7 ~8 minecraft:cracked_stone_bricks
setblock ~1 ~7 ~10 minecraft:cracked_stone_bricks
fill ~1 ~7 ~11 ~2 ~8 ~11 minecraft:stone_bricks
fill ~1 ~7 ~13 ~1 ~8 ~13 minecraft:stone_bricks
fill ~1 ~7 ~14 ~2 ~7 ~14 minecraft:cracked_stone_bricks
setblock ~1 ~7 ~16 minecraft:cracked_stone_bricks
fill ~1 ~7 ~17 ~3 ~8 ~17 minecraft:stone_bricks
setblock ~1 ~7 ~19 minecraft:cracked_stone_bricks
fill ~1 ~7 ~20 ~1 ~8 ~21 minecraft:stone_bricks
setblock ~1 ~7 ~22 minecraft:cracked_stone_bricks
fill ~1 ~7 ~24 ~1 ~8 ~24 minecraft:stone_bricks
setblock ~2 ~7 ~7 minecraft:deepslate_bricks
fill ~2 ~7 ~8 ~3 ~8 ~8 minecraft:stone_bricks
fill ~2 ~7 ~12 ~3 ~10 ~12 minecraft:stone_bricks
fill ~2 ~7 ~13 ~3 ~7 ~13 minecraft:cracked_stone_bricks
fill ~2 ~7 ~15 ~2 ~10 ~15 minecraft:stone_bricks
fill ~3 ~7 ~9 ~4 ~8 ~9 minecraft:stone_bricks
fill ~3 ~7 ~10 ~3 ~9 ~10 minecraft:stone_bricks
fill ~3 ~7 ~14 ~3 ~10 ~14 minecraft:stone_bricks
fill ~3 ~7 ~15 ~3 ~7 ~16 minecraft:cracked_stone_bricks
fill ~3 ~7 ~21 ~3 ~7 ~22 minecraft:stone_bricks
setblock ~3 ~7 ~23 minecraft:cracked_stone_bricks
setblock ~4 ~7 ~5 minecraft:cracked_stone_bricks
setblock ~4 ~7 ~8 minecraft:deepslate_bricks
setblock ~4 ~7 ~10 minecraft:cracked_stone_bricks
setblock ~4 ~7 ~23 minecraft:deepslate_bricks
setblock ~4 ~7 ~25 minecraft:cracked_stone_bricks
fill ~1 ~8 ~7 ~2 ~8 ~7 minecraft:stone_bricks
setblock ~1 ~8 ~8 minecraft:stone_bricks
setblock ~1 ~8 ~9 minecraft:cracked_stone_bricks
setblock ~1 ~8 ~10 minecraft:stone_bricks
setblock ~1 ~8 ~12 minecraft:cracked_stone_bricks
setblock ~1 ~8 ~14 minecraft:stone_bricks
setblock ~1 ~8 ~16 minecraft:stone_bricks
fill ~1 ~8 ~18 ~2 ~8 ~18 minecraft:cracked_stone_bricks
fill ~1 ~8 ~19 ~2 ~8 ~19 minecraft:stone_bricks
fill ~1 ~8 ~22 ~2 ~8 ~23 minecraft:stone_bricks
fill ~2 ~8 ~9 ~2 ~10 ~9 minecraft:stone_bricks
fill ~2 ~8 ~13 ~2 ~9 ~14 minecraft:stone_bricks
setblock ~2 ~8 ~24 minecraft:cracked_stone_bricks
fill ~3 ~8 ~5 ~3 ~9 ~5 minecraft:cracked_stone_bricks
setblock ~3 ~8 ~7 minecraft:cracked_stone_bricks
setblock ~3 ~8 ~13 minecraft:cracked_stone_bricks
fill ~3 ~8 ~15 ~3 ~10 ~16 minecraft:stone_bricks
fill ~3 ~8 ~18 ~4 ~8 ~18 minecraft:stone_bricks
setblock ~3 ~8 ~21 minecraft:stone_bricks
fill ~3 ~8 ~22 ~3 ~9 ~22 minecraft:cracked_stone_bricks
fill ~3 ~8 ~23 ~4 ~8 ~23 minecraft:stone_bricks
setblock ~3 ~8 ~25 minecraft:cracked_stone_bricks
setblock ~4 ~8 ~5 minecraft:stone_bricks
setblock ~4 ~8 ~8 minecraft:cracked_stone_bricks
setblock ~4 ~8 ~10 minecraft:stone_bricks
setblock ~4 ~8 ~15 minecraft:cracked_stone_bricks
setblock ~4 ~8 ~16 minecraft:stone_bricks
setblock ~4 ~8 ~24 minecraft:cracked_stone_bricks
setblock ~4 ~8 ~25 minecraft:deepslate_bricks
fill ~2 ~9 ~7 ~2 ~10 ~8 minecraft:stone_bricks
setblock ~2 ~9 ~10 minecraft:cracked_stone_bricks
fill ~2 ~9 ~11 ~2 ~10 ~11 minecraft:stone_bricks
setblock ~2 ~9 ~17 minecraft:cracked_stone_bricks
fill ~2 ~9 ~18 ~2 ~10 ~19 minecraft:stone_bricks
fill ~2 ~9 ~22 ~2 ~9 ~24 minecraft:stone_bricks
fill ~3 ~9 ~6 ~3 ~9 ~7 minecraft:stone_bricks
setblock ~3 ~9 ~8 minecraft:cracked_stone_bricks
setblock ~3 ~9 ~9 minecraft:stone_bricks
fill ~3 ~9 ~13 ~3 ~10 ~13 minecraft:stone_bricks
fill ~3 ~9 ~17 ~3 ~10 ~17 minecraft:stone_bricks
setblock ~3 ~9 ~18 minecraft:cracked_stone_bricks
setblock ~3 ~9 ~21 minecraft:cracked_stone_bricks
fill ~3 ~9 ~23 ~3 ~10 ~23 minecraft:stone_bricks
fill ~3 ~9 ~25 ~3 ~10 ~25 minecraft:stone_bricks
fill ~1 ~10 ~5 ~1 ~10 ~6 minecraft:stone_bricks
setblock ~1 ~10 ~7 minecraft:cracked_stone_bricks
fill ~1 ~10 ~8 ~1 ~10 ~11 minecraft:stone_bricks
setblock ~1 ~10 ~12 minecraft:cracked_stone_bricks
fill ~1 ~10 ~13 ~1 ~10 ~25 minecraft:stone_bricks
setblock ~2 ~10 ~10 minecraft:stone_bricks
setblock ~2 ~10 ~13 minecraft:cracked_stone_bricks
setblock ~2 ~10 ~14 minecraft:stone_bricks
setblock ~2 ~10 ~16 minecraft:cracked_stone_bricks
setblock ~2 ~10 ~17 minecraft:stone_bricks
fill ~2 ~10 ~22 ~3 ~10 ~22 minecraft:stone_bricks
setblock ~2 ~10 ~23 minecraft:cracked_stone_bricks
setblock ~2 ~10 ~24 minecraft:stone_bricks
setblock ~2 ~10 ~25 minecraft:cracked_stone_bricks
fill ~3 ~10 ~5 ~4 ~10 ~5 minecraft:stone_bricks
setblock ~3 ~10 ~6 minecraft:deepslate_bricks
fill ~3 ~10 ~7 ~3 ~10 ~8 minecraft:stone_bricks
fill ~3 ~10 ~9 ~3 ~10 ~10 minecraft:cracked_stone_bricks
setblock ~3 ~10 ~18 minecraft:stone_bricks
fill ~3 ~10 ~21 ~4 ~10 ~21 minecraft:stone_bricks
setblock ~3 ~10 ~24 minecraft:cracked_stone_bricks
setblock ~4 ~10 ~6 minecraft:stone_bricks
fill ~4 ~10 ~8 ~4 ~10 ~9 minecraft:stone_bricks
fill ~4 ~10 ~11 ~4 ~10 ~12 minecraft:stone_bricks
fill ~4 ~10 ~14 ~4 ~10 ~15 minecraft:stone_bricks
fill ~4 ~10 ~17 ~4 ~10 ~18 minecraft:stone_bricks
setblock ~4 ~10 ~20 minecraft:deepslate_bricks
fill ~4 ~10 ~23 ~4 ~10 ~25 minecraft:stone_bricks

# the courses (fix round 1): a plinth course of deepslate bricks at y 1 and a band of deepslate bricks at y 9 under the corbel table on the outer face (x 1), and on every slit a deepslate sill at y 4 and lintel at y 7
setblock ~1 ~1 ~5 minecraft:polished_blackstone_bricks
setblock ~1 ~1 ~6 minecraft:cracked_deepslate_bricks
setblock ~1 ~1 ~7 minecraft:deepslate_bricks
setblock ~1 ~1 ~8 minecraft:polished_blackstone_bricks
setblock ~1 ~1 ~9 minecraft:deepslate_bricks
setblock ~1 ~1 ~10 minecraft:cracked_deepslate_bricks
setblock ~1 ~1 ~11 minecraft:deepslate_bricks
setblock ~1 ~1 ~12 minecraft:polished_blackstone_bricks
setblock ~1 ~1 ~13 minecraft:deepslate_bricks
setblock ~1 ~1 ~14 minecraft:polished_blackstone_bricks
setblock ~1 ~1 ~15 minecraft:deepslate_bricks
setblock ~1 ~1 ~16 minecraft:cracked_deepslate_bricks
fill ~1 ~1 ~17 ~1 ~1 ~19 minecraft:deepslate_bricks
setblock ~1 ~1 ~20 minecraft:stone_bricks
fill ~1 ~1 ~21 ~1 ~1 ~23 minecraft:deepslate_bricks
fill ~1 ~1 ~24 ~1 ~1 ~25 minecraft:cracked_deepslate_bricks
setblock ~1 ~9 ~5 minecraft:deepslate_bricks
setblock ~1 ~9 ~6 minecraft:stone_bricks
setblock ~1 ~9 ~7 minecraft:polished_blackstone_bricks
setblock ~1 ~9 ~8 minecraft:deepslate_bricks
fill ~1 ~9 ~9 ~1 ~9 ~10 minecraft:polished_blackstone_bricks
setblock ~1 ~9 ~11 minecraft:cracked_deepslate_bricks
setblock ~1 ~9 ~12 minecraft:deepslate_bricks
setblock ~1 ~9 ~13 minecraft:cracked_deepslate_bricks
fill ~1 ~9 ~14 ~1 ~9 ~16 minecraft:deepslate_bricks
setblock ~1 ~9 ~17 minecraft:stone_bricks
setblock ~1 ~9 ~18 minecraft:cracked_deepslate_bricks
fill ~1 ~9 ~19 ~1 ~9 ~20 minecraft:deepslate_bricks
setblock ~1 ~9 ~21 minecraft:polished_blackstone_bricks
fill ~1 ~9 ~22 ~1 ~9 ~23 minecraft:deepslate_bricks
setblock ~1 ~9 ~24 minecraft:cracked_deepslate_bricks
setblock ~1 ~9 ~25 minecraft:polished_blackstone_bricks

# slits: recessed one block, two high, between the pilasters; the plinth lip of cobblestone stairs at y 1 along the outer face between the pilasters
fill ~0 ~1 ~5 ~0 ~1 ~8 minecraft:cobblestone_stairs[facing=east,half=bottom]
fill ~0 ~1 ~10 ~0 ~1 ~14 minecraft:cobblestone_stairs[facing=east,half=bottom]
fill ~0 ~1 ~16 ~0 ~1 ~20 minecraft:cobblestone_stairs[facing=east,half=bottom]
fill ~0 ~1 ~22 ~0 ~1 ~25 minecraft:cobblestone_stairs[facing=east,half=bottom]
setblock ~1 ~4 ~7 minecraft:deepslate_bricks
setblock ~1 ~4 ~12 minecraft:deepslate_bricks
setblock ~1 ~4 ~18 minecraft:deepslate_bricks
setblock ~1 ~4 ~23 minecraft:deepslate_bricks
fill ~1 ~5 ~7 ~1 ~6 ~7 minecraft:air
fill ~1 ~5 ~12 ~1 ~6 ~12 minecraft:air
fill ~1 ~5 ~18 ~1 ~6 ~18 minecraft:air
fill ~1 ~5 ~23 ~1 ~6 ~23 minecraft:air
setblock ~1 ~7 ~7 minecraft:cracked_deepslate_bricks
setblock ~1 ~7 ~12 minecraft:cracked_deepslate_bricks
setblock ~1 ~7 ~18 minecraft:polished_blackstone_bricks
setblock ~1 ~7 ~23 minecraft:deepslate_bricks

# the pilasters at x 0: a rib of stone bricks every sixth block, stepped back at the top with a stair, under the corbel table
setblock ~0 ~1 ~9 minecraft:cracked_deepslate_bricks
setblock ~0 ~1 ~15 minecraft:cracked_deepslate_bricks
setblock ~0 ~1 ~21 minecraft:cracked_deepslate_bricks
setblock ~0 ~2 ~9 minecraft:deepslate_bricks
fill ~0 ~2 ~15 ~0 ~4 ~15 minecraft:deepslate_bricks
setblock ~0 ~2 ~21 minecraft:stone_bricks
setblock ~0 ~3 ~9 minecraft:polished_blackstone_bricks
setblock ~0 ~3 ~21 minecraft:deepslate_bricks
setblock ~0 ~4 ~9 minecraft:cracked_deepslate_bricks
setblock ~0 ~4 ~21 minecraft:stone_bricks
fill ~0 ~5 ~9 ~0 ~9 ~9 minecraft:deepslate_bricks
setblock ~0 ~5 ~15 minecraft:cracked_deepslate_bricks
setblock ~0 ~5 ~21 minecraft:cracked_deepslate_bricks
fill ~0 ~6 ~15 ~0 ~9 ~15 minecraft:deepslate_bricks
fill ~0 ~6 ~21 ~0 ~9 ~21 minecraft:deepslate_bricks

# the inner face (fix round 1): a plinth course and a band under the corbels in deepslate bricks at x 4 (y 1 and y 9) and three ribs (z 7, 13 and 19, y 2..8) under every second corbel, so the face the courtyard sees is not a flat plane
setblock ~4 ~1 ~5 minecraft:polished_blackstone_bricks
setblock ~4 ~1 ~6 minecraft:deepslate_bricks
fill ~4 ~1 ~7 ~4 ~1 ~8 minecraft:cracked_deepslate_bricks
fill ~4 ~1 ~9 ~4 ~1 ~10 minecraft:polished_blackstone_bricks
setblock ~4 ~1 ~11 minecraft:deepslate_bricks
setblock ~4 ~1 ~12 minecraft:stone_bricks
fill ~4 ~1 ~13 ~4 ~1 ~18 minecraft:deepslate_bricks
setblock ~4 ~1 ~19 minecraft:cracked_deepslate_bricks
setblock ~4 ~1 ~20 minecraft:polished_blackstone_bricks
setblock ~4 ~1 ~21 minecraft:deepslate_bricks
setblock ~4 ~1 ~22 minecraft:cracked_deepslate_bricks
fill ~4 ~1 ~23 ~4 ~1 ~24 minecraft:deepslate_bricks
setblock ~4 ~1 ~25 minecraft:polished_blackstone_bricks
setblock ~4 ~2 ~7 minecraft:deepslate_bricks
fill ~4 ~2 ~13 ~4 ~4 ~13 minecraft:deepslate_bricks
fill ~4 ~2 ~19 ~4 ~3 ~19 minecraft:deepslate_bricks
setblock ~4 ~3 ~7 minecraft:stone_bricks
setblock ~4 ~4 ~7 minecraft:deepslate_bricks
setblock ~4 ~4 ~19 minecraft:cracked_deepslate_bricks
setblock ~4 ~5 ~7 minecraft:cracked_deepslate_bricks
setblock ~4 ~5 ~13 minecraft:polished_blackstone_bricks
fill ~4 ~5 ~19 ~4 ~8 ~19 minecraft:deepslate_bricks
setblock ~4 ~6 ~7 minecraft:deepslate_bricks
setblock ~4 ~6 ~13 minecraft:deepslate_bricks
setblock ~4 ~7 ~7 minecraft:cracked_deepslate_bricks
setblock ~4 ~7 ~13 minecraft:cracked_deepslate_bricks
fill ~4 ~8 ~7 ~4 ~9 ~7 minecraft:deepslate_bricks
setblock ~4 ~8 ~13 minecraft:deepslate_bricks
setblock ~4 ~9 ~5 minecraft:stone_bricks
setblock ~4 ~9 ~6 minecraft:deepslate_bricks
setblock ~4 ~9 ~8 minecraft:cracked_deepslate_bricks
setblock ~4 ~9 ~9 minecraft:stone_bricks
setblock ~4 ~9 ~10 minecraft:cracked_deepslate_bricks
setblock ~4 ~9 ~11 minecraft:deepslate_bricks
setblock ~4 ~9 ~12 minecraft:polished_blackstone_bricks
setblock ~4 ~9 ~13 minecraft:cracked_deepslate_bricks
setblock ~4 ~9 ~14 minecraft:polished_blackstone_bricks
fill ~4 ~9 ~15 ~4 ~9 ~18 minecraft:deepslate_bricks
fill ~4 ~9 ~19 ~4 ~9 ~20 minecraft:cracked_deepslate_bricks
setblock ~4 ~9 ~21 minecraft:deepslate_bricks
setblock ~4 ~9 ~22 minecraft:polished_blackstone_bricks
fill ~4 ~9 ~23 ~4 ~9 ~25 minecraft:deepslate_bricks

# the corbel table at y 10: upside-down stone brick stairs under the parapet, the whole length of the wall
fill ~0 ~10 ~5 ~0 ~10 ~6 minecraft:stone_brick_stairs[facing=east,half=top]
fill ~0 ~10 ~8 ~0 ~10 ~11 minecraft:stone_brick_stairs[facing=east,half=top]
fill ~0 ~10 ~13 ~0 ~10 ~17 minecraft:stone_brick_stairs[facing=east,half=top]
fill ~0 ~10 ~19 ~0 ~10 ~22 minecraft:stone_brick_stairs[facing=east,half=top]
fill ~0 ~10 ~24 ~0 ~10 ~25 minecraft:stone_brick_stairs[facing=east,half=top]

# lantern brackets (the detail beat of the face): over every slit a block of deepslate bricks in the corbel table and a lantern hung from it at y 9, a row of lights along the outer face
setblock ~0 ~9 ~7 minecraft:lantern[hanging=true]
setblock ~0 ~9 ~12 minecraft:lantern[hanging=true]
setblock ~0 ~9 ~18 minecraft:lantern[hanging=true]
setblock ~0 ~9 ~23 minecraft:lantern[hanging=true]
setblock ~0 ~10 ~7 minecraft:deepslate_bricks
setblock ~0 ~10 ~12 minecraft:deepslate_bricks
setblock ~0 ~10 ~18 minecraft:deepslate_bricks
setblock ~0 ~10 ~23 minecraft:deepslate_bricks

# the parapet base and the deck at y 11: flagstones on the walk (x 1..2), stone on the outer rim and the inner shoulder
setblock ~0 ~11 ~5 minecraft:stone_bricks
setblock ~0 ~11 ~6 minecraft:deepslate_bricks
setblock ~0 ~11 ~7 minecraft:cracked_stone_bricks
fill ~0 ~11 ~8 ~0 ~11 ~14 minecraft:stone_bricks
setblock ~0 ~11 ~15 minecraft:mossy_stone_bricks
fill ~0 ~11 ~16 ~1 ~11 ~17 minecraft:stone_bricks
setblock ~0 ~11 ~18 minecraft:cracked_stone_bricks
fill ~0 ~11 ~19 ~0 ~11 ~20 minecraft:stone_bricks
setblock ~0 ~11 ~21 minecraft:cracked_stone_bricks
setblock ~0 ~11 ~22 minecraft:mossy_stone_bricks
fill ~0 ~11 ~23 ~2 ~11 ~23 minecraft:stone_bricks
fill ~0 ~11 ~24 ~1 ~11 ~24 minecraft:stone_bricks
setblock ~0 ~11 ~25 minecraft:cracked_stone_bricks
setblock ~1 ~11 ~5 minecraft:cracked_stone_bricks
setblock ~1 ~11 ~6 minecraft:mossy_stone_bricks
setblock ~1 ~11 ~7 minecraft:stone_bricks
setblock ~1 ~11 ~8 minecraft:cracked_stone_bricks
fill ~1 ~11 ~9 ~1 ~11 ~10 minecraft:stone_bricks
setblock ~1 ~11 ~11 minecraft:mossy_stone_bricks
fill ~1 ~11 ~12 ~1 ~11 ~14 minecraft:stone_bricks
setblock ~1 ~11 ~15 minecraft:cracked_stone_bricks
fill ~1 ~11 ~18 ~1 ~11 ~19 minecraft:stone_bricks
fill ~1 ~11 ~20 ~1 ~11 ~21 minecraft:cracked_stone_bricks
setblock ~1 ~11 ~22 minecraft:cobblestone
fill ~1 ~11 ~25 ~2 ~11 ~25 minecraft:mossy_stone_bricks
fill ~2 ~11 ~5 ~2 ~11 ~12 minecraft:stone_bricks
setblock ~2 ~11 ~13 minecraft:cobblestone
fill ~2 ~11 ~14 ~2 ~11 ~21 minecraft:stone_bricks
setblock ~2 ~11 ~22 minecraft:mossy_stone_bricks
setblock ~2 ~11 ~24 minecraft:cracked_stone_bricks
setblock ~3 ~11 ~5 minecraft:cracked_stone_bricks
fill ~3 ~11 ~6 ~3 ~11 ~9 minecraft:stone_bricks
setblock ~3 ~11 ~10 minecraft:deepslate_bricks
fill ~3 ~11 ~11 ~4 ~11 ~12 minecraft:stone_bricks
fill ~3 ~11 ~13 ~4 ~11 ~15 minecraft:cracked_stone_bricks
setblock ~3 ~11 ~16 minecraft:cracked_stone_bricks
fill ~3 ~11 ~17 ~3 ~11 ~18 minecraft:stone_bricks
fill ~3 ~11 ~19 ~4 ~11 ~19 minecraft:cracked_stone_bricks
setblock ~3 ~11 ~20 minecraft:stone_bricks
setblock ~3 ~11 ~21 minecraft:cracked_stone_bricks
setblock ~3 ~11 ~22 minecraft:stone_bricks
fill ~3 ~11 ~23 ~3 ~11 ~24 minecraft:deepslate_bricks
setblock ~3 ~11 ~25 minecraft:cracked_stone_bricks
fill ~4 ~11 ~5 ~4 ~11 ~8 minecraft:stone_bricks
setblock ~4 ~11 ~9 minecraft:deepslate_bricks
setblock ~4 ~11 ~10 minecraft:mossy_stone_bricks
fill ~4 ~11 ~16 ~4 ~11 ~18 minecraft:stone_bricks
setblock ~4 ~11 ~20 minecraft:deepslate_bricks
setblock ~4 ~11 ~21 minecraft:stone_bricks
fill ~4 ~11 ~22 ~4 ~11 ~23 minecraft:cracked_stone_bricks
fill ~4 ~11 ~24 ~4 ~11 ~25 minecraft:stone_bricks

# the inner shoulder (x 3..4, y 12): a waist-high step of stone behind the walk
fill ~3 ~12 ~5 ~4 ~12 ~5 minecraft:stone_bricks
setblock ~3 ~12 ~6 minecraft:cracked_deepslate_bricks
fill ~3 ~12 ~7 ~4 ~12 ~8 minecraft:stone_bricks
fill ~3 ~12 ~9 ~3 ~12 ~12 minecraft:stone_bricks
setblock ~3 ~12 ~13 minecraft:cracked_stone_bricks
setblock ~3 ~12 ~14 minecraft:stone_bricks
setblock ~3 ~12 ~15 minecraft:cracked_stone_bricks
setblock ~3 ~12 ~16 minecraft:deepslate_bricks
setblock ~3 ~12 ~17 minecraft:cracked_stone_bricks
fill ~3 ~12 ~18 ~3 ~12 ~20 minecraft:stone_bricks
setblock ~3 ~12 ~21 minecraft:cracked_deepslate_bricks
setblock ~3 ~12 ~22 minecraft:deepslate_bricks
setblock ~3 ~12 ~23 minecraft:cracked_stone_bricks
setblock ~3 ~12 ~24 minecraft:stone_bricks
setblock ~3 ~12 ~25 minecraft:mossy_stone_bricks
setblock ~4 ~12 ~6 minecraft:cracked_stone_bricks
setblock ~4 ~12 ~9 minecraft:cracked_stone_bricks
setblock ~4 ~12 ~10 minecraft:deepslate_bricks
setblock ~4 ~12 ~11 minecraft:stone_bricks
setblock ~4 ~12 ~12 minecraft:cracked_stone_bricks
setblock ~4 ~12 ~13 minecraft:mossy_stone_bricks
fill ~4 ~12 ~14 ~4 ~12 ~16 minecraft:cracked_stone_bricks
fill ~4 ~12 ~17 ~4 ~12 ~20 minecraft:stone_bricks
setblock ~4 ~12 ~21 minecraft:cracked_stone_bricks
fill ~4 ~12 ~22 ~4 ~12 ~25 minecraft:stone_bricks

# the merlons on the outer face at odd z: two high, a cap of a stone brick slab on top (y 14) so the teeth read as a row of blocks
setblock ~0 ~12 ~5 minecraft:cracked_stone_bricks
setblock ~0 ~12 ~7 minecraft:stone_bricks
setblock ~0 ~12 ~9 minecraft:stone_bricks
setblock ~0 ~12 ~11 minecraft:mossy_stone_bricks
setblock ~0 ~12 ~13 minecraft:deepslate_bricks
setblock ~0 ~12 ~15 minecraft:cracked_stone_bricks
setblock ~0 ~12 ~17 minecraft:cracked_stone_bricks
setblock ~0 ~12 ~19 minecraft:cracked_stone_bricks
setblock ~0 ~12 ~21 minecraft:mossy_stone_bricks
fill ~0 ~12 ~23 ~0 ~13 ~23 minecraft:stone_bricks
fill ~0 ~12 ~25 ~0 ~13 ~25 minecraft:stone_bricks
setblock ~0 ~13 ~5 minecraft:stone_bricks
setblock ~0 ~13 ~7 minecraft:cracked_deepslate_bricks
setblock ~0 ~13 ~9 minecraft:deepslate_bricks
setblock ~0 ~13 ~11 minecraft:stone_bricks
setblock ~0 ~13 ~13 minecraft:stone_bricks
setblock ~0 ~13 ~15 minecraft:stone_bricks
setblock ~0 ~13 ~17 minecraft:stone_bricks
setblock ~0 ~13 ~19 minecraft:deepslate_bricks
setblock ~0 ~13 ~21 minecraft:stone_bricks
setblock ~0 ~14 ~5 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~14 ~7 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~14 ~9 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~14 ~11 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~14 ~13 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~14 ~15 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~14 ~17 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~14 ~19 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~14 ~21 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~14 ~23 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~14 ~25 minecraft:stone_brick_slab[type=bottom]

# the inner face: corbels (upside-down stairs) under the shoulder every third block, a lantern on the shoulder at z 9 and 21
setblock ~4 ~10 ~7 minecraft:stone_brick_stairs[facing=west,half=top]
setblock ~4 ~10 ~10 minecraft:stone_brick_stairs[facing=west,half=top]
setblock ~4 ~10 ~13 minecraft:stone_brick_stairs[facing=west,half=top]
setblock ~4 ~10 ~16 minecraft:stone_brick_stairs[facing=west,half=top]
setblock ~4 ~10 ~19 minecraft:stone_brick_stairs[facing=west,half=top]
setblock ~4 ~10 ~22 minecraft:stone_brick_stairs[facing=west,half=top]
setblock ~4 ~13 ~9 minecraft:lantern[hanging=false]
setblock ~4 ~13 ~21 minecraft:lantern[hanging=false]

# tower z 0..4: walls y 1..10 in deepslate-heavy stone, quoins of dark deepslate at the corners
fill ~0 ~1 ~0 ~4 ~1 ~0 minecraft:deepslate_bricks
fill ~0 ~1 ~4 ~0 ~3 ~4 minecraft:deepslate_bricks
fill ~1 ~1 ~4 ~2 ~1 ~4 minecraft:polished_blackstone_bricks
fill ~3 ~1 ~4 ~4 ~1 ~4 minecraft:deepslate_bricks
setblock ~4 ~1 ~1 minecraft:deepslate_bricks
setblock ~4 ~1 ~2 minecraft:stone_bricks
fill ~0 ~2 ~0 ~3 ~2 ~0 minecraft:stone_bricks
fill ~0 ~2 ~1 ~0 ~8 ~1 minecraft:stone_bricks
fill ~0 ~2 ~2 ~0 ~3 ~2 minecraft:mossy_stone_bricks
fill ~0 ~2 ~3 ~0 ~8 ~3 minecraft:stone_bricks
fill ~1 ~2 ~4 ~3 ~5 ~4 minecraft:stone_bricks
setblock ~4 ~2 ~0 minecraft:deepslate_bricks
setblock ~4 ~2 ~1 minecraft:stone_bricks
setblock ~4 ~2 ~2 minecraft:mossy_stone_bricks
setblock ~4 ~2 ~4 minecraft:cracked_deepslate_bricks
setblock ~0 ~3 ~0 minecraft:deepslate_bricks
fill ~1 ~3 ~0 ~2 ~3 ~0 minecraft:stone_bricks
setblock ~3 ~3 ~0 minecraft:mossy_stone_bricks
setblock ~4 ~3 ~0 minecraft:polished_blackstone_bricks
setblock ~4 ~3 ~1 minecraft:mossy_stone_bricks
fill ~4 ~3 ~2 ~4 ~5 ~2 minecraft:stone_bricks
fill ~4 ~3 ~4 ~4 ~6 ~4 minecraft:deepslate_bricks
setblock ~0 ~4 ~0 minecraft:cracked_deepslate_bricks
setblock ~0 ~4 ~2 minecraft:stone_bricks
setblock ~0 ~4 ~4 minecraft:cracked_deepslate_bricks
fill ~1 ~4 ~0 ~1 ~10 ~0 minecraft:stone_bricks
setblock ~2 ~4 ~0 minecraft:deepslate_bricks
fill ~3 ~4 ~0 ~3 ~7 ~0 minecraft:stone_bricks
setblock ~4 ~4 ~0 minecraft:deepslate_bricks
setblock ~4 ~4 ~1 minecraft:cracked_stone_bricks
fill ~4 ~4 ~3 ~4 ~8 ~3 minecraft:stone_bricks
fill ~0 ~5 ~0 ~0 ~7 ~0 minecraft:deepslate_bricks
fill ~0 ~5 ~4 ~0 ~7 ~4 minecraft:deepslate_bricks
setblock ~4 ~5 ~0 minecraft:cracked_deepslate_bricks
fill ~4 ~5 ~1 ~4 ~8 ~1 minecraft:stone_bricks
fill ~1 ~6 ~4 ~2 ~7 ~4 minecraft:stone_bricks
setblock ~3 ~6 ~4 minecraft:cracked_stone_bricks
fill ~4 ~6 ~0 ~4 ~8 ~0 minecraft:deepslate_bricks
fill ~4 ~6 ~2 ~4 ~7 ~2 minecraft:cracked_stone_bricks
fill ~0 ~7 ~2 ~0 ~8 ~2 minecraft:stone_bricks
setblock ~2 ~7 ~0 minecraft:stone_bricks
setblock ~3 ~7 ~4 minecraft:stone_bricks
setblock ~4 ~7 ~4 minecraft:cracked_deepslate_bricks
fill ~0 ~8 ~0 ~0 ~10 ~0 minecraft:cracked_deepslate_bricks
setblock ~0 ~8 ~4 minecraft:stone_bricks
fill ~1 ~8 ~4 ~1 ~9 ~4 minecraft:deepslate_bricks
fill ~2 ~8 ~0 ~3 ~8 ~0 minecraft:cracked_stone_bricks
setblock ~2 ~8 ~4 minecraft:stone_bricks
setblock ~3 ~8 ~4 minecraft:cracked_stone_bricks
setblock ~4 ~8 ~2 minecraft:stone_bricks
fill ~4 ~8 ~4 ~4 ~9 ~4 minecraft:deepslate_bricks
setblock ~0 ~9 ~1 minecraft:polished_blackstone_bricks
fill ~0 ~9 ~2 ~0 ~9 ~4 minecraft:deepslate_bricks
setblock ~2 ~9 ~0 minecraft:deepslate_bricks
fill ~2 ~9 ~4 ~3 ~9 ~4 minecraft:deepslate_bricks
setblock ~3 ~9 ~0 minecraft:cracked_deepslate_bricks
setblock ~4 ~9 ~0 minecraft:stone_bricks
fill ~4 ~9 ~1 ~4 ~9 ~2 minecraft:deepslate_bricks
setblock ~4 ~9 ~3 minecraft:cracked_deepslate_bricks
fill ~0 ~10 ~1 ~0 ~10 ~2 minecraft:stone_bricks
setblock ~0 ~10 ~3 minecraft:cracked_stone_bricks
setblock ~0 ~10 ~4 minecraft:deepslate_bricks
fill ~1 ~10 ~4 ~3 ~10 ~4 minecraft:stone_bricks
setblock ~2 ~10 ~0 minecraft:cracked_stone_bricks
setblock ~3 ~10 ~0 minecraft:stone_bricks
setblock ~4 ~10 ~0 minecraft:deepslate_bricks
setblock ~4 ~10 ~1 minecraft:cracked_stone_bricks
setblock ~4 ~10 ~2 minecraft:stone_bricks
setblock ~4 ~10 ~3 minecraft:deepslate_bricks
setblock ~4 ~10 ~4 minecraft:cracked_deepslate_bricks

# the plinth of the tower: a lip of stone brick stairs at y 1 along the west face
fill ~0 ~1 ~1 ~0 ~1 ~3 minecraft:cobblestone_stairs[facing=east,half=bottom]

# slits at y 5..6 on the west and the end faces, and the door on the inner side
fill ~0 ~5 ~2 ~0 ~6 ~2 minecraft:air
fill ~2 ~5 ~0 ~2 ~6 ~0 minecraft:air

# the floor at y 11: planks, the ladder hole at x 3, z 2
fill ~1 ~11 ~1 ~3 ~11 ~1 minecraft:spruce_planks
fill ~1 ~11 ~2 ~2 ~11 ~3 minecraft:spruce_planks
setblock ~3 ~11 ~3 minecraft:spruce_planks

# the ladder up the inner wall: (3, 1..12, 2) on the wall x 4 (the door is at z 3)
fill ~3 ~1 ~2 ~3 ~12 ~2 minecraft:ladder[facing=west]

# the door on the inner side: a doorway two high at x 4 (the courtyard side) and a lantern hung inside
fill ~4 ~1 ~3 ~4 ~2 ~3 minecraft:air
setblock ~2 ~3 ~3 minecraft:lantern[hanging=true]
setblock ~4 ~3 ~3 minecraft:chiseled_stone_bricks
setblock ~2 ~4 ~3 minecraft:chain[axis=y]

# the string course at y 11: chiseled stone bricks on the faces, the corbels of the guard room above
setblock ~0 ~11 ~0 minecraft:chiseled_stone_bricks
setblock ~0 ~11 ~1 minecraft:stone_bricks
setblock ~0 ~11 ~2 minecraft:chiseled_stone_bricks
setblock ~0 ~11 ~3 minecraft:stone_bricks
setblock ~0 ~11 ~4 minecraft:chiseled_stone_bricks
setblock ~1 ~11 ~0 minecraft:stone_bricks
setblock ~1 ~11 ~4 minecraft:mossy_stone_bricks
setblock ~2 ~11 ~0 minecraft:chiseled_stone_bricks
setblock ~2 ~11 ~4 minecraft:chiseled_stone_bricks
setblock ~3 ~11 ~0 minecraft:stone_bricks
setblock ~3 ~11 ~4 minecraft:deepslate_bricks
setblock ~4 ~11 ~0 minecraft:chiseled_stone_bricks
setblock ~4 ~11 ~1 minecraft:stone_bricks
setblock ~4 ~11 ~2 minecraft:chiseled_stone_bricks
setblock ~4 ~11 ~3 minecraft:cracked_stone_bricks
setblock ~4 ~11 ~4 minecraft:chiseled_stone_bricks

# the guard room y 12..13: walls with slits on three faces, the way out to the wall-walk in the south face, a cornice of upside-down stairs
fill ~0 ~12 ~0 ~0 ~13 ~0 minecraft:cracked_deepslate_bricks
setblock ~0 ~12 ~1 minecraft:stone_bricks
fill ~0 ~12 ~2 ~0 ~13 ~2 minecraft:air
setblock ~0 ~12 ~3 minecraft:stone_bricks
setblock ~0 ~12 ~4 minecraft:deepslate_bricks
setblock ~1 ~12 ~0 minecraft:stone_bricks
setblock ~1 ~12 ~4 minecraft:cracked_stone_bricks
fill ~2 ~12 ~0 ~2 ~13 ~0 minecraft:air
fill ~2 ~12 ~4 ~2 ~13 ~4 minecraft:air
setblock ~3 ~12 ~0 minecraft:cracked_stone_bricks
setblock ~3 ~12 ~4 minecraft:stone_bricks
setblock ~4 ~12 ~0 minecraft:deepslate_bricks
setblock ~4 ~12 ~1 minecraft:cracked_stone_bricks
setblock ~4 ~12 ~2 minecraft:cracked_deepslate_bricks
setblock ~4 ~12 ~3 minecraft:mossy_stone_bricks
fill ~4 ~12 ~4 ~4 ~13 ~4 minecraft:deepslate_bricks
setblock ~0 ~13 ~1 minecraft:deepslate_brick_stairs[facing=east,half=top]
setblock ~0 ~13 ~3 minecraft:deepslate_brick_stairs[facing=east,half=top]
setblock ~0 ~13 ~4 minecraft:cracked_deepslate_bricks
setblock ~1 ~13 ~0 minecraft:deepslate_brick_stairs[facing=south,half=top]
setblock ~1 ~13 ~4 minecraft:deepslate_brick_stairs[facing=north,half=top]
setblock ~3 ~13 ~0 minecraft:deepslate_brick_stairs[facing=south,half=top]
setblock ~3 ~13 ~4 minecraft:deepslate_brick_stairs[facing=north,half=top]
setblock ~4 ~13 ~0 minecraft:stone_bricks
fill ~4 ~13 ~1 ~4 ~13 ~3 minecraft:deepslate_brick_stairs[facing=west,half=top]

# the pyramid roof: deepslate tile stairs, y 14 ring, y 15 ring, the cap at y 16
fill ~0 ~14 ~0 ~0 ~14 ~4 minecraft:deepslate_tile_stairs[facing=east,half=bottom]
fill ~1 ~14 ~0 ~3 ~14 ~0 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~1 ~14 ~1 ~3 ~14 ~3 minecraft:deepslate_tiles
fill ~1 ~14 ~4 ~3 ~14 ~4 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~4 ~14 ~0 ~4 ~14 ~4 minecraft:deepslate_tile_stairs[facing=west,half=bottom]
fill ~1 ~15 ~1 ~1 ~15 ~3 minecraft:deepslate_tile_stairs[facing=east,half=bottom]
setblock ~2 ~15 ~1 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~2 ~15 ~2 ~2 ~16 ~2 minecraft:deepslate_tiles
setblock ~2 ~15 ~3 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~3 ~15 ~1 ~3 ~15 ~3 minecraft:deepslate_tile_stairs[facing=west,half=bottom]

# the spire: iron bars on the cap, a pennant pole
fill ~2 ~17 ~2 ~2 ~18 ~2 minecraft:iron_bars[north=false,east=false,south=false,west=false]

# tower z 26..30: walls y 1..10 in deepslate-heavy stone, quoins of dark deepslate at the corners
setblock ~0 ~1 ~26 minecraft:cracked_deepslate_bricks
fill ~0 ~1 ~30 ~0 ~4 ~30 minecraft:cracked_deepslate_bricks
fill ~1 ~1 ~26 ~4 ~1 ~26 minecraft:deepslate_bricks
fill ~1 ~1 ~30 ~1 ~2 ~30 minecraft:stone_bricks
setblock ~2 ~1 ~30 minecraft:polished_blackstone_bricks
setblock ~3 ~1 ~30 minecraft:deepslate_bricks
setblock ~4 ~1 ~28 minecraft:cracked_deepslate_bricks
fill ~4 ~1 ~29 ~4 ~1 ~30 minecraft:deepslate_bricks
setblock ~0 ~2 ~26 minecraft:deepslate_bricks
fill ~0 ~2 ~27 ~0 ~2 ~29 minecraft:stone_bricks
fill ~1 ~2 ~26 ~3 ~2 ~26 minecraft:mossy_stone_bricks
setblock ~2 ~2 ~30 minecraft:stone_bricks
fill ~3 ~2 ~30 ~3 ~3 ~30 minecraft:mossy_stone_bricks
setblock ~4 ~2 ~26 minecraft:cracked_deepslate_bricks
fill ~4 ~2 ~28 ~4 ~8 ~29 minecraft:stone_bricks
fill ~4 ~2 ~30 ~4 ~7 ~30 minecraft:deepslate_bricks
setblock ~0 ~3 ~26 minecraft:polished_blackstone_bricks
setblock ~0 ~3 ~27 minecraft:cracked_stone_bricks
setblock ~0 ~3 ~28 minecraft:mossy_stone_bricks
fill ~0 ~3 ~29 ~0 ~8 ~29 minecraft:stone_bricks
fill ~1 ~3 ~26 ~2 ~3 ~26 minecraft:stone_bricks
fill ~1 ~3 ~30 ~2 ~3 ~30 minecraft:mossy_stone_bricks
setblock ~3 ~3 ~26 minecraft:mossy_stone_bricks
setblock ~4 ~3 ~26 minecraft:stone_bricks
setblock ~0 ~4 ~26 minecraft:cracked_deepslate_bricks
fill ~0 ~4 ~27 ~0 ~8 ~27 minecraft:stone_bricks
setblock ~0 ~4 ~28 minecraft:cracked_stone_bricks
setblock ~1 ~4 ~26 minecraft:deepslate_bricks
fill ~1 ~4 ~30 ~3 ~4 ~30 minecraft:stone_bricks
fill ~2 ~4 ~26 ~3 ~8 ~26 minecraft:stone_bricks
setblock ~4 ~4 ~26 minecraft:deepslate_bricks
fill ~4 ~4 ~27 ~4 ~8 ~27 minecraft:stone_bricks
setblock ~0 ~5 ~26 minecraft:polished_blackstone_bricks
setblock ~0 ~5 ~30 minecraft:deepslate_bricks
fill ~1 ~5 ~26 ~1 ~8 ~26 minecraft:stone_bricks
setblock ~1 ~5 ~30 minecraft:stone_bricks
setblock ~3 ~5 ~30 minecraft:stone_bricks
setblock ~4 ~5 ~26 minecraft:cracked_deepslate_bricks
setblock ~0 ~6 ~26 minecraft:deepslate_bricks
fill ~0 ~6 ~30 ~0 ~7 ~30 minecraft:polished_blackstone_bricks
setblock ~1 ~6 ~30 minecraft:deepslate_bricks
setblock ~3 ~6 ~30 minecraft:deepslate_bricks
fill ~4 ~6 ~26 ~4 ~7 ~26 minecraft:deepslate_bricks
setblock ~0 ~7 ~26 minecraft:stone_bricks
setblock ~0 ~7 ~28 minecraft:cracked_stone_bricks
fill ~1 ~7 ~30 ~2 ~8 ~30 minecraft:stone_bricks
setblock ~3 ~7 ~30 minecraft:cracked_stone_bricks
setblock ~0 ~8 ~26 minecraft:deepslate_bricks
setblock ~0 ~8 ~28 minecraft:stone_bricks
fill ~0 ~8 ~30 ~0 ~9 ~30 minecraft:deepslate_bricks
setblock ~3 ~8 ~30 minecraft:stone_bricks
setblock ~4 ~8 ~26 minecraft:cracked_deepslate_bricks
fill ~4 ~8 ~30 ~4 ~10 ~30 minecraft:cracked_deepslate_bricks
fill ~0 ~9 ~26 ~0 ~9 ~27 minecraft:cracked_deepslate_bricks
setblock ~0 ~9 ~28 minecraft:deepslate_bricks
setblock ~0 ~9 ~29 minecraft:cracked_deepslate_bricks
fill ~1 ~9 ~26 ~4 ~9 ~26 minecraft:deepslate_bricks
setblock ~1 ~9 ~30 minecraft:cracked_deepslate_bricks
fill ~2 ~9 ~30 ~3 ~9 ~30 minecraft:deepslate_bricks
fill ~4 ~9 ~27 ~4 ~9 ~28 minecraft:cracked_deepslate_bricks
setblock ~4 ~9 ~29 minecraft:deepslate_bricks
setblock ~0 ~10 ~26 minecraft:polished_blackstone_bricks
fill ~0 ~10 ~27 ~0 ~10 ~28 minecraft:stone_bricks
setblock ~0 ~10 ~29 minecraft:deepslate_bricks
fill ~0 ~10 ~30 ~3 ~10 ~30 minecraft:stone_bricks
setblock ~1 ~10 ~26 minecraft:cracked_stone_bricks
setblock ~2 ~10 ~26 minecraft:stone_bricks
setblock ~3 ~10 ~26 minecraft:cracked_stone_bricks
setblock ~4 ~10 ~26 minecraft:deepslate_bricks
fill ~4 ~10 ~27 ~4 ~10 ~29 minecraft:stone_bricks

# the plinth of the tower: a lip of stone brick stairs at y 1 along the west face
fill ~0 ~1 ~27 ~0 ~1 ~29 minecraft:cobblestone_stairs[facing=east,half=bottom]

# slits at y 5..6 on the west and the end faces, and the door on the inner side
fill ~0 ~5 ~28 ~0 ~6 ~28 minecraft:air
fill ~2 ~5 ~30 ~2 ~6 ~30 minecraft:air

# the floor at y 11: planks, the ladder hole at x 3, z 28
fill ~1 ~11 ~27 ~3 ~11 ~27 minecraft:spruce_planks
fill ~1 ~11 ~28 ~2 ~11 ~28 minecraft:spruce_planks
setblock ~1 ~11 ~29 minecraft:spruce_planks
setblock ~3 ~11 ~29 minecraft:spruce_planks

# the ladder up the inner wall: (3, 1..12, 28) on the wall x 4 (the door is at z 27)
fill ~3 ~1 ~28 ~3 ~12 ~28 minecraft:ladder[facing=west]

# the door on the inner side: a doorway two high at x 4 (the courtyard side) and a lantern hung inside
fill ~4 ~1 ~27 ~4 ~2 ~27 minecraft:air
setblock ~2 ~3 ~27 minecraft:lantern[hanging=true]
setblock ~4 ~3 ~27 minecraft:chiseled_stone_bricks
setblock ~2 ~4 ~27 minecraft:chain[axis=y]

# the string course at y 11: chiseled stone bricks on the faces, the corbels of the guard room above
setblock ~0 ~11 ~26 minecraft:chiseled_stone_bricks
setblock ~0 ~11 ~27 minecraft:mossy_stone_bricks
setblock ~0 ~11 ~28 minecraft:chiseled_stone_bricks
setblock ~0 ~11 ~29 minecraft:stone_bricks
setblock ~0 ~11 ~30 minecraft:chiseled_stone_bricks
setblock ~1 ~11 ~26 minecraft:stone_bricks
setblock ~1 ~11 ~30 minecraft:deepslate_bricks
setblock ~2 ~11 ~26 minecraft:chiseled_stone_bricks
setblock ~2 ~11 ~30 minecraft:chiseled_stone_bricks
setblock ~3 ~11 ~26 minecraft:stone_bricks
setblock ~3 ~11 ~30 minecraft:stone_bricks
setblock ~4 ~11 ~26 minecraft:chiseled_stone_bricks
setblock ~4 ~11 ~27 minecraft:mossy_stone_bricks
setblock ~4 ~11 ~28 minecraft:chiseled_stone_bricks
setblock ~4 ~11 ~29 minecraft:stone_bricks
setblock ~4 ~11 ~30 minecraft:chiseled_stone_bricks

# the open platform at feet 12: a ring of merlons y 12..13 with a gap onto the wall-walk, a brazier in the middle
setblock ~0 ~12 ~26 minecraft:deepslate_bricks
setblock ~0 ~12 ~27 minecraft:cracked_stone_bricks
setblock ~0 ~12 ~28 minecraft:stone_bricks
setblock ~0 ~12 ~29 minecraft:cracked_stone_bricks
setblock ~0 ~12 ~30 minecraft:deepslate_bricks
fill ~1 ~12 ~26 ~2 ~13 ~26 minecraft:air
setblock ~1 ~12 ~30 minecraft:cracked_stone_bricks
fill ~2 ~12 ~30 ~2 ~13 ~30 minecraft:stone_bricks
fill ~3 ~12 ~26 ~4 ~12 ~26 minecraft:stone_bricks
fill ~3 ~12 ~30 ~4 ~12 ~30 minecraft:mossy_stone_bricks
fill ~4 ~12 ~27 ~4 ~12 ~29 minecraft:stone_bricks
setblock ~0 ~13 ~26 minecraft:cracked_stone_bricks
setblock ~0 ~13 ~28 minecraft:air
setblock ~0 ~13 ~30 minecraft:stone_bricks
setblock ~4 ~13 ~26 minecraft:cracked_stone_bricks
setblock ~4 ~13 ~28 minecraft:mossy_stone_bricks
setblock ~4 ~13 ~30 minecraft:stone_bricks

# a brazier of the watch: a campfire on a stone dish on the platform (x 2, z 29), a barrel of arrows
setblock ~2 ~11 ~29 minecraft:stone_bricks
setblock ~1 ~12 ~27 minecraft:barrel[facing=up]
setblock ~2 ~12 ~29 minecraft:campfire[lit=true,signal_fire=false,facing=north]

# weathering: moss on the footing, vines on the damp recesses of the outer face
fill ~0 ~7 ~8 ~0 ~8 ~8 minecraft:vine[east=true]
fill ~0 ~7 ~13 ~0 ~8 ~13 minecraft:vine[east=true]
setblock ~0 ~7 ~16 minecraft:vine[east=true]
fill ~0 ~7 ~19 ~0 ~8 ~19 minecraft:vine[east=true]
fill ~0 ~8 ~6 ~0 ~9 ~6 minecraft:vine[east=true]
fill ~0 ~8 ~11 ~0 ~9 ~11 minecraft:vine[east=true]
setblock ~0 ~8 ~24 minecraft:vine[east=true]
setblock ~0 ~9 ~16 minecraft:vine[east=true]
