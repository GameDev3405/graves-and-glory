"""Helpers for building Blender scenes from Python (bpy, Blender 5.x API).

Use from an example or scratch script:

    import os, sys
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    from bpy_helpers import *

Run with a bpy module (see setup_bpy.sh) or `blender -b --factory-startup -P script.py`.
Verified against bpy 5.2.3. Blender 4.x differs in a few places, noted inline.
"""

import os

import bpy
from mathutils import Vector


def out_dir():
    """Output folder for renders and exports. Defaults to ./build/blender (gitignored)."""
    path = os.environ.get("BLENDER_OUT", os.path.abspath(os.path.join("build", "blender")))
    os.makedirs(path, exist_ok=True)
    return path


def new_scene():
    """Start from an empty factory scene and enable Cycles (its addon is off by default)."""
    import addon_utils  # only importable after bpy is loaded

    bpy.ops.wm.read_factory_settings(use_empty=True)
    addon_utils.enable("cycles", default_set=True)
    return bpy.context.scene


def make_material(name, color=(0.8, 0.8, 0.8, 1.0), roughness=0.5, metallic=0.0,
                  emission=None, emission_strength=0.0):
    """Principled BSDF material. Colors are RGBA in 0-1 linear values."""
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = color
    bsdf.inputs["Roughness"].default_value = roughness
    bsdf.inputs["Metallic"].default_value = metallic
    if emission is not None:
        bsdf.inputs["Emission Color"].default_value = emission
        bsdf.inputs["Emission Strength"].default_value = emission_strength
    return mat


def add_primitive(kind, name, location=(0, 0, 0), rotation=(0, 0, 0), scale=(1, 1, 1),
                  material=None, **dims):
    """Add a primitive mesh. kind: cube, sphere, cylinder, cone, plane, torus, monkey.

    dims are passed to the matching bpy.ops.mesh.primitive_*_add (size, radius, depth, ...).
    Objects are created at the 3D cursor in the active collection; scripts start in Object Mode.
    """
    ops = {
        "cube": bpy.ops.mesh.primitive_cube_add,
        "sphere": bpy.ops.mesh.primitive_uv_sphere_add,
        "cylinder": bpy.ops.mesh.primitive_cylinder_add,
        "cone": bpy.ops.mesh.primitive_cone_add,
        "plane": bpy.ops.mesh.primitive_plane_add,
        "torus": bpy.ops.mesh.primitive_torus_add,
        "monkey": bpy.ops.mesh.primitive_monkey_add,
    }
    # Operators return a set like {'FINISHED'}, not the object. Read the object from context.
    # Scale is set afterwards: not every primitive operator accepts a scale argument.
    ops[kind](location=location, rotation=rotation, **dims)
    ob = bpy.context.active_object
    ob.scale = scale
    ob.name = name
    ob.data.name = name
    if material is not None:
        ob.data.materials.append(material)
    return ob


def mesh_from_pydata(name, verts, faces, location=(0, 0, 0), material=None):
    """Build an object from raw vertices and faces (faces are index tuples, counter-clockwise seen from outside)."""
    me = bpy.data.meshes.new(name)
    me.from_pydata(verts, [], faces)
    me.update()
    ob = bpy.data.objects.new(name, me)
    bpy.context.collection.objects.link(ob)
    ob.location = location
    if material is not None:
        me.materials.append(material)
    return ob


def bevel(ob, width=0.05, segments=3):
    """Bevel modifier limited to sharp edges (angle-based), which reads as a real model edge."""
    mod = ob.modifiers.new("Bevel", "BEVEL")
    mod.width = width
    mod.segments = segments
    mod.limit_method = "ANGLE"
    return mod


def smooth_shading(ob, angle_deg=30):
    """Smooth shading, keeping hard edges sharper than angle_deg. Needs Object Mode."""
    import math
    bpy.context.view_layer.objects.active = ob
    ob.select_set(True)
    bpy.ops.object.shade_smooth_by_angle(angle=math.radians(angle_deg))


