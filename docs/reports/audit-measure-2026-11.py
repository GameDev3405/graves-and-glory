#!/usr/bin/env python3
"""Tables for audit-measure-2026-11.md: python docs/reports/audit-measure-2026-11.py base=DIR nohurt=DIR noarmor=DIR both=DIR (DIR = a server root holding duel/duels.csv, drops.csv)."""
import csv, json, math, sys, collections, pathlib
ROOT = pathlib.Path(__file__).resolve().parents[2]
T = {t['id']: t for t in json.loads((ROOT / 'sheets/design.json').read_text(encoding='utf-8'))['troops']}
runs = dict(a.split('=') for a in sys.argv[1:])

def mono(label):
    p = label.split('+')
    if len(p) != 1 or '*' not in p[0]: return None
    return p[0].split('*')[0]

def load(d):
    out = []
    for r in csv.DictReader(open(pathlib.Path(d) / 'duel/duels.csv', encoding='utf-8')):
        if r['winner'] == 'aborted': continue
        a, b = mono(r['aLineup']), mono(r['bLineup'])
        if a is None or b is None or a == b: continue
        y = 1.0 if r['winner'] == 'a' else 0.5 if r['winner'] == 'draw' else 0.0
        out.append((r['id'], a, b, y))
    return out

def se(p, n): return 100 * math.sqrt(max(p * (1 - p), 1e-9) / n) if n else float('nan')

res = {}
for name, d in runs.items():
    rows = load(d)
    house = collections.defaultdict(list); pair = collections.defaultdict(list); troop = collections.defaultdict(list)
    for _, a, b, y in rows:
        ha, hb = T[a]['house'], T[b]['house']
        troop[a].append(y); troop[b].append(1 - y)
        if ha != hb:
            house[ha].append(y); house[hb].append(1 - y)
            pair[(ha, hb)].append(y); pair[(hb, ha)].append(1 - y)
    res[name] = dict(n=len(rows), house=house, pair=pair, troop=troop)
names = list(runs)
print('fights counted (cross-troop):', {n: res[n]['n'] for n in names})
print('\n## House win % (cross-House fights)\n\n| House | ' + ' | '.join(names) + ' |\n|---|' + '---|' * len(names))
for h in ['graveborn', 'tidecallers', 'ironclad', 'gilded']:
    cells = []
    for n in names:
        v = res[n]['house'][h]; p = sum(v) / len(v) if v else float('nan')
        cells.append(f'{100*p:.1f} (n={len(v)})')
    print(f'| {h} | ' + ' | '.join(cells) + ' |')
print('\n## Pairings (row House win %)\n\n| Pairing | ' + ' | '.join(names) + ' |\n|---|' + '---|' * len(names))
hs = ['graveborn', 'tidecallers', 'ironclad', 'gilded']
for i, a in enumerate(hs):
    for b in hs[i + 1:]:
        print(f'| {a} v {b} | ' + ' | '.join(f'{100*sum(res[n]["pair"][(a,b)])/max(1,len(res[n]["pair"][(a,b)])):.1f}' for n in names) + ' |')
print('\n## Per-troop win % (non-mirror fights) and change from base\n')
base = res[names[0]]
print('| Troop | House | ' + ' | '.join(names[:1] + [f'{n} (d)' for n in names[1:]]) + ' |\n|---|---|' + '---|' * len(names))
for t in sorted(T, key=lambda t: (T[t]['house'], t)):
    if not base['troop'].get(t): continue
    row = [f'{100*sum(base["troop"][t])/len(base["troop"][t]):.1f}']
    for n in names[1:]:
        v = res[n]['troop'][t]; b0 = base['troop'][t]
        p, q = sum(v) / len(v), sum(b0) / len(b0)
        row.append(f'{100*p:.1f} ({100*(p-q):+.1f})')
    print(f'| {t} | {T[t]["house"]} | ' + ' | '.join(row) + ' |')
print('\n## Dropped shots (target died or vanished in flight)\n')
for n in names:
    f = pathlib.Path(runs[n]) / 'duel/drops.csv'
    if f.exists():
        print(f'\n{n}:'); print(f.read_text())
