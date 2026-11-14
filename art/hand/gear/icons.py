"""Relic icons (docs/superpowers/plans/2026-11-03-gear-art.md, Task 8): each relic as the object it is, on a shared
dark round plate, 32 x 32 with a clear 1 px margin. Shapes are drawn without anti-aliasing and lit from the top left:
a layer's texels nearest the light step up its ramp, the far edge steps down, and one ink line runs round the whole
object. The gear item pictures are icon_render.py's (smooth real-model renders); cube_render here draws the weapons.

  python art/hand/gear/icons.py relics <out_dir>        one PNG per relic id
  python art/hand/gear/icons.py sheet <out.png>         the review sheet: 1x and 3x on every slot border colour
  (the gear item pictures: python art/hand/gear/icon_render.py sheet <out.png>)"""
import os, sys
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "common"))
from handlib import ramp, hx, LEATH, GOLD
import render

S = 32
INK = hx("0d0b10")
PLATE = ramp("15141a", "1e1c24", "29262f", "36323c", "4a4550")
IRON = ramp("1a1b20", "2e2f36", "4a4b52", "6a6a70", "8f8e8c", "b7b4ab", "dcd8cc")
BRASS = ramp("2a1a0a", "553712", "8a5d1e", "b8862e", "dcb24c", "f4dc8a")
WOOD = ramp("1f130b", "3a2414", "5a3a1f", "7c5530", "a07446")
BONE = ramp("3a3428", "6a6050", "9c9278", "c9c0a2", "ece4c8")
BLOOD = ramp("2a0608", "54101a", "8a1a22", "c03030", "e4605a")
WAX = ramp("2c0a0c", "5a1418", "8c2224", "b83a30", "d86a52")
EMBER = ramp("5a1a08", "9a3510", "d4601a", "f39a3a", "ffd27a", "fff2c8")
GHOST = ramp("16282a", "2a4a46", "4a7a6e", "7eb09a", "b8dcc4", "e8f6ea")   # grave-light: pale sea-green, not a House ramp
AMBER = ramp("3a1a04", "7a3c08", "c06a10", "eaa02a", "ffd060")
CLOTH = ramp("2a2620", "4a4234", "6e6450", "968a6c", "bcb08e")
FEATH = ramp("2a2a30", "4a4a54", "74747e", "a8a6a8", "dedcd4")
DRAUGHT = ramp("12301a", "1e5a2c", "2e8a3e", "5ab85a", "a6e08a", "e4f8c8")

class Icon:
    def __init__(s):
        s.img = Image.new("RGBA", (S, S), (0, 0, 0, 0)); s.union = set()

    def layer(s, R, base, draw, light=True, outline=True):
        """draw(ImageDraw) paints a 1-bit mask; it is filled from ramp R at base, lit top-left, shaded bottom-right."""
        m = Image.new("1", (S, S), 0); draw(ImageDraw.Draw(m))
        cells = {(x, y) for y in range(S) for x in range(S) if m.getpixel((x, y))}
        for x, y in cells:
            v = base
            if light:
                if (x, y - 1) not in cells or (x - 1, y) not in cells: v += 1
                if (x, y + 1) not in cells or (x + 1, y) not in cells: v -= 1
            s.img.putpixel((x, y), R[max(0, min(len(R) - 1, v))])
        if outline: s.union |= cells
        return cells

    def px(s, pts, c):
        for x, y in pts: s.img.putpixel((x, y), c)

    def done(s):
        ring = {(x + dx, y + dy) for x, y in s.union for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))} - s.union
        for x, y in ring:
            if 0 <= x < S and 0 <= y < S and s.img.getpixel((x, y))[3] == 0: s.img.putpixel((x, y), INK)   # never over painted detail
        return s.img

def plate():
    """The shared dark round plate: a dished disc, its rim lit top-left."""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0)); c = 15.5
    for y in range(S):
        for x in range(S):
            d = ((x - c) ** 2 + (y - c) ** 2) ** 0.5
            if d > 15.0: continue
            if d > 14.0: v = INK
            elif d > 12.6: v = PLATE[4] if (x + y) < 28 else (PLATE[1] if (x + y) > 34 else PLATE[3])
            else: v = PLATE[1] if (x + y) < 26 else PLATE[2]
            img.putpixel((x, y), v)
    return img

def on_plate(icon):
    out = plate(); out.alpha_composite(icon); return out

