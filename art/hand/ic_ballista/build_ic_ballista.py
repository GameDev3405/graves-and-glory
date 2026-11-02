#!/usr/bin/env python3
"""The Ironclad Ballista Crew (troop id ic_ballista), hand-authored at one texel per model pixel.

A crewman in an iron cap with an ember brow band, calm mail and plate under a team tabard, standing behind a compact
engine held at his hip: an oak rail carrying a brass-headed bolt, a slim rope skein either side with bright iron caps,
oak arms swept back to a pale cord, two small iron-tyred wheels with brass hubs on a short axle, a prop foot at the
muzzle, and a windlass drum flush against his belt with a crank grip at each hand. Tier I only: the other armour tiers
fall back to it.

  python build_ic_ballista.py preview <dir> [side] | contact <png> [side] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from siege_kit import *   # noqa: F401,F403

SKIN = (128, 128)
CLOTH = (128, 64)

def wheel_face(face, w, h):
    """One 5 x 5 wheel face: a bright iron tyre, a dark well and a brass hub; no spokes."""
    g = blank(w, h)
    if face not in ("left", "right"):
        return iron_plain(5, "tyre", 1)(face, w, h)
    for y in range(h):
        for x in range(w):
            if x in (0, w - 1) or y in (0, h - 1):
                g[y][x] = IRON[6 if (x == 0 or y == 0) else 4]
            else:
                g[y][x] = IRON[1]
    g[h // 2][w // 2] = BRASS[5]
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)): g[h // 2 + dy][w // 2 + dx] = BRASS[3]
    return g

def wheel_rim(face, w, h):
    return iron_plain(5, "rim", 0)(face, w, h)

def skein_paint(face, w, h):
    """A rope skein: even strands with a slow diagonal lay, one dark seam each side, nothing busier."""
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom"):
                g[y][x] = clamp(IRON, 2); continue
            v = 3 + LIT[face] + (1 if ((x + y // 2) // 2) % 2 == 0 else 0)
            if x in (0, w - 1): v -= 1
            g[y][x] = clamp(ROPE, v)
    return g

def cap_plate(base):
    def paint(face, w, h):
        g = plate(IRON, base, spec=False)(face, w, h)
        if face in ("front", "back") and h == 1:
            for x in range(w): g[0][x] = IRON[min(6, base + 1) if x % 3 else 6]   # rivet heads along the edge
        return g
    return paint

def cord_paint(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w): g[y][x] = clamp(ROPE, 5 + LIT[face] // 2 - (1 if (x + y) % 4 == 0 else 0))
    return g

def bolt_shaft():
    return grain(OAK, "z", 5, "bolt", knots=0)

def bolt_head(face, w, h):
    g = iron_plain(6, "bhead", 0)(face, w, h)
    if face not in ("top", "bottom"):
        for x in range(w): g[h - 1][x] = BRASS[3]
    return g

def fletch(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w): g[y][x] = clamp(ROPE, 4 + LIT[face])
    return g

def grip_paint(face, w, h):
    return brass(4, seed="grip")(face, w, h)

def drum_paint(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom"): g[y][x] = clamp(ROPE, 3 + LIT[face]); continue
            g[y][x] = clamp(ROPE, 3 + LIT[face] + (1 if (x // 2 + y) % 2 == 0 else 0))
    return g

def bones():
    B = crew_bones(plume=True)
    BY = 12.5                                   # the bolt line, a hand below the belt
    CZ = -11                                    # the wheels' axle line
    # carriage: the rail the bolt rides in, the axle block, the axle and the front prop
    B.append(Bone("carriage", "root", tag="siege", boxes=[
        Box((-1.5, 13.5, -20.5), (3, 3, 14), hooped(grain(OAK, "z", 3, "rail", knots=0), [(0, 2), (12, 2)], along="z", R=IRON)),
        Box((-1.5, 16.5, CZ - 1.5), (3, 3, 3), grain(OAK, "x", 3, "block", knots=0)),
        Box((-8, 19.4, CZ - 1), (16, 2, 2), iron_plain(3, "axle", 1)),
        Box((-1, 16.5, -20.5), (2, 5.5, 2), grain(OAK, "y", 3, "prop", knots=0)),
        Box((-1.5, 22, -21), (3, 2, 3), iron_plain(4, "shoe", 1))]))
    # two small wheels: an axis-aligned square and a 45-degree square make the octagon; a brass cap on each hub
    for side, c in (("r", -1), ("l", 1)):
        cx = 8 * c
        B.append(Bone(f"wheel_{side}", "root", (cx, 20.4, CZ), tag="siege", boxes=[Box((-1, -2.5, -2.5), (2, 5, 5), wheel_face, dil=0.04)]))
        B.append(Bone(f"wheel_{side}_rim", "root", (cx, 20.4, CZ), rot=(45, 0, 0), tag="siege", boxes=[Box((-1, -2.5, -2.5), (2, 5, 5), wheel_rim)]))
        B.append(Bone(f"hub_{side}", "root", tag="siege", boxes=[Box((9 * c - (0 if c > 0 else 1), 19.4, CZ - 1), (1, 2, 2), brass(5, seed="hub"))]))
    # the windlass: a rope drum behind the bolt's nock between brass flanges, a crank bar out each side ending in a grip at the hands
    B.append(Bone("windlass", "root", tag="siege", boxes=[
        Box((-3.5, 11, -6.5), (7, 5, 4), drum_paint),
        Box((-4.5, 10.5, -7.5), (1, 6, 5), brass(4, seed="fl")), Box((3.5, 10.5, -7.5), (1, 6, 5), brass(4, seed="fr")),
        Box((-7.5, 11.5, -5.5), (3, 2, 2), iron_plain(3, "crank", 1)), Box((4.5, 11.5, -5.5), (3, 2, 2), iron_plain(3, "crank", 1)),
        Box((-7.5, 10.5, -6), (2, 4, 4), grip_paint), Box((5.5, 10.5, -6), (2, 4, 4), grip_paint)]))
    # torsion: a slim rope skein either side of the rail with bright iron caps, an oak arm swept out and back, a pale cord to the nock
    for side, c in (("r", -1), ("l", 1)):
        x0 = 1.5 if c > 0 else -4.5
        xc = x0 if c > 0 else x0 - 1
        B.append(Bone(f"skein_{side}", "root", tag="siege", boxes=[
            Box((x0, 10, -17.5), (3, 5, 4), skein_paint),
            Box((xc, 9, -18.5), (4, 1, 6), cap_plate(4)),
            Box((xc, 15, -18.5), (4, 1, 6), cap_plate(3))]))
        B.append(Bone(f"arm_{side}", "root", (3 * c, BY, -15), rot=(0, 45 * c, 0), tag="siege", boxes=[
            Box((-1, -1, -0.5), (2, 2, 8), hooped(grain(OAK, "z", 4, "arm", knots=0), [(6, 2)], along="z", R=IRON, trim=BRASS))]))
        tip = (3 * c + 7.5 * 0.7071 * c, -15 + 7.5 * 0.7071)
        B.append(Bone(f"cord_{side}", "root", (tip[0], BY, tip[1]), rot=(0, -68.9 * c, 0), tag="siege", boxes=[
            Box((-0.5, -0.5, -0.4), (1, 1, 9), cord_paint)]))
    B.append(Bone("bolt", "root", tag="siege", boxes=[
        Box((-0.5, 11.5, -20.5), (1, 2, 14), bolt_shaft()),
        Box((-1.5, 11, -23.5), (3, 3, 3), bolt_head),
        Box((-0.5, 12, -25.5), (1, 1, 2), brass(6, seed="tip")),
        Box((-2, 12, -9.5), (4, 1, 3), fletch), Box((-0.5, 10.5, -9.5), (1, 4, 3), fletch)]))
    B.append(Bone("quiver", "body", boxes=[
        Box((-3, -10, 2.2), (6, 9, 3), hooped(grain(OAK, "y", 4, "qv"), [(0, 1), (4, 1), (8, 1)], along="y", R=IRON)),
        Box((-2.5, -11.5, 2.7), (5, 2, 1.5), brass(5, seed="qa"))]))
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
