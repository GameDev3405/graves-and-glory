# size 31 9 37
# supply/frostmarch: supply house of Frostmarch, ridge hut and walled yard: dark spruce and stone under a snowy roof, hides on frames, a woodpile, a sled, a forge corner, a lookout and the yard with the capture circle.
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
fill ~5 ~1 ~2 ~11 ~4 ~8 minecraft:air
fill ~5 ~1 ~10 ~6 ~3 ~10 minecraft:air
fill ~5 ~1 ~14 ~5 ~8 ~35 minecraft:air
fill ~6 ~1 ~13 ~10 ~8 ~34 minecraft:air
fill ~6 ~1 ~35 ~8 ~8 ~35 minecraft:air
fill ~7 ~1 ~9 ~7 ~3 ~10 minecraft:air
fill ~8 ~1 ~10 ~9 ~3 ~10 minecraft:air
fill ~10 ~1 ~9 ~11 ~3 ~10 minecraft:air
fill ~10 ~1 ~35 ~10 ~8 ~35 minecraft:air
fill ~11 ~1 ~12 ~18 ~8 ~20 minecraft:air
fill ~11 ~1 ~21 ~11 ~8 ~36 minecraft:air
fill ~12 ~1 ~3 ~13 ~3 ~10 minecraft:air
fill ~12 ~1 ~28 ~18 ~8 ~36 minecraft:air
fill ~14 ~1 ~2 ~16 ~4 ~8 minecraft:air
fill ~14 ~1 ~9 ~14 ~3 ~10 minecraft:air
fill ~15 ~1 ~10 ~15 ~3 ~10 minecraft:air
fill ~16 ~1 ~9 ~16 ~3 ~10 minecraft:air
fill ~17 ~1 ~3 ~17 ~3 ~10 minecraft:air
fill ~18 ~1 ~2 ~29 ~4 ~2 minecraft:air
fill ~18 ~1 ~4 ~18 ~3 ~10 minecraft:air
fill ~19 ~1 ~3 ~23 ~4 ~6 minecraft:air
fill ~19 ~1 ~7 ~21 ~4 ~8 minecraft:air
fill ~19 ~1 ~9 ~20 ~3 ~10 minecraft:air
fill ~19 ~1 ~13 ~20 ~8 ~35 minecraft:air
fill ~19 ~1 ~36 ~19 ~8 ~36 minecraft:air
fill ~21 ~1 ~10 ~21 ~3 ~10 minecraft:air
fill ~21 ~1 ~12 ~21 ~8 ~35 minecraft:air
fill ~22 ~1 ~8 ~22 ~3 ~8 minecraft:air
fill ~22 ~1 ~13 ~22 ~8 ~35 minecraft:air
fill ~23 ~1 ~7 ~23 ~3 ~10 minecraft:air
fill ~23 ~1 ~12 ~23 ~8 ~35 minecraft:air
fill ~24 ~1 ~4 ~24 ~3 ~10 minecraft:air
fill ~24 ~1 ~13 ~29 ~8 ~13 minecraft:air
fill ~24 ~1 ~14 ~24 ~8 ~35 minecraft:air
fill ~25 ~1 ~3 ~29 ~4 ~6 minecraft:air
fill ~25 ~1 ~7 ~27 ~3 ~10 minecraft:air
fill ~25 ~1 ~19 ~25 ~8 ~35 minecraft:air
fill ~26 ~1 ~15 ~26 ~1 ~16 minecraft:air
setblock ~26 ~1 ~18 minecraft:air
fill ~26 ~1 ~20 ~26 ~8 ~35 minecraft:air
fill ~27 ~1 ~19 ~27 ~8 ~27 minecraft:air
fill ~27 ~1 ~34 ~29 ~8 ~35 minecraft:air
fill ~28 ~1 ~8 ~28 ~3 ~9 minecraft:air
fill ~28 ~1 ~14 ~29 ~8 ~27 minecraft:air
fill ~29 ~1 ~7 ~29 ~3 ~10 minecraft:air
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
fill ~3 ~2 ~35 ~3 ~8 ~35 minecraft:air
fill ~8 ~2 ~9 ~9 ~4 ~9 minecraft:air
fill ~12 ~2 ~2 ~13 ~4 ~2 minecraft:air
fill ~17 ~2 ~2 ~17 ~4 ~2 minecraft:air
fill ~18 ~2 ~3 ~18 ~4 ~3 minecraft:air
fill ~24 ~2 ~3 ~24 ~4 ~3 minecraft:air
fill ~26 ~2 ~19 ~26 ~8 ~19 minecraft:air
fill ~28 ~2 ~28 ~28 ~8 ~28 minecraft:air
fill ~28 ~2 ~30 ~28 ~8 ~30 minecraft:air
fill ~29 ~2 ~12 ~29 ~8 ~12 minecraft:air
fill ~0 ~3 ~12 ~2 ~8 ~13 minecraft:air
fill ~0 ~3 ~23 ~0 ~8 ~26 minecraft:air
fill ~0 ~3 ~35 ~2 ~8 ~36 minecraft:air
fill ~1 ~3 ~24 ~1 ~8 ~24 minecraft:air
fill ~1 ~3 ~26 ~1 ~8 ~26 minecraft:air
fill ~2 ~3 ~23 ~3 ~8 ~26 minecraft:air
fill ~3 ~3 ~13 ~3 ~8 ~13 minecraft:air
fill ~3 ~3 ~36 ~8 ~8 ~36 minecraft:air
fill ~5 ~3 ~9 ~6 ~4 ~9 minecraft:air
fill ~9 ~3 ~35 ~9 ~8 ~36 minecraft:air
fill ~15 ~3 ~9 ~15 ~4 ~9 minecraft:air
fill ~19 ~3 ~12 ~20 ~8 ~12 minecraft:air
fill ~21 ~3 ~9 ~22 ~3 ~9 minecraft:air
fill ~21 ~3 ~36 ~29 ~8 ~36 minecraft:air
fill ~24 ~3 ~12 ~28 ~8 ~12 minecraft:air
fill ~25 ~3 ~14 ~27 ~8 ~14 minecraft:air
fill ~25 ~3 ~17 ~27 ~8 ~18 minecraft:air
fill ~27 ~3 ~31 ~28 ~8 ~32 minecraft:air
fill ~28 ~3 ~29 ~28 ~8 ~29 minecraft:air
fill ~28 ~3 ~33 ~28 ~8 ~33 minecraft:air
fill ~29 ~3 ~28 ~30 ~8 ~28 minecraft:air
fill ~30 ~3 ~12 ~30 ~8 ~20 minecraft:air
fill ~30 ~3 ~29 ~30 ~8 ~36 minecraft:air
fill ~0 ~4 ~15 ~0 ~8 ~15 minecraft:air
fill ~0 ~4 ~21 ~0 ~8 ~21 minecraft:air
fill ~0 ~4 ~27 ~0 ~8 ~27 minecraft:air
fill ~0 ~4 ~32 ~0 ~8 ~32 minecraft:air
fill ~1 ~4 ~2 ~2 ~4 ~9 minecraft:air
fill ~2 ~4 ~14 ~2 ~8 ~15 minecraft:air
fill ~2 ~4 ~32 ~4 ~8 ~32 minecraft:air
fill ~2 ~4 ~34 ~2 ~8 ~34 minecraft:air
fill ~3 ~4 ~4 ~3 ~4 ~9 minecraft:air
fill ~3 ~4 ~15 ~3 ~8 ~15 minecraft:air
fill ~3 ~4 ~28 ~3 ~8 ~29 minecraft:air
fill ~4 ~4 ~3 ~4 ~4 ~9 minecraft:air
fill ~4 ~4 ~16 ~4 ~8 ~16 minecraft:air
fill ~6 ~4 ~12 ~7 ~8 ~12 minecraft:air
setblock ~7 ~4 ~9 minecraft:air
fill ~9 ~4 ~12 ~10 ~8 ~12 minecraft:air
fill ~10 ~4 ~9 ~11 ~4 ~9 minecraft:air
fill ~10 ~4 ~36 ~10 ~8 ~36 minecraft:air
fill ~12 ~4 ~3 ~13 ~4 ~9 minecraft:air
setblock ~14 ~4 ~9 minecraft:air
setblock ~16 ~4 ~9 minecraft:air
fill ~17 ~4 ~3 ~17 ~4 ~9 minecraft:air
fill ~18 ~4 ~4 ~18 ~4 ~9 minecraft:air
fill ~19 ~4 ~9 ~21 ~4 ~9 minecraft:air
fill ~20 ~4 ~36 ~20 ~8 ~36 minecraft:air
fill ~24 ~4 ~4 ~24 ~5 ~6 minecraft:air
fill ~25 ~4 ~15 ~27 ~8 ~16 minecraft:air
fill ~27 ~4 ~28 ~27 ~8 ~30 minecraft:air
fill ~27 ~4 ~33 ~27 ~8 ~33 minecraft:air
fill ~29 ~4 ~7 ~29 ~4 ~9 minecraft:air
fill ~29 ~4 ~29 ~29 ~8 ~33 minecraft:air
fill ~0 ~5 ~0 ~30 ~8 ~0 minecraft:air
fill ~0 ~5 ~1 ~29 ~8 ~1 minecraft:air
fill ~0 ~5 ~11 ~0 ~8 ~11 minecraft:air
fill ~0 ~5 ~14 ~1 ~8 ~14 minecraft:air
fill ~0 ~5 ~16 ~0 ~8 ~20 minecraft:air
fill ~0 ~5 ~22 ~0 ~8 ~22 minecraft:air
fill ~0 ~5 ~28 ~2 ~8 ~31 minecraft:air
fill ~0 ~5 ~33 ~0 ~8 ~34 minecraft:air
fill ~1 ~5 ~4 ~23 ~5 ~6 minecraft:air
fill ~1 ~5 ~7 ~21 ~5 ~7 minecraft:air
fill ~1 ~5 ~10 ~21 ~8 ~11 minecraft:air
fill ~1 ~5 ~15 ~1 ~8 ~22 minecraft:air
fill ~1 ~5 ~27 ~4 ~8 ~27 minecraft:air
fill ~1 ~5 ~32 ~1 ~8 ~34 minecraft:air
fill ~2 ~5 ~16 ~3 ~8 ~22 minecraft:air
fill ~2 ~5 ~33 ~4 ~8 ~33 minecraft:air
fill ~3 ~5 ~14 ~4 ~8 ~14 minecraft:air
fill ~3 ~5 ~30 ~3 ~8 ~31 minecraft:air
fill ~3 ~5 ~34 ~4 ~8 ~34 minecraft:air
fill ~4 ~5 ~15 ~4 ~8 ~15 minecraft:air
fill ~4 ~5 ~17 ~4 ~8 ~22 minecraft:air
fill ~4 ~5 ~28 ~4 ~8 ~31 minecraft:air
fill ~8 ~5 ~12 ~8 ~8 ~12 minecraft:air
fill ~25 ~5 ~4 ~29 ~5 ~6 minecraft:air
setblock ~29 ~5 ~7 minecraft:air
fill ~29 ~5 ~10 ~29 ~8 ~11 minecraft:air
fill ~30 ~5 ~11 ~30 ~8 ~11 minecraft:air
fill ~0 ~6 ~2 ~29 ~8 ~2 minecraft:air
fill ~0 ~6 ~9 ~0 ~8 ~10 minecraft:air
fill ~1 ~6 ~3 ~29 ~8 ~3 minecraft:air
fill ~1 ~6 ~8 ~21 ~8 ~9 minecraft:air
fill ~22 ~6 ~12 ~22 ~8 ~12 minecraft:air
fill ~29 ~6 ~8 ~29 ~8 ~9 minecraft:air
fill ~30 ~6 ~1 ~30 ~8 ~2 minecraft:air
fill ~30 ~6 ~9 ~30 ~8 ~10 minecraft:air
fill ~30 ~6 ~25 ~30 ~8 ~25 minecraft:air
fill ~0 ~7 ~3 ~0 ~8 ~8 minecraft:air
fill ~1 ~7 ~4 ~29 ~8 ~7 minecraft:air
fill ~22 ~7 ~8 ~28 ~8 ~11 minecraft:air
fill ~30 ~7 ~3 ~30 ~8 ~8 minecraft:air
fill ~30 ~7 ~21 ~30 ~8 ~24 minecraft:air
fill ~30 ~7 ~26 ~30 ~8 ~27 minecraft:air

