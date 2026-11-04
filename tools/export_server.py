"""Prepare the dedicated-server zip: tools/server-kit (scripts and config) plus the mod jars. No installer execution or EULA acceptance."""
import hashlib
import json
import sys
import zipfile
from release_config import ROOT, version, source_identity, validate_player_jar
from release_files import resolve_local_path
import build_skins

FABRIC_URL='https://meta.fabricmc.net/v2/versions/loader/1.20.1/0.16.14/1.1.2/server/jar'
FABRIC_SHA256='856a5f9fe4809ac1972778ff5bac71f8995b7ae0513caa6d0be94288b92cf113'
# tools/server-kit/setup.ps1 and setup.sh carry these two values literally; export() refuses if they drift.
KIT_SKIP={'.gitattributes'}
KIT_REQUIRED={'README.md','setup.ps1','setup.sh','start.ps1','start.sh','stop.ps1','stop.sh','backup.ps1','backup.sh','restore.ps1','restore.sh',
              'update.ps1','update.sh','gate.ps1','gate.sh','lib.ps1','lib.sh','server.properties.default','whitelist.template.json'}

def export(argv=()):
    # The tier art gate first, as tools/package.py runs it (design 4.4; Task 9 review I2): refuse while a troop draws a stopgap
    # tier unless --allow-stopgap, which marks the kit -stopgap and takes only the -stopgap player archive.
    stopgap=build_skins.release_gate(ROOT,list(argv),'export_server')
    release=version();jar=ROOT/f'build/libs/graves-and-glory-{release}.jar'
    validate_player_jar(jar,source_identity())
    rows=json.loads((ROOT/'dependencies/platform-lock.json').read_text(encoding='utf8'))
    files={'mods/'+jar.name:jar.read_bytes()}
    for row in rows:
        if row['env']['server']!='required':continue
        data=resolve_local_path(ROOT,row['localPath']).read_bytes()
        if hashlib.sha512(data).hexdigest()!=row['hashes']['sha512']:raise RuntimeError('Stale server dependency')
        files['mods/'+row['fileName']]=data
    manifest=json.loads((ROOT/'dist/package-manifest.json').read_text())
    player=ROOT/'dist'/build_skins.release_archive(manifest,release,stopgap,'export_server')
    if hashlib.sha256(player.read_bytes()).hexdigest()!=manifest['sha256']:raise RuntimeError('Player archive changed')
    with zipfile.ZipFile(player) as pack:
        for name in pack.namelist():
            if name.startswith('docs/'):files[name]=pack.read(name)
    # The kit: tools/server-kit is the one layout (port 25580, whitelist on). Every file in it ships as it is, at the zip root.
    kit=ROOT/'tools/server-kit'
    for path in sorted(kit.iterdir()):
        if path.is_file() and path.name not in KIT_SKIP:files[path.name]=path.read_bytes()
    missing=KIT_REQUIRED-set(files)
    if missing:raise RuntimeError('Server kit incomplete: '+', '.join(sorted(missing)))
    for name in ('setup.ps1','setup.sh'):
        text=files[name].decode('utf8')
        if FABRIC_URL not in text or FABRIC_SHA256 not in text:raise RuntimeError(name+' does not carry the pinned Fabric URL and SHA-256')
    output=ROOT/'dist'/build_skins.release_name(release,stopgap,'-server.zip');temp=output.with_suffix('.zip.tmp')
    with zipfile.ZipFile(temp,'w',zipfile.ZIP_DEFLATED) as pack:
        for name,data in sorted(files.items()):
            info=zipfile.ZipInfo(name,(2026,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
            info.external_attr=(0o755 if name.endswith('.sh') else 0o644)<<16
            pack.writestr(info,data)
    # Read back every allowed entry; no generated runtime/EULA file is allowlisted.
    with zipfile.ZipFile(temp) as pack:
        if pack.testzip() is not None or set(pack.namelist())!=set(files):raise RuntimeError('Invalid server archive')
        for name,data in files.items():
            if pack.read(name)!=data:raise RuntimeError('Server archive readback differs: '+name)
    temp.replace(output)
    report={'fileName':output.name,'bytes':output.stat().st_size,'sha256':hashlib.sha256(output.read_bytes()).hexdigest(),
            'build':source_identity(),'mods':sorted(n for n in files if n.startswith('mods/')),
            'entriesVerified':len(files),'scope':'Kit integrity; runtime results are recorded separately in the work log'}
    (ROOT/'dist/server-manifest.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))

if __name__=='__main__':export(sys.argv[1:])
