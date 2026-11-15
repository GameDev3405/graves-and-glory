# size 13 14 13
# camp/ridge_stair: the stair from the bowl to the ridge, set into the ridge bank (placed with flatten=false).
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# The bank beside the flight stands at local y 11, 10, 9, 8, 7, 6, 6, 5, 4, 3, 2, 1, 0 for z 0..12; the plateau north of z 0 is at 12.

# air above the flight and the landing: the bank is cut back so nothing stands on the steps
fill ~5 ~1 ~12 ~7 ~13 ~12 minecraft:air
fill ~4 ~2 ~11 ~8 ~13 ~11 minecraft:air
fill ~4 ~2 ~12 ~4 ~13 ~12 minecraft:air
fill ~8 ~2 ~12 ~8 ~13 ~12 minecraft:air
fill ~4 ~3 ~10 ~8 ~13 ~10 minecraft:air
fill ~4 ~4 ~9 ~8 ~13 ~9 minecraft:air
fill ~4 ~5 ~8 ~8 ~13 ~8 minecraft:air
fill ~4 ~6 ~7 ~8 ~13 ~7 minecraft:air
fill ~4 ~7 ~6 ~8 ~13 ~6 minecraft:air
fill ~4 ~8 ~5 ~8 ~13 ~5 minecraft:air
fill ~4 ~9 ~4 ~8 ~13 ~4 minecraft:air
fill ~4 ~10 ~3 ~8 ~13 ~3 minecraft:air
fill ~4 ~11 ~2 ~8 ~13 ~2 minecraft:air
fill ~4 ~12 ~1 ~8 ~13 ~1 minecraft:air
fill ~4 ~13 ~0 ~8 ~13 ~0 minecraft:air

# the core of the flight, under the treads (stone bricks, hidden by the walls and the bank)
fill ~4 ~0 ~0 ~8 ~0 ~11 minecraft:stone_bricks
fill ~4 ~1 ~0 ~8 ~1 ~10 minecraft:stone_bricks
fill ~4 ~2 ~0 ~8 ~2 ~9 minecraft:stone_bricks
fill ~4 ~3 ~0 ~8 ~3 ~8 minecraft:stone_bricks
fill ~4 ~4 ~0 ~8 ~4 ~7 minecraft:stone_bricks
fill ~4 ~5 ~0 ~8 ~5 ~6 minecraft:stone_bricks
fill ~4 ~6 ~0 ~8 ~6 ~5 minecraft:stone_bricks
fill ~4 ~7 ~0 ~8 ~7 ~4 minecraft:stone_bricks
fill ~4 ~8 ~0 ~8 ~8 ~3 minecraft:stone_bricks
fill ~4 ~9 ~0 ~8 ~9 ~2 minecraft:stone_bricks
fill ~4 ~10 ~0 ~8 ~10 ~1 minecraft:stone_bricks
fill ~4 ~11 ~0 ~8 ~11 ~0 minecraft:stone_bricks

# the bottom landing, flush with the bowl (feet 1 at z 12)
setblock ~4 ~0 ~12 minecraft:stone_bricks
setblock ~5 ~0 ~12 minecraft:mossy_stone_bricks
setblock ~6 ~0 ~12 minecraft:chiseled_stone_bricks
fill ~7 ~0 ~12 ~8 ~0 ~12 minecraft:stone_bricks

