"""Forge sparks VFX: a particle system emitting glowing instanced sparks, with bloom from the compositor.

Run from the repo root:
    $VENV/bin/python .claude/skills/blender/scripts/examples/03_vfx_sparks.py
Outputs go to build/blender/sparks/ (override with BLENDER_OUT).
"""

import os
import sys

import bpy

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from bpy_helpers import (add_camera, add_primitive, enable_bloom, export_scene, frame_camera,
                         make_material, new_scene, out_dir, render_still, set_world_color,
                         setup_render)

new_scene()

# The spark is an emissive sphere. Strength above 1 is what makes bloom pick it up.
spark_mat = make_material("Spark", color=(1, 0.5, 0.1, 1), emission=(1, 0.45, 0.1, 1),
                          emission_strength=25.0)
spark = add_primitive("sphere", "SparkTemplate", radius=0.015, material=spark_mat)
spark.hide_render = True  # particles instance it; the template itself should not render

# The hearth is the emitter. Particles spawn from its faces and move along their normals.
# It also renders, as a dark block, which is fine for a forge.
hearth_mat = make_material("Hearth", color=(0.12, 0.1, 0.09, 1), roughness=0.9)
hearth = add_primitive("cube", "Hearth", location=(0, 0, 0.1), scale=(0.5, 0.5, 0.1), material=hearth_mat)

bpy.context.view_layer.objects.active = hearth
bpy.ops.object.particle_system_add()
pset = hearth.particle_systems[-1].settings
pset.name = "Sparks"
pset.emit_from = "FACE"
pset.count = 600
pset.frame_start = 1
pset.frame_end = 30          # emit for the first 30 frames only
pset.lifetime = 40
pset.normal_factor = 2.2     # launch speed along the face normal (up)
pset.factor_random = 0.6     # randomness in launch speed
pset.effector_weights.gravity = 1.0
pset.render_type = "OBJECT"  # render each particle as an instance of the spark
pset.instance_object = spark
pset.particle_size = 1.0

set_world_color(color=(0.02, 0.02, 0.03), strength=1.0)
add_primitive("plane", "Ground", location=(0, 0, -0.01), scale=(60, 60, 1), material=hearth_mat)
cam = add_camera(location=(2.2, -3.4, 1.2), target=(0, 0, 0.6), lens=50)
frame_camera(cam, [(-0.5, -0.5, 0), (0.5, 0.5, 1.6)], margin=1.1)

setup_render("CYCLES", width=960, height=540, samples=64)
enable_bloom(threshold=0.6)

# Still at the middle of the emission so the trails are visible. The sequence is a few frames in.
bpy.context.scene.frame_set(24)
out = out_dir()
os.makedirs(os.path.join(out, "sparks"), exist_ok=True)
print("STILL", render_still(os.path.join(out, "sparks", "sparks_frame24.png")))
print("EXPORT", export_scene(os.path.join(out, "sparks", "hearth.glb")))
