"""Barrow Warden weapons (Graveborn): the Gravereaper, a war scythe, and the Wakelight, a lantern flail.

The iron takes the set's rarity ladder (tarnished, blued, violet-black, grave-black on Legendary); the rarity colour rides
on the haft collars and the chain's last link. Bone, black wood, rust and leather never change. Chained parts (the
scythe's blade, the flail's chain) alternate their depth by 0.05 px so no two neighbours share a plane.
"""
from player_armor import paint as P
from player_armor.common import palette
from player_armor.barrow_warden import SPEC as ARMOUR
from . import wpaint as W
from .wengine import Weapon, WBox, seg

SPEC = dict(ARMOUR)
SPEC['mats'] = dict(ARMOUR['mats'], blackwood=('#3E2C24', 0.22))
KEYS = {'plate': 'iron', 'accent': 'iron', 'bone': 'bone', 'rust': 'iron', 'moss': 'linen', 'leather': 'leather',
        'blackwood': 'wood', 'ember': 'glass', 'void': 'iron'}


def pal(rarity):
    return palette(SPEC, rarity, 'crimson')


iron = [P.plate('plate', marks=0.03), P.rust('rust', 0.05)]


def blade_paint(c):
    """A scythe blade section: the edge (the low side of the section) bright, the spine dark, rust on the spine."""
    W.metal('plate', marks=0.02)(c)
    if c.sem in ('front', 'back'):
        c.vline(c.w - 1, tone=4 if c.sem == 'front' else 3)     # the cutting edge
        c.vline(0, tone=1)                                      # the spine
        P.rust('rust', 0.06)(c)


def skull_paint(c):
    P.bone('bone', crack=False)(c)
    if c.sem == 'front':
        eye = 'ember' if c.legendary else 'void'
        c.px(0, 1, mat=eye, tone=4 if c.legendary else 0); c.px(3, 1, mat=eye, tone=4 if c.legendary else 0)
        c.px(1, 1, mat=eye, tone=3 if c.legendary else 0); c.px(2, 1, mat=eye, tone=3 if c.legendary else 0)
        c.px(1, 2, mat='void', tone=1)
        for x in range(4):
            c.px(x, 3, tone=4 if x % 2 == 0 else 1)


def haft_paint(c):
    W.wood('blackwood', flecks=1)(c)
    P.band('accent', [1, 2])(c)


def scythe(tier, frame):
    leg = tier == 'legendary'
    pts = [(0.4, 17.6), (-3.6, 18.2), (-7.6, 17.4), (-10.6, 15.6), (-12.4, 13.0)]
    widths = (2.0, 2.0, 1.5, 1.0)
    b = [
        WBox('haft', (-0.75, -12, -0.75), (1.5, 27, 1.5), [haft_paint], note='black-wood snath, collars in the rarity colour'),
        WBox('socket', (-1, 15, -1), (2, 2.5, 2), iron, note='iron socket'),
        WBox('nib', (0.75, 6, -0.5), (2, 1, 1), [W.wood('blackwood', grain=False)], hide={'west'},
             note='the snath\'s side handle'),
        WBox('butt', (-0.5, -13.5, -0.5), (1, 1.5, 1), iron, hide={'up'}, note='iron butt spike'),
        WBox('skull', (-1, 17.5, -1), (2, 2, 2), [skull_paint], note='a bone skull at the blade root'),
    ]
    for i in range(4):
        b.append(seg('blade_%d' % i, pts[i], pts[i + 1], widths[i], 0.5, [blade_paint], z=0.05 * (i % 2),
                     note='blade section %d' % i))
    if leg:
        # the Lich-warden's reaper: bone spines along the blade's back, an iron crown-collar on the snath
        for i, (p, q) in enumerate((((-3.0, 18.9), (-3.4, 20.6)), ((-6.6, 18.6), (-7.2, 20.0)), ((-9.8, 17.0), (-10.8, 18.2)))):
            b.append(seg('spine_%d' % i, p, q, 0.5, 0.5, [P.bone('bone', crack=False), W.glow_tip()], z=0.3,
                         tier='legendary'))
        b.append(WBox('crown', (-1.25, 13, -1.25), (2.5, 1, 2.5), [W.metal('plate', 0), W.on(('front', 'lit', 'back', 'dark'),
                 lambda c: [c.px(x, 0, mat='ember', tone=3) for x in (1, 3) if x < c.w])], tier='legendary',
                 note='broken iron crown ringing the snath'))
    return b


