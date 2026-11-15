# size 29 18 23
# hollowwood/chapel: broken chapel of Hollowwood: a ruined deepslate nave with its south door and window still standing, a fallen north wall, a ruined rose window, the stump of a bell tower with its bell, a chancel with the altar under a lancet, torn rafters, pews and rubble, and a graveyard with a dead tree and soul lanterns on its fence posts.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# Nave x 2..20, z 9..22 (south wall z 22: door x 5..9 y 1..4, window x 11..14 y 2..3, pier x 15..16 y 1..9); chancel x 20..26, z 12..19 (arch x 20, z 14..17, y 1..6; altar x 24..25, z 15..16); tower x 2..8, z 3..9; graveyard x 10..27, z 0..8.
# Kept clear: x 11..16, z 19..21, y 1..3 (the start of the cover and sight lines); cover (20.5, 2.6, 15.5) is stopped by the pier x 15..16 of the south wall, sight (14.5, 2.6, 20.5) passes the window at about x 13.

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~0 ~1 ~0 ~9 ~1 ~2 minecraft:air
fill ~0 ~1 ~3 ~1 ~1 ~7 minecraft:air
fill ~0 ~1 ~8 ~0 ~17 ~22 minecraft:air
fill ~1 ~1 ~9 ~1 ~8 ~9 minecraft:air
fill ~1 ~1 ~11 ~1 ~1 ~22 minecraft:air
fill ~3 ~1 ~4 ~7 ~1 ~8 minecraft:air
fill ~3 ~1 ~10 ~6 ~1 ~10 minecraft:air
fill ~3 ~1 ~11 ~5 ~2 ~11 minecraft:air
fill ~3 ~1 ~13 ~3 ~1 ~15 minecraft:air
fill ~4 ~1 ~12 ~5 ~6 ~16 minecraft:air
fill ~4 ~1 ~17 ~4 ~6 ~19 minecraft:air
fill ~5 ~1 ~18 ~5 ~4 ~21 minecraft:air
fill ~6 ~1 ~15 ~6 ~13 ~16 minecraft:air
fill ~6 ~1 ~19 ~6 ~8 ~21 minecraft:air
fill ~7 ~1 ~11 ~7 ~7 ~12 minecraft:air
fill ~7 ~1 ~14 ~7 ~4 ~21 minecraft:air
fill ~8 ~1 ~10 ~9 ~4 ~10 minecraft:air
fill ~8 ~1 ~15 ~8 ~7 ~16 minecraft:air
fill ~8 ~1 ~19 ~9 ~6 ~21 minecraft:air
fill ~9 ~1 ~3 ~9 ~1 ~8 minecraft:air
fill ~9 ~1 ~11 ~9 ~7 ~17 minecraft:air
fill ~10 ~1 ~1 ~14 ~17 ~1 minecraft:air
fill ~10 ~1 ~2 ~10 ~2 ~8 minecraft:air
fill ~10 ~1 ~12 ~10 ~17 ~12 minecraft:air
fill ~10 ~1 ~14 ~13 ~4 ~21 minecraft:air
fill ~11 ~1 ~5 ~11 ~17 ~5 minecraft:air
fill ~11 ~1 ~11 ~11 ~17 ~12 minecraft:air
fill ~12 ~1 ~2 ~13 ~17 ~8 minecraft:air
fill ~13 ~1 ~10 ~14 ~17 ~10 minecraft:air
fill ~14 ~1 ~5 ~14 ~17 ~5 minecraft:air
fill ~14 ~1 ~11 ~14 ~2 ~11 minecraft:air
fill ~14 ~1 ~13 ~15 ~6 ~21 minecraft:air
fill ~15 ~1 ~0 ~15 ~1 ~8 minecraft:air
fill ~16 ~1 ~1 ~18 ~17 ~1 minecraft:air
fill ~16 ~1 ~2 ~16 ~17 ~8 minecraft:air
fill ~16 ~1 ~10 ~19 ~3 ~10 minecraft:air
fill ~16 ~1 ~14 ~16 ~8 ~21 minecraft:air
fill ~17 ~1 ~5 ~17 ~17 ~5 minecraft:air
fill ~17 ~1 ~11 ~17 ~17 ~21 minecraft:air
fill ~18 ~1 ~2 ~19 ~1 ~8 minecraft:air
fill ~18 ~1 ~15 ~18 ~17 ~21 minecraft:air
fill ~19 ~1 ~0 ~21 ~17 ~1 minecraft:air
fill ~19 ~1 ~11 ~19 ~3 ~11 minecraft:air
fill ~19 ~1 ~13 ~19 ~7 ~14 minecraft:air
fill ~19 ~1 ~17 ~19 ~2 ~19 minecraft:air
setblock ~19 ~1 ~21 minecraft:air
fill ~20 ~1 ~5 ~20 ~17 ~5 minecraft:air
fill ~21 ~1 ~2 ~21 ~1 ~11 minecraft:air
fill ~21 ~1 ~13 ~25 ~1 ~13 minecraft:air
fill ~21 ~1 ~15 ~21 ~2 ~18 minecraft:air
fill ~21 ~1 ~20 ~26 ~2 ~22 minecraft:air
fill ~22 ~1 ~1 ~22 ~17 ~1 minecraft:air
fill ~22 ~1 ~3 ~22 ~5 ~11 minecraft:air
fill ~22 ~1 ~14 ~23 ~17 ~16 minecraft:air
fill ~22 ~1 ~18 ~22 ~3 ~18 minecraft:air
fill ~23 ~1 ~4 ~23 ~6 ~5 minecraft:air
fill ~23 ~1 ~9 ~23 ~1 ~11 minecraft:air
fill ~23 ~1 ~17 ~23 ~17 ~17 minecraft:air
fill ~24 ~1 ~1 ~27 ~17 ~1 minecraft:air
fill ~24 ~1 ~3 ~24 ~17 ~11 minecraft:air
fill ~24 ~1 ~18 ~24 ~17 ~18 minecraft:air
fill ~25 ~1 ~2 ~25 ~7 ~11 minecraft:air
setblock ~25 ~1 ~14 minecraft:air
setblock ~25 ~1 ~17 minecraft:air
fill ~26 ~1 ~5 ~26 ~17 ~5 minecraft:air
fill ~26 ~1 ~9 ~26 ~4 ~11 minecraft:air
fill ~27 ~1 ~2 ~27 ~2 ~22 minecraft:air
fill ~28 ~1 ~0 ~28 ~17 ~22 minecraft:air
fill ~0 ~2 ~0 ~9 ~17 ~1 minecraft:air
fill ~0 ~2 ~2 ~2 ~13 ~2 minecraft:air
fill ~0 ~2 ~3 ~1 ~6 ~5 minecraft:air
fill ~0 ~2 ~6 ~0 ~17 ~7 minecraft:air
fill ~1 ~2 ~7 ~1 ~13 ~7 minecraft:air
fill ~1 ~2 ~12 ~1 ~3 ~20 minecraft:air
fill ~1 ~2 ~22 ~1 ~3 ~22 minecraft:air
fill ~3 ~2 ~4 ~6 ~2 ~6 minecraft:air
fill ~3 ~2 ~12 ~3 ~3 ~14 minecraft:air
fill ~3 ~2 ~16 ~3 ~4 ~16 minecraft:air
fill ~3 ~2 ~18 ~3 ~7 ~18 minecraft:air
fill ~4 ~2 ~2 ~9 ~2 ~2 minecraft:air
fill ~4 ~2 ~7 ~6 ~2 ~8 minecraft:air
fill ~4 ~2 ~10 ~6 ~5 ~10 minecraft:air
fill ~4 ~2 ~20 ~4 ~7 ~21 minecraft:air
fill ~5 ~2 ~17 ~6 ~12 ~17 minecraft:air
fill ~6 ~2 ~11 ~6 ~17 ~14 minecraft:air
fill ~6 ~2 ~18 ~6 ~17 ~18 minecraft:air
fill ~7 ~2 ~5 ~7 ~4 ~7 minecraft:air
fill ~7 ~2 ~13 ~7 ~17 ~13 minecraft:air
fill ~8 ~2 ~11 ~8 ~6 ~14 minecraft:air
fill ~8 ~2 ~17 ~8 ~7 ~18 minecraft:air
fill ~9 ~2 ~3 ~9 ~2 ~7 minecraft:air
fill ~9 ~2 ~18 ~9 ~11 ~18 minecraft:air
fill ~10 ~2 ~13 ~10 ~17 ~13 minecraft:air
fill ~11 ~2 ~3 ~11 ~17 ~4 minecraft:air
fill ~11 ~2 ~7 ~11 ~17 ~8 minecraft:air
fill ~13 ~2 ~9 ~13 ~17 ~9 minecraft:air
fill ~14 ~2 ~2 ~14 ~17 ~4 minecraft:air
fill ~14 ~2 ~7 ~14 ~17 ~8 minecraft:air
fill ~15 ~2 ~0 ~15 ~17 ~7 minecraft:air
fill ~15 ~2 ~10 ~15 ~17 ~11 minecraft:air
fill ~16 ~2 ~11 ~16 ~17 ~13 minecraft:air
fill ~17 ~2 ~3 ~17 ~17 ~4 minecraft:air
fill ~18 ~2 ~2 ~19 ~17 ~7 minecraft:air
fill ~18 ~2 ~8 ~18 ~17 ~8 minecraft:air
fill ~18 ~2 ~11 ~18 ~17 ~14 minecraft:air
fill ~19 ~2 ~12 ~19 ~17 ~12 minecraft:air
fill ~19 ~2 ~15 ~19 ~6 ~16 minecraft:air
fill ~20 ~2 ~3 ~20 ~17 ~4 minecraft:air
fill ~20 ~2 ~7 ~20 ~17 ~7 minecraft:air
fill ~21 ~2 ~2 ~21 ~4 ~10 minecraft:air
fill ~21 ~2 ~14 ~21 ~17 ~14 minecraft:air
fill ~22 ~2 ~13 ~25 ~2 ~13 minecraft:air
fill ~22 ~2 ~17 ~22 ~17 ~17 minecraft:air
fill ~23 ~2 ~1 ~23 ~5 ~1 minecraft:air
fill ~23 ~2 ~7 ~23 ~17 ~10 minecraft:air
fill ~23 ~2 ~18 ~23 ~17 ~18 minecraft:air
setblock ~24 ~2 ~2 minecraft:air
fill ~24 ~2 ~14 ~24 ~17 ~14 minecraft:air
fill ~24 ~2 ~17 ~24 ~17 ~17 minecraft:air
fill ~26 ~2 ~2 ~26 ~8 ~4 minecraft:air
fill ~26 ~2 ~6 ~26 ~17 ~6 minecraft:air
fill ~26 ~2 ~8 ~26 ~17 ~8 minecraft:air
fill ~3 ~3 ~5 ~3 ~3 ~6 minecraft:air
fill ~3 ~3 ~19 ~3 ~4 ~20 minecraft:air
fill ~4 ~3 ~2 ~4 ~17 ~2 minecraft:air
fill ~4 ~3 ~4 ~6 ~3 ~7 minecraft:air
fill ~4 ~3 ~8 ~5 ~3 ~8 minecraft:air
fill ~4 ~3 ~11 ~5 ~7 ~11 minecraft:air
fill ~6 ~3 ~2 ~11 ~5 ~2 minecraft:air
fill ~7 ~3 ~8 ~7 ~4 ~8 minecraft:air
fill ~9 ~3 ~4 ~9 ~4 ~7 minecraft:air
fill ~10 ~3 ~3 ~10 ~17 ~7 minecraft:air
fill ~10 ~3 ~10 ~11 ~17 ~10 minecraft:air
fill ~10 ~3 ~11 ~10 ~17 ~11 minecraft:air
fill ~11 ~3 ~0 ~13 ~17 ~0 minecraft:air
fill ~11 ~3 ~6 ~11 ~17 ~6 minecraft:air
fill ~11 ~3 ~13 ~12 ~7 ~13 minecraft:air
fill ~12 ~3 ~9 ~12 ~17 ~11 minecraft:air
fill ~13 ~3 ~11 ~13 ~17 ~11 minecraft:air
fill ~14 ~3 ~6 ~14 ~17 ~6 minecraft:air
fill ~15 ~3 ~12 ~15 ~17 ~12 minecraft:air
fill ~16 ~3 ~0 ~17 ~17 ~0 minecraft:air
fill ~17 ~3 ~2 ~17 ~17 ~2 minecraft:air
fill ~17 ~3 ~6 ~17 ~17 ~7 minecraft:air
fill ~19 ~3 ~17 ~19 ~17 ~17 minecraft:air
fill ~19 ~3 ~19 ~19 ~3 ~21 minecraft:air
fill ~20 ~3 ~2 ~20 ~7 ~2 minecraft:air
fill ~20 ~3 ~6 ~20 ~17 ~6 minecraft:air
setblock ~20 ~3 ~8 minecraft:air
fill ~21 ~3 ~15 ~21 ~17 ~17 minecraft:air
fill ~21 ~3 ~21 ~21 ~17 ~22 minecraft:air
fill ~22 ~3 ~13 ~23 ~3 ~13 minecraft:air
fill ~22 ~3 ~20 ~26 ~3 ~22 minecraft:air
fill ~23 ~3 ~0 ~25 ~17 ~0 minecraft:air
fill ~23 ~3 ~6 ~23 ~17 ~6 minecraft:air
fill ~24 ~3 ~12 ~24 ~17 ~13 minecraft:air
fill ~24 ~3 ~15 ~24 ~17 ~16 minecraft:air
fill ~25 ~3 ~14 ~25 ~17 ~14 minecraft:air
fill ~25 ~3 ~17 ~25 ~17 ~17 minecraft:air
fill ~26 ~3 ~7 ~26 ~17 ~7 minecraft:air
fill ~27 ~3 ~0 ~27 ~17 ~0 minecraft:air
fill ~27 ~3 ~2 ~27 ~17 ~13 minecraft:air
fill ~27 ~3 ~15 ~27 ~5 ~22 minecraft:air
fill ~1 ~4 ~11 ~1 ~5 ~14 minecraft:air
fill ~1 ~4 ~16 ~1 ~6 ~20 minecraft:air
fill ~3 ~4 ~6 ~3 ~14 ~6 minecraft:air
fill ~3 ~4 ~8 ~3 ~5 ~8 minecraft:air
fill ~3 ~4 ~10 ~3 ~5 ~10 minecraft:air
fill ~3 ~4 ~12 ~3 ~4 ~13 minecraft:air
fill ~3 ~4 ~15 ~3 ~11 ~15 minecraft:air
fill ~4 ~4 ~5 ~4 ~8 ~8 minecraft:air
fill ~5 ~4 ~4 ~6 ~4 ~7 minecraft:air
fill ~10 ~4 ~8 ~10 ~17 ~8 minecraft:air
fill ~12 ~4 ~12 ~13 ~17 ~12 minecraft:air
fill ~13 ~4 ~13 ~13 ~17 ~13 minecraft:air
fill ~14 ~4 ~11 ~14 ~17 ~12 minecraft:air
fill ~15 ~4 ~8 ~15 ~17 ~8 minecraft:air
fill ~16 ~4 ~10 ~18 ~4 ~10 minecraft:air
fill ~17 ~4 ~8 ~17 ~17 ~8 minecraft:air
fill ~19 ~4 ~18 ~19 ~17 ~20 minecraft:air
fill ~21 ~4 ~11 ~21 ~17 ~11 minecraft:air
fill ~21 ~4 ~20 ~22 ~4 ~20 minecraft:air
fill ~22 ~4 ~21 ~23 ~17 ~22 minecraft:air
fill ~23 ~4 ~11 ~23 ~17 ~11 minecraft:air
fill ~23 ~4 ~13 ~23 ~17 ~13 minecraft:air
fill ~24 ~4 ~20 ~25 ~17 ~22 minecraft:air
fill ~25 ~4 ~15 ~25 ~17 ~16 minecraft:air
fill ~25 ~4 ~18 ~25 ~17 ~18 minecraft:air
fill ~26 ~4 ~21 ~26 ~17 ~22 minecraft:air
setblock ~1 ~5 ~6 minecraft:air
fill ~1 ~5 ~21 ~1 ~17 ~21 minecraft:air
fill ~3 ~5 ~2 ~3 ~6 ~2 minecraft:air
fill ~3 ~5 ~4 ~3 ~6 ~4 minecraft:air
fill ~3 ~5 ~7 ~3 ~17 ~7 minecraft:air
fill ~3 ~5 ~11 ~3 ~7 ~11 minecraft:air
fill ~3 ~5 ~13 ~3 ~11 ~14 minecraft:air
fill ~3 ~5 ~20 ~3 ~6 ~20 minecraft:air
fill ~5 ~5 ~5 ~5 ~9 ~8 minecraft:air
fill ~5 ~5 ~18 ~5 ~9 ~20 minecraft:air
fill ~6 ~5 ~4 ~6 ~6 ~8 minecraft:air
fill ~7 ~5 ~6 ~7 ~7 ~7 minecraft:air
fill ~7 ~5 ~14 ~7 ~7 ~20 minecraft:air
fill ~9 ~5 ~3 ~9 ~17 ~3 minecraft:air
fill ~9 ~5 ~5 ~9 ~5 ~8 minecraft:air
setblock ~9 ~5 ~10 minecraft:air
fill ~10 ~5 ~0 ~10 ~17 ~0 minecraft:air
fill ~10 ~5 ~14 ~13 ~6 ~20 minecraft:air
fill ~10 ~5 ~21 ~10 ~8 ~21 minecraft:air
fill ~11 ~5 ~9 ~11 ~17 ~9 minecraft:air
fill ~12 ~5 ~21 ~13 ~6 ~21 minecraft:air
fill ~14 ~5 ~0 ~14 ~17 ~0 minecraft:air
fill ~14 ~5 ~9 ~14 ~17 ~9 minecraft:air
fill ~17 ~5 ~10 ~17 ~17 ~10 minecraft:air
fill ~18 ~5 ~0 ~18 ~17 ~0 minecraft:air
fill ~18 ~5 ~9 ~18 ~17 ~10 minecraft:air
fill ~19 ~5 ~8 ~19 ~17 ~9 minecraft:air
fill ~19 ~5 ~21 ~20 ~17 ~21 minecraft:air
fill ~20 ~5 ~22 ~20 ~17 ~22 minecraft:air
fill ~21 ~5 ~2 ~22 ~5 ~2 minecraft:air
fill ~21 ~5 ~3 ~21 ~17 ~8 minecraft:air
fill ~22 ~5 ~0 ~22 ~17 ~0 minecraft:air
fill ~22 ~5 ~13 ~22 ~17 ~13 minecraft:air
fill ~22 ~5 ~18 ~22 ~17 ~18 minecraft:air
fill ~22 ~5 ~20 ~23 ~17 ~20 minecraft:air
fill ~23 ~5 ~3 ~23 ~6 ~3 minecraft:air
fill ~23 ~5 ~19 ~25 ~17 ~19 minecraft:air
fill ~24 ~5 ~2 ~24 ~6 ~2 minecraft:air
fill ~25 ~5 ~12 ~25 ~17 ~13 minecraft:air
fill ~26 ~5 ~0 ~26 ~17 ~0 minecraft:air
fill ~26 ~5 ~9 ~26 ~17 ~10 minecraft:air
fill ~26 ~5 ~20 ~26 ~17 ~20 minecraft:air
fill ~27 ~5 ~14 ~27 ~17 ~14 minecraft:air
fill ~1 ~6 ~12 ~1 ~7 ~14 minecraft:air
fill ~1 ~6 ~22 ~1 ~17 ~22 minecraft:air
fill ~3 ~6 ~5 ~3 ~14 ~5 minecraft:air
fill ~3 ~6 ~16 ~3 ~6 ~17 minecraft:air
setblock ~4 ~6 ~4 minecraft:air
setblock ~4 ~6 ~10 minecraft:air
fill ~5 ~6 ~2 ~6 ~10 ~2 minecraft:air
fill ~5 ~6 ~21 ~5 ~8 ~21 minecraft:air
fill ~6 ~6 ~10 ~7 ~7 ~10 minecraft:air
fill ~7 ~6 ~21 ~7 ~8 ~21 minecraft:air
fill ~8 ~6 ~2 ~11 ~17 ~2 minecraft:air
fill ~9 ~6 ~6 ~9 ~10 ~7 minecraft:air
fill ~10 ~6 ~9 ~10 ~17 ~9 minecraft:air
fill ~17 ~6 ~9 ~17 ~17 ~9 minecraft:air
fill ~19 ~6 ~10 ~19 ~17 ~11 minecraft:air
fill ~20 ~6 ~20 ~20 ~17 ~20 minecraft:air
setblock ~21 ~6 ~2 minecraft:air
fill ~21 ~6 ~10 ~21 ~17 ~10 minecraft:air
fill ~21 ~6 ~12 ~21 ~17 ~12 minecraft:air
fill ~22 ~6 ~4 ~22 ~7 ~11 minecraft:air
fill ~23 ~6 ~12 ~23 ~17 ~12 minecraft:air
fill ~26 ~6 ~11 ~26 ~17 ~12 minecraft:air
fill ~27 ~6 ~15 ~27 ~7 ~18 minecraft:air
fill ~27 ~6 ~20 ~27 ~17 ~22 minecraft:air
fill ~0 ~7 ~3 ~1 ~7 ~4 minecraft:air
fill ~0 ~7 ~5 ~0 ~17 ~5 minecraft:air
setblock ~1 ~7 ~10 minecraft:air
fill ~1 ~7 ~17 ~1 ~17 ~18 minecraft:air
fill ~1 ~7 ~20 ~1 ~17 ~20 minecraft:air
fill ~3 ~7 ~16 ~3 ~17 ~16 minecraft:air
fill ~3 ~7 ~19 ~4 ~7 ~19 minecraft:air
fill ~4 ~7 ~12 ~5 ~7 ~12 minecraft:air
fill ~4 ~7 ~14 ~4 ~7 ~17 minecraft:air
fill ~5 ~7 ~13 ~5 ~7 ~16 minecraft:air
fill ~6 ~7 ~5 ~7 ~17 ~5 minecraft:air
fill ~6 ~7 ~6 ~6 ~14 ~8 minecraft:air
fill ~7 ~7 ~8 ~7 ~8 ~8 minecraft:air
fill ~8 ~7 ~10 ~9 ~7 ~10 minecraft:air
setblock ~8 ~7 ~11 minecraft:air
fill ~8 ~7 ~13 ~8 ~7 ~14 minecraft:air
setblock ~8 ~7 ~20 minecraft:air
fill ~9 ~7 ~19 ~9 ~8 ~21 minecraft:air
fill ~10 ~7 ~14 ~13 ~7 ~16 minecraft:air
fill ~10 ~7 ~17 ~11 ~7 ~20 minecraft:air
setblock ~11 ~7 ~21 minecraft:air
fill ~12 ~7 ~18 ~12 ~7 ~20 minecraft:air
fill ~13 ~7 ~17 ~13 ~7 ~21 minecraft:air
fill ~14 ~7 ~13 ~15 ~17 ~20 minecraft:air
fill ~14 ~7 ~21 ~14 ~17 ~21 minecraft:air
fill ~19 ~7 ~16 ~19 ~17 ~16 minecraft:air
fill ~19 ~7 ~22 ~19 ~17 ~22 minecraft:air
fill ~20 ~7 ~8 ~20 ~17 ~8 minecraft:air
fill ~20 ~7 ~11 ~20 ~17 ~12 minecraft:air
fill ~21 ~7 ~9 ~21 ~17 ~9 minecraft:air
fill ~21 ~7 ~19 ~22 ~17 ~19 minecraft:air
fill ~21 ~7 ~20 ~21 ~17 ~20 minecraft:air
setblock ~22 ~7 ~2 minecraft:air
fill ~22 ~7 ~12 ~22 ~17 ~12 minecraft:air
fill ~23 ~7 ~1 ~23 ~17 ~1 minecraft:air
fill ~23 ~7 ~5 ~23 ~17 ~5 minecraft:air
fill ~26 ~7 ~13 ~26 ~17 ~13 minecraft:air
fill ~26 ~7 ~19 ~27 ~17 ~19 minecraft:air
fill ~0 ~8 ~3 ~0 ~17 ~4 minecraft:air
fill ~1 ~8 ~4 ~1 ~17 ~4 minecraft:air
fill ~1 ~8 ~6 ~1 ~17 ~6 minecraft:air
fill ~1 ~8 ~11 ~1 ~17 ~12 minecraft:air
fill ~1 ~8 ~14 ~1 ~11 ~16 minecraft:air
fill ~1 ~8 ~19 ~1 ~17 ~19 minecraft:air
fill ~3 ~8 ~4 ~3 ~10 ~4 minecraft:air
fill ~3 ~8 ~17 ~3 ~12 ~17 minecraft:air
fill ~3 ~8 ~19 ~3 ~10 ~19 minecraft:air
fill ~5 ~8 ~11 ~5 ~17 ~12 minecraft:air
fill ~5 ~8 ~14 ~5 ~13 ~16 minecraft:air
fill ~6 ~8 ~10 ~6 ~17 ~10 minecraft:air
fill ~7 ~8 ~12 ~7 ~17 ~12 minecraft:air
fill ~7 ~8 ~14 ~7 ~13 ~16 minecraft:air
fill ~7 ~8 ~19 ~7 ~10 ~19 minecraft:air
fill ~9 ~8 ~4 ~9 ~9 ~5 minecraft:air
fill ~9 ~8 ~13 ~9 ~12 ~17 minecraft:air
fill ~10 ~8 ~14 ~10 ~9 ~20 minecraft:air
fill ~11 ~8 ~13 ~11 ~17 ~15 minecraft:air
fill ~11 ~8 ~18 ~11 ~17 ~20 minecraft:air
fill ~11 ~8 ~22 ~11 ~17 ~22 minecraft:air
fill ~13 ~8 ~15 ~13 ~17 ~17 minecraft:air
fill ~13 ~8 ~19 ~13 ~17 ~19 minecraft:air
fill ~13 ~8 ~22 ~14 ~17 ~22 minecraft:air
fill ~15 ~8 ~9 ~15 ~17 ~9 minecraft:air
fill ~15 ~8 ~21 ~15 ~17 ~21 minecraft:air
fill ~16 ~8 ~10 ~16 ~17 ~10 minecraft:air
fill ~18 ~8 ~22 ~18 ~17 ~22 minecraft:air
fill ~19 ~8 ~14 ~19 ~17 ~14 minecraft:air
fill ~20 ~8 ~10 ~20 ~17 ~10 minecraft:air
fill ~20 ~8 ~17 ~20 ~17 ~17 minecraft:air
fill ~20 ~8 ~19 ~20 ~17 ~19 minecraft:air
fill ~21 ~8 ~13 ~21 ~17 ~13 minecraft:air
fill ~22 ~8 ~3 ~23 ~17 ~3 minecraft:air
fill ~22 ~8 ~5 ~22 ~17 ~11 minecraft:air
fill ~25 ~8 ~3 ~25 ~17 ~11 minecraft:air
fill ~26 ~8 ~14 ~26 ~17 ~14 minecraft:air
fill ~26 ~8 ~17 ~26 ~17 ~18 minecraft:air
fill ~27 ~8 ~16 ~27 ~17 ~18 minecraft:air
fill ~1 ~9 ~3 ~1 ~17 ~3 minecraft:air
fill ~1 ~9 ~5 ~1 ~17 ~5 minecraft:air
fill ~3 ~9 ~11 ~4 ~9 ~12 minecraft:air
fill ~3 ~9 ~18 ~3 ~11 ~18 minecraft:air
setblock ~3 ~9 ~20 minecraft:air
fill ~4 ~9 ~5 ~4 ~14 ~7 minecraft:air
fill ~4 ~9 ~13 ~5 ~11 ~13 minecraft:air
fill ~4 ~9 ~14 ~4 ~9 ~20 minecraft:air
fill ~4 ~9 ~22 ~10 ~17 ~22 minecraft:air
fill ~6 ~9 ~19 ~6 ~9 ~20 minecraft:air
fill ~7 ~9 ~2 ~7 ~17 ~2 minecraft:air
setblock ~7 ~9 ~4 minecraft:air
fill ~7 ~9 ~6 ~7 ~14 ~6 minecraft:air
fill ~7 ~9 ~11 ~8 ~9 ~11 minecraft:air
fill ~7 ~9 ~17 ~7 ~11 ~18 minecraft:air
setblock ~7 ~9 ~20 minecraft:air
fill ~8 ~9 ~12 ~8 ~9 ~20 minecraft:air
fill ~9 ~9 ~8 ~9 ~17 ~12 minecraft:air
fill ~9 ~9 ~19 ~9 ~9 ~20 minecraft:air
fill ~11 ~9 ~16 ~11 ~17 ~17 minecraft:air
fill ~11 ~9 ~21 ~11 ~17 ~21 minecraft:air
fill ~12 ~9 ~13 ~12 ~9 ~20 minecraft:air
fill ~12 ~9 ~22 ~12 ~17 ~22 minecraft:air
fill ~13 ~9 ~14 ~13 ~17 ~14 minecraft:air
fill ~13 ~9 ~18 ~13 ~17 ~18 minecraft:air
fill ~13 ~9 ~20 ~13 ~17 ~21 minecraft:air
fill ~16 ~9 ~9 ~16 ~17 ~9 minecraft:air
fill ~16 ~9 ~14 ~16 ~9 ~20 minecraft:air
fill ~17 ~9 ~22 ~17 ~17 ~22 minecraft:air
fill ~19 ~9 ~13 ~19 ~17 ~13 minecraft:air
fill ~19 ~9 ~15 ~19 ~17 ~15 minecraft:air
fill ~20 ~9 ~2 ~22 ~17 ~2 minecraft:air
fill ~20 ~9 ~9 ~20 ~17 ~9 minecraft:air
fill ~20 ~9 ~16 ~20 ~17 ~16 minecraft:air
fill ~20 ~9 ~18 ~21 ~17 ~18 minecraft:air
fill ~22 ~9 ~4 ~22 ~17 ~4 minecraft:air
fill ~25 ~9 ~2 ~25 ~17 ~2 minecraft:air
fill ~26 ~9 ~3 ~26 ~17 ~4 minecraft:air
fill ~27 ~9 ~15 ~27 ~17 ~15 minecraft:air
fill ~1 ~10 ~8 ~1 ~17 ~8 minecraft:air
fill ~1 ~10 ~10 ~2 ~17 ~10 minecraft:air
fill ~1 ~10 ~13 ~1 ~17 ~13 minecraft:air
fill ~2 ~10 ~21 ~10 ~17 ~21 minecraft:air
fill ~2 ~10 ~22 ~3 ~17 ~22 minecraft:air
fill ~3 ~10 ~2 ~3 ~17 ~2 minecraft:air
fill ~3 ~10 ~11 ~3 ~17 ~12 minecraft:air
fill ~4 ~10 ~4 ~6 ~17 ~4 minecraft:air
fill ~4 ~10 ~8 ~4 ~17 ~8 minecraft:air
fill ~4 ~10 ~10 ~4 ~17 ~10 minecraft:air
setblock ~4 ~10 ~12 minecraft:air
fill ~4 ~10 ~14 ~4 ~10 ~19 minecraft:air
fill ~5 ~10 ~5 ~5 ~13 ~7 minecraft:air
fill ~5 ~10 ~18 ~5 ~10 ~19 minecraft:air
setblock ~6 ~10 ~19 minecraft:air
fill ~7 ~10 ~8 ~7 ~17 ~8 minecraft:air
fill ~7 ~10 ~10 ~8 ~17 ~10 minecraft:air
fill ~7 ~10 ~11 ~7 ~17 ~11 minecraft:air
fill ~8 ~10 ~12 ~8 ~10 ~19 minecraft:air
fill ~9 ~10 ~4 ~9 ~17 ~4 minecraft:air
setblock ~9 ~10 ~19 minecraft:air
fill ~10 ~10 ~14 ~10 ~10 ~19 minecraft:air
fill ~12 ~10 ~13 ~12 ~10 ~19 minecraft:air
fill ~12 ~10 ~21 ~12 ~17 ~21 minecraft:air
fill ~15 ~10 ~22 ~15 ~17 ~22 minecraft:air
fill ~16 ~10 ~14 ~16 ~10 ~19 minecraft:air
fill ~16 ~10 ~21 ~16 ~17 ~22 minecraft:air
fill ~20 ~10 ~13 ~20 ~17 ~15 minecraft:air
fill ~23 ~10 ~2 ~24 ~17 ~2 minecraft:air
fill ~23 ~10 ~4 ~23 ~17 ~4 minecraft:air
fill ~26 ~10 ~2 ~26 ~17 ~2 minecraft:air
fill ~26 ~10 ~15 ~26 ~17 ~15 minecraft:air
fill ~2 ~11 ~11 ~2 ~17 ~11 minecraft:air
fill ~2 ~11 ~20 ~10 ~17 ~20 minecraft:air
fill ~3 ~11 ~10 ~3 ~17 ~10 minecraft:air
fill ~4 ~11 ~11 ~4 ~17 ~11 minecraft:air
fill ~4 ~11 ~14 ~4 ~11 ~18 minecraft:air
fill ~5 ~11 ~2 ~5 ~17 ~2 minecraft:air
setblock ~5 ~11 ~18 minecraft:air
fill ~7 ~11 ~7 ~7 ~14 ~7 minecraft:air
fill ~7 ~11 ~9 ~7 ~17 ~9 minecraft:air
fill ~8 ~11 ~8 ~8 ~17 ~9 minecraft:air
fill ~8 ~11 ~11 ~8 ~17 ~11 minecraft:air
fill ~8 ~11 ~13 ~8 ~11 ~18 minecraft:air
fill ~9 ~11 ~5 ~9 ~17 ~5 minecraft:air
fill ~9 ~11 ~7 ~9 ~17 ~7 minecraft:air
fill ~10 ~11 ~14 ~10 ~11 ~18 minecraft:air
fill ~12 ~11 ~13 ~12 ~11 ~18 minecraft:air
fill ~12 ~11 ~20 ~12 ~17 ~20 minecraft:air
fill ~16 ~11 ~14 ~16 ~17 ~18 minecraft:air
fill ~16 ~11 ~20 ~16 ~17 ~20 minecraft:air
fill ~26 ~11 ~16 ~26 ~17 ~16 minecraft:air
fill ~1 ~12 ~9 ~1 ~17 ~9 minecraft:air
fill ~1 ~12 ~14 ~1 ~17 ~14 minecraft:air
fill ~1 ~12 ~16 ~1 ~17 ~16 minecraft:air
fill ~2 ~12 ~19 ~10 ~17 ~19 minecraft:air
fill ~3 ~12 ~13 ~3 ~17 ~13 minecraft:air
fill ~4 ~12 ~9 ~4 ~17 ~9 minecraft:air
fill ~4 ~12 ~12 ~4 ~17 ~12 minecraft:air
fill ~4 ~12 ~14 ~4 ~12 ~17 minecraft:air
fill ~5 ~12 ~8 ~5 ~17 ~10 minecraft:air
fill ~5 ~12 ~13 ~5 ~17 ~13 minecraft:air
fill ~6 ~12 ~9 ~6 ~17 ~9 minecraft:air
setblock ~7 ~12 ~17 minecraft:air
fill ~8 ~12 ~6 ~9 ~17 ~6 minecraft:air
fill ~8 ~12 ~7 ~8 ~17 ~7 minecraft:air
fill ~8 ~12 ~12 ~8 ~17 ~12 minecraft:air
fill ~8 ~12 ~14 ~8 ~12 ~17 minecraft:air
fill ~10 ~12 ~14 ~10 ~12 ~17 minecraft:air
fill ~12 ~12 ~13 ~12 ~12 ~17 minecraft:air
fill ~12 ~12 ~19 ~12 ~17 ~19 minecraft:air
fill ~16 ~12 ~19 ~16 ~17 ~19 minecraft:air
fill ~2 ~13 ~12 ~2 ~17 ~13 minecraft:air
setblock ~2 ~13 ~16 minecraft:air
fill ~2 ~13 ~18 ~5 ~17 ~18 minecraft:air
fill ~3 ~13 ~9 ~3 ~17 ~9 minecraft:air
fill ~3 ~13 ~14 ~3 ~13 ~15 minecraft:air
fill ~4 ~13 ~13 ~4 ~17 ~13 minecraft:air
fill ~4 ~13 ~15 ~4 ~13 ~16 minecraft:air
fill ~7 ~13 ~4 ~8 ~17 ~4 minecraft:air
fill ~7 ~13 ~18 ~10 ~17 ~18 minecraft:air
fill ~8 ~13 ~5 ~8 ~17 ~5 minecraft:air
fill ~8 ~13 ~13 ~9 ~17 ~13 minecraft:air
fill ~8 ~13 ~15 ~8 ~13 ~16 minecraft:air
fill ~9 ~13 ~14 ~10 ~13 ~16 minecraft:air
fill ~12 ~13 ~13 ~12 ~13 ~16 minecraft:air
fill ~12 ~13 ~18 ~12 ~17 ~18 minecraft:air
fill ~0 ~14 ~2 ~1 ~17 ~2 minecraft:air
fill ~1 ~14 ~15 ~1 ~17 ~15 minecraft:air
fill ~2 ~14 ~9 ~2 ~17 ~9 minecraft:air
fill ~2 ~14 ~14 ~3 ~17 ~14 minecraft:air
fill ~2 ~14 ~17 ~10 ~17 ~17 minecraft:air
fill ~3 ~14 ~8 ~3 ~17 ~8 minecraft:air
setblock ~5 ~14 ~5 minecraft:air
fill ~5 ~14 ~7 ~5 ~17 ~7 minecraft:air
fill ~5 ~14 ~14 ~5 ~17 ~14 minecraft:air
fill ~6 ~14 ~2 ~6 ~17 ~2 minecraft:air
fill ~7 ~14 ~14 ~10 ~17 ~14 minecraft:air
fill ~8 ~14 ~3 ~8 ~17 ~3 minecraft:air
fill ~12 ~14 ~13 ~12 ~17 ~14 minecraft:air
fill ~12 ~14 ~17 ~12 ~17 ~17 minecraft:air
fill ~1 ~15 ~7 ~2 ~17 ~7 minecraft:air
fill ~2 ~15 ~8 ~2 ~17 ~8 minecraft:air
fill ~2 ~15 ~15 ~3 ~17 ~15 minecraft:air
fill ~2 ~15 ~16 ~2 ~17 ~16 minecraft:air
fill ~4 ~15 ~5 ~4 ~17 ~5 minecraft:air
fill ~4 ~15 ~14 ~4 ~17 ~14 minecraft:air
fill ~5 ~15 ~15 ~10 ~17 ~15 minecraft:air
fill ~5 ~15 ~16 ~7 ~17 ~16 minecraft:air
fill ~6 ~15 ~7 ~6 ~17 ~8 minecraft:air
fill ~9 ~15 ~16 ~10 ~17 ~16 minecraft:air
fill ~12 ~15 ~15 ~12 ~17 ~16 minecraft:air
fill ~2 ~16 ~2 ~2 ~17 ~2 minecraft:air
fill ~2 ~16 ~6 ~2 ~17 ~6 minecraft:air
fill ~3 ~16 ~5 ~3 ~17 ~6 minecraft:air
fill ~4 ~16 ~7 ~4 ~17 ~7 minecraft:air
fill ~4 ~16 ~15 ~4 ~17 ~16 minecraft:air
fill ~5 ~16 ~5 ~5 ~17 ~6 minecraft:air
fill ~6 ~16 ~3 ~7 ~17 ~3 minecraft:air
fill ~6 ~16 ~6 ~6 ~17 ~6 minecraft:air
fill ~7 ~16 ~7 ~7 ~17 ~7 minecraft:air
fill ~8 ~16 ~16 ~8 ~17 ~16 minecraft:air
setblock ~2 ~17 ~5 minecraft:air
setblock ~3 ~17 ~4 minecraft:air
setblock ~4 ~17 ~6 minecraft:air
setblock ~5 ~17 ~3 minecraft:air
setblock ~7 ~17 ~6 minecraft:air

