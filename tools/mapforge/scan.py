"""python tools/mapforge scan MAP.json --world WORLD.json [--design sheets/design.json] [--json]

Read-only layout scan (plan tasks 4A.3 and 3.1 prep): reads a built offline world and reports what the generated game
data has to agree with. It proposes nothing and writes nothing.

  spawns     the manifest's spawn locks: standing height, headroom, and the walking distance to every objective
  captures   the manifest's capture locks: ground heights in the ring and blocks standing in the locked band
  routes     walk distances from each spawn to the capture centres, supply yards and Siege rings (a node graph:
             step up 1, drop 3, two blocks of headroom, water at most 2 deep)
  landmarks  the design sheet's rows of this map against the scanned ground and the playable box
"""
import heapq
import hashlib
import json

from .structure import block_name

# Walk-through blocks, matched by exact name or by a whole-word suffix. Plain substring tests were wrong: 'air' matched
# stairs, 'light' matched light_gray_wool and daylight_detector, 'fire' matched fire_coral_block.
NONSOLID = frozenset((
    'air', 'cave_air', 'void_air', 'water', 'lava', 'short_grass', 'tall_grass', 'grass', 'fern', 'large_fern',
    'torch', 'wall_torch', 'soul_torch', 'soul_wall_torch', 'redstone_torch', 'redstone_wall_torch', 'torchflower',
    'lever', 'vine', 'glow_lichen', 'light', 'ladder', 'rail', 'powered_rail', 'detector_rail', 'activator_rail',
    'fire', 'soul_fire', 'cobweb', 'tripwire', 'tripwire_hook', 'snow', 'redstone_wire', 'string',
    'poppy', 'dandelion', 'cornflower', 'allium', 'azure_bluet', 'blue_orchid', 'oxeye_daisy', 'lily_of_the_valley',
    'wither_rose', 'sunflower', 'lilac', 'rose_bush', 'peony', 'pink_petals', 'dead_bush', 'sweet_berry_bush',
    'seagrass', 'tall_seagrass', 'kelp', 'kelp_plant', 'sugar_cane',
    'cave_vines', 'cave_vines_plant', 'weeping_vines', 'weeping_vines_plant', 'twisting_vines', 'twisting_vines_plant',
))
NONSOLID_SUFFIXES = ('_banner', '_sign', '_button', '_carpet', '_pressure_plate', '_sapling', '_tulip')
STEPS = ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1))


def _solid(state):
    name = block_name(state).split(':')[-1]
    if name == 'snow_block':
        return True
    return name not in NONSOLID and not name.endswith(NONSOLID_SUFFIXES)


