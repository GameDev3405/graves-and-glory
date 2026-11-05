"""Renders and exports the Oathbound knight's combat pack: Fresh Animations-style clips on the articulated rig.

  python tools/animate_combat.py                  everything: the reel, the GIF, the beat sheets and the exports
  python tools/animate_combat.py --still combo 0.4   one frame of a clip, both views
  python tools/animate_combat.py --check          the clips' smoothness, foot-planting and clipping report only
  python tools/animate_combat.py --sheet combo 0.32 0.4 --out DIR   three large views per time, with clipping volumes
  python tools/animate_combat.py --strip combo 0.26 0.5 --out DIR   consecutive 60 fps frames of a strike, tiled

Writes art/animations/oathbound_combat/:
  combat_reel.mp4          60 fps, 1280x720: every clip at full speed, the strikes again in slow motion
  combat_reel.gif          the clips at full speed, 30 fps, 640x360
  <clip>_beats.png         each clip's beats from both views (the sheets Kort reviews)
  combat_clips.java        the clips in the TroopClips builder (they need the articulated model's bones)
  combat_clips.json        the same keys as data, with the rig
  combat_knight.bbmodel    the knight cut at the joints on the articulated rig, with every clip as an animation

See tools/unit_anim/combat_rig.py for the rig and tools/unit_anim/combat_clips.py for the clips.
"""
import argparse
import json
import math
import pathlib
import subprocess
import sys
import tempfile
import uuid as uuidlib

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import animate_unit as AU                                                   # noqa: E402
from build_player_armor import BG, font                                    # noqa: E402
from player_armor.engine import FACES                                      # noqa: E402
from unit_anim import clearance as CL                                      # noqa: E402
from unit_anim import combat_clips as CC                                   # noqa: E402
from unit_anim import combat_rig as CR                                     # noqa: E402
from unit_anim import rig                                                  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / 'art' / 'animations' / 'oathbound_combat'


class CombatUnit(AU.Unit):
    """The knight, sword and shield with the armour and body cut at the waist, elbows, knees and ankles."""

    def __init__(self, rarity='common', team='crimson'):
        super().__init__(rarity, team)
        self.parts = CR.split_parts(self.parts)


def frame(unit, clip, t, yaw, pitch, scale, size, ss=2):
    return AU.frame(unit, clip, t, yaw, pitch, scale, size, ss=ss, world=CR.world)


VIEWS = AU.VIEWS


# ---- checks -------------------------------------------------------------------------------

def smoothness(clip, fps=60):
    return AU.smoothness(clip, fps)


def ground_speed(clip):
    return CC.ADVANCE_SPEED if clip.name == 'combat_advance' else 0.0


def feet_report(clip, fps=60):
    """Sampled as the game would draw it (60 fps between the baked keys): (deepest point of a sole below the floor,
    the largest gap under a foot meant to be planted, the fastest a planted foot slides against the floor, px/s)."""
    n = int(round(clip.length * fps))
    sink = gap = slide = 0.0
    prev = {}
    v = ground_speed(clip)
    for i in range(n + 1):
        t = i / fps
        rot, pos = clip.at(t)
        pose = CR.world(rot, pos)
        for side in ('right', 'left'):
            place = CR.pchip(clip.track[side[0] + '_place'], t)
            pts = CR.foot_points(pose, side)
            low = min(p[1] for p in pts)
            sink = max(sink, -low)
            planted = place[2] < 1e-6 and abs(place[4]) < 1e-6
            if planted:
                gap = max(gap, low)
                c = np.mean(pts, axis=0)
                if side in prev and prev[side][0]:
                    d = (c - prev[side][1]) * fps
                    slide = max(slide, float(np.hypot(d[0], d[2] - v)))
            prev[side] = (planted, np.mean(pts, axis=0))
    return round(sink, 3), round(gap, 3), round(slide, 2)


# ---- frames -------------------------------------------------------------------------------

