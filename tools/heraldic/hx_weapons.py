"""Heraldic units: the weapons, rebuilt.

My own version of the project's work/p2b/make_weapons.py and the weapon
textures. The baseline blades are one texel wide: from any angle a sword is a
one-pixel white sliver with no taper, no fuller and no edge, so it reads as a
stick rather than a blade. The bows are thin on purpose (a bow limb is thin)
but they are flat brown with no grain and no lit edge.

This gives every weapon:
  - a blade two texels wide, so it has a silhouette from the front as well as
    from the side, with a polished edge, a darker flat and a fuller;
  - a real point, a guard with quillons, a wound grip and a pommel;
  - material: steel with a specular edge, gilt with a lit rim, wood with grain,
    horn pale at the nocks, leather dark on the grip, and a taut pale string.

  python work/p2b/hx_weapons.py
"""
import json
import pathlib
import sys

from PIL import Image

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))

from paint import faces, fill, paint_box, rows, hexc  # noqa: E402

CLEAR = (0, 0, 0, 0)

# The palette, matched to the skins (hx_art.py).
STEEL = {'S0': hexc('#E2E7ED'), 'S1': hexc('#BCC3CE'), 'S2': hexc('#98A0AC'), 'S3': hexc('#737A87'),
         'S4': hexc('#51565F'), 'S5': hexc('#34383F')}
GILT = {'G0': hexc('#D4BE8C'), 'G1': hexc('#B09662'), 'G2': hexc('#8B7040'), 'G3': hexc('#634E2B'),
        'G4': hexc('#42341D')}
WOOD = {'O0': hexc('#9A8568'), 'O1': hexc('#7B6A50'), 'O2': hexc('#5C4F3A'), 'O3': hexc('#3E3627')}
LEATHER = {'L0': hexc('#8F7A62'), 'L1': hexc('#715F4A'), 'L2': hexc('#554637'), 'L3': hexc('#3C3126'),
           'L4': hexc('#282019')}
HORN = {'BONE': hexc('#DCD6C4')}
SUN = {'U0': hexc('#FFFBF0'), 'U1': hexc('#F7EDD0'), 'U2': hexc('#E6DAB6'), 'U3': hexc('#CBBB99')}
CORD = {'C0': hexc('#E4DCC6'), 'C1': hexc('#B8AF95')}

# Every weapon's texture sheet is drawn in one pass: a fresh RGBA image per weapon.
SHEETS = {}


def sheet(name, size):
    im = Image.new('RGBA', size, CLEAR)
    SHEETS[name] = im
    return im


def pat(lines, key):
    """A face painter from text rows through a key."""
    def paint(p, r):
        x0, y0, x1, y1 = r
        for j, line in enumerate(lines):
            for i, ch in enumerate(line):
                c = key.get(ch)
                if c is not None and x0 + i < x1 and y0 + j < y1:
                    p[x0 + i, y0 + j] = c
    return paint


def steel_flat(light=STEEL['S0'], mid=STEEL['S1'], dark=STEEL['S2'], deep=STEEL['S3']):
    """A blade's flat: the lit edge, the flat, and the shadowed edge."""
    def paint(p, r):
        x0, y0, x1, y1 = r
        w, h = x1 - x0, y1 - y0
        for j in range(h):
            for i in range(w):
                if i == 0:
                    c = light
                elif i == w - 1:
                    c = deep
                else:
                    c = mid
                p[x0 + i, y0 + j] = c
    return paint


def steel_flat_dark(light=STEEL['S1'], mid=STEEL['S2'], dark=STEEL['S3'], deep=STEEL['S4']):
    """A blade's flat on the shadow side: the same, one step down."""
    def paint(p, r):
        x0, y0, x1, y1 = r
        w, h = x1 - x0, y1 - y0
        for j in range(h):
            for i in range(w):
                if i == 0:
                    c = light
                elif i == w - 1:
                    c = deep
                else:
                    c = mid
                p[x0 + i, y0 + j] = c
    return paint


