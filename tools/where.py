"""where: one command that answers "where is X / what does X link to" with a capped path:line list (trial prototype, not landed).

Order: (1) work/where/overlay.json (data/id links built by tools/where_extract.py), (2) main's AST graph (graphify-out/graph.json, read directly),
(3) rg fallback. Output is grouped data / code / art / anim / sound / test / doc.
Graph location: $WHERE_GRAPH, else <checkout>/graphify-out/graph.json, else the main worktree's (../bannerbound-mainmerge/graphify-out/graph.json).
"""
import argparse, collections, difflib, json, os, pathlib, re, subprocess, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
OVERLAY = ROOT / 'work' / 'where' / 'overlay.json'
KINDS = ['data', 'code', 'art', 'anim', 'sound', 'test', 'doc']
HINTS = {'data': 'data', 'sheet': 'data', 'design': 'data', 'json': None, 'code': 'code', 'java': 'code', 'python': 'code', 'art': 'art', 'texture': 'art', 'textures': 'art',
         'model': 'art', 'models': 'art', 'anim': 'anim', 'animation': 'anim', 'animations': 'anim', 'animate': 'anim', 'clip': 'anim', 'clips': 'anim', 'sound': 'sound',
         'sounds': 'sound', 'audio': 'sound', 'test': 'test', 'tests': 'test', 'fixture': 'test', 'fixtures': 'test', 'doc': 'doc', 'docs': 'doc'}
STOP = set('a an and are as at be by does do for from how in is it of on or that the this to what where which who why with used use uses read reads file files line lines '
           'find me show list all every'.split())
REL_RANK = {'design_key': 0, 'design_row': 0, 'design_ref': 2, 'spec_field': 1, 'spec_value': 1, 'spec_row': 1, 'file': 1, 'files': 3, 'animation_json': 1, 'attack_clips': 1, 'family_clips': 2, 'weapon': 3,
            'rig_module': 3, 'sound_arm': 3, 'plays': 4, 'sounds_json': 4, 'java_reader': 4, 'python_reader': 4, 'loaded_by': 5, 'attack_variant': 5, 'family_code': 2}


def norm(s):
    return re.sub(r'[^a-z0-9]', '', s.lower())


def out(s=''):
    sys.stdout.write(s + '\n')


def kind_of(p):
    name = p.rsplit('/', 1)[-1]
    if p.startswith('sheets/'): return 'data'
    if p.startswith(('test-tools/', 'tests/')) or name.startswith('test_'): return 'test'
    if p.startswith('docs/'): return 'doc'
    if name.startswith('Audio') or p.startswith('tools/audio/') or name == 'sounds.json' or '/sounds/' in p: return 'sound'
    if p.startswith(('art/anim', 'art/clips', 'tools/anim', 'tools/clips')) or any(s in p for s in ('/animations/', '/troop_rig/', '/clips/')): return 'anim'
    if p.startswith('art/') or any(s in p for s in ('/textures/', '/geo/', '/models/')): return 'art'
    return 'code'


def parse_query(q):
    words = re.findall(r'[A-Za-z0-9_]+', q)
    hints, toks = set(), []
    for w in words:
        lw = w.lower()
        if lw in HINTS and len(words) > 1:
            if HINTS[lw]: hints.add(HINTS[lw])
            continue
        if lw in STOP and len(words) > 1: continue
        toks.append(w)
    if not toks: toks = words
    return toks, hints


