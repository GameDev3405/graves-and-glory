"""The hill the commander stands on, built from blocks and drawn at GUI scale 1 (one pixel here is one GUI pixel).

Seen as the game shows it from eye height: the camera stands 3 blocks above the plateau and 25 blocks behind him.
A grass plateau runs out to the brink he stands at; on the left, further off across the drop, a crag of bedded
stone carries the camp (two wool tents, a dead oak, lanterns). Every block is placed by hand below (a typed plan
of the plateau, typed courses of the crag, typed lists for the camp, the plants and the props); every face shows a
hand-typed 16 x 16 texture from blocky.py; the renderer there only works out where each block lands and how much
light each texel gets.

Light: the low sun, and by night the moon, stands up and to the right, beyond the hill, as in the frame, so the
hill is backlit: the fronts of blocks (toward us) are in shadow, tops take the sun at a low angle, right-hand
sides take it full, and every upright thing casts a long shadow toward us and to the left, the commander's
included. Lanterns at the camp and on the brink give warm block light, spread from block to block as the game
spreads it; it shows after dark.

python title_hill.py   -> sprites/hill-day.png, sprites/hill-night.png (960 x 300, placed at GUI 0, 240)"""
import numpy as np
from PIL import Image

import blocky as B
from blocky import World, Camera, Box, AXIS_X, grid, solid, rot
from pixel import save

HILL_Y = 240                       # the layer's top row on the 960 x 540 GUI canvas
W, H = 960, 300

# The camera: at eye height, 3 blocks above the plateau, looking level (+z) with the lens shifted. The horizon is
# at y 208, by the haze line of the frame behind; the vanishing point sits right of the screen (x 1000), so every
# block left of it shows its front and its right-hand side, and the tops of everything below the eye. The
# commander's feet are 25 blocks out, where a block is 64 px (a skin pixel 4 px); the crag face is 100 blocks out,
# where a block is 16 px (a texel 1 px).
CAM = Camera(f=1600, xv=1000, yv=208, eye=(0.0, 3.0, 0.0))
FEET = (-5.2, 0.0, 25.0)           # the middle of his soles (world): GUI (667, 400)

# toward the sun (by night the moon): right, up, and away from us
SUN = np.array([0.55, 0.32, 0.77])
SUN = SUN / np.linalg.norm(SUN)

# ================================================================================================ the plateau
# The plateau as a plan seen from above: one row per depth z (far rows first), one column per x from -12 to -1.
# A letter is the block on top and its height:
#   0 grass, top at y 0       m grass one block down (the step under the version)
#   p dirt path at y 0        c coarse dirt at y 0       v gravel at y 0
#   s stone, a andesite: rock breaking out at the brink, top at y 0
#   . nothing (beyond the brink, or the drop on the left)
PLAN_X0 = -12
PLAN_Z0 = 28                       # the first row's z; each row below is one block nearer
PLAN = [
    #  x -12 .. -1
    "..........00",   # z 28  the lip rises to the right
    ".........000",   # z 27
    ".........000",   # z 26
    "..as00p00000",   # z 25  the brink: he stands on its near edge, at x -5.2
    "..s00cp00000",   # z 24
    "...000pc0000",   # z 23
    "....0cp00000",   # z 22
    "....00pv0000",   # z 21
    "....00p00000",   # z 20
    ".....0vp0000",   # z 19
    ".....00p0000",   # z 18
    ".....00pc0mm",   # z 17  the step down at the right, under the version
    "......0p00mm",   # z 16
    "......0p00mm",   # z 15
    "......0p00mm",   # z 14
]
TOP = {"0": ("grass", 0), "m": ("grass", -1), "p": ("path", 0), "c": ("coarse_dirt", 0), "v": ("gravel", 0),
       "s": ("stone", 0), "a": ("andesite", 0)}
