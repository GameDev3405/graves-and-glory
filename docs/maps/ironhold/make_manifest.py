"""Writes docs/maps/ironhold/manifest.json (gg-mapforge-map/1) from the signed master layout
(layout-draft-2026-10-30.md, signed by Kort 2026-10-30). Run once; it refuses to overwrite the manifest, because the
manifest then carries section status and reviews (use --force only to start over)."""
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / 'tools'))
from mapforge.manifest import MAP_FORMAT, save_manifest  # noqa: E402

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE / 'manifest.json'
Y0, Y1 = -64, 319
COLS = dict(zip('ABCDEFGHIJK', (-350, -289, -225, -161, -97, -33, 31, 95, 159, 223, 287)))
ROWS = (-250, -176, -112, -48, 16, 80, 144, 208)
X_END, Z_END = 349, 249

# Section names from the layout's section table (section 10); rims and backdrop for the rest.
NAMES = {
    'B1': 'Camp north', 'B2': 'Camp north', 'B3': 'Siege camp', 'B4': 'Siege camp', 'B5': 'Camp south',
    'B6': 'Camp south', 'C1': 'West upland', 'C2': 'Mire north', 'C3': 'The Mire', 'C4': 'The Mire',
    'C5': 'Gallows Ridge west', 'C6': 'Gallows Ridge west', 'D1': 'North bluff', 'D2': 'Mire edge north',
    'D3': 'Killing field west', 'D4': 'Killing field west', 'D5': 'Killing field south', 'D6': 'Gallows Ridge',
    'E1': 'Village outskirts', 'E2': 'Burned village', 'E3': 'Front door', 'E4': 'Outer Yard south',
    'E5': 'South sally', 'E6': 'South ford road', 'F1': 'Point A', 'F2': 'North water gate',
    'F3': 'Inner yard and B', 'F4': 'Keep court', 'F5': 'South water gate', 'F6': 'South ford',
    'G1': 'North reach', 'G2': 'East bailey', 'G3': 'East bailey', 'G4': 'East bailey', 'G5': 'Point C',
    'G6': 'Watch Hill shoulder', 'H1': 'East watch', 'H2': 'Supply east north', 'H3': 'Donjon Court',
    'H4': 'Donjon Court south', 'H5': 'South-east corner', 'H6': 'East watch',
}


def name(sid):
    if sid in NAMES:
        return NAMES[sid]
    col, row = sid[0], int(sid[1])
    if col in 'IJK':
        return 'Gorge parapet' if col == 'I' else 'Gorge backdrop'
    if row == 0:
        return 'North Mountains (rim)'
    if row == 7:
        return 'South Ridge (rim)'
    return 'West rim'


def sections():
    out = []
    cols = list(COLS.items())
    for r, z0 in enumerate(ROWS):
        z1 = ROWS[r + 1] - 1 if r + 1 < len(ROWS) else Z_END
        for c, (col, x0) in enumerate(cols):
            x1 = cols[c + 1][1] - 1 if c + 1 < len(cols) else X_END
            sid = f'{col}{r}'
            out.append({'id': sid, 'name': name(sid), 'bounds': {'min': [x0, Y0, z0], 'max': [x1, Y1, z1]},
                        'status': 'todo', 'reviews': []})
    return out


def box(lid, kind, lo, hi, reason, source):
    return {'id': lid, 'kind': kind, 'min': list(lo), 'max': list(hi), 'reason': reason, 'source': source}


def ring(lid, kind, cx, cz, r, reason, source, y=(64, 67)):
    return {'id': lid, 'kind': kind, 'center': [cx, cz], 'radius': r, 'y': list(y), 'reason': reason, 'source': source}


CAMPAIGN = 'src/main/java/gg/gravesandglory/CampaignMaps.java (Domination points, radius 17, elevation 64)'
SIEGE = 'bannerbound-modes sheets/design.json siege modeSites (read-only), 2026-10-30'
SIGHT = 'bannerbound-modes sheets/design.json landmarkLines sight entries (feet = eye y - 1.6); modes lane to confirm'
LAYOUT = 'docs/maps/ironhold/layout-draft-2026-10-30.md (signed)'

