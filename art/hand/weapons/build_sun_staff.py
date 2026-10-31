#!/usr/bin/env python3
"""The Sunweaver's staff, tiers I to III, hand-authored at one texel per model pixel to match the 1x Gilded Order troops.

The staff of an old white-robed sun priest: a warm shaft, a foot that bites the ground, a leather grip and a gilt sun on
cream rays. Each tier is grander than the last. Tier I's head is drawn as pixel art in a small character grid (HEAD_I) and
cut into cubes by `plate`: a rim and field at two px deep, the rays one px deep, so the sun stands proud of its own rays;
tiers II and III build a larger sun from a few slabs and bars (`head`), to stay under the 128-quad frame cap. Each has a
core, a single 1 px cube three px deep with its north and south faces lit. That core is the only glow of a tier: 2 texels, 8
against the weapon glow cap (a 1x texel counts 4), the rest of the budget being the wearer's brooch.

  I    sun_staff     ash shaft, iron foot and spike, leather grip between two gilt rings, a third gilt ring, a gilt sun of
                     3 x 3 on four cream rays.
  II   sun_staff_t2  pale wood bound in steel, a steel foot, steel rings and binders, a steel collar and cup, a 5 x 5 gilt
                     sun on eight cream rays.
  III  sun_staff_t3  a dark stained shaft in gilt fittings, a steel spike in a gilt shoe, a gilt-wired grip, a cross-bar
                     finial with two hanging gilt chains, a gilt cup and a 7 x 7 gilt sun on eight long gilt rays.

Frame: Blockbench, y up, the grip's centre at the origin (the fist covers y -2..+2), the head up +y, the staff's broad faces
across z (north is the front). The grip entry of sun_staff.json is kept: a staff is held upright.

  python build_sun_staff.py preview <png>   a contact sheet of the three tiers, front, three-quarter, side and back
  python build_sun_staff.py install         the sources into art/weapons/, then tools/build_skins.py
"""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import build_swords as W
from build_swords import Cube, by_face, flat, wrap, STEEL as S, GILT as G, LEATH as L, ramp, REPO
from build_knight import LINEN as C   # the cream of the rays

W.KEY.update({"wood": (0x8B, 0x45, 0x13), "bone": (0xEE, 0xEE, 0xDD)})

ASH = ramp("2b1f16", "4b392a", "6e5640", "8e7156", "ad9272", "cdb592")     # a warm ash, pale in the light
PALE = ramp("5f5040", "80705a", "a39278", "c2b497", "dccfb3", "efe6d0")    # the upper tiers' pale wood
CORE = G[6]                                                                  # the lit heart: a white-gold


# ---- painters ----

def px(fn, top=0):
    """A painter from fn(face, u, y, w): u runs along +x on the north and south flats (the north face is mirrored back), and
    y is the texel row's height in the model (`top` is the cube's top edge), so a pattern stays put as a post is cut."""
    def paint(face, w, h):
        return [[fn(face, (w - 1 - x) if face == "north" else x, top - 1 - y, w) for x in range(w)] for y in range(h)]
    return paint


def grain(R, base=3, salt=0):
    """A shaft's wood: the flats at `base`, the lit side a step up, the shadowed side a step down, a grain line and a pale
    streak every few texels."""
    def fn(face, u, y, w):
        if face == "up": return R[min(5, base + 2)]
        if face == "down": return R[max(0, base - 2)]
        i = base + (1 if face == "east" else -1 if face == "west" else 0)
        k = (y * 5 + u * 3 + salt + (2 if face in ("north", "west") else 0)) % 9
        if k == 0: i -= 1
        elif k == 4: i += 1
        return R[max(0, min(5, i))]
    return fn


def metal(R, base=3):
    """A turned piece: the top lit, the bottom shaded, the south-east side brighter than the north-west."""
    def fn(face, u, y, w):
        if face == "up": return R[min(6, base + 2)]
        if face == "down": return R[max(0, base - 2)]
        i = base + (1 if face in ("south", "east") else -1 if face == "west" else 0)
        if w > 1 and face in ("north", "south") and u == 0: i += 1
        return R[max(0, min(len(R) - 1, i))]
    return fn


def binder(R):
    """A strap wound round a shaft: a diagonal hatch of two tones."""
    def fn(face, u, y, w):
        if face == "up": return R[5]
        if face == "down": return R[1]
        if face in ("east", "west"): return R[3] if (y % 2) else R[2]
        return R[4] if (u + y) % 2 == 0 else R[3]
    return fn


