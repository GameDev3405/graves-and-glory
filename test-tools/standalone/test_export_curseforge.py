"""Static and stubbed-build checks for tools/export_curseforge.py: manifest shape, ID gating, and overrides contents.

The export runs against a throwaway ROOT in a temp dir with a stub jar; nothing real is built and nothing is uploaded."""
import hashlib
import io
import json
import pathlib
import sys
import tempfile
import unittest
import zipfile
from contextlib import redirect_stdout

sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[2]/'tools'))
import export_curseforge

REPO=pathlib.Path(__file__).resolve().parents[2]
RELEASE='9.9.9'
DEP_BYTES=b'stub dependency jar'

def stub_rows():
    sha=hashlib.sha512(DEP_BYTES).hexdigest()
    return [{'slug':'dep-a','fileName':'dep-a.jar','localPath':'dependencies/mods/dep-a.jar','hashes':{'sha512':sha}},
            {'slug':'dep-b','fileName':'dep-b.zip','localPath':'dependencies/mods/dep-b.zip','hashes':{'sha512':sha}}]

def stub_ids():
    def entry(slug,name,project,file,file_name):
        return {'name':name,'status':'resolved','projectID':project,'fileID':file,'fileName':file_name,
                'projectUrl':'https://www.curseforge.com/minecraft/mc-mods/'+slug,
                'sourceUrl':'https://www.curseforge.com/minecraft/mc-mods/%s/files/%d'%(slug,file)}
    return {'entries':{'dep-a':entry('dep-a','Dep <A>',111,2221,'dep-a.jar'),'dep-b':entry('dep-b','Dep B',112,2222,'dep-b.zip')}}

class CurseForgeChecks(unittest.TestCase):
    @staticmethod
    def make_root(tmp,ids):
        root=pathlib.Path(tmp)
        (root/'build/libs').mkdir(parents=True);(root/'dist').mkdir();(root/'dependencies/mods').mkdir(parents=True)
        (root/f'build/libs/graves-and-glory-{RELEASE}.jar').write_bytes(b'stub mod jar')
        for name in ('dep-a.jar','dep-b.zip'):(root/'dependencies/mods'/name).write_bytes(DEP_BYTES)
        (root/'dependencies/platform-lock.json').write_text(json.dumps(stub_rows()))
        (root/'dependencies/curseforge-ids.json').write_text(json.dumps(ids))
        player=root/'dist'/f'graves-and-glory-{RELEASE}.zip'
        with zipfile.ZipFile(player,'w') as pack:
            pack.writestr('docs/NOTICE.txt','notice');pack.writestr('README.txt','player readme');pack.writestr('mods/shader-mod.jar','client mod')
        (root/'dist/package-manifest.json').write_text(json.dumps(
            {'fileName':player.name,'sha256':hashlib.sha256(player.read_bytes()).hexdigest()}))
        return root

    @staticmethod
    def run_export(root):
        names=('ROOT','version','source_identity','validate_player_jar')
        saved={n:getattr(export_curseforge,n) for n in names}
        saved_gate=export_curseforge.build_skins.release_gate
        export_curseforge.ROOT=root;export_curseforge.version=lambda:RELEASE
        export_curseforge.source_identity=lambda:{'stub':True}
        export_curseforge.validate_player_jar=lambda path,expected:None
        export_curseforge.build_skins.release_gate=lambda r,argv,tool='':''
        try:
            with redirect_stdout(io.StringIO()):export_curseforge.export([])
        finally:
            for n,v in saved.items():setattr(export_curseforge,n,v)
            export_curseforge.build_skins.release_gate=saved_gate

    def test_manifest_shape(self):
        rows=stub_rows();entries=export_curseforge.load_ids(rows,stub_ids())
        manifest=export_curseforge.build_manifest(rows,entries,RELEASE)
        self.assertEqual(manifest['manifestType'],'minecraftModpack')
        self.assertEqual(manifest['manifestVersion'],1)
        self.assertEqual(manifest['minecraft']['version'],'1.21.1')
        self.assertEqual(manifest['minecraft']['modLoaders'],[{'id':'fabric-0.19.5','primary':True}])
        self.assertEqual(manifest['version'],RELEASE);self.assertEqual(manifest['overrides'],'overrides')
        self.assertEqual(manifest['files'],[{'projectID':111,'fileID':2221,'required':True},{'projectID':112,'fileID':2222,'required':True}])

    def test_modlist_escapes_names(self):
        rows=stub_rows();html=export_curseforge.build_modlist(rows,export_curseforge.load_ids(rows,stub_ids()))
        self.assertIn('Dep &lt;A&gt;',html);self.assertNotIn('Dep <A>',html)
        self.assertEqual(html.count('<li>'),2)

    def test_refuses_unresolved_missing_and_stale_ids(self):
        rows=stub_rows()
        unresolved=stub_ids();unresolved['entries']['dep-b'].update(status='unresolved',projectID=None,fileID=None)
        missing=stub_ids();del missing['entries']['dep-a']
        stale=stub_ids();stale['entries']['dep-a']['fileName']='dep-a-old.jar'
        guessed=stub_ids();guessed['entries']['dep-a']['fileID']='2221'
        nosource=stub_ids();nosource['entries']['dep-a']['sourceUrl']=''
        for label,ids in (('unresolved',unresolved),('missing',missing),('stale',stale),('string id',guessed),('no source',nosource)):
            with self.subTest(label):
                with self.assertRaisesRegex(ValueError,'refusing to export'):export_curseforge.load_ids(rows,ids)

    def test_export_refuses_before_writing_when_unresolved(self):
        ids=stub_ids();ids['entries']['dep-b'].update(status='unresolved',projectID=None,fileID=None)
        with tempfile.TemporaryDirectory() as tmp:
            root=self.make_root(tmp,ids)
            with self.assertRaisesRegex(ValueError,'dep-b: unresolved'):self.run_export(root)
            self.assertEqual(list((root/'dist').glob('*curseforge*')),[])

    def test_export_writes_manifest_modlist_and_overrides_only(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=self.make_root(tmp,stub_ids());self.run_export(root)
            with zipfile.ZipFile(root/'dist'/f'graves-and-glory-{RELEASE}-curseforge.zip') as pack:
                names=set(pack.namelist());manifest=json.loads(pack.read('manifest.json'))
            self.assertEqual(names,{'manifest.json','modlist.html',f'overrides/mods/graves-and-glory-{RELEASE}.jar','overrides/docs/NOTICE.txt'})
            self.assertEqual(len(manifest['files']),2)

    def test_committed_ids_cover_the_platform_lock(self):
        rows=json.loads((REPO/'dependencies/platform-lock.json').read_text(encoding='utf8'))
        ids=json.loads((REPO/'dependencies/curseforge-ids.json').read_text(encoding='utf8'))
        self.assertEqual(set(ids['entries']),{r['slug'] for r in rows if not r.get('curseforgeOverride')})
        for slug,entry in ids['entries'].items():
            with self.subTest(slug):
                self.assertTrue(entry['sourceUrl'].startswith('https://www.curseforge.com/'))
                if entry['status']!='resolved':self.assertIsNone(entry['fileID'])
        try:export_curseforge.load_ids(rows,ids)
        except ValueError as exc:self.assertIn('refusing to export',str(exc))  # unresolved IDs are allowed here; export refuses them

if __name__=='__main__':unittest.main()
