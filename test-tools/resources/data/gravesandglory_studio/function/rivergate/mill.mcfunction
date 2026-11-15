# size 19 16 21
# rivergate/mill: the mill at the north crossing: a stone ground storey, an oak and spruce timber upper storey with a loft, a water wheel over its race, the walled L-shaped yard.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Mill x 8..16, z 0..12; wheel x 17..18, z 3..9 (axle at y 4, z 6) over the race (water at y 0, x 17..18, z 2..10); doors west (x 8, z 9..11) and south (z 12, x 12..14), y 1..3.
# Loft floor y 7 (feet 8), south window x 14..15, y 9..10: from the loft (14.5, 9.6, 11.5) the eye sees the north bridge deck. Cover: from the yard (11.5, 2.6, 17.5) the south wall hides a man from the deck.
# Yard x 0..7 for z 0..20 and z 13..20 for x 0..18, a 2-high wall on its outer sides; gate x 4..8 at z 20 (the mill lane).

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~1 ~1 ~1 ~7 ~9 ~1 minecraft:air
fill ~1 ~1 ~4 ~1 ~15 ~5 minecraft:air
fill ~1 ~1 ~8 ~1 ~15 ~9 minecraft:air
fill ~1 ~1 ~12 ~2 ~15 ~14 minecraft:air
fill ~1 ~1 ~17 ~2 ~15 ~19 minecraft:air
fill ~2 ~1 ~3 ~2 ~15 ~5 minecraft:air
fill ~2 ~1 ~7 ~2 ~15 ~10 minecraft:air
fill ~3 ~1 ~2 ~4 ~15 ~19 minecraft:air
fill ~4 ~1 ~20 ~4 ~15 ~20 minecraft:air
fill ~5 ~1 ~3 ~6 ~15 ~20 minecraft:air
fill ~7 ~1 ~2 ~7 ~9 ~20 minecraft:air
fill ~8 ~1 ~13 ~17 ~10 ~14 minecraft:air
fill ~8 ~1 ~15 ~15 ~15 ~17 minecraft:air
fill ~8 ~1 ~18 ~12 ~15 ~19 minecraft:air
fill ~8 ~1 ~20 ~8 ~15 ~20 minecraft:air
fill ~9 ~1 ~1 ~9 ~6 ~1 minecraft:air
fill ~9 ~1 ~4 ~9 ~6 ~5 minecraft:air
fill ~9 ~1 ~7 ~9 ~6 ~11 minecraft:air
fill ~10 ~1 ~2 ~10 ~6 ~11 minecraft:air
fill ~11 ~1 ~1 ~14 ~6 ~5 minecraft:air
fill ~11 ~1 ~6 ~11 ~6 ~11 minecraft:air
fill ~12 ~1 ~7 ~15 ~6 ~8 minecraft:air
fill ~12 ~1 ~9 ~14 ~6 ~11 minecraft:air
fill ~13 ~1 ~19 ~15 ~15 ~19 minecraft:air
fill ~15 ~1 ~3 ~15 ~6 ~5 minecraft:air
fill ~16 ~1 ~16 ~16 ~15 ~19 minecraft:air
fill ~17 ~1 ~0 ~18 ~1 ~4 minecraft:air
fill ~17 ~1 ~8 ~18 ~1 ~12 minecraft:air
fill ~17 ~1 ~17 ~17 ~15 ~19 minecraft:air
fill ~1 ~2 ~3 ~1 ~15 ~3 minecraft:air
fill ~1 ~2 ~6 ~2 ~15 ~6 minecraft:air
fill ~1 ~2 ~7 ~1 ~15 ~7 minecraft:air
fill ~1 ~2 ~10 ~1 ~15 ~11 minecraft:air
fill ~1 ~2 ~15 ~2 ~15 ~16 minecraft:air
fill ~2 ~2 ~2 ~2 ~15 ~2 minecraft:air
fill ~2 ~2 ~11 ~2 ~15 ~11 minecraft:air
fill ~5 ~2 ~2 ~6 ~15 ~2 minecraft:air
fill ~9 ~2 ~2 ~9 ~6 ~3 minecraft:air
fill ~9 ~2 ~6 ~9 ~6 ~6 minecraft:air
fill ~12 ~2 ~6 ~15 ~5 ~6 minecraft:air
fill ~13 ~2 ~18 ~15 ~15 ~18 minecraft:air
fill ~15 ~2 ~1 ~15 ~6 ~2 minecraft:air
fill ~15 ~2 ~9 ~15 ~6 ~9 minecraft:air
fill ~15 ~2 ~11 ~15 ~6 ~11 minecraft:air
fill ~16 ~2 ~15 ~16 ~15 ~15 minecraft:air
fill ~17 ~2 ~0 ~18 ~2 ~3 minecraft:air
fill ~17 ~2 ~5 ~18 ~3 ~5 minecraft:air
fill ~17 ~2 ~7 ~17 ~3 ~7 minecraft:air
fill ~17 ~2 ~9 ~18 ~2 ~12 minecraft:air
fill ~17 ~2 ~16 ~17 ~15 ~16 minecraft:air
fill ~18 ~2 ~6 ~18 ~3 ~7 minecraft:air
fill ~0 ~3 ~0 ~7 ~9 ~0 minecraft:air
fill ~0 ~3 ~1 ~0 ~15 ~20 minecraft:air
fill ~1 ~3 ~2 ~1 ~15 ~2 minecraft:air
fill ~1 ~3 ~20 ~2 ~15 ~20 minecraft:air
fill ~10 ~3 ~20 ~17 ~15 ~20 minecraft:air
fill ~15 ~3 ~10 ~15 ~6 ~10 minecraft:air
fill ~17 ~3 ~0 ~18 ~9 ~2 minecraft:air
fill ~17 ~3 ~4 ~18 ~3 ~4 minecraft:air
fill ~17 ~3 ~8 ~18 ~3 ~8 minecraft:air
fill ~17 ~3 ~10 ~18 ~9 ~12 minecraft:air
fill ~17 ~3 ~15 ~17 ~15 ~15 minecraft:air
fill ~18 ~3 ~13 ~18 ~15 ~20 minecraft:air
fill ~18 ~4 ~4 ~18 ~5 ~5 minecraft:air
fill ~18 ~4 ~7 ~18 ~5 ~8 minecraft:air
fill ~3 ~5 ~20 ~3 ~15 ~20 minecraft:air
fill ~9 ~5 ~20 ~9 ~15 ~20 minecraft:air
fill ~17 ~5 ~4 ~17 ~5 ~5 minecraft:air
fill ~17 ~5 ~7 ~17 ~5 ~8 minecraft:air
fill ~18 ~5 ~6 ~18 ~6 ~6 minecraft:air
fill ~13 ~6 ~6 ~15 ~6 ~6 minecraft:air
fill ~17 ~6 ~3 ~18 ~9 ~3 minecraft:air
fill ~17 ~6 ~5 ~18 ~6 ~5 minecraft:air
fill ~17 ~6 ~7 ~18 ~6 ~7 minecraft:air
fill ~17 ~6 ~9 ~18 ~9 ~9 minecraft:air
fill ~17 ~7 ~4 ~18 ~9 ~4 minecraft:air
fill ~17 ~7 ~8 ~18 ~9 ~8 minecraft:air
fill ~9 ~8 ~1 ~15 ~11 ~1 minecraft:air
fill ~9 ~8 ~2 ~14 ~11 ~2 minecraft:air
fill ~9 ~8 ~5 ~10 ~11 ~8 minecraft:air
fill ~9 ~8 ~10 ~9 ~11 ~11 minecraft:air
fill ~10 ~8 ~3 ~14 ~12 ~3 minecraft:air
fill ~10 ~8 ~9 ~10 ~12 ~9 minecraft:air
fill ~10 ~8 ~11 ~10 ~12 ~11 minecraft:air
fill ~11 ~8 ~4 ~15 ~11 ~11 minecraft:air
fill ~17 ~8 ~5 ~18 ~9 ~7 minecraft:air
fill ~9 ~9 ~3 ~9 ~11 ~4 minecraft:air
fill ~9 ~9 ~9 ~9 ~11 ~9 minecraft:air
fill ~10 ~9 ~4 ~10 ~12 ~4 minecraft:air
fill ~10 ~9 ~10 ~10 ~12 ~10 minecraft:air
fill ~15 ~9 ~2 ~15 ~11 ~3 minecraft:air
fill ~0 ~10 ~0 ~6 ~15 ~0 minecraft:air
fill ~1 ~10 ~1 ~6 ~15 ~1 minecraft:air
fill ~7 ~10 ~13 ~7 ~15 ~20 minecraft:air
fill ~18 ~10 ~0 ~18 ~15 ~12 minecraft:air
fill ~7 ~11 ~0 ~7 ~15 ~12 minecraft:air
fill ~8 ~11 ~13 ~11 ~15 ~14 minecraft:air
fill ~12 ~11 ~14 ~12 ~15 ~14 minecraft:air
fill ~13 ~11 ~13 ~16 ~15 ~14 minecraft:air
fill ~17 ~11 ~0 ~17 ~15 ~14 minecraft:air
fill ~8 ~12 ~0 ~8 ~15 ~12 minecraft:air
fill ~10 ~12 ~1 ~14 ~12 ~2 minecraft:air
fill ~10 ~12 ~5 ~10 ~12 ~8 minecraft:air
fill ~11 ~12 ~4 ~14 ~12 ~11 minecraft:air
fill ~16 ~12 ~0 ~16 ~15 ~12 minecraft:air
fill ~9 ~13 ~0 ~9 ~15 ~12 minecraft:air
fill ~11 ~13 ~1 ~13 ~13 ~11 minecraft:air
fill ~15 ~13 ~0 ~15 ~15 ~12 minecraft:air
fill ~10 ~14 ~0 ~10 ~15 ~12 minecraft:air
fill ~12 ~14 ~1 ~12 ~14 ~11 minecraft:air
fill ~12 ~14 ~13 ~12 ~15 ~13 minecraft:air
fill ~14 ~14 ~0 ~14 ~15 ~12 minecraft:air
fill ~11 ~15 ~0 ~11 ~15 ~12 minecraft:air
fill ~13 ~15 ~0 ~13 ~15 ~12 minecraft:air

