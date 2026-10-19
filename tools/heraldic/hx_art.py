"""Heraldic units: the shared art library (my own version of the painters).

Built on the project's own conventions (work/p2b/skins.py) but with the things
the baseline lacks: five-tone ramps instead of three, ambient occlusion where
forms meet, edge lines (dark on the shadow side, light on the lit side), and
material texture (mail rings, cloth folds, wood and leather grain).

Light comes from the top front left, the same way the renderer shades, so a
painted highlight and a lit face agree.

  python work/p2b/hx_skins.py [troop ...]
"""
import json
import pathlib

from PIL import Image

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[1]
MODELS = ROOT / 'src/main/resources/assets/gravesandglory/models/troop'
ART = ROOT / 'art/troops'
CLEAR = (0, 0, 0, 0)

# --------------------------------------------------------------------------------------
# The palette. Every material is a ramp of five or six tones, hue-shifted: the lit end
# warms toward the light, the shadow end cools toward the ambient (blue-violet for
# steel, brown for cloth and leather). A ramp is the unit of craft, not a colour.
# --------------------------------------------------------------------------------------
P = {k: tuple(int(v[i:i + 2], 16) for i in (1, 3, 5)) + (255,) for k, v in {
    # enamel: the Order's white surcoat and tabard (warm off-white)
    'E0': '#FBF9F3', 'E1': '#EDE9DC', 'E2': '#D8D3C3', 'E3': '#B8B2A3', 'E4': '#928D80', 'E5': '#6A675F',
    # steel: plate, helms, blades (cool, blue in the shadows)
    'S0': '#E2E7ED', 'S1': '#BCC3CE', 'S2': '#98A0AC', 'S3': '#737A87', 'S4': '#51565F', 'S5': '#34383F',
    # mail: darker and warmer than plate, so the two never read the same
    'M0': '#C3CAD4', 'M1': '#A0A8B4', 'M2': '#7F8693', 'M3': '#5E6470', 'M4': '#42464E',
    # gilt: the Order's trim, warm and desaturated so it passes the team-tint rule
    'G0': '#D4BE8C', 'G1': '#B09662', 'G2': '#8B7040', 'G3': '#634E2B', 'G4': '#42341D',
    # skin, young
    'K0': '#F0C9A6', 'K1': '#D8AD88', 'K2': '#B78B6C', 'K3': '#8F6752', 'K4': '#68493A',
    # skin, weathered and old
    'W0': '#DCAF8C', 'W1': '#BF9271', 'W2': '#9C7057', 'W3': '#77533F', 'W4': '#56392C',
    # hair, brown then grey
    'H0': '#9A724E', 'H1': '#7A563A', 'H2': '#553B28', 'H3': '#3A281B',
    'R0': '#DCD8D0', 'R1': '#B6B2AA', 'R2': '#8B8880', 'R3': '#5E5C57',
    # linen: the archer's cloth
    'N0': '#EFE9D6', 'N1': '#DCD5BE', 'N2': '#C0B8A2', 'N3': '#9B9481', 'N4': '#746E60',
    # yeoman brown: the longbow's felt and leather
    'Y0': '#8E7050', 'Y1': '#6E5539', 'Y2': '#513C28', 'Y3': '#37281B',
    # leather: desaturated, so it never drifts into the gold side colour
    'L0': '#8F7A62', 'L1': '#715F4A', 'L2': '#554637', 'L3': '#3C3126', 'L4': '#282019',
    # wood
    'O0': '#9A8568', 'O1': '#7B6A50', 'O2': '#5C4F3A', 'O3': '#3E3627',
    # sun-white: the Sunweaver
    'U0': '#FFFBF0', 'U1': '#F7EDD0', 'U2': '#E6DAB6', 'U3': '#CBBB99', 'U4': '#A29376',
    # blackened steel: the Marshal
    'B0': '#8B939D', 'B1': '#656D77', 'B2': '#495059', 'B3': '#343A41', 'B4': '#23272C',
    # eyes and mouth
    'EW': '#F2EFE8', 'EP': '#243244', 'EB': '#3A4A5E', 'MO': '#8A5646', 'MW': '#C09A86',
    # bone: the teeth, the fletching
    'BONE': '#DCD6C4',
}.items()}

