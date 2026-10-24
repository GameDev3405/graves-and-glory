# size 25 13 19
# ironhold/hay_barn: supply barn of Ironhold: a weathered timber barn with a steep dark roof, a hay loft and a hoist beam over the east door, a south door, and a fenced yard with stacks of hay, a cart and barrels.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Body x 1..15, z 1..11 (walls y 1..4, plate y 5); roof x 0..16, z 0..12 (eaves y 5, ridge y 11 at z 6, cap y 12). East door x 15, z 5..7 (y 1..3); south door z 11, x 7..9 (y 1..3); loft door x 15, z 5..7, y 7..9.
# Loft floor y 6 over x 11..14, z 3..9 (front beam x 10) (hole at (14, 6, 8) for the ladder (14, 1..7, 8) against the east gable). The aisle z 5..7 (x 2..14) and the south door way x 7..9 are clear y 1..3.
# Yard: the south strip z 13..18 (fence z 18, gate x 11..13) and the east strip x 17..24 (fence x 24, gate z 5..7); hay stacks at x 1..3, 5..7, 16..18, 21..23 (z 14..16).

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~4 ~3 ~0 minecraft:air
fill ~0 ~1 ~1 ~0 ~3 ~12 minecraft:air
fill ~1 ~1 ~12 ~5 ~3 ~13 minecraft:air
fill ~1 ~1 ~17 ~3 ~12 ~17 minecraft:air
fill ~2 ~1 ~5 ~8 ~3 ~7 minecraft:air
fill ~4 ~1 ~14 ~4 ~12 ~17 minecraft:air
fill ~5 ~1 ~17 ~7 ~12 ~17 minecraft:air
fill ~6 ~1 ~13 ~6 ~12 ~13 minecraft:air
fill ~7 ~1 ~8 ~8 ~6 ~10 minecraft:air
fill ~7 ~1 ~12 ~9 ~3 ~13 minecraft:air
fill ~8 ~1 ~14 ~9 ~12 ~17 minecraft:air
fill ~9 ~1 ~2 ~11 ~6 ~2 minecraft:air
fill ~9 ~1 ~3 ~9 ~6 ~10 minecraft:air
fill ~10 ~1 ~4 ~10 ~5 ~8 minecraft:air
fill ~10 ~1 ~13 ~10 ~12 ~17 minecraft:air
fill ~11 ~1 ~3 ~12 ~3 ~9 minecraft:air
fill ~11 ~1 ~10 ~11 ~6 ~10 minecraft:air
fill ~11 ~1 ~12 ~15 ~3 ~17 minecraft:air
fill ~11 ~1 ~18 ~13 ~12 ~18 minecraft:air
fill ~12 ~1 ~0 ~12 ~3 ~0 minecraft:air
fill ~13 ~1 ~2 ~13 ~5 ~10 minecraft:air
fill ~14 ~1 ~3 ~14 ~5 ~7 minecraft:air
fill ~14 ~1 ~9 ~14 ~5 ~10 minecraft:air
fill ~15 ~1 ~0 ~15 ~3 ~0 minecraft:air
fill ~16 ~1 ~1 ~19 ~5 ~1 minecraft:air
fill ~16 ~1 ~2 ~18 ~6 ~2 minecraft:air
fill ~16 ~1 ~3 ~16 ~7 ~3 minecraft:air
fill ~16 ~1 ~5 ~16 ~9 ~7 minecraft:air
fill ~16 ~1 ~9 ~16 ~5 ~11 minecraft:air
fill ~16 ~1 ~13 ~17 ~12 ~13 minecraft:air
fill ~16 ~1 ~17 ~18 ~12 ~17 minecraft:air
fill ~17 ~1 ~4 ~18 ~8 ~9 minecraft:air
fill ~18 ~1 ~3 ~19 ~1 ~3 minecraft:air
fill ~18 ~1 ~10 ~18 ~12 ~13 minecraft:air
fill ~19 ~1 ~5 ~19 ~4 ~16 minecraft:air
fill ~20 ~1 ~2 ~21 ~1 ~4 minecraft:air
fill ~20 ~1 ~6 ~21 ~12 ~13 minecraft:air
fill ~20 ~1 ~14 ~20 ~12 ~16 minecraft:air
fill ~21 ~1 ~17 ~23 ~12 ~17 minecraft:air
fill ~22 ~1 ~1 ~23 ~12 ~1 minecraft:air
setblock ~22 ~1 ~3 minecraft:air
fill ~22 ~1 ~5 ~22 ~12 ~13 minecraft:air
fill ~23 ~1 ~2 ~23 ~12 ~13 minecraft:air
fill ~24 ~1 ~5 ~24 ~12 ~7 minecraft:air
fill ~0 ~2 ~14 ~0 ~12 ~17 minecraft:air
fill ~1 ~2 ~18 ~9 ~12 ~18 minecraft:air
fill ~2 ~2 ~4 ~2 ~8 ~4 minecraft:air
fill ~2 ~2 ~8 ~2 ~8 ~8 minecraft:air
fill ~3 ~2 ~2 ~3 ~6 ~2 minecraft:air
fill ~3 ~2 ~9 ~3 ~7 ~9 minecraft:air
fill ~3 ~2 ~16 ~3 ~12 ~16 minecraft:air
fill ~4 ~2 ~3 ~4 ~7 ~4 minecraft:air
fill ~5 ~2 ~0 ~6 ~3 ~0 minecraft:air
fill ~5 ~2 ~2 ~5 ~4 ~4 minecraft:air
fill ~6 ~2 ~3 ~6 ~7 ~3 minecraft:air
fill ~6 ~2 ~8 ~6 ~7 ~9 minecraft:air
fill ~7 ~2 ~2 ~8 ~6 ~2 minecraft:air
fill ~7 ~2 ~4 ~7 ~8 ~4 minecraft:air
fill ~8 ~2 ~3 ~8 ~7 ~4 minecraft:air
fill ~9 ~2 ~0 ~11 ~3 ~0 minecraft:air
fill ~10 ~2 ~10 ~10 ~6 ~10 minecraft:air
fill ~12 ~2 ~2 ~12 ~4 ~2 minecraft:air
fill ~12 ~2 ~10 ~12 ~4 ~10 minecraft:air
fill ~13 ~2 ~0 ~14 ~3 ~0 minecraft:air
fill ~14 ~2 ~2 ~14 ~6 ~2 minecraft:air
fill ~15 ~2 ~18 ~18 ~12 ~18 minecraft:air
fill ~16 ~2 ~12 ~16 ~3 ~12 minecraft:air
fill ~17 ~2 ~0 ~23 ~12 ~0 minecraft:air
fill ~17 ~2 ~3 ~17 ~12 ~3 minecraft:air
fill ~17 ~2 ~10 ~17 ~12 ~12 minecraft:air
fill ~18 ~2 ~16 ~18 ~12 ~16 minecraft:air
fill ~19 ~2 ~17 ~20 ~12 ~18 minecraft:air
fill ~20 ~2 ~1 ~21 ~12 ~1 minecraft:air
fill ~20 ~2 ~5 ~21 ~12 ~5 minecraft:air
fill ~21 ~2 ~18 ~23 ~12 ~18 minecraft:air
fill ~24 ~2 ~1 ~24 ~12 ~3 minecraft:air
fill ~24 ~2 ~9 ~24 ~12 ~17 minecraft:air
fill ~0 ~3 ~13 ~0 ~12 ~13 minecraft:air
fill ~0 ~3 ~18 ~0 ~12 ~18 minecraft:air
fill ~1 ~3 ~14 ~3 ~12 ~15 minecraft:air
fill ~1 ~3 ~16 ~2 ~12 ~16 minecraft:air
fill ~2 ~3 ~2 ~2 ~6 ~2 minecraft:air
fill ~2 ~3 ~10 ~2 ~6 ~10 minecraft:air
fill ~3 ~3 ~8 ~3 ~8 ~8 minecraft:air
fill ~4 ~3 ~2 ~4 ~6 ~2 minecraft:air
fill ~5 ~3 ~10 ~5 ~4 ~10 minecraft:air
fill ~5 ~3 ~14 ~7 ~12 ~14 minecraft:air
fill ~5 ~3 ~15 ~5 ~12 ~16 minecraft:air
fill ~6 ~3 ~4 ~6 ~8 ~4 minecraft:air
fill ~6 ~3 ~16 ~6 ~12 ~16 minecraft:air
fill ~7 ~3 ~0 ~8 ~3 ~0 minecraft:air
fill ~7 ~3 ~15 ~7 ~12 ~16 minecraft:air
setblock ~16 ~3 ~0 minecraft:air
fill ~16 ~3 ~14 ~18 ~12 ~14 minecraft:air
fill ~16 ~3 ~15 ~16 ~12 ~16 minecraft:air
fill ~17 ~3 ~16 ~17 ~12 ~16 minecraft:air
fill ~18 ~3 ~3 ~18 ~12 ~3 minecraft:air
fill ~18 ~3 ~15 ~18 ~12 ~15 minecraft:air
fill ~21 ~3 ~14 ~23 ~12 ~16 minecraft:air
fill ~22 ~3 ~2 ~22 ~12 ~4 minecraft:air
fill ~24 ~3 ~0 ~24 ~12 ~0 minecraft:air
fill ~24 ~3 ~18 ~24 ~12 ~18 minecraft:air
fill ~0 ~4 ~1 ~0 ~5 ~11 minecraft:air
fill ~1 ~4 ~13 ~5 ~12 ~13 minecraft:air
fill ~2 ~4 ~3 ~3 ~7 ~3 minecraft:air
fill ~2 ~4 ~5 ~2 ~9 ~7 minecraft:air
fill ~2 ~4 ~9 ~2 ~7 ~9 minecraft:air
fill ~3 ~4 ~4 ~3 ~8 ~7 minecraft:air
fill ~3 ~4 ~10 ~3 ~6 ~10 minecraft:air
fill ~4 ~4 ~5 ~8 ~4 ~5 minecraft:air
fill ~4 ~4 ~6 ~4 ~6 ~10 minecraft:air
fill ~5 ~4 ~7 ~5 ~4 ~9 minecraft:air
fill ~6 ~4 ~2 ~6 ~6 ~2 minecraft:air
fill ~6 ~4 ~6 ~8 ~9 ~7 minecraft:air
fill ~6 ~4 ~10 ~6 ~6 ~10 minecraft:air
fill ~6 ~4 ~15 ~6 ~12 ~15 minecraft:air
fill ~7 ~4 ~3 ~7 ~7 ~3 minecraft:air
fill ~7 ~4 ~13 ~9 ~12 ~13 minecraft:air
fill ~11 ~4 ~3 ~12 ~4 ~5 minecraft:air
fill ~11 ~4 ~6 ~11 ~5 ~9 minecraft:air
fill ~11 ~4 ~13 ~15 ~12 ~17 minecraft:air
fill ~12 ~4 ~7 ~12 ~4 ~9 minecraft:air
fill ~16 ~4 ~4 ~16 ~6 ~4 minecraft:air
fill ~16 ~4 ~8 ~16 ~6 ~8 minecraft:air
fill ~17 ~4 ~15 ~17 ~12 ~15 minecraft:air
fill ~19 ~4 ~2 ~21 ~12 ~2 minecraft:air
fill ~19 ~4 ~3 ~19 ~12 ~4 minecraft:air
fill ~20 ~4 ~4 ~20 ~12 ~4 minecraft:air
fill ~21 ~4 ~3 ~21 ~12 ~4 minecraft:air
fill ~4 ~5 ~5 ~4 ~9 ~5 minecraft:air
fill ~6 ~5 ~5 ~8 ~9 ~5 minecraft:air
fill ~10 ~5 ~18 ~10 ~12 ~18 minecraft:air
fill ~11 ~5 ~3 ~11 ~5 ~5 minecraft:air
fill ~14 ~5 ~18 ~14 ~12 ~18 minecraft:air
fill ~19 ~5 ~5 ~19 ~12 ~5 minecraft:air
fill ~19 ~5 ~7 ~19 ~12 ~16 minecraft:air
fill ~20 ~5 ~3 ~20 ~12 ~3 minecraft:air
fill ~24 ~5 ~4 ~24 ~12 ~4 minecraft:air
fill ~24 ~5 ~8 ~24 ~12 ~8 minecraft:air
fill ~0 ~6 ~0 ~16 ~12 ~0 minecraft:air
fill ~0 ~6 ~2 ~0 ~6 ~10 minecraft:air
fill ~0 ~6 ~12 ~16 ~12 ~12 minecraft:air
fill ~5 ~6 ~2 ~5 ~6 ~10 minecraft:air
fill ~12 ~6 ~2 ~13 ~6 ~2 minecraft:air
fill ~12 ~6 ~10 ~14 ~6 ~10 minecraft:air
fill ~16 ~6 ~9 ~16 ~6 ~10 minecraft:air
fill ~17 ~6 ~1 ~19 ~12 ~1 minecraft:air
fill ~0 ~7 ~1 ~16 ~12 ~1 minecraft:air
fill ~0 ~7 ~3 ~0 ~7 ~9 minecraft:air
fill ~0 ~7 ~11 ~16 ~12 ~11 minecraft:air
fill ~4 ~7 ~6 ~4 ~7 ~9 minecraft:air
fill ~5 ~7 ~3 ~5 ~7 ~9 minecraft:air
fill ~7 ~7 ~8 ~8 ~7 ~9 minecraft:air
fill ~9 ~7 ~3 ~10 ~7 ~9 minecraft:air
fill ~11 ~7 ~4 ~11 ~8 ~8 minecraft:air
fill ~12 ~7 ~5 ~12 ~9 ~7 minecraft:air
fill ~13 ~7 ~4 ~13 ~8 ~8 minecraft:air
fill ~14 ~7 ~3 ~14 ~7 ~4 minecraft:air
fill ~14 ~7 ~6 ~14 ~10 ~6 minecraft:air
setblock ~14 ~7 ~9 minecraft:air
setblock ~16 ~7 ~9 minecraft:air
fill ~17 ~7 ~2 ~18 ~12 ~2 minecraft:air
fill ~0 ~8 ~2 ~16 ~12 ~2 minecraft:air
fill ~0 ~8 ~4 ~0 ~8 ~8 minecraft:air
fill ~0 ~8 ~10 ~16 ~12 ~10 minecraft:air
fill ~4 ~8 ~4 ~5 ~8 ~4 minecraft:air
fill ~4 ~8 ~6 ~4 ~8 ~8 minecraft:air
fill ~5 ~8 ~5 ~5 ~8 ~8 minecraft:air
fill ~6 ~8 ~8 ~8 ~8 ~8 minecraft:air
fill ~8 ~8 ~4 ~10 ~8 ~4 minecraft:air
fill ~9 ~8 ~5 ~10 ~8 ~8 minecraft:air
setblock ~12 ~8 ~4 minecraft:air
fill ~14 ~8 ~4 ~14 ~8 ~5 minecraft:air
fill ~14 ~8 ~7 ~14 ~8 ~8 minecraft:air
fill ~0 ~9 ~3 ~16 ~12 ~3 minecraft:air
fill ~0 ~9 ~5 ~0 ~9 ~7 minecraft:air
fill ~0 ~9 ~9 ~16 ~12 ~9 minecraft:air
fill ~3 ~9 ~5 ~3 ~9 ~7 minecraft:air
fill ~4 ~9 ~6 ~4 ~9 ~7 minecraft:air
fill ~5 ~9 ~5 ~5 ~9 ~7 minecraft:air
fill ~9 ~9 ~5 ~11 ~9 ~7 minecraft:air
fill ~13 ~9 ~5 ~14 ~9 ~5 minecraft:air
fill ~13 ~9 ~6 ~13 ~9 ~7 minecraft:air
setblock ~14 ~9 ~7 minecraft:air
fill ~17 ~9 ~4 ~18 ~12 ~5 minecraft:air
fill ~17 ~9 ~7 ~18 ~12 ~9 minecraft:air
fill ~0 ~10 ~4 ~16 ~12 ~4 minecraft:air
setblock ~0 ~10 ~6 minecraft:air
fill ~0 ~10 ~8 ~16 ~12 ~8 minecraft:air
fill ~2 ~10 ~6 ~13 ~10 ~6 minecraft:air
fill ~0 ~11 ~5 ~16 ~12 ~5 minecraft:air
fill ~0 ~11 ~7 ~16 ~12 ~7 minecraft:air
fill ~17 ~11 ~6 ~19 ~12 ~6 minecraft:air

