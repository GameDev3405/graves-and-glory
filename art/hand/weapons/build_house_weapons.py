#!/usr/bin/env python3
"""Six House weapons at one texel per model pixel: the Hearthguard's hearth axe (Ironclad), the Tusk's boar spear
(Wildmoot) and the Tidecallers' coral trident, cutlass, harpoon and admiral trident. Each reuses the sun staff's
post/grain/metal painters.

  hearth_axe     blackened-iron bearded axe on a dark ash haft with an ember band; held like a sword (the sword grip).
  boar_spear     pale ash shaft, a steel leaf head standing over a bone crossbar, a hide-wrapped grip; held upright.
  coral_trident  bleached coral shaft, a bronze (gilt) three-prong head on a crossbar; held upright.
  cutlass        the Raider's curved, notched sea-steel blade in a bronze basket guard with a sea-blue wrap; held like a sword.
  harpoon        the Harpooner's driftwood shaft, sea-blue grip, a coil of line and a barbed iron head; held upright.
  admiral_trident the Admiral's dark driftwood trident: gilt fittings, a sea-blue band, three barbed prongs, a pearl at the fork.

  python build_house_weapons.py preview <png> | install
"""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import build_swords as W
from build_swords import Cube, by_face, flat, wrap, across, bar, STEEL as S, GILT as G, LEATH as L, ramp, REPO
from build_sun_staff import post, px, grain, metal, binder, ASH, PALE

W.KEY.update({"wood": (0x8B, 0x45, 0x13), "bone": (0xEE, 0xEE, 0xDD)})
EMBER = ramp("1f0a06", "4a1608", "7a2a0c", "993f12", "c8966f", "ecc9a6")
CORAL = ramp("6f5d57", "8f7d72", "b0a094", "cdbcad", "e8dccd", "faf4e8")
DARK = ramp("120d0a", "2a1a10", "3c2a1c", "56402b", "6f5438", "8a6c4a")
BRASS7 = ramp("2a1c0a", "3e2b12", "664a20", "85642a", "997530", "c8a870", "e0d0a8")          # Ironclad brass with a lit seventh step
IRONW = ramp("0d0e14", "1a1c25", "2c303b", "444a58", "62697a", "8a92a2", "bcc3cf")

SEA = ramp("0a1426", "11264a", "1a3f6a", "2a5e8a", "386a94", "5e88a6", "9db8c4")           # the Tidecallers' sea blue (guide 10.1)
BRZ = ramp("2a1608", "553014", "7a4c1e", "8f6530", "a38a5e", "c9b184")                      # sea-worn bronze, lights eased under the tint rule
DRIFT = ramp("201c1a", "3a322c", "564a40", "74675a", "94887a", "b3a99a")                    # bleached, salt-grey driftwood
ABYSS = ramp("0f0c0b", "201814", "302319", "42332a", "58463a", "70594a")                    # near-black wet driftwood
WBRZ = ramp("2a1608", "553014", "7a4c1e", "8f6530", "a38a5e", "c9b184", "e0cfa6")           # warm bronze with a lit seventh step (tier III heads)
ROPE = ramp("2a2218", "4a3d2b", "6e5d44", "8f7d5e", "b09d78", "cdbf9c")
PEARL = ramp("6a706c", "8e948f", "b4bab4", "d8dcd4", "eef1ec", "ffffff")

def bit(name, x0, x1, y0, y1, mat, fn, skip=()):
    return Cube(name, [x0, y0, -0.5], [x1, y1, 0.5], mat, px(fn, y1), skip=skip)

