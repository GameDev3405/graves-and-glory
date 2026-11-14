"""Builds the prop server resource pack (prop kit plan task S2). Stdlib only.

Reads the prop defs (src/main/resources/data/gravesandglory/gg_props/*.json) and the art (<art>/<id>/<part>.json, <art>/<id>/<id>.png; Blockbench
Java item model exports, model px (0,0,0) = min corner of the prop's ANCHOR cell, x east, y up, z south, front north, 16 px = 1 block). A part
without art gets a placeholder model: boxes over the prop's solid cells in that frame, clipped to -16..32, textured with a flat 16x16 colour.

Writes dist/props/gg-props-<sha8>.zip (byte-reproducible: sorted entries, fixed dates) and dist/props/gg-props-latest.sha1 (40 hex of the zip):
  assets/minecraft/models/item/paper.json            vanilla paper look + one override per part, ascending custom_model_data
  assets/gravesandglory/models/prop/<model>.json     each part
  assets/gravesandglory/textures/prop/<id>.png       one sheet per prop
  assets/minecraft/atlases/blocks.json               adds textures/prop to the block atlas (else the parts render magenta-black)
  pack.mcmeta                                        pack_format 15 (1.20.1)

Exit 1: a bad def, or a model element outside -16..32 (named). Exit 2: a paper custom_model_data in 7310001..7319999 already used by the
Excalibur pack or by one of the mod's own resource packs (src/main/resources/resourcepacks/*).
"""
import argparse
import hashlib
import io
import json
import pathlib
import struct
import sys
import zipfile
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
DEFS = ROOT / 'src' / 'main' / 'resources' / 'data' / 'gravesandglory' / 'gg_props'
PACKS = ROOT / 'src' / 'main' / 'resources' / 'resourcepacks'
EXCALIBUR = ROOT / 'dependencies' / 'resourcepacks' / 'Excalibur_V1.20.zip'
CMD_MIN, CMD_MAX = 7310001, 7319999
LO, HI = -16, 32
DATE = (1980, 1, 1, 0, 0, 0)
PAPER = 'assets/minecraft/models/item/paper.json'
ATLAS = 'assets/minecraft/atlases/blocks.json'  # 1.20.1 stitches only textures/block and textures/item into the block atlas
FIXED = {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}


class BuildError(Exception):
    pass


def load_defs(folder):
    defs = []
    for f in sorted(pathlib.Path(folder).glob('*.json')):
        d = json.loads(f.read_text(encoding='utf8'))
        if d.get('id') != f.stem:
            raise BuildError('%s: id %r does not match the file name' % (f.name, d.get('id')))
        defs.append(d)
    models, cmds = {}, {}
    for d in defs:
        for p in d['parts']:
            if p['model'] in models:
                raise BuildError('part model %s used by %s and %s' % (p['model'], models[p['model']], d['id']))
            if p['cmd'] in cmds:
                raise BuildError('cmd %d used by %s and %s' % (p['cmd'], cmds[p['cmd']], p['model']))
            if not CMD_MIN <= p['cmd'] <= CMD_MAX:
                raise BuildError('%s: cmd %d outside %d..%d' % (p['model'], p['cmd'], CMD_MIN, CMD_MAX))
            models[p['model']] = d['id']
            cmds[p['cmd']] = p['model']
    return defs


# ---- clash check ----

def paper_cmds(text, source):
    try:
        model = json.loads(text)
    except ValueError:
        return []
    return [(o.get('predicate', {}).get('custom_model_data'), source) for o in model.get('overrides', [])]


def clashes(excalibur, packs):
    found = []
    if excalibur and pathlib.Path(excalibur).is_file():
        with zipfile.ZipFile(excalibur) as z:
            for n in z.namelist():
                if n.lower().endswith('models/item/paper.json'):
                    found += paper_cmds(z.read(n).decode('utf8', 'replace'), '%s!%s' % (pathlib.Path(excalibur).name, n))
    if packs and pathlib.Path(packs).is_dir():
        for f in sorted(pathlib.Path(packs).rglob('paper.json')):
            if f.parent.name == 'item':
                found += paper_cmds(f.read_text(encoding='utf8', errors='replace'), str(f.relative_to(packs)))
    return [(c, s) for c, s in found if isinstance(c, (int, float)) and CMD_MIN <= c <= CMD_MAX]


# ---- placeholders ----

def png(rgba, size=16):
    raw = b''.join(b'\x00' + bytes(rgba) * size for _ in range(size))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def colour(pid):
    h = hashlib.sha1(pid.encode()).digest()
    return (80 + h[0] % 140, 80 + h[1] % 140, 80 + h[2] % 140, 255)


def boxes(cells):
    """Greedy merge of cells into boxes (x first, then z, then y): [(x0,y0,z0,x1,y1,z1)] inclusive cell ranges."""
    left = set(map(tuple, cells))
    out = []
    for c in sorted(left, key=lambda c: (c[1], c[2], c[0])):
        if c not in left:
            continue
        x0, y0, z0 = c
        x1 = x0
        while (x1 + 1, y0, z0) in left:
            x1 += 1
        z1 = z0
        while all((x, y0, z1 + 1) in left for x in range(x0, x1 + 1)):
            z1 += 1
        y1 = y0
        while all((x, y1 + 1, z) in left for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)):
            y1 += 1
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    left.discard((x, y, z))
        out.append((x0, y0, z0, x1, y1, z1))
    return out


def element(frm, to):
    faces = {f: {'uv': [0, 0, 16, 16], 'texture': '#0'} for f in ('north', 'east', 'south', 'west', 'up', 'down')}
    return {'from': frm, 'to': to, 'faces': faces}


