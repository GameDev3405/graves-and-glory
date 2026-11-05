#!/usr/bin/env python3
"""The Risen's notched sword (Graveborn), hand-authored at one texel per model pixel.

A grave-iron blade eaten by rust: four pixels wide, with real notches bitten out of both edges (the edges are separate
cubes with gaps between), the point snapped off on a slant, a violet cord tied round the ricasso, a bent and broken
cross-guard (one quillon drooping, the other snapped short), a violet-bound grip and a pitted iron knob. Held like the
sword grip.

  python build_gb_notched_sword.py preview <png> | stub | install
  (stub writes art/weapons/notched_sword.* only; install also runs tools/build_skins.py)
"""
import json, os, random, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "common"))
import build_swords as W
from build_swords import Cube, by_face, flat, wrap, REPO
from graveborn import GIRON as I, RUST as R, VIOLET as V, BONE as B, FROST as F, GILT as G
W.KEY.setdefault("bone", (0xEE, 0xEE, 0xDD))   # the scythe's bone key

def pits(base, seed, n, rustp=0.5):
    """A painter: a flat `base` field with `n` pits per face, dark or rust, so the iron looks eaten."""
    def paint(face, w, h):
        rnd = random.Random("%s-%s-%d-%d" % (seed, face, w, h))
        g = [[base] * w for _ in range(h)]
        for _ in range(n):
            x, y = rnd.randrange(w), rnd.randrange(h)
            g[y][x] = R[2 + rnd.randrange(3)] if rnd.random() < rustp else I[1 + rnd.randrange(2)]
        return g
    return paint

def flat_blade(cols, seed, rustp=0.45):
    """Blade flat: columns left to right as the wearer sees them, with scattered pits and rust bloom toward the base."""
    def paint(face, w, h):
        rnd = random.Random("%s-%s-%d-%d" % (seed, face, w, h))
        g = []
        for y in range(h):
            row = list(cols if face == "south" else cols[::-1])
            row = (row + [row[-1]] * w)[:w]
            for x in range(w):
                if rnd.random() < (0.06 + 0.34 * (y / max(1, h)) ** 2):   # more rust toward the root (bottom of the flat)
                    row[x] = R[2 + rnd.randrange(3)] if rnd.random() < rustp else I[3]
            g.append(row)
        return g
    return paint

def edge(light, dark, seed):
    return by_face(up=flat(light), down=flat(dark), rest=pits(light, seed, 3, 0.6))

