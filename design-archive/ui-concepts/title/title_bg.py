"""Title panorama, built from one real frame of the game (run/screenshots/bannerbound-dark-fantasy-combat.png,
Iris + Complementary, point B of the savanna field, Dawn and Dusk mid-clash; copied to src/frame-b-clash.png).

What this script does to it, and nothing else:
  1. The frame is scaled x1.5 (1280x720 -> 1920x1080) and lowered 100 px, so its far fog line sits under the
     title. Rows above that line (the old score bar and the haze over it) fade out into a sky.
  2. The sky above the fog line is painted here (gradient, sun or moon, a few Minecraft-style cloud slabs,
     stars at night): the source frame's sky is hidden behind the HUD score bar, so there is none to keep.
  3. A shader-look grade: golden-hour split tone and haze, bloom on the sun and the lanterns, light shafts;
     at night a moonlit violet grade with the lanterns re-lit as warm pools and short torch rays.
The geometry is never moved, cloned or painted over (map, troops, flags, lanterns stay where the game put them).
The final panorama is to be captured in game with Complementary Reimagined (see TITLE.md).

python title_bg.py            -> bg/pano-day.png, bg/pano-night.png (1920x1080)
"""
import math, os, random
import numpy as np
from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
W, H = 1920, 1080
DY = 100                    # frame offset (screen px) after the x1.5 scale
FADE0, FADE1 = 80, 112      # frame rows (720 space) over which the frame fades in under the sky
SUN = (1792, 214)           # sun centre, screen px (GUI 896, 107): right of the lockup, so the title reads first
MOON = (1590, 112)          # moon centre at night (GUI 795, 56)

# lanterns found in the frame (warm bright clusters, 720 space) and the flag's sea lantern
LANTERNS = [(277, 297), (355, 287), (585, 516), (814, 333), (725, 404), (561, 224), (877, 285), (630, 203),
            (923, 250), (385, 354), (687, 185), (958, 223), (798, 145), (472, 251), (971, 175), (1270, 151),
            (889, 119), (1035, 136)]
SEA_LANTERN = (738, 180)


def lin(c):
    c = np.asarray(c, dtype=np.float32) / 255.0
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def srgb(l):
    l = np.clip(l, 0, 1)
    return np.where(l <= 0.0031308, l * 12.92, 1.055 * np.power(l, 1 / 2.4) - 0.055) * 255.0


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def gradient(stops, t):
    """stops: [(t, (r,g,b)), ...] in sRGB; t array -> float sRGB image"""
    ts = np.array([s[0] for s in stops], np.float32)
    cs = np.array([s[1] for s in stops], np.float32)
    out = np.zeros(t.shape + (3,), np.float32)
    for k in range(3):
        out[..., k] = np.interp(t, ts, cs[:, k])
    return out


def blur(a, r):
    """gaussian blur of a float HxWxC (or HxW) array via PIL, per channel"""
    if a.ndim == 2:
        im = Image.fromarray(np.clip(a * 255 / max(a.max(), 1e-6), 0, 255).astype(np.uint8))
        return np.asarray(im.filter(ImageFilter.GaussianBlur(r)), np.float32) / 255 * max(a.max(), 1e-6)
    return np.dstack([blur(a[..., k], r) for k in range(a.shape[2])])


def frame_layer():
    """the real frame, x1.5, lowered DY; returns sRGB float image and its alpha (fade under the sky)"""
    src = Image.open(os.path.join(HERE, 'src', 'frame-b-clash.png')).convert('RGB')
    big = src.resize((1920, 1080), Image.LANCZOS)
    img = np.zeros((H, W, 3), np.float32)
    img[DY:, :] = np.asarray(big, np.float32)[:H - DY]
    rows = (np.arange(H) - DY) / 1.5                     # source row of each screen row
    a = smooth(FADE0, FADE1, rows)[:, None] * np.ones((1, W), np.float32)
    a[:DY] = 0
    return img, a


def to_screen(p):
    return (p[0] * 1.5, p[1] * 1.5 + DY)


