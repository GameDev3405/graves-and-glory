"""Animates a unit with its weapon: the Oathbound knight's combo and guard idle, rendered and exported.

  python tools/animate_unit.py                 everything: the video, the GIF, the sheets and the exports
  python tools/animate_unit.py --still 0.35    one frame at a time (seconds into the combo), for checking a pose
  python tools/animate_unit.py --check         the clips' smoothness report only

Writes art/animations/oathbound/:
  oath_combo.mp4        60 fps, 1280x720: the combo twice at full speed, once at 35% speed, then the guard loop
  oath_combo.gif        the combo and a breath of the guard at full speed, 30 fps
  oath_combo_beats.png  the combo's beats, two views each (the contact sheet Kort reviews)
  oath_clips.java       the two clips as TroopClips code (the Clip builder BipedClips uses), ready to paste
  oath_clips.json       the same keys as data
  oath_knight.bbmodel   the knight, the Oathblade and the Sunward on the rig, with both clips as Blockbench animations

The preview is rendered with the armour renderer (tools/player_armor/engine.py), the bones posed by tools/unit_anim/rig.py,
which applies the clips exactly as the game's AnimationHelper does (Minecraft's Catmull-Rom between keys).
"""
import argparse
import base64
import io
import json
import math
import pathlib
import subprocess
import sys
import tempfile
import uuid as uuidlib

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from build_player_armor import BG, BODY, BODY_PAL, font                     # noqa: E402
from player_armor.engine import Canvas, FACES, SHADE, render                 # noqa: E402
from player_armor import oathbound as ARMOUR                                 # noqa: E402
from player_weapons import oathbound as WEAPONS                              # noqa: E402
from unit_anim import rig                                                    # noqa: E402
from unit_anim import oathbound_clips as CL                                  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / 'art' / 'animations' / 'oathbound'

# where the held items sit on their hand bones (Blockbench space, before the hand turns)
SWORD_GRIP = (6.0, 12.5, -1.0)      # the fist at the bottom of the right arm, the grip running fore and aft
SHIELD_GRIP = (-6.0, 13.0, -1.0)    # the left fist, the shield's grip upright along the forearm


def _m(R=None, t=(0, 0, 0)):
    M = np.eye(4)
    if R is not None:
        M[:3, :3] = R
    M[:3, 3] = t
    return M


SWORD_LOCAL = _m(t=SWORD_GRIP) @ _m(rig.rx(math.radians(-90)) @ rig.ry(math.radians(90)))   # blade forward, flats to the sides
SHIELD_LOCAL = _m(t=SHIELD_GRIP) @ _m(rig.ry(math.radians(180)))                             # the face toward the enemy


def _tex_faces(boxes, painted, pos, tex, gl, shade_by_face=True):
    out = []
    for b in boxes:
        out.append((b, {f: (tex, gl, pos[(b.name, f)], SHADE[f]) for f in FACES if f not in b.hide}))
    return out


def _bone_of(box):
    n = box.name
    if n.startswith('cape') and n != 'cape_bar':
        return 'cape'
    if n.startswith('plume') and n != 'plume_socket':
        return 'plume'
    return box.bone


