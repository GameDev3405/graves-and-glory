"""Face painters. A painter is a function of a Canvas; a box's paint list runs in order (material first, details after).

Every pattern follows the style guide: one pattern a material, drawn in the face's tone +-1; marks (dents, pits, rust,
scratches) are scattered by hand-like rules from a seed per face, never on a fixed period; the top-left edge of a lit
face takes the highlight and the bottom-right edge the shadow.
"""

LIT = ('front', 'lit', 'top')


def only(*sems):
    """Runs the wrapped painter only on faces of these meanings (front, back, lit, dark, top, bottom)."""
    def wrap(fn):
        def run(c):
            if c.sem in sems:
                fn(c)
        return run
    return wrap


def faces(sems, fn):
    return only(*sems)(fn)


def front(fn):
    return only('front')(fn)


def back(fn):
    return only('back')(fn)


def sides(fn):
    return only('lit', 'dark')(fn)


def outer(fn):
    """The face that looks away from the body: east on the right side, west on the left."""
    def run(c):
        if (c.box.side == 'R' and c.face == 'east') or (c.box.side == 'L' and c.face == 'west'):
            fn(c)
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


def _bevel(c, up=1, down=1):
    """Top row and left column one tone up, bottom row and right column one down, on faces big enough to hold it."""
    if c.w < 2 or c.h < 2:
        return
    if c.sem in ('front', 'lit', 'back', 'dark'):
        for x in range(c.w):
            if c.mat[0, x] is not None:
                c.px(x, 0, d=up)
            if c.mat[c.h - 1, x] is not None:
                c.px(x, c.h - 1, d=-down)
        if c.sem in ('front', 'lit'):
            for y in range(1, c.h - 1):
                if c.mat[y, 0] is not None:
                    c.px(0, y, d=up)
        for y in range(1, c.h - 1):
            if c.mat[y, c.w - 1] is not None:
                c.px(c.w - 1, y, d=-down)
    elif c.sem == 'top':
        # the top's front edge catches the light; its back edge drops
        for x in range(c.w):
            c.px(x, c.h - 1, d=up)
            if c.h > 2:
                c.px(x, 0, d=-1)


def _marks(c, mat, n, d=-1, avoid_edges=True):
    """n single-texel marks (a dent, a pit) at seeded, irregular positions, one tone from the face."""
    if c.w < 3 or c.h < 3:
        return
    used = set()
    for _ in range(n):
        for _try in range(8):
            x = c.rng.randrange(1 if avoid_edges else 0, c.w - (1 if avoid_edges else 0))
            y = c.rng.randrange(1 if avoid_edges else 0, c.h - (1 if avoid_edges else 0))
            if (x, y) in used or (x + 1, y) in used or (x, y + 1) in used or (x - 1, y) in used:
                continue
            if c.mat[y, x] != mat:
                continue
            used.add((x, y))
            c.px(x, y, d=d)
            break


def plate(mat, marks=0.035, bevel=True, tone=None):
    """Plate (steel, iron, enamel, bronze): the face tone, a bevel, scattered hammer marks."""
    def run(c):
        c.fill(mat, c.base if tone is None else tone(c))
        if c.sem in ('front', 'lit', 'back') and c.w >= 3 and c.h >= 3:
            # sheen: the column nearest the light is a tone up on lit faces (style guide section 4, steel)
            if c.sem in ('front', 'lit') and c.w >= 5:
                c.vline(1, 1, c.h - 2, d=1)
            _marks(c, mat, int(c.w * c.h * marks))
        if bevel:
            _bevel(c)
    return run


def flat(mat, d=0):
    """A flat tone from the face table (small faces, trims)."""
    def run(c):
        c.fill(mat, max(0, min(4, c.base + d)))
    return run


def trim(mat):
    """Gilt and brass: tone 3 on lit faces, the top-left texel a tone up, the bottom-right a tone down."""
    def run(c):
        t = {'front': 3, 'lit': 3, 'top': 4, 'back': 2, 'dark': 2, 'bottom': 1}[c.sem]
        c.fill(mat, t)
        if c.w * c.h >= 2:
            c.px(0, 0, d=1)
            if c.w * c.h >= 6:
                c.px(1 if c.w > 2 else 0, 0 if c.w > 2 else 1, d=1)
            c.px(c.w - 1, c.h - 1, d=-1)
    return run


