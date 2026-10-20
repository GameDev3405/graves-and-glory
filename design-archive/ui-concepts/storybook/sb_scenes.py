"""RETIRED (Kort, 2026-10-18): every scene image in the storybook is in-game footage (stills/, see NOTES.md),
and no page uses what this file draws; its old outputs are in art/retired/. Kept for the record only.

The storybook's paintings of the world, built from blocks (sb_world.py draws them).

kings: the c1_fall panel 3, "Day three. The guard could not reach the highest mound through the Tide. Someone
small climbed it alone, with something held in both hands." The field of kings at night under fog: the ring of
barrows, the first king's mound highest; the Tide stands in the fog at its foot, white-eyed, facing the guard; the
guard stands in the foreground with a lantern and the Order's banner, held; and on the summit, against the moon,
someone small with a curved horn held in both hands and a bow on her back. Light: the moon stands behind the mound
(the light is cheated higher than the disc, so tops take it), the lantern is the only warm thing.

python sb_scenes.py kings [--half]   -> art/panel-kings.png (960 x 540; --half renders 480 x 270 to look)"""
import math, random, sys, time
import numpy as np
from PIL import Image
import sb_world as SW
from sb_world import B, lin, srgb, shade, quant, bayer, checker
from sb_common import ART

PX = 1 / 16.0


# ================================================================================================ figures
def g(rows, key):
    return B.grid(rows, key)


def sol(t, w=1, h=1):
    return B.solid(t, w, h)


class Rig:
    """an entity model built from boxes in skin pixels (1/16 block x scale), placed at its feet `at` (world),
    turned by yaw (0 faces the camera, -z; 180 faces away), leaning by pitch (negative leans forward)"""
    def __init__(self, at, yaw=0.0, lean=0.0, scale=1.0):
        self.at = np.array(at, float)
        self.Rf = B.rot(yaw=yaw, pitch=lean)
        self.px = PX * scale
        self.boxes = []

    def add(self, lo, hi, faces, R=None, pivot=None, part=0):
        lo, hi = np.array(lo, float), np.array(hi, float)
        c = (lo + hi) / 2.0
        Rp = np.eye(3) if R is None else np.asarray(R, float)
        if pivot is not None:
            pv = np.array(pivot, float)
            c = pv + (c - pv) @ Rp
        A = Rp @ self.Rf
        self.boxes.append(B.Box(self.at + (c * self.px) @ self.Rf, (hi - lo) / 2.0 * self.px, faces, A, part))
        return self

    def limb(self, lo, hi, faces, pivot, pitch=0.0, roll=0.0, yaw=0.0):
        return self.add(lo, hi, faces, B.rot(yaw=yaw, pitch=pitch, roll=roll), pivot)


def faces(front=None, back=None, left=None, right=None, top=None, bottom=None, all=None):
    """face grids by the part's own sides: front is -z, back +z, left -x, right +x as the part faces the camera"""
    f = {}
    for k, v in (((2, -1), front), ((2, 1), back), ((0, -1), left), ((0, 1), right), ((1, 1), top),
                 ((1, -1), bottom)):
        if v is not None:
            f[k] = v
    if all is not None:
        f["all"] = all
    return f


# ---------------------------------------------------------------- the Tide: drowned raiders and trident guards
TIDE_KEY = {"a": "z0", "b": "z1", "c": "z2", "d": "z3", "n": "Z0", "m": "Z1", "o": "Z2", "e": "F0", "f": "F1",
            "u": "U0", "v": "U1", "w": "U2", "h": "H1", "H": "H2", "k": "X1", "K": "X2", "y": "K0", "i": "V1",
            "j": "V2", "I": "V0"}
RAIDER_HEAD_FRONT = g([
    "vwwwwwwv",
    "uvvvvvvu",
    "bccccccb",
    "beecceeb",
    "bccccccb",
    "bcbyybcb",
    "bbccccbb",
    "abbbbbba"], TIDE_KEY)
RAIDER_HEAD_SIDE = g([
    "wwwwwwvv",
    "vvvvvvuu",
    "cccccbbb",
    "ccccbbbb",
    "cccbbbbb",
    "ccbbbbba",
    "cbbbbbba",
    "bbbbbaaa"], TIDE_KEY)
RAIDER_HEAD_BACK = g([
    "vwwwwwwv",
    "uvvvvvvu",
    "ubbbbbbu",
    "bbbbbbbb",
    "abbbbbba",
    "abbabbba",
    "aabbbbaa",
    "aaaaaaaa"], TIDE_KEY)
SHELL_TOP = sol("U2", 9, 9)
RAIDER_BODY_FRONT = g([
    "bccccccb",
    "bcmcccbb",
    "bccmcccb",
    "bcccmccb",
    "bbcccmcb",
    "bccccomb",
    "bbccccob",
    "bbbccccb",
    "nnnnnnnn",
    "mmmnmmmm",
    "mnmmmnmm",
    "nmmmmmnm"], TIDE_KEY)
RAIDER_BODY_BACK = g([
    "bbbbbbbb",
    "bbbbbbbb",
    "abbbbbba",
    "abbbbbba",
    "abbbbbba",
    "abbbbbba",
    "aabbbbaa",
    "aabbbbaa",
    "nnnnnnnn",
    "mnmmmnmm",
    "mmmnmmmm",
    "nmnmmmnm"], TIDE_KEY)