def frame_floor(unit, clip, t, yaw, pitch, scale, size, ss=2):
    """A frame with floor studs that slide back at the clip's ground speed, so an advance reads as moving."""
    img = frame(unit, clip, t, yaw, pitch, scale, size, ss)
    v = ground_speed(clip)
    if not v:
        return img
    W, H = size
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    fwd = np.array([-sy, 0, cy])
    right = np.array([-cy, 0, -sy])
    up2 = np.array([0, 1, 0]) * cp + fwd * sp
    ax, ay = W / 2, H - 46
    floor = Image.new('RGBA', size, (0, 0, 0, 0))
    d = ImageDraw.Draw(floor)
    off = (v * t) % 8
    for gx in range(-24, 25, 8):
        for gz in range(-40, 41, 8):
            q = np.array([gx, 0, gz + off])
            if abs(gz + off) > 36:
                continue
            x, y = ax + np.dot(q, right) * scale, ay - np.dot(q, up2) * scale
            fade = max(0.0, 1 - abs(gz + off) / 36)
            d.rectangle((x - 2, y - 1, x + 2, y + 1), fill=(64, 68, 80, int(200 * fade)))
    out = Image.new('RGBA', size, BG)
    out.alpha_composite(floor)
    # the unit was drawn over its own background: put the studs under it by keying the background colour out
    a = np.array(img)
    bg = np.all(np.abs(a[..., :3].astype(int) - np.array(BG[:3])) <= 1, axis=-1)
    a[..., 3] = np.where(bg, 0, 255)
    out.alpha_composite(Image.fromarray(a, 'RGBA'))
    return out


TITLES = {'combat_idle': 'idle in guard', 'combat_advance': 'advance in guard', 'combat_combo': 'three-hit combo',
          'combat_block': 'shield block', 'combat_hit': 'hit reaction'}


def composite(unit, clip, t, speed, scale=8.5):
    vw, vh = 640, 620
    out = Image.new('RGBA', (1280, 720), BG)
    for i, (yaw, pitch, name) in enumerate(VIEWS):
        out.alpha_composite(frame_floor(unit, clip, t, yaw, pitch, scale, (vw, vh)), (i * vw, 46))
    d = ImageDraw.Draw(out)
    d.text((24, 14), 'Oathbound knight  ·  combat pack', font=font(20, True), fill=(226, 222, 210))
    label = AU.beat_at(clip.beats, t % clip.length if clip.loop else t)
    d.text((24, 40), '%s  ·  %s  ·  %s' % (clip.name, TITLES[clip.name], label), font=font(14), fill=(170, 174, 184))
    d.text((1280 - 24 - d.textlength(speed, font=font(14, True)), 18), speed, font=font(14, True),
           fill=(232, 137, 42) if speed != '1.0x' else (150, 154, 164))
    for i, (_, _, name) in enumerate(VIEWS):
        d.text((i * 640 + 24, 700 - 22), name, font=font(12), fill=(130, 134, 144))
    x0, x1, y = 24, 1256, 708
    d.line((x0, y, x1, y), fill=(60, 64, 74), width=2)
    for bt, _ in clip.beats:
        bx = x0 + (x1 - x0) * bt / clip.length
        d.line((bx, y - 4, bx, y + 4), fill=(90, 94, 104), width=1)
    px = x0 + (x1 - x0) * ((t % clip.length) / clip.length if clip.loop else min(1.0, t / clip.length))
    d.rectangle((px - 2, y - 6, px + 2, y + 6), fill=(232, 137, 42))
    return out


