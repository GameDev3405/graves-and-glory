"""v4 portraits: the live model render the War Table frames show, as the unit identity spec sets it (rule 8):
the biped chest-up at yaw 30, weapon arm forward, the camera 5 degrees below eye level, 20 model px from
4 px above the head to 8 px below the shoulders (Tall +N adds N at the top). A small ray caster stands in for
InventoryScreen.drawEntity: boxes in the vanilla biped layout, outer layers (hat, armour, EMF shells)
inflated as the game inflates them and drawn without back-face culling, textures sampled nearest, faces lit
as the inventory lights them (tops brightest, the face toward the camera next, the turned side darker).
The skins follow the spec looks: flat-shaded paintings, three to four tones per material, linen panels
where the team cloth goes (portrait models have no team)."""
import math
from v4lib import Spr, rgba

YAW, PITCH = math.radians(30), math.radians(5)


def rot_y(v, a):
    x, y, z = v
    return (x * math.cos(a) + z * math.sin(a), y, -x * math.sin(a) + z * math.cos(a))


def rot_x(v, a):
    # positive a tips the top of the model away from the camera (the camera looks up at it)
    x, y, z = v
    return (x, y * math.cos(a) + z * math.sin(a), -y * math.sin(a) + z * math.cos(a))


def to_view(v):
    return rot_x(rot_y(v, YAW), PITCH)


def from_view(v):
    return rot_y(rot_x(v, -PITCH), -YAW)


class Tex:
    """one face texture: rows of letters, a palette; a dot is a clear texel"""
    def __init__(self, rows, pal):
        self.rows = rows; self.pal = pal
        self.h = len(rows); self.w = len(rows[0])

    def at(self, u, v):
        i = min(self.w - 1, max(0, int(u))); j = min(self.h - 1, max(0, int(v)))
        ch = self.rows[j][i]
        if ch in '. ':
            return None
        return self.pal[ch]


def solid(c, w, h):
    return Tex(['a' * w] * h, {'a': c})


class Box:
    """an axis-aligned box in model px (y up, the model faces +z), inflated like a model layer; faces maps
    front/back/right/left/top/bottom to a Tex (right is the model own right, the -x side). An optional
    rotation about a pivot poses a limb (rx: positive swings the lower end forward)."""
    def __init__(self, x0, y0, z0, x1, y1, z1, faces, inflate=0.0, pivot=None, rx=0.0, inner=None):
        g = inflate
        self.b0 = (x0 - g, y0 - g, z0 - g); self.b1 = (x1 + g, y1 + g, z1 + g)
        self.faces = faces; self.pivot = pivot; self.rx = math.radians(rx); self.inner = inner

    def local(self, p, d):
        if not self.pivot or not self.rx:
            return p, d
        px, py, pz = self.pivot
        q = (p[0] - px, p[1] - py, p[2] - pz)
        q = rot_x(q, -self.rx); dd = rot_x(d, -self.rx)
        return (q[0] + px, q[1] + py, q[2] + pz), dd

    def normal_world(self, n):
        if not self.pivot or not self.rx:
            return n
        return rot_x(n, self.rx)


class Plane:
    """a flat textured card (a held item, fletching, a staff head): origin, the u and v edge vectors in
    model px, a texture stretched over them; drawn from both sides"""
    def __init__(self, origin, u, v, tex):
        self.o = origin; self.u = u; self.v = v; self.tex = tex
        n = (u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0])
        ln = math.sqrt(sum(c * c for c in n)) or 1.0
        self.n = tuple(c / ln for c in n)


FACE_N = {'front': (0, 0, 1), 'back': (0, 0, -1), 'right': (-1, 0, 0), 'left': (1, 0, 0),
          'top': (0, 1, 0), 'bottom': (0, -1, 0)}


def light(n):
    """inventory light on a view-space normal: tops brightest, then the face toward the camera; the side
    turned toward the upper left gets a little more than the one turned away"""
    nx, ny, nz = n
    b = 0.34 + 0.64 * max(0.0, nz) + 0.40 * max(0.0, ny) - 0.10 * max(0.0, -ny) + 0.04 * max(0.0, -nx)
    return max(0.3, min(1.0, b))