def edge_face(light=STEEL['S0'], dark=STEEL['S2']):
    """A blade's thin edge: a bright line with a shadow under it."""
    def paint(p, r):
        x0, y0, x1, y1 = r
        for j, y in enumerate(range(y0, y1)):
            for x in range(x0, x1):
                p[x, y] = light if j % 3 else dark
    return paint


def wood_face(light=WOOD['O0'], mid=WOOD['O1'], dark=WOOD['O2']):
    """Wood: a lit edge, a grain running with the length, a shadowed edge."""
    def paint(p, r):
        x0, y0, x1, y1 = r
        w, h = x1 - x0, y1 - y0
        for j in range(h):
            for i in range(w):
                if i == 0:
                    c = light
                elif i == w - 1:
                    c = dark
                elif (i * 3 + j) % 5 == 0:
                    c = dark
                else:
                    c = mid
                p[x0 + i, y0 + j] = c
    return paint


def wound_grip(light=LEATHER['L0'], mid=LEATHER['L1'], dark=LEATHER['L2']):
    """A grip wound in leather: a spiral of cord."""
    def paint(p, r):
        x0, y0, x1, y1 = r
        w, h = x1 - x0, y1 - y0
        for j in range(h):
            for i in range(w):
                p[x0 + i, y0 + j] = light if (i + j) % 3 == 0 else (dark if (i + j) % 3 == 1 else mid)
    return paint


def gilt_face(light=GILT['G0'], mid=GILT['G1'], dark=GILT['G2'], deep=GILT['G3']):
    """Gilt: a lit rim, a warm field, a dark underside."""
    def paint(p, r):
        x0, y0, x1, y1 = r
        w, h = x1 - x0, y1 - y0
        for j in range(h):
            for i in range(w):
                if j == 0:
                    c = light
                elif j == h - 1:
                    c = deep
                else:
                    c = mid if i % 2 == 0 else dark
                p[x0 + i, y0 + j] = c
    return paint


def sun_face(p, r):
    """The sun disc: a bright core, rays round it, a shadowed rim."""
    x0, y0, x1, y1 = r
    w, h = x1 - x0, y1 - y0
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    for j in range(h):
        for i in range(w):
            d = ((i - cx) ** 2 + (j - cy) ** 2) ** 0.5
            if d <= cx * 0.45:
                c = SUN['U0']
            elif d <= cx * 0.75:
                c = SUN['U1']
            elif d <= cx * 0.95:
                c = SUN['U2']
            else:
                c = SUN['U3']
            p[x0 + i, y0 + j] = c


def ray_face(p, r):
    """The rays behind the disc: gilt spokes radiating out from the centre.

    The angle of each texel from the centre decides the ray, so the burst is
    genuinely radial: a checkerboard of alternating dither reads as noise, not
    as light coming off a sun.
    """
    import math
    x0, y0, x1, y1 = r
    w, h = x1 - x0, y1 - y0
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    for j in range(h):
        for i in range(w):
            dx, dy = i - cx, j - cy
            d = (dx * dx + dy * dy) ** 0.5
            if d < 1.6 or d > cx * 1.22:
                p[x0 + i, y0 + j] = CLEAR
                continue
            ang = math.atan2(dy, dx)
            spoke = (ang + math.pi) * 8.0 / (2 * math.pi)   # 16 spokes round the disc
            if spoke - math.floor(spoke) < 0.42:
                # a spoke, lit on the side facing the light (up and to the left)
                c = GILT['G0'] if (dx + dy) < 0 else (GILT['G1'] if (i + j) % 2 == 0 else GILT['G2'])
            else:
                c = CLEAR
            p[x0 + i, y0 + j] = c


