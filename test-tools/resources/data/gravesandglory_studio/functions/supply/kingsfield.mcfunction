# size 31 9 37
# supply/kingsfield: supply house of Kingsfield, tithe barn and walled yard: fieldstone and oak under a shingle roof, a plot of wheat, stooks of bound hay, a counting table and a bell frame, a forge corner, a lookout and the yard with the capture circle.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Shared shape of the five supply houses (spec 7.1, plan Task 8b): storehouse x 0..30, z 0..11; lookout deck y 5 over x 22..28, z 7..11 with the ladder at (22, 1..5, 12); yard walls y 1..2 along x 0, x 30, z 36; front gate x 11..19; side gate z 22..26;
# capture circle centre (15, 0, 24) radius 10, edge ring 9 to 10, plinth x 14..16 z 23..25; the flag column |x-15|, |z-24| <= 3, y 1..8 is structure void; the braziers (unlit campfires) at (10, 3, 36) and (20, 3, 36); nothing solid above y 6.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~1 ~1 ~1 ~29 ~3 ~1 minecraft:air
fill ~1 ~1 ~2 ~2 ~3 ~10 minecraft:air
fill ~1 ~1 ~13 ~3 ~2 ~15 minecraft:air
fill ~1 ~1 ~16 ~2 ~2 ~21 minecraft:air
fill ~1 ~1 ~23 ~1 ~8 ~23 minecraft:air
fill ~1 ~1 ~26 ~1 ~2 ~27 minecraft:air
fill ~1 ~1 ~29 ~1 ~2 ~29 minecraft:air
fill ~1 ~1 ~31 ~1 ~2 ~31 minecraft:air
fill ~1 ~1 ~33 ~1 ~2 ~35 minecraft:air
fill ~2 ~1 ~22 ~2 ~2 ~28 minecraft:air
fill ~2 ~1 ~30 ~2 ~2 ~30 minecraft:air
fill ~2 ~1 ~32 ~2 ~2 ~32 minecraft:air
fill ~2 ~1 ~34 ~2 ~2 ~34 minecraft:air
fill ~3 ~1 ~4 ~3 ~3 ~10 minecraft:air
fill ~3 ~1 ~17 ~3 ~2 ~19 minecraft:air
fill ~3 ~1 ~21 ~3 ~2 ~34 minecraft:air
fill ~4 ~1 ~3 ~4 ~3 ~10 minecraft:air
fill ~4 ~1 ~12 ~5 ~8 ~13 minecraft:air
fill ~4 ~1 ~15 ~4 ~2 ~17 minecraft:air
fill ~4 ~1 ~19 ~4 ~2 ~21 minecraft:air
fill ~4 ~1 ~23 ~4 ~8 ~26 minecraft:air
fill ~4 ~1 ~28 ~4 ~2 ~29 minecraft:air
fill ~4 ~1 ~31 ~4 ~2 ~33 minecraft:air
fill ~4 ~1 ~35 ~4 ~8 ~35 minecraft:air
fill ~5 ~1 ~2 ~5 ~4 ~8 minecraft:air
fill ~5 ~1 ~10 ~9 ~3 ~10 minecraft:air
fill ~5 ~1 ~14 ~6 ~8 ~29 minecraft:air
fill ~5 ~1 ~32 ~6 ~8 ~33 minecraft:air
fill ~5 ~1 ~34 ~5 ~8 ~35 minecraft:air
fill ~6 ~1 ~3 ~6 ~4 ~8 minecraft:air
fill ~6 ~1 ~35 ~6 ~8 ~35 minecraft:air
fill ~7 ~1 ~2 ~11 ~4 ~8 minecraft:air
fill ~7 ~1 ~13 ~7 ~8 ~34 minecraft:air
fill ~8 ~1 ~14 ~8 ~8 ~32 minecraft:air
fill ~8 ~1 ~34 ~8 ~8 ~35 minecraft:air
fill ~9 ~1 ~13 ~9 ~8 ~34 minecraft:air
fill ~10 ~1 ~9 ~11 ~3 ~10 minecraft:air
fill ~10 ~1 ~14 ~10 ~8 ~35 minecraft:air
fill ~11 ~1 ~12 ~11 ~8 ~36 minecraft:air
fill ~12 ~1 ~3 ~13 ~3 ~10 minecraft:air
fill ~12 ~1 ~13 ~12 ~8 ~20 minecraft:air
fill ~12 ~1 ~28 ~18 ~8 ~36 minecraft:air
fill ~13 ~1 ~12 ~18 ~8 ~20 minecraft:air
fill ~14 ~1 ~2 ~16 ~4 ~8 minecraft:air
fill ~14 ~1 ~9 ~14 ~3 ~10 minecraft:air
fill ~15 ~1 ~10 ~15 ~3 ~10 minecraft:air
fill ~16 ~1 ~9 ~16 ~3 ~10 minecraft:air
fill ~17 ~1 ~3 ~18 ~3 ~10 minecraft:air
fill ~19 ~1 ~2 ~29 ~4 ~2 minecraft:air
fill ~19 ~1 ~4 ~19 ~3 ~10 minecraft:air
fill ~19 ~1 ~13 ~20 ~8 ~35 minecraft:air
fill ~19 ~1 ~36 ~19 ~8 ~36 minecraft:air
fill ~20 ~1 ~3 ~23 ~4 ~6 minecraft:air
fill ~20 ~1 ~7 ~21 ~4 ~8 minecraft:air
fill ~20 ~1 ~9 ~20 ~3 ~10 minecraft:air
fill ~21 ~1 ~10 ~21 ~3 ~10 minecraft:air
fill ~21 ~1 ~12 ~21 ~8 ~33 minecraft:air
fill ~21 ~1 ~35 ~21 ~8 ~35 minecraft:air
fill ~22 ~1 ~8 ~22 ~3 ~8 minecraft:air
fill ~22 ~1 ~13 ~22 ~8 ~35 minecraft:air
fill ~23 ~1 ~7 ~23 ~3 ~10 minecraft:air
fill ~23 ~1 ~12 ~23 ~8 ~35 minecraft:air
fill ~24 ~1 ~4 ~24 ~3 ~10 minecraft:air
fill ~24 ~1 ~13 ~29 ~8 ~13 minecraft:air
fill ~24 ~1 ~14 ~24 ~8 ~14 minecraft:air
fill ~24 ~1 ~17 ~24 ~8 ~35 minecraft:air
fill ~25 ~1 ~3 ~29 ~4 ~6 minecraft:air
fill ~25 ~1 ~7 ~27 ~3 ~10 minecraft:air
fill ~25 ~1 ~15 ~25 ~1 ~16 minecraft:air
fill ~25 ~1 ~18 ~25 ~8 ~27 minecraft:air
fill ~25 ~1 ~33 ~29 ~8 ~33 minecraft:air
fill ~25 ~1 ~34 ~28 ~8 ~34 minecraft:air
fill ~25 ~1 ~35 ~27 ~8 ~35 minecraft:air
fill ~26 ~1 ~14 ~26 ~1 ~18 minecraft:air
fill ~26 ~1 ~20 ~27 ~8 ~27 minecraft:air
fill ~27 ~1 ~15 ~27 ~1 ~16 minecraft:air
fill ~27 ~1 ~18 ~27 ~8 ~18 minecraft:air
fill ~28 ~1 ~8 ~28 ~3 ~9 minecraft:air
fill ~28 ~1 ~14 ~29 ~8 ~14 minecraft:air
fill ~28 ~1 ~17 ~28 ~2 ~27 minecraft:air
fill ~29 ~1 ~7 ~29 ~3 ~10 minecraft:air
fill ~29 ~1 ~15 ~29 ~8 ~18 minecraft:air
fill ~29 ~1 ~20 ~29 ~8 ~27 minecraft:air
fill ~29 ~1 ~35 ~29 ~8 ~35 minecraft:air
fill ~30 ~1 ~22 ~30 ~4 ~26 minecraft:air
setblock ~1 ~2 ~22 minecraft:air
fill ~1 ~2 ~25 ~1 ~8 ~25 minecraft:air
setblock ~1 ~2 ~28 minecraft:air
setblock ~1 ~2 ~30 minecraft:air
setblock ~1 ~2 ~32 minecraft:air
setblock ~2 ~2 ~29 minecraft:air
setblock ~2 ~2 ~31 minecraft:air
setblock ~2 ~2 ~33 minecraft:air
fill ~3 ~2 ~2 ~4 ~4 ~2 minecraft:air
fill ~3 ~2 ~3 ~3 ~4 ~3 minecraft:air
fill ~3 ~2 ~12 ~3 ~8 ~12 minecraft:air
setblock ~3 ~2 ~16 minecraft:air
setblock ~3 ~2 ~20 minecraft:air
fill ~5 ~2 ~9 ~9 ~4 ~9 minecraft:air
fill ~6 ~2 ~2 ~6 ~4 ~2 minecraft:air
fill ~6 ~2 ~13 ~6 ~8 ~13 minecraft:air
fill ~6 ~2 ~30 ~6 ~8 ~30 minecraft:air
fill ~8 ~2 ~12 ~8 ~8 ~13 minecraft:air
fill ~9 ~2 ~35 ~9 ~8 ~35 minecraft:air
fill ~10 ~2 ~13 ~10 ~8 ~13 minecraft:air
fill ~12 ~2 ~2 ~13 ~4 ~2 minecraft:air
fill ~17 ~2 ~2 ~18 ~4 ~2 minecraft:air
fill ~19 ~2 ~3 ~19 ~4 ~3 minecraft:air
fill ~21 ~2 ~9 ~22 ~3 ~9 minecraft:air
fill ~24 ~2 ~3 ~24 ~4 ~3 minecraft:air
fill ~25 ~2 ~28 ~29 ~8 ~28 minecraft:air
fill ~25 ~2 ~29 ~25 ~8 ~32 minecraft:air
fill ~26 ~2 ~19 ~26 ~8 ~19 minecraft:air
fill ~26 ~2 ~30 ~28 ~8 ~32 minecraft:air
fill ~29 ~2 ~29 ~29 ~8 ~32 minecraft:air
fill ~0 ~3 ~12 ~2 ~8 ~12 minecraft:air
fill ~0 ~3 ~36 ~0 ~8 ~36 minecraft:air
fill ~1 ~3 ~13 ~3 ~8 ~13 minecraft:air
fill ~1 ~3 ~24 ~1 ~8 ~24 minecraft:air
fill ~1 ~3 ~26 ~1 ~8 ~26 minecraft:air
fill ~1 ~3 ~35 ~2 ~8 ~35 minecraft:air
fill ~2 ~3 ~23 ~3 ~8 ~26 minecraft:air
fill ~5 ~3 ~30 ~5 ~8 ~31 minecraft:air
fill ~6 ~3 ~12 ~7 ~8 ~12 minecraft:air
fill ~6 ~3 ~31 ~6 ~8 ~31 minecraft:air
fill ~6 ~3 ~34 ~6 ~8 ~34 minecraft:air
fill ~7 ~3 ~35 ~7 ~8 ~35 minecraft:air
fill ~8 ~3 ~36 ~9 ~8 ~36 minecraft:air
fill ~9 ~3 ~12 ~10 ~8 ~12 minecraft:air
fill ~12 ~3 ~12 ~12 ~8 ~12 minecraft:air
fill ~15 ~3 ~9 ~15 ~4 ~9 minecraft:air
fill ~19 ~3 ~12 ~20 ~8 ~12 minecraft:air
fill ~21 ~3 ~36 ~22 ~8 ~36 minecraft:air
fill ~24 ~3 ~12 ~30 ~8 ~12 minecraft:air
fill ~24 ~3 ~15 ~24 ~8 ~16 minecraft:air
fill ~25 ~3 ~14 ~27 ~8 ~14 minecraft:air
fill ~25 ~3 ~16 ~25 ~8 ~17 minecraft:air
fill ~26 ~3 ~17 ~27 ~8 ~17 minecraft:air
fill ~26 ~3 ~18 ~26 ~8 ~18 minecraft:air
fill ~26 ~3 ~29 ~26 ~8 ~29 minecraft:air
fill ~27 ~3 ~15 ~28 ~8 ~15 minecraft:air
fill ~28 ~3 ~16 ~28 ~8 ~18 minecraft:air
fill ~28 ~3 ~20 ~28 ~8 ~27 minecraft:air
fill ~28 ~3 ~29 ~28 ~8 ~29 minecraft:air
fill ~30 ~3 ~20 ~30 ~8 ~20 minecraft:air
fill ~30 ~3 ~28 ~30 ~8 ~28 minecraft:air
fill ~30 ~3 ~36 ~30 ~8 ~36 minecraft:air
fill ~0 ~4 ~13 ~0 ~8 ~35 minecraft:air
fill ~1 ~4 ~2 ~2 ~4 ~9 minecraft:air
fill ~1 ~4 ~14 ~4 ~8 ~22 minecraft:air
fill ~1 ~4 ~27 ~4 ~8 ~34 minecraft:air
fill ~1 ~4 ~36 ~2 ~8 ~36 minecraft:air
fill ~3 ~4 ~4 ~3 ~4 ~9 minecraft:air
fill ~3 ~4 ~35 ~3 ~8 ~36 minecraft:air
fill ~4 ~4 ~3 ~4 ~4 ~9 minecraft:air
fill ~4 ~4 ~36 ~7 ~8 ~36 minecraft:air
fill ~8 ~4 ~33 ~8 ~8 ~33 minecraft:air
fill ~10 ~4 ~9 ~11 ~4 ~9 minecraft:air
fill ~10 ~4 ~36 ~10 ~8 ~36 minecraft:air
fill ~12 ~4 ~3 ~13 ~4 ~9 minecraft:air
setblock ~14 ~4 ~9 minecraft:air
setblock ~16 ~4 ~9 minecraft:air
fill ~17 ~4 ~3 ~18 ~4 ~9 minecraft:air
fill ~19 ~4 ~4 ~19 ~4 ~9 minecraft:air
fill ~20 ~4 ~9 ~21 ~4 ~9 minecraft:air
fill ~20 ~4 ~36 ~20 ~8 ~36 minecraft:air
fill ~21 ~4 ~34 ~21 ~8 ~34 minecraft:air
fill ~23 ~4 ~36 ~27 ~8 ~36 minecraft:air
fill ~24 ~4 ~4 ~24 ~5 ~6 minecraft:air
fill ~25 ~4 ~15 ~26 ~8 ~15 minecraft:air
fill ~26 ~4 ~16 ~27 ~8 ~16 minecraft:air
fill ~28 ~4 ~35 ~28 ~8 ~36 minecraft:air
fill ~29 ~4 ~7 ~29 ~4 ~9 minecraft:air
fill ~29 ~4 ~34 ~29 ~8 ~34 minecraft:air
fill ~29 ~4 ~36 ~29 ~8 ~36 minecraft:air
fill ~30 ~4 ~13 ~30 ~8 ~19 minecraft:air
fill ~30 ~4 ~29 ~30 ~8 ~35 minecraft:air
fill ~0 ~5 ~0 ~30 ~8 ~1 minecraft:air
fill ~0 ~5 ~10 ~21 ~8 ~11 minecraft:air
fill ~1 ~5 ~4 ~23 ~5 ~6 minecraft:air
fill ~1 ~5 ~7 ~21 ~5 ~7 minecraft:air
fill ~25 ~5 ~4 ~29 ~5 ~6 minecraft:air
fill ~27 ~5 ~19 ~29 ~8 ~19 minecraft:air
fill ~27 ~5 ~29 ~27 ~8 ~29 minecraft:air
setblock ~29 ~5 ~7 minecraft:air
fill ~29 ~5 ~10 ~30 ~8 ~11 minecraft:air
fill ~0 ~6 ~2 ~30 ~8 ~3 minecraft:air
fill ~0 ~6 ~8 ~21 ~8 ~9 minecraft:air
fill ~22 ~6 ~12 ~22 ~8 ~12 minecraft:air
fill ~29 ~6 ~8 ~30 ~8 ~9 minecraft:air
fill ~30 ~6 ~21 ~30 ~8 ~27 minecraft:air
fill ~0 ~7 ~4 ~30 ~8 ~7 minecraft:air
fill ~22 ~7 ~8 ~28 ~8 ~11 minecraft:air

