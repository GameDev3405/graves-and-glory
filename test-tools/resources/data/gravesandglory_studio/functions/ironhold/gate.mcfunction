# size 7 17 47
# ironhold/gate: west gate of Ironhold: a gatehouse of two dark towers over an 11-wide passage with a raised portcullis in a stone arch, a rampart across the top, wall stubs either side with broken ends and a stair up the inside.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Outer face x 0 (the west). Passage z 18..28 (world z -5..5), x 0..6, clear y 1..6; portcullis bars at x 1 from y 7; arch y 7..11 over the passage; deck y 12 across it (feet 13).
# Towers z 11..17 and z 29..35: ground room y 1..5, loft floor y 6, top floor y 12 (feet 13); ladders (5, 1..13, 14) and (5, 1..13, 32) on the east wall x 6; doors (3, 1..2, 17) and (3, 1..2, 29) from the passage; parapet y 13, merlons y 14, the west side open at z 13..15 and z 31..33.
# Wall stubs z 0..10 and z 36..46: wall x 1..4 y 1..9, deck y 10 (feet 11), merlons x 0 y 11..12; a stair up the inside at x 5 (north: z 10 to z 1, south: z 36 to z 45); the far ends (z 0 and z 46) are broken.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~0 ~4 ~2 minecraft:air
fill ~0 ~1 ~4 ~0 ~8 ~6 minecraft:air
fill ~0 ~1 ~8 ~0 ~4 ~10 minecraft:air
fill ~0 ~1 ~18 ~6 ~6 ~28 minecraft:air
fill ~0 ~1 ~36 ~0 ~4 ~38 minecraft:air
fill ~0 ~1 ~40 ~0 ~4 ~42 minecraft:air
fill ~0 ~1 ~44 ~0 ~4 ~46 minecraft:air
fill ~1 ~1 ~13 ~2 ~5 ~16 minecraft:air
fill ~1 ~1 ~30 ~5 ~5 ~31 minecraft:air
fill ~1 ~1 ~32 ~4 ~3 ~33 minecraft:air
fill ~3 ~1 ~12 ~5 ~5 ~13 minecraft:air
fill ~3 ~1 ~14 ~4 ~3 ~16 minecraft:air
fill ~3 ~1 ~34 ~4 ~5 ~34 minecraft:air
fill ~5 ~1 ~15 ~5 ~5 ~16 minecraft:air
fill ~5 ~1 ~33 ~5 ~5 ~34 minecraft:air
fill ~6 ~1 ~0 ~6 ~1 ~1 minecraft:air
fill ~6 ~1 ~4 ~6 ~16 ~10 minecraft:air
fill ~6 ~1 ~36 ~6 ~16 ~44 minecraft:air
setblock ~6 ~1 ~46 minecraft:air
fill ~2 ~2 ~12 ~2 ~5 ~12 minecraft:air
fill ~2 ~2 ~34 ~2 ~5 ~34 minecraft:air
fill ~5 ~2 ~10 ~5 ~16 ~10 minecraft:air
fill ~5 ~2 ~36 ~5 ~16 ~36 minecraft:air
fill ~6 ~2 ~1 ~6 ~2 ~3 minecraft:air
setblock ~6 ~2 ~45 minecraft:air
fill ~1 ~3 ~12 ~1 ~5 ~12 minecraft:air
fill ~1 ~3 ~34 ~1 ~5 ~34 minecraft:air
fill ~4 ~3 ~0 ~6 ~16 ~0 minecraft:air
fill ~5 ~3 ~9 ~5 ~16 ~9 minecraft:air
fill ~5 ~3 ~37 ~5 ~16 ~37 minecraft:air
fill ~5 ~3 ~46 ~6 ~16 ~46 minecraft:air
fill ~6 ~3 ~2 ~6 ~16 ~3 minecraft:air
fill ~1 ~4 ~32 ~2 ~5 ~33 minecraft:air
fill ~2 ~4 ~46 ~2 ~16 ~46 minecraft:air
fill ~3 ~4 ~15 ~3 ~5 ~16 minecraft:air
fill ~3 ~4 ~33 ~3 ~5 ~33 minecraft:air
fill ~4 ~4 ~14 ~4 ~5 ~16 minecraft:air
fill ~4 ~4 ~32 ~4 ~5 ~33 minecraft:air
fill ~5 ~4 ~8 ~5 ~16 ~8 minecraft:air
fill ~5 ~4 ~38 ~5 ~16 ~38 minecraft:air
fill ~6 ~4 ~1 ~6 ~16 ~1 minecraft:air
fill ~6 ~4 ~45 ~6 ~16 ~45 minecraft:air
fill ~0 ~5 ~0 ~1 ~16 ~0 minecraft:air
fill ~0 ~5 ~1 ~0 ~8 ~1 minecraft:air
fill ~0 ~5 ~8 ~0 ~8 ~8 minecraft:air
fill ~0 ~5 ~10 ~0 ~6 ~10 minecraft:air
fill ~0 ~5 ~36 ~0 ~8 ~37 minecraft:air
fill ~0 ~5 ~40 ~0 ~8 ~41 minecraft:air
fill ~0 ~5 ~45 ~0 ~8 ~46 minecraft:air
fill ~1 ~5 ~46 ~1 ~16 ~46 minecraft:air
fill ~4 ~5 ~46 ~4 ~16 ~46 minecraft:air
fill ~5 ~5 ~7 ~5 ~16 ~7 minecraft:air
fill ~5 ~5 ~39 ~5 ~16 ~39 minecraft:air
fill ~3 ~6 ~0 ~3 ~16 ~0 minecraft:air
fill ~5 ~6 ~6 ~5 ~16 ~6 minecraft:air
fill ~5 ~6 ~40 ~5 ~16 ~40 minecraft:air
fill ~0 ~7 ~19 ~0 ~7 ~27 minecraft:air
fill ~1 ~7 ~13 ~1 ~11 ~16 minecraft:air
setblock ~1 ~7 ~20 minecraft:air
fill ~1 ~7 ~22 ~1 ~8 ~22 minecraft:air
fill ~1 ~7 ~24 ~1 ~8 ~24 minecraft:air
setblock ~1 ~7 ~26 minecraft:air
fill ~1 ~7 ~30 ~5 ~11 ~31 minecraft:air
fill ~1 ~7 ~32 ~4 ~9 ~33 minecraft:air
fill ~2 ~7 ~0 ~2 ~16 ~0 minecraft:air
fill ~2 ~7 ~12 ~5 ~11 ~13 minecraft:air
fill ~2 ~7 ~14 ~4 ~9 ~16 minecraft:air
fill ~2 ~7 ~19 ~6 ~7 ~27 minecraft:air
fill ~2 ~7 ~34 ~4 ~11 ~34 minecraft:air
fill ~3 ~7 ~46 ~3 ~16 ~46 minecraft:air
fill ~5 ~7 ~5 ~5 ~16 ~5 minecraft:air
fill ~5 ~7 ~15 ~5 ~11 ~16 minecraft:air
fill ~5 ~7 ~33 ~5 ~11 ~34 minecraft:air
fill ~5 ~7 ~41 ~5 ~16 ~41 minecraft:air
fill ~0 ~8 ~9 ~0 ~8 ~10 minecraft:air
fill ~0 ~8 ~20 ~0 ~8 ~26 minecraft:air
setblock ~0 ~8 ~38 minecraft:air
setblock ~0 ~8 ~42 minecraft:air
fill ~1 ~8 ~12 ~1 ~11 ~12 minecraft:air
fill ~1 ~8 ~34 ~1 ~11 ~34 minecraft:air
fill ~2 ~8 ~20 ~2 ~8 ~26 minecraft:air
fill ~3 ~8 ~21 ~3 ~8 ~25 minecraft:air
fill ~4 ~8 ~20 ~6 ~8 ~26 minecraft:air
fill ~5 ~8 ~4 ~5 ~16 ~4 minecraft:air
fill ~5 ~8 ~42 ~5 ~16 ~42 minecraft:air
fill ~0 ~9 ~22 ~0 ~9 ~24 minecraft:air
fill ~0 ~9 ~46 ~0 ~16 ~46 minecraft:air
fill ~2 ~9 ~22 ~6 ~9 ~24 minecraft:air
fill ~5 ~9 ~3 ~5 ~16 ~3 minecraft:air
fill ~5 ~9 ~43 ~5 ~16 ~43 minecraft:air
fill ~1 ~10 ~32 ~2 ~11 ~33 minecraft:air
fill ~2 ~10 ~14 ~2 ~11 ~16 minecraft:air
fill ~3 ~10 ~15 ~3 ~11 ~16 minecraft:air
fill ~3 ~10 ~33 ~3 ~11 ~33 minecraft:air
fill ~4 ~10 ~14 ~4 ~11 ~16 minecraft:air
fill ~4 ~10 ~32 ~4 ~11 ~33 minecraft:air
fill ~5 ~10 ~2 ~5 ~16 ~2 minecraft:air
fill ~5 ~10 ~44 ~5 ~16 ~44 minecraft:air
fill ~0 ~11 ~2 ~4 ~16 ~2 minecraft:air
fill ~0 ~11 ~4 ~2 ~16 ~4 minecraft:air
fill ~0 ~11 ~6 ~0 ~16 ~6 minecraft:air
fill ~0 ~11 ~8 ~0 ~16 ~8 minecraft:air
fill ~0 ~11 ~10 ~0 ~16 ~10 minecraft:air
fill ~0 ~11 ~36 ~2 ~16 ~36 minecraft:air
fill ~0 ~11 ~38 ~0 ~16 ~38 minecraft:air
fill ~0 ~11 ~40 ~0 ~16 ~40 minecraft:air
fill ~0 ~11 ~42 ~0 ~16 ~42 minecraft:air
fill ~0 ~11 ~44 ~0 ~16 ~44 minecraft:air
fill ~1 ~11 ~1 ~5 ~16 ~1 minecraft:air
fill ~1 ~11 ~3 ~4 ~16 ~3 minecraft:air
fill ~1 ~11 ~5 ~2 ~16 ~10 minecraft:air
fill ~1 ~11 ~37 ~2 ~16 ~45 minecraft:air
fill ~3 ~11 ~43 ~4 ~16 ~45 minecraft:air
fill ~5 ~11 ~45 ~5 ~16 ~45 minecraft:air
fill ~3 ~12 ~4 ~4 ~16 ~10 minecraft:air
fill ~3 ~12 ~36 ~4 ~16 ~42 minecraft:air
fill ~0 ~13 ~13 ~0 ~16 ~15 minecraft:air
fill ~0 ~13 ~31 ~0 ~16 ~33 minecraft:air
fill ~1 ~13 ~12 ~5 ~16 ~13 minecraft:air
fill ~1 ~13 ~14 ~4 ~16 ~21 minecraft:air
fill ~1 ~13 ~22 ~2 ~16 ~34 minecraft:air
fill ~3 ~13 ~25 ~3 ~16 ~34 minecraft:air
fill ~4 ~13 ~22 ~4 ~16 ~34 minecraft:air
fill ~5 ~13 ~15 ~5 ~16 ~31 minecraft:air
fill ~5 ~13 ~33 ~5 ~16 ~34 minecraft:air
fill ~0 ~14 ~1 ~0 ~16 ~1 minecraft:air
fill ~0 ~14 ~3 ~0 ~16 ~3 minecraft:air
fill ~0 ~14 ~5 ~0 ~16 ~5 minecraft:air
fill ~0 ~14 ~7 ~0 ~16 ~7 minecraft:air
fill ~0 ~14 ~9 ~0 ~16 ~9 minecraft:air
fill ~0 ~14 ~17 ~0 ~16 ~17 minecraft:air
fill ~0 ~14 ~19 ~0 ~16 ~19 minecraft:air
fill ~0 ~14 ~21 ~0 ~16 ~21 minecraft:air
fill ~0 ~14 ~23 ~0 ~16 ~23 minecraft:air
fill ~0 ~14 ~25 ~0 ~16 ~25 minecraft:air
fill ~0 ~14 ~27 ~0 ~16 ~27 minecraft:air
fill ~0 ~14 ~29 ~0 ~16 ~29 minecraft:air
fill ~0 ~14 ~37 ~0 ~16 ~37 minecraft:air
fill ~0 ~14 ~39 ~0 ~16 ~39 minecraft:air
fill ~0 ~14 ~41 ~0 ~16 ~41 minecraft:air
fill ~0 ~14 ~43 ~0 ~16 ~43 minecraft:air
fill ~0 ~14 ~45 ~0 ~16 ~45 minecraft:air
fill ~2 ~14 ~11 ~2 ~16 ~11 minecraft:air
fill ~2 ~14 ~35 ~2 ~16 ~35 minecraft:air
fill ~4 ~14 ~11 ~4 ~16 ~11 minecraft:air
fill ~4 ~14 ~35 ~4 ~16 ~35 minecraft:air
fill ~5 ~14 ~14 ~5 ~16 ~14 minecraft:air
fill ~5 ~14 ~32 ~5 ~16 ~32 minecraft:air
fill ~6 ~14 ~13 ~6 ~16 ~13 minecraft:air
fill ~6 ~14 ~15 ~6 ~16 ~15 minecraft:air
fill ~6 ~14 ~17 ~6 ~16 ~17 minecraft:air
fill ~6 ~14 ~19 ~6 ~16 ~19 minecraft:air
fill ~6 ~14 ~21 ~6 ~16 ~21 minecraft:air
fill ~6 ~14 ~23 ~6 ~16 ~23 minecraft:air
fill ~6 ~14 ~25 ~6 ~16 ~25 minecraft:air
fill ~6 ~14 ~27 ~6 ~16 ~27 minecraft:air
fill ~6 ~14 ~29 ~6 ~16 ~29 minecraft:air
fill ~6 ~14 ~31 ~6 ~16 ~31 minecraft:air
fill ~6 ~14 ~33 ~6 ~16 ~33 minecraft:air
fill ~0 ~15 ~12 ~0 ~16 ~12 minecraft:air
fill ~0 ~15 ~16 ~0 ~16 ~16 minecraft:air
fill ~0 ~15 ~18 ~0 ~16 ~18 minecraft:air
fill ~0 ~15 ~20 ~0 ~16 ~20 minecraft:air
fill ~0 ~15 ~22 ~0 ~16 ~22 minecraft:air
fill ~0 ~15 ~24 ~0 ~16 ~24 minecraft:air
fill ~0 ~15 ~26 ~0 ~16 ~26 minecraft:air
fill ~0 ~15 ~28 ~0 ~16 ~28 minecraft:air
fill ~0 ~15 ~30 ~0 ~16 ~30 minecraft:air
fill ~0 ~15 ~34 ~0 ~16 ~34 minecraft:air
fill ~1 ~15 ~11 ~1 ~16 ~11 minecraft:air
fill ~1 ~15 ~35 ~1 ~16 ~35 minecraft:air
fill ~3 ~15 ~11 ~3 ~16 ~11 minecraft:air
fill ~3 ~15 ~35 ~3 ~16 ~35 minecraft:air
fill ~5 ~15 ~11 ~5 ~16 ~11 minecraft:air
fill ~5 ~15 ~35 ~5 ~16 ~35 minecraft:air
fill ~6 ~15 ~12 ~6 ~16 ~12 minecraft:air
fill ~6 ~15 ~14 ~6 ~16 ~14 minecraft:air
fill ~6 ~15 ~16 ~6 ~16 ~16 minecraft:air
fill ~6 ~15 ~18 ~6 ~16 ~18 minecraft:air
fill ~6 ~15 ~20 ~6 ~16 ~20 minecraft:air
fill ~6 ~15 ~22 ~6 ~16 ~22 minecraft:air
fill ~6 ~15 ~24 ~6 ~16 ~24 minecraft:air
fill ~6 ~15 ~26 ~6 ~16 ~26 minecraft:air
fill ~6 ~15 ~28 ~6 ~16 ~28 minecraft:air
fill ~6 ~15 ~30 ~6 ~16 ~30 minecraft:air
fill ~6 ~15 ~32 ~6 ~16 ~32 minecraft:air
fill ~6 ~15 ~34 ~6 ~16 ~34 minecraft:air
fill ~3 ~16 ~22 ~3 ~16 ~24 minecraft:air

