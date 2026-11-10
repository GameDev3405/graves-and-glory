"""Generate "Kort's inbox": one private page of everything that waits on Kort, in priority order. Read-only except for --out.

    python tools/inbox.py [--status-dir D] [--repo R] [--out PATH] [--budget 60] [--json]

Sources: `- kort: <ask> | open: <link or none> | min: <minutes> | prio: <1|2|3>` lines in the lead status files
(status/*.md), the old `questions for Kort` value of a status file with no such line, and booth batches
(work/booth/<batch>/db/batch.json in any worktree) that have no verdict.json yet.
"""
import argparse, datetime, html, json, math, pathlib, re, subprocess, sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import state

ROOT = pathlib.Path(__file__).resolve().parents[1]
BOOTH_URL = 'https://claude.ai/artifact/GjNVhTyCYq93qi5igefgVE'
DEFAULT_MIN, DEFAULT_PRIO = 5, 2
PRIO_WORDS = {1: 'Now', 2: 'This week', 3: 'Whenever'}
LEAD_WORDS = {'anim': 'Animation', 'ui': 'UI', 'campaign-build': 'Campaign build'}

def asker_name(lead):
    """anim-lead -> Animation lead; campaign-build -> Campaign build."""
    base, is_lead = (lead[:-5], True) if lead.endswith('-lead') else (lead, False)
    name = LEAD_WORDS.get(base) or base.replace('-', ' ').replace('_', ' ').capitalize()
    return f'{name} lead' if is_lead else name

def make_item(kind, lead, ask, open=None, min=DEFAULT_MIN, prio=DEFAULT_PRIO, old_style=False, batch=None):
    return dict(kind=kind, lead=lead, asker=asker_name(lead), ask=ask, open=open, min=min, prio=prio, old_style=old_style, batch=batch)

FIELD_SPLIT = re.compile(r'\s*\|\s*(?=(?:open|min|prio)\s*:)', re.I)
KORT_LINE = re.compile(r'^\s*(?:[-*+]\s*)?kort\s*:\s*(.*)$', re.I)

def parse_kort_lines(text, lead):
    """Items from the `- kort: ask | open: x | min: n | prio: p` lines of one status file."""
    items = []
    for raw in text.splitlines():
        m = KORT_LINE.match(raw.replace('**', ''))
        if not m: continue
        ask, *rest = FIELD_SPLIT.split(m.group(1).strip())
        ask = ask.strip()
        if not ask: continue
        fields = {}
        for part in rest:
            k, _, v = part.partition(':'); fields.setdefault(k.strip().lower(), v.strip())
        open_ = fields.get('open', '')
        mins = re.match(r'\d+', fields.get('min', ''))
        prio = re.match(r'[123]\b', fields.get('prio', ''))
        items.append(make_item('question', lead, ask, None if state.is_none(open_) else open_,
                               int(mins.group()) if mins and int(mins.group()) > 0 else DEFAULT_MIN,
                               int(prio.group()) if prio else DEFAULT_PRIO))
    return items

ARTIFACT_LINK = re.compile(r'https://claude\.ai/artifact/[^\s)>\]"\'`]+')

def status_items(status_dir):
    items = []
    if not status_dir or not pathlib.Path(status_dir).is_dir(): return items
    for path in sorted(pathlib.Path(status_dir).glob('*.md')):
        if path.name.upper().startswith('DECISIONS-'): continue
        try: text = path.read_text(encoding='utf8', errors='replace')
        except OSError: continue
        new = parse_kort_lines(text, path.stem)
        if new: items += new; continue
        f = state.read_status(path)
        q = f.get('questions for kort') or f.get('questions', '')
        if state.is_none(q): continue
        link = ARTIFACT_LINK.search(q)
        items.append(make_item('question', path.stem, q, link.group().rstrip('.,;:!?') if link else None, old_style=True))
    return items

