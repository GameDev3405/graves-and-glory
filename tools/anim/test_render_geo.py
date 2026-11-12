import math
import os
import sys
import contextlib
import io
import unittest

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import render_geo as rg  # noqa: E402

FIX = os.path.join(HERE, "fixtures")


def geo_of(cubes, tw=64, th=64, bones=None):
    return {"minecraft:geometry": [{"description": {"texture_width": tw, "texture_height": th},
                                     "bones": bones or [{"name": "b", "pivot": [0, 0, 0], "cubes": cubes}]}]}


class RotationTest(unittest.TestCase):
    def test_cube_rotated_plus_90_x(self):
        # Hand-computed: gecko x in [-1,0], y in [0,2], z in [0,1]; rotX = rad(-90): (x,y,z)->(x,z,-y);
        # world = (-gx, gy, -gz) -> x in {0,1}, (y,z) in {(0,0),(1,0),(0,2),(1,2)}.
        m = rg.load_geo(geo_of([{"origin": [0, 0, 0], "size": [16, 32, 16], "pivot": [0, 0, 0],
                                 "rotation": [90, 0, 0], "uv": [0, 0]}]))
        sc = rg.Scene(m)
        fr = sc.evaluate(0)
        lo, hi = sc.aabb(fr)
        np.testing.assert_allclose(lo, [0, 0, 0], atol=1e-9)
        np.testing.assert_allclose(hi, [1, 1, 2], atol=1e-9)
        got = {tuple(np.round(c, 6) + 0.0) for c in fr.corners[0]}
        want = {(x, y, z) for x in (0.0, 1.0) for (y, z) in ((0, 0), (1, 0), (0, 2), (1, 2))}
        self.assertEqual(got, {tuple(float(v) for v in w) for w in want})