# the yard floor: cobble, gravel and packed earth, worn to dirt on the lane from the gate to the doors
setblock ~0 ~0 ~0 minecraft:coarse_dirt
setblock ~0 ~0 ~1 minecraft:gravel
fill ~0 ~0 ~2 ~0 ~0 ~3 minecraft:packed_mud
setblock ~0 ~0 ~4 minecraft:gravel
setblock ~0 ~0 ~5 minecraft:coarse_dirt
fill ~0 ~0 ~6 ~1 ~0 ~6 minecraft:cobblestone
fill ~0 ~0 ~7 ~2 ~0 ~7 minecraft:packed_mud
fill ~0 ~0 ~8 ~1 ~0 ~8 minecraft:coarse_dirt
setblock ~0 ~0 ~9 minecraft:coarse_dirt
setblock ~0 ~0 ~10 minecraft:packed_mud
fill ~0 ~0 ~11 ~1 ~0 ~12 minecraft:coarse_dirt
setblock ~0 ~0 ~13 minecraft:packed_mud
setblock ~0 ~0 ~14 minecraft:gravel
setblock ~0 ~0 ~15 minecraft:coarse_dirt
setblock ~0 ~0 ~16 minecraft:mossy_cobblestone
fill ~0 ~0 ~17 ~0 ~0 ~19 minecraft:cobblestone
setblock ~0 ~0 ~20 minecraft:packed_mud
setblock ~1 ~0 ~0 minecraft:mossy_cobblestone
fill ~1 ~0 ~1 ~2 ~0 ~1 minecraft:cobblestone
fill ~1 ~0 ~2 ~1 ~0 ~3 minecraft:coarse_dirt
fill ~1 ~0 ~4 ~2 ~0 ~4 minecraft:cobblestone
setblock ~1 ~0 ~5 minecraft:gravel
setblock ~1 ~0 ~9 minecraft:packed_mud
setblock ~1 ~0 ~10 minecraft:gravel
fill ~1 ~0 ~13 ~1 ~0 ~15 minecraft:cobblestone
fill ~1 ~0 ~16 ~1 ~0 ~17 minecraft:gravel
setblock ~1 ~0 ~18 minecraft:cobblestone
fill ~1 ~0 ~19 ~3 ~0 ~19 minecraft:packed_mud
setblock ~1 ~0 ~20 minecraft:cobblestone
setblock ~2 ~0 ~0 minecraft:coarse_dirt
setblock ~2 ~0 ~2 minecraft:packed_mud
setblock ~2 ~0 ~3 minecraft:coarse_dirt
fill ~2 ~0 ~5 ~3 ~0 ~5 minecraft:cobblestone
fill ~2 ~0 ~6 ~4 ~0 ~6 minecraft:packed_mud
fill ~2 ~0 ~8 ~2 ~0 ~9 minecraft:gravel
fill ~2 ~0 ~10 ~2 ~0 ~12 minecraft:coarse_dirt
setblock ~2 ~0 ~13 minecraft:gravel
setblock ~2 ~0 ~14 minecraft:cobblestone
setblock ~2 ~0 ~15 minecraft:packed_mud
fill ~2 ~0 ~16 ~2 ~0 ~17 minecraft:coarse_dirt
setblock ~2 ~0 ~18 minecraft:gravel
setblock ~2 ~0 ~20 minecraft:coarse_dirt
fill ~3 ~0 ~0 ~3 ~0 ~1 minecraft:packed_mud
setblock ~3 ~0 ~2 minecraft:cobblestone
fill ~3 ~0 ~3 ~4 ~0 ~3 minecraft:packed_mud
setblock ~3 ~0 ~4 minecraft:packed_mud
setblock ~3 ~0 ~7 minecraft:mossy_cobblestone
fill ~3 ~0 ~8 ~6 ~0 ~8 minecraft:coarse_dirt
setblock ~3 ~0 ~9 minecraft:coarse_dirt
fill ~3 ~0 ~10 ~3 ~0 ~11 minecraft:gravel
fill ~3 ~0 ~12 ~3 ~0 ~17 minecraft:coarse_dirt
fill ~3 ~0 ~18 ~4 ~0 ~18 minecraft:packed_mud
setblock ~3 ~0 ~20 minecraft:packed_mud
setblock ~4 ~0 ~0 minecraft:mossy_cobblestone
setblock ~4 ~0 ~1 minecraft:cobblestone
setblock ~4 ~0 ~2 minecraft:coarse_dirt
setblock ~4 ~0 ~4 minecraft:cobblestone
setblock ~4 ~0 ~5 minecraft:gravel
setblock ~4 ~0 ~7 minecraft:packed_mud
setblock ~4 ~0 ~9 minecraft:packed_mud
fill ~4 ~0 ~10 ~4 ~0 ~11 minecraft:coarse_dirt
setblock ~4 ~0 ~12 minecraft:packed_mud
setblock ~4 ~0 ~13 minecraft:gravel
fill ~4 ~0 ~14 ~5 ~0 ~14 minecraft:packed_mud
setblock ~4 ~0 ~15 minecraft:gravel
setblock ~4 ~0 ~16 minecraft:coarse_dirt
setblock ~4 ~0 ~17 minecraft:gravel
fill ~4 ~0 ~19 ~4 ~0 ~20 minecraft:coarse_dirt
fill ~5 ~0 ~0 ~5 ~0 ~1 minecraft:gravel
setblock ~5 ~0 ~2 minecraft:packed_mud
setblock ~5 ~0 ~3 minecraft:mossy_cobblestone
fill ~5 ~0 ~4 ~6 ~0 ~4 minecraft:gravel
fill ~5 ~0 ~5 ~5 ~0 ~6 minecraft:coarse_dirt
setblock ~5 ~0 ~7 minecraft:cobblestone
setblock ~5 ~0 ~9 minecraft:gravel
fill ~5 ~0 ~10 ~7 ~0 ~10 minecraft:packed_mud
fill ~5 ~0 ~11 ~6 ~0 ~11 minecraft:packed_mud
fill ~5 ~0 ~12 ~7 ~0 ~12 minecraft:coarse_dirt
fill ~5 ~0 ~13 ~6 ~0 ~13 minecraft:coarse_dirt
fill ~5 ~0 ~15 ~5 ~0 ~16 minecraft:coarse_dirt
fill ~5 ~0 ~17 ~6 ~0 ~17 minecraft:packed_mud
fill ~5 ~0 ~18 ~5 ~0 ~19 minecraft:coarse_dirt
setblock ~5 ~0 ~20 minecraft:gravel
fill ~6 ~0 ~0 ~7 ~0 ~0 minecraft:cobblestone
setblock ~6 ~0 ~1 minecraft:packed_mud
setblock ~6 ~0 ~2 minecraft:coarse_dirt
setblock ~6 ~0 ~3 minecraft:packed_mud
fill ~6 ~0 ~5 ~7 ~0 ~5 minecraft:cobblestone
fill ~6 ~0 ~6 ~6 ~0 ~7 minecraft:packed_mud
fill ~6 ~0 ~9 ~7 ~0 ~9 minecraft:coarse_dirt
fill ~6 ~0 ~14 ~6 ~0 ~15 minecraft:gravel
setblock ~6 ~0 ~16 minecraft:coarse_dirt
setblock ~6 ~0 ~18 minecraft:packed_mud
setblock ~6 ~0 ~19 minecraft:coarse_dirt
setblock ~6 ~0 ~20 minecraft:packed_mud
setblock ~7 ~0 ~1 minecraft:cobblestone
setblock ~7 ~0 ~2 minecraft:packed_mud
fill ~7 ~0 ~3 ~7 ~0 ~4 minecraft:coarse_dirt
fill ~7 ~0 ~6 ~7 ~0 ~7 minecraft:cobblestone
setblock ~7 ~0 ~8 minecraft:gravel
setblock ~7 ~0 ~11 minecraft:gravel
fill ~7 ~0 ~13 ~7 ~0 ~14 minecraft:packed_mud
fill ~7 ~0 ~15 ~7 ~0 ~18 minecraft:coarse_dirt
setblock ~7 ~0 ~19 minecraft:gravel
setblock ~7 ~0 ~20 minecraft:coarse_dirt
setblock ~8 ~0 ~13 minecraft:coarse_dirt
fill ~8 ~0 ~14 ~9 ~0 ~14 minecraft:packed_mud
setblock ~8 ~0 ~15 minecraft:packed_mud
fill ~8 ~0 ~16 ~10 ~0 ~16 minecraft:coarse_dirt
setblock ~8 ~0 ~17 minecraft:gravel
fill ~8 ~0 ~18 ~8 ~0 ~19 minecraft:coarse_dirt
setblock ~8 ~0 ~20 minecraft:gravel
fill ~9 ~0 ~13 ~10 ~0 ~13 minecraft:packed_mud
fill ~9 ~0 ~15 ~10 ~0 ~15 minecraft:gravel
setblock ~9 ~0 ~17 minecraft:coarse_dirt
setblock ~9 ~0 ~18 minecraft:packed_mud
fill ~9 ~0 ~19 ~9 ~0 ~20 minecraft:coarse_dirt
setblock ~10 ~0 ~14 minecraft:cobblestone
setblock ~10 ~0 ~17 minecraft:mossy_cobblestone
setblock ~10 ~0 ~18 minecraft:coarse_dirt
fill ~10 ~0 ~19 ~10 ~0 ~20 minecraft:packed_mud
setblock ~11 ~0 ~13 minecraft:gravel
setblock ~11 ~0 ~14 minecraft:coarse_dirt
fill ~11 ~0 ~15 ~11 ~0 ~16 minecraft:packed_mud
setblock ~11 ~0 ~17 minecraft:gravel
fill ~11 ~0 ~18 ~11 ~0 ~19 minecraft:cobblestone
setblock ~11 ~0 ~20 minecraft:packed_mud
fill ~12 ~0 ~13 ~14 ~0 ~13 minecraft:packed_mud
fill ~12 ~0 ~14 ~12 ~0 ~15 minecraft:gravel
setblock ~12 ~0 ~16 minecraft:packed_mud
setblock ~12 ~0 ~17 minecraft:cobblestone
setblock ~12 ~0 ~18 minecraft:packed_mud
setblock ~12 ~0 ~19 minecraft:gravel
setblock ~12 ~0 ~20 minecraft:cobblestone
setblock ~13 ~0 ~14 minecraft:coarse_dirt
setblock ~13 ~0 ~15 minecraft:packed_mud
fill ~13 ~0 ~16 ~13 ~0 ~17 minecraft:mossy_cobblestone
fill ~13 ~0 ~18 ~13 ~0 ~20 minecraft:cobblestone
fill ~14 ~0 ~14 ~15 ~0 ~14 minecraft:gravel
fill ~14 ~0 ~15 ~15 ~0 ~15 minecraft:coarse_dirt
setblock ~14 ~0 ~16 minecraft:coarse_dirt
setblock ~14 ~0 ~17 minecraft:gravel
setblock ~14 ~0 ~18 minecraft:mossy_cobblestone
setblock ~14 ~0 ~19 minecraft:coarse_dirt
setblock ~14 ~0 ~20 minecraft:packed_mud
setblock ~15 ~0 ~13 minecraft:coarse_dirt
fill ~15 ~0 ~16 ~15 ~0 ~17 minecraft:cobblestone
setblock ~15 ~0 ~18 minecraft:coarse_dirt
fill ~15 ~0 ~19 ~15 ~0 ~20 minecraft:cobblestone
setblock ~16 ~0 ~13 minecraft:cobblestone
setblock ~16 ~0 ~14 minecraft:mossy_cobblestone
setblock ~16 ~0 ~15 minecraft:packed_mud
setblock ~16 ~0 ~16 minecraft:coarse_dirt
fill ~16 ~0 ~17 ~16 ~0 ~19 minecraft:cobblestone
setblock ~16 ~0 ~20 minecraft:mossy_cobblestone
setblock ~17 ~0 ~13 minecraft:packed_mud
setblock ~17 ~0 ~14 minecraft:cobblestone
setblock ~17 ~0 ~15 minecraft:gravel
fill ~17 ~0 ~16 ~17 ~0 ~20 minecraft:packed_mud
setblock ~18 ~0 ~13 minecraft:cobblestone
setblock ~18 ~0 ~14 minecraft:coarse_dirt
setblock ~18 ~0 ~15 minecraft:cobblestone
setblock ~18 ~0 ~16 minecraft:gravel
setblock ~18 ~0 ~17 minecraft:packed_mud
fill ~18 ~0 ~18 ~18 ~0 ~19 minecraft:gravel
setblock ~18 ~0 ~20 minecraft:mossy_cobblestone

