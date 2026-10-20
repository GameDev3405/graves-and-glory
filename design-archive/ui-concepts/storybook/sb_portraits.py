"""The company's portraits, posed and lit as the War Table's live-model portraits are (v4a_portrait: the biped
chest-up at yaw 30, the camera 5 degrees below the eyes, 20 model px tall in the window), in the unit identity's
looks for each companion's troop type:

  Maud    Templar (Gilded Rare infantry), helm off by the fire: forties, dark hair going grey, a scar through
          one eyebrow; white plate, the knee tabard with its white cross
  Wren    Bone Archer: moss rag hood over a bare skull, a skull pauldron on the bow shoulder, the bone quiver
  Dunmar  Shieldbreaker (a vindicator's head): shaved and tattooed scalp, black beard, the anvil pauldron on the
          right shoulder, bare tattooed arms, chain belt, a few ember pixels
  Ashka   Hide Hunter, on its boar-kin model (story spec cast table): snout and tusks, ears hanging from the
          crown, russet fur with no rot, leather cap, clay stripes across the brow, the three-feather fan on
          the left of her head, a hide cloak

Team cloth is linen in portraits, as on the War Table.

python sb_portraits.py [name ...]   writes art/<name>.png"""
import sys
from sb_common import *
import v4a_portrait as VP
from v4a_portrait import Box, Tex, Plane, biped, render, GILD, mail, from_view

ASSETS = {}

P = dict(GILD)
P.update({
    # Maud: dark hair going grey, the scar paler than her skin, her brows
    'R': '#3b2b20', 'g': '#9a9288', 'x': '#f3dcc6', 'c': '#2a1f18',
    # bone and moss (Wren)
    'C': '#c9c1a8', 'a': '#a59d86', 'A': '#7f7865', 'o': '#4f5a3c', 'i': '#3a4430', 'j': '#66724e',
    # soot iron, brass, ember, the illager's grey skin, tattoo ink, black beard (Dunmar)
    'I': '#34373b', 't': '#4a4e53', 'Y': '#7f6232', 'Z': '#a58340', 'u': '#e2702a', 'v': '#ffb15c',
    'd': '#b3a796', '1': '#958a79', '2': '#5a5146', '3': '#37465c', '4': '#1b1612', 'e': '#2c4a2c',
    # russet, bark, clay, feathers, Ashka's skin and hair
    '5': '#7a4127', '6': '#5b2f1c', '7': '#4a3626', '8': '#c8b48a', '9': '#efe7d6', '0': '#2f1f14',
    '!': '#d9a07c', '@': '#b77f5f', '#': '#8c5a41', '$': '#6b3a25', '%': '#3d6b3a',
})


def faces(d):
    return {k: Tex(v, P) for k, v in d.items()} if d else {}


# ---------------------------------------------------------------- Maud
MAUD_HEAD = {
    'front': ['RRRRRRRR',
              'RgRRRRRR',
              'gKKKKxKR',
              'RccKcxcR',
              'RwpKKpwR',
              'RKKKKKKR',
              'RKDDDDKR',
              'RkKKKKkR'],
    'right': ['RRRRRRRR',
              'RRRRRRgR',
              'RRRRRgKK',
              'RRRRgKKc',
              'RRRkkKKK',
              'RRRkDKKk',
              'RRRRKKKK',
              'RRRXXKKX'],
    'left': ['RRRRRRRR', 'RgRRRRRR', 'KKgRRRRR', 'cKKgRRRR', 'KKKkkRRR', 'kKKDkRRR', 'KKKKRRRR', 'XKKXXRRR'],
    'back': ['RRRRRRRR', 'RRRgRRRR', 'RRRRRRRR', 'RRRRRgRR', 'RRRRRRRR', 'RRRRRRRR', 'RRRRRRRR', 'XRRRRRRX'],
    'top': ['RRRRRRRR', 'RRgRRRRR', 'RRRgRRRR', 'RRRRRRRR', 'RRRRRgRR', 'RRRRRRRR', 'RRRRRRRR', 'RRRRRRRR'],
    'bottom': ['XXXXXXXX'] * 8,
}