# the footing at y 0: worn cobble under the stubs (x 0..5) and the towers (x 0..6); the passage floor stays void so the road runs through
fill ~0 ~0 ~0 ~0 ~0 ~1 minecraft:cobblestone
fill ~0 ~0 ~2 ~0 ~0 ~3 minecraft:mossy_cobblestone
setblock ~0 ~0 ~4 minecraft:cobblestone
setblock ~0 ~0 ~5 minecraft:mossy_cobblestone
setblock ~0 ~0 ~6 minecraft:cobblestone
fill ~0 ~0 ~7 ~1 ~0 ~9 minecraft:mossy_cobblestone
fill ~0 ~0 ~10 ~0 ~0 ~11 minecraft:cobblestone
fill ~0 ~0 ~12 ~0 ~0 ~13 minecraft:mossy_cobblestone
setblock ~0 ~0 ~14 minecraft:cobblestone
fill ~0 ~0 ~15 ~1 ~0 ~15 minecraft:andesite
fill ~0 ~0 ~16 ~0 ~0 ~17 minecraft:cobblestone
fill ~0 ~0 ~29 ~6 ~0 ~29 minecraft:cobblestone
fill ~0 ~0 ~30 ~1 ~0 ~33 minecraft:cobblestone
setblock ~0 ~0 ~34 minecraft:mossy_cobblestone
fill ~0 ~0 ~35 ~0 ~0 ~36 minecraft:cobblestone
setblock ~0 ~0 ~37 minecraft:andesite
setblock ~0 ~0 ~38 minecraft:mossy_cobblestone
setblock ~0 ~0 ~39 minecraft:andesite
fill ~0 ~0 ~40 ~0 ~0 ~41 minecraft:cobblestone
fill ~0 ~0 ~42 ~0 ~0 ~43 minecraft:andesite
setblock ~0 ~0 ~44 minecraft:mossy_cobblestone
fill ~0 ~0 ~45 ~0 ~0 ~46 minecraft:cobblestone
fill ~1 ~0 ~0 ~1 ~0 ~4 minecraft:mossy_cobblestone
setblock ~1 ~0 ~5 minecraft:andesite
fill ~1 ~0 ~6 ~2 ~0 ~6 minecraft:mossy_cobblestone
setblock ~1 ~0 ~10 minecraft:mossy_cobblestone
fill ~1 ~0 ~11 ~1 ~0 ~14 minecraft:cobblestone
fill ~1 ~0 ~16 ~1 ~0 ~17 minecraft:mossy_cobblestone
fill ~1 ~0 ~34 ~1 ~0 ~37 minecraft:cobblestone
setblock ~1 ~0 ~38 minecraft:andesite
setblock ~1 ~0 ~39 minecraft:cobblestone
setblock ~1 ~0 ~40 minecraft:andesite
fill ~1 ~0 ~41 ~2 ~0 ~42 minecraft:cobblestone
fill ~1 ~0 ~43 ~1 ~0 ~44 minecraft:cobblestone
fill ~1 ~0 ~45 ~2 ~0 ~45 minecraft:mossy_cobblestone
setblock ~1 ~0 ~46 minecraft:andesite
fill ~2 ~0 ~0 ~5 ~0 ~0 minecraft:cobblestone
setblock ~2 ~0 ~1 minecraft:andesite
fill ~2 ~0 ~2 ~3 ~0 ~2 minecraft:mossy_cobblestone
setblock ~2 ~0 ~3 minecraft:mossy_cobblestone
fill ~2 ~0 ~4 ~2 ~0 ~5 minecraft:cobblestone
setblock ~2 ~0 ~7 minecraft:cobblestone
fill ~2 ~0 ~8 ~2 ~0 ~9 minecraft:mossy_cobblestone
setblock ~2 ~0 ~10 minecraft:cobblestone
setblock ~2 ~0 ~11 minecraft:andesite
fill ~2 ~0 ~12 ~2 ~0 ~17 minecraft:cobblestone
setblock ~2 ~0 ~30 minecraft:andesite
setblock ~2 ~0 ~31 minecraft:cobblestone
fill ~2 ~0 ~32 ~2 ~0 ~33 minecraft:mossy_cobblestone
setblock ~2 ~0 ~34 minecraft:andesite
setblock ~2 ~0 ~35 minecraft:cobblestone
fill ~2 ~0 ~36 ~2 ~0 ~37 minecraft:mossy_cobblestone
setblock ~2 ~0 ~38 minecraft:cobblestone
fill ~2 ~0 ~39 ~2 ~0 ~40 minecraft:mossy_cobblestone
setblock ~2 ~0 ~43 minecraft:andesite
setblock ~2 ~0 ~44 minecraft:cobblestone
setblock ~2 ~0 ~46 minecraft:mossy_cobblestone
setblock ~3 ~0 ~1 minecraft:cobblestone
fill ~3 ~0 ~3 ~4 ~0 ~4 minecraft:cobblestone
setblock ~3 ~0 ~5 minecraft:mossy_cobblestone
fill ~3 ~0 ~6 ~3 ~0 ~9 minecraft:cobblestone
setblock ~3 ~0 ~10 minecraft:andesite
fill ~3 ~0 ~11 ~3 ~0 ~15 minecraft:cobblestone
setblock ~3 ~0 ~16 minecraft:mossy_cobblestone
fill ~3 ~0 ~17 ~5 ~0 ~17 minecraft:cobblestone
fill ~3 ~0 ~30 ~4 ~0 ~30 minecraft:cobblestone
setblock ~3 ~0 ~31 minecraft:mossy_cobblestone
fill ~3 ~0 ~32 ~3 ~0 ~37 minecraft:cobblestone
fill ~3 ~0 ~38 ~3 ~0 ~40 minecraft:andesite
setblock ~3 ~0 ~41 minecraft:mossy_cobblestone
setblock ~3 ~0 ~42 minecraft:cobblestone
fill ~3 ~0 ~43 ~3 ~0 ~45 minecraft:mossy_cobblestone
setblock ~3 ~0 ~46 minecraft:cobblestone
fill ~4 ~0 ~1 ~4 ~0 ~2 minecraft:mossy_cobblestone
fill ~4 ~0 ~5 ~5 ~0 ~5 minecraft:cobblestone
setblock ~4 ~0 ~6 minecraft:mossy_cobblestone
fill ~4 ~0 ~7 ~4 ~0 ~8 minecraft:cobblestone
setblock ~4 ~0 ~9 minecraft:andesite
setblock ~4 ~0 ~10 minecraft:cobblestone
setblock ~4 ~0 ~11 minecraft:mossy_cobblestone
fill ~4 ~0 ~12 ~4 ~0 ~14 minecraft:cobblestone
fill ~4 ~0 ~15 ~5 ~0 ~16 minecraft:mossy_cobblestone
fill ~4 ~0 ~31 ~5 ~0 ~32 minecraft:cobblestone
setblock ~4 ~0 ~33 minecraft:andesite
fill ~4 ~0 ~34 ~4 ~0 ~35 minecraft:mossy_cobblestone
fill ~4 ~0 ~36 ~4 ~0 ~40 minecraft:cobblestone
setblock ~4 ~0 ~41 minecraft:andesite
setblock ~4 ~0 ~42 minecraft:mossy_cobblestone
fill ~4 ~0 ~43 ~4 ~0 ~45 minecraft:cobblestone
fill ~4 ~0 ~46 ~5 ~0 ~46 minecraft:andesite
fill ~5 ~0 ~1 ~5 ~0 ~3 minecraft:cobblestone
setblock ~5 ~0 ~4 minecraft:mossy_cobblestone
fill ~5 ~0 ~6 ~5 ~0 ~14 minecraft:cobblestone
setblock ~5 ~0 ~30 minecraft:mossy_cobblestone
fill ~5 ~0 ~33 ~5 ~0 ~35 minecraft:cobblestone
setblock ~5 ~0 ~36 minecraft:mossy_cobblestone
fill ~5 ~0 ~37 ~5 ~0 ~38 minecraft:cobblestone
setblock ~5 ~0 ~39 minecraft:mossy_cobblestone
setblock ~5 ~0 ~40 minecraft:cobblestone
setblock ~5 ~0 ~41 minecraft:mossy_cobblestone
setblock ~5 ~0 ~42 minecraft:cobblestone
fill ~5 ~0 ~43 ~5 ~0 ~45 minecraft:mossy_cobblestone
fill ~6 ~0 ~11 ~6 ~0 ~17 minecraft:cobblestone
setblock ~6 ~0 ~30 minecraft:cobblestone
setblock ~6 ~0 ~31 minecraft:mossy_cobblestone
fill ~6 ~0 ~32 ~6 ~0 ~34 minecraft:cobblestone
setblock ~6 ~0 ~35 minecraft:mossy_cobblestone

