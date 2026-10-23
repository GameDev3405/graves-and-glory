# size 23 16 23
# camp/graveyard: the Graveborn graveyard: terraces of graves climbing from a colonnade to a blackstone shrine set into the ridge (faces south).
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Slots: x 6..16 step 2 at z 19 (feet y 1) under the colonnade roof (y 5); the Graveborn wall banner at (11,4,18) facing south over the arch.
# The bank outside the sides is at y 10, 8, 6, 5, 4, 3, 2, 1 for z 0..7; the plateau behind z 0 is at y 12.

# the ground layer: cobbled deepslate paths, polished blackstone under the colonnade, soil elsewhere
fill ~0 ~0 ~0 ~22 ~0 ~11 minecraft:cobbled_deepslate
setblock ~0 ~0 ~12 minecraft:cobbled_deepslate
setblock ~0 ~0 ~13 minecraft:deepslate_bricks
setblock ~0 ~0 ~14 minecraft:cracked_deepslate_bricks
fill ~0 ~0 ~15 ~0 ~0 ~16 minecraft:deepslate_bricks
setblock ~0 ~0 ~17 minecraft:cobbled_deepslate
fill ~0 ~0 ~18 ~3 ~0 ~19 minecraft:coarse_dirt
fill ~0 ~0 ~20 ~1 ~0 ~20 minecraft:coarse_dirt
fill ~0 ~0 ~21 ~1 ~0 ~22 minecraft:rooted_dirt
setblock ~1 ~0 ~12 minecraft:cracked_deepslate_bricks
fill ~1 ~0 ~13 ~2 ~0 ~15 minecraft:cobbled_deepslate
setblock ~1 ~0 ~16 minecraft:cobbled_deepslate
setblock ~1 ~0 ~17 minecraft:cracked_deepslate_bricks
setblock ~2 ~0 ~12 minecraft:deepslate_bricks
setblock ~2 ~0 ~16 minecraft:mossy_cobblestone
setblock ~2 ~0 ~17 minecraft:cobbled_deepslate
fill ~2 ~0 ~20 ~3 ~0 ~20 minecraft:rooted_dirt
fill ~2 ~0 ~21 ~3 ~0 ~22 minecraft:podzol
fill ~3 ~0 ~12 ~3 ~0 ~14 minecraft:cobbled_deepslate
setblock ~3 ~0 ~15 minecraft:deepslate_bricks
fill ~3 ~0 ~16 ~3 ~0 ~17 minecraft:cobbled_deepslate
fill ~4 ~0 ~12 ~5 ~0 ~13 minecraft:moss_block
setblock ~4 ~0 ~14 minecraft:rooted_dirt
fill ~4 ~0 ~15 ~5 ~0 ~16 minecraft:cobbled_deepslate
fill ~4 ~0 ~17 ~5 ~0 ~17 minecraft:podzol
fill ~4 ~0 ~18 ~6 ~0 ~18 minecraft:polished_blackstone
fill ~4 ~0 ~19 ~5 ~0 ~19 minecraft:polished_blackstone
setblock ~4 ~0 ~20 minecraft:polished_blackstone
fill ~4 ~0 ~21 ~5 ~0 ~21 minecraft:coarse_dirt
fill ~4 ~0 ~22 ~5 ~0 ~22 minecraft:podzol
setblock ~5 ~0 ~14 minecraft:coarse_dirt
setblock ~5 ~0 ~20 minecraft:polished_blackstone_bricks
fill ~6 ~0 ~12 ~7 ~0 ~13 minecraft:rooted_dirt
setblock ~6 ~0 ~14 minecraft:podzol
fill ~6 ~0 ~15 ~7 ~0 ~15 minecraft:cracked_deepslate_bricks
fill ~6 ~0 ~16 ~7 ~0 ~16 minecraft:cobbled_deepslate
fill ~6 ~0 ~17 ~7 ~0 ~17 minecraft:coarse_dirt
setblock ~6 ~0 ~19 minecraft:polished_blackstone_bricks
fill ~6 ~0 ~20 ~9 ~0 ~20 minecraft:polished_blackstone
fill ~6 ~0 ~21 ~7 ~0 ~21 minecraft:podzol
fill ~6 ~0 ~22 ~7 ~0 ~22 minecraft:coarse_dirt
setblock ~7 ~0 ~14 minecraft:coarse_dirt
setblock ~7 ~0 ~18 minecraft:polished_blackstone_bricks
fill ~7 ~0 ~19 ~10 ~0 ~19 minecraft:polished_blackstone
fill ~8 ~0 ~12 ~9 ~0 ~14 minecraft:podzol
fill ~8 ~0 ~15 ~8 ~0 ~16 minecraft:cobbled_deepslate
fill ~8 ~0 ~17 ~9 ~0 ~17 minecraft:podzol
fill ~8 ~0 ~18 ~11 ~0 ~18 minecraft:polished_blackstone
setblock ~8 ~0 ~21 minecraft:cracked_deepslate_bricks
setblock ~8 ~0 ~22 minecraft:mossy_cobblestone
fill ~9 ~0 ~15 ~10 ~0 ~15 minecraft:deepslate_bricks
setblock ~9 ~0 ~16 minecraft:mossy_cobblestone
fill ~9 ~0 ~21 ~10 ~0 ~22 minecraft:cobbled_deepslate
setblock ~10 ~0 ~12 minecraft:mossy_cobblestone
setblock ~10 ~0 ~13 minecraft:deepslate_bricks
setblock ~10 ~0 ~14 minecraft:cracked_deepslate_bricks
fill ~10 ~0 ~16 ~10 ~0 ~17 minecraft:cobbled_deepslate
setblock ~10 ~0 ~20 minecraft:polished_blackstone_bricks
fill ~11 ~0 ~12 ~12 ~0 ~12 minecraft:cobbled_deepslate
setblock ~11 ~0 ~13 minecraft:cobbled_deepslate
setblock ~11 ~0 ~14 minecraft:deepslate_bricks
setblock ~11 ~0 ~15 minecraft:cobbled_deepslate
fill ~11 ~0 ~16 ~11 ~0 ~17 minecraft:deepslate_bricks
setblock ~11 ~0 ~19 minecraft:polished_blackstone_bricks
fill ~11 ~0 ~20 ~14 ~0 ~20 minecraft:polished_blackstone
setblock ~11 ~0 ~21 minecraft:deepslate_bricks
setblock ~11 ~0 ~22 minecraft:mossy_cobblestone
setblock ~12 ~0 ~13 minecraft:deepslate_bricks
setblock ~12 ~0 ~14 minecraft:cobbled_deepslate
setblock ~12 ~0 ~15 minecraft:cracked_deepslate_bricks
fill ~12 ~0 ~16 ~12 ~0 ~17 minecraft:cobbled_deepslate
setblock ~12 ~0 ~18 minecraft:polished_blackstone_bricks
fill ~12 ~0 ~19 ~15 ~0 ~19 minecraft:polished_blackstone
setblock ~12 ~0 ~21 minecraft:mossy_cobblestone
setblock ~12 ~0 ~22 minecraft:cobbled_deepslate
fill ~13 ~0 ~12 ~13 ~0 ~14 minecraft:coarse_dirt
fill ~13 ~0 ~15 ~13 ~0 ~16 minecraft:cobbled_deepslate
setblock ~13 ~0 ~17 minecraft:rooted_dirt
fill ~13 ~0 ~18 ~16 ~0 ~18 minecraft:polished_blackstone
fill ~13 ~0 ~21 ~14 ~0 ~22 minecraft:cobbled_deepslate
fill ~14 ~0 ~12 ~17 ~0 ~13 minecraft:podzol
fill ~14 ~0 ~14 ~15 ~0 ~14 minecraft:coarse_dirt
setblock ~14 ~0 ~15 minecraft:deepslate_bricks
setblock ~14 ~0 ~16 minecraft:cobbled_deepslate
fill ~14 ~0 ~17 ~17 ~0 ~17 minecraft:podzol
fill ~15 ~0 ~15 ~17 ~0 ~16 minecraft:cobbled_deepslate
setblock ~15 ~0 ~20 minecraft:polished_blackstone_bricks
fill ~15 ~0 ~21 ~17 ~0 ~21 minecraft:podzol
setblock ~15 ~0 ~22 minecraft:podzol
setblock ~16 ~0 ~14 minecraft:podzol
setblock ~16 ~0 ~19 minecraft:polished_blackstone_bricks
setblock ~16 ~0 ~20 minecraft:polished_blackstone
fill ~16 ~0 ~22 ~17 ~0 ~22 minecraft:rooted_dirt
setblock ~17 ~0 ~14 minecraft:rooted_dirt
setblock ~17 ~0 ~18 minecraft:polished_blackstone_bricks
fill ~17 ~0 ~19 ~17 ~0 ~20 minecraft:polished_blackstone
fill ~18 ~0 ~12 ~18 ~0 ~13 minecraft:coarse_dirt
setblock ~18 ~0 ~14 minecraft:podzol
setblock ~18 ~0 ~15 minecraft:cracked_deepslate_bricks
setblock ~18 ~0 ~16 minecraft:cobbled_deepslate
setblock ~18 ~0 ~17 minecraft:moss_block
fill ~18 ~0 ~18 ~18 ~0 ~20 minecraft:polished_blackstone
setblock ~18 ~0 ~21 minecraft:coarse_dirt
fill ~18 ~0 ~22 ~21 ~0 ~22 minecraft:podzol
fill ~19 ~0 ~12 ~21 ~0 ~12 minecraft:deepslate_bricks
fill ~19 ~0 ~13 ~19 ~0 ~14 minecraft:cobbled_deepslate
fill ~19 ~0 ~15 ~19 ~0 ~16 minecraft:deepslate_bricks
setblock ~19 ~0 ~17 minecraft:cobbled_deepslate
fill ~19 ~0 ~18 ~21 ~0 ~21 minecraft:coarse_dirt
fill ~20 ~0 ~13 ~21 ~0 ~13 minecraft:deepslate_bricks
setblock ~20 ~0 ~14 minecraft:deepslate_bricks
fill ~20 ~0 ~15 ~20 ~0 ~16 minecraft:cobbled_deepslate
setblock ~20 ~0 ~17 minecraft:deepslate_bricks
setblock ~21 ~0 ~14 minecraft:cobbled_deepslate
setblock ~21 ~0 ~15 minecraft:deepslate_bricks
fill ~21 ~0 ~16 ~21 ~0 ~17 minecraft:cobbled_deepslate
setblock ~22 ~0 ~12 minecraft:mossy_cobblestone
setblock ~22 ~0 ~13 minecraft:cobbled_deepslate
fill ~22 ~0 ~14 ~22 ~0 ~15 minecraft:mossy_cobblestone
setblock ~22 ~0 ~16 minecraft:cracked_deepslate_bricks
setblock ~22 ~0 ~17 minecraft:cobbled_deepslate
fill ~22 ~0 ~18 ~22 ~0 ~19 minecraft:rooted_dirt
setblock ~22 ~0 ~20 minecraft:coarse_dirt
setblock ~22 ~0 ~21 minecraft:podzol
setblock ~22 ~0 ~22 minecraft:moss_block

