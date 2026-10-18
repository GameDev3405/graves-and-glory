"""Exports the UI kit (UI plan Task 1): every texture in tools/ui_art/manifest.py, painted at GUI scale 1 by the
mockups' own painters (copied into tools/ui_art/), into src/main/resources/assets/gravesandglory/textures/gui/
<folder>/<name>.png, with atlas.json holding each PNG's size, box offset, kind and slice insets.

  python tools/build_ui_art.py               paint every manifest entry (and remove PNGs the manifest no longer has)
  python tools/build_ui_art.py --verify      repaint into a temp dir and compare; exit 1 naming the first difference
  python tools/build_ui_art.py --import DIR  refresh the painter copies from DIR (only when a task says so)

Deterministic: the painters iterate sets of int tuples, whose order is fixed for one Python version, so the
script runs only under Python 3.12, and it re-executes itself with PYTHONHASHSEED=0. Pillow is a dev tool only."""
import json, os, re, subprocess, sys, tempfile
from pathlib import Path

if sys.version_info[:2] != (3, 12):
    sys.exit('build_ui_art: needs Python 3.12 (the exported pixels depend on it), not %d.%d' % sys.version_info[:2])
if os.environ.get('PYTHONHASHSEED') != '0':
    os.environ['PYTHONHASHSEED'] = '0'
    sys.exit(subprocess.call([sys.executable] + sys.argv))
sys.dont_write_bytecode = True

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'tools' / 'ui_art'
OUT = ROOT / 'src/main/resources/assets/gravesandglory/textures/gui'
JAVA = (ROOT / 'src/main/java', ROOT / 'test-tools/java')
PAINTERS = ('v4lib', 'v4pal', 'v4a_table', 'v4a_paper', 'v4a_hud', 'v4a_recruit', 'v4a_ink', 'v4a_map', 'v4a_furn',
            'v4a_portrait', 'v4a_recruit2', 'v4art')
NAME_IN_JAVA = re.compile(r'UiTex\.\w+\(\w+,\s*"([a-z0-9/-]+)"')
KINDS = ('fixed', 'nine', 'three-h', 'three-v', 'sheet')


def manifest():
    sys.path.insert(0, str(ART))
    import manifest as m
    return m.MANIFEST


def check_entry(name, e, w, h):
    """the slices, frames and offset must fit the painted PNG, or the game would tile garbage"""
    problems = []
    if e.kind not in KINDS:
        problems.append('kind ' + e.kind)
    if not (0 <= e.ox < w and 0 <= e.oy < h):
        problems.append('offset (%d, %d) outside %dx%d' % (e.ox, e.oy, w, h))
    if e.kind in ('nine', 'three-h') and e.l + e.r >= w:
        problems.append('l %d + r %d leave no middle in width %d' % (e.l, e.r, w))
    if e.kind in ('nine', 'three-v') and e.t + e.b >= h:
        problems.append('t %d + b %d leave no middle in height %d' % (e.t, e.b, h))
    if e.kind == 'sheet' and (e.frames < 1 or w % e.frames):
        problems.append('%d frames do not divide width %d' % (e.frames, w))
    if problems:
        raise SystemExit('build_ui_art: %s: %s' % (name, '; '.join(problems)))


def paint(out):
    """paints every manifest entry into out/<folder>/<name>.png and writes out/atlas.json; returns the atlas"""
    textures = {}
    for name, e in manifest().items():
        im = e.paint()
        im = getattr(im, 'im', im).convert('RGBA')
        check_entry(name, e, im.width, im.height)
        path = out / (name + '.png')
        path.parent.mkdir(parents=True, exist_ok=True)
        im.save(path, 'PNG')
        textures[name] = {'w': im.width, 'h': im.height, 'ox': e.ox, 'oy': e.oy, 'kind': e.kind,
                          'l': e.l, 't': e.t, 'r': e.r, 'b': e.b, 'frames': e.frames}
    lines = ['%s:%s' % (json.dumps(k), json.dumps(v, separators=(',', ':'))) for k, v in textures.items()]
    (out / 'atlas.json').write_text('{"python":"3.12","textures":{\n' + ',\n'.join(lines) + '\n}}\n',
                                    encoding='utf-8', newline='\n')
    return {'python': '3.12', 'textures': textures}


def pngs(root):
    return sorted(p.relative_to(root).with_suffix('').as_posix() for p in root.rglob('*.png'))


def export():
    atlas = paint(OUT)
    for stale in sorted(set(pngs(OUT)) - set(atlas['textures'])):
        (OUT / (stale + '.png')).unlink()
        print('build_ui_art: removed', stale + '.png')
    print('build_ui_art: %d textures painted into %s' % (len(atlas['textures']), OUT.relative_to(ROOT).as_posix()))


def fail(why):
    print('build_ui_art: FAIL ' + why)
    sys.exit(1)


def verify():
    from PIL import Image
    with tempfile.TemporaryDirectory() as tmp:
        fresh = paint(Path(tmp))
        if not (OUT / 'atlas.json').is_file():
            fail('atlas.json is missing; run python tools/build_ui_art.py')
        committed = json.loads((OUT / 'atlas.json').read_text(encoding='utf-8'))
        have = committed.get('textures', {})
        for name, entry in fresh['textures'].items():
            if name not in have:
                fail('atlas.json lacks ' + name)
            if have[name] != entry:
                fail('atlas.json has %s as %s; the manifest paints %s' % (name, have[name], entry))
        if committed != fresh:
            fail('atlas.json differs from the manifest (entries it alone has: %s)' % sorted(set(have) - set(fresh['textures'])))
        on_disk = set(pngs(OUT))
        for name in fresh['textures']:
            if name not in on_disk:
                fail('%s.png is missing from %s' % (name, OUT.relative_to(ROOT).as_posix()))
            a = Image.open(OUT / (name + '.png')).convert('RGBA')
            b = Image.open(Path(tmp) / (name + '.png')).convert('RGBA')
            if a.size != b.size or a.tobytes() != b.tobytes():
                fail('%s.png differs from its repaint (RGBA)' % name)
        extra = sorted(on_disk - set(fresh['textures']))
        if extra:
            fail('%s.png is not in atlas.json' % extra[0])
    for src in JAVA:
        for path in sorted(src.rglob('*.java')):
            for name in NAME_IN_JAVA.findall(path.read_text(encoding='utf-8')):
                if name not in fresh['textures']:
                    fail('%s names texture "%s", which atlas.json lacks' % (path.relative_to(ROOT).as_posix(), name))
    print('build_ui_art: %d textures verified' % len(fresh['textures']))


def import_from(src):
    src = Path(src)
    for n in PAINTERS:
        data = (src / (n + '.py')).read_bytes().replace(b'\r\n', b'\n')   # stored as LF, as .gitattributes keeps *.py
        dest = ART / (n + '.py')
        old = dest.read_bytes() if dest.is_file() else None
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(data)
        print('build_ui_art: %s %s.py' % ('kept' if old == data else 'updated' if old else 'copied', n))


if __name__ == '__main__':
    args = sys.argv[1:]
    if args == ['--verify']:
        verify()
    elif len(args) == 2 and args[0] == '--import':
        import_from(args[1])
    elif not args:
        export()
    else:
        sys.exit(__doc__)