# the stubs: wall x 1..4, y 1..9, dark stone in patches, moss low where the damp is
fill ~1 ~1 ~0 ~1 ~1 ~2 minecraft:stone_bricks
fill ~1 ~1 ~3 ~3 ~1 ~4 minecraft:deepslate_bricks
fill ~1 ~1 ~5 ~2 ~1 ~8 minecraft:deepslate_bricks
setblock ~1 ~1 ~9 minecraft:cracked_deepslate_bricks
setblock ~1 ~1 ~10 minecraft:deepslate_bricks
fill ~1 ~1 ~36 ~4 ~1 ~44 minecraft:mossy_stone_bricks
setblock ~1 ~1 ~45 minecraft:stone_bricks
setblock ~1 ~1 ~46 minecraft:deepslate_bricks
fill ~2 ~1 ~0 ~4 ~2 ~0 minecraft:mossy_stone_bricks
setblock ~2 ~1 ~1 minecraft:cracked_stone_bricks
setblock ~2 ~1 ~2 minecraft:cracked_deepslate_bricks
fill ~2 ~1 ~9 ~4 ~1 ~9 minecraft:deepslate_bricks
setblock ~2 ~1 ~10 minecraft:cracked_deepslate_bricks
fill ~2 ~1 ~45 ~4 ~2 ~46 minecraft:mossy_stone_bricks
fill ~3 ~1 ~1 ~4 ~3 ~2 minecraft:mossy_stone_bricks
setblock ~3 ~1 ~5 minecraft:cracked_deepslate_bricks
fill ~3 ~1 ~6 ~3 ~1 ~7 minecraft:deepslate_bricks
fill ~3 ~1 ~8 ~4 ~1 ~8 minecraft:cracked_deepslate_bricks
fill ~3 ~1 ~10 ~4 ~1 ~10 minecraft:deepslate_bricks
fill ~4 ~1 ~3 ~4 ~4 ~4 minecraft:mossy_stone_bricks
fill ~4 ~1 ~5 ~4 ~1 ~7 minecraft:deepslate_bricks
fill ~1 ~2 ~0 ~1 ~3 ~5 minecraft:mossy_stone_bricks
fill ~1 ~2 ~6 ~1 ~2 ~10 minecraft:stone_bricks
setblock ~1 ~2 ~36 minecraft:cracked_stone_bricks
fill ~1 ~2 ~37 ~1 ~2 ~46 minecraft:mossy_stone_bricks
fill ~2 ~2 ~1 ~2 ~4 ~7 minecraft:mossy_stone_bricks
fill ~2 ~2 ~8 ~2 ~2 ~10 minecraft:stone_bricks
fill ~2 ~2 ~36 ~3 ~2 ~38 minecraft:stone_bricks
fill ~2 ~2 ~39 ~2 ~2 ~44 minecraft:mossy_stone_bricks
fill ~3 ~2 ~3 ~3 ~4 ~9 minecraft:mossy_stone_bricks
setblock ~3 ~2 ~10 minecraft:stone_bricks
fill ~3 ~2 ~39 ~3 ~3 ~40 minecraft:stone_bricks
fill ~3 ~2 ~41 ~3 ~2 ~44 minecraft:mossy_stone_bricks
fill ~4 ~2 ~5 ~4 ~4 ~10 minecraft:mossy_stone_bricks
setblock ~4 ~2 ~36 minecraft:mossy_stone_bricks
fill ~4 ~2 ~37 ~4 ~3 ~42 minecraft:stone_bricks
fill ~4 ~2 ~43 ~4 ~2 ~44 minecraft:mossy_stone_bricks
fill ~1 ~3 ~6 ~1 ~5 ~6 minecraft:mossy_stone_bricks
fill ~1 ~3 ~7 ~1 ~3 ~9 minecraft:stone_bricks
setblock ~1 ~3 ~10 minecraft:cracked_stone_bricks
fill ~1 ~3 ~36 ~4 ~3 ~36 minecraft:stone_bricks
fill ~1 ~3 ~37 ~2 ~3 ~37 minecraft:cracked_stone_bricks
fill ~1 ~3 ~38 ~1 ~3 ~46 minecraft:mossy_stone_bricks
fill ~2 ~3 ~0 ~3 ~3 ~0 minecraft:mossy_stone_bricks
fill ~2 ~3 ~8 ~2 ~5 ~8 minecraft:mossy_stone_bricks
setblock ~2 ~3 ~9 minecraft:cracked_stone_bricks
setblock ~2 ~3 ~10 minecraft:stone_bricks
fill ~2 ~3 ~38 ~2 ~3 ~39 minecraft:stone_bricks
fill ~2 ~3 ~40 ~2 ~3 ~45 minecraft:mossy_stone_bricks
fill ~3 ~3 ~10 ~3 ~5 ~10 minecraft:mossy_stone_bricks
fill ~3 ~3 ~37 ~3 ~3 ~38 minecraft:stone_bricks
setblock ~3 ~3 ~41 minecraft:stone_bricks
fill ~3 ~3 ~42 ~3 ~3 ~46 minecraft:mossy_stone_bricks
setblock ~4 ~3 ~43 minecraft:stone_bricks
fill ~4 ~3 ~44 ~4 ~3 ~46 minecraft:mossy_stone_bricks
fill ~1 ~4 ~1 ~1 ~5 ~5 minecraft:mossy_stone_bricks
fill ~1 ~4 ~7 ~1 ~5 ~10 minecraft:mossy_stone_bricks
fill ~1 ~4 ~36 ~4 ~5 ~36 minecraft:mossy_stone_bricks
fill ~1 ~4 ~37 ~1 ~4 ~39 minecraft:stone_bricks
fill ~1 ~4 ~40 ~1 ~4 ~42 minecraft:deepslate_bricks
fill ~1 ~4 ~43 ~1 ~4 ~44 minecraft:stone_bricks
fill ~1 ~4 ~45 ~1 ~4 ~46 minecraft:mossy_stone_bricks
setblock ~2 ~4 ~0 minecraft:mossy_stone_bricks
fill ~2 ~4 ~9 ~2 ~5 ~10 minecraft:mossy_stone_bricks
fill ~2 ~4 ~37 ~4 ~5 ~38 minecraft:mossy_stone_bricks
fill ~2 ~4 ~39 ~2 ~6 ~40 minecraft:deepslate_bricks
fill ~2 ~4 ~41 ~2 ~4 ~45 minecraft:stone_bricks
setblock ~3 ~4 ~0 minecraft:stone_bricks
fill ~3 ~4 ~1 ~3 ~4 ~2 minecraft:mossy_stone_bricks
fill ~3 ~4 ~39 ~4 ~5 ~40 minecraft:mossy_stone_bricks
fill ~3 ~4 ~41 ~3 ~4 ~42 minecraft:cracked_stone_bricks
fill ~3 ~4 ~43 ~3 ~8 ~43 minecraft:stone_bricks
fill ~3 ~4 ~44 ~3 ~5 ~44 minecraft:cracked_stone_bricks
fill ~3 ~4 ~45 ~3 ~4 ~46 minecraft:stone_bricks
fill ~4 ~4 ~1 ~4 ~9 ~2 minecraft:stone_bricks
fill ~4 ~4 ~41 ~4 ~5 ~42 minecraft:mossy_stone_bricks
setblock ~4 ~4 ~43 minecraft:cracked_stone_bricks
fill ~4 ~4 ~44 ~4 ~7 ~45 minecraft:stone_bricks
fill ~1 ~5 ~37 ~1 ~6 ~38 minecraft:stone_bricks
fill ~1 ~5 ~39 ~1 ~5 ~40 minecraft:deepslate_bricks
fill ~1 ~5 ~41 ~2 ~5 ~41 minecraft:cracked_deepslate_bricks
fill ~1 ~5 ~42 ~1 ~5 ~44 minecraft:deepslate_bricks
fill ~1 ~5 ~45 ~1 ~7 ~45 minecraft:stone_bricks
fill ~2 ~5 ~0 ~2 ~6 ~1 minecraft:stone_bricks
fill ~2 ~5 ~2 ~2 ~5 ~7 minecraft:mossy_stone_bricks
setblock ~2 ~5 ~42 minecraft:cracked_deepslate_bricks
fill ~2 ~5 ~43 ~2 ~8 ~44 minecraft:stone_bricks
fill ~2 ~5 ~45 ~2 ~6 ~45 minecraft:cracked_stone_bricks
fill ~3 ~5 ~1 ~3 ~5 ~3 minecraft:stone_bricks
fill ~3 ~5 ~4 ~3 ~5 ~9 minecraft:mossy_stone_bricks
fill ~3 ~5 ~41 ~3 ~6 ~41 minecraft:stone_bricks
setblock ~3 ~5 ~42 minecraft:cracked_stone_bricks
fill ~3 ~5 ~45 ~3 ~9 ~45 minecraft:stone_bricks
setblock ~3 ~5 ~46 minecraft:cracked_stone_bricks
fill ~4 ~5 ~3 ~4 ~6 ~5 minecraft:stone_bricks
fill ~4 ~5 ~6 ~4 ~5 ~10 minecraft:mossy_stone_bricks
fill ~4 ~5 ~43 ~4 ~9 ~43 minecraft:stone_bricks
fill ~1 ~6 ~1 ~1 ~6 ~7 minecraft:stone_bricks
setblock ~1 ~6 ~8 minecraft:cracked_stone_bricks
fill ~1 ~6 ~9 ~1 ~6 ~10 minecraft:stone_bricks
fill ~1 ~6 ~36 ~2 ~6 ~36 minecraft:stone_bricks
setblock ~1 ~6 ~39 minecraft:deepslate_bricks
setblock ~1 ~6 ~40 minecraft:cracked_deepslate_bricks
fill ~1 ~6 ~41 ~2 ~6 ~42 minecraft:deepslate_bricks
setblock ~1 ~6 ~43 minecraft:cracked_deepslate_bricks
setblock ~1 ~6 ~44 minecraft:deepslate_bricks
fill ~2 ~6 ~2 ~2 ~8 ~2 minecraft:stone_bricks
setblock ~2 ~6 ~3 minecraft:cracked_stone_bricks
fill ~2 ~6 ~4 ~2 ~6 ~8 minecraft:stone_bricks
fill ~2 ~6 ~9 ~3 ~6 ~9 minecraft:cracked_stone_bricks
fill ~2 ~6 ~10 ~3 ~6 ~10 minecraft:stone_bricks
fill ~2 ~6 ~37 ~3 ~6 ~37 minecraft:deepslate_bricks
setblock ~2 ~6 ~38 minecraft:cracked_deepslate_bricks
fill ~3 ~6 ~1 ~3 ~6 ~2 minecraft:cracked_stone_bricks
setblock ~3 ~6 ~3 minecraft:stone_bricks
setblock ~3 ~6 ~4 minecraft:cracked_stone_bricks
fill ~3 ~6 ~5 ~3 ~6 ~8 minecraft:stone_bricks
setblock ~3 ~6 ~36 minecraft:cracked_deepslate_bricks
fill ~3 ~6 ~38 ~3 ~6 ~39 minecraft:deepslate_bricks
setblock ~3 ~6 ~40 minecraft:cracked_deepslate_bricks
fill ~3 ~6 ~42 ~3 ~8 ~42 minecraft:stone_bricks
setblock ~3 ~6 ~44 minecraft:stone_bricks
setblock ~4 ~6 ~6 minecraft:stone_bricks
setblock ~4 ~6 ~7 minecraft:cracked_stone_bricks
fill ~4 ~6 ~8 ~4 ~7 ~10 minecraft:stone_bricks
fill ~4 ~6 ~36 ~4 ~6 ~38 minecraft:deepslate_bricks
fill ~4 ~6 ~39 ~4 ~6 ~42 minecraft:stone_bricks
fill ~1 ~7 ~1 ~1 ~7 ~5 minecraft:stone_bricks
fill ~1 ~7 ~6 ~1 ~9 ~6 minecraft:cracked_stone_bricks
fill ~1 ~7 ~7 ~1 ~7 ~8 minecraft:stone_bricks
setblock ~1 ~7 ~9 minecraft:cracked_stone_bricks
fill ~1 ~7 ~10 ~1 ~8 ~10 minecraft:deepslate_bricks
setblock ~1 ~7 ~36 minecraft:cracked_stone_bricks
setblock ~1 ~7 ~37 minecraft:stone_bricks
setblock ~1 ~7 ~38 minecraft:cracked_stone_bricks
fill ~1 ~7 ~39 ~1 ~9 ~44 minecraft:stone_bricks
setblock ~2 ~7 ~1 minecraft:cracked_stone_bricks
fill ~2 ~7 ~3 ~2 ~7 ~4 minecraft:stone_bricks
fill ~2 ~7 ~5 ~3 ~7 ~5 minecraft:cracked_stone_bricks
fill ~2 ~7 ~6 ~2 ~7 ~7 minecraft:stone_bricks
fill ~2 ~7 ~8 ~2 ~7 ~9 minecraft:deepslate_bricks
setblock ~2 ~7 ~10 minecraft:cracked_stone_bricks
fill ~2 ~7 ~36 ~2 ~7 ~42 minecraft:stone_bricks
setblock ~2 ~7 ~45 minecraft:stone_bricks
fill ~3 ~7 ~1 ~3 ~7 ~2 minecraft:stone_bricks
fill ~3 ~7 ~3 ~3 ~8 ~3 minecraft:cracked_stone_bricks
fill ~3 ~7 ~4 ~3 ~9 ~4 minecraft:stone_bricks
fill ~3 ~7 ~6 ~3 ~7 ~7 minecraft:deepslate_bricks
setblock ~3 ~7 ~8 minecraft:cracked_deepslate_bricks
setblock ~3 ~7 ~9 minecraft:cracked_stone_bricks
fill ~3 ~7 ~10 ~3 ~9 ~10 minecraft:stone_bricks
setblock ~3 ~7 ~36 minecraft:cracked_stone_bricks
fill ~3 ~7 ~37 ~3 ~9 ~40 minecraft:stone_bricks
fill ~3 ~7 ~41 ~4 ~7 ~41 minecraft:cracked_stone_bricks
setblock ~3 ~7 ~44 minecraft:cracked_stone_bricks
fill ~4 ~7 ~3 ~4 ~9 ~3 minecraft:stone_bricks
fill ~4 ~7 ~4 ~4 ~8 ~6 minecraft:deepslate_bricks
setblock ~4 ~7 ~7 minecraft:stone_bricks
fill ~4 ~7 ~36 ~4 ~8 ~38 minecraft:stone_bricks
setblock ~4 ~7 ~39 minecraft:cracked_stone_bricks
fill ~4 ~7 ~40 ~4 ~9 ~40 minecraft:stone_bricks
fill ~4 ~7 ~42 ~4 ~9 ~42 minecraft:stone_bricks
fill ~1 ~8 ~1 ~3 ~9 ~1 minecraft:stone_bricks
setblock ~1 ~8 ~2 minecraft:stone_bricks
setblock ~1 ~8 ~3 minecraft:cracked_stone_bricks
fill ~1 ~8 ~4 ~1 ~9 ~5 minecraft:stone_bricks
setblock ~1 ~8 ~7 minecraft:stone_bricks
setblock ~1 ~8 ~8 minecraft:cracked_stone_bricks
fill ~1 ~8 ~9 ~2 ~8 ~9 minecraft:deepslate_bricks
setblock ~1 ~8 ~36 minecraft:cracked_deepslate_bricks
fill ~1 ~8 ~37 ~1 ~9 ~37 minecraft:deepslate_bricks
fill ~1 ~8 ~38 ~2 ~8 ~38 minecraft:stone_bricks
fill ~1 ~8 ~45 ~2 ~8 ~45 minecraft:cracked_stone_bricks
fill ~2 ~8 ~3 ~2 ~9 ~3 minecraft:stone_bricks
setblock ~2 ~8 ~4 minecraft:cracked_stone_bricks
fill ~2 ~8 ~5 ~2 ~9 ~6 minecraft:stone_bricks
setblock ~2 ~8 ~7 minecraft:deepslate_bricks
setblock ~2 ~8 ~8 minecraft:cracked_deepslate_bricks
setblock ~2 ~8 ~10 minecraft:deepslate_bricks
fill ~2 ~8 ~36 ~3 ~8 ~36 minecraft:stone_bricks
setblock ~2 ~8 ~37 minecraft:cracked_stone_bricks
fill ~2 ~8 ~39 ~2 ~8 ~42 minecraft:stone_bricks
setblock ~3 ~8 ~2 minecraft:cracked_stone_bricks
fill ~3 ~8 ~5 ~3 ~8 ~6 minecraft:deepslate_bricks
setblock ~3 ~8 ~7 minecraft:cracked_deepslate_bricks
setblock ~3 ~8 ~8 minecraft:deepslate_bricks
fill ~3 ~8 ~9 ~4 ~9 ~9 minecraft:stone_bricks
fill ~3 ~8 ~41 ~4 ~9 ~41 minecraft:stone_bricks
fill ~3 ~8 ~44 ~4 ~9 ~44 minecraft:stone_bricks
setblock ~4 ~8 ~7 minecraft:deepslate_bricks
setblock ~4 ~8 ~8 minecraft:cracked_stone_bricks
fill ~4 ~8 ~10 ~4 ~9 ~10 minecraft:stone_bricks
fill ~4 ~8 ~39 ~4 ~9 ~39 minecraft:stone_bricks
setblock ~4 ~8 ~45 minecraft:cracked_stone_bricks
fill ~1 ~9 ~2 ~2 ~9 ~2 minecraft:cracked_stone_bricks
setblock ~1 ~9 ~3 minecraft:stone_bricks
setblock ~1 ~9 ~7 minecraft:cracked_stone_bricks
fill ~1 ~9 ~8 ~1 ~9 ~10 minecraft:stone_bricks
fill ~1 ~9 ~36 ~2 ~9 ~36 minecraft:deepslate_bricks
setblock ~1 ~9 ~38 minecraft:cracked_deepslate_bricks
fill ~1 ~9 ~45 ~2 ~9 ~45 minecraft:stone_bricks
setblock ~2 ~9 ~4 minecraft:stone_bricks
fill ~2 ~9 ~7 ~2 ~9 ~8 minecraft:stone_bricks
setblock ~2 ~9 ~9 minecraft:cracked_deepslate_bricks
setblock ~2 ~9 ~10 minecraft:stone_bricks
fill ~2 ~9 ~37 ~2 ~9 ~39 minecraft:stone_bricks
fill ~2 ~9 ~40 ~2 ~9 ~42 minecraft:cracked_stone_bricks
setblock ~2 ~9 ~43 minecraft:stone_bricks
setblock ~2 ~9 ~44 minecraft:cracked_stone_bricks
fill ~3 ~9 ~2 ~3 ~9 ~3 minecraft:stone_bricks
fill ~3 ~9 ~5 ~3 ~9 ~6 minecraft:stone_bricks
setblock ~3 ~9 ~7 minecraft:deepslate_bricks
setblock ~3 ~9 ~8 minecraft:stone_bricks
setblock ~3 ~9 ~36 minecraft:stone_bricks
fill ~3 ~9 ~42 ~3 ~9 ~43 minecraft:cracked_stone_bricks
setblock ~4 ~9 ~4 minecraft:cracked_stone_bricks
setblock ~4 ~9 ~5 minecraft:deepslate_bricks
fill ~4 ~9 ~6 ~4 ~9 ~8 minecraft:stone_bricks
fill ~4 ~9 ~36 ~4 ~9 ~37 minecraft:cracked_stone_bricks
setblock ~4 ~9 ~38 minecraft:stone_bricks
setblock ~4 ~9 ~45 minecraft:stone_bricks

