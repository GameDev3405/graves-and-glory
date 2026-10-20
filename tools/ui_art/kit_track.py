"""The War Table's Chapter page (UI plan Task 7): the Founders reward track as Kort approved it on 2026-10-18 (grade 9;
work/ui-concepts/track: NOTES.md, track-01-sworn.png, track-02-unsworn.png, track-03-claim.png). The painters below are the
track's own, copied verbatim from trackart.py, trackart2.py, trackart3.py and trackart4.py (each copy names its source); like
those sources they import the v4 kit and change nothing in it. Three were adapted, each where it says so: xp_rail paints no
head while nothing is poured, pennant takes the Oath's other patterns (pennant_ivory), and banner_cloth the crowned and the
night cloths. New here: the XP fill cut out for the game to pour (xp_fill, checked against xp_rail by xp_check), the open
page's plaque at the class plaques' width, the dimmed arrows at the track's ends, and the wax seal a title shows on the hero
letter (seal_title).

Not here: the table props (the candle and the ink pot, kept for other pages; Kort took them off this page), this page's oak
(kit_props.table('chapter') cuts it from the kit's oak with the page's own seed), the Longbow's painted portrait (portraits
are live models), and the art of a House the player is sworn to, which waits for the Oath (Phase 1 shows the Oath row in
sepia only). TRACK lists every texture this module exports, by its folder/name."""
import math, random
from v4lib import Spr, Noise, dith, rgba, mix, emboss, grid_mask
from v4pal import *
from v4a_furn import plaque, brass_button, key_engraved, screw, engrave, brushed, tarnish, arc, SPEC, TARN, SH
from v4a_paper import parchment, wax_seal, ribbon, CROWN, SHADOW, PAPER
from v4a_recruit import HOUSES, DEVICES
import v4a_portrait


# ================================================================ trackart.py: ramps, the hanging plaques, the chains, the rod, the tier tags and the arrows

# tarnished brass for anything not yet reached: the brass ramp pulled halfway to iron and down a step
DULL = [mix(mix(BRASS[i], IRON[min(8, i + 1)], 0.55), '#000000', 0.10) for i in range(8)]


# sepia: the Oath row for a player sworn to no House, drawn as an old brown print
SEP = ['#17120d', '#221a12', '#2f2419', '#3e3022', '#4f3e2c', '#615038', '#776446', '#8c7853', '#a08c64']


RUSTW = ['#3b2214', '#4f2c18', '#63391f']


def sepia(src, lo=0.0, hi=1.0):
    """map every pixel's luma onto the sepia ramp, squeezed into [lo, hi] of it; alpha kept"""
    o = Spr(src.w, src.h)
    for y in range(src.h):
        for x in range(src.w):
            p = src.px[x, y]
            if p[3] == 0:
                continue
            l = (0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]) / 255.0
            t = lo + (hi - lo) * l
            c = SEP[max(0, min(len(SEP) - 1, int(t * (len(SEP) - 0.01))))]
            o.px[x, y] = rgba(c)[:3] + (p[3],)
    return o


def soft_shadow(S, steps):
    """a stepped shadow under an opaque shape: steps = [(dx, dy, alpha), ...] nearest first"""
    m = {(x, y) for y in range(S.h) for x in range(S.w) if S.px[x, y][3] > 200}
    sh = Spr(S.w, S.h)
    for (dx, dy, a) in steps:
        for (x, y) in m:
            k = (x + dx, y + dy)
            if k not in m and sh.get(*k)[3] < a * 255:
                sh.set(k[0], k[1], rgba(SH, round(a * 255)))
    sh.blit(S, 0, 0)
    S.im = sh.im; S.px = sh.px
    return S


def pad(S, l=0, t=0, r=0, b=0):
    o = Spr(S.w + l + r, S.h + t + b)
    o.blit(S, l, t)
    return o


def tier_plaque(w=68, h=80, seed=1, kind='lit'):
    """a brass-framed oak plaque, made as the rail's nav plaques are, taller: a 4 px frame (outline, lit
    outer bevel, brushed face, inner bevel falling into the panel), a slotted screw in each corner, grime
    by the screws, the recessed quarter-sawn panel. kind: lit (reached), raised (the current tier: the
    brass polished, a deeper shadow), dull (not reached: the brass tarnished toward iron, the oak darker)"""
    rnd = random.Random(seed * 31 + w + h)
    M = DULL if kind == 'dull' else (BRASS[1:] + [SPEC] if kind == 'raised' else BRASS)
    s = Spr(w, h)
    s.rect(0, 0, w, h, M[0])
    s.rect(1, 1, w - 2, h - 2, M[5])
    brushed(s, 2, 2, w - 2, 4, rnd, 2.0, m=M)
    brushed(s, 2, h - 4, w - 2, h - 2, rnd, 2.0, m=M)
    for y in range(4, h - 4):
        for x in (2, w - 3):
            if rnd.random() < 0.3:
                s.set(x, y, M[6] if x == 2 else M[4])
    s.hline(1, w - 2, 1, M[6]); s.vline(1, 1, h - 2, M[6])
    s.hline(1, w - 2, h - 2, M[3]); s.vline(w - 2, 1, h - 2, M[3])
    s.set(w - 2, 1, M[5]); s.set(1, h - 2, M[4])
    glint = SPEC if kind == 'raised' else M[7]
    for x in range(3, w - 3):
        if rnd.random() < (0.22 if kind == 'raised' else 0.12):
            s.set(x, 1, M[7])
    for k in range(8 if kind == 'raised' else 6):
        s.set(2 + k, 1, glint if k < 4 else M[7])
    s.set(1, 2, M[7]); s.set(1, 3, M[7])
    if kind == 'raised':
        for k in range(5):
            s.set(1, 4 + k, M[7] if k < 3 else M[6])
    for x in range(2, w - 2):
        if s.get(x, 2)[:3] == rgba(M[5])[:3] and rnd.random() < 0.25:
            s.set(x, 2, M[6])
    s.hline(3, w - 4, 3, M[3]); s.vline(3, 3, h - 4, M[3])
    s.hline(3, w - 4, h - 4, M[6]); s.vline(w - 4, 3, h - 4, M[6])
    s.set(3, 3, M[2]); s.set(w - 4, h - 4, M[7])
    # the oak panel, quarter-sawn: straight grain in runs, pale ray flecks across it
    base = 1 if kind == 'dull' else (3 if kind == 'raised' else 2)
    for y in range(4, h - 4):
        for x in range(4, w - 4):
            s.set(x, y, OAK[base])
    for k in range(int((h - 8) / 2.0) + 1):
        yb = 4.6 + k * 2.0 + rnd.random() * 0.8
        amp = rnd.uniform(0.2, 0.7); L = rnd.uniform(50, 130); ph = rnd.uniform(0, 6.28)
        tone = rnd.choice([base - 1, base - 1, base + 1, base + 1, base + 1])
        x = rnd.randint(-20, 4); on = True
        while x < w:
            run = rnd.randint(10, 40) if on else rnd.randint(3, 12)
            if on:
                for xx in range(max(5, x), min(w - 5, x + run)):
                    Y = round(yb + amp * math.sin(6.283 * xx / L + ph))
                    if 5 <= Y <= h - 6:
                        s.set(xx, Y, OAK[max(0, tone)])
            on = not on; x += run
    for _ in range(int(w * h / 260)):
        x = rnd.randint(6, w - 9); y = rnd.randint(6, h - 7)
        for i in range(rnd.randint(2, 3)):
            s.set(x + i, y, OAK[base + 2] if i else OAK[base + 1])
    s.hline(4, w - 5, 4, OAK[0]); s.vline(4, 4, h - 5, OAK[0])
    s.hline(5, w - 5, 5, OAK[max(0, base - 1)]); s.vline(5, 5, h - 6, OAK[max(0, base - 1)])
    s.hline(5, w - 5, h - 5, OAK[base + 1])
    slots = rnd.sample(['-', '|', '/', 'x'], 4)
    for i, (cx, cy) in enumerate(((1, 1), (w - 6, 1), (1, h - 6), (w - 6, h - 6))):
        tarnish(s, cx + 2, cy + 2, 2.2, rnd, 5, keep=lambda X, Y: 4 <= X < w - 4 and 4 <= Y < h - 4)
        screw(s, cx, cy, slots[i], metal=M)
    if kind == 'dull':
        # verdigris and grime along the frame, where the damp sits
        for _ in range(18):
            X = rnd.randint(6, w - 7); Y = rnd.choice([h - 3, h - 2, 2])
            s.set(X, Y, TARN[rnd.randint(0, 2)])
    if kind == 'raised':
        return soft_shadow(pad(s, 0, 0, 4, 5), [(1, 1, 0.62), (2, 2, 0.46), (3, 3, 0.3), (4, 5, 0.14)])
    return soft_shadow(pad(s, 0, 0, 2, 3), [(1, 1, 0.55), (2, 3, 0.25)])


