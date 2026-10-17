"""Validate rejection behavior with real ZIP fixtures, without launching the game."""
import json
import pathlib
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[2] / 'tools'))
from release_config import validate_player_jar

class ReleaseIntegrityChecks(unittest.TestCase):
    def fixture(self, root, *, mod_id='gravesandglory', version='0.3.1', fingerprint='expected', extra=None, missing=False):
        p = pathlib.Path(root) / 'player.jar'
        identity = {'modId':'gravesandglory', 'version':'0.3.1', 'sourceSha256':fingerprint}
        metadata = {'id':mod_id, 'version':version, 'entrypoints':{'main':['gg.gravesandglory.GravesAndGlory'], 'client':['gg.gravesandglory.GravesAndGloryClient']}}
        with zipfile.ZipFile(p,'w') as z:
            z.writestr('fabric.mod.json',json.dumps(metadata))
            z.writestr('gravesandglory-build.json',json.dumps(identity))
            z.writestr('gg/gravesandglory/GravesAndGlory.class',b'fixture')
            if not missing:z.writestr('gg/gravesandglory/GravesAndGloryClient.class',b'fixture')
            if extra:z.writestr(extra,b'fixture')
        return p

    def test_identity_and_stale_build_rejection(self):
        expected = {'modId':'gravesandglory','version':'0.3.1','sourceSha256':'expected'}
        with tempfile.TemporaryDirectory() as root:
            validate_player_jar(self.fixture(root),expected)
            for kwargs in [{'mod_id':'bannerbound'},{'version':'0.3.0'},{'fingerprint':'stale'},{'missing':True}]:
                with self.subTest(kwargs=kwargs),self.assertRaises(RuntimeError):
                    validate_player_jar(self.fixture(root,**kwargs),expected)

    def test_harness_and_service_provider_rejection(self):
        expected = {'modId':'gravesandglory','version':'0.3.1','sourceSha256':'expected'}
        with tempfile.TemporaryDirectory() as root:
            for extra in ['gg/gravesandglory/CampaignChecks.class','gg/gravesandglory/Spec$RevisionChecksRow.class',
                          'gg/gravesandglory/LegacyDataMigrationCheck.class','META-INF/services/gg.gravesandglory.Diagnostics$Probe']:
                with self.subTest(extra=extra),self.assertRaises(RuntimeError):
                    validate_player_jar(self.fixture(root,extra=extra),expected)

if __name__=='__main__':unittest.main(verbosity=2)