def maud():
    boxes, planes = VP.templar()
    boxes = list(boxes)
    boxes[0] = Box(-4, 24, -4, 4, 32, 4, faces(MAUD_HEAD))
    del boxes[4]                                  # the helm is off
    # a short tail of hair at the nape
    tail = {'front': ['RR', 'Rg', 'RR'], 'back': ['RR', 'gR', 'RR'], 'right': ['RR', 'RR', 'RR'],
            'left': ['RR', 'RR', 'RR'], 'top': ['RR', 'RR'], 'bottom': ['RR', 'RR']}
    boxes.append(Box(-1, 23.5, -5.6, 1, 26.5, -4, faces(tail)))
    # her tabard in the company's navy, as she wears it in the world (the portrait panels are linen for units
    # without a team; Maud is a person and has one)
    navy = {'P': '#2a3747', 'Q': '#1d2633'}
    for b_ in boxes[1:]:
        for k_, t_ in list(b_.faces.items()):
            if any(ch in 'PQ' for row in t_.rows for ch in row):
                b_.faces[k_] = Tex(t_.rows, dict(t_.pal, **navy))
    return boxes, planes


# ---------------------------------------------------------------- Wren
WREN_SKULL = {
    'front': ['aCCCCCCa',
              'CCCCCCCa',
              'CqqCCqqa',
              'CqqCCqqa',
              'CCCqqCCa',
              'aCCqqCCA',
              'aqCqCqCA',
              'AaCaCaAA'],
    'right': ['aaCCCCCC', 'aCCCCCCC', 'aaCCCCqq', 'aaCCCCqq', 'AaaCCCCC', 'AaaaCCCC', 'AAaaaCqC', 'AAAaaaCa'],
    'left': ['CCCCCCaa'] * 4 + ['CCCCCaaA'] * 4, 'back': ['aaaaaaaa'] * 8, 'top': ['CCCCCCCC'] * 8,
    'bottom': ['AAAAAAAA'] * 8,
}
WREN_HOOD = {
    'front': ['jooooooi',
              'ooooooio',
              'oi....oi',
              'o......i',
              'o......o',
              'i......i',
              'o......i',
              'oi....io'],
    'right': ['iooooojj', 'ioooooojo', 'iooiooooo', 'iioooooo', 'ioooiooo', 'iiooooio', 'i.ioooo.', '.i.io.i.'],
    'left': ['jjooooi', 'ojoooooi', 'oooooioi', 'oooooiii', 'oooiooii', 'oiooooii', '.ooooi.i', '.i.oi.i.'],
    'back': ['oooooooi', 'ooiooooi', 'oooooioi', 'oiooooii', 'ooooioii', 'ioooooii', 'oioooi.i', '.i.oi.i.'],
    'top': ['jjjooooo', 'jooooooi', 'ooooooii', 'oooioooi', 'ooooooii', 'oooooiii', 'ooooiiii', 'oiiiiiii'],
    'bottom': ['........'] * 8,
}
# the hood rows above are typed by hand at 8 wide; normalise any stray width so a slip cannot crash the render
for _k, _v in WREN_HOOD.items():
    WREN_HOOD[_k] = [(r + '........')[:8] for r in _v]


