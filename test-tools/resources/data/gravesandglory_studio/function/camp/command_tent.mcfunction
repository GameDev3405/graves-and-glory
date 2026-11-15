# size 25 14 15
# camp/command_tent: the commander's tent and war table on the ridge plateau (floor at world y 76).
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# The canopy steps down from the ridge at y 13 (x 12) to the eaves at y 6 (x 0 and 24); gray bands over the pole lines z 0, 7 and 13.

# the floor: spruce planks with a dark oak border, and a porch under the opening
fill ~0 ~0 ~0 ~24 ~0 ~0 minecraft:dark_oak_planks
fill ~0 ~0 ~1 ~0 ~0 ~13 minecraft:dark_oak_planks
fill ~1 ~0 ~1 ~23 ~0 ~13 minecraft:spruce_planks
fill ~6 ~0 ~14 ~15 ~0 ~14 minecraft:spruce_planks
fill ~17 ~0 ~14 ~18 ~0 ~14 minecraft:spruce_planks
fill ~24 ~0 ~1 ~24 ~0 ~13 minecraft:dark_oak_planks

# a worn band of coarse dirt in front of the tent either side of the porch
fill ~0 ~0 ~14 ~5 ~0 ~14 minecraft:coarse_dirt
setblock ~16 ~0 ~14 minecraft:polished_blackstone
fill ~19 ~0 ~14 ~24 ~0 ~14 minecraft:coarse_dirt

# the poles: dark oak logs at the corners, the sides and the ridge ends
fill ~0 ~1 ~0 ~0 ~5 ~0 minecraft:dark_oak_log
fill ~0 ~1 ~7 ~0 ~5 ~7 minecraft:dark_oak_log
fill ~0 ~1 ~13 ~0 ~5 ~13 minecraft:dark_oak_log
fill ~6 ~1 ~0 ~6 ~8 ~0 minecraft:dark_oak_log
fill ~7 ~1 ~13 ~7 ~8 ~13 minecraft:dark_oak_log
fill ~12 ~1 ~0 ~12 ~11 ~0 minecraft:dark_oak_log
fill ~17 ~1 ~13 ~17 ~8 ~13 minecraft:dark_oak_log
fill ~18 ~1 ~0 ~18 ~8 ~0 minecraft:dark_oak_log
fill ~24 ~1 ~0 ~24 ~5 ~0 minecraft:dark_oak_log
fill ~24 ~1 ~7 ~24 ~5 ~7 minecraft:dark_oak_log
fill ~24 ~1 ~13 ~24 ~5 ~13 minecraft:dark_oak_log

# the walls: the back wall and the closed back half of the sides, spruce boards two courses high under black canvas
fill ~0 ~1 ~1 ~0 ~2 ~6 minecraft:spruce_planks
fill ~1 ~1 ~0 ~5 ~2 ~0 minecraft:spruce_planks
fill ~7 ~1 ~0 ~11 ~2 ~0 minecraft:spruce_planks
fill ~13 ~1 ~0 ~17 ~2 ~0 minecraft:spruce_planks
fill ~19 ~1 ~0 ~23 ~2 ~0 minecraft:spruce_planks
fill ~24 ~1 ~1 ~24 ~2 ~6 minecraft:spruce_planks
fill ~0 ~3 ~1 ~0 ~5 ~6 minecraft:black_wool
fill ~1 ~3 ~0 ~5 ~6 ~0 minecraft:black_wool
fill ~7 ~3 ~0 ~11 ~8 ~0 minecraft:black_wool
fill ~13 ~3 ~0 ~17 ~8 ~0 minecraft:black_wool
fill ~19 ~3 ~0 ~23 ~6 ~0 minecraft:black_wool
fill ~24 ~3 ~1 ~24 ~5 ~6 minecraft:black_wool
fill ~3 ~7 ~0 ~5 ~7 ~0 minecraft:black_wool
fill ~19 ~7 ~0 ~21 ~7 ~0 minecraft:black_wool
fill ~8 ~9 ~0 ~11 ~9 ~0 minecraft:black_wool
fill ~13 ~9 ~0 ~16 ~9 ~0 minecraft:black_wool
fill ~10 ~10 ~0 ~11 ~10 ~0 minecraft:black_wool
fill ~13 ~10 ~0 ~14 ~10 ~0 minecraft:black_wool
setblock ~11 ~11 ~0 minecraft:black_wool
setblock ~13 ~11 ~0 minecraft:black_wool

