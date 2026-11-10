"""Local "where is it" overlay extractor (trial prototype, not landed).

The AST graph (graphify-out/graph.json) only knows code calls. Game links live in data and ids: sheets/design.json rows, art paths, asset names,
Spec fields. This script finds those links by real lookup (rg, file listing, parsing design.json / Spec.java / Audio.java) and writes a SEPARATE
overlay, work/where/overlay.json, that tools/where.py reads. It never touches graphify's graph.json.

Sample covered: troops tc_raider and ic_hearthguard (every place each one lives), and every key of each design.json `rules` row (squadSize, ...).
Add a troop: python tools/where_extract.py --troop knight. Rebuild: python tools/where_extract.py
"""
import argparse, json, pathlib, re, subprocess, sys, time

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / 'work' / 'where' / 'overlay.json'
DESIGN = 'sheets/design.json'
SPEC = 'src/main/java/gg/gravesandglory/Spec.java'
AUDIO = 'src/main/java/gg/gravesandglory/Audio.java'
CUES = 'src/main/java/gg/gravesandglory/AudioCues.java'
SOUNDS = 'src/main/resources/assets/gravesandglory/sounds.json'
SKIP = ['!tools/where*.py', '!test-tools/standalone/test_where.py', '!design-archive', '!graphify-out', '!work', '!docs/archive', '!.git', '!*.min.js']
SAMPLE_TROOPS = ['tc_raider', 'ic_hearthguard']
MAX_JAVA_PER_KEY = 400


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

    def describe(self, sec, rid):
        r = self.rows.get((sec, rid), {})
        bits = []
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