# the ground: trodden earth in the yard and the barn, stone aprons and lanes at the doors and gates, a cobble sill under the walls
setblock ~0 ~0 ~13 minecraft:gravel
fill ~0 ~0 ~14 ~0 ~0 ~15 minecraft:packed_mud
fill ~0 ~0 ~16 ~0 ~0 ~17 minecraft:gravel
setblock ~0 ~0 ~18 minecraft:coarse_dirt
setblock ~1 ~0 ~1 minecraft:mossy_cobblestone
setblock ~1 ~0 ~2 minecraft:cobblestone
setblock ~1 ~0 ~3 minecraft:mossy_cobblestone
setblock ~1 ~0 ~4 minecraft:cobblestone
setblock ~1 ~0 ~5 minecraft:mossy_cobblestone
fill ~1 ~0 ~6 ~1 ~0 ~8 minecraft:cobblestone
setblock ~1 ~0 ~9 minecraft:mossy_cobblestone
setblock ~1 ~0 ~10 minecraft:cobblestone
fill ~1 ~0 ~11 ~2 ~0 ~11 minecraft:mossy_cobblestone
fill ~1 ~0 ~13 ~2 ~0 ~14 minecraft:packed_mud
setblock ~1 ~0 ~15 minecraft:gravel
fill ~1 ~0 ~16 ~1 ~0 ~17 minecraft:packed_mud
setblock ~1 ~0 ~18 minecraft:gravel
fill ~2 ~0 ~1 ~3 ~0 ~1 minecraft:cobblestone
setblock ~2 ~0 ~2 minecraft:gravel
fill ~2 ~0 ~3 ~3 ~0 ~4 minecraft:packed_mud
setblock ~2 ~0 ~5 minecraft:packed_mud
setblock ~2 ~0 ~6 minecraft:gravel
setblock ~2 ~0 ~7 minecraft:coarse_dirt
fill ~2 ~0 ~8 ~5 ~0 ~8 minecraft:packed_mud
setblock ~2 ~0 ~9 minecraft:gravel
fill ~2 ~0 ~10 ~4 ~0 ~10 minecraft:packed_mud
fill ~2 ~0 ~15 ~4 ~0 ~15 minecraft:packed_mud
fill ~2 ~0 ~16 ~4 ~0 ~16 minecraft:gravel
fill ~2 ~0 ~17 ~2 ~0 ~18 minecraft:rooted_dirt
setblock ~3 ~0 ~2 minecraft:coarse_dirt
setblock ~3 ~0 ~5 minecraft:gravel
setblock ~3 ~0 ~6 minecraft:packed_mud
setblock ~3 ~0 ~7 minecraft:gravel
fill ~3 ~0 ~9 ~5 ~0 ~9 minecraft:packed_mud
fill ~3 ~0 ~11 ~4 ~0 ~11 minecraft:cobblestone
setblock ~3 ~0 ~13 minecraft:coarse_dirt
fill ~3 ~0 ~14 ~7 ~0 ~14 minecraft:packed_mud
fill ~3 ~0 ~17 ~4 ~0 ~17 minecraft:packed_mud
setblock ~3 ~0 ~18 minecraft:gravel
setblock ~4 ~0 ~1 minecraft:mossy_cobblestone
fill ~4 ~0 ~2 ~8 ~0 ~2 minecraft:packed_mud
fill ~4 ~0 ~3 ~6 ~0 ~3 minecraft:gravel
fill ~4 ~0 ~4 ~7 ~0 ~4 minecraft:packed_mud
fill ~4 ~0 ~5 ~5 ~0 ~5 minecraft:packed_mud
setblock ~4 ~0 ~6 minecraft:gravel
fill ~4 ~0 ~7 ~5 ~0 ~7 minecraft:coarse_dirt
setblock ~4 ~0 ~13 minecraft:rooted_dirt
setblock ~4 ~0 ~18 minecraft:coarse_dirt
fill ~5 ~0 ~1 ~8 ~0 ~1 minecraft:cobblestone
setblock ~5 ~0 ~6 minecraft:packed_mud
setblock ~5 ~0 ~10 minecraft:coarse_dirt
setblock ~5 ~0 ~11 minecraft:mossy_cobblestone
setblock ~5 ~0 ~13 minecraft:coarse_dirt
setblock ~5 ~0 ~15 minecraft:coarse_dirt
fill ~5 ~0 ~16 ~7 ~0 ~16 minecraft:packed_mud
fill ~5 ~0 ~17 ~5 ~0 ~18 minecraft:gravel
fill ~6 ~0 ~5 ~6 ~0 ~6 minecraft:gravel
fill ~6 ~0 ~7 ~7 ~0 ~7 minecraft:packed_mud
fill ~6 ~0 ~8 ~7 ~0 ~8 minecraft:coarse_dirt
setblock ~6 ~0 ~9 minecraft:coarse_dirt
setblock ~6 ~0 ~10 minecraft:packed_mud
setblock ~6 ~0 ~11 minecraft:cobblestone
setblock ~6 ~0 ~13 minecraft:gravel
fill ~6 ~0 ~15 ~8 ~0 ~15 minecraft:packed_mud
fill ~6 ~0 ~17 ~7 ~0 ~17 minecraft:coarse_dirt
setblock ~6 ~0 ~18 minecraft:coarse_dirt
fill ~7 ~0 ~3 ~8 ~0 ~3 minecraft:coarse_dirt
setblock ~7 ~0 ~5 minecraft:coarse_dirt
setblock ~7 ~0 ~6 minecraft:gravel
setblock ~7 ~0 ~9 minecraft:packed_mud
setblock ~7 ~0 ~10 minecraft:rooted_dirt
setblock ~7 ~0 ~13 minecraft:cobblestone
setblock ~7 ~0 ~18 minecraft:gravel
fill ~8 ~0 ~4 ~8 ~0 ~5 minecraft:gravel
fill ~8 ~0 ~6 ~8 ~0 ~8 minecraft:packed_mud
fill ~8 ~0 ~9 ~9 ~0 ~9 minecraft:coarse_dirt
fill ~8 ~0 ~10 ~9 ~0 ~10 minecraft:packed_mud
fill ~8 ~0 ~13 ~8 ~0 ~14 minecraft:gravel
setblock ~8 ~0 ~16 minecraft:gravel
setblock ~8 ~0 ~17 minecraft:packed_mud
setblock ~8 ~0 ~18 minecraft:coarse_dirt
setblock ~9 ~0 ~1 minecraft:mossy_cobblestone
setblock ~9 ~0 ~2 minecraft:coarse_dirt
setblock ~9 ~0 ~3 minecraft:gravel
setblock ~9 ~0 ~4 minecraft:packed_mud
fill ~9 ~0 ~5 ~11 ~0 ~5 minecraft:gravel
setblock ~9 ~0 ~6 minecraft:coarse_dirt
setblock ~9 ~0 ~7 minecraft:rooted_dirt
fill ~9 ~0 ~8 ~10 ~0 ~8 minecraft:packed_mud
setblock ~9 ~0 ~13 minecraft:mossy_cobblestone
setblock ~9 ~0 ~14 minecraft:coarse_dirt
setblock ~9 ~0 ~15 minecraft:gravel
setblock ~9 ~0 ~16 minecraft:packed_mud
fill ~9 ~0 ~17 ~10 ~0 ~17 minecraft:gravel
setblock ~9 ~0 ~18 minecraft:packed_mud
setblock ~10 ~0 ~1 minecraft:cobblestone
setblock ~10 ~0 ~2 minecraft:packed_mud
fill ~10 ~0 ~3 ~10 ~0 ~4 minecraft:rooted_dirt
fill ~10 ~0 ~6 ~11 ~0 ~6 minecraft:rooted_dirt
setblock ~10 ~0 ~7 minecraft:coarse_dirt
fill ~10 ~0 ~9 ~10 ~0 ~10 minecraft:gravel
fill ~10 ~0 ~11 ~14 ~0 ~11 minecraft:cobblestone
fill ~10 ~0 ~13 ~11 ~0 ~13 minecraft:packed_mud
fill ~10 ~0 ~14 ~10 ~0 ~15 minecraft:packed_mud
setblock ~10 ~0 ~16 minecraft:coarse_dirt
setblock ~10 ~0 ~18 minecraft:coarse_dirt
setblock ~11 ~0 ~1 minecraft:andesite
setblock ~11 ~0 ~2 minecraft:gravel
fill ~11 ~0 ~3 ~11 ~0 ~4 minecraft:packed_mud
fill ~11 ~0 ~7 ~11 ~0 ~8 minecraft:packed_mud
fill ~11 ~0 ~9 ~13 ~0 ~10 minecraft:coarse_dirt
setblock ~11 ~0 ~14 minecraft:cobblestone
setblock ~11 ~0 ~15 minecraft:gravel
setblock ~11 ~0 ~16 minecraft:packed_mud
fill ~11 ~0 ~17 ~11 ~0 ~18 minecraft:mossy_cobblestone
fill ~12 ~0 ~1 ~14 ~0 ~1 minecraft:cobblestone
fill ~12 ~0 ~2 ~13 ~0 ~2 minecraft:packed_mud
setblock ~12 ~0 ~3 minecraft:gravel
fill ~12 ~0 ~4 ~12 ~0 ~6 minecraft:packed_mud
setblock ~12 ~0 ~7 minecraft:rooted_dirt
fill ~12 ~0 ~8 ~13 ~0 ~8 minecraft:gravel
setblock ~12 ~0 ~13 minecraft:coarse_dirt
fill ~12 ~0 ~14 ~12 ~0 ~15 minecraft:packed_mud
setblock ~12 ~0 ~16 minecraft:cobblestone
setblock ~12 ~0 ~17 minecraft:packed_mud
fill ~12 ~0 ~18 ~13 ~0 ~18 minecraft:cobblestone
fill ~13 ~0 ~3 ~14 ~0 ~3 minecraft:packed_mud
fill ~13 ~0 ~4 ~13 ~0 ~5 minecraft:rooted_dirt
fill ~13 ~0 ~6 ~13 ~0 ~7 minecraft:packed_mud
setblock ~13 ~0 ~13 minecraft:cobblestone
setblock ~13 ~0 ~14 minecraft:mossy_cobblestone
fill ~13 ~0 ~15 ~13 ~0 ~17 minecraft:gravel
setblock ~14 ~0 ~2 minecraft:coarse_dirt
fill ~14 ~0 ~4 ~14 ~0 ~5 minecraft:coarse_dirt
fill ~14 ~0 ~6 ~14 ~0 ~7 minecraft:gravel
fill ~14 ~0 ~8 ~14 ~0 ~10 minecraft:packed_mud
fill ~14 ~0 ~13 ~16 ~0 ~13 minecraft:packed_mud
setblock ~14 ~0 ~14 minecraft:packed_mud
fill ~14 ~0 ~15 ~14 ~0 ~16 minecraft:coarse_dirt
setblock ~14 ~0 ~17 minecraft:gravel
setblock ~14 ~0 ~18 minecraft:packed_mud
setblock ~15 ~0 ~1 minecraft:mossy_cobblestone
fill ~15 ~0 ~2 ~15 ~0 ~3 minecraft:cobblestone
setblock ~15 ~0 ~4 minecraft:andesite
fill ~15 ~0 ~8 ~15 ~0 ~10 minecraft:cobblestone
setblock ~15 ~0 ~11 minecraft:mossy_cobblestone
fill ~15 ~0 ~14 ~15 ~0 ~16 minecraft:coarse_dirt
fill ~15 ~0 ~17 ~17 ~0 ~17 minecraft:packed_mud
setblock ~15 ~0 ~18 minecraft:gravel
setblock ~16 ~0 ~14 minecraft:gravel
setblock ~16 ~0 ~15 minecraft:packed_mud
setblock ~16 ~0 ~16 minecraft:gravel
fill ~16 ~0 ~18 ~17 ~0 ~18 minecraft:packed_mud
fill ~17 ~0 ~0 ~18 ~0 ~0 minecraft:coarse_dirt
setblock ~17 ~0 ~1 minecraft:coarse_dirt
fill ~17 ~0 ~2 ~18 ~0 ~2 minecraft:gravel
fill ~17 ~0 ~3 ~17 ~0 ~5 minecraft:gravel
fill ~17 ~0 ~6 ~18 ~0 ~6 minecraft:coarse_dirt
setblock ~17 ~0 ~7 minecraft:cobblestone
setblock ~17 ~0 ~8 minecraft:coarse_dirt
fill ~17 ~0 ~9 ~17 ~0 ~11 minecraft:packed_mud
setblock ~17 ~0 ~12 minecraft:gravel
setblock ~17 ~0 ~13 minecraft:coarse_dirt
fill ~17 ~0 ~14 ~17 ~0 ~15 minecraft:packed_mud
setblock ~17 ~0 ~16 minecraft:coarse_dirt
setblock ~18 ~0 ~1 minecraft:packed_mud
fill ~18 ~0 ~3 ~18 ~0 ~4 minecraft:packed_mud
fill ~18 ~0 ~5 ~19 ~0 ~5 minecraft:coarse_dirt
fill ~18 ~0 ~7 ~19 ~0 ~7 minecraft:packed_mud
fill ~18 ~0 ~8 ~19 ~0 ~8 minecraft:cobblestone
setblock ~18 ~0 ~9 minecraft:coarse_dirt
setblock ~18 ~0 ~10 minecraft:gravel
fill ~18 ~0 ~11 ~19 ~0 ~11 minecraft:packed_mud
setblock ~18 ~0 ~12 minecraft:packed_mud
setblock ~18 ~0 ~13 minecraft:rooted_dirt
setblock ~18 ~0 ~14 minecraft:gravel
setblock ~18 ~0 ~15 minecraft:packed_mud
fill ~18 ~0 ~16 ~18 ~0 ~17 minecraft:rooted_dirt
setblock ~18 ~0 ~18 minecraft:coarse_dirt
fill ~19 ~0 ~0 ~20 ~0 ~1 minecraft:gravel
setblock ~19 ~0 ~2 minecraft:packed_mud
fill ~19 ~0 ~3 ~20 ~0 ~3 minecraft:coarse_dirt
setblock ~19 ~0 ~4 minecraft:cobblestone
fill ~19 ~0 ~6 ~20 ~0 ~6 minecraft:packed_mud
fill ~19 ~0 ~9 ~19 ~0 ~10 minecraft:gravel
fill ~19 ~0 ~12 ~21 ~0 ~12 minecraft:gravel
setblock ~19 ~0 ~13 minecraft:coarse_dirt
setblock ~19 ~0 ~14 minecraft:packed_mud
fill ~19 ~0 ~15 ~19 ~0 ~16 minecraft:gravel
setblock ~19 ~0 ~17 minecraft:packed_mud
fill ~19 ~0 ~18 ~20 ~0 ~18 minecraft:gravel
setblock ~20 ~0 ~2 minecraft:gravel
setblock ~20 ~0 ~4 minecraft:gravel
setblock ~20 ~0 ~5 minecraft:mossy_cobblestone
fill ~20 ~0 ~7 ~20 ~0 ~8 minecraft:mossy_cobblestone
fill ~20 ~0 ~9 ~22 ~0 ~9 minecraft:packed_mud
setblock ~20 ~0 ~10 minecraft:packed_mud
setblock ~20 ~0 ~11 minecraft:rooted_dirt
fill ~20 ~0 ~13 ~20 ~0 ~14 minecraft:gravel
setblock ~20 ~0 ~15 minecraft:coarse_dirt
setblock ~20 ~0 ~16 minecraft:packed_mud
setblock ~20 ~0 ~17 minecraft:coarse_dirt
setblock ~21 ~0 ~0 minecraft:packed_mud
fill ~21 ~0 ~1 ~21 ~0 ~2 minecraft:coarse_dirt
setblock ~21 ~0 ~3 minecraft:packed_mud
fill ~21 ~0 ~4 ~22 ~0 ~4 minecraft:coarse_dirt
fill ~21 ~0 ~5 ~22 ~0 ~5 minecraft:cobblestone
fill ~21 ~0 ~6 ~21 ~0 ~7 minecraft:gravel
setblock ~21 ~0 ~8 minecraft:coarse_dirt
setblock ~21 ~0 ~10 minecraft:coarse_dirt
setblock ~21 ~0 ~11 minecraft:packed_mud
fill ~21 ~0 ~13 ~21 ~0 ~14 minecraft:coarse_dirt
fill ~21 ~0 ~15 ~22 ~0 ~15 minecraft:packed_mud
fill ~21 ~0 ~16 ~22 ~0 ~16 minecraft:gravel
fill ~21 ~0 ~17 ~23 ~0 ~17 minecraft:packed_mud
setblock ~21 ~0 ~18 minecraft:coarse_dirt
setblock ~22 ~0 ~0 minecraft:gravel
fill ~22 ~0 ~1 ~22 ~0 ~2 minecraft:packed_mud
fill ~22 ~0 ~3 ~24 ~0 ~3 minecraft:coarse_dirt
setblock ~22 ~0 ~6 minecraft:packed_mud
fill ~22 ~0 ~7 ~23 ~0 ~7 minecraft:cobblestone
fill ~22 ~0 ~8 ~23 ~0 ~8 minecraft:packed_mud
setblock ~22 ~0 ~10 minecraft:packed_mud
fill ~22 ~0 ~11 ~23 ~0 ~11 minecraft:rooted_dirt
fill ~22 ~0 ~12 ~22 ~0 ~13 minecraft:packed_mud
setblock ~22 ~0 ~14 minecraft:gravel
setblock ~22 ~0 ~18 minecraft:packed_mud
setblock ~23 ~0 ~0 minecraft:packed_mud
setblock ~23 ~0 ~1 minecraft:gravel
fill ~23 ~0 ~2 ~24 ~0 ~2 minecraft:packed_mud
setblock ~23 ~0 ~4 minecraft:cobblestone
fill ~23 ~0 ~5 ~23 ~0 ~6 minecraft:packed_mud
fill ~23 ~0 ~9 ~23 ~0 ~10 minecraft:coarse_dirt
setblock ~23 ~0 ~12 minecraft:coarse_dirt
fill ~23 ~0 ~13 ~24 ~0 ~13 minecraft:gravel
setblock ~23 ~0 ~14 minecraft:coarse_dirt
setblock ~23 ~0 ~15 minecraft:gravel
setblock ~23 ~0 ~16 minecraft:coarse_dirt
setblock ~23 ~0 ~18 minecraft:gravel
setblock ~24 ~0 ~0 minecraft:gravel
setblock ~24 ~0 ~1 minecraft:rooted_dirt
fill ~24 ~0 ~4 ~24 ~0 ~5 minecraft:gravel
setblock ~24 ~0 ~6 minecraft:cobblestone
fill ~24 ~0 ~7 ~24 ~0 ~8 minecraft:coarse_dirt
fill ~24 ~0 ~9 ~24 ~0 ~10 minecraft:packed_mud
setblock ~24 ~0 ~11 minecraft:gravel
setblock ~24 ~0 ~12 minecraft:packed_mud
fill ~24 ~0 ~14 ~24 ~0 ~15 minecraft:packed_mud
setblock ~24 ~0 ~16 minecraft:rooted_dirt
setblock ~24 ~0 ~17 minecraft:gravel
setblock ~24 ~0 ~18 minecraft:packed_mud

