# size 31 9 37
# supply/ironhold: supply house of Ironhold, armoury and walled yard: stone brick under a roof of deepslate tiles, iron bars, a lookout, a yard with the capture circle, the full forge corner, weapon racks, a cart and cots.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Shared shape of the five supply houses (spec 7.1, plan Task 8b): storehouse x 0..30, z 0..11; lookout deck y 5 over x 22..28, z 7..11 with the ladder at (22, 1..5, 12); yard walls y 1..2 along x 0, x 30, z 36; front gate x 11..19; side gate z 22..26;
# capture circle centre (15, 0, 24) radius 10, edge ring 9 to 10, plinth x 14..16 z 23..25; the flag column |x-15|, |z-24| <= 3, y 1..8 is structure void; the braziers (unlit campfires) at (10, 3, 36) and (20, 3, 36); nothing solid above y 6.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~1 ~1 ~1 ~2 ~3 ~10 minecraft:air
fill ~1 ~1 ~12 ~5 ~8 ~13 minecraft:air
fill ~1 ~1 ~14 ~3 ~2 ~15 minecraft:air
fill ~1 ~1 ~16 ~2 ~2 ~16 minecraft:air
fill ~1 ~1 ~17 ~1 ~2 ~21 minecraft:air
fill ~1 ~1 ~23 ~1 ~2 ~27 minecraft:air
fill ~1 ~1 ~29 ~1 ~2 ~29 minecraft:air
fill ~1 ~1 ~31 ~1 ~2 ~31 minecraft:air
fill ~1 ~1 ~33 ~1 ~2 ~35 minecraft:air
fill ~2 ~1 ~18 ~2 ~2 ~18 minecraft:air
fill ~2 ~1 ~20 ~2 ~2 ~20 minecraft:air
fill ~2 ~1 ~22 ~2 ~2 ~23 minecraft:air
fill ~2 ~1 ~26 ~3 ~2 ~28 minecraft:air
fill ~2 ~1 ~30 ~2 ~2 ~30 minecraft:air
fill ~2 ~1 ~32 ~2 ~2 ~32 minecraft:air
fill ~2 ~1 ~34 ~2 ~2 ~35 minecraft:air
fill ~3 ~1 ~2 ~3 ~4 ~3 minecraft:air
fill ~3 ~1 ~5 ~4 ~3 ~10 minecraft:air
fill ~3 ~1 ~17 ~3 ~2 ~17 minecraft:air
fill ~3 ~1 ~19 ~4 ~2 ~19 minecraft:air
fill ~3 ~1 ~21 ~3 ~2 ~24 minecraft:air
fill ~3 ~1 ~29 ~3 ~2 ~35 minecraft:air
fill ~4 ~1 ~1 ~5 ~3 ~3 minecraft:air
fill ~4 ~1 ~15 ~4 ~2 ~17 minecraft:air
fill ~4 ~1 ~20 ~4 ~2 ~21 minecraft:air
fill ~4 ~1 ~23 ~4 ~8 ~26 minecraft:air
fill ~4 ~1 ~28 ~4 ~2 ~29 minecraft:air
fill ~4 ~1 ~31 ~4 ~2 ~33 minecraft:air
fill ~4 ~1 ~35 ~4 ~8 ~35 minecraft:air
fill ~5 ~1 ~4 ~5 ~3 ~10 minecraft:air
fill ~5 ~1 ~14 ~5 ~8 ~34 minecraft:air
fill ~6 ~1 ~2 ~6 ~3 ~10 minecraft:air
fill ~6 ~1 ~13 ~12 ~8 ~20 minecraft:air
fill ~6 ~1 ~21 ~11 ~8 ~34 minecraft:air
fill ~7 ~1 ~1 ~8 ~3 ~7 minecraft:air
fill ~7 ~1 ~8 ~7 ~3 ~10 minecraft:air
fill ~7 ~1 ~35 ~8 ~8 ~35 minecraft:air
fill ~8 ~1 ~9 ~8 ~3 ~10 minecraft:air
fill ~9 ~1 ~2 ~9 ~4 ~8 minecraft:air
fill ~9 ~1 ~10 ~9 ~3 ~10 minecraft:air
fill ~10 ~1 ~1 ~11 ~3 ~10 minecraft:air
fill ~10 ~1 ~35 ~11 ~8 ~35 minecraft:air
fill ~11 ~1 ~36 ~11 ~8 ~36 minecraft:air
fill ~12 ~1 ~2 ~25 ~4 ~2 minecraft:air
fill ~12 ~1 ~3 ~19 ~4 ~8 minecraft:air
fill ~12 ~1 ~9 ~14 ~3 ~10 minecraft:air
fill ~12 ~1 ~28 ~18 ~8 ~36 minecraft:air
fill ~13 ~1 ~1 ~29 ~3 ~1 minecraft:air
fill ~13 ~1 ~12 ~17 ~8 ~20 minecraft:air
fill ~15 ~1 ~10 ~15 ~3 ~10 minecraft:air
fill ~16 ~1 ~9 ~19 ~3 ~10 minecraft:air
fill ~18 ~1 ~13 ~20 ~8 ~20 minecraft:air
fill ~19 ~1 ~21 ~20 ~8 ~35 minecraft:air
fill ~19 ~1 ~36 ~19 ~8 ~36 minecraft:air
fill ~20 ~1 ~4 ~21 ~3 ~10 minecraft:air
fill ~21 ~1 ~12 ~21 ~8 ~35 minecraft:air
fill ~22 ~1 ~3 ~27 ~4 ~6 minecraft:air
fill ~22 ~1 ~8 ~22 ~3 ~9 minecraft:air
fill ~22 ~1 ~13 ~22 ~8 ~35 minecraft:air
fill ~23 ~1 ~7 ~27 ~3 ~10 minecraft:air
fill ~23 ~1 ~12 ~23 ~8 ~35 minecraft:air
fill ~24 ~1 ~13 ~29 ~8 ~13 minecraft:air
fill ~24 ~1 ~14 ~24 ~8 ~14 minecraft:air
fill ~24 ~1 ~17 ~24 ~8 ~35 minecraft:air
fill ~25 ~1 ~15 ~25 ~1 ~16 minecraft:air
fill ~25 ~1 ~18 ~25 ~8 ~35 minecraft:air
fill ~26 ~1 ~14 ~26 ~1 ~18 minecraft:air
fill ~26 ~1 ~20 ~26 ~8 ~35 minecraft:air
fill ~27 ~1 ~15 ~27 ~1 ~16 minecraft:air
fill ~27 ~1 ~18 ~27 ~8 ~28 minecraft:air
fill ~27 ~1 ~31 ~27 ~8 ~35 minecraft:air
fill ~28 ~1 ~2 ~29 ~4 ~6 minecraft:air
fill ~28 ~1 ~8 ~28 ~3 ~9 minecraft:air
fill ~28 ~1 ~14 ~29 ~8 ~14 minecraft:air
fill ~28 ~1 ~17 ~28 ~1 ~35 minecraft:air
fill ~29 ~1 ~7 ~29 ~3 ~10 minecraft:air
fill ~29 ~1 ~15 ~29 ~8 ~28 minecraft:air
fill ~29 ~1 ~30 ~29 ~1 ~31 minecraft:air
fill ~29 ~1 ~33 ~29 ~8 ~35 minecraft:air
fill ~30 ~1 ~22 ~30 ~4 ~26 minecraft:air
setblock ~1 ~2 ~22 minecraft:air
setblock ~1 ~2 ~28 minecraft:air
setblock ~1 ~2 ~30 minecraft:air
setblock ~1 ~2 ~32 minecraft:air
setblock ~2 ~2 ~19 minecraft:air
setblock ~2 ~2 ~21 minecraft:air
setblock ~2 ~2 ~29 minecraft:air
setblock ~2 ~2 ~31 minecraft:air
setblock ~2 ~2 ~33 minecraft:air
fill ~3 ~2 ~4 ~4 ~5 ~4 minecraft:air
setblock ~3 ~2 ~16 minecraft:air
setblock ~3 ~2 ~18 minecraft:air
setblock ~3 ~2 ~20 minecraft:air
fill ~3 ~2 ~25 ~3 ~8 ~25 minecraft:air
fill ~6 ~2 ~35 ~6 ~8 ~35 minecraft:air
fill ~8 ~2 ~8 ~8 ~4 ~8 minecraft:air
fill ~9 ~2 ~9 ~9 ~4 ~9 minecraft:air
fill ~20 ~2 ~3 ~21 ~4 ~3 minecraft:air
fill ~24 ~2 ~12 ~24 ~8 ~12 minecraft:air
fill ~26 ~2 ~2 ~27 ~4 ~2 minecraft:air
fill ~26 ~2 ~19 ~26 ~8 ~19 minecraft:air
fill ~27 ~2 ~29 ~27 ~8 ~30 minecraft:air
fill ~28 ~2 ~17 ~28 ~8 ~28 minecraft:air
fill ~28 ~2 ~30 ~28 ~8 ~35 minecraft:air
fill ~29 ~2 ~12 ~29 ~8 ~12 minecraft:air
fill ~29 ~2 ~29 ~29 ~8 ~29 minecraft:air
fill ~29 ~2 ~31 ~29 ~8 ~32 minecraft:air
fill ~0 ~3 ~13 ~0 ~8 ~13 minecraft:air
fill ~0 ~3 ~23 ~3 ~8 ~23 minecraft:air
fill ~0 ~3 ~25 ~2 ~8 ~25 minecraft:air
fill ~0 ~3 ~35 ~3 ~8 ~35 minecraft:air
fill ~1 ~3 ~24 ~3 ~8 ~24 minecraft:air
fill ~1 ~3 ~26 ~3 ~8 ~26 minecraft:air
fill ~1 ~3 ~36 ~1 ~8 ~36 minecraft:air
setblock ~3 ~3 ~1 minecraft:air
fill ~3 ~3 ~36 ~3 ~8 ~36 minecraft:air
fill ~5 ~3 ~35 ~5 ~8 ~36 minecraft:air
setblock ~6 ~3 ~1 minecraft:air
fill ~6 ~3 ~12 ~12 ~8 ~12 minecraft:air
fill ~7 ~3 ~36 ~7 ~8 ~36 minecraft:air
setblock ~9 ~3 ~1 minecraft:air
fill ~9 ~3 ~35 ~9 ~8 ~36 minecraft:air
setblock ~12 ~3 ~1 minecraft:air
fill ~15 ~3 ~9 ~15 ~4 ~9 minecraft:air
fill ~18 ~3 ~12 ~20 ~8 ~12 minecraft:air
fill ~21 ~3 ~36 ~21 ~8 ~36 minecraft:air
fill ~23 ~3 ~36 ~23 ~8 ~36 minecraft:air
fill ~24 ~3 ~15 ~24 ~8 ~16 minecraft:air
fill ~25 ~3 ~12 ~28 ~8 ~12 minecraft:air
fill ~25 ~3 ~14 ~27 ~8 ~14 minecraft:air
fill ~25 ~3 ~16 ~25 ~8 ~17 minecraft:air
fill ~25 ~3 ~36 ~25 ~8 ~36 minecraft:air
fill ~26 ~3 ~17 ~27 ~8 ~17 minecraft:air
fill ~26 ~3 ~18 ~26 ~8 ~18 minecraft:air
fill ~27 ~3 ~15 ~28 ~8 ~15 minecraft:air
fill ~27 ~3 ~36 ~27 ~8 ~36 minecraft:air
fill ~28 ~3 ~16 ~28 ~8 ~16 minecraft:air
fill ~28 ~3 ~29 ~28 ~8 ~29 minecraft:air
fill ~29 ~3 ~30 ~29 ~8 ~30 minecraft:air
fill ~29 ~3 ~36 ~29 ~8 ~36 minecraft:air
fill ~30 ~3 ~13 ~30 ~8 ~13 minecraft:air
fill ~30 ~3 ~15 ~30 ~8 ~15 minecraft:air
fill ~30 ~3 ~17 ~30 ~8 ~17 minecraft:air
fill ~30 ~3 ~19 ~30 ~8 ~20 minecraft:air
fill ~30 ~3 ~28 ~30 ~8 ~29 minecraft:air
fill ~30 ~3 ~31 ~30 ~8 ~31 minecraft:air
fill ~30 ~3 ~33 ~30 ~8 ~33 minecraft:air
fill ~30 ~3 ~35 ~30 ~8 ~35 minecraft:air
fill ~0 ~4 ~12 ~0 ~8 ~12 minecraft:air
fill ~0 ~4 ~14 ~4 ~8 ~22 minecraft:air
fill ~0 ~4 ~24 ~0 ~8 ~24 minecraft:air
fill ~0 ~4 ~26 ~0 ~8 ~34 minecraft:air
fill ~0 ~4 ~36 ~0 ~8 ~36 minecraft:air
fill ~1 ~4 ~2 ~2 ~4 ~9 minecraft:air
fill ~1 ~4 ~27 ~4 ~8 ~34 minecraft:air
fill ~2 ~4 ~36 ~2 ~8 ~36 minecraft:air
fill ~3 ~4 ~5 ~4 ~4 ~9 minecraft:air
fill ~4 ~4 ~2 ~8 ~4 ~3 minecraft:air
fill ~4 ~4 ~36 ~4 ~8 ~36 minecraft:air
fill ~5 ~4 ~4 ~8 ~5 ~7 minecraft:air
fill ~5 ~4 ~8 ~7 ~4 ~9 minecraft:air
fill ~6 ~4 ~36 ~6 ~8 ~36 minecraft:air
setblock ~8 ~4 ~9 minecraft:air
fill ~8 ~4 ~36 ~8 ~8 ~36 minecraft:air
fill ~10 ~4 ~2 ~11 ~4 ~9 minecraft:air
fill ~10 ~4 ~36 ~10 ~8 ~36 minecraft:air
fill ~12 ~4 ~9 ~14 ~4 ~9 minecraft:air
fill ~16 ~4 ~9 ~19 ~4 ~9 minecraft:air
fill ~20 ~4 ~4 ~21 ~4 ~9 minecraft:air
fill ~20 ~4 ~36 ~20 ~8 ~36 minecraft:air
fill ~22 ~4 ~36 ~22 ~8 ~36 minecraft:air
fill ~24 ~4 ~36 ~24 ~8 ~36 minecraft:air
fill ~25 ~4 ~15 ~26 ~8 ~15 minecraft:air
fill ~26 ~4 ~16 ~27 ~8 ~16 minecraft:air
fill ~26 ~4 ~36 ~26 ~8 ~36 minecraft:air
fill ~28 ~4 ~36 ~28 ~8 ~36 minecraft:air
fill ~29 ~4 ~7 ~29 ~4 ~9 minecraft:air
fill ~30 ~4 ~12 ~30 ~8 ~12 minecraft:air
fill ~30 ~4 ~14 ~30 ~8 ~14 minecraft:air
fill ~30 ~4 ~16 ~30 ~8 ~16 minecraft:air
fill ~30 ~4 ~18 ~30 ~8 ~18 minecraft:air
fill ~30 ~4 ~30 ~30 ~8 ~30 minecraft:air
fill ~30 ~4 ~32 ~30 ~8 ~32 minecraft:air
fill ~30 ~4 ~34 ~30 ~8 ~34 minecraft:air
fill ~30 ~4 ~36 ~30 ~8 ~36 minecraft:air
fill ~0 ~5 ~0 ~30 ~8 ~1 minecraft:air
fill ~0 ~5 ~10 ~21 ~8 ~11 minecraft:air
fill ~1 ~5 ~4 ~2 ~5 ~7 minecraft:air
fill ~3 ~5 ~5 ~4 ~5 ~7 minecraft:air
fill ~9 ~5 ~4 ~29 ~5 ~6 minecraft:air
fill ~9 ~5 ~7 ~21 ~5 ~7 minecraft:air
setblock ~29 ~5 ~7 minecraft:air
fill ~29 ~5 ~10 ~30 ~8 ~11 minecraft:air
fill ~0 ~6 ~2 ~30 ~8 ~3 minecraft:air
fill ~0 ~6 ~8 ~21 ~8 ~9 minecraft:air
fill ~22 ~6 ~12 ~22 ~8 ~12 minecraft:air
fill ~29 ~6 ~8 ~30 ~8 ~9 minecraft:air
fill ~30 ~6 ~21 ~30 ~8 ~27 minecraft:air
fill ~0 ~7 ~4 ~30 ~8 ~7 minecraft:air
fill ~22 ~7 ~8 ~28 ~8 ~11 minecraft:air

