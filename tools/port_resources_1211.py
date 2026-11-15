#!/usr/bin/env python3
"""Converts the mod's 1.20.1 resources and data to Minecraft 1.21.1 formats, in place. Idempotent and re-runnable:
main's data keeps landing during the port and is copied over the port branch at set points, then this runs again.

    python tools/port_resources_1211.py            convert (default targets: src/main/resources and test-tools/resources)
    python tools/port_resources_1211.py --check    report what would change, change nothing, exit 1 if anything is unconverted
    python tools/port_resources_1211.py --root DIR --target PATH ...   other checkout / other resource roots (relative to root)

Only the Python standard library. Java is never touched: see docs of the port lane for the code paths that read the renamed folders.

What it does (sources: minecraft.wiki Pack_format, Java_Edition_1.20.3 / 1.20.5 / 1.21, 24w19a, 24w21a):
  1. pack.mcmeta: pack_format 15 -> 34 for resource packs (assets/ only), 48 for data packs (data/ only);
     a pack with both gets 34 plus supported_formats {min_inclusive 34, max_inclusive 48}.
  2. data folders to the singular registry names (1.21): structures -> structure, functions -> function, advancements,
     recipes, loot_tables, predicates, item_modifiers, tags/{blocks,items,entity_types,fluids,game_events,functions}.
     Mod-own folders (gg_props, cast_tip, sectionmaps, ...) are not vanilla registries and stay. Files are moved into an existing
     singular folder when there is one (the incoming file wins), so a re-copy of old data over a converted tree is converted again.
  3. .stk packed section files (tools/sectionmap.py): palette block ids renamed minecraft:grass -> minecraft:short_grass (1.20.3).
     Only the palette tag is spliced; the rest of the bytes are kept. Vanilla structure .nbt files are NOT edited: the game's
     DataFixer upgrades them when they are read through StructureTemplateManager (they carry DataVersion); the script only reports DataVersions.
  4. .mcfunction scripts: setblock banner NBT {Patterns:[{Pattern:"bs",Color:7}]} -> {patterns:[{pattern:"minecraft:stripe_bottom",color:"gray"}]}
     (1.20.5), plain sign text '{"text":"x"}' -> "x" (1.20.3), block id minecraft:grass -> minecraft:short_grass.
  5. fabric.mod.json: minecraft 1.20.1 -> 1.21.1, java >=17 -> >=21; *.mixins.json compatibilityLevel JAVA_17 -> JAVA_21.
Left alone on purpose (no change needed in 1.21.1): item/block model json incl. overrides with custom_model_data predicates (the items/
definitions only arrive in 1.21.4), sounds.json, lang, font, blockstates, damage_type json, tags/damage_type, dimension json, OptiFine .jem.
"""
import argparse
import gzip
import os
import re
import struct
import sys
from pathlib import Path

RESOURCE_PACK_FORMAT = 34
DATA_PACK_FORMAT = 48
DEFAULT_TARGETS = ('src/main/resources', 'test-tools/resources')

