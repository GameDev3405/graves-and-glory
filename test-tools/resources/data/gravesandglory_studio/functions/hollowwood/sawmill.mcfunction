# size 25 13 19
# hollowwood/sawmill: sawmill of Hollowwood: a long dark timber shed under a weathered shingle roof, open on the east, with a saw deck and frame, a log ramp, a stonecutter bench, stacks of planks and a yard of log piles and sawhorses, lit by lanterns.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Shed x 2..20, z 4..14 (walls y 1..5, plate y 5); gable roof along x over x 1..21, z 3..15, ridge y 11 at z 9; open on the east (x 20) and the south (z 14, posts only from x 9). The lane of the map leaves east from the centre (12, 9).
# Saw deck x 3..14, z 5..8, blocks y 1..2 (feet 3); ramp up from the east: stairs at x 16 (y 1) and x 15 (y 2); the saw frame at x 8; the aisle z 9..13 stays open at y 1..4.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~24 ~12 ~0 minecraft:air
fill ~0 ~1 ~1 ~20 ~1 ~3 minecraft:air
fill ~0 ~1 ~4 ~1 ~2 ~12 minecraft:air
fill ~0 ~1 ~13 ~0 ~12 ~18 minecraft:air
fill ~1 ~1 ~14 ~1 ~4 ~18 minecraft:air
fill ~2 ~1 ~15 ~8 ~2 ~15 minecraft:air
fill ~2 ~1 ~16 ~3 ~12 ~18 minecraft:air
fill ~3 ~1 ~9 ~14 ~3 ~11 minecraft:air
fill ~4 ~1 ~12 ~14 ~3 ~12 minecraft:air
fill ~4 ~1 ~18 ~7 ~12 ~18 minecraft:air
fill ~8 ~1 ~16 ~8 ~12 ~18 minecraft:air
fill ~9 ~1 ~13 ~10 ~4 ~13 minecraft:air
fill ~9 ~1 ~14 ~9 ~4 ~18 minecraft:air
fill ~10 ~1 ~15 ~10 ~7 ~18 minecraft:air
fill ~11 ~1 ~14 ~13 ~3 ~18 minecraft:air
fill ~13 ~1 ~13 ~15 ~4 ~13 minecraft:air
fill ~14 ~1 ~15 ~14 ~4 ~15 minecraft:air
fill ~14 ~1 ~18 ~17 ~12 ~18 minecraft:air
fill ~15 ~1 ~10 ~16 ~7 ~12 minecraft:air
fill ~15 ~1 ~14 ~15 ~4 ~15 minecraft:air
fill ~16 ~1 ~15 ~18 ~4 ~15 minecraft:air
fill ~17 ~1 ~9 ~19 ~3 ~12 minecraft:air
fill ~18 ~1 ~13 ~19 ~4 ~13 minecraft:air
fill ~18 ~1 ~16 ~18 ~12 ~18 minecraft:air
fill ~19 ~1 ~14 ~19 ~4 ~18 minecraft:air
fill ~20 ~1 ~5 ~20 ~4 ~13 minecraft:air
fill ~20 ~1 ~15 ~20 ~4 ~18 minecraft:air
fill ~21 ~1 ~3 ~24 ~4 ~3 minecraft:air
fill ~21 ~1 ~6 ~24 ~7 ~6 minecraft:air
fill ~21 ~1 ~9 ~21 ~10 ~9 minecraft:air
fill ~21 ~1 ~13 ~21 ~4 ~18 minecraft:air
fill ~22 ~1 ~7 ~22 ~1 ~18 minecraft:air
fill ~23 ~1 ~9 ~23 ~12 ~9 minecraft:air
fill ~23 ~1 ~12 ~23 ~12 ~18 minecraft:air
fill ~24 ~1 ~10 ~24 ~1 ~18 minecraft:air
fill ~0 ~2 ~1 ~21 ~12 ~2 minecraft:air
fill ~0 ~2 ~3 ~3 ~4 ~3 minecraft:air
fill ~3 ~2 ~12 ~3 ~2 ~13 minecraft:air
fill ~4 ~2 ~13 ~7 ~4 ~13 minecraft:air
fill ~5 ~2 ~3 ~7 ~4 ~3 minecraft:air
fill ~9 ~2 ~3 ~20 ~2 ~3 minecraft:air
fill ~11 ~2 ~13 ~12 ~7 ~13 minecraft:air
fill ~15 ~2 ~5 ~15 ~6 ~5 minecraft:air
fill ~15 ~2 ~9 ~15 ~10 ~9 minecraft:air
fill ~16 ~2 ~6 ~16 ~7 ~9 minecraft:air
fill ~21 ~2 ~4 ~21 ~5 ~5 minecraft:air
fill ~22 ~2 ~9 ~22 ~12 ~9 minecraft:air
fill ~22 ~2 ~12 ~22 ~12 ~18 minecraft:air
fill ~24 ~2 ~1 ~24 ~12 ~2 minecraft:air
fill ~24 ~2 ~4 ~24 ~12 ~5 minecraft:air
fill ~24 ~2 ~7 ~24 ~12 ~8 minecraft:air
fill ~24 ~2 ~12 ~24 ~12 ~18 minecraft:air
fill ~0 ~3 ~4 ~1 ~5 ~4 minecraft:air
fill ~0 ~3 ~5 ~0 ~12 ~12 minecraft:air
fill ~1 ~3 ~6 ~1 ~6 ~13 minecraft:air
fill ~3 ~3 ~8 ~7 ~4 ~8 minecraft:air
fill ~3 ~3 ~13 ~3 ~4 ~13 minecraft:air
fill ~3 ~3 ~15 ~8 ~4 ~15 minecraft:air
fill ~4 ~3 ~5 ~7 ~4 ~5 minecraft:air
fill ~8 ~3 ~13 ~8 ~6 ~13 minecraft:air
fill ~9 ~3 ~3 ~17 ~4 ~3 minecraft:air
fill ~9 ~3 ~5 ~12 ~4 ~5 minecraft:air
fill ~9 ~3 ~7 ~9 ~8 ~8 minecraft:air
fill ~10 ~3 ~8 ~13 ~4 ~8 minecraft:air
fill ~14 ~3 ~6 ~15 ~4 ~8 minecraft:air
fill ~19 ~3 ~3 ~20 ~4 ~3 minecraft:air
fill ~21 ~3 ~7 ~23 ~8 ~8 minecraft:air
fill ~22 ~3 ~2 ~23 ~12 ~2 minecraft:air
fill ~22 ~3 ~5 ~23 ~12 ~5 minecraft:air
fill ~24 ~3 ~10 ~24 ~12 ~11 minecraft:air
fill ~3 ~4 ~6 ~7 ~4 ~7 minecraft:air
fill ~3 ~4 ~9 ~14 ~4 ~9 minecraft:air
fill ~3 ~4 ~11 ~5 ~8 ~11 minecraft:air
fill ~3 ~4 ~14 ~5 ~4 ~14 minecraft:air
setblock ~4 ~4 ~3 minecraft:air
fill ~4 ~4 ~10 ~14 ~4 ~10 minecraft:air
fill ~4 ~4 ~12 ~6 ~4 ~12 minecraft:air
fill ~4 ~4 ~16 ~4 ~12 ~17 minecraft:air
fill ~7 ~4 ~11 ~9 ~7 ~12 minecraft:air
fill ~7 ~4 ~14 ~8 ~4 ~14 minecraft:air
fill ~7 ~4 ~16 ~7 ~12 ~17 minecraft:air
setblock ~8 ~4 ~3 minecraft:air
fill ~9 ~4 ~6 ~13 ~4 ~6 minecraft:air
fill ~10 ~4 ~12 ~13 ~4 ~12 minecraft:air
fill ~11 ~4 ~7 ~11 ~8 ~7 minecraft:air
fill ~11 ~4 ~11 ~14 ~4 ~11 minecraft:air
fill ~11 ~4 ~14 ~13 ~4 ~16 minecraft:air
fill ~11 ~4 ~18 ~11 ~12 ~18 minecraft:air
fill ~12 ~4 ~17 ~13 ~12 ~18 minecraft:air
fill ~13 ~4 ~5 ~14 ~4 ~5 minecraft:air
fill ~13 ~4 ~7 ~13 ~8 ~7 minecraft:air
fill ~14 ~4 ~16 ~14 ~12 ~17 minecraft:air
fill ~17 ~4 ~9 ~19 ~4 ~10 minecraft:air
fill ~17 ~4 ~11 ~17 ~7 ~12 minecraft:air
fill ~17 ~4 ~16 ~17 ~12 ~17 minecraft:air
setblock ~18 ~4 ~12 minecraft:air
fill ~19 ~4 ~11 ~19 ~7 ~12 minecraft:air
fill ~21 ~4 ~11 ~22 ~8 ~11 minecraft:air
fill ~23 ~4 ~10 ~23 ~12 ~11 minecraft:air
fill ~0 ~5 ~3 ~0 ~12 ~3 minecraft:air
fill ~1 ~5 ~5 ~1 ~6 ~5 minecraft:air
setblock ~1 ~5 ~14 minecraft:air
fill ~1 ~5 ~16 ~1 ~12 ~18 minecraft:air
fill ~3 ~5 ~5 ~5 ~5 ~9 minecraft:air
fill ~4 ~5 ~10 ~5 ~9 ~10 minecraft:air
fill ~4 ~5 ~12 ~5 ~6 ~13 minecraft:air
fill ~5 ~5 ~16 ~6 ~12 ~17 minecraft:air
fill ~7 ~5 ~5 ~7 ~6 ~10 minecraft:air
fill ~7 ~5 ~13 ~7 ~6 ~13 minecraft:air
fill ~8 ~5 ~9 ~9 ~9 ~10 minecraft:air
fill ~9 ~5 ~5 ~9 ~6 ~6 minecraft:air
fill ~9 ~5 ~13 ~9 ~6 ~13 minecraft:air
fill ~9 ~5 ~16 ~9 ~12 ~18 minecraft:air
fill ~11 ~5 ~5 ~13 ~6 ~6 minecraft:air
fill ~11 ~5 ~8 ~11 ~8 ~12 minecraft:air
fill ~11 ~5 ~15 ~12 ~6 ~16 minecraft:air
fill ~12 ~5 ~7 ~12 ~7 ~12 minecraft:air
fill ~13 ~5 ~8 ~13 ~6 ~13 minecraft:air
fill ~13 ~5 ~16 ~13 ~12 ~16 minecraft:air
fill ~15 ~5 ~6 ~15 ~7 ~8 minecraft:air
fill ~15 ~5 ~13 ~15 ~6 ~13 minecraft:air
fill ~15 ~5 ~16 ~16 ~12 ~17 minecraft:air
fill ~17 ~5 ~5 ~17 ~6 ~10 minecraft:air
fill ~19 ~5 ~5 ~19 ~6 ~10 minecraft:air
fill ~19 ~5 ~13 ~19 ~6 ~13 minecraft:air
fill ~19 ~5 ~16 ~21 ~12 ~18 minecraft:air
fill ~21 ~5 ~10 ~22 ~9 ~10 minecraft:air
fill ~21 ~5 ~12 ~21 ~5 ~14 minecraft:air
fill ~22 ~5 ~1 ~23 ~12 ~1 minecraft:air
fill ~22 ~5 ~3 ~24 ~12 ~3 minecraft:air
fill ~22 ~5 ~4 ~23 ~12 ~4 minecraft:air
fill ~24 ~5 ~9 ~24 ~12 ~9 minecraft:air
fill ~0 ~6 ~4 ~0 ~12 ~4 minecraft:air
fill ~1 ~6 ~3 ~21 ~12 ~3 minecraft:air
fill ~1 ~6 ~15 ~8 ~12 ~15 minecraft:air
fill ~3 ~6 ~5 ~6 ~6 ~7 minecraft:air
fill ~3 ~6 ~9 ~3 ~9 ~10 minecraft:air
setblock ~3 ~6 ~13 minecraft:air
fill ~4 ~6 ~8 ~6 ~9 ~9 minecraft:air
fill ~6 ~6 ~10 ~6 ~6 ~13 minecraft:air
fill ~10 ~6 ~5 ~10 ~6 ~13 minecraft:air
fill ~14 ~6 ~5 ~14 ~6 ~13 minecraft:air
fill ~14 ~6 ~15 ~21 ~12 ~15 minecraft:air
fill ~18 ~6 ~5 ~18 ~6 ~13 minecraft:air
fill ~20 ~6 ~6 ~20 ~6 ~8 minecraft:air
fill ~20 ~6 ~10 ~20 ~6 ~12 minecraft:air
setblock ~21 ~6 ~5 minecraft:air
fill ~21 ~6 ~12 ~21 ~6 ~13 minecraft:air
fill ~1 ~7 ~4 ~21 ~12 ~4 minecraft:air
fill ~1 ~7 ~6 ~1 ~7 ~12 minecraft:air
fill ~1 ~7 ~14 ~8 ~12 ~14 minecraft:air
fill ~3 ~7 ~6 ~7 ~7 ~7 minecraft:air
fill ~3 ~7 ~8 ~3 ~9 ~8 minecraft:air
fill ~4 ~7 ~12 ~5 ~7 ~12 minecraft:air
fill ~6 ~7 ~10 ~6 ~7 ~12 minecraft:air
fill ~7 ~7 ~8 ~7 ~9 ~10 minecraft:air
fill ~9 ~7 ~6 ~14 ~7 ~6 minecraft:air
fill ~10 ~7 ~7 ~10 ~7 ~13 minecraft:air
setblock ~11 ~7 ~16 minecraft:air
fill ~12 ~7 ~15 ~12 ~7 ~16 minecraft:air
fill ~13 ~7 ~8 ~13 ~7 ~12 minecraft:air
fill ~14 ~7 ~7 ~14 ~7 ~12 minecraft:air
fill ~14 ~7 ~14 ~15 ~12 ~14 minecraft:air
fill ~17 ~7 ~6 ~19 ~7 ~10 minecraft:air
fill ~18 ~7 ~11 ~18 ~7 ~12 minecraft:air
fill ~18 ~7 ~14 ~21 ~12 ~14 minecraft:air
fill ~20 ~7 ~7 ~20 ~7 ~8 minecraft:air
fill ~20 ~7 ~10 ~20 ~7 ~11 minecraft:air
setblock ~21 ~7 ~12 minecraft:air
fill ~1 ~8 ~5 ~21 ~12 ~5 minecraft:air
fill ~1 ~8 ~7 ~1 ~8 ~11 minecraft:air
fill ~1 ~8 ~13 ~9 ~12 ~13 minecraft:air
fill ~3 ~8 ~7 ~8 ~8 ~7 minecraft:air
fill ~6 ~8 ~10 ~6 ~8 ~11 minecraft:air
fill ~7 ~8 ~11 ~9 ~8 ~11 minecraft:air
fill ~8 ~8 ~8 ~8 ~9 ~8 minecraft:air
fill ~9 ~8 ~14 ~9 ~12 ~15 minecraft:air
fill ~10 ~8 ~7 ~10 ~8 ~11 minecraft:air
fill ~10 ~8 ~16 ~10 ~12 ~18 minecraft:air
setblock ~11 ~8 ~13 minecraft:air
fill ~12 ~8 ~7 ~12 ~8 ~11 minecraft:air
fill ~12 ~8 ~16 ~12 ~12 ~16 minecraft:air
fill ~13 ~8 ~8 ~13 ~8 ~11 minecraft:air
fill ~13 ~8 ~13 ~15 ~12 ~13 minecraft:air
fill ~13 ~8 ~14 ~13 ~12 ~15 minecraft:air
fill ~14 ~8 ~7 ~19 ~8 ~8 minecraft:air
fill ~14 ~8 ~9 ~14 ~8 ~11 minecraft:air
fill ~15 ~8 ~10 ~15 ~8 ~11 minecraft:air
fill ~16 ~8 ~9 ~19 ~8 ~11 minecraft:air
fill ~18 ~8 ~13 ~21 ~12 ~13 minecraft:air
fill ~20 ~8 ~8 ~20 ~9 ~8 minecraft:air
fill ~20 ~8 ~10 ~20 ~9 ~10 minecraft:air
fill ~22 ~8 ~6 ~24 ~12 ~6 minecraft:air
fill ~1 ~9 ~6 ~21 ~12 ~6 minecraft:air
fill ~1 ~9 ~8 ~1 ~9 ~10 minecraft:air
fill ~1 ~9 ~12 ~10 ~12 ~12 minecraft:air
setblock ~6 ~9 ~10 minecraft:air
fill ~9 ~9 ~8 ~19 ~9 ~8 minecraft:air
fill ~10 ~9 ~9 ~14 ~9 ~10 minecraft:air
fill ~10 ~9 ~13 ~10 ~12 ~15 minecraft:air
fill ~11 ~9 ~16 ~11 ~12 ~17 minecraft:air
fill ~12 ~9 ~12 ~21 ~12 ~12 minecraft:air
fill ~12 ~9 ~13 ~12 ~12 ~15 minecraft:air
setblock ~15 ~9 ~10 minecraft:air
fill ~16 ~9 ~9 ~19 ~9 ~10 minecraft:air
setblock ~21 ~9 ~8 minecraft:air
fill ~22 ~9 ~7 ~23 ~12 ~8 minecraft:air
fill ~22 ~9 ~11 ~22 ~12 ~11 minecraft:air
fill ~1 ~10 ~7 ~21 ~12 ~7 minecraft:air
setblock ~1 ~10 ~9 minecraft:air
fill ~1 ~10 ~11 ~21 ~12 ~11 minecraft:air
fill ~4 ~10 ~9 ~14 ~10 ~9 minecraft:air
fill ~11 ~10 ~12 ~11 ~12 ~15 minecraft:air
fill ~16 ~10 ~9 ~20 ~10 ~9 minecraft:air
fill ~22 ~10 ~10 ~22 ~12 ~10 minecraft:air
fill ~1 ~11 ~8 ~21 ~12 ~8 minecraft:air
fill ~1 ~11 ~10 ~21 ~12 ~10 minecraft:air
fill ~1 ~12 ~9 ~21 ~12 ~9 minecraft:air

