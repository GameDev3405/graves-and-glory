"""Oathbound: the Gilded Order's knight-commander harness.

Read: an upright white-enamel knight with a keeled armet, a team plume that falls behind the helm, a gilt sun on the
breast, broad three-piece pauldrons, a team cape and a team hip-skirt under gilt-hemmed knees. Legendary (Dawnward):
ivory and gilt with a seven-ray sun halo behind the head, winged pauldrons, a taller plume, a long cape with the sun on it.
"""
from . import paint as P
from .common import R, palette, rare_of, epic_of
from .engine import ArmourSet, Box

SPEC = {
    'plate': {'common': '#D9D3C1', 'uncommon': '#D9D3C1', 'rare': '#AFC0D6', 'epic': '#4A4558',
              'legendary': '#E8DEC2'},
    'plate_step': 0.16,                       # pale enamel takes the pale-cloth step
    'accent': '#7C8591',                      # Common trim bands are bare steel
    'mats': {'gilt': ('#96783A', 0.26), 'steel': '#7C8591', 'leather': '#5A3E2C'},
}


def pal(rarity, team):
    return palette(SPEC, rarity, team)


# ---- helmet ------------------------------------------------------------------

def helm_face(c):
    if c.sem == 'front':
        w = c.w
        # keel: a ridge down the middle, lit on its left column
        c.vline(4, 0, 9, d=1)
        c.vline(5, 0, 9, d=-1)
        # brow shadow, then the two sights either side of the nasal
        # the sight: one dark slit across, broken only by the nasal; its lower lip catches the light
        c.hline(3, 0, 9, d=-1)
        c.hline(4, 1, 8, mat='void', tone=0)
        c.px(4, 4, mat='plate', tone=2); c.px(5, 4, mat='plate', tone=1)
        c.hline(5, 1, 3, d=1); c.hline(5, 6, 8, d=1)
        # breaths on the wearer's right cheek only (the left is the shield side): three short slots
        for x, y in ((1, 6), (2, 7), (1, 8), (2, 9)):
            c.px(x, y, mat='plate', tone=0)
        # gilt rivets where the brow band meets the visor pivots
        c.px(0, 2, mat='gilt', tone=4); c.px(w - 1, 2, mat='gilt', tone=2)
    elif c.sem in ('lit', 'dark'):
        # the visor pivot: a gilt boss on a vertical seam
        x = 3 if c.sem == 'lit' else c.w - 4
        c.vline(x + 1, 3, 9, d=-1)
        c.px(x, 4, mat='gilt', tone=4 if c.sem == 'lit' else 2)
    elif c.sem == 'back':
        # the tail of the helm flares over the nape: two lames
        c.hline(7, d=-1); c.hline(8, d=1); c.hline(9, d=-1)
    elif c.sem == 'top':
        c.vline(4, d=1); c.vline(5, d=-1)


def plume_paint(c):
    P.cloth('team', folds=False, hem=False)(c)
    if c.sem in ('lit', 'dark', 'front', 'back'):
        # horsehair: lengthwise strands, each a run of a tone up or down
        for y in range(c.h):
            x = c.rng.randrange(0, c.w)
            for k in range(c.rng.randrange(2, 5)):
                c.px(x + k, y, d=1 if (y + k) % 3 else -1)
    if c.sem == 'top':
        c.vline(0, d=1)


# ---- chest -------------------------------------------------------------------

