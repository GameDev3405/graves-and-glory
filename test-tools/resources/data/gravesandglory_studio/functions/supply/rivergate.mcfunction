# size 31 9 37
# supply/rivergate: supply house of Rivergate, granary and walled yard: an oak frame on a cobble plinth under a sagging hay roof, a hoist over the door, a lookout on the roof, a yard with the capture circle, crates, a cart, a forge corner and cots.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Shared shape of the five supply houses (spec 7.1, plan Task 8b): storehouse x 0..30, z 0..11; lookout deck y 5 over x 22..28, z 7..11 with the ladder at (22, 1..5, 12); yard walls y 1..2 along x 0, x 30, z 36; front gate x 11..19; side gate z 22..26;
# capture circle centre (15, 0, 24) radius 10, edge ring 9 to 10, plinth x 14..16 z 23..25; the flag column |x-15|, |z-24| <= 3, y 1..8 is structure void; the braziers (unlit campfires) at (10, 3, 36) and (20, 3, 36); nothing solid above y 6.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~1 ~1 ~1 ~29 ~2 ~1 minecraft:air
fill ~1 ~1 ~2 ~2 ~3 ~10 minecraft:air
fill ~1 ~1 ~13 ~3 ~2 ~13 minecraft:air
fill ~1 ~1 ~15 ~2 ~2 ~15 minecraft:air
fill ~1 ~1 ~17 ~1 ~2 ~17 minecraft:air
fill ~1 ~1 ~19 ~2 ~2 ~20 minecraft:air
fill ~1 ~1 ~21 ~1 ~2 ~22 minecraft:air
fill ~1 ~1 ~25 ~1 ~2 ~26 minecraft:air
fill ~1 ~1 ~28 ~2 ~2 ~28 minecraft:air
fill ~1 ~1 ~30 ~2 ~2 ~30 minecraft:air
fill ~1 ~1 ~32 ~2 ~2 ~34 minecraft:air
fill ~1 ~1 ~35 ~1 ~8 ~35 minecraft:air
fill ~2 ~1 ~14 ~4 ~2 ~14 minecraft:air
fill ~2 ~1 ~16 ~3 ~2 ~18 minecraft:air
fill ~2 ~1 ~22 ~2 ~8 ~22 minecraft:air
fill ~2 ~1 ~24 ~2 ~2 ~26 minecraft:air
fill ~3 ~1 ~4 ~3 ~3 ~10 minecraft:air
fill ~3 ~1 ~20 ~3 ~2 ~27 minecraft:air
fill ~3 ~1 ~29 ~3 ~2 ~29 minecraft:air
fill ~3 ~1 ~31 ~3 ~2 ~31 minecraft:air
fill ~3 ~1 ~33 ~3 ~2 ~34 minecraft:air
fill ~4 ~1 ~3 ~4 ~3 ~10 minecraft:air
fill ~4 ~1 ~15 ~4 ~2 ~16 minecraft:air
fill ~4 ~1 ~18 ~4 ~2 ~20 minecraft:air
fill ~4 ~1 ~22 ~4 ~8 ~25 minecraft:air
fill ~4 ~1 ~27 ~4 ~2 ~28 minecraft:air
fill ~4 ~1 ~30 ~4 ~2 ~32 minecraft:air
fill ~4 ~1 ~34 ~4 ~8 ~35 minecraft:air
fill ~5 ~1 ~2 ~5 ~4 ~8 minecraft:air
fill ~5 ~1 ~10 ~9 ~3 ~10 minecraft:air
fill ~5 ~1 ~12 ~8 ~8 ~33 minecraft:air
fill ~6 ~1 ~3 ~6 ~4 ~8 minecraft:air
fill ~7 ~1 ~2 ~11 ~4 ~8 minecraft:air
fill ~9 ~1 ~13 ~10 ~8 ~33 minecraft:air
fill ~10 ~1 ~9 ~11 ~3 ~10 minecraft:air
fill ~10 ~1 ~34 ~10 ~8 ~34 minecraft:air
fill ~11 ~1 ~12 ~18 ~1 ~20 minecraft:air
fill ~11 ~1 ~21 ~11 ~8 ~36 minecraft:air
fill ~12 ~1 ~3 ~13 ~3 ~10 minecraft:air
fill ~12 ~1 ~28 ~18 ~8 ~36 minecraft:air
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
fill ~21 ~1 ~12 ~21 ~8 ~35 minecraft:air
fill ~22 ~1 ~8 ~22 ~3 ~8 minecraft:air
fill ~22 ~1 ~13 ~22 ~8 ~35 minecraft:air
fill ~23 ~1 ~7 ~23 ~3 ~10 minecraft:air
fill ~23 ~1 ~12 ~23 ~8 ~35 minecraft:air
fill ~24 ~1 ~4 ~24 ~3 ~10 minecraft:air
fill ~24 ~1 ~13 ~24 ~8 ~14 minecraft:air
fill ~24 ~1 ~17 ~24 ~8 ~35 minecraft:air
fill ~25 ~1 ~3 ~29 ~4 ~6 minecraft:air
fill ~25 ~1 ~7 ~27 ~3 ~10 minecraft:air
fill ~25 ~1 ~12 ~28 ~8 ~13 minecraft:air
fill ~25 ~1 ~15 ~25 ~1 ~16 minecraft:air
fill ~25 ~1 ~18 ~25 ~8 ~35 minecraft:air
fill ~26 ~1 ~14 ~26 ~1 ~18 minecraft:air
fill ~26 ~1 ~20 ~26 ~8 ~35 minecraft:air
fill ~27 ~1 ~15 ~27 ~1 ~16 minecraft:air
fill ~27 ~1 ~18 ~27 ~8 ~27 minecraft:air
fill ~27 ~1 ~34 ~29 ~8 ~35 minecraft:air
fill ~28 ~1 ~8 ~28 ~3 ~9 minecraft:air
fill ~28 ~1 ~14 ~28 ~8 ~14 minecraft:air
fill ~28 ~1 ~17 ~28 ~8 ~27 minecraft:air
fill ~29 ~1 ~7 ~29 ~3 ~10 minecraft:air
fill ~29 ~1 ~13 ~29 ~8 ~27 minecraft:air
fill ~30 ~1 ~22 ~30 ~4 ~26 minecraft:air
setblock ~1 ~2 ~14 minecraft:air
setblock ~1 ~2 ~16 minecraft:air
setblock ~1 ~2 ~18 minecraft:air
fill ~1 ~2 ~27 ~2 ~2 ~27 minecraft:air
fill ~1 ~2 ~29 ~2 ~2 ~29 minecraft:air
fill ~1 ~2 ~31 ~2 ~2 ~31 minecraft:air
setblock ~2 ~2 ~21 minecraft:air
fill ~2 ~2 ~23 ~2 ~8 ~23 minecraft:air
fill ~2 ~2 ~35 ~2 ~8 ~35 minecraft:air
fill ~3 ~2 ~2 ~4 ~4 ~2 minecraft:air
fill ~3 ~2 ~3 ~3 ~4 ~3 minecraft:air
setblock ~3 ~2 ~15 minecraft:air
setblock ~3 ~2 ~19 minecraft:air
setblock ~3 ~2 ~28 minecraft:air
setblock ~3 ~2 ~30 minecraft:air
setblock ~3 ~2 ~32 minecraft:air
fill ~5 ~2 ~9 ~9 ~4 ~9 minecraft:air
fill ~5 ~2 ~34 ~5 ~8 ~35 minecraft:air
fill ~6 ~2 ~2 ~6 ~3 ~2 minecraft:air
fill ~11 ~2 ~12 ~18 ~3 ~12 minecraft:air
fill ~11 ~2 ~13 ~17 ~8 ~20 minecraft:air
fill ~12 ~2 ~2 ~13 ~4 ~2 minecraft:air
fill ~17 ~2 ~2 ~18 ~4 ~2 minecraft:air
fill ~18 ~2 ~14 ~18 ~8 ~20 minecraft:air
fill ~19 ~2 ~3 ~19 ~4 ~3 minecraft:air
fill ~20 ~2 ~12 ~20 ~8 ~12 minecraft:air
fill ~21 ~2 ~9 ~22 ~3 ~9 minecraft:air
fill ~24 ~2 ~3 ~24 ~4 ~3 minecraft:air
fill ~26 ~2 ~19 ~26 ~8 ~19 minecraft:air
fill ~28 ~2 ~30 ~28 ~8 ~30 minecraft:air
fill ~29 ~2 ~29 ~29 ~8 ~32 minecraft:air
fill ~0 ~3 ~12 ~4 ~8 ~12 minecraft:air
fill ~0 ~3 ~13 ~0 ~8 ~36 minecraft:air
fill ~1 ~3 ~1 ~23 ~3 ~1 minecraft:air
fill ~1 ~3 ~22 ~1 ~8 ~23 minecraft:air
fill ~1 ~3 ~25 ~1 ~8 ~25 minecraft:air
fill ~1 ~3 ~34 ~3 ~8 ~34 minecraft:air
fill ~1 ~3 ~36 ~2 ~8 ~36 minecraft:air
fill ~2 ~3 ~24 ~2 ~8 ~25 minecraft:air
fill ~3 ~3 ~22 ~3 ~8 ~25 minecraft:air
fill ~3 ~3 ~35 ~3 ~8 ~36 minecraft:air
fill ~4 ~3 ~36 ~5 ~8 ~36 minecraft:air
fill ~6 ~3 ~34 ~9 ~8 ~36 minecraft:air
fill ~9 ~3 ~12 ~10 ~8 ~12 minecraft:air
fill ~10 ~3 ~35 ~10 ~8 ~35 minecraft:air
fill ~15 ~3 ~9 ~15 ~4 ~9 minecraft:air
fill ~19 ~3 ~12 ~19 ~8 ~12 minecraft:air
fill ~21 ~3 ~36 ~29 ~8 ~36 minecraft:air
fill ~24 ~3 ~12 ~24 ~8 ~12 minecraft:air
fill ~24 ~3 ~15 ~24 ~8 ~16 minecraft:air
fill ~25 ~3 ~1 ~29 ~3 ~1 minecraft:air
fill ~25 ~3 ~14 ~27 ~8 ~14 minecraft:air
fill ~25 ~3 ~16 ~25 ~8 ~17 minecraft:air
fill ~26 ~3 ~17 ~27 ~8 ~17 minecraft:air
fill ~26 ~3 ~18 ~26 ~8 ~18 minecraft:air
fill ~27 ~3 ~15 ~28 ~8 ~15 minecraft:air
fill ~27 ~3 ~28 ~30 ~8 ~28 minecraft:air
fill ~27 ~3 ~29 ~28 ~8 ~29 minecraft:air
fill ~27 ~3 ~31 ~27 ~8 ~33 minecraft:air
fill ~28 ~3 ~16 ~28 ~8 ~16 minecraft:air
fill ~28 ~3 ~32 ~28 ~8 ~33 minecraft:air
fill ~29 ~3 ~12 ~30 ~8 ~12 minecraft:air
fill ~29 ~3 ~33 ~29 ~8 ~33 minecraft:air
fill ~30 ~3 ~13 ~30 ~8 ~20 minecraft:air
fill ~30 ~3 ~29 ~30 ~8 ~36 minecraft:air
fill ~1 ~4 ~2 ~2 ~4 ~9 minecraft:air
fill ~1 ~4 ~13 ~4 ~8 ~21 minecraft:air
fill ~1 ~4 ~24 ~1 ~8 ~24 minecraft:air
fill ~1 ~4 ~26 ~4 ~8 ~33 minecraft:air
fill ~3 ~4 ~4 ~3 ~4 ~9 minecraft:air
fill ~4 ~4 ~3 ~4 ~4 ~9 minecraft:air
fill ~10 ~4 ~9 ~11 ~4 ~9 minecraft:air
fill ~10 ~4 ~36 ~10 ~8 ~36 minecraft:air
fill ~11 ~4 ~12 ~17 ~8 ~12 minecraft:air
fill ~12 ~4 ~4 ~12 ~4 ~9 minecraft:air
fill ~13 ~4 ~3 ~13 ~4 ~9 minecraft:air
setblock ~14 ~4 ~9 minecraft:air
setblock ~16 ~4 ~9 minecraft:air
fill ~17 ~4 ~3 ~18 ~4 ~9 minecraft:air
fill ~19 ~4 ~4 ~19 ~4 ~9 minecraft:air
fill ~20 ~4 ~9 ~21 ~4 ~9 minecraft:air
fill ~20 ~4 ~36 ~20 ~8 ~36 minecraft:air
fill ~24 ~4 ~4 ~24 ~5 ~6 minecraft:air
fill ~25 ~4 ~15 ~26 ~8 ~15 minecraft:air
fill ~26 ~4 ~16 ~27 ~8 ~16 minecraft:air
fill ~27 ~4 ~30 ~27 ~8 ~30 minecraft:air
fill ~28 ~4 ~31 ~28 ~8 ~31 minecraft:air
fill ~29 ~4 ~7 ~29 ~4 ~9 minecraft:air
fill ~0 ~5 ~0 ~30 ~8 ~1 minecraft:air
fill ~0 ~5 ~10 ~21 ~8 ~10 minecraft:air
fill ~0 ~5 ~11 ~17 ~8 ~11 minecraft:air
fill ~1 ~5 ~4 ~23 ~5 ~6 minecraft:air
fill ~1 ~5 ~7 ~21 ~5 ~7 minecraft:air
fill ~19 ~5 ~11 ~21 ~8 ~11 minecraft:air
fill ~25 ~5 ~4 ~29 ~5 ~6 minecraft:air
setblock ~29 ~5 ~7 minecraft:air
fill ~29 ~5 ~10 ~30 ~8 ~11 minecraft:air
fill ~0 ~6 ~2 ~30 ~8 ~3 minecraft:air
fill ~0 ~6 ~8 ~21 ~8 ~9 minecraft:air
fill ~18 ~6 ~11 ~18 ~8 ~13 minecraft:air
fill ~22 ~6 ~12 ~22 ~8 ~12 minecraft:air
fill ~29 ~6 ~8 ~30 ~8 ~9 minecraft:air
fill ~30 ~6 ~21 ~30 ~8 ~27 minecraft:air
fill ~0 ~7 ~4 ~30 ~8 ~7 minecraft:air
fill ~22 ~7 ~8 ~28 ~8 ~11 minecraft:air