def chain(n=18, metal=BRASS):
    """a hanging chain: links alternate face-on (an oval ring, lit down its left side) and edge-on (a
    bar, lit on its left), each link through the last; a ring at the foot that sits on the plaque's frame"""
    m = metal
    S = Spr(6, n + 3)
    face = ['.hm.', 'h..d', 'h..d', 'm..d', '.dd.']
    edge = ['hk', 'mk', 'mk', 'dk']
    y = 0; i = 0
    while y < n - 1:
        if i % 2 == 0:
            S.grid(0, y, face, {'h': m[7], 'm': m[5], 'd': m[3]})
            y += 4
        else:
            S.grid(1, y, edge, {'h': m[7], 'm': m[5], 'd': m[3], 'k': m[1]})
            y += 3
        i += 1
    S.grid(0, n - 1, ['.hm.', 'h..d', '.dd.'], {'h': m[6], 'm': m[5], 'd': m[3]})
    for yy in range(S.h):
        for xx in range(S.w):
            if yy > n + 1:
                S.set(xx, yy, None)
    return soft_shadow(S, [(1, 1, 0.5)])


def track_rod(w=784):
    """the brass rod the tiers hang from: six tones round its curve, glints along the lit line, a turned
    collar at each end; the ends sit in the arrow bosses"""
    S = Spr(w, 11)
    rows = ((1, BRASS[0]), (2, BRASS[6]), (3, BRASS[7]), (4, BRASS[5]), (5, BRASS[4]), (6, BRASS[3]),
            (7, BRASS[2]), (8, BRASS[0]))
    rnd = random.Random(5)
    for x in range(w):
        for y, c in rows:
            S.set(x, y, c)
        if x % 9 == 0:
            S.set(x, 3, SPEC)
        if x % 13 == 5:
            S.set(x, 5, BRASS[3])
        if rnd.random() < 0.05:
            S.set(x, 6, TARN[1])
    for xc in (4, w - 6):
        for y, c in ((0, BRASS[0]), (1, BRASS[6]), (2, BRASS[7]), (3, BRASS[6]), (4, BRASS[5]), (5, BRASS[4]),
                     (6, BRASS[3]), (7, BRASS[2]), (8, BRASS[1]), (9, BRASS[0])):
            S.set(xc, y, c); S.set(xc + 1, y, BRASS[max(1, BRASS.index(c) - 1)] if c != BRASS[0] else c)
    for x in range(w):
        S.set(x, 9, rgba(SH, 120)); S.set(x, 10, rgba(SH, 50))
    return S


def tier_tag(w=21, h=15, metal=BRASS):
    """the tier's number tag: a brass sleeve clamped round the rod, wider than the rod so it shows above
    and below it; shaded as a cylinder (lit band on top, flat face for the number, a dark lower band),
    its ends turned down, a seam at each end"""
    m = metal
    S = Spr(w, h)
    bands = [m[0], m[7], m[6], m[5], m[5], m[5], m[5], m[5], m[5], m[5], m[5], m[4], m[3], m[2], m[0]]
    for y in range(h):
        for x in range(w):
            S.set(x, y, bands[min(len(bands) - 1, y)])
    for y in range(1, h - 1):
        S.set(0, y, m[0]); S.set(w - 1, y, m[0])
        S.set(1, y, m[3] if y > 2 else m[6]); S.set(w - 2, y, m[2] if y > 2 else m[4])
        S.set(2, y, m[4] if 2 < y < h - 3 else S.get(2, y)); S.set(w - 3, y, m[4] if 2 < y < h - 3 else S.get(w - 3, y))
    for (x, y) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        S.set(x, y, None)
    S.set(3, 1, SPEC); S.set(4, 1, SPEC); S.set(5, 1, m[7])
    for x in range(6, w - 4, 4):
        S.set(x, 2, m[7])
    return soft_shadow(pad(S, 0, 0, 2, 2), [(1, 1, 0.55), (2, 2, 0.22)])


def arrow_stud(direction=1, dim=False):
    """a domed brass boss at the rod's end with a chevron cut into it: the way to the tiers off the table"""
    m = DULL if dim else BRASS
    S = Spr(15, 15)
    c = 7.0
    for y in range(15):
        for x in range(15):
            d = math.hypot(x - c, y - c)
            if d <= 7.2:
                l = -(x - c) / 7.0 - (y - c) / 7.0
                col = m[7] if l > 0.95 else m[6] if l > 0.45 else m[5] if l > -0.2 else m[4] if l > -0.75 else m[3]
                if d > 6.2:
                    col = m[0] if l < 0.4 else m[2]
                S.set(x, y, col)
    S.set(4, 4, SPEC); S.set(5, 4, m[7]); S.set(4, 5, m[7])
    chev = [(0, 0), (1, 1), (2, 2), (3, 3), (2, 4), (1, 5), (0, 6)]
    if direction < 0:
        chev = [(3 - x, y) for (x, y) in chev]
    pts = [(5 + x, 4 + y) for (x, y) in chev]
    pts += [(x + 1, y) for (x, y) in pts]
    engrave(S, pts, m)
    return soft_shadow(pad(S, 0, 0, 2, 2), [(1, 1, 0.55), (2, 2, 0.22)])


# ================================================================ trackart2.py: the Hollow's mark, the tier seal, the hasp and the Roll's icons

# The company's device as a signet cuts it: the standard (the Hollow's banner pole and cloth, the thing
# the player carries into every battle), the cloth cut in a swallowtail, a ring round it.
HOLLOW = ['.x.......',
          '.xxxxxxx.',
          '.xxxxxx..',
          '.xxxxx...',
          '.xxxxxx..',
          '.xxxxxxx.',
          '.x.......',
          '.x.......',
          'xxx......']


def _blob(r, seed, lobes=()):
    rnd = random.Random(seed)
    p1, p2 = rnd.uniform(0, 6.28), rnd.uniform(0, 6.28)
    size = 2 * r + 7
    c = size / 2 - 0.5

    def R(th):
        return r + 0.6 * math.sin(3 * th + p1) + 0.45 * math.sin(5 * th + p2)
    shape = set()
    for y in range(size):
        for x in range(size):
            dx, dy = x - c, y - c
            if math.hypot(dx, dy) <= R(math.atan2(dy, dx)):
                shape.add((x, y))
    for (ang, ln, wd) in lobes:
        for k in range(ln + 2):
            rr = r - 1 + k
            X, Y = round(c + rr * math.cos(ang)), round(c + rr * math.sin(ang))
            w = wd if k < ln else 1
            for a in range(-w + 1, w):
                for b in range(-w + 1, w):
                    if abs(a) + abs(b) < w + (1 if k < ln - 1 else 0):
                        shape.add((X + a, Y + b))
    return shape, c, R, size


