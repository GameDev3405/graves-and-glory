"""What an archer carries besides the bow: the arrow in the draw hand and a hip quiver, plus a bow that draws
continuously for the preview.

All are Thornhide pieces, painted with the set's weapon palette at 2 texels a px (the weapon standard). They are not in
the weapon set's `weapons()`: the arrow and quiver are animation props, not items.

Arrow space follows the weapon convention: the nock at the origin, the shaft up +y to the head. Quiver space: the base at
the origin, the tube up +y, the arrows' fletchings showing at the mouth.

The Antlerbow has four real frames (rest and pull_0 to pull_2: the string at x -3, -4, -5, -6). The game swaps them as a
vanilla bow does. For the preview `bow_at(draw)` builds the bow at any string position between, with the same generator,
so the string follows the hand smoothly. `frame_for(draw)` says which real frame the game shows at that draw.
"""
import numpy as np

from player_armor.engine import FACES, SHADE, Canvas
from player_armor import paint as P
from player_weapons import thornhide as TH
from player_weapons import wpaint as W
from player_weapons.wengine import Weapon, WBox

BRACE = -3.0           # the string's x at rest
FULL = -6.0            # at full draw (pull_2)


# ---- the arrow --------------------------------------------------------------------------------

def _shaft(c):
    W.wood('wood', flecks=0)(c)


def _fletch(c):
    c.fill('feather', W.TONE[c.sem])
    if c.sem in ('front', 'back', 'lit', 'dark'):
        c.hline(0, mat='accent', tone=W.TONE[c.sem])          # a rawhide binding at the vane's head


def _head(c):
    W.edge_steel(c)


def arrow_boxes(tier, frame='rest'):
    return [
        WBox('nock', (-0.25, 0.0, -0.25), (0.5, 0.5, 0.5), [lambda c: P.bone('bone', crack=False)(c)], note='bone nock'),
        WBox('shaft', (-0.25, 0.5, -0.25), (0.5, 9.0, 0.5), [_shaft], note='ash shaft'),
        WBox('vane_l', (-1.25, 1.0, -0.25), (1.0, 2.5, 0.5), [_fletch], note='fletching'),
        WBox('vane_r', (0.25, 1.0, -0.25), (1.0, 2.5, 0.5), [_fletch]),
        WBox('vane_c', (-0.25, 1.0, 0.25), (0.5, 2.5, 1.0), [_fletch], note='cock feather'),
        WBox('head', (-0.5, 9.5, -0.25), (1.0, 1.0, 0.5), [_head], note='bodkin head'),
        WBox('tip', (-0.25, 10.5, -0.25), (0.5, 0.5, 0.5), [_head]),
    ]


ARROW = Weapon('arrow', 'Arrow', 'Thornhide', 'Wildmoot', 'arrow', TH.pal, arrow_boxes, notes=__doc__, keys=TH.KEYS)
ARROW_LENGTH = 11.0


# ---- the hip quiver ------------------------------------------------------------------------------

def _tube(c):
    W.wrap('leather')(c)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        for y in (3, 10):
            c.hline(y, mat='accent', tone=W.TONE[c.sem])          # rawhide lacing bands


def quiver_boxes(tier, frame='rest'):
    b = [
        WBox('base', (-1.5, -0.5, -1.5), (3.0, 1.0, 3.0), [lambda c: P.bone('bone', crack=False)(c)],
             note='bone foot'),
        WBox('tube', (-1.25, 0.5, -1.25), (2.5, 7.5, 2.5), [_tube], hide=('up', 'down'), note='laced hide tube'),
        WBox('mouth', (-1.5, 8.0, -1.5), (3.0, 1.0, 3.0), [lambda c: c.fill('fur', W.TONE[c.sem])],
             hide=('down',), note='fur-trimmed mouth'),
    ]
    for i, (x, z, h) in enumerate(((-0.75, -0.5, 2.0), (0.5, -0.25, 2.5), (-0.25, 0.75, 1.5), (0.75, 0.75, 2.0))):
        b.append(WBox('shaft_%d' % i, (x - 0.25, 9.0, z - 0.25), (0.5, h, 0.5), [_shaft]))
        b.append(WBox('fletch_%d' % i, (x - 0.5, 9.0 + h - 1.5, z - 0.25), (1.0, 1.5, 0.5), [_fletch]))
    return b


QUIVER = Weapon('hip_quiver', 'Hip quiver', 'Thornhide', 'Wildmoot', 'quiver', TH.pal, quiver_boxes, notes=__doc__,
                keys=TH.KEYS)


# ---- the bow at any draw -------------------------------------------------------------------------

def frame_for(draw):
    """The real Antlerbow frame the game shows at a string position (the nearest frame not past it)."""
    if draw > -3.5:
        return 'rest'
    if draw > -4.5:
        return 'pull_0'
    if draw > -5.5:
        return 'pull_1'
    return 'pull_2'


def bow_boxes(draw, tier='base'):
    """The Antlerbow's limbs and string with the string at x = draw (between about -2.4, a string snapping past brace,
    and -6, full draw), from the bow's own generator. Its nocked arrow is left out: the archer's arrow is in the hand."""
    draw = round(float(draw) * 20) / 20
    name = 'd%.2f' % draw
    flex = max(0.0, -draw - 3.0)                  # pull_0..2 are flex 1..3 at x -4..-6
    TH.FLEX[name] = flex
    TH.DRAW[name] = min(draw, -2.0)
    boxes = TH.bow(tier, name if draw < BRACE - 0.05 or draw > BRACE + 0.05 else 'rest')
    return [b for b in boxes if not b.name.startswith('arrow')]


def paint_faces(boxes, rarity='common'):
    """Each box's faces painted on their own (no atlas): [(box, {face: (texture, glow, rect, shade)})]."""
    tier = 'legendary' if rarity == 'legendary' else 'base'
    pal = TH.pal(rarity)
    out = []
    for b in boxes:
        faces = {}
        for f in FACES:
            if f in b.hide:
                continue
            w, h = b.face_size(f)
            c = Canvas(w, h, f, b, tier)
            for layer in b.paint:
                layer(c)
            t = np.zeros((h, w, 4), np.uint8)
            g = np.zeros((h, w), bool)
            for y in range(h):
                for x in range(w):
                    m = c.mat[y, x]
                    if m is not None:
                        t[y, x] = (*[int(round(v)) for v in pal.tones[m][int(c.tone[y, x])]], 255)
                        g[y, x] = m in pal.glow
            faces[f] = (t, g, (0, 0, w, h), SHADE[f])
        out.append((b, faces))
    return out


_BOW_CACHE = {}


def bow_drawn(draw, rarity='common'):
    key = (round(float(draw) * 20) / 20, rarity)
    if key not in _BOW_CACHE:
        _BOW_CACHE[key] = paint_faces(bow_boxes(key[0]), rarity)
    return _BOW_CACHE[key]