def set_keys(ob, data_path, frames):
    """Keyframe a property. frames maps frame number to value, e.g. set_keys(ball, "location", {1: (0,0,0), 24: (0,0,3)})."""
    for frame, value in sorted(frames.items()):
        setattr(ob, data_path, value)
        ob.keyframe_insert(data_path, frame=frame)


def object_fcurves(ob):
    """F-curves of an object's action (works on Blender 4.x and 5.x layered actions)."""
    ad = ob.animation_data
    if ad is None or ad.action is None:
        return []
    if hasattr(ad.action, "fcurves"):  # Blender 4.x and earlier
        return list(ad.action.fcurves)
    # Blender 5.x: F-curves live in the channelbag of the slot this object uses.
    return [fc for layer in ad.action.layers for strip in layer.strips
            for fc in strip.channelbag(ad.action_slot).fcurves]


def set_interpolation(ob, interpolation="BEZIER", easing="AUTO"):
    """Set every keyframe on ob. interpolation: BEZIER, LINEAR, CONSTANT, BACK, BOUNCE, ELASTIC...
    easing (Bezier only): AUTO, EASE_IN, EASE_OUT, EASE_IN_OUT."""
    for fc in object_fcurves(ob):
        for kp in fc.keyframe_points:
            kp.interpolation = interpolation
            kp.easing = easing


def look_at(ob, target):
    """Aim ob so its -Z axis points at target. Cameras and spot/sun lights point along -Z."""
    direction = Vector(target) - Vector(ob.location)
    ob.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()


def add_camera(location, target=(0, 0, 0), lens=50):
    bpy.ops.object.camera_add(location=location)
    cam = bpy.context.active_object
    cam.data.lens = lens
    look_at(cam, target)
    bpy.context.scene.camera = cam
    return cam


def frame_camera(cam, targets, margin=1.15):
    """Move cam back along its current direction until everything in targets fits the frame.

    targets: Objects (their bounding boxes, at the current frame) and/or 3D points.
    Use points to frame a whole animation path. Do this instead of hand-picking distances:
    the narrower field of view decides the fit. Call after the camera is placed, before rendering.
    """
    import math
    points = []
    for target in targets:
        if isinstance(target, bpy.types.Object):
            points += [target.matrix_world @ Vector(corner) for corner in target.bound_box]
        else:
            points.append(Vector(target))
    center = sum(points, Vector()) / len(points)
    radius = max((p - center).length for p in points)
    fov = min(cam.data.angle_x, cam.data.angle_y)
    distance = margin * radius / math.sin(fov / 2)
    direction = (Vector(cam.location) - center).normalized()
    cam.location = center + direction * distance
    look_at(cam, center)
    return cam


def add_light(kind="SUN", location=(4, -4, 6), target=(0, 0, 0), energy=3.0, color=(1, 1, 1), size=None):
    """kind: SUN (energy ~1-5, strength), POINT or SPOT or AREA (energy in watts, e.g. 500-2000)."""
    bpy.ops.object.light_add(type=kind, location=location)
    light = bpy.context.active_object
    light.data.energy = energy
    light.data.color = color
    if size is not None and kind == "AREA":
        light.data.size = size
    look_at(light, target)
    return light


def set_world_color(color=(0.05, 0.06, 0.08), strength=1.0):
    """Solid-color world background; it also lights the scene (ambient)."""
    scene = bpy.context.scene
    if scene.world is None:
        scene.world = bpy.data.worlds.new("World")
    scene.world.use_nodes = True
    bg = scene.world.node_tree.nodes["Background"]
    bg.inputs["Color"].default_value = (*color, 1.0)
    bg.inputs["Strength"].default_value = strength


