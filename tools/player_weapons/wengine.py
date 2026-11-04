"""Player weapons: cubes at 2 texels a model px on a 64x128 sheet, frame groups, rarity textures, Blockbench export.

The weapon standard is docs/ART-STYLE-GUIDE.md section 7: Blockbench space, y up, the grip's centre at the origin (the
4 px fist hides y -2..+2), the big faces across z, Blockbench's south the front. The light comes from the top left as
the viewer faces the front, so south is the front, west the lit side, north the back and east the dark side. Every
weapon has a `rest` group; a draw weapon also has `pull_0` to `pull_2`. Each frame holds at most 16 cubes (18 on a
Legendary, which is the top tier), strings not counted.

Reuses the armour engine's boxes, canvas, packer-free painting and renderer (tools/player_armor/engine.py).
"""
import base64
import io
import json
import math
import uuid as uuidlib

import numpy as np
from PIL import Image

from player_armor.engine import Box, Canvas, FACES, SHADE

WSEM = {'south': 'front', 'north': 'back', 'west': 'lit', 'east': 'dark', 'up': 'top', 'down': 'bottom'}
SHEET = (64, 128)
CAP = {'base': 16, 'legendary': 18}
# the weapon material map's key colours (tools/build_skins.py MATERIALS)
KEYS = {'steel': (255, 0, 0), 'iron': (170, 0, 0), 'gilt': (255, 170, 0), 'wood': (0x8B, 0x45, 0x13),
        'linen': (255, 255, 255), 'leather': (0x55, 0x33, 0x22), 'bone': (0xEE, 0xEE, 0xDD), 'glass': (0, 255, 0)}


class WBox(Box):
    DENSITY = 2

    def __init__(self, name, frm, size, paint, rot=(0, 0, 0), origin=None, tier='all', frames=None, hide=(), note=''):
        super().__init__(name, 'weapon', 'weapon', frm, size, paint, 0.0, rot, origin, tier, None, hide, note)
        self.sem_map = WSEM
        self.frames = frames          # None: every frame


def seg(name, p0, p1, w, d, paint, z=0.0, ext=0.25, **kw):
    """A cube from point p0 toward p1 in the x-y plane, w wide (x before turning) and d deep (z), turned about z on p0 so
    its length runs along p0 -> p1. It starts `ext` px behind p0 so a joint shows no notch (style guide 7.1, joints).
    The length is rounded to whole texels."""
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    length = math.hypot(dx, dy) + ext
    length = max(0.5, round(length * 2) / 2)
    ang = math.degrees(math.atan2(-dx, dy))
    ang = round(ang, 2)
    frm = (p0[0] - w / 2, p0[1] - ext, z - d / 2)
    return WBox(name, frm, (w, length, d), paint, rot=(0, 0, ang), origin=(p0[0], p0[1], z), **kw)


