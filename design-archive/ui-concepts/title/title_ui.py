"""The title screen's other pixel art, authored at GUI scale 1 like the hill and the commander:
- lockup.png        GRAVES and GLORY in Marcellus, rasterised 1-bit at its final size, then cut as brass type
- marker.png        the brass spearhead that marks the hovered menu line
- horn.png          the war horn shown beside "Play with friends" while it is hovered (horn_rally plays)
- embers-*.png      one frame of the embers and ash rising off the battle (the game spawns them; this is a still)
- volley-*.png      one frame of the arrow volley crossing the field (by night, fire arrows)

python title_ui.py   -> sprites/*.png"""
import math, os
from PIL import Image, ImageDraw, ImageFont
from pixel import Canvas, poly_mask, hexrgb, save

HERE = os.path.dirname(os.path.abspath(__file__))
FONT = os.path.join(HERE, '..', 'fonts', 'marcellus.ttf')

# brass type: light from above (the sky), a warm bevel on top edges, dark under every stroke
BRASS = {'K': '#120d16', 'B0': '#7d5d30', 'B1': '#b08a4a', 'B2': '#d9b36a', 'B3': '#f2d898', 'B4': '#fff3cf'}
LOCKUP_W, LOCKUP_H = 560, 96


def glyph_mask(text, size, tracking=0):
    """1-bit raster of a string in Marcellus: no anti-aliasing survives, every pixel is placed"""
    font = ImageFont.truetype(FONT, size)
    w = int(sum(font.getlength(ch) for ch in text) + tracking * len(text) + 8)
    asc, desc = font.getmetrics()
    im = Image.new('L', (w, asc + desc + 4), 0)
    d = ImageDraw.Draw(im)
    x = 2
    for ch in text:
        d.text((x, 2), ch, font=font, fill=255)
        x += font.getlength(ch) + tracking
    px = im.load()
    pts = {(i, j) for j in range(im.height) for i in range(im.width) if px[i, j] >= 128}
    xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
    x0, y0 = min(xs), min(ys)
    return {(x - x0, y - y0) for (x, y) in pts}, max(xs) - x0 + 1, max(ys) - y0 + 1


