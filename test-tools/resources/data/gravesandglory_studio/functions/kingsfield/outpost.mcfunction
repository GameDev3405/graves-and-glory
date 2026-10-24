# size 47 14 47
# kingsfield/outpost: flank outpost on a raised point: a spruce log stockade with two gates, three lookouts, low stone breastworks inside and a paved yard round the point.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Centre block (23, 0, 23) is the point; the ring cells of the arena (16 <= r <= 17, the cells Arena.finishBuild lays in smooth stone) and the centre 3x3 are void at y 0 (the arena keeps its ring and pad); the band cells round them (15.5..16 and 17..17.5) are paving and earth of the piece, so no lone dirt path of the arena road is left between ring and paving, the flag column |dx|, |dz| <= 3 is void at y 1..8.
# Stockade r 21..23, y 1..4 logs and y 5 fence; gates open y 1..5 within 3.5 of the west road (z 23) and of the centre road toward (125, -77). Lookouts: SE x 37..39 z 37..39, SW x 7..9 z 37..39, NW x 7..9 z 7..9.

# the clearing inside and round the stockade: air at y 1..6 from radius 19.5 out to the stockade line (the old stockade of the arena dressing stood at radius 21), and over every gate
fill ~0 ~1 ~19 ~0 ~6 ~22 minecraft:air
fill ~0 ~1 ~24 ~0 ~6 ~27 minecraft:air
fill ~1 ~1 ~15 ~1 ~6 ~16 minecraft:air
fill ~1 ~1 ~30 ~1 ~6 ~31 minecraft:air
fill ~2 ~1 ~13 ~2 ~6 ~13 minecraft:air
fill ~2 ~1 ~33 ~2 ~6 ~33 minecraft:air
fill ~3 ~1 ~11 ~3 ~6 ~11 minecraft:air
fill ~3 ~1 ~17 ~3 ~6 ~29 minecraft:air
fill ~3 ~1 ~35 ~3 ~6 ~35 minecraft:air
fill ~4 ~1 ~10 ~4 ~6 ~10 minecraft:air
fill ~4 ~1 ~15 ~4 ~6 ~18 minecraft:air
fill ~4 ~1 ~28 ~4 ~6 ~31 minecraft:air
fill ~4 ~1 ~36 ~4 ~6 ~36 minecraft:air
fill ~5 ~1 ~8 ~5 ~6 ~8 minecraft:air
fill ~5 ~1 ~13 ~5 ~6 ~14 minecraft:air
fill ~5 ~1 ~31 ~5 ~6 ~33 minecraft:air
fill ~5 ~1 ~38 ~5 ~6 ~38 minecraft:air
fill ~6 ~1 ~7 ~6 ~6 ~7 minecraft:air
fill ~6 ~1 ~11 ~6 ~6 ~13 minecraft:air
fill ~6 ~1 ~33 ~6 ~6 ~35 minecraft:air
fill ~6 ~1 ~39 ~6 ~6 ~39 minecraft:air
fill ~7 ~1 ~6 ~7 ~6 ~6 minecraft:air
fill ~7 ~1 ~10 ~7 ~6 ~11 minecraft:air
fill ~7 ~1 ~35 ~7 ~6 ~36 minecraft:air
fill ~7 ~1 ~40 ~7 ~6 ~40 minecraft:air
fill ~8 ~1 ~5 ~8 ~6 ~5 minecraft:air
fill ~8 ~1 ~41 ~8 ~6 ~41 minecraft:air
fill ~10 ~1 ~4 ~10 ~6 ~4 minecraft:air
fill ~10 ~1 ~7 ~11 ~6 ~7 minecraft:air
fill ~10 ~1 ~8 ~10 ~6 ~8 minecraft:air
fill ~10 ~1 ~38 ~10 ~6 ~39 minecraft:air
fill ~10 ~1 ~42 ~10 ~6 ~42 minecraft:air
fill ~11 ~1 ~3 ~11 ~6 ~3 minecraft:air
fill ~11 ~1 ~6 ~13 ~6 ~6 minecraft:air
fill ~11 ~1 ~39 ~11 ~6 ~40 minecraft:air
fill ~11 ~1 ~43 ~11 ~6 ~43 minecraft:air
fill ~12 ~1 ~40 ~13 ~6 ~40 minecraft:air
fill ~13 ~1 ~2 ~13 ~6 ~2 minecraft:air
fill ~13 ~1 ~5 ~15 ~6 ~5 minecraft:air
fill ~13 ~1 ~41 ~14 ~6 ~41 minecraft:air
fill ~13 ~1 ~44 ~13 ~6 ~44 minecraft:air
fill ~15 ~1 ~1 ~16 ~6 ~1 minecraft:air
fill ~15 ~1 ~4 ~18 ~6 ~4 minecraft:air
fill ~15 ~1 ~42 ~18 ~6 ~42 minecraft:air
fill ~15 ~1 ~45 ~16 ~6 ~45 minecraft:air
fill ~17 ~1 ~3 ~29 ~6 ~3 minecraft:air
fill ~17 ~1 ~43 ~20 ~6 ~43 minecraft:air
fill ~19 ~1 ~0 ~22 ~6 ~0 minecraft:air
fill ~19 ~1 ~46 ~22 ~6 ~46 minecraft:air
fill ~24 ~1 ~0 ~27 ~6 ~0 minecraft:air
fill ~24 ~1 ~46 ~27 ~6 ~46 minecraft:air
fill ~26 ~1 ~43 ~29 ~6 ~43 minecraft:air
fill ~28 ~1 ~4 ~31 ~6 ~4 minecraft:air
fill ~28 ~1 ~42 ~31 ~6 ~42 minecraft:air
fill ~30 ~1 ~1 ~31 ~6 ~1 minecraft:air
fill ~30 ~1 ~45 ~31 ~6 ~45 minecraft:air
fill ~31 ~1 ~5 ~33 ~6 ~5 minecraft:air
fill ~32 ~1 ~41 ~33 ~6 ~41 minecraft:air
fill ~33 ~1 ~2 ~33 ~6 ~2 minecraft:air
fill ~33 ~1 ~6 ~35 ~6 ~6 minecraft:air
fill ~33 ~1 ~40 ~34 ~6 ~40 minecraft:air
fill ~33 ~1 ~44 ~33 ~6 ~44 minecraft:air
fill ~35 ~1 ~3 ~35 ~6 ~3 minecraft:air
fill ~35 ~1 ~7 ~36 ~6 ~7 minecraft:air
fill ~35 ~1 ~39 ~35 ~6 ~40 minecraft:air
fill ~35 ~1 ~43 ~35 ~6 ~43 minecraft:air
fill ~36 ~1 ~4 ~36 ~6 ~4 minecraft:air
fill ~36 ~1 ~8 ~37 ~6 ~8 minecraft:air
fill ~36 ~1 ~38 ~36 ~6 ~39 minecraft:air
fill ~36 ~1 ~42 ~36 ~6 ~42 minecraft:air
fill ~37 ~1 ~9 ~38 ~6 ~9 minecraft:air
fill ~38 ~1 ~5 ~38 ~6 ~5 minecraft:air
fill ~38 ~1 ~10 ~39 ~6 ~10 minecraft:air
fill ~38 ~1 ~41 ~38 ~6 ~41 minecraft:air
fill ~39 ~1 ~6 ~39 ~6 ~6 minecraft:air
fill ~39 ~1 ~11 ~40 ~6 ~11 minecraft:air
fill ~39 ~1 ~35 ~39 ~6 ~36 minecraft:air
fill ~39 ~1 ~40 ~39 ~6 ~40 minecraft:air
fill ~40 ~1 ~7 ~40 ~6 ~7 minecraft:air
fill ~40 ~1 ~12 ~40 ~6 ~13 minecraft:air
fill ~40 ~1 ~33 ~40 ~6 ~35 minecraft:air
fill ~40 ~1 ~39 ~40 ~6 ~39 minecraft:air
fill ~41 ~1 ~8 ~41 ~6 ~8 minecraft:air
fill ~41 ~1 ~13 ~41 ~6 ~15 minecraft:air
fill ~41 ~1 ~31 ~41 ~6 ~33 minecraft:air
fill ~41 ~1 ~38 ~41 ~6 ~38 minecraft:air
fill ~42 ~1 ~10 ~42 ~6 ~10 minecraft:air
fill ~42 ~1 ~15 ~42 ~6 ~18 minecraft:air
fill ~42 ~1 ~28 ~42 ~6 ~31 minecraft:air
fill ~42 ~1 ~36 ~42 ~6 ~36 minecraft:air
fill ~43 ~1 ~11 ~43 ~6 ~11 minecraft:air
fill ~43 ~1 ~17 ~43 ~6 ~29 minecraft:air
fill ~43 ~1 ~35 ~43 ~6 ~35 minecraft:air
fill ~44 ~1 ~13 ~44 ~6 ~13 minecraft:air
fill ~44 ~1 ~33 ~44 ~6 ~33 minecraft:air
fill ~45 ~1 ~15 ~45 ~6 ~16 minecraft:air
fill ~45 ~1 ~30 ~45 ~6 ~31 minecraft:air
fill ~46 ~1 ~19 ~46 ~6 ~22 minecraft:air
fill ~46 ~1 ~24 ~46 ~6 ~27 minecraft:air
fill ~21 ~2 ~43 ~25 ~6 ~43 minecraft:air
fill ~5 ~3 ~15 ~5 ~6 ~15 minecraft:air
fill ~15 ~3 ~41 ~15 ~6 ~41 minecraft:air
fill ~31 ~3 ~41 ~31 ~6 ~41 minecraft:air
fill ~2 ~5 ~32 ~2 ~6 ~32 minecraft:air
fill ~3 ~5 ~12 ~3 ~6 ~12 minecraft:air
fill ~4 ~5 ~35 ~4 ~6 ~35 minecraft:air
fill ~5 ~5 ~10 ~6 ~6 ~10 minecraft:air
fill ~6 ~5 ~8 ~6 ~6 ~8 minecraft:air
fill ~6 ~5 ~37 ~6 ~6 ~37 minecraft:air
fill ~8 ~5 ~6 ~8 ~6 ~6 minecraft:air
fill ~8 ~5 ~40 ~8 ~6 ~40 minecraft:air
fill ~9 ~5 ~5 ~9 ~6 ~5 minecraft:air
fill ~11 ~5 ~4 ~11 ~6 ~4 minecraft:air
fill ~12 ~5 ~41 ~12 ~6 ~41 minecraft:air
fill ~15 ~5 ~43 ~15 ~6 ~43 minecraft:air
fill ~16 ~5 ~44 ~16 ~6 ~44 minecraft:air
fill ~17 ~5 ~1 ~17 ~6 ~1 minecraft:air
fill ~18 ~5 ~44 ~18 ~6 ~44 minecraft:air
fill ~22 ~5 ~1 ~22 ~6 ~1 minecraft:air
fill ~23 ~5 ~2 ~24 ~6 ~2 minecraft:air
fill ~26 ~5 ~2 ~26 ~6 ~2 minecraft:air
fill ~28 ~5 ~1 ~28 ~6 ~1 minecraft:air
fill ~30 ~5 ~43 ~30 ~6 ~43 minecraft:air
fill ~40 ~5 ~37 ~40 ~6 ~37 minecraft:air
fill ~41 ~5 ~35 ~41 ~6 ~35 minecraft:air
fill ~42 ~5 ~13 ~42 ~6 ~13 minecraft:air
fill ~43 ~5 ~12 ~43 ~6 ~12 minecraft:air
fill ~43 ~5 ~14 ~43 ~6 ~14 minecraft:air
fill ~43 ~5 ~34 ~43 ~6 ~34 minecraft:air
fill ~44 ~5 ~18 ~44 ~6 ~18 minecraft:air
fill ~44 ~5 ~25 ~44 ~6 ~25 minecraft:air
fill ~45 ~5 ~24 ~45 ~6 ~24 minecraft:air
setblock ~0 ~6 ~23 minecraft:air
fill ~1 ~6 ~17 ~1 ~6 ~18 minecraft:air
fill ~1 ~6 ~28 ~2 ~6 ~29 minecraft:air
fill ~2 ~6 ~14 ~2 ~6 ~18 minecraft:air
fill ~2 ~6 ~20 ~2 ~6 ~26 minecraft:air
fill ~2 ~6 ~30 ~3 ~6 ~31 minecraft:air
fill ~3 ~6 ~13 ~4 ~6 ~14 minecraft:air
fill ~3 ~6 ~15 ~3 ~6 ~16 minecraft:air
fill ~3 ~6 ~32 ~4 ~6 ~34 minecraft:air
fill ~4 ~6 ~11 ~5 ~6 ~12 minecraft:air
fill ~5 ~6 ~9 ~6 ~6 ~9 minecraft:air
fill ~5 ~6 ~34 ~5 ~6 ~37 minecraft:air
setblock ~6 ~6 ~36 minecraft:air
setblock ~6 ~6 ~38 minecraft:air
fill ~9 ~6 ~6 ~10 ~6 ~6 minecraft:air
fill ~9 ~6 ~40 ~10 ~6 ~41 minecraft:air
fill ~10 ~6 ~5 ~12 ~6 ~5 minecraft:air
fill ~11 ~6 ~41 ~11 ~6 ~42 minecraft:air
fill ~12 ~6 ~3 ~16 ~6 ~3 minecraft:air
fill ~12 ~6 ~4 ~14 ~6 ~4 minecraft:air
fill ~12 ~6 ~42 ~14 ~6 ~43 minecraft:air
fill ~14 ~6 ~2 ~17 ~6 ~2 minecraft:air
fill ~14 ~6 ~44 ~15 ~6 ~44 minecraft:air
setblock ~16 ~6 ~43 minecraft:air
fill ~17 ~6 ~44 ~17 ~6 ~45 minecraft:air
fill ~18 ~6 ~1 ~21 ~6 ~2 minecraft:air
fill ~18 ~6 ~45 ~29 ~6 ~45 minecraft:air
fill ~19 ~6 ~44 ~32 ~6 ~44 minecraft:air
setblock ~22 ~6 ~2 minecraft:air
fill ~23 ~6 ~0 ~23 ~6 ~1 minecraft:air
setblock ~23 ~6 ~46 minecraft:air
fill ~24 ~6 ~1 ~27 ~6 ~1 minecraft:air
setblock ~25 ~6 ~2 minecraft:air
fill ~27 ~6 ~2 ~28 ~6 ~2 minecraft:air
fill ~29 ~6 ~1 ~29 ~6 ~2 minecraft:air
fill ~30 ~6 ~2 ~32 ~6 ~3 minecraft:air
fill ~31 ~6 ~43 ~34 ~6 ~43 minecraft:air
setblock ~32 ~6 ~4 minecraft:air
fill ~32 ~6 ~42 ~35 ~6 ~42 minecraft:air
fill ~33 ~6 ~3 ~34 ~6 ~4 minecraft:air
setblock ~34 ~6 ~5 minecraft:air
fill ~34 ~6 ~41 ~37 ~6 ~41 minecraft:air
setblock ~35 ~6 ~4 minecraft:air
setblock ~36 ~6 ~6 minecraft:air
fill ~36 ~6 ~40 ~38 ~6 ~40 minecraft:air
setblock ~37 ~6 ~5 minecraft:air
setblock ~37 ~6 ~7 minecraft:air
setblock ~38 ~6 ~6 minecraft:air
setblock ~38 ~6 ~8 minecraft:air
setblock ~39 ~6 ~7 minecraft:air
setblock ~39 ~6 ~9 minecraft:air
setblock ~40 ~6 ~8 minecraft:air
setblock ~40 ~6 ~10 minecraft:air
fill ~40 ~6 ~36 ~41 ~6 ~36 minecraft:air
setblock ~40 ~6 ~38 minecraft:air
setblock ~41 ~6 ~9 minecraft:air
fill ~41 ~6 ~12 ~42 ~6 ~12 minecraft:air
setblock ~41 ~6 ~34 minecraft:air
setblock ~41 ~6 ~37 minecraft:air
setblock ~42 ~6 ~14 minecraft:air
fill ~42 ~6 ~32 ~42 ~6 ~35 minecraft:air
setblock ~43 ~6 ~13 minecraft:air
fill ~43 ~6 ~15 ~43 ~6 ~16 minecraft:air
fill ~43 ~6 ~30 ~43 ~6 ~33 minecraft:air
fill ~44 ~6 ~14 ~44 ~6 ~17 minecraft:air
fill ~44 ~6 ~19 ~44 ~6 ~24 minecraft:air
fill ~44 ~6 ~26 ~44 ~6 ~32 minecraft:air
fill ~45 ~6 ~17 ~45 ~6 ~23 minecraft:air
fill ~45 ~6 ~25 ~45 ~6 ~29 minecraft:air
setblock ~46 ~6 ~23 minecraft:air

