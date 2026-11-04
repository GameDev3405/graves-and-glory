"""Oathbound weapons (Gilded Order): the Oathblade, a sun-pommel longsword, and the Sunward, a heater shield.

Both carry the set's rarity ladder on their steel (blued on Rare, blackened toward violet on Epic, the pale sun-steel
of Dawnward on Legendary) and on the trim the rarity colour rides on (the sword's guard finials and pommel ring, the
shield's chief band). Gilt, enamel, leather and wood are the same at every rarity.
"""
from player_armor import paint as P
from player_armor.common import palette, rare_of, epic_of
from . import wpaint as W
from .wengine import Weapon, WBox, seg

STEEL = '#7C8591'
SPEC = {
    'plate': {'common': STEEL, 'uncommon': STEEL, 'rare': rare_of(STEEL), 'epic': epic_of(STEEL),
              'legendary': '#D6CCAE'},
    'accent': '#5A616E',
    'mats': {'gilt': ('#96783A', 0.26), 'leather': '#5A3E2C', 'enamel': ('#D9D3C1', 0.16), 'wood': '#6E5440'},
}
KEYS = {'plate': 'steel', 'accent': 'steel', 'gilt': 'gilt', 'leather': 'leather', 'enamel': 'steel', 'wood': 'wood',
        'ember': 'glass', 'void': 'iron'}


def pal(rarity):
    return palette(SPEC, rarity, 'crimson')


# ---- the Oathblade ------------------------------------------------------------------

