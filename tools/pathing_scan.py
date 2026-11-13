"""tools/pathing_scan.py: test-only, read-only measuring tool for troop pathing on a bundled engine map (plan: docs/superpowers/specs/2026-11-13-engine-map-pathing-design.md).

    python tools/pathing_scan.py MAPDIR cut X0 X1 Z0 Z1      # ASCII plan of what troops can stand in and what blocks them
    python tools/pathing_scan.py MAPDIR route SPAWN POINT    # shortest walk over the floors graph, simplified to turn points, with the narrowest width met
    python tools/pathing_scan.py MAPDIR line X0 Z0 X1 Z1     # ground-open runs along a straight line (a wall: where its gates are, whether any is sealed)
    python tools/pathing_scan.py MAPDIR stuck stuck.csv      # label stuck.csv rows (bot sim) with the nearest passage and its width
MAPDIR is a sectionmaps folder, e.g. src/main/resources/data/gravesandglory/sectionmaps/kingsfield_cq. It reads the .stk tiles and applies the
packer's own walking rule (tools/sectionmap.py floors_of: floors, steps of one block, 4-neighbour). It changes nothing."""
import csv, gzip, json, math, pathlib, sys
from collections import deque

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from mapforge import nbt  # noqa: E402
from mapforge.structure import block_name  # noqa: E402
from sectionmap import floors_of, kind  # noqa: E402


def load(mapdir):
    mapdir = pathlib.Path(mapdir)
    m = json.loads((mapdir / 'map.json').read_text(encoding='utf-8'))
    ox, oz, floor = m['origin'][0], m['origin'][1], m['floorY']
    w, d = m['width'], m['depth']
    floors, solid_k = {}, {}
    for t in m['tiles']:
        _, root = nbt.read_file(mapdir / t['file'])
        plain = root.value
        sx, sy, sz = [int(v.value) for v in plain['size'].value]
        palette = []
        for e in plain['palette'].value:
            name = e.value['Name'].value
            palette.append(name)  # state text is only needed for kind(); name is enough
        stack = np.frombuffer(plain['stack'].value if isinstance(plain['stack'].value, (bytes, bytearray)) else bytes(b & 255 for b in plain['stack'].value), dtype='>i2').reshape(sz, sx, sy)
        for (z, x), ys in floors_of(stack, palette, floor).items():
            floors[(t['z'] + z, t['x'] + x)] = ys
        # blocked at floor level: any collision block 1..2 above the top floor is not tracked here; keep the kinds for the cut view
        k = np.array([kind(p) for p in palette] + [0])[stack]
        for z in range(sz):
            for x in range(sx):
                solid_k[(t['z'] + z, t['x'] + x)] = (k[z, x], floor)
    return m, ox, oz, floor, w, d, floors, solid_k


def reach(floors, w, d, start, near_y):
    here = floors.get((start[1], start[0]), [])
    s = (start[0], start[1], min(here, key=lambda y: abs(y - near_y)))
    dist, prev = {s: 0}, {}
    q = deque([s])
    while q:
        c = q.popleft()
        x, z, y = c
        for nx, nz in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            for ny in floors.get((nz, nx), ()):
                n = (nx, nz, ny)
                if abs(ny - y) <= 1 and n not in dist:
                    dist[n] = dist[c] + 1
                    prev[n] = c
                    q.append(n)
    return s, dist, prev


def width_at(floors, x, z, y, ox_unused=0, reach_max=40):
    """Free width across the passage at a floor cell: the shorter of the x-run and the z-run of floor cells within one block of y through it."""
    def run(dx, dz):
        n = 1
        for sgn in (1, -1):
            cx, cz = x, z
            while n < reach_max:
                cx += dx * sgn
                cz += dz * sgn
                if not any(abs(f - y) <= 1 for f in floors.get((cz, cx), ())):
                    break
                n += 1
        return n
    return min(run(1, 0), run(0, 1))


def main(argv):
    mapdir, cmd = argv[0], argv[1]
    m, ox, oz, floor, w, d, floors, solid_k = load(mapdir)
    if cmd == 'cut':
        x0, x1, z0, z1 = (int(v) for v in argv[2:6])
        for wz in range(z0, z1 + 1):
            row = ''
            for wx in range(x0, x1 + 1):
                c = (wz - oz, wx - ox)
                ys = floors.get(c, [])
                row += '.' if len(ys) == 1 else ('=' if len(ys) > 1 else '#')
            print(f'{wz:5d} {row}')
        print('       . one floor  = two or more floors (gatehouse, bridge)  # none (wall, water, void)   x from', x0)
    elif cmd == 'route':
        spawn = next(s for s in m['spawns'] if s['id'] == argv[2])
        point = next(p for p in m['points'] if p['id'] == argv[3])
        s, dist, prev = reach(floors, w, d, (spawn['x'], spawn['z']), spawn['y'] - 1)
        goal = min((c for c in dist if c[0] == point['x'] and c[1] == point['z']), key=lambda c: dist[c], default=None)
        if goal is None:
            print('unreachable'); return
        path = [goal]
        while path[-1] in prev:
            path.append(prev[path[-1]])
        path.reverse()
        turns = [path[0]]
        for i in range(6, len(path) - 6, 6):
            a, b, c = path[i - 6], path[i], path[i + 6]
            a1, a2 = math.atan2(b[1] - a[1], b[0] - a[0]), math.atan2(c[1] - b[1], c[0] - b[0])
            if abs((a2 - a1 + math.pi) % (2 * math.pi) - math.pi) > 0.5:
                turns.append(b)
        turns.append(path[-1])
        walk = next((r['walk'] for r in m['routes'] if r['spawn'] == argv[2] and r['point'] == point['name']), None)
        widths = [width_at(floors, x, z, y) for (x, z, y) in path[::3]]
        print(f"{argv[2]} -> {argv[3]}: {len(path)} steps (map.json says {walk}), narrowest passage {min(widths)} blocks")
        print('turn points (world x, z, y):', [(x + ox, z + oz, y) for x, z, y in turns])
    elif cmd == 'line':
        x0, z0, x1, z1 = (int(v) for v in argv[2:6])
        n = max(abs(x1 - x0), abs(z1 - z0))
        runs, cur = [], None
        for i in range(n + 1):
            x, z = x0 + round((x1 - x0) * i / max(n, 1)), z0 + round((z1 - z0) * i / max(n, 1))
            ok = any(y <= m['baseY'] + 1 for y in floors.get((z - oz, x - ox), ()))
            if ok:
                cur = [(x, z), (x, z)] if cur is None else [cur[0], (x, z)]
            elif cur:
                runs.append(cur); cur = None
        if cur:
            runs.append(cur)
        print('ground-open runs (start, end):', runs if runs else 'NONE: the line is closed')
    elif cmd == 'stuck':
        rows = list(csv.DictReader(open(argv[2])))
        out = {}
        for r in rows:
            x, z = float(r['x']), float(r['z'])
            cx, cz = int(math.floor(x - ox)), int(math.floor(z - oz))
            ys = floors.get((cz, cx), [])
            y = None
            if ys:
                y = min(ys)
            wd = width_at(floors, cx, cz, y) if y is not None else -1
            key = (round(abs(x) / 4) * 4, round(z / 4) * 4, 'tunnel' if len(ys) > 1 else 'open', wd)
            out[key] = out.get(key, 0) + 1
        for k, v in sorted(out.items(), key=lambda kv: -kv[1])[:30]:
            print(v, k)


if __name__ == '__main__':
    main(sys.argv[1:])