# ---------------------------------------------------------------------------------------
# The weapons. Each is a JSON (the geometry, handed to the build) and a PNG (the paint).
# The blade is two texels wide so it has a silhouette from the front as well as from
# the side; the build's UV layout is respected by laying the boxes out by hand.
# ---------------------------------------------------------------------------------------
def build_arming_sword():
    """The Order Squire's arming sword: a 10 px blade and point, a gilt guard and pommel."""
    w = {
        "weapon": "arming_sword",
        "note": "The Squire's arming sword: a 10 px blade two texels wide, a tapered point, a "
                "gilt crossguard, a leather-wound grip and a gilt pommel.",
        "size": [32, 32],
        "grip": {"offset": [0, 0, -0.5], "rotation": [45, 0, 0]},
        "boxes": [
            {"origin": [-1, -12.5, -1.0], "size": [2, 10, 2], "uv": [0, 0], "note": "blade"},
            {"origin": [-1, -13.5, -0.5], "size": [2, 1, 1], "uv": [8, 0], "note": "point"},
            {"origin": [-1, -2.5, -3.0], "size": [2, 1, 6], "uv": [0, 12], "note": "crossguard"},
            {"origin": [-1, -1.5, -0.5], "size": [2, 4, 1], "uv": [16, 12], "note": "grip, wound"},
            {"origin": [-1, 2.5, -1.0], "size": [2, 1, 2], "uv": [16, 18], "note": "pommel"},
        ],
    }
    im = sheet('arming_sword', (32, 32))
    px = im.load()
    # the blade: the flat faces carry the edge and the fuller, the thin faces the polish
    paint_box(px, (0, 0), (2, 10, 2), {'top': STEEL['S0'], 'bottom': STEEL['S4'],
                                       'west': edge_face(), 'east': edge_face(STEEL['S1'], STEEL['S3']),
                                       'north': steel_flat(), 'south': steel_flat_dark()})
    # the point
    paint_box(px, (8, 0), (2, 1, 1), {'top': STEEL['S0'], 'bottom': STEEL['S4'], 'side': STEEL['S1']})
    # the crossguard: gilt, a lit rim
    paint_box(px, (0, 12), (2, 1, 6), {'top': GILT['G0'], 'bottom': GILT['G4'],
                                       'north': gilt_face(), 'south': gilt_face(GILT['G1'], GILT['G2'], GILT['G3'], GILT['G4']),
                                       'west': GILT['G2'], 'east': GILT['G2']})
    # the grip, wound
    paint_box(px, (16, 12), (2, 4, 1), {'top': LEATHER['L0'], 'bottom': LEATHER['L3'],
                                        'north': wound_grip(), 'south': wound_grip(LEATHER['L1'], LEATHER['L2'], LEATHER['L3']),
                                        'west': LEATHER['L1'], 'east': LEATHER['L1']})
    # the pommel
    paint_box(px, (16, 18), (2, 1, 2), {'top': GILT['G0'], 'bottom': GILT['G4'],
                                        'north': gilt_face(), 'south': gilt_face(GILT['G1'], GILT['G2'], GILT['G3'], GILT['G4']),
                                        'west': GILT['G1'], 'east': GILT['G1']})
    return w


