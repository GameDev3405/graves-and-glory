import copy
import json
import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import booth_render as br  # noqa: E402
import render_geo as rg  # noqa: E402
from booth_config import BoothError  # noqa: E402

FIX = os.path.join(HERE, "fixtures")
GEO = os.path.join(FIX, "rig.geo.json")
TEX = os.path.join(FIX, "rig.png")
ANIM = os.path.join(FIX, "walk.animation.json")


def src(clip, geo=GEO, anim=ANIM, speed=0.0, base=()):
    return br.Source(geo, [TEX], [(anim, b) for b in base] + [(anim, clip)], speed)


class RenderTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)

    def variant(self, name, edit):
        """A copy of a fixture JSON with edit(data) applied."""
        path = os.path.join(FIX, name)
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        edit(data)
        out = os.path.join(self.tmp.name, "v%d_%s" % (len(os.listdir(self.tmp.name)), name))
        with open(out, "w", encoding="utf-8") as f:
            json.dump(data, f)
        return out

    def tall_geo(self):
        def edit(d):
            bones = d["minecraft:geometry"][0]["bones"]
            head = next(b for b in bones if b["name"] == "head")
            cube = copy.deepcopy(head["cubes"][0])
            cube["origin"][1] += 40
            cube.pop("pivot", None)
            cube.pop("rotation", None)
            head["cubes"].append(cube)
        return self.variant("rig.geo.json", edit)

    def test_clip_ticks(self):
        self.assertEqual(br.clip_ticks(ANIM, "arm_swing"), 21)
        self.assertEqual(br.clip_ticks(ANIM, "head_look"), 41)

    def test_a_play_once_clip_ends_on_its_last_pose_not_the_bind_pose(self):
        loop = rg.load_clip(ANIM, "arm_swing")
        self.assertEqual(br._shown_ticks(loop), list(range(21)))
        once = copy.copy(loop)
        once.loop = "once"                  # stopped AT its length: the game never draws that tick
        self.assertIsNone(once.effective_tick(once.length))
        ticks = br._shown_ticks(once)
        self.assertEqual(ticks[:-1], list(range(20)))
        self.assertLess(ticks[-1], once.length)
        self.assertTrue(all(once.effective_tick(t) is not None for t in ticks))

    def test_three_viewers_share_frame_size(self):
        r = br.render_clip({"reference": src("arm_swing", geo=self.tall_geo()),
                            "previous": src("arm_swing"), "new": src("arm_swing")}, ss=1)
        alone = br.render_clip({"reference": None, "previous": None, "new": src("arm_swing")}, ss=1)
        for dist in ("near", "game16"):
            shapes = {f.shape for v in ("reference", "previous", "new") for f in r.frames[v][dist]}
            self.assertEqual(len(shapes), 1, dist)
            self.assertEqual(r.frames["new"][dist][0].dtype.name, "uint8")
            self.assertEqual(r.frames["new"][dist][0].shape[2], 3)
        # the taller reference enlarges the shared near frame
        self.assertGreater(r.frames["new"]["near"][0].shape[0], alone.frames["new"]["near"][0].shape[0])

    def test_only_new(self):
        r = br.render_clip({"reference": None, "previous": None, "new": src("arm_swing")}, ss=1)
        self.assertEqual(set(r.frames), {"new"})
        self.assertEqual(set(r.frames["new"]), {"near", "game16"})
        self.assertEqual(r.ticks, 21)
        self.assertEqual(len(r.frames["new"]["near"]), 21)

    def test_viewers_keep_own_length(self):
        r = br.render_clip({"reference": src("arm_swing"), "previous": None, "new": src("head_look")}, ss=1)
        self.assertEqual(r.ticks, 41)
        self.assertEqual(len(r.frames["reference"]["near"]), 21)
        self.assertEqual(len(r.frames["new"]["game16"]), 41)

    def test_base_clip_is_layered_under_the_reviewed_clip(self):
        plain = br.render_clip({"reference": None, "previous": None, "new": src("arm_swing")}, ss=1)
        based = br.render_clip({"reference": None, "previous": None, "new": src("arm_swing", base=["head_bob"])}, ss=1)
        self.assertEqual(based.ticks, 21)
        self.assertNotEqual(plain.frame_hash, based.frame_hash)

    def test_frame_hash_stable_and_sensitive(self):
        def run(new, ref):
            return br.render_clip({"reference": ref, "previous": None, "new": new}, ss=1).frame_hash
        base = run(src("arm_swing"), src("arm_swing"))
        self.assertRegex(base, r"^[0-9a-f]{64}$")
        self.assertEqual(base, run(src("arm_swing"), src("arm_swing")))

        def nudge(d):
            ch = d["animations"]["arm_swing"]["bones"]["arm_left"]["rotation"]
            key = sorted(ch)[0]
            v = ch[key]
            vec = v["vector"] if isinstance(v, dict) and "vector" in v else v
            vec[2] = vec[2] + 25
        self.assertNotEqual(base, run(src("arm_swing", anim=self.variant("walk.animation.json", nudge)),
                                      src("arm_swing")))
        # a different reference with the same bounds (it walks, so its frames differ) leaves New's hash unchanged
        self.assertEqual(base, run(src("arm_swing"), src("arm_swing", speed=0.05)))

    def one(self, new, ref=None, **kw):
        return br.render_clip({"reference": ref, "previous": None, "new": new}, ss=1, **kw)

    def test_foot_slide_flagged_when_worse_than_reference(self):
        r = self.one(src("walk_sliding", speed=0.05), src("walk_planted", speed=0.05))
        self.assertGreater(r.numbers["foot_slide"], r.numbers["foot_slide_reference"] + 0.05)
        self.assertEqual(r.flags, ["foot_slide"])

    def test_foot_slide_not_flagged_when_like_reference(self):
        r = self.one(src("walk_planted", speed=0.05), src("walk_planted", speed=0.05))
        self.assertEqual(r.numbers["foot_slide"], r.numbers["foot_slide_reference"])
        self.assertEqual(r.flags, [])
        both_slide = self.one(src("walk_sliding", speed=0.05), src("walk_sliding", speed=0.05))
        self.assertEqual(both_slide.flags, [])

    def test_foot_slide_without_reference_is_a_number_only(self):
        r = self.one(src("walk_sliding", speed=0.05))
        self.assertGreater(r.numbers["foot_slide"], 0.05)
        self.assertIsNone(r.numbers["foot_slide_reference"])
        self.assertEqual(r.flags, [])

    def test_foot_slide_only_measured_for_moving_clips(self):
        r = self.one(src("walk_sliding", speed=0.0), src("walk_planted", speed=0.05))
        self.assertEqual(r.numbers, {"foot_slide": None, "foot_slide_reference": None})
        self.assertEqual(r.flags, [])

    def test_no_foot_bones_gives_no_number(self):
        r = self.one(src("walk_sliding", speed=0.05), src("walk_planted", speed=0.05), foot_bones=["no_such_bone"])
        self.assertIsNone(r.numbers["foot_slide"])
        self.assertEqual(r.flags, [])

    def test_new_none_raises(self):
        with self.assertRaises(BoothError):
            br.render_clip({"reference": src("arm_swing"), "previous": None, "new": None}, ss=1)

    def test_wrong_keys_raise(self):
        with self.assertRaises(BoothError):
            br.render_clip({"new": src("arm_swing")}, ss=1)

    def test_missing_clip_raises_booth_error(self):
        with self.assertRaises(BoothError):
            br.clip_ticks(ANIM, "no_such_clip")
        with self.assertRaises(BoothError):
            br.render_clip({"reference": None, "previous": None, "new": src("no_such_clip")}, ss=1)


if __name__ == "__main__":
    unittest.main()
