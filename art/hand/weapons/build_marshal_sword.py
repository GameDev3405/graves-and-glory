#!/usr/bin/env python3
"""The Lord Marshal's sword, tiers I to III, hand-authored at one texel per model pixel to match the 1x Marshal.

A commander's broadsword: a blade two pixels wider than the knight's with a deep fuller, and the "gate guard", a heavy
bar with an iron post hanging from each end toward the blade. Tier I is blackened iron; II bright steel, the posts rising
as well as hanging and capped in gilt, a wheel pommel stamped with the cross; III gilds the bar, the pommel and the
posts' crowns, sets a steel boss over the bar and a gilt sun in the fuller.

Same frame, light rules, helpers and files as build_swords.py.

  python build_marshal_sword.py preview <png> | install
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_swords as W
from build_swords import Cube, flat, by_face, taper, wrap, bar, STEEL as S, GILT as G, LEATH as L
from build_longsword import wheel


def post(name, x, y0, y1, mat, lit, mid, dark, cap=None):
    """A guard post one pixel square from y0 to y1, lit on its outer flat; `cap` paints its two end texels."""
    def side(face, w, h):
        g = [[mid] * w for _ in range(h)]
        g[0] = [lit]; g[h - 1] = [dark]
        if cap: g[0] = [cap]; g[h - 1] = [cap]
        return g
    return Cube(name, [x, y0, -0.5], [x + 1, y1, 0.5], mat,
                by_face(up=flat(cap or dark), down=flat(cap or lit), rest=side))


def blade_cube(y0, y1, cols, rows, dark, edge):
    paint = taper(cols, rows, dark)
    return Cube("blade", [-2.5, y0, -0.5], [2.5, y1, 0.5], "steel" if edge != S[3] else "iron",
                by_face(north=paint, south=paint, up=flat(edge), rest=flat(edge)), skip=("down",))


def point(y, mat, a, b, c):
    """The point in two steps: three wide then one."""
    return [Cube("tip", [-1.5, y, -0.5], [1.5, y + 2, 0.5], mat, by_face(up=flat(a), rest=bar(a, b, b, c)), skip=("down",)),
            Cube("tip2", [-0.5, y + 2, -0.5], [0.5, y + 3, 0.5], mat, by_face(up=flat(a), rest=flat(a)), skip=("down",))]


def tier1():
    """The field sword: a broad blackened-iron blade with a dark fuller and two nicks, an iron gate guard whose posts
    hang toward the blade, a leather grip and a small iron wheel pommel."""
    def rows(face, y, h): return {0: S[2]} if y == 5 else ({4: S[1]} if y == 8 else {})
    return [
        Cube("pommel", [-1.5, -6, -0.5], [1.5, -3, 0.5], "iron", by_face(north=wheel(S[4], S[3], S[1], S[2]), south=wheel(S[4], S[3], S[1], S[2]),
             up=flat(S[4]), down=flat(S[1]), rest=flat(S[2]))),
        Cube("grip", [-0.5, -3, -0.5], [0.5, 2, 0.5], "leather", wrap(L[2], L[1]), skip=("up", "down")),
        Cube("bar", [-5, 2, -1], [5, 3, 1], "iron", by_face(up=flat(S[4]), down=flat(S[1]), rest=bar(S[3], S[3], S[2], S[2]))),
        post("post_l", -5, 3, 5, "iron", S[3], S[2], S[1]),
        post("post_r", 4, 3, 5, "iron", S[3], S[2], S[1]),
        blade_cube(3, 15, [S[3], S[4], S[1], S[3], S[2]], rows, S[2], S[3]),
    ] + point(15, "iron", S[4], S[3], S[2])


def tier2():
    """The marshal's steel: a bright broad blade with a fuller lit at its root, a steel gate guard whose posts rise and
    hang, each end capped in gilt, a leather grip and a wheel pommel stamped with the cross."""
    def rows(face, y, h): return {2: S[6]} if y >= h - 3 else {}
    cross_wheel = wheel(S[5], S[3], S[2], S[1], cross=S[6])
    return [
        Cube("pommel", [-2.5, -8, -0.5], [2.5, -3, 0.5], "steel", by_face(north=cross_wheel, south=cross_wheel,
             up=flat(S[5]), down=flat(S[1]), rest=flat(S[3]))),
        Cube("grip", [-0.5, -3, -0.5], [0.5, 2, 0.5], "leather", wrap(L[3], L[1]), skip=("up", "down")),
        Cube("bar", [-5, 2, -1], [5, 3, 1], "steel", by_face(up=flat(S[6]), down=flat(S[1]), rest=bar(S[5], S[4], S[2], S[3]))),
        post("post_l", -6, 0, 5, "steel", S[5], S[4], S[2], cap=G[5]),
        post("post_r", 5, 0, 5, "steel", S[5], S[4], S[2], cap=G[5]),
        blade_cube(3, 17, [S[5], S[6], S[2], S[4], S[3]], rows, S[3], S[6]),
    ] + point(17, "steel", S[6], S[5], S[3])


def tier3():
    """The Lord Marshal's own: a polished broad blade with a gilt sun set in the fuller over a gilt ricasso, a gilt gate
    guard under a steel boss, its tall steel posts banded and crowned in gilt, a gilt-wired grip and a gilt wheel pommel
    inlaid with a steel cross and capped by a button."""
    def rows(face, y, h):
        k = h - 1 - y
        if k < 2: return {1: G[4], 2: G[5], 3: G[3]}                          # the ricasso
        if k == 4: return {1: G[3], 2: G[5], 3: G[3]}                         # the sun's arms
        if k in (3, 5): return {2: G[4]}
        return {2: S[6]} if k < 8 else {}
    gilt_wheel = wheel(G[5], G[3], G[2], G[1], cross=S[6], hub=S[5])
    def banded(face, w, h):
        g = [[S[4]] * w for _ in range(h)]
        if h > 3:
            for y in (1, h // 2, h - 2): g[y] = [G[4]] * w
        return g
    return [
        Cube("pommel", [-2.5, -9, -0.5], [2.5, -4, 0.5], "gilt", by_face(north=gilt_wheel, south=gilt_wheel,
             up=flat(G[5]), down=flat(G[1]), rest=flat(G[3]))),
        Cube("button", [-0.5, -10, -0.5], [0.5, -9, 0.5], "gilt", by_face(up=flat(G[5]), rest=flat(G[4]))),
        Cube("grip", [-0.5, -4, -0.5], [0.5, 2, 0.5], "leather", wrap(L[3], L[1], G[4]), skip=("up", "down")),
        Cube("bar", [-6, 2, -1], [6, 3, 1], "gilt", by_face(up=flat(G[5]), down=flat(G[1]), rest=bar(G[5], G[4], G[3], G[3]))),
        Cube("boss", [-1.5, 1, -1.5], [1.5, 4, 1.5], "steel", by_face(up=flat(S[6]), down=flat(S[1]), rest=wheel(S[6], S[4], S[2], S[3], hub=G[5]))),
        Cube("post_l", [-7, 0, -0.5], [-6, 5, 0.5], "steel", by_face(rest=banded)),
        Cube("post_r", [6, 0, -0.5], [7, 5, 0.5], "steel", by_face(rest=banded)),
        Cube("crown_l", [-7, -1, -0.5], [-6, 0, 0.5], "gilt", by_face(up=flat(G[6]), rest=flat(G[5]))),
        Cube("crown_r", [6, -1, -0.5], [7, 0, 0.5], "gilt", by_face(up=flat(G[6]), rest=flat(G[5]))),
        Cube("foot_l", [-7, 5, -0.5], [-6, 6, 0.5], "gilt", by_face(rest=flat(G[4]))),
        Cube("foot_r", [6, 5, -0.5], [7, 6, 0.5], "gilt", by_face(rest=flat(G[4]))),
        blade_cube(4, 19, [S[5], S[6], S[2], S[4], S[3]], rows, S[3], S[6]),
    ] + point(19, "steel", S[6], S[5], S[4])


TIERS = {
    "marshal_sword": (tier1, "The Lord Marshal's sword, tier I (plain), hand-authored at 1 texel a px in the Marshal's ramps: a broad "
                      "5 px blackened-iron blade with a dark fuller and two nicks (12 px and a 3 px stepped point), an iron gate "
                      "guard 10 px wide with a post hanging 2 px from each end, a 5 px leather grip and a 3 px iron wheel pommel; "
                      "24 px long."),
    "marshal_sword_t2": (tier2, "The Lord Marshal's sword, tier II (upgraded), hand-authored at 1 texel a px: a bright 5 px steel "
                         "blade with its fuller lit at the root (14 px and 3), a 12 px steel gate guard whose posts rise and hang, "
                         "each end capped in gilt, a leather grip and a 5 px wheel pommel stamped with the cross; 28 px long."),
    "marshal_sword_t3": (tier3, "The Lord Marshal's sword, tier III (elite), hand-authored at 1 texel a px: a polished 5 px blade "
                         "with a gilt sun in its fuller over a gilt ricasso (15 px and 3), a 12 px gilt gate guard under a steel "
                         "boss, tall steel posts banded in gilt with gilt crowns and feet, a gilt-wired grip and a gilt wheel "
                         "pommel inlaid with a steel cross and capped by a button; 32 px long."),
}


def outputs(): return W.outputs(TIERS)


if __name__ == "__main__":
    W.main(TIERS, __doc__)