# the ground layer: planks and straw on the storehouse floor, the paved capture circle (stone brick, a ring of smooth stone, a plinth), trodden earth, gravel and cobble in the yard, a flagged mouth at the gate, a plot of tilled earth
setblock ~0 ~0 ~0 minecraft:stripped_oak_log[axis=x]
fill ~0 ~0 ~1 ~0 ~0 ~2 minecraft:spruce_planks
fill ~0 ~0 ~3 ~0 ~0 ~4 minecraft:oak_planks
setblock ~0 ~0 ~5 minecraft:stripped_oak_log[axis=x]
setblock ~0 ~0 ~6 minecraft:hay_block[axis=x]
setblock ~0 ~0 ~7 minecraft:oak_planks
fill ~0 ~0 ~8 ~0 ~0 ~9 minecraft:cobblestone
setblock ~0 ~0 ~10 minecraft:stripped_oak_log[axis=x]
setblock ~0 ~0 ~11 minecraft:spruce_planks
fill ~0 ~0 ~12 ~4 ~0 ~12 minecraft:gravel
setblock ~0 ~0 ~13 minecraft:cobblestone
fill ~0 ~0 ~14 ~1 ~0 ~14 minecraft:coarse_dirt
fill ~0 ~0 ~15 ~0 ~0 ~16 minecraft:gravel
setblock ~0 ~0 ~17 minecraft:cobblestone
setblock ~0 ~0 ~18 minecraft:andesite
setblock ~0 ~0 ~19 minecraft:cobblestone
fill ~0 ~0 ~20 ~1 ~0 ~21 minecraft:coarse_dirt
setblock ~0 ~0 ~22 minecraft:gravel
setblock ~0 ~0 ~23 minecraft:coarse_dirt
setblock ~0 ~0 ~24 minecraft:gravel
fill ~0 ~0 ~25 ~1 ~0 ~26 minecraft:coarse_dirt
fill ~0 ~0 ~27 ~1 ~0 ~27 minecraft:gravel
setblock ~0 ~0 ~28 minecraft:cobblestone
setblock ~0 ~0 ~29 minecraft:coarse_dirt
setblock ~0 ~0 ~30 minecraft:cobblestone
fill ~0 ~0 ~31 ~0 ~0 ~34 minecraft:coarse_dirt
setblock ~0 ~0 ~35 minecraft:gravel
fill ~0 ~0 ~36 ~1 ~0 ~36 minecraft:coarse_dirt
fill ~1 ~0 ~0 ~2 ~0 ~0 minecraft:oak_planks
setblock ~1 ~0 ~1 minecraft:cobblestone
fill ~1 ~0 ~2 ~1 ~0 ~3 minecraft:oak_planks
fill ~1 ~0 ~4 ~2 ~0 ~4 minecraft:stripped_oak_log[axis=x]
fill ~1 ~0 ~5 ~2 ~0 ~5 minecraft:oak_planks
setblock ~1 ~0 ~6 minecraft:oak_planks
fill ~1 ~0 ~7 ~2 ~0 ~7 minecraft:hay_block[axis=x]
fill ~1 ~0 ~8 ~1 ~0 ~10 minecraft:spruce_planks
fill ~1 ~0 ~11 ~2 ~0 ~11 minecraft:stripped_oak_log[axis=x]
fill ~1 ~0 ~13 ~2 ~0 ~13 minecraft:coarse_dirt
setblock ~1 ~0 ~15 minecraft:coarse_dirt
fill ~1 ~0 ~16 ~2 ~0 ~16 minecraft:gravel
setblock ~1 ~0 ~17 minecraft:andesite
fill ~1 ~0 ~18 ~1 ~0 ~19 minecraft:gravel
setblock ~1 ~0 ~22 minecraft:packed_mud
setblock ~1 ~0 ~23 minecraft:gravel
fill ~1 ~0 ~24 ~2 ~0 ~24 minecraft:coarse_dirt
fill ~1 ~0 ~28 ~5 ~0 ~28 minecraft:gravel
setblock ~1 ~0 ~29 minecraft:cobblestone
setblock ~1 ~0 ~30 minecraft:gravel
setblock ~1 ~0 ~31 minecraft:cobblestone
setblock ~1 ~0 ~32 minecraft:coarse_dirt
setblock ~1 ~0 ~33 minecraft:gravel
fill ~1 ~0 ~34 ~1 ~0 ~35 minecraft:cobblestone
setblock ~2 ~0 ~1 minecraft:stripped_oak_log[axis=x]
setblock ~2 ~0 ~2 minecraft:hay_block[axis=x]
setblock ~2 ~0 ~3 minecraft:oak_planks
setblock ~2 ~0 ~6 minecraft:spruce_planks
setblock ~2 ~0 ~8 minecraft:hay_block[axis=x]
setblock ~2 ~0 ~9 minecraft:spruce_planks
setblock ~2 ~0 ~10 minecraft:oak_planks
setblock ~2 ~0 ~14 minecraft:gravel
setblock ~2 ~0 ~15 minecraft:andesite
fill ~2 ~0 ~17 ~3 ~0 ~17 minecraft:gravel
setblock ~2 ~0 ~18 minecraft:coarse_dirt
fill ~2 ~0 ~19 ~2 ~0 ~21 minecraft:gravel
setblock ~2 ~0 ~22 minecraft:coarse_dirt
setblock ~2 ~0 ~23 minecraft:packed_mud
setblock ~2 ~0 ~25 minecraft:andesite
fill ~2 ~0 ~26 ~3 ~0 ~26 minecraft:gravel
fill ~2 ~0 ~27 ~5 ~0 ~27 minecraft:coarse_dirt
fill ~2 ~0 ~29 ~2 ~0 ~30 minecraft:coarse_dirt
setblock ~2 ~0 ~31 minecraft:gravel
setblock ~2 ~0 ~32 minecraft:andesite
fill ~2 ~0 ~33 ~2 ~0 ~35 minecraft:coarse_dirt
setblock ~2 ~0 ~36 minecraft:gravel
setblock ~3 ~0 ~0 minecraft:cobblestone
fill ~3 ~0 ~1 ~5 ~0 ~1 minecraft:spruce_planks
setblock ~3 ~0 ~2 minecraft:spruce_planks
setblock ~3 ~0 ~3 minecraft:stripped_oak_log[axis=x]
fill ~3 ~0 ~4 ~3 ~0 ~5 minecraft:oak_planks
setblock ~3 ~0 ~6 minecraft:cobblestone
fill ~3 ~0 ~7 ~3 ~0 ~9 minecraft:oak_planks
fill ~3 ~0 ~10 ~5 ~0 ~10 minecraft:hay_block[axis=x]
fill ~3 ~0 ~11 ~5 ~0 ~11 minecraft:oak_planks
setblock ~3 ~0 ~13 minecraft:gravel
fill ~3 ~0 ~14 ~3 ~0 ~15 minecraft:andesite
setblock ~3 ~0 ~16 minecraft:coarse_dirt
fill ~3 ~0 ~18 ~3 ~0 ~19 minecraft:andesite
fill ~3 ~0 ~20 ~3 ~0 ~23 minecraft:gravel
fill ~3 ~0 ~24 ~3 ~0 ~25 minecraft:cobblestone
fill ~3 ~0 ~29 ~4 ~0 ~29 minecraft:gravel
fill ~3 ~0 ~30 ~3 ~0 ~32 minecraft:coarse_dirt
setblock ~3 ~0 ~33 minecraft:gravel
setblock ~3 ~0 ~34 minecraft:cobblestone
fill ~3 ~0 ~35 ~3 ~0 ~36 minecraft:coarse_dirt
setblock ~4 ~0 ~0 minecraft:hay_block[axis=x]
fill ~4 ~0 ~2 ~4 ~0 ~3 minecraft:oak_planks
fill ~4 ~0 ~4 ~5 ~0 ~4 minecraft:stripped_oak_log[axis=x]
setblock ~4 ~0 ~5 minecraft:cobblestone
setblock ~4 ~0 ~6 minecraft:spruce_planks
setblock ~4 ~0 ~7 minecraft:cobblestone
fill ~4 ~0 ~8 ~4 ~0 ~9 minecraft:oak_planks
setblock ~4 ~0 ~13 minecraft:coarse_dirt
fill ~4 ~0 ~14 ~5 ~0 ~14 minecraft:gravel
fill ~4 ~0 ~15 ~4 ~0 ~18 minecraft:gravel
fill ~4 ~0 ~19 ~5 ~0 ~19 minecraft:cobblestone
setblock ~4 ~0 ~20 minecraft:cobblestone
fill ~4 ~0 ~21 ~4 ~0 ~22 minecraft:gravel
fill ~4 ~0 ~23 ~4 ~0 ~24 minecraft:cobblestone
fill ~4 ~0 ~25 ~4 ~0 ~26 minecraft:gravel
setblock ~4 ~0 ~30 minecraft:cobblestone
fill ~4 ~0 ~31 ~4 ~0 ~33 minecraft:gravel
fill ~4 ~0 ~34 ~5 ~0 ~34 minecraft:coarse_dirt
setblock ~4 ~0 ~35 minecraft:andesite
fill ~4 ~0 ~36 ~5 ~0 ~36 minecraft:coarse_dirt
setblock ~5 ~0 ~0 minecraft:oak_planks
setblock ~5 ~0 ~2 minecraft:spruce_planks
setblock ~5 ~0 ~3 minecraft:oak_planks
fill ~5 ~0 ~5 ~5 ~0 ~6 minecraft:spruce_planks
setblock ~5 ~0 ~7 minecraft:stripped_oak_log[axis=x]
fill ~5 ~0 ~8 ~6 ~0 ~8 minecraft:spruce_planks
setblock ~5 ~0 ~9 minecraft:oak_planks
fill ~5 ~0 ~12 ~8 ~0 ~12 minecraft:coarse_dirt
setblock ~5 ~0 ~13 minecraft:packed_mud
setblock ~5 ~0 ~15 minecraft:coarse_dirt
fill ~5 ~0 ~16 ~6 ~0 ~17 minecraft:gravel
setblock ~5 ~0 ~18 minecraft:coarse_dirt
fill ~5 ~0 ~20 ~5 ~0 ~22 minecraft:coarse_dirt
setblock ~5 ~0 ~23 minecraft:gravel
setblock ~5 ~0 ~24 minecraft:smooth_stone
setblock ~5 ~0 ~25 minecraft:coarse_dirt
setblock ~5 ~0 ~26 minecraft:andesite
setblock ~5 ~0 ~29 minecraft:cobblestone
fill ~5 ~0 ~30 ~5 ~0 ~32 minecraft:gravel
setblock ~5 ~0 ~33 minecraft:packed_mud
setblock ~5 ~0 ~35 minecraft:gravel
setblock ~6 ~0 ~0 minecraft:spruce_planks
setblock ~6 ~0 ~1 minecraft:stripped_oak_log[axis=x]
fill ~6 ~0 ~2 ~6 ~0 ~3 minecraft:oak_planks
setblock ~6 ~0 ~4 minecraft:spruce_planks
fill ~6 ~0 ~5 ~7 ~0 ~5 minecraft:oak_planks
setblock ~6 ~0 ~6 minecraft:spruce_planks
setblock ~6 ~0 ~7 minecraft:oak_planks
fill ~6 ~0 ~9 ~6 ~0 ~10 minecraft:spruce_planks
setblock ~6 ~0 ~11 minecraft:hay_block[axis=x]
fill ~6 ~0 ~13 ~6 ~0 ~14 minecraft:coarse_dirt
setblock ~6 ~0 ~15 minecraft:cobblestone
setblock ~6 ~0 ~18 minecraft:gravel
setblock ~6 ~0 ~19 minecraft:packed_mud
fill ~6 ~0 ~20 ~6 ~0 ~23 minecraft:smooth_stone
setblock ~6 ~0 ~24 minecraft:andesite
fill ~6 ~0 ~25 ~6 ~0 ~28 minecraft:smooth_stone
fill ~6 ~0 ~29 ~6 ~0 ~30 minecraft:coarse_dirt
setblock ~6 ~0 ~31 minecraft:gravel
setblock ~6 ~0 ~32 minecraft:cobblestone
setblock ~6 ~0 ~33 minecraft:gravel
fill ~6 ~0 ~34 ~6 ~0 ~35 minecraft:andesite
fill ~6 ~0 ~36 ~7 ~0 ~36 minecraft:gravel
setblock ~7 ~0 ~0 minecraft:cobblestone
setblock ~7 ~0 ~1 minecraft:spruce_planks
setblock ~7 ~0 ~2 minecraft:cobblestone
fill ~7 ~0 ~3 ~8 ~0 ~3 minecraft:oak_planks
setblock ~7 ~0 ~4 minecraft:stripped_oak_log[axis=x]
setblock ~7 ~0 ~6 minecraft:cobblestone
fill ~7 ~0 ~7 ~7 ~0 ~9 minecraft:spruce_planks
setblock ~7 ~0 ~10 minecraft:oak_planks
setblock ~7 ~0 ~11 minecraft:spruce_planks
fill ~7 ~0 ~13 ~9 ~0 ~13 minecraft:gravel
fill ~7 ~0 ~14 ~7 ~0 ~17 minecraft:gravel
fill ~7 ~0 ~18 ~8 ~0 ~18 minecraft:smooth_stone
setblock ~7 ~0 ~19 minecraft:smooth_stone
setblock ~7 ~0 ~20 minecraft:stone_bricks
fill ~7 ~0 ~21 ~8 ~0 ~21 minecraft:mossy_stone_bricks
setblock ~7 ~0 ~22 minecraft:polished_andesite
fill ~7 ~0 ~23 ~9 ~0 ~23 minecraft:stone_bricks
fill ~7 ~0 ~24 ~8 ~0 ~24 minecraft:polished_andesite
fill ~7 ~0 ~25 ~8 ~0 ~25 minecraft:stone_bricks
fill ~7 ~0 ~26 ~8 ~0 ~26 minecraft:cracked_stone_bricks
fill ~7 ~0 ~27 ~7 ~0 ~28 minecraft:stone_bricks
fill ~7 ~0 ~29 ~7 ~0 ~30 minecraft:smooth_stone
fill ~7 ~0 ~31 ~7 ~0 ~32 minecraft:coarse_dirt
setblock ~7 ~0 ~33 minecraft:cobblestone
fill ~7 ~0 ~34 ~7 ~0 ~35 minecraft:coarse_dirt
fill ~8 ~0 ~0 ~9 ~0 ~0 minecraft:spruce_planks
setblock ~8 ~0 ~1 minecraft:cobblestone
fill ~8 ~0 ~2 ~9 ~0 ~2 minecraft:oak_planks
fill ~8 ~0 ~4 ~10 ~0 ~5 minecraft:oak_planks
setblock ~8 ~0 ~6 minecraft:hay_block[axis=x]
setblock ~8 ~0 ~7 minecraft:oak_planks
setblock ~8 ~0 ~8 minecraft:spruce_planks
setblock ~8 ~0 ~9 minecraft:stripped_oak_log[axis=x]
setblock ~8 ~0 ~10 minecraft:spruce_planks
setblock ~8 ~0 ~11 minecraft:oak_planks
setblock ~8 ~0 ~14 minecraft:coarse_dirt
fill ~8 ~0 ~15 ~9 ~0 ~15 minecraft:gravel
setblock ~8 ~0 ~16 minecraft:gravel
fill ~8 ~0 ~17 ~9 ~0 ~17 minecraft:smooth_stone
fill ~8 ~0 ~19 ~8 ~0 ~20 minecraft:stone_bricks
setblock ~8 ~0 ~22 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~27 minecraft:polished_andesite
fill ~8 ~0 ~28 ~9 ~0 ~28 minecraft:andesite
setblock ~8 ~0 ~29 minecraft:stone_bricks
fill ~8 ~0 ~30 ~8 ~0 ~31 minecraft:smooth_stone
setblock ~8 ~0 ~32 minecraft:gravel
setblock ~8 ~0 ~33 minecraft:coarse_dirt
fill ~8 ~0 ~34 ~8 ~0 ~35 minecraft:gravel
setblock ~8 ~0 ~36 minecraft:cobblestone
setblock ~9 ~0 ~1 minecraft:spruce_planks
setblock ~9 ~0 ~3 minecraft:hay_block[axis=x]
setblock ~9 ~0 ~6 minecraft:spruce_planks
setblock ~9 ~0 ~7 minecraft:hay_block[axis=x]
setblock ~9 ~0 ~8 minecraft:stripped_oak_log[axis=x]
setblock ~9 ~0 ~9 minecraft:spruce_planks
setblock ~9 ~0 ~10 minecraft:cobblestone
setblock ~9 ~0 ~11 minecraft:spruce_planks
setblock ~9 ~0 ~12 minecraft:andesite
setblock ~9 ~0 ~14 minecraft:andesite
fill ~9 ~0 ~16 ~10 ~0 ~16 minecraft:smooth_stone
fill ~9 ~0 ~18 ~9 ~0 ~19 minecraft:andesite
setblock ~9 ~0 ~20 minecraft:stone_bricks
fill ~9 ~0 ~21 ~9 ~0 ~22 minecraft:cracked_stone_bricks
setblock ~9 ~0 ~24 minecraft:andesite
fill ~9 ~0 ~25 ~9 ~0 ~26 minecraft:cracked_stone_bricks
fill ~9 ~0 ~27 ~10 ~0 ~27 minecraft:stone_bricks
setblock ~9 ~0 ~29 minecraft:mossy_stone_bricks
fill ~9 ~0 ~30 ~10 ~0 ~30 minecraft:cracked_stone_bricks
fill ~9 ~0 ~31 ~9 ~0 ~32 minecraft:smooth_stone
fill ~9 ~0 ~33 ~10 ~0 ~33 minecraft:gravel
fill ~9 ~0 ~34 ~9 ~0 ~35 minecraft:coarse_dirt
setblock ~9 ~0 ~36 minecraft:andesite
fill ~10 ~0 ~0 ~10 ~0 ~2 minecraft:oak_planks
setblock ~10 ~0 ~3 minecraft:stripped_oak_log[axis=x]
setblock ~10 ~0 ~6 minecraft:hay_block[axis=x]
setblock ~10 ~0 ~7 minecraft:oak_planks
setblock ~10 ~0 ~8 minecraft:cobblestone
setblock ~10 ~0 ~9 minecraft:oak_planks
fill ~10 ~0 ~10 ~11 ~0 ~10 minecraft:hay_block[axis=x]
fill ~10 ~0 ~11 ~11 ~0 ~11 minecraft:oak_planks
setblock ~10 ~0 ~12 minecraft:gravel
fill ~10 ~0 ~13 ~10 ~0 ~14 minecraft:packed_mud
setblock ~10 ~0 ~15 minecraft:coarse_dirt
setblock ~10 ~0 ~17 minecraft:cracked_stone_bricks
fill ~10 ~0 ~18 ~10 ~0 ~19 minecraft:stone_bricks
setblock ~10 ~0 ~20 minecraft:andesite
setblock ~10 ~0 ~21 minecraft:stone_bricks
setblock ~10 ~0 ~22 minecraft:andesite
setblock ~10 ~0 ~23 minecraft:mossy_stone_bricks
setblock ~10 ~0 ~24 minecraft:stone_bricks
setblock ~10 ~0 ~25 minecraft:mossy_stone_bricks
setblock ~10 ~0 ~26 minecraft:andesite
setblock ~10 ~0 ~28 minecraft:cracked_stone_bricks
setblock ~10 ~0 ~29 minecraft:andesite
setblock ~10 ~0 ~31 minecraft:mossy_stone_bricks
setblock ~10 ~0 ~32 minecraft:smooth_stone
setblock ~10 ~0 ~34 minecraft:cobblestone
setblock ~10 ~0 ~35 minecraft:gravel
setblock ~10 ~0 ~36 minecraft:coarse_dirt
fill ~11 ~0 ~0 ~12 ~0 ~0 minecraft:spruce_planks
fill ~11 ~0 ~1 ~11 ~0 ~2 minecraft:hay_block[axis=x]
fill ~11 ~0 ~3 ~11 ~0 ~5 minecraft:spruce_planks
setblock ~11 ~0 ~6 minecraft:oak_planks
setblock ~11 ~0 ~7 minecraft:spruce_planks
setblock ~11 ~0 ~8 minecraft:oak_planks
setblock ~11 ~0 ~9 minecraft:spruce_planks
fill ~11 ~0 ~12 ~13 ~0 ~12 minecraft:coarse_dirt
fill ~11 ~0 ~13 ~12 ~0 ~13 minecraft:gravel
fill ~11 ~0 ~14 ~12 ~0 ~14 minecraft:cobblestone
fill ~11 ~0 ~15 ~14 ~0 ~15 minecraft:smooth_stone
setblock ~11 ~0 ~16 minecraft:mossy_stone_bricks
fill ~11 ~0 ~17 ~11 ~0 ~18 minecraft:stone_bricks
fill ~11 ~0 ~19 ~11 ~0 ~21 minecraft:mossy_stone_bricks
fill ~11 ~0 ~22 ~11 ~0 ~24 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~25 minecraft:stone_bricks
fill ~11 ~0 ~26 ~12 ~0 ~26 minecraft:mossy_stone_bricks
fill ~11 ~0 ~27 ~11 ~0 ~28 minecraft:andesite
setblock ~11 ~0 ~29 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~30 minecraft:mossy_stone_bricks
fill ~11 ~0 ~31 ~11 ~0 ~32 minecraft:stone_bricks
fill ~11 ~0 ~33 ~14 ~0 ~33 minecraft:smooth_stone
setblock ~11 ~0 ~34 minecraft:stone
setblock ~11 ~0 ~35 minecraft:cobblestone
setblock ~11 ~0 ~36 minecraft:stone
setblock ~12 ~0 ~1 minecraft:spruce_planks
fill ~12 ~0 ~2 ~13 ~0 ~2 minecraft:oak_planks
setblock ~12 ~0 ~3 minecraft:oak_planks
setblock ~12 ~0 ~4 minecraft:spruce_planks
fill ~12 ~0 ~5 ~12 ~0 ~7 minecraft:oak_planks
setblock ~12 ~0 ~8 minecraft:spruce_planks
fill ~12 ~0 ~9 ~12 ~0 ~10 minecraft:oak_planks
setblock ~12 ~0 ~11 minecraft:spruce_planks
setblock ~12 ~0 ~16 minecraft:polished_andesite
setblock ~12 ~0 ~17 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~18 minecraft:mossy_stone_bricks
fill ~12 ~0 ~19 ~13 ~0 ~19 minecraft:stone_bricks
setblock ~12 ~0 ~20 minecraft:stone_bricks
fill ~12 ~0 ~21 ~12 ~0 ~22 minecraft:polished_andesite
setblock ~12 ~0 ~23 minecraft:stone_bricks
setblock ~12 ~0 ~24 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~25 minecraft:polished_andesite
fill ~12 ~0 ~27 ~13 ~0 ~27 minecraft:stone_bricks
setblock ~12 ~0 ~28 minecraft:mossy_stone_bricks
fill ~12 ~0 ~29 ~12 ~0 ~30 minecraft:stone_bricks
fill ~12 ~0 ~31 ~12 ~0 ~32 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~34 minecraft:mossy_cobblestone
setblock ~12 ~0 ~35 minecraft:andesite
setblock ~12 ~0 ~36 minecraft:cobblestone
fill ~13 ~0 ~0 ~13 ~0 ~1 minecraft:stripped_oak_log[axis=x]
fill ~13 ~0 ~3 ~16 ~0 ~3 minecraft:spruce_planks
fill ~13 ~0 ~4 ~13 ~0 ~6 minecraft:oak_planks
fill ~13 ~0 ~7 ~14 ~0 ~7 minecraft:cobblestone
setblock ~13 ~0 ~8 minecraft:oak_planks
fill ~13 ~0 ~9 ~13 ~0 ~10 minecraft:hay_block[axis=x]
setblock ~13 ~0 ~11 minecraft:oak_planks
setblock ~13 ~0 ~13 minecraft:coarse_dirt
setblock ~13 ~0 ~14 minecraft:andesite
fill ~13 ~0 ~16 ~14 ~0 ~16 minecraft:cracked_stone_bricks
fill ~13 ~0 ~17 ~13 ~0 ~18 minecraft:andesite
setblock ~13 ~0 ~20 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~21 minecraft:mossy_stone_bricks
fill ~13 ~0 ~22 ~13 ~0 ~23 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~24 minecraft:stone_bricks
fill ~13 ~0 ~25 ~13 ~0 ~26 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~28 minecraft:stone_bricks
fill ~13 ~0 ~29 ~13 ~0 ~30 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~31 minecraft:stone_bricks
setblock ~13 ~0 ~32 minecraft:polished_andesite
setblock ~13 ~0 ~34 minecraft:stone
setblock ~13 ~0 ~35 minecraft:mossy_cobblestone
setblock ~13 ~0 ~36 minecraft:andesite
setblock ~14 ~0 ~0 minecraft:spruce_planks
setblock ~14 ~0 ~1 minecraft:oak_planks
setblock ~14 ~0 ~2 minecraft:cobblestone
fill ~14 ~0 ~4 ~14 ~0 ~6 minecraft:spruce_planks
setblock ~14 ~0 ~8 minecraft:stripped_oak_log[axis=x]
fill ~14 ~0 ~9 ~14 ~0 ~11 minecraft:oak_planks
fill ~14 ~0 ~12 ~15 ~0 ~13 minecraft:gravel
setblock ~14 ~0 ~14 minecraft:gravel
setblock ~14 ~0 ~17 minecraft:mossy_stone_bricks
setblock ~14 ~0 ~18 minecraft:stone_bricks
setblock ~14 ~0 ~19 minecraft:cracked_stone_bricks
setblock ~14 ~0 ~20 minecraft:polished_andesite
setblock ~14 ~0 ~21 minecraft:stone_bricks
setblock ~14 ~0 ~22 minecraft:mossy_stone_bricks
fill ~14 ~0 ~23 ~16 ~0 ~25 minecraft:chiseled_stone_bricks
setblock ~14 ~0 ~26 minecraft:cracked_stone_bricks
setblock ~14 ~0 ~27 minecraft:polished_andesite
setblock ~14 ~0 ~28 minecraft:andesite
setblock ~14 ~0 ~29 minecraft:mossy_stone_bricks
setblock ~14 ~0 ~30 minecraft:polished_andesite
fill ~14 ~0 ~31 ~14 ~0 ~32 minecraft:mossy_stone_bricks
setblock ~14 ~0 ~34 minecraft:mossy_cobblestone
setblock ~14 ~0 ~35 minecraft:stone
setblock ~14 ~0 ~36 minecraft:cobblestone
setblock ~15 ~0 ~0 minecraft:oak_planks
setblock ~15 ~0 ~1 minecraft:stripped_oak_log[axis=x]
fill ~15 ~0 ~2 ~16 ~0 ~2 minecraft:oak_planks
setblock ~15 ~0 ~4 minecraft:cobblestone
setblock ~15 ~0 ~5 minecraft:hay_block[axis=x]
setblock ~15 ~0 ~6 minecraft:oak_planks
setblock ~15 ~0 ~7 minecraft:spruce_planks
fill ~15 ~0 ~8 ~16 ~0 ~9 minecraft:oak_planks
setblock ~15 ~0 ~10 minecraft:oak_planks
setblock ~15 ~0 ~11 minecraft:hay_block[axis=x]
setblock ~15 ~0 ~14 minecraft:smooth_stone
setblock ~15 ~0 ~15 minecraft:mossy_stone_bricks
fill ~15 ~0 ~16 ~15 ~0 ~17 minecraft:stone_bricks
fill ~15 ~0 ~18 ~16 ~0 ~18 minecraft:andesite
setblock ~15 ~0 ~19 minecraft:polished_andesite
setblock ~15 ~0 ~20 minecraft:andesite
setblock ~15 ~0 ~21 minecraft:polished_andesite
setblock ~15 ~0 ~22 minecraft:stone_bricks
setblock ~15 ~0 ~26 minecraft:polished_andesite
setblock ~15 ~0 ~27 minecraft:mossy_stone_bricks
setblock ~15 ~0 ~28 minecraft:stone_bricks
setblock ~15 ~0 ~29 minecraft:andesite
setblock ~15 ~0 ~30 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~31 minecraft:stone_bricks
setblock ~15 ~0 ~32 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~33 minecraft:stone_bricks
setblock ~15 ~0 ~34 minecraft:smooth_stone
fill ~15 ~0 ~35 ~15 ~0 ~36 minecraft:cobblestone
fill ~16 ~0 ~0 ~16 ~0 ~1 minecraft:spruce_planks
fill ~16 ~0 ~4 ~17 ~0 ~4 minecraft:spruce_planks
fill ~16 ~0 ~5 ~16 ~0 ~6 minecraft:oak_planks
setblock ~16 ~0 ~7 minecraft:stripped_oak_log[axis=x]
setblock ~16 ~0 ~10 minecraft:hay_block[axis=x]
setblock ~16 ~0 ~11 minecraft:stripped_oak_log[axis=x]
fill ~16 ~0 ~12 ~16 ~0 ~14 minecraft:coarse_dirt
fill ~16 ~0 ~15 ~19 ~0 ~15 minecraft:smooth_stone
setblock ~16 ~0 ~16 minecraft:cracked_stone_bricks
setblock ~16 ~0 ~17 minecraft:stone_bricks
fill ~16 ~0 ~19 ~16 ~0 ~21 minecraft:stone_bricks
setblock ~16 ~0 ~22 minecraft:mossy_stone_bricks
fill ~16 ~0 ~26 ~17 ~0 ~26 minecraft:stone_bricks
setblock ~16 ~0 ~27 minecraft:stone_bricks
setblock ~16 ~0 ~28 minecraft:andesite
setblock ~16 ~0 ~29 minecraft:mossy_stone_bricks
fill ~16 ~0 ~30 ~16 ~0 ~31 minecraft:stone_bricks
setblock ~16 ~0 ~32 minecraft:polished_andesite
fill ~16 ~0 ~33 ~19 ~0 ~33 minecraft:smooth_stone
setblock ~16 ~0 ~34 minecraft:cobblestone
fill ~16 ~0 ~35 ~16 ~0 ~36 minecraft:andesite
setblock ~17 ~0 ~0 minecraft:stripped_oak_log[axis=x]
fill ~17 ~0 ~1 ~19 ~0 ~1 minecraft:oak_planks
setblock ~17 ~0 ~2 minecraft:spruce_planks
setblock ~17 ~0 ~3 minecraft:oak_planks
setblock ~17 ~0 ~5 minecraft:cobblestone
fill ~17 ~0 ~6 ~17 ~0 ~7 minecraft:oak_planks
setblock ~17 ~0 ~8 minecraft:spruce_planks
fill ~17 ~0 ~9 ~18 ~0 ~10 minecraft:oak_planks
setblock ~17 ~0 ~11 minecraft:oak_planks
fill ~17 ~0 ~12 ~17 ~0 ~14 minecraft:gravel
fill ~17 ~0 ~16 ~18 ~0 ~16 minecraft:stone_bricks
fill ~17 ~0 ~17 ~18 ~0 ~17 minecraft:mossy_stone_bricks
fill ~17 ~0 ~18 ~18 ~0 ~18 minecraft:stone_bricks
setblock ~17 ~0 ~19 minecraft:polished_andesite
fill ~17 ~0 ~20 ~17 ~0 ~22 minecraft:stone_bricks
setblock ~17 ~0 ~23 minecraft:andesite
fill ~17 ~0 ~24 ~19 ~0 ~24 minecraft:stone_bricks
setblock ~17 ~0 ~25 minecraft:mossy_stone_bricks
fill ~17 ~0 ~27 ~17 ~0 ~29 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~30 minecraft:polished_andesite
setblock ~17 ~0 ~31 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~32 minecraft:mossy_stone_bricks
fill ~17 ~0 ~34 ~17 ~0 ~35 minecraft:andesite
setblock ~17 ~0 ~36 minecraft:cobblestone
fill ~18 ~0 ~0 ~19 ~0 ~0 minecraft:cobblestone
fill ~18 ~0 ~2 ~18 ~0 ~3 minecraft:oak_planks
fill ~18 ~0 ~4 ~19 ~0 ~4 minecraft:hay_block[axis=x]
setblock ~18 ~0 ~5 minecraft:oak_planks
setblock ~18 ~0 ~6 minecraft:stripped_oak_log[axis=x]
fill ~18 ~0 ~7 ~18 ~0 ~8 minecraft:spruce_planks
setblock ~18 ~0 ~11 minecraft:spruce_planks
setblock ~18 ~0 ~12 minecraft:coarse_dirt
setblock ~18 ~0 ~13 minecraft:andesite
setblock ~18 ~0 ~14 minecraft:cobblestone
setblock ~18 ~0 ~19 minecraft:stone_bricks
setblock ~18 ~0 ~20 minecraft:cracked_stone_bricks
fill ~18 ~0 ~21 ~20 ~0 ~21 minecraft:stone_bricks
setblock ~18 ~0 ~22 minecraft:stone_bricks
setblock ~18 ~0 ~23 minecraft:polished_andesite
fill ~18 ~0 ~25 ~18 ~0 ~28 minecraft:stone_bricks
setblock ~18 ~0 ~29 minecraft:mossy_stone_bricks
setblock ~18 ~0 ~30 minecraft:stone_bricks
setblock ~18 ~0 ~31 minecraft:mossy_stone_bricks
setblock ~18 ~0 ~32 minecraft:stone_bricks
fill ~18 ~0 ~34 ~19 ~0 ~34 minecraft:cobblestone
setblock ~18 ~0 ~35 minecraft:cobblestone
setblock ~18 ~0 ~36 minecraft:mossy_cobblestone
setblock ~19 ~0 ~2 minecraft:stripped_oak_log[axis=x]
setblock ~19 ~0 ~3 minecraft:spruce_planks
setblock ~19 ~0 ~5 minecraft:spruce_planks
setblock ~19 ~0 ~6 minecraft:hay_block[axis=x]
setblock ~19 ~0 ~7 minecraft:oak_planks
fill ~19 ~0 ~8 ~19 ~0 ~9 minecraft:spruce_planks
setblock ~19 ~0 ~10 minecraft:hay_block[axis=x]
setblock ~19 ~0 ~11 minecraft:oak_planks
fill ~19 ~0 ~12 ~20 ~0 ~12 minecraft:gravel
fill ~19 ~0 ~13 ~19 ~0 ~14 minecraft:gravel
setblock ~19 ~0 ~16 minecraft:polished_andesite
setblock ~19 ~0 ~17 minecraft:stone_bricks
fill ~19 ~0 ~18 ~19 ~0 ~20 minecraft:cracked_stone_bricks
fill ~19 ~0 ~22 ~19 ~0 ~23 minecraft:polished_andesite
setblock ~19 ~0 ~25 minecraft:andesite
fill ~19 ~0 ~26 ~19 ~0 ~30 minecraft:stone_bricks
setblock ~19 ~0 ~31 minecraft:polished_andesite
setblock ~19 ~0 ~32 minecraft:andesite
setblock ~19 ~0 ~35 minecraft:andesite
setblock ~19 ~0 ~36 minecraft:stone
setblock ~20 ~0 ~0 minecraft:hay_block[axis=x]
fill ~20 ~0 ~1 ~22 ~0 ~1 minecraft:spruce_planks
fill ~20 ~0 ~2 ~21 ~0 ~2 minecraft:oak_planks
setblock ~20 ~0 ~3 minecraft:cobblestone
fill ~20 ~0 ~4 ~20 ~0 ~8 minecraft:oak_planks
setblock ~20 ~0 ~9 minecraft:spruce_planks
fill ~20 ~0 ~10 ~21 ~0 ~10 minecraft:cobblestone
fill ~20 ~0 ~11 ~21 ~0 ~11 minecraft:spruce_planks
setblock ~20 ~0 ~13 minecraft:andesite
fill ~20 ~0 ~14 ~20 ~0 ~15 minecraft:gravel
fill ~20 ~0 ~16 ~21 ~0 ~16 minecraft:smooth_stone
setblock ~20 ~0 ~17 minecraft:cracked_stone_bricks
fill ~20 ~0 ~18 ~21 ~0 ~18 minecraft:stone_bricks
setblock ~20 ~0 ~19 minecraft:stone_bricks
setblock ~20 ~0 ~20 minecraft:andesite
setblock ~20 ~0 ~22 minecraft:andesite
setblock ~20 ~0 ~23 minecraft:stone_bricks
setblock ~20 ~0 ~24 minecraft:polished_andesite
setblock ~20 ~0 ~25 minecraft:stone_bricks
setblock ~20 ~0 ~26 minecraft:mossy_stone_bricks
fill ~20 ~0 ~27 ~22 ~0 ~27 minecraft:stone_bricks
setblock ~20 ~0 ~28 minecraft:andesite
fill ~20 ~0 ~29 ~20 ~0 ~31 minecraft:stone_bricks
setblock ~20 ~0 ~32 minecraft:smooth_stone
fill ~20 ~0 ~33 ~21 ~0 ~33 minecraft:coarse_dirt
fill ~20 ~0 ~34 ~20 ~0 ~36 minecraft:coarse_dirt
setblock ~21 ~0 ~0 minecraft:stripped_oak_log[axis=x]
fill ~21 ~0 ~3 ~21 ~0 ~6 minecraft:oak_planks
setblock ~21 ~0 ~7 minecraft:spruce_planks
fill ~21 ~0 ~8 ~21 ~0 ~9 minecraft:oak_planks
fill ~21 ~0 ~12 ~21 ~0 ~14 minecraft:coarse_dirt
fill ~21 ~0 ~15 ~22 ~0 ~15 minecraft:gravel
fill ~21 ~0 ~17 ~22 ~0 ~17 minecraft:smooth_stone
fill ~21 ~0 ~19 ~22 ~0 ~20 minecraft:cracked_stone_bricks
fill ~21 ~0 ~21 ~22 ~0 ~21 minecraft:mossy_stone_bricks
setblock ~21 ~0 ~22 minecraft:polished_andesite
fill ~21 ~0 ~23 ~21 ~0 ~24 minecraft:cracked_stone_bricks
fill ~21 ~0 ~25 ~21 ~0 ~26 minecraft:polished_andesite
setblock ~21 ~0 ~28 minecraft:cracked_stone_bricks
setblock ~21 ~0 ~29 minecraft:mossy_stone_bricks
setblock ~21 ~0 ~30 minecraft:stone_bricks
fill ~21 ~0 ~31 ~21 ~0 ~32 minecraft:smooth_stone
fill ~21 ~0 ~34 ~21 ~0 ~36 minecraft:gravel
setblock ~22 ~0 ~0 minecraft:oak_planks
fill ~22 ~0 ~2 ~22 ~0 ~4 minecraft:spruce_planks
setblock ~22 ~0 ~5 minecraft:oak_planks
setblock ~22 ~0 ~6 minecraft:spruce_planks
setblock ~22 ~0 ~7 minecraft:stripped_oak_log[axis=x]
setblock ~22 ~0 ~8 minecraft:cobblestone
fill ~22 ~0 ~9 ~22 ~0 ~10 minecraft:spruce_planks
fill ~22 ~0 ~11 ~24 ~0 ~11 minecraft:oak_planks
setblock ~22 ~0 ~12 minecraft:gravel
setblock ~22 ~0 ~13 minecraft:andesite
fill ~22 ~0 ~14 ~23 ~0 ~14 minecraft:gravel
fill ~22 ~0 ~16 ~24 ~0 ~16 minecraft:gravel
fill ~22 ~0 ~18 ~23 ~0 ~18 minecraft:smooth_stone
setblock ~22 ~0 ~22 minecraft:andesite
setblock ~22 ~0 ~23 minecraft:stone_bricks
setblock ~22 ~0 ~24 minecraft:polished_andesite
setblock ~22 ~0 ~25 minecraft:stone_bricks
setblock ~22 ~0 ~26 minecraft:polished_andesite
fill ~22 ~0 ~28 ~23 ~0 ~28 minecraft:stone_bricks
setblock ~22 ~0 ~29 minecraft:stone_bricks
fill ~22 ~0 ~30 ~22 ~0 ~31 minecraft:smooth_stone
setblock ~22 ~0 ~32 minecraft:cobblestone
fill ~22 ~0 ~33 ~22 ~0 ~35 minecraft:gravel
fill ~22 ~0 ~36 ~23 ~0 ~36 minecraft:coarse_dirt
setblock ~23 ~0 ~0 minecraft:stripped_oak_log[axis=x]
fill ~23 ~0 ~1 ~23 ~0 ~2 minecraft:oak_planks
setblock ~23 ~0 ~3 minecraft:spruce_planks
setblock ~23 ~0 ~4 minecraft:oak_planks
fill ~23 ~0 ~5 ~24 ~0 ~5 minecraft:spruce_planks
setblock ~23 ~0 ~6 minecraft:hay_block[axis=x]
fill ~23 ~0 ~7 ~23 ~0 ~9 minecraft:oak_planks
setblock ~23 ~0 ~10 minecraft:stripped_oak_log[axis=x]
fill ~23 ~0 ~12 ~23 ~0 ~13 minecraft:coarse_dirt
setblock ~23 ~0 ~15 minecraft:packed_mud
fill ~23 ~0 ~17 ~24 ~0 ~17 minecraft:cobblestone
setblock ~23 ~0 ~19 minecraft:smooth_stone
setblock ~23 ~0 ~20 minecraft:stone_bricks
setblock ~23 ~0 ~21 minecraft:andesite
setblock ~23 ~0 ~22 minecraft:stone_bricks
setblock ~23 ~0 ~23 minecraft:cracked_stone_bricks
setblock ~23 ~0 ~24 minecraft:stone_bricks
setblock ~23 ~0 ~25 minecraft:mossy_stone_bricks
setblock ~23 ~0 ~26 minecraft:stone_bricks
setblock ~23 ~0 ~27 minecraft:cracked_stone_bricks
fill ~23 ~0 ~29 ~23 ~0 ~30 minecraft:smooth_stone
fill ~23 ~0 ~31 ~25 ~0 ~31 minecraft:gravel
fill ~23 ~0 ~32 ~24 ~0 ~33 minecraft:gravel
fill ~23 ~0 ~34 ~23 ~0 ~35 minecraft:gravel
setblock ~24 ~0 ~0 minecraft:spruce_planks
setblock ~24 ~0 ~1 minecraft:stripped_oak_log[axis=x]
setblock ~24 ~0 ~2 minecraft:cobblestone
setblock ~24 ~0 ~3 minecraft:oak_planks
setblock ~24 ~0 ~4 minecraft:cobblestone
setblock ~24 ~0 ~6 minecraft:stripped_oak_log[axis=x]
setblock ~24 ~0 ~7 minecraft:hay_block[axis=x]
setblock ~24 ~0 ~8 minecraft:spruce_planks
setblock ~24 ~0 ~9 minecraft:oak_planks
setblock ~24 ~0 ~10 minecraft:hay_block[axis=x]
fill ~24 ~0 ~12 ~24 ~0 ~13 minecraft:cobblestone
fill ~24 ~0 ~14 ~24 ~0 ~15 minecraft:coarse_dirt
setblock ~24 ~0 ~18 minecraft:coarse_dirt
setblock ~24 ~0 ~19 minecraft:gravel
fill ~24 ~0 ~20 ~24 ~0 ~23 minecraft:smooth_stone
setblock ~24 ~0 ~24 minecraft:mossy_stone_bricks
fill ~24 ~0 ~25 ~24 ~0 ~28 minecraft:smooth_stone
setblock ~24 ~0 ~29 minecraft:gravel
fill ~24 ~0 ~30 ~25 ~0 ~30 minecraft:cobblestone
setblock ~24 ~0 ~34 minecraft:cobblestone
fill ~24 ~0 ~35 ~24 ~0 ~36 minecraft:coarse_dirt
fill ~25 ~0 ~0 ~26 ~0 ~0 minecraft:oak_planks
setblock ~25 ~0 ~1 minecraft:oak_planks
setblock ~25 ~0 ~2 minecraft:hay_block[axis=x]
fill ~25 ~0 ~3 ~25 ~0 ~4 minecraft:spruce_planks
fill ~25 ~0 ~5 ~26 ~0 ~5 minecraft:oak_planks
fill ~25 ~0 ~6 ~25 ~0 ~8 minecraft:oak_planks
fill ~25 ~0 ~9 ~26 ~0 ~9 minecraft:stripped_oak_log[axis=x]
setblock ~25 ~0 ~10 minecraft:spruce_planks
setblock ~25 ~0 ~11 minecraft:hay_block[axis=x]
fill ~25 ~0 ~12 ~26 ~0 ~13 minecraft:coarse_dirt
setblock ~25 ~0 ~14 minecraft:gravel
fill ~25 ~0 ~15 ~25 ~0 ~23 minecraft:coarse_dirt
setblock ~25 ~0 ~24 minecraft:smooth_stone
setblock ~25 ~0 ~25 minecraft:coarse_dirt
setblock ~25 ~0 ~26 minecraft:cobblestone
fill ~25 ~0 ~27 ~26 ~0 ~27 minecraft:coarse_dirt
setblock ~25 ~0 ~28 minecraft:gravel
setblock ~25 ~0 ~29 minecraft:coarse_dirt
setblock ~25 ~0 ~32 minecraft:andesite
setblock ~25 ~0 ~33 minecraft:cobblestone
setblock ~25 ~0 ~34 minecraft:gravel
setblock ~25 ~0 ~35 minecraft:andesite
setblock ~25 ~0 ~36 minecraft:coarse_dirt
setblock ~26 ~0 ~1 minecraft:spruce_planks
setblock ~26 ~0 ~2 minecraft:stripped_oak_log[axis=x]
setblock ~26 ~0 ~3 minecraft:oak_planks
setblock ~26 ~0 ~4 minecraft:spruce_planks
fill ~26 ~0 ~6 ~27 ~0 ~6 minecraft:cobblestone
setblock ~26 ~0 ~7 minecraft:stripped_oak_log[axis=x]
setblock ~26 ~0 ~8 minecraft:spruce_planks
setblock ~26 ~0 ~10 minecraft:stripped_oak_log[axis=x]
fill ~26 ~0 ~11 ~27 ~0 ~11 minecraft:oak_planks
fill ~26 ~0 ~14 ~26 ~0 ~15 minecraft:coarse_dirt
setblock ~26 ~0 ~16 minecraft:gravel
fill ~26 ~0 ~17 ~26 ~0 ~20 minecraft:coarse_dirt
fill ~26 ~0 ~21 ~26 ~0 ~22 minecraft:gravel
setblock ~26 ~0 ~23 minecraft:coarse_dirt
setblock ~26 ~0 ~24 minecraft:gravel
setblock ~26 ~0 ~25 minecraft:andesite
setblock ~26 ~0 ~26 minecraft:gravel
fill ~26 ~0 ~32 ~26 ~0 ~33 minecraft:gravel
setblock ~26 ~0 ~34 minecraft:andesite
fill ~26 ~0 ~35 ~26 ~0 ~36 minecraft:coarse_dirt
setblock ~27 ~0 ~0 minecraft:cobblestone
setblock ~27 ~0 ~1 minecraft:stripped_oak_log[axis=x]
fill ~27 ~0 ~2 ~27 ~0 ~3 minecraft:cobblestone
setblock ~27 ~0 ~4 minecraft:oak_planks
setblock ~27 ~0 ~5 minecraft:spruce_planks
setblock ~27 ~0 ~7 minecraft:spruce_planks
fill ~27 ~0 ~8 ~28 ~0 ~8 minecraft:oak_planks
setblock ~27 ~0 ~9 minecraft:oak_planks
setblock ~27 ~0 ~10 minecraft:cobblestone
setblock ~27 ~0 ~12 minecraft:gravel
setblock ~27 ~0 ~13 minecraft:coarse_dirt
fill ~27 ~0 ~14 ~30 ~0 ~14 minecraft:gravel
fill ~27 ~0 ~15 ~28 ~0 ~15 minecraft:coarse_dirt
setblock ~27 ~0 ~16 minecraft:cobblestone
setblock ~27 ~0 ~17 minecraft:gravel
setblock ~27 ~0 ~18 minecraft:cobblestone
setblock ~27 ~0 ~19 minecraft:gravel
fill ~27 ~0 ~20 ~27 ~0 ~21 minecraft:coarse_dirt
setblock ~27 ~0 ~22 minecraft:gravel
setblock ~27 ~0 ~23 minecraft:packed_mud
fill ~27 ~0 ~24 ~27 ~0 ~27 minecraft:coarse_dirt
setblock ~27 ~0 ~32 minecraft:cobblestone
setblock ~27 ~0 ~33 minecraft:gravel
setblock ~27 ~0 ~34 minecraft:coarse_dirt
setblock ~27 ~0 ~35 minecraft:gravel
setblock ~27 ~0 ~36 minecraft:coarse_dirt
setblock ~28 ~0 ~0 minecraft:stripped_oak_log[axis=x]
fill ~28 ~0 ~1 ~28 ~0 ~2 minecraft:cobblestone
fill ~28 ~0 ~3 ~28 ~0 ~5 minecraft:oak_planks
fill ~28 ~0 ~6 ~28 ~0 ~7 minecraft:spruce_planks
setblock ~28 ~0 ~9 minecraft:spruce_planks
setblock ~28 ~0 ~10 minecraft:hay_block[axis=x]
setblock ~28 ~0 ~11 minecraft:spruce_planks
setblock ~28 ~0 ~12 minecraft:andesite
setblock ~28 ~0 ~13 minecraft:cobblestone
fill ~28 ~0 ~16 ~28 ~0 ~17 minecraft:gravel
fill ~28 ~0 ~18 ~28 ~0 ~20 minecraft:coarse_dirt
setblock ~28 ~0 ~21 minecraft:cobblestone
setblock ~28 ~0 ~22 minecraft:packed_mud
fill ~28 ~0 ~23 ~30 ~0 ~23 minecraft:coarse_dirt
setblock ~28 ~0 ~24 minecraft:gravel
setblock ~28 ~0 ~25 minecraft:cobblestone
fill ~28 ~0 ~26 ~28 ~0 ~27 minecraft:coarse_dirt
fill ~28 ~0 ~32 ~28 ~0 ~33 minecraft:coarse_dirt
setblock ~28 ~0 ~34 minecraft:gravel
fill ~28 ~0 ~35 ~28 ~0 ~36 minecraft:coarse_dirt
fill ~29 ~0 ~0 ~30 ~0 ~0 minecraft:oak_planks
fill ~29 ~0 ~1 ~30 ~0 ~1 minecraft:spruce_planks
fill ~29 ~0 ~2 ~30 ~0 ~3 minecraft:oak_planks
setblock ~29 ~0 ~4 minecraft:cobblestone
setblock ~29 ~0 ~5 minecraft:spruce_planks
setblock ~29 ~0 ~6 minecraft:stripped_oak_log[axis=x]
setblock ~29 ~0 ~7 minecraft:spruce_planks
setblock ~29 ~0 ~8 minecraft:cobblestone
setblock ~29 ~0 ~9 minecraft:hay_block[axis=x]
setblock ~29 ~0 ~10 minecraft:stripped_oak_log[axis=x]
setblock ~29 ~0 ~11 minecraft:oak_planks
setblock ~29 ~0 ~12 minecraft:coarse_dirt
setblock ~29 ~0 ~13 minecraft:packed_mud
setblock ~29 ~0 ~15 minecraft:gravel
setblock ~29 ~0 ~16 minecraft:coarse_dirt
setblock ~29 ~0 ~17 minecraft:gravel
setblock ~29 ~0 ~18 minecraft:cobblestone
setblock ~29 ~0 ~19 minecraft:coarse_dirt
fill ~29 ~0 ~20 ~30 ~0 ~20 minecraft:gravel
setblock ~29 ~0 ~21 minecraft:gravel
setblock ~29 ~0 ~22 minecraft:cobblestone
fill ~29 ~0 ~24 ~29 ~0 ~25 minecraft:coarse_dirt
setblock ~29 ~0 ~26 minecraft:cobblestone
setblock ~29 ~0 ~27 minecraft:packed_mud
setblock ~29 ~0 ~32 minecraft:andesite
setblock ~29 ~0 ~33 minecraft:gravel
setblock ~29 ~0 ~34 minecraft:cobblestone
setblock ~29 ~0 ~35 minecraft:andesite
setblock ~29 ~0 ~36 minecraft:gravel
setblock ~30 ~0 ~4 minecraft:oak_planks
setblock ~30 ~0 ~5 minecraft:cobblestone
setblock ~30 ~0 ~6 minecraft:oak_planks
setblock ~30 ~0 ~7 minecraft:cobblestone
setblock ~30 ~0 ~8 minecraft:spruce_planks
setblock ~30 ~0 ~9 minecraft:cobblestone
fill ~30 ~0 ~10 ~30 ~0 ~11 minecraft:oak_planks
setblock ~30 ~0 ~12 minecraft:gravel
setblock ~30 ~0 ~13 minecraft:cobblestone
setblock ~30 ~0 ~15 minecraft:coarse_dirt
setblock ~30 ~0 ~16 minecraft:gravel
setblock ~30 ~0 ~17 minecraft:andesite
fill ~30 ~0 ~18 ~30 ~0 ~19 minecraft:coarse_dirt
setblock ~30 ~0 ~21 minecraft:andesite
setblock ~30 ~0 ~22 minecraft:packed_mud
setblock ~30 ~0 ~24 minecraft:gravel
setblock ~30 ~0 ~25 minecraft:cobblestone
setblock ~30 ~0 ~26 minecraft:gravel
setblock ~30 ~0 ~27 minecraft:coarse_dirt
setblock ~30 ~0 ~28 minecraft:cobblestone
fill ~30 ~0 ~29 ~30 ~0 ~30 minecraft:coarse_dirt
setblock ~30 ~0 ~31 minecraft:cobblestone
fill ~30 ~0 ~32 ~30 ~0 ~33 minecraft:gravel
setblock ~30 ~0 ~34 minecraft:coarse_dirt
setblock ~30 ~0 ~35 minecraft:gravel
setblock ~30 ~0 ~36 minecraft:coarse_dirt

