"""Build a troop's clips and look at them through the game's engine.

    python tools/troop_anim/run.py <troop> [--sheet clip ...] [--reel] [--out dir]

Writes art/clips/<rig>/<troop>.bbmodel and the troop's troopAttacks rows, runs tools/preflight.py and tools/clips.py, lints the
clips (kit.lint), then for each clip asked runs a scene through MotionPreview and draws a contact sheet (front, three-quarter,
side) on the troop's model."""
import argparse
import importlib
import json
import pathlib
import subprocess
import sys

TOOLS = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
from troop_anim import kit as K  # noqa: E402
import troop_preview as TP  # noqa: E402
import troop_render as R  # noqa: E402

ROOT = TOOLS.parent
HOUSES = {t: 'gilded' for t in ('knight', 'archer', 'sentinel', 'longbow', 'mage', 'gl_marshal')}


def build(troop):
    mod = importlib.import_module('troop_anim.' + HOUSES[troop])
    k, clips = getattr(mod, troop)()
    path = K.write(k, clips)
    for cmd in (['tools/clips.py'], ['tools/preflight.py']):
        r = subprocess.run([sys.executable, *cmd], cwd=ROOT, capture_output=True, text=True)
        if r.returncode:
            raise SystemExit(f'{cmd[0]}: {r.stdout}{r.stderr}')
    return k, clips, path


def scene_for(clip, k):
    """A scene that shows one clip as the game plays it."""
    ev = [{'t': 0, 'do': 'walk', 'speed': 0}]
    if clip.slot:
        ev += [{'t': 0, 'do': 'enemy', 'on': True}, {'t': 1.0, 'do': 'attack', 'clip': clip.name}]
        if clip.base == 'aim':   # a ranged troop shoots from the aim stance (stance_v2 ranged_aim)
            ev += [{'t': 0, 'do': 'stance', 'stance': 'ranged_aim'}]
        return {'length': 1.0 + clip.length + 0.6, 'events': ev}, 1.0
    if clip.name.startswith('idle_alt'):
        return {'length': 1.5 + clip.length + 0.5, 'events': ev + [{'t': 1.5, 'do': 'calm'}]}, 1.5
    if clip.name in ('walk', 'run'):
        return {'length': 4.0, 'events': ev + [{'t': 0, 'do': 'walk', 'speed': 1.4 if clip.name == 'walk' else 4.6}]}, 2.0
    if clip.name.startswith('hit'):
        return {'length': 2.5, 'events': ev + [{'t': 0, 'do': 'enemy', 'on': True}, {'t': 1.0, 'do': 'hit', 'zone': clip.name.split('_')[1] if '_' in clip.name else 'body', 'weight': 1}]}, 1.0
    if clip.name.startswith('block'):
        return {'length': 2.5, 'events': ev + [{'t': 0, 'do': 'enemy', 'on': True}, {'t': 1.0, 'do': 'hit', 'zone': 'body', 'blocked': True}]}, 1.0
    if clip.name.startswith('death'):
        return {'length': 1.0 + clip.length + 1.0, 'events': ev + [{'t': 1.0, 'do': 'die', 'bearing': 0}]}, 1.0
    if clip.mode == 'whole':
        return {'length': 1.0 + clip.length + 0.5, 'events': ev + [{'t': 1.0, 'do': 'scene', 'clip': clip.name}]}, 1.0
    stance = {'guard': [{'t': 0, 'do': 'enemy', 'on': True}], 'aim': [{'t': 0, 'do': 'stance', 'stance': 'ranged_aim'}], 'carry': [{'t': 0, 'do': 'walk', 'speed': 0.0}, {'t': 0, 'do': 'stance', 'stance': 'run'}],   # the march carry (stanceKey: run, no enemy)
             'brace': [{'t': 0, 'do': 'stance', 'stance': 'brace'}]}
    return {'length': clip.length + 1.0, 'events': ev + stance.get(clip.name, [])}, 1.0