# the pilasters at x 0 on the stubs (z 3, 7 and z 39, 43)
setblock ~0 ~1 ~3 minecraft:mossy_stone_bricks
setblock ~0 ~1 ~7 minecraft:mossy_stone_bricks
fill ~0 ~1 ~39 ~0 ~3 ~39 minecraft:mossy_stone_bricks
fill ~0 ~1 ~43 ~0 ~3 ~43 minecraft:deepslate_bricks
fill ~0 ~2 ~3 ~0 ~3 ~3 minecraft:stone_bricks
setblock ~0 ~2 ~7 minecraft:deepslate_bricks
setblock ~0 ~3 ~7 minecraft:cracked_deepslate_bricks
fill ~0 ~4 ~3 ~0 ~6 ~3 minecraft:deepslate_bricks
fill ~0 ~4 ~7 ~0 ~6 ~7 minecraft:deepslate_bricks
fill ~0 ~4 ~39 ~0 ~6 ~39 minecraft:deepslate_bricks
setblock ~0 ~4 ~43 minecraft:cracked_deepslate_bricks
fill ~0 ~5 ~43 ~0 ~9 ~43 minecraft:deepslate_bricks
setblock ~0 ~7 ~3 minecraft:cracked_deepslate_bricks
setblock ~0 ~7 ~7 minecraft:cracked_stone_bricks
fill ~0 ~7 ~39 ~0 ~8 ~39 minecraft:cracked_deepslate_bricks
setblock ~0 ~8 ~3 minecraft:cracked_stone_bricks
fill ~0 ~8 ~7 ~0 ~9 ~7 minecraft:stone_bricks
setblock ~0 ~9 ~3 minecraft:stone_bricks
setblock ~0 ~9 ~39 minecraft:deepslate_bricks

# the corbel table at y 9 on the stub faces: upside-down stairs under the rim
fill ~0 ~9 ~1 ~0 ~9 ~2 minecraft:stone_brick_stairs[facing=east,half=top]
fill ~0 ~9 ~4 ~0 ~9 ~6 minecraft:stone_brick_stairs[facing=east,half=top]
fill ~0 ~9 ~8 ~0 ~9 ~10 minecraft:stone_brick_stairs[facing=east,half=top]
fill ~0 ~9 ~36 ~0 ~9 ~38 minecraft:stone_brick_stairs[facing=east,half=top]
fill ~0 ~9 ~40 ~0 ~9 ~42 minecraft:stone_brick_stairs[facing=east,half=top]
fill ~0 ~9 ~44 ~0 ~9 ~45 minecraft:stone_brick_stairs[facing=east,half=top]

# the stub decks at y 10: the rim at x 0, flagstones on the walk (x 1..2), stone on the shoulder (x 3..4)
fill ~0 ~10 ~1 ~0 ~10 ~2 minecraft:deepslate_bricks
fill ~0 ~10 ~3 ~1 ~10 ~3 minecraft:stone_bricks
fill ~0 ~10 ~4 ~0 ~10 ~10 minecraft:stone_bricks
fill ~0 ~10 ~36 ~1 ~10 ~36 minecraft:stone_bricks
fill ~0 ~10 ~37 ~0 ~10 ~39 minecraft:stone_bricks
fill ~0 ~10 ~40 ~0 ~10 ~45 minecraft:deepslate_bricks
setblock ~1 ~10 ~1 minecraft:stone_bricks
setblock ~1 ~10 ~2 minecraft:cracked_stone_bricks
setblock ~1 ~10 ~4 minecraft:cracked_stone_bricks
fill ~1 ~10 ~5 ~1 ~10 ~10 minecraft:stone_bricks
setblock ~1 ~10 ~37 minecraft:mossy_stone_bricks
fill ~1 ~10 ~38 ~2 ~10 ~38 minecraft:stone_bricks
fill ~1 ~10 ~39 ~2 ~10 ~39 minecraft:cobblestone
setblock ~1 ~10 ~40 minecraft:cobblestone
fill ~1 ~10 ~41 ~1 ~10 ~42 minecraft:stone_bricks
setblock ~1 ~10 ~43 minecraft:cracked_stone_bricks
setblock ~1 ~10 ~44 minecraft:stone_bricks
setblock ~1 ~10 ~45 minecraft:cracked_stone_bricks
setblock ~2 ~10 ~1 minecraft:mossy_stone_bricks
fill ~2 ~10 ~2 ~2 ~10 ~4 minecraft:stone_bricks
setblock ~2 ~10 ~5 minecraft:mossy_stone_bricks
fill ~2 ~10 ~6 ~2 ~10 ~9 minecraft:stone_bricks
setblock ~2 ~10 ~10 minecraft:cracked_stone_bricks
setblock ~2 ~10 ~36 minecraft:cobblestone
fill ~2 ~10 ~37 ~3 ~10 ~37 minecraft:stone_bricks
setblock ~2 ~10 ~40 minecraft:stone_bricks
setblock ~2 ~10 ~41 minecraft:mossy_stone_bricks
fill ~2 ~10 ~42 ~2 ~10 ~43 minecraft:stone_bricks
setblock ~2 ~10 ~44 minecraft:mossy_stone_bricks
setblock ~2 ~10 ~45 minecraft:stone_bricks
fill ~3 ~10 ~1 ~3 ~10 ~2 minecraft:cracked_deepslate_bricks
fill ~3 ~10 ~3 ~3 ~10 ~5 minecraft:deepslate_bricks
fill ~3 ~10 ~6 ~4 ~10 ~6 minecraft:cracked_deepslate_bricks
fill ~3 ~10 ~7 ~4 ~10 ~7 minecraft:deepslate_bricks
setblock ~3 ~10 ~8 minecraft:deepslate_bricks
fill ~3 ~10 ~9 ~4 ~10 ~10 minecraft:stone_bricks
setblock ~3 ~10 ~36 minecraft:cracked_stone_bricks
setblock ~3 ~10 ~38 minecraft:cracked_stone_bricks
fill ~3 ~10 ~39 ~3 ~10 ~40 minecraft:stone_bricks
fill ~3 ~10 ~41 ~3 ~10 ~43 minecraft:deepslate_bricks
setblock ~3 ~10 ~44 minecraft:cracked_deepslate_bricks
fill ~3 ~10 ~45 ~4 ~10 ~45 minecraft:deepslate_bricks
fill ~4 ~10 ~1 ~4 ~10 ~5 minecraft:deepslate_bricks
setblock ~4 ~10 ~8 minecraft:cracked_stone_bricks
fill ~4 ~10 ~36 ~4 ~10 ~38 minecraft:stone_bricks
setblock ~4 ~10 ~39 minecraft:deepslate_bricks
fill ~4 ~10 ~40 ~4 ~10 ~44 minecraft:cracked_deepslate_bricks

# the shoulder at y 11 behind the walk (open at the stair heads), the merlons on the rim at odd z: two high with a slab cap
setblock ~0 ~11 ~1 minecraft:cracked_deepslate_bricks
setblock ~0 ~11 ~3 minecraft:cracked_stone_bricks
fill ~0 ~11 ~5 ~0 ~12 ~5 minecraft:cracked_stone_bricks
fill ~0 ~11 ~7 ~0 ~12 ~7 minecraft:stone_bricks
fill ~0 ~11 ~9 ~0 ~12 ~9 minecraft:stone_bricks
setblock ~0 ~11 ~37 minecraft:deepslate_bricks
setblock ~0 ~11 ~39 minecraft:deepslate_bricks
setblock ~0 ~11 ~41 minecraft:cracked_deepslate_bricks
setblock ~0 ~11 ~43 minecraft:stone_bricks
fill ~0 ~11 ~45 ~0 ~12 ~45 minecraft:deepslate_bricks
fill ~3 ~11 ~4 ~4 ~11 ~5 minecraft:stone_bricks
setblock ~3 ~11 ~6 minecraft:cracked_stone_bricks
setblock ~3 ~11 ~7 minecraft:stone_bricks
setblock ~3 ~11 ~8 minecraft:cracked_stone_bricks
fill ~3 ~11 ~9 ~3 ~11 ~10 minecraft:stone_bricks
setblock ~3 ~11 ~36 minecraft:cracked_stone_bricks
fill ~3 ~11 ~37 ~3 ~11 ~42 minecraft:stone_bricks
fill ~4 ~11 ~6 ~4 ~11 ~8 minecraft:stone_bricks
fill ~4 ~11 ~9 ~4 ~11 ~10 minecraft:deepslate_bricks
fill ~4 ~11 ~36 ~4 ~11 ~39 minecraft:stone_bricks
setblock ~4 ~11 ~40 minecraft:cracked_stone_bricks
fill ~4 ~11 ~41 ~4 ~11 ~42 minecraft:stone_bricks
setblock ~0 ~12 ~1 minecraft:deepslate_bricks
setblock ~0 ~12 ~3 minecraft:stone_bricks
setblock ~0 ~12 ~37 minecraft:stone_bricks
setblock ~0 ~12 ~39 minecraft:stone_bricks
setblock ~0 ~12 ~41 minecraft:deepslate_bricks
setblock ~0 ~12 ~43 minecraft:deepslate_bricks
setblock ~0 ~13 ~1 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~13 ~3 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~13 ~5 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~13 ~7 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~13 ~9 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~13 ~37 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~13 ~39 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~13 ~41 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~13 ~43 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~13 ~45 minecraft:stone_brick_slab[type=bottom]

# the broken ends: a ragged top of broken courses, stair and slab caps in stone brick and moss
setblock ~2 ~3 ~46 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~1 ~4 ~0 minecraft:mossy_stone_brick_slab[type=bottom]
setblock ~4 ~4 ~46 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~3 ~5 ~0 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~3 ~6 ~46 minecraft:stone_brick_stairs[facing=south,half=bottom]

# rubble at the foot of the broken ends (x 5..6, z 0..3 and z 43..46): fallen stone, cracked and mossy, a few slabs
setblock ~5 ~1 ~0 minecraft:cobblestone
setblock ~5 ~1 ~46 minecraft:cobblestone_slab[type=bottom]
fill ~6 ~1 ~2 ~6 ~1 ~3 minecraft:cobblestone
setblock ~6 ~1 ~45 minecraft:mossy_cobblestone
fill ~5 ~2 ~0 ~6 ~2 ~0 minecraft:cracked_stone_bricks
fill ~5 ~2 ~46 ~6 ~2 ~46 minecraft:cracked_stone_bricks
setblock ~6 ~3 ~1 minecraft:cracked_stone_bricks
setblock ~6 ~3 ~45 minecraft:cracked_stone_bricks

