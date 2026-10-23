# size 41 6 33
# camp/parade: the parade ground: avenue from the gates to the standard, cross way to the side pieces, the standard and the spawn plinth.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Avenue x 17..23 (dx -3..3) meets the gate arch; the forks at z 32 meet the pads (x 9..12 and 28..31); the cross way z 13..15 leads to the yard and the archive.
# Standard: 3x3 polished blackstone at (20, 0, 2), pole y 1..4, banner y 5. Spawn plinth: flush polished andesite 3x3 at (20, 0, 26).

# the drill field: one surface of packed mud, with a one-block worn band of coarse dirt where it meets the grass
fill ~0 ~0 ~2 ~0 ~0 ~12 minecraft:coarse_dirt
fill ~0 ~0 ~16 ~0 ~0 ~30 minecraft:coarse_dirt
setblock ~1 ~0 ~1 minecraft:coarse_dirt
fill ~1 ~0 ~2 ~1 ~0 ~12 minecraft:packed_mud
fill ~1 ~0 ~16 ~16 ~0 ~23 minecraft:packed_mud
fill ~1 ~0 ~24 ~15 ~0 ~24 minecraft:packed_mud
fill ~1 ~0 ~25 ~14 ~0 ~25 minecraft:packed_mud
fill ~1 ~0 ~26 ~13 ~0 ~26 minecraft:packed_mud
fill ~1 ~0 ~27 ~12 ~0 ~27 minecraft:packed_mud
fill ~1 ~0 ~28 ~11 ~0 ~28 minecraft:packed_mud
fill ~1 ~0 ~29 ~10 ~0 ~29 minecraft:packed_mud
fill ~1 ~0 ~30 ~9 ~0 ~30 minecraft:packed_mud
setblock ~1 ~0 ~31 minecraft:coarse_dirt
setblock ~2 ~0 ~0 minecraft:coarse_dirt
fill ~2 ~0 ~1 ~3 ~0 ~12 minecraft:packed_mud
fill ~2 ~0 ~31 ~8 ~0 ~32 minecraft:packed_mud
fill ~4 ~0 ~2 ~4 ~0 ~12 minecraft:packed_mud
fill ~5 ~0 ~3 ~6 ~0 ~12 minecraft:packed_mud
fill ~6 ~0 ~0 ~13 ~0 ~0 minecraft:coarse_dirt
fill ~7 ~0 ~1 ~13 ~0 ~1 minecraft:packed_mud
fill ~7 ~0 ~4 ~7 ~0 ~12 minecraft:packed_mud
fill ~8 ~0 ~2 ~13 ~0 ~2 minecraft:packed_mud
fill ~8 ~0 ~5 ~9 ~0 ~12 minecraft:packed_mud
fill ~10 ~0 ~3 ~13 ~0 ~3 minecraft:packed_mud
fill ~10 ~0 ~6 ~10 ~0 ~12 minecraft:packed_mud
fill ~11 ~0 ~4 ~13 ~0 ~4 minecraft:packed_mud
fill ~11 ~0 ~7 ~12 ~0 ~12 minecraft:packed_mud
fill ~13 ~0 ~5 ~14 ~0 ~5 minecraft:packed_mud
fill ~13 ~0 ~8 ~16 ~0 ~12 minecraft:packed_mud
fill ~13 ~0 ~31 ~13 ~0 ~32 minecraft:packed_mud
setblock ~14 ~0 ~6 minecraft:packed_mud
fill ~14 ~0 ~30 ~14 ~0 ~32 minecraft:packed_mud
fill ~15 ~0 ~29 ~15 ~0 ~32 minecraft:packed_mud
fill ~16 ~0 ~28 ~16 ~0 ~32 minecraft:packed_mud
fill ~24 ~0 ~8 ~27 ~0 ~12 minecraft:packed_mud
fill ~24 ~0 ~16 ~39 ~0 ~23 minecraft:packed_mud
fill ~24 ~0 ~29 ~24 ~0 ~32 minecraft:packed_mud
fill ~25 ~0 ~24 ~39 ~0 ~25 minecraft:packed_mud
fill ~25 ~0 ~30 ~25 ~0 ~32 minecraft:packed_mud
setblock ~26 ~0 ~5 minecraft:packed_mud
fill ~26 ~0 ~26 ~39 ~0 ~26 minecraft:packed_mud
fill ~26 ~0 ~31 ~26 ~0 ~32 minecraft:packed_mud
fill ~27 ~0 ~0 ~34 ~0 ~0 minecraft:coarse_dirt
fill ~27 ~0 ~1 ~32 ~0 ~1 minecraft:packed_mud
fill ~27 ~0 ~2 ~31 ~0 ~2 minecraft:packed_mud
fill ~27 ~0 ~3 ~29 ~0 ~3 minecraft:packed_mud
fill ~27 ~0 ~4 ~28 ~0 ~4 minecraft:packed_mud
fill ~27 ~0 ~27 ~39 ~0 ~27 minecraft:packed_mud
setblock ~27 ~0 ~32 minecraft:packed_mud
fill ~28 ~0 ~7 ~28 ~0 ~12 minecraft:packed_mud
fill ~28 ~0 ~28 ~39 ~0 ~28 minecraft:packed_mud
fill ~29 ~0 ~6 ~29 ~0 ~12 minecraft:packed_mud
fill ~29 ~0 ~29 ~39 ~0 ~29 minecraft:packed_mud
fill ~30 ~0 ~5 ~31 ~0 ~12 minecraft:packed_mud
fill ~30 ~0 ~30 ~39 ~0 ~30 minecraft:packed_mud
fill ~31 ~0 ~31 ~38 ~0 ~31 minecraft:packed_mud
fill ~32 ~0 ~4 ~32 ~0 ~12 minecraft:packed_mud
fill ~32 ~0 ~32 ~38 ~0 ~32 minecraft:packed_mud
fill ~33 ~0 ~3 ~34 ~0 ~12 minecraft:packed_mud
fill ~35 ~0 ~2 ~35 ~0 ~12 minecraft:packed_mud
fill ~36 ~0 ~1 ~38 ~0 ~12 minecraft:packed_mud
setblock ~38 ~0 ~0 minecraft:coarse_dirt
setblock ~39 ~0 ~1 minecraft:coarse_dirt
fill ~39 ~0 ~2 ~39 ~0 ~12 minecraft:packed_mud
setblock ~39 ~0 ~31 minecraft:coarse_dirt
fill ~40 ~0 ~2 ~40 ~0 ~12 minecraft:coarse_dirt
fill ~40 ~0 ~16 ~40 ~0 ~30 minecraft:coarse_dirt

