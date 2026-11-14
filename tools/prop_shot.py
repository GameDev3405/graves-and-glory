"""In-game prop shots (docs/props/WORKFLOW.md, step 4): one real client, props placed through Props.place.

    python tools/prop_shot.py <id>[,<id>...] [--compare <id>=<group/name> ...] [--compare-all] [--offline] [--frames] [--turntable] [--motion N[:TICKS[:FOCUS[:VIEW]]]] [--timeout 1500]

Builds the prop pack (tools/build_prop_pack.py) into an isolated run root work/props/shotrun/<stamp>/, enables it as a client
resource pack, runs `gradlew runClient` with the prop-look suite (GRAVES_PHASE1_CHECKS=prop-look, GRAVES_PROPLOOK=<ids>) and copies
each prop's screenshots to work/props/shots/<id>/ for tools/prop_card.py.

--compare <id>=<group/name> (repeatable) also places that structure template (the maps' current plain-block version:
src/main/resources/data/gravesandglory/structures/<group>/<name>.nbt, e.g. camp/tent_gilded, kit/market) 2 blocks east of the
prop and shoots the pair the same four ways: p1-prop-<id>-cmp-<near|far>-<day|night>.png. A missing template is skipped with a message. Exit 0 only when every placement and shot check passed.
A template is looked up in the shipped structures and then in the unshipped test fixtures (test-tools/resources/.../structures/, e.g. compare/trebuchet).

--compare-all pairs every prop id with the maps' plain-block fixture from tools/compare_templates.py (variants go to the base build:
boulder_* to compare/boulder, supply_wagon_* to the nearest wagon, market_stall_* to compare/market_stall, giant bones to compare/giant_rib);
props with no fixture (handcart, brazier_wall, portcullis_raised, ...) are shot alone. An explicit --compare for an id wins.
Regenerate the fixtures with `python tools/compare_templates.py` (needs the maps checkout, read-only).

--turntable replaces the four views with a review turntable (test tooling, for rework advice): 8 daylight angles around the prop from a little above eye
height (p1-prop-<id>-turn-000..315, 000 = seen from the front, 090 = from the east), a flame/glass close-up by day and by night (-close-day, -close-night) and 4 night
angles (-turn-night-000/090/180/270). No Knight and no fence lanterns, so at night only the prop's own light shows. The shots go to work/props/review/shots/<id>/
(work/props/shots/<id>/ is left alone). Framing per prop is TURN_FOCUS below (framed height, orbit distance, close-up point, close-up distance, in blocks).

--motion N[:TICKS[:FOCUS[:VIEW]]] (moving parts, docs/props/MOVING-PARTS.md) also shoots N frames of each prop from one camera, TICKS client
ticks apart (default 4: 5 a second, so a GIF at 200 ms a frame plays in real time), into work/props/frames/<id>/frame-NN.png, and writes
work/props/review/<id>-motion.gif from them. FOCUS (blocks) aims the camera at the mean motion pivot from that distance (a pennant on a tall
tent) instead of framing the whole prop. VIEW is the camera's bearing in degrees from straight in front (+ toward east; default 50,
front-right): 0 looks square at the front, e.g. at the cook spit's roast turning on its rod. Frames never go in the card's shots folder.

Counts as one of the game clients (WORKFLOW.md: at most 4 across the workspace now, 30 minutes each). Refuses to start when a
client from this checkout is already running; it never stops a client it did not start.
"""
import argparse, datetime, os, pathlib, shutil, subprocess, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))


STRUCTURES = ROOT / 'src' / 'main' / 'resources' / 'data' / 'gravesandglory' / 'structures'
# Test-only templates (the compare fixtures): carried by the unshipped local-integration jar, never by the release jar.
FIXTURE_STRUCTURES = ROOT / 'test-tools' / 'resources' / 'data' / 'gravesandglory' / 'structures'


def template_file(tid):
    for root in (STRUCTURES, FIXTURE_STRUCTURES):
        if (root / (tid + '.nbt')).is_file(): return root / (tid + '.nbt')
    return None


def compare_pairs(specs, ids):
    """[(id, template)] from --compare id=group/name; a pair whose template file is missing is dropped with a message."""
    out = []
    for s in specs:
        if '=' not in s: sys.exit('--compare wants <id>=<group/name>, got %r' % s)
        pid, tid = (v.strip() for v in s.split('=', 1))
        tid = tid[:-4] if tid.endswith('.nbt') else tid
        if pid not in ids: sys.exit('--compare %s: %s is not one of the props shot (%s)' % (s, pid, ','.join(ids)))
        if template_file(tid) is None:
            known = sorted({'%s/%s' % (d.name, f.stem) for root in (STRUCTURES, FIXTURE_STRUCTURES) if root.is_dir()
                            for d in root.iterdir() if d.is_dir() and d.name == tid.split('/')[0] for f in d.glob('*.nbt')})
            print('compare %s: no structure template %s (looked in %s and %s); skipping the compare shots. Known: %s'
                  % (pid, tid, STRUCTURES.relative_to(ROOT), FIXTURE_STRUCTURES.relative_to(ROOT), ', '.join(known) or 'none in that group'))
            continue
        out.append((pid, tid))
    return out


