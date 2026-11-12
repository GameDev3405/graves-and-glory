"""tc_raider's late layer (art/anim/knight/raider_review.py) is part of the troop's build and is a fixed point: run on the
shipped clip file it gives that file back byte for byte, and run again on its own output it gives the same bytes."""
import copy
import json
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, os.path.join(REPO, "art", "anim", "knight"))
ASSET = os.path.join(REPO, "src/main/resources/assets/gravesandglory/animations/troop/tc_raider.animation.json")


class RaiderLayer(unittest.TestCase):
    def test_twice_on_the_shipped_file_is_byte_identical(self):
        import raider_review as RR
        with open(ASSET, encoding="utf-8", newline="") as fh:
            shipped = fh.read()
        once = RR.dumps(RR.apply(json.loads(shipped)))
        twice = RR.dumps(RR.apply(json.loads(once)))
        self.assertEqual(once, twice, "a second pass of raider_review.py changed the file")
        self.assertEqual(once, shipped, "the shipped tc_raider clips are not the layer's output: run raider_review.py")

    def test_touches_no_other_troop(self):
        import raider_review as RR
        self.assertEqual(RR.TID, "tc_raider")
        self.assertTrue(str(RR.ASSET).replace("\\", "/").endswith("animations/troop/tc_raider.animation.json"))


if __name__ == "__main__":
    unittest.main()