class Unit:
    """The knight's parts with their textures and bones, ready to pose."""

    def __init__(self, rarity='common', team='crimson'):
        self.rarity, self.team = rarity, team
        tier = 'legendary' if rarity == 'legendary' else 'base'
        aset = ARMOUR.build()
        img, glow = aset.texture(rarity, team)
        W, H, pos = aset.pack()
        tex, gl = np.array(img), np.array(glow)[..., 3] > 0
        self.parts = []          # (box, faces, bone, local matrix)
        for b, faces in _tex_faces(aset.boxes_for(tier), None, pos, tex, gl):
            self.parts.append((b, faces, _bone_of(b), None))
        for b in BODY:
            faces = {}
            for f in FACES:
                w, h = b.face_size(f)
                c = Canvas(w, h, f, b, 'base')
                for layer in b.paint:
                    layer(c)
                t = np.zeros((h, w, 4), np.uint8)
                for y in range(h):
                    for x in range(w):
                        m = c.mat[y, x]
                        if m is not None:
                            t[y, x] = (*[int(v) for v in BODY_PAL.tones[m][int(c.tone[y, x])]], 255)
                faces[f] = (t, np.zeros((h, w), bool), (0, 0, w, h), SHADE[f])
            self.parts.append((b, faces, b.bone, None))
        for wp, bone, local in ((self._weapon('oathblade'), 'right_hand', SWORD_LOCAL),
                                (self._weapon('sunward'), 'left_hand', SHIELD_LOCAL)):
            img, glow, _ = wp.texture(rarity)
            Wt, Ht, wpos = wp.pack()
            wt, wg = np.array(img), np.array(glow)[..., 3] > 0
            for b in wp.boxes(tier, 'rest'):
                self.parts.append((b, {f: (wt, wg, wpos[(b.name, f)], SHADE[f]) for f in FACES if f not in b.hide},
                                   bone, local))
        self.weapons = {w.id: w for w in WEAPONS.weapons()}

    @staticmethod
    def _weapon(wid):
        return {w.id: w for w in WEAPONS.weapons()}[wid]

    def drawables(self, pose):
        out = []
        for b, faces, bone, local in self.parts:
            M = pose[bone] if local is None else pose[bone] @ local
            out.append((b, faces, M))
        return out


# ---- frames ------------------------------------------------------------------------------------

def frame(unit, clip, t, yaw, pitch, scale, size, ss=2, world=None):
    """One view of the unit at time t: supersampled ss times and filtered down, with a contact shadow on the floor."""
    rot, pos = clip.at(t)
    pose = (world or rig.world)(rot, pos)
    W, H = size
    anchor = (W * ss / 2, H * ss - 46 * ss)
    im = render(unit.drawables(pose), yaw, pitch, scale * ss, size=(W * ss, H * ss), anchor=anchor)
    # the floor: a soft shadow under the feet, where the root stands
    root = pose['root'][:3, 3]
    shadow = Image.new('L', (W * ss, H * ss), 0)
    d = ImageDraw.Draw(shadow)
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    sx = anchor[0] + (-(cy * root[0]) - (sy * root[2])) * scale * ss
    syy = anchor[1] + (math.sin(math.radians(pitch)) * (-sy * root[0] + cy * root[2])) * scale * ss
    rw, rh = 9 * scale * ss, max(4, 9 * scale * ss * max(0.12, math.sin(math.radians(pitch + 6))))
    d.ellipse((sx - rw, syy - rh, sx + rw, syy + rh), fill=110)
    shadow = shadow.filter(ImageFilter.GaussianBlur(5 * ss))
    base = Image.new('RGBA', (W * ss, H * ss), BG)
    dark = Image.new('RGBA', (W * ss, H * ss), (14, 15, 19, 255))
    base = Image.composite(dark, base, shadow)
    base.alpha_composite(im)
    return base.resize((W, H), Image.LANCZOS)


VIEWS = ((-32, 8, 'three-quarter, shield side'), (90, 0, 'profile, sword side'))