# data/<namespace>/<old> -> <new>, 1.21 (24w19a: tags/*, 24w21a: the rest)
DATA_RENAMES = {
    'structures': 'structure', 'functions': 'function', 'advancements': 'advancement', 'recipes': 'recipe',
    'loot_tables': 'loot_table', 'predicates': 'predicate', 'item_modifiers': 'item_modifier',
    'tags/blocks': 'tags/block', 'tags/items': 'tags/item', 'tags/entity_types': 'tags/entity_type',
    'tags/fluids': 'tags/fluid', 'tags/game_events': 'tags/game_event', 'tags/functions': 'tags/function',
    'decorated_pot_patterns': 'decorated_pot_pattern',
}
BLOCK_RENAMES = {'minecraft:grass': 'minecraft:short_grass'}  # 1.20.3
# legacy banner pattern codes -> 1.20.5+ registry ids; dye colour ids -> names
BANNER_PATTERNS = {
    'b': 'base', 'bl': 'square_bottom_left', 'br': 'square_bottom_right', 'tl': 'square_top_left', 'tr': 'square_top_right',
    'bs': 'stripe_bottom', 'ts': 'stripe_top', 'ls': 'stripe_left', 'rs': 'stripe_right', 'cs': 'stripe_center', 'ms': 'stripe_middle',
    'drs': 'stripe_downright', 'dls': 'stripe_downleft', 'ss': 'small_stripes', 'cr': 'cross', 'sc': 'straight_cross',
    'bt': 'triangle_bottom', 'tt': 'triangle_top', 'bts': 'triangles_bottom', 'tts': 'triangles_top', 'ld': 'diagonal_left',
    'rd': 'diagonal_up_right', 'lud': 'diagonal_up_left', 'rud': 'diagonal_right', 'mc': 'circle', 'mr': 'rhombus',
    'vh': 'half_vertical', 'hh': 'half_horizontal', 'vhr': 'half_vertical_right', 'hhb': 'half_horizontal_bottom',
    'bo': 'border', 'cbo': 'curly_border', 'gra': 'gradient', 'gru': 'gradient_up', 'bri': 'bricks', 'glb': 'globe',
    'cre': 'creeper', 'sku': 'skull', 'flo': 'flower', 'moj': 'mojang', 'pig': 'piglin', 'flw': 'flow', 'gus': 'guster',
}
DYES = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue',
        'brown', 'green', 'red', 'black']
SKIP_DIRS = {'build', '.git', '.gradle', '__pycache__'}


class Report:
    def __init__(self, check):
        self.check = check
        self.changes = {}   # stage -> list of messages (things converted, or that would be)
        self.warnings = []  # things the script cannot convert (count as unconverted) 
        self.notes = []     # information only

    def change(self, stage, msg):
        self.changes.setdefault(stage, []).append(msg)

    def warn(self, msg):
        self.warnings.append(msg)

    def note(self, msg):
        self.notes.append(msg)


def walk_files(root):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for f in filenames:
            yield Path(dirpath) / f


def rel(path, root):
    return Path(path).relative_to(root).as_posix()


# ---------------------------------------------------------------- 1. pack.mcmeta
def stage_packs(root, targets, rep):
    for t in targets:
        for p in walk_files(t):
            if p.name != 'pack.mcmeta':
                continue
            has_assets, has_data = (p.parent / 'assets').is_dir(), (p.parent / 'data').is_dir()
            if has_assets and has_data:
                want, both = RESOURCE_PACK_FORMAT, True
            elif has_data:
                want, both = DATA_PACK_FORMAT, False
            elif has_assets:
                want, both = RESOURCE_PACK_FORMAT, False
            else:
                rep.warn(f'{rel(p, root)}: pack has neither assets/ nor data/, pack_format not set')
                continue
            text = p.read_text(encoding='utf-8')
            new = re.sub(r'("pack_format"\s*:\s*)\d+', lambda m: f'{m.group(1)}{want}', text, count=1)
            if both and '"supported_formats"' not in new:
                new = re.sub(r'("pack_format"\s*:\s*\d+)',
                             lambda m: m.group(1) + f', "supported_formats": {{"min_inclusive": {RESOURCE_PACK_FORMAT}, "max_inclusive": {DATA_PACK_FORMAT}}}',
                             new, count=1)
            if '"pack_format"' not in text:
                rep.warn(f'{rel(p, root)}: no pack_format field')
            elif new != text:
                rep.change('pack.mcmeta', f'{rel(p, root)}: pack_format -> {want}')
                if not rep.check:
                    p.write_text(new, encoding='utf-8', newline='')