# the terraces: soil (podzol, coarse and rooted dirt, moss) on packed earth, climbing to the shrine; the back corners mound up to the ridge
fill ~1 ~1 ~0 ~5 ~2 ~7 minecraft:packed_mud
fill ~1 ~1 ~8 ~8 ~1 ~10 minecraft:coarse_dirt
fill ~6 ~1 ~5 ~8 ~2 ~7 minecraft:packed_mud
fill ~14 ~1 ~5 ~16 ~2 ~7 minecraft:packed_mud
fill ~14 ~1 ~8 ~21 ~1 ~10 minecraft:coarse_dirt
fill ~17 ~1 ~0 ~21 ~2 ~7 minecraft:packed_mud
setblock ~1 ~2 ~8 minecraft:rooted_dirt
fill ~1 ~2 ~9 ~3 ~2 ~9 minecraft:coarse_dirt
setblock ~1 ~2 ~10 minecraft:podzol
fill ~2 ~2 ~8 ~5 ~2 ~8 minecraft:podzol
setblock ~2 ~2 ~10 minecraft:rooted_dirt
fill ~3 ~2 ~10 ~4 ~2 ~10 minecraft:coarse_dirt
fill ~4 ~2 ~9 ~8 ~2 ~9 minecraft:podzol
fill ~5 ~2 ~10 ~8 ~2 ~10 minecraft:podzol
fill ~6 ~2 ~8 ~7 ~2 ~8 minecraft:rooted_dirt
setblock ~8 ~2 ~8 minecraft:coarse_dirt
fill ~14 ~2 ~8 ~15 ~2 ~9 minecraft:coarse_dirt
fill ~14 ~2 ~10 ~15 ~2 ~10 minecraft:podzol
fill ~16 ~2 ~8 ~17 ~2 ~8 minecraft:podzol
fill ~16 ~2 ~9 ~17 ~2 ~9 minecraft:coarse_dirt
setblock ~16 ~2 ~10 minecraft:rooted_dirt
fill ~17 ~2 ~10 ~18 ~2 ~10 minecraft:coarse_dirt
fill ~18 ~2 ~8 ~19 ~2 ~8 minecraft:coarse_dirt
fill ~18 ~2 ~9 ~19 ~2 ~9 minecraft:podzol
setblock ~19 ~2 ~10 minecraft:podzol
fill ~20 ~2 ~8 ~21 ~2 ~9 minecraft:podzol
setblock ~20 ~2 ~10 minecraft:rooted_dirt
setblock ~21 ~2 ~10 minecraft:podzol
fill ~1 ~3 ~0 ~5 ~4 ~4 minecraft:packed_mud
fill ~1 ~3 ~5 ~8 ~3 ~6 minecraft:coarse_dirt
fill ~14 ~3 ~5 ~21 ~3 ~6 minecraft:coarse_dirt
fill ~17 ~3 ~0 ~21 ~4 ~4 minecraft:packed_mud
fill ~1 ~4 ~5 ~2 ~4 ~5 minecraft:podzol
setblock ~1 ~4 ~6 minecraft:podzol
fill ~2 ~4 ~6 ~3 ~4 ~6 minecraft:moss_block
fill ~3 ~4 ~5 ~4 ~4 ~5 minecraft:coarse_dirt
setblock ~4 ~4 ~6 minecraft:podzol
fill ~5 ~4 ~5 ~5 ~4 ~6 minecraft:podzol
setblock ~6 ~4 ~5 minecraft:rooted_dirt
fill ~6 ~4 ~6 ~8 ~4 ~6 minecraft:podzol
setblock ~7 ~4 ~5 minecraft:coarse_dirt
setblock ~8 ~4 ~5 minecraft:rooted_dirt
fill ~14 ~4 ~5 ~15 ~4 ~5 minecraft:rooted_dirt
fill ~14 ~4 ~6 ~15 ~4 ~6 minecraft:moss_block
fill ~16 ~4 ~5 ~16 ~4 ~6 minecraft:coarse_dirt
fill ~17 ~4 ~5 ~19 ~4 ~5 minecraft:podzol
setblock ~17 ~4 ~6 minecraft:coarse_dirt
fill ~18 ~4 ~6 ~19 ~4 ~6 minecraft:podzol
setblock ~20 ~4 ~5 minecraft:rooted_dirt
fill ~20 ~4 ~6 ~21 ~4 ~6 minecraft:coarse_dirt
setblock ~21 ~4 ~5 minecraft:podzol
fill ~1 ~5 ~0 ~5 ~6 ~1 minecraft:packed_mud
fill ~1 ~5 ~2 ~4 ~5 ~2 minecraft:packed_mud
fill ~1 ~5 ~3 ~4 ~5 ~3 minecraft:coarse_dirt
fill ~5 ~5 ~2 ~5 ~5 ~3 minecraft:coarse_dirt
fill ~17 ~5 ~0 ~21 ~6 ~1 minecraft:packed_mud
fill ~17 ~5 ~2 ~17 ~5 ~3 minecraft:coarse_dirt
fill ~18 ~5 ~2 ~21 ~5 ~2 minecraft:packed_mud
fill ~18 ~5 ~3 ~21 ~5 ~3 minecraft:coarse_dirt
fill ~1 ~6 ~2 ~4 ~6 ~2 minecraft:coarse_dirt
setblock ~1 ~6 ~3 minecraft:podzol
fill ~2 ~6 ~3 ~3 ~6 ~3 minecraft:coarse_dirt
fill ~4 ~6 ~3 ~5 ~6 ~3 minecraft:podzol
setblock ~5 ~6 ~2 minecraft:moss_block
fill ~17 ~6 ~2 ~21 ~6 ~2 minecraft:coarse_dirt
setblock ~17 ~6 ~3 minecraft:rooted_dirt
fill ~18 ~6 ~3 ~19 ~6 ~3 minecraft:coarse_dirt
fill ~20 ~6 ~3 ~21 ~6 ~3 minecraft:podzol
fill ~1 ~7 ~0 ~5 ~9 ~0 minecraft:packed_mud
fill ~1 ~7 ~1 ~4 ~7 ~1 minecraft:packed_mud
setblock ~1 ~7 ~2 minecraft:podzol
fill ~2 ~7 ~2 ~3 ~7 ~2 minecraft:rooted_dirt
setblock ~4 ~7 ~2 minecraft:podzol
fill ~5 ~7 ~1 ~5 ~8 ~1 minecraft:coarse_dirt
fill ~17 ~7 ~0 ~21 ~9 ~0 minecraft:packed_mud
setblock ~17 ~7 ~1 minecraft:coarse_dirt
fill ~18 ~7 ~1 ~21 ~7 ~1 minecraft:packed_mud
fill ~18 ~7 ~2 ~19 ~7 ~2 minecraft:rooted_dirt
fill ~20 ~7 ~2 ~21 ~7 ~2 minecraft:podzol
fill ~1 ~8 ~1 ~4 ~8 ~1 minecraft:coarse_dirt
setblock ~17 ~8 ~1 minecraft:podzol
fill ~18 ~8 ~1 ~21 ~8 ~1 minecraft:coarse_dirt
fill ~1 ~9 ~1 ~4 ~9 ~1 minecraft:podzol
fill ~18 ~9 ~1 ~21 ~9 ~1 minecraft:podzol
fill ~1 ~10 ~0 ~5 ~10 ~0 minecraft:coarse_dirt
fill ~17 ~10 ~0 ~21 ~10 ~0 minecraft:coarse_dirt
setblock ~1 ~11 ~0 minecraft:moss_block
fill ~2 ~11 ~0 ~3 ~11 ~0 minecraft:podzol
fill ~4 ~11 ~0 ~5 ~11 ~0 minecraft:moss_block
fill ~17 ~11 ~0 ~19 ~11 ~0 minecraft:podzol
fill ~20 ~11 ~0 ~21 ~11 ~0 minecraft:rooted_dirt