# the mill race: a stone channel along the east wall, water at y 0 for x 17..18, z 2..10, stone kerbs round it
setblock ~17 ~0 ~0 minecraft:mossy_cobblestone
setblock ~17 ~0 ~1 minecraft:cobblestone
fill ~17 ~0 ~2 ~18 ~0 ~10 minecraft:water
fill ~17 ~0 ~11 ~17 ~0 ~12 minecraft:cobblestone
fill ~18 ~0 ~0 ~18 ~0 ~1 minecraft:cobblestone
setblock ~18 ~0 ~11 minecraft:mossy_cobblestone
setblock ~18 ~0 ~12 minecraft:cobblestone

# the mill floor: stone flags under the walls, spruce boards inside
fill ~8 ~0 ~0 ~11 ~0 ~0 minecraft:cobblestone
fill ~8 ~0 ~1 ~9 ~0 ~1 minecraft:cobblestone
fill ~8 ~0 ~2 ~8 ~0 ~3 minecraft:cobblestone
setblock ~8 ~0 ~4 minecraft:mossy_cobblestone
fill ~8 ~0 ~5 ~8 ~0 ~6 minecraft:cobblestone
setblock ~8 ~0 ~7 minecraft:andesite
fill ~8 ~0 ~8 ~9 ~0 ~8 minecraft:cobblestone
setblock ~8 ~0 ~9 minecraft:mossy_cobblestone
fill ~8 ~0 ~10 ~8 ~0 ~11 minecraft:cobblestone
setblock ~8 ~0 ~12 minecraft:mossy_cobblestone
fill ~9 ~0 ~2 ~9 ~0 ~4 minecraft:spruce_planks
setblock ~9 ~0 ~5 minecraft:stone_bricks
fill ~9 ~0 ~6 ~9 ~0 ~7 minecraft:spruce_planks
setblock ~9 ~0 ~9 minecraft:cobblestone
fill ~9 ~0 ~10 ~9 ~0 ~11 minecraft:spruce_planks
fill ~9 ~0 ~12 ~11 ~0 ~12 minecraft:cobblestone
fill ~10 ~0 ~1 ~13 ~0 ~1 minecraft:spruce_planks
setblock ~10 ~0 ~2 minecraft:stone_bricks
fill ~10 ~0 ~3 ~11 ~0 ~3 minecraft:spruce_planks
setblock ~10 ~0 ~4 minecraft:stone_bricks
fill ~10 ~0 ~5 ~10 ~0 ~6 minecraft:spruce_planks
setblock ~10 ~0 ~7 minecraft:cobblestone
fill ~10 ~0 ~8 ~11 ~0 ~8 minecraft:spruce_planks
setblock ~10 ~0 ~9 minecraft:spruce_planks
setblock ~10 ~0 ~10 minecraft:cobblestone
setblock ~10 ~0 ~11 minecraft:spruce_planks
setblock ~11 ~0 ~2 minecraft:cobblestone
fill ~11 ~0 ~4 ~12 ~0 ~4 minecraft:cobblestone
fill ~11 ~0 ~5 ~12 ~0 ~5 minecraft:stone_bricks
fill ~11 ~0 ~6 ~12 ~0 ~6 minecraft:spruce_planks
setblock ~11 ~0 ~7 minecraft:stone_bricks
setblock ~11 ~0 ~9 minecraft:cobblestone
setblock ~11 ~0 ~10 minecraft:spruce_planks
setblock ~11 ~0 ~11 minecraft:stone_bricks
setblock ~12 ~0 ~0 minecraft:mossy_cobblestone
fill ~12 ~0 ~2 ~13 ~0 ~2 minecraft:spruce_planks
setblock ~12 ~0 ~3 minecraft:stone_bricks
setblock ~12 ~0 ~7 minecraft:spruce_planks
setblock ~12 ~0 ~8 minecraft:stone_bricks
fill ~12 ~0 ~9 ~12 ~0 ~11 minecraft:spruce_planks
setblock ~12 ~0 ~12 minecraft:mossy_cobblestone
setblock ~13 ~0 ~0 minecraft:cobblestone
fill ~13 ~0 ~3 ~13 ~0 ~8 minecraft:spruce_planks
fill ~13 ~0 ~9 ~14 ~0 ~9 minecraft:stone_bricks
fill ~13 ~0 ~10 ~14 ~0 ~10 minecraft:spruce_planks
setblock ~13 ~0 ~11 minecraft:spruce_planks
setblock ~13 ~0 ~12 minecraft:cobblestone
setblock ~14 ~0 ~0 minecraft:andesite
fill ~14 ~0 ~1 ~14 ~0 ~3 minecraft:stone_bricks
fill ~14 ~0 ~4 ~14 ~0 ~5 minecraft:spruce_planks
setblock ~14 ~0 ~6 minecraft:cobblestone
fill ~14 ~0 ~7 ~15 ~0 ~7 minecraft:stone_bricks
fill ~14 ~0 ~8 ~15 ~0 ~8 minecraft:spruce_planks
setblock ~14 ~0 ~11 minecraft:stone_bricks
setblock ~14 ~0 ~12 minecraft:mossy_cobblestone
fill ~15 ~0 ~0 ~16 ~0 ~0 minecraft:cobblestone
fill ~15 ~0 ~1 ~15 ~0 ~4 minecraft:spruce_planks
setblock ~15 ~0 ~5 minecraft:stone_bricks
setblock ~15 ~0 ~6 minecraft:spruce_planks
setblock ~15 ~0 ~9 minecraft:spruce_planks
fill ~15 ~0 ~10 ~15 ~0 ~11 minecraft:stone_bricks
setblock ~15 ~0 ~12 minecraft:cobblestone
fill ~16 ~0 ~1 ~16 ~0 ~3 minecraft:cobblestone
setblock ~16 ~0 ~4 minecraft:andesite
fill ~16 ~0 ~5 ~16 ~0 ~9 minecraft:cobblestone
setblock ~16 ~0 ~10 minecraft:mossy_cobblestone
setblock ~16 ~0 ~11 minecraft:andesite
setblock ~16 ~0 ~12 minecraft:mossy_cobblestone

