"""tools/prop_sheet.py density check (plan 2026-11-08-prop-kit, Task A1)."""
import json, os, subprocess, sys, tempfile, unittest
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TOOL = os.path.join(ROOT, "tools", "prop_sheet.py")

def cube(tex_px, face_px=16):
    k = 16.0 * face_px / tex_px   # uv units for face_px texels
    return {"texture_size": [tex_px, tex_px], "textures": {"0": "gravesandglory:prop/box"},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                          "faces": {f: {"uv": [0, 0, k, k], "texture": "#0"} for f in ("north", "south", "east", "west", "up", "down")}}],
            "display": {"fixed": {"translation": [0, 0, 0]}}}

def run(model, tex_px):
    d = tempfile.mkdtemp(); p = os.path.join(d, "box"); os.makedirs(p)
    Image.new("RGBA", (tex_px, tex_px), (120, 120, 120, 255)).save(os.path.join(p, "box.png"))
    json.dump(model, open(os.path.join(p, "box.json"), "w"))
    return subprocess.run([sys.executable, TOOL, "--art", d, "--props", "box", "--check-only"], capture_output=True, text=True)

class PropSheetDensity(unittest.TestCase):
    def test_16px_cube_passes(self):
        r = run(cube(16), 16)
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)

    def test_32px_texture_on_16_unit_face_fails(self):
        r = run(cube(32, face_px=32), 32)   # each 16 px face gets 32 texels
        self.assertEqual(r.returncode, 1, r.stdout + r.stderr)
        self.assertIn("box/box element 0 face north", r.stdout)

    def test_shipped_props_pass(self):
        art = os.path.join(ROOT, "art", "props")
        if not os.path.isdir(os.path.join(art, "forge")): self.skipTest("no prop art yet")
        r = subprocess.run([sys.executable, TOOL, "--check-only"], capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)

if __name__ == "__main__":
    unittest.main()