# the walls: a cobble sill at y 1 under weathered boards (daub in the middle bays of the south wall), y 2..4
setblock ~1 ~1 ~2 minecraft:cobblestone
setblock ~1 ~1 ~3 minecraft:mossy_cobblestone
setblock ~1 ~1 ~5 minecraft:cobblestone
setblock ~1 ~1 ~7 minecraft:cobblestone
fill ~1 ~1 ~9 ~1 ~1 ~10 minecraft:cobblestone
setblock ~2 ~1 ~1 minecraft:andesite
fill ~2 ~1 ~11 ~3 ~1 ~11 minecraft:cobblestone
setblock ~3 ~1 ~1 minecraft:mossy_cobblestone
fill ~5 ~1 ~1 ~6 ~1 ~1 minecraft:cobblestone
setblock ~5 ~1 ~11 minecraft:cobblestone
setblock ~7 ~1 ~1 minecraft:andesite
fill ~9 ~1 ~1 ~11 ~1 ~1 minecraft:cobblestone
setblock ~11 ~1 ~11 minecraft:andesite
fill ~13 ~1 ~1 ~14 ~1 ~1 minecraft:cobblestone
setblock ~13 ~1 ~11 minecraft:mossy_cobblestone
setblock ~14 ~1 ~11 minecraft:cobblestone
fill ~15 ~1 ~2 ~15 ~1 ~3 minecraft:andesite
setblock ~15 ~1 ~9 minecraft:cobblestone
setblock ~15 ~1 ~10 minecraft:mossy_cobblestone
fill ~1 ~2 ~2 ~1 ~4 ~2 minecraft:spruce_planks
setblock ~1 ~2 ~3 minecraft:stripped_spruce_wood
setblock ~1 ~2 ~5 minecraft:dark_oak_planks
fill ~1 ~2 ~7 ~1 ~3 ~7 minecraft:spruce_planks
setblock ~1 ~2 ~9 minecraft:stripped_spruce_wood
setblock ~1 ~2 ~10 minecraft:dark_oak_planks
setblock ~2 ~2 ~1 minecraft:spruce_planks
setblock ~2 ~2 ~11 minecraft:packed_mud
setblock ~3 ~2 ~1 minecraft:dark_oak_planks
setblock ~3 ~2 ~11 minecraft:mud_bricks
fill ~5 ~2 ~1 ~6 ~2 ~1 minecraft:spruce_planks
fill ~5 ~2 ~11 ~5 ~4 ~11 minecraft:packed_mud
setblock ~7 ~2 ~1 minecraft:stripped_spruce_wood
setblock ~9 ~2 ~1 minecraft:dark_oak_planks
setblock ~10 ~2 ~1 minecraft:spruce_planks
fill ~11 ~2 ~1 ~11 ~4 ~1 minecraft:oak_planks
setblock ~11 ~2 ~11 minecraft:packed_mud
fill ~13 ~2 ~1 ~13 ~3 ~1 minecraft:dark_oak_planks
fill ~13 ~2 ~11 ~14 ~2 ~11 minecraft:packed_mud
setblock ~14 ~2 ~1 minecraft:oak_planks
setblock ~15 ~2 ~2 minecraft:oak_planks
setblock ~15 ~2 ~3 minecraft:dark_oak_planks
setblock ~15 ~2 ~9 minecraft:dark_oak_planks
fill ~15 ~2 ~10 ~15 ~3 ~10 minecraft:spruce_planks
fill ~1 ~3 ~5 ~1 ~4 ~5 minecraft:spruce_planks
setblock ~1 ~3 ~10 minecraft:oak_planks
setblock ~3 ~3 ~1 minecraft:spruce_planks
fill ~3 ~3 ~11 ~3 ~4 ~11 minecraft:packed_mud
setblock ~5 ~3 ~1 minecraft:spruce_planks
fill ~7 ~3 ~1 ~7 ~4 ~1 minecraft:spruce_planks
setblock ~9 ~3 ~1 minecraft:spruce_planks
fill ~11 ~3 ~11 ~11 ~4 ~11 minecraft:mud_bricks
fill ~13 ~3 ~11 ~13 ~4 ~11 minecraft:packed_mud
fill ~15 ~3 ~2 ~15 ~4 ~2 minecraft:dark_oak_planks
setblock ~15 ~3 ~3 minecraft:spruce_planks
fill ~15 ~3 ~9 ~15 ~4 ~9 minecraft:spruce_planks
setblock ~1 ~4 ~7 minecraft:oak_planks
setblock ~1 ~4 ~10 minecraft:spruce_planks
setblock ~2 ~4 ~1 minecraft:spruce_planks
setblock ~2 ~4 ~11 minecraft:packed_mud
setblock ~5 ~4 ~1 minecraft:stripped_spruce_wood
setblock ~9 ~4 ~1 minecraft:dark_oak_planks
setblock ~14 ~4 ~1 minecraft:dark_oak_planks
setblock ~14 ~4 ~11 minecraft:packed_mud
setblock ~15 ~4 ~3 minecraft:oak_planks
setblock ~15 ~4 ~10 minecraft:dark_oak_planks