# the ground layer: planks and loose hay on the storehouse floor, paved capture circle (stone brick, a ring of smooth stone, a plinth), coarse dirt, cobble, gravel and mud in the yard, a cobbled mouth at the gate
setblock ~0 ~0 ~0 minecraft:hay_block[axis=x]
setblock ~0 ~0 ~1 minecraft:dark_oak_planks
fill ~0 ~0 ~2 ~0 ~0 ~3 minecraft:oak_planks
fill ~0 ~0 ~4 ~1 ~0 ~4 minecraft:spruce_planks
fill ~0 ~0 ~5 ~2 ~0 ~5 minecraft:oak_planks
setblock ~0 ~0 ~6 minecraft:spruce_planks
fill ~0 ~0 ~7 ~1 ~0 ~7 minecraft:oak_planks
fill ~0 ~0 ~8 ~0 ~0 ~9 minecraft:oak_planks
setblock ~0 ~0 ~10 minecraft:hay_block[axis=x]
setblock ~0 ~0 ~11 minecraft:spruce_planks
fill ~0 ~0 ~12 ~0 ~0 ~13 minecraft:coarse_dirt
setblock ~0 ~0 ~14 minecraft:mossy_cobblestone
fill ~0 ~0 ~15 ~0 ~0 ~16 minecraft:coarse_dirt
setblock ~0 ~0 ~17 minecraft:cobblestone
setblock ~0 ~0 ~18 minecraft:coarse_dirt
setblock ~0 ~0 ~19 minecraft:cobblestone
setblock ~0 ~0 ~20 minecraft:packed_mud
setblock ~0 ~0 ~21 minecraft:coarse_dirt
setblock ~0 ~0 ~22 minecraft:mossy_cobblestone
setblock ~0 ~0 ~23 minecraft:coarse_dirt
setblock ~0 ~0 ~24 minecraft:gravel
setblock ~0 ~0 ~25 minecraft:mossy_cobblestone
setblock ~0 ~0 ~26 minecraft:packed_mud
setblock ~0 ~0 ~27 minecraft:mossy_cobblestone
fill ~0 ~0 ~28 ~0 ~0 ~30 minecraft:cobblestone
setblock ~0 ~0 ~31 minecraft:coarse_dirt
setblock ~0 ~0 ~32 minecraft:andesite
setblock ~0 ~0 ~33 minecraft:packed_mud
setblock ~0 ~0 ~34 minecraft:cobblestone
fill ~0 ~0 ~35 ~2 ~0 ~35 minecraft:coarse_dirt
setblock ~0 ~0 ~36 minecraft:gravel
fill ~1 ~0 ~0 ~1 ~0 ~1 minecraft:oak_planks
setblock ~1 ~0 ~2 minecraft:dark_oak_planks
fill ~1 ~0 ~3 ~3 ~0 ~3 minecraft:spruce_planks
fill ~1 ~0 ~6 ~2 ~0 ~6 minecraft:oak_planks
setblock ~1 ~0 ~8 minecraft:spruce_planks
setblock ~1 ~0 ~9 minecraft:oak_planks
fill ~1 ~0 ~10 ~2 ~0 ~10 minecraft:spruce_planks
fill ~1 ~0 ~11 ~2 ~0 ~11 minecraft:oak_planks
setblock ~1 ~0 ~12 minecraft:cobblestone
setblock ~1 ~0 ~13 minecraft:coarse_dirt
fill ~1 ~0 ~14 ~2 ~0 ~14 minecraft:cobblestone
fill ~1 ~0 ~15 ~4 ~0 ~15 minecraft:packed_mud
fill ~1 ~0 ~16 ~1 ~0 ~17 minecraft:cobblestone
setblock ~1 ~0 ~18 minecraft:gravel
fill ~1 ~0 ~19 ~2 ~0 ~19 minecraft:andesite
setblock ~1 ~0 ~20 minecraft:coarse_dirt
setblock ~1 ~0 ~21 minecraft:cobblestone
fill ~1 ~0 ~22 ~2 ~0 ~23 minecraft:coarse_dirt
setblock ~1 ~0 ~24 minecraft:cobblestone
fill ~1 ~0 ~25 ~2 ~0 ~25 minecraft:coarse_dirt
setblock ~1 ~0 ~26 minecraft:cobblestone
setblock ~1 ~0 ~27 minecraft:coarse_dirt
setblock ~1 ~0 ~28 minecraft:packed_mud
setblock ~1 ~0 ~29 minecraft:andesite
setblock ~1 ~0 ~30 minecraft:mossy_cobblestone
fill ~1 ~0 ~31 ~3 ~0 ~31 minecraft:gravel
setblock ~1 ~0 ~32 minecraft:cobblestone
setblock ~1 ~0 ~33 minecraft:andesite
fill ~1 ~0 ~34 ~2 ~0 ~34 minecraft:gravel
setblock ~1 ~0 ~36 minecraft:cobblestone
setblock ~2 ~0 ~0 minecraft:spruce_planks
setblock ~2 ~0 ~1 minecraft:hay_block[axis=x]
setblock ~2 ~0 ~2 minecraft:oak_planks
setblock ~2 ~0 ~4 minecraft:dark_oak_planks
setblock ~2 ~0 ~7 minecraft:dark_oak_planks
fill ~2 ~0 ~8 ~2 ~0 ~9 minecraft:oak_planks
setblock ~2 ~0 ~12 minecraft:gravel
setblock ~2 ~0 ~13 minecraft:andesite
setblock ~2 ~0 ~16 minecraft:coarse_dirt
setblock ~2 ~0 ~17 minecraft:mossy_cobblestone
setblock ~2 ~0 ~18 minecraft:cobblestone
setblock ~2 ~0 ~20 minecraft:cobblestone
setblock ~2 ~0 ~21 minecraft:packed_mud
setblock ~2 ~0 ~24 minecraft:gravel
fill ~2 ~0 ~26 ~4 ~0 ~26 minecraft:coarse_dirt
setblock ~2 ~0 ~27 minecraft:cobblestone
setblock ~2 ~0 ~28 minecraft:coarse_dirt
setblock ~2 ~0 ~29 minecraft:gravel
setblock ~2 ~0 ~30 minecraft:packed_mud
setblock ~2 ~0 ~32 minecraft:coarse_dirt
fill ~2 ~0 ~33 ~3 ~0 ~33 minecraft:gravel
setblock ~2 ~0 ~36 minecraft:gravel
fill ~3 ~0 ~0 ~4 ~0 ~2 minecraft:oak_planks
fill ~3 ~0 ~4 ~3 ~0 ~5 minecraft:oak_planks
setblock ~3 ~0 ~6 minecraft:hay_block[axis=x]
setblock ~3 ~0 ~7 minecraft:oak_planks
setblock ~3 ~0 ~8 minecraft:dark_oak_planks
setblock ~3 ~0 ~9 minecraft:oak_planks
setblock ~3 ~0 ~10 minecraft:dark_oak_planks
setblock ~3 ~0 ~11 minecraft:spruce_planks
setblock ~3 ~0 ~12 minecraft:cobblestone
fill ~3 ~0 ~13 ~3 ~0 ~14 minecraft:coarse_dirt
setblock ~3 ~0 ~16 minecraft:packed_mud
setblock ~3 ~0 ~17 minecraft:andesite
setblock ~3 ~0 ~18 minecraft:coarse_dirt
setblock ~3 ~0 ~19 minecraft:cobblestone
setblock ~3 ~0 ~20 minecraft:packed_mud
fill ~3 ~0 ~21 ~4 ~0 ~21 minecraft:coarse_dirt
fill ~3 ~0 ~22 ~3 ~0 ~23 minecraft:cobblestone
setblock ~3 ~0 ~24 minecraft:packed_mud
setblock ~3 ~0 ~25 minecraft:mossy_cobblestone
fill ~3 ~0 ~27 ~5 ~0 ~27 minecraft:coarse_dirt
setblock ~3 ~0 ~28 minecraft:mossy_cobblestone
setblock ~3 ~0 ~29 minecraft:cobblestone
setblock ~3 ~0 ~30 minecraft:coarse_dirt
setblock ~3 ~0 ~32 minecraft:mossy_cobblestone
setblock ~3 ~0 ~34 minecraft:mossy_cobblestone
fill ~3 ~0 ~35 ~3 ~0 ~36 minecraft:andesite
setblock ~4 ~0 ~3 minecraft:oak_planks
setblock ~4 ~0 ~4 minecraft:spruce_planks
fill ~4 ~0 ~5 ~5 ~0 ~5 minecraft:oak_planks
setblock ~4 ~0 ~6 minecraft:spruce_planks
fill ~4 ~0 ~7 ~4 ~0 ~9 minecraft:dark_oak_planks
fill ~4 ~0 ~10 ~6 ~0 ~10 minecraft:spruce_planks
setblock ~4 ~0 ~11 minecraft:dark_oak_planks
setblock ~4 ~0 ~12 minecraft:packed_mud
fill ~4 ~0 ~13 ~5 ~0 ~13 minecraft:cobblestone
setblock ~4 ~0 ~14 minecraft:cobblestone
fill ~4 ~0 ~16 ~4 ~0 ~17 minecraft:coarse_dirt
setblock ~4 ~0 ~18 minecraft:gravel
setblock ~4 ~0 ~19 minecraft:packed_mud
fill ~4 ~0 ~20 ~5 ~0 ~20 minecraft:coarse_dirt
setblock ~4 ~0 ~22 minecraft:coarse_dirt
setblock ~4 ~0 ~23 minecraft:packed_mud
setblock ~4 ~0 ~24 minecraft:andesite
setblock ~4 ~0 ~25 minecraft:cobblestone
fill ~4 ~0 ~28 ~5 ~0 ~28 minecraft:gravel
setblock ~4 ~0 ~29 minecraft:mossy_cobblestone
setblock ~4 ~0 ~30 minecraft:gravel
setblock ~4 ~0 ~31 minecraft:cobblestone
setblock ~4 ~0 ~32 minecraft:gravel
setblock ~4 ~0 ~33 minecraft:coarse_dirt
setblock ~4 ~0 ~34 minecraft:packed_mud
setblock ~4 ~0 ~35 minecraft:coarse_dirt
setblock ~4 ~0 ~36 minecraft:cobblestone
fill ~5 ~0 ~0 ~8 ~0 ~0 minecraft:spruce_planks
fill ~5 ~0 ~1 ~5 ~0 ~2 minecraft:oak_planks
setblock ~5 ~0 ~3 minecraft:dark_oak_planks
fill ~5 ~0 ~4 ~6 ~0 ~4 minecraft:oak_planks
setblock ~5 ~0 ~6 minecraft:dark_oak_planks
fill ~5 ~0 ~7 ~6 ~0 ~8 minecraft:oak_planks
setblock ~5 ~0 ~9 minecraft:hay_block[axis=x]
setblock ~5 ~0 ~11 minecraft:hay_block[axis=x]
setblock ~5 ~0 ~12 minecraft:coarse_dirt
fill ~5 ~0 ~14 ~6 ~0 ~14 minecraft:coarse_dirt
setblock ~5 ~0 ~15 minecraft:gravel
setblock ~5 ~0 ~16 minecraft:mossy_cobblestone
setblock ~5 ~0 ~17 minecraft:gravel
fill ~5 ~0 ~18 ~6 ~0 ~18 minecraft:coarse_dirt
setblock ~5 ~0 ~19 minecraft:mossy_cobblestone
setblock ~5 ~0 ~21 minecraft:gravel
setblock ~5 ~0 ~22 minecraft:cobblestone
setblock ~5 ~0 ~23 minecraft:coarse_dirt
setblock ~5 ~0 ~24 minecraft:smooth_stone
setblock ~5 ~0 ~25 minecraft:coarse_dirt
setblock ~5 ~0 ~26 minecraft:cobblestone
fill ~5 ~0 ~29 ~6 ~0 ~30 minecraft:cobblestone
setblock ~5 ~0 ~31 minecraft:coarse_dirt
fill ~5 ~0 ~32 ~5 ~0 ~33 minecraft:cobblestone
setblock ~5 ~0 ~34 minecraft:coarse_dirt
setblock ~5 ~0 ~35 minecraft:gravel
setblock ~5 ~0 ~36 minecraft:coarse_dirt
setblock ~6 ~0 ~1 minecraft:spruce_planks
setblock ~6 ~0 ~2 minecraft:oak_planks
setblock ~6 ~0 ~3 minecraft:spruce_planks
fill ~6 ~0 ~5 ~9 ~0 ~5 minecraft:dark_oak_planks
setblock ~6 ~0 ~6 minecraft:spruce_planks
setblock ~6 ~0 ~9 minecraft:oak_planks
setblock ~6 ~0 ~11 minecraft:oak_planks
fill ~6 ~0 ~12 ~6 ~0 ~13 minecraft:packed_mud
setblock ~6 ~0 ~15 minecraft:cobblestone
fill ~6 ~0 ~16 ~7 ~0 ~16 minecraft:andesite
fill ~6 ~0 ~17 ~7 ~0 ~17 minecraft:cobblestone
setblock ~6 ~0 ~19 minecraft:coarse_dirt
fill ~6 ~0 ~20 ~6 ~0 ~23 minecraft:smooth_stone
setblock ~6 ~0 ~24 minecraft:stone_bricks
fill ~6 ~0 ~25 ~6 ~0 ~28 minecraft:smooth_stone
setblock ~6 ~0 ~31 minecraft:cobblestone
fill ~6 ~0 ~32 ~6 ~0 ~33 minecraft:gravel
setblock ~6 ~0 ~34 minecraft:cobblestone
fill ~6 ~0 ~35 ~7 ~0 ~35 minecraft:packed_mud
setblock ~6 ~0 ~36 minecraft:cobblestone
setblock ~7 ~0 ~1 minecraft:oak_planks
fill ~7 ~0 ~2 ~8 ~0 ~2 minecraft:spruce_planks
fill ~7 ~0 ~3 ~9 ~0 ~3 minecraft:oak_planks
setblock ~7 ~0 ~4 minecraft:spruce_planks
fill ~7 ~0 ~6 ~7 ~0 ~7 minecraft:oak_planks
fill ~7 ~0 ~8 ~8 ~0 ~10 minecraft:spruce_planks
setblock ~7 ~0 ~11 minecraft:spruce_planks
fill ~7 ~0 ~12 ~8 ~0 ~12 minecraft:coarse_dirt
setblock ~7 ~0 ~13 minecraft:packed_mud
fill ~7 ~0 ~14 ~7 ~0 ~15 minecraft:gravel
fill ~7 ~0 ~18 ~8 ~0 ~18 minecraft:smooth_stone
setblock ~7 ~0 ~19 minecraft:smooth_stone
setblock ~7 ~0 ~20 minecraft:cobblestone
setblock ~7 ~0 ~21 minecraft:cracked_stone_bricks
fill ~7 ~0 ~22 ~8 ~0 ~22 minecraft:cobblestone
setblock ~7 ~0 ~23 minecraft:stone_bricks
setblock ~7 ~0 ~24 minecraft:mossy_stone_bricks
setblock ~7 ~0 ~25 minecraft:cracked_stone_bricks
setblock ~7 ~0 ~26 minecraft:cobblestone
fill ~7 ~0 ~27 ~8 ~0 ~28 minecraft:stone_bricks
fill ~7 ~0 ~29 ~7 ~0 ~30 minecraft:smooth_stone
setblock ~7 ~0 ~31 minecraft:andesite
setblock ~7 ~0 ~32 minecraft:packed_mud
setblock ~7 ~0 ~33 minecraft:mossy_cobblestone
setblock ~7 ~0 ~34 minecraft:coarse_dirt
setblock ~7 ~0 ~36 minecraft:mossy_cobblestone
fill ~8 ~0 ~1 ~10 ~0 ~1 minecraft:spruce_planks
setblock ~8 ~0 ~4 minecraft:hay_block[axis=x]
setblock ~8 ~0 ~6 minecraft:dark_oak_planks
setblock ~8 ~0 ~7 minecraft:hay_block[axis=x]
setblock ~8 ~0 ~11 minecraft:oak_planks
setblock ~8 ~0 ~13 minecraft:coarse_dirt
setblock ~8 ~0 ~14 minecraft:mossy_cobblestone
setblock ~8 ~0 ~15 minecraft:andesite
setblock ~8 ~0 ~16 minecraft:mossy_cobblestone
fill ~8 ~0 ~17 ~9 ~0 ~17 minecraft:smooth_stone
setblock ~8 ~0 ~19 minecraft:mossy_stone_bricks
setblock ~8 ~0 ~20 minecraft:andesite
setblock ~8 ~0 ~21 minecraft:mossy_stone_bricks
fill ~8 ~0 ~23 ~8 ~0 ~24 minecraft:andesite
fill ~8 ~0 ~25 ~8 ~0 ~26 minecraft:cobblestone
setblock ~8 ~0 ~29 minecraft:stone_bricks
fill ~8 ~0 ~30 ~8 ~0 ~31 minecraft:smooth_stone
setblock ~8 ~0 ~32 minecraft:mossy_cobblestone
setblock ~8 ~0 ~33 minecraft:coarse_dirt
fill ~8 ~0 ~34 ~8 ~0 ~35 minecraft:mossy_cobblestone
setblock ~8 ~0 ~36 minecraft:coarse_dirt
setblock ~9 ~0 ~0 minecraft:oak_planks
setblock ~9 ~0 ~2 minecraft:hay_block[axis=x]
setblock ~9 ~0 ~4 minecraft:spruce_planks
fill ~9 ~0 ~6 ~9 ~0 ~7 minecraft:oak_planks
setblock ~9 ~0 ~8 minecraft:dark_oak_planks
setblock ~9 ~0 ~9 minecraft:spruce_planks
fill ~9 ~0 ~10 ~9 ~0 ~11 minecraft:oak_planks
setblock ~9 ~0 ~12 minecraft:gravel
setblock ~9 ~0 ~13 minecraft:cobblestone
fill ~9 ~0 ~14 ~9 ~0 ~15 minecraft:coarse_dirt
fill ~9 ~0 ~16 ~10 ~0 ~16 minecraft:smooth_stone
setblock ~9 ~0 ~18 minecraft:cobblestone
setblock ~9 ~0 ~19 minecraft:cracked_stone_bricks
fill ~9 ~0 ~20 ~9 ~0 ~24 minecraft:stone_bricks
setblock ~9 ~0 ~25 minecraft:cracked_stone_bricks
fill ~9 ~0 ~26 ~9 ~0 ~27 minecraft:stone_bricks
setblock ~9 ~0 ~28 minecraft:cracked_stone_bricks
setblock ~9 ~0 ~29 minecraft:mossy_stone_bricks
setblock ~9 ~0 ~30 minecraft:stone_bricks
fill ~9 ~0 ~31 ~9 ~0 ~32 minecraft:smooth_stone
setblock ~9 ~0 ~33 minecraft:andesite
fill ~9 ~0 ~34 ~10 ~0 ~34 minecraft:packed_mud
setblock ~9 ~0 ~35 minecraft:coarse_dirt
setblock ~9 ~0 ~36 minecraft:andesite
fill ~10 ~0 ~0 ~11 ~0 ~0 minecraft:spruce_planks
setblock ~10 ~0 ~2 minecraft:oak_planks
fill ~10 ~0 ~3 ~10 ~0 ~4 minecraft:spruce_planks
setblock ~10 ~0 ~5 minecraft:oak_planks
setblock ~10 ~0 ~6 minecraft:spruce_planks
setblock ~10 ~0 ~7 minecraft:oak_planks
setblock ~10 ~0 ~8 minecraft:spruce_planks
fill ~10 ~0 ~9 ~10 ~0 ~11 minecraft:oak_planks
fill ~10 ~0 ~12 ~10 ~0 ~13 minecraft:cobblestone
setblock ~10 ~0 ~14 minecraft:gravel
setblock ~10 ~0 ~15 minecraft:packed_mud
setblock ~10 ~0 ~17 minecraft:mossy_stone_bricks
fill ~10 ~0 ~18 ~10 ~0 ~19 minecraft:stone_bricks
fill ~10 ~0 ~20 ~10 ~0 ~21 minecraft:mossy_stone_bricks
fill ~10 ~0 ~22 ~10 ~0 ~23 minecraft:cobblestone
fill ~10 ~0 ~24 ~10 ~0 ~26 minecraft:stone_bricks
setblock ~10 ~0 ~27 minecraft:cracked_stone_bricks
fill ~10 ~0 ~28 ~10 ~0 ~30 minecraft:stone_bricks
setblock ~10 ~0 ~31 minecraft:mossy_stone_bricks
setblock ~10 ~0 ~32 minecraft:smooth_stone
setblock ~10 ~0 ~33 minecraft:coarse_dirt
setblock ~10 ~0 ~35 minecraft:mossy_cobblestone
setblock ~10 ~0 ~36 minecraft:packed_mud
setblock ~11 ~0 ~1 minecraft:oak_planks
fill ~11 ~0 ~2 ~12 ~0 ~2 minecraft:spruce_planks
setblock ~11 ~0 ~3 minecraft:dark_oak_planks
setblock ~11 ~0 ~4 minecraft:oak_planks
setblock ~11 ~0 ~5 minecraft:hay_block[axis=x]
fill ~11 ~0 ~6 ~12 ~0 ~6 minecraft:oak_planks
setblock ~11 ~0 ~7 minecraft:spruce_planks
fill ~11 ~0 ~8 ~11 ~0 ~10 minecraft:hay_block[axis=x]
setblock ~11 ~0 ~11 minecraft:dark_oak_planks
fill ~11 ~0 ~12 ~11 ~0 ~14 minecraft:mossy_cobblestone
fill ~11 ~0 ~15 ~14 ~0 ~15 minecraft:smooth_stone
setblock ~11 ~0 ~16 minecraft:stone_bricks
setblock ~11 ~0 ~17 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~18 minecraft:mossy_stone_bricks
fill ~11 ~0 ~19 ~12 ~0 ~19 minecraft:stone_bricks
fill ~11 ~0 ~20 ~12 ~0 ~20 minecraft:cobblestone
setblock ~11 ~0 ~21 minecraft:cracked_stone_bricks
fill ~11 ~0 ~22 ~11 ~0 ~23 minecraft:stone_bricks
setblock ~11 ~0 ~24 minecraft:cracked_stone_bricks
fill ~11 ~0 ~25 ~12 ~0 ~25 minecraft:stone_bricks
setblock ~11 ~0 ~26 minecraft:cobblestone
setblock ~11 ~0 ~27 minecraft:stone_bricks
setblock ~11 ~0 ~28 minecraft:cobblestone
fill ~11 ~0 ~29 ~13 ~0 ~29 minecraft:stone_bricks
fill ~11 ~0 ~30 ~11 ~0 ~31 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~32 minecraft:stone_bricks
fill ~11 ~0 ~33 ~14 ~0 ~33 minecraft:smooth_stone
fill ~11 ~0 ~34 ~11 ~0 ~35 minecraft:cobblestone
setblock ~11 ~0 ~36 minecraft:stone
fill ~12 ~0 ~0 ~15 ~0 ~0 minecraft:oak_planks
setblock ~12 ~0 ~1 minecraft:dark_oak_planks
fill ~12 ~0 ~3 ~12 ~0 ~4 minecraft:oak_planks
setblock ~12 ~0 ~5 minecraft:spruce_planks
setblock ~12 ~0 ~7 minecraft:hay_block[axis=x]
setblock ~12 ~0 ~8 minecraft:spruce_planks
setblock ~12 ~0 ~9 minecraft:oak_planks
setblock ~12 ~0 ~10 minecraft:dark_oak_planks
setblock ~12 ~0 ~11 minecraft:spruce_planks
setblock ~12 ~0 ~12 minecraft:coarse_dirt
setblock ~12 ~0 ~13 minecraft:cobblestone
setblock ~12 ~0 ~14 minecraft:packed_mud
fill ~12 ~0 ~16 ~12 ~0 ~17 minecraft:cracked_stone_bricks
fill ~12 ~0 ~18 ~13 ~0 ~18 minecraft:stone_bricks
fill ~12 ~0 ~21 ~12 ~0 ~22 minecraft:stone_bricks
setblock ~12 ~0 ~23 minecraft:mossy_stone_bricks
setblock ~12 ~0 ~24 minecraft:andesite
setblock ~12 ~0 ~26 minecraft:stone_bricks
setblock ~12 ~0 ~27 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~28 minecraft:mossy_stone_bricks
setblock ~12 ~0 ~30 minecraft:stone_bricks
setblock ~12 ~0 ~31 minecraft:cobblestone
fill ~12 ~0 ~32 ~13 ~0 ~32 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~34 minecraft:stone
setblock ~12 ~0 ~35 minecraft:andesite
setblock ~12 ~0 ~36 minecraft:mossy_cobblestone
fill ~13 ~0 ~1 ~14 ~0 ~2 minecraft:oak_planks
fill ~13 ~0 ~3 ~14 ~0 ~3 minecraft:spruce_planks
fill ~13 ~0 ~4 ~14 ~0 ~5 minecraft:oak_planks
setblock ~13 ~0 ~6 minecraft:spruce_planks
fill ~13 ~0 ~7 ~13 ~0 ~8 minecraft:oak_planks
setblock ~13 ~0 ~9 minecraft:spruce_planks
setblock ~13 ~0 ~10 minecraft:oak_planks
setblock ~13 ~0 ~11 minecraft:dark_oak_planks
setblock ~13 ~0 ~12 minecraft:packed_mud
setblock ~13 ~0 ~13 minecraft:coarse_dirt
setblock ~13 ~0 ~14 minecraft:andesite
setblock ~13 ~0 ~16 minecraft:stone_bricks
setblock ~13 ~0 ~17 minecraft:mossy_stone_bricks
setblock ~13 ~0 ~19 minecraft:andesite
fill ~13 ~0 ~20 ~13 ~0 ~21 minecraft:stone_bricks
setblock ~13 ~0 ~22 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~23 minecraft:cobblestone
fill ~13 ~0 ~24 ~13 ~0 ~27 minecraft:stone_bricks
setblock ~13 ~0 ~28 minecraft:andesite
setblock ~13 ~0 ~30 minecraft:cobblestone
setblock ~13 ~0 ~31 minecraft:mossy_stone_bricks
fill ~13 ~0 ~34 ~14 ~0 ~34 minecraft:cobblestone
fill ~13 ~0 ~35 ~13 ~0 ~36 minecraft:cobblestone
setblock ~14 ~0 ~6 minecraft:oak_planks
fill ~14 ~0 ~7 ~14 ~0 ~8 minecraft:dark_oak_planks
setblock ~14 ~0 ~9 minecraft:oak_planks
setblock ~14 ~0 ~10 minecraft:dark_oak_planks
setblock ~14 ~0 ~11 minecraft:oak_planks
setblock ~14 ~0 ~12 minecraft:coarse_dirt
setblock ~14 ~0 ~13 minecraft:mossy_cobblestone
setblock ~14 ~0 ~14 minecraft:coarse_dirt
setblock ~14 ~0 ~16 minecraft:cracked_stone_bricks
fill ~14 ~0 ~17 ~14 ~0 ~18 minecraft:stone_bricks
fill ~14 ~0 ~19 ~15 ~0 ~19 minecraft:mossy_stone_bricks
setblock ~14 ~0 ~20 minecraft:andesite
fill ~14 ~0 ~21 ~14 ~0 ~22 minecraft:stone_bricks
fill ~14 ~0 ~23 ~16 ~0 ~25 minecraft:chiseled_stone_bricks
setblock ~14 ~0 ~26 minecraft:stone_bricks
setblock ~14 ~0 ~27 minecraft:cobblestone
fill ~14 ~0 ~28 ~15 ~0 ~28 minecraft:stone_bricks
setblock ~14 ~0 ~29 minecraft:mossy_stone_bricks
setblock ~14 ~0 ~30 minecraft:andesite
fill ~14 ~0 ~31 ~15 ~0 ~31 minecraft:cracked_stone_bricks
setblock ~14 ~0 ~32 minecraft:andesite
setblock ~14 ~0 ~35 minecraft:stone
setblock ~14 ~0 ~36 minecraft:andesite
setblock ~15 ~0 ~1 minecraft:hay_block[axis=x]
setblock ~15 ~0 ~2 minecraft:spruce_planks
fill ~15 ~0 ~3 ~15 ~0 ~4 minecraft:oak_planks
setblock ~15 ~0 ~5 minecraft:spruce_planks
setblock ~15 ~0 ~6 minecraft:hay_block[axis=x]
fill ~15 ~0 ~7 ~15 ~0 ~8 minecraft:oak_planks
fill ~15 ~0 ~9 ~16 ~0 ~9 minecraft:hay_block[axis=x]
fill ~15 ~0 ~10 ~15 ~0 ~11 minecraft:oak_planks
fill ~15 ~0 ~12 ~16 ~0 ~12 minecraft:packed_mud
setblock ~15 ~0 ~13 minecraft:coarse_dirt
setblock ~15 ~0 ~14 minecraft:smooth_stone
setblock ~15 ~0 ~15 minecraft:mossy_stone_bricks
setblock ~15 ~0 ~16 minecraft:stone_bricks
setblock ~15 ~0 ~17 minecraft:mossy_stone_bricks
setblock ~15 ~0 ~18 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~20 minecraft:stone_bricks
setblock ~15 ~0 ~21 minecraft:cobblestone
setblock ~15 ~0 ~22 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~26 minecraft:cobblestone
fill ~15 ~0 ~27 ~16 ~0 ~27 minecraft:stone_bricks
fill ~15 ~0 ~29 ~16 ~0 ~30 minecraft:stone_bricks
setblock ~15 ~0 ~32 minecraft:mossy_stone_bricks
setblock ~15 ~0 ~33 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~34 minecraft:smooth_stone
fill ~15 ~0 ~35 ~15 ~0 ~36 minecraft:cobblestone
setblock ~16 ~0 ~0 minecraft:hay_block[axis=x]
fill ~16 ~0 ~1 ~17 ~0 ~1 minecraft:dark_oak_planks
fill ~16 ~0 ~2 ~16 ~0 ~4 minecraft:oak_planks
setblock ~16 ~0 ~5 minecraft:hay_block[axis=x]
setblock ~16 ~0 ~6 minecraft:oak_planks
fill ~16 ~0 ~7 ~17 ~0 ~7 minecraft:spruce_planks
setblock ~16 ~0 ~8 minecraft:dark_oak_planks
setblock ~16 ~0 ~10 minecraft:spruce_planks
setblock ~16 ~0 ~11 minecraft:hay_block[axis=x]
setblock ~16 ~0 ~13 minecraft:cobblestone
setblock ~16 ~0 ~14 minecraft:coarse_dirt
fill ~16 ~0 ~15 ~19 ~0 ~15 minecraft:smooth_stone
fill ~16 ~0 ~16 ~16 ~0 ~18 minecraft:andesite
fill ~16 ~0 ~19 ~16 ~0 ~21 minecraft:stone_bricks
setblock ~16 ~0 ~22 minecraft:andesite
setblock ~16 ~0 ~26 minecraft:cracked_stone_bricks
setblock ~16 ~0 ~28 minecraft:cracked_stone_bricks
fill ~16 ~0 ~31 ~17 ~0 ~32 minecraft:cobblestone
fill ~16 ~0 ~33 ~19 ~0 ~33 minecraft:smooth_stone
setblock ~16 ~0 ~34 minecraft:cobblestone
setblock ~16 ~0 ~35 minecraft:stone
fill ~16 ~0 ~36 ~17 ~0 ~36 minecraft:cobblestone
setblock ~17 ~0 ~0 minecraft:spruce_planks
setblock ~17 ~0 ~2 minecraft:spruce_planks
fill ~17 ~0 ~3 ~17 ~0 ~5 minecraft:oak_planks
setblock ~17 ~0 ~6 minecraft:hay_block[axis=x]
setblock ~17 ~0 ~8 minecraft:oak_planks
setblock ~17 ~0 ~9 minecraft:spruce_planks
fill ~17 ~0 ~10 ~17 ~0 ~11 minecraft:oak_planks
fill ~17 ~0 ~12 ~18 ~0 ~12 minecraft:gravel
setblock ~17 ~0 ~13 minecraft:mossy_cobblestone
setblock ~17 ~0 ~14 minecraft:cobblestone
fill ~17 ~0 ~16 ~19 ~0 ~16 minecraft:stone_bricks
fill ~17 ~0 ~17 ~17 ~0 ~18 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~19 minecraft:andesite
fill ~17 ~0 ~20 ~17 ~0 ~21 minecraft:stone_bricks
setblock ~17 ~0 ~22 minecraft:mossy_stone_bricks
setblock ~17 ~0 ~23 minecraft:stone_bricks
setblock ~17 ~0 ~24 minecraft:mossy_stone_bricks
fill ~17 ~0 ~25 ~17 ~0 ~27 minecraft:stone_bricks
setblock ~17 ~0 ~28 minecraft:mossy_stone_bricks
fill ~17 ~0 ~29 ~18 ~0 ~29 minecraft:cracked_stone_bricks
fill ~17 ~0 ~30 ~18 ~0 ~30 minecraft:stone_bricks
setblock ~17 ~0 ~34 minecraft:mossy_cobblestone
fill ~17 ~0 ~35 ~18 ~0 ~35 minecraft:cobblestone
fill ~18 ~0 ~0 ~18 ~0 ~2 minecraft:hay_block[axis=x]
setblock ~18 ~0 ~3 minecraft:spruce_planks
setblock ~18 ~0 ~4 minecraft:oak_planks
setblock ~18 ~0 ~5 minecraft:hay_block[axis=x]
fill ~18 ~0 ~6 ~18 ~0 ~10 minecraft:oak_planks
setblock ~18 ~0 ~11 minecraft:spruce_planks
setblock ~18 ~0 ~13 minecraft:andesite
setblock ~18 ~0 ~14 minecraft:gravel
setblock ~18 ~0 ~17 minecraft:mossy_stone_bricks
fill ~18 ~0 ~18 ~18 ~0 ~20 minecraft:cracked_stone_bricks
fill ~18 ~0 ~21 ~19 ~0 ~21 minecraft:stone_bricks
fill ~18 ~0 ~22 ~18 ~0 ~23 minecraft:andesite
fill ~18 ~0 ~24 ~19 ~0 ~24 minecraft:stone_bricks
fill ~18 ~0 ~25 ~19 ~0 ~25 minecraft:cracked_stone_bricks
setblock ~18 ~0 ~26 minecraft:stone_bricks
fill ~18 ~0 ~27 ~19 ~0 ~27 minecraft:cobblestone
setblock ~18 ~0 ~28 minecraft:stone_bricks
fill ~18 ~0 ~31 ~19 ~0 ~31 minecraft:stone_bricks
setblock ~18 ~0 ~32 minecraft:stone_bricks
setblock ~18 ~0 ~34 minecraft:andesite
setblock ~18 ~0 ~36 minecraft:andesite
setblock ~19 ~0 ~0 minecraft:spruce_planks
setblock ~19 ~0 ~1 minecraft:oak_planks
setblock ~19 ~0 ~2 minecraft:spruce_planks
setblock ~19 ~0 ~3 minecraft:hay_block[axis=x]
fill ~19 ~0 ~4 ~19 ~0 ~6 minecraft:spruce_planks
fill ~19 ~0 ~7 ~20 ~0 ~7 minecraft:oak_planks
fill ~19 ~0 ~8 ~19 ~0 ~9 minecraft:oak_planks
fill ~19 ~0 ~10 ~19 ~0 ~11 minecraft:spruce_planks
fill ~19 ~0 ~12 ~19 ~0 ~14 minecraft:coarse_dirt
setblock ~19 ~0 ~17 minecraft:cracked_stone_bricks
setblock ~19 ~0 ~18 minecraft:stone_bricks
setblock ~19 ~0 ~19 minecraft:andesite
setblock ~19 ~0 ~20 minecraft:mossy_stone_bricks
fill ~19 ~0 ~22 ~20 ~0 ~22 minecraft:stone_bricks
setblock ~19 ~0 ~23 minecraft:cobblestone
setblock ~19 ~0 ~26 minecraft:cracked_stone_bricks
fill ~19 ~0 ~28 ~20 ~0 ~28 minecraft:cobblestone
setblock ~19 ~0 ~29 minecraft:stone_bricks
setblock ~19 ~0 ~30 minecraft:mossy_stone_bricks
setblock ~19 ~0 ~32 minecraft:cracked_stone_bricks
fill ~19 ~0 ~34 ~20 ~0 ~34 minecraft:cobblestone
setblock ~19 ~0 ~35 minecraft:andesite
setblock ~19 ~0 ~36 minecraft:stone
setblock ~20 ~0 ~0 minecraft:oak_planks
fill ~20 ~0 ~1 ~22 ~0 ~1 minecraft:spruce_planks
fill ~20 ~0 ~2 ~22 ~0 ~2 minecraft:oak_planks
setblock ~20 ~0 ~3 minecraft:oak_planks
setblock ~20 ~0 ~4 minecraft:hay_block[axis=x]
fill ~20 ~0 ~5 ~20 ~0 ~6 minecraft:spruce_planks
setblock ~20 ~0 ~8 minecraft:hay_block[axis=x]
setblock ~20 ~0 ~9 minecraft:oak_planks
setblock ~20 ~0 ~10 minecraft:dark_oak_planks
setblock ~20 ~0 ~11 minecraft:oak_planks
setblock ~20 ~0 ~12 minecraft:mossy_cobblestone
setblock ~20 ~0 ~13 minecraft:cobblestone
setblock ~20 ~0 ~14 minecraft:coarse_dirt
setblock ~20 ~0 ~15 minecraft:gravel
fill ~20 ~0 ~16 ~21 ~0 ~16 minecraft:smooth_stone
fill ~20 ~0 ~17 ~20 ~0 ~19 minecraft:stone_bricks
fill ~20 ~0 ~20 ~20 ~0 ~21 minecraft:cobblestone
setblock ~20 ~0 ~23 minecraft:cracked_stone_bricks
fill ~20 ~0 ~24 ~21 ~0 ~24 minecraft:andesite
setblock ~20 ~0 ~25 minecraft:mossy_stone_bricks
setblock ~20 ~0 ~26 minecraft:stone_bricks
setblock ~20 ~0 ~27 minecraft:mossy_stone_bricks
setblock ~20 ~0 ~29 minecraft:cracked_stone_bricks
fill ~20 ~0 ~30 ~20 ~0 ~31 minecraft:stone_bricks
setblock ~20 ~0 ~32 minecraft:smooth_stone
fill ~20 ~0 ~33 ~21 ~0 ~33 minecraft:cobblestone
setblock ~20 ~0 ~35 minecraft:packed_mud
fill ~20 ~0 ~36 ~21 ~0 ~36 minecraft:cobblestone
setblock ~21 ~0 ~0 minecraft:dark_oak_planks
setblock ~21 ~0 ~3 minecraft:spruce_planks
fill ~21 ~0 ~4 ~21 ~0 ~5 minecraft:oak_planks
setblock ~21 ~0 ~6 minecraft:dark_oak_planks
setblock ~21 ~0 ~7 minecraft:hay_block[axis=x]
fill ~21 ~0 ~8 ~21 ~0 ~11 minecraft:oak_planks
setblock ~21 ~0 ~12 minecraft:gravel
fill ~21 ~0 ~13 ~23 ~0 ~14 minecraft:coarse_dirt
setblock ~21 ~0 ~15 minecraft:mossy_cobblestone
fill ~21 ~0 ~17 ~22 ~0 ~17 minecraft:smooth_stone
setblock ~21 ~0 ~18 minecraft:mossy_stone_bricks
fill ~21 ~0 ~19 ~21 ~0 ~22 minecraft:stone_bricks
setblock ~21 ~0 ~23 minecraft:mossy_stone_bricks
setblock ~21 ~0 ~25 minecraft:stone_bricks
setblock ~21 ~0 ~26 minecraft:andesite
fill ~21 ~0 ~27 ~22 ~0 ~27 minecraft:stone_bricks
setblock ~21 ~0 ~28 minecraft:cracked_stone_bricks
setblock ~21 ~0 ~29 minecraft:stone_bricks
setblock ~21 ~0 ~30 minecraft:mossy_stone_bricks
fill ~21 ~0 ~31 ~21 ~0 ~32 minecraft:smooth_stone
fill ~21 ~0 ~34 ~21 ~0 ~35 minecraft:packed_mud
fill ~22 ~0 ~0 ~25 ~0 ~0 minecraft:oak_planks
fill ~22 ~0 ~3 ~22 ~0 ~4 minecraft:oak_planks
setblock ~22 ~0 ~5 minecraft:spruce_planks
setblock ~22 ~0 ~6 minecraft:hay_block[axis=x]
setblock ~22 ~0 ~7 minecraft:spruce_planks
setblock ~22 ~0 ~8 minecraft:dark_oak_planks
fill ~22 ~0 ~9 ~23 ~0 ~9 minecraft:oak_planks
setblock ~22 ~0 ~10 minecraft:hay_block[axis=x]
setblock ~22 ~0 ~11 minecraft:oak_planks
setblock ~22 ~0 ~12 minecraft:packed_mud
fill ~22 ~0 ~15 ~23 ~0 ~15 minecraft:coarse_dirt
setblock ~22 ~0 ~16 minecraft:packed_mud
fill ~22 ~0 ~18 ~23 ~0 ~18 minecraft:smooth_stone
setblock ~22 ~0 ~19 minecraft:cobblestone
setblock ~22 ~0 ~20 minecraft:stone_bricks
setblock ~22 ~0 ~21 minecraft:mossy_stone_bricks
fill ~22 ~0 ~22 ~23 ~0 ~22 minecraft:stone_bricks
setblock ~22 ~0 ~23 minecraft:stone_bricks
setblock ~22 ~0 ~24 minecraft:cobblestone
setblock ~22 ~0 ~25 minecraft:cracked_stone_bricks
setblock ~22 ~0 ~26 minecraft:cobblestone
setblock ~22 ~0 ~28 minecraft:andesite
setblock ~22 ~0 ~29 minecraft:mossy_stone_bricks
fill ~22 ~0 ~30 ~22 ~0 ~31 minecraft:smooth_stone
fill ~22 ~0 ~32 ~23 ~0 ~32 minecraft:coarse_dirt
setblock ~22 ~0 ~33 minecraft:coarse_dirt
setblock ~22 ~0 ~34 minecraft:gravel
fill ~22 ~0 ~35 ~22 ~0 ~36 minecraft:coarse_dirt
fill ~23 ~0 ~1 ~26 ~0 ~1 minecraft:oak_planks
fill ~23 ~0 ~2 ~23 ~0 ~3 minecraft:dark_oak_planks
setblock ~23 ~0 ~4 minecraft:spruce_planks
setblock ~23 ~0 ~5 minecraft:dark_oak_planks
fill ~23 ~0 ~6 ~23 ~0 ~8 minecraft:spruce_planks
fill ~23 ~0 ~10 ~23 ~0 ~11 minecraft:oak_planks
setblock ~23 ~0 ~12 minecraft:cobblestone
setblock ~23 ~0 ~16 minecraft:gravel
fill ~23 ~0 ~17 ~24 ~0 ~17 minecraft:cobblestone
setblock ~23 ~0 ~19 minecraft:smooth_stone
setblock ~23 ~0 ~20 minecraft:cracked_stone_bricks
setblock ~23 ~0 ~21 minecraft:cobblestone
setblock ~23 ~0 ~23 minecraft:cracked_stone_bricks
fill ~23 ~0 ~24 ~24 ~0 ~24 minecraft:stone_bricks
setblock ~23 ~0 ~25 minecraft:mossy_stone_bricks
setblock ~23 ~0 ~26 minecraft:stone_bricks
setblock ~23 ~0 ~27 minecraft:cracked_stone_bricks
setblock ~23 ~0 ~28 minecraft:mossy_stone_bricks
fill ~23 ~0 ~29 ~23 ~0 ~30 minecraft:smooth_stone
setblock ~23 ~0 ~31 minecraft:gravel
setblock ~23 ~0 ~33 minecraft:packed_mud
fill ~23 ~0 ~34 ~25 ~0 ~34 minecraft:coarse_dirt
setblock ~23 ~0 ~35 minecraft:cobblestone
setblock ~23 ~0 ~36 minecraft:gravel
setblock ~24 ~0 ~2 minecraft:hay_block[axis=x]
setblock ~24 ~0 ~3 minecraft:spruce_planks
setblock ~24 ~0 ~4 minecraft:dark_oak_planks
setblock ~24 ~0 ~5 minecraft:hay_block[axis=x]
setblock ~24 ~0 ~6 minecraft:dark_oak_planks
fill ~24 ~0 ~7 ~24 ~0 ~9 minecraft:oak_planks
fill ~24 ~0 ~10 ~24 ~0 ~11 minecraft:spruce_planks
setblock ~24 ~0 ~12 minecraft:gravel
fill ~24 ~0 ~13 ~24 ~0 ~14 minecraft:cobblestone
setblock ~24 ~0 ~15 minecraft:packed_mud
setblock ~24 ~0 ~16 minecraft:andesite
setblock ~24 ~0 ~18 minecraft:gravel
fill ~24 ~0 ~19 ~25 ~0 ~19 minecraft:cobblestone
fill ~24 ~0 ~20 ~24 ~0 ~23 minecraft:smooth_stone
fill ~24 ~0 ~25 ~24 ~0 ~28 minecraft:smooth_stone
fill ~24 ~0 ~29 ~24 ~0 ~31 minecraft:coarse_dirt
setblock ~24 ~0 ~32 minecraft:mossy_cobblestone
setblock ~24 ~0 ~33 minecraft:cobblestone
setblock ~24 ~0 ~35 minecraft:coarse_dirt
setblock ~24 ~0 ~36 minecraft:cobblestone
fill ~25 ~0 ~2 ~25 ~0 ~4 minecraft:oak_planks
fill ~25 ~0 ~5 ~25 ~0 ~6 minecraft:dark_oak_planks
setblock ~25 ~0 ~7 minecraft:spruce_planks
fill ~25 ~0 ~8 ~25 ~0 ~10 minecraft:oak_planks
fill ~25 ~0 ~11 ~26 ~0 ~11 minecraft:spruce_planks
setblock ~25 ~0 ~12 minecraft:coarse_dirt
fill ~25 ~0 ~13 ~25 ~0 ~14 minecraft:packed_mud
setblock ~25 ~0 ~15 minecraft:coarse_dirt
setblock ~25 ~0 ~16 minecraft:cobblestone
fill ~25 ~0 ~17 ~26 ~0 ~17 minecraft:coarse_dirt
fill ~25 ~0 ~18 ~27 ~0 ~18 minecraft:cobblestone
setblock ~25 ~0 ~20 minecraft:packed_mud
fill ~25 ~0 ~21 ~25 ~0 ~22 minecraft:coarse_dirt
setblock ~25 ~0 ~23 minecraft:cobblestone
setblock ~25 ~0 ~24 minecraft:smooth_stone
setblock ~25 ~0 ~25 minecraft:coarse_dirt
setblock ~25 ~0 ~26 minecraft:andesite
setblock ~25 ~0 ~27 minecraft:packed_mud
setblock ~25 ~0 ~28 minecraft:gravel
setblock ~25 ~0 ~29 minecraft:packed_mud
setblock ~25 ~0 ~30 minecraft:mossy_cobblestone
setblock ~25 ~0 ~31 minecraft:gravel
setblock ~25 ~0 ~32 minecraft:cobblestone
fill ~25 ~0 ~33 ~26 ~0 ~33 minecraft:gravel
setblock ~25 ~0 ~35 minecraft:mossy_cobblestone
setblock ~25 ~0 ~36 minecraft:andesite
setblock ~26 ~0 ~0 minecraft:spruce_planks
setblock ~26 ~0 ~2 minecraft:hay_block[axis=x]
setblock ~26 ~0 ~3 minecraft:spruce_planks
setblock ~26 ~0 ~4 minecraft:hay_block[axis=x]
setblock ~26 ~0 ~5 minecraft:oak_planks
setblock ~26 ~0 ~6 minecraft:spruce_planks
fill ~26 ~0 ~7 ~26 ~0 ~10 minecraft:oak_planks
setblock ~26 ~0 ~12 minecraft:packed_mud
fill ~26 ~0 ~13 ~26 ~0 ~14 minecraft:coarse_dirt
setblock ~26 ~0 ~15 minecraft:mossy_cobblestone
setblock ~26 ~0 ~16 minecraft:gravel
setblock ~26 ~0 ~19 minecraft:coarse_dirt
setblock ~26 ~0 ~20 minecraft:andesite
setblock ~26 ~0 ~21 minecraft:packed_mud
setblock ~26 ~0 ~22 minecraft:mossy_cobblestone
setblock ~26 ~0 ~23 minecraft:packed_mud
setblock ~26 ~0 ~24 minecraft:coarse_dirt
setblock ~26 ~0 ~25 minecraft:gravel
setblock ~26 ~0 ~26 minecraft:packed_mud
fill ~26 ~0 ~27 ~26 ~0 ~28 minecraft:coarse_dirt
fill ~26 ~0 ~29 ~26 ~0 ~30 minecraft:cobblestone
fill ~26 ~0 ~31 ~26 ~0 ~32 minecraft:coarse_dirt
setblock ~26 ~0 ~34 minecraft:packed_mud
setblock ~26 ~0 ~35 minecraft:coarse_dirt
setblock ~26 ~0 ~36 minecraft:cobblestone
fill ~27 ~0 ~0 ~30 ~0 ~0 minecraft:oak_planks
setblock ~27 ~0 ~1 minecraft:spruce_planks
fill ~27 ~0 ~2 ~28 ~0 ~2 minecraft:dark_oak_planks
setblock ~27 ~0 ~3 minecraft:hay_block[axis=x]
setblock ~27 ~0 ~4 minecraft:oak_planks
setblock ~27 ~0 ~5 minecraft:spruce_planks
setblock ~27 ~0 ~6 minecraft:oak_planks
setblock ~27 ~0 ~7 minecraft:spruce_planks
setblock ~27 ~0 ~8 minecraft:oak_planks
setblock ~27 ~0 ~9 minecraft:dark_oak_planks
setblock ~27 ~0 ~10 minecraft:spruce_planks
setblock ~27 ~0 ~11 minecraft:oak_planks
setblock ~27 ~0 ~12 minecraft:coarse_dirt
setblock ~27 ~0 ~13 minecraft:packed_mud
setblock ~27 ~0 ~14 minecraft:coarse_dirt
setblock ~27 ~0 ~15 minecraft:gravel
setblock ~27 ~0 ~16 minecraft:cobblestone
setblock ~27 ~0 ~17 minecraft:packed_mud
fill ~27 ~0 ~19 ~28 ~0 ~19 minecraft:gravel
setblock ~27 ~0 ~20 minecraft:packed_mud
setblock ~27 ~0 ~21 minecraft:cobblestone
setblock ~27 ~0 ~22 minecraft:andesite
setblock ~27 ~0 ~23 minecraft:coarse_dirt
setblock ~27 ~0 ~24 minecraft:cobblestone
setblock ~27 ~0 ~25 minecraft:packed_mud
setblock ~27 ~0 ~26 minecraft:coarse_dirt
setblock ~27 ~0 ~27 minecraft:andesite
setblock ~27 ~0 ~28 minecraft:gravel
fill ~27 ~0 ~29 ~27 ~0 ~30 minecraft:coarse_dirt
setblock ~27 ~0 ~31 minecraft:packed_mud
setblock ~27 ~0 ~32 minecraft:coarse_dirt
setblock ~27 ~0 ~33 minecraft:packed_mud
setblock ~27 ~0 ~34 minecraft:mossy_cobblestone
setblock ~27 ~0 ~35 minecraft:andesite
fill ~27 ~0 ~36 ~28 ~0 ~36 minecraft:coarse_dirt
setblock ~28 ~0 ~1 minecraft:oak_planks
fill ~28 ~0 ~3 ~30 ~0 ~3 minecraft:oak_planks
setblock ~28 ~0 ~4 minecraft:hay_block[axis=x]
setblock ~28 ~0 ~5 minecraft:oak_planks
setblock ~28 ~0 ~6 minecraft:spruce_planks
fill ~28 ~0 ~7 ~29 ~0 ~7 minecraft:dark_oak_planks
setblock ~28 ~0 ~8 minecraft:dark_oak_planks
fill ~28 ~0 ~9 ~28 ~0 ~11 minecraft:spruce_planks
setblock ~28 ~0 ~12 minecraft:gravel
setblock ~28 ~0 ~13 minecraft:cobblestone
setblock ~28 ~0 ~14 minecraft:gravel
setblock ~28 ~0 ~15 minecraft:packed_mud
fill ~28 ~0 ~16 ~28 ~0 ~17 minecraft:gravel
setblock ~28 ~0 ~18 minecraft:andesite
fill ~28 ~0 ~20 ~28 ~0 ~23 minecraft:cobblestone
fill ~28 ~0 ~24 ~29 ~0 ~24 minecraft:coarse_dirt
setblock ~28 ~0 ~25 minecraft:coarse_dirt
setblock ~28 ~0 ~26 minecraft:gravel
setblock ~28 ~0 ~27 minecraft:cobblestone
fill ~28 ~0 ~28 ~28 ~0 ~29 minecraft:mossy_cobblestone
setblock ~28 ~0 ~30 minecraft:gravel
fill ~28 ~0 ~31 ~30 ~0 ~31 minecraft:coarse_dirt
setblock ~28 ~0 ~32 minecraft:gravel
setblock ~28 ~0 ~33 minecraft:cobblestone
setblock ~28 ~0 ~34 minecraft:gravel
fill ~28 ~0 ~35 ~29 ~0 ~35 minecraft:coarse_dirt
setblock ~29 ~0 ~1 minecraft:hay_block[axis=x]
setblock ~29 ~0 ~2 minecraft:spruce_planks
setblock ~29 ~0 ~4 minecraft:oak_planks
setblock ~29 ~0 ~5 minecraft:dark_oak_planks
setblock ~29 ~0 ~6 minecraft:hay_block[axis=x]
fill ~29 ~0 ~8 ~29 ~0 ~9 minecraft:oak_planks
setblock ~29 ~0 ~10 minecraft:dark_oak_planks
fill ~29 ~0 ~11 ~30 ~0 ~11 minecraft:oak_planks
setblock ~29 ~0 ~12 minecraft:cobblestone
setblock ~29 ~0 ~13 minecraft:packed_mud
fill ~29 ~0 ~14 ~29 ~0 ~15 minecraft:coarse_dirt
setblock ~29 ~0 ~16 minecraft:andesite
fill ~29 ~0 ~17 ~29 ~0 ~18 minecraft:packed_mud
setblock ~29 ~0 ~19 minecraft:cobblestone
setblock ~29 ~0 ~20 minecraft:coarse_dirt
setblock ~29 ~0 ~21 minecraft:mossy_cobblestone
setblock ~29 ~0 ~22 minecraft:gravel
setblock ~29 ~0 ~23 minecraft:mossy_cobblestone
setblock ~29 ~0 ~25 minecraft:cobblestone
fill ~29 ~0 ~26 ~30 ~0 ~26 minecraft:coarse_dirt
setblock ~29 ~0 ~27 minecraft:mossy_cobblestone
setblock ~29 ~0 ~28 minecraft:gravel
setblock ~29 ~0 ~29 minecraft:cobblestone
setblock ~29 ~0 ~30 minecraft:packed_mud
setblock ~29 ~0 ~32 minecraft:cobblestone
setblock ~29 ~0 ~33 minecraft:coarse_dirt
setblock ~29 ~0 ~34 minecraft:mossy_cobblestone
setblock ~29 ~0 ~36 minecraft:mossy_cobblestone
setblock ~30 ~0 ~1 minecraft:oak_planks
setblock ~30 ~0 ~2 minecraft:dark_oak_planks
setblock ~30 ~0 ~4 minecraft:spruce_planks
fill ~30 ~0 ~5 ~30 ~0 ~7 minecraft:oak_planks
setblock ~30 ~0 ~8 minecraft:spruce_planks
fill ~30 ~0 ~9 ~30 ~0 ~10 minecraft:dark_oak_planks
setblock ~30 ~0 ~12 minecraft:coarse_dirt
setblock ~30 ~0 ~13 minecraft:cobblestone
setblock ~30 ~0 ~14 minecraft:mossy_cobblestone
setblock ~30 ~0 ~15 minecraft:gravel
fill ~30 ~0 ~16 ~30 ~0 ~17 minecraft:coarse_dirt
setblock ~30 ~0 ~18 minecraft:packed_mud
fill ~30 ~0 ~19 ~30 ~0 ~20 minecraft:gravel
fill ~30 ~0 ~21 ~30 ~0 ~22 minecraft:coarse_dirt
setblock ~30 ~0 ~23 minecraft:cobblestone
fill ~30 ~0 ~24 ~30 ~0 ~25 minecraft:packed_mud
fill ~30 ~0 ~27 ~30 ~0 ~29 minecraft:cobblestone
setblock ~30 ~0 ~30 minecraft:gravel
setblock ~30 ~0 ~32 minecraft:coarse_dirt
setblock ~30 ~0 ~33 minecraft:andesite
setblock ~30 ~0 ~34 minecraft:coarse_dirt
setblock ~30 ~0 ~35 minecraft:packed_mud
setblock ~30 ~0 ~36 minecraft:andesite