# the ground layer: spruce and dark oak boards on the storehouse floor, the paved capture circle (stone brick, a ring of calcite, a plinth), gravel, mud and snow in the yard, a flagged mouth at the gate
fill ~0 ~0 ~0 ~0 ~0 ~1 minecraft:dark_oak_planks
fill ~0 ~0 ~2 ~1 ~0 ~2 minecraft:stone_bricks
setblock ~0 ~0 ~3 minecraft:spruce_planks
setblock ~0 ~0 ~4 minecraft:stone_bricks
setblock ~0 ~0 ~5 minecraft:spruce_planks
fill ~0 ~0 ~6 ~0 ~0 ~7 minecraft:dark_oak_planks
fill ~0 ~0 ~8 ~1 ~0 ~8 minecraft:spruce_planks
setblock ~0 ~0 ~9 minecraft:dark_oak_planks
setblock ~0 ~0 ~10 minecraft:spruce_planks
setblock ~0 ~0 ~11 minecraft:dark_oak_planks
setblock ~0 ~0 ~12 minecraft:gravel
fill ~0 ~0 ~13 ~1 ~0 ~13 minecraft:cobblestone
setblock ~0 ~0 ~14 minecraft:snow_block
setblock ~0 ~0 ~15 minecraft:andesite
setblock ~0 ~0 ~16 minecraft:cobblestone
setblock ~0 ~0 ~17 minecraft:andesite
setblock ~0 ~0 ~18 minecraft:cobblestone
fill ~0 ~0 ~19 ~0 ~0 ~20 minecraft:snow_block
setblock ~0 ~0 ~21 minecraft:coarse_dirt
fill ~0 ~0 ~22 ~0 ~0 ~23 minecraft:snow_block
setblock ~0 ~0 ~24 minecraft:cobblestone
setblock ~0 ~0 ~25 minecraft:gravel
setblock ~0 ~0 ~26 minecraft:snow_block
setblock ~0 ~0 ~27 minecraft:gravel
setblock ~0 ~0 ~28 minecraft:packed_mud
setblock ~0 ~0 ~29 minecraft:gravel
fill ~0 ~0 ~30 ~0 ~0 ~34 minecraft:cobblestone
fill ~0 ~0 ~35 ~1 ~0 ~35 minecraft:snow_block
setblock ~0 ~0 ~36 minecraft:gravel
fill ~1 ~0 ~0 ~5 ~0 ~0 minecraft:spruce_planks
fill ~1 ~0 ~1 ~3 ~0 ~1 minecraft:spruce_planks
setblock ~1 ~0 ~3 minecraft:stone_bricks
fill ~1 ~0 ~4 ~1 ~0 ~5 minecraft:dark_oak_planks
setblock ~1 ~0 ~6 minecraft:stone_bricks
fill ~1 ~0 ~7 ~3 ~0 ~7 minecraft:spruce_planks
fill ~1 ~0 ~9 ~2 ~0 ~9 minecraft:spruce_planks
fill ~1 ~0 ~10 ~1 ~0 ~11 minecraft:dark_oak_planks
setblock ~1 ~0 ~12 minecraft:andesite
setblock ~1 ~0 ~14 minecraft:gravel
setblock ~1 ~0 ~15 minecraft:snow_block
setblock ~1 ~0 ~16 minecraft:andesite
setblock ~1 ~0 ~17 minecraft:cobblestone
setblock ~1 ~0 ~18 minecraft:snow_block
setblock ~1 ~0 ~19 minecraft:cobblestone
fill ~1 ~0 ~20 ~2 ~0 ~20 minecraft:gravel
setblock ~1 ~0 ~21 minecraft:snow_block
setblock ~1 ~0 ~22 minecraft:cobblestone
setblock ~1 ~0 ~23 minecraft:gravel
setblock ~1 ~0 ~24 minecraft:packed_mud
fill ~1 ~0 ~25 ~1 ~0 ~26 minecraft:cobblestone
setblock ~1 ~0 ~27 minecraft:packed_mud
setblock ~1 ~0 ~28 minecraft:gravel
setblock ~1 ~0 ~29 minecraft:snow_block
setblock ~1 ~0 ~30 minecraft:packed_mud
setblock ~1 ~0 ~31 minecraft:cobblestone
fill ~1 ~0 ~32 ~2 ~0 ~32 minecraft:snow_block
setblock ~1 ~0 ~33 minecraft:gravel
setblock ~1 ~0 ~34 minecraft:cobblestone
setblock ~1 ~0 ~36 minecraft:cobblestone
fill ~2 ~0 ~2 ~3 ~0 ~2 minecraft:dark_oak_planks
fill ~2 ~0 ~3 ~2 ~0 ~5 minecraft:spruce_planks
fill ~2 ~0 ~6 ~3 ~0 ~6 minecraft:dark_oak_planks
setblock ~2 ~0 ~8 minecraft:stone_bricks
fill ~2 ~0 ~10 ~2 ~0 ~11 minecraft:spruce_planks
fill ~2 ~0 ~12 ~2 ~0 ~13 minecraft:gravel
setblock ~2 ~0 ~14 minecraft:snow_block
setblock ~2 ~0 ~15 minecraft:packed_mud
fill ~2 ~0 ~16 ~2 ~0 ~17 minecraft:gravel
fill ~2 ~0 ~18 ~2 ~0 ~19 minecraft:cobblestone
setblock ~2 ~0 ~21 minecraft:gravel
setblock ~2 ~0 ~22 minecraft:snow_block
setblock ~2 ~0 ~23 minecraft:coarse_dirt
setblock ~2 ~0 ~24 minecraft:gravel
setblock ~2 ~0 ~25 minecraft:coarse_dirt
setblock ~2 ~0 ~26 minecraft:cobblestone
setblock ~2 ~0 ~27 minecraft:coarse_dirt
fill ~2 ~0 ~28 ~2 ~0 ~29 minecraft:snow_block
setblock ~2 ~0 ~30 minecraft:cobblestone
setblock ~2 ~0 ~31 minecraft:packed_mud
setblock ~2 ~0 ~33 minecraft:packed_mud
setblock ~2 ~0 ~34 minecraft:snow_block
setblock ~2 ~0 ~35 minecraft:gravel
setblock ~2 ~0 ~36 minecraft:packed_mud
setblock ~3 ~0 ~3 minecraft:dark_oak_planks
fill ~3 ~0 ~4 ~3 ~0 ~5 minecraft:spruce_planks
fill ~3 ~0 ~8 ~4 ~0 ~9 minecraft:spruce_planks
fill ~3 ~0 ~10 ~4 ~0 ~11 minecraft:dark_oak_planks
setblock ~3 ~0 ~12 minecraft:snow_block
setblock ~3 ~0 ~13 minecraft:packed_mud
setblock ~3 ~0 ~14 minecraft:gravel
setblock ~3 ~0 ~15 minecraft:andesite
setblock ~3 ~0 ~16 minecraft:packed_mud
setblock ~3 ~0 ~17 minecraft:snow_block
fill ~3 ~0 ~18 ~3 ~0 ~19 minecraft:packed_mud
fill ~3 ~0 ~20 ~5 ~0 ~21 minecraft:cobblestone
fill ~3 ~0 ~22 ~3 ~0 ~23 minecraft:cobblestone
setblock ~3 ~0 ~24 minecraft:andesite
setblock ~3 ~0 ~25 minecraft:snow_block
fill ~3 ~0 ~26 ~4 ~0 ~26 minecraft:packed_mud
setblock ~3 ~0 ~27 minecraft:gravel
setblock ~3 ~0 ~28 minecraft:cobblestone
setblock ~3 ~0 ~29 minecraft:coarse_dirt
fill ~3 ~0 ~30 ~3 ~0 ~34 minecraft:snow_block
setblock ~3 ~0 ~35 minecraft:andesite
setblock ~3 ~0 ~36 minecraft:snow_block
setblock ~4 ~0 ~1 minecraft:stone_bricks
fill ~4 ~0 ~2 ~4 ~0 ~3 minecraft:spruce_planks
setblock ~4 ~0 ~4 minecraft:stone_bricks
setblock ~4 ~0 ~5 minecraft:spruce_planks
setblock ~4 ~0 ~6 minecraft:stone_bricks
setblock ~4 ~0 ~7 minecraft:dark_oak_planks
fill ~4 ~0 ~12 ~4 ~0 ~14 minecraft:packed_mud
setblock ~4 ~0 ~15 minecraft:gravel
setblock ~4 ~0 ~16 minecraft:snow_block
setblock ~4 ~0 ~17 minecraft:cobblestone
fill ~4 ~0 ~18 ~4 ~0 ~19 minecraft:coarse_dirt
setblock ~4 ~0 ~22 minecraft:snow_block
fill ~4 ~0 ~23 ~4 ~0 ~25 minecraft:cobblestone
setblock ~4 ~0 ~27 minecraft:cobblestone
fill ~4 ~0 ~28 ~5 ~0 ~29 minecraft:snow_block
setblock ~4 ~0 ~30 minecraft:cobblestone
fill ~4 ~0 ~31 ~4 ~0 ~32 minecraft:coarse_dirt
fill ~4 ~0 ~33 ~5 ~0 ~33 minecraft:packed_mud
setblock ~4 ~0 ~34 minecraft:gravel
setblock ~4 ~0 ~35 minecraft:cobblestone
setblock ~4 ~0 ~36 minecraft:coarse_dirt
setblock ~5 ~0 ~1 minecraft:spruce_planks
setblock ~5 ~0 ~2 minecraft:stone_bricks
setblock ~5 ~0 ~3 minecraft:spruce_planks
setblock ~5 ~0 ~4 minecraft:dark_oak_planks
setblock ~5 ~0 ~5 minecraft:stone_bricks
fill ~5 ~0 ~6 ~7 ~0 ~6 minecraft:spruce_planks
fill ~5 ~0 ~7 ~5 ~0 ~10 minecraft:spruce_planks
setblock ~5 ~0 ~11 minecraft:dark_oak_planks
setblock ~5 ~0 ~12 minecraft:cobblestone
setblock ~5 ~0 ~13 minecraft:coarse_dirt
setblock ~5 ~0 ~14 minecraft:cobblestone
setblock ~5 ~0 ~15 minecraft:snow_block
setblock ~5 ~0 ~16 minecraft:coarse_dirt
fill ~5 ~0 ~17 ~6 ~0 ~17 minecraft:snow_block
setblock ~5 ~0 ~18 minecraft:cobblestone
setblock ~5 ~0 ~19 minecraft:snow_block
setblock ~5 ~0 ~22 minecraft:coarse_dirt
setblock ~5 ~0 ~23 minecraft:snow_block
setblock ~5 ~0 ~24 minecraft:calcite
setblock ~5 ~0 ~25 minecraft:coarse_dirt
fill ~5 ~0 ~26 ~5 ~0 ~27 minecraft:cobblestone
setblock ~5 ~0 ~30 minecraft:coarse_dirt
setblock ~5 ~0 ~31 minecraft:gravel
setblock ~5 ~0 ~32 minecraft:andesite
setblock ~5 ~0 ~34 minecraft:packed_mud
fill ~5 ~0 ~35 ~5 ~0 ~36 minecraft:coarse_dirt
fill ~6 ~0 ~0 ~7 ~0 ~1 minecraft:dark_oak_planks
setblock ~6 ~0 ~2 minecraft:spruce_planks
setblock ~6 ~0 ~3 minecraft:stone_bricks
setblock ~6 ~0 ~4 minecraft:spruce_planks
fill ~6 ~0 ~5 ~7 ~0 ~5 minecraft:dark_oak_planks
fill ~6 ~0 ~7 ~7 ~0 ~7 minecraft:dark_oak_planks
setblock ~6 ~0 ~8 minecraft:stone_bricks
setblock ~6 ~0 ~9 minecraft:dark_oak_planks
fill ~6 ~0 ~10 ~6 ~0 ~11 minecraft:spruce_planks
setblock ~6 ~0 ~12 minecraft:packed_mud
fill ~6 ~0 ~13 ~8 ~0 ~13 minecraft:gravel
fill ~6 ~0 ~14 ~6 ~0 ~15 minecraft:snow_block
setblock ~6 ~0 ~16 minecraft:packed_mud
setblock ~6 ~0 ~18 minecraft:andesite
setblock ~6 ~0 ~19 minecraft:coarse_dirt
fill ~6 ~0 ~20 ~6 ~0 ~23 minecraft:calcite
setblock ~6 ~0 ~24 minecraft:polished_andesite
fill ~6 ~0 ~25 ~6 ~0 ~28 minecraft:calcite
fill ~6 ~0 ~29 ~6 ~0 ~30 minecraft:gravel
setblock ~6 ~0 ~31 minecraft:cobblestone
setblock ~6 ~0 ~32 minecraft:packed_mud
fill ~6 ~0 ~33 ~7 ~0 ~33 minecraft:gravel
fill ~6 ~0 ~34 ~7 ~0 ~34 minecraft:snow_block
setblock ~6 ~0 ~35 minecraft:snow_block
setblock ~6 ~0 ~36 minecraft:andesite
fill ~7 ~0 ~2 ~7 ~0 ~3 minecraft:dark_oak_planks
fill ~7 ~0 ~4 ~8 ~0 ~4 minecraft:stone_bricks
fill ~7 ~0 ~8 ~7 ~0 ~9 minecraft:spruce_planks
setblock ~7 ~0 ~10 minecraft:dark_oak_planks
setblock ~7 ~0 ~11 minecraft:spruce_planks
setblock ~7 ~0 ~12 minecraft:cobblestone
setblock ~7 ~0 ~14 minecraft:packed_mud
fill ~7 ~0 ~15 ~7 ~0 ~16 minecraft:cobblestone
setblock ~7 ~0 ~17 minecraft:packed_mud
fill ~7 ~0 ~18 ~8 ~0 ~18 minecraft:calcite
setblock ~7 ~0 ~19 minecraft:calcite
setblock ~7 ~0 ~20 minecraft:stone_bricks
setblock ~7 ~0 ~21 minecraft:cracked_stone_bricks
fill ~7 ~0 ~22 ~7 ~0 ~23 minecraft:stone_bricks
setblock ~7 ~0 ~24 minecraft:andesite
fill ~7 ~0 ~25 ~7 ~0 ~26 minecraft:stone_bricks
setblock ~7 ~0 ~27 minecraft:cobblestone
setblock ~7 ~0 ~28 minecraft:polished_andesite
fill ~7 ~0 ~29 ~7 ~0 ~30 minecraft:calcite
fill ~7 ~0 ~31 ~7 ~0 ~32 minecraft:snow_block
setblock ~7 ~0 ~35 minecraft:cobblestone
fill ~7 ~0 ~36 ~8 ~0 ~36 minecraft:snow_block
fill ~8 ~0 ~0 ~10 ~0 ~0 minecraft:spruce_planks
fill ~8 ~0 ~1 ~9 ~0 ~2 minecraft:spruce_planks
setblock ~8 ~0 ~3 minecraft:dark_oak_planks
setblock ~8 ~0 ~5 minecraft:stone_bricks
setblock ~8 ~0 ~6 minecraft:dark_oak_planks
fill ~8 ~0 ~7 ~8 ~0 ~10 minecraft:spruce_planks
setblock ~8 ~0 ~11 minecraft:dark_oak_planks
setblock ~8 ~0 ~12 minecraft:packed_mud
setblock ~8 ~0 ~14 minecraft:coarse_dirt
fill ~8 ~0 ~15 ~9 ~0 ~15 minecraft:packed_mud
setblock ~8 ~0 ~16 minecraft:snow_block
fill ~8 ~0 ~17 ~9 ~0 ~17 minecraft:calcite
fill ~8 ~0 ~19 ~8 ~0 ~20 minecraft:stone_bricks
setblock ~8 ~0 ~21 minecraft:polished_andesite
setblock ~8 ~0 ~22 minecraft:cobblestone
setblock ~8 ~0 ~23 minecraft:cracked_stone_bricks
fill ~8 ~0 ~24 ~8 ~0 ~26 minecraft:stone_bricks
setblock ~8 ~0 ~27 minecraft:polished_andesite
setblock ~8 ~0 ~28 minecraft:stone_bricks
fill ~8 ~0 ~29 ~9 ~0 ~29 minecraft:andesite
fill ~8 ~0 ~30 ~8 ~0 ~31 minecraft:calcite
fill ~8 ~0 ~32 ~8 ~0 ~34 minecraft:packed_mud
fill ~8 ~0 ~35 ~9 ~0 ~35 minecraft:snow_block
fill ~9 ~0 ~3 ~11 ~0 ~3 minecraft:spruce_planks
setblock ~9 ~0 ~4 minecraft:spruce_planks
setblock ~9 ~0 ~5 minecraft:dark_oak_planks
fill ~9 ~0 ~6 ~10 ~0 ~6 minecraft:spruce_planks
fill ~9 ~0 ~7 ~10 ~0 ~7 minecraft:stone_bricks
setblock ~9 ~0 ~8 minecraft:dark_oak_planks
fill ~9 ~0 ~9 ~9 ~0 ~10 minecraft:spruce_planks
setblock ~9 ~0 ~11 minecraft:stone_bricks
fill ~9 ~0 ~12 ~10 ~0 ~12 minecraft:cobblestone
fill ~9 ~0 ~13 ~9 ~0 ~14 minecraft:cobblestone
fill ~9 ~0 ~16 ~10 ~0 ~16 minecraft:calcite
setblock ~9 ~0 ~18 minecraft:polished_andesite
fill ~9 ~0 ~19 ~10 ~0 ~19 minecraft:andesite
setblock ~9 ~0 ~20 minecraft:andesite
fill ~9 ~0 ~21 ~10 ~0 ~21 minecraft:snow_block
setblock ~9 ~0 ~22 minecraft:polished_andesite
setblock ~9 ~0 ~23 minecraft:andesite
setblock ~9 ~0 ~24 minecraft:polished_andesite
setblock ~9 ~0 ~25 minecraft:andesite
fill ~9 ~0 ~26 ~9 ~0 ~28 minecraft:stone_bricks
setblock ~9 ~0 ~30 minecraft:stone_bricks
fill ~9 ~0 ~31 ~9 ~0 ~32 minecraft:calcite
fill ~9 ~0 ~33 ~9 ~0 ~34 minecraft:coarse_dirt
setblock ~9 ~0 ~36 minecraft:packed_mud
fill ~10 ~0 ~1 ~11 ~0 ~1 minecraft:dark_oak_planks
setblock ~10 ~0 ~2 minecraft:dark_oak_planks
fill ~10 ~0 ~4 ~11 ~0 ~5 minecraft:dark_oak_planks
fill ~10 ~0 ~8 ~10 ~0 ~10 minecraft:spruce_planks
fill ~10 ~0 ~11 ~11 ~0 ~11 minecraft:dark_oak_planks
fill ~10 ~0 ~13 ~10 ~0 ~14 minecraft:coarse_dirt
setblock ~10 ~0 ~15 minecraft:snow_block
fill ~10 ~0 ~17 ~10 ~0 ~18 minecraft:stone_bricks
setblock ~10 ~0 ~20 minecraft:cracked_stone_bricks
fill ~10 ~0 ~22 ~10 ~0 ~23 minecraft:cracked_stone_bricks
setblock ~10 ~0 ~24 minecraft:stone_bricks
setblock ~10 ~0 ~25 minecraft:snow_block
setblock ~10 ~0 ~26 minecraft:polished_andesite
setblock ~10 ~0 ~27 minecraft:cobblestone
fill ~10 ~0 ~28 ~10 ~0 ~29 minecraft:stone_bricks
setblock ~10 ~0 ~30 minecraft:polished_andesite
setblock ~10 ~0 ~31 minecraft:stone_bricks
setblock ~10 ~0 ~32 minecraft:calcite
setblock ~10 ~0 ~33 minecraft:snow_block
setblock ~10 ~0 ~34 minecraft:gravel
setblock ~10 ~0 ~35 minecraft:coarse_dirt
setblock ~10 ~0 ~36 minecraft:andesite
fill ~11 ~0 ~0 ~13 ~0 ~0 minecraft:dark_oak_planks
setblock ~11 ~0 ~2 minecraft:stone_bricks
setblock ~11 ~0 ~6 minecraft:dark_oak_planks
fill ~11 ~0 ~7 ~12 ~0 ~7 minecraft:spruce_planks
fill ~11 ~0 ~8 ~12 ~0 ~8 minecraft:dark_oak_planks
fill ~11 ~0 ~9 ~11 ~0 ~10 minecraft:spruce_planks
fill ~11 ~0 ~12 ~11 ~0 ~14 minecraft:gravel
fill ~11 ~0 ~15 ~14 ~0 ~15 minecraft:calcite
setblock ~11 ~0 ~16 minecraft:stone_bricks
fill ~11 ~0 ~17 ~12 ~0 ~17 minecraft:andesite
setblock ~11 ~0 ~18 minecraft:snow_block
setblock ~11 ~0 ~19 minecraft:polished_andesite
setblock ~11 ~0 ~20 minecraft:andesite
setblock ~11 ~0 ~21 minecraft:stone_bricks
fill ~11 ~0 ~22 ~11 ~0 ~23 minecraft:cobblestone
setblock ~11 ~0 ~24 minecraft:polished_andesite
fill ~11 ~0 ~25 ~12 ~0 ~25 minecraft:stone_bricks
fill ~11 ~0 ~26 ~12 ~0 ~26 minecraft:cracked_stone_bricks
fill ~11 ~0 ~27 ~14 ~0 ~27 minecraft:stone_bricks
setblock ~11 ~0 ~28 minecraft:polished_andesite
fill ~11 ~0 ~29 ~11 ~0 ~31 minecraft:stone_bricks
setblock ~11 ~0 ~32 minecraft:cobblestone
fill ~11 ~0 ~33 ~14 ~0 ~33 minecraft:calcite
setblock ~11 ~0 ~34 minecraft:andesite
fill ~11 ~0 ~35 ~12 ~0 ~35 minecraft:stone_bricks
setblock ~11 ~0 ~36 minecraft:stone_bricks
setblock ~12 ~0 ~1 minecraft:stone_bricks
setblock ~12 ~0 ~2 minecraft:spruce_planks
fill ~12 ~0 ~3 ~12 ~0 ~6 minecraft:dark_oak_planks
setblock ~12 ~0 ~9 minecraft:stone_bricks
setblock ~12 ~0 ~10 minecraft:spruce_planks
setblock ~12 ~0 ~11 minecraft:stone_bricks
fill ~12 ~0 ~12 ~14 ~0 ~12 minecraft:snow_block
setblock ~12 ~0 ~13 minecraft:snow_block
setblock ~12 ~0 ~14 minecraft:packed_mud
setblock ~12 ~0 ~16 minecraft:snow_block
setblock ~12 ~0 ~18 minecraft:cobblestone
fill ~12 ~0 ~19 ~12 ~0 ~21 minecraft:andesite
fill ~12 ~0 ~22 ~12 ~0 ~23 minecraft:stone_bricks
setblock ~12 ~0 ~24 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~28 minecraft:andesite
setblock ~12 ~0 ~29 minecraft:cracked_stone_bricks
fill ~12 ~0 ~30 ~12 ~0 ~32 minecraft:stone_bricks
setblock ~12 ~0 ~34 minecraft:cobblestone
setblock ~12 ~0 ~36 minecraft:cobblestone
setblock ~13 ~0 ~1 minecraft:spruce_planks
fill ~13 ~0 ~2 ~13 ~0 ~3 minecraft:dark_oak_planks
setblock ~13 ~0 ~4 minecraft:stone_bricks
fill ~13 ~0 ~5 ~14 ~0 ~5 minecraft:spruce_planks
fill ~13 ~0 ~6 ~13 ~0 ~7 minecraft:dark_oak_planks
fill ~13 ~0 ~8 ~13 ~0 ~11 minecraft:spruce_planks
setblock ~13 ~0 ~13 minecraft:gravel
setblock ~13 ~0 ~14 minecraft:cobblestone
fill ~13 ~0 ~16 ~14 ~0 ~16 minecraft:stone_bricks
fill ~13 ~0 ~17 ~13 ~0 ~19 minecraft:stone_bricks
setblock ~13 ~0 ~20 minecraft:polished_andesite
setblock ~13 ~0 ~21 minecraft:stone_bricks
setblock ~13 ~0 ~22 minecraft:andesite
fill ~13 ~0 ~23 ~13 ~0 ~24 minecraft:stone_bricks
setblock ~13 ~0 ~25 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~26 minecraft:snow_block
fill ~13 ~0 ~28 ~13 ~0 ~29 minecraft:stone_bricks
fill ~13 ~0 ~30 ~14 ~0 ~30 minecraft:andesite
setblock ~13 ~0 ~31 minecraft:stone_bricks
setblock ~13 ~0 ~32 minecraft:snow_block
setblock ~13 ~0 ~34 minecraft:stone_bricks
setblock ~13 ~0 ~35 minecraft:cobblestone
fill ~13 ~0 ~36 ~14 ~0 ~36 minecraft:cracked_stone_bricks
fill ~14 ~0 ~0 ~15 ~0 ~1 minecraft:spruce_planks
setblock ~14 ~0 ~2 minecraft:spruce_planks
setblock ~14 ~0 ~3 minecraft:dark_oak_planks
fill ~14 ~0 ~4 ~15 ~0 ~4 minecraft:spruce_planks
fill ~14 ~0 ~6 ~15 ~0 ~6 minecraft:spruce_planks
setblock ~14 ~0 ~7 minecraft:dark_oak_planks
setblock ~14 ~0 ~8 minecraft:stone_bricks
setblock ~14 ~0 ~9 minecraft:spruce_planks
setblock ~14 ~0 ~10 minecraft:dark_oak_planks
setblock ~14 ~0 ~11 minecraft:stone_bricks
setblock ~14 ~0 ~13 minecraft:packed_mud
setblock ~14 ~0 ~14 minecraft:coarse_dirt
setblock ~14 ~0 ~17 minecraft:andesite
fill ~14 ~0 ~18 ~15 ~0 ~18 minecraft:stone_bricks
setblock ~14 ~0 ~19 minecraft:polished_andesite
setblock ~14 ~0 ~20 minecraft:andesite
fill ~14 ~0 ~21 ~14 ~0 ~22 minecraft:cracked_stone_bricks
fill ~14 ~0 ~23 ~16 ~0 ~25 minecraft:chiseled_stone_bricks
setblock ~14 ~0 ~26 minecraft:andesite
setblock ~14 ~0 ~28 minecraft:cracked_stone_bricks
setblock ~14 ~0 ~29 minecraft:stone_bricks
setblock ~14 ~0 ~31 minecraft:snow_block
setblock ~14 ~0 ~32 minecraft:stone_bricks
setblock ~14 ~0 ~34 minecraft:cobblestone
setblock ~14 ~0 ~35 minecraft:stone_bricks
fill ~15 ~0 ~2 ~15 ~0 ~3 minecraft:stone_bricks
setblock ~15 ~0 ~5 minecraft:stone_bricks
setblock ~15 ~0 ~7 minecraft:stone_bricks
fill ~15 ~0 ~8 ~15 ~0 ~9 minecraft:dark_oak_planks
fill ~15 ~0 ~10 ~15 ~0 ~11 minecraft:spruce_planks
setblock ~15 ~0 ~12 minecraft:cobblestone
setblock ~15 ~0 ~13 minecraft:snow_block
setblock ~15 ~0 ~14 minecraft:calcite
setblock ~15 ~0 ~15 minecraft:stone_bricks
fill ~15 ~0 ~16 ~15 ~0 ~17 minecraft:andesite
setblock ~15 ~0 ~19 minecraft:cobblestone
setblock ~15 ~0 ~20 minecraft:snow_block
setblock ~15 ~0 ~21 minecraft:cobblestone
setblock ~15 ~0 ~22 minecraft:cracked_stone_bricks
fill ~15 ~0 ~26 ~17 ~0 ~26 minecraft:stone_bricks
setblock ~15 ~0 ~27 minecraft:snow_block
setblock ~15 ~0 ~28 minecraft:stone_bricks
setblock ~15 ~0 ~29 minecraft:cracked_stone_bricks
fill ~15 ~0 ~30 ~16 ~0 ~30 minecraft:polished_andesite
fill ~15 ~0 ~31 ~16 ~0 ~31 minecraft:cracked_stone_bricks
fill ~15 ~0 ~32 ~16 ~0 ~32 minecraft:cobblestone
setblock ~15 ~0 ~33 minecraft:snow_block
setblock ~15 ~0 ~34 minecraft:calcite
setblock ~15 ~0 ~35 minecraft:andesite
setblock ~15 ~0 ~36 minecraft:stone_bricks
fill ~16 ~0 ~0 ~16 ~0 ~1 minecraft:stone_bricks
fill ~16 ~0 ~2 ~16 ~0 ~3 minecraft:spruce_planks
setblock ~16 ~0 ~4 minecraft:stone_bricks
setblock ~16 ~0 ~5 minecraft:spruce_planks
fill ~16 ~0 ~6 ~17 ~0 ~6 minecraft:stone_bricks
fill ~16 ~0 ~7 ~16 ~0 ~9 minecraft:spruce_planks
setblock ~16 ~0 ~10 minecraft:dark_oak_planks
setblock ~16 ~0 ~11 minecraft:spruce_planks
setblock ~16 ~0 ~12 minecraft:gravel
fill ~16 ~0 ~13 ~18 ~0 ~13 minecraft:cobblestone
setblock ~16 ~0 ~14 minecraft:cobblestone
fill ~16 ~0 ~15 ~19 ~0 ~15 minecraft:calcite
setblock ~16 ~0 ~16 minecraft:stone_bricks
setblock ~16 ~0 ~17 minecraft:polished_andesite
setblock ~16 ~0 ~18 minecraft:andesite
setblock ~16 ~0 ~19 minecraft:cracked_stone_bricks
setblock ~16 ~0 ~20 minecraft:stone_bricks
setblock ~16 ~0 ~21 minecraft:cracked_stone_bricks
setblock ~16 ~0 ~22 minecraft:stone_bricks
fill ~16 ~0 ~27 ~16 ~0 ~28 minecraft:stone_bricks
setblock ~16 ~0 ~29 minecraft:andesite
fill ~16 ~0 ~33 ~19 ~0 ~33 minecraft:calcite
fill ~16 ~0 ~34 ~18 ~0 ~34 minecraft:stone_bricks
fill ~16 ~0 ~35 ~17 ~0 ~36 minecraft:stone_bricks
fill ~17 ~0 ~0 ~19 ~0 ~0 minecraft:spruce_planks
setblock ~17 ~0 ~1 minecraft:stone_bricks
setblock ~17 ~0 ~2 minecraft:dark_oak_planks
fill ~17 ~0 ~3 ~17 ~0 ~4 minecraft:spruce_planks
setblock ~17 ~0 ~5 minecraft:dark_oak_planks
setblock ~17 ~0 ~7 minecraft:dark_oak_planks
fill ~17 ~0 ~8 ~17 ~0 ~11 minecraft:spruce_planks
fill ~17 ~0 ~12 ~18 ~0 ~12 minecraft:snow_block
fill ~17 ~0 ~14 ~18 ~0 ~14 minecraft:gravel
setblock ~17 ~0 ~16 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~17 minecraft:snow_block
setblock ~17 ~0 ~18 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~19 minecraft:stone_bricks
setblock ~17 ~0 ~20 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~21 minecraft:stone_bricks
setblock ~17 ~0 ~22 minecraft:snow_block
fill ~17 ~0 ~23 ~17 ~0 ~25 minecraft:andesite
setblock ~17 ~0 ~27 minecraft:cobblestone
setblock ~17 ~0 ~28 minecraft:stone_bricks
setblock ~17 ~0 ~29 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~30 minecraft:snow_block
fill ~17 ~0 ~31 ~19 ~0 ~31 minecraft:stone_bricks
setblock ~17 ~0 ~32 minecraft:stone_bricks
fill ~18 ~0 ~1 ~20 ~0 ~3 minecraft:spruce_planks
fill ~18 ~0 ~4 ~19 ~0 ~5 minecraft:spruce_planks
fill ~18 ~0 ~6 ~18 ~0 ~7 minecraft:spruce_planks
setblock ~18 ~0 ~8 minecraft:dark_oak_planks
fill ~18 ~0 ~9 ~18 ~0 ~11 minecraft:spruce_planks
fill ~18 ~0 ~16 ~19 ~0 ~16 minecraft:stone_bricks
setblock ~18 ~0 ~17 minecraft:cracked_stone_bricks
setblock ~18 ~0 ~18 minecraft:polished_andesite
setblock ~18 ~0 ~19 minecraft:andesite
fill ~18 ~0 ~20 ~18 ~0 ~22 minecraft:cobblestone
fill ~18 ~0 ~23 ~19 ~0 ~23 minecraft:stone_bricks
fill ~18 ~0 ~24 ~18 ~0 ~25 minecraft:stone_bricks
setblock ~18 ~0 ~26 minecraft:cracked_stone_bricks
setblock ~18 ~0 ~27 minecraft:polished_andesite
setblock ~18 ~0 ~28 minecraft:andesite
setblock ~18 ~0 ~29 minecraft:stone_bricks
setblock ~18 ~0 ~30 minecraft:polished_andesite
setblock ~18 ~0 ~32 minecraft:cobblestone
setblock ~18 ~0 ~35 minecraft:andesite
fill ~18 ~0 ~36 ~19 ~0 ~36 minecraft:stone_bricks
setblock ~19 ~0 ~6 minecraft:dark_oak_planks
fill ~19 ~0 ~7 ~19 ~0 ~9 minecraft:spruce_planks
fill ~19 ~0 ~10 ~19 ~0 ~11 minecraft:dark_oak_planks
fill ~19 ~0 ~12 ~20 ~0 ~12 minecraft:gravel
setblock ~19 ~0 ~13 minecraft:packed_mud
fill ~19 ~0 ~14 ~20 ~0 ~14 minecraft:snow_block
fill ~19 ~0 ~17 ~20 ~0 ~17 minecraft:stone_bricks
fill ~19 ~0 ~18 ~19 ~0 ~20 minecraft:stone_bricks
fill ~19 ~0 ~21 ~20 ~0 ~21 minecraft:polished_andesite
fill ~19 ~0 ~22 ~20 ~0 ~22 minecraft:andesite
fill ~19 ~0 ~24 ~19 ~0 ~25 minecraft:andesite
fill ~19 ~0 ~26 ~19 ~0 ~27 minecraft:stone_bricks
setblock ~19 ~0 ~28 minecraft:cracked_stone_bricks
setblock ~19 ~0 ~29 minecraft:cobblestone
fill ~19 ~0 ~30 ~20 ~0 ~30 minecraft:andesite
setblock ~19 ~0 ~32 minecraft:cracked_stone_bricks
fill ~19 ~0 ~34 ~19 ~0 ~35 minecraft:andesite
setblock ~20 ~0 ~0 minecraft:stone_bricks
fill ~20 ~0 ~4 ~21 ~0 ~4 minecraft:stone_bricks
fill ~20 ~0 ~5 ~20 ~0 ~7 minecraft:dark_oak_planks
fill ~20 ~0 ~8 ~21 ~0 ~8 minecraft:spruce_planks
fill ~20 ~0 ~9 ~20 ~0 ~10 minecraft:spruce_planks
setblock ~20 ~0 ~11 minecraft:stone_bricks
setblock ~20 ~0 ~13 minecraft:coarse_dirt
setblock ~20 ~0 ~15 minecraft:snow_block
fill ~20 ~0 ~16 ~21 ~0 ~16 minecraft:calcite
setblock ~20 ~0 ~18 minecraft:snow_block
setblock ~20 ~0 ~19 minecraft:polished_andesite
setblock ~20 ~0 ~20 minecraft:stone_bricks
fill ~20 ~0 ~23 ~21 ~0 ~23 minecraft:andesite
fill ~20 ~0 ~24 ~21 ~0 ~24 minecraft:stone_bricks
fill ~20 ~0 ~25 ~20 ~0 ~29 minecraft:stone_bricks
setblock ~20 ~0 ~31 minecraft:cracked_stone_bricks
setblock ~20 ~0 ~32 minecraft:calcite
setblock ~20 ~0 ~33 minecraft:snow_block
fill ~20 ~0 ~34 ~20 ~0 ~35 minecraft:coarse_dirt
setblock ~20 ~0 ~36 minecraft:gravel
setblock ~21 ~0 ~0 minecraft:dark_oak_planks
setblock ~21 ~0 ~1 minecraft:stone_bricks
setblock ~21 ~0 ~2 minecraft:dark_oak_planks
setblock ~21 ~0 ~3 minecraft:spruce_planks
setblock ~21 ~0 ~5 minecraft:spruce_planks
setblock ~21 ~0 ~6 minecraft:dark_oak_planks
setblock ~21 ~0 ~7 minecraft:stone_bricks
setblock ~21 ~0 ~9 minecraft:dark_oak_planks
fill ~21 ~0 ~10 ~21 ~0 ~11 minecraft:spruce_planks
setblock ~21 ~0 ~12 minecraft:snow_block
setblock ~21 ~0 ~13 minecraft:cobblestone
fill ~21 ~0 ~14 ~21 ~0 ~15 minecraft:gravel
fill ~21 ~0 ~17 ~22 ~0 ~17 minecraft:calcite
fill ~21 ~0 ~18 ~21 ~0 ~19 minecraft:stone_bricks
setblock ~21 ~0 ~20 minecraft:snow_block
fill ~21 ~0 ~21 ~22 ~0 ~21 minecraft:stone_bricks
setblock ~21 ~0 ~22 minecraft:stone_bricks
fill ~21 ~0 ~25 ~21 ~0 ~26 minecraft:andesite
fill ~21 ~0 ~27 ~21 ~0 ~28 minecraft:stone_bricks
setblock ~21 ~0 ~29 minecraft:cobblestone
setblock ~21 ~0 ~30 minecraft:stone_bricks
fill ~21 ~0 ~31 ~21 ~0 ~32 minecraft:calcite
fill ~21 ~0 ~33 ~21 ~0 ~34 minecraft:cobblestone
fill ~21 ~0 ~35 ~22 ~0 ~35 minecraft:gravel
setblock ~21 ~0 ~36 minecraft:packed_mud
fill ~22 ~0 ~0 ~23 ~0 ~0 minecraft:spruce_planks
setblock ~22 ~0 ~1 minecraft:dark_oak_planks
setblock ~22 ~0 ~2 minecraft:spruce_planks
setblock ~22 ~0 ~3 minecraft:stone_bricks
fill ~22 ~0 ~4 ~23 ~0 ~4 minecraft:spruce_planks
fill ~22 ~0 ~5 ~22 ~0 ~6 minecraft:stone_bricks
fill ~22 ~0 ~7 ~22 ~0 ~11 minecraft:spruce_planks
setblock ~22 ~0 ~12 minecraft:gravel
setblock ~22 ~0 ~13 minecraft:packed_mud
fill ~22 ~0 ~14 ~23 ~0 ~14 minecraft:cobblestone
setblock ~22 ~0 ~15 minecraft:andesite
fill ~22 ~0 ~16 ~23 ~0 ~16 minecraft:snow_block
fill ~22 ~0 ~18 ~23 ~0 ~18 minecraft:calcite
setblock ~22 ~0 ~19 minecraft:polished_andesite
fill ~22 ~0 ~20 ~23 ~0 ~20 minecraft:stone_bricks
setblock ~22 ~0 ~22 minecraft:cracked_stone_bricks
fill ~22 ~0 ~23 ~23 ~0 ~23 minecraft:stone_bricks
setblock ~22 ~0 ~24 minecraft:cobblestone
fill ~22 ~0 ~25 ~23 ~0 ~25 minecraft:stone_bricks
setblock ~22 ~0 ~26 minecraft:stone_bricks
setblock ~22 ~0 ~27 minecraft:andesite
fill ~22 ~0 ~28 ~22 ~0 ~29 minecraft:stone_bricks
fill ~22 ~0 ~30 ~22 ~0 ~31 minecraft:calcite
fill ~22 ~0 ~32 ~23 ~0 ~32 minecraft:snow_block
fill ~22 ~0 ~33 ~23 ~0 ~33 minecraft:packed_mud
setblock ~22 ~0 ~34 minecraft:packed_mud
setblock ~22 ~0 ~36 minecraft:cobblestone
fill ~23 ~0 ~1 ~23 ~0 ~2 minecraft:spruce_planks
setblock ~23 ~0 ~3 minecraft:dark_oak_planks
fill ~23 ~0 ~5 ~24 ~0 ~5 minecraft:spruce_planks
setblock ~23 ~0 ~6 minecraft:dark_oak_planks
fill ~23 ~0 ~7 ~23 ~0 ~8 minecraft:stone_bricks
fill ~23 ~0 ~9 ~23 ~0 ~10 minecraft:spruce_planks
setblock ~23 ~0 ~11 minecraft:dark_oak_planks
setblock ~23 ~0 ~12 minecraft:snow_block
fill ~23 ~0 ~13 ~25 ~0 ~13 minecraft:cobblestone
setblock ~23 ~0 ~15 minecraft:cobblestone
setblock ~23 ~0 ~17 minecraft:snow_block
setblock ~23 ~0 ~19 minecraft:calcite
setblock ~23 ~0 ~21 minecraft:polished_andesite
setblock ~23 ~0 ~22 minecraft:cobblestone
setblock ~23 ~0 ~24 minecraft:andesite
setblock ~23 ~0 ~26 minecraft:cracked_stone_bricks
fill ~23 ~0 ~27 ~23 ~0 ~28 minecraft:stone_bricks
fill ~23 ~0 ~29 ~23 ~0 ~30 minecraft:calcite
setblock ~23 ~0 ~31 minecraft:gravel
fill ~23 ~0 ~34 ~23 ~0 ~36 minecraft:cobblestone
setblock ~24 ~0 ~0 minecraft:dark_oak_planks
setblock ~24 ~0 ~1 minecraft:stone_bricks
fill ~24 ~0 ~2 ~24 ~0 ~3 minecraft:spruce_planks
setblock ~24 ~0 ~4 minecraft:dark_oak_planks
fill ~24 ~0 ~6 ~25 ~0 ~6 minecraft:stone_bricks
fill ~24 ~0 ~7 ~25 ~0 ~7 minecraft:spruce_planks
setblock ~24 ~0 ~8 minecraft:spruce_planks
fill ~24 ~0 ~9 ~26 ~0 ~9 minecraft:dark_oak_planks
setblock ~24 ~0 ~10 minecraft:stone_bricks
setblock ~24 ~0 ~11 minecraft:spruce_planks
setblock ~24 ~0 ~12 minecraft:coarse_dirt
fill ~24 ~0 ~14 ~25 ~0 ~14 minecraft:gravel
setblock ~24 ~0 ~15 minecraft:andesite
setblock ~24 ~0 ~16 minecraft:cobblestone
fill ~24 ~0 ~17 ~25 ~0 ~17 minecraft:gravel
fill ~24 ~0 ~18 ~25 ~0 ~18 minecraft:cobblestone
setblock ~24 ~0 ~19 minecraft:gravel
fill ~24 ~0 ~20 ~24 ~0 ~23 minecraft:calcite
setblock ~24 ~0 ~24 minecraft:stone_bricks
fill ~24 ~0 ~25 ~24 ~0 ~28 minecraft:calcite
setblock ~24 ~0 ~29 minecraft:cobblestone
setblock ~24 ~0 ~30 minecraft:snow_block
setblock ~24 ~0 ~31 minecraft:cobblestone
setblock ~24 ~0 ~32 minecraft:packed_mud
fill ~24 ~0 ~33 ~24 ~0 ~34 minecraft:snow_block
setblock ~24 ~0 ~35 minecraft:gravel
setblock ~24 ~0 ~36 minecraft:packed_mud
fill ~25 ~0 ~0 ~26 ~0 ~1 minecraft:spruce_planks
fill ~25 ~0 ~2 ~25 ~0 ~3 minecraft:dark_oak_planks
fill ~25 ~0 ~4 ~25 ~0 ~5 minecraft:spruce_planks
fill ~25 ~0 ~8 ~26 ~0 ~8 minecraft:stone_bricks
fill ~25 ~0 ~10 ~26 ~0 ~10 minecraft:dark_oak_planks
setblock ~25 ~0 ~11 minecraft:dark_oak_planks
setblock ~25 ~0 ~12 minecraft:snow_block
setblock ~25 ~0 ~15 minecraft:packed_mud
setblock ~25 ~0 ~16 minecraft:snow_block
setblock ~25 ~0 ~19 minecraft:coarse_dirt
setblock ~25 ~0 ~20 minecraft:snow_block
setblock ~25 ~0 ~21 minecraft:andesite
setblock ~25 ~0 ~22 minecraft:gravel
setblock ~25 ~0 ~23 minecraft:packed_mud
setblock ~25 ~0 ~24 minecraft:calcite
setblock ~25 ~0 ~25 minecraft:cobblestone
fill ~25 ~0 ~26 ~26 ~0 ~26 minecraft:gravel
setblock ~25 ~0 ~27 minecraft:packed_mud
setblock ~25 ~0 ~28 minecraft:cobblestone
setblock ~25 ~0 ~29 minecraft:snow_block
setblock ~25 ~0 ~30 minecraft:cobblestone
setblock ~25 ~0 ~31 minecraft:packed_mud
setblock ~25 ~0 ~32 minecraft:cobblestone
setblock ~25 ~0 ~33 minecraft:packed_mud
setblock ~25 ~0 ~34 minecraft:snow_block
fill ~25 ~0 ~35 ~26 ~0 ~35 minecraft:cobblestone
setblock ~25 ~0 ~36 minecraft:cobblestone
fill ~26 ~0 ~2 ~26 ~0 ~3 minecraft:spruce_planks
fill ~26 ~0 ~4 ~26 ~0 ~5 minecraft:dark_oak_planks
fill ~26 ~0 ~6 ~26 ~0 ~7 minecraft:spruce_planks
setblock ~26 ~0 ~11 minecraft:spruce_planks
setblock ~26 ~0 ~12 minecraft:gravel
fill ~26 ~0 ~13 ~26 ~0 ~14 minecraft:packed_mud
setblock ~26 ~0 ~15 minecraft:cobblestone
setblock ~26 ~0 ~16 minecraft:packed_mud
setblock ~26 ~0 ~17 minecraft:cobblestone
setblock ~26 ~0 ~18 minecraft:packed_mud
fill ~26 ~0 ~19 ~26 ~0 ~20 minecraft:snow_block
setblock ~26 ~0 ~21 minecraft:gravel
setblock ~26 ~0 ~22 minecraft:snow_block
fill ~26 ~0 ~23 ~26 ~0 ~24 minecraft:cobblestone
setblock ~26 ~0 ~25 minecraft:andesite
setblock ~26 ~0 ~27 minecraft:cobblestone
fill ~26 ~0 ~28 ~28 ~0 ~28 minecraft:snow_block
setblock ~26 ~0 ~29 minecraft:packed_mud
fill ~26 ~0 ~30 ~27 ~0 ~30 minecraft:snow_block
fill ~26 ~0 ~31 ~26 ~0 ~32 minecraft:gravel
fill ~26 ~0 ~33 ~27 ~0 ~33 minecraft:cobblestone
fill ~26 ~0 ~34 ~27 ~0 ~34 minecraft:coarse_dirt
setblock ~26 ~0 ~36 minecraft:gravel
fill ~27 ~0 ~0 ~27 ~0 ~1 minecraft:dark_oak_planks
setblock ~27 ~0 ~2 minecraft:stone_bricks
fill ~27 ~0 ~3 ~27 ~0 ~4 minecraft:spruce_planks
fill ~27 ~0 ~5 ~28 ~0 ~5 minecraft:dark_oak_planks
fill ~27 ~0 ~6 ~27 ~0 ~7 minecraft:stone_bricks
fill ~27 ~0 ~8 ~28 ~0 ~8 minecraft:dark_oak_planks
fill ~27 ~0 ~9 ~28 ~0 ~9 minecraft:spruce_planks
setblock ~27 ~0 ~10 minecraft:spruce_planks
setblock ~27 ~0 ~11 minecraft:dark_oak_planks
fill ~27 ~0 ~12 ~30 ~0 ~12 minecraft:snow_block
setblock ~27 ~0 ~13 minecraft:coarse_dirt
setblock ~27 ~0 ~14 minecraft:cobblestone
setblock ~27 ~0 ~15 minecraft:snow_block
fill ~27 ~0 ~16 ~27 ~0 ~18 minecraft:cobblestone
fill ~27 ~0 ~19 ~28 ~0 ~19 minecraft:gravel
fill ~27 ~0 ~20 ~27 ~0 ~21 minecraft:snow_block
fill ~27 ~0 ~22 ~28 ~0 ~22 minecraft:cobblestone
setblock ~27 ~0 ~23 minecraft:snow_block
fill ~27 ~0 ~24 ~27 ~0 ~26 minecraft:cobblestone
setblock ~27 ~0 ~27 minecraft:gravel
fill ~27 ~0 ~29 ~28 ~0 ~29 minecraft:cobblestone
setblock ~27 ~0 ~31 minecraft:cobblestone
setblock ~27 ~0 ~32 minecraft:gravel
setblock ~27 ~0 ~35 minecraft:andesite
setblock ~27 ~0 ~36 minecraft:cobblestone
fill ~28 ~0 ~0 ~29 ~0 ~0 minecraft:spruce_planks
setblock ~28 ~0 ~1 minecraft:dark_oak_planks
fill ~28 ~0 ~2 ~29 ~0 ~2 minecraft:spruce_planks
fill ~28 ~0 ~3 ~30 ~0 ~3 minecraft:dark_oak_planks
setblock ~28 ~0 ~4 minecraft:stone_bricks
fill ~28 ~0 ~6 ~29 ~0 ~6 minecraft:dark_oak_planks
setblock ~28 ~0 ~7 minecraft:spruce_planks
setblock ~28 ~0 ~10 minecraft:dark_oak_planks
setblock ~28 ~0 ~11 minecraft:spruce_planks
setblock ~28 ~0 ~13 minecraft:cobblestone
setblock ~28 ~0 ~14 minecraft:coarse_dirt
setblock ~28 ~0 ~15 minecraft:andesite
setblock ~28 ~0 ~16 minecraft:coarse_dirt
setblock ~28 ~0 ~17 minecraft:cobblestone
setblock ~28 ~0 ~18 minecraft:coarse_dirt
setblock ~28 ~0 ~20 minecraft:cobblestone
setblock ~28 ~0 ~21 minecraft:gravel
fill ~28 ~0 ~23 ~28 ~0 ~25 minecraft:coarse_dirt
setblock ~28 ~0 ~26 minecraft:packed_mud
fill ~28 ~0 ~27 ~29 ~0 ~27 minecraft:snow_block
setblock ~28 ~0 ~30 minecraft:cobblestone
setblock ~28 ~0 ~31 minecraft:snow_block
setblock ~28 ~0 ~32 minecraft:packed_mud
setblock ~28 ~0 ~33 minecraft:andesite
fill ~28 ~0 ~34 ~28 ~0 ~35 minecraft:cobblestone
setblock ~28 ~0 ~36 minecraft:coarse_dirt
setblock ~29 ~0 ~1 minecraft:stone_bricks
setblock ~29 ~0 ~4 minecraft:dark_oak_planks
setblock ~29 ~0 ~5 minecraft:spruce_planks
setblock ~29 ~0 ~7 minecraft:dark_oak_planks
fill ~29 ~0 ~8 ~29 ~0 ~11 minecraft:spruce_planks
fill ~29 ~0 ~13 ~29 ~0 ~14 minecraft:packed_mud
setblock ~29 ~0 ~15 minecraft:cobblestone
setblock ~29 ~0 ~16 minecraft:packed_mud
setblock ~29 ~0 ~17 minecraft:andesite
setblock ~29 ~0 ~18 minecraft:snow_block
setblock ~29 ~0 ~19 minecraft:cobblestone
setblock ~29 ~0 ~20 minecraft:gravel
setblock ~29 ~0 ~21 minecraft:cobblestone
setblock ~29 ~0 ~22 minecraft:snow_block
setblock ~29 ~0 ~23 minecraft:andesite
fill ~29 ~0 ~24 ~30 ~0 ~24 minecraft:cobblestone
setblock ~29 ~0 ~25 minecraft:packed_mud
fill ~29 ~0 ~26 ~30 ~0 ~26 minecraft:gravel
fill ~29 ~0 ~28 ~29 ~0 ~29 minecraft:cobblestone
setblock ~29 ~0 ~30 minecraft:gravel
fill ~29 ~0 ~31 ~30 ~0 ~31 minecraft:coarse_dirt
setblock ~29 ~0 ~32 minecraft:cobblestone
setblock ~29 ~0 ~33 minecraft:gravel
setblock ~29 ~0 ~34 minecraft:snow_block
fill ~29 ~0 ~35 ~29 ~0 ~36 minecraft:cobblestone
setblock ~30 ~0 ~0 minecraft:dark_oak_planks
fill ~30 ~0 ~1 ~30 ~0 ~2 minecraft:spruce_planks
setblock ~30 ~0 ~4 minecraft:stone_bricks
fill ~30 ~0 ~5 ~30 ~0 ~6 minecraft:dark_oak_planks
fill ~30 ~0 ~7 ~30 ~0 ~8 minecraft:spruce_planks
setblock ~30 ~0 ~9 minecraft:stone_bricks
setblock ~30 ~0 ~10 minecraft:dark_oak_planks
setblock ~30 ~0 ~11 minecraft:spruce_planks
setblock ~30 ~0 ~13 minecraft:cobblestone
setblock ~30 ~0 ~14 minecraft:coarse_dirt
setblock ~30 ~0 ~15 minecraft:packed_mud
setblock ~30 ~0 ~16 minecraft:cobblestone
fill ~30 ~0 ~17 ~30 ~0 ~18 minecraft:gravel
setblock ~30 ~0 ~19 minecraft:snow_block
setblock ~30 ~0 ~20 minecraft:coarse_dirt
fill ~30 ~0 ~21 ~30 ~0 ~22 minecraft:snow_block
setblock ~30 ~0 ~23 minecraft:gravel
setblock ~30 ~0 ~25 minecraft:snow_block
fill ~30 ~0 ~27 ~30 ~0 ~30 minecraft:cobblestone
setblock ~30 ~0 ~32 minecraft:snow_block
setblock ~30 ~0 ~33 minecraft:coarse_dirt
fill ~30 ~0 ~34 ~30 ~0 ~35 minecraft:packed_mud
setblock ~30 ~0 ~36 minecraft:snow_block