# ---- the twelve relics ----
def ward_of_the_wall():          # stand_fast: a squat iron-rimmed heater of wall-stone
    i = Icon()
    i.layer(IRON, 3, lambda d: d.polygon([(8, 6), (23, 6), (23, 17), (19, 23), (15, 26), (12, 23), (8, 17)], fill=1))
    i.layer(BONE, 2, lambda d: d.polygon([(10, 8), (21, 8), (21, 16), (18, 21), (15, 23), (13, 21), (10, 16)], fill=1), outline=False)
    for y in (11, 15, 19): i.px([(x, y) for x in range(10, 22) if not (y == 19 and (x < 13 or x > 18))], BONE[1])   # mortar courses
    i.px([(13, 8), (13, 9), (13, 10), (18, 12), (18, 13), (18, 14), (14, 16), (14, 17), (14, 18), (17, 20)], BONE[1])
    i.px([(9, 7), (22, 7)], IRON[6]); i.px([(15, 6), (16, 6)], IRON[5])
    return i.done()

def reavers_fang():              # bloodrush: a hooked fang on a cord, blood at the tip
    i = Icon()
    i.layer(LEATH, 2, lambda d: d.line([(9, 6), (15, 10), (22, 6)], fill=1, width=1), light=False)
    i.layer(BONE, 3, lambda d: d.polygon([(12, 10), (19, 10), (20, 14), (19, 19), (16, 24), (13, 27), (14, 21), (13, 15)], fill=1))
    i.layer(BRASS, 3, lambda d: d.rectangle([12, 9, 19, 11], fill=1))
    i.px([(14, 22), (14, 23), (13, 24), (13, 25), (14, 21)], BLOOD[3]); i.px([(13, 26), (13, 27)], BLOOD[2])
    i.px([(12, 28)], BLOOD[3])
    return i.done()

def hunters_eye():               # mark_prey: an amber eye set in a brass ring
    i = Icon()
    i.layer(BRASS, 3, lambda d: d.ellipse([6, 8, 25, 23], fill=1))
    i.layer(PLATE, 0, lambda d: d.ellipse([9, 11, 22, 20], fill=1), light=False, outline=False)
    i.layer(AMBER, 2, lambda d: d.ellipse([10, 12, 21, 19], fill=1), outline=False)
    i.px([(15, y) for y in range(12, 20)] + [(16, y) for y in range(12, 20)], INK)
    i.px([(12, 13), (13, 13)], AMBER[4])
    i.px([(15, 4), (16, 4), (15, 5), (16, 5), (15, 6), (16, 6), (15, 7), (16, 7)], BRASS[4])   # the hanging loop
    i.px([(14, 4), (17, 4)], BRASS[2])
    return i.done()

def muster_seal():               # rally_horn: a wax seal on a strip of cloth, a pressed crown of three points
    i = Icon()
    i.layer(CLOTH, 3, lambda d: d.polygon([(9, 18), (13, 18), (12, 27), (10, 25), (8, 27)], fill=1))
    i.layer(CLOTH, 2, lambda d: d.polygon([(18, 18), (22, 18), (23, 27), (21, 25), (19, 27)], fill=1))
    i.layer(WAX, 3, lambda d: d.polygon([(10, 6), (15, 4), (21, 6), (24, 11), (23, 17), (19, 21), (12, 21), (8, 17), (7, 11)], fill=1))
    i.layer(WAX, 2, lambda d: d.ellipse([10, 7, 21, 18], fill=1), light=False, outline=False)
    i.px([(12, 15), (13, 15), (14, 15), (15, 15), (16, 15), (17, 15), (18, 15), (19, 15),
          (12, 14), (12, 13), (12, 11), (15, 14), (15, 13), (15, 12), (15, 10), (19, 14), (19, 13), (19, 11)], WAX[0])
    i.px([(12, 12), (15, 11), (19, 12)], WAX[1]); i.px([(11, 7), (12, 6)], WAX[4])
    return i.done()