RAIDER_ARM = g([
    "cccb",
    "cccb",
    "kccb",
    "cckb",
    "ccKb",
    "kccb",
    "cckb",
    "bccb",
    "bcKb",
    "kbcb",
    "bbcb",
    "bbbb"], TIDE_KEY)
RAIDER_LEG = g([
    "mmmn",
    "mnmn",
    "mmmn",
    "nmmn",
    "mmnn",
    "cbbb",
    "ccbb",
    "cbba",
    "bbba",
    "bbaa",
    "abaa",
    "aaaa"], TIDE_KEY)
KELP = sol("X1")
TRIDENT = sol("V1")
TRIDENT_LIT = sol("V2")


def tide_raider(at, yaw=0.0, lean=-8.0, arms="forward", trident=False, seed=0, coral=False):
    """a drowned raider (the unit identity's look): turtle-shell helm, white eyes, bare verdigris torso under a
    torn sash, kelp streaming from the arms; trident guards carry a trident upright and bleached coral on the
    shoulders"""
    rnd = random.Random(seed)
    r = Rig(at, yaw, lean)
    r.add((-2, 0, -2), (0, 12, 2), faces(all=RAIDER_LEG))
    r.add((0, 0, -2), (2, 12, 2), faces(all=RAIDER_LEG))
    r.add((-4, 12, -2), (4, 24, 2), faces(front=RAIDER_BODY_FRONT, back=RAIDER_BODY_BACK, all=sol("z1", 4, 12)))
    hf = faces(front=RAIDER_HEAD_FRONT, back=RAIDER_HEAD_BACK, left=RAIDER_HEAD_SIDE, right=RAIDER_HEAD_SIDE,
               top=sol("U1", 8, 8), bottom=sol("z0", 8, 8))
    hy = rnd.uniform(-12, 12)
    r.add((-4, 24, -4), (4, 32, 4), hf, B.rot(yaw=hy, pitch=rnd.uniform(-8, 6)), (0, 24, 0))
    r.add((-4.5, 29, -4.5), (4.5, 33, 4.5), faces(top=SHELL_TOP, all=sol("U1", 9, 4)),
          B.rot(yaw=hy), (0, 24, 0))
    ap = {"forward": (80, 84), "down": (8, -6), "raised": (150, 20), "one": (85, 4)}[arms]
    for side, (x0, x1) in ((-1, (-8, -4)), (1, (4, 8))):
        pitch = ap[0] if side < 0 else ap[1]
        pitch += rnd.uniform(-8, 8)
        r.limb((x0, 12, -2), (x1, 24, 2), faces(all=RAIDER_ARM), (x0 + 2 * side * -1 + 2 * side, 22, 0),
               pitch=pitch, roll=side * rnd.uniform(0, 6))
        # kelp: two strands hanging from the forearm
        for k in range(2):
            kx = x0 + 1 + 2 * k
            r.limb((kx, 8, 1.4), (kx + 1, 16, 2.0), faces(all=KELP), (x0 + 2, 22, 0), pitch=pitch * 0.6,
                   roll=side * 4)
    if coral:
        for sx in (-6, 5):
            r.add((sx, 24, -1), (sx + 1, 29, 0), faces(all=sol("H2")))
            r.add((sx - 1, 27, -1), (sx, 31, 0), faces(all=sol("H1")))
            r.add((sx + 1, 26, 0), (sx + 2, 30, 1), faces(all=sol("H2")))
    if trident:
        # held upright in the right hand (screen left), butt on the ground: a shaft and three prongs
        tx = -7.0
        r.add((tx - 0.5, 0, -6.5), (tx + 0.5, 40, -5.5), faces(top=TRIDENT_LIT, all=TRIDENT))
        r.add((tx - 3.5, 38, -6.5), (tx + 3.5, 39.2, -5.5), faces(top=TRIDENT_LIT, all=TRIDENT))
        for px_ in (-3.5, -0.5, 2.5):
            r.add((tx + px_, 39, -6.5), (tx + px_ + 1, 45 if px_ != -0.5 else 47, -5.5),
                  faces(top=TRIDENT_LIT, all=TRIDENT_LIT if px_ == -0.5 else TRIDENT))
    return r.boxes


# ---------------------------------------------------------------- the climber: someone small, a horn, a bow
CLIMB_KEY = {"a": "K0", "b": "K1", "c": "K2", "k": "X0", "K": "X1", "L": "X2", "h": "H1", "H": "H2", "i": "i1"}


def bezier(p0, c, p1, t):
    return tuple((1 - t) ** 2 * a + 2 * (1 - t) * t * b + t * t * d for a, b, d in zip(p0, c, p1))