# the storehouse walls (y 1..3): a footing of stone brick at the foot (y 1), spruce boards with dark oak singles between the posts (y 2) and a plate of dark oak logs (y 3)
fill ~0 ~1 ~1 ~0 ~1 ~2 minecraft:stone_bricks
setblock ~0 ~1 ~3 minecraft:cracked_stone_bricks
setblock ~0 ~1 ~4 minecraft:stone_bricks
fill ~0 ~1 ~6 ~0 ~1 ~10 minecraft:stone_bricks
fill ~1 ~1 ~0 ~3 ~1 ~0 minecraft:stone_bricks
fill ~1 ~1 ~11 ~2 ~1 ~11 minecraft:stone_bricks
setblock ~3 ~1 ~11 minecraft:cracked_stone_bricks
setblock ~4 ~1 ~0 minecraft:cracked_stone_bricks
setblock ~4 ~1 ~11 minecraft:stone_bricks
fill ~6 ~1 ~0 ~8 ~1 ~0 minecraft:stone_bricks
fill ~6 ~1 ~11 ~7 ~1 ~11 minecraft:stone_bricks
setblock ~8 ~1 ~11 minecraft:deepslate_bricks
setblock ~9 ~1 ~0 minecraft:andesite
setblock ~9 ~1 ~11 minecraft:stone_bricks
fill ~11 ~1 ~0 ~14 ~1 ~0 minecraft:stone_bricks
setblock ~11 ~1 ~11 minecraft:andesite
setblock ~12 ~1 ~11 minecraft:stone_bricks
fill ~16 ~1 ~0 ~19 ~1 ~0 minecraft:stone_bricks
fill ~18 ~1 ~11 ~20 ~1 ~11 minecraft:stone_bricks
fill ~21 ~1 ~0 ~22 ~1 ~0 minecraft:cracked_stone_bricks
fill ~22 ~1 ~11 ~24 ~1 ~11 minecraft:stone_bricks
fill ~23 ~1 ~0 ~24 ~1 ~0 minecraft:stone_bricks
setblock ~25 ~1 ~11 minecraft:cracked_stone_bricks
setblock ~26 ~1 ~0 minecraft:stone_bricks
fill ~26 ~1 ~11 ~28 ~1 ~11 minecraft:stone_bricks
setblock ~27 ~1 ~0 minecraft:cracked_stone_bricks
fill ~28 ~1 ~0 ~29 ~1 ~0 minecraft:stone_bricks
fill ~30 ~1 ~1 ~30 ~1 ~2 minecraft:stone_bricks
setblock ~30 ~1 ~3 minecraft:cracked_stone_bricks
setblock ~30 ~1 ~4 minecraft:stone_bricks
fill ~30 ~1 ~6 ~30 ~1 ~10 minecraft:stone_bricks
fill ~0 ~2 ~1 ~0 ~2 ~2 minecraft:spruce_planks
setblock ~0 ~2 ~4 minecraft:spruce_planks
setblock ~0 ~2 ~6 minecraft:dark_oak_planks
setblock ~0 ~2 ~7 minecraft:spruce_planks
fill ~0 ~2 ~9 ~0 ~2 ~10 minecraft:spruce_planks
fill ~1 ~2 ~0 ~3 ~2 ~0 minecraft:spruce_planks
fill ~1 ~2 ~11 ~2 ~2 ~11 minecraft:spruce_planks
setblock ~4 ~2 ~11 minecraft:spruce_planks
setblock ~6 ~2 ~0 minecraft:dark_oak_planks
setblock ~6 ~2 ~11 minecraft:spruce_planks
setblock ~7 ~2 ~0 minecraft:spruce_planks
setblock ~8 ~2 ~0 minecraft:dark_oak_planks
fill ~8 ~2 ~11 ~9 ~2 ~11 minecraft:spruce_planks
setblock ~9 ~2 ~0 minecraft:spruce_planks
fill ~11 ~2 ~0 ~14 ~2 ~0 minecraft:spruce_planks
fill ~11 ~2 ~11 ~12 ~2 ~11 minecraft:spruce_planks
fill ~16 ~2 ~0 ~19 ~2 ~0 minecraft:spruce_planks
setblock ~18 ~2 ~11 minecraft:spruce_planks
setblock ~20 ~2 ~11 minecraft:spruce_planks
setblock ~21 ~2 ~0 minecraft:dark_oak_planks
fill ~22 ~2 ~0 ~24 ~2 ~0 minecraft:spruce_planks
fill ~22 ~2 ~11 ~24 ~2 ~11 minecraft:spruce_planks
fill ~26 ~2 ~11 ~28 ~2 ~11 minecraft:spruce_planks
fill ~27 ~2 ~0 ~29 ~2 ~0 minecraft:spruce_planks
fill ~30 ~2 ~1 ~30 ~2 ~2 minecraft:spruce_planks
setblock ~30 ~2 ~4 minecraft:spruce_planks
fill ~30 ~2 ~6 ~30 ~2 ~7 minecraft:spruce_planks
setblock ~30 ~2 ~9 minecraft:dark_oak_planks
setblock ~30 ~2 ~10 minecraft:spruce_planks
fill ~0 ~3 ~1 ~0 ~3 ~4 minecraft:dark_oak_log[axis=z]
fill ~0 ~3 ~6 ~0 ~3 ~10 minecraft:dark_oak_log[axis=z]
fill ~1 ~3 ~0 ~4 ~3 ~0 minecraft:dark_oak_log[axis=x]
fill ~1 ~3 ~11 ~4 ~3 ~11 minecraft:dark_oak_log[axis=x]
fill ~6 ~3 ~0 ~9 ~3 ~0 minecraft:dark_oak_log[axis=x]
fill ~6 ~3 ~11 ~9 ~3 ~11 minecraft:dark_oak_log[axis=x]
fill ~11 ~3 ~0 ~14 ~3 ~0 minecraft:dark_oak_log[axis=x]
fill ~11 ~3 ~11 ~12 ~3 ~11 minecraft:dark_oak_log[axis=x]
fill ~16 ~3 ~0 ~19 ~3 ~0 minecraft:dark_oak_log[axis=x]
fill ~18 ~3 ~11 ~20 ~3 ~11 minecraft:dark_oak_log[axis=x]
fill ~21 ~3 ~0 ~24 ~3 ~0 minecraft:dark_oak_log[axis=x]
fill ~22 ~3 ~11 ~28 ~3 ~11 minecraft:dark_oak_log[axis=x]
fill ~26 ~3 ~0 ~29 ~3 ~0 minecraft:dark_oak_log[axis=x]
fill ~30 ~3 ~1 ~30 ~3 ~4 minecraft:dark_oak_log[axis=z]
fill ~30 ~3 ~6 ~30 ~3 ~10 minecraft:dark_oak_log[axis=z]

