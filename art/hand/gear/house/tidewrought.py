"""Tidewrought: the Tidecallers' drowned marine harness.

Read: a Corinthian-cut bronze helm (T-shaped face opening, swept cheek pieces) under a finned crest webbed in team
cloth; bronze scale mail with a scallop-shell pectoral; scallop-shell pauldrons; kelp trailing from the forearms; a team
sea-cloak with a wave-cut hem and a team sash knotted at the left hip; navy breeches, bronze greaves with shell knee
cops and finned bronze boots. Verdigris pools in the recesses and barnacles crust the lower edges. Legendary (Abyssal):
abyss-dark bronze, a taller crest with an ember spine, ember eyes, an ember pearl in the shell, bleached coral branching
from both shoulders with ember polyps, longer kelp.
"""
from . import paint as P
from .common import R, palette
from .engine import ArmourSet, Box

SPEC = {
    'plate': {'common': '#72502A', 'uncommon': '#72502A', 'rare': '#4F6E8E', 'epic': '#45374F',
              'legendary': '#23393D'},
    'accent': '#B39A5E',                      # Common trim: a polished bronze rim
    'mats': {'patina': '#4E7A6A', 'barnacle': ('#CFCBBE', 0.16), 'navy': '#1E2C44', 'kelp': '#3E5A2E',
             'shell': ('#C9B79A', 0.16)},
}


def pal(rarity, team):
    return palette(SPEC, rarity, team)


bronze = [P.plate('plate', marks=0.02), P.patina('patina', 0.035), P.barnacles('barnacle', 1)]


# ---- helmet ------------------------------------------------------------------

def helm_face(c):
    if c.sem == 'front':
        eye = 'ember' if c.legendary else 'void'
        c.hline(3, 1, 3, d=1); c.hline(3, 6, 8, d=1)                 # brow arcs
        c.hline(4, 1, 3, mat=eye, tone=3 if c.legendary else 0)
        c.hline(4, 6, 8, mat=eye, tone=3 if c.legendary else 0)
        c.px(1, 4, mat='void', tone=0); c.px(8, 4, mat='void', tone=0)
        c.vline(4, 4, 5, tone=4); c.vline(5, 4, 5, tone=2)             # the nasal
        for y in range(6, 10):                                         # the gap between the cheek pieces
            c.px(4, y, mat='void', tone=0); c.px(5, y, mat='void', tone=0)
        c.vline(3, 6, 9, tone=1); c.vline(6, 6, 9, tone=2)             # cheek-piece edges
        c.px(2, 9, tone=1); c.px(7, 9, tone=1)
    elif c.sem in ('lit', 'dark'):
        # the cheek piece sweeps back: a seam curving from brow to nape
        for x, y in ((3, 4), (4, 5), (5, 6), (6, 7), (7, 8)):
            xx = x if c.sem == 'lit' else c.w - 1 - x
            c.px(xx, y, d=-1)
        P.band('accent', [2])(c)
    elif c.sem == 'back':
        P.band('accent', [2])(c)
        c.hline(8, d=-1)


def fin_paint(c):
    c.fill('team', 2 if c.sem in ('lit', 'front', 'top') else 1)
    if c.sem in ('lit', 'dark'):
        # bronze spines through team webbing, the spine tops in the rarity colour
        for x in range(0, c.w, 2):
            c.vline(x, 0, c.h - 1, mat='plate', tone=3 if c.sem == 'lit' else 2)
            c.px(x, 0, mat='accent', tone=3)
        c.hline(c.h - 1, mat='plate', tone=1)
    elif c.sem == 'top':
        for y in range(0, c.h, 2):
            c.px(0, y, mat='accent', tone=3)
    if c.legendary and c.sem in ('lit', 'dark', 'top'):
        for x in range(1, c.w, 4):
            c.px(x if c.sem != 'top' else 0, 0 if c.sem != 'top' else x, mat='ember', tone=4)


def spine_paint(c):
    P.plate('plate', marks=0)(c)
    if c.sem in ('back', 'lit', 'dark'):
        for y in range(0, c.h, 2):
            c.px(0, y, mat='accent', tone=3)


# ---- chest -------------------------------------------------------------------

def scallop(c, mat='shell'):
    """A scallop: ridges fanning from the hinge at the bottom middle, the top edge scalloped."""
    t = 3 if c.sem in ('front', 'lit', 'top') else 2
    c.fill(mat, t)
    if c.sem in ('front', 'back', 'lit', 'dark') and c.w >= 3:
        for x in range(c.w):
            if x % 2 == 1:
                c.vline(x, 0, c.h - 1, tone=t - 1)
            else:
                c.px(x, 0, tone=min(4, t + 1))
        c.clear(0, 0); c.clear(c.w - 1, 0)
        c.hline(c.h - 1, tone=t - 1)


