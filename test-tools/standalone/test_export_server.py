"""Static and stubbed-build checks for tools/export_server.py (the headless server kit).

The export runs against a throwaway ROOT in a temp dir with a stub jar; nothing real is built, and nothing here ever
writes eula.txt or eula=true. The EULA gate is asserted by reading the generated launcher scripts as text."""
import hashlib
import io
import json
import pathlib
import re
import shutil
import sys
import tempfile
import unittest
import zipfile
from contextlib import redirect_stdout

sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[2]/'tools'))
import export_server

RELEASE='9.9.9'
DEP_BYTES=b'stub dependency jar'
EXPECTED_FIXED={'server.properties','setup-server.ps1','start-server.ps1','SERVER-README.txt'}

class ServerKitChecks(unittest.TestCase):
    @staticmethod
    def run_export(root):
        saved={n:getattr(export_server,n) for n in ('ROOT','version','source_identity','validate_player_jar')}
        saved_gate=export_server.build_skins.release_gate
        export_server.ROOT=root;export_server.version=lambda:RELEASE
        export_server.source_identity=lambda:{'stub':True}
        export_server.validate_player_jar=lambda path,expected:None
        export_server.build_skins.release_gate=lambda r,argv,tool='':''
        try:
            with redirect_stdout(io.StringIO()):export_server.export([])
        finally:
            for n,v in saved.items():setattr(export_server,n,v)
            export_server.build_skins.release_gate=saved_gate

    @classmethod
    def setUpClass(cls):
        cls.tmp=tempfile.TemporaryDirectory()
        root=cls.root=pathlib.Path(cls.tmp.name)
        (root/'build/libs').mkdir(parents=True);(root/'dist').mkdir();(root/'dependencies/mods').mkdir(parents=True)
        jar=root/f'build/libs/graves-and-glory-{RELEASE}.jar';jar.write_bytes(b'stub mod jar')
        (root/'dependencies/mods/dep.jar').write_bytes(DEP_BYTES)
        (root/'dependencies/mods/client-only.jar').write_bytes(b'client only')
        sha=hashlib.sha512(DEP_BYTES).hexdigest()
        rows=[{'env':{'server':'required'},'localPath':'dependencies/mods/dep.jar','fileName':'dep.jar','hashes':{'sha512':sha}},
              {'env':{'server':'unsupported'},'localPath':'dependencies/mods/client-only.jar','fileName':'client-only.jar','hashes':{'sha512':'0'*128}}]
        (root/'dependencies/platform-lock.json').write_text(json.dumps(rows))
        player=root/'dist'/f'graves-and-glory-{RELEASE}.zip'
        with zipfile.ZipFile(player,'w') as pack:
            pack.writestr('docs/NOTICE.txt','notice');pack.writestr('docs/SOURCES.txt','sources')
            # Things the kit must NOT pick up from the player archive.
            pack.writestr('README.txt','player readme');pack.writestr('mods/shader-mod.jar','client mod')
        (root/'dist/package-manifest.json').write_text(json.dumps(
            {'fileName':player.name,'sha256':hashlib.sha256(player.read_bytes()).hexdigest()}))
        with zipfile.ZipFile(root/'dist'/f'graves-and-glory-{RELEASE}.mrpack','w') as pack:
            pack.writestr('server-overrides/server.properties','online-mode=true\n')
            pack.writestr('overrides/options.txt','client file')
        cls.run_export(root)
        cls.output=root/'dist'/f'graves-and-glory-{RELEASE}-server.zip'
        with zipfile.ZipFile(cls.output) as pack:
            cls.names=sorted(pack.namelist())
            cls.files={n:pack.read(n) for n in cls.names}
        cls.manifest=json.loads((root/'dist/server-manifest.json').read_text())

    @classmethod
    def tearDownClass(cls):cls.tmp.cleanup()

    def text(self,name):return self.files[name].decode('utf8')

    def test_expected_file_list(self):
        want=EXPECTED_FIXED|{f'mods/graves-and-glory-{RELEASE}.jar','mods/dep.jar','docs/NOTICE.txt','docs/SOURCES.txt'}
        self.assertEqual(set(self.names),want)
        self.assertEqual(self.manifest['mods'],['mods/dep.jar',f'mods/graves-and-glory-{RELEASE}.jar'])
        self.assertEqual(self.manifest['entriesVerified'],len(want))
        self.assertEqual(self.text('server.properties'),'online-mode=true\n')

    def test_no_harness_test_or_dev_files(self):
        banned_ext=('.java','.class','.py','.pyc','.bat','.sh','.gradle','.kts','.bbmodel','.log','.tmp','.env')
        banned_part=re.compile(r'(^|/)(\.git|\.gitignore|\.claude|\.idea|__pycache__|node_modules|src|tools|test-tools|build|run|saves|logs)(/|$)'
                               r'|harness|gametest|junit|(^|[^a-z])tests?([^a-z]|$)|Check\.|credential|secret|profile',re.I)
        for name in self.names:
            with self.subTest(name=name):
                self.assertFalse(name.lower().endswith(banned_ext),name)
                self.assertIsNone(banned_part.search(name),name)
                self.assertFalse(name.startswith(('/','..')) or '\\' in name or ':' in name,name)
        for name in self.names:
            if name.startswith('mods/'):self.assertTrue(name.endswith('.jar'),name)
        self.assertNotIn('mods/client-only.jar',self.names)
        self.assertNotIn('mods/shader-mod.jar',self.names)

    def test_no_eula_file_in_kit(self):
        self.assertFalse([n for n in self.names if 'eula' in n.lower()])

    def test_launchers_never_write_or_set_eula(self):
        scripts={n:self.text(n) for n in self.names if n.endswith(('.ps1','.bat','.cmd','.sh'))}
        self.assertEqual(set(scripts),{'setup-server.ps1','start-server.ps1'})
        write_cmd=re.compile(r'Set-Content|Add-Content|Out-File|New-Item|Tee-Object|WriteAll|\bsc\s|\bac\s|>>?|\becho\b|\bcopy\b|Copy-Item|Rename-Item',re.I)
        for name,body in scripts.items():
            for no,line in enumerate(body.splitlines(),1):
                if 'eula' not in line.lower():continue
                with self.subTest(script=name,line=no):
                    # The only permitted mention is a read-only check or the instruction text of the refusal.
                    self.assertNotRegex(line,write_cmd)
        self.assertNotIn('eula',scripts['setup-server.ps1'].lower())
        # eula=true appears only as the pattern of a read-only Select-String check.
        for name,body in scripts.items():
            for m in re.finditer(r'eula\s*=\s*true',body,re.I):
                line=body[body.rfind('\n',0,m.start())+1:body.find('\n',m.end())]
                self.assertIn('Select-String',line,f'{name}: eula=true outside a read-only check')
                self.assertNotIn('-Value',line)

    def test_start_requires_operator_supplied_eula(self):
        start=self.text('start-server.ps1')
        gate=[l for l in start.splitlines() if 'eula.txt' in l]
        self.assertEqual(len(gate),1,gate)
        gate=gate[0]
        # Refuses (throw) when eula.txt is missing or does not say eula=true; reads with Select-String only.
        self.assertIn("!(Test-Path -LiteralPath 'eula.txt')",gate)
        self.assertIn("!(Select-String -LiteralPath 'eula.txt' -Pattern '^eula=true$' -Quiet)",gate)
        self.assertIn('-or',gate);self.assertIn('throw',gate)
        self.assertIn('yourself',gate)
        # The gate comes before the server is launched, and the launcher check too.
        self.assertLess(start.index('eula.txt'),start.index('& java'))
        self.assertLess(start.index("throw 'Run setup-server.ps1 first.'"),start.index('& java'))
        self.assertIn("$ErrorActionPreference = 'Stop'",start)

    def test_setup_does_not_start_server_or_accept_terms(self):
        setup=self.text('setup-server.ps1')
        self.assertNotRegex(setup,r'&\s*java|Start-Process|-jar\b')
        self.assertIn('Invoke-WebRequest',setup)
        self.assertIn(export_server.FABRIC_SHA256,setup)
        self.assertIn('Get-FileHash',setup)

    def test_readme_states_operator_acceptance(self):
        readme=self.text('SERVER-README.txt')
        self.assertRegex(readme,r'(?i)EULA acceptance are not included')
        self.assertRegex(readme,r'(?i)Read https://aka\.ms/MinecraftEULA yourself')
        self.assertRegex(readme,r'(?i)supply eula\.txt')
        self.assertRegex(readme,r'(?i)never supplies your agreement')
        self.assertRegex(readme,r'(?i)does not start the server or accept any terms')

    def test_tampered_dependency_stops_the_export(self):
        # QA matrix B10: a server dependency whose bytes no longer match the lock's sha512 is refused and nothing is written.
        with tempfile.TemporaryDirectory() as other:
            root=pathlib.Path(other)/'root'
            shutil.copytree(self.root,root)
            (root/f'dist/graves-and-glory-{RELEASE}-server.zip').unlink()
            (root/'dependencies/mods/dep.jar').write_bytes(DEP_BYTES+b'x')
            with self.assertRaisesRegex(RuntimeError,'Stale server dependency'):self.run_export(root)
            self.assertFalse((root/f'dist/graves-and-glory-{RELEASE}-server.zip').exists())
            self.assertFalse(list((root/'dist').glob('*.tmp')))


if __name__=='__main__':unittest.main(verbosity=2)
