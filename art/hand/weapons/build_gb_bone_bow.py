#!/usr/bin/env python3
"""The Bone Archer's bow (Graveborn), hand-authored at one texel per model pixel.

The Order Archer's recurve geometry (build_bows.order_bow: five staves a limb, the same flex, the same draw frames, nocks
and grip) in other stuff: black grave-wood staves alternating with pale bone strips, each bone strip lashed at its foot with
violet sinew, the grip wound in black leather and violet cord between two knuckles of bone, bone nocks hooked back at the
tips and a small bone spur on the back of the riser. Rest frame plus the three draw frames, like every bow.

  python build_gb_bone_bow.py preview <png> | stub | install
  (stub writes art/weapons/bone_bow.* only; install also runs tools/build_skins.py)
"""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "common"))
import build_swords as W
import build_bows as BW
from build_swords import Cube, by_face, flat, wrap, bar, REPO
from handlib import ramp
from graveborn import VIOLET as V, BONE as B, GIRON as I, GILT as G

WOOD = ramp("0e0b0a", "1b1512", "2a211b", "3a2f26", "4c3e32", "5e4e40")      # black grave-wood
BONE_SEGS = (1, 3)                                                            # the staves that are bone

_stave = BW.stave
def stave(Bk, K, i, upper):
    """Bone strips (segments 1 and 3) between wood staves; the base painter keeps the grain line and the fleck."""
    return _stave(B, B, i, upper) if i in BONE_SEGS else _stave(WOOD, WOOD, i, upper)
BW.stave = stave   # Spec.limb looks the name up in build_bows; this process only

def sinew(face, w, h):
    """A violet sinew lashing: a diagonal hatch of two tones, lit on top, dark underneath."""
    g = []
    for y in range(h):
        row = []
        for x in range(w):
            if face == "up": c = V[3]
            elif face == "down": c = V[0]
            elif face in ("east", "west"): c = V[2] if y % 2 else V[1]
            else: c = V[3] if (x + y) % 2 == 0 else V[2]
            row.append(c)
        g.append(row)
    return g

def knuckle(face, w, h):
    """A bone joint on the riser: pale on top and back, shaded belly, a dark seam."""
    if face == "up": return [[B[4]] * w for _ in range(h)]
    if face == "down": return [[B[1]] * w for _ in range(h)]
    return [[B[3] if face == "east" else B[2]] * w for _ in range(h)]

def spur(face, w, h):
    g = [[B[4] if face in ("east", "up") else B[3]] * w for _ in range(h)]
    if h > 1: g[h - 1] = [B[2]] * w
    return g

def nock_paint(up):
    return BW.horn(B, up)

def bone_bow(tier=1):
    """Tier II (the Barrow Archer): grave-iron knuckles piped in violet, violet sinew on every bone strip, a second bone spur.
    Tier III (the Deadeye): black plate knuckles chased in funeral gilt, gilt bands over the bone, gilt-capped nocks and a
    gilt thread in the grip. Same staves and draw at every tier: the trim escalates, not the size."""
    segs = [(2, 5, 1, 2, 0.35), (2, 15, 1, 2, 0.65), (2, 27, 1, 2, 1.0), (2, 40, 1, 1, 1.2), (2, 10, 1, 1, 1.0)]
    nock = {"n": 2, "d": 1, "rel": -6, "wt": 1.0, "mat": "bone", "mk": nock_paint, "at": 0.8}
    iron = by_face(up=flat(I[5]), down=flat(I[1]), rest=bar(I[5], I[3], I[1], V[2]))
    gilt = by_face(up=flat(G[4]), down=flat(G[1]), rest=bar(G[4], G[3], G[1], G[2]))
    if tier == 1:
        grip = BW.grip_cubes(wrap(I[3], I[2], V[2]))
        kn, kmat = by_face(up=flat(B[4]), down=flat(B[1]), rest=knuckle), "bone"
    elif tier == 2:
        grip = BW.grip_cubes(wrap(I[3], I[2], V[3]))
        kn, kmat = iron, "iron"
    else:
        grip = BW.grip_cubes(wrap(I[2], I[1], G[3]))
        kn, kmat = by_face(up=flat(G[4]), down=flat(I[0]), rest=bar(G[3], I[2], I[1], G[2])), "gilt"
        nock = dict(nock, cap=("gilt", gilt))
    risers = [(("riser_hi", [-1, 2, -1], [1, 4, 1], kmat, kn), {}),
              (("riser_lo", [-1, -3, -1], [1, -2, 1], kmat, kn), {})]
    extras = [(("spur", [1, 3, -0.5], [3, 5, 0.5], "bone", spur), {"turn": -22, "pivot": [1, 3, 0]})]
    if tier >= 2:
        extras.append((("spur_lo", [1, -4, -0.5], [3, -2, 0.5], "bone", spur), {"turn": 22, "pivot": [1, -2, 0]}))
    if tier == 1:
        bands = [(1, "leather", sinew), (3, "leather", sinew)]
    elif tier == 2:
        bands = [(1, "leather", sinew), (2, "leather", sinew), (3, "leather", sinew)]
    else:
        bands = [(1, "gilt", gilt), (3, "gilt", gilt)]
    wid = "bone_bow" + ("" if tier == 1 else "_t%d" % tier)
    return BW.make(wid, wid, root=3, segs=segs, wood=(WOOD, WOOD), nock=nock, grip=grip, risers=risers,
                   extras=extras, bands=bands, draw=(3.2, 5.65))

NOTE = ("The Bone Archer's bow (Graveborn), hand-authored at 1 texel a px: the Order Archer's recurve geometry in black "
        "grave-wood staves alternating with bone strips lashed in violet sinew, a leather and violet-cord grip between two bone "
        "knuckles, hooked bone nocks and a bone spur on the riser's back. Draws like order_bow.")
NOTE2 = NOTE.replace("bow (Graveborn)", "bow, tier II (Graveborn)") + (" Tier II: grave-iron knuckles piped in violet, violet sinew on every "
         "bone strip and a second bone spur.")
NOTE3 = NOTE.replace("bow (Graveborn)", "bow, tier III (Graveborn)") + (" Tier III: black plate knuckles chased in funeral gilt, gilt bands "
         "over the bone strips, gilt-capped nocks, a gilt thread in the grip and both bone spurs.")
BW.NOTES.update({"bone_bow": NOTE, "bone_bow_t2": NOTE2, "bone_bow_t3": NOTE3})
TIERS = {"bone_bow": (bone_bow, NOTE), "bone_bow_t2": (lambda: bone_bow(2), NOTE2), "bone_bow_t3": (lambda: bone_bow(3), NOTE3)}

def outputs(tiers=None):
    return BW.outputs(TIERS)

def write():
    for name, thing in outputs().items():
        path = os.path.join(REPO, "art", "weapons", name)
        if name.endswith(".bbmodel"):
            sys.path.insert(0, os.path.join(REPO, "tools")); import bbmodel; bbmodel.write(thing, path)
        elif isinstance(thing, dict):
            with open(path, "w", encoding="utf-8", newline="\n") as f: f.write(json.dumps(thing, indent=1) + "\n")
        else: thing.save(path)
        print("wrote", path)

if __name__ == "__main__":
    W.outputs = lambda tiers=None: outputs()
    if len(sys.argv) > 1 and sys.argv[1] == "stub": write()
    elif len(sys.argv) > 2 and sys.argv[1] == "preview": BW.preview(TIERS, sys.argv[2])
    else: W.main(TIERS, __doc__)