def already_in_target(repo, data):
    """A batch whose reviewed commit already reached its target branch (an old or test batch) no longer waits on Kort."""
    sha, target = str(data.get('submitted_sha') or ''), str(data.get('target') or '')
    if not re.fullmatch(r'[0-9a-f]{7,40}', sha) or not re.fullmatch(r'[\w./-]+', target): return False
    r = subprocess.run(['git', '-C', str(repo), 'merge-base', '--is-ancestor', sha, target], capture_output=True)
    return r.returncode == 0

def booth_items(repo):
    items, seen = [], set()
    try: trees = state.worktrees(repo)
    except Exception: return items
    for tree in trees:
        if tree['prunable']: continue
        for batch_json in sorted(pathlib.Path(tree['path']).glob('work/booth/*/db/batch.json')):
            try:
                folder = batch_json.parent.parent
                if (folder/'verdict.json').exists(): continue
                data = json.loads(batch_json.read_text(encoding='utf8'))
                if not isinstance(data, dict): continue
                clips = batch_json.parent/'clips'
                n = sum(1 for p in clips.iterdir() if p.is_file()) if clips.is_dir() else 0
                if not n and isinstance(data.get('clip_order'), list): n = len(data['clip_order'])
                if n <= 0: continue
                batch = str(data.get('batch') or folder.name)
                if batch in seen or already_in_target(repo, data): continue
                seen.add(batch)
                troop = re.sub(r'^[a-z]{2}_', '', str(data.get('troop') or folder.name.split('-')[0])).replace('_', ' ').capitalize()
                items.append(make_item('booth', 'anim-lead', f"Watch {troop}'s animations ({n} clip{'s' if n != 1 else ''}) and approve or reject each",
                                       BOOTH_URL, math.ceil(n * 0.5), 1, batch=batch))
            except (OSError, ValueError): continue
    return items

def collect(repo, status_dir):
    return booth_items(repo) + status_items(status_dir)

def order(items):
    return sorted(items, key=lambda i: (i['prio'], 0 if i['kind'] == 'booth' else 1, i['min'], i['asker'], i['ask']))

def split_today(items, budget):
    """(today, later): the longest prefix whose minutes fit the budget, never fewer than one item."""
    total, n = 0, 0
    for i in items:
        if n and total + i['min'] > budget: break
        total += i['min']; n += 1
    return items[:n], items[n:]

def minutes(items): return sum(i['min'] for i in items)

def summary(items, today, later):
    return f'{len(items)} items, {minutes(items)} min; today: {len(today)} items, {minutes(today)} min'

CSS = """
:root{--bg:#f6f3ee;--card:#fffdf9;--ink:#1f1b16;--muted:#6b6358;--line:#e2dccf;--accent:#7a2e22;--on-accent:#fff;--p1:#a8321f;--p2:#8a5a00;--p3:#4f6b4a}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#17140f;--card:#211d17;--ink:#f2ece1;--muted:#a79d8d;--line:#352f26;--accent:#e0a58f;--on-accent:#1f1b16;--p1:#ff9a85;--p2:#e8b95a;--p3:#9fc79a}}
:root[data-theme="dark"]{--bg:#17140f;--card:#211d17;--ink:#f2ece1;--muted:#a79d8d;--line:#352f26;--accent:#e0a58f;--on-accent:#1f1b16;--p1:#ff9a85;--p2:#e8b95a;--p3:#9fc79a}
*{box-sizing:border-box}
html,body{margin:0;overflow-x:hidden}
body{background:var(--bg);color:var(--ink);font:17px/1.45 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif;padding:24px 16px 48px}
main{max-width:720px;margin:0 auto}
h1{font-size:1.9rem;margin:0 0 4px}
.sub{color:var(--muted);margin:0 0 28px}
h2{font-size:1.15rem;margin:32px 0 12px;text-transform:uppercase;letter-spacing:.06em;color:var(--muted)}
.card{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:16px;margin:0 0 12px}
.ask{font-size:1.25rem;line-height:1.35;margin:0 0 10px;overflow-wrap:anywhere}
.meta{display:flex;flex-wrap:wrap;gap:6px 14px;align-items:center;color:var(--muted);font-size:.95rem}
.prio{font-weight:600}.prio-1{color:var(--p1)}.prio-2{color:var(--p2)}.prio-3{color:var(--p3)}
.open{display:inline-block;background:var(--accent);color:var(--on-accent);text-decoration:none;font-weight:600;padding:8px 18px;border-radius:8px}
.path{font-family:ui-monospace,Consolas,monospace;font-size:.85rem;overflow-wrap:anywhere}
.small{font-size:.8rem;color:var(--muted);margin-top:8px;overflow-wrap:anywhere}
.empty{font-size:1.3rem;margin:40px 0}
"""