# posts of dark oak logs at the corners, the door jambs and between the shutters
fill ~0 ~1 ~0 ~0 ~3 ~0 minecraft:dark_oak_log[axis=y]
fill ~0 ~1 ~5 ~0 ~3 ~5 minecraft:dark_oak_log[axis=y]
fill ~0 ~1 ~11 ~0 ~3 ~11 minecraft:dark_oak_log[axis=y]
fill ~5 ~1 ~0 ~5 ~3 ~0 minecraft:dark_oak_log[axis=y]
fill ~5 ~1 ~11 ~5 ~3 ~11 minecraft:dark_oak_log[axis=y]
setblock ~10 ~1 ~0 minecraft:dark_oak_log[axis=y]
fill ~10 ~1 ~11 ~10 ~3 ~11 minecraft:dark_oak_log[axis=y]
fill ~13 ~1 ~11 ~13 ~3 ~11 minecraft:dark_oak_log[axis=y]
fill ~15 ~1 ~0 ~15 ~3 ~0 minecraft:dark_oak_log[axis=y]
fill ~17 ~1 ~11 ~17 ~3 ~11 minecraft:dark_oak_log[axis=y]
setblock ~20 ~1 ~0 minecraft:dark_oak_log[axis=y]
fill ~21 ~1 ~11 ~21 ~3 ~11 minecraft:dark_oak_log[axis=y]
fill ~25 ~1 ~0 ~25 ~3 ~0 minecraft:dark_oak_log[axis=y]
fill ~29 ~1 ~11 ~30 ~3 ~11 minecraft:dark_oak_log[axis=y]
fill ~30 ~1 ~0 ~30 ~3 ~0 minecraft:dark_oak_log[axis=y]
fill ~30 ~1 ~5 ~30 ~3 ~5 minecraft:dark_oak_log[axis=y]
setblock ~10 ~3 ~0 minecraft:dark_oak_log[axis=y]
setblock ~20 ~3 ~0 minecraft:dark_oak_log[axis=y]