# the canopy: black and gray wool stepping from the ridge to the eaves
fill ~0 ~6 ~0 ~0 ~6 ~13 minecraft:gray_wool
fill ~24 ~6 ~0 ~24 ~6 ~13 minecraft:gray_wool
fill ~1 ~7 ~0 ~2 ~7 ~0 minecraft:gray_wool
fill ~1 ~7 ~1 ~2 ~7 ~6 minecraft:black_wool
fill ~1 ~7 ~7 ~2 ~7 ~7 minecraft:gray_wool
fill ~1 ~7 ~8 ~2 ~7 ~12 minecraft:black_wool
fill ~1 ~7 ~13 ~2 ~7 ~13 minecraft:gray_wool
fill ~22 ~7 ~0 ~23 ~7 ~0 minecraft:gray_wool
fill ~22 ~7 ~1 ~23 ~7 ~6 minecraft:black_wool
fill ~22 ~7 ~7 ~23 ~7 ~7 minecraft:gray_wool
fill ~22 ~7 ~8 ~23 ~7 ~12 minecraft:black_wool
fill ~22 ~7 ~13 ~23 ~7 ~13 minecraft:gray_wool
fill ~3 ~8 ~0 ~5 ~8 ~0 minecraft:gray_wool
fill ~3 ~8 ~1 ~5 ~8 ~6 minecraft:black_wool
fill ~3 ~8 ~7 ~5 ~8 ~7 minecraft:gray_wool
fill ~3 ~8 ~8 ~5 ~8 ~12 minecraft:black_wool
fill ~3 ~8 ~13 ~5 ~8 ~13 minecraft:gray_wool
fill ~19 ~8 ~0 ~21 ~8 ~0 minecraft:gray_wool
fill ~19 ~8 ~1 ~21 ~8 ~6 minecraft:black_wool
fill ~19 ~8 ~7 ~21 ~8 ~7 minecraft:gray_wool
fill ~19 ~8 ~8 ~21 ~8 ~12 minecraft:black_wool
fill ~19 ~8 ~13 ~21 ~8 ~13 minecraft:gray_wool
fill ~6 ~9 ~0 ~7 ~9 ~0 minecraft:gray_wool
fill ~6 ~9 ~1 ~7 ~9 ~6 minecraft:black_wool
fill ~6 ~9 ~7 ~7 ~9 ~7 minecraft:gray_wool
fill ~6 ~9 ~8 ~7 ~9 ~12 minecraft:black_wool
setblock ~6 ~9 ~13 minecraft:gray_wool
fill ~17 ~9 ~0 ~18 ~9 ~0 minecraft:gray_wool
fill ~17 ~9 ~1 ~18 ~9 ~6 minecraft:black_wool
fill ~17 ~9 ~7 ~18 ~9 ~7 minecraft:gray_wool
fill ~17 ~9 ~8 ~18 ~9 ~12 minecraft:black_wool
setblock ~18 ~9 ~13 minecraft:gray_wool
fill ~8 ~10 ~0 ~9 ~10 ~0 minecraft:gray_wool
fill ~8 ~10 ~1 ~9 ~10 ~6 minecraft:black_wool
fill ~8 ~10 ~7 ~9 ~10 ~7 minecraft:gray_wool
fill ~8 ~10 ~8 ~9 ~10 ~12 minecraft:black_wool
fill ~8 ~10 ~13 ~9 ~10 ~13 minecraft:gray_wool
fill ~15 ~10 ~0 ~16 ~10 ~0 minecraft:gray_wool
fill ~15 ~10 ~1 ~16 ~10 ~6 minecraft:black_wool
fill ~15 ~10 ~7 ~16 ~10 ~7 minecraft:gray_wool
fill ~15 ~10 ~8 ~16 ~10 ~12 minecraft:black_wool
fill ~15 ~10 ~13 ~16 ~10 ~13 minecraft:gray_wool
setblock ~10 ~11 ~0 minecraft:gray_wool
fill ~10 ~11 ~1 ~10 ~11 ~6 minecraft:black_wool
setblock ~10 ~11 ~7 minecraft:gray_wool
fill ~10 ~11 ~8 ~10 ~11 ~12 minecraft:black_wool
setblock ~10 ~11 ~13 minecraft:gray_wool
setblock ~14 ~11 ~0 minecraft:gray_wool
fill ~14 ~11 ~1 ~14 ~11 ~6 minecraft:black_wool
setblock ~14 ~11 ~7 minecraft:gray_wool
fill ~14 ~11 ~8 ~14 ~11 ~12 minecraft:black_wool
setblock ~14 ~11 ~13 minecraft:gray_wool
setblock ~11 ~12 ~0 minecraft:gray_wool
fill ~11 ~12 ~1 ~11 ~12 ~6 minecraft:black_wool
setblock ~11 ~12 ~7 minecraft:gray_wool
fill ~11 ~12 ~8 ~11 ~12 ~12 minecraft:black_wool
setblock ~11 ~12 ~13 minecraft:gray_wool
setblock ~13 ~12 ~0 minecraft:gray_wool
fill ~13 ~12 ~1 ~13 ~12 ~6 minecraft:black_wool
setblock ~13 ~12 ~7 minecraft:gray_wool
fill ~13 ~12 ~8 ~13 ~12 ~12 minecraft:black_wool
setblock ~13 ~12 ~13 minecraft:gray_wool
setblock ~12 ~13 ~0 minecraft:gray_wool
fill ~12 ~13 ~1 ~12 ~13 ~6 minecraft:black_wool
setblock ~12 ~13 ~7 minecraft:gray_wool
fill ~12 ~13 ~8 ~12 ~13 ~12 minecraft:black_wool
setblock ~12 ~13 ~13 minecraft:gray_wool

