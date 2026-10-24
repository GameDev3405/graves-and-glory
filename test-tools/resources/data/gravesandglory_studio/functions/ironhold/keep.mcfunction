# size 47 22 27
# ironhold/keep: river keep of Ironhold: curtain walls with a wall-walk and crenels crossing the river on iron bars, two land gates in each wall, four corner towers, a timber deck over the river and a donjon on the east bank.
# Local coordinates from the minimum corner; y 0 is the ground layer. Cells no line sets are structure void and keep the ground.
# Written feature by feature: what a block hangs on (a wall, a pole, a chain, a ceiling) is always set before it.
# North wall z 0..2, south wall z 24..26, west wall x 0..2, east wall x 44..46: solid y 1..6, wall-walk feet 7, parapet on the outer face y 7..8 (crenels where the coordinate along the wall is 2 or 3 mod 6, so x 14..15 on the north wall), land gates x 7..11 and 35..39 (y 1..4) through both long walls, iron bars at x 18..28, y 0..1.
# Corner towers 7 x 7 at x 0..6 / 40..46 and z 0..6 / 20..26: ladder (5, 1..13, 4) on the inner face, door on the courtyard side (4, 1..2, 6), the wall-walk enters the upper room (feet 7) at both inner faces; top floor y 12 (feet 13), parapet y 13, merlons y 14.
# Stairs up to the wall-walk on the inner faces of the long walls: x 12..17 (west bank) and x 34 down to 29 (east bank), z 3 and z 23. Deck x 18..28, z 3..23 at y 0. Donjon x 31..41, z 8..18 (floors y 6, 12, platform y 17); ladder (40, 1..18, 13); door (31, 1..3, 12..14); turrets 3 x 3 at the corners of the platform (y 18..20, roofs y 21).

# the clearing: air over the piece from y 1 up, so nothing the battlefield built here before stands in it
fill ~1 ~1 ~3 ~1 ~5 ~5 minecraft:air
fill ~1 ~1 ~21 ~5 ~5 ~21 minecraft:air
fill ~1 ~1 ~22 ~4 ~3 ~23 minecraft:air
fill ~2 ~1 ~2 ~2 ~5 ~5 minecraft:air
fill ~2 ~1 ~24 ~4 ~5 ~24 minecraft:air
fill ~3 ~1 ~1 ~5 ~3 ~3 minecraft:air
fill ~3 ~1 ~4 ~4 ~5 ~5 minecraft:air
fill ~3 ~1 ~7 ~6 ~21 ~8 minecraft:air
fill ~3 ~1 ~9 ~4 ~4 ~9 minecraft:air
fill ~3 ~1 ~17 ~3 ~4 ~19 minecraft:air
fill ~3 ~1 ~25 ~4 ~5 ~25 minecraft:air
fill ~4 ~1 ~11 ~4 ~4 ~12 minecraft:air
fill ~4 ~1 ~15 ~4 ~4 ~19 minecraft:air
fill ~5 ~1 ~5 ~5 ~5 ~5 minecraft:air
fill ~5 ~1 ~10 ~5 ~4 ~12 minecraft:air
fill ~5 ~1 ~14 ~5 ~4 ~16 minecraft:air
fill ~5 ~1 ~18 ~5 ~21 ~19 minecraft:air
fill ~5 ~1 ~23 ~5 ~5 ~25 minecraft:air
fill ~6 ~1 ~9 ~6 ~21 ~19 minecraft:air
fill ~7 ~1 ~0 ~11 ~4 ~26 minecraft:air
fill ~12 ~1 ~4 ~17 ~21 ~17 minecraft:air
fill ~12 ~1 ~18 ~12 ~21 ~20 minecraft:air
fill ~12 ~1 ~22 ~16 ~21 ~22 minecraft:air
fill ~16 ~1 ~18 ~17 ~21 ~20 minecraft:air
fill ~17 ~1 ~21 ~17 ~21 ~22 minecraft:air
fill ~18 ~1 ~1 ~28 ~1 ~1 minecraft:air
fill ~18 ~1 ~3 ~28 ~21 ~4 minecraft:air
fill ~18 ~1 ~5 ~19 ~21 ~23 minecraft:air
fill ~18 ~1 ~25 ~28 ~1 ~25 minecraft:air
fill ~20 ~1 ~6 ~20 ~21 ~20 minecraft:air
fill ~20 ~1 ~22 ~20 ~21 ~23 minecraft:air
fill ~21 ~1 ~5 ~25 ~21 ~23 minecraft:air
fill ~26 ~1 ~6 ~26 ~21 ~20 minecraft:air
fill ~26 ~1 ~22 ~26 ~21 ~23 minecraft:air
fill ~27 ~1 ~5 ~32 ~1 ~7 minecraft:air
fill ~27 ~1 ~8 ~30 ~21 ~8 minecraft:air
fill ~27 ~1 ~9 ~29 ~4 ~22 minecraft:air
fill ~27 ~1 ~23 ~28 ~21 ~23 minecraft:air
fill ~29 ~1 ~4 ~34 ~21 ~4 minecraft:air
fill ~30 ~1 ~10 ~30 ~4 ~16 minecraft:air
fill ~30 ~1 ~18 ~30 ~21 ~22 minecraft:air
fill ~31 ~1 ~19 ~33 ~21 ~20 minecraft:air
fill ~31 ~1 ~21 ~32 ~1 ~22 minecraft:air
fill ~32 ~1 ~11 ~32 ~5 ~15 minecraft:air
fill ~33 ~1 ~6 ~33 ~21 ~7 minecraft:air
fill ~33 ~1 ~10 ~33 ~5 ~16 minecraft:air
fill ~33 ~1 ~22 ~33 ~21 ~22 minecraft:air
fill ~34 ~1 ~5 ~36 ~1 ~6 minecraft:air
fill ~34 ~1 ~9 ~38 ~5 ~10 minecraft:air
fill ~34 ~1 ~12 ~34 ~5 ~14 minecraft:air
fill ~34 ~1 ~16 ~34 ~5 ~17 minecraft:air
fill ~34 ~1 ~20 ~34 ~1 ~22 minecraft:air
fill ~35 ~1 ~0 ~39 ~4 ~4 minecraft:air
fill ~35 ~1 ~7 ~36 ~21 ~7 minecraft:air
fill ~35 ~1 ~11 ~37 ~3 ~17 minecraft:air
fill ~35 ~1 ~19 ~37 ~21 ~20 minecraft:air
fill ~35 ~1 ~21 ~36 ~1 ~26 minecraft:air
fill ~37 ~1 ~6 ~37 ~21 ~7 minecraft:air
fill ~37 ~1 ~22 ~37 ~4 ~26 minecraft:air
fill ~38 ~1 ~5 ~39 ~1 ~6 minecraft:air
fill ~38 ~1 ~12 ~38 ~5 ~14 minecraft:air
fill ~38 ~1 ~16 ~38 ~5 ~17 minecraft:air
fill ~38 ~1 ~20 ~39 ~1 ~26 minecraft:air
fill ~39 ~1 ~7 ~42 ~21 ~7 minecraft:air
fill ~39 ~1 ~10 ~40 ~5 ~12 minecraft:air
fill ~39 ~1 ~13 ~39 ~5 ~16 minecraft:air
fill ~39 ~1 ~19 ~41 ~21 ~19 minecraft:air
fill ~40 ~1 ~14 ~40 ~5 ~16 minecraft:air
fill ~41 ~1 ~1 ~43 ~3 ~3 minecraft:air
fill ~41 ~1 ~5 ~42 ~5 ~5 minecraft:air
fill ~41 ~1 ~21 ~41 ~5 ~21 minecraft:air
fill ~41 ~1 ~23 ~41 ~5 ~25 minecraft:air
fill ~42 ~1 ~4 ~43 ~5 ~4 minecraft:air
fill ~42 ~1 ~8 ~43 ~21 ~8 minecraft:air
fill ~42 ~1 ~10 ~42 ~21 ~16 minecraft:air
fill ~42 ~1 ~18 ~42 ~21 ~19 minecraft:air
fill ~42 ~1 ~22 ~43 ~3 ~25 minecraft:air
fill ~43 ~1 ~9 ~43 ~21 ~18 minecraft:air
fill ~44 ~1 ~2 ~44 ~5 ~5 minecraft:air
fill ~44 ~1 ~21 ~45 ~5 ~23 minecraft:air
fill ~44 ~1 ~24 ~44 ~5 ~24 minecraft:air
fill ~45 ~1 ~3 ~45 ~5 ~5 minecraft:air
fill ~1 ~2 ~2 ~1 ~5 ~2 minecraft:air
fill ~1 ~2 ~24 ~1 ~5 ~24 minecraft:air
fill ~2 ~2 ~1 ~2 ~5 ~1 minecraft:air
fill ~2 ~2 ~25 ~2 ~5 ~25 minecraft:air
fill ~3 ~2 ~11 ~3 ~4 ~12 minecraft:air
fill ~3 ~2 ~15 ~3 ~4 ~16 minecraft:air
fill ~4 ~2 ~10 ~4 ~4 ~10 minecraft:air
fill ~4 ~2 ~13 ~4 ~4 ~14 minecraft:air
fill ~12 ~2 ~3 ~12 ~21 ~3 minecraft:air
fill ~12 ~2 ~21 ~12 ~21 ~21 minecraft:air
fill ~12 ~2 ~23 ~12 ~21 ~23 minecraft:air
setblock ~14 ~2 ~19 minecraft:air
fill ~14 ~2 ~21 ~14 ~21 ~21 minecraft:air
fill ~16 ~2 ~21 ~16 ~21 ~21 minecraft:air
fill ~27 ~2 ~5 ~31 ~21 ~7 minecraft:air
fill ~31 ~2 ~21 ~31 ~21 ~22 minecraft:air
fill ~32 ~2 ~6 ~32 ~21 ~7 minecraft:air
fill ~32 ~2 ~10 ~32 ~5 ~10 minecraft:air
fill ~32 ~2 ~16 ~32 ~5 ~17 minecraft:air
fill ~32 ~2 ~22 ~32 ~21 ~22 minecraft:air
fill ~33 ~2 ~9 ~33 ~5 ~9 minecraft:air
fill ~33 ~2 ~17 ~33 ~5 ~17 minecraft:air
fill ~34 ~2 ~3 ~34 ~21 ~3 minecraft:air
fill ~34 ~2 ~6 ~34 ~21 ~6 minecraft:air
fill ~34 ~2 ~20 ~34 ~21 ~20 minecraft:air
fill ~34 ~2 ~22 ~34 ~21 ~23 minecraft:air
fill ~35 ~2 ~5 ~35 ~21 ~6 minecraft:air
fill ~35 ~2 ~21 ~35 ~4 ~26 minecraft:air
fill ~36 ~2 ~6 ~36 ~21 ~6 minecraft:air
fill ~36 ~2 ~22 ~36 ~4 ~26 minecraft:air
fill ~38 ~2 ~6 ~38 ~21 ~6 minecraft:air
fill ~38 ~2 ~20 ~39 ~21 ~20 minecraft:air
fill ~38 ~2 ~22 ~38 ~4 ~26 minecraft:air
fill ~39 ~2 ~5 ~39 ~21 ~6 minecraft:air
fill ~39 ~2 ~9 ~40 ~5 ~9 minecraft:air
fill ~39 ~2 ~17 ~40 ~5 ~17 minecraft:air
fill ~39 ~2 ~21 ~39 ~4 ~26 minecraft:air
fill ~42 ~2 ~21 ~43 ~5 ~21 minecraft:air
fill ~43 ~2 ~7 ~43 ~21 ~7 minecraft:air
fill ~43 ~2 ~19 ~43 ~21 ~19 minecraft:air
fill ~44 ~2 ~1 ~44 ~5 ~1 minecraft:air
fill ~44 ~2 ~25 ~44 ~5 ~25 minecraft:air
fill ~45 ~2 ~2 ~45 ~5 ~2 minecraft:air
fill ~45 ~2 ~24 ~45 ~5 ~24 minecraft:air
fill ~1 ~3 ~1 ~1 ~5 ~1 minecraft:air
fill ~1 ~3 ~25 ~1 ~5 ~25 minecraft:air
fill ~3 ~3 ~10 ~3 ~4 ~10 minecraft:air
fill ~3 ~3 ~13 ~3 ~4 ~14 minecraft:air
fill ~13 ~3 ~3 ~13 ~21 ~3 minecraft:air
fill ~13 ~3 ~19 ~13 ~4 ~19 minecraft:air
fill ~13 ~3 ~21 ~13 ~21 ~21 minecraft:air
fill ~13 ~3 ~23 ~13 ~21 ~23 minecraft:air
fill ~14 ~3 ~18 ~14 ~4 ~18 minecraft:air
fill ~14 ~3 ~20 ~14 ~4 ~20 minecraft:air
fill ~15 ~3 ~19 ~15 ~4 ~19 minecraft:air
fill ~15 ~3 ~21 ~15 ~21 ~21 minecraft:air
fill ~32 ~3 ~5 ~32 ~21 ~5 minecraft:air
fill ~32 ~3 ~9 ~32 ~5 ~9 minecraft:air
fill ~32 ~3 ~21 ~32 ~21 ~21 minecraft:air
fill ~33 ~3 ~3 ~33 ~21 ~3 minecraft:air
fill ~33 ~3 ~23 ~33 ~21 ~23 minecraft:air
fill ~34 ~3 ~5 ~34 ~21 ~5 minecraft:air
fill ~34 ~3 ~21 ~34 ~21 ~21 minecraft:air
fill ~36 ~3 ~5 ~36 ~21 ~5 minecraft:air
fill ~36 ~3 ~21 ~36 ~21 ~21 minecraft:air
fill ~38 ~3 ~5 ~38 ~21 ~5 minecraft:air
fill ~38 ~3 ~21 ~38 ~21 ~21 minecraft:air
fill ~43 ~3 ~5 ~43 ~5 ~5 minecraft:air
fill ~45 ~3 ~1 ~45 ~5 ~1 minecraft:air
fill ~45 ~3 ~25 ~45 ~5 ~25 minecraft:air
fill ~1 ~4 ~22 ~4 ~5 ~22 minecraft:air
fill ~1 ~4 ~23 ~2 ~5 ~23 minecraft:air
fill ~3 ~4 ~1 ~5 ~5 ~2 minecraft:air
fill ~4 ~4 ~3 ~5 ~5 ~3 minecraft:air
fill ~4 ~4 ~23 ~4 ~5 ~23 minecraft:air
fill ~14 ~4 ~3 ~14 ~21 ~3 minecraft:air
fill ~14 ~4 ~23 ~14 ~21 ~23 minecraft:air
fill ~32 ~4 ~3 ~32 ~21 ~3 minecraft:air
fill ~32 ~4 ~23 ~32 ~21 ~23 minecraft:air
fill ~33 ~4 ~5 ~33 ~21 ~5 minecraft:air
fill ~33 ~4 ~21 ~33 ~21 ~21 minecraft:air
fill ~35 ~4 ~11 ~37 ~5 ~12 minecraft:air
fill ~35 ~4 ~13 ~35 ~5 ~17 minecraft:air
fill ~36 ~4 ~14 ~36 ~5 ~17 minecraft:air
fill ~37 ~4 ~5 ~37 ~21 ~5 minecraft:air
fill ~37 ~4 ~13 ~37 ~5 ~17 minecraft:air
fill ~37 ~4 ~21 ~37 ~21 ~21 minecraft:air
fill ~41 ~4 ~1 ~43 ~5 ~2 minecraft:air
fill ~41 ~4 ~3 ~42 ~5 ~3 minecraft:air
fill ~42 ~4 ~22 ~43 ~5 ~22 minecraft:air
fill ~42 ~4 ~23 ~42 ~5 ~25 minecraft:air
fill ~43 ~4 ~24 ~43 ~5 ~25 minecraft:air
fill ~3 ~5 ~18 ~4 ~21 ~19 minecraft:air
fill ~7 ~5 ~3 ~11 ~21 ~23 minecraft:air
fill ~15 ~5 ~3 ~15 ~21 ~3 minecraft:air
fill ~15 ~5 ~23 ~15 ~21 ~23 minecraft:air
fill ~20 ~5 ~5 ~20 ~21 ~5 minecraft:air
fill ~20 ~5 ~21 ~20 ~21 ~21 minecraft:air
fill ~26 ~5 ~5 ~26 ~21 ~5 minecraft:air
fill ~26 ~5 ~21 ~26 ~21 ~21 minecraft:air
fill ~27 ~5 ~9 ~29 ~6 ~11 minecraft:air
fill ~27 ~5 ~12 ~28 ~21 ~22 minecraft:air
fill ~29 ~5 ~15 ~30 ~6 ~16 minecraft:air
fill ~29 ~5 ~17 ~29 ~21 ~22 minecraft:air
fill ~30 ~5 ~10 ~30 ~6 ~11 minecraft:air
fill ~31 ~5 ~3 ~31 ~21 ~3 minecraft:air
fill ~31 ~5 ~23 ~31 ~21 ~23 minecraft:air
fill ~35 ~5 ~3 ~39 ~21 ~4 minecraft:air
fill ~35 ~5 ~21 ~35 ~21 ~23 minecraft:air
fill ~36 ~5 ~22 ~38 ~21 ~23 minecraft:air
fill ~39 ~5 ~21 ~39 ~21 ~23 minecraft:air
fill ~3 ~6 ~9 ~5 ~21 ~17 minecraft:air
fill ~13 ~6 ~18 ~15 ~21 ~20 minecraft:air
fill ~16 ~6 ~3 ~16 ~21 ~3 minecraft:air
fill ~16 ~6 ~23 ~16 ~21 ~23 minecraft:air
fill ~30 ~6 ~3 ~30 ~21 ~3 minecraft:air
fill ~30 ~6 ~23 ~30 ~21 ~23 minecraft:air
fill ~0 ~7 ~8 ~0 ~21 ~9 minecraft:air
fill ~0 ~7 ~14 ~0 ~21 ~15 minecraft:air
fill ~1 ~7 ~1 ~5 ~9 ~3 minecraft:air
fill ~1 ~7 ~4 ~4 ~11 ~4 minecraft:air
fill ~1 ~7 ~7 ~2 ~21 ~9 minecraft:air
fill ~1 ~7 ~10 ~1 ~21 ~19 minecraft:air
fill ~1 ~7 ~22 ~4 ~9 ~25 minecraft:air
fill ~2 ~7 ~5 ~5 ~11 ~5 minecraft:air
fill ~2 ~7 ~11 ~2 ~21 ~15 minecraft:air
fill ~2 ~7 ~17 ~2 ~21 ~19 minecraft:air
fill ~2 ~7 ~21 ~5 ~11 ~21 minecraft:air
fill ~5 ~7 ~23 ~5 ~11 ~25 minecraft:air
fill ~7 ~7 ~1 ~7 ~21 ~2 minecraft:air
fill ~7 ~7 ~24 ~12 ~21 ~25 minecraft:air
fill ~8 ~7 ~0 ~9 ~21 ~2 minecraft:air
fill ~8 ~7 ~26 ~9 ~21 ~26 minecraft:air
fill ~10 ~7 ~1 ~13 ~21 ~1 minecraft:air
fill ~10 ~7 ~2 ~12 ~21 ~2 minecraft:air
fill ~13 ~7 ~25 ~13 ~21 ~25 minecraft:air
fill ~14 ~7 ~0 ~15 ~21 ~2 minecraft:air
fill ~14 ~7 ~24 ~16 ~21 ~25 minecraft:air
fill ~14 ~7 ~26 ~15 ~21 ~26 minecraft:air
fill ~16 ~7 ~1 ~19 ~21 ~2 minecraft:air
fill ~17 ~7 ~3 ~17 ~21 ~3 minecraft:air
fill ~17 ~7 ~23 ~17 ~21 ~25 minecraft:air
fill ~18 ~7 ~24 ~23 ~21 ~25 minecraft:air
fill ~20 ~7 ~0 ~21 ~21 ~2 minecraft:air
fill ~20 ~7 ~26 ~21 ~21 ~26 minecraft:air
fill ~22 ~7 ~1 ~25 ~21 ~1 minecraft:air
fill ~22 ~7 ~2 ~23 ~21 ~2 minecraft:air
fill ~24 ~7 ~25 ~24 ~21 ~25 minecraft:air
fill ~25 ~7 ~2 ~25 ~21 ~2 minecraft:air
fill ~25 ~7 ~24 ~28 ~21 ~25 minecraft:air
fill ~26 ~7 ~0 ~27 ~21 ~2 minecraft:air
fill ~26 ~7 ~26 ~27 ~21 ~26 minecraft:air
fill ~27 ~7 ~9 ~30 ~21 ~10 minecraft:air
fill ~27 ~7 ~11 ~28 ~21 ~11 minecraft:air
fill ~28 ~7 ~1 ~31 ~21 ~2 minecraft:air
fill ~29 ~7 ~3 ~29 ~21 ~3 minecraft:air
fill ~29 ~7 ~16 ~30 ~21 ~16 minecraft:air
fill ~29 ~7 ~23 ~29 ~21 ~25 minecraft:air
fill ~30 ~7 ~12 ~30 ~21 ~14 minecraft:air
fill ~30 ~7 ~17 ~30 ~21 ~17 minecraft:air
fill ~30 ~7 ~24 ~32 ~21 ~25 minecraft:air
fill ~32 ~7 ~0 ~33 ~21 ~1 minecraft:air
fill ~32 ~7 ~2 ~32 ~21 ~2 minecraft:air
fill ~32 ~7 ~10 ~33 ~11 ~17 minecraft:air
fill ~32 ~7 ~26 ~32 ~21 ~26 minecraft:air
fill ~33 ~7 ~25 ~33 ~21 ~26 minecraft:air
fill ~34 ~7 ~1 ~37 ~21 ~2 minecraft:air
fill ~34 ~7 ~7 ~34 ~21 ~7 minecraft:air
fill ~34 ~7 ~9 ~38 ~9 ~15 minecraft:air
fill ~34 ~7 ~19 ~34 ~21 ~19 minecraft:air
fill ~34 ~7 ~24 ~39 ~21 ~25 minecraft:air
fill ~35 ~7 ~16 ~35 ~11 ~16 minecraft:air
fill ~37 ~7 ~16 ~37 ~11 ~16 minecraft:air
fill ~38 ~7 ~0 ~39 ~21 ~2 minecraft:air
fill ~38 ~7 ~7 ~38 ~21 ~7 minecraft:air
fill ~38 ~7 ~19 ~38 ~21 ~19 minecraft:air
fill ~38 ~7 ~26 ~39 ~21 ~26 minecraft:air
fill ~39 ~7 ~11 ~39 ~11 ~17 minecraft:air
fill ~40 ~7 ~9 ~40 ~11 ~12 minecraft:air
fill ~40 ~7 ~14 ~40 ~11 ~17 minecraft:air
fill ~41 ~7 ~1 ~45 ~9 ~3 minecraft:air
fill ~41 ~7 ~5 ~44 ~11 ~5 minecraft:air
fill ~41 ~7 ~21 ~44 ~11 ~21 minecraft:air
fill ~41 ~7 ~23 ~41 ~11 ~25 minecraft:air
fill ~42 ~7 ~4 ~45 ~11 ~4 minecraft:air
fill ~42 ~7 ~9 ~42 ~21 ~9 minecraft:air
fill ~42 ~7 ~17 ~42 ~21 ~17 minecraft:air
fill ~42 ~7 ~22 ~45 ~9 ~25 minecraft:air
fill ~44 ~7 ~7 ~45 ~21 ~9 minecraft:air
fill ~44 ~7 ~11 ~44 ~21 ~15 minecraft:air
fill ~44 ~7 ~17 ~44 ~21 ~19 minecraft:air
fill ~45 ~7 ~10 ~45 ~21 ~19 minecraft:air
fill ~46 ~7 ~8 ~46 ~21 ~9 minecraft:air
fill ~46 ~7 ~14 ~46 ~21 ~15 minecraft:air
fill ~1 ~8 ~5 ~1 ~11 ~5 minecraft:air
fill ~1 ~8 ~21 ~1 ~11 ~21 minecraft:air
fill ~29 ~8 ~11 ~30 ~21 ~11 minecraft:air
fill ~29 ~8 ~12 ~29 ~21 ~15 minecraft:air
fill ~30 ~8 ~15 ~30 ~21 ~15 minecraft:air
fill ~32 ~8 ~9 ~33 ~11 ~9 minecraft:air
fill ~34 ~8 ~16 ~34 ~11 ~16 minecraft:air
fill ~36 ~8 ~16 ~36 ~11 ~16 minecraft:air
fill ~38 ~8 ~16 ~38 ~11 ~16 minecraft:air
fill ~39 ~8 ~9 ~39 ~11 ~10 minecraft:air
fill ~45 ~8 ~5 ~45 ~11 ~5 minecraft:air
fill ~45 ~8 ~21 ~45 ~11 ~21 minecraft:air
fill ~2 ~9 ~10 ~2 ~21 ~10 minecraft:air
fill ~2 ~9 ~16 ~2 ~21 ~16 minecraft:air
fill ~13 ~9 ~2 ~13 ~21 ~2 minecraft:air
fill ~13 ~9 ~24 ~13 ~21 ~24 minecraft:air
fill ~24 ~9 ~2 ~24 ~21 ~2 minecraft:air
fill ~24 ~9 ~24 ~24 ~21 ~24 minecraft:air
fill ~33 ~9 ~2 ~33 ~21 ~2 minecraft:air
fill ~33 ~9 ~24 ~33 ~21 ~24 minecraft:air
fill ~34 ~9 ~17 ~38 ~11 ~17 minecraft:air
fill ~44 ~9 ~10 ~44 ~21 ~10 minecraft:air
fill ~44 ~9 ~16 ~44 ~21 ~16 minecraft:air
fill ~0 ~10 ~7 ~0 ~21 ~7 minecraft:air
fill ~0 ~10 ~10 ~0 ~21 ~13 minecraft:air
fill ~0 ~10 ~16 ~0 ~21 ~19 minecraft:air
fill ~1 ~10 ~1 ~5 ~11 ~2 minecraft:air
fill ~1 ~10 ~3 ~2 ~11 ~3 minecraft:air
fill ~1 ~10 ~22 ~4 ~11 ~22 minecraft:air
fill ~1 ~10 ~23 ~2 ~11 ~25 minecraft:air
fill ~3 ~10 ~24 ~3 ~11 ~25 minecraft:air
fill ~4 ~10 ~3 ~5 ~11 ~3 minecraft:air
fill ~4 ~10 ~23 ~4 ~11 ~25 minecraft:air
fill ~7 ~10 ~0 ~7 ~21 ~0 minecraft:air
fill ~7 ~10 ~26 ~7 ~21 ~26 minecraft:air
fill ~10 ~10 ~0 ~13 ~21 ~0 minecraft:air
fill ~10 ~10 ~26 ~13 ~21 ~26 minecraft:air
fill ~16 ~10 ~0 ~19 ~21 ~0 minecraft:air
fill ~16 ~10 ~26 ~19 ~21 ~26 minecraft:air
fill ~22 ~10 ~0 ~25 ~21 ~0 minecraft:air
fill ~22 ~10 ~26 ~25 ~21 ~26 minecraft:air
fill ~28 ~10 ~0 ~31 ~21 ~0 minecraft:air
fill ~28 ~10 ~26 ~31 ~21 ~26 minecraft:air
fill ~34 ~10 ~0 ~37 ~21 ~0 minecraft:air
fill ~34 ~10 ~9 ~38 ~11 ~12 minecraft:air
fill ~34 ~10 ~13 ~35 ~11 ~15 minecraft:air
fill ~34 ~10 ~26 ~37 ~21 ~26 minecraft:air
fill ~36 ~10 ~14 ~36 ~11 ~15 minecraft:air
fill ~37 ~10 ~13 ~38 ~11 ~15 minecraft:air
fill ~41 ~10 ~1 ~45 ~11 ~2 minecraft:air
fill ~41 ~10 ~3 ~42 ~11 ~3 minecraft:air
fill ~42 ~10 ~22 ~45 ~11 ~22 minecraft:air
fill ~42 ~10 ~23 ~42 ~11 ~25 minecraft:air
fill ~43 ~10 ~24 ~43 ~11 ~25 minecraft:air
fill ~44 ~10 ~3 ~45 ~11 ~3 minecraft:air
fill ~44 ~10 ~23 ~45 ~11 ~25 minecraft:air
fill ~46 ~10 ~7 ~46 ~21 ~7 minecraft:air
fill ~46 ~10 ~10 ~46 ~21 ~13 minecraft:air
fill ~46 ~10 ~16 ~46 ~21 ~19 minecraft:air
fill ~1 ~13 ~1 ~5 ~21 ~3 minecraft:air
fill ~1 ~13 ~4 ~4 ~21 ~5 minecraft:air
fill ~1 ~13 ~21 ~5 ~21 ~21 minecraft:air
fill ~1 ~13 ~22 ~4 ~21 ~25 minecraft:air
fill ~5 ~13 ~5 ~5 ~21 ~5 minecraft:air
fill ~5 ~13 ~23 ~5 ~21 ~25 minecraft:air
fill ~32 ~13 ~9 ~40 ~16 ~9 minecraft:air
fill ~32 ~13 ~17 ~32 ~16 ~17 minecraft:air
fill ~33 ~13 ~10 ~37 ~16 ~17 minecraft:air
fill ~38 ~13 ~11 ~39 ~16 ~15 minecraft:air
fill ~38 ~13 ~17 ~38 ~16 ~17 minecraft:air
fill ~39 ~13 ~16 ~39 ~16 ~17 minecraft:air
fill ~40 ~13 ~10 ~40 ~16 ~12 minecraft:air
fill ~40 ~13 ~14 ~40 ~16 ~17 minecraft:air
fill ~41 ~13 ~1 ~45 ~21 ~3 minecraft:air
fill ~41 ~13 ~5 ~41 ~21 ~5 minecraft:air
fill ~41 ~13 ~21 ~45 ~21 ~21 minecraft:air
fill ~41 ~13 ~23 ~41 ~21 ~25 minecraft:air
fill ~42 ~13 ~4 ~45 ~21 ~5 minecraft:air
fill ~42 ~13 ~22 ~45 ~21 ~25 minecraft:air
fill ~0 ~14 ~1 ~0 ~21 ~1 minecraft:air
fill ~0 ~14 ~3 ~0 ~21 ~3 minecraft:air
fill ~0 ~14 ~5 ~0 ~21 ~5 minecraft:air
fill ~0 ~14 ~21 ~0 ~21 ~21 minecraft:air
fill ~0 ~14 ~23 ~0 ~21 ~23 minecraft:air
fill ~0 ~14 ~25 ~0 ~21 ~25 minecraft:air
fill ~1 ~14 ~0 ~1 ~21 ~0 minecraft:air
fill ~1 ~14 ~6 ~1 ~21 ~6 minecraft:air
fill ~1 ~14 ~20 ~1 ~21 ~20 minecraft:air
fill ~1 ~14 ~26 ~1 ~21 ~26 minecraft:air
fill ~3 ~14 ~0 ~3 ~21 ~0 minecraft:air
fill ~3 ~14 ~6 ~3 ~21 ~6 minecraft:air
fill ~3 ~14 ~20 ~3 ~21 ~20 minecraft:air
fill ~3 ~14 ~26 ~3 ~21 ~26 minecraft:air
fill ~5 ~14 ~0 ~5 ~21 ~0 minecraft:air
fill ~5 ~14 ~4 ~5 ~21 ~4 minecraft:air
fill ~5 ~14 ~6 ~5 ~21 ~6 minecraft:air
fill ~5 ~14 ~20 ~5 ~21 ~20 minecraft:air
fill ~5 ~14 ~22 ~5 ~21 ~22 minecraft:air
fill ~5 ~14 ~26 ~5 ~21 ~26 minecraft:air
fill ~6 ~14 ~1 ~6 ~21 ~1 minecraft:air
fill ~6 ~14 ~3 ~6 ~21 ~3 minecraft:air
fill ~6 ~14 ~5 ~6 ~21 ~5 minecraft:air
fill ~6 ~14 ~21 ~6 ~21 ~21 minecraft:air
fill ~6 ~14 ~23 ~6 ~21 ~23 minecraft:air
fill ~6 ~14 ~25 ~6 ~21 ~25 minecraft:air
fill ~32 ~14 ~10 ~32 ~16 ~10 minecraft:air
fill ~32 ~14 ~12 ~32 ~16 ~12 minecraft:air
fill ~32 ~14 ~14 ~32 ~16 ~14 minecraft:air
fill ~32 ~14 ~16 ~32 ~16 ~16 minecraft:air
fill ~38 ~14 ~10 ~39 ~16 ~10 minecraft:air
fill ~38 ~14 ~16 ~38 ~16 ~16 minecraft:air
fill ~40 ~14 ~1 ~40 ~21 ~1 minecraft:air
fill ~40 ~14 ~3 ~40 ~21 ~3 minecraft:air
fill ~40 ~14 ~5 ~40 ~21 ~5 minecraft:air
fill ~40 ~14 ~21 ~40 ~21 ~21 minecraft:air
fill ~40 ~14 ~23 ~40 ~21 ~23 minecraft:air
fill ~40 ~14 ~25 ~40 ~21 ~25 minecraft:air
fill ~41 ~14 ~0 ~41 ~21 ~0 minecraft:air
fill ~41 ~14 ~4 ~41 ~21 ~4 minecraft:air
fill ~41 ~14 ~6 ~41 ~21 ~6 minecraft:air
fill ~41 ~14 ~20 ~41 ~21 ~20 minecraft:air
fill ~41 ~14 ~22 ~41 ~21 ~22 minecraft:air
fill ~41 ~14 ~26 ~41 ~21 ~26 minecraft:air
fill ~43 ~14 ~0 ~43 ~21 ~0 minecraft:air
fill ~43 ~14 ~6 ~43 ~21 ~6 minecraft:air
fill ~43 ~14 ~20 ~43 ~21 ~20 minecraft:air
fill ~43 ~14 ~26 ~43 ~21 ~26 minecraft:air
fill ~45 ~14 ~0 ~45 ~21 ~0 minecraft:air
fill ~45 ~14 ~6 ~45 ~21 ~6 minecraft:air
fill ~45 ~14 ~20 ~45 ~21 ~20 minecraft:air
fill ~45 ~14 ~26 ~45 ~21 ~26 minecraft:air
fill ~46 ~14 ~1 ~46 ~21 ~1 minecraft:air
fill ~46 ~14 ~3 ~46 ~21 ~3 minecraft:air
fill ~46 ~14 ~5 ~46 ~21 ~5 minecraft:air
fill ~46 ~14 ~21 ~46 ~21 ~21 minecraft:air
fill ~46 ~14 ~23 ~46 ~21 ~23 minecraft:air
fill ~46 ~14 ~25 ~46 ~21 ~25 minecraft:air
fill ~0 ~15 ~2 ~0 ~21 ~2 minecraft:air
fill ~0 ~15 ~4 ~0 ~21 ~4 minecraft:air
fill ~0 ~15 ~22 ~0 ~21 ~22 minecraft:air
fill ~0 ~15 ~24 ~0 ~21 ~24 minecraft:air
fill ~2 ~15 ~0 ~2 ~21 ~0 minecraft:air
fill ~2 ~15 ~6 ~2 ~21 ~6 minecraft:air
fill ~2 ~15 ~20 ~2 ~21 ~20 minecraft:air
fill ~2 ~15 ~26 ~2 ~21 ~26 minecraft:air
fill ~4 ~15 ~0 ~4 ~21 ~0 minecraft:air
fill ~4 ~15 ~6 ~4 ~21 ~6 minecraft:air
fill ~4 ~15 ~20 ~4 ~21 ~20 minecraft:air
fill ~4 ~15 ~26 ~4 ~21 ~26 minecraft:air
fill ~6 ~15 ~2 ~6 ~21 ~2 minecraft:air
fill ~6 ~15 ~4 ~6 ~21 ~4 minecraft:air
fill ~6 ~15 ~22 ~6 ~21 ~22 minecraft:air
fill ~6 ~15 ~24 ~6 ~21 ~24 minecraft:air
fill ~32 ~15 ~11 ~32 ~16 ~11 minecraft:air
fill ~32 ~15 ~13 ~32 ~16 ~13 minecraft:air
fill ~32 ~15 ~15 ~32 ~16 ~15 minecraft:air
fill ~40 ~15 ~2 ~40 ~21 ~2 minecraft:air
fill ~40 ~15 ~4 ~40 ~21 ~4 minecraft:air
fill ~40 ~15 ~22 ~40 ~21 ~22 minecraft:air
fill ~40 ~15 ~24 ~40 ~21 ~24 minecraft:air
fill ~42 ~15 ~0 ~42 ~21 ~0 minecraft:air
fill ~42 ~15 ~6 ~42 ~21 ~6 minecraft:air
fill ~42 ~15 ~20 ~42 ~21 ~20 minecraft:air
fill ~42 ~15 ~26 ~42 ~21 ~26 minecraft:air
fill ~44 ~15 ~0 ~44 ~21 ~0 minecraft:air
fill ~44 ~15 ~6 ~44 ~21 ~6 minecraft:air
fill ~44 ~15 ~20 ~44 ~21 ~20 minecraft:air
fill ~44 ~15 ~26 ~44 ~21 ~26 minecraft:air
fill ~46 ~15 ~2 ~46 ~21 ~2 minecraft:air
fill ~46 ~15 ~4 ~46 ~21 ~4 minecraft:air
fill ~46 ~15 ~22 ~46 ~21 ~22 minecraft:air
fill ~46 ~15 ~24 ~46 ~21 ~24 minecraft:air
fill ~0 ~17 ~0 ~0 ~21 ~0 minecraft:air
fill ~0 ~17 ~6 ~0 ~21 ~6 minecraft:air
fill ~0 ~17 ~20 ~0 ~21 ~20 minecraft:air
fill ~0 ~17 ~26 ~0 ~21 ~26 minecraft:air
fill ~6 ~17 ~0 ~6 ~21 ~0 minecraft:air
fill ~6 ~17 ~6 ~6 ~21 ~6 minecraft:air
fill ~6 ~17 ~20 ~6 ~21 ~20 minecraft:air
fill ~6 ~17 ~26 ~6 ~21 ~26 minecraft:air
fill ~40 ~17 ~0 ~40 ~21 ~0 minecraft:air
fill ~40 ~17 ~6 ~40 ~21 ~6 minecraft:air
fill ~40 ~17 ~20 ~40 ~21 ~20 minecraft:air
fill ~40 ~17 ~26 ~40 ~21 ~26 minecraft:air
fill ~46 ~17 ~0 ~46 ~21 ~0 minecraft:air
fill ~46 ~17 ~6 ~46 ~21 ~6 minecraft:air
fill ~46 ~17 ~20 ~46 ~21 ~20 minecraft:air
fill ~46 ~17 ~26 ~46 ~21 ~26 minecraft:air
fill ~32 ~18 ~11 ~33 ~21 ~15 minecraft:air
fill ~34 ~18 ~9 ~38 ~21 ~17 minecraft:air
fill ~39 ~18 ~11 ~40 ~21 ~12 minecraft:air
fill ~39 ~18 ~13 ~39 ~21 ~15 minecraft:air
fill ~40 ~18 ~14 ~40 ~21 ~15 minecraft:air
fill ~31 ~19 ~12 ~31 ~21 ~12 minecraft:air
fill ~31 ~19 ~14 ~31 ~21 ~14 minecraft:air
fill ~35 ~19 ~8 ~35 ~21 ~8 minecraft:air
fill ~35 ~19 ~18 ~35 ~21 ~18 minecraft:air
fill ~37 ~19 ~8 ~37 ~21 ~8 minecraft:air
fill ~37 ~19 ~18 ~37 ~21 ~18 minecraft:air
fill ~40 ~19 ~13 ~40 ~21 ~13 minecraft:air
fill ~41 ~19 ~12 ~41 ~21 ~12 minecraft:air
fill ~41 ~19 ~14 ~41 ~21 ~14 minecraft:air
fill ~31 ~20 ~11 ~31 ~21 ~11 minecraft:air
fill ~31 ~20 ~13 ~31 ~21 ~13 minecraft:air
fill ~31 ~20 ~15 ~31 ~21 ~15 minecraft:air
fill ~34 ~20 ~8 ~34 ~21 ~8 minecraft:air
fill ~34 ~20 ~18 ~34 ~21 ~18 minecraft:air
fill ~36 ~20 ~8 ~36 ~21 ~8 minecraft:air
fill ~36 ~20 ~18 ~36 ~21 ~18 minecraft:air
fill ~38 ~20 ~8 ~38 ~21 ~8 minecraft:air
fill ~38 ~20 ~18 ~38 ~21 ~18 minecraft:air
fill ~41 ~20 ~11 ~41 ~21 ~11 minecraft:air
fill ~41 ~20 ~13 ~41 ~21 ~13 minecraft:air
fill ~41 ~20 ~15 ~41 ~21 ~15 minecraft:air