def face_uv(face, p, b0, b1, tex):
    x0, y0, z0 = b0; x1, y1, z1 = b1
    X, Y, Z = p
    if face == 'front':
        u = (X - x0) / (x1 - x0); v = (y1 - Y) / (y1 - y0)
    elif face == 'back':
        u = (x1 - X) / (x1 - x0); v = (y1 - Y) / (y1 - y0)
    elif face == 'right':
        u = (Z - z0) / (z1 - z0); v = (y1 - Y) / (y1 - y0)
    elif face == 'left':
        u = (z1 - Z) / (z1 - z0); v = (y1 - Y) / (y1 - y0)
    elif face == 'top':
        u = (X - x0) / (x1 - x0); v = (Z - z0) / (z1 - z0)
    else:
        u = (X - x0) / (x1 - x0); v = (z1 - Z) / (z1 - z0)
    return tex.at(u * tex.w, v * tex.h)


NAMES = (('right', 'left'), ('bottom', 'top'), ('back', 'front'))


def box_hits(box, o, d):
    o, d = box.local(o, d)
    tmin, tmax = -1e9, 1e9
    fin = fout = None
    for k in range(3):
        if abs(d[k]) < 1e-12:
            if o[k] < box.b0[k] or o[k] > box.b1[k]:
                return []
            continue
        t0 = (box.b0[k] - o[k]) / d[k]; t1 = (box.b1[k] - o[k]) / d[k]
        n0, n1 = NAMES[k]
        if t0 > t1:
            t0, t1 = t1, t0; e_in, e_out = n1, n0
        else:
            e_in, e_out = n0, n1
        if t0 > tmin:
            tmin, fin = t0, e_in
        if t1 < tmax:
            tmax, fout = t1, e_out
    if tmin > tmax or tmax < 0:
        return []
    out = []
    for (t, f, inside) in ((tmin, fin, False), (tmax, fout, True)):
        tex = box.faces.get(f)
        if tex is None:
            continue
        p = (o[0] + d[0] * t, o[1] + d[1] * t, o[2] + d[2] * t)
        c = face_uv(f, p, box.b0, box.b1, tex)
        if c is None:
            continue
        out.append((t, c, box.normal_world(FACE_N[f]), inside))
    return out


def plane_hits(pl, o, d):
    dn = sum(d[i] * pl.n[i] for i in range(3))
    if abs(dn) < 1e-9:
        return []
    t = sum((pl.o[i] - o[i]) * pl.n[i] for i in range(3)) / dn
    p = [o[i] + d[i] * t - pl.o[i] for i in range(3)]
    uu = sum(c * c for c in pl.u); vv = sum(c * c for c in pl.v)
    a = sum(p[i] * pl.u[i] for i in range(3)) / uu
    b = sum(p[i] * pl.v[i] for i in range(3)) / vv
    if not (0 <= a < 1 and 0 <= b < 1):
        return []
    c = pl.tex.at(a * pl.tex.w, b * pl.tex.h)
    if c is None:
        return []
    n = pl.n if dn < 0 else tuple(-x for x in pl.n)
    return [(t, c, n, False)]


def shade(c, b):
    r, g, bl, _ = rgba(c)
    # shadows lean a touch cool, as the rest of the screen does
    k = 1.0 - b
    return (round(r * b), round(g * b), round(min(255, bl * b + 10 * k)), 255)


def render(model, win, tall=0, sil=None, rim=None, dx=0.0):
    """paint the model into a win x win window; sil gives a flat silhouette colour (the locked rows)"""
    boxes, planes = model
    top, bot = 36.0 + tall, 16.0
    s = win / (top - bot)
    S = Spr(win, win)
    d = from_view((0.0, 0.0, -1.0))
    mask = set()
    for py in range(win):
        for px in range(win):
            X = (px + 0.5 - win / 2.0) / s + dx
            Y = top - (py + 0.5) / s
            o = from_view((X, Y, 100.0))
            hs = []
            for b in boxes:
                hs += box_hits(b, o, d)
            for pl in planes:
                hs += plane_hits(pl, o, d)
            if not hs:
                continue
            t, c, n, inside = min(hs, key=lambda h: h[0])
            mask.add((px, py))
            if sil:
                continue
            b = 0.42 if inside else light(to_view(n))
            S.set(px, py, shade(c, b))
    if sil:
        for (x, y) in mask:
            edge = (x - 1, y) not in mask or (x, y - 1) not in mask
            S.set(x, y, rim if (rim and edge) else sil)
    return S


