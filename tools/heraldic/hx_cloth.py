"""Heraldic units: the team cloth template and the side glyph masks.

My own version of the project's work/p2b/make_cloth.py. The baseline cloth is a
flat field with a belt: at 36 px the team colour is the loudest read on the unit
but it says nothing, so every troop wearing the same pale looks the same. This
adds the thing the unit identity spec asks for and the baseline never had: a
*device*.

The field greys (160 lit, 128 mid, 96 shadow) take the side colour at x1.0,
x0.8 and x0.6, so a design drawn in those greys is a two-tone device in the
team's own colour. The white charge is fixed paint. A folded edge is a shadow
grey. The result reads as cloth with a charge on it, in any of the four side
colours, without the skins ever carrying a team colour.

Every team part gets a graded field (lit at the left, shadowed at the right)
and fold shading. The side glyph (sun, moon, star, wave) is stamped in white on
the pennant, the standard, the chest pale, the jerkin and the vestment, so a
colour-blind player still reads the side off the charge, not the hue. The
Templar's tabard keeps its white cross instead: that is his house's own device,
and it is what stops a Squire and a Templar reading alike.

  python work/p2b/hx_cloth.py
"""
import pathlib
import sys

from PIL import Image

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))

from paint import faces, fill, paint_box, rows, hexc  # noqa: E402

SIZE = (128, 64)
F, M, S = (160, 160, 160, 255), (128, 128, 128, 255), (96, 96, 96, 255)
CLEAR = (0, 0, 0, 0)
WHITE, WHITE_SHADE = hexc('#F6F2E7'), hexc('#DCD5C4')
WOOD_LIT, WOOD, WOOD_DARK = hexc('#7A5E42'), hexc('#5C4632'), hexc('#46352A')
LEATHER_LIT, LEATHER, LEATHER_DARK = hexc('#6B5136'), hexc('#4A3A2C'), hexc('#3A2D22')
GILT_DARK, GILT, GILT_LIT = hexc('#6E5527'), hexc('#96783A'), hexc('#C09A52')
STEEL = hexc('#A3ABB6')
KEY = {'F': F, 'M': M, 'S': S, '.': None, '_': CLEAR, 'W': WHITE, 'w': WHITE_SHADE, 'J': GILT_DARK, 'G': GILT,
       'H': GILT_LIT, 'L': LEATHER, 'l': LEATHER_LIT, 'k': LEATHER_DARK, 's': STEEL, 'o': WOOD, 'O': WOOD_LIT,
       'd': WOOD_DARK}

im = Image.new('RGBA', SIZE, (0, 0, 0, 0))
px = im.load()


def pat(lines):
    """A face painter from text rows (KEY); '_' cuts the texel out."""
    def paint(p, r):
        x0, y0, x1, y1 = r
        for j, line in enumerate(lines):
            for i, ch in enumerate(line):
                c = KEY[ch]
                if c is not None and x0 + i < x1 and y0 + j < y1:
                    p[x0 + i, y0 + j] = c
    return paint


# ---------------------------------------------------------------------------------------
# pennant: pole, flag, two brackets. The flag's field is graded (lit at the hoist,
# shadowed at the fly) with a shadowed hem, so the cloth is not a flat colour field.
# The glyph is stamped over it later.
# ---------------------------------------------------------------------------------------
paint_box(px, (0, 0), (1, 24, 1), {'top': WOOD_LIT, 'bottom': WOOD_DARK, 'north': WOOD_LIT, 'west': WOOD, 'east': WOOD,
                                   'south': WOOD_DARK})
FLAG = ['MFFF', 'FFFF', 'FFFF', 'FFFF', 'FFFF', 'FFFF', 'FFFF', 'MMMM']
paint_box(px, (4, 0), (4, 8, 1), {'top': F, 'bottom': S, 'west': M, 'east': S, 'north': pat(FLAG), 'south': pat(FLAG)})
for uv in ((4, 10), (4, 18)):
    paint_box(px, uv, (1, 1, 6), {'top': LEATHER_LIT, 'bottom': LEATHER_DARK, 'side': LEATHER})

# ---------------------------------------------------------------------------------------
# chest_panel: the 4 px pale down the front, belted in gilt, carrying the side device.
# The field is graded left to right and folded down the middle, so the team colour reads
# as cloth over a chest. Rows 2 to 7 of the face are left as field for the glyph.
# ---------------------------------------------------------------------------------------
PANEL = ['MMMM',   # the shoulder line, in shadow under the collar
         'FFFF',   # the device sits on rows 2 to 7 of the face
         'FFFF',
         'FFFF',
         'FFFF',
         'FFFF',
         'FFFF',
         'FFFF',
         'MFFM',   # the pale narrows toward the belt
         'GGGG']   # the gilt belt
