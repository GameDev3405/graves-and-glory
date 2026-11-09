"""Generate docs/STATE.md from git facts and the lead sessions' status files. Read-only except for --out.

    python tools/state.py                      # print
    python tools/state.py --out docs/STATE.md  # write (tools/land.py does this and commits it)

STATE.md replaces the hand-written "where we are" sections: head, open lanes with age, lead status, open
questions with age. Anything it shows is regenerated, so never hand-edit it; fix the source instead
(git, a status file in ../status/, or docs/QUESTIONS-FOR-KORT.md).
"""
import argparse, datetime, pathlib, re, subprocess, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
STATUS_DIR = ROOT.parent/'status'  # C:/Users/kortt/Melty/status on Kort's machine
QUESTIONS = 'docs/QUESTIONS-FOR-KORT.md'

def git(repo, *args):
    return subprocess.run(['git', '-C', str(repo), *args], check=True, capture_output=True, text=True,
                          encoding='utf8', errors='replace').stdout.strip()

def age(now, then):
    days = (now - then).total_seconds() / 86400
    return f'{int(days)}d' if days >= 1 else f'{int(days*24)}h'

def stamp(seconds): return datetime.datetime.fromtimestamp(int(seconds), datetime.timezone.utc)

def worktrees(repo):
    out = []
    for record in git(repo, 'worktree', 'list', '--porcelain').split('\n\n'):
        f = dict(l.split(' ', 1) if ' ' in l else (l, True) for l in record.splitlines() if l)
        if 'worktree' in f:
            out.append(dict(path=f['worktree'], head=f.get('HEAD', ''), branch=str(f.get('branch', '')).removeprefix('refs/heads/') or None,
                            prunable='prunable' in f, locked='locked' in f))
    return out

def default_status_dir(repo):
    """status/ beside the primary checkout (C:/Users/kortt/Melty/status for bannerbound), from whichever checkout runs this."""
    trees = worktrees(repo)
    return pathlib.Path(trees[0]['path']).parent/'status' if trees else STATUS_DIR

KEYS = ('state', 'done since last', 'next', 'blocked', 'questions for kort', 'questions')

def read_status(path):
    """Fields of a lead status file. Accepts 'State: x', '- **State:** x', '1. State: x', '> State: x',
    '## State' headings, and values on the following lines (bullets under a heading are joined)."""
    fields, key = {}, None
    for raw in path.read_text(encoding='utf8', errors='replace').splitlines():
        line = re.sub(r'^\s*(?:[>#]+|[-*+]|\d+[.)])\s*', '', raw.replace('**', '')).strip()
        m = re.match(r'([A-Za-z ]+?)\s*:\s*(.*)$', line) or (re.match(r'([A-Za-z ]+?)\s*$()', line) if raw.lstrip().startswith('#') else None)
        if m and m.group(1).strip().lower() in KEYS:
            key = m.group(1).strip().lower(); fields.setdefault(key, m.group(2).strip()); continue
        if raw.lstrip().startswith('#'): key = None; continue
        if key and line: fields[key] = (fields[key] + ' ' + line).strip()
        elif not line: pass
    return fields

NONE = {'', 'none', 'none yet', 'none so far', 'none for now', 'nothing', 'nothing yet', 'no', 'no questions', 'n/a', '-', '0'}

def is_none(value):
    """Only a wholly empty answer counts: 'None, but pick a map' is a question, and so is anything unrecognised."""
    return re.sub(r'^[(\s]+|[().!\s]+$', '', (value or '').strip().lower()) in NONE

def question_rows(repo, ref):
    """Table rows of QUESTIONS-FOR-KORT.md on `ref`, each with the commit time of its line (git blame)."""
    try: blame = git(repo, 'blame', '--line-porcelain', ref, '--', QUESTIONS)
    except subprocess.CalledProcessError: return []
    rows, when = [], None
    for line in blame.splitlines():
        if line.startswith('author-time '):  # survives rebases, unlike committer-time
            when = stamp(line.split()[1])
        elif line.startswith('\t'):
            cells = [c.strip() for c in line[1:].strip().strip('|').split('|')]
            if line[1:].startswith('|') and len(cells) > 1 and cells[0] not in ('ID', '') and not set(cells[0]) <= set('-: '):
                rows.append(dict(id=cells[0], text=cells[2] if len(cells) > 2 else cells[1], since=when))
    return rows

