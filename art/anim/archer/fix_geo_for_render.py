"""Work copy of the archer geo for demo renders: the bow fix plus the quiver's lean.

python art/anim/archer/fix_geo_for_render.py [out]   (default work/archer_direct/archer.geo.json, rebuilt from the exported geo)

1. bow_* cubes: rotation z negated (the known bow fault: troop_rig.py writes the weapon builder's cube turn unchanged).
2. quiver bone: rotation z negated (28 -> -28). Evidence (2026-11-11, archer fix round 1):
   the hand model art/hand/archer/build_archer.py keys the quiver rot=(0, 0, -28) in its ModelPart frame (y down,
   x + the troop's left: left_arm pivot x +5). The legacy renderer (TroopModelLoader -> ModelTransform, rotationZYX)
   puts the fletching top (0, -9) at x = 9 * sin(-28) = -4.2: over the RIGHT (string-hand) shoulder.
   art/anim/common/troop_rig.py geo() writes bone rotation as (-rx, ry, -rz) (a plain y flip). GeckoLib bakes a bone
   rotation as (-rx, -ry, +rz) on an x-mirrored model and turns the entity 180 degrees, so in the world a file rotation
   (fx, fy, fz) acts as Rz(-fz) Ry(-fy) Rx(fx), while the legacy part acts as Rz(-rz) Ry(-ry) Rx(rx): the file wants
   the hand model's (rx, ry, rz) unchanged, and the exporter's x and z come out mirrored. render_geo (which mirrors
   GeckoLib 4.8.4) shows the fletching at x +4.2, over the LEFT (bow) shoulder: the same family as the bow fault.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
src = ROOT / "src/main/resources/assets/gravesandglory/geo/troop/archer.geo.json"
out = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "work/archer_direct/archer.geo.json"
g = json.loads(src.read_text(encoding="utf-8"))
n = 0
for b in g["minecraft:geometry"][0]["bones"]:
    if b["name"].startswith("bow_"):
        for c in b.get("cubes", []):
            if c.get("rotation"):
                c["rotation"] = [c["rotation"][0], c["rotation"][1], -c["rotation"][2]]
                n += 1
    if b["name"] == "quiver":
        r = b["rotation"]
        b["rotation"] = [r[0], r[1], -r[2]]
        print("quiver rotation", r, "->", b["rotation"])
out.write_text(json.dumps(g), encoding="utf-8")
print(out, n, "bow cubes turned")
