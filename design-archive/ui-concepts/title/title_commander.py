"""The commander on the hill: Steve, as the game's player model, in the Dawn commander's armour, holding a diamond
sword up in his right hand, his left hand on the pole of the Dawn banner planted beside him. Four frames of wind.

He is built the way the game builds a player: boxes in skin pixels (head 8 x 8 x 8, arms and legs 4 x 12 x 4, the
body 8 x 12 x 4 under the chestplate), the armour a second layer of boxes one pixel larger all round (the helmet,
the chestplate with its pauldrons, the boots), the sword an item sprite in his fist, the banner a pole, a crossbar
and a cloth. Every face is a hand-typed grid of tones: our own drawings in the game's manner (Steve's face too),
not the game's files. blocky.py draws them: a parallel view along the line the hill camera sees him on, so a skin
pixel is exactly 4 x 4 GUI px (an item pixel 3 x 3, as items are drawn smaller in the hand). His light is the
hill's: the low sun behind him, so his front is in shade, but as the game lights entities from fixed directions
his face stays readable; tops and right-hand faces catch the sun; the cloth is lit through. Day and night are the
hill's two palettes over one set of codes.

Only the cloth moves between frames: a fold runs down it from the crossbar to the tails, and the tails, last to
move, swing a frame behind. The body, the sword, the pole and the crossbar are the same in all four frames.

python title_commander.py   -> sprites/commander-{day,night}-{0..3}.png (192 x 300, placed at GUI 588, 110),
                               sprites/commander-at.json (where the sprite sits)"""
import json, os
import numpy as np

import blocky as B
from blocky import Box, grid, rot
from pixel import save, OUT

AT = (588, 110)                    # where the sprite sits on the 960 x 540 GUI canvas
SW, SH = 192, 300
FEET = np.array([-5.2, 0.0, 25.0])  # the middle of his soles (world blocks), as in title_hill
PX = 1 / 16                        # a skin pixel in blocks: 4 GUI px at his distance
EYE = np.array([0.0, 3.0, 0.0])    # the hill camera: eye, focal length, vanishing point
FOCAL, XV, YV = 1600, 1000, 208
VIEW = FEET + np.array([0.0, 1.1, 0.0]) - EYE
VIEW = VIEW / VIEW[2]              # the line the camera sees him along, per block of depth
SUN = np.array([0.55, 0.32, 0.77])
SUN = SUN / np.linalg.norm(SUN)


def part(lo, hi, faces, R=None, pivot=None):
    """a box given in skin pixels from his feet (x to his left, y up, z to his back), turned by R about pivot"""
    lo, hi = np.array(lo, float), np.array(hi, float)
    c, h = (lo + hi) / 2, (hi - lo) / 2
    A = np.eye(3) if R is None else np.asarray(R, float)
    if R is not None:
        pv = np.asarray(pivot, float)
        c = pv + (c - pv) @ A
    return Box(FEET + c * PX, h * PX, faces, A)


# ================================================================================================ faces
# Each grid is typed as seen from outside, top row first; the side grids start at the front edge.
SKIN = {"s": "h2", "S": "h3", "d": "h1", "D": "h0", "H": "b0", "h": "b1", "W": "n3", "P": "u1", "p": "u0",
        "m": "b1", "M": "b0"}
FACE = grid([          # Steve's face, our own drawing: hair, brow, the eyes white outside and violet in, the
    "HHHHHHHH",        # nose a shade darker than the skin, the closed mouth darker again
    "HHhHHhHH",
    "HsSssSsH",
    "sssssSss",
    "sWPssPWs",
    "sssddsss",
    "ssdDDdss",
    "sssssSss",
], SKIN)
HEAD_SIDE = grid([
    "HHHHHHHH",
    "HHHHHHHH",
    "sssHHHHH",
    "sssdHHHH",
    "ssddsHHH",
    "sssssHHH",
    "ssssssHH",
    "sssssssH",
], SKIN)
HEAD_TOP = grid(["HhHHHHhH", "HHHHhHHH", "hHHHHHHh", "HHhHHHHH",
                 "HHHHHhHH", "HhHHHHHH", "HHHHhHHh", "HHHHHHHH"], SKIN)

STEEL = {"0": "i0", "1": "i1", "2": "i2", "3": "i3", "b": "e0", "e": "e1", "E": "e2", "F": "e3",
         "t": "t1", "T": "t2", "U": "t3", "o": "t0", "k": "k0", "K": "k1"}