def wren():
    jerk = {'front': ['iooooooi', 'oPPPPPPo', 'oPPPPPPo', 'oPQPPQPo', 'oPPPPPPo', 'oQPPPPQo', 'bBBVBBBb',
                      'aCaCCaCa', 'CaCaaCaC', 'aCaCCaCa', 'CqCqqCqC', 'qCqCCqCq'],
            'right': ['iooo', 'oPPP', 'oPPP', 'oPPQ', 'oPPP', 'oQPP', 'bBBB', 'aCaC', 'CaCa', 'aCaC', 'CqCq', 'qCqC'],
            'left': ['oooi', 'PPPo', 'PPPo', 'QPPo', 'PPPo', 'PPQo', 'BBBb', 'CaCa', 'aCaC', 'CaCa', 'qCqC', 'CqCq'],
            'back': ['oooooooo'] * 6 + ['bbbbbbbb'] + ['aaaaaaaa'] * 5, 'top': ['oooooooo'] * 4,
            'bottom': ['qqqqqqqq'] * 4}
    bone_arm = {'front': ['CC', 'Ca', 'CC', 'aC', 'CC', 'Ca', 'AC', 'CC', 'Ca', 'CC', 'aA', 'AA'],
                'right': ['CC', 'aC', 'CC', 'Ca', 'CC', 'aC', 'CA', 'CC', 'aC', 'CC', 'Aa', 'AA'],
                'left': ['aC'] * 12, 'back': ['aa'] * 12, 'top': ['CC'] * 2, 'bottom': ['AA'] * 2}
    skull_p = {'front': ['aCCCa', 'CqCqC', 'CCqCC', 'aCaCa'], 'right': ['aCCCa', 'CCqqC', 'CCCCa', 'aaCaa'],
               'left': ['aCCCa'] * 4, 'back': ['aaaaa'] * 4, 'top': ['CCCCC'] * 5, 'bottom': ['AAAAA'] * 5}
    quiver = {'front': ['CaCa', 'aCaC', 'CaCa', 'AaAa', 'aAaA', 'AaAa', 'aAaA', 'AaAa', 'aAaA', 'AaAa'],
              'right': ['CaCa', 'aCaC', 'CaCa'] + ['aAaA'] * 7, 'left': ['aCaC'] * 10, 'back': ['AaAa'] * 10,
              'top': ['CCCC'] * 3, 'bottom': ['AAAA'] * 3}
    b = [Box(-4, 24, -4, 4, 32, 4, faces(WREN_SKULL)),
         Box(-4, 24, -4, 4, 32, 4, faces(WREN_HOOD), inflate=1.0),
         Box(-4, 12, -2, 4, 24, 2, faces(jerk)),
         Box(-6, 12, -1, -4, 24, 1, faces(bone_arm), pivot=(-5, 22, 0), rx=14.0),
         Box(4, 12, -1, 6, 24, 1, faces(bone_arm), pivot=(5, 22, 0), rx=-6.0),
         Box(-8.6, 21.5, -2.5, -3.6, 25.5, 2.5, faces(skull_p)),
         Box(2.8, 21.0, -5.6, 6.2, 35.0, -2.8, faces(quiver))]
    return b, []


# ---------------------------------------------------------------- Dunmar
DUN_HEAD = {
    'front': ['33dddd33',
              'd3dddd3d',
              'dd3dd3dd',
              'dddddddd',
              '4444d444',
              'd9e44e9d',
              'd1dddd1d',
              '24dddd42',
              '44424444',
              '24444442'],
    'right': ['d33ddddd', 'dd3ddddd', 'ddd3dddd', 'dddddddd', 'ddddd444', 'ddd1dde9', 'ddd11dd1', 'dd2dd444',
              '2d444424', '22444442'],
    'left': ['ddddd33d', 'ddddd3dd', 'dddd3ddd', 'dddddddd', '444ddddd', '9eddd1dd', '1dd11ddd', '444dd2dd',
             '424444d2', '24444422'],
    'back': ['d3dddd3d', 'dd3dd3dd', 'ddd33ddd', 'dd3dd3dd', 'd3dddd3d', 'dddddddd', 'dddddddd', 'dddddddd',
             'dddddddd', '22222222'],
    'top': ['dd3dd3dd', 'd3dddd3d', 'dddd3ddd', 'd33dd33d', 'ddd33ddd', 'dddddddd', 'd3dddd3d', 'dddddddd'],
    'bottom': ['44444444'] * 8,
}