# the ground at y 0: cobble footings under the walls and towers, flagstones in the gates, a timber deck over the river, flagged courtyards, the donjon plinth and its floor; the river cells of the walls are bars at y 0
fill ~0 ~0 ~0 ~1 ~0 ~0 minecraft:mossy_cobblestone
setblock ~0 ~0 ~1 minecraft:cobblestone
setblock ~0 ~0 ~2 minecraft:mossy_cobblestone
fill ~0 ~0 ~3 ~0 ~0 ~5 minecraft:cobblestone
fill ~0 ~0 ~6 ~0 ~0 ~7 minecraft:mossy_cobblestone
fill ~0 ~0 ~8 ~0 ~0 ~9 minecraft:cobblestone
fill ~0 ~0 ~10 ~0 ~0 ~11 minecraft:andesite
fill ~0 ~0 ~12 ~0 ~0 ~13 minecraft:cobblestone
setblock ~0 ~0 ~14 minecraft:andesite
fill ~0 ~0 ~15 ~0 ~0 ~16 minecraft:cobblestone
setblock ~0 ~0 ~17 minecraft:andesite
fill ~0 ~0 ~18 ~1 ~0 ~18 minecraft:cobblestone
fill ~0 ~0 ~19 ~0 ~0 ~20 minecraft:andesite
fill ~0 ~0 ~21 ~0 ~0 ~23 minecraft:cobblestone
setblock ~0 ~0 ~24 minecraft:mossy_cobblestone
setblock ~0 ~0 ~25 minecraft:andesite
setblock ~0 ~0 ~26 minecraft:cobblestone
setblock ~1 ~0 ~1 minecraft:andesite
fill ~1 ~0 ~2 ~2 ~0 ~4 minecraft:cobblestone
setblock ~1 ~0 ~5 minecraft:cobblestone
setblock ~1 ~0 ~6 minecraft:andesite
fill ~1 ~0 ~7 ~3 ~0 ~7 minecraft:cobblestone
setblock ~1 ~0 ~8 minecraft:mossy_cobblestone
fill ~1 ~0 ~9 ~1 ~0 ~10 minecraft:cobblestone
fill ~1 ~0 ~11 ~1 ~0 ~12 minecraft:mossy_cobblestone
fill ~1 ~0 ~13 ~1 ~0 ~15 minecraft:cobblestone
fill ~1 ~0 ~16 ~3 ~0 ~16 minecraft:mossy_cobblestone
fill ~1 ~0 ~17 ~3 ~0 ~17 minecraft:cobblestone
fill ~1 ~0 ~19 ~2 ~0 ~19 minecraft:cobblestone
fill ~1 ~0 ~20 ~2 ~0 ~20 minecraft:mossy_cobblestone
setblock ~1 ~0 ~21 minecraft:mossy_cobblestone
fill ~1 ~0 ~22 ~1 ~0 ~25 minecraft:cobblestone
setblock ~1 ~0 ~26 minecraft:andesite
setblock ~2 ~0 ~0 minecraft:andesite
setblock ~2 ~0 ~1 minecraft:mossy_cobblestone
setblock ~2 ~0 ~5 minecraft:andesite
fill ~2 ~0 ~6 ~3 ~0 ~6 minecraft:mossy_cobblestone
fill ~2 ~0 ~8 ~5 ~0 ~8 minecraft:cobblestone
setblock ~2 ~0 ~9 minecraft:andesite
setblock ~2 ~0 ~10 minecraft:mossy_cobblestone
fill ~2 ~0 ~11 ~2 ~0 ~15 minecraft:cobblestone
setblock ~2 ~0 ~18 minecraft:mossy_cobblestone
fill ~2 ~0 ~21 ~2 ~0 ~23 minecraft:cobblestone
fill ~2 ~0 ~24 ~5 ~0 ~24 minecraft:mossy_cobblestone
fill ~2 ~0 ~25 ~2 ~0 ~26 minecraft:mossy_cobblestone
fill ~3 ~0 ~0 ~3 ~0 ~5 minecraft:cobblestone
fill ~3 ~0 ~9 ~3 ~0 ~12 minecraft:stone_bricks
setblock ~3 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~3 ~0 ~14 minecraft:stone_bricks
fill ~3 ~0 ~15 ~4 ~0 ~15 minecraft:cobblestone
fill ~3 ~0 ~18 ~3 ~0 ~19 minecraft:cracked_stone_bricks
fill ~3 ~0 ~20 ~3 ~0 ~22 minecraft:cobblestone
fill ~3 ~0 ~23 ~6 ~0 ~23 minecraft:mossy_cobblestone
fill ~3 ~0 ~25 ~4 ~0 ~25 minecraft:cobblestone
setblock ~3 ~0 ~26 minecraft:cobblestone
fill ~4 ~0 ~0 ~5 ~0 ~0 minecraft:mossy_cobblestone
fill ~4 ~0 ~1 ~4 ~0 ~2 minecraft:cobblestone
setblock ~4 ~0 ~3 minecraft:mossy_cobblestone
fill ~4 ~0 ~4 ~4 ~0 ~6 minecraft:cobblestone
setblock ~4 ~0 ~7 minecraft:mossy_cobblestone
setblock ~4 ~0 ~9 minecraft:cobblestone
setblock ~4 ~0 ~10 minecraft:stone_bricks
setblock ~4 ~0 ~11 minecraft:cobblestone
fill ~4 ~0 ~12 ~4 ~0 ~13 minecraft:stone_bricks
setblock ~4 ~0 ~14 minecraft:cracked_stone_bricks
setblock ~4 ~0 ~16 minecraft:andesite
setblock ~4 ~0 ~17 minecraft:mossy_cobblestone
fill ~4 ~0 ~18 ~5 ~0 ~18 minecraft:stone_bricks
setblock ~4 ~0 ~19 minecraft:cobblestone
setblock ~4 ~0 ~20 minecraft:mossy_cobblestone
fill ~4 ~0 ~21 ~4 ~0 ~22 minecraft:cobblestone
setblock ~4 ~0 ~26 minecraft:andesite
fill ~5 ~0 ~1 ~6 ~0 ~1 minecraft:mossy_cobblestone
fill ~5 ~0 ~2 ~5 ~0 ~4 minecraft:cobblestone
setblock ~5 ~0 ~5 minecraft:andesite
setblock ~5 ~0 ~6 minecraft:mossy_cobblestone
fill ~5 ~0 ~7 ~6 ~0 ~7 minecraft:cobblestone
fill ~5 ~0 ~9 ~5 ~0 ~11 minecraft:stone_bricks
setblock ~5 ~0 ~12 minecraft:cobblestone
fill ~5 ~0 ~13 ~8 ~0 ~13 minecraft:stone_bricks
setblock ~5 ~0 ~14 minecraft:stone_bricks
fill ~5 ~0 ~15 ~5 ~0 ~17 minecraft:andesite
setblock ~5 ~0 ~19 minecraft:stone_bricks
fill ~5 ~0 ~20 ~5 ~0 ~22 minecraft:cobblestone
fill ~5 ~0 ~25 ~6 ~0 ~25 minecraft:mossy_cobblestone
setblock ~5 ~0 ~26 minecraft:cobblestone
setblock ~6 ~0 ~0 minecraft:cobblestone
setblock ~6 ~0 ~2 minecraft:mossy_cobblestone
fill ~6 ~0 ~3 ~8 ~0 ~3 minecraft:cobblestone
fill ~6 ~0 ~4 ~7 ~0 ~6 minecraft:cobblestone
setblock ~6 ~0 ~8 minecraft:mossy_cobblestone
setblock ~6 ~0 ~9 minecraft:cobblestone
setblock ~6 ~0 ~10 minecraft:stone_bricks
fill ~6 ~0 ~11 ~6 ~0 ~12 minecraft:cracked_stone_bricks
setblock ~6 ~0 ~14 minecraft:cracked_stone_bricks
fill ~6 ~0 ~15 ~7 ~0 ~16 minecraft:stone_bricks
fill ~6 ~0 ~17 ~7 ~0 ~17 minecraft:mossy_stone_bricks
setblock ~6 ~0 ~18 minecraft:cobblestone
fill ~6 ~0 ~19 ~7 ~0 ~19 minecraft:mossy_cobblestone
setblock ~6 ~0 ~20 minecraft:andesite
fill ~6 ~0 ~21 ~6 ~0 ~22 minecraft:cobblestone
setblock ~6 ~0 ~24 minecraft:cobblestone
setblock ~6 ~0 ~26 minecraft:mossy_cobblestone
fill ~7 ~0 ~0 ~7 ~0 ~1 minecraft:mossy_stone_bricks
fill ~7 ~0 ~2 ~8 ~0 ~2 minecraft:stone_bricks
setblock ~7 ~0 ~7 minecraft:mossy_cobblestone
setblock ~7 ~0 ~8 minecraft:cobblestone
fill ~7 ~0 ~9 ~7 ~0 ~11 minecraft:stone_bricks
setblock ~7 ~0 ~12 minecraft:cracked_stone_bricks
setblock ~7 ~0 ~14 minecraft:cobblestone
setblock ~7 ~0 ~18 minecraft:andesite
fill ~7 ~0 ~20 ~7 ~0 ~23 minecraft:cobblestone
fill ~7 ~0 ~24 ~11 ~0 ~25 minecraft:stone_bricks
fill ~7 ~0 ~26 ~8 ~0 ~26 minecraft:mossy_stone_bricks
fill ~8 ~0 ~0 ~8 ~0 ~1 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~4 minecraft:mossy_cobblestone
fill ~8 ~0 ~5 ~9 ~0 ~5 minecraft:cobblestone
fill ~8 ~0 ~6 ~8 ~0 ~8 minecraft:cobblestone
setblock ~8 ~0 ~9 minecraft:cracked_stone_bricks
setblock ~8 ~0 ~10 minecraft:stone_bricks
fill ~8 ~0 ~11 ~8 ~0 ~12 minecraft:cracked_stone_bricks
fill ~8 ~0 ~14 ~8 ~0 ~15 minecraft:mossy_stone_bricks
fill ~8 ~0 ~16 ~8 ~0 ~17 minecraft:stone_bricks
fill ~8 ~0 ~18 ~8 ~0 ~22 minecraft:cobblestone
setblock ~8 ~0 ~23 minecraft:andesite
fill ~9 ~0 ~0 ~17 ~0 ~0 minecraft:cobblestone
setblock ~9 ~0 ~1 minecraft:stone_bricks
setblock ~9 ~0 ~2 minecraft:cracked_stone_bricks
fill ~9 ~0 ~3 ~9 ~0 ~4 minecraft:stone_bricks
fill ~9 ~0 ~6 ~9 ~0 ~7 minecraft:stone_bricks
setblock ~9 ~0 ~8 minecraft:mossy_stone_bricks
setblock ~9 ~0 ~9 minecraft:andesite
setblock ~9 ~0 ~10 minecraft:mossy_cobblestone
fill ~9 ~0 ~11 ~10 ~0 ~11 minecraft:cobblestone
fill ~9 ~0 ~12 ~14 ~0 ~12 minecraft:stone_bricks
setblock ~9 ~0 ~13 minecraft:cracked_stone_bricks
fill ~9 ~0 ~14 ~9 ~0 ~16 minecraft:stone_bricks
setblock ~9 ~0 ~17 minecraft:mossy_stone_bricks
fill ~9 ~0 ~18 ~9 ~0 ~19 minecraft:stone_bricks
setblock ~9 ~0 ~20 minecraft:mossy_stone_bricks
fill ~9 ~0 ~21 ~10 ~0 ~21 minecraft:cobblestone
fill ~9 ~0 ~22 ~10 ~0 ~22 minecraft:mossy_cobblestone
fill ~9 ~0 ~23 ~10 ~0 ~23 minecraft:cobblestone
fill ~9 ~0 ~26 ~11 ~0 ~26 minecraft:stone_bricks
setblock ~10 ~0 ~1 minecraft:mossy_stone_bricks
fill ~10 ~0 ~2 ~11 ~0 ~3 minecraft:stone_bricks
setblock ~10 ~0 ~4 minecraft:cracked_stone_bricks
fill ~10 ~0 ~5 ~10 ~0 ~8 minecraft:stone_bricks
fill ~10 ~0 ~9 ~11 ~0 ~9 minecraft:cobblestone
fill ~10 ~0 ~10 ~11 ~0 ~10 minecraft:andesite
fill ~10 ~0 ~13 ~11 ~0 ~14 minecraft:stone_bricks
fill ~10 ~0 ~15 ~10 ~0 ~18 minecraft:stone_bricks
setblock ~10 ~0 ~19 minecraft:cobblestone
fill ~10 ~0 ~20 ~11 ~0 ~20 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~1 minecraft:cracked_stone_bricks
fill ~11 ~0 ~4 ~11 ~0 ~6 minecraft:stone_bricks
setblock ~11 ~0 ~7 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~8 minecraft:stone_bricks
setblock ~11 ~0 ~11 minecraft:andesite
setblock ~11 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~11 ~0 ~16 minecraft:cobblestone
fill ~11 ~0 ~17 ~11 ~0 ~19 minecraft:stone_bricks
setblock ~11 ~0 ~21 minecraft:mossy_cobblestone
fill ~11 ~0 ~22 ~11 ~0 ~23 minecraft:cobblestone
fill ~12 ~0 ~1 ~13 ~0 ~1 minecraft:cobblestone
setblock ~12 ~0 ~2 minecraft:andesite
setblock ~12 ~0 ~3 minecraft:mossy_cobblestone
setblock ~12 ~0 ~4 minecraft:cobblestone
setblock ~12 ~0 ~5 minecraft:andesite
setblock ~12 ~0 ~6 minecraft:mossy_cobblestone
fill ~12 ~0 ~7 ~12 ~0 ~8 minecraft:cobblestone
fill ~12 ~0 ~9 ~13 ~0 ~9 minecraft:stone_bricks
setblock ~12 ~0 ~10 minecraft:mossy_stone_bricks
setblock ~12 ~0 ~11 minecraft:cobblestone
fill ~12 ~0 ~13 ~14 ~0 ~13 minecraft:cracked_stone_bricks
fill ~12 ~0 ~14 ~14 ~0 ~14 minecraft:stone_bricks
fill ~12 ~0 ~15 ~12 ~0 ~17 minecraft:stone_bricks
setblock ~12 ~0 ~18 minecraft:mossy_stone_bricks
fill ~12 ~0 ~19 ~12 ~0 ~23 minecraft:stone_bricks
fill ~12 ~0 ~24 ~12 ~0 ~26 minecraft:cobblestone
fill ~13 ~0 ~2 ~13 ~0 ~4 minecraft:cobblestone
setblock ~13 ~0 ~5 minecraft:mossy_cobblestone
fill ~13 ~0 ~6 ~13 ~0 ~8 minecraft:cobblestone
fill ~13 ~0 ~10 ~14 ~0 ~10 minecraft:stone_bricks
setblock ~13 ~0 ~11 minecraft:cracked_stone_bricks
fill ~13 ~0 ~15 ~13 ~0 ~16 minecraft:cobblestone
fill ~13 ~0 ~17 ~13 ~0 ~19 minecraft:stone_bricks
setblock ~13 ~0 ~20 minecraft:mossy_stone_bricks
fill ~13 ~0 ~21 ~13 ~0 ~23 minecraft:stone_bricks
fill ~13 ~0 ~24 ~14 ~0 ~24 minecraft:andesite
fill ~13 ~0 ~25 ~13 ~0 ~26 minecraft:cobblestone
fill ~14 ~0 ~1 ~14 ~0 ~3 minecraft:mossy_cobblestone
fill ~14 ~0 ~4 ~14 ~0 ~7 minecraft:cobblestone
setblock ~14 ~0 ~8 minecraft:mossy_cobblestone
setblock ~14 ~0 ~9 minecraft:cracked_stone_bricks
fill ~14 ~0 ~11 ~16 ~0 ~11 minecraft:stone_bricks
fill ~14 ~0 ~15 ~14 ~0 ~22 minecraft:stone_bricks
setblock ~14 ~0 ~23 minecraft:cobblestone
fill ~14 ~0 ~25 ~15 ~0 ~26 minecraft:mossy_cobblestone
fill ~15 ~0 ~1 ~16 ~0 ~1 minecraft:cobblestone
setblock ~15 ~0 ~2 minecraft:mossy_cobblestone
fill ~15 ~0 ~3 ~17 ~0 ~3 minecraft:stone_bricks
fill ~15 ~0 ~4 ~16 ~0 ~4 minecraft:cracked_stone_bricks
fill ~15 ~0 ~5 ~16 ~0 ~8 minecraft:stone_bricks
setblock ~15 ~0 ~9 minecraft:stone_bricks
setblock ~15 ~0 ~10 minecraft:cracked_stone_bricks
fill ~15 ~0 ~12 ~17 ~0 ~13 minecraft:cobblestone
fill ~15 ~0 ~14 ~15 ~0 ~15 minecraft:mossy_cobblestone
fill ~15 ~0 ~16 ~16 ~0 ~19 minecraft:cobblestone
fill ~15 ~0 ~20 ~16 ~0 ~20 minecraft:mossy_cobblestone
fill ~15 ~0 ~21 ~16 ~0 ~21 minecraft:stone_bricks
fill ~15 ~0 ~22 ~16 ~0 ~22 minecraft:cracked_stone_bricks
setblock ~15 ~0 ~23 minecraft:stone_bricks
fill ~15 ~0 ~24 ~16 ~0 ~24 minecraft:mossy_cobblestone
setblock ~16 ~0 ~2 minecraft:cobblestone
setblock ~16 ~0 ~9 minecraft:cobblestone
setblock ~16 ~0 ~10 minecraft:mossy_stone_bricks
setblock ~16 ~0 ~14 minecraft:andesite
fill ~16 ~0 ~15 ~17 ~0 ~15 minecraft:mossy_cobblestone
setblock ~16 ~0 ~23 minecraft:cracked_stone_bricks
fill ~16 ~0 ~25 ~16 ~0 ~26 minecraft:cobblestone
setblock ~17 ~0 ~1 minecraft:andesite
setblock ~17 ~0 ~2 minecraft:mossy_cobblestone
fill ~17 ~0 ~4 ~17 ~0 ~5 minecraft:stone_bricks
setblock ~17 ~0 ~6 minecraft:mossy_stone_bricks
fill ~17 ~0 ~7 ~17 ~0 ~11 minecraft:stone_bricks
setblock ~17 ~0 ~14 minecraft:cobblestone
setblock ~17 ~0 ~16 minecraft:mossy_cobblestone
fill ~17 ~0 ~17 ~17 ~0 ~18 minecraft:cobblestone
setblock ~17 ~0 ~19 minecraft:mossy_cobblestone
setblock ~17 ~0 ~20 minecraft:andesite
fill ~17 ~0 ~21 ~17 ~0 ~22 minecraft:mossy_stone_bricks
fill ~17 ~0 ~23 ~17 ~0 ~24 minecraft:cobblestone
setblock ~17 ~0 ~25 minecraft:mossy_cobblestone
setblock ~17 ~0 ~26 minecraft:cobblestone
setblock ~18 ~0 ~3 minecraft:dark_oak_planks
fill ~18 ~0 ~4 ~28 ~0 ~4 minecraft:stripped_dark_oak_log[axis=x]
fill ~18 ~0 ~5 ~18 ~0 ~7 minecraft:spruce_planks
setblock ~18 ~0 ~8 minecraft:dark_oak_planks
fill ~18 ~0 ~9 ~28 ~0 ~9 minecraft:stripped_dark_oak_log[axis=x]
fill ~18 ~0 ~10 ~20 ~0 ~10 minecraft:dark_oak_planks
fill ~18 ~0 ~11 ~19 ~0 ~11 minecraft:spruce_planks
setblock ~18 ~0 ~12 minecraft:spruce_planks
setblock ~18 ~0 ~13 minecraft:dark_oak_planks
fill ~18 ~0 ~14 ~28 ~0 ~14 minecraft:stripped_dark_oak_log[axis=x]
fill ~18 ~0 ~15 ~19 ~0 ~15 minecraft:spruce_planks
setblock ~18 ~0 ~16 minecraft:spruce_planks
setblock ~18 ~0 ~17 minecraft:oak_planks
setblock ~18 ~0 ~18 minecraft:dark_oak_planks
fill ~18 ~0 ~19 ~28 ~0 ~19 minecraft:stripped_dark_oak_log[axis=x]
setblock ~18 ~0 ~20 minecraft:dark_oak_planks
fill ~18 ~0 ~21 ~19 ~0 ~21 minecraft:spruce_planks
fill ~18 ~0 ~22 ~28 ~0 ~22 minecraft:stripped_dark_oak_log[axis=x]
fill ~18 ~0 ~23 ~21 ~0 ~23 minecraft:dark_oak_planks
setblock ~19 ~0 ~3 minecraft:spruce_planks
fill ~19 ~0 ~5 ~19 ~0 ~8 minecraft:dark_oak_planks
setblock ~19 ~0 ~12 minecraft:dark_oak_planks
setblock ~19 ~0 ~13 minecraft:spruce_planks
fill ~19 ~0 ~16 ~19 ~0 ~17 minecraft:dark_oak_planks
setblock ~19 ~0 ~18 minecraft:spruce_planks
fill ~19 ~0 ~20 ~20 ~0 ~20 minecraft:spruce_planks
setblock ~20 ~0 ~3 minecraft:oak_planks
setblock ~20 ~0 ~5 minecraft:oak_planks
fill ~20 ~0 ~6 ~20 ~0 ~8 minecraft:dark_oak_planks
fill ~20 ~0 ~11 ~27 ~0 ~11 minecraft:dark_oak_planks
fill ~20 ~0 ~12 ~20 ~0 ~13 minecraft:spruce_planks
setblock ~20 ~0 ~15 minecraft:oak_planks
fill ~20 ~0 ~16 ~20 ~0 ~17 minecraft:spruce_planks
setblock ~20 ~0 ~18 minecraft:dark_oak_planks
setblock ~20 ~0 ~21 minecraft:dark_oak_planks
setblock ~21 ~0 ~3 minecraft:spruce_planks
fill ~21 ~0 ~5 ~27 ~0 ~5 minecraft:dark_oak_planks
fill ~21 ~0 ~6 ~23 ~0 ~6 minecraft:spruce_planks
fill ~21 ~0 ~7 ~23 ~0 ~7 minecraft:dark_oak_planks
fill ~21 ~0 ~8 ~22 ~0 ~8 minecraft:spruce_planks
setblock ~21 ~0 ~10 minecraft:spruce_planks
fill ~21 ~0 ~12 ~22 ~0 ~13 minecraft:dark_oak_planks
fill ~21 ~0 ~15 ~22 ~0 ~16 minecraft:dark_oak_planks
setblock ~21 ~0 ~17 minecraft:spruce_planks
setblock ~21 ~0 ~18 minecraft:oak_planks
setblock ~21 ~0 ~20 minecraft:dark_oak_planks
setblock ~21 ~0 ~21 minecraft:oak_planks
fill ~22 ~0 ~3 ~23 ~0 ~3 minecraft:dark_oak_planks
fill ~22 ~0 ~10 ~28 ~0 ~10 minecraft:dark_oak_planks
setblock ~22 ~0 ~17 minecraft:oak_planks
setblock ~22 ~0 ~18 minecraft:dark_oak_planks
setblock ~22 ~0 ~20 minecraft:oak_planks
setblock ~22 ~0 ~21 minecraft:dark_oak_planks
setblock ~22 ~0 ~23 minecraft:oak_planks
setblock ~23 ~0 ~8 minecraft:dark_oak_planks
setblock ~23 ~0 ~12 minecraft:spruce_planks
fill ~23 ~0 ~13 ~25 ~0 ~13 minecraft:dark_oak_planks
fill ~23 ~0 ~15 ~24 ~0 ~15 minecraft:oak_planks
fill ~23 ~0 ~16 ~24 ~0 ~16 minecraft:dark_oak_planks
fill ~23 ~0 ~17 ~23 ~0 ~18 minecraft:dark_oak_planks
fill ~23 ~0 ~20 ~23 ~0 ~21 minecraft:dark_oak_planks
fill ~23 ~0 ~23 ~28 ~0 ~23 minecraft:dark_oak_planks
setblock ~24 ~0 ~3 minecraft:spruce_planks
fill ~24 ~0 ~6 ~25 ~0 ~8 minecraft:dark_oak_planks
setblock ~24 ~0 ~12 minecraft:oak_planks
setblock ~24 ~0 ~17 minecraft:oak_planks
fill ~24 ~0 ~18 ~25 ~0 ~18 minecraft:dark_oak_planks
setblock ~24 ~0 ~20 minecraft:oak_planks
fill ~24 ~0 ~21 ~25 ~0 ~21 minecraft:dark_oak_planks
setblock ~25 ~0 ~3 minecraft:dark_oak_planks
fill ~25 ~0 ~12 ~26 ~0 ~12 minecraft:spruce_planks
fill ~25 ~0 ~15 ~25 ~0 ~16 minecraft:dark_oak_planks
setblock ~25 ~0 ~17 minecraft:spruce_planks
fill ~25 ~0 ~20 ~28 ~0 ~20 minecraft:dark_oak_planks
setblock ~26 ~0 ~3 minecraft:spruce_planks
fill ~26 ~0 ~6 ~27 ~0 ~6 minecraft:spruce_planks
fill ~26 ~0 ~7 ~27 ~0 ~8 minecraft:dark_oak_planks
fill ~26 ~0 ~13 ~27 ~0 ~13 minecraft:spruce_planks
setblock ~26 ~0 ~15 minecraft:spruce_planks
fill ~26 ~0 ~16 ~27 ~0 ~16 minecraft:dark_oak_planks
setblock ~26 ~0 ~17 minecraft:dark_oak_planks
fill ~26 ~0 ~18 ~27 ~0 ~18 minecraft:oak_planks
setblock ~26 ~0 ~21 minecraft:oak_planks
fill ~27 ~0 ~3 ~28 ~0 ~3 minecraft:dark_oak_planks
setblock ~27 ~0 ~12 minecraft:dark_oak_planks
fill ~27 ~0 ~15 ~28 ~0 ~15 minecraft:dark_oak_planks
fill ~27 ~0 ~17 ~28 ~0 ~17 minecraft:oak_planks
fill ~27 ~0 ~21 ~28 ~0 ~21 minecraft:dark_oak_planks
fill ~28 ~0 ~5 ~28 ~0 ~7 minecraft:spruce_planks
setblock ~28 ~0 ~8 minecraft:dark_oak_planks
setblock ~28 ~0 ~11 minecraft:oak_planks
setblock ~28 ~0 ~12 minecraft:spruce_planks
setblock ~28 ~0 ~13 minecraft:dark_oak_planks
setblock ~28 ~0 ~16 minecraft:spruce_planks
setblock ~28 ~0 ~18 minecraft:dark_oak_planks
setblock ~29 ~0 ~0 minecraft:mossy_cobblestone
fill ~29 ~0 ~1 ~30 ~0 ~1 minecraft:cobblestone
setblock ~29 ~0 ~2 minecraft:cobblestone
setblock ~29 ~0 ~3 minecraft:mossy_stone_bricks
fill ~29 ~0 ~4 ~29 ~0 ~5 minecraft:stone_bricks
fill ~29 ~0 ~6 ~29 ~0 ~8 minecraft:cobblestone
fill ~29 ~0 ~9 ~29 ~0 ~10 minecraft:stone_bricks
fill ~29 ~0 ~11 ~30 ~0 ~11 minecraft:cracked_stone_bricks
setblock ~29 ~0 ~12 minecraft:cobblestone
setblock ~29 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~29 ~0 ~14 minecraft:stone_bricks
setblock ~29 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~29 ~0 ~16 minecraft:stone_bricks
setblock ~29 ~0 ~17 minecraft:cracked_stone_bricks
setblock ~29 ~0 ~18 minecraft:andesite
setblock ~29 ~0 ~19 minecraft:mossy_cobblestone
setblock ~29 ~0 ~20 minecraft:cobblestone
fill ~29 ~0 ~21 ~29 ~0 ~22 minecraft:stone_bricks
setblock ~29 ~0 ~23 minecraft:cracked_stone_bricks
setblock ~29 ~0 ~24 minecraft:mossy_cobblestone
setblock ~29 ~0 ~25 minecraft:andesite
setblock ~29 ~0 ~26 minecraft:cobblestone
fill ~30 ~0 ~0 ~33 ~0 ~0 minecraft:cobblestone
setblock ~30 ~0 ~2 minecraft:mossy_cobblestone
fill ~30 ~0 ~3 ~32 ~0 ~5 minecraft:stone_bricks
setblock ~30 ~0 ~6 minecraft:mossy_stone_bricks
setblock ~30 ~0 ~7 minecraft:stone_bricks
setblock ~30 ~0 ~8 minecraft:cracked_stone_bricks
fill ~30 ~0 ~9 ~31 ~0 ~9 minecraft:cobblestone
fill ~30 ~0 ~10 ~31 ~0 ~10 minecraft:mossy_cobblestone
fill ~30 ~0 ~12 ~30 ~0 ~15 minecraft:stone_bricks
setblock ~30 ~0 ~16 minecraft:cracked_stone_bricks
fill ~30 ~0 ~17 ~30 ~0 ~18 minecraft:stone_bricks
fill ~30 ~0 ~19 ~30 ~0 ~20 minecraft:cracked_stone_bricks
fill ~30 ~0 ~21 ~31 ~0 ~21 minecraft:cobblestone
fill ~30 ~0 ~22 ~30 ~0 ~23 minecraft:andesite
setblock ~30 ~0 ~24 minecraft:cobblestone
setblock ~30 ~0 ~25 minecraft:mossy_cobblestone
setblock ~30 ~0 ~26 minecraft:andesite
setblock ~31 ~0 ~1 minecraft:mossy_cobblestone
setblock ~31 ~0 ~2 minecraft:cobblestone
fill ~31 ~0 ~6 ~32 ~0 ~6 minecraft:stone_bricks
setblock ~31 ~0 ~7 minecraft:cracked_stone_bricks
fill ~31 ~0 ~8 ~33 ~0 ~8 minecraft:cobblestone
setblock ~31 ~0 ~11 minecraft:cobblestone
fill ~31 ~0 ~12 ~31 ~0 ~13 minecraft:mossy_cobblestone
fill ~31 ~0 ~14 ~31 ~0 ~16 minecraft:cobblestone
setblock ~31 ~0 ~17 minecraft:andesite
setblock ~31 ~0 ~18 minecraft:cobblestone
fill ~31 ~0 ~19 ~32 ~0 ~19 minecraft:stone_bricks
setblock ~31 ~0 ~20 minecraft:stone_bricks
fill ~31 ~0 ~22 ~31 ~0 ~23 minecraft:cobblestone
fill ~31 ~0 ~24 ~31 ~0 ~26 minecraft:mossy_cobblestone
fill ~32 ~0 ~1 ~33 ~0 ~1 minecraft:cobblestone
setblock ~32 ~0 ~2 minecraft:andesite
setblock ~32 ~0 ~7 minecraft:stone_bricks
fill ~32 ~0 ~9 ~33 ~0 ~10 minecraft:stone_bricks
setblock ~32 ~0 ~11 minecraft:mossy_stone_bricks
fill ~32 ~0 ~12 ~32 ~0 ~14 minecraft:stone_bricks
fill ~32 ~0 ~15 ~32 ~0 ~16 minecraft:cracked_stone_bricks
fill ~32 ~0 ~17 ~32 ~0 ~18 minecraft:cobblestone
setblock ~32 ~0 ~20 minecraft:cracked_stone_bricks
fill ~32 ~0 ~21 ~32 ~0 ~22 minecraft:mossy_cobblestone
fill ~32 ~0 ~23 ~32 ~0 ~24 minecraft:cobblestone
fill ~32 ~0 ~25 ~33 ~0 ~25 minecraft:mossy_cobblestone
setblock ~32 ~0 ~26 minecraft:mossy_cobblestone
fill ~33 ~0 ~2 ~34 ~0 ~2 minecraft:cobblestone
setblock ~33 ~0 ~3 minecraft:mossy_stone_bricks
fill ~33 ~0 ~4 ~33 ~0 ~5 minecraft:stone_bricks
fill ~33 ~0 ~6 ~34 ~0 ~6 minecraft:cobblestone
fill ~33 ~0 ~7 ~35 ~0 ~7 minecraft:mossy_cobblestone
fill ~33 ~0 ~11 ~33 ~0 ~12 minecraft:stone_bricks
fill ~33 ~0 ~13 ~33 ~0 ~14 minecraft:cracked_stone_bricks
fill ~33 ~0 ~15 ~33 ~0 ~17 minecraft:stone_bricks
setblock ~33 ~0 ~18 minecraft:mossy_cobblestone
fill ~33 ~0 ~19 ~33 ~0 ~20 minecraft:cobblestone
setblock ~33 ~0 ~21 minecraft:cracked_stone_bricks
fill ~33 ~0 ~22 ~33 ~0 ~23 minecraft:stone_bricks
setblock ~33 ~0 ~24 minecraft:cobblestone
setblock ~33 ~0 ~26 minecraft:cobblestone
fill ~34 ~0 ~0 ~34 ~0 ~1 minecraft:mossy_cobblestone
fill ~34 ~0 ~3 ~35 ~0 ~3 minecraft:stone_bricks
fill ~34 ~0 ~4 ~34 ~0 ~5 minecraft:mossy_stone_bricks
setblock ~34 ~0 ~8 minecraft:mossy_cobblestone
setblock ~34 ~0 ~9 minecraft:cobblestone
setblock ~34 ~0 ~10 minecraft:stone_bricks
fill ~34 ~0 ~11 ~35 ~0 ~11 minecraft:cracked_stone_bricks
setblock ~34 ~0 ~12 minecraft:cobblestone
fill ~34 ~0 ~13 ~35 ~0 ~13 minecraft:stone_bricks
setblock ~34 ~0 ~14 minecraft:cobblestone
setblock ~34 ~0 ~15 minecraft:andesite
setblock ~34 ~0 ~16 minecraft:cracked_stone_bricks
fill ~34 ~0 ~17 ~34 ~0 ~20 minecraft:cobblestone
fill ~34 ~0 ~21 ~37 ~0 ~21 minecraft:stone_bricks
fill ~34 ~0 ~22 ~35 ~0 ~22 minecraft:cobblestone
fill ~34 ~0 ~23 ~35 ~0 ~23 minecraft:stone_bricks
fill ~34 ~0 ~24 ~34 ~0 ~26 minecraft:mossy_cobblestone
fill ~35 ~0 ~0 ~38 ~0 ~0 minecraft:stone_bricks
fill ~35 ~0 ~1 ~35 ~0 ~2 minecraft:cracked_stone_bricks
setblock ~35 ~0 ~4 minecraft:cracked_stone_bricks
fill ~35 ~0 ~5 ~35 ~0 ~6 minecraft:cobblestone
setblock ~35 ~0 ~8 minecraft:cobblestone
setblock ~35 ~0 ~9 minecraft:mossy_stone_bricks
fill ~35 ~0 ~10 ~36 ~0 ~10 minecraft:cracked_stone_bricks
setblock ~35 ~0 ~12 minecraft:mossy_stone_bricks
fill ~35 ~0 ~14 ~35 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~35 ~0 ~16 minecraft:cobblestone
setblock ~35 ~0 ~17 minecraft:stone_bricks
fill ~35 ~0 ~18 ~36 ~0 ~18 minecraft:cobblestone
fill ~35 ~0 ~19 ~35 ~0 ~20 minecraft:cobblestone
fill ~35 ~0 ~24 ~35 ~0 ~25 minecraft:stone_bricks
fill ~35 ~0 ~26 ~36 ~0 ~26 minecraft:cracked_stone_bricks
fill ~36 ~0 ~1 ~36 ~0 ~2 minecraft:stone_bricks
fill ~36 ~0 ~3 ~37 ~0 ~3 minecraft:cobblestone
fill ~36 ~0 ~4 ~37 ~0 ~4 minecraft:mossy_cobblestone
fill ~36 ~0 ~5 ~37 ~0 ~5 minecraft:andesite
fill ~36 ~0 ~6 ~37 ~0 ~7 minecraft:cobblestone
setblock ~36 ~0 ~8 minecraft:andesite
setblock ~36 ~0 ~9 minecraft:cobblestone
fill ~36 ~0 ~11 ~36 ~0 ~15 minecraft:stone_bricks
setblock ~36 ~0 ~16 minecraft:andesite
setblock ~36 ~0 ~17 minecraft:cracked_stone_bricks
fill ~36 ~0 ~19 ~37 ~0 ~19 minecraft:stone_bricks
fill ~36 ~0 ~20 ~37 ~0 ~20 minecraft:cracked_stone_bricks
fill ~36 ~0 ~22 ~37 ~0 ~24 minecraft:stone_bricks
setblock ~36 ~0 ~25 minecraft:mossy_stone_bricks
fill ~37 ~0 ~1 ~38 ~0 ~1 minecraft:mossy_stone_bricks
fill ~37 ~0 ~2 ~38 ~0 ~2 minecraft:stone_bricks
fill ~37 ~0 ~8 ~38 ~0 ~8 minecraft:mossy_cobblestone
setblock ~37 ~0 ~9 minecraft:cracked_stone_bricks
setblock ~37 ~0 ~10 minecraft:andesite
setblock ~37 ~0 ~11 minecraft:mossy_stone_bricks
setblock ~37 ~0 ~12 minecraft:cobblestone
fill ~37 ~0 ~13 ~37 ~0 ~14 minecraft:stone_bricks
setblock ~37 ~0 ~15 minecraft:mossy_stone_bricks
fill ~37 ~0 ~16 ~37 ~0 ~17 minecraft:stone_bricks
setblock ~37 ~0 ~18 minecraft:mossy_cobblestone
fill ~37 ~0 ~25 ~38 ~0 ~26 minecraft:stone_bricks
setblock ~38 ~0 ~3 minecraft:andesite
setblock ~38 ~0 ~4 minecraft:cobblestone
fill ~38 ~0 ~5 ~38 ~0 ~6 minecraft:mossy_cobblestone
fill ~38 ~0 ~7 ~39 ~0 ~7 minecraft:cobblestone
setblock ~38 ~0 ~9 minecraft:andesite
setblock ~38 ~0 ~10 minecraft:cracked_stone_bricks
fill ~38 ~0 ~11 ~38 ~0 ~12 minecraft:stone_bricks
fill ~38 ~0 ~13 ~39 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~38 ~0 ~14 minecraft:andesite
fill ~38 ~0 ~15 ~38 ~0 ~16 minecraft:stone_bricks
fill ~38 ~0 ~17 ~39 ~0 ~17 minecraft:cracked_stone_bricks
fill ~38 ~0 ~18 ~39 ~0 ~18 minecraft:cobblestone
setblock ~38 ~0 ~19 minecraft:cracked_stone_bricks
fill ~38 ~0 ~20 ~38 ~0 ~22 minecraft:stone_bricks
fill ~38 ~0 ~23 ~38 ~0 ~24 minecraft:cobblestone
setblock ~39 ~0 ~0 minecraft:mossy_stone_bricks
setblock ~39 ~0 ~1 minecraft:stone_bricks
setblock ~39 ~0 ~2 minecraft:cracked_stone_bricks
fill ~39 ~0 ~3 ~39 ~0 ~6 minecraft:mossy_cobblestone
fill ~39 ~0 ~8 ~40 ~0 ~8 minecraft:cobblestone
fill ~39 ~0 ~9 ~40 ~0 ~9 minecraft:stone_bricks
setblock ~39 ~0 ~10 minecraft:stone_bricks
setblock ~39 ~0 ~11 minecraft:andesite
fill ~39 ~0 ~12 ~40 ~0 ~12 minecraft:stone_bricks
setblock ~39 ~0 ~14 minecraft:mossy_stone_bricks
setblock ~39 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~39 ~0 ~16 minecraft:cobblestone
fill ~39 ~0 ~19 ~40 ~0 ~19 minecraft:stone_bricks
setblock ~39 ~0 ~20 minecraft:mossy_stone_bricks
fill ~39 ~0 ~21 ~39 ~0 ~22 minecraft:stone_bricks
setblock ~39 ~0 ~23 minecraft:mossy_stone_bricks
fill ~39 ~0 ~24 ~39 ~0 ~25 minecraft:stone_bricks
setblock ~39 ~0 ~26 minecraft:cracked_stone_bricks
setblock ~40 ~0 ~0 minecraft:andesite
fill ~40 ~0 ~1 ~40 ~0 ~2 minecraft:mossy_cobblestone
fill ~40 ~0 ~3 ~41 ~0 ~3 minecraft:cobblestone
fill ~40 ~0 ~4 ~40 ~0 ~6 minecraft:mossy_cobblestone
setblock ~40 ~0 ~7 minecraft:andesite
fill ~40 ~0 ~10 ~41 ~0 ~11 minecraft:cobblestone
fill ~40 ~0 ~13 ~40 ~0 ~14 minecraft:stone_bricks
setblock ~40 ~0 ~15 minecraft:cobblestone
fill ~40 ~0 ~16 ~40 ~0 ~17 minecraft:stone_bricks
setblock ~40 ~0 ~18 minecraft:mossy_cobblestone
fill ~40 ~0 ~20 ~41 ~0 ~22 minecraft:cobblestone
setblock ~40 ~0 ~23 minecraft:mossy_cobblestone
fill ~40 ~0 ~24 ~40 ~0 ~26 minecraft:cobblestone
fill ~41 ~0 ~0 ~41 ~0 ~2 minecraft:mossy_cobblestone
fill ~41 ~0 ~4 ~43 ~0 ~4 minecraft:cobblestone
fill ~41 ~0 ~5 ~42 ~0 ~6 minecraft:cobblestone
fill ~41 ~0 ~7 ~41 ~0 ~8 minecraft:cobblestone
setblock ~41 ~0 ~9 minecraft:mossy_cobblestone
fill ~41 ~0 ~12 ~41 ~0 ~15 minecraft:cobblestone
fill ~41 ~0 ~16 ~41 ~0 ~17 minecraft:mossy_cobblestone
setblock ~41 ~0 ~18 minecraft:cobblestone
setblock ~41 ~0 ~19 minecraft:mossy_stone_bricks
fill ~41 ~0 ~23 ~42 ~0 ~24 minecraft:cobblestone
setblock ~41 ~0 ~25 minecraft:mossy_cobblestone
setblock ~41 ~0 ~26 minecraft:cobblestone
fill ~42 ~0 ~0 ~43 ~0 ~1 minecraft:cobblestone
setblock ~42 ~0 ~2 minecraft:cobblestone
setblock ~42 ~0 ~3 minecraft:andesite
fill ~42 ~0 ~7 ~42 ~0 ~10 minecraft:mossy_cobblestone
setblock ~42 ~0 ~11 minecraft:cobblestone
fill ~42 ~0 ~12 ~43 ~0 ~12 minecraft:cracked_stone_bricks
setblock ~42 ~0 ~13 minecraft:cracked_stone_bricks
setblock ~42 ~0 ~14 minecraft:mossy_stone_bricks
setblock ~42 ~0 ~15 minecraft:cracked_stone_bricks
setblock ~42 ~0 ~16 minecraft:stone_bricks
setblock ~42 ~0 ~17 minecraft:mossy_stone_bricks
setblock ~42 ~0 ~18 minecraft:stone_bricks
fill ~42 ~0 ~19 ~42 ~0 ~20 minecraft:cobblestone
fill ~42 ~0 ~21 ~42 ~0 ~22 minecraft:mossy_cobblestone
setblock ~42 ~0 ~25 minecraft:cobblestone
setblock ~42 ~0 ~26 minecraft:mossy_cobblestone
fill ~43 ~0 ~2 ~43 ~0 ~3 minecraft:mossy_cobblestone
setblock ~43 ~0 ~5 minecraft:mossy_cobblestone
setblock ~43 ~0 ~6 minecraft:cobblestone
setblock ~43 ~0 ~7 minecraft:andesite
fill ~43 ~0 ~8 ~44 ~0 ~8 minecraft:cobblestone
fill ~43 ~0 ~9 ~43 ~0 ~10 minecraft:mossy_cobblestone
setblock ~43 ~0 ~11 minecraft:andesite
fill ~43 ~0 ~13 ~43 ~0 ~17 minecraft:stone_bricks
setblock ~43 ~0 ~18 minecraft:cracked_stone_bricks
setblock ~43 ~0 ~19 minecraft:stone_bricks
fill ~43 ~0 ~20 ~43 ~0 ~21 minecraft:cobblestone
setblock ~43 ~0 ~22 minecraft:andesite
fill ~43 ~0 ~23 ~45 ~0 ~23 minecraft:mossy_cobblestone
fill ~43 ~0 ~24 ~44 ~0 ~24 minecraft:cobblestone
fill ~43 ~0 ~25 ~43 ~0 ~26 minecraft:andesite
setblock ~44 ~0 ~0 minecraft:mossy_cobblestone
fill ~44 ~0 ~1 ~44 ~0 ~3 minecraft:cobblestone
setblock ~44 ~0 ~4 minecraft:mossy_cobblestone
setblock ~44 ~0 ~5 minecraft:cobblestone
setblock ~44 ~0 ~6 minecraft:andesite
fill ~44 ~0 ~7 ~45 ~0 ~7 minecraft:cobblestone
fill ~44 ~0 ~9 ~45 ~0 ~10 minecraft:cobblestone
fill ~44 ~0 ~11 ~44 ~0 ~14 minecraft:cobblestone
setblock ~44 ~0 ~15 minecraft:mossy_cobblestone
fill ~44 ~0 ~16 ~45 ~0 ~19 minecraft:cobblestone
setblock ~44 ~0 ~20 minecraft:andesite
fill ~44 ~0 ~21 ~44 ~0 ~22 minecraft:cobblestone
setblock ~44 ~0 ~25 minecraft:mossy_cobblestone
setblock ~44 ~0 ~26 minecraft:cobblestone
fill ~45 ~0 ~0 ~45 ~0 ~1 minecraft:cobblestone
fill ~45 ~0 ~2 ~46 ~0 ~2 minecraft:mossy_cobblestone
setblock ~45 ~0 ~3 minecraft:mossy_cobblestone
fill ~45 ~0 ~4 ~45 ~0 ~5 minecraft:cobblestone
setblock ~45 ~0 ~6 minecraft:mossy_cobblestone
setblock ~45 ~0 ~8 minecraft:mossy_cobblestone
setblock ~45 ~0 ~11 minecraft:mossy_cobblestone
fill ~45 ~0 ~12 ~45 ~0 ~13 minecraft:cobblestone
fill ~45 ~0 ~14 ~45 ~0 ~15 minecraft:mossy_cobblestone
fill ~45 ~0 ~20 ~45 ~0 ~21 minecraft:cobblestone
fill ~45 ~0 ~22 ~46 ~0 ~22 minecraft:mossy_cobblestone
fill ~45 ~0 ~24 ~45 ~0 ~26 minecraft:mossy_cobblestone
setblock ~46 ~0 ~0 minecraft:mossy_cobblestone
setblock ~46 ~0 ~1 minecraft:cobblestone
fill ~46 ~0 ~3 ~46 ~0 ~8 minecraft:cobblestone
fill ~46 ~0 ~9 ~46 ~0 ~10 minecraft:mossy_cobblestone
setblock ~46 ~0 ~11 minecraft:cobblestone
setblock ~46 ~0 ~12 minecraft:andesite
setblock ~46 ~0 ~13 minecraft:mossy_cobblestone
fill ~46 ~0 ~14 ~46 ~0 ~15 minecraft:cobblestone
setblock ~46 ~0 ~16 minecraft:mossy_cobblestone
setblock ~46 ~0 ~17 minecraft:cobblestone
setblock ~46 ~0 ~18 minecraft:mossy_cobblestone
fill ~46 ~0 ~19 ~46 ~0 ~21 minecraft:cobblestone
fill ~46 ~0 ~23 ~46 ~0 ~25 minecraft:cobblestone
setblock ~46 ~0 ~26 minecraft:mossy_cobblestone

