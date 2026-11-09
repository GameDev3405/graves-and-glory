import os
import sys
import tempfile
import unittest

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import booth_pack as bp  # noqa: E402
from booth_config import BoothError  # noqa: E402
from booth_render import ClipRender  # noqa: E402

VI = {"reference": 0, "previous": 1, "new": 2}


def colour(viewer, dist, tick):
    return (40 + 80 * VI[viewer], 60 if dist == "near" else 180, 20 + 10 * tick)


def fake(viewers=("reference", "previous", "new"), n=14, near=(480, 400), game=(96, 110), lengths=None):
    frames = {}
    for v in viewers:
        k = (lengths or {}).get(v, n)
        frames[v] = {
            "near": [np.full(near + (3,), colour(v, "near", t), dtype=np.uint8) for t in range(k)],
            "game16": [np.full(game + (3,), colour(v, "game16", t), dtype=np.uint8) for t in range(k)],
        }
    return ClipRender(frames, len(frames["new"]["near"]), "0" * 64, {"foot_slide": None}, [])


class PackTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.out = os.path.join(self.tmp.name, "clip.webp")

    def test_sections_order_and_offsets(self):
        secs = bp.pack_sheet(fake(), self.out)
        self.assertEqual([(s["viewer"], s["distance"]) for s in secs],
                         [("reference", "near"), ("reference", "game16"), ("previous", "near"),
                          ("previous", "game16"), ("new", "near"), ("new", "game16")])
        y = 0
        for s in secs:
            self.assertEqual(set(s), {"viewer", "distance", "y", "cell_w", "cell_h", "cols", "frames"})
            self.assertEqual(s["y"], y)
            self.assertEqual((s["cols"], s["frames"]), (12, 14))
            y += s["cell_h"] * 2                      # 14 frames in 12 columns = 2 rows
        near, game = secs[0], secs[1]
        self.assertEqual((near["cell_w"], near["cell_h"]), (200, 240))
        self.assertEqual((game["cell_w"], game["cell_h"]), (110, 96))
        with Image.open(self.out) as im:
            self.assertEqual(im.format, "WEBP")
            self.assertEqual(im.size, (12 * 200, y))

    def test_cell_round_trip(self):
        secs = bp.pack_sheet(fake(), self.out)
        with Image.open(self.out) as im:
            rgb = np.array(im.convert("RGB")).astype(int)
        for viewer, dist, tick in (("new", "near", 13), ("reference", "game16", 0), ("previous", "near", 11)):
            s = next(x for x in secs if x["viewer"] == viewer and x["distance"] == dist)
            x0 = (tick % s["cols"]) * s["cell_w"]
            y0 = s["y"] + (tick // s["cols"]) * s["cell_h"]
            cell = rgb[y0 + 4:y0 + s["cell_h"] - 4, x0 + 4:x0 + s["cell_w"] - 4]
            got = cell.reshape(-1, 3).mean(axis=0)
            for g, w in zip(got, colour(viewer, dist, tick)):
                self.assertLess(abs(g - w), 6, (viewer, dist, tick, got))

    def test_missing_viewers_omitted(self):
        secs = bp.pack_sheet(fake(viewers=("new",)), self.out)
        self.assertEqual([(s["viewer"], s["distance"]) for s in secs], [("new", "near"), ("new", "game16")])

    def test_viewers_with_own_lengths(self):
        secs = bp.pack_sheet(fake(lengths={"reference": 5, "new": 30}), self.out)
        frames = {(s["viewer"], s["distance"]): s["frames"] for s in secs}
        self.assertEqual(frames[("reference", "near")], 5)
        self.assertEqual(frames[("previous", "game16")], 14)
        self.assertEqual(frames[("new", "near")], 30)

    def test_near_scaled_down_only(self):
        self.assertEqual(bp.pack_sheet(fake(near=(480, 400)), self.out)[0]["cell_h"], 240)
        small = bp.pack_sheet(fake(near=(100, 90)), self.out)[0]
        self.assertEqual((small["cell_w"], small["cell_h"]), (90, 100))

    def test_too_large_raises(self):
        with self.assertRaises(BoothError):
            bp.pack_sheet(fake(viewers=("new",), n=2000, near=(240, 8), game=(8, 8)), self.out)
        self.assertFalse(os.path.exists(self.out))

    def test_total_bytes(self):
        a = os.path.join(self.tmp.name, "a.bin")
        b = os.path.join(self.tmp.name, "b.bin")
        for p, n in ((a, 10), (b, 32)):
            with open(p, "wb") as f:
                f.write(b"x" * n)
        self.assertEqual(bp.total_bytes([a, b]), 42)


if __name__ == "__main__":
    unittest.main()