def post(name, y0, y1, hw, hd, mat, fn, skip=()):
    return Cube(name, [-hw, y0, -hd], [hw, y1, hd], mat, px(fn, y1), skip=skip)


# ---- the pixel-art head, cut into cubes ----

# char -> (ramp, shade index, material, layer): the rim and field are `disc` (2 px deep), the rays are `ray` (1 px deep).
def legend(rays_ramp, rays_mat, tipshade, rayshade, **over):
    leg = {
        "D": (G, 5, "gilt", "disc"), "d": (G, 4, "gilt", "disc"), "e": (G, 3, "gilt", "disc"), "k": (G, 2, "gilt", "disc"),
        "m": (G, 3, "gilt", "disc"), "h": (G, 5, "gilt", "disc"),
        "r": (rays_ramp, rayshade, rays_mat, "ray"), "R": (rays_ramp, tipshade, rays_mat, "ray"),
        "s": (rays_ramp, rayshade - 1, rays_mat, "ray"),
        "c": (G, 6, "gilt", "core"),
    }
    leg.update(over)
    return leg


def plate(tag, grid, ytop, leg):
    """Cut a character grid (south view: left to right is +x) into cubes: the cells of one layer and material merge into
    rectangles, each painted cell by cell; `c` is the 1 px core, three px deep. Row 0's top edge is at ytop; the middle
    column is x = 0. Returns the cubes, and skips the faces two neighbours already hide."""
    H, Wd = len(grid), len(grid[0])
    depth = {"disc": 2, "ray": 1, "core": 3}
    cell = {}
    for r, row in enumerate(grid):
        for c, ch in enumerate(row):
            if ch != ".": cell[(r, c)] = ch
    def deep(rc): return depth[leg[cell[rc]][3]] if rc in cell else 0
    used, cubes = set(), []
    def key(rc): return (leg[cell[rc]][3], leg[cell[rc]][2])
    for r in range(H):
        for c in range(Wd):
            if (r, c) not in cell or (r, c) in used: continue
            k = key((r, c)); w = 1
            while (r, c + w) in cell and (r, c + w) not in used and key((r, c + w)) == k: w += 1
            h = 1
            while all((r + h, c + i) in cell and (r + h, c + i) not in used and key((r + h, c + i)) == k for i in range(w)): h += 1
            rect = [(r + j, c + i) for j in range(h) for i in range(w)]
            used.update(rect)
            layer, mat = k; d = depth[layer]
            skip = []
            if all(deep((r - 1, c + i)) >= d for i in range(w)): skip.append("up")
            if all(deep((r + h, c + i)) >= d for i in range(w)): skip.append("down")
            if all(deep((r + j, c - 1)) >= d for j in range(h)): skip.append("west")
            if all(deep((r + j, c + w)) >= d for j in range(h)): skip.append("east")
            rs = lambda ch: leg[ch][0]
            def paint(face, pw, ph, r=r, c=c, w=w, h=h):
                def at(j, i):
                    R, s = leg[cell[(r + j, c + i)]][:2]
                    return R[s]
                def edge(j, i, step):
                    R, s = leg[cell[(r + j, c + i)]][:2]
                    return R[max(0, min(len(R) - 1, s + step))]
                if face in ("south", "north"):
                    return [[at(j, (w - 1 - i) if face == "north" else i) for i in range(w)] for j in range(h)]
                if face == "up": return [[edge(0, i, 1) for i in range(w)] for _ in range(ph)]
                if face == "down": return [[edge(h - 1, i, -2) for i in range(w)] for _ in range(ph)]
                col = 0 if face == "west" else w - 1
                return [[edge(j, col, -1 if face == "west" else 0) for _ in range(pw)] for j in range(h)]
            x0 = c - Wd / 2; y1 = ytop - r
            cubes.append(Cube("%s_%d_%d" % (tag, r, c), [x0, y1 - h, -d / 2], [x0 + w, y1, d / 2], mat, paint, skip=skip,
                              lit=("north", "south") if layer == "core" else ()))
    return cubes


# ---- the larger heads, built from a few cubes ----
#
# Cut cell by cell (`plate`), a 5 x 5 or 7 x 7 sun costs 160 to 220 quads, over the 128 a weapon frame may draw. Tiers II
# and III build the head instead from a disc of two or three overlapping 2 px slabs (each shifted 0.05 px along z, so no two
# faces share a plane) painted from a character grid, one ray bar along x and one up, both running through the disc, two
# diagonal bars turned 45 degrees about the core (one 0.05 px behind the straight bars, one 0.05 px before; the x bar sits 0.05 px low, off the core's top), and the core. The bars' middles are hidden.