# the storehouse walls (y 1..3): a plinth of cobble and moss at the foot (y 1), weathered spruce boards, mud brick and oak patches between the posts (y 2) and a plate of dark oak logs (y 3)
fill ~0 ~1 ~1 ~0 ~1 ~4 minecraft:cobblestone
fill ~0 ~1 ~6 ~0 ~1 ~7 minecraft:cobblestone
setblock ~0 ~1 ~8 minecraft:mossy_cobblestone
setblock ~0 ~1 ~9 minecraft:cobbled_deepslate
setblock ~0 ~1 ~10 minecraft:mossy_cobblestone
setblock ~1 ~1 ~0 minecraft:andesite
fill ~1 ~1 ~11 ~2 ~1 ~11 minecraft:cobblestone
fill ~2 ~1 ~0 ~3 ~1 ~0 minecraft:cobblestone
setblock ~3 ~1 ~11 minecraft:mossy_cobblestone
setblock ~4 ~1 ~0 minecraft:cobbled_deepslate
setblock ~4 ~1 ~11 minecraft:cobblestone
fill ~6 ~1 ~0 ~9 ~1 ~0 minecraft:cobblestone
fill ~6 ~1 ~11 ~9 ~1 ~11 minecraft:cobblestone
fill ~11 ~1 ~0 ~12 ~1 ~0 minecraft:cobblestone
fill ~11 ~1 ~11 ~12 ~1 ~11 minecraft:cobblestone
setblock ~13 ~1 ~0 minecraft:cobbled_deepslate
setblock ~14 ~1 ~0 minecraft:cobblestone
setblock ~16 ~1 ~0 minecraft:andesite
fill ~17 ~1 ~0 ~18 ~1 ~0 minecraft:cobblestone
setblock ~18 ~1 ~11 minecraft:mossy_cobblestone
setblock ~19 ~1 ~0 minecraft:cobbled_deepslate
setblock ~19 ~1 ~11 minecraft:cobblestone
setblock ~20 ~1 ~11 minecraft:andesite
fill ~21 ~1 ~0 ~24 ~1 ~0 minecraft:cobblestone
fill ~22 ~1 ~11 ~25 ~1 ~11 minecraft:cobblestone
setblock ~26 ~1 ~0 minecraft:cobblestone
setblock ~26 ~1 ~11 minecraft:mossy_cobblestone
setblock ~27 ~1 ~0 minecraft:andesite
fill ~27 ~1 ~11 ~28 ~1 ~11 minecraft:cobblestone
setblock ~28 ~1 ~0 minecraft:cobblestone
setblock ~29 ~1 ~0 minecraft:mossy_cobblestone
setblock ~30 ~1 ~1 minecraft:mossy_cobblestone
fill ~30 ~1 ~2 ~30 ~1 ~3 minecraft:cobblestone
setblock ~30 ~1 ~4 minecraft:stone
fill ~30 ~1 ~6 ~30 ~1 ~9 minecraft:cobblestone
setblock ~30 ~1 ~10 minecraft:stone
setblock ~0 ~2 ~1 minecraft:mud_bricks
setblock ~0 ~2 ~2 minecraft:spruce_planks
setblock ~0 ~2 ~4 minecraft:mud_bricks
fill ~0 ~2 ~6 ~0 ~2 ~7 minecraft:oak_planks
setblock ~0 ~2 ~9 minecraft:oak_planks
setblock ~0 ~2 ~10 minecraft:stripped_spruce_log[axis=y]
setblock ~1 ~2 ~0 minecraft:stripped_spruce_log[axis=y]
fill ~1 ~2 ~11 ~2 ~2 ~11 minecraft:spruce_planks
setblock ~2 ~2 ~0 minecraft:mud_bricks
setblock ~3 ~2 ~0 minecraft:stripped_spruce_log[axis=y]
setblock ~4 ~2 ~11 minecraft:spruce_planks
fill ~6 ~2 ~0 ~7 ~2 ~0 minecraft:oak_planks
setblock ~6 ~2 ~11 minecraft:spruce_planks
setblock ~8 ~2 ~0 minecraft:stripped_spruce_log[axis=y]
fill ~8 ~2 ~11 ~9 ~2 ~11 minecraft:oak_planks
setblock ~9 ~2 ~0 minecraft:spruce_planks
setblock ~11 ~2 ~0 minecraft:spruce_planks
setblock ~11 ~2 ~11 minecraft:mud_bricks
setblock ~12 ~2 ~0 minecraft:stripped_spruce_log[axis=y]
setblock ~12 ~2 ~11 minecraft:spruce_planks
fill ~13 ~2 ~0 ~14 ~2 ~0 minecraft:spruce_planks
fill ~16 ~2 ~0 ~17 ~2 ~0 minecraft:spruce_planks
setblock ~18 ~2 ~0 minecraft:oak_planks
setblock ~18 ~2 ~11 minecraft:spruce_planks
setblock ~19 ~2 ~0 minecraft:stripped_spruce_log[axis=y]
setblock ~20 ~2 ~11 minecraft:spruce_planks
fill ~21 ~2 ~0 ~24 ~2 ~0 minecraft:spruce_planks
fill ~22 ~2 ~11 ~23 ~2 ~11 minecraft:spruce_planks
setblock ~24 ~2 ~11 minecraft:oak_planks
fill ~26 ~2 ~11 ~27 ~2 ~11 minecraft:spruce_planks
setblock ~27 ~2 ~0 minecraft:mud_bricks
fill ~28 ~2 ~0 ~29 ~2 ~0 minecraft:spruce_planks
setblock ~28 ~2 ~11 minecraft:mud_bricks
setblock ~30 ~2 ~1 minecraft:mud_bricks
setblock ~30 ~2 ~2 minecraft:spruce_planks
setblock ~30 ~2 ~4 minecraft:spruce_planks
setblock ~30 ~2 ~6 minecraft:oak_planks
setblock ~30 ~2 ~7 minecraft:dark_oak_planks
setblock ~30 ~2 ~9 minecraft:stripped_spruce_log[axis=y]
setblock ~30 ~2 ~10 minecraft:dark_oak_planks
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

