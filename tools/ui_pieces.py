"""The UI editor's piece list (UI editor design 5.1 and 5.9, phase 1: words).

Reads every UiText.t("id", "default", "slot", value, ...) and UiText.locked("id", ...) call in src/main/java/gg/gravesandglory and
writes src/main/resources/assets/gravesandglory/ui/pieces.json, which the server validates edits against.

Fails (exit 1, one line per finding) when:
- a call's id, default or a slot name is not a single string literal;
- an id is malformed or declared at two call sites (a row drawn in a loop is one call site, so one piece);
- a default names a slot its call does not supply, or holds a lone brace;
- a slot is supplied twice in one call;
- an edit in ui/builtin-edits.json is an orphan (no such piece) or breaks the piece's rules (5.9: reported before release);
- a rename in ui/renames.json points at an unknown piece;
- with --verify: pieces.json is not what the source gives (run this tool without --verify to regenerate it).

Usage: python tools/ui_pieces.py [--verify | --check]   (--check: findings only, writes nothing)
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
SRC = ROOT / 'src/main/java/gg/gravesandglory'
UI = ROOT / 'src/main/resources/assets/gravesandglory/ui'
PIECES = UI / 'pieces.json'
BUILTIN = UI / 'builtin-edits.json'
RENAMES = UI / 'renames.json'

CALL = re.compile(r'\bUiText\s*\.\s*(t|locked)\s*\(')
ID = re.compile(r'^[a-z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+$')
SLOT = re.compile(r'^[a-z][a-zA-Z0-9_]{0,31}$')
KEY_ID = re.compile(r'^[a-z0-9_]+$')
TEXT_CAP = 300


def strip_comments(src):
    """Blanks // and /* */ comments (keeping line breaks and string literals), so a call in a comment is not read."""
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c == '"' or c == "'":
            j = i + 1
            while j < n and src[j] != c:
                j += 2 if src[j] == '\\' else 1
            out.append(src[i:j + 1]); i = j + 1
        elif src.startswith('//', i):
            j = src.find('\n', i); j = n if j < 0 else j
            out.append(' ' * (j - i)); i = j
        elif src.startswith('/*', i):
            j = src.find('*/', i + 2); j = n if j < 0 else j + 2
            out.append(''.join('\n' if ch == '\n' else ' ' for ch in src[i:j])); i = j
        else:
            out.append(c); i += 1
    return ''.join(out)


def split_args(src, start):
    """The top-level arguments of the call whose '(' ends just before start, and the index after its ')'."""
    args, depth, i, n, cur = [], 0, start, len(src), start
    while i < n:
        c = src[i]
        if c == '"' or c == "'":
            j = i + 1
            while j < n and src[j] != c:
                j += 2 if src[j] == '\\' else 1
            i = j + 1; continue
        if c in '([{': depth += 1
        elif c in ')]}':
            if depth == 0:
                args.append(src[cur:i].strip()); return args, i + 1
            depth -= 1
        elif c == ',' and depth == 0:
            args.append(src[cur:i].strip()); cur = i + 1
        i += 1
    raise ValueError('unclosed call')


LITERAL = re.compile(r'^"((?:[^"\\]|\\.)*)"$', re.S)


def literal(arg):
    """The value of a single Java string literal, or None."""
    m = LITERAL.match(arg)
    if not m: return None
    s, out, i = m.group(1), [], 0
    while i < len(s):
        if s[i] == '\\':
            e = s[i + 1]
            if e == 'u':
                out.append(chr(int(s[i + 2:i + 6], 16))); i += 6; continue
            out.append({'n': '\n', 't': '\t', '"': '"', "'": "'", '\\': '\\', 'r': '\r', 'b': '\b', 'f': '\f', '0': '\0'}.get(e, e)); i += 2; continue
        out.append(s[i]); i += 1
    return ''.join(out)


def template_parts(t):
    """(slots, problems, has_key) of a template, by the grammar of UiTemplate."""
    slots, problems, has_key, i, n = [], [], False, 0, len(t)
    while i < n:
        c = t[i]
        if t.startswith('{{', i) or t.startswith('}}', i): i += 2; continue
        if c == '{':
            close = t.find('}', i + 1)
            inside = t[i + 1:close] if close >= 0 else None
            if inside is not None and inside.startswith('key:') and KEY_ID.match(inside[4:]):
                has_key = True; i = close + 1; continue
            if inside is not None and SLOT.match(inside):
                if inside not in slots: slots.append(inside)
                i = close + 1; continue
            problems.append(f"a '{{' at {i + 1} opens no slot (write {{{{ for a brace)"); i += 1; continue
        if c == '}': problems.append(f"a '}}' at {i + 1} closes no slot (write }}}} for a brace)")
        i += 1
    return slots, problems, has_key


