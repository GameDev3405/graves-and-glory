"""Every shipped sound file keeps the mix rules (audio audit 2026-11-09, step 2): no clipping, no near-peak file (true peak above -1.0 dBTP),
every listed variant group within 1.5 LU, the rarity ladder rising and ui_back near ui_confirm. It runs tools/audio/mixfix.py --verify,
which measures every .ogg with ffmpeg (about 5 s) and changes nothing. Skipped where ffmpeg is not installed (the tool looks for it
itself). To fix a failure: python tools/audio/mixfix.py --dry-run, then without --dry-run (docs/reports/mixfix-2026-11-09.md)."""
import subprocess, sys, unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOL = ROOT / "tools/audio/mixfix.py"


class AudioMixRules(unittest.TestCase):
    def test_mixfix_verify_passes(self):
        if not TOOL.exists():
            self.skipTest("tools/audio/mixfix.py is not in this checkout")
        r = subprocess.run([sys.executable, "-I", str(TOOL), "--verify"], cwd=ROOT, capture_output=True, text=True,
                           encoding="utf8", errors="replace", timeout=300)
        out = (r.stdout + r.stderr).strip()
        if r.returncode != 0 and "cannot find ffmpeg" in out.lower():
            self.skipTest("ffmpeg is not available: " + out.splitlines()[-1])
        self.assertEqual(r.returncode, 0, "sound files break the mix rules; fix with tools/audio/mixfix.py:\n" + out[-2500:])


if __name__ == "__main__":
    unittest.main()
