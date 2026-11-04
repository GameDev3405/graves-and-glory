#!/usr/bin/env python3
"""The Hollow King's grave sword (Graveborn), hand-authored at one texel per model pixel.

A Souls-like greatsword of black grave-iron, 36 px from pommel to point: a broad blade 5 px wide (a 3 px core with a dark
fuller, a honed pale edge in separate segments with chips bitten out of both sides, the point worn to a slant), a few
broken gilt flecks along the fuller, a violet cord tied round the ricasso, a heavy cross-guard on a block (the left
quillon drooping, the right one snapped short), a long grip wound in violet cord with three gilt-wire collars and a
chipped iron pommel. No glow: the Hollow King's 16 lit texels belong to his ribcage. Held like the sword grip.

  python build_gb_grave_sword.py preview <png> | stub | install
  (stub writes art/weapons/grave_sword.* only; install also runs tools/build_skins.py)
"""
import json, os, random, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "common"))
import build_swords as W
from build_swords import Cube, by_face, flat, REPO
from graveborn import BONE as B, GIRON as I, VIOLET as V, GILT as G, FROST as F, RUST as R

def pits(base, seed, n, gilt=0.0):
    """A flat `base` field with `n` dark pits (or flecks of worn gilt) per face, so the iron looks old and struck."""
    def paint(face, w, h):
        rnd = random.Random("%s-%s-%d-%d" % (seed, face, w, h))
        g = [[base] * w for _ in range(h)]
        for _ in range(n):
            x, y = rnd.randrange(w), rnd.randrange(h)
            g[y][x] = G[1 + rnd.randrange(2)] if rnd.random() < gilt else I[1 + rnd.randrange(2)]
        return g
    return paint