def troop(ov, dz, tid, allfiles, tables):
    row = dz.rows[('troops', tid)]
    fam = row.get('family')
    ent = ov.ent('troop', tid, 'data', f'troop {tid} "{row["name"]}": house {row["house"]}, {row.get("group")}, family {fam}, weapon {row.get("weapon")}, ability {row.get("ability")}',
                 [row['name']], DESIGN, dz.idx and next(i for i in range(1, len(dz.lines)) if dz.idx[i] == ('troops', tid, i)))
    pat = r'(?<![A-Za-z0-9])%s(?![A-Za-z0-9])' % re.escape(tid)
    # ---- every text hit
    design_rows, spec_hits, other = {}, [], {}
    for path, line, text in rg(pat):
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
        tb = tables.get(max([k for k in tables if k <= line], default=0)) if line in tables else None
        name = tables[line][0] if line in tables else 'Spec'
        ov.link(ent, ov.loc('code', SPEC, line, f'Spec.{name} generated row (from design.json {name.lower()}): ' + snippet(text, pat, 70)), 'spec_row')
    for path, hs in sorted(other.items()):
        k = kind_of(path)
        for line, text in hs[:40 if k in ('code', 'test') else 3]:
            ov.link(ent, ov.loc(k, path, line, snippet(text, pat)), 'mentions')
    # ---- every file whose path carries the id (models, textures, rigs, animations, art json); directories collapse
    bydir = {}
    for f in allfiles:
        if re.search(pat, f) and not (f.startswith('docs/') and f.endswith(('.png', '.jpg'))): bydir.setdefault(f.rsplit('/', 1)[0], []).append(f)
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
                ov.link(ent, ov.loc(k, f, 1, describe_file(f)), 'file')
    # ---- troop_rig.py entry: art dir, builder module, weapon module, weapon id
    rig_hits = [h for h in rg(r'^\s*"%s": \(' % re.escape(tid), ['art/anim/common/troop_rig.py'])]
    for path, line, text in rig_hits:
        mods = re.findall(r'"([^"]+)"', text)
        for mod in mods[2:5]:
            for f in allfiles:
                if f.endswith('/' + mod + '.py') and f.startswith('art/'):
                    ov.link(ent, ov.loc('art', f, 1, f'{mod}.py, named in the troop_rig.py entry for {tid}'), 'rig_module')
        if len(mods) >= 5:
            wid = mods[4]
            for f in allfiles:
                if re.fullmatch(r'art/weapons/%s(_t\d)?\.json' % re.escape(wid), f):
                    ov.link(ent, ov.loc('art', f, 1, f'weapon art json for {wid} (the weapon {tid} holds)'), 'weapon')
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
    if fam:
        fent = ov.ent('family', fam, 'anim', f'weapon family {fam}: attack clips and variants', [], '', 0)
        ov.link(ent, fent, 'family')
        for (sec, rid), r in dz.rows.items():
            if r.get('family') == fam and sec != 'troops':
                rl = next(i for i in range(1, len(dz.lines)) if dz.idx[i] == (sec, rid, i))
                ov.link(fent, ov.loc('data', DESIGN, rl, f'design.json {sec} row {rid} for family {fam}: {r.get("kind", "")} impact tick {r.get("impactTicks", "")}'), 'attack_variant')
        for path, line, text in rg(r"(\(\s*'[a-z]+',\s*'%s'\s*\)|'%s':\s*\(|^def %s\()" % (fam, fam, fam), ['art/clips', 'tools', 'art/anim'], ['*.py']):
            ov.link(fent, ov.loc(kind_of(path), path, line, f'family {fam}: ' + snippet(text, fam, 80)), 'family_code')
        famfiles = [f for f in allfiles if kind_of(f) == 'anim' and (f'/{fam}/' in f or f.rsplit('/', 1)[-1].rsplit('.', 1)[0] == fam) and '/out/' not in f]
        bd = {}
        for f in famfiles: bd.setdefault(f.rsplit('/', 1)[0], []).append(f)
        for d, fs in bd.items():
            if len(fs) > 1: ov.link(fent, ov.loc('anim', d + '/', 0, f'{len(fs)} {fam} family clip files'), 'family_clips')
            else: ov.link(fent, ov.loc('anim', fs[0], 1, f'{fam} family clip file'), 'family_clips')
        ov.link(ent, fent, 'family')
    # ---- linked rows: ability, house
    for col, sec in (('ability', 'abilities'), ('house', 'houses')):
        v = row.get(col)
        if v and (sec, v) in dz.rows:
            rl = next(i for i in range(1, len(dz.lines)) if dz.idx[i] == (sec, v, i))
            e2 = ov.ent(col, v, 'data', f'{col} {v}: ' + dz.describe(sec, v), [dz.rows[(sec, v)].get('name', '')], DESIGN, rl)
            ov.link(ent, e2, col)
            same = [i for k, i in ov.keys.items() if len(k) == 3 and k[:2] == (DESIGN, rl) and isinstance(k[2], str)]
            ov.link(e2, same[0] if same else ov.loc('data', DESIGN, rl, f'design.json {sec} row {v}: ' + dz.describe(sec, v)), 'design_row')
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
                     [f'rules.{key}', f'Spec.RULES.{key}', 'squad size' if key == 'squadSize' else re.sub(r'(?<!^)(?=[A-Z])', ' ', key).lower()], DESIGN, line_in_design)
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


def main():
    ap = argparse.ArgumentParser(description='Build work/where/overlay.json')
    ap.add_argument('--troop', action='append', help='extra troop id to cover (default: tc_raider, ic_hearthguard)')
    a = ap.parse_args()
    t0 = time.time()
    dz, allf = Design(), files()
    tables, records = spec_tables()
    ov = Overlay()
    ids = SAMPLE_TROOPS + (a.troop or [])
    ents = {}
    for tid in ids:
        e = troop(ov, dz, tid, allf, tables)
        ents[e] = [ov.nodes[t]['path'] for s, t, r in ov.edges if s == e and r in ('file', 'files', 'animation_json')]
    loaders(ov, ents)
    rules(ov, dz, tables, records)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps({'meta': {'built': time.strftime('%Y-%m-%d %H:%M'), 'troops': ids, 'sample': 'rules row keys + troops'}, 'nodes': ov.nodes, 'edges': ov.edges},
                              separators=(',', ':')), encoding='utf-8')
    print(f'overlay: {len(ov.nodes)} nodes, {len(ov.edges)} edges -> {OUT.relative_to(ROOT)} ({OUT.stat().st_size // 1024} KB, {time.time() - t0:.1f}s)')


if __name__ == '__main__':
    sys.exit(main())
