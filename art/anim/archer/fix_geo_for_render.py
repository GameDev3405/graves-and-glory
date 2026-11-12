"""Work copy of a bow troop's geo for render_geo pictures: bow_* cube rotation z negated.

python art/anim/archer/fix_geo_for_render.py [out] [troop]   (default work/archer_direct/archer.geo.json, archer)

The game is right and render_geo is wrong here (2026-11-12, run archer-geo1b): with the exported geo the game draws a
smooth bow at full draw, while render_geo draws the same file as a sawtooth with the string behind the head. render_geo
reads a cube's rotation z with the opposite sign to the game, so this copy turns the bow cubes back for the pictures.
Nothing else changes: the quiver's lean is fixed at the source (art/anim/common/troop_rig.py geo()), and render_geo
already agrees with the game on bone rotation.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
troop = sys.argv[2] if len(sys.argv) > 2 else "archer"
src = ROOT / f"src/main/resources/assets/gravesandglory/geo/troop/{troop}.geo.json"
out = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "work/archer_direct/archer.geo.json"
g = json.loads(src.read_text(encoding="utf-8"))
n = 0
for b in g["minecraft:geometry"][0]["bones"]:
    if b["name"].startswith("bow_"):
        for c in b.get("cubes", []):
            if c.get("rotation"):
                c["rotation"] = [c["rotation"][0], c["rotation"][1], -c["rotation"][2]]
                n += 1
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(g), encoding="utf-8")
print(out, n, "bow cubes turned")