# oak posts at the corners, the door jambs and between the vents
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
fill ~14 ~3 ~11 ~16 ~3 ~11 minecraft:dark_oak_log[axis=x]

# vents: slats of oak fence in the infill, the front wall, the back wall and the gables
setblock ~0 ~2 ~3 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~0 ~2 ~8 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~3 ~2 ~11 minecraft:oak_fence[north=false,east=true,south=true,west=true]
setblock ~4 ~2 ~0 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~7 ~2 ~11 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~10 ~2 ~0 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~19 ~2 ~11 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~20 ~2 ~0 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~25 ~2 ~11 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~26 ~2 ~0 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~30 ~2 ~3 minecraft:oak_fence[north=true,east=false,south=true,west=false]
setblock ~30 ~2 ~8 minecraft:oak_fence[north=true,east=false,south=true,west=false]

# the hay roof: bales of hay in half-block steps, a dark oak stair at the eave, rotten patches of moss and dried kelp, three holes that show the rafters
fill ~0 ~4 ~0 ~30 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~4 ~1 ~1 ~4 ~1 minecraft:dried_kelp_block
setblock ~0 ~4 ~10 minecraft:hay_block[axis=x]
fill ~0 ~4 ~11 ~21 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~1 ~4 ~10 ~4 ~4 ~10 minecraft:dried_kelp_block
setblock ~2 ~4 ~1 minecraft:hay_block[axis=x]
setblock ~3 ~4 ~1 minecraft:hay_block[axis=y]
fill ~4 ~4 ~1 ~8 ~4 ~1 minecraft:dried_kelp_block
fill ~5 ~4 ~10 ~8 ~4 ~10 minecraft:hay_block[axis=x]
setblock ~9 ~4 ~1 minecraft:hay_block[axis=y]
fill ~9 ~4 ~10 ~10 ~4 ~10 minecraft:dried_kelp_block
setblock ~10 ~4 ~1 minecraft:dried_kelp_block
fill ~11 ~4 ~1 ~12 ~4 ~1 minecraft:hay_block[axis=x]
setblock ~11 ~4 ~10 minecraft:moss_block
setblock ~12 ~4 ~10 minecraft:hay_block[axis=x]
setblock ~13 ~4 ~1 minecraft:hay_block[axis=y]
fill ~13 ~4 ~10 ~14 ~4 ~10 minecraft:hay_block[axis=y]
setblock ~14 ~4 ~1 minecraft:dried_kelp_block
setblock ~15 ~4 ~1 minecraft:hay_block[axis=y]
fill ~15 ~4 ~10 ~18 ~4 ~10 minecraft:hay_block[axis=x]
fill ~16 ~4 ~1 ~20 ~4 ~1 minecraft:hay_block[axis=x]
fill ~19 ~4 ~10 ~20 ~4 ~10 minecraft:hay_block[axis=y]
setblock ~21 ~4 ~1 minecraft:dark_oak_planks
setblock ~21 ~4 ~10 minecraft:hay_block[axis=x]
fill ~22 ~4 ~1 ~23 ~4 ~1 minecraft:dried_kelp_block
setblock ~25 ~4 ~1 minecraft:moss_block
fill ~26 ~4 ~1 ~29 ~4 ~1 minecraft:hay_block[axis=x]
setblock ~29 ~4 ~10 minecraft:hay_block[axis=y]
fill ~29 ~4 ~11 ~30 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~30 ~4 ~1 minecraft:hay_block[axis=y]
setblock ~30 ~4 ~10 minecraft:hay_block[axis=x]
fill ~0 ~5 ~2 ~2 ~5 ~2 minecraft:hay_block[axis=x]
fill ~0 ~5 ~3 ~1 ~5 ~3 minecraft:hay_block[axis=x]
fill ~0 ~5 ~8 ~2 ~5 ~9 minecraft:dried_kelp_block
setblock ~2 ~5 ~3 minecraft:dark_oak_planks
fill ~3 ~5 ~2 ~3 ~5 ~3 minecraft:spruce_planks
fill ~3 ~5 ~8 ~9 ~5 ~8 minecraft:hay_block[axis=x]
setblock ~3 ~5 ~9 minecraft:hay_block[axis=y]
fill ~4 ~5 ~2 ~4 ~5 ~3 minecraft:hay_block[axis=x]
fill ~4 ~5 ~9 ~9 ~5 ~9 minecraft:hay_block[axis=x]
setblock ~5 ~5 ~2 minecraft:dried_kelp_block
setblock ~5 ~5 ~3 minecraft:hay_block[axis=x]
fill ~7 ~5 ~2 ~8 ~5 ~3 minecraft:dried_kelp_block
fill ~9 ~5 ~2 ~11 ~5 ~2 minecraft:hay_block[axis=x]
setblock ~9 ~5 ~3 minecraft:hay_block[axis=y]
fill ~10 ~5 ~3 ~11 ~5 ~3 minecraft:hay_block[axis=x]
setblock ~10 ~5 ~8 minecraft:moss_block
setblock ~10 ~5 ~9 minecraft:dried_kelp_block
fill ~11 ~5 ~8 ~13 ~5 ~9 minecraft:hay_block[axis=x]
setblock ~12 ~5 ~2 minecraft:moss_block
fill ~13 ~5 ~2 ~14 ~5 ~2 minecraft:dried_kelp_block
setblock ~13 ~5 ~3 minecraft:dried_kelp_block
setblock ~14 ~5 ~3 minecraft:spruce_planks
fill ~14 ~5 ~8 ~15 ~5 ~8 minecraft:dried_kelp_block
fill ~14 ~5 ~9 ~15 ~5 ~9 minecraft:hay_block[axis=x]
setblock ~15 ~5 ~2 minecraft:spruce_planks
setblock ~15 ~5 ~3 minecraft:dark_oak_planks
fill ~16 ~5 ~2 ~19 ~5 ~2 minecraft:hay_block[axis=x]
setblock ~16 ~5 ~3 minecraft:dried_kelp_block
fill ~16 ~5 ~8 ~17 ~5 ~9 minecraft:moss_block
fill ~17 ~5 ~3 ~19 ~5 ~3 minecraft:hay_block[axis=x]
fill ~18 ~5 ~8 ~21 ~5 ~8 minecraft:hay_block[axis=x]
setblock ~18 ~5 ~9 minecraft:dried_kelp_block
setblock ~19 ~5 ~9 minecraft:hay_block[axis=x]
fill ~20 ~5 ~2 ~20 ~5 ~3 minecraft:moss_block
setblock ~20 ~5 ~9 minecraft:hay_block[axis=y]
fill ~21 ~5 ~2 ~21 ~5 ~3 minecraft:spruce_planks
setblock ~21 ~5 ~9 minecraft:hay_block[axis=x]
setblock ~22 ~5 ~2 minecraft:hay_block[axis=y]
setblock ~22 ~5 ~3 minecraft:spruce_planks
fill ~23 ~5 ~2 ~23 ~5 ~3 minecraft:dried_kelp_block
fill ~24 ~5 ~3 ~29 ~5 ~3 minecraft:dried_kelp_block
fill ~25 ~5 ~2 ~27 ~5 ~2 minecraft:hay_block[axis=x]
fill ~28 ~5 ~2 ~30 ~5 ~2 minecraft:dried_kelp_block
fill ~29 ~5 ~8 ~30 ~5 ~8 minecraft:hay_block[axis=x]
setblock ~29 ~5 ~9 minecraft:hay_block[axis=x]
setblock ~30 ~5 ~3 minecraft:spruce_planks
setblock ~30 ~5 ~9 minecraft:spruce_planks
setblock ~0 ~6 ~4 minecraft:dried_kelp_block
fill ~0 ~6 ~5 ~0 ~6 ~6 minecraft:hay_block[axis=x]
setblock ~0 ~6 ~7 minecraft:hay_block[axis=y]
fill ~1 ~6 ~4 ~5 ~6 ~5 minecraft:hay_block[axis=x]
setblock ~1 ~6 ~6 minecraft:hay_block[axis=x]
fill ~1 ~6 ~7 ~2 ~6 ~7 minecraft:dried_kelp_block
setblock ~2 ~6 ~6 minecraft:moss_block
setblock ~3 ~6 ~6 minecraft:dried_kelp_block
setblock ~3 ~6 ~7 minecraft:hay_block[axis=x]
fill ~4 ~6 ~6 ~5 ~6 ~7 minecraft:hay_block[axis=x]
fill ~6 ~6 ~4 ~8 ~6 ~5 minecraft:dried_kelp_block
setblock ~6 ~6 ~6 minecraft:hay_block[axis=y]
fill ~6 ~6 ~7 ~8 ~6 ~7 minecraft:hay_block[axis=x]
fill ~7 ~6 ~6 ~8 ~6 ~6 minecraft:dried_kelp_block
fill ~9 ~6 ~4 ~11 ~6 ~4 minecraft:hay_block[axis=x]
setblock ~9 ~6 ~5 minecraft:moss_block
fill ~9 ~6 ~6 ~10 ~6 ~7 minecraft:hay_block[axis=x]
setblock ~10 ~6 ~5 minecraft:dried_kelp_block
fill ~11 ~6 ~5 ~11 ~6 ~7 minecraft:hay_block[axis=x]
setblock ~12 ~6 ~5 minecraft:moss_block
fill ~12 ~6 ~6 ~12 ~6 ~7 minecraft:hay_block[axis=x]
setblock ~13 ~6 ~4 minecraft:moss_block
fill ~13 ~6 ~5 ~14 ~6 ~7 minecraft:hay_block[axis=x]
setblock ~14 ~6 ~4 minecraft:hay_block[axis=y]
fill ~15 ~6 ~4 ~18 ~6 ~6 minecraft:hay_block[axis=x]
setblock ~15 ~6 ~7 minecraft:dried_kelp_block
setblock ~16 ~6 ~7 minecraft:moss_block
fill ~17 ~6 ~7 ~18 ~6 ~7 minecraft:dried_kelp_block
fill ~19 ~6 ~4 ~20 ~6 ~5 minecraft:moss_block
fill ~19 ~6 ~6 ~20 ~6 ~7 minecraft:hay_block[axis=x]
setblock ~21 ~6 ~4 minecraft:spruce_planks
setblock ~21 ~6 ~5 minecraft:dark_oak_planks
setblock ~21 ~6 ~6 minecraft:spruce_planks
setblock ~21 ~6 ~7 minecraft:hay_block[axis=x]
fill ~22 ~6 ~4 ~24 ~6 ~4 minecraft:moss_block
fill ~22 ~6 ~5 ~27 ~6 ~5 minecraft:dried_kelp_block
fill ~22 ~6 ~6 ~23 ~6 ~6 minecraft:hay_block[axis=x]
fill ~24 ~6 ~6 ~27 ~6 ~6 minecraft:dried_kelp_block
setblock ~25 ~6 ~4 minecraft:hay_block[axis=x]
setblock ~26 ~6 ~4 minecraft:hay_block[axis=y]
setblock ~27 ~6 ~4 minecraft:hay_block[axis=x]
fill ~28 ~6 ~4 ~29 ~6 ~5 minecraft:dried_kelp_block
setblock ~28 ~6 ~6 minecraft:hay_block[axis=x]
fill ~29 ~6 ~6 ~29 ~6 ~7 minecraft:dried_kelp_block
setblock ~30 ~6 ~4 minecraft:hay_block[axis=x]
setblock ~30 ~6 ~5 minecraft:hay_block[axis=y]
fill ~30 ~6 ~6 ~30 ~6 ~7 minecraft:hay_block[axis=x]

