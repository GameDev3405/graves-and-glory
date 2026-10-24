# size 27 14 19
# frostmarch/lodge: caravan lodge and yard of the Frostmarch pass: a long dark-timber hall on a stone plinth under a snow-laden roof with a cross gable and a tall chimney, a porch on its yard door, a yard inside a low stone wall with a covered wagon, a well and firewood, and a gatehouse on the lane with a lit upper room and an east window.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Hall x 0..16, z 6..14 (open door east at x 16, z 9..11; the wing door at x 7..9, z 6); north wing x 5..11, z 0..5 under a cross gable (door north at x 7..9, z 0); porch x 17..19, z 7..13; yard x 17..25, z 1..17 inside a 2-high wall (north z 0, east x 26 for z 0..10, south z 18, return walls at x 16).
# Gatehouse x 23..26, z 11..17: passage z 12..16 (the gate at x 26, z 12..16), clear y 1..4 across x 23..26; room floor y 6 (feet 7), interior x 24..25, z 12..16, east window x 26, y 8..9, z 14..15; entry by the ladder (24, 1..6, 10) on its north face and the doorway (24, 7..8, 11).
# Cover line from the yard (17.5, 2.6, 2.5) to the lane (42.5, g+2.6, 20.5) is stopped by the east wall (x 26, y 1..2, z 0..10); sight from the room (25.5, 8.6, 14.5) through the window to the lane is clear.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~4 ~4 ~5 minecraft:air
fill ~0 ~1 ~15 ~15 ~4 ~18 minecraft:air
fill ~1 ~1 ~7 ~3 ~6 ~8 minecraft:air
fill ~1 ~1 ~12 ~3 ~6 ~13 minecraft:air
fill ~4 ~1 ~8 ~4 ~2 ~12 minecraft:air
fill ~6 ~1 ~2 ~7 ~6 ~2 minecraft:air
fill ~6 ~1 ~5 ~6 ~6 ~5 minecraft:air
fill ~7 ~1 ~3 ~7 ~7 ~5 minecraft:air
fill ~8 ~1 ~1 ~9 ~7 ~5 minecraft:air
fill ~10 ~1 ~3 ~10 ~6 ~5 minecraft:air
fill ~12 ~1 ~0 ~15 ~4 ~5 minecraft:air
fill ~12 ~1 ~7 ~12 ~2 ~13 minecraft:air
fill ~13 ~1 ~8 ~15 ~7 ~11 minecraft:air
fill ~13 ~1 ~12 ~13 ~6 ~13 minecraft:air
fill ~15 ~1 ~12 ~15 ~7 ~12 minecraft:air
fill ~17 ~1 ~7 ~18 ~3 ~12 minecraft:air
fill ~17 ~1 ~13 ~17 ~3 ~13 minecraft:air
fill ~18 ~1 ~2 ~20 ~13 ~2 minecraft:air
fill ~18 ~1 ~3 ~18 ~13 ~4 minecraft:air
fill ~18 ~1 ~6 ~19 ~13 ~6 minecraft:air
fill ~19 ~1 ~4 ~20 ~13 ~5 minecraft:air
fill ~19 ~1 ~8 ~22 ~2 ~12 minecraft:air
fill ~19 ~1 ~14 ~19 ~1 ~16 minecraft:air
fill ~20 ~1 ~7 ~23 ~13 ~7 minecraft:air
fill ~20 ~1 ~13 ~20 ~1 ~16 minecraft:air
fill ~21 ~1 ~6 ~24 ~13 ~6 minecraft:air
fill ~22 ~1 ~13 ~22 ~13 ~14 minecraft:air
fill ~22 ~1 ~16 ~22 ~13 ~16 minecraft:air
fill ~23 ~1 ~9 ~24 ~13 ~9 minecraft:air
fill ~23 ~1 ~10 ~23 ~8 ~10 minecraft:air
fill ~23 ~1 ~12 ~26 ~4 ~16 minecraft:air
fill ~4 ~2 ~7 ~11 ~4 ~7 minecraft:air
fill ~4 ~2 ~13 ~11 ~4 ~13 minecraft:air
fill ~5 ~2 ~9 ~11 ~2 ~11 minecraft:air
fill ~6 ~2 ~1 ~7 ~6 ~1 minecraft:air
fill ~6 ~2 ~3 ~6 ~6 ~4 minecraft:air
fill ~10 ~2 ~2 ~10 ~6 ~2 minecraft:air
fill ~14 ~2 ~12 ~14 ~6 ~13 minecraft:air
fill ~17 ~2 ~1 ~17 ~13 ~6 minecraft:air
fill ~17 ~2 ~14 ~17 ~13 ~17 minecraft:air
fill ~19 ~2 ~17 ~20 ~13 ~17 minecraft:air
fill ~20 ~2 ~3 ~20 ~13 ~3 minecraft:air
fill ~20 ~2 ~13 ~20 ~13 ~13 minecraft:air
fill ~21 ~2 ~2 ~21 ~13 ~5 minecraft:air
fill ~22 ~2 ~1 ~25 ~13 ~1 minecraft:air
fill ~22 ~2 ~5 ~24 ~13 ~5 minecraft:air
fill ~22 ~2 ~17 ~22 ~13 ~17 minecraft:air
setblock ~23 ~2 ~3 minecraft:air
fill ~24 ~2 ~7 ~24 ~13 ~8 minecraft:air
fill ~25 ~2 ~2 ~25 ~5 ~10 minecraft:air
fill ~4 ~3 ~9 ~4 ~4 ~11 minecraft:air
fill ~5 ~3 ~8 ~11 ~4 ~9 minecraft:air
fill ~5 ~3 ~10 ~7 ~7 ~12 minecraft:air
fill ~8 ~3 ~11 ~8 ~4 ~12 minecraft:air
fill ~9 ~3 ~10 ~11 ~7 ~12 minecraft:air
fill ~10 ~3 ~1 ~10 ~6 ~1 minecraft:air
fill ~12 ~3 ~7 ~15 ~4 ~7 minecraft:air
fill ~12 ~3 ~9 ~12 ~4 ~11 minecraft:air
fill ~12 ~3 ~13 ~12 ~4 ~13 minecraft:air
fill ~15 ~3 ~13 ~15 ~6 ~13 minecraft:air
fill ~16 ~3 ~5 ~16 ~4 ~5 minecraft:air
fill ~16 ~3 ~15 ~16 ~4 ~15 minecraft:air
fill ~18 ~3 ~5 ~18 ~13 ~5 minecraft:air
setblock ~18 ~3 ~13 minecraft:air
fill ~19 ~3 ~3 ~19 ~13 ~3 minecraft:air
fill ~19 ~3 ~8 ~23 ~3 ~8 minecraft:air
fill ~19 ~3 ~9 ~22 ~3 ~9 minecraft:air
fill ~19 ~3 ~11 ~19 ~3 ~12 minecraft:air
fill ~20 ~3 ~10 ~22 ~13 ~12 minecraft:air
fill ~22 ~3 ~3 ~22 ~4 ~3 minecraft:air
fill ~22 ~3 ~15 ~22 ~13 ~15 minecraft:air
fill ~23 ~3 ~2 ~23 ~4 ~2 minecraft:air
fill ~23 ~3 ~4 ~23 ~4 ~4 minecraft:air
fill ~23 ~3 ~18 ~25 ~8 ~18 minecraft:air
fill ~24 ~3 ~3 ~24 ~4 ~3 minecraft:air
fill ~26 ~3 ~2 ~26 ~13 ~2 minecraft:air
fill ~16 ~4 ~1 ~16 ~13 ~4 minecraft:air
fill ~16 ~4 ~16 ~16 ~13 ~17 minecraft:air
fill ~17 ~4 ~0 ~20 ~13 ~0 minecraft:air
fill ~17 ~4 ~8 ~18 ~4 ~12 minecraft:air
fill ~17 ~4 ~18 ~17 ~13 ~18 minecraft:air
fill ~18 ~4 ~1 ~18 ~13 ~1 minecraft:air
fill ~18 ~4 ~17 ~18 ~13 ~18 minecraft:air
fill ~19 ~4 ~18 ~20 ~13 ~18 minecraft:air
fill ~20 ~4 ~8 ~23 ~13 ~8 minecraft:air
fill ~20 ~4 ~9 ~22 ~13 ~9 minecraft:air
fill ~21 ~4 ~13 ~21 ~13 ~13 minecraft:air
fill ~21 ~4 ~17 ~21 ~13 ~17 minecraft:air
fill ~22 ~4 ~0 ~25 ~13 ~0 minecraft:air
fill ~22 ~4 ~18 ~22 ~13 ~18 minecraft:air
fill ~26 ~4 ~1 ~26 ~13 ~1 minecraft:air
fill ~26 ~4 ~3 ~26 ~13 ~9 minecraft:air
fill ~0 ~5 ~0 ~3 ~13 ~4 minecraft:air
fill ~0 ~5 ~16 ~15 ~13 ~18 minecraft:air
fill ~5 ~5 ~7 ~7 ~6 ~9 minecraft:air
fill ~5 ~5 ~13 ~7 ~6 ~13 minecraft:air
fill ~9 ~5 ~7 ~11 ~6 ~9 minecraft:air
fill ~9 ~5 ~13 ~11 ~6 ~13 minecraft:air
fill ~13 ~5 ~0 ~16 ~13 ~0 minecraft:air
fill ~13 ~5 ~1 ~15 ~13 ~4 minecraft:air
fill ~13 ~5 ~7 ~15 ~6 ~7 minecraft:air
fill ~16 ~5 ~18 ~16 ~13 ~18 minecraft:air
fill ~17 ~5 ~7 ~19 ~13 ~7 minecraft:air
fill ~17 ~5 ~9 ~19 ~5 ~11 minecraft:air
fill ~17 ~5 ~13 ~19 ~13 ~13 minecraft:air
fill ~18 ~5 ~14 ~18 ~13 ~14 minecraft:air
fill ~19 ~5 ~1 ~20 ~13 ~1 minecraft:air
fill ~20 ~5 ~6 ~20 ~13 ~6 minecraft:air
fill ~21 ~5 ~0 ~21 ~13 ~1 minecraft:air
fill ~21 ~5 ~18 ~21 ~13 ~18 minecraft:air
fill ~23 ~5 ~13 ~23 ~5 ~15 minecraft:air
fill ~24 ~5 ~12 ~25 ~5 ~12 minecraft:air
setblock ~24 ~5 ~14 minecraft:air
fill ~24 ~5 ~16 ~25 ~5 ~16 minecraft:air
fill ~25 ~5 ~13 ~26 ~5 ~15 minecraft:air
fill ~26 ~5 ~0 ~26 ~13 ~0 minecraft:air
fill ~26 ~5 ~10 ~26 ~8 ~10 minecraft:air
fill ~26 ~5 ~18 ~26 ~8 ~18 minecraft:air
fill ~0 ~6 ~5 ~3 ~13 ~5 minecraft:air
fill ~0 ~6 ~15 ~16 ~13 ~15 minecraft:air
fill ~4 ~6 ~0 ~4 ~13 ~5 minecraft:air
fill ~4 ~6 ~7 ~4 ~6 ~13 minecraft:air
fill ~6 ~6 ~6 ~10 ~6 ~6 minecraft:air
fill ~8 ~6 ~7 ~8 ~6 ~13 minecraft:air
fill ~12 ~6 ~0 ~12 ~13 ~5 minecraft:air
fill ~12 ~6 ~7 ~12 ~6 ~13 minecraft:air
fill ~13 ~6 ~5 ~16 ~13 ~5 minecraft:air
fill ~17 ~6 ~8 ~19 ~13 ~8 minecraft:air
fill ~17 ~6 ~10 ~19 ~6 ~10 minecraft:air
fill ~17 ~6 ~12 ~19 ~13 ~12 minecraft:air
fill ~18 ~6 ~16 ~21 ~13 ~16 minecraft:air
fill ~19 ~6 ~14 ~21 ~13 ~14 minecraft:air
fill ~25 ~6 ~2 ~25 ~13 ~9 minecraft:air
fill ~0 ~7 ~6 ~4 ~13 ~6 minecraft:air
fill ~0 ~7 ~14 ~16 ~13 ~14 minecraft:air
fill ~1 ~7 ~8 ~6 ~7 ~8 minecraft:air
fill ~1 ~7 ~12 ~3 ~7 ~12 minecraft:air
fill ~4 ~7 ~9 ~6 ~8 ~9 minecraft:air
fill ~4 ~7 ~10 ~4 ~7 ~12 minecraft:air
fill ~5 ~7 ~0 ~5 ~13 ~6 minecraft:air
fill ~7 ~7 ~1 ~7 ~7 ~2 minecraft:air
fill ~7 ~7 ~6 ~9 ~7 ~9 minecraft:air
fill ~8 ~7 ~10 ~8 ~7 ~12 minecraft:air
fill ~10 ~7 ~8 ~12 ~7 ~9 minecraft:air
fill ~11 ~7 ~0 ~11 ~13 ~6 minecraft:air
fill ~12 ~7 ~6 ~16 ~13 ~6 minecraft:air
fill ~12 ~7 ~10 ~12 ~7 ~12 minecraft:air
fill ~13 ~7 ~12 ~14 ~7 ~12 minecraft:air
fill ~17 ~7 ~9 ~19 ~13 ~9 minecraft:air
fill ~17 ~7 ~11 ~19 ~13 ~11 minecraft:air
fill ~18 ~7 ~15 ~21 ~13 ~15 minecraft:air
fill ~22 ~7 ~2 ~24 ~13 ~4 minecraft:air
fill ~24 ~7 ~10 ~24 ~8 ~10 minecraft:air
fill ~24 ~7 ~12 ~24 ~10 ~15 minecraft:air
fill ~25 ~7 ~13 ~25 ~11 ~15 minecraft:air
fill ~0 ~8 ~7 ~5 ~13 ~7 minecraft:air
fill ~0 ~8 ~13 ~16 ~13 ~13 minecraft:air
fill ~4 ~8 ~10 ~6 ~8 ~11 minecraft:air
fill ~6 ~8 ~0 ~6 ~13 ~7 minecraft:air
fill ~7 ~8 ~9 ~7 ~8 ~11 minecraft:air
fill ~8 ~8 ~1 ~8 ~8 ~11 minecraft:air
fill ~9 ~8 ~9 ~15 ~8 ~11 minecraft:air
fill ~10 ~8 ~0 ~10 ~13 ~7 minecraft:air
fill ~11 ~8 ~7 ~16 ~13 ~7 minecraft:air
fill ~17 ~8 ~10 ~19 ~13 ~10 minecraft:air
fill ~24 ~8 ~16 ~25 ~10 ~16 minecraft:air
setblock ~25 ~8 ~10 minecraft:air
fill ~25 ~8 ~12 ~25 ~10 ~12 minecraft:air
fill ~0 ~9 ~8 ~6 ~13 ~8 minecraft:air
fill ~0 ~9 ~12 ~16 ~13 ~12 minecraft:air
fill ~4 ~9 ~10 ~15 ~9 ~10 minecraft:air
fill ~7 ~9 ~0 ~7 ~13 ~8 minecraft:air
fill ~9 ~9 ~0 ~9 ~13 ~8 minecraft:air
fill ~10 ~9 ~8 ~16 ~13 ~8 minecraft:air
fill ~0 ~10 ~9 ~0 ~13 ~9 minecraft:air
fill ~0 ~10 ~11 ~0 ~13 ~11 minecraft:air
fill ~4 ~10 ~9 ~7 ~13 ~9 minecraft:air
fill ~4 ~10 ~11 ~16 ~13 ~11 minecraft:air
fill ~8 ~10 ~0 ~8 ~13 ~9 minecraft:air
fill ~9 ~10 ~9 ~16 ~13 ~9 minecraft:air
fill ~23 ~10 ~10 ~26 ~13 ~10 minecraft:air
fill ~23 ~10 ~18 ~26 ~13 ~18 minecraft:air
fill ~0 ~11 ~10 ~0 ~13 ~10 minecraft:air
fill ~4 ~11 ~10 ~16 ~13 ~10 minecraft:air
fill ~23 ~11 ~11 ~26 ~13 ~11 minecraft:air
fill ~23 ~11 ~17 ~26 ~13 ~17 minecraft:air
setblock ~24 ~11 ~13 minecraft:air
setblock ~24 ~11 ~15 minecraft:air
fill ~23 ~12 ~12 ~26 ~13 ~12 minecraft:air
fill ~23 ~12 ~16 ~26 ~13 ~16 minecraft:air
setblock ~25 ~12 ~14 minecraft:air
setblock ~1 ~13 ~11 minecraft:air
fill ~23 ~13 ~13 ~26 ~13 ~13 minecraft:air
fill ~23 ~13 ~15 ~26 ~13 ~15 minecraft:air