def sprite(grid, left, top, leg):
    """A painter maker for a disc slab: the south texel at model (x, y) takes the grid cell at column floor(x - left), row
    floor(top - y); the north face is mirrored back; the rim faces take the edge cells as `plate` paints them."""
    def make(lo, hi):
        w, h = int(round(hi[0] - lo[0])), int(round(hi[1] - lo[1]))
        r0, c0 = int(round(top - hi[1])), int(round(lo[0] - left))
        def cell(i, j, step=0):
            R, s = leg[grid[r0 + j][c0 + i]][:2]
            return R[max(0, min(len(R) - 1, s + step))]
        def paint(face, pw, ph):
            if face in ("south", "north"):
                return [[cell((w - 1 - i) if face == "north" else i, j) for i in range(w)] for j in range(h)]
            if face == "up": return [[cell(i, 0, 1) for i in range(w)] for _ in range(ph)]
            if face == "down": return [[cell(i, h - 1, -2) for i in range(w)] for _ in range(ph)]
            col = 0 if face == "west" else w - 1
            return [[cell(col, j, -1 if face == "west" else 0) for _ in range(pw)] for j in range(h)]
        return paint
    return make


def ray(R, body, tip, along="y"):
    """A ray bar: `body` along its length and `tip` on its outermost texel at each end; a bar along y has its east and west
    sides a step down and its ends at the tip shade; a bar along x is lit on top and shaded under."""
    def paint(face, w, h):
        if along == "x":
            if face in ("east", "west"): return [[R[tip]] * w for _ in range(h)]
            sh = 1 if face == "up" else -2 if face == "down" else 0
            return [[R[max(0, min(len(R) - 1, (tip if x in (0, w - 1) else body) + sh))] for x in range(w)] for _ in range(h)]
        if face in ("up", "down"): return [[R[tip if face == "up" else max(0, tip - 2)]] * w for _ in range(h)]
        step = -1 if face in ("east", "west") else 0
        return [[R[max(0, (tip if y in (0, h - 1) else body) + step)]] * w for y in range(h)]
    return paint


def head(R, mat, body, tip, cy, cross, up, diag, discs, grid, left, top, leg):
    """The cubes of a sun head centred at (0, cy): the disc slabs [(half-width, half-height)], a ray bar along x `cross` px
    long, one from the core up to cy + up, two diagonals `diag` px long turned 45 degrees, and the core."""
    make = sprite(grid, left, top, leg)
    out = []
    for i, (hw, hh) in enumerate(discs):
        dz = (0, -0.05, 0.05)[i]   # every slab 2 px deep (no stretched face), shifted so no two faces share a plane
        lo, hi = [-hw, cy - hh, -1 + dz], [hw, cy + hh, 1 + dz]
        out.append(Cube("disc_%d" % i, lo, hi, "gilt", make(lo, hi)))
    out.append(Cube("ray_x", [-cross / 2, cy - 0.55, -0.5], [cross / 2, cy + 0.45, 0.5], mat, ray(R, body, tip, "x")))
    out.append(Cube("ray_up", [-0.5, cy + 0.5, -0.5], [0.5, cy + up, 0.5], mat, ray(R, body, tip), skip=("down",)))
    for tag, t, dz in (("l", 45, -0.05), ("r", -45, 0.05)):   # one a hair behind the straight bars, one a hair before
        out.append(Cube("ray_" + tag, [-0.5, cy - diag / 2, -0.5 + dz], [0.5, cy + diag / 2, 0.5 + dz], mat, ray(R, body, tip),
                        turn=t, pivot=[0, cy, 0]))
    CR, cs = leg["c"][:2]
    out.append(Cube("core", [-0.5, cy - 0.5, -1.5], [0.5, cy + 0.5, 1.5], "gilt",
                    by_face(up=flat(CR[min(len(CR) - 1, cs)]), down=flat(CR[cs - 2]), rest=flat(CR[cs])), lit=("north", "south")))
    return out


# ---- the three staves ----

# Each grid is the head seen from the south, row 0 on top. D, d, e are the rim (lit, side, shaded), m and k the field, c the
# core, r and R the ray body and tip.
HEAD_I = [
    "...R...",
    "...r...",
    "..DDD..",
    "RrdcdrR",
    "..eee..",
]