# ------------------------------------------------------------------------------------------------ day
def sky_day():
    y = np.arange(H, dtype=np.float32)[:, None] * np.ones((1, W), np.float32)
    x = np.ones((H, 1), np.float32) * np.arange(W, dtype=np.float32)[None, :]
    hz = 268.0                                             # where the sky meets the haze over the fog line
    t = np.clip(y / hz, 0, 1.2)
    sky = gradient([(0.0, (34, 30, 58)), (0.30, (50, 42, 74)), (0.55, (80, 58, 88)), (0.72, (122, 80, 96)),
                    (0.84, (184, 112, 94)), (0.93, (230, 154, 100)), (1.0, (248, 198, 134)), (1.2, (250, 214, 160))], t)
    # the side away from the sun stays cooler and darker; the sun side warms
    side = np.clip((x - 200) / 1500, 0, 1)
    sky *= (0.86 + 0.14 * side)[..., None]
    # sun glow: wide warm glow plus a tight bloom core (Complementary's sun bloom)
    d = np.hypot(x - SUN[0], (y - SUN[1]) * 1.25)
    glow = np.exp(-(d / 280) ** 2)[..., None] * np.array([132, 80, 26], np.float32)     # halo kept off the lockup
    core = np.exp(-(d / 62) ** 2)[..., None] * np.array([255, 236, 190], np.float32)
    disc = (d < 22).astype(np.float32)[..., None] * np.array([255, 250, 235], np.float32)
    sky = sky + glow * (0.55 + 0.45 * smooth(60, 260, y))[..., None] + core * 0.85
    sky = np.maximum(sky, disc)
    return np.clip(sky, 0, 255), y, x


def clouds(sky, night=False):
    """distant clouds: at this camera height the cloud layer only shows within ~10 degrees of the horizon,
    so the clouds are long, thin, foreshortened streaks, lit from below by the low sun (by the moon at night).
    They keep out of the band behind the tagline (x 700-1220, y 128-176)."""
    rnd = np.random.default_rng(9)
    y = np.arange(H, dtype=np.float32)[:, None]
    x = np.arange(W, dtype=np.float32)[None, :]
    out = sky.copy()
    streaks = [  # x0, x1, centre y, half-thickness
        (40, 640, 192, 7), (1160, 1910, 152, 9), (1290, 1780, 98, 5), (0, 430, 116, 5),
        (1480, 1920, 246, 4), (380, 760, 236, 3), (60, 520, 62, 4), (1500, 1900, 40, 5)]
    for (x0, x1, cy, th) in streaks:
        n1 = np.interp(x[0], np.linspace(x0, x1, 9), rnd.uniform(0.3, 1.0, 9))
        n2 = np.interp(x[0], np.linspace(x0, x1, 23), rnd.uniform(0.6, 1.0, 23))
        env = np.clip(np.minimum((x[0] - x0) / 120, (x1 - x[0]) / 120), 0, 1) * n1 * n2      # ragged ends
        thick = th * (0.55 + 0.6 * n1)
        prof = np.exp(-(((y - cy) / np.maximum(thick, 1)[None, :]) ** 2))                    # soft body
        under = np.clip((y - cy) / np.maximum(thick, 1)[None, :] + 0.6, 0, 1)                # lit underside
        a = prof * env[None, :] * (0.85 if not night else 0.75)
        if not night:
            ds = np.exp(-(np.abs(x - SUN[0]) / 700) ** 2)
            lit = np.array([214, 132, 112], np.float32) * (1 - ds[..., None]) + np.array([255, 196, 128], np.float32) * ds[..., None]
            top = np.array([70, 56, 86], np.float32)
        else:
            dm = np.exp(-(np.abs(x - MOON[0]) / 600) ** 2)
            lit = np.array([70, 58, 108], np.float32) * (1 - dm[..., None]) + np.array([132, 124, 178], np.float32) * dm[..., None]
            top = np.array([24, 20, 46], np.float32)
        col = top * (1 - under[..., None]) + lit * under[..., None]
        out = out * (1 - a[..., None]) + col * a[..., None]
    return out


def grade_day(img):
    """golden hour: warm lights, violet shadows, haze rising with distance, a little more contrast"""
    l = lin(img)
    lum = (0.2126 * l[..., 0] + 0.7152 * l[..., 1] + 0.0722 * l[..., 2])[..., None]
    warm = l * np.array([1.18, 0.98, 0.70], np.float32)            # sunlight goes amber
    shade = l * np.array([0.82, 0.80, 1.04], np.float32)           # skylight in shadow stays violet-blue
    k = smooth(0.02, 0.22, lum)
    g = shade * (1 - k) + warm * k
    g = np.power(np.clip(g * 1.12, 0, None), 1.06)                 # contrast
    return srgb(g)


def haze_day(img, y, x):
    """atmospheric haze: far rows (top of the frame) fade toward the horizon glow; strongest toward the sun"""
    rows = (y - DY) / 1.5
    far = 1 - smooth(110, 380, rows)                     # 1 at the fog line, 0 by the middle of the field
    toward_sun = np.exp(-((x - SUN[0]) / 700) ** 2)
    amt = (0.10 + 0.55 * far + 0.18 * toward_sun * far)[..., None]
    hcol = np.where(toward_sun[..., None] > 0.5, np.array([250, 196, 128], np.float32),
                    np.array([196, 140, 120], np.float32))
    hcol = np.array([206, 150, 116], np.float32) * (1 - toward_sun[..., None]) + np.array([252, 204, 140], np.float32) * toward_sun[..., None]
    return img * (1 - amt) + hcol * amt