def cuirass_face(c):
    if c.sem == 'front':
        # rows: 0-2 sit behind the bevor; the sun is centred on row 6; plackart line row 10; waist band row 12
        c.vline(4, 2, 9, d=1)
        sun = {4: (3, 5), 5: (2, 6), 6: (2, 6), 7: (2, 6), 8: (3, 5)}
        for y, (x0, x1) in sun.items():
            for x in range(x0, x1 + 1):
                c.px(x, y, mat='gilt', tone=3)
        c.px(3, 4, mat='gilt', tone=4); c.px(2, 5, mat='gilt', tone=4)
        c.px(6, 7, mat='gilt', tone=1); c.px(5, 8, mat='gilt', tone=1)
        for x, y in ((4, 3), (4, 9), (1, 6), (7, 6)):        # the four short rays
            c.px(x, y, mat='gilt', tone=2)
        if c.legendary:
            for x, y in ((4, 5), (3, 6), (4, 6), (5, 6), (4, 7)):
                c.px(x, y, mat='ember', tone=3)
            c.px(4, 6, mat='ember', tone=4)
        else:
            c.px(4, 6, mat='gilt', tone=4)
        c.hline(10, d=-1)
        c.hline(11, 0, 8, d=1)
    elif c.sem == 'back':
        # cape straps run under the cape: only their buckles show above it
        c.px(1, 0, mat='leather', tone=2); c.px(7, 0, mat='leather', tone=2)
        c.hline(10, d=-1)


