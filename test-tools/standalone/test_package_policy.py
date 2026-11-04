"""Strict package policy (plan 6.1) on synthetic archives and the checked-in build.gradle / dependency manifest."""
import json
import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from package_policy import excalibur_errors, package_policy_errors, pin_errors

GRADLE = (ROOT / 'build.gradle').read_text(encoding='utf8')
# The real Excalibur pin is covered by test_excalibur_pin.py; here DEPS is everything else so the optional/required cases stay synthetic.
DEPS = [d for d in json.loads((ROOT / 'dependencies/manifest.json').read_text(encoding='utf8')) if d['slug'] != 'excalibur']
EXCALIBUR = {'slug': 'excalibur', 'title': 'Excalibur', 'version': '1.0', 'fileName': 'Excalibur.zip',
             'path': 'dependencies/resourcepacks/Excalibur.zip', 'licenseMetadata': {'id': 'CC-BY-NC-ND-3.0'}}


def archive(deps, version='0.5.0', extra=()):
    """A clean synthetic archive for deps: (names, texts)."""
    names = ['docs/LICENSE', 'docs/README.md', 'docs/AUDIO-CREDITS.md', 'docs/THIRD_PARTY.md', 'mods/graves-and-glory-%s.jar' % version,
             'mods/fabric-api-0.92.2+1.20.1.jar', *extra]
    credits = 'Fabric API\nPixabay Content Licence\n' +''.join('## %s %s\nMaffhew CC BY-NC-ND\n' % (d['title'], d['version']) for d in deps)
    for d in deps:
        names.append('docs/third-party/%s/LICENSE' % d['slug'])
        if '/mods/' in d['path']:
            names.append('mods/' + d['fileName'])
    return names, {'docs/THIRD_PARTY.md': credits}


class PackagePolicyChecks(unittest.TestCase):
    def errors(self, deps, version='0.5.0', name='graves-and-glory-0.5.0.zip', gradle=GRADLE, extra=(), drop=()):
        names, texts = archive(deps, version, extra)
        names = [n for n in names if n not in drop]
        return package_policy_errors(names, texts, deps, version, name, gradle)

    def test_pixabay_audio_credit_is_required(self):
        self.assertEqual(self.errors(DEPS), [])
        self.assertTrue(any('Pixabay' in e for e in self.errors(DEPS, drop=('docs/AUDIO-CREDITS.md',))))
        names, texts = archive(DEPS)
        texts['docs/THIRD_PARTY.md'] = texts['docs/THIRD_PARTY.md'].replace('Pixabay', 'Other')
        self.assertTrue(any('Pixabay' in e for e in package_policy_errors(names, texts, DEPS, '0.5.0', 'graves-and-glory-0.5.0.zip', GRADLE)))

    def test_checked_in_platform_pins_are_exact(self):
        self.assertEqual(pin_errors(GRADLE), [])
        for bad in ('0.92.2+1.20.1', '1.20.1'):
            loose = GRADLE.replace("fabric-api:" + bad, "fabric-api:[0.92,)") if bad in GRADLE else GRADLE
            if loose != GRADLE:
                self.assertTrue(pin_errors(loose))
                break
        self.assertTrue(pin_errors(GRADLE.replace("fabric-loader:0.16.14", "fabric-loader:0.16.+")))
        self.assertTrue(pin_errors(GRADLE.replace("minecraft:1.20.1", "minecraft:1.20.1-SNAPSHOT")))

    def test_every_pinned_dependency_has_sha_licence_and_source(self):
        for dep in DEPS:
            with self.subTest(dep=dep['slug']):
                self.assertEqual(len(dep['sha256']), 64)
                self.assertTrue(dep['licenseMetadata']['id'])
                self.assertTrue(dep['project'])
                self.assertIn(dep['path'].split('/')[1], ('mods', 'shaders', 'resourcepacks'))

    def test_clean_archive_passes(self):
        self.assertEqual(self.errors(DEPS), [])

    def test_stopgap_name_harness_and_stray_jars_are_refused(self):
        self.assertTrue(self.errors(DEPS, name='graves-and-glory-0.5.0-stopgap.zip'))
        self.assertTrue(self.errors(DEPS, extra=['mods/local-integration-DO-NOT-SHIP.jar']))
        self.assertTrue(self.errors(DEPS, extra=['mods/surprise.jar']))
        self.assertTrue(self.errors(DEPS, drop=['mods/fabric-api-0.92.2+1.20.1.jar']))

    def test_every_third_party_file_needs_credit_and_licence(self):
        names, texts = archive(DEPS)
        texts['docs/THIRD_PARTY.md'] = texts['docs/THIRD_PARTY.md'].replace(DEPS[0]['title'], 'x')
        self.assertTrue(package_policy_errors(names, texts, DEPS, '0.5.0', 'a.zip', GRADLE))
        names, texts = archive(DEPS)
        names = [n for n in names if not n.startswith('docs/third-party/%s/' % DEPS[0]['slug'])]
        self.assertTrue(package_policy_errors(names, texts, DEPS, '0.5.0', 'a.zip', GRADLE))

    def test_excalibur_is_optional_before_1_0_0_and_required_after(self):
        self.assertEqual(excalibur_errors([], {}, DEPS, '0.9.9'), [])
        self.assertTrue(excalibur_errors([], {}, DEPS, '1.0.0'))
        self.assertTrue(self.errors(DEPS, version='1.0.0'))

    def test_excalibur_needs_licence_and_author_credit(self):
        deps = DEPS + [EXCALIBUR]
        self.assertEqual(self.errors(deps, version='1.0.0'), [])
        names, texts = archive(deps, '1.0.0')
        texts['docs/THIRD_PARTY.md'] = texts['docs/THIRD_PARTY.md'].replace('Maffhew', 'someone')
        self.assertTrue(excalibur_errors(names, texts, deps, '1.0.0'))
        self.assertTrue(excalibur_errors(names, texts, DEPS + [dict(EXCALIBUR, licenseMetadata={'id': 'MIT'})], '1.0.0'))
        self.assertTrue(excalibur_errors(names, texts, DEPS + [dict(EXCALIBUR, path='dependencies/mods/E.jar')], '1.0.0'))


if __name__ == '__main__':
    unittest.main(verbosity=2)