def dunmar():
    nose = {'front': ['dd', 'd1', '11', '12'], 'right': ['dd', 'd1', '11', '21'], 'left': ['dd', '1d', '11', '12'],
            'back': ['dd'] * 4, 'top': ['dd'], 'bottom': ['22']}
    beard = {'front': ['4.4444.4', '44424444', '42444424', '.444244.'],
             'right': ['44444444', '44424444', '44444244', '.444444.'],
             'left': ['44444444', '44442444', '44244444', '.444444.'],
             'back': ['44444444'] * 4, 'top': ['44444444'] * 2, 'bottom': ['44444444'] * 2}
    tabard = {'front': ['IIIIIIII', 'dPPPPPPd', 'dPPPPPPd', 'dPQPPQPd', 'dPPPPPPd', 'dQPPPPQd', 'tItItItI',
                        'ItItYtIt', 'QPPPPPPQ', 'QPPPPPPQ', 'QQPPPPQQ', 'OQQPPQQO'],
              'right': ['dddI', 'dddP', 'dd3P', 'dddQ', 'd3dP', 'dddQ', 'tItI', 'ItIt', 'QPPP', 'QPPP', 'QQPP', 'OQQP'],
              'left': ['Iddd', 'Pddd', 'P3dd', 'Qddd', 'Pd3d', 'Qddd', 'ItIt', 'tItI', 'PPPQ', 'PPPQ', 'PPQQ', 'PQQO'],
              'back': ['dddddddd'] * 6 + ['tItItItI', 'ItItItIt'] + ['QPPPPPPQ'] * 4,
              'top': ['dddddddd'] * 4, 'bottom': ['QQQQQQQQ'] * 4}
    arm = {'front': ['dddd', 'dddd', '3333', 'd33d', '3333', 'dddd', 'dddd', '2dd2', 'dddd', 'dddd', '1dd1', '1111'],
           'right': ['dddd', 'dddd', '3333', '3dd3', '3333', 'dddd', 'dddd', 'd22d', 'dddd', 'dddd', 'd11d', '1111'],
           'left': ['dddd', 'dddd', '3333', 'd33d', '3333'] + ['dddd'] * 7, 'back': ['dddd', 'dddd', '3333', '3dd3', '3333'] + ['dddd'] * 7,
           'top': ['dddd'] * 4, 'bottom': ['1111'] * 4}
    anvil = {'front': ['ZYYYYYZ', 'tIIIIIt', '.tIIIt.', '.IIuII.'],
             'right': ['ZYYYYYYY', 'tIIIIIII', '.tIIIIt.', '.IIIIuI.'],
             'left': ['YYYYYYYZ'] * 1 + ['IIIIIIIt', '.tIIIIt.', '.IIIIII.'],
             'back': ['ZYYYYYZ', 'tIIIIIt', '.tIIIt.', '.IIIII.'],
             'top': ['ZZYYYYZ'] * 8, 'bottom': ['IIIIIII'] * 8}
    b = [Box(-4, 24, -4, 4, 34, 4, faces(DUN_HEAD)),
         Box(-1, 26, 4, 1, 30, 6, faces(nose)),
         Box(-4, 22.5, 3.4, 4, 26.6, 4.9, faces(beard)),
         Box(-4, 12, -3, 4, 24, 3, faces(tabard)),
         Box(-8, 12, -2, -4, 24, 2, faces(arm), pivot=(-5, 22, 0), rx=12.0),
         Box(4, 12, -2, 8, 24, 2, faces(arm), pivot=(5, 22, 0), rx=-6.0),
         Box(-10.2, 22.6, -4.0, -3.2, 26.6, 4.0, faces(anvil))]
    return b, []