# One ramp step down, and one up. A painter moves a form through these to model it.
DARKER = {'E0': 'E1', 'E1': 'E2', 'E2': 'E3', 'E3': 'E4', 'E4': 'E5',
           'S0': 'S1', 'S1': 'S2', 'S2': 'S3', 'S3': 'S4', 'S4': 'S5',
           'M0': 'M1', 'M1': 'M2', 'M2': 'M3', 'M3': 'M4',
           'G0': 'G1', 'G1': 'G2', 'G2': 'G3', 'G3': 'G4',
           'K0': 'K1', 'K1': 'K2', 'K2': 'K3', 'K3': 'K4',
           'W0': 'W1', 'W1': 'W2', 'W2': 'W3', 'W3': 'W4',
           'H0': 'H1', 'H1': 'H2', 'H2': 'H3', 'R0': 'R1', 'R1': 'R2', 'R2': 'R3',
           'N0': 'N1', 'N1': 'N2', 'N2': 'N3', 'N3': 'N4',
           'Y0': 'Y1', 'Y1': 'Y2', 'Y2': 'Y3',
           'L0': 'L1', 'L1': 'L2', 'L2': 'L3', 'L3': 'L4',
           'O0': 'O1', 'O1': 'O2', 'O2': 'O3',
           'U0': 'U1', 'U1': 'U2', 'U2': 'U3', 'U3': 'U4',
           'B0': 'B1', 'B1': 'B2', 'B2': 'B3', 'B3': 'B4',
           'MO': 'MW', 'MW': 'MO'}
LIGHTER = {v: k for k, v in DARKER.items()}


def darker(code, steps=1):
    for _ in range(steps):
        code = DARKER.get(code, code)
    return code


def lighter(code, steps=1):
    for _ in range(steps):
        code = LIGHTER.get(code, code)
    return code


# --------------------------------------------------------------------------------------
# Row helpers. A face is painted from text rows of palette codes.
# --------------------------------------------------------------------------------------
def rows(*specs):
    """rows((n, 'A B C D'), ...): n copies of each row."""
    out = []
    for n, r in specs:
        out += [r] * n
    return out


def mirror(lines):
    """Mirrors a set of rows left to right."""
    if callable(lines):
        return lambda w, h: mirror(lines(w, h))
    return [' '.join(reversed(r.split())) for r in lines]


def grid(lines, key):
    """One-character rows through a key to two-character codes."""
    return [' '.join(key[c] for c in r) for r in lines]


def ao(lines, at='bottom', code=None):
    """Ambient occlusion: the row where a form meets another form goes one step down."""
    out = [list(r.split()) for r in lines]
    if at == 'bottom' and out:
        out[-1] = [(code or darker(c)) for c in out[-1]]
    elif at == 'top' and out:
        out[0] = [(code or darker(c)) for c in out[0]]
    elif at == 'left':
        for row in out:
            if row:
                row[0] = code or darker(row[0])
    elif at == 'right':
        for row in out:
            if row:
                row[-1] = code or darker(row[-1])
    return [' '.join(r) for r in out]


def rim(lines, at='left', code=None):
    """A lit edge: the row or column facing the light goes one step up."""
    out = [list(r.split()) for r in lines]
    if at == 'left':
        for row in out:
            if row:
                row[0] = code or lighter(row[0])
    elif at == 'right':
        for row in out:
            if row:
                row[-1] = code or lighter(row[-1])
    elif at == 'top' and out:
        out[0] = [code or lighter(c) for c in out[0]]
    elif at == 'bottom' and out:
        out[-1] = [code or lighter(c) for c in out[-1]]
    return [' '.join(r) for r in out]


def band(lines, at, code, width=1):
    """A band of one code along an edge: the hem, the belt, the border."""
    out = [list(r.split()) for r in lines]
    h = len(out)
    for j, row in enumerate(out):
        if at == 'bottom' and j >= h - width:
            row = [code] * len(row)
        elif at == 'top' and j < width:
            row = [code] * len(row)
        elif at == 'left':
            row = [code] * width + row[width:]
        elif at == 'right':
            row = row[:-width] + [code] * width
        out[j] = row
    return [' '.join(r) for r in out]


def column(lines, index, code):
    """One column of one code, used for a fold or a seam."""
    out = [list(r.split()) for r in lines]
    for row in out:
        if 0 <= index < len(row):
            row[index] = code
    return [' '.join(r) for r in out]


# --------------------------------------------------------------------------------------
# Material generators. Each returns rows of codes for a face of the given size.
# --------------------------------------------------------------------------------------
def mail(w, n, start=0, hi='M0', mid='M1', lo='M2'):
    """Chain mail: a nearly flat dark field with a sparse shadow mottle.

    At 4 texels wide (2 screen pixels at the 36 px read size) any regular
    pattern collapses into a checkerboard, which reads as noise and fights the
    face and the mark for the eye. So the mail is almost flat: the mid tone,
    with a shadow speckled sparsely through it and the lit edge left to the
    caller. The mottle is deliberately irregular, never a repeat.
    """
    out = []
    for j in range(n):
        row = []
        for i in range(w):
            if (i * 7 + j * 13 + start * 3) % 11 == 0:
                row.append(lo)
            else:
                row.append(mid)
        out.append(' '.join(row))
    return out