def rays(y, x, origin, length, strength, color, n=48, seed=3, spread=1.0):
    """crepuscular rays: angular noise around a light source, fading with distance (additive)"""
    rnd = random.Random(seed)
    ang = np.arctan2(y - origin[1], x - origin[0])
    d = np.hypot(x - origin[0], y - origin[1])
    a = np.zeros_like(ang)
    for i in range(n):
        c = rnd.uniform(-math.pi, math.pi)
        w = rnd.uniform(0.008, 0.035) * spread
        a += rnd.uniform(0.35, 1.0) * np.exp(-((np.angle(np.exp(1j * (ang - c)))) / w) ** 2)
    a = np.clip(a, 0, 1.2)
    fall = np.exp(-d / length) * smooth(10, 60, d)
    return (a * fall * strength)[..., None] * np.array(color, np.float32)


def bloom(img, thresh, radius, amount):
    l = img.max(-1, keepdims=True)
    hi = np.clip((l - thresh) / (255 - thresh), 0, 1) * img
    return img + blur(hi, radius) * amount


def lantern_glow(y, x, rad, col, amount, sources):
    out = np.zeros((H, W, 3), np.float32)
    for p in sources:
        sx, sy = to_screen(p)
        d = np.hypot(x - sx, y - sy)
        out += np.exp(-(d / rad) ** 2)[..., None] * np.array(col, np.float32) * amount
    return out


def vignette(img, y, x, strength=0.42):
    """the menu overlay (vanilla ships one: textures/gui/title/background/panorama_overlay.png): darker
    toward the bottom-left where the menu sits and toward the top where the title sits"""
    nx, ny = x / W, y / H
    bl = np.exp(-(((nx - 0.0) / 0.42) ** 2 + ((ny - 1.0) / 0.42) ** 2))
    top = np.clip(1 - ny / 0.16, 0, 1) ** 1.5
    edge = np.clip((np.hypot(nx - 0.55, ny - 0.5) - 0.45) / 0.45, 0, 1)
    k = np.clip(0.55 * bl + 0.35 * top + 0.45 * edge, 0, 1) * strength
    return img * (1 - k[..., None])


def build_day():
    frame, a = frame_layer()
    sky, y, x = sky_day()
    sky = clouds(sky)
    f = grade_day(frame)
    f = haze_day(f, y, x)
    img = sky * (1 - a[..., None]) + f * a[..., None]
    # a warm haze band where the sky meets the fog line (Complementary's volumetric fog toward the sun)
    rows = (y - DY) / 1.5
    band = np.exp(-((rows - 96) / 26) ** 2) * (0.25 + 0.75 * np.exp(-((x - SUN[0]) / 620) ** 2))
    img = img * (1 - 0.55 * band[..., None]) + np.array([255, 212, 150], np.float32) * 0.55 * band[..., None]
    # light shafts from the low sun, falling across the field (screen blend)
    r = rays(y, x, SUN, 900, 0.20, (255, 196, 120), n=56, seed=11) * (0.18 + 0.82 * smooth(SUN[1] - 30, SUN[1] + 140, y))[..., None]
    img = 255 - (255 - img) * (1 - r / 255)
    # lantern bloom (small by day), sun bloom
    img = img + lantern_glow(y, x, 7, (255, 170, 80), 0.35, LANTERNS)
    img = bloom(img, 200, 14, 0.35)
    img = vignette(img, y, x, 0.40)
    return np.clip(img, 0, 255)


# ------------------------------------------------------------------------------------------------ night
def sky_night():
    y = np.arange(H, dtype=np.float32)[:, None] * np.ones((1, W), np.float32)
    x = np.ones((H, 1), np.float32) * np.arange(W, dtype=np.float32)[None, :]
    hz = 268.0
    t = np.clip(y / hz, 0, 1.2)
    sky = gradient([(0.0, (10, 9, 26)), (0.35, (20, 16, 44)), (0.62, (38, 28, 70)), (0.82, (66, 46, 100)),
                    (0.94, (100, 74, 132)), (1.0, (122, 96, 150)), (1.2, (130, 104, 156))], t)
    # moon halo
    d = np.hypot(x - MOON[0], y - MOON[1])
    sky += np.exp(-(d / 260) ** 2)[..., None] * np.array([46, 40, 78], np.float32)
    sky += np.exp(-(d / 70) ** 2)[..., None] * np.array([90, 90, 130], np.float32)
    # stars: sparse, fewer toward the horizon, none in the moon's glow
    rnd = np.random.default_rng(5)
    n = 520
    sx = rnd.integers(0, W, n); sy = (rnd.random(n) ** 1.6 * 250).astype(int)
    for i in range(n):
        if np.hypot(sx[i] - MOON[0], sy[i] - MOON[1]) < 150:
            continue
        b = rnd.uniform(0.25, 1.0) * (1 - sy[i] / 300)
        c = np.array([210, 205, 240], np.float32) * b
        sky[sy[i], sx[i]] = np.maximum(sky[sy[i], sx[i]], c)
        if b > 0.8 and sx[i] + 1 < W:
            sky[sy[i], sx[i] + 1] = np.maximum(sky[sy[i], sx[i] + 1], c * 0.6)
    return sky, y, x