# the retaining walls at x 3 and 9: stone bricks from the ground to the tread line, worn and mossy low down
fill ~3 ~0 ~0 ~3 ~0 ~3 minecraft:mossy_stone_bricks
setblock ~3 ~0 ~4 minecraft:stone_bricks
setblock ~3 ~0 ~5 minecraft:mossy_cobblestone
fill ~3 ~0 ~6 ~3 ~2 ~6 minecraft:stone_bricks
setblock ~3 ~0 ~7 minecraft:mossy_stone_bricks
fill ~3 ~0 ~8 ~3 ~3 ~8 minecraft:stone_bricks
fill ~3 ~0 ~9 ~3 ~1 ~9 minecraft:mossy_stone_bricks
setblock ~3 ~0 ~10 minecraft:cracked_stone_bricks
setblock ~3 ~0 ~11 minecraft:stone_bricks
setblock ~9 ~0 ~0 minecraft:mossy_stone_bricks
fill ~9 ~0 ~1 ~9 ~0 ~3 minecraft:stone_bricks
setblock ~9 ~0 ~4 minecraft:mossy_stone_bricks
fill ~9 ~0 ~5 ~9 ~0 ~6 minecraft:stone_bricks
fill ~9 ~0 ~7 ~9 ~0 ~10 minecraft:mossy_stone_bricks
fill ~9 ~0 ~11 ~9 ~1 ~11 minecraft:stone_bricks
fill ~3 ~1 ~0 ~3 ~2 ~1 minecraft:mossy_stone_bricks
fill ~3 ~1 ~2 ~3 ~2 ~3 minecraft:stone_bricks
setblock ~3 ~1 ~4 minecraft:mossy_stone_bricks
setblock ~3 ~1 ~5 minecraft:stone_bricks
setblock ~3 ~1 ~7 minecraft:cracked_stone_bricks
fill ~3 ~1 ~10 ~3 ~1 ~11 minecraft:mossy_stone_bricks
setblock ~9 ~1 ~0 minecraft:stone_bricks
setblock ~9 ~1 ~1 minecraft:mossy_stone_bricks
fill ~9 ~1 ~2 ~9 ~1 ~4 minecraft:stone_bricks
setblock ~9 ~1 ~5 minecraft:cracked_stone_bricks
setblock ~9 ~1 ~6 minecraft:mossy_stone_bricks
fill ~9 ~1 ~7 ~9 ~3 ~7 minecraft:stone_bricks
setblock ~9 ~1 ~8 minecraft:mossy_cobblestone
fill ~9 ~1 ~9 ~9 ~2 ~9 minecraft:stone_bricks
setblock ~9 ~1 ~10 minecraft:mossy_stone_bricks
fill ~3 ~2 ~4 ~3 ~3 ~4 minecraft:mossy_cobblestone
setblock ~3 ~2 ~5 minecraft:mossy_stone_bricks
setblock ~3 ~2 ~7 minecraft:stone_bricks
fill ~3 ~2 ~9 ~3 ~2 ~10 minecraft:stone_bricks
setblock ~9 ~2 ~0 minecraft:mossy_stone_bricks
fill ~9 ~2 ~1 ~9 ~3 ~1 minecraft:stone_bricks
setblock ~9 ~2 ~2 minecraft:mossy_stone_bricks
fill ~9 ~2 ~3 ~9 ~2 ~4 minecraft:stone_bricks
setblock ~9 ~2 ~5 minecraft:mossy_stone_bricks
setblock ~9 ~2 ~6 minecraft:stone_bricks
setblock ~9 ~2 ~8 minecraft:mossy_stone_bricks
setblock ~9 ~2 ~10 minecraft:cracked_stone_bricks
fill ~3 ~3 ~0 ~3 ~4 ~1 minecraft:stone_bricks
setblock ~3 ~3 ~2 minecraft:mossy_cobblestone
fill ~3 ~3 ~3 ~3 ~4 ~3 minecraft:stone_bricks
setblock ~3 ~3 ~5 minecraft:stone_bricks
fill ~3 ~3 ~6 ~3 ~3 ~7 minecraft:cracked_stone_bricks
setblock ~3 ~3 ~9 minecraft:stone_bricks
fill ~9 ~3 ~0 ~9 ~4 ~0 minecraft:cracked_stone_bricks
setblock ~9 ~3 ~2 minecraft:cracked_stone_bricks
setblock ~9 ~3 ~3 minecraft:mossy_stone_bricks
setblock ~9 ~3 ~4 minecraft:stone_bricks
setblock ~9 ~3 ~5 minecraft:cracked_stone_bricks
setblock ~9 ~3 ~6 minecraft:mossy_stone_bricks
fill ~9 ~3 ~8 ~9 ~4 ~8 minecraft:stone_bricks
setblock ~9 ~3 ~9 minecraft:mossy_stone_bricks
setblock ~3 ~4 ~2 minecraft:stone_bricks
fill ~3 ~4 ~4 ~3 ~4 ~5 minecraft:mossy_stone_bricks
setblock ~3 ~4 ~6 minecraft:cracked_stone_bricks
fill ~3 ~4 ~7 ~3 ~4 ~8 minecraft:mossy_stone_bricks
setblock ~9 ~4 ~1 minecraft:mossy_stone_bricks
fill ~9 ~4 ~2 ~9 ~4 ~3 minecraft:stone_bricks
fill ~9 ~4 ~4 ~9 ~6 ~4 minecraft:mossy_stone_bricks
fill ~9 ~4 ~5 ~9 ~4 ~7 minecraft:mossy_cobblestone
fill ~3 ~5 ~0 ~3 ~6 ~0 minecraft:stone_bricks
fill ~3 ~5 ~1 ~3 ~8 ~1 minecraft:mossy_stone_bricks
fill ~3 ~5 ~2 ~3 ~5 ~4 minecraft:cracked_stone_bricks
setblock ~3 ~5 ~5 minecraft:stone_bricks
setblock ~3 ~5 ~6 minecraft:mossy_stone_bricks
setblock ~3 ~5 ~7 minecraft:stone_bricks
setblock ~9 ~5 ~0 minecraft:mossy_stone_bricks
setblock ~9 ~5 ~1 minecraft:stone_bricks
fill ~9 ~5 ~2 ~9 ~7 ~2 minecraft:cracked_stone_bricks
setblock ~9 ~5 ~3 minecraft:stone_bricks
fill ~9 ~5 ~5 ~9 ~7 ~5 minecraft:stone_bricks
fill ~9 ~5 ~6 ~9 ~6 ~6 minecraft:mossy_stone_bricks
setblock ~9 ~5 ~7 minecraft:stone_bricks
setblock ~3 ~6 ~2 minecraft:stone_bricks
fill ~3 ~6 ~3 ~3 ~6 ~5 minecraft:mossy_stone_bricks
setblock ~3 ~6 ~6 minecraft:stone_bricks
setblock ~9 ~6 ~0 minecraft:stone_bricks
setblock ~9 ~6 ~1 minecraft:mossy_stone_bricks
setblock ~9 ~6 ~3 minecraft:cracked_stone_bricks
setblock ~3 ~7 ~0 minecraft:mossy_stone_bricks
fill ~3 ~7 ~2 ~3 ~7 ~3 minecraft:cracked_stone_bricks
fill ~3 ~7 ~4 ~3 ~8 ~4 minecraft:stone_bricks
setblock ~3 ~7 ~5 minecraft:mossy_stone_bricks
setblock ~9 ~7 ~0 minecraft:mossy_stone_bricks
setblock ~9 ~7 ~1 minecraft:mossy_cobblestone
setblock ~9 ~7 ~3 minecraft:mossy_stone_bricks
fill ~9 ~7 ~4 ~9 ~8 ~4 minecraft:stone_bricks
setblock ~3 ~8 ~0 minecraft:stone_bricks
fill ~3 ~8 ~2 ~3 ~8 ~3 minecraft:mossy_stone_bricks
setblock ~9 ~8 ~0 minecraft:cracked_stone_bricks
setblock ~9 ~8 ~1 minecraft:mossy_stone_bricks
fill ~9 ~8 ~2 ~9 ~8 ~3 minecraft:stone_bricks
setblock ~3 ~9 ~0 minecraft:mossy_stone_bricks
fill ~3 ~9 ~1 ~3 ~9 ~2 minecraft:cracked_stone_bricks
setblock ~3 ~9 ~3 minecraft:stone_bricks
fill ~9 ~9 ~0 ~9 ~10 ~0 minecraft:stone_bricks
fill ~9 ~9 ~1 ~9 ~9 ~2 minecraft:cracked_stone_bricks
setblock ~9 ~9 ~3 minecraft:stone_bricks
fill ~3 ~10 ~0 ~3 ~10 ~2 minecraft:stone_bricks
fill ~9 ~10 ~1 ~9 ~10 ~2 minecraft:stone_bricks
fill ~3 ~11 ~0 ~3 ~11 ~1 minecraft:cracked_stone_bricks
fill ~9 ~11 ~0 ~9 ~11 ~1 minecraft:mossy_stone_bricks
setblock ~3 ~12 ~0 minecraft:stone_bricks
setblock ~9 ~12 ~0 minecraft:stone_bricks