# the stone base: cobblestone and stone bricks, y 1..4, mossy at the foot and green along the race
fill ~8 ~1 ~0 ~9 ~1 ~0 minecraft:cracked_stone_bricks
setblock ~8 ~1 ~1 minecraft:mossy_stone_bricks
fill ~8 ~1 ~2 ~8 ~1 ~3 minecraft:stone_bricks
setblock ~8 ~1 ~4 minecraft:mossy_stone_bricks
setblock ~8 ~1 ~5 minecraft:mossy_cobblestone
fill ~8 ~1 ~6 ~8 ~1 ~8 minecraft:stone_bricks
fill ~8 ~1 ~12 ~8 ~3 ~12 minecraft:stone_bricks
fill ~9 ~1 ~12 ~9 ~2 ~12 minecraft:mossy_stone_bricks
setblock ~10 ~1 ~0 minecraft:mossy_cobblestone
setblock ~10 ~1 ~12 minecraft:mossy_cobblestone
fill ~11 ~1 ~0 ~12 ~1 ~0 minecraft:mossy_stone_bricks
setblock ~11 ~1 ~12 minecraft:cracked_stone_bricks
fill ~13 ~1 ~0 ~13 ~4 ~0 minecraft:stone_bricks
setblock ~14 ~1 ~0 minecraft:mossy_stone_bricks
setblock ~15 ~1 ~0 minecraft:cracked_stone_bricks
setblock ~15 ~1 ~12 minecraft:mossy_stone_bricks
fill ~16 ~1 ~0 ~16 ~1 ~3 minecraft:mossy_cobblestone
fill ~16 ~1 ~4 ~16 ~1 ~5 minecraft:mossy_stone_bricks
fill ~16 ~1 ~6 ~16 ~2 ~6 minecraft:mossy_cobblestone
setblock ~16 ~1 ~7 minecraft:mossy_stone_bricks
fill ~16 ~1 ~8 ~16 ~1 ~10 minecraft:mossy_cobblestone
fill ~16 ~1 ~11 ~16 ~2 ~11 minecraft:mossy_stone_bricks
setblock ~16 ~1 ~12 minecraft:mossy_cobblestone
setblock ~8 ~2 ~0 minecraft:mossy_stone_bricks
fill ~8 ~2 ~1 ~8 ~3 ~1 minecraft:stone_bricks
fill ~8 ~2 ~2 ~8 ~2 ~3 minecraft:mossy_stone_bricks
fill ~8 ~2 ~5 ~8 ~3 ~5 minecraft:stone_bricks
setblock ~8 ~2 ~6 minecraft:mossy_stone_bricks
fill ~8 ~2 ~7 ~8 ~3 ~7 minecraft:stone_bricks
setblock ~8 ~2 ~8 minecraft:mossy_stone_bricks
fill ~9 ~2 ~0 ~10 ~3 ~0 minecraft:stone_bricks
fill ~10 ~2 ~12 ~11 ~2 ~12 minecraft:stone_bricks
setblock ~11 ~2 ~0 minecraft:mossy_cobblestone
setblock ~12 ~2 ~0 minecraft:stone_bricks
fill ~15 ~2 ~0 ~15 ~4 ~0 minecraft:stone_bricks
setblock ~15 ~2 ~12 minecraft:stone_bricks
fill ~16 ~2 ~0 ~16 ~2 ~1 minecraft:mossy_cobblestone
fill ~16 ~2 ~2 ~16 ~2 ~3 minecraft:mossy_stone_bricks
setblock ~16 ~2 ~4 minecraft:mossy_cobblestone
fill ~16 ~2 ~7 ~16 ~2 ~8 minecraft:mossy_cobblestone
fill ~16 ~2 ~9 ~16 ~2 ~10 minecraft:mossy_stone_bricks
setblock ~16 ~2 ~12 minecraft:mossy_stone_bricks
fill ~8 ~3 ~0 ~8 ~4 ~0 minecraft:stone_bricks
fill ~8 ~3 ~2 ~8 ~4 ~3 minecraft:stone_bricks
fill ~8 ~3 ~6 ~8 ~4 ~6 minecraft:stone_bricks
setblock ~8 ~3 ~8 minecraft:cobblestone
fill ~9 ~3 ~12 ~9 ~4 ~12 minecraft:cracked_stone_bricks
fill ~10 ~3 ~12 ~10 ~4 ~12 minecraft:stone_bricks
setblock ~11 ~3 ~0 minecraft:stone_bricks
setblock ~11 ~3 ~12 minecraft:cracked_stone_bricks
setblock ~12 ~3 ~0 minecraft:cobblestone
fill ~15 ~3 ~12 ~16 ~3 ~12 minecraft:cobblestone
fill ~16 ~3 ~0 ~16 ~3 ~1 minecraft:stone_bricks
fill ~16 ~3 ~2 ~16 ~3 ~3 minecraft:cobblestone
setblock ~16 ~3 ~4 minecraft:cracked_stone_bricks
fill ~16 ~3 ~6 ~16 ~4 ~6 minecraft:cobblestone
fill ~16 ~3 ~7 ~16 ~3 ~8 minecraft:cracked_stone_bricks
fill ~16 ~3 ~9 ~16 ~4 ~9 minecraft:cobblestone
fill ~16 ~3 ~10 ~16 ~3 ~11 minecraft:stone_bricks
setblock ~8 ~4 ~1 minecraft:cobblestone
setblock ~8 ~4 ~4 minecraft:stone_bricks
setblock ~8 ~4 ~5 minecraft:cobblestone
setblock ~8 ~4 ~7 minecraft:cobblestone
setblock ~9 ~4 ~0 minecraft:cobblestone
setblock ~10 ~4 ~0 minecraft:stone_bricks
setblock ~11 ~4 ~0 minecraft:cracked_stone_bricks
setblock ~12 ~4 ~0 minecraft:stone_bricks
setblock ~14 ~4 ~0 minecraft:stone_bricks
setblock ~16 ~4 ~0 minecraft:stone_bricks
setblock ~16 ~4 ~1 minecraft:cracked_stone_bricks
fill ~16 ~4 ~2 ~16 ~4 ~5 minecraft:stone_bricks
setblock ~16 ~4 ~7 minecraft:cobblestone
setblock ~16 ~4 ~8 minecraft:stone_bricks
setblock ~16 ~4 ~10 minecraft:cracked_stone_bricks
setblock ~16 ~4 ~11 minecraft:cobblestone
setblock ~16 ~4 ~12 minecraft:cracked_stone_bricks