def collect(repo, status_dir=STATUS_DIR, now=None, main='main'):
    repo = pathlib.Path(repo); now = now or datetime.datetime.now(datetime.timezone.utc)
    sha, subject, ct = git(repo, 'log', '-1', '--format=%H%x00%s%x00%ct', main).split('\x00')
    head = dict(sha=sha, subject=subject, when=stamp(ct))
    try: head['unpushed'] = int(git(repo, 'rev-list', '--count', f'origin/{main}..{main}'))
    except subprocess.CalledProcessError: head['unpushed'] = None
    lanes, merged = [], 0
    for w in worktrees(repo):
        if w['branch'] == main or w['prunable']: continue
        ref = w['branch'] or w['head']
        ahead = int(git(repo, 'rev-list', '--count', f'{main}..{ref}'))
        if not ahead: merged += 1; continue
        behind = int(git(repo, 'rev-list', '--count', f'{ref}..{main}'))
        last = stamp(git(repo, 'log', '-1', '--format=%ct', ref))
        lanes.append(dict(name=w['branch'] or f'detached {w["head"][:8]}', path=w['path'], ahead=ahead, behind=behind, last=last))
    lanes.sort(key=lambda l: l['last'])
    leads, questions = [], []
    status_ok = bool(status_dir) and pathlib.Path(status_dir).is_dir()
    for path in sorted(pathlib.Path(status_dir).glob('*.md')) if status_ok else []:
        f = read_status(path); since = stamp(path.stat().st_mtime)
        if 'state' not in f: continue  # not a lead status file (e.g. DECISIONS-*.md)
        leads.append(dict(name=path.stem, since=since, state=f['state'] or '?', blocked=f.get('blocked', '')))
        q = f.get('questions for kort') or f.get('questions', '')
        if not is_none(q): questions.append(dict(id=path.stem, text=q, since=since, from_status=True))
    questions += question_rows(repo, main)
    return dict(status_dir=str(status_dir), status_ok=status_ok, now=now, main=main, head=head, lanes=lanes, merged_worktrees=merged, leads=leads, questions=questions)

def clip(text, n=140): return text if len(text) <= n else text[:n-3].rstrip() + '...'

def lane_lines(lanes, now, full=False, group_at=5):
    """One line per lane; families of `group_at`+ lanes with one prefix (anim/v2-*) fold into one line unless full."""
    def fam(name):
        head, _, tail = name.partition('/')  # anim/v2-wm-stalker -> anim/v2-*
        return f'{head}/{tail.split("-", 1)[0]}-*' if tail and '-' in tail else None
    groups = {}
    for l in lanes: groups.setdefault(fam(l['name']), []).append(l)
    out, folded = [], set()
    for l in lanes:
        f = fam(l['name'])
        if not full and f and len(groups[f]) >= group_at:
            if f in folded: continue
            g = groups[f]; folded.add(f)
            out.append(f'- `{f}` {len(g)} lanes, {min(x["ahead"] for x in g)} to {max(x["ahead"] for x in g)} ahead, '
                       f'up to {max(x["behind"] for x in g)} behind, last commits {age(now, max(x["last"] for x in g))} to {age(now, min(x["last"] for x in g))} ago')
        else:
            out.append(f'- `{l["name"]}` {l["ahead"]} ahead, {l["behind"]} behind, last commit {age(now, l["last"])} ago')
    return out

def render(s, full=False):
    now, h = s['now'], s['head']
    out = ['# State', '', f'Generated by `tools/state.py` at {now:%Y-%m-%d %H:%M} UTC. Do not edit; fix the source (git, `../status/*.md`, `{QUESTIONS}`).', '',
           f'**Head:** `{s["main"]}` {h["sha"][:10]} {h["subject"]} ({age(now, h["when"])} ago).']
    if h['unpushed'] is not None: out.append(f'**Backup:** {h["unpushed"]} commits not on `origin/{s["main"]}`.')
    out += ['', f'## Open lanes ({len(s["lanes"])}; {s["merged_worktrees"]} merged worktrees kept)', '']
    out += lane_lines(s['lanes'], now, full) or ['- none']
    out += ['', '## Leads', '']
    for l in s['leads']:
        line = f'- **{l["name"]}** ({age(now, l["since"])} ago): {clip(l["state"])}'
        if not is_none(l['blocked']): line += f' Blocked: {clip(l["blocked"], 80)}'
        out.append(line)
    if not s['leads']: out.append('- no lead status files' + ('' if s.get('status_ok', True) else f' (folder {s["status_dir"]} not found)'))
    out += ['', '## Open questions for Kort', '']
    out += [f'- **{q["id"]}** ({"status updated " + age(now, q["since"]) + " ago" if q.get("from_status") else age(now, q["since"]) + " old"}): {clip(q["text"])}' for q in sorted(s['questions'], key=lambda q: q['since'])] or ['- none']
    return '\n'.join(out) + '\n'

def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    p.add_argument('--repo', default=str(ROOT)); p.add_argument('--status-dir', help='default: status/ beside the primary checkout')
    p.add_argument('--out'); p.add_argument('--main', default='main')
    p.add_argument('--full', action='store_true', help='one line per lane, no folding of lane families')
    a = p.parse_args(argv)
    text = render(collect(a.repo, a.status_dir or default_status_dir(a.repo), main=a.main), a.full)
    if a.out:
        out = pathlib.Path(a.out); out = out if out.is_absolute() else pathlib.Path(a.repo)/out
        out.parent.mkdir(parents=True, exist_ok=True); out.write_text(text, encoding='utf8', newline='\n')
    else: print(text, end='')
    return 0

if __name__ == '__main__':
    if hasattr(sys.stdout, 'reconfigure'): sys.stdout.reconfigure(encoding='utf8', errors='replace')
    raise SystemExit(main())