# the walls: dark patched stone y 1..6, moss low; the gates are open y 1..4; the river cells are bars at y 0..1 under solid wall
fill ~18 ~0 ~0 ~28 ~0 ~0 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~18 ~0 ~2 ~28 ~0 ~2 minecraft:iron_bars[north=false,east=true,south=true,west=true]
fill ~18 ~0 ~24 ~28 ~0 ~24 minecraft:iron_bars[north=true,east=true,south=false,west=true]
fill ~18 ~0 ~26 ~28 ~0 ~26 minecraft:iron_bars[north=false,east=true,south=false,west=true]
setblock ~1 ~1 ~7 minecraft:stone_bricks
fill ~1 ~1 ~8 ~2 ~1 ~10 minecraft:deepslate_bricks
fill ~1 ~1 ~11 ~1 ~3 ~11 minecraft:cracked_deepslate_bricks
fill ~1 ~1 ~12 ~1 ~3 ~12 minecraft:deepslate_bricks
fill ~1 ~1 ~13 ~1 ~1 ~18 minecraft:mossy_stone_bricks
fill ~1 ~1 ~19 ~1 ~3 ~19 minecraft:deepslate_bricks
fill ~2 ~1 ~7 ~2 ~3 ~7 minecraft:mossy_stone_bricks
fill ~2 ~1 ~11 ~2 ~2 ~14 minecraft:deepslate_bricks
fill ~2 ~1 ~15 ~2 ~1 ~19 minecraft:mossy_stone_bricks
fill ~12 ~1 ~1 ~14 ~1 ~2 minecraft:stone_bricks
fill ~12 ~1 ~24 ~13 ~2 ~25 minecraft:mossy_stone_bricks
fill ~14 ~1 ~24 ~15 ~1 ~25 minecraft:deepslate_bricks
fill ~15 ~1 ~1 ~17 ~3 ~1 minecraft:deepslate_bricks
setblock ~15 ~1 ~2 minecraft:cracked_deepslate_bricks
fill ~16 ~1 ~2 ~17 ~3 ~2 minecraft:deepslate_bricks
setblock ~16 ~1 ~24 minecraft:cracked_deepslate_bricks
setblock ~16 ~1 ~25 minecraft:deepslate_bricks
setblock ~17 ~1 ~24 minecraft:deepslate_bricks
setblock ~17 ~1 ~25 minecraft:cracked_deepslate_bricks
setblock ~18 ~1 ~0 minecraft:iron_bars[north=false,east=true,south=false,west=false]
fill ~18 ~1 ~2 ~28 ~1 ~2 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~18 ~1 ~24 ~28 ~1 ~24 minecraft:iron_bars[north=false,east=true,south=false,west=true]
setblock ~18 ~1 ~26 minecraft:iron_bars[north=false,east=true,south=false,west=false]
fill ~19 ~1 ~0 ~27 ~1 ~0 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~19 ~1 ~26 ~27 ~1 ~26 minecraft:iron_bars[north=false,east=true,south=false,west=true]
setblock ~28 ~1 ~0 minecraft:iron_bars[north=false,east=false,south=false,west=true]
setblock ~28 ~1 ~26 minecraft:iron_bars[north=false,east=false,south=false,west=true]
fill ~29 ~1 ~1 ~32 ~1 ~2 minecraft:mossy_stone_bricks
fill ~29 ~1 ~24 ~29 ~1 ~25 minecraft:cracked_deepslate_bricks
fill ~30 ~1 ~24 ~34 ~1 ~24 minecraft:mossy_stone_bricks
setblock ~30 ~1 ~25 minecraft:deepslate_bricks
fill ~31 ~1 ~25 ~34 ~1 ~25 minecraft:mossy_stone_bricks
setblock ~33 ~1 ~1 minecraft:stone_bricks
fill ~33 ~1 ~2 ~33 ~3 ~2 minecraft:mossy_stone_bricks
fill ~34 ~1 ~1 ~34 ~3 ~1 minecraft:mossy_stone_bricks
setblock ~34 ~1 ~2 minecraft:stone_bricks
setblock ~44 ~1 ~7 minecraft:cracked_stone_bricks
setblock ~44 ~1 ~8 minecraft:stone_bricks
setblock ~44 ~1 ~9 minecraft:cracked_stone_bricks
fill ~44 ~1 ~10 ~44 ~1 ~14 minecraft:stone_bricks
fill ~44 ~1 ~15 ~44 ~1 ~19 minecraft:mossy_stone_bricks
fill ~45 ~1 ~7 ~45 ~2 ~9 minecraft:stone_bricks
setblock ~45 ~1 ~10 minecraft:cracked_stone_bricks
fill ~45 ~1 ~11 ~45 ~2 ~16 minecraft:stone_bricks
fill ~45 ~1 ~17 ~45 ~1 ~19 minecraft:mossy_stone_bricks
fill ~0 ~2 ~7 ~0 ~4 ~7 minecraft:stone_bricks
setblock ~0 ~2 ~8 minecraft:cracked_stone_bricks
fill ~0 ~2 ~9 ~1 ~5 ~9 minecraft:stone_bricks
fill ~0 ~2 ~10 ~0 ~3 ~11 minecraft:stone_bricks
setblock ~0 ~2 ~12 minecraft:deepslate_bricks
setblock ~0 ~2 ~13 minecraft:cracked_deepslate_bricks
fill ~0 ~2 ~14 ~0 ~3 ~16 minecraft:deepslate_bricks
fill ~0 ~2 ~17 ~0 ~3 ~17 minecraft:cracked_deepslate_bricks
fill ~0 ~2 ~18 ~0 ~2 ~19 minecraft:deepslate_bricks
fill ~1 ~2 ~7 ~1 ~2 ~8 minecraft:mossy_stone_bricks
setblock ~1 ~2 ~10 minecraft:deepslate_bricks
fill ~1 ~2 ~13 ~1 ~2 ~18 minecraft:deepslate_bricks
fill ~2 ~2 ~8 ~2 ~2 ~10 minecraft:mossy_stone_bricks
fill ~2 ~2 ~15 ~2 ~4 ~15 minecraft:deepslate_bricks
fill ~2 ~2 ~16 ~2 ~3 ~16 minecraft:cracked_deepslate_bricks
fill ~2 ~2 ~17 ~2 ~3 ~17 minecraft:deepslate_bricks
setblock ~2 ~2 ~18 minecraft:cracked_stone_bricks
fill ~2 ~2 ~19 ~2 ~4 ~19 minecraft:stone_bricks
fill ~12 ~2 ~0 ~15 ~2 ~0 minecraft:stone_bricks
fill ~12 ~2 ~1 ~13 ~5 ~2 minecraft:stone_bricks
fill ~12 ~2 ~26 ~14 ~2 ~26 minecraft:mossy_stone_bricks
setblock ~14 ~2 ~1 minecraft:cracked_stone_bricks
fill ~14 ~2 ~2 ~14 ~5 ~2 minecraft:stone_bricks
fill ~14 ~2 ~24 ~15 ~2 ~24 minecraft:deepslate_bricks
fill ~14 ~2 ~25 ~14 ~5 ~25 minecraft:deepslate_bricks
fill ~15 ~2 ~2 ~15 ~4 ~2 minecraft:deepslate_bricks
setblock ~15 ~2 ~25 minecraft:cracked_deepslate_bricks
fill ~15 ~2 ~26 ~15 ~5 ~26 minecraft:stone_bricks
fill ~16 ~2 ~0 ~17 ~4 ~0 minecraft:deepslate_bricks
fill ~16 ~2 ~24 ~17 ~5 ~24 minecraft:stone_bricks
fill ~16 ~2 ~25 ~16 ~5 ~26 minecraft:stone_bricks
fill ~17 ~2 ~25 ~17 ~3 ~25 minecraft:cracked_stone_bricks
fill ~17 ~2 ~26 ~17 ~6 ~26 minecraft:stone_bricks
fill ~18 ~2 ~1 ~18 ~3 ~1 minecraft:cracked_deepslate_bricks
fill ~18 ~2 ~25 ~22 ~2 ~25 minecraft:stone_bricks
fill ~19 ~2 ~1 ~23 ~2 ~1 minecraft:deepslate_bricks
setblock ~23 ~2 ~25 minecraft:cracked_deepslate_bricks
setblock ~24 ~2 ~1 minecraft:cracked_deepslate_bricks
fill ~24 ~2 ~25 ~25 ~4 ~25 minecraft:deepslate_bricks
fill ~25 ~2 ~1 ~28 ~2 ~1 minecraft:deepslate_bricks
setblock ~26 ~2 ~25 minecraft:cracked_deepslate_bricks
fill ~27 ~2 ~25 ~29 ~3 ~25 minecraft:deepslate_bricks
setblock ~29 ~2 ~0 minecraft:deepslate_bricks
fill ~29 ~2 ~1 ~30 ~3 ~2 minecraft:stone_bricks
fill ~29 ~2 ~24 ~31 ~2 ~24 minecraft:deepslate_bricks
fill ~29 ~2 ~26 ~30 ~2 ~26 minecraft:deepslate_bricks
fill ~30 ~2 ~0 ~34 ~2 ~0 minecraft:mossy_stone_bricks
setblock ~30 ~2 ~25 minecraft:cracked_deepslate_bricks
fill ~31 ~2 ~1 ~33 ~3 ~1 minecraft:mossy_stone_bricks
fill ~31 ~2 ~2 ~32 ~2 ~2 minecraft:mossy_stone_bricks
fill ~31 ~2 ~25 ~31 ~5 ~25 minecraft:stone_bricks
setblock ~31 ~2 ~26 minecraft:cracked_stone_bricks
fill ~32 ~2 ~24 ~34 ~2 ~26 minecraft:stone_bricks
fill ~34 ~2 ~2 ~34 ~3 ~2 minecraft:mossy_stone_bricks
fill ~44 ~2 ~7 ~44 ~4 ~7 minecraft:stone_bricks
fill ~44 ~2 ~8 ~44 ~3 ~8 minecraft:cracked_stone_bricks
fill ~44 ~2 ~9 ~44 ~4 ~9 minecraft:stone_bricks
setblock ~44 ~2 ~10 minecraft:cracked_stone_bricks
fill ~44 ~2 ~11 ~44 ~2 ~18 minecraft:stone_bricks
setblock ~44 ~2 ~19 minecraft:mossy_stone_bricks
fill ~45 ~2 ~10 ~45 ~5 ~10 minecraft:stone_bricks
fill ~45 ~2 ~17 ~46 ~2 ~19 minecraft:stone_bricks
fill ~46 ~2 ~7 ~46 ~6 ~7 minecraft:stone_bricks
setblock ~46 ~2 ~8 minecraft:cracked_stone_bricks
fill ~46 ~2 ~9 ~46 ~5 ~11 minecraft:stone_bricks
setblock ~46 ~2 ~12 minecraft:cracked_stone_bricks
fill ~46 ~2 ~13 ~46 ~3 ~15 minecraft:stone_bricks
setblock ~46 ~2 ~16 minecraft:cracked_stone_bricks
fill ~0 ~3 ~8 ~0 ~4 ~8 minecraft:stone_bricks
setblock ~0 ~3 ~12 minecraft:cracked_deepslate_bricks
fill ~0 ~3 ~13 ~2 ~3 ~13 minecraft:deepslate_bricks
setblock ~0 ~3 ~18 minecraft:deepslate_bricks
setblock ~0 ~3 ~19 minecraft:cracked_deepslate_bricks
fill ~1 ~3 ~7 ~1 ~5 ~8 minecraft:stone_bricks
setblock ~1 ~3 ~10 minecraft:stone_bricks
fill ~1 ~3 ~14 ~1 ~3 ~15 minecraft:deepslate_bricks
setblock ~1 ~3 ~16 minecraft:cracked_deepslate_bricks
fill ~1 ~3 ~17 ~1 ~4 ~18 minecraft:deepslate_bricks
setblock ~2 ~3 ~8 minecraft:mossy_stone_bricks
setblock ~2 ~3 ~9 minecraft:cracked_deepslate_bricks
setblock ~2 ~3 ~10 minecraft:deepslate_bricks
fill ~2 ~3 ~11 ~2 ~3 ~12 minecraft:cracked_deepslate_bricks
setblock ~2 ~3 ~14 minecraft:cracked_deepslate_bricks
fill ~2 ~3 ~18 ~2 ~5 ~18 minecraft:stone_bricks
fill ~12 ~3 ~0 ~12 ~4 ~0 minecraft:stone_bricks
fill ~12 ~3 ~24 ~12 ~3 ~26 minecraft:mossy_stone_bricks
fill ~13 ~3 ~0 ~14 ~3 ~0 minecraft:cracked_stone_bricks
fill ~13 ~3 ~24 ~13 ~5 ~24 minecraft:deepslate_bricks
fill ~13 ~3 ~25 ~13 ~3 ~26 minecraft:mossy_stone_bricks
fill ~14 ~3 ~1 ~14 ~4 ~1 minecraft:stone_bricks
setblock ~14 ~3 ~24 minecraft:cracked_deepslate_bricks
setblock ~14 ~3 ~26 minecraft:deepslate_bricks
fill ~15 ~3 ~0 ~15 ~6 ~0 minecraft:stone_bricks
fill ~15 ~3 ~24 ~15 ~3 ~25 minecraft:deepslate_bricks
setblock ~18 ~3 ~0 minecraft:cracked_deepslate_bricks
fill ~18 ~3 ~2 ~18 ~4 ~2 minecraft:deepslate_bricks
setblock ~18 ~3 ~24 minecraft:cracked_stone_bricks
fill ~18 ~3 ~25 ~18 ~5 ~26 minecraft:stone_bricks
fill ~19 ~3 ~0 ~25 ~3 ~0 minecraft:deepslate_bricks
fill ~19 ~3 ~1 ~19 ~3 ~2 minecraft:deepslate_bricks
fill ~19 ~3 ~24 ~22 ~3 ~24 minecraft:stone_bricks
fill ~19 ~3 ~25 ~19 ~3 ~26 minecraft:cracked_stone_bricks
setblock ~20 ~3 ~1 minecraft:cracked_deepslate_bricks
fill ~20 ~3 ~2 ~23 ~3 ~2 minecraft:deepslate_bricks
fill ~20 ~3 ~25 ~21 ~3 ~26 minecraft:stone_bricks
fill ~21 ~3 ~1 ~25 ~3 ~1 minecraft:deepslate_bricks
fill ~22 ~3 ~25 ~23 ~3 ~26 minecraft:deepslate_bricks
fill ~23 ~3 ~24 ~27 ~3 ~24 minecraft:deepslate_bricks
fill ~24 ~3 ~2 ~24 ~4 ~2 minecraft:cracked_deepslate_bricks
fill ~24 ~3 ~26 ~25 ~4 ~26 minecraft:deepslate_bricks
fill ~25 ~3 ~2 ~26 ~4 ~2 minecraft:deepslate_bricks
fill ~26 ~3 ~0 ~27 ~3 ~0 minecraft:cracked_deepslate_bricks
setblock ~26 ~3 ~1 minecraft:cracked_deepslate_bricks
fill ~26 ~3 ~25 ~26 ~4 ~26 minecraft:deepslate_bricks
fill ~27 ~3 ~1 ~27 ~3 ~2 minecraft:deepslate_bricks
fill ~27 ~3 ~26 ~27 ~4 ~26 minecraft:deepslate_bricks
fill ~28 ~3 ~0 ~28 ~5 ~1 minecraft:deepslate_bricks
fill ~28 ~3 ~2 ~28 ~5 ~2 minecraft:stone_bricks
fill ~28 ~3 ~24 ~29 ~3 ~24 minecraft:cracked_deepslate_bricks
setblock ~28 ~3 ~26 minecraft:cracked_deepslate_bricks
fill ~29 ~3 ~0 ~30 ~3 ~0 minecraft:stone_bricks
setblock ~29 ~3 ~26 minecraft:deepslate_bricks
setblock ~30 ~3 ~24 minecraft:deepslate_bricks
fill ~30 ~3 ~25 ~30 ~3 ~26 minecraft:stone_bricks
fill ~31 ~3 ~0 ~34 ~3 ~0 minecraft:mossy_stone_bricks
fill ~31 ~3 ~2 ~31 ~5 ~2 minecraft:stone_bricks
fill ~31 ~3 ~24 ~33 ~3 ~24 minecraft:stone_bricks
fill ~31 ~3 ~26 ~32 ~4 ~26 minecraft:stone_bricks
setblock ~32 ~3 ~2 minecraft:mossy_stone_bricks
fill ~32 ~3 ~25 ~32 ~4 ~25 minecraft:cracked_stone_bricks
fill ~33 ~3 ~25 ~34 ~4 ~26 minecraft:stone_bricks
setblock ~34 ~3 ~24 minecraft:cracked_stone_bricks
fill ~44 ~3 ~10 ~44 ~4 ~12 minecraft:stone_bricks
setblock ~44 ~3 ~13 minecraft:cracked_stone_bricks
fill ~44 ~3 ~14 ~44 ~5 ~16 minecraft:stone_bricks
setblock ~44 ~3 ~17 minecraft:cracked_stone_bricks
fill ~44 ~3 ~18 ~44 ~5 ~19 minecraft:stone_bricks
fill ~45 ~3 ~7 ~45 ~4 ~7 minecraft:cracked_stone_bricks
fill ~45 ~3 ~8 ~46 ~5 ~8 minecraft:stone_bricks
fill ~45 ~3 ~9 ~45 ~5 ~9 minecraft:stone_bricks
fill ~45 ~3 ~11 ~45 ~3 ~13 minecraft:stone_bricks
setblock ~45 ~3 ~14 minecraft:cracked_stone_bricks
fill ~45 ~3 ~15 ~45 ~4 ~17 minecraft:stone_bricks
setblock ~45 ~3 ~18 minecraft:cracked_stone_bricks
fill ~45 ~3 ~19 ~45 ~5 ~19 minecraft:stone_bricks
fill ~46 ~3 ~12 ~46 ~6 ~12 minecraft:stone_bricks
fill ~46 ~3 ~16 ~46 ~3 ~19 minecraft:stone_bricks
setblock ~0 ~4 ~10 minecraft:stone_bricks
setblock ~0 ~4 ~11 minecraft:cracked_stone_bricks
fill ~0 ~4 ~12 ~0 ~6 ~15 minecraft:stone_bricks
fill ~0 ~4 ~16 ~0 ~5 ~16 minecraft:cracked_stone_bricks
setblock ~0 ~4 ~17 minecraft:deepslate_bricks
fill ~0 ~4 ~18 ~0 ~4 ~19 minecraft:mossy_stone_bricks
setblock ~1 ~4 ~10 minecraft:cracked_stone_bricks
fill ~1 ~4 ~11 ~1 ~5 ~14 minecraft:stone_bricks
fill ~1 ~4 ~15 ~1 ~4 ~16 minecraft:deepslate_bricks
fill ~1 ~4 ~19 ~1 ~5 ~19 minecraft:stone_bricks
fill ~2 ~4 ~7 ~2 ~5 ~7 minecraft:stone_bricks
setblock ~2 ~4 ~8 minecraft:cracked_stone_bricks
fill ~2 ~4 ~9 ~2 ~4 ~12 minecraft:stone_bricks
fill ~2 ~4 ~13 ~2 ~4 ~14 minecraft:deepslate_bricks
setblock ~2 ~4 ~16 minecraft:deepslate_bricks
fill ~2 ~4 ~17 ~2 ~5 ~17 minecraft:stone_bricks
fill ~12 ~4 ~24 ~12 ~5 ~26 minecraft:deepslate_bricks
fill ~13 ~4 ~0 ~14 ~5 ~0 minecraft:stone_bricks
fill ~13 ~4 ~25 ~13 ~4 ~26 minecraft:deepslate_bricks
fill ~14 ~4 ~24 ~15 ~5 ~24 minecraft:deepslate_bricks
setblock ~14 ~4 ~26 minecraft:cracked_deepslate_bricks
setblock ~15 ~4 ~1 minecraft:stone_bricks
setblock ~15 ~4 ~25 minecraft:cracked_deepslate_bricks
fill ~16 ~4 ~1 ~17 ~4 ~1 minecraft:deepslate_bricks
fill ~16 ~4 ~2 ~16 ~5 ~2 minecraft:deepslate_bricks
setblock ~17 ~4 ~2 minecraft:cracked_deepslate_bricks
fill ~17 ~4 ~25 ~17 ~5 ~25 minecraft:stone_bricks
fill ~18 ~4 ~0 ~20 ~4 ~1 minecraft:deepslate_bricks
fill ~18 ~4 ~24 ~20 ~4 ~24 minecraft:stone_bricks
fill ~19 ~4 ~2 ~22 ~4 ~2 minecraft:cracked_deepslate_bricks
fill ~19 ~4 ~25 ~19 ~4 ~26 minecraft:stone_bricks
fill ~20 ~4 ~25 ~21 ~4 ~26 minecraft:deepslate_bricks
setblock ~21 ~4 ~0 minecraft:cracked_deepslate_bricks
fill ~21 ~4 ~1 ~24 ~4 ~1 minecraft:deepslate_bricks
fill ~21 ~4 ~24 ~22 ~5 ~24 minecraft:cracked_deepslate_bricks
fill ~22 ~4 ~0 ~27 ~4 ~0 minecraft:deepslate_bricks
setblock ~22 ~4 ~25 minecraft:cracked_deepslate_bricks
fill ~22 ~4 ~26 ~22 ~6 ~26 minecraft:deepslate_bricks
setblock ~23 ~4 ~2 minecraft:deepslate_bricks
fill ~23 ~4 ~24 ~23 ~5 ~25 minecraft:deepslate_bricks
setblock ~23 ~4 ~26 minecraft:cracked_deepslate_bricks
fill ~24 ~4 ~24 ~25 ~4 ~24 minecraft:cracked_deepslate_bricks
setblock ~25 ~4 ~1 minecraft:cracked_deepslate_bricks
fill ~26 ~4 ~1 ~27 ~4 ~1 minecraft:deepslate_bricks
fill ~26 ~4 ~24 ~29 ~4 ~24 minecraft:deepslate_bricks
setblock ~27 ~4 ~2 minecraft:cracked_deepslate_bricks
setblock ~27 ~4 ~25 minecraft:cracked_deepslate_bricks
fill ~28 ~4 ~25 ~28 ~4 ~26 minecraft:deepslate_bricks
fill ~29 ~4 ~0 ~29 ~5 ~2 minecraft:stone_bricks
fill ~29 ~4 ~25 ~30 ~4 ~25 minecraft:cracked_stone_bricks
fill ~29 ~4 ~26 ~30 ~6 ~26 minecraft:stone_bricks
fill ~30 ~4 ~0 ~31 ~4 ~1 minecraft:cracked_stone_bricks
fill ~30 ~4 ~2 ~30 ~5 ~2 minecraft:stone_bricks
fill ~30 ~4 ~24 ~32 ~4 ~24 minecraft:stone_bricks
fill ~32 ~4 ~0 ~33 ~4 ~1 minecraft:stone_bricks
setblock ~32 ~4 ~2 minecraft:stone_bricks
setblock ~33 ~4 ~2 minecraft:deepslate_bricks
setblock ~33 ~4 ~24 minecraft:cracked_stone_bricks
fill ~34 ~4 ~0 ~34 ~4 ~2 minecraft:deepslate_bricks
setblock ~34 ~4 ~24 minecraft:stone_bricks
setblock ~44 ~4 ~8 minecraft:stone_bricks
fill ~44 ~4 ~13 ~46 ~5 ~13 minecraft:stone_bricks
setblock ~44 ~4 ~17 minecraft:stone_bricks
setblock ~45 ~4 ~11 minecraft:stone_bricks
setblock ~45 ~4 ~12 minecraft:cracked_stone_bricks
fill ~45 ~4 ~14 ~46 ~5 ~14 minecraft:stone_bricks
fill ~45 ~4 ~18 ~45 ~5 ~18 minecraft:stone_bricks
fill ~46 ~4 ~15 ~46 ~4 ~16 minecraft:cracked_stone_bricks
fill ~46 ~4 ~17 ~46 ~4 ~19 minecraft:stone_bricks
fill ~0 ~5 ~7 ~0 ~5 ~8 minecraft:cracked_stone_bricks
setblock ~0 ~5 ~10 minecraft:cracked_stone_bricks
setblock ~0 ~5 ~11 minecraft:stone_bricks
fill ~0 ~5 ~17 ~0 ~5 ~19 minecraft:stone_bricks
setblock ~1 ~5 ~10 minecraft:stone_bricks
fill ~1 ~5 ~15 ~1 ~5 ~16 minecraft:stone_bricks
setblock ~1 ~5 ~17 minecraft:cracked_stone_bricks
setblock ~1 ~5 ~18 minecraft:stone_bricks
fill ~2 ~5 ~8 ~2 ~5 ~9 minecraft:stone_bricks
fill ~2 ~5 ~10 ~2 ~5 ~11 minecraft:cracked_stone_bricks
fill ~2 ~5 ~12 ~2 ~5 ~16 minecraft:stone_bricks
setblock ~2 ~5 ~19 minecraft:cracked_stone_bricks
setblock ~12 ~5 ~0 minecraft:cracked_stone_bricks
fill ~13 ~5 ~25 ~13 ~5 ~26 minecraft:cracked_deepslate_bricks
fill ~14 ~5 ~1 ~15 ~5 ~1 minecraft:cracked_stone_bricks
fill ~14 ~5 ~26 ~14 ~6 ~26 minecraft:deepslate_bricks
setblock ~15 ~5 ~2 minecraft:stone_bricks
setblock ~15 ~5 ~25 minecraft:stone_bricks
fill ~16 ~5 ~0 ~16 ~6 ~0 minecraft:stone_bricks
setblock ~16 ~5 ~1 minecraft:deepslate_bricks
fill ~17 ~5 ~0 ~17 ~5 ~1 minecraft:cracked_deepslate_bricks
fill ~17 ~5 ~2 ~19 ~5 ~2 minecraft:stone_bricks
fill ~18 ~5 ~0 ~21 ~5 ~1 minecraft:stone_bricks
fill ~18 ~5 ~24 ~19 ~5 ~24 minecraft:stone_bricks
setblock ~19 ~5 ~25 minecraft:deepslate_bricks
setblock ~19 ~5 ~26 minecraft:cracked_deepslate_bricks
setblock ~20 ~5 ~2 minecraft:cracked_stone_bricks
setblock ~20 ~5 ~24 minecraft:deepslate_bricks
setblock ~20 ~5 ~25 minecraft:cracked_deepslate_bricks
fill ~20 ~5 ~26 ~20 ~6 ~26 minecraft:deepslate_bricks
setblock ~21 ~5 ~2 minecraft:stone_bricks
fill ~21 ~5 ~25 ~22 ~5 ~25 minecraft:deepslate_bricks
setblock ~21 ~5 ~26 minecraft:cracked_deepslate_bricks
setblock ~22 ~5 ~0 minecraft:cracked_stone_bricks
fill ~22 ~5 ~1 ~22 ~5 ~2 minecraft:stone_bricks
fill ~23 ~5 ~0 ~24 ~5 ~1 minecraft:stone_bricks
setblock ~23 ~5 ~2 minecraft:stone_bricks
fill ~23 ~5 ~26 ~23 ~6 ~26 minecraft:deepslate_bricks
setblock ~24 ~5 ~2 minecraft:deepslate_bricks
fill ~24 ~5 ~24 ~27 ~5 ~24 minecraft:deepslate_bricks
fill ~24 ~5 ~25 ~24 ~5 ~26 minecraft:deepslate_bricks
fill ~25 ~5 ~0 ~27 ~6 ~0 minecraft:deepslate_bricks
fill ~25 ~5 ~1 ~26 ~5 ~1 minecraft:deepslate_bricks
setblock ~25 ~5 ~2 minecraft:cracked_deepslate_bricks
fill ~25 ~5 ~25 ~25 ~5 ~26 minecraft:cracked_deepslate_bricks
fill ~26 ~5 ~2 ~27 ~5 ~2 minecraft:deepslate_bricks
fill ~26 ~5 ~25 ~27 ~5 ~25 minecraft:deepslate_bricks
setblock ~26 ~5 ~26 minecraft:cracked_deepslate_bricks
setblock ~27 ~5 ~1 minecraft:cracked_deepslate_bricks
fill ~27 ~5 ~26 ~27 ~6 ~26 minecraft:stone_bricks
fill ~28 ~5 ~24 ~28 ~5 ~26 minecraft:stone_bricks
fill ~29 ~5 ~24 ~30 ~5 ~24 minecraft:cracked_stone_bricks
fill ~29 ~5 ~25 ~30 ~5 ~25 minecraft:stone_bricks
fill ~30 ~5 ~0 ~32 ~5 ~0 minecraft:stone_bricks
fill ~30 ~5 ~1 ~31 ~5 ~1 minecraft:cracked_stone_bricks
setblock ~31 ~5 ~24 minecraft:stone_bricks
fill ~31 ~5 ~26 ~31 ~6 ~26 minecraft:stone_bricks
setblock ~32 ~5 ~1 minecraft:stone_bricks
fill ~32 ~5 ~2 ~33 ~5 ~2 minecraft:cracked_deepslate_bricks
setblock ~32 ~5 ~24 minecraft:cracked_stone_bricks
setblock ~32 ~5 ~25 minecraft:cracked_deepslate_bricks
fill ~32 ~5 ~26 ~32 ~6 ~26 minecraft:deepslate_bricks
fill ~33 ~5 ~0 ~33 ~5 ~1 minecraft:deepslate_bricks
fill ~33 ~5 ~24 ~34 ~5 ~26 minecraft:deepslate_bricks
setblock ~34 ~5 ~0 minecraft:cracked_deepslate_bricks
fill ~34 ~5 ~1 ~34 ~5 ~2 minecraft:deepslate_bricks
fill ~44 ~5 ~7 ~45 ~5 ~7 minecraft:deepslate_bricks
fill ~44 ~5 ~8 ~44 ~5 ~9 minecraft:deepslate_bricks
fill ~44 ~5 ~10 ~44 ~5 ~11 minecraft:stone_bricks
setblock ~44 ~5 ~12 minecraft:cracked_stone_bricks
fill ~44 ~5 ~17 ~45 ~5 ~17 minecraft:cracked_stone_bricks
setblock ~45 ~5 ~11 minecraft:cracked_stone_bricks
setblock ~45 ~5 ~12 minecraft:stone_bricks
fill ~45 ~5 ~15 ~46 ~5 ~15 minecraft:stone_bricks
setblock ~45 ~5 ~16 minecraft:stone_bricks
fill ~46 ~5 ~16 ~46 ~5 ~17 minecraft:cracked_stone_bricks
fill ~46 ~5 ~18 ~46 ~6 ~19 minecraft:stone_bricks
fill ~0 ~6 ~7 ~0 ~6 ~10 minecraft:stone_bricks
setblock ~0 ~6 ~11 minecraft:cracked_stone_bricks
fill ~0 ~6 ~16 ~0 ~6 ~17 minecraft:stone_bricks
setblock ~0 ~6 ~18 minecraft:cracked_stone_bricks
setblock ~0 ~6 ~19 minecraft:stone_bricks
fill ~7 ~6 ~0 ~12 ~6 ~0 minecraft:stone_bricks
fill ~7 ~6 ~26 ~9 ~6 ~26 minecraft:stone_bricks
setblock ~10 ~6 ~26 minecraft:cracked_stone_bricks
setblock ~11 ~6 ~26 minecraft:deepslate_bricks
setblock ~12 ~6 ~26 minecraft:cracked_deepslate_bricks
setblock ~13 ~6 ~0 minecraft:cracked_stone_bricks
setblock ~13 ~6 ~26 minecraft:deepslate_bricks
setblock ~14 ~6 ~0 minecraft:stone_bricks
fill ~15 ~6 ~26 ~16 ~6 ~26 minecraft:cracked_stone_bricks
fill ~17 ~6 ~0 ~24 ~6 ~0 minecraft:stone_bricks
setblock ~18 ~6 ~26 minecraft:stone_bricks
setblock ~19 ~6 ~26 minecraft:deepslate_bricks
setblock ~21 ~6 ~26 minecraft:deepslate_bricks
fill ~24 ~6 ~26 ~25 ~6 ~26 minecraft:deepslate_bricks
setblock ~26 ~6 ~26 minecraft:cracked_stone_bricks
setblock ~28 ~6 ~0 minecraft:deepslate_bricks
setblock ~28 ~6 ~26 minecraft:stone_bricks
setblock ~29 ~6 ~0 minecraft:cracked_stone_bricks
setblock ~30 ~6 ~0 minecraft:stone_bricks
setblock ~31 ~6 ~0 minecraft:cracked_stone_bricks
setblock ~32 ~6 ~0 minecraft:stone_bricks
fill ~33 ~6 ~0 ~37 ~6 ~0 minecraft:deepslate_bricks
fill ~33 ~6 ~26 ~34 ~6 ~26 minecraft:deepslate_bricks
fill ~35 ~6 ~26 ~39 ~6 ~26 minecraft:stone_bricks
fill ~38 ~6 ~0 ~39 ~6 ~0 minecraft:cracked_deepslate_bricks
setblock ~46 ~6 ~8 minecraft:stone_bricks
setblock ~46 ~6 ~9 minecraft:cracked_stone_bricks
fill ~46 ~6 ~10 ~46 ~6 ~11 minecraft:stone_bricks
setblock ~46 ~6 ~13 minecraft:cracked_stone_bricks
fill ~46 ~6 ~14 ~46 ~6 ~17 minecraft:stone_bricks