# plants on the plateau: (kind, x, z), each on top of its column; kept clear of him, of his shadow and of the path
PLANTS = [("short_grass", -9, 24), ("short_grass", -1, 27), ("short_grass", -3, 26), ("allium", -2, 28),
          ("short_grass", -4, 25)]
# a lantern on a fence post at the lip, right of him: (name, x, y, z)
BRINK = [("fence", -2, 0, 26), ("lantern", -2, 1, 26)]

# ================================================================================================ the crag
# The crag across the valley on the left, where the camp is: 100 blocks out, so a block is 16 px and a texel 1 px.
# Its face is typed course by course from the top, one letter per block from x -64 to -42; it runs back from z 100
# to 107 (108 under the shoulder). Its crest steps down once: the left shoulder (x -64 to -55) stands a block
# higher. G grass, D dirt, K coarse dirt, S stone, A andesite, C cobblestone, . nothing. The menu sits on plain
# dirt and stone (x -61 to -50, courses -9 to -17); the beds of andesite and cobblestone run beside and below it.
CRAG_X0, CRAG_Z0 = -64, 100
CRAG_BACK = {x: (109 if x <= -55 else 108) for x in range(-64, -41)}    # where the crest ends, per column
CRAG = {
    -7:  "GGGGGGGGGG.............",
    -8:  "DDDDDDDDDDGGGGGGGGGGGGG",
    -9:  "DDKDDDDDDDDDDDDDDKDDDKD",
    -10: "DDDDDDDDDDDDDDDSDDDSDSS",
    -11: "SSSSSSSSSSSSSSSSSSSSSSS",
    -12: "SSSSSSSSSSSSSSSSSSSAAAS",
    -13: "AASSSSSSSSSSSSSSSSAAAAA",
    -14: "SSSSSSSSSSSSSSSSSSSSSSA",
    -15: "SSSSSSSSSSSSSSSSSSSSSSS",
    -16: "SAASSSSSSSSSSSSSSSSSCCS",
    -17: "SSSSSSSSSSSSSSSSSSSCCCS",
    -18: "AAAAAAAAAAAAAAAAAAAAAAA",
    -19: "CCSSCCCCSSSCCCSSCCCSSCC",
    -20: "SSSSSSSSSSSSSSSSSSSSSSS",
    -21: "SSSSSSSSSSSSSSSSSSSSSSS",
}
CRAG_KEY = {"G": "grass", "D": "dirt", "K": "coarse_dirt", "S": "stone", "A": "andesite", "C": "cobblestone"}
# relief on the face, right of the menu: blocks standing proud (z 99) and a hollow (a block taken out)
CRAG_RELIEF = [("stone", -46, -12, 99), ("andesite", -46, -13, 99), ("andesite", -45, -14, 99),
               ("stone", -44, -14, 99), (None, -44, -11, 100), (None, -43, -16, 100)]