HELM_FRONT = grid([    # the commander's helm: steel, a brass band riveted across the brow, the face open, cheek
    "0122332210",      # guards beside the mouth
    "1233333321",
    "1222222221",
    "beEFEEFEeb",
    "2........2",
    "2........2",
    "21......12",
    "21......12",
    "11......11",
    "0........0",
], STEEL)
HELM_SIDE = grid([
    "1223333221",
    "1233333321",
    "1222222221",
    "beEEFEEEeb",
    "2222222222",
    "2233222222",
    "2232222222",
    "1222222221",
    "1122222211",
    "0111111110",
], STEEL)
HELM_TOP = grid(["1222222221", "1223333221", "1233333321", "1233333321", "1223333221",
                 "1223333221", "1233333321", "1233333321", "1223333221", "beEEEEEEeb"], STEEL)
CHEST_FRONT = grid([   # plate, and over it the Dawn's teal tabard with the brass sun; a leather belt and buckle
    "0eEEEEEEe0",
    "13tTTTTt31",
    "13tTeeTt31",
    "13TtEEtT31",
    "13eEFFEe31",
    "13eEFFEe31",
    "13TtEEtT31",
    "13tTeeTt31",
    "12tTTTTt21",
    "12tTTTTt21",
    "0kkkEEkkk0",
    "1ktTTTTtk1",
    "12tTTTTt21",
    "01oTttTo10",
], STEEL)
CHEST_SIDE = grid(["0eEEe0", "123321", "123321", "122221", "122221", "123321", "122221",
                   "122221", "122221", "122221", "0kkkk0", "122221", "122221", "011110"], STEEL)
CHEST_TOP = grid(["0111111110", "1222222221", "1233333321", "1233333321", "1222222221", "0eEEEEEEe0"], STEEL)
PAUL_FRONT = grid([    # a pauldron: two lames and a brass edge
    "122221",
    "133331",
    "122221",
    "011110",
    "122221",
    "133331",
    "beEEeb",
], STEEL)
PAUL_SIDE = grid(["122221", "133331", "122221", "011110", "122221", "133331", "beEEeb"], STEEL)
PAUL_TOP = grid(["122221", "123321", "133331", "133331", "123321", "122221"], STEEL)
BOOT_FRONT = grid(["eEEEEe", "122221", "123321", "122221", "122221", "011110", "000000"], STEEL)
BOOT_SIDE = grid(["eEEEEe", "122221", "123321", "122221", "122221", "011110", "000000"], STEEL)
BOOT_TOP = grid(["beeeeb", "e....e", "e....e", "e....e", "e....e", "beeeeb"], STEEL)

ARM = [                # an arm hanging, shoulder at the top (the pauldron hides the top six rows), the hand last
    "ssSs",
    "sSss",
    "ssss",
    "ssss",
    "sssS",
    "ssss",
    "sSss",
    "ssss",
    "ssSs",
    "sdss",
    "ddsd",
    "dddd",
]
ARM_FRONT = grid(ARM, SKIN)
ARM_SIDE = grid(["ssss", "ssSs", "ssss", "Ssss", "ssss", "sssS", "ssss", "sSss", "ssss", "dsss", "dsdd", "dddd"],
                SKIN)
FIST = grid(["dDDd", "DddD", "DddD", "dDDd"], SKIN)
JEANS = {"j": "j1", "J": "j2", "y": "j0", "Y": "j3"}
LEG_FRONT = grid(["jjJj", "jJjj", "jjjJ", "Jjjj", "jjJj", "yjjy",
                  "jjjj", "jjjj", "jjjj", "jjjj", "jjjj", "jjjj"], JEANS)
LEG_SIDE = grid(["jjjJ", "Jjjj", "jjJj", "jjjj", "jJjj", "yjjy",
                 "jjjj", "jjjj", "jjjj", "jjjj", "jjjj", "jjjj"], JEANS)

