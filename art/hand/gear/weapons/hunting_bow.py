"""Frostmarch Longbow (hunting_bow): a tall yew self bow, each limb four lashed staves curving back to bone nocks, the
string at the belly side (+x, toward the archer when held). Rarity sits on the bone collars, gems above them, and at
Legendary gold horn tips."""
from weaponlib import *
from frostmarch_hunt import BONEM, FROST

LEN, LEAN, WIDE = (3.2, 3.2, 3, 2.8), (6, 14, 24, 34), (2.0, 1.8, 1.5, 1.2)

def make(r, side):
    paints = [bands(YEW, 3, 3, BONEM, 2), grain(YEW, 3), grain(YEW, 3, salt=4), grain(YEW, 3, salt=2)]
    a = accent(r)
    col = stone(a[0]) if a and r <= RARE else tone(BONEM, 3, lead=False)
    c = [Cube("grip", [-1, -2.5, -1], [1, 2.5, 1], "leather", wound(LEATH, 2))]
    c += chain("up", (0, 2.45), LEN, LEAN, WIDE, 1.2, "wood", paints); tx, ty = chain.end
    c += chain("dn", (0, 2.45), LEN, LEAN, WIDE, 1.2, "wood", paints, mirror=True)
    if r >= UNCOMMON:
        c += [Cube("collar_u", [-1.15, 2.4, -0.8], [1.15, 3.2, 0.8], "bone", col),
              Cube("collar_d", [-1.15, -3.2, -0.8], [1.15, -2.4, 0.8], "bone", col)]
    if r >= RARE:
        for seg in (c[2], c[6]):
            cx, cy = seg.pivot[0], seg.pivot[1]
            c.append(Cube(seg.name + "_wrap", [cx - 1.15, cy - 0.7, -0.75], [cx + 1.15, cy + 0.7, 0.75], "bone",
                          bands(BONEM, 2, 2, LEATH, 2), turn=seg.turn, pivot=[cx, cy, 0]))
    if r >= EPIC:
        c.append(Cube("plate", [-1.6, -4.5, -0.6], [-0.95, 4.5, 0.6], "gilt" if r == LEGEND else "bone",
                      tone(GOLD if r == LEGEND else BONEM, 3, lead=False)))
    T, up = (GOLD, 1.6) if r == LEGEND else (BONEM, 0.9)
    c += [Cube("nock_u", [tx - 0.75, ty - 0.6, -0.6], [tx + 0.6, ty + up, 0.6], "bone", tone(T, 3, lead=False), turn=-34, pivot=[tx, ty, 0]),
          Cube("nock_d", [tx - 0.75, -ty - up, -0.6], [tx + 0.6, -ty + 0.6, 0.6], "bone", tone(T, 3, lead=False), turn=34, pivot=[tx, -ty, 0])]
    sx = tx + 0.25
    c.append(Cube("string", [sx - 0.3, -ty, -0.3], [sx + 0.3, ty, 0.3], "cloth", tone(FROST, 1, lead=False)))
    if r == EPIC: c += bead("gem", [-1.85, -0.6, -0.4], [-1.55, 0.6, 0.4], r)
    if r == LEGEND:
        c += [Cube("horn_u", [tx - 1.5, ty - 2.2, -0.5], [tx - 0.6, ty - 0.4, 0.5], "gilt", tone(GOLD, 3, lead=False), turn=-34, pivot=[tx, ty, 0]),
              Cube("horn_d", [tx - 1.5, -ty + 0.4, -0.5], [tx - 0.6, -ty + 2.2, 0.5], "gilt", tone(GOLD, 3, lead=False), turn=34, pivot=[tx, -ty, 0])]
    return c

def weapon(): return Weapon("hunting_bow", make, "pole", note="tall yew limbs, sinew lashing, bone nocks; the string toward the archer")
