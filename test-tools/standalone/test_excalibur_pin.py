"""Excalibur resource pack pin: manifest and platform lock agree, the exports carry it, and every path refuses on a hash mismatch."""
import hashlib
import json
import pathlib
import sys
import tempfile
import unittest

REPO=pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0,str(REPO/'tools'))
import export_curseforge
import export_modrinth

SHA512='858c0367346ec6d58e34ecd2f3b69b73e779d655a49ca12bcdd0113106bf4516f378013bab74331ad1f02ba0b0c2c465374802210ebb3f1f6d26b1cc8936a591'
SIZE=18103896
URL='https://cdn.modrinth.com/data/hJAzl1Bs/versions/ZRWxgeyn/Excalibur_V1.21.1.zip'

def load(name):return json.loads((REPO/'dependencies'/name).read_text(encoding='utf8'))

class ExcaliburPin(unittest.TestCase):
    def setUp(self):
        self.dep=next(d for d in load('manifest.json') if d['slug']=='excalibur')
        self.row=next(r for r in load('platform-lock.json') if r['slug']=='excalibur')

    def test_pins_agree(self):
        self.assertEqual((self.dep['size'],self.dep['sha512'],self.dep['download']),(SIZE,SHA512,URL))
        self.assertEqual((self.row['fileSize'],self.row['hashes']['sha512'],self.row['downloads']),(SIZE,SHA512,[URL]))
        self.assertEqual(self.dep['sha256'],self.row['sha256'])
        self.assertEqual(self.row['kind'],'resourcepack')
        self.assertIn('NC-ND',self.dep['licenseMetadata']['id'])

    def test_mrpack_entry_is_url_download_with_hashes(self):
        entry={'path':'resourcepacks/'+self.row['fileName'],'hashes':self.row['hashes'],'downloads':self.row['downloads'],
               'env':self.row['env'],'fileSize':SIZE}
        export_modrinth.validate_index({'formatVersion':1,'game':'minecraft','files':[entry]})
        self.assertEqual(entry['path'],'resourcepacks/Excalibur_V1.21.1.zip')

    def test_curseforge_bundles_instead_of_listing(self):
        self.assertEqual(export_curseforge.split_rows([self.row]),([],[self.row]))

    def stub(self,tamper):
        data=b'stub pack bytes'
        row=dict(self.row);row['hashes']={'sha512':hashlib.sha512(data).hexdigest(),'sha1':'0'*40};row['fileSize']=len(data)
        root=pathlib.Path(tempfile.mkdtemp());path=root/row['localPath'];path.parent.mkdir(parents=True)
        path.write_bytes(data+b'!' if tamper else data)
        return row,root,data

    def test_exports_accept_matching_file(self):
        row,root,data=self.stub(False)
        self.assertEqual(list(export_curseforge.check_bundled([row],root).values()),[data])
        self.assertEqual(export_modrinth.locked_files([row],root)[0]['fileSize'],len(data))

    def test_exports_refuse_on_mismatch(self):
        row,root,_=self.stub(True)
        with self.assertRaisesRegex(RuntimeError,'excalibur'):export_curseforge.check_bundled([row],root)
        with self.assertRaisesRegex(RuntimeError,'excalibur'):export_modrinth.locked_files([row],root)

    def test_local_file_matches_pin_when_present(self):
        path=REPO/self.dep['path']
        if not path.exists():self.skipTest('pack not downloaded in this checkout (dependencies/resourcepacks is untracked)')
        data=path.read_bytes()
        self.assertEqual((len(data),hashlib.sha512(data).hexdigest()),(SIZE,SHA512))

if __name__=='__main__':unittest.main()
