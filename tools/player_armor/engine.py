"""Player armour sets: boxes, face painting, atlas, Blockbench export and a turntable renderer.

Space is Blockbench's: y up, feet at y 0, the player's front faces north (-z), the player's right arm is at +x (so the
viewer, facing the player, sees +x on the left). Every box is authored at 1 texel per model px (integer sizes); `inflate`
only separates layers (it grows the cube by a fraction of a px without changing its texels, as vanilla armour does).

Faces are painted as the viewer sees them from outside: u to the viewer's right, v down (Minecraft block-face UV
conventions, which Blockbench uses), so a painter never has to think about mirroring. The lit side follows the style
guide (light from the top left as the viewer faces the player): the front (north) and the viewer's-left side (east, +x)
are lit, the back (south) is base, the far side (west, -x) is dark.
"""
import base64
import colorsys
import io
import json
import math
import random
import uuid as uuidlib

import numpy as np
from PIL import Image

FACES = ('north', 'east', 'south', 'west', 'up', 'down')
SEM = {'north': 'front', 'south': 'back', 'east': 'lit', 'west': 'dark', 'up': 'top', 'down': 'bottom'}
# style guide section 3 (top 4 is softened to 3 + an edge highlight, so no face is a whole highlight)
FACE_TONE = {'front': 3, 'lit': 3, 'back': 2, 'dark': 1, 'top': 3, 'bottom': 0}
SHADE = {'north': 0.86, 'south': 0.86, 'east': 0.70, 'west': 0.70, 'up': 1.0, 'down': 0.52}

# ---------------------------------------------------------------- colour

def hex_rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def rgb_hex(c):
    return '#%02X%02X%02X' % tuple(int(round(v)) for v in c)


def _toward(h, target, deg):
    d = (target - h + 540) % 360 - 180
    if abs(d) <= deg:
        return target % 360
    return (h + math.copysign(deg, d)) % 360


def ramp(base, step=0.22, clamp=True, hue_shift=6.0):
    """The style guide's 5-tone ramp (section 1): tone 2 is the base; value +-step a tone, shadows turn toward 268 deg,
    lights toward 52 deg, darks gain 0.04 saturation a step and lights lose 0.07; saturation clamps to 0.44 above value
    0.60 (the team tint rule) unless clamp is False (team cloth, rarity accents, glow)."""
    r, g, b = (c / 255 for c in hex_rgb(base))
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    h *= 360
    out = []
    for k in (-2, -1, 0, 1, 2):
        vv = min(1.0, max(0.06, v * (1 + step * k)))
        if k < 0:
            hh = _toward(h, 268, hue_shift * -k) if s > 0.02 or True else h
            ss = min(1.0, s + 0.04 * -k)
        elif k > 0:
            hh = _toward(h, 52, hue_shift * k)
            ss = max(0.0, s - 0.07 * k)
        else:
            hh, ss = h, s
        if s < 0.03:   # near-greys keep a faint temperature but do not swing hue wildly
            ss = min(ss, 0.06)
        if clamp and vv > 0.60 and ss > 0.44:
            ss = 0.44
        rr, gg, bb = colorsys.hsv_to_rgb(hh / 360, ss, vv)
        out.append((rr * 255, gg * 255, bb * 255))
    return out


def shift(base, dh=0.0, ds=0.0, dv=1.0, hue=None):
    """A base colour moved in HSV: hue to `hue` (or by dh), saturation by ds, value times dv."""
    r, g, b = (c / 255 for c in hex_rgb(base))
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    h = (hue / 360) if hue is not None else (h + dh / 360) % 1
    s = min(1, max(0, s + ds))
    v = min(1, max(0, v * dv))
    return rgb_hex(tuple(c * 255 for c in colorsys.hsv_to_rgb(h, s, v)))


# ---------------------------------------------------------------- canvas