# the frame: dark oak corner posts, posts at the door jambs and between, the wall plate at y 5
fill ~1 ~1 ~1 ~1 ~4 ~1 minecraft:dark_oak_log[axis=y]
fill ~1 ~1 ~4 ~1 ~4 ~4 minecraft:dark_oak_log[axis=y]
fill ~1 ~1 ~6 ~1 ~4 ~6 minecraft:dark_oak_log[axis=y]
fill ~1 ~1 ~8 ~1 ~4 ~8 minecraft:dark_oak_log[axis=y]
fill ~1 ~1 ~11 ~1 ~4 ~11 minecraft:dark_oak_log[axis=y]
fill ~4 ~1 ~1 ~4 ~4 ~1 minecraft:dark_oak_log[axis=y]
fill ~4 ~1 ~11 ~4 ~4 ~11 minecraft:dark_oak_log[axis=y]
fill ~6 ~1 ~11 ~6 ~4 ~11 minecraft:dark_oak_log[axis=y]
fill ~8 ~1 ~1 ~8 ~4 ~1 minecraft:dark_oak_log[axis=y]
fill ~10 ~1 ~11 ~10 ~4 ~11 minecraft:dark_oak_log[axis=y]
fill ~12 ~1 ~1 ~12 ~4 ~1 minecraft:dark_oak_log[axis=y]
fill ~12 ~1 ~11 ~12 ~4 ~11 minecraft:dark_oak_log[axis=y]
fill ~15 ~1 ~1 ~15 ~4 ~1 minecraft:dark_oak_log[axis=y]
fill ~15 ~1 ~4 ~15 ~4 ~4 minecraft:dark_oak_log[axis=y]
fill ~15 ~1 ~8 ~15 ~4 ~8 minecraft:dark_oak_log[axis=y]
fill ~15 ~1 ~11 ~15 ~4 ~11 minecraft:dark_oak_log[axis=y]
fill ~1 ~5 ~1 ~15 ~5 ~1 minecraft:dark_oak_log[axis=x]
fill ~1 ~5 ~2 ~1 ~5 ~10 minecraft:dark_oak_log[axis=z]
fill ~1 ~5 ~11 ~15 ~5 ~11 minecraft:dark_oak_log[axis=x]
fill ~15 ~5 ~2 ~15 ~5 ~10 minecraft:dark_oak_log[axis=z]