# the ground inside radius 15.5: paving of stone bricks and andesite round a trampled earth middle, a compass of trim along the axes (the arena keeps the centre 3x3)
setblock ~8 ~0 ~18 minecraft:stone_bricks
setblock ~8 ~0 ~19 minecraft:andesite
fill ~8 ~0 ~20 ~8 ~0 ~21 minecraft:stone_bricks
fill ~8 ~0 ~22 ~8 ~0 ~23 minecraft:cobblestone
fill ~8 ~0 ~24 ~8 ~0 ~25 minecraft:stone_bricks
setblock ~8 ~0 ~26 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~27 minecraft:andesite
fill ~8 ~0 ~28 ~11 ~0 ~28 minecraft:stone_bricks
fill ~9 ~0 ~16 ~10 ~0 ~16 minecraft:cracked_stone_bricks
fill ~9 ~0 ~17 ~9 ~0 ~18 minecraft:cracked_stone_bricks
setblock ~9 ~0 ~19 minecraft:cobblestone
fill ~9 ~0 ~20 ~9 ~0 ~21 minecraft:andesite
setblock ~9 ~0 ~22 minecraft:mossy_stone_bricks
fill ~9 ~0 ~23 ~10 ~0 ~23 minecraft:stone_bricks
setblock ~9 ~0 ~24 minecraft:andesite
setblock ~9 ~0 ~25 minecraft:mossy_stone_bricks
setblock ~9 ~0 ~26 minecraft:andesite
fill ~9 ~0 ~27 ~10 ~0 ~27 minecraft:cobblestone
setblock ~9 ~0 ~29 minecraft:stone_bricks
setblock ~9 ~0 ~30 minecraft:cracked_stone_bricks
setblock ~10 ~0 ~14 minecraft:cobblestone
setblock ~10 ~0 ~15 minecraft:andesite
fill ~10 ~0 ~17 ~11 ~0 ~17 minecraft:stone_bricks
fill ~10 ~0 ~18 ~10 ~0 ~21 minecraft:stone_bricks
setblock ~10 ~0 ~22 minecraft:andesite
setblock ~10 ~0 ~24 minecraft:cracked_stone_bricks
fill ~10 ~0 ~25 ~10 ~0 ~26 minecraft:stone_bricks
setblock ~10 ~0 ~29 minecraft:andesite
fill ~10 ~0 ~30 ~10 ~0 ~32 minecraft:stone_bricks
setblock ~11 ~0 ~13 minecraft:stone_bricks
setblock ~11 ~0 ~14 minecraft:andesite
setblock ~11 ~0 ~15 minecraft:stone_bricks
setblock ~11 ~0 ~16 minecraft:andesite
setblock ~11 ~0 ~18 minecraft:mossy_stone_bricks
fill ~11 ~0 ~19 ~12 ~0 ~19 minecraft:stone_bricks
setblock ~11 ~0 ~20 minecraft:stone_bricks
fill ~11 ~0 ~21 ~11 ~0 ~22 minecraft:andesite
setblock ~11 ~0 ~23 minecraft:mossy_stone_bricks
fill ~11 ~0 ~24 ~11 ~0 ~26 minecraft:stone_bricks
setblock ~11 ~0 ~27 minecraft:cracked_stone_bricks
fill ~11 ~0 ~29 ~11 ~0 ~32 minecraft:stone_bricks
fill ~11 ~0 ~33 ~12 ~0 ~33 minecraft:cobblestone
fill ~12 ~0 ~12 ~14 ~0 ~12 minecraft:andesite
setblock ~12 ~0 ~13 minecraft:cobblestone
setblock ~12 ~0 ~14 minecraft:stone_bricks
setblock ~12 ~0 ~15 minecraft:andesite
fill ~12 ~0 ~16 ~12 ~0 ~17 minecraft:stone_bricks
setblock ~12 ~0 ~18 minecraft:andesite
fill ~12 ~0 ~20 ~12 ~0 ~21 minecraft:andesite
setblock ~12 ~0 ~22 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~23 minecraft:andesite
fill ~12 ~0 ~24 ~12 ~0 ~25 minecraft:mossy_stone_bricks
setblock ~12 ~0 ~26 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~27 minecraft:cobblestone
fill ~12 ~0 ~28 ~13 ~0 ~29 minecraft:mossy_stone_bricks
setblock ~12 ~0 ~30 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~31 minecraft:andesite
fill ~12 ~0 ~32 ~13 ~0 ~32 minecraft:cobblestone
setblock ~12 ~0 ~34 minecraft:stone_bricks
setblock ~13 ~0 ~11 minecraft:stone_bricks
fill ~13 ~0 ~13 ~13 ~0 ~14 minecraft:mossy_stone_bricks
fill ~13 ~0 ~15 ~14 ~0 ~15 minecraft:stone_bricks
setblock ~13 ~0 ~16 minecraft:andesite
setblock ~13 ~0 ~17 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~18 minecraft:cobblestone
fill ~13 ~0 ~19 ~13 ~0 ~21 minecraft:andesite
setblock ~13 ~0 ~22 minecraft:mossy_stone_bricks
fill ~13 ~0 ~23 ~13 ~0 ~24 minecraft:stone_bricks
setblock ~13 ~0 ~25 minecraft:cobblestone
fill ~13 ~0 ~26 ~13 ~0 ~27 minecraft:stone_bricks
setblock ~13 ~0 ~30 minecraft:stone_bricks
setblock ~13 ~0 ~31 minecraft:cracked_stone_bricks
fill ~13 ~0 ~33 ~13 ~0 ~34 minecraft:stone_bricks
setblock ~13 ~0 ~35 minecraft:cobblestone
fill ~14 ~0 ~10 ~14 ~0 ~11 minecraft:stone_bricks
setblock ~14 ~0 ~13 minecraft:andesite
setblock ~14 ~0 ~14 minecraft:cobblestone
setblock ~14 ~0 ~16 minecraft:cobblestone
fill ~14 ~0 ~17 ~14 ~0 ~18 minecraft:stone_bricks
setblock ~14 ~0 ~19 minecraft:cracked_stone_bricks
fill ~14 ~0 ~20 ~14 ~0 ~21 minecraft:andesite
fill ~14 ~0 ~22 ~21 ~0 ~24 minecraft:stone_bricks
setblock ~14 ~0 ~25 minecraft:stone_bricks
setblock ~14 ~0 ~26 minecraft:mossy_stone_bricks
setblock ~14 ~0 ~27 minecraft:cracked_stone_bricks
setblock ~14 ~0 ~28 minecraft:andesite
fill ~14 ~0 ~29 ~14 ~0 ~35 minecraft:stone_bricks
setblock ~14 ~0 ~36 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~10 minecraft:mossy_stone_bricks
fill ~15 ~0 ~11 ~15 ~0 ~12 minecraft:cobblestone
fill ~15 ~0 ~13 ~16 ~0 ~13 minecraft:stone_bricks
setblock ~15 ~0 ~14 minecraft:stone_bricks
setblock ~15 ~0 ~15 minecraft:andesite
fill ~15 ~0 ~16 ~17 ~0 ~16 minecraft:stone_bricks
setblock ~15 ~0 ~17 minecraft:andesite
setblock ~15 ~0 ~18 minecraft:stone_bricks
fill ~15 ~0 ~19 ~15 ~0 ~20 minecraft:gravel
setblock ~15 ~0 ~21 minecraft:packed_mud
setblock ~15 ~0 ~25 minecraft:packed_mud
setblock ~15 ~0 ~26 minecraft:coarse_dirt
fill ~15 ~0 ~27 ~16 ~0 ~27 minecraft:packed_mud
setblock ~15 ~0 ~28 minecraft:cobblestone
fill ~15 ~0 ~29 ~16 ~0 ~29 minecraft:andesite
fill ~15 ~0 ~30 ~16 ~0 ~30 minecraft:stone_bricks
setblock ~15 ~0 ~31 minecraft:mossy_stone_bricks
setblock ~15 ~0 ~32 minecraft:stone_bricks
fill ~15 ~0 ~33 ~15 ~0 ~34 minecraft:cracked_stone_bricks
fill ~15 ~0 ~35 ~15 ~0 ~36 minecraft:mossy_stone_bricks
fill ~16 ~0 ~9 ~17 ~0 ~9 minecraft:stone_bricks
fill ~16 ~0 ~10 ~16 ~0 ~11 minecraft:andesite
setblock ~16 ~0 ~12 minecraft:cracked_stone_bricks
fill ~16 ~0 ~14 ~17 ~0 ~14 minecraft:mossy_stone_bricks
setblock ~16 ~0 ~15 minecraft:cobblestone
setblock ~16 ~0 ~17 minecraft:cobblestone
fill ~16 ~0 ~18 ~19 ~0 ~18 minecraft:gravel
fill ~16 ~0 ~19 ~17 ~0 ~19 minecraft:coarse_dirt
fill ~16 ~0 ~20 ~16 ~0 ~21 minecraft:packed_mud
setblock ~16 ~0 ~25 minecraft:coarse_dirt
fill ~16 ~0 ~26 ~17 ~0 ~26 minecraft:packed_mud
setblock ~16 ~0 ~28 minecraft:gravel
fill ~16 ~0 ~31 ~17 ~0 ~31 minecraft:stone_bricks
fill ~16 ~0 ~32 ~16 ~0 ~33 minecraft:andesite
fill ~16 ~0 ~34 ~18 ~0 ~34 minecraft:stone_bricks
setblock ~16 ~0 ~35 minecraft:cobblestone
fill ~16 ~0 ~36 ~16 ~0 ~37 minecraft:stone_bricks
fill ~17 ~0 ~10 ~17 ~0 ~11 minecraft:stone_bricks
fill ~17 ~0 ~12 ~17 ~0 ~13 minecraft:andesite
setblock ~17 ~0 ~15 minecraft:cracked_stone_bricks
fill ~17 ~0 ~17 ~20 ~0 ~17 minecraft:coarse_dirt
setblock ~17 ~0 ~20 minecraft:coarse_dirt
setblock ~17 ~0 ~21 minecraft:packed_mud
setblock ~17 ~0 ~25 minecraft:gravel
fill ~17 ~0 ~27 ~17 ~0 ~28 minecraft:coarse_dirt
setblock ~17 ~0 ~29 minecraft:packed_mud
setblock ~17 ~0 ~30 minecraft:mossy_stone_bricks
setblock ~17 ~0 ~32 minecraft:stone_bricks
setblock ~17 ~0 ~33 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~35 minecraft:stone_bricks
setblock ~17 ~0 ~36 minecraft:andesite
setblock ~17 ~0 ~37 minecraft:mossy_stone_bricks
setblock ~18 ~0 ~8 minecraft:cracked_stone_bricks
setblock ~18 ~0 ~9 minecraft:andesite
setblock ~18 ~0 ~10 minecraft:cracked_stone_bricks
fill ~18 ~0 ~11 ~18 ~0 ~12 minecraft:stone_bricks
setblock ~18 ~0 ~13 minecraft:cracked_stone_bricks
fill ~18 ~0 ~14 ~19 ~0 ~14 minecraft:stone_bricks
setblock ~18 ~0 ~15 minecraft:cobblestone
setblock ~18 ~0 ~16 minecraft:gravel
fill ~18 ~0 ~19 ~18 ~0 ~20 minecraft:packed_mud
setblock ~18 ~0 ~21 minecraft:coarse_dirt
fill ~18 ~0 ~25 ~18 ~0 ~28 minecraft:packed_mud
setblock ~18 ~0 ~29 minecraft:coarse_dirt
setblock ~18 ~0 ~30 minecraft:packed_mud
fill ~18 ~0 ~31 ~18 ~0 ~32 minecraft:andesite
fill ~18 ~0 ~33 ~19 ~0 ~33 minecraft:cobblestone
fill ~18 ~0 ~35 ~18 ~0 ~36 minecraft:andesite
fill ~18 ~0 ~37 ~18 ~0 ~38 minecraft:stone_bricks
setblock ~19 ~0 ~8 minecraft:mossy_stone_bricks
setblock ~19 ~0 ~9 minecraft:cracked_stone_bricks
setblock ~19 ~0 ~10 minecraft:stone_bricks
setblock ~19 ~0 ~11 minecraft:cobblestone
fill ~19 ~0 ~12 ~20 ~0 ~12 minecraft:andesite
fill ~19 ~0 ~13 ~20 ~0 ~13 minecraft:cobblestone
setblock ~19 ~0 ~15 minecraft:coarse_dirt
fill ~19 ~0 ~16 ~20 ~0 ~16 minecraft:packed_mud
fill ~19 ~0 ~19 ~19 ~0 ~21 minecraft:gravel
fill ~19 ~0 ~25 ~19 ~0 ~26 minecraft:coarse_dirt
fill ~19 ~0 ~27 ~20 ~0 ~27 minecraft:packed_mud
fill ~19 ~0 ~28 ~19 ~0 ~29 minecraft:coarse_dirt
fill ~19 ~0 ~30 ~21 ~0 ~30 minecraft:gravel
setblock ~19 ~0 ~31 minecraft:coarse_dirt
setblock ~19 ~0 ~32 minecraft:stone_bricks
setblock ~19 ~0 ~34 minecraft:andesite
fill ~19 ~0 ~35 ~20 ~0 ~35 minecraft:mossy_stone_bricks
setblock ~19 ~0 ~36 minecraft:cracked_stone_bricks
setblock ~19 ~0 ~37 minecraft:mossy_stone_bricks
setblock ~19 ~0 ~38 minecraft:andesite
setblock ~20 ~0 ~8 minecraft:cobblestone
fill ~20 ~0 ~9 ~20 ~0 ~10 minecraft:andesite
setblock ~20 ~0 ~11 minecraft:stone_bricks
setblock ~20 ~0 ~14 minecraft:andesite
fill ~20 ~0 ~15 ~21 ~0 ~15 minecraft:gravel
fill ~20 ~0 ~18 ~20 ~0 ~21 minecraft:packed_mud
fill ~20 ~0 ~25 ~20 ~0 ~26 minecraft:gravel
setblock ~20 ~0 ~28 minecraft:gravel
setblock ~20 ~0 ~29 minecraft:packed_mud
fill ~20 ~0 ~31 ~21 ~0 ~31 minecraft:packed_mud
setblock ~20 ~0 ~32 minecraft:mossy_stone_bricks
setblock ~20 ~0 ~33 minecraft:andesite
setblock ~20 ~0 ~34 minecraft:stone_bricks
setblock ~20 ~0 ~36 minecraft:cobblestone
setblock ~20 ~0 ~37 minecraft:cracked_stone_bricks
setblock ~20 ~0 ~38 minecraft:stone_bricks
fill ~21 ~0 ~8 ~22 ~0 ~8 minecraft:stone_bricks
fill ~21 ~0 ~9 ~21 ~0 ~10 minecraft:stone_bricks
fill ~21 ~0 ~11 ~23 ~0 ~11 minecraft:cobblestone
fill ~21 ~0 ~12 ~23 ~0 ~12 minecraft:stone_bricks
fill ~21 ~0 ~13 ~21 ~0 ~14 minecraft:andesite
setblock ~21 ~0 ~16 minecraft:coarse_dirt
setblock ~21 ~0 ~17 minecraft:packed_mud
fill ~21 ~0 ~18 ~21 ~0 ~19 minecraft:coarse_dirt
setblock ~21 ~0 ~20 minecraft:gravel
setblock ~21 ~0 ~21 minecraft:coarse_dirt
fill ~21 ~0 ~25 ~21 ~0 ~29 minecraft:coarse_dirt
setblock ~21 ~0 ~32 minecraft:andesite
fill ~21 ~0 ~33 ~21 ~0 ~34 minecraft:cobblestone
fill ~21 ~0 ~35 ~21 ~0 ~38 minecraft:stone_bricks
setblock ~22 ~0 ~9 minecraft:andesite
setblock ~22 ~0 ~10 minecraft:cracked_stone_bricks
fill ~22 ~0 ~13 ~22 ~0 ~21 minecraft:stone_bricks
fill ~22 ~0 ~25 ~24 ~0 ~32 minecraft:stone_bricks
setblock ~22 ~0 ~33 minecraft:mossy_stone_bricks
fill ~22 ~0 ~34 ~22 ~0 ~35 minecraft:stone_bricks
setblock ~22 ~0 ~36 minecraft:cracked_stone_bricks
fill ~22 ~0 ~37 ~23 ~0 ~38 minecraft:stone_bricks
setblock ~23 ~0 ~8 minecraft:cobblestone
fill ~23 ~0 ~9 ~24 ~0 ~9 minecraft:stone_bricks
setblock ~23 ~0 ~10 minecraft:stone_bricks
setblock ~23 ~0 ~13 minecraft:andesite
fill ~23 ~0 ~14 ~23 ~0 ~21 minecraft:stone_bricks
setblock ~23 ~0 ~33 minecraft:cobblestone
setblock ~23 ~0 ~34 minecraft:cracked_stone_bricks
fill ~23 ~0 ~35 ~23 ~0 ~36 minecraft:andesite
setblock ~24 ~0 ~8 minecraft:andesite
fill ~24 ~0 ~10 ~24 ~0 ~11 minecraft:mossy_stone_bricks
setblock ~24 ~0 ~12 minecraft:andesite
fill ~24 ~0 ~13 ~24 ~0 ~21 minecraft:stone_bricks
setblock ~24 ~0 ~33 minecraft:stone_bricks
fill ~24 ~0 ~34 ~25 ~0 ~34 minecraft:cobblestone
setblock ~24 ~0 ~35 minecraft:mossy_stone_bricks
fill ~24 ~0 ~36 ~24 ~0 ~38 minecraft:stone_bricks
setblock ~25 ~0 ~8 minecraft:stone_bricks
setblock ~25 ~0 ~9 minecraft:cracked_stone_bricks
fill ~25 ~0 ~10 ~25 ~0 ~12 minecraft:stone_bricks
setblock ~25 ~0 ~13 minecraft:cobblestone
fill ~25 ~0 ~14 ~26 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~25 ~0 ~15 minecraft:packed_mud
setblock ~25 ~0 ~16 minecraft:coarse_dirt
setblock ~25 ~0 ~17 minecraft:packed_mud
fill ~25 ~0 ~18 ~26 ~0 ~18 minecraft:coarse_dirt
fill ~25 ~0 ~19 ~27 ~0 ~19 minecraft:packed_mud
fill ~25 ~0 ~20 ~26 ~0 ~20 minecraft:gravel
setblock ~25 ~0 ~21 minecraft:packed_mud
fill ~25 ~0 ~22 ~33 ~0 ~24 minecraft:stone_bricks
fill ~25 ~0 ~25 ~29 ~0 ~25 minecraft:coarse_dirt
fill ~25 ~0 ~26 ~25 ~0 ~30 minecraft:coarse_dirt
setblock ~25 ~0 ~31 minecraft:gravel
fill ~25 ~0 ~32 ~27 ~0 ~32 minecraft:stone_bricks
setblock ~25 ~0 ~33 minecraft:cracked_stone_bricks
setblock ~25 ~0 ~35 minecraft:stone_bricks
setblock ~25 ~0 ~36 minecraft:mossy_stone_bricks
setblock ~25 ~0 ~37 minecraft:andesite
setblock ~25 ~0 ~38 minecraft:cracked_stone_bricks
setblock ~26 ~0 ~8 minecraft:cracked_stone_bricks
fill ~26 ~0 ~9 ~27 ~0 ~9 minecraft:andesite
setblock ~26 ~0 ~10 minecraft:cracked_stone_bricks
fill ~26 ~0 ~11 ~26 ~0 ~12 minecraft:stone_bricks
fill ~26 ~0 ~13 ~27 ~0 ~13 minecraft:cracked_stone_bricks
fill ~26 ~0 ~15 ~26 ~0 ~16 minecraft:coarse_dirt
setblock ~26 ~0 ~17 minecraft:gravel
setblock ~26 ~0 ~21 minecraft:coarse_dirt
setblock ~26 ~0 ~26 minecraft:packed_mud
fill ~26 ~0 ~27 ~27 ~0 ~27 minecraft:gravel
fill ~26 ~0 ~28 ~26 ~0 ~31 minecraft:coarse_dirt
setblock ~26 ~0 ~33 minecraft:cobblestone
setblock ~26 ~0 ~34 minecraft:stone_bricks
setblock ~26 ~0 ~35 minecraft:cobblestone
setblock ~26 ~0 ~36 minecraft:andesite
fill ~26 ~0 ~37 ~26 ~0 ~38 minecraft:stone_bricks
fill ~27 ~0 ~8 ~28 ~0 ~8 minecraft:andesite
setblock ~27 ~0 ~10 minecraft:stone_bricks
setblock ~27 ~0 ~11 minecraft:cracked_stone_bricks
setblock ~27 ~0 ~12 minecraft:cobblestone
setblock ~27 ~0 ~14 minecraft:mossy_stone_bricks
setblock ~27 ~0 ~15 minecraft:packed_mud
fill ~27 ~0 ~16 ~27 ~0 ~18 minecraft:gravel
setblock ~27 ~0 ~20 minecraft:coarse_dirt
setblock ~27 ~0 ~21 minecraft:packed_mud
fill ~27 ~0 ~26 ~28 ~0 ~26 minecraft:gravel
fill ~27 ~0 ~28 ~28 ~0 ~28 minecraft:packed_mud
fill ~27 ~0 ~29 ~27 ~0 ~30 minecraft:coarse_dirt
setblock ~27 ~0 ~31 minecraft:gravel
setblock ~27 ~0 ~33 minecraft:andesite
setblock ~27 ~0 ~34 minecraft:mossy_stone_bricks
setblock ~27 ~0 ~35 minecraft:cracked_stone_bricks
fill ~27 ~0 ~36 ~27 ~0 ~37 minecraft:stone_bricks
setblock ~27 ~0 ~38 minecraft:andesite
fill ~28 ~0 ~9 ~28 ~0 ~11 minecraft:stone_bricks
setblock ~28 ~0 ~12 minecraft:andesite
setblock ~28 ~0 ~13 minecraft:cobblestone
setblock ~28 ~0 ~14 minecraft:stone_bricks
setblock ~28 ~0 ~15 minecraft:andesite
setblock ~28 ~0 ~16 minecraft:coarse_dirt
setblock ~28 ~0 ~17 minecraft:packed_mud
setblock ~28 ~0 ~18 minecraft:gravel
setblock ~28 ~0 ~19 minecraft:coarse_dirt
fill ~28 ~0 ~20 ~28 ~0 ~21 minecraft:packed_mud
fill ~28 ~0 ~27 ~29 ~0 ~27 minecraft:packed_mud
setblock ~28 ~0 ~29 minecraft:packed_mud
setblock ~28 ~0 ~30 minecraft:gravel
setblock ~28 ~0 ~31 minecraft:mossy_stone_bricks
setblock ~28 ~0 ~32 minecraft:cracked_stone_bricks
setblock ~28 ~0 ~33 minecraft:stone_bricks
setblock ~28 ~0 ~34 minecraft:cracked_stone_bricks
setblock ~28 ~0 ~35 minecraft:stone_bricks
setblock ~28 ~0 ~36 minecraft:cracked_stone_bricks
setblock ~28 ~0 ~37 minecraft:stone_bricks
setblock ~28 ~0 ~38 minecraft:cracked_stone_bricks
setblock ~29 ~0 ~9 minecraft:cracked_stone_bricks
fill ~29 ~0 ~10 ~30 ~0 ~10 minecraft:stone_bricks
setblock ~29 ~0 ~11 minecraft:cracked_stone_bricks
fill ~29 ~0 ~12 ~29 ~0 ~14 minecraft:stone_bricks
setblock ~29 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~29 ~0 ~16 minecraft:stone_bricks
fill ~29 ~0 ~17 ~29 ~0 ~18 minecraft:coarse_dirt
setblock ~29 ~0 ~19 minecraft:packed_mud
setblock ~29 ~0 ~20 minecraft:gravel
setblock ~29 ~0 ~21 minecraft:packed_mud
setblock ~29 ~0 ~26 minecraft:coarse_dirt
setblock ~29 ~0 ~28 minecraft:coarse_dirt
setblock ~29 ~0 ~29 minecraft:gravel
fill ~29 ~0 ~30 ~30 ~0 ~30 minecraft:andesite
fill ~29 ~0 ~31 ~30 ~0 ~31 minecraft:stone_bricks
setblock ~29 ~0 ~32 minecraft:stone_bricks
setblock ~29 ~0 ~33 minecraft:andesite
fill ~29 ~0 ~34 ~30 ~0 ~34 minecraft:stone_bricks
setblock ~29 ~0 ~35 minecraft:andesite
setblock ~29 ~0 ~36 minecraft:stone_bricks
setblock ~29 ~0 ~37 minecraft:cobblestone
setblock ~30 ~0 ~9 minecraft:andesite
setblock ~30 ~0 ~11 minecraft:stone_bricks
fill ~30 ~0 ~12 ~31 ~0 ~12 minecraft:cobblestone
setblock ~30 ~0 ~13 minecraft:andesite
setblock ~30 ~0 ~14 minecraft:stone_bricks
setblock ~30 ~0 ~15 minecraft:andesite
setblock ~30 ~0 ~16 minecraft:cracked_stone_bricks
fill ~30 ~0 ~17 ~31 ~0 ~17 minecraft:andesite
setblock ~30 ~0 ~18 minecraft:gravel
fill ~30 ~0 ~19 ~31 ~0 ~19 minecraft:coarse_dirt
fill ~30 ~0 ~20 ~30 ~0 ~21 minecraft:packed_mud
fill ~30 ~0 ~25 ~30 ~0 ~26 minecraft:packed_mud
fill ~30 ~0 ~27 ~31 ~0 ~27 minecraft:coarse_dirt
setblock ~30 ~0 ~28 minecraft:packed_mud
setblock ~30 ~0 ~29 minecraft:cobblestone
setblock ~30 ~0 ~32 minecraft:andesite
setblock ~30 ~0 ~33 minecraft:cobblestone
setblock ~30 ~0 ~35 minecraft:cobblestone
fill ~30 ~0 ~36 ~32 ~0 ~36 minecraft:andesite
setblock ~30 ~0 ~37 minecraft:cracked_stone_bricks
setblock ~31 ~0 ~10 minecraft:cracked_stone_bricks
setblock ~31 ~0 ~11 minecraft:andesite
fill ~31 ~0 ~13 ~31 ~0 ~14 minecraft:stone_bricks
fill ~31 ~0 ~15 ~32 ~0 ~15 minecraft:mossy_stone_bricks
setblock ~31 ~0 ~16 minecraft:mossy_stone_bricks
setblock ~31 ~0 ~18 minecraft:stone_bricks
setblock ~31 ~0 ~20 minecraft:coarse_dirt
setblock ~31 ~0 ~21 minecraft:packed_mud
setblock ~31 ~0 ~25 minecraft:coarse_dirt
setblock ~31 ~0 ~26 minecraft:packed_mud
setblock ~31 ~0 ~28 minecraft:andesite
setblock ~31 ~0 ~29 minecraft:mossy_stone_bricks
fill ~31 ~0 ~30 ~31 ~0 ~34 minecraft:stone_bricks
setblock ~31 ~0 ~35 minecraft:mossy_stone_bricks
setblock ~32 ~0 ~10 minecraft:mossy_stone_bricks
setblock ~32 ~0 ~11 minecraft:stone_bricks
setblock ~32 ~0 ~12 minecraft:andesite
setblock ~32 ~0 ~13 minecraft:mossy_stone_bricks
setblock ~32 ~0 ~14 minecraft:stone_bricks
fill ~32 ~0 ~16 ~32 ~0 ~19 minecraft:stone_bricks
setblock ~32 ~0 ~20 minecraft:cobblestone
fill ~32 ~0 ~21 ~33 ~0 ~21 minecraft:andesite
setblock ~32 ~0 ~25 minecraft:andesite
setblock ~32 ~0 ~26 minecraft:stone_bricks
setblock ~32 ~0 ~27 minecraft:cobblestone
fill ~32 ~0 ~28 ~32 ~0 ~29 minecraft:stone_bricks
setblock ~32 ~0 ~30 minecraft:andesite
fill ~32 ~0 ~31 ~32 ~0 ~32 minecraft:cobblestone
fill ~32 ~0 ~33 ~34 ~0 ~33 minecraft:stone_bricks
setblock ~32 ~0 ~34 minecraft:stone_bricks
setblock ~32 ~0 ~35 minecraft:cracked_stone_bricks
setblock ~33 ~0 ~11 minecraft:cracked_stone_bricks
setblock ~33 ~0 ~12 minecraft:stone_bricks
setblock ~33 ~0 ~13 minecraft:andesite
setblock ~33 ~0 ~14 minecraft:cobblestone
setblock ~33 ~0 ~15 minecraft:andesite
setblock ~33 ~0 ~16 minecraft:cracked_stone_bricks
setblock ~33 ~0 ~17 minecraft:stone_bricks
setblock ~33 ~0 ~18 minecraft:mossy_stone_bricks
setblock ~33 ~0 ~19 minecraft:andesite
setblock ~33 ~0 ~20 minecraft:mossy_stone_bricks
fill ~33 ~0 ~25 ~33 ~0 ~28 minecraft:stone_bricks
fill ~33 ~0 ~29 ~33 ~0 ~30 minecraft:cracked_stone_bricks
fill ~33 ~0 ~31 ~34 ~0 ~32 minecraft:andesite
setblock ~33 ~0 ~34 minecraft:cobblestone
setblock ~33 ~0 ~35 minecraft:andesite
setblock ~34 ~0 ~12 minecraft:mossy_stone_bricks
setblock ~34 ~0 ~13 minecraft:cobblestone
setblock ~34 ~0 ~14 minecraft:stone_bricks
setblock ~34 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~34 ~0 ~16 minecraft:stone_bricks
setblock ~34 ~0 ~17 minecraft:mossy_stone_bricks
fill ~34 ~0 ~18 ~34 ~0 ~20 minecraft:stone_bricks
setblock ~34 ~0 ~21 minecraft:cobblestone
fill ~34 ~0 ~22 ~35 ~0 ~22 minecraft:mossy_stone_bricks
setblock ~34 ~0 ~23 minecraft:cobblestone
setblock ~34 ~0 ~24 minecraft:cracked_stone_bricks
fill ~34 ~0 ~25 ~36 ~0 ~25 minecraft:cobblestone
setblock ~34 ~0 ~26 minecraft:cobblestone
fill ~34 ~0 ~27 ~35 ~0 ~27 minecraft:stone_bricks
fill ~34 ~0 ~28 ~34 ~0 ~29 minecraft:stone_bricks
setblock ~34 ~0 ~30 minecraft:cracked_stone_bricks
setblock ~34 ~0 ~34 minecraft:stone_bricks
setblock ~35 ~0 ~13 minecraft:mossy_stone_bricks
setblock ~35 ~0 ~14 minecraft:cracked_stone_bricks
fill ~35 ~0 ~15 ~35 ~0 ~16 minecraft:stone_bricks
fill ~35 ~0 ~17 ~35 ~0 ~18 minecraft:cobblestone
fill ~35 ~0 ~19 ~38 ~0 ~19 minecraft:stone_bricks
fill ~35 ~0 ~20 ~36 ~0 ~20 minecraft:stone_bricks
setblock ~35 ~0 ~21 minecraft:cracked_stone_bricks
setblock ~35 ~0 ~23 minecraft:mossy_stone_bricks
setblock ~35 ~0 ~24 minecraft:andesite
setblock ~35 ~0 ~26 minecraft:andesite
setblock ~35 ~0 ~28 minecraft:andesite
setblock ~35 ~0 ~29 minecraft:cracked_stone_bricks
setblock ~35 ~0 ~30 minecraft:cobblestone
setblock ~35 ~0 ~31 minecraft:cracked_stone_bricks
fill ~35 ~0 ~32 ~35 ~0 ~33 minecraft:stone_bricks
setblock ~36 ~0 ~14 minecraft:mossy_stone_bricks
setblock ~36 ~0 ~15 minecraft:cobblestone
setblock ~36 ~0 ~16 minecraft:mossy_stone_bricks
setblock ~36 ~0 ~17 minecraft:andesite
setblock ~36 ~0 ~18 minecraft:cobblestone
setblock ~36 ~0 ~21 minecraft:cobblestone
fill ~36 ~0 ~22 ~36 ~0 ~24 minecraft:stone_bricks
fill ~36 ~0 ~26 ~37 ~0 ~29 minecraft:stone_bricks
fill ~36 ~0 ~30 ~37 ~0 ~30 minecraft:andesite
setblock ~36 ~0 ~31 minecraft:mossy_stone_bricks
setblock ~36 ~0 ~32 minecraft:cracked_stone_bricks
setblock ~37 ~0 ~16 minecraft:cracked_stone_bricks
setblock ~37 ~0 ~17 minecraft:mossy_stone_bricks
fill ~37 ~0 ~18 ~38 ~0 ~18 minecraft:andesite
setblock ~37 ~0 ~20 minecraft:cracked_stone_bricks
fill ~37 ~0 ~21 ~37 ~0 ~24 minecraft:stone_bricks
setblock ~37 ~0 ~25 minecraft:andesite
fill ~38 ~0 ~20 ~38 ~0 ~22 minecraft:stone_bricks
setblock ~38 ~0 ~23 minecraft:mossy_stone_bricks
setblock ~38 ~0 ~24 minecraft:stone_bricks
setblock ~38 ~0 ~25 minecraft:cobblestone
setblock ~38 ~0 ~26 minecraft:andesite
setblock ~38 ~0 ~27 minecraft:stone_bricks
setblock ~38 ~0 ~28 minecraft:andesite