# the floor of the shed: wood chips and sawdust (coarse dirt, trodden path, worn boards) with a boarded floor under the deck and the saw
setblock ~3 ~0 ~5 minecraft:spruce_planks
setblock ~3 ~0 ~6 minecraft:dark_oak_planks
fill ~3 ~0 ~7 ~4 ~0 ~7 minecraft:spruce_planks
setblock ~3 ~0 ~8 minecraft:spruce_planks
setblock ~3 ~0 ~9 minecraft:podzol
setblock ~3 ~0 ~10 minecraft:coarse_dirt
fill ~3 ~0 ~11 ~3 ~0 ~12 minecraft:coarse_dirt
setblock ~3 ~0 ~13 minecraft:packed_mud
setblock ~4 ~0 ~5 minecraft:dark_oak_planks
fill ~4 ~0 ~6 ~5 ~0 ~6 minecraft:spruce_planks
setblock ~4 ~0 ~8 minecraft:dark_oak_planks
setblock ~4 ~0 ~9 minecraft:spruce_planks
fill ~4 ~0 ~10 ~4 ~0 ~13 minecraft:coarse_dirt
fill ~5 ~0 ~5 ~6 ~0 ~5 minecraft:spruce_planks
setblock ~5 ~0 ~7 minecraft:dark_oak_planks
fill ~5 ~0 ~8 ~6 ~0 ~8 minecraft:spruce_planks
fill ~5 ~0 ~9 ~5 ~0 ~11 minecraft:coarse_dirt
fill ~5 ~0 ~12 ~6 ~0 ~12 minecraft:podzol
setblock ~5 ~0 ~13 minecraft:podzol
setblock ~6 ~0 ~6 minecraft:dark_oak_planks
fill ~6 ~0 ~7 ~7 ~0 ~7 minecraft:spruce_planks
setblock ~6 ~0 ~9 minecraft:podzol
setblock ~6 ~0 ~10 minecraft:coarse_dirt
fill ~6 ~0 ~11 ~7 ~0 ~11 minecraft:spruce_planks
fill ~6 ~0 ~13 ~7 ~0 ~13 minecraft:coarse_dirt
setblock ~7 ~0 ~5 minecraft:dark_oak_planks
fill ~7 ~0 ~6 ~8 ~0 ~6 minecraft:spruce_planks
setblock ~7 ~0 ~8 minecraft:dark_oak_planks
setblock ~7 ~0 ~9 minecraft:coarse_dirt
setblock ~7 ~0 ~10 minecraft:oak_planks
setblock ~7 ~0 ~12 minecraft:packed_mud
fill ~8 ~0 ~5 ~9 ~0 ~5 minecraft:spruce_planks
setblock ~8 ~0 ~7 minecraft:dark_oak_planks
fill ~8 ~0 ~8 ~9 ~0 ~8 minecraft:spruce_planks
setblock ~8 ~0 ~9 minecraft:spruce_planks
setblock ~8 ~0 ~10 minecraft:packed_mud
fill ~8 ~0 ~11 ~8 ~0 ~12 minecraft:oak_planks
setblock ~8 ~0 ~13 minecraft:podzol
setblock ~9 ~0 ~6 minecraft:dark_oak_planks
fill ~9 ~0 ~7 ~10 ~0 ~7 minecraft:spruce_planks
setblock ~9 ~0 ~9 minecraft:podzol
setblock ~9 ~0 ~10 minecraft:coarse_dirt
setblock ~9 ~0 ~11 minecraft:spruce_planks
setblock ~9 ~0 ~12 minecraft:podzol
setblock ~9 ~0 ~13 minecraft:packed_mud
setblock ~10 ~0 ~5 minecraft:dark_oak_planks
fill ~10 ~0 ~6 ~11 ~0 ~6 minecraft:spruce_planks
setblock ~10 ~0 ~8 minecraft:dark_oak_planks
setblock ~10 ~0 ~9 minecraft:coarse_dirt
setblock ~10 ~0 ~10 minecraft:oak_planks
setblock ~10 ~0 ~11 minecraft:coarse_dirt
fill ~10 ~0 ~12 ~11 ~0 ~12 minecraft:packed_mud
setblock ~10 ~0 ~13 minecraft:spruce_planks
fill ~11 ~0 ~5 ~12 ~0 ~5 minecraft:spruce_planks
setblock ~11 ~0 ~7 minecraft:dark_oak_planks
setblock ~11 ~0 ~8 minecraft:spruce_planks
fill ~11 ~0 ~9 ~11 ~0 ~11 minecraft:oak_planks
setblock ~11 ~0 ~13 minecraft:coarse_dirt
setblock ~12 ~0 ~6 minecraft:dark_oak_planks
fill ~12 ~0 ~7 ~12 ~0 ~8 minecraft:spruce_planks
fill ~12 ~0 ~9 ~13 ~0 ~9 minecraft:coarse_dirt
setblock ~12 ~0 ~10 minecraft:coarse_dirt
setblock ~12 ~0 ~11 minecraft:spruce_planks
fill ~12 ~0 ~12 ~12 ~0 ~13 minecraft:oak_planks
setblock ~13 ~0 ~5 minecraft:dark_oak_planks
fill ~13 ~0 ~6 ~13 ~0 ~7 minecraft:spruce_planks
setblock ~13 ~0 ~8 minecraft:dark_oak_planks
fill ~13 ~0 ~10 ~14 ~0 ~11 minecraft:coarse_dirt
setblock ~13 ~0 ~12 minecraft:coarse_dirt
setblock ~13 ~0 ~13 minecraft:podzol
fill ~14 ~0 ~5 ~14 ~0 ~6 minecraft:spruce_planks
setblock ~14 ~0 ~7 minecraft:dark_oak_planks
fill ~14 ~0 ~8 ~14 ~0 ~9 minecraft:spruce_planks
setblock ~14 ~0 ~12 minecraft:spruce_planks
setblock ~14 ~0 ~13 minecraft:coarse_dirt
fill ~15 ~0 ~5 ~15 ~0 ~6 minecraft:coarse_dirt
fill ~15 ~0 ~7 ~16 ~0 ~7 minecraft:podzol
fill ~15 ~0 ~8 ~15 ~0 ~9 minecraft:coarse_dirt
fill ~15 ~0 ~10 ~15 ~0 ~11 minecraft:spruce_planks
setblock ~15 ~0 ~12 minecraft:podzol
setblock ~15 ~0 ~13 minecraft:spruce_planks
setblock ~16 ~0 ~5 minecraft:oak_planks
setblock ~16 ~0 ~6 minecraft:coarse_dirt
fill ~16 ~0 ~8 ~17 ~0 ~8 minecraft:oak_planks
fill ~16 ~0 ~9 ~16 ~0 ~10 minecraft:coarse_dirt
fill ~16 ~0 ~11 ~17 ~0 ~11 minecraft:spruce_planks
fill ~16 ~0 ~12 ~16 ~0 ~13 minecraft:coarse_dirt
fill ~17 ~0 ~5 ~17 ~0 ~6 minecraft:packed_mud
setblock ~17 ~0 ~7 minecraft:coarse_dirt
setblock ~17 ~0 ~9 minecraft:coarse_dirt
setblock ~17 ~0 ~10 minecraft:coarse_dirt
setblock ~17 ~0 ~12 minecraft:podzol
setblock ~17 ~0 ~13 minecraft:coarse_dirt
fill ~18 ~0 ~5 ~18 ~0 ~6 minecraft:coarse_dirt
fill ~18 ~0 ~7 ~18 ~0 ~9 minecraft:packed_mud
setblock ~18 ~0 ~10 minecraft:coarse_dirt
setblock ~18 ~0 ~11 minecraft:podzol
setblock ~18 ~0 ~12 minecraft:coarse_dirt
setblock ~18 ~0 ~13 minecraft:coarse_dirt
setblock ~19 ~0 ~5 minecraft:packed_mud
setblock ~19 ~0 ~6 minecraft:spruce_planks
setblock ~19 ~0 ~7 minecraft:oak_planks
setblock ~19 ~0 ~8 minecraft:spruce_planks
fill ~19 ~0 ~9 ~19 ~0 ~10 minecraft:coarse_dirt
setblock ~19 ~0 ~11 minecraft:spruce_planks
setblock ~19 ~0 ~12 minecraft:coarse_dirt
setblock ~19 ~0 ~13 minecraft:podzol