# the hall floor: spruce boards on stone flags along the walls; the wing and the porch stone flags
setblock ~0 ~0 ~6 minecraft:stone_bricks
setblock ~0 ~0 ~7 minecraft:andesite
setblock ~0 ~0 ~8 minecraft:stone_bricks
setblock ~0 ~0 ~9 minecraft:andesite
setblock ~0 ~0 ~10 minecraft:stone_bricks
fill ~0 ~0 ~11 ~0 ~0 ~13 minecraft:andesite
setblock ~0 ~0 ~14 minecraft:stone_bricks
fill ~1 ~0 ~6 ~2 ~0 ~6 minecraft:andesite
fill ~1 ~0 ~7 ~13 ~0 ~8 minecraft:spruce_planks
fill ~1 ~0 ~9 ~6 ~0 ~11 minecraft:spruce_planks
fill ~1 ~0 ~12 ~1 ~0 ~13 minecraft:spruce_planks
setblock ~1 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~2 ~0 ~12 minecraft:dark_oak_planks
fill ~2 ~0 ~13 ~4 ~0 ~13 minecraft:spruce_planks
setblock ~2 ~0 ~14 minecraft:stone_bricks
fill ~3 ~0 ~6 ~4 ~0 ~6 minecraft:stone_bricks
fill ~3 ~0 ~12 ~6 ~0 ~12 minecraft:spruce_planks
setblock ~3 ~0 ~14 minecraft:andesite
setblock ~4 ~0 ~14 minecraft:cracked_stone_bricks
fill ~5 ~0 ~0 ~5 ~0 ~1 minecraft:stone_bricks
setblock ~5 ~0 ~2 minecraft:andesite
fill ~5 ~0 ~3 ~6 ~0 ~3 minecraft:stone_bricks
fill ~5 ~0 ~4 ~5 ~0 ~5 minecraft:stone_bricks
setblock ~5 ~0 ~6 minecraft:cracked_stone_bricks
setblock ~5 ~0 ~13 minecraft:dark_oak_planks
fill ~5 ~0 ~14 ~9 ~0 ~14 minecraft:stone_bricks
setblock ~6 ~0 ~0 minecraft:cobblestone
fill ~6 ~0 ~1 ~7 ~0 ~1 minecraft:cracked_stone_bricks
fill ~6 ~0 ~2 ~7 ~0 ~2 minecraft:stone_bricks
setblock ~6 ~0 ~4 minecraft:cobblestone
fill ~6 ~0 ~5 ~6 ~0 ~6 minecraft:cracked_stone_bricks
setblock ~6 ~0 ~13 minecraft:spruce_planks
setblock ~7 ~0 ~0 minecraft:stone_bricks
setblock ~7 ~0 ~3 minecraft:cracked_stone_bricks
setblock ~7 ~0 ~4 minecraft:andesite
setblock ~7 ~0 ~5 minecraft:cobblestone
fill ~7 ~0 ~6 ~9 ~0 ~6 minecraft:stone_bricks
setblock ~7 ~0 ~9 minecraft:dark_oak_planks
fill ~7 ~0 ~10 ~7 ~0 ~13 minecraft:spruce_planks
setblock ~8 ~0 ~0 minecraft:andesite
setblock ~8 ~0 ~1 minecraft:stone_bricks
setblock ~8 ~0 ~2 minecraft:cobblestone
setblock ~8 ~0 ~3 minecraft:stone_bricks
setblock ~8 ~0 ~4 minecraft:cobblestone
setblock ~8 ~0 ~5 minecraft:cracked_stone_bricks
fill ~8 ~0 ~9 ~8 ~0 ~13 minecraft:spruce_planks
fill ~9 ~0 ~0 ~11 ~0 ~1 minecraft:stone_bricks
fill ~9 ~0 ~2 ~9 ~0 ~3 minecraft:stone_bricks
fill ~9 ~0 ~4 ~9 ~0 ~5 minecraft:cracked_stone_bricks
setblock ~9 ~0 ~9 minecraft:dark_oak_planks
fill ~9 ~0 ~10 ~9 ~0 ~13 minecraft:spruce_planks
setblock ~10 ~0 ~2 minecraft:andesite
fill ~10 ~0 ~3 ~10 ~0 ~5 minecraft:stone_bricks
fill ~10 ~0 ~6 ~11 ~0 ~6 minecraft:cracked_stone_bricks
fill ~10 ~0 ~9 ~14 ~0 ~11 minecraft:spruce_planks
fill ~10 ~0 ~12 ~11 ~0 ~13 minecraft:spruce_planks
setblock ~10 ~0 ~14 minecraft:andesite
fill ~11 ~0 ~2 ~11 ~0 ~3 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~4 minecraft:cobblestone
setblock ~11 ~0 ~5 minecraft:stone_bricks
setblock ~11 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~6 minecraft:cobblestone
setblock ~12 ~0 ~12 minecraft:dark_oak_planks
setblock ~12 ~0 ~13 minecraft:spruce_planks
setblock ~12 ~0 ~14 minecraft:cobblestone
fill ~13 ~0 ~6 ~14 ~0 ~6 minecraft:stone_bricks
fill ~13 ~0 ~12 ~14 ~0 ~13 minecraft:spruce_planks
setblock ~13 ~0 ~14 minecraft:andesite
fill ~14 ~0 ~7 ~14 ~0 ~8 minecraft:dark_oak_planks
setblock ~14 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~6 minecraft:andesite
fill ~15 ~0 ~7 ~15 ~0 ~13 minecraft:spruce_planks
setblock ~15 ~0 ~14 minecraft:stone_bricks
setblock ~16 ~0 ~6 minecraft:cobblestone
setblock ~16 ~0 ~7 minecraft:andesite
fill ~16 ~0 ~8 ~16 ~0 ~10 minecraft:stone_bricks
setblock ~16 ~0 ~11 minecraft:cobblestone
setblock ~16 ~0 ~12 minecraft:andesite
setblock ~16 ~0 ~13 minecraft:stone_bricks
setblock ~16 ~0 ~14 minecraft:cracked_stone_bricks

