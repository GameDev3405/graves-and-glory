"""AI handoff: export a bundle a Claude session can read, and merge its draft back in as proposals.

Locking (spec, AI draft rules): an object Kort placed, edited or accepted (design.locked) is never changed or
deleted by a draft; such edits are refused with a message and listed in the design's `review`. A draft that is
not a valid design is refused whole. Every object the draft adds or changes comes in as by ai, state proposed.
"""
import copy
import json
import math
import pathlib
import struct
import zlib

import numpy as np

from mapengine import design
from mapengine.terrain import NO_WATER, heightfield

SCALE = 2        # px a block in top.png
NOTE_SCALE = 4   # px a block in notes/<id>.png
NOTE_PAD = 16    # blocks around a note's radius in its crop
GRID = 50        # blocks between grid lines in the pictures
TOP_KEYS = ('name', 'width', 'depth', 'baseY', 'brief')

DRAFT_RULES = """\
## How to answer

1. Read `design.json` (the source of truth), `top.png` and the crops in `notes/`.
2. Write `draft.json` in this folder: a full copy of `design.json` with your additions.
3. Add detail as new objects with new ids (`<kind>-<n>`, n above any id in use). Set `"by": "ai"` and
   `"state": "proposed"` (the import sets them anyway).
4. **Locked:** every object with `"by": "kort"` or `"state": "accepted"` is Kort's. Leave it exactly as it is and
   do not delete it. Changes to it are refused. You may change or drop your own proposals.
5. Do not change `id`, `name`, `width`, `depth`, `baseY` or `brief`. Leave `review` out.
6. The file must pass the schema below, or the whole draft is refused.

Kort then reviews each proposal in the editor: accept, reject or edit.
"""

SCHEMA = """\
## Schema (format 1)

Coordinates are blocks on the canvas, 0 <= x < width, 0 <= z < depth; heights are relative to baseY.
Every object has `id` (unique), `kind`, `by` (kort or ai) and `state` (accepted or proposed).

| kind | fields |
| --- | --- |
| hill | `outline` [[x, z], ...] (3 or more), `height` integer -40..120 (negative digs), `steepness` 0..1 |
| river | `path` [[x, z], ...] (2 or more), `width` 1..64, `depth` 1..12 |
| road | `path` [[x, z], ...] (2 or more), `width` 1..16 |
| point | `x`, `z`, `name` (unique), `radius` 2..32 |
| spawn | `x`, `z`, `team` dawn or dusk |
| zone | `outline` [[x, z], ...] (3 or more), `palette` one of: {palettes} |
| note | `x`, `z`, `radius` 4..200, `text` (Kort's notes; do not add any) |
| building | `piece` a library id below, integer `x`, `z` (the piece's centre block), `rotation` 0, 90, 180 or 270 (clockwise) |

## Building library

Use only these approved pieces; do not invent buildings. Footprint is x by z at rotation 0 (90 and 270 swap
them). Door is the side the entrance faces at rotation 0, turned clockwise with the piece; keep the cells in
front of it clear and walkable. Footprints must stay on the canvas and not overlap each other, points or spawns.

| piece | footprint | height | door |
| --- | --- | --- | --- |
{library}
"""


class DraftError(Exception):
    def __init__(self, problems):
        self.problems = list(problems)
        super().__init__('; '.join(self.problems))


# ---------- PNG ----------

def png_bytes(rgb):
    """Deterministic RGB PNG from a uint8 [h, w, 3] array."""
    h, w, _ = rgb.shape
    raw = b''.join(b'\x00' + rgb[y].tobytes() for y in range(h))

    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def png_size(data):
    return struct.unpack('>II', data[16:24])


# ---------- rendering ----------

DIGITS = ['111101101101111', '010110010010111', '111001111100111', '111001111001111', '101101111001001',
          '111100111001111', '111100111101111', '111001001001001', '111101111101111', '111101111001111']
WHITE, YELLOW, MAGENTA, DARK = (255, 255, 255), (255, 214, 64), (255, 64, 200), (20, 20, 20)
TEAM = {'dawn': (255, 179, 71), 'dusk': (184, 148, 255)}
KIND_LINE = {'hill': (240, 220, 140), 'zone': (154, 208, 232), 'river': (124, 192, 255), 'road': (240, 208, 144)}