def hollow_stamp(r=11, seed=21, fresh=False, drips=((1.9, 2, 2),)):
    """a claimed tier: red wax pressed flat with the company's signet. The lip squeezed out round the
    signet, its slope lit on the top left; the flat of the stamp; the ring cut in the signet and the
    standard inside it both sunk, their top-left walls in shadow and their lower-right walls catching
    the light. A fresh stamp is still wet: brighter on the lip, a drip running down."""
    shape, c, R, size = _blob(r, seed, drips if fresh else ())
    S = Spr(size, size)
    lx, ly = -0.7071, -0.7071
    for (x, y) in shape:
        dx, dy = x - c, y - c
        d = math.hypot(dx, dy) + 1e-6
        facing = (dx / d) * lx + (dy / d) * ly
        Rt = R(math.atan2(dy, dx))
        if d > Rt - 2.2:
            col = WAX[5] if facing > 0.5 else (WAX[4] if facing > -0.1 else (WAX[3] if facing > -0.6 else WAX[2]))
        else:
            col = WAX[3] if (x * 7 + y * 3) % 11 else WAX[4]
        S.set(x, y, col)
    # the signet's ring, cut in, and the standard inside it, sunk
    ring = {(x, y) for (x, y) in shape if abs(math.hypot(x - c, y - c) - (r - 3.3)) < 0.55}
    dev = grid_mask(HOLLOW)
    ox, oy = round(c - 3.6), round(c - 4.4)
    sunk = ring | {(ox + x, oy + y) for (x, y) in dev}
    for (x, y) in sunk:
        S.set(x, y, WAX[2])
    for (x, y) in sunk:
        if (x - 1, y) not in sunk or (x, y - 1) not in sunk:
            S.set(x, y, WAX[1])
    for (x, y) in sunk:
        for (a, b) in ((1, 0), (0, 1), (1, 1)):
            k = (x + a, y + b)
            if k not in sunk and k in shape:
                S.set(k[0], k[1], WAX[5] if fresh else WAX[4])
                break
    outline = {(x + a, y + b) for (x, y) in shape for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - shape
    for p in outline:
        S.set(*p, WAX[0])
    gx, gy = round(c - r * 0.62), round(c - r * 0.62)
    for (a, b) in ((0, 0), (1, 0), (0, 1)) + (((2, -1), (-1, 2), (3, -1)) if fresh else ()):
        if (gx + a, gy + b) in shape:
            S.set(gx + a, gy + b, WAX[6])
    if fresh:
        for (x, y) in shape:
            if (x - 1, y - 1) not in shape and (x, y) not in sunk and math.hypot(x - c, y - c) > r - 2:
                S.set(x, y, WAX[6] if (x + y) % 3 else WAX[5])
    return soft_shadow(pad(S, 0, 0, 2, 3), [(1, 1, 0.5), (1, 2, 0.4), (2, 3, 0.18)])


def num_seal(r=9, seed=5, drips=(), lip=2.4):
    """a seal poured and pressed with a blank signet: the poured lip round it, lit on the top left; the
    pressed field sunk below the lip, its top-left wall in shadow, its lower-right wall lit; the field
    flat and quiet, where the game writes the number; drips; a glint on the lip"""
    shape, c, R, size = _blob(r, seed, drips)
    S = Spr(size, size)
    lx, ly = -0.7071, -0.7071
    nz = Noise(seed + 9, 3)
    for (x, y) in shape:
        dx, dy = x - c, y - c
        d = math.hypot(dx, dy) + 1e-6
        facing = (dx / d) * lx + (dy / d) * ly
        Rt = R(math.atan2(dy, dx))
        if d > r - lip:
            up = facing + (nz(x, y) - 0.5) * 0.3
            col = WAX[6] if (up > 0.82 and d > r - lip + 0.8) else WAX[5] if up > 0.35 else WAX[4] if up > -0.15 else WAX[3] if up > -0.6 else WAX[2]
        elif d > r - lip - 1.1:
            col = WAX[1] if facing > 0.15 else (WAX[5] if facing < -0.45 else WAX[2])
        else:
            col = WAX[2] if nz(x * 2, y * 2) > 0.78 else WAX[3]
        S.set(x, y, col)
    outline = {(x + a, y + b) for (x, y) in shape for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - shape
    for p in outline:
        S.set(*p, WAX[0])
    return soft_shadow(pad(S, 0, 0, 2, 3), [(1, 1, 0.5), (1, 2, 0.4), (2, 3, 0.18)])


def hasp(w=68):
    """a dull iron hasp across the plaque: the hinge plate riveted to the left of the frame, three
    knuckles, the strap (lit top edge, worn face, rust gathering low), its end slotted over the staple
    on the right of the frame, an iron peg through the staple"""
    S = Spr(w + 2, 13)
    I = IRON
    # hinge plate
    S.rect(0, 1, 9, 10, I[1])
    S.rect(1, 2, 7, 8, I[4])
    S.hline(1, 7, 2, I[6]); S.vline(1, 2, 9, I[5]); S.hline(1, 7, 9, I[2]); S.vline(7, 2, 9, I[3])
    for (x, y) in ((3, 4), (3, 7)):
        S.grid(x, y, ['hm', 'md'], {'h': I[7], 'm': I[5], 'd': I[2]})
    # knuckles: three short cylinders, lit on the left
    for y0 in (1, 5, 9):
        for y in range(y0, y0 + 3):
            S.set(9, y, I[6]); S.set(10, y, I[4]); S.set(11, y, I[2])
        S.set(9, y0, I[7]); S.hline(9, 11, y0 + 3, I[1])
    for y in range(1, 12):
        S.set(12, y, I[1])
    # the strap
    for x in range(12, w - 9):
        S.set(x, 3, I[1]); S.set(x, 4, I[6]); S.set(x, 5, I[4]); S.set(x, 6, I[4]); S.set(x, 7, I[3])
        S.set(x, 8, I[1])
    rnd = random.Random(4)
    for x in range(14, w - 10):
        if rnd.random() < 0.12:
            S.set(x, 4, I[7])
        if rnd.random() < 0.18:
            S.set(x, 7, RUSTW[rnd.randint(0, 2)])
        if rnd.random() < 0.06:
            S.set(x, 6, RUSTW[1])
    for x in range(20, w - 14, 17):
        S.set(x, 5, I[5]); S.set(x + 1, 6, I[2])
    # the slotted end over the staple
    xe = w - 10
    S.rect(xe, 2, 7, 8, I[1])
    S.rect(xe + 1, 3, 5, 6, I[4])
    S.hline(xe + 1, xe + 5, 3, I[6]); S.hline(xe + 1, xe + 5, 8, I[2])
    S.rect(xe + 2, 4, 3, 3, I[0])
    # the staple's loop through the slot, and the peg through the loop
    S.set(xe + 2, 5, I[6]); S.set(xe + 3, 5, I[7]); S.set(xe + 4, 5, I[5])
    for y in range(0, 12):
        S.set(xe + 6, y, I[6] if y < 6 else I[4]); S.set(xe + 7, y, I[2])
    S.grid(xe + 5, 0, ['khk', 'kmk'], {'k': I[1], 'h': I[7], 'm': I[5]})
    S.set(xe + 6, 12, I[1]); S.set(xe + 7, 12, I[1])
    return soft_shadow(pad(S, 0, 0, 1, 2), [(1, 1, 0.55), (1, 2, 0.3)])


K = '#140b06'     # the icon outline: darker than the plaque's oak so the shape separates from it


def outline(S, c=K):
    m = S.mask()
    out = {(x + a, y + b) for (x, y) in m for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - m
    for (x, y) in out:
        S.set(x, y, c)


def coin_side(S, ox, oy, rx=7.0, ry=3.2, th=3):
    """one gold crown seen from a little above: the struck face an ellipse, the milled edge under it"""
    W, H = int(2 * rx) + 1, int(2 * ry) + 1
    face = {(x, y) for y in range(H) for x in range(W) if ((x - rx) / (rx + 0.45)) ** 2 + ((y - ry) / (ry + 0.45)) ** 2 <= 1}
    body = set(face)
    for d in range(1, th + 1):
        body |= {(x, y + d) for (x, y) in face}
    for (x, y) in body - face:
        S.set(ox + x, oy + y, BRASS[3] if x % 2 else BRASS[2])
        if y == max(yy for (xx, yy) in body if xx == x):
            S.set(ox + x, oy + y, BRASS[1])
    for (x, y) in face:
        v = (x - rx) * 0.38 + (y - ry) * 1.25
        S.set(ox + x, oy + y, BRASS[6] if v < -1.5 else (BRASS[4] if v > 1.7 else BRASS[5]))
    rim = {p for p in face if any((p[0] + a, p[1] + b) not in face for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
    for (x, y) in rim:
        S.set(ox + x, oy + y, BRASS[4] if y >= ry else BRASS[7])
    S.set(ox + round(rx) - 2, oy + 1, SPEC); S.set(ox + round(rx) - 1, oy + 1, BRASS[7])
    S.set(ox + round(rx) + 1, oy + round(ry), BRASS[4]); S.set(ox + round(rx), oy + round(ry), BRASS[4])


def coin_face(S, ox, oy):
    """a crown stood on its edge, face on: the rim lit top-left, the crown struck in the field"""
    rows = ['...kkkkk...',
            '..kHhhhmk..',
            '.kHhmmmmdk.',
            'kHhmhmhmmdk',
            'khmmhhhmmdk',
            'khmmhhhmmdk',
            'khmmmmmmmdk',
            'kmmmmmmmddk',
            '.kmmmmmddk.',
            '..kdddddk..',
            '...kkkkk...']
    S.grid(ox, oy, rows, {'k': K, 'H': SPEC, 'h': BRASS[7], 'm': BRASS[5], 'd': BRASS[3]})


def coins():
    """30 gold: a tall stack of crowns, a short one beside it, one stood on its edge against them"""
    S = Spr(34, 30)
    for i in range(6):
        coin_side(S, 16, 18 - i * 3)
    for i in range(3):
        coin_side(S, 2, 20 - i * 3)
    coin_face(S, 11, 17)
    outline(S)
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.45)])


SHARD_GILDED = ['#2e1f0c', '#5e4318', '#86642a', '#b08a3c', '#d1ab52', '#e8c97e', '#fff0c4']


SHARD_PLAIN = ['#15191c', '#2b3135', '#4a5258', '#6f797e', '#98a2a6', '#c4ccce', '#eef3f3']


def shard_cluster(ramp, W=30, H=34):
    """House shards: a cluster of crystals, each a six-sided prism with a pointed head. The face toward the
    light lit, the front face mid, the far face dark; a bright edge where the lit face turns; a glint"""
    S = Spr(W, H)
    k = ramp[0]

    def prism(bx, by, L, hw, lean, tip):
        pts = {}
        ax, ay = math.sin(lean), -math.cos(lean)
        nx, ny = -ay, ax
        for y in range(H):
            for x in range(W):
                rx, ry = x + 0.5 - bx, y + 0.5 - by
                v = rx * ax + ry * ay
                u = rx * nx + ry * ny
                if v < 0 or v > L:
                    continue
                lim = hw if v < L - tip else hw * (L - v) / tip
                if abs(u) > lim:
                    continue
                if v >= L - tip:
                    f = 'tl' if u < 0 else 'tr'
                else:
                    f = 'l' if u < -hw / 3 else ('m' if u < hw / 3 else 'r')
                pts[(x, y)] = f
        return pts
    shards = [prism(6.5, 31.5, 17, 3.4, -0.45, 5), prism(23.5, 31.5, 15, 3.2, 0.42, 5),
              prism(15.0, 32.5, 29, 4.8, 0.05, 8), prism(10.0, 33.0, 10, 3.0, -0.12, 4)]
    col = {'l': ramp[4], 'm': ramp[3], 'r': ramp[2], 'tl': ramp[5], 'tr': ramp[3]}
    for pts in shards:
        for (x, y), f in pts.items():
            S.set(x, y, col[f])
        m = set(pts)
        for (x, y), f in pts.items():
            if f == 'l' and pts.get((x + 1, y)) == 'm':
                S.set(x, y, ramp[5])
            if f == 'm' and pts.get((x + 1, y)) == 'r' and y % 3:
                S.set(x, y, ramp[4])
            if f == 'tl' and pts.get((x + 1, y)) == 'tr':
                S.set(x, y, ramp[6])
        edge = {p for p in m if any((p[0] + a, p[1] + b) not in m for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
        for p in edge:
            S.set(*p, k)
    top = min(shards[2], key=lambda q: q[1])
    S.set(top[0] - 1, top[1] + 3, ramp[6]); S.set(top[0] - 1, top[1] + 4, ramp[6])
    S.grid(23, 3, ['..s..', '..s..', 'ssSss', '..s..', '..s..'], {'s': ramp[5], 'S': ramp[6]})
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.45)])


def banderole():
    """a title: a ribbon scroll as a motto is lettered on, bowed, its ends folded back under it and cut in
    swallowtails; two lines of lettering in ink"""
    W, H = 38, 20
    S = Spr(W, H)
    a, b = 8, 29

    def top(x):
        return 3 + round(2.2 * math.sin(math.pi * (x - a) / (b - a)))
    # the tails, behind and lower, the swallowtail at each end
    for (x0, x1, side) in ((1, 10, -1), (27, 36, 1)):
        yt = top(a if side < 0 else b) + 4
        for x in range(x0, x1 + 1):
            depth = (x - x0) if side < 0 else (x1 - x)
            for y in range(yt, yt + 7):
                if depth == 0 and yt + 1 <= y <= yt + 5:
                    continue
                if depth == 1 and yt + 2 <= y <= yt + 4:
                    continue
                if depth == 2 and y == yt + 3:
                    continue
                S.set(x, y, VEL[4] if y == yt else (VEL[2] if y == yt + 6 else VEL[3]))
    # the band
    for x in range(a, b + 1):
        t = top(x)
        for y in range(t, t + 8):
            S.set(x, y, VEL[6])
        S.set(x, t, VEL[7]); S.set(x, t + 1, VEL[7] if (x - a) < 9 else VEL[6]); S.set(x, t + 6, VEL[5]); S.set(x, t + 7, VEL[4])
    for y in range(top(a), top(a) + 8):
        S.set(a, y, VEL[5])
    for y in range(top(b), top(b) + 8):
        S.set(b, y, VEL[4])
    # the folds: a crease where the band turns under
    for x in (a - 1, b + 1):
        t = top(a if x < 20 else b)
        for y in range(t + 2, t + 9):
            S.set(x, y, INK[3])
    # lettering, two lines, in ink
    rnd = random.Random(7)
    for x in range(a + 3, b - 2):
        t = top(x)
        if rnd.random() < 0.78:
            S.set(x, t + 2, INK[1] if rnd.random() < 0.6 else INK[2])
        if x < b - 6 and rnd.random() < 0.7:
            S.set(x, t + 4, INK[2] if rnd.random() < 0.6 else INK[3])
    outline(S)
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.45)])