# the gable ends under the roof (x 0 and x 30)
fill ~0 ~4 ~2 ~0 ~4 ~4 minecraft:spruce_planks
setblock ~0 ~4 ~5 minecraft:oak_planks
fill ~0 ~4 ~6 ~0 ~5 ~7 minecraft:spruce_planks
setblock ~0 ~4 ~8 minecraft:oak_planks
setblock ~0 ~4 ~9 minecraft:spruce_planks
fill ~30 ~4 ~2 ~30 ~4 ~3 minecraft:spruce_planks
fill ~30 ~4 ~4 ~30 ~5 ~4 minecraft:dark_oak_planks
setblock ~30 ~4 ~5 minecraft:oak_planks
fill ~30 ~4 ~6 ~30 ~4 ~7 minecraft:spruce_planks
setblock ~30 ~4 ~8 minecraft:oak_planks
setblock ~30 ~4 ~9 minecraft:dark_oak_planks
fill ~0 ~5 ~4 ~0 ~5 ~5 minecraft:spruce_planks
setblock ~30 ~5 ~5 minecraft:dark_oak_planks
setblock ~30 ~5 ~6 minecraft:oak_planks
setblock ~30 ~5 ~7 minecraft:spruce_planks

# the hay roof: bales of hay in half-block steps, a dark oak stair at the eave, rotten patches of moss and dried kelp, three holes that show the rafters: holes
setblock ~24 ~3 ~1 minecraft:dark_oak_log[axis=z]
setblock ~6 ~4 ~2 minecraft:dark_oak_log[axis=z]
setblock ~12 ~4 ~3 minecraft:dark_oak_log[axis=z]
setblock ~24 ~4 ~1 minecraft:air
fill ~6 ~5 ~2 ~6 ~5 ~3 minecraft:air
setblock ~12 ~5 ~3 minecraft:air
setblock ~24 ~5 ~2 minecraft:air
setblock ~12 ~6 ~4 minecraft:air

