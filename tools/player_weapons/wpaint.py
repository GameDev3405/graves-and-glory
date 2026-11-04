"""Weapon painters (style guide sections 3, 4, 5 and 7.1), at 2 texels a model px.

At the hand (within 1 px of the grip) detail is painted in 2x2 texel units so it stays on the skin's grid; one-texel
detail goes on blades, limbs and heads away from the hand.
"""
from player_armor import paint as P

# the face tones of section 3, by meaning (weapons: south front, west lit)
TONE = {'top': 4, 'front': 3, 'lit': 3, 'back': 2, 'dark': 1, 'bottom': 0}


def blade_flat(lit=True, glints=((0.08,), (0.85,))):
    """One flat of a blade (a cube either side of the fuller). Front: the lit flat tone 4, the shade flat tone 3; the back
    one tone down. A bevel line: the outer column keeps the flat's tone and the inner drops one. Polish glints near the
    guard and at the point's shoulder (a texel a tone up, not on a fixed spacing). Edges: the face table."""
    def run(c):
        if c.sem in ('front', 'back'):
            t = (4 if lit else 3) - (1 if c.sem == 'back' else 0)
            c.fill('plate', t)
            if c.w >= 2:
                outer = 0 if (lit == (c.sem == 'front')) else c.w - 1
                for x in range(c.w):
                    if x != outer:
                        c.vline(x, d=-1)
            for (f,) in glints:
                y = int(round((1 - f) * (c.h - 1)))
                x = 1 if c.w > 2 else 0
                c.px(x, y, d=1)
        else:
            c.fill('plate', TONE[c.sem])
    return run


def fuller(c):
    c.fill('plate', 2 if c.sem in ('front',) else 1 if c.sem in ('back', 'dark', 'bottom') else TONE[c.sem])


def edge_steel(c):
    """A small steel part (a point cube, an edge cube): the face table, a lit top-left texel."""
    c.fill('plate', TONE[c.sem])
    c.px(0, 0, d=1)


def wrap(mat, bands=('a', 'b')):
    """A wound grip: bands across in whole pixels (two texel rows each), tone 3 and 2 on the front, 2 and 1 behind."""
    def run(c):
        hi = {'front': 3, 'lit': 3, 'top': 3, 'back': 2, 'dark': 2, 'bottom': 1}[c.sem]
        c.fill(mat, hi)
        if c.sem in ('top', 'bottom'):
            return
        for y in range(c.h):
            if (y // 2) % 2 == 1:
                c.hline(y, tone=hi - 1)
    return run


def wood(mat='wood', grain=True, flecks=2):
    """Wood: the face tone, one grain line along the long axis one tone down (the +x column on a front face), a few
    light flecks on the lit edge."""
    def run(c):
        c.fill(mat, TONE[c.sem] if c.sem not in ('top',) else 3)
        if c.sem in ('top', 'bottom') or c.w < 2:
            return
        if grain:
            c.vline(c.w - 1 if c.sem in ('front', 'lit') else 0, d=-1)
        for _ in range(flecks):
            if c.h >= 6:
                c.px(0, c.rng.randrange(1, c.h - 1), d=1)
    return run


def metal(mat='plate', marks=0.02):
    return P.plate(mat, marks=marks)


def trim(mat):
    return P.trim(mat)


def flat(mat, d=0):
    def run(c):
        c.fill(mat, max(0, min(4, TONE[c.sem] + d)))
    return run


def band_rows(mat, rows):
    """Bands across the side faces at texel rows (negative from the bottom), in whole pixels when given as pairs."""
    def run(c):
        if c.sem in ('top', 'bottom'):
            return
        for y in rows:
            yy = y if y >= 0 else c.h + y
            if 0 <= yy < c.h:
                c.hline(yy, mat=mat, tone=TONE[c.sem])
    return run


def ends(mat, which='both', rows=2):
    """Paints the first and/or last `rows` texel rows of the side faces in another material (a collar, a cap)."""
    def run(c):
        if c.sem in ('top', 'bottom'):
            if which in ('both', 'top' if c.sem == 'top' else 'bottom'):
                c.fill(mat, TONE[c.sem])
            return
        t = TONE[c.sem]
        if which in ('both', 'top'):
            c.rect(0, 0, c.w, rows, mat=mat, tone=t)
            c.px(0, 0, d=1)
        if which in ('both', 'bottom'):
            c.rect(0, c.h - rows, c.w, rows, mat=mat, tone=max(0, t - 1))
    return run


def glow_tip(mat='ember', where='top'):
    """The tip of a part lit: its top face and the top texel row of its sides."""
    def run(c):
        if c.sem == 'top' and where == 'top':
            c.fill(mat, 4)
        elif c.sem in ('front', 'lit', 'back', 'dark') and where == 'top':
            c.hline(0, mat=mat, tone=3)
    return run


def legendary(fn):
    def run(c):
        if c.legendary:
            fn(c)
    return run


def base(fn):
    def run(c):
        if not c.legendary:
            fn(c)
    return run


def on(sems, fn):
    def run(c):
        if c.sem in sems:
            fn(c)
    return run
