"""RETIRED (Kort, 2026-10-18): every scene image in the storybook is in-game footage (stills/, see NOTES.md),
and no page uses what this file draws; its old outputs are in art/retired/. Kept for the record only.

The storybook paintings of the game world, drawn the way the game draws it: whole blocks with hand-typed 16 x 16
textures, entity models as boxes with hand-typed faces, one ray per GUI pixel, light worked out per texel (the
game's sky light with smooth lighting and corner shading, a moon or sun that casts shadows, block light from fire
and lanterns), then the book's own treatment: fog in a few hard steps, a sky painted in banded steps with square
stars and the game's square moon. Kort's rule from the title round: if it shows the world, it is blocky.

This file is the engine and the shared blocks; the scenes live in sb_scenes.py. It extends title/blocky.py (the
title's renderer) with the blocks the storybook needs: stone bricks, mossy stone bricks and cobblestone, still
water, oak leaves, a campfire flame, poppies, tall grass, and a few tones."""
import math
import numpy as np
from PIL import Image
from sb_common import *          # paths
import blocky as B

# ================================================================================================ tones
B.ALBEDO.update({
    # moss on stone (the barrow kerbs)
    "m0": "#2f3d1f", "m1": "#3f5228", "m2": "#516632", "m3": "#647a3d",
    # still water: the river, dark and a little green, with lighter ripples
    "o0": "#1f3442", "o1": "#2a4555", "o2": "#365869", "o3": "#4d7383",
    # oak leaves (fast graphics: opaque), deep greens
    "x0": "#1d3313", "x1": "#28451a", "x2": "#345722", "x3": "#436a2b",
    # poppy red, and a stem green
    "R0": "#6b1a12", "R1": "#9a2a1c", "R2": "#c43c28",
    # spruce bark and planks, darker than oak
    "S0": "#2a1d12", "S1": "#3b2a1a", "S2": "#4d3822", "S3": "#5e462b",
    # the Tide (unit identity: verdigris #4E7A6A, barnacle #CFCBBE, navy #1E2C44, sea-foam eyes #CFF3E4), the
    # turtle-shell helm, kelp
    "z0": "#2a453d", "z1": "#3a5f53", "z2": "#4e7a6a", "z3": "#6a9686",
    "Z0": "#141d2e", "Z1": "#1e2c44", "Z2": "#2c3d5a",
    "F0": "#cff3e4", "F1": "#9fd8c4",
    "U0": "#2f4a2a", "U1": "#46683a", "U2": "#5f8a4a",
    "X0": "#2c3a1a", "X1": "#3b4a22", "X2": "#556b2e",
    "V0": "#2f5a50", "V1": "#43786a", "V2": "#5e9a88",
    # bone and horn
    "H0": "#8a7f63", "H1": "#b3a684", "H2": "#d6cba8", "H3": "#efe6c8",
    # dark cloth, near black
    "K0": "#16130f", "K1": "#221d17", "K2": "#2f281f",
    # enamel and gilt (the Order), a red wax, linen
    "E0": "#8f897a", "E1": "#b5ae9c", "E2": "#d9d3c1", "E3": "#eeeadf",
    "G0": "#6e5527", "G1": "#96783a", "G2": "#c09a52",
    "W0": "#5a1610", "W1": "#8a2b1f", "W2": "#a83c2a",
    "L0": "#867d66", "L1": "#a39a80", "L2": "#bfb59a", "L3": "#d6cdb3",
    # skin (Maud), hair
    "h4": "#e3b591", "h5": "#c99671", "h6": "#a8775a", "h7": "#7d5340",
    "y0": "#2a2420", "y1": "#3d342c", "y2": "#6e675e", "y3": "#9a948a",
    # steel
    "T0": "#464c55", "T1": "#626a75", "T2": "#7c8591", "T3": "#a3abb5",
    # blue cloth (a Hollow cloak), a teal for the Dawn
    "C0": "#1d2633", "C1": "#2a3747", "C2": "#3a4b60",
})


