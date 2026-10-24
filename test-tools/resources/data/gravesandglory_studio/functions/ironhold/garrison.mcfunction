# size 31 15 23
# ironhold/garrison: garrison of Ironhold: a long two-storey stone barracks under a deepslate tile roof with a porch and chimneys, a walled yard with a gate to the lane, drill dummies, a weapon rack and a forge shed.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Yard x 0..30, z 0..11, wall y 1..4 (piers y 1..5); gate x 13..17 (clear y 1..4, beam y 5). Barracks body x 1..29, z 12..20; door x 14..16, y 1..3 (lintel y 4); back door (15, 1..2, 20); porch x 12..18, z 9..11.
# Inside: a hall y 1..4 (x 2..28, z 13..19), the upper storey y 6..7 on a floor at y 5; the stair at x 27, z 19 (y 1) up to z 15 (y 5), the floor open over x 27, z 16..19. The way from the door to the middle of the hall (x 14..16, z 12..16, y 1..3) is clear.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~12 ~0 ~8 ~14 minecraft:air
fill ~0 ~1 ~18 ~0 ~6 ~22 minecraft:air
fill ~1 ~1 ~1 ~12 ~14 ~2 minecraft:air
fill ~1 ~1 ~3 ~1 ~6 ~11 minecraft:air
fill ~1 ~1 ~21 ~2 ~6 ~22 minecraft:air
fill ~2 ~1 ~9 ~2 ~14 ~9 minecraft:air
fill ~2 ~1 ~13 ~2 ~4 ~19 minecraft:air
fill ~3 ~1 ~3 ~12 ~14 ~5 minecraft:air
fill ~3 ~1 ~6 ~4 ~1 ~9 minecraft:air
fill ~3 ~1 ~15 ~3 ~4 ~18 minecraft:air
fill ~3 ~1 ~22 ~5 ~14 ~22 minecraft:air
fill ~4 ~1 ~10 ~4 ~14 ~10 minecraft:air
fill ~4 ~1 ~14 ~4 ~4 ~18 minecraft:air
fill ~5 ~1 ~7 ~5 ~5 ~11 minecraft:air
fill ~5 ~1 ~15 ~5 ~4 ~18 minecraft:air
fill ~6 ~1 ~6 ~8 ~1 ~9 minecraft:air
fill ~6 ~1 ~10 ~7 ~5 ~11 minecraft:air
fill ~6 ~1 ~14 ~6 ~4 ~19 minecraft:air
fill ~6 ~1 ~21 ~6 ~6 ~22 minecraft:air
fill ~7 ~1 ~15 ~7 ~4 ~19 minecraft:air
fill ~7 ~1 ~22 ~7 ~14 ~22 minecraft:air
fill ~8 ~1 ~11 ~8 ~6 ~11 minecraft:air
fill ~8 ~1 ~13 ~22 ~3 ~16 minecraft:air
fill ~8 ~1 ~17 ~10 ~4 ~17 minecraft:air
fill ~8 ~1 ~18 ~9 ~4 ~19 minecraft:air
fill ~8 ~1 ~21 ~8 ~6 ~22 minecraft:air
fill ~9 ~1 ~7 ~9 ~14 ~9 minecraft:air
fill ~9 ~1 ~22 ~14 ~14 ~22 minecraft:air
fill ~10 ~1 ~6 ~12 ~1 ~8 minecraft:air
fill ~10 ~1 ~9 ~11 ~5 ~11 minecraft:air
fill ~10 ~1 ~19 ~12 ~4 ~19 minecraft:air
fill ~12 ~1 ~10 ~12 ~4 ~10 minecraft:air
fill ~13 ~1 ~0 ~17 ~3 ~11 minecraft:air
fill ~13 ~1 ~17 ~13 ~4 ~17 minecraft:air
fill ~14 ~1 ~19 ~16 ~4 ~19 minecraft:air
fill ~15 ~1 ~17 ~15 ~4 ~17 minecraft:air
fill ~15 ~1 ~21 ~15 ~6 ~22 minecraft:air
fill ~16 ~1 ~22 ~18 ~14 ~22 minecraft:air
fill ~17 ~1 ~17 ~17 ~4 ~17 minecraft:air
fill ~18 ~1 ~1 ~23 ~4 ~3 minecraft:air
fill ~18 ~1 ~4 ~22 ~14 ~5 minecraft:air
fill ~18 ~1 ~6 ~20 ~1 ~8 minecraft:air
fill ~18 ~1 ~10 ~18 ~4 ~10 minecraft:air
fill ~18 ~1 ~19 ~20 ~4 ~19 minecraft:air
fill ~19 ~1 ~9 ~20 ~5 ~11 minecraft:air
fill ~19 ~1 ~21 ~20 ~5 ~22 minecraft:air
fill ~20 ~1 ~17 ~22 ~4 ~17 minecraft:air
fill ~21 ~1 ~7 ~21 ~14 ~10 minecraft:air
fill ~21 ~1 ~18 ~22 ~4 ~19 minecraft:air
fill ~21 ~1 ~22 ~21 ~14 ~22 minecraft:air
fill ~22 ~1 ~6 ~22 ~1 ~11 minecraft:air
fill ~22 ~1 ~21 ~22 ~6 ~22 minecraft:air
fill ~23 ~1 ~5 ~23 ~5 ~11 minecraft:air
fill ~23 ~1 ~15 ~23 ~4 ~19 minecraft:air
fill ~23 ~1 ~22 ~24 ~14 ~22 minecraft:air
fill ~24 ~1 ~2 ~24 ~4 ~2 minecraft:air
fill ~24 ~1 ~4 ~24 ~1 ~11 minecraft:air
fill ~24 ~1 ~14 ~24 ~4 ~19 minecraft:air
fill ~25 ~1 ~1 ~27 ~4 ~1 minecraft:air
fill ~25 ~1 ~3 ~25 ~4 ~5 minecraft:air
fill ~25 ~1 ~7 ~25 ~5 ~11 minecraft:air
fill ~25 ~1 ~15 ~26 ~4 ~19 minecraft:air
fill ~25 ~1 ~21 ~25 ~5 ~22 minecraft:air
fill ~26 ~1 ~2 ~26 ~4 ~3 minecraft:air
fill ~26 ~1 ~5 ~26 ~1 ~10 minecraft:air
fill ~26 ~1 ~13 ~28 ~4 ~14 minecraft:air
fill ~26 ~1 ~22 ~27 ~14 ~22 minecraft:air
fill ~27 ~1 ~3 ~27 ~4 ~9 minecraft:air
fill ~27 ~1 ~11 ~28 ~6 ~11 minecraft:air
fill ~28 ~1 ~2 ~28 ~4 ~8 minecraft:air
fill ~28 ~1 ~15 ~28 ~4 ~19 minecraft:air
fill ~28 ~1 ~21 ~29 ~6 ~22 minecraft:air
fill ~29 ~1 ~1 ~29 ~4 ~2 minecraft:air
fill ~29 ~1 ~5 ~29 ~6 ~11 minecraft:air
fill ~30 ~1 ~12 ~30 ~8 ~14 minecraft:air
fill ~30 ~1 ~18 ~30 ~6 ~22 minecraft:air
fill ~2 ~2 ~4 ~2 ~14 ~4 minecraft:air
fill ~2 ~2 ~6 ~3 ~14 ~6 minecraft:air
fill ~2 ~2 ~8 ~2 ~14 ~8 minecraft:air
fill ~3 ~2 ~7 ~4 ~14 ~9 minecraft:air
fill ~3 ~2 ~13 ~7 ~4 ~13 minecraft:air
fill ~3 ~2 ~14 ~3 ~4 ~14 minecraft:air
fill ~3 ~2 ~19 ~5 ~4 ~19 minecraft:air
fill ~3 ~2 ~21 ~3 ~6 ~21 minecraft:air
fill ~5 ~2 ~14 ~5 ~4 ~14 minecraft:air
fill ~5 ~2 ~21 ~5 ~5 ~21 minecraft:air
fill ~6 ~2 ~7 ~6 ~14 ~9 minecraft:air
fill ~7 ~2 ~6 ~7 ~14 ~9 minecraft:air
fill ~7 ~2 ~14 ~7 ~4 ~14 minecraft:air
fill ~7 ~2 ~21 ~7 ~5 ~21 minecraft:air
fill ~8 ~2 ~7 ~8 ~14 ~10 minecraft:air
fill ~9 ~2 ~10 ~9 ~14 ~10 minecraft:air
fill ~10 ~2 ~7 ~10 ~14 ~8 minecraft:air
fill ~10 ~2 ~21 ~11 ~5 ~21 minecraft:air
fill ~11 ~2 ~6 ~12 ~14 ~8 minecraft:air
fill ~11 ~2 ~17 ~12 ~4 ~17 minecraft:air
fill ~13 ~2 ~19 ~13 ~4 ~19 minecraft:air
fill ~14 ~2 ~17 ~14 ~4 ~17 minecraft:air
fill ~14 ~2 ~21 ~14 ~6 ~21 minecraft:air
fill ~16 ~2 ~17 ~16 ~4 ~17 minecraft:air
fill ~16 ~2 ~21 ~18 ~5 ~21 minecraft:air
fill ~17 ~2 ~19 ~17 ~4 ~19 minecraft:air
fill ~18 ~2 ~6 ~19 ~14 ~8 minecraft:air
fill ~18 ~2 ~17 ~19 ~4 ~17 minecraft:air
fill ~20 ~2 ~7 ~20 ~14 ~8 minecraft:air
fill ~22 ~2 ~7 ~22 ~6 ~11 minecraft:air
fill ~23 ~2 ~13 ~25 ~4 ~13 minecraft:air
fill ~23 ~2 ~14 ~23 ~4 ~14 minecraft:air
fill ~23 ~2 ~21 ~24 ~5 ~21 minecraft:air
fill ~24 ~2 ~1 ~24 ~4 ~1 minecraft:air
fill ~24 ~2 ~3 ~24 ~4 ~5 minecraft:air
fill ~24 ~2 ~7 ~24 ~6 ~11 minecraft:air
fill ~25 ~2 ~2 ~25 ~4 ~2 minecraft:air
fill ~25 ~2 ~14 ~25 ~4 ~14 minecraft:air
fill ~26 ~2 ~5 ~26 ~14 ~5 minecraft:air
fill ~26 ~2 ~7 ~26 ~14 ~10 minecraft:air
fill ~27 ~2 ~2 ~27 ~4 ~2 minecraft:air
fill ~27 ~2 ~10 ~27 ~14 ~10 minecraft:air
fill ~27 ~2 ~19 ~27 ~9 ~19 minecraft:air
fill ~27 ~2 ~21 ~27 ~6 ~21 minecraft:air
fill ~28 ~2 ~1 ~28 ~4 ~1 minecraft:air
fill ~28 ~2 ~9 ~28 ~14 ~10 minecraft:air
fill ~2 ~3 ~3 ~2 ~14 ~3 minecraft:air
fill ~2 ~3 ~5 ~2 ~14 ~5 minecraft:air
fill ~2 ~3 ~7 ~2 ~14 ~7 minecraft:air
fill ~2 ~3 ~10 ~3 ~14 ~10 minecraft:air
fill ~3 ~3 ~11 ~3 ~6 ~11 minecraft:air
fill ~4 ~3 ~6 ~4 ~14 ~6 minecraft:air
fill ~6 ~3 ~6 ~6 ~14 ~6 minecraft:air
fill ~8 ~3 ~6 ~8 ~14 ~6 minecraft:air
fill ~10 ~3 ~6 ~10 ~14 ~6 minecraft:air
fill ~10 ~3 ~18 ~20 ~4 ~18 minecraft:air
fill ~12 ~3 ~21 ~13 ~5 ~21 minecraft:air
fill ~20 ~3 ~6 ~20 ~14 ~6 minecraft:air
fill ~22 ~3 ~6 ~22 ~14 ~6 minecraft:air
fill ~24 ~3 ~6 ~24 ~14 ~6 minecraft:air
fill ~26 ~3 ~6 ~26 ~14 ~6 minecraft:air
fill ~27 ~3 ~18 ~27 ~10 ~18 minecraft:air
fill ~29 ~3 ~3 ~29 ~4 ~3 minecraft:air
fill ~2 ~4 ~11 ~2 ~6 ~11 minecraft:air
fill ~5 ~4 ~6 ~5 ~14 ~6 minecraft:air
fill ~8 ~4 ~13 ~22 ~4 ~14 minecraft:air
fill ~8 ~4 ~15 ~10 ~4 ~16 minecraft:air
fill ~9 ~4 ~6 ~9 ~14 ~6 minecraft:air
fill ~11 ~4 ~16 ~14 ~4 ~16 minecraft:air
fill ~12 ~4 ~15 ~18 ~4 ~15 minecraft:air
fill ~13 ~4 ~0 ~17 ~4 ~9 minecraft:air
fill ~13 ~4 ~10 ~14 ~4 ~11 minecraft:air
setblock ~15 ~4 ~11 minecraft:air
fill ~16 ~4 ~10 ~17 ~4 ~11 minecraft:air
fill ~16 ~4 ~16 ~19 ~4 ~16 minecraft:air
fill ~20 ~4 ~15 ~22 ~4 ~16 minecraft:air
fill ~21 ~4 ~6 ~21 ~14 ~6 minecraft:air
fill ~25 ~4 ~6 ~25 ~14 ~6 minecraft:air
fill ~27 ~4 ~17 ~27 ~11 ~17 minecraft:air
fill ~0 ~5 ~1 ~0 ~14 ~4 minecraft:air
fill ~0 ~5 ~6 ~0 ~14 ~10 minecraft:air
fill ~1 ~5 ~0 ~5 ~14 ~0 minecraft:air
fill ~7 ~5 ~0 ~11 ~14 ~0 minecraft:air
fill ~13 ~5 ~1 ~22 ~14 ~3 minecraft:air
fill ~13 ~5 ~4 ~17 ~14 ~8 minecraft:air
fill ~19 ~5 ~0 ~23 ~14 ~0 minecraft:air
fill ~24 ~5 ~5 ~25 ~14 ~5 minecraft:air
fill ~25 ~5 ~0 ~29 ~14 ~0 minecraft:air
fill ~27 ~5 ~5 ~28 ~14 ~8 minecraft:air
fill ~27 ~5 ~9 ~27 ~14 ~9 minecraft:air
fill ~27 ~5 ~16 ~27 ~12 ~16 minecraft:air
fill ~30 ~5 ~1 ~30 ~14 ~4 minecraft:air
fill ~30 ~5 ~6 ~30 ~14 ~10 minecraft:air
fill ~2 ~6 ~13 ~2 ~9 ~19 minecraft:air
fill ~3 ~6 ~15 ~3 ~11 ~17 minecraft:air
fill ~4 ~6 ~14 ~4 ~10 ~18 minecraft:air
fill ~5 ~6 ~7 ~5 ~14 ~10 minecraft:air
fill ~5 ~6 ~15 ~5 ~11 ~17 minecraft:air
fill ~6 ~6 ~10 ~7 ~14 ~10 minecraft:air
setblock ~6 ~6 ~11 minecraft:air
fill ~6 ~6 ~13 ~6 ~9 ~19 minecraft:air
fill ~7 ~6 ~15 ~7 ~11 ~17 minecraft:air
fill ~8 ~6 ~14 ~8 ~10 ~18 minecraft:air
fill ~9 ~6 ~15 ~9 ~11 ~17 minecraft:air
fill ~10 ~6 ~9 ~20 ~14 ~10 minecraft:air
fill ~10 ~6 ~13 ~20 ~9 ~19 minecraft:air
setblock ~11 ~6 ~11 minecraft:air
setblock ~11 ~6 ~21 minecraft:air
fill ~13 ~6 ~0 ~17 ~14 ~0 minecraft:air
fill ~13 ~6 ~11 ~17 ~6 ~11 minecraft:air
setblock ~13 ~6 ~21 minecraft:air
fill ~16 ~6 ~21 ~17 ~6 ~21 minecraft:air
setblock ~19 ~6 ~11 minecraft:air
fill ~19 ~6 ~21 ~19 ~6 ~22 minecraft:air
fill ~20 ~6 ~22 ~20 ~14 ~22 minecraft:air
fill ~21 ~6 ~15 ~21 ~11 ~17 minecraft:air
fill ~22 ~6 ~14 ~22 ~10 ~18 minecraft:air
fill ~23 ~6 ~1 ~29 ~14 ~4 minecraft:air
fill ~23 ~6 ~5 ~23 ~14 ~10 minecraft:air
fill ~23 ~6 ~15 ~23 ~11 ~17 minecraft:air
fill ~24 ~6 ~13 ~24 ~9 ~19 minecraft:air
setblock ~24 ~6 ~21 minecraft:air
fill ~25 ~6 ~7 ~25 ~14 ~10 minecraft:air
fill ~25 ~6 ~15 ~25 ~9 ~19 minecraft:air
fill ~25 ~6 ~22 ~25 ~14 ~22 minecraft:air
fill ~26 ~6 ~13 ~28 ~9 ~15 minecraft:air
fill ~26 ~6 ~16 ~26 ~9 ~19 minecraft:air
fill ~28 ~6 ~16 ~28 ~9 ~19 minecraft:air
fill ~0 ~7 ~0 ~0 ~14 ~0 minecraft:air
fill ~0 ~7 ~5 ~0 ~14 ~5 minecraft:air
fill ~0 ~7 ~18 ~0 ~8 ~20 minecraft:air
fill ~0 ~7 ~22 ~2 ~14 ~22 minecraft:air
fill ~1 ~7 ~3 ~1 ~14 ~10 minecraft:air
fill ~3 ~7 ~13 ~3 ~9 ~14 minecraft:air
fill ~3 ~7 ~18 ~3 ~9 ~19 minecraft:air
fill ~4 ~7 ~19 ~4 ~9 ~19 minecraft:air
fill ~5 ~7 ~13 ~5 ~9 ~14 minecraft:air
fill ~5 ~7 ~18 ~5 ~9 ~19 minecraft:air
fill ~6 ~7 ~0 ~6 ~14 ~0 minecraft:air
fill ~6 ~7 ~22 ~6 ~14 ~22 minecraft:air
fill ~7 ~7 ~13 ~7 ~9 ~14 minecraft:air
fill ~7 ~7 ~18 ~7 ~9 ~19 minecraft:air
fill ~8 ~7 ~19 ~8 ~9 ~19 minecraft:air
fill ~8 ~7 ~22 ~8 ~14 ~22 minecraft:air
fill ~9 ~7 ~13 ~9 ~9 ~14 minecraft:air
fill ~9 ~7 ~18 ~9 ~9 ~19 minecraft:air
fill ~15 ~7 ~22 ~15 ~14 ~22 minecraft:air
fill ~19 ~7 ~22 ~19 ~14 ~22 minecraft:air
fill ~21 ~7 ~13 ~21 ~9 ~14 minecraft:air
fill ~21 ~7 ~18 ~21 ~9 ~19 minecraft:air
fill ~22 ~7 ~7 ~22 ~14 ~10 minecraft:air
fill ~22 ~7 ~19 ~22 ~9 ~19 minecraft:air
fill ~22 ~7 ~22 ~22 ~14 ~22 minecraft:air
fill ~23 ~7 ~13 ~23 ~9 ~14 minecraft:air
fill ~23 ~7 ~18 ~23 ~9 ~19 minecraft:air
fill ~24 ~7 ~0 ~24 ~14 ~0 minecraft:air
fill ~24 ~7 ~7 ~24 ~14 ~10 minecraft:air
fill ~25 ~7 ~13 ~25 ~9 ~14 minecraft:air
fill ~28 ~7 ~22 ~30 ~14 ~22 minecraft:air
fill ~29 ~7 ~5 ~30 ~14 ~5 minecraft:air
fill ~29 ~7 ~6 ~29 ~14 ~10 minecraft:air
fill ~30 ~7 ~0 ~30 ~14 ~0 minecraft:air
fill ~30 ~7 ~18 ~30 ~8 ~20 minecraft:air
fill ~4 ~8 ~13 ~4 ~9 ~13 minecraft:air
fill ~8 ~8 ~13 ~8 ~9 ~13 minecraft:air
fill ~12 ~8 ~0 ~12 ~14 ~0 minecraft:air
fill ~18 ~8 ~0 ~18 ~14 ~0 minecraft:air
fill ~22 ~8 ~13 ~22 ~9 ~13 minecraft:air
fill ~0 ~9 ~11 ~30 ~14 ~11 minecraft:air
fill ~0 ~9 ~13 ~0 ~9 ~14 minecraft:air
fill ~0 ~9 ~18 ~0 ~9 ~19 minecraft:air
fill ~0 ~9 ~21 ~30 ~14 ~21 minecraft:air
fill ~30 ~9 ~13 ~30 ~9 ~14 minecraft:air
fill ~30 ~9 ~18 ~30 ~9 ~19 minecraft:air
fill ~0 ~10 ~12 ~30 ~14 ~12 minecraft:air
setblock ~0 ~10 ~14 minecraft:air
setblock ~0 ~10 ~18 minecraft:air
fill ~0 ~10 ~20 ~30 ~14 ~20 minecraft:air
fill ~2 ~10 ~14 ~3 ~10 ~14 minecraft:air
fill ~2 ~10 ~15 ~2 ~10 ~18 minecraft:air
setblock ~3 ~10 ~18 minecraft:air
fill ~5 ~10 ~14 ~7 ~10 ~14 minecraft:air
setblock ~5 ~10 ~18 minecraft:air
fill ~6 ~10 ~15 ~6 ~10 ~18 minecraft:air
setblock ~7 ~10 ~18 minecraft:air
fill ~9 ~10 ~14 ~21 ~10 ~14 minecraft:air
setblock ~9 ~10 ~18 minecraft:air
fill ~10 ~10 ~15 ~20 ~10 ~18 minecraft:air
setblock ~21 ~10 ~18 minecraft:air
fill ~23 ~10 ~14 ~28 ~10 ~14 minecraft:air
setblock ~23 ~10 ~18 minecraft:air
fill ~24 ~10 ~15 ~28 ~11 ~15 minecraft:air
fill ~24 ~10 ~16 ~26 ~10 ~18 minecraft:air
fill ~28 ~10 ~16 ~28 ~10 ~18 minecraft:air
setblock ~30 ~10 ~14 minecraft:air
setblock ~30 ~10 ~18 minecraft:air
fill ~0 ~11 ~13 ~30 ~14 ~13 minecraft:air
fill ~0 ~11 ~19 ~30 ~14 ~19 minecraft:air
fill ~2 ~11 ~15 ~2 ~11 ~17 minecraft:air
fill ~4 ~11 ~15 ~4 ~11 ~17 minecraft:air
fill ~6 ~11 ~15 ~6 ~11 ~17 minecraft:air
fill ~8 ~11 ~15 ~8 ~11 ~17 minecraft:air
fill ~10 ~11 ~15 ~20 ~11 ~17 minecraft:air
fill ~22 ~11 ~15 ~22 ~11 ~17 minecraft:air
fill ~24 ~11 ~16 ~26 ~11 ~17 minecraft:air
fill ~28 ~11 ~16 ~28 ~11 ~17 minecraft:air
fill ~0 ~12 ~14 ~30 ~14 ~14 minecraft:air
fill ~0 ~12 ~18 ~30 ~14 ~18 minecraft:air
fill ~2 ~12 ~16 ~26 ~12 ~16 minecraft:air
setblock ~28 ~12 ~16 minecraft:air
fill ~0 ~13 ~15 ~30 ~14 ~15 minecraft:air
fill ~0 ~13 ~17 ~30 ~14 ~17 minecraft:air