def beats_sheet(unit, clip, out, scale=5):
    picks = []
    for t, name in clip.beats:
        if clip.loop and t >= clip.length - 1e-6:
            continue
        if picks and abs(picks[-1][0] - t) < 1e-6:
            continue
        picks.append((t, name))
    if len(picks) > 14:                       # the idle: every other breath
        picks = picks[::2]
    cw, ch, cols = 260, 300, 7
    rows = math.ceil(len(picks) / cols) * 2
    im = Image.new('RGBA', (cols * cw + 20, 60 + rows * ch), BG)
    d = ImageDraw.Draw(im)
    d.text((20, 16), '%s  ·  %s  ·  shield side above, sword side below' % (clip.name, TITLES[clip.name]),
           font=font(20, True), fill=(226, 222, 210))
    for i, (t, name) in enumerate(picks):
        r, c = divmod(i, cols)
        for v, (yaw, pitch, _) in enumerate(VIEWS):
            im.alpha_composite(frame_floor(unit, clip, t, yaw, pitch, scale, (cw, ch)), (10 + c * cw, 50 + (r * 2 + v) * ch))
        d.text((22 + c * cw, 50 + r * 2 * ch + 6), '%.2f s  %s' % (t, name), font=font(12, True), fill=(214, 216, 222))
    im.convert('RGB').save(out)


# ---- exports ------------------------------------------------------------------------------

def _keys(keys):
    return ', '.join('%gf,%g,%g,%g' % (round(k[0], 3), round(k[1], 2), round(k[2], 2), round(k[3], 2)) for k in keys)


def java(clips):
    lines = ['// The Oathbound knight\'s combat pack, written by tools/animate_combat.py in the TroopClips builder BipedClips',
             '// uses. These clips need the articulated troop model: the bones chest, right_forearm, left_forearm, right_shin,',
             '// left_shin, right_foot and left_foot (pivots in combat_clips.json) on top of the standard ones, and the armour',
             '// cut at those joints. Not wired: gameplay-visible, it waits on Kort\'s call. Custom clips, cubic like the loops.',
             '']
    for c in clips:
        kind = 'TroopClips.Kind.LOOP' if c.loop else 'TroopClips.Kind.ONCE'
        lines.append('new Clip("%s", %s, %sf, false)' % (c.name, kind, '%g' % c.length))
        for b in CR.BONES:
            if b in ('cape', 'plume'):
                continue
            for chan, keys in (('rot', c.rot.get(b)), ('pos', c.pos.get(b))):
                if keys:
                    lines.append('    .%s("%s", %s)' % (chan, b, _keys(keys)))
        lines.append('')
    lines.append('// The follow parts the preview simulates (a model that gives the cape and the plume bones can take these):')
    for c in clips:
        for b in ('cape', 'plume'):
            lines.append('// %s %s: %s' % (c.name, b, _keys(c.rot[b])))
    return '\n'.join(lines) + '\n'


def data(clips):
    r4 = lambda keys: [[round(x, 4) for x in k] for k in keys]
    return {'convention': CR.__doc__.strip() + '\n\n' + rig.__doc__.strip(),
            'bones': {b: {'parent': p, 'pivot_blockbench': list(v), 'new': b in CR.NEW_BONES} for b, (p, v) in CR.BONES.items()},
            'clips': [{'name': c.name, 'length': c.length, 'loop': c.loop, 'interpolation': 'minecraft catmull-rom',
                       'ground_speed_px_per_s': ground_speed(c),
                       'rotation': {b: r4(v) for b, v in c.rot.items()},
                       'position': {b: r4(v) for b, v in c.pos.items()},
                       'beats': [[t, n] for t, n in c.beats],
                       'feet': {'right': r4(c.track['r_place']), 'left': r4(c.track['l_place'])}} for c in clips]}