def mail(mat):
    """Mail: a two-texel check of the face tone and one under it (a structure, not a mark, so it may repeat)."""
    def run(c):
        t = max(1, min(3, c.base))
        c.fill(mat, t)
        for y in range(c.h):
            for x in range(c.w):
                if ((x // 1) + (y // 1) + (y // 2)) % 2:
                    c.px(x, y, tone=t - 1)
        for x in range(c.w):
            c.px(x, 0, tone=min(4, t + 1)) if c.sem in LIT else None
    return run


def scales(mat, edge_mat=None):
    """Overlapping scales in staggered rows of 2x2: a lit upper texel, a shadowed lower lip (structure)."""
    def run(c):
        t = max(1, min(3, c.base))
        c.fill(mat, t)
        if c.sem in ('top', 'bottom'):
            return
        for y in range(c.h):
            row = y // 2
            for x in range(c.w):
                k = (x + row) % 2
                if y % 2 == 0:
                    c.px(x, y, tone=t + 1 if k == 0 else t)
                else:
                    c.px(x, y, tone=t - 1 if k == 1 else t)
        if edge_mat:
            c.hline(c.h - 1, mat=edge_mat, tone=max(0, t - 1))
    return run


def cloth(mat, folds=True, hem=True, ragged=0):
    """Cloth: the face tone with fold lines one tone down; a darker hem; optionally a ragged (torn) lower edge."""
    def run(c):
        t = {'front': 2, 'lit': 2, 'back': 2, 'dark': 1, 'top': 3, 'bottom': 0}[c.sem]
        c.fill(mat, t)
        if c.sem in ('front', 'back', 'lit', 'dark') and c.w >= 3:
            if folds:
                xs, x = [], c.rng.randrange(1, 3)
                while x < c.w - 1:
                    xs.append(x)
                    x += c.rng.choice((2, 3, 3, 4))
                for x in xs:
                    top = c.rng.randrange(0, max(1, c.h // 3))
                    c.vline(x, top, c.h - 1, d=-1)
                    if x - 1 >= 0 and c.sem in ('front', 'lit'):
                        c.vline(x - 1, top + 1, c.h - 2, d=1) if c.rng.random() < 0.5 else None
            if c.sem in ('front', 'lit') and c.w >= 2:
                c.vline(0, 0, c.h - 1, d=1)
            if hem and c.h >= 4:
                c.hline(c.h - 1, d=-1)
            if ragged:
                prev = 0
                for x in range(c.w):
                    k = c.rng.choice((0, 0, 1, 1, 2, ragged)) if prev < ragged else c.rng.choice((0, 1))
                    k = min(k, ragged, c.h - 1)
                    for y in range(c.h - k, c.h):
                        c.clear(x, y)
                    if k and c.h - k - 1 >= 0:
                        c.px(x, c.h - k - 1, d=-1)
                    prev = k
    return run


def leather(mat, pits=0.04, stitch=None):
    def run(c):
        c.fill(mat, max(0, min(4, c.base - (1 if c.sem in ('front', 'lit') else 0))))
        _marks(c, mat, int(c.w * c.h * pits))
        if c.sem in ('front', 'lit') and c.w >= 3 and c.h >= 3:
            c.hline(0, d=1)
        if c.sem != 'top' and c.h >= 3:
            c.hline(c.h - 1, d=-1)
        if stitch and c.sem in ('front', 'back', 'lit', 'dark') and c.h >= 4:
            for y in range(1, c.h - 1, 2):
                c.px(0 if c.sem != 'dark' else c.w - 1, y, mat=stitch, tone=3)
    return run


def fur(mat, ragged=2, top_dark=False):
    """Fur: strands down the face (each column a run of the tone above or below), a ragged lower edge."""
    def run(c):
        t = {'front': 2, 'lit': 2, 'back': 2, 'dark': 1, 'top': 3, 'bottom': 1}[c.sem]
        c.fill(mat, t)
        if c.sem in ('top', 'bottom'):
            _marks(c, mat, int(c.w * c.h * 0.12), d=1)
            return
        for x in range(c.w):
            # one strand per column: a lit tip somewhere, a dark root somewhere else
            y = c.rng.randrange(0, max(1, c.h))
            ln = c.rng.randrange(1, 3)
            for k in range(ln):
                c.px(x, y + k, d=1 if (x % 3) else -1)
        if ragged:
            for x in range(c.w):
                k = c.rng.choice([0] + list(range(1, ragged + 1)) * 2)
                k = min(k, c.h - 1)
                for y in range(c.h - k, c.h):
                    c.clear(x, y)
                if c.h - k - 1 >= 0:
                    c.px(x, c.h - k - 1, d=-1)
        if c.sem in ('front', 'lit'):
            c.hline(0, d=1)
    return run


def bone(mat, crack=True):
    def run(c):
        c.fill(mat, c.base if c.sem != 'front' else 3)
        if crack and c.w * c.h >= 6 and c.sem in ('front', 'lit', 'back', 'dark'):
            # one crack, along the long axis (style guide: bone)
            if c.h >= c.w:
                x = c.rng.randrange(0, c.w)
                y0 = c.rng.randrange(0, max(1, c.h // 2))
                for y in range(y0, min(c.h, y0 + max(2, c.h // 2))):
                    c.px(x, y, tone=1)
                    if c.rng.random() < 0.3:
                        x = max(0, min(c.w - 1, x + c.rng.choice((-1, 1))))
            else:
                y = c.rng.randrange(0, c.h)
                x0 = c.rng.randrange(0, max(1, c.w // 2))
                for x in range(x0, min(c.w, x0 + max(2, c.w // 2))):
                    c.px(x, y, tone=1)
        _bevel(c)
    return run


def rust(mat, amount=0.05, under='plate'):
    """Rust blooms on plate: a few 1-2 texel patches of rust tones, more toward the bottom edge (where water sits)."""
    def run(c):
        if c.sem == 'top' or c.w < 3 or c.h < 3:
            return
        n = max(1, int(c.w * c.h * amount))
        for _ in range(n):
            x = c.rng.randrange(0, c.w)
            y = min(c.h - 1, int(c.h * (1 - c.rng.random() ** 1.8)))
            if c.mat[y, x] != under:
                continue
            c.px(x, y, mat=mat, tone=c.rng.choice((1, 2, 2)))
            if c.rng.random() < 0.5 and y + 1 < c.h and c.mat[y + 1, x] == under:
                c.px(x, y + 1, mat=mat, tone=1)
    return run


def patina(mat, amount=0.07, under='plate'):
    """Verdigris on bronze: in the recesses and along the lower edges, in short drips."""
    def run(c):
        if c.w < 2 or c.h < 2:
            return
        n = max(1, int(c.w * c.h * amount))
        for _ in range(n):
            x = c.rng.randrange(0, c.w)
            y = min(c.h - 1, int(c.h * (1 - c.rng.random() ** 1.5)))
            if c.mat[y, x] != under:
                continue
            c.px(x, y, mat=mat, tone=c.rng.choice((2, 2, 3)) if c.sem in LIT else 1)
            if c.rng.random() < 0.4 and y > 0 and c.mat[y - 1, x] == under:
                c.px(x, y - 1, mat=mat, tone=2)
    return run


def barnacles(mat, n=3):
    def run(c):
        if c.w < 3 or c.h < 3 or c.sem == 'bottom':
            return
        for _ in range(n):
            x, y = c.rng.randrange(0, c.w), c.rng.randrange(c.h // 2, c.h)
            c.px(x, y, mat=mat, tone=3)
            if x + 1 < c.w and c.rng.random() < 0.6:
                c.px(x + 1, y, mat=mat, tone=1)
    return run


def rivets(mat, pts):
    """Rivets at face positions: pts is a function (canvas) -> [(x, y)], so placement can depend on the face size."""
    def run(c):
        for x, y in pts(c):
            c.px(x, y, mat=mat, tone=4 if c.sem in LIT else 3)
            c.px(x, y + 1, d=-1) if (y + 1 < c.h and c.mat[y + 1, x] != mat) else None
    return run


def band(mat, rows, painter=None, kind='trim'):
    """A horizontal band of a trim material across the given rows (rarity accents ride on these)."""
    def run(c):
        if c.sem in ('top', 'bottom'):
            return
        for i, y in enumerate(rows(c) if callable(rows) else rows):
            yy = y if y >= 0 else c.h + y
            if not 0 <= yy < c.h:
                continue
            for x in range(c.w):
                if c.mat[yy, x] is None:
                    continue
                t = {'front': 3, 'lit': 3, 'back': 2, 'dark': 1}[c.sem]
                if kind == 'trim' and i == 0:
                    t += 0
                c.px(x, yy, mat=mat, tone=t)
            if c.sem in ('front', 'lit'):
                c.px(0, yy, d=1)
            c.px(c.w - 1, yy, d=-1)
    return run


def edge(mat, which=('bottom',), d=0):
    """Trim along the named edges of side faces (top, bottom, left, right)."""
    def run(c):
        if c.sem in ('top', 'bottom'):
            if 'cap' in which:
                c.fill(mat, 3 if c.sem == 'top' else 1)
            return
        t = {'front': 3, 'lit': 3, 'back': 2, 'dark': 1}[c.sem] + d
        for w in which:
            if w == 'bottom':
                c.hline(c.h - 1, mat=mat, tone=t - 1)
            elif w == 'top':
                c.hline(0, mat=mat, tone=t + 1 if c.sem in LIT else t)
            elif w == 'left':
                c.vline(0, mat=mat, tone=t)
            elif w == 'right':
                c.vline(c.w - 1, mat=mat, tone=t - 1)
    return run


def glow_dots(mat, pts, tone=3):
    def run(c):
        for x, y in pts(c):
            c.px(x, y, mat=mat, tone=tone)
    return run


def draw(fn):
    """A one-off detail: fn(c) draws directly (used for faces, emblems and slits)."""
    return fn