# ------------------------------------------------------------------ overlay
def overlay_lookup(q, toks, hints):
    if not OVERLAY.exists(): return None
    o = json.loads(OVERLAY.read_text(encoding='utf-8'))
    N, E = o['nodes'], o['edges']
    qn = norm(q)
    tn = [norm(t) for t in toks]
    ents = [n for n in N if 'ent' in n]
    scored = []
    for e in ents:
        names = {norm(e['label'])} | {norm(a) for a in e.get('alias', []) if a}
        names.discard('')
        sc = 0
        if qn in names: sc = 100
        elif any(t in names for t in tn): sc = 90
        elif any(len(nm) >= 5 and nm in qn for nm in names): sc = 70
        elif any(len(t) >= 6 and difflib.get_close_matches(t, list(names), 1, 0.88) for t in tn): sc = 60
        if sc: scored.append((sc, e))
    if not scored: return None
    best = max(s for s, _ in scored)
    # keep entities at the best tier; a clip entity is only wanted when it is what the query names
    chosen = [e for s, e in scored if s == best][:4]
    adj = collections.defaultdict(list)
    for a, b, r in E: adj[a].append((b, r))
    result = []
    for e in chosen:
        locs, linked = {}, []
        for b, r in adj[e['id']]:
            nb = N[b]
            if 'ent' in nb:
                if nb['ent'] in ('clip', 'rule'): continue
                linked.append(nb)
                for c, r2 in adj[b]:
                    if 'ent' not in N[c]: locs.setdefault(c, r2)
            else:
                locs.setdefault(b, r)
                if e['ent'] == 'troop' and r in ('animation_json',):
                    pass
        if e['ent'] == 'clip':
            for b, r in adj[e['id']]:
                if 'ent' not in N[b]: locs[b] = r
        # remaining query words boost matching locations
        rest = [t for t in toks if norm(t) not in {norm(e['label'])} | {norm(a) for a in e.get('alias', [])}]
        rows = []
        for i, r in locs.items():
            n = N[i]
            if hints and n['kind'] not in hints: continue
            if n.get('sec') and e['ent'] != 'clip': continue
            text = (n['path'] + ' ' + n['desc']).lower()
            boost = sum(1 for t in rest if t.lower() in text)
            rows.append((n['kind'], n['path'], n['line'], n['desc'], 2 if r == 'mentions' and n['kind'] in ('anim', 'art', 'sound') else REL_RANK.get(r, 6), -boost))
        result.append((e, linked, rows))
    return result


WEIGHT = {'data': 3, 'code': 3, 'art': 3, 'anim': 3, 'sound': 2, 'test': 2, 'doc': 1}


def group_key(kind, path, line, desc, rank):
    """Rows that are the same kind of fact collapse: lines of one source file; design.json rows of one sheet section. Distinct facts stay apart."""
    if kind == 'data': return (path, desc.split(' row ')[0])
    if kind in ('code', 'test', 'doc', 'art') or (kind == 'anim' and rank >= 2): return path
    return (path, line, desc)


def allocate(sizes, cap):
    """Spread cap over kinds by WEIGHT (at least 1 each while it fits), never more than a kind has; leftover goes to the heaviest kinds."""
    ks = list(sizes)
    quota = {k: 0 for k in ks}
    if cap >= len(ks):
        for k in ks: quota[k] = 1
    tw = sum(WEIGHT.get(k, 1) for k in ks) or 1
    for k in ks:
        quota[k] = min(sizes[k], max(quota[k], int(cap * WEIGHT.get(k, 1) / tw)))
    left = cap - sum(quota.values())
    order = sorted(ks, key=lambda k: -WEIGHT.get(k, 1))
    while left > 0 and any(quota[k] < sizes[k] for k in ks):
        for k in order:
            if left > 0 and quota[k] < sizes[k]: quota[k] += 1; left -= 1
    while sum(quota.values()) > cap:
        k = max((k for k in ks if quota[k] > 0), key=lambda k: quota[k]); quota[k] -= 1
    return quota


def grouped_print(rows, cap):
    """rows: (kind, path, line, desc, rank, boost). Returns (printed, total groups). Cap is spread across kinds."""
    by = collections.OrderedDict((k, collections.OrderedDict()) for k in KINDS)
    for kind, path, line, desc, rank, boost in sorted(rows, key=lambda r: (r[5], r[4], r[1], r[2])):
        by.setdefault(kind, collections.OrderedDict()).setdefault(group_key(kind, path, line, desc, rank), []).append((line, desc, rank, boost, path))
    by = {k: v for k, v in by.items() if v}
    if not by: return 0, 0
    quota = allocate({k: len(v) for k, v in by.items()}, cap)
    total = sum(len(v) for v in by.values())
    printed = 0
    for k, files in by.items():
        if not quota[k]: continue
        out(f'[{k}]')
        for key, hs in list(files.items())[:quota[k]]:
            hs.sort(key=lambda h: (h[2], h[0]))
            line, desc, path = hs[0][0], hs[0][1], hs[0][4]
            more = ''
            if len(hs) > 1:
                rest = sorted(h[0] for h in hs[1:])
                what = 'rows' if k == 'data' else 'more'
                more = '  (+%d %s: %s)' % (len(rest), what, ','.join(':%d' % l for l in rest[:4]) + (',...' if len(rest) > 4 else ''))
            out(f'  {path}:{line}  {desc}{more}'.replace(':0  ', '  '))
            printed += 1
    return printed, total