# ================================================================================================ textures
def digits(prefix, n=6):
    return {str(i): prefix + str(i) for i in range(n)}


BRICK_MAP = [
    "AAAAAAA.BBBBBBB.",
    "AAAAAAA.BBBBBBB.",
    "AAAAAAA.BBBBBBB.",
    "................",
    "CCC.DDDDDDD.EEEE",
    "CCC.DDDDDDD.EEEE",
    "CCC.DDDDDDD.EEEE",
    "................",
    "FFFFFFF.GGGGGGG.",
    "FFFFFFF.GGGGGGG.",
    "FFFFFFF.GGGGGGG.",
    "................",
    "HHH.IIIIIII.JJJJ",
    "HHH.IIIIIII.JJJJ",
    "HHH.IIIIIII.JJJJ",
    "................",
]
BRICK_TONE = dict(A=3, B=2, C=2, D=3, E=3, F=2, G=3, H=3, I=2, J=2)
_bricks = B.stones(BRICK_MAP, BRICK_TONE, {".": "0", ",": "1"})
# a hairline crack and a few chips, typed in by hand
_crack = {(10, 1): "1", (11, 2): "1", (11, 0): "1", (4, 9): "1", (5, 10): "1", (13, 13): "1", (2, 5): "4"}
_bricks = ["".join(_crack.get((x, y), _bricks[y][x]) for x in range(16)) for y in range(16)]
B.tex("stone_bricks", digits("s"), _bricks)
# moss creeping over the top course and down the joints
_moss_at = {(x, y) for y in range(16) for x in range(16)
            if (y <= 1 and (x * 7 + 3) % 5 != 0) or (y in (2, 3) and x in (0, 1, 2, 5, 6, 9, 13, 14))
            or (y in (7, 11) and x in (1, 2, 3, 8, 9, 12)) or (y in (4, 8) and x in (3, 7, 11, 15))}
_mossk = dict(digits("s"), a="m0", b="m1", c="m2", e="m3")
B.tex("mossy_stone_bricks", _mossk,
      ["".join(("e" if (x + y) % 3 == 0 else "c" if (x * 3 + y) % 4 else "b") if (x, y) in _moss_at else _bricks[y][x]
               for x in range(16)) for y in range(16)])
_cob = B.TEX["cobblestone"][0]
_cmoss = {(x, y) for y in range(16) for x in range(16) if (x * 5 + y * 3) % 7 in (0, 3) and y < 9}
B.tex("mossy_cobblestone", dict(digits("c"), a="m0", b="m1", c="m2", e="m3"),
      ["".join(("c" if (x + y) % 2 else "b") if ((x, y) in _cmoss and _cob[y][x] in "234") else _cob[y][x]
               for x in range(16)) for y in range(16)])