def placeholder(d, index):
    """Part 0: boxes over the solid cells (anchor-cell frame, clipped to -16..32; a small cube when there are none). Later parts: a small marker on the anchor cell's north face."""
    px, pz = (d['size'][0] - 1) // 2, (d['size'][2] - 1) // 2
    elements = []
    if index == 0:
        for x0, y0, z0, x1, y1, z1 in boxes(d['solid']):
            frm = [(x0 - px) * 16, y0 * 16, (z0 - pz) * 16]
            to = [(x1 - px + 1) * 16, (y1 + 1) * 16, (z1 - pz + 1) * 16]
            frm, to = [max(LO, min(HI, v)) for v in frm], [max(LO, min(HI, v)) for v in to]
            if all(a < b for a, b in zip(frm, to)):
                elements.append(element(frm, to))
        if not elements:  # no solid cell (a wall-hung or overhead prop): a small cube in the anchor cell
            elements.append(element([4, 4, 4], [12, 12, 12]))
    else:
        y = 2 + 6 * (index % 5)  # stacked markers stay inside -16..32: five to a column, four columns, then they wrap (a split prop has up to 100 parts)
        x = 2 + 8 * ((index // 5) % 4)
        elements.append(element([x, y, -2], [x + 6, y + 4, 0]))
    lo = [min(e['from'][i] for e in elements) for i in range(3)]
    hi = [max(e['to'][i] for e in elements) for i in range(3)]
    scale = round(0.625 * 16 / max(16, max(h - l for h, l in zip(hi, lo))), 4)
    tex = 'gravesandglory:prop/' + d['id']
    return {'textures': {'0': tex, 'particle': tex}, 'elements': elements,
            'display': {'fixed': FIXED, 'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [scale] * 3}}}


# ---- art ----

def from_art(model, pid):
    tex = 'gravesandglory:prop/' + pid
    model = dict(model)
    model['textures'] = {k: tex for k in model.get('textures', {'0': tex})} or {'0': tex}
    return model


def check_elements(name, model):
    for e in model.get('elements', []):
        for v in list(e.get('from', [])) + list(e.get('to', [])):
            if not LO <= v <= HI:
                raise BuildError('model %s: element %s..%s outside %d..%d' % (name, e.get('from'), e.get('to'), LO, HI))


def build(defs, art):
    files, overrides, placeholders = {}, [], 0
    for d in defs:
        pid = d['id']
        sheet = art / pid / (pid + '.png')
        for i, p in enumerate(d['parts']):
            src = art / pid / (p['model'] + '.json')
            if src.is_file():
                if not sheet.is_file():
                    raise BuildError('model %s: art %s has no texture %s' % (p['model'], src, sheet))
                model = from_art(json.loads(src.read_text(encoding='utf8')), pid)
            else:
                model = placeholder(d, i)
                placeholders += 1
            check_elements(p['model'], model)
            files['assets/gravesandglory/models/prop/%s.json' % p['model']] = json.dumps(model, indent=1).encode()
            overrides.append({'predicate': {'custom_model_data': p['cmd']}, 'model': 'gravesandglory:prop/' + p['model']})
        files['assets/gravesandglory/textures/prop/%s.png' % pid] = sheet.read_bytes() if sheet.is_file() else png(colour(pid))
    overrides.sort(key=lambda o: o['predicate']['custom_model_data'])
    files[PAPER] = json.dumps({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'minecraft:item/paper'},
                               'overrides': overrides}, indent=1).encode()
    files[ATLAS] = json.dumps({'sources': [{'type': 'directory', 'source': 'prop', 'prefix': 'prop/'}]}, indent=1).encode()
    files['pack.mcmeta'] = json.dumps({'pack': {'pack_format': 15, 'description': 'Graves and Glory props'}}, indent=1).encode()
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w', zipfile.ZIP_DEFLATED) as z:
        for name in sorted(files):
            info = zipfile.ZipInfo(name, DATE)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            info.create_system = 0
            z.writestr(info, files[name])
    return buf.getvalue(), placeholders


def main(argv=None):
    a = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    a.add_argument('--art', default=str(ROOT / 'art' / 'props'))
    a.add_argument('--out', default=str(ROOT / 'dist' / 'props'))
    a.add_argument('--excalibur', default=str(EXCALIBUR))
    a.add_argument('--defs', default=str(DEFS))
    a.add_argument('--packs', default=str(PACKS), help="the mod's own resource packs scanned for clashes")
    args = a.parse_args(argv)
    try:
        defs = load_defs(args.defs)
        if not pathlib.Path(args.excalibur).is_file():
            print('note: no Excalibur pack at %s; clash check covers the mod packs only' % args.excalibur)
        found = clashes(args.excalibur, args.packs)
        if found:
            for c, s in found:
                print('clash: custom_model_data %d on paper in %s' % (c, s))
            return 2
        data, placeholders = build(defs, pathlib.Path(args.art))
    except (BuildError, KeyError, ValueError) as e:
        print('error: %s' % e, file=sys.stderr)
        return 1
    sha = hashlib.sha1(data).hexdigest()
    out = pathlib.Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    (out / ('gg-props-%s.zip' % sha[:8])).write_bytes(data)
    (out / 'gg-props-latest.sha1').write_text(sha + '\n')
    print('placeholders: %d' % placeholders)
    print('wrote %s (sha1 %s)' % (out / ('gg-props-%s.zip' % sha[:8]), sha))
    return 0


if __name__ == '__main__':
    sys.exit(main())