# the ways: stone bricks, worn and mossy: the forks to the pads, the cross way, the north ways, the plaza
fill ~0 ~0 ~13 ~1 ~0 ~15 minecraft:stone_bricks
setblock ~2 ~0 ~13 minecraft:cracked_stone_bricks
fill ~2 ~0 ~14 ~4 ~0 ~15 minecraft:stone_bricks
setblock ~3 ~0 ~0 minecraft:stone_bricks
fill ~3 ~0 ~13 ~9 ~0 ~13 minecraft:stone_bricks
setblock ~4 ~0 ~0 minecraft:cracked_stone_bricks
fill ~4 ~0 ~1 ~6 ~0 ~1 minecraft:stone_bricks
setblock ~5 ~0 ~0 minecraft:mossy_stone_bricks
fill ~5 ~0 ~2 ~6 ~0 ~2 minecraft:mossy_stone_bricks
fill ~5 ~0 ~14 ~6 ~0 ~14 minecraft:cracked_stone_bricks
fill ~5 ~0 ~15 ~7 ~0 ~15 minecraft:stone_bricks
setblock ~7 ~0 ~2 minecraft:stone_bricks
setblock ~7 ~0 ~3 minecraft:cracked_stone_bricks
fill ~7 ~0 ~14 ~12 ~0 ~14 minecraft:stone_bricks
fill ~8 ~0 ~3 ~9 ~0 ~3 minecraft:stone_bricks
setblock ~8 ~0 ~4 minecraft:stone_bricks
setblock ~8 ~0 ~15 minecraft:mossy_stone_bricks
setblock ~9 ~0 ~4 minecraft:cracked_stone_bricks
fill ~9 ~0 ~15 ~13 ~0 ~15 minecraft:stone_bricks
setblock ~9 ~0 ~31 minecraft:stone_bricks
setblock ~9 ~0 ~32 minecraft:mossy_stone_bricks
fill ~10 ~0 ~4 ~10 ~0 ~5 minecraft:stone_bricks
setblock ~10 ~0 ~13 minecraft:mossy_stone_bricks
setblock ~10 ~0 ~30 minecraft:stone_bricks
setblock ~10 ~0 ~31 minecraft:cobblestone
setblock ~10 ~0 ~32 minecraft:stone_bricks
fill ~11 ~0 ~5 ~12 ~0 ~6 minecraft:stone_bricks
fill ~11 ~0 ~13 ~12 ~0 ~13 minecraft:cracked_stone_bricks
fill ~11 ~0 ~29 ~11 ~0 ~30 minecraft:stone_bricks
setblock ~11 ~0 ~31 minecraft:mossy_stone_bricks
setblock ~11 ~0 ~32 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~28 minecraft:cracked_stone_bricks
setblock ~12 ~0 ~29 minecraft:mossy_stone_bricks
fill ~12 ~0 ~30 ~13 ~0 ~30 minecraft:stone_bricks
fill ~12 ~0 ~31 ~12 ~0 ~32 minecraft:stone_bricks
fill ~13 ~0 ~6 ~13 ~0 ~7 minecraft:stone_bricks
fill ~13 ~0 ~13 ~16 ~0 ~13 minecraft:stone_bricks
setblock ~13 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~13 ~0 ~27 minecraft:stone_bricks
fill ~13 ~0 ~28 ~13 ~0 ~29 minecraft:cobblestone
setblock ~14 ~0 ~7 minecraft:mossy_stone_bricks
setblock ~14 ~0 ~14 minecraft:stone_bricks
setblock ~14 ~0 ~15 minecraft:cracked_stone_bricks
fill ~14 ~0 ~26 ~14 ~0 ~29 minecraft:stone_bricks
setblock ~15 ~0 ~0 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~1 minecraft:stone_bricks
fill ~15 ~0 ~3 ~16 ~0 ~4 minecraft:stone_bricks
setblock ~15 ~0 ~7 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~15 minecraft:mossy_stone_bricks
setblock ~15 ~0 ~25 minecraft:mossy_stone_bricks
setblock ~15 ~0 ~26 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~27 minecraft:stone_bricks
setblock ~15 ~0 ~28 minecraft:cracked_stone_bricks
setblock ~16 ~0 ~0 minecraft:stone_bricks
setblock ~16 ~0 ~1 minecraft:cobblestone
setblock ~16 ~0 ~2 minecraft:mossy_stone_bricks
setblock ~16 ~0 ~5 minecraft:stone_bricks
fill ~16 ~0 ~14 ~16 ~0 ~15 minecraft:stone_bricks
fill ~16 ~0 ~24 ~16 ~0 ~26 minecraft:stone_bricks
setblock ~16 ~0 ~27 minecraft:cracked_stone_bricks
fill ~24 ~0 ~0 ~25 ~0 ~0 minecraft:stone_bricks
fill ~24 ~0 ~1 ~24 ~0 ~2 minecraft:cobblestone
fill ~24 ~0 ~3 ~24 ~0 ~5 minecraft:stone_bricks
fill ~24 ~0 ~13 ~25 ~0 ~13 minecraft:mossy_stone_bricks
fill ~24 ~0 ~14 ~25 ~0 ~14 minecraft:stone_bricks
setblock ~24 ~0 ~15 minecraft:stone_bricks
setblock ~24 ~0 ~24 minecraft:cracked_stone_bricks
fill ~24 ~0 ~25 ~24 ~0 ~28 minecraft:stone_bricks
setblock ~25 ~0 ~1 minecraft:mossy_stone_bricks
setblock ~25 ~0 ~3 minecraft:mossy_stone_bricks
setblock ~25 ~0 ~4 minecraft:cracked_stone_bricks
setblock ~25 ~0 ~7 minecraft:cracked_stone_bricks
setblock ~25 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~25 ~0 ~26 minecraft:stone_bricks
fill ~25 ~0 ~27 ~26 ~0 ~27 minecraft:cracked_stone_bricks
fill ~25 ~0 ~28 ~26 ~0 ~28 minecraft:stone_bricks
setblock ~25 ~0 ~29 minecraft:stone_bricks
setblock ~26 ~0 ~6 minecraft:cobblestone
setblock ~26 ~0 ~7 minecraft:stone_bricks
fill ~26 ~0 ~13 ~29 ~0 ~13 minecraft:stone_bricks
setblock ~26 ~0 ~14 minecraft:cobblestone
setblock ~26 ~0 ~15 minecraft:stone_bricks
setblock ~26 ~0 ~29 minecraft:cobblestone
fill ~26 ~0 ~30 ~27 ~0 ~30 minecraft:mossy_stone_bricks
setblock ~27 ~0 ~5 minecraft:stone_bricks
setblock ~27 ~0 ~6 minecraft:mossy_stone_bricks
setblock ~27 ~0 ~7 minecraft:cracked_stone_bricks
fill ~27 ~0 ~14 ~27 ~0 ~15 minecraft:stone_bricks
setblock ~27 ~0 ~28 minecraft:cracked_stone_bricks
fill ~27 ~0 ~29 ~28 ~0 ~29 minecraft:mossy_stone_bricks
setblock ~27 ~0 ~31 minecraft:stone_bricks
setblock ~28 ~0 ~5 minecraft:cobblestone
setblock ~28 ~0 ~6 minecraft:stone_bricks
setblock ~28 ~0 ~14 minecraft:mossy_stone_bricks
fill ~28 ~0 ~15 ~29 ~0 ~15 minecraft:cracked_stone_bricks
fill ~28 ~0 ~30 ~28 ~0 ~31 minecraft:stone_bricks
setblock ~28 ~0 ~32 minecraft:cracked_stone_bricks
fill ~29 ~0 ~4 ~29 ~0 ~5 minecraft:stone_bricks
setblock ~29 ~0 ~14 minecraft:stone_bricks
setblock ~29 ~0 ~30 minecraft:cobblestone
fill ~29 ~0 ~31 ~30 ~0 ~31 minecraft:stone_bricks
setblock ~29 ~0 ~32 minecraft:mossy_stone_bricks
fill ~30 ~0 ~3 ~32 ~0 ~3 minecraft:stone_bricks
fill ~30 ~0 ~4 ~31 ~0 ~4 minecraft:mossy_stone_bricks
fill ~30 ~0 ~13 ~30 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~30 ~0 ~15 minecraft:stone_bricks
fill ~30 ~0 ~32 ~31 ~0 ~32 minecraft:stone_bricks
fill ~31 ~0 ~13 ~33 ~0 ~13 minecraft:stone_bricks
setblock ~31 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~31 ~0 ~15 minecraft:mossy_stone_bricks
setblock ~32 ~0 ~2 minecraft:mossy_stone_bricks
fill ~32 ~0 ~14 ~34 ~0 ~14 minecraft:stone_bricks
setblock ~32 ~0 ~15 minecraft:cobblestone
fill ~33 ~0 ~1 ~35 ~0 ~1 minecraft:stone_bricks
setblock ~33 ~0 ~2 minecraft:cobblestone
setblock ~33 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~34 ~0 ~2 minecraft:stone_bricks
fill ~34 ~0 ~13 ~35 ~0 ~13 minecraft:mossy_stone_bricks
setblock ~34 ~0 ~15 minecraft:stone_bricks
fill ~35 ~0 ~0 ~37 ~0 ~0 minecraft:stone_bricks
fill ~35 ~0 ~14 ~35 ~0 ~15 minecraft:cobblestone
fill ~36 ~0 ~13 ~36 ~0 ~15 minecraft:cracked_stone_bricks
fill ~37 ~0 ~13 ~37 ~0 ~14 minecraft:stone_bricks
setblock ~37 ~0 ~15 minecraft:mossy_stone_bricks
fill ~38 ~0 ~13 ~39 ~0 ~13 minecraft:cracked_stone_bricks
fill ~38 ~0 ~14 ~38 ~0 ~15 minecraft:stone_bricks
setblock ~39 ~0 ~14 minecraft:cracked_stone_bricks
fill ~39 ~0 ~15 ~40 ~0 ~15 minecraft:stone_bricks
setblock ~40 ~0 ~13 minecraft:stone_bricks
setblock ~40 ~0 ~14 minecraft:cobblestone

