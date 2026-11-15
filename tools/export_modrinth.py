"""Prepare a local .mrpack from verified release files; never publishes anything."""
import hashlib
import json
import pathlib
import re
import sys
import urllib.parse
import zipfile
from release_config import ROOT, version, source_identity, validate_player_jar
from release_files import resolve_local_path, mrpack_summary
import build_skins

MINECRAFT_VERSION='1.21.1'
FABRIC_LOADER='0.19.5'  # the pin export_curseforge.py reuses
ALLOWED_HOSTS={'cdn.modrinth.com','github.com','raw.githubusercontent.com','gitlab.com'}

def validate_index(index):
    if index['formatVersion']!=1 or index['game']!='minecraft':raise ValueError('Invalid pack format')
    paths=set()
    for file in index['files']:
        path=file['path'];parts=path.replace('\\','/').split('/')
        if path.startswith(('/','\\')) or ':' in path or '..' in parts or '' in parts or '.' in parts or '\\' in path:
            raise ValueError('Unsafe instance path: '+path)
        if path in paths:raise ValueError('Duplicate download path: '+path)
        paths.add(path)
        if not all(key in file['hashes'] for key in ('sha1','sha512')):raise ValueError('Required file hashes missing')
        for algorithm,length in [('sha1',40),('sha512',128)]:
            if not re.fullmatch('[0-9a-f]{'+str(length)+'}',file['hashes'][algorithm]):raise ValueError('Malformed file hash')
        if not file['downloads']:raise ValueError('Missing download URL')
        for url in file['downloads']:
            parsed=urllib.parse.urlparse(url)
            if parsed.scheme!='https' or parsed.hostname not in ALLOWED_HOSTS:raise ValueError('Unapproved download host')
        for side in ('client','server'):
            if file['env'][side] not in ('required','unsupported','optional'):raise ValueError('Invalid environment')

def locked_files(rows,root):
    """Index entries for every pinned file (mods, shaders, resource packs); refuse when a local file differs from its pin."""
    files=[]
    for row in rows:
        data=resolve_local_path(root,row['localPath']).read_bytes()
        if hashlib.sha512(data).hexdigest()!=row['hashes']['sha512']:raise RuntimeError('Platform lock is stale: '+row['slug'])
        folder={'mod':'mods','shader':'shaderpacks','resourcepack':'resourcepacks'}[row['kind']]
        files.append({'path':folder+'/'+row['fileName'],'hashes':row['hashes'],
                      'downloads':row['downloads'],'fileSize':len(data),'env':row['env']})
    return files

def export(argv=()):
    # The tier art gate first, as tools/package.py runs it (design 4.4; Task 9 review I2): refuse while a troop draws a stopgap
    # tier unless --allow-stopgap, which marks the .mrpack -stopgap and takes only the -stopgap player archive.
    stopgap=build_skins.release_gate(ROOT,list(argv),'export_modrinth')
    release=version();jar=ROOT/f'build/libs/graves-and-glory-{release}.jar'
    validate_player_jar(jar,source_identity())
    rows=json.loads((ROOT/'dependencies/platform-lock.json').read_text(encoding='utf8'))
    index={'formatVersion':1,'game':'minecraft','versionId':release,'name':'Graves and Glory',
           'summary':mrpack_summary(release),
           'dependencies':{'minecraft':MINECRAFT_VERSION,'fabric-loader':FABRIC_LOADER},'files':[]}
    index['files']=locked_files(rows,ROOT)
    validate_index(index)
    manifest=json.loads((ROOT/'dist/package-manifest.json').read_text())
    archive=ROOT/'dist'/build_skins.release_archive(manifest,release,stopgap,'export_modrinth')
    output=ROOT/'dist'/build_skins.release_name(release,stopgap,'.mrpack');temp=output.with_suffix('.mrpack.tmp')
    with zipfile.ZipFile(temp,'w',zipfile.ZIP_DEFLATED) as pack:
        pack.writestr('modrinth.index.json',json.dumps(index,indent=2))
        pack.write(jar,'overrides/mods/'+jar.name)
        # Documentation/notices only, from the verified ordinary player archive.
        if hashlib.sha256(archive.read_bytes()).hexdigest()!=manifest['sha256']:raise RuntimeError('Player archive hash mismatch')
        with zipfile.ZipFile(archive) as source:
            for name in source.namelist():
                if name.startswith('docs/'):pack.writestr('overrides/'+name,source.read(name))
        server='\n'.join(['# Local dedicated-server candidate; configure hosting before public use.',
                          'level-name=Graves and Glory','level-type=minecraft:flat',
                          'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains","structures":{}}',
                          'generate-structures=false',
                          'gamemode=adventure','difficulty=normal','online-mode=true','max-players=16',
                          'server-ip=127.0.0.1','server-port=25565','spawn-protection=0','enable-rcon=false',''])
        pack.writestr('server-overrides/server.properties',server)
        pack.writestr('server-overrides/SERVER-README.txt',
                      'Install Java 21+ and Fabric for Minecraft 1.21.1. Review the Minecraft server EULA yourself.\n'
                      'No EULA acceptance or credentials are supplied. This template binds localhost only.\n'
                      'Public matchmaking and centrally secured profiles are not implemented yet.\n')
    temp.replace(output)
    print(json.dumps({'fileName':output.name,'sha256':hashlib.sha256(output.read_bytes()).hexdigest(),
                      'downloadFiles':len(index['files']),'serverDownloadFiles':sum(r['env']['server']=='required' for r in rows),
                      'status':'Prepared locally; launcher import and platform moderation remain pending'},indent=2))

if __name__=='__main__':export(sys.argv[1:])