# the plinth lip: cobblestone stairs at y 1 on the outer faces between the gates and the towers, and the timber lintels of the gates and the water gates
fill ~0 ~1 ~7 ~0 ~1 ~19 minecraft:cobblestone_stairs[facing=east,half=bottom]
fill ~12 ~1 ~0 ~17 ~1 ~0 minecraft:cobblestone_stairs[facing=south,half=bottom]
fill ~12 ~1 ~26 ~17 ~1 ~26 minecraft:cobblestone_stairs[facing=north,half=bottom]
fill ~29 ~1 ~0 ~34 ~1 ~0 minecraft:cobblestone_stairs[facing=south,half=bottom]
fill ~29 ~1 ~26 ~34 ~1 ~26 minecraft:cobblestone_stairs[facing=north,half=bottom]
fill ~46 ~1 ~7 ~46 ~1 ~19 minecraft:cobblestone_stairs[facing=west,half=bottom]
fill ~18 ~2 ~0 ~28 ~2 ~0 minecraft:dark_oak_log[axis=x]
fill ~18 ~2 ~2 ~28 ~2 ~2 minecraft:dark_oak_log[axis=x]
fill ~18 ~2 ~24 ~28 ~2 ~24 minecraft:dark_oak_log[axis=x]
fill ~18 ~2 ~26 ~28 ~2 ~26 minecraft:dark_oak_log[axis=x]
fill ~7 ~5 ~0 ~11 ~5 ~2 minecraft:dark_oak_log[axis=z]
fill ~7 ~5 ~24 ~11 ~5 ~26 minecraft:dark_oak_log[axis=z]
fill ~35 ~5 ~0 ~39 ~5 ~2 minecraft:dark_oak_log[axis=z]
fill ~35 ~5 ~24 ~39 ~5 ~26 minecraft:dark_oak_log[axis=z]