# the timber upper storey: oak planks in a spruce log frame (posts at corners and every four blocks, a sill at y 5), y 5..10 on the gables, y 5..10 on the long sides
fill ~8 ~5 ~0 ~8 ~10 ~0 minecraft:spruce_log[axis=y]
fill ~8 ~5 ~1 ~8 ~5 ~3 minecraft:spruce_log[axis=z]
fill ~8 ~5 ~4 ~8 ~10 ~4 minecraft:spruce_log[axis=y]
fill ~8 ~5 ~5 ~8 ~5 ~7 minecraft:spruce_log[axis=z]
fill ~8 ~5 ~8 ~8 ~10 ~8 minecraft:spruce_log[axis=y]
fill ~8 ~5 ~9 ~8 ~5 ~11 minecraft:spruce_log[axis=z]
fill ~8 ~5 ~12 ~8 ~10 ~12 minecraft:spruce_log[axis=y]
fill ~9 ~5 ~0 ~11 ~5 ~0 minecraft:spruce_log[axis=x]
fill ~9 ~5 ~12 ~11 ~5 ~12 minecraft:spruce_log[axis=x]
fill ~12 ~5 ~0 ~12 ~10 ~0 minecraft:spruce_log[axis=y]
fill ~12 ~5 ~12 ~12 ~10 ~12 minecraft:spruce_log[axis=y]
fill ~13 ~5 ~0 ~15 ~5 ~0 minecraft:spruce_log[axis=x]
fill ~13 ~5 ~12 ~15 ~5 ~12 minecraft:spruce_log[axis=x]
fill ~16 ~5 ~0 ~16 ~10 ~0 minecraft:spruce_log[axis=y]
fill ~16 ~5 ~1 ~16 ~5 ~3 minecraft:spruce_log[axis=z]
fill ~16 ~5 ~4 ~16 ~10 ~4 minecraft:spruce_log[axis=y]
fill ~16 ~5 ~5 ~16 ~5 ~7 minecraft:spruce_log[axis=z]
fill ~16 ~5 ~8 ~16 ~10 ~8 minecraft:spruce_log[axis=y]
fill ~16 ~5 ~9 ~16 ~5 ~11 minecraft:spruce_log[axis=z]
fill ~16 ~5 ~12 ~16 ~10 ~12 minecraft:spruce_log[axis=y]
fill ~8 ~6 ~1 ~8 ~7 ~1 minecraft:spruce_planks
setblock ~8 ~6 ~3 minecraft:spruce_planks
fill ~8 ~6 ~5 ~8 ~10 ~5 minecraft:oak_planks
setblock ~8 ~6 ~7 minecraft:oak_planks
fill ~8 ~6 ~9 ~8 ~6 ~11 minecraft:oak_planks
fill ~9 ~6 ~0 ~11 ~6 ~0 minecraft:oak_planks
fill ~9 ~6 ~12 ~9 ~7 ~12 minecraft:oak_planks
fill ~11 ~6 ~12 ~11 ~10 ~12 minecraft:oak_planks
fill ~13 ~6 ~12 ~15 ~6 ~12 minecraft:oak_planks
fill ~14 ~6 ~0 ~14 ~10 ~0 minecraft:oak_planks
fill ~16 ~6 ~1 ~16 ~7 ~1 minecraft:oak_planks
setblock ~16 ~6 ~3 minecraft:spruce_planks
fill ~16 ~6 ~5 ~16 ~9 ~7 minecraft:oak_planks
fill ~16 ~6 ~9 ~16 ~10 ~9 minecraft:oak_planks
fill ~16 ~6 ~11 ~16 ~7 ~11 minecraft:oak_planks
fill ~8 ~7 ~2 ~8 ~7 ~3 minecraft:oak_planks
fill ~8 ~7 ~6 ~8 ~10 ~6 minecraft:oak_planks
setblock ~8 ~7 ~7 minecraft:stripped_oak_wood
setblock ~8 ~7 ~9 minecraft:stripped_oak_wood
fill ~8 ~7 ~10 ~8 ~10 ~10 minecraft:oak_planks
setblock ~8 ~7 ~11 minecraft:stripped_oak_wood
fill ~9 ~7 ~0 ~10 ~8 ~0 minecraft:oak_planks
fill ~10 ~7 ~12 ~10 ~9 ~12 minecraft:oak_planks
setblock ~11 ~7 ~0 minecraft:spruce_planks
fill ~13 ~7 ~0 ~13 ~10 ~0 minecraft:oak_planks
setblock ~13 ~7 ~12 minecraft:stripped_oak_wood
fill ~14 ~7 ~12 ~15 ~8 ~12 minecraft:oak_planks
fill ~15 ~7 ~0 ~15 ~8 ~0 minecraft:oak_planks
setblock ~16 ~7 ~2 minecraft:spruce_planks
fill ~16 ~7 ~3 ~16 ~10 ~3 minecraft:oak_planks
fill ~16 ~7 ~10 ~16 ~9 ~10 minecraft:oak_planks
fill ~8 ~8 ~1 ~8 ~10 ~2 minecraft:oak_planks
setblock ~8 ~8 ~3 minecraft:spruce_planks
setblock ~8 ~8 ~7 minecraft:oak_planks
setblock ~8 ~8 ~9 minecraft:oak_planks
fill ~8 ~8 ~11 ~8 ~9 ~11 minecraft:oak_planks
setblock ~9 ~8 ~12 minecraft:stripped_oak_wood
fill ~11 ~8 ~0 ~11 ~10 ~0 minecraft:oak_planks
setblock ~13 ~8 ~12 minecraft:oak_planks
setblock ~16 ~8 ~1 minecraft:spruce_planks
fill ~16 ~8 ~2 ~16 ~10 ~2 minecraft:oak_planks
setblock ~16 ~8 ~11 minecraft:stripped_oak_wood
fill ~8 ~9 ~3 ~8 ~10 ~3 minecraft:oak_planks
setblock ~8 ~9 ~7 minecraft:spruce_planks
setblock ~8 ~9 ~9 minecraft:spruce_planks
setblock ~9 ~9 ~0 minecraft:spruce_planks
fill ~9 ~9 ~12 ~9 ~10 ~12 minecraft:oak_planks
fill ~10 ~9 ~0 ~10 ~10 ~0 minecraft:oak_planks
setblock ~15 ~9 ~0 minecraft:spruce_planks
fill ~16 ~9 ~1 ~16 ~10 ~1 minecraft:oak_planks
fill ~16 ~9 ~11 ~16 ~10 ~11 minecraft:oak_planks
setblock ~8 ~10 ~7 minecraft:oak_planks
setblock ~8 ~10 ~9 minecraft:oak_planks
setblock ~8 ~10 ~11 minecraft:stripped_oak_wood
setblock ~9 ~10 ~0 minecraft:oak_planks
setblock ~10 ~10 ~12 minecraft:stripped_oak_wood
setblock ~15 ~10 ~0 minecraft:oak_planks
setblock ~16 ~10 ~5 minecraft:oak_planks
fill ~16 ~10 ~6 ~16 ~10 ~7 minecraft:spruce_planks
setblock ~16 ~10 ~10 minecraft:stripped_oak_wood