# braces: dark oak logs set across the corner bays of the long walls, a diagonal of two steps at y 3..4
setblock ~2 ~3 ~1 minecraft:dark_oak_log[axis=x]
setblock ~2 ~3 ~11 minecraft:dark_oak_log[axis=x]
setblock ~14 ~3 ~1 minecraft:dark_oak_log[axis=x]
setblock ~14 ~3 ~11 minecraft:dark_oak_log[axis=x]
setblock ~3 ~4 ~1 minecraft:dark_oak_log[axis=x]
setblock ~13 ~4 ~1 minecraft:dark_oak_log[axis=x]

# the doors: an opening three wide and three high in the east gable and the south wall, a dark oak lintel at y 4, shutters swung open beside each, a stone sill
fill ~7 ~0 ~11 ~9 ~0 ~11 minecraft:stone_bricks
fill ~15 ~0 ~5 ~15 ~0 ~7 minecraft:stone_bricks
fill ~6 ~1 ~12 ~6 ~3 ~12 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
fill ~7 ~1 ~11 ~9 ~3 ~11 minecraft:air
fill ~10 ~1 ~12 ~10 ~3 ~12 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
fill ~15 ~1 ~5 ~15 ~3 ~7 minecraft:air
fill ~16 ~1 ~4 ~16 ~3 ~4 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
fill ~16 ~1 ~8 ~16 ~3 ~8 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
fill ~7 ~4 ~11 ~9 ~4 ~11 minecraft:dark_oak_log[axis=x]
fill ~15 ~4 ~5 ~15 ~4 ~7 minecraft:dark_oak_log[axis=z]