# the yard: cobble, gravel and packed earth, a trodden band from the hall door across to the gate, flags at the porch and under the gatehouse
fill ~17 ~0 ~1 ~17 ~0 ~2 minecraft:cobblestone
fill ~17 ~0 ~3 ~17 ~0 ~4 minecraft:packed_mud
fill ~17 ~0 ~5 ~17 ~0 ~6 minecraft:cobblestone
fill ~17 ~0 ~7 ~17 ~0 ~10 minecraft:stone_bricks
setblock ~17 ~0 ~11 minecraft:cracked_stone_bricks
fill ~17 ~0 ~12 ~18 ~0 ~13 minecraft:stone_bricks
setblock ~17 ~0 ~14 minecraft:coarse_dirt
fill ~17 ~0 ~15 ~18 ~0 ~15 minecraft:cobblestone
setblock ~17 ~0 ~16 minecraft:gravel
setblock ~17 ~0 ~17 minecraft:coarse_dirt
fill ~18 ~0 ~1 ~18 ~0 ~2 minecraft:packed_mud
fill ~18 ~0 ~3 ~19 ~0 ~3 minecraft:cobblestone
fill ~18 ~0 ~4 ~19 ~0 ~4 minecraft:gravel
setblock ~18 ~0 ~5 minecraft:gravel
setblock ~18 ~0 ~6 minecraft:packed_mud
setblock ~18 ~0 ~7 minecraft:andesite
setblock ~18 ~0 ~8 minecraft:cobblestone
fill ~18 ~0 ~9 ~18 ~0 ~11 minecraft:cracked_stone_bricks
fill ~18 ~0 ~14 ~20 ~0 ~14 minecraft:cobblestone
fill ~18 ~0 ~16 ~18 ~0 ~17 minecraft:cobblestone
setblock ~19 ~0 ~1 minecraft:cobblestone
setblock ~19 ~0 ~2 minecraft:packed_mud
fill ~19 ~0 ~5 ~19 ~0 ~6 minecraft:andesite
setblock ~19 ~0 ~7 minecraft:stone_bricks
fill ~19 ~0 ~8 ~19 ~0 ~9 minecraft:andesite
fill ~19 ~0 ~10 ~19 ~0 ~11 minecraft:stone_bricks
setblock ~19 ~0 ~12 minecraft:andesite
setblock ~19 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~19 ~0 ~15 minecraft:coarse_dirt
fill ~19 ~0 ~16 ~20 ~0 ~16 minecraft:packed_mud
setblock ~19 ~0 ~17 minecraft:gravel
setblock ~20 ~0 ~1 minecraft:packed_mud
setblock ~20 ~0 ~2 minecraft:gravel
setblock ~20 ~0 ~3 minecraft:packed_mud
setblock ~20 ~0 ~4 minecraft:coarse_dirt
setblock ~20 ~0 ~5 minecraft:cobblestone
setblock ~20 ~0 ~6 minecraft:coarse_dirt
fill ~20 ~0 ~7 ~20 ~0 ~8 minecraft:cobblestone
setblock ~20 ~0 ~9 minecraft:packed_mud
setblock ~20 ~0 ~10 minecraft:gravel
fill ~20 ~0 ~11 ~21 ~0 ~11 minecraft:coarse_dirt
fill ~20 ~0 ~12 ~20 ~0 ~13 minecraft:gravel
setblock ~20 ~0 ~15 minecraft:gravel
setblock ~20 ~0 ~17 minecraft:cobblestone
fill ~21 ~0 ~1 ~21 ~0 ~2 minecraft:cobblestone
setblock ~21 ~0 ~3 minecraft:coarse_dirt
setblock ~21 ~0 ~4 minecraft:gravel
setblock ~21 ~0 ~5 minecraft:coarse_dirt
fill ~21 ~0 ~6 ~22 ~0 ~6 minecraft:cobblestone
fill ~21 ~0 ~7 ~21 ~0 ~10 minecraft:packed_mud
setblock ~21 ~0 ~12 minecraft:packed_mud
setblock ~21 ~0 ~13 minecraft:gravel
fill ~21 ~0 ~14 ~22 ~0 ~15 minecraft:packed_mud
setblock ~21 ~0 ~16 minecraft:coarse_dirt
setblock ~21 ~0 ~17 minecraft:andesite
setblock ~22 ~0 ~1 minecraft:andesite
setblock ~22 ~0 ~2 minecraft:packed_mud
setblock ~22 ~0 ~3 minecraft:gravel
setblock ~22 ~0 ~4 minecraft:cobblestone
setblock ~22 ~0 ~5 minecraft:gravel
setblock ~22 ~0 ~7 minecraft:coarse_dirt
setblock ~22 ~0 ~8 minecraft:gravel
fill ~22 ~0 ~9 ~22 ~0 ~10 minecraft:coarse_dirt
fill ~22 ~0 ~11 ~22 ~0 ~12 minecraft:gravel
setblock ~22 ~0 ~13 minecraft:coarse_dirt
fill ~22 ~0 ~16 ~22 ~0 ~17 minecraft:gravel
setblock ~23 ~0 ~1 minecraft:packed_mud
fill ~23 ~0 ~2 ~23 ~0 ~4 minecraft:gravel
setblock ~23 ~0 ~5 minecraft:cobblestone
setblock ~23 ~0 ~6 minecraft:andesite
setblock ~23 ~0 ~7 minecraft:packed_mud
fill ~23 ~0 ~8 ~24 ~0 ~8 minecraft:cobblestone
setblock ~23 ~0 ~9 minecraft:cobblestone
setblock ~23 ~0 ~10 minecraft:gravel
setblock ~23 ~0 ~11 minecraft:cracked_stone_bricks
fill ~23 ~0 ~12 ~23 ~0 ~14 minecraft:stone_bricks
setblock ~23 ~0 ~15 minecraft:cracked_stone_bricks
fill ~23 ~0 ~16 ~23 ~0 ~17 minecraft:stone_bricks
setblock ~24 ~0 ~1 minecraft:andesite
setblock ~24 ~0 ~2 minecraft:coarse_dirt
setblock ~24 ~0 ~3 minecraft:gravel
fill ~24 ~0 ~4 ~24 ~0 ~6 minecraft:cobblestone
setblock ~24 ~0 ~7 minecraft:gravel
fill ~24 ~0 ~9 ~25 ~0 ~10 minecraft:gravel
fill ~24 ~0 ~11 ~26 ~0 ~13 minecraft:stone_bricks
fill ~24 ~0 ~14 ~24 ~0 ~17 minecraft:stone_bricks
setblock ~25 ~0 ~1 minecraft:cobblestone
setblock ~25 ~0 ~2 minecraft:packed_mud
setblock ~25 ~0 ~3 minecraft:cobblestone
fill ~25 ~0 ~4 ~25 ~0 ~6 minecraft:coarse_dirt
setblock ~25 ~0 ~7 minecraft:cobblestone
setblock ~25 ~0 ~8 minecraft:coarse_dirt
setblock ~25 ~0 ~14 minecraft:cobblestone
fill ~25 ~0 ~15 ~25 ~0 ~17 minecraft:stone_bricks
fill ~26 ~0 ~14 ~26 ~0 ~16 minecraft:cracked_stone_bricks
setblock ~26 ~0 ~17 minecraft:stone_bricks