def composite(unit, clip, t, label, speed, beats, scale=8.5):
    vw, vh = 640, 620
    out = Image.new('RGBA', (1280, 720), BG)
    for i, (yaw, pitch, name) in enumerate(VIEWS):
        out.alpha_composite(frame(unit, clip, t, yaw, pitch, scale, (vw, vh)), (i * vw, 46))
    d = ImageDraw.Draw(out)
    d.text((24, 14), 'Oathbound knight  ·  Oathblade and Sunward', font=font(20, True), fill=(226, 222, 210))
    d.text((24, 40), '%s  ·  %s' % (clip.name, label), font=font(14), fill=(170, 174, 184))
    d.text((1280 - 24 - d.textlength(speed, font=font(14, True)), 18), speed, font=font(14, True),
           fill=(232, 137, 42) if speed != '1.0x' else (150, 154, 164))
    for i, (_, _, name) in enumerate(VIEWS):
        d.text((i * 640 + 24, 700 - 22), name, font=font(12), fill=(130, 134, 144))
    # the timeline: the clip's beats as ticks, the playhead as a bar
    x0, x1, y = 24, 1256, 708
    d.line((x0, y, x1, y), fill=(60, 64, 74), width=2)
    for bt, _ in beats:
        bx = x0 + (x1 - x0) * bt / clip.length
        d.line((bx, y - 4, bx, y + 4), fill=(90, 94, 104), width=1)
    px = x0 + (x1 - x0) * ((t % clip.length) / clip.length)
    d.rectangle((px - 2, y - 6, px + 2, y + 6), fill=(232, 137, 42))
    return out


def beat_at(beats, t):
    label = beats[0][1]
    for bt, name in beats:
        if bt <= t + 1e-6:
            label = name
    return label


# ---- smoothness ---------------------------------------------------------------------------------

def smoothness(clip, fps=60):
    """The largest change per frame and per frame squared of every keyed angle (degrees), the jerk a viewer sees as a
    pop. Returns {bone: (max step, max change of step)}."""
    n = int(round(clip.length * fps))
    out = {}
    for b in clip.rot:
        vals = [np.array(rig.sample(clip.rot[b], i / fps)) for i in range(n + 1)]
        steps = [np.abs(vals[i + 1] - vals[i]).max() for i in range(n)]
        acc = [abs(steps[i + 1] - steps[i]) for i in range(n - 1)]
        out[b] = (round(max(steps), 1), round(max(acc), 1))
    return out


# ---- exports ------------------------------------------------------------------------------------

def java(clips):
    lines = ['// The Oathbound knight\'s clips, written by tools/animate_unit.py in the TroopClips builder BipedClips uses.',
             '// Paste into a rig\'s clip set when Kort approves wiring them (they are not one of the 18 standard clips: they',
             '// use the custom-clip constructor, cubic like the loops).', '']
    for c in clips:
        kind = 'TroopClips.Kind.LOOP' if c.loop else 'TroopClips.Kind.ONCE'
        lines.append('new Clip("%s", %s, %sf, false)' % (c.name, kind, ('%g' % c.length)))
        for b in rig.GAME_BONES:
            for chan, keys in (('rot', c.rot.get(b)), ('pos', c.pos.get(b))):
                if not keys:
                    continue
                nums = ', '.join('%gf,%g,%g,%g' % (round(k[0], 3), round(k[1], 2), round(k[2], 2), round(k[3], 2))
                                 for k in keys)
                lines.append('    .%s("%s", %s)' % (chan, b, nums))
        lines.append('')
    lines.append('// Not game bones (the follow parts the preview simulates): cape and plume. A troop model that gives its')
    lines.append('// cape and plume their own bones can take these keys too:')
    for c in clips:
        for b in ('cape', 'plume'):
            if b in c.rot:
                nums = ', '.join('%gf,%g,%g,%g' % (round(k[0], 3), round(k[1], 2), round(k[2], 2), round(k[3], 2))
                                 for k in c.rot[b])
                lines.append('// %s %s: %s' % (c.name, b, nums))
    return '\n'.join(lines) + '\n'


def data(clips):
    return {'convention': rig.__doc__.strip(), 'bones': {b: {'parent': p, 'pivot_blockbench': list(v)}
                                                         for b, (p, v) in rig.BONES.items()},
            'clips': [{'name': c.name, 'length': c.length, 'loop': c.loop, 'interpolation': 'minecraft catmull-rom',
                       'rotation': {b: [[round(x, 4) for x in k] for k in v] for b, v in c.rot.items()},
                       'position': {b: [[round(x, 4) for x in k] for k in v] for b, v in c.pos.items()},
                       'beats': [[t, n] for t, n in (CL.BEATS if c.name == 'oath_combo' else [])]} for c in clips]}