# the avenue: stone bricks between polished andesite kerbs, the full depth
setblock ~17 ~0 ~2 minecraft:polished_andesite
fill ~17 ~0 ~5 ~17 ~0 ~6 minecraft:polished_andesite
fill ~17 ~0 ~8 ~17 ~0 ~12 minecraft:polished_andesite
setblock ~17 ~0 ~13 minecraft:stone_bricks
setblock ~17 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~17 ~0 ~15 minecraft:stone_bricks
fill ~17 ~0 ~16 ~17 ~0 ~32 minecraft:polished_andesite
fill ~18 ~0 ~0 ~18 ~0 ~2 minecraft:stone_bricks
setblock ~18 ~0 ~3 minecraft:mossy_stone_bricks
setblock ~18 ~0 ~4 minecraft:stone_bricks
fill ~18 ~0 ~6 ~20 ~0 ~7 minecraft:stone_bricks
setblock ~18 ~0 ~9 minecraft:mossy_stone_bricks
setblock ~18 ~0 ~10 minecraft:stone_bricks
setblock ~18 ~0 ~11 minecraft:mossy_stone_bricks
fill ~18 ~0 ~12 ~18 ~0 ~15 minecraft:stone_bricks
setblock ~18 ~0 ~16 minecraft:mossy_stone_bricks
setblock ~18 ~0 ~17 minecraft:cracked_stone_bricks
setblock ~18 ~0 ~18 minecraft:stone_bricks
setblock ~18 ~0 ~19 minecraft:mossy_stone_bricks
fill ~18 ~0 ~20 ~18 ~0 ~21 minecraft:stone_bricks
setblock ~18 ~0 ~22 minecraft:mossy_stone_bricks
setblock ~18 ~0 ~23 minecraft:stone_bricks
setblock ~18 ~0 ~29 minecraft:stone_bricks
setblock ~18 ~0 ~30 minecraft:cracked_stone_bricks
setblock ~18 ~0 ~31 minecraft:mossy_stone_bricks
setblock ~18 ~0 ~32 minecraft:stone_bricks
setblock ~19 ~0 ~0 minecraft:cobblestone
fill ~19 ~0 ~4 ~20 ~0 ~4 minecraft:cracked_stone_bricks
fill ~19 ~0 ~9 ~22 ~0 ~10 minecraft:stone_bricks
fill ~19 ~0 ~11 ~20 ~0 ~11 minecraft:stone_bricks
fill ~19 ~0 ~12 ~19 ~0 ~13 minecraft:mossy_stone_bricks
fill ~19 ~0 ~14 ~20 ~0 ~14 minecraft:cracked_stone_bricks
fill ~19 ~0 ~15 ~20 ~0 ~15 minecraft:stone_bricks
setblock ~19 ~0 ~16 minecraft:cobblestone
setblock ~19 ~0 ~17 minecraft:stone_bricks
setblock ~19 ~0 ~18 minecraft:cracked_stone_bricks
fill ~19 ~0 ~19 ~19 ~0 ~21 minecraft:stone_bricks
fill ~19 ~0 ~22 ~20 ~0 ~22 minecraft:cobblestone
setblock ~19 ~0 ~23 minecraft:cracked_stone_bricks
fill ~19 ~0 ~29 ~19 ~0 ~30 minecraft:cracked_stone_bricks
fill ~19 ~0 ~31 ~20 ~0 ~32 minecraft:stone_bricks
fill ~20 ~0 ~0 ~22 ~0 ~0 minecraft:stone_bricks
setblock ~20 ~0 ~5 minecraft:cracked_stone_bricks
fill ~20 ~0 ~12 ~20 ~0 ~13 minecraft:stone_bricks
fill ~20 ~0 ~16 ~20 ~0 ~18 minecraft:stone_bricks
setblock ~20 ~0 ~19 minecraft:cracked_stone_bricks
fill ~20 ~0 ~20 ~20 ~0 ~21 minecraft:stone_bricks
setblock ~20 ~0 ~23 minecraft:stone_bricks
fill ~20 ~0 ~29 ~20 ~0 ~30 minecraft:mossy_stone_bricks
setblock ~21 ~0 ~4 minecraft:mossy_stone_bricks
setblock ~21 ~0 ~6 minecraft:cracked_stone_bricks
fill ~21 ~0 ~7 ~22 ~0 ~7 minecraft:mossy_stone_bricks
fill ~21 ~0 ~11 ~21 ~0 ~13 minecraft:mossy_stone_bricks
fill ~21 ~0 ~14 ~23 ~0 ~14 minecraft:stone_bricks
setblock ~21 ~0 ~15 minecraft:mossy_stone_bricks
setblock ~21 ~0 ~16 minecraft:cracked_stone_bricks
fill ~21 ~0 ~17 ~22 ~0 ~20 minecraft:stone_bricks
fill ~21 ~0 ~21 ~21 ~0 ~23 minecraft:stone_bricks
fill ~21 ~0 ~29 ~22 ~0 ~29 minecraft:stone_bricks
fill ~21 ~0 ~30 ~22 ~0 ~30 minecraft:cracked_stone_bricks
setblock ~21 ~0 ~31 minecraft:cracked_stone_bricks
setblock ~21 ~0 ~32 minecraft:stone_bricks
fill ~22 ~0 ~1 ~22 ~0 ~3 minecraft:stone_bricks
setblock ~22 ~0 ~4 minecraft:cracked_stone_bricks
setblock ~22 ~0 ~6 minecraft:stone_bricks
setblock ~22 ~0 ~11 minecraft:stone_bricks
fill ~22 ~0 ~12 ~22 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~22 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~22 ~0 ~16 minecraft:cobblestone
setblock ~22 ~0 ~21 minecraft:cracked_stone_bricks
fill ~22 ~0 ~22 ~22 ~0 ~23 minecraft:stone_bricks
fill ~22 ~0 ~31 ~22 ~0 ~32 minecraft:stone_bricks
setblock ~23 ~0 ~2 minecraft:polished_andesite
fill ~23 ~0 ~5 ~23 ~0 ~6 minecraft:polished_andesite
fill ~23 ~0 ~8 ~23 ~0 ~12 minecraft:polished_andesite
setblock ~23 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~23 ~0 ~15 minecraft:stone_bricks
fill ~23 ~0 ~16 ~23 ~0 ~32 minecraft:polished_andesite

