# size 31 9 37
# supply/hollowwood: supply house of Hollowwood, woodstore and walled yard: mossy cobble and spruce logs under a roof gone to moss and rafters, soul lanterns, cobweb, a herb bench, a woodpile, a broken cart, a cold forge, a lookout and the yard with the capture circle.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Shared shape of the five supply houses (spec 7.1, plan Task 8b): storehouse x 0..30, z 0..11; lookout deck y 5 over x 22..28, z 7..11 with the ladder at (22, 1..5, 12); yard walls y 1..2 along x 0, x 30, z 36; front gate x 11..19; side gate z 22..26;
# capture circle centre (15, 0, 24) radius 10, edge ring 9 to 10, plinth x 14..16 z 23..25; the flag column |x-15|, |z-24| <= 3, y 1..8 is structure void; the braziers (unlit campfires) at (10, 3, 36) and (20, 3, 36); nothing solid above y 6.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~1 ~1 ~1 ~16 ~1 ~1 minecraft:air
fill ~1 ~1 ~2 ~2 ~2 ~10 minecraft:air
fill ~1 ~1 ~13 ~1 ~2 ~20 minecraft:air
fill ~1 ~1 ~23 ~1 ~8 ~23 minecraft:air
fill ~1 ~1 ~25 ~1 ~2 ~27 minecraft:air
fill ~1 ~1 ~29 ~1 ~2 ~29 minecraft:air
fill ~1 ~1 ~31 ~1 ~2 ~31 minecraft:air
fill ~1 ~1 ~33 ~1 ~2 ~35 minecraft:air
fill ~2 ~1 ~12 ~5 ~8 ~12 minecraft:air
fill ~2 ~1 ~14 ~3 ~2 ~15 minecraft:air
fill ~2 ~1 ~16 ~2 ~2 ~28 minecraft:air
fill ~2 ~1 ~30 ~2 ~2 ~30 minecraft:air
fill ~2 ~1 ~32 ~2 ~2 ~32 minecraft:air
fill ~2 ~1 ~34 ~2 ~2 ~34 minecraft:air
fill ~3 ~1 ~4 ~3 ~3 ~10 minecraft:air
fill ~3 ~1 ~17 ~3 ~2 ~19 minecraft:air
fill ~3 ~1 ~21 ~3 ~2 ~33 minecraft:air
fill ~3 ~1 ~35 ~4 ~8 ~35 minecraft:air
fill ~4 ~1 ~3 ~14 ~3 ~8 minecraft:air
fill ~4 ~1 ~9 ~5 ~3 ~10 minecraft:air
fill ~4 ~1 ~13 ~10 ~8 ~13 minecraft:air
fill ~4 ~1 ~15 ~4 ~2 ~17 minecraft:air
fill ~4 ~1 ~19 ~4 ~2 ~21 minecraft:air
fill ~4 ~1 ~23 ~4 ~8 ~26 minecraft:air
fill ~4 ~1 ~28 ~4 ~2 ~29 minecraft:air
fill ~4 ~1 ~31 ~4 ~2 ~33 minecraft:air
fill ~5 ~1 ~2 ~16 ~3 ~2 minecraft:air
fill ~5 ~1 ~14 ~10 ~8 ~17 minecraft:air
fill ~5 ~1 ~18 ~5 ~8 ~34 minecraft:air
fill ~6 ~1 ~10 ~7 ~3 ~10 minecraft:air
fill ~6 ~1 ~19 ~6 ~8 ~35 minecraft:air
fill ~7 ~1 ~18 ~10 ~8 ~30 minecraft:air
fill ~7 ~1 ~32 ~7 ~8 ~35 minecraft:air
fill ~8 ~1 ~9 ~14 ~2 ~10 minecraft:air
fill ~8 ~1 ~31 ~10 ~8 ~34 minecraft:air
fill ~8 ~1 ~35 ~8 ~8 ~35 minecraft:air
fill ~10 ~1 ~35 ~10 ~8 ~35 minecraft:air
fill ~11 ~1 ~12 ~18 ~8 ~20 minecraft:air
fill ~11 ~1 ~21 ~11 ~8 ~36 minecraft:air
fill ~12 ~1 ~28 ~18 ~8 ~36 minecraft:air
fill ~15 ~1 ~4 ~15 ~4 ~8 minecraft:air
fill ~15 ~1 ~10 ~15 ~3 ~10 minecraft:air
fill ~16 ~1 ~3 ~17 ~2 ~10 minecraft:air
fill ~18 ~1 ~1 ~29 ~2 ~1 minecraft:air
fill ~18 ~1 ~2 ~25 ~4 ~2 minecraft:air
fill ~18 ~1 ~4 ~18 ~4 ~8 minecraft:air
fill ~19 ~1 ~3 ~21 ~2 ~10 minecraft:air
fill ~19 ~1 ~13 ~20 ~8 ~35 minecraft:air
fill ~19 ~1 ~36 ~19 ~8 ~36 minecraft:air
fill ~21 ~1 ~12 ~21 ~8 ~35 minecraft:air
fill ~22 ~1 ~4 ~22 ~5 ~6 minecraft:air
fill ~22 ~1 ~8 ~22 ~3 ~9 minecraft:air
fill ~22 ~1 ~13 ~22 ~8 ~35 minecraft:air
fill ~23 ~1 ~3 ~23 ~3 ~9 minecraft:air
fill ~23 ~1 ~12 ~23 ~8 ~35 minecraft:air
fill ~24 ~1 ~4 ~24 ~3 ~10 minecraft:air
fill ~24 ~1 ~13 ~28 ~8 ~13 minecraft:air
fill ~24 ~1 ~14 ~24 ~8 ~14 minecraft:air
fill ~24 ~1 ~17 ~24 ~8 ~29 minecraft:air
fill ~24 ~1 ~31 ~24 ~8 ~35 minecraft:air
fill ~25 ~1 ~3 ~26 ~3 ~10 minecraft:air
fill ~25 ~1 ~15 ~25 ~1 ~16 minecraft:air
fill ~25 ~1 ~18 ~25 ~8 ~35 minecraft:air
fill ~26 ~1 ~14 ~26 ~1 ~18 minecraft:air
fill ~26 ~1 ~20 ~26 ~8 ~35 minecraft:air
fill ~27 ~1 ~2 ~29 ~4 ~6 minecraft:air
fill ~27 ~1 ~7 ~27 ~3 ~10 minecraft:air
fill ~27 ~1 ~15 ~27 ~1 ~16 minecraft:air
fill ~27 ~1 ~18 ~27 ~8 ~27 minecraft:air
fill ~27 ~1 ~34 ~29 ~8 ~34 minecraft:air
fill ~27 ~1 ~35 ~27 ~8 ~35 minecraft:air
fill ~28 ~1 ~8 ~29 ~2 ~9 minecraft:air
fill ~28 ~1 ~14 ~29 ~8 ~14 minecraft:air
fill ~28 ~1 ~17 ~28 ~8 ~27 minecraft:air
fill ~29 ~1 ~10 ~29 ~3 ~10 minecraft:air
fill ~29 ~1 ~15 ~29 ~8 ~17 minecraft:air
fill ~29 ~1 ~20 ~29 ~8 ~27 minecraft:air
fill ~30 ~1 ~22 ~30 ~4 ~26 minecraft:air
fill ~1 ~2 ~21 ~1 ~2 ~22 minecraft:air
setblock ~1 ~2 ~28 minecraft:air
setblock ~1 ~2 ~30 minecraft:air
setblock ~1 ~2 ~32 minecraft:air
fill ~2 ~2 ~1 ~7 ~2 ~1 minecraft:air
setblock ~2 ~2 ~29 minecraft:air
setblock ~2 ~2 ~31 minecraft:air
setblock ~2 ~2 ~33 minecraft:air
fill ~3 ~2 ~2 ~4 ~4 ~2 minecraft:air
fill ~3 ~2 ~3 ~3 ~4 ~3 minecraft:air
setblock ~3 ~2 ~16 minecraft:air
setblock ~3 ~2 ~20 minecraft:air
fill ~6 ~2 ~9 ~7 ~4 ~9 minecraft:air
fill ~9 ~2 ~1 ~17 ~2 ~1 minecraft:air
fill ~15 ~2 ~3 ~15 ~4 ~3 minecraft:air
fill ~17 ~2 ~2 ~17 ~4 ~2 minecraft:air
fill ~18 ~2 ~3 ~18 ~4 ~3 minecraft:air
fill ~18 ~2 ~10 ~18 ~3 ~10 minecraft:air
fill ~22 ~2 ~3 ~22 ~4 ~3 minecraft:air
fill ~23 ~2 ~10 ~23 ~3 ~10 minecraft:air
fill ~24 ~2 ~3 ~24 ~4 ~3 minecraft:air
fill ~26 ~2 ~2 ~26 ~4 ~2 minecraft:air
fill ~26 ~2 ~19 ~26 ~8 ~19 minecraft:air
fill ~28 ~2 ~35 ~28 ~8 ~35 minecraft:air
fill ~29 ~2 ~7 ~29 ~5 ~7 minecraft:air
fill ~29 ~2 ~13 ~29 ~8 ~13 minecraft:air
fill ~29 ~2 ~18 ~29 ~8 ~19 minecraft:air
fill ~0 ~3 ~12 ~1 ~8 ~13 minecraft:air
fill ~0 ~3 ~23 ~0 ~8 ~25 minecraft:air
fill ~0 ~3 ~35 ~2 ~8 ~35 minecraft:air
fill ~1 ~3 ~1 ~6 ~3 ~1 minecraft:air
fill ~1 ~3 ~2 ~2 ~4 ~9 minecraft:air
setblock ~1 ~3 ~10 minecraft:air
fill ~1 ~3 ~25 ~1 ~8 ~26 minecraft:air
fill ~2 ~3 ~23 ~3 ~8 ~26 minecraft:air
fill ~3 ~3 ~13 ~3 ~8 ~13 minecraft:air
fill ~3 ~3 ~36 ~4 ~8 ~36 minecraft:air
fill ~6 ~3 ~36 ~6 ~8 ~36 minecraft:air
fill ~8 ~3 ~9 ~15 ~4 ~9 minecraft:air
fill ~8 ~3 ~10 ~9 ~3 ~10 minecraft:air
fill ~8 ~3 ~36 ~8 ~8 ~36 minecraft:air
setblock ~9 ~3 ~1 minecraft:air
fill ~9 ~3 ~35 ~9 ~8 ~36 minecraft:air
fill ~11 ~3 ~10 ~14 ~3 ~10 minecraft:air
fill ~14 ~3 ~1 ~18 ~3 ~1 minecraft:air
fill ~16 ~3 ~3 ~17 ~4 ~9 minecraft:air
setblock ~16 ~3 ~10 minecraft:air
fill ~18 ~3 ~9 ~18 ~4 ~9 minecraft:air
fill ~19 ~3 ~3 ~21 ~3 ~9 minecraft:air
fill ~20 ~3 ~1 ~24 ~3 ~1 minecraft:air
fill ~20 ~3 ~10 ~21 ~3 ~10 minecraft:air
fill ~20 ~3 ~12 ~20 ~8 ~12 minecraft:air
fill ~21 ~3 ~36 ~21 ~8 ~36 minecraft:air
fill ~24 ~3 ~12 ~28 ~8 ~12 minecraft:air
fill ~24 ~3 ~15 ~24 ~8 ~16 minecraft:air
fill ~25 ~3 ~14 ~27 ~8 ~14 minecraft:air
fill ~25 ~3 ~16 ~27 ~8 ~17 minecraft:air
fill ~25 ~3 ~36 ~26 ~8 ~36 minecraft:air
setblock ~26 ~3 ~1 minecraft:air
fill ~26 ~3 ~18 ~26 ~8 ~18 minecraft:air
setblock ~28 ~3 ~9 minecraft:air
fill ~28 ~3 ~15 ~28 ~8 ~16 minecraft:air
fill ~28 ~3 ~28 ~28 ~8 ~28 minecraft:air
fill ~28 ~3 ~33 ~28 ~8 ~33 minecraft:air
fill ~28 ~3 ~36 ~28 ~8 ~36 minecraft:air
setblock ~29 ~3 ~1 minecraft:air
fill ~29 ~3 ~8 ~29 ~4 ~9 minecraft:air
fill ~29 ~3 ~32 ~29 ~8 ~32 minecraft:air
fill ~29 ~3 ~35 ~30 ~8 ~36 minecraft:air
fill ~30 ~3 ~12 ~30 ~8 ~20 minecraft:air
fill ~30 ~3 ~28 ~30 ~8 ~29 minecraft:air
fill ~30 ~3 ~31 ~30 ~8 ~31 minecraft:air
fill ~30 ~3 ~33 ~30 ~8 ~33 minecraft:air
fill ~0 ~4 ~14 ~1 ~8 ~22 minecraft:air
fill ~0 ~4 ~26 ~0 ~8 ~34 minecraft:air
fill ~0 ~4 ~36 ~2 ~8 ~36 minecraft:air
fill ~1 ~4 ~24 ~1 ~8 ~24 minecraft:air
fill ~1 ~4 ~27 ~4 ~8 ~34 minecraft:air
fill ~2 ~4 ~13 ~2 ~8 ~22 minecraft:air
fill ~3 ~4 ~4 ~3 ~4 ~9 minecraft:air
fill ~3 ~4 ~14 ~4 ~8 ~22 minecraft:air
fill ~4 ~4 ~3 ~7 ~4 ~8 minecraft:air
fill ~4 ~4 ~9 ~5 ~4 ~9 minecraft:air
fill ~5 ~4 ~2 ~8 ~4 ~2 minecraft:air
fill ~5 ~4 ~35 ~5 ~8 ~36 minecraft:air
fill ~6 ~4 ~12 ~7 ~8 ~12 minecraft:air
fill ~7 ~4 ~36 ~7 ~8 ~36 minecraft:air
fill ~8 ~4 ~4 ~8 ~4 ~8 minecraft:air
fill ~9 ~4 ~3 ~9 ~4 ~8 minecraft:air
setblock ~10 ~4 ~2 minecraft:air
fill ~10 ~4 ~4 ~10 ~4 ~8 minecraft:air
fill ~10 ~4 ~12 ~10 ~8 ~12 minecraft:air
fill ~10 ~4 ~36 ~10 ~8 ~36 minecraft:air
fill ~11 ~4 ~3 ~11 ~4 ~8 minecraft:air
fill ~12 ~4 ~2 ~16 ~4 ~2 minecraft:air
fill ~12 ~4 ~4 ~12 ~4 ~8 minecraft:air
fill ~13 ~4 ~3 ~14 ~4 ~8 minecraft:air
fill ~19 ~4 ~4 ~19 ~4 ~9 minecraft:air
fill ~19 ~4 ~12 ~19 ~8 ~12 minecraft:air
fill ~20 ~4 ~3 ~21 ~4 ~9 minecraft:air
fill ~20 ~4 ~36 ~20 ~8 ~36 minecraft:air
fill ~22 ~4 ~36 ~24 ~8 ~36 minecraft:air
fill ~23 ~4 ~3 ~23 ~4 ~6 minecraft:air
fill ~24 ~4 ~4 ~24 ~5 ~6 minecraft:air
fill ~25 ~4 ~3 ~26 ~4 ~6 minecraft:air
fill ~25 ~4 ~15 ~25 ~8 ~15 minecraft:air
fill ~27 ~4 ~15 ~27 ~8 ~15 minecraft:air
fill ~27 ~4 ~28 ~27 ~8 ~29 minecraft:air
fill ~27 ~4 ~33 ~27 ~8 ~33 minecraft:air
fill ~27 ~4 ~36 ~27 ~8 ~36 minecraft:air
fill ~28 ~4 ~29 ~29 ~8 ~30 minecraft:air
fill ~28 ~4 ~31 ~28 ~8 ~32 minecraft:air
fill ~29 ~4 ~12 ~29 ~8 ~12 minecraft:air
fill ~30 ~4 ~30 ~30 ~8 ~30 minecraft:air
fill ~30 ~4 ~32 ~30 ~8 ~32 minecraft:air
fill ~30 ~4 ~34 ~30 ~8 ~34 minecraft:air
fill ~0 ~5 ~0 ~30 ~8 ~1 minecraft:air
fill ~0 ~5 ~10 ~21 ~8 ~11 minecraft:air
fill ~1 ~5 ~4 ~2 ~5 ~7 minecraft:air
fill ~3 ~5 ~5 ~3 ~5 ~7 minecraft:air
fill ~4 ~5 ~4 ~21 ~5 ~7 minecraft:air
fill ~6 ~5 ~18 ~6 ~8 ~18 minecraft:air
fill ~7 ~5 ~31 ~7 ~8 ~31 minecraft:air
fill ~8 ~5 ~12 ~9 ~8 ~12 minecraft:air
fill ~23 ~5 ~4 ~23 ~5 ~6 minecraft:air
fill ~24 ~5 ~30 ~24 ~8 ~30 minecraft:air
fill ~25 ~5 ~4 ~29 ~5 ~6 minecraft:air
fill ~26 ~5 ~15 ~26 ~8 ~15 minecraft:air
fill ~27 ~5 ~30 ~27 ~8 ~32 minecraft:air
fill ~29 ~5 ~10 ~30 ~8 ~11 minecraft:air
fill ~29 ~5 ~28 ~29 ~8 ~28 minecraft:air
fill ~29 ~5 ~31 ~29 ~8 ~31 minecraft:air
fill ~29 ~5 ~33 ~29 ~8 ~33 minecraft:air
fill ~0 ~6 ~2 ~30 ~8 ~3 minecraft:air
fill ~0 ~6 ~8 ~21 ~8 ~9 minecraft:air
fill ~22 ~6 ~12 ~22 ~8 ~12 minecraft:air
fill ~29 ~6 ~8 ~30 ~8 ~9 minecraft:air
fill ~30 ~6 ~21 ~30 ~8 ~27 minecraft:air
fill ~0 ~7 ~4 ~30 ~8 ~7 minecraft:air
fill ~22 ~7 ~8 ~28 ~8 ~11 minecraft:air