def hearth_axe(tier=1):
    """A bearded axe: dark haft, ember band, riveted iron head with a hammered edge, a spike behind. Tier II: brass bands on
    the haft and socket, a longer back spike. Tier III: a brass socket, a gilt rune plate on the beard, crown and butt spikes."""
    out = [
        post("butt", -6, -4, 1, 1, "iron", metal(IRONW, 3), skip=("up",)),
        post("haft_lo", -4, -2, 0.5, 0.5, "wood", grain(DARK, 3, 1), skip=("down", "up")),
        Cube("grip", [-1, -2, -1], [1, 2, 1], "leather", wrap(L[2], L[1]), skip=("up", "down")),
        post("haft", 2, 14, 0.5, 0.5, "wood", grain(DARK, 3, 4), skip=("down", "up")),
        post("band", 6, 7, 1, 1, "iron", metal(EMBER, 3)),
        post("socket", 12, 15, 1, 1, "gilt" if tier >= 3 else "iron", metal(BRASS7 if tier >= 3 else IRONW, 3)),
        Cube("beard", [1, 7, -0.5], [4, 15, 0.5], "iron", by_face(rest=lambda f, w, h: [[IRONW[4 if y < h - 2 else 3 if x < w - 1 else 6] for x in range(w)] for y in range(h)])),
        Cube("blade", [4, 8, -0.5], [6, 14, 0.5], "iron", lambda f, w, h: [[IRONW[6 if x == w - 1 else 5 if (f in ("north", "south") and x == w - 2) else 4] for x in range(w)] for y in range(h)]),
        bit("spike", -3 - (tier >= 2), -1, 11, 13, "iron", metal(IRONW, 3)),
    ]
    if tier >= 2:
        out += [post("ring_hi", 10, 11, 1, 1, "gilt", metal(BRASS7, 3)), post("ring_lo", -4, -3, 1, 1, "gilt", metal(BRASS7, 3)),
                bit("spike_tip", -5 - (tier >= 3), -4, 11.5, 12.5, "iron", metal(IRONW, 5))]
    if tier >= 3:
        rune = lambda f, w, h: [[BRASS7[5 if (x + y) % 3 == 0 else 3] for x in range(w)] for y in range(h)]
        out += [Cube("rune", [1.5, 9, -1], [3.5, 13, 1], "gilt", by_face(north=rune, south=rune, rest=flat(BRASS7[2]))),
                post("crown", 15, 18, 0.5, 0.5, "iron", metal(IRONW, 4), skip=("down",)),
                post("butt_spike", -8, -6, 0.5, 0.5, "iron", metal(IRONW, 4), skip=("up",))]
    return out

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

