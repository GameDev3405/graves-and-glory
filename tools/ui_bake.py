"""Bakes a server's published UI edits into the mod's built-in edits (UI editor design 5.8; a release step).

    python tools/ui_bake.py <server config/gravesandglory/ui-edits.json> [--server-out PATH]

- Reads the server sheet (it is never changed in place) and src/main/resources/assets/gravesandglory/ui/pieces.json.
- Writes src/main/resources/assets/gravesandglory/ui/builtin-edits.json: the old built-in edits with the published ones on top.
  An edit equal to its piece's code default drops out. Orphans (no such piece) and edits that break a piece's rules are not baked;
  they are listed, and the tool exits 1 so they are seen before release.
- Writes the trimmed server sheet (only what still differs from the new built-in edits, history kept, a new version recorded) to
  --server-out (default: ui-edits.baked.json next to the input). Put it on the server once the release with the bake is live.
The result is an ordinary change for Kort to review and approve like any other before it ships.
"""
import datetime
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import ui_pieces  # noqa: E402


def texts(sheet):
    return {k: v.get('text', '') for k, v in sheet.get('edits', {}).items() if isinstance(v, dict) and isinstance(v.get('text'), str)}


def as_edits(m):
    return {k: {'text': m[k]} for k in sorted(m)}


def main(argv):
    if not argv or argv[0].startswith('--'):
        print(__doc__); return 2
    src = pathlib.Path(argv[0])
    out = pathlib.Path(argv[argv.index('--server-out') + 1]) if '--server-out' in argv else src.with_name('ui-edits.baked.json')
    server = json.loads(src.read_text(encoding='utf8'))
    pieces = {p['id']: p for p in json.loads(ui_pieces.PIECES.read_text(encoding='utf8'))['pieces']}
    builtin_sheet = json.loads(ui_pieces.BUILTIN.read_text(encoding='utf8')) if ui_pieces.BUILTIN.exists() else {'format': 1, 'version': 0, 'edits': {}}
    builtin = texts(builtin_sheet)
    published = texts(server)
    problems = []
    baked = dict(builtin)
    for pid, text in sorted(published.items()):
        why = ui_pieces.edit_problem(pieces.get(pid), text)
        if why:
            problems.append(f'{pid}: {why} (not baked)'); continue
        if text == pieces[pid]['default']: baked.pop(pid, None)
        else: baked[pid] = text
    builtin_sheet = {'format': 1, 'version': int(builtin_sheet.get('version', 0)) + 1, 'edits': as_edits(baked)}
    ui_pieces.BUILTIN.write_text(json.dumps(builtin_sheet, indent=1, ensure_ascii=False) + '\n', encoding='utf8', newline='\n')
    left = {k: v for k, v in published.items() if baked.get(k) != v and not (k not in baked and pieces.get(k, {}).get('default') == v)}
    now = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    history = list(server.get('history', []))
    history.insert(0, {'version': server.get('version', 0), 'time': server.get('time', ''), 'who': server.get('who', ''),
                       'summary': server.get('summary', ''), 'edits': server.get('edits', {})})
    trimmed = {'format': 1, 'version': int(server.get('version', 0)) + 1, 'time': now, 'who': 'bake',
               'summary': f'baked into the built-in edits; {len(left)} edit(s) still differ', 'edits': as_edits(left), 'history': history[:50]}
    out.write_text(json.dumps(trimmed, indent=2, ensure_ascii=False) + '\n', encoding='utf8')
    print(f'ui_bake: {len(baked)} built-in edit(s) in {ui_pieces.BUILTIN.relative_to(ui_pieces.ROOT).as_posix()}; '
          f'trimmed server sheet ({len(left)} edit(s)) written to {out}')
    for p in problems: print(p)
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