class Canvas:
    """One face's texels: a material name (None is transparent) and a tone 0..4 per texel."""

    def __init__(self, w, h, face, box, tier):
        self.w, self.h, self.face, self.box, self.tier = w, h, face, box, tier
        self.sem = getattr(box, 'sem_map', SEM)[face]
        self.mat = np.full((h, w), None, dtype=object)
        self.tone = np.zeros((h, w), dtype=np.int8)
        self.rng = random.Random('%s/%s/%s' % (box.name, face, tier))

    @property
    def legendary(self):
        return self.tier == 'legendary'

    def fill(self, mat, tone):
        self.mat[:, :] = mat
        self.tone[:, :] = tone

    def px(self, x, y, mat=None, tone=None, d=None):
        """Sets one texel (silently ignores texels off the face). d is a tone change relative to what is there."""
        if not (0 <= x < self.w and 0 <= y < self.h):
            return
        if mat is not None:
            self.mat[y, x] = mat
        if d is not None:
            self.tone[y, x] = max(0, min(4, self.tone[y, x] + d))
        elif tone is not None:
            self.tone[y, x] = max(0, min(4, tone))

    def rect(self, x0, y0, w, h, mat=None, tone=None, d=None):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                self.px(x, y, mat, tone, d)

    def hline(self, y, x0=0, x1=None, mat=None, tone=None, d=None):
        x1 = self.w - 1 if x1 is None else x1
        for x in range(x0, x1 + 1):
            self.px(x, y, mat, tone, d)

    def vline(self, x, y0=0, y1=None, mat=None, tone=None, d=None):
        y1 = self.h - 1 if y1 is None else y1
        for y in range(y0, y1 + 1):
            self.px(x, y, mat, tone, d)

    def clear(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.mat[y, x] = None

    def ux(self, x):
        """x counted from the player's own right side of a front face (mirror-safe placement for symmetric motifs)."""
        return x

    @property
    def base(self):
        return FACE_TONE[self.sem]


# ---------------------------------------------------------------- boxes

class Box:
    DENSITY = 1   # texels a model px; a subclass for weapons paints at 2

    def __init__(self, name, slot, bone, frm, size, paint, inflate=0.0, rot=(0, 0, 0), origin=None, tier='all',
                 side=None, hide=(), note=''):
        self.name, self.slot, self.bone = name, slot, bone
        self.frm = [float(c) for c in frm]
        self.size = [float(c) for c in size]
        for s in self.size:
            if abs(s * self.DENSITY - round(s * self.DENSITY)) > 1e-6 or s <= 0:
                raise ValueError('%s: box sizes are whole texels at %d a px (%s)' % (name, self.DENSITY, size))
        self.paint = paint if isinstance(paint, (list, tuple)) else [paint]
        self.inflate = inflate
        self.rot = tuple(float(r) for r in rot)
        self.origin = list(origin) if origin is not None else [self.frm[i] + self.size[i] / 2 for i in range(3)]
        self.tier = tier          # 'all', 'base' (Common to Epic only) or 'legendary'
        self.side = side          # 'R', 'L' or None
        self.hide = set(hide)     # faces never seen (inside another box); left unpainted and undrawn
        self.note = note

    def in_tier(self, tier):
        return self.tier == 'all' or self.tier == tier or (self.tier == 'base' and tier == 'base')

    def face_size(self, f):
        sx, sy, sz = (int(round(s * self.DENSITY)) for s in self.size)
        return {'north': (sx, sy), 'south': (sx, sy), 'east': (sz, sy), 'west': (sz, sy),
                'up': (sx, sz), 'down': (sx, sz)}[f]

    def mirrored(self, extra_inflate=0.0):
        nm = self.name[:-2] + '_l' if self.name.endswith('_r') else self.name.replace('_r_', '_l_') if '_r_' in self.name else self.name + '_l'
        b = Box(nm,
                self.slot, {'right_arm': 'left_arm', 'left_arm': 'right_arm', 'right_leg': 'left_leg',
                            'left_leg': 'right_leg'}.get(self.bone, self.bone),
                [-(self.frm[0] + self.size[0]), self.frm[1], self.frm[2]], self.size, self.paint,
                self.inflate + extra_inflate, (self.rot[0], -self.rot[1], -self.rot[2]),
                [-self.origin[0], self.origin[1], self.origin[2]], self.tier,
                {'R': 'L', 'L': 'R'}.get(self.side, self.side), self.hide, self.note)
        b.hide = {('west' if f == 'east' else 'east' if f == 'west' else f) for f in self.hide}
        return b

    # geometry
    def lo_hi(self):
        g = self.inflate
        lo = [self.frm[i] - g for i in range(3)]
        hi = [self.frm[i] + self.size[i] + g for i in range(3)]
        return lo, hi

    def matrix(self):
        rx, ry, rz = (math.radians(a) for a in self.rot)
        cx, sx_, cy, sy_, cz, sz_ = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
        mx = np.array([[1, 0, 0], [0, cx, -sx_], [0, sx_, cx]])
        my = np.array([[cy, 0, sy_], [0, 1, 0], [-sy_, 0, cy]])
        mz = np.array([[cz, -sz_, 0], [sz_, cz, 0], [0, 0, 1]])
        return mz @ my @ mx   # Blockbench applies x, then y, then z

    def face_frame(self, f):
        """(origin corner, U, V): the face's texel (0,0) corner and the edges along u and v, in model px, rotated."""
        lo, hi = self.lo_hi()
        L = lambda x, y, z: np.array([x, y, z], dtype=float)
        if f == 'north':
            o, u, v = L(hi[0], hi[1], lo[2]), L(lo[0] - hi[0], 0, 0), L(0, lo[1] - hi[1], 0)
        elif f == 'south':
            o, u, v = L(lo[0], hi[1], hi[2]), L(hi[0] - lo[0], 0, 0), L(0, lo[1] - hi[1], 0)
        elif f == 'east':
            o, u, v = L(hi[0], hi[1], hi[2]), L(0, 0, lo[2] - hi[2]), L(0, lo[1] - hi[1], 0)
        elif f == 'west':
            o, u, v = L(lo[0], hi[1], lo[2]), L(0, 0, hi[2] - lo[2]), L(0, lo[1] - hi[1], 0)
        elif f == 'up':
            o, u, v = L(lo[0], hi[1], lo[2]), L(hi[0] - lo[0], 0, 0), L(0, 0, hi[2] - lo[2])
        else:
            o, u, v = L(lo[0], lo[1], hi[2]), L(hi[0] - lo[0], 0, 0), L(0, 0, lo[2] - hi[2])
        if any(self.rot):
            m = self.matrix()
            p = np.array(self.origin, dtype=float)
            o = m @ (o - p) + p
            u, v = m @ u, m @ v
        return o, u, v


# ---------------------------------------------------------------- materials and palettes

class Palette:
    """Material name -> 5 RGB tones, plus the names that glow (drawn unshaded, and into the _e mask)."""

    def __init__(self):
        self.tones, self.glow = {}, set()

    def add(self, name, base, step=0.22, clamp=True, glow=False, tones=None):
        self.tones[name] = tones if tones is not None else ramp(base, step, clamp)
        if glow:
            self.glow.add(name)
        return self


# ---------------------------------------------------------------- the set

class ArmourSet:
    def __init__(self, sid, title, house, palette_fn, boxes, notes):
        self.id, self.title, self.house = sid, title, house
        self.palette_fn = palette_fn     # (rarity, team) -> Palette
        self.boxes = boxes
        self.notes = notes
        self._painted = {}
        self.layout = None

    def boxes_for(self, tier, slots=None):
        return [b for b in self.boxes if b.in_tier(tier) and (slots is None or b.slot in slots)]

    def paint(self, tier):
        """Paints every face of every box of a tier: {(box name, face): Canvas}."""
        if tier in self._painted:
            return self._painted[tier]
        out = {}
        for b in self.boxes_for(tier):
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

    def pack(self, width=128):
        """Shelf-packs every face of both tiers into one layout shared by every rarity's texture."""
        if self.layout:
            return self.layout
        rects = {}
        for b in self.boxes:
            for f in FACES:
                if f in b.hide:
                    continue
                rects[(b.name, f)] = b.face_size(f)
        order = sorted(rects, key=lambda k: (-rects[k][1], -rects[k][0], k))
        x = y = shelf = 0
        pos = {}
        for k in order:
            w, h = rects[k]
            if x + w > width:
                x, y, shelf = 0, y + shelf, 0
            pos[k] = (x, y, w, h)
            x += w
            shelf = max(shelf, h)
        height = y + shelf
        hh = 16
        while hh < height:
            hh *= 2
        self.layout = (width, hh, pos)
        return self.layout

    def texture(self, rarity, team):
        """(RGBA image, glow mask image) of a rarity and team colour."""
        tier = 'legendary' if rarity == 'legendary' else 'base'
        pal = self.palette_fn(rarity, team)
        W, H, pos = self.pack()
        img = np.zeros((H, W, 4), dtype=np.uint8)
        glow = np.zeros((H, W, 4), dtype=np.uint8)
        for key, c in self.paint(tier).items():
            x0, y0, w, h = pos[key]
            for yy in range(h):
                for xx in range(w):
                    m = c.mat[yy, xx]
                    if m is None:
                        continue
                    if m not in pal.tones:
                        raise KeyError('%s: no material %s in the %s/%s palette' % (key, m, rarity, team))
                    col = pal.tones[m][int(c.tone[yy, xx])]
                    img[y0 + yy, x0 + xx] = (*[int(round(v)) for v in col], 255)
                    if m in pal.glow:
                        glow[y0 + yy, x0 + xx] = (*[int(round(v)) for v in col], 255)
        return Image.fromarray(img, 'RGBA'), Image.fromarray(glow, 'RGBA')

    def stats(self, tier):
        """Texel counts per material over the painted faces of a tier (for the gilt and glow caps)."""
        n = {}
        for c in self.paint(tier).values():
            for m in c.mat.flat:
                if m is not None:
                    n[m] = n.get(m, 0) + 1
        return n


# ---------------------------------------------------------------- checks

def coplanar(boxes, eps=1e-4):
    """Pairs of faces of unrotated boxes that share a plane, face the same way and overlap in area (they z-fight)."""
    out = []
    flat = [b for b in boxes if not any(b.rot)]
    planes = []
    for b in flat:
        lo, hi = b.lo_hi()
        for f in FACES:
            if f in b.hide:
                continue
            ax = {'north': 2, 'south': 2, 'east': 0, 'west': 0, 'up': 1, 'down': 1}[f]
            val = lo[ax] if f in ('north', 'west', 'down') else hi[ax]
            if f == 'down' and val <= 0.01:
                continue   # soles on the ground are only seen from below
            others = [a for a in range(3) if a != ax]
            planes.append((b, f, ax, val, [(lo[a], hi[a]) for a in others]))
    for i in range(len(planes)):
        for j in range(i + 1, len(planes)):
            b1, f1, a1, v1, r1 = planes[i]
            b2, f2, a2, v2, r2 = planes[j]
            if b1 is b2 or f1 != f2 or abs(v1 - v2) > eps:
                continue
            if b1.bone != b2.bone and {b1.bone, b2.bone} - {'body', 'head'}:
                pass
            ov = all(min(p[1], q[1]) - max(p[0], q[0]) > 0.02 for p, q in zip(r1, r2))
            if ov:
                out.append((b1.name, b2.name, f1))
    return out


# ---------------------------------------------------------------- Blockbench export

BONES = {  # Blockbench pivots of the player rig
    'head': [0, 24, 0], 'body': [0, 24, 0], 'right_arm': [5, 22, 0], 'left_arm': [-5, 22, 0],
    'right_leg': [1.9, 12, 0], 'left_leg': [-1.9, 12, 0],
}
SLOTS = ('helmet', 'chest', 'legs', 'boots')


def _n(v):
    v = round(float(v), 4)
    return int(v) if v == int(v) else v


def bbmodel(aset, tier, tex_png, tex_name):
    W, H, pos = aset.pack()
    elements, groups_by = [], {}
    for b in aset.boxes_for(tier):
        uid = str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, 'gg-armour/%s/%s/%s' % (aset.id, tier, b.name)))
        faces = {}
        for f in FACES:
            if f in b.hide:
                faces[f] = {'uv': [0, 0, 0, 0], 'texture': None}
                continue
            x, y, w, h = pos[(b.name, f)]
            faces[f] = {'uv': [x, y, x + w, y + h], 'texture': 0}
        e = {'name': b.name, 'type': 'cube', 'uuid': uid, 'box_uv': False, 'rescale': False, 'locked': False,
             'from': [_n(c) for c in b.frm], 'to': [_n(b.frm[i] + b.size[i]) for i in range(3)],
             'autouv': 0, 'color': SLOTS.index(b.slot) * 2, 'inflate': _n(b.inflate),
             'origin': [_n(c) for c in b.origin], 'faces': faces}
        if any(b.rot):
            e['rotation'] = [_n(r) for r in b.rot]
        elements.append(e)
        groups_by.setdefault(b.bone, {}).setdefault(b.slot, []).append(uid)
    outliner = []
    for bone in BONES:
        if bone not in groups_by:
            continue
        kids = []
        for slot in SLOTS:
            if slot in groups_by[bone]:
                kids.append({'name': slot, 'origin': BONES[bone], 'color': 0,
                             'uuid': str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, 'gg/%s/%s/%s/%s' % (aset.id, tier, bone, slot))),
                             'export': True, 'isOpen': True, 'locked': False, 'visibility': True, 'autouv': 0,
                             'children': groups_by[bone][slot]})
        outliner.append({'name': bone, 'origin': BONES[bone], 'color': 0,
                         'uuid': str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, 'gg/%s/%s/%s' % (aset.id, tier, bone))),
                         'export': True, 'isOpen': True, 'locked': False, 'visibility': True, 'autouv': 0,
                         'children': kids})
    buf = io.BytesIO()
    tex_png.save(buf, 'PNG')
    return {
        'meta': {'format_version': '4.5', 'model_format': 'free', 'box_uv': False},
        'name': '%s%s' % (aset.id, '_legendary' if tier == 'legendary' else ''),
        'model_identifier': aset.id,
        'visible_box': [1, 1, 0], 'variable_placeholders': '', 'variable_placeholder_buttons': [],
        'resolution': {'width': W, 'height': H},
        'elements': elements, 'outliner': outliner,
        'textures': [{'path': '', 'name': tex_name, 'folder': '', 'namespace': '', 'id': '0', 'particle': False,
                      'render_mode': 'default', 'visible': True, 'mode': 'bitmap', 'saved': True, 'uuid':
                      str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, 'gg/tex/%s/%s' % (aset.id, tier))),
                      'relative_path': 'textures/' + tex_name, 'width': W, 'height': H, 'uv_width': W, 'uv_height': H,
                      'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()}],
    }


