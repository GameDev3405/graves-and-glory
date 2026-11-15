"""The Modrinth and CurseForge exports refuse a pinned file whose bytes do not match its hash. Generic: a made-up dependency row."""
import hashlib
import pathlib
import sys
import tempfile
import unittest

REPO = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO / 'tools'))
import export_curseforge  # noqa: E402
import export_modrinth  # noqa: E402

DATA = b'stub pack bytes'


def row_for(data):
    return {'slug': 'made-up-pack', 'kind': 'resourcepack', 'fileName': 'MadeUp.zip', 'localPath': 'dependencies/resourcepacks/MadeUp.zip',
            'hashes': {'sha512': hashlib.sha512(DATA).hexdigest(), 'sha1': '0' * 40}, 'fileSize': len(DATA),
            'downloads': ['https://example.invalid/MadeUp.zip'], 'env': {'client': 'required', 'server': 'unsupported'}}


def stub(tamper):
    row = row_for(DATA)
    root = pathlib.Path(tempfile.mkdtemp())
    path = root / row['localPath']
    path.parent.mkdir(parents=True)
    path.write_bytes(DATA + b'!' if tamper else DATA)
    return row, root


class ExportPinRefusal(unittest.TestCase):
    def test_exports_accept_matching_file(self):
        row, root = stub(False)
        self.assertEqual(list(export_curseforge.check_bundled([row], root).values()), [DATA])
        self.assertEqual(export_modrinth.locked_files([row], root)[0]['fileSize'], len(DATA))

    def test_exports_refuse_on_mismatch(self):
        row, root = stub(True)
        with self.assertRaisesRegex(RuntimeError, 'made-up-pack'):
            export_curseforge.check_bundled([row], root)
        with self.assertRaisesRegex(RuntimeError, 'made-up-pack'):
            export_modrinth.locked_files([row], root)


if __name__ == '__main__':
    unittest.main()