# the plinth: cobblestone at the foot, stone bricks above (y 1..2)
fill ~0 ~1 ~7 ~0 ~1 ~13 minecraft:cobblestone
setblock ~1 ~1 ~6 minecraft:andesite
fill ~1 ~1 ~14 ~2 ~1 ~14 minecraft:cobblestone
fill ~2 ~1 ~6 ~5 ~1 ~6 minecraft:cobblestone
setblock ~3 ~1 ~14 minecraft:andesite
setblock ~4 ~1 ~14 minecraft:cobbled_deepslate
fill ~5 ~1 ~14 ~6 ~1 ~14 minecraft:cobblestone
setblock ~7 ~1 ~14 minecraft:stone
setblock ~8 ~1 ~14 minecraft:cobblestone
fill ~9 ~1 ~14 ~10 ~1 ~14 minecraft:stone
fill ~11 ~1 ~6 ~12 ~1 ~6 minecraft:cobblestone
setblock ~11 ~1 ~14 minecraft:cobblestone
setblock ~12 ~1 ~14 minecraft:cobbled_deepslate
setblock ~13 ~1 ~6 minecraft:stone
setblock ~13 ~1 ~14 minecraft:cobblestone
setblock ~14 ~1 ~6 minecraft:cobblestone
setblock ~14 ~1 ~14 minecraft:stone
setblock ~15 ~1 ~6 minecraft:stone
setblock ~15 ~1 ~14 minecraft:cobblestone
setblock ~16 ~1 ~7 minecraft:andesite
setblock ~16 ~1 ~13 minecraft:andesite
setblock ~0 ~2 ~7 minecraft:deepslate_bricks
fill ~0 ~2 ~8 ~0 ~2 ~10 minecraft:stone_bricks
setblock ~0 ~2 ~11 minecraft:cracked_stone_bricks
fill ~0 ~2 ~12 ~0 ~2 ~13 minecraft:stone_bricks
fill ~1 ~2 ~6 ~3 ~2 ~6 minecraft:stone_bricks
fill ~1 ~2 ~14 ~2 ~2 ~14 minecraft:stone_bricks
setblock ~3 ~2 ~14 minecraft:cracked_stone_bricks
setblock ~4 ~2 ~6 minecraft:deepslate_bricks
setblock ~4 ~2 ~14 minecraft:stone_bricks
setblock ~5 ~2 ~6 minecraft:stone_bricks
setblock ~5 ~2 ~14 minecraft:cracked_stone_bricks
fill ~6 ~2 ~14 ~11 ~2 ~14 minecraft:stone_bricks
fill ~11 ~2 ~6 ~15 ~2 ~6 minecraft:stone_bricks
setblock ~12 ~2 ~14 minecraft:andesite
fill ~13 ~2 ~14 ~15 ~2 ~14 minecraft:stone_bricks
setblock ~16 ~2 ~7 minecraft:stone_bricks
setblock ~16 ~2 ~13 minecraft:stone_bricks

# the timber above the plinth: dark oak posts at the corners and every fourth block, spruce boards between, a stripped spruce log plate at y 5
fill ~0 ~3 ~6 ~0 ~4 ~6 minecraft:dark_oak_log[axis=y]
fill ~0 ~3 ~7 ~0 ~4 ~7 minecraft:spruce_planks
fill ~0 ~3 ~8 ~0 ~4 ~8 minecraft:dark_oak_log[axis=y]
fill ~0 ~3 ~9 ~0 ~3 ~11 minecraft:spruce_planks
fill ~0 ~3 ~12 ~0 ~4 ~12 minecraft:dark_oak_log[axis=y]
fill ~0 ~3 ~13 ~0 ~4 ~13 minecraft:spruce_planks
fill ~0 ~3 ~14 ~0 ~4 ~14 minecraft:dark_oak_log[axis=y]
fill ~1 ~3 ~14 ~3 ~4 ~14 minecraft:spruce_planks
fill ~4 ~3 ~6 ~4 ~4 ~6 minecraft:dark_oak_log[axis=y]
fill ~4 ~3 ~14 ~4 ~4 ~14 minecraft:dark_oak_log[axis=y]
fill ~5 ~3 ~6 ~5 ~4 ~6 minecraft:spruce_planks
fill ~8 ~3 ~14 ~8 ~4 ~14 minecraft:dark_oak_log[axis=y]
fill ~9 ~3 ~14 ~11 ~3 ~14 minecraft:spruce_planks
fill ~11 ~3 ~6 ~11 ~4 ~6 minecraft:spruce_planks
fill ~12 ~3 ~6 ~12 ~4 ~6 minecraft:dark_oak_log[axis=y]
fill ~12 ~3 ~14 ~12 ~4 ~14 minecraft:dark_oak_log[axis=y]
fill ~16 ~3 ~6 ~16 ~4 ~6 minecraft:dark_oak_log[axis=y]
fill ~16 ~3 ~7 ~16 ~4 ~7 minecraft:spruce_planks
setblock ~16 ~3 ~8 minecraft:dark_oak_log[axis=y]
setblock ~16 ~3 ~12 minecraft:dark_oak_log[axis=y]
fill ~16 ~3 ~13 ~16 ~4 ~13 minecraft:spruce_planks
fill ~16 ~3 ~14 ~16 ~4 ~14 minecraft:dark_oak_log[axis=y]
setblock ~9 ~4 ~14 minecraft:spruce_planks
setblock ~10 ~4 ~14 minecraft:dark_oak_planks
setblock ~11 ~4 ~14 minecraft:spruce_planks
fill ~0 ~5 ~6 ~16 ~5 ~6 minecraft:stripped_spruce_log[axis=x]
fill ~0 ~5 ~7 ~0 ~5 ~13 minecraft:stripped_spruce_log[axis=z]
fill ~0 ~5 ~14 ~16 ~5 ~14 minecraft:stripped_spruce_log[axis=x]
fill ~16 ~5 ~7 ~16 ~5 ~13 minecraft:stripped_spruce_log[axis=z]

# the posts stand on the plinth: dark oak from y 1 at the corners and the door posts
fill ~0 ~1 ~6 ~0 ~2 ~6 minecraft:dark_oak_log[axis=y]
fill ~0 ~1 ~14 ~0 ~2 ~14 minecraft:dark_oak_log[axis=y]
fill ~16 ~1 ~6 ~16 ~2 ~6 minecraft:dark_oak_log[axis=y]
fill ~16 ~1 ~8 ~16 ~2 ~8 minecraft:dark_oak_log[axis=y]
fill ~16 ~1 ~12 ~16 ~2 ~12 minecraft:dark_oak_log[axis=y]
fill ~16 ~1 ~14 ~16 ~2 ~14 minecraft:dark_oak_log[axis=y]

# windows: panes in the bays (north bays 0 and 3, south bays 1 and 3), the plinth is the sill and the plate the lintel; the west gable has a lantern window
fill ~1 ~3 ~6 ~3 ~4 ~6 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~5 ~3 ~14 ~7 ~4 ~14 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~13 ~3 ~6 ~15 ~4 ~6 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~13 ~3 ~14 ~15 ~4 ~14 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~0 ~4 ~9 ~0 ~4 ~11 minecraft:glass_pane[north=true,east=true,south=true,west=false]