def _png64(img):
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def bbmodel(unit, clips):
    """The knight on the rig as one Blockbench project: nested bone groups at the rig's pivots, the armour (1 texel a
    px), the sword and the shield (2 texels a px, each its own texture), and both clips as animations. Blockbench keys
    are in its own space: rotation (-pitch, -yaw, roll), position (-x, y, z) of the clip's values."""
    def uid(*a):
        return str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, 'gg-anim/' + '/'.join(map(str, a))))
    aset = ARMOUR.build()
    tier = 'base'
    img, _ = aset.texture('common', 'crimson')
    AW, AH, apos = aset.pack()
    textures = [('armour', img, AW, AH)]
    elements, kids = [], {b: [] for b in rig.BONES}
    for b in aset.boxes_for(tier):
        e = _element(b, apos, 0, uid('a', b.name))
        elements.append(e)
        kids[_bone_of(b)].append(e['uuid'])
    for ti, (wid, bone, local) in enumerate((('oathblade', 'right_hand', SWORD_LOCAL), ('sunward', 'left_hand', SHIELD_LOCAL)),
                                             start=1):
        wp = unit.weapons[wid]
        wimg, _, _ = wp.texture('common')
        WW, WH, wpos = wp.pack()
        textures.append((wid, wimg, WW, WH))
        gid = uid('item', wid)
        cubes = []
        grip = local[:3, 3]
        for b in wp.boxes('base', 'rest'):
            e = _element(b, wpos, ti, uid('w', wid, b.name))
            for k in ('from', 'to', 'origin'):          # the item's grip sits at its group's origin, in the fist
                e[k] = [round(e[k][i] + grip[i], 4) for i in range(3)]
            elements.append(e)
            cubes.append(e['uuid'])
        kids.setdefault('item:' + wid, cubes)
    # each item group carries its grip: an origin at the fist and the rotation that lays the item in it
    groups = []

    def group(name, origin, children, rotation=(0, 0, 0)):
        g = {'name': name, 'origin': [round(v, 4) for v in origin], 'rotation': list(rotation), 'color': 0,
             'uuid': uid('g', name), 'export': True, 'mirror_uv': False, 'isOpen': True, 'locked': False,
             'visibility': True, 'autouv': 0}
        groups.append(g)
        return {'uuid': g['uuid'], 'isOpen': True, 'children': children}

    def tree(b):
        ch = list(kids[b])
        for c, (p, _) in rig.BONES.items():
            if p == b:
                ch.append(tree(c))
        if b == 'right_hand':
            ch.append(group('oathblade', SWORD_GRIP, kids['item:oathblade'], _bb_rotation(SWORD_LOCAL[:3, :3])))
        if b == 'left_hand':
            ch.append(group('sunward', SHIELD_GRIP, kids['item:sunward'], _bb_rotation(SHIELD_LOCAL[:3, :3])))
        return group(b, rig.BONES[b][1], ch)
    outliner = [tree('root')]
    anims = []
    for c in clips:
        animators = {}
        for b in rig.BONES:
            kf = []
            for chan, keys, conv in (('rotation', c.rot.get(b), lambda k: (-k[1], -k[2], k[3])),
                                     ('position', c.pos.get(b), lambda k: (-k[1], k[2], k[3]))):
                for k in keys or []:
                    x, y, z = conv(k)
                    kf.append({'channel': chan, 'data_points': [{'x': round(x, 3), 'y': round(y, 3), 'z': round(z, 3)}],
                               'uuid': uid('k', c.name, b, chan, k[0]), 'time': round(k[0], 4), 'color': -1,
                               'interpolation': 'catmullrom'})
            if kf:
                animators[uid('g', b)] = {'name': b, 'type': 'bone', 'keyframes': kf}
        anims.append({'uuid': uid('anim', c.name), 'name': c.name, 'loop': 'loop' if c.loop else 'once',
                      'override': False, 'length': c.length, 'snapping': 20, 'selected': False, 'anim_time_update': '',
                      'blend_weight': '', 'start_delay': '', 'loop_delay': '', 'animators': animators})
    return {'meta': {'format_version': '4.5', 'model_format': 'free', 'box_uv': False}, 'name': 'oath_knight',
            'model_identifier': 'oath_knight', 'visible_box': [1, 2, 0], 'variable_placeholders': '',
            'variable_placeholder_buttons': [], 'resolution': {'width': AW, 'height': AH},
            'elements': elements, 'groups': groups, 'outliner': outliner,
            'textures': [{'path': '', 'name': n, 'folder': '', 'namespace': '', 'id': str(i), 'particle': i == 0,
                          'render_mode': 'default', 'visible': True, 'mode': 'bitmap', 'saved': True,
                          'uuid': uid('t', n), 'relative_path': '', 'width': w, 'height': h, 'uv_width': w,
                          'uv_height': h, 'source': _png64(im)} for i, (n, im, w, h) in enumerate(textures)],
            'animations': anims}