# the wall-walk at y 6 (feet 7): flagstones, the parapet on the outer faces at y 7..8 with crenels, caps of stone brick slabs, posts with lanterns every twelfth block on the inner edge
fill ~1 ~6 ~7 ~2 ~6 ~7 minecraft:stone_bricks
fill ~1 ~6 ~8 ~1 ~6 ~9 minecraft:stone_bricks
fill ~1 ~6 ~10 ~1 ~6 ~11 minecraft:mossy_stone_bricks
setblock ~1 ~6 ~12 minecraft:cobblestone
setblock ~1 ~6 ~13 minecraft:stone_bricks
setblock ~1 ~6 ~14 minecraft:cracked_stone_bricks
setblock ~1 ~6 ~15 minecraft:mossy_stone_bricks
fill ~1 ~6 ~16 ~1 ~6 ~17 minecraft:cracked_stone_bricks
setblock ~1 ~6 ~18 minecraft:stone_bricks
fill ~1 ~6 ~19 ~2 ~6 ~19 minecraft:mossy_stone_bricks
setblock ~2 ~6 ~8 minecraft:cobblestone
fill ~2 ~6 ~9 ~2 ~6 ~12 minecraft:stone_bricks
setblock ~2 ~6 ~13 minecraft:mossy_stone_bricks
setblock ~2 ~6 ~14 minecraft:stone_bricks
setblock ~2 ~6 ~15 minecraft:cracked_stone_bricks
fill ~2 ~6 ~16 ~2 ~6 ~18 minecraft:stone_bricks
fill ~7 ~6 ~1 ~10 ~6 ~1 minecraft:stone_bricks
setblock ~7 ~6 ~2 minecraft:cobblestone
setblock ~7 ~6 ~24 minecraft:stone_bricks
setblock ~7 ~6 ~25 minecraft:mossy_stone_bricks
setblock ~8 ~6 ~2 minecraft:cracked_stone_bricks
setblock ~8 ~6 ~24 minecraft:mossy_stone_bricks
setblock ~8 ~6 ~25 minecraft:stone_bricks
setblock ~9 ~6 ~2 minecraft:cobblestone
fill ~9 ~6 ~24 ~10 ~6 ~25 minecraft:stone_bricks
fill ~10 ~6 ~2 ~11 ~6 ~2 minecraft:stone_bricks
setblock ~11 ~6 ~1 minecraft:cracked_stone_bricks
setblock ~11 ~6 ~24 minecraft:cracked_stone_bricks
fill ~11 ~6 ~25 ~13 ~6 ~25 minecraft:stone_bricks
setblock ~12 ~6 ~1 minecraft:stone_bricks
setblock ~12 ~6 ~2 minecraft:mossy_stone_bricks
fill ~12 ~6 ~24 ~17 ~6 ~24 minecraft:stone_bricks
fill ~13 ~6 ~1 ~13 ~6 ~2 minecraft:cracked_stone_bricks
setblock ~14 ~6 ~1 minecraft:cobblestone
setblock ~14 ~6 ~2 minecraft:cracked_stone_bricks
setblock ~14 ~6 ~25 minecraft:cracked_stone_bricks
setblock ~15 ~6 ~1 minecraft:mossy_stone_bricks
setblock ~15 ~6 ~2 minecraft:stone_bricks
setblock ~15 ~6 ~25 minecraft:stone_bricks
fill ~16 ~6 ~1 ~16 ~6 ~2 minecraft:stone_bricks
setblock ~16 ~6 ~25 minecraft:cracked_stone_bricks
setblock ~17 ~6 ~1 minecraft:cracked_stone_bricks
setblock ~17 ~6 ~2 minecraft:mossy_stone_bricks
setblock ~17 ~6 ~25 minecraft:stone_bricks
fill ~18 ~6 ~1 ~21 ~6 ~1 minecraft:stone_bricks
fill ~18 ~6 ~2 ~20 ~6 ~2 minecraft:stone_bricks
setblock ~18 ~6 ~24 minecraft:cobblestone
setblock ~18 ~6 ~25 minecraft:cracked_stone_bricks
fill ~19 ~6 ~24 ~19 ~6 ~25 minecraft:stone_bricks
setblock ~20 ~6 ~24 minecraft:cracked_stone_bricks
fill ~20 ~6 ~25 ~21 ~6 ~25 minecraft:stone_bricks
setblock ~21 ~6 ~2 minecraft:mossy_stone_bricks
setblock ~21 ~6 ~24 minecraft:mossy_stone_bricks
setblock ~22 ~6 ~1 minecraft:mossy_stone_bricks
setblock ~22 ~6 ~2 minecraft:stone_bricks
fill ~22 ~6 ~24 ~24 ~6 ~24 minecraft:stone_bricks
setblock ~22 ~6 ~25 minecraft:cracked_stone_bricks
fill ~23 ~6 ~1 ~23 ~6 ~2 minecraft:stone_bricks
fill ~23 ~6 ~25 ~24 ~6 ~25 minecraft:stone_bricks
setblock ~24 ~6 ~1 minecraft:mossy_stone_bricks
setblock ~24 ~6 ~2 minecraft:stone_bricks
setblock ~25 ~6 ~1 minecraft:cracked_stone_bricks
setblock ~25 ~6 ~2 minecraft:mossy_stone_bricks
setblock ~25 ~6 ~24 minecraft:cobblestone
setblock ~25 ~6 ~25 minecraft:cracked_stone_bricks
fill ~26 ~6 ~1 ~30 ~6 ~1 minecraft:stone_bricks
fill ~26 ~6 ~2 ~27 ~6 ~2 minecraft:stone_bricks
fill ~26 ~6 ~24 ~26 ~6 ~25 minecraft:stone_bricks
setblock ~27 ~6 ~24 minecraft:cracked_stone_bricks
setblock ~27 ~6 ~25 minecraft:stone_bricks
fill ~28 ~6 ~2 ~29 ~6 ~2 minecraft:mossy_stone_bricks
setblock ~28 ~6 ~24 minecraft:stone_bricks
setblock ~28 ~6 ~25 minecraft:mossy_stone_bricks
setblock ~29 ~6 ~24 minecraft:cracked_stone_bricks
setblock ~29 ~6 ~25 minecraft:stone_bricks
setblock ~30 ~6 ~2 minecraft:stone_bricks
fill ~30 ~6 ~24 ~30 ~6 ~25 minecraft:mossy_stone_bricks
fill ~31 ~6 ~1 ~33 ~6 ~1 minecraft:cracked_stone_bricks
setblock ~31 ~6 ~2 minecraft:mossy_stone_bricks
setblock ~31 ~6 ~24 minecraft:cobblestone
fill ~31 ~6 ~25 ~32 ~6 ~25 minecraft:stone_bricks
fill ~32 ~6 ~2 ~36 ~6 ~2 minecraft:stone_bricks
setblock ~32 ~6 ~24 minecraft:cracked_stone_bricks
fill ~33 ~6 ~24 ~35 ~6 ~24 minecraft:stone_bricks
fill ~33 ~6 ~25 ~34 ~6 ~25 minecraft:cracked_stone_bricks
fill ~34 ~6 ~1 ~37 ~6 ~1 minecraft:stone_bricks
fill ~35 ~6 ~25 ~39 ~6 ~25 minecraft:stone_bricks
setblock ~36 ~6 ~24 minecraft:cobblestone
setblock ~37 ~6 ~2 minecraft:cobblestone
fill ~37 ~6 ~24 ~38 ~6 ~24 minecraft:cracked_stone_bricks
setblock ~38 ~6 ~1 minecraft:mossy_stone_bricks
setblock ~38 ~6 ~2 minecraft:stone_bricks
setblock ~39 ~6 ~1 minecraft:stone_bricks
setblock ~39 ~6 ~2 minecraft:mossy_stone_bricks
setblock ~39 ~6 ~24 minecraft:mossy_stone_bricks
fill ~44 ~6 ~7 ~45 ~6 ~9 minecraft:stone_bricks
fill ~44 ~6 ~10 ~44 ~6 ~13 minecraft:stone_bricks
fill ~44 ~6 ~14 ~45 ~6 ~14 minecraft:cracked_stone_bricks
fill ~44 ~6 ~15 ~45 ~6 ~15 minecraft:stone_bricks
setblock ~44 ~6 ~16 minecraft:stone_bricks
setblock ~44 ~6 ~17 minecraft:cobblestone
fill ~44 ~6 ~18 ~45 ~6 ~19 minecraft:stone_bricks
setblock ~45 ~6 ~10 minecraft:cracked_stone_bricks
setblock ~45 ~6 ~11 minecraft:cobblestone
fill ~45 ~6 ~12 ~45 ~6 ~13 minecraft:stone_bricks
fill ~45 ~6 ~16 ~45 ~6 ~17 minecraft:mossy_stone_bricks
setblock ~0 ~7 ~7 minecraft:deepslate_bricks
setblock ~0 ~7 ~10 minecraft:cracked_stone_bricks
fill ~0 ~7 ~11 ~0 ~7 ~13 minecraft:stone_bricks
fill ~0 ~7 ~16 ~0 ~7 ~19 minecraft:cracked_stone_bricks
setblock ~2 ~7 ~10 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~2 ~7 ~16 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~7 ~7 ~0 minecraft:stone_bricks
fill ~7 ~7 ~26 ~7 ~8 ~26 minecraft:deepslate_bricks
fill ~10 ~7 ~0 ~13 ~7 ~0 minecraft:stone_bricks
fill ~10 ~7 ~26 ~13 ~7 ~26 minecraft:stone_bricks
setblock ~13 ~7 ~2 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~13 ~7 ~24 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~16 ~7 ~0 minecraft:cracked_stone_bricks
fill ~16 ~7 ~26 ~17 ~7 ~26 minecraft:deepslate_bricks
fill ~17 ~7 ~0 ~18 ~7 ~0 minecraft:stone_bricks
fill ~18 ~7 ~26 ~18 ~8 ~26 minecraft:stone_bricks
fill ~19 ~7 ~0 ~19 ~8 ~0 minecraft:deepslate_bricks
setblock ~19 ~7 ~26 minecraft:cracked_stone_bricks
setblock ~22 ~7 ~0 minecraft:deepslate_bricks
fill ~22 ~7 ~26 ~25 ~7 ~26 minecraft:deepslate_bricks
fill ~23 ~7 ~0 ~25 ~7 ~0 minecraft:stone_bricks
setblock ~24 ~7 ~2 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~24 ~7 ~24 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
fill ~28 ~7 ~0 ~29 ~8 ~0 minecraft:stone_bricks
setblock ~28 ~7 ~26 minecraft:cracked_stone_bricks
setblock ~29 ~7 ~26 minecraft:stone_bricks
setblock ~30 ~7 ~0 minecraft:cracked_deepslate_bricks
fill ~30 ~7 ~26 ~31 ~7 ~26 minecraft:cracked_stone_bricks
setblock ~31 ~7 ~0 minecraft:deepslate_bricks
setblock ~33 ~7 ~2 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~33 ~7 ~24 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
fill ~34 ~7 ~0 ~35 ~7 ~0 minecraft:stone_bricks
setblock ~34 ~7 ~26 minecraft:stone_bricks
setblock ~35 ~7 ~26 minecraft:cracked_stone_bricks
setblock ~36 ~7 ~0 minecraft:deepslate_bricks
fill ~36 ~7 ~26 ~37 ~7 ~26 minecraft:stone_bricks
setblock ~37 ~7 ~0 minecraft:cracked_deepslate_bricks
setblock ~44 ~7 ~10 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~44 ~7 ~16 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
fill ~46 ~7 ~7 ~46 ~8 ~7 minecraft:stone_bricks
fill ~46 ~7 ~10 ~46 ~8 ~12 minecraft:stone_bricks
setblock ~46 ~7 ~13 minecraft:cracked_stone_bricks
setblock ~46 ~7 ~16 minecraft:deepslate_bricks
fill ~46 ~7 ~17 ~46 ~7 ~19 minecraft:stone_bricks
setblock ~0 ~8 ~7 minecraft:cracked_stone_bricks
setblock ~0 ~8 ~10 minecraft:cracked_deepslate_bricks
fill ~0 ~8 ~11 ~0 ~8 ~13 minecraft:deepslate_bricks
setblock ~0 ~8 ~16 minecraft:deepslate_bricks
fill ~0 ~8 ~17 ~0 ~8 ~19 minecraft:stone_bricks
setblock ~2 ~8 ~10 minecraft:lantern[hanging=false]
setblock ~2 ~8 ~16 minecraft:lantern[hanging=false]
setblock ~7 ~8 ~0 minecraft:deepslate_bricks
fill ~10 ~8 ~0 ~11 ~8 ~0 minecraft:stone_bricks
setblock ~10 ~8 ~26 minecraft:cracked_deepslate_bricks
fill ~11 ~8 ~26 ~13 ~8 ~26 minecraft:stone_bricks
fill ~12 ~8 ~0 ~13 ~8 ~0 minecraft:deepslate_bricks
setblock ~13 ~8 ~2 minecraft:lantern[hanging=false]
setblock ~13 ~8 ~24 minecraft:lantern[hanging=false]
fill ~16 ~8 ~0 ~18 ~8 ~0 minecraft:deepslate_bricks
setblock ~16 ~8 ~26 minecraft:stone_bricks
setblock ~17 ~8 ~26 minecraft:cracked_stone_bricks
setblock ~19 ~8 ~26 minecraft:stone_bricks
setblock ~22 ~8 ~0 minecraft:cracked_deepslate_bricks
setblock ~22 ~8 ~26 minecraft:stone_bricks
fill ~23 ~8 ~0 ~25 ~8 ~0 minecraft:deepslate_bricks
fill ~23 ~8 ~26 ~24 ~8 ~26 minecraft:cracked_stone_bricks
setblock ~24 ~8 ~2 minecraft:lantern[hanging=false]
setblock ~24 ~8 ~24 minecraft:lantern[hanging=false]
setblock ~25 ~8 ~26 minecraft:stone_bricks
setblock ~28 ~8 ~26 minecraft:cracked_deepslate_bricks
fill ~29 ~8 ~26 ~31 ~8 ~26 minecraft:deepslate_bricks
fill ~30 ~8 ~0 ~31 ~8 ~0 minecraft:stone_bricks
setblock ~33 ~8 ~2 minecraft:lantern[hanging=false]
setblock ~33 ~8 ~24 minecraft:lantern[hanging=false]
setblock ~34 ~8 ~0 minecraft:stone_bricks
setblock ~34 ~8 ~26 minecraft:cracked_stone_bricks
setblock ~35 ~8 ~0 minecraft:cracked_stone_bricks
setblock ~35 ~8 ~26 minecraft:stone_bricks
fill ~36 ~8 ~0 ~37 ~8 ~0 minecraft:stone_bricks
setblock ~36 ~8 ~26 minecraft:cracked_stone_bricks
setblock ~37 ~8 ~26 minecraft:stone_bricks
setblock ~44 ~8 ~10 minecraft:lantern[hanging=false]
setblock ~44 ~8 ~16 minecraft:lantern[hanging=false]
setblock ~46 ~8 ~13 minecraft:stone_bricks
setblock ~46 ~8 ~16 minecraft:cracked_stone_bricks
setblock ~46 ~8 ~17 minecraft:stone_bricks
setblock ~46 ~8 ~18 minecraft:cracked_stone_bricks
setblock ~46 ~8 ~19 minecraft:stone_bricks
setblock ~0 ~9 ~7 minecraft:stone_brick_slab[type=bottom]
fill ~0 ~9 ~10 ~0 ~9 ~13 minecraft:stone_brick_slab[type=bottom]
fill ~0 ~9 ~16 ~0 ~9 ~19 minecraft:stone_brick_slab[type=bottom]
setblock ~7 ~9 ~0 minecraft:stone_brick_slab[type=bottom]
setblock ~7 ~9 ~26 minecraft:stone_brick_slab[type=bottom]
fill ~10 ~9 ~0 ~13 ~9 ~0 minecraft:stone_brick_slab[type=bottom]
fill ~10 ~9 ~26 ~13 ~9 ~26 minecraft:stone_brick_slab[type=bottom]
fill ~16 ~9 ~0 ~19 ~9 ~0 minecraft:stone_brick_slab[type=bottom]
fill ~16 ~9 ~26 ~19 ~9 ~26 minecraft:stone_brick_slab[type=bottom]
fill ~22 ~9 ~0 ~25 ~9 ~0 minecraft:stone_brick_slab[type=bottom]
fill ~22 ~9 ~26 ~25 ~9 ~26 minecraft:stone_brick_slab[type=bottom]
fill ~28 ~9 ~0 ~31 ~9 ~0 minecraft:stone_brick_slab[type=bottom]
fill ~28 ~9 ~26 ~31 ~9 ~26 minecraft:stone_brick_slab[type=bottom]
fill ~34 ~9 ~0 ~37 ~9 ~0 minecraft:stone_brick_slab[type=bottom]
fill ~34 ~9 ~26 ~37 ~9 ~26 minecraft:stone_brick_slab[type=bottom]
setblock ~46 ~9 ~7 minecraft:stone_brick_slab[type=bottom]
fill ~46 ~9 ~10 ~46 ~9 ~13 minecraft:stone_brick_slab[type=bottom]
fill ~46 ~9 ~16 ~46 ~9 ~19 minecraft:stone_brick_slab[type=bottom]

# the stairs up to the wall-walk on the inner faces of the long walls: six steps east of the gates on the west bank (x 12..17) and west of them on the east bank (x 34..29), on wedges of cobble
setblock ~12 ~1 ~3 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~12 ~1 ~23 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~13 ~1 ~3 minecraft:cobblestone
fill ~13 ~1 ~23 ~14 ~1 ~23 minecraft:cobblestone
setblock ~14 ~1 ~3 minecraft:andesite
fill ~15 ~1 ~3 ~15 ~3 ~3 minecraft:cobblestone
fill ~15 ~1 ~23 ~15 ~2 ~23 minecraft:andesite
fill ~16 ~1 ~3 ~17 ~1 ~3 minecraft:mossy_cobblestone
setblock ~16 ~1 ~23 minecraft:mossy_cobblestone
fill ~17 ~1 ~23 ~17 ~3 ~23 minecraft:cobblestone
setblock ~29 ~1 ~3 minecraft:andesite
fill ~29 ~1 ~23 ~29 ~4 ~23 minecraft:cobblestone
fill ~30 ~1 ~3 ~32 ~1 ~3 minecraft:cobblestone
fill ~30 ~1 ~23 ~30 ~2 ~23 minecraft:mossy_cobblestone
fill ~31 ~1 ~23 ~31 ~2 ~23 minecraft:cobblestone
fill ~32 ~1 ~23 ~33 ~1 ~23 minecraft:mossy_cobblestone
setblock ~33 ~1 ~3 minecraft:mossy_cobblestone
setblock ~34 ~1 ~3 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~34 ~1 ~23 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~13 ~2 ~3 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~13 ~2 ~23 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~14 ~2 ~3 minecraft:cobblestone
setblock ~14 ~2 ~23 minecraft:mossy_cobblestone
fill ~16 ~2 ~3 ~17 ~3 ~3 minecraft:cobblestone
fill ~16 ~2 ~23 ~16 ~4 ~23 minecraft:cobblestone
fill ~29 ~2 ~3 ~29 ~4 ~3 minecraft:cobblestone
fill ~30 ~2 ~3 ~30 ~4 ~3 minecraft:mossy_cobblestone
setblock ~31 ~2 ~3 minecraft:cobblestone
setblock ~32 ~2 ~3 minecraft:andesite
setblock ~32 ~2 ~23 minecraft:cobblestone
setblock ~33 ~2 ~3 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~33 ~2 ~23 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~14 ~3 ~3 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~14 ~3 ~23 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~15 ~3 ~23 minecraft:cobblestone
fill ~30 ~3 ~23 ~30 ~4 ~23 minecraft:cobblestone
setblock ~31 ~3 ~3 minecraft:andesite
setblock ~31 ~3 ~23 minecraft:andesite
setblock ~32 ~3 ~3 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~32 ~3 ~23 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~15 ~4 ~3 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~15 ~4 ~23 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~16 ~4 ~3 minecraft:andesite
setblock ~17 ~4 ~3 minecraft:mossy_cobblestone
setblock ~17 ~4 ~23 minecraft:mossy_cobblestone
setblock ~31 ~4 ~3 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~31 ~4 ~23 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~16 ~5 ~3 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~16 ~5 ~23 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~17 ~5 ~3 minecraft:andesite
setblock ~17 ~5 ~23 minecraft:cobblestone
setblock ~29 ~5 ~3 minecraft:andesite
setblock ~29 ~5 ~23 minecraft:andesite
setblock ~30 ~5 ~3 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~30 ~5 ~23 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~17 ~6 ~3 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~17 ~6 ~23 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~29 ~6 ~3 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~29 ~6 ~23 minecraft:stone_brick_stairs[facing=west,half=bottom]

# corner tower at 0,0: walls y 1..11 with dark quoins, moss low
setblock ~0 ~1 ~0 minecraft:stone_bricks
fill ~0 ~1 ~6 ~0 ~2 ~6 minecraft:polished_blackstone_bricks
fill ~1 ~1 ~6 ~2 ~1 ~6 minecraft:deepslate_bricks
setblock ~3 ~1 ~6 minecraft:mossy_stone_bricks
setblock ~5 ~1 ~6 minecraft:stone_bricks
fill ~6 ~1 ~0 ~6 ~2 ~0 minecraft:cracked_deepslate_bricks
fill ~6 ~1 ~1 ~6 ~1 ~3 minecraft:deepslate_bricks
fill ~6 ~1 ~4 ~6 ~1 ~5 minecraft:stone_bricks
fill ~6 ~1 ~6 ~6 ~2 ~6 minecraft:deepslate_bricks
fill ~0 ~2 ~0 ~0 ~2 ~2 minecraft:cracked_deepslate_bricks
setblock ~0 ~2 ~3 minecraft:deepslate_bricks
fill ~0 ~2 ~4 ~0 ~4 ~4 minecraft:cracked_deepslate_bricks
fill ~0 ~2 ~5 ~0 ~4 ~5 minecraft:deepslate_bricks
fill ~1 ~2 ~0 ~4 ~2 ~0 minecraft:mossy_stone_bricks
fill ~1 ~2 ~6 ~2 ~2 ~6 minecraft:cracked_deepslate_bricks
setblock ~3 ~2 ~6 minecraft:cracked_stone_bricks
fill ~5 ~2 ~0 ~5 ~3 ~0 minecraft:cracked_deepslate_bricks
fill ~5 ~2 ~6 ~5 ~4 ~6 minecraft:mossy_stone_bricks
fill ~6 ~2 ~1 ~6 ~3 ~3 minecraft:stone_bricks
fill ~6 ~2 ~4 ~6 ~3 ~5 minecraft:mossy_stone_bricks
fill ~0 ~3 ~0 ~1 ~3 ~0 minecraft:deepslate_bricks
fill ~0 ~3 ~1 ~0 ~3 ~2 minecraft:deepslate_bricks
fill ~0 ~3 ~6 ~1 ~4 ~6 minecraft:deepslate_bricks
fill ~2 ~3 ~0 ~2 ~4 ~0 minecraft:mossy_stone_bricks
setblock ~2 ~3 ~6 minecraft:cracked_deepslate_bricks
fill ~3 ~3 ~6 ~3 ~6 ~6 minecraft:stone_bricks
setblock ~4 ~3 ~0 minecraft:mossy_stone_bricks
setblock ~6 ~3 ~0 minecraft:polished_blackstone_bricks
setblock ~6 ~3 ~6 minecraft:cracked_deepslate_bricks
setblock ~0 ~4 ~0 minecraft:deepslate_bricks
fill ~0 ~4 ~1 ~0 ~5 ~2 minecraft:stone_bricks
setblock ~1 ~4 ~0 minecraft:cracked_stone_bricks
fill ~2 ~4 ~6 ~2 ~6 ~6 minecraft:stone_bricks
setblock ~4 ~4 ~0 minecraft:deepslate_bricks
setblock ~4 ~4 ~6 minecraft:cracked_stone_bricks
fill ~5 ~4 ~0 ~5 ~6 ~0 minecraft:stone_bricks
setblock ~6 ~4 ~0 minecraft:cracked_deepslate_bricks
fill ~6 ~4 ~1 ~6 ~6 ~1 minecraft:stone_bricks
setblock ~6 ~4 ~2 minecraft:cracked_stone_bricks
fill ~6 ~4 ~3 ~6 ~7 ~4 minecraft:stone_bricks
setblock ~6 ~4 ~5 minecraft:mossy_stone_bricks
fill ~6 ~4 ~6 ~6 ~5 ~6 minecraft:deepslate_bricks
setblock ~0 ~5 ~0 minecraft:cracked_deepslate_bricks
fill ~0 ~5 ~3 ~0 ~5 ~5 minecraft:stone_bricks
setblock ~0 ~5 ~6 minecraft:cracked_deepslate_bricks
fill ~1 ~5 ~0 ~4 ~6 ~0 minecraft:stone_bricks
fill ~1 ~5 ~6 ~1 ~6 ~6 minecraft:stone_bricks
fill ~4 ~5 ~6 ~5 ~5 ~6 minecraft:stone_bricks
setblock ~6 ~5 ~0 minecraft:deepslate_bricks
fill ~6 ~5 ~2 ~6 ~6 ~2 minecraft:stone_bricks
fill ~6 ~5 ~5 ~6 ~8 ~5 minecraft:deepslate_bricks
setblock ~0 ~6 ~0 minecraft:deepslate_bricks
fill ~0 ~6 ~1 ~0 ~6 ~2 minecraft:cracked_stone_bricks
setblock ~0 ~6 ~3 minecraft:stone_bricks
fill ~0 ~6 ~4 ~0 ~6 ~5 minecraft:cracked_stone_bricks
setblock ~0 ~6 ~6 minecraft:deepslate_bricks
fill ~4 ~6 ~6 ~4 ~7 ~6 minecraft:cracked_stone_bricks
fill ~5 ~6 ~6 ~5 ~7 ~6 minecraft:deepslate_bricks
setblock ~6 ~6 ~0 minecraft:polished_blackstone_bricks
setblock ~6 ~6 ~6 minecraft:cracked_deepslate_bricks
fill ~0 ~7 ~0 ~0 ~8 ~0 minecraft:cracked_deepslate_bricks
fill ~0 ~7 ~6 ~0 ~8 ~6 minecraft:polished_blackstone_bricks
setblock ~3 ~7 ~6 minecraft:cracked_stone_bricks
setblock ~6 ~7 ~0 minecraft:deepslate_bricks
setblock ~6 ~7 ~6 minecraft:stone_bricks
fill ~0 ~8 ~1 ~0 ~9 ~1 minecraft:deepslate_bricks
setblock ~0 ~8 ~2 minecraft:cracked_deepslate_bricks
fill ~0 ~8 ~3 ~0 ~8 ~4 minecraft:deepslate_bricks
setblock ~0 ~8 ~5 minecraft:cracked_deepslate_bricks
fill ~1 ~8 ~0 ~3 ~8 ~0 minecraft:deepslate_bricks
setblock ~3 ~8 ~6 minecraft:cracked_deepslate_bricks
setblock ~4 ~8 ~0 minecraft:cracked_deepslate_bricks
fill ~4 ~8 ~6 ~5 ~11 ~6 minecraft:stone_bricks
fill ~5 ~8 ~0 ~5 ~10 ~0 minecraft:deepslate_bricks
setblock ~6 ~8 ~0 minecraft:polished_blackstone_bricks
setblock ~6 ~8 ~3 minecraft:cracked_stone_bricks
fill ~6 ~8 ~4 ~6 ~9 ~4 minecraft:stone_bricks
setblock ~6 ~8 ~6 minecraft:deepslate_bricks
setblock ~0 ~9 ~0 minecraft:stone_bricks
setblock ~0 ~9 ~2 minecraft:deepslate_bricks
fill ~0 ~9 ~4 ~0 ~10 ~6 minecraft:deepslate_bricks
fill ~1 ~9 ~0 ~2 ~9 ~0 minecraft:deepslate_bricks
setblock ~1 ~9 ~6 minecraft:cracked_deepslate_bricks
fill ~2 ~9 ~6 ~2 ~10 ~6 minecraft:deepslate_bricks
fill ~3 ~9 ~6 ~3 ~11 ~6 minecraft:stone_bricks
fill ~4 ~9 ~0 ~4 ~10 ~0 minecraft:deepslate_bricks
fill ~6 ~9 ~0 ~6 ~10 ~0 minecraft:deepslate_bricks
fill ~6 ~9 ~1 ~6 ~11 ~3 minecraft:stone_bricks
setblock ~6 ~9 ~5 minecraft:stone_bricks
setblock ~6 ~9 ~6 minecraft:cracked_deepslate_bricks
setblock ~0 ~10 ~0 minecraft:polished_blackstone_bricks
setblock ~0 ~10 ~1 minecraft:stone_bricks
setblock ~0 ~10 ~2 minecraft:cracked_stone_bricks
setblock ~1 ~10 ~0 minecraft:stone_bricks
fill ~1 ~10 ~6 ~1 ~11 ~6 minecraft:deepslate_bricks
setblock ~2 ~10 ~0 minecraft:deepslate_bricks
fill ~6 ~10 ~4 ~6 ~10 ~5 minecraft:cracked_stone_bricks
setblock ~6 ~10 ~6 minecraft:deepslate_bricks
setblock ~0 ~11 ~0 minecraft:deepslate_bricks
setblock ~0 ~11 ~6 minecraft:deepslate_bricks
setblock ~2 ~11 ~6 minecraft:stone_bricks
setblock ~6 ~11 ~0 minecraft:polished_blackstone_bricks
fill ~6 ~11 ~4 ~6 ~11 ~5 minecraft:stone_bricks
setblock ~6 ~11 ~6 minecraft:cracked_deepslate_bricks

# the plinth lip on the two outer faces, the string course at y 7 under the high slits
fill ~0 ~1 ~1 ~0 ~1 ~5 minecraft:cobblestone_stairs[facing=east,half=bottom]
fill ~1 ~1 ~0 ~5 ~1 ~0 minecraft:cobblestone_stairs[facing=south,half=bottom]
fill ~0 ~7 ~1 ~0 ~7 ~5 minecraft:chiseled_stone_bricks
fill ~1 ~7 ~0 ~5 ~7 ~0 minecraft:chiseled_stone_bricks

# the floors: planks at y 6, flagstones at y 12 across the top
fill ~1 ~6 ~1 ~5 ~6 ~3 minecraft:spruce_planks
fill ~1 ~6 ~4 ~4 ~6 ~5 minecraft:spruce_planks
setblock ~5 ~6 ~5 minecraft:spruce_planks
setblock ~0 ~12 ~0 minecraft:cobblestone
fill ~0 ~12 ~1 ~0 ~12 ~2 minecraft:stone_bricks
fill ~0 ~12 ~3 ~0 ~12 ~4 minecraft:cracked_stone_bricks
fill ~0 ~12 ~5 ~2 ~12 ~5 minecraft:stone_bricks
fill ~0 ~12 ~6 ~1 ~12 ~6 minecraft:mossy_stone_bricks
fill ~1 ~12 ~0 ~2 ~12 ~0 minecraft:stone_bricks
setblock ~1 ~12 ~1 minecraft:mossy_stone_bricks
fill ~1 ~12 ~2 ~1 ~12 ~4 minecraft:cobblestone
fill ~2 ~12 ~1 ~4 ~12 ~3 minecraft:stone_bricks
setblock ~2 ~12 ~4 minecraft:cobblestone
fill ~2 ~12 ~6 ~3 ~12 ~6 minecraft:stone_bricks
fill ~3 ~12 ~0 ~4 ~12 ~0 minecraft:cracked_stone_bricks
setblock ~3 ~12 ~4 minecraft:stone_bricks
setblock ~3 ~12 ~5 minecraft:mossy_stone_bricks
fill ~4 ~12 ~4 ~4 ~12 ~6 minecraft:mossy_stone_bricks
fill ~5 ~12 ~0 ~5 ~12 ~1 minecraft:mossy_stone_bricks
setblock ~5 ~12 ~2 minecraft:cracked_stone_bricks
setblock ~5 ~12 ~3 minecraft:mossy_stone_bricks
fill ~5 ~12 ~5 ~6 ~12 ~5 minecraft:stone_bricks
setblock ~5 ~12 ~6 minecraft:cracked_stone_bricks
fill ~6 ~12 ~0 ~6 ~12 ~1 minecraft:stone_bricks
setblock ~6 ~12 ~2 minecraft:mossy_stone_bricks
setblock ~6 ~12 ~3 minecraft:stone_bricks
setblock ~6 ~12 ~4 minecraft:cracked_stone_bricks
setblock ~6 ~12 ~6 minecraft:stone_bricks

# the ladder up the inner face (u 5, v 4) from y 1 to y 13 through both floors
fill ~5 ~1 ~4 ~5 ~13 ~4 minecraft:ladder[facing=west]

# the door onto the courtyard (u 4, v 6), the openings from the wall-walks into the upper room, slits
fill ~4 ~1 ~6 ~4 ~2 ~6 minecraft:air
fill ~0 ~3 ~3 ~0 ~4 ~3 minecraft:air
fill ~3 ~3 ~0 ~3 ~4 ~0 minecraft:air
setblock ~4 ~3 ~6 minecraft:chiseled_stone_bricks
fill ~1 ~7 ~6 ~2 ~8 ~6 minecraft:air
fill ~6 ~7 ~1 ~6 ~8 ~2 minecraft:air
fill ~0 ~9 ~3 ~0 ~10 ~3 minecraft:air
fill ~3 ~9 ~0 ~3 ~10 ~0 minecraft:air

# the ground room: barrels and a lantern on a chain; the upper room: a barrel and a lantern
fill ~1 ~1 ~1 ~2 ~1 ~1 minecraft:barrel[facing=up]
setblock ~1 ~1 ~2 minecraft:barrel[facing=up]
setblock ~1 ~2 ~1 minecraft:barrel[facing=up]
setblock ~3 ~4 ~3 minecraft:lantern[hanging=true]
setblock ~3 ~5 ~3 minecraft:chain[axis=y]
setblock ~1 ~7 ~5 minecraft:barrel[facing=up]
setblock ~3 ~10 ~3 minecraft:lantern[hanging=true]
setblock ~3 ~11 ~3 minecraft:chain[axis=y]

# the corbel course at y 11 on the two outer faces: upside-down deepslate brick stairs under the rampart
fill ~0 ~11 ~1 ~0 ~11 ~5 minecraft:deepslate_brick_stairs[facing=east,half=top]
fill ~1 ~11 ~0 ~5 ~11 ~0 minecraft:deepslate_brick_stairs[facing=south,half=top]

# the parapet ring at y 13 with merlons at y 14 and lantern posts at the corners
fill ~0 ~13 ~0 ~0 ~14 ~0 minecraft:deepslate_bricks
setblock ~0 ~13 ~1 minecraft:cracked_deepslate_bricks
fill ~0 ~13 ~2 ~0 ~13 ~3 minecraft:deepslate_bricks
setblock ~0 ~13 ~4 minecraft:cracked_deepslate_bricks
setblock ~0 ~13 ~5 minecraft:deepslate_bricks
setblock ~0 ~13 ~6 minecraft:cracked_deepslate_bricks
fill ~1 ~13 ~0 ~2 ~13 ~0 minecraft:cracked_deepslate_bricks
fill ~1 ~13 ~6 ~5 ~13 ~6 minecraft:deepslate_bricks
fill ~3 ~13 ~0 ~6 ~13 ~0 minecraft:deepslate_bricks
fill ~6 ~13 ~1 ~6 ~13 ~3 minecraft:deepslate_bricks
setblock ~6 ~13 ~4 minecraft:cracked_deepslate_bricks
fill ~6 ~13 ~5 ~6 ~13 ~6 minecraft:deepslate_bricks
setblock ~0 ~14 ~2 minecraft:deepslate_bricks
setblock ~0 ~14 ~4 minecraft:deepslate_bricks
setblock ~0 ~14 ~6 minecraft:deepslate_bricks
setblock ~2 ~14 ~0 minecraft:cracked_deepslate_bricks
setblock ~2 ~14 ~6 minecraft:stone_bricks
setblock ~4 ~14 ~0 minecraft:deepslate_bricks
setblock ~4 ~14 ~6 minecraft:cracked_stone_bricks
setblock ~6 ~14 ~0 minecraft:deepslate_bricks
setblock ~6 ~14 ~2 minecraft:stone_bricks
setblock ~6 ~14 ~4 minecraft:stone_bricks
setblock ~6 ~14 ~6 minecraft:deepslate_bricks
setblock ~0 ~15 ~0 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~0 ~15 ~6 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~6 ~15 ~0 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~6 ~15 ~6 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~0 ~16 ~0 minecraft:lantern[hanging=false]
setblock ~0 ~16 ~6 minecraft:lantern[hanging=false]
setblock ~6 ~16 ~0 minecraft:lantern[hanging=false]
setblock ~6 ~16 ~6 minecraft:lantern[hanging=false]