# the east door: x 16, z 9..11, y 1..3 open, a dark oak lintel at y 4 across z 8..12
fill ~16 ~1 ~9 ~16 ~3 ~11 minecraft:air
fill ~16 ~4 ~8 ~16 ~4 ~12 minecraft:dark_oak_log[axis=z]

# the door from the hall into the wing: x 7..9, z 6, y 1..3 open, a lintel at y 4 and dark oak posts at x 6 and 10
fill ~6 ~1 ~6 ~6 ~3 ~6 minecraft:dark_oak_log[axis=y]
fill ~7 ~1 ~6 ~9 ~3 ~6 minecraft:air
fill ~10 ~1 ~6 ~10 ~3 ~6 minecraft:dark_oak_log[axis=y]
fill ~6 ~4 ~6 ~10 ~4 ~6 minecraft:dark_oak_log[axis=x]

# the wing: stone plinth y 1..2, spruce boards in a dark oak frame y 3..5 on the west, east and north sides, the north gable door (x 7..9, z 0) and a window on each side wall
fill ~5 ~1 ~0 ~6 ~3 ~0 minecraft:dark_oak_log[axis=y]
fill ~5 ~1 ~1 ~5 ~1 ~3 minecraft:cobblestone
setblock ~5 ~1 ~4 minecraft:andesite
setblock ~5 ~1 ~5 minecraft:stone
fill ~7 ~1 ~0 ~9 ~3 ~0 minecraft:air
fill ~10 ~1 ~0 ~11 ~3 ~0 minecraft:dark_oak_log[axis=y]
fill ~11 ~1 ~1 ~11 ~1 ~3 minecraft:cobblestone
setblock ~11 ~1 ~4 minecraft:andesite
setblock ~11 ~1 ~5 minecraft:cobblestone
fill ~5 ~2 ~1 ~5 ~2 ~3 minecraft:stone_bricks
setblock ~5 ~2 ~4 minecraft:cracked_stone_bricks
setblock ~5 ~2 ~5 minecraft:stone_bricks
fill ~11 ~2 ~1 ~11 ~2 ~5 minecraft:stone_bricks
fill ~5 ~3 ~1 ~5 ~4 ~1 minecraft:spruce_planks
fill ~5 ~3 ~2 ~5 ~4 ~2 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~5 ~3 ~3 ~5 ~4 ~3 minecraft:dark_oak_log[axis=y]
fill ~5 ~3 ~4 ~5 ~4 ~4 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~5 ~3 ~5 ~5 ~4 ~5 minecraft:spruce_planks
fill ~11 ~3 ~1 ~11 ~4 ~1 minecraft:spruce_planks
fill ~11 ~3 ~2 ~11 ~4 ~2 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~11 ~3 ~3 ~11 ~4 ~3 minecraft:dark_oak_log[axis=y]
fill ~11 ~3 ~4 ~11 ~4 ~4 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~11 ~3 ~5 ~11 ~4 ~5 minecraft:spruce_planks
setblock ~5 ~4 ~0 minecraft:dark_oak_log[axis=y]
fill ~6 ~4 ~0 ~10 ~4 ~0 minecraft:dark_oak_log[axis=x]
setblock ~11 ~4 ~0 minecraft:dark_oak_log[axis=y]
fill ~5 ~5 ~0 ~11 ~5 ~0 minecraft:stripped_spruce_log[axis=x]
fill ~5 ~5 ~1 ~5 ~5 ~5 minecraft:stripped_spruce_log[axis=z]
fill ~11 ~5 ~1 ~11 ~5 ~5 minecraft:stripped_spruce_log[axis=z]

# the roofs: dark oak stairs at the eaves (three rows), snow blocks above, a dark oak verge round the snowy field, the ridge snow; the cross gable over the wing meets the hall roof in a valley, the porch roof leans on the east gable
fill ~17 ~4 ~7 ~19 ~4 ~7 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~17 ~4 ~13 ~19 ~4 ~13 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~0 ~5 ~5 ~3 ~5 ~5 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~5 ~15 ~16 ~5 ~15 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~4 ~5 ~0 ~4 ~5 ~5 minecraft:dark_oak_stairs[facing=east,half=bottom]
fill ~12 ~5 ~0 ~12 ~5 ~5 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~13 ~5 ~5 ~16 ~5 ~5 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~17 ~5 ~8 ~19 ~5 ~8 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~17 ~5 ~12 ~19 ~5 ~12 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~0 ~6 ~6 ~4 ~6 ~6 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~6 ~14 ~16 ~6 ~14 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~5 ~6 ~0 ~5 ~6 ~6 minecraft:dark_oak_stairs[facing=east,half=bottom]
fill ~11 ~6 ~0 ~11 ~6 ~6 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~12 ~6 ~6 ~16 ~6 ~6 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~17 ~6 ~9 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~17 ~6 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~18 ~6 ~9 minecraft:snow_block
setblock ~18 ~6 ~11 minecraft:snow_block
setblock ~19 ~6 ~9 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~19 ~6 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~0 ~7 ~7 ~5 ~7 ~7 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~7 ~13 ~16 ~7 ~13 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~6 ~7 ~0 ~6 ~7 ~7 minecraft:dark_oak_stairs[facing=east,half=bottom]
fill ~10 ~7 ~0 ~10 ~7 ~7 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~11 ~7 ~7 ~16 ~7 ~7 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~17 ~7 ~10 ~19 ~7 ~10 minecraft:snow_block
setblock ~0 ~8 ~8 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~0 ~8 ~12 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~1 ~8 ~8 ~6 ~8 ~8 minecraft:snow_block
fill ~1 ~8 ~12 ~15 ~8 ~12 minecraft:snow_block
setblock ~7 ~8 ~0 minecraft:dark_oak_stairs[facing=east,half=bottom]
fill ~7 ~8 ~1 ~7 ~8 ~8 minecraft:snow_block
setblock ~9 ~8 ~0 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~9 ~8 ~1 ~9 ~8 ~8 minecraft:snow_block
fill ~10 ~8 ~8 ~15 ~8 ~8 minecraft:snow_block
setblock ~16 ~8 ~8 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~16 ~8 ~12 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~0 ~9 ~9 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~0 ~9 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~4 ~9 ~9 ~7 ~9 ~9 minecraft:snow_block
fill ~4 ~9 ~11 ~15 ~9 ~11 minecraft:snow_block
fill ~8 ~9 ~0 ~8 ~9 ~9 minecraft:snow_block
fill ~9 ~9 ~9 ~15 ~9 ~9 minecraft:snow_block
setblock ~16 ~9 ~9 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~16 ~9 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~0 ~10 ~10 minecraft:snow_block
fill ~4 ~10 ~10 ~16 ~10 ~10 minecraft:snow_block

# the gable walls: the west and east gables of the hall (a king post at z 10 on the west gable) and the north gable of the wing, boards in a dark oak frame
fill ~0 ~6 ~7 ~0 ~6 ~9 minecraft:spruce_planks
fill ~0 ~6 ~10 ~0 ~9 ~10 minecraft:dark_oak_log[axis=y]
fill ~0 ~6 ~11 ~0 ~6 ~13 minecraft:spruce_planks
fill ~6 ~6 ~0 ~7 ~6 ~0 minecraft:spruce_planks
fill ~8 ~6 ~0 ~8 ~8 ~0 minecraft:dark_oak_log[axis=y]
fill ~9 ~6 ~0 ~10 ~6 ~0 minecraft:spruce_planks
fill ~16 ~6 ~7 ~16 ~6 ~13 minecraft:spruce_planks
fill ~0 ~7 ~8 ~0 ~7 ~9 minecraft:spruce_planks
fill ~0 ~7 ~11 ~0 ~7 ~12 minecraft:spruce_planks
setblock ~7 ~7 ~0 minecraft:spruce_planks
setblock ~9 ~7 ~0 minecraft:spruce_planks
fill ~16 ~7 ~8 ~16 ~7 ~12 minecraft:spruce_planks
setblock ~0 ~8 ~9 minecraft:spruce_planks
setblock ~0 ~8 ~11 minecraft:spruce_planks

# the east loft window (z 9..11, y 8, and z 10, y 9) above the porch roof; the porch posts at z 7 and 13 and the tie beam at y 4
fill ~19 ~1 ~7 ~19 ~3 ~7 minecraft:dark_oak_log[axis=y]
fill ~19 ~1 ~13 ~19 ~3 ~13 minecraft:dark_oak_log[axis=y]
setblock ~19 ~3 ~10 minecraft:lantern[hanging=true]
fill ~19 ~4 ~8 ~19 ~4 ~12 minecraft:dark_oak_log[axis=z]
setblock ~16 ~8 ~9 minecraft:glass_pane[north=false,east=false,south=true,west=false]
setblock ~16 ~8 ~10 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~16 ~8 ~11 minecraft:glass_pane[north=true,east=false,south=false,west=false]
setblock ~16 ~9 ~10 minecraft:glass_pane[north=false,east=false,south=false,west=false]