# the ground layer: deepslate tiles and moss on the storehouse floor, the paved capture circle (deepslate brick, a ring of mossy stone brick, a plinth), podzol, moss, coarse dirt and fallen cobble in the yard, a cobbled mouth at the gate
setblock ~0 ~0 ~0 minecraft:cracked_deepslate_tiles
fill ~0 ~0 ~1 ~2 ~0 ~1 minecraft:deepslate_tiles
setblock ~0 ~0 ~2 minecraft:cobbled_deepslate
setblock ~0 ~0 ~3 minecraft:cracked_deepslate_tiles
setblock ~0 ~0 ~4 minecraft:cobbled_deepslate
setblock ~0 ~0 ~5 minecraft:cracked_deepslate_tiles
fill ~0 ~0 ~6 ~0 ~0 ~7 minecraft:moss_block
fill ~0 ~0 ~8 ~1 ~0 ~8 minecraft:cracked_deepslate_tiles
setblock ~0 ~0 ~9 minecraft:polished_deepslate
setblock ~0 ~0 ~10 minecraft:cracked_deepslate_tiles
setblock ~0 ~0 ~11 minecraft:deepslate_tiles
fill ~0 ~0 ~12 ~1 ~0 ~12 minecraft:rooted_dirt
fill ~0 ~0 ~13 ~1 ~0 ~13 minecraft:moss_block
setblock ~0 ~0 ~14 minecraft:moss_block
setblock ~0 ~0 ~15 minecraft:coarse_dirt
setblock ~0 ~0 ~16 minecraft:moss_block
setblock ~0 ~0 ~17 minecraft:rooted_dirt
fill ~0 ~0 ~18 ~1 ~0 ~18 minecraft:moss_block
fill ~0 ~0 ~19 ~1 ~0 ~19 minecraft:podzol
setblock ~0 ~0 ~20 minecraft:podzol
setblock ~0 ~0 ~21 minecraft:rooted_dirt
setblock ~0 ~0 ~22 minecraft:coarse_dirt
setblock ~0 ~0 ~23 minecraft:mossy_cobblestone
fill ~0 ~0 ~24 ~0 ~0 ~28 minecraft:rooted_dirt
setblock ~0 ~0 ~29 minecraft:coarse_dirt
fill ~0 ~0 ~30 ~0 ~0 ~32 minecraft:rooted_dirt
setblock ~0 ~0 ~33 minecraft:moss_block
fill ~0 ~0 ~34 ~1 ~0 ~34 minecraft:podzol
fill ~0 ~0 ~35 ~1 ~0 ~35 minecraft:rooted_dirt
setblock ~0 ~0 ~36 minecraft:podzol
fill ~1 ~0 ~0 ~6 ~0 ~0 minecraft:deepslate_tiles
fill ~1 ~0 ~2 ~4 ~0 ~2 minecraft:deepslate_tiles
fill ~1 ~0 ~3 ~2 ~0 ~3 minecraft:moss_block
setblock ~1 ~0 ~4 minecraft:cracked_deepslate_tiles
setblock ~1 ~0 ~5 minecraft:cobbled_deepslate
setblock ~1 ~0 ~6 minecraft:polished_deepslate
setblock ~1 ~0 ~7 minecraft:cobbled_deepslate
setblock ~1 ~0 ~9 minecraft:cracked_deepslate_tiles
setblock ~1 ~0 ~10 minecraft:deepslate_tiles
setblock ~1 ~0 ~11 minecraft:cracked_deepslate_tiles
setblock ~1 ~0 ~14 minecraft:mossy_cobblestone
setblock ~1 ~0 ~15 minecraft:moss_block
setblock ~1 ~0 ~16 minecraft:podzol
setblock ~1 ~0 ~17 minecraft:coarse_dirt
setblock ~1 ~0 ~20 minecraft:mossy_cobblestone
fill ~1 ~0 ~21 ~1 ~0 ~22 minecraft:podzol
fill ~1 ~0 ~23 ~1 ~0 ~24 minecraft:moss_block
setblock ~1 ~0 ~25 minecraft:rooted_dirt
fill ~1 ~0 ~26 ~2 ~0 ~26 minecraft:podzol
setblock ~1 ~0 ~27 minecraft:coarse_dirt
setblock ~1 ~0 ~28 minecraft:mossy_cobblestone
fill ~1 ~0 ~29 ~1 ~0 ~30 minecraft:podzol
fill ~1 ~0 ~31 ~1 ~0 ~33 minecraft:rooted_dirt
setblock ~1 ~0 ~36 minecraft:moss_block
fill ~2 ~0 ~4 ~2 ~0 ~5 minecraft:deepslate_tiles
fill ~2 ~0 ~6 ~3 ~0 ~6 minecraft:cracked_deepslate_tiles
fill ~2 ~0 ~7 ~2 ~0 ~8 minecraft:deepslate_tiles
setblock ~2 ~0 ~9 minecraft:polished_deepslate
setblock ~2 ~0 ~10 minecraft:cracked_deepslate_tiles
setblock ~2 ~0 ~11 minecraft:cobbled_deepslate
setblock ~2 ~0 ~12 minecraft:coarse_dirt
setblock ~2 ~0 ~13 minecraft:podzol
setblock ~2 ~0 ~14 minecraft:moss_block
fill ~2 ~0 ~15 ~2 ~0 ~16 minecraft:rooted_dirt
setblock ~2 ~0 ~17 minecraft:moss_block
setblock ~2 ~0 ~18 minecraft:podzol
setblock ~2 ~0 ~19 minecraft:rooted_dirt
fill ~2 ~0 ~20 ~2 ~0 ~21 minecraft:podzol
setblock ~2 ~0 ~22 minecraft:coarse_dirt
setblock ~2 ~0 ~23 minecraft:rooted_dirt
fill ~2 ~0 ~24 ~2 ~0 ~25 minecraft:moss_block
fill ~2 ~0 ~27 ~4 ~0 ~27 minecraft:rooted_dirt
setblock ~2 ~0 ~28 minecraft:coarse_dirt
setblock ~2 ~0 ~29 minecraft:mossy_cobblestone
setblock ~2 ~0 ~30 minecraft:rooted_dirt
fill ~2 ~0 ~31 ~3 ~0 ~31 minecraft:coarse_dirt
setblock ~2 ~0 ~32 minecraft:rooted_dirt
fill ~2 ~0 ~33 ~2 ~0 ~34 minecraft:podzol
setblock ~2 ~0 ~35 minecraft:mossy_cobblestone
setblock ~2 ~0 ~36 minecraft:podzol
setblock ~3 ~0 ~1 minecraft:cobbled_deepslate
fill ~3 ~0 ~3 ~3 ~0 ~4 minecraft:cracked_deepslate_tiles
setblock ~3 ~0 ~5 minecraft:cobbled_deepslate
setblock ~3 ~0 ~7 minecraft:cobbled_deepslate
fill ~3 ~0 ~8 ~4 ~0 ~8 minecraft:deepslate_tiles
setblock ~3 ~0 ~9 minecraft:deepslate_tiles
fill ~3 ~0 ~10 ~4 ~0 ~10 minecraft:polished_deepslate
setblock ~3 ~0 ~11 minecraft:deepslate_tiles
setblock ~3 ~0 ~12 minecraft:podzol
setblock ~3 ~0 ~13 minecraft:moss_block
setblock ~3 ~0 ~14 minecraft:coarse_dirt
setblock ~3 ~0 ~15 minecraft:cobbled_deepslate
setblock ~3 ~0 ~16 minecraft:mossy_cobblestone
fill ~3 ~0 ~17 ~3 ~0 ~18 minecraft:podzol
setblock ~3 ~0 ~19 minecraft:moss_block
setblock ~3 ~0 ~20 minecraft:cobbled_deepslate
fill ~3 ~0 ~21 ~3 ~0 ~22 minecraft:rooted_dirt
fill ~3 ~0 ~23 ~4 ~0 ~23 minecraft:podzol
setblock ~3 ~0 ~24 minecraft:coarse_dirt
setblock ~3 ~0 ~25 minecraft:moss_block
setblock ~3 ~0 ~26 minecraft:coarse_dirt
fill ~3 ~0 ~28 ~5 ~0 ~28 minecraft:podzol
setblock ~3 ~0 ~29 minecraft:coarse_dirt
setblock ~3 ~0 ~30 minecraft:podzol
fill ~3 ~0 ~32 ~4 ~0 ~32 minecraft:podzol
setblock ~3 ~0 ~33 minecraft:mossy_cobblestone
fill ~3 ~0 ~34 ~5 ~0 ~34 minecraft:rooted_dirt
fill ~3 ~0 ~35 ~5 ~0 ~35 minecraft:podzol
setblock ~3 ~0 ~36 minecraft:rooted_dirt
fill ~4 ~0 ~1 ~5 ~0 ~1 minecraft:deepslate_tiles
fill ~4 ~0 ~3 ~4 ~0 ~4 minecraft:deepslate_tiles
setblock ~4 ~0 ~5 minecraft:cracked_deepslate_tiles
fill ~4 ~0 ~6 ~4 ~0 ~7 minecraft:polished_deepslate
setblock ~4 ~0 ~9 minecraft:cracked_deepslate_tiles
fill ~4 ~0 ~11 ~5 ~0 ~11 minecraft:cracked_deepslate_tiles
setblock ~4 ~0 ~12 minecraft:rooted_dirt
fill ~4 ~0 ~13 ~6 ~0 ~13 minecraft:podzol
fill ~4 ~0 ~14 ~4 ~0 ~15 minecraft:podzol
setblock ~4 ~0 ~16 minecraft:moss_block
setblock ~4 ~0 ~17 minecraft:coarse_dirt
setblock ~4 ~0 ~18 minecraft:mossy_cobblestone
fill ~4 ~0 ~19 ~4 ~0 ~21 minecraft:podzol
fill ~4 ~0 ~22 ~5 ~0 ~22 minecraft:packed_mud
setblock ~4 ~0 ~24 minecraft:mossy_cobblestone
setblock ~4 ~0 ~25 minecraft:coarse_dirt
setblock ~4 ~0 ~26 minecraft:cobbled_deepslate
setblock ~4 ~0 ~29 minecraft:mossy_cobblestone
fill ~4 ~0 ~30 ~4 ~0 ~31 minecraft:cobbled_deepslate
setblock ~4 ~0 ~33 minecraft:podzol
setblock ~4 ~0 ~36 minecraft:podzol
setblock ~5 ~0 ~2 minecraft:cobbled_deepslate
setblock ~5 ~0 ~3 minecraft:polished_deepslate
setblock ~5 ~0 ~4 minecraft:cracked_deepslate_tiles
fill ~5 ~0 ~5 ~5 ~0 ~6 minecraft:deepslate_tiles
setblock ~5 ~0 ~7 minecraft:moss_block
setblock ~5 ~0 ~8 minecraft:cracked_deepslate_tiles
setblock ~5 ~0 ~9 minecraft:moss_block
setblock ~5 ~0 ~10 minecraft:deepslate_tiles
setblock ~5 ~0 ~12 minecraft:mossy_cobblestone
setblock ~5 ~0 ~14 minecraft:mossy_cobblestone
setblock ~5 ~0 ~15 minecraft:podzol
setblock ~5 ~0 ~16 minecraft:packed_mud
setblock ~5 ~0 ~17 minecraft:mossy_cobblestone
fill ~5 ~0 ~18 ~6 ~0 ~18 minecraft:podzol
setblock ~5 ~0 ~19 minecraft:coarse_dirt
setblock ~5 ~0 ~20 minecraft:moss_block
setblock ~5 ~0 ~21 minecraft:coarse_dirt
setblock ~5 ~0 ~23 minecraft:packed_mud
setblock ~5 ~0 ~24 minecraft:mossy_stone_bricks
setblock ~5 ~0 ~25 minecraft:podzol
setblock ~5 ~0 ~26 minecraft:coarse_dirt
setblock ~5 ~0 ~27 minecraft:mossy_cobblestone
setblock ~5 ~0 ~29 minecraft:packed_mud
setblock ~5 ~0 ~30 minecraft:mossy_cobblestone
fill ~5 ~0 ~31 ~5 ~0 ~32 minecraft:podzol
setblock ~5 ~0 ~33 minecraft:coarse_dirt
setblock ~5 ~0 ~36 minecraft:mossy_cobblestone
setblock ~6 ~0 ~1 minecraft:cracked_deepslate_tiles
setblock ~6 ~0 ~2 minecraft:deepslate_tiles
fill ~6 ~0 ~3 ~6 ~0 ~4 minecraft:cobbled_deepslate
setblock ~6 ~0 ~5 minecraft:polished_deepslate
setblock ~6 ~0 ~6 minecraft:cracked_deepslate_tiles
fill ~6 ~0 ~7 ~7 ~0 ~11 minecraft:deepslate_tiles
fill ~6 ~0 ~12 ~7 ~0 ~12 minecraft:rooted_dirt
setblock ~6 ~0 ~14 minecraft:podzol
setblock ~6 ~0 ~15 minecraft:mossy_cobblestone
fill ~6 ~0 ~16 ~7 ~0 ~16 minecraft:podzol
setblock ~6 ~0 ~17 minecraft:coarse_dirt
setblock ~6 ~0 ~19 minecraft:packed_mud
fill ~6 ~0 ~20 ~6 ~0 ~23 minecraft:mossy_stone_bricks
setblock ~6 ~0 ~24 minecraft:cracked_deepslate_bricks
fill ~6 ~0 ~25 ~6 ~0 ~28 minecraft:mossy_stone_bricks
setblock ~6 ~0 ~29 minecraft:cobbled_deepslate
setblock ~6 ~0 ~30 minecraft:moss_block
setblock ~6 ~0 ~31 minecraft:rooted_dirt
fill ~6 ~0 ~32 ~6 ~0 ~33 minecraft:coarse_dirt
setblock ~6 ~0 ~34 minecraft:mossy_cobblestone
setblock ~6 ~0 ~35 minecraft:rooted_dirt
setblock ~6 ~0 ~36 minecraft:podzol
setblock ~7 ~0 ~0 minecraft:cracked_deepslate_tiles
setblock ~7 ~0 ~1 minecraft:polished_deepslate
setblock ~7 ~0 ~2 minecraft:cracked_deepslate_tiles
setblock ~7 ~0 ~3 minecraft:moss_block
setblock ~7 ~0 ~4 minecraft:polished_deepslate
setblock ~7 ~0 ~5 minecraft:cracked_deepslate_tiles
setblock ~7 ~0 ~6 minecraft:polished_deepslate
fill ~7 ~0 ~13 ~8 ~0 ~13 minecraft:rooted_dirt
fill ~7 ~0 ~14 ~8 ~0 ~14 minecraft:coarse_dirt
fill ~7 ~0 ~15 ~10 ~0 ~15 minecraft:podzol
setblock ~7 ~0 ~17 minecraft:podzol
fill ~7 ~0 ~18 ~8 ~0 ~18 minecraft:mossy_stone_bricks
setblock ~7 ~0 ~19 minecraft:mossy_stone_bricks
fill ~7 ~0 ~20 ~7 ~0 ~21 minecraft:deepslate_bricks
setblock ~7 ~0 ~22 minecraft:polished_deepslate
fill ~7 ~0 ~23 ~7 ~0 ~24 minecraft:deepslate_bricks
setblock ~7 ~0 ~25 minecraft:mossy_cobblestone
setblock ~7 ~0 ~26 minecraft:cobbled_deepslate
fill ~7 ~0 ~27 ~8 ~0 ~27 minecraft:deepslate_bricks
setblock ~7 ~0 ~28 minecraft:polished_deepslate
fill ~7 ~0 ~29 ~7 ~0 ~30 minecraft:mossy_stone_bricks
fill ~7 ~0 ~31 ~7 ~0 ~32 minecraft:coarse_dirt
setblock ~7 ~0 ~33 minecraft:podzol
setblock ~7 ~0 ~34 minecraft:moss_block
setblock ~7 ~0 ~35 minecraft:podzol
setblock ~7 ~0 ~36 minecraft:rooted_dirt
setblock ~8 ~0 ~0 minecraft:deepslate_tiles
setblock ~8 ~0 ~1 minecraft:cracked_deepslate_tiles
setblock ~8 ~0 ~2 minecraft:cobbled_deepslate
fill ~8 ~0 ~3 ~10 ~0 ~4 minecraft:deepslate_tiles
setblock ~8 ~0 ~5 minecraft:moss_block
setblock ~8 ~0 ~6 minecraft:cracked_deepslate_tiles
setblock ~8 ~0 ~7 minecraft:cobbled_deepslate
fill ~8 ~0 ~8 ~8 ~0 ~10 minecraft:deepslate_tiles
setblock ~8 ~0 ~11 minecraft:cracked_deepslate_tiles
setblock ~8 ~0 ~12 minecraft:podzol
setblock ~8 ~0 ~16 minecraft:cobbled_deepslate
fill ~8 ~0 ~17 ~9 ~0 ~17 minecraft:mossy_stone_bricks
fill ~8 ~0 ~19 ~8 ~0 ~25 minecraft:deepslate_bricks
setblock ~8 ~0 ~26 minecraft:polished_deepslate
fill ~8 ~0 ~28 ~8 ~0 ~29 minecraft:deepslate_bricks
fill ~8 ~0 ~30 ~8 ~0 ~31 minecraft:mossy_stone_bricks
setblock ~8 ~0 ~32 minecraft:packed_mud
setblock ~8 ~0 ~33 minecraft:coarse_dirt
fill ~8 ~0 ~34 ~9 ~0 ~35 minecraft:podzol
setblock ~8 ~0 ~36 minecraft:coarse_dirt
setblock ~9 ~0 ~0 minecraft:cracked_deepslate_tiles
setblock ~9 ~0 ~1 minecraft:cobbled_deepslate
fill ~9 ~0 ~2 ~10 ~0 ~2 minecraft:polished_deepslate
setblock ~9 ~0 ~5 minecraft:cracked_deepslate_tiles
setblock ~9 ~0 ~6 minecraft:cobbled_deepslate
fill ~9 ~0 ~7 ~9 ~0 ~9 minecraft:deepslate_tiles
setblock ~9 ~0 ~10 minecraft:cobbled_deepslate
setblock ~9 ~0 ~11 minecraft:deepslate_tiles
setblock ~9 ~0 ~12 minecraft:rooted_dirt
setblock ~9 ~0 ~13 minecraft:podzol
setblock ~9 ~0 ~14 minecraft:rooted_dirt
fill ~9 ~0 ~16 ~10 ~0 ~16 minecraft:mossy_stone_bricks
setblock ~9 ~0 ~18 minecraft:cobbled_deepslate
setblock ~9 ~0 ~19 minecraft:polished_deepslate
fill ~9 ~0 ~20 ~9 ~0 ~21 minecraft:deepslate_bricks
fill ~9 ~0 ~22 ~10 ~0 ~22 minecraft:cracked_deepslate_bricks
setblock ~9 ~0 ~23 minecraft:deepslate_bricks
setblock ~9 ~0 ~24 minecraft:cobbled_deepslate
fill ~9 ~0 ~25 ~10 ~0 ~25 minecraft:cracked_deepslate_bricks
fill ~9 ~0 ~26 ~10 ~0 ~27 minecraft:deepslate_bricks
setblock ~9 ~0 ~28 minecraft:mossy_cobblestone
setblock ~9 ~0 ~29 minecraft:deepslate_bricks
setblock ~9 ~0 ~30 minecraft:cobbled_deepslate
fill ~9 ~0 ~31 ~9 ~0 ~32 minecraft:mossy_stone_bricks
setblock ~9 ~0 ~33 minecraft:cobbled_deepslate
setblock ~9 ~0 ~36 minecraft:mossy_cobblestone
setblock ~10 ~0 ~0 minecraft:polished_deepslate
setblock ~10 ~0 ~1 minecraft:cracked_deepslate_tiles
fill ~10 ~0 ~5 ~10 ~0 ~6 minecraft:deepslate_tiles
setblock ~10 ~0 ~7 minecraft:cobbled_deepslate
fill ~10 ~0 ~8 ~11 ~0 ~8 minecraft:cracked_deepslate_tiles
setblock ~10 ~0 ~9 minecraft:cracked_deepslate_tiles
setblock ~10 ~0 ~10 minecraft:deepslate_tiles
setblock ~10 ~0 ~11 minecraft:cobbled_deepslate
setblock ~10 ~0 ~12 minecraft:podzol
fill ~10 ~0 ~13 ~11 ~0 ~13 minecraft:rooted_dirt
setblock ~10 ~0 ~14 minecraft:coarse_dirt
setblock ~10 ~0 ~17 minecraft:cobbled_deepslate
setblock ~10 ~0 ~18 minecraft:deepslate_bricks
fill ~10 ~0 ~19 ~11 ~0 ~19 minecraft:mossy_cobblestone
setblock ~10 ~0 ~20 minecraft:cracked_deepslate_bricks
setblock ~10 ~0 ~21 minecraft:mossy_cobblestone
setblock ~10 ~0 ~23 minecraft:cobbled_deepslate
fill ~10 ~0 ~24 ~11 ~0 ~24 minecraft:deepslate_bricks
setblock ~10 ~0 ~28 minecraft:deepslate_bricks
setblock ~10 ~0 ~29 minecraft:cobbled_deepslate
setblock ~10 ~0 ~30 minecraft:deepslate_bricks
setblock ~10 ~0 ~31 minecraft:cobbled_deepslate
setblock ~10 ~0 ~32 minecraft:mossy_stone_bricks
fill ~10 ~0 ~33 ~10 ~0 ~35 minecraft:podzol
setblock ~10 ~0 ~36 minecraft:moss_block
setblock ~11 ~0 ~0 minecraft:cobbled_deepslate
fill ~11 ~0 ~1 ~11 ~0 ~3 minecraft:deepslate_tiles
setblock ~11 ~0 ~4 minecraft:polished_deepslate
setblock ~11 ~0 ~5 minecraft:cracked_deepslate_tiles
setblock ~11 ~0 ~6 minecraft:polished_deepslate
setblock ~11 ~0 ~7 minecraft:deepslate_tiles
setblock ~11 ~0 ~9 minecraft:cobbled_deepslate
setblock ~11 ~0 ~10 minecraft:polished_deepslate
fill ~11 ~0 ~11 ~13 ~0 ~11 minecraft:deepslate_tiles
fill ~11 ~0 ~12 ~12 ~0 ~12 minecraft:rooted_dirt
fill ~11 ~0 ~14 ~12 ~0 ~14 minecraft:mossy_cobblestone
fill ~11 ~0 ~15 ~14 ~0 ~15 minecraft:mossy_stone_bricks
setblock ~11 ~0 ~16 minecraft:polished_deepslate
fill ~11 ~0 ~17 ~11 ~0 ~18 minecraft:deepslate_bricks
setblock ~11 ~0 ~20 minecraft:polished_deepslate
fill ~11 ~0 ~21 ~12 ~0 ~22 minecraft:cracked_deepslate_bricks
setblock ~11 ~0 ~23 minecraft:mossy_cobblestone
setblock ~11 ~0 ~25 minecraft:deepslate_bricks
setblock ~11 ~0 ~26 minecraft:polished_deepslate
setblock ~11 ~0 ~27 minecraft:mossy_cobblestone
setblock ~11 ~0 ~28 minecraft:polished_deepslate
fill ~11 ~0 ~29 ~12 ~0 ~29 minecraft:cracked_deepslate_bricks
setblock ~11 ~0 ~30 minecraft:cracked_deepslate_bricks
setblock ~11 ~0 ~31 minecraft:deepslate_bricks
setblock ~11 ~0 ~32 minecraft:mossy_cobblestone
fill ~11 ~0 ~33 ~14 ~0 ~33 minecraft:mossy_stone_bricks
setblock ~11 ~0 ~34 minecraft:cobbled_deepslate
setblock ~11 ~0 ~35 minecraft:mossy_cobblestone
setblock ~11 ~0 ~36 minecraft:cobbled_deepslate
fill ~12 ~0 ~0 ~12 ~0 ~1 minecraft:cracked_deepslate_tiles
setblock ~12 ~0 ~2 minecraft:cobbled_deepslate
fill ~12 ~0 ~3 ~13 ~0 ~3 minecraft:polished_deepslate
fill ~12 ~0 ~4 ~13 ~0 ~4 minecraft:deepslate_tiles
fill ~12 ~0 ~5 ~12 ~0 ~8 minecraft:deepslate_tiles
fill ~12 ~0 ~9 ~12 ~0 ~10 minecraft:cracked_deepslate_tiles
setblock ~12 ~0 ~13 minecraft:coarse_dirt
fill ~12 ~0 ~16 ~14 ~0 ~16 minecraft:deepslate_bricks
setblock ~12 ~0 ~17 minecraft:cracked_deepslate_bricks
setblock ~12 ~0 ~18 minecraft:polished_deepslate
setblock ~12 ~0 ~19 minecraft:cobbled_deepslate
setblock ~12 ~0 ~20 minecraft:deepslate_bricks
setblock ~12 ~0 ~23 minecraft:deepslate_bricks
fill ~12 ~0 ~24 ~12 ~0 ~26 minecraft:polished_deepslate
fill ~12 ~0 ~27 ~13 ~0 ~28 minecraft:deepslate_bricks
fill ~12 ~0 ~30 ~13 ~0 ~31 minecraft:deepslate_bricks
setblock ~12 ~0 ~32 minecraft:cobbled_deepslate
setblock ~12 ~0 ~34 minecraft:cracked_deepslate_bricks
setblock ~12 ~0 ~35 minecraft:deepslate_bricks
setblock ~12 ~0 ~36 minecraft:mossy_cobblestone
fill ~13 ~0 ~0 ~14 ~0 ~0 minecraft:deepslate_tiles
setblock ~13 ~0 ~1 minecraft:polished_deepslate
fill ~13 ~0 ~2 ~15 ~0 ~2 minecraft:deepslate_tiles
setblock ~13 ~0 ~5 minecraft:polished_deepslate
fill ~13 ~0 ~6 ~13 ~0 ~7 minecraft:deepslate_tiles
setblock ~13 ~0 ~8 minecraft:moss_block
setblock ~13 ~0 ~9 minecraft:cobbled_deepslate
fill ~13 ~0 ~10 ~15 ~0 ~10 minecraft:deepslate_tiles
fill ~13 ~0 ~12 ~15 ~0 ~13 minecraft:podzol
setblock ~13 ~0 ~14 minecraft:rooted_dirt
fill ~13 ~0 ~17 ~14 ~0 ~17 minecraft:deepslate_bricks
fill ~13 ~0 ~18 ~13 ~0 ~19 minecraft:deepslate_bricks
setblock ~13 ~0 ~20 minecraft:polished_deepslate
setblock ~13 ~0 ~21 minecraft:cobbled_deepslate
setblock ~13 ~0 ~22 minecraft:deepslate_bricks
setblock ~13 ~0 ~23 minecraft:mossy_cobblestone
fill ~13 ~0 ~24 ~13 ~0 ~25 minecraft:deepslate_bricks
fill ~13 ~0 ~26 ~14 ~0 ~26 minecraft:cracked_deepslate_bricks
setblock ~13 ~0 ~29 minecraft:cobbled_deepslate
setblock ~13 ~0 ~32 minecraft:polished_deepslate
fill ~13 ~0 ~34 ~14 ~0 ~34 minecraft:cobbled_deepslate
fill ~13 ~0 ~35 ~13 ~0 ~36 minecraft:cracked_deepslate_bricks
setblock ~14 ~0 ~1 minecraft:cracked_deepslate_tiles
setblock ~14 ~0 ~3 minecraft:deepslate_tiles
setblock ~14 ~0 ~4 minecraft:cracked_deepslate_tiles
fill ~14 ~0 ~5 ~15 ~0 ~5 minecraft:deepslate_tiles
setblock ~14 ~0 ~6 minecraft:cracked_deepslate_tiles
fill ~14 ~0 ~7 ~14 ~0 ~8 minecraft:deepslate_tiles
setblock ~14 ~0 ~9 minecraft:cracked_deepslate_tiles
setblock ~14 ~0 ~11 minecraft:moss_block
setblock ~14 ~0 ~14 minecraft:podzol
fill ~14 ~0 ~18 ~14 ~0 ~19 minecraft:polished_deepslate
setblock ~14 ~0 ~20 minecraft:cracked_deepslate_bricks
setblock ~14 ~0 ~21 minecraft:mossy_cobblestone
setblock ~14 ~0 ~22 minecraft:polished_deepslate
fill ~14 ~0 ~23 ~16 ~0 ~25 minecraft:chiseled_stone_bricks
setblock ~14 ~0 ~27 minecraft:cracked_deepslate_bricks
fill ~14 ~0 ~28 ~14 ~0 ~30 minecraft:mossy_cobblestone
fill ~14 ~0 ~31 ~15 ~0 ~31 minecraft:cobbled_deepslate
setblock ~14 ~0 ~32 minecraft:cobbled_deepslate
fill ~14 ~0 ~35 ~15 ~0 ~36 minecraft:deepslate_bricks
fill ~15 ~0 ~0 ~16 ~0 ~0 minecraft:cracked_deepslate_tiles
setblock ~15 ~0 ~1 minecraft:cobbled_deepslate
setblock ~15 ~0 ~3 minecraft:cracked_deepslate_tiles
fill ~15 ~0 ~4 ~16 ~0 ~4 minecraft:deepslate_tiles
fill ~15 ~0 ~6 ~16 ~0 ~6 minecraft:deepslate_tiles
setblock ~15 ~0 ~7 minecraft:cracked_deepslate_tiles
setblock ~15 ~0 ~8 minecraft:cobbled_deepslate
fill ~15 ~0 ~9 ~16 ~0 ~9 minecraft:deepslate_tiles
fill ~15 ~0 ~11 ~16 ~0 ~11 minecraft:deepslate_tiles
setblock ~15 ~0 ~14 minecraft:mossy_stone_bricks
fill ~15 ~0 ~15 ~15 ~0 ~16 minecraft:deepslate_bricks
setblock ~15 ~0 ~17 minecraft:polished_deepslate
fill ~15 ~0 ~18 ~15 ~0 ~20 minecraft:deepslate_bricks
setblock ~15 ~0 ~21 minecraft:cobbled_deepslate
setblock ~15 ~0 ~22 minecraft:cracked_deepslate_bricks
setblock ~15 ~0 ~26 minecraft:deepslate_bricks
setblock ~15 ~0 ~27 minecraft:polished_deepslate
setblock ~15 ~0 ~28 minecraft:deepslate_bricks
setblock ~15 ~0 ~29 minecraft:cobbled_deepslate
setblock ~15 ~0 ~30 minecraft:deepslate_bricks
fill ~15 ~0 ~32 ~16 ~0 ~32 minecraft:mossy_cobblestone
setblock ~15 ~0 ~33 minecraft:mossy_cobblestone
setblock ~15 ~0 ~34 minecraft:mossy_stone_bricks
setblock ~16 ~0 ~1 minecraft:polished_deepslate
fill ~16 ~0 ~2 ~16 ~0 ~3 minecraft:cracked_deepslate_tiles
setblock ~16 ~0 ~5 minecraft:cobbled_deepslate
setblock ~16 ~0 ~7 minecraft:deepslate_tiles
fill ~16 ~0 ~8 ~17 ~0 ~8 minecraft:polished_deepslate
setblock ~16 ~0 ~10 minecraft:polished_deepslate
setblock ~16 ~0 ~12 minecraft:rooted_dirt
fill ~16 ~0 ~13 ~17 ~0 ~13 minecraft:podzol
setblock ~16 ~0 ~14 minecraft:moss_block
fill ~16 ~0 ~15 ~19 ~0 ~15 minecraft:mossy_stone_bricks
setblock ~16 ~0 ~16 minecraft:deepslate_bricks
setblock ~16 ~0 ~17 minecraft:mossy_cobblestone
fill ~16 ~0 ~18 ~16 ~0 ~19 minecraft:cobbled_deepslate
setblock ~16 ~0 ~20 minecraft:polished_deepslate
fill ~16 ~0 ~21 ~18 ~0 ~21 minecraft:deepslate_bricks
fill ~16 ~0 ~22 ~17 ~0 ~22 minecraft:deepslate_bricks
fill ~16 ~0 ~26 ~16 ~0 ~28 minecraft:cracked_deepslate_bricks
setblock ~16 ~0 ~29 minecraft:polished_deepslate
setblock ~16 ~0 ~30 minecraft:mossy_cobblestone
fill ~16 ~0 ~31 ~17 ~0 ~31 minecraft:deepslate_bricks
fill ~16 ~0 ~33 ~19 ~0 ~33 minecraft:mossy_stone_bricks
setblock ~16 ~0 ~34 minecraft:deepslate_bricks
setblock ~16 ~0 ~35 minecraft:cobbled_deepslate
setblock ~16 ~0 ~36 minecraft:cracked_deepslate_bricks
fill ~17 ~0 ~0 ~18 ~0 ~0 minecraft:deepslate_tiles
fill ~17 ~0 ~1 ~17 ~0 ~3 minecraft:deepslate_tiles
setblock ~17 ~0 ~4 minecraft:cobbled_deepslate
setblock ~17 ~0 ~5 minecraft:moss_block
setblock ~17 ~0 ~6 minecraft:cracked_deepslate_tiles
fill ~17 ~0 ~7 ~18 ~0 ~7 minecraft:cobbled_deepslate
setblock ~17 ~0 ~9 minecraft:polished_deepslate
setblock ~17 ~0 ~10 minecraft:cobbled_deepslate
setblock ~17 ~0 ~11 minecraft:cracked_deepslate_tiles
setblock ~17 ~0 ~12 minecraft:coarse_dirt
setblock ~17 ~0 ~14 minecraft:rooted_dirt
setblock ~17 ~0 ~16 minecraft:cracked_deepslate_bricks
setblock ~17 ~0 ~17 minecraft:cobbled_deepslate
setblock ~17 ~0 ~18 minecraft:mossy_cobblestone
setblock ~17 ~0 ~19 minecraft:polished_deepslate
setblock ~17 ~0 ~20 minecraft:cracked_deepslate_bricks
setblock ~17 ~0 ~23 minecraft:cracked_deepslate_bricks
setblock ~17 ~0 ~24 minecraft:polished_deepslate
setblock ~17 ~0 ~25 minecraft:cracked_deepslate_bricks
setblock ~17 ~0 ~26 minecraft:mossy_cobblestone
fill ~17 ~0 ~27 ~17 ~0 ~28 minecraft:cracked_deepslate_bricks
setblock ~17 ~0 ~29 minecraft:deepslate_bricks
setblock ~17 ~0 ~30 minecraft:polished_deepslate
setblock ~17 ~0 ~32 minecraft:cracked_deepslate_bricks
fill ~17 ~0 ~34 ~19 ~0 ~34 minecraft:cobbled_deepslate
setblock ~17 ~0 ~35 minecraft:mossy_cobblestone
setblock ~17 ~0 ~36 minecraft:cobbled_deepslate
setblock ~18 ~0 ~1 minecraft:cracked_deepslate_tiles
setblock ~18 ~0 ~2 minecraft:cobbled_deepslate
fill ~18 ~0 ~3 ~18 ~0 ~4 minecraft:deepslate_tiles
setblock ~18 ~0 ~5 minecraft:cracked_deepslate_tiles
setblock ~18 ~0 ~6 minecraft:polished_deepslate
fill ~18 ~0 ~8 ~18 ~0 ~9 minecraft:deepslate_tiles
setblock ~18 ~0 ~10 minecraft:polished_deepslate
setblock ~18 ~0 ~11 minecraft:deepslate_tiles
setblock ~18 ~0 ~12 minecraft:podzol
fill ~18 ~0 ~13 ~18 ~0 ~14 minecraft:rooted_dirt
fill ~18 ~0 ~16 ~18 ~0 ~17 minecraft:cobbled_deepslate
fill ~18 ~0 ~18 ~18 ~0 ~19 minecraft:deepslate_bricks
setblock ~18 ~0 ~20 minecraft:cobbled_deepslate
setblock ~18 ~0 ~22 minecraft:cracked_deepslate_bricks
fill ~18 ~0 ~23 ~18 ~0 ~27 minecraft:deepslate_bricks
setblock ~18 ~0 ~28 minecraft:cracked_deepslate_bricks
fill ~18 ~0 ~29 ~19 ~0 ~29 minecraft:polished_deepslate
fill ~18 ~0 ~30 ~20 ~0 ~30 minecraft:cracked_deepslate_bricks
setblock ~18 ~0 ~31 minecraft:mossy_cobblestone
setblock ~18 ~0 ~32 minecraft:deepslate_bricks
fill ~18 ~0 ~35 ~19 ~0 ~35 minecraft:cobbled_deepslate
setblock ~18 ~0 ~36 minecraft:mossy_cobblestone
setblock ~19 ~0 ~0 minecraft:cracked_deepslate_tiles
fill ~19 ~0 ~1 ~19 ~0 ~2 minecraft:deepslate_tiles
fill ~19 ~0 ~3 ~19 ~0 ~4 minecraft:cracked_deepslate_tiles
fill ~19 ~0 ~5 ~19 ~0 ~8 minecraft:deepslate_tiles
setblock ~19 ~0 ~9 minecraft:polished_deepslate
setblock ~19 ~0 ~10 minecraft:deepslate_tiles
fill ~19 ~0 ~11 ~20 ~0 ~11 minecraft:polished_deepslate
setblock ~19 ~0 ~12 minecraft:mossy_cobblestone
setblock ~19 ~0 ~13 minecraft:coarse_dirt
setblock ~19 ~0 ~14 minecraft:podzol
setblock ~19 ~0 ~16 minecraft:polished_deepslate
setblock ~19 ~0 ~17 minecraft:mossy_cobblestone
setblock ~19 ~0 ~18 minecraft:cobbled_deepslate
setblock ~19 ~0 ~19 minecraft:polished_deepslate
fill ~19 ~0 ~20 ~20 ~0 ~20 minecraft:deepslate_bricks
setblock ~19 ~0 ~21 minecraft:mossy_cobblestone
setblock ~19 ~0 ~22 minecraft:deepslate_bricks
setblock ~19 ~0 ~23 minecraft:cobbled_deepslate
setblock ~19 ~0 ~24 minecraft:cracked_deepslate_bricks
setblock ~19 ~0 ~25 minecraft:cobbled_deepslate
fill ~19 ~0 ~26 ~20 ~0 ~27 minecraft:deepslate_bricks
fill ~19 ~0 ~28 ~20 ~0 ~28 minecraft:polished_deepslate
setblock ~19 ~0 ~31 minecraft:cobbled_deepslate
setblock ~19 ~0 ~32 minecraft:cracked_deepslate_bricks
setblock ~19 ~0 ~36 minecraft:cobbled_deepslate
fill ~20 ~0 ~0 ~22 ~0 ~0 minecraft:deepslate_tiles
setblock ~20 ~0 ~1 minecraft:cracked_deepslate_tiles
setblock ~20 ~0 ~2 minecraft:polished_deepslate
setblock ~20 ~0 ~3 minecraft:deepslate_tiles
setblock ~20 ~0 ~4 minecraft:cracked_deepslate_tiles
setblock ~20 ~0 ~5 minecraft:cobbled_deepslate
fill ~20 ~0 ~6 ~21 ~0 ~6 minecraft:cracked_deepslate_tiles
fill ~20 ~0 ~7 ~21 ~0 ~7 minecraft:deepslate_tiles
setblock ~20 ~0 ~8 minecraft:polished_deepslate
fill ~20 ~0 ~9 ~20 ~0 ~10 minecraft:deepslate_tiles
setblock ~20 ~0 ~12 minecraft:moss_block
setblock ~20 ~0 ~13 minecraft:rooted_dirt
fill ~20 ~0 ~14 ~21 ~0 ~14 minecraft:coarse_dirt
setblock ~20 ~0 ~15 minecraft:coarse_dirt
fill ~20 ~0 ~16 ~21 ~0 ~16 minecraft:mossy_stone_bricks
fill ~20 ~0 ~17 ~20 ~0 ~18 minecraft:deepslate_bricks
setblock ~20 ~0 ~19 minecraft:mossy_cobblestone
fill ~20 ~0 ~21 ~20 ~0 ~22 minecraft:cracked_deepslate_bricks
fill ~20 ~0 ~23 ~20 ~0 ~24 minecraft:mossy_cobblestone
setblock ~20 ~0 ~25 minecraft:cracked_deepslate_bricks
setblock ~20 ~0 ~29 minecraft:deepslate_bricks
setblock ~20 ~0 ~31 minecraft:deepslate_bricks
setblock ~20 ~0 ~32 minecraft:mossy_stone_bricks
setblock ~20 ~0 ~33 minecraft:mossy_cobblestone
setblock ~20 ~0 ~34 minecraft:podzol
setblock ~20 ~0 ~35 minecraft:rooted_dirt
setblock ~20 ~0 ~36 minecraft:moss_block
setblock ~21 ~0 ~1 minecraft:polished_deepslate
setblock ~21 ~0 ~2 minecraft:cracked_deepslate_tiles
setblock ~21 ~0 ~3 minecraft:polished_deepslate
setblock ~21 ~0 ~4 minecraft:cobbled_deepslate
fill ~21 ~0 ~5 ~23 ~0 ~5 minecraft:deepslate_tiles
fill ~21 ~0 ~8 ~21 ~0 ~9 minecraft:deepslate_tiles
setblock ~21 ~0 ~10 minecraft:cracked_deepslate_tiles
setblock ~21 ~0 ~11 minecraft:deepslate_tiles
setblock ~21 ~0 ~12 minecraft:rooted_dirt
setblock ~21 ~0 ~13 minecraft:podzol
setblock ~21 ~0 ~15 minecraft:podzol
fill ~21 ~0 ~17 ~22 ~0 ~17 minecraft:mossy_stone_bricks
setblock ~21 ~0 ~18 minecraft:deepslate_bricks
setblock ~21 ~0 ~19 minecraft:cobbled_deepslate
setblock ~21 ~0 ~20 minecraft:cracked_deepslate_bricks
setblock ~21 ~0 ~21 minecraft:deepslate_bricks
setblock ~21 ~0 ~22 minecraft:cracked_deepslate_bricks
setblock ~21 ~0 ~23 minecraft:cobbled_deepslate
setblock ~21 ~0 ~24 minecraft:polished_deepslate
setblock ~21 ~0 ~25 minecraft:deepslate_bricks
setblock ~21 ~0 ~26 minecraft:cracked_deepslate_bricks
setblock ~21 ~0 ~27 minecraft:polished_deepslate
setblock ~21 ~0 ~28 minecraft:deepslate_bricks
setblock ~21 ~0 ~29 minecraft:cracked_deepslate_bricks
setblock ~21 ~0 ~30 minecraft:deepslate_bricks
fill ~21 ~0 ~31 ~21 ~0 ~32 minecraft:mossy_stone_bricks
setblock ~21 ~0 ~33 minecraft:coarse_dirt
setblock ~21 ~0 ~34 minecraft:rooted_dirt
fill ~21 ~0 ~35 ~21 ~0 ~36 minecraft:podzol
setblock ~22 ~0 ~1 minecraft:cracked_deepslate_tiles
fill ~22 ~0 ~2 ~22 ~0 ~3 minecraft:deepslate_tiles
setblock ~22 ~0 ~4 minecraft:polished_deepslate
setblock ~22 ~0 ~6 minecraft:deepslate_tiles
setblock ~22 ~0 ~7 minecraft:polished_deepslate
setblock ~22 ~0 ~8 minecraft:cracked_deepslate_tiles
fill ~22 ~0 ~9 ~22 ~0 ~11 minecraft:deepslate_tiles
fill ~22 ~0 ~12 ~23 ~0 ~12 minecraft:podzol
fill ~22 ~0 ~13 ~22 ~0 ~14 minecraft:rooted_dirt
fill ~22 ~0 ~15 ~22 ~0 ~16 minecraft:moss_block
fill ~22 ~0 ~18 ~23 ~0 ~18 minecraft:mossy_stone_bricks
fill ~22 ~0 ~19 ~22 ~0 ~20 minecraft:cracked_deepslate_bricks
setblock ~22 ~0 ~21 minecraft:mossy_cobblestone
fill ~22 ~0 ~22 ~22 ~0 ~29 minecraft:deepslate_bricks
fill ~22 ~0 ~30 ~22 ~0 ~31 minecraft:mossy_stone_bricks
setblock ~22 ~0 ~32 minecraft:coarse_dirt
fill ~22 ~0 ~33 ~22 ~0 ~35 minecraft:moss_block
setblock ~22 ~0 ~36 minecraft:podzol
fill ~23 ~0 ~0 ~23 ~0 ~1 minecraft:cracked_deepslate_tiles
setblock ~23 ~0 ~2 minecraft:cobbled_deepslate
fill ~23 ~0 ~3 ~23 ~0 ~4 minecraft:polished_deepslate
setblock ~23 ~0 ~6 minecraft:polished_deepslate
fill ~23 ~0 ~7 ~23 ~0 ~8 minecraft:deepslate_tiles
setblock ~23 ~0 ~9 minecraft:cobbled_deepslate
setblock ~23 ~0 ~10 minecraft:cracked_deepslate_tiles
setblock ~23 ~0 ~11 minecraft:deepslate_tiles
fill ~23 ~0 ~13 ~23 ~0 ~14 minecraft:podzol
setblock ~23 ~0 ~15 minecraft:rooted_dirt
setblock ~23 ~0 ~16 minecraft:coarse_dirt
fill ~23 ~0 ~17 ~24 ~0 ~17 minecraft:podzol
setblock ~23 ~0 ~19 minecraft:mossy_stone_bricks
setblock ~23 ~0 ~20 minecraft:mossy_cobblestone
setblock ~23 ~0 ~21 minecraft:cobbled_deepslate
setblock ~23 ~0 ~22 minecraft:polished_deepslate
setblock ~23 ~0 ~23 minecraft:cracked_deepslate_bricks
fill ~23 ~0 ~24 ~24 ~0 ~24 minecraft:deepslate_bricks
fill ~23 ~0 ~25 ~23 ~0 ~27 minecraft:deepslate_bricks
setblock ~23 ~0 ~28 minecraft:cracked_deepslate_bricks
fill ~23 ~0 ~29 ~23 ~0 ~30 minecraft:mossy_stone_bricks
setblock ~23 ~0 ~31 minecraft:coarse_dirt
fill ~23 ~0 ~32 ~23 ~0 ~33 minecraft:podzol
setblock ~23 ~0 ~34 minecraft:rooted_dirt
fill ~23 ~0 ~35 ~23 ~0 ~36 minecraft:podzol
setblock ~24 ~0 ~0 minecraft:deepslate_tiles
setblock ~24 ~0 ~1 minecraft:polished_deepslate
fill ~24 ~0 ~2 ~26 ~0 ~2 minecraft:deepslate_tiles
setblock ~24 ~0 ~3 minecraft:deepslate_tiles
setblock ~24 ~0 ~4 minecraft:cobbled_deepslate
setblock ~24 ~0 ~5 minecraft:cracked_deepslate_tiles
setblock ~24 ~0 ~6 minecraft:cobbled_deepslate
fill ~24 ~0 ~7 ~24 ~0 ~8 minecraft:cracked_deepslate_tiles
setblock ~24 ~0 ~9 minecraft:moss_block
fill ~24 ~0 ~10 ~26 ~0 ~11 minecraft:deepslate_tiles
fill ~24 ~0 ~12 ~25 ~0 ~12 minecraft:rooted_dirt
setblock ~24 ~0 ~13 minecraft:coarse_dirt
setblock ~24 ~0 ~14 minecraft:rooted_dirt
setblock ~24 ~0 ~15 minecraft:podzol
setblock ~24 ~0 ~16 minecraft:mossy_cobblestone
fill ~24 ~0 ~18 ~24 ~0 ~19 minecraft:podzol
fill ~24 ~0 ~20 ~24 ~0 ~23 minecraft:mossy_stone_bricks
fill ~24 ~0 ~25 ~24 ~0 ~28 minecraft:mossy_stone_bricks
setblock ~24 ~0 ~29 minecraft:coarse_dirt
setblock ~24 ~0 ~30 minecraft:podzol
setblock ~24 ~0 ~31 minecraft:moss_block
setblock ~24 ~0 ~32 minecraft:coarse_dirt
fill ~24 ~0 ~33 ~25 ~0 ~33 minecraft:moss_block
fill ~24 ~0 ~34 ~25 ~0 ~35 minecraft:podzol
fill ~24 ~0 ~36 ~25 ~0 ~36 minecraft:coarse_dirt
setblock ~25 ~0 ~0 minecraft:polished_deepslate
setblock ~25 ~0 ~1 minecraft:cracked_deepslate_tiles
setblock ~25 ~0 ~3 minecraft:moss_block
setblock ~25 ~0 ~4 minecraft:cracked_deepslate_tiles
setblock ~25 ~0 ~5 minecraft:polished_deepslate
fill ~25 ~0 ~6 ~26 ~0 ~6 minecraft:cracked_deepslate_tiles
setblock ~25 ~0 ~7 minecraft:deepslate_tiles
setblock ~25 ~0 ~8 minecraft:polished_deepslate
setblock ~25 ~0 ~9 minecraft:cracked_deepslate_tiles
fill ~25 ~0 ~13 ~25 ~0 ~14 minecraft:podzol
fill ~25 ~0 ~15 ~26 ~0 ~15 minecraft:packed_mud
setblock ~25 ~0 ~16 minecraft:cobbled_deepslate
setblock ~25 ~0 ~17 minecraft:rooted_dirt
setblock ~25 ~0 ~18 minecraft:coarse_dirt
setblock ~25 ~0 ~19 minecraft:rooted_dirt
setblock ~25 ~0 ~20 minecraft:podzol
setblock ~25 ~0 ~21 minecraft:rooted_dirt
setblock ~25 ~0 ~22 minecraft:podzol
setblock ~25 ~0 ~23 minecraft:cobbled_deepslate
setblock ~25 ~0 ~24 minecraft:mossy_stone_bricks
setblock ~25 ~0 ~25 minecraft:cobbled_deepslate
setblock ~25 ~0 ~26 minecraft:podzol
setblock ~25 ~0 ~27 minecraft:coarse_dirt
setblock ~25 ~0 ~28 minecraft:podzol
setblock ~25 ~0 ~29 minecraft:packed_mud
setblock ~25 ~0 ~30 minecraft:coarse_dirt
setblock ~25 ~0 ~31 minecraft:podzol
setblock ~25 ~0 ~32 minecraft:packed_mud
fill ~26 ~0 ~0 ~26 ~0 ~1 minecraft:cracked_deepslate_tiles
fill ~26 ~0 ~3 ~27 ~0 ~3 minecraft:deepslate_tiles
setblock ~26 ~0 ~4 minecraft:polished_deepslate
setblock ~26 ~0 ~5 minecraft:deepslate_tiles
setblock ~26 ~0 ~7 minecraft:cracked_deepslate_tiles
setblock ~26 ~0 ~8 minecraft:deepslate_tiles
setblock ~26 ~0 ~9 minecraft:cobbled_deepslate
setblock ~26 ~0 ~12 minecraft:coarse_dirt
setblock ~26 ~0 ~13 minecraft:mossy_cobblestone
fill ~26 ~0 ~14 ~27 ~0 ~14 minecraft:rooted_dirt
fill ~26 ~0 ~16 ~28 ~0 ~16 minecraft:podzol
setblock ~26 ~0 ~17 minecraft:coarse_dirt
setblock ~26 ~0 ~18 minecraft:podzol
fill ~26 ~0 ~19 ~26 ~0 ~20 minecraft:coarse_dirt
setblock ~26 ~0 ~21 minecraft:cobbled_deepslate
setblock ~26 ~0 ~22 minecraft:coarse_dirt
setblock ~26 ~0 ~23 minecraft:moss_block
setblock ~26 ~0 ~24 minecraft:packed_mud
setblock ~26 ~0 ~25 minecraft:coarse_dirt
setblock ~26 ~0 ~26 minecraft:cobbled_deepslate
fill ~26 ~0 ~27 ~27 ~0 ~27 minecraft:mossy_cobblestone
setblock ~26 ~0 ~28 minecraft:rooted_dirt
setblock ~26 ~0 ~29 minecraft:podzol
setblock ~26 ~0 ~30 minecraft:moss_block
setblock ~26 ~0 ~31 minecraft:rooted_dirt
fill ~26 ~0 ~32 ~27 ~0 ~32 minecraft:coarse_dirt
setblock ~26 ~0 ~33 minecraft:packed_mud
setblock ~26 ~0 ~34 minecraft:rooted_dirt
setblock ~26 ~0 ~35 minecraft:podzol
fill ~26 ~0 ~36 ~27 ~0 ~36 minecraft:rooted_dirt
fill ~27 ~0 ~0 ~28 ~0 ~0 minecraft:deepslate_tiles
fill ~27 ~0 ~1 ~28 ~0 ~1 minecraft:cracked_deepslate_tiles
setblock ~27 ~0 ~2 minecraft:polished_deepslate
fill ~27 ~0 ~4 ~27 ~0 ~6 minecraft:cracked_deepslate_tiles
setblock ~27 ~0 ~7 minecraft:deepslate_tiles
setblock ~27 ~0 ~8 minecraft:cracked_deepslate_tiles
fill ~27 ~0 ~9 ~30 ~0 ~9 minecraft:deepslate_tiles
fill ~27 ~0 ~10 ~27 ~0 ~11 minecraft:polished_deepslate
fill ~27 ~0 ~12 ~28 ~0 ~12 minecraft:rooted_dirt
setblock ~27 ~0 ~13 minecraft:podzol
setblock ~27 ~0 ~15 minecraft:rooted_dirt
setblock ~27 ~0 ~17 minecraft:podzol
setblock ~27 ~0 ~18 minecraft:coarse_dirt
setblock ~27 ~0 ~19 minecraft:mossy_cobblestone
fill ~27 ~0 ~20 ~27 ~0 ~21 minecraft:coarse_dirt
setblock ~27 ~0 ~22 minecraft:cobbled_deepslate
fill ~27 ~0 ~23 ~27 ~0 ~24 minecraft:mossy_cobblestone
fill ~27 ~0 ~25 ~28 ~0 ~26 minecraft:podzol
setblock ~27 ~0 ~28 minecraft:podzol
fill ~27 ~0 ~29 ~27 ~0 ~30 minecraft:mossy_cobblestone
setblock ~27 ~0 ~31 minecraft:podzol
setblock ~27 ~0 ~33 minecraft:mossy_cobblestone
fill ~27 ~0 ~34 ~27 ~0 ~35 minecraft:podzol
setblock ~28 ~0 ~2 minecraft:deepslate_tiles
setblock ~28 ~0 ~3 minecraft:cracked_deepslate_tiles
fill ~28 ~0 ~4 ~28 ~0 ~7 minecraft:deepslate_tiles
setblock ~28 ~0 ~8 minecraft:polished_deepslate
setblock ~28 ~0 ~10 minecraft:cracked_deepslate_tiles
setblock ~28 ~0 ~11 minecraft:deepslate_tiles
fill ~28 ~0 ~13 ~29 ~0 ~14 minecraft:rooted_dirt
fill ~28 ~0 ~15 ~29 ~0 ~15 minecraft:podzol
fill ~28 ~0 ~17 ~30 ~0 ~17 minecraft:rooted_dirt
fill ~28 ~0 ~18 ~29 ~0 ~18 minecraft:podzol
fill ~28 ~0 ~19 ~28 ~0 ~20 minecraft:rooted_dirt
fill ~28 ~0 ~21 ~29 ~0 ~21 minecraft:podzol
setblock ~28 ~0 ~22 minecraft:coarse_dirt
fill ~28 ~0 ~23 ~28 ~0 ~24 minecraft:rooted_dirt
fill ~28 ~0 ~27 ~28 ~0 ~28 minecraft:coarse_dirt
setblock ~28 ~0 ~29 minecraft:moss_block
fill ~28 ~0 ~30 ~28 ~0 ~32 minecraft:rooted_dirt
setblock ~28 ~0 ~33 minecraft:coarse_dirt
setblock ~28 ~0 ~34 minecraft:rooted_dirt
fill ~28 ~0 ~35 ~28 ~0 ~36 minecraft:podzol
setblock ~29 ~0 ~0 minecraft:cobbled_deepslate
setblock ~29 ~0 ~1 minecraft:moss_block
setblock ~29 ~0 ~2 minecraft:cracked_deepslate_tiles
setblock ~29 ~0 ~3 minecraft:cobbled_deepslate
setblock ~29 ~0 ~4 minecraft:polished_deepslate
setblock ~29 ~0 ~5 minecraft:moss_block
fill ~29 ~0 ~6 ~29 ~0 ~7 minecraft:deepslate_tiles
setblock ~29 ~0 ~8 minecraft:moss_block
fill ~29 ~0 ~10 ~30 ~0 ~10 minecraft:polished_deepslate
setblock ~29 ~0 ~11 minecraft:polished_deepslate
setblock ~29 ~0 ~12 minecraft:podzol
setblock ~29 ~0 ~16 minecraft:coarse_dirt
fill ~29 ~0 ~19 ~30 ~0 ~19 minecraft:podzol
fill ~29 ~0 ~20 ~30 ~0 ~20 minecraft:rooted_dirt
fill ~29 ~0 ~22 ~30 ~0 ~22 minecraft:podzol
setblock ~29 ~0 ~23 minecraft:podzol
setblock ~29 ~0 ~24 minecraft:rooted_dirt
fill ~29 ~0 ~25 ~29 ~0 ~26 minecraft:coarse_dirt
setblock ~29 ~0 ~27 minecraft:podzol
setblock ~29 ~0 ~28 minecraft:mossy_cobblestone
setblock ~29 ~0 ~29 minecraft:rooted_dirt
setblock ~29 ~0 ~30 minecraft:moss_block
setblock ~29 ~0 ~31 minecraft:mossy_cobblestone
fill ~29 ~0 ~32 ~30 ~0 ~32 minecraft:moss_block
fill ~29 ~0 ~33 ~30 ~0 ~33 minecraft:rooted_dirt
fill ~29 ~0 ~34 ~30 ~0 ~34 minecraft:podzol
setblock ~29 ~0 ~35 minecraft:rooted_dirt
setblock ~29 ~0 ~36 minecraft:mossy_cobblestone
setblock ~30 ~0 ~0 minecraft:deepslate_tiles
fill ~30 ~0 ~1 ~30 ~0 ~3 minecraft:cracked_deepslate_tiles
fill ~30 ~0 ~4 ~30 ~0 ~5 minecraft:deepslate_tiles
fill ~30 ~0 ~6 ~30 ~0 ~7 minecraft:cracked_deepslate_tiles
setblock ~30 ~0 ~8 minecraft:polished_deepslate
setblock ~30 ~0 ~11 minecraft:cobbled_deepslate
setblock ~30 ~0 ~12 minecraft:moss_block
fill ~30 ~0 ~13 ~30 ~0 ~15 minecraft:podzol
setblock ~30 ~0 ~16 minecraft:mossy_cobblestone
setblock ~30 ~0 ~18 minecraft:rooted_dirt
setblock ~30 ~0 ~21 minecraft:rooted_dirt
setblock ~30 ~0 ~23 minecraft:moss_block
setblock ~30 ~0 ~24 minecraft:podzol
setblock ~30 ~0 ~25 minecraft:rooted_dirt
setblock ~30 ~0 ~26 minecraft:podzol
fill ~30 ~0 ~27 ~30 ~0 ~28 minecraft:rooted_dirt
setblock ~30 ~0 ~29 minecraft:podzol
setblock ~30 ~0 ~30 minecraft:coarse_dirt
setblock ~30 ~0 ~31 minecraft:podzol
setblock ~30 ~0 ~35 minecraft:mossy_cobblestone
setblock ~30 ~0 ~36 minecraft:coarse_dirt