# the retaining walls at the front of each terrace (z 11, 7 and 4), stone bricks worn and mossy, with a ragged line
fill ~1 ~1 ~11 ~1 ~2 ~11 minecraft:stone_bricks
fill ~2 ~1 ~11 ~2 ~2 ~11 minecraft:mossy_stone_bricks
setblock ~3 ~1 ~11 minecraft:stone_bricks
setblock ~4 ~1 ~11 minecraft:mossy_stone_bricks
fill ~5 ~1 ~11 ~8 ~2 ~11 minecraft:stone_bricks
fill ~14 ~1 ~11 ~15 ~1 ~11 minecraft:mossy_stone_bricks
setblock ~16 ~1 ~11 minecraft:stone_bricks
fill ~17 ~1 ~11 ~18 ~1 ~11 minecraft:cracked_stone_bricks
setblock ~19 ~1 ~11 minecraft:stone_bricks
setblock ~20 ~1 ~11 minecraft:cracked_stone_bricks
fill ~21 ~1 ~11 ~21 ~2 ~11 minecraft:stone_bricks
setblock ~3 ~2 ~11 minecraft:cracked_stone_bricks
setblock ~4 ~2 ~11 minecraft:stone_bricks
setblock ~14 ~2 ~11 minecraft:cracked_stone_bricks
fill ~15 ~2 ~11 ~16 ~2 ~11 minecraft:mossy_stone_bricks
setblock ~17 ~2 ~11 minecraft:cracked_stone_bricks
setblock ~18 ~2 ~11 minecraft:mossy_stone_bricks
setblock ~19 ~2 ~11 minecraft:cracked_stone_bricks
setblock ~20 ~2 ~11 minecraft:mossy_stone_bricks
setblock ~1 ~3 ~7 minecraft:stone_bricks
fill ~2 ~3 ~7 ~3 ~4 ~7 minecraft:mossy_stone_bricks
fill ~4 ~3 ~7 ~5 ~3 ~7 minecraft:stone_bricks
setblock ~6 ~3 ~7 minecraft:cracked_stone_bricks
fill ~7 ~3 ~7 ~8 ~3 ~7 minecraft:stone_bricks
setblock ~14 ~3 ~7 minecraft:mossy_stone_bricks
fill ~15 ~3 ~7 ~16 ~3 ~7 minecraft:stone_bricks
fill ~17 ~3 ~7 ~17 ~4 ~7 minecraft:mossy_stone_bricks
setblock ~18 ~3 ~7 minecraft:cracked_stone_bricks
fill ~19 ~3 ~7 ~20 ~3 ~7 minecraft:mossy_cobblestone
fill ~21 ~3 ~7 ~21 ~4 ~7 minecraft:stone_bricks
setblock ~1 ~4 ~7 minecraft:cracked_stone_bricks
setblock ~4 ~4 ~7 minecraft:mossy_stone_bricks
fill ~5 ~4 ~7 ~6 ~4 ~7 minecraft:stone_bricks
setblock ~7 ~4 ~7 minecraft:mossy_cobblestone
setblock ~8 ~4 ~7 minecraft:stone_bricks
fill ~14 ~4 ~7 ~15 ~4 ~7 minecraft:stone_bricks
setblock ~16 ~4 ~7 minecraft:mossy_stone_bricks
fill ~18 ~4 ~7 ~19 ~4 ~7 minecraft:stone_bricks
setblock ~20 ~4 ~7 minecraft:mossy_cobblestone
fill ~1 ~5 ~4 ~2 ~5 ~4 minecraft:stone_bricks
fill ~3 ~5 ~4 ~4 ~5 ~4 minecraft:cracked_stone_bricks
setblock ~5 ~5 ~4 minecraft:mossy_stone_bricks
setblock ~17 ~5 ~4 minecraft:stone_bricks
setblock ~18 ~5 ~4 minecraft:cracked_stone_bricks
setblock ~19 ~5 ~4 minecraft:mossy_cobblestone
setblock ~20 ~5 ~4 minecraft:stone_bricks
setblock ~21 ~5 ~4 minecraft:mossy_stone_bricks
setblock ~1 ~6 ~4 minecraft:cracked_stone_bricks
setblock ~2 ~6 ~4 minecraft:stone_bricks
setblock ~3 ~6 ~4 minecraft:mossy_cobblestone
fill ~4 ~6 ~4 ~5 ~6 ~4 minecraft:stone_bricks
fill ~17 ~6 ~4 ~20 ~6 ~4 minecraft:mossy_stone_bricks
setblock ~21 ~6 ~4 minecraft:stone_bricks

