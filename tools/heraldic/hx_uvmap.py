"""Prints the used and free UV space on a troop's 128x64 sheet.

Every box claims a rectangle: the 2D layout of a ModelPart.Cuboid is
(u + d, v, u + d + w, v + d + h) for the side faces, plus the top and bottom
strips above them. The build refuses two boxes that overlap, so new geometry
needs free space.

  python work/p2b/hx_uvmap.py [troop ...]
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
MODELS = ROOT / 'src/main/resources/assets/gravesandglory/models/troop'
W, H = 128, 64


def rects(box):
    (u, v), (w, h, d) = box['uv'], box['size']
    return [
        ('top', u + d, v, u + d + w, v + d),
        ('bottom', u + d + w, v, u + d + 2 * w, v + d),
        ('west', u, v + d, u + d, v + d + h),
        ('north', u + d, v + d, u + d + w, v + d + h),
        ('east', u + d + w, v + d, u + 2 * d + w, v + d + h),
        ('south', u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    ]


def main(troops):
    for t in troops:
        model = json.loads((MODELS / (t + '.json')).read_text())
        grid = [[0] * W for _ in range(H)]
        owners = {}
        for b in model['bones']:
            for i, box in enumerate(b['boxes']):
                for name, x0, y0, x1, y1 in rects(box):
                    for y in range(max(0, y0), min(H, y1)):
                        for x in range(max(0, x0), min(W, x1)):
                            grid[y][x] += 1
                            owners.setdefault((x, y), b['name'])
        print('=== %s  (%dx%d, %d bones) ===' % (t, model['size'][0], model['size'][1], len(model['bones'])))
        for y in range(H):
            print('%2d %s' % (y, ''.join('#' if grid[y][x] else '.' for x in range(W))))
        # free runs per row
        print('  free runs (x0-x1) by row:')
        for y in range(H):
            run, out = None, []
            for x in range(W + 1):
                if x < W and not grid[y][x]:
                    if run is None:
                        run = x
                elif run is not None:
                    if x - run >= 4:
                        out.append('%d-%d' % (run, x))
                    run = None
            if out:
                print('   y%-2d %s' % (y, ' '.join(out)))
        print()


if __name__ == '__main__':
    args = sys.argv[1:] or ['knight']
    main(args)
