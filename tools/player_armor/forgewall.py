"""Forgewall: the Ironclad hold-captain's harness.

Read: wide and square. A flat-topped box helm with a brass face-grille, a projecting brow and riveted cheek plates; slab
pauldrons (a flat slab over the shoulder and a plate dropping down the outside, an L in section) that square off the
shoulders; a riveted cuirass with a furnace-grille plackart; big blocky gauntlets with brass knuckles; a team tabard
and apron front and back; a knee-length scale skirt split per leg; square iron boots with toe caps. Legendary
(Forgeheart): soot-black iron, an ember-lit grille in the helm and the chest, ember vents in the pauldron plates and twin
brass-banded smokestacks rising behind the shoulders.
"""
from . import paint as P
from .common import R, palette
from .engine import ArmourSet, Box

SPEC = {
    'plate': {'common': '#5F6469', 'uncommon': '#5F6469', 'rare': '#4A6788', 'epic': '#3E3749',
              'legendary': '#34363A'},
    'accent': '#9DA3A9',                      # Common trim: bright polished steel bands
    'mats': {'brass': ('#7F6232', 0.26), 'soot': '#34373B', 'leather': '#4B3A2C', 'copper': '#7A4A2E'},
}


def pal(rarity, team):
    return palette(SPEC, rarity, team)


def corners(inset=1):
    return lambda c: [(inset, inset), (c.w - 1 - inset, inset), (inset, c.h - 1 - inset), (c.w - 1 - inset, c.h - 1 - inset)] \
        if c.w > 2 * inset + 1 and c.h > 2 * inset + 1 else []


def riveted(inset=1):
    return P.rivets('brass', corners(inset))


iron = [P.plate('plate', marks=0.03)]


# ---- helmet ------------------------------------------------------------------