class Grid:
    def __init__(self, world, lo, hi):
        self.blocks = world.blocks
        self.cols = {}
        for (x, y, z) in self.blocks:
            if lo[0] <= x <= hi[0] and lo[1] <= z <= hi[1]:
                self.cols.setdefault((x, z), []).append(y)
        for ys in self.cols.values():
            ys.sort()
        self._solid, self._water = {}, {}

    def solid(self, x, y, z):
        k = (x, y, z)
        v = self._solid.get(k)
        if v is None:
            c = self.blocks.get(k)
            v = self._solid[k] = c is not None and _solid(c.state)
        return v

    def water(self, x, y, z):
        c = self.blocks.get((x, y, z))
        return c is not None and block_name(c.state) == 'minecraft:water'

    def barrier(self, x, y, z):
        c = self.blocks.get((x, y, z))
        return c is not None and block_name(c.state) == 'minecraft:barrier'

    def stands(self, x, z):
        """y of every standing spot in the column: solid below, two free cells, not under 3+ blocks of water. A barrier
        (a prop's solid cell, Props.place) is solid and never a floor: routes go round props, not through or over them."""
        out = []
        for y in self.cols.get((x, z), ()):
            s = y + 1
            if not self.solid(x, y, z) or self.barrier(x, y, z) or self.solid(x, s, z) or self.solid(x, s + 1, z):
                continue
            d = 0
            while d < 3 and self.water(x, s + d, z):
                d += 1
            if d <= 2:
                out.append(s)
        return out

    def top_stand(self, x, z):
        s = self.stands(x, z)
        return max(s) if s else None

    def near(self, x, z, y=None, r=6):
        """A standing node near (x, z); y picks the nearest height, otherwise the highest."""
        best = None
        for d in range(r + 1):
            for dx in range(-d, d + 1):
                for dz in range(-d, d + 1):
                    if max(abs(dx), abs(dz)) != d:
                        continue
                    s = self.stands(x + dx, z + dz)
                    if s:
                        pick = min(s, key=lambda v: abs(v - y)) if y is not None else max(s)
                        if best is None or d < best[0]:
                            best = (d, (x + dx, pick, z + dz))
            if best:
                return best[1]
        return None

    def dijkstra(self, start):
        dist, heap = {start: 0.0}, [(0.0, start)]
        while heap:
            d, (x, y, z) = heapq.heappop(heap)
            if d > dist[(x, y, z)]:
                continue
            for dx, dz in STEPS:
                nx, nz = x + dx, z + dz
                if dx and dz and not (self.stands(x + dx, z) and self.stands(x, z + dz)):
                    continue
                for ny in self.stands(nx, nz):
                    if ny - y > 1 or y - ny > 3:
                        continue
                    nd = d + (1.4142 if dx and dz else 1.0)
                    if nd < dist.get((nx, ny, nz), 1e18):
                        dist[(nx, ny, nz)] = nd
                        heapq.heappush(heap, (nd, (nx, ny, nz)))
        return dist


def _centre(lock):
    if 'center' in lock:
        return lock['center'][0], lock['center'][1]
    return (lock['min'][0] + lock['max'][0]) // 2, (lock['min'][2] + lock['max'][2]) // 2


def _reach(dist, g, x, z, y=None):
    node = g.near(x, z, y)
    if node is None:
        return None
    best = min((d for (nx, ny, nz), d in dist.items() if abs(nx - node[0]) <= 2 and abs(nz - node[2]) <= 2 and abs(ny - node[1]) <= 3), default=None)
    return None if best is None else round(best)


