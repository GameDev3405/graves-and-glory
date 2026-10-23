# size 19 13 15
# kit/customs: toll house: a stone ground storey with a covered arch through it, a timber upper storey bridging the arch, a deepslate roof, a counterweighted toll bar, toll board, hanging sign and bracket lanterns at the road-side mouth, the arms of the crown on both gables.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Body x 1..17, z 2..12. The arch passage x 8..12 runs through from z 0 to z 14, open from y 1 to 4 under the arch (z 2..12); the toll bar crosses its north mouth at z 0, y 3, so the way is clear to y 2 there. Side doors into the toll room (x 7, z 7) and the guard room (x 13, z 7) open off it.
# The ground layer is the plinth of the rooms and the paving of the passage (x 8..12, z 1..13): every other cell of y 0 is structure void, so the ground of the map runs up to the walls.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~5 ~1 ~0 minecraft:air
fill ~0 ~1 ~1 ~1 ~8 ~1 minecraft:air
fill ~0 ~1 ~2 ~0 ~4 ~14 minecraft:air
fill ~1 ~1 ~13 ~6 ~2 ~14 minecraft:air
fill ~2 ~1 ~5 ~3 ~4 ~10 minecraft:air
fill ~2 ~1 ~11 ~2 ~4 ~11 minecraft:air
fill ~3 ~1 ~1 ~4 ~1 ~1 minecraft:air
fill ~3 ~1 ~3 ~5 ~4 ~4 minecraft:air
fill ~4 ~1 ~6 ~4 ~3 ~11 minecraft:air
fill ~5 ~1 ~5 ~5 ~4 ~11 minecraft:air
fill ~6 ~1 ~1 ~6 ~2 ~1 minecraft:air
fill ~6 ~1 ~4 ~6 ~4 ~11 minecraft:air
fill ~7 ~1 ~14 ~7 ~12 ~14 minecraft:air
fill ~8 ~1 ~0 ~12 ~2 ~14 minecraft:air
fill ~13 ~1 ~14 ~13 ~12 ~14 minecraft:air
fill ~14 ~1 ~0 ~18 ~2 ~1 minecraft:air
fill ~14 ~1 ~3 ~15 ~3 ~10 minecraft:air
fill ~14 ~1 ~13 ~17 ~2 ~13 minecraft:air
fill ~16 ~1 ~4 ~16 ~4 ~4 minecraft:air
fill ~16 ~1 ~6 ~16 ~4 ~10 minecraft:air
fill ~16 ~1 ~14 ~17 ~12 ~14 minecraft:air
fill ~18 ~1 ~2 ~18 ~4 ~14 minecraft:air
fill ~0 ~2 ~0 ~2 ~12 ~0 minecraft:air
fill ~2 ~2 ~3 ~2 ~4 ~4 minecraft:air
fill ~4 ~2 ~5 ~4 ~4 ~5 minecraft:air
fill ~5 ~2 ~0 ~5 ~12 ~0 minecraft:air
fill ~14 ~2 ~11 ~16 ~4 ~11 minecraft:air
fill ~16 ~2 ~3 ~16 ~4 ~3 minecraft:air
fill ~1 ~3 ~13 ~5 ~8 ~14 minecraft:air
fill ~6 ~3 ~3 ~6 ~4 ~3 minecraft:air
fill ~6 ~3 ~14 ~6 ~12 ~14 minecraft:air
fill ~8 ~3 ~1 ~12 ~3 ~2 minecraft:air
fill ~8 ~3 ~4 ~8 ~3 ~10 minecraft:air
fill ~8 ~3 ~12 ~8 ~3 ~14 minecraft:air
fill ~9 ~3 ~3 ~11 ~3 ~14 minecraft:air
fill ~12 ~3 ~4 ~12 ~3 ~10 minecraft:air
fill ~12 ~3 ~12 ~12 ~3 ~14 minecraft:air
fill ~14 ~3 ~0 ~18 ~12 ~0 minecraft:air
fill ~14 ~3 ~14 ~15 ~12 ~14 minecraft:air
fill ~15 ~3 ~1 ~18 ~8 ~1 minecraft:air
fill ~15 ~3 ~13 ~17 ~8 ~13 minecraft:air
fill ~16 ~3 ~5 ~16 ~4 ~5 minecraft:air
fill ~3 ~4 ~0 ~4 ~12 ~0 minecraft:air
setblock ~4 ~4 ~6 minecraft:air
fill ~4 ~4 ~8 ~4 ~4 ~11 minecraft:air
fill ~6 ~4 ~0 ~13 ~12 ~0 minecraft:air
fill ~8 ~4 ~1 ~9 ~4 ~1 minecraft:air
fill ~8 ~4 ~3 ~12 ~4 ~6 minecraft:air
fill ~8 ~4 ~8 ~12 ~4 ~11 minecraft:air
fill ~8 ~4 ~13 ~12 ~4 ~14 minecraft:air
fill ~11 ~4 ~1 ~12 ~4 ~1 minecraft:air
fill ~14 ~4 ~3 ~15 ~4 ~6 minecraft:air
fill ~14 ~4 ~7 ~14 ~4 ~10 minecraft:air
fill ~15 ~4 ~8 ~15 ~4 ~10 minecraft:air
fill ~0 ~5 ~2 ~0 ~8 ~6 minecraft:air
fill ~0 ~5 ~8 ~0 ~8 ~14 minecraft:air
fill ~2 ~5 ~1 ~6 ~8 ~1 minecraft:air
fill ~6 ~5 ~13 ~6 ~8 ~13 minecraft:air
fill ~7 ~5 ~3 ~7 ~9 ~11 minecraft:air
fill ~8 ~5 ~14 ~12 ~12 ~14 minecraft:air
fill ~13 ~5 ~3 ~13 ~9 ~11 minecraft:air
fill ~14 ~5 ~1 ~14 ~8 ~1 minecraft:air
fill ~14 ~5 ~13 ~14 ~8 ~13 minecraft:air
fill ~18 ~5 ~2 ~18 ~8 ~6 minecraft:air
fill ~18 ~5 ~8 ~18 ~8 ~14 minecraft:air
fill ~2 ~6 ~3 ~6 ~9 ~11 minecraft:air
fill ~7 ~6 ~1 ~13 ~8 ~1 minecraft:air
fill ~7 ~6 ~13 ~13 ~8 ~13 minecraft:air
fill ~8 ~6 ~3 ~12 ~9 ~11 minecraft:air
fill ~14 ~6 ~3 ~16 ~9 ~11 minecraft:air
fill ~0 ~7 ~7 ~0 ~11 ~7 minecraft:air
fill ~18 ~7 ~7 ~18 ~11 ~7 minecraft:air
fill ~0 ~9 ~3 ~0 ~9 ~6 minecraft:air
fill ~0 ~9 ~8 ~0 ~9 ~11 minecraft:air
fill ~0 ~9 ~14 ~5 ~12 ~14 minecraft:air
fill ~18 ~9 ~3 ~18 ~9 ~6 minecraft:air
fill ~18 ~9 ~8 ~18 ~9 ~11 minecraft:air
fill ~18 ~9 ~14 ~18 ~12 ~14 minecraft:air
fill ~0 ~10 ~1 ~18 ~12 ~2 minecraft:air
fill ~0 ~10 ~5 ~0 ~10 ~6 minecraft:air
fill ~0 ~10 ~8 ~0 ~10 ~9 minecraft:air
fill ~0 ~10 ~12 ~18 ~12 ~13 minecraft:air
fill ~2 ~10 ~5 ~16 ~10 ~9 minecraft:air
fill ~18 ~10 ~5 ~18 ~10 ~6 minecraft:air
fill ~18 ~10 ~8 ~18 ~10 ~9 minecraft:air
fill ~0 ~11 ~3 ~18 ~12 ~4 minecraft:air
fill ~0 ~11 ~10 ~18 ~12 ~11 minecraft:air
fill ~2 ~11 ~7 ~16 ~11 ~7 minecraft:air
fill ~0 ~12 ~5 ~18 ~12 ~6 minecraft:air
fill ~0 ~12 ~8 ~18 ~12 ~9 minecraft:air

