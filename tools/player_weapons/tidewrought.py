"""Tidewrought weapons (Tidecallers): the Undertow, a barbed trident, and the Brinecutter, a shell-guarded cutlass.

The bronze takes the rarity ladder (abyss-dark bronze on Legendary, with an ember pearl and ember points); the rarity
colour rides on the trident's crossbar bands and the cutlass's ricasso collar. Shell, barnacle, kelp and driftwood
never change. Verdigris pools only where water would sit.
"""
from player_armor import paint as P
from player_armor.common import palette
from player_armor.tidewrought import SPEC as ARMOUR
from . import wpaint as W
from .wengine import Weapon, WBox, seg, chain

SPEC = dict(ARMOUR)
SPEC['mats'] = dict(ARMOUR['mats'], driftwood=('#7A6A55', 0.20), leather='#5A3E2C')
KEYS = {'plate': 'steel', 'accent': 'steel', 'patina': 'iron', 'barnacle': 'bone', 'navy': 'linen', 'kelp': 'linen',
        'shell': 'bone', 'driftwood': 'wood', 'leather': 'leather', 'ember': 'glass', 'void': 'iron'}


def pal(rarity):
    return palette(SPEC, rarity, 'crimson')


bronze = [P.plate('plate', marks=0.02), P.patina('patina', 0.04)]


def scallop(c):
    t = 3 if c.sem in ('front', 'lit', 'top') else 2
    c.fill('shell', t)
    if c.sem in ('front', 'back') and c.w >= 3:
        for x in range(c.w):
            if x % 2 == 1:
                c.vline(x, tone=t - 1)
        c.clear(0, 0); c.clear(c.w - 1, 0)
        c.hline(c.h - 1, tone=t - 1)


# ---- the Undertow ---------------------------------------------------------------------

def haft_paint(c):
    W.wood('driftwood')(c)
    if c.sem in ('front', 'lit', 'back', 'dark'):
        # kelp wound round the haft below the head, in two short spirals
        for k in range(6):
            c.px(k % c.w, 8 + k, mat='kelp', tone=2)
            c.px(k % c.w, 20 + k, mat='kelp', tone=1)


def crossbar_paint(c):
    for layer in bronze:
        layer(c)
    P.band('accent', [0, -1])(c)


def tine_paint(c):
    for layer in bronze:
        layer(c)
    if c.sem in ('front', 'back'):
        c.vline(0, tone=4 if c.sem == 'front' else 3)


def point_paint(c):
    W.edge_steel(c)
    if c.legendary and c.sem != 'bottom':
        c.px(0, 0, mat='ember', tone=4)