# the nave floor: deepslate tiles (one in four cracked), a strip of polished deepslate down the aisle (z 15..16), a few moss blocks; the chancel floor of polished deepslate with a chiseled step; the tower floor of cobbled deepslate
fill ~3 ~0 ~4 ~7 ~0 ~4 minecraft:cobbled_deepslate
setblock ~3 ~0 ~5 minecraft:cobbled_deepslate
setblock ~3 ~0 ~6 minecraft:moss_block
fill ~3 ~0 ~7 ~3 ~0 ~8 minecraft:cobbled_deepslate
setblock ~3 ~0 ~10 minecraft:deepslate_tiles
setblock ~3 ~0 ~11 minecraft:polished_deepslate
setblock ~3 ~0 ~12 minecraft:cobbled_deepslate
setblock ~3 ~0 ~13 minecraft:cracked_deepslate_tiles
setblock ~3 ~0 ~14 minecraft:deepslate_tiles
fill ~3 ~0 ~15 ~9 ~0 ~16 minecraft:polished_deepslate
setblock ~3 ~0 ~17 minecraft:deepslate_tiles
setblock ~3 ~0 ~18 minecraft:cobbled_deepslate
setblock ~3 ~0 ~19 minecraft:cracked_deepslate_tiles
fill ~3 ~0 ~20 ~5 ~0 ~21 minecraft:deepslate_tiles
setblock ~4 ~0 ~5 minecraft:moss_block
fill ~4 ~0 ~6 ~4 ~0 ~8 minecraft:cobbled_deepslate
setblock ~4 ~0 ~10 minecraft:moss_block
fill ~4 ~0 ~11 ~5 ~0 ~11 minecraft:deepslate_tiles
setblock ~4 ~0 ~12 minecraft:cracked_deepslate_tiles
setblock ~4 ~0 ~13 minecraft:deepslate_tiles
setblock ~4 ~0 ~14 minecraft:cobbled_deepslate
setblock ~4 ~0 ~17 minecraft:cobbled_deepslate
setblock ~4 ~0 ~18 minecraft:polished_deepslate
setblock ~4 ~0 ~19 minecraft:cobbled_deepslate
fill ~5 ~0 ~5 ~6 ~0 ~6 minecraft:cobbled_deepslate
setblock ~5 ~0 ~7 minecraft:moss_block
fill ~5 ~0 ~8 ~6 ~0 ~8 minecraft:cobbled_deepslate
setblock ~5 ~0 ~10 minecraft:cracked_deepslate_tiles
fill ~5 ~0 ~12 ~7 ~0 ~12 minecraft:deepslate_tiles
setblock ~5 ~0 ~13 minecraft:moss_block
fill ~5 ~0 ~14 ~6 ~0 ~14 minecraft:cracked_deepslate_tiles
setblock ~5 ~0 ~17 minecraft:deepslate_tiles
setblock ~5 ~0 ~18 minecraft:cracked_deepslate_tiles
setblock ~5 ~0 ~19 minecraft:polished_deepslate
fill ~6 ~0 ~7 ~7 ~0 ~7 minecraft:cobbled_deepslate
setblock ~6 ~0 ~10 minecraft:polished_deepslate
setblock ~6 ~0 ~11 minecraft:cobbled_deepslate
setblock ~6 ~0 ~13 minecraft:deepslate_tiles
fill ~6 ~0 ~17 ~7 ~0 ~17 minecraft:cracked_deepslate_tiles
fill ~6 ~0 ~18 ~6 ~0 ~20 minecraft:deepslate_tiles
setblock ~6 ~0 ~21 minecraft:polished_deepslate
fill ~7 ~0 ~5 ~7 ~0 ~6 minecraft:moss_block
setblock ~7 ~0 ~8 minecraft:moss_block
setblock ~7 ~0 ~10 minecraft:deepslate_tiles
setblock ~7 ~0 ~11 minecraft:moss_block
setblock ~7 ~0 ~13 minecraft:polished_deepslate
setblock ~7 ~0 ~14 minecraft:deepslate_tiles
setblock ~7 ~0 ~18 minecraft:cracked_deepslate_tiles
setblock ~7 ~0 ~19 minecraft:deepslate_tiles
setblock ~7 ~0 ~20 minecraft:polished_deepslate
setblock ~7 ~0 ~21 minecraft:deepslate_tiles
fill ~8 ~0 ~10 ~11 ~0 ~10 minecraft:cracked_deepslate_tiles
setblock ~8 ~0 ~11 minecraft:deepslate_tiles
setblock ~8 ~0 ~12 minecraft:cobbled_deepslate
fill ~8 ~0 ~13 ~8 ~0 ~14 minecraft:cracked_deepslate_tiles
fill ~8 ~0 ~17 ~8 ~0 ~19 minecraft:deepslate_tiles
fill ~8 ~0 ~20 ~8 ~0 ~21 minecraft:cracked_deepslate_tiles
fill ~9 ~0 ~11 ~12 ~0 ~11 minecraft:cracked_deepslate_tiles
fill ~9 ~0 ~12 ~10 ~0 ~12 minecraft:cracked_deepslate_tiles
setblock ~9 ~0 ~13 minecraft:deepslate_tiles
setblock ~9 ~0 ~14 minecraft:cracked_deepslate_tiles
setblock ~9 ~0 ~17 minecraft:cobbled_deepslate
fill ~9 ~0 ~18 ~9 ~0 ~21 minecraft:deepslate_tiles
setblock ~10 ~0 ~13 minecraft:cracked_deepslate_tiles
fill ~10 ~0 ~14 ~10 ~0 ~16 minecraft:polished_deepslate
setblock ~10 ~0 ~17 minecraft:deepslate_tiles
fill ~10 ~0 ~18 ~11 ~0 ~18 minecraft:cracked_deepslate_tiles
fill ~10 ~0 ~19 ~10 ~0 ~20 minecraft:deepslate_tiles
setblock ~10 ~0 ~21 minecraft:cracked_deepslate_tiles
setblock ~11 ~0 ~12 minecraft:moss_block
setblock ~11 ~0 ~13 minecraft:deepslate_tiles
fill ~11 ~0 ~14 ~12 ~0 ~14 minecraft:cracked_deepslate_tiles
fill ~11 ~0 ~15 ~19 ~0 ~16 minecraft:polished_deepslate
fill ~11 ~0 ~17 ~12 ~0 ~17 minecraft:polished_deepslate
fill ~11 ~0 ~19 ~12 ~0 ~19 minecraft:cracked_deepslate_tiles
setblock ~11 ~0 ~20 minecraft:polished_deepslate
fill ~11 ~0 ~21 ~12 ~0 ~21 minecraft:deepslate_tiles
setblock ~12 ~0 ~10 minecraft:deepslate_tiles
fill ~12 ~0 ~12 ~12 ~0 ~13 minecraft:polished_deepslate
setblock ~12 ~0 ~18 minecraft:cobbled_deepslate
setblock ~12 ~0 ~20 minecraft:moss_block
setblock ~13 ~0 ~10 minecraft:cracked_deepslate_tiles
setblock ~13 ~0 ~11 minecraft:polished_deepslate
setblock ~13 ~0 ~12 minecraft:deepslate_tiles
setblock ~13 ~0 ~13 minecraft:polished_deepslate
setblock ~13 ~0 ~14 minecraft:cobbled_deepslate
fill ~13 ~0 ~17 ~13 ~0 ~21 minecraft:deepslate_tiles
fill ~14 ~0 ~10 ~16 ~0 ~10 minecraft:deepslate_tiles
fill ~14 ~0 ~11 ~16 ~0 ~11 minecraft:cracked_deepslate_tiles
fill ~14 ~0 ~12 ~15 ~0 ~12 minecraft:cracked_deepslate_tiles
setblock ~14 ~0 ~13 minecraft:cobbled_deepslate
setblock ~14 ~0 ~14 minecraft:deepslate_tiles
setblock ~14 ~0 ~17 minecraft:cobbled_deepslate
fill ~14 ~0 ~18 ~14 ~0 ~19 minecraft:deepslate_tiles
fill ~14 ~0 ~20 ~16 ~0 ~20 minecraft:cracked_deepslate_tiles
fill ~14 ~0 ~21 ~15 ~0 ~21 minecraft:cracked_deepslate_tiles
fill ~15 ~0 ~13 ~15 ~0 ~14 minecraft:deepslate_tiles
fill ~15 ~0 ~17 ~15 ~0 ~18 minecraft:deepslate_tiles
setblock ~15 ~0 ~19 minecraft:polished_deepslate
fill ~16 ~0 ~12 ~17 ~0 ~13 minecraft:deepslate_tiles
setblock ~16 ~0 ~14 minecraft:deepslate_tiles
setblock ~16 ~0 ~17 minecraft:cobbled_deepslate
setblock ~16 ~0 ~18 minecraft:moss_block
fill ~16 ~0 ~19 ~17 ~0 ~19 minecraft:deepslate_tiles
setblock ~16 ~0 ~21 minecraft:polished_deepslate
setblock ~17 ~0 ~10 minecraft:cobbled_deepslate
setblock ~17 ~0 ~11 minecraft:polished_deepslate
setblock ~17 ~0 ~14 minecraft:cobbled_deepslate
setblock ~17 ~0 ~17 minecraft:deepslate_tiles
setblock ~17 ~0 ~18 minecraft:cracked_deepslate_tiles
setblock ~17 ~0 ~20 minecraft:polished_deepslate
setblock ~17 ~0 ~21 minecraft:deepslate_tiles
setblock ~18 ~0 ~10 minecraft:cracked_deepslate_tiles
fill ~18 ~0 ~11 ~19 ~0 ~11 minecraft:deepslate_tiles
fill ~18 ~0 ~12 ~19 ~0 ~12 minecraft:cobbled_deepslate
fill ~18 ~0 ~13 ~18 ~0 ~14 minecraft:deepslate_tiles
setblock ~18 ~0 ~17 minecraft:cobbled_deepslate
fill ~18 ~0 ~18 ~18 ~0 ~19 minecraft:polished_deepslate
fill ~18 ~0 ~20 ~19 ~0 ~20 minecraft:deepslate_tiles
fill ~18 ~0 ~21 ~19 ~0 ~21 minecraft:cracked_deepslate_tiles
setblock ~19 ~0 ~10 minecraft:polished_deepslate
setblock ~19 ~0 ~13 minecraft:cracked_deepslate_tiles
setblock ~19 ~0 ~14 minecraft:deepslate_tiles
fill ~19 ~0 ~17 ~19 ~0 ~18 minecraft:deepslate_tiles
setblock ~19 ~0 ~19 minecraft:cobbled_deepslate
fill ~21 ~0 ~13 ~25 ~0 ~14 minecraft:polished_deepslate
fill ~21 ~0 ~15 ~21 ~0 ~16 minecraft:chiseled_deepslate
fill ~21 ~0 ~17 ~21 ~0 ~18 minecraft:polished_deepslate
fill ~22 ~0 ~15 ~25 ~0 ~18 minecraft:polished_deepslate

