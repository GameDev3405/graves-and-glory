"""Writes the placeholder camp NPC rig (lobby NPC spec 3.1): a chunky biped with a hammer and an anvil, on the knight's
biped bone names, so the NPC code draws something until the animation-skill session delivers the real blacksmith.

    python tools/camp_npc_placeholder.py

Writes assets/gravesandglory/geo/camp_npc/placeholder.geo.json, animations/camp_npc/placeholder.animation.json and
textures/entity/camp_npc/placeholder.png (flat colours, box UV). Not art: the real rig replaces it by its campNpcs row's rig.
"""
import json, pathlib
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / 'src/main/resources/assets/gravesandglory'
TEX = 128

SKIN, LEATHER, CLOTH, BOOT = (196, 146, 108), (104, 66, 38), (62, 52, 46), (40, 30, 24)
BRASS, WOOD, IRON, ANVIL, BEARD = (176, 124, 52), (120, 80, 42), (128, 128, 134), (66, 66, 72), (64, 42, 26)

# name, parent, pivot, cubes [(origin, size, colour)]; front is -z, the right side -x (the knight rig's axes)
BONES = [
    ('root', None, [0, 0, 0], []),
    ('hips', 'root', [0, 12, 0], [([-5.5, 11, -3], [11, 3, 6], CLOTH)]),
    ('torso', 'hips', [0, 14, 0], [([-7, 14, -4], [14, 10, 8], LEATHER)]),
    ('head', 'torso', [0, 24, 0], [([-4, 24, -4.5], [8, 7, 8], SKIN), ([-3, 21, -5.5], [6, 4, 2], BEARD)]),
    ('right_arm', 'torso', [-9, 23, 0], [([-11, 18, -2.5], [4, 6, 5], SKIN)]),
    ('right_forearm', 'right_arm', [-9, 18, 0], [([-11.5, 12, -3], [5, 6, 6], SKIN)]),
    ('right_hand', 'right_forearm', [-9, 12, 0], [([-11, 9, -2.5], [4, 3, 5], SKIN)]),
    ('hammer', 'right_hand', [-9, 10, 0], [([-9.5, 10, -10], [1, 1, 8], WOOD), ([-11, 8, -13], [4, 4, 3], IRON)]),
    ('left_arm', 'torso', [9, 23, 0], [([7, 18, -2.5], [4, 6, 5], SKIN)]),
    ('left_forearm', 'left_arm', [9, 18, 0], [([6.5, 12, -3], [5, 6, 6], BRASS)]),
    ('left_hand', 'left_forearm', [9, 12, 0], [([7, 9, -2.5], [4, 3, 5], SKIN)]),
    ('right_leg', 'hips', [-3, 12, 0], [([-5, 7, -2.5], [4, 5, 5], CLOTH)]),
    ('right_shin', 'right_leg', [-3, 7, 0], [([-5, 2, -2.5], [4, 5, 5], CLOTH)]),
    ('right_foot', 'right_shin', [-3, 2, 0], [([-5, 0, -4], [4, 2, 6], BOOT)]),
    ('left_leg', 'hips', [3, 12, 0], [([1, 7, -2.5], [4, 5, 5], CLOTH)]),
    ('left_shin', 'left_leg', [3, 7, 0], [([1, 2, -2.5], [4, 5, 5], CLOTH)]),
    ('left_foot', 'left_shin', [3, 2, 0], [([1, 0, -4], [4, 2, 6], BOOT)]),
    ('anvil', 'root', [0, 0, -14], [([-4, 0, -16], [8, 2, 5], ANVIL), ([-2, 2, -15.5], [4, 3, 4], ANVIL), ([-5, 5, -17], [10, 3, 6], ANVIL)]),
]

# idle: two-beat hammer strikes with a hip drop every 3 s (the spec's work loop, roughly); interact: hammer to the shoulder, lean in
ANIMATIONS = {
    'idle': {'loop': True, 'animation_length': 3.0, 'bones': {
        'right_arm': {'rotation': {'0.0': [0, 0, 0], '0.6': [-75, 0, 0], '0.85': [-10, 0, 0], '1.2': [-55, 0, 0], '1.45': [-10, 0, 0], '2.1': [0, 0, 0], '3.0': [0, 0, 0]}},
        'root': {'position': {'0.0': [0, 0, 0], '0.85': [0, -0.6, 0], '1.1': [0, 0, 0], '1.45': [0, -0.6, 0], '1.7': [0, 0, 0], '3.0': [0, 0, 0]}},
        'torso': {'rotation': {'0.0': [0, 0, 0], '0.6': [-4, 0, 0], '0.85': [6, 0, 0], '1.45': [6, 0, 0], '2.1': [0, 0, 0], '3.0': [0, 0, 0]}},
        'head': {'rotation': {'0.0': [0, 0, 0], '1.5': [4, 0, 0], '3.0': [0, 0, 0]}}}},
    'interact': {'loop': False, 'animation_length': 1.5, 'bones': {
        'right_arm': {'rotation': {'0.0': [0, 0, 0], '0.3': [-160, 0, 25], '1.2': [-160, 0, 25], '1.5': [0, 0, 0]}},
        'torso': {'rotation': {'0.0': [0, 0, 0], '0.4': [12, 0, 0], '1.2': [12, 0, 0], '1.5': [0, 0, 0]}},
        'left_arm': {'rotation': {'0.0': [0, 0, 0], '0.4': [-40, 0, 0], '1.2': [-40, 0, 0], '1.5': [0, 0, 0]}}}},
}


def main():
    img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
    px = img.load()
    u = v = row = 0
    bones = []
    for name, parent, pivot, cubes in BONES:
        out = []
        for origin, size, colour in cubes:
            w, h, d = size
            span, tall = 2 * (d + w), d + h
            if u + span > TEX: u, v, row = 0, v + row, 0
            assert v + tall <= TEX, 'placeholder texture full'
            dark = tuple(int(c * .7) for c in colour) + (255,)
            for x in range(span):
                for y in range(tall):
                    edge = x in (0, span - 1) or y in (0, tall - 1) or x == d or y == d
                    px[u + x, v + y] = dark if edge else colour + (255,)
            if name == 'head' and colour == SKIN:  # eyes and nose on the north (front) face
                fx, fy = u + d, v + d
                for ex in (2, 5): px[fx + ex, fy + 2] = (20, 20, 20, 255)
                px[fx + 3, fy + 4] = px[fx + 4, fy + 4] = (150, 100, 70, 255)
            out.append({'origin': origin, 'size': size, 'uv': [u, v]})
            u += span; row = max(row, tall)
        bone = {'name': name, 'pivot': pivot}
        if parent: bone['parent'] = parent
        if out: bone['cubes'] = out
        bones.append(bone)
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{'description': {
        'identifier': 'geometry.gravesandglory.camp_npc_placeholder', 'texture_width': TEX, 'texture_height': TEX,
        'visible_bounds_width': 3, 'visible_bounds_height': 3.5, 'visible_bounds_offset': [0, 1.25, 0]}, 'bones': bones}]}
    for rel, data in (('geo/camp_npc/placeholder.geo.json', geo), ('animations/camp_npc/placeholder.animation.json', {'format_version': '1.8.0', 'animations': ANIMATIONS})):
        path = ASSETS / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, indent=1) + '\n', newline='\n')
    tex = ASSETS / 'textures/entity/camp_npc/placeholder.png'
    tex.parent.mkdir(parents=True, exist_ok=True)
    img.save(tex)
    print('wrote the placeholder rig, clips and texture')


if __name__ == '__main__':
    main()