# the storehouse walls (y 1..3): mossy cobble at the foot (y 1), cobble and deepslate with tuff flecks (y 2) and a plate of spruce logs (y 3)
setblock ~0 ~1 ~1 minecraft:cobblestone
setblock ~0 ~1 ~2 minecraft:mossy_cobblestone
fill ~0 ~1 ~3 ~0 ~1 ~4 minecraft:cobblestone
fill ~0 ~1 ~6 ~0 ~1 ~7 minecraft:cobbled_deepslate
setblock ~0 ~1 ~8 minecraft:cobblestone
setblock ~0 ~1 ~9 minecraft:cobbled_deepslate
setblock ~0 ~1 ~10 minecraft:mossy_cobblestone
fill ~1 ~1 ~0 ~1 ~2 ~0 minecraft:cobblestone
fill ~1 ~1 ~11 ~2 ~1 ~11 minecraft:mossy_cobblestone
setblock ~2 ~1 ~0 minecraft:mossy_cobblestone
setblock ~3 ~1 ~0 minecraft:cobbled_deepslate
setblock ~3 ~1 ~11 minecraft:cobblestone
setblock ~4 ~1 ~0 minecraft:mossy_cobblestone
setblock ~4 ~1 ~11 minecraft:mossy_cobblestone
setblock ~6 ~1 ~0 minecraft:tuff
fill ~6 ~1 ~11 ~6 ~2 ~11 minecraft:mossy_cobblestone
fill ~7 ~1 ~0 ~9 ~1 ~0 minecraft:mossy_cobblestone
setblock ~7 ~1 ~11 minecraft:cobbled_deepslate
setblock ~8 ~1 ~11 minecraft:mossy_cobblestone
setblock ~9 ~1 ~11 minecraft:cobbled_deepslate
fill ~11 ~1 ~0 ~12 ~1 ~0 minecraft:cobblestone
setblock ~11 ~1 ~11 minecraft:mossy_cobblestone
setblock ~12 ~1 ~11 minecraft:cobblestone
setblock ~13 ~1 ~0 minecraft:mossy_cobblestone
setblock ~14 ~1 ~0 minecraft:cobblestone
fill ~16 ~1 ~0 ~16 ~2 ~0 minecraft:cobblestone
fill ~17 ~1 ~0 ~17 ~2 ~0 minecraft:mossy_cobblestone
fill ~18 ~1 ~0 ~19 ~1 ~0 minecraft:cobblestone
fill ~18 ~1 ~11 ~20 ~1 ~11 minecraft:mossy_cobblestone
setblock ~21 ~1 ~0 minecraft:mossy_cobblestone
fill ~22 ~1 ~0 ~22 ~2 ~0 minecraft:cobblestone
setblock ~22 ~1 ~11 minecraft:mossy_cobblestone
setblock ~23 ~1 ~0 minecraft:mossy_stone_bricks
setblock ~23 ~1 ~11 minecraft:cobblestone
fill ~24 ~1 ~0 ~24 ~2 ~0 minecraft:mossy_cobblestone
setblock ~24 ~1 ~11 minecraft:mossy_cobblestone
setblock ~25 ~1 ~11 minecraft:cobblestone
setblock ~26 ~1 ~0 minecraft:mossy_cobblestone
setblock ~26 ~1 ~11 minecraft:mossy_cobblestone
fill ~27 ~1 ~0 ~27 ~2 ~0 minecraft:tuff
setblock ~27 ~1 ~11 minecraft:cobbled_deepslate
fill ~28 ~1 ~0 ~28 ~2 ~0 minecraft:cobblestone
fill ~28 ~1 ~11 ~28 ~2 ~11 minecraft:mossy_cobblestone
setblock ~29 ~1 ~0 minecraft:mossy_cobblestone
fill ~30 ~1 ~1 ~30 ~2 ~2 minecraft:mossy_cobblestone
setblock ~30 ~1 ~3 minecraft:cobblestone
setblock ~30 ~1 ~4 minecraft:tuff
fill ~30 ~1 ~6 ~30 ~2 ~6 minecraft:mossy_cobblestone
fill ~30 ~1 ~7 ~30 ~1 ~8 minecraft:cobblestone
setblock ~30 ~1 ~9 minecraft:mossy_cobblestone
setblock ~30 ~1 ~10 minecraft:cobblestone
setblock ~0 ~2 ~1 minecraft:deepslate_bricks
setblock ~0 ~2 ~2 minecraft:cobblestone
setblock ~0 ~2 ~4 minecraft:cobbled_deepslate
fill ~0 ~2 ~6 ~0 ~2 ~7 minecraft:mossy_cobblestone
setblock ~0 ~2 ~9 minecraft:mossy_cobblestone
setblock ~0 ~2 ~10 minecraft:cobbled_deepslate
setblock ~1 ~2 ~11 minecraft:tuff
setblock ~2 ~2 ~0 minecraft:cobblestone
setblock ~2 ~2 ~11 minecraft:mossy_cobblestone
setblock ~3 ~2 ~0 minecraft:tuff
setblock ~4 ~2 ~11 minecraft:cobblestone
fill ~6 ~2 ~0 ~8 ~2 ~0 minecraft:cobblestone
setblock ~8 ~2 ~11 minecraft:cobblestone
setblock ~9 ~2 ~0 minecraft:tuff
setblock ~9 ~2 ~11 minecraft:mossy_cobblestone
setblock ~11 ~2 ~0 minecraft:cobblestone
setblock ~11 ~2 ~11 minecraft:cobblestone
setblock ~12 ~2 ~0 minecraft:mossy_cobblestone
setblock ~12 ~2 ~11 minecraft:tuff
setblock ~13 ~2 ~0 minecraft:tuff
setblock ~14 ~2 ~0 minecraft:cobbled_deepslate
setblock ~18 ~2 ~0 minecraft:cobbled_deepslate
setblock ~18 ~2 ~11 minecraft:mossy_cobblestone
setblock ~19 ~2 ~0 minecraft:tuff
setblock ~20 ~2 ~11 minecraft:cobblestone
setblock ~21 ~2 ~0 minecraft:cobblestone
setblock ~22 ~2 ~11 minecraft:tuff
setblock ~23 ~2 ~0 minecraft:cobbled_deepslate
setblock ~23 ~2 ~11 minecraft:deepslate_bricks
setblock ~24 ~2 ~11 minecraft:cobblestone
fill ~26 ~2 ~11 ~27 ~2 ~11 minecraft:tuff
setblock ~29 ~2 ~0 minecraft:tuff
setblock ~30 ~2 ~4 minecraft:mossy_cobblestone
setblock ~30 ~2 ~7 minecraft:mossy_cobblestone
setblock ~30 ~2 ~9 minecraft:cobblestone
setblock ~30 ~2 ~10 minecraft:cobbled_deepslate
fill ~0 ~3 ~1 ~0 ~3 ~4 minecraft:spruce_log[axis=z]
fill ~0 ~3 ~6 ~0 ~3 ~10 minecraft:spruce_log[axis=z]
fill ~1 ~3 ~0 ~4 ~3 ~0 minecraft:spruce_log[axis=x]
fill ~1 ~3 ~11 ~4 ~3 ~11 minecraft:spruce_log[axis=x]
fill ~6 ~3 ~0 ~9 ~3 ~0 minecraft:spruce_log[axis=x]
fill ~6 ~3 ~11 ~9 ~3 ~11 minecraft:spruce_log[axis=x]
fill ~11 ~3 ~0 ~14 ~3 ~0 minecraft:spruce_log[axis=x]
fill ~11 ~3 ~11 ~12 ~3 ~11 minecraft:spruce_log[axis=x]
fill ~16 ~3 ~0 ~19 ~3 ~0 minecraft:spruce_log[axis=x]
fill ~18 ~3 ~11 ~20 ~3 ~11 minecraft:spruce_log[axis=x]
fill ~21 ~3 ~0 ~24 ~3 ~0 minecraft:spruce_log[axis=x]
fill ~22 ~3 ~11 ~28 ~3 ~11 minecraft:spruce_log[axis=x]
fill ~26 ~3 ~0 ~29 ~3 ~0 minecraft:spruce_log[axis=x]
fill ~30 ~3 ~1 ~30 ~3 ~4 minecraft:spruce_log[axis=z]
fill ~30 ~3 ~6 ~30 ~3 ~10 minecraft:spruce_log[axis=z]

