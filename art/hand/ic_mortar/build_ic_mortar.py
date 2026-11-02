#!/usr/bin/env python3
"""The Ironhold Mortar (troop id ic_mortar), hand-authored at one texel per model pixel.

A scorch-bearded gunner in an iron cap with goggles pushed up on the brow, a leather apron over mail and lames and a
big left spaulder, lugging a brass-hooped mortar tube slung diagonally across his back, its ember-lit bore at the
right shoulder, a shell bandolier across the chest and a powder keg at the small of his back. Tier I only.

  python build_ic_mortar.py preview <dir> [side] | contact <png> [side] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "ic_ballista"))
from siege_kit import *   # noqa: F401,F403

SKIN = (128, 128)
CLOTH = (128, 64)

def tube(face, w, h):
    """The barrel: gunmetal with brass hoops; the top face is the bore, a ring of iron round an ember core."""
    if face == "top":
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                ring = x in (0, w - 1) or y in (0, h - 1)
                g[y][x] = IRON[5] if ring else EMBER[5 if (x, y) in ((2, 2), (1, 1)) else 4]
        return g
    p = hooped(iron_plain(3, "tube", 3), [(0, 2), (7, 2), (14, 3)], along="y", R=BRASS)
    return p(face, w, h)

def bore_glow(face, w, h):
    g = blank(w, h)
    if face == "top":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = EMBER[5 if (x + y) % 2 else 4]
    return g

def shell(face, w, h):
    return brass(4, seed="shell")(face, w, h)

def keg(face, w, h):
    return hooped(grain(OAK, "y", 3, "keg", knots=1), [(0, 1), (2, 1), (h - 3, 1), (h - 1, 1)], along="y", R=IRON)(face, w, h)

def beard(face, w, h):
    g = fur(R=OILED, hang=True, seed="beard")(face, w, h)
    if face == "front":
        for x in range(w):
            if g[h - 1][x] is not None and x % 2 == 0: g[h - 1][x] = EMBER[3]    # singed tips
    return g

def lens(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w): g[y][x] = EMBER[5 if (face == "front" and (x, y) == (0, 0)) else 4]
    return g

def bones():
    B = crew_bones()
    B.append(Bone("apron", "body", tag="apron", boxes=[Box((-4, -9, -2), (8, 9, 4), leather_ic(2, stitch=True), dil=0.55)]))
    B.append(Bone("beard", "head", boxes=[Box((-3.5, -3.5, -4.6), (7, 4, 1.2), beard), Box((-1, -1, -5.1), (2, 2, 1), beard)]))
    B.append(Bone("goggles", "head", tag="helm", boxes=[
        Box((-3.5, -8.7, -5.2), (7, 2, 1), brass(3, seed="gog")),
        Box((-3, -8.5, -5.5), (2, 1.5, 0.5), lens), Box((1, -8.5, -5.5), (2, 1.5, 0.5), lens)]))
    B.append(Bone("keg", "body", boxes=[Box((-3, -4, 2.2), (6, 6, 4), keg), Box((-2.5, -4.5, 2.6), (5, 1, 3), iron_plain(4, "kegcap", 1))]))
    B.append(Bone("tube", "body", (-1, -5, 4.2), rot=(0, 0, -34), tag="siege",
                  boxes=[Box((-2.5, -10, -2.5), (5, 18, 5), tube), Box((-3, 8, -3), (6, 2, 6), brass(3, seed="breech"))]))
    B.append(Bone("sling", "body", (0, -5.5, -2.1), rot=(0, 0, 38), boxes=[Box((-0.75, -9.5, -0.25), (1.5, 19, 0.6), leather_ic(1, stitch=True), dil=0.1)]))
    for i, (x, y) in enumerate(((-3.1, -9.5), (-0.6, -6.2), (1.9, -2.9))):
        B.append(Bone(f"shell_{i}", "body", boxes=[Box((x, y, -3.3), (2, 3, 1.4), shell)]))
    return B

def cloth_painter(spec, side): return crew_cloth(spec, side)

def glow(bone, box):
    return bore_glow if bone.name == "tube" and box.size == [5, 18, 5] else None

IC_MORTAR = Troop("ic_mortar", SKIN, CLOTH, bones, cloth_painter,
                  {"weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "tall 4", "faces": {"count": 4, "card": 0}},
                  tiers=(1,), glow=glow)
def build(): return IC_MORTAR.build()
def outputs(): return IC_MORTAR.outputs()

if __name__ == "__main__":
    IC_MORTAR.main(sys.argv)
