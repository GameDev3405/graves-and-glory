#!/usr/bin/env python3
"""The Ironclad weapons at tier I (ic_axe for the Shieldbreaker, ic_crossbow for the Arbalist), hand-authored at one texel
per model pixel in the Ironclad's blackened iron with ember inlay. Same frame as the swords: Blockbench, y up, the grip's
centre at the origin (the fist covers y -2..+2), the head up +y, the broad faces across z.

  ic_axe       a bearded axe: oiled haft in two ember-wrapped rings, an iron socket, a broad blade whose lower edge
               hooks down into a beard to catch a shield rim, a back spike, an ember inlay along the blade's spine. 13 cubes.
  ic_crossbow  a heavy steel-prod crossbow: dark wood tiller and butt, leather-wound grip, an iron lock, a stirrup, a prod
               with ember-bound limb tips, a waxed string and a loaded bolt with a dark fletch. 14 cubes.

  python build_ic_weapons.py install   the sources into art/weapons/, then tools/build_skins.py
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "ic_breaker"))
import build_swords as W
from build_swords import Cube, by_face, flat, wrap, across, REPO, ramp
from ironclad import IRON as S, EMBER as E, OILED as O, FUR   # noqa: E402

B = ramp("2a1c0a", "3e2b12", "664a20", "85642a", "997530", "c8a870", "e0d0a8")   # Ironclad brass (ironclad_tiers, tint-rule safe)
W.KEY.update({"wood": (0x8B, 0x45, 0x13)})

def metal(R, base=3):
    """A turned or forged piece: top lit, bottom shaded, the lit side a step brighter."""
    def paint(face, w, h):
        i = {"up": base + 2, "down": base - 2}.get(face, base)
        return [[R[max(0, min(len(R) - 1, i + (1 if (face in ("south", "east") and x == 0 and w > 1) else 0)))] for x in range(w)] for y in range(h)]
    return paint

def wood(R, base=2, salt=0):
    def paint(face, w, h):
        g = []
        for y in range(h):
            row = []
            for x in range(w):
                i = base + (1 if face in ("up", "east") else -1 if face in ("down", "west") else 0)
                k = (y * 5 + x * 3 + salt) % 7
                if k == 0: i -= 1
                elif k == 3: i += 1
                row.append(R[max(0, min(len(R) - 1, i))])
            g.append(row)
        return g
    return paint

def band(ember=False):
    R = E if ember else S
    def paint(face, w, h):
        if face == "up": return [[R[3]] * w for _ in range(h)]
        if face == "down": return [[R[0]] * w for _ in range(h)]
        return [[R[3 if (x + y) % 2 == 0 else 2] for x in range(w)] for y in range(h)]
    return paint

# ---- the axe ----
def axe(tier=1):
    I = S
    # the blade's flat, columns left to right along +x from the socket to the edge; the spine inlay is ember
    def flat_blade(w, h, inlay_row=None, chips=()):
        cols = [I[3]] * (w - 2) + [I[4], I[5]]               # a bright hammered edge at the outer two columns
        def rows(face, y, hh):
            o = {}
            if inlay_row is not None and y == inlay_row:
                for x in range(1, w - 2): o[x] = E[2 if x % 2 else 3]
            if (y, face) in chips: o[w - 1] = I[2]
            return o
        return across(cols, rows)
    out = [
        Cube("butt", [-1, -5, -1], [1, -3, 1], "iron", metal(I, 3)),
        Cube("ring_lo", [-1, -3, -1], [1, -2, 1], "iron", band(True), skip=("up",)),
        Cube("grip", [-0.5, -2, -0.5], [0.5, 2, 0.5], "leather", wrap(O[3], O[1]), skip=("up", "down")),
        Cube("haft_a", [-0.5, 2, -0.5], [0.5, 9, 0.5], "wood", wood(O, 3, 1), skip=("down",)),
        Cube("ring_mid", [-1, 9, -1], [1, 10, 1], "iron", band(), skip=("down",)),
        Cube("haft_b", [-0.5, 10, -0.5], [0.5, 15, 0.5], "wood", wood(O, 3, 4), skip=("down",)),
        Cube("socket", [-1, 15, -1], [1, 19, 1], "iron", metal(I, 3), skip=("down",)),
        Cube("socket_ring", [-1.5, 17, -1.5], [1.5, 18, 1.5], "iron", band(True)),
        Cube("blade_up", [1, 16, -0.5], [8, 21, 0.5], "iron",
             by_face(north=flat_blade(7, 5, 2, {(0, "north")}), south=flat_blade(7, 5, 2, {(0, "south")}), up=flat(I[5]), rest=flat(I[4]))),
        Cube("beard", [1, 12, -0.5], [6, 16, 0.5], "iron",
             by_face(north=flat_blade(5, 4, 1), south=flat_blade(5, 4, 1), rest=flat(I[4]), down=flat(I[1])), skip=("up",)),
        Cube("hook", [5, 10, -0.5], [6, 12, 0.5], "iron", by_face(north=flat(I[5]), south=flat(I[5]), rest=flat(I[3])), skip=("up",)),
        Cube("spike", [-4, 16, -0.5], [-1, 19, 0.5], "iron", by_face(north=across([I[2], I[3], I[3]]), south=across([I[2], I[3], I[3]]), rest=flat(I[3]))),
        Cube("spike_tip", [-5 - (tier >= 2), 17, -0.5], [-4, 18, 0.5], "iron", flat(I[5])),
    ]
    if tier >= 2:   # brass rings on the haft, a longer spike
        out += [Cube("ring_b", [-1, 12, -1], [1, 13, 1], "gilt", metal(B, 3), skip=("down",)), Cube("ring_c", [-1, 4, -1], [1, 5, 1], "gilt", metal(B, 3))]
    if tier >= 3:   # a crown spike, a taller edge, a brass plate on the blade, a butt spike
        out += [Cube("crown", [-0.5, 19, -0.5], [0.5, 22, 0.5], "iron", metal(I, 5), skip=("down",)),
                Cube("edge_top", [6, 21, -0.5], [8, 22, 0.5], "iron", by_face(rest=flat(I[5]), up=flat(I[6]))),
                Cube("plate", [2, 17, -1], [4, 20, 1], "gilt", by_face(north=across([B[4], B[3]]), south=across([B[4], B[3]]), rest=flat(B[2]))),
                Cube("butt_spike", [-0.5, -7, -0.5], [0.5, -5, 0.5], "iron", metal(I, 4), skip=("up",))]
    return out

# ---- the crossbow ----
def crossbow(tier=1):
    """Tier II: brass bands on the tiller, a brass stirrup and lock plate, brass limb caps. Tier III: a doubled steel prod, a
    windlass crank across the butt, a brass crest plate on the tiller and a brass-headed bolt."""
    I = S
    out = [
        Cube("butt", [-1, -6, -1], [1, -3, 1], "wood", wood(O, 2, 2)),
        Cube("butt_cap", [-1, -7, -1], [1, -6, 1], "iron", metal(I, 3), skip=("up",)),
        Cube("grip", [-1, -3, -1], [1, 2, 1], "leather", wrap(O[3], O[1]), skip=("up", "down")),
        Cube("lock", [-1.5, 2, -1.5], [1.5, 6, 1.5], "iron", metal(I, 3)),
        Cube("tiller", [-1, 6, -1], [1, 13, 1], "wood", wood(O, 3, 5), skip=("down",)),
        Cube("rail", [-0.5, 6, -1.5], [0.5, 13, -1], "iron", metal(I, 4), skip=("down",)),
        Cube("stirrup", [-1.5, 15, -1], [1.5, 17, 1], "gilt" if tier >= 2 else "iron", metal(B if tier >= 2 else I, 3), skip=("down",)),
        Cube("prod_c", [-3, 13, -1], [3, 15, 1], "iron", metal(I, 3)),
        Cube("prod_l", [-7, 13, -0.5], [-3, 15, 0.5], "iron", by_face(north=across([I[2], I[3], I[4], I[4]]), south=across([I[2], I[3], I[4], I[4]]), up=flat(I[5]), rest=flat(I[3]))),
        Cube("prod_r", [3, 13, -0.5], [7, 15, 0.5], "iron", by_face(north=across([I[4], I[4], I[3], I[2]]), south=across([I[4], I[4], I[3], I[2]]), up=flat(I[5]), rest=flat(I[3]))),
        Cube("tip_l", [-8, 15, -0.5], [-7, 16, 0.5], "iron", flat(E[3])),
        Cube("tip_r", [7, 15, -0.5], [8, 16, 0.5], "iron", flat(E[3])),
        Cube("string_l", [-7, 12, 0.5], [0, 13, 1.5], "leather", flat(FUR[3]), skip=("up", "down"), turn=-20, pivot=[0, 12.5, 1]),
        Cube("string_r", [0, 12, 0.55], [7, 13, 1.55], "leather", flat(FUR[3]), skip=("up", "down"), turn=20, pivot=[0, 12.5, 1]),
        Cube("bolt", [-0.5, 7, -2.5], [0.5, 18, -1.5], "iron", by_face(north=across([I[4]], lambda f, y, h: {0: (B[5] if tier >= 3 else I[6])} if y == 0 else ({0: O[2]} if y > h - 4 else {})),
                                                                     south=across([I[4]], lambda f, y, h: {0: (B[5] if tier >= 3 else I[6])} if y == 0 else ({0: O[2]} if y > h - 4 else {})), rest=flat(I[4]))),
    ]
    if tier >= 2:
        out = [c for c in out if c.name not in ("tip_l", "tip_r")]
        out += [Cube("band_a", [-1.5, 8, -2], [1.5, 9, 1], "gilt", metal(B, 3), skip=("south",)), Cube("band_b", [-1.5, 11, -2], [1.5, 12, 1], "gilt", metal(B, 3), skip=("south",)),
                Cube("cap_l", [-9, 14, -1], [-7, 16, 1], "gilt", metal(B, 4)), Cube("cap_r", [7, 14, -1], [9, 16, 1], "gilt", metal(B, 4)),
                Cube("lock_plate", [1.5, 2, -1], [2, 6, 1], "gilt", metal(B, 3))]
    if tier >= 3:
        out += [Cube("prod2_l", [-6, 15, -0.5], [-3, 16, 0.5], "iron", metal(I, 4)), Cube("prod2_r", [3, 15, -0.5], [6, 16, 0.5], "iron", metal(I, 4)),
                Cube("crank", [-3, -5, -0.5], [3, -4, 0.5], "iron", metal(I, 4)),
                Cube("knob_l", [-4, -6, -1], [-3, -3, 1], "gilt", metal(B, 4)), Cube("knob_r", [3, -6, -1], [4, -3, 1], "gilt", metal(B, 4)),
                Cube("crest", [-1, 9, 1], [1, 11, 2], "gilt", by_face(rest=flat(B[3]), down=flat(B[1]), up=flat(B[5])))]
    return out

TIERS = {
    "ic_axe": (axe, "The Ironclad Shieldbreaker's bearded axe, tier I, hand-authored at 1 texel a px: an oiled haft in ember-wrapped rings, an iron socket, a broad blade with a hooked beard and an ember inlay along its spine, a back spike."),
    "ic_crossbow": (crossbow, "The Ironclad Arbalist's crossbow, tier I, hand-authored at 1 texel a px: a dark wood tiller and butt, a leather-wound grip, an iron lock and stirrup, a steel prod with ember limb tips, a waxed string and a loaded bolt."),
}

_T = {"ic_axe": ("brass rings on the haft and a longer back spike", "a crown spike, a taller edge, a brass plate on the blade and a butt spike"),
      "ic_crossbow": ("brass bands on the tiller, a brass stirrup, lock plate and limb caps",
                      "a doubled steel prod, a windlass crank with brass knobs, a brass crest plate and a brass-headed bolt")}
for _wid, (_t2, _t3) in _T.items():
    _fn = TIERS[_wid][0]; _d = TIERS[_wid][1].replace(", tier I,", ",")
    TIERS[_wid + "_t2"] = ((lambda f: lambda: f(2))(_fn), _d + " Tier II: " + _t2 + ".")
    TIERS[_wid + "_t3"] = ((lambda f: lambda: f(3))(_fn), _d + " Tier III: " + _t2 + "; " + _t3 + ".")

W_outputs = W.outputs

def outputs(tiers=None):
    out = W_outputs(tiers or TIERS)
    grip = dict(W.GRIP)
    for wid in (tiers or TIERS):
        out[wid + ".json"]["grip"] = grip
    return out

def main():
    W.outputs = outputs
    W.main(TIERS, __doc__)

if __name__ == "__main__":
    main()
