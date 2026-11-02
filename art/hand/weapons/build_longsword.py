#!/usr/bin/env python3
"""The Templar's longsword, tiers I to III, hand-authored at one texel per model pixel to match the 1x Templar.

A hand and a half longer than the knight's arming sword: a long grip for two fists, a wheel pommel, and a cross whose
arms droop toward the blade, the Order's mark. Tier I is dark iron with chipped edges; II bright steel with a fuller and
a cross stamped in the pommel; III gilds the cross, the pommel and the ricasso and sets a gilt cross in the fuller.

Same frame, light rules, helpers and files as build_swords.py.

  python build_longsword.py preview <png> | install
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_swords as W
from build_swords import Cube, flat, by_face, taper, across, wrap, ring, bar, STEEL as S, GILT as G, LEATH as L


def arms(mat, lit, mid, dark, tip, y, droop):
    """The cross's two arms, each turned `droop` degrees down toward the blade from the centre block, and their
    terminals: flat end blocks one texel taller than the arm."""
    paint = by_face(up=flat(lit), down=flat(dark), rest=bar(lit, mid, dark))
    end = by_face(up=flat(tip), down=flat(dark), rest=bar(tip, mid, dark))
    return [
        Cube("arm_l", [-5, y, -0.5], [-1, y + 1, 0.5], mat, paint, turn=droop, pivot=[-1, y + 0.5, 0]),
        Cube("arm_r", [1, y, -0.5], [5, y + 1, 0.5], mat, paint, turn=-droop, pivot=[1, y + 0.5, 0]),
        Cube("end_l", [-6, y - 1.5, -0.5], [-5, y + 0.5, 0.5], mat, end, turn=droop, pivot=[-1, y + 0.5, 0]),
        Cube("end_r", [5, y - 1.5, -0.5], [6, y + 0.5, 0.5], mat, end, turn=-droop, pivot=[1, y + 0.5, 0]),
    ]


def wheel(lit, mid, dark, corner, cross=None, hub=None):
    """A round pommel's flat: dark corners round it off, lit along the top, shadowed along the bottom; `cross` stamps
    a cross from the centre to one texel short of the rim, `hub` sets the centre."""
    def paint(face, w, h):
        g = [[mid] * w for _ in range(h)]
        for x in range(w): g[0][x] = lit; g[h - 1][x] = dark
        for (x, y) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)): g[y][x] = corner
        if cross:
            for i in range(1, w - 1): g[h // 2][i] = cross
            for i in range(1, h - 1): g[i][w // 2] = cross
        if hub: g[h // 2][w // 2] = hub
        return g
    return paint


def tier1():
    """The initiate's iron longsword: a chipped iron blade with a dull ridge, an iron cross drooping into flat
    terminals, a long leather grip with an iron ferrule and an iron ring pommel."""
    chips = lambda face, y, h: {0: S[2]} if y in (3, 9) else ({2: S[2]} if y == 6 else {})
    blade = taper([S[3], S[4], S[2]], chips, S[2])
    return [
        Cube("pommel", [-1.5, -7, -0.5], [1.5, -4, 0.5], "iron", by_face(north=wheel(S[4], S[3], S[1], S[2]), south=wheel(S[4], S[3], S[1], S[2]),
             up=flat(S[4]), down=flat(S[1]), rest=flat(S[2]))),
        Cube("ferrule", [-1, -4, -1], [1, -3, 1], "iron", by_face(up=flat(S[4]), down=flat(S[1]), rest=bar(S[3], S[3], S[2]))),
        Cube("grip", [-0.5, -3, -0.5], [0.5, 2, 0.5], "leather", wrap(L[2], L[1]), skip=("up", "down")),
        Cube("block", [-1.5, 2, -1], [1.5, 3, 1], "iron", by_face(up=flat(S[4]), down=flat(S[1]), rest=bar(S[3], S[3], S[2], S[2]))),
    ] + arms("iron", S[4], S[3], S[1], S[4], 2, 12) + [
        Cube("blade", [-1.5, 3, -0.5], [1.5, 16, 0.5], "iron",
             by_face(north=blade, south=blade, up=flat(S[3]), rest=flat(S[4])), skip=("down",)),
        Cube("tip", [-0.5, 16, -0.5], [0.5, 18, 0.5], "iron", by_face(up=flat(S[5]), rest=flat(S[4])), skip=("down",)),
    ]


def tier2():
    """The brother's steel longsword: a bright blade with a long fuller, a steel cross over a langet block, a leather
    grip and a wheel pommel stamped with the cross."""
    def fuller(face, y, h):
        return {1: S[3] if y >= h // 4 else S[6]} | ({1: S[4]} if y == h // 4 else {})
    blade = taper([S[5], S[3], S[4]], fuller, S[3])
    cross_wheel = wheel(S[5], S[3], S[2], S[1], cross=S[6])
    return [
        Cube("pommel", [-2.5, -9, -0.5], [2.5, -4, 0.5], "steel", by_face(north=cross_wheel, south=cross_wheel,
             up=flat(S[5]), down=flat(S[1]), rest=flat(S[3]))),
        Cube("ferrule", [-1, -4, -1], [1, -3, 1], "steel", by_face(up=flat(S[5]), down=flat(S[1]), rest=bar(S[5], S[4], S[2]))),
        Cube("grip", [-0.5, -3, -0.5], [0.5, 2, 0.5], "leather", wrap(L[3], L[1]), skip=("up", "down")),
        Cube("block", [-1.5, 1, -1], [1.5, 4, 1], "steel", by_face(up=flat(S[6]), down=flat(S[1]), rest=bar(S[5], S[4], S[2], S[3]))),
    ] + arms("steel", S[6], S[4], S[2], S[6], 2, 15) + [
        Cube("blade", [-1.5, 4, -0.5], [1.5, 18, 0.5], "steel",
             by_face(north=blade, south=blade, up=flat(S[5]), rest=flat(S[6])), skip=("down",)),
        Cube("tip", [-0.5, 18, -0.5], [0.5, 20, 0.5], "steel", by_face(up=flat(S[6]), rest=flat(S[6])), skip=("down",)),
    ]


def tier3():
    """The commander's gilded longsword: a polished blade with a gilt cross set in the fuller above a gilt ricasso,
    a gilt cross with drooping arms and lit terminals, a gilt-wired grip and a gilt wheel pommel with a steel cross."""
    def fuller(face, y, h):
        if y >= h - 3: return {1: (G[5], G[4], G[2])[y - (h - 3)]}          # the ricasso
        if y in (h - 7, h - 6): return {0: G[4], 1: G[5], 2: G[3]} if y == h - 7 else {1: G[4]}   # the cross's arms
        if h - 9 <= y < h - 3: return {1: G[4]}                              # the cross's stem
        if y >= h // 4: return {1: S[3]}
        return {1: S[6]}
    blade = taper([S[5], S[3], S[4]], fuller, S[3])
    gilt_wheel = wheel(G[5], G[3], G[2], G[1], cross=S[6], hub=S[5])
    return [
        Cube("pommel", [-2.5, -9, -0.5], [2.5, -4, 0.5], "gilt", by_face(north=gilt_wheel, south=gilt_wheel,
             up=flat(G[5]), down=flat(G[1]), rest=flat(G[3]))),
        Cube("button", [-0.5, -10, -0.5], [0.5, -9, 0.5], "gilt", by_face(up=flat(G[5]), rest=flat(G[4]))),
        Cube("ferrule", [-1, -4, -1], [1, -3, 1], "gilt", by_face(up=flat(G[5]), down=flat(G[1]), rest=bar(G[5], G[3], G[2]))),
        Cube("grip", [-0.5, -3, -0.5], [0.5, 2, 0.5], "leather", wrap(L[3], L[1], G[4]), skip=("up", "down")),
        Cube("block", [-1.5, 1, -1], [1.5, 4, 1], "gilt", by_face(up=flat(G[5]), down=flat(G[1]), rest=bar(G[5], G[3], G[2], G[2]))),
    ] + arms("gilt", G[5], G[3], G[1], G[6], 2, 15) + [
        Cube("blade", [-1.5, 4, -0.5], [1.5, 19, 0.5], "steel",
             by_face(north=blade, south=blade, up=flat(S[5]), rest=flat(S[6])), skip=("down",)),
        Cube("tip", [-0.5, 19, -0.5], [0.5, 21, 0.5], "steel", by_face(up=flat(S[6]), rest=flat(S[6])), skip=("down",)),
    ]


TIERS = {
    "longsword": (tier1, "The Templar's longsword, tier I (plain), hand-authored at 1 texel a px in the Templar's ramps: a 3 px "
                  "dark-iron blade with a dull ridge and three edge chips (13 px and a 2 px point), an iron cross whose arms "
                  "droop 12 degrees into flat terminals, a 5 px leather grip over an iron ferrule and a 3 px iron ring pommel; "
                  "no gilt; 25 px long."),
    "longsword_t2": (tier2, "The Templar's longsword, tier II (upgraded), hand-authored at 1 texel a px: a 3 px bright steel "
                     "blade with a fuller three quarters up (14 px and 2), a steel langet block and cross drooping 15 degrees "
                     "into lit terminals, a leather grip over a steel ferrule and a 5 px wheel pommel stamped with the cross; "
                     "29 px long."),
    "longsword_t3": (tier3, "The Templar's longsword, tier III (elite), hand-authored at 1 texel a px: a polished 3 px blade "
                     "with a gilt cross set in its fuller above a gilt ricasso (15 px and 2), a gilt block and drooping gilt "
                     "cross, a gilt-wired grip over a gilt ferrule and a gilt wheel pommel inlaid with a steel cross and "
                     "capped by a button; 31 px long."),
}


def outputs(): return W.outputs(TIERS)


if __name__ == "__main__":
    W.main(TIERS, __doc__)