def build_longsword():
    """The Templar's longsword: a 13 px blade, a wide guard and a hand-and-a-half grip."""
    w = {
        "weapon": "longsword",
        "note": "The Templar's longsword: a 13 px blade two texels wide with a tapered point, a "
                "wide gilt guard, a hand-and-a-half grip and a gilt pommel.",
        "size": [32, 32],
        "grip": {"offset": [0, 0, -0.5], "rotation": [45, 0, 0]},
        "boxes": [
            {"origin": [-1, -15.5, -1.0], "size": [2, 13, 2], "uv": [0, 0], "note": "blade"},
            {"origin": [-1, -16.5, -0.5], "size": [2, 1, 1], "uv": [8, 0], "note": "point"},
            {"origin": [-1, -2.5, -4.0], "size": [2, 1, 8], "uv": [0, 15], "note": "crossguard"},
            {"origin": [-1, -1.5, -0.5], "size": [2, 6, 1], "uv": [24, 15], "note": "grip, wound"},
            {"origin": [-1, 4.5, -1.0], "size": [2, 2, 2], "uv": [16, 24], "note": "pommel"},
        ],
    }
    im = sheet('longsword', (32, 32))
    px = im.load()
    paint_box(px, (0, 0), (2, 13, 2), {'top': STEEL['S0'], 'bottom': STEEL['S4'],
                                       'west': edge_face(), 'east': edge_face(STEEL['S1'], STEEL['S3']),
                                       'north': steel_flat(), 'south': steel_flat_dark()})
    paint_box(px, (8, 0), (2, 1, 1), {'top': STEEL['S0'], 'bottom': STEEL['S4'], 'side': STEEL['S1']})
    paint_box(px, (0, 15), (2, 1, 8), {'top': GILT['G0'], 'bottom': GILT['G4'],
                                       'north': gilt_face(), 'south': gilt_face(GILT['G1'], GILT['G2'], GILT['G3'], GILT['G4']),
                                       'west': GILT['G2'], 'east': GILT['G2']})
    paint_box(px, (24, 15), (2, 6, 1), {'top': LEATHER['L0'], 'bottom': LEATHER['L3'],
                                        'north': wound_grip(), 'south': wound_grip(LEATHER['L1'], LEATHER['L2'], LEATHER['L3']),
                                        'west': LEATHER['L1'], 'east': LEATHER['L1']})
    paint_box(px, (16, 24), (2, 2, 2), {'top': GILT['G0'], 'bottom': GILT['G4'],
                                        'north': gilt_face(), 'south': gilt_face(GILT['G1'], GILT['G2'], GILT['G3'], GILT['G4']),
                                        'west': GILT['G1'], 'east': GILT['G1']})
    return w


def build_marshal_sword():
    """Lord Marshal Aldric's sword: a broad 14 px blade with a fuller, a gilt guard with
    dropped quillons, a wound grip and a gilt wheel pommel."""
    w = {
        "weapon": "marshal_sword",
        "note": "Lord Marshal Aldric's sword: a broad 14 px blade two texels wide with a fuller, "
                "a gilt guard with dropped quillons, a wound grip and a gilt wheel pommel.",
        "size": [32, 32],
        "grip": {"offset": [0, 0, -0.5], "rotation": [45, 0, 0]},
        "boxes": [
            {"origin": [-1, -16.5, -1.5], "size": [2, 14, 3], "uv": [0, 0], "note": "blade"},
            {"origin": [-1, -17.5, -1], "size": [2, 1, 2], "uv": [10, 0], "note": "shoulder of the point"},
            {"origin": [-1, -18.5, -0.5], "size": [2, 1, 1], "uv": [18, 0], "note": "point"},
            {"origin": [-1, -2.5, -5.0], "size": [2, 1, 10], "uv": [0, 18], "note": "crossguard"},
            {"origin": [-1, -1.5, -5], "size": [2, 1, 1], "uv": [24, 0], "note": "right quillon drop"},
            {"origin": [-1, -1.5, 4], "size": [2, 1, 1], "uv": [24, 2], "note": "left quillon drop"},
            {"origin": [-1, -1.5, -0.5], "size": [2, 5, 1], "uv": [24, 4], "note": "grip, wound"},
            {"origin": [-2.0, 3.5, -1.0], "size": [4, 2, 2], "uv": [18, 10], "note": "pommel"},
        ],
    }
    im = sheet('marshal_sword', (32, 32))
    px = im.load()
    # the broad blade: a polished edge, a flat and a dark fuller down the middle
    def broad(p, r):
        x0, y0, x1, y1 = r
        wdt, h = x1 - x0, y1 - y0
        for j in range(h):
            for i in range(wdt):
                if i == 0:
                    c = STEEL['S0']
                elif i == wdt - 1:
                    c = STEEL['S4']
                else:
                    c = STEEL['S2']
                p[x0 + i, y0 + j] = c
    paint_box(px, (0, 0), (2, 14, 3), {'top': STEEL['S0'], 'bottom': STEEL['S4'],
                                        'west': edge_face(), 'east': edge_face(STEEL['S1'], STEEL['S3']),
                                        'north': broad, 'south': broad})
    paint_box(px, (10, 0), (2, 1, 2), {'top': STEEL['S0'], 'bottom': STEEL['S4'],
                                      'north': steel_flat(), 'south': steel_flat_dark(),
                                      'west': STEEL['S1'], 'east': STEEL['S2']})
    paint_box(px, (18, 0), (2, 1, 1), {'top': STEEL['S0'], 'bottom': STEEL['S4'], 'side': STEEL['S1']})
    paint_box(px, (0, 18), (2, 1, 10), {'top': GILT['G0'], 'bottom': GILT['G4'],
                                        'north': gilt_face(), 'south': gilt_face(GILT['G1'], GILT['G2'], GILT['G3'], GILT['G4']),
                                        'west': GILT['G2'], 'east': GILT['G2']})
    for uv in ((24, 0), (24, 2)):
        paint_box(px, uv, (2, 1, 1), {'top': GILT['G0'], 'bottom': GILT['G4'], 'side': GILT['G1']})
    paint_box(px, (24, 4), (2, 5, 1), {'top': LEATHER['L0'], 'bottom': LEATHER['L3'],
                                        'north': wound_grip(), 'south': wound_grip(LEATHER['L1'], LEATHER['L2'], LEATHER['L3']),
                                        'west': LEATHER['L1'], 'east': LEATHER['L1']})
    paint_box(px, (18, 10), (4, 2, 2), {'top': GILT['G0'], 'bottom': GILT['G4'],
                                       'north': gilt_face(), 'south': gilt_face(GILT['G1'], GILT['G2'], GILT['G3'], GILT['G4']),
                                       'west': GILT['G1'], 'east': GILT['G1']})
    return w