# the ground: a trodden yard of cobble, gravel and packed mud with a flagged parade strip (x 13..17) from the gate to the porch; footings of cobble under the yard wall and the barracks walls; a flagged hall floor
setblock ~0 ~0 ~0 minecraft:cobblestone
setblock ~0 ~0 ~1 minecraft:mossy_cobblestone
fill ~0 ~0 ~2 ~0 ~0 ~3 minecraft:cobblestone
setblock ~0 ~0 ~4 minecraft:mossy_cobblestone
setblock ~0 ~0 ~5 minecraft:cobblestone
fill ~0 ~0 ~6 ~1 ~0 ~6 minecraft:mossy_cobblestone
fill ~0 ~0 ~7 ~0 ~0 ~9 minecraft:cobblestone
setblock ~0 ~0 ~10 minecraft:andesite
fill ~0 ~0 ~11 ~1 ~0 ~11 minecraft:cobblestone
setblock ~1 ~0 ~0 minecraft:andesite
setblock ~1 ~0 ~1 minecraft:coarse_dirt
fill ~1 ~0 ~2 ~1 ~0 ~3 minecraft:packed_mud
setblock ~1 ~0 ~4 minecraft:gravel
setblock ~1 ~0 ~5 minecraft:coarse_dirt
setblock ~1 ~0 ~7 minecraft:coarse_dirt
fill ~1 ~0 ~8 ~2 ~0 ~8 minecraft:cobblestone
fill ~1 ~0 ~9 ~2 ~0 ~9 minecraft:mossy_cobblestone
setblock ~1 ~0 ~10 minecraft:mossy_cobblestone
setblock ~1 ~0 ~12 minecraft:andesite
fill ~1 ~0 ~13 ~1 ~0 ~14 minecraft:cobblestone
fill ~1 ~0 ~15 ~1 ~0 ~17 minecraft:mossy_cobblestone
fill ~1 ~0 ~18 ~2 ~0 ~18 minecraft:cobblestone
setblock ~1 ~0 ~19 minecraft:cobblestone
setblock ~1 ~0 ~20 minecraft:mossy_cobblestone
setblock ~2 ~0 ~0 minecraft:cobblestone
setblock ~2 ~0 ~1 minecraft:gravel
setblock ~2 ~0 ~2 minecraft:cobblestone
fill ~2 ~0 ~3 ~3 ~0 ~3 minecraft:packed_mud
fill ~2 ~0 ~4 ~3 ~0 ~4 minecraft:coarse_dirt
setblock ~2 ~0 ~5 minecraft:packed_mud
setblock ~2 ~0 ~6 minecraft:gravel
setblock ~2 ~0 ~7 minecraft:packed_mud
setblock ~2 ~0 ~10 minecraft:cobblestone
setblock ~2 ~0 ~11 minecraft:packed_mud
setblock ~2 ~0 ~12 minecraft:cobblestone
fill ~2 ~0 ~13 ~3 ~0 ~13 minecraft:stone_bricks
setblock ~2 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~2 ~0 ~15 minecraft:stone_bricks
setblock ~2 ~0 ~16 minecraft:cracked_stone_bricks
setblock ~2 ~0 ~17 minecraft:mossy_stone_bricks
setblock ~2 ~0 ~19 minecraft:mossy_stone_bricks
setblock ~2 ~0 ~20 minecraft:cobblestone
fill ~3 ~0 ~0 ~7 ~0 ~0 minecraft:mossy_cobblestone
setblock ~3 ~0 ~1 minecraft:mossy_cobblestone
setblock ~3 ~0 ~2 minecraft:gravel
fill ~3 ~0 ~5 ~3 ~0 ~6 minecraft:mossy_cobblestone
setblock ~3 ~0 ~7 minecraft:gravel
setblock ~3 ~0 ~8 minecraft:mossy_cobblestone
fill ~3 ~0 ~9 ~3 ~0 ~10 minecraft:packed_mud
fill ~3 ~0 ~11 ~3 ~0 ~12 minecraft:cobblestone
fill ~3 ~0 ~14 ~3 ~0 ~15 minecraft:stone_bricks
fill ~3 ~0 ~16 ~4 ~0 ~16 minecraft:andesite
setblock ~3 ~0 ~17 minecraft:andesite
setblock ~3 ~0 ~18 minecraft:mossy_stone_bricks
setblock ~3 ~0 ~19 minecraft:cobblestone
setblock ~3 ~0 ~20 minecraft:mossy_cobblestone
setblock ~4 ~0 ~1 minecraft:gravel
setblock ~4 ~0 ~2 minecraft:mossy_cobblestone
fill ~4 ~0 ~3 ~5 ~0 ~3 minecraft:cobblestone
setblock ~4 ~0 ~4 minecraft:cobblestone
setblock ~4 ~0 ~5 minecraft:coarse_dirt
setblock ~4 ~0 ~6 minecraft:packed_mud
setblock ~4 ~0 ~7 minecraft:cobblestone
fill ~4 ~0 ~8 ~4 ~0 ~9 minecraft:coarse_dirt
fill ~4 ~0 ~10 ~6 ~0 ~10 minecraft:packed_mud
setblock ~4 ~0 ~11 minecraft:packed_mud
fill ~4 ~0 ~12 ~6 ~0 ~12 minecraft:cobblestone
setblock ~4 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~4 ~0 ~14 minecraft:andesite
setblock ~4 ~0 ~15 minecraft:stone_bricks
setblock ~4 ~0 ~17 minecraft:stone_bricks
setblock ~4 ~0 ~18 minecraft:cobblestone
setblock ~4 ~0 ~19 minecraft:cracked_stone_bricks
fill ~4 ~0 ~20 ~7 ~0 ~20 minecraft:cobblestone
setblock ~5 ~0 ~1 minecraft:mossy_cobblestone
setblock ~5 ~0 ~2 minecraft:gravel
setblock ~5 ~0 ~4 minecraft:mossy_cobblestone
setblock ~5 ~0 ~5 minecraft:gravel
fill ~5 ~0 ~6 ~5 ~0 ~7 minecraft:cobblestone
setblock ~5 ~0 ~8 minecraft:packed_mud
fill ~5 ~0 ~9 ~7 ~0 ~9 minecraft:coarse_dirt
setblock ~5 ~0 ~11 minecraft:gravel
setblock ~5 ~0 ~13 minecraft:andesite
setblock ~5 ~0 ~14 minecraft:stone_bricks
setblock ~5 ~0 ~15 minecraft:cobblestone
setblock ~5 ~0 ~16 minecraft:stone_bricks
setblock ~5 ~0 ~17 minecraft:mossy_stone_bricks
fill ~5 ~0 ~18 ~5 ~0 ~19 minecraft:stone_bricks
setblock ~6 ~0 ~1 minecraft:cobblestone
setblock ~6 ~0 ~2 minecraft:packed_mud
setblock ~6 ~0 ~3 minecraft:gravel
setblock ~6 ~0 ~4 minecraft:packed_mud
setblock ~6 ~0 ~5 minecraft:cobblestone
setblock ~6 ~0 ~6 minecraft:coarse_dirt
fill ~6 ~0 ~7 ~7 ~0 ~7 minecraft:cobblestone
setblock ~6 ~0 ~8 minecraft:mossy_cobblestone
fill ~6 ~0 ~11 ~7 ~0 ~11 minecraft:coarse_dirt
fill ~6 ~0 ~13 ~6 ~0 ~14 minecraft:stone_bricks
fill ~6 ~0 ~15 ~8 ~0 ~15 minecraft:mossy_stone_bricks
fill ~6 ~0 ~16 ~6 ~0 ~18 minecraft:cobblestone
setblock ~6 ~0 ~19 minecraft:andesite
setblock ~7 ~0 ~1 minecraft:gravel
fill ~7 ~0 ~2 ~7 ~0 ~3 minecraft:cobblestone
setblock ~7 ~0 ~4 minecraft:gravel
fill ~7 ~0 ~5 ~7 ~0 ~6 minecraft:coarse_dirt
setblock ~7 ~0 ~8 minecraft:gravel
setblock ~7 ~0 ~10 minecraft:mossy_cobblestone
fill ~7 ~0 ~12 ~8 ~0 ~12 minecraft:mossy_cobblestone
setblock ~7 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~7 ~0 ~14 minecraft:stone_bricks
setblock ~7 ~0 ~16 minecraft:stone_bricks
setblock ~7 ~0 ~17 minecraft:cracked_stone_bricks
fill ~7 ~0 ~18 ~7 ~0 ~19 minecraft:stone_bricks
fill ~8 ~0 ~0 ~8 ~0 ~2 minecraft:cobblestone
fill ~8 ~0 ~3 ~8 ~0 ~4 minecraft:coarse_dirt
setblock ~8 ~0 ~5 minecraft:cobblestone
fill ~8 ~0 ~6 ~8 ~0 ~7 minecraft:packed_mud
setblock ~8 ~0 ~8 minecraft:cobblestone
setblock ~8 ~0 ~9 minecraft:gravel
setblock ~8 ~0 ~10 minecraft:cobblestone
fill ~8 ~0 ~11 ~10 ~0 ~11 minecraft:packed_mud
fill ~8 ~0 ~13 ~8 ~0 ~14 minecraft:cobblestone
setblock ~8 ~0 ~16 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~17 minecraft:cobblestone
setblock ~8 ~0 ~18 minecraft:cracked_stone_bricks
fill ~8 ~0 ~19 ~8 ~0 ~20 minecraft:cobblestone
setblock ~9 ~0 ~0 minecraft:andesite
fill ~9 ~0 ~1 ~9 ~0 ~3 minecraft:gravel
setblock ~9 ~0 ~4 minecraft:cobblestone
setblock ~9 ~0 ~5 minecraft:mossy_cobblestone
setblock ~9 ~0 ~6 minecraft:gravel
setblock ~9 ~0 ~7 minecraft:packed_mud
setblock ~9 ~0 ~8 minecraft:mossy_cobblestone
setblock ~9 ~0 ~9 minecraft:cobblestone
setblock ~9 ~0 ~10 minecraft:coarse_dirt
setblock ~9 ~0 ~12 minecraft:cobblestone
fill ~9 ~0 ~13 ~9 ~0 ~14 minecraft:stone_bricks
fill ~9 ~0 ~15 ~10 ~0 ~15 minecraft:cracked_stone_bricks
fill ~9 ~0 ~16 ~9 ~0 ~19 minecraft:stone_bricks
setblock ~9 ~0 ~20 minecraft:cobblestone
fill ~10 ~0 ~0 ~11 ~0 ~0 minecraft:cobblestone
setblock ~10 ~0 ~1 minecraft:coarse_dirt
fill ~10 ~0 ~2 ~10 ~0 ~3 minecraft:cobblestone
setblock ~10 ~0 ~4 minecraft:packed_mud
fill ~10 ~0 ~5 ~11 ~0 ~5 minecraft:cobblestone
fill ~10 ~0 ~6 ~10 ~0 ~7 minecraft:coarse_dirt
fill ~10 ~0 ~8 ~11 ~0 ~8 minecraft:packed_mud
fill ~10 ~0 ~9 ~11 ~0 ~9 minecraft:gravel
setblock ~10 ~0 ~10 minecraft:gravel
setblock ~10 ~0 ~12 minecraft:mossy_cobblestone
setblock ~10 ~0 ~13 minecraft:cobblestone
setblock ~10 ~0 ~14 minecraft:stone_bricks
setblock ~10 ~0 ~16 minecraft:andesite
setblock ~10 ~0 ~17 minecraft:cracked_stone_bricks
setblock ~10 ~0 ~18 minecraft:stone_bricks
fill ~10 ~0 ~19 ~10 ~0 ~20 minecraft:cobblestone
setblock ~11 ~0 ~1 minecraft:packed_mud
setblock ~11 ~0 ~2 minecraft:coarse_dirt
setblock ~11 ~0 ~3 minecraft:cobblestone
setblock ~11 ~0 ~4 minecraft:gravel
setblock ~11 ~0 ~6 minecraft:cobblestone
setblock ~11 ~0 ~7 minecraft:coarse_dirt
setblock ~11 ~0 ~10 minecraft:cobblestone
setblock ~11 ~0 ~11 minecraft:gravel
fill ~11 ~0 ~12 ~12 ~0 ~12 minecraft:cobblestone
fill ~11 ~0 ~13 ~11 ~0 ~14 minecraft:cracked_stone_bricks
fill ~11 ~0 ~15 ~11 ~0 ~16 minecraft:stone_bricks
setblock ~11 ~0 ~17 minecraft:cobblestone
fill ~11 ~0 ~18 ~11 ~0 ~19 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~20 minecraft:andesite
setblock ~12 ~0 ~0 minecraft:mossy_cobblestone
fill ~12 ~0 ~1 ~12 ~0 ~2 minecraft:cobblestone
fill ~12 ~0 ~3 ~12 ~0 ~4 minecraft:gravel
setblock ~12 ~0 ~5 minecraft:packed_mud
setblock ~12 ~0 ~6 minecraft:mossy_cobblestone
setblock ~12 ~0 ~7 minecraft:cobblestone
setblock ~12 ~0 ~8 minecraft:coarse_dirt
setblock ~12 ~0 ~9 minecraft:stone_bricks
setblock ~12 ~0 ~10 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~11 minecraft:mossy_stone_bricks
fill ~12 ~0 ~13 ~12 ~0 ~14 minecraft:stone_bricks
setblock ~12 ~0 ~15 minecraft:andesite
setblock ~12 ~0 ~16 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~17 minecraft:andesite
setblock ~12 ~0 ~18 minecraft:mossy_stone_bricks
setblock ~12 ~0 ~19 minecraft:stone_bricks
setblock ~12 ~0 ~20 minecraft:cobblestone
setblock ~13 ~0 ~0 minecraft:cobblestone
fill ~13 ~0 ~1 ~13 ~0 ~2 minecraft:stone_bricks
setblock ~13 ~0 ~3 minecraft:cobblestone
setblock ~13 ~0 ~4 minecraft:stone_bricks
setblock ~13 ~0 ~5 minecraft:cobblestone
fill ~13 ~0 ~6 ~14 ~0 ~6 minecraft:stone_bricks
fill ~13 ~0 ~7 ~13 ~0 ~9 minecraft:stone_bricks
setblock ~13 ~0 ~10 minecraft:mossy_stone_bricks
setblock ~13 ~0 ~11 minecraft:cracked_stone_bricks
fill ~13 ~0 ~12 ~13 ~0 ~14 minecraft:andesite
fill ~13 ~0 ~15 ~13 ~0 ~16 minecraft:cracked_stone_bricks
fill ~13 ~0 ~17 ~13 ~0 ~19 minecraft:stone_bricks
setblock ~13 ~0 ~20 minecraft:mossy_cobblestone
fill ~14 ~0 ~0 ~17 ~0 ~0 minecraft:stone_bricks
fill ~14 ~0 ~1 ~14 ~0 ~3 minecraft:cracked_stone_bricks
setblock ~14 ~0 ~4 minecraft:cobblestone
setblock ~14 ~0 ~5 minecraft:cracked_stone_bricks
setblock ~14 ~0 ~7 minecraft:cracked_stone_bricks
setblock ~14 ~0 ~8 minecraft:stone_bricks
setblock ~14 ~0 ~9 minecraft:cracked_stone_bricks
fill ~14 ~0 ~10 ~14 ~0 ~11 minecraft:stone_bricks
setblock ~14 ~0 ~12 minecraft:mossy_cobblestone
fill ~14 ~0 ~13 ~14 ~0 ~14 minecraft:cracked_stone_bricks
fill ~14 ~0 ~15 ~14 ~0 ~16 minecraft:stone_bricks
setblock ~14 ~0 ~17 minecraft:andesite
setblock ~14 ~0 ~18 minecraft:stone_bricks
fill ~14 ~0 ~19 ~15 ~0 ~19 minecraft:cracked_stone_bricks
fill ~14 ~0 ~20 ~16 ~0 ~20 minecraft:cobblestone
fill ~15 ~0 ~1 ~17 ~0 ~1 minecraft:stone_bricks
setblock ~15 ~0 ~2 minecraft:mossy_stone_bricks
setblock ~15 ~0 ~3 minecraft:cracked_stone_bricks
fill ~15 ~0 ~4 ~15 ~0 ~5 minecraft:stone_bricks
setblock ~15 ~0 ~6 minecraft:mossy_stone_bricks
setblock ~15 ~0 ~7 minecraft:stone_bricks
setblock ~15 ~0 ~8 minecraft:mossy_stone_bricks
fill ~15 ~0 ~9 ~15 ~0 ~10 minecraft:stone_bricks
fill ~15 ~0 ~11 ~15 ~0 ~12 minecraft:cobblestone
setblock ~15 ~0 ~13 minecraft:andesite
setblock ~15 ~0 ~14 minecraft:stone_bricks
setblock ~15 ~0 ~15 minecraft:andesite
setblock ~15 ~0 ~16 minecraft:cobblestone
fill ~15 ~0 ~17 ~15 ~0 ~18 minecraft:stone_bricks
fill ~16 ~0 ~2 ~16 ~0 ~3 minecraft:stone_bricks
fill ~16 ~0 ~4 ~17 ~0 ~4 minecraft:mossy_stone_bricks
fill ~16 ~0 ~5 ~17 ~0 ~5 minecraft:stone_bricks
fill ~16 ~0 ~6 ~16 ~0 ~8 minecraft:stone_bricks
setblock ~16 ~0 ~9 minecraft:cracked_stone_bricks
fill ~16 ~0 ~10 ~17 ~0 ~10 minecraft:stone_bricks
setblock ~16 ~0 ~11 minecraft:mossy_stone_bricks
setblock ~16 ~0 ~12 minecraft:cobblestone
fill ~16 ~0 ~13 ~16 ~0 ~14 minecraft:cracked_stone_bricks
fill ~16 ~0 ~15 ~16 ~0 ~16 minecraft:mossy_stone_bricks
setblock ~16 ~0 ~17 minecraft:cobblestone
setblock ~16 ~0 ~18 minecraft:stone_bricks
setblock ~16 ~0 ~19 minecraft:mossy_stone_bricks
setblock ~17 ~0 ~2 minecraft:cobblestone
setblock ~17 ~0 ~3 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~6 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~7 minecraft:stone_bricks
setblock ~17 ~0 ~8 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~9 minecraft:cobblestone
setblock ~17 ~0 ~11 minecraft:stone_bricks
fill ~17 ~0 ~12 ~18 ~0 ~12 minecraft:mossy_cobblestone
setblock ~17 ~0 ~13 minecraft:andesite
setblock ~17 ~0 ~14 minecraft:cobblestone
setblock ~17 ~0 ~15 minecraft:stone_bricks
setblock ~17 ~0 ~16 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~17 minecraft:stone_bricks
setblock ~17 ~0 ~18 minecraft:cobblestone
fill ~17 ~0 ~19 ~18 ~0 ~19 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~20 minecraft:mossy_cobblestone
setblock ~18 ~0 ~0 minecraft:mossy_cobblestone
setblock ~18 ~0 ~1 minecraft:packed_mud
fill ~18 ~0 ~2 ~18 ~0 ~3 minecraft:gravel
fill ~18 ~0 ~4 ~20 ~0 ~4 minecraft:cobblestone
fill ~18 ~0 ~5 ~19 ~0 ~5 minecraft:mossy_cobblestone
setblock ~18 ~0 ~6 minecraft:cobblestone
setblock ~18 ~0 ~7 minecraft:coarse_dirt
setblock ~18 ~0 ~8 minecraft:gravel
fill ~18 ~0 ~9 ~18 ~0 ~10 minecraft:cracked_stone_bricks
setblock ~18 ~0 ~11 minecraft:mossy_stone_bricks
setblock ~18 ~0 ~13 minecraft:cobblestone
setblock ~18 ~0 ~14 minecraft:andesite
setblock ~18 ~0 ~15 minecraft:cobblestone
setblock ~18 ~0 ~16 minecraft:mossy_stone_bricks
fill ~18 ~0 ~17 ~19 ~0 ~17 minecraft:cracked_stone_bricks
setblock ~18 ~0 ~18 minecraft:stone_bricks
fill ~18 ~0 ~20 ~19 ~0 ~20 minecraft:cobblestone
setblock ~19 ~0 ~0 minecraft:andesite
fill ~19 ~0 ~1 ~20 ~0 ~1 minecraft:cobblestone
fill ~19 ~0 ~2 ~20 ~0 ~3 minecraft:packed_mud
setblock ~19 ~0 ~6 minecraft:gravel
fill ~19 ~0 ~7 ~20 ~0 ~7 minecraft:cobblestone
setblock ~19 ~0 ~8 minecraft:mossy_cobblestone
setblock ~19 ~0 ~9 minecraft:coarse_dirt
fill ~19 ~0 ~10 ~20 ~0 ~10 minecraft:cobblestone
fill ~19 ~0 ~11 ~20 ~0 ~11 minecraft:gravel
setblock ~19 ~0 ~12 minecraft:cobblestone
setblock ~19 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~19 ~0 ~14 minecraft:mossy_stone_bricks
setblock ~19 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~19 ~0 ~16 minecraft:andesite
fill ~19 ~0 ~18 ~19 ~0 ~19 minecraft:cracked_stone_bricks
setblock ~20 ~0 ~0 minecraft:mossy_cobblestone
setblock ~20 ~0 ~5 minecraft:coarse_dirt
setblock ~20 ~0 ~6 minecraft:packed_mud
setblock ~20 ~0 ~8 minecraft:coarse_dirt
fill ~20 ~0 ~9 ~21 ~0 ~9 minecraft:cobblestone
fill ~20 ~0 ~12 ~21 ~0 ~12 minecraft:mossy_cobblestone
setblock ~20 ~0 ~13 minecraft:stone_bricks
setblock ~20 ~0 ~14 minecraft:andesite
fill ~20 ~0 ~15 ~22 ~0 ~15 minecraft:mossy_stone_bricks
setblock ~20 ~0 ~16 minecraft:cobblestone
fill ~20 ~0 ~17 ~20 ~0 ~18 minecraft:stone_bricks
setblock ~20 ~0 ~19 minecraft:cracked_stone_bricks
setblock ~20 ~0 ~20 minecraft:mossy_cobblestone
fill ~21 ~0 ~0 ~23 ~0 ~1 minecraft:cobblestone
setblock ~21 ~0 ~2 minecraft:cobblestone
fill ~21 ~0 ~3 ~22 ~0 ~3 minecraft:gravel
fill ~21 ~0 ~4 ~23 ~0 ~4 minecraft:mossy_cobblestone
setblock ~21 ~0 ~5 minecraft:mossy_cobblestone
setblock ~21 ~0 ~6 minecraft:cobblestone
setblock ~21 ~0 ~7 minecraft:packed_mud
setblock ~21 ~0 ~8 minecraft:mossy_cobblestone
setblock ~21 ~0 ~10 minecraft:gravel
setblock ~21 ~0 ~11 minecraft:coarse_dirt
setblock ~21 ~0 ~13 minecraft:andesite
setblock ~21 ~0 ~14 minecraft:stone_bricks
fill ~21 ~0 ~16 ~26 ~0 ~16 minecraft:stone_bricks
setblock ~21 ~0 ~17 minecraft:andesite
setblock ~21 ~0 ~18 minecraft:cracked_stone_bricks
setblock ~21 ~0 ~19 minecraft:mossy_stone_bricks
setblock ~21 ~0 ~20 minecraft:andesite
setblock ~22 ~0 ~2 minecraft:coarse_dirt
setblock ~22 ~0 ~5 minecraft:cobblestone
fill ~22 ~0 ~6 ~22 ~0 ~7 minecraft:packed_mud
setblock ~22 ~0 ~8 minecraft:coarse_dirt
setblock ~22 ~0 ~9 minecraft:gravel
fill ~22 ~0 ~10 ~22 ~0 ~11 minecraft:packed_mud
fill ~22 ~0 ~12 ~24 ~0 ~12 minecraft:cobblestone
setblock ~22 ~0 ~13 minecraft:cobblestone
setblock ~22 ~0 ~14 minecraft:cracked_stone_bricks
fill ~22 ~0 ~17 ~22 ~0 ~18 minecraft:cracked_stone_bricks
setblock ~22 ~0 ~19 minecraft:stone_bricks
fill ~22 ~0 ~20 ~23 ~0 ~20 minecraft:cobblestone
setblock ~23 ~0 ~2 minecraft:mossy_cobblestone
setblock ~23 ~0 ~3 minecraft:cobblestone
setblock ~23 ~0 ~5 minecraft:mossy_cobblestone
setblock ~23 ~0 ~6 minecraft:coarse_dirt
fill ~23 ~0 ~7 ~24 ~0 ~7 minecraft:packed_mud
fill ~23 ~0 ~8 ~23 ~0 ~10 minecraft:packed_mud
fill ~23 ~0 ~11 ~24 ~0 ~11 minecraft:coarse_dirt
fill ~23 ~0 ~13 ~23 ~0 ~14 minecraft:stone_bricks
fill ~23 ~0 ~15 ~24 ~0 ~15 minecraft:cobblestone
fill ~23 ~0 ~17 ~24 ~0 ~17 minecraft:mossy_stone_bricks
fill ~23 ~0 ~18 ~24 ~0 ~18 minecraft:stone_bricks
fill ~23 ~0 ~19 ~24 ~0 ~19 minecraft:cracked_stone_bricks
setblock ~24 ~0 ~0 minecraft:mossy_cobblestone
setblock ~24 ~0 ~1 minecraft:coarse_dirt
fill ~24 ~0 ~2 ~24 ~0 ~3 minecraft:gravel
setblock ~24 ~0 ~4 minecraft:coarse_dirt
fill ~24 ~0 ~5 ~24 ~0 ~6 minecraft:gravel
setblock ~24 ~0 ~8 minecraft:coarse_dirt
setblock ~24 ~0 ~9 minecraft:gravel
setblock ~24 ~0 ~10 minecraft:packed_mud
setblock ~24 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~24 ~0 ~14 minecraft:andesite
fill ~24 ~0 ~20 ~25 ~0 ~20 minecraft:mossy_cobblestone
setblock ~25 ~0 ~0 minecraft:andesite
setblock ~25 ~0 ~1 minecraft:gravel
fill ~25 ~0 ~2 ~25 ~0 ~6 minecraft:cobblestone
setblock ~25 ~0 ~7 minecraft:mossy_cobblestone
setblock ~25 ~0 ~8 minecraft:packed_mud
setblock ~25 ~0 ~9 minecraft:coarse_dirt
setblock ~25 ~0 ~10 minecraft:gravel
setblock ~25 ~0 ~11 minecraft:cobblestone
setblock ~25 ~0 ~12 minecraft:andesite
fill ~25 ~0 ~13 ~25 ~0 ~14 minecraft:stone_bricks
setblock ~25 ~0 ~15 minecraft:mossy_stone_bricks
setblock ~25 ~0 ~17 minecraft:stone_bricks
setblock ~25 ~0 ~18 minecraft:cobblestone
setblock ~25 ~0 ~19 minecraft:stone_bricks
fill ~26 ~0 ~0 ~28 ~0 ~0 minecraft:cobblestone
setblock ~26 ~0 ~1 minecraft:cobblestone
fill ~26 ~0 ~2 ~26 ~0 ~3 minecraft:gravel
setblock ~26 ~0 ~4 minecraft:cobblestone
setblock ~26 ~0 ~5 minecraft:coarse_dirt
fill ~26 ~0 ~6 ~26 ~0 ~7 minecraft:cobblestone
setblock ~26 ~0 ~8 minecraft:coarse_dirt
fill ~26 ~0 ~9 ~26 ~0 ~10 minecraft:gravel
fill ~26 ~0 ~11 ~27 ~0 ~11 minecraft:packed_mud
setblock ~26 ~0 ~12 minecraft:mossy_cobblestone
setblock ~26 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~26 ~0 ~14 minecraft:andesite
setblock ~26 ~0 ~15 minecraft:cobblestone
setblock ~26 ~0 ~17 minecraft:cracked_stone_bricks
setblock ~26 ~0 ~18 minecraft:stone_bricks
fill ~26 ~0 ~19 ~26 ~0 ~20 minecraft:cobblestone
fill ~27 ~0 ~1 ~27 ~0 ~2 minecraft:packed_mud
setblock ~27 ~0 ~3 minecraft:cobblestone
fill ~27 ~0 ~4 ~28 ~0 ~4 minecraft:coarse_dirt
setblock ~27 ~0 ~5 minecraft:gravel
setblock ~27 ~0 ~6 minecraft:mossy_cobblestone
setblock ~27 ~0 ~7 minecraft:coarse_dirt
setblock ~27 ~0 ~8 minecraft:gravel
setblock ~27 ~0 ~9 minecraft:packed_mud
setblock ~27 ~0 ~10 minecraft:coarse_dirt
fill ~27 ~0 ~12 ~27 ~0 ~13 minecraft:cobblestone
fill ~27 ~0 ~14 ~27 ~0 ~15 minecraft:stone_bricks
setblock ~27 ~0 ~16 minecraft:cobblestone
fill ~27 ~0 ~17 ~27 ~0 ~19 minecraft:stone_bricks
setblock ~27 ~0 ~20 minecraft:mossy_cobblestone
setblock ~28 ~0 ~1 minecraft:mossy_cobblestone
fill ~28 ~0 ~2 ~29 ~0 ~2 minecraft:coarse_dirt
setblock ~28 ~0 ~3 minecraft:gravel
fill ~28 ~0 ~5 ~29 ~0 ~5 minecraft:coarse_dirt
fill ~28 ~0 ~6 ~29 ~0 ~6 minecraft:cobblestone
setblock ~28 ~0 ~7 minecraft:cobblestone
setblock ~28 ~0 ~8 minecraft:packed_mud
setblock ~28 ~0 ~9 minecraft:mossy_cobblestone
fill ~28 ~0 ~10 ~28 ~0 ~12 minecraft:cobblestone
setblock ~28 ~0 ~13 minecraft:stone_bricks
fill ~28 ~0 ~14 ~29 ~0 ~14 minecraft:cobblestone
setblock ~28 ~0 ~15 minecraft:andesite
setblock ~28 ~0 ~16 minecraft:cracked_stone_bricks
setblock ~28 ~0 ~17 minecraft:cobblestone
setblock ~28 ~0 ~18 minecraft:stone_bricks
fill ~28 ~0 ~19 ~28 ~0 ~20 minecraft:cobblestone
setblock ~29 ~0 ~0 minecraft:andesite
setblock ~29 ~0 ~1 minecraft:cobblestone
fill ~29 ~0 ~3 ~30 ~0 ~3 minecraft:cobblestone
setblock ~29 ~0 ~4 minecraft:gravel
fill ~29 ~0 ~7 ~30 ~0 ~7 minecraft:mossy_cobblestone
fill ~29 ~0 ~8 ~30 ~0 ~9 minecraft:cobblestone
setblock ~29 ~0 ~10 minecraft:gravel
setblock ~29 ~0 ~11 minecraft:cobblestone
fill ~29 ~0 ~12 ~29 ~0 ~13 minecraft:mossy_cobblestone
fill ~29 ~0 ~15 ~29 ~0 ~19 minecraft:cobblestone
setblock ~29 ~0 ~20 minecraft:mossy_cobblestone
fill ~30 ~0 ~0 ~30 ~0 ~1 minecraft:cobblestone
setblock ~30 ~0 ~2 minecraft:andesite
setblock ~30 ~0 ~4 minecraft:mossy_cobblestone
fill ~30 ~0 ~5 ~30 ~0 ~6 minecraft:cobblestone
setblock ~30 ~0 ~10 minecraft:cobblestone
setblock ~30 ~0 ~11 minecraft:mossy_cobblestone