# the ground outside radius 17.5: packed mud and gravel trodden by the garrison, inside the stockade and round it to the footprint edge (grass left at the far corners)
fill ~0 ~0 ~9 ~0 ~0 ~11 minecraft:packed_mud
setblock ~0 ~0 ~12 minecraft:coarse_dirt
setblock ~0 ~0 ~13 minecraft:packed_mud
setblock ~0 ~0 ~14 minecraft:gravel
fill ~0 ~0 ~15 ~0 ~0 ~16 minecraft:packed_mud
fill ~0 ~0 ~17 ~0 ~0 ~18 minecraft:coarse_dirt
setblock ~0 ~0 ~19 minecraft:packed_mud
fill ~0 ~0 ~20 ~1 ~0 ~20 minecraft:gravel
setblock ~0 ~0 ~21 minecraft:packed_mud
fill ~0 ~0 ~22 ~1 ~0 ~22 minecraft:gravel
fill ~0 ~0 ~23 ~2 ~0 ~23 minecraft:coarse_dirt
fill ~0 ~0 ~24 ~0 ~0 ~25 minecraft:packed_mud
setblock ~0 ~0 ~26 minecraft:coarse_dirt
setblock ~0 ~0 ~27 minecraft:rooted_dirt
fill ~0 ~0 ~28 ~1 ~0 ~28 minecraft:packed_mud
setblock ~0 ~0 ~29 minecraft:coarse_dirt
setblock ~0 ~0 ~30 minecraft:gravel
setblock ~0 ~0 ~31 minecraft:packed_mud
setblock ~0 ~0 ~32 minecraft:coarse_dirt
fill ~0 ~0 ~33 ~0 ~0 ~36 minecraft:packed_mud
setblock ~0 ~0 ~37 minecraft:gravel
setblock ~0 ~0 ~38 minecraft:packed_mud
setblock ~0 ~0 ~39 minecraft:rooted_dirt
fill ~0 ~0 ~40 ~0 ~0 ~41 minecraft:packed_mud
setblock ~1 ~0 ~8 minecraft:rooted_dirt
setblock ~1 ~0 ~9 minecraft:gravel
fill ~1 ~0 ~10 ~2 ~0 ~10 minecraft:packed_mud
setblock ~1 ~0 ~11 minecraft:gravel
fill ~1 ~0 ~12 ~4 ~0 ~12 minecraft:packed_mud
setblock ~1 ~0 ~13 minecraft:rooted_dirt
setblock ~1 ~0 ~14 minecraft:packed_mud
fill ~1 ~0 ~15 ~1 ~0 ~16 minecraft:gravel
fill ~1 ~0 ~17 ~1 ~0 ~19 minecraft:packed_mud
fill ~1 ~0 ~21 ~2 ~0 ~21 minecraft:coarse_dirt
fill ~1 ~0 ~24 ~3 ~0 ~24 minecraft:coarse_dirt
setblock ~1 ~0 ~25 minecraft:coarse_dirt
setblock ~1 ~0 ~26 minecraft:packed_mud
setblock ~1 ~0 ~27 minecraft:gravel
setblock ~1 ~0 ~29 minecraft:packed_mud
setblock ~1 ~0 ~30 minecraft:rooted_dirt
setblock ~1 ~0 ~31 minecraft:gravel
fill ~1 ~0 ~32 ~1 ~0 ~33 minecraft:rooted_dirt
setblock ~1 ~0 ~34 minecraft:gravel
fill ~1 ~0 ~35 ~1 ~0 ~37 minecraft:packed_mud
setblock ~1 ~0 ~38 minecraft:coarse_dirt
fill ~1 ~0 ~39 ~2 ~0 ~39 minecraft:gravel
fill ~1 ~0 ~40 ~1 ~0 ~41 minecraft:gravel
setblock ~2 ~0 ~4 minecraft:coarse_dirt
setblock ~2 ~0 ~5 minecraft:rooted_dirt
setblock ~2 ~0 ~6 minecraft:gravel
setblock ~2 ~0 ~7 minecraft:packed_mud
fill ~2 ~0 ~8 ~2 ~0 ~9 minecraft:coarse_dirt
fill ~2 ~0 ~11 ~5 ~0 ~11 minecraft:coarse_dirt
fill ~2 ~0 ~13 ~2 ~0 ~14 minecraft:coarse_dirt
fill ~2 ~0 ~15 ~5 ~0 ~15 minecraft:packed_mud
setblock ~2 ~0 ~16 minecraft:packed_mud
setblock ~2 ~0 ~17 minecraft:gravel
setblock ~2 ~0 ~18 minecraft:packed_mud
fill ~2 ~0 ~19 ~3 ~0 ~19 minecraft:gravel
setblock ~2 ~0 ~20 minecraft:packed_mud
fill ~2 ~0 ~22 ~4 ~0 ~22 minecraft:coarse_dirt
fill ~2 ~0 ~25 ~2 ~0 ~29 minecraft:gravel
fill ~2 ~0 ~30 ~2 ~0 ~34 minecraft:packed_mud
fill ~2 ~0 ~35 ~4 ~0 ~35 minecraft:gravel
setblock ~2 ~0 ~36 minecraft:coarse_dirt
setblock ~2 ~0 ~37 minecraft:gravel
setblock ~2 ~0 ~38 minecraft:packed_mud
setblock ~2 ~0 ~40 minecraft:packed_mud
fill ~3 ~0 ~1 ~4 ~0 ~1 minecraft:coarse_dirt
fill ~3 ~0 ~2 ~4 ~0 ~2 minecraft:packed_mud
setblock ~3 ~0 ~3 minecraft:gravel
fill ~3 ~0 ~4 ~3 ~0 ~5 minecraft:packed_mud
fill ~3 ~0 ~6 ~3 ~0 ~8 minecraft:coarse_dirt
setblock ~3 ~0 ~9 minecraft:rooted_dirt
setblock ~3 ~0 ~10 minecraft:gravel
fill ~3 ~0 ~13 ~5 ~0 ~14 minecraft:gravel
fill ~3 ~0 ~16 ~4 ~0 ~16 minecraft:gravel
fill ~3 ~0 ~17 ~4 ~0 ~17 minecraft:packed_mud
setblock ~3 ~0 ~18 minecraft:coarse_dirt
setblock ~3 ~0 ~20 minecraft:coarse_dirt
fill ~3 ~0 ~21 ~4 ~0 ~21 minecraft:gravel
fill ~3 ~0 ~23 ~5 ~0 ~23 minecraft:packed_mud
setblock ~3 ~0 ~25 minecraft:packed_mud
setblock ~3 ~0 ~26 minecraft:gravel
fill ~3 ~0 ~27 ~4 ~0 ~27 minecraft:packed_mud
setblock ~3 ~0 ~28 minecraft:gravel
fill ~3 ~0 ~29 ~5 ~0 ~29 minecraft:packed_mud
fill ~3 ~0 ~30 ~3 ~0 ~31 minecraft:gravel
setblock ~3 ~0 ~32 minecraft:packed_mud
setblock ~3 ~0 ~33 minecraft:gravel
setblock ~3 ~0 ~34 minecraft:packed_mud
fill ~3 ~0 ~36 ~3 ~0 ~39 minecraft:packed_mud
fill ~3 ~0 ~40 ~4 ~0 ~40 minecraft:gravel
fill ~3 ~0 ~41 ~4 ~0 ~41 minecraft:packed_mud
fill ~3 ~0 ~46 ~4 ~0 ~46 minecraft:gravel
setblock ~4 ~0 ~0 minecraft:gravel
setblock ~4 ~0 ~3 minecraft:packed_mud
fill ~4 ~0 ~4 ~4 ~0 ~5 minecraft:coarse_dirt
setblock ~4 ~0 ~6 minecraft:packed_mud
setblock ~4 ~0 ~7 minecraft:gravel
fill ~4 ~0 ~8 ~4 ~0 ~10 minecraft:packed_mud
setblock ~4 ~0 ~18 minecraft:packed_mud
fill ~4 ~0 ~19 ~4 ~0 ~20 minecraft:coarse_dirt
fill ~4 ~0 ~24 ~5 ~0 ~24 minecraft:packed_mud
fill ~4 ~0 ~25 ~5 ~0 ~25 minecraft:gravel
fill ~4 ~0 ~26 ~5 ~0 ~26 minecraft:coarse_dirt
setblock ~4 ~0 ~28 minecraft:rooted_dirt
setblock ~4 ~0 ~30 minecraft:coarse_dirt
setblock ~4 ~0 ~31 minecraft:packed_mud
setblock ~4 ~0 ~32 minecraft:coarse_dirt
fill ~4 ~0 ~33 ~4 ~0 ~34 minecraft:packed_mud
fill ~4 ~0 ~36 ~4 ~0 ~38 minecraft:coarse_dirt
setblock ~4 ~0 ~39 minecraft:packed_mud
fill ~4 ~0 ~42 ~5 ~0 ~42 minecraft:packed_mud
setblock ~4 ~0 ~45 minecraft:packed_mud
fill ~5 ~0 ~1 ~5 ~0 ~5 minecraft:packed_mud
setblock ~5 ~0 ~6 minecraft:coarse_dirt
fill ~5 ~0 ~7 ~5 ~0 ~8 minecraft:packed_mud
setblock ~5 ~0 ~9 minecraft:gravel
fill ~5 ~0 ~10 ~6 ~0 ~10 minecraft:coarse_dirt
fill ~5 ~0 ~12 ~6 ~0 ~12 minecraft:gravel
setblock ~5 ~0 ~16 minecraft:packed_mud
setblock ~5 ~0 ~17 minecraft:coarse_dirt
fill ~5 ~0 ~18 ~5 ~0 ~20 minecraft:gravel
fill ~5 ~0 ~21 ~6 ~0 ~22 minecraft:coarse_dirt
setblock ~5 ~0 ~27 minecraft:gravel
setblock ~5 ~0 ~28 minecraft:coarse_dirt
setblock ~5 ~0 ~30 minecraft:gravel
setblock ~5 ~0 ~31 minecraft:coarse_dirt
setblock ~5 ~0 ~32 minecraft:packed_mud
setblock ~5 ~0 ~33 minecraft:gravel
fill ~5 ~0 ~34 ~6 ~0 ~34 minecraft:coarse_dirt
setblock ~5 ~0 ~35 minecraft:packed_mud
fill ~5 ~0 ~36 ~6 ~0 ~36 minecraft:gravel
setblock ~5 ~0 ~37 minecraft:gravel
setblock ~5 ~0 ~38 minecraft:packed_mud
fill ~5 ~0 ~39 ~6 ~0 ~39 minecraft:gravel
fill ~5 ~0 ~40 ~6 ~0 ~40 minecraft:coarse_dirt
setblock ~5 ~0 ~41 minecraft:gravel
fill ~5 ~0 ~43 ~5 ~0 ~44 minecraft:packed_mud
setblock ~5 ~0 ~45 minecraft:gravel
setblock ~5 ~0 ~46 minecraft:rooted_dirt
fill ~6 ~0 ~3 ~7 ~0 ~3 minecraft:gravel
fill ~6 ~0 ~4 ~7 ~0 ~4 minecraft:coarse_dirt
fill ~6 ~0 ~5 ~6 ~0 ~7 minecraft:packed_mud
setblock ~6 ~0 ~8 minecraft:gravel
fill ~6 ~0 ~9 ~7 ~0 ~9 minecraft:packed_mud
setblock ~6 ~0 ~11 minecraft:rooted_dirt
setblock ~6 ~0 ~13 minecraft:packed_mud
setblock ~6 ~0 ~14 minecraft:gravel
fill ~6 ~0 ~15 ~6 ~0 ~17 minecraft:coarse_dirt
setblock ~6 ~0 ~18 minecraft:packed_mud
setblock ~6 ~0 ~19 minecraft:gravel
setblock ~6 ~0 ~20 minecraft:packed_mud
setblock ~6 ~0 ~24 minecraft:gravel
setblock ~6 ~0 ~25 minecraft:packed_mud
fill ~6 ~0 ~26 ~6 ~0 ~27 minecraft:gravel
setblock ~6 ~0 ~28 minecraft:packed_mud
fill ~6 ~0 ~29 ~6 ~0 ~30 minecraft:coarse_dirt
fill ~6 ~0 ~31 ~6 ~0 ~33 minecraft:packed_mud
setblock ~6 ~0 ~35 minecraft:coarse_dirt
fill ~6 ~0 ~37 ~6 ~0 ~38 minecraft:packed_mud
setblock ~6 ~0 ~41 minecraft:packed_mud
fill ~6 ~0 ~42 ~6 ~0 ~43 minecraft:coarse_dirt
fill ~6 ~0 ~44 ~7 ~0 ~44 minecraft:packed_mud
fill ~6 ~0 ~45 ~9 ~0 ~45 minecraft:coarse_dirt
fill ~6 ~0 ~46 ~7 ~0 ~46 minecraft:coarse_dirt
setblock ~7 ~0 ~2 minecraft:packed_mud
setblock ~7 ~0 ~5 minecraft:gravel
setblock ~7 ~0 ~6 minecraft:rooted_dirt
setblock ~7 ~0 ~7 minecraft:packed_mud
setblock ~7 ~0 ~8 minecraft:coarse_dirt
fill ~7 ~0 ~10 ~7 ~0 ~11 minecraft:gravel
fill ~7 ~0 ~12 ~8 ~0 ~12 minecraft:packed_mud
setblock ~7 ~0 ~13 minecraft:coarse_dirt
fill ~7 ~0 ~14 ~7 ~0 ~15 minecraft:packed_mud
fill ~7 ~0 ~16 ~7 ~0 ~17 minecraft:gravel
setblock ~7 ~0 ~29 minecraft:packed_mud
setblock ~7 ~0 ~30 minecraft:coarse_dirt
setblock ~7 ~0 ~31 minecraft:gravel
fill ~7 ~0 ~32 ~8 ~0 ~33 minecraft:packed_mud
setblock ~7 ~0 ~34 minecraft:packed_mud
fill ~7 ~0 ~35 ~7 ~0 ~36 minecraft:gravel
setblock ~7 ~0 ~37 minecraft:coarse_dirt
fill ~7 ~0 ~38 ~7 ~0 ~40 minecraft:packed_mud
setblock ~7 ~0 ~41 minecraft:rooted_dirt
setblock ~7 ~0 ~42 minecraft:packed_mud
setblock ~7 ~0 ~43 minecraft:gravel
setblock ~8 ~0 ~1 minecraft:gravel
setblock ~8 ~0 ~2 minecraft:rooted_dirt
setblock ~8 ~0 ~3 minecraft:coarse_dirt
fill ~8 ~0 ~4 ~9 ~0 ~4 minecraft:gravel
setblock ~8 ~0 ~5 minecraft:coarse_dirt
setblock ~8 ~0 ~6 minecraft:packed_mud
setblock ~8 ~0 ~7 minecraft:coarse_dirt
fill ~8 ~0 ~8 ~8 ~0 ~9 minecraft:gravel
fill ~8 ~0 ~10 ~9 ~0 ~10 minecraft:packed_mud
setblock ~8 ~0 ~11 minecraft:coarse_dirt
fill ~8 ~0 ~13 ~9 ~0 ~13 minecraft:gravel
setblock ~8 ~0 ~14 minecraft:gravel
setblock ~8 ~0 ~34 minecraft:coarse_dirt
setblock ~8 ~0 ~35 minecraft:rooted_dirt
setblock ~8 ~0 ~36 minecraft:coarse_dirt
setblock ~8 ~0 ~37 minecraft:gravel
setblock ~8 ~0 ~38 minecraft:coarse_dirt
setblock ~8 ~0 ~39 minecraft:packed_mud
setblock ~8 ~0 ~40 minecraft:gravel
setblock ~8 ~0 ~41 minecraft:coarse_dirt
setblock ~8 ~0 ~42 minecraft:gravel
fill ~8 ~0 ~43 ~8 ~0 ~44 minecraft:packed_mud
setblock ~8 ~0 ~46 minecraft:gravel
setblock ~9 ~0 ~0 minecraft:rooted_dirt
setblock ~9 ~0 ~1 minecraft:packed_mud
fill ~9 ~0 ~2 ~10 ~0 ~2 minecraft:gravel
fill ~9 ~0 ~3 ~10 ~0 ~3 minecraft:packed_mud
setblock ~9 ~0 ~5 minecraft:packed_mud
setblock ~9 ~0 ~6 minecraft:gravel
fill ~9 ~0 ~7 ~9 ~0 ~8 minecraft:packed_mud
setblock ~9 ~0 ~9 minecraft:coarse_dirt
fill ~9 ~0 ~11 ~10 ~0 ~11 minecraft:packed_mud
fill ~9 ~0 ~12 ~10 ~0 ~12 minecraft:coarse_dirt
setblock ~9 ~0 ~33 minecraft:coarse_dirt
fill ~9 ~0 ~34 ~10 ~0 ~34 minecraft:gravel
setblock ~9 ~0 ~35 minecraft:coarse_dirt
fill ~9 ~0 ~36 ~10 ~0 ~36 minecraft:gravel
fill ~9 ~0 ~37 ~10 ~0 ~38 minecraft:packed_mud
setblock ~9 ~0 ~39 minecraft:coarse_dirt
setblock ~9 ~0 ~40 minecraft:packed_mud
fill ~9 ~0 ~41 ~10 ~0 ~41 minecraft:gravel
setblock ~9 ~0 ~42 minecraft:rooted_dirt
setblock ~9 ~0 ~43 minecraft:gravel
fill ~9 ~0 ~44 ~10 ~0 ~44 minecraft:packed_mud
fill ~9 ~0 ~46 ~10 ~0 ~46 minecraft:packed_mud
setblock ~10 ~0 ~0 minecraft:gravel
setblock ~10 ~0 ~1 minecraft:coarse_dirt
setblock ~10 ~0 ~4 minecraft:packed_mud
setblock ~10 ~0 ~5 minecraft:rooted_dirt
fill ~10 ~0 ~6 ~11 ~0 ~6 minecraft:packed_mud
setblock ~10 ~0 ~7 minecraft:gravel
setblock ~10 ~0 ~8 minecraft:packed_mud
setblock ~10 ~0 ~9 minecraft:gravel
setblock ~10 ~0 ~10 minecraft:rooted_dirt
setblock ~10 ~0 ~35 minecraft:rooted_dirt
setblock ~10 ~0 ~39 minecraft:packed_mud
setblock ~10 ~0 ~40 minecraft:rooted_dirt
fill ~10 ~0 ~42 ~10 ~0 ~43 minecraft:gravel
fill ~10 ~0 ~45 ~11 ~0 ~45 minecraft:packed_mud
setblock ~11 ~0 ~0 minecraft:coarse_dirt
fill ~11 ~0 ~1 ~11 ~0 ~2 minecraft:packed_mud
setblock ~11 ~0 ~3 minecraft:rooted_dirt
fill ~11 ~0 ~4 ~11 ~0 ~5 minecraft:coarse_dirt
setblock ~11 ~0 ~7 minecraft:packed_mud
fill ~11 ~0 ~8 ~11 ~0 ~9 minecraft:gravel
setblock ~11 ~0 ~10 minecraft:packed_mud
fill ~11 ~0 ~36 ~12 ~0 ~36 minecraft:packed_mud
setblock ~11 ~0 ~37 minecraft:coarse_dirt
setblock ~11 ~0 ~38 minecraft:rooted_dirt
setblock ~11 ~0 ~39 minecraft:gravel
fill ~11 ~0 ~40 ~11 ~0 ~42 minecraft:packed_mud
setblock ~11 ~0 ~43 minecraft:gravel
setblock ~11 ~0 ~44 minecraft:coarse_dirt
setblock ~11 ~0 ~46 minecraft:gravel
setblock ~12 ~0 ~0 minecraft:packed_mud
setblock ~12 ~0 ~1 minecraft:gravel
fill ~12 ~0 ~2 ~12 ~0 ~4 minecraft:packed_mud
setblock ~12 ~0 ~5 minecraft:gravel
fill ~12 ~0 ~6 ~13 ~0 ~6 minecraft:coarse_dirt
fill ~12 ~0 ~7 ~13 ~0 ~7 minecraft:gravel
fill ~12 ~0 ~8 ~13 ~0 ~8 minecraft:coarse_dirt
setblock ~12 ~0 ~9 minecraft:coarse_dirt
setblock ~12 ~0 ~10 minecraft:gravel
fill ~12 ~0 ~37 ~13 ~0 ~37 minecraft:packed_mud
setblock ~12 ~0 ~38 minecraft:gravel
setblock ~12 ~0 ~39 minecraft:packed_mud
fill ~12 ~0 ~40 ~12 ~0 ~41 minecraft:coarse_dirt
setblock ~12 ~0 ~42 minecraft:rooted_dirt
setblock ~12 ~0 ~43 minecraft:coarse_dirt
setblock ~12 ~0 ~44 minecraft:packed_mud
setblock ~12 ~0 ~45 minecraft:coarse_dirt
fill ~12 ~0 ~46 ~17 ~0 ~46 minecraft:packed_mud
setblock ~13 ~0 ~0 minecraft:gravel
setblock ~13 ~0 ~1 minecraft:packed_mud
fill ~13 ~0 ~2 ~14 ~0 ~2 minecraft:coarse_dirt
setblock ~13 ~0 ~3 minecraft:gravel
setblock ~13 ~0 ~4 minecraft:packed_mud
setblock ~13 ~0 ~5 minecraft:rooted_dirt
setblock ~13 ~0 ~9 minecraft:packed_mud
fill ~13 ~0 ~38 ~13 ~0 ~42 minecraft:packed_mud
fill ~13 ~0 ~43 ~13 ~0 ~44 minecraft:gravel
setblock ~13 ~0 ~45 minecraft:rooted_dirt
fill ~14 ~0 ~0 ~15 ~0 ~0 minecraft:coarse_dirt
setblock ~14 ~0 ~1 minecraft:gravel
fill ~14 ~0 ~3 ~15 ~0 ~3 minecraft:packed_mud
fill ~14 ~0 ~4 ~14 ~0 ~5 minecraft:coarse_dirt
setblock ~14 ~0 ~6 minecraft:packed_mud
setblock ~14 ~0 ~7 minecraft:rooted_dirt
setblock ~14 ~0 ~8 minecraft:gravel
setblock ~14 ~0 ~38 minecraft:coarse_dirt
fill ~14 ~0 ~39 ~15 ~0 ~40 minecraft:packed_mud
setblock ~14 ~0 ~41 minecraft:coarse_dirt
fill ~14 ~0 ~42 ~14 ~0 ~43 minecraft:rooted_dirt
fill ~14 ~0 ~44 ~14 ~0 ~45 minecraft:coarse_dirt
fill ~15 ~0 ~1 ~16 ~0 ~1 minecraft:packed_mud
setblock ~15 ~0 ~2 minecraft:rooted_dirt
fill ~15 ~0 ~4 ~15 ~0 ~5 minecraft:packed_mud
setblock ~15 ~0 ~6 minecraft:gravel
setblock ~15 ~0 ~7 minecraft:coarse_dirt
fill ~15 ~0 ~41 ~17 ~0 ~41 minecraft:gravel
fill ~15 ~0 ~42 ~15 ~0 ~43 minecraft:gravel
setblock ~15 ~0 ~44 minecraft:packed_mud
setblock ~15 ~0 ~45 minecraft:gravel
setblock ~16 ~0 ~0 minecraft:rooted_dirt
setblock ~16 ~0 ~2 minecraft:gravel
setblock ~16 ~0 ~3 minecraft:rooted_dirt
setblock ~16 ~0 ~4 minecraft:coarse_dirt
setblock ~16 ~0 ~5 minecraft:rooted_dirt
setblock ~16 ~0 ~6 minecraft:coarse_dirt
setblock ~16 ~0 ~7 minecraft:packed_mud
fill ~16 ~0 ~39 ~17 ~0 ~39 minecraft:gravel
fill ~16 ~0 ~40 ~17 ~0 ~40 minecraft:packed_mud
fill ~16 ~0 ~42 ~17 ~0 ~42 minecraft:packed_mud
setblock ~16 ~0 ~43 minecraft:packed_mud
setblock ~16 ~0 ~44 minecraft:gravel
setblock ~16 ~0 ~45 minecraft:coarse_dirt
setblock ~17 ~0 ~0 minecraft:packed_mud
fill ~17 ~0 ~1 ~18 ~0 ~1 minecraft:gravel
fill ~17 ~0 ~2 ~18 ~0 ~2 minecraft:packed_mud
setblock ~17 ~0 ~3 minecraft:gravel
fill ~17 ~0 ~4 ~18 ~0 ~4 minecraft:packed_mud
fill ~17 ~0 ~5 ~17 ~0 ~7 minecraft:gravel
setblock ~17 ~0 ~43 minecraft:rooted_dirt
fill ~17 ~0 ~44 ~17 ~0 ~45 minecraft:coarse_dirt
setblock ~18 ~0 ~0 minecraft:rooted_dirt
setblock ~18 ~0 ~3 minecraft:rooted_dirt
fill ~18 ~0 ~5 ~19 ~0 ~5 minecraft:packed_mud
setblock ~18 ~0 ~6 minecraft:packed_mud
fill ~18 ~0 ~40 ~19 ~0 ~40 minecraft:coarse_dirt
fill ~18 ~0 ~41 ~19 ~0 ~41 minecraft:packed_mud
setblock ~18 ~0 ~42 minecraft:gravel
fill ~18 ~0 ~43 ~19 ~0 ~43 minecraft:coarse_dirt
fill ~18 ~0 ~44 ~19 ~0 ~44 minecraft:packed_mud
fill ~18 ~0 ~45 ~19 ~0 ~45 minecraft:rooted_dirt
setblock ~18 ~0 ~46 minecraft:coarse_dirt
fill ~19 ~0 ~0 ~21 ~0 ~0 minecraft:packed_mud
fill ~19 ~0 ~1 ~19 ~0 ~2 minecraft:rooted_dirt
setblock ~19 ~0 ~3 minecraft:packed_mud
setblock ~19 ~0 ~4 minecraft:coarse_dirt
setblock ~19 ~0 ~6 minecraft:coarse_dirt
fill ~19 ~0 ~42 ~20 ~0 ~42 minecraft:coarse_dirt
fill ~19 ~0 ~46 ~20 ~0 ~46 minecraft:packed_mud
fill ~20 ~0 ~1 ~21 ~0 ~1 minecraft:coarse_dirt
fill ~20 ~0 ~2 ~20 ~0 ~3 minecraft:coarse_dirt
fill ~20 ~0 ~4 ~20 ~0 ~5 minecraft:packed_mud
setblock ~20 ~0 ~6 minecraft:gravel
setblock ~20 ~0 ~40 minecraft:packed_mud
fill ~20 ~0 ~41 ~21 ~0 ~41 minecraft:gravel
setblock ~20 ~0 ~43 minecraft:packed_mud
setblock ~20 ~0 ~44 minecraft:coarse_dirt
setblock ~20 ~0 ~45 minecraft:gravel
fill ~21 ~0 ~2 ~22 ~0 ~2 minecraft:packed_mud
setblock ~21 ~0 ~3 minecraft:rooted_dirt
setblock ~21 ~0 ~4 minecraft:gravel
fill ~21 ~0 ~5 ~22 ~0 ~5 minecraft:packed_mud
setblock ~21 ~0 ~6 minecraft:rooted_dirt
setblock ~21 ~0 ~40 minecraft:rooted_dirt
fill ~21 ~0 ~42 ~21 ~0 ~46 minecraft:packed_mud
fill ~22 ~0 ~0 ~22 ~0 ~1 minecraft:gravel
setblock ~22 ~0 ~3 minecraft:gravel
setblock ~22 ~0 ~4 minecraft:coarse_dirt
setblock ~22 ~0 ~6 minecraft:coarse_dirt
fill ~22 ~0 ~40 ~22 ~0 ~41 minecraft:packed_mud
fill ~22 ~0 ~42 ~23 ~0 ~42 minecraft:gravel
setblock ~22 ~0 ~43 minecraft:packed_mud
setblock ~22 ~0 ~44 minecraft:rooted_dirt
setblock ~22 ~0 ~45 minecraft:gravel
setblock ~22 ~0 ~46 minecraft:coarse_dirt
fill ~23 ~0 ~0 ~23 ~0 ~1 minecraft:packed_mud
setblock ~23 ~0 ~2 minecraft:coarse_dirt
fill ~23 ~0 ~3 ~25 ~0 ~4 minecraft:packed_mud
setblock ~23 ~0 ~5 minecraft:gravel
setblock ~23 ~0 ~41 minecraft:packed_mud
setblock ~23 ~0 ~43 minecraft:coarse_dirt
setblock ~23 ~0 ~44 minecraft:gravel
setblock ~23 ~0 ~45 minecraft:coarse_dirt
fill ~23 ~0 ~46 ~25 ~0 ~46 minecraft:packed_mud
setblock ~24 ~0 ~0 minecraft:rooted_dirt
fill ~24 ~0 ~1 ~25 ~0 ~1 minecraft:coarse_dirt
fill ~24 ~0 ~2 ~25 ~0 ~2 minecraft:gravel
setblock ~24 ~0 ~5 minecraft:packed_mud
setblock ~24 ~0 ~6 minecraft:coarse_dirt
fill ~24 ~0 ~40 ~26 ~0 ~40 minecraft:packed_mud
fill ~24 ~0 ~41 ~24 ~0 ~42 minecraft:coarse_dirt
setblock ~24 ~0 ~43 minecraft:gravel
fill ~24 ~0 ~44 ~24 ~0 ~45 minecraft:coarse_dirt
setblock ~25 ~0 ~0 minecraft:packed_mud
setblock ~25 ~0 ~5 minecraft:coarse_dirt
setblock ~25 ~0 ~6 minecraft:packed_mud
fill ~25 ~0 ~41 ~27 ~0 ~41 minecraft:packed_mud
setblock ~25 ~0 ~42 minecraft:packed_mud
setblock ~25 ~0 ~43 minecraft:coarse_dirt
setblock ~25 ~0 ~44 minecraft:packed_mud
setblock ~25 ~0 ~45 minecraft:coarse_dirt
setblock ~26 ~0 ~0 minecraft:gravel
setblock ~26 ~0 ~1 minecraft:packed_mud
fill ~26 ~0 ~2 ~27 ~0 ~2 minecraft:rooted_dirt
fill ~26 ~0 ~3 ~28 ~0 ~3 minecraft:gravel
fill ~26 ~0 ~4 ~26 ~0 ~5 minecraft:packed_mud
setblock ~26 ~0 ~6 minecraft:rooted_dirt
fill ~26 ~0 ~42 ~26 ~0 ~43 minecraft:gravel
setblock ~26 ~0 ~44 minecraft:coarse_dirt
setblock ~26 ~0 ~45 minecraft:rooted_dirt
setblock ~26 ~0 ~46 minecraft:gravel
setblock ~27 ~0 ~0 minecraft:packed_mud
setblock ~27 ~0 ~1 minecraft:coarse_dirt
fill ~27 ~0 ~4 ~28 ~0 ~4 minecraft:coarse_dirt
setblock ~27 ~0 ~5 minecraft:coarse_dirt
setblock ~27 ~0 ~6 minecraft:gravel
setblock ~27 ~0 ~40 minecraft:gravel
setblock ~27 ~0 ~42 minecraft:coarse_dirt
fill ~27 ~0 ~43 ~29 ~0 ~43 minecraft:packed_mud
fill ~27 ~0 ~44 ~29 ~0 ~44 minecraft:gravel
fill ~27 ~0 ~45 ~27 ~0 ~46 minecraft:packed_mud
fill ~28 ~0 ~0 ~29 ~0 ~0 minecraft:gravel
setblock ~28 ~0 ~1 minecraft:gravel
setblock ~28 ~0 ~2 minecraft:coarse_dirt
fill ~28 ~0 ~5 ~28 ~0 ~6 minecraft:packed_mud
fill ~28 ~0 ~40 ~31 ~0 ~40 minecraft:packed_mud
setblock ~28 ~0 ~41 minecraft:coarse_dirt
fill ~28 ~0 ~42 ~30 ~0 ~42 minecraft:gravel
fill ~28 ~0 ~45 ~29 ~0 ~45 minecraft:coarse_dirt
setblock ~28 ~0 ~46 minecraft:packed_mud
setblock ~29 ~0 ~1 minecraft:rooted_dirt
fill ~29 ~0 ~2 ~29 ~0 ~3 minecraft:gravel
setblock ~29 ~0 ~4 minecraft:packed_mud
setblock ~29 ~0 ~5 minecraft:coarse_dirt
fill ~29 ~0 ~6 ~30 ~0 ~6 minecraft:gravel
setblock ~29 ~0 ~7 minecraft:rooted_dirt
setblock ~29 ~0 ~39 minecraft:rooted_dirt
fill ~29 ~0 ~41 ~31 ~0 ~41 minecraft:packed_mud
setblock ~29 ~0 ~46 minecraft:coarse_dirt
fill ~30 ~0 ~0 ~31 ~0 ~1 minecraft:packed_mud
fill ~30 ~0 ~2 ~30 ~0 ~3 minecraft:coarse_dirt
setblock ~30 ~0 ~4 minecraft:gravel
setblock ~30 ~0 ~5 minecraft:rooted_dirt
setblock ~30 ~0 ~7 minecraft:packed_mud
setblock ~30 ~0 ~39 minecraft:gravel
setblock ~30 ~0 ~43 minecraft:coarse_dirt
setblock ~30 ~0 ~44 minecraft:packed_mud
setblock ~30 ~0 ~45 minecraft:rooted_dirt
fill ~30 ~0 ~46 ~31 ~0 ~46 minecraft:packed_mud
setblock ~31 ~0 ~2 minecraft:packed_mud
setblock ~31 ~0 ~3 minecraft:coarse_dirt
setblock ~31 ~0 ~4 minecraft:packed_mud
setblock ~31 ~0 ~5 minecraft:coarse_dirt
setblock ~31 ~0 ~6 minecraft:rooted_dirt
fill ~31 ~0 ~7 ~32 ~0 ~7 minecraft:coarse_dirt
setblock ~31 ~0 ~39 minecraft:coarse_dirt
setblock ~31 ~0 ~42 minecraft:coarse_dirt
setblock ~31 ~0 ~43 minecraft:packed_mud
setblock ~31 ~0 ~44 minecraft:gravel
setblock ~31 ~0 ~45 minecraft:coarse_dirt
setblock ~32 ~0 ~0 minecraft:coarse_dirt
fill ~32 ~0 ~1 ~32 ~0 ~3 minecraft:packed_mud
fill ~32 ~0 ~4 ~32 ~0 ~5 minecraft:coarse_dirt
setblock ~32 ~0 ~6 minecraft:packed_mud
setblock ~32 ~0 ~8 minecraft:rooted_dirt
fill ~32 ~0 ~38 ~32 ~0 ~40 minecraft:gravel
setblock ~32 ~0 ~41 minecraft:coarse_dirt
fill ~32 ~0 ~42 ~32 ~0 ~43 minecraft:rooted_dirt
fill ~32 ~0 ~44 ~32 ~0 ~45 minecraft:packed_mud
setblock ~32 ~0 ~46 minecraft:rooted_dirt
fill ~33 ~0 ~0 ~33 ~0 ~1 minecraft:packed_mud
setblock ~33 ~0 ~2 minecraft:coarse_dirt
fill ~33 ~0 ~3 ~33 ~0 ~5 minecraft:packed_mud
setblock ~33 ~0 ~6 minecraft:gravel
fill ~33 ~0 ~7 ~33 ~0 ~9 minecraft:packed_mud
setblock ~33 ~0 ~37 minecraft:coarse_dirt
setblock ~33 ~0 ~38 minecraft:packed_mud
fill ~33 ~0 ~39 ~33 ~0 ~40 minecraft:gravel
setblock ~33 ~0 ~41 minecraft:packed_mud
setblock ~33 ~0 ~42 minecraft:coarse_dirt
fill ~33 ~0 ~43 ~33 ~0 ~44 minecraft:packed_mud
setblock ~33 ~0 ~45 minecraft:gravel
setblock ~33 ~0 ~46 minecraft:coarse_dirt
fill ~34 ~0 ~0 ~34 ~0 ~1 minecraft:coarse_dirt
fill ~34 ~0 ~2 ~34 ~0 ~3 minecraft:packed_mud
fill ~34 ~0 ~4 ~36 ~0 ~4 minecraft:gravel
fill ~34 ~0 ~5 ~34 ~0 ~7 minecraft:coarse_dirt
setblock ~34 ~0 ~8 minecraft:gravel
setblock ~34 ~0 ~9 minecraft:coarse_dirt
fill ~34 ~0 ~10 ~35 ~0 ~10 minecraft:packed_mud
fill ~34 ~0 ~36 ~35 ~0 ~36 minecraft:coarse_dirt
fill ~34 ~0 ~37 ~35 ~0 ~37 minecraft:packed_mud
setblock ~34 ~0 ~38 minecraft:coarse_dirt
fill ~34 ~0 ~39 ~35 ~0 ~39 minecraft:packed_mud
setblock ~34 ~0 ~40 minecraft:packed_mud
setblock ~34 ~0 ~41 minecraft:gravel
fill ~34 ~0 ~42 ~34 ~0 ~43 minecraft:packed_mud
setblock ~34 ~0 ~44 minecraft:gravel
setblock ~34 ~0 ~45 minecraft:packed_mud
setblock ~34 ~0 ~46 minecraft:gravel
fill ~35 ~0 ~0 ~37 ~0 ~0 minecraft:packed_mud
fill ~35 ~0 ~1 ~35 ~0 ~2 minecraft:gravel
setblock ~35 ~0 ~3 minecraft:packed_mud
fill ~35 ~0 ~5 ~35 ~0 ~6 minecraft:packed_mud
fill ~35 ~0 ~7 ~35 ~0 ~8 minecraft:coarse_dirt
fill ~35 ~0 ~9 ~37 ~0 ~9 minecraft:gravel
fill ~35 ~0 ~38 ~36 ~0 ~38 minecraft:packed_mud
setblock ~35 ~0 ~40 minecraft:gravel
fill ~35 ~0 ~41 ~35 ~0 ~42 minecraft:rooted_dirt
fill ~35 ~0 ~43 ~35 ~0 ~45 minecraft:packed_mud
setblock ~35 ~0 ~46 minecraft:coarse_dirt
setblock ~36 ~0 ~1 minecraft:packed_mud
fill ~36 ~0 ~2 ~36 ~0 ~3 minecraft:coarse_dirt
setblock ~36 ~0 ~5 minecraft:coarse_dirt
fill ~36 ~0 ~6 ~36 ~0 ~7 minecraft:gravel
fill ~36 ~0 ~8 ~37 ~0 ~8 minecraft:coarse_dirt
fill ~36 ~0 ~10 ~37 ~0 ~10 minecraft:gravel
fill ~36 ~0 ~11 ~37 ~0 ~11 minecraft:coarse_dirt
setblock ~36 ~0 ~12 minecraft:coarse_dirt
fill ~36 ~0 ~34 ~39 ~0 ~34 minecraft:gravel
fill ~36 ~0 ~35 ~36 ~0 ~36 minecraft:packed_mud
setblock ~36 ~0 ~37 minecraft:coarse_dirt
fill ~36 ~0 ~39 ~36 ~0 ~40 minecraft:gravel
setblock ~36 ~0 ~41 minecraft:coarse_dirt
fill ~36 ~0 ~42 ~36 ~0 ~43 minecraft:packed_mud
setblock ~36 ~0 ~44 minecraft:gravel
setblock ~36 ~0 ~45 minecraft:packed_mud
setblock ~36 ~0 ~46 minecraft:gravel
setblock ~37 ~0 ~1 minecraft:coarse_dirt
fill ~37 ~0 ~2 ~38 ~0 ~2 minecraft:rooted_dirt
fill ~37 ~0 ~3 ~37 ~0 ~4 minecraft:packed_mud
setblock ~37 ~0 ~5 minecraft:gravel
setblock ~37 ~0 ~6 minecraft:packed_mud
setblock ~37 ~0 ~7 minecraft:gravel
fill ~37 ~0 ~12 ~38 ~0 ~12 minecraft:packed_mud
setblock ~37 ~0 ~13 minecraft:packed_mud
setblock ~37 ~0 ~33 minecraft:packed_mud
setblock ~37 ~0 ~35 minecraft:coarse_dirt
setblock ~37 ~0 ~36 minecraft:packed_mud
fill ~37 ~0 ~37 ~37 ~0 ~39 minecraft:gravel
setblock ~37 ~0 ~40 minecraft:rooted_dirt
fill ~37 ~0 ~41 ~37 ~0 ~42 minecraft:gravel
setblock ~37 ~0 ~43 minecraft:packed_mud
setblock ~37 ~0 ~44 minecraft:coarse_dirt
fill ~37 ~0 ~45 ~39 ~0 ~45 minecraft:gravel
setblock ~37 ~0 ~46 minecraft:coarse_dirt
setblock ~38 ~0 ~1 minecraft:gravel
fill ~38 ~0 ~3 ~39 ~0 ~3 minecraft:coarse_dirt
setblock ~38 ~0 ~4 minecraft:packed_mud
setblock ~38 ~0 ~5 minecraft:coarse_dirt
fill ~38 ~0 ~6 ~39 ~0 ~6 minecraft:gravel
fill ~38 ~0 ~7 ~39 ~0 ~7 minecraft:coarse_dirt
fill ~38 ~0 ~8 ~38 ~0 ~11 minecraft:gravel
setblock ~38 ~0 ~13 minecraft:coarse_dirt
setblock ~38 ~0 ~14 minecraft:rooted_dirt
setblock ~38 ~0 ~32 minecraft:gravel
setblock ~38 ~0 ~33 minecraft:rooted_dirt
setblock ~38 ~0 ~35 minecraft:gravel
setblock ~38 ~0 ~36 minecraft:coarse_dirt
fill ~38 ~0 ~37 ~38 ~0 ~40 minecraft:packed_mud
setblock ~38 ~0 ~41 minecraft:coarse_dirt
setblock ~38 ~0 ~42 minecraft:packed_mud
fill ~38 ~0 ~43 ~39 ~0 ~43 minecraft:gravel
fill ~38 ~0 ~44 ~39 ~0 ~44 minecraft:packed_mud
setblock ~39 ~0 ~2 minecraft:packed_mud
fill ~39 ~0 ~4 ~40 ~0 ~5 minecraft:coarse_dirt
setblock ~39 ~0 ~8 minecraft:packed_mud
setblock ~39 ~0 ~9 minecraft:gravel
fill ~39 ~0 ~10 ~39 ~0 ~11 minecraft:packed_mud
setblock ~39 ~0 ~12 minecraft:coarse_dirt
setblock ~39 ~0 ~13 minecraft:gravel
setblock ~39 ~0 ~14 minecraft:packed_mud
fill ~39 ~0 ~15 ~40 ~0 ~15 minecraft:coarse_dirt
fill ~39 ~0 ~16 ~39 ~0 ~17 minecraft:gravel
setblock ~39 ~0 ~29 minecraft:packed_mud
setblock ~39 ~0 ~30 minecraft:rooted_dirt
fill ~39 ~0 ~31 ~39 ~0 ~33 minecraft:packed_mud
setblock ~39 ~0 ~35 minecraft:coarse_dirt
fill ~39 ~0 ~36 ~39 ~0 ~37 minecraft:packed_mud
fill ~39 ~0 ~38 ~40 ~0 ~38 minecraft:rooted_dirt
setblock ~39 ~0 ~39 minecraft:coarse_dirt
fill ~39 ~0 ~40 ~39 ~0 ~42 minecraft:packed_mud
fill ~40 ~0 ~3 ~41 ~0 ~3 minecraft:packed_mud
fill ~40 ~0 ~6 ~40 ~0 ~8 minecraft:coarse_dirt
fill ~40 ~0 ~9 ~40 ~0 ~10 minecraft:packed_mud
setblock ~40 ~0 ~11 minecraft:coarse_dirt
setblock ~40 ~0 ~12 minecraft:gravel
fill ~40 ~0 ~13 ~41 ~0 ~13 minecraft:packed_mud
fill ~40 ~0 ~14 ~41 ~0 ~14 minecraft:gravel
fill ~40 ~0 ~16 ~40 ~0 ~17 minecraft:packed_mud
setblock ~40 ~0 ~18 minecraft:rooted_dirt
setblock ~40 ~0 ~19 minecraft:coarse_dirt
fill ~40 ~0 ~20 ~41 ~0 ~20 minecraft:gravel
setblock ~40 ~0 ~21 minecraft:gravel
fill ~40 ~0 ~22 ~41 ~0 ~22 minecraft:rooted_dirt
setblock ~40 ~0 ~24 minecraft:gravel
setblock ~40 ~0 ~25 minecraft:coarse_dirt
setblock ~40 ~0 ~26 minecraft:packed_mud
setblock ~40 ~0 ~27 minecraft:gravel
fill ~40 ~0 ~28 ~40 ~0 ~29 minecraft:coarse_dirt
setblock ~40 ~0 ~30 minecraft:packed_mud
setblock ~40 ~0 ~31 minecraft:coarse_dirt
fill ~40 ~0 ~32 ~40 ~0 ~33 minecraft:gravel
fill ~40 ~0 ~34 ~41 ~0 ~36 minecraft:packed_mud
setblock ~40 ~0 ~37 minecraft:coarse_dirt
fill ~40 ~0 ~39 ~40 ~0 ~40 minecraft:packed_mud
fill ~40 ~0 ~41 ~40 ~0 ~42 minecraft:gravel
setblock ~40 ~0 ~43 minecraft:coarse_dirt
setblock ~40 ~0 ~44 minecraft:rooted_dirt
setblock ~40 ~0 ~45 minecraft:packed_mud
fill ~41 ~0 ~4 ~42 ~0 ~4 minecraft:gravel
fill ~41 ~0 ~5 ~43 ~0 ~5 minecraft:coarse_dirt
setblock ~41 ~0 ~6 minecraft:gravel
setblock ~41 ~0 ~7 minecraft:packed_mud
fill ~41 ~0 ~8 ~43 ~0 ~8 minecraft:coarse_dirt
fill ~41 ~0 ~9 ~41 ~0 ~10 minecraft:coarse_dirt
fill ~41 ~0 ~11 ~42 ~0 ~11 minecraft:rooted_dirt
fill ~41 ~0 ~12 ~42 ~0 ~12 minecraft:coarse_dirt
fill ~41 ~0 ~15 ~41 ~0 ~17 minecraft:packed_mud
setblock ~41 ~0 ~18 minecraft:gravel
setblock ~41 ~0 ~19 minecraft:packed_mud
setblock ~41 ~0 ~21 minecraft:coarse_dirt
setblock ~41 ~0 ~23 minecraft:packed_mud
setblock ~41 ~0 ~24 minecraft:rooted_dirt
fill ~41 ~0 ~25 ~41 ~0 ~29 minecraft:packed_mud
fill ~41 ~0 ~30 ~42 ~0 ~30 minecraft:gravel
setblock ~41 ~0 ~31 minecraft:packed_mud
fill ~41 ~0 ~32 ~41 ~0 ~33 minecraft:coarse_dirt
setblock ~41 ~0 ~37 minecraft:packed_mud
setblock ~41 ~0 ~38 minecraft:coarse_dirt
fill ~41 ~0 ~39 ~42 ~0 ~39 minecraft:gravel
fill ~41 ~0 ~40 ~41 ~0 ~45 minecraft:gravel
fill ~42 ~0 ~6 ~43 ~0 ~6 minecraft:coarse_dirt
fill ~42 ~0 ~7 ~43 ~0 ~7 minecraft:gravel
fill ~42 ~0 ~9 ~43 ~0 ~10 minecraft:packed_mud
fill ~42 ~0 ~13 ~42 ~0 ~15 minecraft:coarse_dirt
fill ~42 ~0 ~16 ~42 ~0 ~17 minecraft:gravel
fill ~42 ~0 ~18 ~42 ~0 ~24 minecraft:packed_mud
setblock ~42 ~0 ~25 minecraft:coarse_dirt
fill ~42 ~0 ~26 ~43 ~0 ~26 minecraft:packed_mud
setblock ~42 ~0 ~27 minecraft:coarse_dirt
fill ~42 ~0 ~28 ~42 ~0 ~29 minecraft:packed_mud
setblock ~42 ~0 ~31 minecraft:coarse_dirt
fill ~42 ~0 ~32 ~42 ~0 ~33 minecraft:packed_mud
setblock ~42 ~0 ~34 minecraft:gravel
setblock ~42 ~0 ~35 minecraft:coarse_dirt
setblock ~42 ~0 ~36 minecraft:packed_mud
fill ~42 ~0 ~37 ~44 ~0 ~37 minecraft:coarse_dirt
setblock ~42 ~0 ~38 minecraft:packed_mud
fill ~42 ~0 ~40 ~43 ~0 ~40 minecraft:coarse_dirt
setblock ~42 ~0 ~41 minecraft:coarse_dirt
setblock ~42 ~0 ~42 minecraft:packed_mud
fill ~43 ~0 ~11 ~43 ~0 ~13 minecraft:coarse_dirt
fill ~43 ~0 ~14 ~44 ~0 ~14 minecraft:gravel
fill ~43 ~0 ~15 ~43 ~0 ~16 minecraft:coarse_dirt
fill ~43 ~0 ~17 ~44 ~0 ~18 minecraft:packed_mud
setblock ~43 ~0 ~19 minecraft:packed_mud
setblock ~43 ~0 ~20 minecraft:coarse_dirt
fill ~43 ~0 ~21 ~44 ~0 ~21 minecraft:packed_mud
setblock ~43 ~0 ~22 minecraft:rooted_dirt
setblock ~43 ~0 ~23 minecraft:gravel
fill ~43 ~0 ~24 ~44 ~0 ~24 minecraft:packed_mud
setblock ~43 ~0 ~25 minecraft:gravel
fill ~43 ~0 ~27 ~43 ~0 ~28 minecraft:gravel
setblock ~43 ~0 ~29 minecraft:rooted_dirt
setblock ~43 ~0 ~30 minecraft:packed_mud
fill ~43 ~0 ~31 ~43 ~0 ~32 minecraft:gravel
setblock ~43 ~0 ~33 minecraft:packed_mud
fill ~43 ~0 ~34 ~43 ~0 ~35 minecraft:rooted_dirt
setblock ~43 ~0 ~36 minecraft:gravel
setblock ~43 ~0 ~38 minecraft:gravel
setblock ~43 ~0 ~39 minecraft:packed_mud
setblock ~43 ~0 ~41 minecraft:packed_mud
fill ~44 ~0 ~6 ~45 ~0 ~6 minecraft:gravel
fill ~44 ~0 ~7 ~44 ~0 ~9 minecraft:coarse_dirt
fill ~44 ~0 ~10 ~44 ~0 ~11 minecraft:packed_mud
setblock ~44 ~0 ~12 minecraft:coarse_dirt
setblock ~44 ~0 ~13 minecraft:packed_mud
setblock ~44 ~0 ~15 minecraft:packed_mud
setblock ~44 ~0 ~16 minecraft:gravel
setblock ~44 ~0 ~19 minecraft:rooted_dirt
fill ~44 ~0 ~20 ~45 ~0 ~20 minecraft:packed_mud
setblock ~44 ~0 ~22 minecraft:gravel
setblock ~44 ~0 ~23 minecraft:coarse_dirt
fill ~44 ~0 ~25 ~44 ~0 ~31 minecraft:packed_mud
setblock ~44 ~0 ~32 minecraft:rooted_dirt
setblock ~44 ~0 ~33 minecraft:coarse_dirt
setblock ~44 ~0 ~34 minecraft:packed_mud
setblock ~44 ~0 ~35 minecraft:coarse_dirt
setblock ~44 ~0 ~36 minecraft:packed_mud
setblock ~44 ~0 ~38 minecraft:rooted_dirt
setblock ~44 ~0 ~39 minecraft:coarse_dirt
setblock ~45 ~0 ~7 minecraft:gravel
fill ~45 ~0 ~8 ~45 ~0 ~9 minecraft:packed_mud
setblock ~45 ~0 ~10 minecraft:gravel
fill ~45 ~0 ~11 ~46 ~0 ~11 minecraft:packed_mud
fill ~45 ~0 ~12 ~45 ~0 ~15 minecraft:gravel
fill ~45 ~0 ~16 ~45 ~0 ~17 minecraft:packed_mud
fill ~45 ~0 ~18 ~46 ~0 ~18 minecraft:gravel
fill ~45 ~0 ~19 ~46 ~0 ~19 minecraft:coarse_dirt
fill ~45 ~0 ~21 ~46 ~0 ~21 minecraft:rooted_dirt
setblock ~45 ~0 ~22 minecraft:coarse_dirt
fill ~45 ~0 ~23 ~45 ~0 ~24 minecraft:packed_mud
fill ~45 ~0 ~25 ~45 ~0 ~26 minecraft:gravel
setblock ~45 ~0 ~27 minecraft:coarse_dirt
setblock ~45 ~0 ~28 minecraft:rooted_dirt
setblock ~45 ~0 ~29 minecraft:coarse_dirt
fill ~45 ~0 ~30 ~45 ~0 ~32 minecraft:gravel
setblock ~45 ~0 ~33 minecraft:rooted_dirt
setblock ~45 ~0 ~34 minecraft:gravel
setblock ~45 ~0 ~35 minecraft:packed_mud
setblock ~45 ~0 ~36 minecraft:rooted_dirt
setblock ~45 ~0 ~37 minecraft:packed_mud
setblock ~45 ~0 ~38 minecraft:coarse_dirt
setblock ~46 ~0 ~8 minecraft:rooted_dirt
setblock ~46 ~0 ~9 minecraft:packed_mud
setblock ~46 ~0 ~10 minecraft:coarse_dirt
setblock ~46 ~0 ~12 minecraft:packed_mud
setblock ~46 ~0 ~13 minecraft:coarse_dirt
setblock ~46 ~0 ~14 minecraft:gravel
setblock ~46 ~0 ~15 minecraft:packed_mud
fill ~46 ~0 ~16 ~46 ~0 ~17 minecraft:coarse_dirt
setblock ~46 ~0 ~20 minecraft:gravel
setblock ~46 ~0 ~22 minecraft:rooted_dirt
setblock ~46 ~0 ~23 minecraft:coarse_dirt
fill ~46 ~0 ~24 ~46 ~0 ~25 minecraft:packed_mud
fill ~46 ~0 ~26 ~46 ~0 ~27 minecraft:coarse_dirt
fill ~46 ~0 ~28 ~46 ~0 ~29 minecraft:packed_mud
setblock ~46 ~0 ~30 minecraft:coarse_dirt
fill ~46 ~0 ~31 ~46 ~0 ~32 minecraft:packed_mud
setblock ~46 ~0 ~33 minecraft:gravel
fill ~46 ~0 ~34 ~46 ~0 ~35 minecraft:coarse_dirt
setblock ~46 ~0 ~36 minecraft:packed_mud
setblock ~46 ~0 ~37 minecraft:coarse_dirt