# posts of spruce logs at the corners, the door jambs and between the openings
fill ~0 ~1 ~0 ~0 ~3 ~0 minecraft:spruce_log[axis=y]
fill ~0 ~1 ~5 ~0 ~3 ~5 minecraft:spruce_log[axis=y]
fill ~0 ~1 ~11 ~0 ~3 ~11 minecraft:spruce_log[axis=y]
fill ~5 ~1 ~0 ~5 ~3 ~0 minecraft:spruce_log[axis=y]
fill ~5 ~1 ~11 ~5 ~3 ~11 minecraft:spruce_log[axis=y]
setblock ~10 ~1 ~0 minecraft:spruce_log[axis=y]
fill ~10 ~1 ~11 ~10 ~3 ~11 minecraft:spruce_log[axis=y]
fill ~13 ~1 ~11 ~13 ~3 ~11 minecraft:spruce_log[axis=y]
fill ~15 ~1 ~0 ~15 ~3 ~0 minecraft:spruce_log[axis=y]
fill ~17 ~1 ~11 ~17 ~3 ~11 minecraft:spruce_log[axis=y]
setblock ~20 ~1 ~0 minecraft:spruce_log[axis=y]
fill ~21 ~1 ~11 ~21 ~3 ~11 minecraft:spruce_log[axis=y]
fill ~25 ~1 ~0 ~25 ~3 ~0 minecraft:spruce_log[axis=y]
fill ~29 ~1 ~11 ~30 ~3 ~11 minecraft:spruce_log[axis=y]
fill ~30 ~1 ~0 ~30 ~3 ~0 minecraft:spruce_log[axis=y]
fill ~30 ~1 ~5 ~30 ~3 ~5 minecraft:spruce_log[axis=y]
setblock ~10 ~3 ~0 minecraft:spruce_log[axis=y]
setblock ~20 ~3 ~0 minecraft:spruce_log[axis=y]