# the loft floor at y 7 over the mill (feet 8), oak boards on spruce beams, the ladder hole at (10, 7, 1)
fill ~9 ~7 ~1 ~9 ~7 ~11 minecraft:oak_planks
fill ~10 ~7 ~2 ~10 ~7 ~11 minecraft:oak_planks
fill ~11 ~7 ~1 ~11 ~7 ~11 minecraft:oak_planks
fill ~12 ~7 ~1 ~12 ~7 ~11 minecraft:spruce_log[axis=z]
fill ~13 ~7 ~1 ~15 ~7 ~11 minecraft:oak_planks

# the doors: west into the yard (x 8, z 9..11) and south (z 12, x 12..14), y 1..3, under oak lintels
fill ~8 ~1 ~9 ~8 ~3 ~11 minecraft:air
fill ~12 ~1 ~12 ~14 ~3 ~12 minecraft:air
fill ~8 ~4 ~8 ~8 ~4 ~12 minecraft:stripped_oak_log[axis=z]
fill ~11 ~4 ~12 ~15 ~4 ~12 minecraft:stripped_oak_log[axis=x]

# windows: shuttered openings on the ground storey, glass panes in the timber storey; the loft window south at x 14..15, y 9..10, open
fill ~8 ~2 ~4 ~8 ~3 ~4 minecraft:air
fill ~14 ~2 ~0 ~14 ~3 ~0 minecraft:air
fill ~16 ~2 ~5 ~16 ~3 ~5 minecraft:air
setblock ~8 ~6 ~2 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~8 ~6 ~6 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~10 ~6 ~12 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~13 ~6 ~0 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~15 ~6 ~0 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~16 ~6 ~2 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~16 ~6 ~10 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~13 ~9 ~12 ~13 ~10 ~12 minecraft:spruce_log[axis=y]
fill ~14 ~9 ~12 ~15 ~10 ~12 minecraft:air