# the stockade: spruce logs y 1..4 on the ring r 21..23, a spruce fence on top at y 5, the logs a little ragged (some stand one shorter)
fill ~1 ~1 ~17 ~1 ~4 ~18 minecraft:spruce_log[axis=y]
fill ~1 ~1 ~28 ~2 ~4 ~29 minecraft:spruce_log[axis=y]
fill ~2 ~1 ~14 ~2 ~4 ~18 minecraft:spruce_log[axis=y]
fill ~2 ~1 ~30 ~3 ~3 ~32 minecraft:spruce_log[axis=y]
fill ~3 ~1 ~12 ~3 ~3 ~16 minecraft:spruce_log[axis=y]
fill ~3 ~1 ~33 ~3 ~4 ~34 minecraft:spruce_log[axis=y]
fill ~4 ~1 ~11 ~4 ~4 ~14 minecraft:spruce_log[axis=y]
fill ~4 ~1 ~32 ~4 ~3 ~35 minecraft:spruce_log[axis=y]
fill ~5 ~1 ~9 ~5 ~3 ~12 minecraft:spruce_log[axis=y]
fill ~5 ~1 ~34 ~5 ~4 ~37 minecraft:spruce_log[axis=y]
fill ~6 ~1 ~8 ~6 ~3 ~10 minecraft:spruce_log[axis=y]
fill ~6 ~1 ~36 ~6 ~3 ~38 minecraft:spruce_log[axis=y]
fill ~8 ~1 ~6 ~8 ~3 ~6 minecraft:spruce_log[axis=y]
fill ~8 ~1 ~40 ~10 ~3 ~40 minecraft:spruce_log[axis=y]
fill ~9 ~1 ~5 ~10 ~3 ~6 minecraft:spruce_log[axis=y]
fill ~9 ~1 ~41 ~12 ~3 ~41 minecraft:spruce_log[axis=y]
fill ~11 ~1 ~4 ~11 ~3 ~5 minecraft:spruce_log[axis=y]
fill ~11 ~1 ~42 ~14 ~4 ~42 minecraft:spruce_log[axis=y]
fill ~12 ~1 ~3 ~13 ~4 ~4 minecraft:spruce_log[axis=y]
fill ~12 ~1 ~5 ~12 ~4 ~5 minecraft:spruce_log[axis=y]
fill ~12 ~1 ~43 ~16 ~3 ~43 minecraft:spruce_log[axis=y]
fill ~14 ~1 ~2 ~16 ~4 ~3 minecraft:spruce_log[axis=y]
fill ~14 ~1 ~4 ~14 ~4 ~4 minecraft:spruce_log[axis=y]
fill ~14 ~1 ~44 ~32 ~3 ~44 minecraft:spruce_log[axis=y]
fill ~17 ~1 ~1 ~22 ~3 ~2 minecraft:spruce_log[axis=y]
fill ~17 ~1 ~45 ~29 ~4 ~45 minecraft:spruce_log[axis=y]
fill ~23 ~1 ~0 ~23 ~3 ~2 minecraft:spruce_log[axis=y]
fill ~23 ~1 ~46 ~23 ~4 ~46 minecraft:spruce_log[axis=y]
fill ~24 ~1 ~1 ~29 ~3 ~2 minecraft:spruce_log[axis=y]
fill ~30 ~1 ~2 ~32 ~4 ~3 minecraft:spruce_log[axis=y]
fill ~30 ~1 ~43 ~34 ~3 ~43 minecraft:spruce_log[axis=y]
fill ~32 ~1 ~4 ~32 ~4 ~4 minecraft:spruce_log[axis=y]
fill ~32 ~1 ~42 ~35 ~4 ~42 minecraft:spruce_log[axis=y]
fill ~33 ~1 ~3 ~34 ~4 ~4 minecraft:spruce_log[axis=y]
fill ~34 ~1 ~5 ~34 ~4 ~5 minecraft:spruce_log[axis=y]
fill ~34 ~1 ~41 ~37 ~4 ~41 minecraft:spruce_log[axis=y]
fill ~35 ~1 ~4 ~35 ~4 ~4 minecraft:spruce_log[axis=y]
fill ~36 ~1 ~40 ~38 ~4 ~40 minecraft:spruce_log[axis=y]
fill ~40 ~1 ~36 ~40 ~3 ~38 minecraft:spruce_log[axis=y]
fill ~41 ~1 ~12 ~43 ~3 ~12 minecraft:spruce_log[axis=y]
fill ~41 ~1 ~34 ~41 ~3 ~37 minecraft:spruce_log[axis=y]
fill ~42 ~1 ~13 ~43 ~3 ~14 minecraft:spruce_log[axis=y]
fill ~42 ~1 ~32 ~42 ~4 ~35 minecraft:spruce_log[axis=y]
fill ~43 ~1 ~15 ~43 ~4 ~16 minecraft:spruce_log[axis=y]
fill ~43 ~1 ~30 ~43 ~3 ~34 minecraft:spruce_log[axis=y]
fill ~44 ~1 ~14 ~44 ~3 ~32 minecraft:spruce_log[axis=y]
fill ~45 ~1 ~17 ~45 ~3 ~29 minecraft:spruce_log[axis=y]
fill ~46 ~1 ~23 ~46 ~4 ~23 minecraft:spruce_log[axis=y]
fill ~2 ~4 ~30 ~3 ~4 ~31 minecraft:spruce_log[axis=y]
setblock ~2 ~4 ~32 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~3 ~4 ~12 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
fill ~3 ~4 ~13 ~3 ~4 ~16 minecraft:spruce_log[axis=y]
fill ~3 ~4 ~32 ~4 ~4 ~32 minecraft:spruce_log[axis=y]
fill ~4 ~4 ~33 ~4 ~4 ~34 minecraft:spruce_log[axis=y]
setblock ~4 ~4 ~35 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
fill ~5 ~4 ~9 ~6 ~4 ~9 minecraft:spruce_log[axis=y]
setblock ~5 ~4 ~10 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
fill ~5 ~4 ~11 ~5 ~4 ~12 minecraft:spruce_log[axis=y]
setblock ~6 ~4 ~8 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~6 ~4 ~10 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~6 ~4 ~36 minecraft:spruce_log[axis=y]
setblock ~6 ~4 ~37 minecraft:spruce_fence[north=true,east=true,south=true,west=true]
setblock ~6 ~4 ~38 minecraft:spruce_log[axis=y]
setblock ~8 ~4 ~6 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~8 ~4 ~40 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~9 ~4 ~5 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
fill ~9 ~4 ~6 ~10 ~4 ~6 minecraft:spruce_log[axis=y]
fill ~9 ~4 ~40 ~10 ~4 ~41 minecraft:spruce_log[axis=y]
fill ~10 ~4 ~5 ~11 ~4 ~5 minecraft:spruce_log[axis=y]
setblock ~11 ~4 ~4 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~11 ~4 ~41 minecraft:spruce_log[axis=y]
setblock ~12 ~4 ~41 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
fill ~12 ~4 ~43 ~14 ~4 ~43 minecraft:spruce_log[axis=y]
fill ~14 ~4 ~44 ~15 ~4 ~44 minecraft:spruce_log[axis=y]
setblock ~15 ~4 ~43 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~16 ~4 ~43 minecraft:spruce_log[axis=y]
setblock ~16 ~4 ~44 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~17 ~4 ~1 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~17 ~4 ~2 minecraft:spruce_log[axis=y]
setblock ~17 ~4 ~44 minecraft:spruce_log[axis=y]
fill ~18 ~4 ~1 ~21 ~4 ~2 minecraft:spruce_log[axis=y]
setblock ~18 ~4 ~44 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
fill ~19 ~4 ~44 ~32 ~4 ~44 minecraft:spruce_log[axis=y]
setblock ~22 ~4 ~1 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~22 ~4 ~2 minecraft:spruce_log[axis=y]
fill ~23 ~4 ~0 ~23 ~4 ~1 minecraft:spruce_log[axis=y]
fill ~23 ~4 ~2 ~24 ~4 ~2 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
fill ~24 ~4 ~1 ~27 ~4 ~1 minecraft:spruce_log[axis=y]
setblock ~25 ~4 ~2 minecraft:spruce_log[axis=y]
setblock ~26 ~4 ~2 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
fill ~27 ~4 ~2 ~28 ~4 ~2 minecraft:spruce_log[axis=y]
setblock ~28 ~4 ~1 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
fill ~29 ~4 ~1 ~29 ~4 ~2 minecraft:spruce_log[axis=y]
setblock ~30 ~4 ~43 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
fill ~31 ~4 ~43 ~34 ~4 ~43 minecraft:spruce_log[axis=y]
fill ~40 ~4 ~36 ~41 ~4 ~36 minecraft:spruce_log[axis=y]
setblock ~40 ~4 ~37 minecraft:spruce_fence[north=true,east=true,south=true,west=true]
setblock ~40 ~4 ~38 minecraft:spruce_log[axis=y]
fill ~41 ~4 ~12 ~42 ~4 ~12 minecraft:spruce_log[axis=y]
setblock ~41 ~4 ~34 minecraft:spruce_log[axis=y]
setblock ~41 ~4 ~35 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~41 ~4 ~37 minecraft:spruce_log[axis=y]
setblock ~42 ~4 ~13 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~42 ~4 ~14 minecraft:spruce_log[axis=y]
setblock ~43 ~4 ~12 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~43 ~4 ~13 minecraft:spruce_log[axis=y]
setblock ~43 ~4 ~14 minecraft:spruce_fence[north=true,east=true,south=true,west=true]
fill ~43 ~4 ~30 ~43 ~4 ~33 minecraft:spruce_log[axis=y]
setblock ~43 ~4 ~34 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
fill ~44 ~4 ~14 ~44 ~4 ~17 minecraft:spruce_log[axis=y]
setblock ~44 ~4 ~18 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
fill ~44 ~4 ~19 ~44 ~4 ~24 minecraft:spruce_log[axis=y]
setblock ~44 ~4 ~25 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
fill ~44 ~4 ~26 ~44 ~4 ~32 minecraft:spruce_log[axis=y]
fill ~45 ~4 ~17 ~45 ~4 ~23 minecraft:spruce_log[axis=y]
setblock ~45 ~4 ~24 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
fill ~45 ~4 ~25 ~45 ~4 ~29 minecraft:spruce_log[axis=y]
setblock ~1 ~5 ~17 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~1 ~5 ~18 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~1 ~5 ~28 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~1 ~5 ~29 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~2 ~5 ~14 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
fill ~2 ~5 ~15 ~2 ~5 ~16 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
fill ~2 ~5 ~17 ~2 ~5 ~18 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
fill ~2 ~5 ~28 ~2 ~5 ~29 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
setblock ~2 ~5 ~30 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~2 ~5 ~31 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~3 ~5 ~13 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~3 ~5 ~14 minecraft:spruce_fence[north=true,east=true,south=true,west=true]
setblock ~3 ~5 ~15 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
setblock ~3 ~5 ~16 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~3 ~5 ~30 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~3 ~5 ~31 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
fill ~3 ~5 ~32 ~3 ~5 ~33 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~3 ~5 ~34 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~4 ~5 ~11 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~4 ~5 ~12 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~4 ~5 ~13 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
setblock ~4 ~5 ~14 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~4 ~5 ~32 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~4 ~5 ~33 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
setblock ~4 ~5 ~34 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~5 ~5 ~9 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
setblock ~5 ~5 ~11 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~5 ~5 ~12 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~5 ~5 ~34 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~5 ~5 ~35 minecraft:spruce_fence[north=true,east=false,south=true,west=false]
setblock ~5 ~5 ~36 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~5 ~5 ~37 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
setblock ~6 ~5 ~9 minecraft:spruce_fence[north=false,east=true,south=false,west=true]
setblock ~6 ~5 ~36 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
setblock ~6 ~5 ~38 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~9 ~5 ~6 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~9 ~5 ~40 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~9 ~5 ~41 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~10 ~5 ~5 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~10 ~5 ~6 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~10 ~5 ~40 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~10 ~5 ~41 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~11 ~5 ~5 minecraft:spruce_fence[north=false,east=true,south=false,west=true]
setblock ~11 ~5 ~41 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~11 ~5 ~42 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~12 ~5 ~3 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~12 ~5 ~4 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~12 ~5 ~5 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
fill ~12 ~5 ~42 ~13 ~5 ~42 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~12 ~5 ~43 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~13 ~5 ~3 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~13 ~5 ~4 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~13 ~5 ~43 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~14 ~5 ~2 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~14 ~5 ~3 minecraft:spruce_fence[north=true,east=true,south=true,west=true]
setblock ~14 ~5 ~4 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~14 ~5 ~42 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~14 ~5 ~43 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
setblock ~14 ~5 ~44 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
fill ~15 ~5 ~2 ~16 ~5 ~2 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~15 ~5 ~3 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~15 ~5 ~44 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
setblock ~16 ~5 ~3 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~16 ~5 ~43 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~17 ~5 ~2 minecraft:spruce_fence[north=false,east=true,south=false,west=true]
setblock ~17 ~5 ~44 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~17 ~5 ~45 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~18 ~5 ~1 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
fill ~18 ~5 ~2 ~21 ~5 ~2 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~18 ~5 ~45 minecraft:spruce_fence[north=false,east=true,south=false,west=true]
fill ~19 ~5 ~1 ~20 ~5 ~1 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~19 ~5 ~44 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
fill ~19 ~5 ~45 ~22 ~5 ~45 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
fill ~20 ~5 ~44 ~29 ~5 ~44 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~21 ~5 ~1 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~22 ~5 ~2 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
setblock ~23 ~5 ~0 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~23 ~5 ~1 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~23 ~5 ~45 minecraft:spruce_fence[north=true,east=true,south=true,west=true]
setblock ~23 ~5 ~46 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
setblock ~24 ~5 ~1 minecraft:spruce_fence[north=false,east=true,south=false,west=true]
fill ~24 ~5 ~45 ~28 ~5 ~45 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~25 ~5 ~1 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~25 ~5 ~2 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
setblock ~26 ~5 ~1 minecraft:spruce_fence[north=false,east=true,south=false,west=true]
setblock ~27 ~5 ~1 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~27 ~5 ~2 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~28 ~5 ~2 minecraft:spruce_fence[north=false,east=true,south=false,west=true]
setblock ~29 ~5 ~1 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
setblock ~29 ~5 ~2 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~29 ~5 ~45 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
fill ~30 ~5 ~2 ~31 ~5 ~2 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~30 ~5 ~3 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~30 ~5 ~44 minecraft:spruce_fence[north=false,east=true,south=false,west=true]
setblock ~31 ~5 ~3 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~31 ~5 ~43 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~31 ~5 ~44 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~32 ~5 ~2 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~32 ~5 ~3 minecraft:spruce_fence[north=true,east=true,south=true,west=true]
setblock ~32 ~5 ~4 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~32 ~5 ~42 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~32 ~5 ~43 minecraft:spruce_fence[north=true,east=true,south=true,west=true]
setblock ~32 ~5 ~44 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~33 ~5 ~3 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~33 ~5 ~4 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~33 ~5 ~42 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~33 ~5 ~43 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~34 ~5 ~3 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~34 ~5 ~4 minecraft:spruce_fence[north=true,east=true,south=true,west=true]
setblock ~34 ~5 ~5 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~34 ~5 ~41 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~34 ~5 ~42 minecraft:spruce_fence[north=true,east=true,south=true,west=true]
setblock ~34 ~5 ~43 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~35 ~5 ~4 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~35 ~5 ~41 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~35 ~5 ~42 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~36 ~5 ~40 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~36 ~5 ~41 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~37 ~5 ~40 minecraft:spruce_fence[north=true,east=true,south=true,west=true]
setblock ~37 ~5 ~41 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~38 ~5 ~40 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
setblock ~40 ~5 ~36 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
setblock ~40 ~5 ~38 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~41 ~5 ~12 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~41 ~5 ~34 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
setblock ~41 ~5 ~36 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~41 ~5 ~37 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
setblock ~42 ~5 ~12 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~42 ~5 ~14 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~42 ~5 ~32 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~42 ~5 ~33 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~42 ~5 ~34 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
setblock ~42 ~5 ~35 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
setblock ~43 ~5 ~13 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~43 ~5 ~15 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~43 ~5 ~16 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~43 ~5 ~30 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
setblock ~43 ~5 ~31 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~43 ~5 ~32 minecraft:spruce_fence[north=true,east=true,south=true,west=true]
setblock ~43 ~5 ~33 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~44 ~5 ~14 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
fill ~44 ~5 ~15 ~44 ~5 ~16 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
setblock ~44 ~5 ~17 minecraft:spruce_fence[north=true,east=true,south=false,west=false]
setblock ~44 ~5 ~19 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
fill ~44 ~5 ~20 ~44 ~5 ~23 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
setblock ~44 ~5 ~24 minecraft:spruce_fence[north=true,east=false,south=false,west=false]
setblock ~44 ~5 ~26 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
fill ~44 ~5 ~27 ~44 ~5 ~29 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
fill ~44 ~5 ~30 ~44 ~5 ~31 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
setblock ~44 ~5 ~32 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~45 ~5 ~17 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
setblock ~45 ~5 ~18 minecraft:spruce_fence[north=true,east=false,south=true,west=false]
fill ~45 ~5 ~19 ~45 ~5 ~22 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
setblock ~45 ~5 ~23 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~45 ~5 ~25 minecraft:spruce_fence[north=false,east=false,south=true,west=false]
fill ~45 ~5 ~26 ~45 ~5 ~28 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
setblock ~45 ~5 ~29 minecraft:spruce_fence[north=true,east=false,south=false,west=true]
setblock ~46 ~5 ~23 minecraft:spruce_fence[north=false,east=false,south=false,west=true]