# the barracks walls: dark patched stone y 1..8, damp and moss at the foot, a dark course at floor level (y 5) and under the eaves (y 8)
fill ~1 ~1 ~12 ~2 ~1 ~12 minecraft:mossy_stone_bricks
fill ~1 ~1 ~13 ~1 ~1 ~20 minecraft:mossy_stone_bricks
fill ~2 ~1 ~20 ~6 ~1 ~20 minecraft:mossy_stone_bricks
fill ~3 ~1 ~12 ~4 ~1 ~12 minecraft:stone_bricks
setblock ~5 ~1 ~12 minecraft:cracked_stone_bricks
fill ~6 ~1 ~12 ~8 ~1 ~12 minecraft:stone_bricks
setblock ~7 ~1 ~20 minecraft:deepslate_bricks
fill ~8 ~1 ~20 ~10 ~1 ~20 minecraft:stone_bricks
setblock ~9 ~1 ~12 minecraft:mossy_stone_bricks
setblock ~10 ~1 ~12 minecraft:cracked_deepslate_bricks
setblock ~11 ~1 ~12 minecraft:deepslate_bricks
setblock ~11 ~1 ~20 minecraft:cracked_stone_bricks
fill ~12 ~1 ~12 ~12 ~3 ~12 minecraft:stone_bricks
fill ~12 ~1 ~20 ~12 ~2 ~20 minecraft:stone_bricks
setblock ~13 ~1 ~20 minecraft:mossy_stone_bricks
setblock ~14 ~1 ~20 minecraft:cracked_stone_bricks
setblock ~16 ~1 ~20 minecraft:deepslate_bricks
setblock ~17 ~1 ~20 minecraft:cracked_deepslate_bricks
fill ~18 ~1 ~12 ~19 ~1 ~12 minecraft:stone_bricks
setblock ~18 ~1 ~20 minecraft:deepslate_bricks
fill ~19 ~1 ~20 ~20 ~1 ~20 minecraft:cracked_deepslate_bricks
fill ~20 ~1 ~12 ~29 ~1 ~12 minecraft:mossy_stone_bricks
fill ~21 ~1 ~20 ~23 ~1 ~20 minecraft:stone_bricks
fill ~24 ~1 ~20 ~28 ~1 ~20 minecraft:mossy_stone_bricks
fill ~29 ~1 ~13 ~29 ~1 ~20 minecraft:mossy_stone_bricks
fill ~1 ~2 ~12 ~2 ~3 ~12 minecraft:deepslate_bricks
setblock ~1 ~2 ~13 minecraft:mossy_stone_bricks
fill ~1 ~2 ~15 ~1 ~2 ~17 minecraft:mossy_stone_bricks
setblock ~1 ~2 ~19 minecraft:deepslate_bricks
fill ~1 ~2 ~20 ~2 ~2 ~20 minecraft:cracked_stone_bricks
setblock ~3 ~2 ~12 minecraft:cracked_deepslate_bricks
fill ~3 ~2 ~20 ~4 ~4 ~20 minecraft:mossy_stone_bricks
fill ~4 ~2 ~12 ~4 ~4 ~12 minecraft:deepslate_bricks
fill ~5 ~2 ~12 ~5 ~3 ~12 minecraft:mossy_stone_bricks
fill ~5 ~2 ~20 ~5 ~3 ~20 minecraft:cracked_deepslate_bricks
fill ~7 ~2 ~12 ~7 ~3 ~12 minecraft:mossy_stone_bricks
setblock ~7 ~2 ~20 minecraft:cracked_deepslate_bricks
fill ~8 ~2 ~12 ~8 ~3 ~12 minecraft:stone_bricks
fill ~8 ~2 ~20 ~8 ~4 ~20 minecraft:stone_bricks
fill ~9 ~2 ~12 ~10 ~3 ~12 minecraft:deepslate_bricks
fill ~9 ~2 ~20 ~10 ~3 ~20 minecraft:mossy_stone_bricks
fill ~13 ~2 ~20 ~13 ~3 ~20 minecraft:deepslate_bricks
setblock ~14 ~2 ~20 minecraft:stone_bricks
fill ~16 ~2 ~20 ~18 ~3 ~20 minecraft:stone_bricks
setblock ~18 ~2 ~12 minecraft:stone_bricks
fill ~20 ~2 ~12 ~21 ~3 ~12 minecraft:stone_bricks
fill ~20 ~2 ~20 ~21 ~4 ~20 minecraft:stone_bricks
fill ~22 ~2 ~12 ~23 ~2 ~12 minecraft:mossy_stone_bricks
fill ~22 ~2 ~20 ~23 ~2 ~20 minecraft:cracked_stone_bricks
fill ~25 ~2 ~12 ~29 ~4 ~12 minecraft:mossy_stone_bricks
setblock ~25 ~2 ~20 minecraft:stone_bricks
fill ~26 ~2 ~20 ~28 ~2 ~20 minecraft:mossy_stone_bricks
fill ~29 ~2 ~13 ~29 ~4 ~13 minecraft:mossy_stone_bricks
fill ~29 ~2 ~15 ~29 ~4 ~17 minecraft:mossy_stone_bricks
fill ~29 ~2 ~19 ~29 ~4 ~20 minecraft:mossy_stone_bricks
setblock ~1 ~3 ~13 minecraft:deepslate_bricks
fill ~1 ~3 ~15 ~1 ~4 ~16 minecraft:mossy_stone_bricks
fill ~1 ~3 ~17 ~1 ~5 ~17 minecraft:deepslate_bricks
fill ~1 ~3 ~19 ~1 ~4 ~19 minecraft:cracked_deepslate_bricks
fill ~1 ~3 ~20 ~2 ~3 ~20 minecraft:stone_bricks
fill ~3 ~3 ~12 ~3 ~5 ~12 minecraft:deepslate_bricks
fill ~7 ~3 ~20 ~7 ~4 ~20 minecraft:deepslate_bricks
setblock ~12 ~3 ~20 minecraft:deepslate_bricks
setblock ~14 ~3 ~20 minecraft:deepslate_bricks
fill ~18 ~3 ~12 ~18 ~5 ~12 minecraft:deepslate_bricks
fill ~22 ~3 ~12 ~23 ~4 ~12 minecraft:stone_bricks
fill ~22 ~3 ~20 ~23 ~3 ~20 minecraft:stone_bricks
setblock ~25 ~3 ~20 minecraft:deepslate_bricks
fill ~26 ~3 ~20 ~27 ~3 ~20 minecraft:cracked_deepslate_bricks
fill ~28 ~3 ~20 ~28 ~4 ~20 minecraft:mossy_stone_bricks
setblock ~1 ~4 ~12 minecraft:stone_bricks
fill ~1 ~4 ~13 ~1 ~4 ~14 minecraft:mossy_stone_bricks
fill ~1 ~4 ~18 ~1 ~5 ~18 minecraft:deepslate_bricks
setblock ~1 ~4 ~20 minecraft:cracked_deepslate_bricks
setblock ~2 ~4 ~12 minecraft:deepslate_bricks
setblock ~2 ~4 ~20 minecraft:stone_bricks
fill ~5 ~4 ~12 ~5 ~5 ~12 minecraft:deepslate_bricks
fill ~5 ~4 ~20 ~6 ~5 ~20 minecraft:deepslate_bricks
setblock ~6 ~4 ~12 minecraft:cracked_stone_bricks
setblock ~7 ~4 ~12 minecraft:stone_bricks
setblock ~8 ~4 ~12 minecraft:cracked_stone_bricks
fill ~9 ~4 ~12 ~12 ~4 ~12 minecraft:mossy_stone_bricks
setblock ~9 ~4 ~20 minecraft:cracked_stone_bricks
fill ~10 ~4 ~20 ~11 ~4 ~20 minecraft:stone_bricks
setblock ~12 ~4 ~20 minecraft:cracked_deepslate_bricks
fill ~13 ~4 ~20 ~16 ~4 ~20 minecraft:mossy_stone_bricks
fill ~17 ~4 ~20 ~19 ~4 ~20 minecraft:stone_bricks
fill ~19 ~4 ~12 ~21 ~4 ~12 minecraft:deepslate_bricks
setblock ~22 ~4 ~20 minecraft:cracked_stone_bricks
fill ~23 ~4 ~20 ~25 ~4 ~20 minecraft:stone_bricks
setblock ~24 ~4 ~12 minecraft:mossy_stone_bricks
setblock ~26 ~4 ~20 minecraft:cracked_deepslate_bricks
fill ~27 ~4 ~20 ~27 ~5 ~20 minecraft:deepslate_bricks
setblock ~29 ~4 ~14 minecraft:mossy_stone_bricks
setblock ~29 ~4 ~18 minecraft:mossy_stone_bricks
fill ~1 ~5 ~12 ~2 ~5 ~12 minecraft:polished_blackstone_bricks
fill ~1 ~5 ~13 ~1 ~5 ~15 minecraft:deepslate_bricks
setblock ~1 ~5 ~16 minecraft:polished_blackstone_bricks
fill ~1 ~5 ~19 ~1 ~5 ~20 minecraft:deepslate_bricks
setblock ~2 ~5 ~20 minecraft:polished_blackstone_bricks
fill ~3 ~5 ~20 ~4 ~7 ~20 minecraft:deepslate_bricks
setblock ~4 ~5 ~12 minecraft:chiseled_stone_bricks
setblock ~6 ~5 ~12 minecraft:chiseled_stone_bricks
fill ~7 ~5 ~12 ~9 ~5 ~12 minecraft:deepslate_bricks
setblock ~7 ~5 ~20 minecraft:chiseled_stone_bricks
setblock ~8 ~5 ~20 minecraft:polished_blackstone_bricks
fill ~9 ~5 ~20 ~10 ~6 ~20 minecraft:deepslate_bricks
setblock ~10 ~5 ~12 minecraft:polished_blackstone_bricks
fill ~11 ~5 ~12 ~12 ~5 ~12 minecraft:chiseled_stone_bricks
fill ~11 ~5 ~20 ~13 ~5 ~20 minecraft:chiseled_stone_bricks
fill ~13 ~5 ~12 ~17 ~6 ~12 minecraft:deepslate_bricks
setblock ~14 ~5 ~20 minecraft:polished_blackstone_bricks
fill ~15 ~5 ~20 ~15 ~6 ~20 minecraft:deepslate_bricks
setblock ~16 ~5 ~20 minecraft:polished_blackstone_bricks
fill ~17 ~5 ~20 ~18 ~6 ~20 minecraft:deepslate_bricks
setblock ~19 ~5 ~12 minecraft:deepslate_bricks
setblock ~19 ~5 ~20 minecraft:polished_blackstone_bricks
setblock ~20 ~5 ~12 minecraft:chiseled_stone_bricks
setblock ~20 ~5 ~20 minecraft:chiseled_stone_bricks
fill ~21 ~5 ~12 ~22 ~6 ~12 minecraft:deepslate_bricks
fill ~21 ~5 ~20 ~22 ~5 ~20 minecraft:deepslate_bricks
setblock ~23 ~5 ~12 minecraft:polished_blackstone_bricks
setblock ~23 ~5 ~20 minecraft:chiseled_stone_bricks
fill ~24 ~5 ~12 ~25 ~5 ~12 minecraft:deepslate_bricks
setblock ~24 ~5 ~20 minecraft:deepslate_bricks
setblock ~25 ~5 ~20 minecraft:chiseled_stone_bricks
fill ~26 ~5 ~12 ~27 ~5 ~12 minecraft:chiseled_stone_bricks
setblock ~26 ~5 ~20 minecraft:deepslate_bricks
fill ~28 ~5 ~12 ~29 ~5 ~12 minecraft:deepslate_bricks
setblock ~28 ~5 ~20 minecraft:polished_blackstone_bricks
fill ~29 ~5 ~13 ~29 ~5 ~14 minecraft:polished_blackstone_bricks
fill ~29 ~5 ~15 ~29 ~5 ~18 minecraft:deepslate_bricks
setblock ~29 ~5 ~19 minecraft:polished_blackstone_bricks
fill ~29 ~5 ~20 ~29 ~8 ~20 minecraft:deepslate_bricks
fill ~1 ~6 ~12 ~2 ~7 ~12 minecraft:stone_bricks
fill ~1 ~6 ~13 ~1 ~7 ~13 minecraft:stone_bricks
fill ~1 ~6 ~15 ~1 ~6 ~17 minecraft:stone_bricks
setblock ~1 ~6 ~19 minecraft:cracked_stone_bricks
fill ~1 ~6 ~20 ~2 ~6 ~20 minecraft:cracked_deepslate_bricks
fill ~3 ~6 ~12 ~4 ~6 ~12 minecraft:cracked_stone_bricks
setblock ~5 ~6 ~12 minecraft:stone_bricks
fill ~5 ~6 ~20 ~5 ~7 ~20 minecraft:stone_bricks
fill ~7 ~6 ~12 ~7 ~8 ~12 minecraft:deepslate_bricks
fill ~7 ~6 ~20 ~7 ~7 ~20 minecraft:stone_bricks
setblock ~8 ~6 ~12 minecraft:cracked_deepslate_bricks
fill ~8 ~6 ~20 ~8 ~7 ~20 minecraft:deepslate_bricks
fill ~9 ~6 ~12 ~10 ~7 ~12 minecraft:stone_bricks
setblock ~12 ~6 ~12 minecraft:stone_bricks
fill ~12 ~6 ~20 ~14 ~6 ~20 minecraft:deepslate_bricks
setblock ~16 ~6 ~20 minecraft:cracked_deepslate_bricks
setblock ~18 ~6 ~12 minecraft:cracked_deepslate_bricks
setblock ~20 ~6 ~12 minecraft:deepslate_bricks
fill ~20 ~6 ~20 ~20 ~7 ~20 minecraft:deepslate_bricks
fill ~21 ~6 ~20 ~23 ~7 ~20 minecraft:stone_bricks
setblock ~23 ~6 ~12 minecraft:cracked_deepslate_bricks
setblock ~25 ~6 ~12 minecraft:deepslate_bricks
fill ~25 ~6 ~20 ~25 ~7 ~20 minecraft:stone_bricks
fill ~26 ~6 ~12 ~29 ~6 ~12 minecraft:stone_bricks
setblock ~26 ~6 ~20 minecraft:cracked_stone_bricks
fill ~27 ~6 ~20 ~28 ~7 ~20 minecraft:stone_bricks
fill ~29 ~6 ~13 ~29 ~7 ~13 minecraft:stone_bricks
fill ~29 ~6 ~15 ~29 ~6 ~17 minecraft:stone_bricks
fill ~29 ~6 ~19 ~29 ~7 ~19 minecraft:deepslate_bricks
setblock ~1 ~7 ~15 minecraft:cracked_stone_bricks
fill ~1 ~7 ~16 ~1 ~7 ~17 minecraft:stone_bricks
setblock ~1 ~7 ~19 minecraft:stone_bricks
fill ~1 ~7 ~20 ~2 ~7 ~20 minecraft:deepslate_bricks
fill ~3 ~7 ~12 ~4 ~7 ~12 minecraft:stone_bricks
fill ~5 ~7 ~12 ~5 ~8 ~12 minecraft:deepslate_bricks
fill ~8 ~7 ~12 ~8 ~8 ~12 minecraft:deepslate_bricks
fill ~9 ~7 ~20 ~10 ~7 ~20 minecraft:cracked_deepslate_bricks
fill ~12 ~7 ~12 ~16 ~7 ~12 minecraft:deepslate_bricks
setblock ~12 ~7 ~20 minecraft:cracked_deepslate_bricks
fill ~13 ~7 ~20 ~18 ~7 ~20 minecraft:stone_bricks
fill ~17 ~7 ~12 ~18 ~7 ~12 minecraft:stone_bricks
fill ~20 ~7 ~12 ~23 ~7 ~12 minecraft:stone_bricks
setblock ~25 ~7 ~12 minecraft:cracked_stone_bricks
setblock ~26 ~7 ~12 minecraft:stone_bricks
setblock ~26 ~7 ~20 minecraft:stone_bricks
setblock ~27 ~7 ~12 minecraft:cracked_stone_bricks
fill ~28 ~7 ~12 ~29 ~7 ~12 minecraft:stone_bricks
setblock ~29 ~7 ~15 minecraft:stone_bricks
setblock ~29 ~7 ~16 minecraft:cracked_stone_bricks
setblock ~29 ~7 ~17 minecraft:stone_bricks
fill ~1 ~8 ~12 ~2 ~8 ~12 minecraft:deepslate_bricks
setblock ~1 ~8 ~13 minecraft:deepslate_bricks
setblock ~1 ~8 ~14 minecraft:chiseled_stone_bricks
setblock ~1 ~8 ~15 minecraft:deepslate_bricks
setblock ~1 ~8 ~16 minecraft:chiseled_stone_bricks
setblock ~1 ~8 ~17 minecraft:polished_blackstone_bricks
fill ~1 ~8 ~18 ~1 ~8 ~19 minecraft:deepslate_bricks
setblock ~1 ~8 ~20 minecraft:polished_blackstone_bricks
fill ~2 ~8 ~20 ~3 ~8 ~20 minecraft:deepslate_bricks
setblock ~3 ~8 ~12 minecraft:chiseled_stone_bricks
setblock ~4 ~8 ~12 minecraft:deepslate_bricks
setblock ~4 ~8 ~20 minecraft:chiseled_stone_bricks
setblock ~5 ~8 ~20 minecraft:deepslate_bricks
setblock ~6 ~8 ~12 minecraft:deepslate_bricks
setblock ~6 ~8 ~20 minecraft:polished_blackstone_bricks
setblock ~7 ~8 ~20 minecraft:deepslate_bricks
fill ~8 ~8 ~20 ~10 ~8 ~20 minecraft:chiseled_stone_bricks
fill ~9 ~8 ~12 ~10 ~8 ~12 minecraft:polished_blackstone_bricks
setblock ~11 ~8 ~12 minecraft:chiseled_stone_bricks
setblock ~11 ~8 ~20 minecraft:deepslate_bricks
setblock ~12 ~8 ~12 minecraft:deepslate_bricks
fill ~12 ~8 ~20 ~13 ~8 ~20 minecraft:chiseled_stone_bricks
fill ~13 ~8 ~12 ~14 ~8 ~12 minecraft:polished_blackstone_bricks
setblock ~14 ~8 ~20 minecraft:polished_blackstone_bricks
setblock ~15 ~8 ~12 minecraft:chiseled_stone_bricks
setblock ~15 ~8 ~20 minecraft:deepslate_bricks
fill ~16 ~8 ~12 ~18 ~8 ~12 minecraft:deepslate_bricks
setblock ~16 ~8 ~20 minecraft:chiseled_stone_bricks
fill ~17 ~8 ~20 ~18 ~8 ~20 minecraft:polished_blackstone_bricks
setblock ~19 ~8 ~12 minecraft:chiseled_stone_bricks
setblock ~19 ~8 ~20 minecraft:deepslate_bricks
fill ~20 ~8 ~12 ~22 ~8 ~12 minecraft:deepslate_bricks
setblock ~20 ~8 ~20 minecraft:chiseled_stone_bricks
fill ~21 ~8 ~20 ~26 ~8 ~20 minecraft:deepslate_bricks
setblock ~23 ~8 ~12 minecraft:chiseled_stone_bricks
fill ~24 ~8 ~12 ~29 ~8 ~12 minecraft:deepslate_bricks
fill ~27 ~8 ~20 ~28 ~8 ~20 minecraft:polished_blackstone_bricks
setblock ~29 ~8 ~13 minecraft:chiseled_stone_bricks
setblock ~29 ~8 ~14 minecraft:deepslate_bricks
setblock ~29 ~8 ~15 minecraft:chiseled_stone_bricks
setblock ~29 ~8 ~16 minecraft:deepslate_bricks
setblock ~29 ~8 ~17 minecraft:chiseled_stone_bricks
setblock ~29 ~8 ~18 minecraft:deepslate_bricks
setblock ~29 ~8 ~19 minecraft:chiseled_stone_bricks