def gravebloom_censer():         # grave_bloom: a censer swung on its chain, grave-light pouring out and opening into a flower
    i = Icon()
    i.layer(IRON, 4, lambda d: d.ellipse([9, 2, 11, 4], outline=1))                                 # the ring it swings from
    i.layer(IRON, 3, lambda d: d.line([(10, 5), (10, 8)], fill=1, width=2)); i.px([(10, 6), (11, 7)], IRON[1])   # the chain, link by link
    i.layer(IRON, 4, lambda d: d.ellipse([7, 9, 14, 13], fill=1))                                  # the domed lid
    i.layer(IRON, 3, lambda d: d.polygon([(6, 13), (15, 13), (15, 18), (13, 21), (8, 21), (6, 18)], fill=1))   # the bowl
    i.layer(IRON, 2, lambda d: d.rectangle([8, 21, 13, 22], fill=1))                               # the foot
    i.px([(8, 15), (10, 15), (12, 15), (9, 17), (11, 17), (13, 17)], GHOST[5])                     # light through the piercings
    i.layer(GHOST, 3, lambda d: d.line([(15, 12), (17, 11), (18, 13), (20, 14), (21, 12), (20, 10)], fill=1, width=2))   # the smoke
    i.layer(GHOST, 4, lambda d: d.ellipse([20, 3, 23, 6], fill=1)); i.layer(GHOST, 4, lambda d: d.ellipse([24, 6, 27, 9], fill=1))
    i.layer(GHOST, 4, lambda d: d.ellipse([20, 9, 23, 12], fill=1)); i.layer(GHOST, 4, lambda d: d.ellipse([16, 6, 19, 9], fill=1))
    i.layer(AMBER, 3, lambda d: d.ellipse([20, 6, 23, 9], fill=1))                                # the bloom's heart
    return i.done()

def forward_cache():             # forward_supply: a strapped supply crate with a lantern-hook stake
    i = Icon()
    i.layer(WOOD, 3, lambda d: d.rectangle([7, 12, 24, 25], fill=1))
    i.layer(WOOD, 4, lambda d: d.polygon([(7, 12), (11, 8), (27, 8), (24, 12)], fill=1))
    i.layer(WOOD, 2, lambda d: d.polygon([(24, 12), (27, 8), (27, 21), (24, 25)], fill=1))
    i.px([(x, 18) for x in range(8, 24)], WOOD[1])
    for x in (11, 20): i.px([(x, y) for y in range(12, 26)], LEATH[2])
    i.px([(11, 18), (20, 18)], IRON[5]); i.px([(9, 14), (22, 14), (9, 23), (22, 23)], IRON[4])
    return i.done()

