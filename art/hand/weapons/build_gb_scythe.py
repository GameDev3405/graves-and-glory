#!/usr/bin/env python3
"""The Grave Reaper's scythe (Graveborn), hand-authored at one texel per model pixel.

A long dark grave-wood haft, a spike on the butt, a grip wound in black leather and violet cord, two more violet
bindings, and at the top a grave-iron collar holding the mark: a 10 px curved blade that rises over the wearer's hood and
hooks down at the point. The blade is a 2 px deep spine over a 1 px body with a honed pale edge on the inner curve. Held
upright like the sun staff, the blade arcing over toward the wearer's hood (weapon +x).

  python build_gb_scythe.py preview <png> | stub | install
  (stub writes art/weapons/scythe.* only; install also runs tools/build_skins.py)
"""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "common"))
import build_swords as W
from build_swords import Cube, by_face, flat, wrap, REPO
from build_sun_staff import post, grain, metal, plate
from handlib import ramp
from graveborn import GIRON as I, VIOLET as V, BONE as B, GILT as G

W.KEY.update({"wood": (0x8B, 0x45, 0x13), "bone": (0xEE, 0xEE, 0xDD)})
def binder(R):
    """A violet cord wound round the haft: a diagonal hatch of two tones, a lit top and a shaded underside."""
    def fn(face, u, y, w):
        if face == "up": return R[3]
        if face == "down": return R[0]
        if face in ("east", "west"): return R[2] if (y % 2) else R[1]
        return R[3] if (u + y) % 2 == 0 else R[2]
    return fn

HAFT = ramp("0e0b0a", "1b1512", "2a211b", "3a2f26", "4c3e32", "5e4e40")   # black grave-wood

# The blade seen from the south, row 0 on top, column 0 at the collar (x -1): s spine (2 px deep), m body, e honed edge,
# E the point. The root runs down into the collar, the blade rises and arcs 10 px out over the hood, the point hooks down.
BLADE = [
    "...ssssss...",
    ".sssmmmmmss.",
    "ssmmmmmmmmms",
    "smmeeeeeemes",
    "mmme......ee",
    "..e.......eE",
    "..e.........",
    "............",
]

def legend(tier=1):
    return {
        "s": (G, 2, "gilt", "disc") if tier == 3 else (I, 2, "iron", "disc"),
        "m": (I, 3, "iron", "ray"),
        "e": (I, 6, "iron", "ray"),
        "E": (B, 5 if tier >= 2 else 4, "bone" if tier >= 2 else "iron", "ray"),
    }

def tier1(tier=1):
    """Spike, black haft, violet-corded grip, bindings, iron collar, curved blade. Tier II (the Barrow Reaper): a bone butt
    spike and shoe, a bone mid ring and a bone-tipped point. Tier III (the Harvest of Kings): the collar and blade spine in
    funeral gilt over tier II's bone. Same cubes at every tier: trim, metal and bone escalate, not size."""
    bone = metal(B, 3) if tier >= 2 else None
    cubes = [
        post("spike", -16, -14, 0.5, 0.5, *(("bone", bone) if bone else ("iron", metal(I, 4))), skip=("up",)),
        post("shoe", -14, -11, 1, 1, *(("bone", metal(B, 2)) if bone else ("iron", metal(I, 3)))),
        post("shaft_lo", -11, -4, 0.5, 0.5, "wood", grain(HAFT, 3, 1), skip=("up", "down")),
        post("bind_lo", -4, -2, 1, 1, "leather", binder(V), skip=("up",)),
        Cube("grip", [-1, -2, -1], [1, 2, 1], "leather", wrap(I[3], I[2], V[2]), skip=("up", "down")),
        post("bind_hi", 2, 4, 1, 1, "leather", binder(V), skip=("down",)),
        post("shaft_a", 4, 12, 0.5, 0.5, "wood", grain(HAFT, 3, 4), skip=("up", "down")),
        post("bind_mid", 12, 14, 1, 1, *(("bone", binder(B)) if bone else ("leather", binder(V))), skip=("up", "down")),
        post("shaft_b", 14, 22, 0.5, 0.5, "wood", grain(HAFT, 3, 7), skip=("up", "down")),
        post("collar", 22, 26, 1, 1, *(("gilt", metal(G, 2)) if tier == 3 else ("iron", metal(I, 3))), skip=("down",)),
        post("collar_bind", 22, 23, 1.5, 1.5, "leather", binder(V)),
    ]
    blade = plate("blade", BLADE, 31, legend(tier))
    dx = -1 + len(BLADE[0]) / 2          # plate centres the grid on x = 0; column 0 belongs at x = -1
    for c in blade:
        c.lo[0] += dx; c.hi[0] += dx; c.pivot[0] += dx
    return cubes + blade

TIERS = {"scythe": (tier1, "The Grave Reaper's scythe (Graveborn), hand-authored at 1 texel a px: a black grave-wood haft with a "
                    "butt spike, a leather and violet-cord grip and violet bindings, an iron collar and a grave-iron blade 10 px "
                    "long that rises over the wearer's hood and hooks down at the point; held upright."),
         "scythe_t2": (lambda: tier1(2), "The Grave Reaper's scythe, tier II (Graveborn): tier I with a bone butt spike and shoe, a bone "
                       "mid ring and a bone-tipped point; same cubes, hand-authored at 1 texel a px, held upright."),
         "scythe_t3": (lambda: tier1(3), "The Grave Reaper's scythe, tier III (Graveborn): tier II with the collar and the blade spine "
                       "in funeral gilt; same cubes, hand-authored at 1 texel a px, held upright.")}
_ORIG = W.outputs
UPRIGHT = {"offset": [0, 0, 0], "rotation": [0, 0, 0], "note": "held upright like the sun staff"}

def outputs():
    out = _ORIG(TIERS)
    for wid in TIERS: out[wid + ".json"]["grip"] = UPRIGHT
    return out

def quads():
    return sum(1 for c in tier1() for f in W.FACES if f not in c.skip and all(c.size(f)))

def write():
    for name, thing in outputs().items():
        path = os.path.join(REPO, "art", "weapons", name)
        if name.endswith(".bbmodel"):
            sys.path.insert(0, os.path.join(REPO, "tools")); import bbmodel; bbmodel.write(thing, path)
        elif isinstance(thing, dict):
            with open(path, "w", encoding="utf-8", newline="\n") as f: f.write(json.dumps(thing, indent=1) + "\n")
        else: thing.save(path)
        print("wrote", path)
    print("quads", quads())

if __name__ == "__main__":
    W.outputs = lambda tiers=None: outputs()
    if len(sys.argv) > 1 and sys.argv[1] == "stub": write()
    elif len(sys.argv) > 1 and sys.argv[1] == "quads": print(quads())
    else: W.main(TIERS, __doc__)