# the yard to the east and the way out of the aisle: trodden earth, bark and gravel, a worn track from the aisle to the south-east edge, the log yard on podzol
fill ~20 ~0 ~0 ~21 ~0 ~0 minecraft:coarse_dirt
fill ~20 ~0 ~1 ~20 ~0 ~2 minecraft:coarse_dirt
setblock ~20 ~0 ~3 minecraft:podzol
setblock ~20 ~0 ~4 minecraft:coarse_dirt
setblock ~20 ~0 ~5 minecraft:packed_mud
setblock ~20 ~0 ~6 minecraft:podzol
setblock ~20 ~0 ~7 minecraft:packed_mud
setblock ~20 ~0 ~8 minecraft:podzol
fill ~20 ~0 ~9 ~20 ~0 ~11 minecraft:coarse_dirt
setblock ~20 ~0 ~12 minecraft:podzol
fill ~20 ~0 ~13 ~21 ~0 ~13 minecraft:dirt_path
setblock ~20 ~0 ~14 minecraft:packed_mud
setblock ~20 ~0 ~15 minecraft:dirt_path
setblock ~20 ~0 ~16 minecraft:coarse_dirt
fill ~20 ~0 ~17 ~21 ~0 ~17 minecraft:podzol
fill ~20 ~0 ~18 ~21 ~0 ~18 minecraft:gravel
fill ~21 ~0 ~1 ~22 ~0 ~1 minecraft:podzol
fill ~21 ~0 ~2 ~21 ~0 ~3 minecraft:coarse_dirt
setblock ~21 ~0 ~4 minecraft:rooted_dirt
fill ~21 ~0 ~5 ~22 ~0 ~5 minecraft:podzol
fill ~21 ~0 ~6 ~21 ~0 ~11 minecraft:coarse_dirt
setblock ~21 ~0 ~12 minecraft:gravel
fill ~21 ~0 ~14 ~22 ~0 ~16 minecraft:dirt_path
fill ~22 ~0 ~0 ~23 ~0 ~0 minecraft:podzol
setblock ~22 ~0 ~2 minecraft:rooted_dirt
setblock ~22 ~0 ~3 minecraft:podzol
setblock ~22 ~0 ~4 minecraft:coarse_dirt
fill ~22 ~0 ~6 ~23 ~0 ~8 minecraft:podzol
fill ~22 ~0 ~9 ~23 ~0 ~9 minecraft:packed_mud
fill ~22 ~0 ~10 ~22 ~0 ~12 minecraft:coarse_dirt
setblock ~22 ~0 ~13 minecraft:packed_mud
setblock ~22 ~0 ~17 minecraft:gravel
fill ~22 ~0 ~18 ~23 ~0 ~18 minecraft:coarse_dirt
fill ~23 ~0 ~1 ~23 ~0 ~2 minecraft:coarse_dirt
setblock ~23 ~0 ~3 minecraft:rooted_dirt
setblock ~23 ~0 ~4 minecraft:podzol
setblock ~23 ~0 ~5 minecraft:coarse_dirt
setblock ~23 ~0 ~10 minecraft:podzol
setblock ~23 ~0 ~11 minecraft:coarse_dirt
fill ~23 ~0 ~12 ~23 ~0 ~13 minecraft:packed_mud
setblock ~23 ~0 ~14 minecraft:coarse_dirt
fill ~23 ~0 ~15 ~24 ~0 ~17 minecraft:dirt_path
setblock ~24 ~0 ~0 minecraft:coarse_dirt
fill ~24 ~0 ~1 ~24 ~0 ~2 minecraft:podzol
setblock ~24 ~0 ~3 minecraft:coarse_dirt
setblock ~24 ~0 ~4 minecraft:packed_mud
fill ~24 ~0 ~5 ~24 ~0 ~7 minecraft:podzol
setblock ~24 ~0 ~8 minecraft:packed_mud
setblock ~24 ~0 ~9 minecraft:podzol
fill ~24 ~0 ~10 ~24 ~0 ~11 minecraft:gravel
setblock ~24 ~0 ~12 minecraft:podzol
setblock ~24 ~0 ~13 minecraft:packed_mud
setblock ~24 ~0 ~14 minecraft:podzol
setblock ~24 ~0 ~18 minecraft:dirt_path