def pearl_shell(c):
    scallop(c)
    if c.sem == 'front':
        c.rect(c.w // 2 - 1, c.h - 2, 2, 1, mat='ember' if c.legendary else 'barnacle', tone=4)


def kelp_paint(c):
    c.fill('kelp', 2 if c.sem in ('front', 'lit') else 1)
    for y in range(c.h):
        if c.rng.random() < 0.3:
            c.px(0, y, d=1)


def coral_paint(c):
    c.fill('barnacle', 3 if c.sem in ('front', 'lit', 'top') else 2)


def trident(tier, frame):
    leg = tier == 'legendary'
    b = [
        WBox('haft', (-0.75, -12, -0.75), (1.5, 27, 1.5), [haft_paint], note='driftwood haft wound with kelp'),
        WBox('butt', (-1, -13, -1), (2, 1, 2), bronze, hide={'up'}),
        WBox('crossbar', (-3.75, 15, -0.75), (7.5, 1.5, 1.5), [crossbar_paint], note='bronze crossbar, rarity bands'),
        WBox('tine_c', (-0.5, 16.5, -0.5), (1, 7, 1), [tine_paint], hide={'down'}),
        WBox('head_c', (-0.75, 23.5, -0.25), (1.5, 1, 0.5), [point_paint], hide={'down'}, note='the centre barb'),
        WBox('point_c', (-0.25, 24.5, -0.25), (0.5, 1, 0.5), [point_paint], hide={'down'}),
        WBox('tine_l', (-3.5, 16.5, -0.5), (1, 4.5, 1), [tine_paint], hide={'down'}),
        WBox('tine_r', (2.5, 16.5, -0.5), (1, 4.5, 1), [tine_paint], hide={'down'}),
        WBox('point_l', (-3.25, 21, -0.25), (0.5, 1, 0.5), [point_paint], hide={'down'}),
        WBox('point_r', (2.75, 21, -0.25), (0.5, 1, 0.5), [point_paint], hide={'down'}),
        WBox('barb_l', (-4, 19.5, -0.25), (0.5, 1, 0.5), [W.edge_steel], note='outward barb'),
        WBox('barb_r', (3.5, 19.5, -0.25), (0.5, 1, 0.5), [W.edge_steel]),
        WBox('shell', (-1, 14.5, 0.75), (2, 2, 0.5), [pearl_shell], hide={'north'},
             note='scallop on the crossbar (an ember pearl on Legendary)'),
        WBox('kelp', (-3.25, 11.5, -0.25), (0.5, 3.5, 0.5), [kelp_paint], hide={'up'}, note='kelp trailing from the bar'),
    ]
    if leg:
        b += [seg('coral_l', (-3.5, 15.75), (-5.2, 18.0), 0.5, 0.5, [coral_paint, W.glow_tip()], z=0.4, tier='legendary'),
              seg('coral_r', (3.5, 15.75), (5.2, 18.0), 0.5, 0.5, [coral_paint, W.glow_tip()], z=0.4, tier='legendary')]
    return b


# ---- the Brinecutter -----------------------------------------------------------------------

def blade_paint(i):
    def run(c):
        W.metal('plate', marks=0.015)(c)
        if c.sem in ('front', 'back'):
            c.vline(0, tone=4 if c.sem == 'front' else 3)        # the edge (the convex side)
            c.vline(c.w - 1, tone=1)                            # the back
            if i == 0:
                P.patina('patina', 0.04)(c)
        if c.legendary and i == 3 and c.sem == 'front':
            c.vline(0, 0, 2, mat='ember', tone=4)
    return [run]


def guard_paint(c):
    scallop(c)
    if c.sem == 'top':
        for x in range(1, c.w, 2):
            c.vline(x, d=-1)                                    # the shell's ribs, seen from above
    P.barnacles('barnacle', 1)(c)


def cutlass(tier, frame):
    leg = tier == 'legendary'
    b = [
        WBox('grip', (-0.75, -2.5, -0.75), (1.5, 5, 1.5), [W.wrap('leather')], hide={'up', 'down'}),
        WBox('pommel', (-1, -4, -1), (2, 1.5, 2), bronze, note='bronze pommel'),
        WBox('shell_guard', (-2, 2.5, -2), (4, 1.5, 4), [guard_paint], note='scallop-shell cup over the hand'),
        WBox('ricasso', (-1, 4, -0.75), (2, 1, 1.5), [W.flat('accent')], note='ricasso collar, rarity colour'),
    ]
    blade, _ = chain('blade', (0.1, 5.0), ((5.5, -6, 2.0, 0.5), (4.5, -12, 2.5, 0.5), (4.5, -22, 2.5, 0.5),
                                           (2.0, -32, 1.5, 0.5)), blade_paint)
    b += blade
    bow, _ = chain('knuckle', (-1.75, 2.5), ((3.5, 172, 0.5, 0.5), (3.0, 205, 0.5, 0.5)),
                   [W.flat('plate')], z=-0.5)
    b += bow
    if leg:
        b += [WBox('coral_a', (1.5, 4, -0.5), (0.5, 1.5, 0.5), [coral_paint, W.glow_tip()], tier='legendary'),
              WBox('coral_b', (-2, 4, 0.5), (0.5, 1, 0.5), [coral_paint, W.glow_tip()], tier='legendary'),
              WBox('pearl', (-0.5, -3.75, 1), (1, 1, 0.5), [lambda c: c.fill('ember', 4)], tier='legendary',
                   hide={'north'}, note='ember pearl set in the pommel')]
    return b


def weapons():
    return [
        Weapon('undertow', 'Undertow', 'Tidewrought', 'Tidecallers', 'trident', pal, trident, notes=__doc__, keys=KEYS,
               sidecar={'thickness': 2}),
        Weapon('brinecutter', 'Brinecutter', 'Tidewrought', 'Tidecallers', 'cutlass', pal, cutlass, notes=__doc__,
               keys=KEYS, sidecar={'thickness': 1, 'length_px': 24.5}),
    ]