def bbmodel(unit, clips):
    """The knight cut at the joints on the articulated rig: nested groups at every pivot, the armour, the sword and the
    shield (each item in a group that turns it into the fist), and every clip as an animation in Blockbench's space
    (rotation -pitch, -yaw, roll; position -x, y, z)."""
    def uid(*a):
        return str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, 'gg-combat/' + '/'.join(map(str, a))))
    texs, tex_index = [], {}

    def tex_id(arr, name):
        k = id(arr)
        if k not in tex_index:
            tex_index[k] = len(texs)
            texs.append((name, Image.fromarray(arr.astype(np.uint8), 'RGBA')))
        return tex_index[k]
    elements, kids, items = [], {b: [] for b in CR.BONES}, {}
    for box, faces, bone, local in unit.parts:
        if box.name.startswith('p_'):
            continue                                  # the placeholder body stays out, as in the armour files
        if local is None:
            name = 'armour'
        else:
            name = 'oathblade' if bone == 'right_hand' else 'sunward'
        grip = np.zeros(3) if local is None else local[:3, 3]
        fe = {}
        for f in FACES:
            if f in faces and f not in box.hide:
                tex, gl, (x, y, w, h), sh = faces[f]
                fe[f] = {'uv': [x, y, x + w, y + h], 'texture': tex_id(tex, name)}
        e = {'name': box.name, 'type': 'cube', 'uuid': uid('e', name, box.name), 'box_uv': False, 'rescale': False,
             'locked': False, 'from': [round(box.frm[i] + grip[i], 4) for i in range(3)],
             'to': [round(box.frm[i] + box.size[i] + grip[i], 4) for i in range(3)], 'autouv': 0, 'color': 0,
             'inflate': round(box.inflate, 4), 'origin': [round(box.origin[i] + grip[i], 4) for i in range(3)],
             'faces': fe}
        if any(box.rot):
            e['rotation'] = list(box.rot)
        elements.append(e)
        if local is None:
            kids[bone].append(e['uuid'])
        else:
            items.setdefault((bone, name), (local, []))[1].append(e['uuid'])
    groups = []

    def group(name, origin, children, rotation=(0, 0, 0)):
        g = {'name': name, 'origin': [round(float(v), 4) for v in origin], 'rotation': list(rotation), 'color': 0,
             'uuid': uid('g', name), 'export': True, 'mirror_uv': False, 'isOpen': True, 'locked': False,
             'visibility': True, 'autouv': 0}
        groups.append(g)
        return {'uuid': g['uuid'], 'isOpen': True, 'children': children}

    def tree(b):
        ch = list(kids[b])
        for c, (p, _) in CR.BONES.items():
            if p == b:
                ch.append(tree(c))
        for (bone, name), (local, cubes) in items.items():
            if bone == b:
                ch.append(group(name, local[:3, 3], cubes, AU._bb_rotation(local[:3, :3])))
        return group(b, CR.BONES[b][1], ch)
    outliner = [tree('root')]
    anims = []
    for c in clips:
        animators = {}
        for b in CR.BONES:
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
                      'override': False, 'length': c.length, 'snapping': 40, 'selected': False, 'anim_time_update': '',
                      'blend_weight': '', 'start_delay': '', 'loop_delay': '', 'animators': animators})
    W, H = texs[0][1].size
    return {'meta': {'format_version': '4.5', 'model_format': 'free', 'box_uv': False}, 'name': 'combat_knight',
            'model_identifier': 'combat_knight', 'visible_box': [1, 2, 0], 'variable_placeholders': '',
            'variable_placeholder_buttons': [], 'resolution': {'width': W, 'height': H},
            'elements': elements, 'groups': groups, 'outliner': outliner,
            'textures': [{'path': '', 'name': n, 'folder': '', 'namespace': '', 'id': str(i), 'particle': i == 0,
                          'render_mode': 'default', 'visible': True, 'mode': 'bitmap', 'saved': True,
                          'uuid': uid('t', n), 'relative_path': '', 'width': im.size[0], 'height': im.size[1],
                          'uv_width': im.size[0], 'uv_height': im.size[1], 'source': AU._png64(im)}
                         for i, (n, im) in enumerate(texs)],
            'animations': anims}


# ---- the reel -----------------------------------------------------------------------------

_W = {}


def _init(rarity):
    _W['unit'] = CombatUnit(rarity)
    _W['clips'] = {c.name: c for c in (f() for f in CC.CLIPS)}


