"""The Chapter plaque, part two: the wax (the Hollow's mark that stamps a claimed tier, the tier seals),
the iron hasp over a tier not yet reached, and the reward icons."""
import math, os, random, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
sys.path.insert(0, HERE)
from v4lib import Spr, Noise, dith, rgba, mix, emboss, grid_mask
from v4pal import *
from v4a_furn import screw, engrave, SPEC, TARN, SH
from v4a_paper import parchment, wax_seal, ribbon, CROWN, SHADOW, PAPER
from v4a_recruit import HOUSES, DEVICES
import v4a_portrait
from trackart import DULL, SEP, RUSTW, sepia, soft_shadow, pad

ASSETS = {}


# ================================================================ the Hollow's mark
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


ASSETS['stamp-hollow'] = lambda: hollow_stamp(11, 21)
ASSETS['stamp-hollow-b'] = lambda: hollow_stamp(11, 27)
ASSETS['stamp-hollow-fresh'] = lambda: hollow_stamp(11, 23, fresh=True)


# ================================================================ the tier seal: wax with a flat field for a number
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


ASSETS['seal-tier-big'] = lambda: num_seal(18, 8, drips=((2.2, 3, 2), (0.9, 2, 2)), lip=3.6)


# ================================================================ the iron hasp over a tier not reached
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


ASSETS['hasp'] = hasp


# ================================================================ reward icons (drawn on the plaque's oak)
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


ASSETS['ic-coins'] = coins


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


ASSETS['ic-shards'] = lambda: shard_cluster(SHARD_GILDED)
ASSETS['ic-shards-plain'] = lambda: shard_cluster(SHARD_PLAIN)


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


ASSETS['ic-title'] = banderole


def troop_letter():
    """the chosen Rare: a letter as the recruit table's letters come, sealed in red"""
    from v4a_recruit import envelope
    e, vy = envelope(30, 21, 31, 'hi')
    S = Spr(34, 26)
    S.blit(e, 1, 1)
    S.grid(12, 1 + vy - 4, ['..kkkk..', '.kHhhfk.', 'kHhfffsk', 'khffffsk', 'kfffffsk', '.kfsssk.', '..kkkk..'],
           {'k': WAX[0], 'H': WAX[6], 'h': WAX[5], 'f': WAX[3], 's': WAX[2]})
    return S


ASSETS['ic-letter'] = troop_letter


# ---------------------------------------------------------------- a troop chest-up, for the House portrait frame
def mini_bust(name, n=24):
    return v4a_portrait.portrait(name, n)