# ================================================================ trackart3.py: the XP rail, the dull Claim plate and key, the open page's plaque

def xp_rail(w=368, fill=138, tiers=30):
    """the chapter as one brass rail screwed to the table: a frame lit on its top and left, a broad top band
    engraved with the 30 tier marks (every fifth longer), a channel cut along the middle, a bottom band,
    a slotted screw at each end of each band, and the brass poured into the channel as far as the XP goes,
    polished, its head rounded"""
    H = 20
    S = Spr(w, H)
    rnd = random.Random(3)
    S.rect(0, 0, w, H, BRASS[0])
    S.rect(1, 1, w - 2, H - 2, BRASS[5])
    brushed(S, 2, 2, w - 2, 6, rnd, 1.8)
    brushed(S, 2, 14, w - 2, 18, rnd, 1.8)
    S.hline(1, w - 2, 1, BRASS[7]); S.vline(1, 1, H - 2, BRASS[6])
    S.hline(1, w - 2, H - 2, BRASS[3]); S.vline(w - 2, 1, H - 2, BRASS[3])
    for x in range(2, 14):
        S.set(x, 1, SPEC if x < 8 else BRASS[7])
    c0, c1 = 4, w - 5               # the channel, 360 px: 12 px a tier
    S.hline(c0 - 1, c1 + 1, 6, BRASS[2]); S.vline(c0 - 1, 6, 13, BRASS[2])
    S.hline(c0 - 1, c1 + 1, 13, BRASS[6]); S.vline(c1 + 1, 6, 13, BRASS[6])
    for x in range(c0, c1 + 1):
        S.set(x, 7, OAK[0])
        for y in (8, 9, 10, 11):
            S.set(x, y, OAK[1])
        S.set(x, 12, OAK[2])
    step = (c1 - c0 + 1) / tiers
    for k in range(1, tiers):
        x = c0 + round(k * step) - 1
        long = k % 5 == 0
        for y in ((2, 3, 4, 5) if long else (3, 4, 5)):
            S.set(x, y, BRASS[2])
        for y in ((3, 4, 5) if long else (4, 5)):
            S.set(x + 1, y, BRASS[6])
        S.set(x, 12, OAK[0])
    # the fill
    for x in range(c0, c0 + fill):
        S.set(x, 7, BRASS[6]); S.set(x, 8, SPEC if x % 7 == 3 else BRASS[7]); S.set(x, 9, BRASS[6])
        S.set(x, 10, BRASS[5]); S.set(x, 11, BRASS[4]); S.set(x, 12, BRASS[3])
        if x % 11 == 6:
            S.set(x, 10, BRASS[4])
    if fill > 0:                    # adapted (UI plan Task 7): no head while nothing is poured
        hx = c0 + fill - 1
        for y, c in ((7, BRASS[4]), (8, BRASS[6]), (9, BRASS[5]), (10, BRASS[4]), (11, BRASS[3]), (12, BRASS[2])):
            S.set(hx, y, c)
        for y in (8, 9, 10, 11):
            S.set(hx + 1, y, BRASS[2])
    # the screws at the ends of each band
    for (x, y, sl) in ((1, 1, '/'), (w - 6, 1, '-'), (1, H - 6, '|'), (w - 6, H - 6, 'x')):
        screw(S, x, y, sl)
    for (dx, dy) in ((0, 0), (w - 1, 0), (0, H - 1), (w - 1, H - 1)):
        S.set(dx, dy, None)
    return soft_shadow(pad(S, 0, 0, 2, 3), [(1, 1, 0.55), (1, 2, 0.4), (2, 3, 0.16)])