def climber(at, yaw=-90.0, scale=0.85):
    """someone small: a moss rag hood with its point at the back, a short cloak, a bow slung across the back;
    leaning into the climb, both hands holding a great curved horn by its narrow end, its body sweeping forward
    and up in front of her face and opening into a flared bell. Read as a silhouette against the moon, so every
    face is near black; only the top edges take the moonlight"""
    r = Rig(at, yaw, -8.0, scale=scale)
    dark = sol("K1")
    r.limb((-2, 0, -2), (0, 12, 2), faces(all=dark), (-1, 12, 0), pitch=-16)
    r.limb((0, 0, -2), (2, 12, 2), faces(all=dark), (1, 12, 0), pitch=14)
    r.add((-4, 12, -2), (4, 24, 2), faces(all=sol("K2")))
    r.add((-4.5, 9, -2.5), (4.5, 22, 2.6), faces(all=sol("X0")))            # the cloak, short
    head = B.rot(pitch=-6)
    r.add((-4, 24, -4), (4, 32, 4), faces(all=sol("X0")), head, (0, 24, 0))
    r.add((-4.6, 24.5, -4.6), (4.6, 32.6, 4.6), faces(all=sol("X1")), head, (0, 24, 0))   # the hood
    r.add((-1.5, 27, 4.4), (1.5, 31, 8.0), faces(all=sol("X0")), head, (0, 24, 0))         # its point
    # both arms forward and down to the hands under the horn's curve
    for (x0, x1) in ((-8, -4), (4, 8)):
        r.limb((x0, 10, -2), (x1, 22, 2), faces(all=sol("K1")), ((x0 + x1) / 2, 22, 0), pitch=60,
               yaw=14 if x0 < 0 else -14)
    # the horn along a curve from the hands: forward, then up, widening, then the bell
    p0, c, p1 = (24.0, -5.0), (4.0, -13.0), (30.0, -22.0)
    n = 8
    pts = [bezier(p0, c, p1, k / float(n)) for k in range(n + 1)]
    hk = faces(all=sol("H1"), top=sol("H2"))
    for k in range(n):
        (ya, za), (yb, zb) = pts[k], pts[k + 1]
        dy, dz = yb - ya, zb - za
        L = math.hypot(dy, dz) + 0.5
        pch = math.degrees(math.atan2(-dy, dz))
        my, mz = (ya + yb) / 2, (za + zb) / 2
        wd = 1.1 + 2.3 * (k / float(n - 1)) ** 1.7
        r.add((-wd / 2, my - wd / 2, mz - L / 2), (wd / 2, my + wd / 2, mz + L / 2), hk, B.rot(pitch=pch),
              (0, my, mz))
    by, bz = pts[-1]
    r.add((-2.4, by - 1.2, bz - 2.4), (2.4, by + 0.6, bz + 2.4), hk)         # the bell flaring
    r.add((-3.6, by + 0.4, bz - 3.6), (3.6, by + 1.8, bz + 3.6), hk)         # and its lip
    # the bow slung across the back, canted: a stave in three pieces, bent, and its string
    bow = B.rot(roll=0, pitch=-24)
    for (y0, y1, p) in ((7, 15, 20), (14, 24, 0), (23, 31, -20)):
        r.add((-0.6, y0, 3.8), (0.6, y1, 5.0), faces(all=sol("K2")), B.rot(pitch=p) @ bow, (0, 19, 0))
    r.add((-0.3, 8, 2.7), (0.3, 30, 3.1), faces(all=sol("K0")), bow, (0, 19, 0))
    return r.boxes


# ---------------------------------------------------------------- the guard: Order knights, seen from behind
KN_KEY = {"a": "T0", "b": "T1", "c": "T2", "d": "T3", "e": "E0", "f": "E1", "g": "E2", "h": "E3", "n": "C0",
          "m": "C1", "o": "C2", "y": "K0", "z": "K1", "G": "G1", "H": "G2", "k": "k1", "K": "k2", "w": "S1",
          "W": "S2"}
HELM_BACK = g([
    "dddddddd",
    "ccccbccc",
    "ccccbccc",
    "cccbbccc",
    "ccccbccc",
    "bbbbabbb",
    "aGaaaaGa",
    "aaaaaaaa"], KN_KEY)
HELM_SIDE = g([
    "dddddddd",
    "cccccccb",
    "ccccccbb",
    "yyyyyccb",
    "cccccccb",
    "bbbbbbba",
    "aaGaaaaa",
    "aaaaaaaa"], KN_KEY)
HELM_FRONT = g([
    "cdddddcc",
    "bccccccb",
    "ccccccca",
    "yyyyyyya",
    "ccccccca",
    "cbccccba",
    "bcbccbba",
    "abbbbbaa"], KN_KEY)
HELM_TOP = g(["dddddddd"] * 8, KN_KEY)
TABARD_BACK = g([
    "fggggggf",
    "mmmhhmmm",
    "mmmhhmmn",
    "mmmhhmmn",
    "hhhhhhhh",
    "hhhhhhhh",
    "mmmhhmmn",
    "mmmhhmmn",
    "mmmhhmmn",
    "nmmhhmnn",
    "GGGHHGGG",
    "nmmhhmnn"], KN_KEY)
SKIRT_BACK = g([
    "nmmhhmmn",
    "nmmhhmmn",
    "mmmhhmmn",
    "nmmhhmnn",
    "nmmhhmnn",
    "nnmhhmnn",
    "nnmhhnnn",
    "nnnnnnnn"], KN_KEY)
TABARD_SIDE = g(["ffff"] + ["mmmn"] * 9 + ["GGGG", "mmnn"], KN_KEY)
PLATE_ARM = g([
    "ghhg",
    "gggf",
    "fggf",
    "bccb",
    "gggf",
    "fggf",
    "fgff",
    "bccb",
    "fggf",
    "eeee",
    "bbbb",
    "abba"], KN_KEY)
PLATE_LEG = g(["gggf", "fggf", "bccb", "fggf", "eggf", "efgf", "eeff", "bbcb", "eeee", "abba", "aaaa", "aaaa"],
              KN_KEY)
SHIELD_BACK = g([
    "kKKKKKKKKk",
    "KkkkkkkkkK",
    "KkkkkkkkkK",
    "KkkzzzkkkK",
    "zzzzzzzzzz",
    "KkkzzzkkkK",
    "KkkkkkkkkK",
    "KkkkkkkkkK",
    "KkkkkkkkkK",
    "KkkzzzkkkK",
    "zzzzzzzzzz",
    "KkkzzzkkkK",
    ".KkkkkkkK.",
    "..KkkkkK..",
    "...KkkK...",
    "....KK...."], KN_KEY)