# the cemetery wall along the sides (x 0 and 22): stone bricks to the terrace or the bank, whichever is higher, two courses beside the court; gates at z 15..17
fill ~0 ~1 ~0 ~0 ~1 ~2 minecraft:stone_bricks
setblock ~0 ~1 ~3 minecraft:mossy_cobblestone
fill ~0 ~1 ~4 ~0 ~1 ~5 minecraft:cracked_stone_bricks
setblock ~0 ~1 ~6 minecraft:mossy_cobblestone
setblock ~0 ~1 ~7 minecraft:mossy_stone_bricks
setblock ~0 ~1 ~8 minecraft:cracked_stone_bricks
setblock ~0 ~1 ~9 minecraft:stone_bricks
setblock ~0 ~1 ~10 minecraft:mossy_cobblestone
fill ~0 ~1 ~11 ~0 ~1 ~13 minecraft:stone_bricks
setblock ~0 ~1 ~14 minecraft:mossy_stone_bricks
setblock ~0 ~1 ~18 minecraft:stone_bricks
setblock ~0 ~1 ~19 minecraft:mossy_stone_bricks
setblock ~0 ~1 ~20 minecraft:stone_bricks
fill ~22 ~1 ~0 ~22 ~1 ~1 minecraft:stone_bricks
setblock ~22 ~1 ~2 minecraft:mossy_stone_bricks
fill ~22 ~1 ~3 ~22 ~2 ~4 minecraft:stone_bricks
setblock ~22 ~1 ~5 minecraft:mossy_stone_bricks
setblock ~22 ~1 ~6 minecraft:cracked_stone_bricks
setblock ~22 ~1 ~7 minecraft:stone_bricks
setblock ~22 ~1 ~8 minecraft:mossy_stone_bricks
fill ~22 ~1 ~9 ~22 ~1 ~10 minecraft:stone_bricks
setblock ~22 ~1 ~11 minecraft:mossy_cobblestone
setblock ~22 ~1 ~12 minecraft:stone_bricks
fill ~22 ~1 ~13 ~22 ~1 ~14 minecraft:cracked_stone_bricks
setblock ~22 ~1 ~18 minecraft:cracked_stone_bricks
setblock ~22 ~1 ~19 minecraft:mossy_stone_bricks
setblock ~22 ~1 ~20 minecraft:stone_bricks
setblock ~0 ~2 ~0 minecraft:mossy_stone_bricks
setblock ~0 ~2 ~1 minecraft:cracked_stone_bricks
setblock ~0 ~2 ~2 minecraft:stone_bricks
fill ~0 ~2 ~3 ~0 ~3 ~3 minecraft:cracked_stone_bricks
setblock ~0 ~2 ~4 minecraft:mossy_cobblestone
fill ~0 ~2 ~5 ~0 ~2 ~6 minecraft:mossy_stone_bricks
fill ~0 ~2 ~7 ~0 ~2 ~8 minecraft:stone_bricks
setblock ~0 ~2 ~9 minecraft:mossy_stone_bricks
setblock ~0 ~2 ~10 minecraft:stone_bricks
setblock ~0 ~2 ~11 minecraft:cracked_stone_bricks
fill ~22 ~2 ~0 ~22 ~3 ~0 minecraft:cracked_stone_bricks
setblock ~22 ~2 ~1 minecraft:mossy_cobblestone
setblock ~22 ~2 ~2 minecraft:stone_bricks
fill ~22 ~2 ~5 ~22 ~2 ~6 minecraft:stone_bricks
setblock ~22 ~2 ~7 minecraft:mossy_stone_bricks
setblock ~22 ~2 ~8 minecraft:stone_bricks
fill ~22 ~2 ~9 ~22 ~2 ~10 minecraft:mossy_stone_bricks
setblock ~22 ~2 ~11 minecraft:stone_bricks
setblock ~0 ~3 ~0 minecraft:stone_bricks
fill ~0 ~3 ~1 ~0 ~3 ~2 minecraft:mossy_stone_bricks
setblock ~0 ~3 ~4 minecraft:mossy_stone_bricks
setblock ~0 ~3 ~5 minecraft:cracked_stone_bricks
setblock ~0 ~3 ~6 minecraft:mossy_stone_bricks
fill ~0 ~3 ~7 ~0 ~4 ~7 minecraft:stone_bricks
setblock ~22 ~3 ~1 minecraft:mossy_stone_bricks
setblock ~22 ~3 ~2 minecraft:cracked_stone_bricks
setblock ~22 ~3 ~3 minecraft:stone_bricks
fill ~22 ~3 ~4 ~22 ~3 ~5 minecraft:mossy_stone_bricks
setblock ~22 ~3 ~6 minecraft:cracked_stone_bricks
fill ~22 ~3 ~7 ~22 ~4 ~7 minecraft:stone_bricks
fill ~0 ~4 ~0 ~0 ~4 ~1 minecraft:cracked_stone_bricks
fill ~0 ~4 ~2 ~0 ~4 ~3 minecraft:mossy_stone_bricks
fill ~0 ~4 ~4 ~0 ~6 ~4 minecraft:stone_bricks
setblock ~0 ~4 ~5 minecraft:mossy_stone_bricks
setblock ~0 ~4 ~6 minecraft:mossy_cobblestone
setblock ~22 ~4 ~0 minecraft:mossy_stone_bricks
fill ~22 ~4 ~1 ~22 ~4 ~2 minecraft:stone_bricks
setblock ~22 ~4 ~3 minecraft:mossy_stone_bricks
fill ~22 ~4 ~4 ~22 ~4 ~6 minecraft:stone_bricks
fill ~0 ~5 ~0 ~0 ~6 ~0 minecraft:stone_bricks
fill ~0 ~5 ~1 ~0 ~7 ~1 minecraft:mossy_cobblestone
fill ~0 ~5 ~2 ~0 ~6 ~2 minecraft:stone_bricks
setblock ~0 ~5 ~3 minecraft:mossy_stone_bricks
fill ~22 ~5 ~0 ~22 ~5 ~1 minecraft:stone_bricks
setblock ~22 ~5 ~2 minecraft:mossy_stone_bricks
setblock ~22 ~5 ~3 minecraft:cracked_stone_bricks
setblock ~22 ~5 ~4 minecraft:mossy_stone_bricks
setblock ~0 ~6 ~3 minecraft:mossy_cobblestone
fill ~22 ~6 ~0 ~22 ~7 ~0 minecraft:stone_bricks
fill ~22 ~6 ~1 ~22 ~6 ~2 minecraft:mossy_cobblestone
fill ~22 ~6 ~3 ~22 ~6 ~4 minecraft:stone_bricks
setblock ~0 ~7 ~0 minecraft:mossy_cobblestone
setblock ~0 ~7 ~2 minecraft:mossy_cobblestone
fill ~22 ~7 ~1 ~22 ~8 ~1 minecraft:stone_bricks
setblock ~22 ~7 ~2 minecraft:cracked_stone_bricks
fill ~0 ~8 ~0 ~0 ~8 ~1 minecraft:stone_bricks
setblock ~22 ~8 ~0 minecraft:mossy_stone_bricks
setblock ~0 ~9 ~0 minecraft:mossy_cobblestone
setblock ~0 ~9 ~1 minecraft:stone_bricks
fill ~22 ~9 ~0 ~22 ~10 ~0 minecraft:cracked_stone_bricks
setblock ~22 ~9 ~1 minecraft:mossy_stone_bricks
setblock ~0 ~10 ~0 minecraft:stone_bricks
setblock ~0 ~11 ~0 minecraft:cracked_stone_bricks
setblock ~22 ~11 ~0 minecraft:mossy_stone_bricks

# copings: stone brick wall rails along the terrace fronts and the side walls where the ground beyond them lies lower; a few stones are gone
setblock ~0 ~2 ~13 minecraft:stone_brick_wall[north=none,east=none,south=low,west=none,up=true]
setblock ~0 ~2 ~14 minecraft:mossy_stone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~0 ~2 ~19 minecraft:mossy_stone_brick_wall[north=none,east=none,south=low,west=none,up=true]
setblock ~0 ~2 ~20 minecraft:stone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~22 ~2 ~13 minecraft:stone_brick_wall[north=none,east=none,south=low,west=none,up=true]
setblock ~22 ~2 ~14 minecraft:mossy_stone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~22 ~2 ~18 minecraft:stone_brick_wall[north=none,east=none,south=low,west=none,up=true]
setblock ~22 ~2 ~19 minecraft:mossy_stone_brick_wall[north=low,east=none,south=low,west=none,up=false]
setblock ~22 ~2 ~20 minecraft:stone_brick_wall[north=low,east=none,south=none,west=none,up=true]
fill ~0 ~3 ~8 ~0 ~3 ~10 minecraft:stone_brick_wall[north=low,east=none,south=low,west=none,up=false]
setblock ~0 ~3 ~11 minecraft:stone_brick_wall[north=low,east=low,south=none,west=none,up=true]
setblock ~1 ~3 ~11 minecraft:stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~2 ~3 ~11 minecraft:mossy_stone_brick_wall[north=none,east=none,south=none,west=low,up=true]
setblock ~4 ~3 ~11 minecraft:mossy_stone_brick_wall[north=none,east=low,south=none,west=none,up=true]
setblock ~5 ~3 ~11 minecraft:mossy_stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~6 ~3 ~11 minecraft:stone_brick_wall[north=none,east=none,south=none,west=low,up=true]
setblock ~8 ~3 ~11 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~14 ~3 ~11 minecraft:stone_brick_wall[north=none,east=low,south=none,west=none,up=true]
setblock ~15 ~3 ~11 minecraft:stone_brick_wall[north=none,east=none,south=none,west=low,up=true]
setblock ~17 ~3 ~11 minecraft:mossy_stone_brick_wall[north=none,east=low,south=none,west=none,up=true]
setblock ~18 ~3 ~11 minecraft:mossy_stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~19 ~3 ~11 minecraft:stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~20 ~3 ~11 minecraft:mossy_stone_brick_wall[north=low,east=low,south=none,west=low,up=true]
setblock ~21 ~3 ~11 minecraft:stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
fill ~22 ~3 ~8 ~22 ~3 ~9 minecraft:stone_brick_wall[north=low,east=none,south=low,west=none,up=false]
setblock ~22 ~3 ~10 minecraft:mossy_stone_brick_wall[north=low,east=none,south=low,west=none,up=false]
setblock ~22 ~3 ~11 minecraft:stone_brick_wall[north=low,east=none,south=none,west=low,up=true]
setblock ~0 ~5 ~5 minecraft:stone_brick_wall[north=low,east=none,south=low,west=none,up=false]
setblock ~0 ~5 ~6 minecraft:mossy_stone_brick_wall[north=low,east=none,south=low,west=none,up=false]
setblock ~0 ~5 ~7 minecraft:mossy_stone_brick_wall[north=low,east=low,south=none,west=none,up=true]
setblock ~1 ~5 ~7 minecraft:stone_brick_wall[north=none,east=none,south=none,west=low,up=true]
setblock ~3 ~5 ~7 minecraft:mossy_stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~5 ~5 ~7 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~15 ~5 ~7 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~17 ~5 ~7 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~19 ~5 ~7 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~22 ~5 ~5 minecraft:mossy_stone_brick_wall[north=low,east=none,south=low,west=none,up=false]
setblock ~22 ~5 ~6 minecraft:stone_brick_wall[north=low,east=none,south=low,west=none,up=false]
setblock ~22 ~5 ~7 minecraft:stone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~0 ~7 ~3 minecraft:mossy_stone_brick_wall[north=low,east=none,south=low,west=none,up=false]
setblock ~0 ~7 ~4 minecraft:stone_brick_wall[north=low,east=low,south=none,west=none,up=true]
setblock ~1 ~7 ~4 minecraft:stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~2 ~7 ~4 minecraft:stone_brick_wall[north=none,east=none,south=none,west=low,up=true]
fill ~17 ~7 ~4 ~18 ~7 ~4 minecraft:stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
fill ~19 ~7 ~4 ~20 ~7 ~4 minecraft:mossy_stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~21 ~7 ~4 minecraft:mossy_stone_brick_wall[north=none,east=none,south=none,west=low,up=true]
setblock ~22 ~7 ~3 minecraft:mossy_stone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~0 ~8 ~2 minecraft:stone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~22 ~8 ~2 minecraft:stone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~0 ~10 ~1 minecraft:mossy_stone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~22 ~10 ~1 minecraft:mossy_stone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~0 ~12 ~0 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~22 ~12 ~0 minecraft:mossy_stone_brick_wall[north=none,east=none,south=none,west=none,up=true]