def _bb_rotation(R):
    """Blockbench's group rotation (x, y, z degrees, applied x then y then z) of a rotation matrix."""
    b = math.asin(max(-1.0, min(1.0, -R[2, 0])))
    return [round(math.degrees(math.atan2(R[2, 1], R[2, 2])), 4), round(math.degrees(b), 4),
            round(math.degrees(math.atan2(R[1, 0], R[0, 0])), 4)]


def _element(b, pos, tex, u):
    faces = {}
    for f in FACES:
        if f in b.hide:
            continue
        x, y, w, h = pos[(b.name, f)]
        faces[f] = {'uv': [x, y, x + w, y + h], 'texture': tex}
    e = {'name': b.name, 'type': 'cube', 'uuid': u, 'box_uv': False, 'rescale': False, 'locked': False,
         'from': [round(c, 4) for c in b.frm], 'to': [round(b.frm[i] + b.size[i], 4) for i in range(3)],
         'autouv': 0, 'color': 0, 'inflate': b.inflate, 'origin': [round(c, 4) for c in b.origin], 'faces': faces}
    if any(b.rot):
        e['rotation'] = list(b.rot)
    return e


# ---- the outputs --------------------------------------------------------------------------------

def encode(frames_dir, pattern, fps, out):
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-framerate', str(fps), '-i', str(frames_dir / pattern),
                    '-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '16', '-preset', 'slow', '-movflags', '+faststart',
                    str(out)], check=True)


def gif(frames, out, fps=30):
    pal = frames[0].convert('RGB').quantize(colors=255, method=Image.Quantize.MEDIANCUT)
    qs = [f.convert('RGB').quantize(palette=pal, dither=Image.Dither.NONE) for f in frames]
    qs[0].save(out, save_all=True, append_images=qs[1:], duration=int(1000 / fps), loop=0, optimize=True)


def beats_sheet(unit, clip, out, scale=5):
    picks = [b for b in CL.BEATS if b[1] not in ('wind-up hold', 'bash hold', 'guard')][:12]
    picks = [(0.0, 'guard')] + picks
    cw, ch = 300, 330
    cols = 7
    rows = math.ceil(len(picks) / cols) * 2
    im = Image.new('RGBA', (cols * cw + 20, 70 + rows * ch), BG)
    d = ImageDraw.Draw(im)
    d.text((20, 16), 'oath_combo  ·  the beats, shield side above, sword side below', font=font(20, True),
           fill=(226, 222, 210))
    for i, (t, name) in enumerate(picks):
        r, cidx = divmod(i, cols)
        for v, (yaw, pitch, _) in enumerate(VIEWS):
            f = frame(unit, clip, t, yaw, pitch, scale, (cw, ch))
            im.alpha_composite(f, (10 + cidx * cw, 60 + (r * 2 + v) * ch))
        d.text((24 + cidx * cw, 60 + r * 2 * ch + 6), '%.2f s  %s' % (t, name), font=font(13, True), fill=(214, 216, 222))
    im.convert('RGB').save(out)


