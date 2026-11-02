#!/usr/bin/env python3
"""Three House weapons at one texel per model pixel: the Hearthguard's hearth axe (Ironclad), the Tusk's boar spear
(Wildmoot) and the Tideguard's coral trident (Tidecallers). Each reuses the sun staff's post/grain/metal painters.

  hearth_axe     blackened-iron bearded axe on a dark ash haft with an ember band; held like a sword (the sword grip).
  boar_spear     pale ash shaft, a steel leaf head standing over a bone crossbar, a hide-wrapped grip; held upright.
  coral_trident  bleached coral shaft, a bronze (gilt) three-prong head on a crossbar; held upright.

  python build_house_weapons.py preview <png> | install
"""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import build_swords as W
from build_swords import Cube, by_face, flat, wrap, STEEL as S, GILT as G, LEATH as L, ramp, REPO
from build_sun_staff import post, px, grain, metal, binder, ASH, PALE

W.KEY.update({"wood": (0x8B, 0x45, 0x13), "bone": (0xEE, 0xEE, 0xDD)})
EMBER = ramp("1f0a06", "4a1608", "7a2a0c", "993f12", "c8966f", "ecc9a6")
CORAL = ramp("6f5d57", "8f7d72", "b0a094", "cdbcad", "e8dccd", "faf4e8")
DARK = ramp("120d0a", "2a1a10", "3c2a1c", "56402b", "6f5438", "8a6c4a")
IRONW = ramp("0d0e14", "1a1c25", "2c303b", "444a58", "62697a", "8a92a2", "bcc3cf")

def bit(name, x0, x1, y0, y1, mat, fn, skip=()):
    return Cube(name, [x0, y0, -0.5], [x1, y1, 0.5], mat, px(fn, y1), skip=skip)

def hearth_axe():
    """A bearded axe: dark haft, ember band, riveted iron head with a hammered edge, a spike behind."""
    return [
        post("butt", -6, -4, 1, 1, "iron", metal(IRONW, 3), skip=("up",)),
        post("haft_lo", -4, -2, 0.5, 0.5, "wood", grain(DARK, 3, 1), skip=("down", "up")),
        Cube("grip", [-1, -2, -1], [1, 2, 1], "leather", wrap(L[2], L[1]), skip=("up", "down")),
        post("haft", 2, 14, 0.5, 0.5, "wood", grain(DARK, 3, 4), skip=("down", "up")),
        post("band", 6, 7, 1, 1, "iron", metal(EMBER, 3)),
        post("socket", 12, 15, 1, 1, "iron", metal(IRONW, 3)),
        Cube("beard", [1, 7, -0.5], [4, 15, 0.5], "iron", by_face(rest=lambda f, w, h: [[IRONW[4 if y < h - 2 else 3 if x < w - 1 else 6] for x in range(w)] for y in range(h)])),
        Cube("blade", [4, 8, -0.5], [6, 14, 0.5], "iron", lambda f, w, h: [[IRONW[6 if x == w - 1 else 5 if (f in ("north", "south") and x == w - 2) else 4] for x in range(w)] for y in range(h)]),
        bit("spike", -3, -1, 11, 13, "iron", metal(IRONW, 3)),
    ]

def boar_spear():
    """An upright boar spear: ash shaft, hide grip, bone crossbar under a steel leaf head, a tuft of bristle."""
    return [
        post("foot", -14, -12, 0.5, 0.5, "steel", metal(S, 4), skip=("up",)),
        post("shaft_lo", -12, -3, 0.5, 0.5, "wood", grain(PALE, 3, 2), skip=("down", "up")),
        post("ring_lo", -3, -2, 1, 1, "bone", metal(CORAL, 3), skip=("up",)),
        Cube("grip", [-1, -2, -1], [1, 2, 1], "leather", wrap(L[2], L[1]), skip=("up", "down")),
        post("shaft", 2, 21, 0.5, 0.5, "wood", grain(PALE, 3, 6), skip=("down", "up")),
        post("binder", 14, 16, 1, 1, "leather", binder(DARK)),
        post("cross", 21, 22, 3, 0.5, "bone", metal(CORAL, 3)),
        post("collar", 22, 24, 1, 1, "steel", metal(S, 3), skip=("up",)),
        post("blade_lo", 24, 28, 1.5, 0.5, "steel", metal(S, 4)),
        post("blade_mid", 28, 31, 1, 0.5, "steel", metal(S, 5)),
        post("tip", 31, 33, 0.5, 0.5, "steel", metal(S, 6), skip=("down",)),
    ]

def coral_trident():
    """An upright trident: a bleached coral shaft, a bronze crossbar and three prongs with barbs."""
    return [
        post("foot", -12, -10, 0.5, 0.5, "bone", metal(CORAL, 3), skip=("up",)),
        post("shaft_lo", -10, -2, 0.5, 0.5, "bone", grain(CORAL, 3, 3), skip=("down", "up")),
        Cube("grip", [-1, -2, -1], [1, 2, 1], "leather", wrap(L[2], L[1]), skip=("up", "down")),
        post("shaft", 2, 18, 0.5, 0.5, "bone", grain(CORAL, 3, 7), skip=("down", "up")),
        post("band", 8, 9, 1, 1, "gilt", metal(G, 3)),
        post("cross", 18, 20, 3, 0.5, "gilt", metal(G, 4)),
        post("prong_c", 20, 29, 0.5, 0.5, "gilt", metal(G, 5), skip=("down",)),
        bit("prong_l", -3, -2, 20, 27, "gilt", metal(G, 5), skip=("down",)),
        bit("prong_r", 2, 3, 20, 27, "gilt", metal(G, 5), skip=("down",)),
        bit("barb_l", -2, -1, 22, 23, "gilt", metal(G, 4)),
        bit("barb_r", 1, 2, 22, 23, "gilt", metal(G, 4)),
    ]

TIERS = {
    "hearth_axe": (hearth_axe, "The Hearthguard's hearth axe (Ironclad), hand-authored at 1 texel a px: a dark ash haft with an ember band, an iron socket and a bearded iron head with a hammered edge and a rear spike; one-handed, held like a sword."),
    "boar_spear": (boar_spear, "The Tusk's boar spear (Wildmoot), hand-authored at 1 texel a px: a pale ash shaft with a hide grip and binder, a bone crossbar under a steel leaf head; held upright."),
    "coral_trident": (coral_trident, "The Tideguard's coral trident (Tidecallers), hand-authored at 1 texel a px: a bleached coral shaft, a bronze crossbar and three barbed prongs; held upright."),
}
UPRIGHT = {"offset": [0, 0, 0], "rotation": [0, 0, 0], "note": "held upright like the sun staff"}
W_outputs = W.outputs

def outputs(tiers=None):
    out = W_outputs(tiers or TIERS)
    for wid in (tiers or TIERS):
        if wid != "hearth_axe": out[wid + ".json"]["grip"] = UPRIGHT
    return out

def main():
    W.outputs = outputs
    W.main(TIERS, __doc__)

if __name__ == "__main__":
    main()