# the plaza ring around the standard
fill ~14 ~0 ~0 ~14 ~0 ~4 minecraft:polished_andesite
fill ~15 ~0 ~5 ~15 ~0 ~6 minecraft:polished_andesite
fill ~16 ~0 ~6 ~16 ~0 ~7 minecraft:polished_andesite
fill ~17 ~0 ~0 ~17 ~0 ~1 minecraft:mossy_stone_bricks
fill ~17 ~0 ~3 ~17 ~0 ~4 minecraft:mossy_stone_bricks
setblock ~17 ~0 ~7 minecraft:polished_andesite
fill ~18 ~0 ~5 ~19 ~0 ~5 minecraft:mossy_stone_bricks
fill ~18 ~0 ~8 ~22 ~0 ~8 minecraft:polished_andesite
fill ~21 ~0 ~5 ~22 ~0 ~5 minecraft:mossy_stone_bricks
fill ~23 ~0 ~0 ~23 ~0 ~1 minecraft:mossy_stone_bricks
fill ~23 ~0 ~3 ~23 ~0 ~4 minecraft:mossy_stone_bricks
setblock ~23 ~0 ~7 minecraft:polished_andesite
fill ~24 ~0 ~6 ~24 ~0 ~7 minecraft:polished_andesite
fill ~25 ~0 ~5 ~25 ~0 ~6 minecraft:polished_andesite
fill ~26 ~0 ~0 ~26 ~0 ~4 minecraft:polished_andesite