# the north wall (z 4, x 2..20) and the west wall (x 2, z 4..14): vertical boards of spruce, one in six dark oak, between a dark oak frame; a cobble foot at y 1
fill ~2 ~1 ~5 ~2 ~1 ~13 minecraft:cobblestone
fill ~3 ~1 ~4 ~5 ~1 ~4 minecraft:cobblestone
fill ~7 ~1 ~4 ~9 ~1 ~4 minecraft:cobblestone
fill ~11 ~1 ~4 ~13 ~1 ~4 minecraft:cobblestone
fill ~15 ~1 ~4 ~17 ~1 ~4 minecraft:cobblestone
setblock ~19 ~1 ~4 minecraft:cobblestone
fill ~2 ~2 ~5 ~2 ~4 ~6 minecraft:spruce_planks
fill ~2 ~2 ~7 ~2 ~2 ~8 minecraft:dark_oak_planks
fill ~2 ~2 ~9 ~2 ~2 ~11 minecraft:spruce_planks
setblock ~2 ~2 ~12 minecraft:dark_oak_planks
fill ~2 ~2 ~13 ~2 ~3 ~13 minecraft:spruce_planks
fill ~3 ~2 ~4 ~4 ~2 ~4 minecraft:spruce_planks
fill ~5 ~2 ~4 ~5 ~3 ~4 minecraft:dark_oak_planks
fill ~7 ~2 ~4 ~8 ~3 ~4 minecraft:spruce_planks
setblock ~9 ~2 ~4 minecraft:dark_oak_planks
fill ~11 ~2 ~4 ~13 ~2 ~4 minecraft:spruce_planks
fill ~15 ~2 ~4 ~15 ~4 ~4 minecraft:spruce_planks
fill ~16 ~2 ~4 ~16 ~3 ~4 minecraft:dark_oak_planks
fill ~17 ~2 ~4 ~17 ~4 ~4 minecraft:spruce_planks
setblock ~19 ~2 ~4 minecraft:dark_oak_planks
fill ~2 ~3 ~7 ~2 ~4 ~8 minecraft:spruce_planks
setblock ~2 ~3 ~9 minecraft:dark_oak_planks
fill ~2 ~3 ~10 ~2 ~3 ~12 minecraft:spruce_planks
setblock ~3 ~3 ~4 minecraft:dark_oak_planks
fill ~4 ~3 ~4 ~4 ~4 ~4 minecraft:spruce_planks
fill ~9 ~3 ~4 ~9 ~4 ~4 minecraft:spruce_planks
fill ~11 ~3 ~4 ~12 ~4 ~4 minecraft:spruce_planks
setblock ~13 ~3 ~4 minecraft:dark_oak_planks
fill ~19 ~3 ~4 ~19 ~4 ~4 minecraft:spruce_planks
setblock ~2 ~4 ~9 minecraft:spruce_planks
setblock ~2 ~4 ~10 minecraft:dark_oak_planks
fill ~2 ~4 ~11 ~2 ~4 ~12 minecraft:spruce_planks
setblock ~2 ~4 ~13 minecraft:dark_oak_planks
setblock ~3 ~4 ~4 minecraft:spruce_planks
setblock ~5 ~4 ~4 minecraft:spruce_planks
setblock ~7 ~4 ~4 minecraft:dark_oak_planks
setblock ~8 ~4 ~4 minecraft:spruce_planks
setblock ~13 ~4 ~4 minecraft:spruce_planks
setblock ~16 ~4 ~4 minecraft:spruce_planks