# ---------------------------------------------------------------- Ashka
# Her troop type is the Hide Hunter (story spec cast table), so she has the Hide Hunter's model: the boar-kin
# build (a zombified piglin's geometry, `ArmyBoarkin`) in russet fur with no rot. The piglin's head is ten wide,
# with the snout standing a pixel proud of the lower face, two small tusks beside it, and the ears hanging out
# from the crown at thirty degrees. The leather cap sits over the crown; clay stripes cross the brow; the
# three-feather fan rises on the left of the head (the unit's mark).
ASH_HEAD = {
    'front': ['7777777777',
              '7777777777',
              '5885$$5885',
              '5905$$5095',
              '5$5$55$5$5',
              '55$5555$55',
              '$5555555$5',
              '$$55$$55$$'],
    'right': ['77777777', '77777777', '5555$585', '555$5558', '55$55555', '5555$555', '555$5555', '$$$$$$$$'],
    'left': ['77777777', '77777777', '585$5555', '8555$555', '55555$55', '555$5555', '5555$555', '$$$$$$$$'],
    'back': ['7777777777', '7787787787', '5555$55555', '55$5555$55', '5555555555', '555$55$555', '$555555$55',
             '$$$$$$$$$$'],
    'top': ['7777777777', '7787787787', '7777777777', '7777877777', '7777777777', '7787777877', '7777777777',
            '7777777777'],
    'bottom': ['$$$$$$$$$$'] * 8,
}
ASH_SNOUT = {'front': ['@!!@', '!00!', '!!!!', '@!!@'], 'right': ['@', '!', '!', '@'], 'left': ['@', '!', '!', '@'],
             'back': ['@@@@'] * 4, 'top': ['!!!!'], 'bottom': ['@@@@']}
ASH_TUSK = {'front': ['9', '8'], 'right': ['9', '8'], 'left': ['9', '8'], 'back': ['8', '8'], 'top': ['9'],
            'bottom': ['8']}
ASH_EAR = ['5555', '5!!5', '5!!5', '5!@5', '5@@5', '$55$']


