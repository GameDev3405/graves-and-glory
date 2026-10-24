"""Throw-away placeholder face patches for the faces pipeline (texel-3 faces design F3), until the art agent's real
art/troops/<troop>_faces.png (F1, branch texel3-art) lands and replaces them.

  python tools/placeholder_faces.py     write art/troops/<troop>_faces.png for every troop whose json has a faces block

Patch 0 is the canonical head exactly as the troop's stage 1 skin paints it (so variant 0 renders as the head always did); patches
1 to count-1 are tint-shifted copies of it (darker, lighter, cooler), enough to see the system work end to end and to tell the
variants apart at 6 blocks. Deterministic. Not art: nothing here is to be kept once the real patches are in."""
import colorsys
import sys
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))
from PIL import Image
import build_skins as b

# per-channel multipliers of patches 1, 2, 3 (a patch past the third repeats the cycle)
SHIFTS = [(0.72, 0.70, 0.68), (1.18, 1.15, 1.10), (0.90, 1.00, 1.12)]


def shifted(c, k):
    """The colour times patch k's multipliers, its saturation pulled down to 0.40 where the team tint rule (saturation over 0.45 and
    value over 0.60 near a side colour) would object."""
    m = SHIFTS[(k - 1) % len(SHIFTS)]
    rgb = [max(0, min(255, int(c[i] * m[i] + 0.5))) for i in range(3)]
    h, sat, val = colorsys.rgb_to_hsv(*(x / 255 for x in rgb))
    if sat > 0.40 and val > 0.55:
        rgb = [int(x * 255 + 0.5) for x in colorsys.hsv_to_rgb(h, 0.40, val)]
    return tuple(rgb) + (c[3],)


def patches(cat, troop, model):
    """The stacked patches of one faces troop, from its stage 1 skin's head faces."""
    bones = [x for x in model['bones'] if x.get('variants')]
    density = model.get('density', b.TROOP_DENSITY)
    box = bones[0]['boxes'][0]
    (u, v), size = box['uv'], box['size']
    fw, fh = b.face_size(bones[0])
    skin = b.read_png(cat.root / 'art/troops' / (troop + '_s1.png'), troop)
    out = Image.new('RGBA', (fw * density, fh * density * len(bones)), (0, 0, 0, 0))
    for k in range(len(bones)):
        for x0, y0, x1, y1 in b.footprint((0, 0), size):
            for y in range(y0 * density, y1 * density):
                for x in range(x0 * density, x1 * density):
                    c = skin.getpixel((u * density + x, v * density + y))
                    out.putpixel((x, k * fh * density + y), c if k == 0 else shifted(c, k))
    return out


def main():
    cat = b.load(b.ROOT)
    for troop in sorted(cat.troops):
        if 'faces' not in cat.troops[troop]:
            continue
        path = b.ROOT / 'art/troops' / (troop + b.FACES_SUFFIX + '.png')
        patches(cat, troop, b.bake(cat, troop)).save(path)
        print('placeholder_faces: ' + path.relative_to(b.ROOT).as_posix())
    return 0


if __name__ == '__main__':
    sys.exit(main())