# the storehouse walls (y 1..3): fieldstone to the plate, oak logs at the posts, a plate of oak logs (y 3)
fill ~0 ~1 ~1 ~0 ~2 ~2 minecraft:stone
setblock ~0 ~1 ~3 minecraft:coarse_dirt
setblock ~0 ~1 ~4 minecraft:cobblestone
setblock ~0 ~1 ~6 minecraft:andesite
fill ~0 ~1 ~7 ~0 ~1 ~10 minecraft:cobblestone
fill ~1 ~1 ~0 ~1 ~2 ~0 minecraft:cobblestone
fill ~1 ~1 ~11 ~2 ~2 ~11 minecraft:cobblestone
fill ~2 ~1 ~0 ~2 ~2 ~0 minecraft:stone
setblock ~3 ~1 ~0 minecraft:mossy_cobblestone
fill ~3 ~1 ~11 ~4 ~1 ~11 minecraft:stone
setblock ~4 ~1 ~0 minecraft:cobblestone
setblock ~6 ~1 ~0 minecraft:mossy_cobblestone
setblock ~6 ~1 ~11 minecraft:stone
fill ~7 ~1 ~0 ~7 ~2 ~0 minecraft:cobblestone
setblock ~7 ~1 ~11 minecraft:cobblestone
setblock ~8 ~1 ~0 minecraft:stone
setblock ~8 ~1 ~11 minecraft:coarse_dirt
setblock ~9 ~1 ~0 minecraft:andesite
fill ~9 ~1 ~11 ~9 ~2 ~11 minecraft:cobblestone
setblock ~11 ~1 ~0 minecraft:stone
fill ~11 ~1 ~11 ~12 ~1 ~11 minecraft:cobblestone
setblock ~12 ~1 ~0 minecraft:andesite
fill ~13 ~1 ~0 ~14 ~1 ~0 minecraft:stone
setblock ~16 ~1 ~0 minecraft:cobblestone
setblock ~17 ~1 ~0 minecraft:gravel
setblock ~18 ~1 ~0 minecraft:cobblestone
setblock ~18 ~1 ~11 minecraft:stone
fill ~19 ~1 ~0 ~19 ~2 ~0 minecraft:stone
fill ~19 ~1 ~11 ~20 ~1 ~11 minecraft:cobblestone
setblock ~21 ~1 ~0 minecraft:stone
setblock ~22 ~1 ~0 minecraft:gravel
setblock ~22 ~1 ~11 minecraft:coarse_dirt
fill ~23 ~1 ~0 ~23 ~2 ~0 minecraft:cobblestone
setblock ~23 ~1 ~11 minecraft:cobblestone
setblock ~24 ~1 ~0 minecraft:coarse_dirt
fill ~24 ~1 ~11 ~24 ~2 ~11 minecraft:mossy_cobblestone
setblock ~25 ~1 ~11 minecraft:stone
setblock ~26 ~1 ~0 minecraft:cobblestone
fill ~26 ~1 ~11 ~27 ~1 ~11 minecraft:andesite
setblock ~27 ~1 ~0 minecraft:stone
setblock ~28 ~1 ~0 minecraft:andesite
fill ~28 ~1 ~11 ~28 ~2 ~11 minecraft:cobblestone
fill ~29 ~1 ~0 ~29 ~2 ~0 minecraft:cobblestone
fill ~30 ~1 ~1 ~30 ~1 ~3 minecraft:cobblestone
fill ~30 ~1 ~4 ~30 ~2 ~4 minecraft:andesite
setblock ~30 ~1 ~6 minecraft:cobblestone
setblock ~30 ~1 ~7 minecraft:gravel
setblock ~30 ~1 ~8 minecraft:cobblestone
setblock ~30 ~1 ~9 minecraft:stone
fill ~30 ~1 ~10 ~30 ~2 ~10 minecraft:cobblestone
setblock ~0 ~2 ~4 minecraft:mossy_cobblestone
setblock ~0 ~2 ~6 minecraft:cobblestone
setblock ~0 ~2 ~7 minecraft:stone
setblock ~0 ~2 ~9 minecraft:mossy_cobblestone
setblock ~0 ~2 ~10 minecraft:stone
setblock ~3 ~2 ~0 minecraft:cobblestone
setblock ~4 ~2 ~11 minecraft:andesite
setblock ~6 ~2 ~0 minecraft:cobblestone
setblock ~6 ~2 ~11 minecraft:andesite
setblock ~8 ~2 ~0 minecraft:stone_bricks
setblock ~8 ~2 ~11 minecraft:andesite
setblock ~9 ~2 ~0 minecraft:cobblestone
setblock ~11 ~2 ~0 minecraft:cobblestone
setblock ~11 ~2 ~11 minecraft:cobblestone
fill ~12 ~2 ~0 ~13 ~2 ~0 minecraft:stone
setblock ~12 ~2 ~11 minecraft:andesite
setblock ~14 ~2 ~0 minecraft:cobblestone
setblock ~16 ~2 ~0 minecraft:andesite
setblock ~17 ~2 ~0 minecraft:cobblestone
setblock ~18 ~2 ~0 minecraft:stone
setblock ~18 ~2 ~11 minecraft:andesite
setblock ~20 ~2 ~11 minecraft:cobblestone
setblock ~21 ~2 ~0 minecraft:cobblestone
setblock ~22 ~2 ~0 minecraft:stone_bricks
setblock ~22 ~2 ~11 minecraft:cobblestone
setblock ~23 ~2 ~11 minecraft:stone
setblock ~24 ~2 ~0 minecraft:andesite
setblock ~26 ~2 ~11 minecraft:cobblestone
setblock ~27 ~2 ~0 minecraft:cobblestone
setblock ~27 ~2 ~11 minecraft:stone
setblock ~28 ~2 ~0 minecraft:stone
setblock ~30 ~2 ~1 minecraft:stone_bricks
setblock ~30 ~2 ~2 minecraft:stone
setblock ~30 ~2 ~6 minecraft:stone
setblock ~30 ~2 ~7 minecraft:stone_bricks
setblock ~30 ~2 ~9 minecraft:cobblestone
fill ~0 ~3 ~1 ~0 ~3 ~4 minecraft:oak_log[axis=z]
fill ~0 ~3 ~6 ~0 ~3 ~10 minecraft:oak_log[axis=z]
fill ~1 ~3 ~0 ~4 ~3 ~0 minecraft:oak_log[axis=x]
fill ~1 ~3 ~11 ~4 ~3 ~11 minecraft:oak_log[axis=x]
fill ~6 ~3 ~0 ~9 ~3 ~0 minecraft:oak_log[axis=x]
fill ~6 ~3 ~11 ~9 ~3 ~11 minecraft:oak_log[axis=x]
fill ~11 ~3 ~0 ~14 ~3 ~0 minecraft:oak_log[axis=x]
fill ~11 ~3 ~11 ~12 ~3 ~11 minecraft:oak_log[axis=x]
fill ~16 ~3 ~0 ~19 ~3 ~0 minecraft:oak_log[axis=x]
fill ~18 ~3 ~11 ~20 ~3 ~11 minecraft:oak_log[axis=x]
fill ~21 ~3 ~0 ~24 ~3 ~0 minecraft:oak_log[axis=x]
fill ~22 ~3 ~11 ~28 ~3 ~11 minecraft:oak_log[axis=x]
fill ~26 ~3 ~0 ~29 ~3 ~0 minecraft:oak_log[axis=x]
fill ~30 ~3 ~1 ~30 ~3 ~4 minecraft:oak_log[axis=z]
fill ~30 ~3 ~6 ~30 ~3 ~10 minecraft:oak_log[axis=z]