# the spawn medallion: a chiseled ring around the flush plinth
fill ~18 ~0 ~24 ~22 ~0 ~24 minecraft:chiseled_stone_bricks
fill ~18 ~0 ~25 ~18 ~0 ~28 minecraft:chiseled_stone_bricks
fill ~19 ~0 ~28 ~21 ~0 ~28 minecraft:chiseled_stone_bricks
fill ~22 ~0 ~25 ~22 ~0 ~28 minecraft:chiseled_stone_bricks

# the spawn plinth: flush 3x3 polished andesite centred on (20, 0, 26)
fill ~19 ~0 ~25 ~21 ~0 ~27 minecraft:polished_andesite

# the standard plinth: 3x3 polished blackstone at (20, 0, 2), stepped up around the pole
fill ~19 ~0 ~1 ~21 ~0 ~3 minecraft:polished_blackstone
fill ~19 ~1 ~1 ~19 ~1 ~3 minecraft:polished_blackstone_stairs[facing=east]
setblock ~20 ~1 ~1 minecraft:polished_blackstone_stairs[facing=south]
setblock ~20 ~1 ~3 minecraft:polished_blackstone_stairs[facing=north]
fill ~21 ~1 ~1 ~21 ~1 ~3 minecraft:polished_blackstone_stairs[facing=west]