def chargers_spur():             # dash: a rider's boot at full stride, the spur's star rowel at the heel, speed streaks behind
    i = Icon()
    i.layer(LEATH, 3, lambda d: d.polygon([(12, 5), (19, 5), (19, 18), (25, 20), (26, 23), (12, 23)], fill=1))   # shaft and foot
    i.layer(LEATH, 4, lambda d: d.rectangle([11, 4, 20, 6], fill=1))                               # the turned-down cuff
    i.layer(LEATH, 1, lambda d: d.rectangle([12, 24, 26, 25], fill=1))                             # the sole
    i.layer(IRON, 4, lambda d: d.line([(12, 18), (9, 19)], fill=1, width=2))                       # the spur's neck
    i.layer(BRASS, 4, lambda d: d.polygon([(7, 15), (8, 18), (11, 19), (8, 20), (7, 23), (6, 20), (4, 19), (6, 18)], fill=1))   # the rowel
    i.px([(7, 19)], INK); i.px([(13, 18), (18, 18)], BRASS[4])                                     # the strap's buckle line
    i.px([(x, 18) for x in range(14, 18)], LEATH[1])
    for y, x0 in ((8, 5), (11, 4), (14, 6)): i.px([(x, y) for x in range(x0, x0 + 5)], IRON[5 - (y - 8) // 3])   # speed streaks
    return i.done()

def gatebreakers_shove():        # shield_bash: an armoured fist, knuckle plates forward
    i = Icon()
    i.layer(IRON, 2, lambda d: d.polygon([(9, 18), (17, 18), (19, 26), (8, 26)], fill=1))   # cuff
    i.layer(IRON, 3, lambda d: d.rounded_rectangle([7, 7, 23, 19], radius=3, fill=1))
    for y in (9, 13): i.px([(x, y) for x in range(9, 22)], IRON[1])
    i.px([(12, y) for y in range(8, 18)] + [(17, y) for y in range(8, 18)], IRON[2])
    i.layer(IRON, 4, lambda d: d.polygon([(19, 12), (25, 11), (26, 16), (21, 18)], fill=1))   # thumb plate
    i.px([(10, 8), (14, 8), (19, 8)], IRON[6]); i.px([(10, 24), (15, 24)], IRON[4])
    return i.done()

def lantern_of_mercy():          # heal_pulse: a hooded lantern, warm light in its panes
    i = Icon()
    i.layer(IRON, 3, lambda d: d.arc([12, 2, 19, 9], 180, 360, fill=1))
    i.layer(IRON, 4, lambda d: d.polygon([(11, 9), (20, 9), (22, 12), (9, 12)], fill=1))
    i.layer(IRON, 2, lambda d: d.rectangle([10, 12, 21, 24], fill=1))
    i.layer(EMBER, 3, lambda d: d.rectangle([12, 14, 19, 22], fill=1), outline=False)
    i.px([(15, y) for y in range(14, 23)] + [(16, y) for y in range(14, 23)], IRON[2])
    i.px([(13, 18), (14, 18), (17, 18), (18, 18), (13, 19), (18, 19)], EMBER[5])
    i.layer(IRON, 3, lambda d: d.rectangle([9, 24, 22, 26], fill=1))
    return i.done()

def hunters_volley():            # volley: three arrows fanned upward, red-dyed fletching
    i = Icon()
    shafts = ((11, 27, 7, 9), (15, 28, 15, 7), (19, 27, 24, 9))
    for a in shafts:
        i.layer(WOOD, 3, lambda d, a=a: d.line([(a[0], a[1]), (a[2], a[3])], fill=1), light=False)
    for (hx_, hy) in ((7, 7), (15, 5), (24, 7)):
        i.layer(IRON, 4, lambda d, h=(hx_, hy): d.polygon([(h[0], h[1] - 3), (h[0] + 2, h[1] + 1), (h[0] - 2, h[1] + 1)], fill=1))
    for (x0, y0, x1, y1) in shafts:   # two vanes hugging each shaft near its nock
        t = [(x0 + (x1 - x0) * k / (y0 - y1), y0 - k) for k in (2, 3, 4, 5)]
        i.px([(round(x) - 1, round(y)) for x, y in t] + [(round(x) + 1, round(y)) for x, y in t], BLOOD[3])
        i.px([(round(t[0][0]) - 1, round(t[0][1])), (round(t[0][0]) + 1, round(t[0][1]))], BLOOD[2])
    return i.done()

def battle_cry():                # war_cry: a curved war horn, mouthpiece low left, the bell flaring up right, brass bands
    i = Icon()
    def curve(t): return (6 + 18 * t, 25 - 6 * t - 10 * t * t)          # spine of the horn
    def half(t): return 1.2 + 4.8 * t ** 1.6                           # half-width grows to the bell
    up, dn = [], []
    for k in range(13):
        t = k / 12; x, y = curve(t); w = half(t)
        up.append((x - w * 0.55, y - w * 0.85)); dn.append((x + w * 0.55, y + w * 0.85))
    i.layer(BONE, 3, lambda d: d.polygon(up + dn[::-1], fill=1))
    bx, by = curve(0.93)
    i.layer(BONE, 0, lambda d: d.ellipse([bx - 2, by - 4, bx + 2, by + 3], fill=1), light=False, outline=False)   # the bell's dark throat
    for t in (0.35, 0.7):
        x, y = curve(t); w = half(t)
        i.layer(BRASS, 3, lambda d, a=(x, y, w): d.line([(a[0] - a[2] * 0.6 - 1, a[1] - a[2] * 0.9), (a[0] + a[2] * 0.6 + 1, a[1] + a[2] * 0.9)], fill=1, width=2))
    i.layer(BRASS, 4, lambda d: d.rectangle([4, 24, 6, 26], fill=1))                  # mouthpiece
    return i.done()

def second_wind():               # iron_skin: a stoppered phial of green-gold draught
    i = Icon()
    i.layer(DRAUGHT, 2, lambda d: d.ellipse([8, 12, 23, 27], fill=1))
    i.layer(DRAUGHT, 2, lambda d: d.rectangle([13, 7, 18, 13], fill=1))
    i.layer(FEATH, 4, lambda d: d.ellipse([9, 13, 22, 26], fill=1), light=False, outline=False)   # glass above the line
    i.layer(DRAUGHT, 3, lambda d: d.chord([9, 13, 22, 26], 0, 180, fill=1), outline=False)
    i.layer(WOOD, 3, lambda d: d.rectangle([13, 4, 18, 7], fill=1))
    i.px([(11, 16), (11, 17), (12, 15)], DRAUGHT[5]); i.px([(14, 22), (17, 23), (19, 21)], DRAUGHT[4])
    i.layer(LEATH, 3, lambda d: d.rectangle([12, 8, 19, 9], fill=1))
    return i.done()

RELICS = {"stand_fast": ward_of_the_wall, "bloodrush": reavers_fang, "mark_prey": hunters_eye, "rally_horn": muster_seal,
          "grave_bloom": gravebloom_censer, "forward_supply": forward_cache, "dash": chargers_spur,
          "shield_bash": gatebreakers_shove, "heal_pulse": lantern_of_mercy, "volley": hunters_volley,
          "war_cry": battle_cry, "iron_skin": second_wind}

def relics(): return {k: on_plate(f()) for k, f in RELICS.items()}

# ---- weapon renders for the gear pictures (icon_render.py, the baked real-model pictures Kort approved 2026-11-13) ----
HERE = os.path.dirname(os.path.abspath(__file__))
SRC = 6           # cube_render's default px per model texel
ICON_SIDE = "linen"   # gear pictures carry no side colour: cloth shows as undyed linen
POSES = {          # item -> (yaw, pitch, roll): long arms lie on the diagonal, point or head up right
    "kite_shield": (-25, 10, 0), "quiver": (-30, 10, -25), "hunting_bow": (-20, 8, -40), "crossbow": (-30, 20, -45)}

def cube_render(cubes, yaw, pitch, roll, px=SRC):
    """build_swords.render with a roll and a clear background: every texel a quad, far to near, flat face light."""
    sys.path.insert(0, os.path.join(HERE, "..", "weapons")); import build_swords as W
    cam = render.mul(render.rot(0, 0, roll), W.rot(pitch, yaw, 0)); light = [-0.35, 0.85, -0.45]; quads = []
    for c in cubes:
        R = W.rot(0, 0, c.turn); lit = set(c.lit)
        for f in W.FACES:
            if f in c.skip: continue
            w, h = c.size(f)
            if not (w and h): continue
            g = c.paint(f, w, h); n = W.app(R, W.NORMAL[f])
            shade = 1.0 if f in lit else 0.62 + 0.38 * max(0.0, sum(n[i] * light[i] for i in range(3)))
            for ty in range(h):
                for tx in range(w):
                    col = g[ty][tx]
                    if col is None: continue
                    pts = []
                    for s_, t_ in ((tx / w, ty / h), ((tx + 1) / w, ty / h), ((tx + 1) / w, (ty + 1) / h), (tx / w, (ty + 1) / h)):
                        pick = W.CORNER[f](s_, t_); p = [c.lo[i] + (c.hi[i] - c.lo[i]) * pick[i] for i in range(3)]
                        q = W.app(R, [p[i] - c.pivot[i] for i in range(3)]); q = render.app(cam, [q[i] + c.pivot[i] for i in range(3)])
                        pts.append((q[0], -q[1], q[2]))
                    quads.append((sum(p[2] for p in pts) / 4, [(p[0], p[1]) for p in pts], tuple(int(col[i] * shade) for i in range(3)) + (255,)))
    xs = [x for _, ps, _ in quads for x, _ in ps]; ys = [y for _, ps, _ in quads for _, y in ps]
    mx, my = min(xs) - 1, min(ys) - 1
    img = Image.new("RGBA", (int((max(xs) - mx + 1) * px), int((max(ys) - my + 1) * px)), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    for _, ps, col in sorted(quads, key=lambda q: -q[0]):
        d.polygon([((x - mx) * px, (y - my) * px) for x, y in ps], fill=col)
    return img

BORDERS = ["9d9d9d", "3fae49", "3d7fd6", "9a4fd6", "e8892b"]

def slot(icon, border, scale):
    """The icon in a 44 px gear slot (32 px icon, 6 px frame), as the gear screen draws it."""
    s = Image.new("RGBA", (44, 44), hx("201e26")); d = ImageDraw.Draw(s)
    d.rectangle([0, 0, 43, 43], outline=hx(border), width=2); s.alpha_composite(icon, (6, 6))
    return s.resize((44 * scale, 44 * scale), Image.NEAREST)

def review_sheet(out):
    from PIL import ImageFont
    ics = relics(); names = list(ics)
    pad = 8; cell = 44 * 3 + pad
    W = pad + len(names) * (44 + pad); H = 3 * 44 + 44 * 3 * 2 + 60
    img = Image.new("RGBA", (max(W, 6 * cell + pad), 44 + pad * 3 + 2 * (44 * 3 + pad) + 20), hx("28262e"))
    x = pad
    for k in names: img.alpha_composite(slot(ics[k], BORDERS[0], 1), (x, pad)); x += 44 + pad
    for r, k in enumerate(names):
        row, col = divmod(r, 6)
        img.alpha_composite(slot(ics[k], BORDERS[col % 5], 3), (pad + col * cell, 44 + pad * 2 + row * cell))
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True); img.save(out); print("icon sheet", out)

if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "sheet"
    if cmd == "relics":
        d = sys.argv[2]; os.makedirs(d, exist_ok=True)
        for k, im in relics().items(): im.save(os.path.join(d, k + ".png"))
    else: review_sheet(sys.argv[2])