# the door: an opening three wide and two high in the front wall at x 14..16 (jambs at x 13 and 17), a beam over it at y 3
fill ~14 ~1 ~11 ~16 ~2 ~11 minecraft:air
fill ~14 ~3 ~11 ~16 ~3 ~11 minecraft:dark_oak_log[axis=x]

# windows: open spruce shutters in the boards
setblock ~0 ~2 ~3 minecraft:spruce_trapdoor[facing=west,half=bottom,open=true,powered=false,waterlogged=false]
setblock ~0 ~2 ~8 minecraft:spruce_trapdoor[facing=west,half=bottom,open=true,powered=false,waterlogged=false]
setblock ~3 ~2 ~11 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true,powered=false,waterlogged=false]
setblock ~4 ~2 ~0 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true,powered=false,waterlogged=false]
setblock ~7 ~2 ~11 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true,powered=false,waterlogged=false]
setblock ~10 ~2 ~0 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true,powered=false,waterlogged=false]
setblock ~19 ~2 ~11 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true,powered=false,waterlogged=false]
setblock ~20 ~2 ~0 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true,powered=false,waterlogged=false]
setblock ~25 ~2 ~11 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true,powered=false,waterlogged=false]
setblock ~26 ~2 ~0 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true,powered=false,waterlogged=false]
setblock ~30 ~2 ~3 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true,powered=false,waterlogged=false]
setblock ~30 ~2 ~8 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true,powered=false,waterlogged=false]

