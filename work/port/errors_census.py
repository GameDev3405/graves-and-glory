"""Turns a Gradle compileJava log into work/port/errors-<name>.txt: errors per file, deduplicated, with javac's symbol lines."""
import collections, re, sys

log, dest = sys.argv[1], sys.argv[2]
lines = open(log, encoding='utf-8', errors='replace').read().splitlines()
BS = chr(92)
errs, seen = [], set()
for i, line in enumerate(lines):
    m = re.match(r'^\s*([A-Z]:.*?\.java):(\d+): error: (.*)$', line)
    if not m:
        continue
    path = m.group(1).replace(BS, '/')
    f = path.split('/src/main/java/')[-1].split('/test-tools/')[-1].replace('gg/gravesandglory/', '')
    key = (f, m.group(2), m.group(3))
    if key in seen:
        continue
    seen.add(key)
    ctx = []
    for j in range(i + 1, min(i + 7, len(lines))):
        t = lines[j].strip()
        if re.match(r'^([A-Z]:|\d+ errors?)', t):
            break
        if t.startswith(('symbol:', 'location:', 'required:', 'found:', 'reason:')):
            ctx.append(t)
    errs.append((f, int(m.group(2)), m.group(3), ctx))
by = collections.defaultdict(list)
for e in errs:
    by[e[0]].append(e)
out = [f'# compileJava errors on port/1.21.1 after the core lane: {len(errs)} errors in {len(by)} files (javac -Xmaxerrs 10000, -PallErrors).',
       '# A floor, not a total: lambda bodies under a failed overload (the old 4- and 5-argument receivers) and code behind a failed line are not attributed yet.',
       '', '## Count per file']
for f, es in sorted(by.items(), key=lambda kv: (-len(kv[1]), kv[0])):
    out.append(f'{len(es):4d}  {f}')
out.append('')
for f, es in sorted(by.items()):
    out.append(f'## {f} ({len(es)})')
    for _, ln, msg, ctx in sorted(es, key=lambda e: e[1]):
        out.append(f'  {ln}: {msg}' + (('  [' + '; '.join(ctx) + ']') if ctx else ''))
    out.append('')
open(dest, 'w', newline='\n', encoding='utf-8').write('\n'.join(out))
print(len(errs), 'errors in', len(by), 'files')