# ---------------------------------------------------------------- 2. folder renames
def move_tree(src, dst, rep, root):
    """Moves src into dst (created or merged); an incoming file replaces an existing one. Returns the number of files moved."""
    n = 0
    for f in sorted(walk_files(src)):
        target = dst / f.relative_to(src)
        n += 1
        if rep.check:
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        os.replace(f, target)
    if not rep.check:
        for dirpath, dirnames, filenames in os.walk(src, topdown=False):
            try:
                os.rmdir(dirpath)
            except OSError:
                pass
    return n


def stage_renames(root, targets, rep):
    for t in targets:
        data_dirs = []
        for dirpath, dirnames, _ in os.walk(t):
            dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS and d != 'assets']
            if Path(dirpath).name == 'data':
                data_dirs.append(Path(dirpath))
                dirnames[:] = [d for d in dirnames]  # namespaces are handled below; no need to look inside for another data/
        for data in data_dirs:
            for ns in sorted(p for p in data.iterdir() if p.is_dir()):
                # longest (tags/*) first is not needed: they are distinct paths; do tags before plain names for readability
                for old, new in sorted(DATA_RENAMES.items(), key=lambda kv: -kv[0].count('/')):
                    src, dst = ns / old, ns / new
                    if src.is_dir():
                        n = move_tree(src, dst, rep, root)
                        rep.change('folder renames', f'{rel(src, root)} -> {new} ({n} files)')


# ---------------------------------------------------------------- NBT helpers (stdlib; skipping reader, no full parse)
FIXED = {1: 1, 2: 2, 3: 4, 4: 8, 5: 4, 6: 8}


def skip(b, t, p):
    if t in FIXED:
        return p + FIXED[t]
    if t == 7:
        return p + 4 + struct.unpack_from('>i', b, p)[0]
    if t == 8:
        return p + 2 + struct.unpack_from('>H', b, p)[0]
    if t == 9:
        it = b[p]
        n = struct.unpack_from('>i', b, p + 1)[0]
        p += 5
        if it in FIXED:
            return p + n * FIXED[it]
        for _ in range(max(n, 0)):
            p = skip(b, it, p)
        return p
    if t == 10:
        while b[p] != 0:
            tt = b[p]
            nl = struct.unpack_from('>H', b, p + 1)[0]
            p = skip(b, tt, p + 3 + nl)
        return p + 1
    if t == 11:
        return p + 4 + 4 * struct.unpack_from('>i', b, p)[0]
    if t == 12:
        return p + 4 + 8 * struct.unpack_from('>i', b, p)[0]
    raise ValueError(f'bad nbt tag type {t}')


def top_tags(b):
    """Yields (name, type, payload_start, payload_end) for the tags of the root compound of uncompressed NBT bytes."""
    if b[0] != 10:
        raise ValueError('root is not a compound')
    p = 3 + struct.unpack_from('>H', b, 1)[0]
    while b[p] != 0:
        tt = b[p]
        nl = struct.unpack_from('>H', b, p + 1)[0]
        name = b[p + 3:p + 3 + nl].decode('utf-8')
        start = p + 3 + nl
        end = skip(b, tt, start)
        yield name, tt, start, end
        p = end


def read_gz(path):
    return gzip.decompress(Path(path).read_bytes())


def write_gz(path, data):
    Path(path).write_bytes(gzip.compress(data, compresslevel=9, mtime=0))


def is_data_structure_dir(path):
    parts = path.parts
    return 'structures' in parts or 'structure' in parts


def rename_block(name):
    base, br, props = name.partition('[')
    return BLOCK_RENAMES[base] + br + props if base in BLOCK_RENAMES else None


