"""Thornhide weapons (Wildmoot): the Tuskspear, a boar spear, and the Antlerbow, a recurve hunting bow.

The spear's iron head takes the rarity ladder (the charred, ember-veined head of the Antlerking on Legendary). The bow has
little metal, so its rarity shows on the rawhide wraps at the grip and on the limbs (`accent`) and on the iron nock caps.
Bone, fur, wood and leather never change. The bow has the standard draw frames: `rest` and `pull_0` to `pull_2`, the
limbs flexing a little more each frame and the string drawn back to the arrow's nock.
"""
from player_armor import paint as P
from player_armor.common import palette, rare_of, epic_of
from player_armor.thornhide import SPEC as ARMOUR
from . import wpaint as W
from .wengine import Weapon, WBox, seg, chain, mirror_y

IRON = '#6E7378'
SPEC = {
    'plate': {'common': IRON, 'uncommon': IRON, 'rare': rare_of(IRON), 'epic': epic_of(IRON), 'legendary': '#3A3430'},
    'accent': ARMOUR['accent'],
    'mats': dict(ARMOUR['mats'], wood='#6E5440', string=('#D9D3C1', 0.16)),
}
KEYS = {'plate': 'iron', 'accent': 'leather', 'fur': 'leather', 'bark': 'wood', 'bone': 'bone', 'clay': 'bone',
        'leather': 'leather', 'wrap': 'linen', 'feather': 'bone', 'wood': 'wood', 'string': 'linen', 'ember': 'glass',
        'void': 'iron'}


def pal(rarity):
    return palette(SPEC, rarity, 'crimson')


# ---- the Tuskspear ------------------------------------------------------------------

def haft_paint(c):
    W.wood('wood')(c)
    P.band('accent', [-1, -2, -4, -5])(c)        # rawhide binding under the socket