# the half wall on the south-west (z 14, x 2..8, y 1..2) with a counter on top (spruce slabs); the south side is otherwise open
fill ~3 ~1 ~14 ~5 ~2 ~14 minecraft:spruce_planks
fill ~7 ~1 ~14 ~8 ~2 ~14 minecraft:spruce_planks
fill ~3 ~3 ~14 ~5 ~3 ~14 minecraft:spruce_slab[type=bottom]
fill ~7 ~3 ~14 ~8 ~3 ~14 minecraft:spruce_slab[type=bottom]

# the posts of dark oak logs (y 1..5) at x 2, 6, 10, 14, 18 and 20 on the north and south sides, braced with logs to the plate (stripped spruce logs at y 5)
fill ~2 ~1 ~4 ~2 ~4 ~4 minecraft:dark_oak_log[axis=y]
fill ~2 ~1 ~14 ~2 ~4 ~14 minecraft:dark_oak_log[axis=y]
fill ~6 ~1 ~4 ~6 ~4 ~4 minecraft:dark_oak_log[axis=y]
fill ~6 ~1 ~14 ~6 ~4 ~14 minecraft:dark_oak_log[axis=y]
fill ~10 ~1 ~4 ~10 ~4 ~4 minecraft:dark_oak_log[axis=y]
fill ~10 ~1 ~14 ~10 ~4 ~14 minecraft:dark_oak_log[axis=y]
fill ~14 ~1 ~4 ~14 ~4 ~4 minecraft:dark_oak_log[axis=y]
fill ~14 ~1 ~14 ~14 ~4 ~14 minecraft:dark_oak_log[axis=y]
fill ~18 ~1 ~4 ~18 ~4 ~4 minecraft:dark_oak_log[axis=y]
fill ~18 ~1 ~14 ~18 ~4 ~14 minecraft:dark_oak_log[axis=y]
fill ~20 ~1 ~4 ~20 ~4 ~4 minecraft:dark_oak_log[axis=y]
fill ~20 ~1 ~14 ~20 ~4 ~14 minecraft:dark_oak_log[axis=y]
fill ~2 ~5 ~4 ~2 ~5 ~14 minecraft:stripped_spruce_log[axis=z]
fill ~3 ~5 ~4 ~20 ~5 ~4 minecraft:stripped_spruce_log[axis=x]
fill ~3 ~5 ~14 ~15 ~5 ~14 minecraft:stripped_spruce_log[axis=x]
fill ~18 ~5 ~14 ~20 ~5 ~14 minecraft:stripped_spruce_log[axis=x]

# tie beams of dark oak across the shed at y 5 (x 6, 10, 14, 18) and at the east end (x 20)
fill ~6 ~5 ~5 ~6 ~5 ~13 minecraft:dark_oak_log[axis=z]
fill ~10 ~5 ~5 ~10 ~5 ~13 minecraft:dark_oak_log[axis=z]
fill ~14 ~5 ~5 ~14 ~5 ~13 minecraft:dark_oak_log[axis=z]
fill ~18 ~5 ~5 ~18 ~5 ~13 minecraft:dark_oak_log[axis=z]
fill ~20 ~5 ~5 ~20 ~5 ~13 minecraft:dark_oak_log[axis=z]