POLE = sol("w")
POLE_KEY = {"w": "S1"}


def knight(at, yaw=180.0, sword=True, shield=True, arm_up=None, seed=0):
    """an Order knight (the Templar's look: closed flat-topped great helm, white plate, a knee-length tabard in
    the team cloth with a white cross, a longsword), drawn to be seen from behind at a few px per skin px"""
    rnd = random.Random(seed)
    r = Rig(at, yaw, rnd.uniform(-2, 2))
    r.add((-2, 0, -2), (0, 12, 2), faces(all=PLATE_LEG))
    r.add((0, 0, -2), (2, 12, 2), faces(all=PLATE_LEG))
    r.add((-4, 12, -2), (4, 24, 2), faces(back=TABARD_BACK, front=TABARD_BACK, left=TABARD_SIDE,
                                          right=TABARD_SIDE, top=sol("E2", 8, 4)))
    r.add((-4.3, 5, -2.3), (4.3, 13, 2.3), faces(back=SKIRT_BACK, front=SKIRT_BACK, all=sol("C1", 4, 8)))
    hy = rnd.uniform(-10, 10)
    r.add((-4, 24, -4), (4, 32, 4), faces(back=HELM_BACK, front=HELM_FRONT, left=HELM_SIDE, right=HELM_SIDE,
                                          top=HELM_TOP), B.rot(yaw=hy), (0, 24, 0))
    # pauldrons
    for sx in (-1, 1):
        x0 = 4 if sx > 0 else -9
        r.add((x0, 21, -2.6), (x0 + 5, 25, 2.6), faces(top=sol("E3", 5, 5), all=sol("E2", 5, 4)))
    # the right arm (screen right from behind) holds the sword point down and forward; the left the shield
    rp = 18 if arm_up is None else arm_up
    r.limb((4, 12, -2), (8, 24, 2), faces(all=PLATE_ARM), (6, 22, 0), pitch=rp, roll=-4)
    r.limb((-8, 12, -2), (-4, 24, 2), faces(all=PLATE_ARM), (-6, 22, 0), pitch=30, roll=4)
    if shield:
        r.add((-11.5, 6, -6), (-10.5, 22, 4), faces(left=SHIELD_BACK, right=SHIELD_BACK, all=sol("k1")),
              B.rot(yaw=-8), (-10, 14, 0))
    if sword:
        a = math.radians(rp)
        hy_ = 22 - 11 * math.cos(a)
        hz_ = -11 * math.sin(a)
        R_ = B.rot(pitch=rp - 70)
        r.add((5.5, hy_ - 18, hz_ - 0.5), (6.5, hy_, hz_ + 0.5), faces(all=sol("T3"), top=sol("T3")), R_,
              (6, hy_, hz_))
        r.add((4, hy_ - 0.5, hz_ - 0.7), (8, hy_ + 0.8, hz_ + 0.7), faces(all=sol("G1")), R_, (6, hy_, hz_))
    return r.boxes


def standard_bearer(at, seed=0):
    """a knight holding the Order's banner upright: the pole in his raised right hand, the cloth hanging from a
    crossbar, seen from behind (its reverse, the device in mirror)"""
    boxes = knight(at, sword=False, shield=False, arm_up=40, seed=seed)
    x, y, z = at
    px = PX
    pole_x = x - 0.42
    boxes.append(B.Box((pole_x, 1.9, z - 0.2), (0.5 * px, 1.9, 0.5 * px), {"all": sol("S2")}))
    boxes.append(B.Box((pole_x, 3.62, z - 0.2), (0.62, 0.6 * px, 0.6 * px), {"all": sol("S1")}))
    boxes.append(B.Box((pole_x, 3.82, z - 0.2), (1.2 * px, 1.2 * px, 1.2 * px), {"all": sol("G2")}))
    cloth = g([
        "ggggggggggggggggggg",
        "ggggggggggggggggggg",
        "gggggggggHggggggggg",
        "ggggggggHHHgggggggg",
        "gggggggggHggggggggg",
        "ggggggGgGHGgGgggggg",
        "gggggggGHHHGggggggg",
        "ggggggGHHHHHGgggggg",
        "gggggggGHHHGggggggg",
        "ggggggGgGHGgGgggggg",
        "gggggggggHggggggggg",
        "ggggggggggggggggggg",
        "fggggggggggggggggff",
        "fgggggggggggggggfff",
        "ffggggggggggggggfff",
        "fffgggggggggggfffff",
        "fffggggfggggggfffff",
        "ffffgggfffgggffffff",
        "fffffggfffffgffffff",
        "ffffff.fffff.ffffff",
        "fffff...fff...fffff",
        "ffff.....f.....ffff",
    ], KN_KEY)
    boxes.append(B.Box((pole_x, 2.9, z - 0.14), (0.58, 0.66, 0.4 * px), {(2, 1): cloth, (2, -1): cloth,
                                                                          "all": sol("E1")}, B.rot(yaw=6)))
    return boxes