# vents: iron bars two blocks high in the north and west walls, a slit pair in the south wall
setblock ~1 ~3 ~3 minecraft:iron_bars[north=true,east=true,south=true,west=false]
setblock ~1 ~3 ~9 minecraft:iron_bars[north=true,east=true,south=true,west=false]
setblock ~6 ~3 ~1 minecraft:iron_bars[north=false,east=true,south=true,west=true]
fill ~10 ~3 ~1 ~10 ~4 ~1 minecraft:iron_bars[north=false,east=true,south=false,west=true]
setblock ~1 ~4 ~3 minecraft:iron_bars[north=true,east=false,south=true,west=false]
setblock ~1 ~4 ~9 minecraft:iron_bars[north=true,east=false,south=true,west=false]
setblock ~6 ~4 ~1 minecraft:iron_bars[north=false,east=true,south=false,west=true]

# the roof: dark oak stairs from the eaves at y 5 (z 0 and 12) to a ridge of dark oak planks at y 11 (z 6), over x 0..16, a block proud of both gables
fill ~0 ~5 ~0 ~16 ~5 ~0 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~5 ~12 ~16 ~5 ~12 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~0 ~6 ~1 ~7 ~6 ~1 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~6 ~11 ~5 ~6 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~6 ~6 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~7 ~6 ~11 ~16 ~6 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~8 ~6 ~1 minecraft:cobblestone_stairs[facing=south,half=bottom]
fill ~9 ~6 ~1 ~16 ~6 ~1 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~7 ~2 ~2 ~7 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~7 ~10 ~11 ~7 ~10 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~3 ~7 ~2 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~4 ~7 ~2 ~16 ~7 ~2 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~12 ~7 ~10 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~13 ~7 ~10 ~16 ~7 ~10 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~0 ~8 ~3 ~13 ~8 ~3 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~8 ~9 ~3 ~8 ~9 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~4 ~8 ~9 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~5 ~8 ~9 ~16 ~8 ~9 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~14 ~8 ~3 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~15 ~8 ~3 ~16 ~8 ~3 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~9 ~4 ~9 ~9 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~9 ~8 ~16 ~9 ~8 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~10 ~9 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~11 ~9 ~4 ~16 ~9 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~10 ~5 ~16 ~10 ~5 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~0 ~10 ~7 ~16 ~10 ~7 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~0 ~11 ~6 ~16 ~11 ~6 minecraft:dark_oak_planks

# the gables: boards up to the roof on a collar beam at y 6, a king post above; a loft door in the east gable; the west gable has a round vent
fill ~1 ~6 ~2 ~1 ~6 ~10 minecraft:dark_oak_log[axis=z]
fill ~15 ~6 ~2 ~15 ~6 ~10 minecraft:dark_oak_log[axis=z]
setblock ~1 ~7 ~3 minecraft:oak_planks
setblock ~1 ~7 ~4 minecraft:iron_bars[north=true,east=false,south=true,west=false]
setblock ~1 ~7 ~5 minecraft:dark_oak_planks
fill ~1 ~7 ~6 ~1 ~10 ~6 minecraft:dark_oak_log[axis=y]
fill ~1 ~7 ~7 ~1 ~9 ~7 minecraft:spruce_planks
setblock ~1 ~7 ~8 minecraft:iron_bars[north=true,east=false,south=true,west=false]
setblock ~1 ~7 ~9 minecraft:spruce_planks
setblock ~15 ~7 ~3 minecraft:stripped_spruce_wood
setblock ~15 ~7 ~4 minecraft:oak_planks
fill ~15 ~7 ~5 ~15 ~9 ~7 minecraft:air
fill ~15 ~7 ~8 ~15 ~8 ~8 minecraft:oak_planks
setblock ~15 ~7 ~9 minecraft:spruce_planks
setblock ~1 ~8 ~4 minecraft:iron_bars[north=false,east=false,south=true,west=false]
fill ~1 ~8 ~5 ~1 ~9 ~5 minecraft:spruce_planks
setblock ~1 ~8 ~8 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~15 ~8 ~4 minecraft:dark_oak_planks
setblock ~15 ~10 ~6 minecraft:dark_oak_log[axis=y]

# the eaves: a soffit of upside-down dark oak stairs under each eave, the length of the roof; the ridge cap of dark oak slabs and a few patched shingles
fill ~0 ~4 ~0 ~16 ~4 ~0 minecraft:dark_oak_stairs[facing=south,half=top]
fill ~0 ~4 ~12 ~16 ~4 ~12 minecraft:dark_oak_stairs[facing=north,half=top]
fill ~0 ~12 ~6 ~16 ~12 ~6 minecraft:dark_oak_slab[type=bottom]