def ashka():
    cloak = {'front': ['99999999', '55555555', '5PPPPPP5', '5PPPPPP5', '5QPPPPQ5', '5PPPPPP5', '7777777',
                       '5PPPPPP5', '5QPPPPQ5', '6QQPPQQ6', '6QQQQQQ6', '66QQQQ66'],
             'right': ['9999', '5556', '5556', '5566', '5556', '5566', '7777', '5556', '5566', '5666', '6666', '6666'],
             'left': ['9999', '6555', '6555', '6655', '6555', '6655', '7777', '6555', '6655', '6665', '6666', '6666'],
             'back': ['99999999'] + ['55555555', '56555565'] * 5 + ['66666666'], 'top': ['99999999'] * 4,
             'bottom': ['66666666'] * 4}
    for k in cloak:
        cloak[k] = [(r + r[-1] * 8)[:len(cloak['front'][0]) if k in ('front', 'back', 'top', 'bottom') else 4]
                    for r in cloak[k]]
    # the arms in the hide sleeve, then russet fur to the hand (no bare skin: she is boar-kin)
    sleeve = {'front': ['5555', '5655', '5555', '6555', '5555', '5565', '7777', '5$55', '55$5', '$55$', '5$$5', '$$$$'],
              'right': ['5555', '5565', '5555', '5556', '5555', '5655', '7777', '55$5', '5$55', '$55$', '5$$5', '$$$$'],
              'left': ['5555'] * 6 + ['7777'] + ['5$55'] * 5, 'back': ['5555'] * 12, 'top': ['9999'] * 4,
              'bottom': ['$$$$'] * 4}
    # the three-feather fan on the left of the head: three cards splayed, clay, russet and white with dark tips
    feather = [Tex(['0.', '88', '88', '98', '88', '88', '.8', '.7'], P),
               Tex(['0.', '55', '65', '55', '65', '55', '.5', '.7'], P),
               Tex(['0.', '99', '98', '99', '99', '98', '.9', '.7'], P)]
    planes = []
    for k, (ox, oy, lean) in enumerate(((4.4, 37.0, -3.6), (5.6, 38.4, -0.4), (6.8, 37.4, 3.0))):
        o = (ox, oy, -1.0 + k * 0.6)
        planes.append(Plane(o, (0.0, 0.0, 2.0), (lean, -7.0, 0.0), feather[k]))
    # the ears hang out from the crown at thirty degrees, pink inside
    ear = Tex(ASH_EAR, P)
    planes.append(Plane((5.6, 30.2, -2.0), (0.0, 0.0, 4.0), (3.0, -5.2, 0.0), ear))
    planes.append(Plane((-5.6, 30.2, -2.0), (0.0, 0.0, 4.0), (-3.0, -5.2, 0.0), ear))
    cap = {'front': ['77777777777', '00000000000'] + ['...........'] * 2,
           'right': ['777777777', '000000000'] + ['.........'] * 2,
           'left': ['777777777', '000000000'] + ['.........'] * 2,
           'back': ['77777777777', '00000000000', '77777777777', '77777777777'],
           'top': ['77777777777'] * 9, 'bottom': ['...........'] * 9}
    b = [Box(-5, 24, -4, 5, 32, 4, faces(ASH_HEAD)),
         Box(-5.4, 29.0, -4.4, 5.4, 32.6, 4.4, faces(cap)),
         Box(-2, 24, 4, 2, 28, 5, faces(ASH_SNOUT)),
         Box(2, 24, 4, 3, 26, 5, faces(ASH_TUSK)),
         Box(-3, 24, 4, -2, 26, 5, faces(ASH_TUSK)),
         Box(-4, 12, -2, 4, 24, 2, faces(cloak)),
         Box(-4.6, 19, -2.6, 4.6, 24.6, 2.6, faces({'front': ['9999999999'] + ['5555555555'] * 4 + ['6556556556'],
                                                     'right': ['99999'] + ['55555'] * 4 + ['65565'],
                                                     'left': ['99999'] + ['55555'] * 4 + ['65565'],
                                                     'back': ['9999999999'] + ['5555555555'] * 5,
                                                     'top': ['9999999999'] * 5, 'bottom': ['6666666666'] * 5})),
         Box(-8, 12, -2, -4, 24, 2, faces(sleeve), pivot=(-5, 22, 0), rx=14.0),
         Box(4, 12, -2, 8, 24, 2, faces(sleeve), pivot=(5, 22, 0), rx=-6.0)]
    return b, planes


MODELS = {'maud': (maud, 0), 'wren': (wren, 0), 'dunmar': (dunmar, 2), 'ashka': (ashka, 4)}


def portrait(name, win, grey=False):
    mk, tall = MODELS[name]
    s = render(mk(), win, tall)
    if grey:
        # resting or wounded: the portrait drained to a cool grey and its range pressed toward the middle, so the
        # sitter reads as a pale likeness against the frame's dark ground, as the book greys it
        for y in range(s.h):
            for x in range(s.w):
                r, g, b, a = s.get(x, y)
                if a:
                    v = round(34 + (0.30 * r + 0.59 * g + 0.11 * b) * 0.72)
                    s.set(x, y, (v, v + 3, v + 8, 255))
    return s


for _n in MODELS:
    ASSETS['pt-%s-40' % _n] = (lambda n: (lambda: portrait(n, 40)))(_n)
    ASSETS['pt-%s-40-grey' % _n] = (lambda n: (lambda: portrait(n, 40, grey=True)))(_n)
ASSETS['pt-maud-60'] = lambda: portrait('maud', 60)
ASSETS['pt-maud-80'] = lambda: portrait('maud', 80)

if __name__ == '__main__':
    names = sys.argv[1:] or list(ASSETS)
    for n in names:
        save(ASSETS[n](), n)