PANEL_BACK = ['MMMM', 'FFFF', 'FFFF', 'FFFF', 'FFFF', 'FFFF', 'FFFF', 'FFFF', 'MFFM', 'GGGG']
PANEL_SIDE = ['MMMMM'] * 6 + ['GGGGG'] + ['MMMMM'] * 3
paint_box(px, (18, 0), (4, 10, 5), {'top': F, 'bottom': S, 'west': pat(PANEL_SIDE), 'east': pat(PANEL_SIDE),
                                    'north': pat(PANEL), 'south': pat(PANEL_BACK)})
RUN = ['FFFF', 'FFFF', 'MFFM', 'SMMS']
paint_box(px, (18, 15), (4, 4, 6), {'top': M, 'bottom': S, 'side': M, 'north': pat(RUN), 'south': pat(RUN)})

# ---------------------------------------------------------------------------------------
# long_tabard: the Templar's white cross on the team field, with the field graded and a
# shadow under the cross so the charge sits on the cloth. This is his house's device and
# it is what keeps him from reading like the Squire.
# ---------------------------------------------------------------------------------------
TABARD = ['FFWWFF', 'WWWWWW', 'FFWWFF', 'FFWWFF', 'MFWWFM', 'MFWWFM', 'MFWWFM', 'GGHGGG', 'MFFFFM', 'MFFFFM', 'MMFFMM']
TABARD_BACK = ['FFWWFF', 'WWWWWW', 'FFWWFF', 'FFWWFF', 'MFWWFM', 'MFWWFM', 'MFWWFM', 'GGGGGG', 'MFFFFM', 'MFFFFM', 'MMFFMM']
TABARD_SIDE = ['MMMMM'] * 7 + ['GGGGG'] + ['MMMMM'] * 3
paint_box(px, (38, 0), (6, 11, 5), {'top': F, 'bottom': S, 'west': pat(TABARD_SIDE), 'east': pat(TABARD_SIDE),
                                    'north': pat(TABARD), 'south': pat(TABARD_BACK)})
FLAP = ['MMM', 'FFF', 'FFF', 'FFF', 'FFF', 'FMF', 'FMF', 'MFM', 'SMS']
paint_box(px, (38, 16), (3, 9, 1), {'top': F, 'bottom': S, 'side': M, 'north': pat(FLAP), 'south': pat(FLAP)})
paint_box(px, (46, 16), (3, 9, 1), {'top': F, 'bottom': S, 'side': M, 'north': pat(FLAP), 'south': pat(FLAP)})

# ---------------------------------------------------------------------------------------
# jerkin: the jacket (neck cut out, the side device on the chest, lacing below it, a
# leather belt) and its skirt. The lacing is a real ladder of eyelets and cords.
# ---------------------------------------------------------------------------------------
JERKIN = ['FF____FF',
          'FFF__FFF',
          'MFFFFFFM',   # the device sits on rows 2 to 7, columns 2 to 5
          'MFFFFFFM',
          'MFFFFFFM',
          'MFFFFFFM',
          'MFFFFFFM',
          'MFFFFFFM',
          'MMFMMFMM',   # the lacing
          'MMFMMFMM',
          'kLLlsLLk',   # the belt
          'MMFFFFMM']
JERKIN_BACK = ['FFFFFFFF', 'FFFFFFFF', 'MFFFFFFM', 'MFFFFFFM', 'MFFFFFFM', 'MFFFFFFM', 'MFFFFFFM', 'MFFFFFFM',
               'MMFFFFMM', 'MMFMMFMM', 'kLLLLLLk', 'MMFFFFMM']
JERKIN_SIDE = ['FFFF', 'MFFM', 'MFFM', 'MFFM', 'MFFM', 'MFFM', 'MFFM', 'MMMM', 'MMMM', 'MMMM', 'LLLL', 'MMMM']
paint_box(px, (64, 0), (8, 12, 4), {'top': F, 'bottom': S, 'west': pat(JERKIN_SIDE), 'east': pat(JERKIN_SIDE),
                                    'north': pat(JERKIN), 'south': pat(JERKIN_BACK)})
SKIRT = ['MMMMSMMMM', 'FFFFSFFFF', 'MMFFSFFMM']
SKIRT_SIDE = ['MMMMM', 'FFFFF', 'MMMMM']
paint_box(px, (88, 0), (9, 3, 5), {'top': M, 'bottom': S, 'west': pat(SKIRT_SIDE), 'east': pat(SKIRT_SIDE),
                                   'north': pat(SKIRT), 'south': pat(SKIRT)})

# ---------------------------------------------------------------------------------------
# vestment: the Sunweaver's scapular and its run down the robe's tiers, a gilt hem.
# The device sits on rows 2 to 7 of the face.
# ---------------------------------------------------------------------------------------
VEST = ['FFFF', 'FFFF', 'FFFF', 'FFFF', 'FFFF', 'FFFF', 'FFFF', 'FFFF', 'MFFM', 'MFFM', 'MFFM', 'MMMM', 'MMMM']
VEST_SIDE = ['MMMMM'] * 13
paint_box(px, (64, 16), (4, 13, 5), {'top': F, 'bottom': S, 'west': pat(VEST_SIDE), 'east': pat(VEST_SIDE),
                                     'north': pat(VEST), 'south': pat(VEST)})