# shutters at the loft door: trapdoors open against the gable either side, a brace under the sill
fill ~16 ~7 ~4 ~16 ~8 ~4 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]
fill ~16 ~7 ~8 ~16 ~8 ~8 minecraft:spruce_trapdoor[facing=east,half=bottom,open=true]

# the hoist: a dark oak beam from the ridge end (x 16..19, y 10, z 6) with a chain and a hoisted bale, a post brace below
setblock ~19 ~5 ~6 minecraft:hay_block[axis=x]
fill ~19 ~6 ~6 ~19 ~9 ~6 minecraft:chain[axis=y]
setblock ~17 ~9 ~6 minecraft:dark_oak_stairs[facing=west,half=top]
setblock ~18 ~9 ~6 minecraft:air
fill ~16 ~10 ~6 ~19 ~10 ~6 minecraft:dark_oak_log[axis=x]

# the hay loft: a floor of spruce planks over x 11..14, z 3..9 on a front beam at x 10 and two posts; the ladder at (14, 1..7, 8) against the east gable
fill ~10 ~1 ~3 ~10 ~5 ~3 minecraft:dark_oak_log[axis=y]
fill ~10 ~1 ~9 ~10 ~5 ~9 minecraft:dark_oak_log[axis=y]
fill ~14 ~1 ~8 ~14 ~7 ~8 minecraft:ladder[facing=west]
fill ~10 ~6 ~3 ~10 ~6 ~9 minecraft:dark_oak_log[axis=z]
fill ~11 ~6 ~3 ~14 ~6 ~7 minecraft:spruce_planks
fill ~11 ~6 ~8 ~13 ~6 ~9 minecraft:spruce_planks
setblock ~14 ~6 ~9 minecraft:spruce_planks

# hay on the loft and a lantern: bales along the loft sides (low, under the roof), a lantern on a chain from the king post line
fill ~11 ~7 ~3 ~13 ~7 ~3 minecraft:hay_block[axis=x]
setblock ~11 ~7 ~9 minecraft:hay_block[axis=x]
setblock ~12 ~7 ~4 minecraft:hay_block[axis=x]
fill ~12 ~7 ~8 ~12 ~7 ~9 minecraft:hay_block[axis=z]
setblock ~13 ~7 ~9 minecraft:hay_block[axis=x]
setblock ~12 ~8 ~8 minecraft:lantern[hanging=false]

# the barn floor: stacked hay in the west half and the corners, barrels and a feed trough on the north wall; the aisle and the south door way stay clear
fill ~2 ~1 ~2 ~2 ~1 ~4 minecraft:hay_block[axis=x]
fill ~2 ~1 ~8 ~2 ~1 ~10 minecraft:hay_block[axis=x]
fill ~3 ~1 ~2 ~3 ~1 ~4 minecraft:hay_block[axis=z]
fill ~3 ~1 ~8 ~3 ~1 ~10 minecraft:hay_block[axis=z]
fill ~4 ~1 ~2 ~4 ~1 ~4 minecraft:hay_block[axis=x]
fill ~4 ~1 ~8 ~4 ~1 ~10 minecraft:hay_block[axis=x]
fill ~5 ~1 ~2 ~5 ~1 ~4 minecraft:hay_block[axis=z]
fill ~5 ~1 ~8 ~5 ~1 ~10 minecraft:hay_block[axis=z]
fill ~6 ~1 ~2 ~6 ~1 ~4 minecraft:hay_block[axis=x]
fill ~6 ~1 ~8 ~6 ~1 ~10 minecraft:hay_block[axis=x]
fill ~7 ~1 ~2 ~7 ~1 ~4 minecraft:hay_block[axis=z]
fill ~8 ~1 ~2 ~8 ~1 ~4 minecraft:hay_block[axis=x]
setblock ~10 ~1 ~10 minecraft:hay_block[axis=y]
setblock ~12 ~1 ~2 minecraft:barrel[facing=up]
setblock ~12 ~1 ~10 minecraft:barrel[facing=up]
setblock ~14 ~1 ~2 minecraft:hay_block[axis=y]
fill ~2 ~2 ~2 ~2 ~2 ~3 minecraft:hay_block[axis=z]
fill ~2 ~2 ~9 ~2 ~2 ~10 minecraft:hay_block[axis=z]
fill ~3 ~2 ~3 ~3 ~2 ~4 minecraft:hay_block[axis=x]
setblock ~3 ~2 ~8 minecraft:hay_block[axis=x]
setblock ~3 ~2 ~10 minecraft:hay_block[axis=x]
setblock ~4 ~2 ~2 minecraft:hay_block[axis=z]
fill ~4 ~2 ~8 ~4 ~2 ~10 minecraft:hay_block[axis=z]
fill ~5 ~2 ~8 ~5 ~2 ~10 minecraft:hay_block[axis=x]
setblock ~6 ~2 ~2 minecraft:hay_block[axis=z]
setblock ~6 ~2 ~4 minecraft:hay_block[axis=z]
setblock ~6 ~2 ~10 minecraft:hay_block[axis=z]
setblock ~7 ~2 ~3 minecraft:hay_block[axis=x]
setblock ~2 ~3 ~3 minecraft:hay_block[axis=x]
setblock ~2 ~3 ~9 minecraft:hay_block[axis=x]
fill ~3 ~3 ~3 ~3 ~3 ~4 minecraft:hay_block[axis=z]
setblock ~3 ~3 ~10 minecraft:hay_block[axis=z]
fill ~4 ~3 ~8 ~4 ~3 ~10 minecraft:hay_block[axis=x]
fill ~5 ~3 ~8 ~5 ~3 ~9 minecraft:hay_block[axis=z]
setblock ~6 ~3 ~2 minecraft:hay_block[axis=x]
setblock ~6 ~3 ~10 minecraft:hay_block[axis=x]
setblock ~7 ~3 ~3 minecraft:hay_block[axis=z]

# a tie beam across the barn at y 5 over the aisle, lanterns on chains
setblock ~5 ~4 ~6 minecraft:lantern[hanging=true]
setblock ~12 ~4 ~6 minecraft:lantern[hanging=true]
fill ~5 ~5 ~2 ~5 ~5 ~10 minecraft:dark_oak_log[axis=z]
fill ~12 ~5 ~2 ~12 ~5 ~10 minecraft:dark_oak_log[axis=z]

# the fence round the yard: dark oak fences, gate posts of logs with lanterns at the south gate (x 10 and 14) and the east gate (z 4 and 8)
setblock ~0 ~1 ~13 minecraft:dark_oak_log[axis=y]
fill ~0 ~1 ~14 ~0 ~1 ~16 minecraft:dark_oak_fence[north=true,east=true,south=true,west=false]
setblock ~0 ~1 ~17 minecraft:dark_oak_fence[north=true,east=false,south=true,west=false]
setblock ~0 ~1 ~18 minecraft:dark_oak_log[axis=y]
fill ~1 ~1 ~18 ~9 ~1 ~18 minecraft:dark_oak_fence[north=false,east=true,south=false,west=true]
fill ~10 ~1 ~18 ~10 ~3 ~18 minecraft:dark_oak_log[axis=y]
fill ~14 ~1 ~18 ~14 ~3 ~18 minecraft:dark_oak_log[axis=y]
fill ~15 ~1 ~18 ~18 ~1 ~18 minecraft:dark_oak_fence[north=false,east=true,south=false,west=true]
setblock ~16 ~1 ~0 minecraft:dark_oak_log[axis=y]
fill ~17 ~1 ~0 ~23 ~1 ~0 minecraft:dark_oak_fence[north=false,east=true,south=false,west=true]
fill ~19 ~1 ~18 ~20 ~1 ~18 minecraft:dark_oak_fence[north=true,east=true,south=false,west=true]
fill ~21 ~1 ~18 ~23 ~1 ~18 minecraft:dark_oak_fence[north=false,east=true,south=false,west=true]
setblock ~24 ~1 ~0 minecraft:dark_oak_log[axis=y]
fill ~24 ~1 ~1 ~24 ~1 ~3 minecraft:dark_oak_fence[north=true,east=false,south=true,west=false]
fill ~24 ~1 ~4 ~24 ~3 ~4 minecraft:dark_oak_log[axis=y]
fill ~24 ~1 ~8 ~24 ~3 ~8 minecraft:dark_oak_log[axis=y]
fill ~24 ~1 ~9 ~24 ~1 ~13 minecraft:dark_oak_fence[north=true,east=false,south=true,west=false]
fill ~24 ~1 ~14 ~24 ~1 ~16 minecraft:dark_oak_fence[north=true,east=false,south=true,west=true]
setblock ~24 ~1 ~17 minecraft:dark_oak_fence[north=true,east=false,south=true,west=false]
setblock ~24 ~1 ~18 minecraft:dark_oak_log[axis=y]
setblock ~0 ~2 ~13 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~0 ~2 ~18 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~16 ~2 ~0 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~24 ~2 ~0 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~24 ~2 ~18 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~10 ~4 ~18 minecraft:lantern[hanging=false]
setblock ~14 ~4 ~18 minecraft:lantern[hanging=false]
setblock ~24 ~4 ~4 minecraft:lantern[hanging=false]
setblock ~24 ~4 ~8 minecraft:lantern[hanging=false]

