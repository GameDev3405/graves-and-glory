"""Giant's Rune Stone (`rune_stone`): 2x3x2 cells, anchor cell (0,0,0). Brief: art/props/rune_stone/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..32, y 0..48, z 0..32 px. The prop is exactly 3 cells tall, so each part
stays one part stored shifted down 16 px (y -16..32).

A standing stone of the giants: a broad tapering slab on a squat foot, a sunk panel on its face carved with four
giant-scale runes, each rune two hands high. The runes hold a faint cold light (their own full-bright part).
Build:  python art/props/build_props.py rune_stone      Card:  python tools/prop_card.py rune_stone
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, rects   # noqa: F401
from giant_bones import mound
from proplib import make_ramp
from giant_relics import stone

RUNE_DIM = make_ramp("4E8A7E", 0.16)   # the runes' faint cold light: dim teal, full-bright part


def rune_core(face, P, c, r, w, h):
    """A darker core: on a 2 px stroke the lower/right texel row is a tone down (light caught on one wall of a groove);
    on a wider box its inner texels are."""
    if w >= 3 and h >= 3: return -1 if 0 < c < w - 1 and 0 < r < h - 1 else 0
    if w <= 2 and h > 2: return -1 if c == 1 else 0
    if h <= 2 and w > 2: return -1 if r == 1 else 0
    return 0

# Runes as 2 px cells, rows top to bottom, column 0 the viewer's left (= the higher x on the north face).
RUNES = [
    ["#.#", "#.#", "###", ".#.", ".#."],     # a forked stave
    ["#..", "##.", "#.#", "##.", "#.."],     # a stave with two hooked branches
]
def rune_boxes(glyph, xr, y_top, zf, mat):
    """One rune: its 2 px cells merged into few boxes, 1 px proud of the face at z = zf."""
    cells = {(c, r) for r, row in enumerate(glyph) for c, ch in enumerate(row) if ch == "#"}
    out = []
    for c0, r0, c1, r1 in rects(cells):
        x1, x0 = xr - 2 * c0, xr - 2 * c1
        out.append(E((x0, y_top - 2 * r1, zf - 1), (x1, y_top - 2 * r0, zf), mat, skip="south", name="rune"))
    return out


def build():
    S = stone(moss_h=7, seed=3, edges=False)   # no per-box seams: one stone, not stacked slabs
    S2 = stone(moss_h=3, seed=4)
    body = [
        # a rough menhir: a buried root, then courses of the one stone each a little narrower and leaning east,
        # the face stepping back once, the crown broken unevenly
        E((4, 0, 8), (28, 8, 24), S, skip="down", name="root"),
        E((6, 8, 11), (26, 24, 22), S, name="lower body"),
        E((7, 24, 12), (26, 36, 22), S, skip="down", name="upper body"),
        E((8, 36, 12), (25, 42, 21), S, skip="down", name="shoulder"),
        E((9, 42, 13), (20, 47, 20), S, skip="down", name="crown"),
        E((20, 42, 14), (24, 44, 20), S, skip="down", name="broken crown"),
        # the west side tapers in one long chipped face, leaning toward the crown
        E((5, 6, 12), (9, 38, 21), S, skip="down east", name="west taper", rot=("z", -22.5, (5, 6, 12))),
        # a big flake gone from the east shoulder
        E((22, 30, 12), (27, 42, 21), S, skip="west", name="east chip", rot=("z", 22.5, (27, 30, 12))),
        # fallen chips at the foot
        E((24, 0, 25), (30, 4, 30), S2, skip="down", name="fallen chip", rot=("y", 22.5, (24, 0, 25))),
        E((2, 0, 4), (6, 2, 7), S2, skip="down", name="pebble"),
    ]
    body += mound((0, 0, 3), (32, 4, 30), layers=2, seed=5)
    runes = []
    M = Mat(RUNE_DIM, 1, rune_core, edges=False, lit=False)   # dim, with a darker core along each stroke
    runes += rune_boxes(RUNES[0], 20, 36, 12, M)     # on the upper body, y 26..36
    runes += rune_boxes(RUNES[1], 19, 22, 11, M)     # on the lower body, y 12..22
    return Prop("rune_stone", {"rune_stone": body, "rune_stone_runes": runes},
                glow=("rune_stone_runes",), shift={"rune_stone": (0, 16, 0), "rune_stone_runes": (0, 16, 0)})