# the ground layer: flags on the floor of the storehouse, the paved capture circle (stone brick, a ring of polished blackstone, a plinth), flags, gravel and trodden earth in the yard, a flagged mouth at the gate
fill ~0 ~0 ~0 ~1 ~0 ~0 minecraft:stone_bricks
setblock ~0 ~0 ~1 minecraft:andesite
setblock ~0 ~0 ~2 minecraft:stone_bricks
fill ~0 ~0 ~3 ~1 ~0 ~3 minecraft:cracked_stone_bricks
setblock ~0 ~0 ~4 minecraft:andesite
fill ~0 ~0 ~5 ~0 ~0 ~8 minecraft:stone_bricks
setblock ~0 ~0 ~9 minecraft:cracked_stone_bricks
setblock ~0 ~0 ~10 minecraft:cobblestone
setblock ~0 ~0 ~11 minecraft:stone_bricks
fill ~0 ~0 ~12 ~1 ~0 ~12 minecraft:cracked_stone_bricks
setblock ~0 ~0 ~13 minecraft:cobblestone
setblock ~0 ~0 ~14 minecraft:gravel
fill ~0 ~0 ~15 ~2 ~0 ~15 minecraft:andesite
fill ~0 ~0 ~16 ~0 ~0 ~17 minecraft:gravel
setblock ~0 ~0 ~18 minecraft:andesite
setblock ~0 ~0 ~19 minecraft:cobblestone
fill ~0 ~0 ~20 ~0 ~0 ~22 minecraft:andesite
fill ~0 ~0 ~23 ~2 ~0 ~23 minecraft:cobblestone
setblock ~0 ~0 ~24 minecraft:andesite
setblock ~0 ~0 ~25 minecraft:cracked_stone_bricks
setblock ~0 ~0 ~26 minecraft:cobblestone
setblock ~0 ~0 ~27 minecraft:stone
setblock ~0 ~0 ~28 minecraft:cobblestone
setblock ~0 ~0 ~29 minecraft:andesite
setblock ~0 ~0 ~30 minecraft:gravel
setblock ~0 ~0 ~31 minecraft:coarse_dirt
fill ~0 ~0 ~32 ~2 ~0 ~32 minecraft:andesite
fill ~0 ~0 ~33 ~1 ~0 ~33 minecraft:gravel
setblock ~0 ~0 ~34 minecraft:coarse_dirt
fill ~0 ~0 ~35 ~0 ~0 ~36 minecraft:cobblestone
setblock ~1 ~0 ~1 minecraft:stone_bricks
setblock ~1 ~0 ~2 minecraft:andesite
fill ~1 ~0 ~4 ~2 ~0 ~4 minecraft:cracked_stone_bricks
setblock ~1 ~0 ~5 minecraft:cobblestone
setblock ~1 ~0 ~6 minecraft:andesite
setblock ~1 ~0 ~7 minecraft:cobblestone
fill ~1 ~0 ~8 ~2 ~0 ~8 minecraft:cracked_stone_bricks
setblock ~1 ~0 ~9 minecraft:cobblestone
setblock ~1 ~0 ~10 minecraft:stone_bricks
setblock ~1 ~0 ~11 minecraft:andesite
fill ~1 ~0 ~13 ~1 ~0 ~14 minecraft:gravel
setblock ~1 ~0 ~16 minecraft:cobblestone
setblock ~1 ~0 ~17 minecraft:andesite
setblock ~1 ~0 ~18 minecraft:gravel
setblock ~1 ~0 ~19 minecraft:cracked_stone_bricks
fill ~1 ~0 ~20 ~1 ~0 ~21 minecraft:cobblestone
fill ~1 ~0 ~22 ~2 ~0 ~22 minecraft:gravel
fill ~1 ~0 ~24 ~2 ~0 ~24 minecraft:cobblestone
fill ~1 ~0 ~25 ~1 ~0 ~26 minecraft:gravel
setblock ~1 ~0 ~27 minecraft:andesite
setblock ~1 ~0 ~28 minecraft:stone
fill ~1 ~0 ~29 ~2 ~0 ~29 minecraft:cobblestone
setblock ~1 ~0 ~30 minecraft:cobblestone
setblock ~1 ~0 ~31 minecraft:cracked_stone_bricks
setblock ~1 ~0 ~34 minecraft:stone
setblock ~1 ~0 ~35 minecraft:andesite
setblock ~1 ~0 ~36 minecraft:gravel
fill ~2 ~0 ~0 ~3 ~0 ~1 minecraft:cracked_stone_bricks
fill ~2 ~0 ~2 ~2 ~0 ~3 minecraft:stone_bricks
fill ~2 ~0 ~5 ~2 ~0 ~6 minecraft:andesite
setblock ~2 ~0 ~7 minecraft:stone_bricks
fill ~2 ~0 ~9 ~2 ~0 ~10 minecraft:andesite
setblock ~2 ~0 ~11 minecraft:stone_bricks
fill ~2 ~0 ~12 ~3 ~0 ~12 minecraft:gravel
setblock ~2 ~0 ~13 minecraft:andesite
setblock ~2 ~0 ~14 minecraft:cobblestone
fill ~2 ~0 ~16 ~2 ~0 ~17 minecraft:gravel
setblock ~2 ~0 ~18 minecraft:cobblestone
fill ~2 ~0 ~19 ~3 ~0 ~19 minecraft:andesite
setblock ~2 ~0 ~20 minecraft:andesite
setblock ~2 ~0 ~21 minecraft:coarse_dirt
setblock ~2 ~0 ~25 minecraft:andesite
fill ~2 ~0 ~26 ~2 ~0 ~27 minecraft:cobblestone
setblock ~2 ~0 ~28 minecraft:andesite
setblock ~2 ~0 ~30 minecraft:coarse_dirt
setblock ~2 ~0 ~31 minecraft:stone
setblock ~2 ~0 ~33 minecraft:cobblestone
fill ~2 ~0 ~34 ~2 ~0 ~35 minecraft:gravel
setblock ~2 ~0 ~36 minecraft:cobblestone
setblock ~3 ~0 ~2 minecraft:cobblestone
setblock ~3 ~0 ~3 minecraft:cracked_stone_bricks
setblock ~3 ~0 ~4 minecraft:andesite
fill ~3 ~0 ~5 ~4 ~0 ~5 minecraft:cracked_stone_bricks
fill ~3 ~0 ~6 ~4 ~0 ~6 minecraft:andesite
setblock ~3 ~0 ~7 minecraft:cracked_stone_bricks
setblock ~3 ~0 ~8 minecraft:andesite
setblock ~3 ~0 ~9 minecraft:cracked_stone_bricks
fill ~3 ~0 ~10 ~3 ~0 ~11 minecraft:stone_bricks
setblock ~3 ~0 ~13 minecraft:stone
setblock ~3 ~0 ~14 minecraft:gravel
setblock ~3 ~0 ~15 minecraft:cracked_stone_bricks
fill ~3 ~0 ~16 ~4 ~0 ~16 minecraft:cobblestone
fill ~3 ~0 ~17 ~3 ~0 ~18 minecraft:gravel
setblock ~3 ~0 ~20 minecraft:cracked_stone_bricks
setblock ~3 ~0 ~21 minecraft:stone
setblock ~3 ~0 ~22 minecraft:cobblestone
fill ~3 ~0 ~23 ~3 ~0 ~24 minecraft:coarse_dirt
setblock ~3 ~0 ~25 minecraft:cobblestone
setblock ~3 ~0 ~26 minecraft:gravel
setblock ~3 ~0 ~27 minecraft:cobblestone
setblock ~3 ~0 ~28 minecraft:gravel
setblock ~3 ~0 ~29 minecraft:stone
fill ~3 ~0 ~30 ~3 ~0 ~31 minecraft:cracked_stone_bricks
fill ~3 ~0 ~32 ~4 ~0 ~32 minecraft:gravel
setblock ~3 ~0 ~33 minecraft:stone
fill ~3 ~0 ~34 ~4 ~0 ~34 minecraft:cobblestone
fill ~3 ~0 ~35 ~4 ~0 ~36 minecraft:gravel
setblock ~4 ~0 ~0 minecraft:cobblestone
setblock ~4 ~0 ~1 minecraft:cracked_stone_bricks
fill ~4 ~0 ~2 ~4 ~0 ~4 minecraft:stone_bricks
setblock ~4 ~0 ~7 minecraft:andesite
fill ~4 ~0 ~8 ~5 ~0 ~9 minecraft:stone_bricks
setblock ~4 ~0 ~10 minecraft:cobblestone
fill ~4 ~0 ~11 ~7 ~0 ~11 minecraft:cracked_stone_bricks
fill ~4 ~0 ~12 ~5 ~0 ~12 minecraft:cobblestone
setblock ~4 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~4 ~0 ~14 minecraft:andesite
setblock ~4 ~0 ~15 minecraft:gravel
setblock ~4 ~0 ~17 minecraft:coarse_dirt
setblock ~4 ~0 ~18 minecraft:cobblestone
fill ~4 ~0 ~19 ~4 ~0 ~20 minecraft:gravel
setblock ~4 ~0 ~21 minecraft:andesite
setblock ~4 ~0 ~22 minecraft:stone
setblock ~4 ~0 ~23 minecraft:andesite
setblock ~4 ~0 ~24 minecraft:cobblestone
setblock ~4 ~0 ~25 minecraft:coarse_dirt
setblock ~4 ~0 ~26 minecraft:cobblestone
setblock ~4 ~0 ~27 minecraft:coarse_dirt
setblock ~4 ~0 ~28 minecraft:andesite
fill ~4 ~0 ~29 ~4 ~0 ~30 minecraft:cobblestone
setblock ~4 ~0 ~31 minecraft:andesite
setblock ~4 ~0 ~33 minecraft:gravel
setblock ~5 ~0 ~0 minecraft:cracked_stone_bricks
fill ~5 ~0 ~1 ~6 ~0 ~1 minecraft:stone_bricks
fill ~5 ~0 ~2 ~6 ~0 ~2 minecraft:cracked_stone_bricks
setblock ~5 ~0 ~3 minecraft:cracked_stone_bricks
setblock ~5 ~0 ~4 minecraft:stone_bricks
setblock ~5 ~0 ~5 minecraft:andesite
setblock ~5 ~0 ~6 minecraft:stone_bricks
setblock ~5 ~0 ~7 minecraft:cracked_stone_bricks
setblock ~5 ~0 ~10 minecraft:stone_bricks
setblock ~5 ~0 ~13 minecraft:andesite
fill ~5 ~0 ~14 ~5 ~0 ~15 minecraft:cobblestone
setblock ~5 ~0 ~16 minecraft:andesite
fill ~5 ~0 ~17 ~5 ~0 ~18 minecraft:cobblestone
fill ~5 ~0 ~19 ~5 ~0 ~20 minecraft:andesite
setblock ~5 ~0 ~21 minecraft:cobblestone
setblock ~5 ~0 ~22 minecraft:gravel
setblock ~5 ~0 ~23 minecraft:stone
setblock ~5 ~0 ~24 minecraft:polished_blackstone
fill ~5 ~0 ~25 ~5 ~0 ~27 minecraft:cobblestone
setblock ~5 ~0 ~28 minecraft:gravel
setblock ~5 ~0 ~29 minecraft:coarse_dirt
setblock ~5 ~0 ~30 minecraft:cobblestone
setblock ~5 ~0 ~31 minecraft:cracked_stone_bricks
fill ~5 ~0 ~32 ~6 ~0 ~32 minecraft:coarse_dirt
setblock ~5 ~0 ~33 minecraft:stone
setblock ~5 ~0 ~34 minecraft:gravel
setblock ~5 ~0 ~35 minecraft:andesite
setblock ~5 ~0 ~36 minecraft:cobblestone
setblock ~6 ~0 ~0 minecraft:andesite
setblock ~6 ~0 ~3 minecraft:stone_bricks
fill ~6 ~0 ~4 ~6 ~0 ~5 minecraft:cobblestone
fill ~6 ~0 ~6 ~7 ~0 ~6 minecraft:cracked_stone_bricks
setblock ~6 ~0 ~7 minecraft:stone_bricks
setblock ~6 ~0 ~8 minecraft:andesite
setblock ~6 ~0 ~9 minecraft:stone_bricks
setblock ~6 ~0 ~10 minecraft:andesite
fill ~6 ~0 ~12 ~6 ~0 ~13 minecraft:andesite
setblock ~6 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~6 ~0 ~15 minecraft:coarse_dirt
setblock ~6 ~0 ~16 minecraft:cobblestone
setblock ~6 ~0 ~17 minecraft:cracked_stone_bricks
fill ~6 ~0 ~18 ~6 ~0 ~19 minecraft:andesite
fill ~6 ~0 ~20 ~6 ~0 ~23 minecraft:polished_blackstone
setblock ~6 ~0 ~24 minecraft:polished_andesite
fill ~6 ~0 ~25 ~6 ~0 ~28 minecraft:polished_blackstone
fill ~6 ~0 ~29 ~6 ~0 ~31 minecraft:andesite
setblock ~6 ~0 ~33 minecraft:cracked_stone_bricks
setblock ~6 ~0 ~34 minecraft:cobblestone
fill ~6 ~0 ~35 ~8 ~0 ~35 minecraft:gravel
setblock ~6 ~0 ~36 minecraft:andesite
setblock ~7 ~0 ~0 minecraft:cobblestone
setblock ~7 ~0 ~1 minecraft:andesite
fill ~7 ~0 ~2 ~7 ~0 ~3 minecraft:stone_bricks
setblock ~7 ~0 ~4 minecraft:cracked_stone_bricks
setblock ~7 ~0 ~5 minecraft:cobblestone
fill ~7 ~0 ~7 ~7 ~0 ~8 minecraft:andesite
setblock ~7 ~0 ~9 minecraft:cobblestone
fill ~7 ~0 ~10 ~9 ~0 ~10 minecraft:cracked_stone_bricks
setblock ~7 ~0 ~12 minecraft:cobblestone
setblock ~7 ~0 ~13 minecraft:gravel
fill ~7 ~0 ~14 ~7 ~0 ~16 minecraft:cobblestone
setblock ~7 ~0 ~17 minecraft:stone
fill ~7 ~0 ~18 ~8 ~0 ~18 minecraft:polished_blackstone
setblock ~7 ~0 ~19 minecraft:polished_blackstone
setblock ~7 ~0 ~20 minecraft:polished_andesite
setblock ~7 ~0 ~21 minecraft:andesite
fill ~7 ~0 ~22 ~7 ~0 ~25 minecraft:stone_bricks
setblock ~7 ~0 ~26 minecraft:cracked_stone_bricks
fill ~7 ~0 ~27 ~8 ~0 ~27 minecraft:stone_bricks
fill ~7 ~0 ~28 ~8 ~0 ~28 minecraft:polished_andesite
fill ~7 ~0 ~29 ~7 ~0 ~30 minecraft:polished_blackstone
setblock ~7 ~0 ~31 minecraft:cobblestone
setblock ~7 ~0 ~32 minecraft:gravel
setblock ~7 ~0 ~33 minecraft:stone
setblock ~7 ~0 ~34 minecraft:coarse_dirt
setblock ~7 ~0 ~36 minecraft:stone
setblock ~8 ~0 ~0 minecraft:andesite
setblock ~8 ~0 ~1 minecraft:cobblestone
setblock ~8 ~0 ~2 minecraft:andesite
setblock ~8 ~0 ~3 minecraft:cobblestone
setblock ~8 ~0 ~4 minecraft:andesite
setblock ~8 ~0 ~5 minecraft:cracked_stone_bricks
fill ~8 ~0 ~6 ~9 ~0 ~6 minecraft:stone_bricks
setblock ~8 ~0 ~7 minecraft:cobblestone
fill ~8 ~0 ~8 ~11 ~0 ~8 minecraft:stone_bricks
fill ~8 ~0 ~9 ~9 ~0 ~9 minecraft:andesite
setblock ~8 ~0 ~11 minecraft:cobblestone
setblock ~8 ~0 ~12 minecraft:coarse_dirt
setblock ~8 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~14 minecraft:coarse_dirt
fill ~8 ~0 ~15 ~8 ~0 ~16 minecraft:cobblestone
fill ~8 ~0 ~17 ~9 ~0 ~17 minecraft:polished_blackstone
fill ~8 ~0 ~19 ~8 ~0 ~21 minecraft:stone_bricks
setblock ~8 ~0 ~22 minecraft:polished_andesite
setblock ~8 ~0 ~23 minecraft:stone_bricks
fill ~8 ~0 ~24 ~10 ~0 ~24 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~25 minecraft:stone_bricks
setblock ~8 ~0 ~26 minecraft:andesite
setblock ~8 ~0 ~29 minecraft:stone_bricks
fill ~8 ~0 ~30 ~8 ~0 ~31 minecraft:polished_blackstone
fill ~8 ~0 ~32 ~8 ~0 ~33 minecraft:cobblestone
setblock ~8 ~0 ~34 minecraft:stone
setblock ~8 ~0 ~36 minecraft:cobblestone
setblock ~9 ~0 ~0 minecraft:cracked_stone_bricks
fill ~9 ~0 ~1 ~11 ~0 ~1 minecraft:andesite
fill ~9 ~0 ~2 ~10 ~0 ~2 minecraft:stone_bricks
fill ~9 ~0 ~3 ~9 ~0 ~4 minecraft:stone_bricks
setblock ~9 ~0 ~5 minecraft:andesite
setblock ~9 ~0 ~7 minecraft:andesite
setblock ~9 ~0 ~11 minecraft:stone_bricks
setblock ~9 ~0 ~12 minecraft:cracked_stone_bricks
setblock ~9 ~0 ~13 minecraft:gravel
setblock ~9 ~0 ~14 minecraft:cobblestone
setblock ~9 ~0 ~15 minecraft:gravel
fill ~9 ~0 ~16 ~10 ~0 ~16 minecraft:polished_blackstone
setblock ~9 ~0 ~18 minecraft:andesite
fill ~9 ~0 ~19 ~9 ~0 ~20 minecraft:mossy_stone_bricks
setblock ~9 ~0 ~21 minecraft:andesite
setblock ~9 ~0 ~22 minecraft:stone_bricks
setblock ~9 ~0 ~23 minecraft:polished_andesite
setblock ~9 ~0 ~25 minecraft:mossy_stone_bricks
fill ~9 ~0 ~26 ~9 ~0 ~27 minecraft:polished_andesite
fill ~9 ~0 ~28 ~10 ~0 ~28 minecraft:stone_bricks
setblock ~9 ~0 ~29 minecraft:cracked_stone_bricks
setblock ~9 ~0 ~30 minecraft:stone_bricks
fill ~9 ~0 ~31 ~9 ~0 ~32 minecraft:polished_blackstone
setblock ~9 ~0 ~33 minecraft:gravel
setblock ~9 ~0 ~34 minecraft:coarse_dirt
fill ~9 ~0 ~35 ~9 ~0 ~36 minecraft:cobblestone
setblock ~10 ~0 ~0 minecraft:stone_bricks
setblock ~10 ~0 ~3 minecraft:andesite
fill ~10 ~0 ~4 ~10 ~0 ~6 minecraft:stone_bricks
setblock ~10 ~0 ~7 minecraft:cracked_stone_bricks
setblock ~10 ~0 ~9 minecraft:stone_bricks
setblock ~10 ~0 ~10 minecraft:andesite
setblock ~10 ~0 ~11 minecraft:cracked_stone_bricks
setblock ~10 ~0 ~12 minecraft:andesite
setblock ~10 ~0 ~13 minecraft:coarse_dirt
setblock ~10 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~10 ~0 ~15 minecraft:andesite
setblock ~10 ~0 ~17 minecraft:stone_bricks
setblock ~10 ~0 ~18 minecraft:polished_andesite
setblock ~10 ~0 ~19 minecraft:stone_bricks
setblock ~10 ~0 ~20 minecraft:polished_andesite
setblock ~10 ~0 ~21 minecraft:stone_bricks
setblock ~10 ~0 ~22 minecraft:polished_andesite
setblock ~10 ~0 ~23 minecraft:stone_bricks
setblock ~10 ~0 ~25 minecraft:cracked_stone_bricks
setblock ~10 ~0 ~26 minecraft:stone_bricks
setblock ~10 ~0 ~27 minecraft:mossy_stone_bricks
fill ~10 ~0 ~29 ~10 ~0 ~31 minecraft:stone_bricks
setblock ~10 ~0 ~32 minecraft:polished_blackstone
setblock ~10 ~0 ~33 minecraft:cracked_stone_bricks
fill ~10 ~0 ~34 ~10 ~0 ~35 minecraft:gravel
setblock ~10 ~0 ~36 minecraft:cobblestone
fill ~11 ~0 ~0 ~12 ~0 ~0 minecraft:andesite
setblock ~11 ~0 ~2 minecraft:andesite
setblock ~11 ~0 ~3 minecraft:cobblestone
setblock ~11 ~0 ~4 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~5 minecraft:cobblestone
setblock ~11 ~0 ~6 minecraft:stone_bricks
setblock ~11 ~0 ~7 minecraft:andesite
setblock ~11 ~0 ~9 minecraft:cobblestone
setblock ~11 ~0 ~10 minecraft:stone_bricks
setblock ~11 ~0 ~11 minecraft:andesite
setblock ~11 ~0 ~12 minecraft:cobblestone
fill ~11 ~0 ~13 ~11 ~0 ~14 minecraft:andesite
fill ~11 ~0 ~15 ~14 ~0 ~15 minecraft:polished_blackstone
setblock ~11 ~0 ~16 minecraft:stone_bricks
setblock ~11 ~0 ~17 minecraft:polished_andesite
setblock ~11 ~0 ~18 minecraft:stone_bricks
setblock ~11 ~0 ~19 minecraft:polished_andesite
fill ~11 ~0 ~20 ~12 ~0 ~20 minecraft:mossy_stone_bricks
fill ~11 ~0 ~21 ~11 ~0 ~22 minecraft:cracked_stone_bricks
fill ~11 ~0 ~23 ~12 ~0 ~23 minecraft:polished_andesite
setblock ~11 ~0 ~24 minecraft:andesite
setblock ~11 ~0 ~25 minecraft:polished_andesite
setblock ~11 ~0 ~26 minecraft:cracked_stone_bricks
fill ~11 ~0 ~27 ~11 ~0 ~29 minecraft:stone_bricks
setblock ~11 ~0 ~30 minecraft:cracked_stone_bricks
fill ~11 ~0 ~31 ~11 ~0 ~32 minecraft:stone_bricks
fill ~11 ~0 ~33 ~14 ~0 ~33 minecraft:polished_blackstone
fill ~11 ~0 ~34 ~11 ~0 ~35 minecraft:stone_bricks
setblock ~11 ~0 ~36 minecraft:cracked_stone_bricks
fill ~12 ~0 ~1 ~12 ~0 ~2 minecraft:cobblestone
setblock ~12 ~0 ~3 minecraft:andesite
fill ~12 ~0 ~4 ~12 ~0 ~5 minecraft:stone_bricks
fill ~12 ~0 ~6 ~12 ~0 ~7 minecraft:andesite
setblock ~12 ~0 ~8 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~9 minecraft:andesite
setblock ~12 ~0 ~10 minecraft:cracked_stone_bricks
fill ~12 ~0 ~11 ~12 ~0 ~13 minecraft:cobblestone
setblock ~12 ~0 ~14 minecraft:andesite
fill ~12 ~0 ~16 ~13 ~0 ~16 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~17 minecraft:stone_bricks
setblock ~12 ~0 ~18 minecraft:andesite
setblock ~12 ~0 ~19 minecraft:stone_bricks
fill ~12 ~0 ~21 ~12 ~0 ~22 minecraft:stone_bricks
setblock ~12 ~0 ~24 minecraft:stone_bricks
setblock ~12 ~0 ~25 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~26 minecraft:stone_bricks
setblock ~12 ~0 ~27 minecraft:mossy_stone_bricks
setblock ~12 ~0 ~28 minecraft:cracked_stone_bricks
fill ~12 ~0 ~29 ~12 ~0 ~31 minecraft:stone_bricks
setblock ~12 ~0 ~32 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~34 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~35 minecraft:cobblestone
setblock ~12 ~0 ~36 minecraft:stone_bricks
setblock ~13 ~0 ~0 minecraft:cobblestone
fill ~13 ~0 ~1 ~14 ~0 ~1 minecraft:andesite
setblock ~13 ~0 ~2 minecraft:andesite
setblock ~13 ~0 ~3 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~4 minecraft:andesite
fill ~13 ~0 ~5 ~13 ~0 ~6 minecraft:stone_bricks
fill ~13 ~0 ~7 ~13 ~0 ~9 minecraft:cracked_stone_bricks
fill ~13 ~0 ~10 ~13 ~0 ~11 minecraft:stone_bricks
setblock ~13 ~0 ~12 minecraft:gravel
fill ~13 ~0 ~13 ~13 ~0 ~14 minecraft:cobblestone
setblock ~13 ~0 ~17 minecraft:andesite
fill ~13 ~0 ~18 ~14 ~0 ~18 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~19 minecraft:mossy_stone_bricks
fill ~13 ~0 ~20 ~17 ~0 ~20 minecraft:stone_bricks
fill ~13 ~0 ~21 ~13 ~0 ~22 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~23 minecraft:stone_bricks
setblock ~13 ~0 ~24 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~25 minecraft:stone_bricks
setblock ~13 ~0 ~26 minecraft:andesite
setblock ~13 ~0 ~27 minecraft:cracked_stone_bricks
fill ~13 ~0 ~28 ~13 ~0 ~29 minecraft:stone_bricks
setblock ~13 ~0 ~30 minecraft:andesite
setblock ~13 ~0 ~31 minecraft:cracked_stone_bricks
fill ~13 ~0 ~32 ~14 ~0 ~32 minecraft:stone_bricks
fill ~13 ~0 ~34 ~13 ~0 ~36 minecraft:stone_bricks
setblock ~14 ~0 ~0 minecraft:stone_bricks
setblock ~14 ~0 ~2 minecraft:stone_bricks
setblock ~14 ~0 ~3 minecraft:andesite
fill ~14 ~0 ~4 ~15 ~0 ~4 minecraft:stone_bricks
setblock ~14 ~0 ~5 minecraft:andesite
fill ~14 ~0 ~6 ~14 ~0 ~10 minecraft:stone_bricks
setblock ~14 ~0 ~11 minecraft:andesite
setblock ~14 ~0 ~12 minecraft:cobblestone
fill ~14 ~0 ~13 ~14 ~0 ~14 minecraft:coarse_dirt
setblock ~14 ~0 ~16 minecraft:andesite
setblock ~14 ~0 ~17 minecraft:stone_bricks
fill ~14 ~0 ~19 ~15 ~0 ~19 minecraft:polished_andesite
setblock ~14 ~0 ~21 minecraft:polished_andesite
setblock ~14 ~0 ~22 minecraft:andesite
fill ~14 ~0 ~23 ~16 ~0 ~25 minecraft:chiseled_stone_bricks
setblock ~14 ~0 ~26 minecraft:stone_bricks
setblock ~14 ~0 ~27 minecraft:mossy_stone_bricks
setblock ~14 ~0 ~28 minecraft:polished_andesite
setblock ~14 ~0 ~29 minecraft:cracked_stone_bricks
setblock ~14 ~0 ~30 minecraft:stone_bricks
setblock ~14 ~0 ~31 minecraft:andesite
setblock ~14 ~0 ~34 minecraft:andesite
setblock ~14 ~0 ~35 minecraft:cracked_stone_bricks
setblock ~14 ~0 ~36 minecraft:andesite
setblock ~15 ~0 ~0 minecraft:cobblestone
setblock ~15 ~0 ~1 minecraft:stone_bricks
fill ~15 ~0 ~2 ~15 ~0 ~3 minecraft:andesite
setblock ~15 ~0 ~5 minecraft:stone_bricks
setblock ~15 ~0 ~6 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~7 minecraft:cobblestone
setblock ~15 ~0 ~8 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~9 minecraft:stone_bricks
setblock ~15 ~0 ~10 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~11 minecraft:stone_bricks
setblock ~15 ~0 ~12 minecraft:cracked_stone_bricks
fill ~15 ~0 ~13 ~17 ~0 ~13 minecraft:cobblestone
setblock ~15 ~0 ~14 minecraft:polished_blackstone
setblock ~15 ~0 ~15 minecraft:polished_andesite
fill ~15 ~0 ~16 ~18 ~0 ~16 minecraft:stone_bricks
fill ~15 ~0 ~17 ~15 ~0 ~18 minecraft:andesite
setblock ~15 ~0 ~21 minecraft:andesite
setblock ~15 ~0 ~22 minecraft:polished_andesite
fill ~15 ~0 ~26 ~15 ~0 ~27 minecraft:andesite
fill ~15 ~0 ~28 ~15 ~0 ~29 minecraft:stone_bricks
setblock ~15 ~0 ~30 minecraft:cracked_stone_bricks
fill ~15 ~0 ~31 ~16 ~0 ~32 minecraft:stone_bricks
setblock ~15 ~0 ~33 minecraft:stone_bricks
setblock ~15 ~0 ~34 minecraft:polished_blackstone
fill ~15 ~0 ~35 ~16 ~0 ~35 minecraft:stone_bricks
setblock ~15 ~0 ~36 minecraft:cobblestone
setblock ~16 ~0 ~0 minecraft:andesite
setblock ~16 ~0 ~1 minecraft:cobblestone
setblock ~16 ~0 ~2 minecraft:cracked_stone_bricks
setblock ~16 ~0 ~3 minecraft:cobblestone
fill ~16 ~0 ~4 ~17 ~0 ~4 minecraft:andesite
fill ~16 ~0 ~5 ~17 ~0 ~5 minecraft:cobblestone
fill ~16 ~0 ~6 ~17 ~0 ~7 minecraft:stone_bricks
fill ~16 ~0 ~8 ~16 ~0 ~9 minecraft:stone_bricks
setblock ~16 ~0 ~10 minecraft:andesite
setblock ~16 ~0 ~11 minecraft:cobblestone
setblock ~16 ~0 ~12 minecraft:andesite
setblock ~16 ~0 ~14 minecraft:stone
fill ~16 ~0 ~15 ~19 ~0 ~15 minecraft:polished_blackstone
fill ~16 ~0 ~17 ~17 ~0 ~17 minecraft:stone_bricks
setblock ~16 ~0 ~18 minecraft:cracked_stone_bricks
setblock ~16 ~0 ~19 minecraft:andesite
fill ~16 ~0 ~21 ~17 ~0 ~21 minecraft:mossy_stone_bricks
setblock ~16 ~0 ~22 minecraft:stone_bricks
setblock ~16 ~0 ~26 minecraft:stone_bricks
setblock ~16 ~0 ~27 minecraft:cracked_stone_bricks
setblock ~16 ~0 ~28 minecraft:polished_andesite
fill ~16 ~0 ~29 ~16 ~0 ~30 minecraft:andesite
fill ~16 ~0 ~33 ~19 ~0 ~33 minecraft:polished_blackstone
fill ~16 ~0 ~34 ~17 ~0 ~34 minecraft:cobblestone
fill ~16 ~0 ~36 ~18 ~0 ~36 minecraft:stone_bricks
fill ~17 ~0 ~0 ~19 ~0 ~1 minecraft:stone_bricks
fill ~17 ~0 ~2 ~18 ~0 ~2 minecraft:cobblestone
fill ~17 ~0 ~3 ~19 ~0 ~3 minecraft:stone_bricks
fill ~17 ~0 ~8 ~18 ~0 ~8 minecraft:cobblestone
setblock ~17 ~0 ~9 minecraft:andesite
fill ~17 ~0 ~10 ~17 ~0 ~11 minecraft:cobblestone
setblock ~17 ~0 ~12 minecraft:stone
fill ~17 ~0 ~14 ~18 ~0 ~14 minecraft:gravel
setblock ~17 ~0 ~18 minecraft:stone_bricks
setblock ~17 ~0 ~19 minecraft:polished_andesite
fill ~17 ~0 ~22 ~17 ~0 ~23 minecraft:mossy_stone_bricks
fill ~17 ~0 ~24 ~17 ~0 ~25 minecraft:stone_bricks
fill ~17 ~0 ~26 ~17 ~0 ~27 minecraft:andesite
setblock ~17 ~0 ~28 minecraft:stone_bricks
setblock ~17 ~0 ~29 minecraft:mossy_stone_bricks
fill ~17 ~0 ~30 ~17 ~0 ~31 minecraft:stone_bricks
setblock ~17 ~0 ~32 minecraft:andesite
fill ~17 ~0 ~35 ~18 ~0 ~35 minecraft:cracked_stone_bricks
fill ~18 ~0 ~4 ~20 ~0 ~4 minecraft:stone_bricks
fill ~18 ~0 ~5 ~18 ~0 ~6 minecraft:stone_bricks
setblock ~18 ~0 ~7 minecraft:andesite
setblock ~18 ~0 ~9 minecraft:cobblestone
setblock ~18 ~0 ~10 minecraft:stone_bricks
setblock ~18 ~0 ~11 minecraft:cracked_stone_bricks
setblock ~18 ~0 ~12 minecraft:andesite
fill ~18 ~0 ~13 ~19 ~0 ~13 minecraft:gravel
setblock ~18 ~0 ~17 minecraft:cracked_stone_bricks
setblock ~18 ~0 ~18 minecraft:polished_andesite
setblock ~18 ~0 ~19 minecraft:stone_bricks
setblock ~18 ~0 ~20 minecraft:andesite
setblock ~18 ~0 ~21 minecraft:stone_bricks
setblock ~18 ~0 ~22 minecraft:polished_andesite
setblock ~18 ~0 ~23 minecraft:stone_bricks
setblock ~18 ~0 ~24 minecraft:polished_andesite
fill ~18 ~0 ~25 ~19 ~0 ~25 minecraft:stone_bricks
setblock ~18 ~0 ~26 minecraft:polished_andesite
setblock ~18 ~0 ~27 minecraft:andesite
setblock ~18 ~0 ~28 minecraft:cracked_stone_bricks
fill ~18 ~0 ~29 ~18 ~0 ~32 minecraft:stone_bricks
fill ~18 ~0 ~34 ~19 ~0 ~34 minecraft:cracked_stone_bricks
setblock ~19 ~0 ~2 minecraft:cracked_stone_bricks
fill ~19 ~0 ~5 ~19 ~0 ~6 minecraft:cracked_stone_bricks
fill ~19 ~0 ~7 ~22 ~0 ~7 minecraft:stone_bricks
fill ~19 ~0 ~8 ~19 ~0 ~9 minecraft:stone_bricks
setblock ~19 ~0 ~10 minecraft:cracked_stone_bricks
fill ~19 ~0 ~11 ~19 ~0 ~12 minecraft:cobblestone
setblock ~19 ~0 ~14 minecraft:andesite
fill ~19 ~0 ~16 ~19 ~0 ~19 minecraft:polished_andesite
setblock ~19 ~0 ~20 minecraft:mossy_stone_bricks
setblock ~19 ~0 ~21 minecraft:polished_andesite
setblock ~19 ~0 ~22 minecraft:cracked_stone_bricks
fill ~19 ~0 ~23 ~20 ~0 ~23 minecraft:andesite
setblock ~19 ~0 ~24 minecraft:andesite
fill ~19 ~0 ~26 ~19 ~0 ~29 minecraft:cracked_stone_bricks
setblock ~19 ~0 ~30 minecraft:andesite
setblock ~19 ~0 ~31 minecraft:polished_andesite
setblock ~19 ~0 ~32 minecraft:stone_bricks
fill ~19 ~0 ~35 ~19 ~0 ~36 minecraft:stone_bricks
setblock ~20 ~0 ~0 minecraft:cracked_stone_bricks
fill ~20 ~0 ~1 ~21 ~0 ~1 minecraft:stone_bricks
setblock ~20 ~0 ~2 minecraft:andesite
setblock ~20 ~0 ~3 minecraft:cracked_stone_bricks
setblock ~20 ~0 ~5 minecraft:cobblestone
setblock ~20 ~0 ~6 minecraft:andesite
setblock ~20 ~0 ~8 minecraft:cracked_stone_bricks
fill ~20 ~0 ~9 ~20 ~0 ~10 minecraft:cobblestone
setblock ~20 ~0 ~11 minecraft:stone_bricks
setblock ~20 ~0 ~12 minecraft:cracked_stone_bricks
fill ~20 ~0 ~13 ~22 ~0 ~13 minecraft:cobblestone
setblock ~20 ~0 ~14 minecraft:coarse_dirt
setblock ~20 ~0 ~15 minecraft:gravel
fill ~20 ~0 ~16 ~21 ~0 ~16 minecraft:polished_blackstone
setblock ~20 ~0 ~17 minecraft:mossy_stone_bricks
setblock ~20 ~0 ~18 minecraft:andesite
setblock ~20 ~0 ~19 minecraft:stone_bricks
fill ~20 ~0 ~20 ~20 ~0 ~21 minecraft:cracked_stone_bricks
fill ~20 ~0 ~22 ~21 ~0 ~22 minecraft:stone_bricks
fill ~20 ~0 ~24 ~20 ~0 ~25 minecraft:stone_bricks
setblock ~20 ~0 ~26 minecraft:mossy_stone_bricks
fill ~20 ~0 ~27 ~20 ~0 ~31 minecraft:stone_bricks
setblock ~20 ~0 ~32 minecraft:polished_blackstone
fill ~20 ~0 ~33 ~20 ~0 ~34 minecraft:stone
setblock ~20 ~0 ~35 minecraft:cobblestone
setblock ~20 ~0 ~36 minecraft:gravel
setblock ~21 ~0 ~0 minecraft:cobblestone
setblock ~21 ~0 ~2 minecraft:cobblestone
setblock ~21 ~0 ~3 minecraft:stone_bricks
setblock ~21 ~0 ~4 minecraft:cracked_stone_bricks
fill ~21 ~0 ~5 ~22 ~0 ~5 minecraft:andesite
setblock ~21 ~0 ~6 minecraft:cobblestone
fill ~21 ~0 ~8 ~22 ~0 ~11 minecraft:stone_bricks
setblock ~21 ~0 ~12 minecraft:gravel
fill ~21 ~0 ~14 ~21 ~0 ~15 minecraft:cobblestone
fill ~21 ~0 ~17 ~22 ~0 ~17 minecraft:polished_blackstone
setblock ~21 ~0 ~18 minecraft:stone_bricks
setblock ~21 ~0 ~19 minecraft:cracked_stone_bricks
setblock ~21 ~0 ~20 minecraft:andesite
setblock ~21 ~0 ~21 minecraft:cracked_stone_bricks
fill ~21 ~0 ~23 ~22 ~0 ~23 minecraft:cracked_stone_bricks
setblock ~21 ~0 ~24 minecraft:andesite
setblock ~21 ~0 ~25 minecraft:stone_bricks
setblock ~21 ~0 ~26 minecraft:polished_andesite
setblock ~21 ~0 ~27 minecraft:andesite
setblock ~21 ~0 ~28 minecraft:cracked_stone_bricks
fill ~21 ~0 ~29 ~22 ~0 ~29 minecraft:stone_bricks
setblock ~21 ~0 ~30 minecraft:stone_bricks
fill ~21 ~0 ~31 ~21 ~0 ~32 minecraft:polished_blackstone
setblock ~21 ~0 ~33 minecraft:gravel
setblock ~21 ~0 ~34 minecraft:cracked_stone_bricks
setblock ~21 ~0 ~35 minecraft:stone
setblock ~21 ~0 ~36 minecraft:coarse_dirt
setblock ~22 ~0 ~0 minecraft:stone_bricks
fill ~22 ~0 ~1 ~22 ~0 ~3 minecraft:andesite
fill ~22 ~0 ~4 ~23 ~0 ~4 minecraft:cobblestone
setblock ~22 ~0 ~6 minecraft:cracked_stone_bricks
setblock ~22 ~0 ~12 minecraft:stone
setblock ~22 ~0 ~14 minecraft:andesite
setblock ~22 ~0 ~15 minecraft:gravel
setblock ~22 ~0 ~16 minecraft:stone
fill ~22 ~0 ~18 ~23 ~0 ~18 minecraft:polished_blackstone
fill ~22 ~0 ~19 ~22 ~0 ~20 minecraft:andesite
setblock ~22 ~0 ~21 minecraft:stone_bricks
setblock ~22 ~0 ~22 minecraft:polished_andesite
setblock ~22 ~0 ~24 minecraft:stone_bricks
setblock ~22 ~0 ~25 minecraft:andesite
setblock ~22 ~0 ~26 minecraft:cracked_stone_bricks
fill ~22 ~0 ~27 ~22 ~0 ~28 minecraft:polished_andesite
fill ~22 ~0 ~30 ~22 ~0 ~31 minecraft:polished_blackstone
fill ~22 ~0 ~32 ~23 ~0 ~32 minecraft:gravel
setblock ~22 ~0 ~33 minecraft:cobblestone
setblock ~22 ~0 ~34 minecraft:gravel
setblock ~22 ~0 ~35 minecraft:cobblestone
setblock ~22 ~0 ~36 minecraft:stone
fill ~23 ~0 ~0 ~23 ~0 ~1 minecraft:cracked_stone_bricks
fill ~23 ~0 ~2 ~23 ~0 ~3 minecraft:stone_bricks
fill ~23 ~0 ~5 ~23 ~0 ~8 minecraft:stone_bricks
fill ~23 ~0 ~9 ~24 ~0 ~9 minecraft:cracked_stone_bricks
fill ~23 ~0 ~10 ~23 ~0 ~11 minecraft:cracked_stone_bricks
fill ~23 ~0 ~12 ~24 ~0 ~12 minecraft:cobblestone
setblock ~23 ~0 ~13 minecraft:coarse_dirt
fill ~23 ~0 ~14 ~23 ~0 ~16 minecraft:gravel
fill ~23 ~0 ~17 ~24 ~0 ~17 minecraft:andesite
setblock ~23 ~0 ~19 minecraft:polished_blackstone
setblock ~23 ~0 ~20 minecraft:stone_bricks
setblock ~23 ~0 ~21 minecraft:andesite
fill ~23 ~0 ~22 ~23 ~0 ~27 minecraft:stone_bricks
setblock ~23 ~0 ~28 minecraft:cracked_stone_bricks
fill ~23 ~0 ~29 ~23 ~0 ~30 minecraft:polished_blackstone
setblock ~23 ~0 ~31 minecraft:cobblestone
fill ~23 ~0 ~33 ~25 ~0 ~33 minecraft:andesite
setblock ~23 ~0 ~34 minecraft:cobblestone
setblock ~23 ~0 ~35 minecraft:stone
setblock ~23 ~0 ~36 minecraft:cobblestone
fill ~24 ~0 ~0 ~25 ~0 ~0 minecraft:stone_bricks
fill ~24 ~0 ~1 ~25 ~0 ~1 minecraft:cracked_stone_bricks
fill ~24 ~0 ~2 ~25 ~0 ~2 minecraft:andesite
fill ~24 ~0 ~3 ~25 ~0 ~3 minecraft:stone_bricks
fill ~24 ~0 ~4 ~25 ~0 ~4 minecraft:andesite
fill ~24 ~0 ~5 ~25 ~0 ~5 minecraft:cracked_stone_bricks
setblock ~24 ~0 ~6 minecraft:cracked_stone_bricks
setblock ~24 ~0 ~7 minecraft:andesite
setblock ~24 ~0 ~8 minecraft:stone_bricks
fill ~24 ~0 ~10 ~27 ~0 ~10 minecraft:stone_bricks
setblock ~24 ~0 ~11 minecraft:andesite
fill ~24 ~0 ~13 ~25 ~0 ~13 minecraft:cobblestone
fill ~24 ~0 ~14 ~24 ~0 ~16 minecraft:cobblestone
fill ~24 ~0 ~18 ~24 ~0 ~19 minecraft:andesite
fill ~24 ~0 ~20 ~24 ~0 ~23 minecraft:polished_blackstone
setblock ~24 ~0 ~24 minecraft:andesite
fill ~24 ~0 ~25 ~24 ~0 ~28 minecraft:polished_blackstone
setblock ~24 ~0 ~29 minecraft:cobblestone
setblock ~24 ~0 ~30 minecraft:andesite
fill ~24 ~0 ~31 ~25 ~0 ~32 minecraft:gravel
fill ~24 ~0 ~34 ~25 ~0 ~34 minecraft:gravel
setblock ~24 ~0 ~35 minecraft:gravel
setblock ~24 ~0 ~36 minecraft:stone
fill ~25 ~0 ~6 ~26 ~0 ~6 minecraft:andesite
setblock ~25 ~0 ~7 minecraft:stone_bricks
setblock ~25 ~0 ~8 minecraft:cracked_stone_bricks
setblock ~25 ~0 ~9 minecraft:cobblestone
setblock ~25 ~0 ~11 minecraft:cobblestone
setblock ~25 ~0 ~12 minecraft:andesite
setblock ~25 ~0 ~14 minecraft:gravel
setblock ~25 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~25 ~0 ~16 minecraft:cobblestone
setblock ~25 ~0 ~17 minecraft:coarse_dirt
setblock ~25 ~0 ~18 minecraft:gravel
fill ~25 ~0 ~19 ~25 ~0 ~20 minecraft:cobblestone
fill ~25 ~0 ~21 ~25 ~0 ~22 minecraft:gravel
setblock ~25 ~0 ~23 minecraft:cobblestone
setblock ~25 ~0 ~24 minecraft:polished_blackstone
fill ~25 ~0 ~25 ~25 ~0 ~26 minecraft:cobblestone
fill ~25 ~0 ~27 ~25 ~0 ~28 minecraft:gravel
setblock ~25 ~0 ~29 minecraft:cracked_stone_bricks
setblock ~25 ~0 ~30 minecraft:cobblestone
setblock ~25 ~0 ~35 minecraft:stone
setblock ~25 ~0 ~36 minecraft:gravel
fill ~26 ~0 ~0 ~26 ~0 ~2 minecraft:cracked_stone_bricks
fill ~26 ~0 ~3 ~27 ~0 ~3 minecraft:andesite
setblock ~26 ~0 ~4 minecraft:cobblestone
fill ~26 ~0 ~5 ~27 ~0 ~5 minecraft:andesite
setblock ~26 ~0 ~7 minecraft:cracked_stone_bricks
setblock ~26 ~0 ~8 minecraft:cobblestone
setblock ~26 ~0 ~9 minecraft:andesite
setblock ~26 ~0 ~11 minecraft:stone_bricks
setblock ~26 ~0 ~12 minecraft:coarse_dirt
setblock ~26 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~26 ~0 ~14 minecraft:cobblestone
fill ~26 ~0 ~15 ~26 ~0 ~16 minecraft:gravel
setblock ~26 ~0 ~17 minecraft:stone
fill ~26 ~0 ~18 ~26 ~0 ~23 minecraft:cobblestone
setblock ~26 ~0 ~24 minecraft:coarse_dirt
setblock ~26 ~0 ~25 minecraft:andesite
fill ~26 ~0 ~26 ~27 ~0 ~26 minecraft:cobblestone
setblock ~26 ~0 ~27 minecraft:cracked_stone_bricks
fill ~26 ~0 ~28 ~26 ~0 ~29 minecraft:stone
setblock ~26 ~0 ~30 minecraft:cracked_stone_bricks
setblock ~26 ~0 ~31 minecraft:coarse_dirt
setblock ~26 ~0 ~32 minecraft:andesite
setblock ~26 ~0 ~33 minecraft:stone
setblock ~26 ~0 ~34 minecraft:cobblestone
fill ~26 ~0 ~35 ~26 ~0 ~36 minecraft:coarse_dirt
setblock ~27 ~0 ~0 minecraft:cobblestone
setblock ~27 ~0 ~1 minecraft:andesite
setblock ~27 ~0 ~2 minecraft:stone_bricks
setblock ~27 ~0 ~4 minecraft:stone_bricks
fill ~27 ~0 ~6 ~28 ~0 ~6 minecraft:stone_bricks
setblock ~27 ~0 ~7 minecraft:andesite
fill ~27 ~0 ~8 ~28 ~0 ~9 minecraft:stone_bricks
setblock ~27 ~0 ~11 minecraft:andesite
fill ~27 ~0 ~12 ~28 ~0 ~12 minecraft:cobblestone
fill ~27 ~0 ~13 ~27 ~0 ~14 minecraft:stone
setblock ~27 ~0 ~15 minecraft:cobblestone
setblock ~27 ~0 ~16 minecraft:gravel
setblock ~27 ~0 ~17 minecraft:coarse_dirt
setblock ~27 ~0 ~18 minecraft:gravel
setblock ~27 ~0 ~19 minecraft:andesite
fill ~27 ~0 ~20 ~30 ~0 ~20 minecraft:cobblestone
setblock ~27 ~0 ~21 minecraft:cobblestone
fill ~27 ~0 ~22 ~28 ~0 ~22 minecraft:coarse_dirt
setblock ~27 ~0 ~23 minecraft:gravel
fill ~27 ~0 ~24 ~27 ~0 ~25 minecraft:cracked_stone_bricks
setblock ~27 ~0 ~27 minecraft:gravel
setblock ~27 ~0 ~28 minecraft:cobblestone
setblock ~27 ~0 ~29 minecraft:stone
fill ~27 ~0 ~30 ~27 ~0 ~32 minecraft:gravel
fill ~27 ~0 ~33 ~28 ~0 ~33 minecraft:cobblestone
setblock ~27 ~0 ~34 minecraft:stone
fill ~27 ~0 ~35 ~27 ~0 ~36 minecraft:cobblestone
setblock ~28 ~0 ~0 minecraft:stone_bricks
setblock ~28 ~0 ~1 minecraft:cracked_stone_bricks
fill ~28 ~0 ~2 ~29 ~0 ~2 minecraft:andesite
setblock ~28 ~0 ~3 minecraft:cracked_stone_bricks
setblock ~28 ~0 ~4 minecraft:andesite
setblock ~28 ~0 ~5 minecraft:cracked_stone_bricks
fill ~28 ~0 ~7 ~29 ~0 ~7 minecraft:stone_bricks
setblock ~28 ~0 ~10 minecraft:andesite
setblock ~28 ~0 ~11 minecraft:stone_bricks
fill ~28 ~0 ~13 ~29 ~0 ~13 minecraft:cobblestone
fill ~28 ~0 ~14 ~29 ~0 ~14 minecraft:andesite
setblock ~28 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~28 ~0 ~16 minecraft:coarse_dirt
fill ~28 ~0 ~17 ~28 ~0 ~19 minecraft:gravel
setblock ~28 ~0 ~21 minecraft:gravel
setblock ~28 ~0 ~23 minecraft:cobblestone
setblock ~28 ~0 ~24 minecraft:andesite
setblock ~28 ~0 ~25 minecraft:cobblestone
setblock ~28 ~0 ~26 minecraft:stone
setblock ~28 ~0 ~27 minecraft:cobblestone
setblock ~28 ~0 ~28 minecraft:cracked_stone_bricks
setblock ~28 ~0 ~29 minecraft:cobblestone
setblock ~28 ~0 ~30 minecraft:cracked_stone_bricks
setblock ~28 ~0 ~31 minecraft:gravel
setblock ~28 ~0 ~32 minecraft:cracked_stone_bricks
setblock ~28 ~0 ~34 minecraft:andesite
setblock ~28 ~0 ~35 minecraft:coarse_dirt
setblock ~28 ~0 ~36 minecraft:andesite
setblock ~29 ~0 ~0 minecraft:cracked_stone_bricks
fill ~29 ~0 ~1 ~30 ~0 ~1 minecraft:stone_bricks
setblock ~29 ~0 ~3 minecraft:stone_bricks
setblock ~29 ~0 ~4 minecraft:cobblestone
fill ~29 ~0 ~5 ~30 ~0 ~5 minecraft:andesite
setblock ~29 ~0 ~6 minecraft:cobblestone
setblock ~29 ~0 ~8 minecraft:cobblestone
fill ~29 ~0 ~9 ~30 ~0 ~9 minecraft:stone_bricks
fill ~29 ~0 ~10 ~30 ~0 ~10 minecraft:cobblestone
setblock ~29 ~0 ~11 minecraft:cobblestone
setblock ~29 ~0 ~12 minecraft:coarse_dirt
setblock ~29 ~0 ~15 minecraft:gravel
setblock ~29 ~0 ~16 minecraft:cracked_stone_bricks
fill ~29 ~0 ~17 ~29 ~0 ~18 minecraft:cobblestone
setblock ~29 ~0 ~19 minecraft:gravel
fill ~29 ~0 ~21 ~30 ~0 ~21 minecraft:cobblestone
setblock ~29 ~0 ~22 minecraft:cobblestone
setblock ~29 ~0 ~23 minecraft:cracked_stone_bricks
fill ~29 ~0 ~24 ~30 ~0 ~24 minecraft:gravel
fill ~29 ~0 ~25 ~30 ~0 ~25 minecraft:andesite
setblock ~29 ~0 ~26 minecraft:gravel
setblock ~29 ~0 ~27 minecraft:andesite
fill ~29 ~0 ~28 ~30 ~0 ~28 minecraft:gravel
setblock ~29 ~0 ~29 minecraft:coarse_dirt
fill ~29 ~0 ~30 ~29 ~0 ~31 minecraft:cobblestone
fill ~29 ~0 ~32 ~29 ~0 ~33 minecraft:andesite
fill ~29 ~0 ~34 ~30 ~0 ~35 minecraft:cobblestone
setblock ~29 ~0 ~36 minecraft:gravel
setblock ~30 ~0 ~0 minecraft:cobblestone
setblock ~30 ~0 ~2 minecraft:cracked_stone_bricks
setblock ~30 ~0 ~3 minecraft:cobblestone
setblock ~30 ~0 ~4 minecraft:cracked_stone_bricks
setblock ~30 ~0 ~6 minecraft:stone_bricks
setblock ~30 ~0 ~7 minecraft:andesite
setblock ~30 ~0 ~8 minecraft:cracked_stone_bricks
setblock ~30 ~0 ~11 minecraft:stone_bricks
setblock ~30 ~0 ~12 minecraft:stone
fill ~30 ~0 ~13 ~30 ~0 ~14 minecraft:andesite
setblock ~30 ~0 ~15 minecraft:cobblestone
setblock ~30 ~0 ~16 minecraft:gravel
setblock ~30 ~0 ~17 minecraft:stone
setblock ~30 ~0 ~18 minecraft:gravel
setblock ~30 ~0 ~19 minecraft:andesite
setblock ~30 ~0 ~22 minecraft:stone
setblock ~30 ~0 ~23 minecraft:cobblestone
setblock ~30 ~0 ~26 minecraft:andesite
setblock ~30 ~0 ~27 minecraft:cobblestone
fill ~30 ~0 ~29 ~30 ~0 ~31 minecraft:cobblestone
setblock ~30 ~0 ~32 minecraft:gravel
setblock ~30 ~0 ~33 minecraft:coarse_dirt
setblock ~30 ~0 ~36 minecraft:stone