def _base_colors(f):
    h = f.height.astype(np.float64)
    t = np.clip(h / 50.0, 0, 1)
    lo = np.clip(-h / 20.0, 0, 1)
    up = np.stack([86 + 64 * t, 122 + 18 * t, 70 + 48 * t], -1)
    down = np.stack([86 - 26 * lo, 122 - 30 * lo, 70 - 12 * lo], -1)
    rgb = np.where((h < 0)[..., None], down, up)
    shade = np.zeros_like(h)
    shade[:, 1:] = h[:, 1:] - h[:, :-1]
    shade[1:, :] += h[1:, :] - h[:-1, :]
    rgb = rgb * np.clip(1 + 0.08 * shade, 0.6, 1.4)[..., None]
    rgb[f.road] = (196, 170, 120)
    wet = f.water != NO_WATER
    rgb[wet] = (52, 104, 170)
    return np.clip(np.rint(rgb), 0, 255).astype(np.uint8)


class _Canvas:
    def __init__(self, base, x0, z0, scale):
        self.img = np.repeat(np.repeat(base, scale, 0), scale, 1).copy()
        self.x0, self.z0, self.s = x0, z0, scale

    def px(self, x, z):
        return int(math.floor((x - self.x0) * self.s)), int(math.floor((z - self.z0) * self.s))

    def dot(self, px, pz, c, r=0):
        h, w, _ = self.img.shape
        self.img[max(0, pz - r):max(0, min(h, pz + r + 1)), max(0, px - r):max(0, min(w, px + r + 1))] = c

    def line(self, a, b, c, dash=False):
        (ax, az), (bx, bz) = self.px(*a), self.px(*b)
        n = max(abs(bx - ax), abs(bz - az), 1)
        for i in range(n + 1):
            if dash and (i // 6) % 2:
                continue
            self.dot(round(ax + (bx - ax) * i / n), round(az + (bz - az) * i / n), c)

    def ring(self, x, z, r, c, dash=False):
        n = max(24, int(2 * math.pi * r * self.s))
        pts = [(x + r * math.cos(2 * math.pi * i / n), z + r * math.sin(2 * math.pi * i / n)) for i in range(n + 1)]
        for i, (a, b) in enumerate(zip(pts, pts[1:])):
            if not (dash and (i * 6 // max(1, n // 8)) % 2):
                self.line(a, b, c)

    def number(self, n, px, pz, c, k=2):
        s = str(n)
        x = px - (len(s) * 4 * k) // 2
        for ch in s:
            g = DIGITS[int(ch)]
            for j in range(15):
                if g[j] == '1':
                    self.dot(x + (j % 3) * k, pz - 5 * k // 2 + (j // 3) * k, c, k // 2)
            x += 4 * k


def note_numbers(d):
    return {o['id']: i + 1 for i, o in enumerate(o for o in d['objects'] if o['kind'] == 'note')}


def render(d, f, scale, window=None, base=None):
    """RGB uint8 image of the canvas window (x0, z0, x1, z1) at scale px a block, with every object drawn.
    Proposals are drawn in magenta and dashed; notes in yellow with their marker number."""
    x0, z0, x1, z1 = window or (0, 0, d['width'], d['depth'])
    base = _base_colors(f) if base is None else base
    cv = _Canvas(base[z0:z1, x0:x1], x0, z0, scale)
    numbers = note_numbers(d)
    img = cv.img  # a faint line every GRID blocks, so positions can be read off the picture
    for gx in range((x0 + GRID - 1) // GRID * GRID, x1, GRID):
        img[:, (gx - x0) * scale] = img[:, (gx - x0) * scale].astype(np.uint16) * 3 // 4
    for gz in range((z0 + GRID - 1) // GRID * GRID, z1, GRID):
        img[(gz - z0) * scale, :] = img[(gz - z0) * scale, :].astype(np.uint16) * 3 // 4
    order = ('zone', 'hill', 'river', 'road', 'building', 'point', 'spawn', 'note')
    for kind in order:
        for o in d['objects']:
            if o['kind'] != kind:
                continue
            prop = o.get('state') == 'proposed'
            col = MAGENTA if prop else KIND_LINE.get(kind, WHITE)
            pts = o.get('outline') or o.get('path')
            if kind == 'building':
                from mapengine import buildings
                bx0, bz0, bx1, bz1 = buildings.box(o)
                corners = [[bx0, bz0], [bx1 + 1, bz0], [bx1 + 1, bz1 + 1], [bx0, bz1 + 1]]
                for a, b in zip(corners, corners[1:] + corners[:1]):
                    cv.line(a, b, MAGENTA if prop else WHITE, dash=prop)
                for x, z in buildings.door(o)[1]:
                    cv.ring(x + 0.5, z + 0.5, 0.5, MAGENTA if prop else YELLOW)
            elif pts:
                seq = list(pts) + ([pts[0]] if 'outline' in o else [])
                for a, b in zip(seq, seq[1:]):
                    cv.line(a, b, col, dash=prop)
            elif kind == 'point':
                cv.ring(o['x'], o['z'], o['radius'], MAGENTA if prop else WHITE, dash=prop)
                cv.dot(*cv.px(o['x'], o['z']), MAGENTA if prop else WHITE, scale)
            elif kind == 'spawn':
                px, pz = cv.px(o['x'], o['z'])
                cv.dot(px, pz, DARK, 4 * scale + 1)
                cv.dot(px, pz, MAGENTA if prop else TEAM.get(o['team'], WHITE), 4 * scale)
            elif kind == 'note':
                cv.ring(o['x'], o['z'], o['radius'], YELLOW)
                px, pz = cv.px(o['x'], o['z'])
                cv.dot(px, pz, DARK, 9 + 6 * len(str(numbers[o['id']])))
                cv.number(numbers[o['id']], px, pz, YELLOW, 4)
    return cv.img


# ---------- export ----------

def brief_text(d):
    palettes = ', '.join(sorted(design.palette_keys()))
    lines = [f"# AI handoff: {d['name']} ({d['id']})", '',
             f"Canvas {d['width']} by {d['depth']} blocks, ground level y {d['baseY']}. "
             'In the game, design (x, z) is world (x - width/2, z - depth/2).', '',
             '## Brief', '', (d.get('brief') or '').strip() or '(no brief yet)', '',
             '## Notes on areas', '',
             f'Numbered yellow rings in `top.png`; `notes/<id>.png` is a close-up at {NOTE_SCALE} px a block.', '']
    numbers = note_numbers(d)
    notes = [o for o in d['objects'] if o['kind'] == 'note']
    if not notes:
        lines.append('(no notes)')
    for o in notes:
        lines.append(f"{numbers[o['id']]}. `{o['id']}` at ({o['x']}, {o['z']}), radius {o['radius']}: {o['text'].strip()}")
    lines += ['', '## Pictures', '',
              f'`top.png` is the whole canvas at {SCALE} px a block, north up, x to the right, with a faint grid line '
              f'every {GRID} blocks. Green to grey is '
              'ground from low to high, blue is water, tan is road. White rings are capture points, orange and '
              'violet squares the dawn and dusk spawns. Thin lines are the outlines of hills (yellow), zones '
              '(pale blue), rivers and roads. Magenta dashed shapes are AI proposals not yet reviewed.', '',
              '## Objects', '']
    for o in d['objects']:
        if o['kind'] == 'note':
            continue
        tag = 'locked' if design.locked(o) else 'proposal'
        where = (f"({o['x']}, {o['z']})" if 'x' in o else
                 f"{len(o.get('outline') or o.get('path'))} vertices")
        extra = {'hill': lambda: f"height {o['height']}, steepness {o['steepness']}",
                 'river': lambda: f"width {o['width']}, depth {o['depth']}",
                 'road': lambda: f"width {o['width']}",
                 'point': lambda: f"{o['name']}, radius {o['radius']}",
                 'spawn': lambda: o['team'],
                 'zone': lambda: o['palette'],
                 'building': lambda: f"{o['piece']}, rotation {o['rotation']}"}[o['kind']]()
        lines.append(f"- `{o['id']}` {o['kind']} {where}, {extra} ({tag})")
    from mapengine import library
    lib = '\n'.join(f"| `{k}` | {e['footprint'][0]} x {e['footprint'][1]} | {e['size'][1]} | {e['door']} |"
                    for k, e in sorted(library.load().items()))
    lines += ['', DRAFT_RULES, SCHEMA.format(palettes=palettes, library=lib)]
    return '\n'.join(lines)


def export(d, out_dir):
    """Write design.json, BRIEF.md, top.png and notes/<id>.png into out_dir. Same design, same bytes."""
    out = pathlib.Path(out_dir)
    (out / 'notes').mkdir(parents=True, exist_ok=True)
    for old in (out / 'notes').glob('*.png'):
        old.unlink()
    design.save(out / 'design.json', d)
    (out / 'BRIEF.md').write_bytes(brief_text(d).encode('utf-8'))
    f = heightfield(d)
    base = _base_colors(f)
    (out / 'top.png').write_bytes(png_bytes(render(d, f, SCALE, base=base)))
    for o in d['objects']:
        if o['kind'] != 'note':
            continue
        r = o['radius'] + NOTE_PAD
        win = (max(0, int(o['x'] - r)), max(0, int(o['z'] - r)),
               min(d['width'], int(math.ceil(o['x'] + r))), min(d['depth'], int(math.ceil(o['z'] + r))))
        (out / 'notes' / f"{o['id']}.png").write_bytes(png_bytes(render(d, f, NOTE_SCALE, win, base)))
    return out


# ---------- import ----------

def _same(a, b):
    return json.dumps(a, sort_keys=True) == json.dumps(b, sort_keys=True)


def merge(current, draft):
    """(merged design, report) for an AI draft against the current design; see the module doc.
    report: {added, updated, withdrawn, refused}. Raises DraftError when the draft is not a valid design."""
    if not isinstance(draft, dict):
        raise DraftError(['draft: not a design object'])
    if isinstance(draft.get('objects'), list):  # DRAFT_RULES: by/state may be omitted, the import sets them
        draft = dict(draft, objects=[dict({'by': 'ai', 'state': 'proposed'}, **o) if isinstance(o, dict) else o
                                     for o in draft['objects']])
    problems = design.validate(draft)
    if problems:
        raise DraftError(['the draft fails the schema, nothing was imported'] + problems)
    if draft.get('id') != current['id']:
        raise DraftError([f"the draft is for design {draft.get('id')!r}, not {current['id']!r}"])
    merged = copy.deepcopy(current)
    refused, added, updated, withdrawn = [], [], [], []
    for k in TOP_KEYS:
        if k in draft and not _same(draft.get(k), current.get(k)):
            refused.append(f"{k}: Kort's, the draft's change was not applied")
    cur = {o['id']: o for o in current['objects']}
    drafted = {o['id']: o for o in draft['objects']}
    out = []
    for o in current['objects']:
        d = drafted.get(o['id'])
        if design.locked(o):
            out.append(copy.deepcopy(o))
            if d is None:
                refused.append(f"{o['id']}: locked (Kort's), the draft may not delete it")
            elif not _same(d, o):
                refused.append(f"{o['id']}: locked (Kort's), the draft's change was not applied")
        elif d is None:
            withdrawn.append(o['id'])
        else:
            n = dict(copy.deepcopy(d), by='ai', state='proposed')
            if not _same(n, o):
                updated.append(o['id'])
            out.append(n)
    for o in draft['objects']:
        if o['id'] not in cur:
            out.append(dict(copy.deepcopy(o), by='ai', state='proposed'))
            added.append(o['id'])
    merged['objects'] = out
    # Drop proposals that make the merged design invalid (a point name clashing with Kort's, say).
    for _ in range(len(out) + 1):
        problems = design.validate(dict(merged, review=[]))
        bad = {p.split(':', 1)[0] for p in problems} & {o['id'] for o in merged['objects'] if not design.locked(o)}
        # validate blames the later object, so a proposal taking an earlier Kort point name is blamed on Kort's.
        kort = {o['id']: o.get('name') for o in merged['objects'] if design.locked(o) and o.get('kind') == 'point'}
        for p in problems:
            oid = p.split(':', 1)[0]
            if oid in kort and 'duplicate point name' in p:
                for o in merged['objects']:
                    if o.get('kind') == 'point' and o.get('name') == kort[oid] and not design.locked(o):
                        bad.add(o['id'])
                        refused.append(f"{o['id']}: point name {kort[oid]!r} is Kort's (proposal dropped)")
        if not bad:
            break
        for p in problems:
            if p.split(':', 1)[0] in bad:
                refused.append(p + ' (proposal dropped)')
        merged['objects'] = [o for o in merged['objects'] if o['id'] not in bad]
        added = [i for i in added if i not in bad]
        updated = [i for i in updated if i not in bad]
    problems = design.validate(dict(merged, review=[]))
    if problems:
        raise DraftError(['the merged design would be invalid, nothing was imported'] + problems)
    merged['review'] = refused
    return merged, {'added': added, 'updated': updated, 'withdrawn': withdrawn, 'refused': refused}


def load_draft(path):
    try:
        return json.loads(pathlib.Path(path).read_text(encoding='utf-8'))
    except (OSError, ValueError, UnicodeDecodeError) as e:
        raise DraftError([f'draft not read: {e}'])