def pectoral_paint(c):
    scallop(c)
    if c.sem == 'front':
        c.px(2, 4, mat='ember' if c.legendary else 'shell', tone=4 if c.legendary else 1)   # the hinge / the pearl
        c.hline(c.h - 1, 1, 3, mat='accent', tone=3)


def cuirass_face(c):
    if c.sem in ('top', 'bottom'):
        return
    if c.sem == 'front':
        c.hline(c.h - 1, mat='accent', tone=3)
    # verdigris only where water sits: the lowest laps of the scales
    for x in range(c.w):
        if c.rng.random() < 0.35:
            c.px(x, c.h - 2, mat='patina', tone=1)


def pauldron_paint(c):
    scallop(c)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        c.hline(c.h - 1, mat='accent', tone=3 if c.sem in ('front', 'lit') else 2)
    P.barnacles('barnacle', 2)(c)


def arm_face(c):
    if c.sem in ('top', 'bottom'):
        return
    # top 24.5: rows 0-5 scales (under the pauldron), 6-12 a bronze vambrace with a rarity cuff
    t = c.base
    for y in range(6, 13):
        for x in range(c.w):
            c.px(x, y, mat='plate', tone=max(1, min(3, t)))
    c.hline(6, mat='accent', tone=3 if c.sem in ('front', 'lit') else 2)
    if c.sem in ('front', 'lit'):
        c.vline(1, 7, 11, d=1)
    c.hline(12, d=-1)
    P.patina('patina', 0.03)(c)


def kelp_paint(c):
    c.fill('kelp', 2 if c.sem in ('front', 'lit') else 1)
    for y in range(c.h):
        if c.rng.random() < 0.35:
            c.px(0 if c.w == 1 else c.rng.randrange(c.w), y, d=1)
    c.px(0, c.h - 1, d=-1)


def cloak_paint(c):
    P.cloth('team', hem=False)(c)
    if c.sem in ('front', 'back'):
        # a wave-cut hem: crests and troughs, the crest lit
        for x in range(c.w):
            k = (0, 1, 2, 2, 1)[x % 5]
            for y in range(c.h - k, c.h):
                c.clear(x, y)
            c.px(x, c.h - k - 1, d=1 if k == 0 else -1)


def sash_paint(c):
    P.cloth('team', folds=False, hem=False)(c)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        c.hline(0, d=1)
        for x in range(0, c.w, 3):
            c.px(x, 1, d=-1)


def tail_paint(c):
    P.cloth('team', ragged=1)(c)


def coral_paint(c):
    c.fill('barnacle', 3 if c.sem in ('front', 'lit', 'top') else 2)
    if c.sem in ('front', 'lit') and c.h >= 2:
        c.px(0, c.h - 1, tone=1)


def coral_tip(c):
    coral_paint(c)
    if c.sem != 'bottom':
        c.px(0, 0, mat='ember', tone=4)


# ---- legs and boots ------------------------------------------------------------

def greave_paint(c):
    P.plate('plate', marks=0.02)(c)
    P.patina('patina', 0.03)(c)
    P.band('accent', [0])(c)


def knee_paint(c):
    scallop(c)


def tasset_paint(c):
    P.scales('plate')(c)
    P.edge('accent', ('bottom',))(c)


def boot_face(c):
    if c.sem in ('front', 'lit', 'dark', 'back'):
        c.hline(c.h - 1, mat='plate', tone=0)
        c.hline(0, d=1)


def boot_fin(c):
    c.fill('plate', 2)
    if c.sem in ('lit', 'dark'):
        for x in range(0, c.w, 1):
            c.vline(x, 0, c.h - 1, tone=3 if x % 2 == 0 else 1)
        c.px(0, 0, mat='accent', tone=3)


# ---- the set --------------------------------------------------------------------