# the diamond sword, our own drawing in the manner of the game's item: tip up and to the left as he holds it; a
# dark outline, the blade light along its middle, a dark guard across it, the grip and the pommel below
DIAMOND = {"K": "q0", "d": "q1", "c": "q2", "w": "q3", "x": "q4", "G": "q0", "h": "k1", "P": "q1"}
SWORD = [
    "KKK.............",
    "KxcK............",
    "KdwcK...........",
    ".KdwcK..........",
    "..KdwcK.........",
    "...KdwcK........",
    "....KdwcK.......",
    ".....KdwcK..KK..",
    "......KdwcKKGK..",
    ".......KdwGGK...",
    "........KGGhK...",
    ".......KGKKhhK..",
    ".......KK..KhhK.",
    "............KhPK",
    ".............KPP",
    "..............KK",
]
SWORD_FRONT = grid(SWORD, DIAMOND)
SWORD_BACK = grid([r[::-1] for r in SWORD], DIAMOND)
SWORD_GRIP = (12.5, 12.0)          # the texel his fist closes on
ITEM = 0.75                        # an item pixel in skin pixels (items are drawn smaller in the hand)

# the banner: a pole of oak with an iron foot, a brass finial, an oak crossbar with brass caps; the cloth is typed
# below and drawn hanging from the crossbar
OAK = {"k": "k0", "K": "k1", "L": "k2", "1": "i1", "2": "i2", "0": "i0", "e": "e1", "E": "e2", "F": "e3",
       "b": "e0"}
POLE_TILE = ["Kk", "Kk", "Lk", "Kk", "KK", "Kk", "Lk", "Kk"]     # the grain runs along the pole
POLE_FRONT = grid(POLE_TILE * 8 + ["21", "12", "21", "00"], OAK)[-58:]
BAR_FRONT = grid(["KKKLKKKKKLKKKK", "kkkkkKkkkkkkkk"], OAK)
BRASS = grid(["EF", "eE", "be"], OAK)

CLOTH_KEY = {"T": "t2", "t": "t1", "U": "t3", "o": "t0", "e": "e1", "E": "e2", "F": "e3", "b": "e0"}
CLOTH = [              # the Dawn banner: teal, a brass sun with eight rays, a light band, swallow tails
    "bbbbbbbbbbbb",
    "oTTTTTTTTTTo",
    "tTTTTTTTTTTt",
    "tTTTTeeTTTTt",
    "tTTeTTTTeTTt",
    "tTTTTEETTTTt",
    "teeTEFFETeet",
    "teeTEFFETeet",
    "tTTTTEETTTTt",
    "tTTeTTTTeTTt",
    "tTTTTeeTTTTt",
    "tTTTTTTTTTTt",
    "tTTTTTTTTTTt",
    "tUUUUUUUUUUt",
    "tTTTTTTTTTTt",
    "tTTTTTTTTTTt",
    "tTTTTTTTTTTt",
    "tTTTTTTTTTTt",
    "tTTTTTTTTTTt",
    "tTTTTTTTTTTt",
    "tTTTT..TTTTt",
    "tTTT....TTTt",
    "tTT......TTt",
    "tT........Tt",
]
CLOTH_AT = (7.0, 52.0, -2.6)       # its top-left corner, in skin pixels from his feet
# the wind, typed per frame: how far each band of rows stands out to the right (GUI px), and the light on it as a
# fold passes (-1 in the trough, +1 on the crest). The fold runs down a band per frame; the tails lag a frame.
WAVE = [
    #  rows 0-3  4-7  8-11 12-15 16-19 20-23
    [(0, 0), (1, 1), (2, 0), (2, -1), (3, 0), (4, 1)],
    [(0, 0), (1, 0), (2, 1), (3, 0), (3, -1), (4, 0)],
    [(0, 0), (1, -1), (2, 0), (3, 1), (4, 0), (4, -1)],
    [(0, 0), (1, 0), (1, -1), (2, 0), (4, 1), (5, 0)],
]


RAISE = -140                       # the sword arm, turned up from hanging (degrees, clockwise as we see it)


def fist():
    """where his raised fist closes on the grip (skin pixels from his feet)"""
    pr = np.array([-5.0, 22.0, 0.0])
    return pr + (np.array([-6.0, 13.5, 0.0]) - pr) @ rot(roll=RAISE)