# the lookout: the roof cut away over x 22..28, z 7..11, the wall plate of the front wall raised to y 4 and a deck of planks at y 5; beams under it
fill ~22 ~1 ~7 ~22 ~3 ~7 minecraft:oak_log[axis=y]
fill ~22 ~1 ~10 ~22 ~3 ~10 minecraft:oak_log[axis=y]
fill ~28 ~1 ~7 ~28 ~3 ~7 minecraft:oak_log[axis=y]
fill ~28 ~1 ~10 ~28 ~3 ~10 minecraft:oak_log[axis=y]
fill ~22 ~4 ~7 ~28 ~4 ~7 minecraft:air
fill ~22 ~4 ~8 ~28 ~4 ~8 minecraft:dark_oak_log[axis=x]
fill ~22 ~4 ~9 ~28 ~4 ~9 minecraft:air
fill ~22 ~4 ~10 ~28 ~4 ~10 minecraft:dark_oak_log[axis=x]
setblock ~22 ~4 ~11 minecraft:oak_planks
fill ~23 ~4 ~11 ~25 ~4 ~11 minecraft:spruce_planks
fill ~26 ~4 ~11 ~27 ~5 ~11 minecraft:oak_planks
fill ~28 ~4 ~11 ~28 ~5 ~11 minecraft:spruce_planks
setblock ~22 ~5 ~7 minecraft:dark_oak_planks
fill ~22 ~5 ~8 ~22 ~5 ~10 minecraft:oak_planks
setblock ~22 ~5 ~11 minecraft:dark_oak_planks
setblock ~23 ~5 ~7 minecraft:oak_planks
setblock ~23 ~5 ~8 minecraft:spruce_planks
fill ~23 ~5 ~9 ~23 ~5 ~10 minecraft:oak_planks
setblock ~23 ~5 ~11 minecraft:spruce_planks
setblock ~24 ~5 ~7 minecraft:spruce_planks
fill ~24 ~5 ~8 ~26 ~5 ~8 minecraft:oak_planks
fill ~24 ~5 ~9 ~24 ~5 ~10 minecraft:dark_oak_planks
setblock ~24 ~5 ~11 minecraft:oak_planks
setblock ~25 ~5 ~7 minecraft:dark_oak_planks
setblock ~25 ~5 ~9 minecraft:spruce_planks
setblock ~25 ~5 ~10 minecraft:oak_planks
setblock ~25 ~5 ~11 minecraft:dark_oak_planks
fill ~26 ~5 ~7 ~27 ~5 ~7 minecraft:oak_planks
setblock ~26 ~5 ~9 minecraft:dark_oak_planks
setblock ~26 ~5 ~10 minecraft:spruce_planks
setblock ~27 ~5 ~8 minecraft:spruce_planks
fill ~27 ~5 ~9 ~28 ~5 ~10 minecraft:oak_planks
fill ~28 ~5 ~7 ~28 ~5 ~8 minecraft:spruce_planks
setblock ~22 ~6 ~8 minecraft:air
setblock ~22 ~6 ~11 minecraft:air
fill ~23 ~6 ~7 ~27 ~6 ~10 minecraft:air