# the storehouse walls (y 1..3): courses of stone brick: sound at the foot (y 1), cracked in the middle (y 2) and mossy under the plate (y 3)
setblock ~0 ~1 ~1 minecraft:andesite
fill ~0 ~1 ~2 ~0 ~2 ~2 minecraft:mossy_stone_bricks
setblock ~0 ~1 ~3 minecraft:stone_bricks
fill ~0 ~1 ~4 ~0 ~2 ~4 minecraft:cracked_stone_bricks
fill ~0 ~1 ~6 ~0 ~3 ~6 minecraft:stone_bricks
setblock ~0 ~1 ~7 minecraft:mossy_stone_bricks
setblock ~0 ~1 ~8 minecraft:stone_bricks
fill ~0 ~1 ~9 ~0 ~2 ~9 minecraft:cracked_stone_bricks
fill ~0 ~1 ~10 ~0 ~2 ~10 minecraft:stone_bricks
fill ~1 ~1 ~0 ~1 ~3 ~0 minecraft:stone_bricks
setblock ~1 ~1 ~11 minecraft:mossy_stone_bricks
fill ~2 ~1 ~0 ~4 ~1 ~0 minecraft:cracked_stone_bricks
fill ~2 ~1 ~11 ~3 ~1 ~11 minecraft:stone_bricks
fill ~4 ~1 ~11 ~4 ~2 ~11 minecraft:cracked_stone_bricks
setblock ~6 ~1 ~0 minecraft:mossy_stone_bricks
setblock ~6 ~1 ~11 minecraft:andesite
fill ~7 ~1 ~0 ~8 ~1 ~0 minecraft:stone_bricks
setblock ~7 ~1 ~11 minecraft:mossy_stone_bricks
fill ~8 ~1 ~11 ~9 ~1 ~11 minecraft:stone_bricks
setblock ~9 ~1 ~0 minecraft:mossy_stone_bricks
fill ~11 ~1 ~0 ~11 ~2 ~0 minecraft:stone_bricks
fill ~11 ~1 ~11 ~12 ~1 ~11 minecraft:stone_bricks
fill ~12 ~1 ~0 ~14 ~1 ~0 minecraft:cracked_stone_bricks
fill ~16 ~1 ~0 ~16 ~2 ~0 minecraft:cracked_stone_bricks
setblock ~17 ~1 ~0 minecraft:stone_bricks
setblock ~18 ~1 ~0 minecraft:mossy_stone_bricks
setblock ~18 ~1 ~11 minecraft:stone_bricks
fill ~19 ~1 ~0 ~19 ~2 ~0 minecraft:cracked_stone_bricks
fill ~19 ~1 ~11 ~20 ~1 ~11 minecraft:cracked_stone_bricks
fill ~21 ~1 ~0 ~24 ~1 ~0 minecraft:stone_bricks
fill ~22 ~1 ~11 ~24 ~1 ~11 minecraft:stone_bricks
setblock ~25 ~1 ~11 minecraft:cracked_stone_bricks
setblock ~26 ~1 ~0 minecraft:cracked_stone_bricks
fill ~26 ~1 ~11 ~28 ~1 ~11 minecraft:stone_bricks
fill ~27 ~1 ~0 ~28 ~1 ~0 minecraft:stone_bricks
setblock ~29 ~1 ~0 minecraft:andesite
fill ~30 ~1 ~1 ~30 ~2 ~1 minecraft:cracked_stone_bricks
fill ~30 ~1 ~2 ~30 ~1 ~3 minecraft:stone_bricks
setblock ~30 ~1 ~4 minecraft:andesite
fill ~30 ~1 ~6 ~30 ~2 ~6 minecraft:mossy_stone_bricks
setblock ~30 ~1 ~7 minecraft:stone_bricks
setblock ~30 ~1 ~8 minecraft:mossy_stone_bricks
fill ~30 ~1 ~9 ~30 ~1 ~10 minecraft:stone_bricks
setblock ~0 ~2 ~1 minecraft:stone_bricks
setblock ~0 ~2 ~7 minecraft:cracked_stone_bricks
fill ~1 ~2 ~11 ~2 ~2 ~11 minecraft:cracked_stone_bricks
setblock ~2 ~2 ~0 minecraft:cracked_stone_bricks
setblock ~3 ~2 ~0 minecraft:mossy_stone_bricks
fill ~6 ~2 ~0 ~7 ~2 ~0 minecraft:cracked_stone_bricks
setblock ~6 ~2 ~11 minecraft:mossy_stone_bricks
fill ~8 ~2 ~0 ~9 ~2 ~0 minecraft:stone_bricks
setblock ~8 ~2 ~11 minecraft:cracked_stone_bricks
setblock ~9 ~2 ~11 minecraft:andesite
fill ~11 ~2 ~11 ~12 ~2 ~11 minecraft:cracked_stone_bricks
setblock ~12 ~2 ~0 minecraft:cracked_stone_bricks
setblock ~13 ~2 ~0 minecraft:andesite
setblock ~14 ~2 ~0 minecraft:cracked_stone_bricks
setblock ~17 ~2 ~0 minecraft:andesite
setblock ~18 ~2 ~0 minecraft:cracked_stone_bricks
setblock ~18 ~2 ~11 minecraft:cracked_stone_bricks
fill ~20 ~2 ~11 ~20 ~3 ~11 minecraft:stone_bricks
setblock ~21 ~2 ~0 minecraft:stone_bricks
fill ~22 ~2 ~0 ~24 ~2 ~0 minecraft:cracked_stone_bricks
setblock ~22 ~2 ~11 minecraft:mossy_stone_bricks
fill ~23 ~2 ~11 ~24 ~2 ~11 minecraft:stone_bricks
setblock ~26 ~2 ~11 minecraft:mossy_stone_bricks
fill ~27 ~2 ~0 ~27 ~3 ~0 minecraft:stone_bricks
fill ~27 ~2 ~11 ~27 ~3 ~11 minecraft:stone_bricks
fill ~28 ~2 ~0 ~29 ~2 ~0 minecraft:cracked_stone_bricks
setblock ~28 ~2 ~11 minecraft:cracked_stone_bricks
fill ~30 ~2 ~2 ~30 ~3 ~2 minecraft:stone_bricks
setblock ~30 ~2 ~4 minecraft:cracked_stone_bricks
setblock ~30 ~2 ~7 minecraft:andesite
fill ~30 ~2 ~9 ~30 ~2 ~10 minecraft:cracked_stone_bricks
setblock ~0 ~3 ~1 minecraft:mossy_stone_bricks
setblock ~0 ~3 ~2 minecraft:stone_bricks
fill ~0 ~3 ~3 ~0 ~3 ~4 minecraft:mossy_stone_bricks
setblock ~0 ~3 ~7 minecraft:mossy_stone_bricks
setblock ~0 ~3 ~8 minecraft:stone_bricks
fill ~0 ~3 ~9 ~0 ~3 ~10 minecraft:mossy_stone_bricks
setblock ~1 ~3 ~11 minecraft:cracked_stone_bricks
fill ~2 ~3 ~0 ~3 ~3 ~0 minecraft:stone_bricks
fill ~2 ~3 ~11 ~3 ~3 ~11 minecraft:stone_bricks
setblock ~4 ~3 ~0 minecraft:cracked_stone_bricks
setblock ~4 ~3 ~11 minecraft:mossy_stone_bricks
setblock ~6 ~3 ~0 minecraft:mossy_stone_bricks
setblock ~6 ~3 ~11 minecraft:stone_bricks
setblock ~7 ~3 ~0 minecraft:cracked_stone_bricks
setblock ~7 ~3 ~11 minecraft:mossy_stone_bricks
setblock ~8 ~3 ~0 minecraft:stone_bricks
fill ~8 ~3 ~11 ~9 ~3 ~11 minecraft:stone_bricks
setblock ~9 ~3 ~0 minecraft:mossy_stone_bricks
setblock ~11 ~3 ~0 minecraft:mossy_stone_bricks
fill ~11 ~3 ~11 ~12 ~3 ~11 minecraft:mossy_stone_bricks
fill ~12 ~3 ~0 ~14 ~3 ~0 minecraft:stone_bricks
setblock ~16 ~3 ~0 minecraft:mossy_stone_bricks
fill ~17 ~3 ~0 ~19 ~3 ~0 minecraft:stone_bricks
fill ~18 ~3 ~11 ~19 ~3 ~11 minecraft:stone_bricks
fill ~21 ~3 ~0 ~24 ~3 ~0 minecraft:mossy_stone_bricks
setblock ~22 ~3 ~11 minecraft:stone_bricks
setblock ~23 ~3 ~11 minecraft:mossy_stone_bricks
fill ~24 ~3 ~11 ~26 ~3 ~11 minecraft:stone_bricks
setblock ~26 ~3 ~0 minecraft:stone_bricks
setblock ~28 ~3 ~0 minecraft:mossy_stone_bricks
setblock ~28 ~3 ~11 minecraft:mossy_stone_bricks
setblock ~29 ~3 ~0 minecraft:stone_bricks
setblock ~30 ~3 ~1 minecraft:stone_bricks
fill ~30 ~3 ~3 ~30 ~3 ~4 minecraft:mossy_stone_bricks
setblock ~30 ~3 ~6 minecraft:cracked_stone_bricks
fill ~30 ~3 ~7 ~30 ~3 ~9 minecraft:stone_bricks
setblock ~30 ~3 ~10 minecraft:mossy_stone_bricks

