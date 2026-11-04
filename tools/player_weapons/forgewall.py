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

# The prod (the crossbow's bow): three sections a side from the bridle, out and slightly forward, then recurving back
# to the brass tip caps, so the string at rest stands well behind the prod's middle and the prod and string read as one
# closed bow shape. length, angle (degrees from +y, positive toward -x), width, depth.
PROD = ((3.5, 55, 2.0, 1.5), (3.5, 100, 1.5, 1.0), (2.5, 148, 1.0, 1.0))
PROD_Y = 12.25
FLEX = {'rest': 0.0, 'pull_0': 3.0, 'pull_1': 6.0, 'pull_2': 9.0}
NUT_Y = 5.5
DRAW = {'pull_0': 9.5, 'pull_1': 7.5, 'pull_2': NUT_Y}


def tiller_paint(c):
    W.wood('oak')(c)
    if c.sem == 'front':
        # the bolt groove down the tiller's top, from the nut to the prod; the brass nut across it
        # rows count down from the tiller's top (y 12), two a px: the nut sits at y NUT_Y
        g = c.w // 2
        ny = int(round(2 * (12 - NUT_Y)))
        for y in range(0, ny):
            c.px(g - 1, y, tone=1); c.px(g, y, tone=1)
        c.rect(0, ny, c.w, 2, mat='brass', tone=3)
        c.px(0, ny, mat='brass', tone=4)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        c.hline(c.h - 3, mat='brass', tone=W.TONE[c.sem])    # a brass band where the tiller meets the stock


def stock_paint(c):
    W.wood('oak')(c)
    if c.sem in ('lit', 'dark'):
        # the lock plate on each side of the stock, brass, riveted
        c.rect(1, 2, c.w - 2, 4, mat='brass', tone=W.TONE[c.sem])
        c.px(1, 2, mat='brass', tone=4)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        c.hline(c.h - 1, mat='brass', tone=max(0, W.TONE[c.sem] - 1))   # the butt plate
        c.hline(c.h - 2, mat='brass', tone=W.TONE[c.sem])


def bridle_paint(c):
    for layer in iron:
        layer(c)
    if c.sem in ('front', 'back'):
        for x in (1, 2, c.w - 3, c.w - 2):
            c.vline(x, mat='leather', tone=2 if c.sem == 'front' else 1)   # the cord binding the prod on


def prod_paint(i):
    def run(c):
        W.metal('plate', marks=0)(c)
        if c.sem in ('front', 'back'):
            c.vline(0, tone=4 if c.sem == 'front' else 3)
    return [run]


def cap_paint(c):
    W.trim('brass')(c)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        c.hline(c.h - 1, mat='accent', tone=W.TONE[c.sem])     # the rarity colour on the tip caps
        c.hline(c.h - 2, mat='accent', tone=W.TONE[c.sem])


def string_paint(c):
    c.fill('string', 3)      # a heavy cord, a tone brighter than a bowstring so the bow shape reads at a glance


def bolt_paint(c):
    c.fill('string', max(0, W.TONE[c.sem] - 1))        # a pale ash shaft, so it shows against the oak tiller
    if c.sem in ('top', 'bottom'):
        if c.sem == 'top':
            c.fill('ember' if c.legendary else 'plate', 4 if c.legendary else 3)
        return
    head = 'ember' if c.legendary else 'plate'
    for y in range(0, 3):                                   # the head (top three rows), a point at the top
        c.hline(y, mat=head, tone=(4 if c.legendary else W.TONE[c.sem]))
    c.px(0, 0, mat=head, tone=4 if c.legendary else 4)
    for y in range(c.h - 4, c.h):                           # fletching at the nut end
        c.hline(y, mat='string', tone=3 if y % 2 else 2)