# posts of oak logs at the corners, the door jambs and between the windows
fill ~0 ~1 ~0 ~0 ~3 ~0 minecraft:oak_log[axis=y]
fill ~0 ~1 ~5 ~0 ~3 ~5 minecraft:oak_log[axis=y]
fill ~0 ~1 ~11 ~0 ~3 ~11 minecraft:oak_log[axis=y]
fill ~5 ~1 ~0 ~5 ~3 ~0 minecraft:oak_log[axis=y]
fill ~5 ~1 ~11 ~5 ~3 ~11 minecraft:oak_log[axis=y]
setblock ~10 ~1 ~0 minecraft:oak_log[axis=y]
fill ~10 ~1 ~11 ~10 ~3 ~11 minecraft:oak_log[axis=y]
fill ~13 ~1 ~11 ~13 ~3 ~11 minecraft:oak_log[axis=y]
fill ~15 ~1 ~0 ~15 ~3 ~0 minecraft:oak_log[axis=y]
fill ~17 ~1 ~11 ~17 ~3 ~11 minecraft:oak_log[axis=y]
setblock ~20 ~1 ~0 minecraft:oak_log[axis=y]
fill ~21 ~1 ~11 ~21 ~3 ~11 minecraft:oak_log[axis=y]
fill ~25 ~1 ~0 ~25 ~3 ~0 minecraft:oak_log[axis=y]
fill ~29 ~1 ~11 ~30 ~3 ~11 minecraft:oak_log[axis=y]
fill ~30 ~1 ~0 ~30 ~3 ~0 minecraft:oak_log[axis=y]
fill ~30 ~1 ~5 ~30 ~3 ~5 minecraft:oak_log[axis=y]
setblock ~10 ~3 ~0 minecraft:oak_log[axis=y]
setblock ~20 ~3 ~0 minecraft:oak_log[axis=y]