# pillars of deepslate brick at the corners, the door jambs and between the bars
fill ~0 ~1 ~0 ~0 ~2 ~0 minecraft:deepslate_bricks
fill ~0 ~1 ~5 ~0 ~2 ~5 minecraft:deepslate_bricks
fill ~0 ~1 ~11 ~0 ~2 ~11 minecraft:deepslate_bricks
fill ~5 ~1 ~0 ~5 ~2 ~0 minecraft:deepslate_bricks
fill ~5 ~1 ~11 ~5 ~2 ~11 minecraft:deepslate_bricks
setblock ~10 ~1 ~0 minecraft:deepslate_bricks
fill ~10 ~1 ~11 ~10 ~2 ~11 minecraft:deepslate_bricks
fill ~13 ~1 ~11 ~13 ~2 ~11 minecraft:deepslate_bricks
fill ~15 ~1 ~0 ~15 ~2 ~0 minecraft:deepslate_bricks
fill ~17 ~1 ~11 ~17 ~2 ~11 minecraft:deepslate_bricks
setblock ~20 ~1 ~0 minecraft:deepslate_bricks
fill ~21 ~1 ~11 ~21 ~2 ~11 minecraft:deepslate_bricks
fill ~25 ~1 ~0 ~25 ~2 ~0 minecraft:deepslate_bricks
fill ~29 ~1 ~11 ~30 ~2 ~11 minecraft:deepslate_bricks
fill ~30 ~1 ~0 ~30 ~2 ~0 minecraft:deepslate_bricks
fill ~30 ~1 ~5 ~30 ~2 ~5 minecraft:deepslate_bricks
setblock ~0 ~3 ~0 minecraft:chiseled_stone_bricks
setblock ~0 ~3 ~5 minecraft:chiseled_stone_bricks
setblock ~0 ~3 ~11 minecraft:chiseled_stone_bricks
setblock ~5 ~3 ~0 minecraft:chiseled_stone_bricks
setblock ~5 ~3 ~11 minecraft:chiseled_stone_bricks
setblock ~10 ~3 ~0 minecraft:chiseled_stone_bricks
setblock ~10 ~3 ~11 minecraft:chiseled_stone_bricks
setblock ~13 ~3 ~11 minecraft:chiseled_stone_bricks
setblock ~15 ~3 ~0 minecraft:chiseled_stone_bricks
setblock ~17 ~3 ~11 minecraft:chiseled_stone_bricks
setblock ~20 ~3 ~0 minecraft:chiseled_stone_bricks
setblock ~21 ~3 ~11 minecraft:chiseled_stone_bricks
setblock ~25 ~3 ~0 minecraft:chiseled_stone_bricks
fill ~29 ~3 ~11 ~30 ~3 ~11 minecraft:chiseled_stone_bricks
setblock ~30 ~3 ~0 minecraft:chiseled_stone_bricks
setblock ~30 ~3 ~5 minecraft:chiseled_stone_bricks