def _job(job):
    k, name, t, speed, tmp = job
    img = composite(_W['unit'], _W['clips'][name], t, speed)
    img.convert('RGB').save(pathlib.Path(tmp) / ('f%05d.png' % k))
    return k


def reel_plan(clips, fps=60):
    """idle; the advance twice; each strike clip at full speed then slowed; the idle again."""
    c = {x.name: x for x in clips}
    plan = []

    def run(clip, speed=1.0, times=1, label=None):
        n = int(round(clip.length * fps / speed))
        for _ in range(times):
            for i in range(n + (0 if clip.loop else 1)):
                plan.append((clip.name, i * speed / fps, label or ('1.0x' if speed == 1 else '%gx  slow motion' % speed)))
    run(c['combat_idle'])
    run(c['combat_advance'], times=2)
    for name, slow in (('combat_combo', 0.35), ('combat_block', 0.5), ('combat_hit', 0.5)):
        run(c[name])
        for i in range(int(0.3 * fps)):
            plan.append(('combat_idle', i / fps, '1.0x'))
        run(c[name], slow)
        for i in range(int(0.3 * fps)):
            plan.append(('combat_idle', i / fps, '1.0x'))
    run(c['combat_idle'])
    return plan


# ---- review sheets (for blocking and checking poses) ------------------------------------------

REVIEW_VIEWS = ((-32, 8), (90, 0), (0, 4))      # three-quarter shield side, profile sword side, front


def review_sheet(unit, items, out, scale=7, cell=(340, 380), cols=2):
    """[(label, clip, t)] -> one sheet, each item in three views at a large scale, with its clipping volume."""
    cl = CL.Clipper()
    cw, ch = cell
    rows = math.ceil(len(items) / cols)
    im = Image.new('RGBA', (cols * 3 * cw, rows * ch), BG)
    d = ImageDraw.Draw(im)
    for k, (label, clip, t) in enumerate(items):
        r, c = divmod(k, cols)
        for i, (yaw, pitch) in enumerate(REVIEW_VIEWS):
            im.alpha_composite(frame(unit, clip, t, yaw, pitch, scale, cell), ((c * 3 + i) * cw, r * ch))
        v = cl.volumes(CR.world(*clip.at(t)))
        d.text((c * 3 * cw + 8, r * ch + 6), '%s   inside the body: sword %.1f, shield %.1f px^3' %
               (label, v['oathblade'], v['sunward']), font=font(13, True), fill=(230, 230, 230))
    im.convert('RGB').save(out)
    return out


