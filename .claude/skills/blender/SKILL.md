---
name: blender
description: Expert Blender (bpy) guidance for building 3D models, materials, lighting, animation, VFX and renders by writing and running Python scripts headlessly. Use for any Blender or 3D task - modeling, materials, keyframe, shape-key or armature animation, particles, geometry nodes, rigid bodies, compositing and bloom, Cycles or EEVEE renders, and glTF, OBJ, FBX or USD export.
---

# Blender expert

You make Blender work by writing a Python script that builds the scene, running it headless with `bpy`, and checking the result (a rendered PNG, an exported file, a number read from the scene) before reporting back. Do not describe a result you have not rendered or measured.

## Target and verification

- Target API: **Blender 5.x**, verified on `bpy 5.2.3`. Many online tutorials are for 2.8x-4.x and break on 5.x. See "Blender 5.x API changes" below.
- Anything not listed as verified: probe it first in a two-line script before relying on it, e.g.
  `print([p.identifier for p in bpy.types.BevelModifier.bl_rna.properties])` or `dir(obj)`.

## Setup

```bash
.claude/skills/blender/scripts/setup_bpy.sh                 # once per machine or session (~20 s)
$HOME/.cache/blender-bpy/bin/python path/to/script.py       # run a script headless
```

The setup script installs `bpy` into a venv and the EGL libraries EEVEE needs on Linux. Cycles works without EGL. If a real `blender` binary is installed, `blender -b --factory-startup -P script.py` also works.

Run helper-based scripts from the repo root. Output goes to `build/blender/` (gitignored); set `BLENDER_OUT` to change it.

## Workflow

1. **Plan in meters, Z-up.** Decide the deliverable first: a still, an animation, a game asset, or a file the user will keep editing (save a `.blend`).
2. **Write one script** that imports the helpers (see below) and builds the whole scene from scratch, so it is re-runnable.
3. **Run it and read the output**, including warnings. Fix errors at the line they point to.
4. **Look at the result.** Render a still with `render_still`, or tile sampled frames into a contact sheet with `ffmpeg -vf "select='not(mod(n\,8))',scale=320:180,tile=6x1"` and open the PNG. Check framing, lighting, and whether motion reads correctly.
5. **Export** only what is needed (`export_scene`). Keep the `.blend` if the user will keep editing.

## Helper library: `scripts/bpy_helpers.py`

Import it with:

```python
import os, sys
# From scripts/examples/: the helpers are one folder up.
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from bpy_helpers import *
# From anywhere else, insert the absolute path to .claude/skills/blender/scripts instead.
```

| Helper | Purpose |
|---|---|
| `new_scene()` | Factory-empty scene; enables the Cycles addon |
| `make_material(name, color, roughness, metallic, emission, emission_strength)` | Principled BSDF material |
| `add_primitive(kind, name, location, rotation, scale, material, **dims)` | cube, sphere, cylinder, cone, plane, torus, monkey |
| `mesh_from_pydata(name, verts, faces, location, material)` | Raw geometry from vertex and face lists |
| `bevel(ob, width, segments)` | Angle-limited bevel modifier |
| `smooth_shading(ob, angle_deg)` | Smooth shading, keeping hard edges above the angle |
| `set_keys(ob, data_path, {frame: value})` | Keyframe `location`, `rotation_euler`, `scale`, etc. |
| `object_fcurves(ob)` / `set_interpolation(ob, interp, easing)` | Read or set F-curve keyframes (works on 5.x layered actions) |
| `look_at(ob, target)` | Aim a camera or light (they point along -Z) |
| `add_camera(location, target, lens)` / `frame_camera(cam, targets, margin)` | Camera; `frame_camera` fits objects or points into the frame |
| `add_light(kind, location, target, energy, color, size)` | SUN (energy ~1-5), POINT/SPOT/AREA (watts) |
| `set_world_color(color, strength)` | Solid world background, also ambient light |
| `setup_render(engine, width, height, samples, fps, frame_range)` | `'CYCLES'` or `'BLENDER_EEVEE'` |
| `enable_bloom(threshold)` | Compositor glow for emissive surfaces |
| `render_still(path)` / `render_sequence(folder)` | PNG stills or frame sequences |
| `export_scene(path)` | Format from extension: `.glb .gltf .obj .fbx .stl .ply .usdc .blend` |

Examples in `scripts/examples/`, all verified to run:

- `01_model_crate.py` - beveled prop, metal bands, torus handle, preview render, `.glb` export
- `02_animate_bounce.py` - keyframed bounce with squash and stretch, frame sequence, mp4 (if ffmpeg exists), animated `.glb`
- `03_vfx_sparks.py` - particle sparks instancing an emissive sphere, bloom in the compositor

## Modeling

- Start from primitives or `mesh_from_pydata`. Apply scale before beveling if you need exact bevel widths, since bevel width is in local space and non-uniform scale distorts it.
- Model like an artist: quads where the mesh will deform, loops along bevels, no hidden n-gons on characters, consistent normals (`recalc normals`), and origin at the base for props and characters.
- Modifiers stay live until export. Blender's glTF exporter does **not** apply them by default, so the crate's bevels would be missing from the `.glb`. `export_scene` sets `export_apply` (glTF), `apply_modifiers` (OBJ, STL, PLY) and `use_mesh_modifiers` (FBX) explicitly. Verified: the re-imported crate body has 139 vertices, not the 24 of an unbeveled cube.

## Materials

- Principled BSDF inputs in 5.x include `Base Color`, `Roughness`, `Metallic`, `IOR`, `Alpha`, `Coat Weight`, `Emission Color`, `Emission Strength`, `Subsurface Weight`, `Transmission Weight`. Set them with `inputs["Name"].default_value`.
- Glowing material: give it an emission color and set `Emission Strength` above 1. Bloom only picks up bright pixels.
- Textures: add `ShaderNodeTexImage`, link its Color output to Base Color, and set `image.colorspace_settings.name = "Non-Color"` for roughness, metallic and normal maps. Normal maps also need a `ShaderNodeNormalMap` between the image and the Normal input.

## Lighting and camera

- Sun strength 1-5. Point, spot and area lights take watts (hundreds to thousands for a room).
- Use `frame_camera(cam, [obj, (x, y, z), ...])` rather than hand-picking distances. The narrower field of view decides the fit.
- Thin or dim scenes often need a world color or a second light so shadows do not go to black.

## Animation

- Keyframe in order: set the value, then `keyframe_insert` at that frame. `set_keys` does this.
- Squash and stretch: keep volume roughly constant (xy up when z goes down). On impact, scale z down and xy out.
- Shape keys: `obj.shape_key_add(name, from_mix=False)`, then `key_block.keyframe_insert("value", frame=n)`. Verified.
- Armatures: `bpy.ops.object.armature_add()`, edit bones in Edit Mode, then `pose.bones[i].keyframe_insert("rotation_quaternion", frame=n)` in Pose Mode (verified on the default bone). Rigify ships but is off by default: `addon_utils.enable("rigify", default_set=True)` enables it (verified), and `bpy.ops.object.armature_human_metarig_add()` then becomes available.
- Interpolation: `BEZIER` with `AUTO` easing for natural motion, `LINEAR` for constant spins, `CONSTANT` for flicker or stepped animation.
- Animations reach the glTF file: `02_animate_bounce.py` produces a `.glb` with one animation and its location and scale channels (verified).

## VFX

- **Particles** (verified): `bpy.ops.object.particle_system_add()` on the emitter, then `ps.settings` with `count`, `lifetime`, `frame_start`, `frame_end`, `normal_factor` (launch speed along face normals), `factor_random`, `emit_from = "FACE"`, `effector_weights.gravity`, `render_type = "OBJECT"` and `instance_object`. See `03_vfx_sparks.py`.
- **Geometry nodes** (verified): build a node tree with `bpy.data.node_groups.new(name, "GeometryNodeTree")`, declare sockets with `ng.interface.new_socket(...)`, then add a `NODES` modifier with `modifier.node_group = ng`. Scattering works (points on faces, instance on points). Note: the output replaces the object's geometry, so join the input geometry back in if you want to keep the original mesh.
- **Rigid bodies** (verified): `bpy.ops.rigidbody.world_add()`, then `rigidbody.object_add(type="ACTIVE")` for falling objects and `type="PASSIVE"` for floors. Simulate by stepping frames in order (`frame_set(f)` for each f). Jumping straight to frame 60 gives no fall.
- **Bloom and glow** (verified): `enable_bloom(threshold)` builds the compositor graph and turns on compositing.
- Fluids, smoke, cloth and fire use the same physics-cache approach, but are not verified in this skill. Probe the API before relying on it.

## Rendering

