"""Thornhide: the Wildmoot hunt-leader's harness.

Read: a fur hood with a bone boar-snout and tusks, two pricked ears and a three-feather fan on the left of the head;
hide lamellar laced in rawhide under a ragged fur collar; a lamellar pad on the right shoulder and a whole pelt, paw and
claws hanging, on the left; a fang necklace, a team cloak and a team loincloth; wrapped shins and fur-cuffed boots.
Ochre clay warpaint across the brow and under the right eye. Legendary (Antlerking): charred hide, branching antlers
with ember tips, ember warpaint and eyes, bone spikes on the pad, longer tusks.
"""
from . import paint as P
from .common import R, palette
from .engine import ArmourSet, Box

SPEC = {
    'plate': {'common': '#8C6440', 'uncommon': '#8C6440', 'rare': '#4A6A88', 'epic': '#45365A',
              'legendary': '#3A2E26'},
    'accent': '#A88A5E',                      # Common trim: rawhide lacing
    'mats': {'fur': ('#7A4127', 0.20), 'bark': '#4A3626', 'bone': ('#C9C1A8', 0.16), 'clay': ('#C8B48A', 0.16),
             'leather': '#5A3E2C', 'wrap': ('#8B7B5E', 0.18), 'feather': ('#D8CFB8', 0.14)},
}


def pal(rarity, team):
    return palette(SPEC, rarity, team)


def lamellar(mat='plate'):
    """Hide lamellar: rows three texels tall of small plates two texels wide, staggered a texel row to row. Each plate is
    lit on its left column and dark along its right seam; each row hangs over the next (a dark lip); rawhide lacing
    shows only at the row seams, at every other plate."""
    def run(c):
        t = max(1, min(3, c.base))
        c.fill(mat, t)
        if c.sem in ('top', 'bottom'):
            return
        for y in range(c.h):
            band, sub = divmod(y, 3)
            off = band % 2
            for x in range(c.w):
                k = (x + off) % 2
                if sub == 2:
                    c.px(x, y, tone=t - 1)                 # the lip of the row above the next
                elif k == 1:
                    c.px(x, y, tone=t - 1 if sub == 1 else t)   # the seam between plates
                elif sub == 0 and c.sem in ('front', 'lit'):
                    c.px(x, y, tone=t + 1)                 # light catches the plate's top
            for x in range(c.w):
                if sub == 1 and (x + off + band) % 4 == 1:
                    c.px(x, y, mat='accent', tone=t)       # a lace through the seam
        if c.sem in ('front', 'lit'):
            c.hline(0, d=1)
    return run


# ---- helmet ------------------------------------------------------------------

def hood_paint(c):
    P.fur('fur', ragged=0)(c)
    if c.sem == 'front':
        # rows 0-2 fur brim (ragged into the mask), rows 3-9 a dark hide mask the boar-snout is strapped over
        for y in range(3, c.h):
            for x in range(c.w):
                c.px(x, y, mat='bark', tone=2 if x > 0 else 3)
        c.hline(c.h - 1, mat='bark', tone=1)
        for x in range(c.w):
            if c.rng.random() < 0.5:
                c.px(x, 3, mat='fur', tone=1)
        paint = 'ember' if c.legendary else 'clay'
        # brow stripe (broken over the nose) and three claw-stripes under the right eye
        for x in (1, 2, 3, 6, 7, 8):
            c.px(x, 4, mat=paint, tone=3)
        c.hline(5, 2, 3, mat='void', tone=0)
        c.hline(5, 6, 7, mat='void', tone=0)
        if c.legendary:
            c.px(3, 5, mat='ember', tone=4); c.px(6, 5, mat='ember', tone=4)
        for x, y0, y1 in ((1, 6, 8), (2, 6, 7)):
            c.vline(x, y0, y1, mat=paint, tone=2)
        c.vline(8, 6, 7, mat=paint, tone=2)
    elif c.sem in ('lit', 'dark', 'back'):
        for x in range(c.w):       # the fur hangs ragged at the shoulders
            if c.rng.random() < 0.5:
                c.clear(x, c.h - 1)