def coral_trident(tier=1):
    """An upright trident: a bleached coral shaft, a bronze crossbar and three prongs with barbs. Tier II adds coral barbs on
    the shaft and a sea-blue band; tier III a pearl at the fork and barbs on the middle prong."""
    out = [
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
    if tier >= 2:
        out += [post("sea_band", 12, 14, 1, 1, "leather", binder(SEA)),
                bit("coral_l", -2.5, -0.5, 14, 17, "bone", grain(CORAL, 3, 1)),
                bit("coral_r", 0.5, 2.5, 15, 18, "bone", grain(CORAL, 3, 2))]
    if tier >= 3:
        out += [post("pearl", 20, 22, 1, 1, "bone", metal(PEARL, 3)),
                bit("barb_cl", -1.5, -0.5, 25, 26, "gilt", metal(G, 5)),
                bit("barb_cr", 0.5, 1.5, 25, 26, "gilt", metal(G, 5))]
    return out

def cutlass(tier=1):
    """A curved cutlass: a notched sea-steel blade swelling toward a clipped point, a bronze basket with a knuckle bow, a sea-blue wrap."""
    st = S
    def seg(name, x0, x1, y0, y1, cols, nick=None, skip=()):
        if tier >= 3 and len(cols) >= 3: cols = cols[:-2] + [SEA[4]] + cols[-1:]   # tier III: a sea-blue fuller
        return Cube(name, [x0, y0, -0.5], [x1, y1, 0.5], "steel",
                    by_face(north=across(cols, nick), south=across(cols, nick), up=flat(st[5]), rest=flat(st[4])), skip=skip)
    spine = lambda w, ys: (lambda f, y, h: {w - 1: st[1]} if y in ys else {})
    M = BRZ if tier < 3 else G
    br = lambda: by_face(up=flat(M[4]), down=flat(M[1]), rest=bar(M[4], M[3], M[2], M[1]))
    pearl = by_face(up=flat(PEARL[4]), down=flat(PEARL[1]), rest=bar(PEARL[4], PEARL[3], PEARL[2], PEARL[1]))
    out = [
        Cube("pommel", [-1, -4, -1], [1, -2, 1], "gilt" if tier < 3 else "bone", br() if tier < 3 else pearl),
        Cube("grip", [-0.5, -2, -0.5], [0.5, 2, 0.5], "leather", wrap(SEA[3], SEA[1]), skip=("up", "down")),
        Cube("guard", [-3.5, 2, -1], [3.5, 3, 1], "gilt", br()),
        Cube("bow", [-3.5, -3, -0.5], [-2.5, 2, 0.5], "gilt", br()),
        Cube("bow_lo", [-3, -4, -0.5], [-1, -3, 0.5], "gilt", br()),
        Cube("bow_mid", [-2.5, 0, -0.5], [-1.5, 1, 0.5], "gilt", flat(BRZ[2])),
        seg("b1", -1.5, 1.5, 3, 6, [st[5], st[4], st[2]], spine(3, (1,)), skip=("down",)),
        seg("b2", -1, 2, 6, 9, [st[6], st[4], st[2]], spine(3, (0, 2))),
        seg("b3", -0.5, 3.5, 9, 12, [st[6], st[5], st[3], st[2]], spine(4, (1,))),
        seg("b4", 0.5, 4.5, 12, 15, [st[6], st[5], st[3], st[2]], spine(4, (0, 2))),
        seg("b5", 1.5, 4.5, 15, 17, [st[6], st[4], st[2]]),
        seg("tip", 3, 5, 17, 18, [st[6], st[3]]),
        seg("point", 4, 5, 18, 19, [st[6]]),
    ]
    if tier >= 2:   # a sea-blue enamelled shell plate on the guard and a down-curled quillon
        out += [Cube("shell", [-2.5, 3, -1.5], [2.5, 4, 1.5], "leather", by_face(up=flat(SEA[4]), down=flat(SEA[1]), rest=bar(SEA[5], SEA[3], SEA[2], SEA[1]))),
                Cube("quillon", [3.5, 1, -0.5], [4.5, 3, 0.5], "gilt", br())]
    return out

def harpoon(tier=1):
    """An upright harpoon: a salt-grey driftwood shaft, a sea-blue grip, a hemp coil with a trailing line, a barbed iron head.
    Tier II: a bronze collar, a sea-blue coil and longer steel hooks; tier III a pearl-set bronze head with a sea-blue ring."""
    HD = IRONW if tier == 1 else (S if tier == 2 else WBRZ)
    hk = 0 if tier == 1 else 1
    return [
        post("foot", -14, -12, 0.5, 0.5, "iron", metal(IRONW, 3), skip=("up",)),
        post("shaft_lo", -12, -2, 0.5, 0.5, "wood", grain(DRIFT, 3, 2), skip=("down", "up")),
        Cube("grip", [-1, -2, -1], [1, 2, 1], "leather", wrap(SEA[3], SEA[1]), skip=("up", "down")),
        post("shaft", 2, 20, 0.5, 0.5, "wood", grain(DRIFT, 3, 5), skip=("down", "up")),
        post("sea", 5, 7, 1, 1, "leather", binder(SEA)),
        post("coil_a", 13, 14, 1.5, 1.5, "leather", binder(ROPE)),
        post("coil_b", 14, 15, 1, 1, "leather", binder(ROPE if tier == 1 else SEA)),
        post("coil_c", 15, 16, 1.5, 1.5, "leather", binder(ROPE)),
        post("coil_d", 16, 17, 1, 1, "leather", binder(ROPE)),
        bit("line", 1.5, 2.5, 7, 15, "leather", metal(ROPE, 3)),
        bit("line_end", 2.5, 3.5, 7, 10, "leather", metal(ROPE, 2)),
        post("collar", 20, 22, 1, 1, "iron" if tier == 1 else ("gilt" if tier == 2 else "leather"),
             metal(IRONW, 3) if tier == 1 else (metal(WBRZ, 4) if tier == 2 else binder(SEA)), skip=("up",)),
        post("head_lo", 22, 26, 1.5, 0.5, "iron" if tier < 3 else "bone", metal(HD, 4) if tier < 3 else metal(PEARL, 3)),
        post("head_mid", 26, 30 + hk, 1, 0.5, "iron", metal(HD, 5)),
        post("tip", 30 + hk, 33 + hk, 0.5, 0.5, "iron", metal(HD, 6), skip=("down",)),
        bit("barb_l", -2.5, -1.5, 23, 27 + hk, "iron", metal(HD, 4)),
        bit("barb_r", 1.5, 2.5, 23, 27 + hk, "iron", metal(HD, 4)),
        bit("hook_l", -3.5 - hk, -2.5, 22 - hk, 25, "iron", metal(HD, 5)),
        bit("hook_r", 2.5, 3.5 + hk, 22 - hk, 25, "iron", metal(HD, 5)),
    ]

def admiral_trident(tier=1):
    """The Admiral's trident: dark wet driftwood, gilt rings and crown, a sea-blue band, three barbed prongs, a pearl at the fork.
    Tier II: a sea-blue lower band, a broader crossbar and a longer middle prong; tier III pearl-tipped outer prongs."""
    k = 1 if tier > 1 else 0
    out = [
        post("foot", -14, -12, 1, 1, "gilt", metal(G, 3), skip=("up",)),
        post("shaft_lo", -12, -2, 0.5, 0.5, "wood", grain(ABYSS, 3, 3), skip=("down", "up")),
        post("ring_lo", -7 - k, -6, 1, 1, "gilt" if tier == 1 else "leather", metal(G, 3) if tier == 1 else binder(SEA)),
        Cube("grip", [-1, -2, -1], [1, 2, 1], "leather", wrap(SEA[3], SEA[1], G[3]), skip=("up", "down")),
        post("shaft", 2, 19, 0.5, 0.5, "wood", grain(ABYSS, 3, 8), skip=("down", "up")),
        post("ring_a", 5, 6, 1, 1, "gilt", metal(G, 3)),
        post("sea", 6, 9, 1, 1, "leather", binder(SEA)),
        post("ring_b", 9, 10, 1, 1, "gilt", metal(G, 3)),
        post("collar", 19, 21, 1.5, 1, "gilt", metal(G, 4), skip=("up",)),
        post("cross", 21, 22, 3 + k, 0.5, "gilt", metal(G, 4)),
        bit("pearl", -1, 1, 22, 24, "bone", metal(PEARL, 3)),
        post("prong_c", 24, 33 + 2 * k, 0.5, 0.5, "gilt", metal(G, 5), skip=("down",)),
        bit("prong_l", -3, -2, 22, 28, "gilt", metal(G, 5), skip=("down",)),
        bit("prong_r", 2, 3, 22, 28, "gilt", metal(G, 5), skip=("down",)),
        bit("tip_l", -2, -1, 28, 30, "gilt", metal(G, 6), skip=("down",)),
        bit("tip_r", 1, 2, 28, 30, "gilt", metal(G, 6), skip=("down",)),
        bit("barb_cl", -1.5, -0.5, 26, 28, "gilt", metal(G, 4)),
        bit("barb_cr", 0.5, 1.5, 26, 28, "gilt", metal(G, 4)),
        bit("barb_ol", -4, -3, 23, 25, "gilt", metal(G, 4)),
        bit("barb_or", 3, 4, 23, 25, "gilt", metal(G, 4)),
    ]
    if tier >= 3:   # pearl tips on the outer prongs
        out = [c for c in out if c.name not in ("tip_l", "tip_r")] + [
            bit("tip_l", -3, -1, 28, 30, "bone", metal(PEARL, 3)), bit("tip_r", 1, 3, 28, 30, "bone", metal(PEARL, 3))]
    return out

def boar_spear_t2():
    """Tier II: a steel-bound ash shaft, a wider bone crossbar with short tusk lugs, a broader steel leaf."""
    return [
        post("foot", -14, -12, 0.5, 0.5, "steel", metal(S, 4), skip=("up",)),
        post("shaft_lo", -12, -3, 0.5, 0.5, "wood", grain(PALE, 3, 2), skip=("down", "up")),
        post("ring_lo", -3, -2, 1, 1, "steel", metal(S, 3), skip=("up",)),
        Cube("grip", [-1, -2, -1], [1, 2, 1], "leather", wrap(L[2], L[1]), skip=("up", "down")),
        post("ring_hi", 2, 3, 1, 1, "steel", metal(S, 3)),
        post("shaft", 3, 21, 0.5, 0.5, "wood", grain(PALE, 3, 6), skip=("down", "up")),
        post("binder_lo", 7, 9, 1, 1, "leather", binder(DARK)),
        post("binder", 14, 17, 1, 1, "leather", binder(DARK)),
        post("cross", 21, 22, 4, 0.5, "bone", metal(CORAL, 3)),
        bit("lug_l", -4, -3, 22, 24, "bone", metal(CORAL, 3), skip=("down",)),
        bit("lug_r", 3, 4, 22, 24, "bone", metal(CORAL, 3), skip=("down",)),
        post("collar", 22, 24, 1, 1, "steel", metal(S, 3), skip=("up",)),
        post("blade_lo", 24, 29, 2, 0.5, "steel", metal(S, 4)),
        post("blade_mid", 29, 33, 1.5, 0.5, "steel", metal(S, 5)),
        post("blade_hi", 33, 35, 1, 0.5, "steel", metal(S, 5)),
        post("tip", 35, 37, 0.5, 0.5, "steel", metal(S, 6), skip=("down",)),
    ]

def boar_spear_t3():
    """Tier III: a dark stained shaft in gilt bands, a bone crossbar with swept tusk lugs, a bristle tuft, a long gilt-collared head with barbs."""
    return [
        post("foot", -14, -12, 0.5, 0.5, "gilt", metal(G, 4), skip=("up",)),
        post("shaft_lo", -12, -3, 0.5, 0.5, "wood", grain(DARK, 3, 2), skip=("down", "up")),
        post("ring_lo", -3, -2, 1, 1, "gilt", metal(G, 3), skip=("up",)),
        Cube("grip", [-1, -2, -1], [1, 2, 1], "leather", wrap(L[2], L[1]), skip=("up", "down")),
        post("ring_hi", 2, 3, 1, 1, "gilt", metal(G, 3)),
        post("shaft", 3, 21, 0.5, 0.5, "wood", grain(DARK, 3, 6), skip=("down", "up")),
        post("binder_lo", 7, 9, 1, 1, "gilt", binder(G)),
        post("binder", 14, 17, 1, 1, "gilt", binder(G)),
        bit("tuft_l", -1.5, -0.5, 17, 21, "leather", metal(DARK, 3)),
        bit("tuft_r", 0.5, 1.5, 17, 21, "leather", metal(DARK, 3)),
        post("cross", 21, 22, 4, 0.5, "bone", metal(CORAL, 3)),
        bit("lug_l", -4, -3, 22, 24, "bone", metal(CORAL, 3), skip=("down",)),
        bit("lug_r", 3, 4, 22, 24, "bone", metal(CORAL, 3), skip=("down",)),
        bit("sweep_l", -5, -4, 23, 26, "bone", metal(CORAL, 3), skip=("down",)),
        bit("sweep_r", 4, 5, 23, 26, "bone", metal(CORAL, 3), skip=("down",)),
        post("collar", 22, 25, 1, 1, "gilt", metal(G, 3), skip=("up",)),
        post("blade_lo", 25, 30, 2, 0.5, "steel", metal(S, 4)),
        post("blade_mid", 30, 35, 1.5, 0.5, "steel", metal(S, 5)),
        post("blade_hi", 35, 38, 1, 0.5, "steel", metal(S, 5)),
        post("tip", 38, 40, 0.5, 0.5, "steel", metal(S, 6), skip=("down",)),
        bit("barb_l", -3, -2, 25, 27, "steel", metal(S, 4)),
        bit("barb_r", 2, 3, 25, 27, "steel", metal(S, 4)),
    ]

TIERS = {
    "hearth_axe": (hearth_axe, "The Hearthguard's hearth axe (Ironclad), hand-authored at 1 texel a px: a dark ash haft with an ember band, an iron socket and a bearded iron head with a hammered edge and a rear spike; one-handed, held like a sword."),
    "boar_spear": (boar_spear, "The Tusk's boar spear (Wildmoot), hand-authored at 1 texel a px: a pale ash shaft with a hide grip and binder, a bone crossbar under a steel leaf head; held upright."),
    "boar_spear_t2": (boar_spear_t2, "The Tusk's boar spear, tier II (upgraded), hand-authored at 1 texel a px: steel rings and two hide binders on a pale ash shaft, a wider bone crossbar with tusk lugs under a broader steel leaf head; held upright."),
    "boar_spear_t3": (boar_spear_t3, "The Tusk's boar spear, tier III (elite), hand-authored at 1 texel a px: a dark stained shaft in gilt bands, a hide bristle tuft, a bone crossbar with swept tusk lugs and a long gilt-collared barbed steel head; held upright."),
    "coral_trident": (coral_trident, "The Tideguard's coral trident (Tidecallers), hand-authored at 1 texel a px: a bleached coral shaft, a bronze crossbar and three barbed prongs; held upright."),
    "cutlass": (cutlass, "The Drowned Raider's cutlass (Tidecallers), hand-authored at 1 texel a px: a curved, notched sea-steel blade clipping to a point, a bronze basket guard with a knuckle bow and a sea-blue wrap; one-handed, held like a sword."),
    "harpoon": (harpoon, "The Harpooner's harpoon (Tidecallers), hand-authored at 1 texel a px: a salt-grey driftwood shaft, a sea-blue grip and band, a hemp coil with a trailing line and a barbed iron head; held upright."),
    "admiral_trident": (admiral_trident, "Admiral Vael's trident (Tidecallers), hand-authored at 1 texel a px: a dark wet driftwood shaft with gilt rings and crown, a sea-blue band, three barbed gilt prongs and a pearl at the fork; held upright."),
}
_T2 = {"hearth_axe": "brass bands on the haft and socket and a longer back spike", "coral_trident": "coral barbs and a sea-blue band on the shaft", "cutlass": "a sea-blue enamelled shell guard and a quillon",
       "harpoon": "a bronze collar, a sea-blue coil and a longer steel head and hooks", "admiral_trident": "a sea-blue lower band, a broader crossbar and a longer middle prong"}
_T3 = {"hearth_axe": "a brass socket, a gilt rune plate on the beard, a crown spike and a butt spike", "coral_trident": "adds a pearl at the fork and barbs on the middle prong", "cutlass": "a gilt basket, a pearl pommel and a sea-blue fuller",
       "harpoon": "a bronze head set with pearl and a sea-blue collar", "admiral_trident": "adds pearl tips on the outer prongs"}
for _wid in _T2:
    _fn = TIERS[_wid][0]
    TIERS[_wid + "_t2"] = ((lambda f: lambda: f(2))(_fn), TIERS[_wid][1] + " Tier II: " + _T2[_wid] + ".")
    TIERS[_wid + "_t3"] = ((lambda f: lambda: f(3))(_fn), TIERS[_wid][1] + " Tier III: " + _T2[_wid] + "; " + _T3[_wid] + ".")
UPRIGHT = {"offset": [0, 0, 0], "rotation": [0, 0, 0], "note": "held upright like the sun staff"}
W_outputs = W.outputs

def outputs(tiers=None):
    out = W_outputs(tiers or TIERS)
    for wid in (tiers or TIERS):
        if wid.split("_t")[0] not in ("hearth_axe", "cutlass"): out[wid + ".json"]["grip"] = UPRIGHT
    return out

def main():
    W.outputs = outputs
    W.main(TIERS, __doc__)

if __name__ == "__main__":
    main()
