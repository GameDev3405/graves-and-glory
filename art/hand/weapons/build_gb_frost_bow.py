#!/usr/bin/env python3
"""The Barrow Stalker's frost bow (Graveborn, weapon id frost_bow), hand-authored at one texel per model pixel.

The order bow's geometry (build_bows.order_bow tier I: the same five staves a limb, the same grip, the same draw frames and
nocks), so it is held and drawn the same. Dressed for the barrow: the limbs are pale grave-wood on the belly with a
frost-grey back, rimed white along the top; the riser is a block of black grave-iron over a grip wound in grave-iron cord and
violet; two violet bindings sit at the knees; the nocks are grave-iron capped in rime; a long rime shard hangs from the
riser. Violet is the accent, never the field.

  python build_gb_frost_bow.py preview <png> | stub | install
  (stub writes art/weapons/frost_bow.* only; install also runs tools/build_skins.py)
"""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "common"))
import build_swords as W
import build_bows as BW
from build_swords import by_face, flat, wrap, bar, REPO
from handlib import ramp
from graveborn import GIRON as I, VIOLET as V, FROST as F, BONE as B, GILT as G

PALE = ramp("2a2825", "48443d", "6a6558", "8c8573", "ada592", "cbc4b0")      # weathered pale grave-wood, the belly

def cord(face, w, h):
    """A violet binding: light up, dark under, a diagonal hatch of two violet tones round the sides."""
    if face == "up": return [[V[3]] * w for _ in range(h)]
    if face == "down": return [[V[0]] * w for _ in range(h)]
    return [[V[3] if (x + y) % 2 == 0 else V[2] for x in range(w)] for y in range(h)]

def iron(top, mid, low):
    return by_face(up=flat(I[top + 1]), down=flat(I[0]), rest=bar(I[top], I[mid], I[low]))

def frost_bow(tier=1):
    """Tier II (the Barrow Hunter): bone-capped nocks, a bone lower riser, a bone shard for the rime one, brighter violet grip.
    Tier III (the Rime Warden): risers, nocks and grip thread in funeral gilt, gilt knee bands, the bone shard. The staves
    and draw never change: trim, metal and bone escalate, not size."""
    segs = [(2, 5, 1, 2, 0.35), (2, 15, 1, 2, 0.65), (2, 27, 1, 2, 1.0), (2, 40, 1, 1, 1.2), (2, 10, 1, 1, 1.0)]
    nock = {"n": 1, "d": 1, "rel": 0, "wt": 1.0, "mat": "iron",
            "mk": lambda up: by_face(up=flat(I[5]), down=flat(I[1]), rest=flat(I[3])), "at": 0.5,
            "cap": ("bone", by_face(up=flat(F[6]), down=flat(F[3]), rest=bar(F[6], F[5], F[4])))}
    grip = BW.grip_cubes(wrap(I[3], I[2], V[2]))
    plate = iron(5, 4, 2)
    risers = [(("riser_hi", [-1.5, 2, -1.5], [1.5, 4, 1.5], "iron", by_face(up=flat(F[5]), down=flat(I[0]), rest=bar(I[5], I[4], I[2], I[1]))), {}),
              (("riser_lo", [-1, -3, -1], [1, -2, 1], "iron", plate), {}),
              (("riser_bind", [-1.5, 4, -1.5], [1.5, 5, 1.5], "leather", cord), {"skip": ("north", "south")})]
    bands = [(1, "leather", cord), (3, "leather", cord)]
    shard = by_face(up=flat(F[6]), down=flat(F[3]), east=flat(F[4]), west=flat(F[3]), rest=bar(F[6], F[5], F[3]))
    extras = [(("rime_shard", [1, -6, -0.25], [2, -2, 0.75], "bone", shard), {"turn": 12, "pivot": [1.5, -2, 0]})]
    gilt = by_face(up=flat(G[4]), down=flat(G[1]), rest=bar(G[4], G[3], G[1], G[2]))
    bone = by_face(up=flat(B[5]), down=flat(B[2]), rest=bar(B[5], B[4], B[2]))
    if tier >= 2:   # the frame cap leaves no room to add cubes: tiers swap materials on the same cubes
        nock = dict(nock, cap=("bone", bone))
        grip = BW.grip_cubes(wrap(I[3], I[2], V[3]))
        extras = [(("bone_shard", [1, -6, -0.25], [2, -2, 0.75], "bone", by_face(up=flat(B[5]), down=flat(B[2]), east=flat(B[4]), west=flat(B[3]),
                   rest=bar(B[5], B[4], B[2]))), {"turn": 12, "pivot": [1.5, -2, 0]})]
        risers[1] = (("riser_lo", [-1, -3, -1], [1, -2, 1], "bone", bone), {})
    if tier == 3:
        nock = dict(nock, cap=("gilt", gilt))
        grip = BW.grip_cubes(wrap(I[2], I[1], G[3]))
        risers[0] = (("riser_hi", [-1.5, 2, -1.5], [1.5, 4, 1.5], "gilt", by_face(up=flat(G[4]), down=flat(I[0]), rest=bar(G[3], I[3], I[1], G[2]))), {})
        risers[1] = (("riser_lo", [-1, -3, -1], [1, -2, 1], "gilt", gilt), {})
        bands = [(1, "gilt", gilt), (3, "gilt", gilt)]
    wid = "frost_bow" + ("" if tier == 1 else "_t%d" % tier)
    return BW.make(wid, {1: NOTE, 2: NOTE2, 3: NOTE3}[tier], root=3, segs=segs, wood=(F, PALE), nock=nock, grip=grip, risers=risers,
                   extras=extras, bands=bands, draw=(3.2, 5.65))

NOTE = ("The Barrow Stalker's frost bow (Graveborn), hand-authored at 1 texel a px on the order bow's geometry: pale grave-wood "
        "limbs with a frost-grey rimed back, a black grave-iron riser and nocks capped in rime, a violet-wound grip and knee "
        "bindings, a rime shard hanging from the riser. Drawn through the same four frames as the order bow.")
NOTE2 = NOTE.replace("bow (Graveborn)", "bow, tier II (Graveborn)") + " Tier II: bone-capped nocks, a bone lower riser and a bone shard."
NOTE3 = NOTE.replace("bow (Graveborn)", "bow, tier III (Graveborn)") + (" Tier III: risers, nocks and grip thread chased in funeral gilt, gilt "
         "knee bands and the bone shard.")
TIERS = {"frost_bow": (frost_bow, NOTE), "frost_bow_t2": (lambda: frost_bow(2), NOTE2), "frost_bow_t3": (lambda: frost_bow(3), NOTE3)}

# the Stalker's mantle and front flaps hang 3.4 px out from the body and hid the bow at rest: hold it out in front of them
HELD = {"offset": [1, 0, -3], "rotation": [0, 0, 0], "note": "the grip's centre is the model origin; held 3 px forward of the hand, outside the mantle flaps"}

def outputs(tiers=None):
    out = BW.outputs(TIERS)
    for wid in TIERS: out[wid + ".json"]["grip"] = HELD
    return out

def quads():
    spec = frost_bow(); frames, _ = spec.frames()
    return sum(1 for c in frames["rest"] if not isinstance(c, BW.Thread) for f in W.FACES if f not in c.skip and all(c.size(f)))

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
    if len(sys.argv) > 2 and sys.argv[1] == "preview": BW.preview(TIERS, sys.argv[2])
    elif len(sys.argv) > 1 and sys.argv[1] == "stub": write()
    elif len(sys.argv) > 1 and sys.argv[1] == "quads": print(quads())
    else: W.main(TIERS, __doc__)