# corner tower at 40,0: walls y 1..11 with dark quoins, moss low
setblock ~40 ~1 ~0 minecraft:cracked_deepslate_bricks
fill ~40 ~1 ~1 ~40 ~3 ~5 minecraft:mossy_stone_bricks
fill ~40 ~1 ~6 ~40 ~9 ~6 minecraft:deepslate_bricks
fill ~41 ~1 ~6 ~41 ~3 ~6 minecraft:mossy_stone_bricks
setblock ~44 ~1 ~6 minecraft:stone_bricks
setblock ~45 ~1 ~6 minecraft:cracked_stone_bricks
fill ~46 ~1 ~0 ~46 ~2 ~0 minecraft:deepslate_bricks
setblock ~46 ~1 ~6 minecraft:stone_bricks
fill ~40 ~2 ~0 ~40 ~6 ~0 minecraft:deepslate_bricks
fill ~41 ~2 ~0 ~44 ~2 ~0 minecraft:mossy_stone_bricks
fill ~43 ~2 ~6 ~45 ~3 ~6 minecraft:mossy_stone_bricks
setblock ~45 ~2 ~0 minecraft:stone_bricks
fill ~46 ~2 ~1 ~46 ~2 ~2 minecraft:deepslate_bricks
setblock ~46 ~2 ~3 minecraft:cracked_deepslate_bricks
fill ~46 ~2 ~4 ~46 ~3 ~5 minecraft:mossy_stone_bricks
fill ~46 ~2 ~6 ~46 ~3 ~6 minecraft:polished_blackstone_bricks
fill ~41 ~3 ~0 ~42 ~3 ~0 minecraft:mossy_stone_bricks
fill ~44 ~3 ~0 ~45 ~4 ~0 minecraft:mossy_stone_bricks
setblock ~46 ~3 ~0 minecraft:polished_blackstone_bricks
setblock ~46 ~3 ~1 minecraft:deepslate_bricks
fill ~46 ~3 ~2 ~46 ~5 ~2 minecraft:mossy_stone_bricks
fill ~40 ~4 ~1 ~40 ~4 ~3 minecraft:deepslate_bricks
setblock ~40 ~4 ~4 minecraft:cracked_deepslate_bricks
fill ~40 ~4 ~5 ~40 ~5 ~5 minecraft:deepslate_bricks
fill ~41 ~4 ~0 ~42 ~4 ~0 minecraft:deepslate_bricks
fill ~41 ~4 ~6 ~44 ~4 ~6 minecraft:deepslate_bricks
setblock ~45 ~4 ~6 minecraft:cracked_deepslate_bricks
setblock ~46 ~4 ~0 minecraft:deepslate_bricks
fill ~46 ~4 ~1 ~46 ~5 ~1 minecraft:mossy_stone_bricks
setblock ~46 ~4 ~4 minecraft:mossy_stone_bricks
fill ~46 ~4 ~5 ~46 ~6 ~5 minecraft:cracked_deepslate_bricks
fill ~46 ~4 ~6 ~46 ~6 ~6 minecraft:deepslate_bricks
fill ~40 ~5 ~1 ~40 ~6 ~4 minecraft:stone_bricks
fill ~41 ~5 ~0 ~42 ~5 ~0 minecraft:stone_bricks
setblock ~41 ~5 ~6 minecraft:cracked_deepslate_bricks
fill ~42 ~5 ~6 ~42 ~6 ~6 minecraft:deepslate_bricks
fill ~43 ~5 ~0 ~44 ~6 ~0 minecraft:deepslate_bricks
fill ~43 ~5 ~6 ~44 ~5 ~6 minecraft:cracked_deepslate_bricks
setblock ~45 ~5 ~0 minecraft:mossy_stone_bricks
setblock ~45 ~5 ~6 minecraft:deepslate_bricks
fill ~46 ~5 ~0 ~46 ~6 ~0 minecraft:cracked_deepslate_bricks
setblock ~46 ~5 ~3 minecraft:mossy_stone_bricks
fill ~46 ~5 ~4 ~46 ~6 ~4 minecraft:deepslate_bricks
fill ~40 ~6 ~5 ~40 ~11 ~5 minecraft:stone_bricks
fill ~41 ~6 ~0 ~42 ~6 ~0 minecraft:cracked_stone_bricks
setblock ~41 ~6 ~6 minecraft:deepslate_bricks
setblock ~43 ~6 ~6 minecraft:deepslate_bricks
fill ~44 ~6 ~6 ~45 ~6 ~6 minecraft:cracked_deepslate_bricks
setblock ~45 ~6 ~0 minecraft:deepslate_bricks
fill ~46 ~6 ~1 ~46 ~6 ~3 minecraft:deepslate_bricks
setblock ~40 ~7 ~0 minecraft:polished_blackstone_bricks
fill ~40 ~7 ~3 ~40 ~7 ~4 minecraft:cracked_stone_bricks
fill ~41 ~7 ~6 ~41 ~8 ~6 minecraft:stone_bricks
setblock ~42 ~7 ~6 minecraft:cracked_stone_bricks
fill ~43 ~7 ~6 ~43 ~10 ~6 minecraft:stone_bricks
fill ~46 ~7 ~0 ~46 ~8 ~0 minecraft:deepslate_bricks
fill ~46 ~7 ~6 ~46 ~9 ~6 minecraft:cracked_deepslate_bricks
setblock ~40 ~8 ~0 minecraft:cracked_deepslate_bricks
fill ~40 ~8 ~3 ~40 ~11 ~4 minecraft:stone_bricks
fill ~41 ~8 ~0 ~41 ~10 ~0 minecraft:stone_bricks
setblock ~42 ~8 ~0 minecraft:cracked_stone_bricks
fill ~42 ~8 ~6 ~42 ~10 ~6 minecraft:stone_bricks
fill ~43 ~8 ~0 ~45 ~8 ~0 minecraft:stone_bricks
fill ~46 ~8 ~1 ~46 ~8 ~4 minecraft:stone_bricks
setblock ~46 ~8 ~5 minecraft:cracked_stone_bricks
fill ~40 ~9 ~0 ~40 ~11 ~0 minecraft:deepslate_bricks
fill ~40 ~9 ~1 ~40 ~11 ~2 minecraft:stone_bricks
setblock ~41 ~9 ~6 minecraft:cracked_stone_bricks
fill ~42 ~9 ~0 ~42 ~10 ~0 minecraft:stone_bricks
setblock ~44 ~9 ~0 minecraft:cracked_stone_bricks
setblock ~44 ~9 ~6 minecraft:cracked_stone_bricks
fill ~45 ~9 ~0 ~45 ~10 ~0 minecraft:stone_bricks
setblock ~45 ~9 ~6 minecraft:stone_bricks
setblock ~46 ~9 ~0 minecraft:cracked_deepslate_bricks
fill ~46 ~9 ~1 ~46 ~9 ~2 minecraft:stone_bricks
fill ~46 ~9 ~4 ~46 ~9 ~5 minecraft:stone_bricks
fill ~40 ~10 ~6 ~40 ~11 ~6 minecraft:cracked_deepslate_bricks
fill ~41 ~10 ~6 ~41 ~11 ~6 minecraft:stone_bricks
setblock ~44 ~10 ~0 minecraft:stone_bricks
fill ~44 ~10 ~6 ~45 ~11 ~6 minecraft:deepslate_bricks
fill ~46 ~10 ~0 ~46 ~10 ~2 minecraft:deepslate_bricks
setblock ~46 ~10 ~4 minecraft:cracked_deepslate_bricks
fill ~46 ~10 ~5 ~46 ~10 ~6 minecraft:deepslate_bricks
setblock ~42 ~11 ~6 minecraft:cracked_stone_bricks
setblock ~43 ~11 ~6 minecraft:deepslate_bricks
setblock ~46 ~11 ~0 minecraft:deepslate_bricks
setblock ~46 ~11 ~6 minecraft:deepslate_bricks

# the plinth lip on the two outer faces, the string course at y 7 under the high slits
fill ~41 ~1 ~0 ~45 ~1 ~0 minecraft:cobblestone_stairs[facing=south,half=bottom]
fill ~46 ~1 ~1 ~46 ~1 ~5 minecraft:cobblestone_stairs[facing=west,half=bottom]
fill ~41 ~7 ~0 ~45 ~7 ~0 minecraft:chiseled_stone_bricks
fill ~46 ~7 ~1 ~46 ~7 ~5 minecraft:chiseled_stone_bricks

# the floors: planks at y 6, flagstones at y 12 across the top
fill ~41 ~6 ~1 ~45 ~6 ~3 minecraft:spruce_planks
setblock ~41 ~6 ~5 minecraft:spruce_planks
fill ~42 ~6 ~4 ~45 ~6 ~5 minecraft:spruce_planks
fill ~40 ~12 ~0 ~42 ~12 ~0 minecraft:stone_bricks
setblock ~40 ~12 ~1 minecraft:stone_bricks
setblock ~40 ~12 ~2 minecraft:cobblestone
fill ~40 ~12 ~3 ~41 ~12 ~3 minecraft:cracked_stone_bricks
setblock ~40 ~12 ~4 minecraft:mossy_stone_bricks
fill ~40 ~12 ~5 ~41 ~12 ~5 minecraft:stone_bricks
setblock ~40 ~12 ~6 minecraft:stone_bricks
setblock ~41 ~12 ~1 minecraft:mossy_stone_bricks
setblock ~41 ~12 ~2 minecraft:stone_bricks
setblock ~41 ~12 ~6 minecraft:cracked_stone_bricks
fill ~42 ~12 ~1 ~43 ~12 ~1 minecraft:stone_bricks
setblock ~42 ~12 ~2 minecraft:mossy_stone_bricks
fill ~42 ~12 ~3 ~42 ~12 ~4 minecraft:stone_bricks
setblock ~42 ~12 ~5 minecraft:cobblestone
setblock ~42 ~12 ~6 minecraft:stone_bricks
setblock ~43 ~12 ~0 minecraft:mossy_stone_bricks
fill ~43 ~12 ~2 ~43 ~12 ~6 minecraft:stone_bricks
fill ~44 ~12 ~0 ~44 ~12 ~2 minecraft:stone_bricks
fill ~44 ~12 ~3 ~44 ~12 ~4 minecraft:cracked_stone_bricks
setblock ~44 ~12 ~5 minecraft:mossy_stone_bricks
setblock ~44 ~12 ~6 minecraft:stone_bricks
setblock ~45 ~12 ~0 minecraft:cobblestone
setblock ~45 ~12 ~1 minecraft:stone_bricks
setblock ~45 ~12 ~2 minecraft:cracked_stone_bricks
fill ~45 ~12 ~3 ~46 ~12 ~3 minecraft:mossy_stone_bricks
fill ~45 ~12 ~4 ~45 ~12 ~6 minecraft:stone_bricks
fill ~46 ~12 ~0 ~46 ~12 ~1 minecraft:stone_bricks
setblock ~46 ~12 ~2 minecraft:cobblestone
fill ~46 ~12 ~4 ~46 ~12 ~5 minecraft:cobblestone
setblock ~46 ~12 ~6 minecraft:cracked_stone_bricks

# the ladder up the inner face (u 5, v 4) from y 1 to y 13 through both floors
fill ~41 ~1 ~4 ~41 ~13 ~4 minecraft:ladder[facing=east]

# the door onto the courtyard (u 4, v 6), the openings from the wall-walks into the upper room, slits
fill ~42 ~1 ~6 ~42 ~2 ~6 minecraft:air
setblock ~42 ~3 ~6 minecraft:chiseled_stone_bricks
fill ~43 ~3 ~0 ~43 ~4 ~0 minecraft:air
fill ~46 ~3 ~3 ~46 ~4 ~3 minecraft:air
fill ~40 ~7 ~1 ~40 ~8 ~2 minecraft:air
fill ~44 ~7 ~6 ~45 ~8 ~6 minecraft:air
fill ~43 ~9 ~0 ~43 ~10 ~0 minecraft:air
fill ~46 ~9 ~3 ~46 ~10 ~3 minecraft:air

# the ground room: barrels and a lantern on a chain; the upper room: a barrel and a lantern
fill ~44 ~1 ~1 ~45 ~1 ~1 minecraft:barrel[facing=up]
setblock ~45 ~1 ~2 minecraft:barrel[facing=up]
setblock ~45 ~2 ~1 minecraft:barrel[facing=up]
setblock ~43 ~4 ~3 minecraft:lantern[hanging=true]
setblock ~43 ~5 ~3 minecraft:chain[axis=y]
setblock ~45 ~7 ~5 minecraft:barrel[facing=up]
setblock ~43 ~10 ~3 minecraft:lantern[hanging=true]
setblock ~43 ~11 ~3 minecraft:chain[axis=y]

# the corbel course at y 11 on the two outer faces: upside-down deepslate brick stairs under the rampart
fill ~41 ~11 ~0 ~45 ~11 ~0 minecraft:deepslate_brick_stairs[facing=south,half=top]
fill ~46 ~11 ~1 ~46 ~11 ~5 minecraft:deepslate_brick_stairs[facing=west,half=top]

# the parapet ring at y 13 with merlons at y 14 and lantern posts at the corners
fill ~40 ~13 ~0 ~42 ~13 ~0 minecraft:stone_bricks
fill ~40 ~13 ~1 ~40 ~13 ~6 minecraft:stone_bricks
fill ~41 ~13 ~6 ~44 ~13 ~6 minecraft:stone_bricks
setblock ~43 ~13 ~0 minecraft:cracked_stone_bricks
fill ~44 ~13 ~0 ~45 ~13 ~0 minecraft:stone_bricks
setblock ~45 ~13 ~6 minecraft:cracked_stone_bricks
setblock ~46 ~13 ~0 minecraft:cracked_stone_bricks
fill ~46 ~13 ~1 ~46 ~13 ~6 minecraft:stone_bricks
setblock ~40 ~14 ~0 minecraft:deepslate_bricks
setblock ~40 ~14 ~2 minecraft:stone_bricks
setblock ~40 ~14 ~4 minecraft:stone_bricks
setblock ~40 ~14 ~6 minecraft:deepslate_bricks
setblock ~42 ~14 ~0 minecraft:stone_bricks
setblock ~42 ~14 ~6 minecraft:cracked_stone_bricks
setblock ~44 ~14 ~0 minecraft:stone_bricks
setblock ~44 ~14 ~6 minecraft:deepslate_bricks
setblock ~46 ~14 ~0 minecraft:deepslate_bricks
setblock ~46 ~14 ~2 minecraft:stone_bricks
setblock ~46 ~14 ~4 minecraft:deepslate_bricks
setblock ~46 ~14 ~6 minecraft:deepslate_bricks
setblock ~40 ~15 ~0 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~40 ~15 ~6 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~46 ~15 ~0 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~46 ~15 ~6 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~40 ~16 ~0 minecraft:lantern[hanging=false]
setblock ~40 ~16 ~6 minecraft:lantern[hanging=false]
setblock ~46 ~16 ~0 minecraft:lantern[hanging=false]
setblock ~46 ~16 ~6 minecraft:lantern[hanging=false]

# corner tower at 0,20: walls y 1..11 with dark quoins, moss low
fill ~0 ~1 ~20 ~0 ~2 ~20 minecraft:polished_blackstone_bricks
fill ~0 ~1 ~26 ~0 ~3 ~26 minecraft:deepslate_bricks
setblock ~1 ~1 ~20 minecraft:stone_bricks
setblock ~2 ~1 ~20 minecraft:cracked_stone_bricks
fill ~3 ~1 ~20 ~3 ~5 ~20 minecraft:mossy_stone_bricks
fill ~5 ~1 ~20 ~6 ~1 ~20 minecraft:deepslate_bricks
fill ~6 ~1 ~21 ~6 ~1 ~22 minecraft:deepslate_bricks
setblock ~6 ~1 ~23 minecraft:cracked_deepslate_bricks
fill ~6 ~1 ~24 ~6 ~5 ~25 minecraft:mossy_stone_bricks
fill ~6 ~1 ~26 ~6 ~2 ~26 minecraft:deepslate_bricks
fill ~0 ~2 ~21 ~0 ~2 ~22 minecraft:stone_bricks
setblock ~0 ~2 ~23 minecraft:cracked_stone_bricks
fill ~0 ~2 ~24 ~0 ~3 ~24 minecraft:stone_bricks
fill ~0 ~2 ~25 ~0 ~3 ~25 minecraft:mossy_stone_bricks
setblock ~1 ~2 ~20 minecraft:cracked_stone_bricks
fill ~1 ~2 ~26 ~5 ~2 ~26 minecraft:deepslate_bricks
fill ~2 ~2 ~20 ~2 ~6 ~20 minecraft:stone_bricks
fill ~5 ~2 ~20 ~5 ~4 ~20 minecraft:mossy_stone_bricks
setblock ~6 ~2 ~20 minecraft:stone_bricks
fill ~6 ~2 ~21 ~6 ~3 ~21 minecraft:deepslate_bricks
fill ~6 ~2 ~22 ~6 ~4 ~23 minecraft:mossy_stone_bricks
setblock ~0 ~3 ~20 minecraft:deepslate_bricks
setblock ~0 ~3 ~21 minecraft:cracked_stone_bricks
fill ~0 ~3 ~22 ~0 ~5 ~22 minecraft:stone_bricks
setblock ~1 ~3 ~20 minecraft:stone_bricks
fill ~1 ~3 ~26 ~2 ~3 ~26 minecraft:deepslate_bricks
fill ~4 ~3 ~26 ~5 ~3 ~26 minecraft:deepslate_bricks
setblock ~6 ~3 ~20 minecraft:cracked_deepslate_bricks
setblock ~6 ~3 ~26 minecraft:polished_blackstone_bricks
fill ~0 ~4 ~20 ~0 ~4 ~21 minecraft:stone_bricks
setblock ~0 ~4 ~24 minecraft:cracked_stone_bricks
fill ~0 ~4 ~25 ~0 ~6 ~25 minecraft:stone_bricks
setblock ~0 ~4 ~26 minecraft:polished_blackstone_bricks
setblock ~1 ~4 ~20 minecraft:cracked_stone_bricks
fill ~1 ~4 ~26 ~2 ~6 ~26 minecraft:stone_bricks
fill ~4 ~4 ~20 ~4 ~5 ~20 minecraft:mossy_stone_bricks
fill ~4 ~4 ~26 ~4 ~6 ~26 minecraft:stone_bricks
fill ~5 ~4 ~26 ~6 ~4 ~26 minecraft:deepslate_bricks
fill ~6 ~4 ~20 ~6 ~6 ~20 minecraft:deepslate_bricks
setblock ~6 ~4 ~21 minecraft:cracked_stone_bricks
setblock ~0 ~5 ~20 minecraft:cracked_deepslate_bricks
setblock ~0 ~5 ~21 minecraft:deepslate_bricks
fill ~0 ~5 ~23 ~0 ~6 ~24 minecraft:stone_bricks
fill ~0 ~5 ~26 ~0 ~7 ~26 minecraft:deepslate_bricks
fill ~1 ~5 ~20 ~1 ~6 ~20 minecraft:stone_bricks
setblock ~3 ~5 ~26 minecraft:cracked_stone_bricks
fill ~5 ~5 ~20 ~5 ~11 ~20 minecraft:stone_bricks
setblock ~5 ~5 ~26 minecraft:cracked_stone_bricks
fill ~6 ~5 ~21 ~6 ~7 ~22 minecraft:stone_bricks
setblock ~6 ~5 ~23 minecraft:mossy_stone_bricks
fill ~6 ~5 ~26 ~6 ~6 ~26 minecraft:deepslate_bricks
setblock ~0 ~6 ~20 minecraft:deepslate_bricks
setblock ~0 ~6 ~21 minecraft:cracked_deepslate_bricks
setblock ~0 ~6 ~22 minecraft:cracked_stone_bricks
fill ~3 ~6 ~20 ~4 ~6 ~20 minecraft:cracked_stone_bricks
setblock ~3 ~6 ~26 minecraft:stone_bricks
setblock ~5 ~6 ~26 minecraft:stone_bricks
setblock ~6 ~6 ~23 minecraft:cracked_stone_bricks
fill ~6 ~6 ~24 ~6 ~6 ~25 minecraft:stone_bricks
setblock ~0 ~7 ~20 minecraft:stone_bricks
fill ~3 ~7 ~20 ~3 ~8 ~20 minecraft:deepslate_bricks
setblock ~4 ~7 ~20 minecraft:cracked_stone_bricks
fill ~6 ~7 ~20 ~6 ~8 ~20 minecraft:cracked_deepslate_bricks
fill ~6 ~7 ~23 ~6 ~11 ~23 minecraft:stone_bricks
setblock ~6 ~7 ~26 minecraft:stone_bricks
fill ~0 ~8 ~20 ~0 ~8 ~23 minecraft:deepslate_bricks
fill ~0 ~8 ~24 ~0 ~8 ~26 minecraft:cracked_deepslate_bricks
fill ~1 ~8 ~26 ~5 ~8 ~26 minecraft:stone_bricks
setblock ~4 ~8 ~20 minecraft:deepslate_bricks
fill ~6 ~8 ~21 ~6 ~11 ~21 minecraft:stone_bricks
fill ~6 ~8 ~22 ~6 ~9 ~22 minecraft:cracked_stone_bricks
fill ~6 ~8 ~26 ~6 ~10 ~26 minecraft:cracked_deepslate_bricks
fill ~0 ~9 ~20 ~0 ~9 ~21 minecraft:cracked_deepslate_bricks
setblock ~0 ~9 ~22 minecraft:deepslate_bricks
fill ~0 ~9 ~24 ~0 ~9 ~26 minecraft:deepslate_bricks
setblock ~1 ~9 ~20 minecraft:deepslate_bricks
fill ~1 ~9 ~26 ~2 ~9 ~26 minecraft:stone_bricks
fill ~2 ~9 ~20 ~3 ~9 ~20 minecraft:cracked_deepslate_bricks
fill ~4 ~9 ~20 ~4 ~11 ~20 minecraft:stone_bricks
fill ~4 ~9 ~26 ~5 ~9 ~26 minecraft:stone_bricks
setblock ~6 ~9 ~20 minecraft:deepslate_bricks
fill ~6 ~9 ~24 ~6 ~9 ~25 minecraft:stone_bricks
setblock ~0 ~10 ~20 minecraft:deepslate_bricks
fill ~0 ~10 ~21 ~0 ~10 ~22 minecraft:stone_bricks
fill ~0 ~10 ~24 ~0 ~10 ~25 minecraft:stone_bricks
setblock ~0 ~10 ~26 minecraft:cracked_deepslate_bricks
fill ~1 ~10 ~20 ~3 ~10 ~20 minecraft:stone_bricks
setblock ~1 ~10 ~26 minecraft:cracked_stone_bricks
setblock ~2 ~10 ~26 minecraft:stone_bricks
setblock ~4 ~10 ~26 minecraft:stone_bricks
setblock ~5 ~10 ~26 minecraft:cracked_deepslate_bricks
fill ~6 ~10 ~20 ~6 ~11 ~20 minecraft:cracked_deepslate_bricks
fill ~6 ~10 ~22 ~6 ~11 ~22 minecraft:stone_bricks
setblock ~6 ~10 ~24 minecraft:deepslate_bricks
setblock ~6 ~10 ~25 minecraft:cracked_deepslate_bricks
fill ~0 ~11 ~20 ~1 ~11 ~20 minecraft:stone_bricks
setblock ~0 ~11 ~26 minecraft:deepslate_bricks
setblock ~2 ~11 ~20 minecraft:cracked_stone_bricks
setblock ~3 ~11 ~20 minecraft:stone_bricks
setblock ~6 ~11 ~24 minecraft:cracked_deepslate_bricks
fill ~6 ~11 ~25 ~6 ~11 ~26 minecraft:deepslate_bricks

# the plinth lip on the two outer faces, the string course at y 7 under the high slits
fill ~0 ~1 ~21 ~0 ~1 ~25 minecraft:cobblestone_stairs[facing=east,half=bottom]
fill ~1 ~1 ~26 ~5 ~1 ~26 minecraft:cobblestone_stairs[facing=north,half=bottom]
fill ~0 ~7 ~21 ~0 ~7 ~25 minecraft:chiseled_stone_bricks
fill ~1 ~7 ~26 ~5 ~7 ~26 minecraft:chiseled_stone_bricks

# the floors: planks at y 6, flagstones at y 12 across the top
fill ~1 ~6 ~21 ~5 ~6 ~21 minecraft:spruce_planks
fill ~1 ~6 ~22 ~4 ~6 ~25 minecraft:spruce_planks
fill ~5 ~6 ~23 ~5 ~6 ~25 minecraft:spruce_planks
fill ~0 ~12 ~20 ~0 ~12 ~26 minecraft:stone_bricks
setblock ~1 ~12 ~20 minecraft:cracked_stone_bricks
fill ~1 ~12 ~21 ~3 ~12 ~21 minecraft:stone_bricks
setblock ~1 ~12 ~22 minecraft:cobblestone
fill ~1 ~12 ~23 ~3 ~12 ~23 minecraft:stone_bricks
setblock ~1 ~12 ~24 minecraft:stone_bricks
setblock ~1 ~12 ~25 minecraft:mossy_stone_bricks
setblock ~1 ~12 ~26 minecraft:stone_bricks
fill ~2 ~12 ~20 ~6 ~12 ~20 minecraft:stone_bricks
fill ~2 ~12 ~22 ~4 ~12 ~22 minecraft:stone_bricks
setblock ~2 ~12 ~24 minecraft:cobblestone
setblock ~2 ~12 ~25 minecraft:stone_bricks
setblock ~2 ~12 ~26 minecraft:cracked_stone_bricks
setblock ~3 ~12 ~24 minecraft:mossy_stone_bricks
setblock ~3 ~12 ~25 minecraft:cobblestone
setblock ~3 ~12 ~26 minecraft:stone_bricks
setblock ~4 ~12 ~21 minecraft:cobblestone
fill ~4 ~12 ~23 ~4 ~12 ~24 minecraft:cracked_stone_bricks
fill ~4 ~12 ~25 ~4 ~12 ~26 minecraft:stone_bricks
setblock ~5 ~12 ~21 minecraft:stone_bricks
setblock ~5 ~12 ~23 minecraft:cobblestone
fill ~5 ~12 ~24 ~5 ~12 ~25 minecraft:stone_bricks
setblock ~5 ~12 ~26 minecraft:mossy_stone_bricks
setblock ~6 ~12 ~21 minecraft:cracked_stone_bricks
fill ~6 ~12 ~22 ~6 ~12 ~26 minecraft:stone_bricks

# the ladder up the inner face (u 5, v 4) from y 1 to y 13 through both floors
fill ~5 ~1 ~22 ~5 ~13 ~22 minecraft:ladder[facing=west]

# the door onto the courtyard (u 4, v 6), the openings from the wall-walks into the upper room, slits
fill ~4 ~1 ~20 ~4 ~2 ~20 minecraft:air
fill ~0 ~3 ~23 ~0 ~4 ~23 minecraft:air
fill ~3 ~3 ~26 ~3 ~4 ~26 minecraft:air
setblock ~4 ~3 ~20 minecraft:chiseled_stone_bricks
fill ~1 ~7 ~20 ~2 ~8 ~20 minecraft:air
fill ~6 ~7 ~24 ~6 ~8 ~25 minecraft:air
fill ~0 ~9 ~23 ~0 ~10 ~23 minecraft:air
fill ~3 ~9 ~26 ~3 ~10 ~26 minecraft:air

# the ground room: barrels and a lantern on a chain; the upper room: a barrel and a lantern
fill ~1 ~1 ~24 ~1 ~1 ~25 minecraft:barrel[facing=up]
setblock ~2 ~1 ~25 minecraft:barrel[facing=up]
setblock ~1 ~2 ~25 minecraft:barrel[facing=up]
setblock ~3 ~4 ~23 minecraft:lantern[hanging=true]
setblock ~3 ~5 ~23 minecraft:chain[axis=y]
setblock ~1 ~7 ~21 minecraft:barrel[facing=up]
setblock ~3 ~10 ~23 minecraft:lantern[hanging=true]
setblock ~3 ~11 ~23 minecraft:chain[axis=y]

# the corbel course at y 11 on the two outer faces: upside-down deepslate brick stairs under the rampart
fill ~0 ~11 ~21 ~0 ~11 ~25 minecraft:deepslate_brick_stairs[facing=east,half=top]
fill ~1 ~11 ~26 ~5 ~11 ~26 minecraft:deepslate_brick_stairs[facing=north,half=top]

# the parapet ring at y 13 with merlons at y 14 and lantern posts at the corners
fill ~0 ~13 ~20 ~2 ~13 ~20 minecraft:stone_bricks
fill ~0 ~13 ~21 ~0 ~13 ~22 minecraft:stone_bricks
setblock ~0 ~13 ~23 minecraft:cracked_stone_bricks
fill ~0 ~13 ~24 ~0 ~13 ~26 minecraft:stone_bricks
fill ~1 ~13 ~26 ~2 ~13 ~26 minecraft:stone_bricks
setblock ~3 ~13 ~20 minecraft:cracked_stone_bricks
setblock ~3 ~13 ~26 minecraft:cracked_stone_bricks
fill ~4 ~13 ~20 ~6 ~13 ~20 minecraft:stone_bricks
fill ~4 ~13 ~26 ~4 ~14 ~26 minecraft:stone_bricks
setblock ~5 ~13 ~26 minecraft:cracked_deepslate_bricks
fill ~6 ~13 ~21 ~6 ~13 ~23 minecraft:stone_bricks
fill ~6 ~13 ~24 ~6 ~13 ~26 minecraft:deepslate_bricks
setblock ~0 ~14 ~20 minecraft:deepslate_bricks
setblock ~0 ~14 ~22 minecraft:cracked_stone_bricks
setblock ~0 ~14 ~24 minecraft:stone_bricks
setblock ~0 ~14 ~26 minecraft:deepslate_bricks
setblock ~2 ~14 ~20 minecraft:stone_bricks
setblock ~2 ~14 ~26 minecraft:stone_bricks
setblock ~4 ~14 ~20 minecraft:stone_bricks
setblock ~6 ~14 ~20 minecraft:deepslate_bricks
setblock ~6 ~14 ~22 minecraft:stone_bricks
setblock ~6 ~14 ~24 minecraft:stone_bricks
setblock ~6 ~14 ~26 minecraft:deepslate_bricks
setblock ~0 ~15 ~20 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~0 ~15 ~26 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~6 ~15 ~20 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~6 ~15 ~26 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~0 ~16 ~20 minecraft:lantern[hanging=false]
setblock ~0 ~16 ~26 minecraft:lantern[hanging=false]
setblock ~6 ~16 ~20 minecraft:lantern[hanging=false]
setblock ~6 ~16 ~26 minecraft:lantern[hanging=false]

