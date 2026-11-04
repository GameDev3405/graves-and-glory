"""The player armour sets build cleanly: no z-fighting faces, no ember on the Common-to-Epic tier, and the committed
textures match what the build paints (so a hand-edited PNG or a stale build is caught)."""
import pathlib
import sys
import unittest

import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import build_player_armor as B   # noqa: E402


class PlayerArmourTest(unittest.TestCase):
    def test_sets_are_clean_and_match_the_committed_textures(self):
        self.assertEqual(len(B.SETS), 5)
        for name, make in B.SETS.items():
            aset = make()
            problems, glow = B.checks(aset)
            self.assertEqual(problems, [], name)
            self.assertGreater(glow, 0, name + ': a Legendary needs glowing texels')
            for rarity in ('common', 'legendary'):
                img, _ = aset.texture(rarity, 'crimson')
                path = ROOT / 'art/armor' / name / 'textures' / ('%s_%s_crimson.png' % (name, rarity))
                self.assertTrue(path.exists(), path)
                self.assertTrue(np.array_equal(np.array(img), np.array(Image.open(path).convert('RGBA'))),
                                '%s is stale; run python tools/build_player_armor.py' % path.name)


if __name__ == '__main__':
    unittest.main()