# the roof: shingles of spruce stairs over x 1..21, z 3..15, eaves y 5, ridge of dark oak planks at z 9, y 11; courses of dark oak stairs at the eave (y 5) and again at y 8, here and there a single dark tile; the verge of dark oak. A cross gable (the hoist gable) on the south slope over x 9..13 meets the main roof in a valley: its eave rows y 7, its ridge y 9
fill ~1 ~5 ~3 ~21 ~5 ~3 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~1 ~5 ~15 ~8 ~5 ~15 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~14 ~5 ~15 ~21 ~5 ~15 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~1 ~6 ~4 ~2 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~1 ~6 ~14 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~2 ~6 ~14 ~8 ~6 ~14 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~3 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~4 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~5 ~6 ~4 ~17 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~14 ~6 ~14 ~15 ~6 ~14 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~18 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~18 ~6 ~14 ~20 ~6 ~14 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~19 ~6 ~4 ~20 ~6 ~4 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~21 ~6 ~4 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~21 ~6 ~14 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~1 ~7 ~5 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~1 ~7 ~13 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~2 ~7 ~5 ~7 ~7 ~5 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~2 ~7 ~13 ~9 ~7 ~13 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~9 ~7 ~5 ~13 ~7 ~5 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~9 ~7 ~14 minecraft:spruce_stairs[facing=east,half=bottom]
setblock ~9 ~7 ~15 minecraft:dark_oak_stairs[facing=east,half=bottom]
fill ~13 ~7 ~13 ~15 ~7 ~13 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~13 ~7 ~14 minecraft:spruce_stairs[facing=west,half=bottom]
setblock ~13 ~7 ~15 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~14 ~7 ~5 ~15 ~7 ~5 minecraft:oak_stairs[facing=south,half=bottom]
fill ~16 ~7 ~5 ~17 ~7 ~5 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~18 ~7 ~5 ~19 ~7 ~5 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~18 ~7 ~13 ~20 ~7 ~13 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~20 ~7 ~5 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~21 ~7 ~5 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~21 ~7 ~13 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~1 ~8 ~6 ~13 ~8 ~6 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~1 ~8 ~12 ~2 ~8 ~12 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~3 ~8 ~12 ~4 ~8 ~12 minecraft:oak_stairs[facing=north,half=bottom]
fill ~5 ~8 ~12 ~10 ~8 ~12 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~10 ~8 ~13 ~10 ~8 ~15 minecraft:dark_oak_stairs[facing=east,half=bottom]
fill ~12 ~8 ~12 ~21 ~8 ~12 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~12 ~8 ~13 ~12 ~8 ~15 minecraft:dark_oak_stairs[facing=west,half=bottom]
fill ~14 ~8 ~6 ~15 ~8 ~6 minecraft:oak_stairs[facing=south,half=bottom]
fill ~16 ~8 ~6 ~21 ~8 ~6 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~1 ~9 ~7 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~1 ~9 ~11 ~2 ~9 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~2 ~9 ~7 ~20 ~9 ~7 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~3 ~9 ~11 ~4 ~9 ~11 minecraft:oak_stairs[facing=north,half=bottom]
fill ~5 ~9 ~11 ~9 ~9 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~10 ~9 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~11 ~9 ~11 ~17 ~9 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
fill ~11 ~9 ~12 ~11 ~9 ~15 minecraft:dark_oak_planks
fill ~18 ~9 ~11 ~19 ~9 ~11 minecraft:oak_stairs[facing=north,half=bottom]
setblock ~20 ~9 ~11 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~21 ~9 ~7 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~21 ~9 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~1 ~10 ~8 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~1 ~10 ~10 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~2 ~10 ~8 ~4 ~10 ~8 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~2 ~10 ~10 ~8 ~10 ~10 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~5 ~10 ~8 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~6 ~10 ~8 ~15 ~10 ~8 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~9 ~10 ~10 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~10 ~10 ~10 ~13 ~10 ~10 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~14 ~10 ~10 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~15 ~10 ~10 ~17 ~10 ~10 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~16 ~10 ~8 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~17 ~10 ~8 ~20 ~10 ~8 minecraft:spruce_stairs[facing=south,half=bottom]
fill ~18 ~10 ~10 ~19 ~10 ~10 minecraft:oak_stairs[facing=north,half=bottom]
setblock ~20 ~10 ~10 minecraft:spruce_stairs[facing=north,half=bottom]
setblock ~21 ~10 ~8 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~21 ~10 ~10 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~1 ~11 ~9 ~21 ~11 ~9 minecraft:dark_oak_planks

# the hoist gable: boards under its eave rows where the main slope is lower (the cheeks), its front face at z 14 filled with boards down to the plate, a hatch one wide and two high at (11, 6..7, 14) framed by dark oak logs, a beam of logs out over the yard (x 11, y 8, z 15..17) with a chain and a lantern for the hook
setblock ~11 ~4 ~17 minecraft:lantern[hanging=true]
setblock ~9 ~5 ~15 minecraft:dark_oak_planks
fill ~11 ~5 ~17 ~11 ~7 ~17 minecraft:chain[axis=y]
fill ~13 ~5 ~15 ~13 ~6 ~15 minecraft:spruce_planks
fill ~9 ~6 ~14 ~9 ~6 ~15 minecraft:spruce_planks
fill ~10 ~6 ~14 ~10 ~7 ~14 minecraft:dark_oak_log[axis=y]
fill ~11 ~6 ~14 ~11 ~7 ~14 minecraft:air
fill ~12 ~6 ~14 ~12 ~7 ~14 minecraft:dark_oak_log[axis=y]
setblock ~13 ~6 ~14 minecraft:spruce_planks
setblock ~11 ~7 ~15 minecraft:dark_oak_stairs[facing=south,half=top]
setblock ~11 ~8 ~14 minecraft:spruce_planks
fill ~11 ~8 ~15 ~11 ~8 ~17 minecraft:dark_oak_log[axis=z]

# the west gable (x 2) filled with boards in a dark frame to the roof, a loft opening at z 8..10, y 7..8 framed by dark oak
fill ~2 ~6 ~5 ~2 ~6 ~6 minecraft:spruce_planks
setblock ~2 ~6 ~7 minecraft:dark_oak_planks
fill ~2 ~6 ~8 ~2 ~6 ~10 minecraft:dark_oak_log[axis=z]
fill ~2 ~6 ~11 ~2 ~6 ~13 minecraft:spruce_planks
setblock ~2 ~7 ~6 minecraft:dark_oak_planks
fill ~2 ~7 ~7 ~2 ~8 ~7 minecraft:spruce_planks
fill ~2 ~7 ~8 ~2 ~8 ~10 minecraft:air
setblock ~2 ~7 ~11 minecraft:dark_oak_planks
setblock ~2 ~7 ~12 minecraft:spruce_planks
setblock ~2 ~8 ~11 minecraft:spruce_planks
fill ~2 ~9 ~8 ~2 ~9 ~10 minecraft:dark_oak_log[axis=z]
setblock ~2 ~10 ~9 minecraft:dark_oak_log[axis=y]

# the east end is open: a truss of dark oak at x 20 (a king post at z 9, y 6..9 and a pair of braces) under the verge, the gable above it left open to the sky of the roof frame
setblock ~20 ~6 ~5 minecraft:dark_oak_stairs[facing=south,half=bottom]
fill ~20 ~6 ~9 ~20 ~9 ~9 minecraft:dark_oak_log[axis=y]
setblock ~20 ~6 ~13 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~20 ~7 ~6 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~20 ~7 ~12 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~20 ~8 ~7 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~20 ~8 ~11 minecraft:dark_oak_stairs[facing=north,half=bottom]