# ------------------------------------------------------------------ AST graph
def graph_path():
    for p in (os.environ.get('WHERE_GRAPH'), ROOT / 'graphify-out' / 'graph.json', ROOT.parent / 'bannerbound-mainmerge' / 'graphify-out' / 'graph.json'):
        if p and pathlib.Path(p).exists(): return pathlib.Path(p)
    return None


def build_index(gp):
    """Compact per-symbol rows from main's graph.json: [display name, norm name, file, line, is_file, file_type, calls, called_by, label, neighbours]."""
    g = json.loads(gp.read_text(encoding='utf-8'))
    nodes = {n['id']: n for n in g['nodes']}
    parent, deg_in, deg_out = {}, collections.Counter(), collections.Counter()
    nb = collections.defaultdict(list)
    for e in g['links']:
        s, t, r = e['source'], e['target'], e['relation']
        if r in ('contains', 'method'): parent[t] = s
        elif r in ('calls', 'indirect_call', 'references', 'uses', 'inherits', 'implements', 'imports_from', 'case_of'):
            deg_out[s] += 1; deg_in[t] += 1
            nb[s].append((t, 1)); nb[t].append((s, 0))

    def file_of(i):
        for _ in range(6):
            if i not in nodes: return ''
            f = nodes[i].get('source_file')
            if f: return f
            i = parent.get(i)
        return ''

    def name(i):
        l = nodes[i]['label']
        if l.startswith('.'):
            p = parent.get(i)
            c = nodes[p]['label'] if p in nodes and not nodes[p]['label'].endswith(('.java', '.py', '.js')) else ''
            return (c + l) if c else l[1:]
        return l

    rows = []
    for i, n in nodes.items():
        ft = n.get('file_type')
        if ft == 'concept': continue
        f = file_of(i)
        if not f or 'vendor' in f or f.endswith('.min.js'): continue
        label = n['label']
        isfile = label.endswith(('.java', '.py'))
        conn = []
        for t, outgoing in sorted(nb[i], key=lambda x: -(deg_in[x[0]] + deg_out[x[0]]))[:12]:
            if t in nodes and nodes[t].get('file_type') == 'code':
                nm = ('-> ' if outgoing else '<- ') + name(t)
                if nm not in conn: conn.append(nm)
            if len(conn) >= 4: break
        rows.append([name(i)[:100] if ft != 'rationale' else label[:90], norm(label.strip('.').rstrip('()')), f,
                     int(re.sub(r'\D', '', str(n.get('source_location') or 'L1')) or 1), int(isfile), ft, deg_out[i], deg_in[i], label if ft == 'rationale' else '', conn])
    return rows


def load_index():
    gp = graph_path()
    if not gp: return []
    cache = ROOT / 'work' / 'where' / 'ast-index.json'
    st = gp.stat()
    sig = [str(gp), st.st_size, int(st.st_mtime)]
    try:
        c = json.loads(cache.read_text(encoding='utf-8'))
        if c['sig'] == sig: return c['rows']
    except (OSError, ValueError, KeyError):
        pass
    rows = build_index(gp)
    try:
        cache.parent.mkdir(parents=True, exist_ok=True)
        cache.write_text(json.dumps({'sig': sig, 'rows': rows}, separators=(',', ':')), encoding='utf-8')
    except OSError:
        pass
    return rows


def ast_lookup(q, toks, hints, cap):
    rows_ = load_index()
    tn = [norm(t) for t in toks if len(norm(t)) >= 2]
    qn = norm(q)
    hits = []
    for r in rows_:
        name, ln, f, line, isfile, ft, calls, calledby, rat, conn = r
        sc = 0
        if ft == 'rationale':
            low = rat.lower()
            if len(toks) > 1 and all(t.lower() in low for t in toks): sc = 40
        else:
            stem = norm(f.rsplit('/', 1)[-1].rsplit('.', 1)[0])
            if ln and (ln == qn or ln in tn): sc = 98 if isfile else 100
            elif isfile and stem in tn: sc = 98
            elif any(len(t) >= 4 and ln.startswith(t) for t in tn): sc = 60
            elif any(len(t) >= 4 and t in ln for t in tn): sc = 50
            elif len(tn) == 1 and len(tn[0]) >= 4 and tn[0] in norm(f): sc = 30
        if sc: hits.append((sc, calls + calledby, r))
    if not hits and len(qn) >= 5:
        pool = {x[1]: x for x in rows_ if x[5] == 'code' and abs(len(x[1]) - len(qn)) <= 2}
        for m in difflib.get_close_matches(qn, list(pool), 8, 0.85):
            hits.append((45, 0, pool[m]))
    hits.sort(key=lambda h: (-h[0], -h[1]))
    res = []
    for k, (sc, d, r) in enumerate(hits[:cap * 3]):
        name, ln, f, line, isfile, ft, calls, calledby, rat, conn = r
        desc = rat if ft == 'rationale' else f"{name} ({'file' if isfile else 'symbol'}; calls {calls}, called by {calledby})"
        res.append((kind_of(f), f, line, desc, 7, 0, ', '.join(conn) if k < 3 else ''))
    return res