# the ground: worn paving through the arch and on the thresholds of both mouths, cobble plinth under the rooms
fill ~1 ~0 ~2 ~4 ~0 ~2 minecraft:cobblestone
fill ~1 ~0 ~3 ~3 ~0 ~3 minecraft:mossy_cobblestone
setblock ~1 ~0 ~4 minecraft:andesite
fill ~1 ~0 ~5 ~1 ~0 ~6 minecraft:mossy_cobblestone
setblock ~1 ~0 ~7 minecraft:cobblestone
fill ~1 ~0 ~8 ~1 ~0 ~9 minecraft:mossy_cobblestone
fill ~1 ~0 ~10 ~1 ~0 ~11 minecraft:cobblestone
fill ~1 ~0 ~12 ~2 ~0 ~12 minecraft:mossy_cobblestone
fill ~2 ~0 ~4 ~3 ~0 ~4 minecraft:cobblestone
fill ~2 ~0 ~5 ~4 ~0 ~5 minecraft:andesite
fill ~2 ~0 ~6 ~2 ~0 ~7 minecraft:mossy_cobblestone
fill ~2 ~0 ~8 ~2 ~0 ~9 minecraft:cobblestone
setblock ~2 ~0 ~10 minecraft:mossy_cobblestone
setblock ~2 ~0 ~11 minecraft:cobblestone
fill ~3 ~0 ~6 ~3 ~0 ~12 minecraft:cobblestone
fill ~4 ~0 ~3 ~5 ~0 ~3 minecraft:cobblestone
setblock ~4 ~0 ~4 minecraft:mossy_cobblestone
setblock ~4 ~0 ~6 minecraft:mossy_cobblestone
fill ~4 ~0 ~7 ~5 ~0 ~9 minecraft:cobblestone
fill ~4 ~0 ~10 ~5 ~0 ~10 minecraft:mossy_cobblestone
fill ~4 ~0 ~11 ~6 ~0 ~11 minecraft:cobblestone
setblock ~4 ~0 ~12 minecraft:cobblestone
setblock ~5 ~0 ~2 minecraft:mossy_cobblestone
fill ~5 ~0 ~4 ~6 ~0 ~4 minecraft:cobblestone
fill ~5 ~0 ~5 ~5 ~0 ~6 minecraft:mossy_cobblestone
setblock ~5 ~0 ~12 minecraft:andesite
setblock ~6 ~0 ~2 minecraft:cobblestone
setblock ~6 ~0 ~3 minecraft:mossy_cobblestone
fill ~6 ~0 ~5 ~6 ~0 ~7 minecraft:andesite
fill ~6 ~0 ~8 ~7 ~0 ~8 minecraft:mossy_cobblestone
fill ~6 ~0 ~9 ~7 ~0 ~10 minecraft:cobblestone
fill ~6 ~0 ~12 ~7 ~0 ~12 minecraft:mossy_cobblestone
setblock ~7 ~0 ~2 minecraft:mossy_cobblestone
fill ~7 ~0 ~3 ~7 ~0 ~7 minecraft:cobblestone
setblock ~7 ~0 ~11 minecraft:andesite
setblock ~8 ~0 ~1 minecraft:cracked_stone_bricks
fill ~8 ~0 ~2 ~8 ~0 ~3 minecraft:stone_bricks
setblock ~8 ~0 ~4 minecraft:cobblestone
setblock ~8 ~0 ~5 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~6 minecraft:stone_bricks
setblock ~8 ~0 ~7 minecraft:mossy_stone_bricks
setblock ~8 ~0 ~8 minecraft:stone_bricks
setblock ~8 ~0 ~9 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~10 minecraft:stone_bricks
fill ~8 ~0 ~11 ~8 ~0 ~12 minecraft:cracked_stone_bricks
fill ~8 ~0 ~13 ~9 ~0 ~13 minecraft:cobblestone
fill ~9 ~0 ~1 ~12 ~0 ~1 minecraft:stone_bricks
setblock ~9 ~0 ~2 minecraft:mossy_stone_bricks
setblock ~9 ~0 ~3 minecraft:stone_bricks
setblock ~9 ~0 ~4 minecraft:mossy_stone_bricks
fill ~9 ~0 ~5 ~9 ~0 ~9 minecraft:stone_bricks
setblock ~9 ~0 ~10 minecraft:cracked_stone_bricks
fill ~9 ~0 ~11 ~10 ~0 ~11 minecraft:stone_bricks
setblock ~9 ~0 ~12 minecraft:cracked_stone_bricks
fill ~10 ~0 ~2 ~10 ~0 ~3 minecraft:cracked_stone_bricks
fill ~10 ~0 ~4 ~10 ~0 ~5 minecraft:stone_bricks
setblock ~10 ~0 ~6 minecraft:cobblestone
setblock ~10 ~0 ~7 minecraft:mossy_stone_bricks
fill ~10 ~0 ~8 ~11 ~0 ~10 minecraft:stone_bricks
fill ~10 ~0 ~12 ~10 ~0 ~13 minecraft:stone_bricks
fill ~11 ~0 ~2 ~12 ~0 ~2 minecraft:stone_bricks
fill ~11 ~0 ~3 ~12 ~0 ~3 minecraft:mossy_stone_bricks
setblock ~11 ~0 ~4 minecraft:cracked_stone_bricks
fill ~11 ~0 ~5 ~12 ~0 ~5 minecraft:stone_bricks
fill ~11 ~0 ~6 ~11 ~0 ~7 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~11 minecraft:mossy_stone_bricks
setblock ~11 ~0 ~12 minecraft:cracked_stone_bricks
fill ~11 ~0 ~13 ~12 ~0 ~13 minecraft:stone_bricks
setblock ~12 ~0 ~4 minecraft:mossy_stone_bricks
setblock ~12 ~0 ~6 minecraft:stone_bricks
setblock ~12 ~0 ~7 minecraft:cobblestone
setblock ~12 ~0 ~8 minecraft:mossy_stone_bricks
fill ~12 ~0 ~9 ~13 ~0 ~9 minecraft:cobblestone
setblock ~12 ~0 ~10 minecraft:mossy_stone_bricks
setblock ~12 ~0 ~11 minecraft:stone_bricks
setblock ~12 ~0 ~12 minecraft:mossy_stone_bricks
fill ~13 ~0 ~2 ~15 ~0 ~2 minecraft:mossy_cobblestone
setblock ~13 ~0 ~3 minecraft:andesite
fill ~13 ~0 ~4 ~14 ~0 ~4 minecraft:cobblestone
setblock ~13 ~0 ~5 minecraft:cobblestone
fill ~13 ~0 ~6 ~13 ~0 ~8 minecraft:mossy_cobblestone
fill ~13 ~0 ~10 ~13 ~0 ~11 minecraft:cobblestone
setblock ~13 ~0 ~12 minecraft:mossy_cobblestone
setblock ~14 ~0 ~3 minecraft:mossy_cobblestone
fill ~14 ~0 ~5 ~15 ~0 ~5 minecraft:mossy_cobblestone
setblock ~14 ~0 ~6 minecraft:andesite
fill ~14 ~0 ~7 ~14 ~0 ~8 minecraft:cobblestone
fill ~14 ~0 ~9 ~14 ~0 ~11 minecraft:mossy_cobblestone
setblock ~14 ~0 ~12 minecraft:cobblestone
fill ~15 ~0 ~3 ~15 ~0 ~4 minecraft:cobblestone
fill ~15 ~0 ~6 ~15 ~0 ~11 minecraft:cobblestone
fill ~15 ~0 ~12 ~17 ~0 ~12 minecraft:mossy_cobblestone
setblock ~16 ~0 ~2 minecraft:cobblestone
setblock ~16 ~0 ~3 minecraft:mossy_cobblestone
fill ~16 ~0 ~4 ~16 ~0 ~6 minecraft:cobblestone
fill ~16 ~0 ~7 ~17 ~0 ~7 minecraft:andesite
setblock ~16 ~0 ~8 minecraft:mossy_cobblestone
fill ~16 ~0 ~9 ~16 ~0 ~11 minecraft:cobblestone
setblock ~17 ~0 ~2 minecraft:mossy_cobblestone
setblock ~17 ~0 ~3 minecraft:cobblestone
setblock ~17 ~0 ~4 minecraft:andesite
fill ~17 ~0 ~5 ~17 ~0 ~6 minecraft:cobblestone
fill ~17 ~0 ~8 ~17 ~0 ~11 minecraft:cobblestone

