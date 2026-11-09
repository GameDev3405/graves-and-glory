import contextlib
import io
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
from test_booth_git import commit, git, make_repo, put  # noqa: E402

FIX = os.path.join(HERE, "fixtures")
GEO = "assets/geo/rig.geo.json"
TEX = "assets/tex/rig.png"
ANIM = "assets/anim/rig.animation.json"
CFG = "art/anim/fam/booth.rig.json"


def read_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def write_json(repo, rel, obj):
    put(repo, rel, json.dumps(obj, indent=1))


def nudge(repo, clip, bone, by=25):
    data = read_json(os.path.join(repo, *ANIM.split("/")))
    chans = data["animations"][clip]["bones"][bone]
    ch = chans.get("rotation") or chans["position"]
    for v in ch.values():       # every key, so a looping clip still meets itself at the wrap (the scorecard checks it)
        vec = v["vector"] if isinstance(v, dict) and "vector" in v else v
        vec[2] = vec[2] + by
    write_json(repo, ANIM, data)


def config(clips):
    return {"troop": "rig", "family": "fam", "geo": GEO, "textures": [TEX], "anim": ANIM,
            "reference": {"geo": GEO, "textures": [TEX], "anim": ANIM}, "clips": clips}


class BuildTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.repo = repo = make_repo(self.tmp.name)
        put(repo, ".gitignore", "work/\n")
        for rel, name in ((GEO, "rig.geo.json"), (TEX, "rig.png"), (ANIM, "walk.animation.json")):
            os.makedirs(os.path.dirname(os.path.join(repo, rel)), exist_ok=True)
            shutil.copyfile(os.path.join(FIX, name), os.path.join(repo, rel))
        write_json(repo, CFG, config([{"name": "arm_swing"}, {"name": "head_bob"}]))
        put(repo, "src/X.java", "v1")
        self.target_sha = commit(repo, "base")
        git(repo, "checkout", "-q", "-b", "lane")
        nudge(repo, "arm_swing", "arm_left")
        data = read_json(os.path.join(repo, *ANIM.split("/")))
        data["animations"]["wave"] = data["animations"]["head_look"]
        write_json(repo, ANIM, data)
        write_json(repo, CFG, config([{"name": "arm_swing", "impact_tick": 7, "reference": "head_bob"},
                                      {"name": "head_bob"}, {"name": "wave"}]))
        put(repo, "src/X.java", "v2")
        self.lane_sha = commit(repo, "lane work")

    def build(self, **kw):
        return booth.build(self.repo, CFG, "target", ss=1, **kw)

    def folder(self, m):
        return os.path.join(self.repo, "work", "booth", m["batch"])

    def clip(self, m, name):
        return next(c for c in m["clips"] if c["name"] == name)

    def viewers(self, m, name):
        return {s["viewer"] for s in self.clip(m, name)["sections"]}

    def test_round_one_manifest(self):
        m = self.build()
        self.assertRegex(m["batch"], r"^rig-\d{8}-%s$" % self.lane_sha[:7])
        self.assertEqual((m["round"], m["troop"], m["lane"], m["target"]), (1, "rig", "lane", "target"))
        self.assertEqual((m["submitted_sha"], m["target_sha"], m["previous_sha"]),
                         (self.lane_sha, self.target_sha, self.target_sha))
        self.assertEqual([c["name"] for c in m["clips"]], ["arm_swing", "head_bob", "wave"])
        self.assertEqual(self.viewers(m, "arm_swing"), {"reference", "previous", "new"})
        self.assertEqual(self.viewers(m, "head_bob"), {"previous", "new"})
        a = self.clip(m, "arm_swing")
        self.assertEqual((a["ticks"], a["impact_tick"], a["changed"], a["round"]), (21, 7, True, 1))
        self.assertRegex(a["frame_hash"], r"^[0-9a-f]{64}$")
        self.assertEqual(a["sheet"], "round-1/arm_swing.webp")
        self.assertEqual((a["previous_sha"], a["reference_sha"]), (self.target_sha, self.target_sha))
        self.assertIsNone(self.clip(m, "head_bob")["reference_sha"])
        d = self.folder(m)
        for c in m["clips"]:
            self.assertTrue(os.path.isfile(os.path.join(d, *c["sheet"].split("/"))))
        self.assertEqual(read_json(os.path.join(d, "manifest.json")), m)

    def test_db_documents_and_upload_list(self):
        m = self.build()
        d = self.folder(m)
        batch = read_json(os.path.join(d, "db", "batch.json"))
        self.assertNotIn("clips", batch)
        self.assertEqual(batch["clip_order"], ["arm_swing", "head_bob", "wave"])
        self.assertEqual((batch["batch"], batch["round"], batch["submitted_sha"]), (m["batch"], 1, self.lane_sha))
        self.assertRegex(batch["created_at"], r"^\d{4}-\d\d-\d\dT")
        doc = read_json(os.path.join(d, "db", "clips", "arm_swing.json"))
        self.assertIsNone(doc["sheet_asset"])
        self.assertEqual({k: v for k, v in doc.items() if k != "sheet_asset"}, self.clip(m, "arm_swing"))
        with open(os.path.join(d, "upload.txt"), encoding="utf-8") as f:
            lines = [x for x in f.read().splitlines() if x]
        self.assertEqual(len(lines), 3)
        for p in lines:
            self.assertTrue(os.path.isabs(p) and os.path.isfile(p), p)

    def test_new_clip_has_no_previous(self):
        self.assertEqual(self.viewers(self.build(), "wave"), {"new"})

    def test_other_files_listed(self):
        self.assertEqual(self.build()["other_files"], ["src/X.java"])

    def test_dirty_input_refused(self):
        nudge(self.repo, "head_bob", "head")
        with self.assertRaises(BoothError) as cm:
            self.build()
        self.assertIn(ANIM, str(cm.exception))
        self.assertFalse(os.path.exists(os.path.join(self.repo, "work", "booth")))

    def test_untracked_config_refused(self):
        other = "art/anim/fam/booth.other.json"
        write_json(self.repo, other, config([{"name": "head_bob"}]))
        with self.assertRaises(BoothError) as cm:
            booth.build(self.repo, other, "target", ss=1)
        self.assertIn(other, str(cm.exception))

    def test_clips_filter_round_one(self):
        m = self.build(clips=["wave"])
        self.assertEqual([c["name"] for c in m["clips"]], ["wave"])
        with self.assertRaises(BoothError):
            self.build(clips=["nope"])

    def test_round_one_twice_refused(self):
        self.build()
        with self.assertRaises(BoothError) as cm:
            self.build()
        self.assertIn("--batch", str(cm.exception))

    def test_round_two_carries_unchanged(self):
        m1 = self.build()
        nudge(self.repo, "head_bob", "head")
        sha2 = commit(self.repo, "fix head_bob")
        m2 = self.build(batch=m1["batch"])
        self.assertEqual((m2["batch"], m2["round"], m2["submitted_sha"]), (m1["batch"], 2, sha2))
        self.assertEqual(m2["previous_sha"], self.lane_sha)
        self.assertEqual(m2["created_at"], m1["created_at"])
        self.assertEqual({c["name"]: c["changed"] for c in m2["clips"]},
                         {"arm_swing": False, "head_bob": True, "wave": False})
        self.assertEqual(self.clip(m2, "arm_swing"), dict(self.clip(m1, "arm_swing"), changed=False))
        h = self.clip(m2, "head_bob")
        self.assertEqual((h["sheet"], h["round"], h["previous_sha"]), ("round-2/head_bob.webp", 2, self.lane_sha))
        self.assertNotEqual(h["frame_hash"], self.clip(m1, "head_bob")["frame_hash"])
        d = self.folder(m2)
        with open(os.path.join(d, "upload.txt"), encoding="utf-8") as f:
            lines = [x for x in f.read().splitlines() if x]
        self.assertEqual([os.path.basename(p) for p in lines], ["head_bob.webp"])
        self.assertTrue(os.path.isfile(os.path.join(d, "round-1", "arm_swing.webp")))
        self.assertEqual(os.listdir(os.path.join(d, "db", "clips")), ["head_bob.json"])
        with self.assertRaises(BoothError):
            self.build(batch=m1["batch"], clips=["head_bob"])

    def test_round_two_target_mismatch_and_unknown_batch(self):
        m1 = self.build()
        with self.assertRaises(BoothError):
            booth.build(self.repo, CFG, "lane", batch=m1["batch"], ss=1)
        with self.assertRaises(BoothError):
            self.build(batch="rig-20000101-abcdef0")

    def test_later_round_keeps_a_clips_batch_to_its_clips(self):
        m1 = self.build(clips=["wave"])
        self.assertTrue(m1["partial"])
        nudge(self.repo, "head_bob", "head")
        commit(self.repo, "touch another clip")
        m2 = self.build(batch=m1["batch"])
        self.assertEqual([c["name"] for c in m2["clips"]], ["wave"])
        self.assertTrue(m2["partial"])

    def test_clip_added_in_a_later_round_compares_against_target(self):
        m1 = self.build()
        self.assertFalse(m1["partial"])
        data = read_json(os.path.join(self.repo, *ANIM.split("/")))
        data["animations"]["nod"] = data["animations"]["head_bob"]
        write_json(self.repo, ANIM, data)
        write_json(self.repo, CFG, config([{"name": "arm_swing", "impact_tick": 7, "reference": "head_bob"},
                                           {"name": "head_bob"}, {"name": "wave"}, {"name": "nod"}]))
        commit(self.repo, "add nod")
        m2 = self.build(batch=m1["batch"])
        n = self.clip(m2, "nod")
        self.assertEqual((n["previous_sha"], n["changed"], n["round"]), (self.target_sha, True, 2))
        self.assertEqual(self.viewers(m2, "nod"), {"new"})

    def test_main_target_and_detached_head_refused(self):
        git(self.repo, "branch", "main", "target")
        with self.assertRaises(BoothError) as cm:
            booth.build(self.repo, CFG, "main", ss=1)
        self.assertIn("main", str(cm.exception))
        git(self.repo, "checkout", "-q", "--detach")
        with self.assertRaises(BoothError) as cm:
            self.build()
        self.assertIn("branch", str(cm.exception))

    def test_size_budget(self):
        with self.assertRaises(BoothError) as cm:
            self.build(budget_bytes=1000)
        self.assertIn("budget", str(cm.exception))
        self.assertFalse(os.path.exists(os.path.join(self.repo, "work", "booth")))

    def test_cli(self):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = booth.main(["build", "--repo", self.repo, "--config", CFG, "--target", "no-such", "--ss", "1"])
        self.assertEqual(code, 2)
        self.assertTrue(err.getvalue().startswith("error:"), err.getvalue())
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = booth.main(["build", "--repo", self.repo, "--config", CFG, "--target", "target", "--ss", "1",
                               "--clips", "wave"])
        self.assertEqual(code, 0, err.getvalue())
        self.assertTrue(os.path.isfile(os.path.join(out.getvalue().strip().splitlines()[-1], "manifest.json")))

    def test_worktree_untouched(self):
        before = (git(self.repo, "status", "--porcelain"), git(self.repo, "rev-parse", "HEAD"),
                  git(self.repo, "rev-parse", "--abbrev-ref", "HEAD"))
        self.build()
        self.assertEqual((git(self.repo, "status", "--porcelain"), git(self.repo, "rev-parse", "HEAD"),
                          git(self.repo, "rev-parse", "--abbrev-ref", "HEAD")), before)


if __name__ == "__main__":
    unittest.main()
