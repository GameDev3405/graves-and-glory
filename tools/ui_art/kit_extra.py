"""Painters for the UI kit textures no mockup drew (UI plan Task 1; spec section 7 leaves their pixels to the
builder). Each follows the rule of its mock siblings: the order glyphs move, charge and retreat and the formation
glyphs wedge and loose are 9 x 9 ink grids in the GLYPHS style (with the mock's -faded rule); the neutral point
seal is the contested seal's square plate without the swords; the Dusk health fill is hp_fill in the Dusk ramp;
the crescent feed glyph is an FG grid inked as the notices are; the iron Enlist plate is the brass plate in iron."""
from v4lib import Spr, rgba
from v4pal import IRON, BRASS, VIOL
from v4a_hud import glyph, INK, PAPER, FEEDV_GLYPH, SHADOW, iron_shade

# 'k' ink, 'L' the light vellum inside a shape, as in v4a_hud.GLYPHS
GLYPHS = {
    # move: travel to a place, an arrow with a shaft (outlined like the sword and shield)
    'g-move':    ['.........', '.....k...', '.....kk..', 'kkkkkkLk.', 'kLLLLLLLk', 'kkkkkkLk.', '.....kk..', '.....k...', '.........'],
    # charge: commit at speed, a solid head with speed lines behind it
    'g-charge':  ['....k....', '....kk...', 'kk..kkk..', '....kkkk.', 'kkk.kkkkk', '....kkkk.', 'kk..kkk..', '....kk...', '....k....'],
    # retreat: turn back, a U-turn arrow
    'g-retreat': ['...kkkk..', '..k....k.', '..k.....k', '..k.....k', 'kkkkk...k', '.kkk....k', '..k.....k', '........k', '........k'],
    # wedge: the compact glyph's squares as a point, one, two, three
    'g-wedge':   ['...kk....', '...kk....', '.........', '.kk..kk..', '.kk..kk..', '.........', 'kk.kk.kk.', 'kk.kk.kk.', '.........'],
    # loose: the compact glyph's squares spread out
    'g-loose':   ['kk....kk.', 'kk....kk.', '.........', '...kk....', '...kk....', '.........', 'kk....kk.', 'kk....kk.', '.........'],
}


def glyph_ink(name):
    return glyph(GLYPHS[name])


def glyph_faded(name):
    """the mock's -faded rule: ink INK[3], the light parts in the grey (routing) card's light"""
    return glyph(GLYPHS[name], INK[3], PAPER['grey']['light'])


def seal_neutral():
    """the neutral point: seal('B')'s square iron plate, its bead ring and its shadow, without the crossed swords,
    the field bare iron in IRON[4] (a square with nothing on it reads as nobody's, without colour)"""
    size, o = 19, 4
    S = Spr(27, 27)
    outer = {(x, y) for y in range(1, size - 1) for x in range(1, size - 1)}
    inner = {(x, y) for y in range(4, size - 4) for x in range(4, size - 4)}
    for (x, y) in outer:
        k = (x + 1 + o, y + 2 + o)
        if (x + 1, y + 2) not in outer and S.get(*k)[3] < 0.5 * 255:
            S.set(k[0], k[1], rgba(SHADOW, round(0.5 * 255)))
    iron_shade(S, outer, o, o, seed=51 + ord('B'), scratches=False)
    for t in range(3, size - 3, 3):
        for (x, y) in ((t, 2), (t, size - 3), (2, t), (size - 3, t)):
            S.set(o + x, o + y, IRON[7]); S.set(o + x + 1, o + y + 1, IRON[2])
    for (x, y) in inner:
        S.set(o + x, o + y, IRON[4])
    return S


def hp_full_dusk(n=102):
    """v4a_hud.hp_fill's rule in the Dusk ramp: rows VIOL[4], VIOL[3], VIOL[2], the dark edge VIOL[1] in the last column"""
    S = Spr(n, 3)
    for x in range(n):
        for y, c in enumerate((VIOL[4], VIOL[3], VIOL[2])):
            S.set(x, y, c)
    for y in range(3):
        S.set(n - 1, y, VIOL[1])
    return S


# an 8 x 8 crescent in the FG style: 'x' body, 'h' the lit pixels on the upper left, 'd' the shaded side
FG_MOON = ['..hxx...', '.hxx....', 'hxx.....', 'hxx.....', 'xxd.....', 'xxdd....', '.xxddd..', '..xxx...']


def fgv_moon():
    """the Dusk emblem for the feed's deploy and notice lines, inked as v4a_hud.feed_glyph_ink inks 'ivory'"""
    d, m, l = FEEDV_GLYPH['ivory']
    S = Spr(8, 8)
    S.grid(0, 0, FG_MOON, {'x': m, 'h': l, 'd': d})
    return S


def btn_iron(brass):
    """the iron Enlist plate (short of gold): the brass plate with every BRASS[i] pixel replaced by IRON[max(0, i-3)],
    so its face BRASS[5] becomes IRON[2] #1f2427; the shadow, the specular and the tarnish stay as painted"""
    lut = {rgba(c)[:3]: rgba(IRON[max(0, i - 3)])[:3] for i, c in enumerate(BRASS)}
    px = brass.px
    for y in range(brass.h):
        for x in range(brass.w):
            p = px[x, y]
            if p[:3] in lut:
                px[x, y] = lut[p[:3]] + (p[3],)
    return brass