# the stair up the middle (x 10..12): cobbled deepslate risers under deepslate brick treads, z 11 (y 1) to z 6 (y 6), a landing at z 5 (y 7)
fill ~10 ~1 ~5 ~12 ~1 ~10 minecraft:cobbled_deepslate
fill ~10 ~1 ~11 ~12 ~1 ~11 minecraft:deepslate_brick_stairs[facing=north]
fill ~10 ~2 ~5 ~12 ~2 ~9 minecraft:cobbled_deepslate
fill ~10 ~2 ~10 ~12 ~2 ~10 minecraft:deepslate_brick_stairs[facing=north]
fill ~10 ~2 ~11 ~12 ~8 ~11 minecraft:air
fill ~10 ~3 ~5 ~12 ~3 ~8 minecraft:cobbled_deepslate
fill ~10 ~3 ~9 ~12 ~3 ~9 minecraft:deepslate_brick_stairs[facing=north]
fill ~10 ~3 ~10 ~12 ~8 ~10 minecraft:air
fill ~10 ~4 ~5 ~12 ~4 ~7 minecraft:cobbled_deepslate
fill ~10 ~4 ~8 ~12 ~4 ~8 minecraft:deepslate_brick_stairs[facing=north]
fill ~10 ~4 ~9 ~12 ~8 ~9 minecraft:air
fill ~10 ~5 ~5 ~12 ~5 ~6 minecraft:cobbled_deepslate
fill ~10 ~5 ~7 ~12 ~5 ~7 minecraft:deepslate_brick_stairs[facing=north]
fill ~10 ~5 ~8 ~12 ~8 ~8 minecraft:air
fill ~10 ~6 ~5 ~12 ~6 ~5 minecraft:cobbled_deepslate
fill ~10 ~6 ~6 ~12 ~6 ~6 minecraft:deepslate_brick_stairs[facing=north]
fill ~10 ~6 ~7 ~12 ~8 ~7 minecraft:air
setblock ~10 ~7 ~5 minecraft:deepslate_bricks
fill ~10 ~7 ~6 ~12 ~8 ~6 minecraft:air
setblock ~11 ~7 ~5 minecraft:chiseled_deepslate
setblock ~12 ~7 ~5 minecraft:deepslate_bricks
fill ~10 ~8 ~5 ~12 ~10 ~5 minecraft:air

# the stair cheeks (x 9 and 13): blackstone walls from the terraces to a coping above the treads
setblock ~9 ~1 ~5 minecraft:cracked_polished_blackstone_bricks
fill ~9 ~1 ~6 ~9 ~1 ~11 minecraft:polished_blackstone_bricks
fill ~13 ~1 ~5 ~13 ~1 ~7 minecraft:polished_blackstone_bricks
setblock ~13 ~1 ~8 minecraft:polished_blackstone
fill ~13 ~1 ~9 ~13 ~2 ~10 minecraft:polished_blackstone_bricks
setblock ~13 ~1 ~11 minecraft:cracked_polished_blackstone_bricks
fill ~9 ~2 ~5 ~9 ~3 ~5 minecraft:polished_blackstone
fill ~9 ~2 ~6 ~9 ~6 ~6 minecraft:polished_blackstone_bricks
setblock ~9 ~2 ~7 minecraft:polished_blackstone
fill ~9 ~2 ~8 ~9 ~2 ~10 minecraft:polished_blackstone_bricks
setblock ~9 ~2 ~11 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=low,up=true]
fill ~13 ~2 ~5 ~13 ~3 ~5 minecraft:polished_blackstone_bricks
setblock ~13 ~2 ~6 minecraft:cracked_polished_blackstone_bricks
fill ~13 ~2 ~7 ~13 ~2 ~8 minecraft:polished_blackstone_bricks
setblock ~13 ~2 ~11 minecraft:polished_blackstone_brick_wall[north=low,east=low,south=none,west=none,up=true]
fill ~9 ~3 ~7 ~9 ~3 ~9 minecraft:polished_blackstone_bricks
setblock ~9 ~3 ~10 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
fill ~13 ~3 ~6 ~13 ~6 ~6 minecraft:polished_blackstone_bricks
fill ~13 ~3 ~7 ~13 ~5 ~7 minecraft:cracked_polished_blackstone_bricks
fill ~13 ~3 ~8 ~13 ~3 ~9 minecraft:polished_blackstone
setblock ~13 ~3 ~10 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~9 ~4 ~5 minecraft:polished_blackstone_bricks
fill ~9 ~4 ~7 ~9 ~4 ~8 minecraft:polished_blackstone_bricks
setblock ~9 ~4 ~9 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~13 ~4 ~5 minecraft:cracked_polished_blackstone_bricks
setblock ~13 ~4 ~8 minecraft:polished_blackstone_bricks
setblock ~13 ~4 ~9 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~9 ~5 ~5 minecraft:cracked_polished_blackstone_bricks
setblock ~9 ~5 ~7 minecraft:polished_blackstone_bricks
setblock ~9 ~5 ~8 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
fill ~13 ~5 ~5 ~13 ~7 ~5 minecraft:polished_blackstone_bricks
setblock ~13 ~5 ~8 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
fill ~9 ~6 ~5 ~9 ~7 ~5 minecraft:polished_blackstone_bricks
setblock ~9 ~6 ~7 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~13 ~6 ~7 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~9 ~7 ~6 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~13 ~7 ~6 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~9 ~8 ~5 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
setblock ~13 ~8 ~5 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]