DARK = ['#100e0b', '#1b1813', '#26221b', '#302b22', '#3a342a', '#443d31', '#5a5141', '#6f6552']


def dull_plate():
    """the Claim plate when there is nothing to claim: the same plate gone dark with tarnish, its shine gone"""
    s = brass_button(240, 40)
    table = {rgba(a)[:3]: rgba(b)[:3] for a, b in zip(BRASS + [SPEC], DARK + [DARK[7]])}
    for k, c in zip(TARN, ('#2a261d', '#353024', '#3d3a2c')):
        table[rgba(k)[:3]] = rgba(c)[:3]
    for y in range(s.h):
        for x in range(s.w):
            p = s.px[x, y]
            if p[3] and p[:3] in table:
                s.px[x, y] = table[p[:3]] + (p[3],)
    return s


def dull_key(w=37):
    s = key_engraved(w)
    table = {rgba(a)[:3]: rgba(b)[:3] for a, b in zip(BRASS, DARK)}
    for y in range(s.h):
        for x in range(s.w):
            p = s.px[x, y]
            if p[3] and p[:3] in table:
                s.px[x, y] = table[p[:3]] + (p[3],)
    return s


def plaque_here(w=112):
    """the rail's Chapter plaque while the Chapter page is open: the brass polished a step brighter and the
    oak panel lit warm, as if drawn toward the lamp"""
    s = plaque(w)
    up = {rgba(OAK[i])[:3]: rgba(OAK[min(8, i + 3)])[:3] for i in range(9)}
    br = {rgba(BRASS[i])[:3]: rgba(BRASS[i + 1] if i < 7 else SPEC)[:3] for i in range(8)}
    for y in range(s.h):
        for x in range(s.w):
            p = s.px[x, y]
            if not p[3]:
                continue
            if 4 <= x < w - 4 and 4 <= y < 24 and p[:3] in up:
                s.px[x, y] = up[p[:3]] + (p[3],)
            elif p[:3] in br and p[3] == 255:
                s.px[x, y] = br[p[:3]] + (p[3],)
    return s


# ================================================================ trackart4.py: the wax pin and the Oath's House rewards

GOLD = HOUSES['gilded'][0] + ['#f6dfa0']      # the Gilded Order's gold, dark to light, and its glint