# the doorstep and the way out: cracked tiles in the doorway, cobble and gravel from the door to the edge of the piece (the lane of the map goes on from here)
fill ~5 ~0 ~22 ~9 ~0 ~22 minecraft:cracked_deepslate_tiles
setblock ~20 ~0 ~14 minecraft:polished_deepslate
fill ~20 ~0 ~15 ~20 ~0 ~16 minecraft:chiseled_deepslate
setblock ~20 ~0 ~17 minecraft:polished_deepslate

# the graveyard ground: podzol, moss and coarse dirt with a worn path between the rows
setblock ~10 ~0 ~0 minecraft:podzol
fill ~10 ~0 ~1 ~10 ~0 ~2 minecraft:packed_mud
setblock ~10 ~0 ~3 minecraft:coarse_dirt
setblock ~10 ~0 ~4 minecraft:podzol
fill ~10 ~0 ~5 ~11 ~0 ~5 minecraft:gravel
fill ~10 ~0 ~6 ~10 ~0 ~8 minecraft:podzol
setblock ~11 ~0 ~0 minecraft:moss_block
fill ~11 ~0 ~1 ~11 ~0 ~3 minecraft:podzol
setblock ~11 ~0 ~4 minecraft:coarse_dirt
setblock ~11 ~0 ~6 minecraft:moss_block
fill ~11 ~0 ~7 ~12 ~0 ~8 minecraft:podzol
fill ~12 ~0 ~0 ~13 ~0 ~5 minecraft:gravel
fill ~12 ~0 ~6 ~13 ~0 ~6 minecraft:podzol
setblock ~13 ~0 ~7 minecraft:moss_block
setblock ~13 ~0 ~8 minecraft:podzol
fill ~14 ~0 ~0 ~14 ~0 ~2 minecraft:podzol
setblock ~14 ~0 ~3 minecraft:coarse_dirt
setblock ~14 ~0 ~4 minecraft:podzol
fill ~14 ~0 ~5 ~27 ~0 ~5 minecraft:gravel
setblock ~14 ~0 ~6 minecraft:coarse_dirt
setblock ~14 ~0 ~7 minecraft:podzol
setblock ~14 ~0 ~8 minecraft:coarse_dirt
setblock ~15 ~0 ~0 minecraft:coarse_dirt
fill ~15 ~0 ~1 ~16 ~0 ~1 minecraft:podzol
fill ~15 ~0 ~2 ~16 ~0 ~2 minecraft:coarse_dirt
setblock ~15 ~0 ~3 minecraft:podzol
setblock ~15 ~0 ~4 minecraft:rooted_dirt
setblock ~15 ~0 ~6 minecraft:podzol
setblock ~15 ~0 ~7 minecraft:packed_mud
setblock ~15 ~0 ~8 minecraft:podzol
setblock ~16 ~0 ~0 minecraft:moss_block
setblock ~16 ~0 ~3 minecraft:rooted_dirt
setblock ~16 ~0 ~4 minecraft:coarse_dirt
setblock ~16 ~0 ~6 minecraft:moss_block
fill ~16 ~0 ~7 ~18 ~0 ~7 minecraft:coarse_dirt
setblock ~16 ~0 ~8 minecraft:packed_mud
setblock ~17 ~0 ~0 minecraft:coarse_dirt
setblock ~17 ~0 ~1 minecraft:packed_mud
setblock ~17 ~0 ~2 minecraft:podzol
setblock ~17 ~0 ~3 minecraft:moss_block
setblock ~17 ~0 ~4 minecraft:podzol
fill ~17 ~0 ~6 ~19 ~0 ~6 minecraft:podzol
setblock ~17 ~0 ~8 minecraft:rooted_dirt
setblock ~18 ~0 ~0 minecraft:podzol
setblock ~18 ~0 ~1 minecraft:rooted_dirt
setblock ~18 ~0 ~2 minecraft:moss_block
setblock ~18 ~0 ~3 minecraft:coarse_dirt
setblock ~18 ~0 ~4 minecraft:packed_mud
fill ~18 ~0 ~8 ~20 ~0 ~8 minecraft:coarse_dirt
fill ~19 ~0 ~0 ~20 ~0 ~0 minecraft:coarse_dirt
setblock ~19 ~0 ~1 minecraft:packed_mud
fill ~19 ~0 ~2 ~20 ~0 ~2 minecraft:podzol
setblock ~19 ~0 ~3 minecraft:packed_mud
setblock ~19 ~0 ~4 minecraft:coarse_dirt
setblock ~19 ~0 ~7 minecraft:rooted_dirt
setblock ~20 ~0 ~1 minecraft:moss_block
fill ~20 ~0 ~3 ~20 ~0 ~4 minecraft:podzol
setblock ~20 ~0 ~6 minecraft:moss_block
setblock ~20 ~0 ~7 minecraft:podzol
setblock ~21 ~0 ~0 minecraft:packed_mud
setblock ~21 ~0 ~1 minecraft:coarse_dirt
setblock ~21 ~0 ~2 minecraft:moss_block
setblock ~21 ~0 ~3 minecraft:rooted_dirt
setblock ~21 ~0 ~4 minecraft:moss_block
setblock ~21 ~0 ~6 minecraft:podzol
setblock ~21 ~0 ~7 minecraft:moss_block
setblock ~21 ~0 ~8 minecraft:packed_mud
setblock ~21 ~0 ~9 minecraft:rooted_dirt
fill ~21 ~0 ~10 ~21 ~0 ~11 minecraft:moss_block
setblock ~22 ~0 ~0 minecraft:moss_block
setblock ~22 ~0 ~1 minecraft:podzol
setblock ~22 ~0 ~2 minecraft:coarse_dirt
fill ~22 ~0 ~3 ~22 ~0 ~4 minecraft:podzol
fill ~22 ~0 ~6 ~22 ~0 ~7 minecraft:moss_block
setblock ~22 ~0 ~8 minecraft:podzol
setblock ~22 ~0 ~9 minecraft:moss_block
setblock ~22 ~0 ~10 minecraft:podzol
setblock ~22 ~0 ~11 minecraft:rooted_dirt
fill ~23 ~0 ~0 ~23 ~0 ~2 minecraft:podzol
fill ~23 ~0 ~3 ~24 ~0 ~3 minecraft:rooted_dirt
fill ~23 ~0 ~4 ~24 ~0 ~4 minecraft:podzol
setblock ~23 ~0 ~6 minecraft:packed_mud
fill ~23 ~0 ~7 ~24 ~0 ~7 minecraft:rooted_dirt
setblock ~23 ~0 ~8 minecraft:rooted_dirt
setblock ~23 ~0 ~9 minecraft:packed_mud
setblock ~23 ~0 ~10 minecraft:coarse_dirt
fill ~23 ~0 ~11 ~24 ~0 ~11 minecraft:podzol
setblock ~24 ~0 ~0 minecraft:coarse_dirt
setblock ~24 ~0 ~1 minecraft:podzol
setblock ~24 ~0 ~2 minecraft:coarse_dirt
setblock ~24 ~0 ~6 minecraft:moss_block
setblock ~24 ~0 ~8 minecraft:coarse_dirt
setblock ~24 ~0 ~9 minecraft:podzol
setblock ~24 ~0 ~10 minecraft:packed_mud
setblock ~25 ~0 ~0 minecraft:packed_mud
setblock ~25 ~0 ~1 minecraft:moss_block
setblock ~25 ~0 ~2 minecraft:podzol
setblock ~25 ~0 ~3 minecraft:coarse_dirt
setblock ~25 ~0 ~4 minecraft:packed_mud
setblock ~25 ~0 ~6 minecraft:coarse_dirt
setblock ~25 ~0 ~7 minecraft:podzol
fill ~25 ~0 ~8 ~25 ~0 ~9 minecraft:moss_block
fill ~25 ~0 ~10 ~26 ~0 ~10 minecraft:podzol
setblock ~25 ~0 ~11 minecraft:moss_block
fill ~26 ~0 ~0 ~26 ~0 ~2 minecraft:podzol
setblock ~26 ~0 ~3 minecraft:rooted_dirt
setblock ~26 ~0 ~4 minecraft:podzol
setblock ~26 ~0 ~6 minecraft:podzol
setblock ~26 ~0 ~7 minecraft:rooted_dirt
setblock ~26 ~0 ~8 minecraft:coarse_dirt
fill ~26 ~0 ~9 ~27 ~0 ~9 minecraft:podzol
setblock ~26 ~0 ~11 minecraft:podzol
setblock ~27 ~0 ~0 minecraft:moss_block
fill ~27 ~0 ~1 ~27 ~0 ~2 minecraft:podzol
fill ~27 ~0 ~3 ~27 ~0 ~4 minecraft:moss_block
fill ~27 ~0 ~6 ~27 ~0 ~7 minecraft:coarse_dirt
setblock ~27 ~0 ~8 minecraft:packed_mud
setblock ~27 ~0 ~10 minecraft:packed_mud
setblock ~27 ~0 ~11 minecraft:coarse_dirt

