"""Build one generator's pieces end to end: plan -> flat workshop world -> apply -> one nbt per printed export box,
saved to ../pieces and to the test resources, plus an isometric picture of the whole sampler.
Usage: python mk.py GENERATOR.py TAG WORKDIR [PIECE [GENERATOR ARGS]]   (TAG makes the work files unique; nothing is deleted)
Generators that print no export boxes use the fixed boxes below (the piece corner must never move: the map places by it)."""
import sys, os, subprocess, json, shutil
gen, tag, work = sys.argv[1], sys.argv[2], sys.argv[3]
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..', '..', '..'))
MF = [sys.executable, os.path.join(ROOT, 'tools', 'mapforge')]
plan, world = os.path.join(work, tag + '-plan.json'), os.path.join(work, tag + '-w.json')
FIXED = {'base.py': ('k1_base', 0, 64, 0, 72, 85, 79), 'grange.py': ('k2_grange', -1, 64, -1, 56, 80, 56), 'barn.py': ('k3_barn', -1, 64, -4, 34, 79, 19),
         'windmill.py': ('k4_windmill', -4, 64, -2, 16, 88, 18), 'crane.py': ('k6_crane', 0, 64, -3, 8, 82, 20), 'hold.py': ('s9_hold', -1, 64, 0, 23, 75, 23)}
out = subprocess.run([sys.executable, os.path.join(HERE, gen), plan] + sys.argv[5:], capture_output=True, text=True, cwd=HERE)
if gen in FIXED:
    f = FIXED[gen]
    out.stdout += chr(10) + '%s %s' % (sys.argv[4] if len(sys.argv) > 4 else f[0], ' '.join(str(v) for v in f[1:]))
print(out.stdout[-600:], out.stderr[-2000:])
pad = json.load(open(plan))['locks'][0]
lo, hi = pad['min'], pad['max']
def run(*a):
    r = subprocess.run(MF + list(a), capture_output=True, text=True, cwd=ROOT)
    if r.returncode: print('FAILED', a[0], r.stdout[-1500:], r.stderr[-1500:]); sys.exit(1)
run('flat', '--out', world, '--min', str(lo[0]), str(lo[2]), '--max', str(hi[0]), str(hi[2]), '--y', '64', '--depth', '2', '--block', 'grass_block')
run('apply', plan, '--world', world, '--undo', os.path.join(work, tag + '-undo.json'))
for line in out.stdout.splitlines():
    p = line.split()
    if len(p) == 7 and p[1].lstrip('-').isdigit():
        name, box = p[0], p[1:]
        dst = os.path.join(HERE, '..', 'pieces', name + '.nbt')
        run('export', '--world', world, '--min', *box[:3], '--max', *box[3:], '--out', dst)
        shutil.copy(dst, os.path.join(ROOT, 'test-tools', 'resources', 'data', 'ggmaps', 'structure', 'kf', name + '.nbt'))
        print('piece', name, box)
run('render', world, '--out', os.path.join(work, tag + '-iso.png'), '--view', 'iso', '--min', str(lo[0]), '63', str(lo[2]), '--max', str(hi[0]), '96', str(hi[2]))
print('picture', os.path.join(work, tag + '-iso.png'))
