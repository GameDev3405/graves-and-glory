"""Battle map prototype: two looks of a bundled engine map's playable field, plus overlay mock-ups.

    python tools/battlemap_preview.py [MAP_DIR] [OUT_DIR]
    (defaults: the bundled New Kingsfield, kingsfield_cq; OUT_DIR the current directory)

Input is only a grid of (top block id, top height) per column, which is what the server will scan; everything below is
grid-based, deterministic (no random numbers, only a cheap integer hash of cell coordinates) and linear in the area.
World x/z = map-local + map.json origin; the field is |x| <= halfX, |z| <= halfZ (north up, east right).

Steps (port order):
 1. Scan: for each field column take the highest block whose model covers the whole cell (skip air, torches, flowers,
    plants, small decorations); keep its id and height (y + box top / 16).
 2. REAL look: colour = the block's top colour (tools/mapforge/colours.json); light from the north-west:
    f = clamp(1 + 0.035 (h - median), 0.72, 1.28) * (1 + 0.09 clamp((h - h_north) + (h - h_west), -3, 3)); a column
    2+ lower than its north-west neighbour gets * 0.84 (cast shadow). One flat colour per column, scaled up by nearest.
 3. PARCHMENT look:
  a. kind per column from battlemap/kinds.json (ordered rules: id, suffix, prefix; default ground).
  b. terrain T: heights of natural kinds (ground, rock, crops, road, water); building and foliage columns are filled
     by repeated 4-neighbour averaging from known cells. A column with a 'flat' rule that stands less than 'raise'
     above T takes its flat kind (paving, low walls, hay on a field).
  c. clean-up: road, rock and crops cells with fewer than 3 equal 8-neighbours fall back to ground (twice); building
     cells with fewer than 2 building 4-neighbours become ground; building and water masks get a 1-cell closing.
  d. at S px per block: kind masks are scaled up; water, road, crops and rock masks are box-blurred (radius S) and
     thresholded at 0.5 so their edges are round; building masks stay square.
  e. layers, bottom to top: parchment base (base colour, +-3 per-pixel hash grain, 2 low-frequency blotch octaves of
     hashed lattice values bilinearly interpolated, edge vignette); faint hillshade from smoothed T; rock wash + sparse
     hashed stipple; crops hatching (lines every 4 px along the field's long axis, from per-cell row/column run length);
     contour lines every 4 blocks of smoothed T (thin, faint; every 5th darker); cliff hachures (where T drops 3+ to
     a 4-neighbour: a short tick downhill, every other cell); roads (tan band, thin ink edge); water (blue-grey wash,
     ink shore, 2 inner ripple lines at blurred-mask levels); building shadows (+2,+2 px) then warm fill with a 2 px
     ink outline (mask minus its 2-step erosion); trees: discs on a 5-block lattice with hashed jitter and radius,
     wherever the lattice cell's column is foliage, drawn north to south (fill + 1 px ink outline, a small shade
     crescent), so canopies read as scalloped clusters; double ink border.
 4. Overlays (mock-up only) are drawn in screen space over the scaled-down map.
"""
import json
import math
import pathlib
import sys
import time

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from mapforge import nbt  # noqa: E402
from mapforge.render import Looks, _small  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
MAP_DIR = ROOT / 'src/main/resources/data/gravesandglory/sectionmaps/kingsfield_cq'
KINDS = ROOT / 'src/main/resources/assets/gravesandglory/battlemap/kinds.json'
S = 3  # px per block for the parchment and real renders


# ---------------------------------------------------------------- scan

def read_tile(path):
    _, root = nbt.read_file(path)
    v = root.value
    sx, sy, sz = [t.value for t in v['size'].value]
    pal = []
    for e in v['palette'].value:
        name = e.value['Name'].value
        props = e.value.get('Properties')
        pal.append(name + ('[' + ','.join(f'{k}={t.value}' for k, t in sorted(props.value.items())) + ']' if props else ''))
    stack = np.frombuffer(v['stack'].value, dtype='>i2').reshape(sz, sx, sy)
    return pal, stack


def scan(map_dir, looks):
    """(manifest, ids [d, w] object array of block ids or '', height [d, w] float, top colour [d, w, 3])."""
    m = json.loads((map_dir / 'map.json').read_text(encoding='utf-8'))
    w, d = m['width'], m['depth']
    ids = np.full((d, w), '', dtype=object)
    height = np.full((d, w), float(m['floorY']), dtype=np.float32)
    colour = np.zeros((d, w, 3), dtype=np.float32)
    for tile in m['tiles']:
        pal, stack = read_tile(map_dir / tile['file'])
        sz, sx, sy = stack.shape
        info = [looks.get(p) for p in pal]
        full = np.array([bool(l) and not _small(l[2]) for l in info] + [False])
        mask = full[stack]
        hit = mask.any(axis=2)
        top = sy - 1 - np.argmax(mask[:, :, ::-1], axis=2)
        idx = np.take_along_axis(stack, top[:, :, None], 2)[:, :, 0]
        boxtop = np.array([(l[2][4] if l else 16) for l in info] + [16])[idx]
        cols = np.array([(l[0] if l else (0, 0, 0)) for l in info] + [(0, 0, 0)], dtype=np.float32)[idx]
        names = np.array([p.split('[')[0] for p in pal] + [''], dtype=object)[idx]
        z0, x0 = tile['z'], tile['x']
        sl = (slice(z0, z0 + sz), slice(x0, x0 + sx))
        ids[sl] = np.where(hit, names, '')
        height[sl] = np.where(hit, m['floorY'] + top + boxtop / 16.0, m['floorY'])
        colour[sl] = cols
    return m, ids, height, colour