B.tex("water", digits("o", 4), [
    "1111111111111111",
    "1112221111111111",
    "1111111111122211",
    "1111111111111111",
    "0011111111111100",
    "1111111222111111",
    "1111111111111111",
    "1111111111111111",
    "1122211111111111",
    "1111111111113311",
    "1111111111111111",
    "0001111111111000",
    "1111111111111111",
    "1111112221111111",
    "1111111111111111",
    "1111111111111111",
])
B.tex("oak_leaves", digits("x", 4), [
    "2132213021302213",
    "1323321312231321",
    "2201132213210231",
    "3122310232113122",
    "1213221123230213",
    "2310132211321302",
    "1232213032102231",
    "0221321123213123",
    "2132302231121302",
    "1322113202312213",
    "2013221321302131",
    "3221310232213220",
    "1302132113021312",
    "2213220132132201",
    "1321031223211322",
    "2132213102132213",
])
B.tex("fire", {"0": "f0", "1": "f1", "2": "f2", "3": "f3"}, [
    "................",
    "......0.........",
    "......0....0....",
    ".....01....0....",
    ".....01...01....",
    "..0..011..01....",
    "..0.0121.011.0..",
    "..010121.0121.0.",
    ".01.01221012100.",
    ".012012210122101",
    ".01221232012321.",
    "0122123321223210",
    "0122233332232210",
    "1223333333333221",
    "1233333333333321",
    "................",
])
B.tex("poppy", dict(R="R1", r="R0", Q="R2", a="g0", b="g1", c="g2"), [
    "................",
    "................",
    "................",
    "................",
    "......QR........",
    ".....QRRr.......",
    ".....RRrr.......",
    "......rr........",
    ".......b........",
    ".......b..a.....",
    "......cb.a......",
    ".......ba.......",
    ".......b........",
    "......ab........",
    ".......b........",
    "................",
])
B.tex("tall_grass", dict(a="g0", b="g1", c="g2", e="g3", f="g4"), [
    "................",
    "........b.......",
    "...a....b...c...",
    "...a...bc...c...",
    "...ab..bc..cb...",
    "....b..bc..c....",
    "..a.b..c.b.c..b.",
    "..a.bb.c.bcb..b.",
    "...abb.c.bc..bc.",
    "...ab.bc.bc..b..",
    ".b.ab.bcebc.bc..",
    ".ba.bbbc.bcbbc..",
    "..bab.bce.cbc...",
    "..babbbcebcbc...",
    "...bbbbceebbc...",
    "...abbbcebbb....",
])

B.block("stone_bricks", "stone_bricks")
B.block("mossy_stone_bricks", "mossy_stone_bricks")
B.block("mossy_cobblestone", "mossy_cobblestone")
B.block("water", "water", model=[(0, 0, 0, 16, 14, 16)])
B.block("oak_leaves", "oak_leaves")
B.block("fire", "fire", model="cross", glow=1.0)
B.block("poppy", "poppy", model="cross")
B.block("tall_grass", "tall_grass", model="cross")
B.block("stone_slab", "stone", model=[(0, 0, 0, 16, 8, 16)])
B.block("brick_slab", "stone_bricks", model=[(0, 0, 0, 16, 8, 16)])
B.block("glow", "lantern", model=[(5, 0, 5, 11, 7, 11), (6, 7, 6, 10, 9, 10)], glow=1.0)
B.block("light", "stone", model=[], glow=1.0)          # an unseen light: a torch held in a hand


def refresh():
    B.CROSS_IDS = [i for i, b in enumerate(B.BLOCKS) if b and b["model"] == "cross"]
    B.OPAQUE_TEXEL = {name: np.array([[ch != "." for ch in r] for r in rows]) for name, (rows, key) in B.TEX.items()}
    B.MODEL_IDS = [i for i, b in enumerate(B.BLOCKS) if b and b["model"] is not None and b["model"] != "cross"]
    B.FULL = np.array([bool(b) and b["model"] is None for b in B.BLOCKS])


refresh()


# ================================================================================================ sampling
def sample(world, P, nrm, bid, cell):
    """the texture tone at each block hit, and the texel centre its light is read at (pixel-locked light)"""
    n = len(P)
    r = np.arange(n)
    f = np.clip(P - cell, 0, 1 - 1e-9)
    ax = np.argmax(np.abs(nrm), 1)
    s = np.sign(nrm[r, ax]).astype(int)
    fx, fy, fz = f[:, 0], f[:, 1], f[:, 2]
    col = np.where(ax == 1, fx, np.where(ax == 2, np.where(s < 0, fx, 1 - fx), np.where(s > 0, fz, 1 - fz)))
    row = np.where(ax == 1, np.where(s > 0, 1 - fz, fz), 1 - fy)
    cross = np.isin(bid, B.CROSS_IDS)
    col = np.where(cross, np.where(np.abs(nrm[:, 0]) > 0, fz, fx), col)
    row = np.where(cross, 1 - fy, row)
    col = np.clip((col * 16).astype(int), 0, 15)
    row = np.clip((row * 16).astype(int), 0, 15)
    snap = (np.floor(f * 16) + 0.5) / 16
    snap[r, ax] = f[r, ax]
    Pc = cell + np.where(cross[:, None], f, snap)
    lax = world.axis[tuple((cell - world.lo).T)]
    tone = np.empty(n, dtype=object)
    for k in range(n):
        b = B.BLOCKS[bid[k]]
        a = ax[k]
        la = int(lax[k])
        log_axis = (1, 0, 2)[la]
        if b["model"] == "cross":
            name, cc, rr = b["top"], col[k], row[k]
        elif b["name"] in ("oak_log", "stripped_oak"):
            name = b["top"] if a == log_axis else b["side"]
            col_dir = 2 if a == 0 else 0
            cc, rr = (row[k], col[k]) if (col_dir == log_axis and a != log_axis) else (col[k], row[k])
        else:
            name = b["top"] if (a == 1 and s[k] > 0) else b["bottom"] if a == 1 else b["side"]
            cc, rr = col[k], row[k]
        rows, key = B.TEX[name]
        tone[k] = key[rows[rr][cc]]
    return tone, Pc