def lockup():
    cv = Canvas(LOCKUP_W, LOCKUP_H)
    big, bw, bh = glyph_mask('GRAVES', 56, 4)
    big2, gw, gh = glyph_mask('GLORY', 56, 4)
    big = big | {(x + 1, y) for (x, y) in big}; bw += 1
    big2 = big2 | {(x + 1, y) for (x, y) in big2}; gw += 1
    small, sw, sh = glyph_mask('and', 22, 1)
    gap = 14
    total = bw + gap + sw + gap + gw
    x0 = (LOCKUP_W - total) // 2
    y0 = 10
    words = [(big, x0, y0, bh), (small, x0 + bw + gap, y0 + bh // 2 - sh // 2 + 2, sh), (big2, x0 + bw + gap + sw + gap, y0, gh)]
    letters = set()
    for mask, ox, oy, hh in words:
        for (x, y) in mask:
            letters.add((x + ox, y + oy))
    # the rule under the words: a hairline with a lozenge at its centre and short tapering ends
    cy = y0 + bh + 9
    rule = set()
    for x in range(x0 + 8, x0 + total - 8):
        rule.add((x, cy))
    cx = LOCKUP_W // 2
    for k in range(5):
        for x in range(cx - (4 - k), cx + (4 - k) + 1):
            rule.add((x, cy - k)); rule.add((x, cy + k))
    for side in (-1, 1):                              # two small lozenges flanking the centre
        mx = cx + side * 22
        for k in range(3):
            for x in range(mx - (2 - k), mx + (2 - k) + 1):
                rule.add((x, cy - k)); rule.add((x, cy + k))
    shape = letters | rule
    # drop shadow (hard, 2 px down) and a one-pixel outline, both in the dark token
    for (x, y) in shape:
        cv.put(x + 1, y + 2, 'K', 1)
    for (x, y) in shape:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in shape:
                cv.put(x + dx, y + dy, 'K', 1)
    # brass: a vertical ramp per word (light from the sky), a bevel light on top edges, a dark lip under strokes
    for mask, ox, oy, hh in words:
        for (x, y) in mask:
            X, Y = x + ox, y + oy
            f = y / max(1, hh - 1)
            c = 'B3' if f < 0.30 else 'B2' if f < 0.62 else 'B1'
            if (X, Y - 1) not in shape:
                c = 'B4' if f < 0.5 else 'B3'
            elif (X, Y + 1) not in shape or (X + 1, Y + 1) not in shape and f > 0.7:
                c = 'B0'
            cv.put(X, Y, c, 2)
    for (x, y) in rule:
        c = 'B3' if y < cy else 'B2' if y == cy else 'B1'
        if (x, y + 1) not in shape:
            c = 'B0'
        cv.put(x, y, c, 2)
    cv.put(cx, cy - 4, 'B4', 2); cv.put(cx, cy - 3, 'B4', 2)
    return cv


def marker_rows():
    """the hovered-line marker: a leaf-bladed brass spearhead pointing at the label, socket and collar on the left"""
    half = {3: 1, 4: 2, 5: 2, 6: 2, 7: 2, 8: 1, 9: 1, 10: 1, 11: 0, 12: 0}
    cells = {}
    for x, h in half.items():
        for y in range(3 - h, 3 + h + 1):
            cells[(x, y)] = '3' if y < 3 else '2' if y == 3 else '1'
    for x in (0, 1):
        for y in (2, 3, 4):
            cells[(x, y)] = '2' if y == 2 else '1'
    for y in range(1, 6):
        cells[(2, y)] = '0' if y in (1, 5) else '1'
    cells[(12, 3)] = '4'; cells[(5, 2)] = '4'; cells[(6, 2)] = '4'
    return outlined(cells)


def horn_rows():
    """a war horn: an ox horn curving up to its bell on the right, bound in brass at the mouthpiece, the middle
    and the bell rim, the open mouth a dark slot; bone lit from above"""
    pts = []
    for i in range(241):
        u = i / 240
        pts.append((u, 1.5 + 12.0 * u + 1.4 * math.sin(math.pi * u), 9.5 - 7.0 * u ** 1.2 + 1.6 * math.sin(math.pi * u)))
    cells = {}
    for y in range(-2, 14):
        for x in range(-2, 18):
            best = None
            for (u, cx, cy) in pts:
                d = (x + .5 - cx) ** 2 + (y + .5 - cy) ** 2
                if best is None or d < best[0]:
                    best = (d, u, cx, cy)
            d, u, cx, cy = best
            r = 1.0 + 2.6 * u ** 1.5
            if d > r * r:
                continue
            side = (cy - (y + .5)) / r                 # +1 top edge (lit), -1 bottom edge
            if u < 0.09 or 0.50 < u < 0.58 or u > 0.90:
                tone = '3' if side > 0.25 else '2' if side > -0.35 else '1'      # brass mounts
                if u > 0.97 and abs(side) < 0.45:
                    tone = 'k'                                               # the open mouth
                elif u > 0.90 and side > 0.55:
                    tone = '4'
            else:
                tone = 'H' if side > 0.3 else 'h' if side > -0.4 else 'j'     # bone
            cells[(x + 2, y + 2)] = tone
    return outlined(cells)


def outlined(cells):
    out = dict(cells)
    for (x, y) in cells:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in cells:
                out[(x + dx, y + dy)] = 'k'
    xs = [p[0] for p in out]; ys = [p[1] for p in out]
    x0, y0 = min(xs), min(ys)
    rows = [['.'] * (max(xs) - x0 + 1) for _ in range(max(ys) - y0 + 1)]
    for (x, y), c in out.items():
        rows[y - y0][x - x0] = c
    return [''.join(r) for r in rows]


def grid(rows, pal):
    h, w = len(rows), max(len(r) for r in rows)
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = im.load()
    for j, r in enumerate(rows):
        for i, ch in enumerate(r):
            if ch in pal:
                px[i, j] = hexrgb(pal[ch]) + (255,)
    return im


GRID_PAL = {'k': '#120d16', '0': '#5e4422', '1': '#8c6c38', '2': '#c8a56b', '3': '#efd497', '4': '#fff3cf',
            'b': '#5e4422', 'j': '#7d6a52', 'h': '#b9a47e', 'H': '#e6d7b2'}

# ---------------------------------------------------------------------------------------------- particles
EMBER_DAY = {'E0': '#8c321e', 'E1': '#e0582a', 'E2': '#ffa04a', 'A0': '#3b3036', 'A1': '#7f7076'}   # deep orange: reads on the pale field
EMBER_NIGHT = {'E0': '#8a3424', 'E1': '#ff9a48', 'E2': '#ffe3a0', 'A0': '#262234', 'A1': '#5d5878'}
SPARKS = {  # each a little sprite, moving up and to the right with the wind (tails trail down-left)
    'e1': ['2'], 'e2': ['12'], 'e3': ['.22', '122', '0..'], 'e4': ['..2', '.1.', '0..'],
    'e5': ['22', '12'], 'e6': ['.22.', '1221', '.11.', '0...'],
    'a1': ['a'], 'a2': ['ab'], 'a3': ['b.', '.a'],
}
KEEP_CLEAR = [(196, 14, 764, 112), (16, 388, 210, 520), (800, 496, 960, 540)]   # lockup, menu, version
# the commander, his body and banner and his raised sword arm: nothing drifts across him
HERO = [(620, 150, 748, 404), (590, 244, 640, 320)]

# Embers rise off the melee in three plumes, drifting up and to the right with the wind, thinning out and shrinking
# as they climb; a few big near ones float up past the brink. By night there are more, and a fourth plume where the
# fire arrows have caught on the left. Every spark and flake is placed by hand: (sprite, x, y).
EMBERS_DAY = [
    # the plume off the left of the melee
    ("e4", 334, 286), ("e3", 341, 274), ("e5", 338, 262), ("e5", 346, 251), ("e2", 352, 238), ("e2", 349, 226),
    ("e1", 358, 213), ("e2", 364, 199), ("e1", 371, 184), ("e1", 384, 171), ("e1", 391, 153), ("e1", 401, 134),
    # off its centre
    ("e3", 404, 292), ("e4", 410, 281), ("e5", 408, 268), ("e5", 417, 257), ("e2", 424, 243), ("e2", 421, 230),
    ("e2", 432, 219), ("e1", 441, 205), ("e1", 450, 190), ("e2", 462, 176), ("e1", 474, 160), ("e1", 487, 143),
    ("e1", 500, 128),
    # off its right, where the Dusk line is breaking
    ("e4", 462, 312), ("e5", 470, 301), ("e5", 468, 289), ("e5", 478, 278), ("e2", 488, 265), ("e4", 497, 252),
    ("e2", 509, 232), ("e1", 520, 216), ("e1", 533, 205), ("e1", 546, 188), ("e1", 560, 170), ("e1", 574, 152),
    # big near ones, risen past the brink
    ("e3", 446, 380), ("e6", 520, 362), ("e6", 568, 392), ("e6", 772, 372), ("e3", 842, 356), ("e6", 905, 420),
    ("e3", 290, 340),
]
# by day the sparks at the foot of each plume, thick over the fighting (by night the extra plume and strays below
# take their places)
BASES_DAY = [("e5", 326, 302), ("e1", 340, 297), ("e1", 392, 300), ("e5", 414, 299), ("e1", 452, 316),
             ("e5", 470, 326)]
EMBERS_NIGHT = EMBERS_DAY + [
    # the fourth plume, from the fire arrows caught on the left
    ("e3", 262, 314), ("e2", 268, 302), ("e1", 266, 290), ("e2", 275, 278), ("e1", 283, 264), ("e1", 290, 248),
    ("e2", 300, 233), ("e1", 309, 216), ("e1", 320, 199), ("e1", 330, 182), ("e1", 334, 146), ("e1", 355, 128),
    # the three plumes thicker
    ("e3", 329, 298), ("e1", 344, 280), ("e1", 336, 244), ("e2", 355, 226), ("e4", 398, 304), ("e1", 414, 274),
    ("e1", 428, 236), ("e2", 446, 212), ("e1", 456, 182), ("e3", 456, 322), ("e1", 474, 294), ("e2", 492, 256),
    ("e1", 503, 244), ("e1", 527, 210), ("e2", 553, 180),
    # strays, near and far
    ("e4", 488, 404), ("e3", 596, 424), ("e1", 880, 340), ("e2", 930, 300), ("e1", 782, 300), ("e1", 820, 250),
    ("e2", 870, 210), ("e1", 910, 170), ("e1", 940, 140),
]
ASH_DAY = [("a1", 344, 244), ("a2", 395, 210), ("a1", 430, 262), ("a3", 455, 232), ("a1", 482, 196),
           ("a2", 515, 262), ("a1", 540, 228), ("a1", 366, 178), ("a2", 412, 160), ("a1", 458, 140),
           ("a1", 588, 206), ("a2", 300, 232), ("a1", 278, 262), ("a1", 526, 182), ("a3", 556, 252),
           ("a1", 584, 296), ("a2", 430, 330), ("a3", 360, 322)]
ASH_NIGHT = [("a1", 344, 244), ("a2", 395, 210), ("a1", 482, 196), ("a3", 556, 252), ("a1", 412, 160),
             ("a2", 430, 330), ("a1", 300, 262), ("a1", 526, 182), ("a2", 588, 206), ("a1", 278, 230),
             ("a1", 466, 246), ("a3", 360, 322)]


def particles(tod):
    cv = Canvas(960, 540)

    def put(sprite, x, y):
        for j, r in enumerate(SPARKS[sprite]):
            for i, ch in enumerate(r):
                if ch == '.':
                    continue
                code = {'0': 'E0', '1': 'E1', '2': 'E2', 'a': 'A0', 'b': 'A1'}[ch]
                cv.put(x + i, y + j, code, 1)

    for (sprite, x, y) in (EMBERS_DAY + BASES_DAY + ASH_DAY if tod == 'day' else EMBERS_NIGHT + ASH_NIGHT):
        for (x0, y0, x1, y1) in KEEP_CLEAR + HERO:
            assert not (x0 - 4 <= x <= x1 + 4 and y0 - 4 <= y <= y1 + 4), (sprite, x, y)
        put(sprite, x, y)
    return cv


VOLLEY_DAY = {'K': '#2a1f26', 'F': '#efe2c6', 'H': '#3d3540', 'G': '#ffcf88'}
VOLLEY_NIGHT = {'K': '#151019', 'F': '#3a3550', 'H': '#ff9a48', 'G': '#ffe9b0', 'T': '#e8622a'}


def arrow(cv, x, y, L, burning):
    """one arrow falling to the right on a clean 1:2 line: fletching at the tail (vanes either side of the
    shaft), a barbed head at the front. Backlit by day, the feathers glow; by night the head burns."""
    for i in range(L):
        cv.put(x + i, y + i // 2, 'K', 1)
    for i in range(4):                                    # vanes
        cv.put(x + i, y + i // 2 - 1, 'F', 1)
        if i < 3:
            cv.put(x + i + 1, y + (i + 1) // 2 + 1, 'F', 1)
    hx, hy = x + L - 1, y + (L - 1) // 2
    cv.put(hx + 1, hy + 1, 'G', 1)                        # the point
    cv.put(hx, hy, 'H', 1); cv.put(hx - 1, hy - 1, 'H', 1); cv.put(hx - 1, hy + 1, 'H', 1)
    if burning:                                           # flame licking back along the shaft, up with the heat
        cv.put(hx - 2, hy - 2, 'T', 1); cv.put(hx - 3, hy - 2, 'T', 1); cv.put(hx - 1, hy - 2, 'G', 1)


# the arrows: (tail x, tail y, length), placed by hand along the flight, clear of the lockup and the commander
VOLLEY = [(337, 159, 13), (347, 139, 12), (349, 150, 14), (372, 152, 13), (374, 161, 13), (399, 174, 12),
          (417, 189, 11), (439, 197, 11), (480, 204, 11), (485, 220, 14), (516, 220, 13), (519, 238, 13),
          (541, 243, 12), (561, 247, 13)]


def volley(burning):
    """fourteen arrows from the Dawn archers, falling toward the Dusk line across the middle of the field:
    a loose group stretched along its flight, clear of the lockup, the tagline and the commander"""
    cv = Canvas(960, 540)
    for (x, y, L) in VOLLEY:
        arrow(cv, x, y, L, burning)
    return cv


if __name__ == '__main__':
    lk = lockup()
    im = lk.render(BRASS)
    save(im.crop(im.getbbox()), 'lockup.png')                 # tight, so nothing else sits inside its box
    save(grid(marker_rows(), GRID_PAL), 'marker.png')
    save(grid(horn_rows(), GRID_PAL), 'horn.png')
    for tod, pal, vpal in (('day', EMBER_DAY, VOLLEY_DAY), ('night', EMBER_NIGHT, VOLLEY_NIGHT)):
        save(particles(tod).render(pal), f'embers-{tod}.png')
        save(volley(tod == 'night').render(vpal), f'volley-{tod}.png')
    print('lockup, marker, horn, embers, volley written')