def filmstrip(unit, clip, t0, t1, out, fps=60, cols=7):
    """Consecutive 60 fps frames from t0 to t1 in the three-quarter view, tiled: arcs and pops show here, not on a
    sheet of beats."""
    ts = [t0 + i / fps for i in range(int(round((t1 - t0) * fps)) + 1)]
    cw, ch = 220, 250
    rows = math.ceil(len(ts) / cols)
    im = Image.new('RGBA', (cols * cw, rows * ch), BG)
    d = ImageDraw.Draw(im)
    for k, t in enumerate(ts):
        r, c = divmod(k, cols)
        im.alpha_composite(frame(unit, clip, t, -32, 8, 4.5, (cw, ch)), (c * cw, r * ch))
        d.text((c * cw + 6, r * ch + 4), '%.3f' % t, font=font(11), fill=(200, 200, 200))
    im.convert('RGB').save(out)
    return out


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('--still', nargs=2, metavar=('CLIP', 'SECONDS'))
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--sheet', nargs='+', metavar=('CLIP', 'SECONDS'),
                    help='three views of a clip at its beats (or at the given times), large, with clipping volumes')
    ap.add_argument('--strip', nargs=3, metavar=('CLIP', 'FROM', 'TO'), help='consecutive 60 fps frames, tiled')
    ap.add_argument('--out', help='where --sheet and --strip write (default: the system temp folder)')
    ap.add_argument('--rarity', default='common')
    a = ap.parse_args(argv)
    clips = [f() for f in CC.CLIPS]
    by_name = {c.name: c for c in clips}
    pick = lambda n: by_name['combat_' + n.replace('combat_', '')]
    if a.sheet or a.strip:
        unit = CombatUnit(a.rarity)
        name = (a.sheet or a.strip)[0]
        out = pathlib.Path(a.out or tempfile.gettempdir()) / ('%s_%s.png' % ('sheet' if a.sheet else 'strip', name))
        clip = pick(name)
        if a.sheet:
            ts = [float(x) for x in a.sheet[1:]] or [t for t, _ in clip.beats]
            labels = dict(clip.beats)
            print(review_sheet(unit, [('%.2f s  %s' % (t, labels.get(t, '')), clip, t) for t in ts], out))
        else:
            print(filmstrip(unit, clip, float(a.strip[1]), float(a.strip[2]), out))
        return 0
    if a.check:
        for c in clips:
            print(c.name, 'ik %.3f' % c.ik_error, 'feet (sink, gap, slide)', feet_report(c))
            print('   ', smoothness(c))
            print('    items inside the body:', clipping_report(c))
        return 0
    unit = CombatUnit(a.rarity)
    OUT.mkdir(parents=True, exist_ok=True)
    if a.still:
        clip = pick(a.still[0])
        p = OUT / ('still_%s_%.2f.png' % (clip.name, float(a.still[1])))
        composite(unit, clip, float(a.still[1]), '1.0x').convert('RGB').save(p)
        print(p)
        return 0
    (OUT / 'combat_clips.java').write_text(java(clips), encoding='utf-8')
    (OUT / 'combat_clips.json').write_text(json.dumps(data(clips), indent=1) + '\n', encoding='utf-8')
    (OUT / 'combat_knight.bbmodel').write_text(json.dumps(bbmodel(unit, clips), indent=1) + '\n', encoding='utf-8')
    for c in clips:
        beats_sheet(unit, c, OUT / ('%s_beats.png' % c.name.replace('combat_', '')))
    plan = reel_plan(clips)
    import multiprocessing as mp
    with tempfile.TemporaryDirectory() as tmp:
        jobs = [(k, name, t, speed, tmp) for k, (name, t, speed) in enumerate(plan)]
        with mp.get_context('fork').Pool(initializer=_init, initargs=(a.rarity,)) as pool:
            for done, k in enumerate(pool.imap_unordered(_job, jobs, chunksize=4)):
                if done % 120 == 0:
                    print('frame %d of %d' % (done, len(jobs)), flush=True)
        AU.encode(pathlib.Path(tmp), 'f%05d.png', 60, OUT / 'combat_reel.mp4')
        gif_frames = [Image.open(pathlib.Path(tmp) / ('f%05d.png' % k)).resize((640, 360), Image.LANCZOS)
                      for k, (name, t, speed) in enumerate(plan) if speed == '1.0x' and k % 2 == 0]
    AU.gif(gif_frames, OUT / 'combat_reel.gif')
    print('wrote', OUT)
    return 0


if __name__ == '__main__':
    sys.exit(main())


# ---- clipping -------------------------------------------------------------------------------

def clipping_report(clip, fps=60, clipper=None):
    """Per item, over the clip: (most px^3 inside the body on a frame, at what time, the deepest point and where)."""
    cl = clipper or CL.Clipper()
    worst = {}
    for i in range(int(round(clip.length * fps)) + 1):
        for item, (v, d, part) in cl.volumes(CR.world(*clip.at(i / fps)), detail=True).items():
            w = worst.setdefault(item, [0.0, 0.0, ''])
            if v > w[0]:
                w[:] = [round(v, 2), round(i / fps, 2), '%.2f px deep: %s' % (d, part)]
    return worst