def setup_render(engine="CYCLES", width=1280, height=720, samples=64, fps=24, frame_range=None):
    """engine: 'CYCLES' (path tracer, works headless on CPU) or 'BLENDER_EEVEE' (real-time, needs libEGL)."""
    scene = bpy.context.scene
    scene.render.engine = engine
    scene.render.resolution_x = width
    scene.render.resolution_y = height
    scene.render.resolution_percentage = 100
    scene.render.fps = fps
    scene.render.image_settings.file_format = "PNG"
    if frame_range is not None:
        scene.frame_start, scene.frame_end = frame_range
    if engine == "CYCLES":
        scene.cycles.device = "CPU"
        scene.cycles.samples = samples
    else:
        scene.eevee.taa_render_samples = samples
    return scene


def enable_bloom(threshold=0.8):
    """Compositor glow: bloom on anything brighter than threshold (emissive sparks, fire, lights).

    Blender 5.x: the compositor ends in a Group Output node fed by an interface socket.
    The old Composite node no longer exists. Glare type is a socket with display-name values.
    """
    scene = bpy.context.scene
    scene.render.use_compositing = True
    tree = bpy.data.node_groups.new("Bloom", "CompositorNodeTree")
    scene.compositing_node_group = tree
    tree.interface.new_socket("Image", in_out="OUTPUT", socket_type="NodeSocketColor")
    layers = tree.nodes.new("CompositorNodeRLayers")
    glare = tree.nodes.new("CompositorNodeGlare")
    glare.inputs["Type"].default_value = "Bloom"
    glare.inputs["Threshold"].default_value = threshold
    out = tree.nodes.new("NodeGroupOutput")
    tree.links.new(layers.outputs["Image"], glare.inputs["Image"])
    tree.links.new(glare.outputs["Image"], out.inputs["Image"])
    return tree


def render_still(path):
    """Render the current frame to a PNG at path."""
    scene = bpy.context.scene
    scene.render.filepath = os.path.abspath(path)
    bpy.ops.render.render(write_still=True)
    return scene.render.filepath


def render_sequence(folder, prefix="frame_"):
    """Render the full frame range to folder/prefix0001.png, ... (use ffmpeg afterwards for video)."""
    os.makedirs(folder, exist_ok=True)
    scene = bpy.context.scene
    scene.render.filepath = os.path.join(os.path.abspath(folder), prefix)
    bpy.ops.render.render(animation=True)
    return folder


def export_scene(path):
    """Export every object in the scene. Format comes from the extension: .glb .gltf .obj .fbx .stl .ply .usdc .blend."""
    ext = os.path.splitext(path)[1].lower()
    path = os.path.abspath(path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if bpy.context.object and bpy.context.object.mode != "OBJECT":
        bpy.ops.object.mode_set(mode="OBJECT")
    bpy.ops.object.select_all(action="SELECT")
    # Modifier flags are set explicitly: glTF defaults to NOT applying modifiers.
    if ext in (".glb", ".gltf"):
        export_format = "GLB" if ext == ".glb" else "GLTF_SEPARATE"
        bpy.ops.export_scene.gltf(filepath=path, export_format=export_format, use_selection=True,
                                  export_apply=True)
    elif ext == ".obj":
        bpy.ops.wm.obj_export(filepath=path, export_selected_objects=True, apply_modifiers=True)
    elif ext == ".fbx":
        bpy.ops.export_scene.fbx(filepath=path, use_selection=True, use_mesh_modifiers=True)
    elif ext == ".stl":
        bpy.ops.wm.stl_export(filepath=path, export_selected_objects=True, apply_modifiers=True)
    elif ext == ".ply":
        bpy.ops.wm.ply_export(filepath=path, export_selected_objects=True, apply_modifiers=True)
    elif ext in (".usd", ".usdc", ".usda"):
        bpy.ops.wm.usd_export(filepath=path, selected_objects_only=True)
    elif ext == ".blend":
        bpy.ops.wm.save_as_mainfile(filepath=path)
    else:
        raise ValueError(f"No exporter wired for {ext}")
    return path