PROTECT = [
    ring('capture-a', 'capture', -29, -130, 17, 'Domination point A ring: no blocks y 64..67 inside the ring', CAMPAIGN),
    ring('capture-b', 'capture', 0, 0, 17, 'Domination point B ring (river deck): no blocks y 64..67', CAMPAIGN),
    ring('capture-c', 'capture', 29, 130, 17, 'Domination point C ring (Watch Hill): no blocks y 64..67', CAMPAIGN),
    box('spawn-dawn', 'spawn', (-316, 56, -2), (-312, 90, 2), 'Dawn (west) spawn column at (-314,0)', LAYOUT),
    box('spawn-dusk', 'spawn', (126, 56, -2), (130, 90, 2), 'Dusk (east) spawn column at (128,0), moved by pick D1a', LAYOUT),
    box('siege-ward-n', 'siege', (-33, 64, -27), (-6, 70, -23),
        'Siege ward-n (ironhold/ward 35x7x5 at -23,64,-25); trimmed to x -33 where it meets the gate wall', SIEGE),
    box('siege-ward-s', 'siege', (-33, 64, 23), (-24, 70, 27),
        'Siege ward-s (ironhold/ward_south 19x7x5 at -31,64,25); trimmed to x -33..-24 between the gate and keep walls', SIEGE),
    box('siege-gate-w', 'siege', (-16, 64, 45), (-12, 70, 47), 'Siege gate-w (ironhold/ward_gate at -14,64,46)', SIEGE),
    box('siege-gate-e', 'siege', (12, 64, 45), (16, 70, 47), 'Siege gate-e (ironhold/ward_gate at 14,64,46)', SIEGE),
    box('siege-gate-ne', 'siege', (12, 64, 21), (16, 70, 23), 'Siege gate-ne (ironhold/ward_gate at 14,64,22)', SIEGE),
    box('siege-ward-b', 'siege', (-8, 64, -12), (-4, 70, 12), 'Siege ward-b (ironhold/ward_bridge 25x7x5 at -6,64,0, rot 90)', SIEGE),
    ring('siege-stage-a', 'siege', -55, 0, 15, 'Siege stage A ring in the Outer Yard: no blocks y 64..67', LAYOUT),
    ring('siege-stage-c', 'siege', -8, 34, 10, 'Siege stage C ring in the keep court: no blocks y 64..67', LAYOUT),
    box('station-gate-sight', 'station', (-40, 77, -10), (-38, 78, -8),
        'Defender station: gate tower top, gate-sight stand (feet about -39,77,-9)', SIGHT),
    box('station-keep-sight', 'station', (-10, 71, 20), (-8, 72, 22),
        'Defender station: keep north wall-walk, keep-sight stand (feet about -9,71,21)', SIGHT),
]


def manifest():
    return {
        'format': MAP_FORMAT,
        'id': 'ironhold',
        'name': 'Ironhold',
        'palette': 'ironhold (pending the palette work, backlog item 3; grass and leaves tint waits for it)',
        'bounds': {'min': [-350, Y0, -250], 'max': [X_END, Y1, Z_END]},
        'layout': {
            'doc': 'docs/maps/ironhold/layout-draft-2026-10-30.md', 'signedBy': 'Kort', 'signedAt': '2026-10-30',
            'picks': {'D1': 'a: keep coordinates, add the east gorge, Dusk spawn to (128,0)',
                      'D2': 'a: new Great Keep tower to about y 114',
                      'D3': 'a: keep points A/B/C',
                      'D4': 'a: defenders-only keep postern (built by the modes lane)',
                      'D5': 'a: the Mire with a causeway',
                      'D6': 'a: north low and wet, south high'},
            'workflow': 'Sections about 64x64; passes blockout, structure, natural, detail; agents review each pass '
                        'from top-down and isometric renders; Kort signs only the layout and the finished map.',
        },
        'base': 'Until the 2A.2 export of the live map exists, the stand-in base world is built by '
                'docs/maps/ironhold/sections/make_blockout.py base (flat ground top y 63 plus today\'s gate, keep and '
                'west curtain pieces). Replace it with the export before the structure pass.',
        'sections': sections(),
        'protect': PROTECT,
    }


if __name__ == '__main__':
    if OUT.exists() and '--force' not in sys.argv:
        raise SystemExit(f'{OUT} exists and holds section status and reviews; pass --force to start over')
    m = manifest()
    save_manifest(OUT, m)
    print(f'{OUT}: {len(m["sections"])} sections, {len(m["protect"])} protect locks')