# the gates: open from y 1 to 5 where the roads come in; gate posts one taller at each side (stripped spruce), a lintel beam at y 6 with a hanging lantern
fill ~0 ~1 ~23 ~0 ~5 ~23 minecraft:air
fill ~1 ~1 ~19 ~2 ~6 ~19 minecraft:stripped_spruce_log[axis=y]
fill ~1 ~1 ~20 ~2 ~4 ~26 minecraft:air
fill ~1 ~1 ~27 ~2 ~6 ~27 minecraft:stripped_spruce_log[axis=y]
fill ~35 ~1 ~5 ~36 ~6 ~5 minecraft:stripped_spruce_log[axis=y]
fill ~36 ~1 ~6 ~36 ~5 ~6 minecraft:air
fill ~37 ~1 ~5 ~37 ~5 ~7 minecraft:air
fill ~38 ~1 ~6 ~38 ~5 ~8 minecraft:air
fill ~39 ~1 ~7 ~39 ~5 ~9 minecraft:air
fill ~40 ~1 ~8 ~40 ~4 ~10 minecraft:air
fill ~41 ~1 ~9 ~41 ~5 ~10 minecraft:air
fill ~41 ~1 ~11 ~42 ~6 ~11 minecraft:stripped_spruce_log[axis=y]
fill ~1 ~5 ~20 ~2 ~5 ~22 minecraft:air
setblock ~1 ~5 ~23 minecraft:lantern[hanging=true]
fill ~1 ~5 ~24 ~1 ~5 ~26 minecraft:air
fill ~2 ~5 ~23 ~2 ~5 ~26 minecraft:air
setblock ~40 ~5 ~8 minecraft:air
setblock ~40 ~5 ~9 minecraft:lantern[hanging=true]
setblock ~40 ~5 ~10 minecraft:air
fill ~1 ~6 ~20 ~1 ~6 ~26 minecraft:spruce_planks
setblock ~37 ~6 ~6 minecraft:spruce_planks
setblock ~38 ~6 ~7 minecraft:spruce_planks
setblock ~39 ~6 ~8 minecraft:spruce_planks
setblock ~40 ~6 ~9 minecraft:spruce_planks
setblock ~41 ~6 ~10 minecraft:spruce_planks
setblock ~1 ~7 ~19 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
setblock ~1 ~7 ~27 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
setblock ~2 ~7 ~19 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
setblock ~2 ~7 ~27 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
setblock ~35 ~7 ~5 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
setblock ~36 ~7 ~5 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
setblock ~41 ~7 ~11 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
setblock ~42 ~7 ~11 minecraft:spruce_fence[north=false,east=false,south=false,west=true]

