"""Rough mixin target check against a mapped jar: target class, method names (+descriptor if given), @At target owner/name, accessors, shadows.
Also inspects injector handler parameter types vs. target descriptor is NOT done (needs real Mixin); this is names-only."""
import re, sys, subprocess, pathlib, functools
jar, src = sys.argv[1], pathlib.Path(sys.argv[2])
JAVAP = r'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin\javap.exe'
@functools.lru_cache(None)
def members(cls):
    r = subprocess.run([JAVAP, '-p', '-s', '-cp', jar, cls], capture_output=True, text=True)
    if r.returncode: return None
    names, descs = set(), set(); last = None
    for line in r.stdout.splitlines():
        line = line.strip()
        if line.startswith('descriptor:'):
            if last: descs.add(last + line.split(':', 1)[1].strip())
            continue
        m = re.search(r'([\w$<>]+)\(', line)
        if m and not line.startswith(('class', 'public class', 'interface')):
            last = m.group(1); names.add(last); continue
        m = re.search(r'([\w$]+);$', line)
        if m: last = m.group(1); names.add(last)
    if '<init>' not in names: names.add('<init>')
    return names, descs
def ctorfix(cls, names):
    return names | {cls.split('.')[-1].split('$')[-1]}
problems = []; checked = 0
for p in sorted(src.glob('*.java')):
    s = p.read_text(encoding='utf-8')
    m = re.search(r'@Mixin\(\s*(?:value\s*=\s*)?\{?\s*([\w.]+)\.class', s)
    if not m: continue
    simple = m.group(1)
    imp = re.search(r'import ([\w.]+\.' + re.escape(simple.split('.')[0]) + r');', s)
    fq = (imp.group(1) + simple[len(simple.split('.')[0]):]) if imp else simple
    fq = re.sub(r'\.([A-Z]\w*)\.([A-Z]\w*)$', r'.\1$\2', fq)
    mem = members(fq)
    if mem is None: problems.append((p.name, 'target class missing', fq)); continue
    names, descs = mem; names = ctorfix(fq, names)
    for mm in re.finditer(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', s):
        for t in re.findall(r'"([^"]+)"', mm.group(1)):
            checked += 1
            n = t.split('(')[0]
            if n not in names: problems.append((p.name, 'method missing', fq.split('.')[-1] + '.' + t))
            elif '(' in t and (n + t[len(n):]) not in descs: problems.append((p.name, 'method descriptor changed', fq.split('.')[-1] + '.' + t))
    for t in re.findall(r'target\s*=\s*"L([\w/$]+);([\w<>$]+)([^"]*)"', s):
        checked += 1
        owner = t[0].replace('/', '.'); om = members(owner)
        if om is None: problems.append((p.name, '@At owner missing', owner)); continue
        if t[1] not in om[0]: problems.append((p.name, '@At target missing', owner.split('.')[-1] + '.' + t[1]))
        elif t[2].startswith('(') and (t[1] + t[2]) not in om[1]: problems.append((p.name, '@At descriptor changed', owner.split('.')[-1] + '.' + t[1] + t[2]))
        elif t[2].startswith(':') and t[1] not in om[0]: problems.append((p.name, '@At field missing', t[1]))
    for kind, t in re.findall(r'@(Accessor|Invoker)\("([^"]+)"\)', s):
        checked += 1
        if t not in names: problems.append((p.name, kind + ' missing', fq.split('.')[-1] + '.' + t))
    for t in re.findall(r'@Shadow[^;{]*?\s([\w$]+)\s*(?:\(|;)', s):
        checked += 1
        if t not in names: problems.append((p.name, 'shadow missing', fq.split('.')[-1] + '.' + t))
print('mixin references checked:', checked, ' problems:', len(problems))
for x in problems: print(' ', *x)

# Signature drift: same name, but its descriptor set changed between the old and new jar (handler params or @At args need rework).
if len(sys.argv) > 3:
    old = sys.argv[3]
    @functools.lru_cache(None)
    def descs_of(j, cls, name):
        r = subprocess.run([JAVAP, '-p', '-s', '-cp', j, cls], capture_output=True, text=True)
        out = set(); last = None
        for line in r.stdout.splitlines():
            line = line.strip()
            if line.startswith('descriptor:'):
                if last == name: out.add(line.split(':', 1)[1].strip())
                continue
            m = re.search(r'([\w$<>]+)\(', line); last = m.group(1) if m else None
        return frozenset(out)
    drift = []
    for p in sorted(src.glob('*.java')):
        s = p.read_text(encoding='utf-8')
        m = re.search(r'@Mixin\(\s*(?:value\s*=\s*)?\{?\s*([\w.]+)\.class', s)
        if not m: continue
        simple = m.group(1)
        imp = re.search(r'import ([\w.]+\.' + re.escape(simple.split('.')[0]) + r');', s)
        fq = (imp.group(1) + simple[len(simple.split('.')[0]):]) if imp else simple
        fq = re.sub(r'\.([A-Z]\w*)\.([A-Z]\w*)$', r'.\1$\2', fq)
        refs = set()
        for mm in re.finditer(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', s):
            for t in re.findall(r'"([^"]+)"', mm.group(1)): refs.add((fq, t.split('(')[0]))
        for t in re.findall(r'target\s*=\s*"L([\w/$]+);([\w<>$]+)\(', s): refs.add((t[0].replace('/', '.'), t[1]))
        for kind, t in re.findall(r'@(Invoker)\("([^"]+)"\)', s): refs.add((fq, t))
        for cls, n in sorted(refs):
            a, b = descs_of(old, cls, n), descs_of(jar, cls, n)
            if a != b: drift.append((p.name, cls.split('.')[-1] + '.' + n, sorted(a - b), sorted(b - a)))
    print('signature drift (old -> new):', len(drift))
    for x in drift: print(' ', x[0], x[1], '\n     old:', x[2], '\n     new:', x[3])
