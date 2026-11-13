"""booth.py build refuses a batch whose clips fail the scorecard, and packs each clip's scorecard for the page."""
import json
import os
import shutil
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import booth  # noqa: E402
from booth_config import BoothError  # noqa: E402
from test_booth_build import ANIM, CFG, FIX, GEO, TEX, config, read_json, write_json  # noqa: E402
from test_booth_git import commit, git, make_repo, put  # noqa: E402


class GateTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.repo = repo = make_repo(self.tmp.name)
        put(repo, ".gitignore", "work/\n")
        for rel, name in ((GEO, "rig.geo.json"), (TEX, "rig.png"), (ANIM, "walk.animation.json")):
            os.makedirs(os.path.dirname(os.path.join(repo, rel)), exist_ok=True)
            shutil.copyfile(os.path.join(FIX, name), os.path.join(repo, rel))
        data = read_json(os.path.join(repo, *ANIM.split("/")))
        # a loop whose last frame is 60 degrees from its first: a pop every cycle (loop checks apply to any rig; this
        # fixture is not a biped, so its limb speed and bend checks are n/a)
        data["animations"]["whip"] = {"animation_length": 1.0, "loop": True, "bones": {"arm_left": {"rotation": {
            "0": [0, 0, 0], "1.0": [-60, 0, 0]}}}}
        write_json(repo, ANIM, data)
        write_json(repo, CFG, config([{"name": "head_bob"}, {"name": "whip"}]))
        commit(repo, "base")
        git(repo, "checkout", "-q", "-b", "lane")

    def build(self, **kw):
        return booth.build(self.repo, CFG, "target", ss=1, **kw)

    def test_failing_clip_refuses_the_batch(self):
        with self.assertRaises(BoothError) as cm:
            self.build()
        msg = str(cm.exception)
        self.assertIn("whip: loop_pose = 60", msg)
        self.assertIn("limit", msg)
        self.assertNotIn("head_bob", msg)
        self.assertFalse(os.path.exists(os.path.join(self.repo, "work", "booth")))

    def test_an_owner_approved_clip_builds_and_still_shows_its_fail(self):
        ok = {("rig", "whip"): {"loop_pose": {"value": 60.0, "reason": "Kort: keep it"},
                                "loop_vel": {"value": 1e9, "reason": "Kort: keep it"}}}
        cfg_troop = read_json(os.path.join(self.repo, *CFG.split("/")))["troop"]
        ok = {(cfg_troop, k[1]): v for k, v in ok.items()}
        real = booth.sc.load_approved
        booth.sc.load_approved = lambda path=None: ok
        self.addCleanup(setattr, booth.sc, "load_approved", real)
        m = self.build()
        card = next(c for c in m["clips"] if c["name"] == "whip")["scorecard"]
        self.assertTrue(card["pass"])
        row = next(r for r in card["metrics"] if r["id"] == "loop_pose")
        self.assertEqual(row["status"], "fail")
        self.assertEqual(row["approved"], "Kort: keep it")

    def test_passing_batch_carries_scorecards(self):
        m = self.build(clips=["head_bob"])
        card = m["clips"][0]["scorecard"]
        self.assertTrue(card["pass"])
        rows = {r["id"]: r for r in card["metrics"]}
        self.assertEqual(rows["edge_lead"]["status"], "n/a")
        self.assertEqual(rows["foot_slide"]["status"], "n/a")              # not a biped
        self.assertIn("biped", rows["foot_slide"]["note"])
        passed = [r for r in card["metrics"] if r["status"] == "pass"]
        self.assertTrue(passed)
        self.assertEqual(set(passed[0]), {"id", "unit", "status", "value", "limit"})
        doc = read_json(os.path.join(self.repo, "work", "booth", m["batch"], "db", "clips", "head_bob.json"))
        self.assertEqual(doc["scorecard"], card)
        json.dumps(doc)       # the database document stays plain JSON


if __name__ == "__main__":
    unittest.main()
