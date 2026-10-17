import copy
import pathlib
import sys
import unittest

sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[2]/'tools'))
from export_modrinth import validate_index

class ModrinthFormatChecks(unittest.TestCase):
    def fixture(self):
        return {'formatVersion':1,'game':'minecraft','files':[
            {'path':'mods/example.jar','hashes':{'sha1':'a'*40,'sha512':'b'*128},
             'downloads':['https://cdn.modrinth.com/data/project/version/mod.jar'],
             'env':{'client':'required','server':'unsupported'}}]}

    def test_valid_pack(self):validate_index(self.fixture())
    def test_unsafe_paths(self):
        for path in ['../secret','/root/file','C:/secret','mods\\file.jar']:
            with self.subTest(path=path),self.assertRaises(ValueError):
                index=self.fixture();index['files'][0]['path']=path;validate_index(index)
    def test_duplicate_and_unapproved_download(self):
        index=self.fixture();index['files'].append(copy.deepcopy(index['files'][0]))
        with self.assertRaises(ValueError):validate_index(index)
        index=self.fixture();index['files'][0]['downloads']=['https://unapproved.example/mod.jar']
        with self.assertRaises(ValueError):validate_index(index)
    def test_missing_integrity(self):
        index=self.fixture();del index['files'][0]['hashes']['sha512']
        with self.assertRaises(ValueError):validate_index(index)
    def test_malformed_integrity_or_empty_downloads(self):
        index=self.fixture();index['files'][0]['hashes']['sha1']='wrong'
        with self.assertRaises(ValueError):validate_index(index)
        index=self.fixture();index['files'][0]['downloads']=[]
        with self.assertRaises(ValueError):validate_index(index)

if __name__=='__main__':unittest.main(verbosity=2)