def palette_edits(b, s):
    """The (start, end, new bytes) splices that rename the block ids of the palette list whose payload starts at s. Entries are
    compounds with a Name string (the .stk layout) or plain strings."""
    it = b[s]
    n = struct.unpack_from('>i', b, s + 1)[0]
    q = s + 5
    edits = []

    def enc(x):
        raw = x.encode('utf-8')
        return struct.pack('>H', len(raw)) + raw

    for _ in range(max(n, 0)):
        if it == 8:
            ln = struct.unpack_from('>H', b, q)[0]
            new = rename_block(b[q + 2:q + 2 + ln].decode('utf-8'))
            if new:
                edits.append((q, q + 2 + ln, enc(new)))
            q += 2 + ln
        elif it == 10:
            end = skip(b, 10, q)
            r = q
            while b[r] != 0:
                tt = b[r]
                nl = struct.unpack_from('>H', b, r + 1)[0]
                name = b[r + 3:r + 3 + nl]
                ps = r + 3 + nl
                pe = skip(b, tt, ps)
                if name == b'Name' and tt == 8:
                    new = rename_block(b[ps + 2:pe].decode('utf-8'))
                    if new:
                        edits.append((ps, pe, enc(new)))
                r = pe
            q = end
        else:
            return []
    return edits


# ---------------------------------------------------------------- 3. packed .stk palettes + DataVersion report
def stage_stk(root, targets, rep):
    versions = {}
    for t in targets:
        for p in walk_files(t):
            if p.suffix not in ('.stk', '.nbt'):
                continue
            try:
                b = read_gz(p)
                tags = {name: (tt, s, e) for name, tt, s, e in top_tags(b)}
            except Exception as e:  # noqa: BLE001
                rep.warn(f'{rel(p, root)}: unreadable nbt ({e})')
                continue
            if 'stack' not in tags and 'blocks' in tags:
                # vanilla structure template: the game's DataFixer upgrades it on load
                dv = struct.unpack_from('>i', b, tags['DataVersion'][1])[0] if 'DataVersion' in tags and tags['DataVersion'][0] == 3 else None
                versions.setdefault(dv, []).append(rel(p, root))
                continue
            pal = tags.get('palette')
            if 'stack' not in tags or pal is None or pal[0] != 9:
                continue  # not a packed stack file
            edits = palette_edits(b, pal[1])
            if not edits:
                continue
            rep.change('packed .stk palettes', f'{rel(p, root)}: {len(edits)} palette entries renamed (grass -> short_grass)')
            if not rep.check:
                out, last = [], 0
                for s0, e0, new in edits:
                    out += [b[last:s0], new]
                    last = e0
                out.append(b[last:])
                write_gz(p, b''.join(out))
    if versions:
        summary = ', '.join(f"{'MISSING' if k is None else k}: {len(v)}" for k, v in sorted(versions.items(), key=lambda kv: (kv[0] is None, kv[0])))
        rep.note(f'vanilla structure .nbt DataVersion histogram ({summary}); 1.20.1 = 3465, 1.21.1 = 3955; '
                 f'the game upgrades older ones through DataFixer when they are read via StructureTemplateManager.createTemplate/readStructure')
        if None in versions:
            rep.warn('structure .nbt files without DataVersion would be read as version 500: ' + ', '.join(versions[None][:5]))


# ---------------------------------------------------------------- 4. .mcfunction
PATTERNS_RE = re.compile(r'Patterns:\[((?:\{[^{}\]]*\},?)*)\]')
PATTERN_ITEM_RE = re.compile(r'\{([^{}]*)\}')
SIGN_RE = re.compile(r"'\{\"text\":\"((?:[^\"\\]|\\.)*)\"\}'")
GRASS_RE = re.compile(r'(?<![\w:])minecraft:grass(?![\w/.])')


def convert_patterns(m, problems):
    items = []
    for im in PATTERN_ITEM_RE.finditer(m.group(1)):
        body = im.group(1)
        pm, cm = re.search(r'Pattern:"(\w+)"', body), re.search(r'Color:(\d+)', body)
        if not pm or not cm or pm.group(1) not in BANNER_PATTERNS or int(cm.group(1)) >= 16:
            problems.append(m.group(0)[:80])
            return m.group(0)
        items.append(f'{{pattern:"minecraft:{BANNER_PATTERNS[pm.group(1)]}",color:"{DYES[int(cm.group(1))]}"}}')
    return 'patterns:[' + ','.join(items) + ']'