def build_order_bow():
    """The Order Archer's bow: a short recurve in dark wood, gilt tips, the grip wound."""
    w = {
        "weapon": "order_bow",
        "note": "The Order Archer's bow, 22 px: a short recurve in dark wood with a lit edge, gilt "
                "tips and a grip wound in linen; upright in the right hand, the string outward.",
        "size": [32, 32],
        "grip": {"offset": [0, 0, -2.5], "rotation": [0, 0, 0]},
        "boxes": [
            {"origin": [-0.5, -3, -0.5], "size": [1, 6, 1], "uv": [0, 0], "note": "the grip, wound in linen"},
            {"origin": [-1.5, -7, -0.5], "size": [1, 4, 1], "uv": [4, 0], "note": "upper limb"},
            {"origin": [-1.5, 3, -0.5], "size": [1, 4, 1], "uv": [8, 0], "note": "lower limb"},
            {"origin": [-2.5, -10, -0.5], "size": [1, 3, 1], "uv": [12, 0], "note": "upper limb, bending"},
            {"origin": [-2.5, 7, -0.5], "size": [1, 3, 1], "uv": [16, 0], "note": "lower limb, bending"},
            {"origin": [-3.5, -11, -0.5], "size": [1, 1, 1], "uv": [20, 0], "note": "upper tip, gilt"},
            {"origin": [-3.5, 10, -0.5], "size": [1, 1, 1], "uv": [24, 0], "note": "lower tip, gilt"},
            {"origin": [-2.5, -12, -0.5], "size": [1, 1, 1], "uv": [28, 0], "note": "upper recurve, flicked back"},
            {"origin": [-2.5, 11, -0.5], "size": [1, 1, 1], "uv": [0, 8], "note": "lower recurve, flicked back"},
            {"origin": [-4.5, -10, 0], "size": [1, 20, 0], "uv": [4, 8], "note": "string"},
        ],
    }
    im = sheet('order_bow', (32, 32))
    px = im.load()
    paint_box(px, (0, 0), (1, 6, 1), {'top': CORD['C0'], 'bottom': LEATHER['L3'],
                                      'north': wound_grip(CORD['C0'], CORD['C1'], CORD['C1']),
                                      'south': wound_grip(CORD['C1'], CORD['C1'], CORD['C1']),
                                      'west': LEATHER['L1'], 'east': LEATHER['L1']})
    for uv in ((4, 0), (8, 0), (12, 0), (16, 0)):
        paint_box(px, uv, (1, 4, 1), {'top': WOOD['O0'], 'bottom': WOOD['O3'],
                                      'north': wood_face(), 'south': wood_face(WOOD['O1'], WOOD['O2'], WOOD['O3']),
                                      'west': WOOD['O0'], 'east': WOOD['O2']})
    for uv in ((20, 0), (24, 0), (28, 0), (0, 8)):
        paint_box(px, uv, (1, 1, 1), {'top': GILT['G0'], 'bottom': GILT['G3'], 'side': GILT['G1']})
    # the string: a taut pale line
    paint_box(px, (4, 8), (1, 20, 0), {'top': CORD['C0'], 'bottom': CORD['C1'],
                                       'north': CORD['C0'], 'south': CORD['C1'],
                                       'west': CORD['C1'], 'east': CORD['C0']})
    return w