# the chimney on the west gable: a stone stack x 1..3, z 9..11 from the floor through the roof to y 12, a fire cavity facing east (x 2..3, z 9..11, y 1..2), smoke from a campfire on the cap
fill ~1 ~1 ~9 ~1 ~2 ~10 minecraft:stone_bricks
setblock ~1 ~1 ~11 minecraft:cracked_stone_bricks
fill ~2 ~1 ~9 ~3 ~2 ~9 minecraft:air
setblock ~2 ~1 ~10 minecraft:campfire[lit=true,signal_fire=false,facing=east]
fill ~2 ~1 ~11 ~2 ~2 ~11 minecraft:air
fill ~3 ~1 ~10 ~3 ~2 ~11 minecraft:air
setblock ~1 ~2 ~11 minecraft:stone_bricks
setblock ~2 ~2 ~10 minecraft:air
setblock ~1 ~3 ~9 minecraft:deepslate_bricks
setblock ~1 ~3 ~10 minecraft:stone_bricks
setblock ~1 ~3 ~11 minecraft:andesite
fill ~2 ~3 ~9 ~2 ~3 ~11 minecraft:stone_bricks
fill ~3 ~3 ~9 ~3 ~3 ~11 minecraft:chiseled_stone_bricks
setblock ~1 ~4 ~9 minecraft:stone_bricks
setblock ~1 ~4 ~10 minecraft:cracked_stone_bricks
fill ~1 ~4 ~11 ~1 ~9 ~11 minecraft:stone_bricks
fill ~2 ~4 ~9 ~3 ~4 ~9 minecraft:cracked_stone_bricks
fill ~2 ~4 ~10 ~3 ~5 ~11 minecraft:stone_bricks
setblock ~1 ~5 ~9 minecraft:andesite
fill ~1 ~5 ~10 ~1 ~8 ~10 minecraft:stone_bricks
fill ~2 ~5 ~9 ~3 ~9 ~9 minecraft:stone_bricks
fill ~1 ~6 ~9 ~1 ~10 ~9 minecraft:stone_bricks
setblock ~2 ~6 ~10 minecraft:cracked_stone_bricks
setblock ~2 ~6 ~11 minecraft:stone_bricks
setblock ~3 ~6 ~10 minecraft:stone_bricks
setblock ~3 ~6 ~11 minecraft:cracked_stone_bricks
setblock ~2 ~7 ~10 minecraft:stone_bricks
setblock ~2 ~7 ~11 minecraft:cracked_stone_bricks
setblock ~3 ~7 ~10 minecraft:deepslate_bricks
setblock ~3 ~7 ~11 minecraft:andesite
setblock ~2 ~8 ~10 minecraft:andesite
fill ~2 ~8 ~11 ~3 ~8 ~11 minecraft:stone_bricks
setblock ~3 ~8 ~10 minecraft:cracked_stone_bricks
fill ~1 ~9 ~10 ~2 ~9 ~10 minecraft:cracked_stone_bricks
fill ~2 ~9 ~11 ~2 ~12 ~11 minecraft:stone_bricks
fill ~3 ~9 ~10 ~3 ~10 ~10 minecraft:stone_bricks
fill ~3 ~9 ~11 ~3 ~10 ~11 minecraft:cracked_stone_bricks
fill ~1 ~10 ~10 ~1 ~12 ~10 minecraft:stone_bricks
fill ~1 ~10 ~11 ~1 ~11 ~11 minecraft:cracked_stone_bricks
fill ~2 ~10 ~9 ~2 ~12 ~10 minecraft:stone_bricks
setblock ~3 ~10 ~9 minecraft:cracked_stone_bricks
setblock ~1 ~11 ~9 minecraft:cracked_stone_bricks
fill ~3 ~11 ~9 ~3 ~12 ~9 minecraft:stone_bricks
setblock ~3 ~11 ~10 minecraft:cracked_stone_bricks
fill ~3 ~11 ~11 ~3 ~12 ~11 minecraft:stone_bricks
setblock ~1 ~12 ~9 minecraft:stone_bricks
setblock ~1 ~12 ~11 minecraft:stone_bricks
setblock ~3 ~12 ~10 minecraft:stone_bricks
setblock ~2 ~13 ~10 minecraft:campfire[lit=true,signal_fire=false,facing=east]

# tie beams across the hall at x 4, 8 and 12 (y 5), lanterns hung from them
setblock ~4 ~3 ~8 minecraft:lantern[hanging=true]
setblock ~4 ~3 ~12 minecraft:lantern[hanging=true]
setblock ~8 ~3 ~10 minecraft:lantern[hanging=true]
setblock ~12 ~3 ~8 minecraft:lantern[hanging=true]
setblock ~12 ~3 ~12 minecraft:lantern[hanging=true]
setblock ~4 ~4 ~8 minecraft:chain[axis=y]
setblock ~4 ~4 ~12 minecraft:chain[axis=y]
setblock ~8 ~4 ~10 minecraft:chain[axis=y]
setblock ~12 ~4 ~8 minecraft:chain[axis=y]
setblock ~12 ~4 ~12 minecraft:chain[axis=y]
fill ~4 ~5 ~7 ~4 ~5 ~13 minecraft:dark_oak_log[axis=z]
fill ~8 ~5 ~7 ~8 ~5 ~13 minecraft:dark_oak_log[axis=z]
fill ~12 ~5 ~7 ~12 ~5 ~13 minecraft:dark_oak_log[axis=z]

# two long tables (z 8 and z 12, x 5..11) with benches either side, a rug down the aisle, barrels and bookshelves along the south wall, chests by the hearth
setblock ~4 ~1 ~7 minecraft:chest[facing=south]
setblock ~4 ~1 ~13 minecraft:chest[facing=north]
fill ~5 ~1 ~7 ~11 ~1 ~7 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~5 ~1 ~8 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
fill ~5 ~1 ~9 ~11 ~1 ~9 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~5 ~1 ~10 ~11 ~1 ~10 minecraft:red_carpet
fill ~5 ~1 ~11 ~11 ~1 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~5 ~1 ~12 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
fill ~5 ~1 ~13 ~11 ~1 ~13 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~6 ~1 ~8 ~10 ~1 ~8 minecraft:spruce_fence[north=false,east=true,south=false,west=true]
fill ~6 ~1 ~12 ~10 ~1 ~12 minecraft:spruce_fence[north=false,east=true,south=false,west=true]
setblock ~11 ~1 ~8 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
setblock ~11 ~1 ~12 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
fill ~13 ~1 ~7 ~15 ~2 ~7 minecraft:bookshelf
fill ~14 ~1 ~12 ~14 ~1 ~13 minecraft:barrel[facing=up]
fill ~15 ~1 ~13 ~15 ~2 ~13 minecraft:barrel[facing=up]
fill ~5 ~2 ~8 ~11 ~2 ~8 minecraft:spruce_pressure_plate
fill ~5 ~2 ~12 ~11 ~2 ~12 minecraft:spruce_pressure_plate

# inside the wing: barrels, sacks of hay, a bench by the hall door and a lantern on a chain
fill ~6 ~1 ~1 ~7 ~1 ~1 minecraft:barrel[facing=up]
fill ~6 ~1 ~3 ~6 ~1 ~4 minecraft:spruce_stairs[facing=west,half=bottom]
fill ~10 ~1 ~1 ~10 ~1 ~2 minecraft:hay_block[axis=y]
setblock ~10 ~2 ~1 minecraft:hay_block[axis=y]

# the yard wall: two courses of weathered cobblestone; north z 0, east x 26 (z 0..10), south z 18, the return walls at x 16 (z 0..5 and z 15..18)
fill ~16 ~1 ~1 ~16 ~1 ~2 minecraft:cobblestone
setblock ~16 ~1 ~3 minecraft:andesite
fill ~16 ~1 ~4 ~16 ~1 ~5 minecraft:cobblestone
setblock ~16 ~1 ~15 minecraft:cobblestone
setblock ~16 ~1 ~16 minecraft:andesite
setblock ~16 ~1 ~17 minecraft:stone
setblock ~17 ~1 ~0 minecraft:stone
fill ~17 ~1 ~18 ~19 ~1 ~18 minecraft:cobblestone
setblock ~18 ~1 ~0 minecraft:cobbled_deepslate
fill ~19 ~1 ~0 ~19 ~2 ~0 minecraft:cobblestone
setblock ~20 ~1 ~0 minecraft:cobbled_deepslate
fill ~20 ~1 ~18 ~20 ~2 ~18 minecraft:andesite
fill ~22 ~1 ~0 ~23 ~2 ~0 minecraft:cobblestone
setblock ~22 ~1 ~18 minecraft:andesite
fill ~23 ~1 ~18 ~25 ~1 ~18 minecraft:cobblestone
fill ~24 ~1 ~0 ~24 ~2 ~0 minecraft:andesite
setblock ~25 ~1 ~0 minecraft:cobblestone
fill ~26 ~1 ~1 ~26 ~1 ~8 minecraft:cobblestone
setblock ~26 ~1 ~9 minecraft:cobbled_deepslate
setblock ~16 ~2 ~1 minecraft:cobblestone
setblock ~16 ~2 ~2 minecraft:andesite
fill ~16 ~2 ~3 ~16 ~2 ~4 minecraft:cobblestone
setblock ~16 ~2 ~5 minecraft:andesite
setblock ~16 ~2 ~15 minecraft:stone
setblock ~16 ~2 ~16 minecraft:cobblestone
setblock ~16 ~2 ~17 minecraft:cobbled_deepslate
setblock ~17 ~2 ~0 minecraft:cobblestone
setblock ~17 ~2 ~18 minecraft:andesite
setblock ~18 ~2 ~0 minecraft:stone
fill ~18 ~2 ~18 ~19 ~2 ~18 minecraft:cobblestone
setblock ~20 ~2 ~0 minecraft:cobblestone
fill ~22 ~2 ~18 ~24 ~2 ~18 minecraft:cobblestone
setblock ~25 ~2 ~0 minecraft:andesite
setblock ~25 ~2 ~18 minecraft:andesite
setblock ~26 ~2 ~1 minecraft:stone
setblock ~26 ~2 ~2 minecraft:andesite
setblock ~26 ~2 ~3 minecraft:cobblestone
setblock ~26 ~2 ~4 minecraft:andesite
setblock ~26 ~2 ~5 minecraft:cobblestone
setblock ~26 ~2 ~6 minecraft:stone
fill ~26 ~2 ~7 ~26 ~2 ~9 minecraft:cobblestone