# the shrine platform (x 6..16, z 0..4) at y 7 on blackstone, its floor polished deepslate
fill ~6 ~1 ~0 ~6 ~1 ~1 minecraft:polished_blackstone_bricks
fill ~6 ~1 ~2 ~6 ~1 ~3 minecraft:cracked_polished_blackstone_bricks
fill ~6 ~1 ~4 ~12 ~1 ~4 minecraft:polished_blackstone_bricks
fill ~7 ~1 ~0 ~15 ~6 ~3 minecraft:cobbled_deepslate
setblock ~13 ~1 ~4 minecraft:cracked_polished_blackstone_bricks
fill ~14 ~1 ~4 ~14 ~4 ~4 minecraft:polished_blackstone_bricks
setblock ~15 ~1 ~4 minecraft:cracked_polished_blackstone_bricks
setblock ~16 ~1 ~0 minecraft:polished_blackstone
fill ~16 ~1 ~1 ~16 ~7 ~1 minecraft:polished_blackstone_bricks
setblock ~16 ~1 ~2 minecraft:polished_blackstone
setblock ~16 ~1 ~3 minecraft:cracked_polished_blackstone_bricks
setblock ~16 ~1 ~4 minecraft:polished_blackstone_bricks
fill ~6 ~2 ~0 ~6 ~7 ~0 minecraft:polished_blackstone_bricks
setblock ~6 ~2 ~1 minecraft:polished_blackstone
fill ~6 ~2 ~2 ~6 ~6 ~4 minecraft:polished_blackstone_bricks
fill ~7 ~2 ~4 ~7 ~6 ~4 minecraft:polished_blackstone_bricks
setblock ~8 ~2 ~4 minecraft:cracked_polished_blackstone_bricks
setblock ~9 ~2 ~4 minecraft:polished_blackstone_bricks
fill ~10 ~2 ~4 ~11 ~2 ~4 minecraft:cracked_polished_blackstone_bricks
fill ~12 ~2 ~4 ~13 ~3 ~4 minecraft:polished_blackstone_bricks
setblock ~15 ~2 ~4 minecraft:polished_blackstone_bricks
fill ~16 ~2 ~0 ~16 ~7 ~0 minecraft:polished_blackstone_bricks
fill ~16 ~2 ~2 ~16 ~2 ~3 minecraft:polished_blackstone_bricks
fill ~16 ~2 ~4 ~16 ~3 ~4 minecraft:polished_blackstone
setblock ~6 ~3 ~1 minecraft:cracked_polished_blackstone_bricks
fill ~8 ~3 ~4 ~8 ~5 ~4 minecraft:polished_blackstone_bricks
setblock ~9 ~3 ~4 minecraft:cracked_polished_blackstone_bricks
fill ~10 ~3 ~4 ~11 ~3 ~4 minecraft:polished_blackstone_bricks
setblock ~15 ~3 ~4 minecraft:cracked_polished_blackstone_bricks
fill ~16 ~3 ~2 ~16 ~6 ~2 minecraft:polished_blackstone_bricks
setblock ~16 ~3 ~3 minecraft:cracked_polished_blackstone_bricks
fill ~6 ~4 ~1 ~6 ~7 ~1 minecraft:polished_blackstone_bricks
fill ~9 ~4 ~4 ~10 ~7 ~4 minecraft:polished_blackstone_bricks
fill ~11 ~4 ~4 ~12 ~4 ~4 minecraft:polished_blackstone
fill ~13 ~4 ~4 ~13 ~7 ~4 minecraft:polished_blackstone_bricks
fill ~15 ~4 ~4 ~15 ~5 ~4 minecraft:polished_blackstone_bricks
fill ~16 ~4 ~3 ~16 ~4 ~4 minecraft:polished_blackstone_bricks
setblock ~11 ~5 ~4 minecraft:cracked_polished_blackstone_bricks
setblock ~12 ~5 ~4 minecraft:polished_blackstone_bricks
setblock ~14 ~5 ~4 minecraft:cracked_polished_blackstone_bricks
setblock ~16 ~5 ~3 minecraft:cracked_polished_blackstone_bricks
setblock ~16 ~5 ~4 minecraft:polished_blackstone
setblock ~8 ~6 ~4 minecraft:cracked_polished_blackstone_bricks
fill ~11 ~6 ~4 ~11 ~7 ~4 minecraft:polished_blackstone_bricks
setblock ~12 ~6 ~4 minecraft:polished_blackstone
fill ~14 ~6 ~4 ~14 ~7 ~4 minecraft:polished_blackstone_bricks
setblock ~15 ~6 ~4 minecraft:cracked_polished_blackstone_bricks
fill ~16 ~6 ~3 ~16 ~7 ~4 minecraft:polished_blackstone_bricks
setblock ~6 ~7 ~2 minecraft:polished_blackstone_bricks
setblock ~6 ~7 ~3 minecraft:cracked_polished_blackstone_bricks
setblock ~6 ~7 ~4 minecraft:polished_blackstone_bricks
fill ~7 ~7 ~0 ~15 ~7 ~3 minecraft:polished_deepslate
fill ~7 ~7 ~4 ~8 ~7 ~4 minecraft:polished_blackstone
setblock ~12 ~7 ~4 minecraft:polished_blackstone_bricks
setblock ~15 ~7 ~4 minecraft:polished_blackstone_bricks
setblock ~16 ~7 ~2 minecraft:cracked_polished_blackstone_bricks

# the shrine walls (y 8..11): polished blackstone bricks, chiseled corner piers, the door arch x 10..12 in the front (z 4)
setblock ~6 ~8 ~0 minecraft:chiseled_polished_blackstone
setblock ~6 ~8 ~1 minecraft:cracked_polished_blackstone_bricks
fill ~6 ~8 ~2 ~6 ~11 ~3 minecraft:polished_blackstone_bricks
setblock ~6 ~8 ~4 minecraft:chiseled_polished_blackstone
fill ~7 ~8 ~0 ~13 ~8 ~0 minecraft:polished_blackstone_bricks
fill ~7 ~8 ~4 ~8 ~11 ~4 minecraft:polished_blackstone_bricks
setblock ~14 ~8 ~0 minecraft:cracked_polished_blackstone_bricks
fill ~14 ~8 ~4 ~14 ~9 ~4 minecraft:polished_blackstone_bricks
fill ~15 ~8 ~0 ~15 ~11 ~0 minecraft:polished_blackstone_bricks
fill ~15 ~8 ~4 ~15 ~9 ~4 minecraft:polished_blackstone
setblock ~16 ~8 ~0 minecraft:chiseled_polished_blackstone
fill ~16 ~8 ~1 ~16 ~8 ~2 minecraft:polished_blackstone_bricks
fill ~16 ~8 ~3 ~16 ~9 ~3 minecraft:cracked_polished_blackstone_bricks
setblock ~16 ~8 ~4 minecraft:chiseled_polished_blackstone
fill ~6 ~9 ~0 ~6 ~10 ~0 minecraft:polished_blackstone
fill ~6 ~9 ~1 ~6 ~11 ~1 minecraft:polished_blackstone_bricks
fill ~6 ~9 ~4 ~6 ~10 ~4 minecraft:polished_blackstone
fill ~7 ~9 ~0 ~8 ~9 ~0 minecraft:polished_blackstone_bricks
setblock ~9 ~9 ~0 minecraft:cracked_polished_blackstone_bricks
fill ~10 ~9 ~0 ~10 ~11 ~0 minecraft:polished_blackstone_bricks
setblock ~11 ~9 ~0 minecraft:cracked_polished_blackstone_bricks
setblock ~12 ~9 ~0 minecraft:polished_blackstone_bricks
fill ~13 ~9 ~0 ~13 ~10 ~0 minecraft:polished_blackstone
fill ~14 ~9 ~0 ~14 ~11 ~0 minecraft:polished_blackstone_bricks
fill ~16 ~9 ~0 ~16 ~10 ~0 minecraft:polished_blackstone
fill ~16 ~9 ~1 ~16 ~10 ~1 minecraft:cracked_polished_blackstone_bricks
setblock ~16 ~9 ~2 minecraft:polished_blackstone
fill ~16 ~9 ~4 ~16 ~10 ~4 minecraft:polished_blackstone
fill ~7 ~10 ~0 ~7 ~11 ~0 minecraft:polished_blackstone_bricks
setblock ~8 ~10 ~0 minecraft:cracked_polished_blackstone_bricks
setblock ~9 ~10 ~0 minecraft:polished_blackstone_bricks
fill ~11 ~10 ~0 ~11 ~11 ~0 minecraft:polished_blackstone_bricks
setblock ~12 ~10 ~0 minecraft:polished_blackstone
setblock ~14 ~10 ~4 minecraft:cracked_polished_blackstone_bricks
fill ~15 ~10 ~4 ~15 ~11 ~4 minecraft:polished_blackstone_bricks
setblock ~16 ~10 ~2 minecraft:cracked_polished_blackstone_bricks
fill ~16 ~10 ~3 ~16 ~11 ~3 minecraft:polished_blackstone_bricks
setblock ~6 ~11 ~0 minecraft:chiseled_polished_blackstone
setblock ~6 ~11 ~4 minecraft:chiseled_polished_blackstone
setblock ~8 ~11 ~0 minecraft:polished_blackstone_bricks
setblock ~9 ~11 ~0 minecraft:cracked_polished_blackstone_bricks
fill ~12 ~11 ~0 ~13 ~11 ~0 minecraft:polished_blackstone_bricks
setblock ~14 ~11 ~4 minecraft:polished_blackstone
setblock ~16 ~11 ~0 minecraft:chiseled_polished_blackstone
setblock ~16 ~11 ~1 minecraft:polished_blackstone
setblock ~16 ~11 ~2 minecraft:polished_blackstone_bricks
setblock ~16 ~11 ~4 minecraft:chiseled_polished_blackstone

