"""Static and stubbed-build checks for tools/export_server.py (the server zip: tools/server-kit plus the mod jars).

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
REPO=pathlib.Path(__file__).resolve().parents[2]
KIT=REPO/'tools/server-kit'
KIT_FILES={p.name for p in KIT.iterdir() if p.is_file() and p.name!='.gitattributes'}
EXPECTED_FIXED=KIT_FILES
SCRIPT_EXT=('.ps1','.sh')

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
        shutil.copytree(KIT,root/'tools/server-kit')
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

    def test_no_harness_test_or_dev_files(self):
        banned_ext=('.java','.class','.py','.pyc','.bat','.gradle','.kts','.bbmodel','.log','.tmp','.env')
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

    def test_kit_files_are_the_kit_sources_byte_for_byte(self):
        for name in KIT_FILES:self.assertEqual(self.files[name],(KIT/name).read_bytes(),name)
        self.assertNotIn('.gitattributes',self.names)

    def test_no_old_duplicate_scripts_or_readme(self):
        for gone in ('setup-server.ps1','start-server.ps1','SERVER-README.txt','server.properties'):self.assertNotIn(gone,self.names)

    def test_scripts_are_the_kit_scripts(self):
        scripts={n for n in self.names if n.endswith(('.ps1','.bat','.cmd','.sh'))}
        want={j+e for j in ('setup','start','stop','backup','restore','update','gate','lib') for e in SCRIPT_EXT}
        self.assertEqual(scripts,want)

    def test_kit_defaults_are_the_documented_ones(self):
        props=dict(l.split('=',1) for l in self.text('server.properties.default').splitlines() if '=' in l and not l.startswith('#'))
        self.assertEqual(props['server-port'],'25580')
        self.assertEqual(props['white-list'],'true');self.assertEqual(props['enforce-whitelist'],'true')
        self.assertEqual(props['online-mode'],'true');self.assertEqual(props['enable-rcon'],'false')
        self.assertEqual(props['level-name'],'Graves and Glory');self.assertEqual(props['max-players'],'16')
        self.assertEqual(self.text('whitelist.template.json').strip(),'[]')
        self.assertNotIn('25565',self.text('server.properties.default'))

    def test_shell_scripts_ship_executable_with_lf(self):
        with zipfile.ZipFile(self.output) as pack:
            for info in pack.infolist():
                if info.filename.endswith('.sh'):
                    self.assertEqual((info.external_attr>>16)&0o777,0o755,info.filename)
                    self.assertNotIn(b'\r\n',pack.read(info.filename),info.filename)

    def test_no_script_writes_eula_true(self):
        # eula=true appears only in text a person reads or in a read-only check; no script line writes it.
        writer=re.compile(r'(Set-Content|Add-Content|Out-File|New-Item|Tee-Object|WriteAll|printf|echo|>>?)[^\n]*eula',re.I)
        for name in sorted(KIT_FILES):
            if not name.endswith(SCRIPT_EXT):continue
            for no,line in enumerate(self.text(name).splitlines(),1):
                if 'eula' not in line.lower():continue
                with self.subTest(script=name,line=no):
                    if re.search(r'eula\s*=\s*true',line,re.I):
                        self.assertRegex(line,r'(?i)Select-String|grep|yourself|never|person|agree|edit|change',line)
                    if not re.search(r'eula\s*=\s*false',line,re.I):
                        # Only the single eula=false placeholder (and a dry-run notice) may sit on a writing line.
                        self.assertIsNone(writer.search(line) if 'Write-Kit' not in line and 'gg_say' not in line and 'die' not in line else None,line)
        self.assertNotIn('eula=',self.text('setup.ps1').lower());self.assertNotIn('eula=',self.text('setup.sh').lower())

    def test_start_requires_operator_supplied_eula(self):
        ps=self.text('start.ps1');sh=self.text('start.sh')
        self.assertIn("Select-String -LiteralPath 'eula.txt' -Pattern '^eula=true",ps)
        self.assertIn('This kit never does it for you',ps)
        self.assertIn("'eula=false'",ps)
        self.assertNotRegex(ps,r"(?i)Set-Content[^\n]*'eula=true'")
        self.assertLess(ps.index('EULA not accepted. Read'),ps.index('New-Backup'))
        self.assertLess(ps.index('New-Backup'),ps.index('Process]::Start'))
        self.assertRegex(sh,r'grep[^\n]*eula=true')
        self.assertRegex(sh,r'eula=false')

    def test_start_names_setup_when_the_launcher_is_missing(self):
        self.assertIn('Run .\\setup.ps1 first',self.text('start.ps1'))
        self.assertIn('Run ./setup.sh first',self.text('start.sh'))

    def test_setup_does_not_start_server_or_accept_terms(self):
        for name in ('setup.ps1','setup.sh'):
            setup=self.text(name)
            self.assertNotRegex(setup,r'&\s*java|Start-Process|-jar\b|\bjava\b -')
            self.assertIn(export_server.FABRIC_URL,setup)
            self.assertIn(export_server.FABRIC_SHA256,setup)
            self.assertIn('Launcher already exists',setup)
            self.assertIn('hash mismatch',setup)
        self.assertIn('Invoke-WebRequest',self.text('setup.ps1'));self.assertIn('Get-FileHash',self.text('setup.ps1'))

    def test_readme_is_the_one_accurate_readme(self):
        readme=self.text('README.md')
        for needle in ('Java 17','setup.ps1','start.ps1','stop.ps1','backup.ps1','update.ps1','25580','Read https://aka.ms/MinecraftEULA yourself'):
            self.assertIn(needle,readme)
        self.assertIn('No script in this kit writes that line',readme)
        for stale in ('Java 21','Starlink','candidate','server-port=25565','127.0.0.1:25565','overlay','setup-server','start-server','delete the extracted','tools/export_server.py'):
            self.assertNotIn(stale,readme)

    def test_drifted_launcher_pin_stops_the_export(self):
        with tempfile.TemporaryDirectory() as other:
            root=pathlib.Path(other)/'root';shutil.copytree(self.root,root)
            (root/f'dist/graves-and-glory-{RELEASE}-server.zip').unlink()
            sh=root/'tools/server-kit/setup.sh';sh.write_bytes(sh.read_bytes().replace(export_server.FABRIC_SHA256.encode(),b'0'*64))
            with self.assertRaisesRegex(RuntimeError,'pinned Fabric'):self.run_export(root)
            self.assertFalse((root/f'dist/graves-and-glory-{RELEASE}-server.zip').exists())

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