# piers of stone brick, three high, with a lantern on each: the corners, the gate tower flanks and the middle of the long walls
setblock ~16 ~1 ~0 minecraft:cracked_stone_bricks
fill ~16 ~1 ~18 ~16 ~3 ~18 minecraft:stone_bricks
setblock ~21 ~1 ~0 minecraft:stone_bricks
fill ~21 ~1 ~18 ~21 ~3 ~18 minecraft:stone_bricks
setblock ~26 ~1 ~0 minecraft:deepslate_bricks
fill ~26 ~1 ~10 ~26 ~3 ~10 minecraft:stone_bricks
setblock ~26 ~1 ~18 minecraft:deepslate_bricks
fill ~16 ~2 ~0 ~16 ~3 ~0 minecraft:stone_bricks
fill ~21 ~2 ~0 ~21 ~3 ~0 minecraft:cracked_stone_bricks
fill ~26 ~2 ~0 ~26 ~3 ~0 minecraft:stone_bricks
fill ~26 ~2 ~18 ~26 ~3 ~18 minecraft:stone_bricks
setblock ~16 ~4 ~0 minecraft:lantern[hanging=false]
setblock ~16 ~4 ~18 minecraft:lantern[hanging=false]
setblock ~21 ~4 ~0 minecraft:lantern[hanging=false]
setblock ~21 ~4 ~18 minecraft:lantern[hanging=false]
setblock ~26 ~4 ~0 minecraft:lantern[hanging=false]
setblock ~26 ~4 ~10 minecraft:lantern[hanging=false]
setblock ~26 ~4 ~18 minecraft:lantern[hanging=false]

# the gatehouse: the ground storey in stone along z 11 and z 17 (x 23..26, y 1..5), dark oak corner posts to y 9, the passage between them open y 1..5 across x 23..26, z 12..16
fill ~23 ~1 ~11 ~23 ~9 ~11 minecraft:dark_oak_log[axis=y]
fill ~23 ~1 ~17 ~23 ~9 ~17 minecraft:dark_oak_log[axis=y]
fill ~24 ~1 ~11 ~25 ~1 ~11 minecraft:andesite
setblock ~24 ~1 ~17 minecraft:stone
setblock ~25 ~1 ~17 minecraft:andesite
fill ~26 ~1 ~11 ~26 ~9 ~11 minecraft:dark_oak_log[axis=y]
fill ~26 ~1 ~17 ~26 ~9 ~17 minecraft:dark_oak_log[axis=y]
fill ~24 ~2 ~11 ~25 ~2 ~11 minecraft:stone_bricks
fill ~24 ~2 ~17 ~25 ~5 ~17 minecraft:stone_bricks
setblock ~24 ~3 ~11 minecraft:cracked_stone_bricks
fill ~25 ~3 ~11 ~25 ~4 ~11 minecraft:stone_bricks
setblock ~24 ~4 ~11 minecraft:stone_bricks
setblock ~24 ~5 ~11 minecraft:andesite
setblock ~25 ~5 ~11 minecraft:cracked_stone_bricks

# the floor of the room at y 6: logs across the faces (x 23 and 26), planks between, a string course of stripped spruce logs along the stone walls
fill ~23 ~6 ~12 ~23 ~6 ~16 minecraft:dark_oak_log[axis=z]
fill ~24 ~6 ~11 ~25 ~6 ~11 minecraft:stripped_spruce_log[axis=x]
fill ~24 ~6 ~12 ~25 ~6 ~16 minecraft:spruce_planks
fill ~24 ~6 ~17 ~25 ~6 ~17 minecraft:stripped_spruce_log[axis=x]
fill ~26 ~6 ~12 ~26 ~6 ~16 minecraft:dark_oak_log[axis=z]

# corbel stairs under the floor at the mouths of the passage (y 5, upside down) so the gate reads as an arch
setblock ~23 ~5 ~12 minecraft:dark_oak_stairs[facing=south,half=top]
setblock ~23 ~5 ~16 minecraft:dark_oak_stairs[facing=north,half=top]
setblock ~26 ~5 ~12 minecraft:dark_oak_stairs[facing=south,half=top]
setblock ~26 ~5 ~16 minecraft:dark_oak_stairs[facing=north,half=top]

# the room walls y 7..9: boards in a dark oak frame on all four sides; the east window (x 26, y 8..9, z 14..15), the doorway (x 24, y 7..8, z 11), a window west (x 23, y 8, z 14)
fill ~23 ~7 ~12 ~23 ~8 ~12 minecraft:spruce_planks
setblock ~23 ~7 ~13 minecraft:dark_oak_planks
setblock ~23 ~7 ~14 minecraft:spruce_planks
setblock ~23 ~7 ~15 minecraft:dark_oak_planks
fill ~23 ~7 ~16 ~23 ~8 ~16 minecraft:spruce_planks
fill ~24 ~7 ~11 ~24 ~8 ~11 minecraft:air
fill ~24 ~7 ~17 ~24 ~8 ~17 minecraft:spruce_planks
fill ~25 ~7 ~11 ~25 ~8 ~11 minecraft:spruce_planks
setblock ~25 ~7 ~17 minecraft:dark_oak_planks
fill ~26 ~7 ~12 ~26 ~8 ~13 minecraft:spruce_planks
fill ~26 ~7 ~14 ~26 ~7 ~15 minecraft:dark_oak_planks
fill ~26 ~7 ~16 ~26 ~8 ~16 minecraft:spruce_planks
setblock ~23 ~8 ~13 minecraft:spruce_planks
setblock ~23 ~8 ~14 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~23 ~8 ~15 minecraft:spruce_planks
setblock ~25 ~8 ~17 minecraft:spruce_planks
fill ~26 ~8 ~14 ~26 ~9 ~15 minecraft:air
fill ~23 ~9 ~12 ~23 ~9 ~16 minecraft:stripped_spruce_log[axis=z]
fill ~24 ~9 ~11 ~25 ~9 ~11 minecraft:stripped_spruce_log[axis=x]
fill ~24 ~9 ~17 ~25 ~9 ~17 minecraft:stripped_spruce_log[axis=x]
fill ~26 ~9 ~12 ~26 ~9 ~13 minecraft:stripped_spruce_log[axis=z]
setblock ~26 ~9 ~16 minecraft:stripped_spruce_log[axis=z]

# the gatehouse roof: dark oak stairs on the eaves (z 10, 11, 17, 18), snow above, the ridge along x at z 14; the gable walls at x 23 and 26
fill ~23 ~9 ~10 ~26 ~9 ~10 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~23 ~9 ~18 ~26 ~9 ~18 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~23 ~10 ~11 ~26 ~10 ~11 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~23 ~10 ~12 minecraft:spruce_planks
setblock ~23 ~10 ~13 minecraft:dark_oak_planks
fill ~23 ~10 ~14 ~23 ~12 ~14 minecraft:dark_oak_log[axis=y]
fill ~23 ~10 ~15 ~23 ~10 ~16 minecraft:spruce_planks
fill ~23 ~10 ~17 ~26 ~10 ~17 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~26 ~10 ~12 ~26 ~10 ~13 minecraft:spruce_planks
fill ~26 ~10 ~14 ~26 ~12 ~14 minecraft:dark_oak_log[axis=y]
fill ~26 ~10 ~15 ~26 ~10 ~16 minecraft:spruce_planks
setblock ~23 ~11 ~12 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~23 ~11 ~13 minecraft:spruce_planks
setblock ~23 ~11 ~15 minecraft:spruce_planks
setblock ~23 ~11 ~16 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~24 ~11 ~12 ~25 ~11 ~12 minecraft:snow_block
fill ~24 ~11 ~16 ~25 ~11 ~16 minecraft:snow_block
setblock ~26 ~11 ~12 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~26 ~11 ~13 minecraft:spruce_planks
setblock ~26 ~11 ~15 minecraft:spruce_planks
setblock ~26 ~11 ~16 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~23 ~12 ~13 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~23 ~12 ~15 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~24 ~12 ~13 ~25 ~12 ~13 minecraft:snow_block
fill ~24 ~12 ~15 ~25 ~12 ~15 minecraft:snow_block
setblock ~26 ~12 ~13 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~26 ~12 ~15 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~23 ~13 ~14 ~26 ~13 ~14 minecraft:snow_block