# the buttresses on both long walls at x 4, 9, 21, 26, stepped back under the eaves with a stair
fill ~4 ~1 ~11 ~4 ~6 ~11 minecraft:mossy_stone_bricks
fill ~4 ~1 ~21 ~4 ~6 ~21 minecraft:deepslate_bricks
fill ~9 ~1 ~11 ~9 ~6 ~11 minecraft:mossy_stone_bricks
fill ~9 ~1 ~21 ~9 ~6 ~21 minecraft:mossy_stone_bricks
fill ~21 ~1 ~11 ~21 ~6 ~11 minecraft:stone_bricks
fill ~21 ~1 ~21 ~21 ~6 ~21 minecraft:stone_bricks
fill ~26 ~1 ~11 ~26 ~6 ~11 minecraft:cracked_stone_bricks
fill ~26 ~1 ~21 ~26 ~6 ~21 minecraft:stone_bricks
setblock ~4 ~7 ~11 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~4 ~7 ~21 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~9 ~7 ~11 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~9 ~7 ~21 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~21 ~7 ~11 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~21 ~7 ~21 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~26 ~7 ~11 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~26 ~7 ~21 minecraft:stone_brick_stairs[facing=north,half=bottom]

# the eaves: a soffit of upside-down deepslate brick stairs under the eave rows
fill ~0 ~7 ~11 ~3 ~7 ~11 minecraft:deepslate_brick_stairs[facing=south,half=top]
fill ~0 ~7 ~21 ~3 ~7 ~21 minecraft:deepslate_brick_stairs[facing=north,half=top]
setblock ~6 ~7 ~11 minecraft:deepslate_brick_stairs[facing=south,half=top]
setblock ~6 ~7 ~21 minecraft:deepslate_brick_stairs[facing=north,half=top]
setblock ~8 ~7 ~11 minecraft:deepslate_brick_stairs[facing=south,half=top]
setblock ~8 ~7 ~21 minecraft:deepslate_brick_stairs[facing=north,half=top]
setblock ~11 ~7 ~11 minecraft:deepslate_brick_stairs[facing=south,half=top]
setblock ~11 ~7 ~21 minecraft:deepslate_brick_stairs[facing=north,half=top]
fill ~13 ~7 ~11 ~17 ~7 ~11 minecraft:deepslate_brick_stairs[facing=south,half=top]
fill ~13 ~7 ~21 ~17 ~7 ~21 minecraft:deepslate_brick_stairs[facing=north,half=top]
setblock ~19 ~7 ~11 minecraft:deepslate_brick_stairs[facing=south,half=top]
setblock ~19 ~7 ~21 minecraft:deepslate_brick_stairs[facing=north,half=top]
setblock ~22 ~7 ~11 minecraft:deepslate_brick_stairs[facing=south,half=top]
setblock ~22 ~7 ~21 minecraft:deepslate_brick_stairs[facing=north,half=top]
setblock ~24 ~7 ~11 minecraft:deepslate_brick_stairs[facing=south,half=top]
setblock ~24 ~7 ~21 minecraft:deepslate_brick_stairs[facing=north,half=top]
fill ~27 ~7 ~11 ~30 ~7 ~11 minecraft:deepslate_brick_stairs[facing=south,half=top]
fill ~27 ~7 ~21 ~30 ~7 ~21 minecraft:deepslate_brick_stairs[facing=north,half=top]