TIER = ['FFFF', 'FFFF', 'MFFM', 'MFFM']
TIER_LAST = ['FFFF', 'MFFM', 'MFFM', 'GHGG']
paint_box(px, (82, 16), (4, 4, 7), {'top': F, 'bottom': S, 'side': M, 'north': pat(TIER), 'south': pat(TIER)})
paint_box(px, (82, 27), (4, 4, 9), {'top': F, 'bottom': S, 'side': M, 'north': pat(TIER), 'south': pat(TIER)})
paint_box(px, (0, 40), (4, 4, 11), {'top': F, 'bottom': GILT_DARK, 'side': pat(['MMMMMMMMMMM'] * 3 + ['GGGGGGGGGGG']),
                                    'north': pat(TIER_LAST), 'south': pat(TIER_LAST)})

# ---------------------------------------------------------------------------------------
# standard: pole, gilt finial, crossbar, the 6x10 swallowtailed cloth, two brackets.
# The cloth carries a field graded left to right and a shadowed border, so the Marshal's
# banner reads as a banner.
# ---------------------------------------------------------------------------------------
paint_box(px, (124, 0), (1, 34, 1), {'top': WOOD_LIT, 'bottom': WOOD_DARK, 'north': WOOD_LIT, 'west': WOOD,
                                     'east': WOOD, 'south': WOOD_DARK})
paint_box(px, (60, 40), (1, 2, 1), {'top': GILT_LIT, 'bottom': GILT_DARK, 'side': pat(['H', 'G']),
                                    'west': pat(['G', 'J']), 'south': pat(['G', 'J'])})
paint_box(px, (32, 40), (8, 1, 1), {'top': WOOD_LIT, 'bottom': WOOD_DARK, 'side': WOOD,
                                    'north': pat(['GOOOOOOG']), 'south': pat(['GooooooG'])})
STANDARD = ['MFFFFF', 'FFFFFF', 'FFFFFF', 'FFFFFF', 'FFFFFF', 'FFFFFF', 'MFFFFM', 'MF__FM', 'S____S']
paint_box(px, (32, 44), (6, 10, 1), {'top': M, 'bottom': pat(['S____S']), 'west': M, 'east': S,
                                     'north': pat(STANDARD), 'south': pat(STANDARD)})
for uv in ((50, 44), (50, 51)):
    paint_box(px, uv, (1, 1, 6), {'top': LEATHER_LIT, 'bottom': LEATHER_DARK, 'side': LEATHER})

(ROOT / 'art/team').mkdir(parents=True, exist_ok=True)
im.save(ROOT / 'art/team/team_cloth.png')

# ---------------------------------------------------------------------------------------
# The side glyphs. Each is a full-sheet mask, stamped in white by the build on the
# pennant flag, the standard cloth, the chest pale, the jerkin and the vestment.
# Unmirrored for the viewer on both faces.
# ---------------------------------------------------------------------------------------
GLYPHS = {
    'sun': ['....', '.##.', '####', '####', '.##.', '....'],
    'moon': ['....', '.##.', '#...', '#...', '.##.', '....'],
    'star': ['....', '#..#', '.##.', '.##.', '#..#', '....'],
    'wave': ['....', '##..', '..##', '##..', '..##', '....'],
}
flag = faces((4, 0), (4, 8, 1))
standard = faces((32, 44), (6, 10, 1))
pale = faces((18, 0), (4, 10, 5))
jerkin = faces((64, 0), (8, 12, 4))
vest = faces((64, 16), (4, 13, 5))
for name, mark in GLYPHS.items():
    g = Image.new('RGBA', SIZE, (0, 0, 0, 0))
    gp = g.load()
    for face in ('north', 'south'):
        # the pennant flag: rows 1 to 6 of the flag
        x0, y0, x1, y1 = flag[face]
        rows(gp, (x0, y0 + 1, x1, y1), mark, {'#': (255, 255, 255, 255)})
        # the standard cloth: columns 1 to 4, rows 1 to 6
        x0, y0, x1, y1 = standard[face]
        rows(gp, (x0 + 1, y0 + 1, x1 - 1, y1), mark, {'#': (255, 255, 255, 255)})
        # the chest pale: the device on rows 2 to 7 of the face
        x0, y0, x1, y1 = pale[face]
        rows(gp, (x0, y0 + 2, x1, y0 + 8), mark, {'#': (255, 255, 255, 255)})
        # the jerkin: the device on rows 2 to 7, columns 2 to 5
        x0, y0, x1, y1 = jerkin[face]
        rows(gp, (x0 + 2, y0 + 2, x0 + 6, y0 + 8), mark, {'#': (255, 255, 255, 255)})
        # the vestment: the device on rows 2 to 7 of the face
        x0, y0, x1, y1 = vest[face]
        rows(gp, (x0, y0 + 2, x1, y0 + 8), mark, {'#': (255, 255, 255, 255)})
    g.save(ROOT / 'art/team' / ('glyph_%s.png' % name))
print('cloth template and glyphs written')