# inside the shrine: air, then the altar (a blackstone block, candles, a skull) under a soul lantern
fill ~7 ~8 ~1 ~7 ~11 ~3 minecraft:air
setblock ~8 ~8 ~1 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
fill ~8 ~8 ~2 ~8 ~11 ~3 minecraft:air
fill ~9 ~8 ~1 ~9 ~11 ~3 minecraft:air
setblock ~10 ~8 ~1 minecraft:polished_blackstone_bricks
fill ~10 ~8 ~2 ~12 ~11 ~3 minecraft:air
setblock ~11 ~8 ~1 minecraft:chiseled_polished_blackstone
setblock ~12 ~8 ~1 minecraft:polished_blackstone_bricks
fill ~13 ~8 ~1 ~13 ~11 ~3 minecraft:air
setblock ~14 ~8 ~1 minecraft:polished_blackstone_brick_wall[north=low,east=none,south=none,west=none,up=true]
fill ~14 ~8 ~2 ~14 ~11 ~3 minecraft:air
fill ~15 ~8 ~1 ~15 ~11 ~3 minecraft:air
fill ~8 ~10 ~1 ~8 ~11 ~1 minecraft:air
fill ~10 ~10 ~1 ~12 ~11 ~1 minecraft:air
fill ~14 ~10 ~1 ~14 ~11 ~1 minecraft:air

# the shrine roof: deepslate tile courses stepping in to a ridge at y 15, gable ends to the front and back
fill ~6 ~12 ~0 ~6 ~12 ~4 minecraft:deepslate_tile_stairs[facing=east]
fill ~7 ~12 ~0 ~8 ~12 ~0 minecraft:polished_blackstone_bricks
fill ~7 ~12 ~1 ~15 ~12 ~3 minecraft:air
setblock ~7 ~12 ~4 minecraft:polished_blackstone_bricks
setblock ~8 ~12 ~4 minecraft:polished_blackstone
setblock ~9 ~12 ~0 minecraft:polished_blackstone
fill ~9 ~12 ~4 ~10 ~12 ~4 minecraft:polished_blackstone_bricks
setblock ~10 ~12 ~0 minecraft:cracked_polished_blackstone_bricks
setblock ~11 ~12 ~0 minecraft:polished_blackstone_bricks
setblock ~11 ~12 ~4 minecraft:cracked_polished_blackstone_bricks
fill ~12 ~12 ~0 ~14 ~12 ~0 minecraft:cracked_polished_blackstone_bricks
fill ~12 ~12 ~4 ~15 ~12 ~4 minecraft:polished_blackstone_bricks
setblock ~15 ~12 ~0 minecraft:polished_blackstone_bricks
fill ~16 ~12 ~0 ~16 ~12 ~4 minecraft:deepslate_tile_stairs[facing=west]
fill ~7 ~13 ~0 ~7 ~13 ~4 minecraft:deepslate_tile_stairs[facing=east]
fill ~8 ~13 ~0 ~10 ~13 ~0 minecraft:polished_blackstone_bricks
fill ~8 ~13 ~1 ~14 ~13 ~3 minecraft:air
fill ~8 ~13 ~4 ~9 ~13 ~4 minecraft:cracked_polished_blackstone_bricks
setblock ~10 ~13 ~4 minecraft:polished_blackstone_bricks
setblock ~11 ~13 ~0 minecraft:cracked_polished_blackstone_bricks
fill ~12 ~13 ~0 ~14 ~13 ~0 minecraft:polished_blackstone_bricks
setblock ~12 ~13 ~4 minecraft:polished_blackstone_bricks
setblock ~13 ~13 ~4 minecraft:polished_blackstone
setblock ~14 ~13 ~4 minecraft:cracked_polished_blackstone_bricks
fill ~15 ~13 ~0 ~15 ~13 ~4 minecraft:deepslate_tile_stairs[facing=west]
fill ~8 ~14 ~0 ~8 ~14 ~4 minecraft:deepslate_tile_stairs[facing=east]
setblock ~9 ~14 ~0 minecraft:polished_blackstone
fill ~9 ~14 ~1 ~13 ~14 ~3 minecraft:air
setblock ~9 ~14 ~4 minecraft:polished_blackstone_bricks
setblock ~10 ~14 ~0 minecraft:polished_blackstone_bricks
fill ~10 ~14 ~4 ~12 ~14 ~4 minecraft:cracked_polished_blackstone_bricks
setblock ~11 ~14 ~0 minecraft:polished_blackstone
fill ~12 ~14 ~0 ~13 ~14 ~0 minecraft:polished_blackstone_bricks
setblock ~13 ~14 ~4 minecraft:polished_blackstone_bricks
fill ~14 ~14 ~0 ~14 ~14 ~4 minecraft:deepslate_tile_stairs[facing=west]
fill ~9 ~15 ~0 ~9 ~15 ~4 minecraft:deepslate_tile_stairs[facing=east]
fill ~10 ~15 ~0 ~12 ~15 ~4 minecraft:deepslate_tiles
fill ~13 ~15 ~0 ~13 ~15 ~4 minecraft:deepslate_tile_stairs[facing=west]

# a round window of iron bars in the front gable, the door framed in gilded and chiseled blackstone
fill ~9 ~8 ~4 ~9 ~10 ~4 minecraft:polished_blackstone
fill ~13 ~8 ~4 ~13 ~10 ~4 minecraft:polished_blackstone
setblock ~9 ~11 ~4 minecraft:chiseled_polished_blackstone
setblock ~10 ~11 ~4 minecraft:polished_blackstone_bricks
setblock ~11 ~11 ~4 minecraft:gilded_blackstone
setblock ~12 ~11 ~4 minecraft:polished_blackstone_bricks
setblock ~13 ~11 ~4 minecraft:chiseled_polished_blackstone
setblock ~11 ~13 ~4 minecraft:iron_bars[north=false,east=true,south=false,west=true]

# soul lanterns on the stair cheeks before the shrine door
setblock ~9 ~9 ~5 minecraft:soul_lantern
setblock ~13 ~9 ~5 minecraft:soul_lantern

# the altar inside: a skull on the blackstone, candles, soul lanterns on posts either side
setblock ~8 ~9 ~1 minecraft:soul_lantern
setblock ~10 ~9 ~1 minecraft:candle[candles=3,lit=true]
setblock ~11 ~9 ~1 minecraft:skeleton_skull[rotation=0]
setblock ~12 ~9 ~1 minecraft:candle[candles=2,lit=true]
setblock ~14 ~9 ~1 minecraft:soul_lantern

# the colonnade: the back along z 17 (x 4..18) is a blackstone lintel at y 4 on piers (x 4, 7, 9, 13, 15, 18) with iron grilles between them (y 1..3), so the court and the graves show through; the arch at x 10..12 is open; end walls at x 4 and 18
fill ~4 ~1 ~17 ~4 ~4 ~17 minecraft:polished_blackstone
fill ~4 ~1 ~18 ~4 ~2 ~19 minecraft:polished_blackstone_bricks
fill ~5 ~1 ~17 ~6 ~3 ~17 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~7 ~1 ~17 ~7 ~3 ~17 minecraft:polished_blackstone
fill ~8 ~1 ~17 ~8 ~3 ~17 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~9 ~1 ~17 ~9 ~3 ~17 minecraft:polished_blackstone
fill ~13 ~1 ~17 ~13 ~3 ~17 minecraft:polished_blackstone
fill ~14 ~1 ~17 ~14 ~3 ~17 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~15 ~1 ~17 ~15 ~3 ~17 minecraft:polished_blackstone
fill ~16 ~1 ~17 ~17 ~3 ~17 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~18 ~1 ~17 ~18 ~4 ~17 minecraft:polished_blackstone
setblock ~18 ~1 ~18 minecraft:polished_blackstone_bricks
fill ~18 ~1 ~19 ~18 ~2 ~19 minecraft:polished_blackstone
setblock ~18 ~2 ~18 minecraft:cracked_polished_blackstone_bricks
setblock ~4 ~3 ~18 minecraft:polished_blackstone
setblock ~4 ~3 ~19 minecraft:cracked_polished_blackstone_bricks
fill ~18 ~3 ~18 ~18 ~4 ~19 minecraft:polished_blackstone_bricks
fill ~4 ~4 ~18 ~4 ~4 ~19 minecraft:polished_blackstone_bricks
setblock ~5 ~4 ~17 minecraft:cracked_polished_blackstone_bricks
fill ~6 ~4 ~17 ~7 ~4 ~17 minecraft:polished_blackstone_bricks
setblock ~8 ~4 ~17 minecraft:cracked_polished_blackstone_bricks
fill ~9 ~4 ~17 ~14 ~4 ~17 minecraft:polished_blackstone_bricks
setblock ~15 ~4 ~17 minecraft:cracked_polished_blackstone_bricks
setblock ~16 ~4 ~17 minecraft:polished_blackstone_bricks
setblock ~17 ~4 ~17 minecraft:cracked_polished_blackstone_bricks

