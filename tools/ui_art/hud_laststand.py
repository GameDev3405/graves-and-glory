"""The LAST STAND pieces of the battle HUD (War Table port Task 6): the stamp a group at its last stand carries in its state slot
on the card and on the chip, and the feed's glyph for its line.

A group at its last stand is still fighting, so its plate keeps the live look (the same vellum, iron or brass caps as a plain
or a selected plate); what marks it is the stamp, a box in red ink with the words LAST STAND (BattleHud draws the words in the
game's own font over it), and a red count. FALLEN keeps the grey plates, the grey art of the rout state it used to share.

The stamp is the rout stamp's own ruled box (v4a_hud.stamp) made 66 px wide, the width of a chip's state slot, since the words
LAST STAND take 58 px of it in the game's font. The chip's is 11 px tall so the pip rail under it (UiMath.pipSlot) stays clear.
The glyph is an 8 x 8 stamp box with a sword planted in it, inked as the rout glyph was (grey, FEEDV_GLYPH['grey']), in the
feed's own style (v4a_hud.feed_glyph_ink)."""
import random

from v4lib import Spr
from v4a_hud import STAMP, FEEDV_GLYPH, FG

# the stamp box: an outer rule worn where the stamp did not bite (a share of its pixels left out); the card's stamp adds an inner
# rule along its top and bottom only, so the words keep 3 px of vellum at each side
STAMP_W = 66
CHIP_STAMP_H, CARD_STAMP_H = 11, 16


def stamp_last(w=STAMP_W, h=CHIP_STAMP_H, seed=7, inner=False):
    """the LAST STAND stamp: the rule 0 px in from the edge, and for the card a second, more worn one 2 px in along the top and
    bottom. It is w - 6 px wide inside the side rules: the words take 58 of the 60. The chip's has 1 px of vellum above and below
    the words (7 px tall), the card's 3"""
    rnd = random.Random(seed)
    S = Spr(w, h)
    for x in range(0, w):
        for y in (0, h - 1):
            if rnd.random() < 0.95:
                S.set(x, y, STAMP)
    for y in range(0, h):
        for x in (0, w - 1):
            if rnd.random() < 0.95:
                S.set(x, y, STAMP)
    if inner:
        for x in range(3, w - 3):
            for y in (2, h - 3):
                if rnd.random() < 0.82:
                    S.set(x, y, STAMP)
    return S


def chip_stamp():
    return stamp_last(STAMP_W, CHIP_STAMP_H, 7)


def card_stamp():
    return stamp_last(STAMP_W, CARD_STAMP_H, 9, True)


# the feed glyph: a stamp box with a sword planted in it. 'x' body, 'h' the lit pixel, 'd' the dark side
FG['fg-laststand'] = ['xxxxxxxx', 'x..hx..x', 'x..xx..x', 'x.xxxx.x', 'x..xx..x', 'x..xd..x', 'x..xd..x', 'xxxxxxxx']


def fgv_laststand():
    """the feed's LAST STAND glyph, inked grey on the vellum slip as the rout line's was"""
    d, m, l = FEEDV_GLYPH['grey']
    S = Spr(8, 8)
    S.grid(0, 0, FG['fg-laststand'], {'x': m, 'h': l, 'd': d})
    return S