# the south wall of the nave (z 22, x 2..20): deepslate bricks on a cobbled foot, a jagged top (9 at the west end, 8 over the door, 9 on the pier, 4 at the east end)
setblock ~3 ~1 ~22 minecraft:cobbled_deepslate
fill ~11 ~1 ~22 ~14 ~1 ~22 minecraft:deepslate_bricks
fill ~15 ~1 ~22 ~19 ~1 ~22 minecraft:cobbled_deepslate
fill ~3 ~2 ~22 ~3 ~6 ~22 minecraft:deepslate_bricks
setblock ~15 ~2 ~22 minecraft:deepslate_bricks
setblock ~16 ~2 ~22 minecraft:cracked_deepslate_bricks
fill ~17 ~2 ~22 ~19 ~2 ~22 minecraft:deepslate_bricks
setblock ~15 ~3 ~22 minecraft:cracked_deepslate_bricks
fill ~16 ~3 ~22 ~16 ~7 ~22 minecraft:deepslate_bricks
setblock ~17 ~3 ~22 minecraft:cracked_deepslate_bricks
fill ~19 ~3 ~22 ~19 ~5 ~22 minecraft:deepslate_bricks
fill ~15 ~4 ~22 ~15 ~5 ~22 minecraft:deepslate_bricks
fill ~17 ~4 ~22 ~17 ~6 ~22 minecraft:deepslate_bricks
setblock ~2 ~5 ~22 minecraft:cobbled_deepslate
setblock ~11 ~5 ~22 minecraft:cracked_deepslate_bricks
setblock ~14 ~5 ~22 minecraft:cracked_deepslate_bricks
fill ~2 ~6 ~22 ~2 ~7 ~22 minecraft:deepslate_bricks
setblock ~4 ~6 ~22 minecraft:deepslate_bricks
fill ~10 ~6 ~22 ~11 ~7 ~22 minecraft:deepslate_bricks
fill ~13 ~6 ~22 ~14 ~6 ~22 minecraft:deepslate_bricks
setblock ~15 ~6 ~22 minecraft:cracked_deepslate_bricks
setblock ~18 ~6 ~22 minecraft:deepslate_bricks
setblock ~3 ~7 ~22 minecraft:cobbled_deepslate
setblock ~4 ~7 ~22 minecraft:cracked_deepslate_bricks
fill ~5 ~7 ~22 ~9 ~7 ~22 minecraft:deepslate_bricks
setblock ~12 ~7 ~22 minecraft:deepslate_bricks
fill ~14 ~7 ~22 ~15 ~7 ~22 minecraft:deepslate_bricks
setblock ~17 ~7 ~22 minecraft:mossy_stone_bricks
setblock ~15 ~8 ~22 minecraft:deepslate_bricks
setblock ~17 ~8 ~22 minecraft:deepslate_bricks
setblock ~2 ~9 ~22 minecraft:deepslate_bricks
setblock ~3 ~9 ~22 minecraft:cracked_deepslate_bricks
setblock ~15 ~9 ~22 minecraft:mossy_stone_bricks
setblock ~16 ~9 ~22 minecraft:deepslate_bricks

