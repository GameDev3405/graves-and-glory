"""The Iron Ledger's veteran pieces (War Table port Task 7, progression Task 12): the pips a group's chip and card show for its
living soldiers at level 2 and level 3, the glint a plate shows while an Armoury buy for one of its troops is open, and the
feed's glyph for a level-3 line.

Colour never carries any of it (spec 6.1). A pip is a rank chevron like the one a troop wears over its head in the field: an
open chevron for a level-2 soldier, a solid one for a level-3, so the two ranks differ in fill and a row of pips counts the
veterans of the group, the higher rank first. The glint is a four-point spark that grows and shrinks over four frames (BattleHud
steps them), so it is told from the plate's own ornaments by its motion as well as its shape, and it carries a dark rim, so it
reads on the dark iron of a corner screw, on brass and on vellum alike. The feed glyph is two stacked chevrons in an 8 x 8 box,
drawn solid for our own line and cut through for an enemy's, as the point seals are (ours versus theirs by shape).

The pips are 5 x 3 px, laid 6 px apart (UiMath.pipSlot): on a chip on the rail under its strength bar, on a card to the right of
its name, so no plate of the column grows and the bar, the stamp and the count never meet them. A plus mark of 3 x 3 stands for
the veterans past what the row holds."""
from v4lib import Spr
from v4pal import INK
from v4a_hud import FG, feed_glyph_ink

PIP_W, PIP_H = 5, 3

# 'x' the chevron, 'h' its lit apex
PIP2 = ['..h..', '.x.x.', 'x...x']
PIP3 = ['..h..', '.xxx.', 'xxxxx']
MORE = ['.x.', 'xxx', '.x.']


def pip(rows):
    S = Spr(len(rows[0]), len(rows))
    S.grid(0, 0, rows, {'x': INK[1], 'h': INK[3]})
    return S


def pip_l2():
    return pip(PIP2)


def pip_l3():
    return pip(PIP3)


def pip_more():
    return pip(MORE)


# ---------------------------------------------------------------- the glint: four frames of a 9 x 9 spark, side by side
GLINT_N, GLINT_S = 4, 9
CORE, HALO, RIM = '#fff6d0', '#f0d9a0', '#2b2113'


def spark(arm_v, arm_d):
    """the pixels of a spark: a cross of arm_v pixels each way from the middle and a diagonal of arm_d each way"""
    c = GLINT_S // 2
    pts = {(c, c)}
    for i in range(1, arm_v + 1):
        pts |= {(c + i, c), (c - i, c), (c, c + i), (c, c - i)}
    for i in range(1, arm_d + 1):
        pts |= {(c + i, c + i), (c - i, c + i), (c + i, c - i), (c - i, c - i)}
    return pts


def glint():
    """the glint sheet, 36 x 9: a dot, a small cross, the full spark, a small cross again; each one bright core, a halo of the
    lighter brass beside it, and a dark rim round the whole"""
    S = Spr(GLINT_S * GLINT_N, GLINT_S)
    for f, (av, ad) in enumerate(((0, 0), (2, 0), (4, 1), (2, 0))):
        pts = spark(av, ad)
        rim = {(x + dx, y + dy) for (x, y) in pts for dx in (-1, 0, 1) for dy in (-1, 0, 1)} - pts
        for (x, y) in sorted(rim):
            if 0 <= x < GLINT_S and 0 <= y < GLINT_S:
                S.over(f * GLINT_S + x, y, RIM, 200)
        for (x, y) in sorted(pts):
            near = abs(x - GLINT_S // 2) + abs(y - GLINT_S // 2)
            S.set(f * GLINT_S + x, y, CORE if near <= 1 + (1 if av >= 4 else 0) else HALO)
    return S


# ---------------------------------------------------------------- the feed glyph: two chevrons, solid for ours, cut for theirs
FG['fg-levelup'] = ['...hx...', '..hxxd..', '.hx..xd.', 'hx....xd', '...hx...', '..hxxd..', '.hx..xd.', 'hx....xd']
# the enemy's: the same, a diagonal cut ('k', left as bare vellum) through it, as fg-seal-x is cut
FG['fg-levelup-x'] = ['...hx...', '..hxxd..', '.hx.kxd.', 'hx..k.xd', '...hxk..', '..hxkd..', '.hxk.xd.', 'hxk...xd']


def fgv_levelup(kind, enemy=False):
    return feed_glyph_ink('fg-levelup-x' if enemy else 'fg-levelup', kind)