# the roof: dark oak stairs, the ridge along z at x 12 (y 15), from the eaves at y 11 over x 7 and x 17, the whole length z 0..12 with a hood over the south loft window
fill ~7 ~10 ~0 ~7 ~10 ~12 minecraft:dark_oak_stairs[facing=east,half=bottom]
fill ~17 ~10 ~0 ~17 ~10 ~12 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~8 ~11 ~0 ~8 ~11 ~12 minecraft:dark_oak_stairs[facing=east,half=bottom]
fill ~16 ~11 ~0 ~16 ~11 ~12 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~9 ~12 ~0 ~9 ~12 ~12 minecraft:dark_oak_stairs[facing=east,half=bottom]
fill ~15 ~12 ~0 ~15 ~12 ~12 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~10 ~13 ~0 ~10 ~13 ~12 minecraft:dark_oak_stairs[facing=east,half=bottom]
fill ~14 ~13 ~0 ~14 ~13 ~12 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~11 ~14 ~0 ~11 ~14 ~12 minecraft:dark_oak_stairs[facing=east,half=bottom]
fill ~13 ~14 ~0 ~13 ~14 ~12 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~12 ~15 ~0 ~12 ~15 ~12 minecraft:dark_oak_planks

# the gables: oak planks under the roof at z 0 and z 12, a king post at x 12, a hoist beam out over the south gable
fill ~9 ~11 ~0 ~11 ~11 ~0 minecraft:oak_planks
fill ~9 ~11 ~12 ~10 ~11 ~12 minecraft:oak_planks
setblock ~11 ~11 ~12 minecraft:spruce_planks
fill ~12 ~11 ~0 ~12 ~14 ~0 minecraft:spruce_log[axis=y]
fill ~12 ~11 ~12 ~12 ~14 ~12 minecraft:spruce_log[axis=y]
fill ~12 ~11 ~13 ~12 ~12 ~13 minecraft:chain[axis=y]
fill ~13 ~11 ~0 ~15 ~11 ~0 minecraft:oak_planks
fill ~13 ~11 ~12 ~14 ~12 ~12 minecraft:oak_planks
setblock ~15 ~11 ~12 minecraft:spruce_planks
fill ~10 ~12 ~0 ~11 ~12 ~0 minecraft:oak_planks
fill ~10 ~12 ~12 ~11 ~12 ~12 minecraft:oak_planks
fill ~13 ~12 ~0 ~13 ~13 ~0 minecraft:spruce_planks
setblock ~14 ~12 ~0 minecraft:oak_planks
setblock ~11 ~13 ~0 minecraft:oak_planks
setblock ~11 ~13 ~12 minecraft:oak_planks
setblock ~12 ~13 ~13 minecraft:spruce_log[axis=z]
setblock ~13 ~13 ~12 minecraft:oak_planks

# the water wheel: a spruce log axle out of the east wall at y 4, z 6; a two-thick rim of spruce planks and slabs, radius 3, spokes of planks, paddles
fill ~17 ~1 ~5 ~18 ~1 ~5 minecraft:spruce_planks
fill ~17 ~1 ~6 ~18 ~1 ~6 minecraft:spruce_slab[type=top]
fill ~17 ~1 ~7 ~18 ~1 ~7 minecraft:spruce_planks
fill ~17 ~2 ~4 ~18 ~2 ~4 minecraft:spruce_planks
setblock ~17 ~2 ~6 minecraft:spruce_planks
fill ~17 ~2 ~8 ~18 ~2 ~8 minecraft:spruce_planks
fill ~17 ~3 ~3 ~18 ~3 ~3 minecraft:spruce_planks
setblock ~17 ~3 ~6 minecraft:spruce_log[axis=y]
fill ~17 ~3 ~9 ~18 ~3 ~9 minecraft:spruce_planks
fill ~17 ~4 ~3 ~18 ~4 ~3 minecraft:spruce_slab[type=bottom]
setblock ~17 ~4 ~4 minecraft:spruce_planks
setblock ~17 ~4 ~5 minecraft:spruce_log[axis=z]
fill ~17 ~4 ~6 ~18 ~4 ~6 minecraft:spruce_log[axis=x]
setblock ~17 ~4 ~7 minecraft:spruce_log[axis=z]
setblock ~17 ~4 ~8 minecraft:spruce_planks
fill ~17 ~4 ~9 ~18 ~4 ~9 minecraft:spruce_slab[type=bottom]
fill ~17 ~5 ~3 ~18 ~5 ~3 minecraft:spruce_planks
setblock ~17 ~5 ~6 minecraft:spruce_log[axis=y]
fill ~17 ~5 ~9 ~18 ~5 ~9 minecraft:spruce_planks
fill ~17 ~6 ~4 ~18 ~6 ~4 minecraft:spruce_planks
setblock ~17 ~6 ~6 minecraft:spruce_planks
fill ~17 ~6 ~8 ~18 ~6 ~8 minecraft:spruce_planks
fill ~17 ~7 ~5 ~18 ~7 ~5 minecraft:spruce_planks
fill ~17 ~7 ~6 ~18 ~7 ~6 minecraft:spruce_slab[type=bottom]
fill ~17 ~7 ~7 ~18 ~7 ~7 minecraft:spruce_planks