# ================================================================================================ the scene
class Figure:
    """an entity: a list of blocky.Box; drawn only inside its own screen box (fast), depth-tested against the
    world. light: 'world' reads the sky light of its cell like the game, 'flat' gives it full ambient"""
    def __init__(self, boxes, cast=True):
        self.boxes = boxes
        self.cast = cast

    def corners(self):
        out = []
        for b in self.boxes:
            for sx in (-1, 1):
                for sy in (-1, 1):
                    for sz in (-1, 1):
                        out.append(b.c + (np.array([sx, sy, sz]) * b.h) @ b.A)
        return np.array(out)


def render(world, cam, W, H, y0=0, figures=(), L=(0.4, 0.6, 0.7), shadow=True):
    """trace the world and the figures; returns a dict of per-pixel arrays (H, W)"""
    L = np.asarray(L, float); L = L / np.linalg.norm(L)
    D = cam.rays(0, y0, W, H)
    n = len(D)
    eye = cam.eye
    t, nrm, bid, cell = B.trace(world, eye, D)
    tone = np.full(n, "", dtype=object)
    Pc = np.zeros((n, 3))
    hit = np.isfinite(t)
    if hit.any():
        P = eye + D[hit] * t[hit, None]
        tn, pc = sample(world, P, nrm[hit], bid[hit], cell[hit])
        tone[hit] = tn
        Pc[hit] = pc
    full = np.zeros(n, bool)
    full[hit] = B.FULL[bid[hit]]
    ent = np.zeros(n, bool)
    # figures, each inside its own screen box
    for fig in figures:
        cs = fig.corners()
        dz = cs[:, 2] - eye[2]
        if (dz <= 0.05).any():
            continue
        sx = cam.xv + cam.f * (cs[:, 0] - eye[0]) / dz
        sy = cam.yv - cam.f * (cs[:, 1] - eye[1]) / dz - y0
        x0, x1 = int(max(0, math.floor(sx.min()) - 1)), int(min(W, math.ceil(sx.max()) + 2))
        ya, yb = int(max(0, math.floor(sy.min()) - 1)), int(min(H, math.ceil(sy.max()) + 2))
        if x0 >= x1 or ya >= yb:
            continue
        idx = (np.arange(ya, yb)[:, None] * W + np.arange(x0, x1)[None, :]).ravel()
        te, ne, be, toe, _ = B.boxes_hit(eye, D[idx], fig.boxes)
        closer = (be >= 0) & (te < t[idx])
        if not closer.any():
            continue
        j = idx[closer]
        t[j] = te[closer]
        nrm[j] = ne[closer]
        tone[j] = toe[closer]
        Pc[j] = eye + D[j] * t[j, None]
        full[j] = False
        ent[j] = True
        hit[j] = True
        cell[j] = np.floor(Pc[j]).astype(np.int64)
    # light
    sky = B.sky_light(world)
    blk = B.block_light(world)
    lv = np.zeros(n)
    ao = np.ones(n)
    bl = np.zeros(n)
    direct = np.zeros(n)
    h = np.nonzero(hit)[0]
    r = np.arange(h.size)
    nh = nrm[h]
    ax = np.argmax(np.abs(nh), 1)
    lv[h] = B.cell_sky(world, sky, Pc[h] + nh * 0.02)
    bl[h] = B.cell_sky(world, blk, Pc[h] + nh * 0.02, outside=0)
    f = full[h]
    if f.any():
        hf = h[f]
        fs = Pc[hf] - cell[hf]
        a2 = ax[f]
        rr = np.arange(hf.size)
        ua = np.where(a2 == 0, 2, 0)
        va = np.where(a2 == 1, 2, 1)
        fu, fv = fs[rr, ua], fs[rr, va]
        ao[hf] = B.corner_ao(world, cell[hf], nrm[hf], fu, fv)
        lv[hf] = B.smooth_sky(world, sky, cell[hf], nrm[hf], fu, fv)
        bl[hf] = B.smooth_sky(world, blk, cell[hf], nrm[hf], fu, fv, outside=0)
    ndl = np.clip(nh @ L, 0, None)
    up = ndl > 0
    lit = np.zeros(h.size, bool)
    if shadow and up.any():
        O = Pc[h][up] + nh[up] * 2e-3 + L * 2e-3
        Dd = np.broadcast_to(L, (int(up.sum()), 3)).copy()
        blocked = B.trace(world, O, Dd, shadow=True)
        casters = [b for fg in figures if fg.cast for b in fg.boxes]
        if casters:
            blocked |= B.boxes_hit(O, Dd, casters, want_tone=False)[2] >= 0
        lit[up] = ~blocked
    elif up.any():
        lit[up] = True
    direct[h] = ndl * lit
    out = dict(tone=tone.reshape(H, W), t=t.reshape(H, W), nrm=nrm.reshape(H, W, 3), P=Pc.reshape(H, W, 3),
               sky=lv.reshape(H, W), ao=ao.reshape(H, W), blk=bl.reshape(H, W), direct=direct.reshape(H, W),
               hit=hit.reshape(H, W), ent=ent.reshape(H, W), D=D.reshape(H, W, 3), eye=eye)
    return out