# the lookouts: 3x3 spruce towers on the stockade line, log corner posts y 1..8, a plank wall on the inner side for the ladder, platform of planks at y 9
fill ~37 ~1 ~37 ~37 ~8 ~37 minecraft:spruce_log[axis=y]
fill ~37 ~1 ~38 ~39 ~3 ~38 minecraft:air
fill ~37 ~1 ~39 ~37 ~8 ~39 minecraft:spruce_log[axis=y]
fill ~38 ~1 ~37 ~38 ~9 ~37 minecraft:spruce_planks
fill ~38 ~1 ~39 ~38 ~3 ~39 minecraft:air
fill ~39 ~1 ~37 ~39 ~8 ~37 minecraft:spruce_log[axis=y]
fill ~39 ~1 ~39 ~39 ~8 ~39 minecraft:spruce_log[axis=y]
fill ~38 ~4 ~38 ~38 ~8 ~38 minecraft:air
fill ~37 ~5 ~38 ~37 ~8 ~38 minecraft:air
fill ~38 ~5 ~39 ~38 ~8 ~39 minecraft:air
fill ~39 ~5 ~38 ~39 ~8 ~38 minecraft:air
fill ~37 ~9 ~37 ~37 ~9 ~39 minecraft:spruce_planks
fill ~38 ~9 ~38 ~38 ~9 ~39 minecraft:spruce_planks
fill ~39 ~9 ~37 ~39 ~9 ~39 minecraft:spruce_planks