def moon(img):
    """the vanilla moon is a square pixel texture; Complementary keeps it square and adds a halo"""
    tex = ['..AAAA..', '.ABBBBA.', 'ABCBBBBA', 'ABBBBCBA', 'ABBCBBBA', 'ABBBBBCA', '.ABCBBA.', '..AAAA..']
    pal = {'A': (176, 178, 214), 'B': (226, 228, 244), 'C': (190, 192, 222)}
    s = 7
    x0, y0 = MOON[0] - 4 * s, MOON[1] - 4 * s
    for j, row in enumerate(tex):
        for i, ch in enumerate(row):
            if ch in pal:
                img[y0 + j * s:y0 + (j + 1) * s, x0 + i * s:x0 + (i + 1) * s] = pal[ch]
    return img


def grade_night(img, y, x):
    """moonlight: low exposure, violet-blue, desaturated; the lanterns re-light the ground around them"""
    l = lin(img)
    lum = (0.2126 * l[..., 0] + 0.7152 * l[..., 1] + 0.0722 * l[..., 2])[..., None]
    moonlit = (lum * 0.55 + l * 0.45) * np.array([0.16, 0.15, 0.30], np.float32)
    # warm pools: the frame's own surface colour, lit by each lantern (falloff ~5 blocks)
    pool = np.zeros((H, W), np.float32)
    for p in LANTERNS:
        sx, sy = to_screen(p)
        d = np.hypot((x - sx), (y - sy) * 1.6)
        pool += np.exp(-(d / 46) ** 2) * 1.0 + np.exp(-(d / 110) ** 2) * 0.28
    sx, sy = to_screen(SEA_LANTERN)
    sea = np.exp(-(np.hypot(x - sx, (y - sy) * 1.4) / 80) ** 2)
    torch = l * np.array([1.25, 0.72, 0.36], np.float32) * np.clip(pool, 0, 1.2)[..., None] * 0.9
    seal = l * np.array([0.55, 0.78, 0.82], np.float32) * sea[..., None] * 0.7
    return srgb(moonlit + torch + seal)


def build_night():
    frame, a = frame_layer()
    sky, y, x = sky_night()
    sky = clouds(sky, night=True)
    sky = moon(sky)
    f = grade_night(frame, y, x)
    rows = (y - DY) / 1.5
    far = 1 - smooth(110, 380, rows)
    f = f * (1 - 0.45 * far[..., None]) + np.array([70, 52, 104], np.float32) * 0.45 * far[..., None]
    img = sky * (1 - a[..., None]) + f * a[..., None]
    band = np.exp(-((rows - 96) / 24) ** 2)
    img = img * (1 - 0.45 * band[..., None]) + np.array([118, 92, 150], np.float32) * 0.45 * band[..., None]
    # torches: bloom halos and short rays (Complementary's light shafts from block lights)
    img = img + lantern_glow(y, x, 9, (255, 200, 120), 0.9, LANTERNS)
    img = img + lantern_glow(y, x, 26, (255, 140, 60), 0.35, LANTERNS)
    for i, p in enumerate(LANTERNS):
        sp = to_screen(p)
        r = rays(y, x, sp, 38, 0.16, (255, 170, 90), n=10, seed=40 + i, spread=2.2)
        img = 255 - (255 - img) * (1 - np.clip(r, 0, 255) / 255)
    img = img + lantern_glow(y, x, 10, (190, 240, 255), 0.8, [SEA_LANTERN])
    img = img + lantern_glow(y, x, 30, (120, 200, 230), 0.25, [SEA_LANTERN])
    # faint moon shafts through the haze
    r = rays(y, x, MOON, 700, 0.06, (170, 160, 230), n=30, seed=21) * (0.15 + 0.85 * smooth(MOON[1], MOON[1] + 200, y))[..., None]
    img = 255 - (255 - img) * (1 - r / 255)
    img = bloom(img, 190, 10, 0.4)
    img = vignette(img, y, x, 0.42)
    return np.clip(img, 0, 255)


if __name__ == '__main__':
    os.makedirs(os.path.join(HERE, 'bg'), exist_ok=True)
    Image.fromarray(build_day().astype(np.uint8)).save(os.path.join(HERE, 'bg', 'pano-day.png'))
    print('wrote bg/pano-day.png')
    Image.fromarray(build_night().astype(np.uint8)).save(os.path.join(HERE, 'bg', 'pano-night.png'))
    print('wrote bg/pano-night.png')