# the stairs up the inside: x 5, climbing north from z 10 (y 1) to z 1 (y 10) and south from z 36 (y 1) to z 45 (y 10), on a wedge of cobble
setblock ~5 ~1 ~1 minecraft:andesite
fill ~5 ~1 ~2 ~5 ~6 ~2 minecraft:cobblestone
fill ~5 ~1 ~3 ~5 ~1 ~4 minecraft:mossy_cobblestone
fill ~5 ~1 ~5 ~5 ~1 ~7 minecraft:cobblestone
setblock ~5 ~1 ~8 minecraft:mossy_cobblestone
setblock ~5 ~1 ~9 minecraft:cobblestone
setblock ~5 ~1 ~10 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~5 ~1 ~36 minecraft:stone_brick_stairs[facing=south,half=bottom]
fill ~5 ~1 ~37 ~5 ~1 ~38 minecraft:cobblestone
setblock ~5 ~1 ~39 minecraft:mossy_cobblestone
setblock ~5 ~1 ~40 minecraft:cobblestone
fill ~5 ~1 ~41 ~5 ~4 ~41 minecraft:mossy_cobblestone
setblock ~5 ~1 ~42 minecraft:andesite
setblock ~5 ~1 ~43 minecraft:mossy_cobblestone
setblock ~5 ~1 ~44 minecraft:andesite
fill ~5 ~1 ~45 ~5 ~9 ~45 minecraft:cobblestone
fill ~5 ~2 ~1 ~5 ~5 ~1 minecraft:cobblestone
fill ~5 ~2 ~3 ~5 ~7 ~3 minecraft:cobblestone
setblock ~5 ~2 ~4 minecraft:andesite
fill ~5 ~2 ~5 ~5 ~3 ~5 minecraft:mossy_cobblestone
fill ~5 ~2 ~6 ~5 ~2 ~8 minecraft:cobblestone
setblock ~5 ~2 ~9 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~5 ~2 ~37 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~5 ~2 ~38 minecraft:mossy_cobblestone
fill ~5 ~2 ~39 ~5 ~2 ~40 minecraft:andesite
fill ~5 ~2 ~42 ~5 ~2 ~44 minecraft:cobblestone
setblock ~5 ~3 ~4 minecraft:mossy_cobblestone
fill ~5 ~3 ~6 ~5 ~4 ~6 minecraft:cobblestone
setblock ~5 ~3 ~7 minecraft:andesite
setblock ~5 ~3 ~8 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~5 ~3 ~38 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~5 ~3 ~39 minecraft:mossy_cobblestone
fill ~5 ~3 ~40 ~5 ~4 ~40 minecraft:cobblestone
fill ~5 ~3 ~42 ~5 ~3 ~43 minecraft:cobblestone
setblock ~5 ~3 ~44 minecraft:andesite
fill ~5 ~4 ~4 ~5 ~5 ~5 minecraft:cobblestone
setblock ~5 ~4 ~7 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~5 ~4 ~39 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~5 ~4 ~42 minecraft:mossy_cobblestone
fill ~5 ~4 ~43 ~5 ~5 ~43 minecraft:andesite
fill ~5 ~4 ~44 ~5 ~5 ~44 minecraft:cobblestone
setblock ~5 ~5 ~6 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~5 ~5 ~40 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~5 ~5 ~41 minecraft:cobblestone
setblock ~5 ~5 ~42 minecraft:andesite
setblock ~5 ~6 ~1 minecraft:mossy_cobblestone
setblock ~5 ~6 ~4 minecraft:cobblestone
setblock ~5 ~6 ~5 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~5 ~6 ~41 minecraft:stone_brick_stairs[facing=south,half=bottom]
fill ~5 ~6 ~42 ~5 ~6 ~44 minecraft:mossy_cobblestone
setblock ~5 ~7 ~1 minecraft:cobblestone
fill ~5 ~7 ~2 ~5 ~8 ~2 minecraft:mossy_cobblestone
setblock ~5 ~7 ~4 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~5 ~7 ~42 minecraft:stone_brick_stairs[facing=south,half=bottom]
fill ~5 ~7 ~43 ~5 ~7 ~44 minecraft:cobblestone
setblock ~5 ~8 ~1 minecraft:andesite
setblock ~5 ~8 ~3 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~5 ~8 ~43 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~5 ~8 ~44 minecraft:cobblestone
setblock ~5 ~9 ~1 minecraft:cobblestone
setblock ~5 ~9 ~2 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~5 ~9 ~44 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~5 ~10 ~1 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~5 ~10 ~45 minecraft:stone_brick_stairs[facing=south,half=bottom]

# tower z 11..17: walls y 1..11 with dark quoins at the corners, moss low
setblock ~0 ~1 ~11 minecraft:polished_blackstone_bricks
fill ~0 ~1 ~17 ~0 ~2 ~17 minecraft:deepslate_bricks
fill ~1 ~1 ~11 ~2 ~3 ~11 minecraft:mossy_stone_bricks
fill ~1 ~1 ~17 ~2 ~1 ~17 minecraft:mossy_stone_bricks
setblock ~3 ~1 ~11 minecraft:stone_bricks
setblock ~4 ~1 ~11 minecraft:cracked_stone_bricks
fill ~4 ~1 ~17 ~5 ~3 ~17 minecraft:mossy_stone_bricks
fill ~5 ~1 ~11 ~5 ~3 ~11 minecraft:stone_bricks
fill ~6 ~1 ~11 ~6 ~2 ~11 minecraft:deepslate_bricks
fill ~6 ~1 ~12 ~6 ~1 ~13 minecraft:stone_bricks
fill ~6 ~1 ~14 ~6 ~1 ~15 minecraft:cracked_stone_bricks
setblock ~6 ~1 ~16 minecraft:stone_bricks
fill ~6 ~1 ~17 ~6 ~2 ~17 minecraft:deepslate_bricks
setblock ~0 ~2 ~11 minecraft:deepslate_bricks
fill ~0 ~2 ~12 ~0 ~2 ~13 minecraft:cracked_stone_bricks
fill ~0 ~2 ~14 ~0 ~2 ~16 minecraft:stone_bricks
setblock ~1 ~2 ~17 minecraft:cracked_stone_bricks
fill ~2 ~2 ~17 ~2 ~3 ~17 minecraft:stone_bricks
fill ~3 ~2 ~11 ~4 ~3 ~11 minecraft:mossy_stone_bricks
fill ~6 ~2 ~12 ~6 ~3 ~12 minecraft:stone_bricks
fill ~6 ~2 ~13 ~6 ~2 ~14 minecraft:cracked_stone_bricks
fill ~6 ~2 ~15 ~6 ~3 ~16 minecraft:mossy_stone_bricks
fill ~0 ~3 ~11 ~0 ~3 ~13 minecraft:stone_bricks
fill ~0 ~3 ~15 ~0 ~4 ~15 minecraft:stone_bricks
setblock ~0 ~3 ~16 minecraft:cracked_stone_bricks
setblock ~0 ~3 ~17 minecraft:polished_blackstone_bricks
fill ~1 ~3 ~17 ~1 ~6 ~17 minecraft:deepslate_bricks
setblock ~6 ~3 ~11 minecraft:stone_bricks
setblock ~6 ~3 ~13 minecraft:stone_bricks
fill ~6 ~3 ~14 ~6 ~5 ~14 minecraft:mossy_stone_bricks
setblock ~6 ~3 ~17 minecraft:cracked_deepslate_bricks
fill ~0 ~4 ~11 ~0 ~6 ~11 minecraft:deepslate_bricks
fill ~0 ~4 ~12 ~0 ~4 ~13 minecraft:stone_bricks
fill ~0 ~4 ~16 ~0 ~4 ~17 minecraft:deepslate_bricks
fill ~1 ~4 ~11 ~2 ~4 ~11 minecraft:stone_bricks
fill ~2 ~4 ~17 ~2 ~6 ~17 minecraft:deepslate_bricks
setblock ~3 ~4 ~11 minecraft:deepslate_bricks
setblock ~3 ~4 ~17 minecraft:cracked_stone_bricks
fill ~4 ~4 ~11 ~5 ~4 ~11 minecraft:mossy_stone_bricks
fill ~4 ~4 ~17 ~5 ~6 ~17 minecraft:stone_bricks
fill ~6 ~4 ~11 ~6 ~5 ~11 minecraft:deepslate_bricks
fill ~6 ~4 ~12 ~6 ~5 ~13 minecraft:mossy_stone_bricks
setblock ~6 ~4 ~15 minecraft:mossy_stone_bricks
fill ~6 ~4 ~16 ~6 ~8 ~16 minecraft:stone_bricks
setblock ~6 ~4 ~17 minecraft:deepslate_bricks
fill ~0 ~5 ~12 ~0 ~5 ~13 minecraft:deepslate_bricks
setblock ~0 ~5 ~14 minecraft:cracked_deepslate_bricks
fill ~0 ~5 ~15 ~0 ~5 ~16 minecraft:deepslate_bricks
setblock ~0 ~5 ~17 minecraft:cracked_deepslate_bricks
fill ~1 ~5 ~11 ~2 ~7 ~11 minecraft:deepslate_bricks
setblock ~3 ~5 ~11 minecraft:cracked_deepslate_bricks
fill ~3 ~5 ~17 ~3 ~7 ~17 minecraft:stone_bricks
fill ~4 ~5 ~11 ~4 ~6 ~11 minecraft:deepslate_bricks
setblock ~5 ~5 ~11 minecraft:mossy_stone_bricks
setblock ~6 ~5 ~15 minecraft:stone_bricks
fill ~6 ~5 ~17 ~6 ~8 ~17 minecraft:cracked_deepslate_bricks
setblock ~0 ~6 ~12 minecraft:cracked_deepslate_bricks
fill ~0 ~6 ~13 ~0 ~6 ~15 minecraft:deepslate_bricks
setblock ~0 ~6 ~16 minecraft:cracked_deepslate_bricks
setblock ~0 ~6 ~17 minecraft:stone_bricks
fill ~3 ~6 ~11 ~3 ~7 ~11 minecraft:deepslate_bricks
setblock ~5 ~6 ~11 minecraft:deepslate_bricks
setblock ~6 ~6 ~11 minecraft:polished_blackstone_bricks
fill ~6 ~6 ~12 ~6 ~6 ~14 minecraft:stone_bricks
setblock ~6 ~6 ~15 minecraft:cracked_stone_bricks
setblock ~0 ~7 ~11 minecraft:polished_blackstone_bricks
setblock ~0 ~7 ~17 minecraft:cracked_deepslate_bricks
fill ~1 ~7 ~17 ~2 ~8 ~17 minecraft:stone_bricks
fill ~4 ~7 ~11 ~6 ~7 ~11 minecraft:stone_bricks
setblock ~4 ~7 ~17 minecraft:cracked_stone_bricks
setblock ~5 ~7 ~17 minecraft:stone_bricks
setblock ~6 ~7 ~12 minecraft:cracked_stone_bricks
fill ~6 ~7 ~13 ~6 ~7 ~15 minecraft:stone_bricks
fill ~0 ~8 ~11 ~1 ~9 ~11 minecraft:deepslate_bricks
setblock ~0 ~8 ~12 minecraft:deepslate_bricks
setblock ~0 ~8 ~13 minecraft:cracked_deepslate_bricks
fill ~0 ~8 ~14 ~0 ~9 ~14 minecraft:deepslate_bricks
fill ~0 ~8 ~15 ~0 ~8 ~16 minecraft:stone_bricks
fill ~0 ~8 ~17 ~0 ~10 ~17 minecraft:deepslate_bricks
setblock ~2 ~8 ~11 minecraft:cracked_deepslate_bricks
fill ~3 ~8 ~11 ~3 ~11 ~11 minecraft:stone_bricks
fill ~3 ~8 ~17 ~4 ~8 ~17 minecraft:deepslate_bricks
setblock ~4 ~8 ~11 minecraft:cracked_stone_bricks
fill ~5 ~8 ~11 ~5 ~11 ~11 minecraft:stone_bricks
setblock ~5 ~8 ~17 minecraft:cracked_deepslate_bricks
fill ~6 ~8 ~11 ~6 ~8 ~12 minecraft:deepslate_bricks
fill ~6 ~8 ~13 ~6 ~8 ~14 minecraft:cracked_deepslate_bricks
setblock ~6 ~8 ~15 minecraft:deepslate_bricks
setblock ~0 ~9 ~13 minecraft:deepslate_bricks
setblock ~0 ~9 ~15 minecraft:stone_bricks
fill ~1 ~9 ~17 ~1 ~10 ~17 minecraft:stone_bricks
setblock ~2 ~9 ~11 minecraft:cracked_stone_bricks
fill ~2 ~9 ~17 ~2 ~10 ~17 minecraft:cracked_stone_bricks
fill ~3 ~9 ~17 ~5 ~9 ~17 minecraft:stone_bricks
fill ~4 ~9 ~11 ~4 ~11 ~11 minecraft:stone_bricks
fill ~6 ~9 ~11 ~6 ~11 ~11 minecraft:deepslate_bricks
fill ~6 ~9 ~13 ~6 ~9 ~15 minecraft:stone_bricks
setblock ~6 ~9 ~16 minecraft:cracked_stone_bricks
setblock ~6 ~9 ~17 minecraft:polished_blackstone_bricks
fill ~0 ~10 ~11 ~0 ~11 ~11 minecraft:deepslate_bricks
fill ~0 ~10 ~13 ~0 ~10 ~14 minecraft:stone_bricks
setblock ~0 ~10 ~15 minecraft:cracked_stone_bricks
fill ~1 ~10 ~11 ~2 ~10 ~11 minecraft:stone_bricks
setblock ~3 ~10 ~17 minecraft:cracked_stone_bricks
fill ~4 ~10 ~17 ~5 ~10 ~17 minecraft:stone_bricks
fill ~6 ~10 ~13 ~6 ~11 ~13 minecraft:stone_bricks
setblock ~6 ~10 ~14 minecraft:cracked_stone_bricks
fill ~6 ~10 ~15 ~6 ~11 ~16 minecraft:stone_bricks
setblock ~6 ~10 ~17 minecraft:deepslate_bricks
setblock ~0 ~11 ~17 minecraft:polished_blackstone_bricks
setblock ~1 ~11 ~11 minecraft:stone_bricks
setblock ~1 ~11 ~17 minecraft:cracked_stone_bricks
setblock ~2 ~11 ~11 minecraft:cracked_stone_bricks
fill ~2 ~11 ~17 ~3 ~11 ~17 minecraft:stone_bricks
setblock ~4 ~11 ~17 minecraft:cracked_stone_bricks
setblock ~5 ~11 ~17 minecraft:stone_bricks
setblock ~6 ~11 ~12 minecraft:stone_bricks
setblock ~6 ~11 ~14 minecraft:stone_bricks
setblock ~6 ~11 ~17 minecraft:cracked_deepslate_bricks

