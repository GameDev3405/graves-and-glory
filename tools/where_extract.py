"""Local "where is it" overlay extractor.

The AST graph (graphify-out/graph.json) only knows code calls. Game links live in data and ids: sheets/design.json rows, art paths, asset names,
Spec fields. This script finds those links by real lookup (rg, file listing, parsing design.json / Spec.java / Audio.java, python imports) and
writes a SEPARATE overlay, work/where/overlay.json, that tools/where.py reads. It never touches graphify's graph.json and calls no model.

Covers: every troop in design.json `troops` (sheet row, Spec row, art, model, both texture folders, animation builders and their imports,
clips, family attackVariants, sound route), every id of every other design.json section that is a list of rows with an `id` (sheet row, Spec
row, every place the id appears), and every key of the `rules` row (Java and Python readers). Ids under 3 characters, and non-troop ids that
match more than MAX_ID_HITS places (block, shield, main, ...), are skipped and listed in meta.skipped; where.py says so when asked for one.
Run: python tools/where_extract.py        (about 10 s; land.py runs it after the graph refresh, a lane runs it once by hand)
"""
import argparse, collections, json, os, pathlib, re, shutil, subprocess, sys, time

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / 'work' / 'where' / 'overlay.json'
DESIGN = 'sheets/design.json'
SPEC = 'src/main/java/gg/gravesandglory/Spec.java'
AUDIO = 'src/main/java/gg/gravesandglory/Audio.java'
CUES = 'src/main/java/gg/gravesandglory/AudioCues.java'
SOUNDS = 'src/main/resources/assets/gravesandglory/sounds.json'
SKIP = ['!tools/where*.py', '!test-tools/standalone/test_where.py', '!design-archive', '!graphify-out', '!work', '!docs/archive', '!.git', '!*.min.js']
MAX_JAVA_PER_KEY = 400
MAX_ID_HITS = 200          # a non-troop id with more text matches than this is too generic to link
MIN_ID_LEN = 3
PER_ID_CAP = 30            # nodes kept per id (and 3 lines per file); troops get PER_TROOP_CAP
PER_TROOP_CAP = 120
TEXT_EXCLUDE = ['!design-archive', '!graphify-out', '!work', '!docs/archive', '!.git', '!*.min.js', '!**/out/**', '!tools/where*.py', '!test-tools/standalone/test_where.py']
_LINES = {}


def line_text(path, line):
    if path not in _LINES:
        try: _LINES[path] = rd(path).splitlines()
        except OSError: _LINES[path] = []
    ls = _LINES[path]
    return ls[line - 1] if 0 < line <= len(ls) else ''


def rd(rel):
    return (ROOT / rel).read_text(encoding='utf-8', errors='replace')


def rg(pattern, paths=('.',), globs=(), extra=()):
    """[(path, line, text)] for a PCRE pattern; paths with forward slashes, relative to ROOT."""
    cmd = ['rg', '-n', '-P', '--no-heading', '--color', 'never', '--with-filename', '--no-messages']
    for g in list(SKIP) + list(globs):
        cmd += ['-g', g]
    cmd += list(extra) + ['-e', pattern, '--', *paths]
    res = subprocess.run(cmd, cwd=ROOT, capture_output=True, encoding='utf-8', errors='replace')
    hits = []
    for ln in res.stdout.splitlines():
        m = re.match(r'(.+?):(\d+):(.*)$', ln)
        if m:
            hits.append((m.group(1).replace('\\', '/').lstrip('./'), int(m.group(2)), m.group(3)))
    return hits


def files():
    res = subprocess.run(['git', 'ls-files', '--cached', '--others', '--exclude-standard'], cwd=ROOT, capture_output=True, encoding='utf-8')
    return [f for f in res.stdout.splitlines()
            if not f.startswith(('design-archive/', 'graphify-out/', 'work/', 'docs/archive/', '.git/')) and (ROOT / f).is_file()]


def kind_of(p):
    name = p.rsplit('/', 1)[-1]
    if p.startswith('sheets/'): return 'data'
    if p.startswith(('test-tools/', 'tests/')) or name.startswith('test_'): return 'test'
    if p.startswith('docs/'): return 'doc'
    if name.startswith('Audio') or p.startswith('tools/audio/') or name == 'sounds.json' or '/sounds/' in p: return 'sound'
    if p.startswith(('art/anim', 'art/clips', 'tools/anim', 'tools/clips')) or any(s in p for s in ('/animations/', '/troop_rig/', '/clips/')): return 'anim'
    if p.startswith('art/') or any(s in p for s in ('/textures/', '/geo/', '/models/')): return 'art'
    return 'code'