# the floor of the upper storey at y 5: spruce planks, open over the stair at x 27, z 16..19
fill ~2 ~5 ~13 ~28 ~5 ~14 minecraft:spruce_planks
fill ~2 ~5 ~15 ~26 ~5 ~19 minecraft:spruce_planks
fill ~28 ~5 ~15 ~28 ~5 ~19 minecraft:spruce_planks

# the doors: the front door x 14..16 under a dark oak lintel and posts, the back door at (15, 1..2, 20)
fill ~13 ~1 ~12 ~13 ~4 ~12 minecraft:dark_oak_log[axis=y]
fill ~14 ~1 ~12 ~16 ~3 ~12 minecraft:air
fill ~15 ~1 ~20 ~15 ~2 ~20 minecraft:air
fill ~17 ~1 ~12 ~17 ~4 ~12 minecraft:dark_oak_log[axis=y]
setblock ~15 ~3 ~20 minecraft:chiseled_stone_bricks
fill ~14 ~4 ~12 ~16 ~4 ~12 minecraft:dark_oak_log[axis=x]

# windows: barred slits on the ground storey, glass in the upper storey with shutters swung open; both gables get barred slits
fill ~1 ~2 ~14 ~1 ~3 ~14 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~1 ~2 ~18 ~1 ~3 ~18 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~6 ~2 ~12 ~6 ~3 ~12 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~6 ~2 ~20 ~6 ~3 ~20 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~11 ~2 ~12 ~11 ~3 ~12 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~11 ~2 ~20 ~11 ~3 ~20 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~19 ~2 ~12 ~19 ~3 ~12 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~19 ~2 ~20 ~19 ~3 ~20 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~24 ~2 ~12 ~24 ~3 ~12 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~24 ~2 ~20 ~24 ~3 ~20 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~29 ~2 ~14 ~29 ~3 ~14 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~29 ~2 ~18 ~29 ~3 ~18 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~1 ~6 ~14 ~1 ~7 ~14 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~1 ~6 ~18 ~1 ~7 ~18 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~5 ~6 ~11 ~5 ~7 ~11 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
fill ~5 ~6 ~21 ~5 ~7 ~21 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
fill ~6 ~6 ~12 ~6 ~7 ~12 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~6 ~6 ~20 ~6 ~7 ~20 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~7 ~6 ~11 ~7 ~7 ~11 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
fill ~7 ~6 ~21 ~7 ~7 ~21 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
fill ~10 ~6 ~11 ~10 ~7 ~11 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
fill ~10 ~6 ~21 ~10 ~7 ~21 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
fill ~11 ~6 ~12 ~11 ~7 ~12 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~11 ~6 ~20 ~11 ~7 ~20 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~12 ~6 ~11 ~12 ~7 ~11 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
fill ~12 ~6 ~21 ~12 ~7 ~21 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
fill ~18 ~6 ~11 ~18 ~7 ~11 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
fill ~18 ~6 ~21 ~18 ~7 ~21 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
fill ~19 ~6 ~12 ~19 ~7 ~12 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~19 ~6 ~20 ~19 ~7 ~20 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~20 ~6 ~11 ~20 ~7 ~11 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
fill ~20 ~6 ~21 ~20 ~7 ~21 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
fill ~23 ~6 ~11 ~23 ~7 ~11 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
fill ~23 ~6 ~21 ~23 ~7 ~21 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
fill ~24 ~6 ~12 ~24 ~7 ~12 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~24 ~6 ~20 ~24 ~7 ~20 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~25 ~6 ~11 ~25 ~7 ~11 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
fill ~25 ~6 ~21 ~25 ~7 ~21 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
fill ~29 ~6 ~14 ~29 ~7 ~14 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~29 ~6 ~18 ~29 ~7 ~18 minecraft:glass_pane[north=true,east=false,south=true,west=false]