def lantern_pole(at, seed=0):
    """a knight holding a lantern up on a short staff in his right hand; the lantern is a lit box (its light is
    an unseen light block placed in the world at the same spot)"""
    boxes = knight(at, sword=False, shield=True, arm_up=70, seed=seed)
    x, y, z = at
    lx, ly, lz = x + 0.42, 2.05, z - 0.55
    boxes.append(B.Box((lx, ly - 0.42, lz), (0.5 * PX, 0.5, 0.5 * PX), {"all": sol("S1")}))
    cage = g(["iIIi", "IffI", "IffI", "IffI", "iIIi"], {"i": "i0", "I": "i1", "f": "f2"})
    boxes.append(B.Box((lx, ly + 0.2, lz), (2.4 * PX, 3 * PX, 2.4 * PX), {"all": cage, (1, 1): sol("i1", 4, 4)}))
    boxes.append(B.Box((lx, ly + 0.43, lz), (1.2 * PX, 0.6 * PX, 1.2 * PX), {"all": sol("i0")}))
    return boxes, (lx, ly, lz)


# ================================================================================================ the world
def mound(w, cx, cz, R, Hh, kerb=0.7, door=False, seed=0, cairn=None, plants=None, shape=0.72):
    """a barrow: a round mound of turf over earth, its profile a flattened dome stepped by whole blocks; a ring of
    kerbstones at its foot (stone bricks, some mossy, some cracked out); a passage door on the near side (faced
    in stone bricks, a lintel, a dark way in)"""
    rnd = random.Random(seed)
    tops = {}
    for x in range(int(math.floor(cx - R - 1)), int(math.ceil(cx + R + 2))):
        for z in range(int(math.floor(cz - R - 1)), int(math.ceil(cz + R + 2))):
            rr = math.hypot(x + 0.5 - cx, z + 0.5 - cz) / R
            if rr >= 1:
                continue
            h = Hh * (1 - rr * rr) ** shape
            ty = int(round(h + rnd.uniform(-0.18, 0.18)))
            if ty <= 0:
                continue
            for y in range(0, ty):
                w.set(x, y, z, "grass" if y == ty - 1 else "dirt")
            tops[(x, z)] = ty
    if kerb:
        n = int(2 * math.pi * R * 1.1)
        for i in range(n):
            a = 2 * math.pi * i / n
            x = int(math.floor(cx + math.cos(a) * (R + 0.4)))
            z = int(math.floor(cz + math.sin(a) * (R + 0.4)))
            if rnd.random() > kerb:
                continue
            kind = rnd.choice(["stone_bricks", "stone_bricks", "mossy_stone_bricks", "mossy_stone_bricks",
                               "mossy_cobblestone"])
            if tops.get((x, z), 0) <= 1:
                w.set(x, 0, z, kind)
                if rnd.random() < 0.35:
                    w.set(x, 1, z, rnd.choice(["mossy_stone_bricks", "stone_bricks", "brick_slab"]))
    if door:
        dz = int(math.floor(cz - R * 0.80))
        dx = int(math.floor(cx))
        for y in range(0, 4):
            for x in range(dx - 2, dx + 3):
                w.set(x, y, dz, "stone_bricks" if (x + y) % 3 else "mossy_stone_bricks")
        for x in range(dx - 2, dx + 3):
            w.set(x, 3, dz, "stone_bricks")
            w.set(x, 3, dz - 1, "stone_slab" if x in (dx - 2, dx + 2) else "stone_bricks")
        for y in range(0, 2):
            for z in range(dz - 1, dz + 5):
                w.set(dx, y, z, None)
            w.set(dx - 1, y, dz - 1, "mossy_stone_bricks")
            w.set(dx + 1, y, dz - 1, "stone_bricks")
        w.set(dx, 2, dz - 1, "stone_bricks")
        for z in range(dz - 6, dz - 1):
            for x in range(dx - 1, dx + 2):
                if rnd.random() < 0.75:
                    w.set(x, -1, z, rnd.choice(["stone_bricks", "mossy_stone_bricks", "path", "gravel"]))
    if cairn:
        (x, z) = cairn
        ty = tops.get((x, z), 0)
        w.set(x, ty, z, "mossy_cobblestone")
        w.set(x + 1, ty, z, "stone_slab") if tops.get((x + 1, z), 0) == ty else None
    if plants:
        for (x, z), ty in tops.items():
            if rnd.random() < plants and w.get(x, ty, z) is None:
                w.set(x, ty, z, rnd.choice(["short_grass", "short_grass", "tall_grass", "short_grass"]))
    return tops


def stone_post(w, x, z, h, kind="stone"):
    for y in range(0, h):
        w.set(x, y, z, kind if y < h - 1 or kind != "stone" else "andesite")


# ================================================================================================ the sky
SKY_BANDS = ["#05070e", "#060912", "#080c17", "#0a101d", "#0d1424", "#10192b", "#141e33", "#18243b", "#1d2b44",
             "#23324c", "#2a3a55", "#33435e"]
MOON = [
    "................",
    "..aaaaaaaaaaaa..",
    ".abbbbbbbbbbbba.",
    ".abbbbcbbbbbbba.",
    ".abbbccbbbbdbba.",
    ".abbbbbbbbddbba.",
    ".abdbbbbbbbbbba.",
    ".abddbbbbbbbcba.",
    ".abbbbbcbbbbbba.",
    ".abbbbccbbbbbba.",
    ".abbbbbbbbbdbba.",
    ".abcbbbbbbddbba.",
    ".abccbbbbbbbbba.",
    ".abbbbbbbbbbbba.",
    "..aaaaaaaaaaaa..",
    "................",
]
MOON_KEY = {"a": "#c3ccd6", "b": "#e8edf1", "c": "#c9d1da", "d": "#b3bdc8"}