# the roof: dark spruce stairs at the eave and along the verges, snow blocks over the slopes in stripes and drifts, snow on the ridge
fill ~0 ~4 ~0 ~30 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~0 ~4 ~1 minecraft:spruce_slab[type=top]
setblock ~0 ~4 ~10 minecraft:spruce_slab[type=top]
fill ~0 ~4 ~11 ~21 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~1 ~4 ~1 ~29 ~4 ~1 minecraft:snow_block
fill ~1 ~4 ~10 ~21 ~4 ~10 minecraft:snow_block
setblock ~29 ~4 ~10 minecraft:snow_block
fill ~29 ~4 ~11 ~30 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~30 ~4 ~1 minecraft:spruce_slab[type=top]
setblock ~30 ~4 ~10 minecraft:spruce_slab[type=top]
setblock ~0 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~0 ~5 ~3 minecraft:spruce_slab[type=top]
setblock ~0 ~5 ~8 minecraft:spruce_slab[type=top]
setblock ~0 ~5 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~1 ~5 ~2 ~20 ~5 ~3 minecraft:snow_block
fill ~1 ~5 ~8 ~21 ~5 ~8 minecraft:snow_block
fill ~1 ~5 ~9 ~11 ~5 ~9 minecraft:snow_block
fill ~12 ~5 ~9 ~21 ~5 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~21 ~5 ~2 ~23 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~21 ~5 ~3 ~23 ~5 ~3 minecraft:snow_block
fill ~24 ~5 ~2 ~24 ~5 ~3 minecraft:snow_block
fill ~25 ~5 ~2 ~30 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~25 ~5 ~3 ~29 ~5 ~3 minecraft:snow_block
fill ~29 ~5 ~8 ~29 ~5 ~9 minecraft:snow_block
setblock ~30 ~5 ~3 minecraft:spruce_slab[type=top]
setblock ~30 ~5 ~8 minecraft:spruce_slab[type=top]
setblock ~30 ~5 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~0 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~0 ~6 ~5 ~0 ~6 ~6 minecraft:dark_oak_log[axis=z]
setblock ~0 ~6 ~7 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~1 ~6 ~4 ~13 ~6 ~5 minecraft:snow_block
fill ~1 ~6 ~6 ~4 ~6 ~7 minecraft:snow_block
setblock ~5 ~6 ~6 minecraft:spruce_planks
fill ~5 ~6 ~7 ~11 ~6 ~7 minecraft:snow_block
fill ~6 ~6 ~6 ~12 ~6 ~6 minecraft:snow_block
fill ~12 ~6 ~7 ~21 ~6 ~7 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~13 ~6 ~6 minecraft:spruce_planks
setblock ~14 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~14 ~6 ~5 ~14 ~6 ~6 minecraft:snow_block
fill ~15 ~6 ~4 ~29 ~6 ~5 minecraft:snow_block
setblock ~15 ~6 ~6 minecraft:snow_block
setblock ~16 ~6 ~6 minecraft:spruce_planks
fill ~17 ~6 ~6 ~18 ~6 ~6 minecraft:snow_block
setblock ~19 ~6 ~6 minecraft:spruce_planks
fill ~20 ~6 ~6 ~22 ~6 ~6 minecraft:snow_block
setblock ~23 ~6 ~6 minecraft:spruce_planks
fill ~24 ~6 ~6 ~29 ~6 ~6 minecraft:snow_block
setblock ~29 ~6 ~7 minecraft:snow_block
setblock ~30 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~30 ~6 ~5 ~30 ~6 ~6 minecraft:dark_oak_log[axis=z]
setblock ~30 ~6 ~7 minecraft:spruce_stairs[facing=north,half=bottom]