# ================================================================ the biped, as the vanilla model lays it out
def mail(w, h, ph=0):
    """chain mail: steel with the rings dark in a staggered lattice"""
    return [''.join('N' if (x + (y % 2) * 2 + ph) % 4 == 0 else 'M' for x in range(w)) for y in range(h)]


def biped(P, head, body, rarm, larm, hat=None, hat_g=0.5, coat=None, coat_g=1.0, rsl=None, lsl=None, sl_g=1.0,
          extra=(), planes=(), r_fwd=22.0, l_fwd=-6.0):
    """P: palette. head/body/rarm/larm/hat/coat/rsl/lsl: dicts of face name to rows. extra: more boxes."""
    def faces(d):
        return {k: Tex(v, P) for k, v in d.items()} if d else {}
    b = [Box(-4, 24, -4, 4, 32, 4, faces(head)),
         Box(-4, 12, -2, 4, 24, 2, faces(body)),
         Box(-8, 12, -2, -4, 24, 2, faces(rarm), pivot=(-5, 22, 0), rx=r_fwd),
         Box(4, 12, -2, 8, 24, 2, faces(larm), pivot=(5, 22, 0), rx=l_fwd)]
    if hat:
        b.append(Box(-4, 24, -4, 4, 32, 4, faces(hat), inflate=hat_g))
    if coat:
        b.append(Box(-4, 12, -2, 4, 24, 2, faces(coat), inflate=coat_g))
    if rsl:
        b.append(Box(-8, 12, -2, -4, 24, 2, faces(rsl), inflate=sl_g, pivot=(-5, 22, 0), rx=r_fwd))
    if lsl:
        b.append(Box(4, 12, -2, 8, 24, 2, faces(lsl), inflate=sl_g, pivot=(5, 22, 0), rx=l_fwd))
    for (x0, y0, z0, x1, y1, z1, d) in extra:
        b.append(Box(x0, y0, z0, x1, y1, z1, faces(d)))
    return b, list(planes)


# ---------------------------------------------------------------- Gilded palette (spec colours, flat tones)
GILD = {
    # skin
    'K': '#e3b591', 'k': '#c99671', 'D': '#a8775a', 'X': '#7d5340',
    'w': '#e9e3d6', 'p': '#34435e', 'm': '#8c4c3c', 'h': '#5e4129', 'y': '#8e8a84',
    # enamel #D9D3C1
    'F': '#eeeadf', 'E': '#d9d3c1', 'e': '#bcb5a2', 'f': '#9d9683',
    # steel #7C8591
    'U': '#a3abb5', 'S': '#7c8591', 's': '#626a75', 'T': '#464c55', 'M': '#7c8591', 'N': '#5a616c',
    # gilt #96783A
    'H': '#c09a52', 'G': '#96783a', 'J': '#6e5527',
    # linen #BFB59A, the portrait panel linen #CFC6B0, the white cross
    'L': '#bfb59a', 'l': '#a39a80', 'O': '#867d66', 'n': '#d6cdb3', 'P': '#cfc6b0', 'Q': '#b5ac96', 'W': '#f3efe4',
    # yeoman brown #6B5136 and leather
    'B': '#6b5136', 'b': '#523d28', 'V': '#86684a', 'r': '#5b4330',
    'k2': '#000000',
    'q': '#1b1b21', 'z': '#33333b',
}


DARK8 = ['qqqqqqqq'] * 8
DARK4 = ['qqqq'] * 12
DARKB = ['qqqqqqqq'] * 12


