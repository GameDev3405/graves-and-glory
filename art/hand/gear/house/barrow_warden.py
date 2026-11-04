"""Barrow Warden: the Graveborn grave-guard's harness.

Read: a hunched, ragged shape under a team grave-hood, a cracked bone skull-mask in the hood's mouth, bone ribs strapped
over a rust-bloomed iron cuirass, a skull pauldron on the right shoulder and a bare iron spaulder on the left
(asymmetric), a tattered team mantle and back-drape, mail chausses with bone kneecaps, iron boots with bone toe-claws.
Legendary (the Lich-warden): grave-black iron, a broken iron crown over the hood, ember light in the mask's sockets and
between the ribs, horns on the skull pauldron, spikes on the spaulder and a drape to the ankles.
"""
from . import paint as P
from .common import R, palette, row
from .engine import ArmourSet, Box

SPEC = {
    'plate': {'common': '#6A6660', 'uncommon': '#6A6660', 'rare': '#4E6680', 'epic': '#3D3548',
              'legendary': '#2E2B30'},
    'accent': '#7A6248',                       # Common trim: a rusted bronze band
    'mats': {'bone': ('#C9C1A8', 0.16), 'rust': '#6B4A33', 'moss': '#4F5A3C', 'leather': '#4A3428'},
}


def pal(rarity, team):
    return palette(SPEC, rarity, team)


iron = [P.plate('plate', marks=0.03), P.rust('rust', 0.06)]

# ---- helmet ------------------------------------------------------------------


def mask_face(c):
    """A 7x7 skull: brow, two 2x2 sockets, a notched nose, a row of teeth, a jaw line. One crack from the brow."""
    if c.sem != 'front':
        return
    c.hline(0, tone=4)
    for x0 in (1, 4):
        c.rect(x0, 1, 2, 2, mat='void', tone=0)
    c.px(0, 3, tone=1); c.px(6, 3, tone=1)          # cheekbone shadow
    c.px(3, 3, mat='void', tone=0); c.px(3, 4, mat='void', tone=1)
    c.px(2, 4, tone=2); c.px(4, 4, tone=2)
    for x in range(1, 6):                             # teeth: bone and gaps
        c.px(x, 5, tone=4 if x % 2 else 1)
    c.hline(6, 1, 5, tone=1)
    c.px(0, 6, tone=2); c.px(6, 6, tone=2)
    c.px(2, 0, tone=1); c.px(2, 1, mat='bone', tone=1) if False else None
    c.vline(5, 3, 4, tone=1)                          # the crack runs from the right socket down the cheek
    if c.legendary:
        c.px(1, 2, mat='ember', tone=4); c.px(5, 2, mat='ember', tone=4)
        c.px(2, 2, mat='ember', tone=2); c.px(4, 2, mat='ember', tone=2)


def hood_paint(c):
    P.cloth('team', folds=True, hem=False)(c)
    if c.sem == 'front':
        # the hood's mouth: open in the middle, a dark inner rim, a ragged brim over the brow
        for y in range(2, c.h):
            for x in range(2, c.w - 2):
                c.clear(x, y)
        for x in range(2, c.w - 2):
            c.px(x, 1, d=-1)
            if c.rng.random() < 0.45:
                c.px(x, 2, mat='team', tone=1)
        for y in range(2, c.h):
            c.px(1, y, tone=1); c.px(c.w - 2, y, tone=0)
    elif c.sem in ('lit', 'dark', 'back'):
        # torn lower edge
        for x in range(c.w):
            k = c.rng.choice((0, 0, 1, 1, 2))
            for y in range(c.h - k, c.h):
                c.clear(x, y)


def hood_tail(c):
    P.cloth('team', ragged=3)(c)


def crown_paint(c):
    P.plate('plate', marks=0)(c)
    P.rust('rust', 0.1)(c)


# ---- chest -------------------------------------------------------------------

TOP = 24.5
RIBS = (21.5, 19.5, 17.5)


