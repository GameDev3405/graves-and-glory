"""The player weapons pass the weapon standard's checks (read back with tools/bbmodel.py) and the committed textures
match what the build paints."""
import pathlib
import sys
import tempfile
import unittest

import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import build_player_weapons as B   # noqa: E402


class PlayerWeaponsTest(unittest.TestCase):
    def test_weapons_pass_the_checks_and_match_the_committed_textures(self):
        weapons = B.load()
        self.assertEqual(len(weapons), 10)
        for wid, wp in weapons.items():
            with tempfile.TemporaryDirectory() as tmp:
                self.assertEqual(B.check(wp, pathlib.Path(tmp)), [], wid)
            for rarity in ('common', 'legendary'):
                img, _, _ = wp.texture(rarity)
                path = ROOT / 'art/player_weapons' / wid / 'textures' / ('%s_%s.png' % (wid, rarity))
                self.assertTrue(path.exists(), path)
                self.assertTrue(np.array_equal(np.array(img), np.array(Image.open(path).convert('RGBA'))),
                                '%s is stale; run python tools/build_player_weapons.py' % path.name)


if __name__ == '__main__':
    unittest.main()