# the door: an opening three wide and two high in the front wall at x 14..16 (jambs at x 13 and 17), a beam over it at y 3
fill ~14 ~1 ~11 ~16 ~2 ~11 minecraft:air
fill ~14 ~3 ~11 ~16 ~3 ~11 minecraft:deepslate_bricks

# windows: iron bars in the second course
setblock ~0 ~2 ~3 minecraft:iron_bars[north=true,east=false,south=true,west=false]
setblock ~0 ~2 ~8 minecraft:iron_bars[north=true,east=false,south=true,west=false]
setblock ~3 ~2 ~11 minecraft:iron_bars[north=false,east=true,south=false,west=true]
setblock ~4 ~2 ~0 minecraft:iron_bars[north=false,east=true,south=false,west=true]
setblock ~7 ~2 ~11 minecraft:iron_bars[north=false,east=true,south=true,west=true]
setblock ~10 ~2 ~0 minecraft:iron_bars[north=false,east=true,south=false,west=true]
setblock ~19 ~2 ~11 minecraft:iron_bars[north=false,east=true,south=true,west=true]
setblock ~20 ~2 ~0 minecraft:iron_bars[north=false,east=true,south=false,west=true]
setblock ~25 ~2 ~11 minecraft:iron_bars[north=false,east=true,south=false,west=true]
setblock ~26 ~2 ~0 minecraft:iron_bars[north=false,east=true,south=false,west=true]
setblock ~30 ~2 ~3 minecraft:iron_bars[north=true,east=false,south=true,west=false]
setblock ~30 ~2 ~8 minecraft:iron_bars[north=true,east=false,south=true,west=false]