# the west gable (x 2, z 9..22): stepped to a ragged apex of 13 over the aisle, broken lower on the north side
setblock ~2 ~1 ~10 minecraft:cobbled_deepslate
setblock ~2 ~1 ~11 minecraft:deepslate_bricks
fill ~2 ~1 ~12 ~2 ~1 ~14 minecraft:cobbled_deepslate
fill ~2 ~1 ~15 ~2 ~1 ~17 minecraft:deepslate_bricks
fill ~2 ~1 ~18 ~2 ~1 ~20 minecraft:cobbled_deepslate
fill ~2 ~1 ~21 ~2 ~8 ~21 minecraft:deepslate_bricks
fill ~2 ~2 ~10 ~2 ~6 ~10 minecraft:deepslate_bricks
setblock ~2 ~2 ~11 minecraft:deepslate_tiles
fill ~2 ~2 ~12 ~2 ~4 ~14 minecraft:deepslate_bricks
fill ~2 ~2 ~15 ~2 ~2 ~16 minecraft:cracked_deepslate_bricks
fill ~2 ~2 ~17 ~2 ~2 ~19 minecraft:deepslate_bricks
setblock ~2 ~2 ~20 minecraft:cracked_deepslate_bricks
fill ~2 ~3 ~11 ~2 ~5 ~11 minecraft:deepslate_bricks
fill ~2 ~3 ~15 ~2 ~3 ~18 minecraft:deepslate_bricks
setblock ~2 ~3 ~19 minecraft:cracked_deepslate_bricks
fill ~2 ~3 ~20 ~2 ~6 ~20 minecraft:deepslate_bricks
fill ~2 ~4 ~15 ~2 ~4 ~17 minecraft:deepslate_bricks
setblock ~2 ~4 ~18 minecraft:cobbled_deepslate
setblock ~2 ~4 ~19 minecraft:deepslate_bricks
fill ~2 ~5 ~12 ~2 ~6 ~12 minecraft:deepslate_bricks
setblock ~2 ~5 ~13 minecraft:cracked_deepslate_bricks
setblock ~2 ~5 ~14 minecraft:deepslate_bricks
fill ~2 ~5 ~15 ~2 ~5 ~16 minecraft:cracked_deepslate_bricks
fill ~2 ~5 ~17 ~2 ~6 ~17 minecraft:deepslate_bricks
fill ~2 ~5 ~18 ~2 ~5 ~19 minecraft:cracked_deepslate_bricks
setblock ~2 ~6 ~11 minecraft:cracked_deepslate_bricks
fill ~2 ~6 ~13 ~2 ~7 ~13 minecraft:deepslate_bricks
setblock ~2 ~6 ~14 minecraft:cracked_deepslate_bricks
fill ~2 ~6 ~15 ~2 ~6 ~16 minecraft:deepslate_bricks
setblock ~2 ~6 ~18 minecraft:mossy_stone_bricks
fill ~2 ~6 ~19 ~2 ~10 ~19 minecraft:deepslate_bricks
setblock ~2 ~7 ~10 minecraft:cracked_deepslate_bricks
setblock ~2 ~7 ~11 minecraft:deepslate_bricks
setblock ~2 ~7 ~12 minecraft:cracked_deepslate_bricks
setblock ~2 ~7 ~18 minecraft:deepslate_bricks
setblock ~2 ~7 ~20 minecraft:cracked_deepslate_bricks
setblock ~2 ~8 ~10 minecraft:deepslate_bricks
setblock ~2 ~8 ~11 minecraft:cracked_deepslate_bricks
setblock ~2 ~8 ~12 minecraft:deepslate_bricks
setblock ~2 ~8 ~20 minecraft:deepslate_bricks
setblock ~2 ~9 ~10 minecraft:deepslate_tiles
fill ~2 ~9 ~11 ~2 ~10 ~11 minecraft:deepslate_bricks
setblock ~2 ~9 ~12 minecraft:cracked_deepslate_bricks
setblock ~2 ~9 ~20 minecraft:cracked_deepslate_bricks
setblock ~2 ~10 ~12 minecraft:deepslate_bricks
setblock ~2 ~11 ~12 minecraft:cracked_deepslate_bricks
setblock ~2 ~12 ~13 minecraft:deepslate_bricks
fill ~2 ~13 ~14 ~2 ~13 ~15 minecraft:deepslate_bricks

# the north wall of the nave (z 9, x 9..20): fallen to stumps, a gap at x 13, two stubs up to 7 and 8
setblock ~9 ~1 ~9 minecraft:cobbled_deepslate
fill ~10 ~1 ~9 ~12 ~2 ~9 minecraft:deepslate_bricks
setblock ~13 ~1 ~9 minecraft:cobbled_deepslate
fill ~14 ~1 ~9 ~16 ~1 ~9 minecraft:deepslate_bricks
setblock ~17 ~1 ~9 minecraft:cobbled_deepslate
setblock ~18 ~1 ~9 minecraft:deepslate_bricks
setblock ~19 ~1 ~9 minecraft:cobbled_deepslate
setblock ~20 ~1 ~9 minecraft:deepslate_bricks
setblock ~9 ~2 ~9 minecraft:deepslate_tiles
setblock ~14 ~2 ~9 minecraft:mossy_stone_bricks
setblock ~15 ~2 ~9 minecraft:deepslate_bricks
setblock ~16 ~2 ~9 minecraft:cracked_deepslate_bricks
fill ~17 ~2 ~9 ~17 ~5 ~9 minecraft:deepslate_bricks
setblock ~18 ~2 ~9 minecraft:cracked_deepslate_bricks
fill ~19 ~2 ~9 ~19 ~4 ~9 minecraft:deepslate_bricks
setblock ~20 ~2 ~9 minecraft:cracked_deepslate_bricks
setblock ~9 ~3 ~9 minecraft:cracked_deepslate_bricks
setblock ~10 ~3 ~9 minecraft:deepslate_bricks
setblock ~11 ~3 ~9 minecraft:cracked_deepslate_bricks
fill ~14 ~3 ~9 ~14 ~4 ~9 minecraft:deepslate_bricks
setblock ~15 ~3 ~9 minecraft:deepslate_tiles
fill ~16 ~3 ~9 ~16 ~4 ~9 minecraft:deepslate_bricks
setblock ~18 ~3 ~9 minecraft:deepslate_bricks
fill ~20 ~3 ~9 ~20 ~5 ~9 minecraft:deepslate_bricks
fill ~9 ~4 ~9 ~9 ~5 ~9 minecraft:deepslate_bricks
setblock ~10 ~4 ~9 minecraft:cracked_deepslate_bricks
setblock ~15 ~4 ~9 minecraft:mossy_stone_bricks
setblock ~10 ~5 ~9 minecraft:deepslate_bricks
setblock ~15 ~5 ~9 minecraft:deepslate_bricks
setblock ~16 ~5 ~9 minecraft:cracked_deepslate_bricks
setblock ~9 ~6 ~9 minecraft:cracked_deepslate_bricks
fill ~15 ~6 ~9 ~15 ~7 ~9 minecraft:cracked_deepslate_bricks
fill ~16 ~6 ~9 ~16 ~7 ~9 minecraft:deepslate_bricks
setblock ~20 ~6 ~9 minecraft:deepslate_tiles
setblock ~20 ~7 ~9 minecraft:deepslate_bricks
setblock ~16 ~8 ~9 minecraft:cracked_deepslate_bricks

# the east end of the nave (x 20): a pillar either side of the chancel arch and a ruined lintel over it, the south and north shoulders broken
fill ~20 ~1 ~10 ~20 ~1 ~11 minecraft:deepslate_bricks
setblock ~20 ~1 ~12 minecraft:cobbled_deepslate
setblock ~20 ~1 ~19 minecraft:cobbled_deepslate
setblock ~20 ~1 ~20 minecraft:deepslate_bricks
setblock ~20 ~1 ~21 minecraft:cobbled_deepslate
setblock ~20 ~2 ~10 minecraft:cracked_deepslate_bricks
fill ~20 ~2 ~11 ~20 ~2 ~12 minecraft:deepslate_bricks
fill ~20 ~2 ~19 ~20 ~2 ~20 minecraft:cracked_deepslate_bricks
fill ~20 ~2 ~21 ~20 ~4 ~21 minecraft:deepslate_bricks
fill ~20 ~3 ~10 ~20 ~5 ~11 minecraft:deepslate_bricks
setblock ~20 ~3 ~12 minecraft:mossy_stone_bricks
fill ~20 ~3 ~19 ~20 ~3 ~20 minecraft:deepslate_bricks
setblock ~20 ~4 ~12 minecraft:cobbled_deepslate
setblock ~20 ~4 ~19 minecraft:cobbled_deepslate
setblock ~20 ~4 ~20 minecraft:deepslate_bricks
fill ~20 ~5 ~12 ~20 ~6 ~12 minecraft:deepslate_bricks
fill ~20 ~5 ~19 ~20 ~5 ~20 minecraft:cracked_deepslate_bricks
fill ~20 ~6 ~10 ~20 ~6 ~11 minecraft:cracked_deepslate_bricks
setblock ~20 ~6 ~19 minecraft:cobbled_deepslate
setblock ~20 ~7 ~10 minecraft:cracked_deepslate_bricks
fill ~20 ~8 ~14 ~20 ~8 ~16 minecraft:deepslate_bricks
fill ~20 ~9 ~14 ~20 ~9 ~15 minecraft:deepslate_bricks

# the chancel walls: south (z 19), north (z 12) and east (x 26), low and ragged, the east wall higher round the lancet behind the altar
setblock ~22 ~1 ~12 minecraft:cobbled_deepslate
fill ~22 ~1 ~19 ~23 ~1 ~19 minecraft:cobbled_deepslate
setblock ~23 ~1 ~12 minecraft:deepslate_bricks
fill ~24 ~1 ~12 ~25 ~1 ~12 minecraft:cobbled_deepslate
fill ~24 ~1 ~19 ~25 ~2 ~19 minecraft:deepslate_bricks
fill ~26 ~1 ~13 ~26 ~1 ~18 minecraft:cobbled_deepslate
setblock ~22 ~2 ~12 minecraft:deepslate_bricks
fill ~22 ~2 ~19 ~22 ~5 ~19 minecraft:deepslate_bricks
setblock ~23 ~2 ~12 minecraft:mossy_stone_bricks
setblock ~23 ~2 ~19 minecraft:cobbled_deepslate
fill ~24 ~2 ~12 ~25 ~2 ~12 minecraft:deepslate_bricks
fill ~26 ~2 ~13 ~26 ~2 ~16 minecraft:deepslate_bricks
fill ~26 ~2 ~17 ~26 ~2 ~18 minecraft:cracked_deepslate_bricks
fill ~22 ~3 ~12 ~23 ~3 ~12 minecraft:cracked_deepslate_bricks
fill ~23 ~3 ~19 ~24 ~3 ~19 minecraft:deepslate_bricks
setblock ~25 ~3 ~12 minecraft:cracked_deepslate_bricks
setblock ~25 ~3 ~19 minecraft:cracked_deepslate_bricks
fill ~26 ~3 ~13 ~26 ~3 ~14 minecraft:deepslate_bricks
fill ~26 ~3 ~17 ~26 ~3 ~18 minecraft:deepslate_bricks
setblock ~22 ~4 ~12 minecraft:deepslate_bricks
setblock ~23 ~4 ~19 minecraft:deepslate_bricks
setblock ~25 ~4 ~19 minecraft:deepslate_bricks
setblock ~26 ~4 ~13 minecraft:cracked_deepslate_bricks
fill ~26 ~4 ~14 ~26 ~7 ~14 minecraft:deepslate_bricks
fill ~26 ~4 ~17 ~26 ~6 ~17 minecraft:deepslate_bricks
setblock ~26 ~4 ~18 minecraft:cracked_deepslate_bricks
setblock ~21 ~5 ~12 minecraft:deepslate_bricks
fill ~21 ~5 ~19 ~21 ~6 ~19 minecraft:deepslate_bricks
setblock ~22 ~5 ~12 minecraft:cracked_deepslate_bricks
fill ~26 ~5 ~12 ~26 ~5 ~13 minecraft:deepslate_bricks
fill ~26 ~5 ~18 ~26 ~5 ~19 minecraft:deepslate_bricks
setblock ~22 ~6 ~12 minecraft:deepslate_bricks
setblock ~26 ~6 ~13 minecraft:deepslate_bricks
setblock ~26 ~6 ~19 minecraft:deepslate_bricks
setblock ~26 ~7 ~17 minecraft:cracked_deepslate_bricks
setblock ~26 ~8 ~15 minecraft:deepslate_bricks
setblock ~26 ~8 ~16 minecraft:cobbled_deepslate
setblock ~26 ~9 ~15 minecraft:deepslate_tiles
setblock ~26 ~9 ~16 minecraft:deepslate_bricks