# the parapet of bottom slabs at y 6 on the open sides (south and east, the west side from the second cell; the entry from the ladder at (22, 6, 11) stays open), posts at the corners with a lantern on the south-east one
setblock ~22 ~6 ~7 minecraft:dark_oak_log[axis=y]
fill ~22 ~6 ~9 ~22 ~6 ~10 minecraft:oak_slab[type=bottom]
fill ~23 ~6 ~11 ~27 ~6 ~11 minecraft:oak_slab[type=bottom]
setblock ~28 ~6 ~7 minecraft:dark_oak_log[axis=y]
fill ~28 ~6 ~8 ~28 ~6 ~10 minecraft:oak_slab[type=bottom]
setblock ~28 ~6 ~11 minecraft:dark_oak_log[axis=y]

# the ladder up the front wall at x 22 from the yard to the deck (y 1..5)
fill ~22 ~1 ~12 ~22 ~5 ~12 minecraft:ladder[facing=south]

# the yard walls (y 1..2): a cobble footing and a skin of weathered boards, patched
fill ~0 ~1 ~12 ~0 ~1 ~13 minecraft:cobblestone
setblock ~0 ~1 ~14 minecraft:stone
setblock ~0 ~1 ~15 minecraft:mossy_cobblestone
setblock ~0 ~1 ~16 minecraft:cobblestone
setblock ~0 ~1 ~17 minecraft:stone
setblock ~0 ~1 ~18 minecraft:mossy_cobblestone
setblock ~0 ~1 ~19 minecraft:cobbled_deepslate
setblock ~0 ~1 ~20 minecraft:cobblestone
setblock ~0 ~1 ~21 minecraft:andesite
fill ~0 ~1 ~22 ~0 ~1 ~23 minecraft:cobblestone
fill ~0 ~1 ~24 ~0 ~1 ~25 minecraft:mossy_cobblestone
setblock ~0 ~1 ~26 minecraft:cobbled_deepslate
setblock ~0 ~1 ~27 minecraft:mossy_cobblestone
setblock ~0 ~1 ~28 minecraft:stone
setblock ~0 ~1 ~29 minecraft:cobblestone
setblock ~0 ~1 ~30 minecraft:mossy_cobblestone
setblock ~0 ~1 ~31 minecraft:cobblestone
fill ~0 ~1 ~32 ~0 ~1 ~34 minecraft:mossy_cobblestone
fill ~0 ~1 ~35 ~0 ~1 ~36 minecraft:cobblestone
setblock ~1 ~1 ~36 minecraft:cobbled_deepslate
fill ~2 ~1 ~36 ~3 ~1 ~36 minecraft:mossy_cobblestone
setblock ~4 ~1 ~36 minecraft:cobblestone
setblock ~5 ~1 ~36 minecraft:mossy_cobblestone
setblock ~6 ~1 ~36 minecraft:cobblestone
setblock ~7 ~1 ~36 minecraft:stone
fill ~8 ~1 ~36 ~9 ~1 ~36 minecraft:cobblestone
setblock ~21 ~1 ~36 minecraft:mossy_cobblestone
fill ~22 ~1 ~36 ~25 ~1 ~36 minecraft:cobblestone
setblock ~26 ~1 ~36 minecraft:andesite
fill ~27 ~1 ~36 ~30 ~1 ~36 minecraft:cobblestone
setblock ~30 ~1 ~12 minecraft:mossy_cobblestone
setblock ~30 ~1 ~13 minecraft:andesite
setblock ~30 ~1 ~14 minecraft:cobblestone
setblock ~30 ~1 ~15 minecraft:cobbled_deepslate
fill ~30 ~1 ~16 ~30 ~1 ~17 minecraft:mossy_cobblestone
setblock ~30 ~1 ~18 minecraft:cobblestone
setblock ~30 ~1 ~19 minecraft:mossy_cobblestone
setblock ~30 ~1 ~20 minecraft:cobblestone
setblock ~30 ~1 ~28 minecraft:mossy_cobblestone
fill ~30 ~1 ~29 ~30 ~1 ~31 minecraft:cobblestone
setblock ~30 ~1 ~32 minecraft:cobbled_deepslate
setblock ~30 ~1 ~33 minecraft:andesite
setblock ~30 ~1 ~34 minecraft:cobblestone
setblock ~30 ~1 ~35 minecraft:stone
fill ~0 ~2 ~12 ~0 ~2 ~13 minecraft:spruce_planks
setblock ~0 ~2 ~14 minecraft:oak_planks
setblock ~0 ~2 ~15 minecraft:spruce_planks
setblock ~0 ~2 ~16 minecraft:oak_planks
fill ~0 ~2 ~17 ~0 ~2 ~18 minecraft:dark_oak_planks
fill ~0 ~2 ~19 ~0 ~2 ~20 minecraft:oak_planks
fill ~0 ~2 ~21 ~0 ~2 ~22 minecraft:spruce_planks
setblock ~0 ~2 ~23 minecraft:oak_planks
setblock ~0 ~2 ~24 minecraft:spruce_planks
setblock ~0 ~2 ~25 minecraft:oak_planks
setblock ~0 ~2 ~26 minecraft:cobblestone
setblock ~0 ~2 ~27 minecraft:spruce_planks
setblock ~0 ~2 ~28 minecraft:oak_planks
setblock ~0 ~2 ~29 minecraft:spruce_planks
setblock ~0 ~2 ~30 minecraft:dark_oak_planks
fill ~0 ~2 ~31 ~0 ~2 ~32 minecraft:oak_planks
setblock ~0 ~2 ~33 minecraft:spruce_planks
setblock ~0 ~2 ~34 minecraft:dark_oak_planks
setblock ~0 ~2 ~35 minecraft:oak_planks
fill ~0 ~2 ~36 ~1 ~2 ~36 minecraft:spruce_planks
setblock ~2 ~2 ~36 minecraft:cobblestone
setblock ~3 ~2 ~36 minecraft:oak_planks
setblock ~4 ~2 ~36 minecraft:spruce_planks
fill ~5 ~2 ~36 ~7 ~2 ~36 minecraft:dark_oak_planks
fill ~8 ~2 ~36 ~9 ~2 ~36 minecraft:spruce_planks
fill ~21 ~2 ~36 ~22 ~2 ~36 minecraft:spruce_planks
setblock ~23 ~2 ~36 minecraft:cobblestone
setblock ~24 ~2 ~36 minecraft:oak_planks
fill ~25 ~2 ~36 ~26 ~2 ~36 minecraft:spruce_planks
setblock ~27 ~2 ~36 minecraft:mossy_cobblestone
setblock ~28 ~2 ~36 minecraft:spruce_planks
setblock ~29 ~2 ~36 minecraft:oak_planks
fill ~30 ~2 ~12 ~30 ~2 ~13 minecraft:spruce_planks
fill ~30 ~2 ~14 ~30 ~2 ~15 minecraft:mossy_cobblestone
setblock ~30 ~2 ~16 minecraft:spruce_planks
setblock ~30 ~2 ~17 minecraft:oak_planks
setblock ~30 ~2 ~18 minecraft:mossy_cobblestone
setblock ~30 ~2 ~19 minecraft:spruce_planks
setblock ~30 ~2 ~20 minecraft:oak_planks
setblock ~30 ~2 ~28 minecraft:spruce_planks
setblock ~30 ~2 ~29 minecraft:mossy_cobblestone
fill ~30 ~2 ~30 ~30 ~2 ~33 minecraft:spruce_planks
setblock ~30 ~2 ~34 minecraft:dark_oak_planks
setblock ~30 ~2 ~35 minecraft:oak_planks
setblock ~30 ~2 ~36 minecraft:spruce_planks