def build_longbow():
    """The Order Longbow's yew longbow: a shallow D, horn nocks, a leather grip."""
    w = {
        "weapon": "longbow",
        "note": "The Order Longbow's yew longbow, 38 px: a shallow D of yew with a lit edge, "
                "horn nocks, a leather grip and a taut string; upright in the right hand, its "
                "top 6 px past the head and its foot just off the ground (the mark).",
        "size": [64, 64],
        "grip": {"offset": [0, 0, -2.5], "rotation": [0, 0, 0]},
        "boxes": [
            {"origin": [-0.5, -2, -0.5], "size": [1, 4, 1], "uv": [0, 0], "note": "the leather grip"},
            {"origin": [-0.5, -12, -0.5], "size": [1, 10, 1], "uv": [4, 0], "note": "the stave over the grip"},
            {"origin": [-1.5, -18, -0.5], "size": [1, 6, 1], "uv": [8, 0], "note": "upper limb"},
            {"origin": [-1.5, 2, -0.5], "size": [1, 6, 1], "uv": [12, 0], "note": "lower limb"},
            {"origin": [-2.5, -22, -0.5], "size": [1, 4, 1], "uv": [16, 0], "note": "upper limb, bending"},
            {"origin": [-2.5, 8, -0.5], "size": [1, 4, 1], "uv": [20, 0], "note": "lower limb, bending"},
            {"origin": [-3.5, -24, -0.5], "size": [1, 2, 1], "uv": [24, 0], "note": "upper horn nock"},
            {"origin": [-3.5, 12, -0.5], "size": [1, 2, 1], "uv": [28, 0], "note": "lower horn nock"},
            {"origin": [-4.5, -23, 0], "size": [1, 36, 0], "uv": [32, 0], "note": "string"},
        ],
    }
    im = sheet('longbow', (64, 64))
    px = im.load()
    paint_box(px, (0, 0), (1, 4, 1), {'top': LEATHER['L0'], 'bottom': LEATHER['L3'],
                                      'north': wound_grip(), 'south': wound_grip(LEATHER['L1'], LEATHER['L2'], LEATHER['L3']),
                                      'west': LEATHER['L1'], 'east': LEATHER['L1']})
    for uv, h in (((4, 0), 10), ((8, 0), 6), ((12, 0), 6), ((16, 0), 4), ((20, 0), 4)):
        paint_box(px, uv, (1, h, 1), {'top': WOOD['O0'], 'bottom': WOOD['O3'],
                                      'north': wood_face(), 'south': wood_face(WOOD['O1'], WOOD['O2'], WOOD['O3']),
                                      'west': WOOD['O0'], 'east': WOOD['O2']})
    # the horn nocks: pale, catching the light
    for uv in ((24, 0), (28, 0)):
        paint_box(px, uv, (1, 2, 1), {'top': HORN['BONE'], 'bottom': HORN['BONE'],
                                      'north': HORN['BONE'], 'south': HORN['BONE'],
                                      'west': HORN['BONE'], 'east': HORN['BONE']})
    # the string
    paint_box(px, (32, 0), (1, 36, 0), {'top': CORD['C0'], 'bottom': CORD['C1'],
                                        'north': CORD['C0'], 'south': CORD['C1'],
                                        'west': CORD['C1'], 'east': CORD['C0']})
    return w