def convert_function(text, problems):
    text = PATTERNS_RE.sub(lambda m: convert_patterns(m, problems), text)
    text = SIGN_RE.sub(lambda m: '"' + m.group(1) + '"', text)
    text = GRASS_RE.sub('minecraft:short_grass', text)
    if re.search(r'\bPatterns:|\bPattern:"', text):
        problems.append('Patterns/Pattern left in a form the script does not parse')
    if re.search(r"'\{\"text\"", text):
        problems.append('sign/JSON text component with more than {"text":"..."}')
    return text


def stage_functions(root, targets, rep):
    for t in targets:
        for p in walk_files(t):
            if p.suffix != '.mcfunction':
                continue
            raw = p.read_bytes()
            text = raw.decode('utf-8')
            problems = []
            new = convert_function(text, problems)
            for pr in problems:
                rep.warn(f'{rel(p, root)}: unconverted: {pr}')
            if new != text:
                rep.change('mcfunction', f'{rel(p, root)}: banner/sign/grass syntax updated')
                if not rep.check:
                    p.write_bytes(new.encode('utf-8'))


# ---------------------------------------------------------------- 5. fabric.mod.json + mixin configs
def stage_metadata(root, targets, rep):
    for t in targets:
        for p in walk_files(t):
            if p.name != 'fabric.mod.json' and not p.name.endswith('.mixins.json'):
                continue
            text = p.read_text(encoding='utf-8')
            new = text
            if p.name == 'fabric.mod.json':
                new = re.sub(r'("minecraft"\s*:\s*)"1\.20\.1"', r'\1"1.21.1"', new)
                new = re.sub(r'("java"\s*:\s*)">=17"', r'\1">=21"', new)
            else:
                new = re.sub(r'("compatibilityLevel"\s*:\s*)"JAVA_17"', r'\1"JAVA_21"', new)
            if new != text:
                rep.change('mod metadata', f'{rel(p, root)}: minecraft/java/mixin level updated')
                if not rep.check:
                    p.write_bytes(new.encode('utf-8'))
            elif p.name == 'fabric.mod.json' and re.search(r'"fabric-api"|"geckolib"', text):
                rep.note(f'{rel(p, root)}: fabric-api / geckolib version ranges are not touched; confirm they have 1.21.1 builds')


# ---------------------------------------------------------------- main
STAGES = (stage_packs, stage_renames, stage_stk, stage_functions, stage_metadata)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('--check', action='store_true', help='change nothing; exit 1 if anything is unconverted')
    ap.add_argument('--root', default=str(Path(__file__).resolve().parent.parent), help='checkout root (default: this script\'s repo)')
    ap.add_argument('--target', action='append', help='resource root relative to --root (repeatable; default src/main/resources and test-tools/resources)')
    args = ap.parse_args(argv)
    root = Path(args.root).resolve()
    targets = [root / t for t in (args.target or DEFAULT_TARGETS)]
    targets = [t for t in targets if t.is_dir()]
    if not targets:
        print('no resource roots found', file=sys.stderr)
        return 2
    rep = Report(args.check)
    for stage in STAGES:
        stage(root, targets, rep)
    verb = 'would change' if args.check else 'changed'
    total = 0
    for stage, msgs in rep.changes.items():
        total += len(msgs)
        print(f'[{stage}] {verb} {len(msgs)}')
        for m in msgs[:12]:
            print('   ', m)
        if len(msgs) > 12:
            print(f'    ... and {len(msgs) - 12} more')
    for n in rep.notes:
        print('note:', n)
    for w in rep.warnings:
        print('UNCONVERTED:', w)
    print(f'{"check" if args.check else "convert"}: {total} item(s) {verb}, {len(rep.warnings)} unconvertible')
    if args.check:
        return 1 if (total or rep.warnings) else 0
    return 1 if rep.warnings else 0


if __name__ == '__main__':
    sys.exit(main())