def cylinder(w, h, tones, fold=None, fold_col=None):
    """A rounded form lit from the left.

    `tones` runs from the lit side to the shadowed side. The falloff is weighted
    the way a lit cylinder actually reads: a narrow lit band, a broad middle, a
    narrow shadowed band, and the darkest tone only at the very edge. Distributing
    the tones evenly put half the width in the darkest tone, so the form read as
    two flat halves instead of a round one.

    `fold` is one column that takes `fold_col` (a crease in the cloth), placed
    by fraction of the width so it never lands on the lit or shadowed edge.
    """
    out = []
    n = len(tones)
    fcol = int(w * fold) if fold is not None and fold_col else None
    # the share of the width each tone takes, lit side first
    weights = [0.12, 0.30, 0.30, 0.18, 0.10][:n] if n <= 5 else [1.0 / n] * n
    total = sum(weights)
    bounds = []
    acc = 0.0
    for wgt in weights:
        acc += wgt / total
        bounds.append(acc)
    for j in range(h):
        row = []
        for i in range(w):
            if fcol is not None and i == fcol and 0 < j < h - 1:
                row.append(fold_col)
                continue
            t = (i + 0.5) / w
            k = 0
            while k < n - 1 and t > bounds[k]:
                k += 1
            row.append(tones[k])
        out.append(' '.join(row))
    return out


def plate(w, n, base='S1', lit='S0', dark='S3', deep='S4', seam=None, seam_col=None):
    """Plate: a lit edge down the left, a graded field, a shadowed edge down the right, and
    an optional seam line at a column."""
    out = []
    for j in range(n):
        row = []
        for i in range(w):
            if seam is not None and i == seam:
                row.append(seam_col or dark)
            elif i == 0:
                row.append(lit)
            elif i == w - 1:
                row.append(dark)
            else:
                row.append(base)
        out.append(' '.join(row))
    if seam is not None:
        out = ao(out, 'bottom', deep)
    return out


def cloth(w, n, base='E2', lit='E1', dark='E3', folds=(), fold_col=None, hem=None, hem_col=None):
    """Cloth: a graded field with fold lines at the given columns and a hem band."""
    out = []
    for j in range(n):
        row = []
        for i in range(w):
            if i == 0:
                row.append(lit)
            elif i == w - 1:
                row.append(dark)
            else:
                row.append(base)
        out.append(' '.join(row))
    for f in folds:
        out = column(out, f, fold_col or dark)
    if hem:
        out = band(out, 'bottom', hem_col or dark, hem)
    return out


def wood(w, n, base='O1', lit='O0', dark='O2'):
    """Wood: long grain lines running with the length."""
    out = []
    for j in range(n):
        row = []
        for i in range(w):
            if i == 0:
                row.append(lit)
            elif i == w - 1:
                row.append(dark)
            elif (i + j) % 4 == 0:
                row.append(dark)
            else:
                row.append(base)
        out.append(' '.join(row))
    return out


def leather(w, n, base='L1', lit='L0', dark='L2'):
    """Leather: a soft field with a few grain specks."""
    out = []
    for j in range(n):
        row = []
        for i in range(w):
            if i == 0:
                row.append(lit)
            elif i == w - 1:
                row.append(dark)
            elif (i * 3 + j * 5) % 7 == 0:
                row.append(dark)
            else:
                row.append(base)
        out.append(' '.join(row))
    return out