# the newels at the foot: three courses with a chiseled cap
setblock ~3 ~0 ~12 minecraft:cracked_stone_bricks
fill ~9 ~0 ~12 ~9 ~1 ~12 minecraft:mossy_stone_bricks
setblock ~3 ~1 ~12 minecraft:mossy_stone_bricks
setblock ~3 ~2 ~12 minecraft:chiseled_stone_bricks
setblock ~9 ~2 ~12 minecraft:chiseled_stone_bricks

# the treads: a 5-wide stone_brick_stairs flight facing north, feet 2 at z 11 to feet 13 at z 0 (world feet 66 to 77)
fill ~4 ~1 ~11 ~8 ~1 ~11 minecraft:stone_brick_stairs[facing=north]
fill ~4 ~2 ~10 ~8 ~2 ~10 minecraft:stone_brick_stairs[facing=north]
fill ~4 ~3 ~9 ~8 ~3 ~9 minecraft:stone_brick_stairs[facing=north]
fill ~4 ~4 ~8 ~8 ~4 ~8 minecraft:stone_brick_stairs[facing=north]
fill ~4 ~5 ~7 ~8 ~5 ~7 minecraft:stone_brick_stairs[facing=north]
fill ~4 ~6 ~6 ~8 ~6 ~6 minecraft:stone_brick_stairs[facing=north]
fill ~4 ~7 ~5 ~8 ~7 ~5 minecraft:stone_brick_stairs[facing=north]
fill ~4 ~8 ~4 ~8 ~8 ~4 minecraft:stone_brick_stairs[facing=north]
fill ~4 ~9 ~3 ~8 ~9 ~3 minecraft:stone_brick_stairs[facing=north]
fill ~4 ~10 ~2 ~8 ~10 ~2 minecraft:stone_brick_stairs[facing=north]
fill ~4 ~11 ~1 ~8 ~11 ~1 minecraft:stone_brick_stairs[facing=north]
fill ~4 ~12 ~0 ~8 ~12 ~0 minecraft:stone_brick_stairs[facing=north]