# ================================================================================================ colour
def lin(hexc):
    c = np.array(B.hexrgb(hexc), float) / 255.0
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def srgb(c):
    c = np.clip(np.asarray(c, float), 0, 1)
    return np.round(255 * np.where(c <= 0.0031308, c * 12.92, 1.055 * c ** (1 / 2.4) - 0.055)).astype(int)


BAYER4 = (np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) + 0.5) / 16.0
EMIT = ("f0", "f1", "f2", "f3", "F0", "F1")


def bayer(H, W, x0=0, y0=0):
    return BAYER4[(np.arange(H)[:, None] + y0) % 4, (np.arange(W)[None, :] + x0) % 4]


def checker(H, W):
    """the plainest dither: a half-tone checker (thresholds 0.25 and 0.75), so a dithered edge between two steps
    is one clean checkerboard band and never a scatter of dots"""
    return np.where((np.arange(H)[:, None] + np.arange(W)[None, :]) % 2 == 0, 0.25, 0.75)


def quant(v, q, dither=None, band=0.34):
    """v (0..1) to q hard steps; with a Bayer threshold array the steps break up in an ordered dither only in a
    narrow band where two steps meet (band: its width as a share of a step), so every step stays flat and only
    its edge dithers, the one place a falloff may"""
    x = np.clip(v, 0, 1) * q
    if dither is None:
        return np.clip(np.round(x), 0, q) / q
    lo = np.floor(x)
    f = x - lo
    up = f > 0.5 + (dither - 0.5) * band
    return np.clip(lo + up, 0, q) / q