# the door: an opening three wide and two high in the front wall at x 14..16 (jambs at x 13 and 17), a beam over it at y 3
fill ~14 ~1 ~11 ~16 ~2 ~11 minecraft:air
fill ~14 ~3 ~11 ~16 ~3 ~11 minecraft:oak_log[axis=x]

# windows: panes of glass in the second course
setblock ~0 ~2 ~3 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~0 ~2 ~8 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~3 ~2 ~11 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~4 ~2 ~0 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~7 ~2 ~11 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~10 ~2 ~0 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~19 ~2 ~11 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~20 ~2 ~0 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~25 ~2 ~11 minecraft:glass_pane[north=false,east=true,south=true,west=true]
setblock ~26 ~2 ~0 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~30 ~2 ~3 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~30 ~2 ~8 minecraft:glass_pane[north=true,east=false,south=true,west=false]

# the roof: oak shingles (stairs and slabs) gone dark in streaks, a ridge of oak, patches of spruce, jungle and dark oak where it has been mended
setblock ~0 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~4 ~1 ~14 ~4 ~1 minecraft:oak_slab[type=top]
setblock ~0 ~4 ~10 minecraft:dark_oak_slab[type=top]
setblock ~0 ~4 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~1 ~4 ~0 ~8 ~4 ~0 minecraft:oak_stairs[facing=south,half=bottom]
fill ~1 ~4 ~10 ~16 ~4 ~10 minecraft:oak_slab[type=top]
fill ~1 ~4 ~11 ~2 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~3 ~4 ~11 ~10 ~4 ~11 minecraft:oak_stairs[facing=north,half=bottom]
setblock ~9 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~10 ~4 ~0 ~12 ~4 ~0 minecraft:oak_stairs[facing=south,half=bottom]
setblock ~11 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~12 ~4 ~11 ~16 ~4 ~11 minecraft:oak_stairs[facing=north,half=bottom]
setblock ~13 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~14 ~4 ~0 ~15 ~4 ~0 minecraft:jungle_stairs[facing=south,half=bottom]
fill ~15 ~4 ~1 ~16 ~4 ~1 minecraft:jungle_slab[type=top]
fill ~16 ~4 ~0 ~18 ~4 ~0 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~17 ~4 ~1 minecraft:spruce_slab[type=top]
setblock ~17 ~4 ~10 minecraft:dark_oak_slab[type=top]
fill ~17 ~4 ~11 ~18 ~4 ~11 minecraft:jungle_stairs[facing=north,half=bottom]
setblock ~18 ~4 ~1 minecraft:jungle_slab[type=top]
setblock ~18 ~4 ~10 minecraft:spruce_slab[type=top]
fill ~19 ~4 ~0 ~28 ~4 ~0 minecraft:oak_stairs[facing=south,half=bottom]
fill ~19 ~4 ~1 ~27 ~4 ~1 minecraft:oak_slab[type=top]
fill ~19 ~4 ~10 ~21 ~4 ~10 minecraft:oak_slab[type=top]
setblock ~19 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~20 ~4 ~11 ~21 ~4 ~11 minecraft:oak_stairs[facing=north,half=bottom]
fill ~28 ~4 ~1 ~29 ~4 ~1 minecraft:dark_oak_slab[type=top]
setblock ~29 ~4 ~0 minecraft:jungle_stairs[facing=south,half=bottom]
setblock ~29 ~4 ~10 minecraft:dark_oak_slab[type=top]
fill ~29 ~4 ~11 ~30 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~30 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~30 ~4 ~1 minecraft:spruce_slab[type=top]
setblock ~30 ~4 ~10 minecraft:jungle_slab[type=top]
fill ~0 ~5 ~2 ~15 ~5 ~2 minecraft:oak_stairs[facing=south,half=bottom]
fill ~0 ~5 ~3 ~8 ~5 ~3 minecraft:oak_slab[type=top]
fill ~0 ~5 ~8 ~21 ~5 ~8 minecraft:oak_slab[type=top]
setblock ~0 ~5 ~9 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~1 ~5 ~9 ~14 ~5 ~9 minecraft:oak_stairs[facing=north,half=bottom]
setblock ~9 ~5 ~3 minecraft:jungle_slab[type=top]
fill ~10 ~5 ~3 ~16 ~5 ~3 minecraft:oak_slab[type=top]
fill ~15 ~5 ~9 ~16 ~5 ~9 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~16 ~5 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~17 ~5 ~2 ~19 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~17 ~5 ~3 minecraft:jungle_slab[type=top]
setblock ~17 ~5 ~9 minecraft:jungle_stairs[facing=north,half=bottom]
setblock ~18 ~5 ~3 minecraft:dark_oak_slab[type=top]
setblock ~18 ~5 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~19 ~5 ~3 minecraft:jungle_slab[type=top]
fill ~19 ~5 ~9 ~21 ~5 ~9 minecraft:oak_stairs[facing=north,half=bottom]
fill ~20 ~5 ~2 ~22 ~5 ~2 minecraft:oak_stairs[facing=south,half=bottom]
fill ~20 ~5 ~3 ~22 ~5 ~3 minecraft:oak_slab[type=top]
setblock ~23 ~5 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~23 ~5 ~3 minecraft:jungle_slab[type=top]
setblock ~24 ~5 ~2 minecraft:jungle_stairs[facing=south,half=bottom]
setblock ~24 ~5 ~3 minecraft:spruce_slab[type=top]
fill ~25 ~5 ~2 ~28 ~5 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~25 ~5 ~3 minecraft:dark_oak_slab[type=top]
fill ~26 ~5 ~3 ~28 ~5 ~3 minecraft:oak_slab[type=top]
setblock ~29 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~29 ~5 ~3 ~30 ~5 ~3 minecraft:dark_oak_slab[type=top]
fill ~29 ~5 ~8 ~30 ~5 ~8 minecraft:oak_slab[type=top]
setblock ~29 ~5 ~9 minecraft:oak_stairs[facing=north,half=bottom]
setblock ~30 ~5 ~2 minecraft:jungle_stairs[facing=south,half=bottom]
setblock ~30 ~5 ~9 minecraft:jungle_stairs[facing=north,half=bottom]
setblock ~0 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~6 ~5 ~1 ~6 ~6 minecraft:oak_planks
fill ~0 ~6 ~7 ~21 ~6 ~7 minecraft:oak_stairs[facing=north,half=bottom]
fill ~1 ~6 ~4 ~3 ~6 ~4 minecraft:oak_stairs[facing=south,half=bottom]
setblock ~2 ~6 ~5 minecraft:spruce_planks
setblock ~2 ~6 ~6 minecraft:oak_planks
fill ~3 ~6 ~5 ~3 ~6 ~6 minecraft:oak_planks
setblock ~4 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~4 ~6 ~5 minecraft:spruce_planks
setblock ~4 ~6 ~6 minecraft:oak_planks
fill ~5 ~6 ~4 ~22 ~6 ~4 minecraft:oak_stairs[facing=south,half=bottom]
fill ~5 ~6 ~5 ~8 ~6 ~6 minecraft:oak_planks
fill ~9 ~6 ~5 ~9 ~6 ~6 minecraft:spruce_planks
fill ~10 ~6 ~5 ~23 ~6 ~5 minecraft:oak_planks
fill ~10 ~6 ~6 ~17 ~6 ~6 minecraft:oak_planks
setblock ~18 ~6 ~6 minecraft:spruce_planks
fill ~19 ~6 ~6 ~21 ~6 ~6 minecraft:oak_planks
setblock ~22 ~6 ~6 minecraft:spruce_planks
setblock ~23 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~23 ~6 ~6 ~24 ~6 ~6 minecraft:oak_planks
setblock ~24 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~24 ~6 ~5 minecraft:spruce_planks
setblock ~25 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~25 ~6 ~5 ~25 ~6 ~6 minecraft:oak_planks
fill ~26 ~6 ~4 ~29 ~6 ~4 minecraft:oak_stairs[facing=south,half=bottom]
setblock ~26 ~6 ~5 minecraft:spruce_planks
fill ~26 ~6 ~6 ~28 ~6 ~6 minecraft:oak_planks
fill ~27 ~6 ~5 ~30 ~6 ~5 minecraft:oak_planks
setblock ~29 ~6 ~6 minecraft:spruce_planks
fill ~29 ~6 ~7 ~30 ~6 ~7 minecraft:oak_stairs[facing=north,half=bottom]
setblock ~30 ~6 ~4 minecraft:jungle_stairs[facing=south,half=bottom]
setblock ~30 ~6 ~6 minecraft:oak_planks