# id -> (framed height, orbit distance, close-up x, y, z from the prop's min corner, close-up distance), blocks; unlisted: height = cells, 0.9*h+0.6 away
TURN_FOCUS = {
    'street_lamp': (3.0, 2.6, 0.5, 2.5, 0.5, 1.2),
    'street_lamp_wall': (1.0, 1.35, 0.5, 0.4, 0.3, 0.9),
    'cauldron_tripod': (1.9, 1.8, 0.5, 0.45, 0.5, 1.1),
}


def main(argv=None):
    a = argparse.ArgumentParser(); a.add_argument('ids'); a.add_argument('--offline', action='store_true')
    a.add_argument('--frames', action='store_true', help='also shoot near-day 3, 6 and 9 ticks later (-t3/-t6/-t9) to see animated textures move')
    a.add_argument('--turntable', action='store_true', help='turntable review shots (8 day angles, close-ups day/night, 4 night angles) to work/props/review/shots/<id>/')
    a.add_argument('--timeout', type=int, default=1500)
    a.add_argument('--compare-all', action='store_true',
                   help='pair every id with the plain-block fixture of the maps (tools/compare_templates.py); ids without one are shot alone')
    a.add_argument('--compare', action='append', default=[], metavar='ID=GROUP/NAME',
                   help='shoot the prop beside this structure template (data/gravesandglory/structures/GROUP/NAME.nbt); repeatable')
    a.add_argument('--motion', default='', metavar='N[:TICKS[:FOCUS[:VIEW]]]',
                   help='also shoot N frames TICKS client ticks apart (default 4), FOCUS blocks from the motion pivots (default: whole prop), camera bearing VIEW degrees from the front (default 50), and make work/props/review/<id>-motion.gif')
    o = a.parse_args(argv)
    ids = [i for i in o.ids.split(',') if i]
    specs = list(o.compare)
    if o.compare_all:
        import compare_templates
        paired, alone = compare_templates.pairs_for(ids)
        explicit = {s.split('=', 1)[0].strip() for s in specs}
        specs += ['%s=%s' % pt for pt in paired if pt[0] not in explicit]
        print('compare-all:', ', '.join('%s=%s' % pt for pt in paired) or 'no fixtures', '| shot alone:', ', '.join(alone) or 'none', flush=True)
    cmp = compare_pairs(specs, ids)
    motion = (0, 4, 0, 50)
    if o.motion:
        try: n, t, fo, vw = (o.motion.split(':') + ['', '', ''])[:4]; motion = (int(n), int(t or 4), int(fo or 0), int(vw or 50))
        except ValueError: sys.exit('--motion wants N, N:TICKS, N:TICKS:FOCUS or N:TICKS:FOCUS:VIEW, got %r' % o.motion)
        if not (1 <= motion[0] <= 200 and motion[1] >= 1): sys.exit('--motion: N 1..200, TICKS at least 1')
    import verify   # game_guard, java_home: the same guards as tools/verify.py game
    verify.game_guard()
    jdk = verify.java_home()
    stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
    root = ROOT / 'work' / 'props' / 'shotrun' / stamp
    run = root / 'run'; (run / 'resourcepacks').mkdir(parents=True); (root / 'evidence').mkdir()
    pack = root / 'pack'
    subprocess.check_call([sys.executable, 'tools/build_prop_pack.py', '--out', str(pack)], cwd=ROOT)
    zips = sorted(pack.glob('gg-props-*.zip'))
    if not zips: sys.exit('pack build wrote no zip')
    shutil.copy(zips[-1], run / 'resourcepacks' / 'gg-props.zip')
    (run / 'options.txt').write_text('pauseOnLostFocus:false\nonboardAccessibility:false\nrenderDistance:8\nsimulationDistance:6\n'
                                     'guiScale:3\nresourcePacks:["fabric","gravesandglory:armory","file/gg-props.zip"]\n', encoding='utf8')
    env = dict(os.environ, JAVA_HOME=str(jdk), PATH=str(jdk / 'bin') + os.pathsep + os.environ['PATH'],
               GRAVES_ROADMAP_TEST_ROOT=str(root), GRAVES_PHASE1_CHECKS='prop-look', GRAVES_PROPLOOK=','.join(ids),
               GRAVES_PROPCMP=';'.join('%s=%s' % kv for kv in cmp), GRAVES_PROPFRAMES='1' if o.frames else '', GRAVES_PROPTURN='1' if o.turntable else '',
               GRAVES_PROPFOCUS=';'.join('%s=%s' % (k, ','.join(str(x) for x in v)) for k, v in TURN_FOCUS.items()) if o.turntable else '',
               GRAVES_PROPMOTION='%d,%d,%d,%d' % motion,
               GRAVES_PROPEXAMPLE=str(ROOT / 'docs' / 'props' / 'examples' / 'prop_facings.nbt'))
    gradle = [str(ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew')), '--no-daemon'] + (['--offline'] if o.offline else [])
    log = root / 'game.log'
    print('client running, log', log, flush=True)
    with log.open('w', encoding='utf8') as stream:
        child = subprocess.Popen(gradle + ['runClient', '-PlocalIntegration', '-I', 'test-tools/isolated-checks.gradle'],
                                 cwd=ROOT, env=env, stdout=stream, stderr=subprocess.STDOUT)
        try: code = child.wait(timeout=o.timeout)
        except subprocess.TimeoutExpired:   # only the tree this script started
            subprocess.run(['taskkill', '/PID', str(child.pid), '/T', '/F'], stdout=stream, stderr=subprocess.STDOUT); child.wait(); code = 124
    ev = root / 'evidence' / 'local-checks.txt'
    lines = ev.read_text(encoding='utf8').splitlines() if ev.exists() else []
    for line in lines:
        if 'prop' in line: print(' ', line)
    shots = run / 'screenshots'; copied = 0
    for pid in ids:
        dest = ROOT / 'work' / 'props' / ('review/shots' if o.turntable else 'shots') / pid
        if dest.exists(): shutil.rmtree(dest)
        dest.mkdir(parents=True)
        for f in sorted(shots.glob('p1-prop-%s-*.png' % pid)) if shots.exists() else []:
            shutil.copy(f, dest / f.name); copied += 1
        print('%s: %d shots -> %s' % (pid, len(list(dest.glob('*.png'))), dest))
    if not o.turntable and shots.exists() and (shots / 'p1-prop-lineup.png').exists():
        shutil.copy(shots / 'p1-prop-lineup.png', ROOT / 'work' / 'props' / 'shots' / ('lineup-' + '-'.join(ids)[:60] + '.png'))
    gifs = []
    if motion[0]:
        for pid in ids:
            fdir = ROOT / 'work' / 'props' / 'frames' / pid
            if fdir.exists(): shutil.rmtree(fdir)
            fdir.mkdir(parents=True)
            got = sorted(shots.glob('p1-propframe-%s-*.png' % pid)) if shots.exists() else []
            for f in got: shutil.copy(f, fdir / ('frame-' + f.stem.rsplit('-', 1)[1] + '.png'))
            if got: gifs.append(make_gif(sorted(fdir.glob('frame-*.png')), ROOT / 'work' / 'props' / 'review' / ('%s-motion.gif' % pid), motion[1] * 50))
            print('%s: %d frames -> %s' % (pid, len(got), fdir))
        for g in gifs: print('gif', g)
    ok = code == 0 and lines and not any(l.startswith('fail ') for l in lines) and any('pass p1-prop-look' in l for l in lines) and copied \
        and (not motion[0] or len(gifs) == len(ids))
    print('prop-look', 'PASS' if ok else 'FAIL (exit %s, log %s)' % (code, log))
    return 0 if ok else 1


def make_gif(files, out, ms, width=560):
    """An animated GIF of the frames (cropped to the middle 60% of the width, scaled to `width` px), `ms` per frame, looping."""
    from PIL import Image
    out.parent.mkdir(parents=True, exist_ok=True)
    imgs = []
    for f in files:
        im = Image.open(f).convert('RGB'); w, h = im.size
        im = im.crop((w // 5, 0, w - w // 5, h))
        imgs.append(im.resize((width, round(im.size[1] * width / im.size[0])), Image.LANCZOS))
    pal = imgs[0].quantize(colors=255, method=Image.MEDIANCUT)
    frames = [im.quantize(palette=pal, dither=Image.NONE) for im in imgs]   # one palette: no flicker between frames
    frames[0].save(out, save_all=True, append_images=frames[1:], duration=ms, loop=0, optimize=False)
    return out


if __name__ == '__main__':
    sys.exit(main())