def pommel_paint(c):
    W.metal('plate', marks=0)(c)
    if c.sem in ('front', 'back'):
        # a gilt sun boss in the wheel, whole pixels (it sits at the hand)
        c.rect(c.w // 2 - 1, c.h // 2 - 1, 2, 2, mat='gilt', tone=4 if c.sem == 'front' else 3)
        c.px(c.w // 2, c.h // 2, mat='gilt', tone=2)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        c.hline(c.h - 1, mat='accent', tone=W.TONE[c.sem])      # the pommel ring, in the rarity colour
        c.hline(c.h - 2, mat='accent', tone=max(0, W.TONE[c.sem] - 1))


def guard_paint(c):
    W.metal('plate', marks=0)(c)
    if c.sem == 'front':
        c.hline(0, d=1)


def boss_paint(c):
    W.metal('plate', marks=0)(c)
    if c.sem in ('front', 'back'):
        # the Order's sun on the blade-root boss: a gilt disc, an ember heart on Legendary
        cx = c.w // 2
        c.rect(cx - 1, 0, 2, c.h, mat='gilt', tone=3)
        c.px(cx - 2, 1, mat='gilt', tone=2); c.px(cx + 1, 1, mat='gilt', tone=2)
        c.px(cx - 1, 0, mat='gilt', tone=4)
        if c.legendary:
            c.px(cx - 1, 1, mat='ember', tone=4); c.px(cx, 1, mat='ember', tone=3)


def sword(tier, frame):
    leg = tier == 'legendary'
    b = [
        WBox('pommel', (-1.25, -4.5, -1), (2.5, 2, 2), [pommel_paint], note='wheel pommel, gilt sun boss, rarity ring'),
        WBox('grip', (-0.75, -2.5, -0.75), (1.5, 5, 1.5), [W.wrap('leather')], hide={'up', 'down'},
             note='leather grip wound in whole-pixel bands'),
        WBox('guard', (-4, 2.5, -0.75), (8, 1, 1.5), [guard_paint], note='straight cross'),
        WBox('boss', (-1.5, 3.5, -1), (3, 1.5, 2), [boss_paint], note='blade-root boss with the sun painted on'),
        WBox('fuller', (-0.25, 4.5, -0.25), (0.5, 13, 0.5), [W.fuller], hide={'down'}, note='recessed fuller'),
        WBox('flat_l', (-1.25, 5, -0.5), (1, 12.5, 1), [W.blade_flat(True)], rot=(0, 0, -1.0), origin=(-1.25, 5, 0),
             hide={'down'}, note='lit flat, tapering 1 degree inward'),
        WBox('flat_r', (0.25, 5, -0.5), (1, 12.5, 1), [W.blade_flat(False)], rot=(0, 0, 1.0), origin=(1.25, 5, 0),
             hide={'down'}, note='shade flat'),
        WBox('point_a', (-1, 17.5, -0.5), (2, 1, 1), [W.edge_steel], hide={'down'}),
        WBox('point_b', (-0.5, 18.5, -0.25), (1, 1, 0.5), [W.edge_steel], hide={'down'}),
        WBox('point_tip', (-0.25, 19.5, -0.25), (0.5, 0.5, 0.5), [W.edge_steel], hide={'down'}),
    ]
    if not leg:
        b += [WBox('finial_l', (-5, 2, -1), (1, 2, 2), [W.trim('accent')], note='rarity finial'),
              WBox('finial_r', (4, 2, -1), (1, 2, 2), [W.trim('accent')])]
    else:
        # Dawnward: the quillons sweep up into gilt wings, the pommel throws two rays, the boss two more
        b += [seg('wing_l', (-4, 3), (-5.6, 5.4), 1, 1, [W.trim('accent'), W.glow_tip()], tier='legendary'),
              seg('wing_r', (4, 3), (5.6, 5.4), 1, 1, [W.trim('accent'), W.glow_tip()], tier='legendary'),
              WBox('ray_l', (-2.25, -3.75, -0.25), (1, 0.5, 0.5), [W.trim('gilt')], tier='legendary'),
              WBox('ray_r', (1.25, -3.75, -0.25), (1, 0.5, 0.5), [W.trim('gilt')], tier='legendary'),
              seg('boss_ray_l', (-1.5, 4.5), (-2.6, 5.6), 0.5, 1, [W.trim('gilt')], tier='legendary'),
              seg('boss_ray_r', (1.5, 4.5), (2.6, 5.6), 0.5, 1, [W.trim('gilt')], tier='legendary')]
    return b


# ---- the Sunward ------------------------------------------------------------------------

BOARD_Z = 2.0   # the board's back sits 2 px in front of the grip, clear of the 4 px fist


def board_paint(c):
    if c.sem == 'front':
        c.fill('enamel', 3)
        for x in (0, c.w - 1):                         # a steel rim down each edge
            c.vline(x, mat='plate', tone=3 if x == 0 else 2)
        P._marks(c, 'enamel', int(c.w * c.h * 0.01))
    elif c.sem == 'back':
        W.wood('wood', grain=False, flecks=3)(c)
        for x in range(3, c.w, 6):                     # planks
            c.vline(x, d=-1)
    else:
        c.fill('plate', W.TONE[c.sem])
        c.px(0, 0, d=1)


def chief_paint(c):
    board_paint(c)
    if c.sem == 'front':
        # the chief: a band across the top in the rarity colour, a steel rim along the top edge
        c.hline(0, mat='plate', tone=4)
        for y in (2, 3, 4):
            c.hline(y, 1, c.w - 2, mat='accent', tone=3 if y < 4 else 2)
        # the Order's sun around the boss (boss texels: columns 9-14, rows 7-12): a ring, then eight rays
        for x in range(8, 16):
            c.px(x, 6, mat='gilt', tone=3); c.px(x, 13, mat='gilt', tone=2)
        for y in range(7, 13):
            c.px(8, y, mat='gilt', tone=3); c.px(15, y, mat='gilt', tone=2)
        for k in range(3):
            c.px(11, 5 - k, mat='gilt', tone=3); c.px(12, 5 - k, mat='gilt', tone=2)      # up
            c.px(11, 14 + k, mat='gilt', tone=2); c.px(12, 14 + k, mat='gilt', tone=1)    # down
            c.px(7 - k, 9, mat='gilt', tone=3); c.px(7 - k, 10, mat='gilt', tone=2)       # left
            c.px(16 + k, 9, mat='gilt', tone=2); c.px(16 + k, 10, mat='gilt', tone=1)     # right
        for k in range(2):
            c.px(7 - k, 5 - k, mat='gilt', tone=3); c.px(16 + k, 5 - k, mat='gilt', tone=2)
            c.px(7 - k, 14 + k, mat='gilt', tone=2); c.px(16 + k, 14 + k, mat='gilt', tone=1)
        if c.legendary:
            c.px(11, 2, mat='gilt', tone=4); c.px(12, 2, mat='gilt', tone=4)


def shield_boss(c):
    W.trim('gilt')(c)
    if c.sem == 'front':
        c.rect(2, 2, 2, 2, mat='ember' if c.legendary else 'gilt', tone=4 if c.legendary else 2)


def shield(tier, frame):
    z0 = BOARD_Z
    b = [
        WBox('board', (-6, -1, z0), (12, 10, 1), [chief_paint], note='upper board: enamel field, rarity chief, steel rim'),
        WBox('step_1', (-5.5, -3, z0), (11, 2, 1), [board_paint], hide={'up'}),
        WBox('step_2', (-4.5, -5, z0), (9, 2, 1), [board_paint], hide={'up'}),
        WBox('step_3', (-3, -6.5, z0), (6, 1.5, 1), [board_paint], hide={'up'}),
        WBox('step_4', (-1.5, -7.5, z0), (3, 1, 1), [board_paint], hide={'up'}, note='the heater point'),
        WBox('boss', (-1.5, 2.5, z0 + 1), (3, 3, 1), [shield_boss], hide={'north'}, note='gilt sun boss'),
        WBox('grip', (-0.75, -2, -0.75), (1.5, 4, 1.5), [W.wrap('leather')], hide={'up', 'down'}),
        WBox('bracket_t', (-0.75, 2, -1), (1.5, 1, 3), [W.flat('plate')], note='grip bracket'),
        WBox('bracket_b', (-0.75, -3, -1), (1.5, 1, 3), [W.flat('plate')]),
    ]
    if tier == 'legendary':
        c = (0, 4.0)
        for i, (dx, dy) in enumerate(((0, 1.4), (0, -1.4), (-1.4, 0), (1.4, 0))):
            p0 = (c[0] + dx * 1.15, c[1] + dy * 1.15)
            p1 = (c[0] + dx * 3.2, c[1] + dy * 3.2)
            b.append(seg('ray_%d' % i, p0, p1, 0.5, 0.5, [W.trim('gilt'), W.glow_tip()], z=z0 + 1.25,
                         tier='legendary', note='Dawnward sun ray'))
        b += [WBox('crest_l', (-6, 9, z0 - 0.25), (1.5, 1.5, 1.5), [W.trim('accent'), W.glow_tip()], tier='legendary'),
              WBox('crest_r', (4.5, 9, z0 - 0.25), (1.5, 1.5, 1.5), [W.trim('accent'), W.glow_tip()], tier='legendary')]
    return b


def weapons():
    return [
        Weapon('oathblade', 'Oathblade', 'Oathbound', 'Gilded Order', 'longsword', pal, sword, notes=__doc__, keys=KEYS,
               sidecar={'thickness': 1, 'length_px': 24.5}),
        Weapon('sunward', 'Sunward', 'Oathbound', 'Gilded Order', 'heater shield', pal, shield, notes=__doc__, keys=KEYS,
               sidecar={'thickness': 1, 'offhand': True}),
    ]
