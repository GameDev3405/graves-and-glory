"""Review images of an offline world: top-down (per-block colour, height and slope shading, a 16-block grid with
coordinate labels), 2:1 isometric (three-face shading, model shapes), and diffs of two worlds or of a world against an
undo record (added, removed and changed cells). Needs Pillow; everything else in mapforge is stdlib only."""
import statistics

from .colours import FULL, INVISIBLE, UNKNOWN, Palette
from .structure import parse_state
from .world import AIR_CELL, Cell

BACKGROUND = (24, 26, 31)
VOID_CHECK = (40, 43, 51)
CAPTION_BG, CAPTION_FG = (14, 15, 18), (225, 228, 232)
GRID_ALPHA = 64
KINDS = {'added': (46, 204, 64), 'removed': (255, 65, 54), 'changed': (255, 176, 0)}
TOP_SHADE, SOUTH_SHADE, EAST_SHADE = 1.0, 0.80, 0.62


class RenderError(Exception):
    pass


def require_pillow():
    try:
        from PIL import Image, ImageDraw, ImageFont
    except ImportError:
        raise RenderError('render needs Pillow (python -m pip install pillow); the other mapforge commands are stdlib only')
    return Image, ImageDraw, ImageFont


def _font(size):
    _, _, ImageFont = require_pillow()
    try:
        return ImageFont.load_default(size=size)
    except TypeError:  # Pillow before 10.1 has only the fixed bitmap font
        return ImageFont.load_default()


def _shade(rgb, f):
    return tuple(max(0, min(255, round(c * f))) for c in rgb)


def _mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def _mute(rgb):
    """Unchanged cells in a diff: pale grey that keeps the light and dark of the block."""
    grey = round(0.3 * rgb[0] + 0.59 * rgb[1] + 0.11 * rgb[2])
    return _mix(_mix(rgb, (grey, grey, grey), 0.75), (200, 200, 200), 0.35)


# --- block looks ---

class Looks:
    """State string -> (top rgb, side rgb, box in 1/16, full opaque cube) with state properties applied: logs on
    their side swap bark and rings, slabs and trapdoors sit high or double, snow layers stack."""

    def __init__(self, palette=None):
        self.palette = palette or Palette()
        self._cache = {}

    def get(self, state):
        look = self._cache.get(state)
        if look is None:
            name, props = parse_state(state)
            if name in INVISIBLE:
                look = None
            else:
                top, side, box = self.palette.get(name)
                box = list(box)
                if props.get('axis') in ('x', 'z'):
                    top, side = side, top
                if props.get('type') == 'double':
                    box = list(FULL)
                elif props.get('type') == 'top' or (props.get('half') == 'top' and box[4] - box[1] < 8):
                    h = box[4] - box[1]
                    box[1], box[4] = 16 - h, 16
                if 'layers' in props and name.endswith(':snow'):
                    box[4] = 2 * int(props['layers'])
                if props.get('open') == 'true' and name.endswith('trapdoor'):
                    box = [0, 0, 13, 16, 16, 16]
                look = (top, side, tuple(box), tuple(box) == FULL and (top, side) != (UNKNOWN, UNKNOWN))
            self._cache[state] = look or False
        return look or None


def unknown_blocks(world, lo, hi, palette):
    counts = {}
    for pos, cell in world.blocks.items():
        if _inside(pos, lo, hi):
            name = cell.state.split('[', 1)[0]
            if name not in INVISIBLE and name not in palette:
                counts[name] = counts.get(name, 0) + 1
    return dict(sorted(counts.items()))


# --- boxes ---

def _inside(pos, lo, hi):
    return all(lo[i] <= pos[i] <= hi[i] for i in range(3))


def crop_box(worlds, lo=None, hi=None):
    """Inclusive (lo, hi) from optional (x, z) or (x, y, z) bounds; missing parts come from the worlds' bounds."""
    bounds = [w.bounds() for w in worlds if w.bounds()]
    if not bounds:
        raise RenderError('nothing to render: the world is empty')
    blo = tuple(min(b[0][i] for b in bounds) for i in range(3))
    bhi = tuple(max(b[1][i] for b in bounds) for i in range(3))

    def full(v, default):
        if v is None:
            return default
        v = tuple(v)
        if len(v) == 2:
            return (v[0], default[1], v[1])
        if len(v) == 3:
            return v
        raise RenderError('--min and --max take X Z or X Y Z')
    lo, hi = full(lo, blo), full(hi, bhi)
    if any(lo[i] > hi[i] for i in range(3)):
        raise RenderError(f'empty render box {lo}..{hi}')
    return lo, hi