# ------------------------------------------------------------------ rg fallback
def rg_fallback(q, toks, cap):
    skip = ['!tools/where*.py', '!test-tools/standalone/test_where.py', '!design-archive', '!graphify-out', '!work', '!.git', '!docs/archive', '!*.min.js', '!*.png', '!*.ogg']
    pats = [re.escape(q)] if len(toks) > 1 else []
    long_toks = [re.escape(t) for t in toks if len(t) >= 4]
    if long_toks: pats.append('|'.join(long_toks) if len(toks) > 1 else long_toks[0])
    for pat in pats:
        cmd = ['rg', '-n', '-i', '--no-heading', '--color', 'never', '--no-messages', '-m', '2', '--max-columns', '400', '--max-columns-preview'] + [x for g in skip for x in ('-g', g)] + ['-e', pat, '--', '.']
        r = subprocess.run(cmd, cwd=ROOT, capture_output=True, encoding='utf-8', errors='replace')
        rows = []
        for ln in r.stdout.splitlines():
            m = re.match(r'(.+?):(\d+):(.*)$', ln)
            if not m: continue
            p = m.group(1).replace('\\', '/').lstrip('./')
            txt = m.group(3).strip()
            mm = re.search(pat, txt, re.I)
            s = max(0, (mm.start() if mm else 0) - 30)
            rows.append((kind_of(p), p, int(m.group(2)), ('rg: ' + (txt[s:s + 100])), 8, 0))
        if rows: return sorted(rows, key=lambda r: (r[1], r[2]))
    return []


def main():
    ap = argparse.ArgumentParser(prog='where.py', description='Find where a name, id or short phrase lives: path:line + one-line description, grouped by kind. '
                                 'Order: overlay (data/id links), main AST graph, rg fallback.')
    ap.add_argument('query', nargs='+', help='symbol, file, troop id, rules key or short phrase')
    ap.add_argument('--max', type=int, default=15, metavar='N', help='cap on printed lines (default 15)')
    a = ap.parse_args()
    if hasattr(sys.stdout, 'reconfigure'): sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    q = ' '.join(a.query)
    toks, hints = parse_query(q)
    cap = max(1, a.max)
    used = []
    ov = overlay_lookup(q, toks, hints)
    printed_total = 0
    if ov:
        used.append('overlay')
        share = max(1, cap // len(ov))
        for e, linked, rows in ov:
            out(f"== {e['label']} ({e['ent']}): {e['desc']}")
            p, t = grouped_print(rows, share if len(ov) > 1 else cap)
            printed_total += p
            if t > p: out(f'  (+{t - p} more files; --max {cap + (t - p)} or narrow the query)')
            conn = []
            for l in linked:
                if (l['ent'], l['label']) not in conn: conn.append((l['ent'], l['label']))
            if conn: out('connected to: ' + ', '.join(f'{k} {v}' for k, v in conn[:8]))
            elif rows:
                top = collections.Counter(r[1].rsplit('/', 1)[-1] for r in rows).most_common(5)
                out('connected to (files): ' + ', '.join(f'{n} x{c}' if c > 1 else n for n, c in top))
    if printed_total < max(3, cap // 2):
        rows = ast_lookup(q, toks, hints, cap)
        if hints: rows = [r for r in rows if r[0] in hints] or rows
        if rows:
            used.append('graph')
            out('== AST graph (main)')
            conns = {(r[1], r[2]): r[6] for r in rows if r[6]}
            p, t = grouped_print([r[:6] for r in rows], cap - printed_total)
            printed_total += p
            for (path, line), c in list(conns.items())[:3]:
                out(f'connected to [{path.rsplit("/", 1)[-1]}:{line}]: {c}')
    if printed_total == 0:
        rows = rg_fallback(q, toks, cap)
        if rows:
            used.append('rg')
            out('== no overlay/graph match; rg fallback')
            grouped_print(rows, cap)
        else:
            out(f'no match for "{q}" in overlay, graph or rg')


if __name__ == '__main__':
    main()