# the gable ends under the roof (x 0 and x 30)
fill ~0 ~4 ~2 ~0 ~4 ~3 minecraft:stripped_oak_log[axis=y]
fill ~0 ~4 ~4 ~0 ~4 ~5 minecraft:oak_planks
fill ~0 ~4 ~6 ~0 ~5 ~6 minecraft:spruce_planks
fill ~0 ~4 ~7 ~0 ~4 ~8 minecraft:stripped_oak_log[axis=y]
setblock ~0 ~4 ~9 minecraft:oak_planks
fill ~30 ~4 ~2 ~30 ~4 ~4 minecraft:oak_planks
setblock ~30 ~4 ~5 minecraft:spruce_planks
fill ~30 ~4 ~6 ~30 ~4 ~8 minecraft:oak_planks
setblock ~30 ~4 ~9 minecraft:stripped_oak_log[axis=y]
fill ~0 ~5 ~4 ~0 ~5 ~5 minecraft:stripped_oak_log[axis=y]
setblock ~0 ~5 ~7 minecraft:oak_planks
setblock ~30 ~5 ~4 minecraft:spruce_planks
setblock ~30 ~5 ~5 minecraft:oak_planks
setblock ~30 ~5 ~6 minecraft:spruce_planks
setblock ~30 ~5 ~7 minecraft:stripped_oak_log[axis=y]