def scan(src_dir=SRC):
    """(pieces sorted by id, findings)."""
    pieces, findings, where = {}, [], {}
    for path in sorted(src_dir.rglob('*.java')):
        if path.name in ('UiText.java',): continue
        text = strip_comments(path.read_text(encoding='utf8'))
        rel = path.relative_to(src_dir).as_posix()
        for m in CALL.finditer(text):
            line = text.count('\n', 0, m.start()) + 1
            at = f'{rel}:{line}'
            try:
                args, _ = split_args(text, m.end())
            except ValueError:
                findings.append(f'{at}: unclosed UiText call'); continue
            kind = m.group(1)
            pid = literal(args[0]) if args else None
            if pid is None:
                findings.append(f'{at}: the piece id must be a string literal'); continue
            if not ID.match(pid):
                findings.append(f'{at}: piece id "{pid}" is not dotted lower-camel words (screen.part.name)'); continue
            if pid in where:
                findings.append(f'{at}: piece id "{pid}" is already declared at {where[pid]}'); continue
            where[pid] = at
            if kind == 'locked':
                if len(args) != 3 or literal(args[2]) is None:
                    findings.append(f'{at}: UiText.locked takes (id, text, "source") with the source a literal'); continue
                pieces[pid] = {'id': pid, 'locked': True, 'source': literal(args[2]), 'file': rel}
                continue
            if len(args) < 2 or literal(args[1]) is None:
                findings.append(f'{at}: the default wording of "{pid}" must be one string literal (live values go in slots)'); continue
            default = literal(args[1])
            rest = args[2:]
            if len(rest) % 2:
                findings.append(f'{at}: "{pid}" has an odd number of slot arguments (name, value pairs)'); continue
            supplied = []
            for k in range(0, len(rest), 2):
                name = literal(rest[k])
                if name is None or not SLOT.match(name):
                    findings.append(f'{at}: slot name {rest[k]} of "{pid}" must be a lower-camel string literal'); break
                if name in supplied:
                    findings.append(f'{at}: slot "{name}" of "{pid}" is supplied twice'); break
                supplied.append(name)
            else:
                used, problems, has_key = template_parts(default)
                for p in problems: findings.append(f'{at}: default of "{pid}": {p}')
                for s in used:
                    if s not in supplied: findings.append(f'{at}: default of "{pid}" names {{{s}}}, which the call does not supply')
                if any(ord(c) < 32 or c == '§' for c in default):
                    findings.append(f'{at}: default of "{pid}" holds a control character or section sign')
                pieces[pid] = {'id': pid, 'default': default, 'slots': supplied, 'keys': has_key, 'file': rel}
    return [pieces[k] for k in sorted(pieces)], findings


def edit_problem(piece, text):
    if piece is None: return 'orphan: no such piece'
    if piece.get('locked'): return 'the piece is locked (' + piece.get('source', '') + ')'
    if len(text) > TEXT_CAP: return f'longer than {TEXT_CAP} characters'
    if any(ord(c) < 32 or c == '§' for c in text): return 'control character or section sign'
    used, problems, has_key = template_parts(text)
    if problems: return problems[0]
    for s in used:
        if s not in piece['slots']: return f'{{{s}}} is not a live value this line offers'
    if has_key and not piece['keys']: return 'this line does not show key hints'
    return None


def check_resources(pieces):
    findings, by_id = [], {p['id']: p for p in pieces}
    if BUILTIN.exists():
        try:
            edits = json.loads(BUILTIN.read_text(encoding='utf8')).get('edits', {})
        except ValueError as e:
            return [f'{BUILTIN.name}: not JSON ({e})']
        for pid, edit in sorted(edits.items()):
            p = edit_problem(by_id.get(pid), edit.get('text', ''))
            if p: findings.append(f'{BUILTIN.name}: {pid}: {p}')
    if RENAMES.exists():
        for old, new in json.loads(RENAMES.read_text(encoding='utf8')).get('renames', {}).items():
            if new not in by_id: findings.append(f'{RENAMES.name}: {old} -> {new}: no such piece')
            if old in by_id: findings.append(f'{RENAMES.name}: {old} is renamed but still declared')
    return findings


def render(pieces):
    return json.dumps({'format': 1, 'pieces': pieces}, indent=1, ensure_ascii=False) + '\n'


def main(argv):
    pieces, findings = scan()
    findings += check_resources(pieces)
    out = render(pieces)
    if '--check' in argv:
        pass  # findings only: workers converting screens in parallel never write pieces.json
    elif '--verify' in argv:
        if not PIECES.exists() or PIECES.read_text(encoding='utf8') != out:
            findings.append(f'{PIECES.relative_to(ROOT).as_posix()} is stale: run python tools/ui_pieces.py')
    else:
        UI.mkdir(parents=True, exist_ok=True)
        PIECES.write_text(out, encoding='utf8', newline='\n')
    for f in findings: print(f)
    if findings: return 1
    print(f'ui_pieces: {len(pieces)} pieces, clean')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