# the roof of deepslate tiles: stairs and slabs of tile, a ridge of tiles, the odd cracked or cobbled patch where tiles have fallen, two bare patches
setblock ~0 ~4 ~0 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~0 ~4 ~1 ~11 ~4 ~1 minecraft:deepslate_tile_slab[type=top]
fill ~0 ~4 ~10 ~21 ~4 ~10 minecraft:deepslate_tile_slab[type=top]
fill ~0 ~4 ~11 ~21 ~4 ~11 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~1 ~4 ~0 ~2 ~4 ~0 minecraft:cobbled_deepslate_stairs[facing=south,half=bottom]
fill ~3 ~4 ~0 ~25 ~4 ~0 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~12 ~4 ~1 minecraft:cobbled_deepslate_slab[type=top]
fill ~13 ~4 ~1 ~26 ~4 ~1 minecraft:deepslate_tile_slab[type=top]
setblock ~26 ~4 ~0 minecraft:cobbled_deepslate_stairs[facing=south,half=bottom]
fill ~27 ~4 ~0 ~30 ~4 ~0 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~27 ~4 ~1 minecraft:cobbled_deepslate_slab[type=top]
fill ~28 ~4 ~1 ~30 ~4 ~1 minecraft:deepslate_tile_slab[type=top]
fill ~29 ~4 ~10 ~30 ~4 ~10 minecraft:deepslate_tile_slab[type=top]
fill ~29 ~4 ~11 ~30 ~4 ~11 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~0 ~5 ~2 ~1 ~5 ~2 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~0 ~5 ~3 minecraft:cobbled_deepslate_slab[type=top]
fill ~0 ~5 ~8 ~6 ~5 ~8 minecraft:deepslate_tile_slab[type=top]
fill ~0 ~5 ~9 ~17 ~5 ~9 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~1 ~5 ~3 ~4 ~5 ~3 minecraft:deepslate_tile_slab[type=top]
fill ~2 ~5 ~2 ~4 ~5 ~2 minecraft:cobbled_deepslate_stairs[facing=south,half=bottom]
fill ~7 ~5 ~8 ~8 ~5 ~8 minecraft:cobbled_deepslate_slab[type=top]
fill ~8 ~5 ~2 ~30 ~5 ~2 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~8 ~5 ~3 ~12 ~5 ~3 minecraft:deepslate_tile_slab[type=top]
setblock ~9 ~5 ~8 minecraft:deepslate_tile_slab[type=top]
setblock ~10 ~5 ~8 minecraft:cobbled_deepslate_slab[type=top]
fill ~11 ~5 ~8 ~20 ~5 ~8 minecraft:deepslate_tile_slab[type=top]
fill ~16 ~5 ~3 ~19 ~5 ~3 minecraft:deepslate_tile_slab[type=top]
setblock ~18 ~5 ~9 minecraft:cobbled_deepslate_stairs[facing=north,half=bottom]
fill ~19 ~5 ~9 ~21 ~5 ~9 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~20 ~5 ~3 minecraft:cobbled_deepslate_slab[type=top]
fill ~21 ~5 ~3 ~30 ~5 ~3 minecraft:deepslate_tile_slab[type=top]
setblock ~21 ~5 ~8 minecraft:cobbled_deepslate_slab[type=top]
fill ~29 ~5 ~8 ~30 ~5 ~8 minecraft:deepslate_tile_slab[type=top]
fill ~29 ~5 ~9 ~30 ~5 ~9 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~0 ~6 ~4 minecraft:cobbled_deepslate_stairs[facing=south,half=bottom]
fill ~0 ~6 ~5 ~3 ~6 ~5 minecraft:deepslate_tiles
setblock ~0 ~6 ~6 minecraft:deepslate_tiles
fill ~0 ~6 ~7 ~21 ~6 ~7 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~1 ~6 ~4 ~6 ~6 ~4 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~1 ~6 ~6 minecraft:cracked_deepslate_tiles
fill ~2 ~6 ~6 ~6 ~6 ~6 minecraft:deepslate_tiles
setblock ~4 ~6 ~5 minecraft:cracked_deepslate_tiles
fill ~5 ~6 ~5 ~10 ~6 ~5 minecraft:deepslate_tiles
setblock ~7 ~6 ~4 minecraft:cobbled_deepslate_stairs[facing=south,half=bottom]
setblock ~7 ~6 ~6 minecraft:cracked_deepslate_tiles
fill ~8 ~6 ~4 ~12 ~6 ~4 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~8 ~6 ~6 ~11 ~6 ~6 minecraft:deepslate_tiles
setblock ~11 ~6 ~5 minecraft:cobbled_deepslate
fill ~12 ~6 ~5 ~12 ~6 ~6 minecraft:deepslate_tiles
setblock ~13 ~6 ~5 minecraft:cracked_deepslate_tiles
setblock ~13 ~6 ~6 minecraft:deepslate_tiles
fill ~14 ~6 ~5 ~20 ~6 ~6 minecraft:deepslate_tiles
setblock ~16 ~6 ~4 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~17 ~6 ~4 minecraft:cobbled_deepslate_stairs[facing=south,half=bottom]
fill ~18 ~6 ~4 ~19 ~6 ~4 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~20 ~6 ~4 minecraft:cobbled_deepslate_stairs[facing=south,half=bottom]
fill ~21 ~6 ~4 ~25 ~6 ~4 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~21 ~6 ~5 minecraft:cracked_deepslate_tiles
setblock ~21 ~6 ~6 minecraft:deepslate_tiles
fill ~22 ~6 ~5 ~24 ~6 ~6 minecraft:deepslate_tiles
fill ~25 ~6 ~5 ~26 ~6 ~5 minecraft:cracked_deepslate_tiles
setblock ~25 ~6 ~6 minecraft:deepslate_tiles
setblock ~26 ~6 ~4 minecraft:cobbled_deepslate_stairs[facing=south,half=bottom]
setblock ~26 ~6 ~6 minecraft:cracked_deepslate_tiles
fill ~27 ~6 ~4 ~30 ~6 ~4 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~27 ~6 ~5 ~29 ~6 ~6 minecraft:deepslate_tiles
fill ~29 ~6 ~7 ~30 ~6 ~7 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~30 ~6 ~5 minecraft:cracked_deepslate_tiles
setblock ~30 ~6 ~6 minecraft:deepslate_tiles