# lookout se: bracing of spruce fences between the posts at y 4, the ladder on the inner side, a parapet of bottom slabs at y 10 with a gap at the ladder head
fill ~7 ~1 ~37 ~7 ~8 ~37 minecraft:spruce_log[axis=y]
fill ~7 ~1 ~38 ~9 ~3 ~38 minecraft:air
fill ~7 ~1 ~39 ~7 ~8 ~39 minecraft:spruce_log[axis=y]
fill ~8 ~1 ~37 ~8 ~9 ~37 minecraft:spruce_planks
fill ~8 ~1 ~39 ~8 ~3 ~39 minecraft:air
fill ~9 ~1 ~37 ~9 ~8 ~37 minecraft:spruce_log[axis=y]
fill ~9 ~1 ~39 ~9 ~8 ~39 minecraft:spruce_log[axis=y]
fill ~38 ~1 ~36 ~38 ~9 ~36 minecraft:ladder[facing=north]
fill ~8 ~4 ~38 ~8 ~8 ~38 minecraft:air
setblock ~37 ~4 ~38 minecraft:spruce_fence[north=true,east=false,south=true,west=false]
setblock ~38 ~4 ~39 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~39 ~4 ~38 minecraft:spruce_fence[north=true,east=true,south=true,west=false]
fill ~7 ~5 ~38 ~7 ~8 ~38 minecraft:air
fill ~8 ~5 ~39 ~8 ~8 ~39 minecraft:air
fill ~9 ~5 ~38 ~9 ~8 ~38 minecraft:air
fill ~7 ~9 ~37 ~7 ~9 ~39 minecraft:spruce_planks
fill ~8 ~9 ~38 ~8 ~9 ~39 minecraft:spruce_planks
fill ~9 ~9 ~37 ~9 ~9 ~39 minecraft:spruce_planks
fill ~37 ~10 ~37 ~37 ~10 ~39 minecraft:spruce_slab[type=bottom]
setblock ~38 ~10 ~39 minecraft:spruce_slab[type=bottom]
fill ~39 ~10 ~37 ~39 ~10 ~38 minecraft:spruce_slab[type=bottom]