# corner tower at 40,20: walls y 1..11 with dark quoins, moss low
fill ~40 ~1 ~20 ~40 ~2 ~20 minecraft:stone_bricks
fill ~40 ~1 ~21 ~40 ~1 ~23 minecraft:deepslate_bricks
fill ~40 ~1 ~24 ~40 ~2 ~24 minecraft:cracked_deepslate_bricks
fill ~40 ~1 ~25 ~40 ~2 ~26 minecraft:deepslate_bricks
setblock ~41 ~1 ~20 minecraft:cracked_deepslate_bricks
fill ~44 ~1 ~20 ~44 ~6 ~20 minecraft:deepslate_bricks
fill ~45 ~1 ~20 ~46 ~1 ~20 minecraft:cracked_deepslate_bricks
setblock ~46 ~1 ~26 minecraft:deepslate_bricks
fill ~40 ~2 ~21 ~40 ~5 ~22 minecraft:mossy_stone_bricks
fill ~40 ~2 ~23 ~40 ~6 ~23 minecraft:deepslate_bricks
fill ~41 ~2 ~20 ~41 ~4 ~20 minecraft:deepslate_bricks
fill ~41 ~2 ~26 ~41 ~6 ~26 minecraft:deepslate_bricks
fill ~42 ~2 ~26 ~43 ~2 ~26 minecraft:mossy_stone_bricks
setblock ~43 ~2 ~20 minecraft:cracked_deepslate_bricks
fill ~44 ~2 ~26 ~45 ~3 ~26 minecraft:stone_bricks
fill ~45 ~2 ~20 ~45 ~5 ~20 minecraft:deepslate_bricks
setblock ~46 ~2 ~20 minecraft:polished_blackstone_bricks
fill ~46 ~2 ~21 ~46 ~6 ~22 minecraft:stone_bricks
setblock ~46 ~2 ~23 minecraft:cracked_stone_bricks
fill ~46 ~2 ~24 ~46 ~3 ~25 minecraft:stone_bricks
setblock ~46 ~2 ~26 minecraft:cracked_deepslate_bricks
setblock ~40 ~3 ~20 minecraft:deepslate_bricks
fill ~40 ~3 ~24 ~40 ~3 ~25 minecraft:deepslate_bricks
setblock ~40 ~3 ~26 minecraft:cracked_deepslate_bricks
fill ~42 ~3 ~26 ~42 ~5 ~26 minecraft:mossy_stone_bricks
fill ~43 ~3 ~20 ~43 ~5 ~20 minecraft:deepslate_bricks
setblock ~46 ~3 ~20 minecraft:cracked_deepslate_bricks
setblock ~46 ~3 ~26 minecraft:deepslate_bricks
fill ~40 ~4 ~20 ~40 ~5 ~20 minecraft:cracked_deepslate_bricks
setblock ~40 ~4 ~24 minecraft:deepslate_bricks
fill ~40 ~4 ~25 ~40 ~5 ~25 minecraft:cracked_deepslate_bricks
fill ~40 ~4 ~26 ~40 ~5 ~26 minecraft:stone_bricks
fill ~42 ~4 ~20 ~42 ~5 ~20 minecraft:cracked_deepslate_bricks
fill ~44 ~4 ~26 ~44 ~6 ~26 minecraft:stone_bricks
setblock ~45 ~4 ~26 minecraft:cracked_stone_bricks
setblock ~46 ~4 ~20 minecraft:deepslate_bricks
setblock ~46 ~4 ~24 minecraft:cracked_stone_bricks
fill ~46 ~4 ~25 ~46 ~6 ~25 minecraft:stone_bricks
setblock ~46 ~4 ~26 minecraft:cracked_deepslate_bricks
setblock ~40 ~5 ~24 minecraft:cracked_deepslate_bricks
setblock ~41 ~5 ~20 minecraft:cracked_stone_bricks
setblock ~43 ~5 ~26 minecraft:cracked_stone_bricks
fill ~45 ~5 ~26 ~45 ~6 ~26 minecraft:stone_bricks
setblock ~46 ~5 ~20 minecraft:stone_bricks
fill ~46 ~5 ~23 ~46 ~5 ~24 minecraft:stone_bricks
setblock ~46 ~5 ~26 minecraft:polished_blackstone_bricks
fill ~40 ~6 ~20 ~40 ~7 ~20 minecraft:deepslate_bricks
fill ~40 ~6 ~21 ~40 ~8 ~22 minecraft:stone_bricks
fill ~40 ~6 ~24 ~40 ~6 ~25 minecraft:deepslate_bricks
setblock ~40 ~6 ~26 minecraft:polished_blackstone_bricks
fill ~41 ~6 ~20 ~41 ~11 ~20 minecraft:stone_bricks
setblock ~42 ~6 ~20 minecraft:deepslate_bricks
fill ~42 ~6 ~26 ~43 ~6 ~26 minecraft:stone_bricks
setblock ~43 ~6 ~20 minecraft:cracked_deepslate_bricks
setblock ~45 ~6 ~20 minecraft:cracked_deepslate_bricks
setblock ~46 ~6 ~20 minecraft:deepslate_bricks
setblock ~46 ~6 ~23 minecraft:stone_bricks
setblock ~46 ~6 ~24 minecraft:cracked_stone_bricks
setblock ~46 ~6 ~26 minecraft:stone_bricks
fill ~40 ~7 ~23 ~40 ~8 ~23 minecraft:stone_bricks
fill ~40 ~7 ~26 ~40 ~8 ~26 minecraft:deepslate_bricks
fill ~42 ~7 ~20 ~43 ~8 ~20 minecraft:stone_bricks
setblock ~46 ~7 ~20 minecraft:polished_blackstone_bricks
setblock ~46 ~7 ~26 minecraft:cracked_deepslate_bricks
setblock ~40 ~8 ~20 minecraft:polished_blackstone_bricks
fill ~41 ~8 ~26 ~45 ~8 ~26 minecraft:stone_bricks
fill ~46 ~8 ~20 ~46 ~9 ~20 minecraft:deepslate_bricks
fill ~46 ~8 ~21 ~46 ~8 ~25 minecraft:stone_bricks
setblock ~46 ~8 ~26 minecraft:deepslate_bricks
fill ~40 ~9 ~20 ~40 ~10 ~20 minecraft:deepslate_bricks
fill ~40 ~9 ~21 ~40 ~9 ~23 minecraft:cracked_stone_bricks
fill ~40 ~9 ~24 ~40 ~10 ~25 minecraft:stone_bricks
setblock ~40 ~9 ~26 minecraft:polished_blackstone_bricks
fill ~41 ~9 ~26 ~42 ~9 ~26 minecraft:cracked_stone_bricks
fill ~42 ~9 ~20 ~42 ~11 ~20 minecraft:stone_bricks
setblock ~43 ~9 ~20 minecraft:cracked_stone_bricks
fill ~44 ~9 ~20 ~44 ~10 ~20 minecraft:stone_bricks
fill ~44 ~9 ~26 ~45 ~9 ~26 minecraft:stone_bricks
setblock ~45 ~9 ~20 minecraft:cracked_stone_bricks
setblock ~46 ~9 ~21 minecraft:stone_bricks
setblock ~46 ~9 ~22 minecraft:cracked_stone_bricks
setblock ~46 ~9 ~24 minecraft:cracked_stone_bricks
setblock ~46 ~9 ~25 minecraft:stone_bricks
fill ~46 ~9 ~26 ~46 ~10 ~26 minecraft:cracked_deepslate_bricks
fill ~40 ~10 ~21 ~40 ~11 ~21 minecraft:stone_bricks
fill ~40 ~10 ~22 ~40 ~10 ~23 minecraft:cracked_stone_bricks
fill ~40 ~10 ~26 ~42 ~10 ~26 minecraft:deepslate_bricks
setblock ~43 ~10 ~20 minecraft:stone_bricks
fill ~44 ~10 ~26 ~45 ~10 ~26 minecraft:deepslate_bricks
fill ~45 ~10 ~20 ~45 ~11 ~20 minecraft:deepslate_bricks
setblock ~46 ~10 ~20 minecraft:polished_blackstone_bricks
fill ~46 ~10 ~21 ~46 ~10 ~22 minecraft:deepslate_bricks
fill ~46 ~10 ~24 ~46 ~10 ~25 minecraft:deepslate_bricks
setblock ~40 ~11 ~20 minecraft:stone_bricks
setblock ~40 ~11 ~22 minecraft:cracked_stone_bricks
fill ~40 ~11 ~23 ~40 ~11 ~24 minecraft:stone_bricks
fill ~40 ~11 ~25 ~40 ~11 ~26 minecraft:deepslate_bricks
setblock ~43 ~11 ~20 minecraft:cracked_deepslate_bricks
setblock ~44 ~11 ~20 minecraft:deepslate_bricks
setblock ~46 ~11 ~20 minecraft:cracked_deepslate_bricks
setblock ~46 ~11 ~26 minecraft:deepslate_bricks

# the plinth lip on the two outer faces, the string course at y 7 under the high slits
fill ~41 ~1 ~26 ~45 ~1 ~26 minecraft:cobblestone_stairs[facing=north,half=bottom]
fill ~46 ~1 ~21 ~46 ~1 ~25 minecraft:cobblestone_stairs[facing=west,half=bottom]
fill ~41 ~7 ~26 ~45 ~7 ~26 minecraft:chiseled_stone_bricks
fill ~46 ~7 ~21 ~46 ~7 ~25 minecraft:chiseled_stone_bricks

# the floors: planks at y 6, flagstones at y 12 across the top
fill ~41 ~6 ~21 ~45 ~6 ~21 minecraft:spruce_planks
fill ~41 ~6 ~23 ~41 ~6 ~25 minecraft:spruce_planks
fill ~42 ~6 ~22 ~45 ~6 ~25 minecraft:spruce_planks
fill ~40 ~12 ~20 ~41 ~12 ~20 minecraft:stone_bricks
fill ~40 ~12 ~21 ~40 ~12 ~22 minecraft:stone_bricks
setblock ~40 ~12 ~23 minecraft:cobblestone
fill ~40 ~12 ~24 ~42 ~12 ~25 minecraft:stone_bricks
setblock ~40 ~12 ~26 minecraft:stone_bricks
setblock ~41 ~12 ~21 minecraft:mossy_stone_bricks
fill ~41 ~12 ~23 ~42 ~12 ~23 minecraft:mossy_stone_bricks
setblock ~41 ~12 ~26 minecraft:cracked_stone_bricks
setblock ~42 ~12 ~20 minecraft:cracked_stone_bricks
setblock ~42 ~12 ~21 minecraft:stone_bricks
setblock ~42 ~12 ~22 minecraft:cracked_stone_bricks
fill ~42 ~12 ~26 ~46 ~12 ~26 minecraft:stone_bricks
fill ~43 ~12 ~20 ~43 ~12 ~21 minecraft:stone_bricks
setblock ~43 ~12 ~22 minecraft:cobblestone
setblock ~43 ~12 ~23 minecraft:stone_bricks
fill ~43 ~12 ~24 ~44 ~12 ~24 minecraft:cracked_stone_bricks
setblock ~43 ~12 ~25 minecraft:mossy_stone_bricks
fill ~44 ~12 ~20 ~45 ~12 ~20 minecraft:cracked_stone_bricks
setblock ~44 ~12 ~21 minecraft:cracked_stone_bricks
fill ~44 ~12 ~22 ~44 ~12 ~23 minecraft:stone_bricks
setblock ~44 ~12 ~25 minecraft:cobblestone
fill ~45 ~12 ~21 ~46 ~12 ~21 minecraft:stone_bricks
fill ~45 ~12 ~22 ~45 ~12 ~23 minecraft:mossy_stone_bricks
setblock ~45 ~12 ~24 minecraft:cobblestone
fill ~45 ~12 ~25 ~46 ~12 ~25 minecraft:cracked_stone_bricks
setblock ~46 ~12 ~20 minecraft:mossy_stone_bricks
setblock ~46 ~12 ~22 minecraft:stone_bricks
setblock ~46 ~12 ~23 minecraft:mossy_stone_bricks
setblock ~46 ~12 ~24 minecraft:stone_bricks

# the ladder up the inner face (u 5, v 4) from y 1 to y 13 through both floors
fill ~41 ~1 ~22 ~41 ~13 ~22 minecraft:ladder[facing=east]

# the door onto the courtyard (u 4, v 6), the openings from the wall-walks into the upper room, slits
fill ~42 ~1 ~20 ~42 ~2 ~20 minecraft:air
setblock ~42 ~3 ~20 minecraft:chiseled_stone_bricks
fill ~43 ~3 ~26 ~43 ~4 ~26 minecraft:air
fill ~46 ~3 ~23 ~46 ~4 ~23 minecraft:air
fill ~40 ~7 ~24 ~40 ~8 ~25 minecraft:air
fill ~44 ~7 ~20 ~45 ~8 ~20 minecraft:air
fill ~43 ~9 ~26 ~43 ~10 ~26 minecraft:air
fill ~46 ~9 ~23 ~46 ~10 ~23 minecraft:air

# the ground room: barrels and a lantern on a chain; the upper room: a barrel and a lantern
setblock ~44 ~1 ~25 minecraft:barrel[facing=up]
fill ~45 ~1 ~24 ~45 ~1 ~25 minecraft:barrel[facing=up]
setblock ~45 ~2 ~25 minecraft:barrel[facing=up]
setblock ~43 ~4 ~23 minecraft:lantern[hanging=true]
setblock ~43 ~5 ~23 minecraft:chain[axis=y]
setblock ~45 ~7 ~21 minecraft:barrel[facing=up]
setblock ~43 ~10 ~23 minecraft:lantern[hanging=true]
setblock ~43 ~11 ~23 minecraft:chain[axis=y]

# the corbel course at y 11 on the two outer faces: upside-down deepslate brick stairs under the rampart
fill ~41 ~11 ~26 ~45 ~11 ~26 minecraft:deepslate_brick_stairs[facing=north,half=top]
fill ~46 ~11 ~21 ~46 ~11 ~25 minecraft:deepslate_brick_stairs[facing=west,half=top]

# the parapet ring at y 13 with merlons at y 14 and lantern posts at the corners
fill ~40 ~13 ~20 ~43 ~13 ~20 minecraft:deepslate_bricks
fill ~40 ~13 ~21 ~40 ~13 ~26 minecraft:deepslate_bricks
setblock ~41 ~13 ~26 minecraft:deepslate_bricks
setblock ~42 ~13 ~26 minecraft:stone_bricks
setblock ~43 ~13 ~26 minecraft:cracked_stone_bricks
fill ~44 ~13 ~20 ~44 ~14 ~20 minecraft:cracked_deepslate_bricks
fill ~44 ~13 ~26 ~45 ~13 ~26 minecraft:stone_bricks
fill ~45 ~13 ~20 ~46 ~13 ~20 minecraft:stone_bricks
setblock ~46 ~13 ~21 minecraft:stone_bricks
setblock ~46 ~13 ~22 minecraft:cracked_stone_bricks
fill ~46 ~13 ~23 ~46 ~13 ~25 minecraft:stone_bricks
fill ~46 ~13 ~26 ~46 ~14 ~26 minecraft:deepslate_bricks
setblock ~40 ~14 ~20 minecraft:deepslate_bricks
setblock ~40 ~14 ~22 minecraft:deepslate_bricks
setblock ~40 ~14 ~24 minecraft:deepslate_bricks
setblock ~40 ~14 ~26 minecraft:deepslate_bricks
setblock ~42 ~14 ~20 minecraft:deepslate_bricks
setblock ~42 ~14 ~26 minecraft:deepslate_bricks
setblock ~44 ~14 ~26 minecraft:cracked_deepslate_bricks
setblock ~46 ~14 ~20 minecraft:deepslate_bricks
setblock ~46 ~14 ~22 minecraft:deepslate_bricks
setblock ~46 ~14 ~24 minecraft:stone_bricks
setblock ~40 ~15 ~20 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~40 ~15 ~26 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~46 ~15 ~20 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~46 ~15 ~26 minecraft:stone_brick_wall[north=none,east=none,south=none,west=none,up=true]
setblock ~40 ~16 ~20 minecraft:lantern[hanging=false]
setblock ~40 ~16 ~26 minecraft:lantern[hanging=false]
setblock ~46 ~16 ~20 minecraft:lantern[hanging=false]
setblock ~46 ~16 ~26 minecraft:lantern[hanging=false]

# the donjon x 31..41, z 8..18: walls y 1..16 in patched dark stone with dark quoins, string courses at y 6 and y 12
setblock ~31 ~1 ~8 minecraft:stone_bricks
fill ~31 ~1 ~9 ~31 ~1 ~10 minecraft:mossy_stone_bricks
fill ~31 ~1 ~16 ~31 ~1 ~17 minecraft:mossy_stone_bricks
fill ~31 ~1 ~18 ~31 ~2 ~18 minecraft:deepslate_bricks
fill ~32 ~1 ~8 ~33 ~1 ~8 minecraft:deepslate_bricks
fill ~32 ~1 ~18 ~36 ~1 ~18 minecraft:mossy_stone_bricks
setblock ~34 ~1 ~8 minecraft:cracked_deepslate_bricks
fill ~35 ~1 ~8 ~40 ~1 ~8 minecraft:mossy_stone_bricks
fill ~37 ~1 ~18 ~39 ~1 ~18 minecraft:stone_bricks
fill ~40 ~1 ~18 ~40 ~4 ~18 minecraft:mossy_stone_bricks
fill ~41 ~1 ~8 ~41 ~5 ~8 minecraft:deepslate_bricks
fill ~41 ~1 ~9 ~41 ~1 ~17 minecraft:mossy_stone_bricks
fill ~41 ~1 ~18 ~41 ~5 ~18 minecraft:deepslate_bricks
setblock ~31 ~2 ~8 minecraft:polished_blackstone_bricks
fill ~31 ~2 ~9 ~31 ~2 ~10 minecraft:deepslate_bricks
fill ~31 ~2 ~16 ~31 ~5 ~17 minecraft:stone_bricks
fill ~32 ~2 ~8 ~33 ~3 ~8 minecraft:mossy_stone_bricks
fill ~32 ~2 ~18 ~33 ~5 ~18 minecraft:stone_bricks
fill ~35 ~2 ~8 ~37 ~4 ~8 minecraft:mossy_stone_bricks
fill ~35 ~2 ~18 ~35 ~3 ~18 minecraft:cracked_stone_bricks
fill ~36 ~2 ~18 ~36 ~5 ~18 minecraft:stone_bricks
fill ~37 ~2 ~18 ~37 ~3 ~18 minecraft:mossy_stone_bricks
fill ~39 ~2 ~8 ~40 ~5 ~8 minecraft:mossy_stone_bricks
fill ~39 ~2 ~18 ~39 ~3 ~18 minecraft:mossy_stone_bricks
fill ~41 ~2 ~9 ~41 ~5 ~9 minecraft:mossy_stone_bricks
fill ~41 ~2 ~11 ~41 ~5 ~15 minecraft:mossy_stone_bricks
fill ~41 ~2 ~17 ~41 ~5 ~17 minecraft:mossy_stone_bricks
setblock ~31 ~3 ~8 minecraft:stone_bricks
fill ~31 ~3 ~9 ~31 ~5 ~9 minecraft:deepslate_bricks
setblock ~31 ~3 ~10 minecraft:cracked_deepslate_bricks
setblock ~31 ~3 ~18 minecraft:cracked_deepslate_bricks
setblock ~31 ~4 ~8 minecraft:polished_blackstone_bricks
fill ~31 ~4 ~10 ~31 ~7 ~10 minecraft:deepslate_bricks
fill ~31 ~4 ~18 ~31 ~6 ~18 minecraft:deepslate_bricks
fill ~32 ~4 ~8 ~32 ~5 ~8 minecraft:cracked_deepslate_bricks
fill ~33 ~4 ~8 ~34 ~5 ~8 minecraft:deepslate_bricks
fill ~34 ~4 ~18 ~35 ~4 ~18 minecraft:stone_bricks
fill ~37 ~4 ~18 ~39 ~5 ~18 minecraft:stone_bricks
fill ~38 ~4 ~8 ~38 ~5 ~8 minecraft:mossy_stone_bricks
fill ~41 ~4 ~10 ~41 ~5 ~10 minecraft:mossy_stone_bricks
fill ~41 ~4 ~16 ~41 ~5 ~16 minecraft:mossy_stone_bricks
fill ~31 ~5 ~8 ~31 ~9 ~8 minecraft:deepslate_bricks
setblock ~31 ~5 ~11 minecraft:cracked_deepslate_bricks
setblock ~31 ~5 ~12 minecraft:deepslate_bricks
setblock ~31 ~5 ~13 minecraft:cracked_deepslate_bricks
setblock ~31 ~5 ~14 minecraft:deepslate_bricks
setblock ~31 ~5 ~15 minecraft:cracked_stone_bricks
fill ~34 ~5 ~18 ~35 ~5 ~18 minecraft:deepslate_bricks
setblock ~35 ~5 ~8 minecraft:stone_bricks
fill ~36 ~5 ~8 ~37 ~5 ~8 minecraft:mossy_stone_bricks
fill ~40 ~5 ~18 ~40 ~6 ~18 minecraft:deepslate_bricks
setblock ~31 ~6 ~9 minecraft:chiseled_stone_bricks
setblock ~31 ~6 ~11 minecraft:polished_blackstone_bricks
fill ~31 ~6 ~12 ~31 ~6 ~14 minecraft:chiseled_stone_bricks
setblock ~31 ~6 ~15 minecraft:deepslate_bricks
setblock ~31 ~6 ~16 minecraft:polished_blackstone_bricks
setblock ~31 ~6 ~17 minecraft:deepslate_bricks
setblock ~32 ~6 ~8 minecraft:chiseled_stone_bricks
setblock ~32 ~6 ~18 minecraft:deepslate_bricks
fill ~33 ~6 ~8 ~33 ~10 ~8 minecraft:deepslate_bricks
setblock ~33 ~6 ~18 minecraft:polished_blackstone_bricks
fill ~34 ~6 ~8 ~35 ~6 ~8 minecraft:chiseled_stone_bricks
setblock ~34 ~6 ~18 minecraft:deepslate_bricks
setblock ~35 ~6 ~18 minecraft:polished_blackstone_bricks
fill ~36 ~6 ~8 ~37 ~6 ~8 minecraft:deepslate_bricks
setblock ~36 ~6 ~18 minecraft:deepslate_bricks
setblock ~37 ~6 ~18 minecraft:polished_blackstone_bricks
setblock ~38 ~6 ~8 minecraft:polished_blackstone_bricks
setblock ~38 ~6 ~18 minecraft:chiseled_stone_bricks
setblock ~39 ~6 ~8 minecraft:deepslate_bricks
setblock ~39 ~6 ~18 minecraft:deepslate_bricks
fill ~40 ~6 ~8 ~41 ~6 ~8 minecraft:chiseled_stone_bricks
setblock ~41 ~6 ~9 minecraft:polished_blackstone_bricks
fill ~41 ~6 ~10 ~41 ~6 ~11 minecraft:deepslate_bricks
setblock ~41 ~6 ~12 minecraft:polished_blackstone_bricks
setblock ~41 ~6 ~13 minecraft:deepslate_bricks
setblock ~41 ~6 ~14 minecraft:chiseled_stone_bricks
setblock ~41 ~6 ~15 minecraft:deepslate_bricks
fill ~41 ~6 ~16 ~41 ~6 ~17 minecraft:polished_blackstone_bricks
setblock ~41 ~6 ~18 minecraft:chiseled_stone_bricks
fill ~31 ~7 ~9 ~31 ~9 ~9 minecraft:deepslate_bricks
fill ~31 ~7 ~11 ~31 ~8 ~11 minecraft:cracked_deepslate_bricks
fill ~31 ~7 ~12 ~31 ~16 ~12 minecraft:deepslate_bricks
setblock ~31 ~7 ~14 minecraft:deepslate_bricks
setblock ~31 ~7 ~15 minecraft:cracked_deepslate_bricks
setblock ~31 ~7 ~16 minecraft:deepslate_bricks
setblock ~31 ~7 ~17 minecraft:stone_bricks
setblock ~31 ~7 ~18 minecraft:polished_blackstone_bricks
fill ~32 ~7 ~8 ~32 ~12 ~8 minecraft:deepslate_bricks
fill ~32 ~7 ~18 ~35 ~7 ~18 minecraft:stone_bricks
setblock ~34 ~7 ~8 minecraft:cracked_deepslate_bricks
fill ~35 ~7 ~8 ~35 ~10 ~8 minecraft:deepslate_bricks
setblock ~36 ~7 ~8 minecraft:cracked_stone_bricks
setblock ~36 ~7 ~18 minecraft:cracked_stone_bricks
fill ~37 ~7 ~8 ~40 ~7 ~8 minecraft:stone_bricks
fill ~37 ~7 ~18 ~37 ~10 ~18 minecraft:stone_bricks
fill ~38 ~7 ~18 ~39 ~7 ~18 minecraft:cracked_stone_bricks
fill ~40 ~7 ~18 ~40 ~11 ~18 minecraft:stone_bricks
setblock ~41 ~7 ~8 minecraft:deepslate_bricks
fill ~41 ~7 ~9 ~41 ~7 ~10 minecraft:cracked_stone_bricks
fill ~41 ~7 ~11 ~41 ~7 ~17 minecraft:stone_bricks
setblock ~41 ~7 ~18 minecraft:deepslate_bricks
fill ~31 ~8 ~14 ~31 ~9 ~14 minecraft:cracked_deepslate_bricks
fill ~31 ~8 ~15 ~31 ~10 ~15 minecraft:deepslate_bricks
fill ~31 ~8 ~17 ~31 ~10 ~18 minecraft:deepslate_bricks
fill ~32 ~8 ~18 ~33 ~8 ~18 minecraft:stone_bricks
setblock ~35 ~8 ~18 minecraft:cracked_stone_bricks
fill ~36 ~8 ~8 ~37 ~11 ~8 minecraft:deepslate_bricks
setblock ~36 ~8 ~18 minecraft:stone_bricks
setblock ~39 ~8 ~8 minecraft:stone_bricks
fill ~39 ~8 ~18 ~39 ~11 ~18 minecraft:stone_bricks
fill ~40 ~8 ~8 ~40 ~9 ~8 minecraft:cracked_stone_bricks
fill ~41 ~8 ~8 ~41 ~8 ~9 minecraft:stone_bricks
fill ~41 ~8 ~11 ~41 ~8 ~15 minecraft:stone_bricks
fill ~41 ~8 ~17 ~41 ~9 ~17 minecraft:stone_bricks
setblock ~41 ~8 ~18 minecraft:polished_blackstone_bricks
fill ~31 ~9 ~11 ~31 ~11 ~11 minecraft:deepslate_bricks
setblock ~31 ~9 ~13 minecraft:cracked_deepslate_bricks
fill ~32 ~9 ~18 ~32 ~11 ~18 minecraft:deepslate_bricks
setblock ~33 ~9 ~18 minecraft:cracked_deepslate_bricks
setblock ~35 ~9 ~18 minecraft:stone_bricks
setblock ~36 ~9 ~18 minecraft:cracked_stone_bricks
setblock ~39 ~9 ~8 minecraft:cracked_stone_bricks
setblock ~41 ~9 ~8 minecraft:polished_blackstone_bricks
setblock ~41 ~9 ~9 minecraft:stone_bricks
fill ~41 ~9 ~11 ~41 ~10 ~13 minecraft:stone_bricks
setblock ~41 ~9 ~14 minecraft:cracked_stone_bricks
fill ~41 ~9 ~15 ~41 ~10 ~15 minecraft:stone_bricks
setblock ~41 ~9 ~18 minecraft:stone_bricks
fill ~31 ~10 ~8 ~31 ~10 ~9 minecraft:cracked_deepslate_bricks
fill ~31 ~10 ~10 ~31 ~13 ~10 minecraft:deepslate_bricks
fill ~31 ~10 ~13 ~31 ~11 ~14 minecraft:deepslate_bricks
setblock ~31 ~10 ~16 minecraft:deepslate_bricks
fill ~33 ~10 ~18 ~36 ~11 ~18 minecraft:deepslate_bricks
setblock ~34 ~10 ~8 minecraft:deepslate_bricks
setblock ~38 ~10 ~8 minecraft:cracked_deepslate_bricks
setblock ~38 ~10 ~18 minecraft:stone_bricks
fill ~39 ~10 ~8 ~41 ~10 ~8 minecraft:deepslate_bricks
fill ~41 ~10 ~9 ~41 ~11 ~9 minecraft:deepslate_bricks
setblock ~41 ~10 ~10 minecraft:stone_bricks
fill ~41 ~10 ~14 ~41 ~11 ~14 minecraft:stone_bricks
fill ~41 ~10 ~16 ~41 ~11 ~16 minecraft:stone_bricks
setblock ~41 ~10 ~17 minecraft:cracked_stone_bricks
setblock ~41 ~10 ~18 minecraft:polished_blackstone_bricks
setblock ~31 ~11 ~8 minecraft:stone_bricks
fill ~31 ~11 ~9 ~31 ~13 ~9 minecraft:deepslate_bricks
fill ~31 ~11 ~15 ~31 ~11 ~16 minecraft:cracked_deepslate_bricks
fill ~31 ~11 ~17 ~31 ~14 ~17 minecraft:deepslate_bricks
setblock ~31 ~11 ~18 minecraft:cracked_deepslate_bricks
fill ~33 ~11 ~8 ~35 ~11 ~8 minecraft:cracked_deepslate_bricks
fill ~37 ~11 ~18 ~37 ~12 ~18 minecraft:deepslate_bricks
fill ~38 ~11 ~8 ~40 ~11 ~8 minecraft:deepslate_bricks
setblock ~38 ~11 ~18 minecraft:cracked_deepslate_bricks
setblock ~41 ~11 ~8 minecraft:cracked_deepslate_bricks
fill ~41 ~11 ~10 ~41 ~11 ~13 minecraft:deepslate_bricks
setblock ~41 ~11 ~15 minecraft:cracked_stone_bricks
setblock ~41 ~11 ~17 minecraft:stone_bricks
setblock ~41 ~11 ~18 minecraft:cracked_deepslate_bricks
setblock ~31 ~12 ~8 minecraft:polished_blackstone_bricks
setblock ~31 ~12 ~11 minecraft:polished_blackstone_bricks
setblock ~31 ~12 ~13 minecraft:chiseled_stone_bricks
fill ~31 ~12 ~14 ~31 ~12 ~16 minecraft:deepslate_bricks
setblock ~31 ~12 ~18 minecraft:chiseled_stone_bricks
setblock ~32 ~12 ~18 minecraft:polished_blackstone_bricks
fill ~33 ~12 ~8 ~33 ~13 ~8 minecraft:deepslate_bricks
fill ~33 ~12 ~18 ~34 ~12 ~18 minecraft:deepslate_bricks
setblock ~34 ~12 ~8 minecraft:chiseled_stone_bricks
fill ~35 ~12 ~8 ~35 ~16 ~8 minecraft:deepslate_bricks
setblock ~35 ~12 ~18 minecraft:chiseled_stone_bricks
setblock ~36 ~12 ~8 minecraft:polished_blackstone_bricks
setblock ~36 ~12 ~18 minecraft:deepslate_bricks
fill ~37 ~12 ~8 ~37 ~16 ~8 minecraft:deepslate_bricks
setblock ~38 ~12 ~8 minecraft:polished_blackstone_bricks
setblock ~38 ~12 ~18 minecraft:polished_blackstone_bricks
fill ~39 ~12 ~8 ~40 ~12 ~8 minecraft:deepslate_bricks
setblock ~39 ~12 ~18 minecraft:deepslate_bricks
setblock ~40 ~12 ~18 minecraft:polished_blackstone_bricks
fill ~41 ~12 ~8 ~41 ~12 ~9 minecraft:chiseled_stone_bricks
fill ~41 ~12 ~10 ~41 ~12 ~11 minecraft:deepslate_bricks
setblock ~41 ~12 ~12 minecraft:chiseled_stone_bricks
fill ~41 ~12 ~13 ~41 ~12 ~16 minecraft:deepslate_bricks
setblock ~41 ~12 ~17 minecraft:chiseled_stone_bricks
setblock ~41 ~12 ~18 minecraft:deepslate_bricks
fill ~31 ~13 ~8 ~31 ~15 ~8 minecraft:deepslate_bricks
setblock ~31 ~13 ~11 minecraft:deepslate_bricks
fill ~31 ~13 ~13 ~31 ~13 ~15 minecraft:deepslate_bricks
setblock ~31 ~13 ~16 minecraft:cracked_deepslate_bricks
setblock ~31 ~13 ~18 minecraft:polished_blackstone_bricks
setblock ~32 ~13 ~8 minecraft:cracked_deepslate_bricks
fill ~32 ~13 ~18 ~32 ~16 ~18 minecraft:deepslate_bricks
fill ~33 ~13 ~18 ~34 ~13 ~18 minecraft:stone_bricks
setblock ~34 ~13 ~8 minecraft:deepslate_bricks
fill ~35 ~13 ~18 ~35 ~14 ~18 minecraft:cracked_stone_bricks
setblock ~36 ~13 ~8 minecraft:deepslate_bricks
fill ~36 ~13 ~18 ~38 ~13 ~18 minecraft:stone_bricks
setblock ~38 ~13 ~8 minecraft:deepslate_bricks
fill ~39 ~13 ~8 ~40 ~14 ~8 minecraft:stone_bricks
fill ~39 ~13 ~18 ~39 ~14 ~18 minecraft:cracked_stone_bricks
fill ~40 ~13 ~18 ~40 ~16 ~18 minecraft:stone_bricks
setblock ~41 ~13 ~8 minecraft:cracked_deepslate_bricks
setblock ~41 ~13 ~9 minecraft:cracked_stone_bricks
setblock ~41 ~13 ~10 minecraft:stone_bricks
fill ~41 ~13 ~11 ~41 ~13 ~12 minecraft:cracked_stone_bricks
fill ~41 ~13 ~13 ~41 ~13 ~18 minecraft:stone_bricks
setblock ~31 ~14 ~9 minecraft:cracked_stone_bricks
setblock ~31 ~14 ~11 minecraft:stone_bricks
fill ~31 ~14 ~13 ~31 ~16 ~13 minecraft:deepslate_bricks
fill ~31 ~14 ~14 ~31 ~15 ~14 minecraft:cracked_deepslate_bricks
fill ~31 ~14 ~15 ~31 ~16 ~15 minecraft:deepslate_bricks
setblock ~31 ~14 ~18 minecraft:cracked_deepslate_bricks
fill ~32 ~14 ~8 ~33 ~14 ~8 minecraft:stone_bricks
setblock ~33 ~14 ~18 minecraft:cracked_stone_bricks
setblock ~37 ~14 ~18 minecraft:cracked_stone_bricks
setblock ~41 ~14 ~8 minecraft:deepslate_bricks
fill ~41 ~14 ~9 ~41 ~16 ~9 minecraft:stone_bricks
setblock ~41 ~14 ~11 minecraft:cracked_stone_bricks
fill ~41 ~14 ~12 ~41 ~14 ~14 minecraft:stone_bricks
setblock ~41 ~14 ~15 minecraft:cracked_stone_bricks
fill ~41 ~14 ~17 ~41 ~16 ~17 minecraft:stone_bricks
setblock ~41 ~14 ~18 minecraft:polished_blackstone_bricks
setblock ~31 ~15 ~9 minecraft:cracked_deepslate_bricks
fill ~31 ~15 ~11 ~31 ~16 ~11 minecraft:deepslate_bricks
setblock ~31 ~15 ~17 minecraft:cracked_deepslate_bricks
fill ~31 ~15 ~18 ~31 ~16 ~18 minecraft:deepslate_bricks
fill ~32 ~15 ~8 ~33 ~16 ~8 minecraft:deepslate_bricks
setblock ~33 ~15 ~18 minecraft:stone_bricks
fill ~35 ~15 ~18 ~35 ~16 ~18 minecraft:stone_bricks
fill ~37 ~15 ~18 ~37 ~16 ~18 minecraft:stone_bricks
setblock ~39 ~15 ~8 minecraft:cracked_stone_bricks
fill ~39 ~15 ~18 ~39 ~16 ~18 minecraft:stone_bricks
fill ~40 ~15 ~8 ~40 ~16 ~8 minecraft:stone_bricks
setblock ~41 ~15 ~8 minecraft:polished_blackstone_bricks
fill ~41 ~15 ~11 ~41 ~15 ~12 minecraft:stone_bricks
setblock ~41 ~15 ~13 minecraft:cracked_stone_bricks
fill ~41 ~15 ~14 ~41 ~16 ~15 minecraft:stone_bricks
fill ~41 ~15 ~18 ~41 ~16 ~18 minecraft:deepslate_bricks
setblock ~31 ~16 ~8 minecraft:stone_bricks
setblock ~31 ~16 ~9 minecraft:deepslate_bricks
setblock ~31 ~16 ~10 minecraft:cracked_deepslate_bricks
setblock ~31 ~16 ~14 minecraft:deepslate_bricks
fill ~31 ~16 ~16 ~31 ~16 ~17 minecraft:deepslate_bricks
setblock ~33 ~16 ~18 minecraft:deepslate_bricks
setblock ~34 ~16 ~8 minecraft:deepslate_bricks
setblock ~34 ~16 ~18 minecraft:stone_bricks
setblock ~36 ~16 ~8 minecraft:cracked_deepslate_bricks
setblock ~36 ~16 ~18 minecraft:stone_bricks
setblock ~38 ~16 ~8 minecraft:deepslate_bricks
setblock ~38 ~16 ~18 minecraft:cracked_stone_bricks
setblock ~39 ~16 ~8 minecraft:stone_bricks
setblock ~41 ~16 ~8 minecraft:deepslate_bricks
fill ~41 ~16 ~10 ~41 ~16 ~11 minecraft:stone_bricks
setblock ~41 ~16 ~12 minecraft:cracked_stone_bricks
setblock ~41 ~16 ~13 minecraft:stone_bricks
setblock ~41 ~16 ~16 minecraft:stone_bricks