def crossbow(tier, frame):
    leg = tier == 'legendary'
    f = FLEX[frame]
    b = [
        WBox('tiller', (-1.25, -1.5, -1), (2.5, 13.5, 2), [tiller_paint], note='oak tiller: bolt groove, brass nut'),
        WBox('stock', (-2, -7, -1.5), (4, 6, 3), [stock_paint], note='the broad stock, brass lock plates and butt plate'),
        WBox('lever', (-0.25, -6.5, -2), (0.5, 6, 1), [W.flat('plate')], hide={'south'},
             note='the long trigger lever under the stock'),
        WBox('bridle', (-2, 11, -1.5), (4, 2.5, 3), [bridle_paint], note='iron bridle, the prod corded on'),
        WBox('stirrup_l', (-2, 13.5, -0.25), (0.5, 2.5, 0.5), [W.flat('plate')]),
        WBox('stirrup_r', (1.5, 13.5, -0.25), (0.5, 2.5, 0.5), [W.flat('plate')]),
        WBox('stirrup_bar', (-2, 16, -0.25), (4, 0.5, 0.5), [W.flat('plate')], note='the open stirrup loop'),
    ]
    left, lp = chain('prod_l', (-1.75, PROD_Y), [(L, a + f, w, d) for L, a, w, d in PROD], prod_paint)
    right, rp = chain('prod_r', (1.75, PROD_Y), [(L, -(a + f), w, d) for L, a, w, d in PROD], prod_paint)
    b += left + right
    tl, tr = lp[-1], rp[-1]
    b += [WBox('cap_l', (tl[0] - 0.5, tl[1] - 0.5, -0.75), (1, 1, 1.5), [cap_paint], note='brass tip cap'),
          WBox('cap_r', (tr[0] - 0.5, tr[1] - 0.5, -0.75), (1, 1, 1.5), [cap_paint])]
    if frame == 'rest':
        span = round((tr[0] - tl[0]) * 2) / 2
        b.append(WBox('string', (tl[0], tl[1] - 0.25, -0.25), (span, 0.5, 0.5), [string_paint],
                      note='the string, standing behind the prod'))
    else:
        d = (0.0, DRAW[frame])
        b.append(seg('string_a_%s' % frame, tl, d, 0.5, 0.5, [string_paint], ext=0, frames=(frame,)))
        b.append(seg('string_b_%s' % frame, tr, d, 0.5, 0.5, [string_paint], ext=0, z=0.1, frames=(frame,)))
    if frame == 'pull_2':
        b.append(WBox('bolt', (-0.5, NUT_Y, 1.05), (1, 11, 0.5), [bolt_paint], frames=('pull_2',),
                      note='the bolt in the groove when spanned: fletched at the nut, a steel head past the bridle'))
    else:
        b.append(WBox('bolt_clipped', (1.25, -0.5, -0.25), (1, 11, 0.5), [bolt_paint], frames=(frame,),
                      note='the same bolt clipped along the tiller until the crossbow is spanned'))
    if leg:
        b.append(WBox('firebox', (-1.25, 5, -2.5), (2.5, 3, 1.5), [W.metal('soot', 0), riv,
                 W.on(('back', 'lit', 'dark'), lambda c: c.hline(c.h // 2, mat='ember', tone=4))],
                 tier='legendary', hide={'south'}, note='Forgeheart firebox under the tiller'))
    return b


def weapons():
    return [
        Weapon('anvilfall', 'Anvilfall', 'Forgewall', 'Ironclad', 'war hammer', pal, hammer, notes=__doc__, keys=KEYS,
               sidecar={'thickness': 3}),
        Weapon('siegewright', 'Siegewright', 'Forgewall', 'Ironclad', 'crossbow', pal, crossbow,
               frames=('rest', 'pull_0', 'pull_1', 'pull_2'), notes=__doc__, keys=KEYS,
               sidecar={'thickness': 3, 'pull': [3.0, 6.0, 9.0], 'string_y': [DRAW['pull_0'], DRAW['pull_1'], DRAW['pull_2']],
                        'nut_y': NUT_Y}),
    ]