# lookout sw: bracing of spruce fences between the posts at y 4, the ladder on the inner side, a parapet of bottom slabs at y 10 with a gap at the ladder head
fill ~7 ~1 ~7 ~7 ~8 ~7 minecraft:spruce_log[axis=y]
fill ~7 ~1 ~8 ~7 ~3 ~8 minecraft:air
fill ~7 ~1 ~9 ~7 ~8 ~9 minecraft:spruce_log[axis=y]
fill ~8 ~1 ~7 ~8 ~3 ~8 minecraft:air
fill ~8 ~1 ~9 ~8 ~9 ~9 minecraft:spruce_planks
fill ~8 ~1 ~36 ~8 ~9 ~36 minecraft:ladder[facing=north]
fill ~9 ~1 ~7 ~9 ~8 ~7 minecraft:spruce_log[axis=y]
fill ~9 ~1 ~8 ~9 ~3 ~8 minecraft:air
fill ~9 ~1 ~9 ~9 ~8 ~9 minecraft:spruce_log[axis=y]
setblock ~7 ~4 ~38 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
fill ~8 ~4 ~8 ~8 ~8 ~8 minecraft:air
setblock ~8 ~4 ~39 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~9 ~4 ~38 minecraft:spruce_fence[north=true,east=false,south=true,west=false]
fill ~7 ~5 ~8 ~7 ~8 ~8 minecraft:air
fill ~8 ~5 ~7 ~8 ~8 ~7 minecraft:air
fill ~9 ~5 ~8 ~9 ~8 ~8 minecraft:air
fill ~7 ~9 ~7 ~9 ~9 ~8 minecraft:spruce_planks
setblock ~7 ~9 ~9 minecraft:spruce_planks
setblock ~9 ~9 ~9 minecraft:spruce_planks
fill ~7 ~10 ~37 ~7 ~10 ~38 minecraft:spruce_slab[type=bottom]
setblock ~8 ~10 ~39 minecraft:spruce_slab[type=bottom]
fill ~9 ~10 ~37 ~9 ~10 ~39 minecraft:spruce_slab[type=bottom]

# lookout nw: bracing of spruce fences between the posts at y 4, the ladder on the inner side, a parapet of bottom slabs at y 10 with a gap at the ladder head
fill ~8 ~1 ~10 ~8 ~9 ~10 minecraft:ladder[facing=south]
setblock ~7 ~4 ~8 minecraft:spruce_fence[north=true,east=false,south=true,west=true]
setblock ~8 ~4 ~7 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~9 ~4 ~8 minecraft:spruce_fence[north=true,east=false,south=true,west=false]
fill ~7 ~10 ~8 ~7 ~10 ~9 minecraft:spruce_slab[type=bottom]
fill ~8 ~10 ~7 ~9 ~10 ~7 minecraft:spruce_slab[type=bottom]
fill ~9 ~10 ~8 ~9 ~10 ~9 minecraft:spruce_slab[type=bottom]

# a pole with a lantern on the outer corner of each lookout (away from the sight lines over the roads)
setblock ~7 ~10 ~7 minecraft:spruce_log[axis=y]
setblock ~7 ~10 ~39 minecraft:spruce_log[axis=y]
setblock ~39 ~10 ~39 minecraft:spruce_log[axis=y]
setblock ~7 ~11 ~7 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~7 ~11 ~39 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~39 ~11 ~39 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~7 ~12 ~7 minecraft:lantern
setblock ~7 ~12 ~39 minecraft:lantern
setblock ~39 ~12 ~39 minecraft:lantern

# the breastworks: two courses of stone brick between radius 9 and 12 within 15 degrees of the NW, SE and SW diagonals, capped with slabs on the second course
setblock ~13 ~1 ~17 minecraft:stone_bricks
fill ~13 ~1 ~29 ~16 ~1 ~29 minecraft:stone_bricks
setblock ~14 ~1 ~16 minecraft:mossy_stone_bricks
setblock ~14 ~1 ~17 minecraft:cracked_stone_bricks
setblock ~14 ~1 ~30 minecraft:mossy_cobblestone
fill ~15 ~1 ~15 ~15 ~2 ~16 minecraft:mossy_stone_bricks
fill ~15 ~1 ~17 ~15 ~1 ~18 minecraft:stone_bricks
fill ~15 ~1 ~28 ~15 ~2 ~28 minecraft:mossy_stone_bricks
fill ~15 ~1 ~30 ~16 ~1 ~30 minecraft:mossy_stone_bricks
fill ~15 ~1 ~31 ~16 ~1 ~31 minecraft:stone_bricks
setblock ~16 ~1 ~14 minecraft:mossy_stone_bricks
fill ~16 ~1 ~15 ~18 ~1 ~15 minecraft:stone_bricks
setblock ~16 ~1 ~16 minecraft:cracked_stone_bricks
fill ~16 ~1 ~17 ~16 ~2 ~17 minecraft:mossy_stone_bricks
setblock ~16 ~1 ~32 minecraft:mossy_stone_bricks
fill ~17 ~1 ~13 ~17 ~1 ~14 minecraft:mossy_stone_bricks
setblock ~17 ~1 ~16 minecraft:stone_bricks
fill ~17 ~1 ~30 ~17 ~1 ~33 minecraft:stone_bricks
setblock ~18 ~1 ~31 minecraft:cracked_stone_bricks
fill ~28 ~1 ~31 ~29 ~1 ~31 minecraft:mossy_stone_bricks
setblock ~29 ~1 ~30 minecraft:stone_bricks
setblock ~29 ~1 ~32 minecraft:mossy_stone_bricks
setblock ~29 ~1 ~33 minecraft:stone_bricks
fill ~30 ~1 ~29 ~30 ~1 ~31 minecraft:mossy_stone_bricks
setblock ~30 ~1 ~32 minecraft:stone_bricks
fill ~31 ~1 ~28 ~31 ~1 ~31 minecraft:stone_bricks
setblock ~32 ~1 ~29 minecraft:mossy_cobblestone
setblock ~32 ~1 ~30 minecraft:mossy_stone_bricks
setblock ~33 ~1 ~29 minecraft:mossy_stone_bricks
setblock ~13 ~2 ~17 minecraft:stone_brick_slab[type=bottom]
setblock ~13 ~2 ~29 minecraft:stone_bricks
fill ~14 ~2 ~16 ~14 ~2 ~17 minecraft:stone_brick_slab[type=bottom]
setblock ~14 ~2 ~29 minecraft:stone_brick_slab[type=bottom]
fill ~14 ~2 ~30 ~15 ~2 ~30 minecraft:stone_bricks
setblock ~15 ~2 ~17 minecraft:stone_brick_slab[type=bottom]
setblock ~15 ~2 ~18 minecraft:mossy_stone_bricks
fill ~15 ~2 ~29 ~16 ~2 ~29 minecraft:stone_bricks
setblock ~15 ~2 ~31 minecraft:mossy_stone_bricks
setblock ~16 ~2 ~14 minecraft:stone_bricks
setblock ~16 ~2 ~15 minecraft:stone_brick_slab[type=bottom]
setblock ~16 ~2 ~16 minecraft:stone_bricks
setblock ~16 ~2 ~30 minecraft:mossy_stone_bricks
setblock ~16 ~2 ~31 minecraft:stone_brick_slab[type=bottom]
setblock ~16 ~2 ~32 minecraft:stone_bricks
setblock ~17 ~2 ~13 minecraft:mossy_stone_bricks
setblock ~17 ~2 ~14 minecraft:stone_brick_slab[type=bottom]
fill ~17 ~2 ~15 ~18 ~2 ~15 minecraft:stone_bricks
setblock ~17 ~2 ~16 minecraft:stone_brick_slab[type=bottom]
fill ~17 ~2 ~30 ~17 ~2 ~31 minecraft:stone_bricks
setblock ~17 ~2 ~32 minecraft:mossy_stone_bricks
setblock ~17 ~2 ~33 minecraft:stone_bricks
setblock ~18 ~2 ~31 minecraft:mossy_stone_bricks
setblock ~28 ~2 ~31 minecraft:mossy_stone_bricks
fill ~29 ~2 ~30 ~29 ~2 ~32 minecraft:stone_brick_slab[type=bottom]
setblock ~29 ~2 ~33 minecraft:mossy_stone_bricks
setblock ~30 ~2 ~29 minecraft:mossy_stone_bricks
setblock ~30 ~2 ~30 minecraft:stone_bricks
setblock ~30 ~2 ~31 minecraft:stone_brick_slab[type=bottom]
setblock ~30 ~2 ~32 minecraft:mossy_stone_bricks
fill ~31 ~2 ~28 ~31 ~2 ~29 minecraft:stone_bricks
fill ~31 ~2 ~30 ~31 ~2 ~31 minecraft:stone_brick_slab[type=bottom]
fill ~32 ~2 ~29 ~33 ~2 ~29 minecraft:stone_bricks
setblock ~32 ~2 ~30 minecraft:stone_bricks

# the garrison stores against the stockade: barrels, hay and crates in the corners between the lookouts and the gates, a covered well-head by the south wall, braziers
setblock ~5 ~1 ~15 minecraft:hay_block[axis=y]
setblock ~5 ~1 ~16 minecraft:barrel[facing=up]
setblock ~6 ~1 ~15 minecraft:barrel[facing=up]
setblock ~11 ~1 ~9 minecraft:cobblestone
setblock ~11 ~1 ~37 minecraft:cobblestone
fill ~15 ~1 ~40 ~15 ~1 ~41 minecraft:hay_block[axis=y]
fill ~16 ~1 ~41 ~17 ~1 ~41 minecraft:barrel[facing=up]
setblock ~29 ~1 ~41 minecraft:hay_block[axis=y]
setblock ~30 ~1 ~41 minecraft:barrel[facing=up]
fill ~31 ~1 ~40 ~31 ~1 ~41 minecraft:barrel[facing=up]
setblock ~35 ~1 ~37 minecraft:cobblestone
fill ~40 ~1 ~29 ~41 ~1 ~29 minecraft:barrel[facing=up]
setblock ~41 ~1 ~30 minecraft:hay_block[axis=y]
setblock ~5 ~2 ~15 minecraft:barrel[facing=up]
setblock ~11 ~2 ~9 minecraft:campfire[lit=true,signal_fire=false,facing=north]
setblock ~11 ~2 ~37 minecraft:campfire[lit=true,signal_fire=false,facing=north]
setblock ~15 ~2 ~41 minecraft:barrel[facing=up]
setblock ~31 ~2 ~41 minecraft:barrel[facing=up]
setblock ~35 ~2 ~37 minecraft:campfire[lit=true,signal_fire=false,facing=north]
setblock ~41 ~2 ~29 minecraft:barrel[facing=up]

# tents for the garrison inside the south and west stockade: white canvas on spruce ridge poles
fill ~21 ~1 ~41 ~25 ~1 ~41 minecraft:white_wool
setblock ~21 ~1 ~42 minecraft:spruce_fence[north=true,east=false,south=true,west=false]
fill ~21 ~1 ~43 ~25 ~1 ~43 minecraft:white_wool
setblock ~25 ~1 ~42 minecraft:spruce_fence[north=true,east=false,south=true,west=false]
fill ~21 ~2 ~42 ~25 ~2 ~42 minecraft:white_wool
