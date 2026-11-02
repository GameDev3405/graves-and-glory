#!/usr/bin/env python3
"""The Ironclad Ballista Crew (troop id ic_ballista), hand-authored at one texel per model pixel.

A crewman in an iron cap with an ember brow band, mail and riveted lames under a team tabard, standing behind his
engine: an oak carriage on two iron-shod wheels, a rope torsion skein either side of the stock with sprung arms swept
back to a cord, a bolt with a brass head laid in the channel, a windlass drum with a crank bar at his hand, a prop
leg at the muzzle and a team cloth mantlet hung from the carriage. Tier I only: the other armour tiers fall back to it.

  python build_ic_ballista.py preview <dir> [side] | contact <png> [side] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from siege_kit import *   # noqa: F401,F403

SKIN = (128, 128)
CLOTH = (128, 64)

def wheel_face(flip):
    """An 8 x 8 wheel: an iron tyre, oak planking, an iron strap cross and a brass hub."""
    def paint(face, w, h):
        g = blank(w, h)
        if face in ("front", "back", "top", "bottom"):
            return iron_plain(4, "tyre", 2)(face, w, h)
        for y in range(h):
            for x in range(w):
                edge = x in (0, w - 1) or y in (0, h - 1)
                if edge:
                    v = 3 + (1 if (y == 0 or x == 0) else -1)
                    g[y][x] = clamp(IRON, v)
                    continue
                v = 3 + (-1 if x % 2 == 0 else 0) + (1 if y == 1 else 0)
                g[y][x] = clamp(OAK, v)
        for i in range(1, w - 1):
            g[h // 2 - 1][i] = clamp(IRON, 3); g[h // 2][i] = clamp(IRON, 2)
            g[i][w // 2 - 1] = clamp(IRON, 3); g[i][w // 2] = clamp(IRON, 2)
        for y in (h // 2 - 1, h // 2):
            for x in (w // 2 - 1, w // 2): g[y][x] = BRASS[5 if (x, y) == (w // 2 - 1, h // 2 - 1) else 3]
        for c in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)): g[c[1]][c[0]] = IRON[5]   # rivets
        if flip: g = [r[::-1] for r in g]
        return g
    return paint

def bolt_shaft():
    return hooped(grain(OAK, "z", 3, "bolt", knots=0), [(8, 2)], along="z", R=IRON)

def fletch(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w): g[y][x] = clamp(LEATH_IC, 3 + LIT[face] + (1 if (x + y) % 3 == 0 else 0))
    return g

LEATH_IC = OILED

def bones():
    B = crew_bones(plume=True)
    n0 = len(B)
    # carriage: the bed over the axle, the axle, wheels, the stock
    B.append(Bone("carriage", "root", tag="siege", boxes=[
        Box((-9, 15, -15), (18, 4, 5), hooped(grain(OAK, "x", 3, "bed"), [(0, 2), (16, 2)], along="x", R=IRON)),
        Box((-12, 18, -13.5), (24, 2, 2), hooped(iron_plain(3, "axle", 1), [], along="x")),
        Box((-1.5, 12, -24), (3, 3, 20), hooped(grain(OAK, "z", 3, "stock"), [(0, 2), (8, 2), (17, 3)], along="z", R=IRON, trim=BRASS)),
        Box((-5, 12, -12), (10, 3, 2), hooped(grain(OAK, "x", 4, "rear"), [(0, 1), (9, 1)], along="x", R=IRON)),
        Box((-1.5, 15, -26.5), (3, 9, 2), hooped(grain(OAK, "y", 3, "prop"), [(6, 2)], along="y", R=IRON))]))
    for side, x, flip in (("r", -13, False), ("l", 11, True)):
        B.append(Bone(f"wheel_{side}", "root", tag="siege", boxes=[
            Box((x, 15.5, -15), (2, 8, 8), wheel_face(flip))]))
    # the windlass: a drum on the stock and a crank bar a crewman's hand can reach
    B.append(Bone("windlass", "root", tag="siege", boxes=[
        Box((-2.5, 9.5, -10), (5, 5, 3), hooped(rope("x", 3, "drum"), [(0, 1), (4, 1)], along="x", R=IRON)),
        Box((-6.5, 11, -9.5), (13, 2, 2), hooped(iron_plain(3, "crank", 2), [], along="x")),
        Box((-7, 9.5, -10), (1.5, 5, 3), brass(4)), Box((5.5, 9.5, -10), (1.5, 5, 3), brass(4))]))
    # torsion: a rope skein either side of the stock capped in iron, an oak arm swept back to a cord
    for side, c in (("r", -1), ("l", 1)):
        B.append(Bone(f"skein_{side}", "root", tag="siege", boxes=[
            Box((8 * c - 2.5, 10, -27), (5, 8, 5), hooped(rope("y", 3, "skein" + side), [], along="y")),
            Box((8 * c - 3, 9, -27.5), (6, 1.5, 6), iron_plain(4, "skcapt", 2)),
            Box((8 * c - 3, 17.5, -27.5), (6, 1.5, 6), iron_plain(3, "skcapb", 2))
            ]))
        B.append(Bone(f"arm_{side}", "root", (8 * c, 13.5, -24.5), rot=(0, 38 * -c, 0), tag="siege", boxes=[
            Box((-1.25, -1.5, -0.5), (2.5, 3, 8), hooped(grain(OAK, "z", 4, "arm" + side), [(5, 2)], along="z", R=IRON, trim=BRASS))]))
        B.append(Bone(f"cord_{side}", "root", (8 * c - c * 4.7, 13.5, -24.5 + 6.1), rot=(0, -46 * c, 0), tag="siege", boxes=[
            Box((-0.5, -0.5, 0), (1, 1, 6.6), rope("z", 4, "cord"))]))
    B.append(Bone("bolt", "root", tag="siege", boxes=[
        Box((-0.5, 10.5, -26), (1, 1.5, 13), bolt_shaft()),
        Box((-1.5, 9.75, -29), (3, 3, 3), brass(5)),
        Box((-0.5, 10.5, -30), (1, 1.5, 1), brass(6)),
        Box((-2.5, 10.7, -13), (5, 1, 2.5), fletch), Box((-0.5, 8.7, -13), (1, 5, 2.5), fletch)]))
    B.append(Bone("quiver", "body", boxes=[
        Box((-3, -10, 2.2), (6, 9, 3), hooped(grain(OAK, "y", 4, "qv"), [(0, 1), (4, 1), (8, 1)], along="y", R=IRON)),
        Box((-2.5, -11.5, 2.7), (5, 2, 1.5), brass(5, seed="qa"))]))
    B.append(Bone("mantlet", "root", team=True, boxes=[Box((-8, 15, -15.6), (16, 5, 0), "mantlet", sheet="cloth")]))
    for b in [b for b in B[n0:] if b.parent == "root"]:   # nudge the engine back toward the crewman
        if any(b.pivot): b.pivot[2] += 6
        else:
            for bx in b.boxes: bx.origin[2] += 6
    return B

def mantlet(T):
    pcog = {"L": BRASS[5], "m": BRASS[3], "d": IRON[0]}
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + LIT[face] + (-1 if x in (0, w - 1) else 0) + (1 if y == 0 else 0))
        g[h - 1] = [EMBER[2] if x % 2 else EMBER[3] for x in range(w)]
        if face == "front": over(g, grid(COG, pcog), (w - 5) // 2, 0)
        return g
    return paint

def cloth_painter(spec, side):
    if spec == "mantlet": return mantlet(TEAM_IC[side][0])
    return crew_cloth(spec, side)

IC_BALLISTA = Troop("ic_ballista", SKIN, CLOTH, bones, cloth_painter,
                    {"weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "tall 4", "faces": {"count": 4, "card": 0}},
                    tiers=(1,))
def build(): return IC_BALLISTA.build()
def outputs(): return IC_BALLISTA.outputs()

if __name__ == "__main__":
    IC_BALLISTA.main(sys.argv)