# the parade standard: dark oak fence pole, y 1..4
fill ~20 ~1 ~2 ~20 ~4 ~2 minecraft:dark_oak_fence

# braziers flanking the standard: blackstone wall posts
setblock ~15 ~0 ~2 minecraft:polished_blackstone
setblock ~25 ~0 ~2 minecraft:polished_blackstone
setblock ~15 ~1 ~2 minecraft:polished_blackstone_wall
setblock ~25 ~1 ~2 minecraft:polished_blackstone_wall

# banner poles along the avenue (dark oak fence, y 1..3)
fill ~16 ~1 ~8 ~16 ~3 ~8 minecraft:dark_oak_fence
fill ~16 ~1 ~18 ~16 ~3 ~18 minecraft:dark_oak_fence
fill ~24 ~1 ~8 ~24 ~3 ~8 minecraft:dark_oak_fence
fill ~24 ~1 ~18 ~24 ~3 ~18 minecraft:dark_oak_fence

# lantern posts at the crossing (dark oak fence, lantern)
fill ~16 ~1 ~12 ~16 ~2 ~12 minecraft:dark_oak_fence
fill ~16 ~1 ~16 ~16 ~2 ~16 minecraft:dark_oak_fence
fill ~24 ~1 ~12 ~24 ~2 ~12 minecraft:dark_oak_fence
fill ~24 ~1 ~16 ~24 ~2 ~16 minecraft:dark_oak_fence
setblock ~16 ~3 ~12 minecraft:lantern
setblock ~16 ~3 ~16 minecraft:lantern
setblock ~24 ~3 ~12 minecraft:lantern
setblock ~24 ~3 ~16 minecraft:lantern