# the roof: deepslate tile stairs from the eaves at y 8 (z 11 and 21) to a ridge of deepslate tiles at y 13 (z 16), over x 0..30, a block proud of both gables
fill ~0 ~8 ~11 ~13 ~8 ~11 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~0 ~8 ~21 ~30 ~8 ~21 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~14 ~8 ~11 minecraft:mossy_stone_brick_stairs[facing=south,half=bottom]
fill ~15 ~8 ~11 ~30 ~8 ~11 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~0 ~9 ~12 ~4 ~9 ~12 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~0 ~9 ~20 ~7 ~9 ~20 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~5 ~9 ~12 minecraft:stone_brick_stairs[facing=south,half=bottom]
fill ~6 ~9 ~12 ~30 ~9 ~12 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~8 ~9 ~20 minecraft:cobblestone_stairs[facing=north,half=bottom]
fill ~9 ~9 ~20 ~30 ~9 ~20 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~0 ~10 ~13 ~11 ~10 ~13 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~0 ~10 ~19 ~22 ~10 ~19 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~12 ~10 ~13 minecraft:cobblestone_stairs[facing=south,half=bottom]
fill ~13 ~10 ~13 ~30 ~10 ~13 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~23 ~10 ~19 minecraft:stone_brick_stairs[facing=north,half=bottom]
fill ~24 ~10 ~19 ~30 ~10 ~19 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~0 ~11 ~14 ~17 ~11 ~14 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~0 ~11 ~18 ~25 ~11 ~18 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~18 ~11 ~14 minecraft:stone_brick_stairs[facing=south,half=bottom]
fill ~19 ~11 ~14 ~30 ~11 ~14 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~26 ~11 ~18 minecraft:cobblestone_stairs[facing=north,half=bottom]
fill ~27 ~11 ~18 ~30 ~11 ~18 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~1 ~12 ~15 ~9 ~12 ~15 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~1 ~12 ~17 ~29 ~12 ~17 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~10 ~12 ~15 minecraft:mossy_cobblestone_stairs[facing=south,half=bottom]
fill ~11 ~12 ~15 ~29 ~12 ~15 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~1 ~13 ~16 ~29 ~13 ~16 minecraft:deepslate_tiles