# the gable ends under the roof (x 0 and x 30)
fill ~0 ~4 ~2 ~0 ~4 ~7 minecraft:spruce_planks
fill ~0 ~4 ~8 ~0 ~4 ~9 minecraft:dark_oak_planks
fill ~30 ~4 ~2 ~30 ~4 ~9 minecraft:spruce_planks
fill ~0 ~5 ~4 ~0 ~5 ~7 minecraft:spruce_planks
fill ~30 ~5 ~4 ~30 ~5 ~7 minecraft:spruce_planks

# the lookout: the roof cut away over x 22..28, z 7..11, the wall plate of the front wall raised to y 4 and a deck of planks at y 5; beams under it
fill ~22 ~1 ~7 ~22 ~3 ~7 minecraft:dark_oak_log[axis=y]
fill ~22 ~1 ~10 ~22 ~3 ~10 minecraft:dark_oak_log[axis=y]
fill ~28 ~1 ~7 ~28 ~3 ~7 minecraft:dark_oak_log[axis=y]
fill ~28 ~1 ~10 ~28 ~3 ~10 minecraft:dark_oak_log[axis=y]
fill ~22 ~4 ~7 ~28 ~4 ~7 minecraft:air
fill ~22 ~4 ~8 ~28 ~4 ~8 minecraft:dark_oak_log[axis=x]
fill ~22 ~4 ~9 ~28 ~4 ~9 minecraft:air
fill ~22 ~4 ~10 ~28 ~4 ~10 minecraft:dark_oak_log[axis=x]
setblock ~22 ~4 ~11 minecraft:dark_oak_planks
fill ~23 ~4 ~11 ~28 ~4 ~11 minecraft:spruce_planks
setblock ~22 ~5 ~7 minecraft:stripped_spruce_log[axis=x]
fill ~22 ~5 ~8 ~23 ~5 ~8 minecraft:spruce_planks
setblock ~22 ~5 ~9 minecraft:stripped_spruce_log[axis=x]
setblock ~22 ~5 ~10 minecraft:dark_oak_planks
fill ~22 ~5 ~11 ~23 ~5 ~11 minecraft:stripped_spruce_log[axis=x]
fill ~23 ~5 ~7 ~24 ~5 ~7 minecraft:spruce_planks
setblock ~23 ~5 ~9 minecraft:dark_oak_planks
setblock ~23 ~5 ~10 minecraft:spruce_planks
setblock ~24 ~5 ~8 minecraft:stripped_spruce_log[axis=x]
setblock ~24 ~5 ~9 minecraft:spruce_planks
setblock ~24 ~5 ~10 minecraft:dark_oak_planks
setblock ~24 ~5 ~11 minecraft:spruce_planks
setblock ~25 ~5 ~7 minecraft:dark_oak_planks
fill ~25 ~5 ~8 ~25 ~5 ~11 minecraft:spruce_planks
fill ~26 ~5 ~7 ~26 ~5 ~9 minecraft:stripped_spruce_log[axis=x]
fill ~26 ~5 ~10 ~28 ~5 ~10 minecraft:spruce_planks
setblock ~26 ~5 ~11 minecraft:spruce_planks
setblock ~27 ~5 ~7 minecraft:dark_oak_planks
setblock ~27 ~5 ~8 minecraft:spruce_planks
setblock ~27 ~5 ~9 minecraft:dark_oak_planks
fill ~27 ~5 ~11 ~28 ~5 ~11 minecraft:dark_oak_planks
fill ~28 ~5 ~7 ~28 ~5 ~8 minecraft:spruce_planks
setblock ~28 ~5 ~9 minecraft:stripped_spruce_log[axis=x]
setblock ~22 ~6 ~8 minecraft:air
setblock ~22 ~6 ~11 minecraft:air
fill ~23 ~6 ~7 ~27 ~6 ~10 minecraft:air

# the parapet of bottom slabs at y 6 on the open sides (south and east, the west side from the second cell; the entry from the ladder at (22, 6, 11) stays open), posts at the corners with a lantern on the south-east one
setblock ~22 ~6 ~7 minecraft:dark_oak_log[axis=y]
fill ~22 ~6 ~9 ~22 ~6 ~10 minecraft:spruce_slab[type=bottom]
fill ~23 ~6 ~11 ~27 ~6 ~11 minecraft:spruce_slab[type=bottom]
setblock ~28 ~6 ~7 minecraft:dark_oak_log[axis=y]
fill ~28 ~6 ~8 ~28 ~6 ~10 minecraft:spruce_slab[type=bottom]
setblock ~28 ~6 ~11 minecraft:dark_oak_log[axis=y]

# the ladder up the front wall at x 22 from the yard to the deck (y 1..5)
fill ~22 ~1 ~12 ~22 ~5 ~12 minecraft:ladder[facing=south]

# the yard walls (y 1..2): a stone footing and a palisade of spruce logs, planks between
fill ~0 ~1 ~12 ~0 ~1 ~14 minecraft:stone_bricks
setblock ~0 ~1 ~16 minecraft:stone_bricks
setblock ~0 ~1 ~18 minecraft:stone_bricks
setblock ~0 ~1 ~20 minecraft:stone_bricks
fill ~0 ~1 ~22 ~0 ~1 ~26 minecraft:stone_bricks
setblock ~0 ~1 ~27 minecraft:cracked_stone_bricks
setblock ~0 ~1 ~29 minecraft:stone_bricks
setblock ~0 ~1 ~31 minecraft:stone_bricks
fill ~0 ~1 ~33 ~0 ~1 ~36 minecraft:stone_bricks
fill ~1 ~1 ~36 ~5 ~1 ~36 minecraft:stone_bricks
setblock ~6 ~1 ~36 minecraft:cracked_stone_bricks
fill ~7 ~1 ~36 ~9 ~1 ~36 minecraft:stone_bricks
fill ~21 ~1 ~36 ~22 ~1 ~36 minecraft:stone_bricks
setblock ~23 ~1 ~36 minecraft:cracked_stone_bricks
fill ~24 ~1 ~36 ~29 ~1 ~36 minecraft:stone_bricks
fill ~30 ~1 ~12 ~30 ~1 ~15 minecraft:stone_bricks
setblock ~30 ~1 ~16 minecraft:andesite
fill ~30 ~1 ~17 ~30 ~1 ~20 minecraft:stone_bricks
fill ~30 ~1 ~28 ~30 ~1 ~31 minecraft:stone_bricks
fill ~30 ~1 ~32 ~30 ~1 ~34 minecraft:cracked_stone_bricks
fill ~30 ~1 ~35 ~30 ~1 ~36 minecraft:stone_bricks
setblock ~0 ~2 ~12 minecraft:spruce_log[axis=y]
setblock ~0 ~2 ~13 minecraft:dark_oak_planks
setblock ~0 ~2 ~14 minecraft:spruce_log[axis=y]
setblock ~0 ~2 ~15 minecraft:dark_oak_planks
setblock ~0 ~2 ~16 minecraft:spruce_log[axis=y]
setblock ~0 ~2 ~17 minecraft:spruce_planks
setblock ~0 ~2 ~18 minecraft:spruce_log[axis=y]
setblock ~0 ~2 ~19 minecraft:spruce_planks
setblock ~0 ~2 ~20 minecraft:spruce_log[axis=y]
setblock ~0 ~2 ~21 minecraft:spruce_planks
setblock ~0 ~2 ~22 minecraft:spruce_log[axis=y]
setblock ~0 ~2 ~23 minecraft:spruce_planks
setblock ~0 ~2 ~24 minecraft:spruce_log[axis=y]
setblock ~0 ~2 ~25 minecraft:spruce_planks
setblock ~0 ~2 ~26 minecraft:spruce_log[axis=y]
setblock ~0 ~2 ~27 minecraft:spruce_planks
setblock ~0 ~2 ~28 minecraft:spruce_log[axis=y]
setblock ~0 ~2 ~29 minecraft:spruce_planks
setblock ~0 ~2 ~30 minecraft:spruce_log[axis=y]
setblock ~0 ~2 ~31 minecraft:spruce_planks
setblock ~0 ~2 ~32 minecraft:spruce_log[axis=y]
setblock ~0 ~2 ~33 minecraft:spruce_planks
setblock ~0 ~2 ~34 minecraft:spruce_log[axis=y]
setblock ~0 ~2 ~35 minecraft:spruce_planks
setblock ~0 ~2 ~36 minecraft:spruce_log[axis=y]
setblock ~1 ~2 ~36 minecraft:spruce_planks
setblock ~2 ~2 ~36 minecraft:spruce_log[axis=y]
setblock ~3 ~2 ~36 minecraft:spruce_planks
setblock ~4 ~2 ~36 minecraft:spruce_log[axis=y]
setblock ~5 ~2 ~36 minecraft:spruce_planks
setblock ~6 ~2 ~36 minecraft:spruce_log[axis=y]
setblock ~7 ~2 ~36 minecraft:spruce_planks
setblock ~8 ~2 ~36 minecraft:spruce_log[axis=y]
setblock ~9 ~2 ~36 minecraft:spruce_planks
setblock ~21 ~2 ~36 minecraft:spruce_planks
setblock ~22 ~2 ~36 minecraft:spruce_log[axis=y]
setblock ~23 ~2 ~36 minecraft:spruce_planks
setblock ~24 ~2 ~36 minecraft:spruce_log[axis=y]
setblock ~25 ~2 ~36 minecraft:spruce_planks
setblock ~26 ~2 ~36 minecraft:spruce_log[axis=y]
setblock ~27 ~2 ~36 minecraft:spruce_planks
setblock ~28 ~2 ~36 minecraft:spruce_log[axis=y]
setblock ~29 ~2 ~36 minecraft:spruce_planks
setblock ~30 ~2 ~12 minecraft:spruce_log[axis=y]
setblock ~30 ~2 ~13 minecraft:spruce_planks
setblock ~30 ~2 ~14 minecraft:spruce_log[axis=y]
setblock ~30 ~2 ~15 minecraft:spruce_planks
setblock ~30 ~2 ~16 minecraft:spruce_log[axis=y]
setblock ~30 ~2 ~17 minecraft:spruce_planks
setblock ~30 ~2 ~18 minecraft:spruce_log[axis=y]
setblock ~30 ~2 ~19 minecraft:spruce_planks
setblock ~30 ~2 ~20 minecraft:spruce_log[axis=y]
setblock ~30 ~2 ~28 minecraft:spruce_log[axis=y]
setblock ~30 ~2 ~29 minecraft:spruce_planks
setblock ~30 ~2 ~30 minecraft:spruce_log[axis=y]
setblock ~30 ~2 ~31 minecraft:spruce_planks
setblock ~30 ~2 ~32 minecraft:spruce_log[axis=y]
setblock ~30 ~2 ~33 minecraft:spruce_planks
setblock ~30 ~2 ~34 minecraft:spruce_log[axis=y]
setblock ~30 ~2 ~35 minecraft:spruce_planks
setblock ~30 ~2 ~36 minecraft:spruce_log[axis=y]