def cuirass_face(c):
    if c.sem == 'front':
        # the iron is darkened where the ribs lie, so the bone stands off it; Legendary glows between them
        for y_rib in RIBS + (15.5,):
            r = row(TOP, y_rib) - 1
            gap = r + 1
            if 0 <= gap < c.h:
                for x in range(0, c.w):
                    if x in (4,):
                        continue
                    if c.legendary and 1 <= x <= c.w - 2 and y_rib != 15.5:
                        c.px(x, gap, mat='ember', tone=2 if x in (1, c.w - 2) else 3)
                    else:
                        c.px(x, gap, mat='plate', tone=0)
        c.hline(c.h - 1, mat='accent', tone=2)
    elif c.sem == 'back':
        c.vline(4, 0, c.h - 1, d=-1)                  # the backplate's spine seam


def rib_paint(c):
    P.bone('bone', crack=False)(c)
    if c.sem == 'front' and c.w >= 3:
        c.px(c.w - 1, 0, tone=2)


def mantle_paint(c):
    P.cloth('team', ragged=2)(c)
    if c.sem == 'front':
        # the mantle parts at the throat; a bone toggle holds it
        c.clear(5, 2); c.clear(5, 1)
        c.px(5, 0, mat='bone', tone=4)


def drape_paint(c):
    P.cloth('team', ragged=4)(c)
    if c.sem in ('back', 'front'):
        # moth holes and tears, placed by hand-like rules: more toward the hem
        for _ in range(3 if not c.legendary else 5):
            x, y = c.rng.randrange(1, c.w - 1), c.rng.randrange(c.h // 3, c.h - 3)
            c.clear(x, y)
            c.px(x, y - 1, d=-1)


def skull_paint(c):
    P.bone('bone')(c)
    if c.face in ('east', 'west') and (c.box.side == 'R' and c.face == 'east' or c.box.side == 'L' and c.face == 'west'):
        # the skull looks outward from the shoulder: sockets, nose, teeth
        c.rect(0, 1, 2, 2, mat='void', tone=0); c.rect(3, 1, 2, 2, mat='void', tone=0)
        c.px(2, 3, mat='void', tone=0)
        for x in range(0, c.w):
            c.px(x, 4, tone=4 if x % 2 == 0 else 1)
        c.hline(0, tone=4)
        if c.legendary:
            c.px(1, 2, mat='ember', tone=4); c.px(3, 2, mat='ember', tone=4)
    elif c.sem == 'top':
        c.vline(c.w // 2, 0, c.h - 1, tone=1)        # the sagittal suture


def spaulder_paint(c):
    for layer in iron:
        layer(c)
    P.band('accent', [-1])(c)
    if c.sem in ('front', 'back', 'lit', 'dark') and c.w >= 4:
        c.px(1, 1, mat='plate', tone=4); c.px(c.w - 2, 1, mat='plate', tone=3)


def arm_face(c):
    if c.sem in ('top', 'bottom'):
        return
    # top 24.5: rows 0-4 iron (rerebrace), 5 a strap, 6-11 bone splints on the forearm bound with leather, 12 claws
    c.hline(5, mat='leather', tone=2)
    # splints only on the faces a blow lands on: the front and the outer side
    out = (c.box.side == 'R' and c.face == 'east') or (c.box.side == 'L' and c.face == 'west')
    if c.sem == 'front' or out:
        for x in (1, 3):
            c.vline(x, 6, 11, mat='bone', tone=3 if c.sem in ('front', 'lit') else 2)
            c.px(x, 6, tone=4 if c.sem in ('front', 'lit') else 3)
            c.px(x, 11, tone=1)
        c.hline(9, mat='leather', tone=1)
    if c.sem in ('front', 'lit', 'dark', 'back'):
        c.hline(12, mat='plate', tone=1)
        if c.sem == 'front':
            for x in (0, 2, 4):
                c.px(x, 12, mat='bone', tone=2)


# ---- legs and boots ------------------------------------------------------------

def belt_paint(c):
    P.mail('plate')(c)
    P.rust('rust', 0.08)(c)


def buckle_paint(c):
    P.bone('bone', crack=False)(c)
    if c.sem == 'front':
        c.px(0, 0, mat='void', tone=0); c.px(1, 0, mat='void', tone=0)
        c.px(0, 1, tone=4); c.px(1, 1, tone=2)


def rag_paint(c):
    P.cloth('moss', ragged=3)(c)


def kneecap(c):
    P.bone('bone', crack=False)(c)
    if c.sem == 'front':
        c.clear(0, 0); c.clear(2, 0); c.clear(0, 2); c.clear(2, 2)
        c.px(1, 1, tone=4); c.px(1, 2, tone=2)


def tasset_paint(c):
    for layer in iron:
        layer(c)
    P.band('accent', [-1])(c)


def boot_face(c):
    if c.sem in ('front', 'lit', 'dark', 'back'):
        c.hline(0, mat='accent', tone=3 if c.sem in ('front', 'lit') else 2)
        c.hline(3, mat='plate', tone=0)


def claw_paint(c):
    P.bone('bone', crack=False)(c)
    if c.sem == 'front':
        c.px(0, 0, tone=4)


def horn_paint(c):
    P.bone('bone', crack=False)(c)


# ---- the set --------------------------------------------------------------------

def build():
    b = []
    # helmet
    b.append(Box('sallet', 'helmet', 'head', (-5, 23, -5), (10, 10, 10), iron, hide={'down'},
                 note='rusted iron sallet: only the brow shows under the hood'))
    b.append(Box('mask', 'helmet', 'head', (-3.5, 24, -5.3), (7, 7, 1), [P.bone('bone', crack=False), P.draw(mask_face)],
                 hide={'south'}, note='bone skull mask in the hood mouth, ember sockets on Legendary'))
    b.append(Box('hood', 'helmet', 'head', (-5.5, 25, -5.5), (11, 9, 11), [hood_paint], hide={'down'},
                 note='team grave-hood, open at the face, torn at the shoulders'))
    b.append(Box('hood_peak', 'helmet', 'head', (-1, 34, 0), (2, 1, 4), [P.cloth('team', folds=False, hem=False)],
                 hide={'down'}, note='the hood folds to a point behind the crown'))
    b.append(Box('hood_tail', 'helmet', 'head', (-4, 18, 4.6), (8, 7, 1), [hood_tail], hide={'north'},
                 note='the hood hangs down the back in strips'))
    # Legendary: a broken iron crown ringing the hood, five spikes, one snapped short
    b.append(Box('crown_front', 'helmet', 'head', (-5, 33.2, -6), (10, 1, 1), [crown_paint], tier='legendary'))
    b += R(Box('crown_side_r', 'helmet', 'head', (5, 33.2, -6), (1, 1, 11), [crown_paint], tier='legendary', side='R'))
    b.append(Box('crown_back', 'helmet', 'head', (-5, 33.2, 4), (10, 1, 1), [crown_paint], tier='legendary'))
    for i, (x, h) in enumerate(((-4.5, 2), (-1.5, 3), (0.5, 1), (3.5, 2))):
        b.append(Box('crown_spike_%d' % i, 'helmet', 'head', (x, 34.2, -6), (1, h, 1),
                     [crown_paint, P.draw(lambda c: c.px(0, 0, tone=4) if c.sem in ('front', 'lit') else None)],
                     tier='legendary', note='crown spike'))
    b.append(Box('crown_gem', 'helmet', 'head', (-0.5, 32.9, -6.4), (1, 1, 1),
                 [P.draw(lambda c: c.fill('ember', 4 if c.sem == 'front' else 2))], tier='legendary', hide={'south'}))
    # chest
    b.append(Box('cuirass', 'chest', 'body', (-4.5, 11.5, -3), (9, 13, 6), iron + [P.draw(cuirass_face)],
                 note='rusted iron cuirass, darkened under the ribs (ember between them on Legendary)'))
    b.append(Box('sternum', 'chest', 'body', (-0.5, 15.5, -3.6), (1, 7, 1), [P.bone('bone')], hide={'south'}))
    for i, y in enumerate(RIBS):
        b += R(Box('rib%d_r' % i, 'chest', 'body', (0.5, y, -3.5), (3, 1, 1), [rib_paint], side='R', hide={'south'}),
               Box('ribend%d_r' % i, 'chest', 'body', (3.4, y - 1.1, -3.4), (1, 2, 1), [rib_paint], side='R',
                   hide={'south'}))
    b += R(Box('rib3_r', 'chest', 'body', (0.5, 15.5, -3.5), (2, 1, 1), [rib_paint], side='R', hide={'south'}))
    b.append(Box('mantle', 'chest', 'body', (-5.5, 21.6, -3.7), (11, 3, 7), [mantle_paint],
                 note='team mantle over the shoulders, ragged hem, bone toggle'))
    b.append(Box('drape', 'chest', 'body', (-4.5, 7, 3.1), (9, 15, 1), [drape_paint], tier='base',
                 note='team back-drape to the thigh, torn'))
    b.append(Box('drape_long', 'chest', 'body', (-4.5, 3, 3.1), (9, 19, 1), [drape_paint], tier='legendary'))
    # right shoulder: a skull; left: a bare rusted spaulder (the warden is lopsided)
    b.append(Box('skull_pauldron', 'chest', 'right_arm', (4.5, 21, -2.5), (5, 5, 5), [skull_paint], side='R',
                 note='bone skull over the right shoulder, looking outward'))
    b.append(Box('skull_lame', 'chest', 'right_arm', (4, 19, -3), (5, 2, 6), [tasset_paint], inflate=0.1, side='R',
                 hide={'up'}))
    b.append(Box('skull_horn_back', 'chest', 'right_arm', (6.5, 26, 1), (1, 3, 1), [horn_paint], tier='legendary',
                 side='R', hide={'down'}))
    b.append(Box('skull_horn_tip', 'chest', 'right_arm', (6.5, 28, 2), (1, 2, 1), [horn_paint], tier='legendary',
                 side='R', hide={'down'}))
    b.append(Box('skull_horn_front', 'chest', 'right_arm', (6.5, 26, -2), (1, 2, 1), [horn_paint], tier='legendary',
                 side='R', hide={'down'}))
    b.append(Box('spaulder', 'chest', 'left_arm', (-10, 21.5, -3), (5, 3, 6), [spaulder_paint], side='L',
                 note='bare rusted spaulder, rarity edge'))
    b.append(Box('spaulder_lame', 'chest', 'left_arm', (-9.5, 19.5, -2.8), (4, 2, 6), [spaulder_paint], inflate=0.1,
                 side='L', hide={'up'}))
    for i, z in enumerate((-2, 0, 2)):
        b.append(Box('spaulder_spike_%d' % i, 'chest', 'left_arm', (-8.5 + (i % 2) * 0.5, 24.5, z - 0.5), (1, 2 + (i == 1), 1),
                     [crown_paint], tier='legendary', side='L', hide={'down'}))
    b += R(Box('arm_r', 'chest', 'right_arm', (3.5, 11.5, -2.5), (5, 13, 5), iron + [P.draw(arm_face)], inflate=0.02,
               side='R', hide={'up'}, note='iron arm, bone splints bound with leather on the forearm'))
    # legs
    b.append(Box('belt', 'legs', 'body', (-5, 10.8, -3.5), (10, 1, 7), [belt_paint], note='rusted chain belt'))
    b.append(Box('buckle', 'legs', 'body', (-1, 10.5, -3.9), (2, 2, 1), [buckle_paint], hide={'south'},
                 note='a tiny bone skull for a buckle'))
    b.append(Box('rag_front', 'legs', 'body', (-2.5, 4, -3.3), (5, 7, 1), [rag_paint], hide={'south'},
                 note='grave-moss rag hanging between the legs'))
    b += R(Box('leg_r', 'legs', 'right_leg', (-0.6, 2.5, -2.5), (5, 10, 5), [P.mail('plate'), P.rust('rust', 0.05)],
               side='R', hide={'up', 'down'}, note='mail chausses'),
           Box('tasset_r', 'legs', 'right_leg', (-0.4, 7.5, -2.9), (5, 4, 1), [tasset_paint], side='R', hide={'south'}),
           Box('kneecap_r', 'legs', 'right_leg', (0.4, 4.5, -3.2), (3, 3, 1), [kneecap], side='R', hide={'south'}))
    # boots
    b += R(Box('boot_r', 'boots', 'right_leg', (-0.6, 0.25, -2.5), (5, 4, 5), iron + [P.draw(boot_face)], inflate=0.25,
               side='R'))
    for i, x in enumerate((-0.3, 1.4, 3.1)):
        b += R(Box('claw%d_r' % i, 'boots', 'right_leg', (x, 0, -3.75), (1, 1, 1), [claw_paint], side='R',
                   hide={'south', 'down'}))
    return ArmourSet('barrow_warden', 'Barrow Warden', 'Graveborn', pal, b, __doc__)