def hexv(c):
    c = c.lstrip("#")
    return np.array([int(c[i:i + 2], 16) for i in (0, 2, 4)], float)


def night_sky(W, H, horizon, moon, scale=1, seed=7):
    """the night sky in banded steps (each band edge broken by a two-row ordered dither), square stars in three
    brightnesses kept off the moon, the game's square moon (16 x 16 texels at 4 px each) and its halo in three
    stepped rings, rounded square like the moon"""
    rnd = random.Random(seed)
    img = np.zeros((H, W, 3))
    th = bayer(H, W)
    nb = len(SKY_BANDS)
    for y in range(H):
        u = min(1.0, max(0.0, y / float(horizon))) ** 1.25 * (nb - 1)
        k = int(u)
        f = u - k
        lo, hi = hexv(SKY_BANDS[k]), hexv(SKY_BANDS[min(nb - 1, k + 1)])
        row = np.where((f > 0.82) & (th[y] < (f - 0.82) / 0.18), 1, 0)[:, None]
        img[y] = lo * (1 - row) + hi * row
    mx, my = moon
    # halo
    yy, xx = np.mgrid[0:H, 0:W]
    dx, dy = np.abs(xx - mx), np.abs(yy - my)
    d = 0.55 * np.maximum(dx, dy) + 0.45 * np.hypot(dx, dy)
    for (r, a) in ((92 * scale, 0.03), (68 * scale, 0.045), (50 * scale, 0.07)):
        m = d < r
        img[m] = img[m] * (1 - a) + hexv("#9fb3d0") * a
    # stars
    for i in range(int(170 * scale * scale)):
        x, y = rnd.randrange(W), rnd.randrange(int(horizon * 0.86))
        if math.hypot(x - mx, y - my) < 130 * scale:
            continue
        c = rnd.choice(["#7d8aa3", "#7d8aa3", "#a7b3c8", "#a7b3c8", "#dfe7f2"])
        img[y, x] = hexv(c)
        if c == "#dfe7f2" and rnd.random() < 0.45 and scale > 0.6:
            for (ax, ay) in ((1, 0), (0, 1), (1, 1)):
                if x + ax < W and y + ay < H:
                    img[y + ay, x + ax] = hexv("#c3cddd")
    # the moon
    s = max(1, int(round(4 * scale)))
    x0, y0 = int(mx - 8 * s), int(my - 8 * s)
    for j, row in enumerate(MOON):
        for i, ch in enumerate(row):
            if ch != ".":
                img[y0 + j * s:y0 + (j + 1) * s, x0 + i * s:x0 + (i + 1) * s] = hexv(MOON_KEY[ch])
    return img


# ================================================================================================ fog
def value_noise(x, z, cell, seed):
    """blocky value noise on whole blocks: smooth between cell corners, read at block centres"""
    xi, zi = np.floor(x / cell), np.floor(z / cell)
    fx, fz = x / cell - xi, z / cell - zi

    def h(a, b):
        v = np.sin(a * 127.1 + b * 311.7 + seed * 74.7) * 43758.5453
        return v - np.floor(v)
    sx, sz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
    return (h(xi, zi) * (1 - sx) * (1 - sz) + h(xi + 1, zi) * sx * (1 - sz) + h(xi, zi + 1) * (1 - sx) * sz +
            h(xi + 1, zi + 1) * sx * sz)


def ground_fog(R, d0, hs, seed=3, bank=0.6):
    """fog lying on the ground: density d0 * exp(-y / hs), thicker in banks (blocky noise over x, z), integrated
    along each ray from the eye to what it hits. Returns the amount 0..1 per pixel"""
    P, eye, hit = R["P"], R["eye"], R["hit"]
    seg = np.linalg.norm(P - eye, axis=2)
    ye, yp = eye[1], np.maximum(P[..., 1], 0.0)
    dy = ye - yp
    e1, e2 = np.exp(-yp / hs), np.exp(-ye / hs)
    mean = np.where(np.abs(dy) > 1e-3, hs * (e1 - e2) / np.where(np.abs(dy) > 1e-3, dy, 1), e1)
    bx = np.floor(P[..., 0]) + 0.5
    bz = np.floor(P[..., 2]) + 0.5
    n = value_noise(bx, bz, 7.0, seed) * 0.65 + value_noise(bx, bz, 3.0, seed + 1) * 0.35
    dens = d0 * (1 - bank + 2 * bank * n)
    return np.where(hit, 1 - np.exp(-dens * seg * mean), 0.0)


def stepped_fog(R, ground, layers, steps=5, band=0.3):
    """the fog as a painter lays it on parallax layers: each depth layer takes one flat haze (layers: [(far
    edge in blocks, haze)], nearest first), and the ground fog over it in hard steps whose edges break into a
    narrow checker on the world (never on figures, which take the nearest step whole)"""
    t = R["t"]
    hz = np.zeros(t.shape)
    lo = 0.0
    for (far, h) in layers:
        hz[(t >= lo) & (t < far)] = h
        lo = far
    hz[t >= lo] = layers[-1][1]
    H, W = t.shape
    gq = np.where(R["ent"], quant(ground, steps), quant(ground, steps, checker(H, W), band))
    return np.where(R["hit"], 1 - (1 - hz) * (1 - gq), 0.0)