# the lookout: the roof cut away over x 22..28, z 7..11, the wall plate of the front wall raised to y 4 and a deck of planks at y 5; beams under it
fill ~22 ~1 ~7 ~22 ~3 ~7 minecraft:oak_log[axis=y]
fill ~22 ~1 ~10 ~22 ~3 ~10 minecraft:oak_log[axis=y]
fill ~28 ~1 ~7 ~28 ~3 ~7 minecraft:oak_log[axis=y]
fill ~28 ~1 ~10 ~28 ~3 ~10 minecraft:oak_log[axis=y]
fill ~22 ~4 ~7 ~28 ~4 ~7 minecraft:air
fill ~22 ~4 ~8 ~28 ~4 ~8 minecraft:oak_log[axis=x]
fill ~22 ~4 ~9 ~28 ~4 ~9 minecraft:air
fill ~22 ~4 ~10 ~28 ~4 ~10 minecraft:oak_log[axis=x]
fill ~22 ~4 ~11 ~24 ~4 ~11 minecraft:oak_planks
setblock ~25 ~4 ~11 minecraft:spruce_planks
fill ~26 ~4 ~11 ~28 ~4 ~11 minecraft:oak_planks
fill ~22 ~5 ~7 ~24 ~5 ~7 minecraft:spruce_planks
fill ~22 ~5 ~8 ~23 ~5 ~10 minecraft:oak_planks
setblock ~22 ~5 ~11 minecraft:oak_planks
setblock ~23 ~5 ~11 minecraft:spruce_planks
fill ~24 ~5 ~8 ~25 ~5 ~8 minecraft:dark_oak_planks
fill ~24 ~5 ~9 ~26 ~5 ~9 minecraft:oak_planks
fill ~24 ~5 ~10 ~25 ~5 ~10 minecraft:spruce_planks
fill ~24 ~5 ~11 ~25 ~5 ~11 minecraft:oak_planks
setblock ~25 ~5 ~7 minecraft:oak_planks
fill ~26 ~5 ~7 ~26 ~5 ~8 minecraft:dark_oak_planks
fill ~26 ~5 ~10 ~26 ~5 ~11 minecraft:oak_planks
fill ~27 ~5 ~7 ~27 ~5 ~8 minecraft:oak_planks
setblock ~27 ~5 ~9 minecraft:spruce_planks
fill ~27 ~5 ~10 ~28 ~5 ~10 minecraft:dark_oak_planks
fill ~27 ~5 ~11 ~28 ~5 ~11 minecraft:spruce_planks
setblock ~28 ~5 ~7 minecraft:dark_oak_planks
fill ~28 ~5 ~8 ~28 ~5 ~9 minecraft:spruce_planks
setblock ~22 ~6 ~8 minecraft:air
setblock ~22 ~6 ~11 minecraft:air
fill ~23 ~6 ~7 ~27 ~6 ~10 minecraft:air

# the parapet of bottom slabs at y 6 on the open sides (south and east, the west side from the second cell; the entry from the ladder at (22, 6, 11) stays open), posts at the corners with a lantern on the south-east one
setblock ~22 ~6 ~7 minecraft:oak_log[axis=y]
fill ~22 ~6 ~9 ~22 ~6 ~10 minecraft:oak_slab[type=bottom]
fill ~23 ~6 ~11 ~27 ~6 ~11 minecraft:oak_slab[type=bottom]
setblock ~28 ~6 ~7 minecraft:oak_log[axis=y]
fill ~28 ~6 ~8 ~28 ~6 ~10 minecraft:oak_slab[type=bottom]
setblock ~28 ~6 ~11 minecraft:oak_log[axis=y]

# the ladder up the front wall at x 22 from the yard to the deck (y 1..5)
fill ~22 ~1 ~12 ~22 ~5 ~12 minecraft:ladder[facing=south]

# the yard walls (y 1..2): fieldstone, an oak rail along the top away from the gates
fill ~0 ~1 ~12 ~0 ~2 ~12 minecraft:stone
fill ~0 ~1 ~13 ~0 ~1 ~14 minecraft:coarse_dirt
setblock ~0 ~1 ~16 minecraft:mossy_cobblestone
setblock ~0 ~1 ~18 minecraft:stone
setblock ~0 ~1 ~20 minecraft:mossy_cobblestone
setblock ~0 ~1 ~22 minecraft:mossy_cobblestone
fill ~0 ~1 ~23 ~0 ~2 ~23 minecraft:cobblestone
setblock ~0 ~1 ~24 minecraft:stone
fill ~0 ~1 ~25 ~0 ~2 ~25 minecraft:andesite
fill ~0 ~1 ~26 ~0 ~1 ~27 minecraft:cobblestone
setblock ~0 ~1 ~29 minecraft:mossy_cobblestone
setblock ~0 ~1 ~31 minecraft:cobblestone
fill ~0 ~1 ~33 ~0 ~1 ~34 minecraft:mossy_cobblestone
fill ~0 ~1 ~35 ~0 ~2 ~35 minecraft:cobblestone
fill ~0 ~1 ~36 ~0 ~2 ~36 minecraft:andesite
setblock ~1 ~1 ~36 minecraft:cobblestone
setblock ~2 ~1 ~36 minecraft:gravel
setblock ~3 ~1 ~36 minecraft:andesite
fill ~4 ~1 ~36 ~4 ~2 ~36 minecraft:mossy_cobblestone
fill ~5 ~1 ~36 ~6 ~1 ~36 minecraft:stone
fill ~7 ~1 ~36 ~9 ~1 ~36 minecraft:cobblestone
setblock ~21 ~1 ~36 minecraft:gravel
fill ~22 ~1 ~36 ~22 ~2 ~36 minecraft:andesite
fill ~23 ~1 ~36 ~24 ~1 ~36 minecraft:cobblestone
setblock ~25 ~1 ~36 minecraft:andesite
setblock ~26 ~1 ~36 minecraft:cobblestone
setblock ~27 ~1 ~36 minecraft:stone
fill ~28 ~1 ~36 ~29 ~1 ~36 minecraft:cobblestone
fill ~30 ~1 ~12 ~30 ~2 ~12 minecraft:cobblestone
setblock ~30 ~1 ~13 minecraft:mossy_cobblestone
setblock ~30 ~1 ~14 minecraft:cobblestone
setblock ~30 ~1 ~15 minecraft:coarse_dirt
setblock ~30 ~1 ~16 minecraft:andesite
setblock ~30 ~1 ~17 minecraft:gravel
setblock ~30 ~1 ~18 minecraft:andesite
setblock ~30 ~1 ~19 minecraft:cobblestone
setblock ~30 ~1 ~20 minecraft:andesite
setblock ~30 ~1 ~28 minecraft:stone
setblock ~30 ~1 ~29 minecraft:mossy_cobblestone
fill ~30 ~1 ~30 ~30 ~2 ~30 minecraft:andesite
fill ~30 ~1 ~31 ~30 ~2 ~31 minecraft:cobblestone
setblock ~30 ~1 ~32 minecraft:stone
setblock ~30 ~1 ~33 minecraft:andesite
fill ~30 ~1 ~34 ~30 ~2 ~34 minecraft:stone
setblock ~30 ~1 ~35 minecraft:coarse_dirt
fill ~30 ~1 ~36 ~30 ~2 ~36 minecraft:stone
setblock ~0 ~2 ~13 minecraft:mossy_cobblestone
setblock ~0 ~2 ~14 minecraft:cobblestone
setblock ~0 ~2 ~15 minecraft:stone
setblock ~0 ~2 ~16 minecraft:cobblestone
setblock ~0 ~2 ~17 minecraft:stone
fill ~0 ~2 ~18 ~0 ~2 ~19 minecraft:cobblestone
fill ~0 ~2 ~20 ~0 ~2 ~21 minecraft:stone
setblock ~0 ~2 ~22 minecraft:andesite
setblock ~0 ~2 ~24 minecraft:andesite
fill ~0 ~2 ~26 ~0 ~2 ~27 minecraft:stone
setblock ~0 ~2 ~28 minecraft:cobblestone
fill ~0 ~2 ~29 ~0 ~2 ~31 minecraft:stone
fill ~0 ~2 ~32 ~0 ~2 ~33 minecraft:cobblestone
setblock ~0 ~2 ~34 minecraft:stone_bricks
setblock ~1 ~2 ~36 minecraft:stone
setblock ~2 ~2 ~36 minecraft:andesite
setblock ~3 ~2 ~36 minecraft:stone_bricks
setblock ~5 ~2 ~36 minecraft:andesite
setblock ~6 ~2 ~36 minecraft:stone_bricks
setblock ~7 ~2 ~36 minecraft:cobblestone
setblock ~8 ~2 ~36 minecraft:mossy_cobblestone
setblock ~9 ~2 ~36 minecraft:cobblestone
setblock ~21 ~2 ~36 minecraft:stone
setblock ~23 ~2 ~36 minecraft:stone
fill ~24 ~2 ~36 ~25 ~2 ~36 minecraft:cobblestone
setblock ~26 ~2 ~36 minecraft:stone_bricks
fill ~27 ~2 ~36 ~28 ~2 ~36 minecraft:andesite
setblock ~29 ~2 ~36 minecraft:cobblestone
fill ~30 ~2 ~13 ~30 ~2 ~14 minecraft:andesite
fill ~30 ~2 ~15 ~30 ~2 ~18 minecraft:cobblestone
setblock ~30 ~2 ~19 minecraft:stone
setblock ~30 ~2 ~20 minecraft:cobblestone
fill ~30 ~2 ~28 ~30 ~2 ~29 minecraft:cobblestone
setblock ~30 ~2 ~32 minecraft:stone_bricks
setblock ~30 ~2 ~33 minecraft:cobblestone
setblock ~30 ~2 ~35 minecraft:stone

# the gate posts of the front gate (x 10 and 20, y 1..2) with the braziers on them, unlit campfires at (10, 3, 36) and (20, 3, 36)
setblock ~10 ~1 ~36 minecraft:oak_log[axis=y]
setblock ~20 ~1 ~36 minecraft:oak_log[axis=y]
setblock ~10 ~2 ~36 minecraft:stripped_oak_log[axis=y]
setblock ~20 ~2 ~36 minecraft:stripped_oak_log[axis=y]
setblock ~10 ~3 ~36 minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false]
setblock ~20 ~3 ~36 minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false]