class Weapon:
    def __init__(self, wid, title, set_title, house, kind, palette_fn, build_fn, frames=('rest',), notes='',
                 keys=None, sidecar=None):
        self.id, self.title, self.set_title, self.house, self.kind = wid, title, set_title, house, kind
        self.palette_fn, self.build_fn, self.frames, self.notes = palette_fn, build_fn, frames, notes
        self.keys = keys or {}        # material -> KEYS name
        self.sidecar = sidecar or {}
        self._boxes, self._painted, self.layout = {}, {}, None

    def boxes(self, tier, frame='rest'):
        k = (tier, frame)
        if k not in self._boxes:
            bs = [b for b in self.build_fn(tier, frame) if b.in_tier(tier) and (b.frames is None or frame in b.frames)]
            names = [b.name for b in bs]
            if len(set(names)) != len(names):
                raise ValueError('%s %s/%s: duplicate cube names' % (self.id, tier, frame))
            self._boxes[k] = bs
        return self._boxes[k]

    def all_boxes(self, tier):
        seen = {}
        for f in self.frames:
            for b in self.boxes(tier, f):
                prev = seen.get(b.name)
                if prev is not None and [prev.face_size(x) for x in FACES] != [b.face_size(x) for x in FACES]:
                    raise ValueError('%s: cube %s changes size between frames' % (self.id, b.name))
                seen.setdefault(b.name, b)
        return list(seen.values())

    def paint(self, tier):
        if tier in self._painted:
            return self._painted[tier]
        out = {}
        for b in self.all_boxes(tier):
            for f in FACES:
                if f in b.hide:
                    continue
                w, h = b.face_size(f)
                c = Canvas(w, h, f, b, tier)
                for layer in b.paint:
                    layer(c)
                out[(b.name, f)] = c
        self._painted[tier] = out
        return out

    def pack(self):
        if self.layout:
            return self.layout
        rects = {}
        for tier in ('base', 'legendary'):
            for b in self.all_boxes(tier):
                for f in FACES:
                    if f not in b.hide:
                        rects[(b.name, f)] = b.face_size(f)
        order = sorted(rects, key=lambda k: (-rects[k][1], -rects[k][0], k))
        W, H = SHEET
        x = y = shelf = 0
        pos = {}
        for k in order:
            w, h = rects[k]
            if x + w > W:
                x, y, shelf = 0, y + shelf, 0
            pos[k] = (x, y, w, h)
            x += w
            shelf = max(shelf, h)
        if y + shelf > H:
            raise ValueError('%s: the faces need %d texel rows; the weapon sheet has %d' % (self.id, y + shelf, H))
        self.layout = (W, H, pos)
        return self.layout

    def texture(self, rarity):
        tier = 'legendary' if rarity == 'legendary' else 'base'
        pal = self.palette_fn(rarity)
        W, H, pos = self.pack()
        img = np.zeros((H, W, 4), np.uint8)
        glow = np.zeros((H, W, 4), np.uint8)
        keys = np.zeros((H, W, 4), np.uint8)
        for key, c in self.paint(tier).items():
            x0, y0, w, h = pos[key]
            for yy in range(h):
                for xx in range(w):
                    m = c.mat[yy, xx]
                    if m is None:
                        continue
                    col = pal.tones[m][int(c.tone[yy, xx])]
                    img[y0 + yy, x0 + xx] = (*[int(round(v)) for v in col], 255)
                    if m in pal.glow:
                        glow[y0 + yy, x0 + xx] = img[y0 + yy, x0 + xx]
                    kname = self.keys.get(m)
                    if kname is None:
                        raise KeyError('%s: material %s has no material-map key' % (self.id, m))
                    keys[y0 + yy, x0 + xx] = (*KEYS[kname], 255)
        return Image.fromarray(img, 'RGBA'), Image.fromarray(glow, 'RGBA'), Image.fromarray(keys, 'RGBA')

    def stats(self, tier):
        n = {}
        for c in self.paint(tier).values():
            for m in c.mat.flat:
                if m is not None:
                    n[m] = n.get(m, 0) + 1
        return n


def _n(v):
    v = round(float(v), 4)
    return int(v) if v == int(v) else v