def build_sun_staff():
    """The Sunweaver's staff: a pale ash shaft, a gilt collar and the 6x6 sun disc."""
    w = {
        "weapon": "sun_staff",
        "note": "The Sunweaver's staff: a pale ash shaft with a gilt band, a gilt collar and the "
                "6x6 sun disc (the mark), its face lit, gilt rays behind it; upright in the right "
                "hand, the disc 4 px over the head.",
        "size": [32, 64],
        "grip": {"offset": [0, 0, -2.5], "rotation": [0, 0, 0]},
        "boxes": [
            {"origin": [-0.5, -21, -0.5], "size": [1, 33, 1], "uv": [0, 0], "note": "the shaft, gilt-banded"},
            {"origin": [-1, -22, -1], "size": [2, 1, 2], "uv": [4, 0], "note": "the gilt collar"},
            {"origin": [-3, -28, -0.5], "size": [6, 6, 1], "uv": [12, 0], "note": "the sun disc"},
            {"origin": [-4, -29, 0.3], "size": [8, 8, 0], "uv": [12, 8], "note": "the rays behind the disc"},
        ],
    }
    im = sheet('sun_staff', (32, 64))
    px = im.load()
    # the shaft: pale ash with a gilt band
    paint_box(px, (0, 0), (1, 33, 1), {'top': hexc('#B0A48A'), 'bottom': hexc('#6B6252'),
                                      'north': wood_face(hexc('#C4B99F'), hexc('#A89C82'), hexc('#7E735E')),
                                      'south': wood_face(hexc('#9A8E75'), hexc('#867A63'), hexc('#5E5646')),
                                      'west': hexc('#C4B99F'), 'east': hexc('#7E735E')})
    paint_box(px, (4, 0), (2, 1, 2), {'top': GILT['G0'], 'bottom': GILT['G4'],
                                      'north': gilt_face(), 'south': gilt_face(GILT['G1'], GILT['G2'], GILT['G3'], GILT['G4']),
                                      'west': GILT['G1'], 'east': GILT['G1']})
    # the disc and its rays
    paint_box(px, (12, 0), (6, 6, 1), {'top': SUN['U1'], 'bottom': SUN['U3'],
                                       'north': sun_face, 'south': sun_face,
                                       'west': SUN['U2'], 'east': SUN['U3']})
    paint_box(px, (12, 8), (8, 8, 0), {'top': GILT['G0'], 'bottom': GILT['G4'],
                                       'north': ray_face, 'south': ray_face,
                                       'west': GILT['G1'], 'east': GILT['G2']})
    return w


WEAPONS = [build_arming_sword, build_longsword, build_marshal_sword,
           build_order_bow, build_longbow, build_sun_staff]


def main():
    folder = ROOT / 'art/weapons'
    folder.mkdir(parents=True, exist_ok=True)
    for build in WEAPONS:
        w = build()
        wid = w['weapon']
        (folder / (wid + '.json')).write_text(json.dumps(w, indent=1, ensure_ascii=True) + '\n', encoding='utf-8')
        SHEETS[wid].save(folder / (wid + '.png'))
        print('%s: weapon written' % wid)


if __name__ == '__main__':
    main()
