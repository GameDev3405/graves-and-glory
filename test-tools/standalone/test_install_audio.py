"""Tests tools/install_audio.py against a temporary repo and candidate folder (no real audio is copied)."""
import contextlib, io, json, sys, tempfile, unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tools"))
import install_audio as ia


class InstallAudioTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        t = Path(self.tmp.name)
        self.repo, self.cues = t / "repo", t / "cues"
        sd = self.repo / ia.SOUNDS_REL
        sd.mkdir(parents=True)
        (self.repo / "src/main/java/gg/gravesandglory").mkdir(parents=True)
        (self.repo / "src/main/java/gg/gravesandglory/Veterans.java").write_text(
            "static final SoundEvent FLASH = SoundEvents.ENTITY_PLAYER_LEVELUP;\n")
        (self.repo / ia.JSON_REL).write_text(json.dumps(
            {"victory": {"subtitle": "subtitles.x.victory", "sounds": [{"name": "gravesandglory:victory", "stream": False}]}}, indent=2) + "\n")
        for cue, n in (("veteran_levelup", 3), ("title_music", 2), ("mortar", 0)):
            (self.cues / cue).mkdir(parents=True)
        for i in (1, 2, 3):
            (self.cues / "veteran_levelup" / f"veteran_levelup_{i}.ogg").write_bytes(b"OggS%d" % i)
        for i in (1, 2):
            (self.cues / "title_music" / f"title_music_{i}.ogg").write_bytes(b"OggT%d" % i)
        (self.cues / "mortar" / "mortar_fire_1.ogg").write_bytes(b"M1")
        (self.cues / "mortar" / "shell_burst_1.ogg").write_bytes(b"M2")
        (self.cues / "victory.ogg").write_bytes(b"NEWVICTORY")
        mk = lambda f, a: {"item": "x", "file": f, "url": "http://u/" + a, "author": a, "licence": "CC0", "credit": "No credit"}
        (self.cues / "manifest.json").write_text(json.dumps({
            "veteran_levelup_2": mk("veteran_levelup/veteran_levelup_2.ogg", "ann"),
            "title_music_1": mk("title_music/title_music_1.ogg", "bob"),
            "mortar_fire_1": mk("mortar/mortar_fire_1.ogg", "cy")}))
        self.picks = self.cues / "PICKS.json"
        self.picks.write_text(json.dumps({
            "veteran_levelup": ["veteran_levelup_2.ogg", "veteran_levelup_3.ogg"],
            "title_music": ["title_music_1.ogg"],
            "mortar": ["mortar_fire_1.ogg", "shell_burst_1.ogg"],
            "victory": ["victory.ogg"]}))

    def tearDown(self):
        self.tmp.cleanup()

    def run_tool(self, *extra):
        out = io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
            code = ia.main(["--repo", str(self.repo), "--cues", str(self.cues), *extra])
        return code, out.getvalue()

    def test_dry_run_writes_nothing(self):
        before = (self.repo / ia.JSON_REL).read_text()
        code, out = self.run_tool("--dry-run")
        self.assertEqual(code, 0)
        self.assertIn("[dry run]", out)
        self.assertEqual((self.repo / ia.JSON_REL).read_text(), before)
        self.assertEqual([p.name for p in (self.repo / ia.SOUNDS_REL).iterdir()], [])
        self.assertFalse((self.repo / ia.CREDITS_REL).exists())

    def test_install(self):
        code, out = self.run_tool()
        self.assertEqual(code, 0, out)
        sd = self.repo / ia.SOUNDS_REL
        self.assertEqual(sorted(p.name for p in sd.iterdir()),
                         ["mortar_fire_1.ogg", "shell_burst_1.ogg", "title_music_1.ogg", "veteran_levelup_2.ogg", "veteran_levelup_3.ogg", "victory.ogg"])
        self.assertEqual((sd / "victory.ogg").read_bytes(), b"NEWVICTORY")
        data = json.loads((self.repo / ia.JSON_REL).read_text())
        self.assertEqual([s["name"] for s in data["veteran_levelup"]["sounds"]],
                         ["gravesandglory:veteran_levelup_2", "gravesandglory:veteran_levelup_3"])
        self.assertTrue(data["title_music"]["sounds"][0]["stream"])
        self.assertIn("mortar_fire", data)
        self.assertIn("shell_burst", data)
        self.assertEqual(data["victory"]["subtitle"], "subtitles.x.victory")  # kept
        self.assertEqual(data["victory"]["sounds"][0]["name"], "gravesandglory:victory")
        credits = (self.repo / ia.CREDITS_REL).read_text()
        self.assertIn("| veteran_levelup | veteran_levelup_2.ogg | ann |", credits)
        self.assertIn("| mortar_fire | mortar_fire_1.ogg | cy |", credits)
        self.assertNotIn("veteran_levelup_3", credits)  # not in the manifest
        # idempotent: a second run adds no credit rows
        self.run_tool()
        self.assertEqual((self.repo / ia.CREDITS_REL).read_text(), credits)

    def test_bad_pick_writes_nothing(self):
        self.picks.write_text(json.dumps({"mortar": ["nope.ogg"], "ghost": ["a.ogg"]}))
        code, out = self.run_tool()
        self.assertEqual(code, 1)
        self.assertIn("nope.ogg not found", out)
        self.assertIn("ghost: no such cue", out)
        self.assertEqual([p.name for p in (self.repo / ia.SOUNDS_REL).iterdir()], [])

    def test_report_lists_call_sites_only(self):
        code, out = self.run_tool("--report")
        self.assertEqual(code, 0)
        self.assertIn("Veterans.java:1", out)
        self.assertEqual([p.name for p in (self.repo / ia.SOUNDS_REL).iterdir()], [])

    def test_example_is_first_take_of_each_cue(self):
        code, out = self.run_tool("--example")
        ex = json.loads(out)
        self.assertEqual(ex["veteran_levelup"], ["veteran_levelup_1.ogg"])
        self.assertEqual(ex["mortar"], ["mortar_fire_1.ogg", "shell_burst_1.ogg"])
        self.assertEqual(ex["victory"], ["victory.ogg"])


if __name__ == "__main__":
    unittest.main()