# inside the room: a lantern on a chain from the ridge, barrels against the south wall and a bench by the door
fill ~24 ~7 ~16 ~25 ~7 ~16 minecraft:barrel[facing=up]
setblock ~25 ~7 ~12 minecraft:spruce_stairs[facing=west,half=bottom]
setblock ~24 ~11 ~14 minecraft:lantern[hanging=true]
setblock ~24 ~12 ~14 minecraft:chain[axis=y]

# the ladder up the north face of the gatehouse: (24, 1..6, 10) on the wall z 11, a pier with a lantern beside the doorway
fill ~24 ~1 ~10 ~24 ~6 ~10 minecraft:ladder[facing=north]
setblock ~25 ~6 ~10 minecraft:stone_bricks
setblock ~25 ~7 ~10 minecraft:lantern[hanging=false]

# the passage: two lanterns hung under the floor beams
setblock ~24 ~5 ~13 minecraft:lantern[hanging=true]
setblock ~24 ~5 ~15 minecraft:lantern[hanging=true]

# the well: a cobble ring (x 22..24, z 2..4, y 1..2) round a water source, four dark oak fence posts, a plank roof at y 5 and a lantern on a chain; it stands clear of the cover line from the north-west corner of the yard to the east wall
fill ~22 ~1 ~2 ~24 ~1 ~2 minecraft:cobblestone
setblock ~22 ~1 ~3 minecraft:cobbled_deepslate
fill ~22 ~1 ~4 ~24 ~1 ~4 minecraft:cobblestone
setblock ~23 ~1 ~3 minecraft:water
setblock ~24 ~1 ~3 minecraft:andesite
setblock ~22 ~2 ~2 minecraft:cobblestone
fill ~22 ~2 ~3 ~22 ~2 ~4 minecraft:andesite
setblock ~23 ~2 ~2 minecraft:andesite
setblock ~23 ~2 ~4 minecraft:cobblestone
setblock ~24 ~2 ~2 minecraft:stone
fill ~24 ~2 ~3 ~24 ~2 ~4 minecraft:cobblestone
fill ~22 ~3 ~2 ~22 ~4 ~2 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~22 ~3 ~4 ~22 ~4 ~4 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~23 ~3 ~3 minecraft:lantern[hanging=true]
fill ~24 ~3 ~2 ~24 ~4 ~2 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~24 ~3 ~4 ~24 ~4 ~4 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~23 ~4 ~3 minecraft:chain[axis=y]
fill ~22 ~5 ~2 ~24 ~5 ~4 minecraft:dark_oak_planks

# firewood stacked against the north wall (x 18..21, y 1..3 at z 1) and a chopping stump with a split log
fill ~18 ~1 ~1 ~21 ~3 ~1 minecraft:spruce_log[axis=x]
setblock ~19 ~1 ~3 minecraft:stripped_spruce_log[axis=y]
setblock ~20 ~1 ~3 minecraft:spruce_log[axis=x]

# the covered wagon (x 18..22, z 13..17): axles and wheels of spruce logs, a slab bed, a white and grey canvas hood over barrels and hay, the tongue pointing at the gate
fill ~18 ~1 ~13 ~18 ~1 ~17 minecraft:spruce_log[axis=z]
fill ~21 ~1 ~13 ~21 ~1 ~17 minecraft:spruce_log[axis=z]
setblock ~22 ~1 ~15 minecraft:spruce_log[axis=x]
setblock ~18 ~2 ~13 minecraft:spruce_log[axis=z]
fill ~18 ~2 ~14 ~21 ~2 ~16 minecraft:spruce_slab[type=bottom]
setblock ~18 ~2 ~17 minecraft:spruce_log[axis=z]
setblock ~21 ~2 ~13 minecraft:spruce_log[axis=z]
setblock ~21 ~2 ~17 minecraft:spruce_log[axis=z]
fill ~18 ~3 ~14 ~21 ~3 ~14 minecraft:light_gray_wool
fill ~18 ~3 ~15 ~20 ~3 ~15 minecraft:barrel[facing=up]
fill ~18 ~3 ~16 ~21 ~3 ~16 minecraft:light_gray_wool
setblock ~21 ~3 ~15 minecraft:hay_block[axis=y]
fill ~18 ~4 ~14 ~20 ~4 ~15 minecraft:white_wool
setblock ~18 ~4 ~16 minecraft:white_wool
setblock ~19 ~4 ~16 minecraft:light_gray_wool
setblock ~20 ~4 ~16 minecraft:white_wool
setblock ~21 ~4 ~14 minecraft:light_gray_wool
fill ~21 ~4 ~15 ~21 ~4 ~16 minecraft:white_wool
fill ~18 ~5 ~15 ~21 ~5 ~15 minecraft:white_wool

# fodder and barrels by the porch, a lantern post in the middle of the yard, a hitching rail near the north-east
fill ~17 ~1 ~5 ~18 ~1 ~5 minecraft:barrel[facing=up]
setblock ~17 ~1 ~6 minecraft:barrel[facing=up]
fill ~20 ~1 ~6 ~20 ~3 ~6 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~23 ~1 ~8 ~23 ~2 ~8 minecraft:hay_block[axis=y]
fill ~24 ~1 ~7 ~24 ~1 ~8 minecraft:hay_block[axis=y]
setblock ~18 ~2 ~5 minecraft:barrel[facing=up]
setblock ~20 ~4 ~6 minecraft:lantern[hanging=false]

# snow banked inside the yard along the walls and round the well, one layer, two in the corners; the trodden lane stays bare
setblock ~17 ~1 ~1 minecraft:snow[layers=2]
fill ~17 ~1 ~2 ~17 ~1 ~4 minecraft:snow[layers=1]
setblock ~17 ~1 ~14 minecraft:snow[layers=2]
setblock ~17 ~1 ~15 minecraft:snow[layers=1]
setblock ~17 ~1 ~16 minecraft:snow[layers=2]
setblock ~17 ~1 ~17 minecraft:snow[layers=1]
setblock ~19 ~1 ~17 minecraft:snow[layers=1]
setblock ~20 ~1 ~17 minecraft:snow[layers=2]
fill ~21 ~1 ~2 ~21 ~1 ~3 minecraft:snow[layers=1]
setblock ~21 ~1 ~4 minecraft:snow[layers=2]
setblock ~21 ~1 ~5 minecraft:snow[layers=1]
setblock ~22 ~1 ~1 minecraft:snow[layers=1]
setblock ~22 ~1 ~5 minecraft:snow[layers=2]
setblock ~22 ~1 ~17 minecraft:snow[layers=1]
setblock ~23 ~1 ~1 minecraft:snow[layers=2]
fill ~23 ~1 ~5 ~24 ~1 ~5 minecraft:snow[layers=1]
setblock ~24 ~1 ~1 minecraft:snow[layers=1]
setblock ~25 ~1 ~1 minecraft:snow[layers=2]
fill ~25 ~1 ~2 ~25 ~1 ~5 minecraft:snow[layers=1]
fill ~25 ~1 ~6 ~25 ~1 ~8 minecraft:snow[layers=2]
fill ~25 ~1 ~9 ~25 ~1 ~10 minecraft:snow[layers=1]

# snow settles on every sky-facing top of a full block: wall copings, piers, the woodpile, the canvas and the well roof (one layer)
setblock ~19 ~2 ~3 minecraft:snow[layers=1]
setblock ~22 ~2 ~15 minecraft:snow[layers=1]
fill ~16 ~3 ~1 ~16 ~3 ~4 minecraft:snow[layers=1]
fill ~16 ~3 ~16 ~16 ~3 ~17 minecraft:snow[layers=1]
fill ~17 ~3 ~0 ~20 ~3 ~0 minecraft:snow[layers=1]
setblock ~17 ~3 ~18 minecraft:snow[layers=1]
fill ~18 ~3 ~17 ~18 ~3 ~18 minecraft:snow[layers=1]
fill ~19 ~3 ~18 ~20 ~3 ~18 minecraft:snow[layers=1]
setblock ~21 ~3 ~13 minecraft:snow[layers=1]
setblock ~21 ~3 ~17 minecraft:snow[layers=1]
fill ~22 ~3 ~0 ~25 ~3 ~0 minecraft:snow[layers=1]
setblock ~22 ~3 ~18 minecraft:snow[layers=1]
setblock ~26 ~3 ~1 minecraft:snow[layers=1]
fill ~26 ~3 ~3 ~26 ~3 ~9 minecraft:snow[layers=1]
fill ~19 ~4 ~1 ~21 ~4 ~1 minecraft:snow[layers=1]
fill ~18 ~5 ~16 ~21 ~5 ~16 minecraft:snow[layers=1]
fill ~19 ~5 ~14 ~21 ~5 ~14 minecraft:snow[layers=1]
fill ~18 ~6 ~15 ~21 ~6 ~15 minecraft:snow[layers=1]
fill ~22 ~6 ~2 ~24 ~6 ~4 minecraft:snow[layers=1]
fill ~1 ~13 ~9 ~3 ~13 ~9 minecraft:snow[layers=1]
setblock ~1 ~13 ~10 minecraft:snow[layers=1]
setblock ~2 ~13 ~11 minecraft:snow[layers=1]
fill ~3 ~13 ~10 ~3 ~13 ~11 minecraft:snow[layers=1]