# the bell tower stump (x 2..8, z 3..9): walls of deepslate bricks to a ragged crown of 17 at the north-west, 9 at the south-east
fill ~2 ~1 ~3 ~2 ~1 ~9 minecraft:deepslate_bricks
setblock ~3 ~1 ~3 minecraft:cobbled_deepslate
fill ~3 ~1 ~9 ~3 ~3 ~9 minecraft:deepslate_bricks
fill ~4 ~1 ~3 ~4 ~5 ~3 minecraft:deepslate_bricks
setblock ~5 ~1 ~3 minecraft:cobbled_deepslate
fill ~6 ~1 ~3 ~7 ~1 ~3 minecraft:deepslate_bricks
setblock ~6 ~1 ~9 minecraft:cobbled_deepslate
fill ~7 ~1 ~9 ~7 ~2 ~9 minecraft:deepslate_bricks
setblock ~8 ~1 ~3 minecraft:cobbled_deepslate
fill ~8 ~1 ~4 ~8 ~4 ~4 minecraft:deepslate_bricks
fill ~8 ~1 ~5 ~8 ~1 ~7 minecraft:cobbled_deepslate
fill ~8 ~1 ~8 ~8 ~1 ~9 minecraft:deepslate_bricks
fill ~2 ~2 ~3 ~2 ~17 ~3 minecraft:deepslate_bricks
fill ~2 ~2 ~4 ~2 ~3 ~4 minecraft:cracked_deepslate_bricks
fill ~2 ~2 ~5 ~2 ~2 ~6 minecraft:deepslate_bricks
fill ~2 ~2 ~7 ~2 ~4 ~7 minecraft:cracked_deepslate_bricks
fill ~2 ~2 ~8 ~2 ~7 ~8 minecraft:deepslate_bricks
setblock ~2 ~2 ~9 minecraft:mossy_stone_bricks
setblock ~3 ~2 ~3 minecraft:cracked_deepslate_bricks
fill ~5 ~2 ~3 ~6 ~2 ~3 minecraft:deepslate_bricks
setblock ~6 ~2 ~9 minecraft:deepslate_bricks
fill ~7 ~2 ~3 ~7 ~3 ~3 minecraft:cracked_deepslate_bricks
setblock ~8 ~2 ~3 minecraft:deepslate_bricks
fill ~8 ~2 ~5 ~8 ~2 ~8 minecraft:deepslate_bricks
setblock ~8 ~2 ~9 minecraft:deepslate_tiles
fill ~2 ~3 ~5 ~2 ~7 ~5 minecraft:deepslate_bricks
setblock ~2 ~3 ~6 minecraft:cracked_deepslate_bricks
fill ~2 ~3 ~9 ~2 ~12 ~9 minecraft:deepslate_bricks
setblock ~3 ~3 ~3 minecraft:deepslate_bricks
fill ~5 ~3 ~3 ~5 ~4 ~3 minecraft:cracked_deepslate_bricks
fill ~6 ~3 ~3 ~6 ~4 ~3 minecraft:deepslate_bricks
setblock ~6 ~3 ~9 minecraft:cracked_deepslate_bricks
setblock ~7 ~3 ~9 minecraft:cobbled_deepslate
fill ~8 ~3 ~3 ~8 ~4 ~3 minecraft:cracked_deepslate_bricks
fill ~8 ~3 ~5 ~8 ~3 ~7 minecraft:deepslate_bricks
fill ~8 ~3 ~8 ~8 ~3 ~9 minecraft:cracked_deepslate_bricks
setblock ~2 ~4 ~4 minecraft:deepslate_bricks
setblock ~2 ~4 ~6 minecraft:deepslate_bricks
setblock ~3 ~4 ~3 minecraft:cracked_deepslate_bricks
fill ~3 ~4 ~9 ~3 ~5 ~9 minecraft:cracked_deepslate_bricks
fill ~4 ~4 ~9 ~8 ~4 ~9 minecraft:deepslate_bricks
fill ~7 ~4 ~3 ~7 ~7 ~3 minecraft:deepslate_bricks
fill ~8 ~4 ~5 ~8 ~5 ~5 minecraft:deepslate_bricks
fill ~8 ~4 ~6 ~8 ~4 ~8 minecraft:cracked_deepslate_bricks
fill ~2 ~5 ~6 ~2 ~6 ~6 minecraft:cobbled_deepslate
fill ~4 ~5 ~9 ~4 ~6 ~9 minecraft:deepslate_bricks
fill ~5 ~5 ~3 ~5 ~8 ~3 minecraft:deepslate_bricks
setblock ~5 ~5 ~9 minecraft:cobbled_deepslate
fill ~6 ~5 ~9 ~8 ~5 ~9 minecraft:deepslate_bricks
setblock ~8 ~5 ~3 minecraft:mossy_stone_bricks
setblock ~8 ~5 ~4 minecraft:cracked_deepslate_bricks
fill ~8 ~5 ~6 ~8 ~7 ~7 minecraft:deepslate_bricks
setblock ~8 ~5 ~8 minecraft:cracked_deepslate_bricks
fill ~3 ~6 ~9 ~3 ~12 ~9 minecraft:deepslate_bricks
setblock ~4 ~6 ~3 minecraft:cracked_deepslate_bricks
fill ~5 ~6 ~9 ~5 ~8 ~9 minecraft:deepslate_bricks
setblock ~6 ~6 ~9 minecraft:cracked_deepslate_bricks
fill ~7 ~6 ~9 ~7 ~10 ~9 minecraft:deepslate_bricks
fill ~8 ~6 ~3 ~8 ~7 ~3 minecraft:cracked_deepslate_bricks
fill ~8 ~6 ~4 ~8 ~11 ~4 minecraft:deepslate_bricks
setblock ~8 ~6 ~5 minecraft:cobbled_deepslate
fill ~8 ~6 ~8 ~8 ~8 ~9 minecraft:deepslate_bricks
fill ~2 ~7 ~4 ~2 ~8 ~4 minecraft:deepslate_bricks
fill ~2 ~7 ~6 ~2 ~7 ~7 minecraft:deepslate_bricks
setblock ~3 ~7 ~3 minecraft:cracked_deepslate_bricks
fill ~4 ~7 ~3 ~4 ~8 ~3 minecraft:deepslate_bricks
setblock ~4 ~7 ~9 minecraft:mossy_stone_bricks
setblock ~6 ~7 ~3 minecraft:deepslate_bricks
setblock ~6 ~7 ~9 minecraft:cobbled_deepslate
setblock ~8 ~7 ~5 minecraft:deepslate_bricks
setblock ~2 ~8 ~5 minecraft:cracked_deepslate_bricks
fill ~2 ~8 ~6 ~2 ~10 ~6 minecraft:deepslate_bricks
setblock ~2 ~8 ~7 minecraft:mossy_stone_bricks
setblock ~2 ~8 ~8 minecraft:deepslate_tiles
fill ~3 ~8 ~3 ~3 ~15 ~3 minecraft:deepslate_bricks
setblock ~4 ~8 ~9 minecraft:cracked_deepslate_bricks
fill ~6 ~8 ~3 ~7 ~8 ~3 minecraft:cracked_deepslate_bricks
setblock ~6 ~8 ~9 minecraft:deepslate_bricks
fill ~8 ~8 ~3 ~8 ~9 ~3 minecraft:deepslate_bricks
setblock ~8 ~8 ~5 minecraft:cracked_deepslate_bricks
fill ~8 ~8 ~6 ~8 ~11 ~6 minecraft:deepslate_bricks
setblock ~8 ~8 ~7 minecraft:cobbled_deepslate
setblock ~2 ~9 ~4 minecraft:cracked_deepslate_bricks
fill ~2 ~9 ~5 ~2 ~10 ~5 minecraft:deepslate_bricks
setblock ~2 ~9 ~7 minecraft:deepslate_bricks
setblock ~2 ~9 ~8 minecraft:mossy_stone_bricks
fill ~4 ~9 ~3 ~5 ~9 ~3 minecraft:cracked_deepslate_bricks
setblock ~4 ~9 ~9 minecraft:deepslate_bricks
fill ~5 ~9 ~9 ~6 ~9 ~9 minecraft:cracked_deepslate_bricks
fill ~6 ~9 ~3 ~7 ~10 ~3 minecraft:deepslate_bricks
fill ~8 ~9 ~5 ~8 ~12 ~5 minecraft:deepslate_bricks
fill ~8 ~9 ~7 ~8 ~10 ~7 minecraft:deepslate_bricks
setblock ~8 ~9 ~8 minecraft:cracked_deepslate_bricks
setblock ~8 ~9 ~9 minecraft:deepslate_bricks
setblock ~2 ~10 ~4 minecraft:deepslate_bricks
fill ~2 ~10 ~7 ~2 ~10 ~8 minecraft:cracked_deepslate_bricks
fill ~4 ~10 ~3 ~5 ~10 ~3 minecraft:deepslate_bricks
setblock ~4 ~10 ~9 minecraft:cracked_deepslate_bricks
fill ~5 ~10 ~9 ~6 ~10 ~9 minecraft:deepslate_bricks
setblock ~8 ~10 ~3 minecraft:cracked_deepslate_bricks
setblock ~8 ~10 ~8 minecraft:deepslate_bricks
setblock ~2 ~11 ~4 minecraft:deepslate_tiles
fill ~2 ~11 ~7 ~2 ~13 ~7 minecraft:deepslate_bricks
setblock ~2 ~11 ~8 minecraft:cracked_deepslate_bricks
fill ~4 ~11 ~9 ~5 ~11 ~9 minecraft:deepslate_bricks
setblock ~6 ~11 ~3 minecraft:cracked_deepslate_bricks
fill ~7 ~11 ~3 ~7 ~13 ~3 minecraft:deepslate_bricks
setblock ~8 ~11 ~3 minecraft:cobbled_deepslate
setblock ~2 ~12 ~4 minecraft:cracked_deepslate_bricks
fill ~2 ~12 ~8 ~2 ~13 ~8 minecraft:deepslate_bricks
setblock ~6 ~12 ~3 minecraft:cobbled_deepslate
fill ~8 ~12 ~3 ~8 ~12 ~4 minecraft:cracked_deepslate_bricks
fill ~2 ~13 ~4 ~2 ~15 ~4 minecraft:deepslate_bricks
fill ~6 ~13 ~3 ~6 ~14 ~3 minecraft:deepslate_bricks
setblock ~8 ~13 ~3 minecraft:cobbled_deepslate
setblock ~2 ~14 ~7 minecraft:cracked_deepslate_bricks
setblock ~7 ~14 ~3 minecraft:cracked_deepslate_bricks
setblock ~6 ~15 ~3 minecraft:mossy_stone_bricks
fill ~2 ~16 ~4 ~2 ~17 ~4 minecraft:cracked_deepslate_bricks
setblock ~2 ~16 ~5 minecraft:deepslate_bricks
setblock ~3 ~16 ~3 minecraft:cracked_deepslate_bricks
setblock ~4 ~16 ~3 minecraft:deepslate_bricks
setblock ~5 ~16 ~3 minecraft:deepslate_tiles
setblock ~3 ~17 ~3 minecraft:deepslate_bricks

# the footing and corner buttresses: a polished deepslate course at y 2 round the nave and the tower, buttresses on the corners of the chancel
setblock ~2 ~1 ~22 minecraft:cobbled_deepslate
setblock ~20 ~1 ~22 minecraft:cobbled_deepslate
setblock ~21 ~1 ~12 minecraft:cobbled_deepslate
setblock ~21 ~1 ~19 minecraft:cobbled_deepslate
setblock ~26 ~1 ~12 minecraft:cobbled_deepslate
setblock ~26 ~1 ~19 minecraft:cobbled_deepslate
setblock ~2 ~2 ~22 minecraft:polished_deepslate
setblock ~20 ~2 ~22 minecraft:polished_deepslate
setblock ~21 ~2 ~12 minecraft:polished_deepslate
setblock ~21 ~2 ~19 minecraft:polished_deepslate
setblock ~26 ~2 ~12 minecraft:polished_deepslate
setblock ~26 ~2 ~19 minecraft:polished_deepslate
setblock ~2 ~3 ~22 minecraft:cobbled_deepslate
setblock ~20 ~3 ~22 minecraft:cobbled_deepslate
setblock ~21 ~3 ~12 minecraft:cobbled_deepslate
setblock ~21 ~3 ~19 minecraft:cobbled_deepslate
setblock ~26 ~3 ~12 minecraft:cobbled_deepslate
setblock ~26 ~3 ~19 minecraft:cobbled_deepslate
setblock ~2 ~4 ~22 minecraft:polished_deepslate
setblock ~20 ~4 ~22 minecraft:polished_deepslate
setblock ~21 ~4 ~12 minecraft:polished_deepslate
setblock ~21 ~4 ~19 minecraft:polished_deepslate
setblock ~26 ~4 ~12 minecraft:polished_deepslate
setblock ~26 ~4 ~19 minecraft:polished_deepslate

# the south door (x 5..9, y 1..4, open) under a ragged arch of chiseled and polished deepslate whose right springer has fallen; the window (x 11..14, y 2..3, open) with a lancet of purple glass remnants above it; a second lancet at x 18
fill ~4 ~1 ~22 ~4 ~5 ~22 minecraft:polished_deepslate
fill ~5 ~1 ~22 ~9 ~4 ~22 minecraft:air
fill ~10 ~1 ~22 ~10 ~4 ~22 minecraft:polished_deepslate
fill ~11 ~2 ~22 ~14 ~3 ~22 minecraft:air
fill ~18 ~3 ~22 ~18 ~4 ~22 minecraft:purple_stained_glass_pane[north=false,east=true,south=false,west=true]
setblock ~11 ~4 ~22 minecraft:chiseled_deepslate
fill ~12 ~4 ~22 ~13 ~4 ~22 minecraft:polished_deepslate
setblock ~14 ~4 ~22 minecraft:chiseled_deepslate
fill ~5 ~5 ~22 ~6 ~5 ~22 minecraft:polished_deepslate
setblock ~7 ~5 ~22 minecraft:chiseled_deepslate
setblock ~8 ~5 ~22 minecraft:polished_deepslate
fill ~9 ~5 ~22 ~10 ~5 ~22 minecraft:air
fill ~12 ~5 ~22 ~13 ~5 ~22 minecraft:purple_stained_glass_pane[north=false,east=true,south=false,west=true]
setblock ~18 ~5 ~22 minecraft:air
fill ~5 ~6 ~22 ~6 ~6 ~22 minecraft:polished_deepslate_stairs[facing=east,half=bottom]
setblock ~7 ~6 ~22 minecraft:polished_deepslate
fill ~8 ~6 ~22 ~9 ~6 ~22 minecraft:air
setblock ~12 ~6 ~22 minecraft:purple_stained_glass_pane[north=false,east=true,south=false,west=true]

# the chancel arch (x 20, z 14..17, y 1..6) with polished jambs, the arch ring cracked away at the south, the door from the tower into the nave (x 4..5, z 9, y 1..3)
fill ~4 ~1 ~9 ~5 ~3 ~9 minecraft:air
setblock ~20 ~1 ~13 minecraft:deepslate_tiles
fill ~20 ~1 ~14 ~20 ~5 ~17 minecraft:air
setblock ~20 ~1 ~18 minecraft:cracked_deepslate_bricks
setblock ~20 ~2 ~13 minecraft:deepslate_bricks
setblock ~20 ~2 ~18 minecraft:deepslate_bricks
setblock ~20 ~3 ~13 minecraft:polished_deepslate
setblock ~20 ~3 ~18 minecraft:polished_deepslate
setblock ~20 ~4 ~13 minecraft:cracked_deepslate_bricks
fill ~20 ~4 ~18 ~20 ~5 ~18 minecraft:deepslate_bricks
setblock ~20 ~5 ~13 minecraft:cobbled_deepslate
setblock ~20 ~6 ~13 minecraft:polished_deepslate
setblock ~20 ~6 ~15 minecraft:air
setblock ~20 ~6 ~17 minecraft:air
setblock ~20 ~6 ~18 minecraft:polished_deepslate
setblock ~20 ~7 ~13 minecraft:chiseled_deepslate
fill ~20 ~7 ~14 ~20 ~7 ~16 minecraft:polished_deepslate
setblock ~20 ~7 ~18 minecraft:chiseled_deepslate
setblock ~20 ~8 ~13 minecraft:deepslate_bricks
setblock ~20 ~8 ~18 minecraft:deepslate_bricks

# the ruined rose window in the west gable: a ring of chiseled deepslate round a hole three blocks across (z 14..17, y 8..11), iron mullions, a few panes of purple glass left
fill ~2 ~7 ~14 ~2 ~7 ~17 minecraft:chiseled_deepslate
fill ~2 ~8 ~13 ~2 ~11 ~13 minecraft:chiseled_deepslate
setblock ~2 ~8 ~14 minecraft:air
setblock ~2 ~8 ~15 minecraft:iron_bars[north=false,east=false,south=true,west=false]
fill ~2 ~8 ~16 ~2 ~11 ~16 minecraft:iron_bars[north=true,east=false,south=true,west=false]
setblock ~2 ~8 ~17 minecraft:purple_stained_glass_pane[north=true,east=false,south=true,west=false]
fill ~2 ~8 ~18 ~2 ~11 ~18 minecraft:chiseled_deepslate
fill ~2 ~9 ~14 ~2 ~10 ~15 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~2 ~9 ~17 ~2 ~10 ~17 minecraft:iron_bars[north=true,east=false,south=true,west=false]
setblock ~2 ~11 ~14 minecraft:purple_stained_glass_pane[north=true,east=false,south=true,west=false]
setblock ~2 ~11 ~15 minecraft:iron_bars[north=true,east=false,south=true,west=false]
setblock ~2 ~11 ~17 minecraft:purple_stained_glass_pane[north=true,east=false,south=true,west=false]
fill ~2 ~12 ~14 ~2 ~12 ~17 minecraft:chiseled_deepslate

# the lancet behind the altar (x 26, z 15..16, y 3..7), open to the sky at the top, a pane of purple glass at the foot
setblock ~26 ~3 ~15 minecraft:purple_stained_glass_pane[north=true,east=false,south=false,west=false]
fill ~26 ~4 ~15 ~26 ~7 ~15 minecraft:air
setblock ~26 ~4 ~16 minecraft:purple_stained_glass_pane[north=false,east=false,south=true,west=false]
setblock ~26 ~7 ~16 minecraft:air

# the belfry of the tower: open arches on the north face (x 4..5, z 3) and on the west face (x 2, z 5..6), y 11..14, the way up to the crown; narrow loops with purple glass lower down
setblock ~2 ~5 ~4 minecraft:air
fill ~2 ~5 ~7 ~2 ~6 ~7 minecraft:purple_stained_glass_pane[north=true,east=false,south=true,west=false]
fill ~6 ~5 ~3 ~6 ~6 ~3 minecraft:purple_stained_glass_pane[north=false,east=true,south=false,west=true]
setblock ~3 ~6 ~3 minecraft:air
fill ~2 ~11 ~5 ~2 ~11 ~6 minecraft:air
fill ~5 ~11 ~3 ~5 ~14 ~3 minecraft:air
setblock ~2 ~12 ~5 minecraft:air
setblock ~4 ~12 ~3 minecraft:air
fill ~2 ~14 ~5 ~2 ~14 ~6 minecraft:air
fill ~2 ~15 ~5 ~2 ~15 ~6 minecraft:polished_deepslate_stairs[facing=east,half=top]
fill ~4 ~15 ~3 ~5 ~15 ~3 minecraft:polished_deepslate_stairs[facing=south,half=top]

# the bell of the tower: a beam of dark oak logs across the belfry at y 15 from the west wall, the bell hanging from it, a second beam fallen across the crown
setblock ~5 ~14 ~6 minecraft:bell[attachment=ceiling,facing=north]
fill ~3 ~15 ~6 ~7 ~15 ~6 minecraft:dark_oak_log[axis=x]

# tie beams of dark oak at y 8 over the nave (x 4, x 8, x 12), the last two broken short of the north wall
fill ~4 ~8 ~10 ~4 ~8 ~21 minecraft:dark_oak_log[axis=z]
fill ~8 ~8 ~10 ~8 ~8 ~21 minecraft:dark_oak_log[axis=z]
fill ~12 ~8 ~14 ~12 ~8 ~21 minecraft:dark_oak_log[axis=z]