# ---------------------------------------------------------------- Templar: closed flat-topped great helm,
# white plate, knee-length tabard (linen in the portrait) with a white cross
def templar():
    helm = {
        'front': ['FFEEEEEe',
                  'FEEHGEEe',
                  'EEEHGEEe',
                  'qqqHGqqq',
                  'EEEHGEEe',
                  'EEEHGEze',
                  'eEEHGEEz',
                  'feeJJeef'],
        'right': ['eeEEEEEF',
                  'eEEEEEEE',
                  'eEEEEEEE',
                  'eEEEEqqq',
                  'eEESEEEE',
                  'eEEEEEEE',
                  'feEEEEEe',
                  'ffeeeeef'],
        'left': ['FEEEEEee', 'EEEEEEEe', 'EEEEEEEe', 'qqqEEEEe', 'EEEESEEe', 'EEEEEEEe', 'eEEEEEef', 'feeeeeff'],
        'back': ['eEEEEEEe'] * 7 + ['feeeeeef'],
        'bottom': ['TTTTTTTT'] + ['qqqqqqqq'] * 7,
        'top': ['FFFFFFFE', 'FEEEEEEe', 'FEEEEEEe', 'FEEEEEEe', 'FEEEEEEe', 'FEEEEEEe', 'FEEEEEEe', 'Eeeeeeee'],
    }
    coat = {
        'front': ['eFEEEEFe',
                  'EPPWWPPE',
                  'EWWWWWWE',
                  'EPPWWPPE',
                  'EPPWWPPE',
                  'EQPWWPQE',
                  'eQPWWPQe',
                  'eQPWWPQe',
                  'JGGHGGGJ',
                  'eQPPPPQe',
                  'eQPPPPQe',
                  'fQQPPQQf'],
        'right': ['eEEF', 'eEEP', 'eEEW', 'eEEP', 'eEEP', 'eEEQ', 'eeEQ', 'eeEQ', 'JGGG', 'eeEQ', 'eeEQ', 'feeQ'],
        'left': ['FEEe', 'PEEe', 'WEEe', 'PEEe', 'PEEe', 'QEEe', 'QEee', 'QEee', 'GGGJ', 'QEee', 'QEee', 'Qeef'],
        'back': ['eEEEEEEe'] * 12,
        'bottom': ['eeeeeeee'] * 4, 'top': ['FEEEEEEe'] * 4,
    }
    arm = {
        'front': ['FFEe', 'EEEe', 'HGGJ', 'EEEe', 'eEEf', 'sSSs', 'EEEe', 'EEEe', 'eEEf', 'EEEe', 'sSSs', 'TssT'],
        'right': ['eFFE', 'eEEE', 'JGGH', 'eEEE', 'eEEE', 'sSSU', 'eEEE', 'eEEE', 'feEe', 'eEEE', 'sSSS', 'TssT'],
        'left': ['EFFe', 'EEEe', 'HGGJ', 'EEEe', 'EEEe', 'USSs', 'EEEe', 'EEEe', 'eEef', 'EEEe', 'SSSs', 'TssT'],
        'back': ['eEEe'] * 12, 'top': ['FEEe'] * 4, 'bottom': ['TssT'] * 4,
    }
    return biped(GILD, {'front': DARK8, 'right': DARK8}, {'front': DARKB, 'right': DARK4},
                 {'front': DARK4, 'right': DARK4}, {'front': DARK4},
                 hat=helm, hat_g=1.25, coat=coat, coat_g=1.0, rsl=arm, lsl=arm, sl_g=1.0, r_fwd=12.0)