def _columns(world, lo, hi, looks):
    """(x, z) -> [(y, look), ...] from the top down, only visible blocks inside the box."""
    cols = {}
    for (x, y, z), cell in world.blocks.items():
        if lo[0] <= x <= hi[0] and lo[1] <= y <= hi[1] and lo[2] <= z <= hi[2]:
            look = looks.get(cell.state)
            if look:
                cols.setdefault((x, z), []).append((y, look))
    for v in cols.values():
        v.sort(key=lambda t: -t[0])
    return cols


def _small(box):
    return box[3] - box[0] < 12 or box[5] - box[2] < 12


def _surface(column):
    """(ground look, ground top height, small top look or None) of a column; a post, plant or lantern on top is
    drawn inset over the first full-footprint block below it."""
    top = column[0]
    for y, look in column:
        if not _small(look[2]):
            return look, y + look[2][4] / 16, (top[1] if top[1] is not look else None)
    return top[1], top[0] + top[1][2][4] / 16, None


def _heights(cols):
    return {k: _surface(v)[1] for k, v in cols.items()}


def _height_ref(heights):
    return statistics.median(heights.values()) if heights else 0


def _height_factor(h, ref):
    return max(0.72, min(1.28, 1 + 0.035 * (h - ref)))


def auto_scale(lo, hi, view):
    w, d = hi[0] - lo[0] + 1, hi[2] - lo[2] + 1
    if view == 'iso':
        t = max(2, min(16, 1400 // (w + d)))
        return t - t % 2
    return max(1, min(16, 768 // max(w, d)))


# --- captions ---

def _caption(img, lines, legend=()):
    Image, ImageDraw, _ = require_pillow()
    font = _font(13)
    line_h = 18
    height = 8 + line_h * (len(lines) + (1 if legend else 0))
    probe = ImageDraw.Draw(img)
    text_w = max([probe.textlength(line, font=font) for line in lines] +
                 [sum(24 + probe.textlength(t, font=font) for _, t in legend)])
    out = Image.new('RGB', (max(img.width, int(text_w) + 16), img.height + height), CAPTION_BG)
    out.paste(img, (0, height))
    draw = ImageDraw.Draw(out)
    y = 4
    for line in lines:
        draw.text((8, y), line, fill=CAPTION_FG, font=font)
        y += line_h
    x = 8
    for colour, text in legend:
        draw.rectangle((x, y + 3, x + 11, y + 14), fill=colour)
        draw.text((x + 16, y), text, fill=CAPTION_FG, font=font)
        x += 24 + int(draw.textlength(text, font=font))
    return out


def _describe(lo, hi):
    return f'x {lo[0]}..{hi[0]}  z {lo[2]}..{hi[2]}  y {lo[1]}..{hi[1]}'


# --- top-down ---

def topdown(world, lo, hi, scale=None, grid=True, looks=None, kinds=None, title=None):
    """Top-down image, north up, east right. kinds (x, z) -> 'added' | 'removed' | 'changed' turns it into a diff:
    other columns are muted and these are washed with their kind colour."""
    Image, ImageDraw, _ = require_pillow()
    looks = looks or Looks()
    s = scale or auto_scale(lo, hi, 'top')
    w, d = hi[0] - lo[0] + 1, hi[2] - lo[2] + 1
    cols = _columns(world, lo, hi, looks)
    heights = _heights(cols)
    surfaces = {k: _surface(v)[0] for k, v in cols.items()}
    ref = _height_ref(heights)
    margin = 30 if grid else 0
    img = Image.new('RGB', (w * s + margin, d * s + margin), BACKGROUND)
    draw = ImageDraw.Draw(img)
    inset = max(1, s // 4) if s >= 4 else 0
    for x in range(lo[0], hi[0] + 1):  # empty columns: a dark checker, so a hole never reads as a black block
        for z in range(lo[2], hi[2] + 1):
            if (x, z) not in cols and (x + z) % 2:
                px, py = margin + (x - lo[0]) * s, margin + (z - lo[2]) * s
                draw.rectangle((px, py, px + s - 1, py + s - 1), fill=VOID_CHECK)
    for (x, z), column in cols.items():
        ground, h, small = _surface(column)
        hn, hw = heights.get((x, z - 1), h), heights.get((x - 1, z), h)
        slope = max(-3.0, min(3.0, (h - hn) + (h - hw)))
        f = _height_factor(h, ref) * (1 + 0.09 * slope)
        colour = _shade(ground[0], f)
        smallc = _shade(small[0], _height_factor(h, ref)) if small else None
        kind = kinds.get((x, z)) if kinds is not None else None
        if kinds is not None:
            colour = _mix(colour, KINDS[kind], 0.62) if kind else _mute(colour)
            smallc = smallc and (_mix(smallc, KINDS[kind], 0.62) if kind else _mute(smallc))
        px, py = margin + (x - lo[0]) * s, margin + (z - lo[2]) * s
        draw.rectangle((px, py, px + s - 1, py + s - 1), fill=colour)
        if s >= 6:  # a hairline where the surface block changes, so similar colours (gravel, cobblestone) still part
            edge = _shade(colour, 0.78)
            for (nx, nz), line in (((x - 1, z), (px, py, px, py + s - 1)), ((x, z - 1), (px, py, px + s - 1, py))):
                other = surfaces.get((nx, nz))
                if other is not None and other is not ground:
                    draw.line(line, fill=edge)
        if smallc:
            if inset:
                draw.rectangle((px + inset, py + inset, px + s - 1 - inset, py + s - 1 - inset), fill=smallc)
            else:
                draw.point((px, py), fill=smallc)
    if kinds is not None:  # removed columns that are now empty
        for (x, z), kind in kinds.items():
            if (x, z) not in cols and lo[0] <= x <= hi[0] and lo[2] <= z <= hi[2]:
                px, py = margin + (x - lo[0]) * s, margin + (z - lo[2]) * s
                draw.rectangle((px, py, px + s - 1, py + s - 1), fill=_mix(BACKGROUND, KINDS[kind], 0.62))
    if grid:
        _grid(img, lo, hi, s, margin)
    lines = [title or world.name, f'top-down  {_describe(lo, hi)}  {s} px/block  north up']
    legend = ()
    if kinds is not None:
        counts = {k: sum(1 for v in kinds.values() if v == k) for k in KINDS}
        legend = [(KINDS[k], f'{k} {counts[k]} col') for k in KINDS]
    return _caption(img, lines, legend)


def _grid(img, lo, hi, s, margin):
    Image, ImageDraw, _ = require_pillow()
    font = _font(11)
    over = Image.new('RGBA', img.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(over)
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 0, img.width, margin - 1), fill=CAPTION_BG)
    draw.rectangle((0, 0, margin - 1, img.height), fill=CAPTION_BG)
    for x in range(lo[0], hi[0] + 2):
        if x % 16 == 0:
            px = margin + (x - lo[0]) * s
            od.line((px, margin, px, img.height), fill=(255, 255, 255, GRID_ALPHA), width=1)
            od.line((px, margin - 6, px, margin - 1), fill=(255, 255, 255, 200))
            draw.text((px + 2, 4), str(x), fill=CAPTION_FG, font=font)
    for z in range(lo[2], hi[2] + 2):
        if z % 16 == 0:
            py = margin + (z - lo[2]) * s
            od.line((margin, py, img.width, py), fill=(255, 255, 255, GRID_ALPHA), width=1)
            od.line((margin - 6, py, margin - 1, py), fill=(255, 255, 255, 200))
            draw.text((2, py + 2), str(z), fill=CAPTION_FG, font=font)
    img.paste(Image.alpha_composite(img.convert('RGBA'), over).convert('RGB'))


# --- isometric ---

_NEIGHBOURS = {'top': (0, 1, 0), 'south': (0, 0, 1), 'east': (1, 0, 0)}


def isometric(world, lo, hi, scale=None, looks=None, kinds=None, ghosts=None, title=None):
    """2:1 isometric view from the south-east (north-west is at the back), top, south and east faces shaded.
    kinds pos -> kind tints those blocks and mutes the rest; ghosts pos -> Cell draws removed blocks as outlines."""
    Image, ImageDraw, _ = require_pillow()
    looks = looks or Looks()
    t = scale or auto_scale(lo, hi, 'iso')
    cells = []
    full = set()
    for pos, cell in world.blocks.items():
        if _inside(pos, lo, hi):
            look = looks.get(cell.state)
            if look:
                cells.append((pos, look, None))
                if look[3]:
                    full.add(pos)
    for pos, cell in (ghosts or {}).items():
        look = looks.get(cell.state)
        if look and _inside(pos, lo, hi) and pos not in full:
            cells.append((pos, look, 'ghost'))
    if not cells:
        raise RenderError('nothing to render in the box')
    ys = [p[1] for p, _, _ in cells]
    ref = statistics.median(ys)
    y0, y1 = min(ys), max(ys) + 1

    def proj(x, y, z):
        return ((x - z) * t, (x + z) * t / 2 - y * t)
    ox = -proj(lo[0], 0, hi[2] + 1)[0] + 12
    oy = -((lo[0] + lo[2]) * t / 2 - y1 * t) + 12
    width = round((hi[0] + 1 - lo[0] + hi[2] + 1 - lo[2]) * t) + 24
    height = round((hi[0] + 1 - lo[0] + hi[2] + 1 - lo[2]) * t / 2 + (y1 - y0) * t) + 24
    img = Image.new('RGB', (width, height), BACKGROUND)
    draw = ImageDraw.Draw(img)

    def pt(x, y, z):
        px, py = proj(x, y, z)
        return (px + ox, py + oy)
    cells.sort(key=lambda c: (c[0][0] + c[0][1] + c[0][2], c[0][1], c[0][0]))
    edge = t >= 6
    for (x, y, z), (top, side, box, _), mode in cells:
        hidden = {face for face, (dx, dy, dz) in _NEIGHBOURS.items() if (x + dx, y + dy, z + dz) in full}
        if mode is None and len(hidden) == 3:
            continue
        bx0, by0, bz0 = x + box[0] / 16, y + box[1] / 16, z + box[2] / 16
        bx1, by1, bz1 = x + box[3] / 16, y + box[4] / 16, z + box[5] / 16
        faces = {
            'top': ([pt(bx0, by1, bz0), pt(bx1, by1, bz0), pt(bx1, by1, bz1), pt(bx0, by1, bz1)], top, TOP_SHADE),
            'south': ([pt(bx0, by0, bz1), pt(bx1, by0, bz1), pt(bx1, by1, bz1), pt(bx0, by1, bz1)], side, SOUTH_SHADE),
            'east': ([pt(bx1, by0, bz0), pt(bx1, by0, bz1), pt(bx1, by1, bz1), pt(bx1, by1, bz0)], side, EAST_SHADE),
        }
        kind = kinds.get((x, y, z)) if kinds is not None else None
        hf = _height_factor(y, ref)
        for face, (poly, rgb, shade) in faces.items():
            full_face = (face == 'top' and box[4] == 16) or (face == 'south' and box[5] == 16) or (face == 'east' and box[3] == 16)
            if face in hidden and full_face and mode is None:
                continue
            if mode == 'ghost':
                draw.polygon(poly, outline=_shade(KINDS['removed'], shade))
                continue
            colour = _shade(rgb, shade * hf)
            if kinds is not None:
                colour = _mix(colour, KINDS[kind], 0.55) if kind else _mute(colour)
            draw.polygon(poly, fill=colour, outline=_shade(colour, 0.84) if edge else None)
    lines = [title or world.name, f'isometric  {_describe(lo, hi)}  {t} px/half-tile  viewed from the south-east']
    legend = ()
    if kinds is not None:
        counts = {k: sum(1 for v in kinds.values() if v == k) for k in KINDS}
        legend = [(KINDS[k], f'{k} {counts[k]}') for k in KINDS]
    return _caption(img, lines, legend)


# --- diffs ---

def diff_cells(before, after, lo, hi):
    """pos -> 'added' (air to block), 'removed' (block to air) or 'changed' (one block to another)."""
    out = {}
    for pos in set(before.blocks) | set(after.blocks):
        if not _inside(pos, lo, hi):
            continue
        b, a = before.get(pos), after.get(pos)
        if a != b:
            out[pos] = 'added' if b == AIR_CELL else 'removed' if a == AIR_CELL else 'changed'
    return out


def column_kinds(before, after, cells, looks, lo, hi):
    """(x, z) -> kind for top-down: the surface rose (added), fell (removed) or kept its height (changed)."""
    cols_b, cols_a = _heights(_columns(before, lo, hi, looks)), _heights(_columns(after, lo, hi, looks))
    out = {}
    for x, _, z in cells:
        if (x, z) in out:
            continue
        hb, ha = cols_b.get((x, z), lo[1]), cols_a.get((x, z), lo[1])
        out[(x, z)] = 'added' if ha > hb else 'removed' if ha < hb else 'changed'
    return out


def worlds_from_undo(world, undo):
    """(before, after) rebuilt from an undo record on top of world, whichever side world is on now."""
    before, after = world.copy(), world.copy()
    for row in undo['changes']:
        pos = tuple(row[:3])
        before.set(pos, Cell.from_json(row[3]))
        after.set(pos, Cell.from_json(row[4]))
    return before, after


def render(world, view='top', lo=None, hi=None, scale=None, grid=True, before=None, palette=None, title=None):
    """One PIL image. With before, a diff of before -> world."""
    require_pillow()
    looks = Looks(palette)
    lo, hi = crop_box([world] + ([before] if before else []), lo, hi)
    if before is None:
        if view == 'iso':
            return isometric(world, lo, hi, scale, looks, title=title)
        return topdown(world, lo, hi, scale, grid, looks, title=title)
    cells = diff_cells(before, world, lo, hi)
    title = title or f'{world.name}: diff'
    if view == 'iso':
        ghosts = {p: before.get(p) for p, k in cells.items() if k == 'removed'}
        return isometric(world, lo, hi, scale, looks, kinds=cells, ghosts=ghosts, title=title)
    return topdown(world, lo, hi, scale, grid, looks, kinds=column_kinds(before, world, cells, looks, lo, hi), title=title)


def undo_renders(undo_path, undo, world_after, margin=8):
    """Before, after and diff images written beside an undo record; the box is the changed area plus margin."""
    require_pillow()
    import pathlib
    before, after = worlds_from_undo(world_after, undo)
    if not undo['changes']:
        return []
    xs = [r[0] for r in undo['changes']]
    zs = [r[2] for r in undo['changes']]
    lo, hi = crop_box([before, after], (min(xs) - margin, min(zs) - margin), (max(xs) + margin, max(zs) + margin))
    stem = pathlib.Path(undo_path)
    stem = stem.with_name(stem.name[:-5] if stem.name.endswith('.json') else stem.name)
    jobs = [('before-top', before, 'top', None), ('after-top', after, 'top', None),
            ('diff-top', after, 'top', before), ('before-iso', before, 'iso', None), ('after-iso', after, 'iso', None),
            ('diff-iso', after, 'iso', before)]
    out = []
    for suffix, w, view, b in jobs:
        path = stem.with_name(f'{stem.name}.{suffix}.png')
        render(w, view, lo, hi, before=b, title=f'{undo["plan"]}: {suffix}').save(path)
        out.append(path)
    return out


def contact_sheet(rows, title=None, gap=12):
    """One image of rows of images (a list of lists), each row left-aligned, with an optional title strip."""
    Image, ImageDraw, _ = require_pillow()
    widths = [sum(i.width for i in row) + gap * (len(row) + 1) for row in rows]
    heights = [max(i.height for i in row) for row in rows]
    top = 30 if title else 0
    sheet = Image.new('RGB', (max(widths), top + sum(heights) + gap * (len(rows) + 1)), CAPTION_BG)
    if title:
        ImageDraw.Draw(sheet).text((gap, 8), title, fill=CAPTION_FG, font=_font(15))
    y = top + gap
    for row, h in zip(rows, heights):
        x = gap
        for img in row:
            sheet.paste(img, (x, y))
            x += img.width + gap
        y += h + gap
    return sheet