def bbmodel(wp, tier, tex):
    """A Blockbench project (format 5, free model) in the weapon convention: one group per frame, `rest` visible, the
    pull frames saved hidden, cubes named <frame>/<cube>, every face's UV exactly twice its size on the 64x128 sheet."""
    W, H, pos = wp.pack()
    elements, groups, outliner = [], [], []
    for frame in wp.frames:
        kids = []
        for b in wp.boxes(tier, frame):
            uid = str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, 'gg-weapon/%s/%s/%s/%s' % (wp.id, tier, frame, b.name)))
            faces = {}
            for f in FACES:
                if f in b.hide:
                    continue
                x, y, w, h = pos[(b.name, f)]
                faces[f] = {'uv': [x, y, x + w, y + h], 'texture': 0}
            e = {'name': '%s/%s' % (frame, b.name), 'box_uv': False, 'render_order': 'default', 'locked': False,
                 'export': True, 'scope': 0, 'allow_mirror_modeling': True,
                 'from': [_n(c) for c in b.frm], 'to': [_n(b.frm[i] + b.size[i]) for i in range(3)],
                 'autouv': 0, 'color': 4, 'origin': [_n(c) for c in b.origin], 'faces': faces, 'type': 'cube',
                 'uuid': uid}
            if any(b.rot):
                e['rotation'] = [_n(r) for r in b.rot]
            elements.append(e)
            kids.append(uid)
        gid = str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, 'gg-weapon/%s/%s/%s' % (wp.id, tier, frame)))
        groups.append({'name': frame, 'origin': [0, 0, 0], 'color': 0, 'uuid': gid, 'export': True, 'mirror_uv': False,
                       'isOpen': False, 'locked': False, 'visibility': frame == 'rest', 'autouv': 0})
        outliner.append({'uuid': gid, 'isOpen': False, 'children': kids})
    buf = io.BytesIO()
    tex.save(buf, 'PNG')
    name = wp.id + ('_legendary' if tier == 'legendary' else '')
    return {'meta': {'format_version': '5.0', 'model_format': 'free', 'box_uv': False}, 'name': name,
            'model_identifier': wp.id, 'visible_box': [1, 1, 0], 'variable_placeholders': '',
            'variable_placeholder_buttons': [], 'resolution': {'width': 16, 'height': 16},
            'elements': elements, 'groups': groups, 'outliner': outliner,
            'textures': [{'path': '', 'name': name, 'folder': '', 'namespace': '', 'id': '0', 'particle': False,
                          'render_mode': 'default', 'visible': True, 'mode': 'bitmap', 'saved': True,
                          'uuid': str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, 'gg-weapon-tex/%s/%s' % (wp.id, tier))),
                          'relative_path': 'textures/%s_%s.png' % (wp.id, 'legendary' if tier == 'legendary' else 'common'),
                          'width': W, 'height': H, 'uv_width': W, 'uv_height': H,
                          'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()}]}


def drawables(wp, rarity, frame='rest'):
    tier = 'legendary' if rarity == 'legendary' else 'base'
    img, glow, _ = wp.texture(rarity)
    tex = np.array(img)
    gl = np.array(glow)[..., 3] > 0
    W, H, pos = wp.pack()
    out = []
    for b in wp.boxes(tier, frame):
        out.append((b, {f: (tex, gl, pos[(b.name, f)], SHADE[f]) for f in FACES if f not in b.hide}))
    return out


def chain(prefix, start, parts, paint, z=0.0, ext=0.25, **kw):
    """A run of cubes from `start`, each (length, angle, width, depth): the length (with the `ext` overlap, a multiple
    of 0.5) is fixed, so a frame can bend the run (change the angles) without changing any cube's size. An angle is
    degrees from +y, positive toward -x. Neighbours alternate a 0.05 px depth offset so none share a plane.
    Returns (cubes, the points along the run)."""
    pts = [start]
    out = []
    p = start
    for i, (length, ang, w, d) in enumerate(parts):
        rad = math.radians(ang)
        frm = (p[0] - w / 2, p[1] - ext, z + (0.05 if i % 2 else 0.0) - d / 2)
        out.append(WBox('%s_%d' % (prefix, i), frm, (w, length, d), paint if not callable(paint) else paint(i),
                        rot=(0, 0, round(ang, 2)), origin=(p[0], p[1], z), **kw))
        run = length - ext
        p = (p[0] - math.sin(rad) * run, p[1] + math.cos(rad) * run)
        pts.append(p)
    return out, pts


def mirror_y(boxes, suffix='_lo'):
    """The same cubes reflected in y = 0 (a bow's lower limb from its upper one): y negates and so does each turn."""
    out = []
    for b in boxes:
        lo, size = b.frm, b.size
        frm = (lo[0], -(lo[1] + size[1]), lo[2])
        out.append(WBox(b.name + suffix, frm, size, b.paint, rot=(b.rot[0], b.rot[1], -b.rot[2]),
                        origin=(b.origin[0], -b.origin[1], b.origin[2]), tier=b.tier, frames=b.frames, hide=b.hide,
                        note=b.note))
    return out