# the ground storey: stone brick walls, mossy and cracked low down, round the toll room (x 1..7) and the guard room (x 13..17)
fill ~1 ~1 ~2 ~1 ~4 ~2 minecraft:stone_bricks
fill ~1 ~1 ~3 ~1 ~1 ~4 minecraft:mossy_stone_bricks
setblock ~1 ~1 ~5 minecraft:cracked_stone_bricks
fill ~1 ~1 ~6 ~1 ~1 ~12 minecraft:stone_bricks
fill ~2 ~1 ~2 ~2 ~2 ~2 minecraft:mossy_stone_bricks
fill ~2 ~1 ~12 ~2 ~3 ~12 minecraft:stone_bricks
fill ~3 ~1 ~2 ~5 ~1 ~2 minecraft:stone_bricks
setblock ~3 ~1 ~12 minecraft:mossy_stone_bricks
setblock ~4 ~1 ~12 minecraft:stone_bricks
fill ~5 ~1 ~12 ~7 ~1 ~12 minecraft:mossy_stone_bricks
setblock ~6 ~1 ~2 minecraft:mossy_cobblestone
fill ~7 ~1 ~2 ~7 ~3 ~2 minecraft:stone_bricks
setblock ~7 ~1 ~3 minecraft:cracked_stone_bricks
fill ~7 ~1 ~4 ~7 ~1 ~6 minecraft:stone_bricks
setblock ~7 ~1 ~8 minecraft:stone_bricks
fill ~7 ~1 ~9 ~7 ~1 ~10 minecraft:mossy_stone_bricks
fill ~7 ~1 ~11 ~7 ~3 ~11 minecraft:stone_bricks
fill ~13 ~1 ~2 ~17 ~1 ~2 minecraft:stone_bricks
setblock ~13 ~1 ~3 minecraft:cracked_stone_bricks
fill ~13 ~1 ~4 ~13 ~2 ~4 minecraft:stone_bricks
fill ~13 ~1 ~5 ~13 ~1 ~6 minecraft:mossy_stone_bricks
fill ~13 ~1 ~8 ~13 ~3 ~8 minecraft:mossy_stone_bricks
fill ~13 ~1 ~9 ~13 ~1 ~10 minecraft:stone_bricks
fill ~13 ~1 ~11 ~13 ~1 ~12 minecraft:mossy_stone_bricks
fill ~14 ~1 ~12 ~15 ~1 ~12 minecraft:stone_bricks
fill ~16 ~1 ~12 ~17 ~1 ~12 minecraft:mossy_stone_bricks
setblock ~17 ~1 ~3 minecraft:mossy_stone_bricks
fill ~17 ~1 ~4 ~17 ~3 ~4 minecraft:stone_bricks
setblock ~17 ~1 ~5 minecraft:cracked_stone_bricks
setblock ~17 ~1 ~6 minecraft:stone_bricks
fill ~17 ~1 ~7 ~17 ~2 ~7 minecraft:mossy_stone_bricks
fill ~17 ~1 ~8 ~17 ~1 ~10 minecraft:stone_bricks
fill ~17 ~1 ~11 ~17 ~2 ~11 minecraft:cracked_stone_bricks
fill ~1 ~2 ~3 ~1 ~3 ~3 minecraft:mossy_stone_bricks
setblock ~1 ~2 ~4 minecraft:stone_bricks
fill ~1 ~2 ~6 ~1 ~3 ~6 minecraft:mossy_stone_bricks
fill ~1 ~2 ~7 ~1 ~4 ~7 minecraft:stone_bricks
setblock ~1 ~2 ~8 minecraft:cracked_stone_bricks
fill ~1 ~2 ~10 ~1 ~4 ~10 minecraft:stone_bricks
setblock ~1 ~2 ~11 minecraft:mossy_stone_bricks
setblock ~1 ~2 ~12 minecraft:stone_bricks
fill ~3 ~2 ~2 ~3 ~4 ~2 minecraft:stone_bricks
setblock ~3 ~2 ~12 minecraft:cracked_stone_bricks
fill ~5 ~2 ~2 ~6 ~2 ~2 minecraft:mossy_stone_bricks
setblock ~5 ~2 ~12 minecraft:mossy_stone_bricks
setblock ~6 ~2 ~12 minecraft:stone_bricks
fill ~7 ~2 ~3 ~7 ~4 ~3 minecraft:stone_bricks
setblock ~7 ~2 ~4 minecraft:mossy_cobblestone
fill ~7 ~2 ~5 ~7 ~4 ~5 minecraft:mossy_stone_bricks
setblock ~7 ~2 ~6 minecraft:stone_bricks
setblock ~7 ~2 ~8 minecraft:mossy_stone_bricks
fill ~7 ~2 ~9 ~7 ~2 ~10 minecraft:stone_bricks
setblock ~7 ~2 ~12 minecraft:mossy_stone_bricks
fill ~13 ~2 ~2 ~13 ~4 ~2 minecraft:stone_bricks
setblock ~13 ~2 ~3 minecraft:mossy_stone_bricks
setblock ~13 ~2 ~5 minecraft:stone_bricks
setblock ~13 ~2 ~6 minecraft:cracked_stone_bricks
setblock ~13 ~2 ~9 minecraft:mossy_stone_bricks
fill ~13 ~2 ~10 ~13 ~2 ~11 minecraft:stone_bricks
setblock ~13 ~2 ~12 minecraft:mossy_stone_bricks
fill ~14 ~2 ~2 ~14 ~3 ~2 minecraft:cracked_stone_bricks
setblock ~14 ~2 ~12 minecraft:cracked_stone_bricks
fill ~16 ~2 ~2 ~17 ~2 ~2 minecraft:cracked_stone_bricks
fill ~16 ~2 ~12 ~16 ~3 ~12 minecraft:mossy_stone_bricks
fill ~17 ~2 ~3 ~17 ~4 ~3 minecraft:stone_bricks
setblock ~17 ~2 ~6 minecraft:cracked_stone_bricks
fill ~17 ~2 ~8 ~17 ~4 ~8 minecraft:mossy_stone_bricks
fill ~17 ~2 ~10 ~17 ~4 ~10 minecraft:stone_bricks
fill ~17 ~2 ~12 ~17 ~4 ~12 minecraft:stone_bricks
setblock ~1 ~3 ~4 minecraft:cracked_stone_bricks
fill ~1 ~3 ~8 ~1 ~4 ~8 minecraft:stone_bricks
setblock ~1 ~3 ~11 minecraft:cracked_stone_bricks
setblock ~1 ~3 ~12 minecraft:mossy_stone_bricks
fill ~2 ~3 ~2 ~2 ~4 ~2 minecraft:stone_bricks
setblock ~3 ~3 ~12 minecraft:stone_bricks
setblock ~5 ~3 ~2 minecraft:mossy_stone_bricks
fill ~5 ~3 ~12 ~5 ~4 ~12 minecraft:stone_bricks
fill ~6 ~3 ~2 ~6 ~4 ~2 minecraft:stone_bricks
fill ~6 ~3 ~12 ~7 ~3 ~12 minecraft:cracked_stone_bricks
setblock ~7 ~3 ~4 minecraft:stone_bricks
fill ~7 ~3 ~6 ~7 ~3 ~9 minecraft:cracked_stone_bricks
setblock ~7 ~3 ~10 minecraft:mossy_stone_bricks
fill ~13 ~3 ~3 ~13 ~4 ~3 minecraft:stone_bricks
setblock ~13 ~3 ~4 minecraft:mossy_stone_bricks
setblock ~13 ~3 ~5 minecraft:cracked_stone_bricks
setblock ~13 ~3 ~6 minecraft:mossy_stone_bricks
fill ~13 ~3 ~7 ~13 ~4 ~7 minecraft:stone_bricks
setblock ~13 ~3 ~9 minecraft:stone_bricks
setblock ~13 ~3 ~10 minecraft:mossy_stone_bricks
setblock ~13 ~3 ~11 minecraft:cracked_stone_bricks
fill ~13 ~3 ~12 ~14 ~4 ~12 minecraft:stone_bricks
setblock ~16 ~3 ~2 minecraft:mossy_stone_bricks
fill ~17 ~3 ~2 ~17 ~4 ~2 minecraft:stone_bricks
setblock ~17 ~3 ~6 minecraft:mossy_stone_bricks
fill ~17 ~3 ~7 ~17 ~4 ~7 minecraft:stone_bricks
setblock ~17 ~3 ~11 minecraft:stone_bricks
fill ~1 ~4 ~3 ~1 ~4 ~6 minecraft:stone_bricks
setblock ~1 ~4 ~9 minecraft:stone_bricks
setblock ~1 ~4 ~11 minecraft:mossy_stone_bricks
setblock ~1 ~4 ~12 minecraft:stone_bricks
fill ~2 ~4 ~12 ~3 ~4 ~12 minecraft:mossy_stone_bricks
setblock ~4 ~4 ~2 minecraft:stone_bricks
setblock ~4 ~4 ~12 minecraft:stone_bricks
setblock ~5 ~4 ~2 minecraft:cracked_stone_bricks
setblock ~6 ~4 ~12 minecraft:cracked_stone_bricks
setblock ~7 ~4 ~2 minecraft:cracked_stone_bricks
setblock ~7 ~4 ~4 minecraft:mossy_stone_bricks
fill ~7 ~4 ~6 ~7 ~4 ~10 minecraft:stone_bricks
setblock ~7 ~4 ~11 minecraft:mossy_stone_bricks
setblock ~7 ~4 ~12 minecraft:stone_bricks
setblock ~13 ~4 ~4 minecraft:cracked_stone_bricks
setblock ~13 ~4 ~5 minecraft:stone_bricks
setblock ~13 ~4 ~6 minecraft:cracked_stone_bricks
setblock ~13 ~4 ~8 minecraft:stone_bricks
fill ~13 ~4 ~9 ~13 ~4 ~10 minecraft:cracked_stone_bricks
setblock ~13 ~4 ~11 minecraft:stone_bricks
fill ~14 ~4 ~2 ~15 ~4 ~2 minecraft:mossy_stone_bricks
fill ~15 ~4 ~12 ~16 ~4 ~12 minecraft:stone_bricks
setblock ~16 ~4 ~2 minecraft:cracked_stone_bricks
fill ~17 ~4 ~4 ~17 ~4 ~5 minecraft:cracked_stone_bricks
setblock ~17 ~4 ~6 minecraft:stone_bricks
setblock ~17 ~4 ~9 minecraft:stone_bricks
setblock ~17 ~4 ~11 minecraft:mossy_stone_bricks