def helm_face(c):
    if c.sem == 'front':
        # rows: 0-1 under the brow, 3-7 the grille (brass bars over a dark slot), 8-9 the chin
        glow = c.legendary
        for y in range(3, 8):
            for x in range(1, 9):
                if x % 2 == 0:
                    c.px(x, y, mat='brass', tone=3 if y == 3 else 2)
                else:
                    c.px(x, y, mat='ember' if glow and y in (4, 5) else 'void', tone=3 if glow else 0)
        c.hline(2, 1, 8, mat='brass', tone=4)
        c.hline(8, 1, 8, mat='brass', tone=1)
        c.px(0, 9, tone=1); c.px(9, 9, tone=1)
    elif c.sem in ('lit', 'dark', 'back'):
        c.hline(2, mat='accent', tone=3 if c.sem == 'lit' else 2)
        c.hline(7, d=-1)
    elif c.sem == 'top':
        c.vline(c.w // 2, mat='brass', tone=3)   # a brass ridge down the flat top


def brow_paint(c):
    P.plate('plate', marks=0)(c)
    P.rivets('brass', lambda c: [(1, 0), (c.w // 2, 0), (c.w - 2, 0)] if c.sem == 'front' else [])(c)


def cheek_paint(c):
    for layer in iron:
        layer(c)
    if c.sem in ('lit', 'dark', 'front'):
        P.rivets('brass', lambda c: [(0, 0), (0, c.h - 2)] if c.w >= 2 else [])(c)


# ---- chest -------------------------------------------------------------------

def cuirass_face(c):
    if c.sem == 'front':
        # rows of rivets down both edges, and the furnace grille on the plackart (rows 7-11), framed in brass
        for y in (1, 4):
            c.px(1, y, mat='brass', tone=4); c.px(c.w - 2, y, mat='brass', tone=3)
    elif c.sem in ('lit', 'dark'):
        c.vline(c.w // 2, 0, c.h - 1, d=-1)        # the side hinge
        c.px(c.w // 2, 2, mat='brass', tone=3); c.px(c.w // 2, 8, mat='brass', tone=2)
    elif c.sem == 'back':
        for y in (1, 6, 11):
            c.px(1, y, mat='brass', tone=2); c.px(c.w - 2, y, mat='brass', tone=2)


def grille_paint(c):
    P.trim('brass')(c)
    if c.sem == 'front':
        for y in (1, 3):
            for x in range(1, c.w - 1):
                c.px(x, y, mat='ember' if c.legendary else 'soot', tone=3 if c.legendary else (1 if x % 2 else 0))
        if c.legendary:
            c.px(c.w // 2, 1, mat='ember', tone=4)


def gorget_paint(c):
    P.plate('plate', marks=0)(c)
    P.edge('brass', ('top',))(c)


def slab_paint(c):
    for layer in iron:
        layer(c)
    P.edge('accent', ('bottom',))(c)
    if c.sem == 'top':
        P.rivets('brass', corners(1))(c)
    elif c.sem in ('front', 'back', 'lit', 'dark'):
        c.px(1, 0, mat='brass', tone=4 if c.sem in ('front', 'lit') else 2)
        c.px(c.w - 2, 0, mat='brass', tone=3 if c.sem in ('front', 'lit') else 2)


def drop_paint(c):
    for layer in iron:
        layer(c)
    P.band('accent', [-1])(c)
    out = (c.box.side == 'R' and c.face == 'east') or (c.box.side == 'L' and c.face == 'west')
    if out:
        P.rivets('brass', corners(1))(c)
        if c.legendary:
            for x in range(2, c.w - 2):
                c.px(x, 2, mat='ember', tone=3 if x % 2 else 4)


def tabard_paint(c):
    P.cloth('team', folds=True)(c)
    if c.sem == 'front':
        # an anvil in the cloth's own dark tones: horn to the left, the waist, the foot
        x0, y0 = (c.w - 5) // 2, 3
        for x in range(x0, x0 + 5):
            c.px(x, y0, tone=0)
        c.px(x0 - 1, y0, tone=1)
        c.px(x0 + 1, y0 + 1, tone=0); c.px(x0 + 2, y0 + 1, tone=0); c.px(x0 + 3, y0 + 1, tone=0)
        c.px(x0 + 2, y0 + 2, tone=0)
        for x in range(x0 + 1, x0 + 4):
            c.px(x, y0 + 3, tone=0)
        c.hline(c.h - 1, mat='brass', tone=2)
    if c.sem in ('front', 'back'):
        top = 2 if c.box.name == 'apron' else 0      # the apron's first rows sit behind the belt
        c.px(0, top, mat='brass', tone=4); c.px(c.w - 1, top, mat='brass', tone=3)   # the rings it hangs from


def gauntlet_paint(c):
    for layer in iron:
        layer(c)
    P.band('accent', [0])(c)
    if c.sem == 'front':
        c.hline(c.h - 1, mat='brass', tone=3)          # brass knuckle bar
        for x in range(0, c.w, 2):
            c.px(x, c.h - 1, mat='brass', tone=4)
    if c.sem == 'bottom':
        c.fill('soot', 1)


def stack_paint(c):
    P.plate('soot', marks=0)(c)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        for y in (1, 6, 11):
            c.hline(y, mat='brass', tone=3 if c.sem in ('front', 'lit') else 2)
    if c.sem == 'top':
        c.fill('ember', 4)


# ---- legs and boots ------------------------------------------------------------

def belt_paint(c):
    P.leather('leather')(c)
    if c.sem == 'front':
        c.rect(3, 0, 4, 2, mat='brass', tone=3)
        c.px(3, 0, mat='brass', tone=4); c.px(6, 1, mat='brass', tone=1)
        c.px(4, 1, mat='soot', tone=1); c.px(5, 1, mat='soot', tone=1)
    if c.sem in ('lit', 'dark', 'back'):
        c.hline(0, mat='accent', tone=3)


def skirt_paint(c):
    P.scales('plate')(c)
    P.edge('brass', ('bottom',))(c)


def knee_paint(c):
    for layer in iron:
        layer(c)
    if c.sem == 'front':
        c.px(1, 1, mat='brass', tone=4)
        c.px(2, 1, mat='brass', tone=2)


def boot_paint(c):
    for layer in iron:
        layer(c)
    if c.sem in ('front', 'lit', 'dark', 'back'):
        c.hline(0, mat='accent', tone=3 if c.sem in ('front', 'lit') else 2)
        c.hline(c.h - 1, mat='soot', tone=1)
    out = (c.box.side == 'R' and c.face == 'east') or (c.box.side == 'L' and c.face == 'west')
    if out:
        c.rect(2, 1, 2, 2, mat='brass', tone=3)        # the side buckle
        c.px(2, 1, mat='brass', tone=4)


def toe_paint(c):
    P.plate('plate', marks=0)(c)
    if c.sem == 'top':
        c.hline(c.h - 1, mat='accent', tone=3)


# ---- the set --------------------------------------------------------------------

def build():
    b = []
    b.append(Box('box_helm', 'helmet', 'head', (-5, 23, -5), (10, 10, 10), iron + [P.draw(helm_face)], hide={'down'},
                 note='flat-topped box helm, brass face-grille (ember-lit on Legendary), rarity band round the sides'))
    b.append(Box('brow', 'helmet', 'head', (-5.5, 30, -6), (11, 1, 2), [brow_paint], hide={'south'},
                 note='projecting brow over the grille, three brass rivets'))
    b += R(Box('cheek_r', 'helmet', 'head', (5, 23, -5.4), (1, 5, 6), [cheek_paint], side='R', hide={'west'},
               note='riveted cheek plate standing off the helm'))
    # chest
    b.append(Box('cuirass', 'chest', 'body', (-4.5, 11.5, -3), (9, 13, 6), iron + [P.draw(cuirass_face)],
                 note='riveted cuirass with a side hinge'))
    b.append(Box('grille', 'chest', 'body', (-2.5, 13.5, -3.5), (5, 5, 1), [grille_paint], hide={'south'},
                 note='furnace-grille plackart: two soot slots (ember on Legendary) in a brass frame'))
    b.append(Box('gorget', 'chest', 'body', (-4, 23, -3.5), (8, 2, 7), [gorget_paint], inflate=0.3,
                 note='gorget, seen only without the helm'))
    b.append(Box('tabard_back', 'chest', 'body', (-3, 6, 3.1), (6, 17, 1), [tabard_paint], hide={'north'},
                 note='team tabard down the back to the knee'))
    b += R(Box('slab_r', 'chest', 'right_arm', (3.5, 24.2, -4), (7, 2, 8), [slab_paint], inflate=0.1, side='R',
               note='slab pauldron: the flat slab'),
           Box('drop_r', 'chest', 'right_arm', (9, 19, -4), (2, 5, 8), [drop_paint], side='R', hide={'west'},
               note='slab pauldron: the plate down the outside, rivets, ember vents on Legendary'),
           Box('arm_r', 'chest', 'right_arm', (3.5, 11.5, -2.5), (5, 13, 5), iron + [riveted()], inflate=0.02,
               side='R', hide={'up'}),
           Box('gauntlet_r', 'chest', 'right_arm', (3, 11, -3), (6, 5, 6), [gauntlet_paint], inflate=0.15, side='R', hide={'up'},
               note='big square gauntlet, brass knuckle bar'))
    b += R(Box('stack_r', 'chest', 'body', (5.5, 19, 3.3), (2, 15, 2), [stack_paint], side='R', tier='legendary',
               note='Forgeheart smokestack behind the shoulder, ember mouth'))
    # legs
    b.append(Box('belt', 'legs', 'body', (-5, 10.6, -3.5), (10, 2, 7), [belt_paint]))
    b.append(Box('apron', 'legs', 'body', (-3, 3.5, -3.4), (6, 9, 1), [tabard_paint], hide={'south'},
                 note='team apron from the belt to below the knee, the anvil mark, brass rings and hem'))
    b += R(Box('leg_r', 'legs', 'right_leg', (-0.6, 2.5, -2.5), (5, 10, 5), iron, side='R', hide={'up', 'down'}),
           Box('scale_skirt_r', 'legs', 'right_leg', (-0.6, 6.5, -2.5), (5, 6, 5), [skirt_paint], inflate=0.3,
               side='R', hide={'up'}, note='knee-length scale skirt, split per leg, brass hem'),
           Box('knee_r', 'legs', 'right_leg', (-0.1, 4, -3.2), (4, 3, 1), [knee_paint], side='R', hide={'south'}))
    # boots
    b += R(Box('boot_r', 'boots', 'right_leg', (-0.9, 0, -3), (6, 4, 6), [boot_paint], side='R',
               note='square iron boot, brass side buckle'),
           Box('toe_r', 'boots', 'right_leg', (-0.4, 0, -4), (5, 2, 1), [toe_paint], side='R', hide={'south', 'down'}))
    return ArmourSet('forgewall', 'Forgewall', 'Ironclad', pal, b, __doc__)