def snout_paint(c):
    P.bone('bone', crack=False)(c)
    if c.sem == 'front':
        # the boar's disc: dark hide with two nostrils, the bone around it
        c.hline(0, tone=4)
        c.rect(0, 1, 4, 2, mat='leather', tone=2)
        c.px(1, 1, mat='void', tone=0); c.px(2, 1, mat='void', tone=0)
        c.px(0, 1, mat='leather', tone=3)
    elif c.sem in ('lit', 'dark'):
        for x in range(0, c.w):
            c.px(x, c.h - 1, tone=4 if x % 2 == 0 else 1)   # teeth along the jaw
    elif c.sem == 'top':
        c.hline(c.h - 1, tone=4)


def tusk_paint(c):
    c.fill('bone', 4 if c.sem in ('front', 'lit', 'top') else 2)


def ear_paint(c):
    P.fur('fur', ragged=0)(c)
    if c.sem == 'front':
        c.vline(c.w // 2, 1, c.h - 1, mat='leather', tone=3)


def feather_paint(c):
    c.fill('feather', 3 if c.sem in ('front', 'lit', 'top') else 2)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        c.hline(0, mat='bark', tone=2)
        c.hline(1, mat='bark', tone=1)
        c.hline(3, mat='bark', tone=2)
        c.px(0, c.h - 1, mat='bark', tone=1)     # the quill


# ---- chest -------------------------------------------------------------------

def cuirass_face(c):
    if c.sem == 'front':
        # fang necklace on a sinew cord: three fangs and a big claw in the middle
        c.hline(1, 1, 7, mat='leather', tone=2)
        for x in (2, 6):
            c.px(x, 2, mat='bone', tone=4); c.px(x, 3, mat='bone', tone=2)
        c.px(4, 2, mat='bone', tone=4); c.px(4, 3, mat='bone', tone=3); c.px(4, 4, mat='bone', tone=1)
        if c.legendary:
            c.px(4, 2, mat='ember', tone=4)


def mantle_paint(c):
    P.fur('fur', ragged=2)(c)


def cloak_paint(c):
    P.cloth('team', ragged=3)(c)


def pad_paint(c):
    lamellar()(c)
    if c.sem in ('front', 'lit', 'dark', 'back'):
        P.edge('fur', ('bottom',))(c)


def pelt_paint(c):
    P.fur('fur', ragged=2)(c)
    if c.sem == 'top':
        c.hline(c.h // 2, tone=1)        # the spine line of the pelt


def paw_paint(c):
    P.fur('fur', ragged=0)(c)
    if c.sem == 'front':
        for x in range(c.w):
            c.px(x, c.h - 1, mat='bone', tone=4 if x % 2 == 0 else 2)   # claws


def bracer_paint(c):
    lamellar()(c)
    out = (c.box.side == 'R' and c.face == 'east') or (c.box.side == 'L' and c.face == 'west')
    if out and c.h >= 4:
        # crossed rawhide lacing on the outer face
        for i in range(c.h - 1):
            c.px(1 + (i % 3), i, mat='accent', tone=3)
            c.px(3 - (i % 3), i, mat='accent', tone=2)


def arm_face(c):
    if c.sem in ('top', 'bottom'):
        return
    # top 24.5: hand wraps on the last three rows
    for y in (10, 11, 12):
        c.hline(y, mat='wrap', tone=2 if c.sem in ('front', 'lit') else 1)
    c.hline(11, d=1)


# ---- legs and boots ------------------------------------------------------------

def belt_paint(c):
    P.leather('leather')(c)
    if c.sem == 'front':
        c.px(4, 0, mat='bone', tone=4); c.px(5, 0, mat='bone', tone=3)     # bone toggle
        c.px(4, 1, mat='bone', tone=2)
    if c.sem in ('front', 'lit', 'dark', 'back'):
        for x in (1, 7, 8) if c.sem == 'front' else (1, c.w - 2):
            if 0 <= x < c.w:
                c.px(x, 1, mat='accent', tone=3)


def loin_paint(c):
    P.cloth('team', ragged=2)(c)
    if c.sem == 'front':
        # a tusk chevron in the cloth's dark tone (the team's own mark of the Moot)
        for x, y in ((0, 1), (1, 2), (2, 2), (3, 1)):
            c.px(x, y, tone=0)


def leg_face(c):
    if c.sem in ('top', 'bottom'):
        return
    # top 12.5: shin wraps from row 5 down, diagonal bands
    for y in range(5, 10):
        for x in range(c.w):
            c.px(x, y, mat='wrap', tone=(2 if c.sem in ('front', 'lit') else 1) + (1 if (x + y) % 3 == 0 else 0))
            if (x + y) % 3 == 2:
                c.px(x, y, d=-1)


def boot_face(c):
    if c.sem in ('front', 'lit', 'dark', 'back'):
        c.hline(1, mat='wrap', tone=2)
        c.hline(3, mat='bark', tone=1)
        if c.sem == 'front':
            c.px(1, 1, mat='accent', tone=3); c.px(3, 1, mat='accent', tone=3)


def antler_paint(c):
    c.fill('bone', 3 if c.sem in ('front', 'lit', 'top') else 2)
    if c.sem in ('front', 'lit', 'back', 'dark') and c.h >= 3:
        c.px(0, c.h - 1, tone=1)


def antler_tip(c):
    antler_paint(c)
    if c.sem != 'bottom':
        c.px(0, 0, mat='ember', tone=4)


# ---- the set --------------------------------------------------------------------

def build():
    b = []
    b.append(Box('hood', 'helmet', 'head', (-5, 23, -5), (10, 10, 10), [hood_paint], hide={'down'},
                 note='fur hood over a hide mask: clay brow stripe, claw stripes under the right eye, a scar'))
    b.append(Box('snout', 'helmet', 'head', (-2, 24, -7), (4, 3, 2), [snout_paint], hide={'south'},
                 note='bone boar-snout over the mouth'))
    b += R(Box('tusk_r', 'helmet', 'head', (2, 24.5, -7), (1, 2, 1), [tusk_paint], side='R', hide={'west'}),
           Box('tusk_tip_r', 'helmet', 'head', (3, 26, -7.2), (1, 1, 1), [tusk_paint], side='R'),
           Box('tusk_long_r', 'helmet', 'head', (3, 27, -7.2), (1, 2, 1), [tusk_paint], side='R', tier='legendary'),
           Box('tusk_up_r', 'helmet', 'head', (3, 27, -7.2), (1, 1, 1), [tusk_paint], side='R', tier='base'),
           Box('ear_r', 'helmet', 'head', (2.5, 32.5, -1), (2, 2, 1), [ear_paint], rot=(0, 0, -22.5),
               origin=(3.5, 32.5, -0.5), side='R', hide={'down'}, note='pricked ear'),
           Box('ear_tip_r', 'helmet', 'head', (3, 34.5, -1), (1, 1, 1), [ear_paint], rot=(0, 0, -22.5),
               origin=(3.5, 32.5, -0.5), side='R', hide={'down'}))
    for i, (ang, z) in enumerate(((10, 0.5), (32.5, 1.5), (55, 2.5))):
        b.append(Box('feather_%d' % i, 'helmet', 'head', (-6, 28, z), (1, 5, 1), [feather_paint],
                     rot=(0, 0, ang), origin=(-5.5, 28, z + 0.5), side='L', note='feather fan, left of the head'))
    # Antlerking: two branching antlers, ember at every tip
    b += R(Box('antler_base_r', 'helmet', 'head', (4.6, 31, -0.5), (1, 3, 1), [antler_paint], side='R',
               tier='legendary'),
           Box('antler_beam_r', 'helmet', 'head', (5.6, 33, -0.5), (2, 1, 1), [antler_paint], side='R',
               tier='legendary'),
           Box('antler_tine1_r', 'helmet', 'head', (5.8, 34, -0.5), (1, 2, 1), [antler_tip], side='R',
               tier='legendary'),
           Box('antler_main_r', 'helmet', 'head', (7.6, 33, -0.5), (1, 4, 1), [antler_tip], side='R',
               tier='legendary'),
           Box('antler_fore_r', 'helmet', 'head', (6.6, 33.5, -1.5), (1, 1, 1), [antler_tip], side='R',
               tier='legendary'),
           Box('antler_out_r', 'helmet', 'head', (8.6, 35, -0.5), (1, 1, 1), [antler_tip], side='R',
               tier='legendary'))
    # chest
    b.append(Box('jerkin', 'chest', 'body', (-4.5, 11.5, -3), (9, 13, 6), [lamellar(), P.draw(cuirass_face)],
                 note='hide lamellar laced in rawhide (the lacing is the rarity colour), fang necklace'))
    b.append(Box('fur_collar', 'chest', 'body', (-6, 20.6, -4.5), (12, 4, 9), [mantle_paint],
                 note='ragged fur collar over both shoulders'))
    b.append(Box('cloak', 'chest', 'body', (-4.5, 7, 3.1), (9, 14, 1), [cloak_paint], tier='base',
                 note='team cloak, torn hem'))
    b.append(Box('cloak_long', 'chest', 'body', (-4.5, 3, 3.1), (9, 18, 1), [cloak_paint], tier='legendary'))
    b.append(Box('pad', 'chest', 'right_arm', (4.5, 20.3, -3), (6, 4, 6), [pad_paint], side='R',
                 note='lamellar shoulder pad, fur-trimmed'))
    b.append(Box('pad_spike_a', 'chest', 'right_arm', (6, 24.3, -1.5), (1, 2, 1), [antler_paint], side='R',
                 tier='legendary', hide={'down'}))
    b.append(Box('pad_spike_b', 'chest', 'right_arm', (8, 24.3, 0.5), (1, 3, 1), [antler_tip], side='R',
                 tier='legendary', hide={'down'}))
    b.append(Box('pelt', 'chest', 'left_arm', (-10.5, 19.8, -3.5), (6, 5, 7), [pelt_paint], side='L',
                 note='a whole pelt over the left shoulder'))
    b.append(Box('paw', 'chest', 'left_arm', (-9.5, 16.8, -3.9), (2, 3, 1), [paw_paint], side='L', hide={'south'},
                 note='the pelt\'s forepaw hangs at the front, bone claws'))
    b += R(Box('arm_r', 'chest', 'right_arm', (3.5, 11.5, -2.5), (5, 13, 5), [P.leather('leather'), P.draw(arm_face)],
               inflate=0.02, side='R', hide={'up'}, note='leather sleeve, hand wraps'),
           Box('bracer_r', 'chest', 'right_arm', (3.5, 12, -2.5), (5, 5, 5), [bracer_paint], inflate=0.22, side='R',
               hide={'up', 'down'}, note='lamellar bracer, crossed lacing outside'))
    # legs
    b.append(Box('belt', 'legs', 'body', (-5, 10.5, -3.5), (10, 2, 7), [belt_paint]))
    b.append(Box('loincloth', 'legs', 'body', (-2, 3.5, -3.8), (4, 7, 1), [loin_paint], hide={'south'},
                 note='team loincloth with the tusk chevron'))
    b += R(Box('leg_r', 'legs', 'right_leg', (-0.6, 2.5, -2.5), (5, 10, 5), [P.leather('leather'), P.draw(leg_face)],
               side='R', hide={'up', 'down'}, note='hide trousers, wrapped shins'),
           Box('thigh_r', 'legs', 'right_leg', (-0.4, 7.5, -2.9), (5, 4, 1), [lamellar()], side='R', hide={'south'}))
    # boots
    b += R(Box('boot_r', 'boots', 'right_leg', (-0.6, 0.25, -2.5), (5, 4, 5), [P.leather('leather'), P.draw(boot_face)],
               inflate=0.25, side='R'),
           Box('cuff_r', 'boots', 'right_leg', (-0.6, 3.6, -2.5), (5, 2, 5), [P.fur('fur', ragged=1)], inflate=0.45,
               side='R', note='fur boot cuff'))
    return ArmourSet('thornhide', 'Thornhide', 'Wildmoot', pal, b, __doc__)