# the arch: two rings of stone brick over the passage at the mouths and the middle, upside-down stairs springing at y 4, a vault of stone bricks at y 5
setblock ~8 ~4 ~2 minecraft:stone_brick_stairs[facing=west,half=top]
setblock ~8 ~4 ~7 minecraft:stone_brick_stairs[facing=west,half=top]
setblock ~8 ~4 ~12 minecraft:stone_brick_stairs[facing=west,half=top]
fill ~9 ~4 ~2 ~11 ~4 ~2 minecraft:air
setblock ~9 ~4 ~7 minecraft:air
fill ~9 ~4 ~12 ~11 ~4 ~12 minecraft:air
setblock ~11 ~4 ~7 minecraft:air
setblock ~12 ~4 ~2 minecraft:stone_brick_stairs[facing=east,half=top]
setblock ~12 ~4 ~7 minecraft:stone_brick_stairs[facing=east,half=top]
setblock ~12 ~4 ~12 minecraft:stone_brick_stairs[facing=east,half=top]
fill ~8 ~5 ~3 ~11 ~5 ~3 minecraft:stone_bricks
fill ~8 ~5 ~4 ~8 ~5 ~9 minecraft:stone_bricks
setblock ~8 ~5 ~10 minecraft:cracked_stone_bricks
setblock ~8 ~5 ~11 minecraft:stone_bricks
fill ~9 ~5 ~4 ~10 ~5 ~4 minecraft:mossy_stone_bricks
fill ~9 ~5 ~5 ~10 ~5 ~7 minecraft:stone_bricks
setblock ~9 ~5 ~8 minecraft:mossy_stone_bricks
fill ~9 ~5 ~9 ~9 ~5 ~11 minecraft:stone_bricks
fill ~10 ~5 ~8 ~12 ~5 ~8 minecraft:stone_bricks
setblock ~10 ~5 ~9 minecraft:cracked_stone_bricks
fill ~10 ~5 ~10 ~11 ~5 ~11 minecraft:stone_bricks
fill ~11 ~5 ~4 ~11 ~5 ~5 minecraft:stone_bricks
fill ~11 ~5 ~6 ~11 ~5 ~7 minecraft:cracked_stone_bricks
fill ~11 ~5 ~9 ~12 ~5 ~9 minecraft:stone_bricks
fill ~12 ~5 ~3 ~12 ~5 ~4 minecraft:mossy_stone_bricks
setblock ~12 ~5 ~5 minecraft:stone_bricks
setblock ~12 ~5 ~6 minecraft:mossy_stone_bricks
setblock ~12 ~5 ~7 minecraft:cracked_stone_bricks
setblock ~12 ~5 ~10 minecraft:cracked_stone_bricks
setblock ~12 ~5 ~11 minecraft:stone_bricks