# drill dummies on the north field, two each side of the avenue: a post, a bound straw body (dried kelp) and a skull for a head
setblock ~6 ~1 ~10 minecraft:dark_oak_fence
setblock ~9 ~1 ~10 minecraft:dark_oak_fence
setblock ~31 ~1 ~10 minecraft:dark_oak_fence
setblock ~34 ~1 ~10 minecraft:dark_oak_fence
setblock ~6 ~2 ~10 minecraft:dried_kelp_block
setblock ~9 ~2 ~10 minecraft:dried_kelp_block
setblock ~31 ~2 ~10 minecraft:dried_kelp_block
setblock ~34 ~2 ~10 minecraft:dried_kelp_block

# the dummies' heads
setblock ~6 ~3 ~10 minecraft:skeleton_skull[rotation=15]
setblock ~9 ~3 ~10 minecraft:skeleton_skull[rotation=0]
setblock ~31 ~3 ~10 minecraft:skeleton_skull[rotation=0]
setblock ~34 ~3 ~10 minecraft:skeleton_skull[rotation=1]

# the Steward's post by the east fork: a map table and a barrel (he stands at x 24.5, z 30.5)
setblock ~25 ~1 ~30 minecraft:cartography_table
setblock ~25 ~1 ~31 minecraft:barrel[facing=up]

# campfires on the braziers
setblock ~15 ~2 ~2 minecraft:campfire[lit=true]
setblock ~25 ~2 ~2 minecraft:campfire[lit=true]

# banners: the camp standard on its pole, the avenue banners
setblock ~16 ~4 ~8 minecraft:black_banner[rotation=0]{Patterns:[{Pattern:"cs",Color:7},{Pattern:"bo",Color:4}]}
setblock ~16 ~4 ~18 minecraft:black_banner[rotation=0]{Patterns:[{Pattern:"cs",Color:7},{Pattern:"bo",Color:4}]}
setblock ~24 ~4 ~8 minecraft:black_banner[rotation=0]{Patterns:[{Pattern:"cs",Color:7},{Pattern:"bo",Color:4}]}
setblock ~24 ~4 ~18 minecraft:black_banner[rotation=0]{Patterns:[{Pattern:"cs",Color:7},{Pattern:"bo",Color:4}]}
setblock ~20 ~5 ~2 minecraft:black_banner[rotation=0]{Patterns:[{Pattern:"bo",Color:4},{Pattern:"mc",Color:4}]}