# four stacks of hay in the south strip (3 x 3, two high, the top course turned, a bale less at a corner), a taller one by the east wall
setblock ~1 ~1 ~14 minecraft:hay_block[axis=z]
setblock ~1 ~1 ~15 minecraft:hay_block[axis=x]
setblock ~1 ~1 ~16 minecraft:hay_block[axis=z]
setblock ~2 ~1 ~14 minecraft:hay_block[axis=x]
setblock ~2 ~1 ~15 minecraft:hay_block[axis=z]
setblock ~2 ~1 ~16 minecraft:hay_block[axis=x]
setblock ~3 ~1 ~14 minecraft:hay_block[axis=z]
setblock ~3 ~1 ~15 minecraft:hay_block[axis=x]
setblock ~3 ~1 ~16 minecraft:hay_block[axis=z]
setblock ~5 ~1 ~14 minecraft:hay_block[axis=x]
setblock ~5 ~1 ~15 minecraft:hay_block[axis=z]
setblock ~5 ~1 ~16 minecraft:hay_block[axis=x]
setblock ~6 ~1 ~14 minecraft:hay_block[axis=z]
setblock ~6 ~1 ~15 minecraft:hay_block[axis=x]
setblock ~6 ~1 ~16 minecraft:hay_block[axis=z]
setblock ~7 ~1 ~14 minecraft:hay_block[axis=x]
setblock ~7 ~1 ~15 minecraft:hay_block[axis=z]
setblock ~7 ~1 ~16 minecraft:hay_block[axis=x]
setblock ~16 ~1 ~14 minecraft:hay_block[axis=z]
setblock ~16 ~1 ~15 minecraft:hay_block[axis=x]
setblock ~16 ~1 ~16 minecraft:hay_block[axis=z]
setblock ~17 ~1 ~14 minecraft:hay_block[axis=x]
setblock ~17 ~1 ~15 minecraft:hay_block[axis=z]
setblock ~17 ~1 ~16 minecraft:hay_block[axis=x]
setblock ~18 ~1 ~14 minecraft:hay_block[axis=z]
setblock ~18 ~1 ~15 minecraft:hay_block[axis=x]
setblock ~18 ~1 ~16 minecraft:hay_block[axis=z]
fill ~19 ~1 ~17 ~20 ~1 ~17 minecraft:hay_block[axis=x]
setblock ~21 ~1 ~14 minecraft:hay_block[axis=x]
setblock ~21 ~1 ~15 minecraft:hay_block[axis=z]
setblock ~21 ~1 ~16 minecraft:hay_block[axis=x]
setblock ~22 ~1 ~14 minecraft:hay_block[axis=z]
setblock ~22 ~1 ~15 minecraft:hay_block[axis=x]
setblock ~22 ~1 ~16 minecraft:hay_block[axis=z]
setblock ~23 ~1 ~14 minecraft:hay_block[axis=x]
setblock ~23 ~1 ~15 minecraft:hay_block[axis=z]
setblock ~23 ~1 ~16 minecraft:hay_block[axis=x]
setblock ~1 ~2 ~14 minecraft:hay_block[axis=x]
setblock ~1 ~2 ~15 minecraft:hay_block[axis=z]
setblock ~1 ~2 ~16 minecraft:hay_block[axis=x]
setblock ~2 ~2 ~14 minecraft:hay_block[axis=z]
setblock ~2 ~2 ~15 minecraft:hay_block[axis=x]
setblock ~2 ~2 ~16 minecraft:hay_block[axis=z]
setblock ~3 ~2 ~14 minecraft:hay_block[axis=x]
setblock ~3 ~2 ~15 minecraft:hay_block[axis=z]
setblock ~5 ~2 ~14 minecraft:hay_block[axis=z]
setblock ~5 ~2 ~15 minecraft:hay_block[axis=x]
setblock ~5 ~2 ~16 minecraft:hay_block[axis=z]
setblock ~6 ~2 ~14 minecraft:hay_block[axis=x]
setblock ~6 ~2 ~15 minecraft:hay_block[axis=z]
setblock ~6 ~2 ~16 minecraft:hay_block[axis=x]
setblock ~7 ~2 ~14 minecraft:hay_block[axis=z]
setblock ~7 ~2 ~15 minecraft:hay_block[axis=x]
setblock ~7 ~2 ~16 minecraft:hay_block[axis=z]
setblock ~16 ~2 ~14 minecraft:hay_block[axis=x]
setblock ~16 ~2 ~15 minecraft:hay_block[axis=z]
setblock ~16 ~2 ~16 minecraft:hay_block[axis=x]
setblock ~17 ~2 ~14 minecraft:hay_block[axis=z]
setblock ~17 ~2 ~15 minecraft:hay_block[axis=x]
setblock ~17 ~2 ~16 minecraft:hay_block[axis=z]
setblock ~18 ~2 ~14 minecraft:hay_block[axis=x]
setblock ~18 ~2 ~15 minecraft:hay_block[axis=z]
setblock ~21 ~2 ~14 minecraft:hay_block[axis=z]
setblock ~21 ~2 ~15 minecraft:hay_block[axis=x]
setblock ~21 ~2 ~16 minecraft:hay_block[axis=z]
setblock ~22 ~2 ~14 minecraft:hay_block[axis=x]
setblock ~22 ~2 ~15 minecraft:hay_block[axis=z]
setblock ~22 ~2 ~16 minecraft:hay_block[axis=x]
setblock ~23 ~2 ~14 minecraft:hay_block[axis=z]
setblock ~23 ~2 ~15 minecraft:hay_block[axis=x]
setblock ~23 ~2 ~16 minecraft:hay_block[axis=z]
setblock ~6 ~3 ~15 minecraft:hay_block[axis=y]
setblock ~17 ~3 ~15 minecraft:hay_block[axis=y]

# the cart: a plank bed on four fence legs with two wheels of trapdoors, hay piled on it, a tongue of two stairs
setblock ~17 ~1 ~3 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~19 ~1 ~2 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~19 ~1 ~4 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~20 ~1 ~1 ~21 ~1 ~1 minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]
fill ~20 ~1 ~5 ~21 ~1 ~5 minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]
setblock ~22 ~1 ~2 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~22 ~1 ~4 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~18 ~2 ~3 minecraft:spruce_stairs[facing=east,half=bottom]
fill ~19 ~2 ~2 ~22 ~2 ~4 minecraft:spruce_planks
fill ~19 ~3 ~2 ~21 ~3 ~4 minecraft:hay_block[axis=x]
setblock ~20 ~4 ~3 minecraft:hay_block[axis=z]

# barrels and a rain barrel at the barn corners, a woodpile under the north eave, moss at the foot of the north wall
setblock ~5 ~1 ~0 minecraft:moss_carpet
fill ~6 ~1 ~0 ~9 ~1 ~0 minecraft:oak_log[axis=x]
fill ~10 ~1 ~0 ~11 ~1 ~0 minecraft:moss_carpet
fill ~13 ~1 ~0 ~14 ~1 ~0 minecraft:moss_carpet
setblock ~16 ~1 ~12 minecraft:barrel[facing=up]
setblock ~17 ~1 ~10 minecraft:cauldron
fill ~17 ~1 ~11 ~17 ~1 ~12 minecraft:barrel[facing=up]
fill ~7 ~2 ~0 ~8 ~2 ~0 minecraft:oak_log[axis=x]

# the loft door sill and a pulley frame inside the loft door: dark oak fences either side
setblock ~14 ~7 ~5 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~14 ~7 ~7 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