# the upper floor at y 5 over both rooms, spruce boards
fill ~2 ~5 ~3 ~6 ~5 ~10 minecraft:spruce_planks
setblock ~2 ~5 ~11 minecraft:spruce_planks
fill ~4 ~5 ~11 ~6 ~5 ~11 minecraft:spruce_planks
fill ~14 ~5 ~3 ~16 ~5 ~11 minecraft:spruce_planks

# the upper storey: dark oak framed spruce plank walls (y 6..8) on a stone string course at y 5
fill ~1 ~5 ~2 ~2 ~5 ~2 minecraft:mossy_stone_bricks
fill ~1 ~5 ~3 ~1 ~5 ~8 minecraft:stone_bricks
fill ~1 ~5 ~9 ~1 ~5 ~10 minecraft:mossy_stone_bricks
fill ~1 ~5 ~11 ~1 ~5 ~12 minecraft:stone_bricks
fill ~2 ~5 ~12 ~3 ~5 ~12 minecraft:stone_bricks
fill ~3 ~5 ~2 ~4 ~5 ~2 minecraft:stone_bricks
fill ~4 ~5 ~12 ~5 ~5 ~12 minecraft:chiseled_stone_bricks
setblock ~5 ~5 ~2 minecraft:mossy_stone_bricks
setblock ~6 ~5 ~2 minecraft:stone_bricks
fill ~6 ~5 ~12 ~12 ~5 ~12 minecraft:stone_bricks
setblock ~7 ~5 ~2 minecraft:mossy_stone_bricks
fill ~8 ~5 ~2 ~10 ~5 ~2 minecraft:stone_bricks
setblock ~11 ~5 ~2 minecraft:mossy_stone_bricks
fill ~12 ~5 ~2 ~13 ~5 ~2 minecraft:stone_bricks
fill ~13 ~5 ~12 ~14 ~5 ~12 minecraft:mossy_stone_bricks
setblock ~14 ~5 ~2 minecraft:mossy_stone_bricks
fill ~15 ~5 ~2 ~17 ~5 ~2 minecraft:stone_bricks
fill ~15 ~5 ~12 ~16 ~5 ~12 minecraft:stone_bricks
setblock ~17 ~5 ~3 minecraft:mossy_stone_bricks
setblock ~17 ~5 ~4 minecraft:stone_bricks
fill ~17 ~5 ~5 ~17 ~5 ~6 minecraft:mossy_stone_bricks
fill ~17 ~5 ~7 ~17 ~5 ~9 minecraft:stone_bricks
fill ~17 ~5 ~10 ~17 ~5 ~12 minecraft:mossy_stone_bricks
fill ~1 ~6 ~2 ~1 ~8 ~2 minecraft:dark_oak_log[axis=y]
fill ~1 ~6 ~3 ~1 ~6 ~6 minecraft:spruce_planks
fill ~1 ~6 ~7 ~1 ~8 ~7 minecraft:dark_oak_log[axis=y]
fill ~1 ~6 ~8 ~1 ~7 ~8 minecraft:stripped_spruce_wood
setblock ~1 ~6 ~9 minecraft:spruce_planks
fill ~1 ~6 ~10 ~1 ~8 ~10 minecraft:stripped_spruce_wood
fill ~1 ~6 ~11 ~1 ~7 ~11 minecraft:spruce_planks
fill ~1 ~6 ~12 ~1 ~8 ~12 minecraft:dark_oak_log[axis=y]
fill ~2 ~6 ~2 ~4 ~6 ~2 minecraft:spruce_planks
fill ~2 ~6 ~12 ~4 ~6 ~12 minecraft:spruce_planks
fill ~5 ~6 ~2 ~5 ~8 ~2 minecraft:dark_oak_log[axis=y]
fill ~5 ~6 ~12 ~5 ~8 ~12 minecraft:dark_oak_log[axis=y]
fill ~6 ~6 ~2 ~7 ~6 ~2 minecraft:spruce_planks
fill ~6 ~6 ~12 ~8 ~6 ~12 minecraft:spruce_planks
setblock ~8 ~6 ~2 minecraft:stripped_spruce_wood
fill ~9 ~6 ~2 ~9 ~8 ~2 minecraft:dark_oak_log[axis=y]
fill ~9 ~6 ~12 ~9 ~8 ~12 minecraft:dark_oak_log[axis=y]
fill ~11 ~6 ~2 ~12 ~6 ~2 minecraft:spruce_planks
fill ~11 ~6 ~12 ~11 ~8 ~12 minecraft:spruce_planks
setblock ~12 ~6 ~12 minecraft:stripped_spruce_wood
fill ~13 ~6 ~2 ~13 ~8 ~2 minecraft:dark_oak_log[axis=y]
fill ~13 ~6 ~12 ~13 ~8 ~12 minecraft:dark_oak_log[axis=y]
fill ~14 ~6 ~2 ~16 ~6 ~2 minecraft:spruce_planks
fill ~14 ~6 ~12 ~14 ~7 ~12 minecraft:spruce_planks
setblock ~15 ~6 ~12 minecraft:stripped_spruce_wood
fill ~16 ~6 ~12 ~16 ~8 ~12 minecraft:spruce_planks
fill ~17 ~6 ~2 ~17 ~8 ~2 minecraft:dark_oak_log[axis=y]
fill ~17 ~6 ~3 ~17 ~6 ~5 minecraft:spruce_planks
setblock ~17 ~6 ~6 minecraft:stripped_spruce_wood
fill ~17 ~6 ~7 ~17 ~8 ~7 minecraft:dark_oak_log[axis=y]
fill ~17 ~6 ~8 ~17 ~6 ~11 minecraft:spruce_planks
fill ~17 ~6 ~12 ~17 ~8 ~12 minecraft:dark_oak_log[axis=y]
fill ~1 ~7 ~3 ~1 ~8 ~4 minecraft:spruce_planks
fill ~1 ~7 ~6 ~1 ~8 ~6 minecraft:spruce_planks
fill ~2 ~7 ~2 ~2 ~8 ~2 minecraft:spruce_planks
fill ~2 ~7 ~12 ~2 ~8 ~12 minecraft:spruce_planks
fill ~4 ~7 ~2 ~4 ~8 ~2 minecraft:spruce_planks
setblock ~4 ~7 ~12 minecraft:stripped_spruce_wood
fill ~7 ~7 ~2 ~8 ~8 ~2 minecraft:spruce_planks
fill ~7 ~7 ~12 ~8 ~7 ~12 minecraft:spruce_planks
fill ~11 ~7 ~2 ~11 ~8 ~2 minecraft:spruce_planks
fill ~14 ~7 ~2 ~14 ~8 ~2 minecraft:spruce_planks
setblock ~16 ~7 ~2 minecraft:spruce_planks
setblock ~17 ~7 ~3 minecraft:stripped_spruce_wood
fill ~17 ~7 ~4 ~17 ~8 ~4 minecraft:spruce_planks
fill ~17 ~7 ~6 ~17 ~8 ~6 minecraft:spruce_planks
fill ~17 ~7 ~8 ~17 ~8 ~8 minecraft:spruce_planks
fill ~17 ~7 ~10 ~17 ~8 ~11 minecraft:spruce_planks
setblock ~1 ~8 ~5 minecraft:stripped_spruce_wood
setblock ~1 ~8 ~8 minecraft:spruce_planks
setblock ~1 ~8 ~9 minecraft:stripped_spruce_wood
setblock ~1 ~8 ~11 minecraft:stripped_spruce_wood
setblock ~3 ~8 ~2 minecraft:spruce_planks
setblock ~3 ~8 ~12 minecraft:stripped_spruce_wood
setblock ~4 ~8 ~12 minecraft:spruce_planks
setblock ~6 ~8 ~2 minecraft:spruce_planks
setblock ~6 ~8 ~12 minecraft:spruce_planks
setblock ~7 ~8 ~12 minecraft:stripped_spruce_wood
setblock ~8 ~8 ~12 minecraft:spruce_planks
setblock ~10 ~8 ~2 minecraft:spruce_planks
setblock ~10 ~8 ~12 minecraft:spruce_planks
setblock ~12 ~8 ~2 minecraft:spruce_planks
setblock ~12 ~8 ~12 minecraft:spruce_planks
fill ~14 ~8 ~12 ~15 ~8 ~12 minecraft:stripped_spruce_wood
fill ~15 ~8 ~2 ~16 ~8 ~2 minecraft:stripped_spruce_wood
setblock ~17 ~8 ~3 minecraft:spruce_planks
setblock ~17 ~8 ~5 minecraft:spruce_planks
setblock ~17 ~8 ~9 minecraft:stripped_spruce_wood