# the side gate in the east wall (z 22..26): posts at z 21 and 27 four high and a beam over the opening at y 5, clear below
fill ~30 ~1 ~21 ~30 ~4 ~21 minecraft:oak_log[axis=y]
fill ~30 ~1 ~27 ~30 ~4 ~27 minecraft:oak_log[axis=y]
fill ~30 ~5 ~21 ~30 ~5 ~27 minecraft:oak_log[axis=z]

# the yard walls (y 1..2): fieldstone, an oak rail along the top away from the gates: the oak rail of fence along the top of the yard walls, away from the gates
setblock ~0 ~3 ~13 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~0 ~3 ~23 minecraft:oak_fence[north=false,east=false,south=true,west=false]
fill ~0 ~3 ~24 ~0 ~3 ~25 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~0 ~3 ~26 minecraft:oak_fence[north=true,east=false,south=false,west=false]
setblock ~0 ~3 ~35 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~1 ~3 ~36 minecraft:oak_fence[north=false,east=true,south=false,west=false]
setblock ~2 ~3 ~36 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~3 ~3 ~36 minecraft:oak_fence[north=true,east=true,south=false,west=true]
fill ~4 ~3 ~36 ~6 ~3 ~36 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~7 ~3 ~36 minecraft:oak_fence[north=false,east=false,south=false,west=true]
setblock ~23 ~3 ~36 minecraft:oak_fence[north=false,east=true,south=false,west=false]
fill ~24 ~3 ~36 ~27 ~3 ~36 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~28 ~3 ~36 minecraft:oak_fence[north=true,east=true,south=false,west=true]
setblock ~29 ~3 ~36 minecraft:oak_fence[north=false,east=false,south=false,west=true]
setblock ~30 ~3 ~13 minecraft:oak_fence[north=false,east=false,south=true,west=false]
fill ~30 ~3 ~14 ~30 ~3 ~18 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~30 ~3 ~19 minecraft:oak_fence[north=true,east=false,south=false,west=true]
setblock ~30 ~3 ~29 minecraft:oak_fence[north=false,east=false,south=true,west=false]
fill ~30 ~3 ~30 ~30 ~3 ~33 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~30 ~3 ~34 minecraft:oak_fence[north=true,east=false,south=true,west=true]
setblock ~30 ~3 ~35 minecraft:oak_fence[north=true,east=false,south=false,west=false]

# the forge corner: a lean-to roof of slabs at y 3 against the west wall on posts
fill ~4 ~1 ~14 ~4 ~2 ~14 minecraft:oak_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~18 ~4 ~2 ~18 minecraft:oak_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~22 ~4 ~2 ~22 minecraft:oak_fence[north=false,east=false,south=false,west=false]
fill ~0 ~3 ~14 ~4 ~3 ~22 minecraft:oak_slab[type=bottom]

# the forge: anvil, grindstone, smithing table, furnace, cauldron, barrels
setblock ~0 ~1 ~15 minecraft:anvil[facing=east]
setblock ~0 ~1 ~17 minecraft:grindstone[face=floor,facing=east]
setblock ~0 ~1 ~19 minecraft:smithing_table
setblock ~0 ~1 ~21 minecraft:blast_furnace[facing=east,lit=false]
setblock ~1 ~1 ~22 minecraft:cauldron
setblock ~3 ~1 ~16 minecraft:barrel[facing=up,open=false]
setblock ~3 ~1 ~20 minecraft:barrel[facing=up,open=false]

# the cots: a lean-to of slabs over four beds, chests and barrels between them
fill ~4 ~1 ~27 ~4 ~2 ~27 minecraft:oak_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~30 ~4 ~2 ~30 minecraft:oak_fence[north=false,east=true,south=false,west=false]
fill ~4 ~1 ~34 ~4 ~2 ~34 minecraft:oak_fence[north=false,east=false,south=false,west=false]
fill ~0 ~3 ~27 ~4 ~3 ~34 minecraft:oak_slab[type=bottom]

# the beds, a chest and barrels
setblock ~0 ~1 ~28 minecraft:brown_bed[facing=east,part=foot,occupied=false]
setblock ~0 ~1 ~30 minecraft:brown_bed[facing=east,part=foot,occupied=false]
setblock ~0 ~1 ~32 minecraft:brown_bed[facing=east,part=foot,occupied=false]
setblock ~1 ~1 ~28 minecraft:brown_bed[facing=east,part=head,occupied=false]
setblock ~1 ~1 ~30 minecraft:brown_bed[facing=east,part=head,occupied=false]
setblock ~1 ~1 ~32 minecraft:brown_bed[facing=east,part=head,occupied=false]
setblock ~2 ~1 ~29 minecraft:barrel[facing=up,open=false]
setblock ~2 ~1 ~31 minecraft:chest[facing=west,type=single,waterlogged=false]
setblock ~2 ~1 ~33 minecraft:barrel[facing=up,open=false]

# the tithe table: a counter of oak planks with a lectern, a bookshelf of ledgers at the back, two chests and a barrel by it
fill ~6 ~1 ~12 ~10 ~1 ~12 minecraft:oak_planks
setblock ~6 ~1 ~13 minecraft:chest[facing=north,type=single,waterlogged=false]
setblock ~8 ~1 ~13 minecraft:barrel[facing=up,open=false]
setblock ~10 ~1 ~13 minecraft:chest[facing=north,type=single,waterlogged=false]
setblock ~6 ~2 ~12 minecraft:bookshelf
setblock ~7 ~2 ~12 minecraft:lectern[facing=south,has_book=false,powered=false]
setblock ~9 ~2 ~12 minecraft:oak_slab[type=bottom]
setblock ~10 ~2 ~12 minecraft:bookshelf

# a weapon rack of blocks: stripped logs with pike heads of lightning rods and iron bars
setblock ~24 ~1 ~12 minecraft:stripped_oak_log[axis=x]
setblock ~25 ~1 ~12 minecraft:iron_bars[north=true,east=true,south=false,west=true]
setblock ~26 ~1 ~12 minecraft:stripped_oak_log[axis=x]
setblock ~27 ~1 ~12 minecraft:iron_bars[north=true,east=true,south=false,west=true]
setblock ~28 ~1 ~12 minecraft:stripped_oak_log[axis=x]
setblock ~24 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~25 ~2 ~12 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~26 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~27 ~2 ~12 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~28 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]

# a cart: a plank bed on four fence legs with wheels of trapdoors, its load and a tongue
fill ~24 ~1 ~15 ~24 ~1 ~16 minecraft:oak_trapdoor[facing=west,half=bottom,open=true]
setblock ~25 ~1 ~14 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~25 ~1 ~17 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~26 ~1 ~19 minecraft:oak_fence[north=false,east=true,south=false,west=false]
setblock ~27 ~1 ~14 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~27 ~1 ~17 minecraft:oak_fence[north=false,east=false,south=false,west=false]
fill ~28 ~1 ~15 ~28 ~1 ~16 minecraft:oak_trapdoor[facing=east,half=bottom,open=true]
fill ~24 ~2 ~15 ~24 ~2 ~16 minecraft:oak_trapdoor[facing=west,half=top,open=true]
fill ~25 ~2 ~14 ~27 ~2 ~17 minecraft:oak_planks
setblock ~26 ~2 ~18 minecraft:oak_stairs[facing=north,half=top]
fill ~28 ~2 ~15 ~28 ~2 ~16 minecraft:oak_trapdoor[facing=east,half=top,open=true]
setblock ~25 ~3 ~15 minecraft:hay_block[axis=x]
setblock ~26 ~3 ~15 minecraft:barrel[facing=up,open=false]
setblock ~26 ~3 ~16 minecraft:chest[facing=north,type=single,waterlogged=false]
setblock ~27 ~3 ~16 minecraft:hay_block[axis=x]

# the wheat plot: farmland with ripe wheat, a low fence of oak on the south and west sides, a scarecrow
fill ~26 ~0 ~28 ~29 ~0 ~31 minecraft:farmland[moisture=7]
setblock ~25 ~1 ~28 minecraft:oak_fence[north=false,east=false,south=true,west=false]
fill ~25 ~1 ~29 ~25 ~1 ~31 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~25 ~1 ~32 minecraft:oak_fence[north=true,east=true,south=false,west=false]
fill ~26 ~1 ~28 ~29 ~1 ~28 minecraft:wheat[age=7]
setblock ~26 ~1 ~29 minecraft:wheat[age=5]
fill ~26 ~1 ~30 ~26 ~1 ~31 minecraft:wheat[age=7]
fill ~26 ~1 ~32 ~29 ~1 ~32 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~27 ~1 ~29 minecraft:oak_fence[north=false,east=false,south=false,west=false]
fill ~27 ~1 ~30 ~28 ~1 ~30 minecraft:wheat[age=5]
fill ~27 ~1 ~31 ~28 ~1 ~31 minecraft:wheat[age=7]
fill ~28 ~1 ~29 ~29 ~1 ~29 minecraft:wheat[age=7]
fill ~29 ~1 ~30 ~29 ~1 ~31 minecraft:wheat[age=7]
setblock ~26 ~2 ~29 minecraft:oak_fence[north=false,east=true,south=false,west=false]
setblock ~27 ~2 ~29 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~28 ~2 ~29 minecraft:oak_fence[north=false,east=false,south=false,west=true]
setblock ~27 ~3 ~29 minecraft:hay_block[axis=y]
setblock ~27 ~4 ~29 minecraft:carved_pumpkin[facing=south]

# the tithe bell: a frame of two oak posts and a beam, the bell hung in the middle
fill ~27 ~1 ~19 ~27 ~4 ~19 minecraft:oak_log[axis=y]
fill ~29 ~1 ~19 ~29 ~4 ~19 minecraft:oak_log[axis=y]
setblock ~28 ~3 ~19 minecraft:bell[attachment=ceiling,facing=north,powered=false]
setblock ~28 ~4 ~19 minecraft:oak_log[axis=x]

# stooks of bound hay along the south wall and in the south-west corner, a stake of fence on each
fill ~3 ~1 ~35 ~3 ~2 ~35 minecraft:hay_block[axis=y]
setblock ~6 ~1 ~34 minecraft:hay_block[axis=y]
setblock ~7 ~1 ~35 minecraft:hay_block[axis=y]
fill ~8 ~1 ~33 ~8 ~2 ~33 minecraft:hay_block[axis=y]
fill ~21 ~1 ~34 ~21 ~2 ~34 minecraft:hay_block[axis=y]
fill ~28 ~1 ~35 ~28 ~2 ~35 minecraft:hay_block[axis=y]
fill ~29 ~1 ~34 ~29 ~2 ~34 minecraft:hay_block[axis=y]
setblock ~6 ~2 ~34 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~7 ~2 ~35 minecraft:oak_fence[north=false,east=false,south=true,west=false]
setblock ~3 ~3 ~35 minecraft:oak_fence[north=false,east=false,south=true,west=false]
setblock ~8 ~3 ~33 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~21 ~3 ~34 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~28 ~3 ~35 minecraft:oak_fence[north=false,east=false,south=true,west=false]
setblock ~29 ~3 ~34 minecraft:oak_fence[north=false,east=true,south=false,west=false]

# hay in the yard: a stack of bales under the south-west corner of the cots, a broken bale on the ground
fill ~5 ~1 ~30 ~5 ~1 ~31 minecraft:hay_block[axis=x]
fill ~6 ~1 ~30 ~6 ~1 ~31 minecraft:hay_block[axis=z]
fill ~5 ~2 ~30 ~5 ~2 ~31 minecraft:hay_block[axis=z]
setblock ~6 ~2 ~31 minecraft:hay_block[axis=x]

# crates and barrels along the front wall of the storehouse and in the corners
setblock ~1 ~1 ~12 minecraft:barrel[facing=north,open=false]
setblock ~1 ~1 ~24 minecraft:barrel[facing=west,open=false]
setblock ~1 ~1 ~25 minecraft:chest[facing=west,type=single,waterlogged=false]
setblock ~2 ~1 ~12 minecraft:spruce_planks
setblock ~2 ~1 ~35 minecraft:barrel[facing=up,open=false]
setblock ~3 ~1 ~12 minecraft:barrel[facing=west,open=false]
setblock ~9 ~1 ~35 minecraft:barrel[facing=up,open=false]
setblock ~12 ~1 ~12 minecraft:chest[facing=east,type=single,waterlogged=false]
setblock ~19 ~1 ~12 minecraft:spruce_planks
setblock ~20 ~1 ~12 minecraft:barrel[facing=up,open=false]
setblock ~29 ~1 ~12 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~1 ~2 ~12 minecraft:chest[facing=west,type=single,waterlogged=false]
setblock ~1 ~2 ~24 minecraft:barrel[facing=up,open=false]
setblock ~2 ~2 ~12 minecraft:barrel[facing=up,open=false]
setblock ~2 ~2 ~35 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~12 ~2 ~12 minecraft:barrel[facing=east,open=false]
setblock ~19 ~2 ~12 minecraft:chest[facing=east,type=single,waterlogged=false]
setblock ~20 ~2 ~12 minecraft:barrel[facing=north,open=false]
setblock ~29 ~2 ~12 minecraft:spruce_planks

# inside the storehouse: sacks of grain (hay and composters), barrels, chests of the tithe and a lantern, seen through the door and the panes
fill ~3 ~1 ~2 ~3 ~1 ~3 minecraft:barrel[facing=up,open=false]
setblock ~4 ~1 ~2 minecraft:chest[facing=south,type=single,waterlogged=false]
fill ~5 ~1 ~9 ~7 ~1 ~9 minecraft:hay_block[axis=x]
setblock ~6 ~1 ~2 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~8 ~1 ~9 minecraft:composter[level=3]
setblock ~9 ~1 ~9 minecraft:barrel[facing=up,open=false]
fill ~12 ~1 ~2 ~13 ~1 ~2 minecraft:barrel[facing=up,open=false]
setblock ~15 ~1 ~9 minecraft:barrel[facing=up,open=false]
setblock ~17 ~1 ~2 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~18 ~1 ~2 minecraft:composter[level=3]
setblock ~19 ~1 ~3 minecraft:composter[level=6]
fill ~21 ~1 ~9 ~22 ~1 ~9 minecraft:hay_block[axis=x]
setblock ~24 ~1 ~3 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~15 ~2 ~9 minecraft:lantern[hanging=false]