def build():
    b = []
    b.append(Box('corinthian', 'helmet', 'head', (-5, 23, -5), (10, 10, 10), bronze + [P.draw(helm_face)],
                 hide={'down'}, note='Corinthian-cut bronze helm: T opening, swept cheek pieces, rarity band'))
    b.append(Box('fin', 'helmet', 'head', (-0.5, 33, -5), (1, 3, 11), [fin_paint], hide={'down'},
                 note='fin crest: bronze spines webbed in team cloth, rarity spine tips'))
    b.append(Box('fin_top', 'helmet', 'head', (-0.5, 36, -3), (1, 2, 8), [fin_paint], hide={'down'}))
    b.append(Box('fin_tall', 'helmet', 'head', (-0.5, 38, -1.5), (1, 2, 5), [fin_paint], tier='legendary',
                 hide={'down'}))
    b.append(Box('nape_spine', 'helmet', 'head', (-0.5, 26, 5), (1, 6, 1), [spine_paint], hide={'north'},
                 note='the crest runs on down the nape'))
    # chest
    b.append(Box('scale_coat', 'chest', 'body', (-4.5, 11.5, -3), (9, 13, 6), [P.scales('plate'),
                 P.draw(cuirass_face)], note='bronze scale coat, verdigris in the laps'))
    b.append(Box('pectoral', 'chest', 'body', (-2.5, 16.5, -3.5), (5, 5, 1), [pectoral_paint], hide={'south'},
                 note='scallop-shell pectoral (an ember pearl at its hinge on Legendary)'))
    b.append(Box('cloak', 'chest', 'body', (-4.5, 6, 3.1), (9, 17, 1), [cloak_paint], tier='base',
                 note='team sea-cloak with a wave-cut hem'))
    b.append(Box('cloak_long', 'chest', 'body', (-4.5, 2, 3.1), (9, 21, 1), [cloak_paint], tier='legendary'))
    b += R(Box('shell_r', 'chest', 'right_arm', (4.5, 21.2, -3.5), (6, 3, 7), [pauldron_paint], side='R',
               note='scallop-shell pauldron, rarity rim, barnacles'),
           Box('shell_hump_r', 'chest', 'right_arm', (5.5, 24.2, -2.5), (4, 1, 5), [pauldron_paint], side='R',
               hide={'down'}),
           Box('arm_r', 'chest', 'right_arm', (3.5, 11.5, -2.5), (5, 13, 5), [P.scales('plate'), P.draw(arm_face)],
               inflate=0.02, side='R', hide={'up'}, note='scale sleeve, bronze vambrace'),
           Box('kelp_a_r', 'chest', 'right_arm', (8.55, 8, -1.5), (1, 6, 1), [kelp_paint], side='R', tier='base',
               note='kelp trailing from the forearm'),
           Box('kelp_b_r', 'chest', 'right_arm', (8.55, 9.5, 0.4), (1, 5, 1), [kelp_paint], side='R', tier='base'),
           Box('kelp_la_r', 'chest', 'right_arm', (8.55, 5, -1.5), (1, 9, 1), [kelp_paint], side='R',
               tier='legendary'),
           Box('kelp_lb_r', 'chest', 'right_arm', (8.55, 7.5, 0.4), (1, 7, 1), [kelp_paint], side='R',
               tier='legendary'))
    # Abyssal: bleached coral from each shoulder, ember polyps
    b += R(Box('coral_stem_r', 'chest', 'right_arm', (6.5, 25.2, -0.5), (1, 3, 1), [coral_paint], side='R',
               tier='legendary', hide={'down'}),
           Box('coral_out_r', 'chest', 'right_arm', (7.5, 27, -0.5), (1, 3, 1), [coral_tip], side='R',
               tier='legendary'),
           Box('coral_in_r', 'chest', 'right_arm', (5.5, 27.5, -0.5), (1, 2, 1), [coral_tip], side='R',
               tier='legendary'),
           Box('coral_back_r', 'chest', 'right_arm', (6.5, 27, 0.5), (1, 2, 1), [coral_tip], side='R',
               tier='legendary'))
    # legs
    b.append(Box('sash', 'legs', 'body', (-5, 10.6, -3.5), (10, 2, 7), [sash_paint], note='team sash'))
    b.append(Box('sash_knot', 'legs', 'body', (-5.6, 8.6, -2), (1, 3, 2), [sash_paint], hide={'east'},
                 note='knotted at the left hip'))
    b.append(Box('sash_tail', 'legs', 'body', (-5.5, 4.6, -1.5), (1, 4, 1), [tail_paint], hide={'east'}))
    b += R(Box('leg_r', 'legs', 'right_leg', (-0.6, 2.5, -2.5), (5, 10, 5), [P.cloth('navy')], side='R',
               hide={'up', 'down'}, note='navy breeches'),
           Box('greave_r', 'legs', 'right_leg', (-0.6, 3.5, -2.5), (5, 4, 5), [greave_paint], inflate=0.2, side='R',
               hide={'up', 'down'}),
           Box('knee_r', 'legs', 'right_leg', (0.4, 6.5, -3.2), (3, 2, 1), [knee_paint], side='R', hide={'south'},
               note='shell knee cop'),
           Box('tasset_r', 'legs', 'right_leg', (-0.4, 8.5, -2.9), (5, 3, 1), [tasset_paint], side='R',
               hide={'south'}))
    # boots
    b += R(Box('boot_r', 'boots', 'right_leg', (-0.6, 0.25, -2.5), (5, 3, 5), bronze + [P.draw(boot_face)],
               inflate=0.25, side='R'),
           Box('boot_fin_r', 'boots', 'right_leg', (1.4, 1, 2.75), (1, 4, 2), [boot_fin], side='R', hide={'north'},
               note='calf fin'))
    return ArmourSet('tidewrought', 'Tidewrought', 'Tidecallers', pal, b, __doc__)