# the door: an opening three wide and two high in the front wall at x 14..16 (jambs at x 13 and 17), a beam over it at y 3
fill ~14 ~1 ~11 ~16 ~2 ~11 minecraft:air
fill ~14 ~3 ~11 ~16 ~3 ~11 minecraft:dark_oak_log[axis=x]

# the windows: a pane of purple glass left here and there, the rest open to the dusk
setblock ~0 ~2 ~3 minecraft:purple_stained_glass_pane[north=true,east=false,south=true,west=false]
setblock ~0 ~2 ~8 minecraft:purple_stained_glass_pane[north=true,east=false,south=true,west=false]
setblock ~3 ~2 ~11 minecraft:air
setblock ~4 ~2 ~0 minecraft:air
setblock ~7 ~2 ~11 minecraft:air
setblock ~10 ~2 ~0 minecraft:air
setblock ~19 ~2 ~11 minecraft:purple_stained_glass_pane[north=false,east=true,south=true,west=true]
setblock ~20 ~2 ~0 minecraft:air
setblock ~25 ~2 ~11 minecraft:purple_stained_glass_pane[north=false,east=true,south=true,west=true]
setblock ~26 ~2 ~0 minecraft:air
setblock ~30 ~2 ~3 minecraft:air
setblock ~30 ~2 ~8 minecraft:air

