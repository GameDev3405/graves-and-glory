import json
import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from booth_config import BoothError, load_config  # noqa: E402

BASE = {"troop": "spearman", "family": "spear", "geo": "g.json", "textures": ["a.png", "b.png"],
        "anim": "x.animation.json", "clips": [{"name": "idle"}]}
REF = {"geo": "k.json", "textures": ["k.png"], "anim": "k.animation.json"}


class ConfigTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)

    def write(self, obj):
        p = os.path.join(self.tmp.name, "booth.json")
        with open(p, "w", encoding="utf-8") as f:
            json.dump(obj, f)
        return p

    def bad(self, obj, word):
        with self.assertRaises(BoothError) as cm:
            load_config(self.write(obj))
        self.assertIn(word, str(cm.exception))

    def test_minimal_config_defaults(self):
        c = load_config(self.write(BASE))
        self.assertEqual((c.troop, c.family), ("spearman", "spear"))
        self.assertEqual(c.clips[0].base, [])
        self.assertEqual(c.clips[0].reference_base, [])
        self.assertEqual(c.clips[0].speed, 0.0)
        self.assertIsNone(c.clips[0].impact_tick)
        self.assertIsNone(c.clips[0].reference)
        self.assertIsNone(c.reference)
        self.assertIsNone(c.foot_bones)

    def test_full_config(self):
        cfg = dict(BASE, reference=REF, foot_bones=["left_foot", "right_foot"],
                   clips=[{"name": "attack_thrust", "base": ["stance_ready"], "speed": 0.1, "impact_tick": 7,
                           "reference": "attack_thrust", "reference_base": ["stance_ready"]}])
        c = load_config(self.write(cfg))
        k = c.clips[0]
        self.assertEqual((k.name, k.base, k.speed, k.impact_tick, k.reference, k.reference_base),
                         ("attack_thrust", ["stance_ready"], 0.1, 7, "attack_thrust", ["stance_ready"]))
        self.assertEqual(c.reference.anim, "k.animation.json")
        self.assertEqual(c.foot_bones, ["left_foot", "right_foot"])

    def test_input_paths(self):
        c = load_config(self.write(dict(BASE, textures=["a.png", "b.png", "a.png"])))
        self.assertEqual(c.input_paths(), ["g.json", "a.png", "b.png", "x.animation.json"])

    def test_rejects(self):
        self.bad({k: v for k, v in BASE.items() if k != "troop"}, "troop")
        self.bad(dict(BASE, clips=[]), "clips")
        self.bad(dict(BASE, clips=[{"name": "idle"}, {"name": "idle"}]), "idle")
        self.bad(dict(BASE, clips=[{"name": "idle", "reference": "idle"}]), "reference")
        self.bad(dict(BASE, clips=[{"name": "idle", "impact_tick": -1}]), "impact_tick")
        self.bad(dict(BASE, clips=[{"name": "idle", "impact_tick": 2.5}]), "impact_tick")
        self.bad(dict(BASE, clips=[{"name": "idle", "impact_tick": True}]), "impact_tick")
        self.bad(dict(BASE, geo="C:/abs/g.json"), "geo")
        self.bad(dict(BASE, geo="/abs/g.json"), "geo")
        self.bad(dict(BASE, textures=["../a.png"]), "textures")
        self.bad(dict(BASE, clips=[{"name": "bad name"}]), "name")
        self.bad(dict(BASE, clips=[{"name": "a/b"}]), "name")
        self.bad(dict(BASE, troop="sp/ear"), "troop")

    def test_unknown_key_rejected(self):
        self.bad(dict(BASE, clipz=[]), "clipz")
        self.bad(dict(BASE, clips=[{"name": "idle", "spead": 1}]), "spead")
        self.bad(dict(BASE, reference=dict(REF, extra=1)), "extra")

    def test_not_json_or_missing_file(self):
        p = os.path.join(self.tmp.name, "broken.json")
        with open(p, "w", encoding="utf-8") as f:
            f.write("{nope")
        with self.assertRaises(BoothError):
            load_config(p)
        with self.assertRaises(BoothError):
            load_config(os.path.join(self.tmp.name, "absent.json"))


if __name__ == "__main__":
    unittest.main()