def field_box(m):
    ox, oz = m['origin']
    hx, hz = m['field']['halfX'], m['field']['halfZ']
    return -hx - ox, -hz - oz, hx - ox + 1, hz - oz + 1  # local x0, z0, x1, z1 (exclusive)


# ---------------------------------------------------------------- grid helpers

def shift(a, dz, dx, fill=None):
    """a moved by (dz, dx) with edge cells repeated (fill=None) or a constant."""
    n = max(abs(dz), abs(dx), 1)
    pad = np.pad(a, ((n, n), (n, n)) + ((0, 0),) * (a.ndim - 2), mode='edge' if fill is None else 'constant',
                 **({} if fill is None else {'constant_values': fill}))
    return pad[n - dz:n - dz + a.shape[0], n - dx:n - dx + a.shape[1]].copy()


def box_blur(a, r):
    """Mean over a (2r+1)^2 window, edges repeated; separable running sums."""
    if r <= 0:
        return a.astype(np.float32)
    a = a.astype(np.float32)
    for axis in (0, 1):
        pad = [(0, 0)] * a.ndim
        pad[axis] = (r + 1, r)
        c = np.cumsum(np.pad(a, pad, mode='edge'), axis=axis, dtype=np.float64)
        n = a.shape[axis]
        hi = np.take(c, np.arange(2 * r + 1, 2 * r + 1 + n), axis=axis)
        lo = np.take(c, np.arange(0, n), axis=axis)
        a = ((hi - lo) / (2 * r + 1)).astype(np.float32)
    return a


def erode(mask, steps=1):
    for _ in range(steps):
        mask = mask & shift(mask, 1, 0) & shift(mask, -1, 0) & shift(mask, 0, 1) & shift(mask, 0, -1)
    return mask


def dilate(mask, steps=1):
    for _ in range(steps):
        mask = mask | shift(mask, 1, 0) | shift(mask, -1, 0) | shift(mask, 0, 1) | shift(mask, 0, -1)
    return mask


def neighbours8(mask):
    m = mask.astype(np.int8)
    return sum(shift(m, dz, dx, 0) for dz in (-1, 0, 1) for dx in (-1, 0, 1) if dz or dx)


def neighbours4(mask):
    m = mask.astype(np.int8)
    return shift(m, 1, 0, 0) + shift(m, -1, 0, 0) + shift(m, 0, 1, 0) + shift(m, 0, -1, 0)


def big_parts(mask, smallest):
    """mask of the 8-connected parts of mask with at least `smallest` cells (iterative flood fill)."""
    d, w = mask.shape
    seen = np.zeros_like(mask)
    keep = np.zeros_like(mask)
    for z0, x0 in zip(*np.nonzero(mask)):
        if seen[z0, x0]:
            continue
        part, todo = [], [(z0, x0)]
        seen[z0, x0] = True
        while todo:
            z, x = todo.pop()
            part.append((z, x))
            for dz in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    nz, nx = z + dz, x + dx
                    if 0 <= nz < d and 0 <= nx < w and mask[nz, nx] and not seen[nz, nx]:
                        seen[nz, nx] = True
                        todo.append((nz, nx))
        if len(part) >= smallest:
            for z, x in part:
                keep[z, x] = True
    return keep


def up(a, s):
    return np.repeat(np.repeat(a, s, axis=0), s, axis=1)


def up_linear(a, s):
    """Bilinear scale-up by s, sample centres aligned to cell centres."""
    d, w = a.shape
    zs = (np.arange(d * s) + 0.5) / s - 0.5
    xs = (np.arange(w * s) + 0.5) / s - 0.5
    z0 = np.clip(np.floor(zs).astype(int), 0, d - 1)
    x0 = np.clip(np.floor(xs).astype(int), 0, w - 1)
    z1, x1 = np.clip(z0 + 1, 0, d - 1), np.clip(x0 + 1, 0, w - 1)
    fz = np.clip(zs - np.floor(zs), 0, 1)[:, None]
    fx = np.clip(xs - np.floor(xs), 0, 1)[None, :]
    top = a[z0][:, x0] * (1 - fx) + a[z0][:, x1] * fx
    bot = a[z1][:, x0] * (1 - fx) + a[z1][:, x1] * fx
    return (top * (1 - fz) + bot * fz).astype(np.float32)