def head_flat(c):
    W.metal('plate', marks=0.03)(c)
    if c.sem in ('front', 'back'):
        c.vline(0, tone=4 if c.sem == 'front' else 3)
    if c.legendary and c.sem == 'front' and c.h >= 6:
        c.vline(c.w // 2, 1, c.h - 2, mat='ember', tone=3)   # a vein of ember down each flat


def rib_paint(c):
    W.metal('plate', marks=0)(c)
    if c.sem in ('front', 'back'):
        c.vline(0, d=1)


def lug_paint(c):
    P.bone('bone', crack=False)(c)


def tuft_paint(c):
    P.fur('fur', ragged=2)(c)


def spear(tier, frame):
    leg = tier == 'legendary'
    b = [
        WBox('haft', (-0.75, -10, -0.75), (1.5, 25, 1.5), [haft_paint], note='ash haft, rawhide binding under the head'),
        WBox('butt', (-1, -11, -1), (2, 1, 2), [lug_paint], hide={'up'}, note='bone butt cap'),
        WBox('socket', (-1, 15, -1), (2, 2, 2), [W.metal('plate', 0)], note='iron socket'),
        WBox('tuft', (-1.25, 12.5, -1.25), (2.5, 2.5, 2.5), [tuft_paint], hide={'up', 'down'},
             note='a boar-hair tuft under the socket'),
        WBox('rib', (-0.5, 16.5, -0.75), (1, 9, 1.5), [rib_paint], hide={'down'}, note='proud midrib'),
        WBox('tip', (-0.25, 25.5, -0.5), (0.5, 1, 1), [W.edge_steel], hide={'down'}),
    ]
    b += [seg('flat_l', (-0.5, 17), (-1.9, 20.6), 2, 1, [head_flat]),
          seg('flat_r', (0.5, 17), (1.9, 20.6), 2, 1, [head_flat], z=0.1),
          seg('upper_l', (-1.9, 20.6), (-0.25, 25.5), 1.5, 0.5, [head_flat]),
          seg('upper_r', (1.9, 20.6), (0.25, 25.5), 1.5, 0.5, [head_flat], z=0.1)]
    if not leg:
        b.append(WBox('lugs', (-2.5, 15.5, -0.5), (5, 1, 1), [lug_paint], note='bone lugs: the boar-spear crossbar'))
    else:
        # Antlerking: the lugs become two antler tines a side, ember-tipped; boar tusks at the butt
        for s in (-1, 1):
            n = 'l' if s < 0 else 'r'
            b.append(seg('antler_%s' % n, (s * 0.8, 16.2), (s * 2.6, 17.2), 0.5, 1, [lug_paint], z=-0.15, tier='legendary'))
            b.append(seg('tine_%s' % n, (s * 2.4, 17.0), (s * 2.9, 18.9), 0.5, 0.5, [lug_paint, W.glow_tip()],
                         z=0.3, tier='legendary'))
            b.append(seg('tusk_%s' % n, (s * 0.8, -10.5), (s * 1.9, -9.0), 0.5, 0.5, [lug_paint], tier='legendary'))
    return b


# ---- the Antlerbow ------------------------------------------------------------------------

STAVES = ((4.0, 5, 2.0, 2.0), (3.5, 14, 1.5, 1.5), (3.5, 28, 1.5, 1.5), (2.5, -14, 1.0, 1.0))   # length, angle, w, d
FLEX = {'rest': 0.0, 'pull_0': 1.0, 'pull_1': 2.0, 'pull_2': 3.0}
DRAW = {'pull_0': -4.0, 'pull_1': -5.0, 'pull_2': -6.0}


def limb_paint(i):
    def run(c):
        W.wood('wood')(c)
        if i in (0, 2) and c.sem in ('front', 'back', 'lit', 'dark'):
            for y in (1, 2) if i == 0 else (c.h - 3, c.h - 2):
                c.hline(y, mat='accent', tone=W.TONE[c.sem])     # rawhide limb wraps (rarity)
    return [run]


def nock_paint(c):
    P.bone('bone', crack=False)(c)
    if c.sem in ('front', 'lit', 'back', 'dark'):
        c.hline(c.h - 1, mat='plate', tone=W.TONE[c.sem])
        c.hline(c.h - 2, mat='plate', tone=max(0, W.TONE[c.sem] - 1))


def string_paint(c):
    c.fill('string', 2)


def bow(tier, frame):
    leg = tier == 'legendary'
    flex = FLEX[frame]
    parts = [(L, a + flex * (k + 1) * 0.9, w, d) for k, (L, a, w, d) in enumerate(STAVES)]
    upper, pts = chain('limb', (0, 2.25), parts, limb_paint, ext=0.25)
    lower = mirror_y(upper)
    tip = pts[-1]
    nock_at = pts[-2]
    b = [WBox('grip', (-1, -2.5, -1.25), (2, 5, 2.5), [W.wrap('leather')], note='leather grip')] + upper + lower
    # antler nocks: a short bone tine out of each tip, iron-capped where the string sits
    ant = seg('antler', tip, (tip[0] + 0.9, tip[1] + 1.4), 0.5, 0.5, [nock_paint], z=0.0)
    b += [ant] + mirror_y([ant])
    if leg:
        tine = seg('antler_tine', tip, (tip[0] - 0.9, tip[1] + 1.4), 0.5, 0.5,
                   [P.bone('bone', crack=False), W.glow_tip()], z=-0.3, tier='legendary')
        b += [tine] + mirror_y([tine])
    top = (nock_at[0] - 0.1, nock_at[1])
    if frame == 'rest':
        b.append(WBox('string', (top[0] - 0.25, -top[1], -0.25), (0.5, round(top[1] * 4) / 2, 0.5), [string_paint],
                      note='bowstring, tone 2'))
    else:
        d = (DRAW[frame], 0.0)
        b.append(seg('string_a_%s' % frame, top, d, 0.5, 0.5, [string_paint], ext=0, frames=(frame,)))
        b.append(seg('string_b_%s' % frame, (top[0], -top[1]), d, 0.5, 0.5, [string_paint], z=0.1, ext=0, frames=(frame,)))
        shaft_len = round((2.0 - d[0]) * 2) / 2
        b.append(WBox('arrow_%s' % frame, (d[0], -0.25, -0.35), (shaft_len, 0.5, 0.5),
                      [lambda c: c.fill('wood', W.TONE[c.sem])], frames=(frame,), note='the nocked arrow'))
        b.append(WBox('arrowhead_%s' % frame, (d[0] + shaft_len, -0.5, -0.5), (1, 1, 1), [W.edge_steel],
                      frames=(frame,)))
    return b


def weapons():
    return [
        Weapon('tuskspear', 'Tuskspear', 'Thornhide', 'Wildmoot', 'boar spear', pal, spear, notes=__doc__, keys=KEYS,
               sidecar={'thickness': 2}),
        Weapon('antlerbow', 'Antlerbow', 'Thornhide', 'Wildmoot', 'recurve bow', pal, bow,
               frames=('rest', 'pull_0', 'pull_1', 'pull_2'), notes=__doc__, keys=KEYS,
               sidecar={'thickness': 2, 'pull': [1.0, 2.0, 3.0], 'draw_x': [DRAW['pull_0'], DRAW['pull_1'], DRAW['pull_2']]}),
    ]