# the kiln chimney at x 16..17, z 13..14 (y 1..11), standing in the south side of the shed clear of the aisle (z 10..12) and rising through the south slope of the roof: stone bricks, cobblestone and moss, a furnace and a smoker lit in its south face, a campfire in a crenellated cap (y 12) for the smoke
fill ~16 ~1 ~13 ~16 ~2 ~13 minecraft:cobblestone
setblock ~16 ~1 ~14 minecraft:furnace[facing=south,lit=true]
setblock ~17 ~1 ~13 minecraft:stone_bricks
setblock ~17 ~1 ~14 minecraft:smoker[facing=south,lit=true]
setblock ~16 ~2 ~14 minecraft:cracked_stone_bricks
setblock ~17 ~2 ~13 minecraft:cobblestone
fill ~17 ~2 ~14 ~17 ~3 ~14 minecraft:stone_bricks
fill ~16 ~3 ~13 ~17 ~3 ~13 minecraft:stone_bricks
setblock ~16 ~3 ~14 minecraft:mossy_stone_bricks
setblock ~16 ~4 ~13 minecraft:mossy_stone_bricks
fill ~16 ~4 ~14 ~16 ~5 ~14 minecraft:stone_bricks
fill ~17 ~4 ~13 ~17 ~4 ~14 minecraft:cobblestone
fill ~16 ~5 ~13 ~16 ~6 ~13 minecraft:cobblestone
fill ~17 ~5 ~13 ~17 ~6 ~13 minecraft:mossy_stone_bricks
fill ~17 ~5 ~14 ~17 ~6 ~14 minecraft:cobblestone
setblock ~16 ~6 ~14 minecraft:mossy_stone_bricks
setblock ~16 ~7 ~13 minecraft:stone_bricks
setblock ~16 ~7 ~14 minecraft:cracked_stone_bricks
setblock ~17 ~7 ~13 minecraft:cracked_stone_bricks
setblock ~17 ~7 ~14 minecraft:mossy_stone_bricks
setblock ~16 ~8 ~13 minecraft:cobblestone
setblock ~16 ~8 ~14 minecraft:stone_bricks
fill ~17 ~8 ~13 ~17 ~8 ~14 minecraft:stone_bricks
setblock ~16 ~9 ~13 minecraft:mossy_stone_bricks
setblock ~16 ~9 ~14 minecraft:cracked_stone_bricks
setblock ~17 ~9 ~13 minecraft:cracked_stone_bricks
fill ~17 ~9 ~14 ~17 ~11 ~14 minecraft:stone_bricks
fill ~16 ~10 ~13 ~16 ~11 ~13 minecraft:stone_bricks
setblock ~16 ~10 ~14 minecraft:mossy_stone_bricks
setblock ~17 ~10 ~13 minecraft:mossy_stone_bricks
setblock ~16 ~11 ~14 minecraft:stone_bricks
setblock ~17 ~11 ~13 minecraft:stone_bricks
setblock ~16 ~12 ~13 minecraft:campfire[lit=true,signal_fire=false,facing=north]
setblock ~16 ~12 ~14 minecraft:stone_brick_wall[north=none,east=low,south=none,west=none,up=true]
setblock ~17 ~12 ~13 minecraft:stone_brick_wall[north=none,east=none,south=low,west=none,up=true]
setblock ~17 ~12 ~14 minecraft:stone_brick_wall[north=low,east=none,south=none,west=low,up=true]

# the saw deck x 3..14, z 5..8: a platform two blocks high of planks in a log frame (y 1..2), top at y 3
fill ~3 ~1 ~5 ~14 ~2 ~5 minecraft:spruce_log[axis=x]
fill ~3 ~1 ~6 ~3 ~2 ~7 minecraft:spruce_log[axis=z]
fill ~3 ~1 ~8 ~14 ~2 ~8 minecraft:spruce_log[axis=x]
fill ~4 ~1 ~6 ~4 ~2 ~7 minecraft:spruce_planks
fill ~5 ~1 ~6 ~5 ~2 ~6 minecraft:dark_oak_planks
fill ~5 ~1 ~7 ~6 ~2 ~7 minecraft:spruce_planks
fill ~6 ~1 ~6 ~8 ~1 ~6 minecraft:spruce_planks
setblock ~7 ~1 ~7 minecraft:dark_oak_planks
fill ~8 ~1 ~7 ~9 ~1 ~7 minecraft:spruce_planks
setblock ~9 ~1 ~6 minecraft:dark_oak_planks
fill ~10 ~1 ~6 ~13 ~1 ~7 minecraft:spruce_planks
fill ~14 ~1 ~6 ~14 ~2 ~7 minecraft:spruce_log[axis=z]
fill ~6 ~2 ~6 ~7 ~2 ~6 minecraft:spruce_planks
fill ~7 ~2 ~7 ~8 ~2 ~7 minecraft:spruce_planks
setblock ~8 ~2 ~6 minecraft:dark_oak_planks
fill ~9 ~2 ~6 ~13 ~2 ~6 minecraft:spruce_planks
setblock ~9 ~2 ~7 minecraft:dark_oak_planks
fill ~10 ~2 ~7 ~12 ~2 ~7 minecraft:spruce_planks
setblock ~13 ~2 ~7 minecraft:dark_oak_planks

# the log ramp from the east: stairs at x 16 (y 1) and x 15 (y 2) on a block, z 6..8, with logs rolling up it
fill ~15 ~1 ~5 ~16 ~1 ~5 minecraft:spruce_log[axis=x]
fill ~15 ~1 ~6 ~15 ~1 ~8 minecraft:spruce_planks
fill ~15 ~1 ~9 ~16 ~1 ~9 minecraft:spruce_log[axis=x]
fill ~16 ~1 ~6 ~16 ~1 ~8 minecraft:spruce_stairs[facing=west,half=bottom]
fill ~15 ~2 ~6 ~15 ~2 ~8 minecraft:spruce_stairs[facing=west,half=bottom]

# the saw frame at x 8: two posts of dark oak logs y 3..7 at z 5 and z 8, a cross beam at y 7, the blade of iron bars between them (y 3..6, z 6..7)
fill ~8 ~3 ~5 ~8 ~7 ~5 minecraft:dark_oak_log[axis=y]
setblock ~8 ~3 ~6 minecraft:iron_bars[north=true,east=true,south=true,west=true]
setblock ~8 ~3 ~7 minecraft:iron_bars[north=true,east=false,south=true,west=true]
fill ~8 ~3 ~8 ~8 ~7 ~8 minecraft:dark_oak_log[axis=y]
fill ~8 ~4 ~6 ~8 ~6 ~7 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~8 ~7 ~6 ~8 ~7 ~7 minecraft:dark_oak_log[axis=z]

# logs on the deck: three spruce logs end to end waiting at the saw (x 3..7 and x 9..13, z 6 and z 7, y 3), cut boards lying beyond the blade
fill ~3 ~3 ~6 ~7 ~3 ~7 minecraft:spruce_log[axis=x]
fill ~9 ~3 ~6 ~13 ~3 ~6 minecraft:stripped_spruce_log[axis=x]
fill ~10 ~3 ~7 ~13 ~3 ~7 minecraft:spruce_planks
setblock ~10 ~4 ~7 minecraft:spruce_planks
setblock ~12 ~4 ~7 minecraft:spruce_planks

# a lantern post at the head of the ramp and a barrel of saw oil by the frame
fill ~16 ~2 ~5 ~16 ~4 ~5 minecraft:dark_oak_fence[north=true,east=true,south=false,west=false]
fill ~13 ~3 ~5 ~14 ~3 ~5 minecraft:barrel[facing=up]
setblock ~16 ~5 ~5 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
setblock ~16 ~6 ~5 minecraft:lantern[hanging=false]

# the stonecutter bench in the south-west corner: three stonecutters side by side on spruce slabs against the half wall, a crafting table and a grindstone at the end, an anvil for sharpening the saws
setblock ~3 ~1 ~12 minecraft:anvil[facing=east]
fill ~3 ~1 ~13 ~5 ~1 ~13 minecraft:stonecutter[facing=south]
setblock ~6 ~1 ~13 minecraft:crafting_table
setblock ~7 ~1 ~13 minecraft:grindstone[face=floor,facing=north]
fill ~8 ~1 ~13 ~8 ~2 ~13 minecraft:barrel[facing=up]

# stacks of sawn planks in the north-east corner (x 17..19, z 5..8): layers of planks and spacer logs, four high; a cart of boards at the end
fill ~17 ~1 ~5 ~19 ~1 ~8 minecraft:spruce_planks
fill ~17 ~2 ~5 ~19 ~2 ~5 minecraft:oak_planks
fill ~17 ~2 ~6 ~19 ~2 ~6 minecraft:spruce_log[axis=x]
fill ~17 ~2 ~7 ~19 ~2 ~7 minecraft:oak_planks
fill ~17 ~2 ~8 ~19 ~2 ~8 minecraft:spruce_log[axis=x]
fill ~17 ~3 ~5 ~19 ~3 ~8 minecraft:spruce_planks
fill ~17 ~4 ~5 ~19 ~4 ~5 minecraft:oak_planks
fill ~17 ~4 ~6 ~19 ~4 ~6 minecraft:spruce_log[axis=x]
fill ~17 ~4 ~7 ~19 ~4 ~7 minecraft:oak_planks
fill ~17 ~4 ~8 ~19 ~4 ~8 minecraft:spruce_log[axis=x]

