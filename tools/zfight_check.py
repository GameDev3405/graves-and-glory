"""Troop z-fighting check: no two visible faces of different cubes in one plane, facing the same way and overlapping, on any
troop model, old or GeckoLib, in the rest pose or with the forearms and shins bent (art/hand/common/zfight.py has the rule).

  python tools/zfight_check.py                 every troop: models/troop/<troop>.json and geo/troop/<troop>.geo.json
  python tools/zfight_check.py --troop gl_marshal [--kind old|geo]
  python tools/zfight_check.py --list          every failing pair, not only the first 40

Rule: same outward normal, planes within 0.005 px, overlap over 0.25 px2, not hidden inside a third cube; poses rest,
forearms and shins bent 40 and 75 degrees (zfight.BENDS). Weapons (tags weapon, draw) are left to build_skins' frame check.
Exemptions: tools/zfight_exempt.json, rows {kind, troop, a, b, reason} for a cube pair (any faces) that is hidden or
intended. An exemption that no longer matches a failure fails too, so the list stays short and true.
Exit 0 clean, 1 on any failure. Run by tools/verify.py fast (step zfight).
"""
import argparse, json, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'art/hand/common'))
import zfight  # noqa: E402

ASSETS = ROOT / 'src/main/resources/assets/gravesandglory'
EXEMPT = ROOT / 'tools/zfight_exempt.json'


def models(only=None, kind=None):
    """[(kind, troop, Model)] of every shipped troop model."""
    out = []
    if kind in (None, 'old'):
        for p in sorted((ASSETS / 'models/troop').glob('*.json')):
            d = json.loads(p.read_text(encoding='utf-8'))
            if 'bones' not in d or (only and p.stem != only):
                continue
            out.append(('old', p.stem, zfight.Model.old(d)))
    if kind in (None, 'geo'):
        for p in sorted((ASSETS / 'geo/troop').glob('*.geo.json')):
            t = p.name[:-len('.geo.json')]
            if only and t != only:
                continue
            rig = ASSETS / 'troop_rig' / (t + '.json')
            r = json.loads(rig.read_text(encoding='utf-8')) if rig.is_file() else {}
            out.append(('geo', t, zfight.Model.geo(json.loads(p.read_text(encoding='utf-8')), r, t)))
    return out


def load_exempt(path=EXEMPT):
    if not path.is_file():
        return []
    rows = json.loads(path.read_text(encoding='utf-8'))['exempt']
    for r in rows:
        if not all(r.get(k) for k in ('kind', 'troop', 'a', 'b', 'reason')):
            raise SystemExit('zfight_exempt.json: a row needs kind, troop, a, b and reason: %s' % r)
    return rows


def check(items, exempt):
    """(failures [(kind, troop, Hit)], per-troop counts {(kind, troop): n}, stale exemption rows)."""
    used, fails, counts = set(), [], {}
    for kind, troop, m in items:
        n = 0
        for h in zfight.find(m):
            hit = next((i for i, r in enumerate(exempt) if r['kind'] == kind and r['troop'] == troop
                        and {r['a'], r['b']} == {h.a, h.b}), None)
            if hit is not None:
                used.add(hit)
                continue
            fails.append((kind, troop, h))
            n += 1
        counts[(kind, troop)] = n
    checked = {(k, t) for k, t, _ in items}
    stale = [r for i, r in enumerate(exempt) if i not in used and (r['kind'], r['troop']) in checked]
    return fails, counts, stale


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('--troop')
    ap.add_argument('--kind', choices=('old', 'geo'))
    ap.add_argument('--list', action='store_true')
    a = ap.parse_args(argv)
    items = models(a.troop, a.kind)
    fails, counts, stale = check(items, load_exempt())
    bad = {k: n for k, n in counts.items() if n}
    print('zfight: %d models checked, %d failing pairs in %d models, %d stale exemptions' % (
        len(items), len(fails), len(bad), len(stale)))
    for (kind, troop), n in sorted(bad.items()):
        print('  %-3s %-15s %4d' % (kind, troop, n))
    for kind, troop, h in (fails if a.list else fails[:40]):
        print('  ' + h.line(troop, kind))
    if len(fails) > 40 and not a.list:
        print('  ... %d more (--list)' % (len(fails) - 40))
    for r in stale:
        print('  stale exemption (no longer fails): %s %s %s | %s' % (r['kind'], r['troop'], r['a'], r['b']))
    return 1 if fails or stale else 0


if __name__ == '__main__':
    sys.exit(main())