# the plinth lip: cobblestone stairs at y 1 along the west face between the quoins
fill ~0 ~1 ~12 ~0 ~1 ~16 minecraft:cobblestone_stairs[facing=east,half=bottom]

# a string course of chiseled stone bricks at y 7 on the west face, under the high slits
fill ~0 ~7 ~12 ~0 ~7 ~16 minecraft:chiseled_stone_bricks

# the loft floor at y 6 and the top floor at y 12 (feet 13): flagstones
fill ~1 ~6 ~12 ~5 ~6 ~13 minecraft:spruce_planks
fill ~1 ~6 ~14 ~4 ~6 ~16 minecraft:spruce_planks
fill ~5 ~6 ~15 ~5 ~6 ~16 minecraft:spruce_planks
fill ~0 ~12 ~11 ~1 ~12 ~13 minecraft:stone_bricks
setblock ~0 ~12 ~14 minecraft:cracked_stone_bricks
setblock ~0 ~12 ~15 minecraft:mossy_stone_bricks
setblock ~0 ~12 ~16 minecraft:cracked_stone_bricks
setblock ~0 ~12 ~17 minecraft:stone_bricks
fill ~1 ~12 ~14 ~2 ~12 ~16 minecraft:stone_bricks
setblock ~1 ~12 ~17 minecraft:cobblestone
setblock ~2 ~12 ~11 minecraft:cracked_stone_bricks
setblock ~2 ~12 ~12 minecraft:cobblestone
setblock ~2 ~12 ~13 minecraft:cracked_stone_bricks
setblock ~2 ~12 ~17 minecraft:stone_bricks
fill ~3 ~12 ~11 ~3 ~12 ~17 minecraft:stone_bricks
fill ~4 ~12 ~11 ~4 ~12 ~12 minecraft:mossy_stone_bricks
fill ~4 ~12 ~13 ~4 ~12 ~15 minecraft:stone_bricks
setblock ~4 ~12 ~16 minecraft:mossy_stone_bricks
setblock ~4 ~12 ~17 minecraft:stone_bricks
fill ~5 ~12 ~11 ~6 ~12 ~13 minecraft:stone_bricks
fill ~5 ~12 ~15 ~5 ~12 ~16 minecraft:stone_bricks
setblock ~5 ~12 ~17 minecraft:cobblestone
setblock ~6 ~12 ~14 minecraft:stone_bricks
fill ~6 ~12 ~15 ~6 ~12 ~16 minecraft:mossy_stone_bricks
setblock ~6 ~12 ~17 minecraft:stone_bricks

# the ladder up the east wall: (5, 1..13, 14) against x 6, through a hole in both floors
fill ~5 ~1 ~14 ~5 ~13 ~14 minecraft:ladder[facing=west]

# the door onto the passage: two high at x 3 with a chiseled lintel; slits low and high on the west face, high on the east
fill ~3 ~1 ~17 ~3 ~2 ~17 minecraft:air
fill ~0 ~3 ~14 ~0 ~4 ~14 minecraft:air
setblock ~3 ~3 ~17 minecraft:chiseled_stone_bricks
fill ~0 ~9 ~12 ~0 ~10 ~12 minecraft:air
fill ~0 ~9 ~16 ~0 ~10 ~16 minecraft:air
fill ~6 ~9 ~12 ~6 ~10 ~12 minecraft:air

# the ground room: barrels along the far wall and a lantern on a chain; the loft: a barrel and a lantern
fill ~1 ~1 ~12 ~2 ~1 ~12 minecraft:barrel[facing=up]
setblock ~1 ~2 ~12 minecraft:barrel[facing=up]
setblock ~3 ~4 ~14 minecraft:lantern[hanging=true]
setblock ~3 ~5 ~14 minecraft:chain[axis=y]
setblock ~1 ~7 ~12 minecraft:barrel[facing=up]
setblock ~3 ~10 ~14 minecraft:lantern[hanging=true]
setblock ~3 ~11 ~14 minecraft:chain[axis=y]

# the corbel course at y 11 on the west face of the tower: upside-down deepslate brick stairs under the rampart
fill ~0 ~11 ~12 ~0 ~11 ~16 minecraft:deepslate_brick_stairs[facing=east,half=top]

# tower z 29..35: walls y 1..11 with dark quoins at the corners, moss low
setblock ~0 ~1 ~29 minecraft:cracked_deepslate_bricks
fill ~0 ~1 ~35 ~0 ~3 ~35 minecraft:deepslate_bricks
fill ~1 ~1 ~29 ~2 ~3 ~29 minecraft:mossy_stone_bricks
fill ~1 ~1 ~35 ~5 ~3 ~35 minecraft:mossy_stone_bricks
fill ~4 ~1 ~29 ~5 ~2 ~29 minecraft:mossy_stone_bricks
fill ~6 ~1 ~29 ~6 ~2 ~29 minecraft:deepslate_bricks
fill ~6 ~1 ~30 ~6 ~2 ~34 minecraft:mossy_stone_bricks
setblock ~6 ~1 ~35 minecraft:deepslate_bricks
setblock ~0 ~2 ~29 minecraft:deepslate_bricks
fill ~0 ~2 ~30 ~0 ~2 ~34 minecraft:mossy_stone_bricks
setblock ~6 ~2 ~35 minecraft:cracked_deepslate_bricks
setblock ~0 ~3 ~29 minecraft:polished_blackstone_bricks
fill ~0 ~3 ~30 ~0 ~3 ~31 minecraft:mossy_stone_bricks
fill ~0 ~3 ~33 ~0 ~3 ~34 minecraft:mossy_stone_bricks
fill ~4 ~3 ~29 ~5 ~4 ~29 minecraft:stone_bricks
setblock ~6 ~3 ~29 minecraft:cracked_deepslate_bricks
fill ~6 ~3 ~30 ~6 ~3 ~33 minecraft:stone_bricks
setblock ~6 ~3 ~34 minecraft:mossy_stone_bricks
setblock ~6 ~3 ~35 minecraft:stone_bricks
setblock ~0 ~4 ~29 minecraft:cracked_deepslate_bricks
fill ~0 ~4 ~30 ~0 ~4 ~31 minecraft:stone_bricks
fill ~0 ~4 ~33 ~0 ~4 ~34 minecraft:stone_bricks
fill ~0 ~4 ~35 ~0 ~5 ~35 minecraft:cracked_deepslate_bricks
fill ~1 ~4 ~29 ~3 ~4 ~29 minecraft:stone_bricks
fill ~1 ~4 ~35 ~2 ~4 ~35 minecraft:cracked_stone_bricks
fill ~3 ~4 ~35 ~5 ~4 ~35 minecraft:stone_bricks
setblock ~6 ~4 ~29 minecraft:deepslate_bricks
fill ~6 ~4 ~30 ~6 ~5 ~30 minecraft:stone_bricks
setblock ~6 ~4 ~31 minecraft:cracked_stone_bricks
fill ~6 ~4 ~32 ~6 ~6 ~34 minecraft:stone_bricks
setblock ~6 ~4 ~35 minecraft:cracked_deepslate_bricks
fill ~0 ~5 ~29 ~5 ~5 ~29 minecraft:deepslate_bricks
fill ~0 ~5 ~30 ~0 ~5 ~31 minecraft:deepslate_bricks
setblock ~0 ~5 ~32 minecraft:cracked_deepslate_bricks
fill ~0 ~5 ~33 ~0 ~5 ~34 minecraft:deepslate_bricks
setblock ~1 ~5 ~35 minecraft:cracked_deepslate_bricks
setblock ~2 ~5 ~35 minecraft:deepslate_bricks
fill ~3 ~5 ~35 ~3 ~6 ~35 minecraft:stone_bricks
fill ~4 ~5 ~35 ~5 ~5 ~35 minecraft:cracked_stone_bricks
setblock ~6 ~5 ~29 minecraft:cracked_deepslate_bricks
fill ~6 ~5 ~31 ~6 ~7 ~31 minecraft:stone_bricks
setblock ~6 ~5 ~35 minecraft:polished_blackstone_bricks
fill ~0 ~6 ~29 ~2 ~6 ~29 minecraft:deepslate_bricks
setblock ~0 ~6 ~30 minecraft:cracked_deepslate_bricks
fill ~0 ~6 ~31 ~0 ~6 ~32 minecraft:deepslate_bricks
fill ~0 ~6 ~33 ~0 ~6 ~34 minecraft:cracked_deepslate_bricks
fill ~0 ~6 ~35 ~1 ~6 ~35 minecraft:deepslate_bricks
fill ~2 ~6 ~35 ~2 ~7 ~35 minecraft:cracked_deepslate_bricks
fill ~3 ~6 ~29 ~4 ~6 ~29 minecraft:cracked_deepslate_bricks
fill ~4 ~6 ~35 ~4 ~7 ~35 minecraft:stone_bricks
fill ~5 ~6 ~29 ~5 ~9 ~29 minecraft:deepslate_bricks
setblock ~5 ~6 ~35 minecraft:cracked_stone_bricks
fill ~6 ~6 ~29 ~6 ~7 ~29 minecraft:polished_blackstone_bricks
setblock ~6 ~6 ~30 minecraft:cracked_stone_bricks
setblock ~6 ~6 ~35 minecraft:deepslate_bricks
fill ~0 ~7 ~29 ~1 ~7 ~29 minecraft:stone_bricks
setblock ~0 ~7 ~35 minecraft:polished_blackstone_bricks
setblock ~1 ~7 ~35 minecraft:cracked_deepslate_bricks
fill ~2 ~7 ~29 ~4 ~7 ~29 minecraft:deepslate_bricks
fill ~3 ~7 ~35 ~3 ~9 ~35 minecraft:deepslate_bricks
setblock ~5 ~7 ~35 minecraft:stone_bricks
setblock ~6 ~7 ~30 minecraft:cracked_deepslate_bricks
setblock ~6 ~7 ~32 minecraft:stone_bricks
setblock ~6 ~7 ~33 minecraft:cracked_stone_bricks
fill ~6 ~7 ~34 ~6 ~8 ~34 minecraft:stone_bricks
setblock ~6 ~7 ~35 minecraft:polished_blackstone_bricks
setblock ~0 ~8 ~29 minecraft:deepslate_bricks
fill ~0 ~8 ~30 ~0 ~8 ~32 minecraft:stone_bricks
setblock ~0 ~8 ~33 minecraft:cracked_stone_bricks
setblock ~0 ~8 ~34 minecraft:stone_bricks
fill ~0 ~8 ~35 ~2 ~8 ~35 minecraft:deepslate_bricks
fill ~1 ~8 ~29 ~2 ~9 ~29 minecraft:stone_bricks
setblock ~3 ~8 ~29 minecraft:cracked_deepslate_bricks
fill ~4 ~8 ~29 ~4 ~9 ~29 minecraft:deepslate_bricks
fill ~4 ~8 ~35 ~4 ~9 ~35 minecraft:deepslate_bricks
fill ~5 ~8 ~35 ~6 ~8 ~35 minecraft:cracked_deepslate_bricks
fill ~6 ~8 ~29 ~6 ~8 ~33 minecraft:deepslate_bricks
setblock ~0 ~9 ~29 minecraft:polished_blackstone_bricks
setblock ~0 ~9 ~31 minecraft:cracked_stone_bricks
fill ~0 ~9 ~32 ~0 ~10 ~33 minecraft:stone_bricks
setblock ~0 ~9 ~35 minecraft:polished_blackstone_bricks
fill ~1 ~9 ~35 ~2 ~9 ~35 minecraft:deepslate_bricks
setblock ~3 ~9 ~29 minecraft:stone_bricks
fill ~5 ~9 ~35 ~5 ~10 ~35 minecraft:deepslate_bricks
setblock ~6 ~9 ~29 minecraft:deepslate_bricks
fill ~6 ~9 ~31 ~6 ~11 ~33 minecraft:deepslate_bricks
fill ~6 ~9 ~34 ~6 ~9 ~35 minecraft:cracked_deepslate_bricks
fill ~0 ~10 ~29 ~0 ~11 ~29 minecraft:deepslate_bricks
setblock ~0 ~10 ~31 minecraft:stone_bricks
setblock ~0 ~10 ~35 minecraft:cracked_deepslate_bricks
fill ~1 ~10 ~29 ~3 ~10 ~29 minecraft:cracked_stone_bricks
fill ~1 ~10 ~35 ~2 ~11 ~35 minecraft:stone_bricks
fill ~3 ~10 ~35 ~4 ~10 ~35 minecraft:cracked_deepslate_bricks
fill ~4 ~10 ~29 ~5 ~11 ~29 minecraft:stone_bricks
setblock ~6 ~10 ~29 minecraft:polished_blackstone_bricks
fill ~6 ~10 ~34 ~6 ~11 ~35 minecraft:deepslate_bricks
setblock ~0 ~11 ~35 minecraft:deepslate_bricks
fill ~1 ~11 ~29 ~3 ~11 ~29 minecraft:stone_bricks
fill ~3 ~11 ~35 ~4 ~11 ~35 minecraft:deepslate_bricks
setblock ~5 ~11 ~35 minecraft:cracked_deepslate_bricks
fill ~6 ~11 ~29 ~6 ~11 ~30 minecraft:deepslate_bricks