# a bench along the south posts with tools, lanterns hung from the tie beams over the aisle (x 10, 14, 18 at z 11 and z 12), a barrel of water
setblock ~11 ~1 ~13 minecraft:water_cauldron[level=3]
setblock ~12 ~1 ~13 minecraft:barrel[facing=up]
setblock ~6 ~4 ~11 minecraft:lantern[hanging=true]
setblock ~10 ~4 ~11 minecraft:lantern[hanging=true]
setblock ~14 ~4 ~12 minecraft:lantern[hanging=true]
setblock ~18 ~4 ~11 minecraft:lantern[hanging=true]

# log piles in the east yard: two stacks of spruce logs (x 21..24, z 1..2 and z 4..5, three high, pyramid ends), logs lying across the lane head
fill ~21 ~1 ~1 ~24 ~1 ~2 minecraft:spruce_log[axis=x]
fill ~21 ~1 ~4 ~24 ~1 ~5 minecraft:spruce_log[axis=x]
fill ~22 ~2 ~1 ~23 ~2 ~2 minecraft:spruce_log[axis=x]
fill ~22 ~2 ~4 ~23 ~2 ~5 minecraft:spruce_log[axis=x]
fill ~22 ~3 ~1 ~23 ~3 ~1 minecraft:spruce_log[axis=x]
fill ~22 ~3 ~4 ~23 ~3 ~4 minecraft:spruce_log[axis=x]
fill ~22 ~4 ~1 ~23 ~4 ~1 minecraft:stripped_spruce_log[axis=x]
setblock ~22 ~4 ~4 minecraft:spruce_log[axis=x]
setblock ~23 ~4 ~4 minecraft:stripped_spruce_log[axis=x]

# two stacks of sawn boards in the open on the south side (x 4..7 and x 14..17, z 16..17, four high; planks and spacer logs): the seasoning yard
fill ~4 ~1 ~16 ~7 ~1 ~17 minecraft:spruce_planks
fill ~14 ~1 ~16 ~17 ~1 ~17 minecraft:spruce_planks
setblock ~4 ~2 ~16 minecraft:spruce_log[axis=x]
setblock ~4 ~2 ~17 minecraft:oak_planks
setblock ~5 ~2 ~16 minecraft:oak_planks
setblock ~5 ~2 ~17 minecraft:spruce_log[axis=x]
setblock ~6 ~2 ~16 minecraft:spruce_log[axis=x]
setblock ~6 ~2 ~17 minecraft:oak_planks
setblock ~7 ~2 ~16 minecraft:oak_planks
setblock ~7 ~2 ~17 minecraft:spruce_log[axis=x]
setblock ~14 ~2 ~16 minecraft:spruce_log[axis=x]
setblock ~14 ~2 ~17 minecraft:oak_planks
setblock ~15 ~2 ~16 minecraft:oak_planks
setblock ~15 ~2 ~17 minecraft:spruce_log[axis=x]
setblock ~16 ~2 ~16 minecraft:spruce_log[axis=x]
setblock ~16 ~2 ~17 minecraft:oak_planks
setblock ~17 ~2 ~16 minecraft:oak_planks
setblock ~17 ~2 ~17 minecraft:spruce_log[axis=x]
fill ~4 ~3 ~16 ~7 ~3 ~17 minecraft:spruce_planks
fill ~14 ~3 ~16 ~17 ~3 ~17 minecraft:spruce_planks
setblock ~5 ~4 ~16 minecraft:oak_planks
setblock ~5 ~4 ~17 minecraft:spruce_log[axis=x]
setblock ~6 ~4 ~16 minecraft:spruce_log[axis=x]
setblock ~6 ~4 ~17 minecraft:oak_planks
setblock ~15 ~4 ~16 minecraft:oak_planks
setblock ~15 ~4 ~17 minecraft:spruce_log[axis=x]
setblock ~16 ~4 ~16 minecraft:spruce_log[axis=x]
setblock ~16 ~4 ~17 minecraft:oak_planks

# a log skid in the yard north of the lane (x 21..23, z 10..11): a bed of planks on four fence legs with three spruce logs chained on it, a shaft of fence posts
setblock ~21 ~1 ~10 minecraft:dark_oak_fence[north=false,east=false,south=true,west=false]
setblock ~21 ~1 ~11 minecraft:dark_oak_fence[north=true,east=false,south=true,west=false]
setblock ~23 ~1 ~10 minecraft:dark_oak_fence[north=false,east=false,south=true,west=false]
setblock ~23 ~1 ~11 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
fill ~21 ~2 ~10 ~23 ~2 ~11 minecraft:spruce_planks
setblock ~24 ~2 ~10 minecraft:dark_oak_fence[north=true,east=false,south=true,west=true]
setblock ~24 ~2 ~11 minecraft:dark_oak_fence[north=true,east=false,south=false,west=true]
fill ~21 ~3 ~10 ~23 ~3 ~10 minecraft:spruce_log[axis=x]
fill ~21 ~3 ~11 ~23 ~3 ~11 minecraft:stripped_spruce_log[axis=x]
fill ~21 ~4 ~10 ~22 ~4 ~10 minecraft:spruce_log[axis=x]

# sawhorses (two dark oak fence posts with a log across) at (21..23, 7..8) with a half-cut log, a chopping block and an axe stump
setblock ~21 ~1 ~7 minecraft:dark_oak_fence[north=false,east=false,south=true,west=false]
setblock ~21 ~1 ~8 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
setblock ~23 ~1 ~7 minecraft:dark_oak_fence[north=false,east=true,south=true,west=false]
setblock ~23 ~1 ~8 minecraft:dark_oak_fence[north=true,east=true,south=false,west=false]
setblock ~24 ~1 ~7 minecraft:stripped_spruce_log[axis=y]
setblock ~24 ~1 ~8 minecraft:spruce_log[axis=x]
fill ~21 ~2 ~7 ~23 ~2 ~8 minecraft:spruce_log[axis=x]

# a lantern post at the south-east corner of the shed and one in the yard; a signboard on the north-east corner post
fill ~21 ~1 ~12 ~21 ~3 ~12 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
setblock ~24 ~1 ~9 minecraft:dark_oak_fence[north=true,east=false,south=false,west=false]
setblock ~24 ~2 ~9 minecraft:dark_oak_fence[north=false,east=false,south=true,west=false]
setblock ~24 ~3 ~9 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~21 ~4 ~12 minecraft:lantern[hanging=false]
setblock ~24 ~4 ~9 minecraft:lantern[hanging=false]

# moss on the cobble foot and the north wall, vines on the north and west faces, a few cobwebs in the roof frame
fill ~1 ~1 ~13 ~1 ~2 ~13 minecraft:vine[east=true]
fill ~4 ~2 ~3 ~4 ~3 ~3 minecraft:vine[south=true]
fill ~8 ~2 ~3 ~8 ~3 ~3 minecraft:vine[south=true]
fill ~1 ~3 ~5 ~1 ~4 ~5 minecraft:vine[east=true]
fill ~2 ~3 ~15 ~2 ~4 ~15 minecraft:vine[north=true]
setblock ~3 ~3 ~5 minecraft:vine[west=true]
fill ~3 ~3 ~12 ~3 ~7 ~12 minecraft:vine[west=true]
fill ~18 ~3 ~3 ~18 ~4 ~3 minecraft:vine[south=true]
setblock ~3 ~4 ~5 minecraft:vine[north=true]
fill ~3 ~4 ~10 ~3 ~5 ~10 minecraft:vine[west=true]
setblock ~3 ~5 ~13 minecraft:vine[south=true]
setblock ~3 ~6 ~8 minecraft:vine[west=true]
setblock ~3 ~10 ~9 minecraft:cobweb