def shade(R, amb_col, sun_col, warm_col, fog, fog_col, amb_q=10, sun_q=5, warm_q=6, fog_q=8, emit=EMIT,
          face=None, warm_curve=2.0, ent_amb=1.0, dither=True, fog_band=0.34, warm_band=None):
    """turn the traced arrays into linear colours in hard steps: ambient (sky light x corner shading x the game's
    fixed shade per face direction), direct moon or sun light, warm block light, then fog (an (H, W) amount and
    a colour, (3,) or (H, W, 3)), the fog stepped with an ordered dither. Returns (H, W, 3) linear and the hit
    mask"""
    face = face or {(1, 1): 1.0, (1, -1): 0.5, (0, 1): 0.6, (0, -1): 0.6, (2, 1): 0.8, (2, -1): 0.8}
    tone, hit = R["tone"], R["hit"]
    H, W = tone.shape
    nrm = R["nrm"]
    ax = np.argmax(np.abs(nrm), 2)
    sg = np.sign(np.take_along_axis(nrm, ax[..., None], 2)[..., 0]).astype(int)
    fshade = np.ones((H, W))
    for (a, s_), v in face.items():
        fshade[(ax == a) & (sg == s_)] = v
    amb = B.brightness(R["sky"]) * (0.45 + 0.55 * R["ao"]) * fshade
    amb = np.where(R["ent"], amb * ent_amb, amb)
    aq = quant(amb, amb_q)
    sq = quant(R["direct"], sun_q)
    wv = B.brightness(R["blk"]) ** warm_curve
    if warm_band:                                   # step edges on the world break into a checker; figures whole
        wq = np.where(R["ent"], quant(wv, warm_q), quant(wv, warm_q, checker(H, W), warm_band))
    else:
        wq = quant(wv, warm_q)
    lc = np.zeros((H, W, 3))
    cache = {}
    A, S, Wc = np.asarray(amb_col), np.asarray(sun_col), np.asarray(warm_col)
    ys, xs = np.nonzero(hit)
    for y, x in zip(ys.tolist(), xs.tolist()):
        tn = tone[y, x]
        key = (tn, aq[y, x], sq[y, x], wq[y, x])
        c = cache.get(key)
        if c is None:
            alb = lin(B.ALBEDO[tn])
            c = alb if tn in emit else alb * (A * key[1] + S * key[2] + Wc * key[3])
            cache[key] = c
        lc[y, x] = c
    if fog_q:
        fq = quant(np.clip(fog, 0, 1), fog_q, checker(H, W) if dither else None, fog_band)
    else:
        fq = np.clip(fog, 0, 1)                    # already stepped by the scene
    F = np.broadcast_to(np.asarray(fog_col, float), (H, W, 3))
    out = lc * (1 - fq[..., None]) + F * fq[..., None]
    return out, hit, len(cache)


def to_rgb8(lin_img):
    return srgb(lin_img).astype(np.uint8)


def save_rgba(arr8, mask, name):
    H, W = mask.shape
    im = np.zeros((H, W, 4), np.uint8)
    im[..., :3] = arr8
    im[..., 3] = np.where(mask, 255, 0)
    Image.fromarray(im, "RGBA").save(os.path.join(ART, name + ".png"))
    print("  art/" + name + ".png", W, "x", H)


# ================================================================================================ figures
def grid(rows, key):
    return B.grid(rows, key)


def solid(t, w=1, h=1):
    return B.solid(t, w, h)


def part(lo, hi, faces, R=None, pivot=None, px=1 / 16.0, at=(0, 0, 0)):
    """a model part in skin pixels: lo/hi corners relative to the feet, turned by R about pivot (skin px)"""
    lo, hi = np.array(lo, float), np.array(hi, float)
    c = (lo + hi) / 2.0
    half = (hi - lo) / 2.0 * px
    A = np.eye(3) if R is None else np.asarray(R, float)
    if pivot is not None:
        pv = np.array(pivot, float)
        c = pv + (c - pv) @ A
    return B.Box(np.array(at, float) + c * px, half, faces, A)