# the piers along the front (z 20) at x 4, 9, 13 and 18: polished blackstone with chiseled capitals, a lintel at y 4
fill ~4 ~1 ~20 ~4 ~3 ~20 minecraft:polished_blackstone
fill ~9 ~1 ~20 ~9 ~3 ~20 minecraft:polished_blackstone
fill ~13 ~1 ~20 ~13 ~3 ~20 minecraft:polished_blackstone
fill ~18 ~1 ~20 ~18 ~3 ~20 minecraft:polished_blackstone
setblock ~4 ~4 ~20 minecraft:chiseled_polished_blackstone
fill ~5 ~4 ~20 ~8 ~4 ~20 minecraft:polished_blackstone_brick_slab[type=top]
setblock ~9 ~4 ~20 minecraft:chiseled_polished_blackstone
fill ~10 ~4 ~20 ~12 ~4 ~20 minecraft:polished_blackstone_brick_slab[type=top]
setblock ~13 ~4 ~20 minecraft:chiseled_polished_blackstone
fill ~14 ~4 ~20 ~17 ~4 ~20 minecraft:polished_blackstone_brick_slab[type=top]
setblock ~18 ~4 ~20 minecraft:chiseled_polished_blackstone

# the colonnade roof at y 5 (deepslate tiles) with tile stairs along its eaves
fill ~3 ~5 ~17 ~3 ~5 ~20 minecraft:deepslate_tile_stairs[facing=east,half=bottom]
fill ~3 ~5 ~21 ~19 ~5 ~21 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~4 ~5 ~17 ~18 ~5 ~20 minecraft:deepslate_tiles
fill ~19 ~5 ~17 ~19 ~5 ~20 minecraft:deepslate_tile_stairs[facing=west,half=bottom]

# soul lanterns hung on chains between the piers, over the slots
setblock ~7 ~3 ~19 minecraft:soul_lantern[hanging=true]
setblock ~11 ~3 ~19 minecraft:soul_lantern[hanging=true]
setblock ~15 ~3 ~19 minecraft:soul_lantern[hanging=true]
setblock ~7 ~4 ~19 minecraft:chain[axis=y]
setblock ~11 ~4 ~19 minecraft:chain[axis=y]
setblock ~15 ~4 ~19 minecraft:chain[axis=y]

# the low front wall (z 22) of mossy stone with the gateway at x 8..14 and gate piers with soul lanterns
setblock ~0 ~1 ~22 minecraft:stone_brick_wall[north=none,east=low,south=none,west=none,up=true]
setblock ~1 ~1 ~22 minecraft:mossy_stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~2 ~1 ~22 minecraft:stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
fill ~3 ~1 ~22 ~4 ~1 ~22 minecraft:mossy_stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
fill ~5 ~1 ~22 ~6 ~1 ~22 minecraft:stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~7 ~1 ~22 minecraft:stone_bricks
setblock ~15 ~1 ~22 minecraft:stone_bricks
setblock ~16 ~1 ~22 minecraft:mossy_stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~17 ~1 ~22 minecraft:stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~18 ~1 ~22 minecraft:mossy_cobblestone_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~19 ~1 ~22 minecraft:mossy_stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~20 ~1 ~22 minecraft:stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~21 ~1 ~22 minecraft:mossy_cobblestone_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~22 ~1 ~22 minecraft:mossy_cobblestone_wall[north=none,east=none,south=none,west=low,up=true]
setblock ~7 ~2 ~22 minecraft:chiseled_stone_bricks
setblock ~15 ~2 ~22 minecraft:chiseled_stone_bricks
setblock ~7 ~3 ~22 minecraft:soul_lantern
setblock ~15 ~3 ~22 minecraft:soul_lantern

# the graves: headstones (walls, slabs and chiseled stone) at the head of a mound, in rows on every terrace and in the court corners
setblock ~5 ~1 ~13 minecraft:deepslate_tile_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~7 ~1 ~13 minecraft:cobbled_deepslate_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~15 ~1 ~13 minecraft:chiseled_stone_bricks
setblock ~17 ~1 ~13 minecraft:chiseled_stone_bricks
setblock ~2 ~3 ~9 minecraft:mossy_cobblestone_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~4 ~3 ~9 minecraft:mossy_stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~6 ~3 ~9 minecraft:deepslate_tile_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~16 ~3 ~9 minecraft:chiseled_stone_bricks
setblock ~18 ~3 ~9 minecraft:andesite_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~20 ~3 ~9 minecraft:andesite_wall[north=none,east=none,south=low,west=none,up=true]
setblock ~16 ~4 ~9 minecraft:candle[candles=1,lit=true]
setblock ~2 ~5 ~6 minecraft:mossy_stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~4 ~5 ~6 minecraft:stone_brick_stairs[facing=south,half=top]
setblock ~6 ~5 ~6 minecraft:cobbled_deepslate_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~16 ~5 ~6 minecraft:andesite_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~18 ~5 ~6 minecraft:cobbled_deepslate_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~20 ~5 ~6 minecraft:chiseled_stone_bricks
setblock ~20 ~6 ~6 minecraft:candle[candles=1,lit=true]

# a stone sarcophagus in each court corner
fill ~2 ~1 ~13 ~2 ~1 ~14 minecraft:polished_deepslate
fill ~20 ~1 ~13 ~20 ~1 ~14 minecraft:polished_deepslate
fill ~2 ~2 ~13 ~2 ~2 ~14 minecraft:polished_deepslate_slab[type=bottom]
fill ~20 ~2 ~13 ~20 ~2 ~14 minecraft:polished_deepslate_slab[type=bottom]

# soul lanterns on dark oak posts on the terraces
fill ~3 ~3 ~9 ~3 ~4 ~9 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~19 ~3 ~9 ~19 ~4 ~9 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~3 ~5 ~9 minecraft:soul_lantern
fill ~5 ~5 ~6 ~5 ~6 ~6 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~17 ~5 ~6 ~17 ~6 ~6 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~19 ~5 ~9 minecraft:soul_lantern
setblock ~5 ~7 ~6 minecraft:soul_lantern
fill ~17 ~7 ~3 ~17 ~8 ~3 minecraft:dark_oak_fence[north=false,east=false,south=false,west=true]
setblock ~17 ~7 ~6 minecraft:soul_lantern
setblock ~17 ~9 ~3 minecraft:soul_lantern

# a dead tree on the west mound and one on the east terrace: dark oak logs without leaves, roots in the soil
fill ~20 ~3 ~10 ~20 ~6 ~10 minecraft:dark_oak_log
setblock ~19 ~5 ~10 minecraft:dark_oak_log[axis=x]
setblock ~20 ~6 ~9 minecraft:dark_oak_log[axis=z]
setblock ~21 ~6 ~10 minecraft:dark_oak_log[axis=x]
fill ~3 ~8 ~2 ~3 ~11 ~2 minecraft:dark_oak_log
setblock ~2 ~10 ~2 minecraft:dark_oak_log[axis=x]
setblock ~4 ~10 ~2 minecraft:dark_oak_log[axis=x]
setblock ~3 ~11 ~3 minecraft:dark_oak_log[axis=z]
setblock ~5 ~11 ~2 minecraft:dark_oak_log[axis=x]

# ferns, dead bushes and moss on the terraces
setblock ~4 ~1 ~12 minecraft:fern
setblock ~8 ~1 ~12 minecraft:dead_bush
setblock ~13 ~1 ~13 minecraft:fern
setblock ~17 ~1 ~12 minecraft:fern
setblock ~18 ~1 ~13 minecraft:dead_bush
setblock ~3 ~3 ~8 minecraft:fern
setblock ~5 ~3 ~9 minecraft:dead_bush
fill ~6 ~3 ~10 ~7 ~3 ~10 minecraft:fern
setblock ~16 ~3 ~10 minecraft:dead_bush
setblock ~21 ~3 ~8 minecraft:fern
setblock ~8 ~5 ~5 minecraft:fern
setblock ~14 ~5 ~5 minecraft:dead_bush
setblock ~19 ~5 ~5 minecraft:fern
setblock ~21 ~5 ~5 minecraft:dead_bush
setblock ~1 ~7 ~3 minecraft:dead_bush
setblock ~18 ~7 ~3 minecraft:fern
setblock ~4 ~8 ~2 minecraft:dead_bush
setblock ~18 ~8 ~2 minecraft:fern
setblock ~19 ~8 ~2 minecraft:dead_bush
setblock ~5 ~9 ~1 minecraft:dead_bush
setblock ~4 ~10 ~1 minecraft:dead_bush
setblock ~20 ~10 ~1 minecraft:dead_bush

# the Graveborn banner on the back wall of the colonnade over the arch
setblock ~11 ~4 ~18 minecraft:black_wall_banner[facing=south]{Patterns:[{Pattern:"gru",Color:13},{Pattern:"sku",Color:0},{Pattern:"bo",Color:15}]}
