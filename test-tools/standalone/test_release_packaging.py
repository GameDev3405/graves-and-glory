"""Packaging helpers (tools/release_files.py): release-notes gate, mrpack summary, portable Gradle cache paths."""
import hashlib
import io
import json
import os
import pathlib
import sys
import tempfile
import unittest
import zipfile
from contextlib import redirect_stdout
from unittest import mock

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from release_files import gradle_home, mrpack_summary, require_release_notes, resolve_local_path

OLD_WINDOWS = ('C:\\Users\\someone\\.gradle\\caches\\modules-2\\files-2.1\\net.fabricmc.fabric-api\\fabric-api\\'
               '0.92.2+1.20.1\\625ee0\\fabric-api-0.92.2+1.20.1.jar')


class ReleaseNotesGate(unittest.TestCase):
    def test_missing_release_file_gives_a_clear_message(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(SystemExit) as caught:
                require_release_notes(pathlib.Path(tmp), '1.0.0')
            self.assertIn('docs/RELEASE-1.0.0.txt is missing', str(caught.exception))

    def test_present_release_file_is_returned(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            (root / 'docs').mkdir()
            (root / 'docs/RELEASE-1.0.0.txt').write_text('notes', encoding='utf8')
            self.assertEqual(require_release_notes(root, '1.0.0'), root / 'docs/RELEASE-1.0.0.txt')

    def test_the_checked_in_1_0_0_notes_are_player_facing(self):
        text = (ROOT / 'docs/RELEASE-1.0.0.txt').read_text(encoding='utf8')
        self.assertTrue(text.startswith('Graves and Glory 1.0.0'))
        self.assertNotIn('Kort to confirm', text)


class MrpackSummary(unittest.TestCase):
    def test_summary_names_the_version(self):
        self.assertIn('1.0.0', mrpack_summary('1.0.0'))
        self.assertNotIn('Local release candidate', mrpack_summary('1.0.0'))
        self.assertNotEqual(mrpack_summary('1.0.0'), mrpack_summary('1.0.1'))

    def test_prerelease_suffix_is_marked(self):
        self.assertIn('Pre-release', mrpack_summary('1.0.0-prerc1'))
        self.assertNotIn('Pre-release', mrpack_summary('1.0.0'))

    def test_export_modrinth_uses_the_version_driven_summary(self):
        import export_modrinth
        source = (ROOT / 'tools/export_modrinth.py').read_text(encoding='utf8')
        self.assertIn("'summary':mrpack_summary(release)", source)
        self.assertNotIn('Local release candidate', source)
        self.assertIs(export_modrinth.mrpack_summary, mrpack_summary)


class PortableLocalPaths(unittest.TestCase):
    def test_gradle_home_prefers_the_environment_variable(self):
        with mock.patch.dict(os.environ, {'GRADLE_USER_HOME': '/custom/gradle'}):
            self.assertEqual(gradle_home(), pathlib.Path('/custom/gradle'))
        with mock.patch.dict(os.environ):
            os.environ.pop('GRADLE_USER_HOME', None)
            self.assertEqual(gradle_home(), pathlib.Path.home() / '.gradle')

    def test_old_absolute_windows_path_is_rerooted_under_this_gradle_home(self):
        with mock.patch.dict(os.environ, {'GRADLE_USER_HOME': '/other/gradle'}):
            got = resolve_local_path('/repo', OLD_WINDOWS)
        self.assertEqual(got, pathlib.Path('/other/gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/'
                                           '0.92.2+1.20.1/625ee0/fabric-api-0.92.2+1.20.1.jar'))

    def test_posix_gradle_cache_path_is_rerooted_too(self):
        with mock.patch.dict(os.environ, {'GRADLE_USER_HOME': '/other/gradle'}):
            got = resolve_local_path('/repo', '/home/x/.gradle/caches/modules-2/files-2.1/a/b/1/h/b-1.jar')
        self.assertEqual(got, pathlib.Path('/other/gradle/caches/modules-2/files-2.1/a/b/1/h/b-1.jar'))

    def test_relative_paths_stay_under_the_repo_root(self):
        self.assertEqual(resolve_local_path('/repo', 'dependencies/mods/x.jar'), pathlib.Path('/repo/dependencies/mods/x.jar'))

    def test_checked_in_lock_has_no_resolution_into_a_foreign_home(self):
        rows = json.loads((ROOT / 'dependencies/platform-lock.json').read_text(encoding='utf8'))
        with tempfile.TemporaryDirectory() as tmp, mock.patch.dict(os.environ, {'GRADLE_USER_HOME': tmp}):
            for row in rows:
                got = str(resolve_local_path(ROOT, row['localPath']))
                self.assertTrue(got.startswith(tmp) or got.startswith(str(ROOT)), row['slug'])

    def test_export_server_reads_a_gradle_cache_row_from_this_machines_gradle_home(self):
        import export_server
        with tempfile.TemporaryDirectory() as tmp, mock.patch.dict(os.environ, {'GRADLE_USER_HOME': tmp + '/gh'}):
            root = pathlib.Path(tmp) / 'repo'
            for sub in ('build/libs', 'dist', 'dependencies'):
                (root / sub).mkdir(parents=True)
            cache = pathlib.Path(tmp) / 'gh/caches/modules-2/files-2.1/g/a/1/h'
            cache.mkdir(parents=True)
            data = b'fabric api stub'
            (cache / 'a-1.jar').write_bytes(data)
            (root / 'build/libs/graves-and-glory-9.9.9.jar').write_bytes(b'jar')
            row = {'env': {'server': 'required'}, 'fileName': 'a-1.jar', 'hashes': {'sha512': hashlib.sha512(data).hexdigest()},
                   'localPath': 'C:\\Users\\someone\\.gradle\\caches\\modules-2\\files-2.1\\g\\a\\1\\h\\a-1.jar'}
            (root / 'dependencies/platform-lock.json').write_text(json.dumps([row]))
            player = root / 'dist/graves-and-glory-9.9.9.zip'
            with zipfile.ZipFile(player, 'w') as z:
                z.writestr('docs/N.txt', 'n')
            (root / 'dist/package-manifest.json').write_text(
                json.dumps({'fileName': player.name, 'sha256': hashlib.sha256(player.read_bytes()).hexdigest()}))
            with zipfile.ZipFile(root / 'dist/graves-and-glory-9.9.9.mrpack', 'w') as z:
                z.writestr('server-overrides/server.properties', 'x=1\n')
            saved = {n: getattr(export_server, n) for n in ('ROOT', 'version', 'source_identity', 'validate_player_jar')}
            gate = export_server.build_skins.release_gate
            export_server.ROOT = root
            export_server.version = lambda: '9.9.9'
            export_server.source_identity = lambda: {}
            export_server.validate_player_jar = lambda p, e: None
            export_server.build_skins.release_gate = lambda r, argv, tool='': ''
            try:
                with redirect_stdout(io.StringIO()):
                    export_server.export([])
            finally:
                for n, v in saved.items():
                    setattr(export_server, n, v)
                export_server.build_skins.release_gate = gate
            with zipfile.ZipFile(root / 'dist/graves-and-glory-9.9.9-server.zip') as z:
                self.assertEqual(z.read('mods/a-1.jar'), data)


if __name__ == '__main__':
    unittest.main(verbosity=2)
