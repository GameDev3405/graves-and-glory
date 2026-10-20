"""Paint every track sprite into track/art/. Run from work/ui-concepts:  python track/build.py [name ...]"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import trackart, trackart2, trackart3, trackart4, trackart5
ASSETS = {}
for mod in (trackart, trackart2, trackart3, trackart4, trackart5):
    ASSETS.update(mod.ASSETS)
if __name__ == '__main__':
    names = sys.argv[1:] or list(ASSETS)
    for n in names:
        s = ASSETS[n]()
        s.save(os.path.join(trackart.OUT, n + '.png'))
        print('  track/art/' + n + '.png', s.w, 'x', s.h)
