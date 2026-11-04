"""Strict package policy (plan item 6.1) as pure functions: tested without building a package.

tools/verify_package.py applies these to the finished archive. This module is deliberately not part of
release_config.source_identity(), so changing a rule here never invalidates a built jar.
"""
import re

EXACT_PIN = re.compile(r'^\d+(\.\d+)+([+-][A-Za-z0-9.]+)*$')


def gradle_pins(gradle_text):
    """{artifact: version} for the Minecraft, Yarn, Loader and Fabric API coordinates in build.gradle."""
    pins = {}
    for _kind, coord in re.findall(r"^\s*(minecraft|mappings|modImplementation)\s+'([^']+)'", gradle_text, re.MULTILINE):
        parts = coord.split(':')
        if len(parts) >= 3:
            pins[parts[1]] = parts[2]
    return pins


def pin_errors(gradle_text):
    """Every platform coordinate must be one exact version: no ranges, wildcards, dynamic or snapshot versions."""
    errors, pins = [], gradle_pins(gradle_text)
    for need in ('minecraft', 'yarn', 'fabric-loader', 'fabric-api'):
        if need not in pins:
            errors.append('build.gradle does not pin ' + need)
        elif not EXACT_PIN.match(pins[need]) or 'SNAPSHOT' in pins[need].upper():
            errors.append('build.gradle pin for %s is not an exact version: %s' % (need, pins[need]))
    return errors


def _version_tuple(text):
    return tuple(int(x) for x in re.match(r'(\d+)\.(\d+)\.(\d+)', text).groups())


def excalibur_errors(names, texts, deps, release_version):
    """Excalibur (CC BY-NC-ND 3.0) is required from 1.0.0 on. Wherever it is present it must be the pinned,
    byte-identical upstream file (the dependency checksum check proves that) with its licence id and author credit."""
    rows = [d for d in deps if 'excalibur' in d['slug'].lower() or 'excalibur' in d['title'].lower()]
    if not rows:
        if _version_tuple(release_version) >= (1, 0, 0):
            return ['Excalibur is not in dependencies/manifest.json (required from 1.0.0)']
        return []
    errors = []
    for dep in rows:
        if '/mods/' in dep['path'] or '/shaders/' in dep['path']:
            errors.append('Excalibur must be a resource pack, not ' + dep['path'])
        licence = (dep.get('licenseMetadata') or {}).get('id', '')
        if 'NC-ND' not in licence.upper():
            errors.append('Excalibur licence id must record CC BY-NC-ND, got: ' + repr(licence))
        notice = [n for n in names if n.startswith('docs/third-party/%s/' % dep['slug'])]
        blob = texts.get('docs/THIRD_PARTY.md', '') + ''.join(texts.get(n, '') for n in notice)
        for token in ('Maffhew', 'NC-ND'):
            if token.lower() not in blob.lower():
                errors.append('Excalibur credit/licence text lacks ' + token)
    return errors


def package_policy_errors(names, texts, deps, release_version, file_name, gradle_text):
    """Problems that make an archive unfit for a strict release; [] when clean.
    names: archive entry paths; texts: {path: str} for text entries; deps: dependencies/manifest.json rows."""
    errors, names = pin_errors(gradle_text), list(names)
    if '-stopgap' in file_name:
        errors.append('Archive is marked -stopgap (a troop still draws stand-in art): ' + file_name)
    for need in ('docs/LICENSE', 'docs/THIRD_PARTY.md', 'docs/README.md'):
        if need not in names:
            errors.append('Archive lacks ' + need)
    for name in names:
        if 'DO-NOT-SHIP' in name or 'local-integration' in name.lower():
            errors.append('Local test tooling in archive: ' + name)
    credits = texts.get('docs/THIRD_PARTY.md', '')
    api = gradle_pins(gradle_text).get('fabric-api', '?')
    expected_mods = {'mods/graves-and-glory-%s.jar' % release_version, 'mods/fabric-api-%s.jar' % api}
    for dep in deps:
        if '/mods/' in dep['path']:
            expected_mods.add('mods/' + dep['fileName'])
        if '%s %s' % (dep['title'], dep['version']) not in credits:
            errors.append('THIRD_PARTY.md does not credit %s %s' % (dep['title'], dep['version']))
        if not (dep.get('licenseMetadata') or {}).get('id'):
            errors.append('No licence id recorded for ' + dep['slug'])
        if not any(n.startswith('docs/third-party/%s/' % dep['slug']) for n in names):
            errors.append('No licence/notice file packaged for ' + dep['slug'])
    for extra in sorted({n for n in names if n.startswith('mods/') and n.endswith('.jar')} - expected_mods):
        errors.append('Unlisted jar in mods/: ' + extra)
    for missing in sorted(expected_mods - set(names)):
        errors.append('Pinned jar missing from archive: ' + missing)
    if 'Pixabay' not in credits or 'docs/AUDIO.md' not in names:
        errors.append('Pixabay audio credit missing: THIRD_PARTY.md must credit Pixabay and docs/AUDIO.md must be packaged')
    if 'Fabric API' not in credits:
        errors.append('THIRD_PARTY.md does not credit Fabric API')
    errors += excalibur_errors(names, texts, deps, release_version)
    return errors
