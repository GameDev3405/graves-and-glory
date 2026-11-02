"""Read back the finished archive and verify its manifest, inner JAR and pins."""
import hashlib
import io
import json
import zipfile
from release_config import ROOT, source_identity, validate_player_jar
from package_policy import package_policy_errors

manifest = json.loads((ROOT / 'dist/package-manifest.json').read_text())
path = ROOT / 'dist' / manifest['fileName']
if hashlib.sha256(path.read_bytes()).hexdigest() != manifest['sha256']:
    raise RuntimeError('Archive hash differs from manifest')
with zipfile.ZipFile(path) as archive:
    names = archive.namelist()
    if len(names) != len(set(names)):
        raise RuntimeError('Duplicate archive entries')
    if set(names) != {x['path'] for x in manifest['entries']}:
        raise RuntimeError('Archive file inventory differs from manifest')
    for entry in manifest['entries']:
        data = archive.read(entry['path'])
        if len(data) != entry['size'] or hashlib.sha256(data).hexdigest() != entry['sha256']:
            raise RuntimeError('Archive entry failed verification: ' + entry['path'])
        if not entry['path'].startswith(('mods/', 'docs/', 'shaderpacks/', 'resourcepacks/')):
            raise RuntimeError('Unexpected private/runtime path: ' + entry['path'])
    identity = source_identity()
    jar_name = 'mods/graves-and-glory-' + identity['version'] + '.jar'
    validate_player_jar(io.BytesIO(archive.read(jar_name)), identity)
    for dep in json.loads((ROOT / 'dependencies/manifest.json').read_text(encoding='utf8')):
        folder = 'mods' if '/mods/' in dep['path'] else 'shaderpacks' if '/shaders/' in dep['path'] else 'resourcepacks'
        if hashlib.sha256(archive.read(folder + '/' + dep['fileName'])).hexdigest() != dep['sha256']:
            raise RuntimeError('Packaged dependency differs from pin: ' + dep['fileName'])
# Strict package policy (plan 6.1): exact pins, credits and licences for every third-party file, no stopgap mark.
deps = json.loads((ROOT / 'dependencies/manifest.json').read_text(encoding='utf8'))
with zipfile.ZipFile(path) as archive:
    texts = {n: archive.read(n).decode('utf8', errors='replace') for n in names
             if n == 'docs/THIRD_PARTY.md' or (n.startswith('docs/third-party/') and n.endswith(('.md', '.txt', '.json')) and archive.getinfo(n).file_size < 400000)}
problems = package_policy_errors(names, texts, deps, identity['version'], manifest['fileName'],
                                 (ROOT / 'build.gradle').read_text(encoding='utf8'))
if problems:
    raise RuntimeError('Strict package policy failed:\n  ' + '\n  '.join(problems))
print(json.dumps({'result':'PASS', 'file':str(path), 'entriesVerified':len(names),
                  'sha256':manifest['sha256'], 'build':identity,
                  'scope':'Archive integrity only; no Minecraft or launcher session executed.'},indent=2))
