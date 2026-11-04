"""Forgewall weapons (Ironclad): the Anvilfall, a war hammer, and the Siegewright, a brass-banded crossbow.

The iron takes the rarity ladder (soot-black Forgeheart iron on Legendary, with ember vents); the rarity colour rides on
the hammer's head bands and the crossbow's prod caps. Brass, oak and leather never change. The crossbow has the standard
draw frames: the string comes back to the nut over `pull_0` to `pull_2`, the prod bending a little more each frame, and
the bolt sits on the stock when it is spanned.
"""
from player_armor import paint as P
from player_armor.common import palette
from player_armor.forgewall import SPEC as ARMOUR
from . import wpaint as W
from .wengine import Weapon, WBox, seg, chain

SPEC = dict(ARMOUR)
SPEC['mats'] = dict(ARMOUR['mats'], oak=('#6A4E30', 0.22), string=('#D9D3C1', 0.16))
KEYS = {'plate': 'iron', 'accent': 'steel', 'brass': 'gilt', 'soot': 'iron', 'leather': 'leather', 'copper': 'gilt',
        'oak': 'wood', 'string': 'linen', 'ember': 'glass', 'void': 'iron'}


def pal(rarity):
    return palette(SPEC, rarity, 'crimson')


iron = [P.plate('plate', marks=0.03)]


def riv(c):
    """Brass rivets in the corners of the big faces."""
    if c.sem in ('front', 'back', 'lit', 'dark') and c.w >= 4 and c.h >= 4:
        for x, y in ((1, 1), (c.w - 2, 1), (1, c.h - 2), (c.w - 2, c.h - 2)):
            c.px(x, y, mat='brass', tone=4 if c.sem in ('front', 'lit') else 2)


# ---- the Anvilfall --------------------------------------------------------------------