def scan(manifest, world, design=None, box=None):
    if box is None:
        lo, hi = world.bounds()
        box = ((lo[0], lo[2]), (hi[0], hi[2]))
    g = Grid(world, box[0], box[1])
    locks = manifest.get('protect', [])
    report = {'map': manifest['id'], 'spawns': [], 'captures': [], 'landmarks': [], 'problems': []}
    caps = [l for l in locks if l.get('kind') == 'capture']
    sieges = [l for l in locks if l.get('kind') == 'siege']
    targets = [(c['id'], *c['center'], None) for c in caps]
    rows = [r for r in (design or {}).get('supplyHouses', []) if r['map'] == 1] if manifest['id'] == 'ironhold' else []
    targets += [(r['id'], r['yardX'], r['yardZ'], None) for r in rows]
    for s in sieges:
        cx, cz = _centre(s)
        targets.append((s['id'], cx, cz, None))
    for lock in [l for l in locks if l.get('kind') == 'spawn']:
        cx, cz = _centre(lock)
        node = g.near(cx, cz, None, 0)
        entry = {'id': lock['id'], 'at': [cx, cz], 'standY': node[1] if node else None, 'routes': {}}
        if node is None:
            report['problems'].append(f'{lock["id"]}: no standing spot at ({cx},{cz})')
            report['spawns'].append(entry)
            continue
        above = [y for y in g.cols.get((cx, cz), ()) if y >= node[1]]
        entry['blocksAboveSpawn'] = len(above)
        dist = g.dijkstra(node)
        entry['reached'] = len(dist)
        for tid, tx, tz, ty in targets:
            d = _reach(dist, g, tx, tz, ty)
            entry['routes'][tid] = d
            if d is None:
                report['problems'].append(f'{lock["id"]}: no walking route to {tid} ({tx},{tz})')
        report['spawns'].append(entry)
    for c in caps:
        (cx, cz), r, (y0, y1) = c['center'], c['radius'], c['y']
        tops, inband, wet = [], 0, 0
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                if (x - cx) ** 2 + (z - cz) ** 2 > r * r:
                    continue
                s = g.stands(x, z)
                if s:
                    tops.append(max(s) - 1)
                for y in range(y0, y1 + 1):
                    cell = world.blocks.get((x, y, z))
                    if cell is not None and block_name(cell.state) != 'minecraft:air':
                        if block_name(cell.state) == 'minecraft:water':
                            wet += 1
                        else:
                            inband += 1
        entry = {'id': c['id'], 'centre': [cx, cz], 'radius': r, 'band': [y0, y1], 'groundMin': min(tops, default=None),
                 'groundMax': max(tops, default=None), 'blocksInBand': inband, 'waterInBand': wet}
        report['captures'].append(entry)
        if inband:
            report['problems'].append(f'{c["id"]}: {inband} block(s) in the locked band y {y0}..{y1}')
    if design and manifest['id'] == 'ironhold':
        for r in design['landmarks']:
            if r['map'] != 1:
                continue
            stands = g.stands(r['x'], r['z'])
            top = min(stands, key=lambda v: abs(v - r['elevation'] - 1)) if stands else None
            note = []
            if top is not None and abs(top - 1 - r['elevation']) > 2:
                note.append(f'row elevation {r["elevation"]} but scanned ground y {top - 1}')
            if r['x'] >= 166:
                note.append('inside the gorge (playable ground ends near x 165)')
            if r['id'].startswith('ih-curtain-e') and abs(r['x']) == 72:
                note.append('x 72 east curtain: dropped when the rebuilt map is adopted (Kort 2026-10-31)')
            if top is None:
                note.append('no standing ground at the row')
            report['landmarks'].append({'id': r['id'], 'x': r['x'], 'z': r['z'], 'rowElevation': r['elevation'],
                                         'scannedGround': None if top is None else top - 1, 'notes': note})
            if note and not all(n.startswith('x 72') for n in note):
                report['problems'].append(f'{r["id"]} ({r["x"]},{r["z"]}): ' + '; '.join(note))
    if design and manifest['id'] == 'ironhold':
        rows = sorted((r['id'], r['template'], r['x'], r['z'], r['rotation'], r['elevation']) for r in design['landmarks'] if r['map'] == 1)
        report['landmarkFingerprint'] = {'rows': len(rows), 'sha256': hashlib.sha256(json.dumps(rows).encode()).hexdigest()[:16]}
    return report


def format_report(rep):
    out = [f'scan of {rep["map"]}']
    for s in rep['spawns']:
        out.append(f'spawn {s["id"]} at {s["at"]} stand y {s["standY"]} reached {s.get("reached")} nodes')
        for t, d in s['routes'].items():
            out.append(f'  -> {t}: {"NO ROUTE" if d is None else str(d) + " blocks"}')
    for c in rep['captures']:
        out.append(f'capture {c["id"]} {c["centre"]} r{c["radius"]}: ground y {c["groundMin"]}..{c["groundMax"]}, blocks in band {c["blocksInBand"]}, water in band {c["waterInBand"]}')
    for l in rep['landmarks']:
        out.append(f'landmark {l["id"]} ({l["x"]},{l["z"]}) row y {l["rowElevation"]} scanned ground y {l["scannedGround"]}' + (' | ' + '; '.join(l['notes']) if l['notes'] else ''))
    if 'landmarkFingerprint' in rep:
        out.append('landmark rows %(rows)d fingerprint %(sha256)s' % rep['landmarkFingerprint'])
    out.append('problems: ' + ('none' if not rep['problems'] else ''))
    out += ['  ' + p for p in rep['problems']]
    return '\n'.join(out)