# the gate posts of the front gate (x 10 and 20, y 1..2) with the braziers on them, unlit campfires at (10, 3, 36) and (20, 3, 36)
fill ~10 ~1 ~36 ~10 ~2 ~36 minecraft:dark_oak_log[axis=y]
fill ~20 ~1 ~36 ~20 ~2 ~36 minecraft:dark_oak_log[axis=y]
setblock ~10 ~3 ~36 minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false]
setblock ~20 ~3 ~36 minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false]

# the side gate in the east wall (z 22..26): posts at z 21 and 27 four high and a beam over the opening at y 5, clear below
fill ~30 ~1 ~21 ~30 ~4 ~21 minecraft:dark_oak_log[axis=y]
fill ~30 ~1 ~27 ~30 ~4 ~27 minecraft:dark_oak_log[axis=y]
fill ~30 ~5 ~21 ~30 ~5 ~27 minecraft:dark_oak_log[axis=z]

# the forge corner: a lean-to roof of slabs at y 3 against the west wall on posts
fill ~4 ~1 ~14 ~4 ~2 ~14 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~18 ~4 ~2 ~18 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~22 ~4 ~2 ~22 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~0 ~3 ~14 ~4 ~3 ~22 minecraft:spruce_slab[type=top]

# the forge: anvil, grindstone, smithing table, furnace, cauldron, barrels
setblock ~0 ~1 ~15 minecraft:anvil[facing=east]
setblock ~0 ~1 ~17 minecraft:grindstone[face=floor,facing=east]
setblock ~0 ~1 ~19 minecraft:smithing_table
setblock ~1 ~1 ~22 minecraft:cauldron
setblock ~3 ~1 ~16 minecraft:barrel[facing=up,open=false]
setblock ~3 ~1 ~20 minecraft:barrel[facing=up,open=false]

# the smokehouse on the forge corner: a smoker beside the anvil, snow on the lean-to
setblock ~0 ~1 ~21 minecraft:smoker[facing=east,lit=false]

# the cots: a lean-to of slabs over four beds, chests and barrels between them
fill ~4 ~1 ~27 ~4 ~2 ~27 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~30 ~4 ~2 ~30 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~34 ~4 ~2 ~34 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~0 ~3 ~27 ~4 ~3 ~34 minecraft:spruce_slab[type=top]

# the beds, a chest and barrels
setblock ~0 ~1 ~28 minecraft:light_blue_bed[facing=east,part=foot,occupied=false]
setblock ~0 ~1 ~30 minecraft:light_blue_bed[facing=east,part=foot,occupied=false]
setblock ~0 ~1 ~32 minecraft:light_blue_bed[facing=east,part=foot,occupied=false]
setblock ~1 ~1 ~28 minecraft:light_blue_bed[facing=east,part=head,occupied=false]
setblock ~1 ~1 ~30 minecraft:light_blue_bed[facing=east,part=head,occupied=false]
setblock ~1 ~1 ~32 minecraft:light_blue_bed[facing=east,part=head,occupied=false]
setblock ~2 ~1 ~29 minecraft:barrel[facing=up,open=false]
setblock ~2 ~1 ~31 minecraft:chest[facing=west,type=single,waterlogged=false]
setblock ~2 ~1 ~33 minecraft:barrel[facing=up,open=false]

# a hide frame: posts and a rail of spruce fence, hides of wool hung in two courses, a lantern on the rail
setblock ~6 ~1 ~12 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
setblock ~7 ~1 ~12 minecraft:air
fill ~8 ~1 ~12 ~8 ~2 ~12 minecraft:white_wool
fill ~9 ~1 ~12 ~9 ~2 ~12 minecraft:gray_wool
fill ~10 ~1 ~12 ~10 ~3 ~12 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
fill ~6 ~2 ~12 ~6 ~3 ~12 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~7 ~2 ~12 minecraft:gray_wool
fill ~7 ~3 ~12 ~9 ~3 ~12 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~8 ~4 ~12 minecraft:lantern[hanging=false]

# a weapon rack of blocks: stripped logs with pike heads of lightning rods and iron bars
setblock ~24 ~1 ~12 minecraft:stripped_spruce_log[axis=x]
setblock ~25 ~1 ~12 minecraft:iron_bars[north=true,east=true,south=false,west=true]
setblock ~26 ~1 ~12 minecraft:stripped_spruce_log[axis=x]
setblock ~27 ~1 ~12 minecraft:iron_bars[north=true,east=true,south=false,west=true]
setblock ~28 ~1 ~12 minecraft:stripped_spruce_log[axis=x]
setblock ~24 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~25 ~2 ~12 minecraft:iron_bars[north=false,east=false,south=false,west=false]
setblock ~26 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~27 ~2 ~12 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~28 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]

# a sled: runners of spruce slabs with a stair curl at the front, a bed of planks on fence posts, a load of barrels and hides
fill ~25 ~1 ~14 ~25 ~1 ~17 minecraft:spruce_slab[type=bottom]
setblock ~25 ~1 ~18 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~26 ~1 ~14 ~26 ~2 ~14 minecraft:spruce_planks
fill ~26 ~1 ~17 ~26 ~2 ~17 minecraft:spruce_planks
setblock ~26 ~1 ~19 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~27 ~1 ~14 ~27 ~1 ~17 minecraft:spruce_slab[type=bottom]
setblock ~27 ~1 ~18 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~25 ~2 ~14 minecraft:dark_oak_planks
fill ~25 ~2 ~15 ~27 ~2 ~16 minecraft:spruce_planks
setblock ~25 ~2 ~17 minecraft:dark_oak_planks
setblock ~25 ~2 ~18 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
setblock ~26 ~2 ~18 minecraft:spruce_stairs[facing=north,half=top]
setblock ~27 ~2 ~14 minecraft:dark_oak_planks
setblock ~27 ~2 ~17 minecraft:dark_oak_planks
setblock ~27 ~2 ~18 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
fill ~25 ~3 ~15 ~25 ~3 ~16 minecraft:barrel[facing=up,open=false]
setblock ~26 ~3 ~15 minecraft:white_wool
setblock ~26 ~3 ~16 minecraft:light_gray_wool
setblock ~27 ~3 ~15 minecraft:brown_wool
setblock ~27 ~3 ~16 minecraft:chest[facing=north,type=single,waterlogged=false]

# a woodpile: stacks of stripped spruce logs laid along z against the south-east corner
fill ~27 ~1 ~28 ~27 ~1 ~33 minecraft:stripped_spruce_log[axis=z]
fill ~28 ~1 ~28 ~28 ~1 ~33 minecraft:spruce_log[axis=z]
fill ~29 ~1 ~28 ~29 ~1 ~33 minecraft:stripped_spruce_log[axis=z]
fill ~27 ~2 ~28 ~27 ~2 ~33 minecraft:spruce_log[axis=z]
setblock ~28 ~2 ~29 minecraft:stripped_spruce_log[axis=z]
fill ~28 ~2 ~31 ~28 ~2 ~33 minecraft:stripped_spruce_log[axis=z]
fill ~29 ~2 ~28 ~29 ~2 ~33 minecraft:spruce_log[axis=z]
fill ~27 ~3 ~28 ~27 ~3 ~30 minecraft:stripped_spruce_log[axis=z]
setblock ~27 ~3 ~33 minecraft:stripped_spruce_log[axis=z]
fill ~29 ~3 ~29 ~29 ~3 ~33 minecraft:stripped_spruce_log[axis=z]

# crates and barrels along the front wall of the storehouse and in the corners
setblock ~1 ~1 ~12 minecraft:barrel[facing=up,open=false]
setblock ~1 ~1 ~24 minecraft:barrel[facing=up,open=false]
setblock ~1 ~1 ~25 minecraft:chest[facing=west,type=single,waterlogged=false]
setblock ~2 ~1 ~12 minecraft:spruce_planks
setblock ~2 ~1 ~35 minecraft:barrel[facing=east,open=false]
setblock ~3 ~1 ~12 minecraft:barrel[facing=east,open=false]
setblock ~3 ~1 ~35 minecraft:barrel[facing=up,open=false]
setblock ~9 ~1 ~35 minecraft:spruce_planks
setblock ~19 ~1 ~12 minecraft:chest[facing=north,type=single,waterlogged=false]
setblock ~20 ~1 ~12 minecraft:barrel[facing=west,open=false]
setblock ~29 ~1 ~12 minecraft:chest[facing=west,type=single,waterlogged=false]
setblock ~1 ~2 ~12 minecraft:chest[facing=west,type=single,waterlogged=false]
setblock ~1 ~2 ~24 minecraft:barrel[facing=north,open=false]
setblock ~2 ~2 ~12 minecraft:barrel[facing=south,open=false]
setblock ~2 ~2 ~35 minecraft:chest[facing=east,type=single,waterlogged=false]
setblock ~9 ~2 ~35 minecraft:barrel[facing=east,open=false]
fill ~19 ~2 ~12 ~20 ~2 ~12 minecraft:barrel[facing=up,open=false]

# inside the storehouse: barrels, chests, stacked hides and a lantern, seen through the door and the shutters
fill ~3 ~1 ~2 ~4 ~1 ~2 minecraft:barrel[facing=up,open=false]
setblock ~3 ~1 ~3 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~5 ~1 ~9 minecraft:white_wool
setblock ~6 ~1 ~9 minecraft:light_gray_wool
setblock ~8 ~1 ~9 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~9 ~1 ~9 minecraft:barrel[facing=up,open=false]
fill ~12 ~1 ~2 ~13 ~1 ~2 minecraft:barrel[facing=up,open=false]
setblock ~15 ~1 ~9 minecraft:barrel[facing=up,open=false]
setblock ~17 ~1 ~2 minecraft:barrel[facing=up,open=false]
setblock ~18 ~1 ~3 minecraft:chest[facing=south,type=single,waterlogged=false]
fill ~21 ~1 ~9 ~22 ~1 ~9 minecraft:light_gray_wool
setblock ~24 ~1 ~3 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~5 ~2 ~9 minecraft:brown_wool
setblock ~6 ~2 ~9 minecraft:white_wool
setblock ~15 ~2 ~9 minecraft:lantern[hanging=false]
fill ~21 ~2 ~9 ~22 ~2 ~9 minecraft:white_wool

# snow settles on the roofs only: the lean-tos of the forge corner and the cots and the verge slabs of the roof (not above y 6, not on the lookout deck, nothing in the yard or on the walls)
fill ~0 ~4 ~14 ~1 ~4 ~14 minecraft:snow[layers=1]
fill ~0 ~4 ~16 ~0 ~4 ~20 minecraft:snow[layers=1]
setblock ~0 ~4 ~22 minecraft:snow[layers=1]
fill ~0 ~4 ~28 ~2 ~4 ~31 minecraft:snow[layers=1]
fill ~0 ~4 ~33 ~0 ~4 ~34 minecraft:snow[layers=1]
fill ~1 ~4 ~15 ~1 ~4 ~22 minecraft:snow[layers=1]
fill ~1 ~4 ~27 ~4 ~4 ~27 minecraft:snow[layers=1]
fill ~1 ~4 ~32 ~1 ~4 ~34 minecraft:snow[layers=1]
fill ~2 ~4 ~16 ~3 ~4 ~22 minecraft:snow[layers=1]
fill ~2 ~4 ~33 ~4 ~4 ~33 minecraft:snow[layers=1]
fill ~3 ~4 ~14 ~4 ~4 ~14 minecraft:snow[layers=1]
fill ~3 ~4 ~30 ~3 ~4 ~31 minecraft:snow[layers=1]
fill ~3 ~4 ~34 ~4 ~4 ~34 minecraft:snow[layers=1]
setblock ~4 ~4 ~15 minecraft:snow[layers=1]
fill ~4 ~4 ~17 ~4 ~4 ~22 minecraft:snow[layers=1]
fill ~4 ~4 ~28 ~4 ~4 ~31 minecraft:snow[layers=1]
setblock ~0 ~5 ~10 minecraft:snow[layers=1]
setblock ~30 ~5 ~1 minecraft:snow[layers=1]
setblock ~30 ~5 ~10 minecraft:snow[layers=1]
setblock ~0 ~6 ~3 minecraft:snow[layers=1]
setblock ~0 ~6 ~8 minecraft:snow[layers=1]
setblock ~30 ~6 ~3 minecraft:snow[layers=1]
setblock ~30 ~6 ~8 minecraft:snow[layers=1]
fill ~30 ~6 ~21 ~30 ~6 ~24 minecraft:snow[layers=1]
fill ~30 ~6 ~26 ~30 ~6 ~27 minecraft:snow[layers=1]