def model():
    """his boxes (world), the same in every frame"""
    bx = []
    # legs, in jeans; boots over them
    for (x0, x1) in ((-4, 0), (0, 4)):
        bx.append(part((x0, 0, -2), (x1, 12, 2), {(2, -1): LEG_FRONT, (0, 1): LEG_SIDE, (0, -1): LEG_SIDE,
                                                  (2, 1): LEG_FRONT}))
        bx.append(part((x0 - 1, 0, -3), (x1 + 1, 7, 3), {(2, -1): BOOT_FRONT, (0, 1): BOOT_SIDE,
                                                         (0, -1): BOOT_SIDE, (1, 1): BOOT_TOP, (2, 1): BOOT_FRONT}))
    # the chestplate over the body
    bx.append(part((-5, 11, -3), (5, 25, 3), {(2, -1): CHEST_FRONT, (0, 1): CHEST_SIDE, (0, -1): CHEST_SIDE,
                                              (1, 1): CHEST_TOP, (2, 1): CHEST_FRONT}))
    # the head and the helm
    bx.append(part((-4, 24, -4), (4, 32, 4), {(2, -1): FACE, (0, 1): HEAD_SIDE, (0, -1): HEAD_SIDE,
                                              (1, 1): HEAD_TOP, (2, 1): HEAD_TOP}))
    bx.append(part((-5, 23, -5), (5, 33, 5), {(2, -1): HELM_FRONT, (0, 1): HELM_SIDE, (0, -1): HELM_SIDE,
                                              (1, 1): HELM_TOP, (2, 1): HELM_SIDE}))
    # the right arm raised up and out to his right, turned about the shoulder as the game turns a limb; the
    # pauldron turns with it
    RA, pr = rot(roll=RAISE), (-5, 22, 0)
    arm_r = grid([r[::-1] for r in ARM], SKIN)
    bx.append(part((-8, 12, -2), (-4, 24, 2), {(2, -1): arm_r, (0, 1): ARM_SIDE, (0, -1): ARM_SIDE,
                                               (1, -1): FIST, (2, 1): arm_r}, RA, pr))
    bx.append(part((-9, 18, -3), (-3, 25, 3), {(2, -1): PAUL_FRONT, (0, 1): PAUL_SIDE, (0, -1): PAUL_SIDE,
                                               (1, 1): PAUL_TOP, (2, 1): PAUL_FRONT}, RA, pr))
    # the left arm swung forward 15 degrees to hold the pole, as the game holds an item
    R = rot(pitch=15)
    pv = (5, 22, 0)
    bx.append(part((4, 12, -2), (8, 24, 2), {(2, -1): ARM_FRONT, (0, 1): ARM_SIDE, (0, -1): ARM_SIDE,
                                             (1, -1): FIST, (2, 1): ARM_FRONT}, R, pv))
    bx.append(part((3, 18, -3), (9, 25, 3), {(2, -1): PAUL_FRONT, (0, 1): PAUL_SIDE, (0, -1): PAUL_SIDE,
                                             (1, 1): PAUL_TOP, (2, 1): PAUL_FRONT}, R, pv))
    # the sword in the raised fist, upright in the plane through it: its grip where his fist closes, the guard
    # just clear of his knuckles, the blade on up the line of the arm
    gx, gy = SWORD_GRIP
    g = fist()
    x0, y0 = g[0] - gx * ITEM, g[1] + gy * ITEM
    bx.append(part((x0, y0 - 16 * ITEM, -ITEM / 2), (x0 + 16 * ITEM, y0, ITEM / 2),
                   {(2, -1): SWORD_FRONT, (2, 1): SWORD_BACK}))
    # the banner pole through his left fist, its finial, the crossbar
    bx.append(part((5, 0, -3.6), (7, 58, -1.6), {(2, -1): POLE_FRONT, (0, 1): POLE_FRONT, (0, -1): POLE_FRONT,
                                                (2, 1): POLE_FRONT, (1, 1): BRASS}))
    bx.append(part((4.5, 58, -4.1), (7.5, 60, -1.1), {"all": BRASS}))
    bx.append(part((5.5, 60, -3.1), (6.5, 62, -2.1), {"all": BRASS}))
    bx.append(part((5, 52, -3.6), (19, 54, -1.6), {(2, -1): BAR_FRONT, (1, 1): BAR_FRONT, (1, -1): BAR_FRONT,
                                                  (2, 1): BAR_FRONT}))
    bx.append(part((19, 51.5, -4.1), (20, 54.5, -1.1), {"all": BRASS}))
    return bx


def shadow_boxes():
    """everything of his that casts a shadow on the hill: his boxes and the cloth as a sheet"""
    sheet = part((7, 28, -2.7), (19, 52, -2.5), {"all": grid(["T"], CLOTH_KEY)})
    return model() + [sheet]


# ================================================================================================ drawing
def screen(m):
    """a point in skin pixels from his feet to GUI px, along the parallel view"""
    p = FEET + np.asarray(m, float) * PX
    p25 = p - (p[2] - FEET[2]) * VIEW
    return XV + FOCAL * p25[0] / FEET[2], YV - FOCAL * (p25[1] - EYE[1]) / FEET[2]