# the roof: shingles of dark oak and spruce stairs gone to moss, a ridge of dark oak with moss, a collapsed bay and holes that show the rafters
fill ~0 ~4 ~0 ~1 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~0 ~4 ~1 minecraft:moss_block
fill ~0 ~4 ~10 ~2 ~4 ~10 minecraft:moss_block
fill ~0 ~4 ~11 ~1 ~4 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~1 ~4 ~1 ~4 ~4 ~1 minecraft:spruce_slab[type=top]
fill ~2 ~4 ~0 ~6 ~4 ~0 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~2 ~4 ~11 minecraft:moss_block
setblock ~3 ~4 ~10 minecraft:spruce_slab[type=top]
setblock ~3 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~4 ~4 ~10 minecraft:dark_oak_slab[type=top]
setblock ~4 ~4 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~5 ~4 ~1 minecraft:dark_oak_slab[type=top]
fill ~5 ~4 ~10 ~10 ~4 ~10 minecraft:moss_block
setblock ~5 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~6 ~4 ~1 ~7 ~4 ~1 minecraft:spruce_slab[type=top]
fill ~6 ~4 ~11 ~9 ~4 ~11 minecraft:moss_block
setblock ~7 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~8 ~4 ~0 ~9 ~4 ~0 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~10 ~4 ~0 ~11 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~10 ~4 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~11 ~4 ~10 minecraft:spruce_slab[type=top]
fill ~11 ~4 ~11 ~13 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~12 ~4 ~0 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~12 ~4 ~10 ~13 ~4 ~10 minecraft:dark_oak_slab[type=top]
setblock ~13 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~13 ~4 ~1 minecraft:moss_block
setblock ~14 ~4 ~0 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~14 ~4 ~1 ~15 ~4 ~1 minecraft:dark_oak_slab[type=top]
setblock ~14 ~4 ~10 minecraft:moss_block
setblock ~14 ~4 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~15 ~4 ~0 ~18 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~15 ~4 ~10 minecraft:dark_oak_slab[type=top]
setblock ~15 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~16 ~4 ~1 ~17 ~4 ~1 minecraft:spruce_slab[type=top]
setblock ~16 ~4 ~10 minecraft:spruce_slab[type=top]
setblock ~16 ~4 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~17 ~4 ~10 ~19 ~4 ~10 minecraft:moss_block
fill ~17 ~4 ~11 ~18 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~18 ~4 ~1 minecraft:dark_oak_slab[type=top]
setblock ~19 ~4 ~0 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~19 ~4 ~1 minecraft:moss_block
setblock ~19 ~4 ~11 minecraft:moss_block
setblock ~20 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~20 ~4 ~1 minecraft:spruce_slab[type=top]
setblock ~20 ~4 ~10 minecraft:spruce_slab[type=top]
fill ~20 ~4 ~11 ~21 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~21 ~4 ~0 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~21 ~4 ~1 ~22 ~4 ~1 minecraft:dark_oak_slab[type=top]
setblock ~21 ~4 ~10 minecraft:moss_block
setblock ~22 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~23 ~4 ~0 ~24 ~4 ~0 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~23 ~4 ~1 minecraft:spruce_slab[type=top]
setblock ~24 ~4 ~1 minecraft:moss_block
fill ~25 ~4 ~0 ~25 ~4 ~1 minecraft:moss_block
setblock ~26 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~26 ~4 ~1 minecraft:moss_block
fill ~27 ~4 ~0 ~28 ~4 ~1 minecraft:moss_block
setblock ~29 ~4 ~0 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~29 ~4 ~1 ~30 ~4 ~1 minecraft:dark_oak_slab[type=top]
fill ~29 ~4 ~10 ~30 ~4 ~10 minecraft:moss_block
setblock ~29 ~4 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~30 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~30 ~4 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~0 ~5 ~2 ~1 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~0 ~5 ~3 minecraft:moss_block
setblock ~0 ~5 ~8 minecraft:spruce_slab[type=top]
setblock ~0 ~5 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~1 ~5 ~3 ~3 ~5 ~3 minecraft:dark_oak_slab[type=top]
setblock ~1 ~5 ~8 minecraft:dark_oak_slab[type=top]
fill ~1 ~5 ~9 ~2 ~5 ~9 minecraft:moss_block
fill ~2 ~5 ~2 ~6 ~5 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~2 ~5 ~8 ~5 ~5 ~8 minecraft:moss_block
fill ~3 ~5 ~9 ~10 ~5 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~4 ~5 ~3 ~6 ~5 ~3 minecraft:spruce_slab[type=top]
fill ~6 ~5 ~8 ~8 ~5 ~8 minecraft:dark_oak_slab[type=top]
setblock ~7 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~7 ~5 ~3 minecraft:dark_oak_slab[type=top]
setblock ~9 ~5 ~8 minecraft:spruce_slab[type=top]
setblock ~10 ~5 ~8 minecraft:dark_oak_slab[type=top]
setblock ~11 ~5 ~8 minecraft:spruce_slab[type=top]
fill ~11 ~5 ~9 ~12 ~5 ~9 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~12 ~5 ~8 minecraft:dark_oak_slab[type=top]
setblock ~13 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~13 ~5 ~3 minecraft:spruce_slab[type=top]
fill ~13 ~5 ~8 ~14 ~5 ~8 minecraft:spruce_slab[type=top]
setblock ~13 ~5 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~14 ~5 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~14 ~5 ~3 ~15 ~5 ~3 minecraft:dark_oak_slab[type=top]
setblock ~14 ~5 ~9 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~15 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~15 ~5 ~8 minecraft:dark_oak_slab[type=top]
setblock ~15 ~5 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~16 ~5 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~16 ~5 ~3 ~18 ~5 ~3 minecraft:spruce_slab[type=top]
fill ~16 ~5 ~8 ~19 ~5 ~8 minecraft:moss_block
setblock ~16 ~5 ~9 minecraft:moss_block
setblock ~17 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~17 ~5 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~18 ~5 ~2 ~20 ~5 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~18 ~5 ~9 minecraft:moss_block
setblock ~19 ~5 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~20 ~5 ~3 ~23 ~5 ~3 minecraft:moss_block
setblock ~20 ~5 ~8 minecraft:dark_oak_slab[type=top]
setblock ~20 ~5 ~9 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~21 ~5 ~2 ~23 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~21 ~5 ~8 minecraft:spruce_slab[type=top]
setblock ~21 ~5 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~24 ~5 ~2 ~26 ~5 ~3 minecraft:moss_block
setblock ~27 ~5 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~27 ~5 ~3 minecraft:moss_block
fill ~28 ~5 ~2 ~29 ~5 ~3 minecraft:moss_block
fill ~29 ~5 ~8 ~30 ~5 ~8 minecraft:spruce_slab[type=top]
setblock ~29 ~5 ~9 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~30 ~5 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~30 ~5 ~3 minecraft:moss_block
setblock ~30 ~5 ~9 minecraft:moss_block
setblock ~0 ~6 ~4 minecraft:moss_block
setblock ~0 ~6 ~5 minecraft:dark_oak_planks
setblock ~0 ~6 ~6 minecraft:spruce_planks
setblock ~0 ~6 ~7 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~1 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~1 ~6 ~5 minecraft:spruce_planks
setblock ~1 ~6 ~6 minecraft:dark_oak_planks
fill ~1 ~6 ~7 ~3 ~6 ~7 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~2 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~2 ~6 ~5 ~2 ~6 ~6 minecraft:moss_block
fill ~3 ~6 ~6 ~5 ~6 ~6 minecraft:moss_block
setblock ~4 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~4 ~6 ~5 ~12 ~6 ~5 minecraft:moss_block
fill ~4 ~6 ~7 ~5 ~6 ~7 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~5 ~6 ~4 ~7 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~6 ~6 ~6 ~8 ~6 ~6 minecraft:spruce_planks
setblock ~6 ~6 ~7 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~7 ~6 ~7 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~8 ~6 ~7 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~9 ~6 ~6 ~12 ~6 ~6 minecraft:moss_block
setblock ~9 ~6 ~7 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~10 ~6 ~7 ~13 ~6 ~7 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~13 ~6 ~4 ~15 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~13 ~6 ~5 ~15 ~6 ~5 minecraft:spruce_planks
setblock ~13 ~6 ~6 minecraft:spruce_planks
fill ~14 ~6 ~6 ~15 ~6 ~6 minecraft:dark_oak_planks
setblock ~14 ~6 ~7 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~15 ~6 ~7 ~16 ~6 ~7 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~16 ~6 ~4 ~17 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~16 ~6 ~5 minecraft:dark_oak_planks
setblock ~16 ~6 ~6 minecraft:spruce_planks
setblock ~17 ~6 ~5 minecraft:spruce_planks
fill ~17 ~6 ~6 ~18 ~6 ~6 minecraft:moss_block
setblock ~17 ~6 ~7 minecraft:moss_block
setblock ~18 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~18 ~6 ~5 minecraft:dark_oak_planks
fill ~18 ~6 ~7 ~19 ~6 ~7 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~19 ~6 ~5 ~23 ~6 ~5 minecraft:spruce_planks
setblock ~19 ~6 ~6 minecraft:spruce_planks
setblock ~20 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~20 ~6 ~6 minecraft:dark_oak_planks
setblock ~20 ~6 ~7 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~21 ~6 ~4 ~22 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~21 ~6 ~6 ~22 ~6 ~6 minecraft:spruce_planks
setblock ~21 ~6 ~7 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~23 ~6 ~4 minecraft:moss_block
setblock ~23 ~6 ~6 minecraft:dark_oak_planks
setblock ~24 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~24 ~6 ~5 minecraft:dark_oak_planks
setblock ~24 ~6 ~6 minecraft:spruce_planks
fill ~25 ~6 ~4 ~26 ~6 ~4 minecraft:moss_block
fill ~25 ~6 ~5 ~26 ~6 ~6 minecraft:spruce_planks
setblock ~27 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~27 ~6 ~5 minecraft:dark_oak_planks
fill ~27 ~6 ~6 ~29 ~6 ~6 minecraft:moss_block
setblock ~28 ~6 ~4 minecraft:moss_block
fill ~28 ~6 ~5 ~30 ~6 ~5 minecraft:spruce_planks
setblock ~29 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~29 ~6 ~7 ~30 ~6 ~7 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~30 ~6 ~4 minecraft:moss_block
setblock ~30 ~6 ~6 minecraft:spruce_planks