# the gables: stone up to the roof with a collar beam at y 9 and a slit window at y 10..11; the ridge cap of deepslate tile slabs; a few patched shingles
setblock ~1 ~9 ~13 minecraft:deepslate_bricks
fill ~1 ~9 ~14 ~1 ~9 ~18 minecraft:dark_oak_log[axis=z]
setblock ~1 ~9 ~19 minecraft:stone_bricks
setblock ~29 ~9 ~13 minecraft:stone_bricks
fill ~29 ~9 ~14 ~29 ~9 ~18 minecraft:dark_oak_log[axis=z]
setblock ~29 ~9 ~19 minecraft:cracked_deepslate_bricks
fill ~1 ~10 ~14 ~1 ~10 ~15 minecraft:stone_bricks
fill ~1 ~10 ~16 ~1 ~11 ~16 minecraft:iron_bars[north=true,east=false,south=true,west=true]
setblock ~1 ~10 ~17 minecraft:cracked_stone_bricks
setblock ~1 ~10 ~18 minecraft:stone_bricks
setblock ~29 ~10 ~14 minecraft:stone_bricks
setblock ~29 ~10 ~15 minecraft:cracked_stone_bricks
fill ~29 ~10 ~16 ~29 ~11 ~16 minecraft:iron_bars[north=true,east=true,south=true,west=false]
fill ~29 ~10 ~17 ~29 ~10 ~18 minecraft:stone_bricks
setblock ~1 ~11 ~15 minecraft:stone_bricks
setblock ~1 ~11 ~17 minecraft:stone_bricks
setblock ~29 ~11 ~15 minecraft:stone_bricks
setblock ~29 ~11 ~17 minecraft:stone_bricks
setblock ~1 ~12 ~16 minecraft:stone_bricks
setblock ~29 ~12 ~16 minecraft:stone_bricks
fill ~1 ~14 ~16 ~29 ~14 ~16 minecraft:deepslate_tile_slab[type=bottom]

# the chimneys against both gables (x 0 and x 30, z 15..17): cobble stacks narrowing to one flue, a campfire on top
fill ~0 ~0 ~15 ~0 ~0 ~16 minecraft:cobblestone
setblock ~0 ~0 ~17 minecraft:mossy_cobblestone
fill ~30 ~0 ~15 ~30 ~0 ~16 minecraft:mossy_cobblestone
fill ~30 ~0 ~17 ~30 ~1 ~17 minecraft:cobblestone
fill ~0 ~1 ~15 ~0 ~1 ~16 minecraft:mossy_cobblestone
fill ~0 ~1 ~17 ~0 ~2 ~17 minecraft:cobblestone
setblock ~30 ~1 ~15 minecraft:cobblestone
fill ~30 ~1 ~16 ~30 ~2 ~16 minecraft:mossy_cobblestone
fill ~0 ~2 ~15 ~0 ~3 ~16 minecraft:cobblestone
setblock ~30 ~2 ~15 minecraft:mossy_cobblestone
setblock ~30 ~2 ~17 minecraft:mossy_cobblestone
setblock ~0 ~3 ~17 minecraft:mossy_cobblestone
fill ~30 ~3 ~15 ~30 ~4 ~16 minecraft:cobblestone
setblock ~30 ~3 ~17 minecraft:andesite
fill ~0 ~4 ~15 ~0 ~4 ~16 minecraft:mossy_cobblestone
fill ~0 ~4 ~17 ~0 ~7 ~17 minecraft:cobblestone
fill ~30 ~4 ~17 ~30 ~7 ~17 minecraft:cobblestone
setblock ~0 ~5 ~15 minecraft:andesite
fill ~0 ~5 ~16 ~0 ~7 ~16 minecraft:cobblestone
setblock ~30 ~5 ~15 minecraft:andesite
setblock ~30 ~5 ~16 minecraft:cobblestone
fill ~0 ~6 ~15 ~0 ~11 ~15 minecraft:cobblestone
fill ~30 ~6 ~15 ~30 ~8 ~15 minecraft:cobblestone
fill ~30 ~6 ~16 ~30 ~7 ~16 minecraft:mossy_cobblestone
fill ~0 ~8 ~16 ~0 ~8 ~17 minecraft:mossy_cobblestone
fill ~30 ~8 ~16 ~30 ~13 ~16 minecraft:cobblestone
setblock ~30 ~8 ~17 minecraft:andesite
fill ~0 ~9 ~16 ~0 ~11 ~17 minecraft:cobblestone
setblock ~30 ~9 ~15 minecraft:andesite
fill ~30 ~9 ~17 ~30 ~10 ~17 minecraft:cobblestone
setblock ~30 ~10 ~15 minecraft:mossy_cobblestone
setblock ~30 ~11 ~15 minecraft:andesite
setblock ~30 ~11 ~17 minecraft:mossy_cobblestone
setblock ~0 ~12 ~15 minecraft:cobblestone_stairs[facing=south,half=bottom]
fill ~0 ~12 ~16 ~0 ~13 ~16 minecraft:cobblestone
setblock ~0 ~12 ~17 minecraft:cobblestone_stairs[facing=north,half=bottom]
setblock ~30 ~12 ~15 minecraft:cobblestone_stairs[facing=south,half=bottom]
setblock ~30 ~12 ~17 minecraft:cobblestone_stairs[facing=north,half=bottom]
setblock ~0 ~14 ~16 minecraft:campfire[lit=true,signal_fire=false,facing=east]
setblock ~30 ~14 ~16 minecraft:campfire[lit=true,signal_fire=false,facing=west]

# the stair up inside at x 27: stairs rising north from z 19 (y 1) to z 15 (y 5) on a wedge of cobble
fill ~27 ~1 ~15 ~27 ~3 ~15 minecraft:cobblestone
setblock ~27 ~1 ~16 minecraft:mossy_cobblestone
fill ~27 ~1 ~17 ~27 ~1 ~18 minecraft:cobblestone
setblock ~27 ~1 ~19 minecraft:stone_brick_stairs[facing=north,half=bottom]
fill ~27 ~2 ~16 ~27 ~2 ~17 minecraft:cobblestone
setblock ~27 ~2 ~18 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~27 ~3 ~16 minecraft:mossy_cobblestone
setblock ~27 ~3 ~17 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~27 ~4 ~15 minecraft:andesite
setblock ~27 ~4 ~16 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~27 ~5 ~15 minecraft:stone_brick_stairs[facing=north,half=bottom]

# the porch: four stone pillars, a slab roof at y 5 over x 12..18, z 9..11, a lantern on a chain
fill ~12 ~1 ~9 ~12 ~4 ~9 minecraft:chiseled_stone_bricks
fill ~12 ~1 ~11 ~12 ~5 ~11 minecraft:deepslate_bricks
fill ~18 ~1 ~9 ~18 ~5 ~9 minecraft:deepslate_bricks
fill ~18 ~1 ~11 ~18 ~5 ~11 minecraft:deepslate_bricks
setblock ~15 ~4 ~10 minecraft:lantern[hanging=true]
setblock ~12 ~5 ~9 minecraft:deepslate_bricks
setblock ~12 ~5 ~10 minecraft:deepslate_tile_slab[type=bottom]
fill ~13 ~5 ~9 ~17 ~5 ~11 minecraft:deepslate_tile_slab[type=bottom]
setblock ~18 ~5 ~10 minecraft:deepslate_tile_slab[type=bottom]

# the yard wall: stone y 1..3 in patches with a coping of stone brick slabs at y 4, piers every six blocks to y 4 and a cap with a lantern
fill ~0 ~1 ~0 ~0 ~4 ~0 minecraft:cracked_deepslate_bricks
fill ~0 ~1 ~1 ~0 ~1 ~4 minecraft:stone_bricks
fill ~0 ~1 ~5 ~0 ~4 ~5 minecraft:deepslate_bricks
fill ~0 ~1 ~6 ~0 ~1 ~10 minecraft:mossy_stone_bricks
fill ~0 ~1 ~11 ~0 ~4 ~11 minecraft:deepslate_bricks
fill ~1 ~1 ~0 ~5 ~1 ~0 minecraft:stone_bricks
fill ~6 ~1 ~0 ~6 ~4 ~0 minecraft:deepslate_bricks
setblock ~7 ~1 ~0 minecraft:cracked_stone_bricks
fill ~8 ~1 ~0 ~9 ~1 ~0 minecraft:stone_bricks
fill ~10 ~1 ~0 ~11 ~3 ~0 minecraft:mossy_stone_bricks
fill ~19 ~1 ~0 ~20 ~1 ~0 minecraft:cracked_stone_bricks
fill ~21 ~1 ~0 ~23 ~1 ~0 minecraft:stone_bricks
fill ~24 ~1 ~0 ~24 ~4 ~0 minecraft:deepslate_bricks
setblock ~25 ~1 ~0 minecraft:cracked_stone_bricks
fill ~26 ~1 ~0 ~28 ~1 ~0 minecraft:stone_bricks
fill ~29 ~1 ~0 ~29 ~3 ~0 minecraft:mossy_stone_bricks
fill ~30 ~1 ~0 ~30 ~4 ~0 minecraft:stone_bricks
fill ~30 ~1 ~1 ~30 ~3 ~2 minecraft:mossy_stone_bricks
fill ~30 ~1 ~3 ~30 ~1 ~4 minecraft:stone_bricks
fill ~30 ~1 ~5 ~30 ~4 ~5 minecraft:deepslate_bricks
fill ~30 ~1 ~6 ~30 ~1 ~11 minecraft:stone_bricks
fill ~0 ~2 ~1 ~0 ~2 ~2 minecraft:deepslate_bricks
fill ~0 ~2 ~3 ~0 ~2 ~4 minecraft:stone_bricks
fill ~0 ~2 ~6 ~0 ~3 ~8 minecraft:stone_bricks
fill ~0 ~2 ~9 ~0 ~2 ~10 minecraft:cracked_stone_bricks
fill ~1 ~2 ~0 ~1 ~3 ~0 minecraft:deepslate_bricks
fill ~2 ~2 ~0 ~5 ~2 ~0 minecraft:stone_bricks
fill ~7 ~2 ~0 ~9 ~3 ~0 minecraft:mossy_stone_bricks
fill ~19 ~2 ~0 ~20 ~2 ~0 minecraft:deepslate_bricks
fill ~21 ~2 ~0 ~22 ~2 ~0 minecraft:cracked_deepslate_bricks
setblock ~23 ~2 ~0 minecraft:cracked_stone_bricks
fill ~25 ~2 ~0 ~27 ~2 ~0 minecraft:stone_bricks
fill ~28 ~2 ~0 ~28 ~3 ~0 minecraft:mossy_stone_bricks
fill ~30 ~2 ~3 ~30 ~3 ~4 minecraft:mossy_stone_bricks
fill ~30 ~2 ~6 ~30 ~2 ~8 minecraft:stone_bricks
setblock ~30 ~2 ~9 minecraft:cracked_stone_bricks
fill ~30 ~2 ~10 ~30 ~2 ~11 minecraft:stone_bricks
setblock ~0 ~3 ~1 minecraft:deepslate_bricks
fill ~0 ~3 ~2 ~0 ~3 ~3 minecraft:cracked_deepslate_bricks
setblock ~0 ~3 ~4 minecraft:stone_bricks
setblock ~0 ~3 ~9 minecraft:cracked_stone_bricks
setblock ~0 ~3 ~10 minecraft:stone_bricks
setblock ~2 ~3 ~0 minecraft:stone_bricks
setblock ~3 ~3 ~0 minecraft:cracked_stone_bricks
fill ~4 ~3 ~0 ~5 ~3 ~0 minecraft:stone_bricks
setblock ~19 ~3 ~0 minecraft:deepslate_bricks
setblock ~20 ~3 ~0 minecraft:cracked_deepslate_bricks
fill ~21 ~3 ~0 ~22 ~3 ~0 minecraft:deepslate_bricks
setblock ~23 ~3 ~0 minecraft:stone_bricks
fill ~25 ~3 ~0 ~26 ~3 ~0 minecraft:stone_bricks
setblock ~27 ~3 ~0 minecraft:mossy_stone_bricks
setblock ~30 ~3 ~6 minecraft:mossy_stone_bricks
setblock ~30 ~3 ~7 minecraft:cracked_stone_bricks
fill ~30 ~3 ~8 ~30 ~3 ~9 minecraft:stone_bricks
setblock ~30 ~3 ~10 minecraft:deepslate_bricks
fill ~30 ~3 ~11 ~30 ~4 ~11 minecraft:stone_bricks
fill ~0 ~4 ~1 ~0 ~4 ~4 minecraft:stone_brick_slab[type=bottom]
fill ~0 ~4 ~6 ~0 ~4 ~10 minecraft:stone_brick_slab[type=bottom]
fill ~1 ~4 ~0 ~5 ~4 ~0 minecraft:stone_brick_slab[type=bottom]
fill ~7 ~4 ~0 ~11 ~4 ~0 minecraft:stone_brick_slab[type=bottom]
fill ~19 ~4 ~0 ~23 ~4 ~0 minecraft:stone_brick_slab[type=bottom]
fill ~25 ~4 ~0 ~29 ~4 ~0 minecraft:stone_brick_slab[type=bottom]
fill ~30 ~4 ~1 ~30 ~4 ~4 minecraft:stone_brick_slab[type=bottom]
fill ~30 ~4 ~6 ~30 ~4 ~10 minecraft:stone_brick_slab[type=bottom]
setblock ~0 ~5 ~0 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~0 ~5 ~5 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~0 ~5 ~11 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~6 ~5 ~0 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~24 ~5 ~0 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~30 ~5 ~0 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~30 ~5 ~5 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~30 ~5 ~11 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~0 ~6 ~0 minecraft:lantern[hanging=false]
setblock ~0 ~6 ~5 minecraft:lantern[hanging=false]
setblock ~0 ~6 ~11 minecraft:lantern[hanging=false]
setblock ~6 ~6 ~0 minecraft:lantern[hanging=false]
setblock ~24 ~6 ~0 minecraft:lantern[hanging=false]
setblock ~30 ~6 ~0 minecraft:lantern[hanging=false]
setblock ~30 ~6 ~5 minecraft:lantern[hanging=false]
setblock ~30 ~6 ~11 minecraft:lantern[hanging=false]