def pauldron_cap(c):
    P.plate('plate')(c)
    P.edge('gilt', ('bottom',))(c)
    if c.sem in ('front', 'lit', 'dark', 'back'):
        c.px(c.w // 2, 1, mat='gilt', tone=4 if c.sem in ('front', 'lit') else 2)


def couter(c):
    P.plate('plate', marks=0)(c)
    if c.w >= 3 and c.h >= 3:
        c.px(1, 1, mat='gilt', tone=4)


def arm_face(c):
    if c.sem in ('top', 'bottom'):
        return
    # rows (top 24.5): 0-4 under the pauldron, 6 couter line, 9 gilt cuff, 10-12 steel gauntlet lames
    c.hline(5, d=-1)
    for y in (10, 11, 12):
        c.hline(y, mat='steel', tone=2 if c.sem in ('front', 'lit') else 1)
    c.hline(11, d=1)
    c.hline(12, d=-1)
    if c.sem in ('front', 'lit'):
        for x in range(0, c.w, 2):
            c.px(x + (1 if c.sem == 'lit' else 0), 12, mat='steel', tone=3)   # knuckle plates


def cape_paint(c):
    P.cloth('team', ragged=0)(c)
    if c.sem == 'back':
        P.edge('gilt', ('bottom',))(c)
        if c.legendary:
            # the sun embroidered on the cape, gilt with an ember heart
            cx, cy = c.w // 2, 6
            for dx, dy in ((0, -2), (-2, 0), (2, 0), (0, 2), (-1, -1), (1, -1), (-1, 1), (1, 1), (0, -1), (-1, 0),
                           (1, 0), (0, 1)):
                c.px(cx + dx, cy + dy, mat='gilt', tone=3)
            c.px(cx, cy, mat='ember', tone=3)
            for dx, dy in ((0, -4), (-4, 0), (4, 0), (0, 4), (-3, -3), (3, -3), (-3, 3), (3, 3)):
                c.px(cx + dx, cy + dy, mat='gilt', tone=2)


def clasp(c):
    P.trim('gilt')(c)


# ---- legs and boots ------------------------------------------------------------

def belt_paint(c):
    P.leather('leather')(c)
    if c.sem == 'front':
        # gilt buckle, centred, two texels
        c.px(4, 0, mat='gilt', tone=4); c.px(5, 0, mat='gilt', tone=3)
        c.px(4, 1, mat='gilt', tone=3); c.px(5, 1, mat='gilt', tone=1)
        c.px(6, 1, mat='leather', tone=1)   # the tongue's shadow
    if c.sem in ('front', 'lit', 'dark', 'back'):
        for x in range(1, c.w - 1, 3):
            if c.mat[0, x] == 'leather':
                c.px(x, 0, mat='accent', tone=3)


def skirt_paint(c):
    P.cloth('team')(c)
    P.edge('gilt', ('bottom',))(c)


def leg_face(c):
    if c.sem in ('top', 'bottom'):
        return
    # top 12.5: rows 0-4 under the skirt, 5-7 knee, 8-9 greave top (hidden in the boot below row 8)
    c.hline(4, d=-1)
    c.hline(8, d=-1)


def poleyn(c):
    P.plate('plate', marks=0)(c)
    if c.sem == 'front':
        c.px(1, 1, mat='gilt', tone=4)
        c.px(2, 1, mat='gilt', tone=3)
        c.hline(0, mat='accent', tone=3)
        c.px(0, 2, d=-1); c.px(3, 2, d=-1)
    elif c.sem in ('lit', 'dark'):
        c.hline(0, mat='accent', tone=2)


def boot_face(c):
    if c.sem in ('front', 'lit', 'dark', 'back'):
        c.hline(0, mat='accent', tone=3 if c.sem in ('front', 'lit') else 2)
        c.hline(2, d=-1)
        c.hline(3, mat='steel', tone=1)


def toe_paint(c):
    P.plate('plate', marks=0)(c)
    if c.sem in ('top',):
        c.hline(c.h - 1, mat='gilt', tone=3)


# ---- the set --------------------------------------------------------------------

def build():
    b = []
    plate = P.plate('plate', marks=0.015)
    # helmet
    b.append(Box('armet', 'helmet', 'head', (-5, 23, -5), (10, 10, 10),
                 [plate, P.band('accent', [2]), P.draw(helm_face)], hide={'down'},
                 note='keeled armet: brow band (rarity), split sight, breaths on the right cheek, gilt pivots'))
    b.append(Box('bevor', 'helmet', 'head', (-4.5, 22, -5.6), (9, 3, 1),
                 [P.plate('plate', marks=0), P.edge('gilt', ('bottom',)), P.draw(lambda c: c.vline(4, d=1) if c.sem == 'front' else None)],
                 hide={'south'}, note='chin plate over the gorget, gilt lower edge'))
    b.append(Box('plume_socket', 'helmet', 'head', (-1, 33, -3), (2, 1, 2), [P.trim('gilt')], hide={'down'}))
    b.append(Box('plume', 'helmet', 'head', (-1, 34, -3), (2, 2, 8), [plume_paint], tier='base',
                 note='team horsehair crest, front to back'))
    b.append(Box('plume_tail', 'helmet', 'head', (-1, 28, 5), (2, 6, 2), [plume_paint], tier='base',
                 note='the crest falls behind the helm'))
    b.append(Box('plume_tall', 'helmet', 'head', (-1, 34, -3), (2, 3, 8), [plume_paint], tier='legendary'))
    b.append(Box('plume_tail_long', 'helmet', 'head', (-1, 25, 5), (2, 9, 2), [plume_paint], tier='legendary'))
    # legendary sun halo: seven gilt rays fanned behind the head, ember tips
    b.append(Box('halo_hub', 'helmet', 'head', (-1.5, 27.5, 5.2), (3, 3, 1), [P.trim('gilt'),
                 P.draw(lambda c: c.px(1, 1, mat='ember', tone=3) if c.sem in ('back', 'front') else None)],
                 tier='legendary'))
    for i, ang in enumerate((-67.5, -45, -22.5, 0, 22.5, 45, 67.5)):
        ln = 7 if i % 2 == 1 else 5
        b.append(Box('halo_ray_%d' % i, 'helmet', 'head', (-0.5, 30, 5.6), (1, ln, 1),
                     [P.trim('gilt'), P.draw(lambda c: c.px(0, 0, mat='ember', tone=4) if c.sem in ('back', 'front', 'top', 'lit', 'dark') else None)],
                     rot=(0, 0, ang), origin=(0, 29, 6.1), tier='legendary', note='sun ray'))
    # chest
    b.append(Box('cuirass', 'chest', 'body', (-4.5, 11.5, -3), (9, 13, 6),
                 [plate, P.band('accent', [12]), P.draw(cuirass_face)],
                 note='enamel breastplate, gilt sun (ember heart on Legendary), plackart line, rarity waist band'))
    b += R(Box('pauldron_cap_r', 'chest', 'right_arm', (4.5, 22, -3.5), (6, 3, 7), [pauldron_cap], side='R',
               note='shoulder cap, gilt rim and boss'),
           Box('pauldron_dome_r', 'chest', 'right_arm', (5.5, 25, -2), (4, 1, 4), [P.plate('plate', marks=0)],
               side='R', hide={'down'}),
           Box('pauldron_lame_r', 'chest', 'right_arm', (5, 20, -3), (6, 2, 6),
               [P.plate('plate'), P.band('accent', [1])], inflate=0.15, side='R', note='lower lame, rarity edge'),
           Box('arm_r', 'chest', 'right_arm', (3.5, 11.5, -2.5), (5, 13, 5), [plate, P.band('gilt', [9]),
               P.draw(arm_face)], inflate=0.02, side='R', hide={'up'}),
           Box('couter_r', 'chest', 'right_arm', (8.5, 16, -1.5), (1, 3, 3), [couter], side='R', hide={'west'}))
    # Dawnward: each pauldron sweeps up and back into a stepped gilt wing, an ember spark at its tip
    for i, (y, z, d) in enumerate(((25.5, -2.5, 5), (26.5, -1.5, 4), (27.5, -0.5, 3), (28.5, 0.5, 2))):
        tip = i == 3
        b += R(Box('wing%d_r' % i, 'chest', 'right_arm', (9, y, z), (1, 1, d),
                   [P.trim('gilt')] + ([P.draw(lambda c: c.px(c.w - 1 if c.sem == 'lit' else 0, 0, mat='ember', tone=4)
                                               if c.sem in ('lit', 'dark', 'top') else None)] if tip else []),
                   side='R', tier='legendary', note='wing feather %d' % i))
    b.append(Box('cape', 'chest', 'body', (-4.5, 6.5, 3.1), (9, 18, 1), [cape_paint], tier='base',
                 note='team cape to mid-thigh, gilt hem'))
    b.append(Box('cape_long', 'chest', 'body', (-4.5, 2.5, 3.1), (9, 22, 1), [cape_paint], tier='legendary',
                 note='team cape to the ankle with the embroidered sun'))
    b.append(Box('cape_bar', 'chest', 'body', (-3.5, 23.6, 2.9), (7, 1, 1), [clasp], note='gilt bar the cape hangs from'))
    # legs
    b.append(Box('belt', 'legs', 'body', (-5, 11, -3.5), (10, 2, 7), [belt_paint],
                 note='sword belt: gilt buckle, studs in the rarity colour'))
    b += R(Box('leg_r', 'legs', 'right_leg', (-0.6, 2.5, -2.5), (5, 10, 5), [plate, P.draw(leg_face)], side='R',
               hide={'up', 'down'}),
           Box('skirt_r', 'legs', 'right_leg', (-0.6, 6.5, -2.5), (5, 6, 5), [skirt_paint], inflate=0.35, side='R',
               hide={'up'}, note='team surcoat skirt to the knee with a gilt hem, split per leg so it swings'),
           Box('poleyn_r', 'legs', 'right_leg', (0, 4, -3.4), (4, 3, 1), [poleyn], side='R', hide={'south'}))
    # boots
    b += R(Box('sabaton_r', 'boots', 'right_leg', (-0.6, 0.25, -2.5), (5, 4, 5), [plate, P.draw(boot_face)],
               inflate=0.25, side='R', note='sabaton: rarity cuff, one lame line, steel sole'),
           Box('toe_r', 'boots', 'right_leg', (0.4, 0, -4.5), (3, 2, 2), [toe_paint], side='R', hide={'south', 'down'}))
    return ArmourSet('oathbound', 'Oathbound', 'Gilded Order', pal, b, __doc__)
