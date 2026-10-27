"""Animate a bouncing ball with squash and stretch, render the frames, and export an animated .glb.

Run from the repo root:
    $VENV/bin/python .claude/skills/blender/scripts/examples/02_animate_bounce.py
Outputs go to build/blender/bounce/ (override with BLENDER_OUT).
"""

import os
import shutil
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from bpy_helpers import (add_camera, add_light, add_primitive, export_scene, frame_camera,
                         make_material, new_scene, object_fcurves, out_dir, render_sequence,
                         set_interpolation, set_keys, set_world_color, setup_render)

new_scene()

ball_mat = make_material("Ball", color=(0.8, 0.12, 0.08, 1), roughness=0.35)
ground_mat = make_material("Ground", color=(0.55, 0.55, 0.52, 1), roughness=0.9)

# Ball radius 0.5 m, so resting on the ground means z = 0.5.
ball = add_primitive("sphere", "Ball", location=(0, 0, 3.0), radius=0.5, material=ball_mat)
add_primitive("plane", "Ground", scale=(60, 60, 1), material=ground_mat)

# Keyframes are set in order: value first, then keyframe_insert at that frame.
# Squash on impact (scale z down, xy out), stretch in the air. Keep the volume roughly constant.
SQUASH = (1.3, 1.3, 0.75)
STRETCH = (0.92, 0.92, 1.1)
set_keys(ball, "location", {1: (0, 0, 3.0), 13: (0, 0, 0.5), 19: (0, 0, 2.4),
                            31: (0, 0, 0.5), 40: (0, 0, 1.0), 48: (0, 0, 0.5)})
set_keys(ball, "scale", {1: (1, 1, 1), 13: SQUASH, 19: STRETCH,
                         31: SQUASH, 40: (0.95, 0.95, 1.05), 48: (1.25, 1.25, 0.8)})
set_interpolation(ball, "BEZIER", "AUTO")

# Sanity check on keyframe count, using the helper that handles Blender 5 layered actions.
print("FCURVES", len(object_fcurves(ball)), "KEYS PER CURVE", [len(fc.keyframe_points) for fc in object_fcurves(ball)])

set_world_color(color=(0.5, 0.6, 0.75), strength=0.8)
add_light("SUN", location=(3, -5, 8), energy=3.5)
# Frame the whole path of the ball (every frame), not just its position on one frame.
cam = add_camera(location=(3.3, -4.0, 1.6), target=(0, 0, 1.6), lens=50)
frame_camera(cam, [(-0.6, -0.6, 0), (0.6, 0.6, 3.4)], margin=1.1)

setup_render("CYCLES", width=480, height=270, samples=16, fps=24, frame_range=(1, 48))

out = os.path.join(out_dir(), "bounce")
frames_dir = render_sequence(os.path.join(out, "frames"))
print("FRAMES", frames_dir, len(os.listdir(frames_dir)))

# Video is optional: only if ffmpeg is installed on this machine.
if shutil.which("ffmpeg"):
    mp4 = os.path.join(out, "bounce.mp4")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", "24",
                    "-i", os.path.join(frames_dir, "frame_%04d.png"),
                    "-c:v", "libx264", "-pix_fmt", "yuv420p", mp4], check=True)
    print("VIDEO", mp4)

print("EXPORT", export_scene(os.path.join(out, "bounce.glb")))