def show(k, clip, out, tier=1, cols=8):
    scene, start = scene_for(clip, k)
    scene.update(troop=k.id, fps=60, seed=5)
    res = TP.run(scene)
    troop = R.Troop(k.id, tier)
    beats = sorted({start + t for t, _, _ in K.norm_keys(clip.keys)} | {start + m for m in clip.markers.values()})
    if clip.loop or len(beats) < 3:
        beats = [start + clip.length * i / (cols - 1) for i in range(cols)]
    img = TP.sheet(troop, res['frames'], beats, views=(0, 40, 90), label=f"{k.id} / {clip.name}  ({clip.mode}{', over ' + clip.base if clip.mode in ('offset', 'raw') else ''}{', slot ' + clip.slot if clip.slot else ''})  columns: seconds into the scene")
    path = out / f'{k.id}_{clip.name}.png'
    img.save(path)
    return path, res


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('troop')
    ap.add_argument('--sheet', nargs='*', default=None)
    ap.add_argument('--out', default=str(ROOT / 'work/troop_anim/sheets'))
    ap.add_argument('--reel', action='store_true')
    a = ap.parse_args(argv)
    if a.reel:
        out = pathlib.Path(a.out)
        out.mkdir(parents=True, exist_ok=True)
        print(make_reel(a.troop, out))
        return
    k, clips, path = build(a.troop)
    print('wrote', path.relative_to(ROOT))
    problems = K.lint(k, clips)
    print('\n'.join(problems) if problems else 'lint clean')
    out = pathlib.Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    if a.sheet is not None:
        for c in clips:
            if not a.sheet or c.name in a.sheet:
                p, _ = show(k, c, out)
                print(p)



def reel_scene(k, clips):
    """One continuous scene through a troop's whole set as the game would play it: calm with its idle variants, a walk, a run,
    the guard, every attack, the hits, its specials, then its death. Returns (scene, [(start, label)])."""
    t, ev, marks = 0.0, [{'t': 0, 'do': 'walk', 'speed': 0}], []
    def at(label, dur):
        nonlocal t
        marks.append((t, label))
        t += dur
    at('idle', 2.0)
    for c in clips:
        if c.name.startswith('idle_alt'):
            ev.append({'t': t, 'do': 'calm'})
            at(c.name.replace('_', ' '), c.length + 0.8)
    ev.append({'t': t, 'do': 'walk', 'speed': 1.4}); at('walk', 3.0)
    ev.append({'t': t, 'do': 'walk', 'speed': 4.6}); at('run', 2.6)
    ev.append({'t': t, 'do': 'walk', 'speed': 0}); ev.append({'t': t, 'do': 'enemy', 'on': True}); at('guard', 1.6)
    ranged = any(c.base == 'aim' and c.slot for c in clips)
    if ranged:
        ev.append({'t': t, 'do': 'stance', 'stance': 'ranged_aim'}); at('aim', 1.2)
    for c in clips:
        if c.slot:
            ev.append({'t': t, 'do': 'attack', 'clip': c.name})
            at(c.name.split('_', 1)[1].replace('_', ' ') if '_' in c.name else c.name, c.length + 0.5)
    ev.append({'t': t, 'do': 'stance', 'stance': ''})
    for zone in ('body', 'head', 'legs'):
        ev.append({'t': t, 'do': 'hit', 'zone': zone, 'weight': 1}); at(f'hit {zone}', 1.3)
    ev.append({'t': t, 'do': 'enemy', 'on': False})
    for c in clips:
        if c.mode == 'whole' and not c.name.startswith('death'):
            ev.append({'t': t, 'do': 'scene', 'clip': c.name}); at(c.name.replace('_', ' '), c.length + 0.6)
            ev.append({'t': t - 0.3, 'do': 'scene', 'clip': ''})
    ev.append({'t': t + 0.5, 'do': 'die', 'bearing': 0}); at('death', 4.0)
    return {'length': t, 'events': ev}, marks


def make_reel(troop, out, fps=24, tier=1):
    k, clips, _ = build(troop)
    scene, marks = reel_scene(k, clips)
    scene.update(troop=troop, fps=fps, seed=7)
    res = TP.run(scene)
    for f in res['frames']:
        f['label'] = next((lbl for s, lbl in reversed(marks) if f['t'] >= s - 1e-6), '')
    for f in res['frames']:
        f['t_label'] = f['label']
    path = out / f'{troop}_reel.mp4'
    TP.reel(R.Troop(troop, tier), [dict(f, t=f['t']) for f in res['frames']], path, views=(25, 90), scale=5, size=(300, 260),
            title=lambda f: f"{k.row.get('name', troop)}  {f['label']}", fps=fps)
    return path


if __name__ == '__main__':
    main()