# ---- the Wakelight ------------------------------------------------------------------------

def cage_paint(c):
    """The lantern: iron bars over a dark inside, a bone candle at the bottom; lit with ember on Legendary."""
    if c.sem in ('top', 'bottom'):
        W.flat('plate')(c)
        return
    c.fill('plate', W.TONE[c.sem])
    inside = 'ember' if c.legendary else 'void'
    for x in range(c.w):
        if x % 2 == 1:
            for y in range(1, c.h - 1):
                c.px(x, y, mat=inside, tone=(3 if y < c.h - 3 else 4) if c.legendary else 0)
    for x in range(c.w):
        if x % 2 == 1:
            c.px(x, c.h - 2, mat='bone', tone=3)                # the candle
    c.hline(0, d=1)


def link_paint(c):
    W.metal('plate', marks=0)(c)
    P.rust('rust', 0.1)(c)


def last_link(c):
    c.fill('accent', W.TONE[c.sem])


def spike_paint(c):
    c.fill('bone', W.TONE[c.sem])
    c.px(0, 0, d=1)


def flail(tier, frame):
    leg = tier == 'legendary'
    b = [
        WBox('haft', (-0.75, -4, -0.75), (1.5, 11, 1.5), [W.wood('blackwood'), W.band_rows('leather', [8, 9, 10, 11])],
             note='black-wood haft, a leather band above the fist'),
        WBox('pommel', (-1, -5, -1), (2, 1, 2), iron),
        WBox('cap', (-1, 7, -1), (2, 1, 2), iron),
        WBox('eye', (-0.5, 8, -0.25), (1, 1, 0.5), [W.flat('plate')], hide={'down'}),
    ]
    pts = [(0, 8.6), (1.5, 9.0), (2.9, 8.4), (3.9, 7.1), (4.3, 5.6)]
    for i in range(4):
        w, d = ((0.5, 1.0), (1.0, 0.5))[i % 2]
        paint = [last_link] if i == 3 else [link_paint]
        b.append(seg('link_%d' % i, pts[i], pts[i + 1], w, d, paint, note='chain link %d' % i))
    hx = 4.3
    b += [
        WBox('lamp_cap', (hx - 1.25, 4.6, -1.25), (2.5, 1, 2.5), iron, note='lantern cap'),
        WBox('lamp', (hx - 1, 1.1, -1), (2, 3.5, 2), [cage_paint], hide={'up', 'down'},
             note='iron-barred lantern, a bone candle inside (lit ember on Legendary)'),
        WBox('lamp_base', (hx - 1.25, 0.1, -1.25), (2.5, 1, 2.5), iron),
        WBox('spike_l', (hx - 1.5, 2.6, -0.25), (0.5, 0.5, 0.5), [spike_paint]),
        WBox('spike_r', (hx + 1, 2.6, -0.25), (0.5, 0.5, 0.5), [spike_paint]),
        WBox('spike_f', (hx - 0.25, 2.6, 1), (0.5, 0.5, 0.5), [spike_paint]),
    ]
    if leg:
        b += [WBox('spike_b', (hx - 0.25, 2.6, -1.5), (0.5, 0.5, 0.5), [spike_paint], tier='legendary'),
              WBox('spike_d', (hx - 0.25, -0.4, -0.25), (0.5, 0.5, 0.5), [spike_paint], tier='legendary'),
              WBox('horn_l', (hx - 1.5, 5.6, -0.25), (0.5, 1, 0.5), [spike_paint, W.glow_tip()], tier='legendary'),
              WBox('horn_r', (hx + 1, 5.6, -0.25), (0.5, 1, 0.5), [spike_paint, W.glow_tip()], tier='legendary')]
    return b


def weapons():
    return [
        Weapon('gravereaper', 'Gravereaper', 'Barrow Warden', 'Graveborn', 'war scythe', pal, scythe, notes=__doc__,
               keys=KEYS, sidecar={'thickness': 2}),
        Weapon('wakelight', 'Wakelight', 'Barrow Warden', 'Graveborn', 'lantern flail', pal, flail, notes=__doc__,
               keys=KEYS, sidecar={'thickness': 2}),
    ]
