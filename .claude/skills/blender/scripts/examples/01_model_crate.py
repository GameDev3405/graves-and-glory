"""Model a stylized wooden crate with iron bands, then render a preview and export a .glb.

Run from the repo root:
    $VENV/bin/python .claude/skills/blender/scripts/examples/01_model_crate.py
Outputs go to build/blender/ (override with BLENDER_OUT).
"""

import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from bpy_helpers import (add_camera, add_light, add_primitive, bevel, export_scene,
                         make_material, new_scene, out_dir, render_still, set_world_color,
                         setup_render, smooth_shading)

# Units are meters. Crate is 1.0 m wide, 0.7 m deep, 0.6 m tall, sitting on z = 0.
new_scene()

wood = make_material("Wood", color=(0.42, 0.25, 0.12, 1), roughness=0.75)
iron = make_material("Iron", color=(0.55, 0.56, 0.6, 1), roughness=0.35, metallic=1.0)
ground = make_material("Ground", color=(0.18, 0.2, 0.16, 1), roughness=0.95)

# Body: a unit cube scaled to size. Bevel width is in local space, so keep scale close to
# uniform or apply the scale to the mesh before beveling.
body = add_primitive("cube", "CrateBody", location=(0, 0, 0.3), scale=(0.5, 0.35, 0.3), material=wood)
bevel(body, width=0.015, segments=3)
smooth_shading(body, angle_deg=30)

# Iron bands: thin straps across the top and bottom, plus corner brackets.
for z, name in [(0.02, "BandBottom"), (0.58, "BandTop")]:
    add_primitive("cube", name, location=(0, 0, z), scale=(0.52, 0.37, 0.02), material=iron)

for x in (-0.48, 0.48):
    for y in (-0.33, 0.33):
        add_primitive("cube", f"Bracket_{x}_{y}", location=(x, y, 0.3), scale=(0.03, 0.03, 0.31),
                      material=iron)

# Handle: a half-torus on the front face.
handle = add_primitive("torus", "Handle", location=(0, -0.36, 0.42), rotation=(1.5708, 0, 0),
                       material=iron, major_radius=0.12, minor_radius=0.012)

# Ground plane for context.
add_primitive("plane", "Ground", scale=(3, 3, 1), material=ground)

set_world_color(color=(0.35, 0.4, 0.5), strength=0.6)
add_light("SUN", location=(3, -4, 6), energy=3.0)
add_camera(location=(1.8, -2.2, 1.2), target=(0, 0, 0.3), lens=50)

setup_render("CYCLES", width=960, height=720, samples=48)

out = out_dir()
print("PREVIEW", render_still(os.path.join(out, "crate_preview.png")))
print("EXPORT", export_scene(os.path.join(out, "crate.glb")))