def hash2(x, z, seed=0):
    """Cheap integer hash of (x, z) -> [0, 1); same formula ports to Java with int overflow (mask to 32 bits)."""
    h = (np.asarray(x, dtype=np.int64) * 374761393 + np.asarray(z, dtype=np.int64) * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h = h ^ (h >> 16)
    return (h & 0xFFFF) / 65536.0


def hex_rgb(t):
    return np.array([int(t[i:i + 2], 16) for i in (1, 3, 5)], dtype=np.float32)


def paint(img, mask, colour, alpha=1.0):
    a = (mask.astype(np.float32) * alpha)[..., None]
    img *= 1 - a
    img += a * (hex_rgb(colour) if isinstance(colour, str) else colour)


# ---------------------------------------------------------------- REAL look

def render_real(height, colour, ids, s):
    h = height
    ref = float(np.median(h))
    slope = np.clip((h - shift(h, 1, 0)) + (h - shift(h, 0, 1)), -3, 3)
    f = np.clip(1 + 0.035 * (h - ref), 0.72, 1.28) * (1 + 0.09 * slope)
    f = np.where(shift(h, 1, 1) - h >= 2, f * 0.84, f)
    rgb = np.clip(colour * f[..., None], 0, 255)
    rgb[ids == ''] = (40, 43, 51)
    return Image.fromarray(up(rgb.astype(np.uint8), s), 'RGB')


# ---------------------------------------------------------------- kinds

def load_kinds(path):
    t = json.loads(pathlib.Path(path).read_text(encoding='utf-8'))
    return t


def classify(ids, table):
    kinds = table['kinds']
    code = {k: i for i, k in enumerate(kinds)}
    cache = {}

    def one(name):
        if name in cache:
            return cache[name]
        res = (code[table['default']], -1)
        for r in table['rules']:
            if name in r.get('ids', ()) or any(name.endswith(sfx) for sfx in r.get('suffix', ())) or \
                    any(name.startswith(p) for p in r.get('prefix', ())):
                res = (code[r['kind']], code[r['flat']] if 'flat' in r else -1)
                break
        cache[name] = res
        return res
    flat = np.vectorize(lambda n: one(n)[1], otypes=[np.int8])(ids)
    kind = np.vectorize(lambda n: one(n)[0], otypes=[np.int8])(ids)
    return kind, flat, code


def terrain(height, kind, code):
    natural = np.isin(kind, [code[k] for k in ('ground', 'rock', 'crops', 'road', 'water', 'stonework')])
    t = np.where(natural, height, 0).astype(np.float32)
    known = natural.copy()
    for _ in range(64):
        if known.all():
            break
        kf = known.astype(np.float32)
        tot = shift(t * kf, 1, 0, 0) + shift(t * kf, -1, 0, 0) + shift(t * kf, 0, 1, 0) + shift(t * kf, 0, -1, 0)
        cnt = shift(kf, 1, 0, 0) + shift(kf, -1, 0, 0) + shift(kf, 0, 1, 0) + shift(kf, 0, -1, 0)
        new = ~known & (cnt > 0)
        t = np.where(new, tot / np.maximum(cnt, 1), t)
        known |= new
    return t, natural


def resolve(height, kind, flat, code, raise_by):
    t, natural = terrain(height, kind, code)
    raised = height - t >= raise_by
    kind = np.where((flat >= 0) & ~raised, flat, kind).astype(np.int8)
    g = code['ground']
    # area kinds become smooth regions: the share of the kind in a (2r+1)^2 window against a threshold. Cells that
    # leave a region fall to ground; ground cells that join it take the kind (only ground, never buildings or trees).
    for k, r, thr in (('rock', 3, 0.50), ('crops', 2, 0.45), ('road', 1, 0.45)):
        m = kind == code[k]
        region = box_blur(m.astype(np.float32), r) >= thr
        if k == 'rock':  # ground holes inside a rock area (loose dirt on a quarry floor) read as rock
            region |= box_blur(m.astype(np.float32), 6) >= 0.5
        kind = np.where(m & ~region, g, kind)
        kind = np.where(region & (kind == g), code[k], kind)
    for k, smallest in (('building', 6), ('stonework', 4)):
        m = kind == code[k]
        kind = np.where(m & ~big_parts(m, smallest), g, kind)
    for k in ('building', 'water'):
        m = kind == code[k]
        closed = erode(dilate(m)) & ~m & (kind != code['water'] if k == 'building' else True)
        kind = np.where(closed, code[k], kind)
    t, _ = terrain(height, kind, code)  # again with the resolved kinds
    return kind.astype(np.int8), t


# ---------------------------------------------------------------- PARCHMENT look

PAPER = '#e6d3a6'
INK = '#3e2b1a'
INK_MID = '#6e5232'
SEPIA = '#9a7a52'


def soft(mask_cells, s, r=None):
    m = up(mask_cells.astype(np.float32), s)
    return box_blur(m, r if r is not None else s) >= 0.5


def outline(mask, steps=1):
    return mask & ~erode(mask, steps)


def paper(hp, wp):
    zz, xx = np.mgrid[0:hp, 0:wp]
    img = np.empty((hp, wp, 3), dtype=np.float32)
    img[...] = hex_rgb(PAPER)
    blot = np.zeros((hp, wp), dtype=np.float32)
    for cell, amp, seed in ((96, 7.0, 1), (24, 3.0, 2)):
        lw, lh = wp // cell + 2, hp // cell + 2
        lat = hash2(*np.meshgrid(np.arange(lw), np.arange(lh)), seed=seed).astype(np.float32)
        blot += (up_linear(lat, cell)[:hp, :wp] - 0.5) * 2 * amp
    grain = (hash2(xx, zz, 3) - 0.5) * 6
    img += (blot + grain)[..., None] * np.array([1.0, 0.95, 0.8])
    # vignette: darken and warm toward the edges
    ex = np.minimum(xx, wp - 1 - xx) / (0.18 * wp)
    ez = np.minimum(zz, hp - 1 - zz) / (0.18 * hp)
    e = np.clip(np.minimum(ex, ez), 0, 1)
    v = (1 - e) ** 2
    img -= v[..., None] * np.array([38, 46, 52])
    return img


def render_parchment(height, kind, t, code, s, box):
    d, w = kind.shape
    hp, wp = d * s, w * s
    x0, z0 = box[0], box[1]
    img = paper(hp, wp)
    K = {k: kind == i for k, i in code.items()}

    # hillshade, very faint
    ts = box_blur(box_blur(t, 2), 2)
    hs = np.clip(((shift(ts, 1, 0) - shift(ts, -1, 0)) + (shift(ts, 0, 1) - shift(ts, 0, -1))) * -0.25, -1, 1)
    hsp = up_linear(hs, s)
    img *= (1 + 0.06 * hsp)[..., None]

    # rock: wash and sparse stipple
    rock = soft(K['rock'], s)
    paint(img, rock, '#b8a888', 0.35)
    zz, xx = np.mgrid[0:hp, 0:wp]
    stip = rock & (hash2(xx // 3 + x0, zz // 3 + z0, 11) < 0.03) & (xx % 3 == 1) & (zz % 3 == 1)
    paint(img, stip, INK_MID, 0.30)
    paint(img, outline(rock), SEPIA, 0.25)

    # crops: faint hatching along the field's long axis
    crops_c = K['crops']
    runx = np.zeros_like(crops_c, dtype=np.int32)
    runz = np.zeros_like(crops_c, dtype=np.int32)
    for x in range(1, w):
        runx[:, x] = np.where(crops_c[:, x], runx[:, x - 1] + 1, 0)
    for x in range(w - 2, -1, -1):
        runx[:, x] = np.where(crops_c[:, x], np.maximum(runx[:, x], runx[:, x + 1]), 0)
    for z in range(1, d):
        runz[z] = np.where(crops_c[z], runz[z - 1] + 1, 0)
    for z in range(d - 2, -1, -1):
        runz[z] = np.where(crops_c[z], np.maximum(runz[z], runz[z + 1]), 0)
    along_x = box_blur((runx >= runz).astype(np.float32), 3) >= 0.5
    crops = soft(crops_c, s)
    ax = up(along_x, s)
    hatch = crops & np.where(ax, (zz % 4) == 0, (xx % 4) == 0)
    paint(img, crops, '#d8c08a', 0.45)
    paint(img, hatch, SEPIA, 0.45)
    paint(img, outline(crops), SEPIA, 0.30)

    # contours every 4 blocks
    tsp = up_linear(box_blur(t, 1), s)
    lvl = np.floor(tsp / 4).astype(np.int32)
    edge = (lvl != shift(lvl, 0, -1)) | (lvl != shift(lvl, -1, 0))
    major = edge & (np.maximum(lvl, shift(lvl, 0, -1)) % 5 == 0)
    paint(img, edge & ~major, SEPIA, 0.30)
    paint(img, major, SEPIA, 0.50)

    # cliff hachures
    draw_ticks = []
    natural = ~(K['building'] | K['foliage'])
    for dz, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        drop = t - shift(t, -dz, -dx)  # neighbour at (z+dz, x+dx)
        cliff = (drop >= 3) & natural & ((np.arange(w)[None, :] + np.arange(d)[:, None]) % 2 == 0)
        for z, x in zip(*np.nonzero(cliff)):
            draw_ticks.append((x, z, dx, dz))
    pil = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGB')
    dr = ImageDraw.Draw(pil, 'RGBA')
    ink_mid = tuple(int(c) for c in hex_rgb(INK_MID)) + (170,)
    for x, z, dx, dz in draw_ticks:
        cx, cz = x * s + s / 2, z * s + s / 2
        dr.line((cx + dx * s * 0.3, cz + dz * s * 0.3, cx + dx * s * 1.6, cz + dz * s * 1.6), fill=ink_mid, width=1)
    img = np.asarray(pil).astype(np.float32)

    # roads
    road = soft(K['road'], s)
    paint(img, road, '#dcc391', 0.85)
    paint(img, outline(road), INK_MID, 0.55)

    # water
    wat = soft(K['water'], s)
    wblur = box_blur(up(K['water'].astype(np.float32), s), 2 * s)
    paint(img, wat, '#aebcb8', 0.9)
    for lv in (0.8, 0.95):
        inner = wblur >= lv
        paint(img, outline(inner) & wat, '#7f928f', 0.7)
    paint(img, outline(dilate(wat)), INK, 0.85)

    # stonework (statues, reliefs): pale shape, thin ink outline
    stw = soft(K['stonework'], s, 1)
    paint(img, stw, '#e9e2cf', 0.9)
    paint(img, outline(stw), INK_MID, 0.8)

    # buildings: shadow, fill, outline
    bm = up(K['building'], s)
    sh = shift(shift(bm, 2, 0, False), 0, 2, False) & ~bm
    paint(img, sh, '#5a4024', 0.22)
    paint(img, bm, '#b98458', 0.92)
    paint(img, erode(bm, 2) & (((xx + zz) % 6) == 0), '#8f6340', 0.25)  # light roof hatch
    paint(img, outline(bm, 2), INK, 0.9)

    # trees
    pil = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGB')
    dr = ImageDraw.Draw(pil, 'RGBA')
    step = 4
    fol = K['foliage']
    discs = []
    for gz in range(-1, d // step + 2):
        for gx in range(-1, w // step + 2):
            wx, wz = gx + x0 // step, gz + z0 // step  # world-anchored lattice so the pattern does not swim
            jx = (hash2(wx, wz, 21) - 0.5) * step * 0.8
            jz = (hash2(wx, wz, 22) - 0.5) * step * 0.8
            cx, cz = gx * step + step / 2 + jx, gz * step + step / 2 + jz
            ix, iz = int(cx), int(cz)
            if 0 <= ix < w and 0 <= iz < d and fol[iz, ix]:
                r = (1.7 + hash2(wx, wz, 23) * 0.9) * s
                discs.append((cz, cx, r))
    discs.sort()
    fill = tuple(int(c) for c in hex_rgb('#b9b07a')) + (255,)
    shade = tuple(int(c) for c in hex_rgb('#8f8a58')) + (255,)
    ink = tuple(int(c) for c in hex_rgb(INK_MID)) + (230,)
    for cz, cx, r in discs:
        px, pz = cx * s, cz * s
        dr.ellipse((px - r, pz - r, px + r, pz + r), fill=fill, outline=ink, width=1)
        dr.chord((px - r + 1.5, pz - r + 1.5, px + r - 1.5, pz + r - 1.5), 10, 120, fill=shade)
    return pil


def border(pil):
    dr = ImageDraw.Draw(pil)
    w, h = pil.size
    ink = tuple(int(c) for c in hex_rgb(INK))
    dr.rectangle((3, 3, w - 4, h - 4), outline=ink, width=2)
    dr.rectangle((8, 8, w - 9, h - 9), outline=ink, width=1)
    return pil


# ---------------------------------------------------------------- overlay mock-up

def font(names, size):
    for n in names:
        try:
            return ImageFont.truetype(n, size)
        except OSError:
            continue
    return ImageFont.load_default(size=size)


UI = ['segoeuib.ttf', 'arialbd.ttf']
UIR = ['segoeui.ttf', 'arial.ttf']
SERIF = ['georgiab.ttf', 'cambriab.ttf']
GOLD, VIOLET, NEUTRAL, TEAL, RED, ORANGE = (217, 164, 58), (122, 85, 199), (176, 168, 150), (56, 178, 168), (214, 48, 42), (240, 140, 30)


def text_o(dr, xy, txt, f, fill=(255, 255, 255), anchor='mm', stroke=(20, 16, 12), sw=2):
    dr.text(xy, txt, font=f, fill=fill, anchor=anchor, stroke_width=sw, stroke_fill=stroke)


def dashed(dr, a, b, colour, width=2, dash=8, gap=6, dot=False):
    (x0, y0), (x1, y1) = a, b
    L = math.hypot(x1 - x0, y1 - y0)
    ux, uy = (x1 - x0) / L, (y1 - y0) / L
    p = 0
    while p < L:
        if dot:
            cx, cy = x0 + ux * p, y0 + uy * p
            dr.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=(20, 16, 12))
            dr.ellipse((cx - 1.4, cy - 1.4, cx + 1.4, cy + 1.4), fill=colour)
            p += gap
        else:
            q = min(L, p + dash)
            seg = (x0 + ux * p, y0 + uy * p, x0 + ux * q, y0 + uy * q)
            dr.line(seg, fill=(20, 16, 12), width=width + 2)
            dr.line(seg, fill=colour, width=width)
            p = q + gap


def arrowhead(dr, tip, frm, colour, size=9):
    ang = math.atan2(tip[1] - frm[1], tip[0] - frm[0])
    pts = [tip] + [(tip[0] - size * math.cos(ang + s * 0.5), tip[1] - size * math.sin(ang + s * 0.5)) for s in (-1, 1)]
    dr.polygon(pts, fill=colour, outline=(20, 16, 12))


def shield(dr, cx, cy, sz, fill):
    pts = [(cx - sz, cy - sz), (cx + sz, cy - sz), (cx + sz, cy), (cx, cy + sz * 1.3), (cx - sz, cy)]
    dr.polygon(pts, fill=fill, outline=(20, 16, 12), width=2)


def sword(dr, cx, cy, sz, colour, ang=-45):
    a = math.radians(ang)
    ux, uy = math.cos(a), math.sin(a)
    tip = (cx + ux * sz, cy + uy * sz)
    hilt = (cx - ux * sz * 0.55, cy - uy * sz * 0.55)
    pom = (cx - ux * sz, cy - uy * sz)
    gx, gy = -uy * sz * 0.45, ux * sz * 0.45
    for wd, col in ((5, (20, 16, 12)), (3, colour)):
        dr.line((hilt, tip), fill=col, width=wd)
        dr.line(((hilt[0] - gx, hilt[1] - gy), (hilt[0] + gx, hilt[1] + gy)), fill=col, width=wd)
        dr.line((hilt, pom), fill=col, width=wd)


def overlay(base, m, box, look):
    W, H = 1280, 720
    TOP, BOT = 34, 58
    scr = Image.new('RGB', (W, H), (18, 17, 15))
    avail_w, avail_h = W - 24, H - TOP - BOT - 16
    k = min(avail_w / base.width, avail_h / base.height)
    mw, mh = int(base.width * k), int(base.height * k)
    mx, my = (W - mw) // 2, TOP + 8 + (avail_h - mh) // 2
    scr.paste(base.resize((mw, mh), Image.LANCZOS), (mx, my))
    dr = ImageDraw.Draw(scr, 'RGBA')
    dr.rectangle((mx - 2, my - 2, mx + mw + 1, my + mh + 1), outline=(90, 74, 50), width=2)
    bw = box[2] - box[0]

    def P(lx, lz):  # map-local block -> screen px
        return mx + (lx - box[0] + 0.5) * mw / bw, my + (lz - box[1] + 0.5) * mw / bw

    pts = {p['id']: P(p['x'], p['z']) for p in m['points']}
    f_big = font(UI, 17)
    f_sm = font(UI, 12)
    f_xs = font(UIR, 11)
    f_name = font(SERIF if look == 'parchment' else UI, 12)

    # order lines first (under markers)
    groups = [('INF 10', P(226, 146), pts['B'], 1.0), ('ARC 8', P(150, 238), pts['A'], 0.55)]
    for _, g, tgt, _ in groups:
        dashed(dr, g, (tgt[0] - 14, tgt[1] + 10), GOLD)
        arrowhead(dr, (tgt[0] - 14, tgt[1] + 10), g, GOLD)
    bot = P(300, 360)
    dashed(dr, bot, (pts['D'][0] + 6, pts['D'][1] + 18), TEAL, dot=True, gap=7)
    arrowhead(dr, (pts['D'][0] + 6, pts['D'][1] + 18), bot, TEAL)

    # points
    owner = {'A': GOLD, 'B': NEUTRAL, 'C': GOLD, 'D': NEUTRAL, 'E': VIOLET}
    for p in m['points']:
        cx, cy = pts[p['id']]
        rr = p['radius'] * mw / bw
        dr.ellipse((cx - rr, cy - rr, cx + rr, cy + rr), outline=owner[p['id']] + (170,), width=2)
        r = 13
        dr.ellipse((cx - r - 2, cy - r - 2, cx + r + 2, cy + r + 2), fill=(20, 16, 12))
        dr.ellipse((cx - r, cy - r, cx + r, cy + r), fill=owner[p['id']])
        if p['id'] == 'C':  # contested: Dusk is taking it, 62 %
            dr.arc((cx - r - 6, cy - r - 6, cx + r + 6, cy + r + 6), 0, 360, fill=(20, 16, 12), width=6)
            dr.arc((cx - r - 5, cy - r - 5, cx + r + 5, cy + r + 5), -90, -90 + 360 * 0.62, fill=VIOLET, width=4)
        tc = (20, 16, 12) if owner[p['id']] != VIOLET else (255, 255, 255)
        dr.text((cx, cy + 1), p['id'], font=f_big, fill=tc, anchor='mm')
        text_o(dr, (cx, cy + r + 13), p['name'], f_name, fill=(255, 246, 225))

    # plan flags
    def plan_tag(x, y, glyph, label, col):  # a pennant on a short pole planted next to the point
        tw = dr.textlength(label, font=f_sm)
        dr.line((x, y + 26, x, y - 2), fill=(20, 16, 12), width=3)
        dr.line((x, y + 26, x, y - 2), fill=col, width=1)
        dr.polygon([(x, y - 2), (x + tw + 36, y - 2), (x + tw + 28, y + 9), (x + tw + 36, y + 20), (x, y + 20)],
                   fill=(24, 20, 16, 230), outline=col)
        if glyph == 'shield':
            shield(dr, x + 12, y + 7, 6, col)
        else:
            sword(dr, x + 12, y + 9, 8, col)
        dr.text((x + 24, y + 9), label, font=f_sm, fill=col, anchor='lm')
    ax, ay = pts['A']
    plan_tag(ax - 6, ay - 52, 'shield', 'DEFEND', GOLD)
    bx, by = pts['B']
    plan_tag(bx + 12, by - 44, 'sword', 'ATTACK', (240, 236, 226))

    # popup at D (as if just clicked)
    dx_, dy_ = pts['D']
    pw, ph = 98, 104
    px, py = dx_ + 42, dy_ - ph / 2 - 8
    dr.polygon([(dx_ + 22, dy_), (px + 1, dy_ - 8), (px + 1, dy_ + 8)], fill=(200, 170, 110))
    dr.rounded_rectangle((px, py, px + pw, py + ph), 5, fill=(32, 28, 22, 235), outline=(200, 170, 110), width=1)
    text_o(dr, (px + pw / 2, py + 11), 'Mill Rise', f_sm, fill=(255, 240, 210), sw=0)
    for i, (lab, col) in enumerate((('Attack', (240, 236, 226)), ('Defend', GOLD), ('Hold', (170, 200, 230)), ('Clear', (170, 160, 150)))):
        y = py + 22 + i * 20
        dr.rounded_rectangle((px + 6, y, px + pw - 6, y + 17), 3, fill=(62, 54, 42) if i else (92, 76, 50), outline=(110, 96, 74))
        if i == 0:
            sword(dr, px + 16, y + 9, 5, col)
        elif i == 1:
            shield(dr, px + 16, y + 7, 4, col)
        elif i == 2:
            dr.rectangle((px + 12, y + 4, px + 20, y + 13), fill=col, outline=(20, 16, 12))
        else:
            dr.line((px + 12, y + 4, px + 20, y + 13), fill=col, width=2)
            dr.line((px + 20, y + 4, px + 12, y + 13), fill=col, width=2)
        dr.text((px + 28, y + 8), lab, font=f_sm, fill=(245, 238, 222), anchor='lm')

    # enemies: fresh red triangles and one faded last-seen
    for (lx, lz), fresh in (((330, 196), True), ((338, 186), True), ((334, 206), True), ((452, 120), True), ((236, 104), False)):
        x, y = P(lx, lz)
        tri = [(x, y - 8), (x - 7, y + 5), (x + 7, y + 5)]
        if fresh:
            dr.polygon(tri, fill=RED, outline=(20, 16, 12), width=2)
        else:
            dr.polygon(tri, fill=(214, 48, 42, 90), outline=(20, 16, 12, 150), width=3)
            dr.polygon(tri, outline=(240, 120, 110, 200), width=1)
            text_o(dr, (x, y + 14), '?', f_xs, fill=(230, 200, 190), sw=1)

    # own groups: plaques with health bars
    for label, (gx, gy), _, hp in groups:
        tw = dr.textlength(label, font=f_sm)
        x0, y0 = gx - tw / 2 - 7, gy - 10
        dr.rounded_rectangle((x0 - 1, y0 - 1, x0 + tw + 15, y0 + 23), 4, fill=(20, 16, 12))
        dr.rounded_rectangle((x0, y0, x0 + tw + 14, y0 + 22), 3, fill=GOLD)
        dr.text((gx, y0 + 9), label, font=f_sm, fill=(25, 18, 8), anchor='mm')
        dr.rectangle((x0 + 3, y0 + 17, x0 + tw + 11, y0 + 20), fill=(60, 40, 20))
        dr.rectangle((x0 + 3, y0 + 17, x0 + 3 + (tw + 8) * hp, y0 + 20), fill=(90, 200, 90) if hp > 0.6 else (230, 170, 40))
    # fight marker on ARC 8 (fighting near A)
    fx, fy = P(150, 238)[0] + 32, P(150, 238)[1] - 2
    dr.ellipse((fx - 11, fy - 11, fx + 11, fy + 11), fill=(20, 16, 12, 200), outline=ORANGE, width=2)
    sword(dr, fx, fy, 7, ORANGE, -45)
    sword(dr, fx, fy, 7, ORANGE, -135)
    # bot army
    bx2, by2 = bot
    lab = 'Brann (bot) 12'
    tw = dr.textlength(lab, font=f_sm)
    dr.rounded_rectangle((bx2 - tw / 2 - 8, by2 - 11, bx2 + tw / 2 + 8, by2 + 11), 4, fill=(16, 30, 30, 215), outline=TEAL, width=2)
    dr.text((bx2, by2), lab, font=f_sm, fill=(200, 245, 238), anchor='mm')

    # ping
    qx, qy = P(258, 140)
    for rr, a in ((16, 110), (10, 220)):
        dr.ellipse((qx - rr, qy - rr, qx + rr, qy + rr), outline=(255, 255, 255, a), width=2)
    dr.ellipse((qx - 3, qy - 3, qx + 3, qy + 3), fill=(255, 255, 255))

    # you: white arrow heading north-east
    yx, yy = P(118, 200)
    ang = math.radians(-35)

    def rot(px_, py_):
        return yx + px_ * math.cos(ang) - py_ * math.sin(ang), yy + px_ * math.sin(ang) + py_ * math.cos(ang)
    arrow = [rot(12, 0), rot(-8, -8), rot(-3, 0), rot(-8, 8)]
    dr.polygon(arrow, fill=(255, 255, 255), outline=(20, 16, 12), width=2)

    # top bar
    dr.rectangle((0, 0, W, TOP), fill=(14, 13, 11, 245))
    dr.line((0, TOP, W, TOP), fill=(90, 74, 50), width=1)
    f_bar = font(UI, 16)
    text_o(dr, (16, TOP / 2), 'DOMINATION', f_bar, fill=(235, 225, 205), anchor='lm', sw=0)
    cx = W / 2
    text_o(dr, (cx - 70, TOP / 2), 'Dawn 212', f_bar, fill=GOLD, anchor='rm', sw=0)
    text_o(dr, (cx, TOP / 2), '—', f_bar, fill=(200, 190, 170), sw=0)
    text_o(dr, (cx + 70, TOP / 2), '188 Dusk', f_bar, fill=(170, 140, 235), anchor='lm', sw=0)
    text_o(dr, (cx + 190, TOP / 2), '9:41', f_bar, fill=(235, 225, 205), anchor='lm', sw=0)
    # toggle
    tx = W - 170
    dr.rounded_rectangle((tx, 6, W - 12, TOP - 6), 10, fill=(40, 36, 30), outline=(110, 96, 74))
    half = (W - 12 - tx) / 2
    sel = 0 if look == 'parchment' else 1
    dr.rounded_rectangle((tx + sel * half + 2, 8, tx + (sel + 1) * half - 2, TOP - 8), 8, fill=(200, 170, 110))
    for i, lab in enumerate(('Parchment', 'Real')):
        dr.text((tx + i * half + half / 2, TOP / 2), lab, font=f_sm, anchor='mm',
                fill=(25, 18, 8) if i == sel else (200, 190, 170))

    # bottom bar
    y0 = H - BOT
    dr.rectangle((0, y0, W, H), fill=(14, 13, 11, 245))
    dr.line((0, y0, W, y0), fill=(90, 74, 50), width=1)
    x = 14
    for key, lab, glyph in (('1', 'Attack here', 'sword'), ('2', 'Defend here', 'shield'), ('3', 'Enemy spotted', 'tri'), ('4', 'Ping', 'ring')):
        bw_ = 30 + dr.textlength(lab, font=f_sm) + 26
        dr.rounded_rectangle((x, y0 + 12, x + bw_, y0 + 44), 5, fill=(40, 36, 30), outline=(110, 96, 74))
        gx_, gy_ = x + 16, y0 + 28
        if glyph == 'sword':
            sword(dr, gx_, gy_, 8, (240, 236, 226))
        elif glyph == 'shield':
            shield(dr, gx_, gy_ - 2, 6, GOLD)
        elif glyph == 'tri':
            dr.polygon([(gx_, gy_ - 7), (gx_ - 7, gy_ + 5), (gx_ + 7, gy_ + 5)], fill=RED, outline=(20, 16, 12))
        else:
            dr.ellipse((gx_ - 7, gy_ - 7, gx_ + 7, gy_ + 7), outline=(255, 255, 255), width=2)
        dr.text((x + 30, gy_), lab, font=f_sm, fill=(235, 225, 205), anchor='lm')
        dr.text((x + bw_ - 8, gy_), key, font=f_xs, fill=(150, 140, 120), anchor='rm')
        x += bw_ + 8
    # legend
    lx = x + 24
    items = [('disc', GOLD, 'Ours'), ('disc', VIOLET, 'Theirs'), ('disc', NEUTRAL, 'Neutral'), ('plaque', GOLD, 'Group'),
             ('oplaque', TEAL, 'Ally bot'), ('tri', RED, 'Enemy'), ('ftri', RED, 'Last seen'), ('fight', ORANGE, 'Fight'), ('you', (255, 255, 255), 'You')]
    for kind_, col, lab in items:
        gx_, gy_ = lx + 7, y0 + 29
        if kind_ == 'disc':
            dr.ellipse((gx_ - 7, gy_ - 7, gx_ + 7, gy_ + 7), fill=col, outline=(20, 16, 12), width=2)
        elif kind_ in ('plaque', 'oplaque'):
            dr.rounded_rectangle((gx_ - 8, gy_ - 5, gx_ + 8, gy_ + 5), 2, fill=col if kind_ == 'plaque' else (16, 30, 30), outline=col if kind_ == 'oplaque' else (20, 16, 12), width=2)
        elif kind_ == 'tri':
            dr.polygon([(gx_, gy_ - 7), (gx_ - 7, gy_ + 5), (gx_ + 7, gy_ + 5)], fill=col, outline=(20, 16, 12))
        elif kind_ == 'ftri':
            dr.polygon([(gx_, gy_ - 7), (gx_ - 7, gy_ + 5), (gx_ + 7, gy_ + 5)], fill=(214, 48, 42, 70), outline=(214, 48, 42, 160))
        elif kind_ == 'fight':
            sword(dr, gx_, gy_, 6, col, -45)
            sword(dr, gx_, gy_, 6, col, -135)
        else:
            dr.polygon([(gx_ + 8, gy_), (gx_ - 6, gy_ - 6), (gx_ - 2, gy_), (gx_ - 6, gy_ + 6)], fill=col, outline=(20, 16, 12))
        dr.text((lx + 18, gy_), lab, font=f_xs, fill=(215, 205, 185), anchor='lm')
        lx += 26 + dr.textlength(lab, font=f_xs) + 6
    return scr


# ---------------------------------------------------------------- main

def main(map_dir=MAP_DIR, out_dir='.'):
    out = pathlib.Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    t0 = time.perf_counter()
    m, ids, height, colour = scan(pathlib.Path(map_dir), Looks())
    t_scan = time.perf_counter()
    box = field_box(m)
    sl = (slice(box[1], box[3]), slice(box[0], box[2]))
    ids, height, colour = ids[sl], height[sl], colour[sl]

    real = render_real(height, colour, ids, S)
    t_real = time.perf_counter()
    table = load_kinds(KINDS)
    kind, flat, code = classify(ids, table)
    kind, t = resolve(height, kind, flat, code, table['raise'])
    t_kind = time.perf_counter()
    parch = border(render_parchment(height, kind, t, code, S, box))
    t_parch = time.perf_counter()

    real.save(out / 'real_plain.png', optimize=True)
    parch.save(out / 'parchment_plain.png', optimize=True)
    real.resize((real.width // S, real.height // S), Image.NEAREST).save(out / 'real_plain_1px.png')
    parch.resize((parch.width // S, parch.height // S), Image.LANCZOS).save(out / 'parchment_plain_1px.png')
    for name, img in (('real', real), ('parchment', parch)):
        overlay(img, m, box, name).save(out / f'{name}_overlay.jpg', quality=85, optimize=True)
    t_end = time.perf_counter()
    counts = {k: int((kind == i).sum()) for k, i in code.items()}
    print(f'field {kind.shape[1]} x {kind.shape[0]} columns; kinds {counts}')
    print(f'scan {t_scan - t0:.2f}s  real {t_real - t_scan:.2f}s  kinds {t_kind - t_real:.2f}s  parchment {t_parch - t_kind:.2f}s  '
          f'save+overlays {t_end - t_parch:.2f}s  total {t_end - t0:.2f}s')


if __name__ == '__main__':
    main(*sys.argv[1:])