# the gate: piers at x 12 and 18 to y 5 with a chiseled cap and a lantern, a dark oak beam at y 5 across the opening
fill ~12 ~1 ~0 ~12 ~4 ~0 minecraft:deepslate_bricks
fill ~18 ~1 ~0 ~18 ~4 ~0 minecraft:stone_bricks
setblock ~12 ~5 ~0 minecraft:chiseled_stone_bricks
fill ~13 ~5 ~0 ~17 ~5 ~0 minecraft:dark_oak_log[axis=x]
setblock ~18 ~5 ~0 minecraft:chiseled_stone_bricks
setblock ~12 ~6 ~0 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~18 ~6 ~0 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~12 ~7 ~0 minecraft:lantern[hanging=false]
setblock ~18 ~7 ~0 minecraft:lantern[hanging=false]

# the forge shed against the north wall: a lean-to on three posts, an anvil, a grindstone, a lit blast furnace, a cauldron and barrels
fill ~23 ~1 ~4 ~23 ~4 ~4 minecraft:dark_oak_log[axis=y]
setblock ~24 ~1 ~1 minecraft:cauldron
setblock ~24 ~1 ~3 minecraft:barrel[facing=up]
setblock ~25 ~1 ~2 minecraft:anvil[facing=north]
fill ~26 ~1 ~4 ~26 ~4 ~4 minecraft:dark_oak_log[axis=y]
setblock ~27 ~1 ~2 minecraft:grindstone[face=floor,facing=north]
setblock ~28 ~1 ~1 minecraft:blast_furnace[facing=south,lit=true]
fill ~29 ~1 ~3 ~29 ~2 ~3 minecraft:barrel[facing=up]
fill ~29 ~1 ~4 ~29 ~4 ~4 minecraft:dark_oak_log[axis=y]
fill ~23 ~5 ~1 ~29 ~5 ~4 minecraft:spruce_slab[type=top]

# drill dummies on posts (a hay head on a crossed post), a weapon rack along the west wall, troughs and hay in the corners
setblock ~2 ~1 ~3 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
fill ~2 ~1 ~4 ~2 ~1 ~7 minecraft:spruce_fence[north=true,east=false,south=true,west=false]
setblock ~2 ~1 ~8 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
fill ~2 ~1 ~10 ~3 ~2 ~11 minecraft:hay_block[axis=y]
setblock ~5 ~1 ~6 minecraft:oak_fence[north=false,east=false,south=false,west=false]
fill ~8 ~1 ~10 ~9 ~1 ~10 minecraft:barrel[facing=up]
setblock ~9 ~1 ~6 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~21 ~1 ~6 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~25 ~1 ~6 minecraft:oak_fence[north=false,east=false,south=false,west=false]
fill ~27 ~1 ~10 ~28 ~1 ~10 minecraft:barrel[facing=up]
setblock ~28 ~1 ~9 minecraft:cauldron
setblock ~2 ~2 ~3 minecraft:iron_bars[north=false,east=false,south=false,west=false]
setblock ~2 ~2 ~5 minecraft:iron_bars[north=false,east=false,south=false,west=false]
setblock ~2 ~2 ~7 minecraft:iron_bars[north=false,east=false,south=false,west=false]
setblock ~4 ~2 ~6 minecraft:oak_fence[north=false,east=true,south=false,west=false]
setblock ~5 ~2 ~6 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~6 ~2 ~6 minecraft:oak_fence[north=false,east=false,south=false,west=true]
setblock ~8 ~2 ~6 minecraft:oak_fence[north=false,east=true,south=false,west=false]
setblock ~9 ~2 ~6 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~10 ~2 ~6 minecraft:oak_fence[north=false,east=false,south=false,west=true]
setblock ~20 ~2 ~6 minecraft:oak_fence[north=false,east=true,south=false,west=false]
setblock ~21 ~2 ~6 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~22 ~2 ~6 minecraft:oak_fence[north=false,east=false,south=false,west=true]
setblock ~24 ~2 ~6 minecraft:oak_fence[north=false,east=true,south=false,west=false]
setblock ~25 ~2 ~6 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~26 ~2 ~6 minecraft:oak_fence[north=false,east=false,south=false,west=true]
setblock ~2 ~3 ~11 minecraft:hay_block[axis=x]
setblock ~5 ~3 ~6 minecraft:hay_block[axis=y]
setblock ~9 ~3 ~6 minecraft:hay_block[axis=y]
setblock ~21 ~3 ~6 minecraft:hay_block[axis=y]
setblock ~25 ~3 ~6 minecraft:hay_block[axis=y]

# the hall: bunks along the north wall, a long table with stools, a furnace and a cauldron by the back wall, lanterns hung from the floor above
setblock ~3 ~1 ~13 minecraft:brown_bed[facing=north,part=head]
setblock ~3 ~1 ~14 minecraft:brown_bed[facing=north,part=foot]
setblock ~3 ~1 ~19 minecraft:furnace[facing=north,lit=true]
setblock ~4 ~1 ~13 minecraft:barrel[facing=up]
setblock ~4 ~1 ~19 minecraft:cauldron
setblock ~5 ~1 ~13 minecraft:brown_bed[facing=north,part=head]
setblock ~5 ~1 ~14 minecraft:brown_bed[facing=north,part=foot]
setblock ~5 ~1 ~19 minecraft:barrel[facing=up]
setblock ~6 ~1 ~13 minecraft:barrel[facing=up]
setblock ~7 ~1 ~13 minecraft:brown_bed[facing=north,part=head]
setblock ~7 ~1 ~14 minecraft:brown_bed[facing=north,part=foot]
setblock ~10 ~1 ~18 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
fill ~11 ~1 ~17 ~12 ~1 ~17 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~11 ~1 ~18 ~19 ~1 ~18 minecraft:spruce_fence[north=false,east=true,south=false,west=true]
setblock ~13 ~1 ~19 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~14 ~1 ~17 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~16 ~1 ~17 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~17 ~1 ~19 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~18 ~1 ~17 ~19 ~1 ~17 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~20 ~1 ~18 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
setblock ~23 ~1 ~13 minecraft:brown_bed[facing=north,part=head]
setblock ~23 ~1 ~14 minecraft:brown_bed[facing=north,part=foot]
setblock ~24 ~1 ~13 minecraft:barrel[facing=up]
setblock ~25 ~1 ~13 minecraft:brown_bed[facing=north,part=head]
setblock ~25 ~1 ~14 minecraft:brown_bed[facing=north,part=foot]
fill ~10 ~2 ~18 ~20 ~2 ~18 minecraft:spruce_pressure_plate
setblock ~11 ~4 ~15 minecraft:lantern[hanging=true]
setblock ~15 ~4 ~16 minecraft:lantern[hanging=true]
setblock ~19 ~4 ~15 minecraft:lantern[hanging=true]

# the upper storey: rows of beds along both long walls with a barrel between them, lanterns on the barrels
setblock ~3 ~6 ~13 minecraft:gray_bed[facing=north,part=head]
setblock ~3 ~6 ~14 minecraft:gray_bed[facing=north,part=foot]
setblock ~3 ~6 ~18 minecraft:gray_bed[facing=south,part=foot]
setblock ~3 ~6 ~19 minecraft:gray_bed[facing=south,part=head]
setblock ~4 ~6 ~13 minecraft:barrel[facing=up]
setblock ~4 ~6 ~19 minecraft:barrel[facing=up]
setblock ~5 ~6 ~13 minecraft:gray_bed[facing=north,part=head]
setblock ~5 ~6 ~14 minecraft:gray_bed[facing=north,part=foot]
setblock ~5 ~6 ~18 minecraft:gray_bed[facing=south,part=foot]
setblock ~5 ~6 ~19 minecraft:gray_bed[facing=south,part=head]
setblock ~7 ~6 ~13 minecraft:gray_bed[facing=north,part=head]
setblock ~7 ~6 ~14 minecraft:gray_bed[facing=north,part=foot]
setblock ~7 ~6 ~18 minecraft:gray_bed[facing=south,part=foot]
setblock ~7 ~6 ~19 minecraft:gray_bed[facing=south,part=head]
setblock ~8 ~6 ~13 minecraft:barrel[facing=up]
setblock ~8 ~6 ~19 minecraft:barrel[facing=up]
setblock ~9 ~6 ~13 minecraft:gray_bed[facing=north,part=head]
setblock ~9 ~6 ~14 minecraft:gray_bed[facing=north,part=foot]
setblock ~9 ~6 ~18 minecraft:gray_bed[facing=south,part=foot]
setblock ~9 ~6 ~19 minecraft:gray_bed[facing=south,part=head]
setblock ~21 ~6 ~13 minecraft:gray_bed[facing=north,part=head]
setblock ~21 ~6 ~14 minecraft:gray_bed[facing=north,part=foot]
setblock ~21 ~6 ~18 minecraft:gray_bed[facing=south,part=foot]
setblock ~21 ~6 ~19 minecraft:gray_bed[facing=south,part=head]
setblock ~22 ~6 ~13 minecraft:barrel[facing=up]
setblock ~22 ~6 ~19 minecraft:barrel[facing=up]
setblock ~23 ~6 ~13 minecraft:gray_bed[facing=north,part=head]
setblock ~23 ~6 ~14 minecraft:gray_bed[facing=north,part=foot]
setblock ~23 ~6 ~18 minecraft:gray_bed[facing=south,part=foot]
setblock ~23 ~6 ~19 minecraft:gray_bed[facing=south,part=head]
setblock ~25 ~6 ~13 minecraft:gray_bed[facing=north,part=head]
setblock ~25 ~6 ~14 minecraft:gray_bed[facing=north,part=foot]
setblock ~4 ~7 ~13 minecraft:lantern[hanging=false]
setblock ~8 ~7 ~13 minecraft:lantern[hanging=false]
setblock ~22 ~7 ~13 minecraft:lantern[hanging=false]

# outside the back wall: a woodpile, two barrels and moss at the foot
setblock ~3 ~1 ~21 minecraft:moss_carpet
setblock ~5 ~1 ~21 minecraft:moss_carpet
setblock ~7 ~1 ~21 minecraft:moss_carpet
fill ~10 ~1 ~21 ~11 ~1 ~21 minecraft:moss_carpet
fill ~12 ~1 ~21 ~14 ~1 ~21 minecraft:oak_log[axis=x]
setblock ~16 ~1 ~21 minecraft:moss_carpet
fill ~17 ~1 ~21 ~18 ~1 ~21 minecraft:barrel[facing=up]
fill ~23 ~1 ~21 ~24 ~1 ~21 minecraft:moss_carpet
setblock ~27 ~1 ~21 minecraft:moss_carpet
fill ~12 ~2 ~21 ~13 ~2 ~21 minecraft:oak_log[axis=x]