_W = {}


def _init(rarity, tmp):
    _W['unit'] = Unit(rarity)
    _W['clips'] = {c.name: c for c in (CL.combo(), CL.guard())}
    _W['tmp'] = pathlib.Path(tmp)


def _job(job):
    k, name, t, speed, beats = job
    clip = _W['clips'][name]
    label = beat_at(CL.BEATS, t) if beats else 'guard (idle loop)'
    img = composite(_W['unit'], clip, t, label, speed, CL.BEATS if beats else [(0, '')])
    img.convert('RGB').save(_W['tmp'] / ('f%05d.png' % k))
    return k


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('--still', type=float)
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--rarity', default='common')
    a = ap.parse_args(argv)
    combo, guard = CL.combo(), CL.guard()
    if a.check:
        for c in (combo, guard):
            print(c.name, smoothness(c))
        return 0
    unit = Unit(a.rarity)
    OUT.mkdir(parents=True, exist_ok=True)
    if a.still is not None:
        img = composite(unit, combo, a.still, beat_at(CL.BEATS, a.still), '1.0x', CL.BEATS)
        p = OUT / ('still_%.2f.png' % a.still)
        img.convert('RGB').save(p)
        print(p)
        return 0
    (OUT / 'oath_clips.java').write_text(java([combo, guard]), encoding='utf-8')
    (OUT / 'oath_clips.json').write_text(json.dumps(data([combo, guard]), indent=1) + '\n', encoding='utf-8')
    (OUT / 'oath_knight.bbmodel').write_text(json.dumps(bbmodel(unit, [combo, guard]), indent=1) + '\n', encoding='utf-8')
    beats_sheet(unit, combo, OUT / 'oath_combo_beats.png')
    # the video: guard breath -> combo -> combo -> slow combo -> guard
    plan = []
    fps = 60
    for i in range(int(guard.length * fps * 0.5)):
        plan.append((guard, i / fps, '1.0x', []))
    for rep in range(2):
        for i in range(int(round(combo.length * fps)) + 1):
            plan.append((combo, i / fps, '1.0x', CL.BEATS))
        for i in range(int(0.4 * fps)):
            plan.append((guard, i / fps, '1.0x', []))
    slow = 0.35
    for i in range(int(round(combo.length * fps / slow)) + 1):
        plan.append((combo, i * slow / fps, '0.35x  slow motion', CL.BEATS))
    for i in range(int(guard.length * fps)):
        plan.append((guard, i / fps, '1.0x', []))
    import multiprocessing as mp
    jobs = []
    for k, (clip, t, speed, beats) in enumerate(plan):
        jobs.append((k, clip.name, t, speed, bool(beats)))
    with tempfile.TemporaryDirectory() as tmp:
        tmp = pathlib.Path(tmp)
        with mp.get_context('fork').Pool(initializer=_init, initargs=(a.rarity, str(tmp))) as pool:
            for done, k in enumerate(pool.imap_unordered(_job, jobs, chunksize=4)):
                if done % 60 == 0:
                    print('frame %d of %d' % (done, len(jobs)), flush=True)
        encode(tmp, 'f%05d.png', fps, OUT / 'oath_combo.mp4')
        gif_frames = [Image.open(tmp / ('f%05d.png' % k)).resize((640, 360), Image.LANCZOS)
                      for k, (clip, t, speed, beats) in enumerate(plan) if speed == '1.0x' and k % 2 == 0][:150]
    gif(gif_frames, OUT / 'oath_combo.gif')
    print('wrote', OUT)
    return 0


if __name__ == '__main__':
    sys.exit(main())