def blade_flat(seed, gild=4):
    """The core's flat, 3 columns as the wearer sees them: a lit bevel, the dark fuller, a shaded bevel; pits grow toward
    the root, and broken flecks of gilt inlay run down the fuller."""
    def paint(face, w, h):
        rnd = random.Random("%s-%s-%d-%d" % (seed, face, w, h))
        cols = [I[6], I[3], I[5]]
        if face == "north": cols = cols[::-1]
        if w < 3: cols = [cols[0], cols[2]] if w == 2 else [cols[1]]
        g = []
        for y in range(h):
            row = list(cols)
            for x in range(w):
                if rnd.random() < 0.03 + 0.14 * (1 - y / max(1, h)) ** 2:     # y = 0 is the tip end of the flat
                    row[x] = I[1 + rnd.randrange(2)] if rnd.random() < 0.7 else R[1 + rnd.randrange(2)]
            if y % gild == 1 and rnd.random() < 0.8 and w >= 3: row[1] = G[1 + (y // gild) % 2 + (gild < 4)]
            g.append(row)
        return g
    return paint

def edge(light, dark, seed):
    return by_face(up=flat(light), down=flat(dark), rest=pits(light, seed, 3))

def cord(a, b):
    """A cord wound on the bias: two violet tones in a diagonal hatch."""
    return lambda face, w, h: [[a if (x + y) % 2 == 0 else b for x in range(w)] for y in range(h)]

W.KEY.setdefault("bone", (0xEE, 0xEE, 0xDD))   # the scythe's bone key

def swap(cubes, name, mat, paint):
    for c in cubes:
        if c.name == name: c.mat, c.paint = mat, paint

def tier1(tier=1):
    """Tier II (the Barrow King): a bone grip and pommel skull-knob, the snapped quillon replaced by a grafted bone spur.
    Tier III (the Hollow Crown): the guard block chased in funeral gilt, a gilt-filled fuller, a gilt-capped pommel and
    rime-pale honed edges. Same cubes at every tier (trim, metal and bone escalate, not size)."""
    gild = 4 if tier < 3 else 2
    spine = by_face(north=blade_flat("gs", gild), south=blade_flat("gs", gild), up=flat(I[6]), down=flat(I[1]), rest=flat(I[4]))
    hone = F[4] if tier < 3 else F[5]
    cubes = [
        Cube("pommel", [-1.5, -6, -1], [1.5, -3, 1], "iron", by_face(up=flat(I[5]), down=flat(I[1]), rest=pits(I[3], "pm", 4, 0.3))),
        Cube("pomrim", [-1, -3, -0.5], [1, -2, 0.5], "gilt", by_face(up=flat(G[2]), down=flat(G[0]), rest=flat(G[3]))),
        Cube("grip", [-0.5, -2, -0.5], [0.5, 5, 0.5], "leather", cord(V[3], V[2]), skip=("up", "down")),
        Cube("wire0", [-1, -1, -1], [1, 0, 1], "gilt", by_face(up=flat(G[4]), down=flat(G[1]), rest=flat(G[3]))),
        Cube("wire1", [-1, 1, -1], [1, 2, 1], "gilt", by_face(up=flat(G[4]), down=flat(G[1]), rest=flat(G[3]))),
        Cube("wire2", [-1, 3, -1], [1, 4, 1], "gilt", by_face(up=flat(G[4]), down=flat(G[1]), rest=flat(G[2]))),
        # the guard: a block on the grip, a crossbar through it, the left quillon bent down, the right snapped short
        Cube("block", [-2, 5, -1.5], [2, 9, 1.5], "iron", by_face(up=flat(I[6]), down=flat(I[1]), rest=pits(I[4], "bk", 6, 0.35))),
        Cube("bar_l", [-6, 6, -1], [-2, 8, 1], "iron", by_face(up=flat(I[6]), down=flat(I[1]), rest=pits(I[4], "bl", 3, 0.2))),
        Cube("bar_r", [2, 6, -1], [5, 8, 1], "iron", by_face(up=flat(I[6]), down=flat(I[1]), rest=pits(I[4], "br", 4, 0.2))),
        Cube("quill_l", [-7, 4, -1], [-6, 8, 1], "iron", by_face(up=flat(I[6]), down=flat(I[2]), rest=pits(I[4], "ql", 2, 0.2))),
        Cube("quill_r", [5, 6, -0.5], [6, 8, 0.5], "iron", by_face(up=flat(I[5]), down=flat(I[1]), rest=flat(I[3]))),
        # the ricasso and the violet cord tied round it
        Cube("ricasso", [-1.5, 9, -0.5], [1.5, 12, 0.5], "iron", by_face(north=blade_flat("rc"), south=blade_flat("rc"), up=flat(I[5]), down=flat(I[1]), rest=flat(I[4]))),
        Cube("tie", [-2, 9.5, -1], [2, 11.5, 1], "leather", by_face(up=flat(V[3]), down=flat(V[1]), rest=cord(V[3], V[2]))),
        # the blade: a 3 px core with a fuller, a 1 px honed edge on each side in segments with chips between
        Cube("core", [-1.5, 12, -0.5], [1.5, 32, 0.5], "iron", spine, skip=("down",)),
    ]
    for n, (y0, y1) in enumerate(((12, 17), (18, 23), (24, 31))):
        cubes.append(Cube("edge_r%d" % n, [1.5, y0, -0.5], [2.5, y1, 0.5], "iron", edge(hone, I[3], "er%d" % n)))
    for n, (y0, y1) in enumerate(((12, 15), (16, 22), (24, 31))):
        cubes.append(Cube("edge_l%d" % n, [-2.5, y0, -0.5], [-1.5, y1, 0.5], "iron", edge(hone, I[3], "el%d" % n)))
    if tier >= 2:   # bone: a skull-knob pommel, the grafted spur where the right quillon snapped
        bone = by_face(up=flat(B[5]), down=flat(B[1]), rest=pits(B[3], "bn", 3))
        swap(cubes, "pommel", "bone", bone if tier == 2 else by_face(up=flat(G[4]), down=flat(B[1]), rest=pits(B[3], "bn", 3, 0.5)))
        swap(cubes, "quill_r", "bone", by_face(up=flat(B[5]), down=flat(B[2]), rest=flat(B[4])))
        swap(cubes, "pomrim", "leather", by_face(up=flat(V[3]), down=flat(V[0]), rest=flat(V[2])))
    if tier >= 3:   # funeral gilt on the guard
        swap(cubes, "block", "gilt", by_face(up=flat(G[4]), down=flat(I[1]), rest=pits(G[3], "bk", 6)))
        swap(cubes, "pomrim", "gilt", by_face(up=flat(G[4]), down=flat(G[1]), rest=flat(G[3])))
        for n in ("bar_l", "bar_r", "quill_l"):
            swap(cubes, n, "iron", by_face(up=flat(G[3]), down=flat(I[1]), rest=pits(I[4], n, 4, 0.8)))
    # the point, worn to a slant
    cubes.append(Cube("tip0", [-1.0, 32, -0.5], [1.0, 34, 0.5], "iron", by_face(up=flat(F[4]), rest=blade_flat("t0")), skip=("down",)))
    cubes.append(Cube("tip1", [-0.5, 34, -0.5], [0.5, 35, 0.5], "iron", by_face(up=flat(F[5]), rest=flat(I[6])), skip=("down",)))
    return cubes

TIERS = {"grave_sword": (tier1, "The Hollow King's grave sword (Graveborn), hand-authored at 1 texel a px: a black grave-iron greatsword "
                         "36 px long, a 5 px blade with a dark fuller, broken gilt flecks and chipped honed edges, the point worn to "
                         "a slant, a violet cord on the ricasso, a heavy cross-guard (one quillon bent down, one snapped), a grip "
                         "wound in violet cord with gilt-wire collars and a chipped iron pommel; held like the sword grip."),
         "grave_sword_t2": (lambda: tier1(2), "The Hollow King's grave sword, tier II (Graveborn): tier I with a bone skull-knob pommel on a "
                            "violet rim and a grafted bone spur where the right quillon snapped; same cubes, hand-authored at 1 texel a px."),
         "grave_sword_t3": (lambda: tier1(3), "The Hollow King's grave sword, tier III (Graveborn): tier II with the guard chased in funeral "
                            "gilt, a gilt-filled fuller, a gilt-capped bone pommel and rime-pale honed edges; same cubes, 1 texel a px.")}

def write():
    for name, thing in W.outputs(TIERS).items():
        path = os.path.join(REPO, "art", "weapons", name)
        if name.endswith(".bbmodel"):
            sys.path.insert(0, os.path.join(REPO, "tools")); import bbmodel; bbmodel.write(thing, path)
        elif isinstance(thing, dict):
            with open(path, "w", encoding="utf-8", newline="\n") as f: f.write(json.dumps(thing, indent=1) + "\n")
        else: thing.save(path)
        print("wrote", path)

def outputs(): return W.outputs(TIERS)

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "stub": write()
    else: W.main(TIERS, __doc__)