# ---------------------------------------------------------------- Order Squire: iron cap, mail coif, white
# surcoat with the linen panel, mail sleeves; young and clean-shaven
def squire(cap_white=False, cross=False):
    head = {
        'front': ['hhhhhhhh',
                  'hhhhhhhh',
                  'MKKKKKKN',
                  'NhhKKhhM',
                  'MwpKKpwN',
                  'NKkDDkKM',
                  'MKKmmKKN',
                  'NMkKKkMN'],
        'right': mail(8, 8, 1)[:7] + ['MNMNMNMk'],
        'left': mail(8, 8, 0),
        'back': mail(8, 8, 0), 'top': ['hhhhhhhh'] * 8,
        'bottom': ['NMNMNMNM'] + ['qqqqqqqq'] * 7,
    }
    if cap_white:
        # Kort: a white kettle hat with a gilt rim
        cap = {'front': ['FFFEEEEe', 'EEEEEEEe', 'JGGHGGGJ'] + ['........'] * 5,
               'right': ['eEEEEEFF', 'eEEEEEEE', 'JGGGGHGG'] + ['........'] * 5,
               'left': ['FFEEEEEe', 'EEEEEEEe', 'GGHGGGGJ'] + ['........'] * 5,
               'back': ['eEEEEEEe', 'eEEEEEEe', 'JGGGGGGJ'] + ['........'] * 5,
               'top': ['FFFFFFFE'] + ['FEEEEEEe'] * 6 + ['Eeeeeeee'], 'bottom': ['........'] * 8}
        brim = {'front': ['GHGGGGGGGGHG'], 'right': ['GGGGGGGGGGGH'], 'left': ['HGGGGGGGGGGG'],
                'back': ['GGGGGGGGGGGG'], 'bottom': ['ffffffffffff'] * 12, 'top': ['EEEEEEEEEEEE'] * 12}
        extra = [(-6.5, 28.6, -6.5, 6.5, 29.6, 6.5, brim)]
    else:
        cap = {'front': ['UUSSSSSs', 'SSSSSSSs', 'TTTTTTTT'] + ['........'] * 5,
               'right': ['sSSSSSUU', 'sSSSSSSS', 'TTTTTTTT'] + ['........'] * 5,
               'left': ['UUSSSSSs', 'SSSSSSSs', 'TTTTTTTT'] + ['........'] * 5,
               'back': ['sSSSSSSs', 'sSSSSSSs', 'TTTTTTTT'] + ['........'] * 5,
               'top': ['UUUUUUUS'] + ['USSSSSSs'] * 6 + ['Ssssssss'], 'bottom': ['........'] * 8}
        extra = []
    if cross:
        panel = ['NMNMNMNM', 'EEEHGEEE', 'EEEHGEEE', 'EHGHGHGE', 'EJJHGJJE', 'eEEHGEEe', 'eEEHGEEe', 'eeEHGEee',
                 'JGGGGGGJ', 'eeEJJEee', 'eeEEEEee', 'feeeeeef']
    else:
        panel = ['NMNMNMNM', 'EEEEEEEE', 'EEPPPPEE', 'EePPPPeE', 'EePPPPeE', 'EePPPPeE', 'eePPPPee', 'eeQPPQee',
                 'JGGHGGGJ', 'eeQQQQee', 'eeQQQQee', 'feQQQQef']
    coat = {'front': panel,
            'right': ['MNMN', 'eEEE', 'eEEE', 'eEEE', 'eEEE', 'eEEE', 'eeEE', 'eeEE', 'JGGG', 'eeEe', 'eeEe', 'feef'],
            'left': ['NMNM', 'EEEe', 'EEEe', 'EEEe', 'EEEe', 'EEEe', 'EEee', 'EEee', 'GGGJ', 'eEee', 'eEee', 'feef'],
            'back': ['eEEEEEEe'] * 12, 'top': ['NMNMNMNM'] * 4, 'bottom': ['eeeeeeee'] * 4}
    sleeve = {'front': mail(4, 10, 0) + ['bBBb', 'bbbb'], 'right': mail(4, 10, 1) + ['bBBV', 'bbbb'],
              'left': mail(4, 10, 1) + ['VBBb', 'bbbb'], 'back': mail(4, 12, 0),
              'top': mail(4, 4, 0), 'bottom': ['bbbb'] * 4}
    return biped(GILD, head, {'front': DARKB, 'right': DARK4}, {'front': DARK4, 'right': DARK4}, {'front': DARK4},
                 hat=cap, hat_g=1.25 if not cap_white else 1.0, coat=coat, coat_g=0.5, rsl=sleeve, lsl=sleeve,
                 sl_g=0.5, extra=extra, r_fwd=12.0)


