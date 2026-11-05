#!/usr/bin/env python3
"""The Ironhold Mortar (troop id ic_mortar), hand-authored at one texel per model pixel.

A scorch-bearded gunner in an iron cap with goggles pushed up on the brow, calm mail and plate under a leather apron
and a big left spaulder. A squat brass-hooped gunmetal mortar tube (iron lip, ember bore) rides on a pack board on
his back, tilted so the muzzle rises above the right shoulder; a leather sling runs from shoulder to hip and meets
the tube, and a powder keg sits against the small of his back. Tier II (the
Bombardier): a brass cap band, a fourth spaulder lame with a brass boss, bracers, knee plates, and brass hoops on the tube.
Tier III (the Thunder Master): spaulder spikes, greaves, an ember-seamed comb, brass trunnions and an ember band on the tube.
No back flag.

  python build_ic_mortar.py preview <dir> [side] | contact <png> [side] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "ic_ballista"))
from siege_kit import *   # noqa: F401,F403
sys.path.insert(0, os.path.join(HERE, "..", "common"))
import house_stub
import ironclad_tiers as IT

SKIN = (128, 128)
CLOTH = (128, 64)

def barrel(rows_hoop, R=IRON, base=3, seed="tube"):
    """A short gunmetal barrel with brass hoops on the listed rows: bold bands, a lit edge, no noise."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"):
                    g[y][x] = clamp(R, base + LIT[face] + (1 if x == 0 else 0)); continue
                lx = 1 if x == 0 else (-1 if x == w - 1 else 0)
                if face in ("back", "left"): lx = -abs(lx)
                if y in rows_hoop:
                    first = y == min(rows_hoop); last = y == max(rows_hoop)
                    g[y][x] = clamp(BRASS, 4 + lx + (1 if first else 0) - (2 if last else 0))
                elif y - 1 in rows_hoop and y - 1 == max(rows_hoop):
                    g[y][x] = clamp(R, 1 + max(lx, 0))                    # the shadow under a hoop
                else:
                    g[y][x] = clamp(R, base + lx + (1 if y == 0 else 0))
        return g
    return paint

def tube_body(face, w, h):
    return barrel({1, 2, 9, 10}, IRON, 3)(face, w, h)

def lip(face, w, h):
    """The muzzle lip: bright iron, an ember line round it; the top face is the bore, an iron ring round an ember core."""
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face == "top":
                ring = x in (0, w - 1) or y in (0, h - 1)
                core = abs(x - w // 2) + abs(y - h // 2) <= 1
                g[y][x] = IRON[5] if ring else (IRON[1] if x in (1, w - 2) or y in (1, h - 2) else EMBER[5 if core else 4])
            elif face == "bottom":
                g[y][x] = IRON[2]
            else:
                g[y][x] = EMBER[3] if y == h - 1 else IRON[5 if y == 0 else 4]
    return g

def bore_glow(face, w, h):
    g = blank(w, h)
    if face == "top":
        for y in range(2, h - 2):
            for x in range(2, w - 2): g[y][x] = EMBER[5 if abs(x - w // 2) + abs(y - h // 2) <= 1 else 4]
    if face not in ("top", "bottom"):
        for x in range(w): g[h - 1][x] = EMBER[3]
    return g

def breech(face, w, h):
    return brass(4, seed="breech")(face, w, h)

def keg(face, w, h):
    return hooped(grain(OAK, "y", 3, "keg", knots=0), [(0, 1), (h - 1, 1)], along="y", R=IRON)(face, w, h)

def buckle(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            edge = x in (0, w - 1) or y in (0, h - 1)
            g[y][x] = BRASS[5 if (x, y) == (0, 0) else (4 if edge else 2)]
    return g

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
    # the carry: a leather pack board on the back, the tube lashed to it on a slant, muzzle up past the right shoulder
    B.append(Bone("board", "body", tag="siege", boxes=[Box((-3.5, -8, 2.55), (7, 8, 3), leather_ic(2, stitch=True))]))
    for t in (1, 2, 3):
        bx = [Box((-3.5, -12, -3.5), (7, 12, 7), tube_body), Box((-3.5, -14, -3.5), (7, 2, 7), lip, dil=0.35), Box((-3.5, 0, -3.5), (7, 2, 7), breech, dil=0.35)]
        if t >= 2:   # brass hoops round the barrel
            bx += [Box((-4, -9, -4), (8, 1, 8), IT.brass(3, "mh1")), Box((-4, -4, -4), (8, 1, 8), IT.brass(3, "mh2"))]
        if t >= 3:   # brass trunnions and an ember band under the lip
            bx += [Box((-5, -7, -1), (1.5, 2, 2), IT.brass(4, "mt1")), Box((3.5, -7, -1), (1.5, 2, 2), IT.brass(4, "mt2")),
                   Box((-4, -11.5, -4), (8, 1, 8), IT.ember_line(IT.IRON, 3))]
        B.append(Bone("tube" if t == 1 else f"tube_t{t}", "body", (-0.5, -7.5, 9.05), rot=(0, 0, -22), tag="siege", weapon_tiers=[t], boxes=bx))
    B.append(Bone("keg", "body", tag="siege", boxes=[Box((-3, -0.5, 2.55), (6, 6, 3), keg), Box((-2.5, -1, 2.55), (5, 1, 3), iron_plain(4, "kegcap", 1))]))
    B.append(Bone("sling", "body", (0.4, -4.5, 0), rot=(0, 0, -38), tag="strap", boxes=[
        Box((-1, -4.5, -3.5), (2, 9, 1), leather_ic(1, stitch=False))]))
    B.append(Bone("buckle", "body", tag="strap", boxes=[Box((2, -3, -3.6), (2, 2, 1), buckle)]))
    IT.crew_tiers(B, "mo", bevor=False)
    return B

NOTE = ("Ironhold Mortar (unit identity, Ironclad): a scorch-bearded gunner, iron cap, goggles, leather apron, a brass-hooped mortar "
        "tube on a back pack board and a powder keg. Tier II brass cap band, spaulder lame and boss, bracers, knee plates, tube hoops; "
        "tier III spaulder spikes, greaves, an ember comb, brass trunnions. No back flag.")

def cloth_painter(spec, side): return crew_cloth(spec, side)

def glow(bone, box):
    return bore_glow if bone.name.startswith("tube") and box.size == [7, 2, 7] and box.dil else None

IC_MORTAR = Troop("ic_mortar", SKIN, CLOTH, bones, cloth_painter,
                  {"weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "tall 4", "faces": {"count": 4, "card": 0}},
                  glow=glow)
def build(): return IC_MORTAR.build()
def outputs(): return IC_MORTAR.outputs()

if __name__ == "__main__":
    house_stub.main(IC_MORTAR, NOTE, sys.argv)