# windows: arrow slits in the ground storey, glass in the upper storey, a balcony window over each mouth
fill ~1 ~2 ~5 ~1 ~3 ~5 minecraft:air
fill ~1 ~2 ~9 ~1 ~3 ~9 minecraft:air
fill ~4 ~2 ~2 ~4 ~3 ~2 minecraft:air
fill ~4 ~2 ~12 ~4 ~3 ~12 minecraft:air
fill ~15 ~2 ~2 ~15 ~3 ~2 minecraft:air
fill ~15 ~2 ~12 ~15 ~3 ~12 minecraft:air
fill ~17 ~2 ~5 ~17 ~3 ~5 minecraft:air
fill ~17 ~2 ~9 ~17 ~3 ~9 minecraft:air
fill ~10 ~6 ~2 ~10 ~7 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~10 ~6 ~12 ~10 ~7 ~12 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~1 ~7 ~5 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~1 ~7 ~9 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~3 ~7 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~3 ~7 ~12 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~6 ~7 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~6 ~7 ~12 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~12 ~7 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~12 ~7 ~12 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~15 ~7 ~2 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~15 ~7 ~12 minecraft:glass_pane[north=false,east=true,south=false,west=true]
setblock ~17 ~7 ~5 minecraft:glass_pane[north=true,east=false,south=true,west=false]
setblock ~17 ~7 ~9 minecraft:glass_pane[north=true,east=false,south=true,west=false]