# ================================================================================================ the camp
# on the crag, all typed: (name, x, y, z[, axis]). The ground is y -6 on the shoulder and y -7 below it. The tents
# and their lanterns stand back from the edge, so their light stays on the crest and off the face below.
CAMP = [
    # the Dawn tent: 3 wide, 2 high, 3 deep, white under a teal ridge, as players build a tent from wool; a lantern
    # stands in its open door; a fence for its pole on the ridge
    ("white_wool", -51, -7, 103), ("lantern", -50, -7, 103), ("white_wool", -49, -7, 103),
    ("white_wool", -51, -7, 104), ("white_wool", -49, -7, 104),
    ("white_wool", -51, -7, 105), ("white_wool", -50, -7, 105), ("white_wool", -49, -7, 105),
    ("dawn_wool", -50, -6, 103), ("dawn_wool", -50, -6, 104), ("dawn_wool", -50, -6, 105),
    ("fence", -50, -5, 103),
    # a second tent on the shoulder, the colours turned about: teal under a white ridge; a lantern in its door too
    ("dawn_wool", -59, -6, 104), ("lantern", -58, -6, 104), ("dawn_wool", -57, -6, 104),
    ("dawn_wool", -59, -6, 105), ("dawn_wool", -57, -6, 105),
    ("dawn_wool", -59, -6, 106), ("dawn_wool", -58, -6, 106), ("dawn_wool", -57, -6, 106),
    ("white_wool", -58, -5, 104), ("white_wool", -58, -5, 105), ("white_wool", -58, -5, 106),
    # a lantern on a post between the tents
    ("fence", -54, -7, 103), ("lantern", -54, -6, 103),
    # the dead oak at the front of the shoulder: four logs; two stripped logs laid sideways for the boughs where the
    # bark has come away; the bare twigs are fences joined to them and to each other, as players build them
    ("oak_log", -62, -6, 101), ("oak_log", -62, -5, 101), ("oak_log", -62, -4, 101), ("oak_log", -62, -3, 101),
    ("stripped_oak", -63, -4, 101, AXIS_X), ("stripped_oak", -61, -3, 101, AXIS_X),
    ("fence", -63, -3, 101), ("fence", -64, -3, 101), ("fence", -64, -2, 101),
    ("fence", -61, -2, 101), ("fence", -60, -2, 101), ("fence", -60, -1, 101),
    ("fence", -62, -2, 101), ("fence", -62, -1, 101),
    # dead bushes on the crest
    ("dead_bush", -64, -6, 100), ("dead_bush", -53, -7, 100), ("dead_bush", -46, -7, 101),
]
# block light comes from the lanterns themselves (blocky.block_light), as the game spreads it


def build():
    w = World(-66, 2, -22, 8, 10, 112)
    for r, row in enumerate(PLAN):
        z = PLAN_Z0 - r
        for i, ch in enumerate(row):
            if ch == ".":
                continue
            x = PLAN_X0 + i
            name, top = TOP[ch]
            for y in range(-12, top):
                w.set(x, y, z, name if y == top - 1 else "dirt" if y >= top - 3 else "stone")
    for (kind, x, z) in PLANTS:
        top = TOP[PLAN[PLAN_Z0 - z][x - PLAN_X0]][1]
        w.set(x, top, z, kind)
    for (name, x, y, z) in BRINK:
        w.set(x, y, z, name)
    for y, row in CRAG.items():
        for i, ch in enumerate(row):
            if ch == ".":
                continue
            for z in range(CRAG_Z0, CRAG_BACK[CRAG_X0 + i]):
                w.set(CRAG_X0 + i, y, z, CRAG_KEY[ch])
    for (name, x, y, z) in CRAG_RELIEF:
        w.set(x, y, z, name)
    for item in CAMP:
        name, x, y, z = item[:4]
        w.set(x, y, z, name, item[4] if len(item) > 4 else 0)
    w.finish()
    return w


