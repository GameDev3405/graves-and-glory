"""Checks every face pattern in a troop's skin against the face it paints.

The painter raises on a mismatch, but it stops at the first one. This walks every
troop and every part and reports all the mismatches at once, so a fix round
fixes them all.

  python work/p2b/hx_check.py [troop ...]
"""
import json
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[1]
MODELS = ROOT / 'src/main/resources/assets/gravesandglory/models/troop'
ART = ROOT / 'art/troops'

FACES = ('top', 'bottom', 'west', 'north', 'east', 'south')


def rects(bone, face, box):
    b = bone['boxes'][box]
    (u, v), (w, h, d) = b['uv'], b['size']
    return {'top': (u + d, v, u + d + w, v + d), 'bottom': (u + d + w, v, u + d + 2 * w, v + d),
            'west': (u, v + d, u + d, v + d + h), 'north': (u + d, v + d, u + d + w, v + d + h),
            'east': (u + d + w, v + d, u + 2 * d + w, v + d + h),
            'south': (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}[face]


def main(troops):
    """The face size of every bone and box in every troop, for the painter to check against."""
    for t in troops:
        model = json.loads((MODELS / (t + '.json')).read_text())
        print('=== %s ===' % t)
        for b in model['bones']:
            for i, box in enumerate(b['boxes']):
                sizes = []
                for f in FACES:
                    x0, y0, x1, y1 = rects(b, f, i)
                    sizes.append('%s %dx%d' % (f, x1 - x0, y1 - y0))
                print('  %-16s box %d: %s' % (b['name'], i, ' | '.join(sizes)))


if __name__ == '__main__':
    args = sys.argv[1:] or ['knight', 'archer', 'sentinel', 'longbow', 'mage', 'gl_marshal']
    main(args)