# Tiers II and III paint only their discs from a grid (cells outside every slab are never read); their rays are bars.
DISC_II = [            # x -2.5..2.5, y 21..16: slabs of 3 x 5 and 5 x 3, a rounded sun
    ".DDD.",
    "dhhhd",
    "dmcmd",
    "dmmmd",
    ".eee.",
]

DISC_III = [           # x -3.5..3.5, y 21..14: slabs of 3 x 7, 5 x 5 and 7 x 3, a round sun
    "..DDD..",
    ".DmmmD.",
    "dmkkkmd",
    "dmkckmd",
    "dmkkkmd",
    ".emmme.",
    "..eee..",
]


def tier1():
    """The Sunweaver's ash staff: an iron foot and spike, an ash shaft with a grain line, a leather grip between gilt
    rings, a gilt neck and a small gilt sun on four cream rays."""
    return [
        post("spike", -11, -9, 0.5, 0.5, "iron", metal(S, 3)),
        post("shoe", -9, -6, 1, 1, "iron", metal(S, 2)),
        post("shaft_lo", -6, -3, 0.5, 0.5, "wood", grain(ASH, 3, 1), skip=("down",)),
        post("ring_lo", -3, -2, 1, 1, "gilt", metal(G, 3), skip=("up",)),
        Cube("grip", [-1, -2, -1], [1, 2, 1], "leather", wrap(L[3], L[2]), skip=("up", "down")),
        post("ring_hi", 2, 3, 1, 1, "gilt", metal(G, 3), skip=("down",)),
        post("shaft_a", 3, 8, 0.5, 0.5, "wood", grain(ASH, 3, 2), skip=("down",)),
        post("ring_mid", 8, 9, 1, 1, "gilt", metal(G, 3), skip=("down",)),
        post("shaft_b", 9, 14, 0.5, 0.5, "wood", grain(ASH, 3, 5), skip=("down",)),
        post("neck", 14, 16, 1, 1, "gilt", metal(G, 3), skip=("down", "up")),
    ] + plate("head", HEAD_I, 21, legend(C, "bone", 5, 4))


def tier2():
    """The Sunweaver's pale staff, bound in steel: a steel foot and spike, one pale shaft run through steel rings about the
    grip and two hatched binders, a steel collar and cup and a 5 x 5 gilt sun on eight cream rays."""
    return [
        post("spike", -12, -10, 0.5, 0.5, "steel", metal(S, 4), skip=("up",)),
        post("shoe", -10, -7, 1, 1, "steel", metal(S, 3), skip=("up",)),
        post("shoe_ring", -7, -6, 1.5, 1.5, "steel", metal(S, 4)),
        post("shaft", -6, 15, 0.5, 0.5, "wood", grain(PALE, 3, 3), skip=("down", "up")),
        post("ring_lo", -3, -2, 1, 1, "steel", metal(S, 4), skip=("up",)),
        Cube("grip", [-1, -2, -1], [1, 2, 1], "leather", wrap(L[3], L[2]), skip=("up", "down")),
        post("ring_hi", 2, 3, 1, 1, "steel", metal(S, 4), skip=("down",)),
        post("binder_a", 5, 7, 1, 1, "steel", binder(S)),
        post("binder_b", 11, 13, 1, 1, "steel", binder(S)),
        post("collar", 14, 15, 1.5, 1.5, "steel", metal(S, 4)),
        post("cup", 15, 16, 2.5, 1.5, "steel", metal(S, 5)),
    ] + head(C, "bone", 4, 5, 18.5, 11, 5.5, 11, [(1.5, 2.5), (2.5, 1.5)], DISC_II, -2.5, 21, legend(C, "bone", 5, 4))


STAIN = ramp("1f150f", "2b1f16", "4b392a", "6e5640", "8e7156", "ad9272")   # the ash a step darker, for gilt to sit on


def barface(R):
    def paint(face, w, h):
        return [[R[5] if (y == 0 and h > 1) else R[2] if (y == h - 1 and h > 1) else R[4] if face == "south" else R[3]
                 for x in range(w)] for y in range(h)]
    return paint