# the ridge beam and the front lintel (dark oak logs)
fill ~7 ~9 ~13 ~17 ~9 ~13 minecraft:dark_oak_log[axis=x]
fill ~12 ~12 ~0 ~12 ~12 ~13 minecraft:dark_oak_log[axis=z]

# the lining: pale canvas under the black, stepped with it, so the room reads by day (the ridge beam shows between its halves)
fill ~1 ~6 ~1 ~2 ~6 ~12 minecraft:light_gray_wool
fill ~22 ~6 ~1 ~23 ~6 ~12 minecraft:light_gray_wool
fill ~3 ~7 ~1 ~5 ~7 ~12 minecraft:light_gray_wool
fill ~19 ~7 ~1 ~21 ~7 ~12 minecraft:light_gray_wool
fill ~6 ~8 ~1 ~7 ~8 ~12 minecraft:light_gray_wool
fill ~17 ~8 ~1 ~18 ~8 ~12 minecraft:light_gray_wool
fill ~8 ~9 ~1 ~9 ~9 ~12 minecraft:light_gray_wool
fill ~15 ~9 ~1 ~16 ~9 ~12 minecraft:light_gray_wool
fill ~10 ~10 ~1 ~10 ~10 ~12 minecraft:light_gray_wool
fill ~14 ~10 ~1 ~14 ~10 ~12 minecraft:light_gray_wool
fill ~11 ~11 ~1 ~11 ~11 ~12 minecraft:light_gray_wool
fill ~13 ~11 ~1 ~13 ~11 ~12 minecraft:light_gray_wool

# the front gable above the lintel, closed with black canvas
fill ~10 ~10 ~13 ~14 ~10 ~13 minecraft:black_wool
fill ~11 ~11 ~13 ~13 ~11 ~13 minecraft:black_wool

# the war table: dark oak top at y 1 (planks inside, an upturned-stair lip around); the carpet cells x 5..19, z 2..10, y 2 stay void
fill ~4 ~1 ~1 ~4 ~1 ~11 minecraft:dark_oak_stairs[facing=east,half=top]
fill ~5 ~1 ~1 ~19 ~1 ~1 minecraft:dark_oak_stairs[facing=south,half=top]
fill ~5 ~1 ~2 ~19 ~1 ~10 minecraft:dark_oak_planks
fill ~5 ~1 ~11 ~19 ~1 ~11 minecraft:dark_oak_stairs[facing=north,half=top]
fill ~20 ~1 ~1 ~20 ~1 ~11 minecraft:dark_oak_stairs[facing=west,half=top]

# the commander's chair at the west head of the table, and the map chests and stores
fill ~1 ~1 ~1 ~1 ~2 ~1 minecraft:barrel[facing=up]
setblock ~1 ~1 ~2 minecraft:barrel[facing=east]
setblock ~2 ~1 ~1 minecraft:chest[facing=south]
setblock ~3 ~1 ~6 minecraft:dark_oak_stairs[facing=west]
setblock ~22 ~1 ~1 minecraft:chest[facing=south]
setblock ~23 ~1 ~1 minecraft:barrel[facing=up]
setblock ~23 ~1 ~2 minecraft:hay_block