def head_paint(c):
    for layer in iron:
        layer(c)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        for y in (1, c.h - 2):
            c.hline(y, mat='accent', tone=W.TONE[c.sem])      # the head's bands, in the rarity colour
        if c.legendary and c.sem in ('front', 'back'):
            for x in range(2, c.w - 2, 2):
                c.px(x, c.h // 2, mat='ember', tone=4)        # forge vents
                c.px(x, c.h // 2 - 1, mat='void', tone=0)
        else:
            riv(c)


def face_paint(c):
    for layer in iron:
        layer(c)
    if c.sem == 'dark' or c.face == 'west':
        # the striking face, scored in a cross-hatch (a structure, so it repeats)
        # scored with a cross where it has struck (two short lines, not a hatch)
        cx, cy = c.w // 2, c.h // 2
        for k in range(-2, 2):
            c.px(cx + k, cy + k, d=-1)
            c.px(cx + k, cy - k - 1, d=-1)
        if c.legendary:
            c.rect(c.w // 2 - 1, c.h // 2 - 1, 2, 2, mat='ember', tone=4)
    P.edge('brass', ('top', 'bottom'))(c)


def beak_paint(c):
    W.metal('plate', marks=0)(c)
    if c.sem in ('front', 'back'):
        c.vline(c.w - 1, tone=4)


def haft_paint(c):
    W.wood('oak')(c)
    P.band('brass', [-3, -4])(c)


def hammer(tier, frame):
    leg = tier == 'legendary'
    b = [
        WBox('haft', (-0.75, -8, -0.75), (1.5, 19.5, 1.5), [haft_paint, W.band_rows('leather', list(range(20, 28)))],
             note='oak haft, leather grip band, brass collar'),
        WBox('pommel', (-1, -9, -1), (2, 1, 2), [W.trim('brass')], note='brass pommel cap'),
        WBox('langet', (-1, 10, -1), (2, 2, 2), iron + [riv], note='iron langet collar'),
        WBox('head', (-3.5, 12, -2), (7, 4, 4), [head_paint], note='square head, rarity bands, brass rivets'),
        WBox('face', (-5, 11.5, -2.5), (1.5, 5, 5), [face_paint], note='the striking face, scored, brass rims'),
        WBox('spike', (-0.5, 16, -0.5), (1, 2.5, 1), [W.edge_steel], hide={'down'}, note='top spike'),
    ]
    beak, pts = chain('beak', (3.5, 14), ((4.0, -80, 2.0, 2.0), (2.5, -55, 1.0, 1.0)), [beak_paint], ext=0.25)
    b += beak
    if leg:
        # Forgeheart: two little smokestacks on the head, ember mouths
        for i, x in enumerate((-2.25, 1.25)):
            b.append(WBox('stack_%d' % i, (x, 16, 0.25), (1, 2.5, 1), [W.metal('soot', 0), W.band_rows('brass', [1]),
                     W.glow_tip()], tier='legendary', note='Forgeheart smokestack'))
    return b


# ---- the Siegewright ---------------------------------------------------------------------

PROD = ((3.5, 84, 1.5, 1.5), (3.0, 104, 1.0, 1.0))        # length, angle, w, d: out to the left and curving back
FLEX = {'rest': 0.0, 'pull_0': 3.0, 'pull_1': 6.0, 'pull_2': 9.0}
NUT_Y = 4.0
DRAW = {'pull_0': 9.0, 'pull_1': 6.5, 'pull_2': NUT_Y + 0.5}


def stock_paint(c):
    W.wood('oak')(c)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        for y in (4, 5, 22, 23):
            c.hline(y, mat='brass', tone=W.TONE[c.sem])


def prod_paint(i):
    def run(c):
        W.metal('plate', marks=0)(c)
        if i == 1 and c.sem in ('front', 'back', 'lit', 'dark'):
            c.hline(c.h - 1, mat='accent', tone=W.TONE[c.sem])
            c.hline(c.h - 2, mat='accent', tone=W.TONE[c.sem])
    return [run]


def string_paint(c):
    c.fill('string', 2)


def crossbow(tier, frame):
    leg = tier == 'legendary'
    f = FLEX[frame]
    b = [
        WBox('stock', (-1, -7, -1), (2, 19, 2), [stock_paint], note='oak tiller, brass bands'),
        WBox('butt', (-1.5, -9, -1.25), (3, 2.5, 2.5), [W.wood('oak'), riv], note='the butt'),
        WBox('prod_block', (-1.5, 11, -1.5), (3, 2, 3), iron + [riv], note='iron prod block'),
        WBox('nut', (-0.75, NUT_Y - 0.5, 1), (1.5, 1, 0.5), [W.flat('brass')], hide={'north'}, note='brass nut'),
        WBox('lever', (-0.25, -6, -2), (0.5, 4, 1), [W.flat('plate')], hide={'south'}, note='trigger lever'),
        WBox('stirrup_l', (-1.25, 13, -0.25), (0.5, 2, 0.5), [W.flat('plate')]),
        WBox('stirrup_r', (0.75, 13, -0.25), (0.5, 2, 0.5), [W.flat('plate')]),
        WBox('stirrup_bar', (-1.25, 15, -0.25), (2.5, 0.5, 0.5), [W.flat('plate')], note='the stirrup'),
    ]
    left, lp = chain('prod_l', (-1.25, 12), [(L, a + f, w, d) for L, a, w, d in PROD], prod_paint)
    right, rp = chain('prod_r', (1.25, 12), [(L, -(a + f), w, d) for L, a, w, d in PROD], prod_paint)
    b += left + right
    tl, tr = lp[-1], rp[-1]
    if frame == 'rest':
        span = round((tr[0] - tl[0]) * 2) / 2
        b.append(WBox('string', (tl[0], tl[1] - 0.25, -0.25), (span, 0.5, 0.5), [string_paint]))
    else:
        d = (0.0, DRAW[frame])
        b.append(seg('string_a_%s' % frame, tl, d, 0.5, 0.5, [string_paint], ext=0, frames=(frame,)))
        b.append(seg('string_b_%s' % frame, tr, d, 0.5, 0.5, [string_paint], ext=0, z=0.1, frames=(frame,)))
    if frame == 'pull_2':
        b.append(WBox('bolt', (-0.25, NUT_Y + 0.5, 1.05), (0.5, 8, 0.5), [W.wood('oak', grain=False)], frames=('pull_2',),
                      note='the bolt, laid on the stock when spanned'))
        b.append(WBox('bolt_head', (-0.5, NUT_Y + 8.5, 0.75), (1, 1, 1),
                      [(lambda c: c.fill('ember', 4)) if leg else W.edge_steel], frames=('pull_2',)))
    if leg:
        b.append(WBox('firebox', (-1.25, 6, -2.25), (2.5, 3, 1.5), [W.metal('soot', 0), riv,
                 W.on(('back', 'lit', 'dark'), lambda c: c.hline(c.h // 2, mat='ember', tone=4))],
                 tier='legendary', hide={'south'}, note='Forgeheart firebox under the tiller'))
    return b


def weapons():
    return [
        Weapon('anvilfall', 'Anvilfall', 'Forgewall', 'Ironclad', 'war hammer', pal, hammer, notes=__doc__, keys=KEYS,
               sidecar={'thickness': 3}),
        Weapon('siegewright', 'Siegewright', 'Forgewall', 'Ironclad', 'crossbow', pal, crossbow,
               frames=('rest', 'pull_0', 'pull_1', 'pull_2'), notes=__doc__, keys=KEYS,
               sidecar={'thickness': 3, 'pull': [3.0, 6.0, 9.0], 'string_y': [DRAW['pull_0'], DRAW['pull_1'], DRAW['pull_2']]}),
    ]