def is_url(value): return bool(re.fullmatch(r'https?://[^\s"<>]+', value or '', re.I))

def card(i):
    e = html.escape
    parts = [f'<p class="ask">{e(i["ask"])}</p>', '<div class="meta">',
             f'<span class="who">{e(i["asker"])}</span>', f'<span>~{i["min"]} min</span>',
             f'<span class="prio prio-{i["prio"]}">{e(PRIO_WORDS.get(i["prio"], ""))}</span>']
    if is_url(i['open']): parts.append(f'<a class="open" href="{e(i["open"], quote=True)}">Open</a>')
    elif i['open']: parts.append(f'<span class="path">{e(i["open"])}</span>')
    parts.append('</div>')
    notes = ([f'batch {e(i["batch"])}'] if i.get('batch') else []) + (['old-style note'] if i.get('old_style') else [])
    if notes: parts.append(f'<div class="small">{" · ".join(notes)}</div>')
    return '<article class="card">' + ''.join(parts) + '</article>'

def render_html(items, today, later, now=None):
    now = now or datetime.datetime.now()
    body = [f'<h1>Kort\'s inbox</h1>', f'<p class="sub">Updated {now:%H:%M}. About {minutes(today)} minutes today.</p>']
    if not items: body.append('<p class="empty">Nothing waits on you.</p>')
    for title, group in (('Today', today), ('Later', later)):
        if group: body += [f'<h2>{title}</h2>'] + [card(i) for i in group]
    return ('<!doctype html>\n<html lang="en"><head><meta charset="utf-8">'
            '<meta name="viewport" content="width=device-width, initial-scale=1">'
            f"<title>Kort's inbox</title><style>{CSS}</style></head><body><main>" + '\n'.join(body) + '</main></body></html>\n')

def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    p.add_argument('--repo', default=str(ROOT)); p.add_argument('--status-dir', help='default: status/ beside the primary checkout')
    p.add_argument('--out', help='default: <repo>/work/inbox/index.html'); p.add_argument('--budget', type=int, default=60, help='minutes for "today"')
    p.add_argument('--json', action='store_true', help='print the item list as JSON (writes the page only with --out)')
    a = p.parse_args(argv)
    repo = pathlib.Path(a.repo)
    items = order(collect(repo, a.status_dir or state.default_status_dir(repo)))
    today, later = split_today(items, a.budget)
    out = pathlib.Path(a.out) if a.out else (None if a.json else pathlib.Path('work/inbox/index.html'))
    if out is not None:
        out = out if out.is_absolute() else repo/out
        out.parent.mkdir(parents=True, exist_ok=True); out.write_text(render_html(items, today, later), encoding='utf8', newline='\n')
    if a.json:
        print(json.dumps([dict(i, today=n < len(today)) for n, i in enumerate(items)], indent=1))
    else:
        print(out); print(summary(items, today, later))
    return 0

if __name__ == '__main__':
    if hasattr(sys.stdout, 'reconfigure'): sys.stdout.reconfigure(encoding='utf8', errors='replace')
    raise SystemExit(main())