def tier3():
    """The Sunweaver's gilded staff: a steel spike in a gilt shoe, a stained shaft in gilt fittings, a gilt-wired grip, a
    gilt band, a cross-bar finial hung with two gilt chains ending in cream beads, a gilt collar and cup and a 7 x 7 gilt
    sun inside a crown of long gilt rays."""
    def chain(face, w, h):   # two gilt links over a cream bead, top down
        rows = [G[5], G[3], G[4], G[2], C[4], C[3]]
        if face == "down": return [[C[2]] * w for _ in range(h)]
        return [[rows[min(y, 5)] if face != "west" else (rows[min(y, 5)] if y >= 4 else G[2 + (y % 2)])] * w for y in range(h)]
    cubes = [
        post("spike", -10, -8, 0.5, 0.5, "steel", metal(S, 4), skip=("up",)),
        post("shoe", -8, -5, 1, 1, "gilt", metal(G, 3), skip=("up",)),
        post("shoe_ring", -5, -4, 1.5, 1.5, "gilt", metal(G, 4)),
        post("shaft", -4, 13, 0.5, 0.5, "wood", grain(STAIN, 2, 3), skip=("down", "up")),
        post("ring_lo", -3, -2, 1, 1, "gilt", metal(G, 4), skip=("up",)),
        Cube("grip", [-1, -2, -1], [1, 2, 1], "leather", wrap(L[3], L[2], G[5]), skip=("up", "down")),
        post("ring_hi", 2, 3, 1, 1, "gilt", metal(G, 4), skip=("down",)),
        post("band", 6, 7, 1, 1, "gilt", metal(G, 4)),
        Cube("crossbar", [-3.5, 10, -1], [3.5, 11, 1], "gilt", by_face(up=flat(G[5]), down=flat(G[1]), rest=barface(G))),
        post("collar", 12, 13, 1.5, 1.5, "gilt", metal(G, 4)),
        post("cup", 13, 14, 2.5, 1.5, "gilt", metal(G, 5)),
    ]
    for tag, x0 in (("l", -3.5), ("r", 2.5)):   # chains hang from the cross-bar's ends
        cubes.append(Cube("chain_" + tag, [x0, 4, -0.5], [x0 + 1, 10, 0.5], "gilt", chain, skip=("up",)))
    return cubes + head(G, "gilt", 5, 6, 17.5, 13, 6.5, 13, [(1.5, 3.5), (2.5, 2.5), (3.5, 1.5)], DISC_III, -3.5, 21,
                        legend(G, "gilt", 6, 5, m=(G, 4, "gilt", "disc"), k=(G, 3, "gilt", "disc")))


TIERS = {
    "sun_staff": (tier1, "The Sunweaver's staff, tier I (plain), hand-authored at 1 texel a px: a warm ash shaft with a grain line "
                  "on an iron foot and spike, a leather grip between gilt rings, and a small gilt sun 3 px across on four cream "
                  "rays; the sun's heart is one 1 px cube, lit on its north and south faces (2 glow texels); 32 px long."),
    "sun_staff_t2": (tier2, "The Sunweaver's staff, tier II (upgraded), hand-authored at 1 texel a px: a pale wood shaft bound in "
                     "steel (two hatched binders and rings about the grip) on a steel foot and spike, a steel collar and cup, and a "
                     "5 px gilt sun on eight cream rays; the heart is one 1 px cube, lit on its north and south faces "
                     "(2 glow texels); 36 px long."),
    "sun_staff_t3": (tier3, "The Sunweaver's staff, tier III (elite), hand-authored at 1 texel a px: a dark stained shaft in gilt "
                     "fittings on a steel spike in a gilt shoe, a gilt-wired grip, a gilt band, a cross-bar finial hung with two gilt "
                     "chains ending in cream beads, a gilt collar and cup, and a 7 px gilt sun on eight long gilt rays; the heart is "
                     "one 1 px cube, lit on its north and south faces (2 glow texels); 34 px long."),
}


# ---- files ----

W_outputs = W.outputs


def outputs(tiers=None):
    """W.outputs for the three staves, with the thickness and the upright grip of the current sun_staff.json."""
    out = W_outputs(tiers or TIERS)
    with open(os.path.join(REPO, "art", "weapons", "sun_staff.json"), encoding="utf-8") as f: cur = json.load(f)
    for wid, (_, note) in (tiers or TIERS).items():
        out[wid + ".json"] = {"weapon": wid, "density": 1, "thickness": cur["thickness"], "grip": cur["grip"], "note": note}
    return out


def main():
    W.outputs = outputs   # install writes these files, not the swords'
    W.main(TIERS, __doc__)


if __name__ == "__main__":
    main()