def tier1(tier=1):
    """Tier II (the Barrow Revenant): a bone knob, the guard scoured to grave-iron with a bone spur grafted on the snapped
    end, rime-pale edges. Tier III (the Oathless Revenant): the guard chased in funeral gilt, a gilt cap on the bone knob,
    the blade's rust bloom cleaned away. The blade and its notches never change: trim, metal and bone escalate, not size."""
    rust = 0.6 if tier < 3 else 0.15
    blade_flat = flat_blade([I[6], I[5]], "nb", rust)
    spine = by_face(north=blade_flat, south=blade_flat, up=flat(I[6]), down=flat(I[1]), rest=flat(I[5]))
    cord = lambda face, w, h: [[V[3] if (x + y) % 2 == 0 else V[2] for x in range(w)] for y in range(h)]
    cubes = [
        Cube("knob", [-1, -4, -1], [1, -2, 1], "iron", by_face(up=flat(I[5]), down=flat(I[1]), rest=pits(I[3], "kn", 3))),
        Cube("grip", [-0.5, -2, -0.5], [0.5, 2, 0.5], "leather", wrap(V[3], V[1]), skip=("up", "down")),
        # the guard: a rust-pitted bar, its left end bent down, its right end snapped short
        Cube("guard", [-3, 2, -1], [2, 3, 1], "iron", by_face(up=flat(R[4]), down=flat(R[1]), rest=pits(I[3], "gd", 5, 0.8))),
        Cube("quillon_bent", [-3, 0.0, -0.5], [-2, 2, 0.5], "iron", by_face(up=flat(R[3]), down=flat(R[0]), rest=pits(I[3], "qb", 2, 0.8))),
        # the blade: a 2 px core and 1 px edge strips with notches bitten out
        Cube("core", [-1, 3, -0.5], [1, 16, 0.5], "iron", spine, skip=("down",)),
        Cube("cord", [-1.5, 3, -1], [1.5, 5, 1], "leather", by_face(up=flat(V[2]), down=flat(V[1]), rest=cord), skip=()),
    ]
    # left edge strips (wearer's left is +x here: blade seen from the south face reads left to right along +x)
    for n, (y0, y1) in enumerate(((5, 7), (8, 11), (12, 14), (14.0, 15))):
        cubes.append(Cube("edge_r%d" % n, [1, y0, -0.5], [2, y1, 0.5], "iron", edge(F[3 if tier < 2 else 4], I[3], "er%d" % n), skip=("down",) if n == 3 else ()))
    for n, (y0, y1) in enumerate(((5, 8), (9, 10), (11, 15))):
        cubes.append(Cube("edge_l%d" % n, [-2, y0, -0.5], [-1, y1, 0.5], "iron", edge(F[3 if tier < 2 else 4], I[3], "el%d" % n), skip=("down",) if n == 2 else ()))
    # the snapped point: a slanted stub, off the centre line
    cubes.append(Cube("tip", [-1, 16, -0.5], [0, 18, 0.5], "iron", by_face(up=flat(F[4]), rest=flat_blade([I[6]], "tp", 0.5)), skip=("down",)))
    if tier >= 2:
        for c in cubes:
            if c.name == "knob": c.mat, c.paint = "bone", by_face(up=flat(B[5]), down=flat(B[1]), rest=pits(B[3], "kn", 2, 0))
            if c.name == "guard": c.paint = by_face(up=flat(I[5]), down=flat(I[1]), rest=pits(I[4], "gd", 4, 0.2))
            if c.name == "quillon_bent": c.paint = by_face(up=flat(I[5]), down=flat(I[0]), rest=pits(I[3], "qb", 2, 0.2))
        cubes.append(Cube("bone_spur", [2, 2, -0.5], [4, 3, 0.5], "bone", by_face(up=flat(B[5]), down=flat(B[2]), rest=flat(B[4]))))
    if tier >= 3:
        for c in cubes:
            if c.name == "guard": c.mat, c.paint = "gilt", by_face(up=flat(G[4]), down=flat(G[1]), rest=pits(G[3], "gd", 3, 0))
            if c.name == "quillon_bent": c.mat, c.paint = "gilt", by_face(up=flat(G[4]), down=flat(G[0]), rest=flat(G[2]))
        cubes.append(Cube("knob_cap", [-1.5, -5, -1.5], [1.5, -4, 1.5], "gilt", by_face(up=flat(G[3]), down=flat(G[1]), rest=flat(G[2]))))
    return cubes

TIERS = {"notched_sword": (tier1, "The Risen's notched sword (Graveborn), hand-authored at 1 texel a px: a pitted grave-iron blade 4 px wide "
                           "with notches bitten from both edges and a snapped slanted point, a violet cord on the ricasso, a bent and "
                           "broken cross-guard, a violet-bound grip and a pitted iron knob; held like the sword grip."),
         "notched_sword_t2": (lambda: tier1(2), "The Risen's notched sword, tier II (Graveborn): tier I with a bone knob, the guard scoured to "
                              "grave-iron with a bone spur on the snapped end and rime-pale edges; 1 texel a px."),
         "notched_sword_t3": (lambda: tier1(3), "The Risen's notched sword, tier III (Graveborn): tier II with the guard chased in funeral "
                              "gilt, a gilt cap under the bone knob and the rust bloom cleaned from the blade; 1 texel a px.")}

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