# ---------------------------------------------------------------- the turntable renderer

def render(drawables, yaw, pitch, scale, size=None, bg=(0, 0, 0, 0), anchor=None):
    """Orthographic render of [(box, {face: (texture RGBA array, glow bool array, (x,y,w,h))})]. yaw turns the camera
    toward the player's right (+x side) by yaw degrees; pitch looks down. Returns an RGBA image cropped later."""
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    # camera basis in model space: it starts at -z looking +z, right = -x, up = +y
    fwd = np.array([-sy, 0, cy])
    right = np.array([-cy, 0, -sy])
    up = np.array([0, 1, 0])
    fwd2 = fwd * cp + up * -sp
    up2 = up * cp + fwd * sp
    W, H = size
    col = np.zeros((H, W, 4), dtype=np.float32)
    col[:, :] = bg
    depth = np.full((H, W), np.inf)
    cx0, cy0 = anchor if anchor else (W / 2, H * 0.9)
    for item in drawables:
        box, faces = item[0], item[1]
        M = item[2] if len(item) > 2 else None   # an optional 4x4 bone matrix (an animated rig)
        for f, (tex, glow, rect, shade) in faces.items():
            o, u, v = box.face_frame(f)
            centre = o + u / 2 + v / 2
            mid = np.array([(a + b) / 2 for a, b in zip(*box.lo_hi())])
            if any(box.rot):
                m = box.matrix()
                p = np.array(box.origin, dtype=float)
                mid = m @ (mid - p) + p
            if M is not None:
                R3, t3 = M[:3, :3], M[:3, 3]
                o, u, v = R3 @ o + t3, R3 @ u, R3 @ v
                centre, mid = R3 @ centre + t3, R3 @ mid + t3
            n = np.cross(u, v)
            if np.dot(n, centre - mid) < 0:
                n = -n
            if M is not None:
                # light by where the face now points: up 1.0, down 0.52, z faces 0.86, x faces 0.70, blended
                nn = n / (np.linalg.norm(n) + 1e-12)
                shade = 0.70 * nn[0] ** 2 + 0.86 * nn[2] ** 2 + (1.0 if nn[1] > 0 else 0.52) * nn[1] ** 2
            if np.dot(n, fwd2) >= -1e-9:
                continue   # back face
            P = lambda q: np.array([cx0 + np.dot(q, right) * scale, cy0 - np.dot(q, up2) * scale, np.dot(q, fwd2)])
            so, su, sv = P(o), P(o + u) - P(o), P(o + v) - P(o)
            pts = [so[:2], (so + su)[:2], (so + su + sv)[:2], (so + sv)[:2]]
            xs, ys = [p[0] for p in pts], [p[1] for p in pts]
            x0, x1 = max(0, int(math.floor(min(xs)))), min(W - 1, int(math.ceil(max(xs))))
            y0, y1 = max(0, int(math.floor(min(ys)))), min(H - 1, int(math.ceil(max(ys))))
            if x1 < x0 or y1 < y0:
                continue
            A = np.array([[su[0], sv[0]], [su[1], sv[1]]])
            det = np.linalg.det(A)
            if abs(det) < 1e-9:
                continue
            Ai = np.linalg.inv(A)
            gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            dx, dy = gx - so[0], gy - so[1]
            s = Ai[0, 0] * dx + Ai[0, 1] * dy
            t = Ai[1, 0] * dx + Ai[1, 1] * dy
            inside = (s >= 0) & (s < 1) & (t >= 0) & (t < 1)
            if not inside.any():
                continue
            z = so[2] + s * su[2] + t * sv[2]
            rx, ry, rw, rh = rect
            tu = np.clip((s * rw).astype(int), 0, int(math.ceil(rw)) - 1)
            tv = np.clip((t * rh).astype(int), 0, int(math.ceil(rh)) - 1)
            texel = tex[ry + tv, rx + tu]
            gl = glow[ry + tv, rx + tu]
            vis = inside & (texel[..., 3] > 0)
            sub = depth[y0:y1 + 1, x0:x1 + 1]
            vis &= z < sub - 1e-6
            if not vis.any():
                continue
            # light: model-fixed, per face direction (vanilla-like entity shading)
            k = shade
            rgb = texel[..., :3].astype(np.float32) * np.where(gl, 1.0, k)[..., None]
            region = col[y0:y1 + 1, x0:x1 + 1]
            region[vis, :3] = rgb[vis]
            region[vis, 3] = 255
            sub[vis] = z[vis]
    return Image.fromarray(np.clip(col, 0, 255).astype(np.uint8), 'RGBA')


def face_shade(box, f, yaw):
    """Vanilla-like entity shading from the face's direction in the model (up 1.0, down 0.5, z faces 0.86, x 0.7)."""
    return SHADE[f]
