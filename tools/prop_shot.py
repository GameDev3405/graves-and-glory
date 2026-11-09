"""In-game prop shots (docs/props/WORKFLOW.md, step 4): one real client, props placed through Props.place.

    python tools/prop_shot.py <id>[,<id>...] [--offline] [--timeout 1500]

Builds the prop pack (tools/build_prop_pack.py) into an isolated run root work/props/shotrun/<stamp>/, enables it as a client
resource pack, runs `gradlew runClient` with the prop-look suite (GRAVES_PHASE1_CHECKS=prop-look, GRAVES_PROPLOOK=<ids>) and copies
each prop's screenshots to work/props/shots/<id>/ for tools/prop_card.py. Exit 0 only when every placement and shot check passed.

Counts as one of the game clients (WORKFLOW.md: at most 4 across the workspace now, 30 minutes each). Refuses to start when a
client from this checkout is already running; it never stops a client it did not start.
"""
import argparse, datetime, os, pathlib, shutil, subprocess, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))


def main(argv=None):
    a = argparse.ArgumentParser(); a.add_argument('ids'); a.add_argument('--offline', action='store_true')
    a.add_argument('--timeout', type=int, default=1500)
    o = a.parse_args(argv)
    ids = [i for i in o.ids.split(',') if i]
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
               GRAVES_ROADMAP_TEST_ROOT=str(root), GRAVES_PHASE1_CHECKS='prop-look', GRAVES_PROPLOOK=','.join(ids))
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
        dest = ROOT / 'work' / 'props' / 'shots' / pid
        if dest.exists(): shutil.rmtree(dest)
        dest.mkdir(parents=True)
        for f in sorted(shots.glob('p1-prop-%s-*.png' % pid)) if shots.exists() else []:
            shutil.copy(f, dest / f.name); copied += 1
        print('%s: %d shots -> %s' % (pid, len(list(dest.glob('*.png'))), dest))
    if shots.exists() and (shots / 'p1-prop-lineup.png').exists():
        shutil.copy(shots / 'p1-prop-lineup.png', ROOT / 'work' / 'props' / 'shots' / ('lineup-' + '-'.join(ids)[:60] + '.png'))
    ok = code == 0 and lines and not any(l.startswith('fail ') for l in lines) and any('pass p1-prop-look' in l for l in lines) and copied
    print('prop-look', 'PASS' if ok else 'FAIL (exit %s, log %s)' % (code, log))
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