# the plinth lip: cobblestone stairs at y 1 along the west face between the quoins
fill ~0 ~1 ~30 ~0 ~1 ~34 minecraft:cobblestone_stairs[facing=east,half=bottom]

# a string course of chiseled stone bricks at y 7 on the west face, under the high slits
fill ~0 ~7 ~30 ~0 ~7 ~34 minecraft:chiseled_stone_bricks

# the loft floor at y 6 and the top floor at y 12 (feet 13): flagstones
fill ~1 ~6 ~30 ~5 ~6 ~31 minecraft:spruce_planks
fill ~1 ~6 ~32 ~4 ~6 ~34 minecraft:spruce_planks
fill ~5 ~6 ~33 ~5 ~6 ~34 minecraft:spruce_planks
setblock ~0 ~12 ~29 minecraft:cracked_stone_bricks
fill ~0 ~12 ~30 ~0 ~12 ~32 minecraft:stone_bricks
setblock ~0 ~12 ~33 minecraft:cracked_stone_bricks
setblock ~0 ~12 ~34 minecraft:stone_bricks
setblock ~0 ~12 ~35 minecraft:cracked_stone_bricks
setblock ~1 ~12 ~29 minecraft:stone_bricks
fill ~1 ~12 ~30 ~2 ~12 ~30 minecraft:cobblestone
fill ~1 ~12 ~31 ~2 ~12 ~33 minecraft:stone_bricks
setblock ~1 ~12 ~34 minecraft:cracked_stone_bricks
fill ~1 ~12 ~35 ~2 ~12 ~35 minecraft:stone_bricks
fill ~2 ~12 ~29 ~3 ~12 ~29 minecraft:cracked_stone_bricks
setblock ~2 ~12 ~34 minecraft:mossy_stone_bricks
fill ~3 ~12 ~30 ~3 ~12 ~31 minecraft:stone_bricks
setblock ~3 ~12 ~32 minecraft:cobblestone
setblock ~3 ~12 ~33 minecraft:mossy_stone_bricks
fill ~3 ~12 ~34 ~3 ~12 ~35 minecraft:stone_bricks
fill ~4 ~12 ~29 ~6 ~12 ~29 minecraft:stone_bricks
setblock ~4 ~12 ~30 minecraft:mossy_stone_bricks
setblock ~4 ~12 ~31 minecraft:stone_bricks
setblock ~4 ~12 ~32 minecraft:mossy_stone_bricks
fill ~4 ~12 ~33 ~5 ~12 ~34 minecraft:stone_bricks
setblock ~4 ~12 ~35 minecraft:mossy_stone_bricks
fill ~5 ~12 ~30 ~6 ~12 ~31 minecraft:stone_bricks
setblock ~5 ~12 ~35 minecraft:stone_bricks
setblock ~6 ~12 ~32 minecraft:stone_bricks
setblock ~6 ~12 ~33 minecraft:cracked_stone_bricks
fill ~6 ~12 ~34 ~6 ~12 ~35 minecraft:stone_bricks

# the ladder up the east wall: (5, 1..13, 32) against x 6, through a hole in both floors
fill ~5 ~1 ~32 ~5 ~13 ~32 minecraft:ladder[facing=west]

# the door onto the passage: two high at x 3 with a chiseled lintel; slits low and high on the west face, high on the east
fill ~3 ~1 ~29 ~3 ~2 ~29 minecraft:air
fill ~0 ~3 ~32 ~0 ~4 ~32 minecraft:air
setblock ~3 ~3 ~29 minecraft:chiseled_stone_bricks
fill ~0 ~9 ~30 ~0 ~10 ~30 minecraft:air
fill ~0 ~9 ~34 ~0 ~10 ~34 minecraft:air
fill ~6 ~9 ~30 ~6 ~10 ~30 minecraft:air

# the ground room: barrels along the far wall and a lantern on a chain; the loft: a barrel and a lantern
fill ~1 ~1 ~34 ~2 ~1 ~34 minecraft:barrel[facing=up]
setblock ~1 ~2 ~34 minecraft:barrel[facing=up]
setblock ~3 ~4 ~32 minecraft:lantern[hanging=true]
setblock ~3 ~5 ~32 minecraft:chain[axis=y]
setblock ~1 ~7 ~34 minecraft:barrel[facing=up]
setblock ~3 ~10 ~32 minecraft:lantern[hanging=true]
setblock ~3 ~11 ~32 minecraft:chain[axis=y]

# the corbel course at y 11 on the west face of the tower: upside-down deepslate brick stairs under the rampart
fill ~0 ~11 ~30 ~0 ~11 ~34 minecraft:deepslate_brick_stairs[facing=east,half=top]

# the arch over the passage: voussoirs from y 7 at the springs to y 10 at the crown, the soffit of upside-down stairs; clear below
fill ~1 ~7 ~18 ~6 ~7 ~18 minecraft:stone_brick_stairs[facing=north,half=top]
fill ~1 ~7 ~28 ~6 ~7 ~28 minecraft:stone_brick_stairs[facing=south,half=top]
fill ~0 ~8 ~18 ~0 ~11 ~18 minecraft:stone_bricks
setblock ~0 ~8 ~28 minecraft:mossy_stone_bricks
setblock ~1 ~8 ~18 minecraft:cracked_deepslate_bricks
fill ~1 ~8 ~19 ~6 ~8 ~19 minecraft:stone_brick_stairs[facing=north,half=top]
fill ~1 ~8 ~27 ~6 ~8 ~27 minecraft:stone_brick_stairs[facing=south,half=top]
fill ~1 ~8 ~28 ~2 ~8 ~28 minecraft:stone_bricks
fill ~2 ~8 ~18 ~2 ~11 ~18 minecraft:stone_bricks
setblock ~3 ~8 ~18 minecraft:mossy_stone_bricks
setblock ~3 ~8 ~28 minecraft:cracked_stone_bricks
setblock ~4 ~8 ~18 minecraft:stone_bricks
fill ~4 ~8 ~28 ~5 ~10 ~28 minecraft:deepslate_bricks
setblock ~5 ~8 ~18 minecraft:deepslate_bricks
setblock ~6 ~8 ~18 minecraft:cracked_deepslate_bricks
setblock ~6 ~8 ~28 minecraft:stone_bricks
setblock ~0 ~9 ~19 minecraft:stone_bricks
fill ~0 ~9 ~27 ~2 ~9 ~27 minecraft:stone_bricks
setblock ~0 ~9 ~28 minecraft:stone_bricks
fill ~1 ~9 ~18 ~1 ~9 ~19 minecraft:stone_bricks
fill ~1 ~9 ~20 ~2 ~9 ~21 minecraft:stone_brick_stairs[facing=north,half=top]
fill ~1 ~9 ~25 ~6 ~9 ~25 minecraft:stone_brick_stairs[facing=south,half=top]
fill ~1 ~9 ~26 ~2 ~9 ~26 minecraft:stone_brick_stairs[facing=south,half=top]
setblock ~1 ~9 ~28 minecraft:cracked_stone_bricks
setblock ~2 ~9 ~19 minecraft:stone_bricks
setblock ~2 ~9 ~28 minecraft:mossy_stone_bricks
setblock ~3 ~9 ~18 minecraft:deepslate_bricks
setblock ~3 ~9 ~19 minecraft:cracked_stone_bricks
setblock ~3 ~9 ~21 minecraft:stone_brick_stairs[facing=north,half=top]
fill ~3 ~9 ~27 ~4 ~9 ~27 minecraft:deepslate_bricks
setblock ~3 ~9 ~28 minecraft:deepslate_bricks
setblock ~4 ~9 ~18 minecraft:cracked_stone_bricks
setblock ~4 ~9 ~19 minecraft:deepslate_bricks
fill ~4 ~9 ~20 ~6 ~9 ~21 minecraft:stone_brick_stairs[facing=north,half=top]
fill ~4 ~9 ~26 ~6 ~9 ~26 minecraft:stone_brick_stairs[facing=south,half=top]
fill ~5 ~9 ~18 ~5 ~10 ~18 minecraft:stone_bricks
setblock ~5 ~9 ~19 minecraft:cracked_stone_bricks
setblock ~5 ~9 ~27 minecraft:cracked_deepslate_bricks
setblock ~6 ~9 ~18 minecraft:cracked_stone_bricks
setblock ~6 ~9 ~19 minecraft:cracked_deepslate_bricks
fill ~6 ~9 ~27 ~6 ~9 ~28 minecraft:cracked_stone_bricks
setblock ~0 ~10 ~19 minecraft:deepslate_bricks
setblock ~0 ~10 ~20 minecraft:stone_bricks
setblock ~0 ~10 ~21 minecraft:cracked_stone_bricks
fill ~0 ~10 ~25 ~0 ~10 ~26 minecraft:deepslate_bricks
setblock ~0 ~10 ~27 minecraft:stone_bricks
setblock ~0 ~10 ~28 minecraft:deepslate_bricks
setblock ~1 ~10 ~18 minecraft:mossy_stone_bricks
fill ~1 ~10 ~19 ~1 ~10 ~21 minecraft:stone_bricks
fill ~1 ~10 ~22 ~2 ~10 ~22 minecraft:cracked_deepslate_bricks
fill ~1 ~10 ~23 ~1 ~11 ~23 minecraft:deepslate_bricks
setblock ~1 ~10 ~24 minecraft:cracked_deepslate_bricks
fill ~1 ~10 ~25 ~1 ~11 ~26 minecraft:stone_bricks
setblock ~1 ~10 ~27 minecraft:mossy_stone_bricks
setblock ~1 ~10 ~28 minecraft:cracked_deepslate_bricks
setblock ~2 ~10 ~19 minecraft:cracked_deepslate_bricks
fill ~2 ~10 ~20 ~2 ~11 ~20 minecraft:stone_bricks
fill ~2 ~10 ~21 ~3 ~10 ~21 minecraft:cracked_deepslate_bricks
fill ~2 ~10 ~23 ~3 ~10 ~23 minecraft:cracked_deepslate_bricks
fill ~2 ~10 ~24 ~2 ~10 ~25 minecraft:deepslate_bricks
fill ~2 ~10 ~26 ~2 ~11 ~26 minecraft:stone_bricks
fill ~2 ~10 ~27 ~2 ~10 ~28 minecraft:cracked_deepslate_bricks
fill ~3 ~10 ~18 ~4 ~10 ~19 minecraft:stone_bricks
setblock ~3 ~10 ~20 minecraft:cracked_stone_bricks
fill ~3 ~10 ~22 ~5 ~10 ~22 minecraft:deepslate_bricks
setblock ~3 ~10 ~24 minecraft:cracked_deepslate_bricks
fill ~3 ~10 ~25 ~4 ~10 ~25 minecraft:stone_bricks
fill ~3 ~10 ~26 ~4 ~10 ~26 minecraft:mossy_stone_bricks
setblock ~3 ~10 ~27 minecraft:stone_bricks
setblock ~3 ~10 ~28 minecraft:cracked_stone_bricks
fill ~4 ~10 ~20 ~5 ~10 ~20 minecraft:stone_bricks
setblock ~4 ~10 ~21 minecraft:cracked_stone_bricks
fill ~4 ~10 ~23 ~4 ~11 ~23 minecraft:stone_bricks
setblock ~4 ~10 ~24 minecraft:polished_blackstone_bricks
setblock ~4 ~10 ~27 minecraft:deepslate_bricks
setblock ~5 ~10 ~19 minecraft:mossy_stone_bricks
setblock ~5 ~10 ~21 minecraft:stone_bricks
setblock ~5 ~10 ~23 minecraft:deepslate_bricks
fill ~5 ~10 ~24 ~5 ~11 ~24 minecraft:stone_bricks
fill ~5 ~10 ~25 ~5 ~11 ~25 minecraft:mossy_stone_bricks
setblock ~5 ~10 ~26 minecraft:deepslate_bricks
fill ~5 ~10 ~27 ~6 ~10 ~27 minecraft:cracked_stone_bricks
fill ~6 ~10 ~18 ~6 ~10 ~19 minecraft:stone_bricks
setblock ~6 ~10 ~20 minecraft:mossy_stone_bricks
fill ~6 ~10 ~21 ~6 ~10 ~25 minecraft:deepslate_bricks
setblock ~6 ~10 ~26 minecraft:stone_bricks
fill ~6 ~10 ~28 ~6 ~11 ~28 minecraft:stone_bricks
setblock ~0 ~11 ~19 minecraft:stone_bricks
setblock ~0 ~11 ~20 minecraft:cracked_stone_bricks
setblock ~0 ~11 ~21 minecraft:cracked_deepslate_bricks
fill ~0 ~11 ~22 ~1 ~11 ~22 minecraft:stone_bricks
setblock ~0 ~11 ~24 minecraft:deepslate_bricks
setblock ~0 ~11 ~25 minecraft:cracked_deepslate_bricks
setblock ~0 ~11 ~26 minecraft:deepslate_bricks
setblock ~0 ~11 ~27 minecraft:cracked_deepslate_bricks
setblock ~0 ~11 ~28 minecraft:stone_bricks
setblock ~1 ~11 ~18 minecraft:stone_bricks
fill ~1 ~11 ~19 ~1 ~11 ~21 minecraft:cracked_deepslate_bricks
setblock ~1 ~11 ~24 minecraft:cracked_stone_bricks
fill ~1 ~11 ~27 ~1 ~11 ~28 minecraft:cracked_stone_bricks
setblock ~2 ~11 ~19 minecraft:deepslate_bricks
setblock ~2 ~11 ~21 minecraft:deepslate_bricks
fill ~2 ~11 ~22 ~2 ~11 ~23 minecraft:cracked_stone_bricks
setblock ~2 ~11 ~24 minecraft:cracked_deepslate_bricks
setblock ~2 ~11 ~25 minecraft:deepslate_bricks
setblock ~2 ~11 ~27 minecraft:stone_bricks
setblock ~2 ~11 ~28 minecraft:cracked_deepslate_bricks
setblock ~3 ~11 ~18 minecraft:stone_bricks
setblock ~3 ~11 ~19 minecraft:cracked_stone_bricks
setblock ~3 ~11 ~20 minecraft:deepslate_bricks
setblock ~3 ~11 ~21 minecraft:cracked_stone_bricks
setblock ~3 ~11 ~22 minecraft:deepslate_bricks
setblock ~3 ~11 ~23 minecraft:stone_bricks
setblock ~3 ~11 ~24 minecraft:deepslate_bricks
setblock ~3 ~11 ~25 minecraft:mossy_stone_bricks
setblock ~3 ~11 ~26 minecraft:cracked_stone_bricks
fill ~3 ~11 ~27 ~4 ~11 ~27 minecraft:cracked_deepslate_bricks
fill ~3 ~11 ~28 ~5 ~11 ~28 minecraft:stone_bricks
setblock ~4 ~11 ~18 minecraft:cracked_stone_bricks
setblock ~4 ~11 ~19 minecraft:deepslate_bricks
fill ~4 ~11 ~20 ~4 ~11 ~22 minecraft:stone_bricks
setblock ~4 ~11 ~24 minecraft:cracked_deepslate_bricks
setblock ~4 ~11 ~25 minecraft:deepslate_bricks
fill ~4 ~11 ~26 ~5 ~11 ~26 minecraft:stone_bricks
setblock ~5 ~11 ~18 minecraft:cracked_deepslate_bricks
fill ~5 ~11 ~19 ~6 ~11 ~19 minecraft:stone_bricks
setblock ~5 ~11 ~20 minecraft:mossy_stone_bricks
fill ~5 ~11 ~21 ~5 ~11 ~22 minecraft:deepslate_bricks
setblock ~5 ~11 ~23 minecraft:stone_bricks
setblock ~5 ~11 ~27 minecraft:mossy_stone_bricks
setblock ~6 ~11 ~18 minecraft:cracked_stone_bricks
fill ~6 ~11 ~20 ~6 ~11 ~21 minecraft:stone_bricks
fill ~6 ~11 ~22 ~6 ~11 ~23 minecraft:cracked_stone_bricks
setblock ~6 ~11 ~24 minecraft:mossy_stone_bricks
setblock ~6 ~11 ~25 minecraft:cracked_stone_bricks
setblock ~6 ~11 ~26 minecraft:deepslate_bricks
setblock ~6 ~11 ~27 minecraft:stone_bricks