def pin_plain(r=6, seed=7):
    """a drop of red wax pressed onto the rod over the current tier: domed, lit on the top left with a
    glint, its point run down toward the plaques below; no number (the chapter's seal carries it)"""
    shape, c, R, size = _blob(r, seed, ((1.5708, 3, 2),))
    S = Spr(size, size)
    L = (-0.55, -0.62, 0.56)
    for (x, y) in shape:
        dx, dy = (x - c) / (r + 0.5), (y - c) / (r + 0.5)
        d2 = dx * dx + dy * dy
        z = math.sqrt(max(0.0, 1.0 - min(1.0, d2)))
        n = (dx, dy, z)
        v = n[0] * L[0] + n[1] * L[1] + n[2] * L[2]
        col = WAX[6] if v > 0.86 else WAX[5] if v > 0.62 else WAX[4] if v > 0.3 else WAX[3] if v > 0.0 else WAX[2]
        if d2 > 1.0:                                    # the run below the dome
            col = WAX[3] if (x - c) < 0 else WAX[2]
        S.set(x, y, col)
    S.set(round(c - r * 0.45), round(c - r * 0.5), SPEC)
    S.set(round(c - r * 0.45) + 1, round(c - r * 0.5), WAX[6])
    out = {(x + a, y + b) for (x, y) in shape for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - shape
    for p in out:
        S.set(*p, WAX[0])
    return soft_shadow(pad(S, 0, 0, 2, 3), [(1, 1, 0.5), (1, 2, 0.4), (2, 3, 0.18)])


SWORD = ['.x.x.x.',          # the Gilded Order's crowned sword, cut down for an icon
         '.xxxxx.',
         '...x...',
         'xxxxxxx',
         '...x...',
         '...x...',
         '...x...',
         '...x...',
         '...x...',
         '...x...']


def device(S, ox, oy, lit, mid, dark):
    """the crowned sword stitched in: each stitch lit on its upper left, the shadow it throws on the cloth"""
    m = {(ox + i, oy + j) for j, row in enumerate(SWORD) for i, ch in enumerate(row) if ch == 'x'}
    for (x, y) in m:
        if (x + 1, y + 1) not in m:
            S.set(x + 1, y + 1, dark)
    for (x, y) in m:
        S.set(x, y, lit if ((x - 1, y) not in m or (x, y - 1) not in m) else mid)


def house_frame():
    """the House's portrait frame for the player's own portrait: gilt, beaded, a rosette at each corner and
    the Order's crown cast on top, the player's face in the window (not a troop's: the mastery frames are
    brass and per troop)"""
    W, H, N, top = 32, 38, 30, 7
    S = Spr(W, H)
    ox, oy = 1, top
    rings = {0: (GOLD[0], GOLD[0]), 1: (GOLD[5], GOLD[2]), 2: (GOLD[4], GOLD[3]), 3: (GOLD[1], GOLD[4])}
    for y in range(N):
        for x in range(N):
            d = min(x, y, N - 1 - x, N - 1 - y)
            if d in rings:
                tl = (y == d and x < N - 1 - d) or (x == d and y < N - 1 - d)
                S.set(ox + x, oy + y, rings[d][0 if tl else 1])
    for k in range(5, N - 5, 3):                        # the beading on the frame's face
        S.set(ox + k, oy + 2, GOLD[6]); S.set(ox + k, oy + N - 3, GOLD[5])
        S.set(ox + 2, oy + k, GOLD[6]); S.set(ox + N - 3, oy + k, GOLD[5])
    tones = ['#141a1d', '#182024', '#1d262a', '#222d31', '#283439']
    for y in range(4, N - 4):
        for x in range(4, N - 4):
            dd = math.hypot((x - 14.5) / 12, (y - 10) / 13)
            S.set(ox + x, oy + y, tones[max(0, min(4, int((1.1 - dd) * 4.0)))])
    S.blit(mini_bust('kort', 22), ox + 4, oy + 4)
    ros = ['.kk.', 'khmk', 'kmdk', '.kk.']
    for (x, y) in ((0, 0), (N - 4, 0), (0, N - 4), (N - 4, N - 4)):
        S.grid(ox + x, oy + y, ros, {'k': GOLD[0], 'h': GOLD[6], 'm': GOLD[4], 'd': GOLD[2]})
    crown = ['.x...x...x.',
             '.xx..x..xx.',
             '.xxxxxxxxx.',
             'xxxxxxxxxxx',
             '..xxxxxxx..',
             '...xxxxx...',
             '....xxx....']
    cx0 = ox + N // 2 - 5
    m = {(cx0 + i, j + 1) for j, row in enumerate(crown) for i, ch in enumerate(row) if ch == 'x'}
    for (x, y) in m:
        up, lf = (x, y - 1) not in m, (x - 1, y) not in m
        S.set(x, y, GOLD[6] if (up and lf) else GOLD[5] if up else GOLD[2] if (x + 1, y) not in m else GOLD[4])
    S.set(cx0 + 5, 1, SPEC)
    outline(S)
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.5)])


def tabard():
    """the commander's tabard in the Order's colours: ivory wool with a gilt border, short flaps over the
    shoulders, the crowned sword on the chest, a belt at the waist; lit from the top left, folds falling"""
    W, H = 30, 34
    S = Spr(W, H)
    inside = set()
    for y in range(1, 33):
        for x in range(W):
            if y < 9:                                   # the shoulder flaps, rounded at their ends
                ok = 2 <= x <= 27 and not (y == 1 and (x < 4 or x > 25))
                if y >= 7 and (x < 5 or x > 24):
                    ok = False
            else:
                f = (y - 9) / 23.0
                ok = round(6 - 1.5 * f) <= x <= round(23 + 1.5 * f)
            if ok:
                inside.add((x, y))
    for (x, y) in inside:
        t = x / (W - 1)
        c = VEL[7] if t < 0.3 else VEL[6] if t < 0.64 else VEL[5]
        if y > 21 and x in (10, 19):                    # the skirt's folds under the belt
            c = VEL[5] if x == 10 else VEL[4]
        S.set(x, y, c)
    for (x, y) in inside:                               # the gilt border round every edge
        nb = [(x + a, y + b) not in inside for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))]
        if any(nb):
            S.set(x, y, GOLD[5] if (nb[1] or nb[3]) else GOLD[3])
    for x in range(12, 18):                             # the neck, open to the dark inside
        S.set(x, 1, '#2a1d12')
        if 13 <= x <= 16:
            S.set(x, 2, '#2a1d12')
    device(S, 11, 8, GOLD[4], GOLD[3], VEL[3])
    for x in range(6, 24):                              # the belt and its buckle
        if (x, 20) in inside:
            S.set(x, 20, INK[3] if x < 15 else INK[2]); S.set(x, 21, INK[1])
    S.grid(13, 19, ['hmmd', 'm..d', 'hmmd'], {'h': GOLD[6], 'm': GOLD[4], 'd': GOLD[2]})
    outline(S)
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.45)])


def cloak():
    """the commander's cloak in the Order's gold, hanging empty: the hood up and dark inside, a gilt clasp at
    the throat, the wool falling from the shoulders in folds that open toward a wavy hem, the fronts parted
    on the dark inside with an ivory facing on each edge; lit from the top left"""
    W, H = 34, 36
    S = Spr(W, H)
    cx = 16.5

    def hw(y):
        if y < 11:
            return 2.6 + 4.9 * min(1.0, (y - 1) / 5.0) ** 0.6
        sh = min(1.0, (y - 10) / 4.0)
        return 7.5 + 4.3 * (3 * sh * sh - 2 * sh * sh * sh) + 3.6 * max(0, y - 14) / 19.0

    def hem(x):
        return 33 - (1 if math.sin((x - cx) * 0.78 + 0.4) > 0.35 else 0)

    body = {(x, y) for y in range(1, 34) for x in range(W) if abs(x + 0.5 - cx - 0.5) <= hw(y) and y <= hem(x)}
    WOOL = [GOLD[1], GOLD[2], GOLD[3], GOLD[4], GOLD[5]]
    for (x, y) in body:
        u = x - cx
        a = u / max(1.0, hw(y))
        fold = math.sin(a * 5.2 + 0.5) if y > 11 else 0.0
        v = 2.1 + fold * 0.9 * min(1.0, (y - 10) / 8.0 if y > 10 else 0) - a * 0.8
        S.set(x, y, WOOL[max(0, min(4, int(round(v))))])
    void = set()
    for (x, y) in body:                                 # the hood's dark inside
        if ((x - cx) / 4.3) ** 2 + ((y - 8.3) / 4.6) ** 2 <= 1.0 and y <= 11:
            void.add((x, y))
    for (x, y) in void:
        S.set(x, y, '#1e140c' if y > 5 else '#2a1d12')
    for (x, y) in void:                                 # the hood's gilt rim, brighter on the lit side
        for (a2, b2) in ((-1, 0), (1, 0), (0, -1)):
            q = (x + a2, y + b2)
            if q in body and q not in void:
                S.set(*q, GOLD[5] if q[0] < cx else GOLD[3])
    for (x, y) in body:                                 # the fronts parting below the clasp
        if y < 14:
            continue
        g = 0.5 + (y - 14) * 0.24
        u = x + 0.5 - cx - 0.5
        if abs(u) < g - 1.0:
            S.set(x, y, '#1e140c' if y < 30 else '#2a1d12')
        elif abs(u) < g:
            S.set(x, y, VEL[6] if u < 0 else VEL[4])    # the ivory facing on each front
    for x in range(W):                                  # the gilt hem along the wave
        y = hem(x)
        if (x, y) in body and S.get(x, y) not in (rgba('#1e140c'), rgba('#2a1d12')):
            S.set(x, y, GOLD[5] if x < 18 else GOLD[4])
    S.grid(15, 12, ['.hm.', 'hmmd', 'mmdd', '.dd.'], {'h': GOLD[6], 'm': GOLD[4], 'd': GOLD[2]})
    S.set(16, 12, SPEC)
    outline(S)
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.45)])