- **Cycles**: path tracer, the default for quality. Works headless on CPU. Use 16-64 samples for previews, 128+ for finals.
- **EEVEE** (`'BLENDER_EEVEE'`): real-time, faster for previews. Needs the EGL libraries on headless Linux (`setup_bpy.sh` installs them).
- Resolution: set `render.resolution_x/y` and `resolution_percentage`. Use a PNG output for stills and sequences.
- Animation: set `frame_start` and `frame_end` (or `frame_range=` in `setup_render`), then `render_sequence`. Make an mp4 with `ffmpeg -framerate 24 -i frame_%04d.png -c:v libx264 -pix_fmt yuv420p out.mp4`.

## Export

| Target | Format | Notes |
|---|---|---|
| Web, Godot, Unity, Unreal, three.js | `.glb` | Single file, materials and animations included. Exporter converts Z-up to Y-up by default (verified). |
| Older tools, 3D printing | `.obj` (+ `.mtl`), `.stl`, `.ply` | Static meshes. OBJ has no animation. STL has no color. |
| Unity/Unreal interchange | `.fbx` | Includes animations and armatures. |
| Houdini, Omniverse, USD pipelines | `.usdc` | Verified export. |
| Keep editing later | `.blend` | Source of truth. |

**Project note.** This repo is a Minecraft Java Edition mod. Minecraft does not load `.glb` or `.obj` directly. Its models are JSON with cuboid elements and separate texture files. Converting Blender output into that format is a separate step that these helpers do not cover.

## Blender 5.x API changes (verified on bpy 5.2.3)

- **Action F-curves moved.** `action.fcurves` no longer exists. Actions are layered, and F-curves live in the channelbag for the slot an object uses: `ob.animation_data.action.layers[0].strips[0].channelbag(ob.animation_data.action_slot).fcurves`. Use `object_fcurves(ob)`.
- **Cycles is an addon.** Enable it with `addon_utils.enable("cycles", default_set=True)` before the engine appears. `new_scene()` does this.
- **One EEVEE engine id.** The only identifier is `BLENDER_EEVEE`.
- **Composite node removed.** `CompositorNodeComposite` and `CompositorNodeGroupOutput` do not exist. Use `scene.compositing_node_group`, a `NodeGroupOutput` node, and an interface socket (see `enable_bloom`). Render Layers (`CompositorNodeRLayers`) and Glare (`CompositorNodeGlare`) exist.
- **Glare type uses display names.** Set `inputs["Type"].default_value = "Bloom"`. Other values: `Ghosts`, `Streaks`, `Fog Glow`, `Simple Star`, `Sun Beams`, `Kernel`.
- **Primitive operators are inconsistent.** Not every primitive accepts `scale` (the torus does not). Set `obj.scale` afterwards.
- **Operators return `{'FINISHED'}`**, not the new object. Read the object from `bpy.context.active_object`.

## Gotchas

- `bpy.ops.wm.read_factory_settings(use_empty=True)` wipes the scene. Start each script with it (`new_scene()`).
- Operators need Object Mode and an active object. Call `bpy.ops.object.mode_set(mode="OBJECT")` first if a script might leave Edit Mode.
- Deformed or simulated results live on the evaluated object. Read them with `ob.evaluated_get(bpy.context.evaluated_depsgraph_get())`, not `ob.data.vertices`. Shape keys and rigid bodies both need this (verified).
- On the original object, `matrix_world` does not show rigid-body simulation results (verified: it stayed at the start height). Read positions from the evaluated object.
- Harmless log noise: `ERROR Draco ... not available` and `MeshOptimizer ...` (optional compression libraries are not in the wheel). `EGL Error` lines during EEVEE render are also harmless as long as an image is written.
- Relative render and export paths resolve against the open `.blend`, which may not be where you expect. Use absolute paths (the helpers do).
- Shade smooth with `shade_smooth_by_angle` (in `smooth_shading`). The plain `shade_smooth` makes every edge soft, including hard corners.
- Units are meters, 1 Blender unit = 1 m. Game engines often expect other scales. Check the target before exporting.
- Don't commit renders. Everything under `build/` is gitignored.

## Limits

- Headless Blender has no GUI. Viewport-only tasks, such as sculpting by eye or hand-placing UVs, must be described as code, or the user must do them in the Blender window.
- You can't watch motion. Check it by sampling frames, measuring positions from the evaluated scene, or both.