# side doors off the passage into the toll room and the guard room
fill ~7 ~1 ~7 ~7 ~2 ~7 minecraft:air
fill ~13 ~1 ~7 ~13 ~2 ~7 minecraft:air

# the roof: deepslate tile stairs from the eaves at y 9 (z 1 and 13) at half a block a row to a ridge of deepslate tiles at y 12 (z 7), over x 0..18, a block proud of both gables
fill ~0 ~9 ~1 ~2 ~9 ~1 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~0 ~9 ~2 ~18 ~9 ~2 minecraft:deepslate_tile_slab[type=top]
fill ~0 ~9 ~12 ~18 ~9 ~12 minecraft:deepslate_tile_slab[type=top]
fill ~0 ~9 ~13 ~18 ~9 ~13 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~3 ~9 ~1 minecraft:cobbled_deepslate_stairs[facing=south,half=bottom]
fill ~4 ~9 ~1 ~18 ~9 ~1 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~0 ~10 ~3 ~13 ~10 ~3 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~0 ~10 ~4 ~18 ~10 ~4 minecraft:deepslate_tile_slab[type=top]
fill ~0 ~10 ~10 ~18 ~10 ~10 minecraft:deepslate_tile_slab[type=top]
fill ~0 ~10 ~11 ~15 ~10 ~11 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~14 ~10 ~3 minecraft:cobbled_deepslate_stairs[facing=south,half=bottom]
fill ~15 ~10 ~3 ~18 ~10 ~3 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~16 ~10 ~11 minecraft:cobbled_deepslate_stairs[facing=north,half=bottom]
fill ~17 ~10 ~11 ~18 ~10 ~11 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~0 ~11 ~5 ~18 ~11 ~5 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~0 ~11 ~6 ~18 ~11 ~6 minecraft:deepslate_tile_slab[type=top]
fill ~0 ~11 ~8 ~18 ~11 ~8 minecraft:deepslate_tile_slab[type=top]
fill ~0 ~11 ~9 ~18 ~11 ~9 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~0 ~12 ~7 ~18 ~12 ~7 minecraft:deepslate_tiles

# the gables at x 1 and x 17: spruce planks up to the roof with a dark oak king post
fill ~1 ~9 ~3 ~1 ~9 ~6 minecraft:spruce_planks
fill ~1 ~9 ~7 ~1 ~11 ~7 minecraft:dark_oak_log[axis=y]
fill ~1 ~9 ~8 ~1 ~9 ~11 minecraft:spruce_planks
fill ~17 ~9 ~3 ~17 ~9 ~6 minecraft:spruce_planks
fill ~17 ~9 ~7 ~17 ~11 ~7 minecraft:dark_oak_log[axis=y]
fill ~17 ~9 ~8 ~17 ~9 ~11 minecraft:spruce_planks
fill ~1 ~10 ~5 ~1 ~10 ~6 minecraft:spruce_planks
fill ~1 ~10 ~8 ~1 ~10 ~9 minecraft:spruce_planks
fill ~17 ~10 ~5 ~17 ~10 ~6 minecraft:spruce_planks
fill ~17 ~10 ~8 ~17 ~10 ~9 minecraft:spruce_planks

# the mouths: stone piers either side (x 7 and 13 at z 1 and 13) carrying a pentice of deepslate stairs at y 5 over the threshold
fill ~7 ~1 ~1 ~7 ~2 ~1 minecraft:stone_bricks
setblock ~7 ~1 ~13 minecraft:stone_bricks
fill ~13 ~1 ~1 ~13 ~2 ~1 minecraft:stone_bricks
fill ~13 ~1 ~13 ~13 ~4 ~13 minecraft:stone_bricks
setblock ~7 ~2 ~13 minecraft:cracked_stone_bricks
fill ~7 ~3 ~1 ~7 ~4 ~1 minecraft:cracked_stone_bricks
fill ~7 ~3 ~13 ~7 ~4 ~13 minecraft:stone_bricks
setblock ~13 ~3 ~1 minecraft:cracked_stone_bricks
setblock ~13 ~4 ~1 minecraft:stone_bricks
fill ~7 ~5 ~1 ~13 ~5 ~1 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
fill ~7 ~5 ~13 ~13 ~5 ~13 minecraft:deepslate_tile_stairs[facing=north,half=bottom]