# ================================================================================================ props
# The Dawn shield, dropped and half sunk in the turf, leaning back; the broken spear: its butt end stuck in the
# ground with a teal rag tied below the break, its head lying on the trodden earth by the path. Each is an entity
# box with hand-typed faces.
SHIELD_FACE = grid([   # the Dawn's sun on teal, as the game puts a banner's pattern on a shield, in an iron rim
    "IiiiiiiiI",
    "iTTTTTTTi",
    "iTTTeTTTi",
    "iTeTETeTi",
    "iTTEEETTi",
    "ieEEFEEei",
    "iTTEEETTi",
    "iTeTETeTi",
    "iTTTeTTTi",
    "iTTTTTTTi",
    "iSTTTTTTi",
    "iTTTTTTSi",
    "iTTTTTTTi",
    "IiiiiiiiI",
], {"i": "i2", "I": "i1", "T": "t2", "S": "t1", "e": "e1", "E": "e2", "F": "e3"})
OAK_EDGE = solid("k1")
PROPS = [
    # the shield fallen back against the turf, its foot sunk in the grass
    Box((-7.9, 0.24, 22.4), (0.26, 0.42, 0.03), {(2, -1): SHIELD_FACE, "all": OAK_EDGE},
        rot(yaw=24, pitch=40, roll=-18)),
    # the butt end, stuck in the ground, leaning left; the splintered end is pale
    Box((-2.72, 0.40, 23.4), (0.03, 0.48, 0.03), {"all": grid(["w", "k", "k", "k", "k", "k", "k", "k"],
                                                            {"w": "w3", "k": "k2"})}, rot(roll=22)),
    # the rag, tied below the break
    Box((-2.86, 0.62, 23.36), (0.07, 0.11, 0.01), {"all": grid(["tt", "Tt", "tT", "T."], {"t": "t2", "T": "t3"})},
        rot(roll=10, yaw=12)),
    # the head and a stub of shaft, lying on the earth
    Box((-3.75, 0.03, 22.45), (0.22, 0.025, 0.025), {"all": grid(["kkkkw"], {"k": "k2", "w": "w3"})},
        rot(yaw=20)),
    Box((-4.03, 0.035, 22.36), (0.08, 0.02, 0.05), {"all": grid(["iI", "Ii"], {"i": "i2", "I": "i3"})},
        rot(yaw=20)),
]


# ================================================================================================ rendering
NL = 16                            # sky and sun light levels
NB = 6                             # block light levels
A_SKY, A_SUN = 0.40, 1.0           # how much sky light and sun light a face can take
VMAX = 0.85
AMB = {(0, 1): 0.55, (0, -1): 0.55, (1, 1): 0.75, (1, -1): 0.35, (2, 1): 0.62, (2, -1): 0.62}   # (axis, sign)


def sample(world, P, nrm, bid, cell):
    """the texture tone at each block hit, and the texel centre (world) its light is read at"""
    n = len(P)
    r = np.arange(n)
    f = np.clip(P - cell, 0, 1 - 1e-9)
    ax = np.argmax(np.abs(nrm), 1)
    s = np.sign(nrm[r, ax]).astype(int)
    fx, fy, fz = f[:, 0], f[:, 1], f[:, 2]
    col = np.where(ax == 1, fx, np.where(ax == 2, np.where(s < 0, fx, 1 - fx), np.where(s > 0, fz, 1 - fz)))
    row = np.where(ax == 1, np.where(s > 0, 1 - fz, fz), 1 - fy)
    cross = np.isin(bid, B.CROSS_IDS)
    col = np.where(cross, fx, col)
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
            col_dir = 2 if a == 0 else 0             # the world axis a face's columns run along
            cc, rr = (row[k], col[k]) if (col_dir == log_axis and a != log_axis) else (col[k], row[k])
        else:
            name = b["top"] if (a == 1 and s[k] > 0) else b["bottom"] if a == 1 else b["side"]
            cc, rr = col[k], row[k]
        rows, key = B.TEX[name]
        tone[k] = key[rows[rr][cc]]
    return tone, Pc