class BoxUvTest(unittest.TestCase):
    def test_each_face_maps_to_its_region(self):
        sx, sy, sz = 8, 6, 4
        u, v = 4, 2
        regions = {
            "west": (u + sz + sx, v + sz, sz, sy), "east": (u, v + sz, sz, sy),
            "north": (u + sz, v + sz, sx, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy),
            "up": (u + sz, v, sx, sz), "down": (u + sz + sx, v, sx, sz),
        }
        cols = {"west": (255, 0, 0), "east": (0, 255, 0), "north": (0, 0, 255),
                "south": (255, 255, 0), "up": (255, 0, 255), "down": (0, 255, 255)}
        tex = np.zeros((64, 64, 4), np.uint8)
        for f, (x, y, w, h) in regions.items():
            tex[y:y + h, x:x + w] = cols[f] + (255,)
        m = rg.load_geo(geo_of([{"origin": [-4, 0, -2], "size": [sx, sy, sz], "uv": [u, v]}]))
        sc = rg.Scene(m, [tex])
        fr = sc.evaluate(0)
        # world +Z sees gecko north (-z); world +X sees gecko west (-x) since world = (-gx, gy, -gz)
        views = {"axis_pz": "north", "axis_nz": "south", "axis_px": "west", "axis_nx": "east",
                 "axis_py": "up", "axis_ny": "down"}
        lo, hi = sc.aabb(fr)
        for view, face in views.items():
            cam = rg.make_cameras([view], (lo, hi), "near", 100.0)[0].bind((0, 0, 0))
            img = rg.render_view(sc, fr, cam, ss=2, marker=False, bg_cache=False)
            c = img[img.shape[0] // 2, img.shape[1] // 2]
            # the centre must be the face colour times its shading, with the face's own hue
            shade = float(rg.shade_of(np.array([rg.DIR_NORMAL[face]]))[0])
            want = [int(round(k * shade)) for k in cols[face]]
            np.testing.assert_allclose(c, want, atol=1, err_msg="%s -> %s" % (view, face))

    def test_north_face_is_not_mirrored(self):
        # north region: left half red, right half blue. Viewer at world +Z has right = world +X.
        tex = np.zeros((16, 16, 4), np.uint8)
        # cube 4x4x4 at uv (0,0): north = x 4..8, y 4..8
        tex[4:8, 4:6] = (255, 0, 0, 255)
        tex[4:8, 6:8] = (0, 0, 255, 255)
        m = rg.load_geo(geo_of([{"origin": [-2, 0, -2], "size": [4, 4, 4], "uv": [0, 0]}], 16, 16))
        sc = rg.Scene(m, [tex])
        fr = sc.evaluate(0)
        lo, hi = sc.aabb(fr)
        cam = rg.make_cameras(["axis_pz"], (lo, hi), "near", 100.0)[0].bind((0, 0, 0))
        img = rg.render_view(sc, fr, cam, ss=2, marker=False, bg_cache=False).astype(int)
        h, w = img.shape[:2]
        left, right = img[h // 2, w // 2 - 12], img[h // 2, w // 2 + 12]
        self.assertGreater(left[0], left[2])    # red on the viewer's left
        self.assertGreater(right[2], right[0])  # blue on the viewer's right


class FlatCubeTest(unittest.TestCase):
    """A zero-depth cube has two faces in one plane. The game's depth test (LEQUAL) lets one of them win everywhere;
    a strict test let float noise choose per pixel and drew a hatch on every skirt and tabard (2026-11-11: three
    reviewers reported it as flicker, and it was not in the game)."""

    def plane_colours(self, tie):
        sx, sy = 8, 8
        u, v = 4, 2
        tex = np.zeros((64, 64, 4), np.uint8)
        tex[v:v + sy, u:u + sx] = (0, 0, 255, 255)              # north, sz = 0
        tex[v:v + sy, u + sx:u + 2 * sx] = (255, 255, 0, 255)    # south
        bones = [{"name": "b", "pivot": [0, 4, 0], "rotation": [20, 30, 10],
                  "cubes": [{"origin": [-4, 0, 0], "size": [sx, sy, 0], "uv": [u, v]}]}]
        sc = rg.Scene(rg.load_geo(geo_of([], bones=bones)), [tex])
        fr = sc.evaluate(0)
        cam = rg.make_cameras(["three_quarter"], sc.aabb(fr), "near", 100.0)[0].bind((0, 0, 0))
        old, rg.DEPTH_TIE = rg.DEPTH_TIE, tie
        try:
            img = rg.render_view(sc, fr, cam, ss=1, marker=False, bg_cache=False).astype(int)
        finally:
            rg.DEPTH_TIE = old
        blue = int(((img[..., 2] > img[..., 0] + 40) & (img[..., 2] > img[..., 1] + 40)).sum())
        yellow = int(((img[..., 0] > img[..., 2] + 40) & (img[..., 1] > img[..., 2] + 40)).sum())
        return blue, yellow

    def test_one_side_wins_everywhere(self):
        blue, yellow = self.plane_colours(rg.DEPTH_TIE)
        self.assertGreater(blue + yellow, 200)
        self.assertEqual(blue, 0, "the later face (south) wins the tie, as with LEQUAL: %d blue pixels" % blue)


class AnimTest(unittest.TestCase):
    def clip(self, bones, loop=True, length=1.0, name="c"):
        return rg.load_clip({"animations": {name: {"loop": loop, "animation_length": length, "bones": bones}}}, name)

    def test_linear_midpoint(self):
        c = self.clip({"b": {"position": {"0": [0, 0, 0], "1": [10, 20, 30]},
                             "rotation": {"0": [0, 0, 0], "1": [90, 90, 90]}}})
        np.testing.assert_allclose(c.sample("b", "position", 10), [5, 10, 15])
        np.testing.assert_allclose(c.sample("b", "rotation", 10), [-math.pi / 4, -math.pi / 4, math.pi / 4])

    def test_catmullrom_matches_formula(self):
        # values 0,1,4,9 at 0..3 s, all catmullrom. Segment ticks 20..40: p0=p1=1 (previous frame's end
        # equals this start), p2=4, p3=9, d = 0.5.
        k = {str(i): {"vector": [v, 0, 0], "easing": "catmullrom"} for i, v in enumerate([0, 1, 4, 9])}
        c = self.clip({"b": {"position": k}}, length=3.0)
        p0, p1, p2, p3, d = 1.0, 1.0, 4.0, 9.0, 0.5
        want = 0.5 * (2 * p1 + (p2 - p0) * d + (2 * p0 - 5 * p1 + 4 * p2 - p3) * d ** 2
                      + (3 * p1 - p0 - 3 * p2 + p3) * d ** 3)
        self.assertAlmostEqual(want, 2.1875)
        self.assertAlmostEqual(float(c.sample("b", "position", 30)[0]), want, places=9)
        # curve passes through the keyframes
        self.assertAlmostEqual(float(c.sample("b", "position", 40)[0]), 4.0, places=9)

    def test_easing_types(self):
        k = {"0": [0, 0, 0], "1": {"vector": [10, 0, 0], "easing": "easeInOutSine"}}
        c = self.clip({"b": {"position": k}})
        want = (1 - math.cos(math.pi / 4)) / 2 * 10      # tick 5: t=.25 -> sine(0.5)/2
        self.assertAlmostEqual(float(c.sample("b", "position", 5)[0]), want, places=9)
        k = {"0": [0, 0, 0], "1": {"vector": [1, 0, 0], "easing": "easeInQuint"}}
        c = self.clip({"b": {"position": k}})
        self.assertAlmostEqual(float(c.sample("b", "position", 10)[0]), 0.5 ** 4, places=12)  # GeckoLib uses pow(4)

    def test_overlapping_bones_error(self):
        a = self.clip({"head": {"position": {"0": [0, 0, 0], "1": [0, 1, 0]}}}, name="a")
        b = self.clip({"head": {"rotation": {"0": [0, 0, 0], "1": [0, 9, 0]}}}, name="b")
        m = rg.load_geo(geo_of([{"origin": [0, 0, 0], "size": [1, 1, 1], "uv": [0, 0]}],
                                bones=[{"name": "head", "pivot": [0, 0, 0],
                                        "cubes": [{"origin": [0, 0, 0], "size": [1, 1, 1], "uv": [0, 0]}]}]))
        with self.assertRaises(rg.GeoError) as cm:
            rg.Scene(m, [], [a, b])
        self.assertIn("head", str(cm.exception))


    def test_additive_clip_adds_onto_the_pose(self):
        # the game adds a hit clip onto the stance (HitController.add): same bone, offsets summed, zero means the stance
        hold = self.clip({"head": {"rotation": {"0": [10, 0, 0], "1": [10, 0, 0]},
                                   "position": {"0": [0, 2, 0], "1": [0, 2, 0]}}}, name="stance_ready_upper")
        hit = self.clip({"head": {"rotation": {"0": [0, 0, 0], "1": [5, 0, 0]},
                                  "position": {"0": [0, 0, 0], "1": [0, 1, 0]}}}, name="hit_light_front")
        m = rg.load_geo(geo_of([{"origin": [0, 0, 0], "size": [1, 1, 1], "uv": [0, 0]}],
                                bones=[{"name": "head", "pivot": [0, 0, 0],
                                        "cubes": [{"origin": [0, 0, 0], "size": [1, 1, 1], "uv": [0, 0]}]}]))
        sc = rg.Scene(m, [], [hold], 0.0, None, [hit])
        self.assertEqual(float(sc._pose(0.0)["head"]["rotation"][0]), float(hold.sample("head", "rotation", 0.0)[0]))
        want = hold.sample("head", "rotation", 10.0)[0] + hit.sample("head", "rotation", 10.0)[0]
        self.assertAlmostEqual(float(sc._pose(10.0)["head"]["rotation"][0]), float(want), places=9)
        want = hold.sample("head", "position", 10.0)[1] + hit.sample("head", "position", 10.0)[1]
        self.assertAlmostEqual(float(sc._pose(10.0)["head"]["position"][1]), float(want), places=9)
        self.assertEqual(sc.clip_label(), "stance_ready_upper+(+)hit_light_front")
        self.assertEqual(rg.additive_under("hit_block", {"stance_ready_upper", "idle_legs"}), ["stance_ready_upper"])
        self.assertIsNone(rg.additive_under("hit_block_legs", {"stance_ready_legs"}))
        self.assertIsNone(rg.additive_under("stun_upper", {"stance_ready_legs"}))
        under = ["stance_ready_upper", "stance_ready_legs"]
        self.assertEqual(rg.additive_split("hit_block", ["hit_block_legs"], under), (under, ["hit_block_legs", "hit_block"], []))
        self.assertEqual(rg.additive_split("stun_out_upper", ["stun_legs"], under),     # the stun hands over to the stance
                         (under, ["stun_out_upper"], ["stun_upper", "stun_legs"]))


class FootSlideTest(unittest.TestCase):
    def drift(self, clip):
        m = rg.load_geo(os.path.join(FIX, "rig.geo.json"))
        c = rg.load_clip(os.path.join(FIX, "walk.animation.json"), clip)
        sc = rg.Scene(m, [], [c], speed=0.05)
        return rg.build_report(sc, [float(t) for t in range(0, 40)])

    def test_planted_walk_has_no_slide(self):
        rep = self.drift("walk_planted")
        self.assertLess(rep["foot_slide"]["max_drift"], 0.005)
        for f in ("foot_left", "foot_right"):
            self.assertGreaterEqual(len(rep["foot_slide"]["feet"][f]["spans"]), 2)

    def test_sliding_walk_slides(self):
        rep = self.drift("walk_sliding")
        self.assertGreater(rep["foot_slide"]["max_drift"], 0.2)
        self.assertLess(rep["foot_slide"]["max_drift"], 0.3)   # 10 ticks * (0.05 - 0.025) = 0.25

    def test_no_feet_gives_none_and_warning(self):
        m = rg.load_geo(geo_of([{"origin": [0, 0, 0], "size": [1, 1, 1], "uv": [0, 0]}]))
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            rep = rg.build_report(rg.Scene(m), [0.0, 1.0, 2.0])
        self.assertIsNone(rep["foot_slide"]["max_drift"])
        self.assertIn("warning", rep["foot_slide"])
        self.assertIn("warning", err.getvalue())

    def test_foot_bones_override(self):
        m = rg.load_geo(os.path.join(FIX, "rig.geo.json"))
        c = rg.load_clip(os.path.join(FIX, "walk.animation.json"), "walk_sliding")
        sc = rg.Scene(m, [], [c], speed=0.05)
        rep = rg.build_report(sc, [float(t) for t in range(0, 40)], foot_names=["foot_left"])
        self.assertEqual(list(rep["foot_slide"]["feet"]), ["foot_left"])
        self.assertGreater(rep["foot_slide"]["max_drift"], 0.2)

    def test_single_sample_span_ignored_and_step_warning(self):
        series = {"foot": [(0.0, np.zeros(3), 0.0), (1.0, np.array([5.0, 0, 0]), 0.5),
                           (2.0, np.array([9.0, 0, 0]), 0.0)]}
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            out = rg.foot_slide(series, ground=0.0, step=2.0)
        self.assertEqual(out["feet"]["foot"]["spans"], [])
        self.assertIn("step", out["warning"])

    def test_ground_param(self):
        series = {"foot": [(0.0, np.zeros(3), 1.0), (1.0, np.array([1.0, 0, 0]), 1.0)]}
        self.assertEqual(rg.foot_slide(series, ground=0.0)["feet"]["foot"]["spans"], [])
        self.assertAlmostEqual(rg.foot_slide(series, ground=1.0)["max_drift"], 1.0)


class KeyframeOrderTest(unittest.TestCase):
    def test_file_order_kept_and_warned(self):
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            kf = rg._get_keyframes({"1": [1, 0, 0], "0.5": [2, 0, 0]}, "t")
        self.assertEqual([k[0] for k in kf], [1.0, 0.5])
        self.assertIn("not ascending", err.getvalue())

    def test_ascending_no_warning(self):
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            rg._get_keyframes({"0": [0, 0, 0], "1": [1, 0, 0]}, "t")
        self.assertEqual(err.getvalue(), "")

    def test_pre_post_vector_objects(self):
        kf = rg._get_keyframes({"1": {"pre": {"vector": [1, 2, 3]}, "post": {"vector": [4, 5, 6]}}}, "t")
        self.assertEqual([list(k[1]) for k in kf], [[1, 2, 3], [4, 5, 6]])


class UnionBoundsTest(unittest.TestCase):
    def test_union_and_shared_camera_size(self):
        a = (np.array([-1.0, 0.0, -0.5]), np.array([1.0, 2.0, 0.5]))
        b = (np.array([-0.5, 0.0, -2.0]), np.array([0.5, 3.0, 0.2]))
        lo, hi = rg.union_bounds([a, b])
        np.testing.assert_allclose(lo, [-1.0, 0.0, -2.0])
        np.testing.assert_allclose(hi, [1.0, 3.0, 0.5])
        for dist in ("near", "game16"):
            c1 = rg.make_cameras(["three_quarter"], (lo, hi), dist)[0]
            c2 = rg.make_cameras(["three_quarter"], rg.union_bounds([b, a]), dist)[0]
            self.assertEqual((c1.W, c1.H, c1.cx, c1.cy), (c2.W, c2.H, c2.cx, c2.cy))

    def test_empty_list_raises(self):
        with self.assertRaises(rg.GeoError):
            rg.union_bounds([])


if __name__ == "__main__":
    unittest.main()
