"""Validates every face pattern in hx_skins.py against the face it will paint.

The painter raises on the first mismatch, which means one fix round per error.
This monkey-patches the painter to collect every mismatch in the troop, then
reports them all at once.

  python work/p2b/hx_validate.py [troop ...]
"""
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import hx_art  # noqa: E402
import hx_skins  # noqa: E402

ERRORS = []
_real_put = hx_art.Painter.put


def put(self, bone, face, lines, key=None, box=0, check=True):
    try:
        return _real_put(self, bone, face, lines, key=key, box=box, check=check)
    except ValueError as e:
        ERRORS.append(str(e))
        return None


hx_art.Painter.put = put

names = sys.argv[1:] or list(hx_skins.TROOPS)
for n in names:
    ERRORS.clear()
    for fn in hx_skins.TROOPS[n]():
        pass
    if ERRORS:
        print('=== %s: %d mismatch(es) ===' % (n, len(ERRORS)))
        for e in ERRORS:
            print('   ' + e)
    else:
        print('=== %s: clean ===' % n)