def light(world, sky, blk, Pc, nrm, full, cell, casters):
    """light level (sky and sun) and block light level per hit. full: the hit is on a full block, which takes the
    game's smooth light and corner shading; anything else reads the sky light of the cell it is in"""
    n = len(Pc)
    r = np.arange(n)
    ax = np.argmax(np.abs(nrm), 1)
    s = np.sign(nrm[r, ax]).astype(int)
    ao = np.ones(n)
    lv = B.cell_sky(world, sky, Pc + nrm * 0.02)
    if full.any():
        fs = Pc - cell
        ua = np.where(ax == 0, 2, 0)
        va = np.where(ax == 1, 2, 1)
        fu, fv = fs[r, ua][full], fs[r, va][full]
        ao[full] = B.corner_ao(world, cell[full], nrm[full], fu, fv)
        lv[full] = B.smooth_sky(world, sky, cell[full], nrm[full], fu, fv)
    ndl = np.clip(nrm @ SUN, 0, None)
    lit = np.zeros(n, bool)
    up = ndl > 0
    if up.any():
        O = Pc[up] + nrm[up] * 2e-3 + SUN * 2e-3
        Dd = np.broadcast_to(SUN, (int(up.sum()), 3)).copy()
        blocked = B.trace(world, O, Dd, shadow=True)
        if casters:
            blocked |= B.boxes_hit(O, Dd, casters)[2] >= 0
        lit[up] = ~blocked
    amb = np.array([AMB[(int(a), int(g))] for a, g in zip(ax, s)])
    v = amb * (0.5 + 0.5 * ao) * B.brightness(lv) ** 3 * A_SKY + ndl * lit * A_SUN
    lvl = np.clip(np.round(v / VMAX * (NL - 1)), 0, NL - 1).astype(int)
    # block light, as the game spreads it from the lanterns: smooth on full faces, the cell's own elsewhere; the
    # game's light curve, squared as a shader pack darkens it, turns the level into how much warm light the face
    # takes (a lantern's glow reaches about five blocks)
    lb = B.cell_sky(world, blk, Pc + nrm * 0.02, outside=0)
    if full.any():
        lb[full] = B.smooth_sky(world, blk, cell[full], nrm[full], fu, fv, outside=0)
    blv = np.clip(np.round(B.brightness(lb) ** 2 * (NB - 1)), 0, NB - 1).astype(int)
    return lvl, blv


def render(world, cam, casters=(), props=()):
    D = cam.rays(0, HILL_Y, W, H)
    n = len(D)
    sky = B.sky_light(world)
    blkl = B.block_light(world)
    t, nrm, bid, cell = B.trace(world, cam.eye, D)
    if props:
        tp, nrp, bip, tonep, _ = B.boxes_hit(cam.eye, D, list(props))
    else:
        tp = np.full(n, np.inf)
    code = np.full(n, "", dtype=object)
    blk = np.isfinite(t) & ~(tp < t)
    if blk.any():
        P = cam.eye + D[blk] * t[blk, None]
        tone, Pc = sample(world, P, nrm[blk], bid[blk], cell[blk])
        lvl, blv = light(world, sky, blkl, Pc, nrm[blk], B.FULL[bid[blk]], cell[blk],
                         list(casters) + list(props))
        code[blk] = [f"{tn}.{l}" + (f"w{b}" if b else "") for tn, l, b in zip(tone, lvl, blv)]
    prp = np.isfinite(tp) & (tp < t)
    if prp.any():
        P = cam.eye + D[prp] * tp[prp, None]
        cl = np.floor(P).astype(np.int64)
        lvl, blv = light(world, sky, blkl, P, nrp[prp], np.zeros(int(prp.sum()), bool), cl, list(casters))
        code[prp] = [f"{tn}.{l}" + (f"w{b}" if b else "") for tn, l, b in zip(tonep[prp], lvl, blv)]
    return code.reshape(H, W)


# ================================================================================================ palettes
# light ramps: hue-shifted, violet in shadow and gold in the sun by day; indigo and cool silver by night
DAY_SKY = B.ramp([(0, (0.025, 0.02, 0.045)), (4, (0.12, 0.10, 0.18)), (6, (0.18, 0.15, 0.23)),
                  (8, (0.26, 0.21, 0.26)), (10, (0.36, 0.28, 0.27)), (11, (0.43, 0.33, 0.29)),
                  (13, (0.68, 0.52, 0.39)), (14, (0.90, 0.69, 0.49)), (15, (1.10, 0.84, 0.57))], NL)
NIGHT_SKY = B.ramp([(0, (0.012, 0.012, 0.03)), (4, (0.035, 0.042, 0.10)), (6, (0.05, 0.06, 0.14)),
                    (8, (0.075, 0.09, 0.20)), (11, (0.12, 0.145, 0.31)), (14, (0.21, 0.25, 0.50)),
                    (15, (0.27, 0.31, 0.60))], NL)