# inside the mill: the grindstone on its frame, sacks of flour (white wool) and grain (hay), barrels, the ladder to the loft
fill ~9 ~1 ~2 ~9 ~1 ~3 minecraft:white_wool
setblock ~9 ~1 ~6 minecraft:white_wool
fill ~10 ~1 ~1 ~10 ~7 ~1 minecraft:ladder[facing=south]
setblock ~12 ~1 ~6 minecraft:smooth_stone_slab[type=bottom]
setblock ~13 ~1 ~6 minecraft:grindstone[face=floor,facing=east]
fill ~14 ~1 ~6 ~15 ~1 ~6 minecraft:spruce_log[axis=x]
fill ~15 ~1 ~1 ~15 ~1 ~2 minecraft:white_wool
fill ~15 ~1 ~9 ~15 ~1 ~10 minecraft:barrel[facing=up]
setblock ~15 ~1 ~11 minecraft:hay_block[axis=y]
setblock ~15 ~2 ~10 minecraft:barrel[facing=up]
setblock ~12 ~6 ~6 minecraft:lantern[hanging=true]

# in the loft: grain sacks and hay away from the window, a lantern on a chain
setblock ~9 ~8 ~3 minecraft:white_wool
setblock ~9 ~8 ~4 minecraft:hay_block[axis=y]
setblock ~9 ~8 ~9 minecraft:white_wool
setblock ~10 ~8 ~4 minecraft:white_wool
setblock ~10 ~8 ~10 minecraft:white_wool
setblock ~15 ~8 ~2 minecraft:hay_block[axis=y]
setblock ~15 ~8 ~3 minecraft:white_wool

# the yard wall: two courses of cobblestone on the outer sides (west x 0, north z 0 for x 0..7, south z 20, east x 18 for z 13..20), gate at x 4..8 on the south
fill ~0 ~1 ~0 ~5 ~1 ~0 minecraft:cobblestone
fill ~0 ~1 ~1 ~0 ~2 ~1 minecraft:cobblestone
fill ~0 ~1 ~2 ~0 ~1 ~6 minecraft:mossy_cobblestone
fill ~0 ~1 ~7 ~0 ~2 ~7 minecraft:cobblestone
fill ~0 ~1 ~8 ~0 ~1 ~9 minecraft:mossy_cobblestone
fill ~0 ~1 ~10 ~0 ~2 ~11 minecraft:cobblestone
fill ~0 ~1 ~12 ~0 ~2 ~14 minecraft:mossy_cobblestone
setblock ~0 ~1 ~15 minecraft:cobbled_deepslate
fill ~0 ~1 ~16 ~0 ~2 ~16 minecraft:mossy_cobblestone
setblock ~0 ~1 ~17 minecraft:cobblestone
fill ~0 ~1 ~18 ~0 ~2 ~18 minecraft:cobbled_deepslate
fill ~0 ~1 ~19 ~0 ~1 ~20 minecraft:cobblestone
fill ~1 ~1 ~20 ~1 ~2 ~20 minecraft:cobblestone
fill ~2 ~1 ~20 ~2 ~2 ~20 minecraft:mossy_cobblestone
fill ~6 ~1 ~0 ~7 ~1 ~0 minecraft:mossy_cobblestone
fill ~10 ~1 ~20 ~12 ~1 ~20 minecraft:cobblestone
setblock ~13 ~1 ~20 minecraft:cobbled_deepslate
fill ~14 ~1 ~20 ~15 ~2 ~20 minecraft:cobblestone
setblock ~16 ~1 ~20 minecraft:cobbled_deepslate
fill ~17 ~1 ~20 ~17 ~2 ~20 minecraft:cobblestone
setblock ~18 ~1 ~13 minecraft:cobbled_deepslate
fill ~18 ~1 ~14 ~18 ~1 ~15 minecraft:cobblestone
setblock ~18 ~1 ~16 minecraft:mossy_cobblestone
fill ~18 ~1 ~17 ~18 ~1 ~20 minecraft:cobblestone
fill ~0 ~2 ~0 ~1 ~2 ~0 minecraft:mossy_cobblestone
fill ~0 ~2 ~2 ~0 ~2 ~4 minecraft:cobbled_deepslate
fill ~0 ~2 ~5 ~0 ~2 ~6 minecraft:mossy_cobblestone
setblock ~0 ~2 ~8 minecraft:cobblestone
setblock ~0 ~2 ~9 minecraft:cobbled_deepslate
setblock ~0 ~2 ~15 minecraft:mossy_cobblestone
setblock ~0 ~2 ~17 minecraft:mossy_cobblestone
setblock ~0 ~2 ~19 minecraft:cobbled_deepslate
setblock ~0 ~2 ~20 minecraft:cobblestone
setblock ~2 ~2 ~0 minecraft:cobblestone
setblock ~3 ~2 ~0 minecraft:cobbled_deepslate
fill ~4 ~2 ~0 ~7 ~2 ~0 minecraft:cobblestone
setblock ~10 ~2 ~20 minecraft:cobblestone
setblock ~11 ~2 ~20 minecraft:mossy_cobblestone
setblock ~12 ~2 ~20 minecraft:cobblestone
setblock ~13 ~2 ~20 minecraft:mossy_cobblestone
setblock ~16 ~2 ~20 minecraft:mossy_cobblestone
fill ~18 ~2 ~13 ~18 ~2 ~15 minecraft:mossy_cobblestone
fill ~18 ~2 ~16 ~18 ~2 ~19 minecraft:cobblestone
setblock ~18 ~2 ~20 minecraft:mossy_cobblestone

# the gate posts: stone brick piers three high with lanterns, at x 3 and x 9 on the south wall
fill ~3 ~1 ~20 ~3 ~3 ~20 minecraft:stone_bricks
fill ~9 ~1 ~20 ~9 ~3 ~20 minecraft:stone_bricks
setblock ~3 ~4 ~20 minecraft:lantern
setblock ~9 ~4 ~20 minecraft:lantern

# the yard: a cart of sacks against the west wall, millstones laid by, a trough, hay, barrels and a well; the lane from the gate to the doors kept clear
fill ~1 ~1 ~2 ~2 ~1 ~2 minecraft:hay_block[axis=y]
setblock ~1 ~1 ~3 minecraft:composter
fill ~1 ~1 ~6 ~2 ~1 ~6 minecraft:barrel[facing=up]
setblock ~1 ~1 ~7 minecraft:barrel[facing=up]
fill ~1 ~1 ~10 ~1 ~1 ~11 minecraft:white_wool
fill ~1 ~1 ~15 ~2 ~1 ~16 minecraft:cauldron
setblock ~2 ~1 ~11 minecraft:grindstone[face=floor,facing=north]
fill ~5 ~1 ~2 ~6 ~1 ~2 minecraft:smooth_stone_slab[type=bottom]
setblock ~13 ~1 ~18 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~14 ~1 ~18 minecraft:spruce_slab[type=top]
setblock ~15 ~1 ~18 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
fill ~16 ~1 ~15 ~17 ~1 ~15 minecraft:barrel[facing=up]
setblock ~17 ~1 ~16 minecraft:hay_block[axis=y]
setblock ~1 ~2 ~2 minecraft:hay_block[axis=y]
setblock ~17 ~2 ~15 minecraft:barrel[facing=up]