# the gable ends under the roof (x 0 and x 30)
fill ~0 ~4 ~2 ~0 ~4 ~3 minecraft:spruce_planks
setblock ~0 ~4 ~4 minecraft:dark_oak_planks
fill ~0 ~4 ~5 ~0 ~4 ~9 minecraft:spruce_planks
fill ~30 ~4 ~2 ~30 ~4 ~3 minecraft:spruce_planks
fill ~30 ~4 ~4 ~30 ~4 ~5 minecraft:dark_oak_planks
fill ~30 ~4 ~6 ~30 ~4 ~9 minecraft:spruce_planks
fill ~0 ~5 ~4 ~0 ~5 ~7 minecraft:spruce_planks
fill ~30 ~5 ~4 ~30 ~5 ~6 minecraft:spruce_planks
setblock ~30 ~5 ~7 minecraft:dark_oak_planks

# the roof: shingles of dark oak and spruce stairs gone to moss, a ridge of dark oak with moss, a collapsed bay and holes that show the rafters: the collapsed bay over x 8..12, z 1..4 (rafters of spruce logs left standing) and two holes
setblock ~8 ~3 ~1 minecraft:spruce_log[axis=z]
setblock ~10 ~3 ~1 minecraft:spruce_log[axis=z]
setblock ~12 ~3 ~1 minecraft:spruce_log[axis=z]
fill ~8 ~4 ~1 ~12 ~4 ~1 minecraft:air
setblock ~8 ~4 ~3 minecraft:spruce_log[axis=z]
setblock ~9 ~4 ~2 minecraft:spruce_log[axis=x]
setblock ~10 ~4 ~3 minecraft:spruce_log[axis=z]
setblock ~11 ~4 ~2 minecraft:spruce_log[axis=x]
setblock ~12 ~4 ~3 minecraft:spruce_log[axis=z]
setblock ~19 ~4 ~3 minecraft:dark_oak_log[axis=z]
setblock ~3 ~5 ~4 minecraft:dark_oak_log[axis=z]
fill ~8 ~5 ~2 ~12 ~5 ~3 minecraft:air
setblock ~19 ~5 ~3 minecraft:air
fill ~3 ~6 ~4 ~3 ~6 ~5 minecraft:air
fill ~8 ~6 ~4 ~12 ~6 ~4 minecraft:air
setblock ~19 ~6 ~4 minecraft:air

# the lookout: the roof cut away over x 22..28, z 7..11, the wall plate of the front wall raised to y 4 and a deck of planks at y 5; beams under it
fill ~22 ~1 ~7 ~22 ~3 ~7 minecraft:spruce_log[axis=y]
fill ~22 ~1 ~10 ~22 ~3 ~10 minecraft:spruce_log[axis=y]
fill ~28 ~1 ~7 ~28 ~3 ~7 minecraft:spruce_log[axis=y]
fill ~28 ~1 ~10 ~28 ~3 ~10 minecraft:spruce_log[axis=y]
fill ~22 ~4 ~7 ~28 ~4 ~7 minecraft:air
fill ~22 ~4 ~8 ~28 ~4 ~8 minecraft:dark_oak_log[axis=x]
fill ~22 ~4 ~9 ~28 ~4 ~9 minecraft:air
fill ~22 ~4 ~10 ~28 ~4 ~10 minecraft:dark_oak_log[axis=x]
fill ~22 ~4 ~11 ~23 ~4 ~11 minecraft:dark_oak_planks
fill ~24 ~4 ~11 ~28 ~4 ~11 minecraft:spruce_planks
setblock ~22 ~5 ~7 minecraft:spruce_planks
fill ~22 ~5 ~8 ~24 ~5 ~8 minecraft:dark_oak_planks
setblock ~22 ~5 ~9 minecraft:spruce_planks
setblock ~22 ~5 ~10 minecraft:dark_oak_planks
fill ~22 ~5 ~11 ~24 ~5 ~11 minecraft:spruce_planks
setblock ~23 ~5 ~7 minecraft:oak_planks
fill ~23 ~5 ~9 ~25 ~5 ~9 minecraft:dark_oak_planks
setblock ~23 ~5 ~10 minecraft:oak_planks
setblock ~24 ~5 ~7 minecraft:spruce_planks
setblock ~24 ~5 ~10 minecraft:dark_oak_planks
setblock ~25 ~5 ~7 minecraft:oak_planks
setblock ~25 ~5 ~8 minecraft:spruce_planks
fill ~25 ~5 ~10 ~25 ~5 ~11 minecraft:spruce_planks
setblock ~26 ~5 ~7 minecraft:spruce_planks
fill ~26 ~5 ~8 ~27 ~5 ~8 minecraft:oak_planks
fill ~26 ~5 ~9 ~26 ~5 ~10 minecraft:spruce_planks
setblock ~26 ~5 ~11 minecraft:dark_oak_planks
setblock ~27 ~5 ~7 minecraft:dark_oak_planks
setblock ~27 ~5 ~9 minecraft:oak_planks
fill ~27 ~5 ~10 ~27 ~5 ~11 minecraft:spruce_planks
fill ~28 ~5 ~7 ~28 ~5 ~8 minecraft:spruce_planks
setblock ~28 ~5 ~9 minecraft:dark_oak_planks
fill ~28 ~5 ~10 ~28 ~5 ~11 minecraft:oak_planks
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

# the yard walls (y 1..2): mossy cobble below, cobble and deepslate above, a few blocks fallen from the top
setblock ~0 ~1 ~12 minecraft:cobbled_deepslate
fill ~0 ~1 ~13 ~0 ~1 ~14 minecraft:cobblestone
setblock ~0 ~1 ~16 minecraft:cobblestone
setblock ~0 ~1 ~18 minecraft:cobbled_deepslate
fill ~0 ~1 ~20 ~0 ~1 ~21 minecraft:cobblestone
fill ~0 ~1 ~22 ~0 ~1 ~23 minecraft:mossy_cobblestone
setblock ~0 ~1 ~24 minecraft:mossy_stone_bricks
setblock ~0 ~1 ~25 minecraft:mossy_cobblestone
setblock ~0 ~1 ~26 minecraft:tuff
setblock ~0 ~1 ~27 minecraft:cobblestone
setblock ~0 ~1 ~29 minecraft:cobbled_deepslate
fill ~0 ~1 ~31 ~0 ~2 ~31 minecraft:cobblestone
setblock ~0 ~1 ~33 minecraft:cobblestone
fill ~0 ~1 ~34 ~0 ~1 ~35 minecraft:mossy_cobblestone
setblock ~0 ~1 ~36 minecraft:mossy_stone_bricks
setblock ~1 ~1 ~36 minecraft:mossy_cobblestone
setblock ~2 ~1 ~36 minecraft:mossy_stone_bricks
setblock ~3 ~1 ~36 minecraft:cobbled_deepslate
setblock ~4 ~1 ~36 minecraft:mossy_stone_bricks
fill ~5 ~1 ~36 ~7 ~1 ~36 minecraft:mossy_cobblestone
setblock ~8 ~1 ~36 minecraft:cobbled_deepslate
fill ~9 ~1 ~36 ~9 ~2 ~36 minecraft:mossy_cobblestone
fill ~21 ~1 ~36 ~23 ~1 ~36 minecraft:cobblestone
setblock ~24 ~1 ~36 minecraft:cobbled_deepslate
setblock ~25 ~1 ~36 minecraft:tuff
setblock ~26 ~1 ~36 minecraft:cobbled_deepslate
fill ~27 ~1 ~36 ~28 ~1 ~36 minecraft:cobblestone
setblock ~29 ~1 ~36 minecraft:mossy_stone_bricks
setblock ~30 ~1 ~12 minecraft:cobblestone
setblock ~30 ~1 ~13 minecraft:mossy_stone_bricks
fill ~30 ~1 ~14 ~30 ~2 ~14 minecraft:cobblestone
setblock ~30 ~1 ~15 minecraft:cobbled_deepslate
setblock ~30 ~1 ~16 minecraft:cobblestone
setblock ~30 ~1 ~17 minecraft:mossy_stone_bricks
fill ~30 ~1 ~18 ~30 ~1 ~19 minecraft:cobblestone
fill ~30 ~1 ~20 ~30 ~2 ~20 minecraft:mossy_cobblestone
setblock ~30 ~1 ~28 minecraft:mossy_cobblestone
setblock ~30 ~1 ~29 minecraft:cobbled_deepslate
setblock ~30 ~1 ~30 minecraft:mossy_stone_bricks
setblock ~30 ~1 ~31 minecraft:cobbled_deepslate
setblock ~30 ~1 ~32 minecraft:mossy_cobblestone
setblock ~30 ~1 ~33 minecraft:cobbled_deepslate
setblock ~30 ~1 ~34 minecraft:tuff
fill ~30 ~1 ~35 ~30 ~1 ~36 minecraft:cobblestone
setblock ~0 ~2 ~12 minecraft:deepslate_bricks
setblock ~0 ~2 ~13 minecraft:cobblestone
setblock ~0 ~2 ~15 minecraft:cobblestone
setblock ~0 ~2 ~16 minecraft:mossy_cobblestone
setblock ~0 ~2 ~17 minecraft:cobblestone
setblock ~0 ~2 ~18 minecraft:tuff
setblock ~0 ~2 ~20 minecraft:cobblestone
setblock ~0 ~2 ~21 minecraft:tuff
setblock ~0 ~2 ~22 minecraft:mossy_cobblestone
setblock ~0 ~2 ~23 minecraft:cobblestone
setblock ~0 ~2 ~24 minecraft:tuff
fill ~0 ~2 ~25 ~0 ~2 ~26 minecraft:cobblestone
fill ~0 ~2 ~27 ~0 ~2 ~28 minecraft:cobbled_deepslate
setblock ~0 ~2 ~30 minecraft:cobblestone
setblock ~0 ~2 ~32 minecraft:cobbled_deepslate
setblock ~0 ~2 ~34 minecraft:mossy_cobblestone
setblock ~0 ~2 ~35 minecraft:cobbled_deepslate
fill ~0 ~2 ~36 ~1 ~2 ~36 minecraft:cobblestone
setblock ~2 ~2 ~36 minecraft:mossy_cobblestone
fill ~4 ~2 ~36 ~5 ~2 ~36 minecraft:cobblestone
setblock ~7 ~2 ~36 minecraft:cobblestone
setblock ~8 ~2 ~36 minecraft:mossy_cobblestone
setblock ~21 ~2 ~36 minecraft:cobblestone
fill ~22 ~2 ~36 ~25 ~2 ~36 minecraft:mossy_cobblestone
setblock ~27 ~2 ~36 minecraft:cobblestone
setblock ~29 ~2 ~36 minecraft:mossy_cobblestone
fill ~30 ~2 ~12 ~30 ~2 ~13 minecraft:mossy_cobblestone
fill ~30 ~2 ~15 ~30 ~2 ~16 minecraft:mossy_cobblestone
setblock ~30 ~2 ~17 minecraft:deepslate_bricks
setblock ~30 ~2 ~18 minecraft:tuff
setblock ~30 ~2 ~19 minecraft:cobblestone
setblock ~30 ~2 ~28 minecraft:cobbled_deepslate
fill ~30 ~2 ~29 ~30 ~2 ~30 minecraft:cobblestone
setblock ~30 ~2 ~31 minecraft:mossy_cobblestone
setblock ~30 ~2 ~32 minecraft:cobbled_deepslate
fill ~30 ~2 ~33 ~30 ~2 ~34 minecraft:cobblestone
setblock ~30 ~2 ~35 minecraft:cobbled_deepslate
setblock ~30 ~2 ~36 minecraft:tuff

