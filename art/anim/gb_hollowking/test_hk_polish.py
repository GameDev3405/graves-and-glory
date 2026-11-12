"""hk_polish runs once: a second pass over its own output changes no byte (python art/anim/gb_hollowking/test_hk_polish.py).

Needs the documented chain to have run (out/ and the exported asset hold this build's polished clips and
out/hk_polish.state.json records them). Works on copies; nothing in the tree is written."""
import json
import sys
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import hk_polish as H  # noqa: E402


class Once(unittest.TestCase):
    def again(self, path, plan):
        text = Path(path).read_text(encoding="utf-8")
        doc = json.loads(text)
        before = json.dumps(doc, indent=1)
        state = H.STATE.read_bytes()
        H.polish(doc, plan, log=lambda *_: None)
        self.assertEqual(json.dumps(doc, indent=1), before)
        self.assertEqual(H.STATE.read_bytes(), state)

    def test_built_twice_is_once(self):
        self.again(HERE / "out" / f"{H.TROOP}.animation.json", H.BUILT)

    def test_late_twice_is_once(self):
        self.again(H.ASSET, H.LATE)

    def test_part_polished_is_refused(self):
        doc = json.loads(H.ASSET.read_text(encoding="utf-8"))
        rec = H._state()["late"]
        self.assertGreater(len(rec), 1)
        one = sorted(rec)[0]
        doc["animations"][one]["animation_length"] = float(doc["animations"][one]["animation_length"]) + 1.0  # "fresh"
        with self.assertRaises(RuntimeError):
            H.polished(doc, "late")


if __name__ == "__main__":
    unittest.main()