# --------------------------------------------------------------------------------------
# Size-aware face generators. Every one of these returns exactly w x h rows, so a
# pattern can never mismatch the face it paints. The hand-written row patterns stay
# for the small, bespoke faces (a face, a helm's slit); the big fields use these.
# --------------------------------------------------------------------------------------
def vgrad(w, h, codes, mid_at=None):
    """A vertical ramp: `codes` is a list of tone codes, distributed down the height.
    One code fills the whole face; two split it at the middle; three give a top
    highlight, a field and a shadowed foot."""
    if isinstance(codes, str):
        codes = [codes]
    if len(codes) == 1:
        return [' '.join([codes[0]] * w)] * h
    out = []
    for j in range(h):
        if len(codes) == 2:
            c = codes[0] if j < (h + 1) // 2 else codes[1]
        else:
            mid_at = mid_at if mid_at is not None else max(1, h // 3)
            if j < mid_at:
                c = codes[0]
            elif j < h - max(1, h // 4):
                c = codes[1]
            else:
                c = codes[2]
        out.append(' '.join([c] * w))
    return out


def hgrad(w, h, left, mid, right):
    """A horizontal ramp across the face: a lit edge, a field, a shadowed edge."""
    out = []
    for _ in range(h):
        row = []
        for i in range(w):
            if i == 0:
                row.append(left)
            elif i == w - 1:
                row.append(right)
            else:
                row.append(mid)
        out.append(' '.join(row))
    return out


def field(w, h, code, lit=None, dark=None):
    """A flat field with a lit top row and a shadowed bottom row (or given edges)."""
    out = []
    for j in range(h):
        if j == 0 and lit:
            out.append(' '.join([lit] * w))
        elif j == h - 1 and dark:
            out.append(' '.join([dark] * w))
        else:
            out.append(' '.join([code] * w))
    return out


def rgrad(w, h, codes):
    """A ramp across both axes: the diagonal from the lit corner to the shadowed one."""
    out = []
    n = len(codes)
    for j in range(h):
        row = []
        for i in range(w):
            t = (i + (h - 1 - j)) / max(1, w + h - 2)
            row.append(codes[min(n - 1, int(t * n))])
        out.append(' '.join(row))
    return out


def box_uv(w, h):
    """The rows of a plain field, for a face that only needs a colour."""
    return None



# --------------------------------------------------------------------------------------
# The painter: a troop's skin over its baked model, addressed by bone, box and face.
# --------------------------------------------------------------------------------------
class Painter:
    def __init__(self, troop):
        self.model = json.loads((MODELS / (troop + '.json')).read_text())
        self.size = tuple(self.model['size'])
        self.im = Image.new('RGBA', self.size, CLEAR)
        self.px = self.im.load()
        self.bones = {b['name']: b for b in self.model['bones']}

    def rect(self, bone, face, box=0):
        b = self.bones[bone]['boxes'][box]
        (u, v), (w, h, d) = b['uv'], b['size']
        return {'top': (u + d, v, u + d + w, v + d), 'bottom': (u + d + w, v, u + d + 2 * w, v + d),
                'west': (u, v + d, u + d, v + d + h), 'north': (u + d, v + d, u + d + w, v + d + h),
                'east': (u + d + w, v + d, u + 2 * d + w, v + d + h),
                'south': (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}[face]

    def dims(self, bone, face, box=0):
        x0, y0, x1, y1 = self.rect(bone, face, box)
        return x1 - x0, y1 - y0

    def put(self, bone, face, lines, key=None, box=0, check=True):
        """Paints a face from rows of palette codes; '..' leaves a texel, '__' clears it."""
        x0, y0, x1, y1 = self.rect(bone, face, box)
        w, h = x1 - x0, y1 - y0
        grid_ = [row.split() if key is None else list(row) for row in lines]
        if check and (len(grid_) != h or any(len(r) != w for r in grid_)):
            raise ValueError('%s %s box %d is %dx%d, the pattern is %dx%d' %
                             (bone, face, box, w, h, len(grid_[0]) if grid_ else 0, len(grid_)))
        for j, row in enumerate(grid_):
            for i, code in enumerate(row):
                if code in ('..', '.'):
                    continue
                self.px[x0 + i, y0 + j] = CLEAR if code in ('__', '_') else P[code]

    def fill(self, bone, face, code, box=0):
        w, h = self.dims(bone, face, box)
        self.put(bone, face, [' '.join([code] * w)] * h, box=box)

    def boxfill(self, bone, codes, box=0):
        for face in ('top', 'bottom', 'west', 'north', 'east', 'south'):
            c = codes.get(face, codes.get('side'))
            if c:
                self.fill(bone, face, c, box=box)

    def paint(self, bone, face, spec, box=0):
        """Paints a face from a size-aware spec: a callable taking (w, h) and returning
        the rows, or a list of rows that must already match the face. A callable can
        never mismatch, so a face is always filled to its edge."""
        w, h = self.dims(bone, face, box)
        lines = spec(w, h) if callable(spec) else spec
        self.put(bone, face, lines, box=box)

    def save(self, name):
        self.im.save(ART / name)


def limb(p, bone, lines, box=0):
    """A limb box: the front as painted, the lit (west) side mirrored so its light edge
    meets the front, the shaded (east) side a step down, and the back a step down."""
    p.paint(bone, 'north', lines, box=box)
    p.paint(bone, 'west', mirror(lines), box=box)
    p.paint(bone, 'east', lambda w, h, s=lines: _step_down(mirror(s)(w, h)), box=box)
    p.paint(bone, 'south', lambda w, h, s=lines: _step_down(s(w, h)), box=box)


def _step_down(lines):
    """Every code one ramp step darker: the face turned from the light."""
    out = []
    for r in lines:
        row = []
        for c in r.split():
            row.append(darker(c))
        out.append(' '.join(row))
    return out