# buttresses on the faces (z 7 and z 19 at x 34 and 38, x 30 and x 42 at z 9 and 17) stepped back with a stair, the plinth lip
fill ~30 ~1 ~9 ~30 ~5 ~9 minecraft:mossy_stone_bricks
fill ~30 ~1 ~17 ~30 ~5 ~17 minecraft:stone_bricks
fill ~34 ~1 ~7 ~34 ~5 ~7 minecraft:stone_bricks
fill ~34 ~1 ~19 ~34 ~5 ~19 minecraft:mossy_stone_bricks
fill ~38 ~1 ~7 ~38 ~5 ~7 minecraft:stone_bricks
fill ~38 ~1 ~19 ~38 ~5 ~19 minecraft:stone_bricks
fill ~42 ~1 ~9 ~42 ~5 ~9 minecraft:stone_bricks
fill ~42 ~1 ~17 ~42 ~5 ~17 minecraft:stone_bricks
setblock ~30 ~6 ~9 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~30 ~6 ~17 minecraft:stone_brick_stairs[facing=east,half=bottom]
setblock ~34 ~6 ~7 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~34 ~6 ~19 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~38 ~6 ~7 minecraft:stone_brick_stairs[facing=south,half=bottom]
setblock ~38 ~6 ~19 minecraft:stone_brick_stairs[facing=north,half=bottom]
setblock ~42 ~6 ~9 minecraft:stone_brick_stairs[facing=west,half=bottom]
setblock ~42 ~6 ~17 minecraft:stone_brick_stairs[facing=west,half=bottom]

# the floors of the donjon: planks at y 6 and y 12 (open at the ladder, x 40), flagstones across the roof platform at y 17
fill ~32 ~6 ~9 ~40 ~6 ~12 minecraft:spruce_planks
fill ~32 ~6 ~13 ~39 ~6 ~17 minecraft:spruce_planks
fill ~40 ~6 ~14 ~40 ~6 ~17 minecraft:spruce_planks
fill ~32 ~12 ~9 ~40 ~12 ~12 minecraft:spruce_planks
fill ~32 ~12 ~13 ~39 ~12 ~17 minecraft:spruce_planks
fill ~40 ~12 ~14 ~40 ~12 ~17 minecraft:spruce_planks
fill ~31 ~17 ~8 ~33 ~17 ~8 minecraft:stone_bricks
setblock ~31 ~17 ~9 minecraft:cracked_stone_bricks
fill ~31 ~17 ~10 ~31 ~17 ~11 minecraft:stone_bricks
fill ~31 ~17 ~12 ~32 ~17 ~12 minecraft:mossy_stone_bricks
fill ~31 ~17 ~13 ~32 ~17 ~13 minecraft:stone_bricks
fill ~31 ~17 ~14 ~31 ~17 ~18 minecraft:stone_bricks
setblock ~32 ~17 ~9 minecraft:cobblestone
setblock ~32 ~17 ~10 minecraft:mossy_stone_bricks
setblock ~32 ~17 ~11 minecraft:stone_bricks
setblock ~32 ~17 ~14 minecraft:cracked_stone_bricks
setblock ~32 ~17 ~15 minecraft:cobblestone
setblock ~32 ~17 ~16 minecraft:mossy_stone_bricks
fill ~32 ~17 ~17 ~32 ~17 ~18 minecraft:stone_bricks
fill ~33 ~17 ~9 ~34 ~17 ~9 minecraft:stone_bricks
setblock ~33 ~17 ~10 minecraft:stone_bricks
setblock ~33 ~17 ~11 minecraft:cobblestone
fill ~33 ~17 ~12 ~33 ~17 ~13 minecraft:cracked_stone_bricks
fill ~33 ~17 ~14 ~34 ~17 ~14 minecraft:stone_bricks
fill ~33 ~17 ~15 ~33 ~17 ~16 minecraft:stone_bricks
setblock ~33 ~17 ~17 minecraft:mossy_stone_bricks
setblock ~33 ~17 ~18 minecraft:cobblestone
setblock ~34 ~17 ~8 minecraft:cobblestone
setblock ~34 ~17 ~10 minecraft:mossy_stone_bricks
fill ~34 ~17 ~11 ~34 ~17 ~12 minecraft:stone_bricks
setblock ~34 ~17 ~13 minecraft:cracked_stone_bricks
fill ~34 ~17 ~15 ~34 ~17 ~16 minecraft:mossy_stone_bricks
setblock ~34 ~17 ~17 minecraft:cracked_stone_bricks
fill ~34 ~17 ~18 ~36 ~17 ~18 minecraft:stone_bricks
fill ~35 ~17 ~8 ~37 ~17 ~8 minecraft:stone_bricks
setblock ~35 ~17 ~9 minecraft:mossy_stone_bricks
fill ~35 ~17 ~10 ~35 ~17 ~13 minecraft:stone_bricks
setblock ~35 ~17 ~14 minecraft:mossy_stone_bricks
fill ~35 ~17 ~15 ~37 ~17 ~16 minecraft:stone_bricks
setblock ~35 ~17 ~17 minecraft:mossy_stone_bricks
fill ~36 ~17 ~9 ~37 ~17 ~9 minecraft:stone_bricks
setblock ~36 ~17 ~10 minecraft:cracked_stone_bricks
setblock ~36 ~17 ~11 minecraft:mossy_stone_bricks
fill ~36 ~17 ~12 ~37 ~17 ~12 minecraft:stone_bricks
setblock ~36 ~17 ~13 minecraft:cracked_stone_bricks
setblock ~36 ~17 ~14 minecraft:cobblestone
setblock ~36 ~17 ~17 minecraft:cobblestone
fill ~37 ~17 ~10 ~38 ~17 ~11 minecraft:stone_bricks
setblock ~37 ~17 ~13 minecraft:cobblestone
setblock ~37 ~17 ~14 minecraft:mossy_stone_bricks
fill ~37 ~17 ~17 ~37 ~17 ~18 minecraft:stone_bricks
setblock ~38 ~17 ~8 minecraft:cracked_stone_bricks
setblock ~38 ~17 ~9 minecraft:cobblestone
setblock ~38 ~17 ~12 minecraft:cobblestone
fill ~38 ~17 ~13 ~38 ~17 ~18 minecraft:stone_bricks
setblock ~39 ~17 ~8 minecraft:mossy_stone_bricks
setblock ~39 ~17 ~9 minecraft:stone_bricks
setblock ~39 ~17 ~10 minecraft:cracked_stone_bricks
fill ~39 ~17 ~11 ~39 ~17 ~12 minecraft:stone_bricks
setblock ~39 ~17 ~13 minecraft:cobblestone
setblock ~39 ~17 ~14 minecraft:cracked_stone_bricks
fill ~39 ~17 ~15 ~39 ~17 ~16 minecraft:stone_bricks
setblock ~39 ~17 ~17 minecraft:mossy_stone_bricks
setblock ~39 ~17 ~18 minecraft:stone_bricks
setblock ~40 ~17 ~8 minecraft:stone_bricks
setblock ~40 ~17 ~9 minecraft:cracked_stone_bricks
setblock ~40 ~17 ~10 minecraft:stone_bricks
fill ~40 ~17 ~11 ~40 ~17 ~12 minecraft:cobblestone
fill ~40 ~17 ~14 ~40 ~17 ~15 minecraft:stone_bricks
setblock ~40 ~17 ~16 minecraft:cobblestone
fill ~40 ~17 ~17 ~40 ~17 ~18 minecraft:stone_bricks
fill ~41 ~17 ~8 ~41 ~17 ~9 minecraft:mossy_stone_bricks
fill ~41 ~17 ~10 ~41 ~17 ~11 minecraft:cracked_stone_bricks
setblock ~41 ~17 ~12 minecraft:stone_bricks
setblock ~41 ~17 ~13 minecraft:cobblestone
setblock ~41 ~17 ~14 minecraft:cracked_stone_bricks
fill ~41 ~17 ~15 ~41 ~17 ~17 minecraft:stone_bricks
setblock ~41 ~17 ~18 minecraft:cobblestone

# the ladder up the east wall: (40, 1..18, 13) against x 41, through the floors to the roof platform
fill ~40 ~1 ~13 ~40 ~18 ~13 minecraft:ladder[facing=west]

# the door in the west face: (31, 1..3, 12..14) under a dark oak lintel and posts; the doorway to the balcony at (31, 7..8, 13); slits and windows
fill ~31 ~1 ~11 ~31 ~3 ~11 minecraft:dark_oak_log[axis=y]
fill ~31 ~1 ~12 ~31 ~3 ~14 minecraft:air
fill ~31 ~1 ~15 ~31 ~3 ~15 minecraft:dark_oak_log[axis=y]
fill ~34 ~2 ~8 ~34 ~3 ~8 minecraft:iron_bars[north=true,east=true,south=false,west=true]
fill ~34 ~2 ~18 ~34 ~3 ~18 minecraft:iron_bars[north=false,east=true,south=true,west=true]
fill ~38 ~2 ~8 ~38 ~3 ~8 minecraft:iron_bars[north=true,east=true,south=false,west=true]
fill ~38 ~2 ~18 ~38 ~3 ~18 minecraft:iron_bars[north=false,east=true,south=true,west=true]
fill ~41 ~2 ~10 ~41 ~3 ~10 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~41 ~2 ~16 ~41 ~3 ~16 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~31 ~4 ~11 ~31 ~4 ~15 minecraft:dark_oak_log[axis=z]
fill ~31 ~7 ~13 ~31 ~8 ~13 minecraft:air
fill ~31 ~8 ~10 ~31 ~9 ~10 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~31 ~8 ~16 ~31 ~9 ~16 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~34 ~8 ~8 ~34 ~9 ~8 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~34 ~8 ~18 ~34 ~9 ~18 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~38 ~8 ~8 ~38 ~9 ~8 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~38 ~8 ~18 ~38 ~9 ~18 minecraft:iron_bars[north=false,east=true,south=false,west=true]
fill ~41 ~8 ~10 ~41 ~9 ~10 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~41 ~8 ~16 ~41 ~9 ~16 minecraft:iron_bars[north=true,east=false,south=true,west=false]
fill ~31 ~14 ~10 ~31 ~15 ~10 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~31 ~14 ~16 ~31 ~15 ~16 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~34 ~14 ~8 ~34 ~15 ~8 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~34 ~14 ~18 ~34 ~15 ~18 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~36 ~14 ~8 ~36 ~15 ~8 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~36 ~14 ~18 ~36 ~15 ~18 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~38 ~14 ~8 ~38 ~15 ~8 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~38 ~14 ~18 ~38 ~15 ~18 minecraft:glass_pane[north=false,east=true,south=false,west=true]
fill ~41 ~14 ~10 ~41 ~15 ~10 minecraft:glass_pane[north=true,east=false,south=true,west=false]
fill ~41 ~14 ~16 ~41 ~15 ~16 minecraft:glass_pane[north=true,east=false,south=true,west=false]

# the balcony on corbels over the door: a stone platform x 29..30, z 12..14 at y 6, stairs under it at y 5, a rail of stone brick walls
fill ~29 ~5 ~12 ~30 ~5 ~14 minecraft:stone_brick_stairs[facing=east,half=top]
setblock ~29 ~6 ~12 minecraft:cobblestone
setblock ~29 ~6 ~13 minecraft:cracked_stone_bricks
setblock ~29 ~6 ~14 minecraft:stone_bricks
fill ~30 ~6 ~12 ~30 ~6 ~13 minecraft:cracked_stone_bricks
setblock ~30 ~6 ~14 minecraft:mossy_stone_bricks
setblock ~29 ~7 ~11 minecraft:stone_brick_wall[north=none,east=low,south=low,west=none,up=true]
fill ~29 ~7 ~12 ~29 ~7 ~14 minecraft:stone_brick_wall[north=low,east=none,south=low,west=none,up=false]
setblock ~29 ~7 ~15 minecraft:stone_brick_wall[north=low,east=low,south=none,west=none,up=true]
setblock ~30 ~7 ~11 minecraft:stone_brick_wall[north=none,east=low,south=none,west=low,up=false]
setblock ~30 ~7 ~15 minecraft:stone_brick_wall[north=none,east=low,south=none,west=low,up=false]

# the parapet of the roof platform: y 18 all round, merlons at y 19; the four turrets (3 x 3, y 18..20) with doors onto the platform, slits and tile roofs at y 21
fill ~31 ~18 ~8 ~32 ~18 ~8 minecraft:deepslate_bricks
fill ~31 ~18 ~9 ~31 ~18 ~10 minecraft:deepslate_bricks
setblock ~31 ~18 ~11 minecraft:stone_bricks
setblock ~31 ~18 ~12 minecraft:cracked_stone_bricks
fill ~31 ~18 ~13 ~31 ~18 ~14 minecraft:stone_bricks
fill ~31 ~18 ~15 ~31 ~18 ~17 minecraft:cracked_deepslate_bricks
fill ~31 ~18 ~18 ~31 ~19 ~18 minecraft:deepslate_bricks
fill ~32 ~18 ~9 ~33 ~19 ~9 minecraft:air
fill ~32 ~18 ~10 ~33 ~19 ~10 minecraft:deepslate_bricks
fill ~32 ~18 ~16 ~33 ~18 ~16 minecraft:deepslate_bricks
fill ~32 ~18 ~17 ~33 ~19 ~17 minecraft:air
fill ~32 ~18 ~18 ~33 ~18 ~18 minecraft:cracked_deepslate_bricks
setblock ~33 ~18 ~8 minecraft:cracked_deepslate_bricks
setblock ~34 ~18 ~8 minecraft:stone_bricks
fill ~34 ~18 ~18 ~34 ~19 ~18 minecraft:deepslate_bricks
fill ~35 ~18 ~8 ~38 ~18 ~8 minecraft:deepslate_bricks
setblock ~35 ~18 ~18 minecraft:cracked_deepslate_bricks
fill ~36 ~18 ~18 ~38 ~18 ~18 minecraft:stone_bricks
setblock ~39 ~18 ~8 minecraft:cracked_deepslate_bricks
fill ~39 ~18 ~9 ~40 ~19 ~9 minecraft:air
fill ~39 ~18 ~10 ~39 ~20 ~10 minecraft:deepslate_bricks
setblock ~39 ~18 ~16 minecraft:deepslate_bricks
fill ~39 ~18 ~17 ~40 ~19 ~17 minecraft:air
setblock ~39 ~18 ~18 minecraft:polished_blackstone_bricks
setblock ~40 ~18 ~8 minecraft:deepslate_bricks
setblock ~40 ~18 ~10 minecraft:cracked_deepslate_bricks
setblock ~40 ~18 ~16 minecraft:cracked_deepslate_bricks
setblock ~40 ~18 ~18 minecraft:cracked_deepslate_bricks
setblock ~41 ~18 ~8 minecraft:cracked_deepslate_bricks
setblock ~41 ~18 ~9 minecraft:deepslate_bricks
fill ~41 ~18 ~10 ~41 ~18 ~14 minecraft:stone_bricks
setblock ~41 ~18 ~15 minecraft:cracked_stone_bricks
setblock ~41 ~18 ~16 minecraft:deepslate_bricks
setblock ~41 ~18 ~17 minecraft:polished_blackstone_bricks
setblock ~41 ~18 ~18 minecraft:deepslate_bricks
setblock ~31 ~19 ~8 minecraft:deepslate_bricks
setblock ~31 ~19 ~9 minecraft:air
fill ~31 ~19 ~10 ~31 ~20 ~10 minecraft:stone_bricks
setblock ~31 ~19 ~11 minecraft:deepslate_bricks
setblock ~31 ~19 ~13 minecraft:deepslate_bricks
fill ~31 ~19 ~15 ~31 ~19 ~16 minecraft:deepslate_bricks
setblock ~31 ~19 ~17 minecraft:air
setblock ~32 ~19 ~8 minecraft:air
setblock ~32 ~19 ~16 minecraft:cracked_deepslate_bricks
setblock ~32 ~19 ~18 minecraft:air
fill ~33 ~19 ~8 ~34 ~19 ~8 minecraft:deepslate_bricks
setblock ~33 ~19 ~16 minecraft:stone_bricks
setblock ~33 ~19 ~18 minecraft:deepslate_bricks
setblock ~36 ~19 ~8 minecraft:deepslate_bricks
setblock ~36 ~19 ~18 minecraft:deepslate_bricks
fill ~38 ~19 ~8 ~39 ~19 ~8 minecraft:deepslate_bricks
fill ~38 ~19 ~18 ~39 ~19 ~18 minecraft:cracked_deepslate_bricks
setblock ~39 ~19 ~16 minecraft:polished_blackstone_bricks
setblock ~40 ~19 ~8 minecraft:air
fill ~40 ~19 ~10 ~41 ~19 ~10 minecraft:deepslate_bricks
setblock ~40 ~19 ~16 minecraft:deepslate_bricks
setblock ~40 ~19 ~18 minecraft:air
setblock ~41 ~19 ~8 minecraft:stone_bricks
setblock ~41 ~19 ~9 minecraft:air
setblock ~41 ~19 ~11 minecraft:cracked_deepslate_bricks
setblock ~41 ~19 ~13 minecraft:deepslate_bricks
setblock ~41 ~19 ~15 minecraft:deepslate_bricks
setblock ~41 ~19 ~16 minecraft:cracked_deepslate_bricks
setblock ~41 ~19 ~17 minecraft:air
setblock ~41 ~19 ~18 minecraft:cracked_deepslate_bricks
setblock ~31 ~20 ~8 minecraft:cracked_deepslate_bricks
setblock ~31 ~20 ~9 minecraft:stone_bricks
fill ~31 ~20 ~16 ~33 ~20 ~16 minecraft:deepslate_bricks
setblock ~31 ~20 ~17 minecraft:polished_blackstone_bricks
setblock ~31 ~20 ~18 minecraft:cracked_deepslate_bricks
setblock ~32 ~20 ~8 minecraft:deepslate_bricks
setblock ~32 ~20 ~9 minecraft:air
setblock ~32 ~20 ~10 minecraft:polished_blackstone_bricks
setblock ~32 ~20 ~17 minecraft:air
setblock ~32 ~20 ~18 minecraft:stone_bricks
setblock ~33 ~20 ~8 minecraft:cracked_deepslate_bricks
setblock ~33 ~20 ~9 minecraft:stone_bricks
setblock ~33 ~20 ~10 minecraft:deepslate_bricks
fill ~33 ~20 ~17 ~33 ~20 ~18 minecraft:polished_blackstone_bricks
fill ~39 ~20 ~8 ~39 ~20 ~9 minecraft:deepslate_bricks
setblock ~39 ~20 ~16 minecraft:deepslate_bricks
fill ~39 ~20 ~17 ~39 ~20 ~18 minecraft:cracked_deepslate_bricks
fill ~40 ~20 ~8 ~41 ~20 ~8 minecraft:cracked_deepslate_bricks
setblock ~40 ~20 ~9 minecraft:air
setblock ~40 ~20 ~10 minecraft:cracked_deepslate_bricks
setblock ~40 ~20 ~16 minecraft:polished_blackstone_bricks
setblock ~40 ~20 ~17 minecraft:air
fill ~40 ~20 ~18 ~41 ~20 ~18 minecraft:deepslate_bricks
setblock ~41 ~20 ~9 minecraft:stone_bricks
setblock ~41 ~20 ~10 minecraft:polished_blackstone_bricks
setblock ~41 ~20 ~16 minecraft:deepslate_bricks
setblock ~41 ~20 ~17 minecraft:polished_blackstone_bricks
fill ~31 ~21 ~8 ~31 ~21 ~10 minecraft:deepslate_tile_stairs[facing=east,half=bottom]
fill ~31 ~21 ~16 ~31 ~21 ~18 minecraft:deepslate_tile_stairs[facing=east,half=bottom]
setblock ~32 ~21 ~8 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~32 ~21 ~9 minecraft:deepslate_tiles
setblock ~32 ~21 ~10 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~32 ~21 ~16 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~32 ~21 ~17 minecraft:deepslate_tiles
setblock ~32 ~21 ~18 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~33 ~21 ~8 ~33 ~21 ~10 minecraft:deepslate_tile_stairs[facing=west,half=bottom]
fill ~33 ~21 ~16 ~33 ~21 ~18 minecraft:deepslate_tile_stairs[facing=west,half=bottom]
fill ~39 ~21 ~8 ~39 ~21 ~10 minecraft:deepslate_tile_stairs[facing=east,half=bottom]
fill ~39 ~21 ~16 ~39 ~21 ~18 minecraft:deepslate_tile_stairs[facing=east,half=bottom]
setblock ~40 ~21 ~8 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~40 ~21 ~9 minecraft:deepslate_tiles
setblock ~40 ~21 ~10 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
setblock ~40 ~21 ~16 minecraft:deepslate_tile_stairs[facing=south,half=bottom]
setblock ~40 ~21 ~17 minecraft:deepslate_tiles
setblock ~40 ~21 ~18 minecraft:deepslate_tile_stairs[facing=north,half=bottom]
fill ~41 ~21 ~8 ~41 ~21 ~10 minecraft:deepslate_tile_stairs[facing=west,half=bottom]
fill ~41 ~21 ~16 ~41 ~21 ~18 minecraft:deepslate_tile_stairs[facing=west,half=bottom]

# inside the donjon: the ground hall with pillars, barrels and hay; the first floor with a long table, beds and chests; the second with a rack of weapons and a brazier
fill ~32 ~1 ~9 ~32 ~2 ~9 minecraft:barrel[facing=up]
setblock ~32 ~1 ~10 minecraft:hay_block[axis=y]
setblock ~32 ~1 ~16 minecraft:hay_block[axis=y]
setblock ~32 ~1 ~17 minecraft:barrel[facing=up]
setblock ~33 ~1 ~9 minecraft:hay_block[axis=y]
setblock ~33 ~1 ~17 minecraft:hay_block[axis=y]
fill ~34 ~1 ~11 ~34 ~5 ~11 minecraft:deepslate_bricks
fill ~34 ~1 ~15 ~34 ~5 ~15 minecraft:deepslate_bricks
fill ~38 ~1 ~11 ~38 ~5 ~11 minecraft:deepslate_bricks
fill ~38 ~1 ~15 ~38 ~5 ~15 minecraft:deepslate_bricks
setblock ~39 ~1 ~9 minecraft:hay_block[axis=y]
fill ~39 ~1 ~17 ~40 ~1 ~17 minecraft:barrel[facing=up]
setblock ~40 ~1 ~9 minecraft:hay_block[axis=x]
setblock ~36 ~4 ~13 minecraft:lantern[hanging=true]
setblock ~36 ~5 ~13 minecraft:chain[axis=y]
setblock ~32 ~7 ~9 minecraft:gray_bed[facing=east,part=foot]
setblock ~33 ~7 ~9 minecraft:gray_bed[facing=east,part=head]
setblock ~34 ~7 ~16 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~34 ~7 ~17 minecraft:spruce_fence[north=false,east=true,south=true,west=false]
fill ~35 ~7 ~17 ~37 ~7 ~17 minecraft:spruce_fence[north=false,east=true,south=true,west=true]
setblock ~36 ~7 ~16 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~38 ~7 ~16 minecraft:spruce_stairs[facing=south,half=bottom]
setblock ~38 ~7 ~17 minecraft:spruce_fence[north=false,east=false,south=true,west=true]
fill ~39 ~7 ~9 ~39 ~7 ~10 minecraft:barrel[facing=up]
fill ~34 ~8 ~17 ~38 ~8 ~17 minecraft:spruce_pressure_plate
setblock ~36 ~10 ~13 minecraft:lantern[hanging=true]
setblock ~36 ~11 ~13 minecraft:chain[axis=y]
setblock ~32 ~13 ~10 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
fill ~32 ~13 ~11 ~32 ~14 ~11 minecraft:iron_bars[north=false,east=false,south=false,west=true]
setblock ~32 ~13 ~12 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
fill ~32 ~13 ~13 ~32 ~14 ~13 minecraft:iron_bars[north=false,east=false,south=false,west=true]
setblock ~32 ~13 ~14 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
fill ~32 ~13 ~15 ~32 ~14 ~15 minecraft:iron_bars[north=false,east=false,south=false,west=true]
setblock ~32 ~13 ~16 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
fill ~38 ~13 ~10 ~39 ~13 ~10 minecraft:barrel[facing=up]
setblock ~38 ~13 ~16 minecraft:hay_block[axis=x]

# the courtyards: a well on the west bank, a lean-to against the west wall with barrels and hay, a rack of weapons by the south wall, drill dummies on the east bank, lantern posts at the deck
fill ~3 ~1 ~10 ~3 ~1 ~12 minecraft:hay_block[axis=x]
fill ~3 ~1 ~13 ~4 ~1 ~14 minecraft:hay_block[axis=y]
setblock ~3 ~1 ~15 minecraft:hay_block[axis=x]
setblock ~3 ~1 ~16 minecraft:barrel[facing=up]
setblock ~4 ~1 ~10 minecraft:barrel[facing=up]
fill ~5 ~1 ~9 ~5 ~4 ~9 minecraft:dark_oak_log[axis=y]
fill ~5 ~1 ~13 ~5 ~4 ~13 minecraft:dark_oak_log[axis=y]
fill ~5 ~1 ~17 ~5 ~4 ~17 minecraft:dark_oak_log[axis=y]
setblock ~12 ~1 ~21 minecraft:spruce_fence[north=false,east=true,south=false,west=false]
fill ~13 ~1 ~18 ~15 ~1 ~18 minecraft:cobblestone
fill ~13 ~1 ~19 ~13 ~2 ~19 minecraft:cobblestone
fill ~13 ~1 ~20 ~15 ~1 ~20 minecraft:mossy_cobblestone
fill ~13 ~1 ~21 ~15 ~1 ~21 minecraft:spruce_fence[north=true,east=true,south=false,west=true]
setblock ~14 ~1 ~19 minecraft:water
fill ~15 ~1 ~19 ~15 ~2 ~19 minecraft:cobblestone
setblock ~16 ~1 ~21 minecraft:spruce_fence[north=false,east=false,south=false,west=true]
fill ~20 ~1 ~5 ~20 ~3 ~5 minecraft:dark_oak_log[axis=y]
fill ~20 ~1 ~21 ~20 ~3 ~21 minecraft:dark_oak_log[axis=y]
fill ~26 ~1 ~5 ~26 ~3 ~5 minecraft:dark_oak_log[axis=y]
fill ~26 ~1 ~21 ~26 ~3 ~21 minecraft:dark_oak_log[axis=y]
setblock ~33 ~1 ~5 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~33 ~1 ~21 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~37 ~1 ~5 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~37 ~1 ~21 minecraft:oak_fence[north=false,east=false,south=false,west=false]
setblock ~42 ~1 ~21 minecraft:barrel[facing=up]
fill ~43 ~1 ~5 ~43 ~1 ~6 minecraft:barrel[facing=up]
setblock ~43 ~1 ~7 minecraft:hay_block[axis=y]
setblock ~43 ~1 ~19 minecraft:hay_block[axis=y]
fill ~43 ~1 ~20 ~43 ~1 ~21 minecraft:barrel[facing=up]
setblock ~3 ~2 ~10 minecraft:barrel[facing=up]
fill ~3 ~2 ~13 ~3 ~2 ~14 minecraft:hay_block[axis=y]
fill ~13 ~2 ~18 ~14 ~2 ~18 minecraft:cobblestone
setblock ~13 ~2 ~20 minecraft:mossy_cobblestone
setblock ~13 ~2 ~21 minecraft:iron_bars[north=true,east=false,south=false,west=false]
fill ~14 ~2 ~20 ~15 ~2 ~20 minecraft:cobblestone
setblock ~15 ~2 ~18 minecraft:mossy_cobblestone
setblock ~15 ~2 ~21 minecraft:iron_bars[north=true,east=false,south=false,west=false]
setblock ~32 ~2 ~5 minecraft:oak_fence[north=false,east=true,south=false,west=false]
setblock ~32 ~2 ~21 minecraft:oak_fence[north=false,east=true,south=false,west=false]
setblock ~33 ~2 ~5 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~33 ~2 ~21 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~34 ~2 ~5 minecraft:oak_fence[north=false,east=false,south=false,west=true]
setblock ~34 ~2 ~21 minecraft:oak_fence[north=false,east=false,south=false,west=true]
setblock ~36 ~2 ~5 minecraft:oak_fence[north=false,east=true,south=false,west=false]
setblock ~36 ~2 ~21 minecraft:oak_fence[north=false,east=true,south=false,west=false]
setblock ~37 ~2 ~5 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~37 ~2 ~21 minecraft:oak_fence[north=false,east=true,south=false,west=true]
setblock ~38 ~2 ~5 minecraft:oak_fence[north=false,east=false,south=false,west=true]
setblock ~38 ~2 ~21 minecraft:oak_fence[north=false,east=false,south=false,west=true]
setblock ~43 ~2 ~5 minecraft:barrel[facing=up]
fill ~13 ~3 ~18 ~13 ~4 ~18 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~13 ~3 ~20 ~13 ~4 ~20 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~14 ~3 ~19 minecraft:lantern[hanging=true]
fill ~15 ~3 ~18 ~15 ~4 ~18 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
fill ~15 ~3 ~20 ~15 ~4 ~20 minecraft:dark_oak_fence[north=false,east=false,south=false,west=false]
setblock ~33 ~3 ~5 minecraft:hay_block[axis=y]
setblock ~33 ~3 ~21 minecraft:hay_block[axis=y]
setblock ~37 ~3 ~5 minecraft:hay_block[axis=y]
setblock ~37 ~3 ~21 minecraft:hay_block[axis=y]
setblock ~14 ~4 ~19 minecraft:chain[axis=y]
setblock ~20 ~4 ~5 minecraft:lantern[hanging=false]
setblock ~20 ~4 ~21 minecraft:lantern[hanging=false]
setblock ~26 ~4 ~5 minecraft:lantern[hanging=false]
setblock ~26 ~4 ~21 minecraft:lantern[hanging=false]
fill ~3 ~5 ~9 ~5 ~5 ~17 minecraft:spruce_slab[type=bottom]
fill ~13 ~5 ~18 ~15 ~5 ~20 minecraft:dark_oak_slab[type=bottom]