# ================================================================================================ the kings
def kings_world():
    rnd = random.Random(21)
    w = B.World(-90, 96, -3, 16, 0, 190)
    w.fill(-90, 96, -3, -1, 0, 190, "dirt")
    w.fill(-90, 96, -1, 0, 0, 190, "grass")
    # coarse earth and trodden patches
    for i in range(420):
        x, z = rnd.randrange(-60, 70), rnd.randrange(4, 120)
        w.set(x, -1, z, rnd.choice(["coarse_dirt", "coarse_dirt", "gravel", "path"]))
    # the old road from the guard to the first king's door
    for z in range(6, 34):
        for x in (2 + (z // 9) % 2, 3 + (z // 9) % 2, 4 + (z // 9) % 2):
            if rnd.random() < 0.8:
                w.set(x, -1, z, rnd.choice(["path", "path", "gravel", "coarse_dirt"]))
    # the first king's mound, highest, and the ring
    tops = mound(w, 4.5, 47.5, 13.5, 10.6, kerb=0.75, door=True, seed=4, cairn=(8, 46), plants=0.10,
                 shape=0.5)
    mound(w, -27.5, 66.5, 9.5, 6.4, kerb=0.6, door=True, seed=5, plants=0.08)
    mound(w, 33.5, 72.5, 10.5, 7.0, kerb=0.6, door=True, seed=6, plants=0.08)
    mound(w, -56.5, 104.5, 9.5, 5.6, kerb=0.5, seed=7)
    mound(w, 62.5, 112.5, 10.5, 6.0, kerb=0.5, seed=8)
    mound(w, -12.5, 128.5, 9.5, 5.0, kerb=0.5, seed=9)
    mound(w, 22.5, 150.5, 11.5, 5.2, kerb=0.4, seed=10)
    mound(w, -38.5, 162.5, 9.5, 4.2, kerb=0.4, seed=11)
    # standing stones on the field
    for (x, z, h, k) in ((-14, 36, 3, "stone"), (-15, 37, 2, "mossy_cobblestone"), (20, 30, 3, "stone"),
                         (-6, 58, 2, "mossy_stone_bricks"), (24, 52, 3, "stone"), (-40, 84, 3, "stone"),
                         (46, 92, 3, "stone"), (12, 24, 2, "mossy_cobblestone"), (-22, 24, 2, "stone"),
                         (34, 20, 3, "andesite"), (-30, 45, 2, "stone")):
        stone_post(w, x, z, h, k)
    # plants on the field
    for i in range(1500):
        x, z = rnd.randrange(-70, 80), rnd.randrange(3, 110)
        if w.get(x, 0, z) is None and w.get(x, -1, z)["name"] == "grass":
            w.set(x, 0, z, rnd.choice(["short_grass"] * 5 + ["tall_grass"] * 2 + ["poppy"] + ["dead_bush"]))
    return w, tops


KINGS_CAM = dict(f=720, xv=430, yv=232, eye=(0.0, 5.5, 0.0))
KINGS_L = (0.16, 0.66, 0.73)
KINGS_MOON = (482, 124)


CLIMBER_AT = (2.3, 44.6)
GUARD = dict(lantern=(-1.7, 18.6), knight1=(-3.7, 17.6), banner=(-5.9, 16.8), knight2=(0.5, 19.8))


class FigList(list):
    pass


def kings_figures(w, tops):
    figs = FigList()
    rnd = random.Random(5)
    # the Tide: a ragged line across the field between the guard and the mound, facing the guard, and more
    # standing round the mound's foot behind them
    spots = []
    for i in range(60):
        x = rnd.uniform(-22, 32)
        z = rnd.uniform(24.0, 31.0) + abs(x - 4) * 0.10
        if any(math.hypot(x - a, z - b) < 1.35 for (a, b) in spots):
            continue
        spots.append((x, z))
    for i in range(34):
        a = rnd.uniform(math.pi * 1.02, math.pi * 1.98)
        x, z = 4.5 + math.cos(a) * 15.6, 47.5 + math.sin(a) * 15.6 + rnd.uniform(-1, 1)
        if any(math.hypot(x - a_, z - b_) < 1.25 for (a_, b_) in spots):
            continue
        spots.append((x, z))
    for k, (x, z) in enumerate(spots):
        yaw = rnd.uniform(-24, 24) + (x + 2) * 0.6
        kind = rnd.random()
        boxes = tide_raider((x, 0.0, z), yaw=yaw, lean=rnd.uniform(-14, -4),
                            arms=rnd.choice(["forward", "forward", "one", "down"]),
                            trident=kind < 0.45, coral=kind < 0.22, seed=k)
        figs.append(SW.Figure(boxes, cast=False))
    # the climber on the summit, on its near shoulder, against the moon
    # she stands at the front edge of the top tier, so nothing of the mound hides her feet
    cx = CLIMBER_AT[0]
    col = int(math.floor(cx))
    top = max(v for (x, z), v in tops.items() if x == col)
    zf = min(z for (x, z), v in tops.items() if x == col and v == top)
    cz = zf + 0.42
    ty = top
    for (x, z), v in tops.items():          # clear the turf round her of plants
        if abs(x - col) <= 3 and abs(z - zf) <= 2:
            b = w.get(x, v, z)
            if b and b["model"] == "cross":
                w.set(x, v, z, None)
    figs.append(SW.Figure(climber((cx, float(ty), cz), yaw=-90.0, scale=0.9), cast=True))
    figs.climber = (cx, ty, cz)
    # the guard, held at the near edge of the Tide
    gb, lpos = lantern_pole((GUARD["lantern"][0], 0.0, GUARD["lantern"][1]), seed=2)
    figs.append(SW.Figure(gb, cast=True))
    figs.append(SW.Figure(knight((GUARD["knight1"][0], 0.0, GUARD["knight1"][1]), seed=3), cast=True))
    figs.append(SW.Figure(standard_bearer((GUARD["banner"][0], 0.0, GUARD["banner"][1]), seed=4), cast=True))
    figs.append(SW.Figure(knight((GUARD["knight2"][0], 0.0, GUARD["knight2"][1]), seed=6), cast=True))
    w.set(int(math.floor(lpos[0])), int(math.floor(lpos[1])), int(math.floor(lpos[2])), "light")
    return figs, lpos, ty


def stepped_halo(Hd, Wd, cx, cy, radii, k=1.0):
    """a stepped round glow on screen: 1 inside the smallest radius, falling a step at each larger one, the step
    edges dithered in a narrow band"""
    yy, xx = np.mgrid[0:Hd, 0:Wd]
    d = np.hypot(xx - cx, yy - cy) / k
    th = bayer(Hd, Wd)
    v = np.zeros((Hd, Wd))
    n = len(radii)
    for i, r in enumerate(sorted(radii, reverse=True)):
        m = (d < r - 1.5) | ((d < r + 1.5) & (th < (r + 1.5 - d) / 3.0))
        v[m] = (i + 1) / float(n)
    return v


def trace_kings(half=False, cache=None):
    import pickle, os
    if cache and os.path.exists(cache):
        with open(cache, "rb") as fh:
            return pickle.load(fh)
    t0 = time.time()
    w, tops = kings_world()
    figs, lpos, cty = kings_figures(w, tops)
    w.finish()
    k = 0.5 if half else 1.0
    Wd, Hd = int(960 * k), int(540 * k)
    cam = B.Camera(KINGS_CAM["f"] * k, KINGS_CAM["xv"] * k, KINGS_CAM["yv"] * k, KINGS_CAM["eye"])
    R = SW.render(w, cam, Wd, Hd, figures=figs, L=KINGS_L)
    R["lpos"], R["k"] = lpos, k
    ccx, cty_, ccz = figs.climber
    hx, hy = cam.project((ccx + 0.3, cty_ + 0.9, ccz))
    R["moon"] = (round(hx / k + 12), round(hy / k - 6))
    print("  traced in %.1fs (climber on y %d)" % (time.time() - t0, cty))
    if cache:
        with open(cache, "wb") as fh:
            pickle.dump(R, fh)
    return R


KINGS_LOOK = dict(amb=(0.034, 0.042, 0.086), moon=(0.50, 0.58, 0.84), warm=(0.95, 0.55, 0.24), fog="#5a6680",
                  fog_glow="#8d9bb4", d0=0.10, hs=0.9, steps=5, band=0.3,
                  layers=[(22.0, 0.0), (70.0, 0.10), (125.0, 0.24), (1e9, 0.40)])


def render_kings(half=False, cache=None, look=None):
    lk = dict(KINGS_LOOK, **(look or {}))
    t0 = time.time()
    R = trace_kings(half, cache)
    k = R["k"]
    Hd, Wd = R["hit"].shape
    cam = B.Camera(KINGS_CAM["f"] * k, KINGS_CAM["xv"] * k, KINGS_CAM["yv"] * k, KINGS_CAM["eye"])
    lpos = R["lpos"]
    fog = stepped_fog(R, ground_fog(R, lk["d0"], lk["hs"], seed=3), lk["layers"], lk["steps"], lk["band"])
    # the fog takes the moon: brighter toward the disc, in three steps
    mx, my = R["moon"][0] * k, R["moon"][1] * k
    md = np.array([(mx - cam.xv) / cam.f, -(my - cam.yv) / cam.f, 1.0])
    md /= np.linalg.norm(md)
    Dn = R["D"] / np.linalg.norm(R["D"], axis=2, keepdims=True)
    gl = np.clip(Dn @ md, 0, 1) ** 40 * (R["t"] > 70)          # only the far fog takes it, not the mound
    glq = quant(gl, 3, checker(Hd, Wd), 0.6)
    fog_col = lin(lk["fog"]) * (1 - glq[..., None]) + lin(lk["fog_glow"]) * glq[..., None]
    out, hit, ncodes = shade(R, amb_col=lk["amb"], sun_col=lk["moon"], warm_col=lk["warm"], fog=fog,
                             fog_col=fog_col, warm_curve=2.4, fog_q=0)
    # the lantern's glow in the fog round it, on whatever stands behind it
    lx, ly = cam.project(lpos)
    halo = stepped_halo(Hd, Wd, lx, ly, (16, 30, 48), k)
    behind = R["t"] > (lpos[2] - cam.eye[2]) - 0.6
    out = out + (halo * behind)[..., None] * lin("#3a2410")[None, None, :]
    sky = night_sky(Wd, Hd, cam.yv + 6 * k, (mx, my), scale=k)
    img = np.where(hit[..., None], srgb(out), sky).astype(np.uint8)
    name = "panel-kings" + ("-half" if half else "")
    Image.fromarray(img, "RGB").save(ART + "/" + name + ".png")
    print("  art/%s.png %dx%d, %d lit codes, %.1fs" % (name, Wd, Hd, ncodes, time.time() - t0))
    return R


if __name__ == "__main__":
    args = sys.argv[1:]
    half = "--half" in args
    cache = None
    for a_ in args:
        if a_.startswith("--cache="):
            cache = a_.split("=", 1)[1]
    if "kings" in args:
        render_kings(half, cache)