# the gable ends under the roof (x 0 and x 30)
setblock ~0 ~4 ~2 minecraft:mossy_stone_bricks
fill ~0 ~4 ~3 ~0 ~4 ~6 minecraft:stone_bricks
setblock ~0 ~4 ~7 minecraft:mossy_stone_bricks
setblock ~0 ~4 ~8 minecraft:stone_bricks
setblock ~0 ~4 ~9 minecraft:mossy_stone_bricks
fill ~30 ~4 ~2 ~30 ~4 ~3 minecraft:stone_bricks
fill ~30 ~4 ~4 ~30 ~4 ~5 minecraft:mossy_stone_bricks
setblock ~30 ~4 ~6 minecraft:stone_bricks
setblock ~30 ~4 ~7 minecraft:cracked_stone_bricks
setblock ~30 ~4 ~8 minecraft:mossy_stone_bricks
setblock ~30 ~4 ~9 minecraft:stone_bricks
setblock ~0 ~5 ~4 minecraft:stone_bricks
setblock ~0 ~5 ~5 minecraft:mossy_stone_bricks
fill ~0 ~5 ~6 ~0 ~5 ~7 minecraft:stone_bricks
setblock ~30 ~5 ~4 minecraft:mossy_stone_bricks
setblock ~30 ~5 ~5 minecraft:cracked_stone_bricks
fill ~30 ~5 ~6 ~30 ~5 ~7 minecraft:mossy_stone_bricks

# the roof of deepslate tiles: stairs and slabs of tile, a ridge of tiles, the odd cracked or cobbled patch where tiles have fallen, two bare patches: bare patches where the tiles are gone (the planks of the roof show)
fill ~5 ~5 ~2 ~7 ~5 ~2 minecraft:dark_oak_planks
setblock ~5 ~5 ~3 minecraft:dark_oak_planks
setblock ~6 ~5 ~3 minecraft:air
setblock ~7 ~5 ~3 minecraft:dark_oak_planks
setblock ~13 ~5 ~3 minecraft:air
fill ~14 ~5 ~3 ~15 ~5 ~3 minecraft:dark_oak_planks
setblock ~13 ~6 ~4 minecraft:dark_oak_planks
fill ~14 ~6 ~4 ~15 ~6 ~4 minecraft:air

# the lookout: the roof cut away over x 22..28, z 7..11, the wall plate of the front wall raised to y 4 and a deck of planks at y 5; beams under it
fill ~22 ~1 ~7 ~22 ~2 ~7 minecraft:deepslate_bricks
fill ~22 ~1 ~10 ~22 ~2 ~10 minecraft:deepslate_bricks
fill ~28 ~1 ~7 ~28 ~2 ~7 minecraft:deepslate_bricks
fill ~28 ~1 ~10 ~28 ~2 ~10 minecraft:deepslate_bricks
setblock ~22 ~3 ~7 minecraft:chiseled_stone_bricks
setblock ~22 ~3 ~10 minecraft:chiseled_stone_bricks
setblock ~28 ~3 ~7 minecraft:chiseled_stone_bricks
setblock ~28 ~3 ~10 minecraft:chiseled_stone_bricks
fill ~22 ~4 ~7 ~28 ~4 ~7 minecraft:air
fill ~22 ~4 ~8 ~28 ~4 ~8 minecraft:dark_oak_log[axis=x]
fill ~22 ~4 ~9 ~28 ~4 ~9 minecraft:air
fill ~22 ~4 ~10 ~28 ~4 ~10 minecraft:dark_oak_log[axis=x]
setblock ~22 ~4 ~11 minecraft:stone_bricks
setblock ~23 ~4 ~11 minecraft:cracked_stone_bricks
setblock ~24 ~4 ~11 minecraft:stone_bricks
fill ~25 ~4 ~11 ~26 ~4 ~11 minecraft:cracked_stone_bricks
fill ~27 ~4 ~11 ~28 ~4 ~11 minecraft:stone_bricks
fill ~22 ~5 ~7 ~22 ~5 ~8 minecraft:oak_planks
setblock ~22 ~5 ~9 minecraft:spruce_planks
setblock ~22 ~5 ~10 minecraft:oak_planks
setblock ~22 ~5 ~11 minecraft:dark_oak_planks
fill ~23 ~5 ~7 ~23 ~5 ~10 minecraft:dark_oak_planks
fill ~23 ~5 ~11 ~25 ~5 ~11 minecraft:oak_planks
setblock ~24 ~5 ~7 minecraft:oak_planks
fill ~24 ~5 ~8 ~24 ~5 ~10 minecraft:spruce_planks
setblock ~25 ~5 ~7 minecraft:dark_oak_planks
setblock ~25 ~5 ~8 minecraft:oak_planks
setblock ~25 ~5 ~9 minecraft:spruce_planks
fill ~25 ~5 ~10 ~26 ~5 ~10 minecraft:dark_oak_planks
fill ~26 ~5 ~7 ~27 ~5 ~7 minecraft:spruce_planks
setblock ~26 ~5 ~8 minecraft:spruce_planks
setblock ~26 ~5 ~9 minecraft:oak_planks
setblock ~26 ~5 ~11 minecraft:spruce_planks
fill ~27 ~5 ~8 ~27 ~5 ~9 minecraft:dark_oak_planks
fill ~27 ~5 ~10 ~27 ~5 ~11 minecraft:oak_planks
setblock ~28 ~5 ~7 minecraft:dark_oak_planks
setblock ~28 ~5 ~8 minecraft:oak_planks
setblock ~28 ~5 ~9 minecraft:dark_oak_planks
setblock ~28 ~5 ~10 minecraft:spruce_planks
setblock ~28 ~5 ~11 minecraft:oak_planks
setblock ~22 ~6 ~8 minecraft:air
setblock ~22 ~6 ~11 minecraft:air
fill ~23 ~6 ~7 ~27 ~6 ~10 minecraft:air

# the parapet of bottom slabs at y 6 on the open sides (south and east, the west side from the second cell; the entry from the ladder at (22, 6, 11) stays open), posts at the corners with a lantern on the south-east one
setblock ~22 ~6 ~7 minecraft:dark_oak_log[axis=y]
fill ~22 ~6 ~9 ~22 ~6 ~10 minecraft:stone_brick_slab[type=bottom]
fill ~23 ~6 ~11 ~27 ~6 ~11 minecraft:stone_brick_slab[type=bottom]
setblock ~28 ~6 ~7 minecraft:dark_oak_log[axis=y]
fill ~28 ~6 ~8 ~28 ~6 ~10 minecraft:stone_brick_slab[type=bottom]
setblock ~28 ~6 ~11 minecraft:dark_oak_log[axis=y]

# the ladder up the front wall at x 22 from the yard to the deck (y 1..5)
fill ~22 ~1 ~12 ~22 ~5 ~12 minecraft:ladder[facing=south]