def pennant(kind='pale'):
    """a pennant pattern of the House on a short staff: gold cloth cut in a swallowtail, the pattern in
    ivory (pale: a stripe down the middle; bend: a band from the hoist's top to the fly's foot; cross: a
    stripe down and a band across), the cloth waving, lit along its upper edge; an ash staff with a gilt
    finial"""
    W, H = 35, 30
    S = Spr(W, H)
    for y in range(2, H):                               # the staff
        S.set(3, y, BRASS[3] if y > 3 else GOLD[5]); S.set(4, y, VEL[2]); S.set(5, y, VEL[1])
    S.grid(2, 0, ['.h.', 'hmd', '.d.'], {'h': GOLD[6], 'm': GOLD[4], 'd': GOLD[2]})
    cloth = {}
    for x in range(6, 34):
        t = (x - 6) / 27.0
        wave = 1.6 * math.sin(t * 5.2)
        y0 = 4 + t * 4 + wave
        y1 = 17 - t * 4 + wave
        for y in range(H):
            if y0 <= y <= y1:
                if x > 27:                              # the swallowtail
                    mid = (y0 + y1) / 2
                    if abs(y - mid) < (x - 27) * 0.75:
                        continue
                cloth[(x, y)] = (t, (y - y0) / max(1.0, y1 - y0), math.cos(t * 5.2))
    for (x, y), (t, v, slope) in cloth.items():
        ivory = pennant_ivory(kind, x, y, t, v, cloth)  # adapted (UI plan Task 7): the Oath's other patterns
        shade = 1 if slope > 0.3 else (0 if slope > -0.4 else -1)
        if ivory:
            c = [VEL[4], VEL[6], VEL[7]][shade + 1]
        else:
            c = [GOLD[2], GOLD[3], GOLD[4]][shade + 1]
        if v < 0.12:
            c = VEL[8] if ivory else GOLD[5]
        S.set(x, y, c)
    outline(S)
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.45)])


def banner_cloth(kind='ivory'):
    """a banner cloth of the House hung from a gilt crossbar, cut at the foot into three points, the folds
    falling straight. ivory: Order white, the cloth ivory with the crowned sword and border in gold (tier
    15); gold: the cloth of gold with the sword and border in ivory (tier 21, drawn large for the hero)"""
    if kind == 'ivory':
        C, B, D = [VEL[5], VEL[6], VEL[7], VEL[8]], (GOLD[5], GOLD[3]), (GOLD[4], GOLD[3], GOLD[1])
    elif kind == 'night':           # adapted (UI plan Task 7): the night cloth, its sword and border in gold
        C, B, D = NIGHT, (GOLD[5], GOLD[3]), (GOLD[5], GOLD[3], NIGHT[0])
    else:                           # gold, and the crowned field (gold, crowns strewn round the sword)
        C, B, D = [GOLD[2], GOLD[3], GOLD[4], GOLD[5]], (VEL[6], VEL[4]), (VEL[8], VEL[6], GOLD[1])
    W, H = 30, 37
    S = Spr(W, H)
    cloth = set()
    for y in range(5, 36):
        for x in range(4, 26):
            if y < 28:
                cloth.add((x, y))
            else:
                k = (x - 4) / 21.0 * 3.0
                tooth = abs((k % 1.0) - 0.5) * 2.0      # 1 at the notch, 0 at the point
                if (y - 28) <= (1 - tooth) * 7.5:
                    cloth.add((x, y))
    for (x, y) in cloth:
        u = (x - 4) / 21.0
        fold = math.sin(u * 3.14159 * 3)
        c = C[2] if fold > 0.45 else C[1] if fold > -0.35 else C[0]
        if u < 0.12:
            c = C[3] if fold > -0.35 else C[2]
        S.set(x, y, c)
    for (x, y) in cloth:
        edge = [(x + a, y + b) not in cloth for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))]
        inner = [(x + 2 * a, y + 2 * b) not in cloth for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))]
        if not any(edge) and any(inner):
            S.set(x, y, B[0] if (x < 15) else B[1])
    device(S, 12, 10, *D)
    if kind == 'crowns':            # adapted (UI plan Task 7): four small crowns strewn round the sword, inside the border
        for (cx, cy) in ((6, 9), (19, 9), (6, 20), (19, 20)):
            S.grid(cx, cy, ['x.x.x', 'xxxxx'], {'x': VEL[7]})
    if kind == 'night':             # adapted (UI plan Task 7): stars in the dark field
        for (sx, sy) in ((7, 9), (21, 8), (8, 20), (21, 19), (15, 24)):
            S.set(sx, sy, VEL[8])
    for x in range(1, 29):                              # the crossbar, its knobs, the rings
        S.set(x, 3, GOLD[5]); S.set(x, 4, GOLD[3])
    for xk in (0, 28):
        S.grid(xk, 2, ['hm', 'md', 'dd'], {'h': GOLD[6], 'm': GOLD[4], 'd': GOLD[2]})
    for xr in (6, 14, 22):
        S.set(xr, 5, GOLD[1])
    outline(S)
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.45)])


def shards_double(ramp):
    """the Oath doubles the shards: two clusters, the second behind and to the right"""
    a = shard_cluster(ramp)
    S = Spr(a.w + 8, a.h + 2)
    S.blit(a, 8, 0)
    S.blit(a, 0, 2)
    return S


# ================================================================ new for the game (UI plan Task 7)
def mini_bust(name, n=24):
    """a troop chest-up for the House portrait frame (trackart2.py's mini_bust)"""
    return v4a_portrait.portrait(name, n)


def pennant_ivory(kind, x, y, t, v, cloth):
    """whether a pennant's pattern lays ivory on its cloth pixel (x, y): t runs along the fly (0 at the hoist), v down the
    cloth (0 at its top edge). Pale, cross and bend are trackart4.py's, unchanged. The others are the Oath's patterns NOTES.md
    names as proposed: fess, a band across the middle; chief, the top third; bordure, a border round the cloth; saltire, the
    two diagonals; quarterly, the quarter at the hoist's top and the one at the fly's foot; gyronny, eight wedges round the
    middle; pily, three piles pointing down from the top edge"""
    if kind == 'pale':
        return 13 <= x <= 17
    if kind == 'cross':
        return 13 <= x <= 15 or 0.38 <= v <= 0.6
    if kind == 'bend':
        return abs((v * 13) - (t * 13) + 0.0) < 2.2
    if kind == 'fess':
        return 0.36 <= v <= 0.64
    if kind == 'chief':
        return v <= 0.34
    if kind == 'bordure':
        return any((x + a, y + b) not in cloth for a, b in ((2, 0), (-2, 0), (0, 2), (0, -2), (1, 1), (-1, -1), (1, -1), (-1, 1)))
    if kind == 'saltire':
        return abs(v - t) < 0.16 or abs(v - (1 - t)) < 0.16
    if kind == 'quarterly':
        return (t < 0.42) == (v < 0.5)
    if kind == 'gyronny':
        return int((math.atan2((v - 0.5) * 13, x - 17.5) + math.pi) / (math.pi / 4)) % 2 == 0
    if kind == 'pily':
        return any(abs(t - (k + 0.5) / 3) < (1 / 6) * (1 - v / 0.9) for k in range(3))
    raise ValueError('pennant pattern ' + kind)