# rafters: the ribs of the roof at x 4 and x 8 whole on both slopes, at x 12 on the south slope only, at x 16 a stub on the south eave; dark oak stairs up the slope to the ridge
setblock ~4 ~8 ~22 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~8 ~8 ~22 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~12 ~8 ~22 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~16 ~8 ~22 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~4 ~9 ~10 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~4 ~9 ~21 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~8 ~9 ~10 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~8 ~9 ~21 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~12 ~9 ~21 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~16 ~9 ~21 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~4 ~10 ~11 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~4 ~10 ~20 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~8 ~10 ~11 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~8 ~10 ~20 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~12 ~10 ~20 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~16 ~10 ~20 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~4 ~11 ~12 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~4 ~11 ~19 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~8 ~11 ~12 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~8 ~11 ~19 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~12 ~11 ~19 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~16 ~11 ~19 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~4 ~12 ~13 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~4 ~12 ~18 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~8 ~12 ~13 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~8 ~12 ~18 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~12 ~12 ~18 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~4 ~13 ~14 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~4 ~13 ~17 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~8 ~13 ~14 minecraft:dark_oak_stairs[facing=south,half=bottom]
setblock ~8 ~13 ~17 minecraft:dark_oak_stairs[facing=north,half=bottom]
setblock ~12 ~13 ~17 minecraft:dark_oak_stairs[facing=north,half=bottom]
fill ~4 ~14 ~15 ~4 ~14 ~16 minecraft:dark_oak_planks
fill ~8 ~14 ~15 ~8 ~14 ~16 minecraft:dark_oak_planks
fill ~12 ~14 ~15 ~12 ~14 ~16 minecraft:dark_oak_planks

# what is left of the roof skin: deepslate tile stairs over the south-west corner of the nave (x 2..10, z 16..22) and the ridge to x 10, torn and holed
fill ~2 ~8 ~22 ~3 ~8 ~22 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~5 ~8 ~22 ~7 ~8 ~22 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~9 ~8 ~22 ~10 ~8 ~22 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~2 ~9 ~21 ~3 ~9 ~21 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~5 ~9 ~21 ~7 ~9 ~21 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~9 ~9 ~21 ~10 ~9 ~21 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~2 ~10 ~20 ~3 ~10 ~20 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~5 ~10 ~20 ~7 ~10 ~20 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~9 ~10 ~20 ~10 ~10 ~20 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~2 ~11 ~19 ~3 ~11 ~19 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~5 ~11 ~19 ~7 ~11 ~19 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~9 ~11 ~19 ~10 ~11 ~19 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~2 ~12 ~18 ~3 ~12 ~18 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~5 ~12 ~18 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~7 ~12 ~18 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~9 ~12 ~18 ~10 ~12 ~18 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~2 ~13 ~17 ~3 ~13 ~17 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~5 ~13 ~17 ~7 ~13 ~17 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~9 ~13 ~17 ~10 ~13 ~17 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~2 ~14 ~15 ~3 ~14 ~15 minecraft:deepslate_tile_slab[type=bottom]
setblock ~2 ~14 ~16 minecraft:deepslate_tile_slab[type=bottom]
fill ~5 ~14 ~15 ~7 ~14 ~16 minecraft:deepslate_tile_slab[type=bottom]
fill ~9 ~14 ~15 ~10 ~14 ~16 minecraft:deepslate_tile_slab[type=bottom]

# pews in the north half (x 6, 8, 16, 18, z 11..14, facing the altar), two in the south half (x 6, 8, z 17..18); the others are under the rubble of the north wall
fill ~6 ~1 ~11 ~6 ~1 ~14 minecraft:spruce_stairs[facing=west,half=bottom]
fill ~6 ~1 ~17 ~6 ~1 ~18 minecraft:spruce_stairs[facing=west,half=bottom]
fill ~8 ~1 ~11 ~8 ~1 ~14 minecraft:spruce_stairs[facing=west,half=bottom]
fill ~8 ~1 ~17 ~8 ~1 ~18 minecraft:spruce_stairs[facing=west,half=bottom]
fill ~16 ~1 ~11 ~16 ~1 ~13 minecraft:spruce_stairs[facing=west,half=bottom]
fill ~18 ~1 ~11 ~18 ~1 ~14 minecraft:spruce_stairs[facing=west,half=bottom]

# rubble where the north wall came down (x 10..15, z 10..13): blocks, stairs and slabs of deepslate, one or two high, a tie beam fallen across it
fill ~10 ~1 ~10 ~12 ~1 ~10 minecraft:cobbled_deepslate
setblock ~10 ~1 ~11 minecraft:tuff
setblock ~10 ~1 ~13 minecraft:mossy_cobblestone
setblock ~11 ~1 ~13 minecraft:cobbled_deepslate
fill ~12 ~1 ~11 ~13 ~1 ~11 minecraft:deepslate_bricks
setblock ~12 ~1 ~12 minecraft:mossy_cobblestone
setblock ~12 ~1 ~13 minecraft:cracked_deepslate_bricks
fill ~13 ~1 ~12 ~14 ~1 ~12 minecraft:cobbled_deepslate
setblock ~13 ~1 ~13 minecraft:deepslate_bricks
fill ~15 ~1 ~10 ~15 ~1 ~11 minecraft:deepslate_bricks
setblock ~15 ~1 ~12 minecraft:cracked_deepslate_bricks
fill ~10 ~2 ~10 ~11 ~2 ~10 minecraft:cobbled_deepslate_slab[type=bottom]
fill ~11 ~2 ~13 ~12 ~2 ~13 minecraft:cobbled_deepslate_slab[type=bottom]
fill ~12 ~2 ~10 ~12 ~2 ~11 minecraft:mossy_cobblestone
setblock ~12 ~2 ~12 minecraft:cobbled_deepslate
setblock ~13 ~2 ~11 minecraft:cobbled_deepslate
setblock ~13 ~2 ~12 minecraft:mossy_cobblestone
fill ~14 ~2 ~12 ~15 ~2 ~12 minecraft:cobbled_deepslate_slab[type=bottom]
fill ~12 ~3 ~12 ~14 ~3 ~12 minecraft:dark_oak_log[axis=x]

# rubble at the south-west corner beside the door (x 3..4, z 18..21) and a fallen beam (x 5..9, z 19..20) across the foot of the pews
setblock ~3 ~1 ~18 minecraft:cobbled_deepslate
setblock ~3 ~1 ~19 minecraft:tuff
fill ~3 ~1 ~20 ~3 ~1 ~21 minecraft:cobbled_deepslate
setblock ~4 ~1 ~20 minecraft:mossy_cobblestone
setblock ~4 ~1 ~21 minecraft:cobbled_deepslate
setblock ~3 ~2 ~19 minecraft:deepslate_bricks
setblock ~3 ~2 ~21 minecraft:cracked_deepslate_bricks

# the font: a pedestal of deepslate brick wall and a cauldron of water at (19, 2, 20), off the cover line of the plan so that the pier of the south wall is what stops it; a lectern pedestal with a ruined book at (19, 1, 12)
setblock ~19 ~1 ~12 minecraft:lectern[facing=west]
setblock ~19 ~1 ~20 minecraft:deepslate_brick_wall[north=none,east=low,south=none,west=none,up=true]
setblock ~19 ~2 ~20 minecraft:water_cauldron[level=3]

# the altar of the chancel: a front row of polished deepslate at x 24 (z 15..16), a raised back row at x 25 (y 1..2), candles on top, soul lanterns on the back row, a cracked step in front
setblock ~24 ~1 ~14 minecraft:deepslate_brick_slab[type=bottom]
fill ~24 ~1 ~15 ~24 ~1 ~16 minecraft:chiseled_deepslate
setblock ~24 ~1 ~17 minecraft:deepslate_brick_slab[type=bottom]
fill ~25 ~1 ~15 ~25 ~2 ~16 minecraft:polished_deepslate
fill ~24 ~2 ~15 ~24 ~2 ~16 minecraft:candle[candles=4,lit=true]
fill ~25 ~3 ~15 ~25 ~3 ~16 minecraft:soul_lantern[hanging=false]

# candles on the floor and on the pew ends: single clusters that still burn, never in the kept-clear cells
setblock ~3 ~1 ~12 minecraft:candle[candles=2,lit=true]
setblock ~3 ~1 ~16 minecraft:candle[candles=1,lit=true]
setblock ~5 ~1 ~17 minecraft:candle[candles=3,lit=true]
setblock ~7 ~1 ~13 minecraft:candle[candles=1,lit=true]
setblock ~9 ~1 ~18 minecraft:candle[candles=1,lit=true]
setblock ~19 ~1 ~15 minecraft:candle[candles=2,lit=true]
setblock ~19 ~1 ~16 minecraft:candle[candles=3,lit=true]
setblock ~21 ~1 ~14 minecraft:candle[candles=1,lit=true]
setblock ~22 ~1 ~17 minecraft:candle[candles=1,lit=true]
setblock ~23 ~1 ~18 minecraft:candle[candles=3,lit=true]

# soul lanterns hung from the tie beams (y 7), a pair on the chancel arch, a chain from the tower beam
setblock ~20 ~6 ~14 minecraft:soul_lantern[hanging=true]
setblock ~20 ~6 ~16 minecraft:soul_lantern[hanging=true]
setblock ~4 ~7 ~13 minecraft:soul_lantern[hanging=true]
setblock ~4 ~7 ~18 minecraft:soul_lantern[hanging=true]
setblock ~8 ~7 ~12 minecraft:soul_lantern[hanging=true]
setblock ~8 ~7 ~19 minecraft:soul_lantern[hanging=true]
setblock ~12 ~7 ~17 minecraft:soul_lantern[hanging=true]

# the graves in two rows (x 11, 14, 17, 20, 23, 26 at z 2 and z 6): a tablet of deepslate bricks with a rounded slab on top, the mound of earth behind it; one tablet cracked, two fallen
setblock ~11 ~1 ~2 minecraft:deepslate_bricks
setblock ~11 ~1 ~3 minecraft:coarse_dirt
setblock ~11 ~1 ~4 minecraft:podzol
setblock ~11 ~1 ~6 minecraft:cracked_deepslate_bricks
setblock ~11 ~1 ~7 minecraft:podzol
setblock ~11 ~1 ~8 minecraft:coarse_dirt
setblock ~14 ~1 ~2 minecraft:deepslate_brick_stairs[facing=south,half=bottom]
fill ~14 ~1 ~3 ~14 ~1 ~4 minecraft:podzol
setblock ~14 ~1 ~6 minecraft:deepslate_bricks
setblock ~14 ~1 ~7 minecraft:podzol
setblock ~14 ~1 ~8 minecraft:moss_block
setblock ~17 ~1 ~2 minecraft:deepslate_bricks
fill ~17 ~1 ~3 ~17 ~1 ~4 minecraft:podzol
setblock ~17 ~1 ~6 minecraft:deepslate_bricks
setblock ~17 ~1 ~7 minecraft:coarse_dirt
setblock ~17 ~1 ~8 minecraft:moss_block
setblock ~20 ~1 ~2 minecraft:deepslate_bricks
setblock ~20 ~1 ~3 minecraft:podzol
setblock ~20 ~1 ~4 minecraft:coarse_dirt
setblock ~20 ~1 ~6 minecraft:deepslate_bricks
setblock ~20 ~1 ~7 minecraft:moss_block
setblock ~20 ~1 ~8 minecraft:podzol
setblock ~23 ~1 ~6 minecraft:deepslate_bricks
setblock ~23 ~1 ~7 minecraft:moss_block
setblock ~23 ~1 ~8 minecraft:podzol
setblock ~26 ~1 ~2 minecraft:cracked_deepslate_bricks
fill ~26 ~1 ~3 ~26 ~1 ~4 minecraft:moss_block
setblock ~26 ~1 ~6 minecraft:deepslate_brick_stairs[facing=south,half=bottom]
setblock ~26 ~1 ~7 minecraft:coarse_dirt
setblock ~26 ~1 ~8 minecraft:moss_block
setblock ~11 ~2 ~2 minecraft:deepslate_brick_slab[type=bottom]
setblock ~14 ~2 ~6 minecraft:deepslate_brick_slab[type=bottom]
setblock ~17 ~2 ~2 minecraft:deepslate_brick_slab[type=bottom]
setblock ~17 ~2 ~6 minecraft:deepslate_brick_slab[type=bottom]
setblock ~20 ~2 ~2 minecraft:deepslate_brick_slab[type=bottom]
setblock ~20 ~2 ~6 minecraft:deepslate_brick_slab[type=bottom]
setblock ~23 ~2 ~6 minecraft:deepslate_brick_slab[type=bottom]

# the fence of the graveyard on its north edge (z 0): posts of polished deepslate with a soul lantern on each, iron bars between, a gap where it has fallen
fill ~10 ~1 ~0 ~10 ~3 ~0 minecraft:polished_deepslate
fill ~11 ~1 ~0 ~13 ~2 ~0 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~14 ~1 ~0 ~14 ~3 ~0 minecraft:polished_deepslate
fill ~16 ~1 ~0 ~16 ~2 ~0 minecraft:iron_bars[north=false,east=true,south=false,west=false]
fill ~17 ~1 ~0 ~17 ~2 ~0 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~18 ~1 ~0 ~18 ~3 ~0 minecraft:polished_deepslate
fill ~22 ~1 ~0 ~22 ~3 ~0 minecraft:polished_deepslate
setblock ~23 ~1 ~0 minecraft:iron_bars[north=false,east=true,south=true,west=true]
fill ~24 ~1 ~0 ~25 ~2 ~0 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~26 ~1 ~0 ~26 ~3 ~0 minecraft:polished_deepslate
fill ~27 ~1 ~0 ~27 ~2 ~0 minecraft:iron_bars[north=false,east=false,south=false,west=true]
setblock ~23 ~2 ~0 minecraft:iron_bars[north=false,east=true,south=false,west=true]
setblock ~10 ~4 ~0 minecraft:soul_lantern[hanging=false]
setblock ~14 ~4 ~0 minecraft:soul_lantern[hanging=false]
setblock ~18 ~4 ~0 minecraft:soul_lantern[hanging=false]
setblock ~22 ~4 ~0 minecraft:soul_lantern[hanging=false]
setblock ~26 ~4 ~0 minecraft:soul_lantern[hanging=false]

# the dead tree at (23, 2): a trunk of dark oak logs eight high, bare branches of logs and fences, roots in the ground
setblock ~22 ~1 ~2 minecraft:dark_oak_log[axis=x]
setblock ~23 ~1 ~1 minecraft:dark_oak_log[axis=z]
fill ~23 ~1 ~2 ~23 ~5 ~2 minecraft:dark_oak_log[axis=y]
setblock ~23 ~1 ~3 minecraft:dark_oak_log[axis=z]
setblock ~24 ~1 ~2 minecraft:dark_oak_log[axis=x]
setblock ~22 ~6 ~2 minecraft:dark_oak_log[axis=x]
setblock ~23 ~6 ~1 minecraft:dark_oak_log[axis=z]
fill ~23 ~6 ~2 ~23 ~8 ~2 minecraft:stripped_dark_oak_log[axis=y]
setblock ~21 ~7 ~2 minecraft:dark_oak_log[axis=x]
setblock ~23 ~7 ~3 minecraft:dark_oak_log[axis=z]
setblock ~24 ~7 ~2 minecraft:dark_oak_log[axis=x]
setblock ~20 ~8 ~2 minecraft:dark_oak_fence[north=false,east=true,south=false,west=false]
fill ~21 ~8 ~2 ~22 ~8 ~2 minecraft:dark_oak_fence[north=false,east=true,south=false,west=true]
setblock ~23 ~8 ~4 minecraft:dark_oak_log[axis=z]
setblock ~25 ~8 ~2 minecraft:dark_oak_log[axis=x]
setblock ~23 ~9 ~2 minecraft:dark_oak_fence[north=false,east=true,south=false,west=false]
setblock ~23 ~9 ~4 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~24 ~9 ~2 minecraft:dark_oak_fence[north=false,east=false,south=false,west=true]
setblock ~26 ~9 ~2 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]