# ---------------------------------------------------------------- Order Archer: linen hood with its point,
# face in shadow, quilted gambeson, the linen jerkin, a quiver strap; white fletching over the shoulder
def archer(green=False):
    P = dict(GILD)
    if green:
        # Ansel: the same cut in a green wool hood (a player skin, not a unit)
        P.update({'L': '#5f8a4f', 'l': '#4b7040', 'O': '#355230', 'n': '#7da868'})
    head = {
        'front': ['hhhhhhhh',
                  'hhhhhhhh',
                  'hXXXXXXh',
                  'hXhDDhXh',
                  'hypDDpyh',
                  'hDDXXDDh',
                  'hDDmmDDh',
                  'hXDDDDXh'],
        'right': ['hhhhhhhh'] * 4 + ['hhhhhhXD', 'hhhhhhDD', 'hhhhhXDD', 'hhhhhhXX'],
        'left': ['hhhhhhhh'] * 8, 'back': ['hhhhhhhh'] * 8, 'top': ['hhhhhhhh'] * 8,
        'bottom': ['XXXXXXXX'] + ['qqqqqqqq'] * 7,
    }
    hood = {
        'front': ['nLLLLLLl',
                  'LLLLLLLl',
                  'LO....Ol',
                  'L......l',
                  'L......l',
                  'L......O',
                  'LL....lO',
                  'LLl..lOO'],
        'right': ['lLLLLLnn', 'lLLLLLLn', 'lLLLLLLL', 'lLLlLLLL', 'lLLLLLLL', 'lLLLLLLl', 'OlLLLLLl', 'OllLLLll'],
        'left': ['nLLLLLLl', 'LLLLLLLl', 'LLLLLLLl', 'LLLLlLLl', 'LLLLLLLl', 'lLLLLLLO', 'lLLLLLlO', 'llLLLllO'],
        'back': ['LLLLLLLl'] * 6 + ['lLLLLLlO', 'OllllllO'],
        'top': ['nnLLLLLL'] + ['nLLLLLLl'] * 6 + ['Llllllll'],
        'bottom': ['OO....OO', 'OO....OO', 'OOO..OOO'] + ['OOOOOOOO'] * 5,
    }
    peak = {'front': ['nLLl', 'LLLl', 'lLlO'], 'right': ['lLLn', 'lLLL', 'OlLl'], 'left': ['nLLl', 'LLLl', 'lLlO'],
            'back': ['LLLl'] * 3, 'top': ['nLLl'] * 3, 'bottom': ['OOOO'] * 3}
    tip = {'front': ['Ll', 'lO'], 'right': ['lL', 'Ol'], 'left': ['Ll', 'lO'], 'back': ['Ll'] * 2,
           'top': ['nL'] * 2, 'bottom': ['OO'] * 2}
    coat = {'front': ['nLLLLLLl',
                      'LBLLLlLl',
                      'lPBPPPPl',
                      'lPPBPPPl',
                      'lPPPBPPl',
                      'lQPPPBPl',
                      'lQPPPPBl',
                      'lQQPPPPB',
                      'bBBVBBBb',
                      'lQQQQQQl',
                      'lQQQQQQl',
                      'OQQQQQQO'],
            'right': ['lLLn', 'lLLB', 'OlLl', 'OlLl', 'OlLl', 'OlLl', 'OllL', 'OllL', 'bBBB', 'Olll', 'Olll', 'OOll'],
            'left': ['nLLl', 'BLLl', 'lLlO', 'lLlO', 'lLlO', 'lLlO', 'LllO', 'LllO', 'BBBb', 'lllO', 'lllO', 'llOO'],
            'back': ['LLLLLLLL', 'BLLLLLLL'] + ['lBLLLLLl'] * 10, 'top': ['nLLLLLLl'] * 4, 'bottom': ['OOOOOOOO'] * 4}
    sleeve = {'front': ['LLLl', 'llll', 'LLLl', 'llll', 'LLLl', 'llll', 'LLLl', 'llll', 'bBBb', 'BVVB', 'bBBb', 'bbbb'],
              'right': ['nLLL', 'llll', 'LLLL', 'llll', 'LLLL', 'llll', 'LLLL', 'llll', 'bBBB', 'BVVB', 'bBBB', 'bbbb'],
              'left': ['LLLn', 'llll', 'LLLL', 'llll', 'LLLL', 'llll', 'LLLL', 'llll', 'BBBb', 'BVVB', 'BBBb', 'bbbb'],
              'back': ['llll'] * 12, 'top': ['nLLl'] * 4, 'bottom': ['bbbb'] * 4}
    # the quiver on the back: white fletching over the left shoulder, nocks dark
    quiv = {'front': ['WnWn', 'nWQW', 'WQnQ', 'QnQn', 'bBBb', 'BbbB', 'bBBb', 'BbbB', 'bBBb', 'BbbB'],
            'right': ['nWnW', 'WQWn', 'QnQW', 'nQnQ', 'bBBb', 'BbbB', 'bBBb', 'BbbB', 'bBBb', 'BbbB'],
            'left': ['WnWn', 'nWQW', 'WQnQ', 'QnQn'] + ['BbbB'] * 6, 'back': ['QnQn'] * 4 + ['BbbB'] * 6,
            'top': ['WnWn', 'nWnW', 'WnWn'], 'bottom': ['bbbb'] * 3}
    extra = [(-2.4, 33.0, -5.6, 2.4, 35.0, -1.6, peak),
             (-1.2, 35.0, -6.6, 1.2, 36.6, -3.8, tip),
             (3.0, 22.0, -5.4, 6.4, 37.0, -2.6, quiv)]
    return biped(P, head, {'front': DARKB, 'right': DARK4}, {'front': DARK4, 'right': DARK4}, {'front': DARK4},
                 hat=hood, hat_g=1.25, coat=coat, coat_g=0.6, rsl=sleeve, lsl=sleeve, sl_g=0.5, extra=extra,
                 r_fwd=12.0)