# the face of the arch at x 0: a ring of dark deepslate on the soffit line and chiseled keystones at the crown
setblock ~0 ~7 ~18 minecraft:deepslate_bricks
setblock ~0 ~7 ~28 minecraft:deepslate_bricks
setblock ~0 ~8 ~19 minecraft:deepslate_bricks
setblock ~0 ~8 ~27 minecraft:deepslate_bricks
fill ~0 ~9 ~20 ~0 ~9 ~21 minecraft:deepslate_bricks
fill ~0 ~9 ~25 ~0 ~9 ~26 minecraft:deepslate_bricks
fill ~0 ~10 ~22 ~0 ~10 ~24 minecraft:chiseled_stone_bricks
setblock ~0 ~11 ~23 minecraft:chiseled_stone_bricks

# the deck across the passage at y 12 (feet 13): flagstones, one rampart with the two towers
setblock ~0 ~12 ~18 minecraft:cracked_stone_bricks
fill ~0 ~12 ~19 ~2 ~12 ~19 minecraft:stone_bricks
fill ~0 ~12 ~20 ~0 ~12 ~21 minecraft:stone_bricks
fill ~0 ~12 ~22 ~1 ~12 ~22 minecraft:mossy_stone_bricks
fill ~0 ~12 ~23 ~0 ~12 ~24 minecraft:stone_bricks
setblock ~0 ~12 ~25 minecraft:cobblestone
fill ~0 ~12 ~26 ~1 ~12 ~26 minecraft:cracked_stone_bricks
setblock ~0 ~12 ~27 minecraft:cobblestone
setblock ~0 ~12 ~28 minecraft:stone_bricks
setblock ~1 ~12 ~18 minecraft:mossy_stone_bricks
setblock ~1 ~12 ~20 minecraft:cracked_stone_bricks
setblock ~1 ~12 ~21 minecraft:stone_bricks
setblock ~1 ~12 ~23 minecraft:cobblestone
setblock ~1 ~12 ~24 minecraft:stone_bricks
setblock ~1 ~12 ~25 minecraft:mossy_stone_bricks
fill ~1 ~12 ~27 ~1 ~12 ~28 minecraft:stone_bricks
setblock ~2 ~12 ~18 minecraft:cracked_stone_bricks
fill ~2 ~12 ~20 ~3 ~12 ~20 minecraft:stone_bricks
fill ~2 ~12 ~21 ~3 ~12 ~21 minecraft:mossy_stone_bricks
fill ~2 ~12 ~22 ~3 ~12 ~22 minecraft:stone_bricks
fill ~2 ~12 ~23 ~3 ~12 ~23 minecraft:cracked_stone_bricks
setblock ~2 ~12 ~24 minecraft:cracked_stone_bricks
fill ~2 ~12 ~25 ~2 ~12 ~27 minecraft:stone_bricks
setblock ~2 ~12 ~28 minecraft:cobblestone
fill ~3 ~12 ~18 ~3 ~12 ~19 minecraft:mossy_stone_bricks
setblock ~3 ~12 ~24 minecraft:cobblestone
setblock ~3 ~12 ~25 minecraft:mossy_stone_bricks
fill ~3 ~12 ~26 ~3 ~12 ~28 minecraft:stone_bricks
fill ~4 ~12 ~18 ~6 ~12 ~19 minecraft:stone_bricks
setblock ~4 ~12 ~20 minecraft:mossy_stone_bricks
fill ~4 ~12 ~21 ~4 ~12 ~27 minecraft:stone_bricks
setblock ~4 ~12 ~28 minecraft:cobblestone
fill ~5 ~12 ~20 ~5 ~12 ~23 minecraft:stone_bricks
fill ~5 ~12 ~24 ~5 ~12 ~25 minecraft:mossy_stone_bricks
fill ~5 ~12 ~26 ~5 ~12 ~27 minecraft:stone_bricks
setblock ~5 ~12 ~28 minecraft:cracked_stone_bricks
setblock ~6 ~12 ~20 minecraft:mossy_stone_bricks
setblock ~6 ~12 ~21 minecraft:stone_bricks
setblock ~6 ~12 ~22 minecraft:cracked_stone_bricks
fill ~6 ~12 ~23 ~6 ~12 ~25 minecraft:stone_bricks
setblock ~6 ~12 ~26 minecraft:cobblestone
setblock ~6 ~12 ~27 minecraft:cracked_stone_bricks
setblock ~6 ~12 ~28 minecraft:stone_bricks

# the raised portcullis: iron bars at x 1 from y 7 up into the arch, every post and a cross bar
setblock ~1 ~7 ~19 minecraft:iron_bars[north=false,east=false,south=false,west=false]
setblock ~1 ~7 ~21 minecraft:iron_bars[north=false,east=false,south=false,west=false]
fill ~1 ~7 ~23 ~1 ~8 ~23 minecraft:iron_bars[north=false,east=false,south=false,west=false]
setblock ~1 ~7 ~25 minecraft:iron_bars[north=false,east=false,south=false,west=false]
setblock ~1 ~7 ~27 minecraft:iron_bars[north=false,east=false,south=false,west=false]
setblock ~1 ~8 ~20 minecraft:iron_bars[north=false,east=false,south=true,west=false]
setblock ~1 ~8 ~21 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~1 ~8 ~25 minecraft:iron_bars[north=false,east=false,south=true,west=false]
setblock ~1 ~8 ~26 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~1 ~9 ~22 minecraft:iron_bars[north=false,east=false,south=true,west=false]
setblock ~1 ~9 ~23 minecraft:iron_bars[north=true,east=false,south=true,west=false]
setblock ~1 ~9 ~24 minecraft:iron_bars[north=true,east=false,south=false,west=false]

# two lanterns hung in the arch (above head height), each from a full block of the soffit (a stair does not carry a hanging lantern)
setblock ~3 ~8 ~20 minecraft:lantern[hanging=true]
setblock ~3 ~8 ~26 minecraft:lantern[hanging=true]
setblock ~3 ~9 ~20 minecraft:deepslate_bricks
setblock ~3 ~9 ~26 minecraft:deepslate_bricks

# the rampart: a parapet at y 13 round the gatehouse top, the west side open at z 13..15 and z 31..33; merlons at y 14 on the rim; corner posts with lanterns
fill ~0 ~13 ~11 ~0 ~13 ~12 minecraft:stone_bricks
fill ~0 ~13 ~16 ~0 ~13 ~20 minecraft:stone_bricks
setblock ~0 ~13 ~21 minecraft:cracked_stone_bricks
setblock ~0 ~13 ~22 minecraft:stone_bricks
setblock ~0 ~13 ~23 minecraft:cracked_deepslate_bricks
fill ~0 ~13 ~24 ~0 ~13 ~27 minecraft:deepslate_bricks
fill ~0 ~13 ~28 ~0 ~13 ~30 minecraft:stone_bricks
fill ~0 ~13 ~34 ~0 ~14 ~35 minecraft:deepslate_bricks
fill ~1 ~13 ~11 ~2 ~13 ~11 minecraft:cracked_stone_bricks
fill ~1 ~13 ~35 ~3 ~13 ~35 minecraft:deepslate_bricks
fill ~3 ~13 ~11 ~6 ~13 ~11 minecraft:stone_bricks
fill ~4 ~13 ~35 ~5 ~13 ~35 minecraft:stone_bricks
fill ~6 ~13 ~12 ~6 ~13 ~14 minecraft:deepslate_bricks
setblock ~6 ~13 ~15 minecraft:cracked_deepslate_bricks
setblock ~6 ~13 ~16 minecraft:deepslate_bricks
fill ~6 ~13 ~17 ~6 ~13 ~21 minecraft:stone_bricks
fill ~6 ~13 ~22 ~6 ~13 ~23 minecraft:deepslate_bricks
fill ~6 ~13 ~24 ~6 ~14 ~24 minecraft:cracked_deepslate_bricks
fill ~6 ~13 ~25 ~6 ~13 ~29 minecraft:deepslate_bricks
fill ~6 ~13 ~30 ~6 ~13 ~34 minecraft:stone_bricks
setblock ~6 ~13 ~35 minecraft:cracked_stone_bricks
setblock ~0 ~14 ~11 minecraft:deepslate_bricks
setblock ~0 ~14 ~12 minecraft:stone_bricks
setblock ~0 ~14 ~16 minecraft:stone_bricks
setblock ~0 ~14 ~18 minecraft:deepslate_bricks
setblock ~0 ~14 ~20 minecraft:deepslate_bricks
setblock ~0 ~14 ~22 minecraft:deepslate_bricks
setblock ~0 ~14 ~24 minecraft:deepslate_bricks
setblock ~0 ~14 ~26 minecraft:stone_bricks
setblock ~0 ~14 ~28 minecraft:stone_bricks
setblock ~0 ~14 ~30 minecraft:cracked_stone_bricks
setblock ~1 ~14 ~11 minecraft:stone_bricks
setblock ~1 ~14 ~35 minecraft:deepslate_bricks
setblock ~3 ~14 ~11 minecraft:stone_bricks
setblock ~3 ~14 ~35 minecraft:cracked_stone_bricks
fill ~5 ~14 ~11 ~6 ~14 ~11 minecraft:deepslate_bricks
setblock ~5 ~14 ~35 minecraft:stone_bricks
setblock ~6 ~14 ~12 minecraft:cracked_deepslate_bricks
setblock ~6 ~14 ~14 minecraft:stone_bricks
setblock ~6 ~14 ~16 minecraft:stone_bricks
setblock ~6 ~14 ~18 minecraft:stone_bricks
setblock ~6 ~14 ~20 minecraft:stone_bricks
setblock ~6 ~14 ~22 minecraft:cracked_stone_bricks
setblock ~6 ~14 ~26 minecraft:deepslate_bricks
setblock ~6 ~14 ~28 minecraft:deepslate_bricks
setblock ~6 ~14 ~30 minecraft:stone_bricks
setblock ~6 ~14 ~32 minecraft:stone_bricks
setblock ~6 ~14 ~34 minecraft:stone_bricks
setblock ~6 ~14 ~35 minecraft:deepslate_bricks
setblock ~0 ~15 ~11 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~0 ~15 ~35 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~6 ~15 ~11 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~6 ~15 ~35 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~0 ~16 ~11 minecraft:lantern[hanging=false]
setblock ~0 ~16 ~35 minecraft:lantern[hanging=false]
setblock ~6 ~16 ~11 minecraft:lantern[hanging=false]
setblock ~6 ~16 ~35 minecraft:lantern[hanging=false]

# the winch of the portcullis on the deck: two dark oak posts with a log drum between them and a coil of chain
fill ~3 ~13 ~22 ~3 ~14 ~22 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~3 ~13 ~23 ~3 ~14 ~23 minecraft:chain[axis=y]
fill ~3 ~13 ~24 ~3 ~14 ~24 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~3 ~15 ~22 ~3 ~15 ~24 minecraft:dark_oak_log[axis=z]

# vines on the stub faces and moss on the stub feet
fill ~0 ~5 ~2 ~0 ~8 ~2 minecraft:vine[east=true]
fill ~0 ~5 ~9 ~0 ~7 ~9 minecraft:vine[east=true]
fill ~0 ~5 ~38 ~0 ~7 ~38 minecraft:vine[east=true]
fill ~0 ~5 ~42 ~0 ~7 ~42 minecraft:vine[east=true]
fill ~0 ~5 ~44 ~0 ~8 ~44 minecraft:vine[east=true]
setblock ~0 ~7 ~10 minecraft:vine[east=true]