# the bracket lanterns: a dark oak fence out from the outer face of each pier, a lantern hung under it
setblock ~6 ~3 ~1 minecraft:lantern[hanging=true]
setblock ~6 ~3 ~13 minecraft:lantern[hanging=true]
setblock ~14 ~3 ~1 minecraft:lantern[hanging=true]
setblock ~14 ~3 ~13 minecraft:lantern[hanging=true]
setblock ~6 ~4 ~1 minecraft:dark_oak_fence[north=false,east=true,south=true,west=true]
setblock ~6 ~4 ~13 minecraft:dark_oak_fence[north=true,east=true,south=false,west=false]
setblock ~14 ~4 ~1 minecraft:dark_oak_fence[north=false,east=false,south=true,west=true]
setblock ~14 ~4 ~13 minecraft:dark_oak_fence[north=true,east=false,south=false,west=true]

# the toll bar at the north mouth: a dark oak post at x 7, a bar of red and white stripes at y 3 from x 6 to the fork post at x 13, a stone counterweight on a chain at x 6
setblock ~6 ~1 ~0 minecraft:cobblestone
fill ~7 ~1 ~0 ~7 ~3 ~0 minecraft:dark_oak_log[axis=y]
fill ~13 ~1 ~0 ~13 ~2 ~0 minecraft:dark_oak_fence[north=false,east=false,south=true,west=false]
setblock ~6 ~2 ~0 minecraft:chain[axis=y]
setblock ~6 ~3 ~0 minecraft:stripped_mangrove_log[axis=x]
setblock ~8 ~3 ~0 minecraft:stripped_mangrove_log[axis=x]
setblock ~9 ~3 ~0 minecraft:stripped_birch_log[axis=x]
setblock ~10 ~3 ~0 minecraft:stripped_mangrove_log[axis=x]
setblock ~11 ~3 ~0 minecraft:stripped_birch_log[axis=x]
setblock ~12 ~3 ~0 minecraft:stripped_mangrove_log[axis=x]
setblock ~13 ~3 ~0 minecraft:stripped_birch_log[axis=x]

# the toll board and the sign: a board of dark oak planks on two dark oak posts before the toll room wall beside the north mouth, four pale birch signs on its face, a hanging sign under the pentice over the passage
fill ~2 ~1 ~1 ~2 ~4 ~1 minecraft:dark_oak_log[axis=y]
fill ~5 ~1 ~1 ~5 ~4 ~1 minecraft:dark_oak_log[axis=y]
setblock ~3 ~2 ~0 minecraft:birch_wall_sign[facing=north]{front_text:{messages:['{"text":"Halt"}','{"text":"at the bar"}','{"text":""}','{"text":""}']}}
fill ~3 ~2 ~1 ~4 ~3 ~1 minecraft:dark_oak_planks
setblock ~4 ~2 ~0 minecraft:birch_wall_sign[facing=north]{front_text:{messages:['{"text":"No pay,"}','{"text":"no road"}','{"text":""}','{"text":""}']}}
setblock ~3 ~3 ~0 minecraft:birch_wall_sign[facing=north]{front_text:{messages:['{"text":"TOLL"}','{"text":"by order"}','{"text":"of the King"}','{"text":""}']}}
setblock ~4 ~3 ~0 minecraft:birch_wall_sign[facing=north]{front_text:{messages:['{"text":"A cart: 2"}','{"text":"A beast: 1"}','{"text":"On foot: 1"}','{"text":""}']}}
fill ~3 ~4 ~1 ~4 ~4 ~1 minecraft:dark_oak_slab[type=bottom]
setblock ~10 ~4 ~1 minecraft:dark_oak_hanging_sign[rotation=8,attached=false]{front_text:{messages:['{"text":""}','{"text":"CUSTOMS"}','{"text":""}','{"text":""}']}}

# the arms of the crown inside the arch: a red banner with a gold lozenge on each wall of the passage at both mouths (fix round 2: two banners over the two windows of a gable read as a face)
setblock ~8 ~3 ~3 minecraft:red_wall_banner[facing=east]{Patterns:[{Pattern:"mr",Color:4},{Pattern:"bs",Color:15}]}
setblock ~8 ~3 ~11 minecraft:red_wall_banner[facing=east]{Patterns:[{Pattern:"mr",Color:4},{Pattern:"bs",Color:15}]}
setblock ~12 ~3 ~3 minecraft:red_wall_banner[facing=west]{Patterns:[{Pattern:"mr",Color:4},{Pattern:"bs",Color:15}]}
setblock ~12 ~3 ~11 minecraft:red_wall_banner[facing=west]{Patterns:[{Pattern:"mr",Color:4},{Pattern:"bs",Color:15}]}

# a lantern on a dark oak bracket over the middle of each gable, hung under the string course (one light on the axis of the gable)
setblock ~0 ~5 ~7 minecraft:lantern[hanging=true]
setblock ~18 ~5 ~7 minecraft:lantern[hanging=true]
setblock ~0 ~6 ~7 minecraft:dark_oak_fence[north=false,east=true,south=false,west=false]
setblock ~18 ~6 ~7 minecraft:dark_oak_fence[north=false,east=false,south=false,west=true]

# the south mouth: the old swing gate of spruce rails, opened back against the guard room wall
fill ~14 ~1 ~14 ~14 ~2 ~14 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
fill ~15 ~1 ~14 ~15 ~2 ~14 minecraft:spruce_fence[north=false,east=false,south=false,west=true]

# inside the toll room: a lectern for the ledger, chests of tolls, barrels, a ladder up; the guard room: racks, a bed, a stand of spears (fences)
fill ~2 ~1 ~3 ~2 ~1 ~4 minecraft:chest[facing=east]
fill ~3 ~1 ~11 ~3 ~5 ~11 minecraft:ladder[facing=north]
setblock ~4 ~1 ~5 minecraft:lectern[facing=east]
fill ~6 ~1 ~3 ~6 ~2 ~3 minecraft:barrel[facing=up]
fill ~14 ~1 ~11 ~15 ~1 ~11 minecraft:gray_wool
setblock ~16 ~1 ~3 minecraft:barrel[facing=up]
setblock ~16 ~1 ~5 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
setblock ~16 ~1 ~11 minecraft:smithing_table
setblock ~16 ~2 ~5 minecraft:spruce_fence[north=false,east=false,south=false,west=false]
setblock ~4 ~4 ~7 minecraft:lantern[hanging=true]
setblock ~10 ~4 ~7 minecraft:lantern[hanging=true]
setblock ~15 ~4 ~7 minecraft:lantern[hanging=true]