# ---------------------------------------------------------------- Order Longbow: peaked felt cap, weathered
# with a short beard, gambeson, the linen jerkin and a gilt sash; the longbow rising past the head
def longbow():
    head = {
        'front': ['BBBBBBBB',
                  'BBBBBBBB',
                  'bkKKKKkb',
                  'bhhKkhhb',
                  'bwpkKpwb',
                  'bKkDDkKb',
                  'brrmmrrb',
                  'bbrrrrbb'],
        'right': ['BBBBBBBB'] * 3 + ['bBBBBbkK', 'bBBBbkkK', 'bbBBbrkD', 'bbbbrrrr', 'bbbbbrrr'],
        'left': ['BBBBBBBB'] * 8, 'back': ['BBBBBBBB'] * 8, 'top': ['BBBBBBBB'] * 8,
        'bottom': ['rrrrrrrr'] + ['qqqqqqqq'] * 7,
    }
    cap = {'front': ['VVVVVVVB', 'BBBBBBBb', 'bbbbbbbb'] + ['........'] * 5,
           'right': ['BBBBVVVV', 'bBBBBBBB', 'bbbbbbbb'] + ['........'] * 5,
           'left': ['VVVVBBBB', 'BBBBBBBb', 'bbbbbbbb'] + ['........'] * 5,
           'back': ['BBBBBBBB', 'bBBBBBBb', 'bbbbbbbb'] + ['........'] * 5,
           'top': ['VVVVVVVB'] + ['VBBBBBBb'] * 6 + ['Bbbbbbbb'], 'bottom': ['........'] * 8}
    peak = {'front': ['bbbbbbbb'], 'right': ['bbbb'], 'left': ['bbbb'], 'back': ['bbbbbbbb'],
            'top': ['VVVVVVVV'] * 4, 'bottom': ['rrrrrrrr'] * 4}
    coat = {'front': ['nLLLLLLl',
                      'LPPPPPHl',
                      'lPPPPGPl',
                      'lPPPGJPl',
                      'lPPGJPPl',
                      'lQGJPPPl',
                      'lGJPPPPl',
                      'GJQPPPPl',
                      'bBBVBBBb',
                      'lQQQQQQl',
                      'lQQQQQQl',
                      'OQQQQQQO'],
            'right': ['lLLn', 'OlLL', 'OlLl', 'OlLl', 'OlLl', 'OlLl', 'OllG', 'OllJ', 'bBBB', 'Olll', 'Olll', 'OOll'],
            'left': ['nLLl', 'HLLl', 'GLlO', 'lLlO', 'lLlO', 'lLlO', 'LllO', 'LllO', 'BBBb', 'lllO', 'lllO', 'llOO'],
            'back': ['LLLLLLLL'] * 12, 'top': ['nLLLLLLl'] * 4, 'bottom': ['OOOOOOOO'] * 4}
    sleeve = {'front': ['LLLl', 'llll', 'LLLl', 'llll', 'LLLl', 'llll', 'LLLl', 'llll', 'bBBb', 'BVVB', 'bBBb', 'bbbb'],
              'right': ['nLLL', 'llll', 'LLLL', 'llll', 'LLLL', 'llll', 'LLLL', 'llll', 'bBBB', 'BVVB', 'bBBB', 'bbbb'],
              'left': ['LLLn', 'llll', 'LLLL', 'llll', 'LLLL', 'llll', 'LLLL', 'llll', 'BBBb', 'BVVB', 'BBBb', 'bbbb'],
              'back': ['llll'] * 12, 'top': ['nLLl'] * 4, 'bottom': ['bbbb'] * 4}
    # the longbow: a tall stave held upright in the forward hand, drawn as the game draws a held item,
    # a flat card; the stave bows away from the string, horn nocks at the tips
    rows = []
    H = 34
    for j in range(H):
        t = (j - (H - 1) / 2.0) / ((H - 1) / 2.0)
        off = round(2.2 * (1 - t * t))
        r = ['.'] * 5
        r[0] = 'l' if 1 < j < H - 2 else '.'
        r[min(4, 1 + off)] = 'V' if j % 5 else 'B'
        if 1 + off + 1 <= 4:
            r[1 + off + 1] = 'b'
        if j in (0, H - 1):
            r = ['.', 'n', 'n', '.', '.']
        if 15 <= j <= 18:
            r[min(4, 1 + off)] = 'r'
        rows.append(''.join(r))
    bow = Tex(rows, GILD)
    o = from_view((-11.0, 40.5, 8.0))
    planes = [Plane(o, from_view((5.0, 0.0, 0.0)), from_view((0.0, -float(H), 0.0)), bow)]
    extra = [(-4.6, 29.2, 4.4, 4.6, 29.9, 7.4, peak)]
    return biped(GILD, head, {'front': DARKB, 'right': DARK4}, {'front': DARK4, 'right': DARK4}, {'front': DARK4},
                 hat=cap, hat_g=1.1, coat=coat, coat_g=0.6, rsl=sleeve, lsl=sleeve, sl_g=0.5, extra=extra,
                 planes=planes, r_fwd=16.0)