# the yard walls (y 1..2): stone brick, cracked above, with a crenel of wall blocks on top at every second cell
setblock ~0 ~1 ~12 minecraft:mossy_stone_bricks
setblock ~0 ~1 ~13 minecraft:cracked_stone_bricks
fill ~0 ~1 ~14 ~0 ~2 ~14 minecraft:stone_bricks
setblock ~0 ~1 ~16 minecraft:cracked_stone_bricks
setblock ~0 ~1 ~18 minecraft:stone_bricks
setblock ~0 ~1 ~20 minecraft:andesite
fill ~0 ~1 ~22 ~0 ~2 ~22 minecraft:mossy_stone_bricks
fill ~0 ~1 ~23 ~0 ~2 ~23 minecraft:stone_bricks
fill ~0 ~1 ~24 ~0 ~1 ~26 minecraft:mossy_stone_bricks
setblock ~0 ~1 ~27 minecraft:cracked_stone_bricks
setblock ~0 ~1 ~29 minecraft:mossy_stone_bricks
setblock ~0 ~1 ~31 minecraft:mossy_stone_bricks
setblock ~0 ~1 ~33 minecraft:mossy_stone_bricks
fill ~0 ~1 ~34 ~0 ~2 ~35 minecraft:stone_bricks
setblock ~0 ~1 ~36 minecraft:andesite
fill ~1 ~1 ~36 ~1 ~2 ~36 minecraft:stone_bricks
setblock ~2 ~1 ~36 minecraft:andesite
setblock ~3 ~1 ~36 minecraft:mossy_stone_bricks
fill ~4 ~1 ~36 ~5 ~1 ~36 minecraft:stone_bricks
setblock ~6 ~1 ~36 minecraft:mossy_stone_bricks
setblock ~7 ~1 ~36 minecraft:cracked_stone_bricks
setblock ~8 ~1 ~36 minecraft:stone_bricks
setblock ~9 ~1 ~36 minecraft:mossy_stone_bricks
setblock ~21 ~1 ~36 minecraft:andesite
fill ~22 ~1 ~36 ~23 ~1 ~36 minecraft:mossy_stone_bricks
setblock ~24 ~1 ~36 minecraft:stone_bricks
fill ~25 ~1 ~36 ~25 ~2 ~36 minecraft:cracked_stone_bricks
fill ~26 ~1 ~36 ~27 ~1 ~36 minecraft:stone_bricks
setblock ~28 ~1 ~36 minecraft:andesite
setblock ~29 ~1 ~36 minecraft:mossy_stone_bricks
setblock ~30 ~1 ~12 minecraft:cracked_stone_bricks
fill ~30 ~1 ~13 ~30 ~1 ~15 minecraft:stone_bricks
setblock ~30 ~1 ~16 minecraft:andesite
setblock ~30 ~1 ~17 minecraft:mossy_stone_bricks
setblock ~30 ~1 ~18 minecraft:stone_bricks
fill ~30 ~1 ~19 ~30 ~2 ~19 minecraft:cracked_stone_bricks
setblock ~30 ~1 ~20 minecraft:mossy_stone_bricks
fill ~30 ~1 ~28 ~30 ~2 ~28 minecraft:stone_bricks
setblock ~30 ~1 ~29 minecraft:andesite
fill ~30 ~1 ~30 ~30 ~2 ~30 minecraft:stone_bricks
setblock ~30 ~1 ~31 minecraft:mossy_stone_bricks
setblock ~30 ~1 ~32 minecraft:stone_bricks
setblock ~30 ~1 ~33 minecraft:mossy_stone_bricks
fill ~30 ~1 ~34 ~30 ~2 ~35 minecraft:stone_bricks
setblock ~30 ~1 ~36 minecraft:andesite
fill ~0 ~2 ~12 ~0 ~2 ~13 minecraft:stone_bricks
fill ~0 ~2 ~15 ~0 ~2 ~17 minecraft:stone_bricks
fill ~0 ~2 ~18 ~0 ~2 ~21 minecraft:cracked_stone_bricks
fill ~0 ~2 ~24 ~0 ~2 ~27 minecraft:stone_bricks
setblock ~0 ~2 ~28 minecraft:cracked_stone_bricks
fill ~0 ~2 ~29 ~0 ~2 ~30 minecraft:stone_bricks
setblock ~0 ~2 ~31 minecraft:cracked_stone_bricks
fill ~0 ~2 ~32 ~0 ~2 ~33 minecraft:stone_bricks
setblock ~0 ~2 ~36 minecraft:cracked_stone_bricks
setblock ~2 ~2 ~36 minecraft:cracked_stone_bricks
setblock ~3 ~2 ~36 minecraft:stone_bricks
setblock ~4 ~2 ~36 minecraft:mossy_stone_bricks
setblock ~5 ~2 ~36 minecraft:cracked_stone_bricks
fill ~6 ~2 ~36 ~7 ~2 ~36 minecraft:stone_bricks
fill ~8 ~2 ~36 ~9 ~2 ~36 minecraft:cracked_stone_bricks
fill ~21 ~2 ~36 ~22 ~2 ~36 minecraft:stone_bricks
setblock ~23 ~2 ~36 minecraft:cracked_stone_bricks
setblock ~24 ~2 ~36 minecraft:andesite
setblock ~26 ~2 ~36 minecraft:mossy_stone_bricks
setblock ~27 ~2 ~36 minecraft:cracked_stone_bricks
fill ~28 ~2 ~36 ~29 ~2 ~36 minecraft:stone_bricks
setblock ~30 ~2 ~12 minecraft:stone_bricks
setblock ~30 ~2 ~13 minecraft:cracked_stone_bricks
setblock ~30 ~2 ~14 minecraft:mossy_stone_bricks
setblock ~30 ~2 ~15 minecraft:stone_bricks
setblock ~30 ~2 ~16 minecraft:cracked_stone_bricks
setblock ~30 ~2 ~17 minecraft:stone_bricks
setblock ~30 ~2 ~18 minecraft:cracked_stone_bricks
setblock ~30 ~2 ~20 minecraft:stone_bricks
setblock ~30 ~2 ~29 minecraft:stone_bricks
setblock ~30 ~2 ~31 minecraft:stone_bricks
setblock ~30 ~2 ~32 minecraft:mossy_stone_bricks
setblock ~30 ~2 ~33 minecraft:cracked_stone_bricks
setblock ~30 ~2 ~36 minecraft:cracked_stone_bricks

# the gate posts of the front gate (x 10 and 20, y 1..2) with the braziers on them, unlit campfires at (10, 3, 36) and (20, 3, 36)
setblock ~10 ~1 ~36 minecraft:deepslate_bricks
setblock ~20 ~1 ~36 minecraft:deepslate_bricks
setblock ~10 ~2 ~36 minecraft:polished_blackstone_bricks
setblock ~20 ~2 ~36 minecraft:polished_blackstone_bricks
setblock ~10 ~3 ~36 minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false]
setblock ~20 ~3 ~36 minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false]

# the side gate in the east wall (z 22..26): posts at z 21 and 27 four high and a beam over the opening at y 5, clear below
fill ~30 ~1 ~21 ~30 ~4 ~21 minecraft:deepslate_bricks
fill ~30 ~1 ~27 ~30 ~4 ~27 minecraft:deepslate_bricks
fill ~30 ~5 ~21 ~30 ~5 ~27 minecraft:dark_oak_log[axis=z]

# crenels: stone brick wall blocks on top of the yard walls at every second cell (south wall away from the gates, and the side walls)
setblock ~0 ~3 ~12 minecraft:stone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~0 ~3 ~24 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~0 ~3 ~26 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~0 ~3 ~36 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~2 ~3 ~36 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~4 ~3 ~36 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~6 ~3 ~36 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~8 ~3 ~36 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~22 ~3 ~36 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~24 ~3 ~36 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~26 ~3 ~36 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~28 ~3 ~36 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~30 ~3 ~12 minecraft:stone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~30 ~3 ~14 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~30 ~3 ~16 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~30 ~3 ~18 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~30 ~3 ~30 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~30 ~3 ~32 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~30 ~3 ~34 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~30 ~3 ~36 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]

# the forge corner: a lean-to roof of slabs at y 3 against the west wall on posts
fill ~4 ~1 ~14 ~4 ~2 ~14 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~18 ~4 ~2 ~18 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~22 ~4 ~2 ~22 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~0 ~3 ~14 ~4 ~3 ~22 minecraft:dark_oak_slab[type=bottom]

# the forge: anvil, grindstone, smithing table, furnace, cauldron, barrels
setblock ~0 ~1 ~15 minecraft:anvil[facing=east]
setblock ~0 ~1 ~17 minecraft:grindstone[face=floor,facing=east]
setblock ~0 ~1 ~19 minecraft:smithing_table
setblock ~0 ~1 ~21 minecraft:blast_furnace[facing=east,lit=false]
setblock ~1 ~1 ~22 minecraft:cauldron

# the forge in full: a second anvil, a lava cauldron, a quenching trough, chains and a lantern under the roof, a pile of coal
setblock ~2 ~1 ~17 minecraft:lantern[hanging=true]
setblock ~2 ~1 ~19 minecraft:lava_cauldron
setblock ~2 ~1 ~21 minecraft:water_cauldron[level=3]
setblock ~3 ~1 ~16 minecraft:anvil[facing=north]
setblock ~3 ~1 ~18 minecraft:smithing_table
setblock ~3 ~1 ~20 minecraft:coal_block
setblock ~2 ~2 ~17 minecraft:chain[axis=y]

# a weapon rack of blocks: stripped logs with pike heads of lightning rods and iron bars
setblock ~6 ~1 ~12 minecraft:stripped_oak_log[axis=x]
setblock ~7 ~1 ~12 minecraft:iron_bars[north=true,east=true,south=false,west=true]
setblock ~8 ~1 ~12 minecraft:stripped_oak_log[axis=x]
setblock ~9 ~1 ~12 minecraft:iron_bars[north=true,east=true,south=false,west=true]
setblock ~10 ~1 ~12 minecraft:stripped_oak_log[axis=x]
setblock ~11 ~1 ~12 minecraft:iron_bars[north=true,east=true,south=false,west=true]
setblock ~12 ~1 ~12 minecraft:stripped_oak_log[axis=x]
setblock ~6 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~7 ~2 ~12 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~8 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~9 ~2 ~12 minecraft:air
setblock ~10 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~11 ~2 ~12 minecraft:air
setblock ~12 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]

# a second rack of pikes and shields at the east end of the front wall: a lectern-high frame of dark oak fence with iron bars
setblock ~24 ~1 ~12 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
setblock ~25 ~1 ~12 minecraft:iron_bars[north=true,east=true,south=false,west=false]
setblock ~26 ~1 ~12 minecraft:iron_bars[north=true,east=true,south=false,west=true]
setblock ~27 ~1 ~12 minecraft:iron_bars[north=true,east=false,south=false,west=true]
fill ~25 ~2 ~12 ~27 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]

# the cots: a lean-to of slabs over four beds, chests and barrels between them
fill ~4 ~1 ~27 ~4 ~2 ~27 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~30 ~4 ~2 ~30 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~34 ~4 ~2 ~34 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~0 ~3 ~27 ~4 ~3 ~34 minecraft:dark_oak_slab[type=bottom]

# the beds, a chest and barrels
setblock ~0 ~1 ~28 minecraft:gray_bed[facing=east,part=foot,occupied=false]
setblock ~0 ~1 ~30 minecraft:gray_bed[facing=east,part=foot,occupied=false]
setblock ~0 ~1 ~32 minecraft:gray_bed[facing=east,part=foot,occupied=false]
setblock ~1 ~1 ~28 minecraft:gray_bed[facing=east,part=head,occupied=false]
setblock ~1 ~1 ~30 minecraft:gray_bed[facing=east,part=head,occupied=false]
setblock ~1 ~1 ~32 minecraft:gray_bed[facing=east,part=head,occupied=false]
setblock ~2 ~1 ~29 minecraft:barrel[facing=up,open=false]
setblock ~2 ~1 ~31 minecraft:chest[facing=west,type=single,waterlogged=false]
setblock ~2 ~1 ~33 minecraft:barrel[facing=up,open=false]

# a cart: a plank bed on four fence legs with wheels of trapdoors, its load and a tongue
fill ~24 ~1 ~15 ~24 ~1 ~16 minecraft:dark_oak_trapdoor[facing=west,half=bottom,open=true]
setblock ~25 ~1 ~14 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~25 ~1 ~17 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~26 ~1 ~19 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~27 ~1 ~14 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~27 ~1 ~17 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~28 ~1 ~15 ~28 ~1 ~16 minecraft:dark_oak_trapdoor[facing=east,half=bottom,open=true]
fill ~24 ~2 ~15 ~24 ~2 ~16 minecraft:dark_oak_trapdoor[facing=west,half=top,open=true]
fill ~25 ~2 ~14 ~27 ~2 ~17 minecraft:dark_oak_planks
setblock ~26 ~2 ~18 minecraft:dark_oak_stairs[facing=north,half=top]
fill ~28 ~2 ~15 ~28 ~2 ~16 minecraft:dark_oak_trapdoor[facing=east,half=top,open=true]
setblock ~25 ~3 ~15 minecraft:barrel[facing=up,open=false]
setblock ~26 ~3 ~15 minecraft:chest[facing=north,type=single,waterlogged=false]
setblock ~26 ~3 ~16 minecraft:barrel[facing=up,open=false]
setblock ~27 ~3 ~16 minecraft:dark_oak_planks

# crates and barrels, a pile of iron blocks and a stack of coal in the corners
setblock ~2 ~1 ~24 minecraft:barrel[facing=up,open=false]
setblock ~2 ~1 ~25 minecraft:spruce_planks
setblock ~3 ~1 ~25 minecraft:barrel[facing=up,open=false]
setblock ~5 ~1 ~35 minecraft:barrel[facing=up,open=false]
setblock ~6 ~1 ~35 minecraft:chest[facing=east,type=single,waterlogged=false]
setblock ~9 ~1 ~35 minecraft:spruce_planks
setblock ~18 ~1 ~12 minecraft:chest[facing=west,type=single,waterlogged=false]
setblock ~19 ~1 ~12 minecraft:barrel[facing=up,open=false]
setblock ~20 ~1 ~12 minecraft:spruce_planks
fill ~27 ~1 ~29 ~27 ~1 ~30 minecraft:barrel[facing=up,open=false]
setblock ~28 ~1 ~12 minecraft:chest[facing=east,type=single,waterlogged=false]
setblock ~29 ~1 ~12 minecraft:barrel[facing=north,open=false]
setblock ~29 ~1 ~29 minecraft:iron_block
setblock ~29 ~1 ~32 minecraft:barrel[facing=up,open=false]
setblock ~2 ~2 ~24 minecraft:chest[facing=east,type=single,waterlogged=false]
setblock ~2 ~2 ~25 minecraft:barrel[facing=up,open=false]
setblock ~5 ~2 ~35 minecraft:barrel[facing=north,open=false]
setblock ~9 ~2 ~35 minecraft:barrel[facing=up,open=false]
setblock ~18 ~2 ~12 minecraft:barrel[facing=up,open=false]
setblock ~19 ~2 ~12 minecraft:barrel[facing=west,open=false]
setblock ~20 ~2 ~12 minecraft:chest[facing=east,type=single,waterlogged=false]
setblock ~28 ~2 ~12 minecraft:spruce_planks
setblock ~28 ~2 ~29 minecraft:iron_block
setblock ~29 ~2 ~30 minecraft:chest[facing=west,type=single,waterlogged=false]

# inside the storehouse: racks of pikes, barrels, chests and a lantern, seen through the door and the bars
setblock ~3 ~1 ~1 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~3 ~1 ~4 minecraft:barrel[facing=up,open=false]
setblock ~4 ~1 ~4 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~6 ~1 ~1 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~8 ~1 ~8 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~9 ~1 ~1 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~9 ~1 ~9 minecraft:barrel[facing=up,open=false]
setblock ~12 ~1 ~1 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~15 ~1 ~9 minecraft:barrel[facing=up,open=false]
fill ~20 ~1 ~3 ~21 ~1 ~3 minecraft:barrel[facing=up,open=false]
setblock ~26 ~1 ~2 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~27 ~1 ~2 minecraft:barrel[facing=up,open=false]
setblock ~3 ~2 ~1 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~6 ~2 ~1 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~9 ~2 ~1 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~12 ~2 ~1 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~15 ~2 ~9 minecraft:lantern[hanging=false]