# the coping: a stair-stepped parapet one block above the treads, rising with them; a chiseled pier under each lantern post
setblock ~3 ~2 ~11 minecraft:stone_brick_stairs[facing=north]
setblock ~9 ~2 ~11 minecraft:stone_brick_stairs[facing=north]
setblock ~3 ~3 ~10 minecraft:mossy_stone_brick_stairs[facing=north]
setblock ~9 ~3 ~10 minecraft:stone_brick_stairs[facing=north]
setblock ~3 ~4 ~9 minecraft:mossy_stone_brick_stairs[facing=north]
setblock ~9 ~4 ~9 minecraft:mossy_stone_brick_stairs[facing=north]
setblock ~3 ~5 ~8 minecraft:chiseled_stone_bricks
setblock ~9 ~5 ~8 minecraft:chiseled_stone_bricks
setblock ~3 ~6 ~7 minecraft:mossy_stone_brick_stairs[facing=north]
setblock ~9 ~6 ~7 minecraft:stone_brick_stairs[facing=north]
setblock ~3 ~7 ~6 minecraft:stone_brick_stairs[facing=north]
setblock ~9 ~7 ~6 minecraft:stone_brick_stairs[facing=north]
setblock ~3 ~8 ~5 minecraft:stone_brick_stairs[facing=north]
setblock ~9 ~8 ~5 minecraft:stone_brick_stairs[facing=north]
setblock ~3 ~9 ~4 minecraft:chiseled_stone_bricks
setblock ~9 ~9 ~4 minecraft:chiseled_stone_bricks
setblock ~3 ~10 ~3 minecraft:stone_brick_stairs[facing=north]
setblock ~9 ~10 ~3 minecraft:stone_brick_stairs[facing=north]
setblock ~3 ~11 ~2 minecraft:stone_brick_stairs[facing=north]
setblock ~9 ~11 ~2 minecraft:stone_brick_stairs[facing=north]
setblock ~3 ~12 ~1 minecraft:stone_brick_stairs[facing=north]
setblock ~9 ~12 ~1 minecraft:stone_brick_stairs[facing=north]
setblock ~3 ~13 ~0 minecraft:stone_brick_stairs[facing=north]
setblock ~9 ~13 ~0 minecraft:stone_brick_stairs[facing=north]

# lantern posts every 4 steps (z 12, 8 and 4): a stone brick wall post on a pier, a lantern on it
setblock ~3 ~3 ~12 minecraft:stone_brick_wall
setblock ~9 ~3 ~12 minecraft:stone_brick_wall
setblock ~3 ~4 ~12 minecraft:lantern
setblock ~9 ~4 ~12 minecraft:lantern
setblock ~3 ~6 ~8 minecraft:stone_brick_wall
setblock ~9 ~6 ~8 minecraft:stone_brick_wall
setblock ~3 ~7 ~8 minecraft:lantern
setblock ~9 ~7 ~8 minecraft:lantern
setblock ~3 ~10 ~4 minecraft:stone_brick_wall
setblock ~9 ~10 ~4 minecraft:stone_brick_wall
setblock ~3 ~11 ~4 minecraft:lantern
setblock ~9 ~11 ~4 minecraft:lantern

# cheek stones beside the landing, half sunk in the bowl
setblock ~2 ~0 ~12 minecraft:mossy_cobblestone
setblock ~10 ~0 ~12 minecraft:mossy_cobblestone
setblock ~2 ~1 ~12 minecraft:mossy_stone_brick_stairs[facing=east]
setblock ~10 ~1 ~12 minecraft:mossy_stone_brick_stairs[facing=west]

# moss on the landing
setblock ~4 ~1 ~12 minecraft:moss_carpet
setblock ~8 ~1 ~12 minecraft:moss_carpet

# ivy on the outer faces of the walls, above the bank (vines hung on full stone blocks)
setblock ~2 ~8 ~4 minecraft:vine[east=true]
setblock ~10 ~8 ~4 minecraft:vine[west=true]
setblock ~10 ~9 ~3 minecraft:vine[west=true]
setblock ~2 ~11 ~1 minecraft:vine[east=true]
setblock ~2 ~12 ~0 minecraft:vine[east=true]