# ---------------------------------------------------------------- the rows not yet earned: flat silhouettes
def _all(rows_w, rows_h, ch='E'):
    return [ch * rows_w] * rows_h


def _solid6(w, h, d):
    return {'front': _all(w, h), 'back': _all(w, h), 'right': _all(d, h), 'left': _all(d, h),
            'top': _all(w, d), 'bottom': _all(w, d)}


def sunweaver():
    """robed triangle: white hood and robe, the sun disc on its staff 4 px above the head (Tall +6)"""
    disc = Tex(['..EE..', '.EEEE.', 'EEEEEE', 'EEEEEE', '.EEEE.', '..EE..'], GILD)
    extra = [(-10.2, 14.0, 2.2, -9.0, 37.0, 3.4, _solid6(1, 8, 1)),      # the staff
             (-6.0, 12.0, -3.4, 6.0, 20.0, 3.4, _solid6(8, 8, 4))]      # the robe flaring at the chest
    planes = [Plane(from_view((-12.6, 43.0, 9.0)), from_view((6.0, 0.0, 0.0)), from_view((0.0, -6.0, 0.0)), disc)]
    return biped(GILD, _solid6(8, 8, 8), _solid6(8, 12, 4), _solid6(4, 12, 4), _solid6(4, 12, 4),
                 hat=_solid6(8, 8, 8), hat_g=1.6, coat=_solid6(8, 12, 4), coat_g=1.0, extra=extra, planes=planes,
                 r_fwd=18.0)


def marshal():
    """broad under his standard: open helm with a gilt laurel, pauldrons, a grey beard (Tall +4)"""
    laurel = {'front': ['E.E.EE.E.E', 'EEEEEEEEEE'], 'right': ['E.E.EE.E.E', 'EEEEEEEEEE'],
              'left': ['E.E.EE.E.E', 'EEEEEEEEEE'], 'back': ['E.E.EE.E.E', 'EEEEEEEEEE'],
              'top': ['..........'] * 10, 'bottom': ['..........'] * 10}
    extra = [(-5.8, 30.0, -5.8, 5.8, 32.2, 5.8, laurel),
             (-10.0, 20.5, -3.6, -3.4, 25.6, 3.6, _solid6(6, 4, 6)),     # pauldrons
             (3.4, 20.5, -3.6, 10.0, 25.6, 3.6, _solid6(6, 4, 6)),
             (-3.2, 21.6, 3.6, 3.2, 25.0, 5.2, _solid6(6, 3, 2))]        # the beard
    return biped(GILD, _solid6(8, 8, 8), _solid6(8, 12, 4), _solid6(4, 12, 4), _solid6(4, 12, 4),
                 hat=_solid6(8, 8, 8), hat_g=1.1, coat=_solid6(8, 12, 4), coat_g=1.0, extra=extra, r_fwd=10.0)


# ================================================================ the sprites the pages use
ASSETS = {}
UNITS = {
    # name: (model, Tall +N, silhouette)
    'kort': (lambda: squire(cap_white=True, cross=True), 0, False),
    'ansel': (lambda: archer(green=True), 0, False),
    'squire': (squire, 0, False),
    'archer': (archer, 0, False),
    'templar': (templar, 2, False),
    'longbow': (longbow, 2, False),
    'sunweaver': (sunweaver, 6, True),
    'marshal': (marshal, 4, True),
}


def portrait(name, win):
    mk, tall, sil = UNITS[name]
    if sil:
        return render(mk(), win, tall, sil='#46555b', rim='#5a6b72')
    return render(mk(), win, tall)


for _n in UNITS:
    ASSETS['pt-%s-40' % _n] = (lambda n: (lambda: portrait(n, 40)))(_n)
ASSETS['pt-templar-160'] = lambda: portrait('templar', 160)