def snippet(text, pat, width=96):
    t = text.strip()
    if len(t) <= width: return t
    m = re.search(pat, text)
    s = max(0, (m.start() if m else 0) - width // 3)
    return ('...' if s else '') + text[s:s + width].strip() + '...'


class Overlay:
    def __init__(self):
        self.nodes, self.edges, self.keys = [], [], {}

    def loc(self, kind, path, line, desc, secondary=False):
        k = (path, line, desc)
        if k not in self.keys:
            self.keys[k] = len(self.nodes)
            n = {'id': len(self.nodes), 'kind': kind, 'path': path, 'line': line, 'desc': desc}
            if secondary: n['sec'] = 1
            self.nodes.append(n)
        return self.keys[k]

    def ent(self, etype, name, kind, desc, aliases=(), path='', line=0):
        k = ('ent', etype, name)
        if k not in self.keys:
            self.keys[k] = len(self.nodes)
            n = {'id': len(self.nodes), 'kind': kind, 'ent': etype, 'label': name, 'alias': list(aliases), 'path': path, 'line': line, 'desc': desc}
            self.nodes.append(n)
        return self.keys[k]

    def link(self, a, b, rel):
        if a != b and (a, b, rel) not in self.keys:
            self.keys[(a, b, rel)] = 1
            self.edges.append([a, b, rel])


class Design:
    def __init__(self):
        self.text = rd(DESIGN)
        self.lines = self.text.splitlines()
        self.data = json.loads(self.text)
        self.idx = [None] * (len(self.lines) + 2)
        sec = rid = None
        rline = 0
        for i, l in enumerate(self.lines, 1):
            m = re.match(r'  "(\w+)": \[', l)
            if m: sec, rid = m.group(1), None
            m = re.match(r'      "id": "([^"]*)"', l)
            if m: rid, rline = m.group(1), i
            self.idx[i] = (sec, rid, rline)
        self.rows = {(s, r['id']): r for s, v in self.data.items() if isinstance(v, list) for r in v if isinstance(r, dict) and 'id' in r}
        self.rowline = {}
        self.pos = {}
        for s, v in self.data.items():
            if isinstance(v, list):
                for n, r in enumerate(v):
                    if isinstance(r, dict) and 'id' in r: self.pos[(s, r['id'])] = n
        for i in range(1, len(self.lines) + 1):
            sec, rid, rl = self.idx[i]
            if rid is not None and rl == i: self.rowline[(sec, rid)] = i

    def describe(self, sec, rid):
        r = self.rows.get((sec, rid), {})
        bits = []
        if sec != 'troops' and r.get('label') and not r.get('name'): bits.append(str(r['label']))
        if r.get('name'): bits.append(str(r['name']))
        if sec == 'troops': bits += [f"house {r.get('house')}", f"family {r.get('family')}"]
        elif r.get('troop'): bits.append(f"troop {r['troop']}")
        elif r.get('text'): bits.append(str(r['text'])[:50])
        return ', '.join(bits)


def spec_tables():
    """{line: (TABLE, section)} and {section: record line} for the generated Spec.java."""
    sections = {}
    tables, records = {}, {}
    for i, l in enumerate(rd(SPEC).splitlines(), 1):
        m = re.match(r'public static (\w+)Row\[\] (\w+) =', l)
        if m: tables[i] = (m.group(2), m.group(1))
        m = re.match(r'public record (\w+)Row\(([^)]*)\)', l)
        if m: records[m.group(1)] = (i, [c.split()[-1] for c in m.group(2).split(',')])
    return tables, records


def spec_table_for(tables, section):
    """(line, TABLE_NAME) of the generated Spec table for a design.json section, or None (record XRow <- section x)."""
    rec = section[0].upper() + section[1:]
    for line, (name, record) in tables.items():
        if record == rec: return line, name
    return None


def row_ent(ov, dz, sec, rid):
    """The entity for one design.json row (section, id), with its sheet row loc linked."""
    r = dz.rows[(sec, rid)]
    rl = dz.rowline[(sec, rid)]
    d = dz.describe(sec, rid)
    e = ov.ent(sec, rid, 'data', f'{sec} {rid}' + (f': {d}' if d else ''), [str(r['name'])] if r.get('name') else [], DESIGN, rl)
    ov.link(e, ov.loc('data', DESIGN, rl, f'design.json {sec} row {rid}' + (f': {d}' if d else '')), 'design_row')
    return e


# ---------------------------------------------------------------- troops
def audio_arm(row):
    """Mirror of Audio.arm(): which sound family a troop's weapon puts it in."""
    w = row.get('weapon') or ''
    armed = bool(w) and not w.endswith(':air')
    if row.get('magic'): return 'MAGIC'
    if row.get('ranged'): return 'BOW' if armed else 'NONE'
    if not armed: return 'HEAVY' if row['id'] == 'ic_colossus' else 'NONE'
    return 'HEAVY' if w.endswith(('axe', 'mace', 'hammer')) else 'BLADE'


def sound_links(ov, ent, row, tid):
    audio = rd(AUDIO).splitlines()
    arm = audio_arm(row)
    arm_line = next((i for i, l in enumerate(audio, 1) if 'public static Arm arm(' in l), 0)
    a = ov.loc('sound', AUDIO, arm_line, f"Audio.arm(): {tid} weapon {row.get('weapon')} -> Arm.{arm} (picks its combat sounds)")
    ov.link(ent, a, 'sound_arm')
    cues = []
    # shotCue: case "id" -> Cue.X
    for i, l in enumerate(audio, 1):
        m = re.search(r'case "%s" -> AudioCues\.Cue\.(\w+)' % re.escape(tid), l)
        if m: cues.append((m.group(1), i))
    # meleeCue: if (cond) return Cue.X; evaluated against the row
    if arm == 'BLADE':
        start = next((i for i, l in enumerate(audio, 1) if 'static AudioCues.Cue meleeCue(' in l), 0)
        for i in range(start, start + 8):
            l = audio[i - 1]
            m = re.search(r'return AudioCues\.Cue\.(\w+);', l)
            if not m: continue
            cond = re.search(r'if \((.*)\) return', l)
            if cond is None or meleeCond(cond.group(1), row): cues.append((m.group(1), i));
            if cond is not None and meleeCond(cond.group(1), row): break
    # playHit: lines carrying Arm.<arm> and the cues just after them
    for i, l in enumerate(audio, 1):
        if f'Arm.{arm}' in l and 'AudioCues.Cue.' in ''.join(audio[i - 1:i + 4]) and i > arm_line + 20:
            for j in range(i, i + 4):
                for c in re.findall(r'AudioCues\.Cue\.(\w+)', audio[j - 1]): cues.append((c, j))
            if f'arm == Arm.{arm}' in l and arm == 'BLADE': pass
    if 'shield' in (row.get('tags') or ''):
        for i, l in enumerate(audio, 1):
            if 'shieldRaised' in l and 'Cue.SHIELD_BLOCK' in l: cues.append(('SHIELD_BLOCK', i)); break
    cuefile = rd(CUES).splitlines()
    sounds = json.loads(rd(SOUNDS))
    snl = rd(SOUNDS).splitlines()
    seen = set()
    for c, line in cues:
        if (c, line) in seen: continue
        seen.add((c, line))
        la = ov.loc('sound', AUDIO, line, f"plays Cue.{c} for {arm}-arm troops ({tid}): {audio[line - 1].strip()[:70]}")
        ov.link(ent, la, 'sound_cue_use')
        cl = next((i for i, l in enumerate(cuefile, 1) if re.match(r'\s*%s\("([^"]+)"' % c, l)), 0)
        m = re.match(r'\s*%s\("([^"]+)"' % c, cuefile[cl - 1]) if cl else None
        event = m.group(1) if m else c.lower()
        cent = ov.ent('cue', c, 'sound', f"AudioCues.Cue.{c}: sound event {event}", [event, c.replace('_', ' ')], CUES, cl)
        ov.link(ent, cent, 'plays')
        ov.link(cent, ov.loc('sound', AUDIO, line, f"plays Cue.{c} for {arm}-arm troops ({tid}): {audio[line - 1].strip()[:70]}"), 'used_at')
        if event in sounds:
            n = len(sounds[event].get('sounds', []))
            sl = next((i for i, l in enumerate(snl, 1) if l.startswith(f'  "{event}"')), 0)
            sn = ov.loc('sound', SOUNDS, sl, f'sounds.json event {event}: {n} audio variants under assets/gravesandglory/sounds/')
            ov.link(cent, sn, 'sounds_json'); ov.link(ent, sn, 'sounds_json')


def meleeCond(cond, row):
    """Evaluate the few condition shapes meleeCue uses: id equals, family equals, weapon endsWith."""
    parts = re.split(r'\|\|', cond.strip().strip('()'))
    for p in parts:
        p = p.strip().strip('()')
        m = re.search(r'u\.spec\.id\(\)\.equals\("([^"]+)"\)', p)
        if m and row['id'] == m.group(1): return True
        m = re.search(r'"([^"]+)"\.equals\(u\.spec\.family\(\)\)', p)
        if m and row.get('family') == m.group(1): return True
        m = re.search(r'weapon\.endsWith\("([^"]+)"\)', p)
        if m and (row.get('weapon') or '').endswith(m.group(1)): return True
    return False


def clip_names(ov, ent, relpath, tid):
    text = rd(relpath).splitlines()
    names = []
    inside = False
    for i, l in enumerate(text, 1):
        if '"animations"' in l: inside = True; continue
        m = re.match(r'  "(\w+)": \{', l) if inside else None
        if m: names.append((m.group(1), i))
    return names


TEX = 'src/main/resources/assets/gravesandglory/textures/entity/troop/'


def troop_rig_entries():
    """{troop id: (line, [strings of its troop_rig.py TROOPS entry])}, read once (art dir, builder module, Troop attr, weapon module, weapon id)."""
    out = {}
    try: lines = rd('art/anim/common/troop_rig.py').splitlines()
    except OSError: return out
    for i, l in enumerate(lines, 1):
        m = re.match(r'\s*"(\w+)": \(', l)
        if m: out[m.group(1)] = (i, re.findall(r'"([^"]+)"', l))
    return out


def builder_imports(allfiles):
    """{py file: [(line, imported art file, module)]}: imports between the art/ python builders, resolved by module stem (a sys.path hint breaks ties)."""
    pys = [f for f in allfiles if f.startswith('art/') and f.endswith('.py') and '/out/' not in f]
    stem = collections.defaultdict(list)
    for f in pys: stem[f.rsplit('/', 1)[-1][:-3]].append(f)
    res = {}
    for f in pys:
        try: lines = rd(f).splitlines()
        except OSError: continue
        hint = {w for l in lines if 'sys.path' in l for w in re.findall(r'["\'](\w+)["\']', l)}
        for i, l in enumerate(lines, 1):
            m = re.match(r'\s*(?:import\s+(\w+)|from\s+(\w+)\s+import)', l)
            mod = m and (m.group(1) or m.group(2))
            c = [g for g in stem.get(mod, []) if g != f] if mod else []
            if len(c) > 1: c = [g for g in c if g.split('/')[-2] in hint or g.split('/')[-2] == f.split('/')[-2]]
            if len(c) == 1: res.setdefault(f, []).append((i, c[0], mod))
    return res


def troop_builders(ov, ent, tid, fam, hit_paths, allfiles, rigent, bidx):
    """Animation/art builders for a troop: python files that name it, its own art/anim/<id>/ scripts, the family builder (build_<family piece>_anims.py),
    then the art imports of each (the axe, spear and archer builders import the knight builder)."""
    found = {}
    pieces = [w for w in (fam or '').split('_') if len(w) >= 3 and w != 'shield']
    for f in allfiles:
        if not f.endswith('.py') or '/out/' in f: continue
        name = f.rsplit('/', 1)[-1]
        if f in hit_paths and f.startswith(('art/anim/', 'art/hand/', 'art/clips/')):
            found.setdefault(f, 'is the animation builder that names this troop' if f.startswith('art/anim/') and not f.startswith('art/anim/common/') and name.startswith('build_') else 'names this troop')
        elif f.startswith((f'art/anim/{tid}/', f'art/hand/{tid}/')): found.setdefault(f, "is this troop's own script")
        elif pieces and f.startswith('art/anim/') and re.fullmatch(r'build_(\w+_)?anims\.py', name) and (
                any(re.search(r'(^|_)%s(_|$)' % re.escape(w), name) for w in pieces) or f.split('/')[2] in pieces):
            found.setdefault(f, f'family builder for {fam}')
    if rigent:
        for mod in rigent[1][1:5]:
            for f in allfiles:
                if f.endswith('/' + mod + '.py') and f.startswith('art/') and '/out/' not in f: found.setdefault(f, 'named in the troop_rig.py entry' if f.startswith('art/anim/') else 'art builder named in the troop_rig.py entry')
    for f, why in sorted(found.items()):
        line = next((i for i, l in enumerate(rd(f).splitlines(), 1) if re.search(r'(?<![A-Za-z0-9])%s(?![A-Za-z0-9])' % re.escape(tid), l)), 1)
        ov.link(ent, ov.loc('anim' if f.startswith(('art/anim', 'art/clips')) else 'art', f, line, f'builder script ({why}) for {tid}'), 'builder' if why.startswith(('is ', 'family', 'named')) else 'builder_ref')
    seen, todo = set(found), [f for f, why in found.items() if why.startswith(('is ', 'family', 'named'))]
    for _ in range(1):   # one level: what each builder imports directly
        nxt = []
        for f in todo:
            for line, target, mod in bidx.get(f, []):
                n = ov.loc('anim', f, line, f'{f.rsplit("/", 1)[-1]} imports {target} (import {mod}): its clips and rig helpers come from there')
                ov.link(ent, n, 'builder_import')
                if target not in seen: seen.add(target); nxt.append(target)
        todo = nxt


def troop(ov, dz, tid, allfiles, tables, thits, rig, bidx):
    row = dz.rows[('troops', tid)]
    fam = row.get('family')
    ent = ov.ent('troop', tid, 'data', f'troop {tid} "{row["name"]}": house {row["house"]}, {row.get("group")}, family {fam}, weapon {row.get("weapon")}, ability {row.get("ability")}',
                 [row['name']], DESIGN, dz.rowline[('troops', tid)])
    pat = r'(?<![A-Za-z0-9])%s(?![A-Za-z0-9])' % re.escape(tid)
    # ---- every text hit (one shared rg pass over all troop ids)
    design_rows, spec_hits, other = {}, [], {}
    for path, line in thits.get(tid, []):
        text = line_text(path, line)
        if path == DESIGN:
            sec, rid, rline = dz.idx[line]
            key = re.match(r'\s*"(\w+)"', text)
            design_rows.setdefault((sec, rid, rline), set()).add(key.group(1) if key else '')
        elif path == SPEC:
            spec_hits.append((line, text))
        else:
            other.setdefault(path, []).append((line, text))
    for (sec, rid, rline), keys in sorted(design_rows.items(), key=lambda x: x[0][2]):
        extra = '' if keys <= {'id'} else ' (' + ','.join(sorted(k for k in keys if k)) + ')'
        d = dz.describe(sec, rid)
        own = rid == tid or rid.startswith(tid + '_') or dz.rows.get((sec, rid), {}).get('troop') == tid
        ov.link(ent, ov.loc('data', DESIGN, rline, f'design.json {sec} row {rid}{extra}' + (f': {d}' if d else '')), 'design_row' if own else 'design_ref')
    for line, text in spec_hits:
        name = tables[line][0] if line in tables else 'Spec'
        ov.link(ent, ov.loc('code', SPEC, line, f'Spec.{name} generated row (from design.json {name.lower()}): ' + snippet(text, pat, 70)), 'spec_row')
    budget = PER_TROOP_CAP
    for path, hs in sorted(other.items()):
        k = kind_of(path)
        for line, text in hs[:6 if k in ('code', 'test') else 2]:
            if budget <= 0: break
            budget -= 1
            ov.link(ent, ov.loc(k, path, line, snippet(text, pat)), 'mentions')
    # ---- texture folders: legacy <id>/, GeckoLib <id>_geo/, and the painted art/troops/<id>_s*.png skins
    explicit = set()
    for suffix, rel, label in (('', 'texture_dir', 'legacy'), ('_geo', 'texture_dir_geo', 'GeckoLib')):
        d = f'{TEX}{tid}{suffix}/'
        fs = sorted(f for f in allfiles if f.startswith(d))
        if fs:
            explicit.add(d.rstrip('/'))
            ov.link(ent, ov.loc('art', d, 0, f'{label} troop textures: {len(fs)} files ({fs[0].rsplit("/", 1)[-1]} ... {fs[-1].rsplit("/", 1)[-1]})'), rel)
    skin_re = r'art/troops/%s_s\d.*\.png' % re.escape(tid)
    skins = sorted(f for f in allfiles if re.fullmatch(skin_re, f))
    if skins:
        ov.link(ent, ov.loc('art', 'art/troops/', 0, f'{len(skins)} painted skin pngs: {skins[0].rsplit("/", 1)[-1]} ... {skins[-1].rsplit("/", 1)[-1]}'), 'texture_art')
    # ---- every other file whose path carries the id (models, rigs, animations, art json); directories collapse
    bydir = {}
    for f in allfiles:
        if re.search(pat, f) and not (f.startswith('docs/') and f.endswith(('.png', '.jpg'))) and f.rsplit('/', 1)[0] not in explicit and not re.fullmatch(skin_re, f):
            bydir.setdefault(f.rsplit('/', 1)[0], []).append(f)
    for d, fs in sorted(bydir.items()):
        k = kind_of(fs[0])
        fs.sort()
        if (len(fs) > 4 or '/out/' in d + '/') and not any(f.endswith('.json') and '/out/' not in f for f in fs):
            ext = sorted({f.rsplit('.', 1)[-1] for f in fs})
            ov.link(ent, ov.loc(k, d + '/', 0, f'{len(fs)} files ({",".join(ext)}): {fs[0].rsplit("/", 1)[-1]} ... {fs[-1].rsplit("/", 1)[-1]}'), 'files')
        else:
            pngs = [f for f in fs if f.endswith(('.png', '.jpg'))]
            if len(pngs) > 1:
                ov.link(ent, ov.loc(k, d + '/', 0, f'{len(pngs)} images: {pngs[0].rsplit("/", 1)[-1]} ... {pngs[-1].rsplit("/", 1)[-1]}'), 'files')
                fs = [f for f in fs if f not in pngs]
            for f in fs:
                if f.endswith('.animation.json') and '/animations/troop/' in f: continue   # the animation_json node below describes it with its clip names
                ov.link(ent, ov.loc(k, f, 1, describe_file(f)), 'model' if f.endswith(('.geo.json', '.bbmodel')) or '/models/' in f else 'file')
    # ---- troop_rig.py entry (art dir, builder module, weapon module, weapon id), weapon art json, builder scripts and their imports
    rigent = rig.get(tid)
    if rigent:
        mods = rigent[1]
        ov.link(ent, ov.loc('anim', 'art/anim/common/troop_rig.py', rigent[0], f'troop_rig.py TROOPS entry for {tid}: art dir {mods[0]}, builder {mods[1] if len(mods) > 1 else ""}, weapon {mods[4] if len(mods) > 4 else ""}'), 'rig_entry')
        if len(mods) >= 5:
            for f in allfiles:
                if re.fullmatch(r'art/weapons/%s(_t\d)?\.json' % re.escape(mods[4]), f):
                    ov.link(ent, ov.loc('art', f, 1, f'weapon art json for {mods[4]} (the weapon {tid} holds)'), 'weapon')
    troop_builders(ov, ent, tid, fam, {p for p, _ in thits.get(tid, [])}, allfiles, rigent, bidx)
    # ---- animation file + clip names
    for f in allfiles:
        if re.fullmatch(r'src/main/resources/assets/gravesandglory/animations/troop/%s\.animation\.json' % re.escape(tid), f):
            names = clip_names(ov, ent, f, tid)
            fn = ov.loc('anim', f, 1, f'exported animation json: {len(names)} clips; attack clips: ' + ', '.join(n for n, _ in names if n.startswith('attack_') and not n.endswith('_legs')))
            ov.link(ent, fn, 'animation_json')
            for n, line in names:
                cn = ov.ent('clip', n, 'anim', f'clip {n} of {tid}', [], f, line)
                ov.nodes[cn].setdefault('troops', []).append(tid)
                ov.link(fn, cn, 'clip')
                ov.link(cn, ov.loc('anim', f, line, f'clip {n} in {tid} animation json', True), 'at')
    # ---- family: clip library, attack variants, family->troop map
    if fam and ('ent', 'family', fam) in ov.keys:
        ov.link(ent, ov.keys[('ent', 'family', fam)], 'family')
    elif fam:
        fent = ov.ent('family', fam, 'anim', f'weapon family {fam}: attack clips and variants', [], '', 0)
        ov.link(ent, fent, 'family')
        for (sec, rid), r in dz.rows.items():
            if r.get('family') == fam and sec == 'attackVariants':
                rl = dz.rowline[(sec, rid)]
                ov.link(fent, ov.loc('data', DESIGN, rl, f'design.json {sec} row {rid} for family {fam}: {r.get("kind", "")} impact tick {r.get("impactTicks", "")}'), 'attack_variant')
        for path, line, text in rg(r"(\(\s*'[a-z]+',\s*'%s'\s*\)|'%s':\s*\(|^def %s\()" % (fam, fam, fam), ['art/clips', 'tools', 'art/anim'], ['*.py']):
            ov.link(fent, ov.loc(kind_of(path), path, line, f'family {fam}: ' + snippet(text, fam, 80)), 'family_code')
        famfiles = [f for f in allfiles if kind_of(f) == 'anim' and (f'/{fam}/' in f or f.rsplit('/', 1)[-1].rsplit('.', 1)[0] == fam) and '/out/' not in f]
        bd = {}
        for f in famfiles: bd.setdefault(f.rsplit('/', 1)[0], []).append(f)
        for d, fs in bd.items():
            if len(fs) > 1: ov.link(fent, ov.loc('anim', d + '/', 0, f'{len(fs)} {fam} family clip files'), 'family_clips')
            else: ov.link(fent, ov.loc('anim', fs[0], 1, f'{fam} family clip file'), 'family_clips')
    # ---- linked rows: ability, house
    for col, sec in (('ability', 'abilities'), ('house', 'houses')):
        v = row.get(col)
        if v and (sec, v) in dz.rows: ov.link(ent, row_ent(ov, dz, sec, v), col)
    sound_links(ov, ent, row, tid)
    return ent


def describe_file(f):
    n = f.rsplit('/', 1)[-1]
    for a, b in (('.geo.json', 'GeckoLib geo model'), ('.animation.json', 'exported animation json'), ('.rig.json', 'rig json'), ('.png', 'texture/image'),
                 ('.bbmodel', 'Blockbench project'), ('.md', 'doc'), ('.py', 'script')):
        if n.endswith(a): kind = b; break
    else: kind = 'json' if n.endswith('.json') else 'file'
    if '/models/' in f: kind = 'legacy model json'
    if '/troop_rig/' in f: kind = 'troop rig rules json (renderer reads it)'
    if f.startswith('art/troops/') and n.endswith('.json'): kind = 'troop art json (bones, skins, weapon)'
    if f.startswith('art/hand/'): kind = 'hand-built art builder'
    return f'{kind}: {n}'


def loaders(ov, ent_by_kind):
    """Java lines that load each resource directory a troop's files live in (real lookup by the directory string)."""
    seen = {}
    for e, fs in ent_by_kind.items():
        for f in fs:
            m = re.search(r'assets/gravesandglory/(.+)/[^/]+$', f)
            if m: seen.setdefault(m.group(1), []).append(e)
    for frag, ents in seen.items():
        for path, line, text in rg(r'"%s/' % re.escape(frag), ['src/main/java'], ['*.java']):
            n = ov.loc('code', path, line, f'loads {frag}/: ' + snippet(text, re.escape(frag), 80))
            for e in ents: ov.link(e, n, 'loaded_by')


# ---------------------------------------------------------------- rules keys
def rules(ov, dz, tables, records):
    rec_line, comps = records['Rules']
    rules_row = dz.data['rules'][0]
    table_line = next(k for k, v in tables.items() if v[1] == 'Rules')
    # which record types declare a component name: a name used by more than one record needs the receiver alias
    owners = {}
    for r, (_, cs) in records.items():
        for c in cs: owners.setdefault(c, set()).add(r)
    java = [f for f in rg(r'\.\w+\(\)', ['src', 'test-tools'], ['*.java'], ['-l'])]
    # aliases of a RulesRow: locals/fields typed Spec.RulesRow, Class.R when the class holds one, Spec.RULES[i]
    alias, cls_alias = {}, {}
    for path, line, text in rg(r'(?:Spec\.)?RulesRow\s+(\w+)\s*=|var\s+(\w+)\s*=\s*Spec\.RULES\[', ['src', 'test-tools'], ['*.java']):
        m = re.search(r'RulesRow\s+(\w+)\s*=|var\s+(\w+)\s*=\s*Spec\.RULES', text)
        nm = m.group(1) or m.group(2)
        alias.setdefault(path, set()).add(nm)
        if 'static' in text and 'private' not in text: cls_alias[path.rsplit('/', 1)[-1][:-5]] = nm
    # python files that bind a name to the rules row
    for key in rules_row:
        if key == 'id': continue
        line_in_design = next((i for i in range(1, len(dz.lines)) if dz.idx[i][0] == 'rules' and re.match(r'\s*"%s":' % key, dz.lines[i - 1])), 0)
        ent = ov.ent('rule', key, 'data', f'rules.{key} = {rules_row[key]} (design.json rules row "{rules_row["id"]}", generated into Spec.RulesRow.{key})',
                     [f'rules.{key}', f'Spec.RULES.{key}', re.sub(r'(?<!^)(?=[A-Z])', ' ', key).lower()], DESIGN, line_in_design)
        ov.link(ent, ov.loc('data', DESIGN, line_in_design, f'design.json rules."{key}": {json.dumps(rules_row[key])}'), 'design_key')
        ov.link(ent, ov.loc('code', SPEC, rec_line, f'Spec.RulesRow record component `{key}` (generated by tools/preflight.py from design.json)'), 'spec_field')
        ov.link(ent, ov.loc('code', SPEC, table_line, f'Spec.RULES row values (generated); accessor Spec.RULES[0].{key}()'), 'spec_value')
        # Java readers
        reads = []
        uniq = len(owners.get(key, ())) <= 1
        for path, line, text in rg(r'\.%s\(\)' % key, ['src', 'test-tools'], ['*.java']):
            if path == SPEC: continue
            code = text.split('//')[0]
            if not re.search(r'\.%s\(\)' % key, code): continue
            if not uniq:
                recv = alias.get(path, set()) | {'Spec.RULES\\[[^\\]]*\\]'} | {c + '.' + a for c, a in cls_alias.items()}
                if not any(re.search(r'(?<![\w.])(%s)\.%s\(\)' % (a, key), code) or re.search(r'(%s)\.%s\(\)' % (a, key), code) for a in recv): continue
            reads.append((path, line, text))
        for path, line, text in reads[:MAX_JAVA_PER_KEY]:
            ov.link(ent, ov.loc('test' if kind_of(path) == 'test' else 'code', path, line, f'reads rules.{key}: ' + snippet(text, r'\.%s\(\)' % key, 90)), 'java_reader')
        # comments / docs that name it
        for path, line, text in rg(r'rules\.%s\b' % key, ['src', 'docs', 'tools', 'test-tools'], ['*.java', '*.md', '*.py']):
            if re.search(r'\.%s\(\)' % key, text): continue
            ov.link(ent, ov.loc(kind_of(path) if kind_of(path) != 'code' else 'code', path, line, f'mentions rules.{key}: ' + snippet(text, r'rules\.%s' % key, 90)), 'mention')
        # Python tools reading the key from the sheet: same-line ['rules'][0] reads, plus aliases bound to the row
        pys = {}
        for path, line, text in rg(r'''['"]rules['"]\]\s*\[0\]''', ['tools', 'test-tools', 'tests', 'art'], ['*.py']):
            m = re.match(r'\s*(\w+)\s*=\s*[\w\[\]\'". ]*[\'"]rules[\'"]\]\s*\[0\]\s*(;|$)', text) or re.match(r'.*?\b(\w+)\s*=\s*\w+\[[\'"]rules[\'"]\]\[0\]', text)
            if m: pys.setdefault(path, set()).add(m.group(1))
            if re.search(r'\b%s\b' % key, text):
                ov.link(ent, ov.loc('test' if kind_of(path) == 'test' else 'code', path, line, f'python reads rules[0].{key}: ' + snippet(text, key, 90)), 'python_reader')
        for path, names in pys.items():
            for i, l in enumerate(rd(path).splitlines(), 1):
                if any(re.search(r'\b%s\b\s*(\[|\.update|\.get)' % n, l) for n in names) and re.search(r'\b%s\b' % key, l) and 'rules' not in l:
                    ov.link(ent, ov.loc('code', path, i, f'python reads rules row alias.{key}: ' + snippet(l, key, 90)), 'python_reader')
        # python code that writes Java text calling the accessor
        for path, line, text in rg(r'\.%s\(\)' % key, ['tools'], ['*.py']):
            ov.link(ent, ov.loc('code', path, line, f'generates Java that reads {key}(): ' + snippet(text, r'\.%s\(\)' % key, 90)), 'java_text')


# ---------------------------------------------------------------- audio drift guard
# audio_arm() and meleeCue() above mirror Audio.java. These are the statements the mirror was written against; if Audio.java's arm() or
# meleeCue() changes shape, audio_shape_problems() says so (the extractor warns, test_where fails) and the mirror must be updated.
ARM_SHAPE = ['if (u == null || u.spec == null) return Arm.NONE;', 'String weapon = u.spec.weapon();', 'boolean armed = weapon != null && !weapon.endsWith(":air");',
             'if (u.spec.magic()) return Arm.MAGIC;', 'if (u.spec.ranged()) return armed ? Arm.BOW : Arm.NONE;',
             'if (!armed) return u.spec.id().equals("ic_colossus") ? Arm.HEAVY : Arm.NONE;',
             'return weapon.endsWith("axe") || weapon.endsWith("mace") || weapon.endsWith("hammer") ? Arm.HEAVY : Arm.BLADE;']
MELEE_SHAPE = ['if (u.spec.id().equals("wm_tusk")) return AudioCues.Cue.TUSK_ATTACK;', 'String weapon = u.spec.weapon();',
               'if ("spear".equals(u.spec.family()) || (weapon != null && (weapon.endsWith("trident") || weapon.endsWith("spear")))) return AudioCues.Cue.SPEAR_THRUST;',
               'return AudioCues.Cue.SWORD_SWING;']


def java_body(text, signature):
    """Statements of the method whose declaration contains `signature`, comments stripped, whitespace normalised ([] when not found)."""
    i = text.find(signature)
    if i < 0: return []
    j = text.index('{', i)
    depth, k = 0, j
    while k < len(text):
        if text[k] == '{': depth += 1
        elif text[k] == '}':
            depth -= 1
            if depth == 0: break
        k += 1
    body = re.sub(r'/\*.*?\*/|//[^\n]*', '', text[j + 1:k], flags=re.S)
    return [re.sub(r'\s+', ' ', l).strip() for l in body.splitlines() if l.strip()]


def audio_shape_problems():
    """[] when Audio.java's arm() and meleeCue() still have the shape the Python mirror encodes, else one message per method that changed."""
    text = rd(AUDIO)
    out = []
    for sig, want in (('public static Arm arm(', ARM_SHAPE), ('static AudioCues.Cue meleeCue(', MELEE_SHAPE)):
        got = java_body(text, sig)
        if got != want:
            out.append(f'{AUDIO}: {sig}...) changed shape; update where_extract.audio_arm()/sound_links() and ARM_SHAPE/MELEE_SHAPE (now {len(got)} statements, mirror expects {len(want)})')
    return out


# ---------------------------------------------------------------- one rg pass over every sheet id
def text_hits(troop_ids, other_ids):
    """({troop id: [(path, line)]}, {other id: [(path, line)]}) from two rg passes over the repo text (fixed-string word pass; troop pass allows an _ neighbour)."""
    base = ['rg', '-n', '-o', '--no-heading', '--color', 'never', '--no-messages', '--max-filesize', '2M']
    for g in TEXT_EXCLUDE: base += ['-g', g]

    def run(args):
        res = subprocess.run(base + args + ['--', '.'], cwd=ROOT, capture_output=True, encoding='utf-8', errors='replace')
        for ln in res.stdout.splitlines():
            m = re.match(r'(.+?):(\d+):(.*)$', ln)
            if m: yield m.group(1).replace('\\', '/').lstrip('./'), int(m.group(2)), m.group(3)

    th, oh = collections.defaultdict(list), collections.defaultdict(list)
    seen = set()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    idfile = OUT.parent / 'ids.txt'
    idfile.write_text('\n'.join(sorted(other_ids)), encoding='utf-8')
    for path, line, m in run(['-F', '-w', '-f', str(idfile)]):
        if (m, path, line) not in seen:
            seen.add((m, path, line)); oh[m].append((path, line))
    alt = '|'.join(sorted(map(re.escape, troop_ids), key=len, reverse=True))
    for path, line, m in run(['-P', '-e', r'(?<![A-Za-z0-9])(?:%s)(?![A-Za-z0-9])' % alt]):
        if (m, path, line) not in seen:
            seen.add((m, path, line)); th[m].append((path, line))
    return th, oh


KIND_ORDER = {'code': 0, 'test': 1, 'data': 2, 'anim': 3, 'art': 4, 'sound': 5, 'doc': 6}


def id_links(ov, dz, tables, sec, rid, hits, troop_ent=None):
    """One non-troop sheet id: sheet row, Spec constant and row, then every place the id string appears (capped).
    A row whose id is a troop id (troopNotes) hangs on that troop's entity: only its sheet row and Spec row are added."""
    if troop_ent is not None:
        ov.link(troop_ent, ov.loc('data', DESIGN, dz.rowline[(sec, rid)], f'design.json {sec} row {rid}'), 'design_row')
        tbl = spec_table_for(tables, sec)
        if tbl and re.search(r'new \w+Row\("%s"' % re.escape(rid), line_text(SPEC, tbl[0])):
            ov.link(troop_ent, ov.loc('code', SPEC, tbl[0], f'Spec.{tbl[1]}[{dz.pos[(sec, rid)]}] generated row for {rid} (record {sec[0].upper() + sec[1:]}Row, from design.json {sec})'), 'spec_row')
        return troop_ent
    e = row_ent(ov, dz, sec, rid)
    pat = r'(?<![A-Za-z0-9])%s(?![A-Za-z0-9])' % re.escape(rid)
    own_line = dz.rowline[(sec, rid)]
    tbl = spec_table_for(tables, sec)
    spec_line = tbl[0] if tbl else 0
    if tbl and re.search(r'new \w+Row\("%s"' % re.escape(rid), line_text(SPEC, spec_line)):
        ov.link(e, ov.loc('code', SPEC, spec_line, f'Spec.{tbl[1]}[{dz.pos[(sec, rid)]}] generated row for {rid} (record {sec[0].upper() + sec[1:]}Row, from design.json {sec})'), 'spec_row')
    ov.nodes[e]['desc'] += f' ({len(hits)} text matches in the repo)'
    budget = PER_ID_CAP
    perfile = collections.Counter()
    for path, line in sorted(hits, key=lambda h: (KIND_ORDER.get(kind_of(h[0]), 9), h[0], h[1])):
        if budget <= 0: break
        if path == DESIGN:
            s2, r2, rl2 = dz.idx[line]
            if rl2 == own_line and s2 == sec: continue
            key = re.match(r'\s*"(\w+)"', line_text(path, line))
            ov.link(e, ov.loc('data', DESIGN, rl2, f'design.json {s2} row {r2} references it ({key.group(1) if key else "?"})'), 'design_ref')
        elif path == SPEC:
            if line == spec_line or line not in tables: continue
            ov.link(e, ov.loc('code', SPEC, line, f'Spec.{tables[line][0]} generated row mentions {rid}'), 'spec_ref')
        else:
            if perfile[path] >= 3: continue
            perfile[path] += 1
            k = kind_of(path)
            ov.link(e, ov.loc(k, path, line, f'mentions {rid}: ' + snippet(line_text(path, line), pat, 90)), 'mentions')
        budget -= 1
    return e


def thin_aliases(ov):
    """A name shared by several entities, shorter than 4 characters, or equal to another entity's id is no alias (the id stays the lookup key)."""
    nz = lambda a: re.sub(r'[^a-z0-9]', '', a.lower())
    cnt = collections.Counter(nz(a) for n in ov.nodes if 'ent' in n for a in set(n['alias']))
    labels = {nz(n['label']) for n in ov.nodes if 'ent' in n}
    for n in ov.nodes:
        if 'ent' in n:
            n['alias'] = [a for a in n['alias'] if a and len(a) >= 4 and cnt[nz(a)] == 1 and nz(a) not in labels]


def main():
    ap = argparse.ArgumentParser(description='Build work/where/overlay.json (every troop, every design-sheet id, the rules keys)')
    ap.parse_args()
    if shutil.which('rg') is None:
        print('where_extract: ripgrep (rg) is not on PATH; overlay not built', file=sys.stderr)
        return 2
    t0 = time.time()
    dz, allf = Design(), files()
    tables, records = spec_tables()
    ov = Overlay()
    troop_ids = [r['id'] for r in dz.data['troops']]
    tset = set(troop_ids)
    all_ids = {rid for (sec, rid) in dz.rows}
    other = sorted(i for i in all_ids - tset if len(i) >= MIN_ID_LEN)
    th, oh = text_hits(troop_ids, other)
    t1 = time.time()
    rig, bidx = troop_rig_entries(), builder_imports(allf)
    ents = {}
    for tid in troop_ids:
        e = troop(ov, dz, tid, allf, tables, th, rig, bidx)
        ents[e] = [ov.nodes[t]['path'] for s_, t, r in ov.edges if s_ == e and r in ('file', 'files', 'animation_json')]
    loaders(ov, ents)
    skipped, covered = {}, 0
    for (sec, rid), r in dz.rows.items():
        if sec == 'troops': continue
        if rid in tset:
            id_links(ov, dz, tables, sec, rid, [], ov.keys[('ent', 'troop', rid)])
            continue
        n = len(oh.get(rid, ()))
        if len(rid) < MIN_ID_LEN or n > MAX_ID_HITS:
            if rid not in skipped:
                skipped[rid] = {'why': f'id shorter than {MIN_ID_LEN} characters' if len(rid) < MIN_ID_LEN else f'{n} text matches (limit {MAX_ID_HITS})', 'rows': []}
            skipped[rid]['rows'].append([sec, DESIGN, dz.rowline[(sec, rid)]])
            continue
        id_links(ov, dz, tables, sec, rid, oh.get(rid, []))
        covered += 1
    rules(ov, dz, tables, records)
    thin_aliases(ov)
    problems = audio_shape_problems()
    for pr in problems: print('WARNING: ' + pr, file=sys.stderr)
    tmp = OUT.with_suffix('.tmp')
    tmp.write_text(json.dumps({'meta': {'built': time.strftime('%Y-%m-%d %H:%M'), 'troops': troop_ids, 'sections': sorted({s_ for s_, _ in dz.rows}), 'ids': covered,
                                        'skipped': skipped, 'audio_drift': problems}, 'nodes': ov.nodes, 'edges': ov.edges}, separators=(',', ':')), encoding='utf-8')
    os.replace(tmp, OUT)
    print(f'overlay: {len(ov.nodes)} nodes, {len(ov.edges)} edges, {len(troop_ids)} troops, {covered} sheet ids, {len(skipped)} generic ids skipped '
          f'-> {OUT.relative_to(ROOT)} ({OUT.stat().st_size // 1024} KB, rg pass {t1 - t0:.1f}s, total {time.time() - t0:.1f}s)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