AMB = {(0, 1): 0.8, (0, -1): 0.8, (1, 1): 1.0, (1, -1): 0.5, (2, 1): 0.8, (2, -1): 1.0}
A_SKY = 0.5                        # the game's entity light: his front stays readable in the backlight


def render(frame):
    import title_hill as T
    xs = np.arange(SW) + AT[0] + 0.5
    ys = np.arange(SH) + AT[1] + 0.5
    X, Y = np.meshgrid(xs, ys)
    n = X.size
    P = np.stack([(X.ravel() - XV) * FEET[2] / FOCAL, EYE[1] - (Y.ravel() - YV) * FEET[2] / FOCAL,
                  np.full(n, FEET[2])], 1)
    D = np.broadcast_to(VIEW, (n, 3)).copy()
    O = P - 3 * D
    boxes = model()
    t, nrm, bix, tone, loc = B.boxes_hit(O, D, boxes)
    code = np.full(n, "", dtype=object)
    # the cloth, drawn hanging from the crossbar, each band of rows set out by the wind
    cx, cy = screen(CLOTH_AT)
    cx, cy = int(round(cx)), int(round(cy))
    canvas = code.reshape(SH, SW)
    for r, row in enumerate(CLOTH):
        dx, fold = WAVE[frame][r // 4]
        for c, ch in enumerate(row):
            if ch == ".":
                continue
            tn = CLOTH_KEY[ch]
            lvl = 12 + fold + (1 if ch in "EF" else 0)
            for j in range(4):
                for i in range(4):
                    x, y = cx + 4 * c + dx + i - AT[0], cy + 4 * r + j - AT[1]
                    if 0 <= x < SW and 0 <= y < SH:
                        canvas[y, x] = f"{tn}.{lvl}c"
    hit = bix >= 0
    if hit.any():
        Ph = O[hit] + D[hit] * t[hit, None]
        nh = nrm[hit]
        ax = np.argmax(np.abs(nh), 1)
        sg = np.sign(nh[np.arange(len(nh)), ax]).astype(int)
        amb = np.array([AMB[(int(a), int(g))] for a, g in zip(ax, sg)])
        ndl = np.clip(nh @ SUN, 0, None)
        lit = np.zeros(len(nh), bool)
        up = ndl > 0
        if up.any():
            Os = Ph[up] + nh[up] * 1e-3
            Ds = np.broadcast_to(SUN, (int(up.sum()), 3)).copy()
            lit[up] = B.boxes_hit(Os, Ds, boxes)[2] < 0
        v = A_SKY * amb + T.A_SUN * ndl * lit
        lvl = np.clip(np.round(v / T.VMAX * (T.NL - 1)), 0, T.NL - 1).astype(int)
        code[hit] = [f"{tn}.{l}" for tn, l in zip(tone[hit], lvl)]
    return code.reshape(SH, SW)


GLOW = {"day": (1.9, 1.9, 1.7), "night": (1.4, 1.5, 1.6)}   # the cloth lit through by the sun, by the moon


def palette(codes, tod):
    import title_hill as T
    plain = {c for c in set(codes.ravel()) - {""} if not c.endswith("c")}
    pal = T.palette(np.array(sorted(plain), dtype=object), tod)
    sky = T.DAY_SKY if tod == "day" else T.NIGHT_SKY
    for c in set(codes.ravel()):
        if c.endswith("c"):
            tn, l = c[:-1].split(".")
            pal[c] = B.lit_hex(B.ALBEDO[tn], np.array(sky[min(int(l), T.NL - 1)]) * GLOW[tod])
    return pal


if __name__ == "__main__":
    import title_hill as T
    frames = [render(f) for f in range(4)]
    body = [np.where(np.char.endswith(fr.astype(str), "c"), "", fr) for fr in frames]
    assert all((b == body[0]).all() for b in body), "the body must be the same in every frame"
    for tod in ("day", "night"):
        for f, codes in enumerate(frames):
            save(T.to_image(codes, palette(codes, tod)), "commander-%s-%d.png" % (tod, f))
    with open(os.path.join(OUT, "commander-at.json"), "w") as fh:
        json.dump(list(AT), fh)
    n = len(set().union(*[set(fr.ravel()) for fr in frames]) - {""})
    print("commander written: 4 frames, %d codes" % n)