# the night cloth's field, dark to light: a deep blue that keeps its gold sword and border readable
NIGHT = ['#141a2c', '#1c2439', '#252f48', '#303c58']

# a title on the hero letter: a laurel wreath, pressed in the wax as the Rare's lozenge is
TITLE_SEAL = ['xx.....xx',
              'x.......x',
              'xx.....xx',
              '.x.....x.',
              '.xx...xx.',
              '..xxxxx..']


def seal_title():
    """the kind seal on the hero letter's top edge when the reward is a title (the Rare's is the lozenge, a House item's the
    crowned sword): trackart3.py's seal size, the wreath in relief"""
    return wax_seal(11, TITLE_SEAL, seed=19, drips=((1.3, 2),))


def hero_letter():
    """the hero's letter, landscape (trackart3.py's hero-letter)"""
    return parchment(400, 200, kind='hi', seed=19, shadow=(4, 6), folds=(140,), stains=0, rim=3, mottle='edge',
                     fibres=0.6)[0]


# the XP rail's channel: its first column and its first fill row, in the rail's own pixels
XP_C0, XP_ROW = 4, 7


def xp_fill(w=368):
    """the brass xp_rail pours into its channel, cut out for the game to pour: the six fill rows (rail rows 7 to 12) of each of
    the channel's 360 columns as xp_rail paints them, then the rounded head (one column) and the lip of shadow it leaves in the
    channel past it (rows 8 to 11; rows 7 and 12 clear). For n columns poured the game draws columns 0 to n-2 at the channel's
    start, the head at n-1 and the lip at n: the picture xp_rail(fill=n) paints (xp_check)"""
    c0, c1 = XP_C0, w - 5
    n = c1 - c0 + 1
    S = Spr(n + 2, 6)
    for i in range(n):
        x = c0 + i
        col = (BRASS[6], SPEC if x % 7 == 3 else BRASS[7], BRASS[6], BRASS[4] if x % 11 == 6 else BRASS[5], BRASS[4], BRASS[3])
        for y, c in enumerate(col):
            S.set(i, y, c)
    for y, c in enumerate((BRASS[4], BRASS[6], BRASS[5], BRASS[4], BRASS[3], BRASS[2])):
        S.set(n, y, c)
    for y in (1, 2, 3, 4):
        S.set(n + 1, y, BRASS[2])
    return S


def xp_poured(n, rail=None, fill=None):
    """the empty rail with n columns poured as the game pours them (UiTex.part of xp_fill): see xp_fill"""
    rail = (rail or xp_rail(fill=0)).im.copy()
    fill = (fill or xp_fill()).im
    if n > 0:
        if n > 1:
            rail.alpha_composite(fill.crop((0, 0, n - 1, 6)), (XP_C0, XP_ROW))
        rail.alpha_composite(fill.crop((360, 0, 361, 6)), (XP_C0 + n - 1, XP_ROW))
        rail.alpha_composite(fill.crop((361, 0, 362, 6)), (XP_C0 + n, XP_ROW))
    return rail


def xp_check():
    """xp_fill, once the game's pour is shown to match the approved painter at every length (xp_rail(fill=n) for n 1 to 360)
    and to pour nothing at 0"""
    rail, fill = xp_rail(fill=0), xp_fill()
    for n in range(0, 361):
        want = xp_rail(fill=n).im if n else rail.im
        got = xp_poured(n, rail, fill)
        assert got.tobytes() == want.tobytes(), 'chapter/xp-fill: the pour differs from xp_rail(fill=%d)' % n
    return fill


# the Oath's rewards by icon stem (NOTES.md, "The Oath row"), each painted in the House's colours; Phase 1 shows them in sepia
OATH_ART = {
    'shards': lambda: shards_double(SHARD_GILDED),
    'frame': house_frame,
    'tabard': tabard,
    'cloak': cloak,
    'cloth': lambda: banner_cloth('ivory'),
    'cloth-gold': lambda: banner_cloth('gold'),
    'cloth-crowns': lambda: banner_cloth('crowns'),
    'cloth-night': lambda: banner_cloth('night'),
}
for _p in ('fess', 'chief', 'bordure', 'saltire', 'quarterly', 'pale', 'bend', 'cross', 'gyronny', 'pily'):
    OATH_ART['pennant-' + _p] = (lambda p: (lambda: pennant(p)))(_p)


def sepia_icon(paint):
    """an Oath reward for a player sworn to no House: trackart4.py's sepia print of the icon"""
    return lambda: sepia(paint(), 0.08, 0.92)


# every texture the module exports, by folder/name: the table's own (the open page's plaques, the dull plate and key, and the
# Hollow's mark, which the Contracts page presses too) and the chapter's
TRACK = {
    'table/plaque-112-here': lambda: plaque_here(112),
    'table/plaque-120-here': lambda: plaque_here(120),
    'table/btn-dull-240x40': dull_plate,
    'table/key-dull-37': dull_key,
    'table/stamp-hollow': lambda: hollow_stamp(11, 21),
    'table/stamp-hollow-b': lambda: hollow_stamp(11, 27),
    'table/stamp-hollow-fresh': lambda: hollow_stamp(11, 23, fresh=True),
    'chapter/hero-letter': hero_letter,
    'chapter/seal-title': seal_title,
    'chapter/seal-tier-big': lambda: num_seal(18, 8, drips=((2.2, 3, 2), (0.9, 2, 2)), lip=3.6),
    'chapter/xp-rail': lambda: xp_rail(fill=0),
    'chapter/xp-fill': xp_check,
    'chapter/track-rod': lambda: track_rod(760),
    'chapter/tag-tier': tier_tag,
    'chapter/pin-tier': pin_plain,
    'chapter/arrow-left': lambda: arrow_stud(-1),
    'chapter/arrow-right': lambda: arrow_stud(1),
    'chapter/arrow-left-dim': lambda: arrow_stud(-1, dim=True),
    'chapter/arrow-right-dim': lambda: arrow_stud(1, dim=True),
    'chapter/plaque-tier': lambda: tier_plaque(seed=1),
    'chapter/plaque-tier-b': lambda: tier_plaque(seed=2),
    'chapter/plaque-tier-raised': lambda: tier_plaque(68, 88, seed=3, kind='raised'),
    'chapter/plaque-tier-dull': lambda: tier_plaque(seed=4, kind='dull'),
    'chapter/plaque-tier-dull-b': lambda: tier_plaque(seed=5, kind='dull'),
    'chapter/plaque-tier-sepia': lambda: sepia(tier_plaque(seed=6), 0.04, 0.82),
    'chapter/chain-18': lambda: chain(18),
    'chapter/chain-10': lambda: chain(10),
    'chapter/chain-dull-18': lambda: chain(18, DULL),
    'chapter/chain-dull-10': lambda: chain(10, DULL),
    'chapter/chain-sepia-10': lambda: sepia(chain(10), 0.04, 0.82),
    'chapter/hasp': hasp,
    'chapter/ic-coins': coins,
    'chapter/ic-title': banderole,
    'chapter/ic-title-sepia': sepia_icon(banderole),
}
for _stem, _paint in OATH_ART.items():
    TRACK['chapter/ic-o-%s-sepia' % _stem] = sepia_icon(_paint)