# the gate posts of the front gate (x 10 and 20, y 1..2) with the braziers on them, unlit campfires at (10, 3, 36) and (20, 3, 36)
setblock ~10 ~1 ~36 minecraft:spruce_log[axis=y]
setblock ~20 ~1 ~36 minecraft:spruce_log[axis=y]
setblock ~10 ~2 ~36 minecraft:mossy_stone_bricks
setblock ~20 ~2 ~36 minecraft:mossy_stone_bricks
setblock ~10 ~3 ~36 minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false]
setblock ~20 ~3 ~36 minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false]

# the side gate in the east wall (z 22..26): posts at z 21 and 27 four high and a beam over the opening at y 5, clear below
fill ~30 ~1 ~21 ~30 ~4 ~21 minecraft:spruce_log[axis=y]
fill ~30 ~1 ~27 ~30 ~4 ~27 minecraft:spruce_log[axis=y]
fill ~30 ~5 ~21 ~30 ~5 ~27 minecraft:dark_oak_log[axis=z]

# a few blocks fallen from the top of the yard walls (the courses beside the cover line stay whole)
setblock ~0 ~2 ~14 minecraft:air
setblock ~0 ~2 ~19 minecraft:air
setblock ~0 ~2 ~29 minecraft:air
setblock ~0 ~2 ~33 minecraft:air
setblock ~6 ~2 ~36 minecraft:air
setblock ~26 ~2 ~36 minecraft:air

# the forge corner: a lean-to roof of slabs at y 3 against the west wall on posts
fill ~4 ~1 ~14 ~4 ~2 ~14 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~18 ~4 ~2 ~18 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~22 ~4 ~2 ~22 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~0 ~3 ~14 ~4 ~3 ~22 minecraft:dark_oak_slab[type=bottom]

# the forge: anvil, grindstone, smithing table, furnace, cauldron, barrels
setblock ~0 ~1 ~17 minecraft:grindstone[face=floor,facing=east]
setblock ~0 ~1 ~19 minecraft:smithing_table
setblock ~1 ~1 ~22 minecraft:cauldron
setblock ~3 ~1 ~16 minecraft:barrel[facing=up,open=false]
setblock ~3 ~1 ~20 minecraft:barrel[facing=up,open=false]

# the forge is cold: a damaged anvil, soot-dark cauldron, a pot of ferns where the fire was
setblock ~0 ~1 ~15 minecraft:damaged_anvil[facing=east]
setblock ~1 ~1 ~21 minecraft:potted_fern

# the cots: a lean-to of slabs over four beds, chests and barrels between them
fill ~4 ~1 ~27 ~4 ~2 ~27 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~30 ~4 ~2 ~30 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~4 ~1 ~34 ~4 ~2 ~34 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
fill ~0 ~3 ~27 ~4 ~3 ~34 minecraft:spruce_slab[type=bottom]

# the beds, a chest and barrels
setblock ~0 ~1 ~28 minecraft:green_bed[facing=east,part=foot,occupied=false]
setblock ~0 ~1 ~30 minecraft:green_bed[facing=east,part=foot,occupied=false]
setblock ~0 ~1 ~32 minecraft:green_bed[facing=east,part=foot,occupied=false]
setblock ~1 ~1 ~28 minecraft:green_bed[facing=east,part=head,occupied=false]
setblock ~1 ~1 ~30 minecraft:green_bed[facing=east,part=head,occupied=false]
setblock ~1 ~1 ~32 minecraft:green_bed[facing=east,part=head,occupied=false]
setblock ~2 ~1 ~29 minecraft:barrel[facing=up,open=false]
setblock ~2 ~1 ~31 minecraft:chest[facing=west,type=single,waterlogged=false]
setblock ~2 ~1 ~33 minecraft:barrel[facing=up,open=false]

# the herb bench: planks at y 1, flower pots with ferns, azalea, brown mushrooms and dead bush on it, a rail at y 3 with hanging roots
fill ~6 ~1 ~12 ~10 ~1 ~12 minecraft:spruce_planks
setblock ~6 ~2 ~12 minecraft:potted_red_mushroom
setblock ~7 ~2 ~12 minecraft:hanging_roots[waterlogged=false]
setblock ~8 ~2 ~12 minecraft:potted_fern
setblock ~9 ~2 ~12 minecraft:hanging_roots[waterlogged=false]
setblock ~10 ~2 ~12 minecraft:potted_red_mushroom
fill ~6 ~3 ~12 ~10 ~3 ~12 minecraft:dark_oak_planks

# a weapon rack of blocks: stripped logs with pike heads of lightning rods and iron bars
setblock ~24 ~1 ~12 minecraft:stripped_spruce_log[axis=x]
setblock ~25 ~1 ~12 minecraft:iron_bars[north=true,east=true,south=false,west=true]
setblock ~26 ~1 ~12 minecraft:stripped_spruce_log[axis=x]
setblock ~27 ~1 ~12 minecraft:iron_bars[north=true,east=true,south=false,west=true]
setblock ~28 ~1 ~12 minecraft:stripped_spruce_log[axis=x]
setblock ~24 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~25 ~2 ~12 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~26 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]
setblock ~27 ~2 ~12 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~28 ~2 ~12 minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]

# a cart: a plank bed on four fence legs with wheels of trapdoors, its load and a tongue
setblock ~24 ~1 ~16 minecraft:spruce_trapdoor[facing=west,half=bottom,open=true]
setblock ~25 ~1 ~14 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~25 ~1 ~17 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~26 ~1 ~19 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~27 ~1 ~14 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~27 ~1 ~17 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~28 ~1 ~15 ~28 ~1 ~16 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
setblock ~24 ~2 ~16 minecraft:spruce_trapdoor[facing=west,half=top,open=true]
fill ~25 ~2 ~14 ~27 ~2 ~17 minecraft:spruce_planks
setblock ~26 ~2 ~18 minecraft:spruce_stairs[facing=north,half=top]
fill ~28 ~2 ~15 ~28 ~2 ~16 minecraft:spruce_trapdoor[facing=east,half=top,open=true]
setblock ~25 ~3 ~15 minecraft:barrel[facing=up,open=false]
setblock ~26 ~3 ~15 minecraft:spruce_planks

# the cart has lost a wheel: the near wheel gone
fill ~24 ~1 ~15 ~24 ~2 ~15 minecraft:air

# a woodpile: stripped dark oak and spruce logs laid along z against the south-east corner, gone to moss
fill ~27 ~1 ~28 ~27 ~1 ~33 minecraft:stripped_dark_oak_log[axis=z]
fill ~28 ~1 ~28 ~28 ~1 ~33 minecraft:spruce_log[axis=z]
fill ~29 ~1 ~28 ~29 ~1 ~33 minecraft:stripped_dark_oak_log[axis=z]
fill ~27 ~2 ~28 ~27 ~2 ~33 minecraft:spruce_log[axis=z]
fill ~28 ~2 ~29 ~28 ~2 ~33 minecraft:stripped_dark_oak_log[axis=z]
fill ~29 ~2 ~28 ~29 ~2 ~33 minecraft:spruce_log[axis=z]
fill ~27 ~3 ~28 ~27 ~3 ~32 minecraft:stripped_dark_oak_log[axis=z]
setblock ~27 ~3 ~33 minecraft:moss_carpet
setblock ~28 ~3 ~29 minecraft:moss_carpet
fill ~28 ~3 ~31 ~28 ~3 ~32 minecraft:moss_carpet
fill ~29 ~3 ~28 ~29 ~3 ~31 minecraft:stripped_dark_oak_log[axis=z]
setblock ~29 ~3 ~33 minecraft:stripped_dark_oak_log[axis=z]
fill ~27 ~4 ~30 ~27 ~4 ~31 minecraft:moss_carpet
setblock ~29 ~4 ~28 minecraft:moss_carpet
setblock ~29 ~4 ~31 minecraft:moss_carpet
setblock ~29 ~4 ~33 minecraft:moss_carpet

# rubble where the wall has come down: heaps of cobble and deepslate in the corners and along the side wall
setblock ~2 ~1 ~13 minecraft:cobbled_deepslate
setblock ~2 ~1 ~35 minecraft:deepslate_bricks
setblock ~3 ~1 ~34 minecraft:cobbled_deepslate
fill ~28 ~1 ~35 ~29 ~1 ~35 minecraft:deepslate_bricks
fill ~29 ~1 ~18 ~29 ~1 ~19 minecraft:cracked_deepslate_bricks
setblock ~2 ~2 ~13 minecraft:mossy_cobblestone
setblock ~3 ~2 ~34 minecraft:deepslate_bricks
setblock ~29 ~2 ~35 minecraft:cobbled_deepslate

# crates and barrels with candles on them, along the front wall and in the corners
setblock ~1 ~1 ~12 minecraft:barrel[facing=west,open=false]
setblock ~1 ~1 ~24 minecraft:barrel[facing=north,open=false]
setblock ~3 ~1 ~13 minecraft:barrel[facing=up,open=false]
setblock ~5 ~1 ~35 minecraft:barrel[facing=east,open=false]
setblock ~9 ~1 ~35 minecraft:spruce_planks
setblock ~19 ~1 ~12 minecraft:chest[facing=north,type=single,waterlogged=false]
setblock ~20 ~1 ~12 minecraft:barrel[facing=up,open=false]
setblock ~29 ~1 ~12 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~29 ~1 ~13 minecraft:barrel[facing=north,open=false]
setblock ~1 ~2 ~12 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~1 ~2 ~24 minecraft:chest[facing=north,type=single,waterlogged=false]
setblock ~5 ~2 ~35 minecraft:barrel[facing=up,open=false]
setblock ~9 ~2 ~35 minecraft:chest[facing=north,type=single,waterlogged=false]
setblock ~19 ~2 ~12 minecraft:barrel[facing=up,open=false]
setblock ~20 ~2 ~12 minecraft:spruce_planks
setblock ~29 ~2 ~12 minecraft:barrel[facing=up,open=false]
setblock ~1 ~3 ~24 minecraft:candle[candles=1,lit=true,waterlogged=false]
setblock ~5 ~3 ~35 minecraft:candle[candles=2,lit=true,waterlogged=false]
setblock ~19 ~3 ~12 minecraft:candle[candles=3,lit=true,waterlogged=false]
setblock ~29 ~3 ~12 minecraft:candle[candles=1,lit=true,waterlogged=false]

# soul lanterns on poles (the only light of the yard)
fill ~6 ~1 ~18 ~6 ~3 ~18 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~7 ~1 ~31 ~7 ~3 ~31 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~24 ~1 ~30 ~24 ~3 ~30 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~6 ~4 ~18 minecraft:soul_lantern[hanging=false]
setblock ~7 ~4 ~31 minecraft:soul_lantern[hanging=false]
setblock ~24 ~4 ~30 minecraft:soul_lantern[hanging=false]

# inside the storehouse: barrels, chests, candles and a soul lantern, seen through the door and the openings
fill ~3 ~1 ~2 ~4 ~1 ~2 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~3 ~1 ~3 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~6 ~1 ~9 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~7 ~1 ~9 minecraft:barrel[facing=up,open=false]
setblock ~15 ~1 ~3 minecraft:barrel[facing=up,open=false]
setblock ~15 ~1 ~9 minecraft:barrel[facing=up,open=false]
setblock ~17 ~1 ~2 minecraft:barrel[facing=up,open=false]
setblock ~18 ~1 ~3 minecraft:barrel[facing=up,open=false]
setblock ~18 ~1 ~9 minecraft:barrel[facing=up,open=false]
setblock ~22 ~1 ~3 minecraft:barrel[facing=up,open=false]
setblock ~24 ~1 ~3 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~26 ~1 ~2 minecraft:chest[facing=south,type=single,waterlogged=false]
setblock ~15 ~2 ~9 minecraft:soul_lantern[hanging=false]
setblock ~18 ~2 ~9 minecraft:candle[candles=3,lit=true,waterlogged=false]

# moss carpet on the tops of the walls and the cobble, cobweb in the corners inside the storehouse
setblock ~17 ~1 ~1 minecraft:cobweb
setblock ~18 ~1 ~10 minecraft:cobweb
setblock ~23 ~1 ~10 minecraft:cobweb
setblock ~29 ~1 ~7 minecraft:cobweb
setblock ~1 ~2 ~1 minecraft:cobweb
setblock ~2 ~2 ~35 minecraft:moss_carpet
setblock ~3 ~2 ~13 minecraft:moss_carpet
setblock ~3 ~2 ~36 minecraft:moss_carpet
setblock ~8 ~2 ~1 minecraft:cobweb
setblock ~28 ~2 ~28 minecraft:moss_carpet
setblock ~28 ~2 ~36 minecraft:moss_carpet
setblock ~0 ~3 ~26 minecraft:moss_carpet
fill ~0 ~3 ~36 ~2 ~3 ~36 minecraft:moss_carpet
setblock ~2 ~3 ~10 minecraft:cobweb
setblock ~2 ~3 ~13 minecraft:moss_carpet
setblock ~5 ~3 ~36 minecraft:moss_carpet
setblock ~7 ~3 ~1 minecraft:cobweb
setblock ~7 ~3 ~36 minecraft:moss_carpet
setblock ~10 ~3 ~10 minecraft:cobweb
setblock ~11 ~3 ~1 minecraft:cobweb
setblock ~13 ~3 ~1 minecraft:cobweb
setblock ~17 ~3 ~10 minecraft:cobweb
setblock ~19 ~3 ~1 minecraft:cobweb
setblock ~19 ~3 ~10 minecraft:cobweb
fill ~22 ~3 ~36 ~24 ~3 ~36 minecraft:moss_carpet
setblock ~25 ~3 ~1 minecraft:cobweb
fill ~27 ~3 ~1 ~28 ~3 ~1 minecraft:cobweb
setblock ~27 ~3 ~15 minecraft:moss_carpet
setblock ~27 ~3 ~36 minecraft:moss_carpet
setblock ~28 ~3 ~8 minecraft:cobweb
setblock ~28 ~3 ~30 minecraft:moss_carpet
setblock ~30 ~3 ~30 minecraft:moss_carpet
setblock ~30 ~3 ~32 minecraft:moss_carpet
setblock ~30 ~3 ~34 minecraft:moss_carpet
fill ~8 ~4 ~12 ~9 ~4 ~12 minecraft:moss_carpet
setblock ~26 ~4 ~15 minecraft:moss_carpet
setblock ~27 ~4 ~32 minecraft:moss_carpet