# moss carpet on the exposed tops of the walls, single blocks by rule
setblock ~10 ~2 ~11 minecraft:moss_carpet
setblock ~11 ~2 ~6 minecraft:moss_carpet
setblock ~13 ~2 ~13 minecraft:moss_carpet
setblock ~17 ~2 ~7 minecraft:moss_carpet
setblock ~20 ~2 ~8 minecraft:moss_carpet
setblock ~26 ~2 ~7 minecraft:moss_carpet
setblock ~25 ~4 ~12 minecraft:moss_carpet
setblock ~19 ~6 ~22 minecraft:moss_carpet
setblock ~18 ~7 ~22 minecraft:moss_carpet
setblock ~20 ~7 ~19 minecraft:moss_carpet
setblock ~20 ~8 ~9 minecraft:moss_carpet
setblock ~20 ~9 ~13 minecraft:moss_carpet
setblock ~8 ~10 ~9 minecraft:moss_carpet
setblock ~26 ~10 ~16 minecraft:moss_carpet
setblock ~6 ~11 ~9 minecraft:moss_carpet
setblock ~8 ~11 ~7 minecraft:moss_carpet
setblock ~2 ~12 ~12 minecraft:moss_carpet
setblock ~2 ~13 ~9 minecraft:moss_carpet
fill ~4 ~15 ~15 ~4 ~15 ~16 minecraft:moss_carpet
setblock ~7 ~15 ~3 minecraft:moss_carpet
setblock ~8 ~15 ~16 minecraft:moss_carpet
setblock ~4 ~16 ~6 minecraft:moss_carpet
setblock ~7 ~16 ~6 minecraft:moss_carpet
setblock ~4 ~17 ~3 minecraft:moss_carpet

# vines on the faces of the walls and the tower, one to three long
fill ~1 ~1 ~8 ~1 ~9 ~8 minecraft:vine[east=true]
fill ~1 ~1 ~10 ~1 ~6 ~10 minecraft:vine[east=true]
fill ~3 ~1 ~17 ~3 ~5 ~17 minecraft:vine[west=true]
fill ~7 ~1 ~10 ~7 ~5 ~10 minecraft:vine[north=true]
fill ~25 ~1 ~18 ~25 ~3 ~18 minecraft:vine[east=true]
fill ~1 ~2 ~6 ~1 ~4 ~6 minecraft:vine[east=true]
fill ~1 ~2 ~11 ~1 ~3 ~11 minecraft:vine[east=true]
fill ~1 ~2 ~21 ~1 ~4 ~21 minecraft:vine[east=true]
fill ~3 ~2 ~2 ~3 ~4 ~2 minecraft:vine[south=true]
fill ~3 ~2 ~7 ~3 ~3 ~8 minecraft:vine[west=true]
fill ~3 ~2 ~10 ~3 ~3 ~10 minecraft:vine[north=true]
fill ~3 ~2 ~15 ~3 ~3 ~15 minecraft:vine[west=true]
fill ~9 ~2 ~8 ~9 ~4 ~8 minecraft:vine[west=true]
fill ~15 ~2 ~8 ~15 ~3 ~8 minecraft:vine[south=true]
fill ~17 ~2 ~8 ~17 ~3 ~8 minecraft:vine[south=true]
fill ~19 ~2 ~8 ~19 ~4 ~8 minecraft:vine[south=true]
fill ~21 ~2 ~11 ~21 ~3 ~11 minecraft:vine[south=true]
fill ~22 ~2 ~2 ~22 ~4 ~2 minecraft:vine[east=true]
fill ~23 ~2 ~3 ~23 ~4 ~3 minecraft:vine[north=true]
fill ~23 ~2 ~11 ~23 ~3 ~11 minecraft:vine[south=true]
fill ~3 ~3 ~11 ~3 ~4 ~11 minecraft:vine[west=true]
fill ~5 ~3 ~2 ~5 ~5 ~2 minecraft:vine[south=true]
fill ~6 ~3 ~8 ~6 ~4 ~8 minecraft:vine[south=true]
fill ~9 ~3 ~3 ~9 ~4 ~3 minecraft:vine[west=true]
setblock ~10 ~3 ~8 minecraft:vine[south=true]
setblock ~13 ~3 ~13 minecraft:vine[north=true]
setblock ~14 ~3 ~11 minecraft:vine[south=true]
setblock ~19 ~3 ~18 minecraft:vine[east=true]
setblock ~21 ~3 ~13 minecraft:vine[west=true]
fill ~21 ~3 ~18 ~21 ~8 ~18 minecraft:vine[west=true]
fill ~24 ~3 ~2 ~24 ~4 ~2 minecraft:vine[west=true]
setblock ~25 ~3 ~13 minecraft:vine[north=true]
setblock ~26 ~3 ~16 minecraft:vine[south=true]
fill ~27 ~3 ~14 ~27 ~4 ~14 minecraft:vine[west=true]
fill ~1 ~4 ~15 ~1 ~7 ~15 minecraft:vine[east=true]
fill ~1 ~4 ~22 ~1 ~5 ~22 minecraft:vine[east=true]
fill ~3 ~4 ~4 ~4 ~4 ~4 minecraft:vine[north=true]
fill ~3 ~4 ~5 ~3 ~5 ~5 minecraft:vine[west=true]
setblock ~3 ~4 ~7 minecraft:vine[west=true]
setblock ~3 ~4 ~14 minecraft:vine[west=true]
fill ~3 ~4 ~21 ~3 ~5 ~21 minecraft:vine[west=true]
setblock ~5 ~4 ~8 minecraft:vine[south=true]
fill ~7 ~4 ~4 ~7 ~8 ~4 minecraft:vine[north=true]
setblock ~11 ~4 ~9 minecraft:vine[west=true]
fill ~19 ~4 ~10 ~19 ~5 ~11 minecraft:vine[east=true]
fill ~20 ~4 ~8 ~20 ~6 ~8 minecraft:vine[south=true]
setblock ~22 ~4 ~13 minecraft:vine[north=true]
setblock ~22 ~4 ~18 minecraft:vine[south=true]
fill ~23 ~4 ~12 ~23 ~5 ~12 minecraft:vine[west=true]
setblock ~23 ~4 ~20 minecraft:vine[north=true]
setblock ~25 ~4 ~13 minecraft:vine[east=true]
setblock ~26 ~4 ~20 minecraft:vine[north=true]
setblock ~3 ~5 ~3 minecraft:vine[east=true]
fill ~3 ~5 ~12 ~3 ~8 ~12 minecraft:vine[west=true]
setblock ~3 ~5 ~16 minecraft:vine[west=true]
fill ~3 ~5 ~19 ~3 ~6 ~19 minecraft:vine[west=true]
fill ~4 ~5 ~4 ~5 ~5 ~4 minecraft:vine[north=true]
setblock ~5 ~5 ~21 minecraft:vine[south=true]
fill ~7 ~5 ~5 ~7 ~6 ~5 minecraft:vine[east=true]
fill ~7 ~5 ~8 ~7 ~6 ~8 minecraft:vine[south=true]
setblock ~7 ~5 ~21 minecraft:vine[south=true]
fill ~8 ~5 ~10 ~8 ~6 ~10 minecraft:vine[north=true]
fill ~9 ~5 ~4 ~9 ~7 ~4 minecraft:vine[west=true]
fill ~11 ~5 ~21 ~11 ~6 ~21 minecraft:vine[south=true]
fill ~16 ~5 ~10 ~16 ~7 ~10 minecraft:vine[north=true]
fill ~21 ~5 ~9 ~21 ~5 ~10 minecraft:vine[west=true]
setblock ~21 ~5 ~13 minecraft:vine[north=true]
fill ~21 ~5 ~20 ~21 ~6 ~20 minecraft:vine[north=true]
setblock ~26 ~5 ~11 minecraft:vine[south=true]
fill ~26 ~5 ~16 ~26 ~6 ~16 minecraft:vine[south=true]
fill ~1 ~6 ~6 ~1 ~7 ~6 minecraft:vine[east=true]
fill ~1 ~6 ~11 ~1 ~7 ~11 minecraft:vine[east=true]
setblock ~2 ~6 ~4 minecraft:vine[south=true]
fill ~5 ~6 ~4 ~5 ~9 ~4 minecraft:vine[north=true]
fill ~5 ~6 ~10 ~5 ~11 ~10 minecraft:vine[north=true]
fill ~7 ~6 ~2 ~7 ~8 ~2 minecraft:vine[south=true]
fill ~9 ~6 ~5 ~9 ~7 ~5 minecraft:vine[west=true]
fill ~9 ~6 ~8 ~9 ~8 ~8 minecraft:vine[west=true]
setblock ~9 ~6 ~10 minecraft:vine[north=true]
setblock ~21 ~6 ~9 minecraft:vine[west=true]
fill ~21 ~6 ~13 ~21 ~7 ~13 minecraft:vine[west=true]
setblock ~22 ~6 ~3 minecraft:vine[north=true]
setblock ~22 ~6 ~19 minecraft:vine[west=true]
fill ~26 ~6 ~18 ~26 ~7 ~18 minecraft:vine[north=true]
setblock ~27 ~6 ~19 minecraft:vine[west=true]
fill ~1 ~7 ~5 ~1 ~8 ~5 minecraft:vine[east=true]
setblock ~1 ~7 ~16 minecraft:vine[east=true]
setblock ~1 ~7 ~19 minecraft:vine[east=true]
fill ~3 ~7 ~2 ~3 ~9 ~2 minecraft:vine[south=true]
fill ~3 ~7 ~8 ~3 ~9 ~8 minecraft:vine[south=true]
fill ~3 ~7 ~10 ~3 ~8 ~10 minecraft:vine[west=true]
setblock ~3 ~7 ~17 minecraft:vine[west=true]
setblock ~3 ~7 ~20 minecraft:vine[west=true]
setblock ~3 ~7 ~21 minecraft:vine[south=true]
fill ~4 ~7 ~4 ~4 ~9 ~4 minecraft:vine[north=true]
fill ~6 ~7 ~4 ~6 ~9 ~4 minecraft:vine[north=true]
fill ~9 ~7 ~9 ~9 ~8 ~9 minecraft:vine[west=true]
setblock ~13 ~7 ~22 minecraft:vine[west=true]
setblock ~15 ~7 ~21 minecraft:vine[south=true]
fill ~19 ~7 ~15 ~19 ~8 ~15 minecraft:vine[east=true]
setblock ~20 ~7 ~17 minecraft:vine[north=true]
setblock ~22 ~7 ~3 minecraft:vine[east=true]
setblock ~1 ~8 ~3 minecraft:vine[east=true]
fill ~1 ~8 ~10 ~1 ~9 ~10 minecraft:vine[east=true]
fill ~1 ~8 ~13 ~1 ~9 ~13 minecraft:vine[east=true]
setblock ~3 ~8 ~20 minecraft:vine[east=true]
setblock ~5 ~8 ~13 minecraft:vine[west=true]
fill ~7 ~8 ~6 ~7 ~8 ~7 minecraft:vine[east=true]
fill ~7 ~8 ~10 ~7 ~8 ~11 minecraft:vine[east=true]
fill ~7 ~8 ~17 ~7 ~8 ~18 minecraft:vine[east=true]
setblock ~7 ~8 ~20 minecraft:vine[east=true]
fill ~9 ~8 ~10 ~9 ~8 ~12 minecraft:vine[west=true]
fill ~11 ~8 ~16 ~11 ~8 ~17 minecraft:vine[east=true]
setblock ~11 ~8 ~21 minecraft:vine[east=true]
setblock ~12 ~8 ~13 minecraft:vine[south=true]
setblock ~13 ~8 ~14 minecraft:vine[west=true]
setblock ~13 ~8 ~18 minecraft:vine[west=true]
fill ~13 ~8 ~20 ~13 ~8 ~21 minecraft:vine[west=true]
setblock ~19 ~8 ~13 minecraft:vine[east=true]
setblock ~22 ~8 ~4 minecraft:vine[east=true]
setblock ~24 ~8 ~2 minecraft:vine[east=true]
setblock ~27 ~8 ~15 minecraft:vine[west=true]
fill ~1 ~9 ~9 ~1 ~11 ~9 minecraft:vine[east=true]
fill ~3 ~9 ~10 ~3 ~10 ~10 minecraft:vine[north=true]
setblock ~4 ~9 ~8 minecraft:vine[south=true]
fill ~7 ~9 ~7 ~7 ~10 ~7 minecraft:vine[east=true]
setblock ~7 ~9 ~10 minecraft:vine[north=true]
fill ~5 ~10 ~8 ~5 ~11 ~8 minecraft:vine[south=true]
setblock ~7 ~10 ~4 minecraft:vine[north=true]
setblock ~9 ~10 ~5 minecraft:vine[west=true]
fill ~3 ~11 ~8 ~3 ~13 ~8 minecraft:vine[west=true]
setblock ~4 ~11 ~3 minecraft:vine[west=true]
fill ~6 ~11 ~2 ~6 ~13 ~2 minecraft:vine[south=true]
setblock ~9 ~11 ~6 minecraft:vine[west=true]
fill ~1 ~12 ~15 ~1 ~13 ~15 minecraft:vine[east=true]
fill ~2 ~12 ~6 ~2 ~13 ~6 minecraft:vine[south=true]
fill ~3 ~12 ~14 ~3 ~12 ~15 minecraft:vine[west=true]
setblock ~7 ~12 ~4 minecraft:vine[north=true]
setblock ~2 ~13 ~5 minecraft:vine[north=true]
fill ~3 ~13 ~4 ~3 ~15 ~4 minecraft:vine[north=true]
fill ~4 ~13 ~3 ~4 ~14 ~3 minecraft:vine[west=true]
setblock ~1 ~14 ~7 minecraft:vine[east=true]
fill ~2 ~14 ~2 ~2 ~15 ~2 minecraft:vine[south=true]
setblock ~2 ~14 ~8 minecraft:vine[north=true]
setblock ~4 ~14 ~14 minecraft:vine[south=true]
setblock ~3 ~15 ~5 minecraft:vine[south=true]
setblock ~4 ~15 ~7 minecraft:vine[north=true]
setblock ~5 ~15 ~5 minecraft:vine[south=true]
setblock ~7 ~15 ~7 minecraft:vine[north=true]
setblock ~3 ~16 ~4 minecraft:vine[west=true]

# cobwebs in the corners of the nave and the tower and under the roof ribs
setblock ~3 ~2 ~20 minecraft:cobweb
fill ~7 ~2 ~4 ~7 ~3 ~4 minecraft:cobweb
setblock ~7 ~2 ~8 minecraft:cobweb
setblock ~19 ~2 ~21 minecraft:cobweb
setblock ~21 ~2 ~13 minecraft:cobweb
setblock ~25 ~2 ~14 minecraft:cobweb
setblock ~25 ~2 ~17 minecraft:cobweb
setblock ~3 ~3 ~4 minecraft:cobweb
setblock ~3 ~3 ~21 minecraft:cobweb
setblock ~21 ~3 ~20 minecraft:cobweb
setblock ~18 ~4 ~9 minecraft:cobweb
setblock ~19 ~4 ~21 minecraft:cobweb
setblock ~21 ~4 ~13 minecraft:cobweb
setblock ~24 ~4 ~19 minecraft:cobweb
setblock ~3 ~6 ~8 minecraft:cobweb
setblock ~3 ~6 ~10 minecraft:cobweb
setblock ~3 ~6 ~21 minecraft:cobweb
setblock ~3 ~7 ~4 minecraft:cobweb
setblock ~4 ~7 ~10 minecraft:cobweb
setblock ~8 ~7 ~21 minecraft:cobweb
setblock ~12 ~7 ~21 minecraft:cobweb
setblock ~23 ~7 ~4 minecraft:cobweb
setblock ~3 ~8 ~11 minecraft:cobweb
setblock ~3 ~8 ~18 minecraft:cobweb
setblock ~3 ~8 ~21 minecraft:cobweb
setblock ~7 ~9 ~8 minecraft:cobweb
setblock ~3 ~10 ~8 minecraft:cobweb
fill ~3 ~11 ~4 ~3 ~12 ~4 minecraft:cobweb
setblock ~7 ~11 ~4 minecraft:cobweb
