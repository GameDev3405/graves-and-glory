"""tools/build_geo_trim.py: the Geo veteran trim overlays (C15-F4) are what their recipes build, and the trim rules hold.

The draw itself (a Proven Geo knight drawing its overlay through GeoTroopRenderer's Trim layer) needs a client: the in-game check is
p1-geo-knight-veteran (GRAVES_PHASE1_CHECKS=geo-knight, GRAVES_GEO_CAPTURE=veteran)."""
import copy
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import build_geo_trim as B  # noqa: E402


class GeoTrimTest(unittest.TestCase):
    def test_committed_overlays_match_recipes(self):
        self.assertEqual(B.main(["--check"]), 0)

    def test_knight_has_a_trim_for_every_tier(self):
        built = B.build("knight")
        self.assertEqual(sorted(built), [1, 2, 3])
        for tier, img in built.items():
            alphas = set(img.getchannel("A").tobytes())
            self.assertLessEqual(alphas, {0, 255}, tier)
            self.assertTrue(B.target("knight", tier).exists(), tier)

    def test_tier_sheets_are_untouched_by_the_trim(self):
        # The overlay is its own file: a fresh knight draws exactly the exporter's sheets.
        sheets = sorted((B.ASSETS / "textures/entity/troop/knight_geo").glob("t*_*.png"))
        self.assertEqual(len(sheets), 18)

    def _fails(self, mutate, needle):
        recipe, geo, rig = B.load("knight")
        bad = copy.deepcopy(recipe)
        mutate(bad)
        orig = B.load
        B.load = lambda troop: (bad, geo, rig)
        try:
            with self.assertRaises(B.TrimError) as cm:
                B.build("knight")
            self.assertIn(needle, str(cm.exception))
        finally:
            B.load = orig

    def test_team_bone_refused(self):
        self._fails(lambda r: r["marks"].append({"bone": "cape", "cube": 0, "pattern": "rope", "rows": [0], "tiers": [3]}), "team bone")

    def test_hidden_bone_refused(self):
        self._fails(lambda r: r["marks"].append({"bone": "right_cuff", "cube": 0, "pattern": "rope", "rows": [0], "tiers": [1]}), "does not show")

    def test_shared_texel_refused(self):
        # A texel another showing cube also uses would put brass there too: point the hips' UV at the belt's and the build must refuse.
        recipe, geo, rig = B.load("knight")
        g2 = copy.deepcopy(geo)
        belt = next(b for b in g2["bones"] if b["name"] == "belt")
        hips = next(b for b in g2["bones"] if b["name"] == "hips")
        hips["cubes"][0]["uv"] = list(belt["cubes"][0]["uv"])   # make the hips paint over the belt's texels
        orig = B.load
        B.load = lambda troop: (recipe, g2, rig)
        try:
            with self.assertRaises(B.TrimError) as cm:
                B.build("knight")
            self.assertIn("also used by", str(cm.exception))
        finally:
            B.load = orig

    def test_renderer_draws_trim_for_veterans(self):
        # Static guard (the pixel proof is the in-game check): the renderer registers the Trim layer and picks the overlay from Look.veteran.
        src = (ROOT / "src/main/java/gg/gravesandglory/GeoTroopRenderer.java").read_text(encoding="utf-8")
        self.assertIn("addRenderLayer(new Trim(this))", src)
        self.assertRegex(src, r"if \(!l\.veteran\(\)\) return null;")
        self.assertIn('"_geo/veteran_t"', src)


if __name__ == "__main__":
    unittest.main()