# the Quartermaster's armoury behind his place (he stands at x 22.5, z 9.5): smithing table, anvil, grindstone, barrels
setblock ~23 ~1 ~12 minecraft:barrel[facing=west]
setblock ~24 ~1 ~8 minecraft:smithing_table
setblock ~24 ~1 ~9 minecraft:anvil[facing=north]
setblock ~24 ~1 ~10 minecraft:grindstone[face=floor,facing=west]
fill ~24 ~1 ~11 ~24 ~2 ~11 minecraft:barrel[facing=up]

# a gray runner in front of the table
fill ~5 ~1 ~12 ~19 ~1 ~12 minecraft:gray_carpet

# the standard pole: dark_oak_fence at (16, 1..11, 14), the anchor for in-world banner cosmetics
fill ~16 ~1 ~14 ~16 ~11 ~14 minecraft:dark_oak_fence

# braziers at the front corners: a polished blackstone wall post with a lit campfire on it
setblock ~2 ~1 ~14 minecraft:polished_blackstone_wall
setblock ~22 ~1 ~14 minecraft:polished_blackstone_wall

# chains from the ridge beam and the canopy
setblock ~3 ~6 ~3 minecraft:chain[axis=y]
setblock ~21 ~6 ~3 minecraft:chain[axis=y]
setblock ~6 ~7 ~3 minecraft:chain[axis=y]
setblock ~6 ~7 ~9 minecraft:chain[axis=y]
setblock ~18 ~7 ~3 minecraft:chain[axis=y]
setblock ~18 ~7 ~9 minecraft:chain[axis=y]
fill ~12 ~8 ~2 ~12 ~11 ~2 minecraft:chain[axis=y]
fill ~12 ~8 ~6 ~12 ~11 ~6 minecraft:chain[axis=y]
fill ~12 ~8 ~10 ~12 ~11 ~10 minecraft:chain[axis=y]

# lanterns hung on the chains and under the lintel
setblock ~3 ~5 ~3 minecraft:lantern[hanging=true]
setblock ~21 ~5 ~3 minecraft:lantern[hanging=true]
setblock ~6 ~6 ~3 minecraft:lantern[hanging=true]
setblock ~6 ~6 ~9 minecraft:lantern[hanging=true]
setblock ~18 ~6 ~3 minecraft:lantern[hanging=true]
setblock ~18 ~6 ~9 minecraft:lantern[hanging=true]
setblock ~12 ~7 ~2 minecraft:lantern[hanging=true]
setblock ~12 ~7 ~6 minecraft:lantern[hanging=true]
setblock ~12 ~7 ~10 minecraft:lantern[hanging=true]
setblock ~12 ~8 ~13 minecraft:lantern[hanging=true]

# campfires on the braziers
setblock ~2 ~2 ~14 minecraft:campfire[lit=true]
setblock ~22 ~2 ~14 minecraft:campfire[lit=true]

# banners: the camp standard on its pole, the war council banners over the table, the sigil on the front gable
setblock ~7 ~4 ~1 minecraft:black_wall_banner[facing=south]{patterns:[{pattern:"minecraft:stripe_bottom",color:"gray"},{pattern:"minecraft:border",color:"yellow"},{pattern:"minecraft:rhombus",color:"yellow"}]}
setblock ~17 ~4 ~1 minecraft:black_wall_banner[facing=south]{patterns:[{pattern:"minecraft:stripe_bottom",color:"gray"},{pattern:"minecraft:border",color:"yellow"},{pattern:"minecraft:rhombus",color:"yellow"}]}
setblock ~12 ~5 ~1 minecraft:black_wall_banner[facing=south]{patterns:[{pattern:"minecraft:border",color:"yellow"},{pattern:"minecraft:circle",color:"yellow"}]}
setblock ~12 ~11 ~14 minecraft:black_wall_banner[facing=south]{patterns:[{pattern:"minecraft:border",color:"yellow"},{pattern:"minecraft:circle",color:"yellow"}]}
setblock ~16 ~12 ~14 minecraft:black_banner[rotation=0]{patterns:[{pattern:"minecraft:border",color:"yellow"},{pattern:"minecraft:circle",color:"yellow"}]}