# the gate posts of the front gate (x 10 and 20, y 1..2) with the braziers on them, unlit campfires at (10, 3, 36) and (20, 3, 36)
setblock ~10 ~1 ~36 minecraft:oak_log[axis=y]
setblock ~20 ~1 ~36 minecraft:oak_log[axis=y]
setblock ~10 ~2 ~36 minecraft:blackstone
setblock ~20 ~2 ~36 minecraft:blackstone
setblock ~10 ~3 ~36 minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false]
setblock ~20 ~3 ~36 minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false]

# the side gate in the east wall (z 22..26): posts at z 21 and 27 four high and a beam over the opening at y 5, clear below
fill ~30 ~1 ~21 ~30 ~4 ~21 minecraft:oak_log[axis=y]
fill ~30 ~1 ~27 ~30 ~4 ~27 minecraft:oak_log[axis=y]
fill ~30 ~5 ~21 ~30 ~5 ~27 minecraft:dark_oak_log[axis=z]

# the hoist: a beam out of the eave over the east side of the door, a chain and a bale hanging on it
setblock ~18 ~2 ~13 minecraft:hay_block[axis=y]
fill ~18 ~3 ~13 ~18 ~4 ~13 minecraft:chain[axis=y]
setblock ~18 ~4 ~12 minecraft:dark_oak_stairs[facing=north,half=top]
fill ~18 ~5 ~11 ~18 ~5 ~13 minecraft:dark_oak_log[axis=z]

# the forge corner: a lean-to roof of slabs at y 3 against the west wall on posts
setblock ~4 ~1 ~13 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
fill ~4 ~1 ~17 ~4 ~2 ~17 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~21 ~4 ~2 ~21 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~4 ~2 ~13 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~1 ~3 ~13 ~4 ~3 ~21 minecraft:spruce_slab[type=bottom]

# the forge: anvil, grindstone, smithing table, furnace, cauldron, barrels
setblock ~1 ~1 ~14 minecraft:anvil[facing=east]
setblock ~1 ~1 ~16 minecraft:grindstone[face=floor,facing=east]
setblock ~1 ~1 ~18 minecraft:smithing_table
setblock ~2 ~1 ~21 minecraft:cauldron
setblock ~3 ~1 ~15 minecraft:barrel[facing=up,open=false]
setblock ~3 ~1 ~19 minecraft:barrel[facing=up,open=false]

# a weapon rack of blocks: stripped logs with pike heads of lightning rods and iron bars
setblock ~1 ~1 ~12 minecraft:stripped_spruce_log[axis=x]
setblock ~1 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~4 ~2 ~12 minecraft:iron_bars[north=true,east=false,south=false,west=true]

# the cots: a lean-to of slabs over four beds, chests and barrels between them
fill ~4 ~1 ~26 ~4 ~2 ~26 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~29 ~4 ~2 ~29 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~33 ~4 ~2 ~33 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~1 ~3 ~26 ~4 ~3 ~33 minecraft:spruce_slab[type=bottom]

# the beds, a chest and barrels
setblock ~1 ~1 ~27 minecraft:red_bed[facing=east,part=foot,occupied=false]
setblock ~1 ~1 ~29 minecraft:red_bed[facing=east,part=foot,occupied=false]
setblock ~1 ~1 ~31 minecraft:red_bed[facing=east,part=foot,occupied=false]
setblock ~2 ~1 ~27 minecraft:red_bed[facing=east,part=head,occupied=false]
setblock ~2 ~1 ~29 minecraft:red_bed[facing=east,part=head,occupied=false]
setblock ~2 ~1 ~31 minecraft:red_bed[facing=east,part=head,occupied=false]
setblock ~3 ~1 ~28 minecraft:barrel[facing=up,open=false]
setblock ~3 ~1 ~30 minecraft:barrel[facing=up,open=false]
setblock ~3 ~1 ~32 minecraft:barrel[facing=up,open=false]

# a cart: a plank bed on four fence legs with wheels of trapdoors, its load and a tongue
fill ~24 ~1 ~15 ~24 ~1 ~16 minecraft:spruce_trapdoor[facing=west,half=bottom,open=true]
setblock ~25 ~1 ~14 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~25 ~1 ~17 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~26 ~1 ~19 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~27 ~1 ~14 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~27 ~1 ~17 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~28 ~1 ~15 ~28 ~1 ~16 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
fill ~24 ~2 ~15 ~24 ~2 ~16 minecraft:spruce_trapdoor[facing=west,half=top,open=true]
fill ~25 ~2 ~14 ~27 ~2 ~17 minecraft:spruce_planks
setblock ~26 ~2 ~18 minecraft:spruce_stairs[facing=north,half=top]
fill ~28 ~2 ~15 ~28 ~2 ~16 minecraft:spruce_trapdoor[facing=east,half=top,open=true]
fill ~25 ~3 ~15 ~26 ~3 ~15 minecraft:hay_block[axis=x]
setblock ~26 ~3 ~16 minecraft:hay_block[axis=x]
setblock ~27 ~3 ~16 minecraft:barrel[facing=up,open=false]

# hay stacks in the south-west and south-east corners of the yard, the bales turned and some missing
fill ~5 ~1 ~34 ~5 ~1 ~35 minecraft:hay_block[axis=x]
fill ~6 ~1 ~34 ~6 ~1 ~35 minecraft:hay_block[axis=z]
fill ~7 ~1 ~34 ~7 ~1 ~35 minecraft:hay_block[axis=x]
fill ~8 ~1 ~34 ~8 ~1 ~35 minecraft:hay_block[axis=z]
fill ~9 ~1 ~34 ~9 ~1 ~35 minecraft:hay_block[axis=x]
fill ~27 ~1 ~28 ~27 ~1 ~33 minecraft:hay_block[axis=x]
fill ~28 ~1 ~28 ~28 ~1 ~33 minecraft:hay_block[axis=z]
fill ~29 ~1 ~28 ~29 ~1 ~33 minecraft:hay_block[axis=x]
fill ~6 ~2 ~34 ~6 ~2 ~35 minecraft:hay_block[axis=x]
fill ~7 ~2 ~34 ~7 ~2 ~35 minecraft:hay_block[axis=z]
fill ~8 ~2 ~34 ~8 ~2 ~35 minecraft:hay_block[axis=x]
fill ~9 ~2 ~34 ~9 ~2 ~35 minecraft:hay_block[axis=z]
fill ~27 ~2 ~28 ~27 ~2 ~33 minecraft:hay_block[axis=z]
fill ~28 ~2 ~28 ~28 ~2 ~29 minecraft:hay_block[axis=x]
fill ~28 ~2 ~31 ~28 ~2 ~33 minecraft:hay_block[axis=x]
setblock ~29 ~2 ~28 minecraft:hay_block[axis=z]
setblock ~29 ~2 ~33 minecraft:hay_block[axis=z]
setblock ~27 ~3 ~30 minecraft:hay_block[axis=z]
setblock ~28 ~3 ~31 minecraft:hay_block[axis=x]

# crates and barrels along the front wall of the storehouse and in the corners
fill ~1 ~1 ~23 ~1 ~2 ~23 minecraft:barrel[facing=up,open=false]
fill ~1 ~1 ~24 ~1 ~2 ~24 minecraft:spruce_planks
setblock ~2 ~1 ~12 minecraft:barrel[facing=up,open=false]
setblock ~2 ~1 ~23 minecraft:chest[facing=north,type=single,waterlogged=false]
setblock ~2 ~1 ~35 minecraft:barrel[facing=up,open=false]
setblock ~3 ~1 ~12 minecraft:spruce_planks
setblock ~3 ~1 ~35 minecraft:chest[facing=north,type=single,waterlogged=false]
setblock ~4 ~1 ~12 minecraft:barrel[facing=up,open=false]
setblock ~9 ~1 ~12 minecraft:chest[facing=south,type=single,waterlogged=false]
fill ~10 ~1 ~12 ~10 ~2 ~12 minecraft:barrel[facing=up,open=false]
fill ~10 ~1 ~35 ~10 ~2 ~35 minecraft:barrel[facing=up,open=false]
setblock ~19 ~1 ~12 minecraft:spruce_planks
setblock ~20 ~1 ~12 minecraft:barrel[facing=up,open=false]
setblock ~24 ~1 ~12 minecraft:barrel[facing=east,open=false]
setblock ~29 ~1 ~12 minecraft:chest[facing=north,type=single,waterlogged=false]
setblock ~2 ~2 ~12 minecraft:chest[facing=north,type=single,waterlogged=false]
setblock ~3 ~2 ~12 minecraft:barrel[facing=west,open=false]
setblock ~3 ~2 ~35 minecraft:barrel[facing=north,open=false]
setblock ~9 ~2 ~12 minecraft:barrel[facing=up,open=false]
setblock ~19 ~2 ~12 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~24 ~2 ~12 minecraft:chest[facing=east,type=single,waterlogged=false]
setblock ~29 ~2 ~12 minecraft:spruce_planks
setblock ~1 ~3 ~24 minecraft:spruce_trapdoor[facing=north,half=bottom,open=false]

# inside the storehouse: barrels, sacks of grain (hay and composters) and a lantern on a stand, seen through the door and the vents
fill ~3 ~1 ~2 ~4 ~1 ~2 minecraft:barrel[facing=up,open=false]
setblock ~3 ~1 ~3 minecraft:composter[level=2]
fill ~5 ~1 ~9 ~7 ~1 ~9 minecraft:hay_block[axis=x]
setblock ~6 ~1 ~2 minecraft:composter[level=3]
setblock ~8 ~1 ~9 minecraft:barrel[facing=up,open=false]
setblock ~9 ~1 ~9 minecraft:composter[level=0]
fill ~12 ~1 ~2 ~13 ~1 ~2 minecraft:barrel[facing=up,open=false]
setblock ~15 ~1 ~9 minecraft:barrel[facing=up,open=false]
fill ~17 ~1 ~2 ~18 ~1 ~2 minecraft:barrel[facing=up,open=false]
setblock ~19 ~1 ~3 minecraft:barrel[facing=up,open=false]
fill ~21 ~1 ~9 ~22 ~1 ~9 minecraft:hay_block[axis=x]
setblock ~24 ~1 ~3 minecraft:composter[level=3]
setblock ~15 ~2 ~9 minecraft:lantern[hanging=false]