DAY_WARM = B.ramp([(0, (0, 0, 0)), (5, (0.10, 0.06, 0.03))], NB)
NIGHT_WARM = B.ramp([(0, (0, 0, 0)), (1, (0.10, 0.05, 0.02)), (3, (0.45, 0.24, 0.08)), (5, (1.20, 0.66, 0.26))], NB)
EMIT = {"f0", "f1", "f2", "f3"}    # flame shows its own colour whatever the light


def palette(codes, tod):
    sky, warm = (DAY_SKY, DAY_WARM) if tod == "day" else (NIGHT_SKY, NIGHT_WARM)
    pal = {}
    for c in set(np.asarray(codes).ravel()) - {""}:
        tn, rest = c.split(".")
        l, _, b = rest.partition("w")
        if tn in EMIT:
            pal[c] = B.ALBEDO[tn]
            continue
        m = np.array(sky[int(l)]) + np.array(warm[int(b) if b else 0])
        pal[c] = B.lit_hex(B.ALBEDO[tn], m)
    return pal


def to_image(codes, pal):
    h, w = codes.shape
    im = np.zeros((h, w, 4), np.uint8)
    lut = {c: B.hexrgb(v) for c, v in pal.items()}
    for y in range(h):
        for x in range(w):
            c = codes[y, x]
            if c:
                im[y, x, :3] = lut[c]
                im[y, x, 3] = 255
    return Image.fromarray(im, "RGBA")


HUD_COVER = [((12, 365), (327, 540)), ((786, 440), (948, 540))]   # the source frame's HUD panels under the hill
MENU_ZONE = (16, 388, 210, 520)
VERSION_ZONE = (840, 492, 956, 540)
# calm ground: under the menu and the version every pixel stays dark enough that the dimmest text there (the
# version's #afb9bc) keeps 4.5:1 against it, and the ground is flat enough that no texel pattern competes with type
CALM_MAX, CALM_STD = 0.066, 0.01


def luminance(rgb):
    c = np.asarray(rgb, float) / 255.0
    c = np.where(c <= 0.03928, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)
    return 0.2126 * c[..., 0] + 0.7152 * c[..., 1] + 0.0722 * c[..., 2]


def check_calm(im):
    a = np.asarray(im.convert("RGB"))
    out = []
    for name, (x0, y0, x1, y1) in (("menu", MENU_ZONE), ("version", VERSION_ZONE)):
        L = luminance(a[y0 - HILL_Y:y1 - HILL_Y, x0:x1])
        assert L.max() <= CALM_MAX and L.std() <= CALM_STD, (name, L.max(), L.std())
        out.append("%s max %.3f" % (name, L.max()))
    return ", ".join(out)


def check_cover(codes):
    miss = []
    for (x0, y0), (x1, y1) in HUD_COVER:
        for y in range(y0, y1):
            for x in range(x0, x1 + 1):
                if not codes[y - HILL_Y, x]:
                    miss.append((x, y))
    return miss


def casters():
    """the commander and his banner, for the shadow he casts on the hill"""
    try:
        from title_commander import shadow_boxes
        return shadow_boxes()
    except ImportError:
        return []


if __name__ == "__main__":
    import time
    t0 = time.time()
    world = build()
    codes = render(world, CAM, casters(), PROPS)
    print("rendered in %.1fs, %d codes" % (time.time() - t0, len(set(codes.ravel())) - 1))
    miss = check_cover(codes)
    assert not miss, "the hill leaves %d HUD pixels uncovered, first %s" % (len(miss), miss[:5])
    calm = []
    for tod in ("day", "night"):
        im = to_image(codes, palette(codes, tod))
        calm.append("%s: %s" % (tod, check_calm(im)))
        save(im, "hill-%s.png" % tod)
    print("hill written, HUD covered; calm ground (luminance) " + "; ".join(calm))
