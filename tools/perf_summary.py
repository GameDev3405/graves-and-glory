"""Summarise a PerfProbe / PerfClient run directory (plan task 3.6): python tools/perf_summary.py work/perf-out"""
import csv, statistics, sys, pathlib

d = pathlib.Path(sys.argv[1])


def rows(name):
    p = d / name
    return list(csv.DictReader(p.open())) if p.exists() else []


def pct(v, q):
    v = sorted(v)
    return v[max(0, min(len(v) - 1, int(round(q * len(v) + 0.5)) - 1))] if v else float('nan')


print('== server builds (wall s, ticks)')
for r in rows('server-builds.csv'):
    print(r)
print('== server phases (ticks, wall s, mean, p50, p95, p99, max, over50)')
for r in rows('server-battles.csv'):
    if r['phase'] in ('battle', 'camp'):
        print(r['phase'], 'map', r['map'], r['ticks'], r['wallSecs'], r['meanMs'], r['p50Ms'], r['p95Ms'], r['p99Ms'], r['maxMs'], r['over50'])
print('== server window rows during battle: heap MB max, chunks, entities, players')
for m in sorted({r['map'] for r in rows('server-windows.csv') if r['phase'] == 'battle'}):
    w = [r for r in rows('server-windows.csv') if r['phase'] == 'battle' and r['map'] == m]
    print('map', m, 'windows', len(w), 'mean-of-p95', round(statistics.mean(float(r['p95Ms']) for r in w), 2), 'max', max(float(r['maxMs']) for r in w),
          'heapMax', max(int(r['heapMb']) for r in w), 'chunks', w[-1]['chunks'], 'entities', max(int(r['entities']) for r in w), 'players', max(int(r['players']) for r in w))
for who in ('PerfHost', 'PerfGuest'):
    c = rows(f'client-{who}.csv')
    print('==', who)
    for idx in sorted({r['mapIdx'] for r in c}):
        for phase in ('battle', 'camp'):
            s = [r for r in c if r['mapIdx'] == idx and r['phase'] == phase and int(r['frames']) > 0]
            if len(s) < 5:
                continue
            fps = [float(r['fpsMean']) for r in s]
            p95 = [float(r['frameP95Ms']) for r in s]
            mx = [float(r['frameMaxMs']) for r in s]
            dt = float(s[-1]['wallS']) - float(s[0]['wallS']) or 1
            din = (int(s[-1]['bytesIn']) - int(s[0]['bytesIn'])) / dt
            dout = (int(s[-1]['bytesOut']) - int(s[0]['bytesOut'])) / dt
            print(f'mapIdx {idx} {phase}: n={len(s)}s fps mean {statistics.mean(fps):.0f} min {min(fps):.0f} p5 {pct(fps, 0.05):.0f}; frame p95 {pct(p95, 0.95):.1f} ms, worst frame {max(mx):.0f} ms; '
                  f'heap max {max(int(r["heapMb"]) for r in s)} MB; in {din / 1024:.1f} KB/s out {dout / 1024:.2f} KB/s; entities {max(int(r["entities"]) for r in s)}; ping {max(int(r["pingMs"]) for r in s)}')
print('== processes (max working set MB, private MB, cpu sec)')
p = rows('procs.csv')
for role in ('server', 'host', 'guest'):
    s = [r for r in p if r['role'] == role]
    if s:
        print(role, max(float(r['workingSetMb']) for r in s), max(float(r['privateMb']) for r in s), s[-1]['cpuSec'])
